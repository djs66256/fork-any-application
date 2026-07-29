# 实现计划：iOS — PRD-13 商城

> 创建日期：2026-07-28
> 对应技术方案：design-ios.md
> 对应需求：spec.md

## 概述

本期 iOS 侧聚焦把 `mall` 一级 tab 从占位页替换为配置化的商城 WKWebView 容器，并补齐搜索桥接、登录承接、返回恢复与容器错误态。计划按轻量 TDD 推进：先落单元测试覆盖各用户场景，再分步接入容器、路由与 bridge，确保每个场景都有自动化验证。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 商城 tab 首次进入时使用配置化 mall 首页地址构造容器请求，并将 mall 登录路由归属到 mall tab | `MALL_BASE_URL` 已配置、`/mall` 首页路径、`AppRoute.mallLogin(context:)` | `MallContainerViewModel` 生成合法首页 `URLRequest`；`AppRoute.mallLogin` 的 `owningTab == .mall`；mall 不再依赖 `PlaceholderTabView` 作为根内容 | 单元测试 | P0 |
| T-02 | 商城容器首屏加载成功、失败、重试三态正确流转 | 首页地址有效 / WebView 导航成功或失败回调 | 初始为 `loading`；成功后进入 `success`；失败后进入可重试 `error`；点击重试重新加载最近首页 URL | 单元测试 | P0 |
| T-03 | H5 发送 `mall.openSearch` 后打开现有搜索页，并在返回时恢复商城上下文 | 合法 `MallSearchContext(source:'mall', returnTarget:'/mall')` | Router 打开现有 `.searchHome`；搜索关闭后重新选中 `.mall`，并触发 `mall.restoreContext(reason: .searchReturn)` | 单元测试 | P0 |
| T-04 | H5 发送 `mall.requestLogin` 后打开商城专属全屏登录承接，并在返回时同步登录态和恢复商城 | 合法 `MallLoginContext(source:'mall', productId, returnTarget:'/mall')` | Router 打开 `.mallLogin(context:)`；关闭/取消/成功后回到 `.mall`，并触发 `mall.syncAuthState` 与 `mall.restoreContext(reason: .loginReturn)` | 单元测试 | P0 |
| T-05 | 非法或重复 bridge 消息不会打断商城上下文 | 非法 `productId`、缺失字段 payload、重复 `mall.requestLogin` | ViewModel 忽略非法消息、对重复登录请求去重、保持当前 mall 容器状态不崩溃 | 单元测试 | P1 |

## 实现步骤

### Step 1：建立商城容器的路由与配置基线

- **关联测试**：T-01
- **目标文件**：`ios/project.yml`、`ios/Configs/Debug.xcconfig`、`ios/Configs/Release.xcconfig`、`ios/ShortDrama/Sources/Core/Config/AppConfig.swift`、`ios/ShortDrama/Sources/App/AppRoute.swift`、`ios/ShortDrama/Sources/Domain/Entities/MallLoginContext.swift`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`
- **实现内容**：
  1. 在 `ios/project.yml` 中新增 `INFOPLIST_KEY_MALL_BASE_URL` 注入项，并保持 XcodeGen 作为唯一配置来源。
  2. 在 `ios/Configs/Debug.xcconfig` 与 `ios/Configs/Release.xcconfig` 中补充 `MALL_BASE_URL`，沿用环境注入方式，避免在 Swift 代码里硬编码商城地址。
  3. 扩展 `ios/ShortDrama/Sources/Core/Config/AppConfig.swift`，新增 `mallBaseURL(bundle:)` 与首页 URL 组装辅助方法，供 Mall 容器统一读取。
  4. 扩展 `ios/ShortDrama/Sources/App/AppRoute.swift`，新增 `.mallLogin(context:)`，明确其 `owningTab == .mall` 与对外 route naming。
  5. 在 `ios/ShortDrama/Sources/Domain/Entities/MallLoginContext.swift` 中沉淀纯 Swift 登录上下文实体，字段与 `design.md`/`design-ios.md` 对齐：`source`、`productID`、`returnTarget`。
  6. 先在 `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` 中补充路由归属与公开语义断言，再实现对应路由与配置入口。
- **验证方式**：
  - 运行 `cd ios && xcodegen generate && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0' -only-testing:ShortDramaTests/NavigationRouterTests` 确认 T-01 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/project.yml` | 修改 | 注入 `MALL_BASE_URL` 对应的 Info.plist key |
