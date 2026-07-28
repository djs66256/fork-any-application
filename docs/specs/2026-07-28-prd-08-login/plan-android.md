# 实现计划：Android — PRD-08 用户登录与注册

> 创建日期：2026-07-28
> 对应技术方案：design-android.md
> 对应需求：spec.md

## 概述

Android 端将在现有 `Compose + Navigation Compose + Hilt + Retrofit/OkHttp + DataStore` 架构上，以轻量 TDD 方式补齐登录页、我的频道、设置页、会话恢复、401 refresh 与排行预约登录拦截闭环。实施顺序按“先锁定认证领域模型与分层持久化，再接通网络与 refresh 基础设施，随后完成登录/我的/设置 UI，最后做导航与拦截收口”推进。

- 前置条件：进入 coding 前，必须先征得用户同意在 `android/app/build.gradle.kts` 新增 Jetpack Security 依赖，用于 `EncryptedPrefsAuthSessionStore` 落地安全存储；若用户不同意，则停止 Android coding，并回到 design 阶段重新约束安全存储方案。
- 分层要求：敏感 `AuthSession` 由 `EncryptedPrefsAuthSessionStore` 持久化，非敏感验证码倒计时由 `AuthCooldownStore` 持久化，避免把 token 与 cooldown 混放在同一存储层。
- 认证主干要求：实现并接通 `AuthSessionProvider`、`AuthStateHolder`、`AuthAuthenticator`、`AuthRefreshCoordinator`，统一承接冷启动恢复、401 single-flight refresh、logout 清理与 UI 登录态同步。
- 验证命令基于仓库当前 Android Gradle 配置推断，统一从仓库根目录执行 `cd android && ./gradlew ...`。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> Android 端每个场景都补齐 `src/test/` 单元测试；新增业务逻辑同步补齐测试。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | `EncryptedPrefsAuthSessionStore` 与 `AuthCooldownStore` 分层持久化正确 | 写入一份 `AuthSession`、写入 cooldown 截止时间、再模拟损坏 payload | session 可安全读回；cooldown 可单独恢复；损坏 payload 会被清理并回落为 `null` | 单元测试 | P0 |
| T-02 | `AuthStateHolder` 冷启动恢复遵循 `me -> refresh -> clear` contract | 本地存在有效 session；或 `me=401` 且 `refresh=success/fail` | 状态按 `RESTORING -> AUTHENTICATED` 或 `RESTORING -> REFRESHING -> EXPIRED -> ANONYMOUS` 迁移 | 单元测试 | P0 |
| T-03 | `AuthRefreshCoordinator` / `AuthAuthenticator` 只发起一次 refresh 并最多重试一次原请求 | 两个并发受保护请求同时返回 401 | refresh API 仅调用一次；原请求共享同一 refresh 结果；单请求最多重放一次 | 单元测试 | P0 |
| T-04 | 登录页协议门禁与字段校验正确 | 未勾选协议、非法手机号、验证码不足 6 位 | 发送验证码/确认登录按钮不可提交；表单给出字段错误；不会发起网络请求 | 单元测试 | P0 |
| T-05 | 发送验证码成功后保存并恢复 cooldown | 合法手机号 + 已勾选协议 + OTP 接口返回 `cooldownSeconds=60` | `AuthCooldownStore` 写入截止时间；`LoginViewModel` 重建后倒计时继续，不重置为初始态 | 单元测试 | P0 |
| T-06 | 验证码登录成功后原子写入 session 并发出回跳 effect | 合法手机号/验证码，`createSession` 返回 `AuthSession` | `EncryptedPrefsAuthSessionStore` 覆盖保存整份 session；`AuthStateHolder` 变为 `AUTHENTICATED`；发出登录成功 effect | 单元测试 | P0 |
| T-07 | 我的频道能正确映射匿名态/恢复中/登录态 | `AuthStateHolder` 分别发出 `ANONYMOUS`、`RESTORING`、`AUTHENTICATED` | `ProfileViewModel` 输出 CTA、loading skeleton、手机号摘要与设置入口三种状态 | 单元测试 | P1 |
| T-08 | 设置页退出登录遵循本地优先 | logout API 成功或失败 | 本地 session 始终被清空；`AuthStateHolder` 回到 `ANONYMOUS`；页面可返回匿名态 Profile | 单元测试 | P0 |
| T-09 | 排行预约匿名点击继续复用登录拦截 | 排行页 booking 榜匿名点击预约 | `RankingViewModel` 发出 `RequireLogin(returnRoute)`；导航层将其路由到 login 并保留 ranking returnRoute | 单元测试 | P0 |
| T-10 | login/profile/settings 路由与回跳规则正确 | 主动从 Profile 进入登录；或从 ranking 拦截进入登录；或 `returnRoute` 非法 | 成功后优先回 `returnRoute`；非法时回 `profile` 安全默认页；取消登录返回来源页 | 单元测试 | P0 |
| T-11 | 认证 API / DTO / Repository 映射契约正确 | 调用 `ApiService` auth 接口与 DTO 转换 | REST path/query/body 命名与 design 一致；DTO 能正确映射到 `AuthSession` / `AuthUser` | 单元测试 | P1 |

