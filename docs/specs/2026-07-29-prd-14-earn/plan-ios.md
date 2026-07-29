# 实现计划：iOS — PRD-14 赚钱中心

> 创建日期：2026-07-29
> 对应技术方案：design-ios.md
> 对应需求：spec.md

## 概述

本期 iOS 侧聚焦将 `earn` 一级 tab 从占位页切换为独立的赚钱 WKWebView 容器，并围绕既有 `design.md` / `design-ios.md` 打通三类能力：配置化 `/earn` 首页加载、`earn.requestLogin` 与 `earn.openTaskPlayer` bridge 承接，以及登录/播放返回后的 `CustomEvent('earn.hostMessage')` 宿主回传。计划遵循轻量 TDD：先列清单元测试场景，再按小步实现容器、路由与 player result 收口，确保每个场景都能通过 Swift Testing 自动验证。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | earn 配置与路由基线建立后，tab 可指向独立容器且相关 route 归属正确 | `EARN_BASE_URL` 已配置、`AppRoute.earnLogin(context:)`、`AppRoute.earnPlayer(context:)` | `AppConfig.earnHomeURL()` 生成 `/earn` 首页 URL；earn 登录/播放路由 `owningTab == .earn`；`TabNavigationHostView` 不再为 earn 返回 `PlaceholderTabView` | 单元测试 | P0 |
| T-02 | earn 容器首次加载、失败重试与登录态初始同步按宿主状态机流转 | 合法 earn 首页 URL、WebView 成功/失败回调、匿名/登录态快照 | `EarnContainerViewModel` 在 `loading / success / error` 间正确切换；重试重新加载最近首页；页面成功后发出 `earn.syncAuthState` host message | 单元测试 | P0 |
| T-03 | H5 发送 `earn.requestLogin` 后，宿主只通过 earn 专属链路打开登录承接并在返回后恢复上下文 | 合法/非法 `EarnLoginContext(source:'earn', returnTarget:'/earn')`、登录取消/成功回调 | 合法消息触发 `.earnLogin(context:)`；非法消息被忽略；登录返回后 router 重新选中 `.earn`，ViewModel 发出 `earn.syncAuthState` 与 `earn.restoreContext(reason: .loginReturn)` | 单元测试 | P0 |
| T-04 | H5 发送 `earn.openTaskPlayer` 后，宿主携带 `EarnTaskContext` 打开 player 并在返回时按 `EarnTaskPlayerResult` 收口 | 合法/非法 `EarnTaskContext(taskId, source:'earn', returnTarget:'/earn', videoId)`、player 完成/未完成结果 | 合法消息触发 `.earnPlayer(context:)` handoff；router 持有待消费的 `EarnTaskPlayerResult`；完成时先发 `earn.completeTask` 再发 `earn.restoreContext(reason: .taskReturn)`，未完成只恢复上下文 | 单元测试 | P0 |
| T-05 | 重复或异常任务/登录 bridge 不会打断 earn 上下文 | 重复 `earn.requestLogin`、重复 `earn.openTaskPlayer`、缺字段 payload、空 `taskId`/`videoId` | ViewModel 对重复在途流程去重；非法 payload 直接忽略；当前容器状态保持稳定，不误发导航与 host message | 单元测试 | P1 |

## 实现步骤

### Step 1：建立赚钱容器的配置、实体与路由基线

- **关联测试**：T-01
- **目标文件**：`ios/project.yml`、`ios/Configs/Debug.xcconfig`、`ios/Configs/Release.xcconfig`、`ios/ShortDrama/Sources/Core/Config/AppConfig.swift`、`ios/ShortDrama/Sources/App/AppRoute.swift`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`、`ios/ShortDrama/Sources/Domain/Entities/EarnLoginContext.swift`、`ios/ShortDrama/Sources/Domain/Entities/EarnTaskContext.swift`、`ios/ShortDrama/Sources/Domain/Entities/EarnTaskPlayerResult.swift`、`ios/ShortDrama/Tests/DomainTests/AppConfigTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`
- **实现内容**：
  1. 在 `ios/project.yml` 中新增 `INFOPLIST_KEY_EARN_BASE_URL`，并在 `Debug.xcconfig` / `Release.xcconfig` 里补充 `EARN_BASE_URL`，继续通过 XcodeGen + xcconfig 管理环境配置，不直接改 `.xcodeproj/project.pbxproj`。
  2. 扩展 `AppConfig`，新增 `earnBaseURL(bundle:)` 与 `earnHomeURL(bundle:)`，沿用现有 URL 组装方式，确保 `/earn` 首页地址统一来源于配置。
  3. 新增纯 Swift 实体 `EarnLoginContext`、`EarnTaskContext`、`EarnTaskPlayerResult`，字段严格对齐 design：`source / returnTarget` 固定为 earn 语义，player result 统一收口 `taskId / videoId / completed / reason / source`。
  4. 扩展 `AppRoute` 新增 `.earnLogin(context:)`、`.earnPlayer(context:)`，并在 `owningTab` / `publicRouteName` 中明确 earn 专属归属，不复用 mall 命名空间。
  5. 修改 `TabNavigationHostView`，将 `.earn` 根内容从 `PlaceholderTabView` 切换为后续将落地的 `EarnContainerView()`，同时为新 route 预留目标分发位置。
  6. 先补 `AppConfigTests` 与 `NavigationRouterTests`，验证 earn URL 配置读取、route public name / owningTab 与根视图切换，再回填实现。
- **验证方式**：
  - 运行 `cd ios && xcodegen generate && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0' -only-testing:ShortDramaTests/AppConfigTests -only-testing:ShortDramaTests/NavigationRouterTests` 确认 T-01 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/project.yml` | 修改 | 注入 `EARN_BASE_URL` 到 Info.plist |
