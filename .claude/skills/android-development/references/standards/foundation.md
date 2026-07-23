# 基础库与基础能力 — Android

> 本文档定义 Android 端的基础库选型、集成方案与基础能力接入规范。

---

## 1. 网络层

使用 Retrofit + OkHttp 作为 HTTP 客户端，kotlinx.serialization 作为 JSON 序列化方案。

### 1.1 Retrofit + OkHttp

**OkHttp 客户端配置**：在 `core/di/NetworkModule.kt` 中提供全局单例。

```kotlin
@Provides
@Singleton
fun provideOkHttpClient(
    @NetworkTimeout timeoutConfig: Long,
    loggingInterceptor: HttpLoggingInterceptor,
    authInterceptor: AuthInterceptor,
    headerInterceptor: HeaderInterceptor,
): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(timeoutConfig, TimeUnit.SECONDS)
    .readTimeout(timeoutConfig, TimeUnit.SECONDS)
    .writeTimeout(timeoutConfig, TimeUnit.SECONDS)
    // 拦截器链顺序固定：Header → Auth → Logging（日志在最外层，记录完整请求）
    .addInterceptor(headerInterceptor)    // 通用 Header（app version, platform, Accept-Language）
    .addInterceptor(authInterceptor)      // Token 注入
    .addInterceptor(loggingInterceptor)   // 请求/响应日志
    .build()
```

**拦截器约定**：
- `HeaderInterceptor`：注入通用 Header（`App-Version`, `Platform: android`, `Accept-Language`）。
- `AuthInterceptor`：从 DataStore 中读取 Token 注入到 `Authorization` Header，Token 过期时触发全局登出事件（不在此拦截器内直接跳转页面）。
- `HttpLoggingInterceptor`：Debug 构建使用 `BODY` 级别，Release 构建使用 `NONE` 级别。

**Retrofit 实例**：

```kotlin
@Provides
@Singleton
fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.API_BASE_URL)  // 通过 BuildConfig 注入，禁止硬编码
    .client(client)
    .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
    .build()
```

**API 接口定义约定**：

```kotlin
// data/remote/HomeApiService.kt
interface HomeApiService {
    @GET("api/v1/home/feed")
    suspend fun getHomeFeed(
        @Query("page") page: Int = 0,
        @Query("page_size") pageSize: Int = 20,
    ): Response<ApiResponse<List<VideoDto>>>

    @POST("api/v1/video/{videoId}/favorite")
    suspend fun toggleFavorite(
        @Path("videoId") videoId: String,
    ): Response<ApiResponse<Unit>>
}
```

所有 API 方法使用 `suspend` 关键字，返回 `Response<T>` 以便在 Repository 层统一处理 HTTP 状态码。

### 1.2 序列化

使用 Kotlinx Serialization（Kotlin 原生、无需反射、编译期安全）：

```kotlin
// build.gradle.kts
plugins {
    kotlin("plugin.serialization") version libs.versions.kotlin
}
dependencies {
    implementation(libs.kotlinx.serialization.json)
}

// DTO 定义
@Serializable
data class VideoDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("cover_url") val coverUrl: String,
    @SerialName("duration") val duration: Int,   // 秒
    @SerialName("episode_count") val episodeCount: Int,
)
```

**序列化策略**：
- 后端字段名使用 snake_case，DTO 属性使用 camelCase，依靠 `@SerialName` 映射。
- 后端可能为 null 的字段，DTO 中使用默认值而非可空类型（`val count: Int = 0` 而非 `val count: Int?`），避免 null 泄漏到业务层。
- 不使用 `Json { ignoreUnknownKeys = true }` 的全局配置，改为在 `@Serializable` 类级别显式声明 `@SerialName`——漏映射的字段会编译报错，防止数据丢失。

### 1.3 API 响应模型

**统一响应包装**：

```kotlin
@Serializable
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?,
)

fun <T> ApiResponse<T>.toResult(): Result<T> {
    return if (code == 0 && data != null) {
        Result.success(data)
    } else {
        Result.failure(AppError.Business(code, message))
    }
}
```

