# Backend 端技术方案：PRD-05 排行体系

> 创建日期：2026-07-27
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

本期 Backend 端在现有 `dramas` 资源域之上扩展排行查询与预约写接口，继续遵守当前仓库已落地的四层结构：Route → Service → Repository → Infrastructure / Shared。首版仍以 mock repository 为默认运行路径，不引入新的第三方依赖；同时为了兼容当前已存在的 Supabase 实现，接口抽象与方案设计需要同步约束 mock / supabase 两套 repository 契约。

```text
GET /api/dramas/rankings
  -> Route Handler (`app/api/dramas/rankings/route.ts`)
     -> withErrorHandler
     -> Zod Query 校验（type / contentType / page / pageSize）
     -> DramaService.listRankings(params, authContext?)
        -> DramaRepository.listRankings(params, authContext?)
           -> Mock: 扩展 drama 种子 + 内存排序/分页
           -> Supabase: 基于 dramas 表排序查询，并兼容缺字段场景
        -> RankingListResponseSchema.parse(result)
     -> 返回 { data, pagination }

POST /api/dramas/[id]/book
  -> Route Handler (`app/api/dramas/[id]/book/route.ts`)
     -> withErrorHandler
     -> requireAuth
     -> Path param 校验（id UUID）
     -> DramaService.bookDrama({ dramaId, userId })
        -> DramaRepository.bookDrama({ dramaId, userId })
           -> Mock: 内存 booking 集合幂等写入 + booking_count 自增
           -> Supabase: 预留 dramas + bookings 持久化实现
        -> BookDramaResponseSchema.parse(result)
     -> 返回 { drama_id, booked, booking_count }
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `backend/src/app/api/dramas/route.ts` | 不变 | 继续服务首页 Feed，不被排行接口覆盖 |
| `backend/src/app/api/dramas/search/route.ts` | 不变 | 搜索发现继续走独立只读接口 |
| `backend/src/app/api/dramas/rankings/route.ts` | 新增 | 承载排行列表查询 |
| `backend/src/app/api/dramas/[id]/book/route.ts` | 新增 | 承载预约写接口 |
| `backend/src/services/drama/drama.service.ts` | 扩展 | 增加排行查询与预约能力，维持 drama 资源域集中编排 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 扩展 | 新增排行查询、预约写入相关参数与返回类型 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 扩展 | 在现有首页种子上补齐 `content_type`、统计字段与预约状态模拟 |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 扩展 | 兼容新接口抽象，并对真实表字段缺口提供落地说明 |
| `backend/src/lib/schemas.ts` | 扩展 | 新增排行 query / response / booking response schema |
| `backend/src/lib/errors.ts` | 不变 | 复用现有错误码集合，不新增枚举 |
| `backend/src/middleware/auth.ts` | 复用 | 预约接口沿用当前 `requireAuth` 骨架认证入口 |
| `backend/src/middleware/error-handler.ts` | 不变 | 继续作为统一错误出口 |
| `backend/src/lib/config.ts` | 不变 | 首版不新增排行专属环境变量 |

### 1.2 与 shared design / 代码现状的兼容说明

shared design 中给出了 `dramas` 扩展字段与未来 `bookings` 持久化方向，但当前代码与数据库现状并未完全具备对应能力，因此 Backend 方案以代码现状为基线，落地方式如下：

1. **成功响应 contract 兼容现有代码**  
   `design.md` 提到统一 `{ code, data, message }` 风格，但当前 Backend 真实实现与已有测试均采用“成功直接返回资源体、失败返回 `{ error: { code, message } }`”模式，例如 `GET /api/dramas`、`GET /api/dramas/search`。为避免破坏已存在客户端与测试，本期排行接口继续沿用当前成功响应结构：
   - 列表接口返回 `{ data, pagination }`
   - 预约接口返回 `{ drama_id, booked, booking_count }`
   - 错误接口继续由 `withErrorHandler` 输出 `{ error: { code, message } }`

2. **数据库字段以现有 migration 为准补充演进**  
   当前 `backend/supabase/migrations/00000000000001_init_tables.sql` 中 `dramas` 表已有 `play_count`，但尚无 `content_type`、`booking_count`、`recommendation_score`、`bookings` 表。方案中会明确：
   - coding 阶段需要新增 migration 扩表，而不是修改已执行 migration；
   - mock repository 在 migration 落地前先以内存种子满足接口；
   - Supabase repository 在字段未落地时应采用兼容实现，不阻断 TypeScript 接口闭合。

3. **认证能力以现有 skeleton 为准**  
   当前 `requireAuth` 只校验 `Authorization: Bearer ...` 头格式，并未真正解析 Supabase 用户身份。因此文档中将预约接口分为两层：
   - route 层先复用 skeleton 认证包装器，保证匿名请求返回 401；
   - service/repository 设计中预留 `userId` 注入位，待 PRD-08 登录能力落地后补齐真实用户解析与持久化。

4. **Service 边界不额外拆分新域服务**  
   排行与预约都围绕 `dramas` 资源展开，且当前服务层只有 `DramaService`。首版继续扩展 `DramaService`，不新增 `RankingService` / `BookingService`，避免过早拆分。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `backend/src/app/api/dramas/rankings/route.ts` | 新增 | 新增 `GET /api/dramas/rankings` 路由，解析 query 并调用 `DramaService.listRankings` |
| `backend/src/app/api/dramas/[id]/book/route.ts` | 新增 | 新增 `POST /api/dramas/:id/book` 路由，接入 `requireAuth` 并调用预约 service |
| `backend/src/services/drama/drama.service.ts` | 修改 | 增加排行列表查询、预约提交、内部 schema 校验与错误包装 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 修改 | 新增 `RankingParams`、`RankingDrama`、`BookDramaParams`、`BookDramaResult` 等契约 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 在现有 12 条首页种子上补充内容类型、热度值、预约数、推荐值与用户预约状态模拟 |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 修改 | 增加排行查询与预约能力，并兼容现有 `dramas` 表字段缺口 |
| `backend/src/lib/schemas.ts` | 修改 | 新增 `RankingTypeSchema`、`RankingContentTypeSchema`、`RankingQuerySchema`、`RankingDramaSchema`、`RankingListResponseSchema`、`BookDramaResponseSchema` |
| `backend/supabase/migrations/<timestamp>_add_ranking_fields.sql` | 新增 | 新建 migration，为 `dramas` 增加排行字段并创建 `bookings` 表；不能修改现有初始化 migration |
| `backend/src/app/api/__tests__/dramas-rankings.test.ts` | 新增 | 覆盖排行接口成功、非法参数、空列表、大页码、内部错误 |
| `backend/src/app/api/__tests__/dramas-book.test.ts` | 新增 | 覆盖预约接口未登录、成功、幂等、not found、内部错误 |
| `backend/src/services/drama/drama.service.test.ts` | 修改 | 增加排行与预约编排测试 |
| `backend/src/repositories/__tests__/drama.mock.repository.test.ts` | 修改 | 增加排行筛选/排序/分页/预约幂等测试 |
| `backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts` | 修改 | 增加排行查询与预约写入的 Supabase 契约测试 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 增加排行 query / response schema 测试 |

> 注：本期仅撰写方案，不修改上述实现文件；表中列出 coding 阶段需要落地的核心改动面。

---

## 3. API 路由设计

### 3.1 路由注册

| 路由文件 | HTTP 方法 | URL 路径 | 中间件链 | 说明 |
|---------|----------|---------|---------|------|
| `backend/src/app/api/dramas/rankings/route.ts` | `GET` | `/api/dramas/rankings` | `withErrorHandler` + Route 内 Zod query 校验 | 返回排行榜分页数据，匿名可访问 |
| `backend/src/app/api/dramas/[id]/book/route.ts` | `POST` | `/api/dramas/:id/book` | `withErrorHandler` + `requireAuth` + Route 内 path param 校验 | 提交预约，首版采用幂等成功语义 |
| `backend/src/app/api/dramas/route.ts` | `GET` | `/api/dramas` | `withErrorHandler` + Zod query 校验 | 现有首页 Feed，保持不变 |
| `backend/src/app/api/dramas/search/route.ts` | `GET` | `/api/dramas/search` | `withErrorHandler` + Zod query 校验 | 现有搜索接口，保持不变 |

### 3.2 路由分组策略

- 排行与预约都属于 `dramas` 资源域的派生能力，因此继续放在 `app/api/dramas/` 子树下，不新建 `/api/rankings` 顶级资源，也不引入版本前缀。
- 排行列表采用集合子路径 `/rankings`，表达“对 dramas 资源做特定排序视图查询”。
- 预约采用成员操作子路径 `/:id/book`，表达“对指定 drama 创建预约关系”。虽然更严格的 REST 可抽象为 `/api/bookings`，但当前仓库既有产品语义、shared design 与 PRD 均以 `POST /api/dramas/:id/book` 为准，本期保持一致。
- 首页 Feed、搜索、热搜、排行、预约共用 `DramaService` / `DramaRepositoryInterface`，避免 route 直接操作 mock 或 Supabase 客户端。

### 3.3 参数校验

```typescript
import { z } from 'zod';

