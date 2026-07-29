# 实现计划：Backend — PRD-14 赚钱中心

> 创建日期：2026-07-29
> 对应技术方案：design-backend.md
> 对应需求：spec.md

## 概述

本期 Backend 端聚焦交付赚钱中心首版最小闭环：新增 `GET /api/earn/overview` 与 `POST /api/earn/complete-task`，延续现有 Route → Service → Repository → Infrastructure/Shared 分层，统一通过 `withErrorHandler` + `AppError` 处理错误，且 `complete-task` 保持 Bearer-only 鉴权。首版数据源严格落在 mock repository 与受控 seed 数据，不新增真实 migration、不假设新的基础设施或外部依赖，先用自动化测试固化 contract，再逐层实现并完成 backend 定向测试、lint 与 build 验证。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> Backend 端需要覆盖 schema、repository、service、route 的核心行为、参数校验与数据转换。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | earn schema 合法输入可通过解析 | 合法的 `EarnTask`、`EarnDailyReward[7]`、overview 响应、`complete-task` 请求/响应 | `EarnOverviewResponseSchema`、`CompleteEarnTaskRequestSchema`、`CompleteEarnTaskResponseSchema` 解析成功并保留字段约束 | 单元测试 | P0 |
| T-02 | earn schema 非法输入被拒绝 | 非 uuid `task_id`、负数金币、非法 `status`、`daily_rewards` 长度不为 7、非法 action payload | Zod 抛出校验错误，脏数据不能进入后续层 | 单元测试 | P0 |
| T-03 | mock 仓储返回匿名/登录态 overview 视图 | 匿名 `userId=undefined`，登录 `userId=<uuid>` | 匿名返回 `coins=0,is_logged_in=false`；登录返回登录视角 coins；两者都包含固定 7 项 `daily_rewards` 与稳定任务结构 | 单元测试 | P0 |
| T-04 | mock 仓储完成任务具备幂等与异常语义 | 同一登录用户首次/重复提交代表性 `taskId`，以及不存在或不允许完成的任务 | 首次完成返回奖励与累计金币；重复完成不重复加币；非法任务返回 `NOT_FOUND` 或 `CONFLICT` | 单元测试 | P0 |
| T-05 | service 能校验 overview 仓储输出并映射异常 | stub repository 返回合法 overview、坏数据、显式 `AppError` | 合法结果按 schema 返回；坏数据转换为 `INTERNAL_ERROR`；显式 `AppError` 原样透传 | 单元测试 | P0 |
| T-06 | service 能校验 complete-task 仓储输出并保持幂等 contract | stub repository 返回合法完成结果、坏数据、显式 `AppError` | 合法结果按 schema 返回；坏数据转换为 `INTERNAL_ERROR`；幂等成功结构稳定 | 单元测试 | P0 |
| T-07 | overview 路由支持匿名访问并对失效 token 采用单一降级策略 | `GET /api/earn/overview`，无 token / 合法 token / 非法 Bearer token | 返回 `200`；匿名与失效 token 都能得到约定视角；路由输出遵循资源型 JSON contract | 单元测试 | P0 |
| T-08 | complete-task 路由保持 Bearer-only | `POST /api/earn/complete-task`，无 token / 非法 token / 合法 Bearer token | 未登录返回 `401 + AUTH_UNAUTHORIZED`；不引入 cookie-only 旁路；合法 Bearer 可进入 service | 单元测试 | P0 |
| T-09 | complete-task 路由校验请求体与业务结果 | 非 uuid `task_id`、不存在 task、代表性 task 成功完成、重复完成 | 参数非法返回 `400 + VALIDATION_ERROR`；不存在返回 `404`；成功与重复完成都返回约定 JSON 结构 | 单元测试 | P0 |

## 实现步骤

### Step 1：固化赚钱中心 contract 与 shared schema

