# 播放器

> 最后更新：2026-07-29
> 覆盖端：Web / Android / iOS / Backend

## 功能概述

播放器当前已具备完整的 Backend 契约与移动端播放页，但在首页发现链路之外，PRD-07 把菜单面板里的“最近在看”纳入播放器入口之一，PRD-12 又把剧场频道卡片点击纳入同一主路径；PRD-14 进一步把赚钱中心代表性任务也接入同一原生播放器承接层。Android / iOS 都会通过 `GET /api/player/recently-viewed` 拉取当前匿名播放会话的最近历史，剧场 feed / 首页 feed / 排行列表 / 菜单最近在看 / 赚钱任务五类入口点击后都继续复用既有播放器主路径，只是赚钱任务会额外挂上 `taskId/source=earn/returnTarget=/earn` 上下文，并在自然播放结束时回流结果。Web 端仍没有对应菜单或剧场入口，赚钱页也不在 H5 内直接播放任务（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:420-566`, `ios/ShortDrama/Sources/App/TabNavigationHostView.swift:67-81`, `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:88-126`）。

- 核心价值：统一承载首页 Feed、剧场卡片、排行列表、菜单最近在看与赚钱任务五类入口，确保多入口都复用同一条播放器主路径与播放历史契约
- 覆盖范围：Web、Android、iOS、Backend
- 当前状态：Android / iOS 已接入首页信息流、剧场频道、排行列表、菜单最近在看和赚钱任务五类播放器入口；Backend 已实现 `progress/start/stop/recently-viewed` 契约；Web 不涉及本期菜单与剧场入口，赚钱页只负责发起 Native 播放承接

## 入口与路由

| 端 | 入口 | 路由 / deeplink | 源文件 |
|----|------|----------------|--------|
| Web | 首页代表性链接；赚钱页任务 CTA 发起 Native 承接 | `/play/[id]`；`earn.openTaskPlayer` bridge message | `web/src/app/play/[id]/page.tsx:14-39`, `web/src/features/home/HomeScreen.tsx:36-38`, `web/src/features/earn/hooks/useEarnPage.ts:381-399`, `web/src/features/earn/bridge/earn-bridge.ts:60-72` |
| Android | 首页 Feed 卡片“播放”按钮、排行卡片点击、菜单最近在看卡片、赚钱任务、deeplink | `play/{videoId}`（兼容 `player/{videoId}`）、`earn/play?taskId=...&source=earn&returnTarget=/earn&videoId=...`、`djsdrama://play/{id}`、`djsdrama://player/{id}` | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:67-70,251-257`, `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelScreen.kt:44-73`, `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt:70-76`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:221-265,420-566`, `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:44-57,102-169`, `android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt:33-36` |
| iOS | 首页 Feed 卡片“观看”按钮、排行卡片点击、菜单最近在看卡片、赚钱任务、deeplink | `play` public route name、`.earnPlayer(context:)`、`djsdrama://play/{id}` | `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:41-67`, `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift:20-45`, `ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift:86-89`, `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift:3-8`, `ios/ShortDrama/Sources/App/AppRoute.swift:23-32,42-64`, `ios/ShortDrama/Sources/App/NavigationRouter.swift:274-283`, `ios/ShortDrama/Sources/App/DeeplinkHandler.swift:26-45` |
| Backend | 菜单最近在看接口 + 播放历史接口 | `GET /api/player/progress`, `GET /api/player/recently-viewed`, `POST /api/player/start`, `POST /api/player/stop` | `backend/src/app/api/player/progress/route.ts:1-45`, `backend/src/app/api/player/recently-viewed/route.ts:1-21`, `backend/src/app/api/player/start/route.ts:1-47`, `backend/src/app/api/player/stop/route.ts:1-48` |

## 核心逻辑

### 流程：从首页信息流、排行列表、菜单最近在看或赚钱任务进入播放页

