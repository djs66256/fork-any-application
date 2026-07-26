# Dramas API 文档

> 最后更新：2026-07-26

---

## GET /api/dramas

### 功能简介

获取首页信息流短剧列表。当前已从“骨架返回空数组”演进为可分页返回首页卡片数据的列表接口，是 Android / iOS Native 首页 Feed 的唯一数据来源。

### 代码文件路径

- Route：`backend/src/app/api/dramas/route.ts:8-24`
- Service：`backend/src/services/drama/drama.service.ts:5-10`
- Repository：`backend/src/repositories/mock/drama.mock.repository.ts:4-180`
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

> 说明：示例响应反映当前代码中的字段形态与分页结构；真实列表由 mock repository 中的 12 条预置短剧分页切片得到（`backend/src/repositories/mock/drama.mock.repository.ts:4-180`）。

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

## POST /api/dramas

### 功能简介

创建短剧。当前仍为占位接口，返回 501 Not Implemented，不属于 PRD-02 首页信息流范围。

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

获取短剧详情。当前仍为 501 占位接口；首页卡片的“详情”入口现阶段只复用客户端既有占位详情页路由，不依赖该接口。

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
| 2026-07-26 | 更新：`GET /api/dramas` 从空骨架修正为首页 Feed 列表接口，补充 canonical query、首页卡片字段、12 条 mock 数据分页行为与 `VALIDATION_ERROR` 校验错误码 |
| 2026-07-24 | 初始创建，项目初始化阶段新增 3 个 dramas API 端点 |

---

*本文档由 llm-wiki skill 自动维护。*