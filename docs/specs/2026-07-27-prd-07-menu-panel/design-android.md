# Android 端技术方案：PRD-07 菜单面板

> 创建日期：2026-07-27
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

Android 端在现有单 Activity + Navigation Compose + Hilt + ViewModel + Repository 架构上，为首页增加一个由 `NavGraph` 外层 `Scaffold` 承载的左侧抽屉式菜单面板。实现继续遵循 `android/CLAUDE.md` 约束，不新增第三方依赖，复用现有 Compose + StateFlow + Retrofit 体系完成抽屉 Overlay、最近在看数据链路与占位承接导航。

```text
NavGraph
  -> Scaffold
     -> bottomBar
     -> Box(content + drawer overlay)
        -> HomeScreen top bar menu button
           -> MainNavigationViewModel.openMenuPanel()
        -> MenuPanelDrawer
           -> MenuPanelViewModel.loadIfNeeded()
              -> GetRecentlyViewedUseCase
                 -> MenuPanelRepository.getRecentlyViewed()
                    -> MenuPanelRemoteDataSource.getRecentlyViewed(sessionId)
                       -> ApiService.getRecentlyViewed(playbackSessionId)
        -> tap recently viewed card
           -> close drawer
           -> navController.navigate(AppDestination.play(dramaId))
        -> tap login/messages/booking/download
           -> close drawer
           -> navController.navigate(AppDestination.menuPlaceholder(kind))
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 在外层 `Scaffold` 上叠加 drawer overlay |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 新增菜单占位承接 routes |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 顶部栏增加菜单按钮回调 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/common/ui/PlaceholderScreen.kt` | 复用 / 扩展 | 继续作为菜单占位页基础容器 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增 `GET player/recently-viewed` |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/PlaybackSessionStore.kt` | 不变 / 复用 | 继续提供 get-or-create session id |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt` | 修改 | 增加抽屉开关状态、待导航目标与关闭完成 effect |

### 1.2 新增子域划分

1. 壳层抽屉状态管理：由 `MainNavigationViewModel` / `NavGraph` 持有，避免只挂在 `HomeScreen`。
2. 菜单 UI 展示：登录引导、消息预览、最近在看、游戏中心、常用功能。
3. 最近在看数据链路：session store → remote datasource → repository → use case → view model。
4. 占位页承接：登录 / 消息 / 我的预约 / 我的下载通过 home graph 新增 route 承接。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 在 `Scaffold` 外层内容区叠加 drawer overlay 和蒙层 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 新增 `menu/login`、`menu/messages`、`menu/booking`、`menu/downloads` routes |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt` | 修改 | 增加 `menuPanelState` 与开关方法 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | `HomeTopBar` 增加菜单按钮与 contentDescription |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelDrawer.kt` | 新增 | Drawer 容器、蒙层、动画 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelScreen.kt` | 新增 | 菜单内容根 Composable |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/components/*` | 新增 | 登录头部、消息区、最近在看区、游戏区、常用功能区组件 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt` | 新增 | 最近在看区状态机 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/RecentlyViewed.kt` | 新增 | 最近在看领域模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/MenuPanelRepository.kt` | 新增 | 菜单面板数据接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetRecentlyViewedUseCase.kt` | 新增 | 最近在看读取用例 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/RecentlyViewedResponseDto.kt` | 新增 | 对齐后端 contract |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/MenuPanelRemoteDataSource.kt` | 新增 | 包装 recently-viewed API 请求 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/MenuPanelRepositoryImpl.kt` | 新增 | DTO -> Domain 映射 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 注入 `MenuPanelRepository` |
| `android/app/src/test/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModelTest.kt` | 新增 | 覆盖最近在看加载、空态、错误、重试 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt` | 新增 / 修改 | 覆盖菜单开关与导航错误处理 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/MenuPanelRepositoryImplTest.kt` | 新增 | DTO 映射测试 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 验证菜单占位 routes 构造 |

---

## 3. UI 层设计

### 3.1 组件层级树

```text
NavGraph
└── Scaffold
    ├── NavigationBar
    └── Box
        ├── NavHost
        └── MenuPanelDrawer
            ├── DimmingScrim
            └── Surface(width = screen * 0.78)
                └── MenuPanelScreen
                    ├── MenuLoginHeader
                    ├── MenuMessagePreview
                    ├── MenuRecentlyViewedSection
                    │   ├── LoadingState
                    │   ├── EmptyState
                    │   ├── ErrorState
                    │   └── RecentlyViewedCard x N
                    ├── MenuGameCenterSection
                    └── MenuCommonFunctionsSection
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `MenuPanelDrawer` | Composable | 壳层抽屉、蒙层、动画和点击拦截 | 否 |
| `MenuPanelScreen` | Composable | 菜单内容容器 | 否 |
| `MenuLoginHeader` | Composable | 匿名登录引导区 | 否 |
| `MenuMessagePreview` | Composable | 单条静态消息区 | 否 |
| `MenuRecentlyViewedSection` | Composable | 最近在看三态与卡片列表 | 否 |
| `RecentlyViewedCard` | Composable | 单张续播卡片 | 否 |
| `MenuGameCenterSection` | Composable | 四宫格游戏入口与“即将上线”提示 | 否 |
| `MenuCommonFunctionsSection` | Composable | 我的预约 / 我的下载入口 | 否 |
| `PlaceholderScreen` | Composable | 登录 / 消息 / 预约 / 下载承接页 | 是 |