1. 用户从首页 Feed、排行列表、菜单最近在看、赚钱任务或 deeplink 进入播放页。
   - Web：首页仍只有代表性链接 `/play/sample`；赚钱页已登录任务点击时不会在 H5 内直接播放，而是发出 `earn.openTaskPlayer` 请求 Native 宿主承接（`web/src/features/home/HomeScreen.tsx:36-38`, `web/src/features/earn/hooks/useEarnPage.ts:381-399`）。
   - Android：首页 `HomeScreen` 卡片点击时调用 `onOpenPlay(drama.id)`；排行页 `RankingScreen` 点击列表项时同样调用 `onOpenPlay(item.id)`；菜单最近在看由 `MenuPanelViewModel` 发出 `OpenPlayback(dramaId)` 事件，再映射为 `PendingRoute.Play(dramaId)`；赚钱页则通过 `AppDestination.earnPlay(...)` 把 `taskId/source/returnTarget/videoId` 一并透传给原生承接路由（`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:251-257`, `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt:221-225`, `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelScreen.kt:44-73`, `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt:70-76`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:427-435`）。
   - iOS：首页 `HomeRouteBuilder.playerRoute(for:)` 与排行页 `RankingRouteBuilder.playRoute(for:)` 都把 `drama.id` 映射到 `.player(videoId:)`；菜单最近在看通过 `MenuPanelViewModel.route(for:)` 把 `RecentlyViewedItem.dramaId` 映射到同一条 `.player(videoId:)` 路由；赚钱页则通过 `router.openPlayerFromEarn(_:)` 导航到 `.earnPlayer(context:)`（`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:65-79`, `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift:3-8`, `ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift:86-89`, `ios/ShortDrama/Sources/App/NavigationRouter.swift:274-283`）。
2. 菜单入口会先关闭抽屉，再由壳层执行真正的播放导航；赚钱任务入口则直接从 earn tab 内打开原生播放器承接页，不经过首页菜单状态机。
   - Android：`closeMenuThenNavigate()` 把首个待跳转目标写入 `pendingMenuRoute`，等待 `onMenuClosedAnimationFinished()` 后再转移给 `pendingRoute` 消费（`android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt:86-116`）。
   - iOS：`closeMenuPanelThenNavigate(to:)` 先把目标写入 `pendingMenuNavigation` 并切到 `.closing`，`markMenuPanelDidClose()` 才真正调用 `navigate(to:)`（`ios/ShortDrama/Sources/App/NavigationRouter.swift:129-155`）。
3. 最近在看列表由 Backend 统一返回，移动端只消费当前匿名播放会话的数据。
   - Backend：`GET /api/player/recently-viewed` 复用 `X-Playback-Session-Id`，先取最近候选窗口，再过滤缺 drama / 缺 episode / drama-episode 不匹配的脏数据，最终最多返回 3 条，不承诺用更老 offset 补足（`backend/src/app/api/player/recently-viewed/route.ts:11-20`, `backend/src/app/api/player/parse-playback-session-id.ts:5-16`, `backend/src/services/player/player.service.ts:100-142`, `backend/src/lib/player.ts:1-2`）。
   - Android：`ApiService.getRecentlyViewed()` 注入同名 header，`MenuPanelViewModel` 使用 `PlaybackSessionStore` 取会话 ID，并将接口结果再 `take(3)` 映射为 content / empty / error 状态（`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt:84-87`, `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt:79-129`, `android/app/src/main/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntries.kt:50-52`）。
   - iOS：`PlayerRemoteDataSource` 同样用 `X-Playback-Session-Id` 请求 `/api/player/recently-viewed`，`MenuPanelViewModel` 根据返回 items 切换 `content / empty / error`，并在点击时校验 `dramaId` 非空（`ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift:23-26,85-95`, `ios/ShortDrama/Sources/Data/Repositories/MenuPanelRepository.swift:10-13`, `ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift:34-89`, `ios/ShortDrama/Sources/Domain/Entities/RecentlyViewedItem.swift:11-19`）。
4. 路由层读取 `videoId` 并交给播放页 View / ViewModel；赚钱任务入口只是在同一播放器主路径上额外挂载 earn 上下文，不创建 H5 内播放器实现。
   - Android：`PlayerViewModel` 从 `SavedStateHandle` 读取 `videoId`，必要时回退到通用 `id` key；earn 承接路由单独负责在完成或退出时回传 `EarnTaskPlayerResult`（`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:14-17`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:494-566`）。
   - iOS：`TabNavigationHostView` 在路由命中 `.player(let videoId)` 时构造普通 `PlayerViewModel(videoId:)`，命中 `.earnPlayer(let context)` 时则构造带 `earnTaskContext` 的同一 ViewModel 类型（`ios/ShortDrama/Sources/App/TabNavigationHostView.swift:31-35,67-81`）。
5. 播放历史仍由 `progress/start/stop` 与播放页本身维护；菜单最近在看与赚钱任务都只是额外入口，不引入新的播放器页面实现或新的 player API。

### 边界与异常处理

