# Android 端技术方案：PRD-01 底部导航与应用路由

> 创建日期：2026-07-25
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

Android 端延续现有的单 Activity + Jetpack Navigation Compose 架构，不新增后端 API、不新增开源依赖。在当前 `MainActivity + NavGraph + Home/Player/DramaDetail` 骨架上，补齐底部 5 Tab 容器、顶层嵌套导航图、多 back stack、deeplink 兼容与占位页承载。

```
┌──────────────────────────────────────────────────────────────┐
│ MainActivity                                                 │
│ └── MainAppScaffold                                          │
│     ├── NavigationBar（首页/剧场/商城/赚钱/我的）             │
│     └── NavHost（单 NavController）                          │
│         ├── home_graph                                       │
│         │   ├── home                                          │
│         │   ├── play/{videoId}        ← canonical             │
│         │   ├── player/{videoId}      ← legacy alias          │
│         │   ├── detail/{dramaId}      ← canonical             │
│         │   └── dramaDetail/{dramaId} ← internal alias        │
│         ├── theater_graph                                     │
│         │   └── theater                                        │
│         ├── mall_graph                                        │
│         │   └── mall                                           │
│         ├── earn_graph                                        │
│         │   └── earn                                           │
│         └── profile_graph                                     │
│             └── profile                                        │
└──────────────────────────────────────────────────────────────┘
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt` | 修改 | 从“仅挂载单个 `NavGraph`”扩展为根级 `MainAppScaffold` 容器，并接收冷启动 / 热启动 deeplink |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 重构为“顶层嵌套导航图 + 多 back stack + canonical/alias 路由” |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 首页增加示例入口，承接跳转 `play` / `detail` |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` | 修改 | 保持 `SavedStateHandle` 参数兼容，优先读 `videoId`，兼容通用 `id` |
| `android/app/src/main/java/com/djs66256/short_drama/feature/dramadetail/viewmodel/DramaDetailViewModel.kt` | 修改 | 保持 `SavedStateHandle` 参数兼容，优先读 `dramaId`，兼容通用 `id` |
| `feature/home/viewmodel/HomeViewModel.kt` | 不变 | 继续负责应用名 / 版本号展示，不承担导航状态 |
| Data / Domain / Network 层 | 不变 | 本期仅为导航骨架设计，不新增接口、Repository、DataSource |

### 1.2 设计原则

| 原则 | Android 落地方式 |
|------|------------------|
| 单容器承载 | 使用单 `NavController`，通过嵌套导航图承载 5 个一级频道 |
| 多 back stack | 使用 Navigation Compose 官方 `saveState + restoreState + popUpTo(findStartDestination())` 策略 |
| canonical 优先 | 公开播放路由统一为 `play/{videoId}`；旧 `player/{videoId}` 仅作兼容入口 |
| 参数兼容 | `SavedStateHandle` 内部继续使用 `videoId` / `dramaId`，避免现有 ViewModel 断裂 |
| 冷启动可延迟执行 | deeplink 先解析为待执行目标，待 `NavHost` ready 后再消费 |
| 占位页复用 | 4 个同构一级频道复用同一个占位 Composable，避免重复实现 |
| 无额外依赖 | 仅复用现有 Compose Material3、Navigation Compose、Hilt |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt` | 修改 | 增加根级 Scaffold、`onNewIntent` 分发、冷启动 deeplink 待执行注入 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 定义顶层图、子图、bottom bar 选中态、canonical/alias 路由注册 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 新增 | 集中维护 Tab、图路由、canonical/alias route builder、参数 key |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt` | 新增 | 解析 `Intent.data`，统一输出 canonical 目标 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt` | 新增 | 管理待执行 deeplink、消费状态与导航级降级 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/common/ui/PlaceholderScreen.kt` | 新增 | 复用剧场 / 商城 / 赚钱 / 我的占位页 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 增加 `onOpenPlay`、`onOpenDetail` 回调入口 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` | 修改 | `SavedStateHandle` 参数读取兼容 `videoId` 与 `id` |
| `android/app/src/main/java/com/djs66256/short_drama/feature/dramadetail/viewmodel/DramaDetailViewModel.kt` | 修改 | `SavedStateHandle` 参数读取兼容 `dramaId` 与 `id` |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 校验 canonical route、legacy alias route 与 deeplink 映射 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/DeeplinkRouteParserTest.kt` | 新增 | 覆盖 deeplink 解析、非法链接降级、冷启动待执行目标 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModelTest.kt` | 修改 | 增加 `SavedStateHandle` 兼容参数测试 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/dramadetail/viewmodel/DramaDetailViewModelTest.kt` | 新增 | 覆盖 `dramaId` / `id` 参数兼容 |

---

## 3. UI 层设计

### 3.1 组件层级树

```
MainActivity
└── MainAppScaffold
    ├── MainNavHost
    │   ├── HomeScreen
    │   │   ├── AppInfoHeader
    │   │   ├── PlayEntryButton
    │   │   └── DetailEntryButton
    │   ├── PlayerScreen
    │   ├── DramaDetailScreen
    │   ├── TabPlaceholderScreen(theater)
    │   ├── TabPlaceholderScreen(mall)
    │   ├── TabPlaceholderScreen(earn)
    │   └── TabPlaceholderScreen(profile)
    └── BottomNavigationBar
        ├── HomeItem
        ├── TheaterItem
        ├── MallItem
        ├── EarnItem
        └── ProfileItem
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `MainAppScaffold` | Composable | 提供 `Scaffold + NavigationBar + NavHost` 根壳层 | 否 |
| `BottomNavigationBar` | Composable | 渲染 5 个一级频道、计算选中态、触发多 back stack 切换 | 是 |
| `HomeScreen` | Composable | 首页占位页，展示应用信息及两个示例跳转入口 | 否 |
| `TabPlaceholderScreen` | Composable | 复用剧场 / 商城 / 赚钱 / 我的同构占位页 | 是 |
| `PlayerScreen` | Composable | 播放页占位，展示 `videoId` | 否 |
| `DramaDetailScreen` | Composable | 详情页占位，展示 `dramaId` | 否 |
| `LegacyRouteForwarder` | Composable | 处理 `player/{videoId}`、`dramaDetail/{dramaId}` 兼容跳转并立即转 canonical | 是 |

