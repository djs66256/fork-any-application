# iOS 端技术方案：PRD-07 菜单面板

> 创建日期：2026-07-27
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

iOS 端在现有 `TabView + NavigationStack + NavigationRouter` 容器内，为 home tab 增加一个由应用壳层承载的左侧抽屉式菜单面板。实现继续遵循 `ios/CLAUDE.md` 约束的 **MVVM + Clean Architecture（Core → Domain → Data → Presentation）**，不引入第三方依赖，网络继续基于 `URLSession`，导航继续复用既有 `NavigationRouter.navigate(to:)` 和 `.player(videoId:)`，并通过新的菜单路由承接登录 / 消息 / 我的预约 / 我的下载占位页。

```text
AppShellView
  -> overlays MenuPanelContainer above TabView when router.selectedTab == .home
     -> dim background, disable tab interaction
     -> render MenuPanelView
        -> static sections (login / message / games / common functions)
        -> recently viewed section
           -> MenuPanelViewModel.loadIfNeeded()
              -> FetchRecentlyViewedUseCase
                 -> MenuPanelRepository.fetchRecentlyViewed()
                    -> PlayerRemoteDataSource.fetchRecentlyViewed(sessionId)
                       -> GET /api/player/recently-viewed
        -> tap recently viewed card
           -> router.closeMenuPanel()
           -> router.navigate(to: .player(videoId: dramaId))
        -> tap login / messages / booking / downloads
           -> router.closeMenuPanel()
           -> router.navigate(to: .menuPlaceholder(kind))
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `ios/ShortDrama/Sources/App/AppShellView.swift` | 修改 | 在 `TabView` 外层叠加 menu overlay，并控制禁用背景交互 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 增加菜单开关状态、关闭完成回调队列与菜单导航辅助方法 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增菜单占位页 route case |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 注册菜单占位页 destination；home tab 仍承接 player / detail |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 顶部 toolbar 增加汉堡按钮 |
| `ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift` | 修改 | 增加 recently-viewed endpoint |
| `ios/ShortDrama/Sources/Core/Storage/PlaybackSessionStore.swift` | 不变 / 复用 | 继续提供 get-or-create session id |
| `ios/ShortDrama/Sources/Features/Shell/Views/PlaceholderTabView.swift` | 不变 | 顶级 tab 占位页继续存在，与菜单承接页职责不同 |

### 1.2 新增子域划分

1. 壳层菜单状态管理：由 `NavigationRouter` / `AppShellView` 统一管理打开、关闭、蒙层和返回语义。
2. 菜单 UI 展示：登录引导、消息预览、最近在看、游戏中心、常用功能。
3. 最近在看数据链路：session store → remote datasource → repository → use case → view model。
4. 占位承接导航：点击入口先关抽屉，再 push 到 home tab 栈中的占位页。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `ios/ShortDrama/Sources/App/AppShellView.swift` | 修改 | 在 `TabView` 上叠加菜单蒙层与抽屉容器 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 增加 `menuPanelState`、`openMenuPanel()`、`closeMenuPanel()`、`navigateFromMenu(to:)` |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增 `.menuPlaceholder(kind: MenuPlaceholderKind)` |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 注册 `MenuPlaceholderView` |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 新增 leading toolbar 菜单按钮 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift` | 新增 | 壳层 overlay 容器，处理蒙层、抽屉动画与背景禁用 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelView.swift` | 新增 | 菜单内容根视图 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/Components/*` | 新增 | 登录头部、消息区、最近在看卡片、游戏区、常用功能区、错误/空态组件 |
| `ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift` | 新增 | 管理 recently-viewed loading / success / empty / error |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPlaceholderView.swift` | 新增 | 登录 / 消息 / 预约 / 下载承接页 |
| `ios/ShortDrama/Sources/Domain/Entities/RecentlyViewedItem.swift` | 新增 | 最近在看领域实体 |
| `ios/ShortDrama/Sources/Domain/Entities/MenuPlaceholderKind.swift` | 新增 | 菜单占位页类型 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/MenuPanelRepositoryProtocol.swift` | 新增 | recently-viewed 读取协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchRecentlyViewedUseCase.swift` | 新增 | 最近在看读取用例 |
| `ios/ShortDrama/Sources/Data/DTOs/RecentlyViewedResponseDTO.swift` | 新增 | 对齐 shared design 的 DTO |
| `ios/ShortDrama/Sources/Data/Repositories/MenuPanelRepository.swift` | 新增 | DTO -> Entity 映射 |
| `ios/ShortDrama/Tests/ViewModelTests/MenuPanelViewModelTests.swift` | 新增 | 覆盖加载、空态、错误、重试、重复请求 |
| `ios/ShortDrama/Tests/DataTests/MenuPanelRepositoryTests.swift` | 新增 | 覆盖 DTO 映射与错误透传 |
| `ios/ShortDrama/Tests/DataTests/PlayerRemoteDataSourceTests.swift` | 修改 | 增加 recently-viewed endpoint headers / decode |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 新增 / 修改 | 覆盖菜单开关与占位页关闭后导航语义 |

---

## 3. View 层设计

### 3.1 组件层级树

```text
AppShellView
├── TabView
│   └── TabNavigationHostView(home/theater/mall/earn/profile)
└── MenuPanelContainerView (when selectedTab == .home)
    ├── DimmingOverlay
    └── MenuPanelView
        ├── MenuLoginHeaderView
        ├── MenuMessagePreviewView
        ├── MenuRecentlyViewedSection
        │   ├── LoadingState
        │   ├── EmptyState
        │   ├── ErrorState
        │   └── RecentlyViewedCard (ForEach <= 3)
        ├── MenuGameCenterSection
        └── MenuCommonFunctionsSection
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `MenuPanelContainerView` | View | 壳层 overlay、蒙层点击关闭、抽屉滑入动画 | 否 |
| `MenuPanelView` | View | 菜单主内容容器 | 否 |
| `MenuLoginHeaderView` | View | 匿名登录引导头部 | 否 |
| `MenuMessagePreviewView` | View | 单条静态消息预览 | 否 |
| `MenuRecentlyViewedSection` | View | 最近在看三态与卡片列表 | 否 |
| `RecentlyViewedCardView` | View | 单张续播卡片 | 否 |
| `MenuGameCenterSection` | View | 四个游戏图标与“即将上线”反馈 | 否 |
| `MenuCommonFunctionsSection` | View | 我的预约 / 我的下载入口 | 否 |
| `MenuPlaceholderView` | View | 登录 / 消息 / 预约 / 下载承接页 | 否 |

