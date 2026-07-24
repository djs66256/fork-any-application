# 实现计划：Android — 项目初始化与架构设计

> 创建日期：2026-07-24
> 对应技术方案：design-android.md
> 对应需求：spec.md

## 概述

从零初始化 Android 工程，搭建 Kotlin 2.0 + Jetpack Compose + Material3 的三层 Clean Architecture（Presentation → Domain → Data + Core）骨架，集成 Hilt DI、Retrofit + OkHttp 网络层、Jetpack Navigation Compose 路由、djsdrama:// Deep Links、Detekt 静态分析，以及对应的单元测试。完成后的工程可通过 `./gradlew assembleDebug` 构建，在模拟器上展示 ShortDrama 应用名和版本号。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> Android 端使用 JUnit 4 + MockK + Turbine，Domain 层纯 Kotlin 可在 JVM 直接运行。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | Gradle 项目可正常构建 | `./gradlew assembleDebug` | BUILD SUCCESSFUL，生成 `app/build/outputs/apk/debug/app-debug.apk` | 构建验证 | P0 |
| T-02 | ApiResult sealed class 各分支正确持有数据 | `ApiResult.Success("data")`, `ApiResult.Error("code","msg")`, `ApiResult.Exception(RuntimeException())` | pattern matching 后各分支能正确取出 data / code+message / throwable | 单元测试 | P0 |
| T-03 | DramaDto.toDomain() 正确转换 | 构造 `DramaDto(id="1",title="Test",...)` | 转换后的 `Drama` 对象字段一一对应，snake_case JSON key 通过 `@SerialName` 正确映射 | 单元测试 | P0 |
| T-04 | GetDramasUseCase 正确委托给 DramaRepository | `invoke(page=1, pageSize=20)` | 调用 `dramaRepository.getDramas(1, 20)` 一次，返回 repository 的 ApiResult | 单元测试 | P0 |
| T-05 | Hilt DI 编译通过 | `./gradlew assembleDebug`（包含 Hilt 注解处理） | KSP 注解处理无错误，DI 依赖关系正确 | 构建验证 | P0 |
| T-06 | HomeViewModel UiState 状态流转 | 创建 HomeViewModel 实例，用 Turbine 收集 StateFlow | 第一帧 `isLoading=true`，第二帧 `isLoading=false`；`appName="ShortDrama"`，`appVersion="0.1.0"` | 单元测试 | P0 |
| T-07 | Navigation Routes 路径生成正确 | `Routes.player("abc123")`, `Routes.dramaDetail("xyz456")` | `"player/abc123"`, `"dramaDetail/xyz456"` | 单元测试 | P1 |
| T-08 | PlayerViewModel 从 SavedStateHandle 提取 videoId | SavedStateHandle 含 `{"videoId":"001"}` | `viewModel.videoId == "001"` | 单元测试 | P1 |
| T-09 | Detekt 静态分析无问题 | `./gradlew detekt` | 0 issues，BUILD SUCCESSFUL | CLI 验证 | P0 |

## 实现步骤

### Step 1：Gradle 构建系统初始化

- **关联测试**：T-01（构建成功）
- **目标文件**：
  - `android/gradle/libs.versions.toml`
  - `android/build.gradle.kts`
  - `android/settings.gradle.kts`
  - `android/app/build.gradle.kts`
  - `android/gradle.properties`
  - `android/gradle/wrapper/gradle-wrapper.properties`
  - `android/app/proguard-rules.pro`
  - `android/.gitignore`

