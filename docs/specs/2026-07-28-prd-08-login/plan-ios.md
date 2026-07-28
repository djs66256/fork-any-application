# 实现计划：iOS — PRD-08 用户登录与注册

> 创建日期：2026-07-28
> 对应技术方案：design-ios.md
> 对应需求：spec.md

## 概述

本期 iOS 端将在现有 `NavigationStack + NavigationRouter + MVVM + Clean Architecture` 基础上，补齐手机号验证码登录、会话持久化与恢复、401 single-flight refresh、我的频道登录态展示、设置页退出登录，以及排行预约登录拦截回跳。计划遵循轻量 TDD：先为每个场景补齐 Swift Testing 单元测试，再逐步实现 `APIError.business(statusCode:businessCode:message:)` 错误透传链路、`AuthStore / AuthRefreshCoordinator / KeychainAuthSessionStore` 核心能力，最后用仓库现有 XcodeGen 与 xcodebuild 命令完成 iOS 全量验证。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | `APIClient` 将认证错误体解析为业务错误并透传 | 认证接口返回 400/401/409/410，body 含 `error.code` 与 `error.message` | 抛出 `APIError.business(statusCode:businessCode:message:)`，保留 HTTP status、业务码与 message | 单元测试 | P0 |
| T-02 | 认证数据层正确发起 send-otp / create-session / me / refresh / logout 请求 | 调用 `AuthRemoteDataSource` 与 `AuthRepository` | path、method、header、body、DTO→Entity 映射符合设计稿 | 单元测试 | P0 |
| T-03 | 登录页协议门禁与验证码提交流程正确 | 合法手机号、6 位验证码、协议未勾选/已勾选、接口成功/失败 | 未勾选协议不可提交；成功后 `AuthStore` 进入 authenticated；错误按业务码映射到字段或全局错误 | 单元测试 | P0 |
| T-04 | `KeychainAuthSessionStore` 与 `AuthStore` 能完成冷启动恢复 | 本地存在有效 session、过期 session、损坏 session | 有效 session 走 `me` 恢复；过期或 401 走 refresh；损坏数据直接清理并回匿名态 | 单元测试 | P0 |
| T-05 | `AuthRefreshCoordinator` 对 401 执行 single-flight refresh 与单次重试 | 多个受保护请求同时 401，refresh 成功或失败 | 同一时刻只发一次 refresh；成功后最多重放一次原请求；失败后清 session 并回匿名态 | 单元测试 | P0 |
| T-06 | “我的”频道、设置页与退出登录状态切换正确 | 匿名态/登录态、点击登录入口、点击设置、点击退出登录 | profile 匿名 CTA 与登录后摘要切换正确；logout 即使接口失败也清本地状态 | 单元测试 | P0 |
| T-07 | 排行预约登录拦截能拉起登录并在成功后恢复来源上下文 | 匿名用户在 booking 榜点击预约，登录成功或取消 | `NavigationRouter` 记录 `LoginInterceptionContext`，成功后回原 ranking context，取消则返回来源页 | 单元测试 | P0 |

## 实现步骤

### Step 1：先锁定认证错误契约与 APIError 透传链路

