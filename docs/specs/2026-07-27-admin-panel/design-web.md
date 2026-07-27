# Web 端技术方案：管理平台（Admin Panel）

> 创建日期：2026-07-27
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

### 1.1 路由结构

```
web/src/app/admin/
├── layout.tsx                  # Admin 独立 Layout（Sidebar + Header）
├── page.tsx                    # 仪表盘（/admin）
├── login/
│   └── page.tsx                # 登录页（/admin/login）
├── dramas/
│   ├── page.tsx                # 短剧列表（/admin/dramas）
│   ├── new/
│   │   └── page.tsx            # 新建短剧（/admin/dramas/new）
│   ├── [id]/
│   │   ├── edit/
│   │   │   └── page.tsx        # 编辑短剧（/admin/dramas/:id/edit）
│   │   └── episodes/
│   │       ├── page.tsx        # 剧集列表（/admin/dramas/:id/episodes）
│   │       ├── new/
│   │       │   └── page.tsx    # 新建剧集
│   │       └── [episodeId]/
│   │           └── edit/
│   │               └── page.tsx # 编辑剧集
└── users/
    └── page.tsx                # 用户列表（/admin/users）
```

### 1.2 组件层次

```
AdminLayout
├── AdminHeader（Logo + 用户头像/退出）
├── AdminSidebar（导航：仪表盘、短剧管理、用户管理）
└── AdminContent（子路由内容区）
    ├── DashboardPage
    │   └── StatCard[]（统计卡片）
    ├── DramaListPage
    │   ├── DataTable（表格）
    │   └── DramaForm（表单，新建/编辑复用）
    ├── EpisodeListPage
    │   ├── DataTable
    │   └── EpisodeForm
    ├── UserListPage
    │   └── DataTable + RoleSelect
    └── LoginPage
        └── LoginForm
```

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `web/src/app/admin/layout.tsx` | 新增 | Admin 独立 Layout |
| `web/src/app/admin/page.tsx` | 新增 | 仪表盘页 |
| `web/src/app/admin/login/page.tsx` | 新增 | 登录页 |
| `web/src/app/admin/dramas/page.tsx` | 新增 | 短剧列表页 |
| `web/src/app/admin/dramas/new/page.tsx` | 新增 | 新建短剧页 |
| `web/src/app/admin/dramas/[id]/edit/page.tsx` | 新增 | 编辑短剧页 |
| `web/src/app/admin/dramas/[id]/episodes/page.tsx` | 新增 | 剧集列表页 |
| `web/src/app/admin/dramas/[id]/episodes/new/page.tsx` | 新增 | 新建剧集页 |
| `web/src/app/admin/dramas/[id]/episodes/[episodeId]/edit/page.tsx` | 新增 | 编辑剧集页 |
| `web/src/app/admin/users/page.tsx` | 新增 | 用户列表页 |
| `web/src/features/admin/` | 新增 | Admin 功能组件目录 |
| `web/src/features/admin/components/AdminSidebar.tsx` | 新增 | 侧边导航 |
| `web/src/features/admin/components/AdminHeader.tsx` | 新增 | 顶部 Header |
| `web/src/features/admin/components/DataTable.tsx` | 新增 | 通用数据表格 |
| `web/src/features/admin/components/DramaForm.tsx` | 新增 | 短剧表单 |
| `web/src/features/admin/components/EpisodeForm.tsx` | 新增 | 剧集表单 |
| `web/src/features/admin/components/StatCard.tsx` | 新增 | 统计卡片 |
| `web/src/features/admin/hooks/useAuth.ts` | 新增 | Auth hook（session 管理） |
| `web/src/features/admin/hooks/useDramas.ts` | 新增 | 短剧数据 hook |
| `web/src/features/admin/hooks/useEpisodes.ts` | 新增 | 剧集数据 hook |
| `web/src/features/admin/hooks/useUsers.ts` | 新增 | 用户数据 hook |
| `web/src/features/admin/api/client.ts` | 新增 | Admin API 客户端 |
| `web/src/middleware.ts` | 修改 | 新增 `/admin/*` 路由认证中间件 |

---

## 3. 状态管理

### 3.1 Auth 状态

