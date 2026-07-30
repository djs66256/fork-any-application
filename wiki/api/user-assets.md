# User Assets API 文档

> 最后更新：2026-07-30

---

## 概述

PRD-11 为移动端“我的预约”真实页新增 `GET /api/users/me/bookings`，用于按当前登录用户读取预约资产列表。该接口是受保护的用户资产读取接口，不复用 `POST /api/dramas/:id/book` 的写入 envelope，而是直接返回 `{ data, pagination, summary }`，供 Android / iOS 在独立 booking 页面中展示双 Tab（`online / upcoming`）与顶部摘要（`backend/src/app/api/users/me/bookings/route.ts:1-29`、`backend/src/lib/schemas.ts:348-379`、`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt:90-95`、`ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift:119-137`）。

当前 contract 的关键特点：

- 强制鉴权：Route 继续沿用 `withErrorHandler(requireAuthContext(...))` 与 `getAuth(request)`，只允许当前 bearer token 对应用户读取自己的预约资产（`backend/src/app/api/users/me/bookings/route.ts:10-28`）。
- 查询默认值由 Zod 收口：`status` 默认 `online`，`page` 默认 `1`，`pageSize` 默认 `20`，且 `pageSize` 上限为 `20`（`backend/src/lib/schemas.ts:351-355`）。
- 响应结构固定为 `{ data, pagination, summary }`，其中 `pagination` 继续沿用 snake_case 字段 `page / page_size / total / total_pages`（`backend/src/lib/schemas.ts:374-379`、`backend/src/repositories/supabase/drama.supabase.repository.ts:569-578`）。
- 服务端统一做状态归类和 summary 聚合：`announced -> upcoming`，`ongoing/completed -> online`；客户端不需要自行重算 Tab 计数（`backend/src/repositories/supabase/drama.supabase.repository.ts:287-297`、`backend/src/repositories/supabase/drama.supabase.repository.ts:334-356`）。
- 无效 join 行和未知 `dramas.status` 都会被过滤，不会泄露到列表或 `summary`；未知状态仅在服务端告警（`backend/src/repositories/supabase/drama.supabase.repository.ts:308-321`、`backend/src/repositories/supabase/drama.supabase.repository.ts:358-378`）。
- Route 自动化测试已覆盖默认 query、显式 query、401、400、超大页码空列表、503 与 500，当前文档依据真实实现而非纯设计稿（`backend/src/app/api/__tests__/users-me-bookings.test.ts:55-252`）。

---

## GET /api/users/me/bookings

### 功能简介

返回当前登录用户在 `bookings` 表中的预约资产列表，并按 `status=online|upcoming` 做分页过滤，同时返回双 Tab 所需的全量 `summary`。首版主要供 Android / iOS 菜单“我的预约”真实页消费，Web 本期 skipped（`backend/src/app/api/users/me/bookings/route.ts:10-28`、`docs/specs/2026-07-30-prd-11-user-assets/design-web.md:12-15`）。

### 代码文件路径

- Route：`backend/src/app/api/users/me/bookings/route.ts:1-29`
- Schema：`backend/src/lib/schemas.ts:348-379`
- Service：`backend/src/services/drama/drama.service.ts:112-120`
- Repository：`backend/src/repositories/supabase/drama.supabase.repository.ts:287-378`、`backend/src/repositories/supabase/drama.supabase.repository.ts:526-579`
- 测试：`backend/src/app/api/__tests__/users-me-bookings.test.ts:50-252`

### path / method

`GET /api/users/me/bookings`

### Headers

| 字段 | 必填 | 说明 |
|------|------|------|
| `Authorization` | 是 | `Bearer <accessToken>`，缺失或无效时返回 `AUTH_UNAUTHORIZED` |

### Query Parameters

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `status` | enum | 否 | `online` | 只允许 `online` 或 `upcoming` |
| `page` | number | 否 | `1` | 1-based 页码，最小为 1 |
| `pageSize` | number | 否 | `20` | 单页数量，最小 1，最大 20 |

