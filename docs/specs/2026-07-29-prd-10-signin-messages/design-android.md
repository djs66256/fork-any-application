# Android 端技术方案：PRD-10 签到与消息系统

> 创建日期：2026-07-29
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

```text
Compose UI
  → ViewModel (StateFlow)
  → UseCase
  → Repository (Domain interface)
  → RemoteDataSource
  → ApiService / Retrofit / OkHttp
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `feature/home` | 扩展 | 首页新增签到浮层宿主与冷启动资格检查 |
| `feature/menu` | 扩展 | 菜单消息预览改为动态状态 |
| `navigation/*` | 扩展 | `menu/messages` 从 placeholder 变为真实消息中心页 |
| `core/network/ApiService.kt` | 扩展 | 新增 5 个签到 / 消息接口声明 |
| `core/storage/PlaybackSessionStore.kt` | 参考 | 复用 DataStore 生成 UUID 的模式实现 installationId store |
| `feature/comments` | 共存 | 首页已有 comments bottom sheet，签到浮层需避免与其冲突 |
| `core/auth/AuthStateHolder` | 复用 | 判断登录态，互动消息请求复用既有 bearer token 注入 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增签到 / 消息接口 |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/InstallationIdStore.kt` | 新增 | 使用 DataStore 生成安装级 UUID |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/CheckInRemoteDataSource.kt` | 新增 | 封装签到请求 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/MessageRemoteDataSource.kt` | 新增 | 封装 preview/system/interactions 请求 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/*CheckIn*.kt` | 新增 | 签到 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/*Message*.kt` | 新增 | 消息 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/*CheckIn*.kt` | 新增 | 签到领域模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/*Message*.kt` | 新增 | 消息领域模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/CheckInRepository.kt` | 新增 | 定义签到仓储接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/MessageRepository.kt` | 新增 | 定义消息仓储接口 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/CheckInRepositoryImpl.kt` | 新增 | 实现签到仓储 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/MessageRepositoryImpl.kt` | 新增 | 实现消息仓储 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetCheckInStatusUseCase.kt` | 新增 | 查询签到状态 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/SubmitCheckInUseCase.kt` | 新增 | 提交签到 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetMessagePreviewUseCase.kt` | 新增 | 查询菜单消息预览 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetSystemMessagesUseCase.kt` | 新增 | 查询系统消息 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetInteractionMessagesUseCase.kt` | 新增 | 查询互动消息 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt` | 修改 | 新增签到浮层状态管理 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 承载签到浮层 composable |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/CheckInPopup.kt` | 新增 | 签到浮层组件 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt` | 修改 | 增加消息 preview 状态 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelScreen.kt` | 修改 | 绑定真实消息 preview 数据 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/messages/ui/MessageCenterScreen.kt` | 新增 | 消息中心页 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/messages/viewmodel/MessageCenterViewModel.kt` | 新增 | 系统消息 / 互动消息 / 登录门槛状态 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 保留 `menu/messages` route，但指向真实页 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 注入真实消息中心 screen |
| `android/app/src/test/java/...` | 新增 | ViewModel / repository / DTO 测试 |

---

## 3. UI 层设计

### 3.1 组件层级树

```text
HomeScreen
├── HomeTopBar
├── HomeFeedContent
├── CheckInPopupHost
│   └── CheckInPopup
│       ├── CheckInPopupHeader
│       ├── CheckInDayGrid
│       ├── CheckInRewardCopy
│       └── CheckInPrimaryButton
└── CommentBottomSheet (existing)

MessageCenterScreen
├── MessageCenterTopBar
├── SystemMessageSection
│   ├── SystemMessageList
│   ├── EmptyState
│   └── ErrorState
└── InteractionSection
    ├── LoginGateCard (anonymous)
    ├── InteractionMessageList (authenticated)
    ├── EmptyState
    └── ErrorState

MenuPanelScreen
└── MenuMessagePreview
    ├── Title
    ├── Summary
    ├── RelativeTime
    └── Chevron
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `CheckInPopup` | Composable | 首页签到浮层 | 否 |
| `CheckInDayCell` | Composable | 单日签到卡片 | 否 |
| `MessageCenterScreen` | Composable | 消息中心页 | 否 |
| `SystemMessageItem` | Composable | 系统消息列表项 | 否 |
| `InteractionMessageItem` | Composable | 互动消息列表项 | 否 |
| `InteractionLoginGateCard` | Composable | 匿名用户登录门槛 | 否 |
| `MenuMessagePreview` | Composable | 菜单消息预览 | 是（保留原组件，替换数据来源） |

### 3.3 Composable 接口定义

```kotlin
@Composable
fun CheckInPopup(
    state: CheckInPopupUiState,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onSubmit: () -> Unit,
) {
    // ...
}

@Composable
fun MessageCenterScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MessageCenterViewModel = hiltViewModel(),
) {
    // ...
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| 父 → 子 | Composable 参数 | `HomeScreen` 传签到浮层状态给 `CheckInPopup` |
| 子 → 父 | Lambda Callback | 关闭、签到、重试、登录按钮 |
| 跨 Composable 共享 | ViewModel + StateFlow | 消息页系统消息 / 互动消息状态 |
| 导航层 → 页面 | NavGraph route + ViewModel 注入 | `menu/messages` 消息中心页 |

### 3.5 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | `BoxWithConstraints` | 签到浮层最大宽度限制 |
| 横竖屏 | `rememberSaveable` + ViewModel 状态 | 避免旋转丢失弹层状态 |
| 字体缩放 | Material3 typography + 自动换行 | 消息摘要与奖励文案不截断核心信息 |
| 深色模式 | 复用 MaterialTheme 颜色 | 不新增硬编码色值 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 UI | 职责 |
|-----------|---------|------|
| `HomeViewModel` | `HomeScreen` | 新增签到浮层查询、关闭态与提交逻辑 |
| `MenuPanelViewModel` | `MenuPanelScreen` | 新增菜单消息预览状态 |
| `MessageCenterViewModel` | `MessageCenterScreen` | 管理系统消息、互动消息、登录门槛与局部重试 |

### 4.2 状态定义

```kotlin
data class CheckInPopupUiState(
    val isVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val serverDate: String? = null,
    val todaySigned: Boolean = false,
    val currentStreak: Int = 0,
    val rewardCopy: String = "",
    val days: List<CheckInDay> = emptyList(),
    val errorMessage: String? = null,
)

data class MessageCenterUiState(
    val systemMessages: List<SystemMessage> = emptyList(),
    val systemLoading: Boolean = false,
    val systemErrorMessage: String? = null,
    val interactionMessages: List<InteractionMessage> = emptyList(),
    val interactionLoading: Boolean = false,
    val interactionErrorMessage: String? = null,
    val showInteractionLoginGate: Boolean = true,
)
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `HomeUiState.checkInPopup` | `CheckInPopupUiState` | 默认隐藏 | 首页签到弹层状态 |
| `MenuPanelUiState.messagePreview` | 新增字段 / 子状态 | `idle` | 菜单消息预览 |
| `MessageCenterUiState.systemMessages` | `List<SystemMessage>` | 空列表 | 系统消息列表 |
| `MessageCenterUiState.showInteractionLoginGate` | `Boolean` | `true` | 匿名用户展示登录门槛 |
| `MessageCenterUiState.interactionErrorMessage` | `String?` | `null` | 互动消息局部错误 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | UI 层表现 |
|---------|---------|----------|
| 签到弹层隐藏 | `!checkInPopup.isVisible` | 首页保持当前状态 |
| 签到弹层展示 | `checkInPopup.isVisible` | 首页上方显示遮罩卡片 |
| 签到提交中 | `checkInPopup.isSubmitting` | 按钮 disabled + loading |
| 菜单消息 preview 有内容 | `preview != null && error == null` | 显示真实摘要 |
| 菜单消息 preview 空态 | `hasLoaded && preview == null && error == null` | 显示“暂无消息” |
| 菜单消息 preview 错误 | `error != null` | 显示降级文案 |
| 消息页匿名态 | `showInteractionLoginGate == true` | 互动分区展示登录卡 |
| 消息页已登录空态 | `!showInteractionLoginGate && interactionMessages.isEmpty()` | 展示“暂无互动消息” |
| 消息页互动区错误 | `interactionErrorMessage != null` | 局部错误态，不影响系统消息 |

---

## 5. Navigation 路由设计

### 5.1 导航方案

- 继续使用 Navigation Compose。
- `PendingRoute.MenuMessages` 保持不变，但目标页面由 placeholder 改为真实 `MessageCenterScreen`。
- 从菜单进入仍走 `MainNavigationViewModel.closeMenuThenNavigate(...)`，关闭动画完成后再导航。
- 消息页内点击登录按钮复用现有登录 route，并固定携带 `returnRoute = menu/messages` 与 `source = menu_messages`，确保登录后唯一回流到消息页。

### 5.2 路由清单

| 路由标识 | 目标 Composable/Activity | 参数 | 导航方式 | 说明 |
|---------|------------------------|------|---------|------|
| `menu/messages` | `MessageCenterScreen` | 无 | `navController.navigate(...)` | 真实消息中心页 |
| `login?returnRoute=...&source=...` | `LoginScreen` | `returnRoute`, `source` | `navController.navigate(...)` | 登录后回到消息页 |

### 5.3 导航图

```kotlin
composable(route = AppDestination.Route.MENU_MESSAGES) {
    MessageCenterScreen(
        onBack = { navController.popBackStack() },
        onLogin = {
            navController.navigate(
                AppDestination.login(
                    returnRoute = AppDestination.menuMessages(),
                    source = "menu_messages",
                ),
            )
        },
    )
}
```

### 5.4 Deep Link 处理

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `djsdrama://messages` | `MessageCenterScreen` | 无 |

本期不实现对外 deeplink，也不注册 `djsdrama://messages` 路由；消息中心仅通过应用内菜单入口进入。 

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | Retrofit | 复用当前 `ApiService` |
| 数据模型 | `@Serializable` DTO | 签到 / 消息新增 DTO |
| 请求拦截器 | 继续复用 `AuthInterceptor`；installationId 由参数 header 注入 | `messages/interactions` 必须同步加入 `requiresAuth()` 受保护路径判定，确保 bearer token 自动注入 |
| 响应解析 | kotlinx.serialization | 继续用 DTO → domain 映射 |
| 错误处理 | `ApiResult<T>` | 保持 Success / Error / Exception 模式 |

### 6.2 API 接口定义

```kotlin
@GET("check-ins/status")
suspend fun getCheckInStatus(
    @Header("X-Installation-Id") installationId: String? = null,
): CheckInStatusDto

@POST("check-ins")
suspend fun submitCheckIn(
    @Header("X-Installation-Id") installationId: String? = null,
): CheckInStatusDto

@GET("messages/preview")
suspend fun getMessagePreview(): MessagePreviewDto?

@GET("messages/system")
suspend fun getSystemMessages(
    @Query("page") page: Int = 1,
    @Query("pageSize") pageSize: Int = 20,
): MessageListResponseDto<SystemMessageDto>

@GET("messages/interactions")
suspend fun getInteractionMessages(
    @Query("page") page: Int = 1,
    @Query("pageSize") pageSize: Int = 20,
): MessageListResponseDto<InteractionMessageDto>
```

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 首页签到状态失败 | 0 | — | 冷启动直接降级不展示 |
| 签到提交失败 | 0 | — | 由用户再次点击重试 |
| 菜单 preview 失败 | 1 | 短退避 | 保持菜单可用 |
| 互动消息失败 | 0 | — | 页面提供局部重试按钮 |
| 401 token 过期 | — | — | 继续复用既有 auth 刷新逻辑 |

### 6.4 网络状态监听

- 不新增专门监听器；
- ViewModel 根据 `ApiResult.Error/Exception` 输出对应错误文案。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| installationId | DataStore (Preferences) | `check_in_installation_id` | 安装期长期有效 | 复用 `PlaybackSessionStore` 模式 |
| 签到关闭态 | DataStore (Preferences) | `check_in_dismissed_server_date` | 每个业务日覆盖 | 本地 UI 控制态 |
| preview 最近成功结果 | 内存状态 | ViewModel 私有字段 | 当前进程生命周期 | 防止菜单频繁打开重复请求 |

### 7.2 Room 实体设计

本需求首版不引入 Room。

### 7.3 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| 菜单消息 preview | 内存缓存 | 当前会话 | ViewModel 重建失效 |
| 签到关闭态 | DataStore 单值 | 1 个服务端业务日 | 新业务日覆盖 |

---

## 8. 交互与状态机细化

### 8.1 首页签到浮层冲突策略

- `HomeScreen` 里同时可能存在：首页 feed 内容、`CommentBottomSheet`、登录占位 dialog 与新签到浮层。
- 首版统一策略：签到浮层只在冷启动进入首页且当前没有其他模态容器时展示。
- 若 `CommentBottomSheet`、登录承接或其他首页模态已处于展示中，本次冷启动直接放弃签到浮层，不等待容器关闭后补弹。
- 这样可保持首页模态优先级单一，避免评论容器关闭瞬间再次弹出签到浮层造成跳变。

### 8.2 消息页双分区加载顺序

```text
进入 MessageCenterScreen
  → 加载 system messages
  → 检查 auth 状态
      → anonymous: show login gate
      → authenticated: load interaction messages
```

---

## 9. 测试策略

### 9.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| DTO / Repository 测试 | DTO→Domain 映射、header/query 传递、ApiResult 映射 | JUnit4 + MockK |
| ViewModel 测试 | Home / MenuPanel / MessageCenter 状态流转 | JUnit4 + MockK + Turbine |
| 存储测试 | installationId 与 dismissed server date | JVM 单测 |
| Navigation 测试 | returnRoute 构造、菜单关闭后导航 | 单测 / 现有导航测试模式 |

### 9.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 测试类型 |
|------|---------|------|---------|---------|
| A-01 | 冷启动且签到应弹出 | `should_show_popup=true`，无本地关闭态 | `checkInPopup.isVisible=true` | ViewModel |
| A-02 | 同服务端业务日已关闭 | 本地 dismissed 与 `serverDate` 相同 | 不展示浮层 | ViewModel |
| A-03 | 签到提交成功 | `submitCheckIn` Success | UI 更新为今日已签到 | ViewModel |
| A-04 | 签到提交失败 | `ApiResult.Error` | 浮层保持打开，可重试 | ViewModel |
| A-05 | 菜单 preview 成功 | preview DTO | 显示真实摘要 | ViewModel |
| A-06 | 菜单 preview 失败 | `ApiResult.Exception` | 显示降级文案 | ViewModel |
| A-07 | 匿名进入消息页 | 无登录态 | 显示登录门槛，不请求互动消息 | ViewModel |
| A-08 | 已登录进入消息页 | 有登录态 | 加载互动消息列表 | ViewModel |
| A-09 | 互动消息失败 | `ApiResult.Error` | 互动区局部错误，不影响系统消息区 | ViewModel |
| A-10 | 菜单点击消息入口 | `PendingRoute.MenuMessages` | 先关闭菜单，后导航到真实消息页 | Navigation |

---

## 10. 风险与实现约束

- 不新增 Jetpack Security / `EncryptedSharedPreferences` 依赖；installationId 继续走 DataStore；
- 不直接使用 `BuildConfig`，如有需要通过 `AppConfig` 间接获取配置；
- 消息中心首版不实现未读态、消息详情、富文本或 push 通知联动；
- 互动消息登录态判断必须复用既有 auth 状态来源，避免页面自己维护第二套登录状态；
- 新增互动消息接口时，必须同步更新 `AuthInterceptor.requiresAuth()`，把 `messages/interactions` 纳入 bearer token 自动注入白名单；
- 新增 ViewModel / repository / datasource 后，需要同步更新 Hilt module 绑定。