export const RankingTypeSchema = z.enum(['hot', 'recommend', 'booking']);
export const RankingContentTypeSchema = z.enum(['all', 'live_action', 'ai']);

export const RankingQuerySchema = z.object({
  type: RankingTypeSchema.default('hot'),
  contentType: RankingContentTypeSchema.default('all'),
  page: z.coerce.number().int().min(1).default(1),
  pageSize: z.coerce.number().int().min(1).max(100).default(10),
});

export const DramaIdParamSchema = z.object({
  id: z.string().uuid(),
});
```

参数约束结论：

| 参数 | 规则 | 说明 |
|------|------|------|
| `type` | `hot | recommend | booking` | 榜单维度，默认 `hot` |
| `contentType` | `all | live_action | ai` | 内容类型维度，默认 `all` |
| `page` | `int >= 1` | 与现有 `/api/dramas` 保持一致 |
| `pageSize` | `1 <= int <= 100` | 默认 10，最大 100 |
| `id` | UUID | 预约接口目标短剧 ID |
| Request Body | 无 | 首版预约不接收 body |

### 3.4 响应契约

#### `GET /api/dramas/rankings`

成功响应：

```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "title": "逆袭归来后我成了豪门团宠",
      "description": "落魄千金重回豪门，在误会与守护中逆风翻盘。",
      "cover_url": "https://example.com/dramas/001.jpg",
      "category": "都市",
      "episode_count": 68,
      "tags": ["逆袭", "豪门"],
      "rating": 8.9,
      "created_at": "2026-07-25T00:00:00Z",
      "updated_at": "2026-07-25T00:00:00Z",
      "content_type": "live_action",
      "play_count": 98210,
      "booking_count": 820,
      "recommendation_score": 58930.6,
      "is_booked": false
    }
  ],
  "pagination": {
    "page": 1,
    "page_size": 10,
    "total": 12,
    "total_pages": 2
  }
}
```

#### `POST /api/dramas/:id/book`

成功响应：

```json
{
  "drama_id": "550e8400-e29b-41d4-a716-446655440001",
  "booked": true,
  "booking_count": 821
}
```

#### 错误响应

继续沿用当前 Backend 统一错误结构：

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed"
  }
}
```

