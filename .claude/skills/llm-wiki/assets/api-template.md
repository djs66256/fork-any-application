# <业务域> API 文档

> 最后更新：YYYY-MM-DD

---

## <接口名称>

### 功能简介

[该接口做什么，一句话描述]

### 代码文件路径

`backend/.../xxx.ts:L30`

### path / method

`POST /api/xxx/:id`

### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | string | 是 | 资源 ID |

### Headers（如有）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `Authorization` | string | 是 | Bearer token |

### Query 参数

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `page` | number | 否 | 1 | 页码 |
| `size` | number | 否 | 20 | 每页条数 |

### Body 参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | 是 | 名称 |
| `type` | string | 否 | 类型 |

**示例：**

```json
{
  "name": "示例名称",
  "type": "video"
}
```

### Response

```json
{
  "id": "xxx",
  "name": "xxx",
  "createdAt": "2026-07-22T00:00:00Z"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 资源 ID |
| `name` | string | 名称 |
| `createdAt` | string | 创建时间（ISO 8601） |

### Response Headers（如有）

| 字段 | 类型 | 说明 |
|------|------|------|
| `X-RateLimit-Remaining` | number | 剩余请求次数 |

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功 |
| 400 | `INVALID_PARAMS` | 参数校验失败 |
| 401 | `UNAUTHORIZED` | 未登录 |
| 404 | `NOT_FOUND` | 资源不存在 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |


## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| YYYY-MM-DD | 初始创建 / [变更描述] |
---

*本文档由 llm-wiki skill 自动维护。*
