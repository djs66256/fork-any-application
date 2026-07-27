# Android 端技术方案：PRD-04 搜索发现

> 创建日期：2026-07-26
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

本期 Android 端继续遵循现有单 Activity + Navigation Compose + Hilt + ViewModel + Repository 分层，不新增第三方依赖。搜索发现能力拆分为“搜索发现页”“搜索结果页”“快捷入口承接页”“本地历史存储”四个子域，并尽量复用首页 Feed 已有的卡片与列表语义。

```text
┌─────────────────────────────────────────────────────────────┐
│ UI Layer (Jetpack Compose)                                 │
│ ├── HomeScreen 搜索入口                                    │
│ ├── SearchHomeScreen 搜索发现页                            │
│ │   ├── SearchTopBar / SearchInputField                    │
│ │   ├── SearchQuickEntrySection                            │
│ │   ├── SearchHistorySection                               │
│ │   └── HotSearchSection                                   │
│ ├── SearchResultScreen 搜索结果页                          │
│ │   ├── SearchResultTopBar                                 │
│ │   ├── SearchResultStateContent                           │
│ │   └── HomeDramaCard (复用)                               │
│ └── DiscoveryPlaceholderScreen 承接页（排行/分类/新剧/演员） │
├─────────────────────────────────────────────────────────────┤
│ ViewModel Layer (StateFlow + SavedStateHandle)             │
│ ├── SearchHomeViewModel                                    │
│ ├── SearchResultViewModel                                  │
│ └── MainNavigationViewModel（扩展 PendingRoute）           │
├─────────────────────────────────────────────────────────────┤
│ Domain Layer                                               │
│ ├── SearchDramasUseCase                                    │
│ ├── GetHotSearchKeywordsUseCase                            │
│ ├── ObserveSearchHistoryUseCase                            │
│ ├── SaveSearchHistoryUseCase                               │
│ └── ClearSearchHistoryUseCase                              │
├─────────────────────────────────────────────────────────────┤
│ Data Layer                                                 │
│ ├── SearchRemoteDataSource                                 │
│ ├── SearchRepositoryImpl                                   │
│ ├── SearchHistoryLocalDataSource (DataStore Preferences)   │
│ └── DramaRepositoryImpl / DramaRemoteDataSource（扩展搜索） │
└─────────────────────────────────────────────────────────────┘
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/home` | 扩展 | 首页右上角新增搜索入口；结果页复用 `HomeDramaCard` 与现有“观看 / 详情”动作语义 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation` | 扩展 | 新增 `search`、`search/result`、`ranking`、`classification`、`new-releases`、`actors` route 与对应 deeplink 解析 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 扩展 | 新增 `GET /dramas/search`、`GET /dramas/hot-search` 接口定义 |
| `android/app/src/main/java/com/djs66256/short_drama/data` | 扩展 | 增加搜索远端数据源、本地历史数据源、DTO 与 repository 接线 |
| `android/app/src/main/java/com/djs66256/short_drama/domain` | 扩展 | 增加搜索实体、repository 接口与 use case |
| `android/app/src/main/java/com/djs66256/short_drama/core/di` | 扩展 | 为搜索 repository、本地 DataStore 存储与 use case 提供 Hilt 绑定 |
| `android/app/src/test/java/com/djs66256/short_drama` | 扩展 | 补充 ViewModel、route/deeplink、本地历史规则与 UI 纯函数测试 |

### 1.2 方案约束

- 与共享 `design.md` 保持一致：仅接入 `GET /api/dramas/search` 与 `GET /api/dramas/hot-search` 两个新接口。
- 搜索结果页复用现有首页卡片交互语义，仅保留“观看 / 详情”，不新增整卡点击语义。
- `new-releases` 与 `actors` 首版固定为 Native 占位承接页，不回退到 Web 页面。
- `ranking`、`classification` 也先注册 Native 承接页，以保持 PRD-05 / PRD-06 前的稳定入口。
- 不新增第三方依赖；本地历史使用项目已存在依赖能力 `androidx.datastore:datastore-preferences`。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 首页顶部新增搜索入口，并保持 Feed 列表与卡片语义不变 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/ui/SearchHomeScreen.kt` | 新增 | 搜索发现页，承载搜索框、快捷入口、历史、热搜 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/ui/SearchResultScreen.kt` | 新增 | 搜索结果页，顶部保留可编辑搜索框，列表区复用 `HomeDramaCard` |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/ui/DiscoveryPlaceholderScreen.kt` | 新增 | 排行 / 分类 / 新剧 / 演员承接页的统一占位实现 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchHomeViewModel.kt` | 新增 | 聚合历史、热搜、快捷入口与搜索触发逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchResultViewModel.kt` | 新增 | 聚合 query、请求状态、结果列表与重试逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 扩展 search 系列 route、query arg 与 `PendingRoute` |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt` | 修改 | 支持 `djsdrama://search`、`djsdrama://search/result/{query}`、`djsdrama://ranking`、`djsdrama://classification`、`djsdrama://new-releases`、`djsdrama://actors` |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 注册搜索页、结果页与 4 个快捷入口承接页 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 添加搜索与热搜 Retrofit 接口，并与 canonical `/api/dramas/*` 前缀对齐 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiClient.kt` | 修改 | 保持 `AppConfig.apiBaseUrl` 作为唯一 base URL 来源；本期需明确校准其默认值/拼接约定，使 `ApiService` 最终命中 `/api/dramas/search` 与 `/api/dramas/hot-search` |
| `android/app/build.gradle.kts` | 修改 | 将 `api.base.url` 默认回退值从旧的 `/api/v1` 校准到与当前 Backend 一致的 canonical `/api/` 前缀（例如 `http://10.0.2.2:3000/api/`），避免搜索接口默认命中错误地址 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/SearchRemoteDataSource.kt` | 新增 | 包装搜索与热搜接口为 `ApiResult`，并解析后端 `{ error: { code, message } }` 错误包体 |
| `android/app/src/main/java/com/djs66256/short_drama/data/local/SearchHistoryLocalDataSource.kt` | 新增 | 基于 DataStore Preferences 管理本地历史读写与清空 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/SearchRepositoryImpl.kt` | 新增 | 接线远端搜索、热搜与本地历史规则 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/SearchRepository.kt` | 新增 | 定义搜索相关 domain 接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/*.kt` | 新增 | 搜索、热搜、历史读写/清空 use case |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt` | 修改 | 提供 DataStore 容器或搜索本地数据源依赖 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 绑定 `SearchRepository` 实现 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/search/...` | 新增 | 搜索页与结果页 ViewModel、DataStore 规则、route/deeplink 测试 |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/ApiServiceTest.kt` | 修改 | 补充搜索与热搜 endpoint 注解及 query 名称测试 |
| `android/app/src/test/java/com/djs66256/short_drama/data/datasource/SearchRemoteDataSourceTest.kt` | 新增 | 覆盖 200 成功、400/500 错误包体解析、异常兜底 |

---

## 3. UI 层设计

### 3.1 组件层级树

```text
HomeScreen
├── HomeTopBar
│   ├── Title
│   └── SearchEntryButton
└── HomeFeedContent
    └── HomeDramaCard (existing)

