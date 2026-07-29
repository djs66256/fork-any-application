# 应用壳 (App Shell)

> 最后更新：2026-07-29

## 功能概述

应用壳负责承载各端应用的启动入口、路由容器与基础页面骨架。PRD-01 已将移动端应用从单页占位结构演进为 5 个一级频道的底部导航容器；PRD-02 让首页频道从“应用信息占位页”演进为 Native 首页 Feed 首屏；PRD-05 把排行能力挂载到首页频道所属的搜索发现链路下；PRD-06 继续把分类浏览能力挂载到同一条发现链路中；PRD-07 再在首页根页左上角补充汉堡菜单触发的左侧抽屉式菜单面板，由 App Shell 统一承载 overlay/menu state，并在关闭动画完成后再执行菜单内导航；PRD-08 再把“我的”频道从纯占位容器推进为真实登录 / 账号承载入口，并在应用壳中补齐登录弹层 / 登录路由、设置页与退出登录回跳链路；PRD-12 则把“剧场”从占位一级频道推进为真实 Native 内容入口，并明确剧场内搜索/分类/排行/新剧会切回首页所属导航栈复用既有页面，只有剧场 feed 本身留在 theater tab 内承载；PRD-13 把“商城”推进为真实 H5 容器入口，补齐 Web `/mall` 与 `/mall/product/[id]`、Android / iOS WebView/WKWebView 容器、搜索/登录 bridge 与商城上下文恢复；PRD-14 再把“赚钱”频道从占位 tab 推进为由 Native 容器承载的 H5 页面，并补齐 earn 专属登录承接、任务播放承接与 host message 回流。Web 端继续维持 SSR-first 的 Next.js App Router 结构，用户端首页仍不是移动端同构实现；商城与赚钱都遵循 H5 承载策略，且 Android / iOS 已分别接入对应容器（`PRODUCT.md:22-25`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:416-570`, `ios/ShortDrama/Sources/App/TabNavigationHostView.swift:53-82`）。

- **覆盖端**：Web、Android、iOS、Backend
- **核心价值**：为首页 Feed、剧场频道、商城频道、赚钱频道、菜单抽屉、搜索发现、排行页、分类页、播放页、详情页，以及“我的”频道的登录 / 设置链路提供统一承载容器
- **当前状态**：移动端导航骨架已落地，其中首页频道已接入 Feed、左侧菜单抽屉、搜索发现、排行与分类等真实 Native 子页面；剧场频道已接入真实 feed；商城与赚钱频道都已接入真实 H5 容器；“我的”频道也已接入真实登录 / 设置 / 退出登录承接

## 入口与路由

### Web
- 入口组件：`web/src/app/layout.tsx:15-33`（根布局与全局 metadata）+ `web/src/app/page.tsx`（首页）
- 路由方案：Next.js App Router，Page 层仅负责路由委托
- 当前可访问骨架路由：`/`、`/play/[id]`、`/detail/[id]`、`/search`、`/rankings`、`/mall`（见 `web/src/app/play/[id]/page.tsx:14-39`、`web/src/app/detail/[id]/page.tsx:14-39`、`web/src/app/search/page.tsx:1-10`、`web/src/app/rankings/page.tsx:1-9`、`web/src/app/mall/page.tsx:1-10`）
- 首页现状：`HomeScreen` 仍展示应用信息和代表性链接，不消费首页 Feed、排行或真实分类数据（`web/src/features/home/HomeScreen.tsx:12-55`）

### Backend
- 入口组件：`backend/src/app/layout.tsx` + `backend/src/app/page.tsx`
- 提供 `/` 页面展示服务信息，并保留 `/api/health` 作为健康检查入口
- 首页 / 剧场 / 搜索 / 热搜 / 分类标签 / 排行相关接口已形成首页频道与剧场频道的数据承载：
  - `GET /api/dramas`（首页 Feed）
  - `GET /api/dramas/channel`（剧场 Feed）
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
- 菜单最近在看与播放器历史仍由 `GET /api/player/recently-viewed`、`POST /api/player/start`、`POST /api/player/stop` 承接，其中 `recently-viewed` 为菜单统一数据源，`start/stop` 当前仍是占位或历史链路依赖接口

### Android
- 入口 Activity：`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt:21-54`
- Manifest 声明：`android/app/src/main/AndroidManifest.xml:13-27`，同一 `MainActivity` 同时承担 LAUNCHER 与 deeplink 入口
- 当前容器：`NavGraph` 在 `Scaffold` 中挂载 `NavigationBar` + `NavHost`，并额外在同层渲染 `MenuPanelDrawer` 与 `SnackbarHost`，统一承载首页菜单 overlay、关闭动画和本地反馈（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:131-171,352-371`）
- 首页子路由：`search`、`search/result?query={query}`、`ranking?contentType={contentType}&type={type}`、`classification`、`new-releases`、`actors`、`menu/login`、`menu/messages`、`menu/booking`、`menu/downloads`、`play/{videoId}`、`player/{videoId}`、`detail/{dramaId}`、`dramaDetail/{dramaId}`、`login?returnRoute=...`（`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:44-57,102-134`）
- 首页现状：home graph 默认展示 `HomeScreen` Feed 状态机；左上角汉堡按钮打开菜单抽屉，抽屉内最近在看、登录/消息/预约/下载承接页和“游戏中心即将上线”反馈都由 home graph + overlay 统一承载；profile graph 则承载 `ProfileScreen` / `SettingsScreen` / `LoginScreen` 形成真实账号链路

