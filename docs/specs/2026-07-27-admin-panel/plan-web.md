# Web 实现计划：管理平台（Admin Panel）

> 创建日期：2026-07-27
> 对应设计：design-web.md

## 实现步骤

### Step 1：依赖安装与配置

**任务**：安装 Supabase 客户端 SDK 并配置环境变量

- [x] 安装 `@supabase/supabase-js` 和 `@supabase/ssr`（需用户同意）
- [x] 配置环境变量：`NEXT_PUBLIC_SUPABASE_URL`、`NEXT_PUBLIC_SUPABASE_ANON_KEY`
- [x] 创建 `web/src/lib/supabase.ts` — Supabase browser client 单例

**测试**：确认 Supabase client 可正常初始化 ✅ 已完成

---

### Step 2：Middleware 认证

**任务**：修改 `web/src/middleware.ts`

- [x] 新增 `/admin/*` 路由认证检查
- [x] 未登录 → 重定向到 `/admin/login`
- [x] 已登录访问 `/admin/login` → 重定向到 `/admin`
- [x] 使用 `@supabase/ssr` 的 `createServerClient` 管理 session

**测试**：手动测试登录/未登录的页面访问 ✅ 已完成

---

### Step 3：Admin Layout + 导航

**任务**：创建管理平台独立 Layout

- [x] `web/src/app/admin/layout.tsx` — AdminLayout 组件（Sidebar + Header + Content）
- [x] `web/src/features/admin/components/AdminSidebar.tsx` — 侧边导航（按 role 过滤）
- [x] `web/src/features/admin/components/AdminHeader.tsx` — 顶部 Header（Logo + 用户信息 + 退出）

**测试**：验证 admin/editor/viewer 角色看到不同的导航项 ✅ 已完成

---

### Step 4：Auth Hook + 登录页

**任务**：实现登录认证

- [x] `web/src/features/admin/hooks/useAuth.ts` — Auth 状态管理
- [x] `web/src/app/admin/login/page.tsx` — 登录页（居中卡片 + 表单）
- [x] 登录成功 → 跳转 `/admin`
- [x] 登录失败 → 显示错误提示

**测试**：`LoginForm` 组件测试 — 正确凭据、错误凭据、空字段 ✅ 已完成

---

### Step 5：API 客户端

**任务**：创建 Admin API 客户端

- [x] `web/src/features/admin/api/client.ts` — 封装所有 admin API 调用
- [x] 自动携带 JWT Bearer token
- [x] 统一错误处理（401 → 重定向登录，403 → 显示无权访问）

**测试**：Mock API 响应测试 ✅ 已完成

---

### Step 6：仪表盘页

**任务**：实现仪表盘

- [x] `web/src/app/admin/page.tsx` — 仪表盘页
- [x] `web/src/features/admin/components/StatCard.tsx` — 统计卡片组件
- [x] 调用 `GET /api/admin/stats` 获取数据
- [x] Loading 骨架屏 + Error 重试 + 空数据（显示 0）

**测试**：`StatCard` 组件测试 — 正常/loading/error 状态 ✅ 已完成

---

### Step 7：短剧管理

**任务**：实现短剧 CRUD 页面

- [x] `web/src/app/admin/dramas/page.tsx` — 短剧列表页（DataTable + 分页）
- [x] `web/src/app/admin/dramas/new/page.tsx` — 新建短剧页（DramaForm）
- [x] `web/src/app/admin/dramas/[id]/edit/page.tsx` — 编辑短剧页（预填 DramaForm）
- [x] `web/src/features/admin/components/DataTable.tsx` — 通用数据表格
- [x] `web/src/features/admin/components/DramaForm.tsx` — 短剧表单（校验 + 提交）
- [x] `web/src/features/admin/hooks/useDramas.ts` — 短剧数据 hook
- [x] 删除确认弹窗（级联删除提示）
- [x] 权限控制：viewer 无新建/编辑/删除按钮

**测试**：
- `DramaForm` 组件测试 — 必填校验、长度限制、提交 loading
- `DataTable` 组件测试 — 空数据、数据渲染、分页
✅ 已完成

---

### Step 8：剧集管理

**任务**：实现剧集 CRUD 页面

- [x] `web/src/app/admin/dramas/[id]/episodes/page.tsx` — 剧集列表页
- [x] `web/src/app/admin/dramas/[id]/episodes/new/page.tsx` — 新建剧集页
- [x] `web/src/app/admin/dramas/[id]/episodes/[episodeId]/edit/page.tsx` — 编辑剧集页
- [x] `web/src/features/admin/components/EpisodeForm.tsx` — 剧集表单
- [x] `web/src/features/admin/hooks/useEpisodes.ts` — 剧集数据 hook

**测试**：`EpisodeForm` 组件测试 ✅ 已完成

---

### Step 9：用户管理

**任务**：实现用户列表 + 角色修改

- [x] `web/src/app/admin/users/page.tsx` — 用户列表页（DataTable + 分页）
- [x] `web/src/features/admin/hooks/useUsers.ts` — 用户数据 hook
- [x] 行内角色下拉选择（admin/editor/viewer）
- [x] 权限控制：仅 admin 可见此页面；editor/viewer 直接访问 URL 显示「无权访问」

**测试**：用户列表组件测试 ✅ 已完成

---

### Step 10：全局错误处理

**任务**：实现统一的错误处理和 Toast 通知

- [x] `web/src/app/admin/error.tsx` — Admin 错误边界
- [x] Toast 通知组件（操作成功/失败提示）
- [x] 网络异常 → 表格区域错误提示 + 重试按钮
- [x] 403 → 页面居中「无权访问」

---

## 验证清单

- [x] `npm run dev` 启动正常
- [x] 管理平台各页面可正常渲染
- [x] 登录流程完整（登录 → 仪表盘 → 登出）
- [x] 权限控制正确（admin/editor/viewer 看到不同 UI）
- [x] CRUD 操作完整（新建 → 列表 → 编辑 → 删除）
- [x] Loading/Empty/Error 状态覆盖
- [ ] `npm test` 全部通过
- [x] `npm run build` 编译通过