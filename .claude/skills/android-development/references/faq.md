# 常见问题 — Android

> 本文档收集 Android 开发中的常见问题与解决方案。

---

## 构建问题

### Gradle 同步失败：Unresolved reference

**错误现象**：

```
Unresolved reference: coil
Unresolved reference: hilt
```

或在 `build.gradle.kts` 中导入依赖后 IDE 红线提示找不到类。

**原因**：

1. `gradle/libs.versions.toml` 中未定义对应的库别名。
2. Version Catalog 的 TOML 语法错误（如 `[versions]` 表名拼写错误）。
3. 依赖未在模块的 `build.gradle.kts` 的 `dependencies {}` 块中添加。
4. Gradle 缓存过期。

**解决方案**：

1. 确认 `gradle/libs.versions.toml` 中存在对应库定义：
   ```toml
   [libraries]
   coil-compose = { module = "io.coil-kt.coil3:coil-compose", version = "3.0.4" }
   ```
2. 在模块 `build.gradle.kts` 中添加：
   ```kotlin
   implementation(libs.coil.compose)
   ```
3. 刷新 Gradle：`./gradlew --refresh-dependencies`
4. 清除 IDE 缓存：Android Studio → File → Invalidate Caches and Restart

---

### 编译错误：java.lang.OutOfMemoryError: Java heap space

**错误现象**：

```
Execution failed for task ':app:compileDebugKotlin'.
> java.lang.OutOfMemoryError: Java heap space
```

**原因**：Gradle Daemon 分配的堆内存不足，常见于大型 Kotlin 项目。

**解决方案**：

