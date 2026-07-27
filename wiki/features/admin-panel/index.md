# 管理平台 (Admin Panel)

> 最后更新：2026-07-27
> 覆盖端：Web / Backend

## 功能概述

管理平台是面向运营人员的 Web 内部管理后台，提供短剧内容管理、剧集管理、用户角色分配和仪表盘概览功能。基于 RBAC（Role-Based Access Control）实现三级权限控制：超级管理员（admin）、内容编辑（editor）、查看者（viewer）。

- 核心价值：让运营人员无需编写代码即可通过图形界面管理短剧和剧集内容，管理员可以控制不同用户的操作权限
- 覆盖范围：Backend 管理 API（`/api/admin/*`）+ Web 管理平台 SPA（`/admin/*`）
- 当前状态：Backend + Web 已实现

## 入口与路由

| 端 | 入口 | 路由 | 源文件 |
|----|------|------|--------|
| Web | 浏览器访问 `/admin/login` | `/admin` 仪表盘、`/admin/dramas` 短剧管理、`/admin/users` 用户管理（仅 admin） | `web/src/app/admin/layout.tsx` |
| Backend | API 路由前缀 `/api/admin/*` | 14 个管理 API 端点 | `backend/src/app/api/admin/` |

Web 端管理平台使用独立 Layout（`AdminLayout`），不含主站导航和页脚，通过 `@supabase/ssr` 的 `createServerClient` 在 middleware 中实现认证拦截。

## 核心逻辑

### 流程：管理员登录

1. 访问 `/admin/login` → 展示登录页（居中卡片，邮箱 + 密码 + 登录按钮）
   - 源文件：`web/src/app/admin/login/page.tsx`
2. 前端调用 `supabase.auth.signInWithPassword` 进行认证
   - 源文件：`web/src/features/admin/hooks/useAuth.ts:65-66`
3. Supabase Auth 验证成功后返回 JWT，前端 session 自动管理
   - 源文件：`web/src/features/admin/hooks/useAuth.ts:29-34`
4. Web middleware 检查 `/admin/*` 路由的 session 状态，未登录重定向到 `/admin/login`
   - 源文件：`web/src/middleware.ts`
5. 登录失败 → 表单内显示错误信息（「邮箱或密码错误」）
   - 源文件：`web/src/features/admin/hooks/useAuth.ts:71-72`

### 流程：短剧 CRUD

1. 导航到 `/admin/dramas` → 表格展示短剧列表（封面、标题、分类、集数、评分、操作）
   - 源文件：`web/src/features/admin/components/DramaList.tsx`
2. 列表数据通过 `GET /api/admin/dramas?page=1&pageSize=20` 获取，需 JWT 认证
   - 源文件：`backend/src/app/api/admin/dramas/route.ts:7-23`
3. 新建短剧 → 表单包含 title（必填）、description、cover_url、category、tags、rating
   - 源文件：`web/src/features/admin/components/DramaForm.tsx`
4. 后端 Zod 校验表单数据 → AdminService → DramaSupabaseRepository
   - 源文件：`backend/src/app/api/admin/dramas/route.ts:25-43`
5. 删除短剧 → 确认弹窗提示级联删除 → DB 层 ON DELETE CASCADE 自动删除关联剧集
   - 源文件：`backend/src/services/admin/admin.service.ts:87-96`

### 流程：角色权限控制

角色权限在前后端双重实施：

- **Backend**：`requireRole` 中间件从 JWT `app_metadata.role` 提取角色并校验
  - 源文件：`backend/src/middleware/auth.ts:91-116`
- **Web**：`AdminSidebar` 根据当前用户 role 过滤导航项；各页面按 role 条件渲染操作按钮
  - 源文件：`web/src/features/admin/components/AdminSidebar.tsx:13-26`
- **Role 同步**：Auth Hook（Postgres trigger）在 `profiles.role` 更新时同步到 `auth.users.raw_app_meta_data.role`，使 JWT 包含当前角色
  - 源文件：`backend/supabase/migrations/`（auth hook migration）

