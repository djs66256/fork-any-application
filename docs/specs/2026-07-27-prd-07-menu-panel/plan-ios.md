# 实现计划：iOS — PRD-07 菜单面板

> 创建日期：2026-07-28
> 对应技术方案：design-ios.md
> 对应需求：spec.md

## 概述

本期 iOS 端将在现有 `TabView + NavigationStack + NavigationRouter + MVVM + Clean Architecture` 基础上，把首页左上角菜单入口、壳层抽屉 overlay、最近在看数据链路、以及登录 / 消息 / 我的预约 / 我的下载占位承接串成一条可测试的 Native 菜单面板主链路。计划遵循轻量 TDD：先用 Swift Testing 锁定 Router、Data、ViewModel 与页面承接行为，再按 **壳层导航 → 数据契约 → 状态机 → UI 接线 → 工程回归** 的顺序推进实现；实现范围仅限 `ios/` 目录，不改动其他端代码。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 首页左上角菜单入口仅在 home 根页打开壳层菜单 | 点击 `HomeView` 顶部菜单按钮 | `NavigationRouter` 进入菜单打开态，`AppShellView` 可承载 overlay，非 home tab 不会保留打开态 | 单元测试 | P0 |
| T-02 | 点击蒙层或关闭动作可稳定关闭菜单 | 菜单已打开，触发蒙层点击或关闭动作 | 菜单进入关闭态并回到 `closed`，背景交互恢复，不残留半开状态 | 单元测试 | P0 |
| T-03 | 点击登录 / 消息 / 我的预约 / 我的下载先关抽屉再导航 | 菜单打开时点击占位入口 | 先进入 closing，待 `markMenuPanelDidClose()` 后才导航到 `.menuPlaceholder(kind:)`，且只压栈一次 | 单元测试 | P0 |
| T-04 | 点击最近在看卡片复用既有播放页路由 | 菜单打开且存在 `dramaId` 有效的最近在看项 | 先关闭菜单，再导航到 `.player(videoId:)`，不新增独立续播路由 | 单元测试 | P0 |
| T-05 | 最近在看接口请求头与 DTO 解码正确 | 调用 recently-viewed endpoint，传入 session id | 发送 `GET /api/player/recently-viewed` 且带 `X-Playback-Session-Id`，响应可正确解码为实体 | 单元测试 | P0 |
| T-06 | 最近在看首次加载成功进入内容态 | 首次打开菜单，session store 返回有效 id，接口返回 1~3 条数据 | `MenuPanelViewModel` 进入 `content(items)`，并保留当前会话内已加载状态 | 单元测试 | P0 |
| T-07 | 最近在看空态与错误态可区分处理 | 接口返回空数组，或 session / 网络请求失败 | ViewModel 分别进入 `empty` 或 `error`，静态区块仍可展示，错误支持重试 | 单元测试 | P0 |
| T-08 | 快速重复打开 / 重试不会产生重复 in-flight 请求与重复导航 | 连续打开菜单、连续点击重试或 closing 期间重复点击入口 | 只保留一个有效请求或一次有效导航，旧结果不得覆盖新状态 | 单元测试 | P1 |
| T-09 | 菜单占位页返回后回到首页常态 | 从 `.menuPlaceholder(kind:)` 返回 | 回到 home 导航栈上一级，菜单保持关闭，不回到“抽屉仍打开”的中间态 | 单元测试 | P1 |

## 实现步骤

### Step 1：先锁定菜单壳层状态机与占位承接导航

