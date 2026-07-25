# 实现计划：iOS — PRD-01 底部导航与应用路由

> 创建日期：2026-07-25
> 对应技术方案：design-ios.md
> 对应需求：spec.md

## 概述

本期 iOS 端将在现有单一 `NavigationStack` 首页骨架上，演进为 `TabView + 5 个一级频道 + 每个 Tab 独立 NavigationStack` 的应用壳，并补齐首页到播放页 / 详情页的路由入口、deeplink 待执行机制以及多 Tab 状态保持。实现过程中不新增后端接口、不引入新依赖，所有核心导航行为通过 Swift Testing 单元测试兜底。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | Router 冷启动默认落在首页 Tab | 新建 `NavigationRouter` | `selectedTab == .home`，5 个 Tab 路径均初始化为空 | 单元测试 | P0 |
| T-02 | 不同 Tab 导航栈彼此隔离 | 先向首页栈 push，再切换到商城 Tab | 首页栈保留原路径，商城栈仍为空 | 单元测试 | P0 |
| T-03 | 首页进入播放页 / 详情页时路由归属正确 | 调用 `navigate(.player("123"))` / `navigate(.dramaDetail("456"))` | 二级页面压入首页所属导航栈，当前仍归属于首页容器 | 单元测试 | P0 |
| T-04 | 冷启动 deeplink 在容器 ready 后执行 | `enqueueDeepLink(.player(videoId: "123"))` 后再 `markContainerReady()` | 容器 ready 后自动跳到首页 Tab 的播放页，并清空 `pendingRoute` | 单元测试 | P0 |
| T-05 | Deeplink 解析仅接受合法 scheme / host / 非空参数 | `djsdrama://open`、`djsdrama://play/123`、`djsdrama://drama/456`、非法 URL | 合法链接映射为正确 `AppRoute`；非法 scheme、未知 host、空参数返回 `nil` | 单元测试 | P0 |
| T-06 | 首页新增路由入口后仍保留现有加载与错误状态 | `HomeViewModel` 正常 / loading / error | 首页继续渲染基础信息，并新增播放页 / 详情页入口区 | 单元测试 | P1 |

## 实现步骤

### Step 1：重构 App 层导航状态为多 Tab 独立栈

- **关联测试**：T-01、T-02、T-03
- **目标文件**：`ios/ShortDrama/Sources/App/AppTab.swift`、`ios/ShortDrama/Sources/App/AppRoute.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`
- **实现内容**：
  1. 新增 `AppTab`，集中定义 `home / theater / mall / earn / profile` 的顺序、标题与图标语义。
  2. 扩展 `AppRoute`，保留端内 `player(videoId:)`、`dramaDetail(dramaId:)` case，同时补齐 `owningTab`、`publicRouteName` 等映射属性。
  3. 将 `NavigationRouter` 从单一 `path` 改造为 `selectedTab + pathsByTab + pendingRoute + containerReady` 多状态模型。
  4. 提供 `pathBinding(for:)`、`select(tab:)`、`navigate(to:)`、`dismiss(in:)`、`popToRoot(of:)` 等接口，保证二级页面统一归属首页 Tab。
  5. 先补齐 Router 单元测试，再落实现有逻辑，确保默认选中态、Tab 栈隔离和二级路由归属正确。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-01、T-02、T-03 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/AppTab.swift` | 新增 | 定义 5 个一级频道枚举与展示元数据 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 增加公开命名映射与所属 Tab 语义 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 从单栈改造为多 Tab 独立导航状态 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 增加默认首页、独立栈、二级路由归属等测试 |

### Step 2：补齐 AppShell、Tab 容器与占位频道页

- **关联测试**：T-01、T-02、T-06
- **目标文件**：`ios/ShortDrama/Sources/App/AppShellView.swift`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`、`ios/ShortDrama/Sources/Features/Shell/Views/PlaceholderTabView.swift`、`ios/ShortDrama/Sources/App/ShortDramaApp.swift`
- **实现内容**：
  1. 新增 `AppShellView`，以 `TabView` 固定承载 5 个一级频道。
  2. 新增 `TabNavigationHostView`，为每个 Tab 绑定独立 `NavigationStack` 与统一的 `navigationDestination` 注册。
  3. 新增 `PlaceholderTabView`，复用剧场 / 商城 / 赚钱 / 我的同构占位页，避免重复实现。
  4. 修改 `ShortDramaApp`，改为挂载 `AppShellView` 并继续注入共享 `NavigationRouter`。
  5. 在 `AppShellView.task` 中触发 `markContainerReady()`，为后续 pending deeplink 消费建立入口。
