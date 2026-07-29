# 应用壳 (App Shell)

> 最后更新：2026-07-27

## 功能概述

应用壳负责承载各端应用的启动入口、路由容器与基础页面骨架。PRD-01 已将移动端应用从单页占位结构演进为 5 个一级频道的底部导航容器；PRD-02 让首页频道从“应用信息占位页”演进为 Native 首页 Feed 首屏；PRD-05 把排行能力挂载到首页频道所属的搜索发现链路下；PRD-06 则继续把分类浏览能力挂载到同一条发现链路中，用真实 Native 分类页替换 Android / iOS 既有分类占位承接页；PRD-08 再把“我的”频道从纯占位容器推进为真实登录 / 账号承载入口，并在应用壳中补齐登录弹层 / 登录路由、设置页与退出登录回跳链路。Web 端继续维持 SSR-first 的 Next.js App Router 结构，但 `/search` 仍是占位页，也未新增真实 `/classification` 或用户端登录承载页面；商城（mall）与赚钱（earn）继续由 H5 承载，不属于本期 Native 分类页范围（`PRODUCT.md:22-25`）。

- **覆盖端**：Web、Android、iOS、Backend
- **核心价值**：为首页 Feed、搜索发现、排行页、分类页、播放页、详情页，以及“我的”频道的登录 / 设置链路提供统一承载容器
- **当前状态**：移动端导航骨架已落地，其中首页频道已接入 Feed、搜索发现、排行与分类等真实 Native 子页面；“我的”频道也已接入真实登录 / 设置 / 退出登录承接，其余频道仍以占位实现为主

## 入口与路由

### Web
- 入口组件：`web/src/app/layout.tsx:15-33`（根布局与全局 metadata）+ `web/src/app/page.tsx`（首页）
- 路由方案：Next.js App Router，Page 层仅负责路由委托
- 当前可访问骨架路由：`/`、`/play/[id]`、`/detail/[id]`、`/search`、`/rankings`、`/mall`（见 `web/src/app/play/[id]/page.tsx:14-39`、`web/src/app/detail/[id]/page.tsx:14-39`、`web/src/app/search/page.tsx:1-10`、`web/src/app/rankings/page.tsx:1-9`、`web/src/app/mall/page.tsx:1-10`）
- 首页现状：`HomeScreen` 仍展示应用信息和代表性链接，不消费首页 Feed、排行或真实分类数据（`web/src/features/home/HomeScreen.tsx:12-55`）

### Backend
- 入口组件：`backend/src/app/layout.tsx` + `backend/src/app/page.tsx`
- 提供 `/` 页面展示服务信息，并保留 `/api/health` 作为健康检查入口
- 首页 / 搜索 / 热搜 / 分类标签 / 排行相关接口已形成首页频道的数据与发现能力承载：
  - `GET /api/dramas`（首页 Feed）
  - `GET /api/dramas/search`（搜索结果、分类标签点击承接）
  - `GET /api/dramas/hot-search`（搜索发现热词）
  - `GET /api/dramas/tags`（分类页标签矩阵）
  - `GET /api/dramas/rankings`、`POST /api/dramas/:id/book`（排行与预约）
- 同时提供移动端“我的”频道所需的认证承载接口：
  - `POST /api/auth/otp-requests`
  - `POST /api/auth/sessions`
  - `POST /api/auth/session-refreshes`
  - `GET /api/users/me`
  - `DELETE /api/auth/session`

### Android
- 入口 Activity：`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt:21-54`
- Manifest 声明：`android/app/src/main/AndroidManifest.xml:13-27`，同一 `MainActivity` 同时承担 LAUNCHER 与 deeplink 入口
- 当前容器：`NavGraph` 在 `Scaffold` 中挂载 `NavigationBar` + `NavHost`，提供 5 个一级频道 graph（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:108-309`）
- 首页子路由：`search`、`search/result?query={query}`、`ranking?contentType={contentType}&type={type}`、`classification`、`play/{videoId}`、`player/{videoId}`、`detail/{dramaId}`、`dramaDetail/{dramaId}`（`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:44-53,102-115`）
- 首页现状：home graph 默认展示 `HomeScreen` Feed 状态机，且搜索发现链路中的 `RankingScreen` 与 `ClassificationScreen` 已替换原榜单 / 分类占位承接页（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:152-218`）

