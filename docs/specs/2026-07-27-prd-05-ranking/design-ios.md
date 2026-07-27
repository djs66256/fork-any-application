# iOS 端技术方案：PRD-05 排行体系

> 创建日期：2026-07-27
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

PRD-05 在现有 iOS `TabView + NavigationStack + NavigationRouter` 容器内，将搜索发现页的 `rankingHome` 承接页从占位实现替换为真实排行页。实现继续严格遵循 `ios/CLAUDE.md` 约束的 **MVVM + Clean Architecture（Core → Domain → Data → Presentation）**，不引入第三方依赖，网络继续基于 `URLSession`，导航继续基于 `NavigationStack + djsdrama://` deeplink。

本期能力拆分为四个子域：

1. 排行列表页壳与双层 Tab 交互。
2. 排行数据读取与分页状态机。
3. 预约榜中的预约动作与登录拦截接入点。
4. 列表项点击复用现有 canonical `play` 路由进入播放器承接链路。

```text
SearchHomeView
  -> QuickEntry(.ranking)
     -> NavigationRouter.navigate(to: .rankingHome)

RankingHomeView
  -> observes RankingViewModel
     -> LoadRankingsUseCase(query)
        -> DramaRepositoryProtocol.fetchRankings(query)
           -> DramaRemoteDataSource.fetchRankings(query)
              -> APIClient.request(DramaEndpoints.GetRankings)
                 -> GET /api/dramas/rankings?type=hot&contentType=all&page=1&pageSize=10
  -> when booking tab item tapped
     -> SubmitDramaBookingUseCase(dramaId)
        -> DramaRepositoryProtocol.bookDrama(dramaId)
           -> DramaRemoteDataSource.bookDrama(dramaId)
              -> APIClient.request(DramaEndpoints.BookDrama)
                 -> POST /api/dramas/{id}/book
  -> when list item tapped
     -> NavigationRouter.navigate(to: .player(videoId: drama.id))
        -> publicRouteName == play
        -> PlayerView(viewModel: PlayerViewModel(videoId: drama.id))
        -> 当前播放器仍为占位承接，不扩展播放能力范围
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 不变 | 继续复用现有 `.rankingHome` 路由和对外公开名 `ranking`，不新增新的排行顶级路由 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 不变 / 轻微扩展 | 现有 push 逻辑已能承接 `.rankingHome`；若后续登录拦截接入统一入口，仅消费排行模块抛出的 effect，不改变排行页归属 tab |
| `ios/ShortDrama/Sources/App/DeeplinkHandler.swift` | 不变 | 继续复用 `djsdrama://ranking -> .rankingHome` 解析结果 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 将 `.rankingHome` 对应页面从 `DiscoveryPlaceholderView(kind: .ranking)` 替换为真实 `RankingHomeView` |
| `ios/ShortDrama/Sources/Features/Search/Views/SearchHomeView.swift` | 不变 | 搜索发现页的「排行」快捷入口与导航动作保持不变 |
| `ios/ShortDrama/Sources/Features/Search/Views/DiscoveryPlaceholderView.swift` | 修改 | `ranking` 分支不再由该占位页承接；`classification/new-releases/actors` 仍保持占位 |
| `ios/ShortDrama/Sources/Features/Home/Views/Components/HomeDramaCardView.swift` | 复用 / 参考 | 排行列表卡片继续复用首页已有封面与元信息组织方式，但新增榜单序号、榜单指标、预约按钮等能力 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 修改 | 扩展排行读取与预约提交协议 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 增加 `GET /api/dramas/rankings` 与 `POST /api/dramas/{id}/book` |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 修改 | 增加 DTO -> Entity 映射与预约结果映射 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 不变 / 复用 | 继续使用现有 URLSession 能力与错误解码；预约接口依旧走统一错误映射 |
| `ios/ShortDrama/Sources/Features/Player/*` | 不变 | 排行点击只复用现有 `.player(videoId:)` 承接；当前 `PlayerView` 仍为占位实现，本 PRD 不扩大播放器范围 |
| `ios/ShortDrama/Tests/*` | 新增 / 修改 | 增补排行 ViewModel、Data 层、Router / Deeplink、预约动作与并发状态测试 |

### 1.2 分层职责

