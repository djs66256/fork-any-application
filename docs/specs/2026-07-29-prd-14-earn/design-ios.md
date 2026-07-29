# iOS 端技术方案：PRD-14 赚钱中心

> 创建日期：2026-07-29
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

```text
AppShellView (earn tab)
→ TabNavigationHostView(tab: .earn)
→ EarnContainerView
  → EarnContainerViewModel
  → EarnWebView (WKWebView wrapper)
  → EarnContainerStateView (loading / error)

EarnWebView JS bridge
→ earn.requestLogin(payload)
  → NavigationRouter.presentEarnLogin(context)
  → close/dismiss/success => router.restore earn context
  → router sync earn auth state to web view
→ earn.openTaskPlayer(payload)
  → NavigationRouter.navigate(to: .earnPlayer(context))
  → Player route opened with held earn task context
  → player close/back => router restores earn context
  → host sends earn.completeTask / earn.restoreContext
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | earn tab 从 `PlaceholderTabView` 切换为 `EarnContainerView` |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 扩展 | 新增 earn login / earn player handoff route |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 扩展 | 管理 earn 登录承接、播放返回与上下文恢复 |
| `ios/ShortDrama/Sources/Core/Config/AppConfig.swift` | 扩展 | 新增 `earnBaseURL` / `earnHomeURL()` |
| `ios/ShortDrama/Sources/Features/Earn/` | 新增 | 赚钱容器、bridge、登录承接、状态视图 |
| `ios/ShortDrama/Sources/Features/Mall/` | 参考 | earn 参考 mall 容器组织方式，但模型与消息独立 |
| `ios/ShortDrama/Sources/Features/Player/` | 复用/扩展 | 现有 player route 仍只接收 `videoId`，earn 需在 router / handoff 层补持任务上下文 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | earn tab root 改为 `EarnContainerView()` |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增 `.earnLogin(context:)`、`.earnPlayer(context:)` |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 新增 earn 登录承接、任务返回、player result 与 restore request 管理 |
| `ios/ShortDrama/Sources/Core/Config/AppConfig.swift` | 修改 | 新增 earn H5 URL 配置读取 |
| `ios/ShortDrama/Sources/Features/Auth/AuthStore.swift` | 复用 | 提供是否已登录与当前 access token 快照给 earn host sync 使用 |
| `ios/ShortDrama/Sources/Domain/Entities/EarnLoginContext.swift` | 新增 | earn 登录上下文实体 |
| `ios/ShortDrama/Sources/Domain/Entities/EarnTaskContext.swift` | 新增 | earn 任务播放上下文实体 |
| `ios/ShortDrama/Sources/Features/Earn/Models/EarnContainerState.swift` | 新增 | earn 容器状态枚举 |
| `ios/ShortDrama/Sources/Features/Earn/Models/EarnHostMessage.swift` | 新增 | earn.syncAuthState / restoreContext / completeTask 宿主消息模型 |
| `ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift` | 新增 | 容器状态、bridge effect、登录/任务返回处理 |
| `ios/ShortDrama/Sources/Features/Earn/Views/EarnContainerView.swift` | 新增 | earn tab 根视图 |
| `ios/ShortDrama/Sources/Features/Earn/Views/Components/EarnWebView.swift` | 新增 | `UIViewRepresentable` 封装 WKWebView |
| `ios/ShortDrama/Sources/Features/Earn/Views/Components/EarnContainerStateView.swift` | 新增 | loading / error 宿主态 |
| `ios/ShortDrama/Sources/Features/Earn/Views/EarnLoginPlaceholderView.swift` | 新增 | earn 专属全屏登录承接页 |
| `ios/ShortDrama/Tests/ViewModelTests/EarnContainerViewModelTests.swift` | 新增 | 覆盖 bridge effect、恢复语义、task completion host message |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 增补 earn login / player / restore 路由测试 |

---

## 3. View 层设计

### 3.1 组件层级树

```text
EarnContainerView
├── EarnContainerStateView (loading / error only)
├── EarnWebView
└── FullScreenCover / Navigation Destination
    └── EarnLoginPlaceholderView
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `EarnContainerView` | View | earn tab 根视图，装配 ViewModel 与 router | 否 |
| `EarnWebView` | View | 承载 WKWebView、注册 script message handler、加载 earn H5 | 否 |
| `EarnContainerStateView` | View | H5 首次加载 loading / error UI | 否 |
| `EarnLoginPlaceholderView` | View | earn 专属全屏登录承接占位页 | 否 |

