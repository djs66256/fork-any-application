# Backend 端技术方案：PRD-11 个人资产管理

> 创建日期：2026-07-30
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

PRD-11 的 Backend 端只新增“当前登录用户预约资产列表”读取能力，不改动既有预约写接口 `POST /api/dramas/:id/book`。整体继续遵守当前仓库已经落地的四层结构：Route → Middleware → Service → Repository → Infrastructure / Shared。

与 PRD-05 排行体系类似，本期真实用户资产数据必须走 Supabase，而不能落到默认 `DramaMockRepository`。原因是当前 repository registry 仍把 drama 默认仓储指向 mock 实现（`backend/src/repositories/repository-registry.ts:17`），它不具备真实 `bookings` / `auth.users` 数据。因此，`GET /api/users/me/bookings` 与现有 `POST /api/dramas/:id/book` 一样，route 层直接实例化 `DramaSupabaseRepository`，由 `DramaService` 负责 schema 校验与错误收口。

```text
GET /api/users/me/bookings
  -> Route Handler (`app/api/users/me/bookings/route.ts`)
     -> withErrorHandler
     -> requireAuthContext
     -> getAuth(request)
     -> BookingAssetQuerySchema.parse(status/page/pageSize)
     -> DramaService.listUserBookings({ userId, status, page, pageSize })
        -> DramaRepositoryInterface.listUserBookings(...)
           -> DramaSupabaseRepository.listUserBookings(...)
              -> Query A: bookings JOIN dramas (summary 投影)
              -> Query B: bookings JOIN dramas (当前 Tab 分页列表)
              -> 服务端归类 dramas.status -> availability_status
              -> 过滤 join 失败 / 未知 status 的脏记录
        -> BookingAssetListResponseSchema.parse(result)
     -> NextResponse.json({ data, pagination, summary })
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `backend/src/app/api/users/me/route.ts` | 不变 | 继续返回认证域包裹响应 `{ code, data, message }`，不承载资产列表 |
| `backend/src/app/api/users/me/bookings/route.ts` | 新增 | 新增当前用户预约资产列表接口，返回资源体直出 `{ data, pagination, summary }` |
| `backend/src/app/api/dramas/[id]/book/route.ts` | 不变 | 保持既有预约写语义与 Supabase repository 直连方式 |
| `backend/src/services/drama/drama.service.ts` | 扩展 | 在 drama 资源域内新增 `listUserBookings()`，继续统一承接 schema 校验与错误包装 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 扩展 | 新增预约资产列表 query/result contract |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 扩展 | 新增 bookings join dramas 的列表与 summary 查询实现 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 扩展 | 为接口闭合补齐 `listUserBookings()`；该方法不作为真实资产列表运行时数据源 |
| `backend/src/lib/schemas.ts` | 扩展 | 新增 `BookingAsset*` 共享 schema |
| `backend/src/middleware/auth.ts` | 复用 | 新接口沿用 `requireAuthContext()` + `getAuth(request)` |
| `backend/src/middleware/error-handler.ts` | 不变 | 继续承接 `AppError` / `ZodError` / unknown error |
| `backend/supabase/migrations/*.sql` | 不变 | 首版复用已存在 `bookings` 表、`dramas.status` 列与现有索引，不新增 migration |

### 1.2 与 shared design / 当前代码现状的兼容说明

1. **成功响应仍然资源体直出**  
   `GET /api/users/me/bookings` 属于 dramas / assets 域接口，不复用认证域 `success()` 包裹函数。原因是当前 dramas 域真实成功响应基线是 `NextResponse.json(result)`（见 `backend/src/app/api/dramas/rankings/route.ts:21`、`backend/src/app/api/dramas/[id]/book/route.ts:27`），shared design 也已定稿为 `{ data, pagination, summary }`。

2. **用户资源路径与业务服务边界分离**  
   虽然 URL 位于 `/api/users/me/bookings`，但底层数据依然来源于 `bookings + dramas`，所以服务层仍放在 `DramaService` / `DramaRepositoryInterface` 内，不额外拆出 `BookingService`。

3. **真实数据源继续显式使用 Supabase repository**  
   当前 `getDramaRepository()` 默认返回 mock（`backend/src/repositories/repository-registry.ts:56`），无法满足“当前登录用户真实预约资产”语义。因此新 route 与已有 booking write route 保持一致，直接实例化 `DramaSupabaseRepository`，避免误读 mock 数据。

4. **首版不新增数据库结构**  
   当前 migration 已具备本期所需底层字段：
   - `bookings` 表：`backend/supabase/migrations/20260727000100_add_ranking_fields_and_bookings.sql:13`
   - `dramas.status`：`backend/supabase/migrations/00000000000001_init_tables.sql:26`
   - `episode_count` 列：`backend/supabase/migrations/20260727000300_rename_episode_count.sql:3`
   - `tags` 列：`backend/supabase/migrations/20260727000200_add_drama_tags.sql:4`

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `backend/src/app/api/users/me/bookings/route.ts` | 新增 | 新增 `GET /api/users/me/bookings` 路由，解析 query、读取 auth、调用 `DramaService.listUserBookings()` |
| `backend/src/services/drama/drama.service.ts` | 修改 | 新增 `listUserBookings()`，对 repository 返回结果做 `BookingAssetListResponseSchema` 校验 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 修改 | 新增 `ListUserBookingsParams` 与 `listUserBookings()` contract |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 修改 | 新增 bookings join dramas 的 summary + 当前 Tab 分页查询与映射逻辑 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 补齐接口实现，避免接口扩展后编译错误；运行时不作为真实资产列表来源 |
| `backend/src/lib/schemas.ts` | 修改 | 新增 `BookingAssetAvailabilityStatusSchema`、`BookingAssetQuerySchema`、`BookingAssetSchema`、`BookingAssetSummarySchema`、`BookingAssetListResponseSchema` |
| `backend/src/app/api/__tests__/users-me-bookings.test.ts` | 新增 | 覆盖默认 query、401、400、空列表、超大页码、错误映射 |
| `backend/src/services/drama/drama.service.test.ts` | 修改 | 新增 `listUserBookings()` 的 schema 校验与错误包装测试 |
| `backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts` | 修改 | 覆盖状态归类、脏数据过滤、排序、分页与 summary 一致性 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 覆盖 `BookingAsset*` schema 的 query / response 约束 |

> 本阶段只产出设计文档，不直接改上述实现文件。

---

## 3. API 路由设计

### 3.1 路由注册

| 路由文件 | HTTP 方法 | URL 路径 | 中间件链 | 说明 |
|---------|----------|---------|---------|------|
| `backend/src/app/api/users/me/bookings/route.ts` | `GET` | `/api/users/me/bookings` | `withErrorHandler` + `requireAuthContext` + Route 内 query Zod 校验 | 返回当前登录用户预约资产列表与双 Tab 摘要 |
| `backend/src/app/api/users/me/route.ts` | `GET` | `/api/users/me` | `withErrorHandler` + `requireAuthContext` | 已有当前用户资料接口，保持不变 |
| `backend/src/app/api/dramas/[id]/book/route.ts` | `POST` | `/api/dramas/:id/book` | `withErrorHandler` + `requireAuthContext` + path param Zod 校验 | 已有预约写接口，作为资产列表的数据来源 |

### 3.2 路由分组策略

- `/api/users/me/bookings` 使用“当前用户资源”路径，而不是继续挂在 `/api/dramas/*` 之下：
  - 路径语义是“读取当前登录用户的私有资产集合”；
  - 路由层不允许传入任意 `userId`，只允许使用认证上下文中的 `userId`；
  - 这样可以从资源路径上直接消除越权读取入口。
- 尽管路径属于 users 资源树，实际业务实现仍复用 drama 域 service / repository，因为展示字段与状态映射依赖 `dramas`。
- 不新增版本前缀，不新增 `/api/bookings` 顶级资源，保持与当前仓库 RESTful 风格一致。

### 3.3 参数校验

```ts
import { z } from 'zod';

export const BookingAssetAvailabilityStatusSchema = z.enum(['online', 'upcoming']);

export const BookingAssetQuerySchema = z.object({
  status: BookingAssetAvailabilityStatusSchema.default('online'),
  page: z.coerce.number().int().min(1).default(1),
  pageSize: z.coerce.number().int().min(1).max(20).default(20),
});
```

Route 层解析方式：

```ts
const { searchParams } = new URL(request.url);
const query = BookingAssetQuerySchema.parse({
  status: searchParams.get('status') ?? undefined,
  page: searchParams.get('page') ?? undefined,
  pageSize: searchParams.get('pageSize') ?? undefined,
});
```

参数约束结论：

| 参数 | 规则 | 说明 |
|------|------|------|
| `status` | `online \| upcoming` | 当前请求 Tab，默认 `online` |
| `page` | `int >= 1` | 与现有列表接口习惯一致 |
| `pageSize` | `1 <= int <= 20` | 首版上限 20，限制单次返回体积 |
| request body | 无 | `GET` 接口不接收 body |

### 3.4 响应契约

成功响应：

```json
{
  "data": [
    {
      "drama_id": "550e8400-e29b-41d4-a716-446655440001",
      "title": "逆袭归来后我成了豪门团宠",
      "cover_url": "https://example.com/dramas/001.jpg",
      "episode_count": 68,
      "booked_at": "2026-07-30T03:25:00.000Z",
      "availability_status": "online"
    }
  ],
  "pagination": {
    "page": 1,
    "page_size": 20,
    "total": 8,
    "total_pages": 1
  },
  "summary": {
    "online_count": 8,
    "upcoming_count": 3
  }
}
```

错误响应继续沿用当前 Backend 统一结构：

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed"
  }
}
```

### 3.5 route 伪代码

```ts
export const GET = withErrorHandler(requireAuthContext(async (request: NextRequest) => {
  const query = BookingAssetQuerySchema.parse(...);
  const auth = getAuth(request);

  const service = new DramaService(new DramaSupabaseRepository());
  const result = await service.listUserBookings({
    userId: auth.userId,
    status: query.status,
    page: query.page,
    pageSize: query.pageSize,
  });

  return NextResponse.json(result);
}));
```

---

## 4. Middleware 链设计

### 4.1 请求流水线

```text
请求
  -> withErrorHandler
  -> requireAuthContext
  -> getAuth(request)
  -> BookingAssetQuerySchema.parse(...)
  -> DramaService.listUserBookings(...)
  -> DramaSupabaseRepository.listUserBookings(...)
  -> BookingAssetListResponseSchema.parse(...)
  -> JSON 响应
```

### 4.2 Middleware 清单

| Middleware | 作用域 | 说明 |
|-----------|--------|------|
| `withErrorHandler` | 路由级 | 统一处理 `AppError`、`ZodError` 与未知异常 |
| `requireAuthContext` | `GET /api/users/me/bookings` | 强制读取当前登录态，未登录直接返回 401 |
| `getAuth(request)` | Route Handler 内 | 从 request 中提取已验证的 `userId`，禁止从 query/path 读用户标识 |
| 限流 middleware | 本期不新增 | 当前 backend 尚无统一 Redis 限流链路；本接口不额外落地主动 429 生成逻辑，仅保留与 shared/spec 对齐的 contract 预留 |
| logger / cors middleware | 不新增 | 保持与当前 route 层一致，不额外挂统一链路 |

### 4.3 错误传播方式

- **未登录**：`requireAuthContext()` 抛 `Errors.authUnauthorized('请先登录')`，由 `withErrorHandler` 输出 `401 + AUTH_UNAUTHORIZED`。
- **参数错误**：`BookingAssetQuerySchema.parse()` 抛 `ZodError`，由 `withErrorHandler` 统一转为 `400 + VALIDATION_ERROR + details`。
- **业务错误**：service / repository 抛 `AppError`，由 `withErrorHandler` 输出对应状态码与错误码。
- **未知异常**：统一收口为 `500 + INTERNAL_ERROR`。
- **上游数据源不可用**：repository 识别为基础设施不可达时抛 `Errors.serviceUnavailable('Supabase')`，route 透传为 `503`。

---

## 5. Service 层设计

### 5.1 Service 清单

| Service | 职责 | 输入 | 输出 | 依赖 |
|---------|------|------|------|------|
| `DramaService.listRankings` | 既有排行查询 | `RankingParams` + 可选 auth | `PaginatedResult<RankingDrama>` | `DramaRepositoryInterface` |
| `DramaService.bookDrama` | 既有预约写入 | `BookDramaParams` | `BookDramaResponse` | `DramaRepositoryInterface` |
| `DramaService.listUserBookings` | 当前用户预约资产列表查询 | `ListUserBookingsParams` | `BookingAssetListResponse` | `DramaRepositoryInterface` |

新增 service 方法建议：

```ts
async listUserBookings(params: ListUserBookingsParams): Promise<BookingAssetListResponse> {
  try {
    return BookingAssetListResponseSchema.parse(await this.dramaRepository.listUserBookings(params));
  } catch (error) {
    if (isAppError(error)) {
      throw error;
    }
    throw Errors.internal('Invalid user booking assets result');
  }
}
```

### 5.2 输入输出 contract

```ts
export interface ListUserBookingsParams {
  userId: string;
  status: BookingAssetAvailabilityStatus;
  page: number;
  pageSize: number;
}
```

Service 输出直接对齐 shared design：

```ts
export const BookingAssetListResponseSchema = z.object({
  data: z.array(BookingAssetSchema),
  pagination: PaginationSchema,
  summary: BookingAssetSummarySchema,
});
```

### 5.3 事务边界

| 操作组合 | 事务隔离级别 | 回滚策略 |
|---------|------------|---------|
| `GET /api/users/me/bookings` summary + 当前页列表读取 | 无显式事务（只读） | 任一查询失败即整体报错，不返回部分成功 |
| `POST /api/dramas/:id/book` | 保持现状 | 不属于本 PRD 新增范围 |

本期新增能力为纯读取链路，不引入显式事务。因为 `summary` 与列表只要求同一请求内口径一致，不要求强一致快照；在当前低频个人资产场景下，单次读请求容忍极短窗口内的并发写差异。

### 5.4 业务异常定义

| 异常类型 | 触发条件 | HTTP 状态码 | 错误码 |
|---------|---------|-----------|--------|
| 参数校验错误 | `status/page/pageSize` 非法 | 400 | `VALIDATION_ERROR` |
| 未登录 | 缺少或无效 bearer token | 401 | `AUTH_UNAUTHORIZED` |
| 频控错误（contract 预留） | 未来若接入统一限流器，或上游明确返回限流错误 | 429 | `TOO_MANY_REQUESTS` / `AUTH_RATE_LIMITED` |
| 数据源不可用 | Supabase 客户端 / PostgREST 不可达 | 503 | `SERVICE_UNAVAILABLE` |
| 结果映射异常 | row shape 不合法、schema parse 失败 | 500 | `INTERNAL_ERROR` |

---

## 6. Repository 与查询设计

### 6.1 Repository contract

建议在 `backend/src/repositories/interfaces/drama.repository.interface.ts` 中新增：

```ts
export interface ListUserBookingsParams {
  userId: string;
  status: BookingAssetAvailabilityStatus;
  page: number;
  pageSize: number;
}

export interface DramaRepositoryInterface {
  // ...existing methods
  listUserBookings(params: ListUserBookingsParams): Promise<BookingAssetListResponse>;
}
```

这里直接返回 `BookingAssetListResponse`，而不是再包一层通用 `PaginatedResult<T>`，因为该接口除 `data + pagination` 外还固定带 `summary`。

### 6.2 Supabase 查询策略

`DramaSupabaseRepository.listUserBookings()` 内部使用同一用户口径做两次轻量查询：

#### Query A：summary 投影

目的：统计当前用户所有**有效 booking** 的 `online_count / upcoming_count`。

读取字段：
- `bookings.drama_id`
- `bookings.created_at`
- `dramas.status`

约束：
- `bookings.user_id = params.userId`
- inner join `dramas`，join 失败记录天然被过滤
- 不读取标题 / 封面等大字段，只投影统计所需字段

#### Query B：当前 Tab 分页列表

目的：返回当前 `status` 对应的分页列表。

读取字段：
- `bookings.drama_id`
- `bookings.created_at`
- `dramas.title`
- `dramas.cover_url`
- `dramas.episode_count`
- `dramas.status`

约束：
- `bookings.user_id = params.userId`
- inner join `dramas`
- 按底层 `dramas.status` 映射过滤：
  - `online` -> `ongoing`, `completed`
  - `upcoming` -> `announced`
- 排序：`bookings.created_at DESC, bookings.drama_id DESC`
- 分页：`range(from, to)` + `count: 'exact'`

### 6.3 状态映射与脏数据过滤

定义内部映射函数：

```ts
function mapDramaStatusToAvailabilityStatus(status: string): 'online' | 'upcoming' | null {
  if (status === 'announced') return 'upcoming';
  if (status === 'ongoing' || status === 'completed') return 'online';
  return null;
}
```

定稿策略：

| 场景 | 处理方式 | 原因 |
|------|---------|------|
| `bookings` 无法联查 `dramas` | 直接过滤，不进入列表也不计入 summary | 与 spec / shared design 一致，避免计数与列表不一致 |
| `dramas.status = announced` | 映射为 `upcoming` | 首版固定规则 |
| `dramas.status in (ongoing, completed)` | 映射为 `online` | 首版固定规则 |
| 未知 `dramas.status` | 过滤并记录 warning，不返回给客户端 | 单条脏数据不应导致整页 500，也不能随意归到错误 Tab |

### 6.4 返回体映射

建议新增 row schema：

```ts
const BookingAssetRowSchema = z.object({
  drama_id: z.string().uuid(),
  created_at: z.string(),
  dramas: z.object({
    id: z.string().uuid(),
    title: z.string().min(1),
    cover_url: z.string().url().nullable().optional(),
    episode_count: z.number().int().min(0),
    status: z.string().min(1),
  }),
});
```

映射到共享 contract：

```ts
function mapRowToBookingAsset(row: BookingAssetRow): BookingAsset | null {
  const availabilityStatus = mapDramaStatusToAvailabilityStatus(row.dramas.status);
  if (!availabilityStatus) {
    return null;
  }

  return BookingAssetSchema.parse({
    drama_id: row.drama_id,
    title: row.dramas.title,
    cover_url: row.dramas.cover_url ?? null,
    episode_count: row.dramas.episode_count,
    booked_at: row.created_at,
    availability_status: availabilityStatus,
  });
}
```

### 6.5 mock repository 策略

当前默认 registry 仍返回 `DramaMockRepository`，但新 route 不经过 registry，因此 mock repository 不承担真实运行时查询职责。为保证接口闭合与测试可编译，建议在 `DramaMockRepository` 中补齐 `listUserBookings()`：

- 最简单实现：抛出 `Errors.notImplemented('User bookings are only available in supabase repository')`
- 若 coding 阶段需要补 repository 单测夹具，也可增量提供最小内存实现，但不作为首屏真实数据源依赖

推荐首版采用**显式 not implemented + route 直连 Supabase**，与现有 `POST /api/dramas/:id/book` 的真实数据源策略保持一致。

---

## 7. 数据库 Migration 计划

### 7.1 变更概述

| 表名 | 操作 | 说明 |
|------|------|------|
| `bookings` | 不变 | 复用已有用户预约关系表 |
| `dramas` | 不变 | 复用已有 `status`、`cover_url`、`episode_count` 等字段 |
| 索引 | 不变 | 首版继续使用已有 `idx_bookings_user_id`、`idx_bookings_created_at`、`idx_dramas_status` |

### 7.2 本期为何不新增 migration

当前 migration 已经覆盖首版读取所需的底层结构：

| 结构 | 位置 | 用途 |
|------|------|------|
| `bookings(user_id, drama_id, created_at)` | `20260727000100_add_ranking_fields_and_bookings.sql` | 用户预约关系与排序时间 |
| `dramas.status` | `00000000000001_init_tables.sql` | 归类为 `online / upcoming` 的底层事实 |
| `dramas.episode_count` | `20260727000300_rename_episode_count.sql` | 列表展示集数 |
| `dramas.tags` | `20260727000200_add_drama_tags.sql` | 与本接口无直接关系，但说明现有读取 schema 已扩展 |

因此首版 Backend 方案不新增 migration，不修改已执行 migration。

### 7.3 索引评估

| 现有索引 | 是否复用 | 说明 |
|---------|---------|------|
| `idx_bookings_user_id` | 是 | 支撑按用户筛选 booking |
| `idx_bookings_created_at` | 是 | 支撑按预约时间倒序读取 |
| `idx_dramas_status` | 是 | 支撑按上线状态过滤 |

若后续线上验证发现某用户 booking 数量显著上升，再考虑新增复合索引（如 `bookings(user_id, created_at DESC)`）。这不属于 PRD-11 首版必做项。

### 7.4 回滚策略

本期无 migration 变更，因此无额外回滚动作；若 coding 阶段误加 migration，应以新 migration 回滚，不回改旧文件。

---

## 8. 后台任务/队列设计

### 8.1 任务清单

| 任务名称 | 触发条件 | 执行频率 | 队列/调度方式 | 重试策略 | 超时 |
|---------|---------|---------|-------------|---------|------|
| 无 | — | — | — | — | — |

本期新增能力为同步读取，不引入异步任务、消息队列或定时汇总表。

### 8.2 不引入队列的原因

- 接口只读，且首版数据规模较小；
- `summary` 完全可以在请求期内按当前用户有效 booking 即时计算；
- 用户没有批准新增开源依赖，当前仓库也未在 dramas 域落 Bull/BullMQ 类基础设施；
- 下载能力、提醒能力、预约到期通知等异步需求均不在本 PRD 范围内。

---

## 9. 配置与环境

| 配置项 | 环境变量 Key | 开发环境值 | 生产环境值 | 说明 |
|--------|-------------|----------|-----------|------|
| Supabase URL | `SUPABASE_URL` | 来自 `.env.local` | 生产环境注入 | repository 继续通过现有 Supabase admin client 访问数据库 |
| Supabase service role | `SUPABASE_SERVICE_ROLE_KEY` | 来自 `.env.local` | 生产环境注入 | 服务端读取 booking / drama 聚合数据 |
| App 端口 | `PORT` | 现有 backend 配置 | 部署环境注入 | 与本接口无新增特殊要求 |
| Redis | `REDIS_URL` | 现有配置 | 部署环境注入 | 本期不新增资产列表缓存或限流逻辑 |

> ⚠️ 禁止硬编码环境地址、用户 ID、token 或固定 host。所有配置沿用现有 `config` / Supabase client 注入方式。

### 9.1 新增环境变量评估

本期**不新增任何环境变量**。原因：
- 不引入新外部服务；
- 不新增 feature flag；
- 不新增缓存开关；
- 不新增下载或消息相关后台任务。

---

## 10. API 调用清单（调用外部服务）

| 外部服务 | API / 能力 | 调用时机 | 超时 | 降级策略 |
|---------|-----------|---------|------|---------|
| Supabase PostgreSQL / PostgREST | `bookings` + `dramas` 读取 | `GET /api/users/me/bookings` 请求期 | 沿用现有 Supabase client 默认超时 | 不可用时返回 `503 SERVICE_UNAVAILABLE` |
| Supabase Auth（通过 middleware） | bearer token 验证 | 进入受保护接口前 | 沿用现有 auth helper | token 无效返回 `401 AUTH_UNAUTHORIZED` |

本接口不调用任何第三方 HTTP 服务，也不触发短信、推送或 MQ。

---

## 11. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Backend 实现方式 |
|---------|---------------|-----------------|
| booking 独立 route | Android / iOS 都使用独立 booking route | Backend 只暴露 `/api/users/me/bookings`，不感知端侧导航细节 |
| 登录承接目标 | 未登录先进入 booking route 登录承接态 | route 强制鉴权，端侧匿名态不应直接请求；若仍请求则返回 401 |
| 默认 Tab | 默认 `status=online&page=1&pageSize=20` | `BookingAssetQuerySchema` 通过 default 值兜底 |
| `summary` 口径 | 双端只消费服务端返回计数 | repository 在服务端统一统计 `online_count / upcoming_count`，客户端不重算 |
| 历史脏 booking 过滤 | join 失败记录从列表和 summary 同时剔除 | Query A / Query B 都基于 valid bookings join 结果 |
| 请求防乱序 | 旧请求晚返回不得覆盖当前状态 | 由移动端处理；Backend 保证单次响应结构稳定 |
| 下载占位延续 | “我的下载”不新增后端请求 | Backend 不新增 downloads 相关 route / schema |

---

## 12. 边界与错误处理

### 12.1 全局错误处理架构

| 层级 | 机制 | 说明 |
|------|------|------|
| Middleware | `withErrorHandler` | 统一格式化 `AppError` / `ZodError` / unknown error |
| Auth | `requireAuthContext` + `getAuth` | 负责 401 鉴权失败与 userId 注入 |
| Service | `BookingAssetListResponseSchema.parse()` | 防止 repository 返回漂移 contract |
| Repository | 受控状态映射 + row schema parse | 防止脏数据、未知 status、join 失败污染 API 响应 |
| 日志 | `console.error` / `console.warn` | 未知异常打 error；未知 `dramas.status` 打 warning |

### 12.2 错误码定义

| 业务错误码 | HTTP 状态码 | 说明 | 响应示例 |
|-----------|------------|------|---------|
| `VALIDATION_ERROR` | 400 | `status/page/pageSize` 非法 | `{ "error": { "code": "VALIDATION_ERROR", "message": "Validation failed" } }` |
| `AUTH_UNAUTHORIZED` | 401 | 未登录或 token 失效 | `{ "error": { "code": "AUTH_UNAUTHORIZED", "message": "请先登录" } }` |
| `TOO_MANY_REQUESTS` | 429 | contract 预留；仅在未来统一限流器或上游能力明确返回限流时使用 | `{ "error": { "code": "TOO_MANY_REQUESTS", "message": "Too many requests" } }` |
| `AUTH_RATE_LIMITED` | 429 | contract 预留；本接口本期不主动实现认证域频控 | `{ "error": { "code": "AUTH_RATE_LIMITED", "message": "Too many authentication attempts" } }` |
| `INTERNAL_ERROR` | 500 | 结果映射错误、未知异常 | `{ "error": { "code": "INTERNAL_ERROR", "message": "Internal server error" } }` |
| `SERVICE_UNAVAILABLE` | 503 | Supabase 不可用 | `{ "error": { "code": "SERVICE_UNAVAILABLE", "message": "Service unavailable: Supabase" } }` |

### 12.3 边界场景

| 场景 | 触发条件 | API 行为 | 说明 |
|------|---------|---------|------|
| 默认 Tab 无数据 | 当前用户没有任何 `online` 预约 | 返回 `200 + data=[] + summary` | 不返回 404 |
| 另一侧有数据 | `online=0`、`upcoming>0` | 当前 status 仍返回空列表，summary 正确 | 不替客户端自动切换 Tab |
| 超大页码 | `page` 大于总页数 | 返回 `200 + data=[]`，保留正确 `pagination.total` / `total_pages` | 与现有列表接口习惯一致 |
| 非法参数 | `status=foo`、`page=0`、`pageSize=999` | 返回 400 `VALIDATION_ERROR` | 由 `withErrorHandler` 输出 details |
| 历史脏 booking | booking 无法联查到 drama | 从列表和 summary 同时过滤 | 与 spec / design 定稿一致 |
| 未知 `dramas.status` | 非 `announced/ongoing/completed` | 过滤记录并记 warning | 避免返回错误状态 |
| token 失效 | bearer 无效 | 返回 401 | 不泄露旧用户数据 |
| 数据源不可用 | Supabase 连接失败 | 返回 503 | 属于 infra 级失败 |

### 12.4 错误日志与监控

首版不新增监控平台集成，但 coding 阶段至少保留以下日志：

- `console.error('Unhandled error:', err)`：沿用现有全局错误处理器；
- `console.warn('[BookingAssets] Unknown drama status', { dramaId, status })`：发现脏状态时记录；
- 如 repository 捕获到 PostgREST / Supabase 连接类错误，记录上游 error message 后转成 `SERVICE_UNAVAILABLE`。

---

## 13. 测试策略

### 13.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| Route 测试 | query 解析、auth gating、状态码与响应结构 | Vitest + NextRequest |
| Service 单测 | schema 校验、AppError 透传、未知错误包装 | Vitest |
| Repository 测试 | Supabase row 映射、状态归类、summary 统计、脏数据过滤、排序分页 | Vitest + mock Supabase client |
| Schema 测试 | `BookingAsset*` query/response 合法性 | Vitest + Zod |

### 13.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 测试类型 |
|------|---------|------|---------|---------|
| B-01 | 默认 query 生效 | 无 query | service 收到 `{ status: 'online', page: 1, pageSize: 20 }` | Route |
| B-02 | 未登录访问 | 缺少 bearer | `401 AUTH_UNAUTHORIZED` | Route |
| B-03 | 非法参数 | `status=foo&page=0&pageSize=21` | `400 VALIDATION_ERROR` | Route |
| B-04 | 空列表 | 用户无当前 Tab 数据 | `200 + data=[] + summary` | Route / Repository |
| B-05 | 超大页码 | `page=999` | `200 + data=[]` 且 pagination 正确 | Route / Repository |
| B-06 | 状态归类 | `announced/ongoing/completed` | 分别映射为 `upcoming/online/online` | Repository |
| B-07 | join 失败过滤 | booking 指向不存在 drama | 列表与 summary 同时剔除 | Repository |
| B-08 | 未知 status 过滤 | `status=archived` 等异常值 | 不返回该记录，记 warning | Repository |
| B-09 | 顺序稳定 | 多条相同秒 booking | 按 `booked_at DESC, drama_id DESC` 排序 | Repository |
| B-10 | service 结果漂移 | repository 返回非法 shape | `500 INTERNAL_ERROR` | Service |
| B-11 | 数据源不可用 | Supabase client error | `503 SERVICE_UNAVAILABLE` | Repository / Route |
| B-12 | 429 contract 预留说明 | 本期未接入统一限流器 | 文档明确 429 仅为 contract 预留，不作为本期主动实现或必测路径 | Design Review |

### 13.3 代表性测试文件

| 文件 | 作用 |
|------|------|
| `backend/src/app/api/__tests__/users-me-bookings.test.ts` | 新接口路由级契约测试 |
| `backend/src/services/drama/drama.service.test.ts` | service 结果校验与错误包装测试 |
| `backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts` | bookings join dramas 的仓储测试 |
| `backend/src/lib/__tests__/schemas.test.ts` | `BookingAssetQuerySchema` / `BookingAssetListResponseSchema` 测试 |

---

## 14. 实施顺序建议

1. 在 `backend/src/lib/schemas.ts` 增加 `BookingAsset*` schema 与 type；
2. 在 `backend/src/repositories/interfaces/drama.repository.interface.ts` 扩展 `listUserBookings()` contract；
3. 在 `backend/src/repositories/supabase/drama.supabase.repository.ts` 实现 summary + 列表查询、状态映射与脏数据过滤；
4. 在 `backend/src/services/drama/drama.service.ts` 增加 `listUserBookings()`；
5. 新增 `backend/src/app/api/users/me/bookings/route.ts`；
6. 补齐 route / service / repository / schema 测试；
7. 仅为接口闭合更新 `DramaMockRepository`，但不把新 route 改为走 registry。

---

## 15. 结论

PRD-11 Backend 首版不新增数据库结构、不引入新依赖、不改动既有预约写接口；只在现有 `bookings` + `dramas.status` 基础上补齐一个受保护的读取接口 `GET /api/users/me/bookings`。实现关键点是：

- route 路径属于 users 资源树，但 service / repository 仍落在 drama 域；
- 真实数据源继续显式使用 `DramaSupabaseRepository`，避免误走 mock；
- `summary` 与列表都仅统计有效 booking，并同步过滤 join 失败 / 未知 status 记录；
- 响应保持 dramas 域资源体直出风格，与 shared design 完全对齐；
- 首版通过现有索引即可支撑，不额外引入 migration、缓存或队列。