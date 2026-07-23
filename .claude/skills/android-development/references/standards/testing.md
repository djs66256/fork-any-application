# 测试规范 — Android

> 本文档定义 Android 端的测试策略、框架选型与编写规范。

---

## 1. 测试金字塔

| 层级 | 框架 | 占比 | 目标 | 执行时间 |
|------|------|------|------|---------|
| Unit | JUnit 5 + MockK + Turbine | ~70% | 业务逻辑验证（ViewModel, UseCase, Repository） | < 1s 每个类 |
| Integration | Robolectric + Room In-Memory | ~20% | Android 环境集成（数据库迁移, DAO, DataStore） | < 5s 每个类 |
| UI | Compose UI Test | ~10% | 关键路径 E2E（首页→详情→播放→返回） | < 30s 每个用例 |

**分层说明**：
- **Unit Test**（最底层，最快）：不依赖 Android Framework，纯 JVM 运行。覆盖 UseCase、Repository（mock DataSource）、ViewModel（mock UseCase）、工具类。
- **Integration Test**（中间层）：需要 Robolectric 模拟 Android 环境，覆盖 Room DAO、Migration、DataStore 读写、Retrofit 接口（MockWebServer）。
- **UI Test**（最顶层，最慢）：在设备/模拟器上运行，覆盖首页 Feed 滚动、视频播放跳转、Tab 切换等关键用户路径。

**测试文件路径约定**：

```
app/src/
├── test/                          # 单元测试（JVM，无需设备）
│   └── java/com/djs66256/short_drama/
│       ├── domain/usecase/        # UseCase 单元测试
│       ├── data/repository/       # Repository 单元测试（mock DataSource）
│       └── ui/viewmodel/          # ViewModel 单元测试
└── androidTest/                   # Instrumentation 测试（需要设备/模拟器）
    └── java/com/djs66256/short_drama/
        ├── data/local/            # Room DAO 测试
        └── ui/                    # Compose UI 测试
```

---

## 2. 单元测试

### 2.1 JUnit 5

**测试类命名**：`<被测试类>Test`，如 `HomeViewModelTest`、`GetHomeFeedUseCaseTest`。

**测试方法命名**：`<方法名>_<条件>_<预期结果>`，如：
```kotlin
@Test
fun `loadFeed_whenNetworkSuccess_emitsVideoList`()
@Test
fun `loadFeed_whenNetworkError_emitsErrorState`()
@Test
fun `loadFeed_whenEmptyResult_emitsEmptyState`()
```

**断言风格**：优先使用 Kotlin 风格的断言库（如 Kotest Assertions 或标准库 `check`/`require`），也可使用 JUnit 5 的 `assertEquals`。

```kotlin
// 推荐：表达力强
import org.junit.jupiter.api.Assertions.*

@Test
fun `toggleFavorite_whenNotFavorited_addsToFavorites`() = runTest {
    val useCase = GetHomeFeedUseCase(repository)
    assertEquals(expected, actual, "Feed should match expected video list")
    assertNotNull(result)
    assertTrue(result.isSuccess)
}
```

### 2.2 MockK

MockK 是 Kotlin 首选 Mock 框架，支持协程、扩展函数、object 类 Mock。

```kotlin
// 基础 mock
private val repository: HomeRepository = mockk()

// relaxed mock（未 stub 的方法返回默认值，不用每个都 mock）
private val repository: HomeRepository = mockk(relaxed = true)

// 基础 verify
verify(exactly = 1) { repository.getHomeFeed(any()) }
verify(atLeast = 1) { repository.getHomeFeed(any()) }
verify { repository wasNot Called }

// 捕获参数
val slot = slot<String>()
every { repository.getVideoDetail(capture(slot)) } returns mockk()
// 使用 slot.captured 获取传入的参数值

// 异常 mock
coEvery { repository.getHomeFeed(any()) } throws IOException("Network error")

// 组合条件参数
coEvery { repository.getHomeFeed(page = 0, pageSize = 20) } returns Result.success(mockFeed)

// 每次调用返回不同的值
coEvery { repository.getHomeFeed(any()) } returnsMany listOf(feed1, feed2) andThen feed3
```

