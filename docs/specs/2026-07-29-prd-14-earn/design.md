# 技术方案（共享部分）：PRD-14 赚钱中心

> 创建日期：2026-07-29
> 对应需求：spec.md

## 整体架构

```mermaid
flowchart LR
    EarnTab[iOS / Android Earn Tab] --> EarnContainer[赚钱 Native 容器\nWKWebView / WebView]
    EarnContainer --> EarnH5[Web Earn H5\n/earn]

    EarnH5 --> OverviewAPI[GET /api/earn/overview]
    EarnH5 --> CompleteTaskAPI[POST /api/earn/complete-task]

    OverviewAPI --> EarnRoute[Route Handler]
    CompleteTaskAPI --> EarnRoute
    EarnRoute --> EarnService[EarnService]
    EarnService --> EarnRepo[EarnRepositoryInterface]
    EarnRepo --> EarnMockRepo[EarnMockRepository]
    EarnMockRepo --> EarnSeed[overview / 任务 / 奖励 seed 数据]

    EarnH5 -. earn.requestLogin .-> NativeLogin[Native 登录承接适配层]
    NativeLogin -. earn.syncAuthState / earn.restoreContext .-> EarnContainer

    EarnH5 -. earn.openTaskPlayer .-> NativePlayer[Native 播放承接层]
    NativePlayer -. earn.completeTask callback .-> EarnContainer
    NativePlayer -. returnTarget=/earn .-> EarnContainer

    Browser[浏览器 / 本地开发] --> EarnH5
    Browser -. no native bridge .-> DegradedMode[按钮降级反馈 / 本地调试模式]
```

### 架构说明

- 本期严格遵循 `PRODUCT.md` 的承载策略：**赚钱首页由 Web H5 提供，iOS / Android 只负责 Native 容器、bridge、登录/播放承接与返回恢复**。
- Backend 延续现有四层架构：**Route → Service → Repository → Mock / Infrastructure**。首版只新增两个 earn 资源接口：
  - `GET /api/earn/overview`
  - `POST /api/earn/complete-task`
- Web 端沿用 PRD-13 mall 已验证的分层模式：**App Router Page shell → Feature 页面状态机 → Core API / schema / config → bridge / host sync**；但 earn 相关配置、schema、消息协议和状态机都需要独立定义，不能直接复用 mall 命名空间。
- Native 容器可参考 mall 的组织方式，但 **earn 不是 mall 的参数化实例**。earn 需要独立的：
  - route / config
  - bridge message schema
  - host sync message schema
  - login adapter
  - player task callback 语义
- 浏览器模式仅用于 H5 UI、接口和异常态调试，不要求真实打开 Native 登录或播放器；因此 Web 必须为 bridge 缺失场景提供可理解的降级反馈，而不是假死。
- 首版代表性任务完成以 **earn 独立完成回调** 为准，**不修改也不依赖当前 player `progress / stop` contract 来隐式推断任务完成**；player 现有接口继续服务原播放主链路，earn 在其外围新增最小回传闭环。

## API 设计

### 涉及变更

| 类型 | 数量 | 说明 |
|------|------|------|
| 新增接口 | 2 | 新增赚钱首页 overview 与任务完成接口 |
| 修改接口 | 0 | 不修改现有 `/api/player/*`、`/api/auth/*` 契约 |
| 废弃接口 | 0 | 无 |

> 兼容性说明：当前仓库的稳定约定并不是模板中的 `{ code, data, message }` 通用包裹。PRD-14 继续沿用代码现实：
> - **大多数资源型成功响应按资源体直出**（如 `NextResponse.json(result)`）
> - **认证相关成功响应** 已存在 `{ code, data, message }` 包裹（如 `/api/auth/sessions`）
> - **失败响应** 统一由 `withErrorHandler` 输出 `{ error: { code, message } }`，参数校验错误可附带 `details`
>
> earn 新接口采用与 mall 相同的资源型成功响应：overview 与 complete-task 都直接返回资源体 JSON；错误响应继续统一走 `withErrorHandler`。