SearchHomeScreen
├── SearchHomeTopBar
│   ├── BackButton
│   └── SearchInputBar
├── SearchQuickEntrySection
│   ├── RankingEntry
│   ├── NewReleasesEntry
│   ├── ClassificationEntry
│   └── ActorsEntry
├── SearchHistorySection
│   ├── SectionHeader(clear)
│   └── HistoryChips
└── HotSearchSection
    ├── SectionHeader
    ├── HotSearchLoading / HotSearchError / HotSearchList
    └── HotSearchRow

SearchResultScreen
├── SearchResultTopBar
│   ├── BackButton
│   └── SearchInputBar
└── SearchResultBody
    ├── ResultLoadingState
    ├── ResultErrorState
    ├── ResultEmptyState
    └── LazyColumn
        └── HomeDramaCard (reuse)

DiscoveryPlaceholderScreen
├── PlaceholderTopBar
└── PlaceholderBody
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `HomeTopBar` | Composable | 首页标题栏，新增搜索入口按钮 | 否 |
| `SearchEntryButton` | Composable | 首页右上角搜索入口 | 否 |
| `SearchHomeScreen` | Composable | 搜索发现页容器 | 否 |
| `SearchInputBar` | Composable | 输入关键词、触发搜索、展示按钮可用态 | 否 |
| `SearchQuickEntrySection` | Composable | 承载 4 个快捷入口 | 否 |
| `SearchHistorySection` | Composable | 展示本地历史、处理点击与清空 | 否 |
| `HotSearchSection` | Composable | 展示热搜 loading/content/error 三态 | 否 |
| `SearchResultScreen` | Composable | 搜索结果页容器 | 否 |
| `SearchResultStateContent` | Composable | 对结果页 loading/content/empty/error 分支渲染 | 否 |
| `HomeDramaCard` | Composable | 搜索结果卡片展示与动作触发 | 是 |
| `DiscoveryPlaceholderScreen` | Composable | 快捷入口承接页占位 | 部分复用现有 `PlaceholderScreen` 思路 |

### 3.3 Composable 接口定义

```kotlin
@Composable
fun SearchHomeScreen(
    onBack: () -> Unit,
    onSubmitQuery: (String) -> Unit,
    onOpenQuickEntry: (SearchQuickEntryType) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchHomeViewModel = hiltViewModel(),
)

@Composable
fun SearchResultScreen(
    onBack: () -> Unit,
    onOpenPlay: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchResultViewModel = hiltViewModel(),
)

@Composable
fun SearchInputBar(
    query: String,
    isSubmitting: Boolean,
    placeholder: String,
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
)
```

说明：
- `SearchInputBar` 在搜索发现页与结果页共用，避免两处输入校验规则漂移。
- `HomeDramaCard` 原有 `onPlay` / `onDetail` 回调保持不变，确保结果页与首页完全一致，只暴露“观看 / 详情”。
- 快捷入口类型用端内枚举 `SearchQuickEntryType` 表示，不改变共享语义。

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| 父 → 子 | Composable 参数 | `SearchHomeScreen` 向 section 组件传 `history`、`hotSearch`、`isLoading` |
| 子 → 父 | Lambda Callback | 历史词点击、热搜词点击、清空历史、快捷入口点击 |
| 页面 → ViewModel | 函数调用 | `onQueryChange`、`submitSearch()`、`retry()`、`clearHistory()` |
| Navigation → ViewModel | `SavedStateHandle` | 结果页读取 route `query` 作为初始关键词 |
| 跨页面共享 | route 参数 + 本地持久化 | 搜索结果页通过 route query 还原关键词；搜索发现页通过 DataStore 看到最新历史 |