**错误码区间映射（示例）**：

| HTTP Code | 含义 | 处理方式 |
|-----------|------|---------|
| 200 | 成功 | 调用 `toResult()` 检查业务 code |
| 401 | Token 过期或未登录 | 清除本地 Token → 跳转登录页 |
| 403 | 权限不足 | 展示 Toast "无权限操作" |
| 404 | 资源不存在 | 展示对应空状态 |
| 500 | 服务端内部错误 | AppError.Server |

**分页响应**：

```kotlin
@Serializable
data class PaginatedResponse<T>(
    val code: Int,
    val message: String,
    val data: PaginatedData<T>?,
)

@Serializable
data class PaginatedData<T>(
    @SerialName("items") val items: List<T>,
    @SerialName("next_page") val nextPage: Int?,
    @SerialName("has_more") val hasMore: Boolean,
)
```

---

## 2. 图片加载

使用 Coil 作为图片加载框架。

### 2.1 配置项

在 `core/di/ImageLoaderModule.kt` 中定义全局 ImageLoader 单例。

```kotlin
@Provides
@Singleton
fun provideImageLoader(app: Application): ImageLoader = ImageLoader.Builder(app)
    .diskCache {
        DiskCache.Builder()
            .directory(app.cacheDir.resolve("image_cache"))
            .maxSizeBytes(200 * 1024 * 1024)  // 200MB 磁盘缓存
            .build()
    }
    .memoryCache {
        MemoryCache.Builder(app)
            .maxSizePercent(0.25)  // 最多占用系统内存的 25%
            .build()
    }
    .crossfade(300)             // 300ms 淡入动画
    .respectCacheHeaders(false) // 忽略服务端 Cache-Control，始终使用本地缓存策略
    .build()
```

**占位图配置**：
- 封面图：`placeholder(R.drawable.img_placeholder_video)`
- 头像：`placeholder(R.drawable.img_placeholder_avatar)`
- 错误图：`error(R.drawable.img_error_load)`

### 2.2 Compose 集成

项目中统一使用 `AsyncImage` 组件，禁止直接使用 ImageView 或 Glide 等替代方案。

**基本用法**：

```kotlin
@Composable
fun VideoCover(
    coverUrl: String,
    modifier: Modifier = Modifier,
) {
    val imageLoader = LocalContext.current.imageLoader
    // 直接使用 AsyncImage
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(coverUrl)
            .crossfade(true)
            .placeholder(R.drawable.img_placeholder_video)
            .error(R.drawable.img_error_load)
            .build(),
        contentDescription = stringResource(R.string.content_desc_video_cover),
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}
```

**预加载**：对于 Feed 流场景，提前预加载下一屏图片：

```kotlin
LaunchedEffect(videos) {
    val upcomingUrls = videos.drop(visibleCount).take(5).map { it.coverUrl }
    upcomingUrls.forEach { url ->
        imageLoader.enqueue(ImageRequest.Builder(context).data(url).build())
    }
}
```

**图片尺寸适配**：
- 封面图请求时附加宽高参数（`?w=375&h=500` 或使用 CDN 裁剪能力），避免加载原图浪费带宽。
- Coil 默认使用 `ImageView` 尺寸作为解码尺寸，Compose 中应设置固定 `Modifier.size()` 约束解码内存占用。

---

## 3. 数据持久化

### 3.1 Room

**Entity 定义**：

```kotlin
@Entity(tableName = "video_cache")
data class VideoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "cover_url") val coverUrl: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "json_data") val jsonData: String,  // JSON 降级备份
)
```

**DAO 定义**：

```kotlin
@Dao
interface VideoDao {
    @Query("SELECT * FROM video_cache ORDER BY cached_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getCachedVideos(limit: Int = 20, offset: Int = 0): List<VideoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Query("DELETE FROM video_cache WHERE cached_at < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM video_cache")
    suspend fun clearAll()
}
```

**Database 定义**：

