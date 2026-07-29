# 排行体系 (Ranking)

> 最后更新：2026-07-29
> 覆盖端：Android / iOS / Backend（Web 本期不实现）

## 功能概述

排行体系在既有搜索发现快捷入口和移动端 Native 路由承接页之上，补齐了“按榜单集中发现内容”的浏览链路。当前 Android 与 iOS 已同时从搜索页“排行”入口和剧场频道快捷入口进入真实排行页：普通“排行”默认请求 `contentType=all&type=hot&page=1&pageSize=10`，而剧场“预约”快捷入口会把排行页初始化到 `all + booking` 上下文；用户可在“全部 / 真人 / AI”与“热榜 / 推荐榜 / 预约榜”两个维度间切换，排行项继续复用既有 canonical `play` 路由语义进入播放器承接页。

与 PRD-09 评论系统相关的真实现状是：排行页本身没有接入评论入口，但它仍是当前仓库中最成熟的“需要登录动作”参考场景之一。未登录预约会先触发登录拦截，登录成功后再回到原榜单语义；评论写操作的“恢复容器上下文、不自动重放”设计在产品语义上参考了这类拦截模式，但已经独立实现了自己的 `CommentLoginContext`。

同时需要注意 Backend 当前的真实运行结构：
- `GET /api/dramas/rankings` 已升级到 `resolveOptionalAuthContext()` 的 canonical 可选鉴权写法；
- `POST /api/dramas/:id/book` 已升级到 `requireAuthContext()` + `getAuth(request)` 的强制鉴权写法；
- 但排行列表 route 通过 `getDramaRepository()` 获取默认仓储，按当前 `repository-registry.ts` 仍落到 `DramaMockRepository()`；
- 预约写接口则直接实例化 `DramaSupabaseRepository()`。

因此，当前排行体系应被描述为“鉴权 helper 已升级、列表默认数据源仍是 mock、预约写路径已走 Supabase repository”。

- **核心价值**：为热门、推荐与预约内容提供结构化聚合入口，补齐搜索发现页、剧场快捷入口到内容消费、登录拦截与预约提交的主路径
- **覆盖范围**：Backend 排行与预约接口、Android 排行页、iOS 排行页、搜索页 / 剧场快捷入口到播放页的导航链路、预约榜登录拦截与登录后继续操作
- **当前状态**：Android / iOS / Backend 已落地；Web 仅保留 `/rankings` 占位页，不实现真实榜单

## 入口与路由

