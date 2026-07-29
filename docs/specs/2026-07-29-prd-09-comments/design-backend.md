# Backend 端技术方案：PRD-09 评论系统

> 创建日期：2026-07-29
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

本期 Backend 评论能力遵循 `backend/CLAUDE.md` 的四层结构：**Route → Service → Repository → Infrastructure + Shared**。评论能力不并入现有 `DramaRepositoryInterface`，而是新增独立的 comments 资源域，避免把评论查询、评论创建、点赞切换、用户态 liked 计算继续堆进现有 drama/feed/player 逻辑。

```text
GET /api/dramas/:id/comments
  -> Route Handler
     -> withErrorHandler
     -> 校验 path/query
     -> getOptionalUserId(request)
     -> CommentService.listByDrama(..., userId?)
        -> DramaRepository.findById(dramaId) / ensureDramaExists
        -> CommentRepository.listByDrama(..., userId?)
           -> MockCommentRepository / SupabaseCommentRepository
        -> CommentListResponseSchema.parse(result)
     -> 返回 { data, pagination }

POST /api/dramas/:id/comments
  -> Route Handler
     -> withErrorHandler
     -> 校验 path/body
     -> getAuthenticatedUserId(request)
     -> CommentService.createComment(..., userId)
        -> ensureDramaExists
        -> CommentRepository.create(...)
        -> CommentSchema.parse(result)
     -> 返回 Comment

POST /api/dramas/:id/comments/:commentId/like
  -> Route Handler
     -> withErrorHandler
     -> 校验 path
     -> getAuthenticatedUserId(request)
     -> CommentService.toggleLike(..., userId)
        -> ensureDramaExists
        -> CommentRepository.toggleLike(...)
        -> ToggleCommentLikeResponseSchema.parse(result)
     -> 返回 { comment_id, liked, like_count }
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `backend/src/app/api/dramas/route.ts` | 不变 | 首页 Feed 继续只负责 drama 列表，不混入 comments 子资源 |
| `backend/src/app/api/dramas/[id]/comments/route.ts` | 新增 | 承载评论列表与发表评论 |
| `backend/src/app/api/dramas/[id]/comments/[commentId]/like/route.ts` | 新增 | 承载点赞/取消点赞 toggle |
| `backend/src/services/drama/drama.service.ts` | 不变 / 复用 | 继续作为 drama 资源域服务，不扩展评论逻辑 |
| `backend/src/services/comment/comment.service.ts` | 新增 | 新增 comments service，封装 drama 校验、评论创建与点赞切换 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 不变 / 复用 | 只在 service 层通过 `findById` 复用 drama existence 校验 |
| `backend/src/repositories/interfaces/comment.repository.interface.ts` | 新增 | 定义评论查询、创建、toggle like 契约 |
| `backend/src/repositories/mock/` | 新增 | 增加 mock comments repository，支撑测试与默认运行模式 |
| `backend/src/repositories/supabase/` | 新增 | 增加 Supabase comments repository，承载真实持久化 |
| `backend/src/repositories/repository-registry.ts` | 修改 | 增加 comments repository 的 create/get/set/reset 管理 |
| `backend/src/lib/schemas.ts` | 修改 | 新增 comments schema 与请求/响应 contract |
| `backend/src/lib/config.ts` | 修改 | 增加 `comments.repository` 配置，支持 `mock|supabase` |
| `backend/src/middleware/auth.ts` | 复用 | 列表使用 `getOptionalUserId()`，写接口使用 `getAuthenticatedUserId()` |
| `backend/src/middleware/error-handler.ts` | 不变 | 继续输出 `{ error: { code, message } }` |
| `backend/supabase/migrations/*` | 新增 | 新建 comments/comment_likes 表与索引，不能修改现有 init migration |

### 1.2 设计原则

1. **独立资源域**：评论是 `dramas` 的子资源，但服务/仓储独立成模块。
2. **读写认证分离**：评论列表匿名可读，评论创建/点赞必须登录。
3. **错误 envelope 以真实代码为准**：成功响应返回业务对象；错误响应继续由 `withErrorHandler` 输出 `{ error: { code, message } }`。
4. **先兼容 skeleton auth**：本期写接口继续对齐 `getAuthenticatedUserId()` 语义，不借 PRD-09 顺带重构完整用户 JWT 链路。
5. **先落 contract，再落真实热排**：`sort=hot` 需要完整支持参数与响应，但首版可以与 `latest` 共用排序实现。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `backend/src/app/api/dramas/[id]/comments/route.ts` | 新增 | 新增 `GET` / `POST` comments route |
| `backend/src/app/api/dramas/[id]/comments/[commentId]/like/route.ts` | 新增 | 新增点赞 toggle route |
| `backend/src/services/comment/comment.service.ts` | 新增 | 封装 list/create/toggleLike 业务逻辑 |
| `backend/src/services/comment/comment.service.test.ts` | 新增 | 覆盖 service 层核心业务与边界行为 |
| `backend/src/repositories/interfaces/comment.repository.interface.ts` | 新增 | 评论仓储协议 |
| `backend/src/repositories/mock/comment.mock.repository.ts` | 新增 | mock 评论仓储实现 |
| `backend/src/repositories/supabase/comment.supabase.repository.ts` | 新增 | Supabase 评论仓储实现 |
| `backend/src/repositories/repository-registry.ts` | 修改 | 注入 comments repository 默认实现与 reset 能力 |
| `backend/src/lib/schemas.ts` | 修改 | 新增 `Comment*` schema |
| `backend/src/lib/config.ts` | 修改 | 新增 `comments.repository` 配置项 |
| `backend/src/lib/errors.ts` | 修改 | 增加 `COMMENT_NOT_FOUND` 错误码与工厂 |
| `backend/supabase/migrations/<timestamp>_add_comments_tables.sql` | 新增 | 创建 `comments` / `comment_likes` 表、索引、RLS、更新时间触发器 |
| `backend/src/app/api/__tests__/dramas-comments.test.ts` | 新增 | 覆盖评论列表/发评论/点赞路由 contract |
| `backend/src/repositories/__tests__/comment.mock.repository.test.ts` | 新增 | 覆盖 mock repository 排序、分页、toggle 幂等 |
| `backend/src/repositories/supabase/__tests__/comment.supabase.repository.test.ts` | 新增 | 覆盖 Supabase repository 查询与错误映射 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 增加 comments schema 测试 |

---

## 3. API 路由设计

### 3.1 路由注册

| 路由文件 | HTTP 方法 | URL 路径 | 中间件链 | 说明 |
|---------|----------|---------|---------|------|
| `backend/src/app/api/dramas/[id]/comments/route.ts` | `GET` | `/api/dramas/:id/comments` | `withErrorHandler` + Route 内 Zod path/query 校验 | 返回评论分页列表，匿名可读 |
| `backend/src/app/api/dramas/[id]/comments/route.ts` | `POST` | `/api/dramas/:id/comments` | `withErrorHandler` + Route 内 path/body 校验 + `getAuthenticatedUserId()` | 创建评论 |
| `backend/src/app/api/dramas/[id]/comments/[commentId]/like/route.ts` | `POST` | `/api/dramas/:id/comments/:commentId/like` | `withErrorHandler` + Route 内 path 校验 + `getAuthenticatedUserId()` | 点赞/取消点赞 toggle |

### 3.2 路由分组策略

- 评论能力作为 `dramas` 的子资源，继续挂在 `app/api/dramas/[id]/comments/**`，不新建顶级 `/api/comments`。
- `GET` 与 `POST` 共享同一路径，分别承载列表与创建，符合当前 App Router Route Handlers 的实现习惯。
- 点赞使用成员操作子路径 `/:commentId/like`，与本期 spec 中的固定 contract 对齐。

### 3.3 参数校验

```ts
import { z } from 'zod';

export const DramaCommentPathSchema = z.object({
  id: z.string().uuid(),
});

export const DramaCommentLikePathSchema = z.object({
  id: z.string().uuid(),
  commentId: z.string().uuid(),
});

export const CommentListQuerySchema = z.object({
  page: z.coerce.number().int().min(1).default(1),
  pageSize: z.coerce.number().int().min(1).max(50).default(20),
  sort: z.enum(['latest', 'hot']).default('latest'),
});

export const CreateCommentRequestSchema = z.object({
  content: z.string().trim().min(1).max(500),
});
```

| 参数 | 规则 | 说明 |
|------|------|------|
| `id` | UUID | dramaId |
| `commentId` | UUID | 目标评论 ID |
| `page` | `int >= 1` | 默认 1 |
| `pageSize` | `1 <= int <= 50` | 默认 20 |
| `sort` | `latest \| hot` | 默认 `latest` |
| `content` | `trim().length in 1..500` | 纯文本评论正文 |

### 3.4 响应契约

#### `GET /api/dramas/:id/comments`

```json
{
  "data": [
    {
      "id": "comment_uuid",
      "drama_id": "drama_uuid",
      "content": "评论正文",
      "like_count": 12,
      "liked": false,
      "created_at": "2026-07-29T09:30:00.000Z",
      "updated_at": "2026-07-29T09:30:00.000Z",
      "user": {
        "id": "user_uuid",
        "display_name": "用户昵称",
        "avatar_url": null
      }
    }
  ],
  "pagination": {
    "page": 1,
    "page_size": 20,
    "total": 36,
    "total_pages": 2
  }
}
```

#### `POST /api/dramas/:id/comments`

```json
{
  "id": "comment_uuid",
  "drama_id": "drama_uuid",
  "content": "评论正文",
  "like_count": 0,
  "liked": false,
  "created_at": "2026-07-29T09:30:00.000Z",
  "updated_at": "2026-07-29T09:30:00.000Z",
  "user": {
    "id": "user_uuid",
    "display_name": "用户昵称",
    "avatar_url": null
  }
}
```

#### `POST /api/dramas/:id/comments/:commentId/like`

```json
{
  "comment_id": "comment_uuid",
  "liked": true,
  "like_count": 13
}
```

#### 错误响应

继续沿用当前真实错误结构：

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed"
  }
}
```

---

## 4. Middleware 链设计

### 4.1 请求流水线

```text
请求
  -> withErrorHandler 包裹的 Route Handler
     -> path/query/body Zod 校验
     -> 读接口：getOptionalUserId(request)
     -> 写接口：getAuthenticatedUserId(request)
     -> CommentService
     -> Repository
     -> JSON 响应
```

### 4.2 Middleware 清单

| Middleware | 作用域 | 说明 |
|-----------|--------|------|
| `withErrorHandler` | 路由级 | 捕获 `AppError` 与 `ZodError` 并输出统一错误结构 |
| `getOptionalUserId` | 评论列表 route 内 | 解析匿名/登录态下的可选用户 ID，用于 `liked` 计算 |
| `getAuthenticatedUserId` | 写接口 route 内 | 无 userId 时抛 `UNAUTHORIZED` |
| `CORS` / `logger` | 全局已有链路 | 评论模块不新增专属 middleware |

### 4.3 错误传播方式

- Route 内参数错误由 Zod 抛出，交给 `withErrorHandler` 返回 `400 VALIDATION_ERROR`。
- Service 内业务错误统一抛 `AppError`：例如 `DRAMA_NOT_FOUND`、`COMMENT_NOT_FOUND`、`UNAUTHORIZED`。
- Repository 内 Supabase 异常转换为 `SERVICE_UNAVAILABLE` 或 `INTERNAL_ERROR`，不把底层错误直接泄露给客户端。

---

## 5. Service 层设计

### 5.1 Service 清单

| Service | 职责 | 输入 | 输出 | 依赖 |
|---------|------|------|------|------|
| `CommentService.listByDrama` | 校验 drama、读取评论列表、按可选用户态返回 liked | `dramaId, page, pageSize, sort, userId?` | `CommentListResponse` | `DramaRepositoryInterface`, `CommentRepositoryInterface` |
| `CommentService.createComment` | 校验 drama、创建评论、返回完整评论对象 | `dramaId, userId, content` | `Comment` | 同上 |
| `CommentService.toggleLike` | 校验 drama、校验 comment 归属、切换点赞状态 | `dramaId, commentId, userId` | `ToggleCommentLikeResponse` | 同上 |

### 5.2 事务边界

| 操作组合 | 事务隔离级别 | 回滚策略 |
|---------|------------|---------|
| 创建评论 | 单次 insert | insert 失败即整次失败 |
| 点赞 toggle | `comment_likes` insert/delete + `comments.like_count` 更新 | 需要在同一事务/同一 RPC 风格逻辑内保证原子性 |
| 列表查询 | 只读查询 | 无事务要求 |

### 5.3 业务异常定义

| 异常类型 | 触发条件 | HTTP 状态码 | 错误码 |
|---------|---------|-----------|--------|
| `Errors.dramaNotFound(dramaId)` | drama 不存在 | 404 | `DRAMA_NOT_FOUND` |
| `Errors.commentNotFound(commentId)` | comment 不存在或不属于该 drama | 404 | `COMMENT_NOT_FOUND` |
| `Errors.unauthorized()` | 匿名写操作 | 401 | `UNAUTHORIZED` |
| `Errors.invalidParams(...)` | 自定义校验补充失败 | 400 | `INVALID_PARAMS` |
| `Errors.serviceUnavailable('Supabase')` | Supabase 客户端异常或依赖不可用 | 503 | `SERVICE_UNAVAILABLE` |
| `Errors.internal(...)` | 仓储映射或非预期内部错误 | 500 | `INTERNAL_ERROR` |

### 5.4 Service 伪代码

```ts
export class CommentService {
  constructor(
    private readonly dramaRepository: DramaRepositoryInterface,
    private readonly commentRepository: CommentRepositoryInterface,
  ) {}

  async listByDrama(input: ListDramaCommentsInput): Promise<CommentListResponse> {
    await this.ensureDramaExists(input.dramaId);
    return this.commentRepository.listByDrama(input);
  }

  async createComment(input: CreateCommentInput): Promise<Comment> {
    await this.ensureDramaExists(input.dramaId);
    return this.commentRepository.create(input);
  }

  async toggleLike(input: ToggleCommentLikeInput): Promise<ToggleCommentLikeResponse> {
    await this.ensureDramaExists(input.dramaId);
    return this.commentRepository.toggleLike(input);
  }

  private async ensureDramaExists(dramaId: string): Promise<void> {
    const drama = await this.dramaRepository.findById(dramaId);
    if (!drama) {
      throw Errors.dramaNotFound(dramaId);
    }
  }
}
```

---

## 6. 数据库 Migration 计划

### 6.1 变更概述

| 表名 | 操作（新建/修改/删除） | 说明 |
|------|----------------------|------|
| `comments` | 新建 | 存储 drama 一级评论与聚合点赞数 |
| `comment_likes` | 新建 | 存储用户与评论点赞关系 |
| `profiles` | 不变 | 继续提供作者摘要 |
| `dramas` | 不变 | 继续作为评论父资源 |

### 6.2 DDL

```sql
CREATE TABLE IF NOT EXISTS public.comments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  drama_id UUID NOT NULL REFERENCES public.dramas(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  content TEXT NOT NULL,
  like_count INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT comments_content_length_check
    CHECK (char_length(btrim(content)) BETWEEN 1 AND 500),
  CONSTRAINT comments_like_count_non_negative_check
    CHECK (like_count >= 0)
);

CREATE TABLE IF NOT EXISTS public.comment_likes (
  comment_id UUID NOT NULL REFERENCES public.comments(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (comment_id, user_id)
);
```

### 6.3 字段详情

| 表名 | 字段 | 类型 | 约束 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `comments` | `id` | UUID | PK | `gen_random_uuid()` | 评论 ID |
| `comments` | `drama_id` | UUID | FK -> `dramas(id)` | — | 所属 drama |
| `comments` | `user_id` | UUID | FK -> `profiles(id)` | — | 评论作者 |
| `comments` | `content` | TEXT | `trim` 后 1~500 字 | — | 评论正文 |
| `comments` | `like_count` | INTEGER | `>= 0` | `0` | 聚合点赞数 |
| `comments` | `created_at` | TIMESTAMPTZ | not null | `now()` | 创建时间 |
| `comments` | `updated_at` | TIMESTAMPTZ | not null | `now()` | 更新时间 |
| `comment_likes` | `comment_id` | UUID | FK -> `comments(id)` | — | 目标评论 |
| `comment_likes` | `user_id` | UUID | FK -> `profiles(id)` | — | 点赞用户 |
| `comment_likes` | `created_at` | TIMESTAMPTZ | not null | `now()` | 点赞时间 |

### 6.4 索引策略

| 表名 | 索引名 | 类型（UNIQUE/INDEX） | 字段 | 用途 |
|------|--------|---------------------|------|------|
| `comments` | `idx_comments_drama_created_at` | INDEX | `(drama_id, created_at DESC)` | latest 排序 |
| `comments` | `idx_comments_drama_like_created_at` | INDEX | `(drama_id, like_count DESC, created_at DESC)` | hot 排序预留 |
| `comments` | `idx_comments_user_id` | INDEX | `(user_id)` | 作者维度查询预留 |
| `comment_likes` | `comment_likes_pkey` | UNIQUE | `(comment_id, user_id)` | 防重复点赞 |
| `comment_likes` | `idx_comment_likes_user_id` | INDEX | `(user_id)` | 用户维度 liked 计算 |

### 6.5 回滚策略

- 本期采用新增 migration，不修改已有 migration。
- 回滚时按逆序删除：先删 `comment_likes`，再删 `comments`。
- 如果已经有业务数据，不建议直接 destructive rollback，应通过新 migration 做兼容修复。

---

## 7. 后台任务/队列设计

### 7.1 任务清单

| 任务名称 | 触发条件 | 执行频率 | 队列/调度方式 | 重试策略 | 超时 |
|---------|---------|---------|-------------|---------|------|
| — | — | — | 本期无需后台任务或队列 | — | — |

### 7.2 说明

- 评论创建与点赞切换均为同步请求链路处理，不引入 MQ、异步 worker 或定时任务。
- `like_count` 直接在写链路中维护，不通过异步聚合补偿。

---

## 8. 配置与环境

| 配置项 | 环境变量 Key | 开发环境值 | 生产环境值 | 说明 |
|--------|-------------|----------|-----------|------|
| 评论仓储实现 | `COMMENTS_REPOSITORY` | `mock` 或 `supabase` | `supabase`（预期） | 控制 comments repository 默认实现 |
| Supabase URL | `SUPABASE_URL` | 本地 Supabase 地址 | 线上环境注入 | `comment.supabase.repository.ts` 依赖 |
| Supabase Service Role | `SUPABASE_SERVICE_ROLE_KEY` | 本地测试 key | 线上环境注入 | 服务端读写评论关系 |
| 服务端口 | `PORT` | 现有配置 | 现有配置 | 评论模块不单独监听端口 |

> ⚠️ 禁止硬编码任何常量。所有配置通过 `config` 模块读取。

### 8.1 `config` 草案

```ts
export const config = {
  ...,
  comments: {
    repository: process.env.COMMENTS_REPOSITORY ?? 'mock',
  },
} as const;
```

---

## 9. API 调用清单（调用外部服务）

| 外部服务 | API 端点 | 调用时机 | 超时 | 降级策略 |
|---------|---------|---------|------|---------|
| Supabase PostgREST / SQL | `comments` / `comment_likes` / `profiles` / `dramas` | 评论列表、创建评论、点赞切换 | 复用 Supabase client 默认 | 异常映射为 `SERVICE_UNAVAILABLE` 或回退到 mock（仅开发配置层） |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Backend 实现方式 |
|---------|---------------|-----------------|
| 评论承载方式 | 页面内抽屉 / sheet | Backend 只提供子资源 API，不暴露独立 comments 页面路由 |
| 首屏请求参数 | `page=1&pageSize=20&sort=latest` | Route query schema 提供默认值 |
| 热评参数兼容 | `sort=hot` 首版保留 contract | Repository 支持 `hot` 分支，可先回退为与 `latest` 相同排序 |
| 评论总数来源 | 使用 `pagination.total` | repository 在列表结果中统一返回总数 |
| 发表评论成功处理 | 返回完整新评论对象 | `create` 直接 join 用户摘要后返回完整 `Comment` |
| 点赞成功处理 | 返回局部更新结果 | `toggleLike` 返回 `{ comment_id, liked, like_count }` |
| 匿名可读、登录可写 | 列表可匿名读取；写接口要求登录 | GET 使用 `getOptionalUserId`，POST 使用 `getAuthenticatedUserId` |
| 登录恢复策略 | 不自动重放写操作 | Backend 不持久化 pending action；仅返回标准 401 |
| 错误隔离 | 评论错误不影响宿主页主内容 | 评论接口独立返回错误，客户端自行做局部错误态 |

---

## 11. 边界与错误处理

### 11.1 全局错误处理架构

| 层级 | 机制 | 说明 |
|------|------|------|
| Route | `withErrorHandler` | 统一转成 JSON 错误响应 |
| Service | `AppError` | 业务错误集中抛出 |
| Repository | 底层异常映射 | Supabase/network/error code 转换为 `AppError` |
| 日志 | `console.error` / 现有服务日志 | 首版沿用当前项目日志基线 |

### 11.2 错误码定义

| 业务错误码 | HTTP 状态码 | 说明 | 响应示例 |
|-----------|------------|------|---------|
| `VALIDATION_ERROR` | 400 | path/query/body 校验失败 | `{ "error": { "code": "VALIDATION_ERROR", "message": "Validation failed" } }` |
| `INVALID_PARAMS` | 400 | service 级补充参数约束失败 | 同上 |
| `UNAUTHORIZED` | 401 | 匿名执行写操作 | `{ "error": { "code": "UNAUTHORIZED", "message": "Authentication required" } }` |
| `DRAMA_NOT_FOUND` | 404 | drama 不存在 | `{ "error": { "code": "DRAMA_NOT_FOUND", "message": "Drama (...) not found" } }` |
| `COMMENT_NOT_FOUND` | 404 | comment 不存在或 drama/comment 不匹配 | `{ "error": { "code": "COMMENT_NOT_FOUND", "message": "Comment (...) not found" } }` |
| `INTERNAL_ERROR` | 500 | 非预期内部错误 | `{ "error": { "code": "INTERNAL_ERROR", "message": "Internal server error" } }` |
| `SERVICE_UNAVAILABLE` | 503 | Supabase 或外部存储不可用 | `{ "error": { "code": "SERVICE_UNAVAILABLE", "message": "Service unavailable: Supabase" } }` |

### 11.3 边界场景

| 场景 | 触发条件 | API 行为 | 说明 |
|------|---------|---------|------|
| drama 无评论 | `comments` 中无记录 | 返回 `200 + data=[] + pagination.total=0` | 客户端进入空态 |
| drama 不存在 | `findById(dramaId)` 为空 | 返回 404 `DRAMA_NOT_FOUND` | 明确不是空态 |
| `commentId` 不存在 | 目标评论缺失 | 返回 404 `COMMENT_NOT_FOUND` | 点赞不成功 |
| `commentId` 不属于该 drama | 路径 drama 与评论归属不一致 | 返回 404 `COMMENT_NOT_FOUND` | 避免跨 drama 操作 |
| 评论内容仅空白 | `trim()` 后为空 | 返回 400 `VALIDATION_ERROR` | Route/body schema 兜底 |
| 评论内容超长 | > 500 | 返回 400 `VALIDATION_ERROR` | Route + DB check 双重约束 |
| 匿名发表评论 | 无用户 header | 返回 401 `UNAUTHORIZED` | 客户端触发登录拦截 |
| 匿名点赞 | 无用户 header | 返回 401 `UNAUTHORIZED` | 同上 |
| 重复快速点赞 | 并发多次点击同一评论 | 以事务内最终 toggle 结果为准 | 客户端仍需单项节流 |
| Supabase 不可用 | client 报错/依赖不可达 | 返回 503 `SERVICE_UNAVAILABLE` | 不泄露原始 SQL/HTTP 细节 |

### 11.4 错误日志与监控

- route 层记录路径、method、dramaId、commentId、userId（如可得）等上下文。
- 不记录评论正文原文到错误日志，避免额外内容暴露。
- 首版不接入新增监控系统，沿用现有日志基线。

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| 单元测试 | `CommentService` 业务逻辑 | Vitest |
| 集成测试 | comments routes + middleware + schema | Vitest + Next route tests |
| Repository 测试 | mock/supabase repository 行为 | Vitest |
| Schema 测试 | Zod schema 成功/失败样例 | Vitest |

### 12.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 测试类型 |
|------|---------|------|---------|---------|
| B1 | 评论列表成功 | 合法 `dramaId` + 默认 query | 200，含 `data/pagination` | Route |
| B2 | 评论列表匿名读取 | 无用户 header | 200，`liked=false` | Route |
| B3 | 评论列表返回 404 | 不存在 `dramaId` | 404 `DRAMA_NOT_FOUND` | Route |
| B4 | 发表评论成功 | 登录 header + 合法正文 | 200，返回完整 Comment | Route |
| B5 | 评论正文空白 | 登录 header + `"   "` | 400 `VALIDATION_ERROR` | Route |
| B6 | 匿名发表评论 | 无登录 header | 401 `UNAUTHORIZED` | Route |
| B7 | 点赞成功切换为 liked=true | 合法 `commentId` | 200，`liked=true` 且计数+1 | Route/Repository |
| B8 | 点赞第二次切回 liked=false | 同一用户再次请求 | 200，`liked=false` 且计数-1 | Route/Repository |
| B9 | 点赞不存在评论 | 不存在 `commentId` | 404 `COMMENT_NOT_FOUND` | Route |
| B10 | mock repository latest 排序 | 多条评论不同时间 | 按 `created_at desc` 排序 | Repository |
| B11 | `sort=hot` 参数兼容 | 请求 `hot` | 200，schema 有效 | Route/Repository |
| B12 | Supabase 异常映射 | repository client throw | 503 `SERVICE_UNAVAILABLE` 或 500 | Repository |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| `CommentRepositoryInterface` | 手写 fake / mock class | Service 测试通过依赖注入 |
| `DramaRepositoryInterface` | 手写 fake / mock class | 只需覆盖 `findById` |
| Supabase client | mock client 对象 | Repository 测试不依赖真实 Supabase |
| Next Request | 现有 route test helper | 复用当前 `dramas.test.ts` 风格 |

---

## 13. 安全考虑

- **认证与授权**：写接口复用 `getAuthenticatedUserId()`；列表接口只暴露公开评论数据。
- **输入校验**：path/query/body 使用 Zod，数据库层再加 CHECK 约束。
- **敏感数据处理**：只返回 `user.id/display_name/avatar_url`，不返回 email 等敏感字段。
- **SQL 注入防护**：通过 Supabase query builder / 参数化查询实现，不拼接原始 SQL 字符串。
- **CSRF/XSS 防护**：Native 客户端主要走 token/header 模式；评论内容按纯文本存储与返回，不支持 HTML。

---

## 14. 性能考虑

- **预期 QPS**：首版面向低并发移动端链路，优先保证正确性与可测试性。
- **缓存策略**：Backend 不做额外缓存；评论数据直接查库。
- **数据库优化**：依赖 `comments(drama_id, created_at)` 与 `comments(drama_id, like_count, created_at)` 索引。
- **连接池配置**：复用现有 Supabase client，无新增连接池策略。
- **计数优化**：`like_count` 反规范化存于 `comments` 表，避免列表查询时实时聚合点赞数。

---

## 15. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| — | — | 本期不新增开源依赖 | 继续复用 Next.js / Supabase / Zod / Vitest 现有栈 |

---

## 16. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| `profiles` 与 comments join 数据不完整 | 评论列表/创建返回用户摘要失败 | 🟡 | 中 | repository 映射时提供默认昵称兜底，必要时抛内部错误并补测试 | 暂时回退为只返回最小用户摘要 |
| `sort=hot` 尚无真实热度算法 | 客户端看到 hot/latest 实际一致 | 🟡 | 高 | 在设计与测试中明确“首版 contract 完整、排序可回退” | 后续单独 PR 优化热排 |
| 点赞 toggle 并发导致计数不一致 | `like_count` 漂移 | 🔴 | 中 | 在 repository / SQL 中使用事务或单次原子更新 | 临时改为每次根据 `comment_likes` 实时回算 |
| 继续沿用 skeleton auth | 评论写接口登录态只是过渡基线 | 🟡 | 高 | spec 与 design 中明确本期范围，后续统一升级真实 JWT | 后续 PR 统一替换 auth helper |
| 默认 mock repository 与真实 Supabase 行为不一致 | 本地测试通过但集成偏差 | 🟡 | 中 | 同步补齐 supabase repository 测试与 route contract 测试 | 本地切换 `COMMENTS_REPOSITORY=supabase` 验证 |

---

## 17. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/features/comments/index.md` | 功能概述、当前状态 | 当前尚无 comments API、列表状态机与点赞链路 |
| `wiki/features/video-player/index.md` | 播放器评论入口现状 | 播放器评论入口仍是视觉占位 |
| `wiki/features/homepage-feed/index.md` | 首页评论入口现状 | 首页卡片仍无评论入口 |
| `wiki/features/auth/index.md` | 认证基线 | 当前业务写接口仍以 skeleton auth 为基线 |
| `wiki/architecture/overview.md` | 系统总览 | 评论是现有首页/播放器链路上的增量能力 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `backend/src/repositories/repository-registry.ts` | 当前 registry 仅覆盖 drama / episode / playback-history，comments 需独立接入 |
| `backend/src/lib/schemas.ts` | 现有 `PaginationSchema` 可直接复用于 comments list |
| `backend/src/lib/config.ts` | 已有 player repository 切换模式，可参考新增 comments.repository |
| `backend/src/middleware/auth.ts` | `getOptionalUserId()` / `getAuthenticatedUserId()` 是评论读写接口的认证基线 |
| `backend/src/middleware/error-handler.ts` | 真实错误 envelope 为 `{ error: { code, message } }` |
| `backend/src/services/player/player.service.ts` | service 层使用 constructor injection 与 `ensureDramaExists` 风格 |
| `backend/src/repositories/interfaces/playback-history.repository.interface.ts` | repository interface 风格参考 |
| `backend/src/repositories/supabase/playback-history.supabase.repository.ts` | Supabase repository 写法与错误映射参考 |
| `backend/src/app/api/__tests__/dramas.test.ts` | route contract 测试风格：成功看 `data/pagination`，错误看 `body.error.code` |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 现有 `findById` 可在 comments service 中复用做 drama existence 校验 |
| `backend/src/lib/errors.ts` | 现有错误码集合与 `Errors.*` 工厂 |
| `backend/supabase/migrations/00000000000001_init_tables.sql` | 当前只有 `dramas` / `episodes` / `profiles`，尚无 comments 相关表 |
