# iOS 端技术方案：PRD-01 底部导航与应用路由

> 创建日期：2026-07-25
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

本期在现有 iOS 单一 `NavigationStack + HomeView` 应用壳之上，演进为“`TabView` 承载 5 个一级频道 + 每个 Tab 独立 `NavigationStack`”的导航骨架。实现重点放在 App 层导航状态管理，不改动 Domain/Data 层契约，不新增后端 API。

```text
┌──────────────────────────────────────────────────────────┐
│ App Layer                                                │
│ ├── ShortDramaApp                                        │
│ │   └── AppShellView                                     │
│ │       └── TabView(selection: $router.selectedTab)      │
│ │           ├── TabNavigationHostView(.home)             │
│ │           │   └── NavigationStack(path: homePath)      │
│ │           │       └── HomeView                         │
│ │           │           ├── 现有首页内容                  │
│ │           │           └── 路由测试入口区                │
│ │           ├── TabNavigationHostView(.theater)          │
│ │           │   └── NavigationStack(path: theaterPath)   │
│ │           │       └── PlaceholderTabView               │
│ │           ├── TabNavigationHostView(.mall)             │
│ │           ├── TabNavigationHostView(.earn)             │
│ │           └── TabNavigationHostView(.profile)          │
│ └── NavigationRouter                                     │
│     ├── selectedTab                                      │
│     ├── pathsByTab                                       │
│     ├── pendingRoute                                     │
│     └── containerReady                                   │
├──────────────────────────────────────────────────────────┤
│ Feature Presentation Layer                               │
│ ├── HomeView / HomeViewModel                             │
│ ├── PlayerView / PlayerViewModel                         │
│ ├── DramaDetailView / DramaDetailViewModel               │
│ └── PlaceholderTabView（剧场/商城/赚钱/我的占位）         │
├──────────────────────────────────────────────────────────┤
│ Domain / Data / Core                                     │
│ ├── 现有 FetchDramasUseCase                              │
│ ├── 现有 DramaRepository                                 │
│ ├── 现有 APIClient / APIEndpoint / APIError              │
│ └── 本期不新增 API、缓存服务或持久化服务                 │
└──────────────────────────────────────────────────────────┘
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `ios/ShortDrama/Sources/App/ShortDramaApp.swift` | 扩展 | 从单一 `NavigationStack` 入口改为挂载 `AppShellView`，并把 deeplink 分发给新的 `NavigationRouter` |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 扩展 | 从单一路径 `path` 升级为 Tab 选中态、每个 Tab 独立 `NavigationPath`、pending deeplink 缓存 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 扩展 | 保留端内 `player` / `dramaDetail` 语义，同时补齐与公开命名 `play` / `detail` 的映射信息 |
| `ios/ShortDrama/Sources/App/DeeplinkHandler.swift` | 扩展 | 继续支持 `open` / `play` / `drama`，并输出统一的端内路由目标 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 扩展 | 保留现有首页骨架与 `HomeViewModel`，增加本期用于进入播放页、详情页的占位入口 |
| `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` | 不变 | 继续作为播放页占位视图，由导航骨架承载 |
| `ios/ShortDrama/Sources/Features/DramaDetail/Views/DramaDetailView.swift` | 不变 | 继续作为详情页占位视图，由导航骨架承载 |
| Domain / Data / Core 网络层 | 不变 | 本期为导航骨架设计，不新增领域模型、仓库协议和 RESTful 接口 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `ios/ShortDrama/Sources/App/AppTab.swift` | 新增 | 定义 5 个 Tab 的内部标识、显示文案、图标 token、公开路径语义 |
| `ios/ShortDrama/Sources/App/AppShellView.swift` | 新增 | `TabView` 根容器，负责挂载 5 个 Tab 与首次 ready 回调 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 新增 | 单个 Tab 的 `NavigationStack` 容器，统一注册 `navigationDestination` |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 在现有 `home` / `player(videoId:)` / `dramaDetail(dramaId:)` 基础上补齐 `owningTab`、`publicRouteName` 等映射属性 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 改造为多 Tab 独立路径、deeplink 待执行缓存、状态恢复兜底入口 |
| `ios/ShortDrama/Sources/App/DeeplinkHandler.swift` | 修改 | 把 `djsdrama://open` / `play/{id}` / `drama/{id}` 解析为统一 `AppRoute`，并对空参数做拦截 |
| `ios/ShortDrama/Sources/App/ShortDramaApp.swift` | 修改 | 注入新的 `NavigationRouter`，在 `onOpenURL` 时先入队 deeplink，再等待容器 ready 执行 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 增加首页内测试入口区，验证 `/play/:id` 和 `/detail/:id` 路由 |
| `ios/ShortDrama/Sources/Features/Shell/Views/PlaceholderTabView.swift` | 新增 | 剧场 / 商城 / 赚钱 / 我的统一占位页，避免为每个 Tab 重复建 View |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 补齐多 Tab 路径隔离、deeplink pending、恢复兜底测试 |
| `ios/ShortDrama/Tests/ViewModelTests/DeeplinkHandlerTests.swift` | 修改 | 补齐公开命名/端内语义映射、非法参数、未知 host 测试 |