- **实现内容**：
  1. 创建 Version Catalog（`libs.versions.toml`），锁定 AGP 8.7.0、Kotlin 2.0.21、Compose BOM 2024.12.01、Hilt 2.53.1、Retrofit 2.11.0、OkHttp 4.12.0、kotlinx-serialization 1.7.3、Detekt 1.23.7 等版本
  2. 创建根 `build.gradle.kts`，声明所有插件（android-application、kotlin-android、kotlin-compose、kotlin-serialization、hilt、ksp、detekt）但均 `apply false`
  3. 创建 `settings.gradle.kts`，配置 pluginManagement/dependencyResolution repositories（Google + Maven Central + 阿里云镜像），设置 `rootProject.name = "ShortDrama"`，`include(":app")`
  4. 创建 `app/build.gradle.kts`，应用所有插件，配置：namespace、compileSdk 36、minSdk 26、targetSdk 36、versionCode 1、versionName "0.1.0"、applicationId、BuildConfig（API_BASE_URL/APP_NAME/APP_VERSION）、compileOptions Java 21、kotlinOptions jvmTarget 21、dependencies（Compose BOM、Material3、Navigation、Hilt、Retrofit、kotlinx-serialization、DataStore、测试库）
  5. 创建 `gradle.properties`（JVM args、parallel、caching、configuration-cache、AndroidX 等）
  6. 创建 `gradle-wrapper.properties`（Gradle 8.9）
  7. 创建 `app/proguard-rules.pro`（基础混淆规则骨架）
  8. 创建 `android/.gitignore`（忽略 build/、.gradle/、*.apk 等）

- **验证方式**：
  - 在 `android/` 目录执行 `./gradlew tasks` 确认项目可被 Gradle 识别
  - Android Studio 打开 `android/` 目录，Gradle Sync 无依赖解析错误

- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/gradle/libs.versions.toml` | 新增 | Version Catalog，含 versions / libraries / plugins 三部分，锁定所有依赖版本 |
| `android/build.gradle.kts` | 新增 | 根项目构建脚本，声明所有 plugin alias 并 apply false |
| `android/settings.gradle.kts` | 新增 | 项目设置，仓库配置（含阿里云镜像），rootProject 命名 |
| `android/app/build.gradle.kts` | 新增 | app 模块构建脚本，android 块、buildTypes（BuildConfig 字段）、dependencies |
| `android/gradle.properties` | 新增 | Gradle JVM 配置、并行/缓存/configuration-cache、AndroidX 开关 |
| `android/gradle/wrapper/gradle-wrapper.properties` | 新增 | Gradle 8.9 wrapper 配置 |
| `android/app/proguard-rules.pro` | 新增 | ProGuard 混淆规则骨架 |
| `android/.gitignore` | 新增 | Android 专用 gitignore（build/、.gradle/、*.apk、*.jks 等） |

---

### Step 2：Core 层 — Theme + AppConfig

- **关联测试**：无独立测试（构建验证 T-01 覆盖）
- **目标文件**：
  - `android/app/src/main/java/com/djs66256/short_drama/core/theme/Color.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/core/theme/Type.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/core/theme/Theme.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/core/config/AppConfig.kt`

- **实现内容**：
  1. 创建 `Color.kt`，定义 Material3 颜色 tokens（Primary/PrimaryContainer、Secondary/SecondaryContainer、Tertiary/TertiaryContainer、Error/ErrorContainer、Background/Surface/SurfaceVariant 及各 onXxx 颜色），light 主题以 M3 默认紫色系为准
  2. 创建 `Type.kt`，定义 `Typography` 实例，覆盖 headlineLarge/headlineMedium/titleLarge/bodyLarge/bodyMedium/labelLarge 五个级别的字体规格
  3. 创建 `Theme.kt`，定义 `LightColorScheme` 和 `DarkColorScheme`，实现 `ShortDramaTheme` Composable，支持系统深色模式跟随（`isSystemInDarkTheme()`），设置状态栏颜色和外观
  4. 创建 `AppConfig.kt`（`BuildConfigWrapper` object），封装 `BuildConfig.DEBUG`、`BuildConfig.API_BASE_URL`、`BuildConfig.APP_NAME`、`BuildConfig.APP_VERSION` 的只读访问

- **验证方式**：
  - Theme 无编译错误（后续步骤依赖 Theme 构建）
  - `BuildConfigWrapper` 可正确引用 Step 1 中 `build.gradle.kts` 定义的 BuildConfig 字段

- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/theme/Color.kt` | 新增 | Material3 颜色 tokens（14 个 primary/secondary/tertiary/error/background/surface 色值） |
| `android/app/src/main/java/com/djs66256/short_drama/core/theme/Type.kt` | 新增 | Typography 实例，6 个字体规格覆盖 headline 到 label |
| `android/app/src/main/java/com/djs66256/short_drama/core/theme/Theme.kt` | 新增 | ShortDramaTheme Composable，LightColorScheme + DarkColorScheme，状态栏适配 |
| `android/app/src/main/java/com/djs66256/short_drama/core/config/AppConfig.kt` | 新增 | BuildConfigWrapper object，封装 BuildConfig 字段访问 |

