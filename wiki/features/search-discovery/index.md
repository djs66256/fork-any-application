# 搜索发现 (Search Discovery)

> 最后更新：2026-07-28
> 覆盖端：Android / iOS / Backend（Web 本期不实现）

## 功能概述

搜索发现能力在 PRD-02 首页信息流与 PRD-03 播放主链路之上，补齐“主动找内容”和“探索式发现”的二级入口。当前 Android 与 iOS 已同时从首页右上角和剧场频道顶部搜索框接入搜索入口，并落地搜索发现页、搜索结果页、快捷入口承接页、本地搜索历史与热搜榜；Backend 同步提供 `GET /api/dramas/search` 与 `GET /api/dramas/hot-search` 两个接口，搜索结果继续复用首页 `Drama` 列表契约与播放/详情路由语义。剧场内触发搜索时会直接切回首页所属导航栈复用既有搜索页，而不会在剧场 tab 内再实现一套搜索页面。Web 仍保持 `/search`、`/rankings` 占位页，不属于本期真实交付范围（`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:41-49`、`ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:92-111`、`android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt`、`backend/src/app/api/dramas/search/route.ts:7-19`、`backend/src/app/api/dramas/hot-search/route.ts:6-11`、`web/src/app/search/page.tsx:1-10`、`web/src/app/rankings/page.tsx:1-10`）。

- **核心价值**：让用户可以从首页主动搜索短剧、查看热搜和通过排行/分类/新剧/演员入口继续探索内容
- **覆盖范围**：Backend 搜索接口与热搜接口、Android 搜索发现页/搜索结果页/本地历史、iOS 搜索发现页/搜索结果页/本地历史
- **当前状态**：Backend + Android + iOS 已实现；Web 继续保持占位页

## 入口与路由

| 端 | 入口 | 路由 / 触发方式 | 源文件 |
|----|------|----------------|--------|
| Android | 首页右上角搜索按钮 | `home -> search -> search/result?query={query}`；快捷入口为 `ranking` / `classification` / `new-releases` / `actors` | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:92-111`、`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:44-49,96-106` |
| iOS | 首页 `toolbar` 搜索按钮 | `.searchHome`、`.searchResult(query:)`、`.rankingHome`、`.classificationHome`、`.newReleases`、`.actorHub` | `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:41-49`、`ios/ShortDrama/Sources/App/AppRoute.swift:7-18,39-59` |
| Backend | 搜索/热搜接口 | `GET /api/dramas/search?q&page&pageSize`、`GET /api/dramas/hot-search` | `backend/src/app/api/dramas/search/route.ts:7-19`、`backend/src/app/api/dramas/hot-search/route.ts:6-11` |
| Web | 占位页入口 | `/search`、`/rankings` 仍为 H5/SSR 占位，不提供真实搜索体验 | `web/src/app/search/page.tsx:1-10`、`web/src/app/rankings/page.tsx:1-10` |

### Deeplink / canonical naming

- Android canonical route 定义为 `search`、`search/result?query={query}`、`ranking`、`classification`、`new-releases`、`actors`，并保留 `player/{videoId}`、`dramaDetail/{dramaId}` 历史别名路由（`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:40-49,88-106`）。
- iOS canonical `publicRouteName` 定义为 `search`、`search/result`、`ranking`、`classification`、`new-releases`、`actors`，播放页 canonical 仍统一为 `play`（`ios/ShortDrama/Sources/App/AppRoute.swift:39-59`）。
- 移动端 deeplink 已扩展支持 `djsdrama://search`、`djsdrama://search/result/<query>`、`djsdrama://ranking`、`djsdrama://classification`、`djsdrama://new-releases`、`djsdrama://actors`；Android 另外兼容 `djsdrama://player/<id>` 并统一映射到 canonical `play` 语义（`android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt:33-64`、`ios/ShortDrama/Sources/App/DeeplinkHandler.swift:27-93`）。

## 核心逻辑

### 流程：首页进入搜索发现页

