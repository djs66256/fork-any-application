# 实现计划：Backend — PRD-05 排行体系

> 创建日期：2026-07-27
> 对应技术方案：design-backend.md
> 对应需求：spec.md

## 概述

本期 Backend 需要在现有 `DramaService + DramaRepository + Next.js Route Handler` 四层结构上补齐排行查询与预约写接口，支撑客户端的双层 Tab 切换、分页加载、预约操作和未登录拦截。

实现顺序采用轻量 TDD：先补测试锁定 contract，再按 `schema/interface → mock repository → service/route → migration/supabase` 收口，最后做回归验证，避免在首页 Feed 与搜索能力之外引入不受控回归。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 排行 query schema 解析默认值与合法参数 | `{}`、`{ type: "booking", contentType: "ai", page: "2", pageSize: "20" }` | 默认解析为 `hot/all/1/10`，合法输入被正确转换 | 单元测试 | P0 |
| T-02 | 排行 query schema 拦截非法参数 | `type=foo`、`contentType=bar`、`page=0`、`pageSize=101` | 抛出校验错误，route 最终返回 `400 + VALIDATION_ERROR` | 单元测试 | P0 |
| T-03 | mock repository 按内容类型筛选并按热榜降序排序 | `contentType=live_action&type=hot&page=1&pageSize=10` | 只返回真人内容，且 `play_count` 降序 | 单元测试 | P0 |
| T-04 | mock repository 支持推荐榜 / 预约榜排序与分页边界 | `type=recommend`、`type=booking`、`page=999` | 分别按 `recommendation_score` / `booking_count` 降序；超大页码返回 `data=[]` 且分页元信息正确 | 单元测试 | P0 |
| T-05 | 排行 route 返回 canonical 响应并覆盖空榜单 | `GET /api/dramas/rankings?contentType=ai&type=booking` 等组合 | `200 + { data, pagination }`；空结果返回 `200 + data=[]` | 路由测试 | P0 |
| T-06 | 排行 service 对非法 repository 输出做内部错误包装 | repository 返回缺失排行字段或非法分页结构 | 返回 `500 + INTERNAL_ERROR`，不把内部问题误报成 400 | 单元测试 | P0 |
| T-07 | 预约 route 对未登录请求做拦截 | `POST /api/dramas/:id/book`，无 `Authorization` header | 返回 `401 + UNAUTHORIZED` | 路由测试 | P0 |
| T-08 | 预约 service / repository 支持成功预约与幂等重复预约 | 同一 `userId` 对同一 `dramaId` 连续调用两次 | 两次都返回 `booked=true`；`booking_count` 只增加一次 | 单元测试 | P0 |
| T-09 | 预约接口处理不存在资源 | `POST /api/dramas/:id/book`，`id` 不存在 | 返回 `404 + NOT_FOUND` | 路由测试 | P0 |
| T-10 | 排行列表返回登录态预约信息 | `GET /api/dramas/rankings` 携带已预约用户上下文 | 对已预约项返回 `is_booked=true`；匿名态固定为 `false` | 单元测试 | P1 |
| T-11 | Supabase repository / migration 契约覆盖新增字段与唯一约束 | `content_type / booking_count / recommendation_score` 字段、`bookings(user_id, drama_id)` | 查询映射与预约幂等契约成立；migration 可应用 | 单元测试 | P1 |

## 实现步骤

### Step 1：先补测试，锁定排行与预约的行为边界

- **关联测试**：T-01、T-02、T-03、T-04、T-05、T-06、T-07、T-08、T-09、T-10、T-11
- **目标文件**：`backend/src/lib/__tests__/schemas.test.ts`、`backend/src/repositories/__tests__/drama.mock.repository.test.ts`、`backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts`、`backend/src/services/drama/drama.service.test.ts`、`backend/src/app/api/__tests__/dramas-rankings.test.ts`、`backend/src/app/api/__tests__/dramas-book.test.ts`
- **实现内容**：
  1. 先新增排行与预约相关测试，覆盖 query 参数校验、排序/筛选/分页、空结果、大页码、未登录、幂等、资源不存在、内部错误包装等核心行为。
  2. 在 repository 测试中固定三类榜单排序规则和 `contentType` 过滤规则，避免后续实现出现排序字段混用。
  3. 在 route 测试中使用 `NextRequest` 固定 `GET /api/dramas/rankings` 与 `POST /api/dramas/:id/book` 的成功、400、401、404、500 语义。
  4. 在 Supabase repository 测试中先锁定字段映射与幂等写入契约，即使 coding 初期仍以 mock 为默认路径，也保证真实存储演进不脱节。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/lib/__tests__/schemas.test.ts`
  - 运行 `cd backend && npm run test -- src/repositories/__tests__/drama.mock.repository.test.ts`
  - 运行 `cd backend && npm run test -- src/repositories/supabase/__tests__/drama.supabase.repository.test.ts`
  - 运行 `cd backend && npm run test -- src/services/drama/drama.service.test.ts`
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/dramas-rankings.test.ts`
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/dramas-book.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 增加排行 query / response schema 的正反向测试 |
| `backend/src/repositories/__tests__/drama.mock.repository.test.ts` | 修改 | 增加排序、筛选、分页、预约幂等测试 |
| `backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts` | 修改 | 增加新增字段映射、预约唯一约束与兼容性测试 |
| `backend/src/services/drama/drama.service.test.ts` | 修改 | 增加排行编排、内部错误包装、预约成功/幂等/not found 测试 |
| `backend/src/app/api/__tests__/dramas-rankings.test.ts` | 新增 | 覆盖排行接口成功、空结果、非法参数、内部错误 |
| `backend/src/app/api/__tests__/dramas-book.test.ts` | 新增 | 覆盖预约接口未登录、成功、幂等、资源不存在、内部错误 |

