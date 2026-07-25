# 实现计划：Backend — PRD-02 首页信息流

> 创建日期：2026-07-25
> 对应技术方案：design-backend.md
> 对应需求：spec.md

## 概述

本期 Backend 端在现有 Next.js 四层结构内，围绕既有 `/api/dramas` 路由补齐首页 Feed 所需的 canonical contract、首页 mock 数据、`DramaSchema` 字段收口与分页回归测试；不新增依赖、不新增路由前缀，也不改变 `GET /api/dramas/[id]` 与 `POST /api/dramas` 当前 501 占位边界。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | `GET /api/dramas` 默认走 canonical contract | `GET /api/dramas` | 返回 `200`，响应外层为 `{ data, pagination }`，默认 `page=1`、`page_size=10` | 单元测试 | P0 |
| T-02 | 首页第一页返回可消费 mock 数据 | `GET /api/dramas?page=1&pageSize=10` | 返回至少 1 条短剧卡片，字段包含 `id/title/description/cover_url/category/episode_count/tags/rating/created_at/updated_at` | 单元测试 | P0 |
| T-03 | 分页切片与大页码行为正确 | `page=2&pageSize=10`、`page=999&pageSize=10` | 第二页返回稳定切片；超大页码返回空数组但 pagination 正确 | 单元测试 | P0 |
| T-04 | 非法分页参数被拦截 | `page=0` 或 `pageSize=101` | 返回 `400`，并保持当前 `withErrorHandler` 的校验错误结构 | 单元测试 | P0 |
| T-05 | `DramaSchema` 与列表响应 schema 收口到首页卡片字段集 | 合法首页卡片对象与列表响应对象 | Zod 解析通过；旧字段 `total_episodes` 不再作为首页 contract 必填字段 | 单元测试 | P0 |
| T-06 | 501 占位接口不回归 | `POST /api/dramas`、`GET /api/dramas/[id]` | 继续返回 `501` + `NOT_IMPLEMENTED` | 单元测试 | P0 |

## 实现步骤

### Step 1：先补齐接口回归测试，锁定 canonical contract 与错误边界

- **关联测试**：T-01、T-03、T-04、T-06
- **目标文件**：`backend/src/app/api/__tests__/dramas.test.ts`、`backend/src/app/api/__tests__/skeleton-endpoints.test.ts`
- **实现内容**：
  1. 先扩展 `dramas.test.ts`，把当前“空列表骨架”断言升级为首页 Feed 契约断言：默认分页、第一页成功态、第二页切片、大页码空数组。
  2. 增加非法参数测试，覆盖 `page=0`、`pageSize=101` 等 400 回归；断言以当前 `withErrorHandler` 的实际错误结构为准，避免计划与现状脱节。
  3. 在 API 测试中继续保留 `POST /api/dramas` 的 501 回归断言，并补一条 `GET /api/dramas/[id]` 的 501 回归，确保首页需求不会误改详情占位边界。
  4. 先让测试失败，再进入后续 schema 与 repository 实现，形成轻量 TDD 起点。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/dramas.test.ts`
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/skeleton-endpoints.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/app/api/__tests__/dramas.test.ts` | 修改 | 扩展 `/api/dramas` canonical contract、分页、非法参数回归测试 |
| `backend/src/app/api/__tests__/skeleton-endpoints.test.ts` | 修改 | 增加 `GET /api/dramas/[id]` 501 回归覆盖 |

### Step 2：收口 shared schema 到首页卡片 contract

- **关联测试**：T-05
- **目标文件**：`backend/src/lib/schemas.ts`、`backend/src/lib/__tests__/schemas.test.ts`
- **实现内容**：
  1. 将 `DramaSchema` 从当前骨架字段集收口到首页实际需要的卡片字段集，重点把 `total_episodes` 统一为 `episode_count`，补齐 `tags`，并维持 `id` 为 UUID、时间字段为字符串。
  2. 同步更新 `DramaListResponseSchema` 的示例与测试输入，确保列表响应以 `{ data, pagination }` 为唯一公开结构。
  3. 在 schema 测试中补充合法首页卡片对象、列表响应对象以及旧字段名不再满足新 contract 的断言，先从 shared 层把接口语义锁住。
  4. 保持改动局限在现有 shared 文件，不引入新 schema 文件或额外依赖。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/lib/__tests__/schemas.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 将 `DramaSchema` 收口到首页 canonical 字段集，补齐 `episode_count` 与 `tags` |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 增加首页卡片 schema 与列表响应 schema 的正反向断言 |