---

## 3. View 层设计

### 3.1 组件层级树

```text
ShortDramaApp
└── AppShellView
    └── TabView
        ├── TabNavigationHostView(tab: .home)
        │   └── NavigationStack(path: router.pathBinding(for: .home))
        │       └── HomeView
        │           ├── BrandHeader
        │           ├── LoadingOrErrorSection
        │           └── HomeRouteEntrySection
        │               ├── PlayPlaceholderEntryButton
        │               └── DetailPlaceholderEntryButton
        ├── TabNavigationHostView(tab: .theater)
        │   └── NavigationStack(path: router.pathBinding(for: .theater))
        │       └── PlaceholderTabView
        ├── TabNavigationHostView(tab: .mall)
        │   └── NavigationStack(path: router.pathBinding(for: .mall))
        │       └── PlaceholderTabView
        ├── TabNavigationHostView(tab: .earn)
        │   └── NavigationStack(path: router.pathBinding(for: .earn))
        │       └── PlaceholderTabView
        └── TabNavigationHostView(tab: .profile)
            └── NavigationStack(path: router.pathBinding(for: .profile))
                └── PlaceholderTabView

NavigationDestination(AppRoute)
├── .player(videoId:)     -> PlayerView
└── .dramaDetail(dramaId:)-> DramaDetailView
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `AppShellView` | View | 作为 iOS 应用主容器，持有 `TabView`，固定 5 个 Tab 顺序与选中态 | 否 |
| `TabNavigationHostView` | View | 为单个 Tab 提供独立 `NavigationStack`，绑定对应 `NavigationPath` | 是 |
| `HomeView` | View | 继续承载首页现有骨架，并增加进入播放页/详情页的测试入口 | 否 |
| `PlaceholderTabView` | View | 统一渲染剧场/商城/赚钱/我的占位内容，避免 4 份重复页面 | 是 |
| `PlayerView` | View | 显示播放页占位与视频 ID | 否 |
| `DramaDetailView` | View | 显示详情页占位与剧集 ID | 否 |

### 3.3 组件接口定义

```swift
struct AppShellView: View {
    @EnvironmentObject private var router: NavigationRouter

