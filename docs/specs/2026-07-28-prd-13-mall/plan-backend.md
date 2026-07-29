# 实现计划：Backend — PRD-13 商城

> 创建日期：2026-07-28
> 对应技术方案：design-backend.md
> 对应需求：spec.md

## 概述

本期 Backend 端聚焦交付商城首页首版只读数据接口 `GET /api/mall/products`，采用现有 Route → Service → Repository 四层架构落地，并以 mock/seed 数据先固化分页 contract，不引入真实 migration。实现过程遵循轻量 TDD：先补 schema、仓储、service、route 的测试场景，再逐层实现，最后统一执行 backend 定向测试、lint 与 build 验证。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 商城商品 query/schema 合法输入可通过解析 | `page=1,pageSize=20`，合法 `MallProduct[]` 与 `pagination` | `MallProductsQuerySchema`、`MallProductSchema`、`MallProductsResponseSchema` 解析成功并保留默认值/字段约束 | 单元测试 | P0 |
| T-02 | 商城商品 query/schema 非法输入被拒绝 | `page=0`、`pageSize=101`、非法 UUID、非法 URL、负价格 | Zod 抛出校验错误，不接受脏数据进入后续层 | 单元测试 | P0 |
| T-03 | mock 仓储按固定顺序返回分页结果 | 默认 seed 商品集，分别请求第 1 页、第 2 页、超大页码 | 页内顺序稳定、分页切片正确、超大页码返回 `200 语义` 的空数组分页结构 | 单元测试 | P0 |
| T-04 | service 能校验仓储输出并映射异常 | stub repository 返回合法结果或非法结构 | 合法结果按 `MallProductsResponseSchema` 返回；非法结构转换为 `INTERNAL_ERROR` | 单元测试 | P0 |
| T-05 | 路由在缺省 query 下返回首屏分页结果 | `GET /api/mall/products` | 返回 `200`，`pagination.page=1`、`pagination.page_size=20`，并透传 service 结果 | 单元测试 | P0 |
| T-06 | 路由对非法参数返回统一校验错误 | `GET /api/mall/products?page=0&pageSize=101` | 返回 `400 + VALIDATION_ERROR`，错误格式由 `withErrorHandler` 统一输出 | 单元测试 | P0 |
| T-07 | 路由对超大页码保留空态 contract | `GET /api/mall/products?page=999&pageSize=20` | 返回 `200 + data=[] + 合法 pagination`，不降级为 4xx/5xx | 单元测试 | P1 |

## 实现步骤

### Step 1：固化商城商品 contract 与 schema

