# Android 端技术方案：PRD-13 商城

> 创建日期：2026-07-28
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

```text
NavGraph.mall graph
→ MallScreen
  → MallViewModel
  → MallWebViewContainer (AndroidView/WebView)
  → MallContainerStateContent (loading / error)

MallWebView bridge
→ mall.openSearch(payload)
  → navigator.openSearch(from = MALL, returnTarget = "/mall")
  → search close/back => restoreMallContext(reason = SearchReturn)
→ mall.requestLogin(payload)
  → navController.navigate(AppDestination.mallLogin(...))
  → close/cancel/success => restoreMallContext(reason = LoginReturn)
  → syncMallAuthStateToWebView()
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `android/.../navigation/NavGraph.kt` | 修改 | `mall` graph 从 `PlaceholderScreen` 改为 `MallScreen` + `mall/login` 路由 |
| `android/.../navigation/AppDestination.kt` | 修改 | 新增 mall login route / helper |
| `android/.../core/config/AppConfig.kt` | 扩展 | 新增 `mallBaseUrl` 配置 |
| `android/.../feature/mall/` | 新增 | 商城容器、bridge、宿主态组件、ViewModel |
| `android/.../domain/repository/AuthSessionProvider.kt` | 复用 | 用于登录承接返回后的登录态刷新语义 |
| `android/.../feature/ranking/viewmodel/RankingViewModel.kt` | 不变 | 仅参考 `RequireLogin(returnRoute)` 的 effect 模式 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | mall graph 接入真实容器与 mall login route |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 新增 `Route.MALL_LOGIN`、`mallLogin(...)` helper |
| `android/app/src/main/java/com/djs66256/short_drama/core/config/AppConfig.kt` | 修改 | 扩展 `mallBaseUrl` |
| `android/app/src/main/java/com/djs66256/short_drama/core/config/BuildConfigAppConfig.kt` | 修改 | 从 BuildConfig 暴露 mall H5 base URL |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallScreen.kt` | 新增 | mall tab 根 Composable |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallWebViewContainer.kt` | 新增 | WebView 封装与 bridge 接线 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallLoginScreen.kt` | 新增 | mall 专属全屏登录承接页 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt` | 新增 | 容器状态、bridge effect、返回恢复 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/model/MallLoginContext.kt` | 新增 | mall 登录上下文模型 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModelTest.kt` | 新增 | mall ViewModel 状态与 effect 测试 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt` | 修改 | 增补 mall graph 与登录 route 测试 |

---

## 3. UI 层设计

### 3.1 组件层级树

```text
MallScreen
├── MallContainerStateContent (loading / error)
├── MallWebViewContainer
└── MallLoginScreen (separate route)
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `MallScreen` | Composable | mall tab 根页面，装配 ViewModel | 否 |
| `MallWebViewContainer` | Composable | 通过 `AndroidView` 承载 WebView，注册 JS bridge | 否 |
| `MallContainerStateContent` | Composable | loading / error 宿主态 UI | 否 |
| `MallLoginScreen` | Composable | mall 专属登录承接占位页 | 否 |

### 3.3 Composable 接口定义

```kotlin
@Composable
fun MallScreen(
    viewModel: MallViewModel = hiltViewModel(),
    onOpenSearch: () -> Unit,
    onOpenMallLogin: (MallLoginContext) -> Unit,
)

@Composable
fun MallWebViewContainer(
    url: String,
    modifier: Modifier = Modifier,
    onPageStateChanged: (MallPageEvent) -> Unit,
    onBridgeMessage: (MallBridgeMessage) -> Unit,
)
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| 父 → 子 | Composable 参数 | WebView URL、状态、导航回调 |
| 子 → 父 | Lambda callback | 页面加载结果、bridge 消息 |
| 跨 Composable 共享 | `MallViewModel` | 容器状态与待登录上下文 |
| Fragment → Fragment | 不使用 | 继续单 Activity + NavHost 模式 |

### 3.5 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | WebView 全尺寸铺满内容区 | H5 自己控制移动布局 |
| 横竖屏 | ViewModel 持有状态 + WebView 尽量复用 | 旋转后避免回退 placeholder |
| 折叠屏 | 宿主态自适应；H5 不做原生特化 | 首版不新增折叠屏定制 UI |
| 深色模式 | 宿主态跟随 MaterialTheme | H5 主题由 Web 控制 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 UI | 职责 |
|-----------|---------|------|
| `MallViewModel` | `MallScreen` | 管理容器状态、bridge 事件、登录承接返回恢复 |

