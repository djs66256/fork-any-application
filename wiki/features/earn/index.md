# 赚钱中心 (Earn Center)

> 最后更新：2026-07-29
> 覆盖端：Web / Android / iOS / Backend

## 功能概述

赚钱中心是由 Web `/earn` 页面提供 UI、由 Android / iOS 原生容器接入的 H5 频道。Native 宿主负责加载赚钱首页、拦截登录与任务播放桥接、向 H5 注入权威登录态与任务回流消息；H5 仅在内存态持有 `apiAccessToken`，并且只有在 Native 播放承接层明确回传 `completed=true` 时才调用奖励结算接口（`web/src/app/earn/page.tsx:1-5`, `web/src/features/earn/hooks/useEarnPage.ts:327-368`, `android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt:163-206`, `ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift:101-135`）。

- 核心价值：在保持赚钱页由 H5 承载的前提下，让 Native 宿主接管登录、播放与任务完成回流，形成最小可用的赚钱闭环
- 覆盖范围：Web `/earn` 页面、Android / iOS WebView/WKWebView 容器、Backend 赚钱首页与任务结算接口
- 当前状态：Web / Android / iOS / Backend 已落地；真实提现、连续看剧福利发奖与多任务并发结算仍未实现

## 入口与路由

| 端 | 入口 | 路由 / deeplink | 源文件 |
|----|------|----------------|--------|
| Web | 赚钱首页 H5 页面 | `/earn` | `web/src/app/earn/page.tsx:1-5`, `web/src/features/earn/EarnPageScreen.tsx:15-78` |
| Android | 底部 `赚钱` 一级 Tab | `earn`、`earn/login?returnTarget=...`、`earn/play?taskId=...&source=earn&returnTarget=/earn&videoId=...` | `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:14-18`, `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:61-63`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:416-570` |
| iOS | `earn` 一级 Tab 与 earn 专属子路由 | `AppTab.earn`、`.earnLogin(context:)`、`.earnPlayer(context:)` | `ios/ShortDrama/Sources/App/AppTab.swift:3-41`, `ios/ShortDrama/Sources/App/AppRoute.swift:23-32`, `ios/ShortDrama/Sources/App/TabNavigationHostView.swift:53-82` |
| Backend | 赚钱首页与结算接口 | `GET /api/earn/overview`、`POST /api/earn/complete-task` | `backend/src/app/api/earn/overview/route.ts:7-12`, `backend/src/app/api/earn/complete-task/route.ts:8-15` |

## 核心逻辑

### 流程：Native 容器加载赚钱首页并同步宿主登录态

1. Web 页面层只委托给 `EarnPageScreen`，真正的状态机都在 `useEarnPage()` 中；页面初次挂载时立即调用 `getEarnOverview()` 拉取首屏数据（`web/src/app/earn/page.tsx:1-5`, `web/src/features/earn/EarnPageScreen.tsx:15-78`, `web/src/features/earn/hooks/useEarnPage.ts:263-284,323-325`）。
2. Backend `GET /api/earn/overview` 通过 `resolveOptionalAuthContext()` 解析可选 bearer token，再由 `EarnService.getOverview()` 返回统一的 `EarnOverviewResponse`；无 token 或无效 token 都会回退匿名视角（`backend/src/app/api/earn/overview/route.ts:7-12`, `backend/src/services/earn/earn.service.ts:18-30`, `backend/src/app/api/__tests__/earn-overview.test.ts:37-90`）。
3. Android `EarnViewModel` 与 iOS `EarnContainerViewModel` 都会把配置中的 earn 基础地址规范化为 `/earn` 首页，并在页面首次加载成功后向 H5 发送 `earn.syncAuthState`（`android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt:62-76`, `android/app/src/test/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModelTest.kt:49-61`, `ios/ShortDrama/Sources/Core/Config/AppConfig.swift:36-49`, `ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift:48-72`）。
4. H5 只监听 `CustomEvent('earn.hostMessage', { detail })`；事件必须通过 `EarnHostMessageSchema` 校验后才会生效，非法 payload 会被忽略（`web/src/features/earn/bridge/earn-host-sync.ts:14-49`, `web/src/lib/schemas.ts:256-314`）。
5. 当 H5 收到 `earn.syncAuthState` 时，会更新 `isLoggedIn` 和内存态 `apiAccessToken`；如果宿主未登录或未下发 token，则 Web 侧不会持有 bearer token（`web/src/features/earn/hooks/useEarnPage.ts:327-338`）。

### 流程：匿名用户在 H5 内触发登录引导，再由 Native 宿主承接登录

1. H5 点击可执行任务时，若当前 `isLoggedIn=false`，不会直接发起 bridge，而是先在赚钱页内显示登录引导层（`web/src/features/earn/hooks/useEarnPage.ts:370-400`, `web/src/features/earn/EarnPageScreen.tsx:68-74`）。
2. 用户点击“继续登录”后，H5 通过 `requestEarnLogin()` 向 `__EARN_NATIVE_BRIDGE__` 发送 `earn.requestLogin`；若 bridge 不存在，则回退为浏览器受控提示（`web/src/features/earn/bridge/earn-bridge.ts:15-29`, `web/src/features/earn/bridge/earn-bridge.ts:44-58`, `web/src/lib/config.ts:21-32`）。
3. Android WebView 和 iOS WKWebView 都通过 `earnBridge` 通道解析 `earn.requestLogin`，并要求 `source='earn'`、`returnTarget='/earn'` 才视为合法上下文（`android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnWebViewContainer.kt:97-104`, `android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnWebViewContainer.kt:154-185`, `ios/ShortDrama/Sources/Features/Earn/Models/EarnBridgeMessage.swift:7-39`）。
4. Android `EarnViewModel.onBridgeMessage()` 与 iOS `EarnContainerViewModel.handleBridgeMessage()` 都会忽略非法或重复登录请求，只保留一个 `pendingLoginContext`，并发出打开 earn 专属登录承接页的路由效果（`android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt:127-161`, `android/app/src/test/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModelTest.kt:99-122`, `ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift:80-91`, `ios/ShortDrama/Tests/ViewModelTests/EarnContainerViewModelTests.swift:172-208`）。
5. 登录返回时，Android 先发 `earn.syncAuthState(reason=login-success|login-cancel)`，再发 `earn.restoreContext(reason=login-return)`；iOS 也遵循同样顺序，并通过 `router.dismissEarnLogin(completed:)` 保持选中 tab 仍为 `earn`（`android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt:163-175`, `android/app/src/test/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModelTest.kt:160-193`, `ios/ShortDrama/Sources/App/NavigationRouter.swift:264-272`, `ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift:101-111`, `ios/ShortDrama/Tests/ViewModelTests/EarnContainerViewModelTests.swift:80-120`）。

### 流程：已登录用户打开代表性任务播放器，并仅在自然播放完成后结算奖励

1. H5 只有在已登录且任务 `action.type === 'play'` 时，才会调用 `openEarnTaskPlayer()` 发送 `earn.openTaskPlayer`；浏览器环境下会降级为“请在 App 内完成该任务”提示（`web/src/features/earn/hooks/useEarnPage.ts:381-399`, `web/src/features/earn/bridge/earn-bridge.ts:60-72`, `web/src/lib/config.ts:24-31`）。
2. Web 传给宿主的任务上下文固定包含 `taskId`、`source='earn'`、`returnTarget='/earn'`、`videoId`，并由 Zod schema 约束（`web/src/lib/schemas.ts:243-254`, `web/src/features/earn/hooks/useEarnPage.ts:386-391`）。
3. Android 把该桥接请求映射为 `earn/play?...` 路由；iOS 则映射为 `.earnPlayer(context:)`，最终都复用原生播放器主路径，而不是在 H5 内自行播放（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:420-441`, `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:162-169`, `ios/ShortDrama/Sources/App/TabNavigationHostView.swift:31-35`, `ios/ShortDrama/Sources/App/NavigationRouter.swift:274-283`）。
4. Android 的 earn 播放承接层只有在 `onPlaybackCompleted` 时才产出 `completed=true`；手动返回或上下文非法时都只产生 `completed=false` 的失败/退出结果（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:514-566`）。
5. iOS 也只在 `PlayerViewModel.handlePlaybackEnded()` 中对 earn 上下文调用 `router.finishEarnTaskPlayer(result: completed=true)`；返回、后台切换、消失或播放失败都走 `completed=false` 的结果（`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:88-126`, `ios/ShortDrama/Sources/Domain/Entities/EarnTaskPlayerResult.swift:18-40`）。
6. Android `EarnViewModel.onEarnTaskPlayerResult()` 与 iOS `EarnContainerViewModel.handleTaskPlayerResult()` 都遵循固定顺序：只有 `result.completed=true` 时先发送 `earn.completeTask`，随后再发送 `earn.restoreContext(reason=task-return)`；`completed=false` 时只发送 restore（`android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt:177-191`, `android/app/src/test/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModelTest.kt:197-260`, `ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift:113-124`, `ios/ShortDrama/Tests/ViewModelTests/EarnContainerViewModelTests.swift:122-170`）。
7. Web 侧在收到 `earn.completeTask` 时会先检查 `payload.completed`；只有为 true 才会进入 `completeTaskFlow()`，否则直接忽略（`web/src/features/earn/hooks/useEarnPage.ts:341-351`）。
8. H5 调用 `completeEarnTask(taskId, accessToken)` 时总是把内存态 token 放进 `Authorization: Bearer ...` header；若 token 缺失或接口返回 401，则清空内存 token 并重新展示登录引导（`web/src/lib/earn/api.ts:15-30`, `web/src/features/earn/hooks/useEarnPage.ts:286-320`）。
9. Backend `POST /api/earn/complete-task` 只接受 bearer 鉴权，且当前只有代表性任务可结算；重复提交保持幂等，第二次 `coins_earned=0`、`total_coins` 不再增加（`backend/src/app/api/earn/complete-task/route.ts:8-15`, `backend/src/repositories/mock/earn.mock.repository.ts:138-164`, `backend/src/app/api/__tests__/earn-complete-task.test.ts:92-168`）。

### 边界与异常处理

| 场景 | 处理方式 | 源文件 |
|------|---------|--------|
| 浏览器环境没有 earn bridge | 登录与任务入口都返回 `browser-fallback`，H5 展示受控提示，不尝试打开 Native 页面 | `web/src/features/earn/bridge/earn-bridge.ts:44-72`, `web/src/features/earn/hooks/useEarnPage.ts:393-417` |
| 非法或未知的宿主消息 | Web 仅消费 `CustomEvent('earn.hostMessage')` 且必须通过 schema 校验，非法 payload 直接忽略 | `web/src/features/earn/bridge/earn-host-sync.ts:14-49` |
| H5 未持有 token 却收到了完成回调 | `completeTaskFlow()` 直接转为 `require-relogin`，不伪造到账结果 | `web/src/features/earn/hooks/useEarnPage.ts:286-294` |
| `complete-task` 返回 401 | Web 清空内存 token、切回未登录态并重新展示登录引导 | `web/src/features/earn/hooks/useEarnPage.ts:303-312` |
| Android / iOS 收到非法或重复 bridge 请求 | Native 容器忽略无效上下文或重复请求，不重复打开登录页/播放器 | `android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt:127-161`, `ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift:80-91` |
| Android 主文档加载失败 | 原生容器展示错误态与重试入口，而不是回退到 placeholder tab | `android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnWebViewContainer.kt:119-151`, `android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt:114-124` |
| iOS 容器加载失败或被系统回收 | `EarnContainerViewModel` 进入 `.error`；容器重建时重新加载 `/earn` 并发送 `earn.restoreContext(reason=container-recreated)` | `ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift:73-79`, `ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift:130-135`, `ios/ShortDrama/Tests/ViewModelTests/EarnContainerViewModelTests.swift:210-249` |
| 非自然结束的任务播放返回 | Native 只发送 `earn.restoreContext(reason=task-return)`，Web 不调用 `POST /api/earn/complete-task` | `android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt:177-191`, `ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift:113-124`, `web/src/features/earn/hooks/useEarnPage.ts:346-350` |

## 多端实现

### Web

- 页面入口：`web/src/app/earn/page.tsx:1-5`
- 功能屏：`web/src/features/earn/EarnPageScreen.tsx:15-78`
- 状态机：`web/src/features/earn/hooks/useEarnPage.ts:21-451`
- bridge 与 host sync：`web/src/features/earn/bridge/earn-bridge.ts:9-72`, `web/src/features/earn/bridge/earn-host-sync.ts:1-49`
- 接口封装与 schema：`web/src/lib/earn/api.ts:1-30`, `web/src/lib/schemas.ts:122-182,243-314`
- 特点：Page 层只做路由委托；H5 仅以内存态持有 `apiAccessToken`，并通过 Zod 约束宿主协议

### Android

- 宿主配置与路由：`android/app/src/main/java/com/djs66256/short_drama/core/config/AppConfig.kt:8-15`, `android/app/src/main/java/com/djs66256/short_drama/core/config/BuildConfigAppConfig.kt:11-18`, `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:14-18,61-63,158-169`
- 容器与状态机：`android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt:31-258`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:416-570`
- WebView 宿主协议：`android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnWebViewContainer.kt:31-237`
- 自动化证据：`android/app/src/test/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModelTest.kt:49-260`
- 特点：通过 `CustomEvent('earn.hostMessage')` 把宿主登录态、上下文恢复和任务结果注入 H5，并保持 `earn` 底部 tab 持续高亮

