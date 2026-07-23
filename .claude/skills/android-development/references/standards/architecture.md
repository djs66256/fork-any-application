# 架构设计 — Android

> 本文档定义 Android 端的整体架构设计规范。

---

## 1. 整体架构

ShortDrama Android 端采用分层架构（Layered Architecture），各层职责单一、依赖方向由外向内（UI → Domain → Data），内部层不感知外部层的存在。

```
UI Layer (Composable + ViewModel)
    ↓ depends on
Domain Layer (UseCase)
    ↓ depends on
Data Layer (Repository + DataSource)
```

**各层职责**：

| 层 | 组件 | 职责 | 不应做什么 |
|----|------|------|-----------|
| **UI** | Composable, ViewModel | 渲染界面、收集 StateFlow、将用户事件转发给 ViewModel | 直接调用 Repository、包含业务逻辑、直接读写数据库 |
| **Domain** | UseCase | 封装单一业务操作、组合多个 Repository 调用、处理业务规则 | 持有 UI 状态、感知 Android 组件生命周期 |
| **Data** | Repository, DataSource | 统一数据来源（网络/本地缓存）、缓存策略、数据模型转换 | 包含 UI 相关逻辑、持有 ViewModel 引用 |

**数据流方向**：
- 向下（UI → Data）：用户操作 → ViewModel → UseCase → Repository → DataSource（Remote/Local）
- 向上（Data → UI）：DataSource → Repository（转为领域模型）→ UseCase → ViewModel（转为 UIState）→ Composable 自动重组

**依赖反转**：Repository 层使用接口定义，具体的 RemoteDataSource / LocalDataSource 通过 Hilt 注入，ViewModel / UseCase 只依赖接口，不依赖具体实现。

### 1.1 数据模型约定

为保持关注点分离，各层使用各自的数据模型：

| 层 | 模型前缀 | 存放位置 | 示例 |
|----|---------|---------|------|
| Data | `Dto` / `Entity` | `data/remote/dto/` / `data/local/entity/` | `VideoDto`, `VideoEntity` |
| Domain | 无前缀（领域模型） | `domain/model/` | `Video`, `HomeFeed` |
| UI | `Ui` 后缀 | `ui/<feature>/state/` | `VideoUi`, `HomeFeedUi` |

各层之间转换使用 `Mapper`（扩展函数或专用类）：
```kotlin
// DTO → Domain
fun VideoDto.toDomain(): Video = Video(id = id, title = title, coverUrl = coverUrl)

// Domain → UIState
fun Video.toUi(): VideoUi = VideoUi(id = id, title = title, coverUrl = coverUrl)
```

### 1.2 包结构约定

```
com.djs66256.short_drama/
├── app/                         # Application + Hilt 入口
├── core/                        # 通用基础设施（各 feature 共享）
│   ├── network/                # Retrofit 实例、拦截器
│   ├── database/               # Room 数据库实例
│   ├── datastore/              # DataStore
│   ├── di/                     # 核心 DI Module
│   ├── ui/theme/               # 全局主题
│   └── logging/                # Timber 初始化
├── domain/                      # 领域层
│   ├── model/                  # 领域模型
│   ├── usecase/                # UseCase（按功能子包）
│   └── repository/             # Repository 接口定义
├── data/                        # 数据层
│   ├── remote/                 # Retrofit API 接口 + DTO
│   ├── local/                  # Room Entity + DAO
│   └── repository/             # Repository 实现类
└── ui/                          # UI 层
    ├── navigation/             # NavHost、路由定义
    ├── home/                   # 首页
    ├── detail/                 # 详情页
    ├── profile/                # 个人中心
    ├── player/                 # 播放器
    ├── search/                 # 搜索
    ├── earn/                   # 赚金币
    └── components/             # 全局共享 Composable 组件
```

### 1.3 关键设计原则

- **单一职责**：每个 UseCase 只做一件事（如 `GetHomeFeedUseCase`、`ToggleFavoriteUseCase`），命名采用动名词短语。
- **Repository 模式**：UI/Domain 不关心数据来自网络还是缓存，由 Repository 内部决定获取策略（Cache-First / Network-First）。
- **不可变数据**：所有数据模型使用 `data class`，属性使用 `val`，UI State 通过 `StateFlow` 暴露。
- **命名一致性**：UseCase 命名为"动词 + 名词 + UseCase"，Repository 命名为"名词 + Repository"，API 接口命名为"名词 + ApiService"。

---

## 2. 导航架构