### iOS
- 入口 App：`ios/ShortDrama/Sources/App/ShortDramaApp.swift:5-23`
- 当前容器：`AppShellView` 使用 `TabView(selection: $router.selectedTab)` 渲染 5 个 Tab，并在最外层承载菜单 overlay 与登录 `fullScreenCover`（`ios/ShortDrama/Sources/App/AppShellView.swift:3-49`）
- 一级频道：`AppTab` 定义 `home`、`theater`、`mall`、`earn`、`profile`，并绑定标题/图标（`ios/ShortDrama/Sources/App/AppTab.swift:3-41`）
- 首页子路由：`searchHome`、`searchResult(query:)`、`rankingHome`、`classificationHome`、`player(videoId:)`、`dramaDetail(dramaId:)`，对外公开名称分别映射到 `search` / `search/result` / `ranking` / `classification` / `play` / `detail`（`ios/ShortDrama/Sources/App/AppRoute.swift:4-60`）
- 首页现状：home Tab 默认展示 `HomeView`；`TabNavigationHostView` 已把 `.rankingHome` 绑定到 `RankingHomeView()`、把 `.classificationHome` 绑定到 `ClassificationHomeView()`，并通过 `MenuPanelContainerView` 统一承载首页菜单；profile Tab 则承载 `ProfileHomeView` / `SettingsView` 与登录弹层回跳链路

## 核心逻辑

### Web 端
1. `web/src/app/layout.tsx` 提供全局 metadata 和字体注入（`web/src/app/layout.tsx:15-33`）。
2. 首页 `HomeScreen` 展示应用元信息，并提供代表性导航链接到 `/play/sample`、`/detail/sample`、`/search`、`/rankings`、`/mall`（`web/src/features/home/HomeScreen.tsx:27-50`）。
3. 动态路由页在服务端先对 `id` 做 `trim()` + 非空校验，非法参数走 `notFound()`（`web/src/app/play/[id]/page.tsx:9-39`、`web/src/app/detail/[id]/page.tsx:9-39`）。
4. 搜索与榜单页当前仍复用 `PlaceholderRouteScreen`，只承担路由骨架职责；但 `/mall` 已切换为真实 `MallPageScreen`，`/mall/product/[id]` 也已提供带 ID guard 的商品详情占位页（`web/src/features/placeholder-route/PlaceholderRouteScreen.tsx:3-21`、`web/src/app/rankings/page.tsx:1-9`、`web/src/app/mall/page.tsx:1-5`、`web/src/app/mall/product/[id]/page.tsx:1-39`）。

