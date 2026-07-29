# 实现计划：Backend — PRD-09 评论系统

> 创建日期：2026-07-29
> 对应技术方案：design-backend.md
> 对应需求：spec.md

## 概述

本期 Backend 需要在现有 `Route → Service → Repository → Shared` 四层结构上补齐评论列表、发表评论、点赞切换三条 RESTful 接口，并新增 comments 数据模型与 Supabase migration。

实现顺序采用轻量 TDD：先补测试锁定 contract 与边界，再按 `schema/contract → repository(mock) → repository(supabase)+migration → service → route+回归` 逐层收口，避免影响既有 `dramas`、`player`、`rankings` 能力。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 评论 query / body / path schema 解析默认值与合法参数 | `{}`、`{ page: "2", pageSize: "20", sort: "hot" }`、`{ content: "  hello " }`、合法 UUID path | query 默认解析为 `page=1/pageSize=20/sort=latest`；body 被 trim；path 解析成功 | 单元测试 | P0 |
| T-02 | 评论 schema 拦截非法参数 | `page=0`、`pageSize=51`、`sort=foo`、空白内容、超长内容、非法 UUID | 抛出校验错误，route 最终返回 `400 + VALIDATION_ERROR` | 单元测试 | P0 |
| T-03 | mock repository 返回 latest 排序分页结果 | 合法 `dramaId` + 多条不同时间评论 + `sort=latest` | 返回 `data/pagination`，按 `created_at desc` 排序 | 单元测试 | P0 |
| T-04 | mock repository 支持 `sort=hot`、空列表与大页码 | `sort=hot`、无评论 drama、`page=999` | `hot` 分支契约可用；空 drama 返回 `data=[]`；大页码保留合法分页元信息 | 单元测试 | P0 |
| T-05 | mock repository 创建评论并返回完整评论对象 | 合法 `dramaId + userId + content` | 返回完整 `Comment`，`like_count=0`、`liked=false`、作者摘要齐全 | 单元测试 | P0 |
| T-06 | mock repository 点赞 toggle 幂等切换 | 同一 `commentId + userId` 连续调用两次 | 第一次返回 `liked=true` 且计数 +1；第二次返回 `liked=false` 且计数 -1 | 单元测试 | P0 |
| T-07 | Supabase repository 正确映射查询 / 创建 / toggle 结果 | mock Supabase client 返回 comments、profiles、comment_likes 数据 | 结果能被 schema 解析；底层异常被转换为 `SERVICE_UNAVAILABLE` 或 `INTERNAL_ERROR` | 单元测试 | P0 |
| T-08 | CommentService 校验 drama/comment 边界并包装内部错误 | 不存在 `dramaId`、不匹配 `commentId`、repository 返回脏数据 | 分别返回 `DRAMA_NOT_FOUND`、`COMMENT_NOT_FOUND`、`INTERNAL_ERROR` | 单元测试 | P0 |
| T-09 | 评论 route 返回 canonical contract | `GET /api/dramas/:id/comments`、`POST /api/dramas/:id/comments`、`POST /api/dramas/:id/comments/:commentId/like` | 成功返回 `{ data, pagination }` / `Comment` / `{ comment_id, liked, like_count }` | 路由测试 | P0 |
| T-10 | 评论 route 处理未登录与非法参数 | 匿名 POST 评论、匿名点赞、非法 path/query/body | 分别返回 `401 + UNAUTHORIZED`、`400 + VALIDATION_ERROR` | 路由测试 | P0 |
| T-11 | 评论 route 处理不存在资源 | 不存在 `dramaId` 或 `commentId` | 返回 `404 + DRAMA_NOT_FOUND` 或 `404 + COMMENT_NOT_FOUND` | 路由测试 | P0 |
| T-12 | migration 可应用且 comments 表约束符合设计 | 本地 Supabase 环境 + 新 migration | `comments/comment_likes` 表、索引、RLS、CHECK 约束成功落地 | 集成验证 | P1 |

## 实现步骤

