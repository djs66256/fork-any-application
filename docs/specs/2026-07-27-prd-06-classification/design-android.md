# Android 端技术方案：PRD-06 分类浏览

> 创建日期：2026-07-27
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

Android 端在现有单 Activity + Navigation Compose + Hilt + ViewModel + Repository 架构上，将搜索发现页已有的 `classification` Native 承接路由从占位页替换为真实分类页。实现继续遵循 `android/CLAUDE.md` 约束，不新增第三方依赖，复用现有 Compose + StateFlow + Retrofit 体系完成分类标签加载、左侧维度导航、右侧分组标签矩阵与标签点击跳搜索结果页。

```text
SearchHomeScreen quick entry
  -> AppDestination.classification()
     -> ClassificationScreen
        -> collects ClassificationUiState from ClassificationViewModel
           -> GetClassificationTagsUseCase(gender)
              -> ClassificationRepository.getClassificationTags(gender)
                 -> ClassificationRemoteDataSource.getClassificationTags(gender)
                    -> ApiService.getDramaTags(gender)
                       -> GET /api/dramas/tags?gender=all|male|female
        -> click left dimension
           -> viewModel.onDimensionSelected(key)
           -> scrollTo(sectionAnchor)
        -> click tag chip
           -> onOpenSearchResult(AppDestination.searchResult(tag))
              -> SearchResultScreen
                 -> SearchResultViewModel reads query from SavedStateHandle
                 -> SearchDramasUseCase(query)
                 -> GET /api/dramas/search?q=标签名&page=1&pageSize=10
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 不变 | 继续复用既有 `classification` canonical route 与 `search/result?query=...` |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 用 `ClassificationScreen` 替换当前 `PlaceholderScreen(title = "分类")` |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/model/SearchQuickEntry.kt` | 不变 / 联动验证 | 搜索发现页分类入口 route 继续指向 `AppDestination.classification()` |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchHomeViewModel.kt` | 不变 / 联动验证 | `OpenQuickEntry(route)` 事件继续驱动分类页导航 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchResultViewModel.kt` | 不变 / 复用 | 继续承接标签点击后的查询与搜索结果状态机 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增 `GET /api/dramas/tags` Retrofit 定义 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/SearchRemoteDataSource.kt` | 不变 | 搜索结果页请求链路不改，由分类页复用其成果 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | 参考复用 | 分类页可复用其请求 token、乱序保护与状态建模思路 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/SearchRepository.kt` | 不变 | 分类标签数据不混入搜索仓储，避免职责污染 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt` | 不变 | classification 更适合走独立数据源，而不是塞入首页 feed 数据源 |

### 1.2 设计原则

- 分类页继续作为首页频道下的子路由，不新增 top-level tab。
- 标签点击后只复用现有 `AppDestination.searchResult(query)`，不新增独立分类结果页 route。
- 分类数据链路采用与 Search / Ranking 平行的独立模块组织：`feature/classification + domain/repository + data/datasource/repository`。
- 页面状态以 `StateFlow` 驱动，使用请求 token 或等价 query key 保护快速切换 gender 时的乱序返回。
- 不引入 Paging / Coil / FlowLayout 第三方依赖；标签矩阵基于 Compose 现有布局能力实现。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/classification/ui/ClassificationScreen.kt` | 新增 | 分类页根 Composable，承载顶部 Tab、左侧维度、右侧标签矩阵、状态页 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt` | 新增 | 管理默认加载、gender 切换、维度选中、并发去重与标签点击 query 规范化 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/classification/model/ClassificationUiModel.kt` | 新增 | 分类页 UI 映射模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/ClassificationTagModels.kt` | 新增 | `ClassificationGender`、`ClassificationDimension`、`ClassificationTagsPayload` |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/ClassificationRepository.kt` | 新增 | 定义分类标签查询接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetClassificationTagsUseCase.kt` | 新增 | 分类标签读取用例 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/ClassificationTagsResponseDto.kt` | 新增 | 对齐 shared design 的 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/ClassificationRemoteDataSource.kt` | 新增 | 复用 `SearchRemoteDataSource` 的错误解析模式封装分类接口 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/ClassificationRepositoryImpl.kt` | 新增 | DTO -> Domain 映射与错误透传 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增 `getDramaTags(@Query("gender"))` |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 注入 `ClassificationRepository` |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 注册真实 `ClassificationScreen`，并把标签点击映射到 `searchResult` |
| `android/app/src/test/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModelTest.kt` | 新增 | 覆盖状态机、Tab 切换、维度重置、并发保护 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/ClassificationRepositoryImplTest.kt` | 新增 | 覆盖 DTO 映射、空维度保留、错误透传 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 继续验证 `classification()` 与 `searchResult(query)` route 语义 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/search/viewmodel/SearchHomeViewModelTest.kt` | 轻微修改 | 继续验证 classification quick entry 导航事件 |

