# Android 端技术方案：PRD-08 用户登录与注册

> 创建日期：2026-07-28
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

PRD-08 在现有 Android `Compose + Navigation + Hilt + Retrofit/OkHttp + StateFlow` 架构内，为 `PROFILE` Tab 接入真实“我的”频道、全屏 Native 登录页、设置页与统一登录拦截，并补齐会话持久化、401 single-flight refresh、冷启动恢复与退出登录清理。实现继续遵循 `android/CLAUDE.md`：使用 Kotlin、Jetpack Compose、Hilt、Retrofit/OkHttp、DataStore / 本地安全存储封装，不直接依赖 BuildConfig 常量；首版不新增新的开源三方依赖，Android 安全存储优先使用 Jetpack Security `EncryptedSharedPreferences`（属于 Jetpack 官方组件，接入前仍需在 coding 前与用户确认依赖变更）。

```text
ProfileGraph
  -> ProfileRoute
     -> ProfileViewModel observes AuthSessionRepository
        -> anonymous: show login CTA
        -> authenticated: show masked phone + settings entry

主动登录 / 业务拦截
  -> LoginRoute(full screen)
     -> LoginViewModel
        -> SendOtpUseCase
           -> AuthRepository.sendOtp()
           -> POST /api/auth/otp-requests
        -> CreateSessionUseCase
           -> AuthRepository.createSession()
           -> POST /api/auth/sessions
        -> AuthSessionStore.save(session)
        -> AuthStateHolder.emit(authenticated)
        -> navController pop + navigate(returnRoute)

冷启动 / 前后台恢复
  -> AuthBootstrapper.restoreIfNeeded()
     -> AuthSessionStore.read()
     -> GET /api/users/me
     -> 401 => RefreshSessionUseCase
     -> success => authenticated / fail => anonymous

受保护请求
  -> Retrofit ApiService call
     -> AuthInterceptor inject bearer
     -> Authenticator / RefreshCoordinator handles 401
     -> refresh success => retry once
     -> refresh fail => clear session + surface unauthorized

排行预约拦截
  -> RankingViewModel emits RequireLogin(returnRoute)
  -> NavGraph navigates LoginRoute(returnRoute)
  -> login success => back to ranking route
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 新增 `LOGIN`、`SETTINGS` 以及带 `returnRoute` 的登录导航构造器 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 将 profile placeholder 替换为真实 Profile/Login/Settings graph；接通 ranking 登录拦截 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt` | 修改 | 注入 access token，并与 refresh 协调层协作 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthSessionProvider.kt` | 扩展 | 从 `isLoggedIn()` 升级为可读 session / auth status / user summary 的 provider |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt` | 修改 | 不再注入假登录 provider，改为真实 auth repository / provider / store |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | 轻微扩展 | 保留 `RequireLogin(returnRoute)` effect，不在 ViewModel 内实现登录逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt` | 保持接点 | 继续通过 `onRequireLogin` 将拦截交给导航层 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增 auth 相关接口定义 |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/PlaybackSessionStore.kt` | 参考复用 | 复用其 DataStore 封装风格处理非敏感状态；认证会话本身改由 `EncryptedPrefsAuthSessionStore` 安全存储 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 注册认证仓储、用例与 session store 依赖 |

### 1.2 分层职责

| 层级 | 新增/扩展对象 | 职责 |
|------|--------------|------|
| core | `AuthSessionStore`、`SecureAuthSessionStore`、`EncryptedPrefsAuthSessionStore`、`AuthRefreshCoordinator`、`AuthAuthenticator`、`AppForegroundObserver` | 会话安全持久化、401 refresh、生命周期恢复触发 |
| data | `AuthRemoteDataSource`、`AuthRepositoryImpl`、`AuthDto`、`AuthSessionEntityMapper` | 调用认证 API，DTO 映射为 domain entity |
| domain | `AuthSession`、`AuthUser`、`AuthStatus`、`LoginInterceptionContext`、`AuthRepository`、`AuthSessionProvider`、`SendOtpUseCase`、`CreateSessionUseCase`、`RefreshSessionUseCase`、`GetCurrentUserUseCase`、`LogoutUseCase` | 认证业务 contract |
| feature/presentation | `ProfileScreen`、`LoginScreen`、`SettingsScreen`、`ProfileViewModel`、`LoginViewModel`、`SettingsViewModel`、`AuthStateHolder` | UI、状态机、导航回跳 |

### 1.3 认证状态单一数据源

Android 端不在多个 ViewModel 内各自维护“是否登录”布尔值，而是将认证状态收敛到单一的 `AuthStateHolder` / `AuthSessionProvider`：