| 场景 | 处理方式 | 源文件 |
|------|---------|--------|
| Web 路由参数为空或全空白 | `trim()` 后为空即 `notFound()`，不渲染有效播放页 | `web/src/app/play/[id]/page.tsx:9-39` |
| Android deeplink 使用历史 `player` host | 解析后统一映射到 `PendingRoute.Play(videoId)` | `android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt:33-36` |
| Android 页面参数 key 不一致 | `PlayerViewModel` 优先读 `videoId`，再回退通用 `id` | `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:14-17` |
| Android 赚钱任务上下文非法 | earn 承接路由直接产出 `completed=false` 的错误结果并返回赚金币页，不进入有效播放完成链路 | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:514-525` |
| iOS 冷启动即收到播放 deeplink | 先写入 `pendingRoute`，待 `TabView` ready 后再导航 | `ios/ShortDrama/Sources/App/ShortDramaApp.swift:13-20`, `ios/ShortDrama/Sources/App/NavigationRouter.swift:39-50` |
| iOS earn 播放被返回/消失/后台/错误打断 | `PlayerViewModel.finishEarnFlowIfNeeded()` 只上报 `completed=false`，不会伪造已完成结果 | `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:88-134` |
| 首页 / 排行卡片 `id` 为空 | Android 不触发播放导航；iOS `HomeRouteBuilder` / `RankingRouteBuilder` 返回 `nil` | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:158-170,209-218`, `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt:214-217`, `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift:5-8` |
| 最近在看 header 缺失或非法 | Backend 直接返回 `INVALID_PLAYBACK_SESSION` 400 | `backend/src/app/api/player/parse-playback-session-id.ts:5-16`, `backend/src/app/api/__tests__/player.recently-viewed.test.ts:135-155` |
| 最近在看候选记录含脏数据 | 服务端过滤无效 drama / episode 或不匹配关系，允许返回不足 3 条 | `backend/src/services/player/player.service.ts:106-129`, `backend/src/services/player/player.service.test.ts:86-171` |
| 菜单关闭阶段重复点击其它入口 | Android / iOS 都只保留首个待导航目标，避免多次跳转 | `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt:86-116`, `android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt:100-130`, `ios/ShortDrama/Sources/App/NavigationRouter.swift:129-155`, `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift:294-308` |

## 多端实现

### Web

- Page 层：`web/src/app/play/[id]/page.tsx:14-39`
- Feature 层：`web/src/features/player/PlayerScreen.tsx:7-25`
- 首页入口：`web/src/features/home/HomeScreen.tsx:36-38`
- 赚钱任务入口：`web/src/features/earn/hooks/useEarnPage.ts:381-399`, `web/src/features/earn/bridge/earn-bridge.ts:60-72`
- 特点：Server Component 先做参数规范化，再委托占位 Feature 渲染；赚钱页任务只发起 Native 播放承接，不在 H5 内嵌播放器

### Android

- 路由定义：`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:44-50,94-109,158-169`
- 导航注册：`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:187-243,475-568`
- 首页 Feed 入口：`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:177-232`
- 排行入口：`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt:193-243`
- 赚钱任务入口：`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:427-435`
- 页面实现：`android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt:14-34`
- 参数读取：`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:14-17`
- 特点：同时兼容 canonical `play` 与 legacy `player` route / deeplink；赚钱任务继续复用同一原生播放器，只是在路由侧额外挂载 earn 上下文与完成回传

### iOS

- 路由定义：`ios/ShortDrama/Sources/App/AppRoute.swift:23-32,39-60`
- 导航注册：`ios/ShortDrama/Sources/App/TabNavigationHostView.swift:11-35,67-81`
- 首页 Feed 入口：`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:73-224`
- 排行入口：`ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift:69-77`, `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift:3-8`
- 赚钱任务入口：`ios/ShortDrama/Sources/App/NavigationRouter.swift:274-283`
- 页面实现：`ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift:4-18`
- 参数承载：`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:4-11,88-134`
- 特点：普通播放页属于首页 Tab 的子路由，对外公开名为 `play`；赚钱任务则通过 `.earnPlayer(context:)` 复用同一 `PlayerViewModel`，并由 ViewModel 统一发出 completed / non-completed 的 earn 结果

### Backend