---

## 3. UI 层设计

### 3.1 组件层级树

```text
ClassificationScreen
├── ClassificationTopBar
│   ├── BackButton
│   └── Title("分类")
├── ClassificationGenderTabRow
│   ├── AllTab
│   ├── MaleTab
│   └── FemaleTab
└── ClassificationBody
    ├── ClassificationLoadingState
    ├── ClassificationErrorState
    └── Row
        ├── ClassificationDimensionRail
        │   ├── EraBackgroundItem
        │   ├── ThemePlotItem
        │   └── CharacterSettingItem
        └── ClassificationTagSectionList
            ├── ClassificationSectionHeader
            ├── ClassificationTagChipGrid
            │   └── ClassificationTagChip (items)
            └── ClassificationEmptySectionState
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `ClassificationScreen` | Composable | 分类页根容器，连接 ViewModel 与导航回调 | 否 |
| `ClassificationGenderTabRow` | Composable | 顶部性别 Tab：全部 / 男频 / 女频 | 否 |
| `ClassificationDimensionRail` | Composable | 左侧维度导航与选中高亮 | 否 |
| `ClassificationTagSectionList` | Composable | 右侧分组标题、标签矩阵与滚动锚点 | 否 |
| `ClassificationTagChip` | Composable | 标签胶囊点击区 | 否 |
| `ClassificationLoadingState` | Composable | 首屏 / 切换性别 loading | 是 |
| `ClassificationErrorState` | Composable | 错误态与重试按钮 | 是 |
| `SearchResultScreen` | Composable | 标签点击后的结果页承接 | 是 |

### 3.3 Composable 接口定义

```kotlin
@Composable
fun ClassificationScreen(
    onBack: () -> Unit,
    onOpenSearchResult: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClassificationViewModel = hiltViewModel(),
)

@Composable
fun ClassificationTagChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

说明：
- `onOpenSearchResult` 最终接收的是完整 route 字符串，统一由 `AppDestination.searchResult(query)` 构造；
- 分类页不直接知道搜索结果页内部状态机；
- 左侧维度点击只更新滚动位置与选中态，不触发新请求。

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| `ClassificationViewModel` -> `ClassificationScreen` | `StateFlow<ClassificationUiState>` | 页面状态渲染 |
| `ClassificationViewModel` -> `ClassificationScreen` | `SharedFlow<ClassificationEffect>`（可选） | 滚动到指定锚点、轻提示 |
| `ClassificationScreen` -> 子组件 | Composable 参数 | Tab 选中态、分组数据、点击行为 |
| 子组件 -> `ClassificationViewModel` | Lambda Callback | 切换 gender、点击维度、点击标签、重试 |
| `NavGraph` -> SearchResult | `navController.navigate(route)` | 标签点击进入既有搜索结果页 |

### 3.5 交互与状态细节

- 首次进入分类页时默认请求 `gender=all`。
- 页面布局固定为顶部 Tab + 左右双栏；左侧始终渲染三个维度项，不能因空维度而少一项。
- 切换任一 gender 时：
  - 清空旧 tags 展示；
  - 重置 `selectedDimensionKey` 为首个维度；
  - 右侧滚动回首个锚点；
  - 发起新请求。
- 点击标签时：
  - 先用与搜索页一致的 `normalizeSearchQueryOrNull` 规则清洗；
  - 成功则 `onOpenSearchResult(AppDestination.searchResult(normalized))`；
  - 清洗后为空则忽略点击。
- 如果某维度 `tags=[]`，右侧对应 section 渲染空态文案，不隐藏 section header。

### 3.6 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 小屏手机 | 左右双栏比例固定，右侧可滚动 | 以竖屏手机为主设计 |
| 横竖屏 | `rememberSaveable` 保持当前 gender / selectedDimension | 旋转后保留当前筛选 |
| 字体缩放 | Material3 Typography + 文本截断 | 标签胶囊单行截断，标题允许两行 |
| 深色模式 | 复用 `ShortDramaTheme` | 选中态与未选中态都使用主题色 |
| 长标签 | 胶囊自动撑开宽度，行内换行由网格布局控制 | 不引入第三方 FlowRow 库 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 UI | 职责 |
|-----------|---------|------|
| `ClassificationViewModel` | `ClassificationScreen` | 管理默认加载、gender 切换、维度选中、旧请求失效保护、标签 query 规范化 |