### 3.3 组件接口示意

```swift
struct MenuPanelContainerView: View {
    @EnvironmentObject private var router: NavigationRouter
    @StateObject private var viewModel: MenuPanelViewModel

    var body: some View {
        ZStack(alignment: .leading) {
            if router.isMenuPanelVisible {
                Color.black.opacity(0.35)
                    .ignoresSafeArea()
                    .onTapGesture { router.closeMenuPanel() }

                MenuPanelView(
                    state: viewModel.viewState,
                    onRetryRecentlyViewed: { await viewModel.retry() },
                    onTapRecentlyViewed: { item in
                        router.closeMenuPanelThenNavigate(to: .player(videoId: item.dramaId))
                    },
                    onTapPlaceholder: { kind in
                        router.closeMenuPanelThenNavigate(to: .menuPlaceholder(kind: kind))
                    },
                    onTapGame: { viewModel.showComingSoonHint() }
                )
                .frame(width: UIScreen.main.bounds.width * 0.78)
                .transition(.move(edge: .leading))
            }
        }
        .allowsHitTesting(router.isMenuPanelVisible)
    }
}
```

### 3.4 交互规则

| 交互 | 规则 |
|------|------|
| 打开菜单 | 仅 home 根页顶部左上角汉堡按钮触发 |
| 点击蒙层 | 关闭菜单，不导航 |
| 点击最近在看 | 先关闭菜单，再进入 `.player(videoId:)` |
| 点击登录 / 消息 / 预约 / 下载 | 先关闭菜单，再进入 `.menuPlaceholder(kind:)` |
| 从占位页返回 | `NavigationStack` pop 后回到首页常态，菜单保持关闭 |
| 点击游戏图标 | 不导航，原位展示“即将上线” |

### 3.5 壳层承载方式

- 抽屉 overlay 放在 `AppShellView` 外层，以保证覆盖 `TabView` 与底部 tab item；
- 只有 `router.selectedTab == .home` 时允许打开菜单；切换到其它 tab 自动关闭；
- 打开时通过 `.disabled(router.isMenuPanelVisible)` 或等价方式禁用 `TabView` 背景交互。

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 View | 职责 |
|-----------|----------|------|
| `MenuPanelViewModel` | `MenuPanelContainerView` / `MenuPanelView` | 管理 recently-viewed 区块状态机 |