| 端 | 入口 | 路由 / 触发方式 | 源文件 |
|----|------|----------------|--------|
| Android | 搜索发现快捷入口“排行” | `AppDestination.ranking()`，默认生成 `ranking?contentType=all&type=hot` | `android/app/src/main/java/com/djs66256/short_drama/feature/search/model/SearchQuickEntry.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` |
| Android | 剧场快捷入口“排行 / 预约” | `ranking?contentType=all&type=hot` / `ranking?contentType=all&type=booking`，先切回 `HOME` tab 再承载 | `android/app/src/main/java/com/djs66256/short_drama/navigation/TheaterShortcutRoute.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` |
| Android | Deeplink | `djsdrama://ranking`，进入 `PendingRoute.Ranking` 后导航至默认排行页 | `android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` |
| iOS | 搜索发现快捷入口“排行” | `QuickEntryType.ranking -> .rankingHome` | `ios/ShortDrama/Sources/Domain/Entities/QuickEntry.swift`、`ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift` |
| iOS | 剧场快捷入口“排行 / 预约” | `.openRanking(TheaterRankingEntryContext(rankingType: .hot/.booking))` | `ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift` |
| iOS | Deeplink | `djsdrama://ranking`，解析为 `.rankingHome` | `ios/ShortDrama/Sources/App/DeeplinkHandler.swift` |
| Android / iOS | 排行项播放入口 | 点击排行项后复用 `play` 语义进入播放页；Android 兼容 `player` 历史别名，iOS 仅公开 `play` | `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift`、`ios/ShortDrama/Sources/App/AppRoute.swift` |
| Android | 预约榜登录拦截 | `RankingEffect.RequireLogin(returnRoute)` → `login?returnRoute=ranking?...&source=ranking_booking` | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` |
| iOS | 预约榜登录拦截 | `.requireLogin(RankingLoginContext)` → `RankingRouteBuilder.loginContext(for:)` → `presentedLoginContext` | `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift`、`ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift`、`ios/ShortDrama/Sources/App/AppShellView.swift` |
| Backend | 排行 / 预约接口 | `GET /api/dramas/rankings`、`POST /api/dramas/:id/book` | `backend/src/app/api/dramas/rankings/route.ts`、`backend/src/app/api/dramas/[id]/book/route.ts` |
| Web | 首页代表性入口 | `/rankings` 仍为占位页，不消费真实排行数据 | `web/src/features/home/HomeScreen.tsx`、`web/src/app/rankings/page.tsx` |

## 核心逻辑

### 流程：从搜索页或剧场快捷入口进入排行页并浏览榜单

1. 搜索发现页或剧场快捷入口触发排行路由。
2. 排行页面首次进入时，默认使用 `all + hot + page=1 + pageSize=10`；若来自剧场预约入口则改为 `all + booking + page=1 + pageSize=10`。
3. 客户端切换一级 Tab（内容类型）或二级 Tab（榜单类型）时，只刷新当前变更的维度，并保留另一维度。
4. Backend `GET /api/dramas/rankings` 会先解析 query，再通过 `resolveOptionalAuthContext(request)` 读取可选登录态。
5. Route 层当前调用 `DramaService(getDramaRepository())`，而 `repository-registry.ts` 默认返回 `DramaMockRepository()`；因此排行列表默认数据源仍是 mock repository。
6. 若请求携带有效 bearer token，service 仍会获得 `{ userId }` 并补充 `is_booked` 等用户态字段。
7. 页面渲染双层 Tab、列表、空态、错误态和尾部分页状态。
8. 点击排行项后继续复用既有播放页路由。

### 流程：预约榜中的预约与登录拦截

1. 只有在预约榜维度下才展示预约按钮。
2. 未登录用户点击预约时，不直接提交接口，而是走登录拦截承接。
   - Android 发出 `RankingEffect.RequireLogin(returnRoute)`。
   - iOS 发出 `.requireLogin(RankingLoginContext)`。
3. 已登录用户点击预约后，调用 `POST /api/dramas/:id/book`。
4. Backend 预约接口已经对齐当前 canonical auth contract：`requireAuthContext()` 先强制校验登录态，再通过 `getAuth(request)` 读取 `userId`。
5. Route 当前直接实例化 `DramaSupabaseRepository()`，因此预约写路径已走 Supabase repository，而不是 mock repository。
6. 预约成功后，客户端只更新当前项的预约状态和预约数，不刷新整页。
7. Backend 预约行为当前是单向幂等 success，不支持取消预约。

### 流程：评论写操作与排行登录拦截模式的关系

1. PRD-09 已定稿：评论写操作登录成功后的恢复策略是“恢复评论抽屉上下文，不自动重放原发送 / 点赞动作”。
2. Android 排行预约中的 `RankingEffect.RequireLogin(returnRoute)` 仍可作为“拦截后保留来源页面语义”的参考模式。
3. iOS 排行预约中的 `RankingLoginContext` + `routeEffect = .requireLogin(...)` 同样可作为参考，但当前评论已独立实现自己的 `CommentLoginContext`。
4. 排行页本身没有接入 comments UI，也不是评论入口。
5. 评论写接口与预约写接口的共同点，是它们现在都已对齐当前 canonical auth helper，而不是早期 helper 命名。

### 边界与异常处理

| 场景 | 处理方式 | 源文件 |
|------|---------|--------|
| 首屏加载失败 | Android / iOS 都进入错误态并提供重试入口 | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt`、`ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` |
| 某维度无数据 | 两端展示空态文案，但保留 Tab 切换能力 | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt`、`ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` |
| 快速切换维度导致旧请求晚返回 | Android 使用 token / queryKey；iOS 使用 `requestToken`，只消费最后一次有效请求结果 | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt`、`ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` |
| 超大页码 | Backend 返回 200 + 空数组，并保留分页元信息 | `backend/src/app/api/__tests__/dramas-rankings.test.ts` |
| 排行 query 非法 | Backend 返回 400 `VALIDATION_ERROR` | `backend/src/app/api/__tests__/dramas-rankings.test.ts` |
| 预约未登录 | Backend 返回 401；客户端侧也有预校验拦截 | `backend/src/app/api/__tests__/dramas-book.test.ts`、`backend/src/middleware/auth.ts` |
| iOS 登录拦截承接 | 当前只展示登录弹层 / 提示，不在排行页内直接完成预约重放 | `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift` |
| Android 登录拦截承接 | 当前通过登录 route / 占位承接，保留 `returnRoute` | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` |

## 多端实现

### Android
- 页面与路由：`NavGraph` 已将 `ranking?contentType={contentType}&type={type}` 挂到真实 `RankingScreen`
- 状态源：`RankingUiState` 聚合 `selectedContentType / selectedRankingType / items / loading / refreshing / appending / appendError / bookingInFlightIds`
- 领域模型：`RankingContentType`、`RankingType`、`RankingQuery`、`RankingPage`、`RankingDrama`
- UI 指标：`RankingDramaItemUiModel` 会根据榜单类型切换展示“热度 / 推荐值 / 预约数”文案
- 登录拦截模式：`RankingEffect.RequireLogin(returnRoute)` 仍是最直接的 Android 侧“登录后返回原榜单语义”参考
- 自动化证据：`RoutesTest`、`DeeplinkRouteParserTest`、`RankingViewModelTest`

### iOS
- 页面与路由：`TabNavigationHostView` 将 `.rankingHome` 绑定到真实 `RankingHomeView`
- 状态源：`RankingViewModel` 使用 `ViewState + isAppending + appendErrorMessage + bookingErrorMessage + routeEffect`
- 领域模型：`RankingDrama`、`RankingQuery`、`BookDramaResult`
- 数据源：`DramaRemoteDataSource` 已接入 `/api/dramas/rankings` 与 `/api/dramas/{id}/book`
- 登录拦截模式：`RankingLoginContext` + `routeEffect = .requireLogin(...)`
- 自动化证据：`NavigationRouterTests`、`APIClientTests`、`RankingViewModelTests`

### Backend
- Route：`GET /api/dramas/rankings` 与 `POST /api/dramas/[id]/book`
- Service：`DramaService` 扩展 `listRankings()` 与 `bookDrama()`
- Repository Contract：`DramaRepositoryInterface` 扩展 `RankingParams`、`AuthContext`、`BookDramaParams`、`listRankings()`、`bookDrama()`
- Schema：`RankingDramaSchema`、`RankingQuerySchema`、`RankingListResponseSchema`、`BookDramaResponseSchema`
- 当前数据源：排行列表 route 默认走 `getDramaRepository()` → `DramaMockRepository()`；预约 route 直接走 `DramaSupabaseRepository()`
- 自动化证据：`dramas-rankings.test.ts` 覆盖默认 query、可选 auth、超大页码空数据、非法参数 400 与异常 500；`dramas-book.test.ts` 覆盖 verified bearer、未登录 401、非法 id 400 与 not found / internal error

### Web
- Web 首页只提供 `/rankings` 代表性链接，`/rankings` 页面本身仍是 `PlaceholderRouteScreen`
- 这与 `PRODUCT.md` 中“除 mall / earn 外其他业务页按 Native 实现”的范围约束一致

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/dramas/rankings` | [../../api/dramas.md](../../api/dramas.md) | 排行页唯一榜单数据源，支持 `type/contentType/page/pageSize`，并通过可选 auth 补充用户态 |
| `POST /api/dramas/:id/book` | [../../api/dramas.md](../../api/dramas.md) | 预约榜提交接口，要求当前认证上下文 |
| `POST /api/auth/sessions` / `GET /api/users/me` | [../../api/auth.md](../../api/auth.md) | 排行预约登录拦截成功后创建 / 校验会话 |
| `GET /api/dramas` | [../../api/dramas.md](../../api/dramas.md) | 与排行共享基础 `Drama` 字段集来源 |
| `POST /api/player/start` / `POST /api/player/stop` | [../../api/player.md](../../api/player.md) | 播放器仍复用既有主路径，排行项只负责跳转 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Android `RankingUiState` | `MutableStateFlow` | 页面级 | 排行页唯一状态源，聚合维度选择、列表、分页、错误与预约进行中状态 | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` |
| Android `latestQueryKey + token` | ViewModel 私有字段 | 页面级 | 避免切换 Tab / 翻页时旧响应覆盖新状态 | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` |
| Android `RankingEffect.RequireLogin(returnRoute)` | `SharedFlow<RankingEffect>` | 页面级 | 预约未登录时发出登录恢复上下文 | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` |
| iOS `selectedContentType / selectedRankingType / viewState` | `@Published` | 页面级 | 排行页的维度选择与内容态统一来源 | `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` |
| iOS `requestToken` | ViewModel 私有字段 | 页面级 | 避免首屏 / 翻页结果乱序落盘 | `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` |
| iOS `routeEffect` / `RankingLoginContext` | `@Published` | 页面级 | 预约未登录时发出结构化登录恢复上下文 | `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` |
| Backend `pagination` | JSON 响应 | 请求级 | 返回 `page / page_size / total / total_pages`，客户端据此判断是否还有下一页 | `backend/src/lib/schemas.ts`、`backend/src/repositories/interfaces/drama.repository.interface.ts` |
| Backend 排行数据源 | repository registry 默认值 | 应用级 | 当前默认列表数据源仍为 `DramaMockRepository()` | `backend/src/repositories/repository-registry.ts` |
| Backend 预约数据源 | 直接实例化 repository | 请求级 | 预约写请求当前直接走 `DramaSupabaseRepository()` | `backend/src/app/api/dramas/[id]/book/route.ts` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 搜索发现 | 入口来源 | 排行页从搜索发现快捷入口进入，不新增顶级频道 |
| 应用壳 | Native 容器承载 | Android / iOS 排行页都挂在首页 Tab / home graph 下 |
| 认证体系 | 预约拦截与登录回跳 | 预约榜未登录时统一进入登录流，登录成功后回到原榜单语义 |
| 剧场频道 | 快捷入口复用 | 剧场内“排行 / 预约”快捷入口复用既有排行页与播放主路径 |
| 播放器 | 路由跳转 | 排行项主动作统一复用 `play/:id` 语义 |
| 深链 | 外部入口 | `djsdrama://ranking` 可直接打开排行页 |
| 评论能力 | 登录拦截模式参考 | 排行提供可参考的登录拦截建模，但不直接承载 comments UI |
| 数据模型 | 共享字段约束 | `RankingDrama` 基于 `Drama` 扩展排行字段，客户端 DTO / Entity 与 Backend schema 需保持一致 |

