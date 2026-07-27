# Android 端技术方案：PRD-05 排行体系

> 创建日期：2026-07-27
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

Android 端在现有单 Activity + Navigation Compose + Hilt + ViewModel + Repository 架构上，将搜索发现页已有的 `ranking` Native 承接路由从占位页替换为真实排行页。实现继续遵循 `android/CLAUDE.md` 约束，不新增第三方依赖，不引入 Paging/图片加载库；分页、Tab 切换、请求去重、预约按钮状态与登录拦截占位均由现有 Compose + StateFlow + Retrofit 体系完成。

```text
SearchHomeScreen quick entry
  -> AppDestination.ranking(contentType=all, type=hot)
     -> RankingScreen
        -> collects RankingUiState from RankingViewModel
           -> GetRankingsUseCase(query)
              -> RankingRepository.getRankings(query)
                 -> RankingRemoteDataSource.getRankings(...)
                    -> ApiService.getDramaRankings(...)
                       -> GET /api/dramas/rankings
        -> click ranking row
           -> onOpenPlay(drama.id)
              -> AppDestination.play(videoId)
        -> click booking button (booking tab only)
           -> AuthSessionProvider.isLoggedIn()
              -> false: emit RequireLogin(returnRoute)
              -> true: BookDramaUseCase(dramaId)
                        -> RankingRepository.bookDrama(dramaId)
                           -> RankingRemoteDataSource.bookDrama(dramaId)
                              -> ApiService.bookDrama(id)
                                 -> POST /api/dramas/{id}/book
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | `ranking` 从固定占位 route 扩展为可携带默认筛选参数的真实页面 route，仍保持 canonical 语义为 `ranking` |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 用 `RankingScreen` 替换当前 `PlaceholderScreen(title = "排行")`，并继续复用现有 `play/{videoId}` 导航 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/model/SearchQuickEntry.kt` | 不变 / 联动验证 | 快捷入口仍调用 `AppDestination.ranking()`，无需新增入口 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增排行榜查询与预约提交接口定义 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt` | 不变 | 继续保留现有认证拦截器占位，不在本 PRD 扩大登录实现范围 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/Drama.kt` | 不变 | 首页/搜索继续使用现有 `Drama`，排行新增独立 `RankingDrama` 避免污染既有消费方 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt` | 不变 | 首页列表链路继续走现有数据源；排行单独使用 `RankingRemoteDataSource` |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt` | 不变 | 避免将排行/预约语义混入首页仓储 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt` | 不变 | 播放器仍为占位页，排行项点击仅导航到现有 `play/{videoId}` |
| `android/app/src/test/java/com/djs66256/short_drama/...` | 新增 / 修改 | 新增排行 ViewModel、Repository、DTO、route 测试，并补充 `NavGraph` / `RoutesTest` 覆盖 |

### 1.2 设计原则

- 只新增 Android 端实现所需文件，不变更 Web / iOS 方案。
- 不新增图片加载、分页、事件总线等第三方依赖；分页用 `LazyListState` + ViewModel 手动实现。
- 排行页与共享 `design.md` 保持一致：默认 `all + hot + page=1 + pageSize=10`，切换任一 Tab 时回到第一页。
- 播放跳转只复用现有 `play/{videoId}`；不新增 `player` 之外的新别名，不改历史兼容策略。
- 登录能力未就绪时，排行模块只负责发出 `RequireLogin(returnRoute, dramaId)` 信号，不在本 PRD 内实现真实登录系统。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt` | 新增 | 排行页根 Composable，承载顶部栏、双层 Tab、列表、空态、错误态、分页尾部与预约按钮 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | 新增 | 管理默认加载、Tab 切换、分页、请求去重、预约按钮状态、登录拦截事件 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/model/RankingUiModel.kt` | 新增 | 封装榜单展示值、榜单序号、按钮可用态等 UI 模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/RankingDrama.kt` | 新增 | 排行项领域实体，承载 `contentType/playCount/bookingCount/recommendationScore/isBooked` |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/RankingQuery.kt` | 新增 | 定义 `RankingContentType`、`RankingType`、`RankingQuery`、`RankingPage` |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/BookDramaResult.kt` | 新增 | 预约成功结果实体 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/RankingRepository.kt` | 新增 | 定义排行查询与预约接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthSessionProvider.kt` | 新增 | 抽象登录态查询；首版提供始终未登录的占位实现，待 PRD-08 接管 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetDramaRankingsUseCase.kt` | 新增 | 排行列表用例 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/BookDramaUseCase.kt` | 新增 | 预约操作用例 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/RankingDramaDto.kt` | 新增 | 排行接口 DTO，兼容 `cover_url = null` 与新增排行字段 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/RankingListResponseDto.kt` | 新增 | `data + pagination` 响应 DTO，复用 `PaginationDto` |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/BookDramaResponseDto.kt` | 新增 | 预约结果 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/RankingRemoteDataSource.kt` | 新增 | 按 `SearchRemoteDataSource` 的错误解析模式封装排行/预约接口 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/RankingRepositoryImpl.kt` | 新增 | DTO -> Domain 映射、分页信息透传、预约结果转换 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增 `getDramaRankings`、`bookDrama` Retrofit 定义 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 注入 `RankingRepository`、`AuthSessionProvider` |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt` | 轻微修改 | 如需提供默认占位 `AuthSessionProvider`，在此集中注册 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | `ranking` route 增加可选 query args，用于默认参数与登录回跳承接 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 注册真实 `RankingScreen`，并将播放导航透传给排行页 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModelTest.kt` | 新增 | 覆盖状态机、分页、预约、登录拦截、并发保护 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/RankingRepositoryImplTest.kt` | 新增 | 覆盖 DTO 映射、错误透传、封面缺失兼容 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 补充 `ranking(contentType, type)` route 构造与默认值断言 |