### 3.5 排序与筛选规则

| 维度 | 规则 | Backend 落地方式 |
|------|------|-----------------|
| 内容类型筛选 | `all` / `live_action` / `ai` | repository 先按 `content_type` 过滤，`all` 不过滤 |
| 热榜 | `play_count` 降序 | 相同分值下回退到既有种子顺序或 `created_at desc` |
| 推荐榜 | `recommendation_score` 降序 | 分值由 Backend 生成并返回，客户端不计算 |
| 预约榜 | `booking_count` 降序 | 同分值下回退到热度或创建时间稳定排序 |
| 超大页码 | 合法但超范围 | 返回 `200 + data=[]`，保留正确分页信息 |
| 空榜单 | 过滤后无结果 | 返回 `200 + data=[]`，不返回 404 |

### 3.6 Zod Schema 与 design.md 对齐说明

```typescript
export const RankingDramaSchema = DramaSchema.extend({
  content_type: z.enum(['live_action', 'ai']),
  play_count: z.number().int().min(0),
  booking_count: z.number().int().min(0),
  recommendation_score: z.number().min(0),
  is_booked: z.boolean(),
});

export const RankingListResponseSchema = z.object({
  data: z.array(RankingDramaSchema),
  pagination: z.object({
    page: z.number().int().min(1),
    page_size: z.number().int().min(1),
    total: z.number().int().min(0),
    total_pages: z.number().int().min(0),
  }),
});

export const BookDramaResponseSchema = z.object({
  drama_id: z.string().uuid(),
  booked: z.literal(true),
  booking_count: z.number().int().min(0),
});
```

---

## 4. Middleware 链设计

### 4.1 请求流水线

```text
GET /api/dramas/rankings
请求
  -> withErrorHandler
  -> 解析 searchParams
  -> RankingQuerySchema.parse(...)
  -> DramaService.listRankings(...)
  -> DramaRepository.listRankings(...)
  -> RankingListResponseSchema.parse(...)
  -> JSON 响应

POST /api/dramas/:id/book
请求
  -> withErrorHandler
  -> requireAuth
  -> 解析 params.id
  -> DramaIdParamSchema.parse(...)
  -> DramaService.bookDrama(...)
  -> DramaRepository.bookDrama(...)
  -> BookDramaResponseSchema.parse(...)
  -> JSON 响应
```

### 4.2 Middleware 清单

| Middleware | 作用域 | 说明 |
|-----------|--------|------|
| `withErrorHandler` | 路由级 | 统一处理 `AppError`、`ZodError` 与未知异常 |
| `requireAuth` | `POST /api/dramas/:id/book` | 当前仅校验 Bearer header 结构，后续承接真实 Supabase Auth |
| `withLogger` | 暂不强制接入 | 仓库已有 logger middleware，但当前 API route 普遍未统一挂载；本期保持现状 |
| `withCors` | 暂不强制接入 | 已存在实现，但现有 route 未统一使用；本期不额外变更全局链路 |
| 限流 middleware | 不新增 | 当前仓库无统一限流链路，且用户未批准新增依赖 |

### 4.3 错误传播方式

- **参数错误**：Zod 在 route 中抛出异常，由 `withErrorHandler` 统一转成 `400 + VALIDATION_ERROR`。
- **业务错误**：service / repository 主动抛出 `AppError`，由 `withErrorHandler` 输出对应 HTTP 状态码与错误码。
- **未知异常**：`withErrorHandler` 记录 `console.error` 后返回 `500 + INTERNAL_ERROR`。
- **认证失败**：`requireAuth` 直接返回 `401 + { error: { code: 'UNAUTHORIZED', ... } }`，不进入 handler 主体。
- **内部数据契约错误**：service 在对 repository 结果做 schema 校验时，如果发现是服务端输出不合法，必须转换为 `Errors.internal(...)`，避免误报成客户端参数错误。

---

## 5. Service 层设计

### 5.1 Service 清单

| Service | 职责 | 输入 | 输出 | 依赖 |
|---------|------|------|------|------|
| `DramaService.listDramas` | 既有首页 Feed 查询 | `PaginationParams` | `PaginatedResult<Drama>` | `DramaRepositoryInterface` |
| `DramaService.searchDramas` | 既有搜索查询 | `SearchDramasParams` | `PaginatedResult<Drama>` | `DramaRepositoryInterface` |
| `DramaService.listHotSearches` | 既有热搜查询 | 无 | `HotSearchListResponse` | `DramaRepositoryInterface` |
| `DramaService.listRankings` | 按榜单维度查询排行 | `RankingParams` + 可选用户上下文 | `PaginatedResult<RankingDrama>` | `DramaRepositoryInterface` |
| `DramaService.bookDrama` | 为用户创建预约关系 | `BookDramaParams` | `BookDramaResult` | `DramaRepositoryInterface` |
| `DramaService.getDramaById` | 保持未实现 | `id` | `Drama` | `DramaRepositoryInterface` |
| `DramaService.createDrama` | 保持未实现 | `payload` | `Drama` | `DramaRepositoryInterface` |

### 5.2 事务边界