| 层级 | 新增/扩展对象 | 职责 |
|------|--------------|------|
| Core | 继续复用 `APIClient`、`APIEndpoint`、`APIError`、`AppConfig` | 统一网络请求、环境配置、错误归一 |
| Domain | `RankingType`、`RankingContentType`、`RankingQuery`、`RankingDrama`、`BookDramaResult`；`FetchRankingsUseCase`、`BookDramaUseCase`；`DramaRepositoryProtocol` 扩展 | 纯业务模型、查询条件、状态流约束 |
| Data | `RankingDramaDTO`、排行分页响应 DTO、预约响应 DTO；`DramaRemoteDataSource`、`DramaRepository` 扩展 | 远端请求与 DTO -> Entity 映射 |
| Presentation | `RankingHomeView`、`RankingViewModel`、列表/Tab/空态/错误态组件 | SwiftUI 页面装配、状态机、分页、导航、预约交互 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 将 `.rankingHome` 注册页替换为 `RankingHomeView()` |
| `ios/ShortDrama/Sources/Features/Search/Views/DiscoveryPlaceholderView.swift` | 修改 | 去掉 `ranking` 的真实承接职责，仅保留其他入口的占位实现 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift` | 新增 | 排行页根视图 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/Components/RankingPrimaryTabBar.swift` | 新增 | 一级 Tab：全部 / 真人 / AI |
| `ios/ShortDrama/Sources/Features/Ranking/Views/Components/RankingSecondaryTabBar.swift` | 新增 | 二级 Tab：热榜 / 推荐榜 / 预约榜 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/Components/RankingListView.swift` | 新增 | 排行列表与分页触底承接 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/Components/RankingDramaCardView.swift` | 新增 | 排行列表项卡片，承载榜单序号、榜单值、预约按钮 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/Components/RankingStateView.swift` | 新增 | loading / empty / error / append error 统一状态容器 |
| `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` | 新增 | 双层 Tab、分页、预约、并发去重、登录拦截 effect |
| `ios/ShortDrama/Sources/Domain/Entities/RankingQuery.swift` | 新增 | 排行查询实体 |
| `ios/ShortDrama/Sources/Domain/Entities/RankingDrama.swift` | 新增 | 排行列表项实体 |
| `ios/ShortDrama/Sources/Domain/Entities/BookDramaResult.swift` | 新增 | 预约提交结果实体 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 修改 | 新增 `fetchRankings` / `bookDrama` 协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchRankingsUseCase.swift` | 新增 | 排行读取用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/BookDramaUseCase.swift` | 新增 | 预约提交用例 |
| `ios/ShortDrama/Sources/Data/DTOs/RankingDramaDTO.swift` | 新增 | 对齐 shared design 的排行 DTO |
| `ios/ShortDrama/Sources/Data/DTOs/BookDramaResponseDTO.swift` | 新增 | 对齐共享方案中的预约响应 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 增加 rankings / book 请求 |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 修改 | 实现新增仓库协议能力 |
| `ios/ShortDrama/Tests/ViewModelTests/RankingViewModelTests.swift` | 新增 | 覆盖默认加载、切换、分页、预约、并发状态 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 补充排行页点击播放承接仍走 `.player(videoId:)` 的约束 |
| `ios/ShortDrama/Tests/ViewModelTests/DeeplinkHandlerTests.swift` | 修改 | 保持 `djsdrama://ranking` 仍落到 `.rankingHome` |
| `ios/ShortDrama/Tests/DataTests/DramaRemoteDataSourceTests.swift` | 修改 | 覆盖 rankings / book endpoint、query 和 body / path 契约 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 新增 | 覆盖 DTO -> Entity 映射、预约结果映射 |

---

## 3. View 层设计

### 3.1 组件层级树

```text
RankingHomeView
├── RankingHeaderBar
│   ├── BackButton
│   └── Title("排行")
├── RankingPrimaryTabBar
│   ├── AllTab
│   ├── LiveActionTab
│   └── AITab
├── RankingSecondaryTabBar
│   ├── HotTab
│   ├── RecommendTab
│   └── BookingTab
└── RankingStateView
    ├── RankingLoadingView
    ├── RankingErrorView
    ├── RankingEmptyView
    └── RankingListView
        └── RankingDramaCardView (ForEach)
            ├── RankingBadgeView
            ├── DramaCoverView
            ├── DramaMetaSection
            │   ├── Title
            │   ├── Description / Tags / Category
            │   └── RankingMetricView
            └── CardActionRow
                ├── PlayButton / WholeCardTap
                └── BookingButton (仅预约榜显示)
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `RankingHomeView` | View | 排行页根视图，承载双层 Tab、状态容器、导航与首屏 task | 否 |
| `RankingPrimaryTabBar` | View | 内容类型一级 Tab：全部 / 真人 / AI | 否 |
| `RankingSecondaryTabBar` | View | 榜单类型二级 Tab：热榜 / 推荐榜 / 预约榜 | 否 |
| `RankingStateView` | View | 根据 ViewModel 状态渲染 loading / content / empty / error / append error | 否 |
| `RankingListView` | View | 列表渲染、触底加载更多、滚动重置锚点 | 否 |
| `RankingDramaCardView` | View | 排行卡片，整合序号、封面、文案、榜单指标、预约按钮 | 否 |
| `RankingMetricView` | View | 根据榜单类型展示热度值 / 推荐值 / 预约数 | 是 |
| `RankingBookingButton` | View | 预约榜按钮，区分未预约 / 提交中 / 已预约 | 是 |
| `DramaCoverView` | View | 复用首页卡片封面加载与占位逻辑 | 是 |
| `HomeDramaCardView` | View | 不直接复用整卡，但复用其视觉组织和封面 / 元信息处理策略 | 参考复用 |

### 3.3 组件接口定义

```swift
struct RankingHomeView: View {
    @EnvironmentObject private var router: NavigationRouter
    @StateObject private var viewModel: RankingViewModel

    var body: some View {
        RankingHomeContent(
            primarySelection: viewModel.selectedContentType,
            secondarySelection: viewModel.selectedRankingType,
            state: viewModel.viewState,
            isAppending: viewModel.isAppending,
            appendErrorMessage: viewModel.appendErrorMessage,
            onSelectContentType: { type in
                Task { await viewModel.selectContentType(type) }
            },
            onSelectRankingType: { type in
                Task { await viewModel.selectRankingType(type) }
            },
            onTapDrama: { drama in
                guard let route = RankingRouteBuilder.playRoute(for: drama) else { return }
                router.navigate(to: route)
            },
            onTapBooking: { drama in
                Task { await viewModel.book(drama: drama) }
            },
            onLoadMore: {
                Task { await viewModel.loadMoreIfNeeded() }
            },
            onRetry: {
                Task { await viewModel.retry() }
            }
        )
        .task { await viewModel.loadIfNeeded() }
        .onReceive(viewModel.$routeEffect.compactMap { $0 }) { effect in
            handle(effect: effect)
        }
    }
}
```

```swift
struct RankingDramaCardView: View {
    let drama: RankingDrama
    let rankingType: RankingType
    let rankIndex: Int
    let onTapCard: () -> Void
    let onTapBooking: () -> Void