```kotlin
@Database(
    entities = [VideoEntity::class, FavoriteEntity::class, SearchHistoryEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ShortDramaDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun searchHistoryDao(): SearchHistoryDao
}
```

**Migration 规范**：
- Schema 文件导出到 `app/schemas/` 目录（`exportSchema = true`）。
- 每个 Migration 单独写一个类，不要使用 `fallbackToDestructiveMigration()`。
- Migration 用 `@VisibleForTesting` 暴露并在单元测试中验证。

```kotlin
// RoomMigration_1_2.kt
class Migration1To2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE video_cache ADD COLUMN view_count INTEGER NOT NULL DEFAULT 0")
    }
}
```

**类型转换器**：

```kotlin
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = Json.decodeFromString(value)
}
```

### 3.2 DataStore

使用 Preferences DataStore 存储简单 KV 配置项（Token、用户设置等），不用于大型列表数据。

**配置**：

```kotlin
// core/datastore/UserPreferences.kt
class UserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val authToken: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_AUTH_TOKEN]
    }

    suspend fun setAuthToken(token: String?) {
        dataStore.edit { prefs ->
            if (token != null) prefs[KEY_AUTH_TOKEN] = token
            else prefs.remove(KEY_AUTH_TOKEN)
        }
    }

    companion object {
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
```

**DataStore vs Room 选择**：

| 数据类型 | 方案 | 说明 |
|---------|------|------|
| Token、用户设置、开关标记 | Preferences DataStore | 单键读取、稀疏写入 |
| Feed 缓存、收藏列表、搜索历史 | Room | 结构化查询、分页、条件过滤 |
| 视频播放历史 | Room | 需要按时间范围查询和排序 |

**禁止行为**：
- 禁止使用 `SharedPreferences`（线程不安全、不支持 Flow）。
- 禁止在主线程读写 DataStore（DataStore 已内部使用 `Dispatchers.IO`，但仍需在协程中调用）。
- 禁止在 DataStore 中存储 JSON 序列化的大列表（应改用 Room）。

---

## 4. 日志系统

使用 Timber 作为日志框架。

### 4.1 日志级别

| 级别 | 使用场景 | 示例 |
|------|---------|------|
| `Timber.v()` | 高频调试信息（网络请求 body、状态变化追踪） | 仅在本地调试时编译，Release 中剔除 |
| `Timber.d()` | 开发期调试（API 调用参数、UI 状态切换） | 可在 Debug 构建中保留 |
| `Timber.i()` | 应用生命周期事件（页面进入/退出、用户关键操作） | Debug 和 Release 均保留 |
| `Timber.w()` | 可恢复的异常（网络超时重试、数据格式降级） | 记录异常但不影响用户 |
| `Timber.e()` | 不可恢复的错误（崩溃边界、数据损坏） | 必须上报到 Crashlytics |

**Log TAG 约定**：
- 使用类名简写作为 TAG，通过 Timber.tag() 设置：`Timber.tag("HomeVM").d("Loading feed page=$page")`
- TAG 格式：`<模块缩写><类型>`，如 `HomeVM`（Home ViewModel）、`NetAuth`（Network AuthInterceptor）。

### 4.2 Release 日志

**Timber Tree 配置**：

```kotlin
// Application.onCreate()
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
} else {
    // Release: 仅ERROR和WARN级别写入日志文件，其余丢弃
    Timber.plant(ReleaseTree())
}

class ReleaseTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority >= Log.WARN) {
            // 写入本地文件（最多保留 7 天，单文件 5MB 上限）
            // 不要在此处调用 Timber 自身，防止递归
            FileLogger.log(priority, tag, message, t)
        }
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.WARN
    }
}
```

**Release 构建规则**：
- `Timber.v()` / `Timber.d()` 在 Release 中被 ProGuard 移除（通过 `-assumenosideeffects` 规则）。
- `Timber.i()` 在 Release 中保留但不写文件（仅输出到 logcat，供现场排查）。
- 日志文件仅保留最近 7 天，`onTrimMemory(TRIM_MEMORY_MODERATE)` 时清理过期日志。
- 禁止在日志中输出 Token、手机号、密码等敏感信息——所有日志写入前必须脱敏。

