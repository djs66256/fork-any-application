# 排行体系 (Ranking)

> 最后更新：2026-07-29
> 覆盖端：Android / iOS / Backend（Web 本期不实现）

## 功能概述

排行体系在既有搜索发现快捷入口和移动端 Native 路由承接页之上，补齐了“按榜单集中发现内容”的浏览链路。当前 Android 与 iOS 都会从搜索页“排行”入口进入真实排行页，默认请求 `contentType=all&type=hot&page=1&pageSize=10`，并通过 Backend `GET /api/dramas/rankings` 返回分页榜单；用户可在“全部 / 真人 / AI”与“热榜 / 推荐榜 / 预约榜”两个维度间切换，排行项继续复用现有 canonical `play` 路由语义进入播放器承接页。PRD-08 进一步把预约榜的登录拦截和 Backend bearer 鉴权从早期骨架态 contract 升级为真实认证闭环：未登录用户先进入登录流，已登录用户再调用 `POST /api/dramas/:id/book` 提交预约；排行列表本身支持可选登录态，用于补充 `is_booked` 字段（`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:185-239`、`ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:114-157`、`backend/src/app/api/dramas/rankings/route.ts:8-24`、`backend/src/app/api/dramas/[id]/book/route.ts:16-28`）。商城（mall）与赚钱（earn）仍按 H5 承载，不属于本期排行实现范围（`PRODUCT.md:22-25`）。

- **核心价值**：为热门、推荐与预约内容提供结构化聚合入口，补齐搜索发现页到内容消费、登录拦截与预约提交的主路径
- **覆盖范围**：Backend 排行与预约接口、Android 排行页、iOS 排行页、搜索页快捷入口到播放页的导航链路、预约榜登录拦截与登录后继续操作
- **当前状态**：Android / iOS / Backend 已落地；Web 仅保留 `/rankings` 占位页，不实现真实榜单（`web/src/app/rankings/page.tsx:1-9`）

## 入口与路由