- **关联测试**：T-01
- **目标文件**：`ios/ShortDrama/Sources/Core/Network/APIError.swift`、`ios/ShortDrama/Sources/Core/Network/APIClient.swift`、`ios/ShortDrama/Tests/DataTests/APIErrorTests.swift`、`ios/ShortDrama/Tests/DataTests/APIClientTests.swift`
- **实现内容**：
  1. 先在 `APIErrorTests`、`APIClientTests` 中补齐认证错误体用例，覆盖 `AUTH_INVALID_CODE`、`AUTH_CODE_EXPIRED`、`AUTH_CODE_COOLDOWN`、`AUTH_UNAUTHORIZED`、`AUTH_REFRESH_EXPIRED` 等状态，锁定 `error.code/error.message` 解析行为。
  2. 扩展 `APIError`，新增 `business(statusCode:businessCode:message:)`，并保证 `LocalizedError`、`Equatable` 语义可支持测试断言。
  3. 修改 `APIClient` 的错误解析逻辑，让现有 `server(code:message:)` 之外的认证业务错误统一走 `APIError.business(...)`，形成“HTTP status + businessCode + message”完整透传链路。
  4. 明确后续 `AuthRemoteDataSource`、`AuthRepository`、`AuthStore`、`LoginViewModel` 都只消费该统一错误模型，不在上层重复解析原始响应 body。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-01 对应用例通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Core/Network/APIError.swift` | 修改 | 新增 `business(statusCode:businessCode:message:)` 及等价判断 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 修改 | 解析认证错误体并透传业务错误码 |
| `ios/ShortDrama/Tests/DataTests/APIErrorTests.swift` | 修改 | 覆盖 business error 的相等性与文案行为 |
| `ios/ShortDrama/Tests/DataTests/APIClientTests.swift` | 修改 | 覆盖认证错误体解码与抛错路径 |

### Step 2：先补认证数据层测试，再接通 Auth API 与会话模型

- **关联测试**：T-02
- **目标文件**：`ios/ShortDrama/Sources/Data/DataSources/AuthRemoteDataSource.swift`、`ios/ShortDrama/Sources/Data/DTOs/AuthDTOs.swift`、`ios/ShortDrama/Sources/Data/Repositories/AuthRepository.swift`、`ios/ShortDrama/Sources/Domain/Entities/AuthSession.swift`、`ios/ShortDrama/Sources/Domain/Entities/AuthUser.swift`、`ios/ShortDrama/Sources/Domain/Entities/AuthStatus.swift`、`ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift`、`ios/ShortDrama/Sources/Domain/RepositoryProtocols/AuthRepositoryProtocol.swift`、`ios/ShortDrama/Sources/Domain/UseCases/SendOtpUseCase.swift`、`ios/ShortDrama/Sources/Domain/UseCases/CreateSessionUseCase.swift`、`ios/ShortDrama/Sources/Domain/UseCases/RefreshSessionUseCase.swift`、`ios/ShortDrama/Sources/Domain/UseCases/GetCurrentUserUseCase.swift`、`ios/ShortDrama/Sources/Domain/UseCases/LogoutUseCase.swift`、`ios/ShortDrama/Tests/DataTests/AuthRemoteDataSourceTests.swift`、`ios/ShortDrama/Tests/DataTests/AuthRepositoryTests.swift`
- **实现内容**：
  1. 先新增 `AuthRemoteDataSourceTests`、`AuthRepositoryTests`，覆盖 `POST /api/auth/otp-requests`、`POST /api/auth/sessions`、`POST /api/auth/session-refreshes`、`GET /api/users/me`、`DELETE /api/auth/session` 的 method、path、请求体、Authorization header 与 envelope/DTO 映射。
  2. 在 Domain 层补齐 `AuthSession`、`AuthUser`、`AuthStatus`、`LoginInterceptionContext` 与 `AuthRepositoryProtocol`，确保 Presentation 只依赖 UseCase/Protocol，不直接依赖 DTO。
  3. 在 Data 层落地 `AuthDTOs`、`AuthRemoteDataSource`、`AuthRepository`，复用现有 `APIClient + URLSession` 风格，并把 `APIError.business(...)` 透传到仓储层，不丢失 `businessCode`。
  4. 新增 `SendOtpUseCase`、`CreateSessionUseCase`、`RefreshSessionUseCase`、`GetCurrentUserUseCase`、`LogoutUseCase`，为后续 `LoginViewModel`、`AuthStore`、`SettingsViewModel` 提供稳定输入。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-02 对应数据层测试通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Data/DataSources/AuthRemoteDataSource.swift` | 新增 | 认证接口远程数据源 |
