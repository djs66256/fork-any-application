# iOS 端技术方案：PRD-13 商城

> 创建日期：2026-07-28
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

```text
AppShellView (mall tab)
→ TabNavigationHostView(tab: .mall)
→ MallContainerView
  → MallContainerViewModel
  → MallWebView (WKWebView wrapper)
  → MallContainerStateView (loading / error)

MallWebView JS bridge
→ mall.openSearch(payload)
  → NavigationRouter.openSearch(from: .mall, returnTarget: "/mall")
  → search close/back => router.restoreMallContext(reason: .searchReturn)
→ mall.requestLogin(payload)
  → router.present full-screen mall login route
  → close/dismiss/success => router.restoreMallContext(reason: .loginReturn)
  → router.syncMallAuthState(to: webView)
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | mall tab 从 `PlaceholderTabView` 切为 `MallContainerView` |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 扩展 | 新增 mall login 承接相关 route |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 扩展 | 管理 mall 容器返回与全屏登录承接 |
| `ios/ShortDrama/Sources/Core/Config/AppConfig.swift` | 扩展 | 新增 `mallBaseURL` / `mallHomePath` 等配置读取 |
| `ios/ShortDrama/Sources/Features/Mall/` | 新增 | 商城容器、bridge、登录承接、状态视图 |
| `ios/ShortDrama/Sources/Features/Ranking/` | 不变 | 仅作为登录拦截上下文字段与 effect 模式参考 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | mall tab root 改为 `MallContainerView()` |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增 `mallLogin(context:)` / mall route 公共名称 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 增加 mall 登录承接的 full-screen / return 处理 |
| `ios/ShortDrama/Sources/Core/Config/AppConfig.swift` | 修改 | 新增商城 H5 URL 配置读取 |
| `ios/ShortDrama/Sources/Domain/Entities/MallLoginContext.swift` | 新增 | 商城登录上下文实体 |
| `ios/ShortDrama/Sources/Features/Mall/Models/MallContainerState.swift` | 新增 | mall 容器状态枚举（Presentation/Feature 层模型） |
| `ios/ShortDrama/Sources/Features/Mall/ViewModels/MallContainerViewModel.swift` | 新增 | 容器 loading / error / bridge effect 管理 |
| `ios/ShortDrama/Sources/Features/Mall/Views/MallContainerView.swift` | 新增 | mall tab 根视图 |
| `ios/ShortDrama/Sources/Features/Mall/Views/Components/MallWebView.swift` | 新增 | `UIViewRepresentable` 封装 WKWebView |
| `ios/ShortDrama/Sources/Features/Mall/Views/Components/MallContainerStateView.swift` | 新增 | loading / error 宿主态 |
| `ios/ShortDrama/Sources/Features/Mall/Views/MallLoginPlaceholderView.swift` | 新增 | 统一全屏登录承接占位页 |
| `ios/ShortDrama/Tests/ViewModelTests/MallContainerViewModelTests.swift` | 新增 | 覆盖 bridge effect 与状态流转 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 增补 mall login / mall return 路由测试 |

---

## 3. View 层设计

### 3.1 组件层级树

```text
MallContainerView
├── MallContainerStateView (loading / error only)
├── MallWebView
└── FullScreenCover / Navigation Destination
    └── MallLoginPlaceholderView
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `MallContainerView` | View | mall tab 根视图，装配 ViewModel 与 router | 否 |
| `MallWebView` | View | 承载 WKWebView、注册 script message handler、加载 mall H5 | 否 |
| `MallContainerStateView` | View | H5 首次加载 loading / error UI | 否 |
| `MallLoginPlaceholderView` | View | 统一全屏登录承接占位页 | 否 |

### 3.3 组件接口定义