**MockK 规则**：
- 使用 Confirm Verifies：在 `@AfterEach` 中调用 `confirmVerified(repository)` 确保所有 stub 的方法都被调用过。
- 禁止滥用 `relaxed = true`——只在辅助依赖上使用，核心被测依赖必须明确 stub。
- suspend 函数 mock 使用 `coEvery` / `coVerify`。

### 2.3 Turbine

Turbine 用于测试 StateFlow / SharedFlow 的值序列。

```kotlin
@Test
fun `uiState_whenLoadFeed_emitsLoadingThenSuccess`() = runTest {
    val viewModel = HomeViewModel(getHomeFeedUseCase)

    viewModel.uiState.test(timeout = 2.seconds) {  // 超时 2 秒
        // isLoading = true（初始加载状态）
        val loadingState = awaitItem()
        assertTrue(loadingState.isLoading)

        // isLoading = false, 有数据
        val successState = awaitItem()
        assertFalse(successState.isLoading)
        assertEquals(20, successState.videos.size)

        // 确认没有更多事件
        expectNoEvents()
    }
}
```

**Turbine 常用 API**：

| API | 用途 |
|-----|------|
| `awaitItem()` | 等待并返回下一个值 |
| `awaitComplete()` | 等待 Flow 完成 |
| `awaitError()` | 等待并返回异常 |
| `expectNoEvents()` | 断言没有更多事件（挂起一段时间） |
| `cancelAndIgnoreRemainingEvents()` | 取消并忽略剩余事件 |

**注意事项**：
- 设置 `timeout` 参数防止测试永久挂起。
- 在 `runTest {}` 协程作用域中使用 Turbine，确保测试取消后 Flow 收集也取消。

### 2.4 ViewModel 测试

```kotlin
@ExtendWith(MainDispatcherExtension::class)  // 确保 Dispatchers.Main 可用
class HomeViewModelTest {

    private val getHomeFeedUseCase: GetHomeFeedUseCase = mockk()
    private val savedStateHandle = SavedStateHandle()
    private lateinit var viewModel: HomeViewModel

    @BeforeEach
    fun setup() {
        viewModel = HomeViewModel(getHomeFeedUseCase, savedStateHandle)
    }

    @Test
    fun `loadFeed_whenSuccess_emitsVideoList`() = runTest {
        val mockVideos = listOf(
            Video(id = "1", title = "Test Episode 1", coverUrl = "url1"),
            Video(id = "2", title = "Test Episode 2", coverUrl = "url2"),
        )
        coEvery { getHomeFeedUseCase(any()) } returns Result.success(mockVideos)

        viewModel.uiState.test {
            viewModel.loadFeed()
            skipItems(1)  // 跳过 loading 状态
            val state = awaitItem()
            assertEquals(2, state.videos.size)
            assertFalse(state.isLoading)
        }
        coVerify { getHomeFeedUseCase.invoke(0) }
    }
}
```

**ViewModel 测试要点**：
- 不要测试 Compose 重组行为（那是 UI 测试的职责）。
- 只测试：状态流转是否按预期 + 副作用（navigation event）是否发出。
- 使用 `MainDispatcherExtension` 覆盖 `Dispatchers.Main`，避免 "Method getMainLooper not mocked" 错误。

### 2.5 Repository 测试

Repository 测试应 mock DataSource 层，测试以下逻辑：
- 数据聚合（Remote + Local 合并）
- 缓存兜底策略（网络失败时返回缓存）
- 数据模型转换（DTO → Domain → Entity）

```kotlin
class HomeRepositoryImplTest {

    private val remoteDataSource: HomeRemoteDataSource = mockk()
    private val localDataSource: HomeLocalDataSource = mockk()
    private lateinit var repository: HomeRepositoryImpl

    @BeforeEach
    fun setup() {
        repository = HomeRepositoryImpl(remoteDataSource, localDataSource)
    }

    @Test
    fun `getHomeFeed_whenRemoteFails_returnsFallbackCache`() = runTest {
        coEvery { remoteDataSource.getHomeFeed(any()) } throws IOException("No network")
        every { localDataSource.getCachedFeed() } returns flowOf(cachedVideosEntity)

        val result = repository.getHomeFeed(0)

        assertTrue(result.isSuccess)
        assertEquals(cachedVideos, result.getOrNull())
    }
}
```

