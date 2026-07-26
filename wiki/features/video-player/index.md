# 播放器

> 最后更新：2026-07-26
> 覆盖端：Web / Android / iOS / Backend

## 功能概述

PRD-03 之后，播放器已经从“只展示路由参数的占位页”演进为以移动端 Native 为主的首版完整观看链路。Backend 已实现并测试 `GET /api/player/progress`、`GET /api/dramas/:id/episodes`、`POST /api/player/start`、`POST /api/player/stop` 四个接口；iOS 已接入基于 `AVPlayer` 的真实播放视图；Android 已接入真实播放器页面、状态机与后端契约，但实际视频宿主仍是 placeholder host，尚未引入 `androidx.media3`，也没有看到系统级沉浸式 bars hiding 代码；Web 播放器继续保持占位页，不在本期真播范围内（`backend/src/app/api/player/progress/route.ts:1-45`、`backend/src/app/api/dramas/[id]/episodes/route.ts:1-20`、`backend/src/app/api/player/start/route.ts:1-47`、`backend/src/app/api/player/stop/route.ts:1-48`、`backend/src/app/api/__tests__/player.progress.test.ts:16-115`、`backend/src/app/api/__tests__/drama-episodes.test.ts:14-103`、`backend/src/app/api/__tests__/player.start.test.ts:11-120`、`backend/src/app/api/__tests__/player.stop.test.ts:13-143`、`ios/ShortDrama/Sources/Features/Player/Views/Components/NativeVideoPlayerView.swift:4-55`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/player/PlaceholderPlayerHost.kt:19-75`、`web/src/features/player/PlayerScreen.tsx:3-25`）。

- 核心价值：打通“首页 Feed / deeplink -> 续播查询 -> 剧集列表 -> 起播 -> 切集 / 倍速 -> 退出上报”的首版观看闭环（`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:157-296`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:188-378`、`backend/src/services/player/player.service.ts:18-150`）。
- 覆盖范围：Backend、Android、iOS 是本期主范围；Web 仍只保留路由占位（`web/src/app/play/[id]/page.tsx:30-39`、`web/src/features/player/PlayerScreen.tsx:7-20`）。
- 当前状态：iOS 已具备真实视频播放；Android 已具备播放器页面壳、状态机和后端接线，但不能写成“已完成真实原生视频播放”；`mall` / `earn` 在 `PRODUCT.md` 中仍是 H5 承载策略，但当前 Android / iOS 代码仍只渲染占位频道页，不是已落地的 H5 容器（`ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift:54-138`、`ios/ShortDrama/Sources/Features/Player/Views/Components/NativeVideoPlayerView.swift:12-54`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt:97-179`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/player/PlaceholderPlayerHost.kt:39-75`、`PRODUCT.md:22-25`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:188-208`、`ios/ShortDrama/Sources/Features/Shell/Views/PlaceholderTabView.swift:6-22`）。

## 入口与路由

| 端 | 入口 | 路由 / deeplink | 当前语义 | 源文件 |
|----|------|----------------|---------|--------|
| Web | 首页代表性链接 | `/play/[id]` | 仍只渲染 `videoId` 占位页 | `web/src/features/home/HomeScreen.tsx:27-51`、`web/src/app/play/[id]/page.tsx:30-39`、`web/src/features/player/PlayerScreen.tsx:7-20` |
| Android | 首页 Feed 卡片、deeplink | canonical `play/{videoId}`，兼容 alias `player/{videoId}`；`djsdrama://play/{id}`、`djsdrama://player/{id}` | 统一以 `play` 为主路径，保留 `player` 兼容 | `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:29-80`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:123-157`、`android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt:28-45`、`android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt:13-34`、`android/app/src/test/java/com/djs66256/short_drama/navigation/DeeplinkRouteParserTest.kt:9-49` |
| iOS | 首页 Feed 卡片、deeplink | public route name `play`；`djsdrama://play/{id}` | 统一以 `play` 为公开语义，不保留 `player` host | `ios/ShortDrama/Sources/App/AppRoute.swift:19-28`、`ios/ShortDrama/Sources/App/DeeplinkHandler.swift:23-40`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift:9-19` |
| Backend | N/A | `GET /api/player/progress`、`GET /api/dramas/:id/episodes`、`POST /api/player/start`、`POST /api/player/stop` | 已实现并具备自动化测试 | `backend/src/app/api/player/progress/route.ts:26-45`、`backend/src/app/api/dramas/[id]/episodes/route.ts:13-20`、`backend/src/app/api/player/start/route.ts:26-47`、`backend/src/app/api/player/stop/route.ts:26-48`、`backend/src/app/api/__tests__/player.progress.test.ts:16-115`、`backend/src/app/api/__tests__/drama-episodes.test.ts:14-103`、`backend/src/app/api/__tests__/player.start.test.ts:11-120`、`backend/src/app/api/__tests__/player.stop.test.ts:13-143` |

## 核心逻辑

### 流程：移动端播放器 bootstrap

1. 客户端先准备匿名 `playback session id`。
   - iOS 使用 `KeychainPlaybackSessionStore` 从 keychain 读取或生成 UUID（`ios/ShortDrama/Sources/Core/Storage/PlaybackSessionStore.swift:75-109`、`ios/ShortDrama/Tests/DomainTests/PlaybackSessionStoreTests.swift:22-46`）。
   - Android 使用 `DataStorePlaybackSessionStore` 持久化 UUID（`android/app/src/main/java/com/djs66256/short_drama/core/storage/PlaybackSessionStore.kt:16-41`、`android/app/src/test/java/com/djs66256/short_drama/core/storage/PlaybackSessionStoreTest.kt:12-31`）。
2. 客户端固定按 `progress -> episodes -> start` 顺序初始化。
   - iOS `performBootstrap()` 先查 progress，再拉 episodes，最后起播目标集（`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:166-193`）；测试明确锁定该顺序（`ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift:42-73`）。
   - Android `bootstrap()` 顺序一致（`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:199-297`）；测试同样校验固定顺序（`android/app/src/test/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModelTest.kt:52-82`、`android/app/src/test/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModelTest.kt:278-283`）。
3. `X-Playback-Session-Id` 只用于 `progress/start/stop`，不用于 `episodes`。
   - Backend 仅在 `progress/start/stop` route 中解析并校验该 header，`episodes` route 不读取它（`backend/src/app/api/player/progress/route.ts:12-24`、`backend/src/app/api/player/start/route.ts:12-24`、`backend/src/app/api/player/stop/route.ts:12-24`、`backend/src/app/api/dramas/[id]/episodes/route.ts:13-20`）。
   - iOS `PlayerRemoteDataSource` 只给 `GetProgress`、`StartPlayback`、`StopPlayback` 设置 header，`GetDramaEpisodes` 没有 `headers` 字段；对应测试也验证了 `episodes` 不带该 header（`ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift:10-107`、`ios/ShortDrama/Tests/DataTests/APIClientTests.swift:81-156`、`ios/ShortDrama/Tests/DataTests/PlayerRemoteDataSourceTests.swift:13-192`）。
   - Android `ApiService` 也只在 `getPlaybackProgress`、`startPlayback`、`stopPlayback` 上声明 `@Header("X-Playback-Session-Id")`，`getDramaEpisodes` 没有该参数；数据层测试已覆盖这一点（`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt:39-63`、`android/app/src/test/java/com/djs66256/short_drama/data/datasource/PlayerRemoteDataSourceTest.kt:29-143`、`android/app/src/test/java/com/djs66256/short_drama/data/repository/PlayerRepositoryImplTest.kt:33-144`）。
4. Backend 负责返回续播状态、剧集列表、起播确认和停播结果。
   - `getPlaybackProgress()` 在无历史或历史 episode 失效时统一返回 `has_history=false`（`backend/src/services/player/player.service.ts:25-72`、`backend/src/services/player/player.service.test.ts:27-65`）。
   - `GET /api/dramas/:id/episodes` 返回按 `episode_number` 升序排列的列表（`backend/src/app/api/dramas/[id]/episodes/route.ts:13-20`、`backend/src/app/api/__tests__/drama-episodes.test.ts:19-68`）。
   - `startPlayback()` 校验 drama、episode 和资源可播放性后返回 receipt（`backend/src/services/player/player.service.ts:74-95`、`backend/src/app/api/__tests__/player.start.test.ts:16-120`）。
   - `stopPlayback()` 会把进度 clamp 到 `[0, duration]` 后再 upsert 最近历史（`backend/src/services/player/player.service.ts:97-127`、`backend/src/app/api/__tests__/player.stop.test.ts:18-143`）。
5. 客户端根据 progress 和 episodes 解析目标集。
   - iOS 若历史集仍可播放则恢复，否则回退到第一条可播放 Episode；若没有可播集则进入 `.noResource`（`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:183-193`、`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:219-230`、`ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift:75-126`）。
   - Android 采用相同策略，进入 `NO_RESOURCE` 前会先保留已拿到的 episode 列表与 `seriesStatus`（`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:224-247`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:336-350`、`android/app/src/test/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModelTest.kt:84-167`）。