### iOS
- 入口 App：`ios/ShortDrama/Sources/App/ShortDramaApp.swift:5-23`
- 当前容器：`AppShellView` 使用 `TabView(selection: $router.selectedTab)` 渲染 5 个 Tab（`ios/ShortDrama/Sources/App/AppShellView.swift:3-20`）
- 一级频道：`AppTab` 定义 `home`、`theater`、`mall`、`earn`、`profile`，并绑定标题/图标（`ios/ShortDrama/Sources/App/AppTab.swift:3-41`）
- 首页子路由：`searchHome`、`searchResult(query:)`、`rankingHome`、`classificationHome`、`player(videoId:)`、`dramaDetail(dramaId:)`，对外公开名称分别映射到 `search` / `search/result` / `ranking` / `classification` / `play` / `detail`（`ios/ShortDrama/Sources/App/AppRoute.swift:4-60`）
- 首页现状：home Tab 默认展示 `HomeView`；`TabNavigationHostView` 已把 `.rankingHome` 绑定到 `RankingHomeView()`、把 `.classificationHome` 绑定到 `ClassificationHomeView()`，不再只是演示型入口页（`ios/ShortDrama/Sources/App/TabNavigationHostView.swift:11-31`）

## 核心逻辑

### Web 端
1. `layout.tsx` 提供全局 metadata 和字体注入（`web/src/app/layout.tsx:15-33`）。
2. 首页 `HomeScreen` 展示应用元信息，并提供代表性导航链接到 `/play/sample`、`/detail/sample`、`/search`、`/rankings`、`/mall`（`web/src/features/home/HomeScreen.tsx:27-50`）。
3. 动态路由页在服务端先对 `id` 做 `trim()` + 非空校验，非法参数走 `notFound()`（`web/src/app/play/[id]/page.tsx:9-39`、`web/src/app/detail/[id]/page.tsx:9-39`）。
4. 搜索、榜单、商城页统一复用 `PlaceholderRouteScreen`，只承担路由骨架职责（`web/src/features/placeholder-route/PlaceholderRouteScreen.tsx:3-21`, `web/src/app/rankings/page.tsx:1-9`）。

### Backend 端
1. 服务首页继续作为运行信息与入口页，不参与移动端首页容器渲染。
2. `/api/health` 继续提供健康检查。
3. `GET /api/dramas` 继续承载首页 Feed 数据，`GET /api/dramas/search` / `GET /api/dramas/hot-search` 继续承载搜索发现链路，`GET /api/dramas/tags` 继续承载分类页标签矩阵，`GET /api/dramas/rankings` / `POST /api/dramas/:id/book` 继续承载排行浏览与预约能力；其中排行列表现已通过 `resolveOptionalAuthContext()` 解析可选 bearer token，预约接口则通过 `requireAuthContext()` 强制要求真实登录态（`backend/src/app/api/dramas/route.ts:8-24`, `backend/src/app/api/dramas/search/route.ts:7-19`, `backend/src/app/api/dramas/hot-search/route.ts:6-11`, `backend/src/app/api/dramas/tags/route.ts:7-18`, `backend/src/app/api/dramas/rankings/route.ts:8-24`, `backend/src/app/api/dramas/[id]/book/route.ts:16-28`, `backend/src/middleware/auth.ts:27-138`）。
4. `POST /api/auth/otp-requests`、`POST /api/auth/sessions`、`POST /api/auth/session-refreshes`、`GET /api/users/me`、`DELETE /api/auth/session` 为移动端“我的”频道和登录拦截提供统一认证闭环；移动端不直接接入 Supabase Auth SDK，而是统一消费这些 Backend Route Handlers（`backend/src/app/api/auth/otp-requests/route.ts:7-14`, `backend/src/app/api/auth/sessions/route.ts:7-14`, `backend/src/app/api/auth/session-refreshes/route.ts:7-14`, `backend/src/app/api/users/me/route.ts:7-13`, `backend/src/app/api/auth/session/route.ts:6-13`）。
5. `/api/player/start` 与 `/api/player/stop` 仍为 501 占位接口，本期未新增播放器真实能力（`backend/src/app/api/player/start/route.ts:1-6`、`backend/src/app/api/player/stop/route.ts:1-6`）。