---

## 3. UI 测试

### 3.1 Compose UI Test

Compose UI Test 在设备上运行，测试用户交互行为。

```kotlin
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun scrollFeed_displaysMoreVideos() {
        composeTestRule.setContent {
            ShortDramaTheme {
                HomeScreen(viewModel = fakeViewModel)
            }
        }
        // 断言：初始状态下 Feed 列表存在
        composeTestRule
            .onNodeWithTag("home_feed_list")
            .assertIsDisplayed()

        // 断言：前 5 个视频卡片可见（首屏）
        composeTestRule
            .onAllNodesWithTag("video_card")
            .assertCountEquals(5)  // 首屏期望 5 个

        // 滚动到底部
        composeTestRule
            .onNodeWithTag("home_feed_list")
            .performScrollToNode(matcher = hasTestTag("video_card[19]"))

        // 断言：更多卡片已加载
        composeTestRule
            .onAllNodesWithTag("video_card")
            .assertCountEquals(20)
    }

    @Test
    fun pullToRefresh_reloadsFeed() {
        composeTestRule.setContent {
            ShortDramaTheme {
                HomeScreen(viewModel = fakeViewModel)
            }
        }
        composeTestRule
            .onNodeWithTag("home_refresh_indicator")
            .performTouchInput { swipeDown(startY = 100, endY = 600) }

        // 断言：loading 状态可见
        composeTestRule.waitForIdle()
        // 验证新数据已加载（通过冷数据不同验证）
        composeTestRule
            .onNodeWithTag("video_card[0]")
            .assert(hasText("Updated Title"))
    }
}
```

**testTag 使用约定**：
```kotlin
// 命名规范：<功能>_<元素>
Modifier.testTag("home_feed_list")
Modifier.testTag("video_card")
Modifier.testTag("video_card[$index]")    // 列表项用索引区分
Modifier.testTag("detail_play_button")
Modifier.testTag("search_input_field")
```

### 3.2 Espresso

由于项目主要使用 Compose，Espresso 仅用于极少数 Compose 无法覆盖的场景（如 WebView、自定义 SurfaceView、权限对话框）。

```kotlin
// Compose + Espresso 混合使用示例（处理系统对话框）
@Test
fun grantPermission_dismissesRationaleDialog() {
    // 先用 Compose 测试 API 操作 UI
    composeTestRule.onNodeWithTag("record_button").performClick()
    // 系统对话框弹出后，用 UiAutomator 处理
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    device.wait(Until.findObject(By.text("Allow")), 5000)
    device.findObject(UiSelector().text("Allow")).click()
    // 回到 Compose 断言
    composeTestRule.onNodeWithTag("recording_indicator").assertIsDisplayed()
}
```

**注意**：不推荐在 Compose 项目中使用 Espresso 测试 Compose 组件——Espresso 对 Compose 的语义树支持不可靠，应优先使用 Compose UI Test API。

---

## 4. 快照测试

### 4.1 Paparazzi

Paparazzi 在 JVM 上渲染 Compose 布局并生成 PNG 截图，无需模拟器或真机。

**集成配置**（`app/build.gradle.kts`）：

```kotlin
plugins {
    id("app.cash.paparazzi") version "1.3.5"
}

dependencies {
    testImplementation(libs.paparazzi)
}
```

**基本测试**：

```kotlin
class VideoCardSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = PIXEL_6.copy(
            nightMode = NIGHT_EXCLUDED,
            locale = "zh-CN",
            screenWidth = 1080,
            screenHeight = 2400,
        ),
    )

    @Test
    fun videoCard_default() {
        paparazzi.snapshot {
            ShortDramaTheme {
                VideoCard(
                    video = VideoUi(
                        id = "test_001",
                        title = "霸道总裁爱上我",
                        coverUrl = "https://example.com/cover.jpg",
                        episodeCount = 80,
                    ),
                    onItemClick = {},
                )
            }
        }
    }

    @Test
    fun videoCard_withHighlight() {
        paparazzi.snapshot {
            ShortDramaTheme {
                VideoCard(
                    video = video,
                    isHighlighted = true,
                    onItemClick = {},
                )
            }
        }
    }
}
```