> 复用策略：根据“相同交互复用单一 case”约束，剧场 / 商城 / 赚钱 / 我的 4 个一级频道只保留一个通用 `TabPlaceholderScreen(title, description)` 实现，不拆 4 份同构页面。

### 3.3 Composable 接口定义

```kotlin
@Composable
fun MainAppScaffold(
    navController: NavHostController,
    pendingRoute: PendingRoute?,
    onPendingRouteConsumed: () -> Unit,
    modifier: Modifier = Modifier,
)

@Composable
fun HomeScreen(
    onOpenPlay: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
)

@Composable
fun TabPlaceholderScreen(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
)
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| 父 → 子 | Composable 参数 | `MainAppScaffold` 向 `HomeScreen` 传导航回调 |
| 子 → 父 | Lambda Callback | 首页按钮点击触发 `onOpenPlay` / `onOpenDetail` |
| 跨 Composable 共享 | Activity 级 `MainNavigationViewModel` | 冷启动 deeplink、热启动 deeplink、待执行状态 |
| 路由 → ViewModel | `SavedStateHandle` | `PlayerViewModel` / `DramaDetailViewModel` 读取参数 |
| NavHost → NavigationBar | `currentBackStackEntryAsState()` + destination hierarchy | 计算当前选中 Tab |

### 3.5 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | 延续单栏布局 | 本期不新增 `WindowSizeClass` 依赖，手机/大屏统一使用底部 `NavigationBar` |
| 横竖屏 | `rememberNavController` + Navigation Compose 状态恢复 | 旋转后保持当前 Tab 与当前子页面 |
| 折叠屏 | 不做专门双栏布局 | 本期目标仅为路由骨架，折叠态仍走底部导航 |
| 深色模式 | 复用现有 `ShortDramaTheme` | NavigationBar 和占位页跟随 Material3 颜色体系 |
| 全屏/安全区 | 复用现有 `enableEdgeToEdge()` | 根 Scaffold 统一处理 content padding |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 UI | 职责 |
|-----------|---------|------|
| `MainNavigationViewModel` | `MainActivity` / `MainAppScaffold` | 保存待执行 deeplink、消费后清理、提供降级目标 |
| `HomeViewModel` | `HomeScreen` | 继续提供 appName / appVersion 展示数据 |
| `PlayerViewModel` | `PlayerScreen` | 从 `SavedStateHandle` 提取 `videoId` |
| `DramaDetailViewModel` | `DramaDetailScreen` | 从 `SavedStateHandle` 提取 `dramaId` |

### 4.2 状态定义

```kotlin
@HiltViewModel
class MainNavigationViewModel @Inject constructor() : ViewModel() {

