# 实现计划：Web — PRD-14 赚钱中心

> 创建日期：2026-07-29
> 对应技术方案：design-web.md
> 对应需求：spec.md

## 概述

Web 端本期负责把 `/earn` 落地为可被 Native 容器加载的赚钱中心 H5 首页，并严格遵守现有五层架构：Page 层只做路由委托，业务状态与交互下沉到 Feature 层，所有 API 调用统一经 `web/src/lib/api-client.ts` 和 Core 封装发起。实现以轻量 TDD 推进，先覆盖 schema、bridge、host sync、状态机与关键页面交互，再完成首页组件与总体验证；不新增未经批准的依赖，不扩展 design 之外的能力。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | Earn schema 与配置校验通过 | 合法/非法的 `EarnOverview`、`EarnTask`、`EarnHostMessage`、earn config 输入 | 合法输入可通过 Zod；非法 `taskId`、非法 `returnTarget`、非法事件名、非法结构被拒绝 | 单元测试 | P0 |
| T-02 | Earn API 调用显式携带 Bearer 且基于 Core base URL | `NEXT_PUBLIC_API_URL` 已配置，`complete-task` 传入 token | `GET /api/earn/overview` 通过 Core client 调用；`POST /api/earn/complete-task` 显式带 `Authorization: Bearer <token>` | 单元测试 | P0 |
| T-03 | Earn bridge 在宿主模式与浏览器模式下正确分流 | Native bridge 可用或不可用两种环境，触发登录或打开任务播放 | bridge 可用时发送 `earn.requestLogin` / `earn.openTaskPlayer`；不可用时返回受控降级反馈，不跳空白页 | 单元测试 | P0 |
| T-04 | Host sync 只消费 `CustomEvent('earn.hostMessage')` | 合法 `CustomEvent`、普通 `message` 事件、非法 `detail` | 仅 `earn.hostMessage` 被解析；其它 transport 或非法 payload 被忽略 | 单元测试 | P0 |
| T-05 | 赚钱首页首屏成功渲染 | overview 成功返回标准数据 | 页面展示收益头图、新手任务卡、7 宫格连续福利、现金任务列表 | 组件测试 | P0 |
| T-06 | 首屏错误态、现金任务空态与重试路径正确 | overview 请求失败，或 overview 成功但 `cash_tasks=[]` | 失败时展示错误态与重试；空任务时保留头图与模块骨架并展示空态说明 | 组件测试 | P0 |
| T-07 | 匿名点击代表性任务触发登录引导 | `isLoggedIn=false`，点击新手任务或代表性现金任务 | 显示页内登录引导；取消后留在当前页；继续时进入 earn 登录 bridge 或浏览器降级反馈 | 组件测试 | P0 |
| T-08 | 宿主同步登录态与 Bearer 快照 | `earn.syncAuthState` 在 `initial-load` / `login-success` / `login-cancel` 场景下到达 | 页面更新 `isLoggedIn` 与内存态 token；过期登录提示关闭；不把 token 写入持久化存储 | 单元测试 | P0 |
| T-09 | 任务完成回调闭环正确 | 收到 `earn.completeTask`，分别覆盖 `completed=true/false` 与 token 缺失 | `completed=true` 且有 token 时调用 `complete-task` 并更新收益；`completed=false` 不调用接口；缺 token 时提示重新登录 | 集成测试 | P0 |
| T-10 | `restoreContext` 恢复赚钱上下文 | `earn.restoreContext(reason='login-return' | 'task-return' | 'container-recreated')` | 关闭弹层与临时反馈；必要时重拉 overview；页面保持 `/earn` 语义 | 单元测试 | P1 |

## 实现步骤

### Step 1：补齐 Earn Core 契约与配置入口

