# 认证体系 (Auth)

> 最后更新：2026-07-29
> 覆盖端：Android / iOS / Backend（Web 本期不实现）

## 功能概述

PRD-08 为移动端补齐了从匿名态到登录态的完整认证闭环：Android 与 iOS 都提供手机号验证码登录页；Backend 提供 OTP 请求、验证码创建会话、刷新会话、当前用户查询与登出接口；“我的”频道和排行预约拦截都基于同一套 `AuthSession` 与 bearer token contract 运作。当前不提供独立注册页，首次验证码校验成功会自动注册用户，并通过 `isNewUser` 向客户端暴露首登语义（`docs/specs/2026-07-28-prd-08-login/spec.md:421-429,488-500`）。

- **核心价值**：统一登录、自动注册、会话恢复、刷新、登出与受保护能力鉴权口径。
- **覆盖范围**：Backend 认证 API 与 middleware、Android / iOS 登录页、会话持久化、Profile 登录后态、排行预约登录拦截。
- **当前状态**：Android / iOS / Backend 已落地；Web 未接入真实用户端登录流程。

## 入口与路由

| 端 | 入口 | 路由 / 触发方式 | 源文件 |
|----|------|----------------|--------|
| Android | “我的”匿名态登录按钮 | `AppDestination.login(returnRoute = profile)` | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:324-333`, `android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/ProfileScreen.kt:28-79` |
| Android | 排行预约登录拦截 | `RankingEffect.RequireLogin(returnRoute)` → `login?returnRoute=...&source=ranking_booking` | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:185-205`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:206-219` |
| iOS | “我的”匿名态登录按钮 | `router.presentLogin(LoginInterceptionContext(source: .profileEntry))` | `ios/ShortDrama/Sources/Features/Profile/Views/ProfileHomeView.swift:29-48` |
| iOS | 排行预约登录拦截 | `RankingRouteBuilder.loginContext(for:)` → `LoginInterceptionContext(source: .rankingBooking, returnRoute: .rankingHome)` | `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift:73-78`, `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift:10-16` |
| iOS | 登录页承载方式 | `AppShellView.fullScreenCover(item: presentedLoginContext)` | `ios/ShortDrama/Sources/App/AppShellView.swift:17-34` |
| Backend | 认证 API | `/api/auth/otp-requests`、`/api/auth/sessions`、`/api/auth/session-refreshes`、`/api/users/me`、`/api/auth/session` | `backend/src/app/api/auth/otp-requests/route.ts:7-14`, `backend/src/app/api/auth/sessions/route.ts:7-14`, `backend/src/app/api/auth/session-refreshes/route.ts:7-14`, `backend/src/app/api/users/me/route.ts:7-13`, `backend/src/app/api/auth/session/route.ts:6-13` |

## 核心逻辑

### 流程：手机号验证码登录 / 自动注册

1. 用户从“我的”匿名态或排行预约拦截进入登录页。
   - Android 登录页是独立 Nav route，支持 `returnRoute` 与 `source` query 参数（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:349-371`）。
   - iOS 登录页由 `AppShellView` 通过 `fullScreenCover` 承载，不额外暴露公开路由（`ios/ShortDrama/Sources/App/AppShellView.swift:17-30`）。
2. 客户端输入手机号、验证码并勾选协议后，可先发起 OTP 请求。
   - Android `LoginUiState.canSendOtp` 会同时校验协议、手机号格式、冷却状态与发送 / 提交中状态（`android/app/src/main/java/com/djs66256/short_drama/feature/auth/model/LoginUiState.kt:3-29`）。
   - iOS `LoginViewModel.canSendOtp` 也会校验手机号、协议与 cooldown（`ios/ShortDrama/Sources/Features/Auth/ViewModels/LoginViewModel.swift:50-79`）。