| `ios/Configs/Debug.xcconfig` | 修改 | 增加开发环境 earn H5 基址 |
| `ios/Configs/Release.xcconfig` | 修改 | 增加生产环境 earn H5 基址 |
| `ios/ShortDrama/Sources/Core/Config/AppConfig.swift` | 修改 | 提供 earn URL 配置读取与首页 URL 组装 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增 earn 登录/播放 handoff route |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | earn tab 根视图切换为 `EarnContainerView()` |
| `ios/ShortDrama/Sources/Domain/Entities/EarnLoginContext.swift` | 新增 | 定义 earn 登录上下文实体 |
| `ios/ShortDrama/Sources/Domain/Entities/EarnTaskContext.swift` | 新增 | 定义 earn 任务播放上下文实体 |
| `ios/ShortDrama/Sources/Domain/Entities/EarnTaskPlayerResult.swift` | 新增 | 定义 player 返回结果收口实体 |
| `ios/ShortDrama/Tests/DomainTests/AppConfigTests.swift` | 修改 | 增加 earn 配置读取断言 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 增加 earn 路由归属与 public name 断言 |

### Step 2：实现赚钱容器状态机、bridge 模型与宿主消息注入

- **关联测试**：T-02、T-03、T-05
- **目标文件**：`ios/ShortDrama/Sources/Features/Earn/Models/EarnContainerState.swift`、`ios/ShortDrama/Sources/Features/Earn/Models/EarnBridgeMessage.swift`、`ios/ShortDrama/Sources/Features/Earn/Models/EarnHostMessage.swift`、`ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift`、`ios/ShortDrama/Sources/Features/Earn/Views/Components/EarnContainerStateView.swift`、`ios/ShortDrama/Sources/Features/Earn/Views/Components/EarnWebView.swift`、`ios/ShortDrama/Sources/Features/Earn/Views/EarnContainerView.swift`、`ios/ShortDrama/Tests/ViewModelTests/EarnContainerViewModelTests.swift`
- **实现内容**：
  1. 先在 `EarnContainerViewModelTests` 中定义首页首次加载、加载失败重试、`earn.requestLogin`/`earn.openTaskPlayer` 合法与非法解析、重复事件去重等场景，保证每个 earn bridge 场景都有单元测试。
  2. 新增 `EarnContainerState`，显式建模 `loading / success / error(message)`，避免把容器错误处理散落在 View 与 WebKit delegate 中。
  3. 新增 `EarnBridgeMessage`，集中解析 `earn.requestLogin` 与 `earn.openTaskPlayer` payload，只接受 design 约束内的 `source:'earn'`、`returnTarget:'/earn'`、非空 `taskId/videoId`，非法输入直接返回 `nil`。
  4. 新增 `EarnHostMessage`，统一封装 `earn.syncAuthState`、`earn.restoreContext`、`earn.completeTask` 三类消息，并且仅通过 `window.dispatchEvent(new CustomEvent('earn.hostMessage', { detail }))` 向 H5 注入，不保留其它 transport。
  5. 新增 `EarnContainerViewModel`，负责首页请求初始化、最近成功首页 URL、登录态快照同步、bridge route effect、重复登录/任务请求去重，以及登录/任务返回后的 host message 编排。
  6. 新增 `EarnWebView` 与 `EarnContainerStateView`，用 `UIViewRepresentable + WKWebView + WKScriptMessageHandler` 承载 H5 页面和宿主态；`EarnContainerView` 负责装配 ViewModel、消费 route effect 与 host message。