| 操作组合 | 事务隔离级别 | 回滚策略 |
|---------|------------|---------|
| 排行查询 | 不涉及事务 | 纯只读查询，无回滚 |
| mock 预约写入 | 内存原子更新 | 若任一步失败，保持原有 Map / Set 状态 |
| supabase 预约写入 | 单事务或单个 RPC/顺序写 | 需确保“创建 booking 关系 + 更新 booking_count”一致提交；失败则整体回滚 |

### 5.3 业务异常定义

| 异常类型 | 触发条件 | HTTP 状态码 | 错误码 |
|---------|---------|-----------|--------|
| 参数校验异常 | `type` / `contentType` / `page` / `pageSize` / `id` 非法 | 400 | `VALIDATION_ERROR` |
| 未登录预约 | 请求缺少 Bearer Token | 401 | `UNAUTHORIZED` |
| 数据不存在 | 预约目标短剧不存在 | 404 | `NOT_FOUND` |
| 幂等冲突 | 持久化层报告唯一约束冲突但状态可恢复 | 200 或 409 | 首版优先收敛为幂等成功；仅在无法确认状态时返回 `CONFLICT` |
| 依赖不可用 | Supabase / Redis 不可用 | 503 | `SERVICE_UNAVAILABLE` |
| 内部输出不合法 | repository 返回不满足 schema | 500 | `INTERNAL_ERROR` |
| 未知异常 | service / repository 运行时异常 | 500 | `INTERNAL_ERROR` |

### 5.4 Service 方法设计

#### `listRankings(params, authContext?)`

职责：
- 接收 route 已校验的排行查询参数；
- 调用 repository 执行筛选、排序、分页；
- 根据登录态补齐 `is_booked`；匿名态统一返回 `false`；
- 用 `RankingListResponseSchema` 校验输出；
- 保证超大页码和空榜单返回空数组而非异常。

实现约束：
- service 不重复计算分页结构，只负责编排与输出校验；
- `recommendation_score` 的具体计算由 repository / 数据层给出，service 不在端侧重算；
- 不在 service 层引入缓存、队列或新的依赖。

#### `bookDrama(params)`

职责：
- 校验用户上下文与目标资源存在性；
- 调用 repository 执行预约写入；
- 保证重复预约时返回幂等成功；
- 使用 `BookDramaResponseSchema` 校验输出。

实现约束：
- 首版不支持取消预约；
- 已预约再次调用时，返回当前最新 `booking_count` 与 `booked=true`；
- 若当前 skeleton auth 无法解析真实 `userId`，实现阶段应通过测试注入或占位 user context 先闭合服务签名，但在文档中必须标记为依赖 PRD-08 补齐。

### 5.5 为什么不拆 `RankingService`

- 当前后端领域边界按 `dramas` / `episodes` / `player` 组织，排行与预约均是 `dramas` 的派生查询和用户操作；
- 现有代码已经以 `DramaService` 聚合列表、搜索、热搜能力；
- PRD-05 首版目标是打通排行浏览和预约闭环，不是建设独立推荐/运营系统；
- 继续集中在 `DramaService` 有利于复用现有测试模式与 repository 抽象，避免引入额外复杂度。

---

## 6. 数据库 Migration 计划

### 6.1 变更概述

| 表名 | 操作（新建/修改/删除） | 说明 |
|------|----------------------|------|
| `dramas` | 修改 | 增加 `content_type`、`booking_count`、`recommendation_score`，并补齐排行查询索引 |
| `bookings` | 新建 | 记录用户与短剧的预约关系，承接 `is_booked` 与预约幂等 |

### 6.2 现状与兼容性

当前 `backend/supabase/migrations/00000000000001_init_tables.sql` 中：
- `dramas` 已存在 `play_count`，可直接复用为热榜排序依据；
- `dramas` 目前字段名仍为 `total_episodes`，由 `DramaSupabaseRepository` 映射到 canonical `episode_count`；
- 尚无 `content_type`、`booking_count`、`recommendation_score`；
- 尚无 `bookings` 表；
- 已启用 RLS，但策略仍是开发期宽松 authenticated 全开。

因此本期 coding 阶段必须新增 migration 文件，而不是修改现有初始化 migration。

### 6.3 DDL

```sql
-- Migration: add ranking fields and bookings table
ALTER TABLE dramas
  ADD COLUMN content_type TEXT NOT NULL DEFAULT 'live_action'
    CHECK (content_type IN ('live_action', 'ai')),
  ADD COLUMN booking_count INTEGER NOT NULL DEFAULT 0 CHECK (booking_count >= 0),
  ADD COLUMN recommendation_score NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (recommendation_score >= 0);

CREATE INDEX IF NOT EXISTS idx_dramas_content_type_created_at
  ON dramas(content_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_dramas_content_type_play_count
  ON dramas(content_type, play_count DESC);
CREATE INDEX IF NOT EXISTS idx_dramas_content_type_booking_count
  ON dramas(content_type, booking_count DESC);
CREATE INDEX IF NOT EXISTS idx_dramas_content_type_recommendation_score
  ON dramas(content_type, recommendation_score DESC);

CREATE TABLE IF NOT EXISTS bookings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  drama_id UUID NOT NULL REFERENCES dramas(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (user_id, drama_id)
);

CREATE INDEX IF NOT EXISTS idx_bookings_user_id_created_at
  ON bookings(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_bookings_drama_id
  ON bookings(drama_id);

ALTER TABLE bookings ENABLE ROW LEVEL SECURITY;
```

> `updated_at` 可复用现有 `update_updated_at_column()` trigger function，保持与 `dramas/episodes/profiles` 一致的更新时间维护方式。