### 3.3 `HomeScreen` 顶部栏调整

当前 `HomeTopBar` 只有搜索按钮，需要扩展为：

```kotlin
@Composable
private fun HomeTopBar(
    onOpenMenu: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    Row(...) {
        IconButton(onClick = onOpenMenu) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Menu,
                contentDescription = HOME_MENU_ENTRY_CONTENT_DESCRIPTION,
            )
        }
        Text(text = "首页", ...)
        IconButton(onClick = onOpenSearch) { ... }
    }
}
```

### 3.4 交互规则

| 交互 | 规则 |
|------|------|
| 点击汉堡按钮 | 打开抽屉 |
| 点击蒙层 | 关闭抽屉 |
| Android 系统返回 | 优先关闭抽屉，其次 pop 路由 |
| 点击最近在看卡片 | 先关闭抽屉，再进入 `play/{videoId}` |
| 点击登录 / 消息 / 预约 / 下载 | 先关闭抽屉，再进入对应 placeholder route |
| 点击游戏图标 | 不导航，弹出 Snackbar / Toast “即将上线” |
| 切换底部 Tab | 若抽屉打开则先关闭，再切 tab |

### 3.5 壳层承载方式

- `MenuPanelDrawer` 放在 `NavGraph` 的 `Scaffold` 内容层之上，以确保覆盖 `NavHost` 和 `NavigationBar`；
- `shouldShowBottomBar()` 不因抽屉打开而隐藏底部栏，但抽屉打开时底部栏被蒙层拦截不可点击；
- 使用 `BackHandler(enabled = uiState.isMenuOpen)` 统一处理返回关闭；
- 所有菜单内导航都通过 `closeMenuThenNavigate(targetRoute)` effect 串行执行：先切到 `CLOSING`，由 `MenuPanelDrawer` 在关闭动画完成后发回 `onMenuClosedAnimationFinished()`，再由 `NavGraph` 消费待导航 route；closing 期间忽略重复点击。

---

## 4. ViewModel 设计

### 4.1 Navigation 层状态

在 `MainNavigationViewModel` 中新增：

```kotlin
enum class MenuPanelPresentationState {
    CLOSED,
    OPENING,
    OPEN,
    CLOSING,
}

data class MainNavigationUiState(
    ...,
    val menuPanelState: MenuPanelPresentationState = MenuPanelPresentationState.CLOSED,
)
```

