# Backend 端技术方案：管理平台（Admin Panel）

> 创建日期：2026-07-27
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

```
请求 → Admin Route Handler → Auth Middleware (JWT) → Zod Validation → AdminService → Supabase Repository → DB
                                                  ├── role 校验
                                                  └── 参数校验
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `backend/src/middleware/auth.ts` | 修改 | 升级为 Supabase JWT 验证 + role 提取 |
| `backend/src/lib/schemas.ts` | 扩展 | 新增 admin API 相关 Zod schema |
| `backend/src/repositories/supabase/` | 扩展 | 新增 admin 专用 repository 方法 |
| `backend/src/services/` | 新增 | 新增 `admin/` service 目录 |
| `backend/src/app/api/admin/` | 新增 | 新增 `/api/admin/*` 路由目录 |
| `backend/src/lib/errors.ts` | 扩展 | 新增 admin 相关错误码 |
| `backend/supabase/migrations/` | 新增 | 新增 migration 文件 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `backend/src/app/api/admin/auth/login/route.ts` | 新增 | 登录接口 |
| `backend/src/app/api/admin/auth/logout/route.ts` | 新增 | 登出接口 |
| `backend/src/app/api/admin/stats/route.ts` | 新增 | 仪表盘统计接口 |
| `backend/src/app/api/admin/dramas/route.ts` | 新增 | 短剧列表 + 新建 |
| `backend/src/app/api/admin/dramas/[id]/route.ts` | 新增 | 短剧详情 + 编辑 + 删除 |
| `backend/src/app/api/admin/dramas/[id]/episodes/route.ts` | 新增 | 剧集列表 + 新建 |
| `backend/src/app/api/admin/episodes/[id]/route.ts` | 新增 | 剧集编辑 + 删除 |
| `backend/src/app/api/admin/users/route.ts` | 新增 | 用户列表 |
| `backend/src/app/api/admin/users/[id]/role/route.ts` | 新增 | 修改用户角色 |
| `backend/src/middleware/auth.ts` | 修改 | 新增 `requireAdminAuth`、`requireRole` 中间件 |
| `backend/src/lib/schemas.ts` | 修改 | 新增 admin Zod schema |
| `backend/src/lib/errors.ts` | 修改 | 新增 admin 错误码 |
| `backend/src/services/admin/admin.service.ts` | 新增 | Admin 业务逻辑服务 |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 修改 | 新增 admin CRUD 方法 |
| `backend/src/repositories/supabase/episode.supabase.repository.ts` | 修改 | 新增 admin CRUD 方法 |
| `backend/src/repositories/supabase/user.supabase.repository.ts` | 新增 | 用户查询 + 角色更新 |
| `backend/supabase/migrations/<timestamp>_add_role_to_profiles.sql` | 新增 | profiles 表添加 role 列 |
| `backend/supabase/migrations/<timestamp>_rename_episode_count.sql` | 新增 | dramas 表重命名列 |
| `backend/supabase/migrations/<timestamp>_auth_hook_role_sync.sql` | 新增 | Auth Hook + trigger |
| `backend/supabase/migrations/<timestamp>_enable_rls.sql` | 新增 | RLS 策略 |

---

## 3. API 路由设计

### 3.1 路由结构

```
backend/src/app/api/admin/
├── auth/
│   ├── login/route.ts          # POST /api/admin/auth/login
│   └── logout/route.ts         # POST /api/admin/auth/logout
├── stats/route.ts              # GET /api/admin/stats
├── dramas/
│   ├── route.ts                # GET /api/admin/dramas, POST /api/admin/dramas
│   └── [id]/
│       ├── route.ts            # GET/PUT/DELETE /api/admin/dramas/:id
│       └── episodes/
│           └── route.ts        # GET/POST /api/admin/dramas/:id/episodes
├── episodes/
│   └── [id]/
│       └── route.ts            # PUT/DELETE /api/admin/episodes/:id
└── users/
    ├── route.ts                # GET /api/admin/users
    └── [id]/
        └── role/
            └── route.ts        # PUT /api/admin/users/:id/role
```

### 3.2 中间件链

每个 Route Handler 使用以下中间件链：

```
authMiddleware → roleMiddleware(requiredRoles) → validationMiddleware(schema) → handler
```

**authMiddleware**：验证 JWT 有效性，提取 `userId` 和 `role` 到 request context
**roleMiddleware**：检查 `role` 是否在允许的角色列表中
**validationMiddleware**：使用 Zod schema 校验 query/body params

### 3.3 示例 Route Handler

```typescript
// backend/src/app/api/admin/dramas/route.ts
import { requireAuth, requireRole } from '@/middleware/auth';
import { validateBody } from '@/middleware/validation';
import { AdminDramaCreateSchema } from '@/lib/schemas';
import { AdminService } from '@/services/admin/admin.service';

export const GET = requireAuth(
  requireRole(['admin', 'editor', 'viewer'], async (request) => {
    const { searchParams } = new URL(request.url);
    const page = parseInt(searchParams.get('page') || '1');
    const pageSize = parseInt(searchParams.get('pageSize') || '20');
    const service = new AdminService();
    const result = await service.listDramas(page, pageSize);
    return NextResponse.json({ code: 0, data: result, message: 'ok' });
  })
);

export const POST = requireAuth(
  requireRole(['admin', 'editor'], async (request) => {
    const body = await request.json();
    const parsed = AdminDramaCreateSchema.parse(body);
    const service = new AdminService();
    const drama = await service.createDrama(parsed);
    return NextResponse.json({ code: 0, data: drama, message: 'ok' }, { status: 201 });
  })
);
```

---

## 4. 中间件设计

### 4.1 Auth Middleware 升级

现有 `backend/src/middleware/auth.ts` 从骨架 token 验证升级为 Supabase JWT 验证：

```typescript
import { getSupabaseAdmin } from '@/infrastructure/supabase';

export async function verifyJwt(request: NextRequest): Promise<{ userId: string; role: string } | null> {
  const token = extractBearerToken(request);
  if (!token) return null;

  const supabase = getSupabaseAdmin();
  const { data: { user }, error } = await supabase.auth.getUser(token);
  if (error || !user) return null;

  const role = user.app_metadata?.role || 'viewer';
  return { userId: user.id, role };
}

export function requireRole(roles: string[], handler: RouteHandler): RouteHandler {
  return async (request: NextRequest, context: unknown) => {
    const auth = await verifyJwt(request);
    if (!auth) {
      return NextResponse.json({ code: 401, data: null, message: '请先登录' }, { status: 401 });
    }
    if (!roles.includes(auth.role)) {
      return NextResponse.json({ code: 403, data: null, message: '无权访问' }, { status: 403 });
    }
    // 注入 auth 信息到 request
    (request as any).auth = auth;
    return handler(request, context);
  };
}
```

---

## 5. Service 层设计

### 5.1 AdminService

```typescript
// backend/src/services/admin/admin.service.ts

export class AdminService {
  constructor(
    private dramaRepo = new DramaSupabaseRepository(),
    private episodeRepo = new EpisodeSupabaseRepository(),
    private userRepo = new UserSupabaseRepository(),
  ) {}

  async getStats(): Promise<AdminStats> { ... }
  async listDramas(page: number, pageSize: number): Promise<DramaListResponse> { ... }
  async createDrama(data: AdminDramaCreate): Promise<Drama> { ... }
  async getDrama(id: string): Promise<Drama> { ... }
  async updateDrama(id: string, data: AdminDramaUpdate): Promise<Drama> { ... }
  async deleteDrama(id: string): Promise<void> { ... }
  async listEpisodes(dramaId: string): Promise<Episode[]> { ... }
  async createEpisode(dramaId: string, data: AdminEpisodeCreate): Promise<Episode> { ... }
  async updateEpisode(id: string, data: AdminEpisodeUpdate): Promise<Episode> { ... }
  async deleteEpisode(id: string): Promise<void> { ... }
  async listUsers(page: number, pageSize: number): Promise<UserListResponse> { ... }
  async updateUserRole(userId: string, role: string, currentUserId: string): Promise<UserProfile> { ... }
}
```

### 5.2 关键业务逻辑

**级联删除 Drama**：
```typescript
async deleteDrama(id: string): Promise<void> {
  // 使用 Supabase transaction
  // 1. 删除该 drama 的所有 episodes
  // 2. 删除 drama 本身
  // RLS 策略确保只有 admin/editor 可执行
}
```

**修改用户角色**：
```typescript
async updateUserRole(userId: string, role: string, currentUserId: string): Promise<UserProfile> {
  if (userId === currentUserId) {
    throw Errors.cannotModifySelf();
  }
  // 更新 profiles.role → Auth Hook 自动同步到 JWT
}
```

---

## 6. Repository 层设计

### 6.1 现有 Repository 扩展

`DramaSupabaseRepository` 新增方法：
- `create(data)` — 插入新短剧
- `update(id, data)` — 更新短剧
- `delete(id)` — 删除短剧及关联剧集
- `count()` — 统计总数

`EpisodeSupabaseRepository` 新增方法：
- `create(data)` — 插入新剧集
- `update(id, data)` — 更新剧集
- `delete(id)` — 删除剧集
- `count()` — 统计总数
- `findByDramaId(dramaId)` — 按短剧 ID 查询剧集列表

### 6.2 新增 Repository

`UserSupabaseRepository`（新增）：
- `list(page, pageSize)` — 分页查询用户列表
- `findById(id)` — 查询单个用户
- `updateRole(userId, role)` — 更新用户角色
- `count()` — 统计用户总数

---

## 7. 测试策略

### 7.1 单元测试

| 测试对象 | 测试文件 | 测试内容 |
|---------|---------|---------|
| AdminService | `backend/src/services/admin/__tests__/admin.service.test.ts` | 业务逻辑：CRUD、级联删除、角色修改 |
| Auth Middleware | `backend/src/middleware/__tests__/auth.test.ts` | JWT 验证、role 提取、权限拒绝 |
| Zod Schemas | `backend/src/lib/__tests__/schemas.test.ts` | Admin schema 校验（正常/边界/异常） |

### 7.2 集成测试

| 测试场景 | 测试内容 |
|---------|---------|
| 登录流程 | 正确凭据 → 200 + JWT；错误凭据 → 401 |
| 权限校验 | viewer 调用写 API → 403；editor 调用用户管理 → 403 |
| 级联删除 | 删除 Drama → 关联 Episode 一并删除 |
| 分页 | 边界值：page=0、pageSize=0、pageSize=1000 |

---

## 8. 迁移策略

1. 新增 migration 文件到 `backend/supabase/migrations/`
2. 本地运行 `supabase migration up` 验证
3. CI/CD 中集成 migration 自动执行
4. 现有 mock 数据不受影响，管理 API 使用 Supabase 表

---

## 9. 参考资料

| 文档 | 关键信息 |
|------|---------|
| `backend/src/infrastructure/supabase.ts` | Supabase 双客户端实例 |
| `backend/src/middleware/auth.ts` | 当前 auth 中间件实现 |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 现有 Supabase DRAMA repository |
| `backend/supabase/migrations/00000000000001_init_tables.sql` | 现有 DB schema |