### 3.5 Compose 交互与状态拆分

- 搜索发现页采用“整体可用、局部失败”策略：热搜失败仅影响热搜区块，历史与手动搜索仍可正常使用。
- 搜索结果页采用“单页面统一状态机”：loading / content / empty / error 互斥，避免旧结果与错误态同时出现。
- 搜索按钮可用规则与 spec 一致：`trim()` 后非空且长度不超过 50 时可触发；请求进行中禁用按钮，防止重复点击。
- 历史区建议使用 `Flow` 持续订阅 DataStore，这样搜索成功返回后即使从结果页返回，搜索发现页也能自动展示最新历史。

### 3.6 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | `LazyColumn` + 响应式间距 | 延续首页卡片布局，避免新增复杂双列排布 |
| 横竖屏 | ViewModel 保留状态 + `rememberSaveable` 保留输入草稿 | 旋转时不丢失用户已输入关键词 |
| 字体缩放 | 使用 Material3 Typography 与自适应换行 | 快捷入口文案和热搜词允许安全换行 |
| 深色模式 | 复用 `MaterialTheme` | 与首页风格保持一致 |
| 无障碍 | 为搜索按钮、返回按钮、热搜行设置 contentDescription / 语义标签 | 便于 TalkBack 朗读 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 UI | 职责 |
|-----------|---------|------|
| `SearchHomeViewModel` | `SearchHomeScreen` | 维护输入草稿、历史列表、热搜状态、快捷入口配置与搜索跳转触发 |
| `SearchResultViewModel` | `SearchResultScreen` | 读取 route query、加载结果、处理重搜 / 重试，并在成功后写入历史 |
| `MainNavigationViewModel` | `NavGraph` | 扩展新的 `PendingRoute`，承接搜索相关 deeplink 导航 |

### 4.2 `SearchHomeUiState` 定义

```kotlin
data class SearchHomeUiState(
    val draftQuery: String = "",
    val normalizedQuery: String = "",
    val history: List<SearchHistoryItem> = emptyList(),
    val hotSearches: List<HotSearchItem> = emptyList(),
    val isHotSearchLoading: Boolean = true,
    val hotSearchErrorMessage: String? = null,
    val quickEntries: List<SearchQuickEntry> = defaultQuickEntries(),
)
```

配套动作：
- `onQueryChange(input: String)`
- `submitFromInput()`
- `submitHistory(keyword: String)`
- `submitHotSearch(keyword: String)`
- `retryHotSearch()`
- `clearHistory()`

### 4.3 `SearchResultUiState` 定义

```kotlin
data class SearchResultUiState(
    val query: String = "",
    val draftQuery: String = "",
    val items: List<Drama> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasLoadedOnce: Boolean = false,
    val isRetrying: Boolean = false,
)
```

说明：
- `query` 表示当前已提交并与列表结果对应的关键词。
- `draftQuery` 表示顶部输入框当前文本，允许用户在结果页继续编辑。
- 首版分页与首页保持一致，仅请求 `page=1&pageSize=10`；pagination 信息不需要进入 UI state。

### 4.4 状态字段详情

#### SearchHomeUiState

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `draftQuery` | `String` | `""` | 输入框原始值 |
| `normalizedQuery` | `String` | `""` | `trim()` 后结果，用于按钮可用态和导航 |
| `history` | `List<SearchHistoryItem>` | `emptyList()` | 最近 10 条本地历史，按最近时间倒序 |
| `hotSearches` | `List<HotSearchItem>` | `emptyList()` | 热搜榜内容 |
| `isHotSearchLoading` | `Boolean` | `true` | 热搜区块 loading |
| `hotSearchErrorMessage` | `String?` | `null` | 热搜区块失败文案；不影响搜索能力 |
| `quickEntries` | `List<SearchQuickEntry>` | 固定 4 项 | 排行 / 新剧 / 分类 / 演员入口 |

#### SearchResultUiState

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `query` | `String` | `""` | 当前已提交搜索词 |
| `draftQuery` | `String` | `""` | 顶部可编辑输入框内容 |
| `items` | `List<Drama>` | `emptyList()` | 搜索结果列表 |
| `isLoading` | `Boolean` | `false` | 首次加载或重搜 loading |
| `errorMessage` | `String?` | `null` | 搜索失败提示 |
| `hasLoadedOnce` | `Boolean` | `false` | 标识是否已完成首轮请求 |
| `isRetrying` | `Boolean` | `false` | 是否为显式点击重试触发 |

### 4.5 UI 状态建模

#### 搜索发现页

| UI 状态 | 判别条件 | UI 层表现 |
|---------|---------|----------|
| Initial / Loading | `isHotSearchLoading == true` 且历史流尚未返回 | 历史先显示空或骨架；热搜区显示 loading |
| Content | 热搜成功或失败均已落定 | 渲染搜索框、快捷入口、历史区；热搜区显示内容或错误 |
| Partial Error | `hotSearchErrorMessage != null` | 仅热搜区块显示错误与重试按钮 |
| Empty History | `history.isEmpty()` | 历史区隐藏或显示“暂无搜索历史”轻提示 |

#### 搜索结果页