#### 权限矩阵

| 操作 | admin | editor | viewer |
|------|-------|--------|--------|
| 查看仪表盘 | Y | Y | Y |
| 查看短剧列表 | Y | Y | Y |
| 新建/编辑/删除短剧 | Y | Y | N |
| 查看剧集列表 | Y | Y | Y |
| 新建/编辑/删除剧集 | Y | Y | N |
| 查看用户列表 | Y | N | N |
| 修改用户角色 | Y | N | N |

### 边界与异常处理

| 场景 | 处理方式 | 源文件 |
|------|---------|--------|
| 未登录访问管理页 | Web middleware 重定向到 `/admin/login` | `web/src/middleware.ts` |
| JWT 过期/无效 | `verifyJwt` 返回 null，`requireRole` 返回 401 | `backend/src/middleware/auth.ts:44-69,91-116` |
| viewer 调用写 API | `requireRole` 校验角色不匹配，返回 403 | `backend/src/middleware/auth.ts:103-108` |
| editor 访问用户管理 | 导航不显示入口；直接访问 API 返回 403 | `web/src/features/admin/components/AdminSidebar.tsx:16`；`backend/src/app/api/admin/users/route.ts:6-7` |
| admin 修改自己角色 | `AdminService.updateUserRole` 抛出 `CANNOT_MODIFY_SELF` 错误 | `backend/src/services/admin/admin.service.ts:178-180` |
| 删除短剧级联删除剧集 | DB 层 ON DELETE CASCADE 自动处理 | `backend/src/services/admin/admin.service.ts:93-95` |
| 剧集号冲突 | 同一短剧下相同 `episode_number`，DB unique constraint 触发 | `backend/supabase/migrations/`（episodes 表 unique 约束） |
| 网络异常（前端） | 表格区域显示错误提示 + 重试按钮 | `web/src/features/admin/components/DataTable.tsx` |
| 表单校验失败 | 后端 Zod 校验返回 400 + 字段级错误 | `backend/src/lib/schemas.ts`（admin schemas） |

## 多端实现

### Web

- 核心组件：`AdminLayout`（`AdminSidebar` + `AdminHeader`）、`DataTable`（通用表格）、`DramaForm`、`EpisodeForm`、`StatCard`、`ConfirmModal`、`Toast`
- 关键 hooks：`useAuth`（session 管理）、`useDramas`、`useEpisodes`、`useStats`、`useUsers`
- 与 Backend 交互方式：REST API，通过 `adminApi` 客户端封装（`web/src/features/admin/api/client.ts`），自动携带 Supabase JWT Bearer token
- 路由：Next.js App Router，`/admin/*` 子路由，独立 Layout（不含主站导航/页脚）
- 状态管理：React hooks + Supabase session（无全局状态库）
- 样式：CSS Modules + CSS 自定义属性

### Backend

