# iOS 端技术方案：PRD-11 个人资产管理

> 创建日期：2026-07-30
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

iOS 端继续沿用当前 `SwiftUI + MVVM + Clean Architecture` 组织方式，在现有 `AppShellView + NavigationRouter + AuthStore + DramaRepository` 基线上，为 home tab 新增一个真实的 booking assets route，并把菜单中的“我的预约”从 `menuPlaceholder(kind: .booking)` 升级为真实页面；“我的下载”继续保留占位页。

本期不新增第三方依赖，不引入新的持久化层，不把预约资产页做成 H5。受保护接口 `GET /api/users/me/bookings` 继续通过现有 `APIClient + URLSession` 发起，但由于该接口必须携带 bearer token，iOS 端需要在 View 层读取 `AuthStore` 的认证状态与 `accessToken`，再把受控的登录态快照传入 ViewModel 行为。

```text
AppShellView
  -> environmentObject(router, authStore)
  -> TabNavigationHostView(home)
     -> NavigationStack
        -> BookingAssetsView(route: .bookingAssets)
           -> BookingAssetsViewModel
              -> FetchBookingAssetsUseCase
                 -> DramaRepositoryProtocol.fetchBookingAssets(query, accessToken)
                    -> DramaRepository
                       -> DramaRemoteDataSource.fetchBookingAssets(query, accessToken)
                          -> GET /api/users/me/bookings

MenuPanelContainerView
  -> onTapBooking
     -> router.closeMenuPanelThenNavigate(to: .bookingAssets)

BookingAssetsView
  -> authStore.status == anonymous/expired
     -> LoginGateView
     -> tap login
        -> router.presentLogin(BookingAssetsRouteBuilder.loginContext())
  -> authStore.status == authenticated/refreshing
     -> viewModel.loadFirstPage(accessToken)
  -> authStore.status == restoring
     -> restoring/loading skeleton
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 扩展 | 新增 booking 独立 route，替代 booking placeholder 语义 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 扩展 | 支持 booking route 的菜单关闭后导航、登录完成后 no-op 回流、防止重复入栈 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 扩展 | 注册 `BookingAssetsView` destination |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift` | 修改 | booking 点击跳真实 route；downloads 保持 placeholder |
| `ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift` | 扩展 | 新增 booking 入口 source，承载登录文案与回流语义 |
| `ios/ShortDrama/Sources/Features/Auth/Views/LoginView.swift` | 修改 | 为 booking 登录承接补齐 copy 语义 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 扩展 | 新增 booking assets 受保护 endpoint |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 扩展 | 新增预约资产读取 contract |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 扩展 | 实现 DTO → Entity 映射 |
| `ios/ShortDrama/Sources/Features/Ranking/*` | 不变 / 复用模式 | 复用 `requestToken` 防乱序、分页追加、routeEffect 登录拦截模式 |
| `ios/ShortDrama/Sources/Domain/Entities/MenuPlaceholderKind.swift` | 不变 / 局部收口 | `.downloads` 继续作为占位说明，`.booking` 不再作为菜单主路径 |
| `ios/ShortDrama/Sources/Data/DTOs/BookDramaResponseDTO.swift` | 不在本期主链路 | 当前与 backend snake_case contract 存在历史漂移，但不作为 PRD-11 booking assets 主路径阻塞项；若后续顺手修复，应作为独立回归收口处理 |

### 1.2 架构决策