1. 首页 Feed 右上角搜索按钮触发导航到搜索发现页，而不是切换新的一级 Tab。
   - Android 在 `HomeTopBar` 中调用 `onOpenSearch()`，由 `NavGraph` 导航到 `AppDestination.search()`（`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:92-111`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:151-169`）。
   - iOS 在 `HomeView.toolbar` 中调用 `router.navigate(to: .searchHome)`（`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:41-49`）。
2. 搜索发现页首屏同时展示搜索框、快捷入口、本地历史和热搜区块。
   - Android `SearchHomeScreen` 依次渲染 `SearchInputBar`、`SearchQuickEntrySection`、`SearchHistorySection`、`HotSearchSection`（`android/app/src/main/java/com/djs66256/short_drama/feature/search/ui/SearchHomeScreen.kt:68-98`）。
   - iOS `SearchHomeView` 依次渲染 `SearchBarSection`、`QuickEntryGrid`、`SearchHistorySection`、`HotSearchSection`（`ios/ShortDrama/Sources/Features/Search/Views/SearchHomeView.swift:21-55`）。
3. 搜索发现页加载时会并行准备本地历史和热搜；热搜失败时仅影响热搜区，不阻塞历史和手动搜索。
   - Android 在 `init` 中订阅 `ObserveSearchHistoryUseCase` 并调用 `loadHotSearches()`（`android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchHomeViewModel.kt:60-67`）。
   - iOS 在 `loadIfNeeded()` 中执行 `reloadHistory()` + `loadHotSearches()`（`ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift:44-49`）。

### 流程：提交搜索并进入搜索结果页

1. 两端都先对输入做 trim 和长度校验，空白词或超过 50 字符不会发请求。
   - Android 规则在 `normalizeSearchQueryOrNull` / `limitSearchQueryDraft` 中统一定义（`android/app/src/main/java/com/djs66256/short_drama/domain/model/SearchQueryRules.kt:3-13`）。
   - iOS 规则在 `SearchHomeViewModel.normalizedQuery` 与 `SearchResultViewModel.normalizedQuery` 中定义（`ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift:59-65`、`ios/ShortDrama/Sources/Features/Search/ViewModels/SearchResultViewModel.swift:105-111`）。
2. 提交后统一跳转到搜索结果页，并固定使用第一页、每页 10 条请求后端。
   - Android `SearchHomeViewModel` 发出 `search/result?query={query}` 导航事件；`SearchResultViewModel` 固定调用 `searchDramasUseCase(query, 1, 10)`（`android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchHomeViewModel.kt:104-109,156-162`、`android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchResultViewModel.kt:103-104,157-162`）。
   - iOS `SearchHomeView` 直接导航到 `.searchResult(query: normalized)`；`SearchResultViewModel` 固定调用 `SearchDramasUseCase.execute(query: page: 1, pageSize: 10)`（`ios/ShortDrama/Sources/Features/Search/Views/SearchHomeView.swift:57-70`、`ios/ShortDrama/Sources/Features/Search/ViewModels/SearchResultViewModel.swift:79-84`）。
3. Backend `GET /api/dramas/search` 使用 `SearchDramaQuerySchema` 校验 `q/page/pageSize`，并通过 `DramaService.searchDramas()` 返回与首页同构的 `{ data, pagination }` 响应（`backend/src/app/api/dramas/search/route.ts:7-19`、`backend/src/lib/schemas.ts:45-61`、`backend/src/services/drama/drama.service.ts:12-20`）。
4. 搜索结果列表继续复用首页 Drama 卡片与播放/详情路由，而不是引入新的结果卡片语义。
   - Android `SearchResultScreen` 直接复用 `HomeDramaCard`，点击后仍进入 `play/{id}` 或 `detail/{id}`（`android/app/src/main/java/com/djs66256/short_drama/feature/search/ui/SearchResultScreen.kt:96-101`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:180-184`）。
   - iOS `SearchResultStateView` 直接复用 `HomeDramaCardView`；播放/详情继续复用 `HomeRouteBuilder`（`ios/ShortDrama/Sources/Features/Search/Views/Components/SearchResultStateView.swift:21-33`、`ios/ShortDrama/Sources/Features/Search/Views/SearchResultView.swift:50-58`、`ios/ShortDrama/Sources/Features/Home/Views/Components/HomeDramaCardView.swift:3-4`）。

### 流程：热搜、历史与快捷入口

1. 热搜接口返回 Top 10 关键词和热度值，两端点击热搜词后走与手动输入相同的搜索结果链路（`backend/src/repositories/mock/drama.mock.repository.ts:151-164`、`android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchHomeViewModel.kt:79-85`、`ios/ShortDrama/Sources/Features/Search/Views/SearchHomeView.swift:64-70`）。
2. 本地搜索历史只在搜索请求成功后写入；空结果也写，失败不写。
   - Android 成功分支统一执行 `saveSearchHistoryUseCase(query)`，随后再根据结果决定展示列表或空态；错误分支不保存（`android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchResultViewModel.kt:103-137`）。
   - iOS 成功分支统一执行 `saveSearchHistoryUseCase.execute(keyword: normalized)`，随后展示 `.empty` 或 `.content`；失败分支不保存（`ios/ShortDrama/Sources/Features/Search/ViewModels/SearchResultViewModel.swift:79-102`）。