3. Backend `POST /api/auth/otp-requests` 创建 OTP 请求；本地测试环境支持 test OTP bypass，短信 provider 未启用时统一映射为 `SERVICE_UNAVAILABLE`（`backend/src/app/api/auth/otp-requests/route.ts:7-14`, `backend/src/services/auth/auth.service.ts:210-248`）。
4. 用户提交验证码后，客户端调用 `POST /api/auth/sessions`。
   - Backend `AuthService.createSession()` 负责执行验证码校验、自动注册 / 登录和 session 构建；payload 统一映射为 camelCase（`backend/src/app/api/auth/sessions/route.ts:7-14`, `backend/src/app/api/auth/_helpers.ts:12-29`）。
5. 登录成功后，客户端保存完整 `AuthSession`，更新全局认证状态，并根据拦截上下文跳转目标页。
   - Android `LoginViewModel` 会发出 `LoginSucceeded(route)` 事件，默认回到 profile，排行预约场景保留原 `ranking?...` route（`android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt:166-238`）。
   - iOS `LoginViewModel.submit()` 成功后通过 `onLoginSuccess` 回调保存 session，再由 `onSuccess` 调用 `router.completeLogin()` 完成回跳（`ios/ShortDrama/Sources/Features/Auth/ViewModels/LoginViewModel.swift:108-139`, `ios/ShortDrama/Sources/App/AppShellView.swift:18-29`）。

### 流程：启动恢复、`me` 校验与 refresh

1. 客户端启动时先从安全存储读取本地 session。
   - Android `AuthStateHolder.restoreIfNeeded()` 先把本地 session 恢复为 `Authenticated` 快照（`android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthStateHolder.kt:26-39`）。
   - iOS `AuthStore.restoreIfNeeded()` 先从 `AuthSessionStore` 读取 session，再进入 `.restoring`（`ios/ShortDrama/Sources/Features/Auth/AuthStore.swift:36-48`）。
2. 若存在 session，则优先调用 `GET /api/users/me` 校验 access token 当前有效性。
   - Android `AuthBootstrapper.restoreIfNeeded()` 调用 `GetCurrentUserUseCase`；401 时再尝试 refresh（`android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthBootstrapper.kt:15-41`）。
   - iOS `AuthStore.restoreIfNeeded()` 同样先 `getCurrentUserUseCase.execute(accessToken:)`；仅在 401 / access token 失效语义下触发 refresh（`ios/ShortDrama/Sources/Features/Auth/AuthStore.swift:49-63,95-99`）。
3. refresh 成功后用新的 `AuthSession` 原子替换本地存储；refresh 失败则清空本地 session，回到匿名 / 过期态。
   - Android refresh 失败后 `markExpired()` 后立即 `clearSession()`，最终回到匿名态（`android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthBootstrapper.kt:30-35`）。
   - iOS refresh 失败后会清空 session，并将状态置为 `.expired`（`ios/ShortDrama/Sources/Features/Auth/AuthStore.swift:80-92`）。
4. Backend 的 `verifyJwt()` 统一负责解析 bearer token：本地测试 token 走 local auth session，正式 token 走 Supabase `getUser(token)`；受保护接口通过 `requireAuthContext()` 强制要求登录（`backend/src/middleware/auth.ts:27-103`）。

### 流程：登出与本地清理

1. 用户从“设置”页点击“退出登录”。
   - Android `SettingsScreen` 先弹确认框，再调用 `SettingsViewModel.logout()`（`android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/SettingsScreen.kt:95-116`）。
   - iOS `SettingsView` 使用 `confirmationDialog` 二次确认，再执行异步 logout（`ios/ShortDrama/Sources/Features/Profile/Views/SettingsView.swift:26-41`）。
