# 剧场频道 (Theater)

> 最后更新：2026-07-28
> 覆盖端：Android / iOS / Backend（Web 本期不实现）

## 功能概述

剧场频道在既有 5 Tab 容器中补齐了独立于首页发现链路的第二个内容主入口。当前 Android 与 iOS 都已把“剧场”作为真实一级频道接入底部导航，进入后默认展示 `all` 频道第一页内容，并在同一页内提供搜索入口、8 个横向频道切换、快捷入口和双列卡片流；Backend 同步提供 `GET /api/dramas/channel` 作为剧场 feed 的唯一数据源。当前运行时只有 `all` 返回真实内容，其余 `real / anime / movie / audio / novel / comic / bigscreen` 频道都返回 200 + 空列表，用于承接合法空态而不是错误页。剧场内的搜索、分类、排行、预约、新剧继续复用首页已拥有的 Native 页面与 canonical 路由；卡片点击则统一复用既有 `play` 播放主路径。

- 核心价值：为用户提供独立于首页的“找剧”入口，并用频道切换与快捷入口把剧场浏览和既有搜索/排行/分类能力串成一条可复用主链路
- 覆盖范围：Backend 剧场 feed 接口、Android 剧场 Tab 与状态机、iOS 剧场 Tab 与状态机、剧场到搜索/分类/排行/新剧/播放页的跨页复用链路
- 当前状态：Android / iOS / Backend 已实现；Web 继续不实现真实剧场频道

## 入口与路由

| 端 | 入口 | 路由 / 触发方式 | 源文件 |
|----|------|----------------|--------|
| Android | 底部导航“剧场”一级频道 | `TopLevelTab.THEATER`，graph=`theater_graph`，root=`theater` | `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:9-20`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:136-159,271-285` |
| Android | 剧场页内搜索框 | `AppDestination.search()`，会先切换到 `HOME` tab，再进入 `search` | `android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:272-282,428-433` |
| Android | 剧场快捷入口 | `classification` / `ranking?contentType=all&type=hot` / `ranking?contentType=all&type=booking` / `new-releases` | `android/app/src/main/java/com/djs66256/short_drama/navigation/TheaterShortcutRoute.kt`, `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:113-124` |
| Android | 剧场卡片点击 | `play/{videoId}` | `android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt:118-123`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:279-281` |
| iOS | 底部导航“剧场”一级频道 | `AppTab.theater`，root view=`TheaterView()` | `ios/ShortDrama/Sources/App/AppTab.swift:3-41`, `ios/ShortDrama/Sources/App/TabNavigationHostView.swift:39-47` |
| iOS | 剧场页内搜索框 | `.searchHome`，会自动切换到 `home` tab 承载 | `ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift`, `ios/ShortDrama/Sources/App/NavigationRouter.swift:52-69` |
| iOS | 剧场快捷入口 | `.classificationHome` / `.rankingHome`（含 `TheaterRankingEntryContext`）/ `.newReleases` | `ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift:62-79`, `ios/ShortDrama/Sources/Domain/Entities/TheaterRankingEntryContext.swift:3-10` |
| iOS | 剧场卡片点击 | `.player(videoId:)`，其 public route name 为 `play` | `ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift:81-84`, `ios/ShortDrama/Sources/App/AppRoute.swift:42-65` |
| Backend | 剧场 feed 接口 | `GET /api/dramas/channel?channel&page&pageSize` | `backend/src/app/api/dramas/channel/route.ts:1-17` |
| Web | 无真实入口 | Web 不提供剧场频道页面 | 范围约束见 `PRODUCT.md` |

### Deeplink / canonical naming

- Android 将剧场定义为真实一级 tab：`TopLevelTab.THEATER` 对应 graph `theater_graph` 与 root route `theater`，播放器仍统一复用 canonical `play/{videoId}`，并兼容 legacy `player/{videoId}`（`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:9-20,38-46,103-105`）。
- iOS 将剧场定义为真实一级 tab：`AppTab.theater` 只负责展示 `TheaterView`，但剧场内跳出的搜索/排行/分类/新剧/播放页都归属于 `home` tab 的导航栈（`ios/ShortDrama/Sources/App/AppTab.swift:3-41`, `ios/ShortDrama/Sources/App/AppRoute.swift:26-40`）。
- 预约快捷入口不是新路由，而是“带剧场上下文进入排行页”：Android 通过 `ranking?contentType=all&type=booking`，iOS 通过 `TheaterRankingEntryContext(contentType: .all, rankingType: .booking)` 注入排行首屏状态（`android/app/src/main/java/com/djs66256/short_drama/navigation/TheaterShortcutRoute.kt:8-23`, `ios/ShortDrama/Sources/Domain/Entities/TheaterRankingEntryContext.swift:3-10`）。