职责：
- 控制抽屉的打开 / 关闭；
- 持有 `pendingMenuRoute`，并只在关闭动画完成后发出真正的导航 effect；
- closing 中拒绝新的菜单导航请求，避免重复压栈；
- 当容器未 ready 或 route 非法时通过现有 error code / event 体系兜底。

### 4.2 菜单 ViewModel

```kotlin
data class MenuPanelUiState(
    val items: List<RecentlyViewed> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasLoaded: Boolean = false,
)

@HiltViewModel
class MenuPanelViewModel @Inject constructor(
    private val getRecentlyViewedUseCase: GetRecentlyViewedUseCase,
    private val playbackSessionStore: PlaybackSessionStore,
) : ViewModel() {
    ...
}
```

### 4.3 状态机规则

| 状态 | 触发条件 | UI 表现 |
|------|---------|---------|
| 初始 | 尚未打开抽屉 | 不请求数据 |
| Loading | 首次打开或手动重试 | 最近在看区 loading |
| Success | `items.isNotEmpty()` | 渲染列表 |
| Empty | 成功但 `items.isEmpty()` | 空态文案 |
| Error | session / network / service 异常 | 错误文案 + 重试 |

### 4.4 核心行为

- `loadIfNeeded()`：抽屉首次打开时调用，若已加载则避免重复请求；
- 调用链：`playbackSessionStore.getOrCreateSessionId()` → `getRecentlyViewedUseCase(sessionId)`；
- `retry()`：仅保留一个 in-flight 请求；
- 当抽屉关闭时不强制取消请求，但返回结果不应触发抽屉重新显示；
- 点击最近在看前校验 `dramaId.isNotBlank()`，否则通过轻提示兜底。

---

## 5. Navigation 路由设计

### 5.1 Route 扩展

在 `AppDestination.Route` 中新增：

```kotlin
const val MENU_LOGIN = "menu/login"
const val MENU_MESSAGES = "menu/messages"
const val MENU_BOOKING = "menu/booking"
const val MENU_DOWNLOADS = "menu/downloads"
```

并增加 helper：

```kotlin
fun menuLogin(): String = Route.MENU_LOGIN
fun menuMessages(): String = Route.MENU_MESSAGES
fun menuBooking(): String = Route.MENU_BOOKING
fun menuDownloads(): String = Route.MENU_DOWNLOADS
```

### 5.2 `NavGraph` 注册

在 home graph 中新增 composable：

```kotlin
composable(route = AppDestination.Route.MENU_LOGIN) {
    PlaceholderScreen(
        title = "登录",
        description = "登录功能建设中，当前为 Native 承接页。",
    )
}
```

消息 / 预约 / 下载同理。

### 5.3 导航封装建议

新增统一机制：

```kotlin
fun closeMenuThenNavigate(targetRoute: String)
fun onMenuClosedAnimationFinished()
```

规则：
- `closeMenuThenNavigate(targetRoute)` 只记录 `pendingMenuRoute=targetRoute` 并把状态切到 `CLOSING`；
- `MenuPanelDrawer` 在 `AnimatedVisibility` / `DrawerState` 关闭动画真正结束后回调 `onMenuClosedAnimationFinished()`；
- 只有在该回调里，`NavGraph` 才消费 `pendingMenuRoute` 并 `navController.navigate(route)`；
- 如果 `pendingMenuRoute` 为空，则仅完成关闭；
- closing 中再次点击入口时忽略，保证一次只执行一个菜单导航。

---

## 6. 网络层设计

### 6.1 Retrofit 接口扩展

```kotlin
@GET("player/recently-viewed")
suspend fun getRecentlyViewed(
    @Header("X-Playback-Session-Id") playbackSessionId: String,
): RecentlyViewedResponseDto
```

### 6.2 DTO 设计