### 4.2 状态定义

```kotlin
class MallViewModel(
    private val appConfig: AppConfig,
) : ViewModel() {
    data class UiState(
        val state: MallContainerState = MallContainerState.Loading,
        val currentUrl: String = "",
        val pendingLoginContext: MallLoginContext? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `state` | `MallContainerState` | `Loading` | 宿主级 loading / success / error |
| `currentUrl` | `String` | `mallHomeUrl` | 当前 WebView URL |
| `pendingLoginContext` | `MallLoginContext?` | `null` | 待登录承接的商品上下文 |
| `lastLoadedHomeUrl` | `String?` | `null` | 成功加载首页后的恢复目标 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | UI 层表现 |
|---------|---------|----------|
| Loading | `state is Loading` | CircularProgressIndicator / skeleton |
| Success | `state is Success` | 展示 WebView |
| Error (可重试) | `state is Error` | 错误说明 + 重试按钮 |
| Error (不可重试) | 暂不单独建模 | 首版统一为可重试 |

---

## 5. Navigation 路由设计

### 5.1 导航方案

- 继续使用 Jetpack Navigation Compose。
- mall 首页属于 `Graph.MALL`，保持底部 mall tab 作为一级承接。
- 搜索 bridge 继续复用 `AppDestination.search()`，允许临时进入 HOME graph，但导航层必须记录来源为 mall 与 `returnTarget=/mall`。
- 搜索关闭 / 回退时必须执行 `restoreMallContext(reason = SearchReturn)`：重新显示 mall root，并向 H5 发送 `mall.restoreContext(reason='search-return')`；若容器已重建则直接重载商城首页。
- 登录承接新增 `mall/login` route，语义与 `menu/login` 分离。
- 登录关闭 / 取消 / 成功时必须先同步 `mall.syncAuthState`，再执行 `restoreMallContext(reason = LoginReturn)`。

### 5.2 路由清单

| 路由标识 | 目标 Composable/Activity | 参数 | 导航方式 | 说明 |
|---------|------------------------|------|---------|------|
| `mall` | `MallScreen` | — | root | mall 一级频道根页 |
| `mall/login?productId={productId}&returnTarget={returnTarget}` | `MallLoginScreen` | `productId`, `returnTarget` | `NavController.navigate` | mall 专属登录承接 |
| `search` | `SearchHomeScreen` | — | 复用现有导航 | mall 搜索 bridge 复用现有搜索页 |

### 5.3 导航图

```kotlin
navigation(
    startDestination = AppDestination.Route.MALL,
    route = AppDestination.Graph.MALL,
) {
    composable(route = AppDestination.Route.MALL) {
        MallScreen(
            onOpenSearch = { navController.navigate(AppDestination.search()) },
            onOpenMallLogin = { context ->
                navController.navigate(AppDestination.mallLogin(context.productId, context.returnTarget))
            },
        )
    }
    composable(route = AppDestination.Route.MALL_LOGIN) {
        MallLoginScreen(onClose = { navController.popBackStack() })
    }
}
```

### 5.4 Deep Link 处理（如适用）

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `djsdrama://mall` | `MallScreen` | — |
| `djsdrama://mall/login` | `MallLoginScreen` | 内部保留，不对外主推 |

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| H5 容器加载 | `android.webkit.WebView` | 加载 `appConfig.mallBaseUrl + /mall` |
| 原生商城 API | 无新增 | 商品列表由 H5 自己请求 Backend |
| bridge 通讯 | `addJavascriptInterface` / `WebMessageListener` | 处理搜索与登录消息 |
| 登录态读取 | `AuthSessionProvider` | 供登录承接页完成后刷新语义使用 |

### 6.2 API 接口定义

```kotlin
data class MallLoginContext(
    val source: String = "mall",
    val productId: String,
    val returnTarget: String = "/mall",
)
```

- Android 首版不直接调用 `GET /api/mall/products`，因此不新增 Retrofit mall endpoint。
- 原生只处理 WebView URL 与 bridge payload。

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| H5 首次加载失败 | 0（手动重试） | — | 点击宿主态“重试”重新加载 mall 首页 |
| bridge payload 解析失败 | 0 | — | 忽略消息并提示日志 |
| 登录返回恢复失败 | 0 | — | 直接 reload mall 首页 |

