# Android 端技术方案：PRD-12 剧场频道

> 创建日期：2026-07-28
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

本期 Android 端在既有 Kotlin + Jetpack Compose + Hilt + StateFlow + Navigation Compose 架构上，将 `theater_graph` 的占位页替换为真实剧场频道页。实现继续遵守当前工程的分层：Presentation（Compose + ViewModel）→ Domain（Model + Repository + UseCase）← Data（DTO + DataSource + RepositoryImpl）← Core（Network / DI / Config）。

```text
┌────────────────────────────────────────────────────────────┐
│ UI Layer                                                   │
│  NavGraph(theater_graph)                                   │
│    └── TheaterScreen                                       │
│        ├── TheaterTopBar                                   │
│        ├── TheaterChannelTabs                              │
│        ├── TheaterShortcutGrid                             │
│        ├── TheaterFeedGrid (LazyVerticalGrid / LazyColumn) │
│        └── Loading / Empty / Error / AppendFooter          │
├────────────────────────────────────────────────────────────┤
│ ViewModel Layer                                            │
│  TheaterViewModel                                          │
│    ├── selectedChannel / items / page / hasNextPage        │
│    ├── isLoading / isAppending / error / appendError       │
│    ├── request token anti-stale protection                 │
│    └── effects (scan placeholder / navigation message)     │
├────────────────────────────────────────────────────────────┤
│ Domain Layer                                               │
│  TheaterChannel / TheaterDrama / TheaterPage               │
│  DramaRepository.fetchTheaterFeed(query)                   │
│  GetTheaterFeedUseCase                                     │
├────────────────────────────────────────────────────────────┤
│ Data + Core                                                │
│  ApiService.getDramaChannel(...)                           │
│  DramaRemoteDataSource.fetchTheaterFeed(...)               │
│  Theater DTO ↔ Domain 映射                                 │
└────────────────────────────────────────────────────────────┘
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 将 theater graph root 从 `PlaceholderScreen` 替换为 `TheaterScreen` |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 扩展 | 视需要补充 theater 内部 route 常量；继续复用 ranking/classification/new-releases/play |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | 不变或轻微扩展 | 已支持 `SavedStateHandle` 初始化榜单参数，可直接复用预约入口直达能力 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增 `GET dramas/channel` 接口 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt` | 修改 | 新增剧场 Feed 请求封装 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt` | 修改 | 新增剧场 Feed repository 实现 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/*` | 新增 | Theater 相关实体与 query 模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/theater/*` | 新增 | 剧场 UI、ViewModel、UI model |
| `android/app/src/test/*` | 扩展 | 补充 TheaterViewModel、DTO 映射、repository、route 构建测试 |

### 1.2 与现有代码现状的兼容说明

1. **Android 已具备 home-owned route 与 ranking 参数化路由，剧场入口继续复用现有承接语义**  
   `AppDestination.ranking(contentType, type)` 已支持 `all + booking` 参数，因此 Android 不需要像 iOS 一样额外引入 router context；同时搜索 / 分类 / 排行 / 新剧 / 播放都继续走现有 home graph / canonical `play` 路由。从剧场进入这些能力时允许切换到底部 `home` Tab，不在 `theater_graph` 内复制承接页副本。

2. **搜索 / 分类 / 排行 / 新剧都在 HOME graph**  
   当前 `NavGraph.kt` 已把这些页面注册在 `home_graph` 中，因此从剧场进入这些能力时允许切换到底部 `home` tab，是与现状一致的正式行为。

3. **Theater 页只替换 root screen，不重做 app shell**  
   仅在 `theater_graph` 中用真实页面替换 placeholder，不改底部导航、menu、player、detail 等现有结构。

4. **网络和错误处理沿用现有 `ApiResult` 体系**  
   新接口继续由 Retrofit 返回 DTO，DataSource / Repository 统一转为 `ApiResult` 或 domain 实体，不额外引入新封装。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | theater graph root 改为 `TheaterScreen` |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 如需，补充 theater screen route builder；继续复用 ranking/classification/new-releases/play |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增 `getDramaChannel(channel, page, pageSize)` |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt` | 修改 | 新增 `fetchTheaterFeed(query)` |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/TheaterFeedResponseDto.kt` | 新增 | 剧场接口 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt` | 修改 | 实现剧场 Feed 拉取与映射 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/TheaterChannel.kt` | 新增 | 子频道枚举 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/TheaterDrama.kt` | 新增 | 剧场卡片实体 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/TheaterQuery.kt` | 新增 | 剧场查询模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/TheaterPage.kt` | 新增 | 分页实体 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/DramaRepository.kt` | 修改 | 新增 `getTheaterFeed(query)` 接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetTheaterFeedUseCase.kt` | 新增 | 剧场 Feed use case |
| `android/app/src/main/java/com/djs66256/short_drama/feature/theater/ui/TheaterScreen.kt` | 新增 | 剧场页根 UI |
| `android/app/src/main/java/com/djs66256/short_drama/feature/theater/ui/TheaterComponents.kt` | 新增 | 频道 Tabs、快捷入口、卡片、空态等组件 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt` | 新增 | 状态机、分页、effect |
| `android/app/src/main/java/com/djs66256/short_drama/feature/theater/model/TheaterDramaItemUiModel.kt` | 新增 | UI 展示模型和 formatter |
| `android/app/src/test/.../TheaterViewModelTest.kt` | 新增 | StateFlow 状态流转测试 |
| `android/app/src/test/.../TheaterFeedDtoTest.kt` | 新增 | DTO -> Domain 映射测试 |
| `android/app/src/test/.../DramaRepositoryImplTest.kt` | 修改 | 新增剧场 Feed 场景 |

---

## 3. UI 层设计

### 3.1 组件层级树

```text
TheaterScreen
├── TheaterTopBar
│   ├── SearchEntryButton
│   └── ScanEntryButton
├── TheaterChannelTabs
├── TheaterShortcutGrid
│   ├── ShortcutCard(Classification)
│   ├── ShortcutCard(Ranking)
│   ├── ShortcutCard(NewReleases)
│   └── ShortcutCard(Booking)
└── TheaterContent
    ├── TheaterLoadingState
    ├── TheaterErrorState
    ├── TheaterEmptyState
    └── TheaterFeedGrid
        ├── TheaterDramaCard (Lazy grid item)
        └── TheaterAppendFooter
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `TheaterScreen` | Composable | 剧场页根容器，收集状态并派发事件 | 否 |
| `TheaterTopBar` | Composable | 搜索入口、识图入口 | 否 |
| `TheaterChannelTabs` | Composable | 8 个子频道切换 | 否 |
| `TheaterShortcutGrid` | Composable | 4 个快捷入口 | 否 |
| `TheaterFeedGrid` | Composable | 双列 Feed 列表与触底加载更多 | 否 |
| `TheaterDramaCard` | Composable | 单张剧场卡片 | 否 |
| `TheaterLoadingState` | Composable | 首屏加载态 | 否 |
| `TheaterErrorState` | Composable | 首屏错误态 + 重试 | 否 |
| `TheaterEmptyState` | Composable | 空态展示 | 否 |
| `SearchHomeScreen` | Composable | 顶部搜索入口承接页 | 是 |
| `RankingScreen` | Composable | 排行 / 预约榜承接页 | 是 |
| `ClassificationScreen` | Composable | 筛选承接页 | 是 |
| `PlaceholderScreen(title = "新剧")` | Composable | 新剧占位页 | 是 |

### 3.3 Composable 接口定义

```kotlin
@Composable
fun TheaterScreen(
    onOpenSearch: () -> Unit,
    onOpenClassification: () -> Unit,
    onOpenRanking: (String) -> Unit,
    onOpenNewReleases: () -> Unit,
    onOpenPlay: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TheaterViewModel = hiltViewModel(),
) { ... }
```

```kotlin
@Composable
fun TheaterDramaCard(
    item: TheaterDramaItemUiModel,
    onOpenPlay: () -> Unit,
    modifier: Modifier = Modifier,
) { ... }
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| 父 → 子 | Composable 参数 | 频道列表、快捷入口、卡片内容、回调 |
| 子 → 父 | Lambda Callback | 点击频道、快捷入口、卡片、重试 |
| UI ↔ ViewModel | `collectAsState()` + 事件函数 | 页面状态与用户交互 |
| Navigation 参数 | `AppDestination.ranking(contentType, type)` | 从剧场进入排行 / 预约榜 |
| 进程恢复 | `SavedStateHandle`（若需要） | 可保留当前频道，但首版不强依赖 |

### 3.5 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | 使用 `LazyVerticalGrid(GridCells.Fixed(2))` 或等价双列布局 | 保证双列卡片稳定 |
| 横竖屏 | `rememberLazyGridState` + `rememberSaveable` | 保持滚动位置和基础状态可恢复 |
| 折叠屏 | 先按常规自适应宽度布局 | 首版不做专属折叠态优化 |
| 字体缩放 | 依赖 Material3 Typography，自适应换行 | 标题行数限制避免卡片撑爆 |
| 深色模式 | 复用现有 `MaterialTheme` | 不新增硬编码颜色 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 UI | 职责 |
|-----------|---------|------|
| `TheaterViewModel` | `TheaterScreen` | 剧场首页数据加载、频道切换、分页、错误态、effect |
| `RankingViewModel` | `RankingScreen` | 现有排行页加载与 booking 逻辑；本期直接复用其入参能力 |

### 4.2 状态定义

```kotlin
data class TheaterUiState(
    val selectedChannel: TheaterChannel = TheaterChannel.ALL,
    val items: List<TheaterDramaItemUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isAppending: Boolean = false,
    val errorMessage: String? = null,
    val appendErrorMessage: String? = null,
    val page: Int = 1,
    val hasNextPage: Boolean = false,
    val hasLoadedOnce: Boolean = false,
)

@HiltViewModel
class TheaterViewModel @Inject constructor(
    private val getTheaterFeedUseCase: GetTheaterFeedUseCase,
) : ViewModel() { ... }
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `selectedChannel` | `TheaterChannel` | `ALL` | 当前频道 |
| `items` | `List<TheaterDramaItemUiModel>` | `emptyList()` | 当前频道卡片数据 |
| `isLoading` | `Boolean` | `true` | 首屏 / 切频道加载态 |
| `isAppending` | `Boolean` | `false` | 分页请求是否在途 |
| `errorMessage` | `String?` | `null` | 首屏错误 |
| `appendErrorMessage` | `String?` | `null` | 分页错误，不影响已有列表 |
| `page` | `Int` | `1` | 当前页码 |
| `hasNextPage` | `Boolean` | `false` | 是否还有下一页 |
| `hasLoadedOnce` | `Boolean` | `false` | 是否已完成至少一次请求 |
| `latestRequestToken` | `Long` | 0 | 防旧请求覆盖新状态 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | UI 层表现 |
|---------|---------|----------|
| Loading | `isLoading && !hasLoadedOnce` | 首屏骨架 / ProgressIndicator |
| Success（有数据） | `items.isNotEmpty() && errorMessage == null` | 双列 Feed |
| Empty | `hasLoadedOnce && items.isEmpty() && errorMessage == null` | 空态图文 |
| Error（可重试） | `errorMessage != null && items.isEmpty()` | 错误视图 + 重试 |
| Append Error | `appendErrorMessage != null && items.isNotEmpty()` | 列表尾部错误 + 重试加载更多 |

### 4.5 状态机约束映射

| shared design 约束 | Android 端落实方式 |
|-------------------|--------------------|
| 默认 `channel=all` | 初始 `selectedChannel = ALL` |
| 切频道重置 page=1 | `refresh(channel)` 时清空 items 并重置页码 |
| 旧请求不得覆盖新状态 | `latestRequestToken` + request key 校验 |
| append failure 不清空已有列表 | 仅更新 `appendErrorMessage` |
| 非 `all` 频道空态 | 后端空数组 -> `items.empty + error=null + hasLoadedOnce=true` |
| 分页只在有下一页时发生 | `loadNextPageIfNeeded()` guard `hasNextPage` |

---

## 5. Navigation 路由设计

### 5.1 导航方案

继续使用 Jetpack Navigation Compose 的嵌套导航图：`theater_graph` 挂载 `TheaterScreen`，而搜索 / 排行 / 分类 / 新剧仍在 `home_graph` 下承接。

### 5.2 路由清单

| 路由标识 | 目标 Composable | 参数 | 导航方式 | 说明 |
|---------|----------------|------|---------|------|
| `AppDestination.Route.THEATER` | `TheaterScreen` | 无 | top-level tab | 剧场根页面 |
| `AppDestination.search()` | `SearchHomeScreen` | 无 | `navController.navigate(...)` | 搜索入口，切入 home graph，并允许底部 tab 切换到 `home` |
| `AppDestination.classification()` | `ClassificationScreen` | 无 | `navController.navigate(...)` | 筛选入口，复用 home-owned 承接 |
| `AppDestination.ranking()` | `RankingScreen` | `contentType=all&type=hot` | `navigate(...)` | 排行快捷入口，复用 home-owned 承接 |
| `AppDestination.ranking(contentType = ALL, type = BOOKING)` | `RankingScreen` | `contentType=all&type=booking` | `navigate(...)` | 预约快捷入口，首屏直达 booking |
| `AppDestination.newReleases()` | `PlaceholderScreen` | 无 | `navigate(...)` | 新剧占位承接，复用 home-owned 承接 |
| `AppDestination.play(videoId)` | `PlayerScreen` | `videoId` | `navigate(...)` | 点击卡片播放，继续复用既有 home-owned canonical `play` 承接语义 |

### 5.3 导航图

```kotlin
navigation(
    startDestination = AppDestination.Route.THEATER,
    route = AppDestination.Graph.THEATER,
) {
    composable(route = AppDestination.Route.THEATER) {
        TheaterScreen(
            onOpenSearch = {
                navigateToTopLevelTab(navController, TopLevelTab.HOME)
                navController.navigate(AppDestination.search())
            },
            onOpenClassification = {
                navigateToTopLevelTab(navController, TopLevelTab.HOME)
                navController.navigate(AppDestination.classification())
            },
            onOpenRanking = { route ->
                navigateToTopLevelTab(navController, TopLevelTab.HOME)
                navController.navigate(route)
            },
            onOpenNewReleases = {
                navigateToTopLevelTab(navController, TopLevelTab.HOME)
                navController.navigate(AppDestination.newReleases())
            },
            onOpenPlay = { videoId ->
                navigateToTopLevelTab(navController, TopLevelTab.HOME)
                navController.navigate(AppDestination.play(videoId))
            },
        )
    }
}
```

### 5.4 Deep Link 处理（如适用）

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `djsdrama://play/{videoId}` | `PlayerScreen` | `videoId` |
| `djsdrama://search` | `SearchHomeScreen` | 无 |
| `djsdrama://ranking?contentType=all&type=booking` | `RankingScreen` | `contentType` / `type` |
| `djsdrama://theater` | `TheaterScreen` | 无 |

Android 现有 ranking 路由已参数化，因此如果未来要补充 deeplink 到 booking 榜，也可以自然沿用现有 query 参数风格。

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | Retrofit + OkHttp | 沿用现有网络层 |
| 数据模型 | kotlinx.serialization DTO | 新增 theater DTO |
| 请求拦截器 | 现有 Interceptor | 不新增剧场专属拦截器 |
| 错误处理 | `ApiResult<T>` | DataSource / Repository 继续统一封装 |

### 6.2 API 接口定义

```kotlin
interface ApiService {
    @GET("dramas/channel")
    suspend fun getDramaChannel(
        @Query("channel") channel: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): TheaterFeedResponseDto
}
```

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 首屏失败 | 不自动重试 | 无 | 由用户点击重试按钮触发 |
| 分页失败 | 不自动重试 | 无 | 由用户重试加载更多或再次触底触发 |
| 5xx / 网络异常 | 不在网络层静默重试 | 无 | 避免多次触发造成列表抖动 |

### 6.4 网络状态监听

本期不新增 `ConnectivityManager` 的专属监听分支。剧场页与现有 ranking/home 一样，采用错误态 + 手动重试即可。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| 剧场 Feed 列表 | 不持久化 | ViewModel 内存态 | 页面销毁即释放 | 首版无需 Room / DataStore |
| 当前子频道 | 可选 `SavedStateHandle`，首版可不持久化 | `selectedChannel` | 进程重建时可恢复或回到默认 `ALL` | 不是硬性需求 |
| 热度格式化规则 | 本地 formatter | 代码常量 | 固定 | 属于展示逻辑 |

### 7.2 Room 实体设计

```kotlin
本期不引入 Room / DataStore 新实体。
剧场页所有状态均保留在内存中。
```

### 7.3 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| 剧场第一页数据 | 无持久化缓存 | — | 页面销毁释放 |
| 图片资源 | 复用现有图片加载 / placeholder 方式 | 现有策略 | 现有策略 |

### 7.4 数据库 Migration

- 本期无 Room schema 变更。
- 不需要 migration 或 `fallbackToDestructiveMigration` 讨论。

---

## 8. 配置与环境

| 配置项 | 管理方式 | 开发环境值 | 生产环境值 | 说明 |
|--------|---------|----------|-----------|------|
| API Base URL | `AppConfig` / `BuildConfig` | 现有配置 | 现有配置 | 继续通过 `AppConfig` 间接访问 |
| Theater feature flag | 不新增 | — | — | 本期默认随客户端交付 |
| 文案 / 频道常量 | 代码枚举 / UI model | 固定 | 固定 | 不属于环境配置 |

> ⚠️ 禁止直接硬编码环境地址、token、BuildConfig 常量访问。剧场页仅复用现有 `AppConfig` 和工程配置。

---

## 9. API 调用清单

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| `GET /api/dramas/channel` | 首次进入剧场页 | 默认 `channel=all,page=1,pageSize=20` | 更新 `uiState.items` / `hasNextPage` | 首屏错误态 |
| `GET /api/dramas/channel` | 切换频道 | 用户点击子频道 Tab | 刷新当前频道第一页 | 错误态但保留选中频道 |
| `GET /api/dramas/channel` | 触底加载更多 | `hasNextPage == true` | 追加列表 | 仅更新 `appendErrorMessage` |
| `GET /api/dramas/rankings` | 点击排行 / 预约入口 | `AppDestination.ranking(...)` 路由参数 | `RankingScreen` 按初始化参数加载 | 沿用现有排行错误处理 |
| `GET /api/dramas/tags` | 点击筛选入口 | 分类页现有逻辑 | 沿用现有分类逻辑 | 沿用现有分类错误处理 |
| `POST /api/dramas/{id}/book` | 在预约榜中点击预约 | 排行页现有逻辑 | 沿用现有 booking 更新 | 沿用现有登录 / 提示 |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Android 端实现方式 |
|---------|---------------|-------------------|
| 默认子频道 | 首次固定 `channel=all` | `TheaterUiState.selectedChannel = ALL` |
| 默认分页 | `page=1&pageSize=20` | `TheaterQuery(channel, page, pageSize)` 默认值 |
| 子频道切换重置 | 清空旧列表，回第一页 | `refresh(channel)` 重置状态 |
| 请求防乱序 | 旧请求不得覆盖新状态 | request token + query key 校验 |
| 加载更多约束 | 仅 `hasNextPage=true` 且当前无请求在途时触发 | `loadNextPageIfNeeded()` guard |
| 空态策略 | 非 `all` 频道统一空态 | `items.isEmpty && error=null` -> Empty UI |
| 热度格式化 | 服务端原始数值，端侧格式化 | `TheaterDramaItemUiModel` 内输出中文短数字 |
| 搜索入口承接 | 进入现有搜索发现页 | 先 `navigateToTopLevelTab(..., HOME)`，再 `navController.navigate(AppDestination.search())` |
| 快捷入口承接 | 筛选 / 排行 / 预约 / 新剧复用现有路由 | 先切到 `HOME` top-level tab，再执行 `navigate(...)` |
| 预约榜直达 | 一步进入 `all + booking` | 切到 `HOME` 后调用 `AppDestination.ranking(contentType = ALL, type = BOOKING)` |
| 播放跳转 | 点击卡片复用 canonical `play` | 先切到 `HOME` top-level tab，再 `navController.navigate(AppDestination.play(id))` |
| 识图入口 | 本地占位，不触发权限/网络 | Snackbar / Toast 提示 |

---

## 11. 边界与错误处理

### 11.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| 网络层 | Retrofit + `ApiResult` | 统一承接 HTTP / 解析错误 |
| ViewModel | `try-catch` + StateFlow 更新 | 区分首屏错误与分页错误 |
| UI 层 | 内联错误态 / Snackbar / Toast | 分页失败不清空已有内容 |
| 日志 | 现有日志体系 | 不新增剧场专属日志库 |

### 11.2 错误码映射表

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `VALIDATION_ERROR` | 请求参数有误，请稍后重试 | 错误页 / Snackbar |
| `UNAUTHORIZED` | 请先登录 | 剧场 feed 不应出现；预约操作沿用排行逻辑 |
| `FORBIDDEN` | 当前不可访问 | 错误页 |
| `NOT_FOUND` | 资源不存在 | Snackbar / 空态 |
| `CONFLICT` | 当前状态冲突，请稍后重试 | Snackbar |
| `TOO_MANY_REQUESTS` | 请求过于频繁，请稍后重试 | Snackbar |
| `INTERNAL_ERROR` | 服务开小差了，请稍后重试 | 错误页 / 尾部错误 |
| `SERVICE_UNAVAILABLE` | 服务暂不可用，请稍后重试 | 错误页 / 尾部错误 |
| `NETWORK_ERROR` | 网络异常，请检查后重试 | 错误页 / 尾部错误 |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| 快速切换频道 | 用户频繁点击多个 Tab | 仅最后一个请求 token 生效 | 🔴 |
| 非 `all` 频道始终为空 | 首版后端统一空数组 | 正常展示空态，不视为错误 | 🔴 |
| 分页失败 | 第二页或后续页失败 | 保留已有内容，只展示尾部重试 | 🔴 |
| 预约入口直达 booking 榜 | 从剧场点击预约 | 直接导航到 `ranking?contentType=all&type=booking` | 🔴 |
| 搜索 / 排行 / 分类归属 home graph | 从 theater 进入这些能力 | 允许底部 tab 切到 home graph | 🟡 |
| 首屏失败后重试 | 断网 / 5xx 恢复 | 重新请求当前频道第一页 | 🟡 |
| 封面缺失 | `coverUrl` 为空或图片异常 | 统一 placeholder，不阻断点击 | 🟡 |
| 配置变更 | 旋转屏幕 | 依赖 ViewModel 保持状态；必要时用 `rememberSaveable` 保留滚动位置 | 🟡 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `TheaterScreen` | 首屏 loading | 渲染频道 + 快捷入口 + Feed | 渲染频道 + 快捷入口 + 空态 | 错误页 + 重试 | 本期无独立不可重试态 |
| `TheaterFeedGrid` | 不展示 | 双列卡片 | 空列表不进 grid | append footer 错误 | — |
| `TheaterShortcutGrid` | 与页面一起展示 | 正常可点击 | 正常可点击 | 正常可点击 | — |
| `TheaterChannelTabs` | 与页面一起展示 | 正常切换 | 正常切换 | 保留当前选中 | — |

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | `TheaterViewModel` 状态机、分页、乱序保护 | 关键场景全覆盖 | JUnit4 + MockK + Turbine |
| 单元测试 | `GetTheaterFeedUseCase` 委托行为 | 关键场景全覆盖 | JUnit4 + MockK |
| DTO/Repository 测试 | `TheaterFeedResponseDto` 映射与 `DramaRepositoryImpl` | 关键场景全覆盖 | JUnit4 |
| UI 测试（轻量） | formatter / route builder / UI model | 关键场景全覆盖 | JUnit4 |

### 12.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| AND-01 | 首次进入默认加载 `all` 第一页 | repository 返回真实列表 | 初始化 ViewModel | `selectedChannel == ALL` 且 `items` 非空 | 单元 |
| AND-02 | 切换非 `all` 频道返回空态 | repository 返回空页 | `onChannelSelected(REAL)` | `items.isEmpty()` 且 `errorMessage == null` | 单元 |
| AND-03 | 快速切换频道旧请求不覆盖新状态 | 第一请求慢、第二请求快 | 连续切换频道 | 最终状态仅属于最后频道 | 单元 |
| AND-04 | 分页失败不清空已有列表 | 第一页成功、第二页错误 | `loadNextPageIfNeeded()` | `items` 保留，`appendErrorMessage != null` | 单元 |
| AND-05 | 预约入口 route 正确 | 无 | 点击预约快捷入口 | 导航到 `ranking?contentType=all&type=booking` | 单元 |
| AND-06 | `heat` 格式化正确 | `heat = 23000` | UI model 映射 | `metricValue` 为中文短数字文案 | 单元 |
| AND-07 | `GET /api/dramas/channel` DTO 映射 | 返回 theater response dto | `toDomain()` | `heat` 为非负整数，分页字段正确 | DTO |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| Repository | MockK mock `DramaRepository` | TheaterViewModel 测试 |
| API 请求 | Fake DataSource / Mock response DTO | 不依赖真实网络 |
| StateFlow 验证 | Turbine | 验证 loading → success/empty/error 流转 |

---

## 13. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | — | — | 本期完全复用现有 Compose / Navigation / Retrofit / MockK / Turbine |

> ⚠️ 不新增开源依赖，避免额外用户确认流程。

---

## 14. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 双列 Feed 实现与现有单列页面风格差异较大 | 剧场页面主视觉 | 🔴 | 中 | 独立 Theater 组件，不污染 home/ranking 现有实现 | 必要时先以稳定双列基础版上线 |
| 快速切频道导致状态串频 | Feed 正确性 | 🔴 | 中 | 使用 request token + query key 校验 | 退化为串行请求提交 |
| 触底加载更多重复触发 | 分页稳定性 | 🟡 | 中 | `isAppending` + `hasNextPage` 双重 guard | 尾部手动重试按钮兜底 |
| 与 home graph 路由归属认知不一致 | 导航体验 | 🟡 | 低 | 明确设计保持现状：从剧场进入这些页允许切到 home | 若未来需要 theater 副本，再作为新 PRD |
| 测试不足导致回归 | 后续改动稳定性 | 🟡 | 中 | TheaterViewModel / route builder / DTO 映射补齐单元测试 | 未补齐测试前不标记平台编码完成 |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/features/app-shell/index.md` | app shell 与 theater tab | 确认 theater 是既有一级 tab |
| `wiki/features/search-discovery/index.md` | 搜索 / 新剧承接 | 确认搜索与新剧入口已存在 |
| `wiki/features/ranking/index.md` | 排行 / 预约榜 | 确认 booking 榜由 ranking 体系承接 |
| `wiki/features/classification/index.md` | 分类页 | 确认筛选入口复用 classification |
| `wiki/features/video-player/index.md` | play route | 确认播放链路继续复用 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | Android 已支持 ranking 参数化路由 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | theater 当前仍是 placeholder；search/ranking/classification/new-releases 均归属 HOME graph |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | `SavedStateHandle` 初始化 `contentType/type`，可直接承接 booking 榜 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt` | 排行页 UI 状态与尾部分页结构参考 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/ui/SearchHomeScreen.kt` | 搜索发现页入口和快捷入口交互模式 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 顶部栏、错误态与卡片组织方式参考 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | Retrofit endpoint 组织方式 |

