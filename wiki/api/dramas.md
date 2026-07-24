# Dramas API 文档

> 最后更新：2026-07-24

---

## GET /api/dramas

### 功能简介

获取短剧列表。当前骨架阶段返回空数组 + 分页元数据。

### 代码文件路径

`backend/src/app/api/dramas/route.ts:L13`

### path / method

`GET /api/dramas`

### Query 参数

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `page` | number | 否 | 1 | 页码（min 1） |
| `pageSize` | number | 否 | 10 | 每页数量（min 1, max 100） |

### Response

```json
{
  "data": [],
  "pagination": {
    "page": 1,
    "page_size": 10,
    "total": 0,
    "total_pages": 0
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `data` | array | 短剧数组 |
| `pagination.page` | number | 当前页码 |
| `pagination.page_size` | number | 每页数量 |
| `pagination.total` | number | 总记录数 |
| `pagination.total_pages` | number | 总页数 |

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功（含空列表） |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

---

## POST /api/dramas

### 功能简介

创建短剧。当前骨架阶段返回 501 Not Implemented。

### 代码文件路径

`backend/src/app/api/dramas/route.ts:L27`

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

获取短剧详情。当前骨架阶段返回 501。

### 代码文件路径

`backend/src/app/api/dramas/[id]/route.ts:L1`

### path / method

`GET /api/dramas/:id`

### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | string | 是 | 短剧 UUID |

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功 |
| 404 | `NOT_FOUND` | 短剧不存在 |
| 501 | `NOT_IMPLEMENTED` | 端点尚未实现 |

---

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-24 | 初始创建，项目初始化阶段新增 3 个 dramas API 端点 |

---

*本文档由 llm-wiki skill 自动维护。*