## 实现步骤

### Step 1：确认 Jetpack Security 前置条件，并先落认证模型与分层持久化

- **关联测试**：T-01
- **目标文件**：`android/app/build.gradle.kts`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/AuthSession.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/AuthUser.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/AuthStatus.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/LoginInterceptionContext.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthSessionProvider.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthStateHolder.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/storage/AuthSessionStore.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/storage/EncryptedPrefsAuthSessionStore.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/storage/AuthSessionSerializer.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/storage/AuthCooldownStore.kt`
- **实现内容**：
  1. coding 开始前先向用户确认是否允许新增 Jetpack Security 依赖；若未获同意，本步骤停止，不进入后续实现。
  2. 新增 `AuthSession`、`AuthUser`、`AuthStatus`、`LoginInterceptionContext` 等 domain model，并把现有 `AuthSessionProvider` 从单一 `isLoggedIn()` 扩展为可读取 `currentSession/accessToken/refreshToken/currentUser` 的 provider contract。
  3. 新增应用级 `AuthStateHolder`，作为 UI 单一登录态来源，后续由 Profile、Login、Settings 与网络层共同消费。
  4. 落地 `EncryptedPrefsAuthSessionStore` 保存整份 `AuthSession`，并通过 `AuthSessionSerializer` 保证登录成功/refresh 成功时原子覆盖整份 session。
  5. 另起 `AuthCooldownStore` 管理验证码倒计时截止时间，明确与安全 session store 分层，避免把敏感 token 放进 DataStore。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.core.storage.EncryptedPrefsAuthSessionStoreTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.core.storage.AuthCooldownStoreTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.core.auth.AuthStateHolderTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/build.gradle.kts` | 修改 | 在用户同意后新增 Jetpack Security 依赖 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/AuthSession.kt` | 新增 | 定义 access/refresh/user/expiresAt 认证会话实体 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/AuthUser.kt` | 新增 | 定义当前用户摘要模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/AuthStatus.kt` | 新增 | 定义匿名、恢复中、已登录、刷新中、过期状态 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/LoginInterceptionContext.kt` | 新增 | 定义主动登录与 ranking 拦截来源上下文 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthSessionProvider.kt` | 修改 | 从布尔接口升级为完整 session provider contract |
| `android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthStateHolder.kt` | 新增 | 暴露应用级认证状态 `StateFlow` |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/AuthSessionStore.kt` | 新增 | 定义认证会话读写接口 |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/EncryptedPrefsAuthSessionStore.kt` | 新增 | 用 `EncryptedSharedPreferences` 保存整份 `AuthSession` |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/AuthSessionSerializer.kt` | 新增 | 负责 session JSON 编解码与损坏数据兜底 |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/AuthCooldownStore.kt` | 新增 | 用 DataStore 保存验证码 cooldown 截止时间 |
| `android/app/src/test/java/com/djs66256/short_drama/core/storage/EncryptedPrefsAuthSessionStoreTest.kt` | 新增 | 覆盖安全读写、覆盖写入、损坏 payload 清理 |
| `android/app/src/test/java/com/djs66256/short_drama/core/storage/AuthCooldownStoreTest.kt` | 新增 | 覆盖 cooldown 读写与恢复 |
| `android/app/src/test/java/com/djs66256/short_drama/core/auth/AuthStateHolderTest.kt` | 新增 | 覆盖基础状态迁移与 provider 协作 |

### Step 2：接通 Auth API、Repository 与 refresh 基础设施

- **关联测试**：T-02、T-03、T-11
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/network/AuthAuthenticator.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/network/AuthRefreshCoordinator.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiClient.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/dto/AuthDtos.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/datasource/AuthRemoteDataSource.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/repository/AuthRepositoryImpl.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthRepository.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/usecase/SendOtpUseCase.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/usecase/CreateSessionUseCase.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/usecase/RefreshSessionUseCase.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetCurrentUserUseCase.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/usecase/LogoutUseCase.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/di/NetworkModule.kt`
- **实现内容**：
  1. 在 `ApiService.kt` 新增 `otp-requests`、`sessions`、`session-refreshes`、`users/me`、`auth/session` 五个认证接口，并为 `ApiServiceTest` 补齐 path/query/body 契约断言。
  2. 新增 `AuthDtos`、`AuthRemoteDataSource`、`AuthRepositoryImpl` 与相关 use case，统一把后端 envelope 映射为 Android domain model，不把 DTO 细节泄漏到 ViewModel。
  3. 实现 `AuthInterceptor`，只给受保护请求注入 `Authorization: Bearer <accessToken>`，不污染 OTP/login/refresh 等匿名接口。
  4. 实现 `AuthRefreshCoordinator` + `AuthAuthenticator`，把 401 恢复收口为 single-flight refresh，并约束“单请求最多重试一次”。
  5. 更新 `AppModule`、`RepositoryModule`、`NetworkModule`、`ApiClient`，把 `AuthSessionProvider`、`AuthStateHolder`、`AuthAuthenticator`、`AuthRefreshCoordinator`、`AuthRepository` 接入真实依赖，而不是继续使用占位假实现。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.core.network.ApiServiceTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.data.repository.AuthRepositoryImplTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.core.network.AuthRefreshCoordinatorTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.core.network.AuthAuthenticatorTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.core.network.AuthInterceptorTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增 5 个认证接口定义 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt` | 修改 | 注入 access token，区分匿名/受保护请求 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthAuthenticator.kt` | 新增 | 处理 401 refresh 与原请求单次重试 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthRefreshCoordinator.kt` | 新增 | 统一实现 single-flight refresh |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiClient.kt` | 修改 | 接入 authenticator/interceptor 所需网络构造调整 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/AuthDtos.kt` | 新增 | 定义认证 DTO 与 envelope 映射对象 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/AuthRemoteDataSource.kt` | 新增 | 封装认证接口调用与错误解析 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/AuthRepositoryImpl.kt` | 新增 | 实现 DTO 到 domain 的认证仓储 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthRepository.kt` | 新增 | 定义 sendOtp/createSession/refresh/me/logout 仓储接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/SendOtpUseCase.kt` | 新增 | 发送验证码用例 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/CreateSessionUseCase.kt` | 新增 | 验证码创建 session 用例 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/RefreshSessionUseCase.kt` | 新增 | refresh 会话用例 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetCurrentUserUseCase.kt` | 新增 | 当前用户查询用例 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/LogoutUseCase.kt` | 新增 | 退出登录用例 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt` | 修改 | 提供真实 auth provider / holder / store 依赖 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 绑定 `AuthRepository` 与相关实现 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/NetworkModule.kt` | 修改 | 注入 auth 网络组件 |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/ApiServiceTest.kt` | 修改 | 补 auth API 路径与参数断言 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/AuthRepositoryImplTest.kt` | 新增 | 覆盖 DTO 映射与错误处理 |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/AuthRefreshCoordinatorTest.kt` | 新增 | 覆盖 single-flight refresh |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/AuthAuthenticatorTest.kt` | 新增 | 覆盖 401 单次重试与失败清理 |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/AuthInterceptorTest.kt` | 新增 | 覆盖鉴权 header 注入规则 |