    var body: some View {
        Button(action: onTapCard) {
            // 使用按钮或 contentShape 承接整卡点击
        }
        .buttonStyle(.plain)
        .overlay(alignment: .topLeading) {
            RankingBadgeView(rank: rankIndex)
        }
        .safeAreaInset(edge: .bottom) {
            if rankingType == .booking {
                RankingBookingButton(
                    booked: drama.isBooked,
                    isSubmitting: drama.isBookingSubmitting,
                    action: onTapBooking
                )
            }
        }
    }
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| `RankingViewModel` -> `RankingHomeView` | `@Published` + `@StateObject` | 当前一级 / 二级 Tab、页面状态、分页态、预约态 |
| `RankingHomeView` -> 子组件 | 构造参数 | Tab 选择、列表数据、错误文案、按钮状态 |
| 子组件 -> `RankingHomeView` | closure callback | 切换 Tab、重试、加载更多、点击排行项、点击预约 |
| 页面 -> `NavigationRouter` | `@EnvironmentObject` | 点击排行项进入 `.player(videoId:)`；未来消费统一登录拦截 effect |
| `RankingViewModel` -> App 层 | `routeEffect` / callback | 预约时未登录，抛出 `requireLogin(source: .ranking(...))` 交由统一登录承接消费 |

### 3.5 交互与复用策略

- 搜索发现页的「排行」快捷入口不变，仍通过 `QuickEntryType.ranking -> .rankingHome` 进入排行页。
- 排行页主体不复用 `DiscoveryPlaceholderView`，而是落地新的真实页面。
- 排行项点击采用整卡点击或主区域点击，两种方式都统一落到 `RankingRouteBuilder.playRoute(for:) -> .player(videoId: drama.id)`。
- 由于本期列表项需要额外展示序号、榜单指标与预约按钮，不能直接复用 `HomeDramaCardView` 整体，但封面、元信息排版、按钮层级应保持视觉一致，避免首页 / 搜索 / 排行三套卡片语义分裂。
- 预约榜中仅显示一个主动作按钮，不再额外引入 Toast SDK、HUD SDK 或弹层依赖；轻反馈优先使用系统现有能力（如内联文案、小型 footer 文案或现有可复用的 SwiftUI 轻提示实现）。

### 3.6 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | `ScrollView + LazyVStack`，顶部 Tab 横向自适应 | 小屏设备下二级 Tab 保持单行可滚动或等宽压缩，不截断核心文字 |
| Dynamic Type | 使用系统语义字体，不写死字号 | 榜单值、标题、标签支持换行与截断 |
| 深色模式 | 复用系统语义色与 `DesignTokens` | 序号 badge、榜单值、按钮状态在深色模式下保持对比度 |
| 安全区域 | 依赖 `NavigationStack` 默认处理 | 不侵入底部 tab 安全区；分页 footer 与按钮不遮挡 home indicator |
| 长列表滚动 | `ScrollViewReader` / 锚点回顶 | 切换任一 Tab 后列表回到顶部并重置分页 |
| 图片缺失 | 沿用首页封面占位图策略 | `cover_url` 为空或失效时不影响列表渲染 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 View | 职责 |
|-----------|----------|------|
| `RankingViewModel` | `RankingHomeView` | 管理默认加载、一级/二级 Tab 切换、分页、并发去重、预约、登录拦截 effect |

### 4.2 状态定义

```swift
@MainActor
final class RankingViewModel: ObservableObject {
    enum ViewState: Equatable {
        case loading
        case content([RankingDrama])
        case empty
        case error(String)
    }

    enum RouteEffect: Equatable {
        case requireLogin(RankingLoginContext)
    }

    @Published private(set) var selectedContentType: RankingContentType = .all
    @Published private(set) var selectedRankingType: RankingType = .hot
    @Published private(set) var viewState: ViewState = .loading
    @Published private(set) var isAppending = false
    @Published private(set) var appendErrorMessage: String?
    @Published private(set) var routeEffect: RouteEffect?

    private var currentPage = 1
    private var totalPages = 1
    private var hasLoaded = false
    private var requestToken: UUID?
    private var currentItems: [RankingDrama] = []
    private var bookingInFlightIDs: Set<String> = []
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `selectedContentType` | `RankingContentType` | `.all` | 一级 Tab 选择，符合 shared design 默认值 |
| `selectedRankingType` | `RankingType` | `.hot` | 二级 Tab 选择，符合 shared design 默认值 |
| `viewState` | `ViewState` | `.loading` | 首屏 / 切换 / 重试后的主页面状态 |
| `isAppending` | `Bool` | `false` | 分页加载更多时控制列表尾部 loading |
| `appendErrorMessage` | `String?` | `nil` | 加载更多失败时展示轻量尾部错误，不清空已有列表 |
| `currentPage` | `Int` | `1` | 当前已成功加载页码 |
| `totalPages` | `Int` | `1` | 服务端返回总页数，用于判定是否可继续分页 |
| `currentItems` | `[RankingDrama]` | `[]` | 当前维度下已加载并渲染的数据 |
| `requestToken` | `UUID?` | `nil` | 区分当前有效请求，避免旧响应覆盖新状态 |
| `bookingInFlightIDs` | `Set<String>` | `[]` | 同一列表项预约请求在途去重 |
| `routeEffect` | `RouteEffect?` | `nil` | 排行模块不自建登录页，通过 effect 交给应用层统一拦截 |
| `hasLoaded` | `Bool` | `false` | 防止页面重复首次初始化 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | View 层表现 |
|---------|---------|-----------|
| Loading | 首次进入或切换 Tab 后请求第一页 | 页面主体显示 loading，占位保留双层 Tab |
| Success (有数据) | `viewState == .content(items)` 且 `items.count > 0` | 展示排行列表；预约榜显示预约按钮 |
| Empty | `viewState == .empty` | 展示「当前榜单暂无内容」，保留双层 Tab 可继续切换 |
| Error (第一页失败) | `viewState == .error(message)` | 展示整页错误态与重试按钮 |
| Appending | `isAppending == true` | 列表尾部 loading，不影响已加载内容 |
| Append Error | `appendErrorMessage != nil` | 列表底部轻量错误提示；允许再次触底重试 |
| Booking Submitting | `bookingInFlightIDs.contains(drama.id)` | 当前项预约按钮 disabled + loading |
| Require Login | 匿名用户点击预约 | 不修改当前列表成功态，抛出登录拦截 effect |

### 4.5 状态机与动作约束

- 默认状态固定为 `contentType = .all`、`rankingType = .hot`、`page = 1`，与 shared design 一致。
- 切换一级 Tab 时保留当前二级 Tab，仅重置页码和列表。
- 切换二级 Tab 时保留当前一级 Tab，仅重置页码和列表。
- 所有维度切换都必须：`clear currentItems -> reset currentPage=1 -> viewState=.loading -> 发起新请求`。
- 首屏和切换后的第一页请求失败进入整页错误态；加载更多失败仅影响尾部状态，保留已加载列表。
- 同一查询维度在途时不可重复发同页请求；通过 `requestToken` 或等价请求序号确保旧响应不覆盖新状态。
- 预约提交仅对预约榜可触发；其他榜单不渲染预约按钮，不进入预约状态机。
- 同一 drama 在 `bookingInFlightIDs` 中时再次点击直接忽略。
- 预约成功后仅局部更新当前项 `isBooked=true` 与 `bookingCount`；不强制整页 reload。
- 匿名用户点击预约时，不直接请求 `POST /book`，而是抛出 `requireLogin(context)` effect，由统一登录拦截消费；若登录能力尚未落地，coding 阶段可先接到受控占位承接，但不在排行模块内部新建登录页面。

---

## 5. Navigation 路由设计

### 5.1 导航方案

继续使用现有 `NavigationStack + NavigationRouter + AppRoute`。排行页不新增新的顶级导航语义，仍通过既有 `.rankingHome` 进入；排行项点击继续复用 `.player(videoId:)`，其公开 route name 仍为 `play`。

### 5.2 路由清单

| 路由标识 | 目标页面 | 参数 | 导航方式 | 说明 |
|---------|---------|------|---------|------|
| `.rankingHome` | `RankingHomeView` | 无 | Push | 搜索发现页「排行」入口与 `djsdrama://ranking` 的承接页 |
| `.player(videoId:)` | `PlayerView` | `videoId` | Push | 排行项点击后的播放承接链路；对外语义仍为 `play` |
| `RouteEffect.requireLogin` | 统一登录拦截入口（由 App 层消费） | `RankingLoginContext` | Push / Sheet / Placeholder，由登录能力决定 | 本 PRD 不在排行模块内部设计新的登录页面 |

### 5.3 路由管理

```swift
enum RankingRouteBuilder {
    static func playRoute(for drama: RankingDrama) -> AppRoute? {
        guard !drama.id.isEmpty else { return nil }
        return .player(videoId: drama.id)
    }
}

struct RankingLoginContext: Equatable {
    let source: String         // "ranking"
    let contentType: RankingContentType
    let rankingType: RankingType
    let dramaID: String
}
```

设计要点：

- 不新增 `.play` / `.rankingDetail` / `.bookingConfirm` 等 route，避免偏离 shared design。
- `RankingLoginContext` 只作为统一登录拦截的来源信息载体，便于登录成功后回到原维度组合。
- `NavigationRouter` 现有 `selectedTab == .home` 逻辑继续有效，排行页仍归属 Home Tab。

### 5.4 Deep Link 处理

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `djsdrama://ranking` | `.rankingHome` | 无 |
| `djsdrama://play/{id}` | `.player(videoId:)` | 复用现有逻辑 |

约束：

- `DeeplinkHandler` 无需新增 host，继续复用当前 `ranking -> .rankingHome` 解析。
- 本 PRD 不新增预约态 deeplink，也不新增登录态 deeplink。
- 排行页点击列表项后进入 `play` 承接即可，播放器当前仍为占位实现，验收标准仅为进入链路与参数透传正确。

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | 现有 `APIClient` + `URLSession` | 不引入 Alamofire 或其他第三方网络库 |
| 请求构建 | 扩展 `DramaEndpoints` | 继续使用 `APIEndpoint` 协议 |
| 响应解析 | `Codable` + `JSONDecoder.convertFromSnakeCase` | 兼容 `cover_url`、`content_type`、`page_size` 等字段 |
| 错误处理 | `APIError` | 统一承接网络、服务端、解码和取消错误 |
| 认证注入 | 复用现有 header / token 注入入口（若已有） | 排行列表公开读取不依赖登录；预约接口在 auth 能力就绪后由统一层注入认证 |

### 6.2 API 端点定义

```swift
struct GetRankingsEndpoint: APIEndpoint {
    typealias Response = RankingListResponseDTO

    let query: RankingQuery

    var path: String { "/api/dramas/rankings" }
    var method: HTTPMethod { .get }
    var queryItems: [URLQueryItem]? {
        [
            URLQueryItem(name: "type", value: query.type.rawValue),
            URLQueryItem(name: "contentType", value: query.contentType.requestValue),
            URLQueryItem(name: "page", value: String(query.page)),
            URLQueryItem(name: "pageSize", value: String(query.pageSize))
        ]
    }
}

struct BookDramaEndpoint: APIEndpoint {
    typealias Response = BookDramaResponseDTO

    let dramaID: String

    var path: String { "/api/dramas/\(dramaID)/book" }
    var method: HTTPMethod { .post }
    var body: EmptyRequestBody? { nil }
}
```

### 6.3 Repository / DataSource 接线

- 在 `DramaRepositoryProtocol` 上新增：
  - `fetchRankings(query: RankingQuery) async throws -> RankingPage`
  - `bookDrama(id: String) async throws -> BookDramaResult`
- `DramaRemoteDataSource` 新增：
  - `fetchRankings(query:) async throws -> RankingListResponseDTO`
  - `bookDrama(id:) async throws -> BookDramaResponseDTO`
- `DramaRepository` 负责：
  - 将 `RankingDramaDTO` 转换为 `RankingDrama`
  - 将分页 DTO 转换为领域分页对象
  - 将 `BookDramaResponseDTO` 转换为 `BookDramaResult`
- Presentation 只依赖 UseCase，不直接接触 DTO 与 `APIClient`。

### 6.4 响应与错误策略

- `GET /api/dramas/rankings` 成功响应沿用现有资源体结构：`{ data, pagination }`。
- `POST /api/dramas/:id/book` 成功响应沿用共享方案定义：`{ drama_id, booked, booking_count }`。
- 错误响应继续由后端统一输出 `{ error: { code, message } }`，iOS 端通过现有 `APIClient` 错误解码承接。
- 不做自动重试：
  - 首屏失败由页面 `retry()` 手动触发。
  - 分页失败通过再次触底或显式尾部重试触发。
  - 预约失败允许用户再次点击按钮触发重试。

### 6.5 请求策略

| 场景 | 策略 | 说明 |
|------|------|------|
| 首次进入排行页 | 自动请求第一页 | 默认 `all + hot + page=1 + pageSize=10` |
| 切换一级 Tab | 取消旧请求 / 忽略旧响应，重置分页后请求第一页 | 保持当前二级 Tab |
| 切换二级 Tab | 取消旧请求 / 忽略旧响应，重置分页后请求第一页 | 保持当前一级 Tab |
| 列表触底 | 仅在 `currentPage < totalPages` 且无分页请求在途时发下一页 | 防止重复加载 |
| 预约提交 | 同项只允许一个请求在途 | 匿名态不发请求，直接登录拦截 |

### 6.6 网络状态监听

- 本期不新增 `NWPathMonitor`。
- 断网 / 超时 / 服务异常统一通过 `APIError` 映射到页面错误或尾部错误。
- 排行模块不实现离线缓存，避免旧榜单数据误导排序结果。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| 排行当前列表 | 内存状态 | `RankingViewModel.currentItems` | 页面生命周期 | 首版不做离线缓存，避免 stale ranking |
| 当前一级 / 二级 Tab 选择 | 内存状态 | `selectedContentType` / `selectedRankingType` | 页面生命周期 | 页面返回时可丢失；若未来需跨会话恢复再单独设计 |
| 预约按钮提交中状态 | 内存状态 | `bookingInFlightIDs` | 请求生命周期 | 避免重复提交 |
| 登录回跳上下文 | 临时内存 / 统一登录模块持有 | `RankingLoginContext` | 登录承接链路生命周期 | 由统一登录模块消费，不由排行模块持久化 |

### 7.2 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| 排行列表数据 | 不持久化，仅页面内存态 | 页面生命周期 | 退出页面即释放 |
| 封面图片 | 交给 `AsyncImage` / URLSession 默认缓存 | 系统默认 | 系统缓存策略 |
| 预约成功状态 | 以内存局部更新为主 | 页面生命周期 | 刷新第一页后以服务端 `is_booked` 为准 |

### 7.3 数据迁移策略

- 本期不引入 CoreData、UserDefaults、Keychain 的新增排行存储，无迁移负担。
- 后续如需要保留用户最后浏览的排行维度，应新增独立的轻量持久化 key，并在设计评审中重新确认，不在本期范围内。

---

## 8. 配置与环境

| 配置项 | 管理方式 | iOS 端策略 | 说明 |
|--------|---------|-----------|------|
| API Base URL | `xcconfig + Info.plist` | 继续通过 `AppConfig.apiBaseURL()` 读取 | 不在排行模块内硬编码环境地址 |
| Deeplink Scheme | `Info.plist` | 继续使用 `djsdrama://` | 不新增新 scheme |
| Auth Token / 登录态 | 统一认证能力注入 | 排行读取不依赖；预约接口通过统一能力注入 | 本 PRD 不自建 token 管理 |
| Feature Flag | 无新增 | 不新增排行开关 | 本期直接交付 |
| 第三方依赖 | 无新增 | 保持现有工程能力 | 明确不新增开源依赖 |

> 说明：所有常量均通过领域枚举、`DesignTokens` 或 `AppConfig` 管理；不硬编码固定环境地址、token 或第三方服务参数。

---

## 9. API 调用清单

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| `GET /api/dramas/rankings?type&contentType&page&pageSize` | 首次进入排行页、切换任一 Tab、加载更多、登录回跳后刷新 | `RankingViewModel` 当前查询状态 | 更新列表、分页状态、空态 / 成功态 | 首屏失败进入错误态；分页失败展示尾部错误 |
| `POST /api/dramas/:id/book` | 预约榜点击预约按钮且已登录 | 当前点击项 `drama.id` | 局部更新 `is_booked` 和 `booking_count` | 401 触发登录拦截；其余错误保持原按钮状态并提示可重试 |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | iOS 端实现方式 |
|---------|---------------|---------------|
| 默认选择 | 首次进入固定为 `contentType=all + type=hot + page=1` | `RankingViewModel` 默认状态与首次 `loadIfNeeded()` 固定使用该组合 |
| 双维度切换 | 切换一级 Tab 保留二级 Tab；切换二级 Tab 保留一级 Tab | `selectContentType(_:)` / `selectRankingType(_:)` 分别只改一维 |
| 分页重置 | 任一维度切换时清空旧列表并回到第一页 | 统一调用 `reload(queryReset: true)` |
| 请求去重 | 同一页请求在途不重复；旧结果不得覆盖新状态 | `requestToken` + 请求中标记 + 结果校验 |
| 展示指标映射 | 热榜展示 `play_count`，推荐榜展示 `recommendation_score`，预约榜展示 `booking_count` + 按钮 | `RankingMetricView` 按 `selectedRankingType` 渲染；预约榜显示 `RankingBookingButton` |
| 空态策略 | 空列表返回 200 + 空数组，端侧展示空态 | `viewState = .empty`，文案为“当前榜单暂无内容” |
| 预约幂等 | 重复预约返回成功态，不重复累加 | 成功后本地直接更新为 `isBooked=true`；重复点击在端侧先 disabled |
| 登录拦截 | 浏览无需登录；点击预约才检查登录 | `book(drama:)` 中先判定登录态，不满足时发出 `requireLogin` effect |
| 播放跳转 | 点击排行项复用现有 `play` 路由语义 | `RankingRouteBuilder.playRoute(for:) -> .player(videoId:)`，不新增播放器路由 |

---

## 11. 边界与错误处理

### 11.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| 输入层 | 枚举约束 + 本地守卫 | Tab 选择为固定枚举，不允许非法字符串进入请求层 |
| 网络层 | `APIClient` -> `APIError` | 统一处理网络、服务端、解码、取消错误 |
| ViewModel | 显式状态机 + `do/catch` | 区分首屏失败、分页失败、预约失败、登录拦截 |
| View 层 | 内联错误页 / 尾部错误 / disabled 按钮 | 不引入第三方 Toast / HUD 依赖 |
| 路由层 | `RankingRouteBuilder` 守卫 | `drama.id` 为空时不执行播放导航 |

### 11.2 错误码映射表

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `VALIDATION_ERROR` | 榜单参数异常，请稍后重试 | 首屏错误态或轻量提示；回退到上一次有效状态 |
| `UNAUTHORIZED` | 请先登录后再预约 | 不更新按钮状态，触发统一登录拦截 |
| `FORBIDDEN` | 当前不可执行该预约操作 | 轻量提示，按钮恢复可点击 |
| `NOT_FOUND` | 当前短剧不存在或已下线 | 播放点击时忽略导航 / 预约时提示并刷新当前页 |
| `CONFLICT` | 当前状态已变化，请刷新后重试 | 预约按钮恢复可点击，可手动重试 |
| `INTERNAL_ERROR` | 服务开小差了，请稍后重试 | 首屏错误态 / 分页尾部错误 / 预约失败提示 |
| `SERVICE_UNAVAILABLE` | 服务暂不可用，请稍后重试 | 同上 |
| `NETWORK_ERROR`（端侧归类） | 网络异常，请检查后重试 | 页面错误态 / 尾部错误 / 预约失败提示 |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| 快速切换一级 / 二级 Tab | 旧请求晚于新请求返回 | 使用 `requestToken` 或请求序号，只接收最后一次有效结果 | 🔴 |
| 连续触底加载更多 | 同一页多次触发分页 | `isAppending == true` 时直接忽略 | 🔴 |
| 首屏为空列表 | `data=[]` 且 `page=1` | 展示空态，不显示错误 | 🔴 |
| 超大页码 | `page` 合法但结果为空 | 视为正常分页结束，不进入错误态 | 🟡 |
| 封面为空 | `cover_url == nil / ""` | 显示统一占位图 | 🟡 |
| 排行项 ID 为空 | 服务端数据异常 | 禁止进入播放页；预约按钮 disabled | 🔴 |
| 匿名点击预约 | 未登录 | 不发 `POST /book`，抛出 `requireLogin` effect | 🔴 |
| 重复点击预约 | 当前项已在提交中 | 按钮 disabled，忽略重复点击 | 🔴 |
| 已预约再次点击 | `is_booked == true` | 按钮显示“已预约”并不可点击 | 🔴 |
| 登录回跳 | 登录成功返回排行页 | 依据 `RankingLoginContext` 恢复维度并刷新第一页或局部刷新当前项 | 🟡 |
| 播放器仍为占位实现 | 排行项点击后进入 `PlayerView` | 以“成功进入 play 路由承接链路”为验收标准，不要求真实播放 | 🔴 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `RankingHomeView` | 首屏 loading | 列表内容 | 空榜单文案 | 整页错误 + 重试 | 无 |
| `RankingListView` | 尾部 loading | 已加载列表追加 | 不适用 | 尾部轻量错误 | 无 |
| `RankingDramaCardView` | 预约按钮 loading | 正常卡片 | 不适用 | 预约失败恢复按钮 | ID 异常时按钮 disabled |
| `RankingPrimaryTabBar` / `RankingSecondaryTabBar` | 始终可见 | 选中态高亮 | 始终可见 | 始终可见 | 无 |

---

## 12. 测试策略

### 12.1 测试范围

遵循 `ios/CLAUDE.md`：使用 **Swift Testing** 编写单元测试，网络层通过 `URLProtocol` mock，不发起真实网络请求。

| 测试类型 | 覆盖内容 | 目标 |
|---------|---------|------|
| ViewModel 单元测试 | 默认加载、双层 Tab 切换、分页、并发去重、预约、登录拦截 | 覆盖 PRD-05 所有关键状态流转 |
| Data 层测试 | rankings / book endpoint、DTO 解码、错误包体、分页映射 | 保证 shared design 契约正确接线 |
| Router / Deeplink 测试 | `.rankingHome` 保持承接、排行项点击仍进入 `.player(videoId:)` | 锁定 canonical 导航语义 |
| 轻量组件验证 | SwiftUI Preview | 仅用于开发观察，不替代自动化 |

### 12.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| I-01 | 默认进入加载热榜第一页 | 排行接口返回第一页 10 条 | `loadIfNeeded()` | `selectedContentType == .all`、`selectedRankingType == .hot`、列表展示第一页 | ViewModel |
| I-02 | 切换一级 Tab 保留二级 Tab | 当前为 `all + recommend` | 切换到 `ai` | 请求 `contentType=ai&type=recommend&page=1`，旧列表被清空 | ViewModel |
| I-03 | 切换二级 Tab 保留一级 Tab | 当前为 `liveAction + hot` | 切换到 `booking` | 请求 `contentType=live_action&type=booking&page=1` | ViewModel |
| I-04 | 空榜单展示空态 | 服务端返回 `data=[]` | 首次加载完成 | `viewState == .empty` | ViewModel |
| I-05 | 首屏失败展示错误态 | 接口返回网络错误 | 首次加载 | `viewState == .error`，可触发 `retry()` | ViewModel |
| I-06 | 加载更多成功追加数据 | 第 1 页已有数据，且仍有下一页 | `loadMoreIfNeeded()` | 新数据追加到尾部，页码递增 | ViewModel |
| I-07 | 加载更多失败不清空已加载数据 | 第 2 页请求失败 | `loadMoreIfNeeded()` | 已有列表保持不变，显示 `appendErrorMessage` | ViewModel |
| I-08 | 快速切换 Tab 仅保留最后结果 | 两个请求乱序返回 | 连续切换不同 Tab | 仅最后一次选择生效 | ViewModel |
| I-09 | 匿名点击预约触发登录拦截 | 当前未登录 | 点击预约按钮 | 不调用 book API，发出 `requireLogin` effect | ViewModel |
| I-10 | 已登录预约成功后局部更新 | 预约接口成功返回新 `booking_count` | 点击预约 | 当前项 `isBooked == true` 且预约数更新 | ViewModel |
| I-11 | 预约请求在途时忽略重复点击 | 同一 drama 正在预约 | 连续点击两次 | 只发送一次请求 | ViewModel |
| I-12 | 排行项点击进入播放页承接 | 列表项 `drama.id` 有效 | 点击卡片 | `RankingRouteBuilder.playRoute(for:) == .player(videoId: id)` | Router |
| I-13 | deeplink 仍命中排行页 | URL 为 `djsdrama://ranking` | 调用 `handleDeepLink` | 返回 `.rankingHome` | Deeplink |
| I-14 | rankings endpoint query 正确 | `all + hot + page=1 + pageSize=10` | 构造 endpoint | path 和 queryItems 与 shared design 一致 | Data |
| I-15 | booking endpoint path 正确 | dramaId 已知 | 构造 endpoint | `POST /api/dramas/{id}/book` | Data |
| I-16 | 预约 401 映射为登录拦截 | book API 返回 401 | 点击预约 | ViewModel 触发登录拦截而非错误成功态 | ViewModel |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| 排行 / 预约接口 | `URLProtocol` Stub 或 `MockDramaRepository` | Data 层验证 endpoint，ViewModel 层验证状态机 |
| 登录态 | 协议抽象 / in-memory fake | 不引入真实登录能力，只验证匿名 / 已登录分支 |
| 导航 | 断言 `AppRoute` / `NavigationRouter.pathsByTab` | 不依赖 UI 自动化 |
| 分页与乱序响应 | 受控异步 mock | 用于验证旧响应不覆盖新状态 |

### 12.4 最小验收结论

iOS 端最小自动化验收应至少覆盖：

- 默认 `全部 + 热榜` 第一页加载。
- 一级 / 二级 Tab 切换保留另一维且重置分页。
- 分页成功追加、失败不清空。
- 预约按钮的匿名拦截、成功更新、重复点击去重。
- deeplink `djsdrama://ranking` 保持不变。
- 排行项点击仍走现有 `play` 语义，对应 `.player(videoId:)`。

---

## 13. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | — | — | 明确不新增第三方依赖；网络、分页状态、图片、测试均复用系统与现有工程能力 |

---

## 14. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 排行切换请求乱序导致脏数据覆盖 | 列表一致性 | 🔴 | 中 | 用 `requestToken` / 请求序号校验只消费最后一次有效结果 | 最低限度在新请求前清空旧结果并忽略非当前查询结果 |
| 预约能力依赖统一登录拦截，但当前登录模块未落地 | 预约闭环 | 🔴 | 高 | 排行模块输出 `requireLogin` effect，不自建登录页；coding 阶段接受统一占位承接 | 登录能力未就绪时，保留受控拦截占位而不影响排行浏览 |
| 排行卡片直接复用首页卡片会缺少榜单特有信息 | UI 表达与交互 | 🟡 | 高 | 新建 `RankingDramaCardView`，但复用首页封面和元信息组织策略 | 如实现复杂，可先保证序号 + 指标 + 播放动作优先落地 |
| 预约成功后仅局部更新可能与服务端真实排序有细微偏差 | 预约榜局部排序一致性 | 🟡 | 中 | 首版只更新当前项状态与数值，不做复杂本地重排；必要时刷新第一页 | 若出现排序争议，改为预约成功后重新请求第一页 |
| 播放器当前仍为占位实现 | 端到端体验 | 🟡 | 高 | 文档与验收明确以“进入 play 路由承接链路”为准 | 保持播放器范围不外溢到本 PRD |
| 不做离线缓存导致返回页面时需重新拉取 | 性能与体验 | 🟢 | 中 | 首版接受页面级内存态，不持久化排行结果 | 后续如需缓存再单独评审 |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/features/video-player/index.md` | 入口与路由、已知限制 | iOS 排行项点击只需复用现有 `play` 语义；当前播放器仍为承接范围，不在本 PRD 扩展 |
| `wiki/features/deeplink/index.md` | Deeplink 格式、iOS 流程 | `djsdrama://ranking` 已存在，iOS `play` 为 canonical 播放语义 |
| `wiki/architecture/overview.md` | 当前首页与播放器承载结构 | iOS 继续使用 `TabView + NavigationStack`；Home Tab 承载首页与播放器主路径 |
| `wiki/api/dramas.md` | 当前分页契约 | `GET /api/dramas` 已有 `data + pagination` 结构和大页码空结果行为，可供 rankings 复用 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `ios/CLAUDE.md` | iOS 端必须遵循 SwiftUI、MVVM + Clean Architecture、URLSession、NavigationStack、Swift Testing 约束 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | `.rankingHome` 已存在，对外 route name 为 `ranking`；`.player(videoId:)` 对外公开名为 `play` |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | Home Tab path 管理与 route push 行为可直接复用 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 当前 `.rankingHome` 仍注册到 `DiscoveryPlaceholderView(kind: .ranking)`，是本期真实页面接线点 |
| `ios/ShortDrama/Sources/App/DeeplinkHandler.swift` | `djsdrama://ranking` 已解析到 `.rankingHome` |
| `ios/ShortDrama/Sources/Features/Search/Views/SearchHomeView.swift` | 搜索发现页已有「排行」快捷入口导航 |
| `ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift` | `QuickEntryType.ranking -> .rankingHome` 已稳定存在 |
| `ios/ShortDrama/Sources/Features/Search/Views/DiscoveryPlaceholderView.swift` | 当前排行承接页仍为占位实现 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 首页使用 `NavigationRouter` 与 `HomeRouteBuilder` 组织播放 / 详情跳转 |
| `ios/ShortDrama/Sources/Features/Home/Views/Components/HomeDramaCardView.swift` | 首页卡片的封面与元信息排版可供排行卡片参考复用 |
| `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` | 当前播放器页面仍为占位展示 `videoId`，需在方案中限制排行点击验收边界 |
| `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | 播放页接收 `videoId` 作为承接参数 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 当前仅有首页 / 详情 / 搜索 / 热搜协议，需要扩展排行与预约能力 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 当前网络数据源模式与 endpoint 定义可直接扩展到排行接口 |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 当前 DTO -> Entity 映射位置 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 当前 URLSession 网络栈和错误解码可继续复用 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 当前已覆盖 `.rankingHome` 属于 Home Tab 与路由公共名规则，可继续扩展 |
| `ios/ShortDrama/Tests/ViewModelTests/SearchResultViewModelTests.swift` | 提供 iOS 端 ViewModel 并发、请求去重测试风格参考 |
| `docs/specs/2026-07-26-prd-04-search-discovery/design-ios.md` | 搜索发现的 iOS 设计文档风格、路由与组件设计写法参考 |
| `docs/specs/2026-07-27-prd-05-ranking/spec.md` | PRD-05 用户故事、边界场景、排行 / 预约 / 播放承接需求 |
| `docs/specs/2026-07-27-prd-05-ranking/design.md` | shared API、数据模型、共享状态机和跨端约束 |