---

### Step 3：Core 层 — Network（网络基础设施）

- **关联测试**：T-02（ApiResult sealed class）
- **目标文件**：
  - `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiResult.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiClient.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt`
  - `android/app/src/test/java/com/djs66256/short_drama/core/network/ApiResultTest.kt`

- **实现内容**：
  1. 创建 `ApiResult.kt`，定义 sealed class `ApiResult<out T>`，含三个分支：`Success(data: T)`、`Error(code: String, message: String)`、`Exception(throwable: Throwable)`
  2. 创建 `ApiService.kt`，定义 Retrofit interface，包含 7 个端点方法：`health()`、`getDramas()`、`createDrama()`、`getDramaDetail()`、`getEpisodeDetail()`、`startPlayer()`、`stopPlayer()`，对应 design.md 中的 API 契约
  3. 创建 `ApiClient.kt`，使用 object 单例，配置 `Json` 实例（`ignoreUnknownKeys = true`, `coerceInputValues = true`），构建 `OkHttpClient`（含 AuthInterceptor + HttpLoggingInterceptor，30s 超时），构建 `Retrofit` 实例（baseUrl 来自 BuildConfigWrapper，kotlinx.serialization converter），暴露 `apiService` 属性
  4. 创建 `AuthInterceptor.kt`，实现 OkHttp `Interceptor`，当前阶段为骨架（空实现，预留 Token 注入）
  5. 创建 `ApiResultTest.kt`，编写 T-02 测试：验证 `Success("hello").data == "hello"`、`Error("404","msg")` 能正确取值、`Exception(RuntimeException("boom"))` 能正确取异常

- **验证方式**：
  - 运行 `./gradlew test` 确认 `ApiResultTest` 通过（T-02）

- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiResult.kt` | 新增 | sealed class ApiResult<T>，Success/Error/Exception 三个分支 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 新增 | Retrofit interface，7 个端点（health/dramas/dramas:id/episodes:id/player:start/player:stop/create） |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiClient.kt` | 新增 | object 单例，OkHttpClient + Retrofit 构建，kotlinx.serialization converter |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt` | 新增 | OkHttp Interceptor 骨架，预留 JWT Token 注入 |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/ApiResultTest.kt` | 新增 | ApiResult sealed class 单元测试（T-02） |

---

### Step 4：Domain 层（业务模型 + 仓库接口 + 用例）

- **关联测试**：T-04（UseCase 委托）
- **目标文件**：
  - `android/app/src/main/java/com/djs66256/short_drama/domain/model/Drama.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/domain/model/Episode.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/domain/repository/DramaRepository.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/domain/repository/EpisodeRepository.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetDramasUseCase.kt`
  - `android/app/src/test/java/com/djs66256/short_drama/domain/usecase/GetDramasUseCaseTest.kt`

- **实现内容**：
  1. 创建 `Drama.kt`，定义 `data class Drama`（id, title, description, coverUrl, category, episodeCount, tags, rating, createdAt, updatedAt），字段名和类型与 Backend Zod Schema 一致
  2. 创建 `Episode.kt`，定义 `data class Episode`（id, dramaId, title, episodeNumber, videoUrl, duration, thumbnailUrl, createdAt, updatedAt）
  3. 创建 `DramaRepository.kt`，定义 `interface DramaRepository`：`suspend fun getDramas(page, pageSize): ApiResult<List<Drama>>`、`suspend fun getDramaDetail(id): ApiResult<Drama>`
  4. 创建 `EpisodeRepository.kt`，定义 `interface EpisodeRepository`：`suspend fun getEpisodeDetail(id): ApiResult<Episode>`
  5. 创建 `GetDramasUseCase.kt`，通过 `@Inject constructor(dramaRepository: DramaRepository)` 注入依赖，`operator fun invoke(page, pageSize)` 委托给 repository
  6. 创建 `GetDramasUseCaseTest.kt`，编写 T-04 测试：使用 MockK mock `DramaRepository`，验证 `invoke(1, 20)` 调用 `repository.getDramas(1, 20)` 一次，并返回 mock 的 ApiResult