| `ios/ShortDrama/Sources/Data/DTOs/AuthDTOs.swift` | 新增 | AuthSession/AuthUser/envelope DTO 定义 |
| `ios/ShortDrama/Sources/Data/Repositories/AuthRepository.swift` | 新增 | Auth DTO 到 Entity 映射与仓储实现 |
| `ios/ShortDrama/Sources/Domain/Entities/AuthSession.swift` | 新增 | 会话实体 |
| `ios/ShortDrama/Sources/Domain/Entities/AuthUser.swift` | 新增 | 当前用户实体 |
| `ios/ShortDrama/Sources/Domain/Entities/AuthStatus.swift` | 新增 | anonymous/restoring/authenticated/refreshing/expired 状态 |
| `ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift` | 新增 | 主动登录与业务拦截统一上下文 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/AuthRepositoryProtocol.swift` | 新增 | 认证仓储协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/SendOtpUseCase.swift` | 新增 | 发送验证码用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/CreateSessionUseCase.swift` | 新增 | 验证码创建会话用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/RefreshSessionUseCase.swift` | 新增 | 刷新会话用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/GetCurrentUserUseCase.swift` | 新增 | 获取当前用户用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/LogoutUseCase.swift` | 新增 | 退出登录用例 |
| `ios/ShortDrama/Tests/DataTests/AuthRemoteDataSourceTests.swift` | 新增 | 覆盖认证 endpoint 契约 |
| `ios/ShortDrama/Tests/DataTests/AuthRepositoryTests.swift` | 新增 | 覆盖 DTO → Entity 与错误透传 |

### Step 3：先写登录与恢复单测，再落地 AuthStore 与 KeychainAuthSessionStore