在 `gradle.properties` 中增加内存分配：
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
org.gradle.parallel=true
org.gradle.caching=true
kotlin.daemon.jvmargs=-Xmx4096m
```

如果仍未解决，停止所有 Gradle Daemon 后重试：
```bash
./gradlew --stop && ./gradlew assembleDebug
```

---

### Room Schema 导出错误：Schema export directory is not provided

**错误现象**：

```
Room cannot pick an export directory since no annotation processor options
were specified. Either annotate the database with @Database(exportSchema = false)
or specify the export directory.
```

**原因**：Room 要求在构建时导出 Schema 到指定目录，但尚未配置。

**解决方案**：

**方案 A（推荐）**：在 `app/build.gradle.kts` 中配置 Schema 导出路径：
```kotlin
android {
    defaultConfig {
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
}
```

**方案 B**：仅在不需要 Migration 测试时禁用：
```kotlin
@Database(entities = [...], version = 1, exportSchema = false)
```

> 注意：方案 B 会导致无法编写 Migration 单元测试，不推荐。

---

### Hilt 编译错误：Cannot find symbol Dagger*Component

**错误现象**：

```
error: cannot find symbol
import com.djs66256.short_drama.DaggerShortDramaApplication_HiltComponents_SingletonC;
```

**原因**：

1. Hilt 的 KSP/KAPT 注解处理器未正确配置。
2. 未在 Application 类上添加 `@HiltAndroidApp` 注解。
3. Gradle 缓存问题导致 Dagger 生成的代码不可见。

**解决方案**：

1. 确认 `app/build.gradle.kts` 中存在：
   ```kotlin
   plugins {
       id("com.google.dagger.hilt.android")
   }
   dependencies {
       implementation(libs.hilt.android)
       ksp(libs.hilt.compiler)  // 或 kapt(libs.hilt.compiler)
   }
   ```
2. 确认 Application 类有 `@HiltAndroidApp`：
   ```kotlin
   @HiltAndroidApp
   class ShortDramaApplication : Application()
   ```
3. 清理并重建：`./gradlew clean assembleDebug`

---

### ktlint 格式化冲突

**错误现象**：

`ktlintFormat` 与 Android Studio 自带的格式化结果不一致，每次格式化后 ktlint 仍然报错。

**原因**：IDE 的 Kotlin 格式化设置与 ktlint 规则冲突（常见于 import 顺序、缩进宽度）。

**解决方案**：

1. 安装 ktlint-idea 插件（Settings → Plugins → 搜索 "ktlint"）
2. 在 `.editorconfig` 中统一配置：
   ```ini
   [*.kt]
   ij_kotlin_imports_layout = *,java.**,javax.**,kotlin.**,^
   ij_kotlin_allow_trailing_comma = true
   ```
3. 提交前使用 `./gradlew ktlintFormat` 覆盖格式化，再将 IDE 的 commit 操作设为 "Reformat code" 关闭。

---

## 运行时问题

### 应用闪退：java.lang.RuntimeException: Method getMainLooper in android.os.Looper not mocked

**错误现象**：

在单元测试中创建 ViewModel 或调用 `Dispatchers.Main` 时抛出此异常：
```
java.lang.RuntimeException: Method getMainLooper in android.os.Looper not mocked
```

**原因**：单元测试在 JVM 上运行，`android.os.Looper.getMainLooper()` 是 Android Framework 方法，在 JVM 中不存在。

**解决方案**：

使用 JUnit 5 扩展设置 Main Dispatcher：

```kotlin
// 方案 A：使用测试扩展
@ExtendWith(MainDispatcherExtension::class)
class HomeViewModelTest {
    // Dispatchers.Main 会被替换为 TestDispatcher
}

// 方案 B：手动设置（JUnit 4 风格）
@BeforeEach
fun setup() {
    Dispatchers.setMain(StandardTestDispatcher())
}
@AfterEach
fun tearDown() {
    Dispatchers.resetMain()
}
```

在 `build.gradle.kts` 中引入 `turbo` 扩展库：
```kotlin
testImplementation("app.cash.turbine:turbine:1.1.0")
```

---

### 应用闪退：java.lang.IllegalStateException: ViewModelStore should be set

**错误现象**：

```
java.lang.IllegalStateException: ViewModelStore should be set before calling get()
```

**原因**：使用 `HiltViewModel` 注入 ViewModel 时，NavBackStackEntry 尚未创建（如在非 Navigation 上下文中直接调用 `hiltViewModel()`）。

**解决方案**：

确认 `hiltViewModel()` 仅在 NavHost 的 `composable() {}` 内部调用：

```kotlin
// 错误：在 Activity 的 setContent 中直接调用
setContent {
    val vm: HomeViewModel = hiltViewModel()  // 此时无 NavBackStackEntry
}

// 正确：在 NavHost 的 composable 中调用
NavHost(navController, startDestination = Route.Home) {
    composable<Route.Home> {
        val vm: HomeViewModel = hiltViewModel()  // NavBackStackEntry 已存在
        HomeScreen(vm)
    }
}
```

---

### Retrofit 请求报错：Expected begin array but was begin object

**错误现象**：

```
com.squareup.moshi.JsonDataException: Expected BEGIN_ARRAY but was BEGIN_OBJECT
```

或 Kotlinx Serialization 同样抛出 JSON 格式不匹配错误。

**原因**：服务端返回的 JSON 结构与 DTO 定义的类型不匹配。例如 DTO 定义 `data: List<Video>` 但后端返回 `data: { items: [...] }`。

**解决方案**：

1. 与后端确认接口文档，确保 DTO 字段类型一致。
2. 使用统一响应包装 `ApiResponse<T>` 作为 Retrofit 接口的返回类型外包装。
3. 排查 Minify 是否误混淆了 `@Serializable` 数据类（确认 ProGuard 保留规则）。
4. 使用 `HttpLoggingInterceptor(BODY)` 在 Logcat 中打印实际返回的 JSON。

---

### Coil 图片不显示，但 URL 在浏览器中可访问

**错误现象**：Compose 界面上某些图片显示为空白或占位图，但复制 URL 到浏览器可以查看。

**原因**：

1. Coil 的全局 ImageLoader 配置中 `respectCacheHeaders(false)` 未设置，服务端缓存策略导致不加载。
2. 图片 URL 使用了自签名证书的 HTTPS（Coil 内 OkHttp 不信任）。
3. Android 9+ 默认禁止明文 HTTP 图片加载。

**解决方案**：

1. 设置 `respectCacheHeaders(false)`：
   ```kotlin
   ImageLoader.Builder(context)
       .respectCacheHeaders(false)
       .build()
   ```
2. 在 `network_security_config.xml` 中为 CDN 域名添加信任配置（或切换到 HTTPS）。
3. 确认 Coil 使用的 OkHttp 与项目网络层共享同一 `OkHttpClient` 实例：
   ```kotlin
   @Singleton
   @Provides
   fun provideImageLoader(app: Application, okHttpClient: OkHttpClient): ImageLoader {
       return ImageLoader.Builder(app)
           .okHttpClient { okHttpClient }
           .build()
   }
   ```

---

### Room 数据库升级后数据丢失

**错误现象**：更新 `@Database(version = 2)` 后，之前存储的数据全部丢失。

**原因**：使用了 `fallbackToDestructiveMigration()`，Room 在无法找到 Migration 时会直接删除并重建数据库。

**解决方案**：

1. 移除 `fallbackToDestructiveMigration()`。
2. 为版本 1→2 编写 Migration：
   ```kotlin
   class Migration1To2 : Migration(1, 2) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("ALTER TABLE video_cache ADD COLUMN view_count INTEGER NOT NULL DEFAULT 0")
       }
   }
   ```
3. 注册 Migration：
   ```kotlin
   Room.databaseBuilder(context, ShortDramaDatabase::class.java, "shortdrama.db")
       .addMigrations(Migration1To2)
       .build()
   ```
4. 编写 Migration 单元测试确保数据保留。

---

## Compose 问题

### LazyColumn 滚动卡顿

**错误现象**：Feed 流快速滑动时出现明显掉帧，帧率低于 45fps。

**原因**：

1. 列表项中使用了未 `@Stable` 的数据类，导致每次重组都重新创建。
2. `key` 参数未设置或设置不当（使用 index 作为 key）。
3. 列表项中执行了耗时操作（如复杂运算、网络图片尺寸计算）。
4. 嵌套滚动（如 `LazyColumn` 内嵌 `LazyRow`）未正确配置 `nestedScroll`。

**解决方案**：

1. 为数据类添加 `@Stable` 注解：
   ```kotlin
   @Stable
   data class VideoUi(val id: String, val title: String, ...)
   ```
2. 使用业务唯一 ID 作为 `key`：
   ```kotlin
   items(videos, key = { it.id }) { video -> VideoCard(video) }
   ```
3. 将耗时计算移到 `ViewModel` 中，Composable 中仅做展示。
4. 嵌套滚动时设置固定高度，避免子列表无限扩展：
   ```kotlin
   LazyRow(modifier = Modifier.height(200.dp)) { ... }
   ```

---

### ModalBottomSheet 弹出后背景内容可滚动

**错误现象**：底部弹出 BottomSheet 后，背后的列表仍然可以滚动。

**原因**：`ModalBottomSheet` 没有阻止背景的触摸事件传播。

**解决方案**：

在 `ModalBottomSheet` 展开时禁用背景滚动：

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeWithSheet() {
    val sheetState = rememberModalBottomSheetState()
    val isExpanded = sheetState.currentValue == SheetValue.Expanded

    Scaffold(
        modifier = Modifier.then(
            if (isExpanded) Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* 消费点击事件，阻止穿透 */ }
            else Modifier
        )
    ) { ... }
}
```