```swift
struct MallContainerView: View {
    @EnvironmentObject private var router: NavigationRouter
    @StateObject private var viewModel: MallContainerViewModel
}

struct MallWebView: UIViewRepresentable {
    let request: URLRequest
    let bridgeHandler: MallBridgeHandler
    let onPageEvent: (MallWebPageEvent) -> Void
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| 父 → 子 | 构造函数参数 | request、bridge handler、state callbacks |
| 子 → 父 | Closure callback | WKWebView 页面事件、bridge 消息回传 |
| 跨层级共享 | `@EnvironmentObject` `NavigationRouter` | 搜索跳转、登录承接、tab 切换 |

### 3.5 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | WebView 全屏铺满安全区内区域 | 遵循 mall H5 移动布局 |
| Dynamic Type | 宿主态文案使用 `DesignTokens` / 系统字体 | loading / error 态保持可读 |
| 深色模式 | 宿主态支持深色；H5 主题由 Web 自行控制 | Native 不强行覆写 H5 样式 |
| 安全区域 | `MallContainerView` 遵循 Tab 宿主布局 | 不让 H5 自行穿透底部 tab |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 View | 职责 |
|-----------|----------|------|
| `MallContainerViewModel` | `MallContainerView` | 管理容器状态、首屏加载、bridge effect、登录返回恢复 |

### 4.2 状态定义

```swift
@MainActor
final class MallContainerViewModel: ObservableObject {
    @Published private(set) var state: MallContainerState = .loading
    @Published private(set) var currentURL: URL?
    @Published private(set) var pendingLoginContext: MallLoginContext?