- **验证方式**：
  - Domain 层纯 Kotlin，无 Android 依赖：可直接在 `src/test/` 下运行
  - 运行 `./gradlew test --tests "GetDramasUseCaseTest"` 确认 T-04 通过

- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/Drama.kt` | 新增 | Drama data class，10 个字段与 Backend Zod Schema 对齐 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/Episode.kt` | 新增 | Episode data class，9 个字段与 Backend Zod Schema 对齐 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/DramaRepository.kt` | 新增 | DramaRepository interface，getDramas + getDramaDetail |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/EpisodeRepository.kt` | 新增 | EpisodeRepository interface，getEpisodeDetail |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetDramasUseCase.kt` | 新增 | GetDramasUseCase，@Inject constructor + operator invoke |
| `android/app/src/test/java/com/djs66256/short_drama/domain/usecase/GetDramasUseCaseTest.kt` | 新增 | UseCase 委托验证单测（T-04），使用 MockK |

---

### Step 5：Data 层（DTO + DataSource + Repository 实现）

- **关联测试**：T-03（DTO toDomain 转换）
- **目标文件**：
  - `android/app/src/main/java/com/djs66256/short_drama/data/dto/DramaDto.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/data/dto/EpisodeDto.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/data/dto/PaginationDto.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/data/dto/ErrorDto.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt`
  - `android/app/src/test/java/com/djs66256/short_drama/data/dto/DramaDtoTest.kt`

- **实现内容**：
  1. 创建 `DramaDto.kt`，`@Serializable` data class，字段使用 `@SerialName` 映射 snake_case JSON key（cover_url、episode_count、created_at、updated_at），提供 `fun toDomain(): Drama` 扩展函数
  2. 创建 `EpisodeDto.kt`，`@Serializable` data class，映射 drama_id、episode_number、video_url、thumbnail_url、created_at、updated_at，提供 `fun toDomain(): Episode`
  3. 创建 `PaginationDto.kt`，字段 page、pageSize（@SerialName("page_size")）、total、totalPages（@SerialName("total_pages")）
  4. 创建 `ErrorDto.kt`，含 `ErrorDto(error: ErrorDetail)` 和 `ErrorDetail(code, message)`，以及 `DramaListResponseDto(data: List<DramaDto>, pagination: PaginationDto)`
  5. 创建 `DramaRemoteDataSource.kt`，`@Singleton @Inject constructor(apiService)`，实现 `getDramas()` 和 `getDramaDetail()`，封装 Retrofit 响应 → ApiResult 转换（含成功/失败/异常分支）
  6. 创建 `DramaRepositoryImpl.kt`，`@Singleton @Inject constructor(remoteDataSource)`，实现 `DramaRepository` 接口，方法委托给 DataSource
  7. 创建 `DramaDtoTest.kt`，编写 T-03 测试：构造 DramaDto 实例，验证 `toDomain()` 后所有字段值一致（含 snake_case mapping 正确性）

- **验证方式**：
  - 运行 `./gradlew test --tests "DramaDtoTest"` 确认 T-03 通过
  - 确认 `DramaRepositoryImpl` 正确实现 Domain 层的 `DramaRepository` 接口（依赖方向 Data → Domain）

- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/DramaDto.kt` | 新增 | @Serializable DTO，@SerialName 映射 snake_case，toDomain() |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/EpisodeDto.kt` | 新增 | @Serializable DTO，@SerialName 映射，toDomain() |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/PaginationDto.kt` | 新增 | @Serializable 分页响应模型 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/ErrorDto.kt` | 新增 | ErrorDto + ErrorDetail + DramaListResponseDto |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt` | 新增 | Remote DataSource，Retrofit 响应 → ApiResult 转换 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt` | 新增 | DramaRepository 实现，委托 DataSource |
| `android/app/src/test/java/com/djs66256/short_drama/data/dto/DramaDtoTest.kt` | 新增 | DTO toDomain() 转换单测（T-03） |

---

### Step 6：DI 层（Hilt 依赖注入框架搭建）

- **关联测试**：T-05（DI 编译验证）
- **目标文件**：
  - `android/app/src/main/java/com/djs66256/short_drama/ShortDramaApplication.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/core/di/NetworkModule.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt`

- **实现内容**：
  1. 创建 `ShortDramaApplication.kt`，添加 `@HiltAndroidApp` 注解，继承 `Application()`
  2. 创建 `AppModule.kt`，`@Module @InstallIn(SingletonComponent::class) object AppModule`，当前为空占位（后续 PRD 添加 DataStore/Analytics 等绑定）
  3. 创建 `NetworkModule.kt`，`@Module @InstallIn(SingletonComponent::class) object NetworkModule`，`@Provides @Singleton fun provideApiService(): ApiService` 提供 `ApiClient.apiService` 单例
  4. 创建 `RepositoryModule.kt`，`@Module @InstallIn(SingletonComponent::class) object RepositoryModule`，`@Provides @Singleton fun provideDramaRepository(dataSource): DramaRepository` 绑定 `DramaRepositoryImpl` 到 `DramaRepository` 接口

- **验证方式**：
  - 在 AndroidManifest 中引用 `ShortDramaApplication`（Step 7 完成），`./gradlew assembleDebug` 编译通过（T-05）
  - KSP 注解处理无错误，DI 依赖图完整

- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/ShortDramaApplication.kt` | 新增 | @HiltAndroidApp Application 入口 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt` | 新增 | Hilt @Module，SingletonComponent，空占位 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/NetworkModule.kt` | 新增 | ApiService 单例提供 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 新增 | DramaRepository 接口 → DramaRepositoryImpl 绑定 |