### Step 3：先写登录页状态机测试，再实现 Login ViewModel 与页面

- **关联测试**：T-04、T-05、T-06
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/auth/ui/LoginScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/auth/model/LoginUiState.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModelTest.kt`
- **实现内容**：
  1. 先补 `LoginViewModelTest`，固定协议勾选、手机号/验证码字段校验、发送验证码成功进入 cooldown、登录成功保存 session 并发出 success effect 等关键行为。
  2. 在 `LoginViewModel` 中接入 `SendOtpUseCase`、`CreateSessionUseCase`、`AuthCooldownStore`、`AuthStateHolder`，统一管理输入态、按钮禁用态、倒计时与错误提示。
  3. 登录成功时只通过单次 effect 驱动导航，由 ViewModel 负责触发 `AuthSessionStore.write(session)` 与 `AuthStateHolder` 状态更新，避免 UI 层直接操作存储。
  4. `LoginScreen` 仅负责渲染手机号输入、验证码输入、协议勾选、发送验证码按钮、登录按钮与错误提示，不在 Composable 内拼认证逻辑。
  5. 对 `AUTH_INVALID_PHONE`、`AUTH_INVALID_CODE`、`AUTH_CODE_EXPIRED`、`AUTH_RATE_LIMITED`、网络异常等错误做 UI 级映射，保证用户提示可理解且不泄漏底层错误。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.auth.viewmodel.LoginViewModelTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/auth/model/LoginUiState.kt` | 新增 | 定义登录页输入、loading、error、success 状态 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt` | 新增 | 实现表单校验、发送验证码、登录提交、cooldown 恢复 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/auth/ui/LoginScreen.kt` | 新增 | 实现全屏 Native 登录页 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModelTest.kt` | 新增 | 覆盖协议门禁、字段校验、cooldown 与登录成功 effect |

### Step 4：实现 Profile/Settings 页面，并接通恢复与退出登录闭环

- **关联测试**：T-02、T-07、T-08
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/ProfileScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/profile/viewmodel/ProfileViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/SettingsScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/profile/viewmodel/SettingsViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/profile/viewmodel/ProfileViewModelTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/profile/viewmodel/SettingsViewModelTest.kt`
- **实现内容**：
  1. 先补 `ProfileViewModelTest` 与 `SettingsViewModelTest`，固定匿名 CTA、恢复中 skeleton、登录后手机号摘要、退出登录本地优先等状态流转。
  2. 在 `ProfileViewModel` 中订阅 `AuthStateHolder`，把匿名态、恢复中、登录态统一映射为 Profile UI 状态，不再在页面层维护额外“是否登录”布尔值。
  3. 新增 `SettingsViewModel` + `SettingsScreen`，接入 `LogoutUseCase`，无论 API 成功与否都清空本地 session，并把 `AuthStateHolder` 切回 `ANONYMOUS`。
  4. 在 `MainActivity.onStart()` 调用 `AuthBootstrapper.restoreIfNeeded()`，让冷启动进入前台与后台回前台都遵循 `me -> refresh -> clear` 规则，避免先渲染假登录态再闪退匿名。
  5. 让 Profile 页面成为真正的“我的”频道承接页，替换当前 placeholder，同时为设置页留出导航入口。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.core.auth.AuthStateHolderTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.profile.viewmodel.ProfileViewModelTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.profile.viewmodel.SettingsViewModelTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/ProfileScreen.kt` | 新增 | 渲染匿名 CTA、恢复态与登录后摘要 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/profile/viewmodel/ProfileViewModel.kt` | 新增 | 订阅 `AuthStateHolder` 并映射 Profile UI 状态 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/SettingsScreen.kt` | 新增 | 提供设置页与退出登录入口 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/profile/viewmodel/SettingsViewModel.kt` | 新增 | 实现 logout 本地优先逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt` | 修改 | 接入认证恢复触发时机 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/profile/viewmodel/ProfileViewModelTest.kt` | 新增 | 覆盖匿名/恢复/登录态映射 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/profile/viewmodel/SettingsViewModelTest.kt` | 新增 | 覆盖退出登录成功与失败都清本地 |

### Step 5：收口 login/profile/settings/ranking interception 导航接入

- **关联测试**：T-09、T-10
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModelTest.kt`
- **实现内容**：
  1. 在 `AppDestination.kt` 新增 `login`、`settings` route 及 `login(returnRoute, source)` builder，保证主动登录与 ranking 拦截复用同一套路由表达。
  2. 在 `NavGraph.kt` 中把 Profile placeholder 替换为真实 `ProfileScreen`，补 login/settings 目的地，并把 ranking 现有 `onRequireLogin` 接到 `AppDestination.login(returnRoute, source = "ranking_booking")`。
  3. 登录成功后根据 `returnRoute` 回跳；`returnRoute` 缺失、损坏或非法时回 `profile` 安全默认页；取消登录则返回来源页且不执行预约。
  4. 视需要在 `MainNavigationViewModel` 中补齐登录相关 pending route / 回跳协作，但不把登录业务逻辑下沉到排名 ViewModel。
  5. 回归现有 `RankingViewModel` 的 `RequireLogin(returnRoute)` 行为，确保 PRD-05 已有预约拦截语义被本期导航层真正消费，而不是改写 ranking 领域逻辑。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.navigation.RoutesTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.navigation.NavGraphTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.navigation.MainNavigationViewModelTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.ranking.viewmodel.RankingViewModelTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 新增 login/settings route 与 returnRoute builder |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 接入 Profile、Login、Settings 页面与 ranking 登录拦截 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt` | 修改 | 按需补齐登录回跳的导航状态协作 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | 修改/回归 | 保持 `RequireLogin(returnRoute)` contract 并对接真实导航流 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 补 login/settings/ranking 回跳路由断言 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt` | 修改 | 回归 login/profile/settings/ranking 导航接线 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt` | 修改 | 覆盖登录回跳相关 pending route 行为 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModelTest.kt` | 修改 | 保持匿名预约触发 `RequireLogin(returnRoute)` 单测 |

