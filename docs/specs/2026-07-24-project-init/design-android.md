# Android 端技术方案：项目初始化与架构设计

> 创建日期：2026-07-24
> 对应需求：spec.md Section 4.5（Android 分层架构）、Section 6.4（US-04 Android 工程初始化）
> 依赖文档：design.md（共享 API 设计、数据模型、跨端共享逻辑）

## 1. 整体架构

### 1.1 架构分层

Android 端采用 **MVVM + Clean Architecture**，分为三层加一个 Core 基础设施层：

```
┌─────────────────────────────────────────────────────────────┐
│  Presentation 层 (UI + ViewModel)                            │
│  职责：Compose UI 渲染、用户交互、UI 状态管理                   │
│  约束：不包含业务逻辑；ViewModel 不依赖 Android View 层        │
│  feature/<name>/ui/          — Compose Screen               │
│  feature/<name>/viewmodel/   — ViewModel + UiState           │
├─────────────────────────────────────────────────────────────┤
│  Domain 层 (UseCase + Model + Repository Interface)          │
│  职责：业务规则、数据模型定义、仓库接口                         │
│  约束：纯 Kotlin/Java 模块，不含任何 Android 框架依赖          │
│  domain/model/        — data class (Drama, Episode)          │
│  domain/usecase/      — GetDramasUseCase 等                  │
│  domain/repository/   — Repository 接口 (interface)          │
├─────────────────────────────────────────────────────────────┤
│  Data 层 (Repository Impl + DataSource + DTO)                │
│  职责：网络请求、本地存储、DTO ↔ Model 转换                   │
│  约束：实现 Domain 层的 Repository 接口                       │
│  data/repository/     — DramaRepositoryImpl 等               │
│  data/datasource/     — DramaRemoteDataSource                │
│  data/dto/            — DramaDto, EpisodeDto                 │
├─────────────────────────────────────────────────────────────┤
│  Core 层 (跨层基础设施)                                        │
│  职责：网络 client、DI 容器、配置管理、Material3 主题          │
│  core/network/        — ApiClient, ApiService, Interceptor   │
│  core/di/             — AppModule, 各层 DI Module            │
│  core/config/         — AppConfig（版本号、BaseURL）         │
│  core/theme/          — Theme.kt, Color.kt, Type.kt          │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 依赖方向

```
Presentation 层  ──依赖──▶  Domain 层  ◀──依赖──  Data 层
       │                        │                      │
       └──────────依赖──────────┼──────────依赖──────────┘
                                │
                                ▼
                           Core 层
                         （全层可依赖）

注：Data 层实现 Domain 层接口（依赖倒置），
    Domain 层不依赖 Data 层具体实现。
```

### 1.3 跨端对齐

| 层面 | Android 实现 | 跨端共享内容 |
|------|-------------|-------------|
| 数据模型 | `domain/model/Drama.kt` | 字段名和类型与 Backend Zod Schema 一致，使用 `@SerializedName` 对齐 snake_case JSON key |
| API 契约 | `ApiService` 接口与 `design.md` 中 7 个 API 端点对应 | 请求/响应格式、错误格式、分页结构均与 Backend 一致 |
| 错误处理 | `core/network/ApiResult.kt` 封装 `ApiError` sealed class | 标准错误码 `NOT_FOUND`、`VALIDATION_ERROR`、`INTERNAL_ERROR`、`NOT_IMPLEMENTED` |
| URL Scheme | Deep Links `djsdrama://` 在 AndroidManifest.xml 声明 | 与 iOS URL Scheme 统一 |
| 分页 | `Pagination` data class | `{ page, pageSize, total, totalPages }` 结构 |

---

## 2. 模块与目录结构

### 2.1 完整文件清单

