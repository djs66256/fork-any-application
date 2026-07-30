# 实现计划：Backend — PRD-11 个人资产管理

> 创建日期：2026-07-30
> 对应技术方案：design-backend.md
> 对应需求：spec.md

## 概述

本期 Backend 只聚焦“当前登录用户预约资产列表”读取链路，新增 `GET /api/users/me/bookings` 及其配套 schema、repository contract、Supabase 查询映射、service 与测试，不扩展下载能力，也不主动实现 429 限流逻辑。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 以下验证命令均基于 `backend/` 目录既有脚本执行。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 预约资产查询 schema 应提供默认值并校验边界 | `status/page/pageSize` 缺省或非法 | 缺省时得到 `online/1/20`；非法值抛出校验错误 | 单元测试 | P0 |
| T-02 | 预约资产响应 schema 应约束列表与 summary 结构 | 合法/非法的 `{ data, pagination, summary }` | 合法数据通过；非法字段或类型漂移被拒绝 | 单元测试 | P0 |
| T-03 | Repository contract 能闭合预约资产读取能力 | service 调用 `listUserBookings` | 接口与 mock 实现可编译，参数为 `userId/status/page/pageSize` | 单元测试 | P0 |
| T-04 | Supabase repository 能按用户读取并映射 online/upcoming | 当前用户 booking join drama 行数据 | 返回 `BookingAssetListResponse`，按 `booked_at DESC` 排序，`announced -> upcoming`、`ongoing/completed -> online` | 单元测试 | P0 |
| T-05 | Supabase repository 会过滤无效 booking 与未知状态 | join 失败记录、未知 `dramas.status` | 脏数据不进入 `data`，也不计入 `summary` | 单元测试 | P0 |
| T-06 | Service 会校验 repository 结果并透传领域错误 | repository 返回合法结果、非法 shape、`AppError` | 合法结果透传；非法 shape 包装为 `INTERNAL_ERROR`；`AppError` 原样抛出 | 单元测试 | P0 |
| T-07 | Route 默认 query、生鉴权与响应契约正确 | 已登录/未登录请求；缺省 query；非法 query | 已登录返回 200 与 `{ data, pagination, summary }`；未登录 401；非法 query 400 | 单元测试 | P0 |
| T-08 | 空列表与超大页码保持列表 contract 稳定 | 无数据用户或 `page` 超过总页数 | 返回 `200 + data: []`，且 `summary/pagination` 仍正确 | 单元测试 | P1 |
| T-09 | 数据源异常会被收口为可预期错误 | Supabase 查询失败 | repository 抛 `SERVICE_UNAVAILABLE` 或受控内部错误，route 返回统一错误结构 | 单元测试 | P1 |

## 实现步骤

### Step 1：补齐预约资产共享 schema 与 schema 测试

- **关联测试**：T-01、T-02
- **目标文件**：`backend/src/lib/schemas.ts`、`backend/src/lib/__tests__/schemas.test.ts`
- **实现内容**：
  1. 在 `backend/src/lib/schemas.ts` 新增 `BookingAssetAvailabilityStatusSchema`、`BookingAssetQuerySchema`、`BookingAssetSchema`、`BookingAssetSummarySchema`、`BookingAssetListResponseSchema` 及对应类型。
  2. 明确 query 默认值与边界：`status` 默认 `online`、`page >= 1`、`1 <= pageSize <= 20`。
  3. 固化响应结构：`data[] + pagination + summary`，其中 `pagination` 继续沿用现有 snake_case contract。
  4. 在 `backend/src/lib/__tests__/schemas.test.ts` 增加合法/非法输入覆盖，确保 contract 不漂移。