### Backend 端
1. 服务首页继续作为运行信息与入口页，不参与移动端首页容器渲染。
2. `/api/health` 继续提供健康检查。
3. `GET /api/dramas`、`GET /api/dramas/channel`、`GET /api/dramas/search`、`GET /api/dramas/hot-search`、`GET /api/dramas/tags`、`GET /api/dramas/rankings`、`POST /api/dramas/:id/book` 共同承载首页 Feed、剧场 Feed 与搜索发现链路；其中排行列表现已通过 `resolveOptionalAuthContext()` 解析可选 bearer token，预约接口则通过 `requireAuthContext()` 强制要求真实登录态。
4. `GET /api/player/recently-viewed` 已成为首页菜单“最近在看”的统一数据源；`progress/start/stop` 继续与其共享 `X-Playback-Session-Id`，由播放器历史链路提供菜单候选记录。
5. `POST /api/auth/otp-requests`、`POST /api/auth/sessions`、`POST /api/auth/session-refreshes`、`GET /api/users/me`、`DELETE /api/auth/session` 为移动端“我的”频道和排行预约登录拦截提供统一认证闭环；移动端不直接接入 Supabase Auth SDK，而是统一消费这些 Backend Route Handlers。

### Android 端
1. `MainActivity` 在冷启动与 `onNewIntent` 两条路径下统一接收 deeplink，并把解析结果写入 `MainNavigationViewModel` 的 `pendingRoute`（`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt:24-53`）。
2. `NavGraph` 通过 `Scaffold(bottomBar = { NavigationBar { ... }})` 渲染 5 个 Tab，点击时使用 `popUpTo(findStartDestination()) + saveState + restoreState` 保留多 back stack 状态（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:108-146`）。
3. 首页 graph 当前承载真实 `HomeScreen` Feed、`SearchHomeScreen`、`SearchResultScreen`、`RankingScreen`、`ClassificationScreen`、`new-releases` / `actors` 占位页，以及 `menu/login`、`menu/messages`、`menu/booking`、`menu/downloads` 等菜单承接页（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:172-258`）。
4. `MainNavigationViewModel` 额外维护 `menuPanelState`、`pendingMenuRoute` 与 `pendingRoute`；菜单打开只允许发生在首页 Tab，菜单关闭后才会把首个待跳转目标转交导航层消费，避免 closing 阶段重复点击导致多次跳转（`android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt:21-116`）。
5. `MenuPanelDrawer` 通过左侧 0.78 屏宽抽屉、0.42 alpha 背景遮罩与 240ms 动画渲染 overlay，并在 `BackHandler` 中优先消费返回动作；`MenuPanelRoute` 则负责最近在看加载、静态区块交互、游戏中心 snackbar 反馈与菜单内播放器跳转（`android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelDrawer.kt:27-106`, `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelScreen.kt:31-123`）。
6. 剧场已切换为真实 `TheaterScreen`；profile graph 也不再是占位页，而是由 `ProfileScreen` / `SettingsScreen` / `LoginScreen` 组成完整账号承载链路：匿名态显示登录入口，已登录态显示用户摘要与设置入口，排行预约未登录时也会跳转到同一登录页并在成功后回跳原榜单 route。
7. `LaunchedEffect(uiState.pendingRoute)` 在导航容器 ready 后消费待执行路由，实现冷启动 deeplink 延迟跳转；其中菜单承接页、最近在看播放页、排行页与分类页都复用同一套 pending route 消费机制。

