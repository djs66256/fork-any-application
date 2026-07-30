# Android 端技术方案：PRD-11 个人资产管理

> 创建日期：2026-07-30
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

Android 端继续沿用当前 `Jetpack Compose + Navigation Compose + Hilt + ViewModel + Repository` 架构，在已有菜单抽屉、认证状态持有器与 ranking 预约链路基础上，把 `menu/booking` 从 placeholder route 升级为真实的预约资产页，并继续保留 `menu/downloads` 为占位页。

本期不新增第三方依赖，不引入 Room / DataStore 持久化，不改变现有 Auth 基建。受保护接口 `GET /api/users/me/bookings` 通过现有 Retrofit + OkHttp + AuthInterceptor 接入；由于 Android 已有 `AuthStateHolder` 与 `AuthSessionProvider`，booking 页直接复用全局登录态与 access token，不新增单独 token 传递协议。

```text
MenuPanelDrawer
  -> tap 我的预约
     -> MainNavigationViewModel.closeMenuThenNavigate(PendingRoute.MenuBooking)
     -> NavGraph consumes pending route
     -> navController.navigate(AppDestination.menuBooking())
     -> BookingAssetsScreen
        -> BookingAssetsViewModel
           -> GetBookingAssetsUseCase
              -> DramaRepository.getBookingAssets(query)
                 -> DramaRepositoryImpl
                    -> DramaRemoteDataSource.getBookingAssets(query)
                       -> ApiService.getUserBookings(status, page, pageSize)
                          -> Authorization header injected by AuthInterceptor

AuthStateHolder
  -> authStatus / currentSession
  -> BookingAssetsViewModel observes login state
     -> anonymous / expired => login gate
     -> authenticated => request first page
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 扩展 | `menu/booking` 保持路由名不变，但语义从 placeholder 升级为真实 booking 页面 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | booking route 改注册真实 `BookingAssetsScreen`；downloads 继续 placeholder |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt` | 复用 / 局部扩展 | 继续使用菜单关闭后导航状态机，无需重做抽屉编排 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntries.kt` | 微调 | “我的预约”文案从建设中占位改成查看预约资产；downloads 保持建设中 |
| `android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthStateHolder.kt` | 复用 | 提供 `authStatus`、`currentSession()`、`accessToken()` |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 扩展 | 新增 `GET users/me/bookings` |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt` | 修改 | 将 `users/me/bookings` 纳入 `requiresAuth()` 白名单，确保 booking 接口自动携带 bearer token |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | 不变 / 复用模式 | 复用 request token、防乱序、append footer、RequireLogin effect 模式 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt` | 不变 / 显式对齐 | 现有 `resolveSuccessRoute()` 已允许 `menu/booking` 透传，无需为 booking 回流新增特殊分支，但需补回归测试确保该约束稳定 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/common/ui/PlaceholderScreen.kt` | 复用 | 继续承接 downloads |

### 1.2 架构决策