    func loadInitialPage() { ... }
    func reload() { ... }
    func handleBridgeMessage(_ message: MallBridgeMessage) { ... }
    func handleLoginCompletion() { ... }
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `state` | `MallContainerState` | `.loading` | 容器级 loading / success / error |
| `currentURL` | `URL?` | `nil` | 当前 mall H5 URL |
| `pendingLoginContext` | `MallLoginContext?` | `nil` | 等待登录承接的上下文 |
| `lastLoadedHomeURL` | `URL?` | `nil` | 成功加载商城首页后的恢复目标 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | View 层表现 |
|---------|---------|-----------|
| Loading | `state == .loading` | `ProgressView` / skeleton 宿主态 |
| Success | `state == .success` | 展示 `MallWebView` |
| Error (可重试) | `state == .error(retryable)` | 错误说明 + 重试按钮 |
| Error (不可重试) | 暂不单独建模 | 首版统一视作可重试 |

---

## 5. Navigation 路由设计

### 5.1 导航方案

- 继续使用现有 `TabView + NavigationStack + NavigationRouter`。
- mall 首页本身不使用 SwiftUI Push 组织页面内容，而是由 `WKWebView` 承载 H5 子路由。
- 登录承接使用 `fullScreenCover` 或等效全屏路由；该页面语义明确为 **mall-owned login handoff**，不复用 `.menuPlaceholder(kind: .login)`。

### 5.2 路由清单

| 路由标识 | 目标页面 | 参数 | 导航方式 | 说明 |
|---------|---------|------|---------|------|
| `.searchHome` | 现有搜索页 | — | Push | mall 搜索 bridge 复用现有 home-owned 搜索页 |
| `.mallLogin(context)` | `MallLoginPlaceholderView` | `MallLoginContext` | FullScreenCover | 商城登录承接 |

### 5.3 路由管理

```swift
enum AppRoute: Hashable {
    case home
    case searchHome
    case mallLogin(context: MallLoginContext)
    // ...existing routes
}
```

- `.searchHome` 继续归属 `.home` tab；从 mall 进入搜索允许临时切换到 home-owned 路由，但 `NavigationRouter` 必须记录 entry context 为 `.mall` 与 `returnTarget=/mall`。
- 搜索关闭 / 回退时统一调用 `restoreMallContext(reason: .searchReturn)`：重新选中 `.mall`，并向 `MallWebView` 发送 `mall.restoreContext(reason='search-return')`；若容器已销毁则重载商城首页。
- `.mallLogin(context:)` 归属 `.mall` 语义，由 `NavigationRouter` 负责在关闭后重新选中 `.mall`。
- 登录成功 / 取消 / 关闭时统一调用 router 的 mall 返回方法，并同步 `mall.syncAuthState` 与 `mall.restoreContext(reason='login-return')`，最低保证回到商城容器首页。

### 5.4 Deep Link 处理（如适用）

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `djsdrama://mall` | `MallContainerView` | 无 |
| `djsdrama://mall/login` | `MallLoginPlaceholderView` | 仅内部使用，不对外公开 |

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| H5 容器加载 | `WKWebView` | 加载 `AppConfig.mallBaseURL + /mall` |
| 原生 API | 无新增商城 API 请求 | 商品列表由 H5 自己调用 Backend |
| 原生配置读取 | `AppConfig` | 读取 mall base URL |
| bridge 通讯 | `WKScriptMessageHandler` | 处理 `mall.openSearch` / `mall.requestLogin` |

### 6.2 API 端点定义

```swift
struct MallLoginContext: Hashable, Sendable {
    let source: String
    let productID: String
    let returnTarget: String
}
```

- iOS 首版不直接请求 `GET /api/mall/products`，因此不新增 `APIEndpoint`。
- 原生只消费 bridge message 与 H5 URL 配置。

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| H5 首次加载失败 | 0（手动重试） | — | 点击“重试”重新加载首页 URL |
| bridge 消息解析失败 | 0 | — | 忽略消息并记录日志 |
| 登录承接返回恢复失败 | 0 | — | 直接重载 mall 首页 |

### 6.4 网络状态监听

- 本期不为 mall 单独做 `NWPathMonitor` 逻辑。
- H5 加载失败的宿主反馈由 `WKNavigationDelegate` 的失败回调驱动。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| mall 容器最近 URL | 内存态 | `MallContainerViewModel` | 会话内有效 | 不做持久化 |
| 登录承接上下文 | 内存态 | `NavigationRouter` / `MallContainerViewModel` | 登录完成即清空 | 不写 UserDefaults |
| H5 自身页面状态 | 由 WebView 内存态持有 | WKWebView | 容器销毁即失效 | 首版不做跨会话恢复 |

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
- 若后续需要恢复 mall scroll position，再评估是否引入轻量 session cache，不在本期实现。

---

## 8. 配置与环境

| 配置项 | 管理方式 | 开发环境值 | 生产环境值 | 说明 |
|--------|---------|----------|-----------|------|
| API Base URL | Info.plist / xcconfig | 现有配置 | 现有配置 | 继续供原生 API 使用 |
| Mall Base URL | Info.plist / xcconfig | 环境注入 | 环境注入 | 用于拼接 `/mall` 首页 |
| App Name | Info.plist | 现有配置 | 现有配置 | 使用 `AppConfig.appName()` |

> ⚠️ 禁止硬编码任何常量。商城 H5 首页 URL 不能写死在 `MallWebView` 或 `MallContainerViewModel` 中。

---

## 9. API 调用清单

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| 无新增原生商城 API | — | — | 商品数据由 H5 自己请求 | iOS 只处理 WebView 加载错误 |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | iOS 端实现方式 |
|---------|---------------|---------------|
| H5 承载首页与详情 | Native 只做容器 | `MallContainerView` + `MallWebView` 承载 mall H5 |
| 搜索 bridge | `mall.openSearch` | `WKScriptMessageHandler` 收到消息后 `router.openSearch(from: .mall, returnTarget: "/mall")` |
| 搜索返回契约 | Native 搜索返回 mall | `restoreMallContext(reason: .searchReturn)`，并向 H5 发送 `mall.restoreContext` |
| 登录 bridge | `mall.requestLogin` | 解析 `MallLoginContext` 后展示 `MallLoginPlaceholderView` 全屏页 |
| 登录态同步 | Native 返回权威登录态 | 初次加载、登录成功/取消、前后台切换后向 H5 发送 `mall.syncAuthState` |
| 登录返回契约 | 返回 `/mall` 且 tab 高亮正确 | 登录关闭时 `restoreMallContext(reason: .loginReturn)`，必要时 reload mall 首页 |
| 容器三态 | loading / success / error | `MallContainerState` 管理宿主 UI |
| 最低恢复保证 | 容器重建后至少回到 mall 首页首屏 | `reloadHome()` 重载首页 URL |

---

## 11. 边界与错误处理

### 11.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| WebView 导航层 | `WKNavigationDelegate` | 捕获页面加载失败 |
| ViewModel | `handleBridgeMessage` / `reload` | 统一处理 bridge 与容器状态 |
| View 层 | 内联错误视图 / 全屏登录页 | 不弹 home 菜单登录 alert |
| 日志 | `os_log` / `print`（开发态） | 记录 bridge 解析失败 |

### 11.2 错误码映射表

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `NETWORK_ERROR` | 商城加载失败，请稍后重试 | 宿主错误页 + 重试 |
| `INTERNAL_ERROR` | 商城暂时不可用 | 宿主错误页 + 重试 |
| `VALIDATION_ERROR` | 页面参数异常 | 忽略当前 bridge 消息，保留商城页 |
| `UNAUTHORIZED` | 请先登录 | 由 H5 先展示拦截层；iOS 只展示全屏登录承接页 |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| H5 首页加载失败 | `didFailProvisionalNavigation` / `didFail` | 展示宿主错误态与重试 | 🔴 |
| bridge payload 缺字段 | 非法 `MallLoginContext` / `MallSearchContext` | 忽略消息并记录日志 | 🔴 |
| Native 未同步登录态 | 首次加载或登录返回后未发送 `mall.syncAuthState` | 默认按匿名处理，并在下次页面激活时补发同步 | 🔴 |
| 搜索跳转后 tab 切到 home | `.searchHome` 归属 `.home` | 允许临时切 tab，但关闭搜索时必须 `restoreMallContext(reason: .searchReturn)` | 🔴 |
| 登录承接被关闭 | 用户取消 / 关闭 | 回到 mall tab，尽量保留现有容器，并发送 `mall.restoreContext(reason='login-return')` | 🔴 |
| 容器被系统回收 | 后台回来 / 登录页后重建 | 重新加载 mall 首页首屏，并重新发送登录态同步 | 🟡 |
| 多次快速点击登录 | 重复收到 `mall.requestLogin` | 若已有 `pendingLoginContext` 则忽略后续事件 | 🟡 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `MallContainerView` | `ProgressView` | `MallWebView` | — | 错误说明 + 重试 | — |
| `MallLoginPlaceholderView` | 轻量加载 | 显示占位登录承接 | — | 关闭返回商城 | — |

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | `MallContainerViewModel` 状态机、bridge effect | 核心逻辑覆盖 | Swift Testing |
| Router 测试 | `NavigationRouter` mall login / return 逻辑 | 核心导航覆盖 | Swift Testing |
| 解析测试 | `MallLoginContext` payload decode / validate | 合法/非法输入覆盖 | Swift Testing |

### 12.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| IOS-MALL-01 | 首页首次加载 | mall URL 有效 | `loadInitialPage()` | 状态从 loading 到 success | 单元 |
| IOS-MALL-02 | 首页加载失败 | webview 回调失败 | `handlePageLoadFailed()` | 状态进入 error | 单元 |
| IOS-MALL-03 | 搜索 bridge | 收到 `mall.openSearch` | 处理 bridge | 调用 `router.navigate(.searchHome)` | 单元 |
| IOS-MALL-04 | 登录 bridge | 收到合法 `MallLoginContext` | 处理 bridge | 展示全屏登录承接 | 单元 |
| IOS-MALL-05 | 登录取消返回 | mall login 关闭 | `handleLoginCompletion()` | 重新选中 `.mall` | Router |
| IOS-MALL-06 | 非法 payload | `productId` 为空 | 处理 bridge | 忽略消息，不崩溃 | 单元 |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| `NavigationRouter` | Mock / test double | 验证导航 effect |
| WebView 页面事件 | 通过 ViewModel 回调模拟 | 不依赖真实 WKWebView |
| 配置读取 | 自定义 `Bundle` / AppConfig wrapper | 避免真实 Info.plist 依赖 |

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
| 继续复用 `.menuPlaceholder(kind: .login)` 破坏 mall 登录语义 | iOS / 产品语义 | 🔴 | 中 | 独立新增 `.mallLogin(context:)` 路由与页面 | 临时 mall 专属占位登录页 |
| WebView bridge 直接耦合 WebKit 细节到 View | iOS | 🟡 | 中 | 抽离 `MallWebView` + ViewModel 回调 | 先保留最小 bridge 封装 |
| 搜索跳转切到 home tab 造成体验不一致 | iOS | 🟡 | 中 | 文档明确允许临时切 tab，最低保证返回 mall 首页 | 失败时停留 mall 并提示 |
| mall H5 地址被硬编码 | iOS / 环境切换 | 🔴 | 中 | 使用 `Info.plist + xcconfig + AppConfig` | 无 |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/features/app-shell/index.md` | iOS、已知限制 | mall 仍为 placeholder，需切换为真实容器 |
| `wiki/features/search-discovery/index.md` | iOS 入口与路由 | 搜索页归属 home-owned 路由，可被 mall 复用 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `ios/CLAUDE.md` | iOS 架构、配置、NavigationStack 约束 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 当前 mall tab 仍为占位实现 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 现有路由归属与 public route name |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | Tab/path 状态管理模式 |
| `ios/ShortDrama/Sources/Core/Config/AppConfig.swift` | Info.plist 配置读取入口 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift` | 现有登录拦截仅为 alert |
| `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift` | `RankingLoginContext` 字段粒度参考 |
| `docs/specs/2026-07-28-prd-13-mall/design.md` | mall shared contract |