### Android 端
1. `MainActivity` 在冷启动与 `onNewIntent` 两条路径下统一接收 deeplink，并把解析结果写入 `MainNavigationViewModel` 的 `pendingRoute`（`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt:24-53`）。
2. `NavGraph` 通过 `Scaffold(bottomBar = { NavigationBar { ... }})` 渲染 5 个 Tab，点击时使用 `popUpTo(findStartDestination()) + saveState + restoreState` 保留多 back stack 状态（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:108-146`）。
3. 首页 graph 当前承载真实 `HomeScreen` Feed、`SearchHomeScreen`、`SearchResultScreen`、`RankingScreen` 与 `ClassificationScreen`；分类页点击标签后继续跳转搜索结果页（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:152-228`）。
4. “我的”频道不再是占位页，而是由 `ProfileScreen` / `SettingsScreen` / `LoginScreen` 组成完整账号承载链路：匿名态显示登录入口，已登录态显示用户摘要与设置入口，排行预约未登录时也会跳转到同一登录页并在成功后回跳原榜单 route（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:205-219,320-371`、`android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/ProfileScreen.kt:28-146`、`android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/SettingsScreen.kt:36-117`、`android/app/src/main/java/com/djs66256/short_drama/feature/auth/ui/LoginScreen.kt:43-177`、`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:185-239`）。
5. `LaunchedEffect(uiState.pendingRoute)` 在导航容器 ready 后消费待执行路由，实现冷启动 deeplink 延迟跳转；其中 `PendingRoute.Ranking` 和 `PendingRoute.Classification` 分别进入真实排行页与真实分类页（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:70-105`、`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:123-133`）。

### iOS 端
1. `ShortDramaApp` 持有单例 `NavigationRouter` 并通过 `.environmentObject(router)` 注入全局导航状态（`ios/ShortDrama/Sources/App/ShortDramaApp.swift:7-23`）。
2. `AppShellView` 以 `TabView` 渲染 5 个一级频道，并在 `.task` 中先调用 `authStore.restoreIfNeeded()`、再调用 `router.markContainerReady()`；同时通过 `.fullScreenCover(item: presentedLoginContext)` 承载登录页（`ios/ShortDrama/Sources/App/AppShellView.swift:7-49`）。
3. `NavigationRouter` 为每个 Tab 维护独立 `NavigationPath`，切换频道时不清空其它频道栈；搜索发现、排行与分类等首页子页都 push 到 `home` Tab 的独立路径中，而登录成功后会根据 `returnRoute` 回到 profile 或 ranking 等目标页（`ios/ShortDrama/Sources/App/NavigationRouter.swift:7-108`, `ios/ShortDrama/Sources/App/AppRoute.swift:24-37`）。
4. `TabNavigationHostView` 为每个 Tab 提供独立 `NavigationStack`，首页注册 `HomeView`、`SearchHomeView`、`SearchResultView`、`RankingHomeView`、`ClassificationHomeView`、`PlayerView`、`DramaDetailView`，profile Tab 则注册 `ProfileHomeView` 并把 `.settings` 绑定到真实 `SettingsView`，其余频道复用 `PlaceholderTabView`（`ios/ShortDrama/Sources/App/TabNavigationHostView.swift:9-65`）。
5. `ProfileHomeView` 会根据 `authStore.status` 在匿名态 / restoring / authenticated 三态间切换；匿名态可拉起登录页，已登录态可进入设置页，排行预约未登录时则由 `RankingHomeView` 通过 `RankingRouteBuilder.loginContext(for:)` 复用同一登录承载（`ios/ShortDrama/Sources/Features/Profile/Views/ProfileHomeView.swift:8-105`、`ios/ShortDrama/Sources/Features/Profile/Views/SettingsView.swift:11-56`、`ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift:19-79`、`ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift:3-24`、`ios/ShortDrama/Sources/Features/Auth/AuthStore.swift:4-109`）。
6. `ClassificationHomeView` 在首页 Tab 的子路径中默认加载分类标签矩阵，请求成功后渲染左侧维度 rail 与右侧标签分组，而不是旧的占位承接页（`ios/ShortDrama/Sources/Features/Classification/Views/ClassificationHomeView.swift:18-69`）。

## 多端实现

### Web
- 源文件：`web/src/app/layout.tsx:15-33`、`web/src/app/play/[id]/page.tsx:14-39`、`web/src/app/detail/[id]/page.tsx:14-39`, `web/src/app/rankings/page.tsx:1-9`
- 首页导航入口：`web/src/features/home/HomeScreen.tsx:27-50`
- 占位页复用：`web/src/features/placeholder-route/PlaceholderRouteScreen.tsx:3-21`
- 技术：Next.js 16、React 19、TypeScript、SSR-first App Router