### 6.4 字段详情

| 表名 | 字段 | 类型 | 约束 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `dramas` | `content_type` | `TEXT` | `NOT NULL` + 枚举检查 | `'live_action'` | 首版内容类型维度 |
| `dramas` | `play_count` | `INTEGER` | 已存在，`>=0` | `0` | 热榜排序依据，复用现有字段 |
| `dramas` | `booking_count` | `INTEGER` | `NOT NULL` + `>=0` | `0` | 预约榜排序依据 |
| `dramas` | `recommendation_score` | `NUMERIC(12,2)` | `NOT NULL` + `>=0` | `0` | 推荐榜排序与展示值 |
| `bookings` | `id` | `UUID` | 主键 | `gen_random_uuid()` | 主键 |
| `bookings` | `user_id` | `UUID` | `NOT NULL` + FK -> `auth.users(id)` | 无 | 预约用户 |
| `bookings` | `drama_id` | `UUID` | `NOT NULL` + FK -> `dramas(id)` | 无 | 预约短剧 |
| `bookings` | `created_at` | `TIMESTAMPTZ` | `NOT NULL` | `NOW()` | 创建时间 |
| `bookings` | `updated_at` | `TIMESTAMPTZ` | `NOT NULL` | `NOW()` | 更新时间 |

### 6.5 索引策略

| 表名 | 索引名 | 类型（UNIQUE/INDEX） | 字段 | 用途 |
|------|--------|---------------------|------|------|
| `dramas` | `idx_dramas_content_type_created_at` | INDEX | `content_type`, `created_at DESC` | 兼容回退排序与内容类型过滤 |
| `dramas` | `idx_dramas_content_type_play_count` | INDEX | `content_type`, `play_count DESC` | 热榜查询 |
| `dramas` | `idx_dramas_content_type_booking_count` | INDEX | `content_type`, `booking_count DESC` | 预约榜查询 |
| `dramas` | `idx_dramas_content_type_recommendation_score` | INDEX | `content_type`, `recommendation_score DESC` | 推荐榜查询 |
| `bookings` | `bookings_user_id_drama_id_key` | UNIQUE | `user_id`, `drama_id` | 预约幂等 |
| `bookings` | `idx_bookings_user_id_created_at` | INDEX | `user_id`, `created_at DESC` | 查询用户预约状态 |
| `bookings` | `idx_bookings_drama_id` | INDEX | `drama_id` | 统计/联结 drama 预约关系 |

### 6.6 回滚策略

- 回滚 migration 时，先删除依赖索引与 `bookings` 表，再删除 `dramas` 新增列；
- 若线上已有预约数据，回滚前需导出 `bookings`，避免用户关系丢失；
- coding 阶段如仍停留在 mock-only 实现，则功能回退可仅移除 route/service/repository/schema 代码，不影响现有首页与搜索接口。

---

## 7. 后台任务/队列设计

### 7.1 任务清单

| 任务名称 | 触发条件 | 执行频率 | 队列/调度方式 | 重试策略 | 超时 |
|---------|---------|---------|-------------|---------|------|
| 无 | 无 | 无 | 无 | 无 | 无 |

### 7.2 结论

首版不需要后台任务或队列，原因如下：

- 排行查询是同步只读排序与分页，不需要异步聚合；
- 推荐值在首版可以作为种子字段或写时预计算字段返回，不需要离线推荐任务；
- 预约写入是一条轻量同步写请求，可直接在请求链路内完成；
- 当前仓库虽然已有 Redis 技术约束，但没有统一队列基础设施，也不能在未经批准的前提下引入 Bull/BullMQ 等新依赖。

### 7.3 失败处理与死信队列

本期无队列，因此无死信队列设计。若后续需要引入“定时重算排行”“预约通知”等异步任务，可基于已存在的 Redis 基础设施做二期评估，并单独走依赖审批。

---

## 8. 配置与环境

| 配置项 | 环境变量 Key | 开发环境值 | 生产环境值 | 说明 |
|--------|-------------|----------|-----------|------|
| 应用名称 | `APP_NAME` | 现有配置 | 现有配置 | 无新增需求 |
| 应用版本 | `APP_VERSION` | 现有配置 | 现有配置 | 可用于通用日志 |
| 监听端口 | `PORT` | 现有配置 | 现有配置 | 不新增端口 |
| Supabase URL | `SUPABASE_URL` | 现有配置 | 现有配置 | Supabase repository 与未来 bookings 持久化依赖 |
| Supabase Anon Key | `SUPABASE_ANON_KEY` | 现有配置 | 现有配置 | 浏览类场景无需新增读取方式 |
| Supabase Service Role Key | `SUPABASE_SERVICE_ROLE_KEY` | 现有配置 | 现有配置 | 预约写入和后台管理查询可用 |
| Redis URL | `REDIS_URL` | 现有配置 | 现有配置 | 本期不启用新缓存或队列 |

关键结论：

- 首版 **不新增任何排行专属环境变量**；
- 所有配置继续通过 `backend/src/lib/config.ts` 读取；
- `config.redis.url` 现有默认值为 `redis://localhost:6379`，方案层只描述现状，不新增对 Redis 的实际运行依赖；
- 不允许硬编码环境地址、token、数据库连接信息。

---

## 9. API 调用清单（调用外部服务）