### iOS 端
1. `ShortDramaApp` 持有单例 `NavigationRouter` 并通过 `.environmentObject(router)` 注入全局导航状态（`ios/ShortDrama/Sources/App/ShortDramaApp.swift:7-23`）。
2. `AppShellView` 以 `TabView` 渲染 5 个一级频道，并在最外层 `ZStack` 中叠加 `MenuPanelContainerView`；同时在 `.task` 中先调用 `authStore.restoreIfNeeded()`、再调用 `router.markContainerReady()`；并通过 `.fullScreenCover(item: presentedLoginContext)` 承载登录页（`ios/ShortDrama/Sources/App/AppShellView.swift:7-49`）。
3. `NavigationRouter` 为每个 Tab 维护独立 `NavigationPath`，并同时维护 `menuPanelState`、`pendingMenuNavigation` 与登录上下文；菜单只允许在 home Tab 打开，关闭动画结束后才会真正执行菜单内导航，而登录成功后会根据 `returnRoute` 回到 profile 或 ranking 等目标页（`ios/ShortDrama/Sources/App/NavigationRouter.swift:7-145`）。
4. `TabNavigationHostView` 为每个 Tab 提供独立 `NavigationStack`，首页注册 `HomeView`、`SearchHomeView`、`SearchResultView`、`RankingHomeView`、`ClassificationHomeView`、`MenuPlaceholderView`、`PlayerView`、`DramaDetailView`，剧场 tab 直接渲染 `TheaterView()`，商城 tab 已切换为 `MallContainerView()`，profile tab 注册 `ProfileHomeView` 并把 `.settings` 绑定到真实 `SettingsView`，当前仅赚钱频道继续复用 `PlaceholderTabView`。
5. `HomeView` 左上角 toolbar 汉堡按钮调用 `router.openMenuPanel()`；`MenuPanelContainerView` 负责最近在看加载、登录/消息/预约/下载先关菜单再导航，以及游戏中心 `Alert` 本地反馈。
6. `ProfileHomeView` 会根据 `authStore.status` 在匿名态 / restoring / authenticated 三态间切换；匿名态可拉起登录页，已登录态可进入设置页，排行预约未登录时则由 `RankingHomeView` 通过 `RankingRouteBuilder.loginContext(for:)` 复用同一登录承载。

## 多端实现

### Web
- 源文件：`web/src/app/layout.tsx:15-33`、`web/src/app/play/[id]/page.tsx:14-39`、`web/src/app/detail/[id]/page.tsx:14-39`, `web/src/app/rankings/page.tsx:1-9`
- 首页导航入口：`web/src/features/home/HomeScreen.tsx:27-50`
- 占位页复用：`web/src/features/placeholder-route/PlaceholderRouteScreen.tsx:3-21`
- 技术：Next.js 16、React 19、TypeScript、SSR-first App Router