---

## 3. UI 层设计

### 3.1 组件层级树

```text
RankingScreen
├── RankingTopBar
│   ├── BackButton
│   └── Title("排行")
├── RankingContentTypeTabRow
│   ├── AllTab
│   ├── LiveActionTab
│   └── AITab
├── RankingTypeTabRow
│   ├── HotTab
│   ├── RecommendTab
│   └── BookingTab
└── RankingBody
    ├── RankingLoadingState
    ├── RankingErrorState
    ├── RankingEmptyState
    └── LazyColumn
        ├── RankingDramaCard (items)
        │   ├── RankIndexBadge
        │   ├── CoverPlaceholder
        │   ├── Title / Meta / Tags
        │   ├── MetricChip
        │   └── BookingButton (booking tab only)
        └── RankingAppendFooter
            ├── LoadingFooter
            ├── AppendErrorFooter
            └── NoMoreFooter
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `RankingScreen` | Composable | 排行页根容器，连接 ViewModel 与导航回调 | 否 |
| `RankingTopBar` | Composable | 返回按钮与标题 | 否 |
| `RankingContentTypeTabRow` | Composable | 一级 Tab：全部 / 真人 / AI | 否 |
| `RankingTypeTabRow` | Composable | 二级 Tab：热榜 / 推荐榜 / 预约榜 | 否 |
| `RankingDramaCard` | Composable | 渲染排行项、榜单指标、预约按钮与整卡点击 | 否 |
| `RankingMetricChip` | Composable | 根据当前榜单类型显示热度/推荐值/预约数 | 是 |
| `RankingBookingButton` | Composable | 预约按钮、loading、已预约态 | 是 |
| `RankingLoadingState` | Composable | 首屏/切 Tab loading | 是 |
| `RankingErrorState` | Composable | 首屏错误态与重试 | 是 |
| `RankingEmptyState` | Composable | 空榜单态 | 是 |
| `RankingAppendFooter` | Composable | 加载更多/分页失败/没有更多 | 是 |

### 3.3 Composable 接口定义

```kotlin
@Composable
fun RankingScreen(
    onBack: () -> Unit,
    onOpenPlay: (String) -> Unit,
    onRequireLogin: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RankingViewModel = hiltViewModel(),
)

