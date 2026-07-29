# 实现计划：Android — PRD-14 赚钱中心

> 创建日期：2026-07-29
> 对应技术方案：design-android.md
> 对应需求：spec.md

## 概述

Android 端本期目标是将 `earn` 一级频道从 `PlaceholderScreen` 替换为真实赚钱容器页，使用 Kotlin + Compose + Hilt + Navigation Compose 承接 `EarnScreen`、`EarnViewModel` 与 WebView 容器，首页地址统一通过 `AppConfig` 读取，禁止在功能代码中直接使用 `BuildConfig`。实现严格遵循 shared / Android design 中的 earn 专属 contract：H5 容器只监听和注入 `CustomEvent('earn.hostMessage')`，播放结果统一按 `EarnTaskPlayerResult` 收口，测试先行覆盖配置、容器状态、bridge、登录返回、播放返回与导航接线。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> Android 要求：每个场景都需要单元测试；新增业务逻辑同步补齐测试。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 赚钱容器初始状态使用配置化首页地址 | fake `AppConfig.earnBaseUrl=https://earn.example.com`，初始化 `EarnViewModel` | `currentUrl=https://earn.example.com/earn`，`state=Loading`，无直接 `BuildConfig` 依赖 | 单元测试 | P0 |
| T-02 | 页面加载成功、失败与重试正确驱动容器状态 | 依次触发 WebView load success、load failed、retry | 状态在 `Loading / Success / Error` 间正确切换，重试后重新加载 `/earn` 首页 | 单元测试 | P0 |
| T-03 | `earn.requestLogin` 仅在合法 payload 下打开登录承接并拦截重复请求 | 合法 `EarnLoginContext(source=earn, returnTarget=/earn)`、非法 source / returnTarget、重复点击 | 合法请求发出 `OpenEarnLogin` effect 并记录 `pendingLoginContext`；非法与重复请求被忽略 | 单元测试 | P0 |
| T-04 | `earn.openTaskPlayer` 仅在合法任务上下文下打开播放承接 | 合法 `EarnTaskContext(taskId, source=earn, returnTarget=/earn, videoId)`、缺失 `taskId` / `videoId`、重复点击 | 合法请求发出 `OpenEarnTaskPlayer` effect 并记录 `pendingTaskContext`；非法与重复请求被忽略 | 单元测试 | P0 |
| T-05 | 登录返回会同步权威登录态并通过 `earn.hostMessage` 恢复赚钱上下文 | 登录成功 / 取消 / 关闭后的回调 + `AuthSessionProvider` 当前会话 | 先发 `earn.syncAuthState`，再发 `earn.restoreContext(reason=login-return)`；清空待登录上下文并保持 `/earn` | 单元测试 | P0 |
| T-06 | 播放返回按 `EarnTaskPlayerResult` 收口并只在完成时触发任务完成消息 | `EarnTaskPlayerResult(completed=true/false, reason=...)` | `completed=true` 时先发 `earn.completeTask` 再发 `earn.restoreContext(reason=task-return)`；`completed=false` 时只恢复上下文，不发完成消息 | 单元测试 | P0 |
| T-07 | earn route、login route、play handoff route 接线稳定且不再回退占位页 | 调用 `AppDestination.earnLogin(...)`、`AppDestination.earnPlay(...)`，检查 `NavGraph` earn graph | `earn` graph 接入 `EarnScreen`，新增 earn login / play handoff 路由，根页不再使用赚钱 placeholder | 单元测试 | P1 |

## 实现步骤

### Step 1：补齐 earn 配置与 ViewModel 初始合同