## 依赖关系

```text
前置条件：用户同意新增 Jetpack Security 依赖

Step 1（认证模型 + 分层持久化）
  └──▶ Step 2（Auth API + Repository + refresh 基础设施）
        └──▶ Step 3（Login ViewModel + LoginScreen）
              └──▶ Step 4（Profile/Settings + restore/logout）
                    └──▶ Step 5（login/profile/settings/ranking interception 导航接入）
```

## 当前执行状态（2026-07-28）

- 已完成 Step 1 ~ Step 5 的代码收口，Android 登录、恢复、refresh、Profile/Settings、ranking 登录拦截主链路均已接通。
- review-coding 本轮已额外修正两项问题：
  - 认证恢复与 refresh 失败语义重新统一为 `Expired -> clearSession() -> Anonymous`；
  - 认证恢复入口调整为 `MainActivity.onStart()`，覆盖冷启动与前台恢复。
- `AuthBootstrapperTest` 与 `AuthRefreshCoordinatorTest` 已同步修正为“refresh 失败清 session”的预期。
- 当前环境缺少 Java Runtime，`./android/gradlew -version` 返回 `Unable to locate a Java Runtime.`，因此以下 Gradle 验证项尚未实际执行，需保持未勾选。

## 验证总览

- [ ] 存储与状态单测通过（`cd android && ./gradlew test --tests "com.djs66256.short_drama.core.storage.EncryptedPrefsAuthSessionStoreTest"`）
- [ ] refresh/网络层单测通过（`cd android && ./gradlew test --tests "com.djs66256.short_drama.core.network.AuthRefreshCoordinatorTest"`）
- [ ] 登录页单测通过（`cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.auth.viewmodel.LoginViewModelTest"`）
- [ ] Profile/Settings 单测通过（`cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.profile.viewmodel.ProfileViewModelTest"`）
- [ ] 导航与 ranking 拦截回归通过（`cd android && ./gradlew test --tests "com.djs66256.short_drama.navigation.NavGraphTest"`）
- [ ] 所有测试通过（`cd android && ./gradlew test`）
- [ ] Build 成功（`cd android && ./gradlew assembleDebug`）
- [ ] 无新增 lint 错误（`cd android && ./gradlew detekt`）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/build.gradle.kts` | 修改 | 在用户同意后新增 Jetpack Security 依赖 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/AuthSession.kt` | 新增 | 认证会话实体 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/AuthUser.kt` | 新增 | 用户摘要实体 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/AuthStatus.kt` | 新增 | 认证状态枚举 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/LoginInterceptionContext.kt` | 新增 | 登录拦截来源上下文 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthSessionProvider.kt` | 修改 | 扩展为完整 session provider |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthRepository.kt` | 新增 | 认证仓储接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/SendOtpUseCase.kt` | 新增 | 发送验证码用例 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/CreateSessionUseCase.kt` | 新增 | 登录建会话用例 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/RefreshSessionUseCase.kt` | 新增 | refresh 用例 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetCurrentUserUseCase.kt` | 新增 | 查询当前用户用例 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/LogoutUseCase.kt` | 新增 | 退出登录用例 |
| `android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthStateHolder.kt` | 新增 | 应用级认证状态 holder |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/AuthSessionStore.kt` | 新增 | 认证会话存储接口 |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/EncryptedPrefsAuthSessionStore.kt` | 新增 | 安全存储 `AuthSession` |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/AuthSessionSerializer.kt` | 新增 | session 编解码 |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/AuthCooldownStore.kt` | 新增 | cooldown DataStore 持久化 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增 auth REST 接口 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt` | 修改 | Bearer token 注入 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthAuthenticator.kt` | 新增 | 401 refresh 与重放 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthRefreshCoordinator.kt` | 新增 | single-flight refresh 协调 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiClient.kt` | 修改 | 接入认证网络能力 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt` | 修改 | 注入 auth store/provider/holder |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 绑定 auth repository |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/NetworkModule.kt` | 修改 | 接入 authenticator/interceptor 依赖 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/AuthDtos.kt` | 新增 | 认证 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/AuthRemoteDataSource.kt` | 新增 | 认证远端数据源 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/AuthRepositoryImpl.kt` | 新增 | 认证仓储实现 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/auth/model/LoginUiState.kt` | 新增 | 登录页状态模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt` | 新增 | 登录页状态机 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/auth/ui/LoginScreen.kt` | 新增 | 登录页 Compose UI |
| `android/app/src/main/java/com/djs66256/short_drama/feature/profile/viewmodel/ProfileViewModel.kt` | 新增 | 我的频道状态机 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/ProfileScreen.kt` | 新增 | 我的频道 UI |
| `android/app/src/main/java/com/djs66256/short_drama/feature/profile/viewmodel/SettingsViewModel.kt` | 新增 | 设置页退出登录逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/SettingsScreen.kt` | 新增 | 设置页 UI |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 新增 login/settings 路由与回跳 builder |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 接通 login/profile/settings/ranking interception |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt` | 修改 | 登录回跳导航状态协作 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | 修改/回归 | 保持并接入 `RequireLogin(returnRoute)` |
| `android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt` | 修改 | 启动恢复 auth 状态 |
| `android/app/src/test/java/com/djs66256/short_drama/core/storage/EncryptedPrefsAuthSessionStoreTest.kt` | 新增 | 安全存储单测 |
| `android/app/src/test/java/com/djs66256/short_drama/core/storage/AuthCooldownStoreTest.kt` | 新增 | cooldown 单测 |
| `android/app/src/test/java/com/djs66256/short_drama/core/auth/AuthStateHolderTest.kt` | 新增 | 认证状态机单测 |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/AuthRefreshCoordinatorTest.kt` | 新增 | single-flight refresh 单测 |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/AuthAuthenticatorTest.kt` | 新增 | 401 重试单测 |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/AuthInterceptorTest.kt` | 新增 | header 注入单测 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/AuthRepositoryImplTest.kt` | 新增 | DTO/错误映射单测 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModelTest.kt` | 新增 | 登录页单测 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/profile/viewmodel/ProfileViewModelTest.kt` | 新增 | 我的频道单测 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/profile/viewmodel/SettingsViewModelTest.kt` | 新增 | 设置页单测 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | auth 路由断言 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt` | 修改 | 导航接线回归 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt` | 修改 | 回跳状态单测 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModelTest.kt` | 修改 | ranking 登录拦截回归 |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/ApiServiceTest.kt` | 修改 | auth API 契约回归 |