- 路由文件：`backend/src/app/api/player/progress/route.ts:1-45`、`backend/src/app/api/player/recently-viewed/route.ts:1-21`、`backend/src/app/api/player/start/route.ts:1-47`、`backend/src/app/api/player/stop/route.ts:1-48`
- Service 与 header 解析：`backend/src/services/player/player.service.ts:25-142`、`backend/src/app/api/player/parse-playback-session-id.ts:1-17`
- 当前行为：`progress/start/stop` 维护匿名播放历史，`recently-viewed` 在固定候选窗口内过滤脏数据后最多返回 3 条
- 特点：播放器历史链路已真实落地，菜单最近在看只是新增入口，不引入新的播放器接口语义

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/player/progress` | [../../api/player.md](../../api/player.md) | 播放页 bootstrap 的续播查询接口 |
| `GET /api/player/recently-viewed` | [../../api/player.md](../../api/player.md) | 菜单最近在看数据源，Android / iOS 都复用该接口 |
| `POST /api/player/start` | [../../api/player.md](../../api/player.md) | 开始播放接口，菜单最近在看点击后仍进入同一播放器起播链路 |
| `POST /api/player/stop` | [../../api/player.md](../../api/player.md) | 停止播放 / 保存历史接口，最近在看候选记录来源于该接口持久化结果 |
| `GET /api/dramas` | [../../api/dramas.md](../../api/dramas.md) | 首页 Feed 提供进入播放页所需的 `drama.id` 与卡片数据 |
| `GET /api/dramas/rankings` | [../../api/dramas.md](../../api/dramas.md) | 排行页提供进入播放页所需的 `drama.id` 与榜单字段 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Web `videoId` | 路由 `params` | 页面级 | 页面渲染时由 App Router 提供，先做非空校验 | `web/src/app/play/[id]/page.tsx:14-39` |
| Web earn task context | bridge payload | 页面级 | `/earn` 只把 `taskId/source/returnTarget/videoId` 发给宿主，不在 H5 内保留播放器实例 | `web/src/features/earn/hooks/useEarnPage.ts:381-399`, `web/src/lib/schemas.ts:243-254` |
| Android `videoId` | `SavedStateHandle` | 页面级 | 从导航参数恢复，兼容旧 key | `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:14-17` |
| Android `pendingRoute` | `StateFlow<UiState>` | 应用级 | deeplink 与菜单关闭后的播放目标都先缓存，稍后再导航 | `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt:21-116` |
| Android earn task result | `latestEarnTaskPlayerResult` + `earnTaskPlayerResultSignal` | graph 级 | 赚钱任务播放结束后，把完成/退出结果回传给 earn 容器，由其决定是否触发奖励结算 | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:458-566` |
| Android 菜单最近在看状态 | `StateFlow<MenuPanelUiState>` | 页面级 | 承载最近在看 items、loading、error、empty 与 retrying 状态 | `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt:23-129` |
| iOS `videoId` | `PlayerViewModel` 初始化参数 | 页面级 | 由 `AppRoute.player(videoId:)` 直接传入 | `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:5-10` |
| iOS earn task context | `PlayerViewModel.earnTaskContext` | 页面级 | 赚钱任务会把 `taskId/videoId/source` 一并注入播放器 ViewModel，用于结果回传 | `ios/ShortDrama/Sources/App/TabNavigationHostView.swift:67-81`, `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:4-19` |
| iOS `pendingRoute` | `NavigationRouter.pendingRoute` | 应用级 | 冷启动 deeplink 场景的播放目标暂存 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:24-27,167-179` |
| iOS earn task result | `NavigationRouter.pendingEarnTaskPlayerResult` | 应用级 | earn 播放器关闭后暂存回传结果，再由 earn 容器转换为 host message | `ios/ShortDrama/Sources/App/NavigationRouter.swift:45-47,278-293` |
| iOS 菜单最近在看状态 | `MenuPanelViewModel.viewState` | 页面级 | 承载 recently viewed 的 idle / loading / content / empty / error 状态与重试中标记 | `ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift:4-84` |
| 当前播放会话 ID | `PlaybackSessionStore` / `KeychainPlaybackSessionStore` | 应用级 | 最近在看、progress、start、stop 统一复用同一个 `X-Playback-Session-Id` | `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt:40-42,91-93`, `ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift:16-18,51-52`, `backend/src/app/api/player/parse-playback-session-id.ts:5-16` |
| 首页 Feed / 排行 / 菜单最近在看 `drama.id` | 列表项字段 | 页面级 | 作为移动端播放路由的统一参数来源 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:251-257`, `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt:221-225`, `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt:70-76`, `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:65-79`, `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift:5-8`, `ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift:86-89` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 应用壳 | 路由承载 | 播放页依附于移动端应用壳与 Web App Router 的路由骨架，菜单入口也由壳层统一关闭后导航 |
| 深链 | 外部入口 | Android/iOS 可通过 deeplink 直接落到播放页 |
| 首页信息流 | 导航入口 | 移动端真实入口之一来自首页 Feed 卡片 |
| 排行体系 | 导航入口 | 移动端另一条真实入口来自排行列表卡片 |
| 菜单面板 | 导航入口 | Android / iOS 菜单中的最近在看卡片会复用播放器主路径 |
| 赚钱中心 | 导航入口 + 完成回流 | earn H5 不内嵌播放器，而是通过 Native 承接页复用同一播放器，再把 completed / non-completed 结果回流给赚钱容器 |

