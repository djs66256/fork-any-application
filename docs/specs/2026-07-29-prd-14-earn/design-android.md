# Android 端技术方案：PRD-14 赚钱中心

> 创建日期：2026-07-29
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

```text
NavGraph.earn graph
→ EarnScreen
  → EarnViewModel
  → EarnWebViewContainer (AndroidView/WebView)
  → EarnContainerStateContent (loading / error)

EarnWebView bridge
→ earn.requestLogin(payload)
  → navController.navigate(AppDestination.earnLogin(...))
  → close/cancel/success => restoreEarnContext(reason = LoginReturn)
  → syncEarnAuthStateToWebView()
→ earn.openTaskPlayer(payload)
  → navController.navigate(AppDestination.earnPlay(...))
  → player opened with held earn task context
  → player close/back => restoreEarnContext(reason = TaskReturn)
  → if completed => dispatch earn.completeTask host message
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `android/.../navigation/NavGraph.kt` | 修改 | `earn` graph 从 `PlaceholderScreen` 改为 `EarnScreen` + earn login / earn play handoff |
| `android/.../navigation/AppDestination.kt` | 修改 | 新增 earn login route / earn play route / helper |
| `android/.../core/config/AppConfig.kt` | 扩展 | 新增 `earnBaseUrl` 配置 |
| `android/.../feature/earn/` | 新增 | 赚钱容器、bridge、宿主态组件、ViewModel |
| `android/.../feature/mall/` | 参考 | earn 参考 mall 容器模式，但模型和消息独立 |
| `android/.../feature/player/` | 复用/扩展 | 现有 player route 仍只接收 `videoId`，earn 需在导航层补 task context 与结果回传 |
| `android/.../core/auth/AuthStateHolder.kt` | 复用 | 提供是否已登录与 access token 快照给 earn host sync 使用 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | earn graph 接入真实容器与 earn login / task handoff route |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 新增 `Route.EARN_LOGIN`、`Route.EARN_PLAY` 与 helper |
| `android/app/src/main/java/com/djs66256/short_drama/core/config/AppConfig.kt` | 修改 | 扩展 `earnBaseUrl` |
| `android/app/src/main/java/com/djs66256/short_drama/core/config/BuildConfigAppConfig.kt` | 修改 | 从 BuildConfig 暴露 earn H5 base URL |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnScreen.kt` | 新增 | earn tab 根 Composable |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnWebViewContainer.kt` | 新增 | WebView 封装与 bridge 接线 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/ui/EarnLoginScreen.kt` | 新增 | earn 专属全屏登录承接页 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModel.kt` | 新增 | 容器状态、bridge effect、登录/任务返回恢复 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnLoginContext.kt` | 新增 | earn 登录上下文模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnTaskContext.kt` | 新增 | earn 任务播放上下文模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/earn/model/EarnHostMessage.kt` | 新增 | syncAuth / restore / completeTask 宿主消息模型 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/earn/viewmodel/EarnViewModelTest.kt` | 新增 | earn ViewModel 状态与 effect 测试 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt` | 修改 | 增补 earn graph、login route、play handoff 路由测试 |

---

## 3. UI 层设计

### 3.1 组件层级树

```text
EarnScreen
├── EarnContainerStateContent (loading / error)
├── EarnWebViewContainer
└── EarnLoginScreen (separate route)
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `EarnScreen` | Composable | earn tab 根页面，装配 ViewModel | 否 |
| `EarnWebViewContainer` | Composable | 通过 `AndroidView` 承载 WebView，注册 JS bridge | 否 |
| `EarnContainerStateContent` | Composable | loading / error 宿主态 UI | 否 |
| `EarnLoginScreen` | Composable | earn 专属登录承接占位页 | 否 |

### 3.3 Composable 接口定义

```kotlin
@Composable
fun EarnScreen(
    viewModel: EarnViewModel = hiltViewModel(),
    onOpenEarnLogin: (EarnLoginContext) -> Unit,
    onOpenEarnTaskPlayer: (EarnTaskContext) -> Unit,
)

@Composable
fun EarnWebViewContainer(
    url: String,
    modifier: Modifier = Modifier,
    onPageStateChanged: (EarnPageEvent) -> Unit,
    onBridgeMessage: (EarnBridgeMessage) -> Unit,
    hostMessageDispatcher: EarnHostMessageDispatcher,
)
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| 父 → 子 | Composable 参数 | WebView URL、状态、导航回调 |
| 子 → 父 | Lambda callback | 页面加载结果、bridge 消息 |
| 跨 Composable 共享 | `EarnViewModel` | 容器状态、待登录上下文、待任务上下文 |
| 路由间传递 | Nav arguments + ViewModel 内存态 | 登录承接、任务播放承接 |

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
| `EarnViewModel` | `EarnScreen` | 管理容器状态、bridge 事件、登录承接返回、任务播放返回 |

