# iOS 端技术方案：PRD-08 用户登录与注册

> 创建日期：2026-07-28
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

PRD-08 在现有 iOS `TabView + NavigationStack + NavigationRouter` 容器内，为 `.profile` 承接真实“我的”频道、全屏登录页、设置页与登录拦截回跳，并在 `Core → Domain → Data → Presentation` 分层下补齐会话持久化、认证请求、single-flight refresh 与冷启动恢复。实现继续遵循 `ios/CLAUDE.md`：Swift 6 + SwiftUI + URLSession，不引入第三方网络/认证库，不直接改 `.xcodeproj/project.pbxproj`。

```text
ProfileTab
  -> ProfileHomeView
     -> observes AuthStore / ProfileViewModel
        -> anonymous: show login CTA
        -> authenticated: show masked phone + settings entry

主动登录
  -> LoginSheet / FullScreenCover
     -> LoginViewModel
        -> SendOtpUseCase
           -> AuthRepository.sendOtp()
           -> POST /api/auth/otp-requests
        -> CreateSessionUseCase
           -> AuthRepository.createSession()
           -> POST /api/auth/sessions
        -> AuthSessionStore.save(session)
        -> AuthStore.transition(.authenticated)
        -> close login + route back

冷启动 / 前台恢复
  -> AuthBootstrapper.restoreIfNeeded()
     -> AuthSessionStore.load()
     -> GetCurrentUserUseCase / RefreshSessionUseCase
     -> AuthStore.transition(.restoring/.refreshing/.authenticated/.anonymous)

受保护请求
  -> AuthenticatedAPIClient.request(endpoint)
     -> inject Authorization bearer
     -> 401 => AuthRefreshCoordinator.refreshIfNeeded()
     -> retry original request once

排行预约拦截
  -> RankingViewModel emits .requireLogin(RankingLoginContext)
  -> RankingHomeView converts to LoginInterceptionContext
  -> NavigationRouter presents login flow
  -> login success => route back to original ranking context
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增 `login(context:)`、`settings`、可选 `profileHome` / `profileEdit` 等最小认证相关 route |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 扩展 | 继续复用 `pendingRoute`；新增登录 modal 管理与回跳处理 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | `.profile` 不再是 `PlaceholderTabView`，改承接真实 `ProfileHomeView` |
| `ios/ShortDrama/Sources/Features/Shell/Views/PlaceholderTabView.swift` | 不变 | 保留 theater / mall / earn 占位，不再承接 profile |
| `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` | 轻微扩展 | `RankingLoginContext` 继续作为排行来源上下文，不直接负责登录实现 |
| `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift` | 扩展 | 继续定义 `RankingLoginContext`，并提供到统一 `LoginInterceptionContext` 的映射 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 修改 | 增加授权 header 注入、401 拦截、单次重放协作点 |
| `ios/ShortDrama/Sources/Core/Storage/PlaybackSessionStore.swift` | 参考复用 | 已有 Keychain 封装模式可复用到 `AuthSessionStore` |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 可选修改 | 若预约接口需统一 auth client，则改依赖支持自动注入 token 的 client |
| `ios/ShortDrama/Sources/Features/Classification/*` | 不变 | 认证能力对其为后续复用基础，不在本期直接接入 |

### 1.2 分层职责

| 层级 | 新增/扩展对象 | 职责 |
|------|--------------|------|
| Core | `AuthSessionStore`、`KeychainAuthSessionStore`、`AuthRefreshCoordinator`、`AuthenticatedAPIClient` / `APIClient` 扩展、`AppLifecycleObserver` | 安全存储、401 刷新、请求重放、生命周期恢复触发 |
| Domain | `AuthSession`、`AuthUser`、`AuthStatus`、`LoginInterceptionContext`、`AuthRepositoryProtocol`、`SendOtpUseCase`、`CreateSessionUseCase`、`RefreshSessionUseCase`、`GetCurrentUserUseCase`、`LogoutUseCase` | 纯业务 contract |
| Data | `AuthRemoteDataSource`、`AuthDTOs`、`AuthRepository` | 认证 API 访问与 DTO 映射 |
| Presentation | `ProfileHomeView`、`LoginView`、`SettingsView`、`AuthStore`、`LoginViewModel`、`ProfileViewModel`、`SettingsViewModel` | 认证 UI、状态机、导航回跳 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增登录与设置相关 route，允许持有登录上下文 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 增加登录页展示、登录成功回跳与取消回退能力 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | `.profile` 承接 `ProfileHomeView()`，并注册 `settings` 等导航目标 |
| `ios/ShortDrama/Sources/Core/Storage/AuthSessionStore.swift` | 新增 | 定义认证会话安全存储协议 |
| `ios/ShortDrama/Sources/Core/Storage/KeychainAuthSessionStore.swift` | 新增 | 复用现有 KeychainClient 模式保存 `AuthSession` |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 修改 | 增加 bearer 注入与 401 refresh/retry 协调点 |
| `ios/ShortDrama/Sources/Core/Network/AuthRequestAdapter.swift` | 新增 | 统一给受保护 endpoint 注入 Authorization |
| `ios/ShortDrama/Sources/Core/Network/AuthRefreshCoordinator.swift` | 新增 | 实现 single-flight refresh |
| `ios/ShortDrama/Sources/Domain/Entities/AuthSession.swift` | 新增 | 认证会话实体 |
| `ios/ShortDrama/Sources/Domain/Entities/AuthUser.swift` | 新增 | 当前登录用户实体 |
| `ios/ShortDrama/Sources/Domain/Entities/AuthStatus.swift` | 新增 | `anonymous/restoring/authenticated/refreshing/expired` |
| `ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift` | 新增 | 登录来源上下文统一结构 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/AuthRepositoryProtocol.swift` | 新增 | 认证仓储协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/SendOtpUseCase.swift` | 新增 | 发送验证码 |
| `ios/ShortDrama/Sources/Domain/UseCases/CreateSessionUseCase.swift` | 新增 | 验证码创建 session |
| `ios/ShortDrama/Sources/Domain/UseCases/RefreshSessionUseCase.swift` | 新增 | refresh session |
| `ios/ShortDrama/Sources/Domain/UseCases/GetCurrentUserUseCase.swift` | 新增 | 获取当前用户 |
| `ios/ShortDrama/Sources/Domain/UseCases/LogoutUseCase.swift` | 新增 | 退出登录 |
| `ios/ShortDrama/Sources/Data/DTOs/AuthDTOs.swift` | 新增 | 对齐 `{ code, data, message }` 结构 |
| `ios/ShortDrama/Sources/Data/DataSources/AuthRemoteDataSource.swift` | 新增 | 调用 `/api/auth/*` 与 `/api/users/me` |
| `ios/ShortDrama/Sources/Data/Repositories/AuthRepository.swift` | 新增 | DTO -> Entity 映射 |
| `ios/ShortDrama/Sources/Features/Profile/Views/ProfileHomeView.swift` | 新增 | “我的”频道最小登录入口 / 登录后摘要 |
| `ios/ShortDrama/Sources/Features/Auth/Views/LoginView.swift` | 新增 | 全屏登录页 |
| `ios/ShortDrama/Sources/Features/Auth/ViewModels/LoginViewModel.swift` | 新增 | 手机号、验证码、协议勾选、倒计时、提交状态 |
| `ios/ShortDrama/Sources/Features/Profile/ViewModels/ProfileViewModel.swift` | 新增 | “我的”频道状态绑定 |
| `ios/ShortDrama/Sources/Features/Profile/Views/SettingsView.swift` | 新增 | 设置页与退出登录入口 |
| `ios/ShortDrama/Sources/Features/Profile/ViewModels/SettingsViewModel.swift` | 新增 | 退出登录确认与执行 |
| `ios/ShortDrama/Sources/Features/Auth/AuthStore.swift` | 新增 | 全局认证状态容器 |
| `ios/ShortDrama/Tests/ViewModelTests/LoginViewModelTests.swift` | 新增 | 覆盖协议门禁、手机号校验、倒计时、成功登录 |
| `ios/ShortDrama/Tests/ViewModelTests/ProfileViewModelTests.swift` | 新增 | 覆盖匿名/登录态切换 |
| `ios/ShortDrama/Tests/ViewModelTests/SettingsViewModelTests.swift` | 新增 | 覆盖退出登录 |
| `ios/ShortDrama/Tests/DataTests/AuthRemoteDataSourceTests.swift` | 新增 | 覆盖认证 endpoint、header、envelope 解码与业务错误码透传 |
| `ios/ShortDrama/Tests/DataTests/AuthRepositoryTests.swift` | 新增 | 覆盖 DTO 映射、错误码到领域错误的转换 |
| `ios/ShortDrama/Tests/CoreTests/APIClientAuthErrorMappingTests.swift` | 新增 | 覆盖 `error.code` -> `APIError.business` 的解析与透传 |
| `ios/ShortDrama/Tests/CoreTests/KeychainAuthSessionStoreTests.swift` | 新增 | 覆盖会话读写、损坏数据清理 |
| `ios/ShortDrama/Tests/CoreTests/AuthRefreshCoordinatorTests.swift` | 新增 | 覆盖 single-flight refresh、`AUTH_REFRESH_EXPIRED` 与单次重放 |

---

## 3. View 层设计

### 3.1 组件层级树

```text
ProfileHomeView
├── AnonymousProfileView
│   ├── Title("登录后同步你的记录")
│   ├── BenefitList
│   └── LoginCTAButton
└── AuthenticatedProfileView
    ├── UserSummaryCard(maskedPhone, displayName)
    ├── SettingsEntryRow
    └── Optional future entries

LoginView (FullScreenCover)
├── LoginNavigationBar
│   ├── CloseButton / BackButton
│   └── Title("手机号登录")
├── LoginHeroCopy
├── PhoneInputSection
│   ├── CountryCodeLabel(+86)
│   └── PhoneTextField
├── OtpInputSection
│   ├── OtpTextField
│   └── SendOtpButton / CooldownLabel
├── AgreementSection
│   ├── Checkbox
│   ├── UserAgreementLink
│   └── PrivacyPolicyLink
├── SubmitButton
└── InlineError / LoadingOverlay

SettingsView
├── NavigationBar
├── LogoutButton
└── LogoutConfirmDialog
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `ProfileHomeView` | View | “我的”频道根页，根据 auth status 展示匿名/登录态 | 否 |
| `AnonymousProfileView` | View | 登录入口与收益文案 | 否 |
| `AuthenticatedProfileView` | View | 登录后摘要与设置入口 | 否 |
| `LoginView` | View | 全屏登录流程 UI | 否 |
| `AgreementSectionView` | View | 协议勾选与链接 | 否 |
| `OtpCountdownButton` | View | 获取验证码 / 冷却态按钮 | 否 |
| `SettingsView` | View | 设置页与退出按钮 | 否 |
| `RankingHomeView` | View | 保持当前业务入口，只负责把拦截上下文交给统一登录流 | 是 |

### 3.3 组件接口定义

```swift
struct ProfileHomeView: View {
    @EnvironmentObject private var router: NavigationRouter
    @EnvironmentObject private var authStore: AuthStore
    @StateObject private var viewModel: ProfileViewModel

    var body: some View {
        Group {
            switch viewModel.viewState {
            case .anonymous:
                AnonymousProfileView(onLogin: {
                    router.presentLogin(context: .profileEntry)
                })
            case .authenticated(let user):
                AuthenticatedProfileView(
                    user: user,
                    onOpenSettings: { router.navigate(to: .settings) }
                )
            case .restoring:
                ProfileLoadingView()
            case .error(let message):
                ProfileErrorView(message: message)
            }
        }
        .task { await viewModel.bindAuthState() }
    }
}
```

```swift
struct LoginView: View {
    @ObservedObject var viewModel: LoginViewModel
    let onClose: () -> Void

    var body: some View {
        // 手机号、验证码、协议区、提交按钮、倒计时、错误提示
    }
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| `AuthStore` -> `ProfileHomeView` | `@EnvironmentObject` / `@Published` | 全局 auth status、当前用户摘要 |
| `LoginViewModel` -> `LoginView` | `@Published` | 手机号、验证码、cooldown、loading、错误信息 |
| `ProfileHomeView` -> `NavigationRouter` | `@EnvironmentObject` | 打开登录页、进入设置页 |
| `RankingHomeView` -> 登录流 | closure + `LoginInterceptionContext` | 匿名预约拦截 |
| `SettingsViewModel` -> `AuthStore` | async action | 退出登录成功后切换匿名态 |

### 3.5 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | 表单主布局使用弹性 VStack + ScrollView | 避免小屏键盘遮挡 |
| Dynamic Type | 使用系统语义字体与容器自适应 | 协议区与错误文案可多行 |
| 深色模式 | 复用系统语义色 / DesignTokens | 提高登录页可读性 |
| 安全区域 | 登录页用全屏 modal + safe area inset | 顶部关闭按钮不侵入状态栏 |
| 键盘处理 | 使用 ScrollView + focused state | 保证验证码输入与按钮可见 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 View | 职责 |
|-----------|----------|------|
| `LoginViewModel` | `LoginView` | 处理手机号/验证码输入、协议勾选、发送 OTP、创建 session、倒计时 |
| `ProfileViewModel` | `ProfileHomeView` | 将 `AuthStore` 状态映射为“我的”频道 UI 状态 |
| `SettingsViewModel` | `SettingsView` | 退出登录确认与执行 |
| `AuthStore` | 全局 | 冷启动恢复、前后台恢复、当前用户与 auth status 单一数据源 |

### 4.2 状态定义

```swift
@MainActor
final class LoginViewModel: ObservableObject {
    enum ViewState: Equatable {
        case editing
        case sendingOtp
        case otpSent(cooldownRemaining: Int)
        case submitting
        case success
        case error(String)
    }

    @Published var phone: String = ""
    @Published var code: String = ""
    @Published var hasAcceptedAgreement = false
    @Published private(set) var viewState: ViewState = .editing
    @Published private(set) var phoneError: String?
    @Published private(set) var codeError: String?
    @Published private(set) var globalError: String?
}
```

```swift
@MainActor
final class AuthStore: ObservableObject {
    @Published private(set) var status: AuthStatus = .anonymous
    @Published private(set) var currentUser: AuthUser?

    func restoreIfNeeded() async { ... }
    func handleLoginSuccess(_ session: AuthSession) async { ... }
    func logout() async { ... }
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `phone` | `String` | `""` | 手机号输入 |
| `code` | `String` | `""` | 验证码输入 |
| `hasAcceptedAgreement` | `Bool` | `false` | 协议勾选态 |
| `viewState` | `LoginViewModel.ViewState` | `.editing` | 登录页主状态 |
| `phoneError` | `String?` | `nil` | 手机号字段级错误 |
| `codeError` | `String?` | `nil` | 验证码字段级错误 |
| `globalError` | `String?` | `nil` | 接口级错误 |
| `AuthStore.status` | `AuthStatus` | `.anonymous` | 全局认证状态 |
| `AuthStore.currentUser` | `AuthUser?` | `nil` | 当前登录用户 |
| `pendingLoginContext` | `LoginInterceptionContext?` | `nil` | 登录成功后的回跳来源 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | View 层表现 |
|---------|---------|-----------|
| 匿名态 | `AuthStore.status == .anonymous` | 我的频道展示 CTA；排行预约点击触发登录 |
| 恢复中 | `.restoring` / `.refreshing` | 我的频道 skeleton；避免闪屏 |
| 登录中 | `LoginViewModel.viewState == .submitting` | 提交按钮 loading + disabled |
| 验证码冷却 | `.otpSent(cooldownRemaining > 0)` | 发送按钮倒计时 |
| 登录成功 | `AuthStore.status == .authenticated` | 关闭登录页并回跳 |
| 登录失败 | `.error(message)` | 内联/Toast 提示，可重试 |

---

## 5. Navigation 路由设计

### 5.1 导航方案

- 顶层仍使用 `NavigationStack + NavigationRouter`。
- 登录页采用 `fullScreenCover`，因为它既支持主动登录，也适合从业务拦截中强提示进入。
- 设置页采用 push route（`AppRoute.settings`），保持“我的”频道层级清晰。

### 5.2 路由清单

| 路由标识 | 目标页面 | 参数 | 导航方式 | 说明 |
|---------|---------|------|---------|------|
| `.login(context:)` | 登录页 | `LoginInterceptionContext?` | `FullScreenCover` | 主动登录或业务拦截统一入口 |
| `.settings` | 设置页 | 无 | Push | 登录后从“我的”频道进入 |
| `.rankingHome` | 排行页 | 现有 route | Push | 登录成功时可回跳 |
| `.profileHome`（可选，不单独建也可） | 我的频道根页 | 无 | Tab root | 登录成功后默认承接页 |

### 5.3 路由管理

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
    case settings
}

struct LoginInterceptionContext: Hashable, Sendable {
    let source: LoginSource
    let returnRoute: AppRoute?
    let rankingContext: RankingLoginContext?
}
```

NavigationRouter 扩展方向：

- 新增 `@Published var presentedLoginContext: LoginInterceptionContext?`
- 提供：
  - `presentLogin(context:)`
  - `completeLogin(using:)`
  - `cancelLogin()`
- 成功登录后：
  - 若 `returnRoute` 存在，优先 `navigate(to:)`
  - 否则 `select(tab: .profile)`

### 5.4 Deep Link 处理（如适用）

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `djsdrama://profile/settings` | `SettingsView` | 无 |
| 认证登录页 | 暂不暴露 Deeplink | 避免外部直接拉起受限流程 |

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | `APIClient`（URLSession） | 继续复用现有网络层 |
| 请求构建 | `APIEndpoint` + Auth endpoints | 新增 auth endpoint 定义 |
| 请求拦截器 | `AuthRequestAdapter` / `APIClient` 扩展 | 给受保护请求注入 bearer |
| 响应解析 | `Codable` + `JSONDecoder` | 解码 `{ code, data, message }` 与错误体 `error.code/error.message` |
| 错误处理 | `APIError(statusCode, businessCode, message)` + Auth-specific mapping | 必须把后端业务错误码透传到 ViewModel |

### 6.2 API 端点定义

```swift
struct SendOtpEndpoint: APIEndpoint {
    typealias Response = ApiEnvelope<SendOtpResponseDTO>
    let phone: String

    var path: String { "/api/auth/otp-requests" }
    var method: HTTPMethod { .post }
    var body: SendOtpRequestDTO? {
        .init(phone: phone, countryCode: "+86", scene: "login")
    }
}

struct GetCurrentUserEndpoint: APIEndpoint {
    typealias Response = ApiEnvelope<AuthUserDTO>
    var path: String { "/api/users/me" }
    var method: HTTPMethod { .get }
    var requiresAuth: Bool { true }
}
```

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 网络超时 | 0~1 | 保持现有策略 | 登录/验证码请求不做激进自动重试 |
| 5xx 服务端错误 | 0 | 用户手动重试 | 避免重复发 OTP |
| 401 token 过期 | 1 | 无退避，先 refresh | refresh 成功后只重放原请求一次 |

### 6.4 single-flight refresh 实现建议

- `AuthRefreshCoordinator` 内部持有 `Task<AuthSession, Error>?`。
- 当多个请求同时 401：
  1. 第一个请求创建 refresh task；
  2. 后续请求 await 同一个 task；
  3. 成功后统一从 `AuthSessionStore` 读取新 session；
  4. 每个原请求最多重试一次。
- `APIClient` 需要补齐错误透传路径：
  - 解析错误体 `{ error: { code, message } }`；
  - 将 `error.code` 透传为 `APIError.business(statusCode: Int, businessCode: String, message: String)`；
  - `AuthRemoteDataSource` / `AuthRepository` 根据 `businessCode` 判断是 `AUTH_INVALID_CODE`、`AUTH_CODE_EXPIRED`、`AUTH_REFRESH_EXPIRED` 还是 `AUTH_UNAUTHORIZED`。
- 若 refresh 失败：
  - coordinator 清空 task；
  - 通知 `AuthStore` 进入 `.expired/.anonymous`；
  - 抛出带 `businessCode` 的 `APIError.business(...)`，确保 ViewModel 可以区分“重新输入验证码”和“重新登录”。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| `AuthSession` | Keychain | `auth.session.payload` | refresh/登出时原子替换或删除 | 安全存储 token 与用户摘要 |
| 登录页瞬时输入 | 内存态 / `@Published` | — | 页面关闭即释放 | 不落盘验证码 |
| OTP 冷却时间戳 | 可选 UserDefaults / Keychain | `auth.otp.cooldownUntil` | 到期后清理 | 登录页关闭再进时恢复倒计时 |
| `pendingLoginContext` | 内存态 | Router / AuthStore | 登录完成或取消后清理 | 无需跨进程持久化 |

### 7.2 Keychain `AuthSessionStore` 设计

```swift
protocol AuthSessionStore: Sendable {
    func load() throws -> AuthSession?
    func save(_ session: AuthSession) throws
    func clear() throws
}

final class KeychainAuthSessionStore: AuthSessionStore, @unchecked Sendable {
    // 复用 PlaybackSessionStore 的 KeychainClient 模式
}
```

### 7.3 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| 当前 `AuthSession` | 单份最新值 | 直到失效/登出 | refresh 成功覆盖、logout 删除 |
| 当前用户摘要 | 不单独缓存 | 跟随 session | 以 session.user 为准 |

### 7.4 数据迁移策略

- 若后续 `AuthSession` 结构升级，`KeychainAuthSessionStore.load()` 读取失败时直接清理损坏数据并回匿名态。
- 不做复杂版本迁移；保证“宁可回匿名，也不进入脏登录态”。

---

## 8. 配置与环境

| 配置项 | 管理方式 | 开发环境值 | 生产环境值 | 说明 |
|--------|---------|----------|-----------|------|
| API Base URL | xcconfig + Info.plist | Debug.xcconfig 注入 | Release.xcconfig 注入 | 继续通过 `AppConfig.apiBaseURL()` 读取 |
| Auth feature flag（可选） | Info.plist / 编译常量 | 默认开启 | 默认开启 | 如需灰度可补充 |
| Keychain access group | 可选配置 | 默认 nil | 生产可扩展 | 首版不强制 |

> ⚠️ 禁止硬编码任何常量。使用 xcconfig + Info.plist 管理配置，敏感信息存入 Keychain。

---

## 9. API 调用清单

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| `POST /api/auth/otp-requests` | 用户点击获取验证码 | 登录页手机号输入 | 进入冷却态 | 内联/Toast |
| `POST /api/auth/sessions` | 用户点击确认登录 | 手机号 + 验证码 | 存 session、更新 `AuthStore`、关闭登录页 | 内联/Toast |
| `POST /api/auth/session-refreshes` | 冷启动恢复 / 401 兜底 / 前台恢复 | Keychain 中 refresh token | 原子替换 session | 失败则清 session |
| `GET /api/users/me` | 冷启动恢复 / 前台校验 | 当前 access token | 恢复登录态 | 401 时转 refresh |
| `DELETE /api/auth/session` | 用户点击退出登录 | 当前 access token | 清 session、切匿名态 | 网络失败也清本地 |
| 受保护业务接口（如预约） | 已登录用户操作 | ViewModel + 自动注入 bearer | 正常更新业务 UI | 401 先 refresh，再重试一次 |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | iOS 端实现方式 |
|---------|---------------|---------------|
| 主动登录入口 | 我的频道匿名 CTA | `ProfileHomeView` 打开 `fullScreenCover` 登录页 |
| 登录成功回跳 | 优先回 `returnRoute` | `NavigationRouter.completeLogin(using:)` |
| 排行预约拦截 | `RankingLoginContext` 接入统一登录流 | `RankingHomeView` 监听 `routeEffect` 后构造 `LoginInterceptionContext` |
| single-flight refresh | 同一时刻一个 refresh | `AuthRefreshCoordinator` 共享 Task |
| 退出登录本地优先 | logout 失败也清 session | `SettingsViewModel.logout()` 最终总是执行 `AuthSessionStore.clear()` |
| 冷启动恢复 | 先 me，401 再 refresh | `AuthStore.restoreIfNeeded()` |
| 会话原子替换 | create/refresh 返回全量 session | `KeychainAuthSessionStore.save(session)` 全量覆盖 |

---

## 11. 边界与错误处理

### 11.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| 网络层 | `APIClient` -> `APIError` | 解析 401/429/503/网络异常 |
| refresh 协调层 | `AuthRefreshCoordinator` | 统一处理 refresh 成功/失败 |
| ViewModel | `do-catch` + 状态字段 | 转成字段级错误 / 全局错误 |
| View 层 | `.alert()` / `.toast()` / 内联错误 | 不直接暴露底层错误文本 |
| 日志 | `os_log` / debug print（受控） | 不打印 token/验证码/完整手机号 |

### 11.2 错误码映射表

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `AUTH_INVALID_PHONE` / `VALIDATION_ERROR` | 请输入正确的手机号 | 手机号输入框下方错误 |
| `AUTH_INVALID_CODE` | 验证码错误，请重新输入 | 验证码输入框下方错误 |
| `AUTH_CODE_EXPIRED` | 验证码已过期，请重新获取 | 清空验证码输入并聚焦发送按钮 |
| `AUTH_CODE_COOLDOWN` / `AUTH_RATE_LIMITED` | 请稍后再试 | 发送按钮进入倒计时/Toast |
| `AUTH_UNAUTHORIZED` | 登录已失效，请重新登录 | 轻提示 + 重新拉起登录 |
| `AUTH_REFRESH_EXPIRED` | 登录状态已过期，请重新登录 | 清 session 后重新拦截登录 |
| `SERVICE_UNAVAILABLE` | 暂时无法登录，请稍后重试 | Toast + 保持输入态 |
| `INTERNAL_ERROR` | 服务开小差了，请稍后重试 | Toast |
| `NETWORK_ERROR` | 网络异常，请检查后重试 | Toast + 重试 |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| 登录页切后台再返回 | 倒计时进行中 | 基于冷却截止时间恢复剩余秒数 | 🔴 |
| Keychain 内容损坏 | session decode 失败 | 清理损坏数据并回匿名态 | 🔴 |
| 多个请求同时 401 | 用户停留多个页面 | 等待同一 refresh task，避免重复弹窗 | 🔴 |
| refresh 成功但重试仍 401 | token 被服务端撤销 | 只重试一次，然后回匿名并重新拦截 | 🔴 |
| 用户快速连点发送验证码 | 短时间多次点击 | loading / cooldown 期间禁用按钮 | 🔴 |
| 用户快速连点确认登录 | 网络慢 | 提交期间禁用按钮 | 🔴 |
| 登录页关闭后再次进入 | 上次已发送验证码 | 可恢复 cooldown，但不强制恢复验证码输入内容 | 🟡 |
| App 长时间后台恢复 | access token 过期 | 先 refresh，再决定是否维持登录态 | 🟡 |
| returnRoute 失效 | 来源页不存在 | 回 `.profile` 或对应 tab 根页 | 🟡 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `ProfileHomeView` | restoring skeleton | 匿名 CTA / 登录后摘要 | — | 轻错误态 | session 损坏后直接匿名 |
| `LoginView` | 发送中 / 提交中 | 登录成功关闭页面 | — | 手机号/验证码/网络错误 | 协议未勾选不提交 |
| `SettingsView` | logout 中 | 退出成功回匿名 | — | logout 网络失败但本地已退出 | — |

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | `LoginViewModel`、`ProfileViewModel`、`SettingsViewModel`、`AuthStore` | 核心路径全覆盖 | Swift Testing |
| 数据层测试 | `AuthRemoteDataSource`、`AuthRepository`、`KeychainAuthSessionStore` | 核心路径全覆盖 | Swift Testing + URLProtocol mock |
| UI 测试 | 主动登录、排行拦截登录、退出登录 | 关键流程覆盖 | 后续可补 XCUITest |

### 12.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| I-01 | 未勾选协议不可发送验证码 | 合法手机号 | 点发送验证码 | 按钮不可提交 | 单元 |
| I-02 | 非法手机号校验 | 手机号不足 11 位 | 点发送验证码 | `phoneError` 非空 | 单元 |
| I-03 | 发送验证码成功 | mock 200 | 点发送验证码 | 进入 cooldown 状态 | 单元 |
| I-04 | 登录成功首登 | mock `isNewUser=true` | 提交验证码 | `AuthStore` 变为 authenticated | 单元 |
| I-05 | 冷启动恢复成功 | Keychain 有未过期 session | 调 restore | 调 `GET /api/users/me` 后 authenticated | 单元 |
| I-06 | 冷启动 refresh 成功 | Keychain 有过期 session | 调 restore | refresh 后 authenticated | 单元 |
| I-07 | refresh 失败回匿名 | Keychain 有无效 refresh token | 调 restore | 清 session，状态 anonymous | 单元 |
| I-08 | 单飞 refresh | 两个请求同时 401 | 触发 refresh | 只调用一次 refresh API | Core 单元 |
| I-09 | 退出登录网络失败 | logout API 报错 | 点退出登录 | 本地仍清 session 并回匿名 | 单元 |
| I-10 | 排行预约登录回跳 | 匿名用户点预约 | 完成登录 | 回到 ranking route | 单元 / UI |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| API 请求 | `URLProtocol` Stub | 遵守 iOS 端测试约束，不发真实网络 |
| Keychain | `KeychainClient` Mock | 复用现有 `PlaybackSessionStore` 模式 |
| Router | Fake `NavigationRouter` / spy | 验证登录成功回跳 |
| 时间 | 注入 `DateProvider` / cooldown deadline | 便于测试倒计时与过期判断 |

---

## 13. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | — | — | 继续复用 URLSession、Keychain、SwiftUI |

> ⚠️ 新增开源依赖前必须征得用户同意（遵守根目录 CLAUDE.md 开发约束）。

---

## 14. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 现有 `APIClient` 没有统一 auth 拦截层 | 所有受保护请求 | 🔴 | 高 | 在 `APIClient` 内或其外层加入 auth adapter + refresh coordinator | 临时只给 auth-sensitive data source 接入包装 client |
| `.profile` 当前是 placeholder | 我的频道承接 | 🔴 | 高 | 本期直接替换为真实 ProfileHomeView | 若实现受阻，至少先落地 CTA + login modal |
| Keychain session 与 PlaybackSessionStore 模式不同 | 安全存储实现 | 🟡 | 中 | 复用现有 `KeychainClient`，只新增 auth-specific store | 先以单一 JSON payload 存储，后续再拆字段 |
| 登录回跳上下文设计不足 | 排行及后续 PRD 复用 | 🟡 | 中 | 一开始就定义 `LoginInterceptionContext` 结构 | 允许新增字段保持兼容 |
| 冷启动恢复造成界面闪烁 | 用户体验 | 🟡 | 中 | profile/home 对 auth restoring 展示 skeleton | 启动阶段短暂使用 overlay |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/decisions/2026-07-24-supabase-baas.md` | Auth 基础设施 | 使用 Supabase Auth |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `ios/CLAUDE.md` | SwiftUI、MVVM + Clean Architecture、URLSession、Swift Testing 约束 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 当前还没有 login/settings route |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 已支持 `pendingRoute` 与统一导航入口 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | `.profile` 仍为 placeholder，但 classification 已是实页，说明可按同样方式升级 |
| `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` | 已有 `.requireLogin(RankingLoginContext)` 拦截 effect |
| `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift` | `RankingLoginContext` 实际定义位置 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 现有请求构建与错误解析基础 |
| `ios/ShortDrama/Sources/Core/Network/APIError.swift` | 当前统一错误模型 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | endpoint/data source 风格基线 |
| `ios/ShortDrama/Sources/Core/Storage/PlaybackSessionStore.swift` | Keychain 封装与注入方式可复用 |
| `ios/ShortDrama/Sources/Features/Search/Views/SearchResultView.swift` | 路由承接与 StateObject 注入模式参考 |
