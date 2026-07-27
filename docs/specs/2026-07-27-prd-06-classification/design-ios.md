# iOS 端技术方案：PRD-06 分类浏览

> 创建日期：2026-07-27
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

PRD-06 在现有 iOS `TabView + NavigationStack + NavigationRouter` 容器内，将搜索发现页的 `classificationHome` 承接页从占位实现替换为真实 Native 分类页。实现继续严格遵循 `ios/CLAUDE.md` 约束的 **MVVM + Clean Architecture（Core → Domain → Data → Presentation）**，不引入第三方依赖，网络继续基于 `URLSession`，导航继续复用既有 `NavigationRouter.navigate(to:)` 与 `.searchResult(query:)`，不新增独立分类结果页路由。

本期能力拆分为四个子域：

1. 分类页三层 UI（顶部性别 Tab / 左侧维度导航 / 右侧标签矩阵）。
2. 分类标签接口读取与并发保护。
3. 左侧维度与右侧滚动锚点同步。
4. 标签点击后复用现有搜索结果页承接。

```text
SearchHomeView
  -> QuickEntry(.classification)
     -> NavigationRouter.navigate(to: .classificationHome)

ClassificationHomeView
  -> observes ClassificationViewModel
     -> FetchClassificationTagsUseCase(gender)
        -> DramaRepositoryProtocol.fetchClassificationTags(gender)
           -> DramaRemoteDataSource.fetchClassificationTags(gender)
              -> APIClient.request(DramaEndpoints.GetClassificationTags)
                 -> GET /api/dramas/tags?gender=all|male|female
  -> tap dimension in left rail
     -> viewModel.selectDimension(key)
     -> scrollTo(anchor: key)
  -> tap tag chip
     -> normalize query
     -> NavigationRouter.navigate(to: .searchResult(query: normalized))
        -> SearchResultViewModel.loadIfNeeded()
        -> SearchDramasUseCase.execute(query)
        -> GET /api/dramas/search?q=标签名&page=1&pageSize=10
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 不变 | 继续复用 `.classificationHome` 与 `.searchResult(query:)`，不新增分类结果页 route |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 不变 | 现有 push 逻辑已能承接 `.classificationHome` 与 `.searchResult(query:)` |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 将 `.classificationHome` 从 `DiscoveryPlaceholderView(kind: .classification)` 替换为真实 `ClassificationHomeView` |
| `ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift` | 不变 / 联动验证 | 搜索发现页 `QuickEntryType.classification -> .classificationHome` 入口保持不变 |
| `ios/ShortDrama/Sources/Features/Search/ViewModels/SearchResultViewModel.swift` | 不变 / 复用 | 继续承接标签点击后的查询提交、loading / empty / error 状态机 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 新增 `GET /api/dramas/tags` endpoint 与解码逻辑 |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 修改 | 增加分类 DTO -> Entity 映射和仓库协议实现 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 修改 | 扩展分类标签查询协议 |
| `ios/ShortDrama/Sources/Features/Ranking/*` | 参考复用 | 分类页状态机、并发保护、切换重置逻辑可参考 `RankingViewModel` 实现模式 |
| `ios/ShortDrama/Sources/Features/Search/Views/SearchResultView.swift` | 复用 | 分类标签点击后继续落到既有搜索结果页，无需新页面 |

### 1.2 分层职责

| 层级 | 新增/扩展对象 | 职责 |
|------|--------------|------|
| Core | 继续复用 `APIClient`、`APIEndpoint`、`APIError`、`AppConfig` | 统一网络请求、环境配置、错误归一 |
| Domain | `ClassificationGender`、`ClassificationDimension`、`ClassificationTagsResponse`；`FetchClassificationTagsUseCase`；`DramaRepositoryProtocol` 扩展 | 纯业务模型、查询条件与状态约束 |
| Data | `ClassificationDimensionDTO`、`ClassificationTagsResponseDTO`；`DramaRemoteDataSource` / `DramaRepository` 扩展 | 远端请求与 DTO -> Entity 映射 |
| Presentation | `ClassificationHomeView`、`ClassificationViewModel`、左侧维度导航/右侧分组标签/状态组件 | 分类页 UI 装配、状态机、滚动同步、导航触发 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 将 `.classificationHome` 的承接页替换为 `ClassificationHomeView()` |
| `ios/ShortDrama/Sources/Features/Classification/Views/ClassificationHomeView.swift` | 新增 | 分类页根视图 |
| `ios/ShortDrama/Sources/Features/Classification/Views/Components/ClassificationGenderTabBar.swift` | 新增 | 顶部性别 Tab（全部 / 男频 / 女频） |
| `ios/ShortDrama/Sources/Features/Classification/Views/Components/ClassificationDimensionRail.swift` | 新增 | 左侧维度导航 |
| `ios/ShortDrama/Sources/Features/Classification/Views/Components/ClassificationTagSectionList.swift` | 新增 | 右侧分组列表、锚点滚动与标签矩阵 |
| `ios/ShortDrama/Sources/Features/Classification/Views/Components/ClassificationTagChip.swift` | 新增 | 标签胶囊 |
| `ios/ShortDrama/Sources/Features/Classification/Views/Components/ClassificationStateView.swift` | 新增 | loading / error / empty-dimension 状态容器 |
| `ios/ShortDrama/Sources/Features/Classification/ViewModels/ClassificationViewModel.swift` | 新增 | 性别切换、并发保护、默认维度重置、标签点击 query 规范化 |
| `ios/ShortDrama/Sources/Domain/Entities/ClassificationGender.swift` | 新增 | 性别筛选领域实体 |
| `ios/ShortDrama/Sources/Domain/Entities/ClassificationDimension.swift` | 新增 | 单维度标签分组实体 |
| `ios/ShortDrama/Sources/Domain/Entities/ClassificationTagsPayload.swift` | 新增 | 分类接口完整响应实体 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchClassificationTagsUseCase.swift` | 新增 | 分类标签读取用例 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 修改 | 增加 `fetchClassificationTags(gender:)` |
| `ios/ShortDrama/Sources/Data/DTOs/ClassificationTagsResponseDTO.swift` | 新增 | 对齐 shared design 的分类接口 DTO |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 增加 `GetClassificationTagsEndpoint` 与 `fetchClassificationTags(gender:)` |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 修改 | 实现分类 DTO -> Entity 映射 |
| `ios/ShortDrama/Tests/ViewModelTests/ClassificationViewModelTests.swift` | 新增 | 覆盖默认加载、Tab 切换、并发保护、维度重置、标签点击 |
| `ios/ShortDrama/Tests/DataTests/DramaRemoteDataSourceTests.swift` | 新增 | 覆盖 `/api/dramas/tags` endpoint query 与解码 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 新增 / 修改 | 增加分类 DTO -> Entity 映射测试；如当前文件不存在则新增 |
| `ios/ShortDrama/Tests/ViewModelTests/SearchHomeViewModelTests.swift` | 轻微修改 | 继续验证 classification quick entry route 不变 |

---

## 3. View 层设计

### 3.1 组件层级树

```text
ClassificationHomeView
├── ClassificationHeaderBar
│   ├── BackButton
│   └── Title("分类")
├── ClassificationGenderTabBar
│   ├── AllTab
│   ├── MaleTab
│   └── FemaleTab
└── ClassificationStateView
    ├── LoadingView
    ├── ErrorView
    └── ClassificationContentView
        ├── ClassificationDimensionRail
        │   ├── EraBackgroundItem
        │   ├── ThemePlotItem
        │   └── CharacterSettingItem
        └── ClassificationTagSectionList
            ├── SectionHeader(时代背景)
            ├── ClassificationTagGrid
            │   └── ClassificationTagChip (ForEach)
            ├── SectionHeader(主题情节)
            └── SectionHeader(角色设定)
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `ClassificationHomeView` | View | 分类页根视图，连接 ViewModel、Router 和滚动同步 | 否 |
| `ClassificationGenderTabBar` | View | 顶部性别 Tab：全部 / 男频 / 女频 | 否 |
| `ClassificationDimensionRail` | View | 左侧维度导航，展示当前选中态 | 否 |
| `ClassificationTagSectionList` | View | 右侧按分组渲染标题、标签矩阵与锚点 | 否 |
| `ClassificationTagChip` | View | 标签胶囊点击态 | 否 |
| `ClassificationStateView` | View | loading / error / content 状态容器 | 否 |
| `SearchResultView` | View | 标签点击后承接搜索结果页 | 是 |
| `DiscoveryPlaceholderView` | View | 不再承接 classification，仅保留其他入口占位 | 否 |

### 3.3 组件接口定义

```swift
struct ClassificationHomeView: View {
    @EnvironmentObject private var router: NavigationRouter
    @StateObject private var viewModel: ClassificationViewModel
    @State private var scrollTarget: ClassificationDimensionKey?

    var body: some View {
        ClassificationHomeContent(
            selectedGender: viewModel.selectedGender,
            selectedDimensionKey: viewModel.selectedDimensionKey,
            state: viewModel.viewState,
            onBack: { router.dismiss() },
            onSelectGender: { gender in
                Task { await viewModel.selectGender(gender) }
            },
            onSelectDimension: { key in
                viewModel.selectDimension(key)
                scrollTarget = key
            },
            onTapTag: { tag in
                guard let query = viewModel.normalizedTagQuery(tag) else { return }
                router.navigate(to: .searchResult(query: query))
            },
            onRetry: {
                Task { await viewModel.retry() }
            }
        )
        .task { await viewModel.loadIfNeeded() }
    }
}
```

```swift
struct ClassificationTagChip: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .lineLimit(1)
        }
        .buttonStyle(.plain)
    }
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| `ClassificationViewModel` -> `ClassificationHomeView` | `@Published` + `@StateObject` | 当前性别、当前维度、页面状态、错误态 |
| `ClassificationHomeView` -> 子组件 | 构造参数 | Tab 选中、分组列表、点击回调 |
| 子组件 -> `ClassificationHomeView` | closure callback | 切换 gender、点击维度、点击标签、重试 |
| 页面 -> `NavigationRouter` | `@EnvironmentObject` | 点击标签进入 `.searchResult(query:)` |
| 页面内部滚动联动 | `ScrollViewReader` + anchor id | 左侧维度点击后定位到右侧对应分组 |

### 3.5 交互与复用策略

- 搜索发现页的「分类」快捷入口不变，仍通过 `SearchHomeViewModel.route(for: .classification) -> .classificationHome` 进入分类页。
- 分类页点击标签不新增新页面，而是直接 `router.navigate(to: .searchResult(query: normalized))`。
- 搜索结果页自身的 query 提交、loading / empty / error、历史记录保存逻辑继续完全复用 `SearchResultViewModel`。
- 左侧维度与右侧滚动同步只在分类页内部处理，不外溢到全局 Router。
- 如果某个维度 `tags=[]`，左侧导航仍展示该项，右侧对应分组展示空态文案而不是隐藏。

### 3.6 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | 左右双栏，左侧固定窄栏，右侧弹性宽度 | 兼容 iPhone 竖屏主场景 |
| Dynamic Type | 使用语义字体，多行文本回退 | 标签胶囊优先单行截断，分组标题允许扩展 |
| 深色模式 | 复用系统语义色与 DesignTokens | 选中态 / 未选中态均保持对比度 |
| 安全区域 | 保持 `NavigationStack` 默认行为 | 顶部返回与标题不侵入状态栏 |
| 长列表滚动 | `ScrollViewReader` + section anchor | 性别切换后回到第一个维度 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 View | 职责 |
|-----------|----------|------|
| `ClassificationViewModel` | `ClassificationHomeView` | 管理默认加载、gender 切换、维度选中、并发保护、标签 query 规范化 |

### 4.2 状态定义

```swift
@MainActor
final class ClassificationViewModel: ObservableObject {
    enum ViewState: Equatable {
        case loading
        case content([ClassificationDimension])
        case error(String)
    }

    @Published private(set) var selectedGender: ClassificationGender = .all
    @Published private(set) var selectedDimensionKey: ClassificationDimensionKey = .eraBackground
    @Published private(set) var viewState: ViewState = .loading

    private let fetchClassificationTagsUseCase: FetchClassificationTagsUseCase
    private var hasLoaded = false
    private var requestToken = UUID()
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `selectedGender` | `ClassificationGender` | `.all` | 顶部性别 Tab 默认值 |
| `selectedDimensionKey` | `ClassificationDimensionKey` | `.eraBackground` | 左侧默认选中第一个维度 |
| `viewState` | `ViewState` | `.loading` | 首屏 / 切换性别 / 重试后的页面主状态 |
| `hasLoaded` | `Bool` | `false` | 防止首次重复加载 |
| `requestToken` | `UUID` | 随机初值 | 保护快速切换 gender 时旧响应不覆盖新状态 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | View 层表现 |
|---------|---------|-----------|
| Loading | 首次进入或切换 gender 后请求中 | 显示全页 loading，占位保留顶部 Tab |
| Success（有标签） | `viewState == .content(dimensions)` 且存在非空 tags | 展示左侧维度 + 右侧标签矩阵 |
| Success（空维度） | `viewState == .content(dimensions)` 且某维度 `tags=[]` | 仍展示对应分组标题与空态文案 |
| Error | `viewState == .error(message)` | 展示错误态与重试按钮 |

### 4.5 关键行为设计

- 默认状态固定为 `gender = .all`，与 shared design 一致。
- 首次加载成功后，默认 `selectedDimensionKey = dimensions.first.key`，也就是固定的第一个维度。
- 切换性别 Tab 时：
  1. 更新 `selectedGender`；
  2. 发起新请求；
  3. 请求成功后把 `selectedDimensionKey` 重置为第一个维度；
  4. 通知 View 层滚动到第一个锚点。
- 用户点击左侧维度时，只更新当前选中 key，并触发 View 层滚动；不发网络请求。
- ViewModel 内部维护 `requestToken`，只有最后一次 gender 请求能写回 UI，避免快速切换时出现回跳。
- 标签点击前通过 `normalizedTagQuery(_:)` 复用当前搜索页一致的 query 规则：首尾空格清洗、非空校验、最大 50 字符限制；若清洗后为空则禁用导航。

---

## 5. Navigation 路由设计

### 5.1 导航方案

继续使用现有 `NavigationStack + NavigationRouter + AppRoute`。分类页不新增新的顶级导航语义，仍通过既有 `.classificationHome` 进入；标签点击继续复用 `.searchResult(query:)`，不新增 `search?q=` 风格路由或中间页。

### 5.2 路由清单

| 路由标识 | 目标页面 | 参数 | 导航方式 | 说明 |
|---------|---------|------|---------|------|
| `.classificationHome` | `ClassificationHomeView` | 无 | Push | 搜索发现页「分类」入口承接页 |
| `.searchResult(query:)` | `SearchResultView` | `query` | Push | 标签点击后的结果页承接 |
| `.searchHome` | `SearchHomeView` | 无 | Pop / Push | 返回搜索发现页时复用现有栈 |

### 5.3 路由管理

```swift
enum AppRoute: Hashable {
    case searchHome
    case searchResult(query: String)
    case classificationHome
}

func route(for entry: QuickEntry) -> AppRoute {
    switch entry.type {
    case .classification:
        return .classificationHome
    default:
        // existing routes
    }
}
```

说明：
- `AppRoute` 无需新增 case，只把 `.classificationHome` 的实际页面从 placeholder 替换为真实页面；
- `publicRouteName` 仍保持 `classification`；
- Deeplink 语义无需变更，分类页仍归属 home tab 导航栈。

### 5.4 Deep Link 处理

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `djsdrama://classification` | `AppRoute.classificationHome` | 无 |
| `djsdrama://search/result/<query>` | `AppRoute.searchResult(query:)` | path 中的 `<query>` |

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | `URLSession` / `APIClient` | 继续复用现有网络基础设施 |
| 请求构建 | `APIEndpoint` | 新增分类 endpoint |
| 响应解析 | `Codable` + `JSONDecoder` | 分类 DTO 沿用既有解码模式 |
| 错误处理 | `APIError` | 保持与搜索、排行一致 |

### 6.2 API 端点定义

```swift
struct GetClassificationTagsEndpoint: APIEndpoint {
    typealias Response = ClassificationTagsResponseDTO

    let gender: ClassificationGender

    var path: String { "/api/dramas/tags" }
    var method: HTTPMethod { .get }
    var queryItems: [URLQueryItem]? {
        [URLQueryItem(name: "gender", value: gender.rawValue)]
    }
}
```

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 分类标签请求失败 | 不在 DataSource 内自动重试 | — | 交由 ViewModel 控制重试按钮 |
| 搜索结果页请求失败 | 维持现有逻辑 | — | 分类页不重复实现搜索重试 |

### 6.4 网络状态监听

本 PRD 不新增 `NWPathMonitor` 或全局联网状态监听；分类页继续遵循当前页面级错误态 + 用户手动重试模式。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| 分类标签数据 | 不持久化 | — | 页面生命周期内内存态 | 避免标签缓存与搜索索引不一致 |
| 当前选中 gender | 不持久化 | — | 页面内有效 | 返回后可重新默认进入 `all` |
| 当前选中维度 | 不持久化 | — | 页面内有效 | 性别切换时需重置为第一项 |

### 7.2 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| 分类标签列表 | 仅内存态 | 页面存在期间 | 页面销毁即释放 |
| 搜索结果 | 维持现有搜索结果页策略 | 现有实现 | 不在本 PRD 扩展 |

### 7.3 数据迁移策略

本 PRD 不新增 CoreData / UserDefaults 持久化结构，无迁移需求。

---

## 8. 测试策略

### 8.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| ViewModel 测试 | 默认加载、gender 切换、并发保护、选中维度重置、标签 query 规范化 | Swift Testing |
| Data 层测试 | `GetClassificationTagsEndpoint` query 构造、DTO 解码、Repository 映射 | Swift Testing + URLProtocolMock |
| 路由测试 | `SearchHomeViewModel.route(for:)` 继续返回 `.classificationHome` | Swift Testing |
| 搜索复用验证 | 标签点击后 query 能传递到 `.searchResult(query:)` | ViewModel / Router 测试 |

### 8.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 测试类型 |
|------|---------|------|---------|---------|
| IOS-T01 | 首次进入默认加载 all | 初始进入分类页 | 请求 `gender=all`，成功后选中第一个维度 | ViewModel |
| IOS-T02 | 切换到 male | 选择 `male` | 重新拉取数据，默认维度重置到首项 | ViewModel |
| IOS-T03 | 快速切换 gender | `all -> male -> female` | 只有最后一次响应写回 UI | ViewModel |
| IOS-T04 | 空维度保留 | 某维度 `tags=[]` | View 仍展示对应分组标题与空态 | ViewModel / View snapshot 可选 |
| IOS-T05 | 标签点击跳搜索 | 点击 `萌宝` | 路由为 `.searchResult(query: "萌宝")` | Router / ViewModel |
| IOS-T06 | 标签 query 清洗 | 点击含首尾空格标签 | 导航 query 为 trim 后字符串 | ViewModel |
| IOS-T07 | 接口错误 | `/api/dramas/tags` 返回 500 | 页面进入 error state，可重试 | ViewModel |
| IOS-T08 | endpoint query 正确 | `gender=.female` | URL query 中存在 `gender=female` | Data |

### 8.3 不在本期测试范围

- 真机滚动性能专项验证；
- UI snapshot 全量回归；
- 搜索结果页内部已有行为的重复覆盖（仅验证分类跳转接入点）。

---

## 9. 参考资料

| 文件 | 关键内容 |
|------|---------|
| `docs/specs/2026-07-27-prd-06-classification/spec.md` | 分类页三层结构、固定三维度、标签复用搜索 |
| `docs/specs/2026-07-27-prd-06-classification/design.md` | shared contract、状态机、错误语义 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | `.classificationHome` / `.searchResult(query:)` 既有路由 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 当前 classification 仍由 placeholder 承接 |
| `ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift` | classification quick entry 路由来源 |
| `ios/ShortDrama/Sources/Features/Search/ViewModels/SearchResultViewModel.swift` | 搜索结果页承接逻辑 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 新增 classification endpoint 的落点 |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 仓库扩展位置 |
| `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` | 请求并发保护、切换重置的实现参考 |
