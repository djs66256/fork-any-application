# Android 端技术方案：PRD-02 首页信息流

> 创建日期：2026-07-25
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

Android 端在 PRD-01 已完成的单 Activity + Navigation Compose 应用壳上，将首页从“应用名 + 示例按钮”占位页演进为**Native Feed 首屏列表**。实现继续遵循现有分层：UI（Compose）→ ViewModel（StateFlow）→ Domain UseCase → Repository → RemoteDataSource → Retrofit API。

```text
HomeScreen
  -> collects HomeUiState from HomeViewModel
     -> HomeViewModel.loadIfNeeded()
        -> GetDramasUseCase(page = 1, pageSize = 10)
           -> DramaRepository.getDramas(page, pageSize)
              -> DramaRemoteDataSource.getDramas(page, pageSize)
                 -> ApiService.getDramas(page, pageSize)
                    -> GET /api/dramas?page=1&pageSize=10
  -> renders Loading / Content / Empty / Error
  -> on click -> navigate to play/{id} or detail/{id}
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `android/app/src/main/java/.../feature/home/ui/HomeScreen.kt` | 修改 | 从占位页重构为首页 Feed 列表、空态、错误态、重试入口 |
| `android/app/src/main/java/.../feature/home/viewmodel/HomeViewModel.kt` | 修改 | 从只暴露 appName/appVersion 扩展为完整 Feed UI 状态模型 |
| `android/app/src/main/java/.../domain/usecase/GetDramasUseCase.kt` | 不变 | 继续复用现有列表用例 |
| `android/app/src/main/java/.../data/repository/DramaRepositoryImpl.kt` | 轻微修改 | 继续复用 DTO -> Domain 映射，适配 query 参数收口 |
| `android/app/src/main/java/.../data/datasource/DramaRemoteDataSource.kt` | 不变 / 轻微调整 | 保持封装形式，随着 ApiService query 命名调整共同收口 |
| `android/app/src/main/java/.../core/network/ApiService.kt` | 修改 | query 从 `page_size` 迁移到 `pageSize` |
| `android/app/src/main/java/.../data/dto/DramaDto.kt` | 不变 | 继续消费 `cover_url`、`episode_count`、`created_at` 等 API 字段 |
| `android/app/src/test/java/.../feature/home/viewmodel/HomeViewModelTest.kt` | 修改 | 从 appName/appVersion 测试扩展到首页状态机测试 |
| PRD-01 导航文件 | 不变 | 继续复用 `play/{videoId}` 与 `detail/{dramaId}` 路由承载 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt` | 修改 | 新增首页列表、空态、错误态、重试动作和首屏只加载一次逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 渲染 Feed 列表卡片与 loading / empty / error / retry UI |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | `getDramas` query 参数改为 `page` + `pageSize` |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt` | 轻微修改 | 无接口签名变更，但与新 query 收口保持一致 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt` | 轻微修改 | 保持 DTO -> Domain 映射和 ApiResult 透传 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModelTest.kt` | 修改 | 扩展到 success / empty / error / retry 测试 |
| `android/app/src/test/java/com/djs66256/short_drama/data/...` | 按需新增 | 如需要，可补 ApiService / DTO / Repository 收口测试 |

---

## 3. UI 层设计

### 3.1 组件层级树

```text
HomeScreen
├── HomeFeedContent
│   ├── LoadingSection
│   ├── ErrorSection
│   │   └── RetryButton
│   ├── EmptySection
│   └── LazyColumn
│       └── HomeDramaCard (items)
│           ├── DramaCover
│           ├── DramaInfoColumn
│           │   ├── Title
│           │   ├── Description
│           │   └── MetaRow(category / tags / rating / episodeCount)
│           └── ActionRow
│               ├── PlayButton
│               └── DetailButton
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `HomeScreen` | Composable | 首页根视图，根据 UI 状态切换 loading / list / empty / error | 否 |
| `HomeDramaCard` | Composable | 渲染单条短剧卡片与双按钮动作 | 是 |
| `HomeFeedEmptyState` | Composable | 空态提示 | 是 |
| `HomeFeedErrorState` | Composable | 错误提示与重试按钮 | 是 |
| `DramaCoverPlaceholder` | Composable | 封面缺失时的占位视觉 | 是 |

### 3.3 Composable 接口定义