### 4.2 状态定义

```kotlin
class EarnViewModel(
    private val appConfig: AppConfig,
    private val authSessionProvider: AuthSessionProvider,
) : ViewModel() {
    data class UiState(
        val state: EarnContainerState = EarnContainerState.Loading,
        val currentUrl: String = "",
        val pendingLoginContext: EarnLoginContext? = null,
        val pendingTaskContext: EarnTaskContext? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `state` | `EarnContainerState` | `Loading` | 宿主级 loading / success / error |
| `currentUrl` | `String` | `earnHomeUrl` | 当前 WebView URL |
| `pendingLoginContext` | `EarnLoginContext?` | `null` | 待登录承接的上下文 |
| `pendingTaskContext` | `EarnTaskContext?` | `null` | 待播放返回与完成的任务上下文 |
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
- earn 首页属于 `Graph.EARN`，保持底部 earn tab 作为一级承接。
- 登录承接新增 `earn/login` route，语义与 mall / menu login 分离。
- 播放承接新增 `earn/play?...` handoff route，持有 `taskId / source / returnTarget / videoId`，最终再导航到现有 `play/{videoId}`。
- 登录关闭 / 成功 / 取消时必须先同步 `earn.syncAuthState`，再 `restoreEarnContext(reason = LoginReturn)`。
- 任务返回时必须根据完成结果决定是否发送 `earn.completeTask` host message。
- earn host sync 的唯一注入协议固定为 `window.dispatchEvent(new CustomEvent('earn.hostMessage', { detail }))`，Android 不再为 earn 走 `window.message` 双写。

### 5.2 路由清单

| 路由标识 | 目标 Composable/Activity | 参数 | 导航方式 | 说明 |
|---------|------------------------|------|---------|------|
| `earn` | `EarnScreen` | — | root | earn 一级频道根页 |
| `earn/login?returnTarget={returnTarget}` | `EarnLoginScreen` | `returnTarget` | `NavController.navigate` | earn 专属登录承接 |
| `earn/play?taskId={taskId}&videoId={videoId}&returnTarget={returnTarget}` | handoff route | `taskId`, `videoId`, `returnTarget` | `NavController.navigate` | 持任务上下文再进入 player |
| `play/{videoId}` | `PlayerScreen` | `videoId` | 复用现有 route | 最终播放器页面 |

### 5.3 导航图

```kotlin
navigation(
    startDestination = AppDestination.Route.EARN,
    route = AppDestination.Graph.EARN,
) {
    composable(route = AppDestination.Route.EARN) {
        EarnScreen(
            onOpenEarnLogin = { context ->
                navController.navigate(AppDestination.earnLogin(context.returnTarget))
            },
            onOpenEarnTaskPlayer = { context ->
                navController.navigate(
                    AppDestination.earnPlay(
                        taskId = context.taskId,
                        videoId = context.videoId,
                        returnTarget = context.returnTarget,
                    ),
                )
            },
        )
    }
    composable(route = AppDestination.Route.EARN_LOGIN) { ... }
    composable(route = AppDestination.Route.EARN_PLAY) { ... }
}
```

### 5.4 Deep Link 处理（如适用）

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `djsdrama://earn` | `EarnScreen` | — |
| `djsdrama://earn/login` | `EarnLoginScreen` | 内部保留，不对外主推 |

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| H5 容器加载 | `android.webkit.WebView` | 加载 `appConfig.earnBaseUrl + /earn` |
| 原生 earn API | 无新增 | overview / complete-task 由 H5 自己请求 Backend |
| bridge 通讯 | `addJavascriptInterface` | 处理登录与播放消息 |
| 登录态读取 | `AuthSessionProvider` | 供登录完成后刷新权威登录态使用 |
| access token 快照 | `AuthSessionProvider.currentSession()?.accessToken` | 仅在 `earn.syncAuthState` 中下发给 H5 内存态使用 |

### 6.2 API 接口定义

```kotlin
data class EarnLoginContext(
    val source: String = "earn",
    val returnTarget: String = "/earn",
)

data class EarnTaskContext(
    val taskId: String,
    val source: String = "earn",
    val returnTarget: String = "/earn",
    val videoId: String,
)
```