### 流程：切集、倍速与退出上报

1. 切集时先 best-effort stop 当前集，再从 `progress=0` start 目标集。
   - iOS `performEpisodeSwitch()` 先 `stopPlaybackIfNeeded(bestEffort: true)` 再 `startPlayback(...)`（`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:203-217`）；测试确认先 stop 再 start（`ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift:128-158`）。
   - Android `switchEpisode()` 先 `stopEpisodeBestEffort(previousEpisode)` 再执行 `startPlaybackUseCase(...)`（`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:123-185`）；测试也确认了 stop/start 顺序（`android/app/src/test/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModelTest.kt:169-215`）。
2. 倍速控制由页面状态驱动。
   - iOS `selectSpeed(_:)` 会同步更新 `currentSpeed` 与 `playbackRate`，`NativeVideoPlayerView` 再把该 rate 应用到 `AVPlayer`（`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:135-138`、`ios/ShortDrama/Sources/Features/Player/Views/Components/NativeVideoPlayerView.swift:20-24`）。
   - Android `PlayerUiState` 内维护 `PlaybackSpeed`，`PlayerScreen` 用 bottom sheet 选择速度；但当前 placeholder adapter 的 `setPlaybackSpeed` 仍为空实现（`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerUiState.kt:17-30`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt:159-178`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/player/PlaceholderPlayerHost.kt:19-37`）。