- `AuthStateHolder`：应用级 `StateFlow<AuthState>`，对 UI 暴露 `anonymous / restoring / authenticated / refreshing / expired`。
- `AuthSessionProvider`：供网络层和业务层读取当前 session、access token、current user。
- `AuthSessionStore`：负责敏感 session 的安全落盘（`EncryptedSharedPreferences`），不直接暴露 UI 状态；DataStore 仅用于 cooldown 等非敏感辅助状态。

这样可以避免：

1. Profile 与 Ranking 各自维护一份登录态；
2. refresh 成功后旧页面仍然使用过期 token；
3. logout 后残留假登录视图。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 新增登录与设置 route；提供 `login(returnRoute)` builder |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 接通 Profile / Login / Settings 页面与 ranking 拦截导航 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增 `requestOtp`、`createAuthSession`、`refreshAuthSession`、`getCurrentUser`、`logout` |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt` | 修改 | 从 `AuthSessionProvider` 读取 token，给受保护请求注入 bearer |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthAuthenticator.kt` | 新增 | 处理 401 refresh 与单次重试 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthRefreshCoordinator.kt` | 新增 | 实现 single-flight refresh，避免并发 401 风暴 |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/AuthSessionStore.kt` | 新增 | 定义认证会话持久化接口 |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/EncryptedPrefsAuthSessionStore.kt` | 新增 | 使用 Jetpack Security `EncryptedSharedPreferences` 安全保存 `AuthSession` |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/AuthSessionSerializer.kt` | 新增 | 负责 `AuthSession` JSON 序列化/反序列化，供安全存储层复用 |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/AuthCooldownStore.kt` | 新增 | 使用 DataStore 保存非敏感的验证码 cooldown 截止时间 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/AuthSession.kt` | 新增 | 认证会话实体 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/AuthUser.kt` | 新增 | 当前用户摘要 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/AuthStatus.kt` | 新增 | `ANONYMOUS/RESTORING/AUTHENTICATED/REFRESHING/EXPIRED` |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/LoginInterceptionContext.kt` | 新增 | 统一登录来源上下文 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthRepository.kt` | 新增 | 认证仓储接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthSessionProvider.kt` | 修改 | 从布尔接口升级为完整 session provider |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/SendOtpUseCase.kt` | 新增 | 发送验证码 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/CreateSessionUseCase.kt` | 新增 | 验证码创建 session |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/RefreshSessionUseCase.kt` | 新增 | refresh session |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetCurrentUserUseCase.kt` | 新增 | 获取当前用户 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/LogoutUseCase.kt` | 新增 | 退出登录 |
| `android/app/src/main/java/com/djs66256/short_drama/data/remote/dto/AuthDtos.kt` | 新增 | 对齐 Backend envelope / DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/remote/AuthRemoteDataSource.kt` | 新增 | 认证 API 调用封装 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/AuthRepositoryImpl.kt` | 新增 | DTO -> domain model 映射 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/ProfileScreen.kt` | 新增 | “我的”频道根页 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/profile/viewmodel/ProfileViewModel.kt` | 新增 | 匿名/登录态摘要绑定 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/auth/ui/LoginScreen.kt` | 新增 | 全屏登录页 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt` | 新增 | 手机号、验证码、协议、倒计时、登录提交 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/SettingsScreen.kt` | 新增 | 设置页与退出按钮 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/profile/viewmodel/SettingsViewModel.kt` | 新增 | 退出登录与确认态 |
| `android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthStateHolder.kt` | 新增 | 应用级认证状态 holder |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt` | 修改 | 提供 AuthInterceptor / AuthAuthenticator / AuthStateHolder |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 绑定 AuthRepositoryImpl、AuthSessionStore、AuthSessionProvider |
| `android/app/src/test/.../LoginViewModelTest.kt` | 新增 | 登录页面核心交互测试 |
| `android/app/src/test/.../ProfileViewModelTest.kt` | 新增 | Profile 匿名/登录态切换测试 |
| `android/app/src/test/.../SettingsViewModelTest.kt` | 新增 | 退出登录测试 |
| `android/app/src/test/.../AuthRepositoryImplTest.kt` | 新增 | DTO 映射与错误处理测试 |
| `android/app/src/test/.../AuthRefreshCoordinatorTest.kt` | 新增 | single-flight refresh 测试 |
| `android/app/src/test/.../EncryptedPrefsAuthSessionStoreTest.kt` | 新增 | 安全持久化读写、损坏数据清理与 clear 行为测试 |
| `android/app/src/test/.../AuthCooldownStoreTest.kt` | 新增 | cooldown 截止时间读写测试 |

---

## 3. UI 层设计

### 3.1 组件层级树

```text
ProfileScreen
├── when(authState)
│   ├── AnonymousProfileContent
│   │   ├── Title("登录后同步你的记录")
│   │   ├── BenefitList
│   │   └── LoginButton
│   ├── AuthenticatedProfileContent
│   │   ├── UserSummaryCard(maskedPhone, displayName)
│   │   ├── SettingsRow
│   │   └── Optional future entry list
│   └── ProfileLoadingContent