### 外部依赖

| 服务 | 用途 | 接入方式 |
|------|------|---------|
| Backend Dramas API | 提供首页卡片与排行项中的 `drama.id` | `GET /api/dramas`, `GET /api/dramas/rankings` |
| Backend Player API | 提供续播、最近在看、起播与停止上报 | `GET /api/player/progress`, `GET /api/player/recently-viewed`, `POST /api/player/start`, `POST /api/player/stop` |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| Web 无菜单最近在看入口 | 无法验证 Web 端与移动端一致的菜单到播放页链路 | 2026-07-28 | Web 本期不涉及菜单面板，首页仍只有代表性播放链接 |
| Web 赚钱页不在 H5 内直接播放任务 | 浏览器环境无法独立验证原生任务播放完成链路 | 2026-07-29 | 赚钱任务必须依赖 Native 宿主打开原生播放器 |
| 菜单最近在看最多只返回 3 条且允许不足 3 条 | 过滤脏数据后可能看到 0-2 条，不承诺继续向后补足 | 2026-07-28 | `RECENTLY_VIEWED_FETCH_LIMIT=10` 只定义候选窗口，不是 offset 补足承诺 |
| 赚钱任务只有自然播放结束才会产出 `completed=true` | 返回、后台、容器销毁或错误退出都不会结算奖励 | 2026-07-29 | 这是 PRD-14 当前实现的显式业务边界 |
| 登录 / 消息 / 预约 / 下载仍是占位承接 | 菜单中的这些入口暂不触发真实业务流，只能验证先关菜单再导航 | 2026-07-28 | Android / iOS 都跳转 Native placeholder |
| 游戏中心仅提供本地反馈 | 菜单内游戏入口不会进入真实播放或其他业务页面 | 2026-07-28 | Android snackbar / iOS alert 均显示“即将上线” |
| 设备级黑盒仍待补测 | 无法确认真实菜单开合、连点与最近在看点击后的设备表现，以及赚钱任务在真机上的完成回流体验 | 2026-07-29 | `docs/specs/2026-07-29-prd-14-earn/qa-test.md:1-601` |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 更新：同步 PRD-14 赚钱中心落地结果，补充 earn H5 通过 Native 承接页复用原生播放器、`.earnPlayer(context:)` / `earn/play?...` 路由、自然播放结束才上报 completed，以及任务完成结果回流给赚钱容器的语义 |
| 2026-07-28 | 更新：同步 PRD-12 剧场频道落地结果，补充剧场卡片点击也复用 canonical `play` 主路径，并明确首页、剧场、排行与菜单最近在看四类入口共用播放器导航语义 |
| 2026-07-28 | 更新：同步 PRD-07 菜单面板落地结果，补充最近在看接口、菜单卡片到 `play` 路由的复用链路、关闭后导航时序与 Web 范围边界 |
| 2026-07-27 | 更新：补充 PRD-05 后播放页真实入口新增排行列表卡片，记录 `drama.id` 映射到 `play/:id` 在首页 Feed 与排行体系中的共用导航链路 |
| 2026-07-26 | 更新：补充 PRD-02 后播放页真实入口已切换为首页信息流卡片，记录 `drama.id` 映射到 `play/:id` 的移动端导航链路，并保留 Backend 仍为 501 的事实 |
| 2026-07-25 | 更新：依据现有代码将播放器文档从“已设计但未初始化”修正为“跨端路由占位已落地、后端接口仍为 501”，并补充各端真实入口与参数透传链路 |
| 2026-07-22 | API 路径重命名：`/api/video/play` → `/api/player/start`，新增 `/api/player/stop`，API 定义移至 `wiki/api/player.md` |

---
*本文档由 llm-wiki skill 自动维护，从代码中提取。如有不一致，以代码为准。*