- **关联测试**：T-01、T-02
- **目标文件**：`web/src/lib/schemas.ts`、`web/src/lib/config.ts`、`web/src/lib/api-client.ts`、`web/src/lib/schemas.test.ts`、`web/src/lib/config.test.ts`、`web/src/lib/api-client.test.ts`
- **实现内容**：
  1. 在 `web/src/lib/schemas.ts` 新增 earn 领域 schema 与类型，覆盖 `EarnOverview`、`EarnTask`、`EarnDailyReward`、`CompleteEarnTaskRequest/Response`、`EarnTaskContext`、`EarnLoginContext`、`EarnBridgeMessage`、`EarnHostMessage`。
  2. 在 `web/src/lib/config.ts` 新增 `config.earn` 配置块，集中维护 `/earn` 路由、bridge 开关、浏览器模式反馈文案等受控常量，并明确 `NEXT_PUBLIC_API_URL` 仍为 API base URL 来源。
  3. 在不破坏现有调用方的前提下，收口 `web/src/lib/api-client.ts` 的请求头与错误解析测试，确保 earn `complete-task` 可通过 Core 层显式传入 Bearer header。
  4. 先补齐 `schemas/config/api-client` 的合法输入、非法输入、错误路径与 Authorization 透传测试，再进入 Feature 实现。
- **验证方式**：
  - 运行 `cd web && npm test -- src/lib/schemas.test.ts src/lib/config.test.ts src/lib/api-client.test.ts` 确认 T-01、T-02 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/lib/schemas.ts` | 修改 | 新增 earn 数据、bridge、host sync schema |
| `web/src/lib/config.ts` | 修改 | 新增 earn 配置读取与受控常量 |
| `web/src/lib/api-client.ts` | 修改 | 保持 Core 封装统一入口并支持 earn Bearer 请求测试收口 |
| `web/src/lib/schemas.test.ts` | 修改 | 覆盖 earn schema 的合法/非法输入 |
| `web/src/lib/config.test.ts` | 修改 | 覆盖 earn 配置读取与默认值 |
| `web/src/lib/api-client.test.ts` | 修改 | 覆盖 Authorization header 与错误结构解析 |

### Step 2：实现 Earn API 封装与 bridge / host sync 适配层

- **关联测试**：T-02、T-03、T-04
- **目标文件**：`web/src/lib/earn/api.ts`、`web/src/lib/earn/api.test.ts`、`web/src/features/earn/bridge/earn-bridge.ts`、`web/src/features/earn/bridge/earn-host-sync.ts`、`web/src/features/earn/bridge/earn-bridge.test.ts`、`web/src/features/earn/bridge/earn-host-sync.test.ts`
- **实现内容**：
  1. 新增 `web/src/lib/earn/api.ts`，统一封装 `getOverview()` 与 `completeTask()`，所有网络请求继续经 `web/src/lib/api-client.ts` 发起，并在 `completeTask()` 中显式要求调用方传入 Bearer token。
  2. 新增 `web/src/features/earn/bridge/earn-bridge.ts`，封装 `earn.requestLogin` 与 `earn.openTaskPlayer` 两类 H5 → Native 消息；无 bridge 时返回浏览器模式反馈结果，不复用 mall 命名空间，也不增加未设计的 fallback 协议。
  3. 新增 `web/src/features/earn/bridge/earn-host-sync.ts`，只监听 `window` 上的 `CustomEvent('earn.hostMessage')`，解析 `earn.syncAuthState`、`earn.restoreContext`、`earn.completeTask`；明确忽略 `window.message` 和非法 payload。
  4. 为 earn API、bridge、host sync 先补齐测试，确保 transport、命名空间、Bearer 头与浏览器降级都在 Feature 落地前被锁定。
- **验证方式**：
  - 运行 `cd web && npm test -- src/lib/earn/api.test.ts src/features/earn/bridge/earn-bridge.test.ts src/features/earn/bridge/earn-host-sync.test.ts` 确认 T-02、T-03、T-04 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/lib/earn/api.ts` | 新增 | earn overview / complete-task Core API 封装 |
| `web/src/lib/earn/api.test.ts` | 新增 | 覆盖 API schema、Bearer header 与错误路径 |
| `web/src/features/earn/bridge/earn-bridge.ts` | 新增 | earn 登录/播放 bridge 适配层 |
| `web/src/features/earn/bridge/earn-host-sync.ts` | 新增 | earn 宿主消息订阅与解析 |
| `web/src/features/earn/bridge/earn-bridge.test.ts` | 新增 | 覆盖 bridge 可用/不可用、浏览器降级与异常路径 |
| `web/src/features/earn/bridge/earn-host-sync.test.ts` | 新增 | 覆盖 `earn.hostMessage` transport 与非法消息过滤 |

