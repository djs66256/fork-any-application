# iOS 端技术方案：PRD-04 搜索发现

> 创建日期：2026-07-26
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

PRD-04 在现有 `TabView + NavigationStack + NavigationRouter` 壳体上，为 iOS 新增两类能力：

1. 从首页进入的搜索发现页。
2. 由关键词驱动的搜索结果页，以及排行 / 分类 / 新剧 / 演员承接页。

实现继续遵循 `ios/CLAUDE.md` 中约束的 **MVVM + Clean Architecture（Core → Domain → Data → Presentation）**，不引入第三方依赖。

```text
HomeView
  -> toolbar search button
     -> NavigationRouter.navigate(to: .searchHome)

SearchHomeView
  -> observes SearchHomeViewModel
     -> LoadSearchHistoryUseCase
        -> SearchHistoryRepositoryProtocol
           -> UserDefaultsSearchHistoryRepository
     -> FetchHotSearchesUseCase
        -> DramaRepositoryProtocol.fetchHotSearches()
           -> DramaRemoteDataSource.fetchHotSearches()
              -> APIClient.request(DramaEndpoints.GetHotSearches)
                 -> GET /api/dramas/hot-search
  -> submit(query / history / hot search)
     -> NavigationRouter.navigate(to: .searchResult(query: normalizedQuery))

SearchResultView
  -> observes SearchResultViewModel
     -> SearchDramasUseCase.execute(query:page:pageSize)
        -> DramaRepositoryProtocol.searchDramas(...)
           -> DramaRemoteDataSource.searchDramas(...)
              -> APIClient.request(DramaEndpoints.SearchDramas)
                 -> GET /api/dramas/search?q=...&page=1&pageSize=10
  -> on successful response only
     -> SaveSearchHistoryUseCase.execute(normalizedQuery)
  -> renders Loading / Content / Empty / Error
  -> card actions reuse existing semantics
     -> 观看 -> .player(videoId: drama.id)
     -> 详情 -> .dramaDetail(dramaId: drama.id)
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 首页右上角新增搜索入口；不改变首页 Feed 主体结构 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 扩展搜索发现、搜索结果、排行、分类、新剧、演员承接页路由 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 支持新增路由入栈；继续保持 Home Tab 承载搜索相关页面 |
| `ios/ShortDrama/Sources/App/DeeplinkHandler.swift` | 修改 | 扩展 `djsdrama://search`、`djsdrama://search/result/{query}`、`djsdrama://ranking`、`djsdrama://classification`、`djsdrama://new-releases`、`djsdrama://actors` |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 注册搜索相关页面与占位承接页 |
| `ios/ShortDrama/Sources/Features/Home/*` | 轻微修改 / 可抽取组件 | 抽取首页已存在的列表卡片视图，供搜索结果页复用交互语义 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 修改 | 扩展搜索与热搜读取能力 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 增加搜索 / 热搜 API 请求 |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 修改 | 承接搜索结果与热搜 DTO → Entity 映射 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 修改 | 保持 URLSession 实现，同时兼容 shared design 约定的错误包体 |
| `ios/ShortDrama/Tests/*` | 修改 / 新增 | 补齐 ViewModel、Router、Deeplink、UserDefaults 持久化、Data 层测试 |

### 1.2 分层职责