```
android/
├── .detekt/
│   └── detekt.yml                          # Detekt 静态分析配置
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml         # LAUNCHER Activity + Deep Links
│   │   │   ├── java/com/djs66256/short_drama/
│   │   │   │   ├── ShortDramaApplication.kt         # Application 入口，Hilt 初始化
│   │   │   │   ├── MainActivity.kt                  # 单 Activity，Deeplink 入口
│   │   │   │   ├── navigation/
│   │   │   │   │   └── NavGraph.kt                  # Compose Navigation 路由图
│   │   │   │   ├── core/
│   │   │   │   │   ├── network/
│   │   │   │   │   │   ├── ApiClient.kt             # OkHttpClient + Retrofit 构建
│   │   │   │   │   │   ├── ApiService.kt            # Retrofit API 接口定义
│   │   │   │   │   │   ├── AuthInterceptor.kt       # Auth Token 拦截器（骨架）
│   │   │   │   │   │   └── ApiResult.kt             # 统一网络响应封装
│   │   │   │   │   ├── di/
│   │   │   │   │   │   ├── AppModule.kt             # Hilt DI 顶层模块
│   │   │   │   │   │   ├── NetworkModule.kt         # 网络层 DI 模块
│   │   │   │   │   │   └── RepositoryModule.kt      # Repository 层 DI 模块
│   │   │   │   │   ├── config/
│   │   │   │   │   │   └── AppConfig.kt             # 应用配置（版本号、BaseURL）
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Theme.kt                 # Material3 主题
│   │   │   │   │       ├── Color.kt                 # 颜色 tokens
│   │   │   │   │       └── Type.kt                  # 字体 tokens
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── Drama.kt                 # 短剧实体 data class
│   │   │   │   │   │   └── Episode.kt               # 剧集实体 data class
│   │   │   │   │   ├── usecase/
│   │   │   │   │   │   └── GetDramasUseCase.kt      # 获取短剧列表用例（骨架）
│   │   │   │   │   └── repository/
│   │   │   │   │       ├── DramaRepository.kt       # 短剧仓库接口
│   │   │   │   │       └── EpisodeRepository.kt     # 剧集仓库接口
│   │   │   │   ├── data/
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   └── DramaRepositoryImpl.kt   # 短剧仓库实现
│   │   │   │   │   ├── datasource/
│   │   │   │   │   │   └── DramaRemoteDataSource.kt # 短剧远程数据源
│   │   │   │   │   └── dto/
│   │   │   │   │       ├── DramaDto.kt              # 短剧 API 响应模型
│   │   │   │   │       ├── EpisodeDto.kt            # 剧集 API 响应模型
│   │   │   │   │       ├── PaginationDto.kt         # 分页响应模型
│   │   │   │   │       └── ErrorDto.kt              # 错误响应模型
│   │   │   │   └── feature/
│   │   │   │       ├── home/
│   │   │   │       │   ├── ui/
│   │   │   │       │   │   └── HomeScreen.kt        # 首页 Compose UI（App Shell）
│   │   │   │       │   └── viewmodel/
│   │   │   │       │       └── HomeViewModel.kt     # 首页 ViewModel（含 UiState）
│   │   │   │       ├── player/
│   │   │   │       │   ├── ui/
│   │   │   │       │   │   └── PlayerScreen.kt      # 播放器占位 UI
│   │   │   │       │   └── viewmodel/
│   │   │   │       │       └── PlayerViewModel.kt   # 播放器 ViewModel（骨架）
│   │   │   │       └── dramadetail/
│   │   │   │           ├── ui/
│   │   │   │           │   └── DramaDetailScreen.kt # 剧集详情占位 UI
│   │   │   │           └── viewmodel/
│   │   │   │               └── DramaDetailViewModel.kt # 详情 ViewModel（骨架）
│   │   │   └── res/
│   │   │       ├── values/
│   │   │       │   ├── strings.xml                  # 字符串资源
│   │   │       │   └── themes.xml                   # XML 主题入口（指向 Compose Theme）
│   │   │       └── mipmap-*/                        # 应用图标
│   │   ├── test/java/com/djs66256/short_drama/
│   │   │   └── feature/home/viewmodel/
│   │   │       └── HomeViewModelTest.kt             # HomeViewModel 单元测试
│   │   └── androidTest/java/com/djs66256/short_drama/
│   │       └── ExampleInstrumentedTest.kt           # 仪器化测试占位
│   ├── build.gradle.kts                             # app 模块构建脚本
│   └── proguard-rules.pro                           # ProGuard 混淆规则
├── build.gradle.kts                                 # 根项目构建脚本
├── settings.gradle.kts                              # 项目设置
└── gradle/
    └── libs.versions.toml                           # Version Catalog（依赖版本管理）
```

### 2.2 包命名规范

所有源文件位于包 `com.djs66256.short_drama` 下，按架构分层组织子包：

| 层 | 包路径 | 说明 |
|----|--------|------|
| Core 网络 | `com.djs66256.short_drama.core.network` | Retrofit + OkHttp 封装 |
| Core 依赖注入 | `com.djs66256.short_drama.core.di` | Hilt Module 定义 |
| Core 配置 | `com.djs66256.short_drama.core.config` | BuildConfig、环境配置 |
| Core 主题 | `com.djs66256.short_drama.core.theme` | Material3 Color/Type/Theme |
| Domain 模型 | `com.djs66256.short_drama.domain.model` | 业务实体 data class |
| Domain 用例 | `com.djs66256.short_drama.domain.usecase` | UseCase 业务逻辑 |
| Domain 仓库 | `com.djs66256.short_drama.domain.repository` | Repository 接口 |
| Data 仓库 | `com.djs66256.short_drama.data.repository` | Repository 实现 |
| Data 数据源 | `com.djs66256.short_drama.data.datasource` | Remote DataSource |
| Data DTO | `com.djs66256.short_drama.data.dto` | API 响应 DTO |
| Feature Home | `com.djs66256.short_drama.feature.home.{ui,viewmodel}` | 首页 |
| Feature Player | `com.djs66256.short_drama.feature.player.{ui,viewmodel}` | 播放器 |
| Feature DramaDetail | `com.djs66256.short_drama.feature.dramadetail.{ui,viewmodel}` | 剧集详情 |

---

## 3. 数据模型

### 3.1 Drama（domain/model/Drama.kt）

与 Backend Zod `DramaSchema` 字段一一对应：

```kotlin
package com.djs66256.short_drama.domain.model

data class Drama(
    val id: String,
    val title: String,
    val description: String,
    val coverUrl: String,
    val category: String,
    val episodeCount: Int,
    val tags: List<String> = emptyList(),
    val rating: Double? = null,
    val createdAt: String,
    val updatedAt: String,
)
```

