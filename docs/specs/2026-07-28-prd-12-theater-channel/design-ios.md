# iOS 端技术方案：PRD-12 剧场频道

> 创建日期：2026-07-28
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

本期 iOS 端在既有 SwiftUI + MVVM + Clean Architecture 架构上，将 `theater` 一级 Tab 从 `PlaceholderTabView` 替换为真实剧场频道页。实现继续遵守当前工程的分层方向：Presentation（SwiftUI View + ViewModel）→ Domain（Entity / RepositoryProtocol / UseCase）← Data（DTO / RemoteDataSource / Repository）。

```text
┌────────────────────────────────────────────────────────────┐
│ Presentation                                               │
│  TabNavigationHostView                                     │
│    └── TheaterView                                         │
│        ├── TheaterTopBar / SearchEntry / ScanEntry         │
│        ├── TheaterChannelTabBar                            │
│        ├── TheaterShortcutGrid                             │
│        ├── TheaterFeedGrid                                 │
│        └── Loading / Empty / Error / AppendError Views     │
├────────────────────────────────────────────────────────────┤
│ ViewModel                                                  │
│  TheaterViewModel                                          │
│    ├── selectedChannel / items / pagination                │
│    ├── first-page loading / append / error state           │
│    ├── request token anti-stale protection                 │
│    └── routeEffect (scan placeholder / ranking entry)      │
├────────────────────────────────────────────────────────────┤
│ Domain                                                     │
│  TheaterChannel / TheaterDrama / TheaterFeedPage           │
│  TheaterRepositoryProtocol                                 │
│  FetchTheaterFeedUseCase                                   │
├────────────────────────────────────────────────────────────┤
│ Data                                                       │
│  TheaterFeedResponseDTO / TheaterDramaDTO                  │
│  DramaRemoteDataSource.fetchTheaterFeed(query:)            │
│  DramaRepository.fetchTheaterFeed(query:)                  │
└────────────────────────────────────────────────────────────┘
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 将 `theater` 分支从 `PlaceholderTabView` 替换为 `TheaterView()` |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 扩展 | 增加剧场内部必要路由（如 theater root），同时保留搜索/排行/分类/新剧归属 `.home` |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 扩展 | 继续负责跨 tab 路由；新增从剧场进入排行页时的初始化上下文注入能力 |
| `ios/ShortDrama/Sources/Features/Home/*` | 不变 | 现有首页能力继续保留，剧场只复用其部分视觉/交互模式 |
| `ios/ShortDrama/Sources/Features/Search/Views/SearchHomeView.swift` | 不变 | 剧场顶部搜索入口继续复用 `.searchHome` 路由 |
| `ios/ShortDrama/Sources/Features/Ranking/*` | 扩展 | 为剧场入口增加排行初始化上下文，支持 `all + booking` 首屏直达 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 扩展 | 新增 `GET /api/dramas/channel` endpoint 与 fetch 方法 |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 扩展 | 新增剧场 Feed repository 能力 |
| `ios/ShortDrama/Tests/*` | 扩展 | 增加 Theater ViewModel、DTO/Repository、Ranking 上下文恢复测试 |

### 1.2 与现有代码现状的兼容说明

1. **剧场页是新内容页，不是新导航容器**  
   当前 `TabNavigationHostView` 中 `.theater` 仅展示 `PlaceholderTabView`。本期只替换 root view，不新增新的 `NavigationStack` 结构，也不复制一套 home-owned 页面到 theater tab 内。

2. **搜索 / 分类 / 排行 / 新剧继续归属于 `.home`**  
   当前 `AppRoute.owningTab` 已将 `.searchHome`、`.rankingHome`、`.classificationHome`、`.newReleases` 归属到 `.home`；`NavigationRouter.navigate(to:)` 会自动切换 `selectedTab`。本期保留这一机制，把“允许切换到底部 home tab”作为正式设计，而不是规避它。

3. **排行页初始化能力需在现有 `RankingViewModel` 基础上扩展**  
   当前 `RankingHomeView` 固定创建默认 `RankingViewModel`，其初始值是 `.all + .hot`。为满足“预约入口一步直达 booking 榜”，需要为 `RankingHomeView` / `RankingViewModel` 引入可选初始化上下文，而不是新建第二个排行页。

4. **网络层继续使用 URLSession + APIEndpoint**  
   现有 `DramaRemoteDataSource.swift` 已采用 `APIEndpoint` 模式定义 dramas、search、rankings 等 endpoint。本期复用同一模式新增 `GetTheaterFeedEndpoint`，不引入新网络抽象。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | `case .theater` 返回 `TheaterView()`，其余 mall/earn/profile 继续 placeholder |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 视实现需要补充 theater 路由 / ranking entry context 承载结构 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 增加剧场进入排行时的上下文暂存与消费能力 |
| `ios/ShortDrama/Sources/Features/Theater/Views/TheaterView.swift` | 新增 | 剧场页根视图 |
| `ios/ShortDrama/Sources/Features/Theater/Views/TheaterFeedGridView.swift` | 新增 | 双列 Feed、尾部 loading / append error |
| `ios/ShortDrama/Sources/Features/Theater/Views/TheaterChannelTabBar.swift` | 新增 | 8 个子频道横向 Tab |
| `ios/ShortDrama/Sources/Features/Theater/Views/TheaterShortcutGrid.swift` | 新增 | 筛选 / 排行 / 新剧 / 预约快捷入口 |
| `ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift` | 新增 | 页面状态机、分页、乱序保护、路由 effect |
| `ios/ShortDrama/Sources/Features/Theater/Models/TheaterShortcut.swift` | 新增 | 快捷入口本地枚举 / UI 模型 |
| `ios/ShortDrama/Sources/Domain/Entities/TheaterChannel.swift` | 新增 | 频道枚举实体 |
| `ios/ShortDrama/Sources/Domain/Entities/TheaterDrama.swift` | 新增 | 剧场卡片业务实体 |
| `ios/ShortDrama/Sources/Domain/Entities/TheaterFeedPage.swift` | 新增 | 分页业务实体 |
| `ios/ShortDrama/Sources/Domain/Entities/TheaterRankingEntryContext.swift` | 新增 | 排行初始化上下文 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 修改 | 增加 `fetchTheaterFeed(query:)` |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchTheaterFeedUseCase.swift` | 新增 | 剧场 Feed use case |
| `ios/ShortDrama/Sources/Data/DTOs/TheaterFeedResponseDTO.swift` | 新增 | 剧场接口 DTO 与映射 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 增加 endpoint / fetch 方法 |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 修改 | 实现剧场 Feed 获取 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift` | 修改 | 从 router 或 init context 读取初始榜单参数 |
| `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` | 修改 | 支持外部 initialContentType / initialRankingType |
| `ios/ShortDrama/Tests/ViewModelTests/TheaterViewModelTests.swift` | 新增 | 状态机 / 分页 / 乱序保护测试 |
| `ios/ShortDrama/Tests/DataTests/TheaterFeedDTOTests.swift` | 新增 | DTO 映射与数值字段校验 |
| `ios/ShortDrama/Tests/ViewModelTests/RankingViewModelTests.swift` | 修改 | 覆盖剧场预约入口的 booking 初始化 |

---

## 3. View 层设计

### 3.1 组件层级树

```text
TheaterView
├── TheaterNavigationBar
│   ├── TheaterSearchEntryButton
│   └── TheaterScanEntryButton
├── TheaterChannelTabBar
├── TheaterShortcutGrid
│   ├── TheaterShortcutButton(.classification)
│   ├── TheaterShortcutButton(.ranking)
│   ├── TheaterShortcutButton(.newReleases)
│   └── TheaterShortcutButton(.booking)
└── TheaterFeedContainer
    ├── TheaterLoadingView
    ├── TheaterErrorView
    ├── TheaterEmptyView
    └── TheaterFeedGridView
        ├── TheaterDramaCardView (LazyVGrid)
        └── TheaterAppendFooterView
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `TheaterView` | View | 剧场页根容器，组合顶部栏、频道、快捷入口与 Feed | 否 |
| `TheaterNavigationBar` | View | 承载搜索入口与识图占位入口 | 否 |
| `TheaterChannelTabBar` | View | 展示 8 个子频道并处理切换 | 否 |
| `TheaterShortcutGrid` | View | 展示 4 个快捷入口 | 否 |
| `TheaterFeedGridView` | View | 双列 Feed、分页尾部、空白补位 | 否 |
| `TheaterDramaCardView` | View | 单张剧场卡片，展示封面、热度、标题、标签 | 否 |
| `TheaterLoadingView` | View | 首屏加载态 | 否 |
| `TheaterEmptyView` | View | 空频道占位态 | 否 |
| `TheaterErrorView` | View | 首屏错误态 + 重试 | 否 |
| `SearchHomeView` | View | 搜索发现承接页 | 是 |
| `RankingHomeView` | View | 排行 / 预约榜承接页 | 是 |
| `ClassificationHomeView` | View | 筛选承接页 | 是 |
| `DiscoveryPlaceholderView(kind: .newReleases)` | View | 新剧占位承接 | 是 |

### 3.3 组件接口定义

```swift
struct TheaterView: View {
    @EnvironmentObject private var router: NavigationRouter
    @StateObject private var viewModel: TheaterViewModel

    init() {
        let repository: DramaRepositoryProtocol = DramaRepository()
        _viewModel = StateObject(
            wrappedValue: TheaterViewModel(
                fetchTheaterFeedUseCase: FetchTheaterFeedUseCase(repository: repository)
            )
        )
    }

    var body: some View { ... }
}
```

```swift
struct TheaterDramaCardView: View {
    let drama: TheaterDrama
    let heatText: String
    let onTap: () -> Void

    var body: some View { ... }
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| `TheaterView -> 子组件` | 构造函数参数 | 当前选中频道、Feed 数据、交互回调 |
| 子组件 -> `TheaterView` | Closure Callback | 点击频道、快捷入口、卡片、重试、加载更多 |
| 跨页面共享 | `@EnvironmentObject NavigationRouter` | 触发 search / ranking / classification / play 路由 |
| View -> ViewModel | 方法调用 + `Task { await ... }` | 首屏加载、频道切换、分页、重试 |
| Router -> Ranking | 上下文暂存 / 消费 | 剧场预约入口直达 `all + booking` |

### 3.5 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | `LazyVGrid(columns: [.flexible(), .flexible()])` | 双列卡片在 iPhone 常规宽度下保持稳定布局 |
| Dynamic Type | 标题和标签使用 `Text` + 既有 `DesignTokens` 字号 | 避免剧场卡片因放大字体溢出过多行 |
| 深色模式 | 复用 Asset Catalog / `DesignTokens` | 不新增硬编码颜色 |
| 安全区域 | 页面滚动容器遵循系统 safe area | 顶部栏与底部 tab 不互相遮挡 |
| 小屏横向 Tab | `ScrollView(.horizontal)` | 确保 8 个频道均可访问 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 View | 职责 |
|-----------|----------|------|
| `TheaterViewModel` | `TheaterView` | 管理剧场频道选中态、首屏加载、分页、错误态、快捷入口 effect |
| `RankingViewModel` | `RankingHomeView` | 继续负责排行页加载；本期补充剧场入口初始榜单上下文 |

### 4.2 状态定义

```swift
@MainActor
final class TheaterViewModel: ObservableObject {
    enum ViewState: Equatable {
        case loading
        case content([TheaterDrama])
        case empty
        case error(String)
    }

    @Published private(set) var selectedChannel: TheaterChannel = .all
    @Published private(set) var viewState: ViewState = .loading
    @Published private(set) var isAppending = false
    @Published private(set) var appendErrorMessage: String?

    private var currentItems: [TheaterDrama] = []
    private var currentPage = 0
    private var totalPages = 1
    private var requestToken = UUID()

    func loadIfNeeded() async { ... }
    func selectChannel(_ channel: TheaterChannel) async { ... }
    func loadMoreIfNeeded() async { ... }
    func retry() async { ... }
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `selectedChannel` | `TheaterChannel` | `.all` | 当前频道 |
| `viewState` | `ViewState` | `.loading` | 首屏 loading / content / empty / error |
| `isAppending` | `Bool` | `false` | 是否正在加载更多 |
| `appendErrorMessage` | `String?` | `nil` | 分页失败提示，不影响已有内容 |
| `currentPage` | `Int` | `0` | 当前已成功加载页码 |
| `totalPages` | `Int` | `1` | 服务端总页数 |
| `requestToken` | `UUID` | 新建 | 防止旧请求覆盖新频道结果 |
| `hasLoaded` | `Bool` | `false` | 避免重复首屏加载 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | View 层表现 |
|---------|---------|-----------|
| Loading | `viewState == .loading` | 页面骨架 / ProgressView |
| Success（有数据） | `viewState == .content && items.count > 0` | 双列 Feed + 快捷入口 |
| Empty | `viewState == .empty` | 空态图文 + 当前频道说明 |
| Error（可重试） | `viewState == .error(message)` | 错误视图 + 重试按钮 |
| Append Error | `appendErrorMessage != nil && currentItems.isNotEmpty()` | 列表尾部错误提示，保留已有内容 |

### 4.5 状态机约束映射

| shared design 约束 | iOS 端落实方式 |
|-------------------|----------------|
| 首次进入默认 `channel=all` | `selectedChannel` 默认 `.all`；`loadIfNeeded()` 请求第一页 |
| 切频道时清空旧列表并回到第一页 | `selectChannel(_:)` 内重置 `currentItems/currentPage/totalPages` 后发起新请求 |
| 旧请求不得覆盖新频道状态 | 每次 reload 生成新 `requestToken`，响应提交前校验 token |
| append failure 不清空已有列表 | `appendErrorMessage` 单独承载，`viewState` 保持 `.content(currentItems)` |
| 非 `all` 频道首版空态 | 接口返回空数组时走 `.empty`，不视为错误 |

---

## 5. Navigation 路由设计

### 5.1 导航方案

继续使用现有 `NavigationStack + NavigationRouter`。剧场页作为 `theater` Tab 的 root view，不在 theater tab 内额外嵌套一套路由系统。

### 5.2 路由清单

| 路由标识 | 目标页面 | 参数 | 导航方式 | 说明 |
|---------|---------|------|---------|------|
| `.searchHome` | `SearchHomeView` | 无 | Push，且 `selectedTab = .home` | 顶部搜索入口复用现有搜索发现页 |
| `.classificationHome` | `ClassificationHomeView` | 无 | Push，且 `selectedTab = .home` | 筛选快捷入口 |
| `.rankingHome` | `RankingHomeView` | 无显式 path 参数，结合 router context | Push，且 `selectedTab = .home` | 排行 / 预约入口都复用此路由 |
| `.newReleases` | `DiscoveryPlaceholderView(kind: .newReleases)` | 无 | Push，且 `selectedTab = .home` | 新剧占位承接 |
| `.player(videoId:)` | `PlayerView` | `videoId` | Push，且 `selectedTab = .home` | 点击卡片播放，继续复用既有 home-owned `play` 承接语义 |
| `.theaterHome`（如补充） | `TheaterView` | 无 | Root | 仅供 deeplink / 结构清晰使用；不要求外露新 public route |

### 5.3 路由管理

```swift
extension NavigationRouter {
    func openRanking(from context: TheaterRankingEntryContext) {
        pendingTheaterRankingEntryContext = context
        navigate(to: .rankingHome)
    }

    func consumeTheaterRankingEntryContext() -> TheaterRankingEntryContext? {
        defer { pendingTheaterRankingEntryContext = nil }
        return pendingTheaterRankingEntryContext
    }
}
```

设计要点：

1. **保留现有 route owningTab 规则**  
   不把 `.rankingHome` 改成 `.theater`，否则会与现有搜索 / 排行体系不一致。

2. **剧场预约入口通过 router context 注入初始榜单**  
   - “排行”入口注入 `.all + .hot`
   - “预约”入口注入 `.all + .booking`
   - `RankingHomeView` 首屏读取该上下文，仅在当前导航来源是剧场入口时消费一次

3. **搜索 / 排行 / 分类 / 新剧允许切到底部 home**  
   这是需求收敛后的正式行为，不再尝试维持 theater 内副本。

### 5.4 Deep Link 处理（如适用）

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `djsdrama://play/{videoId}` | `.player(videoId:)` | `videoId` |
| `djsdrama://search` | `.searchHome` | 无 |
| `djsdrama://ranking` | `.rankingHome` | 无；若未来需要 booking，使用 query 或 router context 扩展 |
| `djsdrama://theater` | theater root | 无 |

本期不要求新增对外公开的 theater 子频道 deeplink；仅保证剧场页在应用内导航链路可达。

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | `APIClient`（URLSession） | 复用现有请求入口 |
| 请求构建 | `APIEndpoint` protocol | 新增 `GetTheaterFeedEndpoint` |
| 响应解析 | `Codable` + `JSONDecoder` | 新增 theater DTO |
| 错误处理 | `APIError` | 继续统一到 repository / ViewModel |
| 日志 | 现有 APIClient 行为 | 本期不新增专属日志库 |

### 6.2 API 端点定义

```swift
struct GetTheaterFeedEndpoint: APIEndpoint {
    typealias Response = TheaterFeedResponseDTO

    let query: TheaterFeedQuery

    var path: String { "/api/dramas/channel" }
    var method: HTTPMethod { .get }
    var queryItems: [URLQueryItem]? {
        [
            URLQueryItem(name: "channel", value: query.channel.rawValue),
            URLQueryItem(name: "page", value: String(query.page)),
            URLQueryItem(name: "pageSize", value: String(query.pageSize))
        ]
    }
}
```

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 页面主动重试 | 0（自动） | 无 | 由用户点击“重试”触发新的完整请求 |
| 加载更多失败 | 0（自动） | 无 | 由用户再次滚动或点击尾部重试触发 |
| 5xx / 网络异常 | 不在网络层静默重试 | 无 | 避免重复请求导致列表状态抖动 |

### 6.4 网络状态监听

本期不新增 `NWPathMonitor` 专属监听逻辑。剧场页沿用当前页面级错误态 + 手动重试策略即可满足首版需求。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| 剧场 Feed 列表 | 不持久化，仅页面内存态 | ViewModel 内存 | 页面销毁即释放 | 首版无需离线缓存 |
| 当前频道 | 不持久化 | ViewModel 状态 | 页面销毁即释放 | 返回 theater root 时默认回到 `all` |
| 排行入口初始化上下文 | Router 暂存 | `NavigationRouter` 属性 | 消费一次后清空 | 仅用于剧场快捷入口跨 tab 注入 |

### 7.2 CoreData 模型设计

```text
本期不引入 CoreData / UserDefaults 新存储。
剧场页列表、分页和选中频道全部保留在页面内存态。
```

### 7.3 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| 剧场第一页数据 | 无额外缓存 | — | 页面释放即回收 |
| 图片资源 | 沿用系统 / 现有实现 | 现有策略 | 现有策略 |

### 7.4 数据迁移策略

- 本期无本地存储 schema 变更。
- 无需 CoreData migration、UserDefaults key 迁移或 Keychain 兼容处理。

---

## 8. 配置与环境

| 配置项 | 管理方式 | 开发环境值 | 生产环境值 | 说明 |
|--------|---------|----------|-----------|------|
| API Base URL | `xcconfig + Info.plist` | 现有配置 | 现有配置 | 继续通过 `AppConfig.apiBaseURL` 访问 |
| Theater page feature flag | 不新增 | — | — | 本期默认随应用构建上线 |
| Heat 文案格式化规则 | 本地常量 / formatter | 固定规则 | 固定规则 | 属于端内展示逻辑，不依赖环境变量 |

> ⚠️ 禁止硬编码环境地址、token、开关或产品名。剧场页仅复用现有 `AppConfig` 与设计 tokens，不新增环境配置项。

---

## 9. API 调用清单

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| `GET /api/dramas/channel` | 首次进入剧场页 | `selectedChannel=.all`，`page=1`，`pageSize=20` | 更新 `viewState` 为 `.content/.empty` | 首屏错误态 + 重试 |
| `GET /api/dramas/channel` | 切换子频道 | 用户点击频道 Tab | 重置第一页并更新对应频道内容 | 错误态但保留选中频道 |
| `GET /api/dramas/channel` | 触底加载更多 | 当前 `hasNextPage == true` | 追加到 `currentItems` | 仅设置 `appendErrorMessage` |
| `GET /api/dramas/rankings` | 从剧场进入排行 / 预约 | `RankingHomeView` 消费剧场入口 context | 加载默认榜单 / 预约榜 | 沿用现有排行错误处理 |
| `GET /api/dramas/tags` | 点击筛选入口后进入分类页 | 分类页现有逻辑 | 沿用现有分类逻辑 | 沿用现有分类错误处理 |
| `POST /api/dramas/{id}/book` | 进入预约榜后执行预约 | 排行页现有逻辑 | 沿用现有预约更新 | 沿用现有登录 / 错误提示 |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | iOS 端实现方式 |
|---------|---------------|---------------|
| 默认子频道 | 首次固定 `channel=all` | `TheaterViewModel.selectedChannel = .all` |
| 默认分页 | `page=1&pageSize=20` | `TheaterFeedQuery(channel: selectedChannel, page: 1, pageSize: 20)` |
| 子频道切换重置 | 切换时清空旧列表、回第一页 | `selectChannel(_:)` 内 reset 状态 |
| 请求防乱序 | 旧请求不得覆盖新状态 | `requestToken` 校验后再提交 UI |
| 加载更多约束 | 仅有下一页且当前无请求在途时触发 | `loadMoreIfNeeded()` guard 条件 |
| 空态策略 | 非 `all` 频道展示空态 | 后端空数组 -> `viewState = .empty` |
| 热度格式化 | 服务端下发数值，端侧本地格式化 | `TheaterHeatFormatter` 生成 `2.3万` 等文案 |
| 搜索入口承接 | 点击搜索进入 home-owned 搜索页 | `router.navigate(to: .searchHome)` |
| 快捷入口承接 | 筛选 / 排行 / 预约 / 新剧复用现有路由 | `router.navigate` / `router.openRanking(from:)` |
| 预约榜直达 | 一步进入 `all + booking` | `TheaterRankingEntryContext(contentType: .all, rankingType: .booking)` |
| 播放跳转 | 点击卡片复用 canonical `play`，并沿用现有 home-owned 承接 | `router.navigate(to: .player(videoId: drama.id))`（由 `NavigationRouter` 按既有 owningTab 规则切到 `.home`） |
| 识图入口 | 本地占位反馈，不请求权限/网络 | 通过 alert / toast style 本地提示 |

---

## 11. 边界与错误处理

### 11.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| 网络层 | `APIError` | 统一转换后抛给 repository / use case |
| ViewModel | `do-catch` + `ViewState` | 区分首屏错误与 append 错误 |
| View 层 | 内联错误视图 + 尾部错误提示 + alert | 不因分页失败清空已有列表 |
| 日志 | 沿用现有本地日志 | 本期不新增 Crashlytics 专属逻辑 |

### 11.2 错误码映射表

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `VALIDATION_ERROR` | 请求参数有误，请稍后重试 | 首屏错误视图 / 轻提示 |
| `UNAUTHORIZED` | 请先登录 | 本期 theater feed 不应出现；预约逻辑沿用排行现有提示 |
| `FORBIDDEN` | 当前不可访问 | 错误视图 |
| `NOT_FOUND` | 资源不存在 | 轻提示或空态 |
| `CONFLICT` | 当前状态冲突，请稍后重试 | 轻提示 |
| `TOO_MANY_REQUESTS` | 请求过于频繁，请稍后重试 | 轻提示 |
| `INTERNAL_ERROR` | 服务开小差了，请稍后重试 | 错误视图 / 尾部错误 |
| `SERVICE_UNAVAILABLE` | 服务暂不可用，请稍后重试 | 错误视图 / 尾部错误 |
| `NETWORK_ERROR` | 网络异常，请检查后重试 | 错误视图 / 尾部错误 |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| 快速切换多个频道 | 用户连续点击不同频道 | 仅最后一个 `requestToken` 可提交结果 | 🔴 |
| 首屏失败后重试 | 首次请求超时 / 5xx | `retry()` 重新请求当前频道第一页 | 🔴 |
| 分页失败 | 下一页请求失败 | 保留当前列表，仅展示尾部错误 | 🔴 |
| 从剧场进入预约榜 | iOS 路由无显式 ranking 参数 | 使用 router 暂存上下文，`RankingHomeView` 首屏消费 | 🔴 |
| 多次点击搜索入口 | 重复点击搜索框 | 依赖 router 与导航栈，避免重复无意义 push | 🟡 |
| 识图入口重复点击 | 用户连点 | 只展示有限次本地提示，不触发权限请求 | 🟡 |
| 封面缺失 | `cover_url == nil` | 统一占位图 / placeholder 样式 | 🟡 |
| 页面退出时请求未完成 | 用户切走 tab / 返回 | Swift Concurrency task 随 ViewModel 生命周期取消或 token 失效 | 🟡 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `TheaterView` | 首屏 loading | 渲染频道 + 快捷入口 + Feed | 渲染频道 + 快捷入口 + 空态 | 错误视图 + 重试 | 本期无独立不可重试态 |
| `TheaterFeedGridView` | 不展示 | 双列卡片 | 空列表不进入 grid | append error footer | — |
| `TheaterShortcutGrid` | 与页面同生命周期 | 正常可点击 | 仍可点击 | 仍可点击 | — |
| `TheaterChannelTabBar` | 与页面同生命周期 | 正常切换 | 正常切换 | 保留选中频道 | — |

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | `TheaterViewModel` 状态机、分页、请求乱序保护 | 关键场景全覆盖 | Swift Testing |
| 单元测试 | `RankingViewModel` 剧场入口初始化上下文 | 关键场景全覆盖 | Swift Testing |
| Data 层测试 | `TheaterFeedResponseDTO` 解码与 `toDomain()` | 关键场景全覆盖 | Swift Testing + URLProtocol Mock |
| Repository 测试 | `DramaRepository.fetchTheaterFeed(query:)` | 关键场景全覆盖 | Swift Testing |
| 视图轻量测试 | formatter / route builder 等纯逻辑 | 关键场景全覆盖 | Swift Testing |

### 12.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| IOS-01 | 首次进入默认加载 `all` 第一页 | mock 返回 theater data | `loadIfNeeded()` | `selectedChannel == .all`，`viewState == .content` | 单元 |
| IOS-02 | 非 `all` 频道返回空态 | mock 返回空数组 | `selectChannel(.real)` | `viewState == .empty` | 单元 |
| IOS-03 | 快速切换频道时旧请求不覆盖新状态 | `all` 响应慢、`anime` 响应快 | 连续切频道 | 最终状态只属于最后一次频道 | 单元 |
| IOS-04 | 分页失败不清空已有列表 | 第一页成功、第二页失败 | `loadMoreIfNeeded()` | `viewState` 仍是 `.content`，`appendErrorMessage != nil` | 单元 |
| IOS-05 | 热度格式化 | `heat = 23000` | formatter 执行 | 输出 `2.3万` 风格文案 | 单元 |
| IOS-06 | 预约入口一步进入 booking 榜 | router 中存在 booking context | 打开 `RankingHomeView` | `selectedContentType == .all` 且 `selectedRankingType == .booking` | 单元 |
| IOS-07 | `GET /api/dramas/channel` endpoint 参数正确 | query = `.all/page1/pageSize20` | 构建 endpoint | path 与 queryItems 正确 | Data |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| API 请求 | `URLProtocol` Stub | 不发起真实网络请求，符合 `ios/CLAUDE.md` |
| Repository | Protocol Mock / Fake Repository | ViewModel 测试隔离网络层 |
| NavigationRouter context | Fake Router / test double | 验证 booking 上下文消费 |

---

## 13. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | — | — | 本期复用现有 SwiftUI / URLSession / Swift Testing 能力 |

> ⚠️ 不新增任何开源依赖，避免额外用户确认成本。

---

## 14. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 预约入口无法稳定直达 booking 榜 | iOS 快捷入口主链路 | 🔴 | 中 | 使用 router context 一次性注入初始榜单，避免依赖现有无参路由 | 若实现复杂度超预期，可在 RankingViewModel init 增加显式参数入口 |
| 频道切换请求乱序导致 UI 串频 | Theater Feed 展示 | 🔴 | 中 | 使用 `requestToken` 防串频 | 保底在完成请求前校验当前频道 |
| 双列布局在小屏设备上拥挤 | 剧场主页面体验 | 🟡 | 中 | 限制卡片文本行数、使用自适应间距 | 降级卡片信息密度 |
| 与现有 home-owned 路由语义冲突 | 搜索/排行/分类承接体验 | 🟡 | 低 | 保持 `owningTab = .home`，不改既有承接页归属 | 如有争议，以 spec/design 收敛结果为准 |
| 测试覆盖不足导致状态机回归 | 开发与后续迭代 | 🟡 | 中 | TheaterViewModel / Ranking context 补齐单元测试 | 未覆盖前不进入 coding 完成态 |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/features/app-shell/index.md` | theater tab 与 app shell | 确认 theater 是既有一级 tab |
| `wiki/features/search-discovery/index.md` | 搜索发现 / 新剧承接 | 确认搜索与新剧承接已存在 |
| `wiki/features/ranking/index.md` | 排行 / 预约榜 | 确认 booking 语义需复用排行页 |
| `wiki/features/classification/index.md` | 分类承接 | 确认筛选入口可复用 classification |
| `wiki/features/video-player/index.md` | play route | 确认播放链路继续复用 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 搜索 / 排行 / 分类 / 新剧均归属 `.home` |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | `navigate(to:)` 会切换 `selectedTab` |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | theater 当前仍是 `PlaceholderTabView` |
| `ios/ShortDrama/Sources/Features/Search/Views/SearchHomeView.swift` | 搜索发现入口与 QuickEntryGrid 路由模式 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift` | 排行页结构与 ViewModel 注入方式 |
| `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` | 默认 `.all + .hot`、分页和错误态管理 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 页面层级、toolbar 搜索入口与错误态组织方式 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | `APIEndpoint` + `APIClient` 的现有 endpoint 组织方式 |