- **关联测试**：T-03、T-04
- **目标文件**：`ios/ShortDrama/Sources/Core/Storage/AuthSessionStore.swift`、`ios/ShortDrama/Sources/Core/Storage/KeychainAuthSessionStore.swift`、`ios/ShortDrama/Sources/Features/Auth/AuthStore.swift`、`ios/ShortDrama/Sources/Features/Auth/ViewModels/LoginViewModel.swift`、`ios/ShortDrama/Tests/DomainTests/KeychainAuthSessionStoreTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/LoginViewModelTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/AuthStoreTests.swift`、`ios/ShortDrama/Tests/Mocks/MockAuthRepository.swift`、`ios/ShortDrama/Tests/Mocks/MockAuthSessionStore.swift`
- **实现内容**：
  1. 先补 `KeychainAuthSessionStoreTests`、`LoginViewModelTests`、`AuthStoreTests`，覆盖协议勾选门禁、手机号/验证码校验、send-otp 冷却、create-session 成功、有效 session 恢复、过期 session refresh、损坏 session 清理等路径。
  2. 基于现有 `PlaybackSessionStore` 的 Keychain 模式新增 `AuthSessionStore` / `KeychainAuthSessionStore`，保证 `AuthSession` 可原子读写、清空，并在 decode 失败时执行损坏数据清理。
  3. 实现 `AuthStore`，统一承接冷启动恢复、登录成功保存 session、logout 清理、本地匿名/登录态切换，明确 `restoring -> authenticated/anonymous` 状态流转。
  4. 实现 `LoginViewModel`，让 send-otp / verify-code 只通过 UseCase 与 `AuthStore` 交互，并把 `APIError.business(...)` 映射成字段级错误、冷却状态或全局错误提示。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-03、T-04 对应用例通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Core/Storage/AuthSessionStore.swift` | 新增 | 会话安全存储协议 |
| `ios/ShortDrama/Sources/Core/Storage/KeychainAuthSessionStore.swift` | 新增 | Keychain 版 AuthSession 原子读写实现 |
| `ios/ShortDrama/Sources/Features/Auth/AuthStore.swift` | 新增 | 全局认证状态源与冷启动恢复 |
| `ios/ShortDrama/Sources/Features/Auth/ViewModels/LoginViewModel.swift` | 新增 | 登录表单、协议门禁、验证码冷却与成功登录处理 |
| `ios/ShortDrama/Tests/DomainTests/KeychainAuthSessionStoreTests.swift` | 新增 | 覆盖读写、清空、损坏数据清理 |
| `ios/ShortDrama/Tests/ViewModelTests/LoginViewModelTests.swift` | 新增 | 覆盖协议门禁、验证码流程与业务错误映射 |
| `ios/ShortDrama/Tests/ViewModelTests/AuthStoreTests.swift` | 新增 | 覆盖 restore/login success/logout 状态迁移 |
| `ios/ShortDrama/Tests/Mocks/MockAuthRepository.swift` | 新增 | 认证仓储测试替身 |
| `ios/ShortDrama/Tests/Mocks/MockAuthSessionStore.swift` | 新增 | 认证存储测试替身 |

### Step 4：先锁定 401 并发与导航测试，再接入 AuthRefreshCoordinator、Router 与 Profile/Settings UI

- **关联测试**：T-05、T-06、T-07
- **目标文件**：`ios/ShortDrama/Sources/Core/Network/AuthRefreshCoordinator.swift`、`ios/ShortDrama/Sources/Core/Network/APIClient.swift`、`ios/ShortDrama/Sources/App/AppRoute.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`、`ios/ShortDrama/Sources/App/ShortDramaApp.swift`、`ios/ShortDrama/Sources/Features/Profile/Views/ProfileHomeView.swift`、`ios/ShortDrama/Sources/Features/Profile/ViewModels/ProfileViewModel.swift`、`ios/ShortDrama/Sources/Features/Profile/Views/SettingsView.swift`、`ios/ShortDrama/Sources/Features/Profile/ViewModels/SettingsViewModel.swift`、`ios/ShortDrama/Sources/Features/Auth/Views/LoginView.swift`、`ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift`、`ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift`、`ios/ShortDrama/Tests/CoreTests/AuthRefreshCoordinatorTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/ProfileViewModelTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/SettingsViewModelTests.swift`
- **实现内容**：
  1. 先在 `AuthRefreshCoordinatorTests`、`NavigationRouterTests`、`ProfileViewModelTests`、`SettingsViewModelTests` 中锁定 three key flows：多请求 401 single-flight refresh、profile 匿名/登录态切换、logout 本地优先、ranking 登录拦截成功回跳与取消回退。
  2. 新增 `AuthRefreshCoordinator`，让 `APIClient` 的受保护请求能在 401 时统一进入 single-flight refresh，并在成功后最多重试一次原请求，失败则通知 `AuthStore` 清 session 并回 `anonymous/expired`。
  3. 扩展 `AppRoute`、`NavigationRouter`、`TabNavigationHostView`、`ShortDramaApp`，新增 login/settings/profile 承接点与 `LoginInterceptionContext` 保存/恢复链路，明确 login/profile/settings/ranking interception 导航与状态恢复策略。
  4. 落地 `ProfileHomeView`、`ProfileViewModel`、`SettingsView`、`SettingsViewModel`、`LoginView`，把“我的”频道 placeholder 替换为真实匿名 CTA / 登录后摘要 / 设置入口，同时把 `RankingViewModel.requireLogin(RankingLoginContext)` 统一映射到登录流并在成功后恢复 ranking context。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-05、T-06、T-07 对应用例通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Core/Network/AuthRefreshCoordinator.swift` | 新增 | 401 single-flight refresh 与单次重放协调器 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 修改 | 接入受保护请求 bearer 注入与 refresh 协调 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增 login/settings/profile 导航语义 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 管理登录弹层、returnRoute、ranking 回跳与取消返回 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | `.profile` 改为真实 ProfileHome 承接页并注册 settings/login 目标 |
| `ios/ShortDrama/Sources/App/ShortDramaApp.swift` | 修改 | 注入 `AuthStore` 并触发启动恢复逻辑 |
| `ios/ShortDrama/Sources/Features/Profile/Views/ProfileHomeView.swift` | 新增 | 我的频道匿名 CTA / 登录后摘要 |
| `ios/ShortDrama/Sources/Features/Profile/ViewModels/ProfileViewModel.swift` | 新增 | profile 页面状态映射 |
| `ios/ShortDrama/Sources/Features/Profile/Views/SettingsView.swift` | 新增 | 设置页与退出登录入口 |
| `ios/ShortDrama/Sources/Features/Profile/ViewModels/SettingsViewModel.swift` | 新增 | logout 执行与确认逻辑 |
| `ios/ShortDrama/Sources/Features/Auth/Views/LoginView.swift` | 新增 | 全屏登录页 UI |
| `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift` | 修改 | ranking login context 到统一拦截上下文映射 |
| `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` | 修改 | 与统一登录拦截 / 状态恢复链路对接 |
| `ios/ShortDrama/Tests/CoreTests/AuthRefreshCoordinatorTests.swift` | 新增 | 覆盖 single-flight refresh、失败清理与单次重试 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 覆盖 login/profile/settings/ranking 导航与回跳 |
| `ios/ShortDrama/Tests/ViewModelTests/ProfileViewModelTests.swift` | 新增 | 覆盖 profile 匿名/登录态切换 |
| `ios/ShortDrama/Tests/ViewModelTests/SettingsViewModelTests.swift` | 新增 | 覆盖 logout 本地优先逻辑 |