### Step 2：补齐 shared schema 与 repository interface，先把 contract 收口

- **关联测试**：T-01、T-02、T-10
- **目标文件**：`backend/src/lib/schemas.ts`、`backend/src/repositories/interfaces/drama.repository.interface.ts`
- **实现内容**：
  1. 在 `schemas.ts` 中新增 `RankingTypeSchema`、`RankingContentTypeSchema`、`RankingQuerySchema`、`RankingDramaSchema`、`RankingListResponseSchema`、`BookDramaResponseSchema`。
  2. 在 repository interface 中新增 `RankingParams`、`RankingDrama`、`BookDramaParams`、`BookDramaResult` 以及 `listRankings`、`bookDrama` 等方法定义。
  3. 明确 `all` 仅作为查询条件，不进入实体存储值；`is_booked` 作为响应字段存在，匿名态默认 `false`。
  4. 保持成功响应继续沿用当前 Backend 资源体直出风格，不在本期引入新的统一 envelope。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/lib/__tests__/schemas.test.ts`
  - 运行 `cd backend && npm run test -- src/services/drama/drama.service.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增排行查询、排行列表、预约结果相关 schema |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 修改 | 扩展排行查询与预约写入契约 |

### Step 3：实现 mock 排行读链路，先闭合排序 / 筛选 / 分页能力

- **关联测试**：T-03、T-04、T-05、T-06、T-10
- **目标文件**：`backend/src/repositories/mock/drama.mock.repository.ts`、`backend/src/services/drama/drama.service.ts`、`backend/src/app/api/dramas/rankings/route.ts`
- **实现内容**：
  1. 扩展 mock drama 种子数据，为每条记录补齐 `content_type`、`play_count`、`booking_count`、`recommendation_score`，并预留按用户计算 `is_booked` 的能力。
  2. 在 `DramaMockRepository` 中实现 `listRankings(params, authContext?)`：先按 `contentType` 过滤，再按 `type` 对应字段排序，最后执行分页切片；超大页码返回空数组而不是异常。
  3. 在 `DramaService` 中新增 `listRankings`，统一做响应 schema 校验，并将 repository 非法输出包装成 `Errors.internal(...)`。
  4. 新增 `/api/dramas/rankings` route，解析 query 后调用 service，保持成功返回 `{ data, pagination }`，参数非法由 `withErrorHandler` 统一转成 `400 + VALIDATION_ERROR`。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/repositories/__tests__/drama.mock.repository.test.ts`
  - 运行 `cd backend && npm run test -- src/services/drama/drama.service.test.ts`
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/dramas-rankings.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 增加排行种子、三类排序、内容类型过滤、分页与 `is_booked` 计算 |
| `backend/src/services/drama/drama.service.ts` | 修改 | 新增排行查询 service，并统一做 schema parse / internal error 包装 |
| `backend/src/app/api/dramas/rankings/route.ts` | 新增 | 新增排行接口，解析 query 并返回 canonical 排行列表契约 |

### Step 4：实现预约写链路，补齐未登录拦截、幂等与不存在资源处理

- **关联测试**：T-07、T-08、T-09
- **目标文件**：`backend/src/middleware/auth.ts`、`backend/src/repositories/mock/drama.mock.repository.ts`、`backend/src/services/drama/drama.service.ts`、`backend/src/app/api/dramas/[id]/book/route.ts`
- **实现内容**：
  1. 复用现有 `requireAuth` 做 Bearer header 拦截，保证匿名请求稳定返回 `401 + UNAUTHORIZED`。
  2. 在 mock repository 中增加预约关系存储（如 `Map/Set`），确保同一 `userId + dramaId` 只计数一次，并在资源不存在时返回 `NOT_FOUND` 语义。
  3. 在 `DramaService.bookDrama` 中封装成功预约、重复预约幂等成功与资源不存在三类路径，并统一用 `BookDramaResponseSchema` 校验输出。
  4. 新增 `POST /api/dramas/:id/book` route；在当前 skeleton auth 下先闭合最小用户上下文注入方案，保证测试与幂等逻辑可验证，后续由 PRD-08 替换为真实用户解析。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/repositories/__tests__/drama.mock.repository.test.ts`
  - 运行 `cd backend && npm run test -- src/services/drama/drama.service.test.ts`
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/dramas-book.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/middleware/auth.ts` | 修改 | 如有必要，补充最小用户上下文透传能力，保持未登录拦截语义不变 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 增加预约关系存储、幂等更新与 not found 处理 |
| `backend/src/services/drama/drama.service.ts` | 修改 | 新增预约 service 方法与错误包装 |
| `backend/src/app/api/dramas/[id]/book/route.ts` | 新增 | 新增预约接口，接入 `requireAuth` 与 path param 校验 |

### Step 5：补齐 Supabase 演进与全量回归，确保实现可向真实存储收口

- **关联测试**：T-11
- **目标文件**：`backend/supabase/migrations/<timestamp>_add_ranking_fields.sql`、`backend/src/repositories/supabase/drama.supabase.repository.ts`、`backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts`
- **实现内容**：
  1. 新增 migration，为 `dramas` 表补齐 `content_type`、`booking_count`、`recommendation_score` 字段，并创建 `bookings` 表及 `(user_id, drama_id)` 唯一约束。
  2. 在 `DramaSupabaseRepository` 中补齐 `listRankings` 与 `bookDrama` 的真实存储契约，确保字段映射、筛选排序和幂等写入与 mock 行为一致。
  3. 保持首页 Feed 与搜索接口的现有字段映射不被破坏，避免 PRD-05 回归影响已存在能力。
  4. 完成后跑全量 backend 回归，并在本地 Supabase 环境可用时验证 migration 能正常应用。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/repositories/supabase/__tests__/drama.supabase.repository.test.ts`
  - 运行 `cd backend && npm run test`
  - 运行 `cd backend && npm run build`
  - 运行 `cd backend && npm run lint`
  - 运行 `cd backend && npx supabase db push`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/supabase/migrations/<timestamp>_add_ranking_fields.sql` | 新增 | 为排行字段与预约关系建模新增 migration |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 修改 | 实现真实存储下的排行查询与预约写入契约 |
| `backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts` | 修改 | 覆盖字段映射、幂等写入与兼容性验证 |

## 依赖关系

```text
Step 1（先补测试）
  └──▶ Step 2（Schema + Interface）
          ├──▶ Step 3（排行只读链路）
          └──▶ Step 4（预约写链路）