    var body: some View {
        TabView(selection: $router.selectedTab) {
            ForEach(AppTab.allCases) { tab in
                TabNavigationHostView(tab: tab)
                    .tabItem { Label(tab.title, systemImage: tab.systemImage) }
                    .tag(tab)
            }
        }
        .task {
            router.markContainerReady()
        }
    }
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| `ShortDramaApp` → `AppShellView` / 各功能页 | `@EnvironmentObject` | 共享 `NavigationRouter`，统一管理选中 Tab、独立导航栈和 deeplink 状态 |
| `TabNavigationHostView` → 根页面 | 构造参数 `tab: AppTab` | 确定每个 Tab 对应的根视图和展示信息 |
| `HomeView` → `PlayerView` / `DramaDetailView` | `router.navigate(to:)` | 首页内点击占位入口时推入二级页面 |
| `PlayerView` / `DramaDetailView` | 构造函数参数 | 通过已有 `PlayerViewModel(videoId:)`、`DramaDetailViewModel(dramaId:)` 接收路由参数 |
| 占位页内容 | 值类型配置 | `PlaceholderTabView(tab: AppTab)` 根据 Tab 枚举派生标题、副标题和占位说明 |

### 3.5 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | 使用 `TabView` + `NavigationStack` 的系统容器 | iPhone / iPad 下都保持系统级导航行为，不自行拼接底栏布局 |
| Dynamic Type | 文本使用系统字体语义（`.title` / `.body` / `.caption`） | 占位页与首页入口区避免固定高度，允许内容自然换行 |
| 深色模式 | 沿用现有 SwiftUI 语义色与 `DesignTokens` | 不引入额外主题配置，确保选中态和占位文案在明暗模式均可读 |
| 安全区域 | 使用系统 `TabView` / `NavigationStack` 安全区处理 | 不手动覆盖底部安全区，避免 Home indicator 区域冲突 |
| 横竖屏切换 | 依赖 SwiftUI 自适应布局 | 频道标题区与按钮区使用垂直布局，避免横屏时底栏挤压 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 View | 职责 |
|-----------|----------|------|
| `NavigationRouter` | `AppShellView` / `TabNavigationHostView` | 管理一级 Tab 选中态、每个 Tab 的 `NavigationPath`、deeplink 待执行队列、恢复兜底 |
| `HomeViewModel` | `HomeView` | 复用现有首页加载状态与应用信息展示；本期不新增网络职责 |
| `PlayerViewModel` | `PlayerView` | 持有 `videoId`，为播放页占位提供参数展示 |
| `DramaDetailViewModel` | `DramaDetailView` | 持有 `dramaId`，为详情页占位提供参数展示 |

### 4.2 状态定义

```swift
@MainActor
final class NavigationRouter: ObservableObject {
    @Published var selectedTab: AppTab = .home
    @Published private(set) var pathsByTab: [AppTab: NavigationPath]
    @Published private(set) var pendingRoute: AppRoute?
    @Published private(set) var containerReady = false

    func pathBinding(for tab: AppTab) -> Binding<NavigationPath> { ... }
    func select(tab: AppTab) { ... }
    func navigate(to route: AppRoute) { ... }
    func enqueueDeepLink(_ route: AppRoute) { ... }
    func markContainerReady() { ... }
    func dismiss(in tab: AppTab? = nil) { ... }
    func popToRoot(of tab: AppTab) { ... }
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `selectedTab` | `AppTab` | `.home` | 冷启动默认首页，符合共享方案默认落地规则 |
| `pathsByTab` | `[AppTab: NavigationPath]` | 5 个空栈 | 每个 Tab 独立维护自己的导航栈，切换 Tab 时不互相污染 |
| `pendingRoute` | `AppRoute?` | `nil` | 容器未 ready 时缓存 1 个待执行 deeplink，采用“最后一次覆盖前一次”策略 |
| `containerReady` | `Bool` | `false` | `AppShellView` 首次完成挂载后置为 `true`，再执行待处理 deeplink |
| `HomeViewModel.isLoading` | `Bool` | `false` | 沿用现有首页加载态 |
| `HomeViewModel.errorMessage` | `String?` | `nil` | 沿用现有首页错误态 |
| `PlayerViewModel.videoId` | `String` | 构造注入 | 播放页占位参数 |
| `DramaDetailViewModel.dramaId` | `String` | 构造注入 | 详情页占位参数 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | View 层表现 |
|---------|---------|-----------|
| AppShell 初始化中 | `containerReady == false` | 直接渲染 `TabView`，但暂不执行待处理 deeplink |
| AppShell 就绪 | `containerReady == true` | 允许响应端内导航和 flush pending deeplink |
| Deeplink 待执行 | `pendingRoute != nil && containerReady == false` | 不弹框，先缓存路由，容器 ready 后一次性执行 |
| 首页 Loading | `HomeViewModel.isLoading == true` | 继续显示现有 `ProgressView` |
| 首页 Success | `isLoading == false && errorMessage == nil` | 展示现有首页内容 + 路由测试入口区 |
| 首页 Error | `errorMessage != nil` | 沿用现有错误文案展示 |
| 占位 Tab | 频道根页已加载 | 显示统一占位页，说明频道名称与“后续 PRD 接入” |
| 子页面 Success | 路由参数合法 | `PlayerView` / `DramaDetailView` 显示 ID 与返回能力 |
| 子页面非法参数 | 路由参数为空或无效 | 不进入目标页，回退首页根页或保持当前根页 |

---

## 5. Navigation 路由设计

### 5.1 导航方案

采用 `TabView + 每个 Tab 独立 NavigationStack + NavigationPath`。

设计原因：

1. `TabView` 能直接表达 5 个一级频道固定结构。
2. 每个 Tab 使用独立 `NavigationPath`，天然满足“切换频道不串栈”的需求。
3. `NavigationRouter` 统一持有选中态和路径，便于 deeplink 在冷启动、前台再次打开、Tab 切换三种场景下复用同一套逻辑。
4. 不引入 UIKit coordinator、第三方导航库或新的开源依赖，符合当前 iOS 约束。

### 5.2 路由清单

| 路由标识 | 公开命名 | 目标页面 | 参数 | 导航方式 | 说明 |
|---------|---------|---------|------|---------|------|
| `AppTab.home` | `home` | 首页根页 `HomeView` | 无 | Tab 切换 | 默认落地 Tab |
| `AppTab.theater` | `theater` | 剧场占位页 | 无 | Tab 切换 | 一级频道，占位承载 |
| `AppTab.mall` | `mall` | 商城占位页 | 无 | Tab 切换 | 一级频道，占位承载 |
| `AppTab.earn` | `earn` | 赚钱占位页 | 无 | Tab 切换 | 一级频道，占位承载 |
| `AppTab.profile` | `profile` | 我的占位页 | 无 | Tab 切换 | 一级频道，占位承载 |
| `AppRoute.home` | `home` | 首页语义目标 | 无 | 语义路由 | 用于 deeplink `open` 和回首页兜底，不入栈 |
| `AppRoute.player(videoId:)` | `play/:id` | `PlayerView` | `videoId` | Push | 公开命名统一为 `play`，端内保留 `player` 语义 |
| `AppRoute.dramaDetail(dramaId:)` | `detail/:id` | `DramaDetailView` | `dramaId` | Push | 公开命名统一为 `detail`，端内保留 `dramaDetail` 语义 |

### 5.3 路由管理

```swift
enum AppTab: String, CaseIterable, Hashable, Identifiable {
    case home, theater, mall, earn, profile