3. 快捷入口固定为排行 / 新剧 / 分类 / 演员，并全部落到 Native 承接页。
   - Android `defaultSearchQuickEntries()` 返回四个 canonical route（`android/app/src/main/java/com/djs66256/short_drama/feature/search/model/SearchQuickEntry.kt:18-39`）。
   - iOS `QuickEntry.defaults` 返回四个入口，`DiscoveryPlaceholderView` 作为首版 Native 承接页（`ios/ShortDrama/Sources/Domain/Entities/QuickEntry.swift:20-27`、`ios/ShortDrama/Sources/Features/Search/Views/DiscoveryPlaceholderView.swift:3-70`）。

### 边界与异常处理

| 场景 | 处理方式 | 源文件 |
|------|---------|--------|
| 搜索词为空或仅空格 | 客户端直接拦截，不导航、不发请求 | `android/app/src/main/java/com/djs66256/short_drama/domain/model/SearchQueryRules.kt:5-10`、`ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift:59-65` |
| 搜索词超过 50 字符 | Android 输入阶段裁剪，iOS 校验阶段拒绝提交；Backend 也会返回 400 | `android/app/src/main/java/com/djs66256/short_drama/domain/model/SearchQueryRules.kt:3-13`、`ios/ShortDrama/Sources/Features/Search/ViewModels/SearchResultViewModel.swift:105-111`、`backend/src/lib/schemas.ts:57-61` |
| 搜索请求失败 | 结果页进入 error 态，且不写入历史 | `android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchResultViewModel.kt:116-150`、`ios/ShortDrama/Sources/Features/Search/ViewModels/SearchResultViewModel.swift:91-102` |
| 搜索结果为空 | 结果页进入 empty 态，但仍写入历史 | `android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchResultViewModel.kt:103-115`、`ios/ShortDrama/Sources/Features/Search/ViewModels/SearchResultViewModel.swift:79-90` |
| 热搜加载失败 | 仅热搜区显示错误，可重试；历史与手动搜索继续可用 | `android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchHomeViewModel.kt:111-157`、`ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift:89-115` |
| 超大页码搜索 | Backend 返回 `200 + data=[]`，但保留分页信息 | `backend/src/app/api/__tests__/dramas-search.test.ts:55-80` |
| Android 重复提交相同进行中的查询 | `requestInFlight` / `activeQuery` 拦截重复搜索 | `android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchResultViewModel.kt:52-54,67-80,84-89` |
| iOS 重复提交相同进行中的查询 | `activeQuery == normalized` 时直接返回 | `ios/ShortDrama/Sources/Features/Search/ViewModels/SearchResultViewModel.swift:68-77` |
| Android 历史存储损坏 | `DataStore.data.catch { emit(emptyPreferences()) }` 降级为空历史 | `android/app/src/main/java/com/djs66256/short_drama/data/local/SearchHistoryLocalDataSource.kt:24-30` |
| iOS 历史存储损坏 | `JSONDecoder` 失败后清空坏数据并返回空历史 | `ios/ShortDrama/Sources/Data/Repositories/UserDefaultsSearchHistoryRepository.swift:21-31` |

## 多端实现

### Android

