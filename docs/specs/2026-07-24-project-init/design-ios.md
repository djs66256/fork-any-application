# iOS 端技术方案：项目初始化与架构设计

> 创建日期：2026-07-24
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

iOS 端采用 **MVVM + Clean Architecture**，分为三层 + Core 跨层基础设施，与 spec.md Section 4.4 定义完全一致。

```
┌─────────────────────────────────────────────────┐
│  Presentation 层 (View + ViewModel)              │
│  职责：UI 渲染、用户交互、状态绑定                 │
│  约束：View 不持有业务逻辑，ViewModel 不依赖 UI    │
│  Sources/Features/<Feature>/Views/                │
│  Sources/Features/<Feature>/ViewModels/           │
├─────────────────────────────────────────────────┤
│  Domain 层 (UseCase + Entity + Repository)       │
│  职责：业务规则、实体定义、仓库接口（协议）         │
│  约束：不依赖任何框架（纯 Swift），不引用 UIKit    │
│  Sources/Domain/UseCases/                        │
│  Sources/Domain/Entities/                        │
│  Sources/Domain/Repositories/ (Protocol only)     │
├─────────────────────────────────────────────────┤
│  Data 层 (Repository Impl + DataSource)          │
│  职责：网络请求、本地存储、DTO ↔ Entity 转换      │
│  约束：实现 Domain 层的 Repository 协议           │
│  Sources/Data/Repositories/                      │
│  Sources/Data/DataSources/                       │
│  Sources/Data/DTOs/                              │
├─────────────────────────────────────────────────┤
│  Core 层 (跨层基础设施)                            │
│  职责：网络 client、配置、工具扩展                 │
│  Sources/Core/Network/                           │
│  Sources/Core/Config/                            │
│  Sources/Core/Extensions/                        │
└─────────────────────────────────────────────────┘
```

### 1.1 与现有架构的关系

现有 `ios/ShortDrama/Sources/` 下仅有 `ShortDramaApp.swift` 和 `ContentView.swift` 两个占位文件。本次设计在现有基础上扩展为完整的三层架构。

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `ShortDramaApp.swift` | 重构 | 从简单 WindowGroup 升级为 NavigationStack + Router 注入 + Deeplink 处理 |
| `ContentView.swift` | 重构 | 迁移为 `Features/Home/Views/HomeView.swift` |
| `project.yml` | 修改 | 新增 Sources 子目录到编译路径，新增 SwiftLint 配置引用 |
| `.swiftlint.yml` | 新增 | 项目中尚无该文件，需新建 |
| `ShortDramaTests.swift` | 重构 | 扩展为分层测试目录结构 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `ios/.swiftlint.yml` | 新增 | SwiftLint 配置，定义代码风格规则 |
| `ios/project.yml` | 修改 | 调整 sources path 以包含分层子目录 |
| `ios/ShortDrama/Sources/App/ShortDramaApp.swift` | 重构 | 从原 `Sources/ShortDramaApp.swift` 迁移，增强为 NavigationStack + Router + Deeplink |
| `ios/ShortDrama/Sources/App/AppDelegate.swift` | 新增 | Deeplink 处理与 URL Scheme 解析 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 新增 | 导航路由管理，NavigationPath + AppRoute 枚举 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 新增 | URLSession 封装，统一 base URL、headers、错误处理 |
| `ios/ShortDrama/Sources/Core/Network/APIError.swift` | 新增 | 统一错误模型，对应 design.md 错误码定义 |
| `ios/ShortDrama/Sources/Core/Network/APIEndpoint.swift` | 新增 | API 端点协议，定义请求构建规范 |
| `ios/ShortDrama/Sources/Core/Config/AppConfig.swift` | 新增 | 环境配置管理，读取 xcconfig 注入的变量 |
| `ios/ShortDrama/Sources/Core/DesignSystem/DesignTokens.swift` | 新增 | 颜色、间距、字体设计 tokens |
| `ios/ShortDrama/Sources/Core/Extensions/View+Extensions.swift` | 新增 | View 通用扩展方法 |
| `ios/ShortDrama/Sources/Domain/Entities/Drama.swift` | 新增 | 短剧业务实体，字段与 design.md DramaSchema 对齐 |
| `ios/ShortDrama/Sources/Domain/Entities/Episode.swift` | 新增 | 剧集业务实体，字段与 design.md EpisodeSchema 对齐 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchDramasUseCase.swift` | 新增 | 获取短剧列表用例（骨架） |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 新增 | 短剧仓库协议 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/EpisodeRepositoryProtocol.swift` | 新增 | 剧集仓库协议 |
| `ios/ShortDrama/Sources/Data/DTOs/DramaDTO.swift` | 新增 | 短剧 API 响应 DTO，含 Entity 转换 |
| `ios/ShortDrama/Sources/Data/DTOs/EpisodeDTO.swift` | 新增 | 剧集 API 响应 DTO |
| `ios/ShortDrama/Sources/Data/DTOs/PaginationDTO.swift` | 新增 | 分页信息 DTO |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 新增 | 短剧远程数据源 |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 新增 | 短剧仓库实现，实现 DramaRepositoryProtocol |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 重构 | 从原 ContentView.swift 迁移，增强为含 ViewModel 绑定 |
| `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` | 新增 | 首页 ViewModel，管理加载状态 |
| `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` | 新增 | 播放器占位 UI |
| `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | 新增 | 播放器 ViewModel 骨架 |
| `ios/ShortDrama/Sources/Features/DramaDetail/Views/DramaDetailView.swift` | 新增 | 剧集详情占位 UI |
| `ios/ShortDrama/Sources/Features/DramaDetail/ViewModels/DramaDetailViewModel.swift` | 新增 | 剧集详情 ViewModel 骨架 |
| `ios/ShortDrama/Tests/ViewModelTests/HomeViewModelTests.swift` | 新增 | HomeViewModel 单元测试 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 新增 | DramaRepository 单元测试（含 Mock） |
| `ios/ShortDrama/Tests/Mocks/MockDramaRepository.swift` | 新增 | Mock DramaRepositoryProtocol |
| `ios/ShortDrama/Sources/ContentView.swift` | 删除 | 内容迁移至 `Features/Home/Views/HomeView.swift` |

---

## 3. View 层设计

### 3.1 组件层级树

```
ShortDramaApp (NavigationStack)
└── HomeView
    ├── VStack (centered)
    │   ├── Image (SF Symbol: "play.rectangle.fill")
    │   ├── Text ("ShortDrama")
    │   ├── Text (version "0.1.0")
    │   └── ProgressView (conditional: isLoading)
    └── .navigationDestination(for: AppRoute.self)
        ├── case .player(videoId) → PlayerView
        └── case .dramaDetail(dramaId) → DramaDetailView
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| HomeView | View | 首页占位 UI，展示应用名+版本号，绑定 HomeViewModel | 否 |
| PlayerView | View | 播放器占位 UI，展示 route 参数 videoId | 否 |
| DramaDetailView | View | 剧集详情占位 UI，展示 route 参数 dramaId | 否 |