### Step 5：执行工程生成与 iOS 全量回归，固化 coding 验收基线

- **关联测试**：T-01 ～ T-07
- **目标文件**：`docs/specs/2026-07-28-prd-08-login/plan-ios.md`
- **实现内容**：
  1. 在 coding 阶段严格按本计划顺序推进：所有新增业务逻辑都先补 Swift Testing 单元测试，再补实现，不允许只靠手工点击验证登录主链路。
  2. 因为会新增 `Features/Auth`、`Features/Profile`、Domain/Data/Core/Tests 多个 Swift 文件，先运行 `xcodegen generate` 更新工程，再执行 test / build / lint 回归。
  3. 将收口标准固定为：`APIError.business(statusCode:businessCode:message:)` 透传链路可回归，`AuthStore / AuthRefreshCoordinator / KeychainAuthSessionStore` 主链路稳定，login/profile/settings/ranking interception 导航与状态恢复均有单元测试覆盖。
  4. 若本地环境已有 Swift Package manifest 或纯 Swift 测试入口，可补充 `swift test` 作为轻量验证；基于仓库真实配置，当前 iOS 主验证命令仍应以 `xcodebuild` 为准，`swift test` 不作为必选验收标准。
- **验证方式**：
  - 运行 `cd ios && xcodegen generate`
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - 运行 `cd ios && swiftlint lint`
  - 可选：若后续补齐 Swift Package 支持，再评估 `cd ios && swift test` 作为辅助验证；当前仓库无 `Package.swift`，不纳入本期必跑命令
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `docs/specs/2026-07-28-prd-08-login/plan-ios.md` | 新增 | 固化 PRD-08 iOS 登录实现步骤、测试矩阵与验收基线 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5
```

## 验证总览

- [ ] 所有测试通过（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [ ] Build 成功（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [ ] 无新增 lint 错误（`cd ios && swiftlint lint`）
- [ ] 新增源码后已执行 `cd ios && xcodegen generate`
- [ ] `APIError.business(statusCode:businessCode:message:)` 可解析并透传认证业务错误
- [ ] `AuthStore`、`AuthRefreshCoordinator`、`KeychainAuthSessionStore` 主链路均有单元测试覆盖
- [ ] login/profile/settings/ranking interception 导航与状态恢复均有单元测试覆盖
- [ ] send-otp、create-session、me、refresh、logout 五条认证 REST API 均有数据层契约测试
- [ ] 当前仓库无 `Package.swift`，不以 `swift test` 作为必跑命令；iOS 验证以 `xcodebuild` 命令为准

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Core/Network/APIError.swift` | 修改 | 新增 business error 模型 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 修改 | 认证错误透传、401 refresh 协调与请求重放 |
| `ios/ShortDrama/Sources/Core/Network/AuthRefreshCoordinator.swift` | 新增 | single-flight refresh 协调器 |
| `ios/ShortDrama/Sources/Core/Storage/AuthSessionStore.swift` | 新增 | 认证会话存储协议 |
| `ios/ShortDrama/Sources/Core/Storage/KeychainAuthSessionStore.swift` | 新增 | Keychain 版认证会话存储 |
| `ios/ShortDrama/Sources/Domain/Entities/AuthSession.swift` | 新增 | 会话实体 |
| `ios/ShortDrama/Sources/Domain/Entities/AuthUser.swift` | 新增 | 用户实体 |
| `ios/ShortDrama/Sources/Domain/Entities/AuthStatus.swift` | 新增 | 认证状态实体 |
| `ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift` | 新增 | 登录拦截上下文实体 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/AuthRepositoryProtocol.swift` | 新增 | 认证仓储协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/SendOtpUseCase.swift` | 新增 | 发送验证码用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/CreateSessionUseCase.swift` | 新增 | 创建会话用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/RefreshSessionUseCase.swift` | 新增 | 刷新会话用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/GetCurrentUserUseCase.swift` | 新增 | 获取当前用户用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/LogoutUseCase.swift` | 新增 | 退出登录用例 |
| `ios/ShortDrama/Sources/Data/DTOs/AuthDTOs.swift` | 新增 | 认证 DTO 与 envelope |
| `ios/ShortDrama/Sources/Data/DataSources/AuthRemoteDataSource.swift` | 新增 | 认证接口接线 |
| `ios/ShortDrama/Sources/Data/Repositories/AuthRepository.swift` | 新增 | Auth 数据仓储实现 |
| `ios/ShortDrama/Sources/Features/Auth/AuthStore.swift` | 新增 | 全局认证状态源 |
| `ios/ShortDrama/Sources/Features/Auth/ViewModels/LoginViewModel.swift` | 新增 | 登录表单状态机 |
| `ios/ShortDrama/Sources/Features/Auth/Views/LoginView.swift` | 新增 | 登录页 UI |
| `ios/ShortDrama/Sources/Features/Profile/ViewModels/ProfileViewModel.swift` | 新增 | 我的频道状态映射 |
| `ios/ShortDrama/Sources/Features/Profile/Views/ProfileHomeView.swift` | 新增 | 我的频道真实承接页 |
| `ios/ShortDrama/Sources/Features/Profile/ViewModels/SettingsViewModel.swift` | 新增 | 设置页退出登录逻辑 |
| `ios/ShortDrama/Sources/Features/Profile/Views/SettingsView.swift` | 新增 | 设置页 UI |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增 login/settings/profile 路由 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 登录弹层、returnRoute 与 ranking 回跳 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | profile 实页承接与 settings/login 目标注册 |
| `ios/ShortDrama/Sources/App/ShortDramaApp.swift` | 修改 | 注入认证状态并在启动时恢复 |
| `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift` | 修改 | ranking 拦截上下文映射 |
| `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` | 修改 | 与统一登录拦截流集成 |
| `ios/ShortDrama/Tests/DataTests/APIErrorTests.swift` | 修改 | business error 断言 |
| `ios/ShortDrama/Tests/DataTests/APIClientTests.swift` | 修改 | 认证错误与请求契约测试 |
| `ios/ShortDrama/Tests/DataTests/AuthRemoteDataSourceTests.swift` | 新增 | 认证 endpoint 测试 |
| `ios/ShortDrama/Tests/DataTests/AuthRepositoryTests.swift` | 新增 | 认证仓储映射测试 |
| `ios/ShortDrama/Tests/DomainTests/KeychainAuthSessionStoreTests.swift` | 新增 | Keychain 会话存储测试 |
| `ios/ShortDrama/Tests/ViewModelTests/LoginViewModelTests.swift` | 新增 | 登录流程与门禁测试 |
| `ios/ShortDrama/Tests/ViewModelTests/AuthStoreTests.swift` | 新增 | 启动恢复与登录态切换测试 |
| `ios/ShortDrama/Tests/CoreTests/AuthRefreshCoordinatorTests.swift` | 新增 | single-flight refresh 测试 |
| `ios/ShortDrama/Tests/ViewModelTests/ProfileViewModelTests.swift` | 新增 | profile 状态测试 |
| `ios/ShortDrama/Tests/ViewModelTests/SettingsViewModelTests.swift` | 新增 | logout 测试 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | login/profile/settings/ranking 导航与回跳测试 |
| `ios/ShortDrama/Tests/Mocks/MockAuthRepository.swift` | 新增 | 认证仓储 mock |
| `ios/ShortDrama/Tests/Mocks/MockAuthSessionStore.swift` | 新增 | 会话存储 mock |