使用 Jetpack Compose Navigation 实现单 Activity 架构下的页面路由。

### 2.1 路由定义

所有路由统一在 `ui/navigation/Route.kt` 中定义。

**类型安全的路由（Kotlin Serialization）**：

```kotlin
@Serializable
sealed interface Route {
    @Serializable
    data object Home : Route                        // 首页 Tab

    @Serializable
    data object Earn : Route                        // 赚金币 Tab

    @Serializable
    data object Profile : Route                     // 我的 Tab

    @Serializable
    data class VideoDetail(val videoId: String) : Route  // 视频详情

    @Serializable
    data class Search(val initialQuery: String = "") : Route  // 搜索页
}
```

**NavHost 设置**：

```kotlin
@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Route.Home,
    ) {
        composable<Route.Home> {
            HomeScreen(onVideoClick = { videoId ->
                navController.navigate(Route.VideoDetail(videoId))
            })
        }
        composable<Route.VideoDetail> { backStackEntry ->
            val route: Route.VideoDetail = backStackEntry.toRoute()
            VideoDetailScreen(videoId = route.videoId)
        }
        // ...
    }
}
```

**导航参数限制**：
- 路由参数仅传递 ID（如 `videoId: String`），不传递大对象。
- 跨越 3 层及以上的参数传递使用共享 ViewModel 或 SavedStateHandle，避免路由参数链路过长。

### 2.2 Deep Link

使用 `djsdrama://` scheme 作为外部唤起入口。

**AndroidManifest.xml 配置**：

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="djsdrama" android:host="video" />
</intent-filter>
```

**Deeplink 路由映射**：

| Deep Link | 目标页面 | 参数 |
|-----------|---------|------|
| `djsdrama://home` | 首页 | 无 |
| `djsdrama://video/{videoId}` | 视频详情 | videoId |
| `dsjdrama://search?q={query}` | 搜索页 | query（可选） |
| `djsdrama://earn` | 赚金币 | 无 |

**NavHost 中注册 Deep Link**：

```kotlin
composable<Route.VideoDetail>(
    deepLinks = listOf(navDeepLink<Route.VideoDetail>(
        basePath = "djsdrama://video/{videoId}"
    ))
) { ... }
```

**Deep Link 处理流程**：
1. 检查用户登录状态（未登录先引导登录，登录后继续导航到目标页）
2. 解析参数，构建 Route 对象
3. 执行 `navController.navigate(route)`
4. 页面加载完成后上报 Deep Link 来源埋点

---

## 3. 状态管理

采用 UDF（单向数据流）模式，ViewModel 是唯一的状态持有者和修改者。

### 3.1 ViewModel

**基本模式**：

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeFeedUseCase: GetHomeFeedUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeFeedUi())
    val uiState: StateFlow<HomeFeedUi> = _uiState.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onRefresh() {
        viewModelScope.launch { ... }
    }
}
```

**SavedStateHandle**：用于在进程被杀死后恢复 UI 状态。

```kotlin
// 用法：存储滚动位置、当前 tab 等轻量状态
var currentTab by savedStateHandle.saveable { mutableStateOf(0) }
```

**注意事项**：
- 不要在 ViewModel 中持有 Context、View、Lifecycle 等 Android 组件引用（使用 `AndroidViewModel` 时也需格外谨慎）。
- ViewModel 构造函数只注入 UseCase / Repository 接口，不注入页面级参数（如 videoId），页面参数通过 SavedStateHandle 获取。
- ViewModel 的作用域默认为 NavBackStackEntry 的 Lifecycle。

### 3.2 StateFlow / SharedFlow

| 类型 | 适用场景 | 特征 |
|------|---------|------|
| `StateFlow` | UI 状态（持续性快照） | 始终有值，新订阅者立即获取最新值，去重 |
| `SharedFlow` | 一次性事件 / 广播 | 无初始值，支持 replay 和 buffer，不自动去重 |

**选择原则**：
- 页面渲染数据（loading、列表、错误）用 `StateFlow`。
- 一次性事件（Toast、Snackbar、导航跳转）用 `Channel + receiveAsFlow()` 转为 `SharedFlow`。

**Channel vs SharedFlow 对比如下**：

```kotlin
// 一次性事件：使用 Channel，保证每个事件只被消费一次
private val _events = Channel<HomeEvent>(Channel.BUFFERED)
val events = _events.receiveAsFlow()