- 搜索发现页与结果页都挂在 HOME graph 内，不新增顶级 Tab（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:147-241`）。
- 搜索远端能力由 `SearchRemoteDataSource -> SearchRepositoryImpl -> SearchDramasUseCase / GetHotSearchKeywordsUseCase` 提供（`android/app/src/main/java/com/djs66256/short_drama/data/datasource/SearchRemoteDataSource.kt:15-55`、`android/app/src/main/java/com/djs66256/short_drama/data/repository/SearchRepositoryImpl.kt:15-48`）。
- 本地历史使用 `DataStore<Preferences>`，key 为 `search_history_entries`，最多 10 条、按更新时间倒序、去重保存（`android/app/src/main/java/com/djs66256/short_drama/data/local/SearchHistoryLocalDataSource.kt:20-53,56-123`）。
- deeplink 已扩展到搜索发现相关 host，并兼容 `player` 旧 host 映射到 canonical `play`（`android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt:33-64`）。
- 自动化证据：`SearchHomeViewModelTest`、`SearchResultViewModelTest`、`DeeplinkRouteParserTest`、`RoutesTest` 已覆盖热搜/历史、成功与失败写历史规则、query 参数和 canonical route（`android/app/src/test/java/com/djs66256/short_drama/feature/search/viewmodel/SearchHomeViewModelTest.kt:48-138`、`android/app/src/test/java/com/djs66256/short_drama/feature/search/viewmodel/SearchResultViewModelTest.kt:41-127`、`android/app/src/test/java/com/djs66256/short_drama/navigation/DeeplinkRouteParserTest.kt:9-76`、`android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt:29-55`）。

### iOS

- 搜索发现页、搜索结果页、排行/分类/新剧/演员承接页都挂在 home tab 的 `NavigationStack` 上（`ios/ShortDrama/Sources/App/TabNavigationHostView.swift:9-31`、`ios/ShortDrama/Sources/App/AppRoute.swift:24-37`）。
- 首页与搜索共用同一个 `DramaRepository`，搜索结果也复用首页 `HomeDramaCardView`（`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:9-13`、`ios/ShortDrama/Sources/Features/Search/Views/SearchResultView.swift:9-18`、`ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift:13-31`、`ios/ShortDrama/Sources/Features/Home/Views/Components/HomeDramaCardView.swift:3-4`）。
- 本地历史使用 `UserDefaultsSearchHistoryRepository`，存储 key 为 `search.history.items`，最多 10 条，保存时 trim、去重、置顶（`ios/ShortDrama/Sources/Data/Repositories/UserDefaultsSearchHistoryRepository.swift:6-18,34-60`）。
- deeplink 现支持 `search/search-result/ranking/classification/new-releases/actors/play/drama`，但不兼容 Android 那样的 `player` 历史 host（`ios/ShortDrama/Sources/App/DeeplinkHandler.swift:27-93`）。
- 自动化证据：`SearchHomeViewModelTests`、`SearchResultViewModelTests`、`DeeplinkHandlerTests` 已覆盖热搜失败、历史清空、空结果也写历史、失败不写历史和新 deeplink host（`ios/ShortDrama/Tests/ViewModelTests/SearchHomeViewModelTests.swift:18-195`、`ios/ShortDrama/Tests/ViewModelTests/SearchResultViewModelTests.swift:33-225`、`ios/ShortDrama/Tests/ViewModelTests/DeeplinkHandlerTests.swift:28-89`）。

### Backend

- `GET /api/dramas/search` 与 `GET /api/dramas/hot-search` 当前 route 层都直接实例化 `DramaMockRepository`，尚未接入 Supabase repository（`backend/src/app/api/dramas/search/route.ts:15-17`、`backend/src/app/api/dramas/hot-search/route.ts:7-9`）。
- 搜索 query 规则由 `SearchDramaQuerySchema` 统一定义：`q.trim().min(1).max(50)`、`page>=1`、`pageSize<=100`（`backend/src/lib/schemas.ts:57-61`）。
- Mock 搜索匹配规则为 `title` + `category` 的大小写不敏感 contains 匹配（`backend/src/repositories/mock/drama.mock.repository.ts:173-175,206-216`）。
- 搜索和首页列表共用 `DramaListResponseSchema`，因此结果列表字段和分页结构与首页完全对齐（`backend/src/lib/schemas.ts:45-61`、`backend/src/services/drama/drama.service.ts:8-20`）。
- 自动化证据：`dramas-search.test.ts`、`dramas-hot-search.test.ts`、`drama.service.test.ts` 已覆盖 trim、空白词、非法分页、超大页码空结果与热搜返回（`backend/src/app/api/__tests__/dramas-search.test.ts:34-111`、`backend/src/app/api/__tests__/dramas-hot-search.test.ts:18-43`、`backend/src/services/drama/drama.service.test.ts:152-199`）。

### Web

- Web `/search` 与 `/rankings` 仍为占位页，不接入搜索 API，也不承担本期 Native 搜索发现能力（`web/src/app/search/page.tsx:1-10`、`web/src/app/rankings/page.tsx:1-10`）。
- 页面承载策略明确除 `mall` / `earn` 外，其他业务页当前优先由 Native 实现（`PRODUCT.md:22-25`）。

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/dramas/search` | [../../api/dramas.md](../../api/dramas.md) | 关键词搜索接口，结果页直接复用首页 Drama 列表契约 |
| `GET /api/dramas/hot-search` | [../../api/dramas.md](../../api/dramas.md) | 搜索发现页热搜榜数据源 |
| `GET /api/dramas` | [../../api/dramas.md](../../api/dramas.md) | 首页 Feed 与搜索结果共用同一 Drama 字段契约 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Android `SearchHomeUiState` | `MutableStateFlow` | 页面级 | 聚合草稿 query、历史、热搜、快捷入口和热搜错误态 | `android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchHomeViewModel.kt:29-58` |
| Android `SearchResultUiState` | `MutableStateFlow` | 页面级 | 聚合 query、items、loading、error、retry 状态 | `android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchResultViewModel.kt:22-30,42-50` |
| Android 搜索历史 | `DataStore<Preferences>` | 设备级 | 本地最多保存 10 条历史，按更新时间倒序 | `android/app/src/main/java/com/djs66256/short_drama/data/local/SearchHistoryLocalDataSource.kt:20-53,56-123` |
| iOS `SearchHomeViewModel` 状态 | `@Published` | 页面级 | 管理草稿 query、historyItems、hotSearchState、quickEntries | `ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift:18-28` |
| iOS `SearchResultViewModel.viewState` | `@Published` | 页面级 | 管理 loading / content / empty / error 四态 | `ios/ShortDrama/Sources/Features/Search/ViewModels/SearchResultViewModel.swift:7-23` |
| iOS 搜索历史 | `UserDefaults` | 设备级 | 以 `search.history.items` 存储最近 10 条历史 | `ios/ShortDrama/Sources/Data/Repositories/UserDefaultsSearchHistoryRepository.swift:10-18,21-52` |
| Backend 搜索/热搜响应 | JSON 响应 | 请求级 | 搜索结果返回 `data + pagination`，热搜返回 `data[]` 最多 10 条 | `backend/src/lib/schemas.ts:45-75` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 首页信息流 | 入口复用 | 搜索入口从首页 Feed 顶部进入，不新增一级 Tab |
| 数据模型 | 共享契约 | 搜索结果与首页共用 `Drama` 字段集和分页结构 |
| 播放器 | 路由跳转 | 搜索结果点击“观看”仍走 `play/:id` / `.player(videoId:)` |
| 剧集详情 | 路由跳转 | 搜索结果点击“详情”仍走 `detail/:id` / `.dramaDetail(dramaId:)` |
| 深链 | 路由扩展 | 搜索发现相关页面已纳入 `djsdrama://` 解析范围 |