### 6.4 网络状态监听

- 首版不单独实现 `ConnectivityManager` 针对 mall 的网络监听。
- WebView 加载错误通过 `WebViewClient.onReceivedError` / `onReceivedHttpError` 驱动宿主 error state。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| mall 当前 URL | 内存态 | `MallViewModel` | 会话内有效 | 不写 DataStore |
| mall 登录上下文 | 内存态 | `MallViewModel` / Nav arguments | 登录完成即清空 | 不持久化 |
| WebView 页面缓存 | WebView 默认缓存 | 系统管理 | 系统回收 | 首版不定制 |

### 7.2 Room 实体设计（如适用）

```text
不使用 Room。
```

### 7.3 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| WebView 缓存 | 依赖系统 WebView | 系统控制 | 系统回收 |
| 最近成功首页 URL | ViewModel 内存态 | 当前进程 | 进程重启清空 |

### 7.4 数据库 Migration

- 本期不新增 Room / DataStore 结构，无 migration。
- 若后续要保留 mall scroll position，再评估 `SavedStateHandle` 持久化最小信息。

---

## 8. 配置与环境

| 配置项 | 管理方式 | 开发环境值 | 生产环境值 | 说明 |
|--------|---------|----------|-----------|------|
| API Base URL | `AppConfig.apiBaseUrl` | 现有配置 | 现有配置 | 保持现有原生 API 配置 |
| Mall Base URL | `AppConfig.mallBaseUrl` | gradle / BuildConfig 注入 | gradle / BuildConfig 注入 | WebView 首页地址 |
| App Name | `AppConfig.appName` | 现有配置 | 现有配置 | 继续复用 |

> ⚠️ 禁止硬编码任何常量。商城 H5 URL 不能写在 `MallScreen`、`NavGraph` 或 `WebViewClient` 内。

---

## 9. API 调用清单

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| 无新增原生商城 API | — | — | 商品数据由 H5 自己请求 | Android 仅处理 WebView 加载错误 |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Android 端实现方式 |
|---------|---------------|-------------------|
| H5 承载商城首页 | Native 只做容器 | `MallScreen` + `MallWebViewContainer` |
| 搜索 bridge | `mall.openSearch` | `navigator.openSearch(from = MALL, returnTarget = "/mall")` |
| 搜索返回契约 | Native 搜索返回 mall | `restoreMallContext(reason = SearchReturn)`，并向 H5 发送 `mall.restoreContext` |
| 登录 bridge | `mall.requestLogin` | 导航到 `mall/login` 专属路由 |
| 登录态同步 | Native 返回权威登录态 | 初次加载、登录成功/取消、前后台切换后向 H5 发送 `mall.syncAuthState` |
| 登录返回契约 | 回到 `/mall`，tab 高亮保持 | login route 关闭后先同步 auth，再 `restoreMallContext(reason = LoginReturn)` |
| 容器三态 | loading / success / error | `MallContainerState` + Compose UI |
| 最低恢复保证 | 容器重建至少回到 mall 首页首屏 | 重新 load mall 首页 URL |

---

## 11. 边界与错误处理

### 11.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| WebView 层 | `WebViewClient` 错误回调 | 处理页面加载失败 |
| ViewModel | `MutableStateFlow` + effects | 统一处理 bridge、重试、登录返回 |
| UI 层 | 内联错误页 / loading 态 | 不回退占位页 |
| 日志 | `Log.e` / Timber（若已有） | 记录 bridge 与加载错误 |