---

### Step 7：Presentation 层 — HomeScreen + MainActivity + 应用壳

- **关联测试**：T-06（HomeViewModel 状态流转）
- **目标文件**：
  - `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt`
  - `android/app/src/main/AndroidManifest.xml`
  - `android/app/src/main/res/values/strings.xml`
  - `android/app/src/main/res/values/themes.xml`
  - `android/app/src/test/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModelTest.kt`

- **实现内容**：
  1. 创建 `HomeViewModel.kt`，`@HiltViewModel`，定义 `HomeUiState` data class（isLoading, appName, appVersion），暴露 `StateFlow<HomeUiState>`，init 块中标记 isLoading = false
  2. 创建 `HomeScreen.kt`，Composable 函数，通过 `hiltViewModel()` 获取 ViewModel，`collectAsState()` 订阅 StateFlow，loading 状态显示 `CircularProgressIndicator`，正常状态显示 Column 居中布局：PlayCircle Icon + 应用名 "ShortDrama" + 版本号 "v0.1.0"
  3. 创建 `MainActivity.kt`，`@AndroidEntryPoint`，继承 `ComponentActivity()`，`onCreate` 中 `enableEdgeToEdge()`，`setContent { ShortDramaTheme { ... } }`，预留 `handleDeepLink(intent)` 方法
  4. 创建 `AndroidManifest.xml`，声明 INTERNET + ACCESS_NETWORK_STATE 权限，配置 `<application android:name=".ShortDramaApplication"`，声明 MainActivity 为 LAUNCHER（singleTask launchMode），包含 `djsdrama://` intent-filter
  5. 创建 `strings.xml`，定义 `app_name=ShortDrama`
  6. 创建 `themes.xml`，定义 `Theme.ShortDrama`（parent 为 Material3 无 ActionBar 主题）
  7. 创建 `HomeViewModelTest.kt`，编写 T-06 测试：使用 Turbine 收集 StateFlow，验证 initialState `isLoading=true`、`appName="ShortDrama"`、`appVersion="0.1.0"`，随后 `isLoading` 变为 false

- **验证方式**：
  - 运行 `./gradlew test --tests "HomeViewModelTest"` 确认 T-06 通过
  - 运行 `./gradlew assembleDebug` 确认 APK 可构建
  - 在模拟器上安装运行，验证 HomeScreen 显示应用名和版本号

- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 新增 | HomeScreen Composable，PlayCircle Icon + 应用名 + 版本号 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt` | 新增 | @HiltViewModel，HomeUiState + StateFlow |
| `android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt` | 新增 | @AndroidEntryPoint，单 Activity，enableEdgeToEdge，ShortDramaTheme |
| `android/app/src/main/AndroidManifest.xml` | 新增 | INTERNET 权限，Application/Activity 声明，djsdrama:// intent-filter |
| `android/app/src/main/res/values/strings.xml` | 新增 | app_name string 资源 |
| `android/app/src/main/res/values/themes.xml` | 新增 | Theme.ShortDrama，Material3 NoActionBar 父主题 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModelTest.kt` | 新增 | HomeViewModel UiState 状态流转单测（T-06），使用 Turbine |