### Android
- 入口与容器：`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt:21-54`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:36-417`
- 路由常量：`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:9-162`
- 首页 / 菜单 / 搜索 / 排行 / 分类承载：`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:44-336`, `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelScreen.kt:31-123`, `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/components/MenuPanelComponents.kt:51-453`, `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt:57-508`, `android/app/src/main/java/com/djs66256/short_drama/feature/classification/ui/ClassificationScreen.kt:58-260`
- 认证与“我的”频道承载：`android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/ProfileScreen.kt:28-146`, `android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/SettingsScreen.kt:36-117`, `android/app/src/main/java/com/djs66256/short_drama/feature/auth/ui/LoginScreen.kt:43-177`, `android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt:29-248`, `android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthBootstrapper.kt:9-46`
- 占位频道：`android/app/src/main/java/com/djs66256/short_drama/feature/common/ui/PlaceholderScreen.kt:14-38`
- 技术：Kotlin 2.0.21、Jetpack Compose、Material3、Navigation Compose、Hilt

### iOS
- 入口与容器：`ios/ShortDrama/Sources/App/ShortDramaApp.swift:5-23`、`ios/ShortDrama/Sources/App/AppShellView.swift:3-49`
- Tab 定义：`ios/ShortDrama/Sources/App/AppTab.swift:3-41`
- 路由与状态：`ios/ShortDrama/Sources/App/AppRoute.swift:4-66`、`ios/ShortDrama/Sources/App/NavigationRouter.swift:3-145`
- 首页 / 菜单 / 搜索 / 排行 / 分类承载：`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:15-182`, `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift:3-72`, `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift:19-109`, `ios/ShortDrama/Sources/Features/Classification/Views/ClassificationHomeView.swift:18-69`
- 认证与“我的”频道承载：`ios/ShortDrama/Sources/Features/Profile/Views/ProfileHomeView.swift:3-111`, `ios/ShortDrama/Sources/Features/Profile/Views/SettingsView.swift:3-63`, `ios/ShortDrama/Sources/Features/Auth/AuthStore.swift:3-110`, `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift:3-24`
- 占位频道：`ios/ShortDrama/Sources/Features/Shell/Views/PlaceholderTabView.swift:3-27`, `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPlaceholderView.swift:3-33`
- 技术：Swift 6、SwiftUI、TabView、NavigationStack、Swift Testing

### Backend
- 服务首页：`backend/src/app/page.tsx`
- 健康检查：`backend/src/app/api/health/route.ts`
- 首页 Feed / 剧场 / 搜索 / 热搜 / 分类 tags / 排行接口：`backend/src/app/api/dramas/route.ts:8-24`、`backend/src/app/api/dramas/channel/route.ts:1-17`、`backend/src/app/api/dramas/search/route.ts:7-19`、`backend/src/app/api/dramas/hot-search/route.ts:6-11`、`backend/src/app/api/dramas/tags/route.ts:7-18`、`backend/src/app/api/dramas/rankings/route.ts:8-24`
- 认证接口：`backend/src/app/api/auth/otp-requests/route.ts:7-14`、`backend/src/app/api/auth/sessions/route.ts:7-14`、`backend/src/app/api/auth/session-refreshes/route.ts:7-14`、`backend/src/app/api/users/me/route.ts:7-13`、`backend/src/app/api/auth/session/route.ts:6-13`
- 真实 bearer 鉴权中间件：`backend/src/middleware/auth.ts:27-138`
- 预约接口：`backend/src/app/api/dramas/[id]/book/route.ts:16-28`
- 菜单最近在看与播放器历史接口：`backend/src/app/api/player/recently-viewed/route.ts:1-21`、`backend/src/app/api/player/parse-playback-session-id.ts:1-17`、`backend/src/services/player/player.service.ts:100-142`
- 播放器起播/停止接口：`backend/src/app/api/player/start/route.ts:1-47`、`backend/src/app/api/player/stop/route.ts:1-48`
- 技术：Next.js 16、TypeScript、App Router Route Handlers

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/health` | [../../api/health.md](../../api/health.md) | 服务健康检查，不受分类接入影响 |
| `GET /api/dramas` | [../../api/dramas.md](../../api/dramas.md) | 移动端首页频道的 Feed 数据源 |
| `GET /api/dramas/channel` | [../../api/dramas.md](../../api/dramas.md) | 移动端剧场频道的 Feed 数据源 |
| `GET /api/dramas/search` | [../../api/dramas.md](../../api/dramas.md) | 搜索结果页与分类标签点击后的统一结果承接接口 |
| `GET /api/dramas/hot-search` | [../../api/dramas.md](../../api/dramas.md) | 搜索发现页热搜数据源 |
| `GET /api/dramas/tags` | [../../api/dramas.md](../../api/dramas.md) | 移动端分类页标签矩阵数据源 |
| `GET /api/dramas/rankings` | [../../api/dramas.md](../../api/dramas.md) | 移动端排行页的数据源，登录态下可补充 `is_booked` |
| `POST /api/dramas/:id/book` | [../../api/dramas.md](../../api/dramas.md) | 排行预约榜的预约接口，要求真实登录态 |
| `GET /api/player/recently-viewed` | [../../api/player.md](../../api/player.md) | 首页菜单“最近在看”区块的统一数据源，Android / iOS 都透传 `X-Playback-Session-Id` |
| `POST /api/auth/otp-requests` | [../../api/auth.md](../../api/auth.md) | 登录页发送验证码 |
| `POST /api/auth/sessions` | [../../api/auth.md](../../api/auth.md) | 验证码登录 / 自动注册并创建会话 |
| `POST /api/auth/session-refreshes` | [../../api/auth.md](../../api/auth.md) | 启动恢复或 token 失效后的 refresh |
| `GET /api/users/me` | [../../api/auth.md](../../api/auth.md) | 恢复本地 session 后校验当前用户 |
| `DELETE /api/auth/session` | [../../api/auth.md](../../api/auth.md) | 设置页退出登录的后端承接 |
| `POST /api/player/start` | [../../api/player.md](../../api/player.md) | 播放页起播接口，当前仍以占位 / 历史链路为主 |
| `POST /api/player/stop` | [../../api/player.md](../../api/player.md) | 播放结束上报接口，当前仍以占位 / 历史链路为主 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Web 路由参数 | Next.js `params` | 页面级 | `play` / `detail` 页面按需读取 `id` 并在服务端先校验 | `web/src/app/play/[id]/page.tsx:14-39`, `web/src/app/detail/[id]/page.tsx:14-39` |
| Android `pendingRoute` | `MutableStateFlow<UiState>` | 应用级 | deeplink 与菜单关闭后的最终导航目标都先入队，待 `NavHost` 可消费后执行 | `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt:21-116` |
| Android `pendingMenuRoute` / `menuPanelState` | `MutableStateFlow<UiState>` | 应用级 | 菜单 opening / open / closing 状态与“先关菜单再导航”的待跳转目标 | `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt:21-116` |
| Android 多 Tab 栈 | `NavController` + `saveState/restoreState` | Tab 级 | 切换频道时保留已访问 graph 的返回栈 | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:143-146,410-416` |
| Android 首页 Feed 状态 | `MutableStateFlow<HomeUiState>` | 页面级 | 首页默认页面状态，承载 loading / list / empty / error / retrying | `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:17-31` |
| Android 菜单最近在看状态 | `MutableStateFlow<MenuPanelUiState>` + `MutableSharedFlow<MenuPanelEvent>` | 页面级 | 承载最近在看 content / empty / error / retry 与卡片点击播放事件 | `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt:23-135` |
| Android 排行状态 | `MutableStateFlow<RankingUiState>` | 页面级 | 排行页默认页面状态，承载维度切换、列表、分页、预约中态 | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:31-44,65-77` |
| Android 分类状态 | `MutableStateFlow<ClassificationUiState>` + `MutableSharedFlow<ClassificationEffect>` | 页面级 | 分类页承载 gender、选中维度、标签矩阵与左右滚动联动 | `android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt:27-35,63-67` |
| Android 认证状态 | `AuthStateHolder.authStatus` | 应用级 | 聚合 anonymous / restoring / authenticated / refreshing / expired，并驱动“我的”频道与受保护操作 | `android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthStateHolder.kt:14-59` |
| Android 登录页状态 / 回跳事件 | `LoginUiState` + `MutableSharedFlow<LoginEvent>` | 页面级 | 承载手机号、验证码、协议勾选、发送/提交中、cooldown，以及登录成功后的 returnRoute 回跳 | `android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt:29-248` |
| iOS `selectedTab` | `@Published var selectedTab` | 应用级 | 控制当前激活的一级频道 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:15-20` |
| iOS `pathsByTab` | `[AppTab: NavigationPath]` | Tab 级 | 每个 Tab 独立维护自己的导航路径 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:21-23,38-43` |
| iOS `containerReady` | `@Published private(set) var containerReady` | 应用级 | 冷启动时用于判定是否可立即执行 deeplink 跳转 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:24-27,119-127` |
| iOS `menuPanelState` / `pendingMenuNavigation` | `@Published private(set)` | 应用级 | 控制菜单 overlay 显隐，以及关闭动画结束后才执行的首个菜单导航目标 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:26-36,71-113` |
| iOS 登录弹层上下文 | `presentedLoginContext` | 应用级 | 区分 profile 入口与 ranking booking 入口，并保存 return route | `ios/ShortDrama/Sources/App/NavigationRouter.swift:13,62-95` |
| iOS 认证状态 | `AuthStore.status` + `currentUser` | 应用级 | 聚合匿名/恢复中/已登录/refreshing/expired，并驱动 profile 与登录弹层后的回跳语义 | `ios/ShortDrama/Sources/Features/Auth/AuthStore.swift:4-109` |
| iOS 首页 Feed 状态 | `@Published private(set) var viewState` | 页面级 | 首页默认页面状态，承载 loading / content / empty / error | `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:7-23` |
| iOS 菜单最近在看状态 | `@Published private(set) var viewState` + `@Published private(set) var isRetrying` | 页面级 | 承载最近在看 idle / loading / content / empty / error 与重试态 | `ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift:4-104` |
| iOS 排行状态 | `@Published` 属性集 | 页面级 | 承载排行页维度选择、内容态、分页与登录拦截 | `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:23-30` |
| iOS 分类状态 | `@Published` 属性集 | 页面级 | 承载 gender、选中维度、分类内容态与 scroll reset | `ios/ShortDrama/Sources/Features/Classification/ViewModels/ClassificationViewModel.swift:17-31` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 深链 | 共享导航容器 | deeplink 解析后的目标需要由 App Shell 负责承载和跳转 |
| 搜索发现 | 首页子路由 | 排行和分类都是搜索发现链路的一部分，继续挂在首页频道内 |
| 菜单面板 | 首页 overlay | 首页左上角汉堡菜单打开的左侧抽屉面板由应用壳统一承载，并复用首页导航栈 |
| 认证体系 | 登录页承载与登录后回跳 | “我的”频道、排行预约拦截与启动恢复都依赖应用壳提供登录 route / fullScreenCover、设置页和 returnRoute 导航承接 |
| 播放器 | 首页子路由 | `play/:id` 当前由应用壳负责注册与参数透传，最近在看卡片也复用同一路由 |
| 剧集详情 | 首页子路由 | `detail/:id` 当前由应用壳负责注册与参数透传 |
| 首页信息流 | 一级频道默认内容 | 首页是默认激活 Tab，并已从占位页演进为 Feed 首屏 |