    var id: String { rawValue }
    var title: String { ... }
    var systemImage: String { ... }
}

enum AppRoute: Hashable {
    case home
    case player(videoId: String)
    case dramaDetail(dramaId: String)

    var owningTab: AppTab {
        switch self {
        case .home, .player, .dramaDetail:
            return .home
        }
    }

    var publicRouteName: String {
        switch self {
        case .home:
            return "home"
        case .player:
            return "play"
        case .dramaDetail:
            return "detail"
        }
    }
}

@MainActor
final class NavigationRouter: ObservableObject {
    @Published var selectedTab: AppTab = .home
    @Published private(set) var pathsByTab = AppTab.allCases.reduce(into: [AppTab: NavigationPath]()) {
        $0[$1] = NavigationPath()
    }
    private(set) var pendingRoute: AppRoute?
    private(set) var containerReady = false

    func navigate(to route: AppRoute) {
        let tab = route.owningTab
        selectedTab = tab

        switch route {
        case .home:
            popToRoot(of: .home)
        case .player, .dramaDetail:
            pathsByTab[tab, default: NavigationPath()].append(route)
        }
    }
}
```

#### 公开命名与端内语义映射

| 对外语义 | iOS 端内语义 | 使用位置 | 说明 |
|---------|-------------|---------|------|
| `home` | `AppTab.home` / `AppRoute.home` | Tab 选中、deeplink `open` | 首页既是一级频道也是回退兜底目标 |
| `play/:id` | `AppRoute.player(videoId:)` | 文档、deeplink、测试命名 | 对外统一 `play`，iOS 内部延续现有 `player` case，避免大范围重命名 |
| `detail/:id` | `AppRoute.dramaDetail(dramaId:)` | 文档、测试命名、页面语义 | 对外统一 `detail`，iOS 内部延续现有 `dramaDetail` case |
| `theater` / `mall` / `earn` / `profile` | `AppTab` 同名 case | Tab 标识 | 一级频道仅用作 Tab 根页，不进入二级路由枚举 |

### 5.4 Deep Link 处理（如适用）

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `djsdrama://open` | `AppRoute.home` | 无 |
| `djsdrama://play/{id}` | `AppRoute.player(videoId:)` | `id` |
| `djsdrama://drama/{id}` | `AppRoute.dramaDetail(dramaId:)` | `id` |
| 非法 scheme / 未知 host / 空参数 | 回退首页或忽略 | 不进入二级页面 |

#### Deeplink 缓存待执行策略

1. `ShortDramaApp.onOpenURL` 收到 URL 后，不直接操作某个具体 `NavigationStack`。
2. `DeeplinkHandler` 先解析 URL 并完成参数校验：
   - `open` 直接映射为 `.home`
   - `play/{id}` 映射为 `.player(videoId:)`
   - `drama/{id}` 映射为 `.dramaDetail(dramaId:)`
   - 空 `id` / 未知 host / 非 `djsdrama` scheme 视为非法输入
3. 若 `router.containerReady == false`，则调用 `router.enqueueDeepLink(route)` 暂存到 `pendingRoute`。
4. `AppShellView.task` 首次执行时调用 `router.markContainerReady()`：
   - 标记容器 ready
   - 读取 `pendingRoute`
   - 按 owning tab 切换到目标 Tab
   - 执行一次 `navigate(to:)`
   - 清空 `pendingRoute`
5. 若 App 已在前台且容器已 ready，则 deeplink 直接导航，不做缓存。
6. 多个 deeplink 在 ready 前同时到达时，以最后一次为准，避免重复 push。

#### 状态保持与恢复策略

- **Tab 切换保持**：`pathsByTab` 常驻 `NavigationRouter`，5 个 `NavigationStack` 始终挂载在 `TabView` 下，切换 Tab 不销毁其他 Tab 的导航栈。
- **二级页面恢复**：用户从首页 push 到播放页/详情页后切换到其他 Tab，再切回首页时，首页栈仍停留在离开前的页面。
- **局部 UI 状态保持**：依赖根视图实例保活；例如 Home 后续演进为列表时，滚动状态可随 Home 根视图实例一并保留。本期不额外新增滚动位置持久化模型。
- **系统回收降级**：若 App 被系统杀死或部分状态不可恢复，本期不新增磁盘级恢复；冷启动统一回到首页根页，符合共享方案兜底要求。
- **手动恢复入口**：`popToRoot(of:)` 作为状态损坏时的统一恢复动作，只回收到目标 Tab 根页，不影响其他 Tab。

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | 复用现有 `APIClient`（`URLSession`） | 本期不涉及/不新增导航相关网络请求 |
| 请求构建 | 复用现有 `APIEndpoint` | 本期不涉及/不新增 endpoint |
| 请求拦截器 | 不新增 | 本期不涉及 token 注入、埋点或导航专属拦截 |
| 响应解析 | 复用现有 `Codable + JSONDecoder` | 本期不涉及/不新增响应模型 |
| 错误处理 | 复用现有 `APIError` | 仅首页既有数据加载继续使用，非本 PRD 新增范围 |

### 6.2 API 端点定义

本期不涉及/不新增 API 端点。`design.md` 已明确本需求为纯客户端导航骨架设计，iOS 端不新增 RESTful 调用。

### 6.3 请求重试策略

本期不涉及/不新增请求重试策略。导航相关逻辑均在本地内存态完成。

### 6.4 网络状态监听

本期不涉及/不新增 `NWPathMonitor` 等网络状态监听能力。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| 当前选中 Tab | 仅内存态 | `NavigationRouter.selectedTab` | App 进程结束即失效 | 本期不新增 UserDefaults 持久化 |
| 各 Tab 导航栈 | 仅内存态 | `NavigationRouter.pathsByTab` | App 进程结束即失效 | 满足 Tab 切换保持；系统回收后允许降级 |
| 待执行 deeplink | 仅内存态 | `NavigationRouter.pendingRoute` | 容器 ready 后立即消费 | 用于冷启动短暂缓存，不落盘 |
| 用户数据 / 登录态 / Token | 不涉及 | — | — | 本期不涉及/不新增 |
| 离线页面缓存 | 不涉及 | — | — | 本期不涉及/不新增 |

### 7.2 CoreData 模型设计（如适用）

本期不涉及/不新增 CoreData 模型。

### 7.3 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| Tab 导航状态 | 进程内保活 | App 生命周期内 | 系统杀进程后整体失效，重新回首页 |
| Pending deeplink | 单条内存缓存 | 直到容器 ready | 执行后清空；若新 deeplink 到达则覆盖旧值 |

### 7.4 数据迁移策略

本期不涉及/不新增数据迁移。原因：

1. 不新增本地数据库或 UserDefaults key。
2. 不修改已有持久化结构。
3. 状态恢复仅基于内存态，不做磁盘兼容问题处理。

---

## 8. 配置与环境

| 配置项 | 管理方式 | 开发环境值 | 生产环境值 | 说明 |
|--------|---------|----------|-----------|------|
| Deeplink scheme | `ios/project.yml` → Info.plist | 沿用现有配置 | 沿用现有配置 | 本期不新增 scheme，继续使用 `djsdrama://` |
| App 名称 / 版本 | `AppConfig` 读取 Info.plist | 沿用现有配置 | 沿用现有配置 | 导航页标题与文案仍通过现有配置获取 |
| API Base URL | xcconfig + Info.plist | 沿用现有配置 | 沿用现有配置 | 本期不涉及/不新增网络配置 |
| Tab 元数据 | `AppTab` 枚举集中定义 | N/A | N/A | 文案、图标语义、顺序集中管理，避免散落硬编码 |

> 说明：本期不新增环境变量、Feature Flag、API Key 或持久化配置。涉及导航命名的字符串常量统一收敛在 `AppTab` / `AppRoute` 中，不分散写在各页面内。

---

## 9. API 调用清单

本期不涉及/不新增 API 调用。

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| 无 | — | — | — | — |

补充说明：`HomeView` 当前既有的数据加载能力保持原样，但不属于本 PRD 新增能力，也不作为底部导航方案的一部分扩展。

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | iOS 端实现方式 |
|---------|---------------|---------------|
| 底部 5 Tab 定义 | 首页 / 剧场 / 商城 / 赚钱 / 我的，顺序固定 | 新增 `AppTab` 枚举，`TabView` 按 `AppTab.allCases` 固定顺序渲染 |
| 默认落地规则 | 冷启动进入 `home` Tab | `NavigationRouter.selectedTab` 初始值为 `.home` |
| 二级路由归属 | `play/:id`、`detail/:id` 归属首页频道容器 | `AppRoute.owningTab` 对 `.player` / `.dramaDetail` 都返回 `.home` |
| 公开路由命名 | 对外统一 `play`、`detail` | `AppRoute.publicRouteName` 统一输出公开语义，端内仍保留 `player` / `dramaDetail` |
| deeplink 兼容 | iOS 保持 `open` / `play` / `drama` 兼容 | `DeeplinkHandler` 继续接受现有 host，并映射到统一 `AppRoute` |
| deeplink 兜底 | 非法 deeplink 回首页或忽略，不崩溃 | scheme/host/参数校验失败时返回 `nil` 或执行 `navigate(.home)` 兜底 |
| 状态保持 | 切换 Tab 保留独立导航栈和局部状态 | 5 个 Tab 各自绑定独立 `NavigationPath`，容器不销毁 |
| 占位页策略 | 各频道与子页面都提供占位内容 | 非首页一级频道使用 `PlaceholderTabView`，播放页/详情页继续复用现有占位页 |
| 系统回收降级 | 允许资源回收后回根页 | 不做磁盘恢复；进程重启后回首页根页 |

---

## 11. 边界与错误处理

### 11.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| Deeplink 解析层 | `DeeplinkHandler` 白名单校验 | 只接受 `djsdrama` scheme 和已支持 host，空参数直接拦截 |
| 路由状态层 | `NavigationRouter` 串行处理导航动作 | 统一在主线程修改 `selectedTab` 和 `pathsByTab`，避免跨 Tab 串栈 |
| View 层 | 留在当前页或回到根页 | 对非法参数不进入子页面，不弹系统级错误框 |
| 日志 | 原生 `Logger` 预留接入点 | 本期不新增监控 SDK；必要时记录 debug 级别导航错误 |

### 11.2 错误码映射表

| 端内错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `INVALID_ROUTE_PARAMS` | 页面参数无效 | 阻止 push；若来自 deeplink，则回首页根页 |
| `UNSUPPORTED_ROUTE` | 暂不支持该页面 | 忽略或回首页；不弹模态框 |
| `NAVIGATION_STATE_LOST` | 已返回首页 | 执行 `popToRoot(of: .home)` |
| `TAB_STATE_RESTORED_PARTIALLY` | 页面已重新加载 | 保留当前选中 Tab，其余 Tab 重置空栈 |
| `DEEPLINK_CONTAINER_NOT_READY` | 正在打开页面 | 写入 `pendingRoute`，容器 ready 后自动执行 |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| 冷启动立即收到 deeplink | `onOpenURL` 早于 `TabView` 稳定挂载 | 先缓存 `pendingRoute`，容器 ready 后再执行 | 🔴 |
| 用户在播放页/详情页切换 Tab | 首页栈存在二级页面 | 切换时不清空首页 `NavigationPath`，切回首页继续停留原子页面 | 🔴 |
| 快速连续点击多个 Tab | 200ms 内多次切换 | 以最后一次 `selectedTab` 为准，不修改其他 Tab 栈 | 🟡 |
| 非法 deeplink 参数 | `play/`、`drama/` 等空 ID | 不进入目标页，回首页根页 | 🔴 |
| 状态部分丢失 | 系统回收部分视图树 | 允许只恢复当前 Tab，其余 Tab 回根页 | 🟡 |
| App 被彻底杀死后重启 | 无内存态可恢复 | 重新进入首页根页；不做持久化恢复 | 🟡 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `AppShellView` | 容器 ready 前不阻塞显示 | 5 Tab 正常渲染 | 不适用 | 不适用 | pending deeplink 失败时回首页 |
| `HomeView` | 复用现有 `ProgressView` | 展示首页骨架和路由入口 | 本期不单独设计 | 复用现有错误文案 | 非法路由参数不跳转 |
| `PlaceholderTabView` | 不适用 | 展示频道占位说明 | 不适用 | 不适用 | 不适用 |
| `PlayerView` | 不适用 | 展示视频 ID 占位页 | 不适用 | 不适用 | 参数非法时不进入该页 |
| `DramaDetailView` | 不适用 | 展示剧集 ID 占位页 | 不适用 | 不适用 | 参数非法时不进入该页 |

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | `NavigationRouter` 多 Tab 状态、deeplink pending、恢复兜底 | 核心分支全覆盖 | Swift Testing |
| 单元测试 | `DeeplinkHandler` 路由解析、公开命名映射、非法输入 | 核心分支全覆盖 | Swift Testing |
| 单元测试 | `HomeViewModel` 既有加载态不回归 | 维持现有覆盖 | Swift Testing |
| 单元测试 | `PlayerViewModel` / `DramaDetailViewModel` 参数注入 | 基础覆盖 | Swift Testing |

> 说明：本期不新增快照测试框架、不新增 XCUITest 工程，也不引入第三方测试依赖。

### 12.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| T-01 | 冷启动默认首页 Tab | 新建 `NavigationRouter` | 不执行任何操作 | `selectedTab == .home`，5 个 Tab 路径均为空 | 单元 |
| T-02 | 首页进入播放页 | Router 位于首页 | `navigate(.player(videoId: "123"))` | `selectedTab == .home`，首页路径数量 +1 | 单元 |
| T-03 | 首页进入详情页 | Router 位于首页 | `navigate(.dramaDetail(dramaId: "456"))` | 首页路径数量 +1，目标为详情页 | 单元 |
| T-04 | 跨 Tab 独立栈保持 | 首页已 push 播放页，当前切到商城 | 再切回首页 | 首页栈仍保留播放页 | 单元 |
| T-05 | 回首页根页 | 首页栈已有多层 | `navigate(.home)` 或 `popToRoot(of: .home)` | 首页路径清空，仍停留首页 Tab | 单元 |
| T-06 | Deeplink 冷启动待执行 | `containerReady == false` | `enqueueDeepLink(.player(videoId: "123"))` 后 `markContainerReady()` | 自动切到首页 Tab 并进入播放页 | 单元 |
| T-07 | 非法 Deeplink scheme | 输入 `http://...` | 解析 | 返回 `nil` | 单元 |
| T-08 | 非法 Deeplink 空参数 | 输入 `djsdrama://play` | 解析 | 返回 `nil` 或明确非法，不产生空 ID 导航 | 单元 |
| T-09 | `drama` host 映射详情语义 | 输入 `djsdrama://drama/d456` | 解析 | 返回 `.dramaDetail(dramaId: "d456")`，公开语义为 `detail` | 单元 |
| T-10 | 重复 deeplink 覆盖策略 | ready 前连续写入两个 deeplink | `markContainerReady()` | 仅执行最后一次 deeplink | 单元 |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| `NavigationRouter` | 直接实例化真实对象 | 该对象本身为纯内存态，适合直接单测 |
| Deeplink 输入 | 构造 `URL` | 覆盖 `open`、`play`、`drama`、非法 scheme、空参数 |
| `FetchDramasUseCase` | 复用现有 mock repository / stub use case | 仅验证首页既有状态不受导航改造影响 |
| 网络请求 | 不新增 | 本 PRD 不新增网络相关测试 |
| 持久化 | 不新增 | 本 PRD 不涉及本地持久化 |

---

## 13. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | — | — | 本期不新增开源依赖，完全基于 SwiftUI、Swift Testing 和现有项目结构实现 |

---

## 14. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 多 Tab `NavigationPath` 绑定实现不当，导致串栈 | 首页/剧场/商城等所有导航场景 | 🔴 | 中 | `pathsByTab` 集中管理，并为每个 Tab 单独写单测验证 | 先回退为首页单栈可用，其他 Tab 仅保留根页 |
| `TabView` 重建导致首页状态丢失 | Tab 切换恢复体验 | 🔴 | 中 | 保持 5 个 `TabNavigationHostView` 恒定挂载，不做条件渲染 | 状态丢失时回目标 Tab 根页 |
| 冷启动 deeplink 直接 push 触发时机错误 | 外部唤起流程 | 🔴 | 中 | 采用 `pendingRoute + markContainerReady()` 两阶段执行 | 解析失败统一回首页 |
| 首页已有异步加载与导航容器初始化竞争 | 首页首屏体验 | 🟡 | 中 | `HomeViewModel` 继续用 `StateObject`，避免 Tab 切换重建；导航层不感知网络状态 | 若异常则保留首页骨架，导航功能先可用 |
| 公开命名与端内语义不一致造成测试歧义 | deeplink / 文档 / 后续 PRD 对齐 | 🟡 | 中 | 在 `AppRoute` 中集中维护 `publicRouteName`，文档和测试均以该映射为准 | 若实现阶段出现歧义，以共享方案 `design.md` 为准修正 |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/index.md` | 功能索引 | 确认 wiki 组织方式与功能入口 |
| `wiki/architecture/overview.md` | 跨端涉及 / 技术栈总览 | 确认 iOS 使用 SwiftUI、XcodeGen，当前为应用骨架阶段 |
| `wiki/features/app-shell/index.md` | 入口与路由 / 已知限制 | 确认 iOS 当前仅有单页骨架，无 `TabView` 或独立导航栈 |
| `wiki/features/deeplink/index.md` | Deeplink 格式 / 多端实现 | 确认 iOS 已支持 `open` / `play` / `drama`，需在新容器下保持兼容 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `docs/specs/2026-07-25-prd-01-bottom-nav/spec.md` | 明确 5 Tab、状态保持、deeplink 兼容与不新增后端 API |
| `docs/specs/2026-07-25-prd-01-bottom-nav/design.md` | 明确公开命名 `play` / `detail`、Tab 独立栈、pending deeplink、错误码语义 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 当前 iOS 端内已有 `home`、`player(videoId:)`、`dramaDetail(dramaId:)` |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 当前仅单一路径 `path`，需升级为多 Tab 独立状态 |
| `ios/ShortDrama/Sources/App/DeeplinkHandler.swift` | 当前解析 `open` / `play` / `drama`，需补齐参数校验与 pending 策略 |
| `ios/ShortDrama/Sources/App/ShortDramaApp.swift` | 当前直接在 `onOpenURL` 中 push，需改为容器 ready 后执行 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 当前首页仅展示应用信息和加载状态，需增加二级路由测试入口 |
| `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` | 现有播放页占位可直接复用 |
| `ios/ShortDrama/Sources/Features/DramaDetail/Views/DramaDetailView.swift` | 现有详情页占位可直接复用 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 当前只覆盖单栈导航，需补齐多 Tab 与恢复场景 |
| `ios/ShortDrama/Tests/ViewModelTests/DeeplinkHandlerTests.swift` | 当前允许空 ID，需要与本方案的参数校验策略对齐 |
| `.claude/skills/feature-workflow/assets/design-ios-template.md` | 本文档模板来源 |
| `.claude/skills/feature-workflow/references/ios-design/arch-design.md` | iOS 方案阶段的设计要求与修复轮次规则 |