2. 客户端调用 `DELETE /api/auth/session`，Backend 以 access token 为输入执行幂等登出。
3. Backend `AuthService.logout()` 对缺失 token、fake token、已失效 session 都保持成功返回；客户端仍以本地清理 session 为准（`backend/src/services/auth/auth.service.ts:379-416`, `backend/src/app/api/__tests__/auth-session.test.ts:18-45`）。
4. 登出成功后：
   - Android 返回 profile 根页（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:334-345`）。
   - iOS 调用 `router.popToRoot(of: .profile)` 回到“我的”根页（`ios/ShortDrama/Sources/Features/Profile/Views/SettingsView.swift:31-35`）。

## 多端实现

### Android

- 登录页与状态：`feature/auth/ui/LoginScreen.kt`、`feature/auth/model/LoginUiState.kt`、`feature/auth/viewmodel/LoginViewModel.kt`
- 全局认证状态：`core/auth/AuthStateHolder.kt`、`core/auth/AuthBootstrapper.kt`
- 登录入口与回跳：`navigation/NavGraph.kt`、`feature/profile/ui/ProfileScreen.kt`、`feature/ranking/viewmodel/RankingViewModel.kt`
- 持久化与冷却：`AuthSessionStore` + `AuthCooldownStore`（后者用于 OTP cooldown 等非敏感状态）
- 自动化证据：`LoginViewModelTest.kt`、`AuthBootstrapperTest.kt`、`AuthStateHolderTest.kt`、`ProfileViewModelTest.kt`、`SettingsViewModelTest.kt`

### iOS

- 登录页与状态：`Features/Auth/Views/LoginView.swift`、`Features/Auth/ViewModels/LoginViewModel.swift`
- 全局认证状态：`Features/Auth/AuthStore.swift`
- 安全存储：`Core/Storage/AuthSessionStore.swift`、`Core/Storage/KeychainAuthSessionStore.swift`
- 登录拦截上下文：`Domain/Entities/LoginInterceptionContext.swift`
- 登录页承载与回跳：`App/AppShellView.swift`、`App/NavigationRouter.swift`、`Features/Profile/Views/ProfileHomeView.swift`、`Features/Ranking/RankingRouteBuilder.swift`
- 自动化证据：`Tests/ViewModelTests/AuthStoreTests.swift`、`Tests/ViewModelTests/NavigationRouterTests.swift`

### Backend

- 路由层：`app/api/auth/*`、`app/api/users/me/route.ts`
- 中间件：`middleware/auth.ts`
- 业务逻辑：`services/auth/auth.service.ts`
- 共享 schema 与 payload 映射：`lib/schemas.ts`、`app/api/auth/_helpers.ts`
- 自动化证据：`src/app/api/__tests__/auth-otp-requests.test.ts`、`auth-sessions.test.ts`、`auth-session-refreshes.test.ts`、`auth-session.test.ts`、`users-me.test.ts`

### Web

- 本期未接入真实用户端登录流程；Web 仍主要承载管理平台登录与 App Router 骨架，不参与 PRD-08 移动端认证闭环。

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `POST /api/auth/otp-requests` | [../../api/auth.md](../../api/auth.md) | 创建 OTP 请求并触发验证码发送 |
| `POST /api/auth/sessions` | [../../api/auth.md](../../api/auth.md) | 验证码登录 / 自动注册并返回 `AuthSession` |
| `POST /api/auth/session-refreshes` | [../../api/auth.md](../../api/auth.md) | 使用 refresh token 换发新会话 |
| `GET /api/users/me` | [../../api/auth.md](../../api/auth.md) | 校验当前 access token 并返回用户摘要 |
| `DELETE /api/auth/session` | [../../api/auth.md](../../api/auth.md) | 当前客户端会话登出 |
| `GET /api/dramas/rankings` | [../../api/dramas.md](../../api/dramas.md) | 排行页在登录态下读取 `is_booked` |
| `POST /api/dramas/:id/book` | [../../api/dramas.md](../../api/dramas.md) | 预约榜提交接口，要求真实登录态 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Android `AuthStatus` | `MutableStateFlow` | 应用级 | 聚合 anonymous / restoring / authenticated / refreshing / expired | `android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthStateHolder.kt:18-59` |
| Android 登录页状态 | `LoginUiState` + `StateFlow` | 页面级 | 聚合手机号、验证码、协议勾选、发送 / 提交中和 cooldown | `android/app/src/main/java/com/djs66256/short_drama/feature/auth/model/LoginUiState.kt:3-29`, `android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt:43-248` |
| Android 登录成功事件 | `MutableSharedFlow<LoginEvent>` | 页面级 | 把登录成功后的回跳 route 发送给 NavGraph | `android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt:46-50` |
| iOS `AuthStore.status` | `@Published` | 应用级 | 聚合 anonymous / restoring / authenticated / refreshing / expired | `ios/ShortDrama/Sources/Features/Auth/AuthStore.swift:4-109` |
| iOS 登录页状态 | `@Published` 属性集 + `ViewState` | 页面级 | 管理手机号、验证码、协议勾选、错误信息与 cooldown | `ios/ShortDrama/Sources/Features/Auth/ViewModels/LoginViewModel.swift:21-260` |
| iOS 登录拦截上下文 | `presentedLoginContext` | 应用级 | 区分 profile 入口与 ranking booking 入口，并保存 return route | `ios/ShortDrama/Sources/App/NavigationRouter.swift:11-14,62-95`, `ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift:3-29` |
| Backend auth context | `request.auth` | 请求级 | 由 `requireAuthContext()` / `requireRole()` 注入，供 route 读取 userId / role | `backend/src/middleware/auth.ts:82-138` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 应用壳 | 登录页承载与回跳 | iOS 由 `AppShellView` 承载登录页；Android 由主 NavGraph 管理登录 route |
| 我的频道 | 主登录入口与登录后态展示 | 匿名态提供登录入口，登录后展示用户摘要与设置入口 |
| 排行体系 | 预约拦截与登录后继续操作 | 预约榜未登录时统一进入登录流，并保留 returnRoute |
| 数据模型 | `AuthSession` / `AuthUser` 共享契约 | Backend、Android、iOS 需保持字段语义一致 |

### 外部依赖

| 服务 / 框架 | 用途 | 接入方式 |
|-------------|------|---------|
| Supabase Auth | OTP、session、token 校验 | 仅 Backend 服务端接入，移动端不直连 |
| Keychain | iOS 安全存储 `AuthSession` | `KeychainAuthSessionStore` |
| Android 安全存储封装 | Android 持久化 `AuthSession` | `AuthSessionStore` 抽象实现 |
| RESTful Auth API | 移动端登录闭环 | Android / iOS 统一通过 Backend Route Handlers 调用 |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| Web 不参与 PRD-08 用户端登录闭环 | Web 没有与移动端对等的用户登录页与“我的”频道 | 2026-07-29 | 本期范围聚焦 Backend + Android + iOS |
| Android refresh 失败后最终回到匿名态 | `markExpired()` 后紧接 `clearSession()`，expired 仅是瞬时过渡态 | 2026-07-29 | 不影响“需要重新登录”的行为，但 UI 不会长期停留 expired |
| iOS 排行登录回跳保留榜单页语义，但不显式恢复更细粒度 query | 登录后回到 `.rankingHome`，不携带更细分的 `contentType/rankingType` 参数 | 2026-07-29 | 当前 iOS 路由层只公开 `rankingHome`，没有 query 化路由 |
| 设备级黑盒执行仍未自动完成 | 当前 wiki 证据主要来自代码、测试与 QA 文档 | 2026-07-29 | `docs/specs/2026-07-28-prd-08-login/qa-test.md` 已记录降级执行口径 |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 初始创建：收录 PRD-08 认证体系，覆盖移动端登录页、自动注册、会话恢复 / refresh / logout、“我的”频道登录后态与排行预约登录拦截 |

---
*本文档由 llm-wiki skill 自动维护。*