3. 退出、后台化或页面销毁时执行 best-effort stop。
   - iOS 在返回、`onDisappear` 和 `scenePhase == .background` 时都会走 `stopPlaybackIfNeeded(bestEffort: true)`（`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:115-133`、`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:232-296`、`ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift:160-236`）。
   - Android 在 `onBackgrounded()` 与 `onScreenDisposed()` 中触发 `reportStopBestEffort()`，异常被吞掉，不阻塞 UI（`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:108-121`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:352-378`、`android/app/src/test/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModelTest.kt:217-252`）。

### 页面可见性与播放器宿主边界

| 场景 | 当前实现 | 源文件 |
|------|---------|--------|
| iOS 播放器宿主 | 使用 `VideoPlayer(player: AVPlayer)`，并通过 periodic time observer 回传播放进度 | `ios/ShortDrama/Sources/Features/Player/Views/Components/NativeVideoPlayerView.swift:12-54` |
| iOS 页面栏位隐藏 | `PlayerView` 显式隐藏 tab bar 和 navigation bar | `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift:38-52` |
| Android 页面栏位隐藏 | 只通过 `shouldShowBottomBar()` 隐藏 app bottom bar；player route 与 alias route 都会触发该逻辑 | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:77-118`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:227-235`、`android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt:9-26` |
| Android 播放宿主 | `PlaceholderPlayerHost` 明确标注“未引入 androidx.media3；真实视频播放待用户授权新增依赖” | `android/app/src/main/java/com/djs66256/short_drama/feature/player/player/PlaceholderPlayerHost.kt:39-75` |
| Web 播放器 | 仅渲染标题、`Video ID` 和“待实现”文案 | `web/src/features/player/PlayerScreen.tsx:7-20` |

## 多端实现

### Web

- 路由页：`web/src/app/play/[id]/page.tsx:9-39`
- Feature 页：`web/src/features/player/PlayerScreen.tsx:7-20`
- 首页入口：`web/src/features/home/HomeScreen.tsx:27-38`
- 结论：PRD-03 没有把 Web 播放器推进到真实播放，只保留了占位路由。

### Android

- 路由与 alias：`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:29-80`
- deeplink 兼容：`android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt:28-45`
- 页面装配：`android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt:46-196`
- 状态机：`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:26-385`
- UI 状态：`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerUiState.kt:6-64`
- 后端接线：`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt:39-63`、`android/app/src/main/java/com/djs66256/short_drama/data/datasource/PlayerRemoteDataSource.kt:15-67`、`android/app/src/main/java/com/djs66256/short_drama/data/repository/PlayerRepositoryImpl.kt:18-86`
- 当前结论：Android 已经不是“只展示 videoId 的占位页”，但也不能写成“已完成 Media3 原生真播”。

### iOS

- 路由：`ios/ShortDrama/Sources/App/AppRoute.swift:19-28`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift:9-45`
- 页面：`ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift:7-138`
- 原生播放器宿主：`ios/ShortDrama/Sources/Features/Player/Views/Components/NativeVideoPlayerView.swift:4-55`
- 状态机：`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:4-303`
- 后端接线：`ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift:3-108`
- 当前结论：iOS 已接入真实 AVPlayer 播放、进度观察、倍速应用和 best-effort stop。

