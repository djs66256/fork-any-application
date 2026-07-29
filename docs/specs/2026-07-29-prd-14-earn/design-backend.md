# Backend 端技术方案：PRD-14 赚钱中心

> 创建日期：2026-07-29
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

```text
请求
→ app/api/earn/overview/route.ts (GET)
→ withErrorHandler
→ resolveOptionalAuthContext(request)
→ EarnService.getOverview({ auth })
→ EarnRepositoryInterface.getOverview({ userId? })
→ EarnMockRepository.getOverview(...)
→ 返回 EarnOverviewResponse

请求
→ app/api/earn/complete-task/route.ts (POST)
→ withErrorHandler
→ CompleteEarnTaskRequestSchema.parse(body)
→ resolveRequiredAuthContext(request)
→ EarnService.completeTask({ auth, taskId })
→ EarnRepositoryInterface.completeTask({ userId, taskId })
→ EarnMockRepository.completeTask(...)
→ 返回 CompleteEarnTaskResponse
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `backend/src/app/api/` | 扩展 | 新增 `earn/overview` 与 `earn/complete-task` 两组 Route |
| `backend/src/services/` | 扩展 | 新增 `services/earn/earn.service.ts` 承载赚钱中心聚合逻辑 |
| `backend/src/repositories/interfaces/` | 扩展 | 新增 `EarnRepositoryInterface`，隔离赚钱任务数据读写职责 |
| `backend/src/repositories/mock/` | 扩展 | 新增 `EarnMockRepository`，提供 overview seed 与任务完成幂等逻辑 |
| `backend/src/repositories/repository-registry.ts` | 扩展 | 新增 `get/set/createDefaultEarnRepository()` 接线 |
| `backend/src/lib/schemas.ts` | 扩展 | 新增 earn overview / task / reward / complete-task schemas |
| `backend/src/middleware/auth.ts` | 复用 | 继续使用 `resolveOptionalAuthContext` / `resolveRequiredAuthContext` |
| `backend/src/app/api/auth/sessions/route.ts` | 参考 | Native 登录完成后沿用现有 session contract，向 H5 同步 access token 快照仅作 earn API Bearer 使用 |
| `backend/src/middleware/error-handler.ts` | 不变 | 继续输出统一 `{ error: { code, message } }` 错误格式 |
| `backend/src/app/api/player/*` | 不变 | 不修改现有 player contract，只在 earn 域外围新增完成接口 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `backend/src/app/api/earn/overview/route.ts` | 新增 | 赚钱首页 overview GET 路由 |
| `backend/src/app/api/earn/complete-task/route.ts` | 新增 | 赚钱任务完成 POST 路由 |
| `backend/src/app/api/__tests__/earn-overview.test.ts` | 新增 | 覆盖匿名/登录态 overview、错误态与 schema 校验 |
| `backend/src/app/api/__tests__/earn-complete-task.test.ts` | 新增 | 覆盖登录校验、非法 taskId、幂等完成 |
| `backend/src/services/earn/earn.service.ts` | 新增 | 赚钱中心聚合 service |
| `backend/src/services/earn/earn.service.test.ts` | 新增 | 覆盖 overview parse、complete-task 幂等与异常映射 |
| `backend/src/repositories/interfaces/earn.repository.interface.ts` | 新增 | 赚钱仓储抽象 |
| `backend/src/repositories/mock/earn.mock.repository.ts` | 新增 | overview seed、任务状态与累计金币 mock 实现 |
| `backend/src/repositories/__tests__/earn.mock.repository.test.ts` | 新增 | 覆盖匿名数据、登录数据、重复完成逻辑 |
| `backend/src/repositories/repository-registry.ts` | 修改 | 注册 earn repository |
| `backend/src/lib/schemas.ts` | 修改 | 新增 earn response/request/data schemas |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 增补 earn schema 合法/非法输入测试 |
| `backend/src/lib/errors.ts` | 不变 | 继续复用 `AUTH_UNAUTHORIZED` / `NOT_FOUND` / `CONFLICT` 等错误码 |

---

## 3. API 路由设计

### 3.1 路由注册

| 路由文件 | HTTP 方法 | URL 路径 | 中间件链 | 说明 |
|---------|----------|---------|---------|------|
| `backend/src/app/api/earn/overview/route.ts` | `GET` | `/api/earn/overview` | `withErrorHandler` → optional auth resolve → `EarnService.getOverview` | 赚钱首页聚合数据源 |
| `backend/src/app/api/earn/complete-task/route.ts` | `POST` | `/api/earn/complete-task` | `withErrorHandler` → body parse → required auth resolve → `EarnService.completeTask` | 代表性任务完成写接口 |

### 3.2 路由分组策略

- 赚钱中心相关接口统一归属 `/api/earn/*`，与 `/api/mall/*`、`/api/player/*` 分离。
- 首版只提供最小读写闭环：
  - `overview`：读首页聚合数据；
  - `complete-task`：写代表性任务完成结果。
- Route 层继续保持“**只做入参解析、认证上下文获取、service 调用、返回 JSON**”的职责边界，不在 route 中拼装 seed 数据或写业务分支。

### 3.3 参数校验

```typescript
export const EarnTaskStatusSchema = z.enum([
  'available',
  'in_progress',
  'completed',
  'claimed',
  'locked',
]);

export const EarnTaskActionSchema = z.discriminatedUnion('type', [
  z.object({
    type: z.literal('play'),
    video_id: z.string().trim().min(1),
  }),
  z.object({
    type: z.literal('placeholder'),
    feedback: z.string().trim().min(1),
  }),
  z.object({
    type: z.literal('login'),
  }),
]);

export const EarnTaskSchema = z.object({
  id: z.string().uuid(),
  title: z.string().trim().min(1).max(100),
  description: z.string().trim().min(1).max(200),
  reward_coins: z.number().int().nonnegative(),
  status: EarnTaskStatusSchema,
  action: EarnTaskActionSchema,
  is_representative: z.boolean().optional(),
});

export const EarnDailyRewardSchema = z.object({
  day: z.number().int().min(1).max(7),
  coins: z.number().int().nonnegative(),
  status: z.enum(['claimable', 'claimed', 'locked']),
});

export const EarnOverviewResponseSchema = z.object({
  coins: z.number().int().nonnegative(),
  is_logged_in: z.boolean(),
  new_user_task: EarnTaskSchema,
  daily_rewards: z.array(EarnDailyRewardSchema).length(7),
  cash_tasks: z.array(EarnTaskSchema),
});

export const CompleteEarnTaskRequestSchema = z.object({
  task_id: z.string().uuid(),
});

export const CompleteEarnTaskResponseSchema = z.object({
  success: z.literal(true),
  task_id: z.string().uuid(),
  coins_earned: z.number().int().nonnegative(),
  total_coins: z.number().int().nonnegative(),
  task_status: z.literal('completed'),
});
```

- `GET /api/earn/overview` 无 query 参数，Route 只解析 optional auth。
- `POST /api/earn/complete-task` 从 JSON body 中读取 `task_id`，统一交给 `CompleteEarnTaskRequestSchema.parse(...)`。
- Repository 输出统一再经过 response schema parse，保证 mock seed 与未来真实仓储共享同一 contract。

---

## 4. Middleware 链设计

### 4.1 请求流水线

```text
GET /api/earn/overview
→ [logger]
→ [cors]
→ withErrorHandler(handler)
→ resolveOptionalAuthContext(request)
→ EarnService.getOverview(auth?)
→ NextResponse.json(result)

POST /api/earn/complete-task
→ [logger]
→ [cors]
→ withErrorHandler(handler)
→ await request.json()
→ CompleteEarnTaskRequestSchema.parse(body)
→ resolveRequiredAuthContext(request)
→ EarnService.completeTask(auth, taskId)
→ NextResponse.json(result)
```

### 4.2 Middleware 清单

| Middleware | 作用域 | 说明 |
|-----------|--------|------|
| `logger` | 全局 | 继续记录请求基础日志 |
| `cors` | 全局 | 继续沿用现有跨域策略 |
| `withErrorHandler` | 路由级 | 统一捕获 `AppError` / `ZodError` / 未知异常 |
| `resolveOptionalAuthContext` | `GET /api/earn/overview` | overview 支持匿名访问，同时识别登录态 |
| `resolveRequiredAuthContext` | `POST /api/earn/complete-task` | complete-task 必须登录 |
| `rate limit` | 预留 | 首版不单独实现；未来若奖励接口需要限流，可接在 `/api/earn/*` |

### 4.3 认证闭环约定

- backend 首版继续保持 `POST /api/earn/complete-task` 为 **Bearer-only**，不新增 cookie-only 或 bridge 自报用户身份的旁路。
- Native 登录链路继续沿用现有 `POST /api/auth/sessions` / `POST /api/auth/session-refreshes` / `DELETE /api/auth/session` contract 管理 session。
- earn H5 仅消费 Native 通过 `earn.syncAuthState` 下发的 `apiAccessToken` 快照，并在调用 earn API 时附加 `Authorization: Bearer <token>`。
- backend 不感知 token 来自 Native host sync 还是浏览器 Supabase session，只认 `Authorization` 头与现有 auth middleware 的校验结果。
- token 失效时，`complete-task` 统一返回 `401 + AUTH_UNAUTHORIZED`，由 H5 清空 token 快照并重新走 earn 登录引导；backend 不为 earn 单独发 refresh token。

### 4.4 错误传播方式

### 4.3 错误传播方式

- request body / schema 校验失败：抛 `ZodError`，由 `withErrorHandler` 返回 `400 + VALIDATION_ERROR + details`。
- complete-task 未登录：`resolveRequiredAuthContext` 抛 `Errors.authUnauthorized('请先登录')`。
- service / repository 发现任务不存在、任务状态非法：抛 `AppError`（如 `Errors.notFound(...)`、`Errors.conflict(...)`）。
- repository 返回坏数据：service 用 `Errors.internal('Invalid earn overview result')` / `Errors.internal('Invalid earn complete-task result')` 兜底。

---

## 5. Service 层设计

### 5.1 Service 清单

| Service | 职责 | 输入 | 输出 | 依赖 |
|---------|------|------|------|------|
| `EarnService` | 聚合 overview 读取、任务完成、schema 校验、异常映射 | `auth?` / `taskId` | `EarnOverviewResponse` / `CompleteEarnTaskResponse` | `EarnRepositoryInterface` |

### 5.2 事务边界

| 操作组合 | 事务隔离级别 | 回滚策略 |
|---------|------------|---------|
| `GET /api/earn/overview` | 无事务 | 只读查询，无事务需求 |
| `POST /api/earn/complete-task` | mock 实现无真实事务 | 首版 in-memory 幂等更新；未来真实仓储需以用户 + task 唯一约束保证原子性 |

### 5.3 业务异常定义

| 异常类型 | 触发条件 | HTTP 状态码 | 错误码 |
|---------|---------|-----------|--------|
| `ZodError` | request body 非法 | 400 | `VALIDATION_ERROR` |
| `Errors.authUnauthorized('请先登录')` | complete-task 未登录 | 401 | `AUTH_UNAUTHORIZED` |
| `Errors.notFound('Earn task not found')` | taskId 不存在 | 404 | `NOT_FOUND` |
| `Errors.conflict('Earn task cannot be completed')` | 任务状态不允许完成（预留） | 409 | `CONFLICT` |
| `Errors.internal('Invalid earn overview result')` | overview 响应不符合 schema | 500 | `INTERNAL_ERROR` |
| `Errors.internal('Invalid earn complete-task result')` | complete-task 响应不符合 schema | 500 | `INTERNAL_ERROR` |

### 5.4 Service 设计要点

- `EarnService.getOverview(auth)`：
  - 将 `auth?.userId` 透传给 repository；
  - 对 repository 返回值执行 `EarnOverviewResponseSchema.parse(...)`；
  - 匿名失败与登录失败不混淆，overview 保持可匿名访问。
- `EarnService.completeTask(auth, taskId)`：
  - 只接受已经过 `resolveRequiredAuthContext()` 的用户上下文；
  - 对 repository 返回值执行 `CompleteEarnTaskResponseSchema.parse(...)`；
  - 首版允许同一 task 重复完成返回幂等成功结果，而不是重复加币。
- service 不负责发明“播放完成判定”，只接收 Native 已确认的 `taskId` 完成意图。

---

## 6. 数据库 Migration 计划

### 6.1 变更概述

| 表名 | 操作（新建/修改/删除） | 说明 |
|------|----------------------|------|
| 无 | 无 | 首版不新增真实数据库表，使用 mock seed + 内存状态 |

### 6.2 DDL

```sql
-- No-op for PRD-14 earn phase 1.
-- 首版赚钱中心不新增 Supabase migration。
```

### 6.3 字段详情

| 表名 | 字段 | 类型 | 约束 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| 无 | — | — | — | — | 暂无真实表结构变更 |

### 6.4 索引策略

| 表名 | 索引名 | 类型（UNIQUE/INDEX） | 字段 | 用途 |
|------|--------|---------------------|------|------|
| 无 | — | — | — | 暂无 |

### 6.5 回滚策略

- 首版无 migration，因此无需数据库级回滚。
- 若后续引入真实金币账本或任务进度表，应新增独立 migration，而不是修改本期 mock contract。
- 未来真实表建议至少包含：
  - `earn_tasks`
  - `earn_task_completions`
  - `earn_coin_balances` 或等价聚合表
  并以 `(user_id, task_id)` 建唯一约束保证幂等。

---

## 7. 后台任务/队列设计

### 7.1 任务清单

| 任务名称 | 触发条件 | 执行频率 | 队列/调度方式 | 重试策略 | 超时 |
|---------|---------|---------|-------------|---------|------|
| 无 | — | — | — | — | — |

### 7.2 任务生命周期

```text
本期无异步任务 / 队列。
```

### 7.3 失败处理与死信队列

- 首版赚钱中心不涉及提现审核、账本对账、奖励补发等异步任务。
- 如果未来引入延迟发奖或风控校验，再补充 Redis / MQ 方案，不在本期超前设计。

---

## 8. 配置与环境

| 配置项 | 环境变量 Key | 开发环境值 | 生产环境值 | 说明 |
|--------|-------------|----------|-----------|------|
| 应用名 | `APP_NAME` | 现有配置 | 现有配置 | 复用现有配置 |
| 应用版本 | `APP_VERSION` | 现有配置 | 现有配置 | 复用现有配置 |
| 数据源仓储选择（可选） | `EARN_REPOSITORY` | `mock`（默认） | 预留 `supabase` | 如未来需要切换真实数据源，可新增该配置 |
| Supabase URL | `SUPABASE_URL` | 现有配置 | 现有配置 | 仅未来接真实仓储时使用 |
| Redis URL | `REDIS_URL` | 现有配置 | 现有配置 | 本期不直接使用 |

> ⚠️ 禁止硬编码任何环境地址、token、用户标识或真实资金常量。即使首版走 mock repository，也应通过 config / registry 控制仓储接线。

---

## 9. API 调用清单（调用外部服务）

| 外部服务 | API 端点 | 调用时机 | 超时 | 降级策略 |
|---------|---------|---------|------|---------|
| Supabase Auth | `auth.getUser(token)` | `verifyJwt()` 校验 Bearer token 时 | 复用现有默认超时 | 校验失败返回未登录，不中断 overview 匿名访问 |
| 无其他外部服务 | — | — | — | 首版业务数据来自 mock repository |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Backend 实现方式 |
|---------|---------------|-----------------|
| overview 匿名可访问 | 未登录也能进入赚钱首页 | route 使用 `resolveOptionalAuthContext` |
| complete-task 必须登录 | 只有登录用户可确认奖励 | route 使用 `resolveRequiredAuthContext` |
| 资源型成功响应 | success 不统一包 `{ code, data, message }` | earn route 直接 `NextResponse.json(result)` |
| 失败统一 error envelope | `{ error: { code, message } }` | 全部通过 `withErrorHandler` 输出 |
| 代表性任务 contract | 任务必须携带 `taskId` / `videoId` / reward / status | schemas 与 mock repository 固定约束 |
| 连续看剧福利固定 7 项 | 7 宫格 UI 稳定渲染 | `EarnOverviewResponseSchema.daily_rewards.length(7)` |
| 幂等完成语义 | 重复完成不重复加币 | repository 以内存进度表维护完成状态 |
| 匿名金币数为 0 | 匿名态收益头图展示默认值 | repository 对无 userId 返回 `coins = 0` |
| 浏览器模式仅调试 H5 | 后端不依赖 Native bridge 才能返回 overview | earn API 与容器无耦合 |

---

## 11. 边界与错误处理

### 11.1 全局错误处理架构

| 层级 | 机制 | 说明 |
|------|------|------|
| Route | `withErrorHandler` | 捕获 `AppError` / `ZodError` / 未知异常 |
| Service | schema parse + `Errors.*` | 统一把仓储异常与坏数据映射为受控错误 |
| Repository | mock seed + in-memory progress | 尽量返回合法结构；非法任务显式抛错 |
| 日志 | `console.error` + 现有 logger | 保留 request 维度日志 |

### 11.2 错误码定义

| 业务错误码 | HTTP 状态码 | 说明 | 响应示例 |
|-----------|------------|------|---------|
| `VALIDATION_ERROR` | 400 | request body schema 校验失败 | `{ "error": { "code": "VALIDATION_ERROR", "message": "Validation failed" } }` |
| `INVALID_PARAMS` | 400 | 预留的业务参数错误 | `{ "error": { "code": "INVALID_PARAMS", "message": "..." } }` |
| `AUTH_UNAUTHORIZED` | 401 | 未登录或 token 无效 | `{ "error": { "code": "AUTH_UNAUTHORIZED", "message": "请先登录" } }` |
| `NOT_FOUND` | 404 | taskId 不存在 | `{ "error": { "code": "NOT_FOUND", "message": "Earn task not found" } }` |
| `CONFLICT` | 409 | 任务状态不允许完成（预留） | `{ "error": { "code": "CONFLICT", "message": "..." } }` |
| `INTERNAL_ERROR` | 500 | 服务内部错误 / schema parse 失败 | `{ "error": { "code": "INTERNAL_ERROR", "message": "Internal server error" } }` |
| `SERVICE_UNAVAILABLE` | 503 | 未来真实数据源不可用 | `{ "error": { "code": "SERVICE_UNAVAILABLE", "message": "Service unavailable: earn-overview" } }` |

### 11.3 边界场景

| 场景 | 触发条件 | API 行为 | 说明 |
|------|---------|---------|------|
| overview 匿名访问 | 不带 token | 返回 `200 + 匿名视角 overview` | 默认金币为 0 |
| overview 携带无效 token | token 失效或解析失败 | 首版建议降级为匿名返回 | 避免首屏因脏 token 被阻断 |
| complete-task 未登录 | 不带或携带非法 token | 返回 `401 + AUTH_UNAUTHORIZED` | H5 决定提示重新登录 |
| taskId 非法 | 非 uuid 或缺失 | 返回 `400 + VALIDATION_ERROR` | Route 不进入 service |
| taskId 不存在 | uuid 合法但不在 seed 中 | 返回 `404 + NOT_FOUND` | 不伪造奖励成功 |
| 重复完成同一任务 | 同一用户再次上报已完成 task | 返回 `200` 幂等成功结果 | 首版优先降低前端复杂度 |
| 非代表性任务调用 complete-task | task 虽存在但不允许完成 | 返回 `409 CONFLICT` 或 `404 NOT_FOUND` | 实现时固定单一策略 |
| seed 数据非法 | daily_rewards 非 7 项 / 字段缺失 | service 抛 `INTERNAL_ERROR` | 不让脏数据外泄 |

### 11.4 错误日志与监控

- API 测试覆盖匿名/登录视角、非法 body、未登录、重复完成、任务不存在。
- service 测试覆盖 repository 返回坏数据时的 `INTERNAL_ERROR` 映射。
- 若未来切 Supabase，实现层需补充 `userId / taskId / requestId` 维度日志。

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| 单元测试 | `EarnService` schema 校验、异常映射、幂等完成 | Vitest |
| 集成测试 | `GET /api/earn/overview` 与 `POST /api/earn/complete-task` 路由行为 | Vitest + Next Route Handler 测试模式 |
| Repository 测试 | `EarnMockRepository` 匿名/登录视图、任务完成、重复完成 | Vitest |
| Schema 测试 | earn task / reward / overview / complete-task 合法与非法输入 | Vitest |

### 12.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 测试类型 |
|------|---------|------|---------|---------|
| BE-EARN-01 | 匿名 overview | `GET /api/earn/overview` 无 token | `200`，`is_logged_in=false`，`coins=0` | 集成 |
| BE-EARN-02 | 登录 overview | `GET /api/earn/overview` 携带有效 token | `200`，`is_logged_in=true`，返回登录视角 coins | 集成 |
| BE-EARN-03 | overview seed 非法 | repository 返回坏数据 | service 抛 `INTERNAL_ERROR` | 单元 |
| BE-EARN-04 | complete-task 未登录 | `POST /api/earn/complete-task` 无 token | `401 + AUTH_UNAUTHORIZED` | 集成 |
| BE-EARN-05 | complete-task 参数非法 | `task_id` 非 uuid | `400 + VALIDATION_ERROR` | 集成 |
| BE-EARN-06 | complete-task 成功 | 合法 token + 代表性 taskId | `200`，`coins_earned` 与 `total_coins` 正确 | 集成 |
| BE-EARN-07 | 重复完成幂等 | 连续两次提交同一 `task_id` | 第二次不重复累计 coins | Repository / 单元 |
| BE-EARN-08 | task 不存在 | 合法 uuid 但不存在 | `404 + NOT_FOUND` | 集成 |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| 赚钱仓储 | `EarnMockRepository` / stub repository | service 测试通过依赖注入替换 |
| AuthContext | route 测试构造带/不带 token 的请求 | 不依赖真实登录流程 |
| Next Request | Route 测试构造请求对象 | 不依赖真实 HTTP Server |
| Supabase / Redis | 不接入真实实例 | 仅 auth middleware 的 token 解析路径走现有 fake/local session 能力 |

---

## 13. 安全考虑

- **认证与授权**：
  - overview 允许匿名读取；
  - complete-task 必须依赖 Bearer token；
  - 不接受 H5 自报用户身份。
- **输入校验**：所有 request / response 使用 Zod 约束。
- **幂等防滥用**：同一用户对同一 taskId 重复完成时不得重复加币。
- **敏感数据处理**：不返回手机号、昵称、token、提现账户等敏感信息。
- **信任边界**：后端只信任通过 auth middleware 校验的用户上下文与受控 `task_id`，不信任 H5 的“已完成”文案。

---

## 14. 性能考虑

- **预期 QPS**：低；首版偏演示 / 内测量级。
- **缓存策略**：首版不引入 Redis 缓存，mock 数据读取成本可忽略。
- **数据库优化**：当前无真实数据库；未来若接真实账本，应以 `(user_id, task_id)` 唯一索引保障幂等。
- **连接池配置**：沿用现有 Supabase runtime 配置，本期无新增要求。

---

## 15. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | — | — | 复用现有 Next.js + Zod + Vitest + auth middleware |

> ⚠️ 新增开源依赖前必须征得用户同意（遵守根目录 CLAUDE.md 开发约束）。

---

## 16. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 把 player 完成语义直接塞进 `/api/player/*` 导致现有 contract 漂移 | Backend / 全端 | 🔴 | 中 | 新增独立 `/api/earn/complete-task` | 维持 player contract 不变 |
| overview 首屏被 token 校验失败阻断 | Backend / Web / Native | 🔴 | 中 | overview 使用 optional auth，失败优先降级匿名视角 | 记录日志后返回匿名数据 |
| 重复 complete-task 重复加币 | Backend / 产品可信度 | 🔴 | 中 | repository 以用户 + task 维度做幂等 | 返回已完成结果，不重复累计 |
| seed 数据结构随手修改导致前端联调反复漂移 | Backend / Web / Native | 🟡 | 中 | 先固化 Zod schema，再写 repository seed | schema 测试拦截坏数据 |

---

## 17. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/features/app-shell/index.md` | 已知限制 | earn 仍为占位页，需新增真实 H5 数据源 |
| `wiki/architecture/overview.md` | 承载策略 | earn 继续采用 H5 + Native 容器模式 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `backend/CLAUDE.md` | Backend 四层架构、auth、测试与环境约束 |
| `backend/src/middleware/auth.ts` | `resolveOptionalAuthContext` / `resolveRequiredAuthContext` 现成可复用 |
| `backend/src/middleware/error-handler.ts` | 统一错误输出格式 |
| `backend/src/lib/errors.ts` | `AUTH_UNAUTHORIZED` / `NOT_FOUND` / `CONFLICT` / `INTERNAL_ERROR` 枚举 |
| `backend/src/repositories/repository-registry.ts` | 现有 registry 接线模式，需要新增 earn repository |
| `backend/src/services/mall/mall.service.ts` | service parse + error mapping 最近实现范式 |
| `backend/src/repositories/interfaces/mall.repository.interface.ts` | repository 抽象写法参考 |
| `backend/src/app/api/mall/products/route.ts` | 资源型读接口 route 写法参考 |
| `backend/src/app/api/auth/sessions/route.ts` | auth 成功响应存在特殊 envelope 的实现现实 |
| `backend/src/app/api/player/progress/route.ts` | player contract 不带 earn task context |
| `backend/src/app/api/player/stop/route.ts` | player stop contract 不适合作为任务完成接口 |
| `docs/specs/2026-07-29-prd-14-earn/design.md` | earn shared contract |