### 3.3 组件接口定义

```swift
struct EarnContainerView: View {
    @EnvironmentObject private var router: NavigationRouter
    @StateObject private var viewModel: EarnContainerViewModel
}

struct EarnWebView: UIViewRepresentable {
    let request: URLRequest
    let loadRevision: Int
    let hostMessage: EarnHostMessage?
    let onPageLoaded: (URL?) -> Void
    let onPageLoadFailed: (URL?, String) -> Void
    let onBridgeMessage: (EarnBridgeMessage) -> Void
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| 父 → 子 | 构造函数参数 | request、hostMessage、bridge handler |
| 子 → 父 | Closure callback | WKWebView 页面事件、bridge 消息回传 |
| 跨层级共享 | `@EnvironmentObject` `NavigationRouter` | 登录承接、播放承接、tab 切换 |

### 3.5 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | WebView 全屏铺满安全区内区域 | 遵循 earn H5 移动布局 |
| Dynamic Type | 宿主态文案使用现有设计系统 | loading / error 态保持可读 |
| 深色模式 | 宿主态支持深色；H5 主题由 Web 控制 | Native 不强行覆写 H5 |
| 安全区域 | `EarnContainerView` 遵循 Tab 宿主布局 | 不让 H5 自行穿透底部 tab |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 View | 职责 |
|-----------|----------|------|
| `EarnContainerViewModel` | `EarnContainerView` | 管理容器状态、bridge effect、登录返回、任务返回、host sync |

### 4.2 状态定义

```swift
@MainActor
final class EarnContainerViewModel: ObservableObject {
    enum RouteEffect: Equatable {
        case requestLogin(EarnLoginContext)
        case openTaskPlayer(EarnTaskContext)
    }