**ProGuard 移除 Debug 日志**：

```proguard
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
}
```

---

## 5. 性能监控

### 5.1 启动耗时

**启动阶段定义**：

| 阶段 | 起点 | 终点 | 参考指标 |
|------|------|------|---------|
| 冷启动 | 进程创建 | 首个 Activity 的 `onResume` 结束 | < 1.5 秒 |
| 温启动 | 进程存活，Activity 重新创建 | onResume 结束 | < 500ms |
| 首帧可见 | 进程创建 | 首个 Composable 渲染完成 | < 1.0 秒 |
| 可交互 | 进程创建 | Feed 首屏数据加载完成 | < 2.0 秒 |

**埋点方案**：

```kotlin
// Application.onCreate()
override fun onCreate() {
    val startTime = SystemClock.uptimeMillis()
    super.onCreate()
    // 初始化后记录
    Timber.tag("Perf").i("App onCreate took ${SystemClock.uptimeMillis() - startTime}ms")
}
```

**首帧追踪**：
```kotlin
// MainActivity
@Composable
fun AppContent() {
    var isFirstFrame by remember { mutableStateOf(true) }
    DisposableEffect(Unit) {
        onDispose { /* 仅用于取消 */ }
    }
    LaunchedEffect(Unit) {
        // 首帧渲染后
        if (isFirstFrame) {
            Timber.tag("Perf").i("First frame rendered at ${SystemClock.uptimeMillis()}")
            isFirstFrame = false
        }
    }
    // ... UI content
}
```

**优化措施**：
- `Application.onCreate()` 中仅初始化核心组件（Hilt、Timber、Crashlytics），其他初始化延迟到首屏渲染后（`LaunchedEffect` 中执行）。
- ContentProvider 初始化应避免，优先使用 App Startup 库的 `Initializer`，配置手动延迟初始化。
- 禁用 AndroidX 的 `ReportFragment` 自动注入（`androidx.lifecycle.process:2.x.x` 中会导致多余初始化）。

### 5.2 卡顿监控

**主线程 Looper 监控**：

使用 Android 内置的 StrictMode（Debug 构建）检测主线程违规：

```kotlin
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .penaltyLog()
            .penaltyFlashScreen()
            .build()
    )
    StrictMode.setVmPolicy(
        StrictMode.VmPolicy.Builder()
            .detectLeakedSqlLiteObjects()
            .detectLeakedClosableObjects()
            .detectActivityLeaks()
            .detectLeakedRegistrationObjects()
            .penaltyLog()
            .build()
    )
}
```

**ANR 监控**：
- 集成 Firebase Crashlytics + Firebase Performance 自动收集 ANR。
- 关键线程（如播放器解码线程）使用 Watchdog 模式：子线程定期向主线程发送心跳，若连续 3 个周期（每个周期 2s）未收到响应，上报日志。
- Release 构建中 ANR 信息通过 Firebase Crashlytics 自动上报，不需要额外代码。

**性能反弹规则**：
- 任一页面从导航开始到首屏数据展示超过 2 秒（Debug）/ 3 秒（Release），视为性能反弹，需要排查原因。
- 下拉刷新耗时超过 1.5 秒需优化。

### 5.3 内存泄漏

**LeakCanary 集成**：Debug 构建中自动安装，不侵入业务代码。

```kotlin
// build.gradle.kts
dependencies {
    debugImplementation(libs.leakcanary)
}
```

**常见泄漏模式与预防**：

| 泄漏场景 | 原因 | 预防措施 |
|---------|------|---------|
| 静态变量持有 Activity/Context | 静态引用 GC Root 可达 | 禁止静态变量持有 Activity/View；Context 使用 `applicationContext` |
| 匿名内部类持有外部类引用 | 内部类默认持有外部类强引用 | 使用静态内部类 + WeakReference |
| 未取消的协程持有 ViewModel | 协程中访问 ViewModel 属性的引用链 | 使用 `viewModelScope` / `lifecycleScope` |
| 单例 Repository 持有旧的回调 | 注册监听后未反注册 | 使用 Flow 代替回调；在 `onCleared()` 中取消收集 |
| Dialog / PopupWindow 未 dismiss | WindowManager 持有 View 引用 | Activity/Fragment 销毁时必须 `dismiss()` |