## 核心逻辑

### 流程：进入剧场 Tab 并加载默认频道

1. 用户点击底部导航中的“剧场”一级频道，进入真实剧场页面，而不是占位页。
   - Android `NavGraph` 已把 `theater_graph -> theater` 绑定到 `TheaterScreen`，不再复用 `PlaceholderScreen`（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:271-285`）。
   - iOS `TabNavigationHostView` 在 `case .theater` 时直接渲染 `TheaterView()`（`ios/ShortDrama/Sources/App/TabNavigationHostView.swift:39-47`）。
2. 页面首次进入时默认请求 `channel=all&page=1&pageSize=20`。
   - Android `TheaterViewModel` 初始化即触发首刷，默认 `selectedChannel = TheaterChannel.ALL`、`page = 1`、`pageSize = 20`（`android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModelTest.kt:47-69`）。
   - iOS `TheaterViewModel.loadIfNeeded()` 同样默认使用 `.all + page 1 + pageSize 20`（`ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift`, `ios/ShortDrama/Tests/ViewModelTests/TheaterViewModelTests.swift:53-66`）。
3. Backend `GET /api/dramas/channel` 使用 `TheaterFeedQuerySchema` 统一收口 query 默认值与参数校验。
   - `channel` 默认 `all`，`page` 默认 1，`pageSize` 默认 20，且 `pageSize` 上限为 100（`backend/src/lib/schemas.ts`, `backend/src/app/api/dramas/channel/route.ts:4-15`）。
4. 响应返回 `data + pagination`，其中每个剧场卡片在基础 `Drama` 字段之外新增 `heat` 整数字段。
   - Android / iOS DTO 都会把 `heat` 透传到 domain model，再由客户端格式化显示（`android/app/src/main/java/com/djs66256/short_drama/data/dto/TheaterFeedResponseDto.kt:9-55`, `ios/ShortDrama/Sources/Data/DTOs/TheaterFeedResponseDTO.swift:3-52`）。

### 流程：频道切换、合法空态与并发保护

1. 剧场顶部固定提供 8 个频道：`all / real / anime / movie / audio / novel / comic / bigscreen`。
   - Android `TheaterChannel` 维护 API 值与展示文案映射，`all` 展示为“找剧”（`android/app/src/main/java/com/djs66256/short_drama/domain/model/TheaterChannel.kt`）。
   - iOS `TheaterChannel` 同样作为固定枚举供 `TheaterChannelTabBar` 横向渲染（`ios/ShortDrama/Sources/Domain/Entities/TheaterChannel.swift`, `ios/ShortDrama/Sources/Features/Theater/Views/TheaterChannelTabBar.swift:3-31`）。
2. 选中非 `all` 频道时，当前运行时应返回 200 + 空数组，而不是报错。
   - Backend mock repository 将 `real / anime / movie / audio / novel / comic / bigscreen` 明确列为 `THEATER_SUPPORTED_EMPTY_CHANNELS`，统一返回空分页（`backend/src/repositories/mock/drama.mock.repository.ts`）。
   - Route 测试明确要求 `channel=real&page=1&pageSize=20` 返回 `data=[]` 且 `total_pages=0`（`backend/src/app/api/__tests__/dramas-channel.test.ts:103-118`）。
   - Android 与 iOS 的 ViewModel 测试都把“非 all 空响应”视为合法空态，而不是错误态（`android/app/src/test/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModelTest.kt:71-97`, `ios/ShortDrama/Tests/ViewModelTests/TheaterViewModelTests.swift:68-83`）。
3. 快速切换频道时，两端都只消费最后一次请求结果，避免旧请求覆盖当前选中频道。
   - Android 使用 request token / active token 丢弃过期响应（`android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt`, `android/app/src/test/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModelTest.kt:99-143`）。
   - iOS 使用 `requestToken = UUID()` 只让最后一次频道请求生效（`ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift`, `ios/ShortDrama/Tests/ViewModelTests/TheaterViewModelTests.swift:85-109`）。

### 流程：快捷入口复用首页拥有的页面

1. 剧场页顶部搜索框不会在剧场 tab 内再实现一套搜索页，而是直接复用首页拥有的搜索发现页。
   - Android `onSearchClick()` 发出 `TheaterEffect.Navigate(TheaterShortcutRoute.Search.route)`，`NavGraph` 通过 `navigateToHomeOwnedDestination()` 先切到 `HOME` tab 再导航（`android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:428-433`）。
   - iOS `routeEffect = .navigate(.searchHome)`，`NavigationRouter.navigate(to:)` 会按 `route.owningTab` 自动切到 `home`（`ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift`, `ios/ShortDrama/Sources/App/NavigationRouter.swift:52-69`）。
2. 分类、排行、新剧快捷入口也都复用首页已有页面，而不是在剧场 tab 内重复注册一套本地页面。
   - Android `TheaterShortcutRoute` 把分类/排行/新剧分别映射到 canonical `classification`、`ranking?...`、`new-releases`（`android/app/src/main/java/com/djs66256/short_drama/navigation/TheaterShortcutRoute.kt:5-23`）。
   - iOS `openShortcut(_:)` 将 `.classification` 映射到 `.classificationHome`，`.newReleases` 映射到 `.newReleases`（`ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift`）。
3. “预约”快捷入口要求直接落到排行页的预约榜上下文。
   - Android 直接构造 `ranking?contentType=all&type=booking`（`android/app/src/main/java/com/djs66256/short_drama/navigation/TheaterShortcutRoute.kt:14-21`, `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:117-120`）。
   - iOS 通过 `.openRanking(TheaterRankingEntryContext(rankingType: .booking))`，由 `NavigationRouter` 暂存上下文，再在 `RankingHomeView(initialEntryContext:)` 注入首屏默认维度（`ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift`, `ios/ShortDrama/Sources/App/NavigationRouter.swift:15-18,57-63`, `ios/ShortDrama/Sources/App/TabNavigationHostView.swift:19-21`, `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift:9-17`）。

### 流程：剧场卡片展示、热度格式化与分页

1. 剧场内容区使用双列卡片流展示封面、热度、标题和元信息。
   - Android `TheaterComponents.kt` 负责双列 feed、空态、错误态、footer 和最后一项触发分页（`android/app/src/main/java/com/djs66256/short_drama/feature/theater/ui/TheaterComponents.kt`）。
   - iOS `TheaterFeedGridView` + `TheaterDramaCardView` 负责双列布局、最后一项 `onAppear` 触发加载更多（`ios/ShortDrama/Sources/Features/Theater/Views/TheaterFeedGridView.swift:3-49`, `ios/ShortDrama/Sources/Features/Theater/Views/TheaterDramaCardView.swift:3-83`）。
2. Backend 只返回原始整数 `heat`，格式化责任由客户端承担。
   - Android `formatHeat()` 把 `10000+` 格式化为 `x万`（`android/app/src/main/java/com/djs66256/short_drama/feature/theater/model/TheaterDramaItemUiModel.kt`）。
   - iOS `TheaterHeatFormatter` 同时支持 `万` 与 `亿`（`ios/ShortDrama/Sources/Features/Theater/Models/TheaterHeatFormatter.swift`）。
3. 翻页时维持同一频道并追加下一页结果；追加失败时保留已有内容并显示尾部错误。
   - Android 测试覆盖了追加失败不清空已有 items、成功后清除 `appendErrorMessage` 的行为（`android/app/src/test/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModelTest.kt:145-183`）。
   - iOS 测试覆盖了追加成功拼接第二页、失败仅更新 `appendErrorMessage` 的行为（`ios/ShortDrama/Tests/ViewModelTests/TheaterViewModelTests.swift:111-145`）。

### 流程：扫码占位、本地反馈与播放页复用

1. 剧场页顶部相机/识图入口当前不接后端，也不跳新页面，只提供本地占位反馈。
   - Android 发出 `TheaterEffect.ShowMessage("识图功能开发中")` 并以 Toast 展示（`android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt`, `android/app/src/main/java/com/djs66256/short_drama/feature/theater/ui/TheaterScreen.kt:40-49`, `android/app/src/test/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModelTest.kt:185-200`）。
   - iOS 发出 `.showScanPlaceholder("识图功能开发中")` 并展示 Alert（`ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift`, `ios/ShortDrama/Sources/Features/Theater/Views/TheaterView.swift:43-49,151-170`, `ios/ShortDrama/Tests/ViewModelTests/TheaterViewModelTests.swift:147-181`）。
2. 点击剧场卡片后继续复用既有 canonical `play` 路由，不创建新的“剧场播放器”语义。
   - Android 发出 `OpenPlay(videoId)` 并导航到 `AppDestination.play(videoId)`（`android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt:118-123`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:279-281`）。
   - iOS 把 `TheaterDrama.id` 映射到 `.player(videoId:)`，其 public route name 仍为 `play`（`ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift:81-84`, `ios/ShortDrama/Sources/App/AppRoute.swift:58-60`）。

