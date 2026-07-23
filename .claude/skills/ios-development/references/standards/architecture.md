# 架构设计 — iOS

> 本文档定义 iOS 端的整体架构设计规范。

---

## 1. 整体架构

ShortDrama iOS 端采用 **MVVM + UseCase + Repository** 分层架构，数据流严格自上而下。

```
┌──────────────────────────────┐
│  View (SwiftUI)              │  声明式 UI，仅负责展示与用户交互转发
│  - 无业务逻辑                  │
│  - 通过 @State / @Observable  │
│    ViewModel 驱动刷新          │
└──────────┬───────────────────┘
           │ 用户动作 → 调用 ViewModel 方法
┌──────────▼───────────────────┐
│  ViewModel / StateObject     │  状态管理，业务编排
│  - @MainActor 标注            │
│  - 持有 UseCase               │
│  - 暴露 @Published / @Observable │
│    状态供 View 读取            │
└──────────┬───────────────────┘
           │ 调用 UseCase
┌──────────▼───────────────────┐
│  UseCase / Domain            │  纯业务逻辑，不依赖任何 UI 框架
│  - 实现具体业务规则            │
│  - 组合多个 Repository         │
│  - 单元测试友好，无 SwiftUI 依赖 │
└──────────┬───────────────────┘
           │ 调用 Repository
┌──────────▼───────────────────┐
│  Repository                  │  数据聚合层，对外隐藏数据来源
│  - 协议定义在 Domain 层        │
│  - 实现类在 Data 层            │
└──────────┬───────────────────┘
           │ 调用 DataSource
┌──────────▼───────────────────┐
│  DataSource                  │  具体的数据来源实现
│  - RemoteDataSource (Alamofire)│
│  - LocalDataSource (Core Data) │
└──────────────────────────────┘
```

### 1.1 各层职责

- **View 层**：SwiftUI 视图。只做三件事——(1) 声明布局，绑定 ViewModel 状态；(2) 用户交互转发给 ViewModel；(3) `#Preview` 覆盖关键状态。不含 if-else 业务分支、不含数据转换逻辑。
- **ViewModel 层**：`@MainActor` 标注。持有 `UseCase`（通过构造器注入），暴露 `@Published` 或 `@Observable` 属性。所有方法要么是 UI 事件处理，要么是 async 数据加载。
- **UseCase 层**：纯 Swift 类，无 `@MainActor`（除非需要），无 SwiftUI 依赖。每个 UseCase 只做一件事，如 `FetchRecommendationsUseCase`、`ClaimRewardUseCase`。
- **Repository 层**：协议在 `Domain/Protocols/` 中定义，实现类在 `Data/Repositories/`。对外暴露 `async throws` 方法，内部协调 Remote 与 Local DataSource。
- **DataSource 层**：最底层。`RemoteDataSource` 封装 Alamofire 请求，返回 DTO；`LocalDataSource` 封装 Core Data 操作，返回 Domain Model。

### 1.2 目录结构

```
ios/ShortDrama/
├── App/                    # App 入口、SceneDelegate、DI Container 装配
├── Domain/
│   ├── Models/             # 纯业务模型（Struct，遵循 Codable/Sendable/Identifiable）
│   ├── UseCases/           # 业务用例
│   └── Protocols/          # Repository 协议、Service 协议
├── Data/
│   ├── DTO/                # API 返回的原始模型（与 Domain Model 分离）
│   ├── DataSources/        # RemoteDataSource、LocalDataSource
│   └── Repositories/       # Repository 协议实现
├── UI/
│   ├── Pages/              # 页面级 View（Home、Player、Profile…）
│   ├── Components/         # 可复用组件（DramaCard、RateButton…）
│   └── ViewModels/         # 各页面对应的 ViewModel
├── Core/
│   ├── Network/            # Alamofire Session 配置、拦截器、API Router
│   ├── Extensions/         # Color+Theme、Font+Theme、View+Modifier 等
│   ├── DI/                 # DI Container 注册（Swinject Assembly 或 Factory）
│   └── Configuration/      # 环境配置、API BaseURL 管理
├── Resources/
│   ├── Fonts/              # 自定义字体
│   └── Assets.xcassets/    # 图片、颜色、数据资源
└── Tests/
    ├── DomainTests/        # UseCase 单元测试
    ├── DataTests/          # Repository 集成测试（含 Mock）
    └── UITests/            # XCUITest E2E 测试
```

