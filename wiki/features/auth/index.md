# 认证体系 (Auth)

> 最后更新：2026-07-29
> 覆盖端：Web / Android / iOS / Backend / Web Admin（Web 用户端仅承接赚钱 H5 登录引导，不提供独立登录页）

## 功能概述

PRD-08 为移动端补齐了从匿名态到登录态的完整认证闭环：Android 与 iOS 都提供手机号验证码登录页；Backend 提供 OTP 请求、验证码创建会话、刷新会话、当前用户查询与登出接口；“我的”频道、排行预约拦截，以及后续 H5 容器承接链路，都基于同一套 `AuthSession` 与 bearer token contract 运作。当前不提供独立注册页，首次验证码校验成功会自动注册用户，并通过 `isNewUser` 向客户端暴露首登语义（`docs/specs/2026-07-28-prd-08-login/spec.md:421-429,488-500`）。

当前仓库中的认证体系已经形成三条并存但职责不同的链路：

1. **移动端用户认证闭环**：Android 与 iOS 已接通手机号验证码登录、自动注册、会话恢复、refresh、`me` 校验与幂等 logout；Backend 通过 Auth API 与 canonical auth middleware 统一提供 access token / refresh token 语义。
2. **评论 / 预约等业务接口鉴权**：评论列表与排行列表使用可选鉴权补充用户态字段；评论写操作、评论点赞与预约继续使用 `requireAuthContext()` + `getAuth(request)` 的强制鉴权语义。
3. **赚钱中心宿主登录同步**：Web `/earn` 页面本身不提供独立登录页，而是通过 `earn.requestLogin` 请求 Native 宿主拉起登录承接页，再由宿主以 `earn.syncAuthState` 回流权威登录快照；H5 只以内存态持有 `apiAccessToken`，不自行持久化会话。

与此同时，**Web Admin 管理端鉴权**继续使用 Supabase JWT + role 校验，通过 `requireRole(...)` 保护 admin routes。

- **核心价值**：统一移动端用户会话、受保护业务接口与管理后台权限校验口径，并为赚钱中心 H5 提供 Native 宿主登录同步能力。
- **覆盖范围**：Backend Auth API / middleware、Android 登录页与会话恢复、iOS 登录页与会话恢复、Profile 登录后态、排行预约登录拦截、评论写接口鉴权、赚钱 H5 宿主登录同步、Web Admin role 校验。
- **当前状态**：Android / iOS / Backend 用户认证闭环已落地；Web 用户端不实现独立登录页；Web `/earn` 已接入宿主同步登录态；Web Admin 已落地真实 JWT + role 校验。

## 入口与路由