| UI 状态 | 判别条件 | UI 层表现 |
|---------|---------|----------|
| Loading | `isLoading == true` | 全页 loading，占据列表区域 |
| Success | `!isLoading && errorMessage == null && items.isNotEmpty()` | `LazyColumn + HomeDramaCard` |
| Empty | `hasLoadedOnce && !isLoading && errorMessage == null && items.isEmpty()` | 空态文案“未找到相关短剧” |
| Error | `errorMessage != null` | 错误态 + 重试按钮，保留顶部 query |

### 4.6 关键行为约束

- 搜索成功返回后才写入历史，包含空结果，不包含失败请求。
- 同一关键词短时间重复提交时，ViewModel 通过 `requestInFlight` 或 `activeQuery` 判断拦截并发，避免请求风暴。
- 结果页重搜时只保留最后一次有效关键词；若用户连续改词并多次触发，旧请求结果不得覆盖新 query 对应状态。
- `SavedStateHandle` 中缺失 query 时，结果页立即进入可理解错误态或回退，不凭空发空搜索请求。

---

## 5. Navigation 路由设计

### 5.1 导航方案

继续使用 Jetpack Navigation Compose。搜索相关页面都注册在 `home_graph` 内，原因如下：
- 搜索入口来自首页右上角，主返回路径是回到首页或搜索发现页；
- 搜索结果页与现有播放页、详情页共享同一内容消费子图；
- 可以直接复用现有 `MainNavigationViewModel + PendingRoute` 的 deeplink 消费机制。

### 5.2 路由清单

| 路由标识 | 目标 Composable | 参数 | 导航方式 | 说明 |
|---------|----------------|------|---------|------|
| `home` | `HomeScreen` | 无 | `NavController.navigate` | 首页 Feed |
| `search` | `SearchHomeScreen` | 无 | `NavController.navigate(AppDestination.search())` | 搜索发现页 |
| `search/result/{query}` 或 `search/result?query={query}` | `SearchResultScreen` | `query` | `NavController.navigate(AppDestination.searchResult(query))` | 搜索结果页，需对 query 做 Uri 编码 |
| `ranking` | `DiscoveryPlaceholderScreen` | 无 | `NavController.navigate(AppDestination.ranking())` | 排行承接页 |
| `classification` | `DiscoveryPlaceholderScreen` | 无 | `NavController.navigate(AppDestination.classification())` | 分类承接页 |
| `new-releases` | `DiscoveryPlaceholderScreen` | 无 | `NavController.navigate(AppDestination.newReleases())` | 首版 Native 占位承接页 |
| `actors` | `DiscoveryPlaceholderScreen` | 无 | `NavController.navigate(AppDestination.actors())` | 首版 Native 占位承接页 |
| `play/{videoId}` | `PlayerScreen` | `videoId` | 复用现有导航 | 观看主链路 |
| `detail/{dramaId}` | `DramaDetailScreen` | `dramaId` | 复用现有导航 | 详情主链路 |

说明：
- 共享设计规定 Android 结果页语义为 `search/result?query={query}`。实现上建议 `AppDestination` 对外暴露 `searchResult(query: String)`，内部统一 `Uri.encode(query)`，并通过 query 参数注册，以避免中文和空格路径解析问题。
- `new-releases` 与 `actors` 明确是 Native 占位页，不接 WebView、不跳 H5。

### 5.3 `AppDestination` 扩展示意

```kotlin
object Route {
    const val SEARCH = "search"
    const val SEARCH_RESULT = "search/result?query={query}"
    const val RANKING = "ranking"
    const val CLASSIFICATION = "classification"
    const val NEW_RELEASES = "new-releases"
    const val ACTORS = "actors"
}

fun search(): String = Route.SEARCH
fun searchResult(query: String): String = "search/result?query=${Uri.encode(query.trim())}"
fun ranking(): String = Route.RANKING
fun classification(): String = Route.CLASSIFICATION
fun newReleases(): String = Route.NEW_RELEASES
fun actors(): String = Route.ACTORS
```

### 5.4 `PendingRoute` 与 deeplink 扩展

建议新增：

```kotlin
sealed interface PendingRoute {
    data object Home : PendingRoute
    data class Play(val videoId: String) : PendingRoute
    data class Detail(val dramaId: String) : PendingRoute
    data object SearchHome : PendingRoute
    data class SearchResult(val query: String) : PendingRoute
    data object Ranking : PendingRoute
    data object Classification : PendingRoute
    data object NewReleases : PendingRoute
    data object Actors : PendingRoute
}
```

`NavGraph` 在 `LaunchedEffect(uiState.pendingRoute)` 中补充对应 `navigate(...)` 分支即可，继续复用“容器就绪后消费待执行路由”的现有模式。

### 5.5 Deep Link 处理

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `djsdrama://search` | `PendingRoute.SearchHome` | 无 |
| `djsdrama://search/result/{query}` | `PendingRoute.SearchResult` | 取 path segment，解码后得到 query |
| `djsdrama://ranking` | `PendingRoute.Ranking` | 无 |
| `djsdrama://classification` | `PendingRoute.Classification` | 无 |
| `djsdrama://new-releases` | `PendingRoute.NewReleases` | 无 |
| `djsdrama://actors` | `PendingRoute.Actors` | 无 |