1. **booking 必须是独立 route**：不继续复用 `menuPlaceholder(kind: .booking)`，否则无法满足“登录成功仍停留在预约页上下文”的要求。
2. **登录承接由 route + authStore 协同完成**：booking 页本身允许匿名进入，但匿名态只展示登录承接，不直接打受保护接口。
3. **summary 只信服务端**：iOS 不本地重算 `online_count / upcoming_count`，只消费接口返回值。
4. **请求防乱序复用 ranking 模式**：Tab 快速切换、分页追加都采用 `requestToken` / append 状态拆分，避免串页。
5. **首版不做离线缓存**：预约资产页仅保留页面级内存状态，不新增 CoreData / UserDefaults / File cache。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增 `case bookingAssets`，其 `owningTab` 固定为 `.home`，补齐 `publicRouteName` |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 菜单 booking 导航改走真实 route；`completeLogin()` 支持 booking 回流且避免重复 push |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 注册 `BookingAssetsView`，注入 `router` / `authStore` 依赖 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift` | 修改 | `onTapBooking` 从 placeholder 改成 `.bookingAssets` |
| `ios/ShortDrama/Sources/Features/Auth/Views/LoginView.swift` | 修改 | 为 booking source 展示“登录后查看我的预约”类文案 |
| `ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift` | 修改 | `Source` 新增 `.bookingAssets` |
| `ios/ShortDrama/Sources/Features/BookingAssets/Views/BookingAssetsView.swift` | 新增 | 预约资产页根视图，承接登录态 / 内容态 / 空态 / 错误态 |
| `ios/ShortDrama/Sources/Features/BookingAssets/ViewModels/BookingAssetsViewModel.swift` | 新增 | 管理 Tab、分页、summary、错误与登录引导 effect |
| `ios/ShortDrama/Sources/Features/BookingAssets/Views/Components/*` | 新增 | Tab 条、列表卡片、空态、错误态、登录承接态、底部 loading/error 组件 |
| `ios/ShortDrama/Sources/Features/BookingAssets/BookingAssetsRouteBuilder.swift` | 新增 | 统一构建 booking 登录承接 context |
| `ios/ShortDrama/Sources/Domain/Entities/BookingAsset.swift` | 新增 | 单条预约资产实体 |
| `ios/ShortDrama/Sources/Domain/Entities/BookingAssetQuery.swift` | 新增 | 查询参数实体（status/page/pageSize） |
| `ios/ShortDrama/Sources/Domain/Entities/BookingAssetSummary.swift` | 新增 | 双 Tab 摘要实体 |
| `ios/ShortDrama/Sources/Domain/Entities/BookingAssetPage.swift` | 新增 | `items + pagination + summary` 领域聚合结果 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 修改 | 新增 `fetchBookingAssets` contract |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchBookingAssetsUseCase.swift` | 新增 | 封装 booking assets 读取逻辑 |
| `ios/ShortDrama/Sources/Data/DTOs/BookingAssetDTO.swift` | 新增 | 对齐 backend `BookingAsset` schema |
| `ios/ShortDrama/Sources/Data/DTOs/BookingAssetSummaryDTO.swift` | 新增 | 对齐 `summary.online_count/upcoming_count` |
| `ios/ShortDrama/Sources/Data/DTOs/BookingAssetListResponseDTO.swift` | 新增 | 对齐 `{ data, pagination, summary }` 响应 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 新增 `GetUserBookingsEndpoint` 与 `fetchBookingAssets` |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 修改 | 实现 booking assets DTO → Entity 映射 |
| `ios/ShortDrama/Sources/Data/DTOs/BookDramaResponseDTO.swift` | 不在本期主链路 | 当前与 backend snake_case contract 存在历史漂移，但不作为 PRD-11 booking assets 主路径阻塞项；若后续顺手修复，应作为独立回归收口处理 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 覆盖 booking route 所属 tab、登录完成回流不重复 push |
| `ios/ShortDrama/Tests/ViewModelTests/BookingAssetsViewModelTests.swift` | 新增 | 覆盖首屏、切 Tab、防乱序、分页、401 回登录承接 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 修改 | 覆盖 booking assets 请求头、DTO decode 与映射 |
| `ios/ShortDrama/Tests/DataTests/APIClientTests.swift` | 修改 | 覆盖 booking assets endpoint query/header 行为 |

---

## 3. View 层设计

### 3.1 组件层级树

```text
BookingAssetsView
├── BookingAssetsNavigationBar
├── BookingAssetsContent
│   ├── RestoringStateView
│   ├── LoginGateView
│   │   ├── Illustration
│   │   ├── Title + Description
│   │   └── LoginButton
│   ├── BookingAssetsTabBar
│   │   ├── OnlineTab(count)
│   │   └── UpcomingTab(count)
│   ├── FirstPageLoadingView
│   ├── BookingAssetsErrorView
│   ├── BookingAssetsEmptyView
│   └── BookingAssetsList
│       ├── BookingAssetCardView (ForEach)
│       ├── AppendLoadingFooter
│       └── AppendErrorFooter
└── Toast / inline error message (optional)
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `BookingAssetsView` | View | 预约资产页根视图，衔接 authStore / router / ViewModel | 否 |
| `BookingAssetsTabBar` | View | 展示 `已上线(N)` / `待上线(N)` 并切换状态 | 否 |
| `BookingAssetCardView` | View | 展示封面、标题、集数、预约时间、状态标签 | 否 |
| `BookingAssetsEmptyView` | View | 根据当前 Tab 展示差异化空态文案 | 否 |
| `BookingAssetsErrorView` | View | 首屏失败态与重试按钮 | 否 |
| `BookingAssetsLoginGateView` | View | 匿名 / expired 时的登录承接页 | 否 |
| `MenuPlaceholderView` | View | 继续承接 downloads | 是 |

### 3.3 组件接口定义

```swift
struct BookingAssetsView: View {
    @EnvironmentObject private var router: NavigationRouter
    @EnvironmentObject private var authStore: AuthStore
    @StateObject private var viewModel: BookingAssetsViewModel

    init() {
        let repository: DramaRepositoryProtocol = DramaRepository()
        _viewModel = StateObject(
            wrappedValue: BookingAssetsViewModel(
                fetchBookingAssetsUseCase: FetchBookingAssetsUseCase(repository: repository)
            )
        )
    }

    var body: some View {
        BookingAssetsScreen(
            authStatus: authStore.status,
            state: viewModel.viewState,
            summary: viewModel.summary,
            selectedStatus: viewModel.selectedStatus,
            onTapLogin: handleLogin,
            onSelectStatus: handleSelectStatus,
            onRetry: handleRetry,
            onLoadMore: handleLoadMore
        )
    }
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| `AppShellView -> BookingAssetsView` | `@EnvironmentObject` | 注入 `router`、`authStore` |
| `BookingAssetsView -> BookingAssetsViewModel` | 方法参数 | 把当前 `authStatus/accessToken` 作为受控快照传入加载动作 |
| `ViewModel -> View` | `@Published` | 列表、summary、loading、error、append 状态 |
| `View -> Router` | closure / direct call | 点击登录、返回、菜单收口后的导航 |

### 3.5 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | SwiftUI 自适应布局 | 列表卡片纵向单列，避免 iPhone 小屏截断 |
| Dynamic Type | 跟随系统字体 | 标题与计数允许换行，不把 count 写死在固定宽度里 |
| 深色模式 | 沿用 DesignTokens | 不新增私有颜色硬编码 |
| 安全区域 | `safeAreaInset` / 默认导航栈 | 底部 append loading 不遮挡 Home Indicator |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 View | 职责 |
|-----------|----------|------|
| `BookingAssetsViewModel` | `BookingAssetsView` | 管理首屏、Tab 切换、分页追加、summary、错误态、防乱序 |

### 4.2 状态定义

```swift
@MainActor
final class BookingAssetsViewModel: ObservableObject {
    enum ViewState: Equatable {
        case idle
        case loading
        case content([BookingAsset])
        case empty
        case error(String)
    }

    @Published private(set) var selectedStatus: BookingAssetAvailabilityStatus = .online
    @Published private(set) var summary: BookingAssetSummary = .empty
    @Published private(set) var viewState: ViewState = .idle
    @Published private(set) var isAppending = false
    @Published private(set) var appendErrorMessage: String?

    private let fetchBookingAssetsUseCase: FetchBookingAssetsUseCase
    private var hasLoaded = false
    private var currentPage = 0
    private var totalPages = 1
    private var currentItems: [BookingAsset] = []
    private var requestToken = UUID()
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `selectedStatus` | `BookingAssetAvailabilityStatus` | `.online` | 当前 Tab |
| `summary` | `BookingAssetSummary` | `.empty` | 仅使用服务端返回摘要 |
| `viewState` | `ViewState` | `.idle` | 首屏主状态机 |
| `isAppending` | `Bool` | `false` | 控制加载更多 footer |
| `appendErrorMessage` | `String?` | `nil` | 仅影响追加区，不清空已有列表 |
| `currentPage` | `Int` | `0` | 当前成功页码 |
| `totalPages` | `Int` | `1` | 服务端返回总页数 |
| `requestToken` | `UUID` | 新值 | 切 Tab / retry 时刷新，丢弃旧响应 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | View 层表现 |
|---------|---------|-----------|
| Restoring | `authStore.status == .restoring` | 骨架 / ProgressView，不出现登录态闪烁 |
| LoginGate | `authStore.status == .anonymous || .expired` | 登录承接态，不发请求 |
| Loading | 已登录且 `viewState == .loading` | 首屏 loading |
| Success | `viewState == .content` | 列表 + Tab + summary |
| Empty | `viewState == .empty` | 对应 Tab 空态 |
| Error | `viewState == .error` | 首屏错误态 + 重试 |
| AppendError | `appendErrorMessage != nil` | footer 局部失败提示 |

### 4.5 核心行为

1. `loadIfNeeded(accessToken:)`
   - 仅已登录状态调用；首次进入默认请求 `online/page=1/pageSize=20`。
   - 若尚未登录则不打请求。
2. `selectStatus(_:accessToken:)`
   - 仅切换当前 Tab，不本地重算计数。
   - 触发首屏重载并刷新 `requestToken`。
3. `retry(accessToken:)`
   - 首屏失败或 429 后重试当前 Tab。
4. `loadMoreIfNeeded(accessToken:)`
   - 只有 `content`、`currentPage < totalPages` 且未 in-flight 时才触发。
5. `handleUnauthorized()`
   - 当后端返回 401 时，清空当前列表快照并回到登录承接态；不展示上一个用户数据。

### 4.6 防乱序策略

复用 `RankingViewModel` 的成熟模式：

```swift
private func reloadFirstPage(accessToken: String) async {
    requestToken = UUID()
    let token = requestToken
    viewState = .loading

    do {
        let response = try await fetchBookingAssetsUseCase.execute(
            query: .init(status: selectedStatus, page: 1, pageSize: 20),
            accessToken: accessToken
        )

        guard token == requestToken else { return }
        summary = response.summary
        currentItems = response.items
        currentPage = response.page
        totalPages = max(response.totalPages, response.page)
        viewState = response.items.isEmpty ? .empty : .content(response.items)
    } catch {
        guard token == requestToken else { return }
        viewState = .error(mapError(error))
    }
}
```

---

## 5. Navigation 路由设计

### 5.1 导航方案

继续使用 `NavigationStack + NavigationRouter`。booking assets 是 home tab 内的真实 push route，不是 modal，也不是 profile tab 页面。

### 5.2 路由清单

| 路由标识 | 目标页面 | 参数 | 导航方式 | 说明 |
|---------|---------|------|---------|------|
| `.bookingAssets` | `BookingAssetsView` | 无 | Push | “我的预约”真实页面 |
| `.menuPlaceholder(kind: .downloads)` | `MenuPlaceholderView` | `downloads` | Push | “我的下载”继续占位 |
| `LoginInterceptionContext(source: .bookingAssets, returnRoute: .bookingAssets)` | `LoginView` | 无 | FullScreenCover | 登录成功后留在 booking route |

### 5.3 路由管理

```swift
enum AppRoute: Hashable, Sendable {
    case home
    case rankingHome
    case bookingAssets
    case menuPlaceholder(kind: MenuPlaceholderKind)
    // ...existing routes
}

private extension AppRoute {
    var owningTab: AppTab {
        switch self {
        case .bookingAssets:
            return .home
        // ...existing mapping
        }
    }

    var publicRouteName: String {
        switch self {
        case .bookingAssets:
            return "menu/booking"
        // ...existing mapping
        }
    }
}
```

### 5.4 登录回流策略

新增 `BookingAssetsRouteBuilder`：

```swift
enum BookingAssetsRouteBuilder {
    static func loginContext() -> LoginInterceptionContext {
        LoginInterceptionContext(
            source: .bookingAssets,
            returnRoute: .bookingAssets
        )
    }
}
```

`NavigationRouter.completeLogin()` 需要新增一层幂等保护：

- 如果当前选中 tab 已是 `.home`，且当前 stack 顶部已经是 `.bookingAssets`，则登录成功后**不再 append 一次新的 booking route**；
- 只保留原页面并让其依据新的 `authStore.status` 自动刷新数据；
- 这样可以满足“登录成功留在 booking route”且避免重复入栈。

### 5.5 菜单关闭后导航改造

`MenuPanelContainerView` 中：

- `onTapBooking` 改为 `router.closeMenuPanelThenNavigate(to: .bookingAssets)`；
- `onTapDownloads` 保持 `router.closeMenuPanelThenNavigate(to: .menuPlaceholder(kind: .downloads))`；
- 不再通过 `.menuPlaceholder(kind: .booking)` 承接预约页。

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | `APIClient` | 继续使用统一 `JSONDecoder.convertFromSnakeCase` |
| 请求构建 | `APIEndpoint` | 新增 booking assets 受保护 endpoint |
| 鉴权头 | endpoint `headers` | 明确写入 `Authorization: Bearer <token>` |
| 响应解析 | `BookingAssetListResponseDTO` | 解析 `{ data, pagination, summary }` |
| 错误处理 | `APIError` | 401/429/500/503 映射为页面态与提示文案 |

### 6.2 API 端点定义

```swift
struct GetUserBookingsEndpoint: APIEndpoint {
    typealias Response = BookingAssetListResponseDTO

    let query: BookingAssetQuery
    let accessToken: String

    var path: String { "/api/users/me/bookings" }
    var method: HTTPMethod { .get }
    var queryItems: [URLQueryItem]? {
        [
            URLQueryItem(name: "status", value: query.status.rawValue),
            URLQueryItem(name: "page", value: String(query.page)),
            URLQueryItem(name: "pageSize", value: String(query.pageSize))
        ]
    }
    var headers: [String: String] {
        ["Authorization": "Bearer \(accessToken)"]
    }
}
```

### 6.3 DTO 设计

```swift
struct BookingAssetDTO: Codable, Equatable {
    let dramaID: String
    let title: String
    let coverURL: String?
    let episodeCount: Int
    let bookedAt: String
    let availabilityStatus: BookingAssetAvailabilityStatusDTO
}

struct BookingAssetSummaryDTO: Codable, Equatable {
    let onlineCount: Int
    let upcomingCount: Int
}

struct BookingAssetListResponseDTO: Codable, Equatable {
    let data: [BookingAssetDTO]
    let pagination: PaginationDTO
    let summary: BookingAssetSummaryDTO
}
```

由于 `APIClient` 已统一使用 `.convertFromSnakeCase`，booking assets DTO 不需要手写 snake_case `CodingKeys`。

> 说明：现有 `BookDramaResponseDTO` 与 backend snake_case contract 存在历史漂移，但它不属于 PRD-11 booking assets 读取链路的主路径依赖。本方案只在参考资料与风险中保留该背景信息，不把它列为本期资产页必须一并改动的阻塞项；若 coding 阶段决定顺手修复，应作为独立回归收口处理，并避免与 booking assets 主实现耦合。

### 6.4 请求重试策略

| 场景 | 重试次数 | 策略 | 说明 |
|------|---------|------|------|
| 网络超时 | 0 | 手动重试 | 维持当前 iOS 既有行为，不新增自动重放 |
| 401 | 0 | 切回登录承接态 | 不保留旧用户资产 |
| 429 | 0 | 提示稍后重试 | 保留当前可展示内容或壳页 |
| 500/503 | 0 | 内联重试 | 首屏错误态或追加 footer 重试 |

### 6.5 API 调用入口

`DramaRemoteDataSource` 新增：

```swift
func fetchBookingAssets(
    query: BookingAssetQuery,
    accessToken: String
) async throws -> BookingAssetListResponseDTO
```

`DramaRepositoryProtocol` 新增：

```swift
func fetchBookingAssets(
    query: BookingAssetQuery,
    accessToken: String
) async throws -> BookingAssetPage
```

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| 认证令牌 | 既有 Keychain | `KeychainAuthSessionStore` | 现有 auth 规则 | 复用，不新增 |
| 预约资产列表 | 不落盘 | 内存态 | 页面生命周期内 | 首版不做离线缓存 |
| Tab 计数摘要 | 不落盘 | ViewModel 内存态 | 每次首屏请求刷新 | 只信服务端 |
| 下载占位状态 | 不落盘 | 静态文案 | — | 继续 placeholder |

### 7.2 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| 当前 Tab 首屏结果 | 内存缓存 | 页面生命周期 | 切页销毁即释放 |
| 另一 Tab 结果 | 不跨路由持久保留 | 页面生命周期 | 登录态变化或 retry 时重拉 |

### 7.3 数据迁移策略

无本地数据 schema 迁移。本期新增的都是内存态、DTO 与领域实体。

---

## 8. 配置与环境

| 配置项 | 管理方式 | 开发环境值 | 生产环境值 | 说明 |
|--------|---------|----------|-----------|------|
| API Base URL | `xcconfig + Info.plist` | 现有 Debug 配置 | 现有 Release 配置 | 继续通过 `AppConfig.apiBaseURL()` 读取 |
| Access Token | `AuthStore + KeychainAuthSessionStore` | 登录后动态获得 | 登录后动态获得 | 禁止硬编码 |
| Booking page feature flag | 无 | — | — | 本期不新增开关 |

> ⚠️ 禁止硬编码环境地址、token、mock 开关或固定 userId。

---

## 9. API 调用清单

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| `GET /api/users/me/bookings` | 进入 booking 页首屏 | `selectedStatus + page=1 + pageSize=20 + authStore.accessToken` | 更新列表、分页、summary | 401 回登录态；429/5xx 展示错误态 |
| `GET /api/users/me/bookings` | 切换 `online/upcoming` | 当前选中 Tab + `page=1` + token | 刷新当前 Tab 列表与 summary | 丢弃旧请求响应 |
| `GET /api/users/me/bookings` | 滚动到底部加载更多 | `currentPage + 1` + token | 追加列表 | 仅展示 append error，不清空已有内容 |

> 本页不新增写接口调用；`POST /api/dramas/:id/book` 仍由 ranking 等页面触发。其 DTO contract 历史漂移不属于 PRD-11 booking assets 主链路阻塞项；若 coding 阶段决定顺手修复，应作为独立回归收口处理，并避免与 booking assets 主实现耦合。 

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | iOS 端实现方式 |
|---------|---------------|---------------|
| 菜单关闭后导航 | 点击菜单入口先关菜单再导航 | 继续复用 `closeMenuPanelThenNavigate(to:)` |
| booking 独立 route | booking 必须是真实页面 | 新增 `AppRoute.bookingAssets` |
| 登录承接目标 | 登录成功回 booking route | `BookingAssetsRouteBuilder.loginContext()` + `completeLogin()` 幂等处理 |
| 默认 Tab | 默认 `online` | ViewModel 初始 `selectedStatus = .online` |
| `summary` 口径 | 不在客户端本地重算 | ViewModel 直接使用服务端 `summary` |
| 请求防乱序 | 快速切换不得串页 | 复用 `requestToken` 模式 |
| 追加分页 | 失败只影响当前 Tab footer | `appendErrorMessage` 与 `content` 分离 |
| 未授权恢复 | token 失效回登录承接态 | 401 时清空页面内存态并展示 LoginGate |
| 下载占位延续 | downloads 不接真实数据 | 保持 `MenuPlaceholderView(kind: .downloads)` |

---

## 11. 边界与错误处理

### 11.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| 网络层 | `APIClient` -> `APIError` | 统一 decode / statusCode / businessCode 解析 |
| ViewModel | `do-catch` + `mapError(_:)` | 区分首屏失败、追加失败、401 登录失效 |
| View 层 | 登录承接 / 错误态 / footer 错误 | 不暴露原始错误码 |
| 日志 | 现有调试日志即可 | 本期不新增埋点系统 |

### 11.2 错误码映射表

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `VALIDATION_ERROR` | 加载失败，请重试 | 首屏错误态 / retry |
| `AUTH_UNAUTHORIZED` / `UNAUTHORIZED` | 请先登录后查看预约 | 回到登录承接态 |
| `TOO_MANY_REQUESTS` / `AUTH_RATE_LIMITED` | 操作过于频繁，请稍后再试 | 首屏错误态或 footer 提示 |
| `INTERNAL_ERROR` | 加载失败，请稍后重试 | 首屏错误态 |
| `SERVICE_UNAVAILABLE` | 服务暂不可用，请稍后重试 | 首屏错误态 |
| `NETWORK_ERROR`（端侧归类） | 网络请求失败，请检查网络后重试 | 首屏错误态 / footer 重试 |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| 登录恢复中 | `authStore.status == .restoring` | 显示 loading，不提前渲染登录态 | 🔴 |
| 登录成功已在 booking route | 登录来自 booking 页登录承接 | `completeLogin()` 不重复 push | 🔴 |
| 快速切换 Tab | `online -> upcoming -> online` 连续点击 | 刷新 `requestToken`，丢弃旧响应 | 🔴 |
| 追加失败 | 第 N+1 页请求失败 | 保留已加载列表，仅 footer 失败 | 🔴 |
| token 过期 | 首屏或分页返回 401 | 清空列表快照，回登录承接态 | 🔴 |
| 后台回前台 | 页面停留期间切后台再回来 | 允许保留当前列表；必要时手动重试，不自动重复入栈 | 🟡 |
| 空态切换 | `online` 空、`upcoming` 有数据 | 不自动帮用户切 Tab | 🟡 |
| downloads 点击 | 用户点“我的下载” | 不发 booking 请求，继续 placeholder | 🟢 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `BookingAssetsView` 首屏 | ✅ | ✅ | ✅ | ✅ | — |
| `BookingAssetsTabBar` | ✅（展示 summary skeleton） | ✅ | ✅ | ✅ | — |
| `BookingAssetsList` | — | ✅ | — | — | — |
| `AppendFooter` | ✅ | — | — | ✅ | — |
| `LoginGateView` | `restoring` 时降级为 loading | — | — | — | 认证缺失时展示登录承接 |

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | `BookingAssetsViewModel` 状态机、`NavigationRouter` 回流逻辑 | 关键路径全覆盖 | Swift Testing |
| Data 层测试 | endpoint query/header、DTO decode、repository mapping | 关键 contract 全覆盖 | Swift Testing + `URLProtocolMock` |
| 现有回归测试 | 无新增历史回归项 | booking assets 主链路测试聚焦本期新增 route / auth / endpoint / DTO / ViewModel | Swift Testing |

### 12.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| IOS-BKG-01 | 匿名进入 booking 页 | `authStatus = .anonymous` | 页面出现 | 只展示登录承接，不发请求 | ViewModel / View 组合测试 |
| IOS-BKG-02 | 已登录首屏成功 | token 有效，接口返回 data | `loadIfNeeded()` | 展示 `content`，summary 与服务端一致 | ViewModel |
| IOS-BKG-03 | 默认 online 空态 | 接口返回 `data=[]` 且 `online_count=0` | 首屏完成 | 展示 online 空态，Tab 可切换 | ViewModel |
| IOS-BKG-04 | 切换 Tab 防乱序 | 两次请求返回顺序颠倒 | 连续切换 Tab | 只消费最后一次响应 | ViewModel |
| IOS-BKG-05 | 追加失败不清空内容 | 当前已有 1 页内容 | `loadMoreIfNeeded()` 失败 | 保留列表，仅显示 footer 错误 | ViewModel |
| IOS-BKG-06 | 401 回登录态 | token 失效 | 首屏请求返回 401 | 清空内容并展示登录承接 | ViewModel |
| IOS-BKG-07 | booking route 属于 home tab | 新增 route | 检查 route metadata | `owningTab == .home`，`publicRouteName == "menu/booking"` | Router |
| IOS-BKG-08 | 登录成功不重复 push booking | 当前顶部已是 booking route | 调 `completeLogin()` | 栈深不增加 | Router |
| IOS-BKG-09 | endpoint 携带 bearer 与 query | query + token | 发请求 | URL 正确，含 `Authorization` header | Data |
| IOS-BKG-10 | booking response snake_case decode | backend 返回 snake_case | decode DTO | `dramaID/episodeCount/bookedAt/availabilityStatus` 正确映射 | Data |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| Booking assets API | `URLProtocolMock` | 校验 path、queryItems、Authorization header |
| `DramaRepositoryProtocol` | `MockDramaRepository` 扩展 | 为 ViewModel 测试注入成功/失败/乱序响应 |
| `AuthStore` 快照 | 直接传入 status/token 参数 | 不在 ViewModel 内部持有真实 store |
| Router | 真实 `NavigationRouter` 单测 | 验证回流与 no-op push |

---

## 13. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | — | — | 复用现有 SwiftUI / URLSession / Swift Testing 即可 |

> ⚠️ 本方案不新增任何开源依赖。

---

## 14. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| booking 登录成功后重复 push | 导航栈、返回体验 | 🔴 | 中 | `completeLogin()` 增加顶部 route 去重 | 临时改为 success 后只 dismiss，不自动 navigate |
| bearer token 传递点散落 | 受保护接口调用 | 🔴 | 中 | 统一由 `BookingAssetsView` 从 `AuthStore` 读取并传入 ViewModel action | 若复杂度过高，可先在 RouteBuilder 层收口 token provider |
| summary 与列表被本地重算 | Tab 计数不一致 | 🔴 | 低 | 明确 ViewModel 不自行聚合 count，只读服务端 summary | 发现漂移时以服务端返回覆盖 |
| booking route 改造点遗漏 | 登录回流、菜单导航与编译完整性 | 🔴 | 中 | 明确 `AppRoute`、`LoginInterceptionContext.Source`、`LoginView`、`NavigationRouter.completeLogin()`、`MenuPanelContainerView` 为同一闭环必改点 | 若实现分阶段推进，至少先保证 route/login 闭环在同一提交内落地 |
| 登录恢复中闪烁到登录页 | 体验割裂 | 🟡 | 中 | `restoring` 单独建模为 loading | 若难以完全消除，至少不触发错误态 |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/features/app-shell/index.md` | 应用壳与菜单承接 | 菜单入口由应用壳统一承载，先关菜单再导航 |
| `wiki/features/ranking/index.md` | 预约与登录拦截 | 排行页已具备 booking 与登录拦截参考模式 |
| `wiki/features/auth/index.md` | 认证恢复与会话管理 | `AuthStore` 已负责 restore / refresh / logout |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 当前没有真实 booking route，只有 `menuPlaceholder` |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 已有菜单关闭后导航与统一登录回流基础；需为 booking 去重扩展 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 当前 destination 已承接 placeholder，需要新增 booking 页 |
| `ios/ShortDrama/Sources/App/AppShellView.swift` | 已向全局注入 `router` 与 `authStore` |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift` | 当前 booking/downloads 都走 placeholder |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPlaceholderView.swift` | downloads 可继续复用占位页 |
| `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` | 可复用 `requestToken`、append 状态与登录拦截 effect 模式 |
| `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift` | 提供 route builder 模式参考 |
| `ios/ShortDrama/Sources/Features/Auth/AuthStore.swift` | 提供 `status`、`accessToken`、restore/logout 入口 |
| `ios/ShortDrama/Sources/Features/Auth/Views/LoginView.swift` | 当前 subtitle 只覆盖 profile/ranking，需要新增 booking 语义 |
| `ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift` | 当前 source 不含 booking，需要扩展 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 当前无 `/api/users/me/bookings` endpoint |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 当前无 booking assets 读取实现 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 当前无 booking assets contract |
| `ios/ShortDrama/Sources/Data/DTOs/PaginationDTO.swift` | 现有 DTO 依赖 snake_case -> camelCase 自动映射 |
| `ios/ShortDrama/Sources/Data/DTOs/BookDramaResponseDTO.swift` | 当前手写 camelCase CodingKeys，与 backend snake_case 存在漂移 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 已有路由与登录上下文测试基线 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 已有 repository + URLProtocolMock 测试模式 |
| `docs/specs/2026-07-30-prd-11-user-assets/spec.md` | 需求范围、错误边界、API contract 定稿 |
| `docs/specs/2026-07-30-prd-11-user-assets/design.md` | 共享 schema、跨端约束、summary 与登录回流口径 |
| `docs/specs/2026-07-27-prd-07-menu-panel/design-ios.md` | 菜单抽屉与 placeholder 承接的历史方案基线 |