| `ios/Configs/Debug.xcconfig` | 修改 | 增加开发环境商城 H5 基址 |
| `ios/Configs/Release.xcconfig` | 修改 | 增加生产环境商城 H5 基址 |
| `ios/ShortDrama/Sources/Core/Config/AppConfig.swift` | 修改 | 增加商城 URL 配置读取与首页 URL 组装 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增 mall 登录承接路由与 tab 归属 |
| `ios/ShortDrama/Sources/Domain/Entities/MallLoginContext.swift` | 新增 | 定义商城登录上下文实体 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 增加 mall 路由归属与命名断言 |

### Step 2：实现商城容器状态机与宿主态视图

- **关联测试**：T-02
- **目标文件**：`ios/ShortDrama/Sources/Features/Mall/Models/MallContainerState.swift`、`ios/ShortDrama/Sources/Features/Mall/ViewModels/MallContainerViewModel.swift`、`ios/ShortDrama/Sources/Features/Mall/Views/MallContainerView.swift`、`ios/ShortDrama/Sources/Features/Mall/Views/Components/MallContainerStateView.swift`、`ios/ShortDrama/Tests/ViewModelTests/MallContainerViewModelTests.swift`
- **实现内容**：
  1. 先在 `ios/ShortDrama/Tests/ViewModelTests/MallContainerViewModelTests.swift` 中定义首页首次加载、导航成功、导航失败、手动重试四类状态流转测试。
  2. 新增 `MallContainerState`，显式建模 `loading / success / error(retryable)`，避免把宿主态散落在 View 中。
  3. 新增 `MallContainerViewModel`，负责首页请求初始化、最近成功首页 URL 记录、失败重载与页面事件处理。
  4. 新增 `MallContainerStateView`，统一承载 loading / error UI，保证宿主错误态与重试入口可复用。
  5. 新增 `MallContainerView`，以 `@StateObject` 管理 ViewModel，并预留 `MallWebView` 挂载位置，先让容器三态完整可测。
- **验证方式**：
  - 运行 `cd ios && xcodegen generate && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0' -only-testing:ShortDramaTests/MallContainerViewModelTests` 确认 T-02 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/Mall/Models/MallContainerState.swift` | 新增 | 商城容器宿主态枚举 |
| `ios/ShortDrama/Sources/Features/Mall/ViewModels/MallContainerViewModel.swift` | 新增 | 管理首页加载、失败重试与最近 URL |
| `ios/ShortDrama/Sources/Features/Mall/Views/MallContainerView.swift` | 新增 | 商城 tab 根视图，装配 ViewModel |
| `ios/ShortDrama/Sources/Features/Mall/Views/Components/MallContainerStateView.swift` | 新增 | loading / error 宿主态组件 |
| `ios/ShortDrama/Tests/ViewModelTests/MallContainerViewModelTests.swift` | 新增 | 覆盖容器状态机核心场景 |

### Step 3：接入 WKWebView 与商城 tab 根视图切换

- **关联测试**：T-01、T-02
- **目标文件**：`ios/ShortDrama/Sources/Features/Mall/Views/Components/MallWebView.swift`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`、`ios/ShortDrama/Sources/Features/Mall/ViewModels/MallContainerViewModel.swift`、`ios/ShortDrama/Tests/ViewModelTests/MallContainerViewModelTests.swift`
- **实现内容**：
  1. 新增 `MallWebView.swift`，使用 `UIViewRepresentable + WKWebView + WKNavigationDelegate + WKScriptMessageHandler` 封装商城容器页面事件。
  2. 在 `MallContainerViewModel` 中补齐 `loadInitialPage()`、`reload()`、`handlePageLoaded()`、`handlePageLoadFailed()` 等页面生命周期入口，并把 `URLRequest` 输出给 `MallWebView`。
  3. 修改 `ios/ShortDrama/Sources/App/TabNavigationHostView.swift`，将 `.mall` 根内容从 `PlaceholderTabView(tab: .mall)` 替换为 `MallContainerView()`，同时保持 `earn`、`profile` 仍走现有占位页。
  4. 在测试中补齐“首次进入 mall tab 时生成首页请求且成功回调后显示 success”的断言，避免后续回归到占位页。
  5. 运行 XcodeGen 让新增 Mall 文件纳入工程，确保目录结构与 `ios/CLAUDE.md` 保持一致。