LoginScreen
├── TopAppBar(close/back)
├── LoginHeroSection
├── PhoneInputField
├── OtpInputField
├── SendOtpButton / CooldownText
├── AgreementRow(checkbox + links)
├── SubmitButton
└── InlineError / Snackbar

SettingsScreen
├── TopAppBar
├── LogoutButton
└── ConfirmLogoutDialog
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `ProfileScreen` | Compose Screen | “我的”频道根页，承接登录入口与登录后摘要 | 否 |
| `AnonymousProfileContent` | Compose | 展示登录 CTA 与权益文案 | 否 |
| `AuthenticatedProfileContent` | Compose | 展示用户摘要与设置入口 | 否 |
| `LoginScreen` | Compose Screen | 全屏登录流程 UI | 否 |
| `AgreementSection` | Compose | 协议勾选与跳转链接 | 否 |
| `OtpSendButton` | Compose | 获取验证码/倒计时 UI | 否 |
| `SettingsScreen` | Compose Screen | 设置页与退出登录 | 否 |
| `RankingScreen` | Compose Screen | 继续复用现有 `onRequireLogin` 接点 | 是 |

### 3.3 组件接口定义

```kotlin
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLoginClick: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState) {
        is ProfileUiState.Anonymous -> AnonymousProfileContent(onLoginClick = onLoginClick)
        is ProfileUiState.Authenticated -> AuthenticatedProfileContent(
            user = (uiState as ProfileUiState.Authenticated).user,
            onOpenSettings = onOpenSettings,
        )
        ProfileUiState.Restoring -> ProfileLoadingContent()
        is ProfileUiState.Error -> ProfileErrorContent(
            message = (uiState as ProfileUiState.Error).message,
            onRetry = viewModel::restore,
        )
    }
}
```

```kotlin
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onClose: () -> Unit,
    onAgreementClick: (AgreementType) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // 输入、按钮、错误、loading 等全部由 uiState 驱动
}
```

### 3.4 状态来源与数据传递

| 传递方向 | 方式 | 场景 |
|---------|------|------|
| `AuthStateHolder` -> `ProfileViewModel` | `StateFlow<AuthState>` | “我的”频道匿名/登录态同步 |
| `LoginViewModel` -> `LoginScreen` | `StateFlow<LoginUiState>` | 输入态、倒计时、按钮状态、错误提示 |
| `NavGraph` -> `LoginScreen` | `navArgument(returnRoute)` | 主动登录 / 业务拦截回跳 |
| `RankingScreen` -> `NavGraph` | `onRequireLogin(returnRoute)` | 匿名预约拦截 |
| `SettingsViewModel` -> `AuthStateHolder` | suspend action | 退出登录后清本地并更新状态 |

### 3.5 适配策略

| 维度 | 策略 | 说明 |
|------|------|------|
| 小屏与键盘 | `Column + verticalScroll + imePadding()` | 避免登录按钮被遮挡 |
| 深色模式 | 使用主题色与 Material 语义色 | 保持与全局主题一致 |
| 可访问性 | 支持 TalkBack label、按钮禁用态说明 | 协议与倒计时文案清晰 |
| 横竖屏 | 表单区域自适应，不依赖固定高度 | 避免横屏裁切 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联页面 | 职责 |
|-----------|---------|------|
| `LoginViewModel` | `LoginScreen` | 输入校验、协议门禁、发送 OTP、登录提交、倒计时恢复 |
| `ProfileViewModel` | `ProfileScreen` | 监听全局 auth state，映射为 Profile UI 状态 |
| `SettingsViewModel` | `SettingsScreen` | 退出登录确认、执行 logout、本地清理 |
| `AuthStateHolder` | 应用级 | 冷启动恢复、前后台恢复、登录成功/退出成功后的统一状态切换 |

### 4.2 状态建模