    @Published private(set) var state: EarnContainerState = .loading
    @Published private(set) var currentRequest: URLRequest?
    @Published private(set) var currentURL: URL?
    @Published private(set) var lastLoadedHomeURL: URL?
    @Published private(set) var loadRevision = 0
    @Published private(set) var pendingLoginContext: EarnLoginContext?
    @Published private(set) var pendingTaskContext: EarnTaskContext?
    @Published private(set) var routeEffect: RouteEffect?
    @Published private(set) var hostMessage: EarnHostMessage?
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `state` | `EarnContainerState` | `.loading` | 容器级 loading / success / error |
| `currentRequest` | `URLRequest?` | `nil` | 当前 earn H5 request |
| `currentURL` | `URL?` | `nil` | 当前页面 URL |
| `lastLoadedHomeURL` | `URL?` | `nil` | 成功加载 `/earn` 后的恢复目标 |
| `pendingLoginContext` | `EarnLoginContext?` | `nil` | 等待登录承接的上下文 |
| `pendingTaskContext` | `EarnTaskContext?` | `nil` | 等待播放返回与任务完成的上下文 |
| `routeEffect` | `RouteEffect?` | `nil` | 交给 View / Router 执行的导航动作 |
| `hostMessage` | `EarnHostMessage?` | `nil` | Native → H5 宿主消息 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | View 层表现 |
|---------|---------|-----------|
| Loading | `state == .loading` | `ProgressView` / skeleton 宿主态 |
| Success | `state == .success` | 展示 `EarnWebView` |
| Error (可重试) | `state == .error(message)` | 错误说明 + 重试按钮 |
| Error (不可重试) | 暂不单独建模 | 首版统一视作可重试 |

---

## 5. Navigation 路由设计

### 5.1 导航方案

- 继续使用现有 `TabView + NavigationStack + NavigationRouter`。
- earn 首页本身不使用 SwiftUI Push 组织子页面，而是由 `WKWebView` 承载 H5 页面。
- 登录承接使用 `fullScreenCover` 或等效全屏路由；该页面语义明确为 **earn-owned login handoff**。
- 播放承接不改造现有 player 页面 UI，但新增 `earnPlayer(context:)` 路由语义，在 router 层持有 `taskId / source / returnTarget / videoId`，再落到现有 `.player(videoId:)`。
- earn host sync 的唯一注入协议固定为 `window.dispatchEvent(new CustomEvent('earn.hostMessage', { detail }))`，iOS 不再为 earn 保留 `window.message` 兼容通道。

### 5.2 路由清单

| 路由标识 | 目标页面 | 参数 | 导航方式 | 说明 |
|---------|---------|------|---------|------|
| `.earnLogin(context)` | `EarnLoginPlaceholderView` | `EarnLoginContext` | FullScreenCover | earn 登录承接 |
| `.earnPlayer(context)` | 现有 `Player` 承接链路 | `EarnTaskContext` | Push / router handoff | 持有任务上下文打开播放 |
| `.player(videoId:)` | 现有播放器页面 | `videoId` | Push | 仍为最终播放器路由 |

### 5.3 路由管理

```swift
enum AppRoute: Hashable, Sendable {
    case player(videoId: String)
    case earnLogin(context: EarnLoginContext)
    case earnPlayer(context: EarnTaskContext)
}
```

- `owningTab` 中：
  - `.earnLogin` 归属 `.earn`；
  - `.earnPlayer` 归属 `.earn`，确保从赚钱任务进入播放器期间 tab 语义仍来自 earn。
- `NavigationRouter` 新增：
  - `presentEarnLogin(_ context: EarnLoginContext)`
  - `dismissEarnLogin(completed: Bool)`
  - `openPlayerFromEarn(_ context: EarnTaskContext)`
  - `finishEarnTaskPlayer(result: EarnTaskPlayerResult)`
  - `consumeEarnTaskPlayerResult() -> EarnTaskPlayerResult?`
- 最低恢复保证：登录与任务返回后重新选中 `.earn`，并生成 `pendingEarnRestoreRequest` 供 `EarnContainerViewModel` 发回 `earn.restoreContext` / `earn.completeTask`。
- `finishEarnTaskPlayer(result:)` 的语义固定为：
  - `completed=true` 仅在 player 明确收到“代表性任务视频自然播放结束”信号时调用；
  - `handleBack()`、`handleDisappear()`、后台切换、异常退出都必须产出 `completed=false` 的结果。

### 5.4 Deep Link 处理（如适用）

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `djsdrama://earn` | `EarnContainerView` | 无 |
| `djsdrama://earn/login` | `EarnLoginPlaceholderView` | 仅内部使用，不对外公开 |

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| H5 容器加载 | `WKWebView` | 加载 `AppConfig.earnBaseURL + /earn` |
| 原生 API | 无新增 earn 原生 API 请求 | overview / complete-task 均由 H5 自己调用 Backend |
| 原生配置读取 | `AppConfig` | 读取 earn base URL |
| bridge 通讯 | `WKScriptMessageHandler` | 处理 `earn.requestLogin` / `earn.openTaskPlayer` |

### 6.2 API 端点定义

```swift
struct EarnLoginContext: Hashable, Sendable {
    let source: String
    let returnTarget: String
}

struct EarnTaskContext: Hashable, Sendable {
    let taskId: String
    let source: String
    let returnTarget: String
    let videoId: String
}
```

- iOS 首版不直接请求 `GET /api/earn/overview` 或 `POST /api/earn/complete-task`。
- 原生只消费 bridge message、维护登录/任务返回语义、并向 H5 发送 host message。

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| H5 首次加载失败 | 0（手动重试） | — | 点击“重试”重新加载首页 URL |
| bridge 消息解析失败 | 0 | — | 忽略消息并记录日志 |
| 登录/任务返回恢复失败 | 0 | — | 直接重载 earn 首页 |

### 6.4 网络状态监听

- 本期不为 earn 单独做 `NWPathMonitor`。
- H5 加载失败宿主反馈由 `WKNavigationDelegate` 的失败回调驱动。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| earn 容器最近 URL | 内存态 | `EarnContainerViewModel` | 会话内有效 | 不做持久化 |
| 登录承接上下文 | 内存态 | `NavigationRouter` / `EarnContainerViewModel` | 登录完成即清空 | 不写 UserDefaults |
| 任务播放上下文 | 内存态 | `NavigationRouter` | 任务返回即清空 | 不做跨会话恢复 |
| H5 页面状态 | 由 WebView 内存态持有 | WKWebView | 容器销毁即失效 | 首版不做跨会话恢复 |

### 7.2 CoreData 模型设计（如适用）

```text
不使用 CoreData。
```

### 7.3 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| WebView 页面缓存 | 依赖 WKWebView 默认缓存 | 系统控制 | 系统回收 |
| 最近成功首页 URL | 内存缓存 | 当前会话 | App 退出清空 |

### 7.4 数据迁移策略

- 首版不新增持久化模型，无 migration。
- 若后续需要恢复 earn scroll position，再评估是否引入轻量 session cache，不在本期实现。

---

## 8. 配置与环境

| 配置项 | 管理方式 | 开发环境值 | 生产环境值 | 说明 |
|--------|---------|----------|-----------|------|
| API Base URL | Info.plist / xcconfig | 现有配置 | 现有配置 | 继续供原生 API 使用 |
| Earn Base URL | Info.plist / xcconfig | 环境注入 | 环境注入 | 用于拼接 `/earn` 首页 |
| App Name | Info.plist | 现有配置 | 现有配置 | 使用 `AppConfig.appName()` |

> ⚠️ 禁止硬编码任何 URL、`/earn` returnTarget、taskId 或 bridge 对象名。赚钱 H5 首页地址必须通过 `AppConfig` 读取。

---

## 9. API 调用清单

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| 无新增原生 earn API | — | — | overview / complete-task 由 H5 自己请求 | iOS 仅处理 WebView 加载错误与 host sync |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | iOS 端实现方式 |
|---------|---------------|---------------|
| H5 承载赚钱首页 | Native 只做容器 | `EarnContainerView` + `EarnWebView` |
| 登录 bridge | `earn.requestLogin` | `WKScriptMessageHandler` 收到消息后 `router.presentEarnLogin(context)` |
| 登录返回契约 | 返回 `/earn` 且 tab 高亮正确 | `dismissEarnLogin(completed:)` 后设置 `pendingEarnRestoreRequest` |
| 播放 bridge | `earn.openTaskPlayer` | router 持有 `EarnTaskContext` 后打开 `.player(videoId:)` |
| player 结果契约 | `EarnTaskPlayerResult` | `PlayerViewModel` / router 在退出时统一产出结果对象 |
| 任务完成闭环 | 播放完成后发 `earn.completeTask` | router / ViewModel 在任务返回时向 H5 发 host message |
| 登录态同步 | Native 返回权威登录态与 access token 快照 | 初次加载、登录成功/取消、App 恢复时发 `earn.syncAuthState` |
| host sync transport | Native 统一注入 `CustomEvent('earn.hostMessage')` | `EarnWebView` 的 JS 注入 helper 统一构造 event name 与 detail |
| 容器三态 | loading / success / error | `EarnContainerState` 管理宿主 UI |
| 最低恢复保证 | 容器重建后至少回到 `/earn` 首屏 | `reloadHome()` 重载首页 URL |

---

## 11. 边界与错误处理

### 11.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| WebView 导航层 | `WKNavigationDelegate` | 捕获页面加载失败 |
| ViewModel | `handleBridgeMessage` / `reload` / `handleTaskReturn` | 统一处理 bridge 与容器状态 |
| View 层 | 内联错误视图 / 全屏登录承接页 | 不退回 placeholder |
| 日志 | `os_log` / `print`（开发态） | 记录 bridge 解析失败 |

### 11.2 错误码映射表

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `NETWORK_ERROR` | 赚钱页加载失败，请稍后重试 | 宿主错误页 + 重试 |
| `INTERNAL_ERROR` | 赚钱页暂时不可用 | 宿主错误页 + 重试 |
| `VALIDATION_ERROR` | 页面参数异常 | 忽略当前 bridge 消息 |
| `AUTH_UNAUTHORIZED` | 请先登录 | 由 H5 先展示引导；iOS 只承接登录页 |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| H5 首页加载失败 | `didFailProvisionalNavigation` / `didFail` | 展示宿主错误态与重试 | 🔴 |
| bridge payload 缺字段 | 非法 `EarnLoginContext` / `EarnTaskContext` | 忽略消息并记录日志 | 🔴 |
| 登录承接被关闭 | 用户取消 / 关闭 | 回到 earn tab，并发 `earn.restoreContext(reason='login-return')` | 🔴 |
| 播放未完成返回 | 用户中途退出播放器 | 只发 `earn.restoreContext(reason='task-return')`，不发完成消息 | 🔴 |
| 播放完成返回 | Native 已确认完成 | 先发 `earn.completeTask(completed=true)` 再恢复上下文 | 🔴 |
| access token 过期 | AuthStore 刷新前后 token 变化 | 下一次 `earn.syncAuthState` 覆盖 H5 快照；不下发 refresh token | 🔴 |
| 容器被系统回收 | 后台回来 / 登录链路后重建 | 重新加载 earn 首页并同步登录态 | 🟡 |
| 多次快速点击任务 | 重复收到 `earn.openTaskPlayer` | 若已有 `pendingTaskContext` 则忽略后续事件 | 🟡 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `EarnContainerView` | `ProgressView` | `EarnWebView` | — | 错误说明 + 重试 | — |
| `EarnLoginPlaceholderView` | 轻量加载 | 显示占位登录承接 | — | 关闭返回赚钱页 | — |

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | `EarnContainerViewModel` 状态机、bridge effect、task return 处理 | 核心逻辑覆盖 | Swift Testing |
| Router 测试 | `NavigationRouter` earn login / player / restore 逻辑 | 核心导航覆盖 | Swift Testing |
| 解析测试 | earn bridge payload decode / validate | 合法/非法输入覆盖 | Swift Testing |

### 12.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| IOS-EARN-01 | 首页首次加载 | earn URL 有效 | `loadInitialPage()` | 状态从 loading 到 success | 单元 |
| IOS-EARN-02 | 首页加载失败 | webview 回调失败 | `handlePageLoadFailed()` | 状态进入 error | 单元 |
| IOS-EARN-03 | 登录 bridge | 收到 `earn.requestLogin` | 处理 bridge | 发送 `requestLogin` route effect | 单元 |
| IOS-EARN-04 | 播放 bridge | 收到合法 `EarnTaskContext` | 处理 bridge | 发送 `openTaskPlayer` effect | 单元 |
| IOS-EARN-05 | 登录成功返回 | earn login 完成 | `handleLoginSuccess()` | 发出 `earn.syncAuthState` 与 `earn.restoreContext` | 单元 |
| IOS-EARN-06 | 任务完成返回 | 播放完成 | `handleTaskCompletion()` | 发出 `earn.completeTask` host message | 单元 |
| IOS-EARN-07 | 非法 payload | taskId 为空 | 处理 bridge | 忽略消息，不崩溃 | 单元 |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| `NavigationRouter` | Mock / test double | 验证导航 effect |
| WebView 页面事件 | 通过 ViewModel 回调模拟 | 不依赖真实 WKWebView |
| 配置读取 | 自定义 `Bundle` / `AppConfig` wrapper | 避免真实 Info.plist 依赖 |
| 登录态提供者 | closure stub | 验证 `earn.syncAuthState` payload |

---

## 13. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | — | — | 复用 SwiftUI、WKWebView、Swift Testing |

> ⚠️ 新增开源依赖前必须征得用户同意（遵守根目录 CLAUDE.md 开发约束）。

---

## 14. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 直接复用 mallLogin route 破坏 earn 返回语义 | iOS / 产品语义 | 🔴 | 中 | 独立新增 `.earnLogin(context:)` 与 restore request | 临时 earn 专属占位登录页 |
| 继续只用 `.player(videoId:)` 导致 task 上下文丢失 | iOS / Earn 闭环 | 🔴 | 中 | 新增 `EarnTaskContext` 与 `.earnPlayer(context:)` handoff 层 | 路由层内存持有上下文再跳现有 player |
| WebView bridge 直接耦合 WebKit 细节到 View | iOS | 🟡 | 中 | 抽离 `EarnWebView` + ViewModel 回调 | 保留最小 bridge 封装 |
| earn H5 地址被硬编码 | iOS / 环境切换 | 🔴 | 中 | 使用 `Info.plist + xcconfig + AppConfig` | 无 |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/features/app-shell/index.md` | iOS、已知限制 | earn 仍为 placeholder，需切换为真实容器 |
| `wiki/architecture/overview.md` | 承载策略 | earn 由 H5 + Native 容器承载 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `ios/CLAUDE.md` | iOS 架构、配置、NavigationStack 与测试约束 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 当前只有 `mallLogin`，没有 earn route |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | mall 搜索/登录恢复模式可参考 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | earn tab 当前仍为 placeholder |
| `ios/ShortDrama/Sources/Core/Config/AppConfig.swift` | 当前仅有 mallBaseURL / mallHomeURL |
| `ios/ShortDrama/Sources/Features/Mall/ViewModels/MallContainerViewModel.swift` | 容器状态、host message、restore 逻辑参考 |
| `ios/ShortDrama/Sources/Features/Mall/Views/MallContainerView.swift` | 容器结构参考 |
| `docs/specs/2026-07-28-prd-13-mall/design-ios.md` | mall 平台设计范式 |
| `docs/specs/2026-07-29-prd-14-earn/design.md` | earn shared contract |