- **验证方式**：
  - 运行 `cd ios && xcodegen generate && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0' -only-testing:ShortDramaTests/EarnContainerViewModelTests` 确认 T-02、T-03、T-05 相关单测通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/Earn/Models/EarnContainerState.swift` | 新增 | 赚钱容器宿主态枚举 |
| `ios/ShortDrama/Sources/Features/Earn/Models/EarnBridgeMessage.swift` | 新增 | 解析 earn bridge 消息与 payload 校验 |
| `ios/ShortDrama/Sources/Features/Earn/Models/EarnHostMessage.swift` | 新增 | 封装 `earn.hostMessage` 自定义事件注入 |
| `ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift` | 新增 | 管理容器状态、bridge effect 与 host sync |
| `ios/ShortDrama/Sources/Features/Earn/Views/Components/EarnContainerStateView.swift` | 新增 | loading / error 宿主态组件 |
| `ios/ShortDrama/Sources/Features/Earn/Views/Components/EarnWebView.swift` | 新增 | WKWebView 容器与 earn bridge/host message 注入 |
| `ios/ShortDrama/Sources/Features/Earn/Views/EarnContainerView.swift` | 新增 | earn tab 根视图与 router 装配 |
| `ios/ShortDrama/Tests/ViewModelTests/EarnContainerViewModelTests.swift` | 新增 | 覆盖容器状态机、bridge 校验、去重与 host message |

### Step 3：实现 earn 登录承接与路由恢复闭环

- **关联测试**：T-03、T-05
- **目标文件**：`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Sources/Features/Earn/Views/EarnLoginPlaceholderView.swift`、`ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift`、`ios/ShortDrama/Sources/Features/Earn/Views/EarnContainerView.swift`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/EarnContainerViewModelTests.swift`
- **实现内容**：
  1. 先补测试，覆盖 `presentEarnLogin`、`dismissEarnLogin(completed:)`、登录成功/取消后重新选中 `.earn`，以及重复登录 bridge 不重复打开承接页。
  2. 在 `NavigationRouter` 中新增 earn 专属登录上下文存储与 `pendingEarnRestoreRequest` 管理，不复用 mall 的 `mallLoginContext` / `pendingMallRestoreRequest`，避免两条 H5 容器链路互相污染。
  3. 新增 `EarnLoginPlaceholderView` 作为 earn-owned 登录承接适配层，占位 UI 可复用现有统一登录能力，但 route、关闭与返回语义必须归 earn 所有。
  4. 在 `EarnContainerView` 中通过 `fullScreenCover` 挂载 `EarnLoginPlaceholderView`，并在关闭/完成回调时调用 router 的 earn 登录关闭接口。
  5. 在 `EarnContainerViewModel` 中补齐 `handleLoginSuccess()`、`handleLoginCompletion()` 等宿主回传入口，保证登录返回时先同步权威登录态，再发 `earn.restoreContext(reason: .loginReturn)`。
- **验证方式**：
  - 运行 `cd ios && xcodegen generate && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0' -only-testing:ShortDramaTests/NavigationRouterTests -only-testing:ShortDramaTests/EarnContainerViewModelTests` 确认 T-03、T-05 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 增加 earn 登录承接、恢复请求与 tab 恢复逻辑 |
| `ios/ShortDrama/Sources/Features/Earn/Views/EarnLoginPlaceholderView.swift` | 新增 | earn 专属全屏登录承接页 |
| `ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift` | 修改 | 处理登录返回后的 auth sync 与 restoreContext |
| `ios/ShortDrama/Sources/Features/Earn/Views/EarnContainerView.swift` | 修改 | 挂载 earn 登录承接展示入口 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 增加 earn 登录路由与恢复断言 |
| `ios/ShortDrama/Tests/ViewModelTests/EarnContainerViewModelTests.swift` | 修改 | 增加登录桥接与去重断言 |

### Step 4：实现任务播放 handoff 与 `EarnTaskPlayerResult` 收口