| 字段 | Kotlin 类型 | Zod 类型 | 说明 |
|------|-----------|---------|------|
| id | String | z.string() | 唯一标识 |
| title | String | z.string().min(1) | 标题 |
| description | String | z.string() | 描述 |
| coverUrl | String | z.string().url() | 封面图 URL |
| category | String | z.string() | 分类 |
| episodeCount | Int | z.number().int().positive() | 剧集数 |
| tags | List\<String\> | z.array(z.string()).optional() | 标签列表（可选，默认空） |
| rating | Double? | z.number().min(0).max(10).optional() | 评分（可选） |
| createdAt | String | z.string().datetime() | 创建时间 |
| updatedAt | String | z.string().datetime() | 更新时间 |

### 3.2 Episode（domain/model/Episode.kt）

```kotlin
package com.djs66256.short_drama.domain.model

data class Episode(
    val id: String,
    val dramaId: String,
    val title: String,
    val episodeNumber: Int,
    val videoUrl: String,
    val duration: Int,  // 秒
    val thumbnailUrl: String,
    val createdAt: String,
    val updatedAt: String,
)
```

### 3.3 数据关系

```
Drama ──1:N──▶ Episode
  │
  └──category: String（分类标签）
```

---

## 4. DTO 层与映射

### 4.1 DTO 设计原则

- DTO 位于 `data/dto/` 包，使用 `kotlinx.serialization` 注解
- 所有字段使用 `@SerialName` 注解映射 snake_case JSON key
- DTO 提供扩展函数 `.toDomain()` 转换为 Domain Model
- Model 提供扩展函数 `.toDto()` 转换回 DTO（用于 POST/PUT 请求）

### 4.2 DramaDto（data/dto/DramaDto.kt）

```kotlin
package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.Drama
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DramaDto(
    val id: String,
    val title: String,
    val description: String,
    @SerialName("cover_url") val coverUrl: String,
    val category: String,
    @SerialName("episode_count") val episodeCount: Int,
    val tags: List<String> = emptyList(),
    val rating: Double? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
) {
    fun toDomain(): Drama = Drama(
        id = id,
        title = title,
        description = description,
        coverUrl = coverUrl,
        category = category,
        episodeCount = episodeCount,
        tags = tags,
        rating = rating,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
```

### 4.3 EpisodeDto（data/dto/EpisodeDto.kt）

```kotlin
package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.Episode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EpisodeDto(
    val id: String,
    @SerialName("drama_id") val dramaId: String,
    val title: String,
    @SerialName("episode_number") val episodeNumber: Int,
    @SerialName("video_url") val videoUrl: String,
    val duration: Int,
    @SerialName("thumbnail_url") val thumbnailUrl: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
) {
    fun toDomain(): Episode = Episode(
        id = id,
        dramaId = dramaId,
        title = title,
        episodeNumber = episodeNumber,
        videoUrl = videoUrl,
        duration = duration,
        thumbnailUrl = thumbnailUrl,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
```

### 4.4 PaginationDto（data/dto/PaginationDto.kt）

```kotlin
package com.djs66256.short_drama.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaginationDto(
    val page: Int,
    @SerialName("page_size") val pageSize: Int,
    val total: Int,
    @SerialName("total_pages") val totalPages: Int,
)
```

### 4.5 ErrorDto（data/dto/ErrorDto.kt）

对应 design.md 中的统一错误响应格式：

```kotlin
package com.djs66256.short_drama.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorDto(
    val error: ErrorDetail,
)

@Serializable
data class ErrorDetail(
    val code: String,
    val message: String,
)

@Serializable
data class DramaListResponseDto(
    val data: List<DramaDto>,
    val pagination: PaginationDto,
)
```

---

## 5. API 设计

### 5.1 ApiService 接口（core/network/ApiService.kt）

与 design.md 定义的 7 个 API 端点一一对应：

```kotlin
package com.djs66256.short_drama.core.network

import com.djs66256.short_drama.data.dto.DramaDto
import com.djs66256.short_drama.data.dto.DramaListResponseDto
import com.djs66256.short_drama.data.dto.ErrorDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // GET /api/health — 健康检查
    @GET("api/health")
    suspend fun health(): Response<Map<String, Any>>

    // GET /api/dramas — 获取短剧列表
    @GET("api/dramas")
    suspend fun getDramas(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): Response<DramaListResponseDto>

    // POST /api/dramas — 创建短剧（骨架，返回 501）
    @POST("api/dramas")
    suspend fun createDrama(@Body body: Map<String, String>): Response<ErrorDto>

    // GET /api/dramas/{id} — 获取短剧详情（骨架，返回 501）
    @GET("api/dramas/{id}")
    suspend fun getDramaDetail(@Path("id") id: String): Response<DramaDto>

    // GET /api/episodes/{id} — 获取剧集详情（骨架，返回 501）
    @GET("api/episodes/{id}")
    suspend fun getEpisodeDetail(@Path("id") id: String): Response<DramaDto>

    // POST /api/player/start — 开始播放（骨架，返回 501）
    @POST("api/player/start")
    suspend fun startPlayer(@Body body: Map<String, Any?>): Response<ErrorDto>

    // POST /api/player/stop — 停止播放（骨架，返回 501）
    @POST("api/player/stop")
    suspend fun stopPlayer(@Body body: Map<String, Any>): Response<ErrorDto>
}
```

### 5.2 统一响应封装（core/network/ApiResult.kt）

```kotlin
package com.djs66256.short_drama.core.network

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: String, val message: String) : ApiResult<Nothing>()
    data class Exception(val throwable: Throwable) : ApiResult<Nothing>()
}
```