```kotlin
sealed interface LoginUiState {
    data class Editing(
        val phone: String = "",
        val code: String = "",
        val hasAcceptedAgreement: Boolean = false,
        val phoneError: String? = null,
        val codeError: String? = null,
        val globalError: String? = null,
        val cooldownRemainingSeconds: Int = 0,
        val isSendingOtp: Boolean = false,
        val isSubmitting: Boolean = false,
    ) : LoginUiState

    data object Success : LoginUiState
}
```

```kotlin
sealed interface AuthState {
    data object Anonymous : AuthState
    data object Restoring : AuthState
    data class Authenticated(val session: AuthSession) : AuthState
    data object Refreshing : AuthState
    data object Expired : AuthState
}
```

### 4.3 核心字段

| 字段 | 类型 | 初始值 | 说明 |
|------|------|--------|------|
| `phone` | `String` | `""` | 手机号输入 |
| `code` | `String` | `""` | 验证码输入 |
| `hasAcceptedAgreement` | `Boolean` | `false` | 协议勾选态 |
| `cooldownRemainingSeconds` | `Int` | `0` | 验证码倒计时 |
| `isSendingOtp` | `Boolean` | `false` | 发送验证码按钮 loading |
| `isSubmitting` | `Boolean` | `false` | 确认登录按钮 loading |
| `globalError` | `String?` | `null` | 接口失败轻提示 / 内联错误 |
| `AuthStateHolder.state` | `StateFlow<AuthState>` | `Anonymous` | 全局 auth 状态 |
| `pendingLoginContext` | `LoginInterceptionContext?` | `null` | 登录成功后的回跳来源 |

### 4.4 UI 状态矩阵

| UI 状态 | 条件 | View 层表现 |
|---------|------|-----------|
| 匿名态 | `AuthState.Anonymous` | Profile 显示 CTA，Ranking 拦截登录 |
| 恢复中 | `Restoring` / `Refreshing` | Profile skeleton / disable sensitive CTA |
| 发送验证码中 | `isSendingOtp=true` | 按钮 loading 且禁用 |
| 验证码冷却 | `cooldownRemainingSeconds > 0` | 按钮显示倒计时 |
| 登录提交中 | `isSubmitting=true` | 提交按钮 loading |
| 登录成功 | `LoginUiState.Success` | 关闭登录页并按上下文回跳 |
| 登录失败 | `globalError != null` / 字段错误 | Toast / 输入框错误 |

---

## 5. Navigation 路由设计

### 5.1 路由清单

| 路由 | 参数 | 说明 |
|------|------|------|
| `profile` | 无 | “我的”频道根页 |
| `login?returnRoute={returnRoute}&source={source}` | 可选 | 登录页，既支持主动登录也支持业务拦截 |
| `settings` | 无 | 设置页 |

### 5.2 `AppDestination` 扩展建议

```kotlin
object AppDestination {
    object Route {
        const val HOME = "home"
        const val THEATER = "theater"
        const val MALL = "mall"
        const val EARN = "earn"
        const val PROFILE = "profile"
        const val SETTINGS = "settings"
        const val LOGIN = "login?returnRoute={returnRoute}&source={source}"
        const val RANKING = "ranking?contentType={contentType}&type={type}"
    }

    fun login(returnRoute: String? = null, source: String = "profile"): String {
        val encodedReturnRoute = Uri.encode(returnRoute ?: "")
        return "login?returnRoute=$encodedReturnRoute&source=$source"
    }
}
```

### 5.3 `NavGraph` 接入方式

| 位置 | 变更说明 |
|------|---------|
| `Graph.PROFILE` | `PROFILE` 由 placeholder 改为 `ProfileScreen` |
| `PROFILE -> SETTINGS` | Push 进入设置页 |
| `PROFILE / ranking intercept -> LOGIN` | 统一导航到 `AppDestination.login(...)` |
| `LOGIN success` | `popBackStack()` + `navigate(returnRoute)` 或切回 `PROFILE` |
| `LOGIN cancel` | 返回来源页，不执行待处理动作 |

### 5.4 排行登录拦截复用

当前 Android 已有：

```kotlin
sealed interface RankingEffect {
    data class RequireLogin(val returnRoute: String) : RankingEffect
    data class ShowMessage(val message: String) : RankingEffect
}
```

本期只需要在 `NavGraph.kt` 中将：

```kotlin
onRequireLogin = { _ ->
    // Login flow is not implemented in this PRD yet.
}
```

改为：

```kotlin
onRequireLogin = { returnRoute ->
    navController.navigate(
        AppDestination.login(
            returnRoute = returnRoute,
            source = "ranking_booking",
        ),
    )
}
```

这样既不侵入 Ranking 领域逻辑，也为后续评论/消息等拦截场景复用提供统一模式。

---