实现注意点：
- `DeeplinkRouteParser` 现状按 `host + 首段 path` 解析。为了兼容 `djsdrama://search/result/{query}`，需要额外读取完整 path segments，并区分 `host=search` 下是首页还是结果页。
- query 解析后必须 `trim()`；空值则返回 `null`，由 `MainActivity` 安全忽略。
- 现有 `play` / `player` / `drama` 语义保持不变，新增 deeplink 仅扩展不破坏旧逻辑。

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | Retrofit + OkHttp（现有） | 不新增网络库 |
| 数据模型 | kotlinx.serialization DTO | 遵循现有 `DramaDto` / `DramaListResponseDto` 风格 |
| 请求封装 | `SearchRemoteDataSource` | 将 Retrofit 调用转为 `ApiResult` |
| Repository | `SearchRepositoryImpl` | 汇总热搜、搜索、本地历史能力 |
| 错误处理 | 统一 `ApiResult<T>` | 沿用现有 Android 约束 |

### 6.2 API 接口定义

```kotlin
interface ApiService {
    @GET("dramas/search")
    suspend fun searchDramas(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10,
    ): DramaListResponseDto

    @GET("dramas/hot-search")
    suspend fun getHotSearches(): HotSearchListResponseDto
}
```

说明：
- `ApiService` 保持相对路径写法，但**前提是** `AppConfig.apiBaseUrl` 必须统一以当前 Backend canonical `/api/` 作为结尾，例如 `http://10.0.2.2:3000/api/`；不得继续沿用旧的 `/api/v1` 默认值。
- 因此本期需要同步修改 `android/app/build.gradle.kts` 中 `api.base.url` 的默认回退值，并在文档/测试中固定该约束，确保 `@GET("dramas/search")` 最终命中 `GET /api/dramas/search`。
- `searchDramas` 成功响应严格复用 `DramaListResponseDto`，与 shared design 保持一致。
- `getHotSearches` 返回轻量 `HotSearchListResponseDto`，仅包含 `data: List<HotSearchItemDto>`。
- 不新增额外 header、认证或云端历史同步请求。

### 6.3 Repository / UseCase 接线

#### SearchRepository 接口建议

```kotlin
interface SearchRepository {
    suspend fun searchDramas(query: String, page: Int, pageSize: Int): ApiResult<List<Drama>>
    suspend fun getHotSearches(): ApiResult<List<HotSearchItem>>
    fun observeSearchHistory(): Flow<List<SearchHistoryItem>>
    suspend fun saveSearchHistory(keyword: String)
    suspend fun clearSearchHistory()
}
```

#### 接线策略

- 若希望尽量少改动现有 `DramaRepository`，搜索能力单独放入 `SearchRepository`，避免把本地历史职责混入 `DramaRepository`。
- `SearchRemoteDataSource` 负责调用 `ApiService.searchDramas()` / `getHotSearches()`。
- `SearchRepositoryImpl` 负责：
  - DTO -> domain model 映射；
  - 成功搜索后由 ViewModel 调用 `saveSearchHistory()`；
  - 热搜、本地历史读写与搜索请求在同一聚合仓储中收口。

### 6.4 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 热搜接口失败 | 0（用户手动重试） | 无 | 避免搜索页进入后台自动重试噪音 |
| 搜索接口失败 | 0（用户手动重试） | 无 | 结果页错误态提供重试按钮 |
| 重复点击搜索 | 0 | 直接忽略重复请求 | 由 ViewModel 拦截并发 |

### 6.5 网络状态监听

首版不新增全局网络监听。继续采用页面级错误态与重试按钮承接：
- 热搜失败只影响热搜区；
- 搜索失败进入结果页错误态；
- 网络切换后用户主动点击重试即可恢复。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| 搜索历史列表 | DataStore (Preferences) | `search_history_entries` | 无时间过期，仅保留最近 10 条 | 项目已有 DataStore 依赖，可直接使用 |
| 结果页临时关键词 | `SavedStateHandle` | `query` | 路由生命周期内有效 | 用于进程重建后恢复当前搜索词 |
| 搜索输入草稿 | `rememberSaveable` + ViewModel | `draftQuery` | 页面生命周期内有效 | 避免旋转丢输入 |

### 7.2 选择 DataStore 的原因

- 仓库已接入 `androidx.datastore:datastore-preferences`，满足“已有基础能力、不新增第三方依赖”的要求。
- 搜索历史是小规模键值数据，不需要 Room。
- 相比直接使用 `SharedPreferences`，DataStore 更符合当前 Kotlin + 协程 + Flow 的现有架构风格。

### 7.3 本地历史数据模型

建议 domain / local 层拆分：

```kotlin
@Serializable
private data class SearchHistoryRecord(
    val keyword: String,
    val updatedAt: String,
)

data class SearchHistoryItem(
    val keyword: String,
    val updatedAt: String,
)
```

说明：
- shared design 定义 `SearchHistoryItem` 仅为端侧本地模型，不进入后端契约。
- DataStore Preferences 不支持对象直接存储，建议将列表编码为 JSON 字符串后持久化到单一 key；使用项目现有 `kotlinx.serialization` 即可，无需新增依赖。

### 7.4 历史规则

| 规则 | Android 落地方式 |
|------|-----------------|
| trim 后去重 | `saveSearchHistory()` 先 `trim()`，空串直接忽略；按清洗后关键词去重 |
| 最近使用时间倒序 | 新词插入首位，旧词命中后移到首位并更新时间 |
| 最多 10 条 | 写入前裁剪到 10 条 |
| 支持一键清空 | DataStore 对应 key 写入空列表 |
| 仅成功搜索后写入 | 由 `SearchResultViewModel` 在 `ApiResult.Success` 分支调用保存 |