### 5.3 ApiClient（core/network/ApiClient.kt）

```kotlin
package com.djs66256.short_drama.core.network

import com.djs66256.short_drama.core.config.BuildConfigWrapper
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfigWrapper.isDebug) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfigWrapper.apiBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
```

### 5.4 AuthInterceptor（core/network/AuthInterceptor.kt）

当前阶段为骨架实现，不处理认证：

```kotlin
package com.djs66256.short_drama.core.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            // TODO: 后续 PRD 实现 JWT Token 注入
            // request.addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}
```

---

## 6. Data 层设计

### 6.1 Repository 接口（Domain 层）

#### DramaRepository（domain/repository/DramaRepository.kt）

```kotlin
package com.djs66256.short_drama.domain.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.Drama

interface DramaRepository {
    suspend fun getDramas(page: Int = 1, pageSize: Int = 20): ApiResult<List<Drama>>
    suspend fun getDramaDetail(id: String): ApiResult<Drama>
}
```

#### EpisodeRepository（domain/repository/EpisodeRepository.kt）

```kotlin
package com.djs66256.short_drama.domain.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.Episode

interface EpisodeRepository {
    suspend fun getEpisodeDetail(id: String): ApiResult<Episode>
}
```

### 6.2 Repository 实现（Data 层）

#### DramaRepositoryImpl（data/repository/DramaRepositoryImpl.kt）

```kotlin
package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.data.datasource.DramaRemoteDataSource
import com.djs66256.short_drama.domain.model.Drama
import com.djs66256.short_drama.domain.repository.DramaRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DramaRepositoryImpl @Inject constructor(
    private val remoteDataSource: DramaRemoteDataSource,
) : DramaRepository {

    override suspend fun getDramas(page: Int, pageSize: Int): ApiResult<List<Drama>> {
        return remoteDataSource.getDramas(page, pageSize)
    }

    override suspend fun getDramaDetail(id: String): ApiResult<Drama> {
        return remoteDataSource.getDramaDetail(id)
    }
}
```

### 6.3 RemoteDataSource（data/datasource/DramaRemoteDataSource.kt）

```kotlin
package com.djs66256.short_drama.data.datasource

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.network.ApiService
import com.djs66256.short_drama.domain.model.Drama
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DramaRemoteDataSource @Inject constructor(
    private val apiService: ApiService,
) {

    suspend fun getDramas(page: Int, pageSize: Int): ApiResult<List<Drama>> {
        return try {
            val response = apiService.getDramas(page, pageSize)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    ApiResult.Success(body.data.map { it.toDomain() })
                } else {
                    ApiResult.Error("EMPTY_BODY", "Response body is null")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                // TODO: 解析 ErrorDto 提取 code 和 message
                ApiResult.Error(
                    code = "HTTP_${response.code()}",
                    message = errorBody ?: "Unknown error",
                )
            }
        } catch (e: Exception) {
            ApiResult.Exception(e)
        }
    }

    suspend fun getDramaDetail(id: String): ApiResult<Drama> {
        return try {
            val response = apiService.getDramaDetail(id)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    ApiResult.Success(body.toDomain())
                } else {
                    ApiResult.Error("EMPTY_BODY", "Response body is null")
                }
            } else {
                ApiResult.Error(
                    code = "HTTP_${response.code()}",
                    message = "Request failed with status ${response.code()}",
                )
            }
        } catch (e: Exception) {
            ApiResult.Exception(e)
        }
    }
}
```

---

## 7. Domain 层设计

### 7.1 GetDramasUseCase（domain/usecase/GetDramasUseCase.kt）

当前阶段为骨架实现，后续 PRD 填充业务逻辑：

```kotlin
package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.Drama
import com.djs66256.short_drama.domain.repository.DramaRepository
import javax.inject.Inject

class GetDramasUseCase @Inject constructor(
    private val dramaRepository: DramaRepository,
) {
    suspend operator fun invoke(page: Int = 1, pageSize: Int = 20): ApiResult<List<Drama>> {
        // TODO: 后续 PRD 添加业务逻辑（缓存策略、过滤、排序等）
        return dramaRepository.getDramas(page, pageSize)
    }
}
```

---

## 8. Presentation 层设计

### 8.1 HomeViewModel（feature/home/viewmodel/HomeViewModel.kt）

```kotlin
package com.djs66256.short_drama.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import com.djs66256.short_drama.core.config.BuildConfigWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val appName: String = BuildConfigWrapper.appName,
    val appVersion: String = BuildConfigWrapper.appVersion,
)

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // 初始化完成后标记加载完成
        _uiState.update { it.copy(isLoading = false) }
    }
}
```

**UiState 字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| isLoading | Boolean | 首页加载状态，初始为 true，初始化完成后为 false |
| appName | String | 应用显示名称（来自 PRODUCT.md：ShortDrama） |
| appVersion | String | 应用版本号（0.1.0） |

### 8.2 HomeScreen（feature/home/ui/HomeScreen.kt）