| 外部服务 | API 端点 | 调用时机 | 超时 | 降级策略 |
|---------|---------|---------|------|---------|
| Supabase Database | `dramas` / `bookings` 表查询写入 | 使用 `DramaSupabaseRepository` 时 | 复用 SDK 默认 / 调用侧超时控制 | 若 Supabase 不可用，返回 `SERVICE_UNAVAILABLE` 或回退到 mock（仅开发态） |
| Supabase Auth（未来） | Bearer Token 校验 / 用户解析 | 预约接口真实鉴权接入后 | 复用 SDK 默认 | 未接入前由 skeleton `requireAuth` 仅做 header 格式校验 |
| Redis | 无 | 本期不调用 | 无 | 无 |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Backend 实现方式 |
|---------|---------------|-----------------|
| 默认榜单 | `all + hot + page=1 + pageSize=10` | `RankingQuerySchema` 提供默认值；未传 query 时直接返回默认榜单第一页 |
| 一级/二级 Tab 切换 | 客户端保留另一维度并重置页码 | Backend 无状态，仅根据最新 query 返回对应列表 |
| 热榜展示字段 | 展示 `play_count` | response 返回 `play_count` |
| 推荐榜展示字段 | 展示 `recommendation_score`，由 Backend 统一计算 | response 返回 `recommendation_score`；客户端不计算 |
| 预约榜展示字段 | 展示 `booking_count` + 预约按钮 | response 返回 `booking_count`，登录态额外返回 `is_booked` |
| 空态策略 | 空榜单返回空数组 | 返回 `200 + { data: [], pagination }` |
| 大页码行为 | 超大页码不是错误 | repository 分页后允许空切片返回 |
| 预约幂等 | 重复预约成功，不重复加数 | repository 通过 `(user_id, drama_id)` 唯一关系或内存 Set 保证幂等 |
| 匿名浏览 | 排行页无需登录 | `GET /api/dramas/rankings` 不接 auth |
| 匿名预约 | 未登录走登录拦截 | `POST /api/dramas/:id/book` 返回 401，端侧映射登录拦截 |
| 兼容旧数据模型 | 不破坏首页/搜索消费方 | 通过新增 schema/实体承接排行字段，不改旧接口成功响应字段集 |

---

## 11. 边界与错误处理

### 11.1 全局错误处理架构

| 层级 | 机制 | 说明 |
|------|------|------|
| Route / Middleware | `withErrorHandler` | 统一把 `AppError` / `ZodError` 转为 JSON 错误结构 |
| Auth | `requireAuth` | 预约接口缺少 Bearer Token 时直接返回 401 |
| Service | `Errors.internal(...)` / `Errors.notFound(...)` 等 | 包装业务错误与内部契约错误 |
| Repository | 数据访问与数据转换 | 不直接返回 HTTP 响应，只抛领域错误 |
| 日志 | `console.error` + 可选 `withLogger` | 当前仓库现状，不新增监控依赖 |

### 11.2 错误码定义

| 业务错误码 | HTTP 状态码 | 说明 | 响应示例 |
|-----------|------------|------|---------|
| `VALIDATION_ERROR` | 400 | query/path 参数校验失败 | `{ "error": { "code": "VALIDATION_ERROR", "message": "Validation failed" } }` |
| `UNAUTHORIZED` | 401 | 未登录或缺少 Bearer Token | `{ "error": { "code": "UNAUTHORIZED", "message": "Authentication required" } }` |
| `FORBIDDEN` | 403 | 已登录但不允许操作（预留） | `{ "error": { "code": "FORBIDDEN", "message": "Access denied" } }` |
| `NOT_FOUND` | 404 | 预约目标短剧不存在 | `{ "error": { "code": "NOT_FOUND", "message": "Drama (<id>) not found" } }` |
| `CONFLICT` | 409 | 写入冲突且无法判定幂等结果（预留） | `{ "error": { "code": "CONFLICT", "message": "..." } }` |
| `TOO_MANY_REQUESTS` | 429 | 限流触发（预留） | `{ "error": { "code": "TOO_MANY_REQUESTS", "message": "Too many requests" } }` |
| `INTERNAL_ERROR` | 500 | 未知内部错误或内部输出不合法 | `{ "error": { "code": "INTERNAL_ERROR", "message": "Internal server error" } }` |
| `SERVICE_UNAVAILABLE` | 503 | Supabase/依赖服务不可用 | `{ "error": { "code": "SERVICE_UNAVAILABLE", "message": "Service unavailable: supabase" } }` |

### 11.3 边界场景

| 场景 | 触发条件 | API 行为 | 说明 |
|------|---------|---------|------|
| 空参数/缺参数 | `type` / `contentType` 非法，`id` 缺失或非法 | 返回 400 | route 层 Zod 兜底 |
| 参数边界值 | `page=0`、`pageSize=101` | 返回 400 | 与现有 `/api/dramas` 保持一致 |
| 超大页码 | `page` 合法但远超总页数 | 返回 200 + 空数组 | 不视为错误 |
| 空榜单 | 某内容类型 + 榜单组合无数据 | 返回 200 + 空数组 | 保持客户端空态可展示 |
| 匿名访问排行 | 无 token | 正常返回排行数据 | 浏览不依赖登录 |
| 匿名访问预约 | 无 Bearer Token | 返回 401 | 端侧据此触发登录拦截 |
| 目标短剧不存在 | `POST /book` 的 `id` 不存在 | 返回 404 | 不静默成功 |
| 重复预约 | 同一用户重复点击 | 返回 200，`booked=true`，计数不重复增加 | 幂等语义 |
| 并发预约 | 同一用户并发提交相同 drama | 只允许一次有效加数，其他请求返回幂等成功 | mock 与 supabase 实现都必须保证 |
| 封面缺失 | `cover_url = null` | 正常返回数据 | 客户端展示占位图 |
| shared design 与代码响应不一致 | 成功响应未使用 `{ code, data, message }` | 保持现有成功响应风格 | 明确以代码现状兼容落地 |