### Backend

- Route：`backend/src/app/api/player/progress/route.ts:1-45`、`backend/src/app/api/dramas/[id]/episodes/route.ts:1-20`、`backend/src/app/api/player/start/route.ts:1-47`、`backend/src/app/api/player/stop/route.ts:1-48`
- Schema：`backend/src/lib/schemas.ts:65-155`
- Service：`backend/src/services/player/player.service.ts:18-150`
- 测试：`backend/src/app/api/__tests__/player.progress.test.ts:16-115`、`backend/src/app/api/__tests__/drama-episodes.test.ts:14-103`、`backend/src/app/api/__tests__/player.start.test.ts:11-120`、`backend/src/app/api/__tests__/player.stop.test.ts:13-143`、`backend/src/services/player/player.service.test.ts:27-117`
- 当前结论：四个播放器接口都已落地，且 route / service 两层都有自动化覆盖。

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/player/progress` | [../../api/player.md](../../api/player.md) | 查询当前匿名播放会话的续播信息 |
| `GET /api/dramas/:id/episodes` | [../../api/player.md](../../api/player.md) | 返回指定 drama 的 canonical episode list |
| `POST /api/player/start` | [../../api/player.md](../../api/player.md) | 在客户端确定目标集后开始播放 |
| `POST /api/player/stop` | [../../api/player.md](../../api/player.md) | best-effort 上报当前 episode 的观看进度 |
| `GET /api/dramas` | [../../api/dramas.md](../../api/dramas.md) | 首页 Feed 为播放器提供 `drama.id` 入口 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| 匿名 `playbackSessionId` | iOS Keychain / Android DataStore | 设备级 | 只用于 `progress/start/stop` 的 header 透传 | `ios/ShortDrama/Sources/Core/Storage/PlaybackSessionStore.swift:75-109`、`android/app/src/main/java/com/djs66256/short_drama/core/storage/PlaybackSessionStore.kt:16-41` |
| 当前剧集 / 剧集列表 | `ObservableObject` / `StateFlow<PlayerUiState>` | 页面级 | bootstrap 后驱动播放器正文、选集面板与 no-resource 状态 | `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:49-61`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerUiState.kt:37-60` |
| 当前进度 | iOS `currentProgress` / Android `currentPlaybackPositionSeconds` | 页面级 | stop 上报、续播和切集都会读取 | `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:52-56`、`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:140-143`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:44-47`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:95-97` |
| 当前倍速 | ViewModel 状态 | 页面级 | 切集时保留；iOS 会同步到 AVPlayer rate，Android 目前主要驱动 UI 状态 | `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:53-56`、`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:135-138`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerUiState.kt:17-30` |
| iOS stop 去重指纹 | `lastStopFingerprint` | 页面级 | 避免同一集、同一进度、同时长的 stop 重复上报 | `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:71-74`、`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:265-295` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 首页信息流 | 导航入口 | Android / iOS 的播放器主入口仍来自首页 Feed 卡片 |
| 深链 | 外部入口 | Android 兼容 `player` alias；iOS 只接受 `play` |
| 数据模型 | Episode / Player models | bootstrap、续播、切集和选集 UI 都依赖统一的剧集与续播模型 |

### 外部依赖

| 服务 / 能力 | 用途 | 接入方式 |
|-------------|------|---------|
| Backend Player API | 续播查询、起播、停播上报 | `GET /api/player/progress`、`POST /api/player/start`、`POST /api/player/stop` |
| Backend Dramas subresource | 剧集列表 | `GET /api/dramas/:id/episodes` |
| AVKit | iOS 实际视频播放 | `VideoPlayer(player: AVPlayer)` |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| Web 播放器仍为占位实现 | 不能把移动端 Native 结论外推到 Web | 2026-07-26 | `web/src/features/player/PlayerScreen.tsx:7-20` |
| Android 尚未接入真实媒体引擎 | Android 当前是完整播放器页面壳、状态机和后端链路，不是完整真播实现 | 2026-07-26 | `android/app/src/main/java/com/djs66256/short_drama/feature/player/player/PlaceholderPlayerHost.kt:39-75` |
| Android 当前只隐藏应用内底栏 | 代码只看到 `shouldShowBottomBar()` 控制 app bottom bar，未看到系统级状态栏 / 导航栏隐藏实现 | 2026-07-26 | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:77-118`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:227-235` |
| `mall` / `earn` 仍是产品 H5 策略，不是当前已落地容器 | 不能把 `PRODUCT.md` 的 H5 策略写成 Android / iOS 已实现 H5 container | 2026-07-26 | `PRODUCT.md:22-25`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:188-208`、`ios/ShortDrama/Sources/Features/Shell/Views/PlaceholderTabView.swift:6-22` |
| 互动能力仍是首版页面承载 | 点赞 / 收藏 / 更多尚未接入后端持久化 | 2026-07-26 | `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:83-93`、`ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift:87-137` |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-26 | 更新：按 PRD-03 真实代码将播放器修正为“Backend 首版接口已实现并测试、iOS 已接入 AVPlayer 真播、Android 已接入真实页面 / 状态机 / 后端契约但播放宿主仍为 placeholder、Web 仍为占位页”，并补充 `play` canonical route、`X-Playback-Session-Id` 使用边界与 H5 策略边界 |
| 2026-07-26 | 更新：补充 PRD-02 后播放页真实入口已切换为首页信息流卡片，记录 `drama.id -> play/:id` 的移动端导航链路，并保留 Backend 仍为 501 的事实 |
| 2026-07-25 | 更新：依据现有代码将播放器文档从“已设计但未初始化”修正为“跨端路由占位已落地、后端接口仍为 501”，并补充各端真实入口与参数透传链路 |
| 2026-07-22 | API 路径重命名：`/api/video/play` -> `/api/player/start`，新增 `/api/player/stop`，API 定义移至 `wiki/api/player.md` |

---
*本文档由 llm-wiki skill 自动维护，从代码中提取。如有不一致，以代码为准。*