### 外部依赖

| 服务 / 框架 | 用途 | 接入方式 |
|-------------|------|---------|
| Next.js Route Handlers | Backend 排行与预约接口承载 | `backend/src/app/api/dramas/rankings/route.ts`、`backend/src/app/api/dramas/[id]/book/route.ts` |
| Supabase Auth | 排行列表可选鉴权与预约接口强制鉴权 | `backend/src/middleware/auth.ts` |
| Supabase PostgreSQL | 预约持久化写入 | `backend/src/repositories/supabase/drama.supabase.repository.ts` |
| Retrofit | Android 拉取排行与提交预约 | `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` |
| URLSession + APIClient | iOS 拉取排行与提交预约 | `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| Web 不实现真实排行页 | Web 只能看到 `/rankings` 占位页，无法浏览真实榜单 | 2026-07-29 | 属于明确范围外，符合 `PRODUCT.md` Native 页面约束 |
| 排行列表默认仍使用 mock repository | 列表顺序、分页与部分用户态逻辑仍来自 mock 数据，而不是 Supabase 内容数据 | 2026-07-29 | `getDramaRepository()` 默认返回 `DramaMockRepository()` |
| 当前仅支持预约，不支持取消预约 | 已预约用户无法在排行页内撤销预约 | 2026-07-29 | Backend `bookDrama()` 只有单向幂等 success 语义 |
| iOS 不显式恢复更细粒度排行 query | 登录后回到 `.rankingHome`，不携带 `contentType/rankingType` 参数 | 2026-07-29 | 当前 iOS 路由层只公开 `rankingHome` |
| mall / earn 的 H5 容器尚未接入 | 文档只能记录产品策略，移动端仍显示 placeholder tab | 2026-07-29 | `PRODUCT.md` 明确它们由 H5 承载 |
| 设备级黑盒测试未执行 | 当前对真实点击、滚动分页、匿名 / 登录态预约的结论仍以代码、自动化测试与 QA 文档为主 | 2026-07-29 | 相关结论以代码为准 |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 更新：以当前代码为准重写排行体系文档，明确 route 已升级到 canonical auth helper、排行列表默认仍走 mock repository、预约写路径直接走 Supabase repository，并保留其对评论登录拦截语义的参考价值 |
| 2026-07-28 | 更新：补充 PRD-12 剧场“排行 / 预约”快捷入口会复用排行页，并分别记录 Android `ranking?contentType=all&type=booking` 与 iOS `TheaterRankingEntryContext` 的首屏上下文注入方案 |
| 2026-07-27 | 初始创建：收录 PRD-05 排行体系的搜索入口、双层 Tab、分页、预约拦截、`play` 路由复用、Backend 排行/预约接口与多端范围边界 |

---
*本文档由 llm-wiki skill 自动维护。*