## 6. 网络层设计

### 6.1 Retrofit 接口定义

| 方法 | 路径 | 请求 DTO | 响应 DTO | 是否鉴权 |
|------|------|---------|---------|---------|
| `POST` | `/api/auth/otp-requests` | `SendOtpRequestDto` | `ApiEnvelope<OtpRequestDataDto>` | 否 |
| `POST` | `/api/auth/sessions` | `CreateSessionRequestDto` | `ApiEnvelope<AuthSessionDto>` | 否 |
| `POST` | `/api/auth/session-refreshes` | `RefreshSessionRequestDto` | `ApiEnvelope<AuthSessionDto>` | 否 |
| `GET` | `/api/users/me` | — | `ApiEnvelope<AuthUserDto>` | 是 |
| `DELETE` | `/api/auth/session` | — | `ApiEnvelope<Unit?>` | 是 |

建议在 `ApiService.kt` 内扩展：

```kotlin
interface ApiService {
    @POST("auth/otp-requests")
    suspend fun requestOtp(@Body body: SendOtpRequestDto): ApiEnvelope<OtpRequestDataDto>

    @POST("auth/sessions")
    suspend fun createAuthSession(@Body body: CreateSessionRequestDto): ApiEnvelope<AuthSessionDto>

    @POST("auth/session-refreshes")
    suspend fun refreshAuthSession(@Body body: RefreshSessionRequestDto): ApiEnvelope<AuthSessionDto>

    @GET("users/me")
    suspend fun getCurrentUser(): ApiEnvelope<AuthUserDto>

    @DELETE("auth/session")
    suspend fun logout(): ApiEnvelope<Unit?>
}
```

### 6.2 `AuthInterceptor` 设计

- 对标 `requiresAuth` 概念，为受保护请求注入 `Authorization: Bearer <accessToken>`。
- 不给 OTP / login / refresh 等匿名接口注入过期 token。
- 若当前无 access token，则保持原请求，由上层/服务端返回 401，再由业务决定是否拦截登录。

```kotlin
class AuthInterceptor @Inject constructor(
    private val authSessionProvider: AuthSessionProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (!original.requiresAuth()) return chain.proceed(original)

        val token = authSessionProvider.currentAccessToken() ?: return chain.proceed(original)
        val request = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}
```

### 6.3 `AuthAuthenticator` + `AuthRefreshCoordinator`

Android 端建议把 refresh 放在 OkHttp `Authenticator` 层，而不是每个 Repository 单独处理，原因：

1. 401 来源可能遍布多个接口；
2. `Authenticator` 更适合生成一次刷新后重试原请求；
3. 可以集中实现 “最多重试一次” 与 “single-flight”。

职责拆分：

| 对象 | 职责 |
|------|------|
| `AuthAuthenticator` | 收到 401 后尝试 refresh，成功则返回带新 token 的请求；失败则返回 null |
| `AuthRefreshCoordinator` | 保证同一时刻只有一个 refresh 请求在执行 |
| `AuthSessionProvider` | 提供当前 refresh token / 保存新 session / 清空 session |

single-flight 方案：

- coordinator 内部维护 `Mutex + Deferred<Result<AuthSession>>`；
- 首个 401 创建 refresh 协程；
- 后续 401 等待同一结果；
- refresh 成功后更新 `AuthStateHolder` 与 `AuthSessionStore`；
- refresh 失败后统一清理本地 session，所有等待者拿到失败结果。

### 6.4 请求重试规则

| 场景 | 行为 | 说明 |
|------|------|------|
| 普通网络错误 | 不自动重试 | 避免验证码重复发送 |
| OTP 请求 429 | 不重试 | 依赖后端 cooldown 告知 UI |
| 401 + 有 refresh token | refresh 后重试一次 | 单次重放 |
| refresh 后仍 401 | 不再重试 | 清 session，回匿名 |
| logout 接口失败 | 不重试 | 本地仍执行退出 |

---

## 7. 数据持久化策略

### 7.1 存储策略

| 数据 | 存储方式 | Key/位置 | 生命周期 | 说明 |
|------|---------|---------|---------|------|
| `AuthSession` | `EncryptedSharedPreferences`（通过 `SecureAuthSessionStore` 封装） | `auth_session_payload` | 登录成功写入，refresh 覆盖，logout 删除 | token 与 refresh token 必须进入安全存储 |
| OTP cooldown 截止时间 | `DataStore` | `auth_otp_cooldown_until` | 倒计时结束即忽略 | 非敏感数据继续使用 DataStore，页面重建后恢复倒计时 |
| 登录表单输入 | 仅内存态 | `LoginViewModel` | 页面销毁即释放 | 不持久化验证码 |
| `pendingLoginContext` | 导航参数 / 内存 | `NavBackStackEntry` | 登录完成或取消后清理 | 不落盘 |