```kotlin
@Serializable
data class RecentlyViewedResponseDto(
    val code: Int,
    val data: RecentlyViewedDataDto,
    val message: String,
)

@Serializable
data class RecentlyViewedDataDto(
    val items: List<RecentlyViewedItemDto>,
)

@Serializable
data class RecentlyViewedItemDto(
    @SerialName("drama_id") val dramaId: String,
    val title: String,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("episode_number") val episodeNumber: Int,
    val progress: Double,
    @SerialName("updated_at") val updatedAt: String,
)
```

### 6.3 Repository 设计

- `MenuPanelRemoteDataSource` 负责把 `ApiService` 结果包装为 `ApiResult`; 
- `MenuPanelRepositoryImpl` 负责 DTO -> Domain 映射；
- 不把 recently-viewed 混入 `PlayerRepository`，避免播放器读写与菜单只读聚合职责混杂。

---

## 7. 数据持久化策略

| 数据类型 | 存储方案 | 说明 |
|---------|---------|------|
| 抽屉开关状态 | 不持久化 | 只保留当前会话 |
| 最近在看列表 | ViewModel 内存态 | 当前菜单会话内可复用 |
| playback session id | 继续复用 `PlaybackSessionStore` (DataStore) | 不新增新 key |
| Placeholder 文案 | 代码静态常量 / enum | 不依赖后端配置 |

---

## 8. 测试策略

### 8.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| ViewModel | 最近在看加载、空态、错误、重试、非法 dramaId | JUnit4 + Turbine + MockK |
| Navigation | 菜单开关、BackHandler 关闭、关闭完成后导航、closing 防重入、route 构造 | JUnit4 |
| Repository | DTO 映射、error 透传、`cover_url=null` | JUnit4 + MockK |
| DataSource | Retrofit 调用 header 透传 | JUnit4 |

### 8.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 |
|------|---------|------|---------|------|
| AND-T01 | 点击首页菜单按钮 | `onOpenMenu()` | `menuPanelState=OPEN` | Navigation |
| AND-T02 | 首次打开抽屉加载成功 | 返回 3 条 items | 最近在看区进入 success | ViewModel |
| AND-T03 | 无历史 | 返回空数组 | 空态文案 | ViewModel |
| AND-T04 | session store 异常 | `getOrCreateSessionId` throw | error state | ViewModel |
| AND-T05 | 点击最近在看 | `dramaId=abc` | 先进入 `CLOSING`，待 `onMenuClosedAnimationFinished()` 后导航 `play/abc` | Navigation |
| AND-T06 | 点击登录入口 | login | 先进入 `CLOSING`，待关闭完成后导航 `menu/login` | Navigation |
| AND-T07 | closing 中重复点击 | 连续点击两个菜单入口 | 只消费第一个待导航目标，不重复压栈 | Navigation |
| AND-T08 | 返回键 | 抽屉打开时 back | 关闭抽屉，不 pop 页面 | Navigation |
| AND-T09 | retrofit header | valid session | 请求带 `X-Playback-Session-Id` | DataSource |
| AND-T10 | `cover_url=null` | DTO 封面为空 | Repository 正常映射 | Repository |

### 8.3 不在本期测试范围

- Compose 截图测试；
- 真机手势交互专项；
- 黑盒体验验证（留到 QA 阶段）。

---

## 9. 参考资料

| 文件 | 关键内容 |
|------|---------|
| `docs/specs/2026-07-27-prd-07-menu-panel/spec.md` | 菜单面板与最近在看需求 |
| `docs/specs/2026-07-27-prd-07-menu-panel/design.md` | shared contract、状态机、错误语义 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | `Scaffold` 与 bottom bar 承载层 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | route helper 扩展点 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 首页菜单按钮接入点 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/common/ui/PlaceholderScreen.kt` | 菜单占位页复用基础 |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/PlaybackSessionStore.kt` | get-or-create session id |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | recently-viewed endpoint 扩展点 |