### iOS

- 宿主配置与 tab 承载：`ios/ShortDrama/Sources/Core/Config/AppConfig.swift:4-64`, `ios/ShortDrama/Sources/App/AppTab.swift:3-41`, `ios/ShortDrama/Sources/App/TabNavigationHostView.swift:53-82`
- 路由与回跳：`ios/ShortDrama/Sources/App/AppRoute.swift:23-32`, `ios/ShortDrama/Sources/App/NavigationRouter.swift:18-21,264-293`
- 容器与状态机：`ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift:4-185`, `ios/ShortDrama/Sources/Features/Earn/Views/EarnContainerView.swift:3-111`
- WKWebView 宿主协议：`ios/ShortDrama/Sources/Features/Earn/Views/Components/EarnWebView.swift:4-100`, `ios/ShortDrama/Sources/Features/Earn/Models/EarnHostMessage.swift:32-107`, `ios/ShortDrama/Sources/Features/Earn/Models/EarnBridgeMessage.swift:3-40`
- 自动化证据：`ios/ShortDrama/Tests/ViewModelTests/EarnContainerViewModelTests.swift:22-250`
- 特点：用 `fullScreenCover` 承接 earn 登录占位页，用 `.earnPlayer(context:)` 复用原生播放器，并在 `completeTask` JavaScript 注入完成后再恢复上下文