### 4.2 快照存储与比对

**快照文件位置**：`app/src/test/snapshots/<测试类名>_<测试方法名>.png`

**黄金文件管理**：
- 快照文件（golden images）提交到 Git 中，作为设计的"唯一真实来源"。
- 首次运行时通过 `./gradlew :app:test --tests "*.SnapshotTest"` 生成基准快照。
- 后续运行时自动与基准比对，不一致时测试失败。
- 若 UI 变更是有意为之（如设计改版），运行 `./gradlew :app:recordPaparazziDebug` 更新快照，并在 PR 中附带快照 diff。

**Paparazzi vs Compose UI Test 选择**：

| 维度 | Paparazzi | Compose UI Test |
|------|-----------|-----------------|
| 运行环境 | JVM（无设备） | 模拟器/真机 |
| 速度 | 极快（< 1s） | 慢（设备启动 + 渲染） |
| 交互测试 | 不支持 | 支持滚动、点击、输入 |
| 适用场景 | 视觉回归、静态布局验证 | 交互流程、状态变化 |
| 图片加载 | 无法加载网络图片 | 可加载真实图片 |

**推荐策略**：
- 快照测试覆盖：各个组件的不同状态（默认、加载中、空状态、错误状态）
- UI 测试覆盖：关键交互路径
- 两者互补，不可替代

---

## 5. 测试覆盖率

### 5.1 工具

使用 Kover（Kotlin 官方覆盖率工具，JaCoCo 的 Kotlin 替代）进行覆盖率统计。

**集成配置**（根 `build.gradle.kts`）：

```kotlin
plugins {
    id("org.jetbrains.kotlinx.kover") version "0.8.3" apply false
}
```

**app/build.gradle.kts**：

```kotlin
plugins {
    id("org.jetbrains.kotlinx.kover")
}

kover {
    reports {
        filters {
            excludes {
                // 排除不需要测试覆盖的代码
                classes(
                    "*.databinding.*",
                    "*.buildconfig.*",
                    "*.di.*",               // DI 模块不需要测试
                    "*.ComposableSingletons*", // Compose 编译器生成的单例
                )
            }
        }
        total {
            html {
                onCheck = true
                setReportDir(layout.buildDirectory.dir("reports/kover/html"))
            }
            xml {
                onCheck = true
                setReportDir(layout.buildDirectory.dir("reports/kover/xml"))
            }
        }
    }
}
```

**覆盖率执行命令**：

```bash
# 运行单元测试并生成覆盖率报告
./gradlew :app:koverHtmlReport

# 查看 HTML 报告
# 输出：app/build/reports/kover/html/index.html

# 验证覆盖率是否达标（不达标则构建失败）
./gradlew :app:koverVerify
```

### 5.2 最低覆盖率

| 维度 | 单元测试（test） | 集成测试（androidTest） | 说明 |
|------|-----------------|------------------------|------|
| 行覆盖率 | >= 80% | 不做硬性要求 | 核心业务逻辑必测 |
| 分支覆盖率 | >= 70% | 不做硬性要求 | 错误分支必测 |
| UseCase | >= 90% | 不适用 | 全部 UseCase 必须覆盖 |
| ViewModel | >= 85% | 不适用 | 状态流转必测 |
| Repository | >= 80% | 不做硬性要求 | 缓存策略必测 |
| Composable | >= 60% | >= 30% | 交互逻辑必测，纯视觉不做硬性要求 |

**豁免场景**（可在 PR 中说明）：
- 纯布局 Composable（无交互、无状态依赖）可不写测试
- 第三方库的封装适配器（如 Coil 的 ImageLoader 配置）可不写测试
- BuildConfig / 自动生成代码无需测试

**CI 门禁**：
- PR 合并前必须通过 `koverVerify`（总体行覆盖率 >= 80%）
- 新增文件的覆盖率必须 >= 85%
- 若覆盖率不达标，在 PR 中注明原因并等待评审批准后放行