---

### Step 8：Navigation 路由 + Deep Links

- **关联测试**：T-07（Routes 路径生成）
- **目标文件**：
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`
  - `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt`

- **实现内容**：
  1. 创建 `NavGraph.kt`，定义 `Routes` object（HOME = "home"、PLAYER = "player/{videoId}"、DRAMA_DETAIL = "dramaDetail/{dramaId}" + 辅助函数），实现 `NavGraph` Composable（NavHost，startDestination = HOME），为每个 composable 配置 deepLinks（djsdrama://open → home、djsdrama://player/{videoId} → player、djsdrama://drama/{dramaId} → dramaDetail）
  2. 更新 `MainActivity.kt`：在 `setContent` 中创建 `rememberNavController()`，将 `NavGraph(navController)` 作为 ShortDramaTheme 的内容
  3. 创建 `RoutesTest.kt`，编写 T-07 测试：验证 `Routes.player("abc")` 返回 `"player/abc"`，`Routes.dramaDetail("xyz")` 返回 `"dramaDetail/xyz"`，`Routes.HOME` 等于 `"home"`

- **验证方式**：
  - 运行 `./gradlew test --tests "RoutesTest"` 确认 T-07 通过
  - 使用 adb 命令验证 Deep Links：`adb shell am start -W -a android.intent.action.VIEW -d "djsdrama://open"` 应启动 ShortDrama 首页

- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 新增 | Routes object + NavGraph Composable，3 个路由 + deepLinks |
| `android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt` | 修改 | setContent 中集成 NavGraph + rememberNavController |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 新增 | Routes 路径生成单测（T-07） |

---

### Step 9：Feature Screens + Detekt + CLAUDE.md 收尾

- **关联测试**：T-08（PlayerViewModel）、T-09（Detekt）
- **目标文件**：
  - `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/feature/dramadetail/ui/DramaDetailScreen.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/feature/dramadetail/viewmodel/DramaDetailViewModel.kt`
  - `android/.detekt/detekt.yml`
  - `android/CLAUDE.md`
  - `android/app/src/test/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModelTest.kt`

- **实现内容**：
  1. 创建 `PlayerScreen.kt`，占位 Composable：居中显示"播放器"标题 + "Video ID: {videoId}"（支持深色模式 Material3 颜色）
  2. 创建 `PlayerViewModel.kt`，`@HiltViewModel`，通过 `SavedStateHandle` 提取 `videoId` 参数，暴露为 val
  3. 创建 `DramaDetailScreen.kt`，占位 Composable：居中显示"剧集详情"标题 + "Drama ID: {dramaId}"
  4. 创建 `DramaDetailViewModel.kt`，`@HiltViewModel`，通过 `SavedStateHandle` 提取 `dramaId` 参数
  5. 创建 `.detekt/detekt.yml`，配置关键规则：maxIssues=0、MagicNumber（忽略常见数字）、WildcardImport/UnusedImports 激活、MaxLineLength=120、FunctionNaming 忽略 @Composable、TooGenericExceptionCaught=false、UndocumentedPublicClass/Function=false
  6. 创建 `PlayerViewModelTest.kt`，编写 T-08 测试：使用 mock `SavedStateHandle`，验证 `videoId` 提取正确
  7. 创建 `android/CLAUDE.md`，定义 Android 端开发规范：技术栈说明、目录结构、架构分层、构建命令、测试策略、编码约束（禁止硬编码、Domain 层纯 Kotlin 等）、代码风格

- **验证方式**：
  - 运行 `./gradlew test` 确认所有单元测试（T-02/T-03/T-04/T-06/T-07/T-08）通过
  - 运行 `./gradlew detekt` 确认 0 issues（T-09）
  - 运行 `./gradlew assembleDebug` 确认完整构建通过（T-01/T-05）

- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt` | 新增 | PlayerScreen 占位 Composable |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` | 新增 | @HiltViewModel，SavedStateHandle 提取 videoId |
| `android/app/src/main/java/com/djs66256/short_drama/feature/dramadetail/ui/DramaDetailScreen.kt` | 新增 | DramaDetailScreen 占位 Composable |
| `android/app/src/main/java/com/djs66256/short_drama/feature/dramadetail/viewmodel/DramaDetailViewModel.kt` | 新增 | @HiltViewModel，SavedStateHandle 提取 dramaId |
| `android/.detekt/detekt.yml` | 新增 | Detekt 配置，maxIssues=0，关键规则覆盖 |
| `android/CLAUDE.md` | 新增 | Android 端开发规范文档 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModelTest.kt` | 新增 | PlayerViewModel videoId 提取单测（T-08） |