### Step 1：先补测试，锁定评论 contract 与核心边界

- **关联测试**：T-01、T-02、T-03、T-04、T-05、T-06、T-07、T-08、T-09、T-10、T-11
- **目标文件**：`backend/src/lib/__tests__/schemas.test.ts`、`backend/src/repositories/__tests__/comment.mock.repository.test.ts`、`backend/src/repositories/supabase/__tests__/comment.supabase.repository.test.ts`、`backend/src/services/comment/comment.service.test.ts`、`backend/src/app/api/__tests__/dramas-comments.test.ts`、`backend/src/lib/__tests__/config.test.ts`
- **实现内容**：
  1. 先新增 comments 相关测试，锁定 query/body/path 参数、响应 contract、错误码、匿名读写差异与分页语义。
  2. 在 mock repository 测试中固定 latest/hot 排序、空列表、大页码、创建评论、点赞 toggle 的预期行为，避免实现阶段出现排序和计数语义漂移。
  3. 在 Supabase repository 测试中提前固定 select 映射、错误转换、comment 不存在 / drama 不匹配处理，确保真实存储实现不会偏离 mock 契约。
  4. 在 service 与 route 测试中锁定 `DRAMA_NOT_FOUND`、`COMMENT_NOT_FOUND`、`UNAUTHORIZED`、`VALIDATION_ERROR` 的分层归属，避免 route 层直接堆业务逻辑。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/lib/__tests__/schemas.test.ts` ✅ 已完成
  - 运行 `cd backend && npm run test -- src/lib/__tests__/config.test.ts` ✅ 已完成
  - 运行 `cd backend && npm run test -- src/repositories/__tests__/comment.mock.repository.test.ts` ✅ 已完成
  - 运行 `cd backend && npm run test -- src/repositories/supabase/__tests__/comment.supabase.repository.test.ts` ✅ 已完成
  - 运行 `cd backend && npm run test -- src/services/comment/comment.service.test.ts` ✅ 已完成
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/dramas-comments.test.ts` ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 增加 comments schema 的正反向测试 |
| `backend/src/lib/__tests__/config.test.ts` | 修改 | 增加 `comments.repository` 默认值测试 |
| `backend/src/repositories/__tests__/comment.mock.repository.test.ts` | 新增 | 覆盖 mock repository 排序、分页、创建、toggle 核心行为 |
| `backend/src/repositories/supabase/__tests__/comment.supabase.repository.test.ts` | 新增 | 覆盖 Supabase repository 查询映射与错误转换 |
| `backend/src/services/comment/comment.service.test.ts` | 新增 | 覆盖 service 成功路径、not found 与内部错误包装 |
| `backend/src/app/api/__tests__/dramas-comments.test.ts` | 新增 | 覆盖 comments routes 的成功、401、400、404 场景 |

### Step 2：补齐 shared schema / contract、错误码与 repository 接口