```kotlin
package com.djs66256.short_drama.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = uiState.appName,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "v${uiState.appVersion}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

### 8.3 PlayerScreen（feature/player/ui/PlayerScreen.kt）

占位 UI，后续 PRD 实现完整播放器：

```kotlin
package com.djs66256.short_drama.feature.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlayerScreen(videoId: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "播放器",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Video ID: $videoId",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

### 8.4 PlayerViewModel（feature/player/viewmodel/PlayerViewModel.kt）

```kotlin
package com.djs66256.short_drama.feature.player.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val videoId: String = savedStateHandle.get<String>("videoId") ?: ""
}
```

### 8.5 DramaDetailScreen（feature/dramadetail/ui/DramaDetailScreen.kt）

```kotlin
package com.djs66256.short_drama.feature.dramadetail.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DramaDetailScreen(dramaId: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "剧集详情",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Drama ID: $dramaId",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

### 8.6 DramaDetailViewModel（feature/dramadetail/viewmodel/DramaDetailViewModel.kt）

```kotlin
package com.djs66256.short_drama.feature.dramadetail.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DramaDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val dramaId: String = savedStateHandle.get<String>("dramaId") ?: ""
}
```

---

## 9. Navigation 路由

### 9.1 路由定义

使用 Jetpack Navigation Compose，路由定义在 `navigation/NavGraph.kt`：

```kotlin
package com.djs66256.short_drama.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.djs66256.short_drama.feature.dramadetail.ui.DramaDetailScreen
import com.djs66256.short_drama.feature.home.ui.HomeScreen
import com.djs66256.short_drama.feature.player.ui.PlayerScreen

object Routes {
    const val HOME = "home"
    const val PLAYER = "player/{videoId}"
    const val DRAMA_DETAIL = "dramaDetail/{dramaId}"

    fun player(videoId: String) = "player/$videoId"
    fun dramaDetail(dramaId: String) = "dramaDetail/$dramaId"
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        // 首页
        composable(
            route = Routes.HOME,
            deepLinks = listOf(
                navDeepLink { uriPattern = "djsdrama://open" },
            ),
        ) {
            HomeScreen()
        }

        // 播放页
        composable(
            route = Routes.PLAYER,
            arguments = listOf(
                navArgument("videoId") { type = NavType.StringType },
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "djsdrama://player/{videoId}" },
            ),
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
            PlayerScreen(videoId = videoId)
        }

        // 剧集详情页
        composable(
            route = Routes.DRAMA_DETAIL,
            arguments = listOf(
                navArgument("dramaId") { type = NavType.StringType },
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "djsdrama://drama/{dramaId}" },
            ),
        ) { backStackEntry ->
            val dramaId = backStackEntry.arguments?.getString("dramaId") ?: ""
            DramaDetailScreen(dramaId = dramaId)
        }
    }
}
```

### 9.2 路由表

| 路由 key | 路径模式 | 参数 | Deeplink URI | 页面 |
|---------|---------|------|--------------|------|
| home | `home` | — | `djsdrama://open` | HomeScreen |
| player | `player/{videoId}` | videoId: String | `djsdrama://player/{videoId}` | PlayerScreen |
| dramaDetail | `dramaDetail/{dramaId}` | dramaId: String | `djsdrama://drama/{dramaId}` | DramaDetailScreen |

### 9.3 Deeplink 处理流程

```
外部 URL: djsdrama://player/123
       │
       ▼
AndroidManifest.xml intent-filter 匹配
       │
       ▼
MainActivity.onCreate() / onNewIntent()
       │
       ▼
intent.data → NavController.handleDeepLink(intent)
       │
       ▼
NavGraph 路由分发 → PlayerScreen(videoId="123")
```

**MainActivity.kt 中 Deeplink 处理**：

```kotlin
package com.djs66256.short_drama

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.djs66256.short_drama.core.theme.ShortDramaTheme
import com.djs66256.short_drama.navigation.NavGraph
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShortDramaTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        // Deeplink 由 Compose Navigation 的 navDeepLink DSL 自动处理
        // 此处保留入口用于后续自定义路由分发逻辑
    }
}
```

---

## 10. 网络层设计

### 10.1 技术栈

| 组件 | 选型 | 用途 |
|------|------|------|
| Retrofit | 2.x | HTTP 客户端，类型安全的 API 定义 |
| OkHttp | 4.x | 底层 HTTP 引擎，拦截器链 |
| kotlinx.serialization | 1.7+ | JSON 序列化/反序列化，与 Retrofit Converter 集成 |
| OkHttp Logging Interceptor | 4.x | Debug 模式下输出请求/响应日志 |

### 10.2 超时与重试策略

| 超时类型 | 默认值 | 说明 |
|---------|--------|------|
| connectTimeout | 30s | 建立 TCP 连接超时 |
| readTimeout | 30s | 读取响应数据超时 |
| writeTimeout | 30s | 写入请求数据超时 |
| 重试策略 | 无自动重试 | 当前阶段由上层调用方按需处理，后续 PRD 按 API 粒度添加重试策略 |

### 10.3 BuildConfig 配置管理

```kotlin
package com.djs66256.short_drama.core.config

object BuildConfigWrapper {
    // 由 app/build.gradle.kts 中的 BuildConfig 生成
    val isDebug: Boolean
        get() = com.djs66256.short_drama.BuildConfig.DEBUG

    val apiBaseUrl: String
        get() = com.djs66256.short_drama.BuildConfig.API_BASE_URL

    val appName: String
        get() = com.djs66256.short_drama.BuildConfig.APP_NAME

    val appVersion: String
        get() = com.djs66256.short_drama.BuildConfig.APP_VERSION
}
```

app/build.gradle.kts 中 BuildConfig 配置：