| 端 | 入口 | 路由 / 触发方式 | 源文件 |
|----|------|----------------|--------|
| Android | 搜索发现快捷入口“排行” | `AppDestination.ranking()`，默认生成 `ranking?contentType=all&type=hot` | `android/app/src/main/java/com/djs66256/short_drama/feature/search/model/SearchQuickEntry.kt:18-39`, `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:48-50,106-110` |
| Android | Deeplink | `djsdrama://ranking`，进入 `PendingRoute.Ranking` 后导航至默认排行页 | `android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt:33-41`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:89-92` |
| iOS | 搜索发现快捷入口“排行” | `QuickEntryType.ranking -> .rankingHome` | `ios/ShortDrama/Sources/Domain/Entities/QuickEntry.swift:20-26`, `ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift:76-87` |
| iOS | Deeplink | `djsdrama://ranking`，解析为 `.rankingHome` | `ios/ShortDrama/Sources/App/DeeplinkHandler.swift:26-45` |
| Android / iOS | 排行项播放入口 | 点击排行项后复用 `play` 语义进入播放页；Android 兼容 `player` 历史别名，iOS 仅公开 `play` | `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:44-50,94-109`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:200-208`, `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift:3-8`, `ios/ShortDrama/Sources/App/AppRoute.swift:39-60` |
| Android | 预约榜登录拦截 | `RankingEffect.RequireLogin(returnRoute)` → `login?returnRoute=ranking?...&source=ranking_booking` | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:185-205`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:205-219` |
| iOS | 预约榜登录拦截 | `.requireLogin(RankingLoginContext)` → `RankingRouteBuilder.loginContext(for:)` → `presentedLoginContext` | `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:122-126,146-150,210-217`, `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift:10-16`, `ios/ShortDrama/Sources/App/AppShellView.swift:17-34` |
| Backend | 排行 / 预约接口 | `GET /api/dramas/rankings`、`POST /api/dramas/:id/book` | `backend/src/app/api/dramas/rankings/route.ts:8-24`, `backend/src/app/api/dramas/[id]/book/route.ts:16-28` |
| Web | 首页代表性入口 | `/rankings` 仍为占位页，不消费真实排行数据 | `web/src/features/home/HomeScreen.tsx:27-50`, `web/src/app/rankings/page.tsx:1-9` |

## 核心逻辑

### 流程：从搜索页进入排行页并浏览榜单

1. 搜索发现页快捷入口触发排行路由。
   - Android “排行”快捷入口直接使用 `AppDestination.ranking()`（`android/app/src/main/java/com/djs66256/short_drama/feature/search/model/SearchQuickEntry.kt:18-39`）。
   - iOS “排行”快捷入口由 `SearchHomeViewModel.route(for:)` 映射到 `.rankingHome`（`ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift:76-87`）。
2. 排行页面首次进入时，默认使用 `all + hot + page=1 + pageSize=10` 作为查询参数。
   - Android 由 `SavedStateHandle` 读取 `contentType/type`，非法值回退到 `.ALL/.HOT`；初始 `refresh()` 固定请求第一页、每页 10 条（`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:58-91,242-278,415-420`）。
   - iOS `RankingViewModel` 默认 `selectedContentType = .all`、`selectedRankingType = .hot`，`RankingQuery` 也固定默认 `page=1,pageSize=10`（`ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:18-29,53-57,201-208`, `ios/ShortDrama/Sources/Domain/Entities/RankingQuery.swift:3-21`）。
   - Backend `RankingQuerySchema` 将 `type/contentType/page/pageSize` 收口为默认 `hot/all/1/10`（`backend/src/lib/schemas.ts:90-97`）。
3. 客户端切换一级 Tab（内容类型）或二级 Tab（榜单类型）时，只刷新当前变更的维度，并保留另一维度。
   - Android `onContentTypeSelected()` / `onRankingTypeSelected()` 都会重置分页并发起新请求（`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:105-127,242-268`）。
   - iOS `selectContentType(_:)` / `selectRankingType(_:)` 同样调用 `reloadFirstPage()`（`ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:59-73,163-199`）。
4. Backend 当前通过 `DramaSupabaseRepository.listRankings()` 直接查询 `dramas` 表，按 `content_type` 过滤、按榜单类型映射的排序列降序分页，并在登录态下额外查询 `bookings` 表补充每项 `is_booked`。
   - Route 先通过 `resolveOptionalAuthContext()` 解析可选 bearer access token，再把 `authContext.userId` 作为可选参数传入 service（`backend/src/app/api/dramas/rankings/route.ts:8-24`, `backend/src/middleware/auth.ts:65-67`）。
   - Repository 使用 `rankingSortColumn(type)` 选择排序字段，并在当前页 dramaIds 上查询同一用户的 booking 记录（`backend/src/repositories/supabase/drama.supabase.repository.ts:330-395`）。
5. 页面渲染双层 Tab、列表、空态、错误态和尾部分页状态。
   - Android `RankingScreen` 提供顶部返回、双层 `TabRow`、列表、空态、错误态、刷新遮罩和加载更多 footer（`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt:57-190,193-508`）。
   - iOS `RankingHomeView` 提供双层 Tab 视图，`RankingStateView` 负责内容态、空态、错误态与加载更多（`ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift:19-67`, `ios/ShortDrama/Sources/Features/Ranking/Views/Components/RankingStateView.swift:24-71`）。
6. 点击排行项后复用既有播放页路由。
   - Android 在 `NavGraph` 中把排行项点击映射为 `AppDestination.play(videoId)`（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:200-204`）。
   - iOS 通过 `RankingRouteBuilder.playRoute(for:)` 将 `drama.id` 映射为 `.player(videoId:)`，其对外公开路由名为 `play`（`ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift:3-8`, `ios/ShortDrama/Sources/App/AppRoute.swift:39-60`）。