- **关联测试**：T-01、T-02、T-08
- **目标文件**：`backend/src/lib/schemas.ts`、`backend/src/lib/errors.ts`、`backend/src/lib/config.ts`、`backend/src/repositories/interfaces/comment.repository.interface.ts`、`backend/src/repositories/repository-registry.ts`
- **实现内容**：
  1. 在 `schemas.ts` 中新增 `CommentUserSummarySchema`、`CommentSchema`、`CommentListQuerySchema`、`CreateCommentRequestSchema`、`CommentListResponseSchema`、`ToggleCommentLikeResponseSchema` 以及 comments path schema，并复用现有 `PaginationSchema`。
  2. 在 `errors.ts` 中补充 `COMMENT_NOT_FOUND`，统一评论域的 not found 语义，避免 route 或 repository 自造字符串错误。
  3. 在 `config.ts` 中新增 `comments.repository` 配置，默认允许 `mock`，为后续切换 `supabase` 预留真实入口。
  4. 新增 `CommentRepositoryInterface`，定义 `listByDrama`、`create`、`toggleLike` 等契约；同时在 `repository-registry.ts` 中接入 `get/set/reset` 能力，保持 route 层通过 registry 注入依赖。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/lib/__tests__/schemas.test.ts` ✅ 已完成
  - 运行 `cd backend && npm run test -- src/lib/__tests__/config.test.ts` ✅ 已完成
  - 运行 `cd backend && npm run test -- src/services/comment/comment.service.test.ts` ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增 comments query、path、实体、响应 schema |
| `backend/src/lib/errors.ts` | 修改 | 增加 `COMMENT_NOT_FOUND` 错误码与工厂 |
| `backend/src/lib/config.ts` | 修改 | 新增 `comments.repository` 配置项 |
| `backend/src/repositories/interfaces/comment.repository.interface.ts` | 新增 | 定义评论仓储输入输出契约 |
| `backend/src/repositories/repository-registry.ts` | 修改 | 接入 comment repository 的默认创建与注入能力 |

### Step 3：实现 mock repository，先闭合开发态读写链路

- **关联测试**：T-03、T-04、T-05、T-06
- **目标文件**：`backend/src/repositories/mock/comment.mock.repository.ts`、`backend/src/repositories/__tests__/comment.mock.repository.test.ts`
- **实现内容**：
  1. 在 `CommentMockRepository` 中建立 comments 与 comment_likes 的内存种子结构，返回与 design 中一致的 `Comment` 形态。
  2. 实现 `listByDrama`：支持 `latest` 排序、`hot` 参数分支、分页切片、空 drama、大页码，以及登录态下 `liked` 字段计算。
  3. 实现 `create`：写入新评论并返回完整作者摘要、时间戳和默认点赞态，确保首版发表评论成功后客户端可直接插入列表顶部。
  4. 实现 `toggleLike`：处理首次点赞、取消点赞、评论不存在和 drama/comment 不匹配，保证 `like_count` 不出现负数。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/repositories/__tests__/comment.mock.repository.test.ts` ✅ 已完成
  - 运行 `cd backend && npm run test -- src/services/comment/comment.service.test.ts` ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/repositories/mock/comment.mock.repository.ts` | 新增 | 实现开发态 comments 查询、创建与点赞 toggle |
| `backend/src/repositories/__tests__/comment.mock.repository.test.ts` | 新增 | 锁定 mock repository 排序、分页、创建、toggle 行为 |

### Step 4：实现 Supabase repository 与 migration，打通真实持久化契约

- **关联测试**：T-07、T-12
- **目标文件**：`backend/src/repositories/supabase/comment.supabase.repository.ts`、`backend/src/repositories/supabase/__tests__/comment.supabase.repository.test.ts`、`backend/supabase/migrations/20260729000100_add_comments_tables.sql`
- **实现内容**：
  1. 新增 comments migration，创建 `comments` 与 `comment_likes` 表、索引、RLS、内容长度与非负点赞数约束，并保持不修改历史 migration。
  2. 在 `CommentSupabaseRepository` 中实现 comments 查询、创建与点赞 toggle，复用 Supabase Admin Client，并将查询结果映射为 `CommentSchema` / `ToggleCommentLikeResponseSchema` 可接受的结构。
  3. 对 Supabase 错误做集中转换：网络/连接异常映射为 `SERVICE_UNAVAILABLE`，数据行缺失、脏结构或非预期错误映射为 `COMMENT_NOT_FOUND` 或 `INTERNAL_ERROR`。
  4. 确保 mock 与 supabase 在 `sort=hot`、空列表、toggle 结果、作者摘要字段上保持 contract 一致，即使首版 hot 排序仍可回退为与 latest 相同实现。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/repositories/supabase/__tests__/comment.supabase.repository.test.ts` ✅ 已完成
  - 运行 `cd backend && docker compose -f tests/docker-compose.yml up -d` ✅ 已完成
  - 运行 `cd backend && npx supabase db push` ⚠️ 已尝试，受本地 Supabase/历史 migration 环境限制未完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/repositories/supabase/comment.supabase.repository.ts` | 新增 | 实现真实存储下的 comments 查询、创建与点赞 toggle |
| `backend/src/repositories/supabase/__tests__/comment.supabase.repository.test.ts` | 新增 | 覆盖 Supabase repository 映射、异常转换与 contract |
| `backend/supabase/migrations/20260729000100_add_comments_tables.sql` | 新增 | 新建 comments / comment_likes 表、索引、RLS 与约束 |

### Step 5：实现 CommentService，统一编排 drama 校验与输出校验

- **关联测试**：T-08
- **目标文件**：`backend/src/services/comment/comment.service.ts`、`backend/src/services/comment/comment.service.test.ts`
- **实现内容**：
  1. 新增 `CommentService`，通过构造注入 `DramaRepositoryInterface` 与 `CommentRepositoryInterface`，保持与现有 `DramaService`、`PlayerService` 一致的分层风格。
  2. 在 `listByDrama`、`createComment`、`toggleLike` 中统一先校验 drama 是否存在，再调用 repository，避免每个 route 单独查询 drama。
  3. 在 `toggleLike` 中把 comment 不存在或 drama/comment 不匹配统一收口为 `COMMENT_NOT_FOUND`；对 repository 返回的脏结构统一包装为 `Errors.internal(...)`。
  4. 所有 service 输出都通过 Zod schema 再次 parse，确保 route 层永远拿到 canonical contract。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/services/comment/comment.service.test.ts` ✅ 已完成
  - 运行 `cd backend && npm run test -- src/repositories/__tests__/comment.mock.repository.test.ts` ✅ 已完成
  - 运行 `cd backend && npm run test -- src/repositories/supabase/__tests__/comment.supabase.repository.test.ts` ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/services/comment/comment.service.ts` | 新增 | 统一评论列表、发表评论、点赞 toggle 的业务编排 |
| `backend/src/services/comment/comment.service.test.ts` | 新增 | 覆盖 service 成功路径、not found 与 internal error 包装 |

### Step 6：实现 route 并完成 backend 回归验证

- **关联测试**：T-09、T-10、T-11、T-12
- **目标文件**：`backend/src/app/api/dramas/[id]/comments/route.ts`、`backend/src/app/api/dramas/[id]/comments/[commentId]/like/route.ts`、`backend/src/app/api/__tests__/dramas-comments.test.ts`
- **实现内容**：
  1. 新增 `GET/POST /api/dramas/[id]/comments` route，解析 path/query/body，读接口使用 `getOptionalUserId()`，写接口使用 `getAuthenticatedUserId()`，通过 registry 获取 comment repository 并调用 `CommentService`。
  2. 新增 `POST /api/dramas/[id]/comments/[commentId]/like` route，校验 path 后调用 `toggleLike`，成功返回 `{ comment_id, liked, like_count }`。
  3. 保持 route 层只负责参数解析、认证获取、service 调用与 `NextResponse.json(...)` 返回，不在 route 中直接实例化 mock repository 或下沉业务判断。
  4. 完成后执行 comments 定向测试、backend 全量测试、build、lint 与 migration 验证，确认新增 comments 模块不破坏既有 dramas/player/admin 接口。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/dramas-comments.test.ts` ✅ 已完成
  - 运行 `cd backend && npm run test -- src/services/comment/comment.service.test.ts` ✅ 已完成
  - 运行 `cd backend && npm run test -- src/repositories/__tests__/comment.mock.repository.test.ts` ✅ 已完成
  - 运行 `cd backend && npm run test -- src/repositories/supabase/__tests__/comment.supabase.repository.test.ts` ✅ 已完成
  - 运行 `cd backend && npm run test` ✅ 已完成
  - 运行 `cd backend && npm run build` ✅ 已完成
  - 运行 `cd backend && npm run lint` ✅ 已完成（存在既有 warnings，无 errors）
  - 运行 `cd backend && docker compose -f tests/docker-compose.yml up -d` ✅ 已完成
  - 运行 `cd backend && npx supabase db push` ⚠️ 已尝试，受本地 Supabase/历史 migration 环境限制未完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/app/api/dramas/[id]/comments/route.ts` | 新增 | 新增评论列表与发表评论接口 |
| `backend/src/app/api/dramas/[id]/comments/[commentId]/like/route.ts` | 新增 | 新增评论点赞 toggle 接口 |
| `backend/src/app/api/__tests__/dramas-comments.test.ts` | 新增 | 覆盖 comments routes 的 contract 与错误路径 |

## 依赖关系

```text
Step 1（先补测试）
  └──▶ Step 2（Schema / Contract / Registry）
          └──▶ Step 3（Mock Repository）
                  └──▶ Step 4（Supabase Repository + Migration）
                          └──▶ Step 5（Service）
                                  └──▶ Step 6（Route + 回归验证）