- **验证方式**：
  - 运行 `cd ios && xcodegen generate && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0' -only-testing:ShortDramaTests/MallContainerViewModelTests -only-testing:ShortDramaTests/NavigationRouterTests` 确认 T-01、T-02 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/Mall/Views/Components/MallWebView.swift` | 新增 | 封装 WKWebView 与页面事件回调 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 将 mall tab 根视图切为 `MallContainerView()` |
| `ios/ShortDrama/Sources/Features/Mall/ViewModels/MallContainerViewModel.swift` | 修改 | 接入 `URLRequest`、页面成功/失败事件处理 |
| `ios/ShortDrama/Tests/ViewModelTests/MallContainerViewModelTests.swift` | 修改 | 增加首页请求生成与页面回调断言 |

### Step 4：实现搜索 bridge 与商城上下文恢复

- **关联测试**：T-03
- **目标文件**：`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Sources/Features/Mall/Models/MallBridgeMessage.swift`、`ios/ShortDrama/Sources/Features/Mall/ViewModels/MallContainerViewModel.swift`、`ios/ShortDrama/Sources/Features/Mall/Views/Components/MallWebView.swift`、`ios/ShortDrama/Tests/ViewModelTests/MallContainerViewModelTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`
- **实现内容**：
  1. 先补测试：定义合法 `mall.openSearch` payload 触发 router 打开 `.searchHome`，以及搜索关闭后恢复 `.mall` tab 的单元测试。
  2. 新增 `MallBridgeMessage.swift`，集中定义并解析 `mall.openSearch` 的 payload 结构，避免在 WebKit 回调里直接拼字典。
  3. 在 `MallWebView` 中注册商城 bridge channel，把 script message 转给 `MallContainerViewModel`。
  4. 在 `MallContainerViewModel` 中处理 `mall.openSearch`，将导航 effect 交给 `NavigationRouter`，不在 View 层直接操作路由。
  5. 扩展 `NavigationRouter`：记录 search 是从 mall 发起、维护 `returnTarget=/mall`，搜索返回时执行 `restoreMallContext(reason: .searchReturn)`，确保 mall tab 重新高亮。
  6. 在 `MallContainerViewModel` 中预留向 WebView 回送 `mall.restoreContext` 的宿主事件接口，首版先做到最小可恢复闭环。
- **验证方式**：
  - 运行 `cd ios && xcodegen generate && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0' -only-testing:ShortDramaTests/MallContainerViewModelTests -only-testing:ShortDramaTests/NavigationRouterTests` 确认 T-03 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 增加 mall 发起搜索、返回恢复与 tab 选中逻辑 |
| `ios/ShortDrama/Sources/Features/Mall/Models/MallBridgeMessage.swift` | 新增 | 定义/解析 `mall.openSearch` bridge 消息 |
| `ios/ShortDrama/Sources/Features/Mall/ViewModels/MallContainerViewModel.swift` | 修改 | 处理搜索 bridge effect 与恢复回调 |
| `ios/ShortDrama/Sources/Features/Mall/Views/Components/MallWebView.swift` | 修改 | 注册 script message handler 并转发搜索事件 |
| `ios/ShortDrama/Tests/ViewModelTests/MallContainerViewModelTests.swift` | 修改 | 增加搜索 bridge 触发与恢复断言 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 增加 mall 搜索返回恢复断言 |

### Step 5：实现登录承接、登录态同步与异常桥接防御