1. **Android booking 继续复用既有 `menu/booking` canonical route**：与 shared design 一致，不新增第二套 route 名。
2. **登录承接在 booking route 内解决**：匿名用户也进入 `menu/booking`，但页面显示登录承接态，不直接请求 401。
3. **受保护接口依赖全局 AuthInterceptor**：不在每个 use case 手动拼 Authorization header。
4. **summary 只读服务端**：双 Tab 计数不在客户端本地重算。
5. **请求防乱序复用 ranking 模式**：refresh/append 各持一个 token，防止 Tab 快切造成串页。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 将 `menu/booking` 从 `PlaceholderScreen` 改为真实 `BookingAssetsScreen` |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 复用 / 注释收口 | 保持 `MENU_BOOKING = "menu/booking"`，作为 booking 真实 route |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntries.kt` | 修改 | 更新 booking subtitle；downloads 保持占位文案 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/BookingAssetsScreen.kt` | 新增 | 预约资产页根 Composable |
| `android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/components/*` | 新增 | Tab 条、卡片、空态、错误态、登录承接态、append footer |
| `android/app/src/main/java/com/djs66256/short_drama/feature/booking/viewmodel/BookingAssetsViewModel.kt` | 新增 | 管理首屏、切 Tab、分页、summary、防乱序与登录 effect |
| `android/app/src/main/java/com/djs66256/short_drama/feature/booking/model/BookingAssetsUiState.kt` | 新增 | 页面 UI state 与 item ui model |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAsset.kt` | 新增 | 单条预约资产领域模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAssetStatus.kt` | 新增 | `ONLINE / UPCOMING` 枚举 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAssetSummary.kt` | 新增 | `onlineCount / upcomingCount` |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAssetsPage.kt` | 新增 | `items + pagination + summary` 聚合结果 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAssetsQuery.kt` | 新增 | `status/page/pageSize` 查询模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/DramaRepository.kt` | 修改 | 新增 `getBookingAssets(query)` contract |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetBookingAssetsUseCase.kt` | 新增 | 预约资产读取用例 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/BookingAssetDto.kt` | 新增 | 对齐 backend `BookingAsset` schema |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/BookingAssetSummaryDto.kt` | 新增 | 对齐 `summary` |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/BookingAssetsResponseDto.kt` | 新增 | 对齐 `{ data, pagination, summary }` |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt` | 修改 | 新增 booking assets 远程读取方法 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt` | 修改 | 实现 booking assets DTO → Domain 映射 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/booking/viewmodel/BookingAssetsViewModelTest.kt` | 新增 | 覆盖登录态、首屏、切 Tab、防乱序、追加失败 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/DramaRepositoryImplTest.kt` | 修改 | 覆盖 booking assets 映射与错误透传 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphBookingRouteTest.kt` | 新增 / 修改 | 覆盖 booking route 导航与菜单关闭后跳转 |

---

## 3. UI 层设计

### 3.1 组件层级树

```text
BookingAssetsScreen
├── TopAppBar(title = 我的预约, navigationIcon = Back)
├── BookingAssetsBody
│   ├── RestoringState
│   ├── LoginGateCard
│   │   ├── Illustration/Icon
│   │   ├── Title + Description
│   │   └── LoginButton
│   ├── BookingStatusTabs
│   │   ├── OnlineTab(count)
│   │   └── UpcomingTab(count)
│   ├── FirstPageLoadingState
│   ├── BookingAssetsErrorState
│   ├── BookingAssetsEmptyState
│   └── BookingAssetsList
│       ├── BookingAssetCard
│       ├── AppendLoadingFooter
│       └── AppendErrorFooter
└── SnackbarHost / one-shot message
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `BookingAssetsScreen` | Composable | 预约资产页根入口，拼装 ViewModel 与导航回调 | 否 |
| `BookingStatusTabs` | Composable | 展示 `已上线(N)` / `待上线(N)` Tab | 否 |
| `BookingAssetCard` | Composable | 展示封面占位、标题、集数、预约时间、状态标签 | 否 |
| `BookingAssetsEmptyState` | Composable | 当前 Tab 空态 | 否 |
| `BookingAssetsErrorState` | Composable | 首屏失败态与重试 | 否 |
| `BookingAssetsLoginGate` | Composable | 匿名 / expired 登录承接态 | 否 |
| `PlaceholderScreen` | Composable | downloads 占位页 | 是 |

### 3.3 Composable 接口定义