### Step 3：为 mock repository 注入稳定首页数据并校验分页切片

- **关联测试**：T-02、T-03、T-05
- **目标文件**：`backend/src/repositories/mock/drama.mock.repository.ts`、`backend/src/repositories/__tests__/drama.mock.repository.test.ts`、`backend/src/services/drama/drama.service.test.ts`
- **实现内容**：
  1. 在 `DramaMockRepository` 中将当前空 `Map` 演进为带稳定初始数据集的内存仓库，准备 10~20 条首页可用 mock dramas，覆盖多分类、空标签或空封面等首屏容错场景。
  2. 保持现有 `findMany` 分页切片逻辑，但让其基于稳定数据返回可预测的 `total`、`total_pages` 与页内内容顺序，便于接口测试复用。
  3. 更新 repository/service 测试，验证第一页有数据、第二页切片正确、大页码为空但不报错，并确认 service 仍保持薄编排层。
  4. 不新增独立数据源或 seed 机制，mock 数据直接留在现有 mock repository 内维护。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/repositories/__tests__/drama.mock.repository.test.ts`
  - 运行 `cd backend && npm run test -- src/services/drama/drama.service.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 注入稳定首页 mock 数据并维持可预测分页切片 |
| `backend/src/repositories/__tests__/drama.mock.repository.test.ts` | 修改 | 增加 mock 数据分页、大页码、字段完整性测试 |
| `backend/src/services/drama/drama.service.test.ts` | 修改 | 校验 service 经由 repository 返回首页分页数据 |

### Step 4：回写 route/service 输出，完成 `/api/dramas` 首页契约收口

- **关联测试**：T-01、T-02、T-03、T-04、T-06
- **目标文件**：`backend/src/app/api/dramas/route.ts`、`backend/src/services/drama/drama.service.ts`
- **实现内容**：
  1. 保持 `/api/dramas`、`page/pageSize` 与 `{ data, pagination }` 外层结构不变，只把 route 输出与更新后的 schema / mock repository 对齐。
  2. 视需要在 route 或 service 出口增加一次 `DramaListResponseSchema` 级别的解析，确保 mock 数据与响应结构在出站前完成 schema 收口。
  3. 明确本期仅实现列表接口；`POST /api/dramas` 与 `GET /api/dramas/[id]` 不做功能扩展，只确保 501 回归测试持续通过。
  4. 跑通本期 Backend 相关测试与 build，作为计划收尾验证。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/dramas.test.ts`
  - 运行 `cd backend && npm run test -- src/services/drama/drama.service.test.ts`
  - 运行 `cd backend && npm run test`
  - 运行 `cd backend && npm run build`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/app/api/dramas/route.ts` | 修改 | 对齐 canonical contract，并在出口收口首页列表响应 |
| `backend/src/services/drama/drama.service.ts` | 修改 | 保持薄 service，同时承接 schema 对齐后的列表输出 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4
```

## 验证总览

- [ ] 所有测试通过（`cd backend && npm run test`）
- [ ] Build 成功（`cd backend && npm run build`）
- [ ] 无新增 lint 错误（`cd backend && npm run lint`）
- [ ] `/api/dramas` 保持 canonical contract：`/api/dramas?page&pageSize` + `{ data, pagination }`
- [ ] 首页 mock 数据可稳定返回第一页与第二页切片
- [ ] 非法参数返回 400，且与当前错误处理中间件输出一致
- [ ] `POST /api/dramas` 与 `GET /api/dramas/[id]` 继续返回 501

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/app/api/__tests__/dramas.test.ts` | 修改 | 首页列表 canonical contract、分页、非法参数回归测试 |
| `backend/src/app/api/__tests__/skeleton-endpoints.test.ts` | 修改 | 501 占位接口回归测试 |
| `backend/src/lib/schemas.ts` | 修改 | 首页卡片 schema 收口 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | shared schema 回归测试 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 首页 mock 数据与分页切片 |
| `backend/src/repositories/__tests__/drama.mock.repository.test.ts` | 修改 | repository 分页与边界测试 |
| `backend/src/services/drama/drama.service.test.ts` | 修改 | service 列表输出回归测试 |
| `backend/src/app/api/dramas/route.ts` | 修改 | `/api/dramas` 列表输出与 schema 收口 |
| `backend/src/services/drama/drama.service.ts` | 修改 | 保持薄 service 并对齐新 contract |