### 7.2 `AuthSessionStore` 设计

```kotlin
interface AuthSessionStore {
    suspend fun read(): AuthSession?
    suspend fun write(session: AuthSession)
    suspend fun clear()
}
```

`EncryptedPrefsAuthSessionStore` 设计约束：

- 使用单一 JSON payload 序列化整份 `AuthSession`，但底层必须存放在 `EncryptedSharedPreferences`；
- refresh 与登录成功都执行原子覆盖；
- decode 失败时清理损坏数据并返回 null；
- 对外仍通过 `AuthSessionStore` 抽象暴露，避免上层感知具体安全存储实现。

### 7.3 Provider / Holder 协作

| 组件 | 负责内容 |
|------|---------|
| `AuthSessionStore` | 持久化读写 |
| `AuthSessionProvider` | 提供当前 access token、refresh token、isLoggedIn、currentUser |
| `AuthStateHolder` | 对 UI 暴露 `StateFlow<AuthState>` |

### 7.4 数据迁移策略

- 首版不拆分多个 preference key，优先使用单一 payload 以减少一致性问题。
- 若后续 `AuthSession` 字段扩展，旧 payload decode 失败时直接清空，回匿名态重新登录。

---

## 8. 配置与环境

| 配置项 | 读取方式 | 开发环境 | 生产环境 | 说明 |
|--------|---------|---------|---------|------|
| API Base URL | `AppConfig` | 指向本地/测试 Backend | 指向正式 Backend | 遵守 `android/CLAUDE.md`，不直读 BuildConfig |
| 登录功能开关（可选） | `AppConfig` / Remote config 预留 | 默认开启 | 默认开启 | 如需灰度可后续补充 |
| DataStore 文件名 | 常量封装 | `auth_prefs` | `auth_prefs` | 非敏感配置，可代码内命名但不包含环境地址 |

> ⚠️ 禁止硬编码固定环境地址、token、localhost 等敏感常量。所有环境差异继续走 `AppConfig`。

---

## 9. API 调用清单

| API | 调用时机 | 数据来源 | 成功后动作 | 失败后处理 |
|-----|---------|---------|-----------|-----------|
| `POST /api/auth/otp-requests` | 登录页点击获取验证码 | 手机号输入 | 进入 cooldown | 内联错误 / Snackbar |
| `POST /api/auth/sessions` | 登录页点击确认登录 | 手机号 + 验证码 | 保存 session、更新 `AuthStateHolder`、回跳 | 内联错误 / Snackbar |
| `POST /api/auth/session-refreshes` | 冷启动恢复 / 401 兜底 / 前台恢复 | 本地 refresh token | 原子替换 session | 清 session 回匿名 |
| `GET /api/users/me` | 冷启动恢复、前台校验 | 当前 access token | 确认登录态用户摘要 | 401 时转 refresh |
| `DELETE /api/auth/session` | 设置页点击退出登录 | 当前 access token | 清 session 并切匿名 | 网络失败也清本地 |
| 受保护业务接口（如预约） | 已登录用户发起操作 | 网络层自动注入 bearer | 返回正常业务结果 | 401 走 refresh 再重试一次 |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | shared contract | Android 落地方式 |
|---------|---------------|------------------|
| 主动登录入口 | Profile 匿名 CTA | `ProfileScreen` 上的 `LoginButton` 导航到 `login` |
| 排行预约拦截 | `RequireLogin(returnRoute)` -> 统一登录流 | `NavGraph` 在 `onRequireLogin` 中导航到 `AppDestination.login(returnRoute)` |
| 登录成功回跳 | 优先回 `returnRoute` | `LoginViewModel` 发出 success effect；`NavGraph` 根据参数回跳 |
| single-flight refresh | 同一时刻只一条 refresh | `AuthRefreshCoordinator` 使用共享协程/锁控制 |
| 原子替换 session | 登录/refresh 返回全量 session | `AuthSessionStore.write(session)` 整体覆盖 |
| refresh 失败回匿名 | 清 session + 重新拦截 | `AuthStateHolder` 先 `Expired` 后 `Anonymous` |
| 退出登录本地优先 | 接口失败也清本地 | `SettingsViewModel.logout()` 最终总是 clear + 回匿名 |
| 冷启动恢复 | 先 `me` 后 `refresh` | `AuthBootstrapper` / `AuthStateHolder.restoreIfNeeded()` |