---

## 依赖关系

```
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5 ──▶ Step 6 ──▶ Step 7 ──▶ Step 8 ──▶ Step 9
 Gradle     Core      Core     Domain     Data      Hilt      Present-  Navigation Feature
(Build)    Theme    Network   (Model    (DTO +    (DI)      ation    + Deep     Screens
           +Config  (Api*)    +RepoI+   RepoImpl)           (Home)    Links     +Detekt
                              UseCase)                                         +CLAUDE
```

- **Step 2** 依赖 Step 1：Theme 和 AppConfig 依赖 Gradle 构建系统（BuildConfig 由 app/build.gradle.kts 生成）
- **Step 3** 依赖 Step 2：ApiClient 依赖 BuildConfigWrapper（baseUrl）
- **Step 4** 依赖 Step 3：Repository 接口返回 `ApiResult<T>`，定义在 Core 网络层
- **Step 5** 依赖 Step 4：RepositoryImpl 实现 Domain 层接口，DataSource 依赖 ApiService（Step 3），DTO 依赖 Domain 层 Model（Step 4）
- **Step 6** 依赖 Step 3 + Step 5：NetworkModule 提供 ApiService（Step 3），RepositoryModule 绑定 DramaRepositoryImpl（Step 5）
- **Step 7** 依赖 Step 2 + Step 6：HomeScreen 使用 Theme（Step 2），HomeViewModel 依赖 Hilt（Step 6），MainActivity 需要 Application（Step 6）和 Theme（Step 2）
- **Step 8** 依赖 Step 7：NavGraph 引用 HomeScreen（Step 7），MainActivity 集成 NavController
- **Step 9** 依赖 Step 8：PlayerScreen/DramaDetailScreen 是 NavGraph（Step 8）的独立路由目标，Detekt 和 CLAUDE.md 是工程收尾

## 验证总览

- [x] **T-01 / T-05**：`./gradlew assembleDebug` 构建成功，APK 生成
- [x] **T-02**：`ApiResultTest` — sealed class 分支正确性
- [x] **T-03**：`DramaDtoTest` — DTO toDomain() 转换正确
- [x] **T-04**：`GetDramasUseCaseTest` — UseCase 委托 Repository 验证
- [x] **T-06**：`HomeViewModelTest` — UiState isLoading 状态流转
- [x] **T-07**：`RoutesTest` — 路由路径生成正确
- [x] **T-08**：`PlayerViewModelTest` — SavedStateHandle 提取参数
- [x] **T-09**：`./gradlew detekt` — 0 issues
- [x] **全量测试**：`./gradlew test` 所有测试通过

## 变更文件汇总