或使用 `BottomSheetScaffold`（Material 3）天然处理此问题。

---

### Compose StateFlow 收集导致无限重组循环

**错误现象**：Composable 函数被无限重组，CPU 100%，可能导致 ANR。

**原因**：在 Composable 中直接修改 `MutableStateFlow` 的值，修改操作触发了新的重组，形成循环：

```kotlin
// 错误：读取 state → 触发重组 → 修改 state → 再次触发重组
@Composable
fun BadExample() {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // 在 Composable 中直接修改状态
    viewModel.modifyState(state.someValue + 1)  // 导致无限重组
}
```

**解决方案**：

状态修改必须由事件驱动，不能放在 Composable 函数体中无条件执行：

```kotlin
// 正确：仅在用户交互时修改
Button(onClick = { viewModel.increment() }) { Text("+1") }
```

如果确实需要在 Compose 中根据 state 副作用修改 state，使用 `LaunchedEffect(someKey)`：
```kotlin
LaunchedEffect(state.scrollToTop) {
    if (state.scrollToTop) {
        listState.animateScrollToItem(0)
        viewModel.onScrolledToTop()  // 重置标记
    }
}
```

---

### TextField 中文输入时拼音显示不全

**错误现象**：使用 Compose `TextField` 输入中文时，拼音候选词显示不全或被裁剪。

