# 认证体系 (Auth)

> 最后更新：2026-07-29
> 覆盖端：Android / iOS / Backend / Web Admin

## 功能概述

当前仓库中的认证体系已经形成两条并存但职责不同的链路：

1. **移动端用户认证闭环**：PRD-08 已为 Android 与 iOS 接通手机号验证码登录、自动注册、会话恢复、refresh、`me` 校验与幂等 logout；Backend 通过 Auth API 与 canonical auth middleware 统一提供 access token / refresh token 语义。
2. **Web Admin 管理端鉴权**：后台管理接口继续使用 Supabase JWT + role 校验，通过 `requireRole(...)` 保护 admin routes。

与 PRD-09 评论系统直接相关的真实现状是：评论列表 `GET /api/dramas/:id/comments` 走 `resolveOptionalAuthContext()` 可选鉴权；发表评论与点赞通过 `requireAuthContext()` + `getAuth(request)` 强制要求登录。也就是说，评论写接口已经对齐当前 canonical auth middleware，而不是早期的 `x-user-id` / `Bearer <user-id>` skeleton helper。

- **核心价值**：统一移动端用户会话、受保护业务接口与管理后台权限校验口径
- **覆盖范围**：Backend Auth API / middleware、Android 登录页与会话恢复、iOS 登录页与会话恢复、Profile 登录后态、排行预约登录拦截、评论写接口鉴权、Web Admin role 校验
- **当前状态**：Android / iOS / Backend 用户认证闭环已落地；Web 用户端不实现真实登录页；Web Admin 已落地真实 JWT + role 校验

## 入口与路由