**排查流程**：
1. 执行关键路径（首页 → 详情 → 播放 → 返回）3-5 次
2. 等待 10 秒让 GC 生效
3. 查看 LeakCanary 通知
4. 若发现泄漏，在 LeakCanary UI 中查看引用链，定位到持有的根对象

---

## 6. 崩溃收集

使用 Firebase Crashlytics 作为崩溃收集方案。

### 6.1 符号化

**ProGuard Mapping 自动上传**：

在 `app/build.gradle.kts` 中配置：

```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            firebaseCrashlytics {
                mappingFileUploadEnabled = true  // 自动上传 mapping.txt
            }
        }
    }
}
```

**验证符号化**：
1. 打 Release 包后，检查 `app/build/outputs/mapping/release/mapping.txt` 是否存在。
2. Firebase Console → Crashlytics → 选择崩溃 → 确认堆栈已展开为源代码行号。
3. 若堆栈仍为混淆名称，检查 Firebase 插件版本和 google-services.json 是否匹配。

**NDK 符号化**（如有 C/C++ 代码）：
- Release 构建保留 `.so` 文件的 `debug_symbols` 目录。
- 在 `firebaseCrashlytics` 块中配置 `nativeSymbolUploadEnabled = true`。

### 6.2 自定义日志

**崩溃前日志上下文**：在崩溃发生前手动记录关键上下文，帮助定位问题。

```kotlin
// 记录当前页面和用户操作
Firebase.crashlytics.log("User navigated to VideoDetail: videoId=$videoId")

// 设置自定义 Key（可在 Firebase 面板中过滤）
Firebase.crashlytics.setCustomKey("last_screen", "VideoDetail")
Firebase.crashlytics.setCustomKey("video_count", videos.size)
Firebase.crashlytics.setCustomKey("is_logged_in", isLoggedIn.toString())
```

**关键埋点位置**：
- 页面切换时记录当前 Route。
- 网络请求失败时记录 error 详情（不记录 Token）。
- 播放器状态变更（开始播放、切换集数、出错）。

**用户标识**：
```kotlin
// 登录/登出时更新，方便按用户追踪崩溃
Firebase.crashlytics.setUserId(userId)
```

**捕获的异常**：使用 `Firebase.crashlytics.recordException(e)` 记录非崩溃异常（如 API 调用失败、数据解析异常），这些不会导致 App 崩溃但在 Firebase 面板中可见：

```kotlin
try {
    // ...
} catch (e: Exception) {
    Firebase.crashlytics.recordException(e)
    // 继续执行降级逻辑
}
```

---

## 7. 国际化 (i18n)

### 7.1 资源组织

支持语言：中文（默认）、英文（en）。

```
res/
├── values/           # 默认 = 中文
│   └── strings.xml
├── values-en/        # 英文
│   └── strings.xml
└── values-en-rUS/    # （可选）美国英语特定覆盖
    └── strings.xml
```

**默认语言**：Android 系统匹配不到用户语言时，fallback 到 `values/`（中文）。不创建 `values-zh/`，避免维护两份中文翻译。

### 7.2 切换语言

支持应用内语言切换（不跟随系统）。

**核心实现**：

```kotlin
object LanguageManager {
    private const val KEY_LANGUAGE = "app_language"
    private const val LANGUAGE_AUTO = "auto"

    fun applyLanguage(context: Context, languageTag: String?) {
        val config = Configuration(context.resources.configuration)
        val locale = when (languageTag) {
            "en" -> Locale.ENGLISH
            "zh" -> Locale.SIMPLIFIED_CHINESE
            else -> Locale.getDefault()  // 跟随系统
        }
        config.setLocale(locale)
        val updated = context.createConfigurationContext(config)
        // 在 BaseActivity 中 attachBaseContext 时注入
    }

    fun persistLanguage(dataStore: UserPreferences, tag: String) {
        // 保存到 DataStore，下次启动时加载
    }
}
```