### Step 3：实现 Earn 页面状态机与任务完成闭环 Hook

- **关联测试**：T-06、T-07、T-08、T-09、T-10
- **目标文件**：`web/src/features/earn/hooks/useEarnPage.ts`、`web/src/features/earn/hooks/useEarnPage.test.ts`
- **实现内容**：
  1. 新增 `useEarnPage`，统一管理 `overview`、`isLoading`、`errorMessage`、`isLoggedIn`、`apiAccessToken`、`loginPromptVisible`、`activeTask`、`pendingCompletionTaskId`、`feedbackMessage`、`pendingRestoreReason` 等页面状态。
  2. 在 Hook 中接入 `getOverview()` 的首屏加载、现金任务空态、错误态、重试与局部反馈，不把请求逻辑下沉到组件层，保持 Feature 通过 Core API 封装发请求。
  3. 实现匿名点击任务时的登录引导逻辑；继续登录走 `earn.requestLogin`，取消则停留当前页；已登录点击代表性任务时走 `earn.openTaskPlayer`。
  4. 订阅 `earn.syncAuthState` / `earn.restoreContext` / `earn.completeTask`：仅当收到 `completed=true` 且内存中存在 token 快照时调用 `complete-task`；`401` 或 token 缺失时清空本地快照并重新展示登录引导；`completed=false` 只恢复上下文不发请求。
  5. 用 Hook 测试锁定状态转换、登录引导开关、token 只存内存、任务完成闭环与 `container-recreated` 重拉 overview 语义。