| 端 | 入口 | 路由 / 触发方式 | 源文件 |
|----|------|----------------|--------|
| Android | “我的”匿名态登录按钮 | `AppDestination.login(returnRoute = profile)` | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/ProfileScreen.kt` |
| Android | 排行预约登录拦截 | `RankingEffect.RequireLogin(returnRoute)` → `login?returnRoute=...&source=ranking_booking` | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` |
| Android | 评论写操作登录拦截 | `CommentEffect.RequireLogin(CommentLoginContext)`，宿主层当前以 placeholder dialog / Toast 承接 | `android/app/src/main/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentSheetViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` |
| iOS | “我的”匿名态登录按钮 | `router.presentLogin(LoginInterceptionContext(source: .profileEntry))` | `ios/ShortDrama/Sources/Features/Profile/Views/ProfileHomeView.swift` |
| iOS | 排行预约登录拦截 | `RankingRouteBuilder.loginContext(for:)` → `LoginInterceptionContext(source: .rankingBooking, returnRoute: .rankingHome)` | `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift`、`ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift` |
| iOS | 评论写操作登录拦截 | `.requireLogin(CommentLoginContext)`，宿主层当前以 alert 承接 | `ios/ShortDrama/Sources/Features/Comments/ViewModels/CommentSheetViewModel.swift`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`、`ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` |
| iOS | 登录页承载方式 | `AppShellView.fullScreenCover(item: presentedLoginContext)` | `ios/ShortDrama/Sources/App/AppShellView.swift` |
| Backend | 用户 Auth API | `/api/auth/otp-requests`、`/api/auth/sessions`、`/api/auth/session-refreshes`、`/api/users/me`、`/api/auth/session` | `backend/src/app/api/auth/otp-requests/route.ts`、`backend/src/app/api/auth/sessions/route.ts`、`backend/src/app/api/auth/session-refreshes/route.ts`、`backend/src/app/api/users/me/route.ts`、`backend/src/app/api/auth/session/route.ts` |
| Backend | 评论可选 / 强制鉴权接口 | `GET /api/dramas/:id/comments`、`POST /api/dramas/:id/comments`、`POST /api/dramas/:id/comments/:commentId/like` | `backend/src/app/api/dramas/[id]/comments/route.ts`、`backend/src/app/api/dramas/[id]/comments/[commentId]/like/route.ts` |
| Web Admin | 管理后台登录 | `POST /api/admin/auth/login` | `backend/src/app/api/admin/auth/login/route.ts` |
| Web Admin | 管理接口访问保护 | `requireRole([...])` 包裹 admin routes | `backend/src/app/api/admin/stats/route.ts`、`backend/src/app/api/admin/dramas/route.ts`、`backend/src/app/api/admin/users/route.ts`、`backend/src/app/api/admin/users/[id]/role/route.ts` |

## 核心逻辑

### 流程：手机号验证码登录 / 自动注册

1. 用户从“我的”匿名态、排行预约拦截，或评论写操作拦截进入登录承接。
2. 客户端输入手机号、验证码并勾选协议后，可先发起 OTP 请求。
   - Android：`LoginUiState.canSendOtp` 同时校验协议、手机号格式、冷却状态与发送 / 提交中状态。
   - iOS：`LoginViewModel.canSendOtp` 同样校验手机号、协议与 cooldown。
3. Backend `POST /api/auth/otp-requests` 创建 OTP 请求；本地测试环境支持 test OTP bypass，短信 provider 未启用时统一映射为 `SERVICE_UNAVAILABLE`。
4. 用户提交验证码后，客户端调用 `POST /api/auth/sessions`。
5. Backend `AuthService.createSession()` 负责验证码校验、自动注册 / 登录和 session 构建；Route 层把响应字段映射为 camelCase payload。
6. 登录成功后，客户端保存完整 `AuthSession`，更新全局认证状态，并依据拦截上下文回到 profile、ranking 或重新打开 comments 容器。

### 流程：启动恢复、`me` 校验与 refresh

1. 客户端启动时先从安全存储读取本地 session。
   - Android：`AuthStateHolder.restoreIfNeeded()` 先恢复本地快照。
   - iOS：`AuthStore.restoreIfNeeded()` 先从 `AuthSessionStore` 读取 session。
2. 若存在 session，则优先调用 `GET /api/users/me` 校验 access token。
3. access token 失效时，再调用 `POST /api/auth/session-refreshes` 尝试刷新。
4. refresh 成功则原子替换本地 session；失败则清空本地 session，回到匿名 / 过期态。
5. Backend `verifyJwt()` 统一负责解析 bearer token：本地测试 token 走 local auth session，正式 token 走 Supabase `auth.getUser(token)`。

### 流程：业务接口鉴权

1. 需要登录的业务 route 使用 `requireAuthContext(...)`，并通过 `getAuth(request)` 读取 `{ userId, role }`。
2. 可匿名访问但需要补充用户态字段的 route 使用 `resolveOptionalAuthContext(request)`。
3. 当前已明确采用这套 canonical helper 的接口包括：
   - `GET /api/dramas/rankings`：可选鉴权，补充 `is_booked`
   - `POST /api/dramas/:id/book`：强制鉴权
   - `GET /api/dramas/:id/comments`：可选鉴权，补充 `liked`
   - `POST /api/dramas/:id/comments`：强制鉴权
   - `POST /api/dramas/:id/comments/:commentId/like`：强制鉴权
4. Admin 接口在此基础上进一步用 `requireRole([...])` 校验 `app_metadata.role`，只接受 `admin / editor / viewer`。

### 流程：登出与本地清理

1. 用户从设置页点击“退出登录”。
2. 客户端调用 `DELETE /api/auth/session`，Backend 以 access token 为输入执行幂等 logout。
3. `AuthService.logout()` 对缺失 token、fake token、已失效 session 都保持成功返回；客户端仍以本地清理 session 为准。
4. 登出成功后：
   - Android 回到 profile 根页。
   - iOS 回到“我的”根页。

### 流程：评论登录恢复只恢复容器上下文

1. 评论输入或点赞前，comments ViewModel 会先判断是否已登录。
2. 未登录时，不直接发起写请求，而是创建结构化 `CommentLoginContext`。
3. 宿主层当前承接仍是轻量方案：Android placeholder dialog / Toast，iOS alert。
4. 登录成功后只重新打开 comments sheet / bottom sheet，不自动重放发送评论或点赞动作。
5. 该语义与 PRD-09 spec / design 保持一致。

## 多端实现

### Android

- 登录页与状态：`android/app/src/main/java/com/djs66256/short_drama/feature/auth/ui/LoginScreen.kt`、`feature/auth/model/LoginUiState.kt`、`feature/auth/viewmodel/LoginViewModel.kt`
- 全局认证状态：`android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthStateHolder.kt`、`core/auth/AuthBootstrapper.kt`
- 登录入口与回跳：`navigation/NavGraph.kt`、`feature/profile/ui/ProfileScreen.kt`、`feature/ranking/viewmodel/RankingViewModel.kt`
- 评论登录恢复：`feature/comments/model/CommentLoginContext.kt`、`feature/comments/viewmodel/CommentSheetViewModel.kt`
- 持久化与冷却：`AuthSessionStore` + `AuthCooldownStore`
- 自动化证据：`LoginViewModelTest.kt`、`AuthBootstrapperTest.kt`、`AuthStateHolderTest.kt`、`ProfileViewModelTest.kt`、`SettingsViewModelTest.kt`

### iOS

- 登录页与状态：`ios/ShortDrama/Sources/Features/Auth/Views/LoginView.swift`、`Features/Auth/ViewModels/LoginViewModel.swift`
- 全局认证状态：`ios/ShortDrama/Sources/Features/Auth/AuthStore.swift`
- 安全存储：`ios/ShortDrama/Sources/Core/Storage/AuthSessionStore.swift`、`Core/Storage/KeychainAuthSessionStore.swift`
- 登录拦截上下文：`ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift`
- 评论登录恢复：`ios/ShortDrama/Sources/Features/Comments/CommentLoginContext.swift`、`Features/Comments/ViewModels/CommentSheetViewModel.swift`
- 登录页承载与回跳：`ios/ShortDrama/Sources/App/AppShellView.swift`、`App/NavigationRouter.swift`、`Features/Profile/Views/ProfileHomeView.swift`、`Features/Ranking/RankingRouteBuilder.swift`
- 自动化证据：`ios/ShortDrama/Tests/ViewModelTests/AuthStoreTests.swift`、`NavigationRouterTests.swift`

### Backend

- 用户认证 Route：`backend/src/app/api/auth/*`、`backend/src/app/api/users/me/route.ts`
- 中间件：`backend/src/middleware/auth.ts`
- 业务逻辑：`backend/src/services/auth/auth.service.ts`
- 共享 schema 与 payload 映射：`backend/src/lib/schemas.ts`、`backend/src/app/api/auth/_helpers.ts`
- 评论鉴权 Route：`backend/src/app/api/dramas/[id]/comments/route.ts`、`backend/src/app/api/dramas/[id]/comments/[commentId]/like/route.ts`
- 自动化证据：`backend/src/app/api/__tests__/auth-otp-requests.test.ts`、`auth-sessions.test.ts`、`auth-session-refreshes.test.ts`、`auth-session.test.ts`、`users-me.test.ts`

### Web / Admin

- Web 用户端本期不接入真实登录页或“我的”频道
- Admin 登录入口：`backend/src/app/api/admin/auth/login/route.ts`
- 权限保护：`backend/src/middleware/auth.ts` 中的 `requireRole(...)`
- 受保护 route 示例：`backend/src/app/api/admin/dramas/route.ts`、`backend/src/app/api/admin/users/route.ts`、`backend/src/app/api/admin/users/[id]/role/route.ts`

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `POST /api/auth/otp-requests` | [../../api/auth.md](../../api/auth.md) | 创建 OTP 请求并触发验证码发送 |
| `POST /api/auth/sessions` | [../../api/auth.md](../../api/auth.md) | 验证码登录 / 自动注册并返回 `AuthSession` |
| `POST /api/auth/session-refreshes` | [../../api/auth.md](../../api/auth.md) | 使用 refresh token 换发新会话 |
| `GET /api/users/me` | [../../api/auth.md](../../api/auth.md) | 校验当前 access token 并返回用户摘要 |
| `DELETE /api/auth/session` | [../../api/auth.md](../../api/auth.md) | 当前客户端会话登出 |
| `GET /api/dramas/rankings` | [../../api/dramas.md](../../api/dramas.md) | 排行页在登录态下补充 `is_booked` |
| `POST /api/dramas/:id/book` | [../../api/dramas.md](../../api/dramas.md) | 预约榜提交接口，要求当前认证上下文 |
| `GET /api/dramas/:id/comments` | [../../api/dramas.md](../../api/dramas.md) | 评论列表接口，支持可选登录态 |
| `POST /api/dramas/:id/comments` | [../../api/dramas.md](../../api/dramas.md) | 评论发表接口，要求登录 |
| `POST /api/dramas/:id/comments/:commentId/like` | [../../api/dramas.md](../../api/dramas.md) | 评论点赞 / 取消点赞接口，要求登录 |
| `POST /api/admin/auth/login` | [../../api/admin.md](../../api/admin.md) | Admin 登录入口，返回 Supabase access token |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Android `AuthStatus` | `MutableStateFlow` | 应用级 | 聚合 anonymous / restoring / authenticated / refreshing / expired | `android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthStateHolder.kt` |
| Android 登录页状态 | `LoginUiState` + `StateFlow` | 页面级 | 聚合手机号、验证码、协议勾选、发送 / 提交中和 cooldown | `android/app/src/main/java/com/djs66256/short_drama/feature/auth/model/LoginUiState.kt`、`feature/auth/viewmodel/LoginViewModel.kt` |
| Android 登录成功事件 | `MutableSharedFlow<LoginEvent>` | 页面级 | 把登录成功后的回跳 route 发送给 NavGraph | `android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt` |
| Android 评论登录上下文 | Compose state / ViewModel state | 页面级 | 首页 / 播放器保留 `pendingCommentLoginContext` 用于恢复评论容器 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`feature/player/viewmodel/PlayerViewModel.kt` |
| iOS `AuthStore.status` | `@Published` | 应用级 | 聚合 anonymous / restoring / authenticated / refreshing / expired | `ios/ShortDrama/Sources/Features/Auth/AuthStore.swift` |
| iOS 登录页状态 | `@Published` 属性集 + `ViewState` | 页面级 | 管理手机号、验证码、协议勾选、错误信息与 cooldown | `ios/ShortDrama/Sources/Features/Auth/ViewModels/LoginViewModel.swift` |
| iOS 登录拦截上下文 | `presentedLoginContext` | 应用级 | 区分 profile / ranking 等来源并保存 return route | `ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift` |
| iOS 评论登录上下文 | `@Published` / ViewModel 私有状态 | 页面级 | 首页 / 播放器保留 `pendingCommentLoginContext` 并恢复 comments sheet | `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift`、`Features/Player/ViewModels/PlayerViewModel.swift` |
| Backend auth context | `request.auth` | 请求级 | 由 `requireAuthContext()` / `requireRole()` 注入，供 route 读取 `userId / role` | `backend/src/middleware/auth.ts` |
| Backend `AuthContext.role` | JWT `app_metadata.role` | 请求级 | 管理端真实权限来源 | `backend/src/middleware/auth.ts` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 应用壳 | 登录页承载与回跳 | iOS 由 `AppShellView` 承载登录页；Android 由主 NavGraph 管理登录 route |
| 我的频道 | 主登录入口与登录后态展示 | 匿名态提供登录入口，登录后展示用户摘要与设置入口 |
| 排行体系 | 预约拦截与登录后继续操作 | 预约榜未登录时统一进入登录流，并保留 return route |
| 评论能力 | 写接口鉴权与容器恢复 | 评论写操作复用当前 auth 基线，并在登录后只恢复评论容器 |
| 数据模型 | `AuthSession` / `AuthUser` 共享契约 | Backend、Android、iOS 需保持字段语义一致 |
| 管理平台 | 真实 role 校验 | Admin routes 依赖 `requireRole(...)` 与 Supabase role metadata |

### 外部依赖

| 服务 / 框架 | 用途 | 接入方式 |
|-------------|------|---------|
| Supabase Auth | OTP、session、token 校验与 Admin 登录 | Backend 服务端接入，移动端不直连 |
| Keychain | iOS 安全存储 `AuthSession` | `KeychainAuthSessionStore` |
| Android 安全存储封装 | Android 持久化 `AuthSession` | `AuthSessionStore` 抽象实现 |
| RESTful Auth API | 移动端登录闭环 | Android / iOS 统一通过 Backend Route Handlers 调用 |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| Web 不参与移动端用户认证闭环 | Web 没有与移动端对等的用户登录页与“我的”频道 | 2026-07-29 | 本期范围聚焦 Backend + Android + iOS 用户认证，以及 Web Admin |
| Android refresh 失败后最终回到匿名态 | `expired` 主要作为瞬时过渡态，不会长期停留 | 2026-07-29 | 不影响“需要重新登录”的行为 |
| iOS 排行登录回跳保留榜单页语义，但不显式恢复更细粒度 query | 登录后回到 `.rankingHome`，不携带更细分参数 | 2026-07-29 | 当前 iOS 路由层只公开 `rankingHome` |
| 评论登录承接仍是轻量宿主方案 | 能验证“拦截 + 恢复评论容器”语义，但不能验证完整登录跳转体验 | 2026-07-29 | Android placeholder dialog；iOS alert |
| 设备级黑盒执行仍未自动完成 | 当前 wiki 证据主要来自代码、测试与 QA 文档 | 2026-07-29 | 相关结论以代码为准 |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 更新：以当前代码为准重写认证体系文档，统一收录移动端用户认证闭环、评论与预约接口对 canonical auth middleware 的复用，以及 Web Admin 的真实 JWT + role 校验 |

---
*本文档由 llm-wiki skill 自动维护。*