- **关联测试**：T-01
- **目标文件**：`android/app/build.gradle.kts`、`android/app/src/main/java/com/djs66256/short_drama/core/config/AppConfig.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/config/BuildConfigAppConfig.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnLoginContext.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnTaskContext.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnTaskPlayerResult.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModelTest.kt`
- **实现内容**：
  1. 先在 `EarnViewModelTest.kt` 增加 T-01，用 fake `AppConfig` 验证 `EarnViewModel` 初始化时会把 `earnBaseUrl` 拼成 `/earn` 首页地址，并以 `Loading` 作为初始宿主态。
  2. 在 `android/app/build.gradle.kts` 复用现有 gradle / `local.properties` 配置读取方式补充 `earn.base.url` 的 `BuildConfig` 字段，但业务代码只经由 `AppConfig` 暴露，不允许在 `feature/earn` 或导航层直接读取 `BuildConfig`。
  3. 扩展 `AppConfig.kt` 与 `BuildConfigAppConfig.kt`，新增 `earnBaseUrl` 配置入口，为 Hilt 注入的 ViewModel 提供可测试配置源。
  4. 新增 `EarnLoginContext`、`EarnTaskContext`、`EarnTaskPlayerResult` 最小模型，固定 `source=earn`、`returnTarget=/earn`，为后续 bridge 与播放回传共用统一 contract。
  5. 新增 `EarnViewModel.kt` 最小骨架，先落 `EarnContainerState`、`EarnUiState`、首页 URL 初始化逻辑，为后续容器状态和 effect 扩展提供承载。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.earn.viewmodel.EarnViewModelTest"` 确认 T-01 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/build.gradle.kts` | 修改 | 注入赚钱 H5 地址配置 |
| `android/app/src/main/java/com/djs66256/short_drama/core/config/AppConfig.kt` | 修改 | 扩展 `earnBaseUrl` 配置接口 |
| `android/app/src/main/java/com/djs66256/short_drama/core/config/BuildConfigAppConfig.kt` | 修改 | 通过 `AppConfig` 暴露赚钱地址 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnLoginContext.kt` | 新增 | 定义赚钱登录上下文模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnTaskContext.kt` | 新增 | 定义赚钱任务播放上下文模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnTaskPlayerResult.kt` | 新增 | 定义播放结果统一模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt` | 新增 | 定义赚钱容器初始状态与首页 URL |
| `android/app/src/test/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModelTest.kt` | 新增 | 新增赚钱 ViewModel 初始态测试 |

### Step 2：落实 WebView 容器三态与 `earn.hostMessage` 注入机制

- **关联测试**：T-02
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnHostMessage.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnPageEvent.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnWebViewContainer.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModelTest.kt`
- **实现内容**：
  1. 先在 `EarnViewModelTest.kt` 补 T-02，覆盖 WebView 首次加载成功、加载失败、点击重试三条状态流转路径。
  2. 在 `EarnViewModel.kt` 中补齐页面事件处理入口，例如 `onPageLoadStarted`、`onPageLoadSucceeded`、`onPageLoadFailed`、`retryLoadHome`，并维护 `lastLoadedHomeUrl` 与 `currentUrl`。
  3. 新增 `EarnHostMessage` 与 `EarnPageEvent` 模型，约束 Native 向 H5 的宿主消息只走 `earn.syncAuthState`、`earn.restoreContext`、`earn.completeTask` 三类消息。
  4. 新增 `EarnScreen.kt`，用 Compose 渲染 loading / error / success 三态；加载失败时只展示赚钱宿主错误态，不回退 `PlaceholderScreen`。
  5. 新增 `EarnWebViewContainer.kt`，通过 `AndroidView(WebView)` 承载页面、注册 JS bridge、监听加载结果，并统一使用 `WebView.evaluateJavascript(...)` 注入 `window.dispatchEvent(new CustomEvent('earn.hostMessage', { detail }))`，不为 earn 保留 `window.message` fallback。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.earn.viewmodel.EarnViewModelTest"` 确认 T-02 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt` | 修改 | 增加加载成功/失败/重试状态机 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnHostMessage.kt` | 新增 | 定义 earn host message 模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnPageEvent.kt` | 新增 | 定义 WebView 页面事件模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnScreen.kt` | 新增 | 渲染赚钱宿主三态 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnWebViewContainer.kt` | 新增 | 承接 WebView 与 `earn.hostMessage` 注入 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModelTest.kt` | 修改 | 增加容器状态流转测试 |

### Step 3：完成 earn bridge 解析、登录承接与登录态同步