### 新增接口

#### `GET /api/earn/overview`

- **功能简介**：返回赚钱首页首屏所需的聚合数据，包括收益头图、新手任务、连续看剧福利和现金任务列表。
- **Path Parameters**：无
- **认证要求**：匿名可访问；若请求携带有效 `Authorization: Bearer <token>`，服务端可返回已登录视角的 seed 数据，否则返回匿名视角默认数据。

- **Query Parameters**：无

- **Request Body**：无

- **Response**：

```json
{
  "coins": 0,
  "is_logged_in": false,
  "new_user_task": {
    "id": "11111111-1111-4111-8111-111111111111",
    "title": "新人7天保底6元",
    "description": "完成首次看剧任务即可领取金币奖励",
    "reward_coins": 600,
    "status": "available",
    "action": {
      "type": "play",
      "video_id": "drama-001-episode-01"
    }
  },
  "daily_rewards": [
    {
      "day": 1,
      "coins": 10,
      "status": "claimable"
    }
  ],
  "cash_tasks": [
    {
      "id": "22222222-2222-4222-8222-222222222222",
      "title": "看剧领现金",
      "description": "完整观看指定短剧可获得金币",
      "reward_coins": 500,
      "status": "available",
      "action": {
        "type": "play",
        "video_id": "drama-001-episode-01"
      },
      "is_representative": true
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `coins` | `number` | 当前累计金币，匿名态固定允许为 `0` |
| `is_logged_in` | `boolean` | 当前视角的登录态快照，供 H5 决定按钮分流 |
| `new_user_task` | `EarnTask` | 首屏新手任务卡 |
| `daily_rewards` | `EarnDailyReward[]` | 连续看剧福利 7 宫格数据，首版固定 7 项 |
| `cash_tasks` | `EarnTask[]` | 现金任务区任务列表，至少包含一个代表性可打通任务 |

- **Error Codes**：

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功；允许任务列表为空 |
| 401 | `AUTH_UNAUTHORIZED` | 仅在传入了非法 token 且服务端选择严格校验时使用；匿名访问本身不报错 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |
| 503 | `SERVICE_UNAVAILABLE` | 未来真实数据源不可用（预留） |

#### `POST /api/earn/complete-task`

- **功能简介**：在 Native 播放承接层确认代表性任务完成后，更新任务状态并返回本次奖励结果。
- **Path Parameters**：无
- **认证要求**：需要登录；通过现有 `Authorization: Bearer <token>` 头解析登录态，不接受 H5 自报登录成功。

- **Query Parameters**：无

- **Request Body**：

```json
{
  "task_id": "22222222-2222-4222-8222-222222222222"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `task_id` | `string(uuid)` | 是 | 代表性任务 ID |

- **Response**：

```json
{
  "success": true,
  "task_id": "22222222-2222-4222-8222-222222222222",
  "coins_earned": 500,
  "total_coins": 500,
  "task_status": "completed"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `success` | `boolean` | 首版固定返回 `true` 表示本次回调被接受 |
| `task_id` | `string(uuid)` | 被完成的任务 ID |
| `coins_earned` | `number` | 本次获得金币数 |
| `total_coins` | `number` | 完成后的累计金币 |
| `task_status` | `string` | 任务状态，首版返回 `completed` |

- **Error Codes**：

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功；若任务已完成，可返回同样结构保持幂等 |
| 400 | `INVALID_PARAMS` / `VALIDATION_ERROR` | `task_id` 非法 |
| 401 | `AUTH_UNAUTHORIZED` | 未登录或 token 无效 |
| 404 | `NOT_FOUND` | 任务不存在 |
| 409 | `CONFLICT` | 任务状态不允许完成（预留） |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

### Zod Schema 定义

```typescript
import { z } from 'zod';

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

export const EarnDailyRewardStatusSchema = z.enum(['claimable', 'claimed', 'locked']);

export const EarnDailyRewardSchema = z.object({
  day: z.number().int().min(1).max(7),
  coins: z.number().int().nonnegative(),
  status: EarnDailyRewardStatusSchema,
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

### Bridge / Host Sync Schema 定义

```typescript
export const EarnTaskContextSchema = z.object({
  taskId: z.string().uuid(),
  source: z.literal('earn'),
  returnTarget: z.literal('/earn'),
  videoId: z.string().trim().min(1),
});

export const EarnLoginContextSchema = z.object({
  source: z.literal('earn'),
  returnTarget: z.literal('/earn'),
});

export const EarnBridgeMessageSchema = z.discriminatedUnion('type', [
  z.object({
    type: z.literal('earn.requestLogin'),
    payload: EarnLoginContextSchema,
  }),
  z.object({
    type: z.literal('earn.openTaskPlayer'),
    payload: EarnTaskContextSchema,
  }),
]);

export const EarnHostTransportSchema = z.object({
  type: z.literal('custom-event'),
  eventName: z.literal('earn.hostMessage'),
});

export const EarnHostAuthStateSchema = z.object({
  source: z.literal('earn'),
  isLoggedIn: z.boolean(),
  reason: z.enum(['initial-load', 'login-success', 'login-cancel', 'app-resume']),
  returnTarget: z.literal('/earn'),
  apiAccessToken: z.string().trim().min(1).nullable().optional(),
  expiresAt: z.string().trim().min(1).nullable().optional(),
});

export const EarnRestoreContextSchema = z.object({
  source: z.literal('earn'),
  reason: z.enum(['login-return', 'task-return', 'container-recreated']),
  returnTarget: z.literal('/earn'),
  preserveScroll: z.boolean().default(false),
});

export const EarnTaskPlayerResultSchema = z.object({
  source: z.literal('earn'),
  taskId: z.string().uuid(),
  videoId: z.string().trim().min(1),
  completed: z.boolean(),
  reason: z.enum(['playback-ended', 'user-exit', 'backgrounded', 'error', 'container-recreated']),
});

export const EarnTaskCompletionSchema = EarnTaskPlayerResultSchema;

export const EarnHostMessageSchema = z.discriminatedUnion('type', [
  z.object({
    type: z.literal('earn.syncAuthState'),
    payload: EarnHostAuthStateSchema,
  }),
  z.object({
    type: z.literal('earn.restoreContext'),
    payload: EarnRestoreContextSchema,
  }),
  z.object({
    type: z.literal('earn.completeTask'),
    payload: EarnTaskCompletionSchema,
  }),
]);
```

### 契约补充

- `GET /api/earn/overview` 不要求登录，但若 Native 宿主已知登录态，应通过 host sync 把权威登录态同步给 H5；H5 不自行猜测“是否已登录”。
- `POST /api/earn/complete-task` 的调用时机只允许是 **Native 播放承接层显式确认代表性任务完成之后**；H5 不能仅凭“按钮点击成功”或“进入播放页”直接调用该接口。
- earn 首版认证方案固定为：
  - Backend `complete-task` 继续保持 `Authorization: Bearer <token>`；
  - Native 宿主在首次加载、登录成功、登录取消、App 恢复时，通过 `earn.syncAuthState` 同步 `isLoggedIn` 与当前 `apiAccessToken` 快照；
  - H5 只在内存态暂存 `apiAccessToken`，仅用于调用 earn backend，不写入 localStorage、sessionStorage、URL、日志或 bridge 请求体；
  - 若 `apiAccessToken` 缺失、过期或 `complete-task` 返回 `AUTH_UNAUTHORIZED`，H5 必须清空本地 token 快照、展示登录引导，并重新走 `earn.requestLogin`；
  - Native 不通过 bridge 暴露 refresh token；token 刷新责任仍由 Native 现有 AuthStore / AuthStateHolder 管理。
- 代表性任务必须包含：
  - `taskId`
  - `source = earn`
  - `returnTarget = /earn`
  - `videoId`
- Native → H5 host sync 的唯一标准 transport 固定为 `CustomEvent('earn.hostMessage', { detail })`：
  - `detail` 必须是完整 `EarnHostMessage` JSON 对象；
  - Web 只监听 `window` 上的 `earn.hostMessage`，不再以 `window.message` 作为 earn 主协议；
  - iOS 通过 `WKWebView.evaluateJavaScript(...)` 注入该 `CustomEvent`；
  - Android 通过 `WebView.evaluateJavascript(...)` 注入该 `CustomEvent`；
  - 首版 earn 不保留 `window.postMessage` fallback，避免重复落入 mall 的历史分叉。
- 浏览器模式下如果没有 Native bridge：
  - `earn.requestLogin` 不触发真实登录，只返回受控反馈；
  - `earn.openTaskPlayer` 不触发真实播放，只返回“需在 App 内完成”的反馈；
  - 页面仍可完成 overview 的加载、错误态和按钮可见性调试。
- `earn.completeTask` host message 是 **Native → H5 的回传语义**，表示播放承接层已经得到任务完成结果。H5 接收到 `completed=true` 后再调用 `POST /api/earn/complete-task`，并在成功后刷新局部收益或重新请求 overview。
- `earn.restoreContext` 是“返回赚钱中心”的最低恢复契约：登录返回、任务返回和容器重建都必须最终回到 `/earn`，并保持 earn tab 高亮。
- player → earn result contract 固定为 `EarnTaskPlayerResult`：
  - 结果对象最少包含 `taskId / videoId / completed / reason / source`；
  - `completed=true` 仅允许由 Native 播放承接层在“代表性任务视频正常播放结束”时产出；
  - `user-exit`、`backgrounded`、`error`、`container-recreated` 一律返回 `completed=false`；
  - earn 容器收到结果后，顺序必须是：`earn.completeTask`（仅 completed=true 时发送）→ `earn.restoreContext(reason='task-return')`。

## 数据模型

### 新增/变更数据表

| 表名 | 操作 | 说明 |
|------|------|------|
| `earn overview seed`（逻辑模型） | 新建 | 首页收益与任务聚合 seed，首版允许存在于 mock repository |
| `earn task progress seed`（逻辑模型） | 新建 | 任务完成幂等与累计金币的 in-memory/mock 状态 |
| 真实数据库表 | 不变 | 首版不新增 Supabase migration |

> 本期先固化 contract 与 mock 数据，不强行引入真实账本或奖励流水表。后续若接入真实仓储，应新增独立 repository 实现并保持相同接口契约。

### 共享实体设计

| 实体 | 字段 | 说明 |
|------|------|------|
| `EarnOverview` | `coins / is_logged_in / new_user_task / daily_rewards / cash_tasks` | 赚钱首页聚合响应 |
| `EarnTask` | `id / title / description / reward_coins / status / action / is_representative` | 任务实体 |
| `EarnDailyReward` | `day / coins / status` | 连续看剧福利日卡实体 |
| `CompleteEarnTaskRequest` | `task_id` | 完成任务请求 |
| `CompleteEarnTaskResult` | `success / task_id / coins_earned / total_coins / task_status` | 完成任务响应 |
| `EarnTaskContext` | `taskId / source / returnTarget / videoId` | H5 → Native 播放承接上下文 |
| `EarnLoginContext` | `source / returnTarget` | H5 → Native 登录承接上下文 |
| `EarnBridgeMessage` | `earn.requestLogin / earn.openTaskPlayer` | H5 → Native bridge 消息 |
| `EarnHostMessage` | `earn.syncAuthState / earn.restoreContext / earn.completeTask` | Native → H5 宿主消息 |
| `EarnPageState` | `overview / isLoading / error / loginPrompt / pendingTask / feedback / pendingCompletion` | H5 首页状态机 |
| `EarnContainerState` | `loading / success / error / lastLoadedHomeUrl` | Native 容器宿主态 |

### 字段语义

| 字段 | 类型 | 语义 | 备注 |
|------|------|------|------|
| `EarnOverview.coins` | `number` | 当前累计金币 | 匿名态允许固定为 0 |
| `EarnOverview.is_logged_in` | `boolean` | 当前视角登录态 | 由 Backend 或 Native sync 给出 |
| `EarnTask.reward_coins` | `number` | 任务奖励金币 | 仅原始数值，不返回展示文案 |
| `EarnTask.status` | `available / in_progress / completed / claimed / locked` | 任务当前状态 | 首版主要使用 `available / completed / locked` |
| `EarnTask.action.type` | `play / placeholder / login` | 点击行为类型 | Web 根据该字段决定分流 |
| `EarnDailyReward.status` | `claimable / claimed / locked` | 7 宫格状态 | 首版仅展示 UI，不触发真实领奖 |
| `EarnTaskContext.source` | `'earn'` | 来源频道标识 | 跨端不可变常量 |
| `EarnTaskContext.returnTarget` | `'/earn'` | 返回目标 | 播放/登录返回时必须遵守 |
| `EarnTaskContext.videoId` | `string` | Native 播放承接需要的播放目标 | 当前 player route 仍只接受 `videoId` |
| `EarnHostAuthState.reason` | `initial-load / login-success / login-cancel / app-resume` | 登录态同步原因 | 与 mall 模式相同但命名空间独立 |
| `EarnRestoreContext.reason` | `login-return / task-return / container-recreated` | 上下文恢复原因 | 首版允许只恢复到首页首屏 |
| `CompleteEarnTaskResult.total_coins` | `number` | 完成后的累计金币 | H5 可直接局部更新或触发 overview 重拉 |

### 数据来源策略

| 场景 | 数据来源 | 说明 |
|------|---------|------|
| 赚钱首页收益与任务 | Backend `GET /api/earn/overview` | 首版使用 mock repository 提供固定结构 |
| 连续看剧福利 7 宫格 | Backend seed | 保证固定 7 项，便于 UI 稳定渲染 |
| 匿名/登录态判断 | Native AuthContext / optional auth header / host sync | 权威登录态保留在 Native；H5 仅在内存态持有 `apiAccessToken` 快照，不做持久化 |
| 代表性任务完成结果 | Native 播放承接层 + `POST /api/earn/complete-task` | 以 `EarnTaskPlayerResult` 回调触发为准 |
| 其它未开放任务反馈 | Web 本地受控反馈 | 不调用 Native 或后端写接口 |

## 跨端共享逻辑

| 共享逻辑 | 说明 | 涉及端 |
|---------|------|--------|
| 赚钱承载策略 | `/earn` 由 H5 承载，Native 只做容器与承接 | Web / iOS / Android |
| overview 首屏 contract | 进入 `/earn` 后固定请求 `GET /api/earn/overview` | Backend / Web |
| earn 独立消息命名空间 | bridge 与 host sync 全部使用 `earn.*`，不混入 `mall.*` | Web / iOS / Android |
| host sync transport | Native → H5 统一使用 `CustomEvent('earn.hostMessage', { detail })` | Web / iOS / Android |
| 登录拦截顺序 | 匿名点击代表性任务时先在 H5 页内展示登录引导，再决定是否触发 Native 登录承接 | Web / iOS / Android |
| 登录返回契约 | 登录成功 / 取消 / 关闭都必须回到 `/earn` 并同步 `earn.syncAuthState` + `earn.restoreContext(reason='login-return')` | Web / iOS / Android |
| earn API 凭证同步 | Native 在 `earn.syncAuthState` 中下发 `apiAccessToken` 快照，H5 仅内存持有并按 Bearer 调用 earn API | Backend / Web / iOS / Android |
| 播放承接契约 | 已登录点击代表性任务时，Native 打开现有播放页，但要额外维护 earn `taskId/source/returnTarget/videoId` 上下文 | Web / iOS / Android |
| player 结果契约 | 播放完成结果统一为 `EarnTaskPlayerResult(taskId, videoId, completed, reason)` | iOS / Android / Web |
| 任务完成闭环 | 只有 Native 播放承接层显式发出 `earn.completeTask(completed=true)`，H5 才调用 `POST /api/earn/complete-task` | Backend / Web / iOS / Android |
| 返回赚钱上下文 | 从登录/播放返回后，earn tab 保持高亮，至少恢复 `/earn` 首页首屏 | Web / iOS / Android |
| 浏览器模式降级 | 无 Native bridge 时只做页面反馈，不调用真实登录/播放 | Web |
| overview 刷新策略 | 登录成功返回、任务完成成功、容器重建后允许重新拉取 overview 兜底 | Web / iOS / Android |
| 幂等完成语义 | 同一 `taskId` 重复 complete-task 不重复累加金币 | Backend / Web |

### 状态机约定

```text
进入 earn tab
→ Native 容器 loading
→ H5 成功加载 / 容器 error(retryable)
→ H5 请求 GET /api/earn/overview
→ success(content) | empty | error(retryable)

匿名点击代表性任务
→ showLoginPrompt
→ cancel => stay in earn context
→ continue => bridge earn.requestLogin(EarnLoginContext)
→ native login adapter
→ success / cancel / fail / close
→ sync auth state + restore earn context
→ refresh overview if needed

已登录点击代表性任务
→ bridge earn.openTaskPlayer(EarnTaskContext)
→ native open player(videoId) with earn task context held by host
→ player emits EarnTaskPlayerResult(taskId, videoId, completed, reason)
→ completed=false => host only sends earn.restoreContext(reason='task-return')
→ completed=true => host sends earn.completeTask(result) then earn.restoreContext(reason='task-return')
→ H5 POST /api/earn/complete-task with Bearer token snapshot
→ success => update coins/task state or refresh overview
```

### bridge 事件约定

#### H5 → Native

| 事件 | Payload | 触发时机 | Native 行为 |
|------|---------|---------|------------|
| `earn.requestLogin` | `EarnLoginContext` | 匿名用户确认继续登录 | 打开 earn 专属登录承接适配层 |
| `earn.openTaskPlayer` | `EarnTaskContext` | 已登录用户点击代表性任务 | 打开现有 Native 播放页，并保存 earn 任务上下文 |

#### Native → H5

| 事件 | Payload | 触发时机 | H5 行为 |
|------|---------|---------|--------|
| `earn.syncAuthState` | `EarnHostAuthState` | 首次加载、登录成功/取消、App 恢复 | 更新 `isLoggedIn`、关闭过期登录提示 |
| `earn.restoreContext` | `EarnRestoreContext` | 登录返回、任务返回、容器重建 | 关闭弹层并按需刷新 overview |
| `earn.completeTask` | `EarnTaskCompletion` | 播放承接层判定任务完成后 | 调用 `POST /api/earn/complete-task` |

## 安全考虑

- **认证与授权**：
  - `GET /api/earn/overview` 允许匿名访问；
  - `POST /api/earn/complete-task` 必须校验现有 auth token；
  - H5 不能把“登录成功”作为可信输入直接传给后端。
- **数据校验**：
  - Backend 使用 Zod 校验 request / response；
  - Web 对 overview、complete-task、bridge message、host message 全部做 schema 校验；
  - Native 对 H5 发来的 bridge payload 做最小字段校验。
- **敏感数据处理**：
  - bridge payload 不携带 token、手机号、昵称等敏感信息；
  - 只传最小任务上下文 `taskId/source/returnTarget/videoId`。
- **幂等与防滥用**：
  - `complete-task` 以 `task_id + 当前用户` 维度保证幂等；
  - 首版不做复杂风控，但不允许重复调用无限加币。
- **信任边界**：
  - H5 可发起“打开登录/播放”的意图，但不拥有“已完成任务”的最终裁定权；
  - 最终完成信号必须来自 Native 播放承接层。

## 边界与错误处理（⚠️ 重点）

### 错误处理架构

- **全局错误处理策略**：Backend 新接口统一使用 `withErrorHandler`；Web 通过 `api-client.ts` 读取 `{ error: { code, message } }`；Native 容器统一维护 loading / success / error 三态。
- **错误响应格式**：失败统一返回 `{ error: { code, message } }`，参数校验失败可带 `details`。
- **错误日志与监控**：首版以现有日志为主；service / schema parse 失败需要打出受控错误日志，bridge 非法消息在端侧记录开发日志即可。

### API 错误码定义

| 业务错误码 | HTTP 状态码 | 说明 | 用户提示文案 |
|-----------|------------|------|-------------|
| `VALIDATION_ERROR` | 400 | request/query schema 校验失败 | 请求参数异常，请稍后重试 |
| `INVALID_PARAMS` | 400 | 业务参数非法（如 taskId 不满足业务要求） | 任务参数异常，请稍后重试 |
| `AUTH_UNAUTHORIZED` | 401 | 未登录或 token 无效 | 请先登录 |
| `NOT_FOUND` | 404 | 任务不存在 | 任务不存在或已失效 |
| `CONFLICT` | 409 | 任务已完成或状态冲突 | 任务已处理，请刷新后查看 |
| `INTERNAL_ERROR` | 500 | 服务内部错误 / seed 数据非法 | 服务开小差了，请稍后重试 |
| `SERVICE_UNAVAILABLE` | 503 | 未来真实数据源不可用 | 服务暂不可用，请稍后重试 |

### 边界场景处理

| 场景 | 触发条件 | API / 系统行为 | 说明 |
|------|---------|----------------|------|
| overview 匿名访问 | 未携带 token | 返回 `200` + 匿名视角数据 | 不报未登录错误 |
| overview 携带非法 token | 宿主误传失效 token | 可降级按匿名返回，或返回 `401 AUTH_UNAUTHORIZED`；实现时需保持单一策略 | 设计建议优先降级匿名，减少首屏打断 |
| complete-task 未登录 | 缺少或无效 token | 返回 `401 AUTH_UNAUTHORIZED` | H5 应提示重新登录 |
| complete-task 参数非法 | `task_id` 缺失 / 非 uuid | 返回 `400 VALIDATION_ERROR` 或 `INVALID_PARAMS` | 不执行任何奖励变更 |
| 同一任务重复完成 | 重复上报相同 `task_id` | 返回 `200` 幂等结果或 `409 CONFLICT` | 首版建议返回 200 幂等结果，便于前端收敛 |
| 播放未完成就返回 | Native 回调 `completed=false` | 不调用 `complete-task`，仅恢复 `/earn` | 奖励不增加 |
| 浏览器模式点击任务 | 无 bridge | 展示“请在 App 内完成”反馈 | 不跳错误页 |
| 容器被系统回收 | 登录 / 播放返回前 WebView 被销毁 | 重新加载 `/earn` 并同步最新 auth state | 最低保证恢复首屏 |
| host message 非法 | 原生注入 payload 缺字段 | H5 忽略消息，不崩溃 | 记录开发日志 |
| 7 宫格数据不满 7 项 | 非法 seed 数据 | Backend schema 校验失败并返回 500 | 不让脏数据进入 H5 |

## 性能考虑

- **预期 QPS**：首版偏内测 / 演示量级，接口压力较低。
- **缓存策略**：
  - 首版 Backend 不引入 Redis 缓存；
  - overview 由 Web 在页面生命周期内短暂持有；
  - 登录/播放返回后优先局部更新，必要时再重新拉取 overview。
- **页面体验优化**：
  - H5 首屏先渲染稳定骨架区块；
  - Web 将首屏错误态和局部反馈态分离；
  - Native 容器避免因局部接口错误回退到 placeholder。
- **数据库优化**：首版无真实数据库表；未来若接入真实奖励流水，再按用户 + task 维度建立唯一索引保证幂等。

## 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/index.md` | 索引 | 定位 earn / mall / app-shell 文档 |
| `wiki/features/app-shell/index.md` | 入口与路由、已知限制 | `earn` 仍为占位；mall 已完成 H5 容器模式 |
| `wiki/architecture/overview.md` | 承载策略、架构决策 | mall / earn 继续采用 H5 承载 + Native 容器 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `docs/specs/2026-07-29-prd-14-earn/spec.md` | PRD-14 的需求边界、用户故事与默认结论 |
| `docs/specs/2026-07-28-prd-13-mall/design.md` | 最近可复用的 H5 容器 shared design 范式 |
| `docs/specs/2026-07-28-prd-13-mall/design-backend.md` | Backend 分层、错误处理与 mock 仓储写法参考 |
| `docs/specs/2026-07-28-prd-13-mall/design-web.md` | Web feature / hook / bridge / host sync 状态机范式 |
| `backend/src/middleware/error-handler.ts` | 失败响应统一为 `{ error: { code, message } }` |
| `backend/src/lib/errors.ts` | 可用错误码枚举与状态码映射 |
| `backend/src/app/api/auth/sessions/route.ts` | auth 成功响应的特殊 `{ code, data, message }` 包裹现实 |
| `backend/src/middleware/auth.ts` | 通过 Authorization 头解析可选 / 必需登录态 |
| `backend/src/app/api/player/progress/route.ts` | 当前 player progress contract 不带 earn task context |
| `backend/src/app/api/player/stop/route.ts` | 当前 player stop contract 不带 earn task callback |
| `backend/src/repositories/repository-registry.ts` | 需要新增 earn repository 注册 |
| `backend/src/services/mall/mall.service.ts` | earn service 的最近实现风格参考 |
| `backend/src/repositories/interfaces/mall.repository.interface.ts` | earn repository interface 参考 |
| `web/src/lib/config.ts` | 当前只有 mall 配置，earn 需新增 route / bridge 配置 |
| `web/src/lib/api-client.ts` | 已适配嵌套 `error.message` 解析 |
| `web/src/features/mall/hooks/useMallPage.ts` | earn 页面状态机、恢复逻辑、反馈建模参考 |
| `web/src/features/mall/bridge/mall-bridge.ts` | mall-specific bridge，earn 需独立协议 |
| `web/src/features/mall/bridge/mall-host-sync.ts` | mall-specific host sync，earn 需独立协议 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 当前没有 earn route，player route 仅带 `videoId` |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | mall 搜索/登录恢复模式可参考到 earn |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | earn tab 仍是 placeholder |
| `ios/ShortDrama/Sources/Core/Config/AppConfig.swift` | 当前仅暴露 mall URL，无 earn URL |
| `ios/ShortDrama/Sources/Features/Mall/Views/MallContainerView.swift` | iOS mall 容器组织参考 |
| `ios/ShortDrama/Sources/Features/Mall/Views/MallLoginPlaceholderView.swift` | mall 登录承接是 mall-specific 占位实现 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | Android 仅有 `play/{videoId}` 与 `mall/login`，无 earn route |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | earn graph 仍渲染 placeholder |
| `android/app/src/main/java/com/djs66256/short_drama/core/config/AppConfig.kt` | 当前仅暴露 `mallBaseUrl`，无 earn URL |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt` | Android 容器状态与 effect 参考 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallScreen.kt` | Android 容器 + host message dispatcher 参考 |
| `docs/product_manager/prd/2026-07-25-earn-center/subtasks.md` | 产品经理对子任务、API 草案和工时拆分 |
| `docs/product_manager/prd/2026-07-25-earn-center/prd-review.md` | 金币术语与 complete-task API 方向 |