- Android 首版不直接调用 `GET /api/earn/overview` 或 `POST /api/earn/complete-task`。
- 原生只处理 WebView URL、bridge payload 与 host message 注入。

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| H5 首次加载失败 | 0（手动重试） | — | 点击宿主态“重试”重新加载 earn 首页 |
| bridge payload 解析失败 | 0 | — | 忽略消息并提示日志 |
| 登录/任务返回恢复失败 | 0 | — | 直接 reload earn 首页 |

### 6.4 网络状态监听

- 首版不单独实现 `ConnectivityManager` 针对 earn 的网络监听。
- WebView 加载错误通过 `WebViewClient.onReceivedError` / `onReceivedHttpError` 驱动宿主 error state。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| earn 当前 URL | 内存态 | `EarnViewModel` | 会话内有效 | 不写 DataStore |
| earn 登录上下文 | 内存态 | `EarnViewModel` / Nav arguments | 登录完成即清空 | 不持久化 |
| earn 任务上下文 | 内存态 | `EarnViewModel` / Nav arguments | 任务返回即清空 | 不持久化 |
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
- 若后续要保留 earn scroll position，再评估 `SavedStateHandle` 持久化最小信息。

---

## 8. 配置与环境

| 配置项 | 管理方式 | 开发环境值 | 生产环境值 | 说明 |
|--------|---------|----------|-----------|------|
| API Base URL | `AppConfig.apiBaseUrl` | 现有配置 | 现有配置 | 保持现有原生 API 配置 |
| Earn Base URL | `AppConfig.earnBaseUrl` | gradle / BuildConfig 注入 | gradle / BuildConfig 注入 | WebView 首页地址 |
| App Name | `AppConfig.appName` | 现有配置 | 现有配置 | 继续复用 |

> ⚠️ 禁止硬编码任何 URL、`/earn` 返回目标、taskId、videoId 或 bridge 对象名。必须通过 `AppConfig`、route helper 和 model 管理。

---

## 9. API 调用清单

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| 无新增原生 earn API | — | — | overview / complete-task 由 H5 自己请求 | Android 仅处理 WebView 加载错误与 host sync |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Android 端实现方式 |
|---------|---------------|-------------------|
| H5 承载赚钱首页 | Native 只做容器 | `EarnScreen` + `EarnWebViewContainer` |
| 登录 bridge | `earn.requestLogin` | 导航到 `earn/login` 专属路由 |
| 登录返回契约 | 回到 `/earn`，tab 高亮保持 | login route 关闭后先同步 auth，再 `restoreEarnContext(reason = LoginReturn)` |
| 播放 bridge | `earn.openTaskPlayer` | 导航到 `earn/play` handoff route，再进入 player |
| player 结果契约 | `EarnTaskPlayerResult` | player 退出时通过 handoff route / ViewModel 统一产出结果对象 |
| 任务完成闭环 | 播放完成后发 `earn.completeTask` | 返回 earn 容器时通过 host message dispatcher 注入 H5 |
| 登录态同步 | Native 返回权威登录态与 access token 快照 | 初次加载、登录成功/取消、前后台切换后向 H5 发送 `earn.syncAuthState` |
| host sync transport | Native 统一注入 `CustomEvent('earn.hostMessage')` | `EarnWebViewContainer` 的 JS helper 统一构造 event name 与 detail |
| 容器三态 | loading / success / error | `EarnContainerState` + Compose UI |
| 最低恢复保证 | 容器重建至少回到 earn 首页首屏 | 重新 load earn 首页 URL |

---

## 11. 边界与错误处理

### 11.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| WebView 层 | `WebViewClient` 错误回调 | 处理页面加载失败 |
| ViewModel | `MutableStateFlow` + effects | 统一处理 bridge、重试、登录/任务返回 |
| UI 层 | 内联错误页 / loading 态 | 不回退占位页 |
| 日志 | `Log.e` / Timber（若已有） | 记录 bridge 与加载错误 |