### 外部依赖

| 服务 / 框架 | 用途 | 接入方式 |
|-------------|------|---------|
| Next.js App Router | Web/Backend 路由承载 | 文件系统路由 + Route Handlers |
| Navigation Compose | Android 多级导航与状态恢复 | `NavHost` + nested navigation graph |
| SwiftUI `TabView` / `NavigationStack` | iOS 一级频道与子路由承载 | 声明式导航容器 |
| `GET /api/dramas` | 移动端首页数据加载 | Backend Route Handler + 各端网络层调用 |
| `GET /api/dramas/channel` | 移动端剧场 feed 加载 | Backend Route Handler + 各端网络层调用 |
| `GET /api/player/recently-viewed` | 菜单最近在看加载 | Backend Route Handler + 各端网络层调用，统一透传 `X-Playback-Session-Id` |
| `GET /api/dramas/search` / `GET /api/dramas/hot-search` | 搜索发现与分类结果承接 | Backend Route Handler + 各端网络层调用 |
| `GET /api/dramas/tags` | 移动端分类页标签矩阵加载 | Backend Route Handler + 各端网络层调用 |
| `POST /api/auth/otp-requests` / `POST /api/auth/sessions` / `POST /api/auth/session-refreshes` / `GET /api/users/me` / `DELETE /api/auth/session` | 移动端登录、恢复、刷新与登出 | Backend Route Handler + Android/iOS 统一 REST 调用 |
| `GET /api/dramas/rankings` / `POST /api/dramas/:id/book` | 移动端排行与预约 | Backend Route Handler + 各端网络层调用 |