@Composable
fun RankingDramaCard(
    item: RankingDramaItemUiModel,
    onOpenPlay: () -> Unit,
    onBook: () -> Unit,
    modifier: Modifier = Modifier,
)
```

说明：
- `onOpenPlay` 只接收 `drama.id`，最终由 `NavGraph` 导航到现有 `AppDestination.play(videoId)`。
- `onRequireLogin` 接收 `returnRoute`，排行模块不关心登录页具体实现，只负责把当前筛选上下文交给应用壳。
- 首版不复用 `HomeDramaCard`，因为排行页需要整卡点击、榜单指标、排行序号、预约按钮与分页尾部，语义已明显不同。

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| `AppDestination` → `RankingViewModel` | `SavedStateHandle` | 初始化默认 `contentType/type`，支持登录后回跳还原筛选 |
| `RankingViewModel` → `RankingScreen` | `StateFlow<RankingUiState>` | 页面状态渲染 |
| `RankingViewModel` → `RankingScreen` | `SharedFlow<RankingEffect>` | 一次性事件：打开登录拦截、展示轻提示 |
| `RankingScreen` → `RankingDramaCard` | Composable 参数 | 列表项渲染与点击行为 |
| `RankingDramaCard` → `RankingViewModel` | Lambda Callback | 点击排行项、点击预约、触底加载更多 |
| `NavGraph` → Player | `navController.navigate(AppDestination.play(id))` | 排行项进入播放占位页 |

### 3.5 交互与状态细节

- 首次进入排行页时，页面顶部不展示更新时间，只展示返回按钮与标题“排行”。
- 一级 Tab 切换只改变 `contentType`；二级 Tab 切换只改变 `type`；切换任一维度时：
  - 清空旧列表
  - `page` 重置为 1
  - 列表滚动回顶部
  - 发起新请求
- 排行项整卡点击进入播放页；若 `drama.id` 为空则忽略点击并显示轻量反馈。
- 预约按钮只在 `type == BOOKING` 时显示；热榜/推荐榜只展示指标，不展示按钮。
- Android 端当前未接入真实图片加载能力，因此继续使用封面占位视图，不新增 Coil/Glide。

### 3.6 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 小屏手机 | 单列 `LazyColumn` | 与首页 Feed 保持一致，不引入双列布局 |
| 横竖屏 | `SavedStateHandle` + `rememberLazyListState` | 旋转后保留当前 Tab，列表数据可按当前 query 恢复 |
| 字体缩放 | Material3 Typography + 文本多行截断 | 标题最多 2 行，标签区自动换行或截断 |
| 深色模式 | 复用 `ShortDramaTheme` | Tab、卡片、空态、错误态全部使用主题色 |
| 缺封面 | 占位块 + 图标 | `cover_url` 为空不影响布局 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 UI | 职责 |
|-----------|---------|------|
| `RankingViewModel` | `RankingScreen` | 管理默认加载、双层 Tab 选择、分页、旧请求失效保护、预约按钮状态、登录拦截事件 |

### 4.2 状态定义

```kotlin
data class RankingUiState(
    val selectedContentType: RankingContentType = RankingContentType.ALL,
    val selectedRankingType: RankingType = RankingType.HOT,
    val items: List<RankingDramaItemUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isAppending: Boolean = false,
    val appendErrorMessage: String? = null,
    val errorMessage: String? = null,
    val page: Int = 1,
    val hasNextPage: Boolean = false,
    val hasLoadedOnce: Boolean = false,
    val bookingInFlightIds: Set<String> = emptySet(),
)

