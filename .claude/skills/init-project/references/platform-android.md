# Android 初始化规范

## 技术栈

| 组件 | 选型 |
|------|------|
| 语言 | Kotlin 2.0 |
| UI 框架 | Jetpack Compose + Material3 |
| 最低 SDK | minSdk 26 / compileSdk 36 |
| 构建工具 | AGP 8.x + Gradle (Kotlin DSL) |
| 依赖管理 | Version Catalog (`libs.versions.toml`) |
| 架构 | MVVM + Clean Architecture（Presentation → Domain → Data + Core） |
| DI | Hilt（推荐） |
| 网络 | Retrofit + OkHttp + kotlinx.serialization |
| Lint | Detekt |
| 测试 | JUnit + MockK + Turbine |

## 标准目录结构

```
android/app/src/main/java/<package>/
├── ShortDramaApplication.kt       # @HiltAndroidApp，初始化 DI
├── MainActivity.kt                # @AndroidEntryPoint，单 Activity + Compose Navigation
├── core/                          # Core 层
│   ├── network/
│   │   └── ApiClient.kt           # Retrofit + OkHttp 封装
│   ├── di/
│   │   ├── AppModule.kt           # Hilt DI 模块
│   │   ├── NetworkModule.kt
│   │   └── RepositoryModule.kt
│   ├── config/
│   │   └── AppConfig.kt           # BuildConfig 读取
│   ├── theme/
│   │   ├── Theme.kt               # Material3 主题
│   │   ├── Color.kt               # 颜色 tokens
│   │   └── Type.kt                # 字体 tokens
│   └── deeplink/
│       └── DeeplinkRouter.kt      # djsdrama:// → NavController 路由
├── domain/                        # Domain 层（纯 Kotlin，无 Android 依赖）
│   ├── model/
│   │   ├── Drama.kt               # @Serializable data class
│   │   └── Episode.kt
│   ├── usecase/
│   │   └── GetDramasUseCase.kt    # 骨架
│   └── repository/
│       ├── DramaRepository.kt     # Interface
│       └── EpisodeRepository.kt
├── data/                          # Data 层
│   ├── repository/
│   │   └── DramaRepositoryImpl.kt # 实现 Domain 层接口
│   ├── datasource/
│   │   └── DramaRemoteDataSource.kt
│   ├── dto/
│   │   ├── DramaDto.kt            # @Serializable API 模型 + toDomain()
│   │   └── EpisodeDto.kt
│   └── di/
│       └── DataModule.kt
└── feature/                       # Presentation 层 — 按业务域独立目录
    ├── home/
    │   ├── ui/
    │   │   └── HomeScreen.kt      # @Composable
    │   └── viewmodel/
    │       └── HomeViewModel.kt   # @HiltViewModel + StateFlow<HomeUiState>
    ├── player/                    # 骨架
    │   ├── ui/
    │   │   └── PlayerScreen.kt
    │   └── viewmodel/
    │       └── PlayerViewModel.kt
    └── dramadetail/               # 骨架
        ├── ui/
        │   └── DramaDetailScreen.kt
        └── viewmodel/
            └── DramaDetailViewModel.kt
```

## AndroidManifest.xml

- `LAUNCHER` Activity
- `djsdrama://` Deep Links intent-filter（引用 PRODUCT.md 中的 schema）
- `android:screenOrientation="portrait"` 竖屏

## Gradle 配置要点

- Version Catalog 管理 Compose BOM、Material3、Hilt、Retrofit、kotlinx.serialization
- Detekt 插件集成
- 提供阿里云 maven 镜像配置方案（READEM.md）

## 关键约束

- Domain 层纯 Kotlin（不含 `import android.*`），可 JVM 单测
- ViewModel 通过 `StateFlow<UiState>` 暴露状态，不暴露 MutableStateFlow
- Repository Interface 在 Domain 层，Impl 在 Data 层（依赖倒置）
- 单 Activity 架构，Compose Navigation 管理所有路由
- Deeplink 在 MainActivity 解析，分发到 NavController
- Hilt DI 注入所有依赖