- **验证方式**：
  - 在 `backend/` 目录执行 `npm run test -- src/lib/__tests__/schemas.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增预约资产 query/response schema 与类型 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 覆盖默认值、边界校验、响应结构校验 |

### Step 2：扩展 repository contract，并补齐 mock 闭合实现

- **关联测试**：T-03
- **目标文件**：`backend/src/repositories/interfaces/drama.repository.interface.ts`、`backend/src/repositories/mock/drama.mock.repository.ts`
- **实现内容**：
  1. 在 `drama.repository.interface.ts` 新增 `ListUserBookingsParams` 与 `listUserBookings()` contract，参数固定为 `userId/status/page/pageSize`。
  2. 明确 repository 返回值直接对齐 `BookingAssetListResponse`，避免再包一层通用分页类型导致 `summary` 丢失。
  3. 在 `DramaMockRepository` 中补齐最小闭合实现，优先采用显式 `notImplemented` 或最小占位实现，保证接口扩展后 service/route 可编译。
  4. 不改动 repository registry 的默认 mock 选择逻辑；预约资产真实读取仍由 route 直连 Supabase repository。
- **验证方式**：
  - 在 `backend/` 目录执行 `npm run test -- src/services/drama/drama.service.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 修改 | 新增预约资产读取 contract 与参数类型 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 补齐接口闭合实现，避免编译缺口 |

### Step 3：实现 Supabase repository 查询、状态映射与脏数据过滤

- **关联测试**：T-04、T-05、T-08、T-09
- **目标文件**：`backend/src/repositories/supabase/drama.supabase.repository.ts`、`backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts`
- **实现内容**：
  1. 在 `DramaSupabaseRepository` 中新增 `listUserBookings()`，按 `user_id` 查询 `bookings` 并联查 `dramas`。
  2. 设计两段读取：一段生成同用户口径的 `summary`，一段生成当前 `status` 下的分页列表；两段都只统计可成功 join 到 `dramas` 的有效 booking。
  3. 新增受控状态映射：`announced -> upcoming`、`ongoing/completed -> online`；未知状态过滤并记录 warning。
  4. 保证排序为 `booked_at DESC`，必要时用 `drama_id DESC` 做次排序，确保结果稳定。
  5. 将 Supabase 行数据映射到 `BookingAssetSchema`，并把基础设施错误转换为受控错误，避免 route 直接暴露底层异常。
  6. 在仓储测试中覆盖：状态映射、summary 统计、空列表、超大页码、脏数据过滤、数据源异常。
