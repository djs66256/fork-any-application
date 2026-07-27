# Dramas API 文档

> 最后更新：2026-07-27

---

## GET /api/dramas

### 功能简介

获取首页信息流短剧列表。当前已从“骨架返回空数组”演进为可分页返回首页卡片数据的列表接口，是 Android / iOS Native 首页 Feed 的唯一数据来源；PRD-04 搜索结果页继续复用同一 `Drama` 列表契约，因此该接口与搜索接口共享响应字段语义。

### 代码文件路径

- Route：`backend/src/app/api/dramas/route.ts:8-24`
- Service：`backend/src/services/drama/drama.service.ts:5-10`
- Repository：`backend/src/repositories/mock/drama.mock.repository.ts:4-234`
- Schema：`backend/src/lib/schemas.ts:15-39`
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

> 说明：示例响应反映当前代码中的字段形态与分页结构；真实列表由 mock repository 中的 12 条预置短剧分页切片得到（`backend/src/repositories/mock/drama.mock.repository.ts:4-234`）。

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

## GET /api/dramas/search

### 功能简介

按关键词搜索短剧。PRD-04 搜索发现页与搜索结果页都依赖该接口；接口继续返回与首页 `GET /api/dramas` 相同的 `DramaListResponse` 结构，因此移动端搜索结果可以直接复用首页卡片组件、播放路由与详情路由（`backend/src/app/api/dramas/search/route.ts:7-19`、`backend/src/services/drama/drama.service.ts:12-20`）。

### 代码文件路径

- Route：`backend/src/app/api/dramas/search/route.ts:1-20`
- Service：`backend/src/services/drama/drama.service.ts:12-20`
- Repository：`backend/src/repositories/mock/drama.mock.repository.ts:173-223`
- Schema：`backend/src/lib/schemas.ts:57-77`
- 路由测试：`backend/src/app/api/__tests__/dramas-search.test.ts:34-111`
- Service 测试：`backend/src/services/drama/drama.service.test.ts:152-199`

### path / method

`GET /api/dramas/search`

### Query 参数

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `q` | string | 是 | — | 搜索关键词；会先 `trim()`，要求长度 1~50 |
| `page` | number | 否 | 1 | 页码（int，min 1） |
| `pageSize` | number | 否 | 10 | 每页数量（int，min 1，max 100） |

