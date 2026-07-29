# Mall API 文档

> 最后更新：2026-07-29

---

## GET /api/mall/products

### 功能简介

获取商城首页双列商品 Feed 的分页数据。该接口是 Web `/mall` 商城首页的唯一商品数据来源，同时也是 Android / iOS 商城 H5 容器首屏与分页追加共同依赖的 backend contract。当前接口为公开只读接口，不要求登录；商品列表首版由 `MallMockRepository` 提供稳定顺序 seed 数据，并通过 `MallService` 再次做 response schema 校验，确保 route 输出始终符合 `{ data, pagination }` 的统一结构（`backend/src/app/api/mall/products/route.ts:1-18`、`backend/src/services/mall/mall.service.ts:1-23`、`backend/src/repositories/mock/mall.mock.repository.ts:1-218`）。

### 代码文件路径

- Route：`backend/src/app/api/mall/products/route.ts:1-18`
- Service：`backend/src/services/mall/mall.service.ts:1-23`
- Repository Contract：`backend/src/repositories/interfaces/mall.repository.interface.ts:1-15`
- Repository Registry：`backend/src/repositories/repository-registry.ts`
- Mock Repository：`backend/src/repositories/mock/mall.mock.repository.ts:1-218`
- Schema：`backend/src/lib/schemas.ts:45-67`
- Route 测试：`backend/src/app/api/__tests__/mall-products.test.ts:1-147`
- Service 测试：`backend/src/services/mall/mall.service.test.ts:1-91`
- Repository 测试：`backend/src/repositories/__tests__/mall.mock.repository.test.ts:1-93`

### path / method

`GET /api/mall/products`

### Query 参数

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `page` | number | 否 | 1 | 页码（int，min 1） |
| `pageSize` | number | 否 | 20 | 每页数量（int，min 1，max 100） |

### Response

```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440101",
      "title": "轻奢真丝睡衣礼盒",
      "image_url": "https://example.com/mall/products/pajama-gift-box.jpg",
      "price": 199,
      "tags": ["热卖", "包邮"]
    }
  ],
  "pagination": {
    "page": 1,
    "page_size": 20,
    "total": 25,
    "total_pages": 2
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `data` | array | 当前页商品卡数组 |
| `data[].id` | string(uuid) | 商品 UUID；同时作为 `/mall/product/[id]` 路由参数与 `MallLoginContext.productId` |
| `data[].title` | string | 商品标题 |
| `data[].image_url` | string(url) | 商品主图 URL |
| `data[].price` | number | 原始价格值，前端负责格式化展示 |
| `data[].tags` | string[] | 商品标签，当前最多 3 个 |
| `pagination.page` | number | 当前页码 |
| `pagination.page_size` | number | 当前页大小（snake_case） |
| `pagination.total` | number | 总记录数 |
| `pagination.total_pages` | number | 总页数 |

### 当前行为说明

- Route 使用 `MallProductsQuerySchema` 解析 `page/pageSize`，默认收口为 `page=1&pageSize=20`（`backend/src/app/api/mall/products/route.ts:8-13`）。
- Route 通过 `MallService(getMallRepository())` 获取数据，不在 route 层直接耦合 seed 数据来源（`backend/src/app/api/mall/products/route.ts:14-18`）。
- `MallService` 会在 repository 返回后再用 `MallProductsResponseSchema` 做结构校验；若 repository 返回非法 UUID、非法 URL 或其他结构错误，会统一包装为 `INTERNAL_ERROR`（`backend/src/services/mall/mall.service.ts:9-22`）。
- 当前默认仓库是 `MallMockRepository`：固定 25 条商品，按稳定顺序分页；`page=1&pageSize=20` 返回 20 条，第二页返回剩余 5 条（`backend/src/repositories/mock/mall.mock.repository.ts:4-218`、`backend/src/app/api/__tests__/mall-products.test.ts:17-44`）。
- 请求超大页码时仍返回 `200 + data=[]`，但保留合法分页信息；空仓库场景下 `total_pages=0`（`backend/src/app/api/__tests__/mall-products.test.ts:67-87`、`backend/src/repositories/__tests__/mall.mock.repository.test.ts:59-76`）。
- `tags` 是可选数组字段，schema 默认值为空数组；repository 会 clone 返回结果，避免调用方污染内部 seed 状态（`backend/src/lib/schemas.ts:47-53`、`backend/src/repositories/__tests__/mall.mock.repository.test.ts:78-93`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功（含空列表和超大页码空结果） |
| 400 | `VALIDATION_ERROR` | `page` / `pageSize` 非法，例如 `page=0` 或 `pageSize=101` |
| 500 | `INTERNAL_ERROR` | 服务内部错误，或 repository 返回非法结构 |

### 调用方与依赖关系

| 调用方 | 调用方式 | 说明 |
|------|---------|------|
| Web `/mall` | `fetchMallProducts({ page, pageSize })` | 商城首页首屏与分页追加唯一商品数据源（`web/src/lib/mall/api.ts:1-14`） |
| Android Mall 容器 | 间接通过 Web H5 消费 | Native 容器不直接请求该接口，而是承载 `/mall` H5 页面 |
| iOS Mall 容器 | 间接通过 Web H5 消费 | Native 容器不直接请求该接口，而是承载 `/mall` H5 页面 |

### 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 初始创建：收录 `GET /api/mall/products` 的 query 默认值、商品字段契约、固定 25 条 seed 数据、超大页码空结果、`MallService` 二次校验与统一错误处理 |

---
*本文档由 llm-wiki skill 自动维护。*