- **关联测试**：T-01、T-02、T-03、T-04、T-09
- **目标文件**：`ios/ShortDrama/Sources/App/AppShellView.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Sources/App/AppRoute.swift`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`
- **实现内容**：
  1. 先在 `NavigationRouterTests` 中补齐菜单状态机测试，锁定 `openMenuPanel()`、`closeMenuPanel()`、`closeMenuPanelThenNavigate(to:)`、`markMenuPanelDidClose()` 的行为，以及 closing 期间重复点击只消费一次导航的规则。
  2. 在 `AppRoute` 中新增 `.menuPlaceholder(kind:)`，并保证其 `owningTab` 仍归属 `.home`，不改变既有 `ranking`、`classification`、`play` 等公开路由语义。
  3. 扩展 `NavigationRouter`，增加菜单展示状态、待执行导航缓存，以及“先关闭抽屉、动画完成后再导航”的时序控制；切换到非 `.home` tab 时自动关闭菜单并清理待导航状态。
  4. 在 `AppShellView` 把菜单 overlay 提升到 `TabView` 外层承载，确保打开菜单时可覆盖首页内容区与底部 Tab 交互层，而不是把状态仅放在 `HomeView` 内部。
  5. 在 `HomeView` 顶部 toolbar 增加 leading 菜单按钮，只负责触发 router 的打开动作；`TabNavigationHostView` 同步注册后续菜单占位页承接入口。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-01、T-02、T-03、T-04、T-09 对应 Router 测试通过；若本机模拟器名称不同，以实际已安装模拟器为准调整 `-destination` ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/AppShellView.swift` | 修改 | 提升菜单 overlay 到壳层容器 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 增加菜单开关状态与关闭后导航机制 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增菜单占位页路由 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 注册菜单占位页 destination |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 首页顶部增加菜单按钮 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 覆盖菜单状态机、导航时序与返回语义 |

### Step 2：先补 recently-viewed 数据契约测试，再接通 Domain / Data 链路

- **关联测试**：T-05
- **目标文件**：`ios/ShortDrama/Sources/Domain/Entities/RecentlyViewedItem.swift`、`ios/ShortDrama/Sources/Domain/RepositoryProtocols/MenuPanelRepositoryProtocol.swift`、`ios/ShortDrama/Sources/Domain/UseCases/FetchRecentlyViewedUseCase.swift`、`ios/ShortDrama/Sources/Data/DTOs/RecentlyViewedResponseDTO.swift`、`ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift`、`ios/ShortDrama/Sources/Data/Repositories/MenuPanelRepository.swift`、`ios/ShortDrama/Tests/DataTests/PlayerRemoteDataSourceTests.swift`、`ios/ShortDrama/Tests/DataTests/MenuPanelRepositoryTests.swift`
- **实现内容**：
  1. 先在 `PlayerRemoteDataSourceTests` 中新增 recently-viewed 请求测试，锁定 path、method、`X-Playback-Session-Id` header 与成功响应解码规则。
  2. 在 Domain 层新增 `RecentlyViewedItem`、`MenuPanelRepositoryProtocol` 与 `FetchRecentlyViewedUseCase`，让菜单动态区块依赖独立用例，而不是把职责继续堆到现有播放器 ViewModel 中。
  3. 在 Data 层新增 `RecentlyViewedResponseDTO`，处理 `code / data / message` 包裹与 snake_case 到 camelCase 的字段映射，兼容 `cover_url = null` 的响应。
  4. 扩展 `PlayerRemoteDataSource` 增加 `fetchRecentlyViewed(playbackSessionId:)`，并新建 `MenuPanelRepository` 把 DTO 映射为 `[RecentlyViewedItem]`，避免污染 `PlayerRepository` 的播放控制职责。
  5. 在 `MenuPanelRepositoryTests` 中补齐 DTO 映射、空封面、错误透传等断言，先锁定 contract 再推进 ViewModel。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-05 的 Data 层契约测试通过；若本机模拟器名称不同，以实际已安装模拟器为准调整 `-destination` ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Domain/Entities/RecentlyViewedItem.swift` | 新增 | 最近在看领域实体 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/MenuPanelRepositoryProtocol.swift` | 新增 | 菜单面板数据读取协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchRecentlyViewedUseCase.swift` | 新增 | 最近在看读取用例 |
| `ios/ShortDrama/Sources/Data/DTOs/RecentlyViewedResponseDTO.swift` | 新增 | recently-viewed 响应 DTO |
| `ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift` | 修改 | 增加 recently-viewed endpoint |
| `ios/ShortDrama/Sources/Data/Repositories/MenuPanelRepository.swift` | 新增 | DTO 到 Entity 映射与错误透传 |
| `ios/ShortDrama/Tests/DataTests/PlayerRemoteDataSourceTests.swift` | 修改 | 覆盖 header、path、decode 契约 |
| `ios/ShortDrama/Tests/DataTests/MenuPanelRepositoryTests.swift` | 新增 | 覆盖 recently-viewed 映射测试 |

### Step 3：先写 ViewModel 测试，再落地最近在看状态机与重试保护

- **关联测试**：T-06、T-07、T-08
- **目标文件**：`ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift`、`ios/ShortDrama/Sources/Core/Storage/PlaybackSessionStore.swift`、`ios/ShortDrama/Tests/ViewModelTests/MenuPanelViewModelTests.swift`、`ios/ShortDrama/Tests/Mocks/MockPlayerRepository.swift`
- **实现内容**：
  1. 先新增 `MenuPanelViewModelTests`，覆盖首次打开加载成功、空态、session store 失败、网络失败、重试恢复、重复点击重试去重、已加载后的 `loadIfNeeded()` 不重复请求等场景。
  2. 在 `MenuPanelViewModel` 中建立清晰状态机，至少覆盖 `idle / loading / content / empty / error`，并管理 `hasLoaded`、`isRetrying`、`inFlightTask` 等状态，保证最近在看区块可独立演进。
  3. 请求前统一复用 `PlaybackSessionStore.getOrCreateSessionId()`；若 session 初始化失败，最近在看区只进入局部错误态或安全降级，而不影响菜单静态区展示。
  4. 实现 `loadIfNeeded()`、`retry()` 与点击最近在看项的输入校验，保证快速重复打开菜单或重试时只保留一个有效请求，旧结果不会覆盖最新状态。
  5. 根据测试需要补最小 mock 能力，例如 recently-viewed 成功 / 空 / 失败返回与 session store 可控异常，不新增额外第三方测试依赖。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-06、T-07、T-08 对应 ViewModel 测试通过；若本机模拟器名称不同，以实际已安装模拟器为准调整 `-destination` ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift` | 新增 | 最近在看状态机、重试与请求去重 |