```typescript
// web/src/features/admin/hooks/useAuth.ts
interface AuthState {
  user: { id: string; email: string; role: 'admin' | 'editor' | 'viewer' } | null;
  isLoading: boolean;
  error: string | null;
}

function useAuth() {
  // 使用 @supabase/ssr 的 createBrowserClient 管理 session
  // 登录：supabase.auth.signInWithPassword
  // 登出：supabase.auth.signOut
  // 获取 role：session.user.app_metadata.role
}
```

### 3.2 数据列表状态

每个列表页使用统一的 `useListData` hook 模式：

```typescript
interface ListState<T> {
  data: T[];
  pagination: { page: number; pageSize: number; total: number; totalPages: number };
  isLoading: boolean;
  error: string | null;
}

function useDramas(page: number, pageSize: number): ListState<Drama> & { refetch: () => void } {
  // GET /api/admin/dramas?page={page}&pageSize={pageSize}
}
```

### 3.3 表单状态

```typescript
interface FormState<T> {
  values: T;
  errors: Record<string, string>;
  isSubmitting: boolean;
  submitError: string | null;
}

function useDramaForm(initialValues?: Drama): FormState<DramaFormData> & {
  setField: (field: string, value: any) => void;
  submit: () => Promise<void>;
}
```

---

## 4. 路由与导航

### 4.1 Middleware 认证

```typescript
// web/src/middleware.ts
import { createServerClient } from '@supabase/ssr';

export async function middleware(request: NextRequest) {
  // 管理平台路由需要登录
  if (request.nextUrl.pathname.startsWith('/admin')) {
    // 登录页不需要认证
    if (request.nextUrl.pathname === '/admin/login') {
      return NextResponse.next();
    }
    // 检查 session
    const supabase = createServerClient(/* ... */);
    const { data: { session } } = await supabase.auth.getSession();
    if (!session) {
      return NextResponse.redirect(new URL('/admin/login', request.url));
    }
  }
}
```

### 4.2 导航权限控制

```typescript
// AdminSidebar.tsx
const navItems = [
  { label: '仪表盘', href: '/admin', roles: ['admin', 'editor', 'viewer'] },
  { label: '短剧管理', href: '/admin/dramas', roles: ['admin', 'editor', 'viewer'] },
  { label: '用户管理', href: '/admin/users', roles: ['admin'] },
];

// 根据 user.role 过滤可见的导航项
const visibleItems = navItems.filter(item => item.roles.includes(user.role));
```

### 4.3 操作按钮权限控制

```tsx
// 短剧列表页
{user.role !== 'viewer' && (
  <Link href="/admin/dramas/new">新建短剧</Link>
)}

// 表格操作列
{user.role !== 'viewer' && (
  <>
    <Link href={`/admin/dramas/${drama.id}/edit`}>编辑</Link>
    <button onClick={() => handleDelete(drama.id)}>删除</button>
  </>
)}
```

---

## 5. UI 设计

### 5.1 设计风格

- **配色**：主色蓝色（#2563EB），中性灰色系背景和边框
- **卡片**：白色背景、浅灰边框（1px solid #e5e7eb）、小圆角（8px）
- **表格**：简洁线条，hover 行高亮，操作按钮使用文字链接
- **按钮**：填充主色（主要操作）、边框+文字（次要操作）、红色文字（删除）
- **无需动画/过渡特效**，保持静态清晰

### 5.2 布局

```
┌──────────────────────────────────────────────────┐
│  Header Bar（Logo + 用户头像/退出）                  │
├──────────┬───────────────────────────────────────┤
│ 导航      │  内容区                                │
│          │                                       │
│ 仪表盘    │  ┌──────┐ ┌──────┐ ┌──────┐          │
│ 短剧管理  │  │短剧N │ │剧集N │ │用户N │          │
│ 用户管理  │  └──────┘ └──────┘ └──────┘          │
│ (admin)  │                                       │
│          │  [表格区域]                             │
│          │                                       │
└──────────┴───────────────────────────────────────┘
```

### 5.3 组件规格