---

## 11. 边界与错误处理

### 11.1 错误处理分层

| 层级 | 机制 | 说明 |
|------|------|------|
| Retrofit / OkHttp | `ApiResult` / 异常映射 | 统一承接 HTTP 与网络异常 |
| `AuthAuthenticator` | 401 refresh + retry once | 隔离网络层认证恢复逻辑 |
| Repository | DTO / envelope 解包 | 把后端错误码映射到 domain 错误 |
| ViewModel | `StateFlow` + UI 错误字段 | 区分字段错误与全局错误 |
| UI | `TextField supportingText` / Snackbar / Dialog | 不泄漏底层堆栈和敏感信息 |

### 11.2 错误码映射

| 后端错误码 | Android 表现 | 用户提示 |
|-----------|-------------|---------|
| `AUTH_INVALID_PHONE` / `VALIDATION_ERROR` | `phoneError` | 请输入正确的手机号 |
| `AUTH_INVALID_CODE` | `codeError` / Snackbar | 验证码错误，请重新输入 |
| `AUTH_CODE_EXPIRED` | 清空验证码输入 + Snackbar | 验证码已过期，请重新获取 |
| `AUTH_CODE_COOLDOWN` / `AUTH_RATE_LIMITED` | 保持 cooldown / Snackbar | 请稍后再试 |
| `AUTH_UNAUTHORIZED` | 触发 refresh 或 clear session + 登录拦截 | 登录已失效，请重新登录 |
| `AUTH_REFRESH_EXPIRED` | clear session + 登录拦截 | 登录状态已过期，请重新登录 |
| `SERVICE_UNAVAILABLE` | Snackbar | 暂时无法登录，请稍后重试 |
| `INTERNAL_ERROR` | Snackbar | 服务开小差了，请稍后重试 |
| 网络异常 | Snackbar | 网络异常，请检查后重试 |

### 11.3 关键边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| profile tab 冷启动闪烁 | 读本地 session 与 me/refresh 之间 | `Restoring` skeleton，不先展示假登录态 | 🔴 |
| 多请求同时 401 | 排行/详情/我的并发请求 | single-flight refresh | 🔴 |
| refresh 成功但重试仍 401 | 服务端吊销 session | 终止重试，清 session 回匿名 | 🔴 |
| 登录页旋转或重组 | Compose 重组 | ViewModel 持有输入态与 cooldown | 🔴 |
| 登录页切后台返回 | 倒计时仍在进行 | 按 cooldown 截止时间恢复剩余秒数 | 🟡 |
| DataStore payload 损坏 | decode 失败 | 清除数据并回匿名 | 🔴 |
| returnRoute 非法或为空 | 拦截来源失效 | 回 `PROFILE` 根页 | 🟡 |
| logout 接口超时 | 服务端退出异常 | 仍清本地并回匿名 | 🔴 |
| 用户快速连点发送/登录 | 网络慢 | loading 期间禁用按钮 | 🔴 |

### 11.4 UI 状态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `ProfileScreen` | restoring skeleton | 登录后摘要 / 匿名 CTA | — | 恢复失败可重试 | session 损坏后直接匿名 |
| `LoginScreen` | 发送中 / 登录中 | success 后关闭页面 | — | 手机号/验证码/网络错误 | 协议未勾选仅禁用提交 |
| `SettingsScreen` | logout 中 | 清 session 后返回匿名态 | — | logout 接口失败但本地已退出 | — |

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖对象 | 目标 |
|---------|---------|------|
| 单元测试 | `LoginViewModel`、`ProfileViewModel`、`SettingsViewModel`、`AuthStateHolder` | 覆盖核心状态机与输入校验 |
| Repository / Data 测试 | `AuthRepositoryImpl`、`AuthRemoteDataSource`、`EncryptedPrefsAuthSessionStore`、`AuthCooldownStore` | 覆盖 DTO 映射、错误处理与敏感/非敏感数据分层持久化 |
| Network / Core 测试 | `AuthRefreshCoordinator`、`AuthAuthenticator` | 覆盖 single-flight refresh 与单次重试 |
| Compose UI 测试 | `ProfileScreen`、`LoginScreen`、`SettingsScreen` | 覆盖关键交互与渲染 |

### 12.2 关键测试场景