**原因**：Compose 的 `TextField` 与输入法（IME）的组合区域通信存在问题，部分 IME 未正确处理 `composingRegion`。

**解决方案**：

1. 升级 Compose BOM 到最新版本（Google 持续修复 IME 兼容性）。
2. 避免在 `TextField` 上使用 `Modifier.height(IntrinsicSize.Min)` 等依赖内容测量的 Modifier。
3. 确保 `TextField` 有足够高度（至少 56dp）。
4. 在 `values` 中显式设置 `BasicTextField` 而非使用 `TextField`（如果问题持续）。
5. 检查 IME 设置，部分手机自带输入法兼容性差，测试时使用 Gboard 作为基准。

---

### @Preview 无法渲染：The following ViewModel could not be created

**错误现象**：`@Preview` 无法渲染，报 ViewModel 相关错误。

**原因**：Preview 中尝试使用 `hiltViewModel()`，但 Hilt 在 Preview 环境中不可用。

**解决方案**：

Preview 函数中不依赖 DI，使用 mock 数据：

```kotlin
@Preview(showBackground = true)
@Composable
private fun VideoCardPreview() {
    ShortDramaTheme {
        VideoCard(
            video = VideoUi(
                id = "preview_001",
                title = "霸道总裁爱上我",
                episodeCount = 80,
            ),
            onItemClick = {},
        )
    }
}
```

对于 Screen 级别的 Preview，可以创建一个不含 ViewModel 的 wrapper Composable，将所有状态作为参数传入。

---

## 性能问题

### 冷启动白屏时间长

**错误现象**：点击应用图标后出现 2-3 秒白屏/黑屏，然后才显示 UI 内容。

**原因**：

1. `Application.onCreate()` 中执行了大量同步初始化操作（如初始化 SDK、数据库、网络等）。
2. `MainActivity` 默认的 `windowBackground` 与应用主题色不一致导致闪白。
3. 首个 Composable 渲染前需要等待数据加载。

**解决方案**：

**方案 A**：延迟初始化非关键组件：
```kotlin
class ShortDramaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 核心初始化（必须同步）
        Hilt.init(this)
        Timber.plant(Timber.DebugTree())
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        // 非核心初始化（延迟到首屏渲染后）
        CoroutineScope(Dispatchers.Default).launch {
            // 预加载、性能监控初始化等
        }
    }
}
```

**方案 B**：使用 SplashScreen API（Android 12+）：

在 `app/build.gradle.kts` 中引入：
```kotlin
implementation("androidx.core:core-splashscreen:1.0.1")
```

```kotlin
// MainActivity
override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    setContent {
        ShortDramaTheme {
            // 首屏内容
        }
    }
}
```

**方案 C**：设置透明窗口背景（治标）：
```xml
<!-- themes.xml -->
<style name="Theme.ShortDrama">
    <item name="android:windowBackground">@android:color/transparent</item>
    <item name="android:windowIsTranslucent">true</item>
</style>
```

---

### 视频播放首帧时间超过 2 秒

**错误现象**：点击视频卡片后，等待超过 2 秒才开始播放，用户感知明显卡顿。