### 7.5 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| 搜索历史 | DataStore 持久化 | 无 | 清空或超过 10 条时移除最旧 |
| 热搜 | 不做磁盘缓存 | 无 | 每次进入搜索发现页重新请求 |
| 搜索结果 | 不做磁盘缓存 | 无 | 页面返回即释放，仅保存在内存状态 |

### 7.6 进程恢复策略

- 结果页使用 `SavedStateHandle` 恢复当前 query，并在必要时自动重发请求。
- 搜索发现页历史通过 DataStore Flow 恢复；热搜重新拉取即可。
- 若 DataStore 数据损坏，解析异常时降级为空历史并覆盖写回，避免页面不可用。

---

## 8. API 调用清单

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| `GET /api/dramas/hot-search` | 进入搜索发现页时 | `SearchHomeViewModel.loadHotSearches()` | 更新 `hotSearches`，清空 `hotSearchErrorMessage` | 仅热搜区块显示错误，不阻塞搜索 |
| `GET /api/dramas/search?q&page&pageSize` | 手动输入 / 点击历史 / 点击热搜 / 结果页重搜 | `SearchResultViewModel.query` | 更新结果列表；若请求成功则写入本地历史 | 结果页进入错误态并支持重试 |

约束：
- 固定首版 `page=1&pageSize=10`，与 spec 和 shared design 对齐。
- 请求最终必须命中 canonical `/api/dramas/search` 与 `/api/dramas/hot-search`，因此 `AppConfig.apiBaseUrl` 必须与 `ApiService` 相对路径拼接规则同时校准。
- 不引入额外接口；快捷入口承接页不请求新 API。

---

## 9. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Android 端实现方式 |
|---------|---------------|-------------------|
| 首页搜索入口 | 首页右上角进入 Native 搜索发现页 | `HomeScreen` 顶部栏新增搜索按钮，`navController.navigate(AppDestination.search())` |
| 页面结构 | 搜索发现页包含顶部返回 + 搜索框、快捷入口、历史、热搜 | `SearchHomeScreen` 用 `LazyColumn` 分 section 渲染 |
| 结果页结构 | 顶部保留可编辑搜索框，下方承载 loading/content/empty/error | `SearchResultScreen + SearchResultUiState` |
| 搜索历史写入时机 | 仅成功返回后写历史，空结果也写，失败不写 | `SearchResultViewModel` 仅在 `ApiResult.Success` 分支调用 `SaveSearchHistoryUseCase` |
| 搜索历史规则 | trim 去重、倒序、最多 10 条、支持清空 | `SearchHistoryLocalDataSource + SearchRepositoryImpl` 统一处理 |
| 触发来源收敛 | 手动输入、历史、热搜走同一搜索执行器 | 搜索发现页所有触发最终导航到 `search/result?query=...`，结果页统一发请求 |
| 结果页交互语义 | 复用首页卡片动作，只保留“观看 / 详情” | 直接复用 `HomeDramaCard`，不提供整卡点击 |
| 快捷入口承接 | `ranking/classification` 为后续承接；`new-releases/actors` 首版占位 | 4 个 route 均注册 Native 页面；其中文案标识“功能建设中” |
| deeplink 语义 | 扩展 search / ranking / classification / new-releases / actors | `DeeplinkRouteParser + PendingRoute + NavGraph` 扩展 |
| 搜索 API 匹配规则 | `title + category`，大小写不敏感包含匹配 | Android 只消费结果，不在端上重复实现匹配逻辑 |
| 失败降级 | 热搜失败不影响手动搜索；搜索失败支持重试 | 搜索发现页 partial error；结果页 full error |

---

## 10. 边界与错误处理

### 10.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| 网络层 | `SearchRemoteDataSource` -> `ApiResult` | 在 Retrofit 异常包装之外，新增后端 error body 解析，把 `{ error: { code, message } }` 转为 `ApiResult.Error` |
| Repository | DTO 映射 + 本地历史容错 | 解析异常转为空历史或 `ApiResult.Exception`；显式消费 `ApiResult.Error` 以区分服务端业务失败与网络异常 |
| ViewModel | `try-catch` + 显式状态机 | 防止协程异常导致页面白屏 |
| UI 层 | 内联错误态 / 重试按钮 | 不新增全局 Snackbar 依赖 |

### 10.2 错误码映射表

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `VALIDATION_ERROR` | 输入内容无效，请检查后重试 | 搜索按钮不可用优先兜底；若服务端返回则结果页内联错误 |
| `INTERNAL_ERROR` | 搜索失败，请稍后重试 | 结果页错误态 + 重试按钮 |
| `NETWORK_ERROR` | 网络异常，请检查后重试 | 结果页错误态或热搜区块错误态 |

说明：
- 热搜接口共享设计只要求 `INTERNAL_ERROR`，但 Android 端可将网络异常统一映射为展示文案“热搜加载失败，请重试”。
- 不暴露服务端原始错误码给用户。