| 端 | 入口 | 路由 / 触发方式 | 源文件 |
|----|------|----------------|--------|
| Web | 赚钱页登录引导 CTA | `earn.requestLogin` bridge message，请求宿主拉起登录承接页 | `web/src/features/earn/bridge/earn-bridge.ts`、`web/src/features/earn/hooks/useEarnPage.ts` |
| Android | “我的”匿名态登录按钮 | `AppDestination.login(returnRoute = profile)` | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/ProfileScreen.kt` |
| Android | 排行预约登录拦截 | `RankingEffect.RequireLogin(returnRoute)` → `login?returnRoute=...&source=ranking_booking` | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` |
| Android | 评论写操作登录拦截 | `CommentEffect.RequireLogin(CommentLoginContext)`，宿主层当前以 placeholder dialog / Toast 承接 | `android/app/src/main/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentSheetViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` |
| Android | 商城商品点击登录承接 | `mall.requestLogin` → `AppDestination.Route.MALL_LOGIN`，并保留 `returnTarget=/mall` | `android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` |
| Android | 赚钱页登录承接 | `earn/login?returnTarget=/earn` | `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` |
| iOS | “我的”匿名态登录按钮 | `router.presentLogin(LoginInterceptionContext(source: .profileEntry))` | `ios/ShortDrama/Sources/Features/Profile/Views/ProfileHomeView.swift` |
| iOS | 排行预约登录拦截 | `RankingRouteBuilder.loginContext(for:)` → `LoginInterceptionContext(source: .rankingBooking, returnRoute: .rankingHome)` | `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift`、`ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift` |
| iOS | 评论写操作登录拦截 | `.requireLogin(CommentLoginContext)`，宿主层当前以 alert 承接 | `ios/ShortDrama/Sources/Features/Comments/ViewModels/CommentSheetViewModel.swift`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`、`ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` |
| iOS | 商城商品点击登录承接 | `MallContainerViewModel` 发出 `.requestLogin`，由 `NavigationRouter.presentMallLogin(_:)` 通过 `fullScreenCover` 承载 | `ios/ShortDrama/Sources/Features/Mall/ViewModels/MallContainerViewModel.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Sources/Features/Mall/Views/MallContainerView.swift` |
| iOS | 登录页承载方式 | `AppShellView.fullScreenCover(item: presentedLoginContext)` | `ios/ShortDrama/Sources/App/AppShellView.swift` |
| iOS | 赚钱页登录承接 | `.earnLogin(context:)` → `router.dismissEarnLogin(completed:)` | `ios/ShortDrama/Sources/App/AppRoute.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift` |
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

### 流程：赚钱中心通过宿主同步登录态，而不是在 H5 内自行持久化会话

1. H5 `/earn` 首次加载完成后，不会主动持久化 token，而是等待 Native 宿主发送 `earn.syncAuthState` 消息。
2. Android `EarnViewModel` 在页面加载成功、登录返回与应用恢复时，都会从 `AuthSessionProvider.currentSession()` 读取当前会话快照，并把 `apiAccessToken` 与 `expiresAt` 作为 host message 发回 H5。
3. iOS `EarnContainerViewModel` 同样在页面加载成功、登录返回与应用恢复时发送 `earn.syncAuthState`；当登录取消时只回传未登录快照，不伪造成功态。
4. H5 收到该消息后只把 `apiAccessToken` 保存在 reducer 内存态；如果宿主未登录、token 缺失或后续 `complete-task` 返回 401，就立即清空内存 token 并重新展示登录引导。
5. 因此赚钱中心沿用了统一 Auth API contract，但 Web 侧并不拥有独立登录页、refresh 流程或持久化 session，它只消费 Native 宿主同步后的权威快照。

## 多端实现

### Web

- 赚钱页登录引导：`web/src/features/earn/bridge/earn-bridge.ts`、`web/src/features/earn/hooks/useEarnPage.ts`
- 宿主消息消费：`web/src/features/earn/bridge/earn-host-sync.ts`
- 特点：Web 不提供独立用户端登录页；只有 `/earn` 页面会通过 `earn.requestLogin` 请求 Native 宿主拉起登录，并以内存态消费 `earn.syncAuthState` 回传的登录快照。

### Android

- 登录页与状态：`android/app/src/main/java/com/djs66256/short_drama/feature/auth/ui/LoginScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/auth/model/LoginUiState.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt`
- 全局认证状态：`android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthStateHolder.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthBootstrapper.kt`
- 登录入口与回跳：`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/ProfileScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt`
- 评论登录恢复：`android/app/src/main/java/com/djs66256/short_drama/feature/comments/model/CommentLoginContext.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentSheetViewModel.kt`
- earn 登录承接与宿主同步：`android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`
- 持久化与冷却：`android/app/src/main/java/com/djs66256/short_drama/core/storage/AuthSessionStore.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/storage/AuthCooldownStore.kt`
- 自动化证据：`android/app/src/test/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModelTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/core/auth/AuthBootstrapperTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/core/auth/AuthStateHolderTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/profile/viewmodel/ProfileViewModelTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/profile/viewmodel/SettingsViewModelTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModelTest.kt`

### iOS

- 登录页与状态：`ios/ShortDrama/Sources/Features/Auth/Views/LoginView.swift`、`ios/ShortDrama/Sources/Features/Auth/ViewModels/LoginViewModel.swift`
- 全局认证状态：`ios/ShortDrama/Sources/Features/Auth/AuthStore.swift`
- 安全存储：`ios/ShortDrama/Sources/Core/Storage/AuthSessionStore.swift`、`ios/ShortDrama/Sources/Core/Storage/KeychainAuthSessionStore.swift`
- 登录拦截上下文：`ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift`
- 评论登录恢复：`ios/ShortDrama/Sources/Features/Comments/CommentLoginContext.swift`、`ios/ShortDrama/Sources/Features/Comments/ViewModels/CommentSheetViewModel.swift`
- 登录页承载与回跳：`ios/ShortDrama/Sources/App/AppShellView.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Sources/Features/Profile/Views/ProfileHomeView.swift`、`ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift`
- earn 登录承接与宿主同步：`ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`
- 自动化证据：`ios/ShortDrama/Tests/ViewModelTests/AuthStoreTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/EarnContainerViewModelTests.swift`

### Backend

- 用户认证 Route：`backend/src/app/api/auth/*`、`backend/src/app/api/users/me/route.ts`
- 中间件：`backend/src/middleware/auth.ts`
- 业务逻辑：`backend/src/services/auth/auth.service.ts`
- 共享 schema 与 payload 映射：`backend/src/lib/schemas.ts`、`backend/src/app/api/auth/_helpers.ts`
- 评论鉴权 Route：`backend/src/app/api/dramas/[id]/comments/route.ts`、`backend/src/app/api/dramas/[id]/comments/[commentId]/like/route.ts`
- 自动化证据：`backend/src/app/api/__tests__/auth-otp-requests.test.ts`、`backend/src/app/api/__tests__/auth-sessions.test.ts`、`backend/src/app/api/__tests__/auth-session-refreshes.test.ts`、`backend/src/app/api/__tests__/auth-session.test.ts`、`backend/src/app/api/__tests__/users-me.test.ts`

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
| `GET /api/users/me` | [../../api/auth.md](../../api/auth.md) | 校验当前 access token 并返回用户摘要；Native 宿主恢复该快照后也可继续向赚钱 H5 同步当前登录态 |
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
| Android 登录页状态 | `LoginUiState` + `StateFlow` | 页面级 | 聚合手机号、验证码、协议勾选、发送 / 提交中和 cooldown | `android/app/src/main/java/com/djs66256/short_drama/feature/auth/model/LoginUiState.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt` |
| Android 登录成功事件 | `MutableSharedFlow<LoginEvent>` | 页面级 | 把登录成功后的回跳 route 发送给 NavGraph | `android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt` |
| Android 评论登录上下文 | Compose state / ViewModel state | 页面级 | 首页 / 播放器保留 `pendingCommentLoginContext` 用于恢复评论容器 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` |
| Android earn auth snapshot | `AuthSessionProvider.currentSession()` → `EarnHostMessage.SyncAuthState` | 页面级 | 赚钱容器在加载成功、登录返回和 app resume 时读取当前 session 并向 H5 同步内存 token 快照 | `android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt` |
| iOS `AuthStore.status` | `@Published` | 应用级 | 聚合 anonymous / restoring / authenticated / refreshing / expired | `ios/ShortDrama/Sources/Features/Auth/AuthStore.swift` |
| iOS 登录页状态 | `@Published` 属性集 + `ViewState` | 页面级 | 管理手机号、验证码、协议勾选、错误信息与 cooldown | `ios/ShortDrama/Sources/Features/Auth/ViewModels/LoginViewModel.swift` |
| iOS 登录拦截上下文 | `presentedLoginContext` | 应用级 | 区分 profile / ranking 等来源并保存 return route | `ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift` |
| iOS 评论登录上下文 | `@Published` / ViewModel 私有状态 | 页面级 | 首页 / 播放器保留 `pendingCommentLoginContext` 并恢复 comments sheet | `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift`、`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` |
| iOS earn auth snapshot | `EarnContainerViewModel.authToken/authExpiry` + `hostMessage` | 页面级 | 赚钱容器通过 `earn.syncAuthState` 把当前登录态与 token 快照回流给 H5 | `ios/ShortDrama/Sources/Features/Earn/ViewModels/EarnContainerViewModel.swift` |
| Web earn token | reducer 内存态 `apiAccessToken` | 页面级 | 只来自宿主同步，不持久化；401 或未登录快照时立即清空 | `web/src/features/earn/hooks/useEarnPage.ts` |
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
| 商城频道 | 商品点击登录承接 | 匿名点击商品时先出现 H5 页内拦截，再复用统一 Native 登录承接并保持 `returnTarget=/mall` |
| 赚钱中心 | 宿主登录同步 | `/earn` 不自持久化 token，而是消费 Native 同步过来的权威快照 |
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
| Web 不提供独立用户端登录页 | Web 没有与移动端对等的 profile 登录页面；只有赚钱 H5 能通过宿主 bridge 请求登录 | 2026-07-29 | Web `/earn` 仍依赖 Native 宿主同步 auth snapshot |
| Android refresh 失败后最终回到匿名态 | `expired` 主要作为瞬时过渡态，不会长期停留 | 2026-07-29 | 不影响“需要重新登录”的行为 |
| iOS 排行登录回跳保留榜单页语义，但不显式恢复更细粒度 query | 登录后回到 `.rankingHome`，不携带更细分参数 | 2026-07-29 | 当前 iOS 路由层只公开 `rankingHome` |
| 评论登录承接仍是轻量宿主方案 | 能验证“拦截 + 恢复评论容器”语义，但不能验证完整登录跳转体验 | 2026-07-29 | Android placeholder dialog；iOS alert |
| 赚钱 H5 的 token 只存在于内存态 | H5 容器重建、刷新或宿主未重新同步时，需要重新等待 `earn.syncAuthState` | 2026-07-29 | 这是当前实现的显式安全边界 |
| 设备级黑盒执行仍未自动完成 | 当前 wiki 证据主要来自代码、测试与 QA 文档 | 2026-07-29 | 相关结论以代码为准 |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 更新：以当前代码为准重写认证体系文档，统一收录移动端用户认证闭环、评论与预约接口对 canonical auth middleware 的复用、商城匿名商品点击登录承接、赚钱 H5 对宿主登录同步的依赖，以及 Web Admin 的真实 JWT + role 校验 |
| 2026-07-29 | 初始创建：收录 PRD-08 认证体系，覆盖移动端登录页、自动注册、会话恢复 / refresh / logout、“我的”频道登录后态与排行预约登录拦截 |

---
*本文档由 llm-wiki skill 自动维护。*