### 4.2 状态定义

```kotlin
data class ClassificationUiState(
    val selectedGender: ClassificationGender = ClassificationGender.ALL,
    val selectedDimensionKey: ClassificationDimensionKey = ClassificationDimensionKey.ERA_BACKGROUND,
    val dimensions: List<ClassificationDimensionUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val hasLoadedOnce: Boolean = false,
)

sealed interface ClassificationEffect {
    data class ScrollToDimension(val key: ClassificationDimensionKey) : ClassificationEffect
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `selectedGender` | `ClassificationGender` | `ALL` | 顶部性别 Tab 选中值 |
| `selectedDimensionKey` | `ClassificationDimensionKey` | `ERA_BACKGROUND` | 左侧默认选中第一项 |
| `dimensions` | `List<ClassificationDimensionUiModel>` | `emptyList()` | 当前 gender 下的三维度分组 |
| `isLoading` | `Boolean` | `true` | 首屏加载中 |
| `isRefreshing` | `Boolean` | `false` | 已有内容后切换 gender 的刷新态 |
| `errorMessage` | `String?` | `null` | 首屏或切换失败提示 |
| `hasLoadedOnce` | `Boolean` | `false` | 是否完成过至少一次请求 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | UI 层表现 |
|---------|---------|----------|
| Initial Loading | `isLoading && !hasLoadedOnce` | 全页 loading |
| Refreshing | `isRefreshing` | 保留顶部 Tab，内容区显示刷新态 |
| Success | `!isLoading && errorMessage == null` | 展示双栏内容与标签矩阵 |
| Error | `errorMessage != null && dimensions.isEmpty()` | 全页错误态 + 重试 |
| Empty Dimension | 某个分组 `tags.isEmpty()` | 该分组展示空态文案 |

### 4.5 关键行为设计

#### 初始化与筛选

- `ClassificationViewModel` 默认以 `ALL` 初始化，不从 route 读取 query 参数，因为当前 canonical route 仅为 `classification`。
- 首次进入时触发 `refresh(gender = ALL)`。
- `onGenderSelected()` 只在新值与当前值不一致时触发请求。

#### 请求去重与乱序保护

- 参考 `RankingViewModel` 维护 `latestGender` + `requestToken`。
- 每次切换 gender 生成新 token；返回时只有 token 与当前选中 gender 同时匹配才允许写状态。
- 这样可保证用户快速连续切换 `all -> male -> female` 时，只看到最后一次结果。

#### 维度选择

- `onDimensionSelected(key)` 不触发网络，仅更新 `selectedDimensionKey`，同时发出 `ScrollToDimension(key)`。
- gender 切换成功后，强制把 `selectedDimensionKey` 重置为第一维度，并发出滚动到首项的 effect。

#### 标签点击

- ViewModel 提供 `buildSearchRoute(rawTag: String): String?`：
  - 先调用 `normalizeSearchQueryOrNull(rawTag)`；
  - 成功则返回 `AppDestination.searchResult(normalized)`；
  - 失败则返回 `null`。
- Screen 层只消费 route，不重复拼 query。

---

## 5. Navigation 路由设计

### 5.1 导航方案

继续使用 Jetpack Navigation Compose。分类页仍归属 `home_graph`，以保证：
- 搜索发现页快捷入口无须变更；
- 返回路径稳定回到搜索页 / 首页上下文；
- 标签点击可直接复用已有 `search/result?query=...` route。

### 5.2 路由清单

| 路由标识 | 目标 Composable | 参数 | 导航方式 | 说明 |
|---------|----------------|------|---------|------|
| `classification` | `ClassificationScreen` | 无 | `NavController.navigate(AppDestination.classification())` | 分类页真实承接 |
| `search/result?query={query}` | `SearchResultScreen` | `query` | `navController.navigate(AppDestination.searchResult(query))` | 标签点击后的结果页 |
| `search` | `SearchHomeScreen` | 无 | 返回 / 重新进入 | 搜索发现页入口保持不变 |

### 5.3 路由定义建议

```kotlin
object Route {
    const val CLASSIFICATION = "classification"
    const val SEARCH_RESULT = "search/result?query={query}"
}

fun classification(): String = Route.CLASSIFICATION

fun searchResult(query: String): String =
    "search/result?query=${encodeRouteParam(normalizeSearchQuery(query))}"