// 广播事件（多订阅者共享）：使用 SharedFlow
private val _broadcast = MutableSharedFlow<BroadcastEvent>(replay = 1)
val broadcast: SharedFlow<BroadcastEvent> = _broadcast.asSharedFlow()
```

**禁止行为**：使用 `SharedFlow` + `replay = 1` 来模拟一次性事件——这会因配置变更导致事件重复触发。

### 3.3 UI State

使用单一不可变数据类：

```kotlin
data class HomeFeedUi(
    val feedType: FeedType = FeedType.RECOMMEND,
    val videos: List<VideoUi> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: ErrorUi? = null,
    val hasNextPage: Boolean = true,
    val currentPage: Int = 0,
)
```

**设计原则**：
- **单一 State 对象**：每个 Screen 对应一个 UI State 数据类，不拆分多个独立 StateFlow（便于 snapshot 和调试）。
- **error 用 sealed class**：不建议用 `String?`，因为需要区分错误类型指导 UI 展示（网络错误显示重试按钮 vs 空内容显示占位图）。
- **独立 loading 子状态**：分离 `isLoading`（首次加载）和 `isRefreshing`（下拉刷新），UI 据此展示不同的 loading 样式。
- `toXxx()` / `fromXxx()` 方法必须为纯函数（无副作用），方便单元测试。

---

## 4. 依赖注入

使用 Hilt 作为 DI 框架，禁止手动创建依赖实例。

### 4.1 Module 组织

Hilt Module 按功能领域拆分，而非按技术分层拆分：

```
core/di/
├── NetworkModule.kt          # Retrofit、OkHttp 实例提供
├── DatabaseModule.kt         # Room 数据库、DAO 提供
├── DataStoreModule.kt        # DataStore 实例提供
├── RepositoryModule.kt       # Repository 实现绑定
└── UseCaseModule.kt          # UseCase 自动注入（如无特殊绑定可省略）

feature/<feature>/di/
└── <Feature>Module.kt        # 功能模块特有的绑定（如播放器引擎）
```

**Module 设计规范**：
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInterceptor())  // 统一添加 Token
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
```

**Repository 绑定**：使用 `@Binds`（抽象绑定，性能更好）而非 `@Provides`：

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository
}
```

### 4.2 Scope

| Scope | 注解 | 用途 |
|-------|------|------|
| Application 级 | `@Singleton` | OkHttp、Retrofit、Room Database、DataStore |
| Activity 级 | `@ActivityScoped` | NavController（如有自定义） |
| ViewModel 级 | `@ViewModelScoped`（通过 Hilt 自动管理） | UseCase 通常不需要 Scope，由 ViewModel 决定生命周期 |
| Fragment 级 | `@FragmentScoped` | 不使用 Fragment 架构，不涉及 |

**原则**：
- 大部分依赖使用 `@Singleton`（网络客户端、数据库、Repository 均为全局单例）。
- 不要在 `@Singleton` 组件中注入 Activity / ViewModel 作用域的依赖（DI 编译时会报错）。
- 每个 @HiltViewModel 的 ViewModel 构造函数中的依赖自动由 Hilt 注入，不需要手动调用工厂方法。

---

## 5. 模块化策略

当项目膨胀到一定程度后进行模块拆分，当前阶段先定义拆分原则。

### 5.1 模块划分

| 模块 | 类型 | 职责 | 依赖方向 |
|------|------|------|---------|
| `:app` | Application | Activity 入口、Application 类、全局导航 | 依赖所有 feature 模块 |
| `:core:network` | Library | OkHttp+Retrofit 封装、拦截器、API 基类 | 无 |
| `:core:database` | Library | Room 数据库、Migration | 无 |
| `:core:ui` | Library | Design System 组件、Theme、公用 Composable | 无 |
| `:core:domain` | Library | 领域模型定义、Repository 接口 | 无 |
| `:core:common` | Library | 工具类、扩展函数、常量 | 无 |
| `:feature:home` | Feature | 首页功能（Tab、Feed 流） | core:domain, core:ui |
| `:feature:detail` | Feature | 视频详情页 | core:domain, core:ui |
| `:feature:player` | Feature | 竖屏视频播放器 | core:domain, core:ui |
| `:feature:profile` | Feature | 个人中心、设置 | core:domain, core:ui |
| `:feature:earn` | Feature | 赚金币功能 | core:domain, core:ui |
| `:feature:search` | Feature | 搜索功能 | core:domain, core:ui |

**拆分时机**：
- 当某个 feature 目录下的文件数超过 15 个时，考虑拆分为独立模块。
- 当某个功能需要被另一个 feature 复用时，必须提取到 `:core` 模块。

### 5.2 模块间通信

模块间依赖应保持单向、无环。

**允许的通信方式**：
- Feature 模块依赖 Core 模块（获取通用能力、领域模型、UI 组件）。
- Feature 模块之间通过 `:core:domain` 中的接口通信（依赖反转）。
- 页面跳转使用 Navigation + Deep Link，不通过直接依赖另一个 feature 模块的方式。

**禁止的通信方式**：
- Feature A 的 build.gradle.kts 中添加 Feature B 的依赖（`implementation project(':feature:home')`）。
- 在 Feature 模块中引用另一个 Feature 模块的 ViewModel / Composable 直接使用。
- 通过 EventBus（如 EventBus、LiveDataBus）跨模块发送事件——这会导致不可追踪的隐式依赖。

**跨模块导航**：通过 `navController.navigate("route_name")` 使用命名路由，不在编译期直接引用目标页面类。

---

## 6. 错误处理

### 6.1 错误分类

定义统一的错误密封类：

```kotlin
sealed class AppError : Throwable() {
    /** 网络连接失败、DNS 解析失败等 */
    data class Network(val cause: Throwable? = null) : AppError()