```kotlin
android {
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:3001/\"")
            buildConfigField("String", "APP_NAME", "\"ShortDrama\"")
            buildConfigField("String", "APP_VERSION", "\"0.1.0\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://api.djsdrama.com/\"")
            buildConfigField("String", "APP_NAME", "\"ShortDrama\"")
            buildConfigField("String", "APP_VERSION", "\"0.1.0\"")
        }
    }
}
```

> 注意：`10.0.2.2` 是 Android 模拟器访问宿主机 localhost 的特殊地址。

### 10.4 AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- 网络权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:name=".ShortDramaApplication"
        android:allowBackup="true"
        android:label="${appName}"
        android:supportsRtl="true"
        android:theme="@style/Theme.ShortDrama">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.ShortDrama"
            android:launchMode="singleTask">

            <!-- LAUNCHER Activity -->
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>

            <!-- djsdrama:// Deep Links -->
            <intent-filter android:autoVerify="true">
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="djsdrama" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

**Deeplink URI 格式**：

| URI | 路由目标 | 说明 |
|-----|---------|------|
| `djsdrama://open` | home | 打开首页 |
| `djsdrama://player/{videoId}` | player/{videoId} | 打开播放页 |
| `djsdrama://drama/{dramaId}` | dramaDetail/{dramaId} | 打开剧集详情 |
| `djsdrama://*` | home | 未匹配的 URI 回退到首页 |

---

## 11. DI 框架

### 11.1 选型：Hilt

选择 Hilt（基于 Dagger）作为 DI 框架，原因：
- Google 官方推荐，与 Jetpack 生态（ViewModel、Navigation Compose）深度集成
- 编译期依赖注入，性能优于运行时反射方案
- `@HiltViewModel` 注解与 ViewModel 无缝结合
- 单 Activity 架构中 Application 级别组件天然适配

### 11.2 ShortDramaApplication.kt

```kotlin
package com.djs66256.short_drama

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ShortDramaApplication : Application()
```

### 11.3 AppModule（core/di/AppModule.kt）

```kotlin
package com.djs66256.short_drama.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // 全局单例绑定在此定义
    // 当前阶段：无额外全局依赖需要绑定
    // 后续 PRD 在此添加：DataStore、Analytics、FeatureFlag 等
}
```

### 11.4 NetworkModule（core/di/NetworkModule.kt）

```kotlin
package com.djs66256.short_drama.core.di

import com.djs66256.short_drama.core.network.ApiClient
import com.djs66256.short_drama.core.network.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideApiService(): ApiService {
        return ApiClient.apiService
    }
}
```

### 11.5 RepositoryModule（core/di/RepositoryModule.kt）

```kotlin
package com.djs66256.short_drama.core.di

import com.djs66256.short_drama.data.datasource.DramaRemoteDataSource
import com.djs66256.short_drama.data.repository.DramaRepositoryImpl
import com.djs66256.short_drama.domain.repository.DramaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @javax.inject.Singleton
    fun provideDramaRepository(
        dataSource: DramaRemoteDataSource,
    ): DramaRepository {
        return DramaRepositoryImpl(remoteDataSource = dataSource)
    }
}
```

### 11.6 依赖注入关系图

```
Hilt SingletonComponent
│
├── NetworkModule
│   └── provideApiService() → ApiService (Singleton)
│
├── RepositoryModule
│   ├── DramaRemoteDataSource ← (injected) ApiService
│   └── provideDramaRepository() → DramaRepository (Singleton)
│       绑定到 DramaRepositoryImpl
│
├── AppModule
│   └── （后续添加 DataStore / Analytics 等）
│
└── ViewModel (通过 @HiltViewModel 自动注入)
    ├── HomeViewModel ← （当前无外部依赖）
    ├── PlayerViewModel ← SavedStateHandle
    └── DramaDetailViewModel ← SavedStateHandle
```

---

## 12. 数据持久化

### 12.1 当前策略

本次初始化阶段无数据持久化需求（spec.md 范围外）。但预留 DataStore Preferences 基础设施用于后续 PRD 存储简单配置：

```kotlin
// data/datasource/PreferencesDataSource.kt（骨架，后续 PRD 启用）

package com.djs66256.short_drama.data.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// 后续 PRD 启用此 DataStore 声明
// private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // TODO: 后续 PRD 实现具体配置读写
    // suspend fun getString(key: Preferences.Key<String>): String?
    // suspend fun putString(key: Preferences.Key<String>, value: String)
}
```

---

## 13. Material3 主题

### 13.1 Color.kt（core/theme/Color.kt）

```kotlin
package com.djs66256.short_drama.core.theme

import androidx.compose.ui.graphics.Color

// Primary
val Primary = Color(0xFF6750A4)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFEADDFF)
val OnPrimaryContainer = Color(0xFF21005D)

// Secondary
val Secondary = Color(0xFF625B71)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFE8DEF8)
val OnSecondaryContainer = Color(0xFF1D192B)

// Tertiary
val Tertiary = Color(0xFF7D5260)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFFFD8E4)
val OnTertiaryContainer = Color(0xFF31111D)

// Error
val Error = Color(0xFFB3261E)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFF9DEDC)
val OnErrorContainer = Color(0xFF410E0B)

// Background / Surface
val Background = Color(0xFFFFFBFE)
val OnBackground = Color(0xFF1C1B1F)
val Surface = Color(0xFFFFFBFE)
val OnSurface = Color(0xFF1C1B1F)
val SurfaceVariant = Color(0xFFE7E0EC)
val OnSurfaceVariant = Color(0xFF49454F)
```