### 流程：预约榜中的预约与登录拦截

1. 只有在预约榜维度下才展示预约按钮。
   - Android `RankingDramaCard` 仅在 `selectedRankingType == BOOKING` 时渲染按钮（`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt:210-213,298-309`）。
   - iOS 在预约榜下展示预约交互并通过 `RankingLoginContext` 记录来源（`ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:114-157,210-217`）。
2. 未登录用户点击预约时，不直接提交接口，而是走登录拦截承接。
   - Android 通过 `AuthSessionProvider.isLoggedIn()` 判断；未登录时发出 `RankingEffect.RequireLogin(returnRoute)`，携带当前 `ranking?...` 返回路由（`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:185-205`）。
   - iOS 未登录时设置 `bookingErrorMessage` 并发出 `.requireLogin(RankingLoginContext)`（`ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:122-126,146-150,210-217`）。
3. 已登录用户点击预约后，调用 `POST /api/dramas/:id/book`。
   - Android Retrofit 和 iOS `DramaRemoteDataSource` 都使用同一 RESTful path（`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt:39-48`, `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift:82-89,161-165`）。
4. Backend 对排行列表与预约提交的鉴权策略已升级：`GET /api/dramas/rankings` 通过 `resolveOptionalAuthContext()` 解析可选 bearer access token，未登录仍可浏览榜单但 `is_booked` 固定为 `false`；`POST /api/dramas/:id/book` 则通过 `requireAuthContext()` 强制要求真实登录态，并从 `getAuth(request)` 读取 userId（`backend/src/app/api/dramas/rankings/route.ts:8-24`、`backend/src/app/api/dramas/[id]/book/route.ts:16-28`、`backend/src/middleware/auth.ts:27-138`）。
5. 预约成功后，客户端只更新当前项的预约状态和预约数，不刷新整页。
   - Android 更新 `rawItems` 中目标项的 `isBooked/bookingCount` 并重新发布 UI（`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:213-226,380-390`）。
   - iOS 调用 `withBookingState(...)` 更新目标项并回写 `viewState`（`ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:133-141,219-223`, `ios/ShortDrama/Sources/Domain/Entities/RankingDrama.swift:58-81`）。
6. Backend 预约行为当前是单向幂等 success，不支持取消预约。
   - Repository 向 `bookings` 表插入 `user_id + drama_id`；若命中唯一约束 `23505`，则直接返回 `booked: true` 与当前 `booking_count`，首次插入成功后才递增 `dramas.booking_count`（`backend/src/repositories/supabase/drama.supabase.repository.ts:404-459`）。

### 边界与异常处理

| 场景 | 处理方式 | 源文件 |
|------|---------|--------|
| 首屏加载失败 | Android / iOS 都进入错误态并提供重试入口 | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:329-349`, `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt:164-169,427-452`, `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:181-199` |
| 某维度无数据 | 两端展示空态文案，但保留 Tab 切换能力 | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt:170-177,405-423`, `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:188-191` |
| 快速切换维度导致旧请求晚返回 | Android 使用 `activeRefreshToken/activeAppendToken + latestQueryKey`，iOS 使用 `requestToken`，只消费最后一次有效请求结果 | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:79-82,145-175,247-304,402-408`, `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:40,87-111,163-199` |
| 超大页码 | Backend 返回 200 + 空数组，并保留分页元信息 | `backend/src/app/api/__tests__/dramas-rankings.test.ts:90-115` |
| 排行 query 非法 | Backend 返回 400 `VALIDATION_ERROR` | `backend/src/app/api/__tests__/dramas-rankings.test.ts:117-124` |
| 预约未登录 | Backend 返回 401；客户端侧也有预校验拦截 | `backend/src/app/api/__tests__/dramas-book.test.ts:66-77`, `backend/src/middleware/auth.ts:25-32` |
| Android 旧 `player` deeplink | Android 继续兼容 `djsdrama://player/{id}`；iOS 不兼容该历史 host | `android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt:33-36`, `ios/ShortDrama/Sources/App/DeeplinkHandler.swift:26-45` |