### Android
- 入口与容器：`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt:21-54`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:36-374`
- 路由常量：`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:9-139`
- 首页 / 搜索 / 排行 / 分类承载：`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:41-288`, `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt:57-508`, `android/app/src/main/java/com/djs66256/short_drama/feature/classification/ui/ClassificationScreen.kt:58-260`
- 认证与“我的”频道承载：`android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/ProfileScreen.kt:28-146`, `android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/SettingsScreen.kt:36-117`, `android/app/src/main/java/com/djs66256/short_drama/feature/auth/ui/LoginScreen.kt:43-177`, `android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt:29-248`, `android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthBootstrapper.kt:9-46`
- 占位频道：`android/app/src/main/java/com/djs66256/short_drama/feature/common/ui/PlaceholderScreen.kt:14-38`（仅剧场 / 商城 / 赚钱仍复用）
- 技术：Kotlin 2.0.21、Jetpack Compose、Material3、Navigation Compose、Hilt

### iOS
- 入口与容器：`ios/ShortDrama/Sources/App/ShortDramaApp.swift:5-23`、`ios/ShortDrama/Sources/App/AppShellView.swift:3-55`
- Tab 定义：`ios/ShortDrama/Sources/App/AppTab.swift:3-41`
- 路由与状态：`ios/ShortDrama/Sources/App/AppRoute.swift:4-60`、`ios/ShortDrama/Sources/App/NavigationRouter.swift:5-108`
- 首页 / 搜索 / 排行 / 分类承载：`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:15-224`, `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift:19-109`, `ios/ShortDrama/Sources/Features/Classification/Views/ClassificationHomeView.swift:18-69`
- 认证与“我的”频道承载：`ios/ShortDrama/Sources/Features/Profile/Views/ProfileHomeView.swift:3-111`, `ios/ShortDrama/Sources/Features/Profile/Views/SettingsView.swift:3-63`, `ios/ShortDrama/Sources/Features/Auth/AuthStore.swift:3-110`, `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift:3-24`
- 占位频道：`ios/ShortDrama/Sources/Features/Shell/Views/PlaceholderTabView.swift:3-27`（仅剧场 / 商城 / 赚钱仍复用）
- 技术：Swift 6、SwiftUI、TabView、NavigationStack、Swift Testing

### Backend
- 服务首页：`backend/src/app/page.tsx`
- 健康检查：`backend/src/app/api/health/route.ts`
- 首页 Feed / 搜索 / 热搜 / 分类 tags / 排行接口：`backend/src/app/api/dramas/route.ts:8-24`、`backend/src/app/api/dramas/search/route.ts:7-19`、`backend/src/app/api/dramas/hot-search/route.ts:6-11`、`backend/src/app/api/dramas/tags/route.ts:7-18`、`backend/src/app/api/dramas/rankings/route.ts:8-24`
- 认证接口：`backend/src/app/api/auth/otp-requests/route.ts:7-14`、`backend/src/app/api/auth/sessions/route.ts:7-14`、`backend/src/app/api/auth/session-refreshes/route.ts:7-14`、`backend/src/app/api/users/me/route.ts:7-13`、`backend/src/app/api/auth/session/route.ts:6-13`
- 真实 bearer 鉴权中间件：`backend/src/middleware/auth.ts:27-138`
- 预约接口：`backend/src/app/api/dramas/[id]/book/route.ts:16-28`
- 与导航骨架相关的播放器接口现状：`backend/src/app/api/player/start/route.ts:1-6`、`backend/src/app/api/player/stop/route.ts:1-6`
- 技术：Next.js 16、TypeScript、App Router Route Handlers

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/health` | [../../api/health.md](../../api/health.md) | 服务健康检查，不受分类接入影响 |
| `GET /api/dramas` | [../../api/dramas.md](../../api/dramas.md) | 移动端首页频道的 Feed 数据源 |
| `GET /api/dramas/search` | [../../api/dramas.md](../../api/dramas.md) | 搜索结果页与分类标签点击后的统一结果承接接口 |
| `GET /api/dramas/hot-search` | [../../api/dramas.md](../../api/dramas.md) | 搜索发现页热搜数据源 |
| `GET /api/dramas/tags` | [../../api/dramas.md](../../api/dramas.md) | 移动端分类页标签矩阵数据源 |
| `GET /api/dramas/rankings` | [../../api/dramas.md](../../api/dramas.md) | 移动端排行页的数据源，登录态下可补充 `is_booked` |
| `POST /api/dramas/:id/book` | [../../api/dramas.md](../../api/dramas.md) | 排行预约榜的预约接口，要求真实登录态 |
| `POST /api/auth/otp-requests` | [../../api/auth.md](../../api/auth.md) | 登录页发送验证码 |
| `POST /api/auth/sessions` | [../../api/auth.md](../../api/auth.md) | 验证码登录 / 自动注册并创建会话 |
| `POST /api/auth/session-refreshes` | [../../api/auth.md](../../api/auth.md) | 启动恢复或 token 失效后的 refresh |
| `GET /api/users/me` | [../../api/auth.md](../../api/auth.md) | 恢复本地 session 后校验当前用户 |
| `DELETE /api/auth/session` | [../../api/auth.md](../../api/auth.md) | 设置页退出登录的后端承接 |
| `POST /api/player/start` | [../../api/player.md](../../api/player.md) | 播放页骨架相关占位接口，当前仍返回 501 |
| `POST /api/player/stop` | [../../api/player.md](../../api/player.md) | 播放结束上报占位接口，当前仍返回 501 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Web 路由参数 | Next.js `params` | 页面级 | `play` / `detail` 页面按需读取 `id` 并在服务端先校验 | `web/src/app/play/[id]/page.tsx:14-39`, `web/src/app/detail/[id]/page.tsx:14-39` |
| Android `pendingRoute` | `MutableStateFlow<UiState>` | 应用级 | deeplink 先入队，待 `NavHost` 可消费后执行 | `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt:14-38` |
| Android 多 Tab 栈 | `NavController` + `saveState/restoreState` | Tab 级 | 切换频道时保留已访问 graph 的返回栈 | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:121-128` |
| Android 首页 Feed 状态 | `MutableStateFlow<HomeUiState>` | 页面级 | 首页默认页面状态，承载 loading / list / empty / error / retrying | `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:17-31` |
| Android 排行状态 | `MutableStateFlow<RankingUiState>` | 页面级 | 排行页默认页面状态，承载维度切换、列表、分页、预约中态 | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:31-44,65-77` |
| Android 分类状态 | `MutableStateFlow<ClassificationUiState>` + `MutableSharedFlow<ClassificationEffect>` | 页面级 | 分类页承载 gender、选中维度、标签矩阵与左右滚动联动 | `android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt:27-35,63-67` |
| Android 认证状态 | `AuthStateHolder.authStatus` | 应用级 | 聚合 anonymous / restoring / authenticated / refreshing / expired，并驱动“我的”频道与受保护操作 | `android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthStateHolder.kt:14-59` |
| Android 登录页状态 / 回跳事件 | `LoginUiState` + `MutableSharedFlow<LoginEvent>` | 页面级 | 承载手机号、验证码、协议勾选、发送/提交中、cooldown，以及登录成功后的 returnRoute 回跳 | `android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt:29-248` |
| iOS `selectedTab` | `@Published var selectedTab` | 应用级 | 控制当前激活的一级频道 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:7` |
| iOS `pathsByTab` | `[AppTab: NavigationPath]` | Tab 级 | 每个 Tab 独立维护自己的导航路径 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:8-18` |
| iOS `containerReady` | `@Published private(set) var containerReady` | 应用级 | 冷启动时用于判定是否可立即执行 deeplink 跳转 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:11-18,52-60` |
| iOS 登录弹层上下文 | `presentedLoginContext` | 应用级 | 区分 profile 入口与 ranking booking 入口，并保存 return route | `ios/ShortDrama/Sources/App/NavigationRouter.swift:13,62-95` |
| iOS 认证状态 | `AuthStore.status` + `currentUser` | 应用级 | 聚合匿名/恢复中/已登录/refreshing/expired，并驱动 profile 与登录弹层后的回跳语义 | `ios/ShortDrama/Sources/Features/Auth/AuthStore.swift:4-109` |
| iOS 首页 Feed 状态 | `@Published private(set) var viewState` | 页面级 | 首页默认页面状态，承载 loading / content / empty / error | `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:7-23` |
| iOS 排行状态 | `@Published` 属性集 | 页面级 | 承载排行页维度选择、内容态、分页与登录拦截 | `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:23-30` |
| iOS 分类状态 | `@Published` 属性集 | 页面级 | 承载 gender、选中维度、分类内容态与 scroll reset | `ios/ShortDrama/Sources/Features/Classification/ViewModels/ClassificationViewModel.swift:17-31` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 深链 | 共享导航容器 | deeplink 解析后的目标需要由 App Shell 负责承载和跳转 |
| 搜索发现 | 首页子路由 | 排行和分类都是搜索发现链路的一部分，继续挂在首页频道内 |
| 认证体系 | 登录页承载与登录后回跳 | “我的”频道、排行预约拦截与启动恢复都依赖应用壳提供登录 route / fullScreenCover、设置页和 returnRoute 导航承接 |
| 播放器 | 首页子路由 | `play/:id` 当前由应用壳负责注册与参数透传，真实播放能力后续补齐 |
| 剧集详情 | 首页子路由 | `detail/:id` 当前由应用壳负责注册与参数透传 |
| 首页信息流 | 一级频道默认内容 | 首页是默认激活 Tab，并已从占位页演进为 Feed 首屏 |

### 外部依赖

| 服务 / 框架 | 用途 | 接入方式 |
|-------------|------|---------|
| Next.js App Router | Web/Backend 路由承载 | 文件系统路由 + Route Handlers |
| Navigation Compose | Android 多级导航与状态恢复 | `NavHost` + nested navigation graph |
| SwiftUI `TabView` / `NavigationStack` | iOS 一级频道与子路由承载 | 声明式导航容器 |
| `GET /api/dramas` | 移动端首页数据加载 | Backend Route Handler + 各端网络层调用 |
| `GET /api/dramas/search` / `GET /api/dramas/hot-search` | 搜索发现与分类结果承接 | Backend Route Handler + 各端网络层调用 |
| `GET /api/dramas/tags` | 移动端分类页标签矩阵加载 | Backend Route Handler + 各端网络层调用 |
| `POST /api/auth/otp-requests` / `POST /api/auth/sessions` / `POST /api/auth/session-refreshes` / `GET /api/users/me` / `DELETE /api/auth/session` | 移动端登录、恢复、刷新与登出 | Backend Route Handler + Android/iOS 统一 REST 调用 |
| `GET /api/dramas/rankings` / `POST /api/dramas/:id/book` | 移动端排行与预约 | Backend Route Handler + 各端网络层调用 |

## 已知限制

- Android / iOS 的“我的”频道已接入真实登录 / 设置承载，但剧场、商城、赚钱仍是占位页，真实业务内容尚未接入。
- Web 端当前只补齐路由骨架，没有实现移动端同等的底部导航 UI，也没有实现真实 Feed / 分类 / 排行页或用户端登录页。
- 商城（mall）与赚钱（earn）保持 H5 承载，但当前移动端代码仍是 placeholder tab，尚未接入真实 H5 容器（`PRODUCT.md:22-25`）。
- 播放页与详情页仍是占位实现，仅展示路由参数，不含真实业务数据或播放能力。
- Backend 排行与预约运行时已切到 `DramaSupabaseRepository()`，但首页 Feed / 搜索 / 热搜 / 分类 tags 仍未全面切到真实内容服务；其中分类 tags 也尚未接入真实运营后台（`backend/src/app/api/dramas/rankings/route.ts:17-22`、`backend/src/app/api/dramas/[id]/book/route.ts:20-27`）。
- iOS 排行登录成功后只回到 `.rankingHome`，不显式恢复更细粒度的 `contentType/rankingType` query；Android 会保留完整 `ranking?...` returnRoute。
- 设备级黑盒验证在本轮 workflow 中按规范降级，仅产出 QA 文档，当前证据主要来自自动化测试、QA 文档与代码审查（`docs/specs/2026-07-28-prd-08-login/qa-test.md:1-40`）。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 更新：同步 PRD-08 登录闭环后“我的”频道已接入真实登录 / 设置 / 退出登录承载，补充移动端登录入口与回跳、Backend 认证接口、排行预约登录拦截，以及 Web 仍无用户端登录页的现状 |
| 2026-07-27 | 更新：同步 PRD-06 后首页频道进一步承载真实分类页，补充 Backend 搜索/热搜/分类 tags 接口、移动端 classification 子路由与 Web 仍无真实分类页的现状 |
| 2026-07-27 | 更新：同步 PRD-05 后首页频道承载搜索发现与真实排行页，补充 Backend 排行/预约接口、移动端排行子路由与 Web 仍为占位页的现状 |
| 2026-07-26 | 更新：同步 PRD-02 后首页频道从占位页演进为 Native Feed 首屏，补充 Backend `GET /api/dramas` 作为首页容器依赖的数据源，并修正文档中的首页承载现状 |
| 2026-07-25 | 更新：移动端应用壳从单页骨架演进为 5 Tab 导航容器，Web 补齐路由骨架，并同步修正文档中的入口、路由、状态管理与限制说明 |

---
*本文档由 llm-wiki skill 自动维护。*