### 边界与异常处理

| 场景 | 处理方式 | 源文件 |
|------|---------|--------|
| `channel` 非法或 `page/pageSize` 越界 | Backend 返回 400 `VALIDATION_ERROR` | `backend/src/app/api/__tests__/dramas-channel.test.ts:120-127` |
| Repository 返回的剧场数据缺失 `heat` 等关键字段 | Service 再次用 schema 校验并转成 500 `INTERNAL_ERROR` | `backend/src/services/drama/drama.service.ts`, `backend/src/services/drama/drama.service.test.ts:101-133`, `backend/src/app/api/__tests__/dramas-channel.test.ts:129-138` |
| 非 `all` 频道当前无数据 | 返回合法空态而不是错误态 | `backend/src/app/api/__tests__/dramas-channel.test.ts:103-118`, `android/app/src/test/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModelTest.kt:71-97`, `ios/ShortDrama/Tests/ViewModelTests/TheaterViewModelTests.swift:68-83` |
| 快速切换频道导致旧请求晚返回 | Android / iOS 都只消费最后一次结果 | `android/app/src/test/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModelTest.kt:99-143`, `ios/ShortDrama/Tests/ViewModelTests/TheaterViewModelTests.swift:85-109` |
| 分页失败 | 保留已加载内容，仅在 footer 暴露 append error | `android/app/src/test/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModelTest.kt:145-183`, `ios/ShortDrama/Tests/ViewModelTests/TheaterViewModelTests.swift:129-145` |
| 扫码入口点击 | 只给本地占位反馈，不发网络请求 | `android/app/src/test/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModelTest.kt:185-200`, `ios/ShortDrama/Tests/ViewModelTests/TheaterViewModelTests.swift:147-181` |
| 剧场卡片 `id` 为空 | 客户端直接拦截，不进入播放页 | `android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt:118-123`, `ios/ShortDrama/Tests/ViewModelTests/TheaterViewModelTests.swift:183-193` |