| 文件 | 操作 | 步骤 | 内容简介 |
|------|------|------|---------|
| `android/gradle/libs.versions.toml` | 新增 | Step 1 | Version Catalog，锁定所有依赖版本 |
| `android/build.gradle.kts` | 新增 | Step 1 | 根项目构建脚本 |
| `android/settings.gradle.kts` | 新增 | Step 1 | 项目设置，仓库配置 |
| `android/app/build.gradle.kts` | 新增 | Step 1 | app 模块构建脚本 |
| `android/gradle.properties` | 新增 | Step 1 | Gradle JVM 配置 |
| `android/gradle/wrapper/gradle-wrapper.properties` | 新增 | Step 1 | Gradle 8.9 wrapper |
| `android/app/proguard-rules.pro` | 新增 | Step 1 | ProGuard 规则骨架 |
| `android/.gitignore` | 新增 | Step 1 | Android 专用 gitignore |
| `android/app/src/main/java/com/djs66256/short_drama/core/theme/Color.kt` | 新增 | Step 2 | Material3 颜色 tokens |
| `android/app/src/main/java/com/djs66256/short_drama/core/theme/Type.kt` | 新增 | Step 2 | Typography 字体规格 |
| `android/app/src/main/java/com/djs66256/short_drama/core/theme/Theme.kt` | 新增 | Step 2 | ShortDramaTheme Composable |
| `android/app/src/main/java/com/djs66256/short_drama/core/config/AppConfig.kt` | 新增 | Step 2 | BuildConfigWrapper |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiResult.kt` | 新增 | Step 3 | sealed class 统一响应封装 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 新增 | Step 3 | Retrofit API 接口定义 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiClient.kt` | 新增 | Step 3 | OkHttpClient + Retrofit 构建 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt` | 新增 | Step 3 | Auth 拦截器骨架 |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/ApiResultTest.kt` | 新增 | Step 3 | ApiResult 单测（T-02） |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/Drama.kt` | 新增 | Step 4 | Drama 业务实体 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/Episode.kt` | 新增 | Step 4 | Episode 业务实体 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/DramaRepository.kt` | 新增 | Step 4 | Drama 仓库接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/EpisodeRepository.kt` | 新增 | Step 4 | Episode 仓库接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetDramasUseCase.kt` | 新增 | Step 4 | 获取短剧列表用例 |
| `android/app/src/test/java/com/djs66256/short_drama/domain/usecase/GetDramasUseCaseTest.kt` | 新增 | Step 4 | UseCase 单测（T-04） |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/DramaDto.kt` | 新增 | Step 5 | Drama API 响应模型 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/EpisodeDto.kt` | 新增 | Step 5 | Episode API 响应模型 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/PaginationDto.kt` | 新增 | Step 5 | 分页响应模型 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/ErrorDto.kt` | 新增 | Step 5 | 错误响应模型 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt` | 新增 | Step 5 | 远程数据源 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt` | 新增 | Step 5 | 仓库实现 |
| `android/app/src/test/java/com/djs66256/short_drama/data/dto/DramaDtoTest.kt` | 新增 | Step 5 | DTO 转换单测（T-03） |
| `android/app/src/main/java/com/djs66256/short_drama/ShortDramaApplication.kt` | 新增 | Step 6 | @HiltAndroidApp 入口 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt` | 新增 | Step 6 | Hilt AppModule |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/NetworkModule.kt` | 新增 | Step 6 | Hilt NetworkModule |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 新增 | Step 6 | Hilt RepositoryModule |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 新增 | Step 7 | 首页 Compose UI |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt` | 新增 | Step 7 | 首页 ViewModel |
| `android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt` | 新增 | Step 7 | 单 Activity 入口 |
| `android/app/src/main/AndroidManifest.xml` | 新增 | Step 7 | 应用清单（权限 + Activity + Deep Links） |
| `android/app/src/main/res/values/strings.xml` | 新增 | Step 7 | 字符串资源 |
| `android/app/src/main/res/values/themes.xml` | 新增 | Step 7 | XML 主题入口 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModelTest.kt` | 新增 | Step 7 | HomeViewModel 单测（T-06） |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 新增 | Step 8 | Compose Navigation 路由图 |
| `android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt` | 修改 | Step 8 | setContent 中集成 NavGraph + NavController |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 新增 | Step 8 | Routes 路径单测（T-07） |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt` | 新增 | Step 9 | 播放器占位 UI |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` | 新增 | Step 9 | 播放器 ViewModel |
| `android/app/src/main/java/com/djs66256/short_drama/feature/dramadetail/ui/DramaDetailScreen.kt` | 新增 | Step 9 | 剧集详情占位 UI |
| `android/app/src/main/java/com/djs66256/short_drama/feature/dramadetail/viewmodel/DramaDetailViewModel.kt` | 新增 | Step 9 | 详情 ViewModel |
| `android/.detekt/detekt.yml` | 新增 | Step 9 | Detekt 静态分析配置 |
| `android/CLAUDE.md` | 新增 | Step 9 | Android 端开发规范 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModelTest.kt` | 新增 | Step 9 | PlayerViewModel 单测（T-08） |