| `ios/ShortDrama/Sources/Core/Storage/PlaybackSessionStore.swift` | 修改 | 如有必要补最小测试注入点，继续复用现有 session store 能力 |
| `ios/ShortDrama/Tests/ViewModelTests/MenuPanelViewModelTests.swift` | 新增 | 覆盖成功、空态、错误、重试、去重 |
| `ios/ShortDrama/Tests/Mocks/MockPlayerRepository.swift` | 修改 | 补最小 mock 以支撑菜单动态区测试，或提炼共享 mock 能力 |

### Step 4：接线菜单 UI、占位页与首页壳层承载

- **关联测试**：T-01、T-02、T-03、T-04、T-07
- **目标文件**：`ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift`、`ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelView.swift`、`ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPlaceholderView.swift`、`ios/ShortDrama/Sources/Features/MenuPanel/Views/Components/*.swift`、`ios/ShortDrama/Sources/App/AppShellView.swift`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`
- **实现内容**：
  1. 按 `design-ios.md` 在 `Features/MenuPanel` 下落地菜单容器、菜单内容页与拆分组件，固定区块顺序为登录引导、消息预览、最近在看、游戏中心、常用功能。
  2. 在 `MenuPanelContainerView` 中实现蒙层点击关闭、左侧抽屉动画、背景交互禁用和关闭完成回调，把“先关抽屉再导航”的时序落实为可测试的 UI 承接逻辑。
  3. 在 `MenuPanelView` 中接入 `MenuPanelViewModel` 的最近在看三态展示，并将最近在看卡片点击统一收口到 `.player(videoId:)`；登录 / 消息 / 我的预约 / 我的下载统一收口到 `.menuPlaceholder(kind:)`。
  4. 新增 `MenuPlaceholderView` 统一承接登录、消息、预约、下载四类占位页，保证返回后回到首页常态，菜单保持关闭；游戏中心 4 个入口只提供“即将上线”反馈，不发生导航。
  5. 通过 `AppShellView` 和 `TabNavigationHostView` 完成壳层接线，确保 overlay 仅由 home tab 承载，且不会破坏现有搜索、排行、分类、播放等主链路。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 回归 T-01、T-02、T-03、T-04、T-07 的页面接线相关测试；若本机模拟器名称不同，以实际已安装模拟器为准调整 `-destination` ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift` | 新增 | 蒙层、抽屉动画与关闭回调容器 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelView.swift` | 新增 | 菜单面板主内容视图 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPlaceholderView.swift` | 新增 | 登录 / 消息 / 预约 / 下载占位页 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/Components/*.swift` | 新增 | 头部、消息区、最近在看卡片、游戏区、常用功能区组件 |
| `ios/ShortDrama/Sources/App/AppShellView.swift` | 修改 | 接入菜单 overlay 与背景禁用 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 接入菜单占位页与菜单相关 destination |

### Step 5：执行工程生成与 iOS 全量回归，固化交付基线

- **关联测试**：T-01 ～ T-09
- **目标文件**：`ios/project.yml`、`ios/ShortDrama.xcodeproj/**`（由 XcodeGen 生成产物）
- **实现内容**：
  1. 在实际 coding 阶段按本计划顺序推进：先补测试，再补实现；新增 Swift 文件后先运行 `xcodegen generate`，不直接修改 `.xcodeproj/project.pbxproj`。
  2. 以仓库现有 iOS 命令完成测试、构建与 lint 回归，确认菜单入口、壳层抽屉、recently-viewed 数据链路、占位页承接与返回语义都已被自动化覆盖。
  3. 若本机环境与 `ios/CLAUDE.md` 中的模拟器名称或工具安装状态不完全一致，命令可按本机实际 Xcode / Simulator / SwiftLint 安装情况微调，但必须保持 `xcodegen -> test -> build -> lint` 的验证顺序。
- **验证方式**：
  - 运行 `cd ios && xcodegen generate` ✅ 已完成
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` ✅ 已完成
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` ✅ 已完成
  - 候选命令：`cd ios && swiftlint lint`（以本机已安装 `swiftlint` 为准）
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/project.yml` | 复核 / 无需修改 | 确认新增源码仍由 XcodeGen 通配纳入工程 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5
```

## 验证总览

- [x] 所有测试通过（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`，若本机模拟器不同则以实际安装项为准）
- [x] Build 成功（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`，若本机模拟器不同则以实际安装项为准）
- [x] 无新增 lint 错误（候选命令：`cd ios && swiftlint lint`，以本机已安装工具为准）
- [x] 新增源码后已执行 `cd ios && xcodegen generate`
- [x] 首页菜单入口、壳层抽屉开关、关闭后导航时序均有单元测试覆盖
- [x] 最近在看 endpoint 的 header、DTO 解码与 Repository 映射均有单元测试覆盖
- [x] 最近在看成功 / 空态 / 错误 / 重试 / 去重均有单元测试覆盖
- [x] 点击最近在看继续复用 `.player(videoId:)`，不新增独立续播路由
- [x] 登录 / 消息 / 我的预约 / 我的下载入口均先关闭菜单，再进入统一占位承接页
- [x] 菜单只由 home 壳层承载，不修改其他 tab 的占位职责

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/AppShellView.swift` | 修改 | 壳层菜单 overlay 承载与背景交互禁用 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 菜单状态机、待导航缓存与关闭后导航 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 菜单占位页 route 扩展 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 菜单占位页与菜单相关 destination 接线 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 首页 leading 菜单按钮 |
| `ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift` | 新增 | 菜单动态区状态机 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift` | 新增 | 抽屉容器与蒙层交互 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelView.swift` | 新增 | 菜单主视图 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPlaceholderView.swift` | 新增 | 登录 / 消息 / 预约 / 下载占位页 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/Components/*.swift` | 新增 | 菜单各区块组件 |
| `ios/ShortDrama/Sources/Domain/Entities/RecentlyViewedItem.swift` | 新增 | 最近在看实体 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/MenuPanelRepositoryProtocol.swift` | 新增 | 菜单仓储协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchRecentlyViewedUseCase.swift` | 新增 | 最近在看用例 |
| `ios/ShortDrama/Sources/Data/DTOs/RecentlyViewedResponseDTO.swift` | 新增 | recently-viewed DTO |
| `ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift` | 修改 | recently-viewed endpoint 接线 |
| `ios/ShortDrama/Sources/Data/Repositories/MenuPanelRepository.swift` | 新增 | recently-viewed 数据映射 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 菜单状态机与导航时序测试 |
| `ios/ShortDrama/Tests/ViewModelTests/MenuPanelViewModelTests.swift` | 新增 | 最近在看状态机测试 |
| `ios/ShortDrama/Tests/DataTests/PlayerRemoteDataSourceTests.swift` | 修改 | recently-viewed 请求契约测试 |
| `ios/ShortDrama/Tests/DataTests/MenuPanelRepositoryTests.swift` | 新增 | recently-viewed 映射测试 |
| `ios/ShortDrama/Tests/Mocks/MockPlayerRepository.swift` | 修改 | 菜单相关 mock 能力补齐 |