### Success Response

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
    "total": 1,
    "total_pages": 1
  },
  "summary": {
    "online_count": 1,
    "upcoming_count": 2
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `data[].drama_id` | string | 剧集 UUID，首版作为列表稳定主键 |
| `data[].title` | string | 剧名，服务端保证非空字符串 |
| `data[].cover_url` | string \| null | 封面 URL，可为空 |
| `data[].episode_count` | number | 剧集总集数，非负整数 |
| `data[].booked_at` | string | 预约时间，ISO 8601 原值 |
| `data[].availability_status` | enum | `online` 或 `upcoming` |
| `pagination.page` | number | 当前页码 |
| `pagination.page_size` | number | 当前请求的页大小 |
| `pagination.total` | number | 当前 `status` 下的总记录数 |
| `pagination.total_pages` | number | 当前 `status` 下的总页数 |
| `summary.online_count` | number | 当前用户全部有效预约中已上线数量 |
| `summary.upcoming_count` | number | 当前用户全部有效预约中待上线数量 |

### 当前行为说明

- Route 从 URL query 中读取 `status/page/pageSize`，交给 `BookingAssetQuerySchema.parse(...)` 统一做默认值填充与校验，再把 `auth.userId` 和 query 参数传给 `DramaService.listUserBookings()`（`backend/src/app/api/users/me/bookings/route.ts:10-26`）。
- `DramaService.listUserBookings()` 不重写 contract，只用 `BookingAssetListResponseSchema` 校验 repository 返回值；若 repository 返回不合法结构，会升级为 `INTERNAL_ERROR`（`backend/src/services/drama/drama.service.ts:112-120`）。
- Repository 先对当前用户执行一次全量 summary 查询，再执行一次当前 `status` 下的分页查询，所以 `summary` 不受当前页码影响，始终反映该用户全量有效 booking 的聚合结果（`backend/src/repositories/supabase/drama.supabase.repository.ts:531-578`）。
- `summary` 查询和分页查询都基于 `bookings JOIN dramas`；`parseBookingAssetRows()` 会过滤 schema 不合法或 `dramas` 为空的脏 join 行（`backend/src/repositories/supabase/drama.supabase.repository.ts:308-321`）。
- 状态映射逻辑固定为：`announced -> upcoming`，`ongoing/completed -> online`。如果底层 `dramas.status` 未命中这三类，服务端只告警并忽略该记录，不会透出未知状态给客户端（`backend/src/repositories/supabase/drama.supabase.repository.ts:287-297`、`backend/src/repositories/supabase/drama.supabase.repository.ts:334-378`）。
- 列表排序固定为 `created_at DESC`、再按 `drama_id DESC`；响应中的 `booked_at` 直接使用 `created_at` 原值返回（`backend/src/repositories/supabase/drama.supabase.repository.ts:323-331`、`backend/src/repositories/supabase/drama.supabase.repository.ts:554-576`）。
- 当前 route 保留了“429 是 contract reserve”的注释，但本身没有主动实现 rate-limit 分支；文档中仍保留该保留位，避免后续联调误把 429 当成未预留（`backend/src/app/api/users/me/bookings/route.ts:8`）。
- 超大页码不会返回 404，而是继续返回 `200 + data: []`，同时保留正确的 `pagination` 与 `summary`（`backend/src/app/api/__tests__/users-me-bookings.test.ts:152-196`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功返回当前用户预约资产列表 |
| 400 | `VALIDATION_ERROR` | `status` 非法、`page < 1` 或 `pageSize > 20` 等 query 校验失败 |
| 401 | `AUTH_UNAUTHORIZED` | 未携带 bearer token、token 无效或 token 失效 |
| 429 | `TOO_MANY_REQUESTS` | 预留 contract，当前 route 未主动实现，但客户端已按保留位处理 |
| 503 | `SERVICE_UNAVAILABLE` | Supabase 查询异常或 repository 上游不可用 |
| 500 | `INTERNAL_ERROR` | service / repository 抛出未预期错误，或返回结构不符合 schema |

---

## 与其它接口的关系

- `POST /api/dramas/:id/book` 仍是预约写入入口，`GET /api/users/me/bookings` 只负责读取当前用户已写入的预约资产，两者共同组成“写入预约 → 菜单回看资产”的闭环（`backend/src/app/api/dramas/[id]/book/route.ts:16-28`、`backend/src/app/api/users/me/bookings/route.ts:10-28`）。
- `GET /api/users/me` 负责验证当前 access token 并恢复用户摘要；移动端 booking 页面在匿名 / 过期状态下会先通过登录闭环恢复会话，再请求本接口（`backend/src/app/api/users/me/route.ts:7-13`、`android/app/src/main/java/com/djs66256/short_drama/feature/booking/viewmodel/BookingAssetsViewModel.kt:150-192`、`ios/ShortDrama/Sources/Features/BookingAssets/ViewModels/BookingAssetsViewModel.swift:38-57`）。

---

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-30 | 初始创建：收录 PRD-11 当前用户预约资产接口，补充 `GET /api/users/me/bookings` 的鉴权、query/default、summary 聚合、状态映射、snake_case 分页与测试覆盖情况 |

---

*本文档由 llm-wiki skill 自动维护。*