- **关联测试**：T-04、T-05
- **目标文件**：`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Sources/App/AppRoute.swift`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`、`ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift`、`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/EarnContainerViewModelTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift`
- **实现内容**：
  1. 先补测试：验证合法 `EarnTaskContext` 能触发 `.earnPlayer(context:)`，router 能存取待消费的 `EarnTaskPlayerResult`，以及完成/未完成两种 player 返回路径的 host message 顺序。
  2. 在 `NavigationRouter` 中新增 `openPlayerFromEarn(_:)`、`finishEarnTaskPlayer(result:)`、`consumeEarnTaskPlayerResult()` 等接口，由 earn handoff route 持有 `taskId/source/returnTarget/videoId`，再落到现有 `.player(videoId:)`。
  3. 在 `TabNavigationHostView` 的 player 装配处注入 earn 上下文消费点，确保从 earn 发起时仍复用现有 `PlayerViewModel`，但不假设播放器已有 earn 任务 contract。
  4. 扩展 `PlayerViewModel`，在 `handleBack()`、`handleDisappear()`、后台切换与代表性播放完成回调中统一产出 `EarnTaskPlayerResult`；只有明确的“代表性任务视频正常播放结束”才标记 `completed=true`，其他退出路径全部回传 `completed=false`。
  5. 在 `EarnContainerViewModel` 中新增任务返回处理：消费 router 回传的 `EarnTaskPlayerResult`，按 design 要求在 `completed=true` 时先发 `earn.completeTask`，再发 `earn.restoreContext(reason: .taskReturn)`；未完成仅恢复上下文。
  6. 对重复 `earn.openTaskPlayer`、缺字段 payload、空 `taskId` / `videoId` 持续保持忽略策略，保证播放 handoff 不会把容器推入异常状态。
- **验证方式**：
  - 运行 `cd ios && xcodegen generate && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0' -only-testing:ShortDramaTests/NavigationRouterTests -only-testing:ShortDramaTests/EarnContainerViewModelTests -only-testing:ShortDramaTests/PlayerViewModelTests` 确认 T-04、T-05 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 增加 earn 播放 handoff、player result 存取与恢复逻辑 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 明确 `.earnPlayer(context:)` 路由语义 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 在 player 装配点接入 earn 上下文消费 |
| `ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift` | 修改 | 消费 `EarnTaskPlayerResult` 并发出 host message |
| `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | 修改 | 在退出/完成路径统一生成 `EarnTaskPlayerResult` |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 增加 earn 播放 handoff 与 result 收口断言 |
| `ios/ShortDrama/Tests/ViewModelTests/EarnContainerViewModelTests.swift` | 修改 | 增加 task return、completeTask 与 restore 顺序断言 |
| `ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift` | 修改 | 增加 earn 场景下的 player result 产生断言 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4
```

## 验证总览

- [ ] 所有测试通过（`cd ios && xcodegen generate && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [ ] Build 成功（`cd ios && xcodegen generate && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [ ] 无新增 lint 错误（`cd ios && swiftlint lint`）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/project.yml` | 修改 | 为 earn H5 地址增加 Info.plist 注入配置 |
| `ios/Configs/Debug.xcconfig` | 修改 | 配置开发环境 earn H5 基址 |
| `ios/Configs/Release.xcconfig` | 修改 | 配置生产环境 earn H5 基址 |
| `ios/ShortDrama/Sources/Core/Config/AppConfig.swift` | 修改 | 提供 earn 首页 URL 读取能力 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增 earn 登录与播放 handoff 路由 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 管理 earn 登录、播放返回与恢复请求 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | earn tab 接入 `EarnContainerView` 与 player handoff 消费 |
| `ios/ShortDrama/Sources/Domain/Entities/EarnLoginContext.swift` | 新增 | 定义 earn 登录上下文 |
| `ios/ShortDrama/Sources/Domain/Entities/EarnTaskContext.swift` | 新增 | 定义 earn 任务播放上下文 |
| `ios/ShortDrama/Sources/Domain/Entities/EarnTaskPlayerResult.swift` | 新增 | 定义 player 结果收口模型 |
| `ios/ShortDrama/Sources/Features/Earn/Models/EarnContainerState.swift` | 新增 | 赚钱容器宿主状态模型 |
| `ios/ShortDrama/Sources/Features/Earn/Models/EarnBridgeMessage.swift` | 新增 | earn bridge 消息解析模型 |
| `ios/ShortDrama/Sources/Features/Earn/Models/EarnHostMessage.swift` | 新增 | `earn.hostMessage` 宿主消息模型 |
| `ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift` | 新增 | 赚钱容器状态机与 bridge/host sync 调度 |
| `ios/ShortDrama/Sources/Features/Earn/Views/EarnContainerView.swift` | 新增 | earn tab 根视图 |
| `ios/ShortDrama/Sources/Features/Earn/Views/EarnLoginPlaceholderView.swift` | 新增 | earn 专属登录承接页 |
| `ios/ShortDrama/Sources/Features/Earn/Views/Components/EarnContainerStateView.swift` | 新增 | loading/error 宿主态组件 |
| `ios/ShortDrama/Sources/Features/Earn/Views/Components/EarnWebView.swift` | 新增 | WKWebView 容器与 host message 注入 |
| `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | 修改 | 统一产出 `EarnTaskPlayerResult` |
| `ios/ShortDrama/Tests/DomainTests/AppConfigTests.swift` | 修改 | 覆盖 earn 配置读取 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 覆盖 earn 登录/播放 handoff 与恢复逻辑 |
| `ios/ShortDrama/Tests/ViewModelTests/EarnContainerViewModelTests.swift` | 新增 | 覆盖 earn 容器状态机、bridge、host sync、去重 |
| `ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift` | 修改 | 覆盖 earn player result 结果收口 |
