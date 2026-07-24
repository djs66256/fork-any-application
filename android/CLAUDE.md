# Android 端开发规范

## 技术栈

- Kotlin 2.0.21 + Jetpack Compose + Material3
- Hilt (2.53.1) DI + KSP
- Retrofit 2.11.0 + OkHttp 4.12.0 + kotlinx.serialization 1.7.3
- Jetpack Navigation Compose 2.8.5
- Detekt 1.23.7 静态分析
- JUnit 4 + MockK + Turbine 测试

## 目录结构

```
app/src/
├── main/java/com/djs66256/short_drama/
│   ├── ShortDramaApplication.kt    # @HiltAndroidApp 入口
│   ├── MainActivity.kt             # 单 Activity + NavHost
│   ├── core/                       # 基础设施层
│   │   ├── config/                 # AppConfig 接口与 BuildConfig 实现
│   │   ├── di/                     # Hilt 模块（AppModule, NetworkModule, RepositoryModule）
│   │   ├── network/                # ApiResult, ApiService, ApiClient, AuthInterceptor
│   │   └── theme/                  # Color, Typography, ShortDramaTheme
│   ├── data/                       # 数据层
│   │   ├── datasource/             # DramaRemoteDataSource
│   │   ├── dto/                    # DramaDto, EpisodeDto, PaginationDto, ErrorDto
│   │   └── repository/             # DramaRepositoryImpl
│   ├── domain/                     # 领域层
│   │   ├── model/                  # Drama, Episode 业务实体
│   │   ├── repository/             # DramaRepository, EpisodeRepository 接口
│   │   └── usecase/                # GetDramasUseCase
│   ├── feature/                    # 功能模块
│   │   ├── home/                   # 首页
│   │   ├── player/                 # 播放器（占位）
│   │   └── dramadetail/            # 剧集详情（占位）
│   └── navigation/                 # Routes + NavGraph
└── test/                           # 单元测试（JVM，无 Android 依赖）
```

## 架构分层

```
Presentation (Composable + ViewModel)
    ↓ depends on
Domain (Model + Repository Interface + UseCase)
    ↑ implemented by
Data (DTO + DataSource + RepositoryImpl)
    ↓ depends on
Core (Network, DI, Theme, Config)
```

- **Domain 层**：纯 Kotlin，无 Android 依赖。Model、Repository 接口、UseCase 在此定义。
- **Data 层**：实现 Domain 层的 Repository 接口。DTO 使用 kotlinx.serialization 反序列化 API 响应。
- **Presentation 层**：Composable UI + ViewModel。使用 StateFlow 暴露状态，hiltViewModel() 注入。

## 构建命令

```bash
# 编译
./gradlew assembleDebug

# 运行所有单元测试
./gradlew test

# 运行指定测试
./gradlew test --tests "fully.qualified.TestClassName"

# 静态分析
./gradlew detekt
```

## 测试策略

- Domain 层用例：MockK mock Repository 接口，验证委托行为。
- DTO 转换：构造 DTO 实例，验证 toDomain() 所有字段正确映射。
- ViewModel：使用 MockK mock 依赖，Turbine 收集 StateFlow 验证状态流转。
- 所有测试位于 `src/test/` 下，纯 JVM 运行，不依赖 Android 框架。

## 编码约束

- **禁止硬编码**：URL、token、环境变量等通过 AppConfig / BuildConfig 获取。
- **禁止直接使用 BuildConfig**：通过 AppConfig 接口访问，保证可测试性。
- **Repository 模式**：Data 层实现 Domain 层接口，依赖方向 Data → Domain。
- **Sealed class 封装**：API 响应统一使用 ApiResult<T>（Success / Error / Exception）。
- **Kotlin 2.0+**：使用 kotlinx.serialization，不使用 Gson/Moshi。

## 代码风格

- 遵循 Kotlin 官方代码风格（kotlin.code.style=official）
- 缩进 4 空格
- 每行最大 120 字符
- Composable 函数命名以大写开头
- ViewModel 使用 @HiltViewModel + @Inject constructor