    data class UiState(
        val pendingRoute: PendingRoute? = null,
        val lastRejectedReason: NavigationErrorCode? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun enqueuePendingRoute(route: PendingRoute) {
        _uiState.update { it.copy(pendingRoute = route, lastRejectedReason = null) }
    }

    fun rejectPendingRoute(reason: NavigationErrorCode) {
        _uiState.update { it.copy(pendingRoute = null, lastRejectedReason = reason) }
    }

    fun consumePendingRoute() {
        _uiState.update { it.copy(pendingRoute = null) }
    }
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `pendingRoute` | `PendingRoute?` | `null` | 冷启动 / 热启动待执行的 canonical 导航目标 |
| `lastRejectedReason` | `NavigationErrorCode?` | `null` | 非法 deeplink 或空参数的拒绝原因，便于日志与测试断言 |
| `HomeUiState.isLoading` | `Boolean` | `true` | 现有首页状态，不新增导航语义 |
| `PlayerViewModel.videoId` | `String` | `""` | 播放页参数，来自 `SavedStateHandle` |
| `DramaDetailViewModel.dramaId` | `String` | `""` | 详情页参数，来自 `SavedStateHandle` |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | UI 层表现 |
|---------|---------|----------|
| RootReady | `pendingRoute == null` | 正常展示当前 Tab 或子页面 |
| PendingDeeplink | `pendingRoute != null` 且 NavHost 尚未消费 | 先展示默认首页壳层，随后自动跳转目标页 |
| InvalidDeeplinkFallback | 解析失败或参数非法 | 保持 / 回退首页 Tab，不弹出阻塞式错误框 |
| PlaceholderContent | 一级频道或二级页面无真实业务数据 | 展示占位标题、说明、路由参数 |

### 4.5 SavedStateHandle 参数兼容

本期重点不是把参数名统一改成抽象 `id`，而是在不破坏现有 ViewModel 的前提下兼容 canonical 命名。

```kotlin
@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val videoId: String =
        savedStateHandle.get<String>(AppDestination.Arg.VIDEO_ID)
            ?: savedStateHandle.get<String>(AppDestination.Arg.ID)
            ?: ""
}

@HiltViewModel
class DramaDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val dramaId: String =
        savedStateHandle.get<String>(AppDestination.Arg.DRAMA_ID)
            ?: savedStateHandle.get<String>(AppDestination.Arg.ID)
            ?: ""
}
```

设计结论：

- Android 端内部参数 key 继续以 `videoId` / `dramaId` 为主，兼容现有代码和测试。
- 若后续共享导航层引入统一 `id` 写法，ViewModel 无需同步重构即可兼容。
- route 名称从 `player` 迁移到 `play` 不影响 `SavedStateHandle` 读取，只要参数 key 不变。

---

## 5. Navigation 路由设计

### 5.1 导航方案

选择 **Jetpack Navigation Compose**，理由：

- 仓库已接入 `androidx.navigation:navigation-compose`，无需新增依赖。
- 现有 `MainActivity` 已是单 Activity + Compose 架构，最适合用嵌套导航图演进。
- 官方已支持顶层多 back stack 推荐写法，可直接满足 5 Tab 状态保持。

### 5.2 路由清单

| 路由标识 | 目标 Composable | 参数 | 导航方式 | 说明 |
|---------|-----------------|------|---------|------|
| `home_graph` | 顶层首页图 | — | Bottom tab switch | 首页频道根图 |
| `theater_graph` | 顶层剧场图 | — | Bottom tab switch | 剧场频道根图 |
| `mall_graph` | 顶层商城图 | — | Bottom tab switch | 商城频道根图 |
| `earn_graph` | 顶层赚钱图 | — | Bottom tab switch | 赚钱频道根图 |
| `profile_graph` | 顶层我的图 | — | Bottom tab switch | 我的频道根图 |
| `home` | `HomeScreen` | — | startDestination | 首页一级页面 |
| `theater` | `TabPlaceholderScreen` | — | startDestination | 剧场占位页 |
| `mall` | `TabPlaceholderScreen` | — | startDestination | 商城占位页 |
| `earn` | `TabPlaceholderScreen` | — | startDestination | 赚钱占位页 |
| `profile` | `TabPlaceholderScreen` | — | startDestination | 我的占位页 |
| `play/{videoId}` | `PlayerScreen` | `videoId` | `NavController.navigate` | canonical 播放页 |
| `player/{videoId}` | `LegacyRouteForwarder` | `videoId` | 兼容旧 route pattern | 立即转发到 `play/{videoId}` |
| `detail/{dramaId}` | `DramaDetailScreen` | `dramaId` | `NavController.navigate` | canonical 详情页 |
| `dramaDetail/{dramaId}` | `LegacyRouteForwarder` | `dramaId` | 兼容现有内部 route | 立即转发到 `detail/{dramaId}` |

### 5.3 顶层嵌套导航图与多 back stack 策略

```kotlin
NavHost(
    navController = navController,
    startDestination = AppDestination.Graph.HOME,
) {
    navigation(
        route = AppDestination.Graph.HOME,
        startDestination = AppDestination.Route.HOME,
    ) {
        composable(AppDestination.Route.HOME) {
            HomeScreen(
                onOpenPlay = { navController.navigate(AppDestination.play(it)) },
                onOpenDetail = { navController.navigate(AppDestination.detail(it)) },
            )
        }
        composable(AppDestination.Route.PLAY, arguments = AppDestination.playArguments) {
            PlayerScreen()
        }
        composable(AppDestination.Route.PLAYER_ALIAS, arguments = AppDestination.playArguments) {
            LegacyRouteForwarder(target = { args -> AppDestination.play(args.videoId) })
        }
        composable(AppDestination.Route.DETAIL, arguments = AppDestination.detailArguments) {
            DramaDetailScreen()
        }
        composable(AppDestination.Route.DRAMA_DETAIL_ALIAS, arguments = AppDestination.detailArguments) {
            LegacyRouteForwarder(target = { args -> AppDestination.detail(args.dramaId) })
        }
    }

    navigation(route = AppDestination.Graph.THEATER, startDestination = AppDestination.Route.THEATER) { ... }
    navigation(route = AppDestination.Graph.MALL, startDestination = AppDestination.Route.MALL) { ... }
    navigation(route = AppDestination.Graph.EARN, startDestination = AppDestination.Route.EARN) { ... }
    navigation(route = AppDestination.Graph.PROFILE, startDestination = AppDestination.Route.PROFILE) { ... }
}
```

`NavigationBar` 切换顶层频道时统一使用：

```kotlin
navController.navigate(tab.graphRoute) {
    launchSingleTop = true
    restoreState = true
    popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
    }
}
```

设计结论：

- 5 个一级频道各自拥有独立导航图，符合“顶层嵌套导航图”要求。
- `play` / `detail` 归属 `home_graph`，因此从首页进入子页面后切换 Tab，再切回首页时仍能保留子页面栈。
- 不为每个 Tab 创建独立 `NavController`，降低实现复杂度，直接使用 Navigation Compose 官方多 back stack 机制。

### 5.4 Deep Link 处理

| Deep Link Pattern | Canonical 目标 | 参数提取 | 说明 |
|------------------|---------------|---------|------|
| `djsdrama://open` | `home_graph -> home` | — | 打开首页 |
| `djsdrama://play/{videoId}` | `play/{videoId}` | `videoId` | canonical 播放 deeplink |
| `djsdrama://player/{videoId}` | `play/{videoId}` | `videoId` | 兼容旧 deeplink，统一归一到 `play` |
| `djsdrama://drama/{dramaId}` | `detail/{dramaId}` | `dramaId` | 兼容现有详情 deeplink |
| 其他 / 非法 deeplink | `home_graph -> home` | — | 降级回首页 |

### 5.5 play 为 canonical 且兼容 player deeplink/route pattern

这是本期 Android 方案的强约束：

1. 对外公开命名、文档命名、后续新入口命名统一使用 `play`。
2. 旧 `player/{videoId}` 不再作为主写法，但必须兼容：
   - 旧 deep link：`djsdrama://player/{videoId}`
   - 旧 route pattern：`player/{videoId}`
3. 所有兼容入口在 Android 端都先归一为 canonical `play/{videoId}`，再进入真实页面，避免同时存在两套播放页栈。

兼容实现建议：

- 入口侧：`DeeplinkRouteParser` 统一把 `player` host 解析为 `PendingRoute.Play(videoId)`。
- 端内侧：保留 `player/{videoId}` alias route，进入后立即 `navigate(play/{videoId})` 且 `popUpTo(alias) { inclusive = true }`。
- 新代码侧：禁止新增 `Routes.player(...)` 调用点，只允许使用 `AppDestination.play(...)`。

### 5.6 冷启动 deeplink 待执行策略

`MainActivity` 目前为 `singleTask`，冷启动和热启动都会进入同一个 Activity 生命周期。为避免“deeplink 到达时 NavHost 尚未 ready”导致的空转，本期采用待执行策略。

流程：

1. `onCreate(intent)` 或 `onNewIntent(intent)` 调用 `DeeplinkRouteParser.parse(intent.data)`。
2. 若解析成功，先写入 `MainNavigationViewModel.pendingRoute`，不直接访问 `navController`。
3. Compose 根层创建 `navController` 和 `NavHost`。
4. `LaunchedEffect(uiState.pendingRoute)` 在 `NavHost` ready 后执行一次导航。
5. 导航成功后 `consumePendingRoute()`；失败或非法参数则 `rejectPendingRoute()` 并回退首页。

```kotlin
LaunchedEffect(uiState.pendingRoute) {
    val route = uiState.pendingRoute ?: return@LaunchedEffect
    val success = navController.navigateToPendingRoute(route)
    if (success) {
        viewModel.consumePendingRoute()
    } else {
        navController.navigate(AppDestination.Graph.HOME) { launchSingleTop = true }
        viewModel.rejectPendingRoute(NavigationErrorCode.DEEPLINK_CONTAINER_NOT_READY)
    }
}
```

设计结论：

- 冷启动 deeplink 不再依赖“NavController 已立即可用”的时机巧合。
- 热启动 `onNewIntent` 同样复用同一套 pending 机制。
- 配合 `launchSingleTop = true`，可避免重复打开相同目标页。

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | 现有 Retrofit + OkHttp | 本期不涉及新增调用 |
| 数据模型 | 现有 DTO / Domain | 本期不新增模型 |
| 请求拦截器 | 现有 `AuthInterceptor` | 本期不变 |
| 响应解析 | 现有 kotlinx.serialization | 本期不变 |
| 错误处理 | 现有 `ApiResult<T>` | 本期不变 |

### 6.2 API 接口定义

本期不涉及、不新增 Android 端 API 调用。`HomeScreen`、一级频道占位页、`PlayerScreen`、`DramaDetailScreen` 均只消费本地路由参数或现有 `AppConfig` 数据，不依赖服务端响应。

### 6.3 请求重试策略

本期不涉及。无新增网络请求，因此无新增重试策略。

### 6.4 网络状态监听

本期不涉及。导航骨架不引入新的网络联动逻辑。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| 导航多 back stack 状态 | Navigation Compose 内建保存 | 内部 back stack saved state | 跟随系统恢复 | 本期使用框架能力，不新增持久化组件 |
| 页面参数恢复 | `SavedStateHandle` | `videoId` / `dramaId` / `id` | 跟随 ViewModel 生命周期 | 用于进程重建后的参数恢复 |
| 业务数据 / 页面缓存 | — | — | — | 本期不涉及 / 不新增 |
| 用户偏好 / 本地数据库 | — | — | — | 本期不涉及 / 不新增 |

### 7.2 Room 实体设计（如适用）

本期不涉及，不新增 Room。

### 7.3 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| Tab 导航栈 | 由 Navigation Compose `saveState/restoreState` 管理 | 运行时 / 系统可恢复窗口 | 系统资源回收时允许丢失并回根页 |
| 路由参数 | `SavedStateHandle` | 跟随 back stack entry | entry 销毁即释放 |

### 7.4 数据库 Migration

本期不涉及 / 不新增。

---

## 8. 配置与环境

| 配置项 | 管理方式 | 开发环境值 | 生产环境值 | 说明 |
|--------|---------|----------|-----------|------|
| deeplink scheme | `AndroidManifest.xml` | `djsdrama` | `djsdrama` | 复用现有 scheme，不新增环境变量 |
| 路由常量 | `navigation/AppDestination.kt` | 固定常量 | 固定常量 | 集中管理，避免在 UI 中散落字符串 |
| 应用名 / 版本号 | 现有 `AppConfig` / `BuildConfig` | 现有配置 | 现有配置 | `HomeViewModel` 继续读取现有配置 |

说明：

- 本期不新增 `BuildConfig` 字段。
- 本期不新增 feature flag。
- 导航 route 虽然是常量，但属于端内协议，不属于被禁止的环境地址硬编码范畴；仍需统一集中维护，避免散落在多个 Composable 中。

---

## 9. API 调用清单

本期无新增 API 调用。

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| — | — | — | — | 本期不涉及 / 不新增 |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Android 端实现方式 |
|---------|---------------|-------------------|
| 底部 5 Tab 定义 | 首页 / 剧场 / 商城 / 赚钱 / 我的 | `AppDestination.TopLevelTab` 集中定义，`NavigationBar` 固定顺序渲染 |
| 默认落地规则 | 冷启动默认 `home` | `NavHost.startDestination = home_graph` |
| 二级路由归属 | `play` / `detail` 归属首页频道容器 | 置于 `home_graph` 下，保证切 Tab 后再返回能恢复子页面栈 |
| `play` canonical | 对外统一用 `play` | Android 统一提供 `AppDestination.play(videoId)` 作为唯一新入口 |
| `player` 兼容 | 接受旧 deeplink / route pattern | `DeeplinkRouteParser` + alias route forwarder 双重兼容 |
| deeplink 兜底 | 非法 deeplink 回首页 | 解析失败时 enqueue fallback 或直接回 `home_graph` |
| 状态保持 | 切换 Tab 保留栈和局部状态 | `restoreState + saveState + popUpTo(findStartDestination())` |
| 占位页策略 | 一级频道与子页面均可渐进替换 | 一级同构页复用 `TabPlaceholderScreen`，播放/详情继续使用现有独立占位页 |

---

## 11. 边界与错误处理

### 11.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| deeplink 解析层 | `DeeplinkRouteParser` 返回受限 `PendingRoute?` | 只接受白名单 host / path |
| 导航状态层 | `MainNavigationViewModel.rejectPendingRoute()` | 记录非法输入或容器未就绪 |
| UI 层 | 保持当前页或回首页 | 本期不弹业务错误页，不阻塞用户使用 |
| 日志 | Android `Log` | 记录非法 deeplink、参数缺失、alias 转 canonical 失败 |

### 11.2 错误码映射表

| 端内错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `INVALID_ROUTE_PARAMS` | 页面参数无效 | 阻止跳转并回首页或当前 Tab 根页 |
| `UNSUPPORTED_ROUTE` | 暂不支持该页面 | 回首页 |
| `NAVIGATION_STATE_LOST` | 已返回频道首页 | 恢复到对应 Tab 根页面 |
| `TAB_STATE_RESTORED_PARTIALLY` | 页面已重新加载 | 静默恢复，不额外弹窗 |
| `DEEPLINK_CONTAINER_NOT_READY` | 正在打开页面 | 先缓存待执行目标，稍后自动跳转 |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| 旧 `player` route 被调用 | 现存代码仍调用 `player/{videoId}` | alias route 立即重定向到 canonical `play/{videoId}` | 🔴 |
| 冷启动收到 deeplink | `onCreate` 时 `NavHost` 未 ready | 先入 `pendingRoute`，待根容器 ready 后消费 | 🔴 |
| 空参数 deeplink | `djsdrama://play/` 或 `player/` 无 id | 拒绝导航并回首页 | 🔴 |
| 在首页子页面切换 Tab | 当前位于 `play` / `detail` | 使用多 back stack 保留首页子栈 | 🔴 |
| 系统回收部分状态 | 后台后进程被杀 | 允许退化为对应 Tab 根页面 | 🟡 |
| 快速重复点击同一路由 | 300ms 内连续点击播放入口 | `launchSingleTop` + route normalize，避免重复叠栈 | 🟡 |
| 重复打开相同 deeplink | Activity 为 `singleTask` 再次收到 intent | `setIntent(intent)` + pendingRoute 去重 | 🟡 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `MainAppScaffold` | 不单独展示 loading | 正常展示当前路由 | 不适用 | 不适用 | deeplink 非法时回首页 |
| `HomeScreen` | 复用现有 `isLoading` | 展示 app 信息和入口按钮 | 不适用 | 不适用 | 不适用 |
| `TabPlaceholderScreen` | 不适用 | 展示频道占位文案 | 频道无真实数据时即为默认成功态 | 不适用 | 不适用 |
| `PlayerScreen` | 不适用 | 展示 `videoId` 占位内容 | 参数为空时不进入此页 | 不适用 | 参数非法时回首页 |
| `DramaDetailScreen` | 不适用 | 展示 `dramaId` 占位内容 | 参数为空时不进入此页 | 不适用 | 参数非法时回首页 |

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | route builder、deeplink 解析、ViewModel 参数兼容、pendingRoute 状态流转 | 导航契约相关核心逻辑 100% | JUnit4 + MockK + kotlinx-coroutines-test |
| ViewModel 测试 | `HomeViewModel`、`PlayerViewModel`、`DramaDetailViewModel`、`MainNavigationViewModel` | 关键状态与参数路径全覆盖 | JUnit4 + Turbine |
| 手工回归 | 多 back stack、冷启动 deeplink、热启动 deeplink、底部选中态 | 覆盖 P0 场景 | 模拟器 / 真机 |

> 说明：遵循当前 Android 目录约束，本期优先在 `src/test` 做纯 JVM 测试；多 back stack 的最终体验通过手工回归补齐。

### 12.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| A-01 | canonical 播放路由生成 | 给定 `videoId=abc123` | 调用 `AppDestination.play("abc123")` | 返回 `play/abc123` | 单元 |
| A-02 | legacy `player` deeplink 兼容 | 给定 `djsdrama://player/abc123` | 解析 deeplink | 输出 canonical `PendingRoute.Play("abc123")` | 单元 |
| A-03 | 非法 deeplink 降级 | 给定 `djsdrama://unknown` | 解析 deeplink | 返回 `null` 或 fallback home | 单元 |
| A-04 | `PlayerViewModel` 参数兼容 | `SavedStateHandle(videoId=123)` / `SavedStateHandle(id=123)` | 创建 ViewModel | `videoId == "123"` | 单元 |
| A-05 | `DramaDetailViewModel` 参数兼容 | `SavedStateHandle(dramaId=456)` / `SavedStateHandle(id=456)` | 创建 ViewModel | `dramaId == "456"` | 单元 |
| A-06 | 冷启动待执行导航 | `pendingRoute = Play(123)` | 根容器 ready 后消费 | 触发一次导航并清空 pendingRoute | 单元 |
| A-07 | 多 back stack 体验回归 | 首页进入播放页后切到商城再切回首页 | 用户回到首页 Tab | 仍位于原播放页上下文 | 手工回归 |
| A-08 | 底部 Tab 默认选中态 | 冷启动应用 | 首次渲染完成 | 首页 Tab 高亮，其余未选中 | 手工回归 |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| `SavedStateHandle` | 直接构造 map | 无需 Android Framework 依赖 |
| `AppConfig` | MockK | 继续测试 `HomeViewModel` |
| deeplink 输入 | 构造 `Uri` / 字符串 | 验证 parser 白名单行为 |
| 协程状态流 | Turbine | 验证 `pendingRoute` 的 enqueue / consume / reject |

---

## 13. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | — | — | 本期完全复用现有 Compose Material3、Navigation Compose、Hilt、JUnit 体系 |

> 本期不新增开源依赖。

---

## 14. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| alias 路由和 canonical 路由并存，导致播放页重复叠栈 | 播放页导航 | 🔴 | 中 | 所有 `player` 入口立即归一到 `play`，并对 alias route `inclusive pop` | 保留 alias 但禁止新增调用点 |
| 多 back stack 配置错误，Tab 切换丢状态 | 首页、商城等 5 个频道 | 🔴 | 中 | 严格使用官方 `saveState/restoreState/popUpTo(findStartDestination())` 模式 | 降级为“切 Tab 回根页”，但保持可用 |
| 冷启动 deeplink 早于 `NavHost` 就绪 | deeplink 流程 | 🔴 | 高 | 引入 `pendingRoute` 待执行策略 | 解析失败统一回首页 |
| `SavedStateHandle` 参数名调整导致现有 ViewModel 失效 | 播放页、详情页 | 🟡 | 中 | 保留 `videoId` / `dramaId` 主 key，同时兼容 `id` | 回滚到仅旧 key 模式 |
| 4 个一级频道各自写一份占位页，后续维护发散 | UI 占位组织 | 🟢 | 中 | 使用单一 `TabPlaceholderScreen` 复用 | 若后续频道差异化再拆分 |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/index.md` | 功能索引 | wiki 以功能域、API、架构、决策组织 |
| `wiki/features/app-shell/index.md` | Android 入口与已知限制 | 当前 Android 仍为单页骨架，尚未实现底部导航 |
| `wiki/features/deeplink/index.md` | Android Deeplink / 已知限制 | 已声明 `djsdrama://` scheme，Android 仍为骨架阶段 |
| `wiki/features/video-player/index.md` | 入口与路由 / 已知限制 | 播放器当前为占位能力，Android 具体承载尚待补齐 |
| `wiki/architecture/overview.md` | 跨端涉及 / 技术栈总览 | 确认 Android 技术栈为 Compose + Navigation Compose + Hilt |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `docs/specs/2026-07-25-prd-01-bottom-nav/spec.md` | 明确 5 Tab、状态保持、deeplink 兼容、`play` canonical 目标 |
| `docs/specs/2026-07-25-prd-01-bottom-nav/design.md` | 共享导航契约、canonical/alias 原则、端内错误码 |
| `android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt` | 当前仅创建 `NavController` 并挂载单一 `NavGraph` |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 当前 route 为 `home` / `player/{videoId}` / `dramaDetail/{dramaId}` |
| `android/app/src/main/AndroidManifest.xml` | `MainActivity` 已声明 `singleTask` 与 `djsdrama` scheme |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 当前首页仅展示应用信息，无跳转入口 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` | 当前仅从 `SavedStateHandle` 读取 `videoId` |
| `android/app/src/main/java/com/djs66256/short_drama/feature/dramadetail/viewmodel/DramaDetailViewModel.kt` | 当前仅从 `SavedStateHandle` 读取 `dramaId` |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt` | 播放页已具备最小占位 UI |
| `android/app/src/main/java/com/djs66256/short_drama/feature/dramadetail/ui/DramaDetailScreen.kt` | 详情页已具备最小占位 UI |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 当前仅覆盖旧 `player` / `dramaDetail` route 生成 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt` | 首页现有状态来源为 `AppConfig` |
| `android/app/build.gradle.kts` | 已接入 Navigation Compose、Hilt、Compose Material3，无需新增依赖 |
| `android/gradle/libs.versions.toml` | 确认 Navigation Compose / Hilt / Test 版本现成可用 |
| `.claude/skills/feature-workflow/assets/design-android-template.md` | 本文档结构模板 |
| `.claude/skills/feature-workflow/references/android-design/arch-design.md` | Android 端设计阶段要求与交付口径 |