### 13.2 Type.kt（core/theme/Type.kt）

```kotlin
package com.djs66256.short_drama.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)
```

### 13.3 Theme.kt（core/theme/Theme.kt）

```kotlin
package com.djs66256.short_drama.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
)

@Composable
fun ShortDramaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
```

---

## 14. 构建配置

### 14.1 Version Catalog（gradle/libs.versions.toml）

```toml
[versions]
agp = "8.7.0"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.28"
compose-bom = "2024.12.01"
compose-compiler = "1.5.15"
activity-compose = "1.9.3"
lifecycle = "2.8.7"
navigation-compose = "2.8.5"
hilt = "2.53.1"
hilt-navigation-compose = "1.2.0"
retrofit = "2.11.0"
okhttp = "4.12.0"
kotlinx-serialization = "1.7.3"
kotlinx-serialization-converter = "1.0.0"
datastore-preferences = "1.1.1"
detekt = "1.23.7"
junit = "4.13.2"
junit-ext = "1.2.1"
espresso = "3.6.1"
mockk = "1.13.13"
turbine = "1.2.0"
coroutines-test = "1.9.0"
core-ktx = "1.15.0"

[libraries]
# Compose BOM
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }

# Activity
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity-compose" }

# Lifecycle
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }

# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }

# Network
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
retrofit-kotlinx-serialization = { group = "com.jakewharton.retrofit", name = "retrofit2-kotlinx-serialization-converter", version.ref = "kotlinx-serialization-converter" }

# DataStore
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore-preferences" }

# Core
core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "core-ktx" }

# Test
junit = { group = "junit", name = "junit", version.ref = "junit" }
junit-ext = { group = "androidx.test.ext", name = "junit", version.ref = "junit-ext" }
espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines-test" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
```

### 14.2 根 build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt) apply false
}
```

### 14.3 settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // 阿里云镜像（国内加速）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}
dependencyResolution {
    repositories {
        google()
        mavenCentral()
        // 阿里云镜像（国内加速）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "ShortDrama"
include(":app")
```

### 14.4 app/build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.djs66256.short_drama"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.djs66256.short_drama"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AndroidManifest 中 ${appName} 占位符
        manifestPlaceholders["appName"] = "ShortDrama"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:3001/\"")
            buildConfigField("String", "APP_NAME", "\"ShortDrama\"")
            buildConfigField("String", "APP_VERSION", "\"0.1.0\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("String", "API_BASE_URL", "\"https://api.djsdrama.com/\"")
            buildConfigField("String", "APP_NAME", "\"ShortDrama\"")
            buildConfigField("String", "APP_VERSION", "\"0.1.0\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }
}

detekt {
    config.setFrom(file("$projectDir/.detekt/detekt.yml"))
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // Activity
    implementation(libs.activity.compose)

    // Lifecycle
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Network
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.kotlinx.serialization)

    // DataStore
    implementation(libs.datastore.preferences)

    // Core
    implementation(libs.core.ktx)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso.core)
}
```

---

## 15. Detekt 配置

### 15.1 .detekt/detekt.yml

基于 detekt 默认配置，仅覆盖关键规则：

```yaml
build:
  maxIssues: 0

style:
  active: true
  MagicNumber:
    active: true
    ignoreNumbers: ['-1', '0', '1', '2', '24', '80', '8', '16']
  WildcardImport:
    active: true
  UnusedImports:
    active: true
  MaxLineLength:
    active: true
    maxLineLength: 120

naming:
  active: true
  FunctionNaming:
    active: true
    ignoreAnnotated: ['Composable']

complexity:
  active: true
  LongParameterList:
    active: true
    functionThreshold: 6

exceptions:
  active: true
  TooGenericExceptionCaught:
    active: false

performance:
  active: true
  SpreadOperator:
    active: false

comments:
  active: true
  EndOfSentenceFormat:
    active: false
  UndocumentedPublicClass:
    active: false
  UndocumentedPublicFunction:
    active: false
```

---

## 16. 测试策略

### 16.1 测试分层

| 层级 | 工具 | 覆盖范围 | 数量 |
|------|------|---------|------|
| 单元测试 | JUnit 4 + MockK + Turbine | ViewModel、UseCase、Repository（Mock 依赖） | 1 个 HomeViewModel 测试骨架 |
| 仪器化测试 | Espresso | 当前无（后续 PRD 添加 UI 测试） | 1 个占位文件 |

### 16.2 HomeViewModel 单元测试骨架

```kotlin
package com.djs66256.short_drama.feature.home.viewmodel

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = kotlinx.coroutines.test.MainCoroutineRule()

    @Test
    fun `initial state has loading true then becomes false`() = runTest {
        val viewModel = HomeViewModel()

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isTrue()
            assertThat(initialState.appName).isEqualTo("ShortDrama")
            assertThat(initialState.appVersion).isEqualTo("0.1.0")

            val finalState = awaitItem()
            assertThat(finalState.isLoading).isFalse()
        }
    }
}
```

测试用 `MainCoroutineRule`（由 `kotlinx-coroutines-test` 提供）：

```kotlin
// 使用 kotlinx-coroutines-test 内置规则
// kotlinx.coroutines.test.MainCoroutineRule 或手动实现
```

### 16.3 测试策略说明

| 策略 | 说明 |
|------|------|
| Domain 层优先测试 | Domain 层纯 Kotlin，无 Android 框架依赖，可 JVM 直接运行，应优先覆盖 |
| Repository 通过 Mock 测试 | 单元测试中 mock `DramaRepository` 接口，测试 UseCase 逻辑 |
| ViewModel 通过 StateFlow 测试 | 使用 Turbine 收集 StateFlow 事件序列，验证状态转换 |
| 不测试框架代码 | Compose UI、Retrofit、Hilt 等框架代码由端到端测试覆盖（后续 PRD） |

---

## 17. 配置与环境

### 17.1 Gradle 配置（gradle.properties）

```properties
# Project-wide Gradle settings
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