> 参数规则由 `SearchDramaQuerySchema` 统一定义：`q.trim().min(1).max(50)`、`page>=1`、`pageSize<=100`（`backend/src/lib/schemas.ts:57-61`）。

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
      "updated_at": "2026-07-25T00:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "page_size": 10,
    "total": 1,
    "total_pages": 1
  }
}
```

> 说明：响应结构与首页列表接口完全一致，便于客户端复用首页 Feed 卡片组件和 `Drama` 字段映射（`backend/src/app/api/__tests__/dramas-search.test.ts:35-53`、`backend/src/lib/schemas.ts:45-61`）。

### 当前行为说明

- Route 层当前直接实例化 `DramaMockRepository`，搜索数据仍来自 mock repository，而不是 Supabase repository（`backend/src/app/api/dramas/search/route.ts:15-17`）。
- 搜索匹配规则为 `title` + `category` 的大小写不敏感 contains 匹配：先对 query/title/category 做 `trim().toLocaleLowerCase()`，再执行 `includes`（`backend/src/repositories/mock/drama.mock.repository.ts:173-175,206-216`）。
- 关键词会在进入 service 前完成 `trim()`；例如 `q=%20逆袭%20` 最终按 `逆袭` 查询（`backend/src/app/api/__tests__/dramas-search.test.ts:35-53`）。
- 超大页码返回 `200 + data=[]`，分页信息保留实际 `page/page_size/total/total_pages`，不视为异常（`backend/src/app/api/__tests__/dramas-search.test.ts:55-80`、`backend/src/services/drama/drama.service.test.ts:166-176`）。
- Service 层会用 `DramaListResponseSchema` 再次校验 repository 输出，非法结构会被包装成 `INTERNAL_ERROR`（`backend/src/services/drama/drama.service.ts:12-20`、`backend/src/services/drama/drama.service.test.ts:186-199`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功（包含空结果和超大页码空结果） |
| 400 | `VALIDATION_ERROR` | `q` 为空、`page<1`、`pageSize>100` 等参数非法 |
| 500 | `INTERNAL_ERROR` | service 抛错或 schema 校验失败 |

---

## GET /api/dramas/hot-search

### 功能简介

返回搜索发现页热搜榜数据。该接口为 Android / iOS 搜索发现首页的热搜区块提供关键词、排名与热度分值，当前数据来自 mock repository 内置的 10 条热搜种子（`backend/src/app/api/dramas/hot-search/route.ts:6-11`、`backend/src/repositories/mock/drama.mock.repository.ts:151-164,219-223`）。

### 代码文件路径

- Route：`backend/src/app/api/dramas/hot-search/route.ts:1-12`
- Service：`backend/src/services/drama/drama.service.ts:22-30`
- Repository：`backend/src/repositories/mock/drama.mock.repository.ts:151-164,219-223`
- Schema：`backend/src/lib/schemas.ts:65-77`
- 路由测试：`backend/src/app/api/__tests__/dramas-hot-search.test.ts:18-43`
- Service 测试：`backend/src/services/drama/drama.service.test.ts:178-199`

### path / method

`GET /api/dramas/hot-search`

### Query 参数

无。

### Response

```json
{
  "data": [
    { "rank": 1, "keyword": "逆袭", "score": 9821 },
    { "rank": 2, "keyword": "豪门", "score": 9540 },
    { "rank": 3, "keyword": "总裁", "score": 9300 }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `data` | array | 热搜列表，最多 10 条 |
| `data[].rank` | number | 排名，从 1 开始 |
| `data[].keyword` | string | 热搜关键词，长度 1~50 |
| `data[].score` | number | 热度分值，int，min 0 |

### 当前行为说明

- Route 层当前直接实例化 `DramaMockRepository`，尚未接入真实热搜聚合逻辑（`backend/src/app/api/dramas/hot-search/route.ts:7-9`）。
- Schema 约束返回数组最多 10 条（`backend/src/lib/schemas.ts:65-75`）。
- 当前 mock 热搜 seed 固定为 10 条：`逆袭`、`豪门`、`总裁`、`甜宠`、`重生`、`穿书`、`都市`、`校园`、`复仇`、`萌宝`（`backend/src/repositories/mock/drama.mock.repository.ts:151-164`）。
- Service 层会用 `HotSearchListResponseSchema` 再次校验 repository 输出，非法结构会被包装成 `INTERNAL_ERROR`（`backend/src/services/drama/drama.service.ts:22-30`、`backend/src/services/drama/drama.service.test.ts:194-199`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功 |
| 500 | `INTERNAL_ERROR` | service 抛错或 schema 校验失败 |

---

## POST /api/dramas

### 功能简介

创建短剧。当前仍为占位接口，返回 501 Not Implemented，不属于 PRD-02 首页信息流或 PRD-04 搜索发现范围。

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

获取短剧详情。当前仍为 501 占位接口；首页卡片和搜索结果卡片的“详情”入口现阶段只复用客户端既有占位详情页路由，不依赖该接口。

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
| 2026-07-27 | 更新：新增 `GET /api/dramas/search` 与 `GET /api/dramas/hot-search` 文档，补充搜索 query 校验、title/category 不区分大小写 contains 匹配、超大页码 `200 + data=[]` 行为、热搜 Top 10 种子和 mock repository 现状 |
| 2026-07-26 | 更新：`GET /api/dramas` 从空骨架修正为首页 Feed 列表接口，补充 canonical query、首页卡片字段、12 条 mock 数据分页行为与 `VALIDATION_ERROR` 校验错误码 |
| 2026-07-24 | 初始创建，项目初始化阶段新增 3 个 dramas API 端点 |

---

*本文档由 llm-wiki skill 自动维护。*