Step 3 ──▶ Step 5（Supabase + 全量回归）
Step 4 ──▶ Step 5（Supabase + 全量回归）
```

## 验证总览

- [ ] 排行与预约测试全部通过（`cd backend && npm run test`）
- [ ] Build 成功（`cd backend && npm run build`）
- [ ] 无新增 lint 错误（`cd backend && npm run lint`）
- [ ] 排行 query 参数合法值、默认值、非法值均被覆盖
- [ ] 排行排序 / 筛选 / 分页 / 大页码空结果行为被自动化测试锁定
- [ ] 预约接口未登录返回 `401 + UNAUTHORIZED`
- [ ] 重复预约保持幂等成功，不重复增加 `booking_count`
- [ ] 不存在资源返回 `404 + NOT_FOUND`
- [ ] 本地 Supabase 环境可用时 migration 可应用（`cd backend && npx supabase db push`）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增排行查询、排行实体、排行响应、预约响应 schema |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 补齐参数校验与响应 schema 测试 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 修改 | 扩展排行查询与预约写入契约 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 实现排行筛选/排序/分页与预约幂等逻辑 |
| `backend/src/repositories/__tests__/drama.mock.repository.test.ts` | 修改 | 覆盖排行与预约核心业务逻辑测试 |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 修改 | 补齐真实存储下的排行与预约能力 |
| `backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts` | 修改 | 覆盖 Supabase 契约与 migration 相关测试 |
| `backend/src/services/drama/drama.service.ts` | 修改 | 新增排行查询与预约 service，并统一做输出校验 |
| `backend/src/services/drama/drama.service.test.ts` | 修改 | 覆盖 service 成功路径与异常路径 |
| `backend/src/middleware/auth.ts` | 修改 | 如有必要，补充 skeleton auth 下的最小用户上下文能力 |
| `backend/src/app/api/dramas/rankings/route.ts` | 新增 | 新增排行只读接口 |
| `backend/src/app/api/dramas/[id]/book/route.ts` | 新增 | 新增预约写接口 |
| `backend/src/app/api/__tests__/dramas-rankings.test.ts` | 新增 | 排行 route 测试 |
| `backend/src/app/api/__tests__/dramas-book.test.ts` | 新增 | 预约 route 测试 |
| `backend/supabase/migrations/<timestamp>_add_ranking_fields.sql` | 新增 | 排行字段与 bookings 表 migration |