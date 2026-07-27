# Backend 实现计划：管理平台（Admin Panel）

> 创建日期：2026-07-27
> 对应设计：design-backend.md

## 实现步骤

### Step 1：Migration 文件

**任务**：创建 4 个 migration SQL 文件

- [ ] `backend/supabase/migrations/<ts>_add_role_to_profiles.sql` — profiles 表添加 role 列（enum: admin/editor/viewer，默认 viewer）
- [ ] `backend/supabase/migrations/<ts>_rename_episode_count.sql` — dramas 表 `total_episodes` 重命名为 `episode_count`
- [ ] `backend/supabase/migrations/<ts>_auth_hook_role_sync.sql` — Auth Hook function + trigger
- [ ] `backend/supabase/migrations/<ts>_enable_rls.sql` — dramas、episodes、profiles 表 RLS 策略

**测试**：`supabase migration up` 本地执行验证

---

### Step 2：Zod Schema 扩展

**任务**：在 `backend/src/lib/schemas.ts` 中新增 admin API 相关 Zod schema

- [ ] `AdminLoginRequestSchema` — `{ email, password }`
- [ ] `AdminStatsResponseSchema` — `{ total_dramas, total_episodes, total_users }`
- [ ] `AdminDramaCreateSchema` — 短剧创建字段
- [ ] `AdminDramaUpdateSchema` — `AdminDramaCreateSchema.partial()`
- [ ] `AdminEpisodeCreateSchema` — 剧集创建字段
- [ ] `AdminEpisodeUpdateSchema` — `AdminEpisodeCreateSchema.partial()`
- [ ] `AdminRoleUpdateSchema` — `{ role: enum }`
- [ ] `AdminUserListResponseSchema` — 用户列表响应

**测试**：`backend/src/lib/__tests__/schemas.test.ts` — 正常/边界/异常输入校验

---

### Step 3：Auth Middleware 升级

**任务**：修改 `backend/src/middleware/auth.ts`

- [ ] 新增 `verifyJwt()` 函数 — 使用 Supabase Admin Client 验证 JWT
- [ ] 新增 `requireRole(roles, handler)` 中间件 — 提取 role 并校验
- [ ] 保留现有 `requireAuth()` 骨架（用户端 API 继续使用）

**测试**：`backend/src/middleware/__tests__/auth.test.ts` — JWT 合法/过期/无 role/权限不足

---

### Step 4：Repository 层扩展

**任务**：扩展/新增 Supabase repository

- [ ] `DramaSupabaseRepository` 新增：`create()`, `update()`, `delete()`, `count()`
- [ ] `EpisodeSupabaseRepository` 新增：`create()`, `update()`, `delete()`, `count()`, `findByDramaId()`
- [ ] 新增 `UserSupabaseRepository`：`list()`, `findById()`, `updateRole()`, `count()`

**测试**：`backend/src/repositories/supabase/__tests__/` — CRUD 操作 + 级联删除

---

### Step 5：AdminService 实现

**任务**：新增 `backend/src/services/admin/admin.service.ts`

- [ ] `getStats()` — 统计查询
- [ ] `listDramas()` / `createDrama()` / `getDrama()` / `updateDrama()` / `deleteDrama()`
- [ ] `listEpisodes()` / `createEpisode()` / `updateEpisode()` / `deleteEpisode()`
- [ ] `listUsers()` / `updateUserRole()`
- [ ] `deleteDrama()` 级联删除逻辑（事务）
- [ ] `updateUserRole()` 自修改保护

**测试**：`backend/src/services/admin/__tests__/admin.service.test.ts` — 所有业务逻辑

---

### Step 6：Route Handlers 实现

**任务**：创建所有 `/api/admin/*` Route Handler

- [ ] `auth/login/route.ts` — POST
- [ ] `auth/logout/route.ts` — POST
- [ ] `stats/route.ts` — GET
- [ ] `dramas/route.ts` — GET (list) + POST (create)
- [ ] `dramas/[id]/route.ts` — GET + PUT + DELETE
- [ ] `dramas/[id]/episodes/route.ts` — GET + POST
- [ ] `episodes/[id]/route.ts` — PUT + DELETE
- [ ] `users/route.ts` — GET
- [ ] `users/[id]/role/route.ts` — PUT

**测试**：集成测试验证完整请求链路

---

### Step 7：错误码扩展

**任务**：在 `backend/src/lib/errors.ts` 中新增 admin 相关错误

- [ ] `INVALID_CREDENTIALS` — 401
- [ ] `FORBIDDEN` — 403
- [ ] `CANNOT_MODIFY_SELF` — 400

---

## 验证清单

- [ ] `npm test` 全部通过
- [ ] `npm run build` 编译通过
- [ ] `supabase migration up` 执行成功
- [ ] 手动测试：登录 → 获取 JWT → 调用 admin API → 验证 role 权限