```kotlin
@Composable
fun BookingAssetsScreen(
    onBack: () -> Unit,
    onRequireLogin: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingAssetsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BookingAssetsEffect.RequireLogin -> onRequireLogin(effect.returnRoute)
                is BookingAssetsEffect.ShowMessage -> { /* Snackbar */ }
            }
        }
    }

    BookingAssetsContent(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRetryAppend = viewModel::retryAppend,
        onSelectStatus = viewModel::onStatusSelected,
        onLoadNextPage = viewModel::loadNextPageIfNeeded,
        onLoginClick = viewModel::onLoginClick,
    )
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| `NavGraph -> BookingAssetsScreen` | Composable 参数 | `onBack`、`onRequireLogin` |
| `BookingAssetsScreen -> ViewModel` | 方法调用 | 选择 Tab、重试、加载更多、点击登录 |
| `ViewModel -> UI` | `StateFlow` | 主页面状态、summary、append 错误 |
| `ViewModel -> NavGraph` | `SharedFlow` effect | 触发统一登录页 |

### 3.5 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | `fillMaxWidth + LazyColumn` | 保持手机单列卡片布局 |
| 横竖屏 | `rememberLazyListState` + ViewModel 状态恢复 | 旋转不丢失当前 Tab |
| 字体缩放 | 使用 Material3 typography，不写死高度 | count 文字允许换行 |
| 深色模式 | 复用现有 MaterialTheme | 不新增硬编码颜色 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 UI | 职责 |
|-----------|---------|------|
| `BookingAssetsViewModel` | `BookingAssetsScreen` | 管理登录承接、首屏加载、Tab 切换、分页、summary、防乱序 |

### 4.2 状态定义

```kotlin
data class BookingAssetsUiState(
    val selectedStatus: BookingAssetStatus = BookingAssetStatus.ONLINE,
    val summary: BookingAssetSummary = BookingAssetSummary(),
    val items: List<BookingAssetItemUiModel> = emptyList(),
    val authGate: BookingAuthGate = BookingAuthGate.Restoring,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isAppending: Boolean = false,
    val appendErrorMessage: String? = null,
    val errorMessage: String? = null,
    val page: Int = 1,
    val hasNextPage: Boolean = false,
    val hasLoadedOnce: Boolean = false,
)