- 核心模块：`backend/src/app/api/admin/`（Route Handlers）、`backend/src/services/admin/admin.service.ts`（AdminService）
- 数据模型：Drama、Episode、UserProfile（复用现有 Schema），新增 AdminStats、AdminRoleUpdate 等 Zod Schema
- 外部依赖：Supabase Auth（JWT 验证）、Supabase Database（数据持久化）
- 中间件链：`requireRole(roles, handler)` → `withErrorHandler(handler)` → Zod validation
- 分层架构：Route → Service → Repository（Supabase 实现）

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `POST /api/admin/auth/login` | [api/admin.md](../../api/admin.md) | 管理员登录 |
| `POST /api/admin/auth/logout` | [api/admin.md](../../api/admin.md) | 管理员登出 |
| `GET /api/admin/stats` | [api/admin.md](../../api/admin.md) | 仪表盘统计 |
| `GET /api/admin/dramas` | [api/admin.md](../../api/admin.md) | 短剧列表（分页） |
| `POST /api/admin/dramas` | [api/admin.md](../../api/admin.md) | 新建短剧 |
| `GET /api/admin/dramas/:id` | [api/admin.md](../../api/admin.md) | 短剧详情 |
| `PUT /api/admin/dramas/:id` | [api/admin.md](../../api/admin.md) | 编辑短剧 |
| `DELETE /api/admin/dramas/:id` | [api/admin.md](../../api/admin.md) | 删除短剧（级联删除剧集） |
| `GET /api/admin/dramas/:id/episodes` | [api/admin.md](../../api/admin.md) | 剧集列表 |
| `POST /api/admin/dramas/:id/episodes` | [api/admin.md](../../api/admin.md) | 新建剧集 |
| `PUT /api/admin/episodes/:id` | [api/admin.md](../../api/admin.md) | 编辑剧集 |
| `DELETE /api/admin/episodes/:id` | [api/admin.md](../../api/admin.md) | 删除剧集 |
| `GET /api/admin/users` | [api/admin.md](../../api/admin.md) | 用户列表（仅 admin） |
| `PUT /api/admin/users/:id/role` | [api/admin.md](../../api/admin.md) | 修改角色（仅 admin） |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Session / User | Supabase Auth session（`@supabase/ssr`） | 全局 | 登录状态、用户角色从 JWT `app_metadata.role` 获取 | `web/src/features/admin/hooks/useAuth.ts:29-34` |
| 短剧列表 | React useState（组件内） | 页面级 | 分页数据、loading、error | `web/src/features/admin/hooks/useDramas.ts` |
| 剧集列表 | React useState（组件内） | 页面级 | 按 dramaId 查询 | `web/src/features/admin/hooks/useEpisodes.ts` |
| 用户列表 | React useState（组件内） | 页面级 | 分页数据，仅 admin 可访问 | `web/src/features/admin/hooks/useUsers.ts` |
| 仪表盘统计 | React useState（组件内） | 页面级 | Stats 数据 | `web/src/features/admin/hooks/useStats.ts` |
| 表单状态 | React useState（组件内） | 页面级 | 表单值、校验错误、提交中 | `web/src/features/admin/components/DramaForm.tsx` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 数据模型 (Data Models) | Schema 复用 | 复用 Drama、Episode、UserProfile Zod Schema，新增 admin 专用 Schema | `backend/src/lib/schemas.ts` |
| Supabase 基础设施 | 双客户端复用 | 使用 `getSupabaseAdmin()` 验证 JWT，AdminService 使用 Supabase Repository | `backend/src/infrastructure/supabase.ts` |
| Auth 中间件 | 升级 | 从骨架 token 验证升级为 Supabase JWT 验证，新增 `requireRole` 中间件 | `backend/src/middleware/auth.ts` |
| Web 工程技术栈 | 路由扩展 | 在现有 Next.js 项目中新增 `/admin/*` 路由 | `web/src/app/admin/` |

### 外部依赖

| 服务 | 用途 | 接入方式 |
|------|------|---------|
| Supabase Auth | 管理员认证（JWT 签发与验证） | `@supabase/supabase-js` + `@supabase/ssr` |
| Supabase Database | 数据持久化（dramas、episodes、profiles） | Supabase Client SDK |
| Supabase RLS | 数据库行级安全策略 | SQL migration |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| 无批量导入/导出 | 运营人员需逐条录入内容 | 2026-07-27 | 远期迭代 |
| 无内容审核工作流 | 编辑发布的内容直接生效 | 2026-07-27 | 远期评估 |
| 无操作审计日志 | 无法追溯谁在何时做了什么操作 | 2026-07-27 | 远期评估 |
| 无移动端适配 | 管理平台仅面向桌面端浏览器 | 2026-07-27 | 设计决策 |
| 管理员账号需手动创建 | 无法在管理平台内注册新管理员 | 2026-07-27 | 首版通过 Supabase Dashboard 手动创建 |
| 无视频文件上传 | 视频 URL 需手动填写 | 2026-07-27 | 已有 CDN URL 方案 |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-27 | 初始创建，基于 PRD-15 管理平台全流程实现 |

---
*本文档由 llm-wiki skill 自动维护。*