- **关联测试**：T-03、T-05
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnBridgeMessage.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnHostAuthState.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnRestoreContext.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnLoginResult.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnWebViewContainer.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnLoginScreen.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModelTest.kt`
- **实现内容**：
  1. 先在 `EarnViewModelTest.kt` 增加 T-03、T-05，覆盖合法/非法 `earn.requestLogin` payload、重复点击、登录成功/取消/关闭返回后的 effect 顺序与上下文清理。
  2. 在 `EarnWebViewContainer.kt` 中解析 H5 发来的 earn bridge 消息，只接受 `earn.requestLogin` 与 design 中定义的合法 payload；非法消息仅记录日志并忽略。
  3. 在 `EarnViewModel.kt` 中维护 `pendingLoginContext`，收到合法登录请求时发出 `OpenEarnLogin` effect；若已有待处理登录流程则直接拦截，避免并发重复承接。
  4. 利用现有 `AuthSessionProvider` 读取权威登录态与 access token 快照，生成 `EarnHostAuthState`，并在首次加载、登录成功、登录取消、App resume 等场景统一发出 `earn.syncAuthState`。
  5. 新增 `EarnLoginScreen.kt` 作为 earn 专属登录承接页，复用现有 Native 登录能力的返回语义；登录返回后由 `EarnViewModel` 按固定顺序发送 `earn.syncAuthState` 与 `earn.restoreContext(reason=login-return)`，保证仍停留在 `/earn`。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.earn.viewmodel.EarnViewModelTest"` 确认 T-03、T-05 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt` | 修改 | 增加登录 bridge、登录态同步、上下文恢复逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnBridgeMessage.kt` | 新增 | 定义 earn bridge 消息模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnHostAuthState.kt` | 新增 | 定义登录态同步 payload |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnRestoreContext.kt` | 新增 | 定义返回赚钱上下文 payload |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnLoginResult.kt` | 新增 | 定义登录承接返回结果 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnScreen.kt` | 修改 | 串联登录承接与宿主消息派发 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnWebViewContainer.kt` | 修改 | 增加 earn bridge 解析与登录相关注入 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnLoginScreen.kt` | 新增 | 赚钱专属登录承接页 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModelTest.kt` | 修改 | 增加登录 bridge 与登录返回测试 |

### Step 4：打通任务播放承接与 `EarnTaskPlayerResult` 回传闭环

- **关联测试**：T-04、T-06
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnTaskContext.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnTaskPlayerResult.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnWebViewContainer.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModelTest.kt`
- **实现内容**：
  1. 先在 `EarnViewModelTest.kt` 增加 T-04、T-06，覆盖合法/非法 `earn.openTaskPlayer` payload、重复点击、播放完成与未完成返回两类路径。
  2. 在 `EarnViewModel.kt` 中维护 `pendingTaskContext`，收到合法任务上下文时发出 `OpenEarnTaskPlayer` effect；非法 `taskId` / `videoId` 或重复请求直接忽略。
  3. 约定所有播放返回都先收口到 `EarnTaskPlayerResult`；只有 `completed=true` 才生成 `earn.completeTask` host message，随后统一发送 `earn.restoreContext(reason=task-return)`。
  4. 在 `EarnScreen.kt` / `EarnWebViewContainer.kt` 串联播放回传消息派发，确保 H5 能依赖同一 `earn.hostMessage` 通道消费结果。
  5. 在导航层为后续 handoff route 预留回传所需参数读取与恢复入口，但不修改现有 player `progress / stop` contract，也不让 H5 直接判定任务完成。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.earn.viewmodel.EarnViewModelTest"` 确认 T-04、T-06 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt` | 修改 | 增加任务播放承接、结果回传与恢复逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnTaskContext.kt` | 修改 | 补齐任务上下文校验语义 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnTaskPlayerResult.kt` | 修改 | 收口 completed / reason 结果模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnScreen.kt` | 修改 | 串联播放承接 effect 与回传恢复 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnWebViewContainer.kt` | 修改 | 支持任务 bridge 与 host message 分发 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 为 earn play handoff route 提供 helper 与参数常量 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 接入 task handoff 与返回恢复 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModelTest.kt` | 修改 | 增加任务 bridge 与播放返回测试 |