sealed interface BookingAssetsEffect {
    data class RequireLogin(val returnRoute: String) : BookingAssetsEffect
    data class ShowMessage(val message: String) : BookingAssetsEffect
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `selectedStatus` | `BookingAssetStatus` | `ONLINE` | 当前 Tab |
| `summary` | `BookingAssetSummary` | `0/0` | 服务端返回摘要 |
| `authGate` | `Restoring/Anonymous/Authenticated/Expired` | `Restoring` | 控制是否允许请求 |
| `items` | `List<BookingAssetItemUiModel>` | `emptyList()` | 当前 Tab 列表 |
| `isLoading` | `Boolean` | `false` | 首屏 loading |
| `isRefreshing` | `Boolean` | `false` | 切 Tab / retry 时覆盖层 loading |
| `isAppending` | `Boolean` | `false` | 下一页 loading |
| `appendErrorMessage` | `String?` | `null` | footer 错误 |
| `errorMessage` | `String?` | `null` | 首屏错误 |
| `page` | `Int` | `1` | 当前页 |
| `hasNextPage` | `Boolean` | `false` | 是否继续分页 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | UI 层表现 |
|---------|---------|----------|
| Restoring | `authGate == Restoring` | loading，不显示登录承接闪烁 |
| LoginGate | `authGate == Anonymous || authGate == Expired` | 登录承接态 |
| Loading | `isLoading && !hasLoadedOnce` | 首屏 loading |
| Success | `items.isNotEmpty()` | 列表 + Tab + summary |
| Empty | `hasLoadedOnce && items.isEmpty() && errorMessage == null` | 当前 Tab 空态 |
| Error | `errorMessage != null && items.isEmpty()` | 首屏错误态 |
| AppendError | `appendErrorMessage != null` | footer 可重试 |

### 4.5 核心行为

1. `observeAuthState()`
   - 订阅 `AuthStateHolder.authStatus`；
   - `Anonymous/Expired` 时回登录承接态并清空旧用户列表；
   - `Authenticated` 时首次进入自动加载首屏。
2. `onStatusSelected(status)`
   - 切换当前 Tab，重置页码并刷新；
   - 不本地重算 `summary`。
3. `retry()`
   - 对当前 Tab 做首屏重试。
4. `loadNextPageIfNeeded()`
   - 只有当前列表非空且 `hasNextPage = true` 时触发。
5. `onLoginClick()`
   - 发出 `RequireLogin(returnRoute = AppDestination.menuBooking())` effect；
   - 登录成功后回同一路由。

### 4.6 防乱序策略

直接复用 `RankingViewModel` 的成熟模式：

- `latestQueryKey = BookingRequestKey(status)`；
- `activeRefreshToken` 与 `activeAppendToken` 分离；
- 只有 token 与 queryKey 都匹配时才写回状态；
- Tab 快速切换、重试、后台回前台时都不会让旧请求覆盖新 UI。

```kotlin
private fun refresh(status: BookingAssetStatus, isRetry: Boolean) {
    val queryKey = BookingRequestKey(status)
    latestQueryKey = queryKey
    activeAppendToken = null
    val token = nextRequestToken()
    activeRefreshToken = token
    // ...launch request
}
```

---

## 5. Navigation 路由设计

### 5.1 导航方案

继续使用现有 `Navigation Compose + MainNavigationViewModel.closeMenuThenNavigate()`。

### 5.2 路由清单

| 路由标识 | 目标 Composable | 参数 | 导航方式 | 说明 |
|---------|----------------|------|---------|------|
| `menu/booking` | `BookingAssetsScreen` | 无 | `navController.navigate()` | 菜单“我的预约”真实页面 |
| `menu/downloads` | `PlaceholderScreen` | 无 | `navController.navigate()` | 菜单“我的下载”继续占位 |
| `login?returnRoute=menu/booking&source=menu_booking` | `LoginScreen` | `returnRoute/source` | `navController.navigate()` | 登录成功后回 booking route |

### 5.3 路由管理

保留现有 `PendingRoute.MenuBooking`，但消费逻辑从导航到 placeholder 改为导航到真实 booking screen。

`NavGraph` 中：

```kotlin
PendingRoute.MenuBooking -> {
    navController.navigate(AppDestination.menuBooking())
    navigationViewModel.consumePendingRoute()
}
```

同时把 home graph 中原有：

```kotlin
composable(route = AppDestination.menuBooking()) {
    PlaceholderScreen(...)
}
```

改为：

```kotlin
composable(route = AppDestination.menuBooking()) {
    BookingAssetsScreen(
        onBack = { navController.popBackStack() },
        onRequireLogin = { returnRoute ->
            navController.navigate(
                AppDestination.login(
                    returnRoute = returnRoute,
                    source = "menu_booking",
                ),
            )
        },
    )
}
```

### 5.4 登录回流

Android 当前登录页已经通过 `returnRoute` 恢复来源页，因此 booking 场景只需要定稿：

- source 使用 `menu_booking`；
- returnRoute 固定为 `AppDestination.menuBooking()`；
- 登录成功后不跳其它 tab，不回首页，不回 placeholder。

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | Retrofit + OkHttp | 复用现有 `ApiService` |
| 数据模型 | `@Serializable` DTO | 继续使用 kotlinx.serialization |
| 鉴权头 | `AuthInterceptor` | 自动注入 access token |
| 响应解析 | `BookingAssetsResponseDto` | 消费 `{ data, pagination, summary }` |
| 错误处理 | `ApiResult<T>` | 与 ranking / theater 一致 |

### 6.2 API 接口定义

在 `ApiService` 中新增：

```kotlin
@GET("users/me/bookings")
suspend fun getUserBookings(
    @Query("status") status: String,
    @Query("page") page: Int = 1,
    @Query("pageSize") pageSize: Int = 20,
): BookingAssetsResponseDto
```

由于 booking 接口走 `/users/me/*` 并依赖当前登录态，Authorization header 继续由全局 `AuthInterceptor` 注入，不在 `ApiService` 方法签名里单独传 token。

### 6.3 DTO 设计

```kotlin
@Serializable
data class BookingAssetDto(
    @SerialName("drama_id") val dramaId: String,
    val title: String,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("episode_count") val episodeCount: Int,
    @SerialName("booked_at") val bookedAt: String,
    @SerialName("availability_status") val availabilityStatus: String,
)

@Serializable
data class BookingAssetSummaryDto(
    @SerialName("online_count") val onlineCount: Int,
    @SerialName("upcoming_count") val upcomingCount: Int,
)

@Serializable
data class BookingAssetsResponseDto(
    val data: List<BookingAssetDto>,
    val pagination: PaginationDto,
    val summary: BookingAssetSummaryDto,
)
```

### 6.4 Repository contract

`DramaRepository` 新增：

```kotlin
suspend fun getBookingAssets(query: BookingAssetsQuery): ApiResult<BookingAssetsPage>
```

其实现要求：

- 仅把 `ONLINE/UPCOMING` 转成 `online/upcoming` query；
- 不在本地变更 summary 口径；
- 401 保持为业务错误，交给 ViewModel 决定回登录承接态。

### 6.5 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 网络超时 | 0 | 手动重试 | 保持现有 App 行为 |
| 401 | 0 | 不自动重放 | 直接回登录承接态 |
| 429 | 0 | 不自动重试 | 提示稍后再试 |
| 5xx/503 | 0 | 手动重试 | 首屏错误态 / footer 重试 |

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| Auth session | 既有 `AuthSessionStore` | 现有 Keychain / preferences 实现 | 复用认证体系 | 不新增 |
| Booking 列表 | 不落盘 | ViewModel 内存态 | 路由生命周期 | 首版不缓存 |
| Summary | 不落盘 | ViewModel 内存态 | 每次接口刷新 | 仅展示用途 |
| Downloads 占位 | 不落盘 | 静态文案 | — | 继续 placeholder |

### 7.2 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| 当前 Tab 列表 | 内存态 | 页面生命周期 | ViewModel 销毁即释放 |
| 另一 Tab 数据 | 不做跨页面缓存 | 同上 | 切回后按需重拉 |

### 7.3 数据库 Migration

无。PRD-11 Android 端不新增 Room / DataStore schema。

---

## 8. 配置与环境

| 配置项 | 管理方式 | 开发环境值 | 生产环境值 | 说明 |
|--------|---------|----------|-----------|------|
| API Base URL | `AppConfig / BuildConfig` | 现有 debug 配置 | 现有 release 配置 | 继续复用 |
| Access Token | `AuthStateHolder` | 登录后动态获得 | 登录后动态获得 | 由 `AuthInterceptor` 注入 |
| Booking Feature Flag | 无 | — | — | 本期不新增开关 |

> ⚠️ 禁止硬编码 host、token、用户 ID、固定验证码等环境常量。

---

## 9. API 调用清单

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| `GET /api/users/me/bookings` | 进入 booking 页首屏 | `selectedStatus + page=1 + pageSize=20` | 更新列表、分页、summary | 401 回登录承接；429/5xx 展示错误态 |
| `GET /api/users/me/bookings` | 切换 `已上线/待上线` | 当前 Tab + `page=1` | 更新当前 Tab 与 summary | 旧请求结果丢弃 |
| `GET /api/users/me/bookings` | 列表到底加载更多 | `page+1` | 追加列表 | append footer 错误，不清空内容 |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Android 端实现方式 |
|---------|---------------|-------------------|
| 菜单关闭后导航 | 点击入口先关菜单再导航 | 继续复用 `MainNavigationViewModel.closeMenuThenNavigate()` |
| booking 独立 route | booking 必须是真实页面 | `menu/booking` 升级为真实 `BookingAssetsScreen` |
| 登录承接目标 | 匿名用户登录成功回 booking route | `AppDestination.login(returnRoute = AppDestination.menuBooking(), source = "menu_booking")` |
| 默认 Tab | 默认 `online` | `BookingAssetsUiState.selectedStatus = ONLINE` |
| `summary` 口径 | 不本地重算 | 直接消费 `summary.onlineCount/upcomingCount` |
| 请求防乱序 | 快切不串页 | 复用 `activeRefreshToken / activeAppendToken` |
| 追加分页 | 失败只影响当前 Tab | `appendErrorMessage` 与主列表状态分离 |
| 未授权恢复 | token 失效退回登录承接态 | 401 时清空旧列表并转登录承接态 |
| 下载占位延续 | downloads 不接真实数据 | `menu/downloads` 继续 `PlaceholderScreen` |

---

## 11. 边界与错误处理

### 11.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| 网络层 | Retrofit 异常 -> `ApiResult.Exception` | 网络错误统一兜底 |
| DataSource/Repository | `ApiResult.Success/Error/Exception` | 保持现有仓储 contract |
| ViewModel | `when (result)` | 区分首屏失败、追加失败、401 登录失效 |
| UI 层 | 空态 / 错误态 / Snackbar | 不展示原始错误码 |

### 11.2 错误码映射表

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `VALIDATION_ERROR` | 加载失败，请重试 | 首屏错误态 |
| `AUTH_UNAUTHORIZED` / `UNAUTHORIZED` | 请先登录后查看预约 | 登录承接态 |
| `TOO_MANY_REQUESTS` / `AUTH_RATE_LIMITED` | 操作过于频繁，请稍后再试 | Snackbar / footer 提示 |
| `INTERNAL_ERROR` | 加载失败，请稍后重试 | 首屏错误态 |
| `SERVICE_UNAVAILABLE` | 服务暂不可用，请稍后重试 | 首屏错误态 |
| 端侧网络异常 | 网络请求失败，请检查网络后重试 | 首屏错误态 / footer 重试 |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| 认证恢复中 | `AuthStatus.Restoring` | 展示 loading，不先露出登录承接态 | 🔴 |
| 匿名进入 booking | 无 session | 显示登录承接态，不直接请求接口 | 🔴 |
| 登录成功回流 | 从 booking 登录承接态进入登录页 | 回 `menu/booking`，不跳其它 tab | 🔴 |
| 快速切换 Tab | 多次点击 online/upcoming | 只消费最新 token 对应请求结果 | 🔴 |
| 追加失败 | page N+1 请求失败 | 保留已有内容，仅 footer 错误 | 🔴 |
| token 过期 | 接口返回 401 | 退回登录承接态并清空旧用户数据 | 🔴 |
| 当前 Tab 空、另一侧有数据 | `online=0/upcoming>0` 等 | 不自动切 Tab | 🟡 |
| downloads 点击 | 用户点“我的下载” | 继续 placeholder，不打 booking API | 🟢 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `BookingAssetsScreen` 首屏 | ✅ | ✅ | ✅ | ✅ | — |
| `BookingStatusTabs` | ✅ | ✅ | ✅ | ✅ | — |
| `BookingAssetsList` | — | ✅ | — | — | — |
| `AppendFooter` | ✅ | — | — | ✅ | — |
| `LoginGate` | restoring 时降级为 loading | — | — | — | 认证缺失时展示 |

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| ViewModel 单测 | booking 状态机、防乱序、401/login gate、append | 关键路径全覆盖 | JUnit4 + MockK + Turbine |
| Repository 单测 | DTO 映射、错误透传 | 关键 contract 全覆盖 | JUnit4 + MockK |
| Navigation 单测 | 菜单关闭后 booking/downloads 导航、login 回流 route | 关键路径全覆盖 | JUnit4 |

### 12.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| AND-BKG-01 | 匿名进入 booking 页 | `AuthStateHolder = Anonymous` | 页面启动 | 展示登录承接态，不发请求 | ViewModel |
| AND-BKG-02 | 已登录首屏成功 | session 有效，接口返回数据 | 初始化 | 渲染列表和 summary | ViewModel |
| AND-BKG-03 | online 空态 | 返回 `data=[]` 且 `onlineCount=0` | 首屏完成 | 展示 online 空态 | ViewModel |
| AND-BKG-04 | Tab 快切防乱序 | 两次请求返回顺序反转 | 连续切换 Tab | 只消费最后一次结果 | ViewModel |
| AND-BKG-05 | 追加失败不清空内容 | 已有一页内容 | 加载更多失败 | items 保留，appendErrorMessage 非空 | ViewModel |
| AND-BKG-06 | 401 退回登录承接态 | token 失效 | 刷新失败 | 清空旧内容并切到 login gate | ViewModel |
| AND-BKG-07 | booking 菜单导航 | 点击菜单“我的预约” | 动画关闭完成 | 导航到 `menu/booking` | Navigation |
| AND-BKG-08 | downloads 仍走占位页 | 点击菜单“我的下载” | 导航 | 进入 `PlaceholderScreen` | Navigation |
| AND-BKG-09 | 登录回流 booking | 登录由 booking 触发 | 登录成功 | 回 `menu/booking` | Navigation |
| AND-BKG-10 | DTO snake_case decode | 服务端返回 snake_case | 解析 DTO | `dramaId/episodeCount/bookedAt` 正确映射 | Repository/Data |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| `DramaRepository` | MockK | ViewModel 场景注入成功/失败/乱序结果 |
| `AuthSessionProvider` / `AuthStateHolder` | Fake / mock flow | 控制匿名、restoring、authenticated、expired |
| Retrofit 数据源 | MockK `ApiService` | 覆盖 DTO 与 query 参数 |
| navigation 回调 | lambda capture | 验证 `onRequireLogin(returnRoute)` 是否为 `menu/booking` |

---

## 13. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | — | — | 复用现有 Compose / Hilt / Retrofit / MockK / Turbine 即可 |

> ⚠️ 本方案不新增 Jetpack Security、EncryptedSharedPreferences 或任何新三方依赖。

---

## 14. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| `menu/booking` 仍被误当 placeholder | 导航与实现歧义 | 🔴 | 中 | 在 NavGraph 中把 booking route 明确替换为真实 screen | 若实现延期，至少在文档与代码中显式标注 pending |
| 401 时展示旧用户数据 | 账号安全与体验 | 🔴 | 中 | 401 立即清空页面状态并切回登录承接态 | 如短期难以完全收口，先禁止缓存复用 |
| summary 被客户端重算 | Tab 计数不一致 | 🔴 | 低 | 强制只读服务端 summary | 发现漂移时以后端为准强制覆盖 |
| request token 处理不当 | Tab 快切串页 | 🟡 | 中 | 直接复制 ranking 的 refresh/append token 模式 | 必要时先禁用并发追加 |
| booking DTO contract 漂移未修 | rank booking / assets decode 风险 | 🟡 | 中 | 一并审视 `BookDramaResponseDto` 与 booking assets DTO | 至少补 decode 回归测试 |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/features/app-shell/index.md` | 应用壳、菜单承接 | 菜单入口由应用壳统一承载 |
| `wiki/features/auth/index.md` | 登录状态与受保护资源 | 可复用既有登录与会话状态 |
| `wiki/features/ranking/index.md` | 预约与登录拦截 | ranking 已具备 booking 与 require-login 模式 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | `menu/booking` 与 `menu/downloads` 已存在 canonical route |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 当前 booking/downloads 都还是 placeholder 注册 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt` | 已有菜单关闭后导航状态机 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntries.kt` | 菜单静态入口已含 booking/downloads |
| `android/app/src/main/java/com/djs66256/short_drama/feature/common/ui/PlaceholderScreen.kt` | downloads 可继续复用占位页 |
| `android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthStateHolder.kt` | 已有 `authStatus` 与 `currentSession()` |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthSessionProvider.kt` | 已暴露 `accessToken()` / `isLoggedIn()` |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 当前无 `users/me/bookings`，需新增受保护接口 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | 可复用 refresh/append token 与 RequireLogin effect |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt` | 展示了 effect -> NavGraph 登录跳转模式 |
| `docs/specs/2026-07-30-prd-11-user-assets/spec.md` | 范围、交互、错误边界与 API contract |
| `docs/specs/2026-07-30-prd-11-user-assets/design.md` | 共享 schema、summary 口径、跨端约束 |
| `docs/specs/2026-07-27-prd-07-menu-panel/design-android.md` | 菜单抽屉与 placeholder 承接的 Android 基线 |