- **关联测试**：T-01、T-02
- **目标文件**：`backend/src/lib/schemas.ts`、`backend/src/lib/__tests__/schemas.test.ts`、`backend/src/repositories/interfaces/mall.repository.interface.ts`
- **实现内容**：
  1. 先在 `backend/src/lib/__tests__/schemas.test.ts` 中新增商城商品 query、实体、响应结构的合法/非法解析用例，覆盖默认分页、非法页码、非法 `image_url`、非法 `id`、负价格、标签数组边界等场景。
  2. 在 `backend/src/lib/schemas.ts` 中新增 `MallProductSchema`、`MallProductsQuerySchema`、`MallProductsResponseSchema` 及对应 TypeScript 类型，复用现有 `PaginationSchema`。
  3. 新增 `backend/src/repositories/interfaces/mall.repository.interface.ts`，定义商城仓储最小接口，例如 `listProducts(params)` 的输入输出契约，确保后续 repository/service 都基于同一类型收敛。
  4. 明确本期不新增 migration，商城数据 contract 先以 schema 作为唯一权威来源。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/lib/__tests__/schemas.test.ts` 确认 T-01、T-02 通过 ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增商城商品 query、实体、响应 schema 与类型 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 增补商城 schema 合法/非法输入测试 |
| `backend/src/repositories/interfaces/mall.repository.interface.ts` | 新增 | 定义商城仓储接口与返回契约 |

### Step 2：落地商城 mock 仓储与 registry 接线

- **关联测试**：T-03
- **目标文件**：`backend/src/repositories/mock/mall.mock.repository.ts`、`backend/src/repositories/__tests__/mall.mock.repository.test.ts`、`backend/src/repositories/repository-registry.ts`
- **实现内容**：
  1. 先在 `backend/src/repositories/__tests__/mall.mock.repository.test.ts` 中定义固定顺序分页、空 seed、超大页码、分页总数与无重复切片等场景。
  2. 新增 `backend/src/repositories/mock/mall.mock.repository.ts`，提供 20 条以上商城商品 seed 数据，并实现基于 `page/pageSize` 的确定性切片逻辑。
  3. 修改 `backend/src/repositories/repository-registry.ts`，新增 `createDefaultMallRepository()`、`getMallRepository()`、`setMallRepository()`，使 route/service 可按现有 registry 模式接入 mall repository。
  4. 保持首版数据源默认走 mock repository，不在 route 层硬编码 seed 数据。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/repositories/__tests__/mall.mock.repository.test.ts` 确认 T-03 通过 ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/repositories/mock/mall.mock.repository.ts` | 新增 | 提供商城商品 seed、分页切片与稳定排序实现 |
| `backend/src/repositories/__tests__/mall.mock.repository.test.ts` | 新增 | 覆盖固定顺序、分页边界、空态与超大页码测试 |
| `backend/src/repositories/repository-registry.ts` | 修改 | 注册 mall repository 默认实现与测试替换入口 |

### Step 3：封装 MallService 并补齐业务单元测试

- **关联测试**：T-04
- **目标文件**：`backend/src/services/mall/mall.service.ts`、`backend/src/services/mall/mall.service.test.ts`
- **实现内容**：
  1. 先在 `backend/src/services/mall/mall.service.test.ts` 中通过 stub repository 定义合法结果、非法结构结果、预期 `AppError` 透传等测试场景。
  2. 新增 `backend/src/services/mall/mall.service.ts`，实现 `listProducts(params)`，对 repository 返回值执行 `MallProductsResponseSchema.parse(...)`。
  3. 沿用现有 service 风格：对已知 `AppError` 直接透传；对 schema 解析失败或未知异常统一转换为 `Errors.internal('Invalid mall products result')`。
  4. 保持 service 只负责业务 contract 与异常映射，不把 query 解析或 HTTP 细节下沉到 service。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/services/mall/mall.service.test.ts` 确认 T-04 通过 ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/services/mall/mall.service.ts` | 新增 | 封装商城商品列表读取、schema 校验与异常映射 |
| `backend/src/services/mall/mall.service.test.ts` | 新增 | 覆盖合法结果、非法仓储输出、异常透传测试 |

### Step 4：暴露商城列表路由并完成 API 场景验证

- **关联测试**：T-05、T-06、T-07
- **目标文件**：`backend/src/app/api/mall/products/route.ts`、`backend/src/app/api/__tests__/mall-products.test.ts`
- **实现内容**：
  1. 先在 `backend/src/app/api/__tests__/mall-products.test.ts` 中编写 route handler 测试，覆盖缺省参数成功返回、非法 query 返回 `VALIDATION_ERROR`、超大页码空数组分页、service 未知异常返回 `INTERNAL_ERROR` 等场景。
  2. 新增 `backend/src/app/api/mall/products/route.ts`，复用 `withErrorHandler`，解析 `searchParams` 后交给 `MallProductsQuerySchema.parse(...)`。
  3. 在 route 中实例化 `MallService(getMallRepository())` 并返回 `NextResponse.json(result)`，保持与现有 `dramas/rankings` 等路由一致的结构。
  4. 确认公开只读接口不接入 auth middleware，返回格式严格对齐 `design-backend.md` 中的 `data + pagination` contract。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/mall-products.test.ts` 确认 T-05、T-06、T-07 通过 ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/app/api/mall/products/route.ts` | 新增 | 新增商城商品分页 GET 路由 |
| `backend/src/app/api/__tests__/mall-products.test.ts` | 新增 | 覆盖默认分页、校验错误、空态、异常响应测试 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4
```

- Step 1 先固化 schema 与接口 contract，后续 repository/service/route 均依赖该契约。
- Step 2 产出的 repository 是 Step 3 service 与 Step 4 route 的基础依赖。
- Step 3 完成后，Step 4 才能以稳定 service contract 暴露 HTTP 接口。

## 验证总览

- [x] 所有测试通过（`cd backend && npm test`；33 个 test files、289 个 tests 全通过）
- [x] Build 成功（`cd backend && npm run build`；产物包含 `ƒ /api/mall/products`）
- [x] 无新增 lint 错误（`cd backend && npm run lint`）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增商城商品 schema 与类型 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 增补商城 schema 测试 |
| `backend/src/repositories/interfaces/mall.repository.interface.ts` | 新增 | 商城仓储接口定义 |
| `backend/src/repositories/mock/mall.mock.repository.ts` | 新增 | 商城商品 mock 仓储实现 |
| `backend/src/repositories/__tests__/mall.mock.repository.test.ts` | 新增 | mock 仓储分页与边界测试 |
| `backend/src/repositories/repository-registry.ts` | 修改 | mall repository registry 接线 |
| `backend/src/services/mall/mall.service.ts` | 新增 | 商城 service |
| `backend/src/services/mall/mall.service.test.ts` | 新增 | service 单元测试 |
| `backend/src/app/api/mall/products/route.ts` | 新增 | 商城商品列表接口 |
| `backend/src/app/api/__tests__/mall-products.test.ts` | 新增 | 路由/API 场景测试 |