### 4.2 状态定义

```swift
@MainActor
final class MenuPanelViewModel: ObservableObject {
    enum RecentlyViewedState: Equatable {
        case idle
        case loading
        case content([RecentlyViewedItem])
        case empty
        case error(String)
    }

    @Published private(set) var viewState: RecentlyViewedState = .idle
    @Published private(set) var isHintVisible = false

    private let fetchRecentlyViewedUseCase: FetchRecentlyViewedUseCase
    private let playbackSessionStore: PlaybackSessionStore
    private var hasLoaded = false
    private var inFlightTask: Task<Void, Never>?
}
```

### 4.3 状态机规则

| 状态 | 触发条件 | UI 表现 |
|------|---------|---------|
| `idle` | 菜单尚未首次打开 | 不展示动态内容 |
| `loading` | 首次打开或手动重试 | 最近在看区 loading |
| `content(items)` | 成功且有数据 | 渲染最多 3 张卡片 |
| `empty` | 成功但 `items=[]` | 展示空态文案 |
| `error(message)` | 网络 / 服务异常 | 展示错误文案 + 重试按钮 |

### 4.4 核心行为

- `loadIfNeeded()`：菜单首次打开时调用；如果已加载过且本次会话内无强制刷新，则不重复请求。
- 请求前执行 `playbackSessionStore.getOrCreateSessionId()`；若失败，则进入 `error` 或安全降级空态。
- `retry()`：取消旧请求并重新进入 `loading`。
- 快速开关菜单时，允许请求继续进行，但关闭后返回结果只更新 ViewModel，不重新打开菜单。
- 游戏“即将上线”提示不进入全局状态机，可作为 transient hint 处理。

---

## 5. Navigation 路由设计

### 5.1 Route 扩展

在 `AppRoute` 中新增：

```swift
enum AppRoute: Hashable {
    case home
    case searchHome
    case searchResult(query: String)
    case rankingHome
    case classificationHome
    case newReleases
    case actorHub
    case player(videoId: String)
    case dramaDetail(dramaId: String)
    case menuPlaceholder(kind: MenuPlaceholderKind)
}
```

并保持其 `owningTab == .home`。

### 5.2 Placeholder 类型

```swift
enum MenuPlaceholderKind: Hashable {
    case login
    case messages
    case booking
    case downloads

    var title: String { ... }
    var description: String { ... }
}
```

### 5.3 Router 扩展

`NavigationRouter` 增加：

```swift
@Published private(set) var menuPanelState: MenuPanelPresentationState = .closed
@Published private(set) var pendingMenuNavigation: AppRoute?

var isMenuPanelVisible: Bool {
    switch menuPanelState {
    case .opening, .open, .closing:
        return true
    case .closed:
        return false
    }
}

func openMenuPanel()
func closeMenuPanel()
func closeMenuPanelThenNavigate(to route: AppRoute)
func markMenuPanelDidClose()
```

规则：
- `closeMenuPanelThenNavigate(to:)` 不直接 `navigate(to:)`，而是先把目标路由写入 `pendingMenuNavigation`，再驱动 `menuPanelState = .closing`；
- `MenuPanelContainerView` 在关闭动画完成后回调 `router.markMenuPanelDidClose()`；仅在该回调里真正消费 `pendingMenuNavigation` 并执行导航；
- closing 期间再次点击入口时忽略新的导航请求，防止重复压栈；
- `select(tab:)` 若切换到非 `.home`，强制清空 `pendingMenuNavigation` 并关闭菜单；
- `dismiss()` 不负责重开菜单，避免返回到中间态。

### 5.4 `TabNavigationHostView` 扩展

新增 destination：

```swift
case .menuPlaceholder(let kind):
    MenuPlaceholderView(kind: kind)
```

---

## 6. 网络层设计

### 6.1 端点定义

在 `PlayerEndpoints` 中新增：

```swift
struct GetRecentlyViewed: APIEndpoint {
    typealias Response = RecentlyViewedResponseDTO

    let playbackSessionId: String

    var path: String { "/api/player/recently-viewed" }
    var method: HTTPMethod { .get }
    var headers: [String: String] {
        ["X-Playback-Session-Id": playbackSessionId]
    }
}
```

### 6.2 DataSource / Repository