### 10.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| 空输入提交 | `trim()` 后为空 | 搜索按钮 disabled / 提交无效，不发请求 | 🔴 |
| 超长输入 | 超过 50 字符 | UI 限制长度或提交前拦截 | 🔴 |
| 连续点击搜索 | 请求进行中再次点击 | ViewModel 忽略重复提交 | 🔴 |
| 热搜加载失败 | `/hot-search` 报错 | 仅热搜区块显示错误，历史和输入继续可用 | 🟡 |
| 搜索失败 | `/search` 报错 | 结果页错误态，不写历史 | 🔴 |
| 搜索结果为空 | 请求成功但无匹配 | 展示空态并写历史 | 🟡 |
| 搜索 query 缺失 | route / deeplink 不合法 | 不发请求，安全忽略或回退 | 🔴 |
| DataStore 数据损坏 | JSON 解析失败 | 降级为空历史并覆盖损坏值 | 🟡 |
| 旋转屏幕 | 配置变更 | ViewModel 与 `rememberSaveable` 保留 query | 🟡 |
| 进程被杀恢复 | 系统回收后恢复 | 结果页依赖 `SavedStateHandle` 恢复 query 并重新加载 | 🟡 |

### 10.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `SearchHomeScreen` 热搜区 | 是 | 是 | 否（少于 10 条按实际展示） | 是 | 否 |
| `SearchHomeScreen` 历史区 | 否 | 是 | 是 | 否（读取失败降级为空） | 否 |
| `SearchResultScreen` | 是 | 是 | 是 | 是 | query 缺失时可直接返回上一页或展示说明 |
| `DiscoveryPlaceholderScreen` | 否 | 是 | 否 | 否 | 否 |

---

## 11. 测试策略

### 11.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | `SearchHomeViewModel`、`SearchResultViewModel`、历史规则、repository DTO 映射 | 关键状态机与分支全覆盖 | JUnit4 + MockK + Turbine |
| 路由测试 | `AppDestination` route 生成、`DeeplinkRouteParser` 新增 host 解析、`MainNavigationViewModel` pending route | 新增搜索路由全部覆盖 | JUnit4 |
| UI 纯函数测试 | `SearchInputBar` 可提交判定、快捷入口映射、结果卡片复用约束 | 关键规则覆盖 | JUnit4 |
| DataStore 单元测试 | 历史去重、裁剪、清空、损坏恢复 | 核心规则覆盖 | JUnit4 + 临时文件 / TestScope |

说明：遵循当前 `android/CLAUDE.md`，测试优先放在 `src/test/` 纯 JVM 层；若当前工程尚未配置 instrumentation，则本期不强依赖 Compose UI instrumentation。

### 11.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| A-01 | 搜索发现页初始化 | 本地历史为空，热搜请求成功 | 创建 `SearchHomeViewModel` 并调用初始化 | 历史为空、热搜展示、快捷入口固定 4 项 | 单元 |
| A-02 | 热搜失败局部降级 | 热搜接口失败 | 初始化搜索发现页 | `hotSearchErrorMessage` 非空，但历史与输入仍可用 | 单元 |
| A-03 | 历史点击触发搜索 | 已有历史词 | 点击历史词 | 输出导航事件到结果页 route | 单元 |
| A-04 | 搜索成功写历史 | `/search` 成功返回结果或空结果 | 结果页发起搜索 | 历史保存被调用一次 | 单元 |
| A-05 | 搜索失败不写历史 | `/search` 返回错误 | 结果页发起搜索 | `errorMessage` 非空，历史保存不被调用 | 单元 |
| A-06 | 重复点击搜索被拦截 | 首次请求未完成 | 连续调用提交 | 仅触发一次 repository 搜索 | 单元 |
| A-07 | 历史规则去重裁剪 | 已有 10 条历史且包含重复项 | 保存新关键词 / 已有关键词 | 结果按最近时间倒序且最多 10 条 | 单元 |
| A-08 | 一键清空历史 | 本地已有历史 | 调用 `clearHistory()` | DataStore 中变为空列表 | 单元 |
| A-09 | 搜索 route 生成 | query 含空格/中文 | 调用 `AppDestination.searchResult(query)` | 生成可导航、可解码的 route | 路由测试 |
| A-10 | deeplink 解析搜索结果页 | 输入 `djsdrama://search/result/逆袭` | 调用 parser | 得到 `PendingRoute.SearchResult("逆袭")` | 路由测试 |
| A-11 | 快捷入口承接页 | 点击 `new-releases` / `actors` | 导航进入承接页 | 展示 Native 占位文案，不跳 Web | 单元 / 路由测试 |
| A-12 | 结果卡片动作复用 | 结果页渲染卡片 | 点击操作 | 仅有“观看 / 详情”两种回调 | UI 纯函数测试 |

### 11.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| 搜索 API | MockK mock `SearchRepository` 或 `SearchRemoteDataSource` | 优先验证 ViewModel 状态流转 |
| 热搜 API | MockK | 覆盖 success / error 两条路径 |
| 本地历史 | fake `SearchHistoryLocalDataSource` 或临时 DataStore 文件 | 覆盖去重、裁剪、清空、损坏恢复 |
| 导航输出 | 断言 route 字符串或单次事件 | 避免在 JVM 测试中依赖真实 `NavController` |

### 11.4 回归关注点

- 现有 `HomeScreenTest`、`RoutesTest`、`DeeplinkRouteParserTest` 需补充搜索相关 case，而不是仅新建平行测试，确保旧路由未被破坏。
- 首页卡片回归：搜索结果页复用 `HomeDramaCard` 后，不得影响首页 Feed 现有测试与交互。
- 若后续接入 PRD-05 / PRD-06 真实页面，当前 `ranking` / `classification` 占位页测试可平滑迁移为 route 存在性与返回路径测试。

---