## 多端实现

### Android

- 页面与路由：`NavGraph` 已将 `ranking?contentType={contentType}&type={type}` 从占位承接页替换为真实 `RankingScreen`（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:187-209`）
- 状态源：`RankingUiState` 聚合 `selectedContentType / selectedRankingType / items / loading / refreshing / appending / appendError / bookingInFlightIds`（`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:31-44`）
- 领域模型：`RankingContentType`、`RankingType`、`RankingQuery`、`RankingPage`、`RankingDrama` 形成 Android 侧完整排行实体集（`android/app/src/main/java/com/djs66256/short_drama/domain/model/RankingQuery.kt:3-54`, `android/app/src/main/java/com/djs66256/short_drama/domain/model/RankingDrama.kt:3-19`）
- DTO 对齐：`RankingDramaDto` 负责 snake_case 字段解码和 `content_type` 到 enum 的转换（`android/app/src/main/java/com/djs66256/short_drama/data/dto/RankingDramaDto.kt:8-52`）
- UI 指标：`RankingDramaItemUiModel` 根据榜单类型切换展示“热度 / 推荐值 / 预约数”文案（`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/model/RankingUiModel.kt:6-71`）
- 自动化证据：`RoutesTest` 覆盖默认排行路由与 query 编码；`DeeplinkRouteParserTest` 覆盖 `ranking` / `player` host；`RankingViewModelTest` 的 `T-09 anonymous booking emits require login and skips api call` 已验证未登录预约时发出 `RequireLogin("ranking?contentType=all&type=booking")` 且不会调用预约 API（`android/app/src/test/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModelTest.kt:416-449`）

### iOS

- 页面与路由：`TabNavigationHostView` 已将 `.rankingHome` 绑定到 `RankingHomeView(isUserLoggedIn: { authStore.isAuthenticated })`，使排行页能直接感知全局登录态（`ios/ShortDrama/Sources/App/TabNavigationHostView.swift:9-31`）
- 状态源：`RankingViewModel` 使用 `ViewState + isAppending + appendErrorMessage + bookingErrorMessage + routeEffect` 承载页面状态（`ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:7-41`）
- 领域模型：`RankingDrama`、`RankingQuery`、`BookDramaResult` 构成 iOS 侧排行实体与预约结果（`ios/ShortDrama/Sources/Domain/Entities/RankingDrama.swift:3-82`, `ios/ShortDrama/Sources/Domain/Entities/RankingQuery.swift:3-21`, `ios/ShortDrama/Sources/Domain/Entities/BookDramaResult.swift:3-8`）
- 数据源：`DramaRemoteDataSource` 已接入 `/api/dramas/rankings` 与 `/api/dramas/{id}/book`（`ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift:65-89,155-165`）
- 页面交互：`RankingHomeView` 负责双层 Tab、播放跳转、登录提示弹窗与分页触发（`ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift:19-109`）
- 自动化证据：`NavigationRouterTests` 已覆盖 `.rankingHome` 的 home-tab 归属和公开路由名 `ranking`（`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift:55-73`）；`APIClientTests` 已覆盖 `/api/dramas/rankings` canonical payload 解码（`ios/ShortDrama/Tests/DataTests/APIClientTests.swift:292-360`）

### Backend

- Route：新增 `GET /api/dramas/rankings` 与 `POST /api/dramas/[id]/book`（`backend/src/app/api/dramas/rankings/route.ts:8-24`, `backend/src/app/api/dramas/[id]/book/route.ts:16-28`）
- Service：`DramaService` 扩展 `listRankings()` 与 `bookDrama()`，统一校验 repository 输出（`backend/src/services/drama/drama.service.ts:44-78`）
- Repository Contract：`DramaRepositoryInterface` 扩展 `RankingParams`、`AuthContext`、`BookDramaParams`、`listRankings()`、`bookDrama()`（`backend/src/repositories/interfaces/drama.repository.interface.ts:21-62`）
- Schema：`RankingDramaSchema`、`RankingQuerySchema`、`RankingListResponseSchema`、`BookDramaResponseSchema` 定义了排行和预约接口的权威返回形态（`backend/src/lib/schemas.ts:30-44,75-105,154-160`）
- 当前数据源：运行时已直接实例化 `DramaSupabaseRepository()`，排行的 `is_booked` 与预约写入都走真实 Supabase repository 路径，而不再使用早期 `DramaMockRepository()`（`backend/src/app/api/dramas/rankings/route.ts:17-23`, `backend/src/app/api/dramas/[id]/book/route.ts:20-27`, `backend/src/repositories/supabase/drama.supabase.repository.ts:330-459`）
- 自动化证据：`dramas-rankings.test.ts` 覆盖默认 query、可选 auth、超大页码空数据、非法参数 400 与异常 500；`dramas-book.test.ts` 覆盖真实 bearer、未登录 401、非法 id 400 与 not found / internal error（`backend/src/app/api/__tests__/dramas-rankings.test.ts:39-138`, `backend/src/app/api/__tests__/dramas-book.test.ts:18-133`）

### Web

- Web 首页只提供 `/rankings` 代表性链接，`/rankings` 页面本身仍是 `PlaceholderRouteScreen`（`web/src/features/home/HomeScreen.tsx:27-50`, `web/src/app/rankings/page.tsx:1-9`）
- 这与 `PRODUCT.md` 中“除 mall / earn 外其他业务页按 Native 实现”的范围约束一致（`PRODUCT.md:22-25`）

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/dramas/rankings` | [../../api/dramas.md](../../api/dramas.md) | 排行页唯一榜单数据源，支持 `type/contentType/page/pageSize` |
| `POST /api/dramas/:id/book` | [../../api/dramas.md](../../api/dramas.md) | 预约榜提交接口，要求真实登录态 |
| `POST /api/auth/sessions` / `GET /api/users/me` | [../../api/auth.md](../../api/auth.md) | 排行预约登录拦截成功后创建 / 校验会话 |
| `GET /api/dramas` | [../../api/dramas.md](../../api/dramas.md) | 与排行共享基础 `Drama` 字段集来源 |
| `POST /api/player/start` / `POST /api/player/stop` | [../../api/player.md](../../api/player.md) | 播放器仍是占位能力，排行项当前只负责跳转，不新增播放 API |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Android `RankingUiState` | `MutableStateFlow` | 页面级 | 排行页唯一状态源，聚合维度选择、列表、分页、错误与预约进行中状态 | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:31-44,65-77` |
| Android `latestQueryKey + token` | ViewModel 私有字段 | 页面级 | 避免切换 Tab / 翻页时旧响应覆盖新状态 | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:79-82,397-413` |
| iOS `selectedContentType / selectedRankingType / viewState` | `@Published` | 页面级 | 排行页的维度选择与内容态统一来源 | `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:23-30` |
| iOS `requestToken` | ViewModel 私有字段 | 页面级 | 避免首屏/翻页结果乱序落盘 | `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:35-41,87-111,163-199` |
| iOS `routeEffect` | `@Published` | 页面级 | 预约未登录时发出登录拦截承接信号 | `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:14-16,29,122-126` |
| Backend `pagination` | JSON 响应 | 请求级 | 返回 `page / page_size / total / total_pages`，客户端据此判断是否还有下一页 | `backend/src/lib/schemas.ts:61-79`, `backend/src/repositories/interfaces/drama.repository.interface.ts:46-54` |
| Backend `bookings` 表 + 请求内 `Set<string>` | Supabase 行数据 + Repository 临时集合 | 持久化级 / 请求级 | `listRankings()` 先查询当前用户当前页 dramaIds 的 booking 记录并落入 `Set` 计算 `is_booked`；`bookDrama()` 通过插入 `bookings` 表与唯一约束实现幂等预约 | `backend/src/repositories/supabase/drama.supabase.repository.ts:361-378,404-459` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 搜索发现 | 入口来源 | 排行页从搜索发现快捷入口进入，不新增顶级频道 |
| 应用壳 | Native 容器承载 | Android/iOS 排行页都挂在首页 Tab / home graph 下 |
| 认证体系 | 预约拦截与登录回跳 | 预约榜未登录时统一进入登录流，登录成功后回到 profile 或 ranking 语义 |
| 播放器 | 路由跳转 | 排行项主动作统一复用 `play/:id` 语义 |
| 深链 | 外部入口 | `djsdrama://ranking` 可直接打开排行页；Android 继续兼容 `player` 历史播放 host |
| 数据模型 | 共享字段约束 | `RankingDrama` 基于 `Drama` 扩展排行字段，客户端 DTO / Entity 与 Backend schema 需保持一致 |