```

## 验证总览

- [x] 评论定向测试全部通过（`cd backend && npm run test -- src/app/api/__tests__/dramas-comments.test.ts`）
- [x] Repository 定向测试全部通过（`cd backend && npm run test -- src/repositories/__tests__/comment.mock.repository.test.ts`、`cd backend && npm run test -- src/repositories/supabase/__tests__/comment.supabase.repository.test.ts`）
- [x] Service 定向测试全部通过（`cd backend && npm run test -- src/services/comment/comment.service.test.ts`）
- [x] Backend 全量测试通过（`cd backend && npm run test`）
- [x] Build 成功（`cd backend && npm run build`）
- [x] 无新增 lint 错误（`cd backend && npm run lint`，存在既有 warnings，无 errors）
- [x] 评论列表支持默认分页、`latest/hot` 参数与空列表返回
- [x] 匿名只读、登录可写语义被自动化测试锁定
- [x] 点赞 toggle 两次调用能在 `liked=true/false` 间稳定切换，`like_count` 不出现负数
- [ ] 本地 Supabase 环境可用时 migration 可应用（`cd backend && docker compose -f tests/docker-compose.yml up -d`、`cd backend && npx supabase db push`；已尝试启动本地环境并执行 push，但被既有历史 migration/本地 CLI 环境阻塞）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增 comments path/query/body、实体、响应 schema |
| `backend/src/lib/errors.ts` | 修改 | 新增 `COMMENT_NOT_FOUND` 错误码 |
| `backend/src/lib/config.ts` | 修改 | 新增 `comments.repository` 配置 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 补齐 comments schema 正反向测试 |
| `backend/src/lib/__tests__/config.test.ts` | 修改 | 补齐 comments config 默认值测试 |
| `backend/src/repositories/interfaces/comment.repository.interface.ts` | 新增 | 定义评论仓储契约 |
| `backend/src/repositories/repository-registry.ts` | 修改 | 接入 comments repository registry |
| `backend/src/repositories/mock/comment.mock.repository.ts` | 新增 | 实现 mock comments 读写能力 |
| `backend/src/repositories/__tests__/comment.mock.repository.test.ts` | 新增 | 覆盖 mock repository 核心行为 |
| `backend/src/repositories/supabase/comment.supabase.repository.ts` | 新增 | 实现 Supabase comments 读写能力 |
| `backend/src/repositories/supabase/__tests__/comment.supabase.repository.test.ts` | 新增 | 覆盖 Supabase repository contract |
| `backend/src/services/comment/comment.service.ts` | 新增 | 评论业务编排与输出校验 |
| `backend/src/services/comment/comment.service.test.ts` | 新增 | 覆盖 service 成功路径与异常路径 |
| `backend/src/app/api/dramas/[id]/comments/route.ts` | 新增 | 评论列表与发表评论 route |
| `backend/src/app/api/dramas/[id]/comments/[commentId]/like/route.ts` | 新增 | 评论点赞 toggle route |
| `backend/src/app/api/__tests__/dramas-comments.test.ts` | 新增 | 覆盖 comments routes contract |
| `backend/supabase/migrations/20260729000100_add_comments_tables.sql` | 新增 | comments / comment_likes migration |