- **关联测试**：T-04、T-05
- **目标文件**：`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Sources/Features/Mall/ViewModels/MallContainerViewModel.swift`、`ios/ShortDrama/Sources/Features/Mall/Views/MallLoginPlaceholderView.swift`、`ios/ShortDrama/Sources/Features/Mall/Views/Components/MallWebView.swift`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`、`ios/ShortDrama/Tests/ViewModelTests/MallContainerViewModelTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`
- **实现内容**：
  1. 先补测试：覆盖合法 `mall.requestLogin` 打开 `.mallLogin(context:)`、登录返回恢复 mall、非法 payload 忽略、重复登录消息去重等场景。
  2. 新增 `MallLoginPlaceholderView.swift` 作为商城专属全屏登录承接页，占位语义与 `menu/login` 解耦。
  3. 在 `MallContainerViewModel` 中处理 `mall.requestLogin`：校验 `productID`、保存 `pendingLoginContext`、对重复请求加防抖/去重。
  4. 在 `NavigationRouter` 中补齐 `.mallLogin(context:)` 的展示与关闭恢复逻辑，确保取消、失败、成功三种返回都重新选中 `.mall`。
  5. 在 `MallWebView`/ViewModel 中补齐向 H5 回传 `mall.syncAuthState` 与 `mall.restoreContext(reason: .loginReturn)` 的宿主事件发送接口；若上下文恢复失败，降级为重新加载 mall 首页。
  6. 在 `TabNavigationHostView` 或 `MallContainerView` 中接入 mall 登录承接展示方式（`fullScreenCover` 或等效路由挂载点），让登录流程完整闭环。
  7. 回写测试，确保每个新增业务逻辑都由 `MallContainerViewModelTests` 或 `NavigationRouterTests` 覆盖。
- **验证方式**：
  - 运行 `cd ios && xcodegen generate && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0' -only-testing:ShortDramaTests/MallContainerViewModelTests -only-testing:ShortDramaTests/NavigationRouterTests` 确认 T-04、T-05 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 增加 mall 登录承接展示、返回恢复与 auth sync 触发 |
| `ios/ShortDrama/Sources/Features/Mall/ViewModels/MallContainerViewModel.swift` | 修改 | 处理登录 bridge、去重、防御非法 payload |
| `ios/ShortDrama/Sources/Features/Mall/Views/MallLoginPlaceholderView.swift` | 新增 | 商城专属全屏登录承接页 |
| `ios/ShortDrama/Sources/Features/Mall/Views/Components/MallWebView.swift` | 修改 | 转发登录 bridge、预留宿主消息发送接口 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 挂载 mall 登录承接展示入口（如需） |
| `ios/ShortDrama/Tests/ViewModelTests/MallContainerViewModelTests.swift` | 修改 | 增加登录 bridge、非法 payload、重复点击断言 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 增加 mall 登录路由与返回恢复断言 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5
```

## 验证总览

- [x] 所有测试通过（`cd ios && xcodegen generate && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [x] Build 成功（`cd ios && xcodegen generate && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [x] 无新增 lint 错误（`cd ios && swiftlint lint`；仅存在仓库既有 warning，0 serious）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/project.yml` | 修改 | 为商城 H5 地址增加 Info.plist 注入配置 |
| `ios/Configs/Debug.xcconfig` | 修改 | 配置开发环境商城 H5 基址 |
| `ios/Configs/Release.xcconfig` | 修改 | 配置生产环境商城 H5 基址 |
| `ios/ShortDrama/Sources/Core/Config/AppConfig.swift` | 修改 | 提供商城首页 URL 配置读取 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增 mall 登录承接路由 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 管理 mall 搜索/登录返回恢复 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | mall tab 接入 `MallContainerView` |
| `ios/ShortDrama/Sources/Domain/Entities/MallLoginContext.swift` | 新增 | 商城登录上下文实体 |
| `ios/ShortDrama/Sources/Features/Mall/Models/MallContainerState.swift` | 新增 | 容器宿主态模型 |
| `ios/ShortDrama/Sources/Features/Mall/Models/MallBridgeMessage.swift` | 新增 | 搜索/登录 bridge 消息模型 |
| `ios/ShortDrama/Sources/Features/Mall/ViewModels/MallContainerViewModel.swift` | 新增 | 商城容器状态机与 bridge 调度 |
| `ios/ShortDrama/Sources/Features/Mall/Views/MallContainerView.swift` | 新增 | 商城 tab 根视图 |
| `ios/ShortDrama/Sources/Features/Mall/Views/MallLoginPlaceholderView.swift` | 新增 | 商城登录承接占位页 |
| `ios/ShortDrama/Sources/Features/Mall/Views/Components/MallContainerStateView.swift` | 新增 | loading/error 宿主态 UI |
| `ios/ShortDrama/Sources/Features/Mall/Views/Components/MallWebView.swift` | 新增 | WKWebView 容器与 bridge 注册 |
| `ios/ShortDrama/Tests/ViewModelTests/MallContainerViewModelTests.swift` | 新增 | 覆盖商城容器与 bridge 核心单测 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 覆盖 mall 路由、搜索返回、登录返回单测 |