## 已知限制

- Android / iOS 的“我的”频道已接入真实登录 / 设置承载，商城与赚钱频道也都已接入真实 H5 容器。
- Web 端当前只补齐用户端路由承接，没有实现移动端同等的底部导航 UI，也没有实现首页汉堡菜单、剧场频道或独立用户端登录页。
- 商城（mall）与赚钱（earn）都保持 H5 承载策略，并已由移动端容器接入；但对应 H5 页面仍不是完整线上业务，只覆盖当前 PRD 所需范围（`PRODUCT.md:22-25`）。
- 菜单中的登录、消息、预约、下载仍是 Native 占位承接页；游戏中心仅提供“即将上线”本地反馈，不导航到真实页面。
- 播放页与详情页仍是占位实现，仅展示路由参数，不含真实业务数据或播放能力。
- Backend 最近在看、播放进度与首页内容当前仍主要来自 mock repository / in-memory history，不是线上内容服务或持久化用户体系；但排行 / 预约与 auth 已切到真实 Supabase 路径。
- iOS 排行登录成功后只回到 `.rankingHome`，不显式恢复更细粒度的 `contentType/rankingType` query；Android 会保留完整 `ranking?...` returnRoute。
- 设备级黑盒验证在本轮 workflow 中按规范降级，仅产出 QA 文档，当前证据主要来自自动化测试、QA 文档与代码审查（`docs/specs/2026-07-28-prd-08-login/qa-test.md:1-40`）。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 更新：同步 PRD-14 赚钱中心落地结果，补充赚钱 tab 已接入真实 H5 容器、Native 登录 / 任务承接与 host message 回流，并与 PRD-13 商城容器现状一并对齐，修正文档中过时的 placeholder 描述 |
| 2026-07-29 | 更新：同步 PRD-08 登录闭环后“我的”频道已接入真实登录 / 设置 / 退出登录承载，补充移动端登录入口与回跳、Backend 认证接口、排行预约登录拦截，以及 Web 仍无用户端登录页的现状 |
| 2026-07-28 | 更新：同步 PRD-12 剧场频道落地结果，补充剧场一级 tab、`GET /api/dramas/channel`、剧场到首页拥有页面的跨 tab 复用策略，并修正文档中剧场仍为占位页的过时描述 |
| 2026-07-28 | 更新：同步 PRD-07 菜单面板落地结果，补充首页左上角汉堡菜单触发的抽屉 overlay、菜单承接页路由、最近在看状态与关闭后导航时序，并明确 Web 不涉及本期菜单面板 |
| 2026-07-27 | 更新：同步 PRD-06 后首页频道进一步承载真实分类页，补充 Backend 搜索/热搜/分类 tags 接口、移动端 classification 子路由与 Web 仍无真实分类页的现状 |
| 2026-07-27 | 更新：同步 PRD-05 后首页频道承载搜索发现与真实排行页，补充 Backend 排行/预约接口、移动端排行子路由与 Web 仍为占位页的现状 |
| 2026-07-26 | 更新：同步 PRD-02 后首页频道从占位页演进为 Native Feed 首屏，补充 Backend `GET /api/dramas` 作为首页容器依赖的数据源，并修正文档中的首页承载现状 |
| 2026-07-25 | 更新：移动端应用壳从单页骨架演进为 5 Tab 导航容器，Web 补齐路由骨架，并同步修正文档中的入口、路由、状态管理与限制说明 |

---
*本文档由 llm-wiki skill 自动维护。*