### 11.4 错误日志与监控

- 本期不新增日志或监控依赖；
- 未知异常继续由 `withErrorHandler` 统一 `console.error`；
- coding 阶段如接入 `withLogger`，只作为可选增强，不改变接口 contract；
- 预约失败场景建议记录最小必要上下文（route、dramaId、是否带 auth header），但不得打印敏感 token。

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| Route 测试 | `/api/dramas/rankings`、`/api/dramas/:id/book` 正常与异常响应 | Vitest + `NextRequest` |
| Service 单元测试 | 排行编排、schema 校验、预约幂等编排 | Vitest |
| Repository 单元测试 | mock / supabase 的筛选、排序、分页、预约行为 | Vitest |
| Schema 测试 | 排行 query / response / booking response schema 边界 | Vitest + Zod |
| Migration / repository 契约测试 | Supabase 字段映射、唯一约束与回退行为 | Vitest + mock Supabase client |

### 12.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 测试类型 |
|------|---------|------|---------|---------|
| B-01 | 默认热榜第一页 | `GET /api/dramas/rankings` | 200，`type=hot` 默认生效，返回分页结构 | Route |
| B-02 | 内容类型筛选 | `contentType=ai&type=hot` | 只返回 AI 内容，分页正确 | Route / Repository |
| B-03 | 推荐榜排序 | `type=recommend` | `recommendation_score` 按降序排列 | Repository / Service |
| B-04 | 预约榜排序 | `type=booking` | `booking_count` 按降序排列 | Repository |
| B-05 | 超大页码 | `page=999&pageSize=10` | 200，`data=[]`，pagination 保留总数 | Route / Repository |
| B-06 | 非法 query | `page=0` 或 `contentType=foo` | 400，`VALIDATION_ERROR` | Route |
| B-07 | 匿名访问预约 | 无 Authorization header | 401，`UNAUTHORIZED` | Route |
| B-08 | 成功预约 | `POST /api/dramas/:id/book` + Bearer token | 200，`booked=true`，`booking_count` 自增 | Route / Service / Repository |
| B-09 | 重复预约 | 同一用户重复请求同一 drama | 200，`booking_count` 不重复增加 | Service / Repository |
| B-10 | 不存在的 drama 预约 | 不存在 UUID | 404，`NOT_FOUND` | Service / Repository |
| B-11 | 内部 schema 错误 | repository 返回非法排行字段 | 500，`INTERNAL_ERROR` | Service |
| B-12 | Supabase 字段兼容 | 旧表缺少新增列时的兼容查询逻辑 | 不应破坏现有首页/搜索接口 | Repository |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| 排行数据源 | 扩展 `DramaMockRepository` 种子数据 | 默认开发与测试路径 |
| 用户预约状态 | mock repository 内存 `Map/Set` | 用于验证幂等与 `is_booked` |
| Supabase Client | `vi.mock('@/infrastructure/supabase')` | 复用现有 supabase repository 测试风格 |
| Route 请求 | `NextRequest` 直接构造 | 与现有 `/api/dramas`、`/api/dramas/search` 测试风格一致 |
| Auth header | 构造 `Authorization: Bearer test-token` | 适配当前 skeleton `requireAuth` |

### 12.4 测试落地要求

- 至少新增两个 route 级测试文件：排行接口、预约接口；
- `drama.service.test.ts` 必须补齐排行与预约测试，而不是只验证首页/搜索/热搜；
- `drama.mock.repository.test.ts` 必须覆盖三类排序规则、内容类型过滤、幂等预约；
- `drama.supabase.repository.test.ts` 必须覆盖新增字段映射与预约写入契约；
- 不新增端到端测试框架，继续使用仓库现有 `vitest` 体系。

---

## 13. 安全考虑

- **认证与授权**：
  - 排行浏览匿名可访问；
  - 预约必须登录，首版 route 层先通过 `requireAuth` 拦截；
  - 待 PRD-08 完成后，应将 Bearer token 解析为真实 Supabase 用户并透传 `userId`。
- **输入校验**：
  - 全部 query/path 参数通过 Zod 白名单校验；
  - 排序维度与内容类型枚举固定，避免任意字符串分支。
- **敏感数据处理**：
  - 不在日志、文档、代码中硬编码 token / URL / 密钥；
  - 不在错误响应中暴露内部堆栈与数据库细节。
- **SQL 注入防护**：
  - Supabase 查询使用 SDK 构建，不手拼原始 SQL；
  - 若后续需要 RPC，也应只传递已校验参数。
- **CSRF/XSS 防护**：
  - 当前主要是原生客户端调用 API，首版不新增 cookie-based 写接口；
  - 文本字段按普通数据返回，由客户端负责显示层安全处理。
- **幂等与防滥用**：
  - 通过唯一键 `(user_id, drama_id)` 或等价内存结构保证预约幂等；
  - 本期不新增限流依赖，但可预留 `TOO_MANY_REQUESTS` 语义。

---

## 14. 性能考虑

- **预期 QPS**：当前 harness / mock 环境以功能正确性为主，低并发即可满足首版验证；
- **缓存策略**：
  - 首版不引入 Redis 缓存；
  - 排行列表直接基于 mock 数据或数据库排序结果返回；
  - 客户端自行处理页面内存态，不依赖服务端缓存。