### 11.2 错误码映射表

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `NETWORK_ERROR` | 赚钱页加载失败，请稍后重试 | 宿主错误态 + 重试 |
| `INTERNAL_ERROR` | 赚钱页暂时不可用 | 宿主错误态 + 重试 |
| `VALIDATION_ERROR` | 页面参数异常 | 忽略当前 bridge 消息 |
| `AUTH_UNAUTHORIZED` | 请先登录 | 由 H5 引导先展示；Android 只承接登录页 |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| H5 加载失败 | WebView 网络错误 / 资源异常 | 展示错误态与重试按钮 | 🔴 |
| bridge payload 非法 | `taskId` / `videoId` 缺失或空字符串 | 忽略消息并记录日志 | 🔴 |
| Native 未同步登录态 | 首次加载或登录返回后未发送 `earn.syncAuthState` | 默认按匿名处理，并在页面恢复时补发同步 | 🔴 |
| 登录承接被关闭 | 取消 / 失败 / 关闭 | 先同步 auth，再 `restoreEarnContext(reason = LoginReturn)` | 🔴 |
| 播放未完成返回 | 中途退出 player | 只恢复上下文，不发完成消息 | 🔴 |
| 播放完成返回 | 已确认代表性任务完成 | 先发 `earn.completeTask`，再恢复上下文 | 🔴 |
| access token 过期 | AuthStateHolder 刷新前后 token 变化 | 下一次 `earn.syncAuthState` 覆盖 H5 快照；不下发 refresh token | 🔴 |
| 容器被系统回收 | 后台回来 / 登录链路后重建 | 重载 earn 首页 URL，并重新发送登录态同步 | 🟡 |
| 重复点击任务 | 多次 `earn.openTaskPlayer` | 若已有 `pendingTaskContext` 则忽略新请求 | 🟡 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `EarnScreen` | CircularProgressIndicator | WebView 容器 | — | 错误说明 + 重试 | — |
| `EarnLoginScreen` | 轻量加载 | 占位登录承接 | — | 关闭返回赚钱页 | — |

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | `EarnViewModel` 状态与 effect、task completion 注入 | 核心逻辑覆盖 | JUnit4 + MockK + Turbine |
| 导航测试 | earn route、login route、play handoff route 注册 | 核心路由覆盖 | NavGraph 测试 |
| bridge 解析测试 | 合法/非法 payload 处理 | 关键边界覆盖 | JUnit4 |

### 12.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| AND-EARN-01 | 首次加载成功 | earn URL 有效 | `loadHome()` | 状态变为 success | 单元 |
| AND-EARN-02 | 加载失败 | WebView error 回调 | 处理回调 | 状态变为 error | 单元 |
| AND-EARN-03 | 登录 bridge | 收到 `earn.requestLogin` | 处理 bridge | 发送 `OpenEarnLogin` effect | 单元 |
| AND-EARN-04 | 播放 bridge | 收到合法 `EarnTaskContext` | 处理 bridge | 发送 `OpenEarnTaskPlayer` effect | 单元 |
| AND-EARN-05 | 登录关闭返回 | earn login route close | 返回上页 | earn root 仍存在，并同步 auth/restore | 导航 |
| AND-EARN-06 | 播放完成返回 | task 完成 | 处理结果 | 注入 `earn.completeTask` host message | 单元 |
| AND-EARN-07 | 非法 payload | 缺少 taskId | 处理 bridge | 不崩溃，不导航 | 单元 |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| `AppConfig` | fake implementation | 避免直接依赖 BuildConfig |
| WebView 事件 | 通过 ViewModel 回调模拟 | 单元测试不依赖 Android WebView 实例 |
| `AuthSessionProvider` | MockK | 校验登录完成后的同步逻辑 |
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
| 继续复用 placeholder earn graph 导致需求无法落地 | Android | 🔴 | 高 | 将 `earn` graph 替换为真实容器 | 无 |
| 继续只用 `play/{videoId}` 导致任务上下文丢失 | Android / Earn 闭环 | 🔴 | 中 | 新增 `earn/play` handoff route + `EarnTaskContext` | ViewModel 内存持有上下文后再跳 player |
| 直接复用 mall login route 导致返回语义错误 | Android / 产品语义 | 🔴 | 中 | 独立 earn login route | earn 专属占位登录页 |
| earn H5 URL 被硬编码 | Android / 环境切换 | 🔴 | 中 | 使用 `AppConfig.earnBaseUrl` | 无 |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/features/app-shell/index.md` | Android、已知限制 | earn 仍为 placeholder，需要替换为真实容器 |
| `wiki/architecture/overview.md` | 承载策略 | earn 由 H5 + Native 容器承载 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `android/CLAUDE.md` | Android 架构、AppConfig、测试与导航约束 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 当前只有 `play/{videoId}` 与 `mall/login`，无 earn route |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | earn graph 当前仍为 placeholder |
| `android/app/src/main/java/com/djs66256/short_drama/core/config/AppConfig.kt` | 当前仅有 `mallBaseUrl`，无 earn 配置 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallWebViewContainer.kt` | WebView bridge 与 host message 注入实现参考 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt` | effect + host sync 模式参考 |
| `docs/specs/2026-07-28-prd-13-mall/design-android.md` | mall 平台设计范式 |
| `docs/specs/2026-07-29-prd-14-earn/design.md` | earn shared contract |