sealed interface RankingEffect {
    data class RequireLogin(val returnRoute: String) : RankingEffect
    data class ShowMessage(val message: String) : RankingEffect
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `selectedContentType` | `RankingContentType` | `ALL` | 一级 Tab 选中值 |
| `selectedRankingType` | `RankingType` | `HOT` | 二级 Tab 选中值 |
| `items` | `List<RankingDramaItemUiModel>` | `emptyList()` | 当前榜单列表 |
| `isLoading` | `Boolean` | `true` | 首次进入或初始失败后重试 |
| `isRefreshing` | `Boolean` | `false` | 切换 Tab 时的重置加载 |
| `isAppending` | `Boolean` | `false` | 加载下一页 |
| `appendErrorMessage` | `String?` | `null` | 尾部分页失败提示，不清空已有列表 |
| `errorMessage` | `String?` | `null` | 首屏/切换维度失败提示 |
| `page` | `Int` | `1` | 当前已成功合并到列表的页码 |
| `hasNextPage` | `Boolean` | `false` | 是否还能继续加载 |
| `hasLoadedOnce` | `Boolean` | `false` | 是否完成过至少一次请求 |
| `bookingInFlightIds` | `Set<String>` | `emptySet()` | 处于预约请求中的 dramaId 集合 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | UI 层表现 |
|---------|---------|----------|
| Initial Loading | `isLoading && !hasLoadedOnce` | 全页 loading |
| Refreshing | `isRefreshing` | 保留 Tab 选中态，列表区展示骨架/进度状态 |
| Success | `!isLoading && items.isNotEmpty() && errorMessage == null` | 正常列表 + 尾部状态 |
| Empty | `hasLoadedOnce && items.isEmpty() && errorMessage == null && !isLoading` | 空态文案“当前榜单暂无内容” |
| Error | `errorMessage != null && items.isEmpty()` | 全页错误态 + 重试按钮 |
| Append Error | `appendErrorMessage != null && items.isNotEmpty()` | 列表尾部错误提示 + 重试 |

### 4.5 关键行为设计

#### 初始化与筛选

- `RankingViewModel` 从 `SavedStateHandle` 读取 route 初始值；缺失时回退到 `ALL + HOT`。
- 默认请求参数固定为 `contentType=all&type=hot&page=1&pageSize=10`。
- `onContentTypeSelected()` 与 `onRankingTypeSelected()` 会复用同一 `refresh(query)` 逻辑，但只更新对应维度。

#### 请求去重与乱序保护

- ViewModel 内部维护 `activeRequestToken: Long` 与 `activeQueryKey(contentType, type)`。
- 每次首屏/切换/分页请求都生成新 token；返回时只有 token 与当前 queryKey 同时匹配才允许写入状态。
- 这样可保证“用户快速连续切换 Tab 时，旧请求晚于新请求返回也不会覆盖新状态”。

#### 分页

- 使用 `page + hasNextPage + isAppending` 三元组控制加载更多。
- 触底时如果 `isAppending == true` 或 `hasNextPage == false`，直接忽略。
- 切换任一 Tab 时清空 `appendErrorMessage`，重新从第一页开始。

#### 预约

- 点击预约时先校验 `drama.id` 非空，再检查 `AuthSessionProvider.isLoggedIn()`。
- 若未登录：不发网络请求，发出 `RankingEffect.RequireLogin(returnRoute)`。
- 若已登录：
  - 将该 `dramaId` 放入 `bookingInFlightIds`
  - 调用 `BookDramaUseCase`
  - 成功后仅原位更新当前列表项的 `isBooked=true` 与 `bookingCount`，并移除 loading
  - 不做立即重排，避免预约榜列表闪动；后续刷新以服务端排序结果为准

### 4.6 推荐的辅助建模

```kotlin
data class RankingRequestKey(
    val contentType: RankingContentType,
    val rankingType: RankingType,
)

enum class RankingContentType(val apiValue: String, val label: String) {
    ALL("all", "全部"),
    LIVE_ACTION("live_action", "真人"),
    AI("ai", "AI"),
}

enum class RankingType(val apiValue: String, val label: String) {
    HOT("hot", "热榜"),
    RECOMMEND("recommend", "推荐榜"),
    BOOKING("booking", "预约榜"),
}
```

---

## 5. Navigation 路由设计

### 5.1 导航方案

继续使用 Jetpack Navigation Compose，排行页仍归属 `home_graph`，以保证：
- 搜索发现页快捷入口无须变更；
- 返回路径稳定回到搜索页/首页上下文；
- 排行项点击可直接复用已有 `play/{videoId}` route。

### 5.2 路由清单

| 路由标识 | 目标 Composable | 参数 | 导航方式 | 说明 |
|---------|----------------|------|---------|------|
| `ranking?contentType={contentType}&type={type}` | `RankingScreen` | `contentType`、`type`，均有默认值 | `NavController.navigate(AppDestination.ranking(...))` | 真实排行页 |
| `play/{videoId}` | `PlayerScreen` | `videoId = drama.id` | `navController.navigate(AppDestination.play(id))` | 复用现有播放器占位承接 |
| `player/{videoId}` | `PlayerScreen` | `videoId` | 保持兼容 | 本 PRD 不修改 |

### 5.3 路由定义建议

```kotlin
object Route {
    const val RANKING = "ranking?contentType={contentType}&type={type}"
}

fun ranking(
    contentType: RankingContentType = RankingContentType.ALL,
    type: RankingType = RankingType.HOT,
): String = "ranking?contentType=${contentType.apiValue}&type=${type.apiValue}"
```

说明：
- route 主语义仍是 `ranking`，只是增加可选 query args，用于默认值恢复和登录回跳；不算新增新的顶级导航语义。
- 搜索发现页快捷入口仍可直接调用 `AppDestination.ranking()`，得到默认的 `all + hot`。
- 若后续 PRD-08 登录页需要回跳，只需保留 `returnRoute = AppDestination.ranking(selectedContentType, selectedRankingType)`。

### 5.4 导航图

```kotlin
composable(
    route = AppDestination.Route.RANKING,
    arguments = listOf(
        navArgument("contentType") { defaultValue = "all" },
        navArgument("type") { defaultValue = "hot" },
    ),
) {
    RankingScreen(
        onBack = { navController.popBackStack() },
        onOpenPlay = { videoId -> navController.navigate(AppDestination.play(videoId)) },
        onRequireLogin = { returnRoute ->
            // 由应用壳 / 后续 PRD-08 统一承接
        },
    )
}
```

### 5.5 Deep Link 处理

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `djsdrama://ranking` | `PendingRoute.Ranking` | 使用默认 `all + hot` |
| `djsdrama://play/{id}` | `play/{videoId}` | 复用现有逻辑 |
| `djsdrama://player/{id}` | `player/{videoId}` | 复用现有兼容逻辑 |

说明：
- 本 PRD 不强制扩展 `djsdrama://ranking` 的 query 参数解析；内部回跳通过 route string 已足够。
- 外部 deeplink 到排行页时默认进入 `全部 + 热榜`，与 spec 保持一致。

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | Retrofit + OkHttp | 复用现有 `ApiClient`，继续从 `AppConfig.apiBaseUrl` 获取 base URL |
| 数据模型 | kotlinx.serialization DTO | 新增 `RankingDramaDto`、`RankingListResponseDto`、`BookDramaResponseDto` |
| 请求数据源 | `RankingRemoteDataSource` | 参考 `SearchRemoteDataSource`，支持解析后端错误包体 |
| Repository | `RankingRepositoryImpl` | DTO -> Domain 映射，分页与预约结果透传 |
| 错误处理 | `ApiResult<T>` | 继续使用 `Success / Error / Exception` |

### 6.2 API 接口定义

```kotlin
interface ApiService {
    @GET("dramas/rankings")
    suspend fun getDramaRankings(
        @Query("type") type: String,
        @Query("contentType") contentType: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10,
    ): RankingListResponseDto

    @POST("dramas/{id}/book")
    suspend fun bookDrama(
        @Path("id") id: String,
    ): BookDramaResponseDto
}
```

说明：
- 相对路径写法延续当前 `ApiService` 风格，由 `ApiClient.normalizeApiBaseUrl()` 保证最终命中 `/api/...`。
- 不新增 header 常量；认证仍通过现有 `AuthInterceptor` 预留能力承接。
- `GET /api/dramas/rankings` 是公开接口；`POST /api/dramas/{id}/book` 只有在 `AuthSessionProvider` 判定已登录时才会在客户端发出。

### 6.3 DTO 设计

```kotlin
@Serializable
data class RankingDramaDto(
    val id: String,
    val title: String,
    val description: String,
    @SerialName("cover_url") val coverUrl: String? = null,
    val category: String,
    @SerialName("episode_count") val episodeCount: Int,
    val tags: List<String> = emptyList(),
    val rating: Double = 0.0,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("content_type") val contentType: String,
    @SerialName("play_count") val playCount: Int,
    @SerialName("booking_count") val bookingCount: Int,
    @SerialName("recommendation_score") val recommendationScore: Double,
    @SerialName("is_booked") val isBooked: Boolean = false,
)
```

关键点：
- `coverUrl` 设计为 nullable，映射时 `orEmpty()`，匹配 spec 中“封面缺失不崩溃”的要求。
- 分页 DTO 继续复用现有 `PaginationDto`，避免重复建模。

### 6.4 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 首屏加载失败 | 0 | 无 | 用户点击重试 |
| 切换 Tab 失败 | 0 | 无 | 保持当前选中态与错误提示 |
| 分页失败 | 0 | 无 | 尾部展示重试入口 |
| 预约失败 | 0 | 无 | 保持原按钮态，可再次点击 |
| 401 未登录 | 0 | 不发起请求或直接映射登录拦截 | 客户端优先本地拦截 |

### 6.5 网络状态监听

首版不新增 `ConnectivityManager` 级别监听：
- 页面只依赖 `ApiResult` 进入错误态。
- 网络恢复后用户可通过重试按钮、再次切换 Tab 或重新进入页面恢复。
- 不引入自动重试，避免在快速切换维度时增加额外并发噪音。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| 排行页列表数据 | 不持久化 | — | 仅 ViewModel 生命周期内有效 | spec 已明确首版不做离线榜单缓存 |
| Tab 选择 | `SavedStateHandle` | `contentType`、`type` | 页面/进程恢复时生效 | 支持旋转与回跳 |
| 分页状态 | ViewModel 内存状态 | `page/hasNextPage` | 页面存活期间有效 | 切换维度即重置 |
| 预约按钮 loading | ViewModel 内存状态 | `bookingInFlightIds` | 请求完成即移除 | 无需落盘 |

### 7.2 为什么不使用 Room / DataStore

- PRD 明确排除“离线榜单缓存”和“复杂缓存预热”。
- 当前会话内的预约状态可由后端 `is_booked` 与当前列表内存更新共同承接，无需额外本地持久化。
- 若未来需要“最近访问榜单”或更强恢复能力，再考虑 DataStore；本期不扩大范围。

### 7.3 进程恢复策略

- 进程被系统回收后，`SavedStateHandle` 恢复 `contentType/type`，页面按该组合重新请求第一页。
- 已加载的旧列表不持久化；恢复后重新拉取可避免 stale data。
- `PlayerScreen` 仍按现有方式从 route 恢复 `videoId`，排行页不接管播放器状态。

---

## 8. 配置与环境

| 配置项 | 管理方式 | 开发环境值 | 生产环境值 | 说明 |
|--------|---------|----------|-----------|------|
| `API_BASE_URL` | `AppConfig.apiBaseUrl` | 由 Gradle/BuildConfig 注入 | 由 Gradle/BuildConfig 注入 | `ApiClient` 统一规范化到 `/api/` |
| 登录态提供器 | Hilt 绑定 `AuthSessionProvider` | 默认占位实现返回未登录 | 后续由 PRD-08 替换真实实现 | 排行模块不直接读取 `BuildConfig` |

> 禁止在排行功能中硬编码任何环境地址、token、mock host 或登录标记。

---

## 9. API 调用清单

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| `GET /api/dramas/rankings` | 首次进入排行页 | 默认 `ALL + HOT + page=1 + pageSize=10` | 渲染第一页列表，更新 `page/hasNextPage` | 首屏错误态 |
| `GET /api/dramas/rankings` | 切换一级 Tab | `selectedContentType` 改变，`selectedRankingType` 保持 | 清空旧列表并刷新第一页 | 保持 Tab 选中态，显示错误态 |
| `GET /api/dramas/rankings` | 切换二级 Tab | `selectedRankingType` 改变，`selectedContentType` 保持 | 清空旧列表并刷新第一页 | 保持 Tab 选中态，显示错误态 |
| `GET /api/dramas/rankings` | 列表触底 | 当前 `page + 1` | 追加到尾部 | 尾部错误提示，不清空旧列表 |
| `POST /api/dramas/{id}/book` | 预约榜点击按钮且已登录 | 当前项 `dramaId` | 原位更新 `isBooked` 与 `bookingCount` | Toast/内联错误，按钮恢复 |

约束：
- 只有 `RankingType.BOOKING` 场景才展示预约按钮并考虑调用预约接口。
- 匿名用户点击预约时不应命中网络层，而应先进入登录拦截承接。
- 排行项点击不调用新接口，直接导航到既有 `play/{videoId}`。

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Android 端实现方式 |
|---------|---------------|-------------------|
| 默认选择 | `all + hot + page=1` | `RankingViewModel` 从 route/default 读入，首次自动加载 |
| 双维度切换 | 一级/二级 Tab 互相保留对方选择 | `selectedContentType` 与 `selectedRankingType` 独立建模 |
| 分页重置 | 切换任一维度回到第一页 | `refresh(query)` 统一重置 `page/items/hasNextPage` |
| 请求去重 | 旧请求不得覆盖新状态 | `activeRequestToken + RankingRequestKey` 双重校验 |
| 指标映射 | 热榜=热度，推荐榜=推荐值，预约榜=预约数+按钮 | `RankingMetricChip` 依据 `selectedRankingType` 渲染 |
| 空态策略 | 200 + 空数组显示空态 | `items.isEmpty() && errorMessage == null` -> `RankingEmptyState` |
| 预约幂等 | 重复预约保持成功态 | 成功后按钮变“已预约”，继续禁用；再次刷新以服务端状态为准 |
| 登录拦截 | 浏览无需登录，预约才检查 | `AuthSessionProvider` 预检查，未登录发 `RequireLogin(returnRoute)` |
| 播放跳转 | 复用 `play` canonical route | `onOpenPlay -> AppDestination.play(id)`，不新建播放器 route |
| 播放器仍占位 | 验收只看路由与参数透传 | `PlayerScreen` 不改造，仍展示 `videoId` 占位内容 |

---

## 11. 边界与错误处理

### 11.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| 网络层 | `RankingRemoteDataSource.execute()` | 参考 `SearchRemoteDataSource` 解析 `{ error: { code, message } }` |
| Repository | DTO 映射 + `ApiResult` 透传 | 不吞掉 401/404/500 等业务错误 |
| ViewModel | 显式状态机 + token 校验 | 防止旧请求污染新状态 |
| UI 层 | 全页错误态 / 尾部错误态 / 轻提示 | 不引入新的全局通知依赖 |

### 11.2 错误码映射表

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `VALIDATION_ERROR` | 榜单参数异常，请重试 | 首屏/列表错误态 |
| `UNAUTHORIZED` | 请先登录后再预约 | 转为登录拦截，不直接显示原始错误 |
| `NOT_FOUND` | 内容不存在或已下线 | 预约失败轻提示；排行项点击则忽略导航 |
| `CONFLICT` | 当前状态已变化，请刷新后重试 | 预约失败轻提示 |
| `INTERNAL_ERROR` | 加载失败，请稍后重试 | 首屏错误态或分页错误尾部 |
| `SERVICE_UNAVAILABLE` | 服务暂不可用，请稍后重试 | 首屏错误态或分页错误尾部 |
| `NETWORK_ERROR` | 网络异常，请检查后重试 | 首屏错误态 / 尾部错误 |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| 连续切换 Tab | 用户快速点击多个 Tab | 仅消费最后一次 token 对应结果 | 🔴 |
| 重复触底 | 同一页多次触发分页 | `isAppending` 防抖，忽略重复请求 | 🔴 |
| 超大页码 | 后端返回空数组 | 尾部停止加载，不视为异常 | 🟡 |
| 封面缺失 | `cover_url = null/blank` | 使用占位封面 | 🟡 |
| 排行项 id 为空 | 点击整卡或预约按钮 | 不导航/不预约，显示轻量提示 | 🔴 |
| 匿名点击预约 | `isLoggedIn() == false` | 发出 `RequireLogin(returnRoute)`，不打接口 | 🔴 |
| 预约进行中重复点击 | 同一 `dramaId` 已在 `bookingInFlightIds` | 按钮 disabled | 🔴 |
| 预约成功后再次刷新 | 服务端返回 `is_booked = true` | 保持已预约态 | 🟡 |
| App 切后台再回来 | Activity/Composable 重建 | 恢复筛选，必要时按当前 query 重新拉取第一页 | 🟡 |
| 进程被杀 | 系统回收后恢复 | `SavedStateHandle` 恢复筛选，列表重新请求 | 🟡 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `RankingScreen` 首屏 | 是 | 是 | 是 | 是 | 否 |
| `LazyColumn` 分页尾部 | 是 | 是 | 否 | 是 | 否 |
| `RankingBookingButton` | 是 | 是（已预约） | 否 | 是（失败恢复） | 否 |
| `PlayerScreen` 跳转后承接 | 否 | 是 | 否 | 依赖既有播放器占位 | 否 |

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | `RankingViewModel` 状态机、分页、预约、登录拦截 | 关键分支全覆盖 | JUnit4 + MockK + Turbine |
| Repository / DTO 测试 | DTO -> Domain 映射、空封面、分页、错误透传 | 关键映射全覆盖 | JUnit4 |
| Route 测试 | `AppDestination.ranking()`、默认参数、回跳 route 构造 | 新增 route 全覆盖 | JUnit4 |
| RemoteDataSource 测试 | 200/400/401/404/500 错误解析 | 关键接口全覆盖 | JUnit4 |

> 遵循 `android/CLAUDE.md`，测试优先放在 `android/app/src/test/` 纯 JVM 层，不新增 instrumentation 依赖。

### 12.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| A-01 | 默认进入排行页 | `SavedStateHandle` 无参数 | 初始化 ViewModel | 请求 `all + hot + page=1 + pageSize=10` | 单元 |
| A-02 | 一级 Tab 切换 | 当前在 `all + hot` | 点击 `真人` | 保留 `hot`，重置到第一页并清空旧列表 | 单元 |
| A-03 | 二级 Tab 切换 | 当前在 `ai + hot` | 点击 `预约榜` | 保留 `ai`，重置到第一页并刷新 | 单元 |
| A-04 | 快速切换防脏写 | 旧请求慢、新请求快 | 先切 `真人` 再切 `AI` | 只展示 `AI` 数据 | 单元 |
| A-05 | 分页成功 | 第一页已加载且有下一页 | 触底加载 | 列表追加且 `page+1` | 单元 |
| A-06 | 分页失败 | 已有列表 | 下一页请求失败 | 保留旧列表，展示尾部错误 | 单元 |
| A-07 | 匿名点击预约 | `AuthSessionProvider=false` | 点击预约按钮 | 发出 `RequireLogin(returnRoute)`，不打接口 | 单元 |
| A-08 | 已登录预约成功 | `AuthSessionProvider=true` 且接口成功 | 点击预约按钮 | 当前项 `isBooked=true`、`bookingCount+1` | 单元 |
| A-09 | 预约重复点击 | 请求未完成 | 再次点击同一按钮 | 第二次点击被忽略 | 单元 |
| A-10 | 排行项空 id | `drama.id` 为空 | 点击整卡 | 不导航并给出轻提示 | 单元 |
| A-11 | DTO 兼容空封面 | `cover_url = null` | DTO 转 Domain | `coverUrl` 变为空字符串，不抛异常 | DTO |
| A-12 | route 回跳构造 | `ranking(ai, booking)` | 生成 route | 包含 `contentType=ai&type=booking` | Route |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| `RankingRepository` | MockK | ViewModel 主路径测试 |
| `AuthSessionProvider` | Fake / MockK | 覆盖已登录/未登录分支 |
| `RankingRemoteDataSource` | Fake / MockK | Repository 映射测试 |
| `SavedStateHandle` | 直接构造 | 验证默认值与回跳筛选恢复 |

---

## 13. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | — | — | 首版分页、列表、按钮状态均基于现有 Compose/StateFlow/Retrofit 能力实现，不引入 Paging、图片加载或认证 SDK |

---

## 14. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 手动分页实现容易重复触发 | 排行列表 | 🔴 | 中 | 用 `isAppending + hasNextPage + LazyListState` 三重保护 | 临时降级为只支持第一页 |
| 旧请求覆盖新 Tab 状态 | 双层 Tab 切换 | 🔴 | 中 | 用 `requestToken + queryKey` 忽略过期响应 | 出现问题时先禁止快速切换期间写状态 |
| 登录能力未就绪导致预约闭环不完整 | 预约榜 | 🟡 | 高 | 抽象 `AuthSessionProvider` + `RequireLogin(returnRoute)`，先接标准占位承接 | 浏览链路正常上线，预约仅保留拦截占位 |
| 当前无图片加载能力，排行视觉可能弱于设计稿 | 卡片展示 | 🟡 | 高 | 沿用现有封面占位策略，避免引入未批准依赖 | 后续单独 PRD 评估图片加载方案 |
| 预约成功后若立即重排列表会闪动 | 预约榜 | 🟡 | 中 | 首版只原位更新按钮和计数，不立即重排 | 下次刷新时以服务端排序为准 |
| `cover_url` 可能为 null | DTO 解析 | 🟡 | 中 | 新增排行 DTO 对空值容错映射 | 若后端返回异常字段，降级为空封面 |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/features/homepage-feed/index.md` | Android / 状态管理 / 入口与路由 | 首页已具备 `StateFlow + LazyColumn + play/detail` 主链路，可复用实现风格 |
| `wiki/features/video-player/index.md` | 入口与路由 / 已知限制 | 播放器仍为占位实现，排行点击验收应以进入 `play` 链路为准 |
| `wiki/features/deeplink/index.md` | Android Deeplink 流程 | `play` 为 canonical，`player` 历史兼容需保持不变 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `android/CLAUDE.md` | Android 技术栈、StateFlow/Repository 约束、禁止新增未批准依赖 |
| `docs/specs/2026-07-27-prd-05-ranking/spec.md` | 双层 Tab、分页、预约拦截、播放复用的验收要求 |
| `docs/specs/2026-07-27-prd-05-ranking/design.md` | 共享 API、跨端状态机、默认值和登录拦截约束 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 现有 `ranking`、`play`、`player` route 定义 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 当前 `ranking` 仍接 `PlaceholderScreen`，需替换为真实页 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/model/SearchQuickEntry.kt` | 搜索发现页已有“排行”快捷入口 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/ui/SearchHomeScreen.kt` | 搜索页快捷入口路由触发方式 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 现有 Compose 列表、封面占位、可导航 id 校验风格 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchHomeViewModel.kt` | 单次事件 + `SharedFlow` 导航模式 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchResultViewModel.kt` | `SavedStateHandle` 初始化与 `requestInFlight` 风格 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 现有 Retrofit 接口定义风格 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiClient.kt` | 通过 `AppConfig` 管理 base URL，禁止硬编码地址 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt` | 认证头注入当前仍为占位实现 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/SearchRemoteDataSource.kt` | 错误包体解析模式可复用于排行接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/Drama.kt` | 现有 `Drama` 不含排行字段，适合新增独立 `RankingDrama` |
