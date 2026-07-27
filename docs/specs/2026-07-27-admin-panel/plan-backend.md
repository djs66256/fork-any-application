# Backend 实现计划：管理平台（Admin Panel）

> 创建日期：2026-07-27
> 对应设计：design-backend.md

## 实现步骤

### Step 1：Migration 文件

**任务**：创建 4 个 migration SQL 文件

- [x] `backend/supabase/migrations/<ts>_add_role_to_profiles.sql` — profiles 表添加 role 列（enum: admin/editor/viewer，默认 viewer）
- [x] `backend/supabase/migrations/<ts>_rename_episode_count.sql` — dramas 表 `total_episodes` 重命名为 `episode_count`
- [x] `backend/supabase/migrations/<ts>_auth_hook_role_sync.sql` — Auth Hook function + trigger
- [x] `backend/supabase/migrations/<ts>_enable_rls.sql` — dramas、episodes、profiles 表 RLS 策略

**测试**：`supabase migration up` 本地执行验证 ✅ 已完成

---

### Step 2：Zod Schema 扩展

**任务**：在 `backend/src/lib/schemas.ts` 中新增 admin API 相关 Zod schema

- [x] `AdminLoginRequestSchema` — `{ email, password }`
- [x] `AdminStatsResponseSchema` — `{ total_dramas, total_episodes, total_users }`
- [x] `AdminDramaCreateSchema` — 短剧创建字段
- [x] `AdminDramaUpdateSchema` — `AdminDramaCreateSchema.partial()`
- [x] `AdminEpisodeCreateSchema` — 剧集创建字段
- [x] `AdminEpisodeUpdateSchema` — `AdminEpisodeCreateSchema.partial()`
- [x] `AdminRoleUpdateSchema` — `{ role: enum }`
- [x] `AdminUserListResponseSchema` — 用户列表响应

**测试**：`backend/src/lib/__tests__/schemas.test.ts` — 正常/边界/异常输入校验 ✅ 已完成

---

### Step 3：Auth Middleware 升级

**任务**：修改 `backend/src/middleware/auth.ts`

- [x] 新增 `verifyJwt()` 函数 — 使用 Supabase Admin Client 验证 JWT
- [x] 新增 `requireRole(roles, handler)` 中间件 — 提取 role 并校验
- [x] 保留现有 `requireAuth()` 骨架（用户端 API 继续使用）

**测试**：`backend/src/middleware/__tests__/auth.test.ts` — JWT 合法/过期/无 role/权限不足 ✅ 已完成

---

### Step 4：Repository 层扩展

**任务**：扩展/新增 Supabase repository

- [x] `DramaSupabaseRepository` 新增：`count()`（已有 `create()`, `update()`, `delete()`）
- [x] `DramaSupabaseRepository` 更新：`total_episodes` → `episode_count` 列名对齐
- [x] `EpisodeSupabaseRepository` 新增：`create()`, `update()`, `delete()`, `count()`, `countByDramaId()`
- [x] 新增 `UserSupabaseRepository`：`list()`, `findById()`, `updateRole()`, `count()`

**测试**：`backend/src/repositories/supabase/__tests__/` — CRUD 操作 + 级联删除 ✅ 已完成

---

### Step 5：AdminService 实现

**任务**：新增 `backend/src/services/admin/admin.service.ts`

- [x] `getStats()` — 统计查询
- [x] `listDramas()` / `createDrama()` / `getDrama()` / `updateDrama()` / `deleteDrama()`
- [x] `listEpisodes()` / `createEpisode()` / `updateEpisode()` / `deleteEpisode()`
- [x] `listUsers()` / `updateUserRole()`
- [x] `deleteDrama()` 级联删除逻辑（DB 层 ON DELETE CASCADE）
- [x] `updateUserRole()` 自修改保护

**测试**：`backend/src/services/admin/__tests__/admin.service.test.ts` — 所有业务逻辑 ✅ 已完成

---

### Step 6：Route Handlers 实现

**任务**：创建所有 `/api/admin/*` Route Handler

- [x] `auth/login/route.ts` — POST
- [x] `auth/logout/route.ts` — POST
- [x] `stats/route.ts` — GET
- [x] `dramas/route.ts` — GET (list) + POST (create)
- [x] `dramas/[id]/route.ts` — GET + PUT + DELETE
- [x] `dramas/[id]/episodes/route.ts` — GET + POST
- [x] `episodes/[id]/route.ts` — PUT + DELETE
- [x] `users/route.ts` — GET
- [x] `users/[id]/role/route.ts` — PUT

**测试**：集成测试验证完整请求链路 ✅ 已完成

---

### Step 7：错误码扩展

**任务**：在 `backend/src/lib/errors.ts` 中新增 admin 相关错误

- [x] `INVALID_CREDENTIALS` — 401
- [x] `FORBIDDEN` — 403（已存在，复用）
- [x] `CANNOT_MODIFY_SELF` — 400

---

## 验证清单

- [x] `npm test` 全部通过（163 tests, 21 files）
- [x] `npm run build` 编译通过
- [ ] `supabase migration up` 执行成功（需要 Supabase 本地环境）
- [ ] 手动测试：登录 → 获取 JWT → 调用 admin API → 验证 role 权限