## 多端实现

### Android

- 一级频道承载：`TopLevelTab.THEATER + theater_graph + TheaterScreen` 构成真实剧场 tab（`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:9-20`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:271-285`）。
- 数据链路：`ApiService.getDramaChannel() -> DramaRemoteDataSource -> DramaRepositoryImpl.getTheaterFeed()`（`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt`, `android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt:43-55`）。
- 状态源：`TheaterViewModel` 维护 `selectedChannel / items / isLoading / isAppending / page / hasNextPage / errorMessage` 与 `effects`（`android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt`）。
- UI：`TheaterScreen` + `TheaterComponents` 负责顶部搜索与扫码、横向频道栏、快捷入口、双列卡片、错误/空态/分页 footer（`android/app/src/main/java/com/djs66256/short_drama/feature/theater/ui/TheaterScreen.kt`, `android/app/src/main/java/com/djs66256/short_drama/feature/theater/ui/TheaterComponents.kt`）。
- 自动化证据：`TheaterViewModelTest` 覆盖默认首屏、非 all 空态、并发请求保护、分页成功/失败、快捷入口和扫码占位（`android/app/src/test/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModelTest.kt:47-229`）。

### iOS

- 一级频道承载：`AppTab.theater` 作为真实 tab，`TabNavigationHostView` root view 直接是 `TheaterView()`（`ios/ShortDrama/Sources/App/AppTab.swift:3-41`, `ios/ShortDrama/Sources/App/TabNavigationHostView.swift:39-47`）。
- 数据链路：`GetTheaterFeedEndpoint(/api/dramas/channel) -> DramaRemoteDataSource.fetchTheaterFeed() -> DramaRepository.fetchTheaterFeed()`（`ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift`, `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift:44-47`）。
- 状态源：`TheaterViewModel` 维护 `selectedChannel / viewState / isAppending / appendErrorMessage / routeEffect` 与 `requestToken`（`ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift`）。
- UI：`TheaterView` + `TheaterChannelTabBar` + `TheaterShortcutGrid` + `TheaterFeedGridView` + `TheaterDramaCardView` 负责 tab、快捷入口、双列卡片和 Alert 占位反馈（`ios/ShortDrama/Sources/Features/Theater/Views/TheaterView.swift:18-172`, `ios/ShortDrama/Sources/Features/Theater/Views/TheaterChannelTabBar.swift:3-31`, `ios/ShortDrama/Sources/Features/Theater/Views/TheaterShortcutGrid.swift:3-31`, `ios/ShortDrama/Sources/Features/Theater/Views/TheaterFeedGridView.swift:3-49`, `ios/ShortDrama/Sources/Features/Theater/Views/TheaterDramaCardView.swift:3-83`）。
- 自动化证据：`TheaterViewModelTests` 覆盖默认首屏、非 all 空态、乱序请求保护、分页成功/失败、快捷入口、扫码占位和空 id 拦截（`ios/ShortDrama/Tests/ViewModelTests/TheaterViewModelTests.swift:5-194`）。

### Backend

- Route：`GET /api/dramas/channel` 解析 query 后调用 `DramaService.listTheaterFeed()`（`backend/src/app/api/dramas/channel/route.ts:1-17`）。
- Service：`DramaService.listTheaterFeed()` 用 `TheaterFeedResponseSchema` 再次校验 repository 输出，非法内部数据统一转 `INTERNAL_ERROR`（`backend/src/services/drama/drama.service.ts`、`backend/src/services/drama/drama.service.test.ts:101-133`）。
- Repository Contract：`DramaRepositoryInterface` 新增 `TheaterFeedParams` 与 `listTheaterFeed()`（`backend/src/repositories/interfaces/drama.repository.interface.ts:14-30,62-66`）。
- 当前运行时数据源：route 通过 `getDramaRepository()` 走 repository registry 注入，不再直接写死 `DramaMockRepository()`（`backend/src/repositories/repository-registry.ts`, `backend/src/app/api/dramas/channel/route.ts:10-14`）。
- Mock 业务语义：`all` 返回真实列表，`heat` 来自 ranking `play_count`；其余支持频道返回合法空分页（`backend/src/repositories/mock/drama.mock.repository.ts`）。
- 自动化证据：`dramas-channel.test.ts` 与 `drama.service.test.ts` 覆盖成功、空态、参数校验和非法内部数据（`backend/src/app/api/__tests__/dramas-channel.test.ts:65-139`, `backend/src/services/drama/drama.service.test.ts:101-133`）。

### Web

- Web 当前没有剧场频道页面，也不消费 `GET /api/dramas/channel`。
- 这与当前“商城和赚钱走 H5，其它业务页默认按 Native 规划”的承载策略一致，剧场频道以 Android / iOS Native 为主（见 `PRODUCT.md` 与 memory 约束）。

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/dramas/channel` | [../../api/dramas.md](../../api/dramas.md) | 剧场频道唯一 feed 数据源，支持 `channel/page/pageSize` |
| `GET /api/dramas/search` | [../../api/dramas.md](../../api/dramas.md) | 剧场顶部搜索入口复用首页搜索发现链路 |
| `GET /api/dramas/tags` | [../../api/dramas.md](../../api/dramas.md) | 剧场“分类”快捷入口复用首页分类页的数据源 |
| `GET /api/dramas/rankings` | [../../api/dramas.md](../../api/dramas.md) | 剧场“排行/预约”快捷入口复用首页排行页与预约榜 |
| `POST /api/dramas/:id/book` | [../../api/dramas.md](../../api/dramas.md) | 剧场“预约”快捷入口最终承接到排行页内的预约接口 |
| `POST /api/player/start` / `POST /api/player/stop` | [../../api/player.md](../../api/player.md) | 剧场卡片点击后继续复用既有播放器主链路 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Android `TheaterUiState` | `MutableStateFlow` | 页面级 | 聚合当前频道、列表、loading、append、错误、页码和是否还有下一页 | `android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt` |
| Android `effects` | `MutableSharedFlow<TheaterEffect>` | 页面级 | 负责导航到搜索/分类/排行/新剧/播放页或触发本地提示 | `android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt` |
| Android request token | ViewModel 私有字段 | 页面级 | 用于丢弃频道快切或翻页时的过期响应 | `android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt` |
| iOS `selectedChannel / viewState / isAppending / appendErrorMessage` | `@Published` | 页面级 | 剧场页唯一状态源，承载当前频道、内容态与分页尾部状态 | `ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift` |
| iOS `routeEffect` | `@Published` | 页面级 | 负责驱动跳首页搜索、跳排行上下文和扫码占位反馈 | `ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift` |
| iOS request token | ViewModel 私有字段 | 页面级 | 防止乱序请求覆盖当前频道的结果 | `ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift` |
| Backend theater query / response | Zod schema + JSON 响应 | 请求级 | 统一约束 `channel/page/pageSize` 与 `heat + pagination` 响应形态 | `backend/src/lib/schemas.ts`, `backend/src/app/api/dramas/channel/route.ts:4-15` |
| iOS 排行进入上下文 | `NavigationRouter.pendingTheaterRankingEntryContext` | 应用级 | 用于剧场预约/排行快捷入口把初始榜单维度传给排行页 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:15-18,57-63` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 应用壳 | 一级频道承载 | 剧场作为 Android / iOS 5 Tab 容器中的真实一级频道落地 |
| 搜索发现 | 路由复用 | 剧场顶部搜索与分类/排行/新剧快捷入口都复用首页拥有的页面 |
| 排行体系 | 上下文复用 | 剧场“排行/预约”直接复用排行页，并注入默认榜单维度 |
| 播放器 | 路由复用 | 剧场卡片点击后继续走 canonical `play` 主路径 |
| 分类浏览 | 快捷入口复用 | 剧场“分类”不会新建页面，继续承接首页分类页 |
| 数据模型 | 共享基础 `Drama` 字段 | 剧场卡片沿用 `Drama` 基础字段，并额外扩展 `heat` |

### 外部依赖

| 服务 / 框架 | 用途 | 接入方式 |
|-------------|------|---------|
| Next.js Route Handlers | Backend 暴露剧场 feed 接口 | `backend/src/app/api/dramas/channel/route.ts` |
| Retrofit + OkHttp | Android 拉取剧场 feed | `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` |
| URLSession + APIClient | iOS 拉取剧场 feed | `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` |
| Zod | Backend theater query / response 校验 | `backend/src/lib/schemas.ts` |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| Web 不实现真实剧场频道 | Web 无法验证剧场 tab、频道切换与快捷入口体验 | 2026-07-28 | 属于明确范围外，符合 Native 优先承载策略 |
| Backend 运行时只有 `all` 频道返回真实内容 | 其它 7 个频道当前只展示合法空态，尚无分类化内容供给 | 2026-07-28 | `real / anime / movie / audio / novel / comic / bigscreen` 均返回 200 + 空列表 |
| Backend 当前仍使用 mock 剧场数据 | 剧场 feed、heat 与分页都来自本地 mock repository，不是线上内容服务 | 2026-07-28 | route 已改为 registry 注入，但默认仓库仍是 mock |
| 扫码/识图入口仍为占位反馈 | 用户无法从剧场进入真实识图链路 | 2026-07-28 | Android Toast / iOS Alert 都显示“识图功能开发中” |
| iOS 与 Android 剧场卡片展示细节不完全一致 | 元信息和 footer 文案存在轻微差异，但不影响主流程一致性 | 2026-07-28 | Android 会展示“没有更多了”并可拼接更多 tags/rating；iOS footer 仅显示加载或错误，元信息只取首个 tag |
| 设备级黑盒测试未自动执行 | 当前结论主要来自代码、单测与 QA 文档，真机手势/滚动表现仍待补测 | 2026-07-28 | 参考 `docs/specs/2026-07-28-prd-12-theater-channel/qa-test.md` |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-28 | 初始创建：收录 PRD-12 剧场频道的独立一级 Tab、`GET /api/dramas/channel`、8 频道切换、合法空态、热度格式化、剧场快捷入口到首页拥有页面的复用策略，以及播放主路径复用与自动化验证结果 |

---
*本文档由 llm-wiki skill 自动维护。*