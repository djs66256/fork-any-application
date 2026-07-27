# RBAC 权限设计

## 角色定义

| 角色 | 查看内容 | 管理内容（CRUD） | 管理用户角色 | 导航可见项 |
|------|---------|-----------------|-------------|-----------|
| admin | ✅ 全部 | ✅ 全部 | ✅ | 仪表盘、短剧管理、用户管理 |
| editor | ✅ 全部 | ✅ 短剧+剧集 | ❌ | 仪表盘、短剧管理 |
| viewer | ✅ 全部 | ❌ | ❌ | 仪表盘、短剧管理（只读） |

## 权限矩阵

| 操作 | admin | editor | viewer | 未登录 |
|------|-------|--------|--------|--------|
| 查看仪表盘 | ✅ | ✅ | ✅ | ❌ → 重定向登录 |
| 查看短剧列表 | ✅ | ✅ | ✅ | ❌ |
| 新建短剧 | ✅ | ✅ | ❌（按钮不可见） | ❌ |
| 编辑短剧 | ✅ | ✅ | ❌ | ❌ |
| 删除短剧 | ✅ | ✅ | ❌ | ❌ |
| 查看剧集列表 | ✅ | ✅ | ✅ | ❌ |
| 新建/编辑/删除剧集 | ✅ | ✅ | ❌ | ❌ |
| 查看用户列表 | ✅ | ❌（导航不可见） | ❌ | ❌ |
| 修改用户角色 | ✅ | ❌ | ❌ | ❌ |

## 实现方案

### 1. 数据模型

`profiles` 表新增 `role` 列：

```sql
CREATE TYPE user_role AS ENUM ('admin', 'editor', 'viewer');

ALTER TABLE profiles ADD COLUMN role user_role NOT NULL DEFAULT 'viewer';
```

### 2. JWT Role 同步（Supabase Auth Hook）

创建数据库函数，在用户每次登录时从 `profiles.role` 同步到 JWT：

```sql
CREATE OR REPLACE FUNCTION public.handle_new_session()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE auth.users
  SET raw_app_meta_data = 
    raw_app_meta_data || 
    jsonb_build_object('role', (
      SELECT role::text FROM public.profiles WHERE id = NEW.user_id
    ))
  WHERE id = NEW.user_id;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_session_created
  AFTER INSERT ON auth.sessions
  FOR EACH ROW
  EXECUTE FUNCTION public.handle_new_session();
```

### 3. API 中间件

```typescript
// backend/src/middleware/admin-auth.ts

import { createClient } from '@/infrastructure/supabase/server';

export function requireRole(...roles: string[]) {
  return async (request: NextRequest) => {
    const token = request.headers.get('Authorization')?.replace('Bearer ', '');
    if (!token) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    
    const supabase = await createClient();
    const { data: { user }, error } = await supabase.auth.getUser(token);
    
    if (error || !user) 
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    
    const userRole = user.app_metadata?.role;
    if (!roles.includes(userRole))
      return NextResponse.json({ error: 'Forbidden' }, { status: 403 });
    
    // 注入用户信息到 request context
    request.user = { id: user.id, role: userRole };
    return null; // 通过
  };
}
```

### 4. RLS 策略

dramas/episodes 表：

```sql
-- 读策略：所有角色可读
CREATE POLICY "admins_read_dramas" ON dramas
  FOR SELECT TO authenticated
  USING ((auth.jwt() -> 'app_metadata' ->> 'role') IN ('admin', 'editor', 'viewer'));

-- 写策略：admin + editor 可写
CREATE POLICY "admins_write_dramas" ON dramas
  FOR INSERT TO authenticated
  WITH CHECK ((auth.jwt() -> 'app_metadata' ->> 'role') IN ('admin', 'editor'));

CREATE POLICY "admins_update_dramas" ON dramas
  FOR UPDATE TO authenticated
  USING ((auth.jwt() -> 'app_metadata' ->> 'role') IN ('admin', 'editor'));

CREATE POLICY "admins_delete_dramas" ON dramas
  FOR DELETE TO authenticated
  USING ((auth.jwt() -> 'app_metadata' ->> 'role') IN ('admin', 'editor'));
```

profiles 表：

```sql
-- 读：所有角色可读自己的 profile
CREATE POLICY "users_read_own_profile" ON profiles
  FOR SELECT TO authenticated
  USING (id = auth.uid());

-- 写 role：仅 admin
CREATE POLICY "admin_update_role" ON profiles
  FOR UPDATE TO authenticated
  USING ((auth.jwt() -> 'app_metadata' ->> 'role') = 'admin')
  WITH CHECK ((auth.jwt() -> 'app_metadata' ->> 'role') = 'admin');
```

### 5. Web 端权限控制

前端根据 `useAuth()` hook 返回的 `role` 做条件渲染：

```typescript
function AdminNav() {
  const { role } = useAuth();
  return (
    <nav>
      <NavItem href="/admin/dashboard">仪表盘</NavItem>
      <NavItem href="/admin/dramas">短剧管理</NavItem>
      {role === 'admin' && (
        <NavItem href="/admin/users">用户管理</NavItem>
      )}
    </nav>
  );
}
```

## 双重防线

```
用户请求 → 前端条件渲染（UI 隐藏 + URL guard） → API 中间件 → RLS 策略
         └── 第一道防线（不可绕过）              └── 第二道   └── 第三道（数据库层）
```

- **前端 UI 控制**：根据角色显示/隐藏导航项和操作按钮（改善体验，但不可作为安全边界）
- **API 中间件**：校验 JWT 中的 role，不符合则返回 403（主要安全边界）
- **RLS 策略**：数据库层兜底，即使绕过 API 层也无法越权操作（最终安全边界）