### Backend

- 路由层：`backend/src/app/api/earn/overview/route.ts:1-13`, `backend/src/app/api/earn/complete-task/route.ts:1-16`
- 业务层：`backend/src/services/earn/earn.service.ts:15-47`
- Repository 与 mock 数据：`backend/src/repositories/mock/earn.mock.repository.ts:18-195`
- schema 与错误：`backend/src/lib/schemas.ts:122-182`, `backend/src/lib/errors.ts:1-136`
- 自动化证据：`backend/src/app/api/__tests__/earn-overview.test.ts:30-101`, `backend/src/app/api/__tests__/earn-complete-task.test.ts:18-192`
- 特点：严格遵循 Route → Service → Repository 分层，overview 使用可选鉴权，complete-task 保持 Bearer-only

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/earn/overview` | [../../api/earn.md](../../api/earn.md) | H5 首屏加载赚钱总览与任务数据，支持匿名/已登录双视角 |
| `POST /api/earn/complete-task` | [../../api/earn.md](../../api/earn.md) | H5 在收到 `earn.completeTask(completed=true)` 后结算代表性任务奖励 |
| `POST /api/player/start` | [../../api/player.md](../../api/player.md) | Native 播放承接层复用原生播放器主路径起播接口 |
| `POST /api/player/stop` | [../../api/player.md](../../api/player.md) | Native 播放承接层在退出/完成时复用停止上报接口 |
| `GET /api/users/me` | [../../api/auth.md](../../api/auth.md) | Native 宿主认证状态恢复后，为 `earn.syncAuthState` 提供权威登录快照 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Web `EarnPageState` | `useReducer` + `useRef` | 页面级 | 聚合 overview、登录态、内存 token、登录引导、待结算任务、反馈消息与 restore reason | `web/src/features/earn/hooks/useEarnPage.ts:21-62,155-247` |
| Web `apiAccessToken` | 内存态 reducer 字段 | 页面级 | 只从 `earn.syncAuthState` 更新，不做持久化；401 时立即清空 | `web/src/features/earn/hooks/useEarnPage.ts:193-230,327-338` |
| Android `EarnUiState` | `MutableStateFlow` | 页面级 | 聚合容器加载态、当前 URL、pending login/task context | `android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt:31-45,68-80` |
| Android `EarnEffect` | `MutableSharedFlow` | 页面级 | 宿主导航与 host message 注入统一通过 effect 下发 | `android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt:47-59,76-80` |
| iOS `EarnContainerViewModel` 发布状态 | `@Published` 属性集 | 页面级 | 管理当前请求、容器加载态、pending login/task context、route effect 与 host message | `ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift:14-29` |
| iOS `NavigationRouter.pendingEarnRestoreRequest` | `@Published` | 应用级 | 统一保存登录返回或任务返回后的 earn 上下文恢复请求 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:18-21,45-47,268-293` |
| Backend 用户任务完成集 | `Map<string, Set<string>>` | Repository 级 | 当前 mock 仓库按 `userId` 记录已完成的代表性任务，用于幂等结算 | `backend/src/repositories/mock/earn.mock.repository.ts:93-107,149-188` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 应用壳 | 一级频道与容器承载 | Android/iOS 都依赖 App Shell 把 `earn` 作为底部 Tab 承载，并管理登录/播放器回跳 |
| 认证体系 | 宿主登录态同步 | Native 登录成功/取消后通过 `earn.syncAuthState` 与 `earn.restoreContext` 把权威登录结果回流给 H5 |
| 播放器 | 原生播放承接 | 代表性任务不在 H5 内播放，而是复用 Native 播放器并把结果回流到赚钱页 |
| 数据模型 | 共享 earn schema / bridge contract | Web、Backend、Android、iOS 共同围绕 taskId/source/returnTarget/videoId 与 host message 语义实现 |