---

## 2. 导航架构

### 2.1 路由定义

- 使用 `NavigationStack` + `NavigationPath`（iOS 16+）实现编程式路由。
- 定义统一的路由枚举 `AppRoute`：
  ```swift
  enum AppRoute: Hashable {
      case dramaDetail(dramaId: String)
      case episodePlayer(dramaId: String, episodeIndex: Int)
      case userProfile(userId: String)
      case rewardHistory
  }
  ```
- 页面通过 `.navigationDestination(for: AppRoute.self)` 注册路由到页面的映射。
- 导航统一由 ViewModel 触发，通过 `Router` 或 NavigationPath 绑定，不在 View 中写导航逻辑。
- 返回上一级统一使用 `@Environment(\.dismiss) private var dismiss`，不自定义返回按钮逻辑。

### 2.2 Deep Link

- 自定义 URL Scheme：`djsdrama://`（定义在 `PRODUCT.md`）。
- 路由格式：`djsdrama://{resource}/{id}?{params}`。
  示例：
  - `djsdrama://drama/12345` → 打开短剧详情页
  - `djsdrama://play/12345?episode=3` → 直接播放指定集数
  - `djsdrama://reward/claim?code=abc` → 打开领奖励页面
- 解析入口在 `App.swift` 的 `.onOpenURL` 中，将 URL 解析为 `AppRoute`，push 到 NavigationPath。
- Universal Link 作为备用方案，关联域名配置在 `apple-app-site-association` 中。

### 2.3 Tab 导航

- 根层级使用 `TabView` + 枚举定义 Tab：
  ```swift
  enum AppTab: Int, CaseIterable {
      case home = 0
      case discover
      case earn
      case profile
  }
  ```
- 每个 Tab 内的导航栈独立维护，使用独立的 `NavigationStack` 和 `NavigationPath`。
- Tab 切换不销毁栈状态——每个 Tab 的 NavigationPath 保存在对应 Tab 的 ViewModel 中。
- `TabView` 使用 `.tabViewStyle(.automatic)` 配合 `.tint()` 设置选中色。

---

## 3. 状态管理

### 3.1 @Observable (iOS 17+)

- 项目部署目标若为 iOS 17+，ViewModel 优先使用 `@Observable` 宏。
- View 中使用 `@State private var viewModel` 持有 ViewModel。
- `@Observable` 自动追踪属性访问，无需 `@Published`。
- 未被 View 读取的辅助属性标记 `@ObservationIgnored`：
  ```swift
  @Observable @MainActor
  final class HomeViewModel {
      var dramas: [Drama] = []
      var isLoading = false
      @ObservationIgnored private var page = 1
      @ObservationIgnored private let fetchUseCase: FetchRecommendationsUseCase
  }
  ```

### 3.2 @StateObject / @ObservedObject (iOS 16 及以下)

- 若需兼容 iOS 16 及以下，ViewModel 遵循 `ObservableObject`，使用 `@StateObject` 持有。
- `@ObservedObject` 仅用于子 View 接收父 View 传入的 ViewModel。
- 区分规则：
  - 当前 View 创建 ViewModel → `@StateObject`
  - 父 View 已创建，子 View 接收 → `@ObservedObject`

### 3.3 @EnvironmentObject

- 用于跨层级共享的全局状态，如：
  - `AuthManager`：当前登录用户信息、Token
  - `ThemeManager`：主题设置
  - `PlayerManager`：全局播放状态（迷你播放器、当前播放剧集）
- 全局状态对象通过 App 入口 `.environmentObject()` 注入。
- 不得滥用 `@EnvironmentObject`——仅用于真正需要跨 Tab 或跨页面深入传递的状态。

---

## 4. 依赖注入

### 4.1 注入方式

- **构造器注入为首选**：所有依赖通过 ViewModel/UseCase/Repository 的 `init` 参数传入。
  ```swift
  final class HomeViewModel {
      private let fetchDramasUseCase: FetchRecommendationsUseCase
      private let claimRewardUseCase: ClaimRewardUseCase

      init(fetchDramasUseCase: FetchRecommendationsUseCase,
           claimRewardUseCase: ClaimRewardUseCase) {
          self.fetchDramasUseCase = fetchDramasUseCase
          self.claimRewardUseCase = claimRewardUseCase
      }
  }
  ```