| 编号 | 场景 | Given | When | Then | 类型 |
|------|------|-------|------|------|------|
| A-01 | 未勾选协议不可发送验证码 | 合法手机号 | 点击发送 | 按钮不可触发请求 | 单元/UI |
| A-02 | 手机号非法校验 | 手机号长度错误 | 点击发送 | `phoneError` 显示 | 单元 |
| A-03 | 发送验证码成功 | mock 200 | 点击发送 | cooldown 开始 | 单元 |
| A-04 | 登录成功首登 | mock `isNewUser=true` | 提交验证码 | `AuthState.Authenticated` | 单元 |
| A-05 | 冷启动恢复成功 | 本地有有效 session | restore | `me` 成功后进入 authenticated | 单元 |
| A-06 | 冷启动 refresh 成功 | access token 过期 | restore | refresh 成功后恢复 authenticated | 单元 |
| A-07 | refresh 失败回匿名 | refresh token 失效 | restore | clear session + anonymous | 单元 |
| A-08 | single-flight refresh | 两个请求并发 401 | 触发网络层恢复 | refresh API 仅调用一次 | Core |
| A-09 | logout 网络失败 | logout 抛异常 | 点击退出登录 | 本地仍清 session | 单元 |
| A-10 | 排行拦截登录回跳 | 匿名用户预约 | 完成登录 | 导航回原 ranking route | UI / Navigation |

### 12.3 Mock / Fake 策略

| 依赖 | Mock 策略 | 说明 |
|------|----------|------|
| `ApiService` | Fake / mock 返回 `ApiEnvelope` | 避免真实网络 |
| `AuthSessionStore` | In-memory fake | 测试 session 读写与回收 |
| `AuthSessionProvider` | Fake provider | 测试 interceptor / authenticator |
| 时间 | 注入 `Clock` / `TimeProvider` | 便于测试倒计时与 token 过期 |
| 导航 | `TestNavHostController` | 测试登录成功回跳 |

---

## 13. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| `androidx.security:security-crypto`（待确认） | 待实现时确定 | 为 `EncryptedSharedPreferences` 提供安全存储底座 | 满足 spec/design 对 access token、refresh token 安全存储的要求；接入前必须先征得用户同意 |

> ⚠️ Android 方案已收敛为 Jetpack Security `EncryptedSharedPreferences` + DataStore 分层存储；由于这属于新增依赖，进入 coding 前必须先征得用户同意。

---

## 14. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| `AuthSessionProvider` 现状过薄，仅有 `isLoggedIn()` | 网络层与业务层接入 | 🔴 | 高 | 扩展为完整 session provider，并在 DI 中统一替换 | 过渡期保留 `isLoggedIn()` 默认实现，内部委托新 provider |
| `AuthInterceptor` 当前是 TODO skeleton | 所有受保护请求 | 🔴 | 高 | 本期接入 bearer 注入 + authenticator | 若实现受阻，优先先接 Profile/Login 主链路，再回补其它受保护接口 |
| `PROFILE` graph 当前为 placeholder | 我的频道落地 | 🔴 | 高 | 直接以真实 `ProfileScreen` 替换 placeholder | 至少先落 CTA + login screen，登录后摘要次级补完 |
| Android 安全存储实现与当前仓库基线不同 | token 存储 | 🔴 | 中 | 设计阶段即收敛为 `EncryptedSharedPreferences` + `AuthSessionStore` 抽象；coding 前向用户确认新增 Jetpack Security 依赖 | 若用户拒绝新增依赖，则需回到 design 阶段重新约束 Android 方案，不能直接以明文 DataStore 继续推进 |
| `returnRoute` 作为字符串传递可能损坏 | 登录回跳 | 🟡 | 中 | 使用 `Uri.encode` + 安全默认页兜底 | 无法解析时回 `PROFILE` |
| Compose 导航回跳与登录弹层时序复杂 | UI 稳定性 | 🟡 | 中 | success 通过单次 effect 驱动，避免在 recomposition 中重复 navigate | 以 popBackStack + safe default route 兜底 |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/decisions/2026-07-24-supabase-baas.md` | Auth 基础设施 | Supabase Auth 是统一认证底座 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `android/CLAUDE.md` | Kotlin + Compose + Hilt + AppConfig 约束 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 当前已有 profile/ranking route，需要扩展 login/settings |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | ranking 拦截和 profile placeholder 的接入点都已存在 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt` | 当前仍为 TODO skeleton |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthSessionProvider.kt` | 当前只有 `isLoggedIn()`，必须升级 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt` | 当前注入的是假登录 provider |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | 已有 `RequireLogin(returnRoute)` effect |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt` | UI 层已暴露 `onRequireLogin` 回调 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | Retrofit 接口组织方式基线 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiResult.kt` | 错误模型基线 |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/PlaybackSessionStore.kt` | DataStore 存储模式可复用 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | repository 注入风格基线 |