# Android settings
android.useAndroidX=true
android.nonTransitiveRClass=true

# Kotlin
kotlin.code.style=official
```

### 17.2 gradle-wrapper.properties

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

### 17.3 .gitignore（Android 部分）

```
# Android
*.apk
*.aab
*.dex
*.class
*.iml
.idea/
.gradle/
local.properties
build/
*/build/
captures/
.externalNativeBuild/
.cxx/
*.jks
*.keystore
release-keystore.properties
```

### 17.4 环境变量映射

| BuildConfig 字段 | Debug 值 | Release 值 | 来源 |
|-----------------|---------|------------|------|
| API_BASE_URL | `http://10.0.2.2:3001/` | `https://api.djsdrama.com/` | app/build.gradle.kts buildTypes |
| APP_NAME | `ShortDrama` | `ShortDrama` | PRODUCT.md |
| APP_VERSION | `0.1.0` | `0.1.0` | spec.md |

> 说明：Debug 环境使用 `10.0.2.2`（Android 模拟器访问宿主机 localhost 地址），避免硬编码 `localhost`。

---

## 18. 构建与运行

### 18.1 构建命令

| 命令 | 说明 |
|------|------|
| `./gradlew assembleDebug` | 构建 Debug APK |
| `./gradlew assembleRelease` | 构建 Release APK |
| `./gradlew test` | 运行单元测试 |
| `./gradlew detekt` | 运行 Detekt 静态分析 |
| `./gradlew connectedAndroidTest` | 运行仪器化测试（需连接设备/模拟器） |

### 18.2 构建流程

```
./gradlew assembleDebug
    │
    ├── 1. 解析 Version Catalog (libs.versions.toml)
    ├── 2. 下载依赖（Maven Central / Google Maven）
    ├── 3. KSP 处理（Hilt 注解处理）
    ├── 4. Kotlin 编译（kotlinc）
    ├── 5. 资源处理（AAPT2）
    ├── 6. DEX 转换（d8）
    ├── 7. APK 打包 & 签名（Debug key）
    └── 8. 输出: app/build/outputs/apk/debug/app-debug.apk
```

### 18.3 开发环境先决条件

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| Android Studio | Latest Stable | IDE |
| JDK | 21 | Kotlin 2.0 编译 |
| Android SDK | 36 | compileSdk |
| Android Emulator / Device | API 26+ | minSdk |
| Gradle | 8.9（wrapper 自带） | 构建工具 |

---

## 19. 风险与开放问题

### 19.1 已知风险

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Maven Central 下载慢 | Gradle Sync 超时 | settings.gradle.kts 包含阿里云镜像；README 说明代理配置 |
| Hilt + KSP 版本兼容 | 编译失败 | Version Catalog 锁定版本，后续升级时统一验证 |
| kotlinx.serialization 与 Retrofit Converter 兼容 | 运行时序列化错误 | 锁定 converter 版本 `1.0.0`，使用 `jakewharton` 维护的实现 |
| Compose BOM 版本冲突 | 编译/运行时崩溃 | 使用 BOM 统一管理 Compose 版本，避免单独声明版本 |

### 19.2 后续 PRD 待实现

| 功能 | 当前状态 | 后续动作 |
|------|---------|---------|
| 认证体系（JWT） | AuthInterceptor 骨架 | 实现 Token 存储、刷新逻辑 |
| 业务功能（首页 Feed、播放器等） | 仅占位 UI | 在各 feature 中填充业务逻辑 |
| 数据持久化（Room/DataStore） | PreferencesDataSource 骨架 | 按业务需求择机启用 |
| 缓存策略 | 无 | 添加 OkHttp Cache、本地 DB 缓存 |
| CI/CD 构建 | 无 | 添加 GitHub Actions Android Job |
| 性能监控 | 无 | 接入性能 SDK |
| 国际化 | 无（硬编码中文） | 引入 strings.xml 多语言 |

---

## 20. 参考资料

| 文档 | 关键信息 |
|------|---------|
| `docs/specs/2026-07-24-project-init/spec.md` Section 4.5 | Android 分层架构定义、目录结构、关键约束 |
| `docs/specs/2026-07-24-project-init/spec.md` Section 6.4 | US-04 Android 工程初始化功能详述、流程、验收标准 |
| `docs/specs/2026-07-24-project-init/design.md` | 共享 API 设计（7 个端点）、数据模型 Schema（Drama/Episode）、统一错误响应格式、分页规范 |
| `PRODUCT.md` | appId: `com.djs66256.short_drama`，schema: `djsdrama://` |