- **验证方式**：
  - 在 `backend/` 目录执行 `npm run test -- src/repositories/supabase/__tests__/drama.supabase.repository.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 修改 | 新增 booking assets 查询、映射、过滤与错误收口 |
| `backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts` | 修改 | 覆盖查询映射、排序、summary 与异常路径 |

### Step 4：在 DramaService 中新增预约资产列表能力

- **关联测试**：T-06
- **目标文件**：`backend/src/services/drama/drama.service.ts`、`backend/src/services/drama/drama.service.test.ts`
- **实现内容**：
  1. 在 `DramaService` 中新增 `listUserBookings()`，调用 repository 并使用 `BookingAssetListResponseSchema` 做最终结果校验。
  2. 保持现有 service 错误策略：`AppError` 直接透传，schema 漂移或未知异常包装为 `INTERNAL_ERROR`。
  3. 在 `drama.service.test.ts` 中补充成功、非法 shape、`AppError` 透传等测试，确保 service 成为 route 与 repository 之间的 contract 护栏。
- **验证方式**：
  - 在 `backend/` 目录执行 `npm run test -- src/services/drama/drama.service.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/services/drama/drama.service.ts` | 修改 | 新增 `listUserBookings()` 业务入口 |
| `backend/src/services/drama/drama.service.test.ts` | 修改 | 覆盖 schema 校验与错误包装行为 |

### Step 5：新增 `/api/users/me/bookings` route 并补齐路由测试

- **关联测试**：T-07、T-08、T-09
- **目标文件**：`backend/src/app/api/users/me/bookings/route.ts`、`backend/src/app/api/__tests__/users-me-bookings.test.ts`
- **实现内容**：
  1. 新增 `GET /api/users/me/bookings` route，沿用 `withErrorHandler` + `requireAuthContext` + `getAuth(request)` 的既有鉴权链路。
  2. 在 route 层解析 `status/page/pageSize`，使用 `BookingAssetQuerySchema` 提供默认值与非法参数拦截。
  3. route 内显式实例化 `DramaSupabaseRepository`，再调用 `DramaService.listUserBookings()`，避免误走默认 mock registry。
  4. 路由测试覆盖：默认 query、生鉴权 401、非法 query 400、成功响应结构、空列表、超大页码、service 抛错路径。
  5. 仅在注释/测试说明中保留 429 contract 预留，不在本步骤加入主动限流实现。
- **验证方式**：
  - 在 `backend/` 目录执行 `npm run test -- src/app/api/__tests__/users-me-bookings.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/app/api/users/me/bookings/route.ts` | 新增 | 新增当前用户预约资产列表接口 |
| `backend/src/app/api/__tests__/users-me-bookings.test.ts` | 新增 | 覆盖 query、鉴权、成功/失败响应契约 |

### Step 6：做预约资产链路回归验证并收口实现

- **关联测试**：T-01 ～ T-09
- **目标文件**：本步骤不新增业务文件，聚焦回归与必要的小幅修正
- **实现内容**：
  1. 串行运行 schema、repository、service、route 相关测试，修正 contract 漂移与命名不一致问题。
  2. 运行 backend 全量测试，确认新增预约资产能力未破坏现有 dramas、users、auth 相关接口。
  3. 运行 lint 与 build，确保新增 route、service、repository 与 schema 在类型和工程层面可通过。
  4. 回看实现范围，确认没有把历史预约写接口的 DTO 变更混入本期计划。
- **验证方式**：
  - 在 `backend/` 目录执行 `npm run test -- src/lib/__tests__/schemas.test.ts`
  - 在 `backend/` 目录执行 `npm run test -- src/repositories/supabase/__tests__/drama.supabase.repository.test.ts`
  - 在 `backend/` 目录执行 `npm run test -- src/services/drama/drama.service.test.ts`
  - 在 `backend/` 目录执行 `npm run test -- src/app/api/__tests__/users-me-bookings.test.ts`
  - 在 `backend/` 目录执行 `npm run test`
  - 在 `backend/` 目录执行 `npm run lint`
  - 在 `backend/` 目录执行 `npm run build`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 复核 | 校准 query/response contract |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 复核 | 校准接口签名 |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 复核 | 校准查询与映射细节 |
| `backend/src/services/drama/drama.service.ts` | 复核 | 校准错误收口 |
| `backend/src/app/api/users/me/bookings/route.ts` | 复核 | 校准 route 输入输出 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5 ──▶ Step 6
```

## 验证总览

- [ ] 在 `backend/` 目录执行 `npm run test -- src/lib/__tests__/schemas.test.ts`
- [ ] 在 `backend/` 目录执行 `npm run test -- src/repositories/supabase/__tests__/drama.supabase.repository.test.ts`
- [ ] 在 `backend/` 目录执行 `npm run test -- src/services/drama/drama.service.test.ts`
- [ ] 在 `backend/` 目录执行 `npm run test -- src/app/api/__tests__/users-me-bookings.test.ts`
- [ ] 在 `backend/` 目录执行 `npm run test`
- [ ] 在 `backend/` 目录执行 `npm run lint`
- [ ] 在 `backend/` 目录执行 `npm run build`
- [ ] 确认本期未主动实现 429 限流，只保留 contract 预留说明

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增预约资产 schema 与类型 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 补充预约资产 schema 测试 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 修改 | 新增 `listUserBookings` contract |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 补齐接口闭合实现 |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 修改 | 实现 booking assets Supabase 查询与映射 |
| `backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts` | 修改 | 增加仓储映射与过滤测试 |
| `backend/src/services/drama/drama.service.ts` | 修改 | 新增预约资产 service 方法 |
| `backend/src/services/drama/drama.service.test.ts` | 修改 | 增加 service contract 测试 |
| `backend/src/app/api/users/me/bookings/route.ts` | 新增 | 新增预约资产读取 route |
| `backend/src/app/api/__tests__/users-me-bookings.test.ts` | 新增 | 新增 route 契约测试 |