### 11.2 错误码映射表

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `NETWORK_ERROR` | 商城加载失败，请稍后重试 | 宿主错误态 + 重试 |
| `INTERNAL_ERROR` | 商城暂时不可用 | 宿主错误态 + 重试 |
| `VALIDATION_ERROR` | 页面参数异常 | 忽略当前 bridge 消息 |
| `UNAUTHORIZED` | 请先登录 | 由 H5 拦截层先展示；Android 只承接登录页 |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| H5 加载失败 | WebView 网络错误 / 资源异常 | 展示错误态与重试按钮 | 🔴 |
| bridge payload 非法 | `productId` 缺失 / 空字符串 / search context 非法 | 忽略消息并记录日志 | 🔴 |
| Native 未同步登录态 | 首次加载或登录返回后未发送 `mall.syncAuthState` | 默认按匿名处理，并在页面恢复时补发同步 | 🔴 |
| 搜索跳转到 HOME graph | `AppDestination.search()` 归属 home | 允许临时切换，但关闭搜索时必须 `restoreMallContext(reason = SearchReturn)` | 🔴 |
| 登录承接被关闭 | 取消 / 失败 / 关闭 | 先同步 auth，再 `restoreMallContext(reason = LoginReturn)` | 🔴 |
| 容器被系统回收 | 后台回来 / 登录链路后重建 | 重载 mall 首页 URL，并重新发送登录态同步 | 🟡 |
| 重复点击登录 | 多次 `mall.requestLogin` | 若已有 `pendingLoginContext` 则忽略新请求 | 🟡 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `MallScreen` | CircularProgressIndicator | WebView 容器 | — | 错误说明 + 重试 | — |
| `MallLoginScreen` | 轻量加载 | 占位登录承接 | — | 关闭返回商城 | — |

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | `MallViewModel` 状态与 effect | 核心逻辑覆盖 | JUnit4 + MockK + Turbine |
| 导航测试 | mall route 与 login route 注册 | 核心路由覆盖 | NavGraph 测试 |
| bridge 解析测试 | 合法/非法 payload 处理 | 关键边界覆盖 | JUnit4 |

### 12.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| AND-MALL-01 | 首次加载成功 | mall URL 有效 | `loadHome()` | 状态变为 success | 单元 |
| AND-MALL-02 | 加载失败 | WebView error 回调 | 处理回调 | 状态变为 error | 单元 |
| AND-MALL-03 | 搜索 bridge | 收到 `mall.openSearch` | 处理 bridge | 发送 `OpenSearch` effect | 单元 |
| AND-MALL-04 | 登录 bridge | 收到合法 `MallLoginContext` | 处理 bridge | 发送 `OpenMallLogin` effect | 单元 |
| AND-MALL-05 | 登录关闭返回 | mall login route close | 返回上页 | mall root 仍存在 | 导航 |
| AND-MALL-06 | 非法 payload | 缺少 productId | 处理 bridge | 不崩溃，不导航 | 单元 |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| `AppConfig` | fake implementation | 避免直接依赖 BuildConfig |
| WebView 事件 | 通过 ViewModel 回调模拟 | 单元测试不依赖 Android WebView 实例 |
| `AuthSessionProvider` | MockK | 校验登录完成后的刷新逻辑 |
| 导航回调 | lambda capture / fake navigator | 验证 effect |

---

## 13. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | — | — | 复用现有 Compose、Navigation、Hilt、JUnit、MockK、Turbine |

> ⚠️ 新增开源依赖前必须征得用户同意（遵守根目录 CLAUDE.md 开发约束）。

---

## 14. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 继续复用 `menu/login` 导致商城登录返回语义错误 | Android / 产品语义 | 🔴 | 中 | 独立 mall login route | mall 专属占位登录页 |
| WebView bridge 与 Compose 页面耦合太深 | Android | 🟡 | 中 | 独立 `MallWebViewContainer` + ViewModel effect | 保持最小容器封装 |
| mall H5 URL 被硬编码 | Android / 环境切换 | 🔴 | 中 | 使用 `AppConfig.mallBaseUrl` | 无 |
| 搜索 bridge 切换到 HOME graph 造成 tab 高亮变化 | Android | 🟡 | 中 | 文档明确允许临时切换，返回后重新进入 mall | 失败时留在 mall 并提示 |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/features/app-shell/index.md` | Android、已知限制 | mall 仍为 placeholder，需要替换为真实容器 |
| `wiki/features/search-discovery/index.md` | Android 路由与入口 | 搜索页归属 HOME graph，可被 mall 复用 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `android/CLAUDE.md` | Android 架构、BuildConfig / AppConfig 约束 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | mall 当前仍为 `PlaceholderScreen` |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | top-level tabs 与现有搜索 route |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthSessionProvider.kt` | 登录态抽象 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt` | 现有 effect + request token 风格参考 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/NetworkModule.kt` | 现有 DI 风格 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt` | 现有 data/domain 分层写法 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | `RequireLogin(returnRoute)` 语义参考 |
| `docs/specs/2026-07-28-prd-13-mall/design.md` | mall shared contract |
