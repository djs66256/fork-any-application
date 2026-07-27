# Dramas API 文档

> 最后更新：2026-07-27

---

## GET /api/dramas

### 功能简介

获取首页信息流短剧列表。当前已从“骨架返回空数组”演进为可分页返回首页卡片数据的列表接口，是 Android / iOS Native 首页 Feed 的唯一数据来源。

### 代码文件路径

- Route：`backend/src/app/api/dramas/route.ts:8-24`
- Service：`backend/src/services/drama/drama.service.ts:29-31`
- Repository：`backend/src/repositories/mock/drama.mock.repository.ts:325-328`
- Schema：`backend/src/lib/schemas.ts:15-28,61-73`
- 测试：`backend/src/app/api/__tests__/dramas.test.ts:6-92`

### path / method

`GET /api/dramas`

### Query 参数

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `page` | number | 否 | 1 | 页码（int，min 1） |
| `pageSize` | number | 否 | 10 | 每页数量（int，min 1，max 100） |

### Response

```json
{
  "data": [
    {
      "id": "11111111-1111-1111-1111-000000000001",
      "title": "重生之我在80年代当后妈",
      "description": "穿回八零年代后，她从保姆逆袭成全家团宠。",
      "cover_url": "https://images.example.com/dramas/retro-mom.jpg",
      "category": "年代",
      "episode_count": 68,
      "tags": ["重生", "家庭", "逆袭"],
      "rating": 8.9,
      "created_at": "2026-07-20T10:00:00.000Z",
      "updated_at": "2026-07-25T08:00:00.000Z"
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

> 说明：示例响应反映当前代码中的字段形态与分页结构；真实列表由 mock repository 中的 12 条预置短剧分页切片得到（`backend/src/repositories/mock/drama.mock.repository.ts:22-227,325-328`）。

| 字段 | 类型 | 说明 |
|------|------|------|
| `data` | array | 首页短剧卡片数组 |
| `data[].id` | string | 短剧 UUID |
| `data[].title` | string | 标题 |
| `data[].description` | string | 描述 |
| `data[].cover_url` | string \| null | 封面图 URL |
| `data[].category` | string | 分类 |
| `data[].episode_count` | number | 集数 |
| `data[].tags` | string[] | 标签列表 |
| `data[].rating` | number \| null | 评分 |
| `data[].created_at` | string | 创建时间 |
| `data[].updated_at` | string | 更新时间 |
| `pagination.page` | number | 当前页码 |
| `pagination.page_size` | number | 每页数量（注意响应仍为 snake_case） |
| `pagination.total` | number | 总记录数 |
| `pagination.total_pages` | number | 总页数 |

### 当前行为说明

- 默认请求 `GET /api/dramas` 返回第一页 10 条数据，当前总数为 12，总页数为 2（`backend/src/app/api/__tests__/dramas.test.ts:17-35`）。
- 请求 `GET /api/dramas?page=2&pageSize=10` 返回第 2 页剩余 2 条数据（`backend/src/app/api/__tests__/dramas.test.ts:37-59`）。
- 请求超大页码时仍返回 200，但 `data=[]`，分页信息保持正确（`backend/src/app/api/__tests__/dramas.test.ts:61-74`）。
- 当前 Android / iOS 客户端都只消费第一页，未实现下拉刷新或加载更多（`android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:50-66,102-106`；`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:14-17,72-81`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功（含空列表和大页码空结果） |
| 400 | `VALIDATION_ERROR` | 分页参数非法，例如 `page=0` 或 `pageSize=101` |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

---

## GET /api/dramas/rankings

### 功能简介

获取排行页榜单列表。当前接口是 Android / iOS 排行页的唯一数据来源，支持内容类型与榜单类型双维度筛选，以及标准分页返回。

### 代码文件路径

- Route：`backend/src/app/api/dramas/rankings/route.ts:8-24`
- Service：`backend/src/services/drama/drama.service.ts:44-56`
- Repository Contract：`backend/src/repositories/interfaces/drama.repository.interface.ts:19-25,45-50`
- Mock Repository：`backend/src/repositories/mock/drama.mock.repository.ts:343-356`
- Schema：`backend/src/lib/schemas.ts:30-44,75-105`
- 测试：`backend/src/app/api/__tests__/dramas-rankings.test.ts:39-138`

### path / method

`GET /api/dramas/rankings`

### Query 参数

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `type` | enum | 否 | `hot` | 榜单类型：`hot` / `recommend` / `booking` |
| `contentType` | enum | 否 | `all` | 内容类型：`all` / `live_action` / `ai` |
| `page` | number | 否 | 1 | 页码（int，min 1） |
| `pageSize` | number | 否 | 10 | 每页数量（int，min 1，max 100） |

### Response

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

> 说明：排行项在基础 `Drama` 字段上额外返回 `content_type / play_count / booking_count / recommendation_score / is_booked`，其中 `is_booked` 会在携带用户身份时按当前用户维度返回（`backend/src/lib/schemas.ts:36-42`, `backend/src/repositories/mock/drama.mock.repository.ts:347-355`）。

| 字段 | 类型 | 说明 |
|------|------|------|
| `data[].content_type` | enum | 内容类型：`live_action` / `ai` |
| `data[].play_count` | number | 热榜排序与展示的热度代理值 |
| `data[].booking_count` | number | 预约榜排序与展示的预约数 |
| `data[].recommendation_score` | number | 推荐榜排序与展示的推荐值 |
| `data[].is_booked` | boolean | 当前用户是否已预约；匿名请求固定返回 `false` |
| `pagination.*` | object | 与 `GET /api/dramas` 相同的统一分页结构 |

### 当前行为说明

- 默认请求 `GET /api/dramas/rankings` 会被解析为 `type=hot&contentType=all&page=1&pageSize=10`（`backend/src/lib/schemas.ts:90-97`, `backend/src/app/api/__tests__/dramas-rankings.test.ts:70-88`）。
- Repository 先按 `content_type` 过滤，再按榜单类型分别使用 `play_count`、`recommendation_score`、`booking_count` 降序排序；分值相同则按 `created_at` 倒序稳定打散（`backend/src/repositories/mock/drama.mock.repository.ts:294-314,343-356`）。
- 当请求携带 `x-user-id` 或 `Authorization: Bearer <user-id>` 时，Route 会把该值透传为可选 `authContext`，并据此计算 `is_booked`（`backend/src/app/api/dramas/rankings/route.ts:17-23`, `backend/src/middleware/auth.ts:16-23`）。
- 超大页码返回 200 + 空数组，不视为错误（`backend/src/app/api/__tests__/dramas-rankings.test.ts:90-115`）。
- 当前运行时数据源仍是 `DramaMockRepository`，不是 Supabase 实时数据（`backend/src/app/api/dramas/rankings/route.ts:17-23`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功（含空列表和大页码空结果） |
| 400 | `VALIDATION_ERROR` | `type` / `contentType` / 分页参数非法 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

---

## POST /api/dramas/[id]/book

### 功能简介

提交短剧预约。该接口服务于预约榜交互，当前要求调用方提供骨架态身份信息；成功后返回单向幂等的 `booked: true` 结果和最新预约数。

### 代码文件路径

- Route：`backend/src/app/api/dramas/[id]/book/route.ts:16-28`
- Auth：`backend/src/middleware/auth.ts:16-32`
- Service：`backend/src/services/drama/drama.service.ts:69-78`
- Repository：`backend/src/repositories/mock/drama.mock.repository.ts:364-395`
- Schema：`backend/src/lib/schemas.ts:99-105`
- 测试：`backend/src/app/api/__tests__/dramas-book.test.ts:18-133`

### path / method

`POST /api/dramas/:id/book`

### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | string | 是 | 短剧 UUID |

### Headers

| 字段 | 必填 | 说明 |
|------|------|------|
| `x-user-id` | 否 | 骨架态显式 userId；若存在则优先于 Authorization |
| `Authorization` | 否 | `Bearer <user-id>`；当前 token 不做 JWT 校验，直接当作 userId 使用 |

> 说明：本接口必须能解析出 userId，否则返回 401 `UNAUTHORIZED`（`backend/src/middleware/auth.ts:25-32`, `backend/src/app/api/__tests__/dramas-book.test.ts:66-77`）。

### Response

```json
{
  "drama_id": "550e8400-e29b-41d4-a716-446655440001",
  "booked": true,
  "booking_count": 821
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `drama_id` | string | 被预约的短剧 UUID |
| `booked` | literal `true` | 当前版本只支持“预约成功 / 已预约”语义，不支持取消 |
| `booking_count` | number | 预约后的最新预约数 |

### 当前行为说明

- 当 `x-user-id` 与 `Authorization` 同时存在时，优先使用 `x-user-id`（`backend/src/middleware/auth.ts:16-23`, `backend/src/app/api/__tests__/dramas-book.test.ts:47-64`）。
- 若同一用户重复预约同一短剧，接口保持幂等 success：返回 `booked: true`，但不会再次增加 `booking_count`（`backend/src/repositories/mock/drama.mock.repository.ts:370-377`）。
- 首次预约成功时，Repository 会在内存中累加 `booking_count`，并把目标项 `is_booked` 标记为 `true`（`backend/src/repositories/mock/drama.mock.repository.ts:379-394`）。
- 若 `id` 不存在，则返回 404 `NOT_FOUND`（`backend/src/repositories/mock/drama.mock.repository.ts:364-368`, `backend/src/app/api/__tests__/dramas-book.test.ts:95-113`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 预约成功或重复预约幂等成功 |
| 400 | `VALIDATION_ERROR` | `id` 不是合法 UUID |
| 401 | `UNAUTHORIZED` | 未提供可解析的 userId |
| 404 | `NOT_FOUND` | 短剧不存在 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

---

## POST /api/dramas

### 功能简介

创建短剧。当前仍为占位接口，返回 501 Not Implemented，不属于 PRD-05 排行体系范围。

### 代码文件路径

`backend/src/app/api/dramas/route.ts:26-28`

### path / method

`POST /api/dramas`

### Response (501)

```json
{
  "error": {
    "code": "NOT_IMPLEMENTED",
    "message": "POST /api/dramas not implemented"
  }
}
```

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 501 | `NOT_IMPLEMENTED` | 端点尚未实现 |

---

## GET /api/dramas/[id]

### 功能简介

获取短剧详情。当前仍为 501 占位接口；首页和排行列表的“详情 / 播放”链路现阶段都只复用客户端既有占位路由，不依赖该接口。

### 代码文件路径

`backend/src/app/api/dramas/[id]/route.ts:1-6`

### path / method

`GET /api/dramas/:id`

### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | string | 是 | 短剧 UUID |

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 501 | `NOT_IMPLEMENTED` | 端点尚未实现 |

---

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-27 | 更新：新增 `GET /api/dramas/rankings` 与 `POST /api/dramas/:id/book` 文档，补充排行 query、扩展字段、可选 auth 上下文、预约幂等行为与当前骨架态认证约束 |
| 2026-07-26 | 更新：`GET /api/dramas` 从空骨架修正为首页 Feed 列表接口，补充 canonical query、首页卡片字段、12 条 mock 数据分页行为与 `VALIDATION_ERROR` 校验错误码 |
| 2026-07-24 | 初始创建，项目初始化阶段新增 3 个 dramas API 端点 |

---

*本文档由 llm-wiki skill 自动维护。*