当前阶段所有 View 均为占位 UI，仅展示路由参数或应用元信息，不含业务交互。

### 3.3 组件接口定义

```swift
// HomeView — 首页占位
struct HomeView: View {
    @StateObject private var viewModel = HomeViewModel()

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.lg) {
            if viewModel.isLoading {
                ProgressView()
            }
            Image(systemName: "play.rectangle.fill")
                .font(.system(size: DesignTokens.IconSize.xl))
                .foregroundColor(.accentColor)
            Text(viewModel.appName)
                .font(.largeTitle)
                .fontWeight(.bold)
            Text(viewModel.appVersion)
                .font(.body)
                .foregroundColor(.secondary)
        }
        .padding()
        .navigationTitle("Home")
    }
}

// PlayerView — 播放器占位
struct PlayerView: View {
    let videoId: String

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            Text("Player")
                .font(.title)
            Text("Video ID: \(videoId)")
                .font(.body)
                .foregroundColor(.secondary)
        }
        .navigationTitle("Player")
    }
}

// DramaDetailView — 详情占位
struct DramaDetailView: View {
    let dramaId: String

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            Text("Drama Detail")
                .font(.title)
            Text("Drama ID: \(dramaId)")
                .font(.body)
                .foregroundColor(.secondary)
        }
        .navigationTitle("Detail")
    }
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| 父 → 子（路由） | NavigationPath + AppRoute 枚举携带参数 | 页面跳转 |
| 子 → 父 | 当前阶段不涉及（占位 UI） | — |
| ViewModel → View | `@Published` / `@ObservedObject` | 状态驱动 UI 刷新 |
| 跨页面共享 | `@EnvironmentObject`（后续阶段引入） | 全局播放状态、用户信息 |

### 3.5 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | 自适应布局（VStack + Spacer + padding） | 占位阶段使用居中布局，后续业务使用 ScrollView 自适应 |
| Dynamic Type | 使用系统字体语义字号（`.largeTitle`、`.body`） | 后续引入自定义 DesignTokens 字体时使用 `@ScaledMetric` |
| 深色模式 | Asset Catalog 颜色集 + `@Environment(\.colorScheme)` | Info.plist 中 LaunchScreen 已使用 AccentColor |
| 安全区域 | SwiftUI 默认 safeArea 适配 | 占位 UI 无安全区域特殊需求 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 View | 职责 |
|-----------|----------|------|
| HomeViewModel | HomeView | 管理首页 UI 状态（isLoading、appName、appVersion），后续触发数据加载 |
| PlayerViewModel | PlayerView | 播放器状态骨架（当前阶段仅接收 videoId） |
| DramaDetailViewModel | DramaDetailView | 剧集详情状态骨架（当前阶段仅接收 dramaId） |

### 4.2 状态定义

```swift
@MainActor
final class HomeViewModel: ObservableObject {
    // MARK: - Published State
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?

    // MARK: - Computed (from AppConfig)
    var appName: String { AppConfig.appName }
    var appVersion: String { AppConfig.appVersion }

    // MARK: - Dependencies
    private let fetchDramasUseCase: FetchDramasUseCase

