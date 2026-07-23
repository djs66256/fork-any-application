# 代码规范 — Android

> 本文档定义 Android 端 Kotlin + Jetpack Compose 的完整编码规范。

---

## 1. Kotlin 编码规范

本项目使用 Kotlin 作为唯一开发语言，所有代码必须遵循以下规范。

### 1.1 命名约定

| 类型 | 规则 | 示例 |
|------|------|------|
| 类 / 接口 / 抽象类 | UpperCamelCase | `VideoPlayerViewModel`, `HomeRepository` |
| 函数 / 方法 | lowerCamelCase（动词或动词短语） | `fetchHomeFeed()`, `onPlayClicked()` |
| 变量 / 参数 | lowerCamelCase | `videoId`, `currentPage` |
| 常量（顶层 / companion object） | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE` |
| 枚举值 | UPPER_SNAKE_CASE | `LOADING, SUCCESS, ERROR` |
| 扩展函数 | lowerCamelCase | `fun View.show()`, `fun String.toVideoId()` |
| Composable 函数 | UpperCamelCase（名词或名词短语） | `VideoCard()`, `HomeScreen()` |

补充规则：
- 禁止使用拼音命名，禁止使用无意义的单字母变量名（`i`/`j` 循环变量除外）。
- Boolean 类型属性命名使用 `is` / `has` / `should` 前缀，如 `isLoading`、`hasNextPage`。
- 回调类型参数使用 `onXxx` 命名，如 `onPlayClick: () -> Unit`。

### 1.2 格式化

**ktlint 配置**：使用 ktlint，配置文件为项目根目录下的 `.editorconfig`，CI 中通过 `./gradlew ktlintCheck` 检查。

`.editorconfig` 关键项：
```ini
[*.kt]
indent_size = 4
max_line_length = 120
ij_kotlin_imports_layout = *,java.**,javax.**,kotlin.**,^
```

**detekt 配置**：使用 detekt 进行静态分析，配置文件为 `config/detekt/detekt.yml`。CI 中通过 `./gradlew detekt` 检查。

建议开启的关键规则：
- `TooManyFunctions`：单文件函数上限 30（工具类放宽到 50）
- `LongParameterList`：函数参数上限 6，超出则封装为数据类
- `ComplexMethod`：圈复杂度上限 15
- `MagicNumber`：除 -1、0、1、2 外，数字必须提取为命名常量

**IDE 格式化**：团队成员统一使用 `ktlint-idea` 插件，提交时通过 Git pre-commit hook 自动格式化：
```bash
# .git/hooks/pre-commit
./gradlew ktlintFormat --include-only='src/**/*.kt'
```

### 1.3 可见性

遵循"最小可见性"原则：默认使用 `private`，仅在需要跨类访问时逐步放宽。

- **顶级声明（函数、属性、类）**：默认不写修饰符即为 `public`，跨模块暴露时需明确评估。仅在当前模块内部使用时标记 `internal`。
- **类成员**：默认 `private`，子类需要访问时用 `protected`，模块内跨类用 `internal`，跨模块 API 才用 `public`。
- **Composable 函数**：页面级别（Screen）可以 `internal`，组件级别（Component）默认 `private`，设计系统组件（Design System）可用 `public`。
- **ViewModel**：仅暴露 `StateFlow<UiState>` 和事件处理方法；内部 State 用 `private val _uiState = MutableStateFlow(...)`，外部暴露 `val uiState: StateFlow<UiState>`。

反例：
```kotlin
// 错误：不必要的 public
class HomeViewModel : ViewModel() {
    val retryCount = 0 // 应改为 private
    fun loadData() { }  // 如果只有 HomeScreen 调用，应改为 internal
}
```

### 1.4 协程规范

**调度器选择**：

| 场景 | 调度器 | 说明 |
|------|--------|------|
| 网络请求、数据库读写 | `Dispatchers.IO` | I/O 密集型，线程池弹性伸缩 |
| 数据解析、计算 | `Dispatchers.Default` | CPU 密集型，线程数 = CPU 核心数 |
| UI 状态更新 | `Dispatchers.Main.immediate` | 直接在主线程执行，跳过不必要的帧分发 |
| 单元测试 | `Dispatchers.Unconfined` 或 `StandardTestDispatcher` | 仅在测试中使用 |

**ViewModel 中使用**：
```kotlin
class HomeViewModel(
    private val repository: HomeRepository,
) : ViewModel() {

    fun loadFeed() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.getHomeFeed()
            withContext(Dispatchers.Main.immediate) {
                _uiState.value = result.toUiState()
            }
        }
    }
}
```

**最佳实践**：
- 禁止使用 `GlobalScope`，一律使用结构化并发（`viewModelScope`、`lifecycleScope`）。
- 所有 suspend 函数必须指明 `CoroutineDispatcher`，不依赖调用方线程假设。
- 不要在 `suspend` 函数内部写 `withContext(Dispatchers.Main)`——让调用方决定线程。
- 长耗时操作（如文件下载）使用 `withContext(Dispatchers.IO)` 包裹，不要阻塞主线程。
- 协程取消必须可响应：在循环中检查 `isActive` 或调用 `yield()`。
- 异常处理使用 `supervisorScope` 或 `CoroutineExceptionHandler`，避免子协程失败传播到父协程。

---

## 2. Jetpack Compose 编码规范

所有 UI 实现使用 Jetpack Compose，遵循声明式范式。

### 2.1 Composable 函数

**参数顺序**：
1. 必选数据参数（如 `videos: List<VideoItem>`）
2. 修饰符 `modifier: Modifier = Modifier`（必须放在第一个可选参数位置）
3. 可选样式参数（如 `isHighlighted: Boolean = false`）
4. 回调参数（如 `onItemClick: (String) -> Unit`）
5. 最后是可选的 Composable 插槽（`content: @Composable () -> Unit`）

**正确示例**：
```kotlin
@Composable
fun VideoCard(
    video: VideoItem,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    onItemClick: (String) -> Unit = {},
    trailingContent: @Composable RowScope.() -> Unit = {},
)
```

**必须提供 Preview**：每个可作为独立组件的 Composable 至少提供一个 `@Preview` 函数，并在其中使用 `ShortDramaTheme` 包裹：
```kotlin
@Preview(showBackground = true)
@Composable
private fun VideoCardPreview() {
    ShortDramaTheme {
        VideoCard(
            video = VideoItem.previewMock(),
            onItemClick = {},
        )
    }
}
```

**函数命名**：Composable 函数以名词或名词短语命名（PascalCase），不使用 `get`/`set`/`create` 等动词前缀。Composable 函数名不得与返回值类型名相同。

### 2.2 状态提升

**核心原则**：单一数据源 + 单向数据流（UDF）。

- **Stateless Composable**：组件不持有自己的状态，所有状态通过参数传入，事件通过回调传出。这是默认选择的模式。
- **Stateful Composable**：仅在"叶子组件"级别使用，如 `TextField` 的内部输入缓冲。页面级别的 Screen 都不应是 Stateful 的，而应委托给 ViewModel。

**状态提升示例**：
```kotlin
// 正确：Stateless，状态由 ViewModel 管理
@Composable
fun VideoList(
    videos: List<VideoItem>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
) {
    if (isLoading) {
        LoadingIndicator(modifier = modifier)
    } else {
        LazyColumn(modifier = modifier) {
            items(videos, key = { it.id }) { video ->
                VideoCard(video = video)
            }
        }
    }
}
```

**禁止模式**：在 Screen 级别的 Composable 中使用 `var state by remember { mutableStateOf(...) }`。这类状态应封装在 ViewModel 中。

### 2.3 副作用

| API | 使用场景 | 注意事项 |
|-----|---------|---------|
| `LaunchedEffect(key)` | 进入组合时发起一次性异步操作（如首屏数据加载、埋点上报） | key 不变不会重新执行；key 变化时上次协程自动取消 |
| `DisposableEffect(key)` | 需要注册/反注册监听器的场景 | `onDispose` 中清理资源，如取消 Flow 收集 |
| `rememberCoroutineScope()` | 用户交互驱动的异步操作（如点击按钮后发起网络请求） | 仅在非 Composable 作用域调用；组合消失时协程自动取消 |
| `SideEffect` | 将 Compose 状态同步到外部非 Compose 系统 | 每次重组都执行 |
| `derivedStateOf` | 从其他状态派生计算值，降低不必要重组 | 仅在计算量较大时使用 |

**典型场景**：
```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    // 首屏加载
    LaunchedEffect(Unit) {
        viewModel.loadHomeFeed()
    }
    // 收集单次事件（Snackbar、导航等）
    LaunchedEffect(Unit) {
        viewModel.oneTimeEvent.collect { event ->
            when (event) {
                is HomeEvent.ShowError -> { /* show snackbar */ }
            }
        }
    }
}
```

### 2.4 重组优化

**避免不必要的重组**：
- 将不变的 lambda 提到 Composable 外部或使用 `remember`：
  ```kotlin
  // 错误：每次重组都创建新 lambda
  VideoCard(onClick = { navigateToDetail(it) })
  // 正确
  val onVideoClick = remember { { id: String -> navigateToDetail(id) } }
  VideoCard(onClick = onVideoClick)
  ```
- 对数据类使用 `@Stable` 或 `@Immutable` 注解，帮助 Compose 编译器跳过不必要重组。
- 列表使用 `LazyColumn` 的 `key` 参数，保证 diff 算法精确：
  ```kotlin
  LazyColumn {
      items(videos, key = { it.id }) { video -> VideoCard(video) }
  }
  ```
- 避免在 Composable 函数中直接使用 `val list = listOf(...)` 创建新集合作为参数——这会在每次重组时创建新实例导致子组件重组。
- 使用 `Modifier` 的位置遵循"外部 Modifier 先调用"原则（如 `Modifier.size().padding().clip()`），不要在组件内部重复设置已在外部传入的 Modifier 属性。

---

## 3. 资源命名规范

所有资源文件使用小写字母 + 下划线分隔（snake_case），禁止使用驼峰或大写。

### 3.1 Drawable

| 前缀 | 用途 | 示例 |
|------|------|------|
| `ic_` | 矢量图标（SVG / VectorDrawable） | `ic_play_24.xml`, `ic_heart_outline_24.xml` |
| `bg_` | 背景图 / 背景 Shape | `bg_gradient_top.xml`, `bg_card_rounded_8.xml` |
| `img_` | 位图图片（PNG / WebP） | `img_placeholder_video.webp`, `img_splash_logo.png` |
| `anim_` | 动画文件（AnimatedVectorDrawable） | `anim_loading_spinner.xml` |

补充规则：
- 图标文件后缀添加尺寸标识（dp），如 `_24`、`_48`，便于适配不同场景。
- 同一图标的多种状态使用后缀区分：`ic_heart_filled_24`、`ic_heart_outline_24`。

### 3.2 String

**命名规则**：`<模块>_<用途>_<描述>`

| 类型 | 规则 | 示例 |
|------|------|------|
| 页面标题 | `title_<页面>` | `title_home`, `title_mine` |
| 按钮文字 | `btn_<动作>` | `btn_login`, `btn_submit` |
| 提示信息 | `msg_<场景>` | `msg_network_error`, `msg_login_success` |
| 标签/说明 | `label_<内容>` | `label_episode_count` |
| 确认/错误/成功 | `confirm_/error_/success_` | `confirm_delete`, `error_invalid_phone` |

**占位符使用**：使用 `%1$s`（String）、`%1$d`（Int）等格式化占位符，禁止字符串拼接：
```xml
<!-- 正确 -->
<string name="label_episode_count">共 %1$d 集</string>
<!-- 错误 -->
<string name="label_episode_total">共</string>
<string name="label_episode_unit">集</string>
```

- 所有面向用户的字符串必须放入 `strings.xml`，禁止在代码中硬编码中文或英文字符串。
- 英文、中文等语言对应文件：`values/strings.xml`（默认中文）、`values-en/strings.xml`（英文）。

### 3.3 Color

**命名规则**：遵循 Material Design Color System。

| 类型 | 前缀 | 示例 |
|------|------|------|
| 主题色 | `primary_` / `secondary_` / `tertiary_` | `primary_500` |
| 背景色 | `background_` / `surface_` | `background_dark`, `surface_card` |
| 文本色 | `on_primary_` / `on_background_` / `text_` | `text_primary`, `text_secondary`, `text_disabled` |
| 功能色 | `error_` / `success_` / `warning_` | `error_default` |
| 叠加色 | `overlay_` | `overlay_40` |

颜色定义在 `Color.kt` 中，使用 Compose `Color` 类型：
```kotlin
val Primary500 = Color(0xFF6C5CE7)
val TextPrimary = Color(0xFF1A1A2E)
val TextSecondary = Color(0xFF8E8E93)
```

### 3.4 Dimen

**命名规则**：`<类型>_<用途>_<数值>`

| 类型 | 规则 | 示例 |
|------|------|------|
| 间距 | `spacing_<size>` | `spacing_4`, `spacing_8`, `spacing_16` |
| 圆角 | `radius_<size>` | `radius_8`, `radius_16` |
| 字号 | `text_<用途>` | `text_body`, `text_title_large`, `text_caption` |
| 组件尺寸 | `<组件>_<属性>` | `appbar_height`, `bottom_nav_height` |

使用 Compose 定义在 `Dimen.kt` 中：
```kotlin
object Dimens {
    val spacing4 = 4.dp
    val spacing8 = 8.dp
    val spacing16 = 16.dp
    val radius8 = 8.dp
    val radius16 = 16.dp
    val appbarHeight = 56.dp
    val bottomNavHeight = 64.dp
}
```

- 缩放无关文字使用 `sp` 单位（通过 `MaterialTheme.typography` 定义）。
- 所有尺寸在 `Dimens` 对象中统一定义，不直接在 Composable 中写魔法数字。
- 使用 4dp 基础网格系统：所有间距应为 4 的倍数。

---

## 4. 代码审查清单

每位 reviewer 在 CR 时必须逐项检查以下内容，不通过不得合并。

**命名与格式**：
- [ ] 类名、函数名、变量名符合命名约定（1.1 节）
- [ ] 无拼音命名、无无意义单字母变量
- [ ] 代码通过 ktlint 和 detekt 检查（`./gradlew ktlintCheck detekt`）

**可见性与封装**：
- [ ] 类成员默认 `private`，仅必要接口放宽
- [ ] ViewModel 内部 mutable state 不暴露给 UI 层
- [ ] 跨模块暴露的 API 有明确文档注释

**协程与线程**：
- [ ] 无 `GlobalScope` 使用
- [ ] 调度器选择正确（I/O / Default / Main）
- [ ] 长耗时操作用 `withContext(Dispatchers.IO)`
- [ ] 协程可响应取消

**Compose**：
- [ ] Composable 参数顺序正确（数据 → Modifier → 样式 → 回调 → 插槽）
- [ ] Screen 级别使用 ViewModel 管理状态，非 `remember { mutableStateOf(...) }`
- [ ] 列表使用 `key` 参数
- [ ] 每个独立组件有 `@Preview`
- [ ] 副作用 API 使用正确（LaunchedEffect / DisposableEffect / rememberCoroutineScope）

**资源与字符串**：
- [ ] 字符串全部从 `strings.xml` 引用，无硬编码
- [ ] Drawable / Color / Dimen 命名符合规范
- [ ] 尺寸使用 `Dimens` 对象引用，无魔法数字

**测试**：
- [ ] 新增业务逻辑有对应单元测试
- [ ] 关键用户路径有 UI 测试覆盖

**安全**：
- [ ] 无硬编码 API Key / Secret / Token
- [ ] 无 HTTP 明文 `http://` 请求（通过 `network_security_config.xml` 限制）
- [ ] 敏感数据使用 EncryptedSharedPreferences 存储