- **验证方式**：
  - 运行 `cd ios && xcodegen generate`
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 App 壳层可编译
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/AppShellView.swift` | 新增 | 以 `TabView` 承载 5 个一级频道 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 新增 | 单个 Tab 的 `NavigationStack` 容器 |
| `ios/ShortDrama/Sources/Features/Shell/Views/PlaceholderTabView.swift` | 新增 | 复用 4 个一级频道占位页 |
| `ios/ShortDrama/Sources/App/ShortDramaApp.swift` | 修改 | 接入新的应用壳与 router 生命周期 |

### Step 3：补齐 deeplink 解析与待执行机制

- **关联测试**：T-04、T-05
- **目标文件**：`ios/ShortDrama/Sources/App/DeeplinkHandler.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Sources/App/ShortDramaApp.swift`、`ios/ShortDrama/Tests/ViewModelTests/DeeplinkHandlerTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`
- **实现内容**：
  1. 收紧 `DeeplinkHandler` 解析规则，只接受 `djsdrama://open`、`djsdrama://play/{id}`、`djsdrama://drama/{id}`，并拒绝空参数。
  2. 在 `NavigationRouter` 中实现 `enqueueDeepLink(_:)` 与 `markContainerReady()` 的待执行逻辑。
  3. 修改 `ShortDramaApp.onOpenURL`，将 deeplink 先写入 router，再由容器 ready 后统一消费。
  4. 补齐单元测试，覆盖合法 deeplink、非法 scheme、未知 host、空参数、pending 覆盖与容器 ready 后消费。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-04、T-05 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/DeeplinkHandler.swift` | 修改 | 严格解析合法 deeplink 并拦截空参数 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 增加 pending deeplink 写入与消费逻辑 |
| `ios/ShortDrama/Sources/App/ShortDramaApp.swift` | 修改 | 将 `onOpenURL` 接入新的待执行导航机制 |
| `ios/ShortDrama/Tests/ViewModelTests/DeeplinkHandlerTests.swift` | 修改 | 校验合法 / 非法 deeplink 映射 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 校验 pending route 的生命周期与消费行为 |

### Step 4：补齐首页路由入口并完成回归验证

- **关联测试**：T-03、T-06
- **目标文件**：`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`、`ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift`、`ios/ShortDrama/Sources/Features/DramaDetail/Views/DramaDetailView.swift`
- **实现内容**：
  1. 在 `HomeView` 中增加进入播放页 / 详情页的占位入口区，保持现有应用信息、loading、error 展示不回归。
  2. 复用现有 `PlayerView`、`DramaDetailView` 作为二级页面占位承载，不改动其业务边界。
  3. 对首页入口文案与导航调用做最小修改，保证后续 PRD 可以在当前壳层继续扩展。
  4. 结合前面步骤完成一次全量测试与构建回归。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - 运行 `cd ios && swiftlint lint`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 增加播放页 / 详情页入口区并保留现有状态展示 |
| `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` | 复核 | 确认继续承接播放页占位参数展示 |
| `ios/ShortDrama/Sources/Features/DramaDetail/Views/DramaDetailView.swift` | 复核 | 确认继续承接详情页占位参数展示 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4
```

## 验证总览

- [ ] 所有测试通过（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [ ] Build 成功（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [ ] 无新增 lint 错误（`cd ios && swiftlint lint`）
- [ ] 新增文件已通过 `cd ios && xcodegen generate` 纳入工程

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/AppTab.swift` | 新增 | 5 个一级频道的集中定义 |
| `ios/ShortDrama/Sources/App/AppShellView.swift` | 新增 | App 主壳层与 `TabView` 容器 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 新增 | 单 Tab 的独立 `NavigationStack` 承载 |
| `ios/ShortDrama/Sources/Features/Shell/Views/PlaceholderTabView.swift` | 新增 | 剧场 / 商城 / 赚钱 / 我的占位页复用组件 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 公开命名与端内语义映射 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 多 Tab 导航状态、pending deeplink、恢复接口 |
| `ios/ShortDrama/Sources/App/DeeplinkHandler.swift` | 修改 | 合法 deeplink 解析与参数校验 |
| `ios/ShortDrama/Sources/App/ShortDramaApp.swift` | 修改 | 接入 AppShell 与 deeplink 待执行机制 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 首页新增二级路由入口 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 多 Tab、pending deeplink、恢复兜底测试 |
| `ios/ShortDrama/Tests/ViewModelTests/DeeplinkHandlerTests.swift` | 修改 | deeplink 映射与非法输入测试 |