### 外部依赖

| 服务 / 框架 | 用途 | 接入方式 |
|-------------|------|---------|
| Next.js Route Handlers | Backend 搜索与热搜接口承载 | `backend/src/app/api/dramas/search/route.ts`、`backend/src/app/api/dramas/hot-search/route.ts` |
| Retrofit + OkHttp | Android 搜索/热搜请求 | `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` |
| URLSession + APIClient | iOS 搜索/热搜请求 | `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` |
| DataStore / UserDefaults | 移动端本地搜索历史 | Android `DataStore<Preferences>`、iOS `UserDefaults` |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| Web 搜索与榜单仍为占位页 | 搜索发现真实体验仅在 Android / iOS 可用 | 2026-07-27 | 属于明确范围外 |
| Backend 当前 route 仍接 `DramaMockRepository` | 搜索与热搜数据仍是 mock / 静态种子，不是线上真实内容服务 | 2026-07-27 | `backend/src/app/api/dramas/search/route.ts:15-17`、`backend/src/app/api/dramas/hot-search/route.ts:7-9` |
| Android 仍保留 `player` / `dramaDetail` 别名路由 | Wiki 需统一以 canonical `play` / `detail` 描述，但实际路由存在兼容入口 | 2026-07-27 | `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:40-43,90-104` |
| iOS 不兼容 `djsdrama://player/{id}` 历史 host | 跨端 deeplink 兼容性与 Android 不完全一致 | 2026-07-27 | `ios/ShortDrama/Sources/App/DeeplinkHandler.swift:27-44` |
| 真机 / 模拟器黑盒尚未执行 | 当前结论主要来自代码和自动化测试，设备级体验仍待补测 | 2026-07-27 | `docs/specs/2026-07-26-prd-04-search-discovery/qa-test.md:23-24,59-60` |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-28 | 更新：补充 PRD-12 剧场频道顶部搜索框会切回首页所属导航栈复用既有搜索发现页，不在剧场 tab 内重复实现搜索页面 |
| 2026-07-27 | 初始创建：收录 PRD-04 搜索发现的首页搜索入口、搜索发现页/结果页、快捷入口承接页、本地历史、热搜榜、deeplink 扩展与 Backend 搜索/热搜接口 |

---
*本文档由 llm-wiki skill 自动维护。*