    /** 服务端返回非 2xx 状态码 */
    data class Server(val code: Int, val message: String) : AppError()

    /** 业务层错误（如积分不足、视频已下线） */
    data class Business(val code: Int, val message: String) : AppError()

    /** 其他未知错误 */
    data class Unknown(val cause: Throwable? = null) : AppError()
}
```

**Repository 层错误映射**：

```kotlin
suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Result<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(AppError.Server(response.code(), response.errorBody()?.string() ?: "Unknown error"))
        }
    } catch (e: UnknownHostException) {
        Result.failure(AppError.Network(e))
    } catch (e: SocketTimeoutException) {
        Result.failure(AppError.Network(e))
    } catch (e: Exception) {
        Result.failure(AppError.Unknown(e))
    }
}
```

**UI 层错误转换**：

```kotlin
fun AppError.toErrorUi(): ErrorUi = when (this) {
    is AppError.Network -> ErrorUi(
        title = "网络连接失败",
        message = "请检查网络设置后重试",
        action = ErrorAction.RETRY
    )
    is AppError.Server -> ErrorUi(
        title = "服务器错误",
        message = "服务繁忙，请稍后重试 (${code})",
        action = ErrorAction.RETRY
    )
    is AppError.Business -> ErrorUi(
        title = "提示",
        message = message,
        action = ErrorAction.DISMISS
    )
    is AppError.Unknown -> ErrorUi(
        title = "出错了",
        message = "未知错误，请稍后重试",
        action = ErrorAction.RETRY
    )
}
```

### 6.2 降级策略

**数据加载优先级**：本地缓存 > 网络请求 > 默认空状态。

| 场景 | 策略 | 实现方式 |
|------|------|---------|
| 首页 Feed 网络失败 | 显示上次缓存的 Feed 内容 + 顶部 Snackbar 提示"网络异常" | Room 缓存 + NetworkBoundResource 模式 |
| 视频播放网络中断 | 继续播放已缓冲内容，Snackbar 提示"正在使用移动网络" | ExoPlayer 内置缓冲 |
| 图片加载失败 | 显示灰色占位图 + 轻点重试 | Coil 的 `error()` + `placeholder()` |
| 搜索无网络 | 显示"网络异常，请检查网络"空状态，不自动重试 | UI 判断 AppError.Network |

**NetworkBoundResource 模式**：

```kotlin
inline fun <T> networkBoundResource(
    crossinline query: suspend () -> Flow<T?>,         // Room 查询
    crossinline fetch: suspend () -> T,                  // 网络请求
    crossinline saveFetchResult: suspend (T) -> Unit,    // 保存到 Room
    crossinline shouldFetch: suspend (T?) -> Boolean = { true },
) = flow {
    val cached = query().first()
    if (!shouldFetch(cached)) {
        emit(Resource.Loading(cached))
        return@flow
    }
    emit(Resource.Loading(cached))
    try {
        val fresh = fetch()
        saveFetchResult(fresh)
        emit(Resource.Success(fresh))
    } catch (e: Exception) {
        if (cached != null) {
            emit(Resource.Success(cached))  // 降级到缓存
        } else {
            emit(Resource.Error(e))
        }
    }
}
```

**离线提示**：当所有策略均失败时，展示全局 Snackbar（需持久展示直到用户手动关闭），并引导用户前往系统设置检查网络。