```

说明：
- `classification` route 保持不变，只替换 UI 内容；
- 不新增 `classification/result` 或 `search?q=` 路由别名；
- 标签 query 编码规则继续完全复用现有 `AppDestination.searchResult()`。

### 5.4 导航图

```kotlin
composable(route = AppDestination.Route.CLASSIFICATION) {
    ClassificationScreen(
        onBack = { navController.popBackStack() },
        onOpenSearchResult = { route ->
            navController.navigate(route)
        },
    )
}
```

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | Retrofit | 继续复用现有客户端 |
| 数据模型 | kotlinx.serialization DTO | 分类接口新增 DTO |
| 错误处理 | `ApiResult<T>` | 由 `ClassificationRemoteDataSource` 统一包装 |
| 请求拦截器 | 现有 `AuthInterceptor` | 分类接口为匿名只读，不新增逻辑 |

### 6.2 API 接口定义

```kotlin
interface ApiService {
    @GET("dramas/tags")
    suspend fun getDramaTags(
        @Query("gender") gender: String = "all",
    ): ClassificationTagsResponseDto
}
```

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 分类标签请求失败 | 不自动重试 | — | 交给 ViewModel 的 retry 按钮处理 |
| 搜索结果页请求失败 | 维持现有实现 | — | 分类页不复制搜索逻辑 |

### 6.4 网络状态监听

本 PRD 不新增 `ConnectivityManager` 监听或全局断网提示；继续沿用页面级错误态与重试策略。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| 分类标签数据 | 不持久化 | — | 页面生命周期内内存态 | 避免与搜索索引错位 |
| 当前选中 gender | `SavedStateHandle` / `rememberSaveable`（可选） | 内存恢复 | 配置变更有效 | 用于旋转场景 |
| 当前选中维度 | `SavedStateHandle` / `rememberSaveable`（可选） | 内存恢复 | 配置变更有效 | 性别切换后仍需重置 |

### 7.2 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| 分类标签列表 | 仅 ViewModel 内存态 | 页面存活期间 | 页面销毁释放 |
| 搜索结果 | 维持现有搜索页策略 | 现有实现 | 不在本 PRD 改造 |

### 7.3 持久化说明

本 PRD 不新增 Room / DataStore 结构，不需要 migration。

---

## 8. 测试策略

### 8.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| ViewModel | 默认加载、gender 切换、维度重置、乱序保护、标签 route 构造 | JUnit4 + Turbine + MockK |
| Repository | DTO -> Domain 映射、空维度保留、错误透传 | JUnit4 + MockK |
| DataSource | 解析后端错误体、成功返回 DTO | JUnit4 |
| Navigation | classification route 不变、标签点击复用 search result route | JUnit4 |

### 8.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 测试类型 |
|------|---------|------|---------|---------|
| AND-T01 | 首次进入默认加载 all | 进入分类页 | 请求 `gender=all`，成功后默认维度为首项 | ViewModel |
| AND-T02 | 切换到 male | 选择 `MALE` | 刷新数据并滚动回第一维度 | ViewModel |
| AND-T03 | 快速切换 gender | `ALL -> MALE -> FEMALE` | 只有最后一次结果写回 | ViewModel |
| AND-T04 | 点击左侧维度 | 选择 `THEME_PLOT` | 更新选中态并发出滚动 effect | ViewModel |
| AND-T05 | 空维度保留 | 某分组 `tags=[]` | UI model 仍保留该分组 | Repository / ViewModel |
| AND-T06 | 标签点击跳搜索 | 点击 `萌宝` | route 为 `search/result?query=萌宝`（编码后） | ViewModel / Navigation |
| AND-T07 | 标签清洗失败 | 空字符串 / 全空白标签 | 不返回 route，不导航 | ViewModel |
| AND-T08 | 接口错误 | `/api/dramas/tags` 返回 500 | 全页错误态 + 重试 | ViewModel |
| AND-T09 | Retrofit query 正确 | `gender=female` | 正确传递到 `ApiService.getDramaTags()` | Repository / DataSource |

### 8.3 不在本期测试范围

- Compose UI 像素级截图；
- 真机滚动性能专项；
- 搜索结果页内部已有状态机的重复测试。

---

## 9. 参考资料

| 文件 | 关键内容 |
|------|---------|
| `docs/specs/2026-07-27-prd-06-classification/spec.md` | 分类页三层结构与标签复用搜索结果页 |
| `docs/specs/2026-07-27-prd-06-classification/design.md` | shared contract、状态机、错误语义 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 既有 `classification` / `search/result?query=...` route |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 当前 classification 仍由 placeholder 承接 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchHomeViewModel.kt` | classification quick entry 事件来源 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchResultViewModel.kt` | 搜索结果页 query 承接逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 分类接口新增落点 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | 请求去重与乱序保护参考实现 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/SearchRemoteDataSource.kt` | 错误解析模式参考 |