- `PlayerRemoteDataSource` 新增 `fetchRecentlyViewed(playbackSessionId:)`；
- 新建 `MenuPanelRepository` 专责菜单面板数据，而不是把最近在看塞回 `PlayerRepository`，避免播放器播放控制职责被污染；
- DTO 使用后端 `code/data/message` 包裹结构，Repository 只向 Domain 暴露 `[RecentlyViewedItem]`。

### 6.3 DTO 设计

```swift
struct RecentlyViewedResponseDTO: Decodable {
    let code: Int
    let data: RecentlyViewedDataDTO
    let message: String
}

struct RecentlyViewedDataDTO: Decodable {
    let items: [RecentlyViewedItemDTO]
}

struct RecentlyViewedItemDTO: Decodable {
    let dramaId: String
    let title: String
    let coverUrl: String?
    let episodeNumber: Int
    let progress: Double
    let updatedAt: String
}
```

> 采用 `CodingKeys` 完成 snake_case 到 camelCase 映射。

---

## 7. 数据持久化策略

| 数据类型 | 存储方案 | 说明 |
|---------|---------|------|
| 菜单开关状态 | 不持久化 | 只在当前 UI 会话内有效 |
| 最近在看列表 | 不持久化 | 每次首次打开菜单时重新请求，允许当前会话内内存缓存 |
| playback session id | 继续复用 `KeychainPlaybackSessionStore` | 不新增新存储 |
| Placeholder 配置 | 代码静态枚举 | 不依赖远端配置 |

---

## 8. 测试策略

### 8.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| ViewModel 测试 | 加载、空态、错误、重试、重复请求保护 | Swift Testing |
| Router 测试 | 菜单开关、`closeMenuPanelThenNavigate`、关闭动画完成后导航、防重入 | Swift Testing |
| Data 层测试 | recently-viewed endpoint header、DTO 解码、Repository 映射 | Swift Testing + URLProtocolMock |
| Route 映射测试 | `.menuPlaceholder(kind:)` 的 `owningTab` 与 `publicRouteName` 行为 | Swift Testing |

### 8.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 |
|------|---------|------|---------|------|
| IOS-T01 | 首次打开菜单 | `openMenuPanel()` | 菜单可见，最近在看进入 loading | Router / ViewModel |
| IOS-T02 | 最近在看成功 | 返回 2 条 items | 渲染 content(2) | ViewModel |
| IOS-T03 | 最近在看空态 | 返回空数组 | 进入 `empty` | ViewModel |
| IOS-T04 | session store 失败 | `getOrCreateSessionId()` throw | 进入 `error` 或安全降级 | ViewModel |
| IOS-T05 | 点击最近在看卡片 | `dramaId=abc` | 先进入 closing，待 `markMenuPanelDidClose()` 后才导航到 `.player(videoId: "abc")` | Router |
| IOS-T06 | 点击登录入口 | `.login` | 先进入 closing，待动画完成后导航到 `.menuPlaceholder(kind: .login)` | Router |
| IOS-T07 | closing 中重复点击 | 连续点击两个入口 | 只消费第一个待导航目标，不重复压栈 | Router |
| IOS-T08 | 返回占位页 | dismiss | 回到首页常态，菜单关闭 | Router |
| IOS-T09 | endpoint header 正确 | recent request | 包含 `X-Playback-Session-Id` | Data |
| IOS-T10 | `cover_url=null` | DTO 中封面为空 | Repository 正常映射 | Data |

### 8.3 不在本期测试范围

- SwiftUI snapshot 全量回归；
- 真机动画性能专项；
- 黑盒交互测试（留到 QA 阶段）。

---

## 9. 参考资料

| 文件 | 关键内容 |
|------|---------|
| `docs/specs/2026-07-27-prd-07-menu-panel/spec.md` | 菜单面板与占位承接需求 |
| `docs/specs/2026-07-27-prd-07-menu-panel/design.md` | shared contract、状态机、错误语义 |
| `ios/ShortDrama/Sources/App/AppShellView.swift` | 壳层 `TabView` 承载点 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 当前导航与 container ready 状态 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 当前 home tab route 管理 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | `NavigationStack` destination 注册点 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 首页 toolbar 菜单按钮接入点 |
| `ios/ShortDrama/Sources/Core/Storage/PlaybackSessionStore.swift` | get-or-create session id 能力 |
| `ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift` | player endpoint 扩展位置 |