- **数据库优化**：
  - `play_count`、`booking_count`、`recommendation_score` 都需要与 `content_type` 组合索引；
  - `bookings(user_id, drama_id)` 唯一约束是幂等与性能基础。
- **连接池配置**：
  - 继续使用现有 Supabase client 管理方式；
  - 本期不新增连接池中间层。
- **兼容成本控制**：
  - 通过新增排行专用 schema，而非改写现有 `DramaSchema` 消费方，降低对首页/搜索的性能与回归影响。

---

## 15. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | 无 | 无 | 首版完全复用现有 Next.js、Zod、Vitest、Supabase、ioredis 技术栈 |

> ⚠️ 本方案不引入任何未获批准的新依赖。

---

## 16. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| shared design 成功响应结构与代码现状不一致 | 新接口可能破坏既有客户端/测试契约 | 🟡 | 高 | 明确以现有代码 contract 为准，成功响应继续直出资源体 | 若 review 需要统一 envelope，必须作为后续独立兼容改造处理 |
| Supabase 表结构尚未具备排行字段 | 真实 repository 无法直接落地排序和预约能力 | 🔴 | 高 | 新增 migration，不修改旧 migration；mock 先闭合功能链路 | 开发期先仅启用 mock repository |
| `requireAuth` 仅为 skeleton | 预约接口无法真正识别用户身份 | 🟡 | 高 | 先保证匿名 401 与 Bearer header 骨架拦截，预留 userId 注入点 | 在登录能力未完成前，仅验证接口与端侧拦截链路 |
| 预约幂等实现不一致 | 可能重复增加 `booking_count` | 🔴 | 中 | 以唯一约束 / 内存 Set 保证同一用户同一 drama 只增加一次 | 出现问题时临时回退到只返回 booked 状态，不累加展示数 |
| 推荐值规则后续变化 | 客户端展示与服务端排序可能不一致 | 🟡 | 中 | 统一由 Backend 返回最终 `recommendation_score`，客户端不计算 | 若规则调整，仅改服务端字段生成逻辑，不改接口形态 |

---

## 17. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/api/dramas.md` | `GET /api/dramas`、错误码 | 当前成功响应为资源体直出，错误响应为 `{ error: { code, message } }` |
| `wiki/architecture/overview.md` | Backend API 服务层、当前首页承载结构 | 当前 Backend 仍基于 `DramaService -> DramaMockRepository` 提供发现类数据 |
| `wiki/features/homepage-feed/index.md` | Backend、状态管理、边界处理 | 现有首页分页 contract 与大页码返回空数组行为可直接复用 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `backend/CLAUDE.md` | 明确四层架构、TypeScript/Next.js/Supabase/Zod/Redis 约束，以及统一错误处理要求 |
| `.claude/skills/feature-workflow/references/design-writing.md` | design-platforms 需查代码与 wiki，并按模板输出方案 |
| `.claude/skills/feature-workflow/references/backend-design/service-design.md` | Backend 方案需覆盖 API、middleware、service、migration、任务、测试 |
| `.claude/skills/feature-workflow/assets/design-backend-template.md` | Backend 方案章节模板 |
| `docs/specs/2026-07-27-prd-05-ranking/spec.md` | PRD-05 用户故事、边界条件、字段语义与验收标准 |
| `docs/specs/2026-07-27-prd-05-ranking/design.md` | shared design 中的 API、共享逻辑、兼容性与数据模型定义 |
| `backend/src/lib/schemas.ts` | 当前 `DramaSchema`、分页 schema、搜索 query schema 的命名与校验风格 |
| `backend/src/lib/errors.ts` | 当前 `ErrorCode`、`AppError` 与 `formatErrorResponse` 契约 |
| `backend/src/middleware/error-handler.ts` | 统一错误处理中对 `AppError`/`ZodError` 的处理方式 |
| `backend/src/middleware/auth.ts` | 当前仅有 skeleton `requireAuth`，只校验 Bearer header 格式 |
| `backend/src/middleware/logger.ts` | 已存在可选 logger wrapper，但 route 未统一接入 |
| `backend/src/middleware/cors.ts` | 已存在可选 CORS wrapper，但 route 未统一接入 |
| `backend/src/services/drama/drama.service.ts` | 现有 `DramaService` 已聚合首页、搜索、热搜能力，适合作为排行与预约扩展入口 |
| `backend/src/services/drama/drama.service.test.ts` | 现有 service 测试组织方式 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 当前 repository 抽象边界与分页返回结构 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 现有 12 条首页种子、分页逻辑与内存 clone 策略 |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 当前 Supabase row 映射、字段差异与接口闭合要求 |
| `backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts` | 当前 Supabase repository 契约测试风格 |
| `backend/src/app/api/dramas/route.ts` | 现有 `GET /api/dramas` query 校验与 service 接线方式 |
| `backend/src/app/api/dramas/search/route.ts` | 现有搜索 route 的 query 解析与响应风格 |
| `backend/src/app/api/__tests__/dramas.test.ts` | 现有 API route 测试风格及大页码/校验错误断言 |
| `backend/src/app/api/__tests__/dramas-search.test.ts` | 现有搜索 route 测试风格 |
| `backend/src/lib/config.ts` | 当前环境变量读取与默认值现状 |
| `backend/package.json` | 当前依赖清单，确认无需新增依赖 |
| `backend/supabase/migrations/00000000000001_init_tables.sql` | 当前 `dramas` 表已含 `play_count`，但缺少 `content_type` / `booking_count` / `recommendation_score` / `bookings` |
