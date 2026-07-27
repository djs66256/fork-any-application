# Wiki 收录报告：管理平台（Admin Panel）

> 收录日期：2026-07-27
> 对应需求：spec.md

## 收录内容

| wiki 文档 | 操作 | 变更章节 | 说明 |
|-----------|------|---------|------|
| wiki/features/admin-panel/index.md | 新建 | 全文 | 管理平台功能文档，含功能概述、入口路由、核心逻辑、多端实现、API 引用、状态管理、依赖关系、已知限制 |
| wiki/api/admin.md | 新建 | 全文 | Admin API 文档，收录 14 个管理端点 |
| wiki/features/index.md | 更新 | 功能列表 | 新增管理平台功能域 |
| wiki/api/index.md | 更新 | API 域列表 | 新增 Admin API 域 |

## 未收录内容

| 类型 | 说明 | 原因 |
|------|------|------|
| 架构文档 | 无新增架构专题 | 管理平台复用现有 Clean Architecture + Supabase 架构，无架构级变更 |
| 技术决策 | 无新增技术决策 | 管理平台沿用现有技术栈（Next.js、Supabase、Zod），无新选型决策 |

## 修订记录

- wiki/revision/2026-07-27-admin-panel.md 已创建

## 收录结论

- [x] Y 所有变更已同步到 wiki
- [ ] N 部分内容因信息不足未收录

## 变更文件清单（来自 git diff）

变更范围：`4a8cfdd..691a9c2`，共 47 个文件，2903 行新增，136 行删除。

### Backend（17 个文件）

| 文件 | 操作 |
|------|------|
| `backend/src/app/api/admin/dramas/[id]/episodes/route.ts` | 新增 |
| `backend/src/app/api/admin/dramas/[id]/route.ts` | 新增 |
| `backend/src/app/api/admin/dramas/route.ts` | 新增 |
| `backend/src/app/api/admin/episodes/[id]/route.ts` | 新增 |
| `backend/src/app/api/admin/users/[id]/role/route.ts` | 新增 |
| `backend/src/app/api/admin/users/route.ts` | 新增 |
| `backend/src/middleware/auth.ts` | 修改（新增 JWT 验证 + requireRole） |
| `backend/src/repositories/supabase/` | 修改/新增（扩展 CRUD 方法、新增 UserSupabaseRepository） |
| `backend/src/services/admin/admin.service.ts` | 新增 |
| `backend/src/lib/schemas.ts` | 修改（新增 admin Zod schemas） |
| `backend/supabase/migrations/` | 新增（4 个 migration 文件） |

### Web（30 个文件）

| 文件 | 操作 |
|------|------|
| `web/src/app/admin/` 下 11 个路由页面 | 新增 |
| `web/src/features/admin/` 下 35 个组件/hooks/api 文件 | 新增 |
| `web/src/middleware.ts` | 修改（新增 admin 路由认证） |

### Spec（3 个文件）

| 文件 | 操作 |
|------|------|
| `docs/specs/2026-07-27-admin-panel/plan-backend.md` | 修改 |
| `docs/specs/2026-07-27-admin-panel/plan-web.md` | 修改 |
| `docs/specs/2026-07-27-admin-panel/workflow.json` | 修改 |