### Step 5：接入 earn NavGraph、专属路由与最终页面装配

- **关联测试**：T-07
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnLoginScreen.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt`
- **实现内容**：
  1. 先在 `RoutesTest.kt` 与 `NavGraphTest.kt` 中新增 T-07，锁定 `earn`、`earn/login`、`earn/play` 的 route helper 与 earn graph 接线结果，防止回退到 `PlaceholderScreen`。
  2. 在 `AppDestination.kt` 新增 `Route.EARN_LOGIN`、`Route.EARN_PLAY` 与 `earnLogin(returnTarget)`、`earnPlay(taskId, videoId, returnTarget)` helper，参数编码方式与现有 route helper 保持一致。
  3. 在 `NavGraph.kt` 将 `Graph.EARN` 的根页面从赚钱 placeholder 替换为 `EarnScreen`，并新增 `earn/login` 与 `earn/play` composable 作为赚钱专属承接路由。
  4. 在 `EarnLoginScreen.kt` 中承接 close / cancel / success 三类登录返回动作；在 `NavGraph.kt` 中串联这些回调，确保底部赚钱 tab 保持高亮并回到 earn 语义。
  5. 收尾 `EarnScreen` 与导航层的装配，确保完整路径为 `EarnScreen -> EarnWebViewContainer -> EarnLoginScreen / earn play handoff -> 返回 EarnScreen`，且不新增未经批准的依赖。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.navigation.RoutesTest" --tests "com.djs66256.short_drama.navigation.NavGraphTest"` 确认 T-07 通过
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.earn.viewmodel.EarnViewModelTest"` 做回归验证
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 新增赚钱登录与播放承接路由 helper |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | earn graph 接入真实赚钱页面与专属路由 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnScreen.kt` | 修改 | 串联导航、ViewModel 与 WebView 容器 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnLoginScreen.kt` | 修改 | 收尾登录承接交互与返回语义 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 校验赚钱路由 helper |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt` | 修改 | 校验 earn graph 不再使用 placeholder |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5
```

## 验证总览

- [ ] 所有测试通过（`cd android && ./gradlew test`）
- [ ] Build 成功（`cd android && ./gradlew assembleDebug`）
- [ ] 无新增 lint 错误（`cd android && ./gradlew detekt`）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/build.gradle.kts` | 修改 | 注入赚钱 H5 地址配置 |
| `android/app/src/main/java/com/djs66256/short_drama/core/config/AppConfig.kt` | 修改 | 新增 `earnBaseUrl` |
| `android/app/src/main/java/com/djs66256/short_drama/core/config/BuildConfigAppConfig.kt` | 修改 | 通过 `AppConfig` 暴露赚钱地址 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 新增赚钱登录与播放承接路由 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | earn graph 接入真实赚钱页面 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnLoginContext.kt` | 新增 | 赚钱登录上下文模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnTaskContext.kt` | 新增 | 赚钱任务上下文模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnTaskPlayerResult.kt` | 新增 | 播放结果统一模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnBridgeMessage.kt` | 新增 | H5 到 Native 的 earn bridge 模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnHostMessage.kt` | 新增 | Native 到 H5 的 earn host message 模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnHostAuthState.kt` | 新增 | 登录态同步 payload |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnRestoreContext.kt` | 新增 | 上下文恢复 payload |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnLoginResult.kt` | 新增 | 登录承接结果模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnPageEvent.kt` | 新增 | WebView 页面事件模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt` | 新增 | 赚钱容器状态、bridge 与恢复逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnScreen.kt` | 新增 | 赚钱根 Composable 与状态承接 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnWebViewContainer.kt` | 新增 | WebView 容器与 `earn.hostMessage` 接线 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnLoginScreen.kt` | 新增 | 赚钱专属登录承接页 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModelTest.kt` | 新增 | 赚钱业务逻辑单元测试 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 增补赚钱路由测试 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt` | 修改 | 增补 earn graph 接线测试 |