**原因**：

1. 未启动预加载：页面跳转后才开始请求播放地址。
2. ExoPlayer 初始化解码器耗时。
3. CDN 首包延迟高（DNS 解析 + TCP 握手 + TLS 握手）。

**解决方案**：

1. 在 Feed 列表中预加载即将播放的视频：
   ```kotlin
   // HomeViewModel 中
   fun preloadVideo(videoId: String) {
       viewModelScope.launch {
           repository.getVideoPlayUrl(videoId)
       }
   }
   // HomeScreen 中，当某个视频卡片可见度 > 50% 时触发预加载
   ```
2. ExoPlayer 使用 `LoadControl` 控制预缓冲量：
   ```kotlin
   val loadControl = DefaultLoadControl.Builder()
       .setBufferDurationsMs(
           500,      // Min buffer（更快起播）
           3000,     // Max buffer
           1000,     // Buffer for playback
           1500,     // Buffer for playback after rebuffer
       )
       .build()
   val player = ExoPlayer.Builder(context)
       .setLoadControl(loadControl)
       .build()
   ```
3. 使用 HTTP/3（QUIC）协议连接 CDN，减少 TLS 握手延迟。
4. DNS 预热：在应用启动时预解析 CDN 域名。

---

### 列表快速滚动时图片频繁重新加载

**错误现象**：Feed 流快速滑动，已经加载过的图片向上滚动出屏幕再滑回来时重新加载。

**原因**：

1. Coil 内存缓存大小不足，图片被 LRU 策略驱逐。
2. `key` 参数使用 index，导致 Composable 被标记为"完全新项"而非"复用项"。
3. 滚动出屏幕时 Composable 被销毁（而非回收），导致图片缓存关联丢失。

**解决方案**：

1. 增大 Coil 内存缓存：
   ```kotlin
   MemoryCache.Builder(context)
       .maxSizePercent(0.25)  // 从 15% 提升到 25%
       .build()
   ```
2. 使用唯一 ID 作为 LazyColumn 的 key：
   ```kotlin
   items(videos, key = { it.id }) { video -> ... }
   ```
3. 使用 `LazyColumn` 的 `contentType` 参数为不同类型的列表项提供复用提示：
   ```kotlin
   items(videos, key = { it.id }, contentType = { "video_card" }) { ... }
   ```
4. 在 ViewModel 中缓存已加载的图片 URL（避免 Flowing 销毁后丢失引用）。

---

### 应用长时间运行后内存占用持续增加

**错误现象**：应用在前台运行 30 分钟后，内存占用从 150MB 增长到 500MB+，最终触发 OOM。

**原因**（常见的内存泄漏积累）：

1. 图片缓存未限制大小或未及时清理。
2. ViewModel 中持有 Activity/View 引用（如 `AndroidViewModel(application)` 内部持有 Context 导致 Activity 重建后旧 Context 无法释放）。
3. 播放器实例未释放（每次进入详情创建新的 ExoPlayer，返回时不 release）。
4. Flow 收集未取消（`viewModelScope.launch { stateFlow.collect {} }` 在 ViewModel 被清除后继续持有引用）。

**解决方案**：

1. 使用 LeakCanary 定位泄漏点：
   ```bash
   ./gradlew installDebug
   # 运行应用，走查关键路径，等待 10 秒，查看 LeakCanary 通知
   ```
2. ExoPlayer 资源管理：
   ```kotlin
   @Composable
   fun VideoPlayer(videoId: String) {
       val context = LocalContext.current
       val player = remember { ExoPlayer.Builder(context).build() }
       DisposableEffect(Unit) {
           onDispose {
               player.release()  // Composable 销毁时必须释放
           }
       }
   }
   ```
3. ViewModel 中使用 `viewModelScope` 进行 Flow 收集，确保 ViewModel 清除时自动取消。
4. 禁止将 Context/Activity/View 传递到单例中，必须使用 `applicationContext`。