- **避免属性注入**（`var dependency: ...?`）——编译期不保证依赖非空，运行时容易崩溃。
- 避免 Service Locator 反模式（通过全局单例获取依赖）。

### 4.2 Container 组织

- 若使用 Swinject（参见 open-source-libs.md 的 🔶 标记），通过 `Assembly` 协议按模块注册：
  ```swift
  final class NetworkAssembly: Assembly {
      func assemble(container: Container) {
          container.register(APISession.self) { _ in
              APISession(configuration: .default)
          }.inObjectScope(.container)
      }
  }
  final class DomainAssembly: Assembly {
      func assemble(container: Container) {
          container.register(FetchRecommendationsUseCase.self) { r in
              FetchRecommendationsUseCase(repository: r.resolve(DramaRepository.self)!)
          }
      }
  }
  ```
- Container 在 `App` 入口通过 `Assembler` 组装，各 View 从 Container 解析 ViewModel。
- 若不引入 DI 框架，则采用手动构造：在 App 入口创建所有依赖图，按需传递。

---

## 5. 模块化策略

### 5.1 Swift Package

- 不急于将代码拆分为独立 Swift Package。拆分时机：
  - Core 层稳定后（网络、日志、主题）→ 抽出 `ShortDramaCore` Package
  - 多 target 共享代码（App + Widget + 通知扩展）→ 抽出 Shared Package
- 本地 Package 放在 `ios/Packages/` 目录，通过 Xcode `Package Dependencies` 添加本地引用。

### 5.2 模块职责

| 模块 | 职责 | 依赖 |
|------|------|------|
| `ShortDramaCore` | 网络层、日志、主题、工具类 | Alamofire、Kingfisher、swift-log |
| `ShortDramaDomain` | 业务模型、UseCase、Repository 协议 | 无外部依赖 |
| `ShortDramaData` | Repository 实现、DTO、DataSource | Core + Domain |
| `ShortDramaUI` | SwiftUI 页面与组件 | Domain（不直接依赖 Data） |
| `ShortDramaApp` | App 入口、DI 装配、启动流程 | 所有模块 |

---

## 6. 错误处理

### 6.1 错误类型

定义统一的 App 错误枚举：

```swift
enum AppError: LocalizedError {
    case network(underlying: Error)
    case server(code: Int, message: String)
    case authRequired
    case dataCorrupted
    case notFound
    case cancelled

    var errorDescription: String? {
        switch self {
        case .network(let error): return "网络错误：\(error.localizedDescription)"
        case .server(_, let msg): return msg
        case .authRequired: return "请先登录"
        case .dataCorrupted: return "数据异常，请重试"
        case .notFound: return "内容不存在"
        case .cancelled: return "操作已取消"
        }
    }
}
```

- Repository 层将底层错误（`AFError`、`DecodingError`）转换为 `AppError`。
- ViewModel 层不直接暴露底层错误类型给 View。

### 6.2 错误处理模式

- 网络请求使用 `async throws`：
  ```swift
  func fetchDramas() async throws -> [Drama] { ... }
  ```
- ViewModel 中使用 do-catch 并设置错误状态：
  ```swift
  func loadContent() async {
      isLoading = true
      defer { isLoading = false }
      do {
          dramas = try await fetchUseCase.execute()
          errorMessage = nil
      } catch let error as AppError where error == .cancelled {
          // 任务被取消，忽略
      } catch {
          errorMessage = error.localizedDescription
      }
  }
  ```
- 不使用 `Result` 作为 ViewModel 暴露给 View 的类型——用 `@Published var errorMessage: String?` + View 的 `.alert()` 更合适。

### 6.3 降级策略

- 网络不可用时，Repository 返回本地缓存数据（Core Data / UserDefaults），优先保证用户可浏览已加载内容。
- 图片加载失败时，Kingfisher 展示预定义占位图：`KFImage(url).placeholder(Image("placeholder_drama"))`。
- 视频播放失败时，展示友好错误提示 + "点击重试"按钮，不自动跳转或崩溃。
- 所有降级行为通过 `Logger` 记录，方便线上排查。