| 层级 | 新增/扩展对象 | 职责 |
|------|--------------|------|
| Core | `APIClient` 错误解码兼容、沿用 `AppConfig` | 统一网络请求与环境配置 |
| Domain | `HotSearchItem`、`SearchHistoryItem`、`QuickEntryType`；搜索 / 历史用例；仓库协议扩展 | 纯业务模型与行为编排 |
| Data | 搜索 / 热搜 endpoint、DTO、本地历史 repository | 远端和本地数据访问实现 |
| Presentation | `SearchHomeView`、`SearchResultView`、占位承接页、对应 ViewModel | SwiftUI 页面、状态机和导航交互 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增 `.searchHome`、`.searchResult(query:)`、`.rankingHome`、`.classificationHome`、`.newReleases`、`.actorHub` |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 新增搜索相关 route 的 push 规则，保持 `owningTab == .home` |
| `ios/ShortDrama/Sources/App/DeeplinkHandler.swift` | 修改 | 新增搜索和快捷入口 deeplink 解析 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 为新增 `AppRoute` 注册页面 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 首页导航栏右上角增加搜索按钮 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeDramaCardView`（建议从 `HomeView.swift` 抽出） | 抽取/复用 | 搜索结果页复用首页卡片视图与“观看 / 详情”动作语义 |
| `ios/ShortDrama/Sources/Features/Search/Views/SearchHomeView.swift` | 新增 | 搜索发现页根视图 |
| `ios/ShortDrama/Sources/Features/Search/Views/SearchResultView.swift` | 新增 | 搜索结果页根视图 |
| `ios/ShortDrama/Sources/Features/Search/Views/Components/*.swift` | 新增 | 搜索框、快捷入口、历史区、热搜区、结果列表、占位页组件 |
| `ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift` | 新增 | 搜索发现页状态机 |
| `ios/ShortDrama/Sources/Features/Search/ViewModels/SearchResultViewModel.swift` | 新增 | 搜索结果页状态机与重搜逻辑 |
| `ios/ShortDrama/Sources/Features/Search/Views/DiscoveryPlaceholderView.swift` | 新增 | `ranking/classification/new-releases/actors` 承接页；其中 `new-releases` / `actors` 首版固定为 Native 占位 |
| `ios/ShortDrama/Sources/Domain/Entities/HotSearchItem.swift` | 新增 | 热搜实体 |
| `ios/ShortDrama/Sources/Domain/Entities/SearchHistoryItem.swift` | 新增 | 本地历史实体 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/SearchHistoryRepositoryProtocol.swift` | 新增 | 本地历史读写协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/SearchDramasUseCase.swift` | 新增 | 搜索短剧用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchHotSearchesUseCase.swift` | 新增 | 获取热搜用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/LoadSearchHistoryUseCase.swift` | 新增 | 读取本地历史用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/SaveSearchHistoryUseCase.swift` | 新增 | 保存历史用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/ClearSearchHistoryUseCase.swift` | 新增 | 清空历史用例 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 增加 `searchDramas`、`fetchHotSearches` |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 修改 | 实现新增 repository 协议方法 |
| `ios/ShortDrama/Sources/Data/Repositories/UserDefaultsSearchHistoryRepository.swift` | 新增 | 基于 `UserDefaults` 的历史持久化实现 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 修改 | 兼容 `{ error: { code, message } }` 错误结构，同时继续支持旧结构 |
| `ios/ShortDrama/Tests/ViewModelTests/SearchHomeViewModelTests.swift` | 新增 | 搜索发现页状态与历史规则测试 |
| `ios/ShortDrama/Tests/ViewModelTests/SearchResultViewModelTests.swift` | 新增 | 搜索结果页 loading / empty / error / 重搜测试 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 补充新增 route 行为测试 |
| `ios/ShortDrama/Tests/ViewModelTests/DeeplinkHandlerTests.swift` | 修改 | 补充新增 deeplink 解析测试 |
| `ios/ShortDrama/Tests/DataTests/UserDefaultsSearchHistoryRepositoryTests.swift` | 新增 | 本地历史去重、裁剪、清空测试 |
| `ios/ShortDrama/Tests/DataTests/DramaRemoteDataSourceTests.swift` | 修改 | 搜索与热搜 endpoint、错误包体、DTO 解码测试 |

---

## 3. View 层设计

### 3.1 组件层级树

```text
HomeView
├── HomeFeedContent
└── ToolbarItem(.topBarTrailing)
    └── SearchEntryButton

SearchHomeView
├── SearchBarSection
│   ├── BackButton
│   ├── SearchTextField
│   └── SearchSubmitButton
├── QuickEntryGrid
│   └── QuickEntryCard (排行 / 新剧 / 分类 / 演员)
├── SearchHistorySection
│   ├── SectionHeader
│   ├── HistoryChipList
│   └── ClearHistoryButton
└── HotSearchSection
    ├── SectionHeader
    ├── HotSearchList
    ├── HotSearchRow
    └── RetryButton (仅热搜区局部失败时显示)

SearchResultView
├── SearchBarSection
├── SearchResultStateContainer
│   ├── LoadingView
│   ├── ErrorView
│   ├── EmptyView
│   └── SearchDramaListView
│       └── ReusedDramaCardView (ForEach)
└── OptionalInlineHint

DiscoveryPlaceholderView
├── PlaceholderIcon
├── Title
├── Description
└── OptionalPrimaryAction
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `SearchEntryButton` | View | 首页右上角搜索入口 | 否 |
| `SearchHomeView` | View | 搜索发现页根视图，承载输入、快捷入口、历史、热搜 | 否 |
| `SearchBarSection` | View | 搜索发现页 / 结果页共享顶部搜索栏 | 是 |
| `QuickEntryGrid` | View | 展示排行 / 新剧 / 分类 / 演员四个入口 | 否 |
| `SearchHistorySection` | View | 展示最近 10 条本地历史与清空动作 | 否 |
| `HotSearchSection` | View | 展示 Top 10 热搜及局部错误重试 | 否 |
| `SearchResultView` | View | 搜索结果页根视图 | 否 |
| `SearchDramaListView` | View | 结果列表容器，内部复用首页卡片视图 | 是 |
| `HomeDramaCardView`（抽取后） | View | 统一 Drama 卡片渲染；搜索结果页仅保留“观看 / 详情”两个动作 | 是 |
| `DiscoveryPlaceholderView` | View | 排行 / 分类 / 新剧 / 演员承接页；`new-releases` / `actors` 首版为 Native 占位页 | 否 |

### 3.3 组件接口定义

```swift
struct SearchHomeView: View {
    @EnvironmentObject private var router: NavigationRouter
    @StateObject private var viewModel: SearchHomeViewModel

    var body: some View {
        SearchHomeContent(
            query: viewModel.query,
            quickEntries: viewModel.quickEntries,
            historyItems: viewModel.historyItems,
            hotSearchState: viewModel.hotSearchState,
            canSubmit: viewModel.canSubmit,
            onQueryChange: viewModel.updateQuery(_:),
            onSubmit: { query in
                guard let normalized = viewModel.normalizedQuery(query) else { return }
                router.navigate(to: .searchResult(query: normalized))
            },
            onTapQuickEntry: { entry in
                router.navigate(to: viewModel.route(for: entry))
            }
        )
        .task { await viewModel.loadIfNeeded() }
    }
}
```

```swift
struct SearchResultView: View {
    @EnvironmentObject private var router: NavigationRouter
    @StateObject private var viewModel: SearchResultViewModel

    let initialQuery: String

    var body: some View {
        Group {
            switch viewModel.viewState {
            case .loading:
                SearchResultLoadingView()
            case .content(let dramas):
                SearchDramaListView(
                    dramas: dramas,
                    onPlay: { router.navigate(to: .player(videoId: $0.id)) },
                    onDetail: { router.navigate(to: .dramaDetail(dramaId: $0.id)) }
                )
            case .empty:
                SearchResultEmptyView(query: viewModel.submittedQuery)
            case .error(let message):
                SearchResultErrorView(message: message) { await viewModel.retry() }
            }
        }
        .task { await viewModel.load(initialQuery: initialQuery) }
    }
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| `SearchHomeViewModel` → `SearchHomeView` | `@Published` + `@StateObject` | 搜索发现页 query、历史、热搜、按钮状态 |
| `SearchResultViewModel` → `SearchResultView` | `@Published` + `@StateObject` | 结果列表、当前关键词、错误态 |
| 父 → 子 | 构造参数 / value object | 历史、热搜、快捷入口和结果列表传递 |
| 子 → 父 | closure callback | 点击搜索、历史词、热搜词、快捷入口 |
| 页面 → `NavigationRouter` | `@EnvironmentObject` | push 到结果页、播放页、详情页、承接页 |

### 3.5 视图复用策略

- 搜索结果列表不重新定义卡片交互语义，直接复用首页卡片视图或抽取出的共享卡片组件。
- 复用范围仅限现有字段与动作：封面、标题、描述、标签、集数、评分，以及“观看 / 详情”两个按钮。
- 不新增整卡点击跳转，不增加演员入口、收藏入口、更多菜单等新语义，避免偏离 `spec.md`。
- 首页卡片若当前仍为 `HomeView.swift` 私有子视图，建议本期抽为 `Features/Home/Views/Components/HomeDramaCardView.swift`，供 Home 与 Search 共用。

### 3.6 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | `ScrollView + LazyVStack` 与弹性网格 | 快捷入口使用两列自适应布局，列表保持单列 |
| Dynamic Type | 使用系统字体语义，不写死字号 | 历史标签与热搜行支持安全换行 / 截断 |
| 深色模式 | 复用系统语义色与 `DesignTokens` | 占位承接页、局部错误态在深色模式可读 |
| 安全区域 | 由 `NavigationStack` 默认处理 | 搜索页不侵入底部 Tab 安全区 |
| 键盘 | 搜索栏与滚动区域分层 | 提交搜索后收起键盘，避免遮挡结果首屏 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 View | 职责 |
|-----------|----------|------|
| `SearchHomeViewModel` | `SearchHomeView` | 管理 query、历史读取/清空、热搜加载、快捷入口配置和提交前校验 |
| `SearchResultViewModel` | `SearchResultView` | 管理关键词提交、搜索请求、成功后写历史、重搜、loading/empty/error 切换 |

### 4.2 状态定义

```swift
@MainActor
final class SearchHomeViewModel: ObservableObject {
    enum HotSearchState: Equatable {
        case idle
        case loading
        case content([HotSearchItem])
        case error(String)
    }

    @Published var query = ""
    @Published private(set) var historyItems: [SearchHistoryItem] = []
    @Published private(set) var hotSearchState: HotSearchState = .idle
    @Published private(set) var quickEntries: [QuickEntry] = QuickEntry.defaults
    @Published private(set) var isSubmitting = false

    var canSubmit: Bool { normalizedQuery(query) != nil && !isSubmitting }
}
```

```swift
@MainActor
final class SearchResultViewModel: ObservableObject {
    enum ViewState: Equatable {
        case loading
        case content([Drama])
        case empty
        case error(String)
    }

    @Published var draftQuery = ""
    @Published private(set) var submittedQuery = ""
    @Published private(set) var viewState: ViewState = .loading
    @Published private(set) var isRetrying = false

    private var requestTask: Task<Void, Never>?
    private var lastSubmittedQuery: String?
}
```

### 4.3 状态字段详情

#### `SearchHomeViewModel`

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `query` | `String` | `""` | 当前输入框内容 |
| `historyItems` | `[SearchHistoryItem]` | `[]` | 本地历史，按最近使用倒序，最多 10 条 |
| `hotSearchState` | `HotSearchState` | `.idle` | 热搜区块状态；失败不影响其余区块 |
| `quickEntries` | `[QuickEntry]` | 默认 4 项 | 排行 / 新剧 / 分类 / 演员入口配置 |
| `isSubmitting` | `Bool` | `false` | 防止重复点击搜索 |
| `hasLoaded` | `Bool` | `false` | 避免重复初始化加载 |

#### `SearchResultViewModel`

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `draftQuery` | `String` | `""` | 顶部可编辑搜索框内容 |
| `submittedQuery` | `String` | `""` | 当前结果列表对应的已提交关键词 |
| `viewState` | `ViewState` | `.loading` | 结果页统一状态 |
| `isRetrying` | `Bool` | `false` | 控制错误态重试按钮与文案 |
| `lastSubmittedQuery` | `String?` | `nil` | 避免同关键词重复触发并发请求 |
| `requestTask` | `Task<Void, Never>?` | `nil` | 新关键词提交时取消旧请求，仅保留最后一次有效结果 |

### 4.4 UI 状态建模

#### 搜索发现页

| UI 状态 | 判别条件 | View 层表现 |
|---------|---------|-----------|
| Initial Loading | 首次进入页面 | 先渲染搜索栏与快捷入口；热搜区显示 loading，占位读取历史 |
| Content | 历史读取完成且热搜成功/失败之一成立 | 搜索栏、快捷入口常驻；历史按有无显示；热搜显示榜单或错误区块 |
| Partial Error | `hotSearchState == .error` | 仅热搜区显示错误态 + 重试按钮，不影响搜索与历史 |

#### 搜索结果页

| UI 状态 | 判别条件 | View 层表现 |
|---------|---------|-----------|
| Loading | 初次搜索 / 重搜请求中 | 顶部保留当前关键词，内容区显示 loading |
| Content | 请求成功且列表非空 | 结果列表；卡片复用首页交互语义 |
| Empty | 请求成功且列表为空 | 展示“未找到相关短剧”空态 |
| Error | 请求失败 | 展示错误态与重试按钮；保留已输入关键词 |

### 4.5 状态机与动作约束

- 所有触发来源统一归并为 `submitSearch(query:source:)`：手动输入、点击历史词、点击热搜词。
- `normalizedQuery` 负责统一 `trim`、空字符串过滤、长度上限 50 校验。
- 搜索结果成功返回后才调用保存历史；失败不写历史。
- 返回空结果也算成功，应写入历史，与 `spec.md` 保持一致。
- 同一关键词在请求进行中再次提交时直接忽略；不同关键词再次提交时取消旧请求，采用最后一次输入结果。
- `SearchHomeViewModel` 不直接执行远端搜索，只负责导航到 `.searchResult(query:)`，保持页面职责清晰。

---

## 5. Navigation 与 Deeplink 设计

### 5.1 导航方案

继续使用现有 `NavigationStack + NavigationRouter + AppRoute` 方案，搜索相关页面全部归属 Home Tab，不新增新 Tab。

### 5.2 路由清单

| 路由标识 | 目标页面 | 参数 | 导航方式 | 说明 |
|---------|---------|------|---------|------|
| `.searchHome` | `SearchHomeView` | 无 | Push | 首页右上角搜索入口默认落点 |
| `.searchResult(query:)` | `SearchResultView` | `query` | Push | 手动输入、历史词、热搜词都进入该页 |
| `.rankingHome` | `DiscoveryPlaceholderView(kind: .ranking)` | 无 | Push | PRD-05 未落地前作为受控 Native 承接页 |
| `.classificationHome` | `DiscoveryPlaceholderView(kind: .classification)` | 无 | Push | PRD-06 未落地前作为受控 Native 承接页 |
| `.newReleases` | `DiscoveryPlaceholderView(kind: .newReleases)` | 无 | Push | 首版固定 Native 占位承接页 |
| `.actorHub` | `DiscoveryPlaceholderView(kind: .actors)` | 无 | Push | 首版固定 Native 占位承接页 |
| `.player(videoId:)` | `PlayerView` | `videoId` | Push | 搜索结果卡片“观看” |
| `.dramaDetail(dramaId:)` | `DramaDetailView` | `dramaId` | Push | 搜索结果卡片“详情” |

### 5.3 `AppRoute` / `NavigationRouter` 扩展要点

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
}
```

- 上述新增 route 的 `owningTab` 统一返回 `.home`。
- `NavigationRouter.navigate(to:)` 对新增 route 采用与现有 `.player` / `.dramaDetail` 相同的 push 行为。
- `.home` 仍保留为回到首页根的语义，不承担搜索发现页。

### 5.4 Deep Link 处理

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `djsdrama://search` | `.searchHome` | 无 |
| `djsdrama://search/result/{query}` | `.searchResult(query:)` | 对 path 第 2 段做 percent-decoding 与 trim |
| `djsdrama://ranking` | `.rankingHome` | 无 |
| `djsdrama://classification` | `.classificationHome` | 无 |
| `djsdrama://new-releases` | `.newReleases` | 无 |
| `djsdrama://actors` | `.actorHub` | 无 |
| `djsdrama://play/{id}` | `.player(videoId:)` | 沿用现有逻辑 |
| `djsdrama://drama/{id}` | `.dramaDetail(dramaId:)` | 沿用现有逻辑 |

#### 解析规则

- `djsdrama://search` 直接进入搜索发现页。
- `djsdrama://search/result/{query}` 必须在 percent-decoding 后仍为非空字符串，否则返回 `nil`。
- `new-releases` / `actors` 不要求额外参数，首版直接进入 Native 占位承接页。
- `ShortDramaApp` 与 `NavigationRouter.pendingRoute` 机制保持不变，仍支持容器未 ready 时暂存 route。

### 5.5 首页入口接线

- `HomeView` 在 `.navigationBarTitleDisplayMode(.large)` 基础上新增 `.toolbar`。
- 入口位置固定为右上角，点击后 `router.navigate(to: .searchHome)`。
- 入口只负责导航，不预取热搜或历史，避免首页职责膨胀。

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | 现有 `APIClient` + `URLSession` | 不引入 Alamofire 等第三方网络库 |
| 请求构建 | 扩展 `DramaEndpoints` | 继续使用 `APIEndpoint` 协议 |
| 响应解析 | `Codable` + `JSONDecoder.convertFromSnakeCase` | 兼容 `cover_url`、`episode_count`、`page_size` 等字段 |
| 错误处理 | `APIError` | 承接本地校验、网络错误、服务端错误和解码错误 |

### 6.2 API 端点定义

```swift
enum DramaEndpoints {
    struct SearchDramas: APIEndpoint {
        typealias Response = DramaListResponse

        let query: String
        let page: Int
        let pageSize: Int

        var path: String { "/api/dramas/search" }
        var method: HTTPMethod { .get }
        var queryItems: [URLQueryItem]? {
            [
                URLQueryItem(name: "q", value: query),
                URLQueryItem(name: "page", value: String(page)),
                URLQueryItem(name: "pageSize", value: String(pageSize))
            ]
        }
    }

    struct GetHotSearches: APIEndpoint {
        typealias Response = HotSearchListResponseDTO

        var path: String { "/api/dramas/hot-search" }
        var method: HTTPMethod { .get }
    }
}
```

### 6.3 Repository / DataSource 接线

- 在 `DramaRepositoryProtocol` 上新增：
  - `searchDramas(query:page:pageSize:) async throws -> [Drama]`
  - `fetchHotSearches() async throws -> [HotSearchItem]`
- `DramaRemoteDataSource` 新增对应方法并复用现有 `APIClient`。
- `DramaRepository` 继续负责 DTO → Entity 映射，保持 ViewModel 不直接依赖 DTO。
- 搜索结果继续使用现有 `Drama` Entity，不新增搜索专用卡片模型。

### 6.4 错误包体兼容策略

shared `design.md` 约定错误结构为 `{ error: { code, message } }`，而当前 `APIClient` 仅解析顶层 `message`。iOS 端需要在不影响现有功能前提下兼容两种结构：

```swift
private struct ErrorResponse: Decodable {
    struct Payload: Decodable {
        let code: String?
        let message: String?
    }

    let message: String
    let code: String?

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let nested = try? container.decode(Nested.self) {
            message = nested.error.message ?? "未知错误"
            code = nested.error.code
        } else if let flat = try? container.decode(Flat.self) {
            message = flat.message ?? "未知错误"
            code = nil
        } else {
            message = "未知错误"
            code = nil
        }
    }
}
```

这样可保证：

- 搜索新接口与 shared 方案对齐。
- 已有旧接口若仍返回旧结构，也不被破坏。

### 6.5 请求策略

| 场景 | 策略 | 说明 |
|------|------|------|
| 搜索发现页热搜请求 | 首次进入加载一次，局部失败可手动重试 | 不阻塞历史展示与手动搜索 |
| 搜索结果页首次加载 | 页面创建即请求第一页 | `page = 1`、`pageSize = 10` |
| 结果页重搜 | 新关键词触发新请求 | 取消旧请求，仅保留最后一次有效结果 |
| 自动重试 | 不做自动重试 | 与现有首页一致，首版依赖用户手动重试 |

### 6.6 网络状态监听

- 本期不新增 `NWPathMonitor`。
- 网络切换、断网、超时统一通过 `APIError.network` 映射到错误态。
- 搜索失败后保留当前 `draftQuery` 和 `submittedQuery`，便于用户直接重试或改词重搜。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| 搜索历史 | `UserDefaults` | `search.history.items` | 无时间过期，仅保留最近 10 条 | 满足 spec 要求；不引入数据库 |
| 搜索发现页 query 草稿 | 内存状态 | `SearchHomeViewModel.query` | 页面生命周期 | 返回上一页后允许重新输入 |
| 搜索结果页当前关键词 | 内存状态 + route 参数 | `SearchResultViewModel.submittedQuery` | 页面生命周期 | 与当前结果一致 |
| 热搜数据 | 不持久化 | — | 页面生命周期 | 首版无离线缓存要求 |

### 7.2 `UserDefaultsSearchHistoryRepository` 设计

```swift
protocol SearchHistoryRepositoryProtocol: Sendable {
    func load() -> [SearchHistoryItem]
    func save(keyword: String)
    func clear()
}

struct SearchHistoryItem: Codable, Equatable {
    let keyword: String
    let updatedAt: Date
}
```

实现规则：

1. 统一对 `keyword` 做 `trim`，空字符串直接忽略。
2. 保存时先去重，再插入首位。
3. 超过 10 条时裁剪尾部。
4. 读取失败或解码失败时兜底返回空数组，并清理损坏数据。
5. 清空操作直接覆盖为空数组，保证 UI 可在 300ms 内响应。

### 7.3 历史写入时机

与 `spec.md` / `design.md` 对齐：

- 只有 `GET /api/dramas/search` 成功返回后才写历史。
- 成功但空结果也写历史。
- 请求失败、参数校验失败、用户取消请求都不写历史。

### 7.4 数据迁移策略

- 搜索历史为新增 key，无旧数据迁移负担。
- 若未来需要扩展字段，使用 `Codable` 的可选字段兼容；本期无需引入版本化迁移。
- 若后续改为其他本地存储实现，保留 `SearchHistoryRepositoryProtocol` 即可平滑替换。

---

## 8. 配置与环境

| 配置项 | 管理方式 | iOS 端策略 | 说明 |
|--------|---------|-----------|------|
| API Base URL | `xcconfig + Info.plist` | 继续通过 `AppConfig.apiBaseURL()` 读取 | 不在搜索模块内硬编码地址 |
| Deeplink Scheme | `Info.plist` | 继续使用 `djsdrama://` | 仅扩展 path/host 语义 |
| Feature Flag | 无新增 | 不增加搜索开关 | 本期直接交付 |
| 第三方 SDK | 无新增 | 保持现状 | 明确不新增第三方依赖 |

> 说明：虽然当前 `AppConfig.apiBaseURL()` 有默认回退值，但搜索模块不新增任何环境常量；所有请求继续统一走 `AppConfig` 和 `APIClient`。

---

## 9. API 调用清单

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| `GET /api/dramas/hot-search` | 进入搜索发现页 | `SearchHomeViewModel.loadIfNeeded()` | 更新热搜区块为榜单 | 仅热搜区块进入错误态，支持重试 |
| `GET /api/dramas/search?q&page&pageSize` | 搜索结果页首次加载 / 重搜 / 点击历史 / 点击热搜 | `SearchResultViewModel.submitSearch` | 更新结果列表；成功后写历史 | 页面进入错误态并提供重试；不写历史 |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | iOS 端实现方式 |
|---------|---------------|---------------|
| 搜索发现页入口 | 首页右上角搜索按钮进入 Native 搜索发现页 | `HomeView.toolbar` + `.searchHome` |
| 搜索结果页结构 | 顶部可编辑搜索框 + loading/content/empty/error 四态 | `SearchResultView` + `SearchResultViewModel.ViewState` |
| 历史写入规则 | 成功后写入，空结果也写，失败不写 | `SearchResultViewModel` 在成功分支调用 `SaveSearchHistoryUseCase` |
| 本地历史规则 | trim、去重、倒序、最多 10 条、支持清空 | `UserDefaultsSearchHistoryRepository` |
| 快捷入口承接 | `ranking/classification` 为后续承接；`new-releases/actors` 首版占位 | `QuickEntry.defaults` + `DiscoveryPlaceholderView` |
| deeplink 语义 | `search/result/ranking/classification/new-releases/actors` | `AppRoute` + `DeeplinkHandler` 扩展 |
| 结果卡片交互语义 | 仅“观看 / 详情” | 复用首页卡片组件，不新增整卡点击 |
| 匹配规则 | `title + category`，大小写不敏感包含 | iOS 只负责发 `q`，不在端上重复实现匹配逻辑 |

---

## 11. 边界与错误处理

### 11.1 全局错误处理架构

| 层级 | 机制 | 说明 |
|------|------|------|
| 输入层 | 本地校验 `trim + maxLength(50)` | 空词或超长直接阻止请求 |
| 网络层 | `APIClient` 抛出 `APIError` | 统一处理网络 / 服务端 / 解码错误 |
| ViewModel | `do/catch` + 显式状态机 | 搜索发现页用局部错误，结果页用页面错误 |
| View 层 | 内联错误态 / 重试按钮 | 不使用额外第三方 Toast 方案 |
| 日志 | 使用现有最小日志能力 | 记录 deeplink 解析失败、存储损坏、请求异常 |

### 11.2 错误码映射表

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `VALIDATION_ERROR` | 输入内容无效，请检查后重试 | 搜索按钮不可用或内联提示；不发请求 |
| `INTERNAL_ERROR` | 搜索失败，请稍后重试 | 结果页错误态 / 热搜区错误态 + 重试 |
| `NETWORK_ERROR`（端侧归类） | 网络异常，请检查后重试 | 错误态 + 重试 |
| `DECODING_ERROR`（端侧归类） | 数据加载失败，请稍后重试 | 错误态 + 重试 |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| 搜索词为空 | 输入为空或全空格 | 按钮 disabled；点击键盘搜索无效 | 🔴 |
| 搜索词超长 | 超过 50 字符 | 本地限制输入或提交前拦截 | 🔴 |
| 快速重复点击搜索 | 同一 query 在进行中再次提交 | 忽略重复提交 | 🔴 |
| 快速切换关键词重搜 | 连续提交不同 query | 取消旧请求，采用最后一次有效结果 | 🔴 |
| 热搜加载失败 | `/hot-search` 失败 | 仅热搜区显示错误态，不阻塞历史和手动搜索 | 🟡 |
| 搜索成功但无结果 | `data.isEmpty` | 展示明确空态，并写入历史 | 🔴 |
| 写历史失败 | `UserDefaults` 写入异常 | 不阻塞结果展示，可记录日志 | 🟡 |
| 读取历史损坏 | 解码失败 | 清理损坏数据，降级为空历史 | 🟡 |
| deeplink query 非法 | `djsdrama://search/result/` 为空 | 返回 `nil`，不入栈 | 🔴 |
| 子功能未开发完成 | 排行 / 分类 / 新剧 / 演员未真实实现 | 进入受控 Native 承接页，不回退 Web | 🔴 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `SearchHomeView` | 热搜区 loading | 历史 + 快捷入口 + 热搜榜 | 无历史时隐藏或空提示 | 热搜区局部错误 | 无 |
| `SearchResultView` | 整页 loading | 结果列表 | “未找到相关短剧” | 整页错误 + 重试 | 无 |
| `DiscoveryPlaceholderView` | 无 | 占位说明可见 | 不适用 | 无 | 无 |

---

## 12. 测试策略

### 12.1 测试范围

遵循 `ios/CLAUDE.md`：使用 **Swift Testing** 编写单元测试，网络层通过 `URLProtocol` mock，不发起真实请求。

| 测试类型 | 覆盖内容 | 目标 |
|---------|---------|------|
| ViewModel 单元测试 | 搜索发现页、搜索结果页状态机 | 覆盖需求中所有关键状态流转 |
| Data 层测试 | endpoint、DTO 解码、错误包体解析、本地历史持久化 | 保证 shared contract 正确接线 |
| Router / Deeplink 测试 | 新 route 与 deeplink 解析 | 确保导航契约稳定 |
| 组件轻量验证 | SwiftUI Preview | 用于开发期观察，不替代自动化 |

### 12.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| I-01 | 搜索发现页首次加载成功 | 本地有 2 条历史，热搜接口返回 3 条 | `loadIfNeeded()` | 历史正确展示，热搜区为 `.content` | ViewModel |
| I-02 | 热搜失败不阻塞搜索页 | 本地历史正常，热搜接口报错 | `loadIfNeeded()` | `historyItems` 正常，`hotSearchState == .error` | ViewModel |
| I-03 | 历史去重与裁剪 | 已有 10 条历史 | 保存第 11 条或重复词 | 新词置顶，重复词提升到首位，总数仍为 10 | Data |
| I-04 | 清空历史 | 已有若干历史 | 调用 `clear()` | 读取结果为空数组 | Data |
| I-05 | 空关键词不可提交 | query 为空或全空格 | 点击搜索 | 不导航、不发请求 | ViewModel |
| I-06 | 搜索成功写入历史 | 搜索接口成功返回结果 | `submitSearch("逆袭")` | `viewState == .content`，并保存“逆袭” | ViewModel |
| I-07 | 搜索成功但空结果也写历史 | 搜索接口返回空数组 | `submitSearch("冷门词")` | `viewState == .empty`，并保存“冷门词” | ViewModel |
| I-08 | 搜索失败不写历史 | 搜索接口报错 | `submitSearch("逆袭")` | `viewState == .error`，历史不变 | ViewModel |
| I-09 | 重复提交同一 query 去抖 | 同一 query 请求进行中 | 连续触发两次提交 | 仅发起一次请求 | ViewModel |
| I-10 | 切换 query 只保留最后一次结果 | 第一次请求未完成，第二次 query 不同 | 连续提交两次 | 首次请求被取消或结果被忽略，最终展示第二次结果 | ViewModel |
| I-11 | 快捷入口路由映射正确 | 四个入口默认配置 | 点击入口 | 分别导航到 `.rankingHome` / `.newReleases` / `.classificationHome` / `.actorHub` | ViewModel / Router |
| I-12 | 搜索结果卡片导航语义正确 | 列表中存在有效 drama | 点击“观看”或“详情” | 分别进入 `.player(videoId:)` / `.dramaDetail(dramaId:)` | Router |
| I-13 | deeplink 解析搜索结果页 | URL 为 `djsdrama://search/result/%E9%80%86%E8%A2%AD` | 调用 `handleDeepLink` | 返回 `.searchResult(query: "逆袭")` | Deeplink |
| I-14 | 搜索 endpoint query 正确 | query/page/pageSize 已知 | 构造 endpoint | path 与 queryItems 符合 shared design | Data |
| I-15 | 热搜 endpoint 正确 | 无 | 构造 endpoint | 请求 `/api/dramas/hot-search` | Data |
| I-16 | 错误包体兼容 | 返回 nested error 或 flat message | `APIClient.request` | 统一映射为可读错误信息 | Data |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| 搜索 / 热搜接口 | `URLProtocol` Stub 或 Mock Repository | Data 层优先验证 endpoint 和解码，ViewModel 层优先验证状态机 |
| 本地历史仓库 | In-memory fake 或独立 `UserDefaults(suiteName:)` | 避免污染真实用户默认存储 |
| 导航 | 直接断言 `AppRoute` / `NavigationRouter.pathsByTab` | 不依赖 UI 自动化 |

### 12.4 最小验收结论

本期 iOS 端最小自动化验收覆盖应至少满足 `spec.md` 第 8.4 节要求：

- 历史读取 / 写入 / 清空规则。
- 搜索成功后写历史、失败不写历史。
- 热搜点击触发搜索主链路。
- 快捷入口导航配置。
- 结果页重搜与 loading / empty / error 状态切换。

---

## 13. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | — | — | 明确不新增第三方依赖；网络、持久化、测试均复用系统与现有工程能力 |

---

## 14. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 现有首页卡片仍为私有子视图，结果页无法直接复用 | View 层复用 | 🟡 | 中 | 将卡片抽为独立组件文件，保持 Home/Search 共用 | 若抽取成本过高，先复制视觉结构但严格保持“观看 / 详情”语义一致 |
| `APIClient` 当前错误解析与 shared design 不一致 | 网络错误展示 | 🔴 | 中 | 扩展错误解码兼容 nested error | 若本期后端暂未切换，先兼容双格式 |
| 搜索结果快速重搜导致旧结果覆盖新结果 | 结果页状态一致性 | 🔴 | 中 | 通过 `requestTask` 取消与 `lastSubmittedQuery` 去重 | 最低限度忽略重复提交 |
| `UserDefaults` 数据损坏导致历史读取失败 | 搜索发现页体验 | 🟡 | 低 | 读取失败自动清理并降级为空历史 | 不影响搜索主链路 |
| 排行 / 分类真实页面未完成 | 快捷入口体验 | 🟡 | 高 | 统一进入受控 Native 承接页 | 后续 PRD 落地后直接替换页面内容 |
| `new-releases` / `actors` 被误实现成真实业务页 | 范围失控 | 🟡 | 中 | 在文档、路由和占位页文案中明确首版仅为 Native 占位承接页 | 保持占位页不接后端 |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/features/homepage-feed/index.md` | 入口与路由 / 多端实现 | 首页 Feed 已在 iOS Native 落地，可复用列表与卡片语义 |
| `wiki/api/dramas.md` | `GET /api/dramas` | 现有 `Drama` canonical 字段和分页结构 |
| `wiki/architecture/overview.md` | 架构概览 | Native 首页和播放器是现有主链路 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `ios/CLAUDE.md` | iOS 端需遵循 SwiftUI、MVVM + Clean Architecture、URLSession、Swift Testing、NavigationStack + deeplink 约束 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 当前仅有 `home/player/dramaDetail`，需扩展搜索相关 route |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 当前 Home Tab 的 path 管理与 pending deeplink 机制可继续复用 |
| `ios/ShortDrama/Sources/App/DeeplinkHandler.swift` | 当前仅支持 `open/play/drama`，需扩展搜索与快捷入口 deeplink |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 搜索相关页面需要在这里注册 `navigationDestination` |
| `ios/ShortDrama/Sources/App/ShortDramaApp.swift` | `onOpenURL` 已接到 router，可继续承接新增 deeplink |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 首页已有 Feed 列表与卡片视图，搜索结果页应复用其卡片交互语义 |
| `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` | 现有 loading/content/empty/error 建模可作为搜索结果页状态机参考 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 现有 `/api/dramas` 和详情请求接线方式可扩展到 search/hot-search |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 现有 DTO → Entity 映射位置 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 现有 URLSession 网络栈与错误解码需要兼容 shared design 的 error envelope |
| `ios/ShortDrama/Sources/Core/Network/APIEndpoint.swift` | 搜索与热搜 endpoint 沿用该协议定义 |
| `docs/specs/2026-07-26-prd-04-search-discovery/spec.md` | 搜索页、结果页、快捷入口、历史规则、deeplink 语义与测试要求 |
| `docs/specs/2026-07-26-prd-04-search-discovery/design.md` | shared API、状态机、路由和跨端约束 |
| `docs/specs/2026-07-25-prd-02-homepage-feed/design-ios.md` | 现有 iOS 文档风格与首页 Feed 复用策略参考 |