- **关联测试**：T-01、T-02
- **目标文件**：`backend/src/lib/schemas.ts`、`backend/src/lib/__tests__/schemas.test.ts`、`backend/src/repositories/interfaces/earn.repository.interface.ts`
- **实现内容**：
  1. 先在 `backend/src/lib/__tests__/schemas.test.ts` 中补充 earn task、daily reward、overview、complete-task 请求/响应的合法与非法解析测试，覆盖固定 7 项奖励、任务 action 判别联合、非负金币、uuid 约束等核心 contract。
  2. 在 `backend/src/lib/schemas.ts` 中新增 `EarnTaskStatusSchema`、`EarnTaskActionSchema`、`EarnTaskSchema`、`EarnDailyRewardSchema`、`EarnOverviewResponseSchema`、`CompleteEarnTaskRequestSchema`、`CompleteEarnTaskResponseSchema` 及对应类型。
  3. 新增 `backend/src/repositories/interfaces/earn.repository.interface.ts`，定义 `getOverview({ userId? })` 与 `completeTask({ userId, taskId })` 的仓储契约，让 Repository / Service / Route 共享同一类型边界。
  4. 明确首版 contract 以 shared schema 为唯一权威来源，不引入 migration，也不在 route 层硬编码 seed 结构。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/lib/__tests__/schemas.test.ts` 确认 T-01、T-02 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增赚钱中心 request/response、任务与奖励 schema 及类型 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 增补 earn schema 合法/非法输入测试 |
| `backend/src/repositories/interfaces/earn.repository.interface.ts` | 新增 | 定义赚钱仓储接口与输入输出契约 |

### Step 2：落地 earn mock repository 与 registry 接线

- **关联测试**：T-03、T-04
- **目标文件**：`backend/src/repositories/mock/earn.mock.repository.ts`、`backend/src/repositories/__tests__/earn.mock.repository.test.ts`、`backend/src/repositories/repository-registry.ts`
- **实现内容**：
  1. 先在 `backend/src/repositories/__tests__/earn.mock.repository.test.ts` 中定义匿名/登录 overview、固定 7 项奖励、代表性任务完成、重复完成幂等、不存在任务、不可完成任务等测试场景。
  2. 新增 `backend/src/repositories/mock/earn.mock.repository.ts`，集中维护受控 seed 数据，并用 in-memory 任务完成状态实现首版闭环，保证同一用户重复完成同一任务时不重复加币。
  3. 修改 `backend/src/repositories/repository-registry.ts`，新增 `createDefaultEarnRepository()`、`getEarnRepository()`、`setEarnRepository()`，使 route/service 能按现有 registry 模式接入 earn repository。
  4. 保持首版默认只走 mock repository，不新增 Supabase repository，不假设 Redis、队列或账本表已存在。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/repositories/__tests__/earn.mock.repository.test.ts` 确认 T-03、T-04 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/repositories/mock/earn.mock.repository.ts` | 新增 | 提供赚钱首页 seed、任务完成幂等与累计金币 mock 实现 |
| `backend/src/repositories/__tests__/earn.mock.repository.test.ts` | 新增 | 覆盖匿名/登录视角、幂等完成与异常场景 |
| `backend/src/repositories/repository-registry.ts` | 修改 | 注册 earn repository 默认实现与测试替换入口 |

### Step 3：实现 EarnService 并统一异常映射

- **关联测试**：T-05、T-06
- **目标文件**：`backend/src/services/earn/earn.service.ts`、`backend/src/services/earn/earn.service.test.ts`
- **实现内容**：
  1. 先在 `backend/src/services/earn/earn.service.test.ts` 中通过 stub repository 定义 overview 合法结果、complete-task 合法结果、仓储返回坏数据、仓储抛出 `AppError`、重复完成幂等结果等测试。
  2. 新增 `backend/src/services/earn/earn.service.ts`，实现 `getOverview({ auth })` 与 `completeTask({ auth, taskId })`，分别对 repository 输出执行 `EarnOverviewResponseSchema.parse(...)` 与 `CompleteEarnTaskResponseSchema.parse(...)`。
  3. 延续现有 service 约定：已知 `AppError` 直接透传；schema 解析失败或未知异常统一转换为 `Errors.internal(...)`，保证 route 只消费受控业务结果或 `AppError`。
  4. 保持 service 只负责业务 contract、幂等结果稳定性与异常映射，不下沉 HTTP 细节，也不新增 cookie/session 分支。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/services/earn/earn.service.test.ts` 确认 T-05、T-06 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/services/earn/earn.service.ts` | 新增 | 封装 overview 聚合、complete-task 业务调用、schema 校验与异常映射 |
| `backend/src/services/earn/earn.service.test.ts` | 新增 | 覆盖合法结果、坏数据映射、`AppError` 透传与幂等 contract |

### Step 4：暴露 `GET /api/earn/overview` 路由并验证匿名闭环

- **关联测试**：T-07
- **目标文件**：`backend/src/app/api/earn/overview/route.ts`、`backend/src/app/api/__tests__/earn-overview.test.ts`
- **实现内容**：
  1. 先在 `backend/src/app/api/__tests__/earn-overview.test.ts` 中编写路由测试，覆盖匿名访问、合法 Bearer 登录访问、失效 Bearer token 的既定降级策略、service 未知异常返回 `INTERNAL_ERROR` 等场景。
  2. 新增 `backend/src/app/api/earn/overview/route.ts`，复用 `withErrorHandler`，通过 `resolveOptionalAuthContext(request)` 获取可选登录态，再调用 `EarnService(getEarnRepository()).getOverview(...)`。
  3. 保持 overview 为资源型成功响应 `NextResponse.json(result)`，不额外包裹 `{ code, data, message }`，错误统一交给 `withErrorHandler`。
  4. 明确 overview 是匿名可访问接口，不因缺少 token 阻断首页首屏加载。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/earn-overview.test.ts` 确认 T-07 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/app/api/earn/overview/route.ts` | 新增 | 新增赚钱首页 overview GET 路由 |
| `backend/src/app/api/__tests__/earn-overview.test.ts` | 新增 | 覆盖匿名/登录态、失效 token 降级与异常响应 |

### Step 5：暴露 `POST /api/earn/complete-task` 路由并保持 Bearer-only

- **关联测试**：T-08、T-09
- **目标文件**：`backend/src/app/api/earn/complete-task/route.ts`、`backend/src/app/api/__tests__/earn-complete-task.test.ts`
- **实现内容**：
  1. 先在 `backend/src/app/api/__tests__/earn-complete-task.test.ts` 中编写 route handler 测试，覆盖未登录返回 `401 + AUTH_UNAUTHORIZED`、非法 `task_id` 返回 `400 + VALIDATION_ERROR`、task 不存在返回 `404`、合法 Bearer 成功完成、重复完成幂等成功等场景。
  2. 新增 `backend/src/app/api/earn/complete-task/route.ts`，复用 `withErrorHandler`，解析 `request.json()` 后使用 `CompleteEarnTaskRequestSchema.parse(...)` 校验请求体。
  3. 在 route 中使用 `resolveRequiredAuthContext(request)` 强制 Bearer-only 登录校验，再调用 `EarnService(getEarnRepository()).completeTask(...)`；不新增 cookie-only、bridge 直传用户身份或其它旁路。
  4. 保持成功响应为资源体 JSON，失败路径统一走 `AppError` + `withErrorHandler` 输出标准 error envelope。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/earn-complete-task.test.ts` 确认 T-08、T-09 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/app/api/earn/complete-task/route.ts` | 新增 | 新增赚钱任务完成 POST 路由，并保持 Bearer-only 鉴权 |
| `backend/src/app/api/__tests__/earn-complete-task.test.ts` | 新增 | 覆盖鉴权、参数校验、成功、幂等与不存在任务测试 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4
                               └──▶ Step 5
```