### 外部依赖

| 服务 / 框架 | 用途 | 接入方式 |
|-------------|------|---------|
| Next.js Route Handlers | Backend 排行与预约接口承载 | `backend/src/app/api/dramas/rankings/route.ts`, `backend/src/app/api/dramas/[id]/book/route.ts` |
| Supabase Auth | 排行列表的可选鉴权与预约接口的真实 bearer 校验 | `backend/src/middleware/auth.ts` |
| Supabase PostgreSQL | 排行数据、预约状态与幂等写入持久化 | `backend/src/repositories/supabase/drama.supabase.repository.ts` |
| Retrofit | Android 拉取排行与提交预约 | `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` |
| URLSession + APIClient | iOS 拉取排行与提交预约 | `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| Web 不实现真实排行页 | Web 只能看到 `/rankings` 占位页，无法浏览真实榜单 | 2026-07-29 | 属于明确范围外，符合 `PRODUCT.md` Native 页面约束 |
| 当前仅支持预约，不支持取消预约 | 已预约用户无法在排行页内撤销预约 | 2026-07-29 | Backend `bookDrama()` 只有单向幂等 success 语义 |
| iOS 不兼容 `player` 历史 deeplink host | 旧播放 deeplink 兼容仅在 Android 保留 | 2026-07-29 | iOS 仅解析 `play` |
| iOS 登录回跳只保留榜单页语义，不显式恢复更细粒度 query | 登录后回到 `.rankingHome`，不携带 `contentType/rankingType` 参数 | 2026-07-29 | 当前 iOS 路由层只公开 `rankingHome` |
| mall / earn 的 H5 容器尚未接入 | 文档仅能记录产品策略，当前移动端仍显示 placeholder tab，而非真实 WebView/WKWebView 容器 | 2026-07-29 | `PRODUCT.md:22-25`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:274-295`, `ios/ShortDrama/Sources/App/TabNavigationHostView.swift:37-43` |
| 设备级黑盒测试未执行 | 当前对真实点击、滚动分页、匿名/登录态预约的结论仍以代码、自动化测试与 QA 文档为主 | 2026-07-29 | `docs/specs/2026-07-28-prd-08-login/qa-test.md:1-40` |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 更新：同步 PRD-08 登录闭环后排行预约的真实 bearer 鉴权、登录拦截、Supabase repository 运行时、Android `T-09` 自动化证据与当前限制 |
| 2026-07-27 | 初始创建：收录 PRD-05 排行体系的搜索入口、双层 Tab、分页、预约拦截、`play` 路由复用、Backend 排行/预约接口与多端范围边界 |

---
*本文档由 llm-wiki skill 自动维护。*