- **验证方式**：
  - 运行 `cd web && npm test -- src/features/earn/hooks/useEarnPage.test.ts` 确认 T-06、T-07、T-08、T-09、T-10 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/features/earn/hooks/useEarnPage.ts` | 新增 | 赚钱首页状态机、登录引导、任务完成闭环 |
| `web/src/features/earn/hooks/useEarnPage.test.ts` | 新增 | 覆盖业务逻辑、状态转换、回调闭环与恢复语义 |

### Step 4：落地 Earn 首页 Feature 组件与 `/earn` 路由承接

- **关联测试**：T-05、T-06、T-07
- **目标文件**：`web/src/app/earn/page.tsx`、`web/src/features/earn/EarnPageScreen.tsx`、`web/src/features/earn/EarnPageScreen.test.tsx`、`web/src/features/earn/index.ts`、`web/src/features/earn/components/*`
- **实现内容**：
  1. 新增 `web/src/app/earn/page.tsx`，Page 层仅负责路由委托到 `EarnPageScreen`，不在 Page 层编写状态逻辑或直接请求接口。
  2. 新增 `EarnPageScreen` 与首页子组件，按 design 固定顺序渲染收益头图、新手任务卡、连续看剧福利 7 宫格、现金任务列表、登录引导层、反馈提示、空态与错误态。
  3. 组件层只消费 `useEarnPage` 提供的状态与回调；Feature 不直接发请求，所有动作都回到 Hook 和 Core API 封装。
  4. 浏览器模式下保持按钮可见并给出“需在 App 内完成”或“暂时无法打开登录”的受控反馈，不跳转不存在页面；未开放任务仅在当前页反馈，不扩展新流程。
  5. 使用现有 CSS Modules 与 Design System tokens 组织样式，不把业务样式或文案散落到 Shared UI。
- **验证方式**：
  - 运行 `cd web && npm test -- src/features/earn/EarnPageScreen.test.tsx` 确认 T-05、T-06、T-07 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/app/earn/page.tsx` | 新增 | `/earn` 路由壳层，仅委托 Feature |
| `web/src/features/earn/EarnPageScreen.tsx` | 新增 | 赚钱首页主 Feature |
| `web/src/features/earn/EarnPageScreen.test.tsx` | 新增 | 覆盖首页渲染、错误态、空态与登录引导 |
| `web/src/features/earn/index.ts` | 新增 | Earn Feature 导出入口 |
| `web/src/features/earn/components/*` | 新增 | 收益头图、任务卡、福利宫格、登录引导、反馈等组件 |

### Step 5：执行 Web 端总体验证并补齐回归测试

- **关联测试**：T-01 ~ T-10
- **目标文件**：`web/src/lib/*.test.ts`、`web/src/lib/earn/*.test.ts`、`web/src/features/earn/**/*.test.ts`、`web/src/features/earn/**/*.test.tsx`
- **实现内容**：
  1. 回看前 4 步测试缺口，补齐 schema 边界、浏览器降级反馈、`earn.hostMessage` 非法 payload、任务完成重复回调、`restoreContext` 恢复语义等回归测试。
  2. 运行 Web 端全量测试、lint、build，确认新增 earn Feature 不破坏现有 home、mall、player、detail、search 与 admin 路由。
  3. 如总体验证暴露路径导出、样式引用或类型收口问题，只在已规划的 Core / Feature 文件内修复，不扩展需求范围，不新增依赖。
- **验证方式**：
  - 运行 `cd web && npm test` 确认 T-01 ~ T-10 全部通过
  - 运行 `cd web && npm run lint` 确认无新增 lint 错误
  - 运行 `cd web && npm run build` 确认构建成功
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/lib/*.test.ts` | 修改 | 补齐 earn 相关 Core 回归测试 |
| `web/src/lib/earn/*.test.ts` | 修改/新增 | 补齐 API 封装与 Bearer 头回归测试 |
| `web/src/features/earn/**/*.test.ts` | 修改/新增 | 补齐 bridge、host sync、Hook 状态机测试 |
| `web/src/features/earn/**/*.test.tsx` | 修改/新增 | 补齐页面渲染、交互与错误态测试 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5
```

## 验证总览

- [ ] 所有测试通过（`cd web && npm test`）
- [ ] Lint 通过（`cd web && npm run lint`）
- [ ] Build 成功（`cd web && npm run build`）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/app/earn/page.tsx` | 新增 | 赚钱首页 App Router 路由委托 |
| `web/src/lib/schemas.ts` | 修改 | 新增 earn schema 与消息契约 |
| `web/src/lib/config.ts` | 修改 | 新增 earn 配置与受控常量 |
| `web/src/lib/api-client.ts` | 修改 | 复用 Core 请求封装并支持 earn Bearer 调用收口 |
| `web/src/lib/earn/api.ts` | 新增 | earn overview / complete-task API 封装 |
| `web/src/features/earn/EarnPageScreen.tsx` | 新增 | 赚钱中心首页 Feature |
| `web/src/features/earn/index.ts` | 新增 | Earn Feature 导出入口 |
| `web/src/features/earn/components/*` | 新增 | 赚钱首页业务组件 |
| `web/src/features/earn/hooks/useEarnPage.ts` | 新增 | 首页状态机与任务完成闭环 |
| `web/src/features/earn/bridge/earn-bridge.ts` | 新增 | earn 登录/播放 bridge 适配层 |
| `web/src/features/earn/bridge/earn-host-sync.ts` | 新增 | 只监听 `earn.hostMessage` 的宿主同步层 |
| `web/src/lib/schemas.test.ts` | 修改 | earn schema 测试 |
| `web/src/lib/config.test.ts` | 修改 | earn 配置测试 |
| `web/src/lib/api-client.test.ts` | 修改 | Authorization 与错误解析测试 |
| `web/src/lib/earn/api.test.ts` | 新增 | earn API 封装测试 |
| `web/src/features/earn/**/*.test.ts` | 新增/修改 | bridge、host sync、Hook 测试 |
| `web/src/features/earn/**/*.test.tsx` | 新增/修改 | 页面渲染、交互、错误态测试 |

## 风险提示

- 最关键风险是 **宿主回传协议不一致**：design 已明确 earn host sync 只能监听 `CustomEvent('earn.hostMessage')`，若实现时混入 mall 的 `window.message` 旧路径，Web 状态机、登录态同步和任务完成闭环会出现双协议分叉，后续 Native 联调成本最高。
- 次级风险是 **`complete-task` Bearer 快照管理**：token 只能由 Native 通过 `earn.syncAuthState` 下发并保存在 Web 内存态，若误写入持久化存储或在 token 缺失时仍提交请求，会直接偏离 design 的信任边界。