- Step 1 先固化 shared schema 与 repository contract，后续 Repository / Service / Route 都依赖这组定义。
- Step 2 提供 mock repository 与 registry 接线，是 Step 3 service 与 Step 4-5 route 的基础数据源。
- Step 3 先稳定业务 contract 与异常映射，再分别暴露 overview 与 complete-task 两条接口。
- Step 4、Step 5 都依赖 Step 3 的 `EarnService`；其中 Step 5 还需要额外遵守 Bearer-only 鉴权约束。

## 验证总览

- [ ] Schema、Repository、Service、Route 定向测试通过（按步骤执行 `cd backend && npm run test -- <target>`）
- [ ] Backend 全量测试通过（`cd backend && npm run test`）
- [ ] 无新增 lint 错误（`cd backend && npm run lint`）
- [ ] Build 成功（`cd backend && npm run build`）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增赚钱中心 schema 与类型 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 增补 earn schema 测试 |
| `backend/src/repositories/interfaces/earn.repository.interface.ts` | 新增 | 赚钱仓储接口定义 |
| `backend/src/repositories/mock/earn.mock.repository.ts` | 新增 | 赚钱中心 mock 仓储实现 |
| `backend/src/repositories/__tests__/earn.mock.repository.test.ts` | 新增 | mock 仓储幂等与视角测试 |
| `backend/src/repositories/repository-registry.ts` | 修改 | earn repository registry 接线 |
| `backend/src/services/earn/earn.service.ts` | 新增 | 赚钱中心 service |
| `backend/src/services/earn/earn.service.test.ts` | 新增 | service 单元测试 |
| `backend/src/app/api/earn/overview/route.ts` | 新增 | overview 接口 |
| `backend/src/app/api/__tests__/earn-overview.test.ts` | 新增 | overview 路由测试 |
| `backend/src/app/api/earn/complete-task/route.ts` | 新增 | complete-task 接口 |
| `backend/src/app/api/__tests__/earn-complete-task.test.ts` | 新增 | complete-task 路由测试 |