**Activity 注入**：
```kotlin
abstract class BaseActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val langTag = // 从 DataStore 读取（同步读取首次缓存值）
        val wrapped = LanguageManager.applyLanguage(newBase, langTag)
        super.attachBaseContext(wrapped)
    }
}
```

**注意事项**：
- 切换语言后必须重建 Activity（`activity.recreate()`），确保所有资源重新加载。
- Jetpack Compose 中使用 `stringResource()` 读取字符串，只需 Activity 重建即可生效，不需要额外处理。
- 日期、数字格式化使用 `java.text.NumberFormat` 和 `java.time.format.DateTimeFormatter`，传入对应 Locale 参数。

---

## 8. 无障碍 (A11y)

根据 W3C WCAG 2.1 AA 标准适配基础无障碍能力。

### 8.1 contentDescription

每个可交互或含语义的 Composable 必须提供 `contentDescription`。

**语义标签规范**：

```kotlin
// 正确：有意义的描述
Icon(
    painter = painterResource(R.drawable.ic_play_24),
    contentDescription = stringResource(R.string.content_desc_play_video),
)
// 错误：空字符串（对 TalkBack 用户不可见）
Icon(painter = painterResource(R.drawable.ic_play_24), contentDescription = null)

// 纯装饰性元素：使用 null（TalkBack 跳过）
Divider()  // 无 contentDescription 参数
Image(
    painter = painterResource(R.drawable.bg_gradient_top),
    contentDescription = null,  // 装饰性背景
)
```

**字符串资源示例**：
```xml
<!-- values/strings.xml -->
<string name="content_desc_play_video">播放视频</string>
<string name="content_desc_video_cover">视频封面</string>
<string name="content_desc_close">关闭</string>
<string name="content_desc_search">搜索</string>
<string name="content_desc_tab_home">首页</string>
<string name="content_desc_tab_earn">赚金币</string>
<string name="content_desc_tab_profile">我的</string>
```

**合并语义**：对于组合组件（如视频卡片 = 封面 + 标题 + 播放数），使用 `Modifier.semantics(mergeDescendants = true) {}` 将子组件语义合并为一个语义节点：

```kotlin
@Composable
fun VideoCard(video: VideoUi, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "${video.title}，共${video.episodeCount}集"
        }
    ) {
        // 内部 Icon/Text 不再需要单独的 contentDescription
    }
}
```

### 8.2 焦点顺序

Compose 默认按代码布局顺序处理焦点遍历，无需手动指定焦点顺序。

**需要特殊处理的情况**：
- **LazyColumn/LazyRow**：TalkBack 用户滚动时，Compose 自动处理焦点回收和复用，不需要额外代码。
- **对话框/底部弹窗**：使用 `Modifier.semantics { isTraversalGroup = true }` 将焦点限制在弹窗内部：
  ```kotlin
  AlertDialog(
      modifier = Modifier.semantics { isTraversalGroup = true },
      // ...
  )
  ```
- **ExoPlayer 控制栏**：播放/暂停/下一集控件需要 `contentDescription` + 可聚焦声明。
- **Tab 切换**：BottomNavigation 自动支持无障碍焦点，但需要额外的 `contentDescription` 和选中状态播报（"已选中，首页"）。

**滚动操作**：确保 LazyList 支持 TalkBack 的滚动手势，若列表高度不符合系统预期，设置 `Modifier.semantics { scrollable = true }`。

**测试方法**：在设备设置中开启 TalkBack（设置 → 辅助功能 → TalkBack），AI agent 可通过 ADB 命令启用：
```bash
adb shell settings put secure enabled_accessibility_services com.android.talkback/com.google.android.marvin.talkback.TalkBackService
```