```kotlin
@Composable
fun HomeScreen(
    onOpenPlay: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
)

@Composable
fun HomeDramaCard(
    drama: Drama,
    onPlay: () -> Unit,
    onDetail: () -> Unit,
    modifier: Modifier = Modifier,
)
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| `HomeViewModel` → `HomeScreen` | `StateFlow<HomeUiState>` | 首页状态渲染 |
| `HomeScreen` → `HomeDramaCard` | Composable 参数 + lambda | 卡片展示与点击事件 |
| `HomeScreen` → NavGraph | `onOpenPlay` / `onOpenDetail` | 进入播放页 / 详情页 |
| Repository → ViewModel | `ApiResult<List<Drama>>` | 网络结果和错误透传 |

### 3.5 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | 单列 `LazyColumn` | 首版不做双栏/平板特化 |
| 横竖屏 | 依赖 Compose 状态恢复 | 返回首页保持当前状态 |
| 字体缩放 | 复用 Material3 typography | 文本可多行 / 截断 |
| 深色模式 | 复用 `ShortDramaTheme` | 占位态与卡片在深浅模式可读 |
| 图片缺失 | 占位块 / icon | 防止空封面造成布局问题 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 UI | 职责 |
|-----------|---------|------|
| `HomeViewModel` | `HomeScreen` | 管理首页首屏请求、状态切换、重试与首屏只加载一次 |

### 4.2 状态定义

```kotlin
data class HomeUiState(
    val isLoading: Boolean = true,
    val items: List<Drama> = emptyList(),
    val errorMessage: String? = null,
    val hasLoadedOnce: Boolean = false,
    val isRetrying: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDramasUseCase: GetDramasUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadIfNeeded() { ... }
    fun retry() { ... }
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `isLoading` | `Boolean` | `true` | 首次进入或重试时为 true |
| `items` | `List<Drama>` | `emptyList()` | 首页列表数据 |
| `errorMessage` | `String?` | `null` | 错误态文案 |
| `hasLoadedOnce` | `Boolean` | `false` | 避免重复自动请求 |
| `isRetrying` | `Boolean` | `false` | 防止错误态重复点击重试 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | UI 层表现 |
|---------|---------|----------|
| Loading | `isLoading == true` | `CircularProgressIndicator` / 占位态 |
| Success (有数据) | `items.isNotEmpty() && errorMessage == null && !isLoading` | 列表卡片 |
| Empty | `items.isEmpty() && errorMessage == null && !isLoading` | 空态文案 |
| Error | `errorMessage != null && !isLoading` | 错误文案 + 重试按钮 |

### 4.5 行为约束

- 首次进入首页自动请求 `page = 1, pageSize = 10`。
- 本期只请求第一页，不做加载更多和下拉刷新。
- `retry()` 与首次加载共用同一条数据链路。
- 错误态重试时应忽略并发重复点击。

---

## 5. Navigation 路由设计

### 5.1 导航方案

继续复用 PRD-01 已落地的 Navigation Compose 路由壳层。

### 5.2 路由清单

| 路由标识 | 目标 Composable | 参数 | 导航方式 | 说明 |
|---------|----------------|------|---------|------|
| `play/{videoId}` | PlayerScreen | `videoId = drama.id` | `navController.navigate(...)` | 首页主动作 |
| `detail/{dramaId}` | DramaDetailScreen | `dramaId = drama.id` | `navController.navigate(...)` | 首页次动作 |

### 5.3 导航图

```kotlin
HomeScreen(
    onOpenPlay = { dramaId -> navController.navigate("play/$dramaId") },
    onOpenDetail = { dramaId -> navController.navigate("detail/$dramaId") },
)
```

### 5.4 Deep Link 处理（如适用）

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `djsdrama://play/{id}` | `play/{videoId}` | `id -> videoId` |
| `djsdrama://drama/{id}` | `detail/{dramaId}` | `id -> dramaId` |

首页 Feed 本身不新增 deeplink 解析规则，仅保证从首页出发的路由与现有契约一致。

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | Retrofit | 继续复用现有 `ApiClient` |
| 数据模型 | `@Serializable` DTO | 保持 snake_case 字段注解 |
| 请求拦截器 | 现有 OkHttp / Retrofit 配置 | 本期无新增鉴权逻辑 |
| 响应解析 | kotlinx.serialization | 继续复用 |
| 错误处理 | `ApiResult<T>` | 继续复用 Success / Error / Exception |

### 6.2 API 接口定义

```kotlin
interface ApiService {
    @GET("dramas")
    suspend fun getDramas(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10,
    ): DramaListResponseDto
}
```

说明：
- 相对路径继续是 `dramas`，由 `ApiClient` 负责 base URL。
- 与 PRD-02 canonical contract 对齐后，不再使用 `page_size` query 名。

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 网络超时 | 0 | — | 本期由用户手动重试 |
| 5xx 服务端错误 | 0 | — | 本期不做自动重试 |
| 401 Token 过期 | — | — | 首页无需登录，不涉及 |

### 6.4 网络状态监听

- 本期不引入 `ConnectivityManager.NetworkCallback` 作为业务逻辑前提。
- 异常统一在 `HomeViewModel` 中落到错误态。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| 首页第一页数据 | 不持久化 | — | 会话内有效 | 首版不做 Room / DataStore 缓存 |
| 首页 UI 状态 | ViewModel 内存状态 | `StateFlow` | ViewModel 生命周期 | 返回首页沿用当前状态 |

### 7.2 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| 首页列表 | 不做本地缓存 | — | 进程回收后丢失 |
| 图片 | 依赖未来方案 | — | 本期不单独设计图片缓存 |

### 7.3 数据迁移策略

- 无 Room / DataStore migration。
- 主要迁移是网络 query 命名：`page_size -> pageSize`。

---

## 8. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Android 实现方式 |
|---------|---------------|-----------------|
| canonical contract | `/api/dramas?page&pageSize` + `{ data, pagination }` | 修改 `ApiService.getDramas` query 命名 |
| 首页状态机 | loading / content / empty / error | 在 `HomeUiState` 中显式建模 |
| 首屏第一页范围 | 仅请求第一页 | `GetDramasUseCase(page = 1, pageSize = 10)` |
| 播放入口映射 | `drama.id -> videoId` | `onOpenPlay(drama.id)` -> `play/{videoId}` |
| 详情入口映射 | `drama.id -> dramaId` | `onOpenDetail(drama.id)` -> `detail/{dramaId}` |
| 缺封面 / 空标签容错 | 客户端容错渲染 | Compose 卡片提供占位块和可选 meta 展示 |

---

## 9. 边界与错误处理

### 9.1 全局错误处理架构

| 层级 | 机制 | 说明 |
|------|------|------|
| DataSource | try/catch -> `ApiResult.Exception` | 捕获 Retrofit / 序列化异常 |
| Repository | 映射 `ApiResult` | 不吞错误 |
| ViewModel | `when (result)` | 映射为 success / empty / error |
| UI | 错误态 + 重试 | 提供用户可恢复入口 |

### 9.2 错误态映射

| 错误来源 | Android 表现 | 说明 |
|---------|-------------|------|
| `ApiResult.Exception` | 错误态 + 通用失败文案 | 网络或未知异常 |
| `ApiResult.Error` | 错误态 + 服务端 message | 标准服务端错误 |
| 空数据 | 空态 | 非错误，单独建模 |

### 9.3 边界场景

| 场景 | 触发条件 | UI 行为 | 说明 |
|------|---------|---------|------|
| 空列表 | 返回 `[]` | 显示空态 | 不回退到旧占位页 |
| 缺封面 | `coverUrl` 为空 | 显示占位封面 | 不崩溃 |
| 长标题 / 长描述 | 文本超长 | 限制行数 / 截断 | 维持卡片布局 |
| 重复重试 | 多次点击重试 | 只保留一次有效请求 | 避免并发抖动 |
| 返回首页 | 从播放/详情返回 | 保持首页现有状态 | 不重新回到 appInfo 页面 |

---

## 10. 测试策略

### 10.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| ViewModel 测试 | loading / success / empty / error / retry | JUnit4 + MockK + Turbine |
| DTO / Repository 测试 | snake_case 字段映射、ApiResult 转换 | JUnit4 |
| 路由联通性测试 | 首页卡片跳到 `play` / `detail` | 现有导航测试基础上补充 |

### 10.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 测试类型 |
|------|---------|------|---------|---------|
| A-01 | 首页首次加载成功且有数据 | use case 返回 1 条 drama | `uiState.items` 非空，`isLoading=false` | ViewModel |
| A-02 | 首页首次加载为空 | use case 返回空列表 | 进入 empty 状态 | ViewModel |
| A-03 | 首页加载失败 | use case 返回 `ApiResult.Error/Exception` | `errorMessage` 非空 | ViewModel |
| A-04 | 首页重试成功 | 首次失败、二次成功 | 状态从 error -> success/empty | ViewModel |
| A-05 | query 命名收敛 | 调用 ApiService | 请求参数使用 `pageSize` | 网络 / Repository |
| A-06 | DTO 映射 | `episode_count` / `cover_url` / `created_at` | 正确映射到 Domain | DTO |

### 10.3 与现有测试的衔接

- 现有 `T-06 uiState transitions from loading to populated` 不再适合作为首页唯一测试，应重构为首页 Feed 状态机测试。
- 如需保留 appName/appVersion，可降级为次要字段断言，不再作为首页主验收标准。

---

## 11. 风险与取舍

| 风险 / 取舍 | 说明 | 对应策略 |
|------------|------|---------|
| 不做本地缓存 | ViewModel 被销毁后需重新请求 | 接受为 MVP 取舍 |
| 不做第二页 | 无法验证列表翻页体验 | 明确列为后续 PRD |
| 首页从占位页完全演进为列表 | 原测试与 UI 结构变化较大 | 以状态机为核心重建测试 |
| query 改名为 `pageSize` | 需同步 Retrofit 与测试期望 | 在设计与实现中优先处理该项收口 |