## 12. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | — | — | 本期明确不新增第三方依赖；DataStore Preferences 已在项目中存在 |

---

## 13. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 结果页 route 直接用 path 承载 query 导致中文/空格解析异常 | 搜索结果页、deeplink | 🔴 | 中 | route 构造统一 `Uri.encode`，优先使用 query 参数注册 | 解析失败时回退到搜索发现页并保留输入 |
| 本地历史序列化格式处理不当 | 搜索发现页历史 | 🟡 | 中 | 使用 `kotlinx.serialization` 编解码并补损坏恢复测试 | 解析失败时清空历史，保证页面可用 |
| 首页搜索入口改造影响现有 Feed 布局 | 首页 | 🟡 | 中 | 顶栏与 Feed 内容解耦，保持 `HomeDramaCard` 不变 | 若布局冲突，先保留 icon-only 入口 |
| 快捷入口未来被真实页面替换时路由漂移 | 搜索发现页、PRD-05/06 | 🟡 | 中 | 本期先固定 canonical route，真实页面仅替换承接内容不改 route | 保留占位页作为 fallback |
| 搜索成功后写历史的时机被误放到提交前 | 历史正确性 | 🔴 | 中 | 在 `SearchResultViewModel` success 分支集中调用保存，并加测试约束 | 出现问题时先关闭写历史逻辑，保主链路 |

---

## 14. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/features/homepage-feed/index.md` | 入口与路由 / 多端实现 / 状态管理 | Android 首页已具备 `HomeScreen`、`HomeViewModel`、`HomeDramaCard` 与 `play/detail` 主链路，可复用为搜索结果页基础 |
| `wiki/features/deeplink/index.md` | Android Deeplink 流程 / 边界处理 | 当前 `PendingRoute` 与 `DeeplinkRouteParser` 已接入，可按同模式扩展 search 系列 deeplink |
| `wiki/architecture/overview.md` | 整体架构 | Android 继续保持 Native 页面承载策略 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `/Users/bytedance/Documents/github/djs66256.github.io/fork-any-application/.claude/worktrees/2026-07-26-prd-04-search-discovery/android/CLAUDE.md` | Android 技术栈、分层与测试约束 |
| `/Users/bytedance/Documents/github/djs66256.github.io/fork-any-application/.claude/worktrees/2026-07-26-prd-04-search-discovery/docs/specs/2026-07-26-prd-04-search-discovery/spec.md` | 搜索发现需求边界、路由约束、历史规则、最小自动化验收 |
| `/Users/bytedance/Documents/github/djs66256.github.io/fork-any-application/.claude/worktrees/2026-07-26-prd-04-search-discovery/docs/specs/2026-07-26-prd-04-search-discovery/design.md` | 共享 API、route/deeplink、跨端状态机与约束 |
| `/Users/bytedance/Documents/github/djs66256.github.io/fork-any-application/.claude/worktrees/2026-07-26-prd-04-search-discovery/android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 当前 route 常量、`PendingRoute` 定义 |
| `/Users/bytedance/Documents/github/djs66256.github.io/fork-any-application/.claude/worktrees/2026-07-26-prd-04-search-discovery/android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt` | 当前 deeplink 仅支持 `open/play/player/drama` |
| `/Users/bytedance/Documents/github/djs66256.github.io/fork-any-application/.claude/worktrees/2026-07-26-prd-04-search-discovery/android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | `home_graph` 当前注册结构与 pending route 消费方式 |
| `/Users/bytedance/Documents/github/djs66256.github.io/fork-any-application/.claude/worktrees/2026-07-26-prd-04-search-discovery/android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | `HomeDramaCard`、“观看 / 详情”按钮与首页列表布局 |
| `/Users/bytedance/Documents/github/djs66256.github.io/fork-any-application/.claude/worktrees/2026-07-26-prd-04-search-discovery/android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt` | 现有 StateFlow + requestInFlight 状态机模式 |
| `/Users/bytedance/Documents/github/djs66256.github.io/fork-any-application/.claude/worktrees/2026-07-26-prd-04-search-discovery/android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 现有 Retrofit 接口风格 |
| `/Users/bytedance/Documents/github/djs66256.github.io/fork-any-application/.claude/worktrees/2026-07-26-prd-04-search-discovery/android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt` | 现有 `ApiResult` 包装方式 |
| `/Users/bytedance/Documents/github/djs66256.github.io/fork-any-application/.claude/worktrees/2026-07-26-prd-04-search-discovery/android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt` | 现有 repository DTO->domain 映射模式 |
| `/Users/bytedance/Documents/github/djs66256.github.io/fork-any-application/.claude/worktrees/2026-07-26-prd-04-search-discovery/android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | Hilt repository 绑定方式 |
| `/Users/bytedance/Documents/github/djs66256.github.io/fork-any-application/.claude/worktrees/2026-07-26-prd-04-search-discovery/android/app/src/test/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModelTest.kt` | ViewModel + Turbine 测试模式 |
| `/Users/bytedance/Documents/github/djs66256.github.io/fork-any-application/.claude/worktrees/2026-07-26-prd-04-search-discovery/android/app/src/test/java/com/djs66256/short_drama/navigation/DeeplinkRouteParserTest.kt` | Deeplink parser 测试结构 |
| `/Users/bytedance/Documents/github/djs66256.github.io/fork-any-application/.claude/worktrees/2026-07-26-prd-04-search-discovery/android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | route 生成断言模式 |