### 外部依赖

| 服务 | 用途 | 接入方式 |
|------|------|---------|
| Backend Earn API | 首页数据与奖励结算 | Web H5 通过 RESTful `/api/earn/*` 接口调用 |
| Backend Auth API | Native 权威登录态恢复 | Android/iOS 通过既有 auth session 体系读取当前登录快照 |
| Backend Player API | Native 代表性任务播放承接 | Android/iOS 原生播放器继续使用 `/api/player/start`、`/api/player/stop` |
| WebView / WKWebView | H5 容器承载 | Android WebView 与 iOS WKWebView 分别加载 `/earn` 并注入 JavaScript 消息 |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| H5 `apiAccessToken` 仅保存在内存中 | 页面重建或刷新后需要依赖宿主再次发送 `earn.syncAuthState` | 2026-07-29 | 这是当前实现的显式安全边界，不写入本地持久化 |
| 当前只有代表性任务允许真正完成结算 | 新手任务、锁定任务与连续看剧福利 7 宫格都不会触发真实奖励 API | 2026-07-29 | Backend mock repository 只允许 `is_representative=true` 的任务完成 |
| 浏览器模式无法真正打开 Native 登录或播放器 | 本地浏览器只能验证 H5 UI、overview API 与受控降级提示 | 2026-07-29 | bridge 缺失时统一返回 fallback 提示 |
| Android 与 iOS 对 task-return 的 `preserveScroll` 语义不一致 | Android 固定发送 `false`，iOS 任务返回时发送 `true` | 2026-07-29 | 当前两端都能回到 `/earn`，但滚动恢复细节尚未统一 |
| 真实提现、账本、连续看剧发奖、多任务并发与风控均未实现 | 赚钱中心当前只提供金币展示与单个代表性任务最小闭环 | 2026-07-29 | 超出 PRD-14 首版范围 |
| 设备级黑盒执行仍未自动完成 | 当前跨端结论主要来自代码、自动化测试与 QA 文档 | 2026-07-29 | `docs/specs/2026-07-29-prd-14-earn/qa-test.md:1-601` 已记录黑盒用例与跳过原因 |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 初始创建：收录 PRD-14 赚钱中心，覆盖 H5 首页、Native 容器接入、earn 专属 bridge / host sync、代表性任务播放回流与奖励结算闭环 |

---
*本文档由 llm-wiki skill 自动维护。*