| 组件 | 规格 |
|------|------|
| StatCard | 白色背景、浅灰边框、8px 圆角、标题 14px 灰色、数值 28px 粗体 |
| DataTable | 表头灰色背景、12px 字体、hover 行浅蓝背景、排序箭头 |
| 表单 | 字段垂直排列、label 上方、输入框 40px 高、错误提示红色 12px |
| 确认弹窗 | 居中 Modal、800px 宽 max、半透明遮罩、标题 + 正文 + 确认/取消按钮 |
| 空状态 | 居中图标 + 文字 + 新建按钮（viewer 无按钮） |
| 错误状态 | 居中图标 + 错误信息 + 重试按钮 |
| Loading | 表格/卡片骨架屏 |

---

## 6. API 客户端

```typescript
// web/src/features/admin/api/client.ts

class AdminApiClient {
  private baseUrl = '/api/admin';

  async login(email: string, password: string): Promise<AuthResponse> { ... }
  async logout(): Promise<void> { ... }
  async getStats(): Promise<AdminStats> { ... }
  async listDramas(page: number, pageSize: number): Promise<DramaListResponse> { ... }
  async createDrama(data: DramaFormData): Promise<Drama> { ... }
  async updateDrama(id: string, data: Partial<DramaFormData>): Promise<Drama> { ... }
  async deleteDrama(id: string): Promise<void> { ... }
  async listEpisodes(dramaId: string): Promise<Episode[]> { ... }
  async createEpisode(dramaId: string, data: EpisodeFormData): Promise<Episode> { ... }
  async updateEpisode(id: string, data: Partial<EpisodeFormData>): Promise<Episode> { ... }
  async deleteEpisode(id: string): Promise<void> { ... }
  async listUsers(page: number, pageSize: number): Promise<UserListResponse> { ... }
  async updateUserRole(userId: string, role: string): Promise<UserProfile> { ... }
}
```

所有请求自动携带 JWT Bearer token（通过 `@supabase/ssr` 的 session 管理）。

---

## 7. 错误处理

### 7.1 全局错误处理

```typescript
// 统一的 API 响应处理
async function handleApiResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const body = await response.json();
    switch (response.status) {
      case 401: redirectToLogin(); break;
      case 403: throw new ForbiddenError(body.message); break;
      case 404: throw new NotFoundError(body.message); break;
      case 409: throw new ConflictError(body.message); break;
      default: throw new ApiError(body.message);
    }
  }
  return (await response.json()).data;
}
```

### 7.2 错误展示

| 错误类型 | 展示方式 |
|---------|---------|
| 表单校验错误 | 字段下方内联红色提示 |
| 操作失败（新建/编辑/删除） | Toast 通知（右上角，3 秒自动消失） |
| 列表加载失败 | 表格区域居中错误提示 + 重试按钮 |
| 401 未登录 | 重定向到 `/admin/login` |
| 403 无权访问 | 页面居中显示「无权访问」 |

---

## 8. 测试策略

### 8.1 组件测试

| 测试对象 | 测试内容 |
|---------|---------|
| LoginForm | 正确凭据提交、错误凭据提示、空字段校验 |
| DramaForm | 必填校验、字段长度限制、提交 loading |
| DataTable | 空数据展示、数据渲染、分页交互 |
| AdminSidebar | 角色过滤导航项：admin 全显、editor 无用户管理、viewer 无新建按钮 |

### 8.2 集成测试

| 测试场景 | 测试内容 |
|---------|---------|
| 登录流程 | 输入凭据 → 点击登录 → 跳转仪表盘 |
| 权限控制 | viewer 登录 → 无新建按钮 → 无用户管理入口 |
| CRUD 流程 | 新建短剧 → 列表刷新 → 编辑 → 删除确认 |

---

## 9. 依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| `@supabase/supabase-js` | ^2.x | Supabase Auth 客户端 |
| `@supabase/ssr` | ^0.x | Next.js SSR session 管理 + middleware |
| React 19 | 已安装 | UI 框架 |
| Next.js 16 | 已安装 | App Router + Route Handlers |

---

## 10. 参考资料

| 文档 | 关键信息 |
|------|---------|
| `web/src/app/layout.tsx` | 现有主站 Layout |
| `web/src/app/page.tsx` | 现有首页 |
| `web/package.json` | 现有依赖 |