    // MARK: - Init
    init(fetchDramasUseCase: FetchDramasUseCase = FetchDramasUseCase(
        repository: DramaRepository(remoteDataSource: DramaRemoteDataSource(apiClient: APIClient.shared))
    )) {
        self.fetchDramasUseCase = fetchDramasUseCase
    }

    // MARK: - Actions
    func loadDramas() async {
        isLoading = true
        defer { isLoading = false }
        do {
            let _ = try await fetchDramasUseCase.execute(page: 1, pageSize: 20)
            errorMessage = nil
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

@MainActor
final class PlayerViewModel: ObservableObject {
    let videoId: String

    init(videoId: String) {
        self.videoId = videoId
    }
}

@MainActor
final class DramaDetailViewModel: ObservableObject {
    let dramaId: String

    init(dramaId: String) {
        self.dramaId = dramaId
    }
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `HomeViewModel.isLoading` | Bool | false | 数据加载中 |
| `HomeViewModel.errorMessage` | String? | nil | 错误信息，非 nil 表示出错 |
| `HomeViewModel.appName` | String | AppConfig.appName | 应用名称（如 "ShortDrama Backend"） |
| `HomeViewModel.appVersion` | String | AppConfig.appVersion | 版本号（如 "0.1.0"） |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | View 层表现 |
|---------|---------|-----------|
| Default | `isLoading == false && errorMessage == nil` | 显示应用图标 + 名称 + 版本号 |
| Loading | `isLoading == true` | 显示 ProgressView（菊花转圈） |
| Error | `errorMessage != nil` | 当前阶段在 ViewModel 中持有 errorMessage，View 层后续通过 `.alert()` 展示 |

---

## 5. Navigation 路由设计

### 5.1 导航方案

使用 **NavigationStack + NavigationPath**（iOS 16+），配合统一的 `AppRoute` 枚举进行路由管理。项目部署目标为 iOS 18.0，NavigationStack 完全可用。

### 5.2 路由清单

| 路由标识 | 目标页面 | 参数 | 导航方式 | 说明 |
|---------|---------|------|---------|------|
| `AppRoute.home` | HomeView | — | 栈根（初始页） | 首页 |
| `AppRoute.player(videoId:)` | PlayerView | `videoId: String` | Push | 播放器页面 |
| `AppRoute.dramaDetail(dramaId:)` | DramaDetailView | `dramaId: String` | Push | 剧集详情页 |

### 5.3 路由管理

```swift
import SwiftUI

@MainActor
final class NavigationRouter: ObservableObject {
    @Published var path = NavigationPath()

    func navigate(to destination: AppRoute) {
        path.append(destination)
    }

    func dismiss() {
        path.removeLast()
    }

    func popToRoot() {
        path.removeLast(path.count)
    }
}

enum AppRoute: Hashable {
    case home
    case player(videoId: String)
    case dramaDetail(dramaId: String)
}
```

### 5.4 Deep Link 处理

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `djsdrama://open` | AppRoute.home | 无 |
| `djsdrama://play/{videoId}` | AppRoute.player | videoId |
| `djsdrama://drama/{dramaId}` | AppRoute.dramaDetail | dramaId |

解析逻辑：

```swift
// 在 AppDelegate / App.onOpenURL 中调用
func handleDeepLink(_ url: URL) -> AppRoute? {
    guard let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
          components.scheme == "djsdrama" else {
        return nil
    }

    let path = components.host ?? ""
    let pathComponents = components.path
        .split(separator: "/")
        .map(String.init)
        .filter { !$0.isEmpty }

    switch path {
    case "open":
        return .home
    case "play":
        if let videoId = pathComponents.first {
            return .player(videoId: videoId)
        }
    case "drama":
        if let dramaId = pathComponents.first {
            return .dramaDetail(dramaId: dramaId)
        }
    default:
        break
    }
    return nil
}
```

ShortDramaApp 的入口更新为：

```swift
@main
struct ShortDramaApp: App {
    @StateObject private var router = NavigationRouter()

    var body: some Scene {
        WindowGroup {
            NavigationStack(path: $router.path) {
                HomeView()
                    .navigationDestination(for: AppRoute.self) { route in
                        switch route {
                        case .home:
                            HomeView()
                        case .player(let videoId):
                            PlayerView(videoId: videoId)
                        case .dramaDetail(let dramaId):
                            DramaDetailView(dramaId: dramaId)
                        }
                    }
            }
            .environmentObject(router)
            .onOpenURL { url in
                if let route = handleDeepLink(url) {
                    router.navigate(to: route)
                }
            }
        }
    }
}
```

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | URLSession（原生封装） | 当前阶段不引入 Alamofire 等第三方库，保持轻量 |
| 请求构建 | `APIEndpoint` 协议 | 统一请求路径、方法、参数、headers |
| 请求拦截器 | 无（初始化阶段无认证需求） | 后续在 APIClient 中通过 `URLRequest` 扩展注入 auth header |
| 响应解析 | `Codable` + `JSONDecoder` | 配置 `keyDecodingStrategy = .convertFromSnakeCase` |
| 错误处理 | 统一 `APIError` 模型 + `APIResponse<T>` 包装 | 与 design.md 统一错误格式对齐 |

### 6.2 API 端点定义

API 返回统一的包装结构（对应 design.md）：

```swift
// 成功响应包装
struct APIResponse<T: Decodable>: Decodable {
    let code: Int
    let message: String?
    let data: T
}

// 分页包装
struct PaginatedResponse<T: Decodable>: Decodable {
    let data: [T]
    let pagination: PaginationDTO
}
```

端点协议：

```swift
protocol APIEndpoint {
    associatedtype Response: Decodable
    var path: String { get }
    var method: HTTPMethod { get }
    var queryItems: [URLQueryItem]? { get }
    var body: Data? { get }
}

enum HTTPMethod: String {
    case get = "GET"
    case post = "POST"
}
```

### 6.3 APIClient 设计

```swift
final class APIClient {
    static let shared = APIClient()

    private let session: URLSession
    private let baseURL: URL
    private let decoder: JSONDecoder

    private init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 15
        config.timeoutIntervalForResource = 30
        self.session = URLSession(configuration: config)

        guard let baseURL = URL(string: AppConfig.apiBaseURL) else {
            fatalError("Invalid API base URL: \(AppConfig.apiBaseURL)")
        }
        self.baseURL = baseURL

        self.decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
    }

    func request<T: Decodable>(_ endpoint: some APIEndpoint) async throws -> T {
        var components = URLComponents(url: baseURL.appendingPathComponent(endpoint.path), resolvingAgainstBaseURL: false)
        components?.queryItems = endpoint.queryItems

        guard let url = components?.url else {
            throw APIError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = endpoint.method.rawValue
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.httpBody = endpoint.body

        let (data, response) = try await session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }

        // 处理 501 Not Implemented（骨架端点）
        if httpResponse.statusCode == 501 {
            if let errorResponse = try? decoder.decode(ErrorResponse.self, from: data) {
                throw APIError.notImplemented(errorResponse.error.message)
            }
            throw APIError.notImplemented(APIError.defaultNotImplementedMessage)
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            if let errorResponse = try? decoder.decode(ErrorResponse.self, from: data) {
                throw APIError.server(code: httpResponse.statusCode, message: errorResponse.error.message)
            }
            throw APIError.server(code: httpResponse.statusCode, message: APIError.defaultServerErrorMessage)
        }

        do {
            return try decoder.decode(T.self, from: data)
        } catch {
            throw APIError.decodingFailed(error)
        }
    }
}
```

### 6.4 APIError 模型

```swift
enum APIError: LocalizedError, Equatable {
    case invalidURL
    case invalidResponse
    case decodingFailed(Error)
    case server(code: Int, message: String)
    case network(underlying: Error)
    case notImplemented(String)
    case cancelled

    static let defaultNotImplementedMessage = "该功能正在开发中"
    static let defaultServerErrorMessage = "服务异常，请稍后重试"

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "无效的请求地址"
        case .invalidResponse:
            return "无效的服务器响应"
        case .decodingFailed(let error):
            return "数据解析失败：\(error.localizedDescription)"
        case .server(_, let message):
            return message
        case .network(let error):
            return "网络错误：\(error.localizedDescription)"
        case .notImplemented(let message):
            return message
        case .cancelled:
            return "操作已取消"
        }
    }

    static func == (lhs: APIError, rhs: APIError) -> Bool {
        switch (lhs, rhs) {
        case (.invalidURL, .invalidURL): return true
        case (.invalidResponse, .invalidResponse): return true
        case (.decodingFailed, .decodingFailed): return true
        case (.server(let lc, let lm), .server(let rc, let rm)): return lc == rc && lm == rm
        case (.network, .network): return true
        case (.notImplemented(let lm), .notImplemented(let rm)): return lm == rm
        case (.cancelled, .cancelled): return true
        default: return false
        }
    }
}

// 对应 design.md 中错误响应格式：{"error":{"code":"...","message":"..."}}
struct ErrorResponse: Decodable {
    let error: ErrorDetail

    struct ErrorDetail: Decodable {
        let code: String
        let message: String
    }
}
```

### 6.5 API 端点枚举定义

```swift
enum DramaEndpoints {
    struct GetDramas: APIEndpoint {
        typealias Response = PaginatedResponse<DramaDTO>
        let path = "/api/dramas"
        let method: HTTPMethod = .get
        let queryItems: [URLQueryItem]?
        let body: Data? = nil

        init(page: Int, pageSize: Int) {
            self.queryItems = [
                URLQueryItem(name: "page", value: String(page)),
                URLQueryItem(name: "pageSize", value: String(pageSize))
            ]
        }
    }

    struct GetDramaDetail: APIEndpoint {
        typealias Response = APIResponse<DramaDTO>
        let path: String
        let method: HTTPMethod = .get
        let queryItems: [URLQueryItem]? = nil
        let body: Data? = nil

        init(id: String) {
            self.path = "/api/dramas/\(id)"
        }
    }
}

enum EpisodeEndpoints {
    struct GetEpisode: APIEndpoint {
        typealias Response = APIResponse<EpisodeDTO>
        let path: String
        let method: HTTPMethod = .get
        let queryItems: [URLQueryItem]? = nil
        let body: Data? = nil

        init(id: String) {
            self.path = "/api/episodes/\(id)"
        }
    }
}

enum HealthEndpoints {
    struct GetHealth: APIEndpoint {
        typealias Response = HealthResponse
        let path = "/api/health"
        let method: HTTPMethod = .get
        let queryItems: [URLQueryItem]? = nil
        let body: Data? = nil
    }
}
```

### 6.6 请求重试策略

初始化阶段不实现自动重试（骨架 API 大多返回 501 或空数据）。后续业务 PRD 阶段补充：

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 网络超时 | 2 | 指数退避 | 后续实现 |
| 5xx 服务端错误 | 3 | 指数退避 | 后续实现 |
| 401 Token 过期 | — | — | 后续实现（需先实现认证体系） |

### 6.7 网络状态监听

初始化阶段不引入 NWPathMonitor。后续业务 PRD 阶段按需接入，处理 Wi-Fi 与蜂窝网络切换时对正在进行的请求的中断与重试。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

初始化阶段无业务数据持久化需求。唯一持久化需求为 app 配置（通过 UserDefaults）。

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| App 配置（首次启动标记） | UserDefaults | `ud_isFirstLaunch` | 不自动过期 | 标记应用是否首次启动 |
| App 配置（缓存时间戳） | UserDefaults | `ud_lastCacheTimestamp` | 不自动过期 | 后续用于缓存过期判断 |

> CoreData、Keychain、FileManager 等持久化方案留待后续业务 PRD 阶段按需引入。

### 7.2 后续阶段持久化规划

| 数据类型 | 拟选方案 | 触发 PRD |
|---------|---------|---------|
| 短剧列表缓存 | CoreData / SwiftData | 首页业务 PRD |
| Auth Token | Keychain（KeychainAccess） | 用户认证 PRD |
| 播放历史 | CoreData | 播放器业务 PRD |
| 用户偏好（主题、语言） | UserDefaults `@AppStorage` | 设置/国际化 PRD |

---

## 8. 配置与环境

| 配置项 | 管理方式 | 开发环境值 | 生产环境值 | 说明 |
|--------|---------|----------|-----------|------|
| API Base URL | xcconfig | `http://localhost:3001` | 生产域名（按需配置） | 后端 API 地址 |
| App Name | Info.plist | ShortDrama | ShortDrama | CFBundleDisplayName |
| App Version | project.yml `MARKETING_VERSION` | 0.1.0 | 按需递增 | 通过 AppConfig 读取 |
| Build Number | project.yml `CURRENT_PROJECT_VERSION` | 1 | CI 自动递增 | 构建编号 |
| Bundle Identifier | project.yml `PRODUCT_BUNDLE_IDENTIFIER` | com.djs66256.short_drama | 同 | 来自 PRODUCT.md |
| URL Scheme | Info.plist `CFBundleURLSchemes` | djsdrama | djsdrama | 来自 PRODUCT.md |
| Swift Version | project.yml `SWIFT_VERSION` | 6.0 | 6.0 | Swift 6 并发安全 |
| Deployment Target | project.yml `deploymentTarget.iOS` | 18.0 | 18.0 | 最低支持 iOS 18.0 |

### 8.1 xcconfig 管理

`Debug.xcconfig`（已存在）：

```
CODE_SIGN_STYLE = Automatic
```

需要添加 API Base URL 配置（新增行）：

```
API_BASE_URL = http:/$(COLON_SLASH)/localhost:3001
COLON_SLASH = /
```

`Release.xcconfig`（已存在）：

```
CODE_SIGN_STYLE = Automatic
```

需要添加 API Base URL 配置（新增行）：

```
API_BASE_URL = https:/$(COLON_SLASH)/api.example.com
COLON_SLASH = /
```

### 8.2 AppConfig

```swift
import Foundation

enum AppConfig {
    static var appName: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleDisplayName") as? String
            ?? Bundle.main.object(forInfoDictionaryKey: "CFBundleName") as? String
            ?? "ShortDrama"
    }

    static var appVersion: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "0.0.0"
    }

    static var buildNumber: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "0"
    }

    static var apiBaseURL: String {
        if let url = Bundle.main.object(forInfoDictionaryKey: "API_BASE_URL") as? String {
            return url
        }
        // Fallback for development
        #if DEBUG
        return "http://localhost:3001"
        #else
        return "https://api.example.com"
        #endif
    }
}
```

### 8.3 project.yml 修改要点

在 project.yml 的 `settings.base` 中新增 API_BASE_URL 注入到 Info.plist，确保 AppConfig 可读取：

```yaml
settings:
  base:
    SWIFT_VERSION: "6.0"
    INFOPLIST_FILE: ShortDrama/Resources/Info.plist
    INFOPLIST_KEY_API_BASE_URL: "$(API_BASE_URL)"
```

> 禁止硬编码任何常量。API base URL 通过 xcconfig 管理，app name/version 从 Bundle/Info.plist 读取。

---

## 9. API 调用清单

与 design.md 中定义的 API 严格保持一致：

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| `GET /api/health` | 暂不调用（骨架） | — | — | — |
| `GET /api/dramas` | 首页加载时（ViewModel.loadDramas()） | HomeViewModel 触发，传 page/pageSize | 更新 dramas 列表（当前为空） | 设置 ViewModel.errorMessage |
| `POST /api/dramas` | 暂不调用（返回 501） | — | — | — |
| `GET /api/dramas/[id]` | 剧集详情页加载时 | DramaDetailViewModel | 更新详情数据 | 设置 errorMessage |
| `GET /api/episodes/[id]` | 暂不调用（返回 501） | — | — | — |
| `POST /api/player/start` | 暂不调用（返回 501） | — | — | — |
| `POST /api/player/stop` | 暂不调用（返回 501） | — | — | — |

> 当前阶段仅有 `GET /api/dramas` 在 HomeViewModel 中实际调用（返回空数组）。其余端点均返回 501 Not Implemented，APIClient 已统一处理 501 错误并抛出 `APIError.notImplemented`。

---

## 10. 跨端共享逻辑落地

对应 design.md 中「跨端共享逻辑」章节：

| 共享逻辑 | design.md 定义 | iOS 端实现方式 |
|---------|---------------|---------------|
| **API 契约** | REST API JSON，统一响应格式 | APIClient 封装 URLSession，`APIResponse<T>` + `ErrorResponse` 对齐统一格式 |
| **数据模型 Schema** | Drama/Episode 字段名和类型一致 | `Domain/Entities/Drama.swift` + `Episode.swift` struct 字段一对一映射 Backend Schema，`Codable` key 通过 `convertFromSnakeCase` 对齐 |
| **错误处理格式** | 统一 `{"error":{"code":"...","message":"..."}}` | APIError 模型 + `ErrorResponse` 解析，标准错误码映射 |
| **URL Scheme** | `djsdrama://` 统一声明 | `Info.plist` CFBundleURLSchemes 声明 + App.onOpenURL 解析路由 |
| **分页规范** | 统一 `{ data, pagination: { page, pageSize, total, totalPages } }` | `PaginatedResponse<T>` + `PaginationDTO` 对齐 |
| **认证机制** | JWT Bearer Auth（后续 PRD） | 当前不实现，APIClient 预留在 `URLRequest` 中注入 Authorization header 的扩展点 |

---

## 11. 边界与错误处理

### 11.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| 网络层 | APIClient.request() → APIError 模型 | HTTP 状态码 → 统一错误类型，501 → notImplemented |
| ViewModel | do-catch / async throws | 设置 errorMessage 状态 |
| View 层 | 当前阶段 ViewModel 持有 errorMessage | 后续通过 `.alert()` 展示错误 |
| 日志 | print / os.Logger（后续 phase） | 初始化阶段使用 print 输出错误信息 |

### 11.2 错误码映射表

与 design.md 中标准错误码对应：

| 后端错误码 | HTTP 状态码 | APIError 映射 | 用户提示文案 | 交互方式 |
|-----------|------------|--------------|------------|---------|
| `VALIDATION_ERROR` | 400 | `.server(code: 400, message:)` | "请求参数有误，请检查后重试" | 后续通过 Alert/Tost 展示 |
| `NOT_FOUND` | 404 | `.server(code: 404, message:)` | "请求的资源不存在" | 后续展示空态页 |
| `INTERNAL_ERROR` | 500 | `.server(code: 500, message:)` | "服务异常，请稍后重试" | 后续通过 Alert + 重试按钮 |
| `NOT_IMPLEMENTED` | 501 | `.notImplemented(message)` | "该功能正在开发中" | ViewModel 记录，不阻塞 UI |
| `SERVICE_UNAVAILABLE` | 503 | `.server(code: 503, message:)` | "服务暂时不可用，请稍后重试" | 后续通过 Alert + 重试按钮 |
| 网络超时 | — | `.network(underlying:)` | "网络错误：请求超时" | 后续通过 Alert + 重试按钮 |
| 解码失败 | — | `.decodingFailed(error)` | "数据解析失败" | 日志记录，降级展示 |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| App 进入后台 | `scenePhase` → `.background` | 取消进行中的网络请求（`Task.cancel()`） | 🟡 后续 phase |
| App 返回前台 | `scenePhase` → `.active` | 刷新过期数据（HomeViewModel 重新加载） | 🟡 后续 phase |
| 页面销毁时未完成请求 | View onDisappear | `task.cancel()` 取消请求 | 🔴 后续 phase |
| 首次启动 | UserDefaults 无缓存标记 | 正常展示占位 UI（无特殊 Onboarding） | 🟡 当前阶段无需特殊处理 |
| xcodegen generate 失败 | project.yml 格式错误 | CI 中作为构建阶段，失败不阻塞 PR 合并（iOS CI 标记 optional） | 🔴 |
| Deeplink 解析失败 | 非法的 URL scheme 或 path | handleDeepLink 返回 nil，不导航 | 🟡 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Default (Loaded) | Loading | Empty | Error |
|-----------|-----------------|---------|-------|-------|
| HomeView | 图标 + 名称 + 版本号 | ProgressView | 当前不展示空态（占位阶段） | ViewModel 持有 errorMessage（后续 UI 展示） |
| PlayerView | 标题 + videoId | 直接展示（无异步加载） | N/A | N/A |
| DramaDetailView | 标题 + dramaId | 直接展示（无异步加载） | N/A | N/A |

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | ViewModel 逻辑、APIError 模型、DTO ↔ Entity 转换 | ≥ 80% | XCTest |
| 集成测试 | APIClient + DataSource → Repository 协作 | ≥ 60% | XCTest |
| UI 测试 | 暂不覆盖（占位 UI 无交互逻辑） | — | 后续 phase |

> 当前阶段 UI 测试和快照测试暂不引入。ViewController 为纯占位 UI，无复杂交互，测试 ROI 低。

### 12.2 关键测试场景

#### HomeViewModel 测试

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| UT-01 | 加载成功返回空列表 | MockDramaRepository 返回空数组 | 调用 loadDramas() | isLoading = false，errorMessage = nil | 单元 |
| UT-02 | 加载失败（网络错误） | MockDramaRepository 抛出 APIError.network | 调用 loadDramas() | errorMessage 非空 | 单元 |
| UT-03 | 加载失败（501 Not Implemented） | MockDramaRepository 抛出 APIError.notImplemented | 调用 loadDramas() | errorMessage 非空 | 单元 |
| UT-04 | 加载中状态 | MockDramaRepository 延迟返回 | 调用 loadDramas() | isLoading 在请求进行中为 true | 单元 |
| UT-05 | appName 正确返回 | — | 访问 viewModel.appName | 返回 "ShortDrama" | 单元 |
| UT-06 | appVersion 正确返回 | — | 访问 viewModel.appVersion | 返回 "0.1.0" | 单元 |

#### APIError 模型测试

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| UT-07 | server error errorDescription | APIError.server(code: 500, message: "boom") | 访问 errorDescription | 返回 "boom" | 单元 |
| UT-08 | notImplemented errorDescription | APIError.notImplemented("not ready") | 访问 errorDescription | 返回 "not ready" | 单元 |
| UT-09 | APIError Equatable | 两个相同 .server(500, "boom") | 比较 | true | 单元 |

#### DTO ↔ Entity 测试

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| UT-10 | DramaDTO 解码成功 | JSON 字符串对应 Drama schema | JSONDecoder.decode | DramaDTO 字段正确 | 单元 |
| UT-11 | DramaDTO → Drama Entity | DramaDTO 实例 | toEntity() | Drama 字段一一对应 | 单元 |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| DramaRepositoryProtocol | 手写 MockDramaRepository | 遵循 Protocol，通过 stubbedResult/stubbedError 控制返回值 |
| APIClient | 不直接 mock | Repository 层依赖 DataSource，DataSource 依赖 APIClient；测试时 mock Repository Protocol |
| UserDefaults | 使用真实 UserDefaults（单元测试沙盒） | 测试环境自动隔离，不需 mock |

Mock 代码骨架：

```swift
final class MockDramaRepository: DramaRepositoryProtocol {
    var stubbedResult: [Drama] = []
    var stubbedError: APIError?
    var fetchCallCount = 0
    var lastPage: Int?
    var lastPageSize: Int?

    func fetchDramas(page: Int, pageSize: Int) async throws -> [Drama] {
        fetchCallCount += 1
        lastPage = page
        lastPageSize = pageSize
        if let error = stubbedError {
            throw error
        }
        return stubbedResult
    }
}
```

### 12.4 测试文件目录结构

```
ios/ShortDrama/Tests/
├── ViewModelTests/
│   └── HomeViewModelTests.swift
├── DataTests/
│   ├── DramaRepositoryTests.swift
│   ├── APIClientTests.swift
│   └── APIErrorTests.swift
├── DomainTests/
│   └── DTOMappingTests.swift
└── Mocks/
    └── MockDramaRepository.swift
```

---

## 13. SwiftLint 配置

### 13.1 `.swiftlint.yml`

项目中尚无 SwiftLint 配置文件，需新建 `ios/.swiftlint.yml`：

```yaml
# SwiftLint 配置 — ShortDrama iOS

# 排除路径
excluded:
  - Pods
  - build
  - .build
  - ShortDrama.xcodeproj
  - ShortDrama/Tests

# 核心规则
line_length: 120
type_body_length: 300
file_length: 500
function_body_length: 60
cyclomatic_complexity: 12

# 可选规则
opt_in_rules:
  - empty_count
  - closure_spacing
  - explicit_init
  - overridden_super_call
  - prohibited_super_call
  - redundant_nil_coalescing
  - first_where
  - toggle_bool
  - unused_import
  - fatal_error_message
  - force_unwrapping

# 禁用规则
disabled_rules:
  - trailing_whitespace
  - todo

# 自定义规则阈值
type_name:
  min_length: 3
  max_length: 50

identifier_name:
  min_length: 2
  max_length: 50
  excluded:
    - id
    - x
    - y

# 嵌套类型深度限制
nesting:
  type_level: 2

# 报告
reporter: "xcode"
```

### 13.2 构建阶段集成

已在 `project.yml` 中配置 preBuildScripts：

```yaml
preBuildScripts:
  - name: SwiftLint
    script: |
      if which swiftlint > /dev/null; then
        swiftlint
      fi
    basedOnDependencyAnalysis: false
```

---

## 14. DesignTokens 设计

```swift
import SwiftUI

enum DesignTokens {
    enum Spacing {
        static let xs: CGFloat = 4
        static let sm: CGFloat = 8
        static let md: CGFloat = 16
        static let lg: CGFloat = 24
        static let xl: CGFloat = 32
    }

    enum IconSize {
        static let sm: CGFloat = 24
        static let md: CGFloat = 40
        static let lg: CGFloat = 60
        static let xl: CGFloat = 80
    }

    enum CornerRadius {
        static let sm: CGFloat = 4
        static let md: CGFloat = 8
        static let lg: CGFloat = 12
    }
}
```

---

## 15. 新增依赖

当前阶段不引入任何第三方开源依赖。所有功能使用 Apple 原生框架实现：

| 层 | 使用框架 | 说明 |
|----|---------|------|
| 网络 | URLSession（Foundation） | 原生 HTTP 客户端 |
| 序列化 | Codable + JSONDecoder | 原生 JSON 解析 |
| UI | SwiftUI | 声明式 UI 框架 |
| 导航 | NavigationStack + NavigationPath | iOS 16+ 原生导航 |
| 测试 | XCTest | 原生单元测试框架 |
| 持久化 | UserDefaults | 原生轻量键值存储 |

后续业务 PRD 阶段按需引入的第三方库候选（需征得用户同意）：

| 候选库 | 用途 | 选型理由 |
|--------|------|---------|
| Alamofire | HTTP 客户端增强 | 拦截器、重试、证书固定等高级特性 |
| Kingfisher | 图片加载与缓存 | 短剧封面、剧照等图片资源优化 |
| KeychainAccess | Keychain 封装 | Token 等敏感信息存储 |

---

## 16. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 目录重构后 XcodeGen 编译中断 | iOS 构建 | 🔴 | 中 | project.yml source path 使用通配符 `Sources/**` 自动发现；重构后先 `xcodegen generate` 验证 | 回退 project.yml 到当前状态 |
| SwiftLint 规则过严导致 CI 阻塞 | iOS CI | 🟡 | 低 | `.swiftlint.yml` 使用宽松初始规则，后续迭代收紧 | 移除 preBuildScripts 中的 SwiftLint 阶段 |
| APIClient 与 Backend 响应格式不一致 | iOS 网络层 | 🟡 | 中 | 严格对齐 design.md 中 `ErrorResponse` 和 `PaginatedResponse` 结构 | 调整 DTO 和 Decoder 配置 |
| Deeplink 解析路径约定不一致 | 跨端导航 | 🟡 | 低 | 与 Android 端统一 URL scheme path 定义在 design.md 中 | 调整 handleDeepLink 解析逻辑 |
| iOS 18.0 部署目标过激 | 设备兼容 | 🟡 | 低 | 当前 App 从零开始，无现有用户；SwiftUI NavigationStack 等特性需 iOS 16+，选 18.0 简化开发 | 降级到 iOS 17.0（仍需 NavigationStack） |

---

## 17. 参考资料

### 已查阅的 spec 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `docs/specs/2026-07-24-project-init/spec.md` | Section 4.4（iOS 分层架构） | 三层架构目录结构、关键约束 |
| `docs/specs/2026-07-24-project-init/spec.md` | Section 6.3（US-03 iOS 工程初始化） | 验收标准：xcodegen generate、模拟器显示、URL Scheme 声明 |

### 已查阅的 design 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `docs/specs/2026-07-24-project-init/design.md` | API 设计 | 7 个端点定义、统一错误响应格式、标准错误码 |
| `docs/specs/2026-07-24-project-init/design.md` | 数据模型 | Drama/Episode/User Schema 定义（Zod → Swift struct 对齐） |
| `docs/specs/2026-07-24-project-init/design.md` | 跨端共享逻辑 | URL Scheme、API 契约、错误格式、分页规范 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `ios/CLAUDE.md` | iOS 端技术约束、架构约束、测试要求 |
| `ios/project.yml` | XcodeGen 配置：Swift 6.0、iOS 18.0、SwiftLint preBuildScript |
| `ios/Configs/Debug.xcconfig` | Debug 签名配置 |
| `ios/Configs/Release.xcconfig` | Release 签名配置 |
| `ios/ShortDrama/Sources/ShortDramaApp.swift` | 当前 App 入口（简单的 WindowGroup） |
| `ios/ShortDrama/Sources/ContentView.swift` | 当前占位首页（VStack + SF Symbol + 名称 + 版本号） |
| `ios/ShortDrama/Resources/Info.plist` | djsdrama URL Scheme 已声明 |
| `ios/ShortDrama/Tests/ShortDramaTests.swift` | 当前测试骨架（#expect(true)） |
| `PRODUCT.md` | 产品名称 ShortDrama、appId com.djs66256.short_drama、schema djsdrama:// |
| `.claude/skills/ios-development/references/standards/architecture.md` | iOS 端架构设计规范（MVVM + UseCase + Repository） |
| `.claude/skills/ios-development/references/standards/testing.md` | 测试规范（XCTest、Mock 策略、覆盖率目标） |
| `.claude/skills/ios-development/references/standards/coding-standards.md` | 代码规范（命名、格式化、SwiftUI 最佳实践） |
