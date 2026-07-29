# Android 端技术方案：PRD-09 评论系统

> 创建日期：2026-07-29
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

Android 端评论能力继续遵循 `android/CLAUDE.md` 的分层约束：**Presentation（Composable + ViewModel） → Domain（Model + Repository Interface + UseCase） ← Data（DTO + DataSource + RepositoryImpl） ← Core（Network / DI / Config）**。评论不新增独立页面 route，而是作为首页卡片与播放器页面内的 `ModalBottomSheet` 能力承载。

本期能力拆成五个子域：

1. 首页卡片评论入口扩展；
2. 播放器评论芯片从空点击改为真实回调；
3. 评论底部抽屉 `ModalBottomSheet` UI 与状态机；
4. comments API 的 Retrofit / DataSource / Repository / UseCase 接入；
5. 登录拦截与结构化 pending action 恢复语义接入。

```text
HomeScreen / PlayerScreen
  -> tap comment entry
     -> open CommentBottomSheet(dramaId, source)
        -> CommentSheetViewModel.loadIfNeeded()
           -> GetDramaCommentsUseCase(query)
              -> CommentRepository.getComments(query)
                 -> CommentRemoteDataSource.getComments(...)
                    -> ApiService.getDramaComments(...)
                       -> GET /api/dramas/{id}/comments
        -> tap submit
           -> authSessionProvider.isLoggedIn()
              -> false: emit RequireLogin(CommentLoginContext)
              -> true: CreateCommentUseCase(...)
        -> tap like
           -> authSessionProvider.isLoggedIn()
              -> false: emit RequireLogin(CommentLoginContext)
              -> true: ToggleCommentLikeUseCase(...)
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 在现有卡片 action row 中扩展评论入口，并承接首页评论 sheet |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/components/PlayerComponents.kt` | 修改 | `AssistChip(onClick = {})` 改为真实 `onOpenComments` callback |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt` | 修改 | 新增第三个 `ModalBottomSheet` 承载评论抽屉 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerUiState.kt` | 修改 | 增加评论抽屉显示态、评论入口上下文 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` | 修改 | 新增打开/关闭评论、require-login effect、评论 sheet viewmodel 依赖 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增 comments API endpoint |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt` | 不变 | 首页 feed 继续使用现有 data source；评论新增独立 data source |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthSessionProvider.kt` | 复用 | 继续以 `isLoggedIn()` 判定是否进入写操作 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt` | 复用 / 轻量调整 | 当前默认 `isLoggedIn() = false`，评论写操作按相同基线工作 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 复用 | 继续复用 `play/{videoId}`、`menu/login` 与 `PendingRoute`；不新增 `homeWithComment` helper |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt` | 参考复用 | 现有 `PendingRoute` / `pendingRoute` 机制可承接登录完成后的来源页恢复 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | 参考复用 | `RequireLogin(returnRoute)` 仅是当前榜单能力的最小语义，评论模块需补齐结构化上下文 |

### 1.2 设计原则

1. **评论作为页面内 `ModalBottomSheet`**：不新增独立 comments route，不改现有导航骨架。
2. **登录恢复上下文结构化**：评论模块单独定义 `CommentLoginContext` / `PendingCommentAction`，不再只靠单一 `returnRoute` 字符串承载全部语义。
3. **评论状态独立封装**：评论列表/发送/点赞状态收敛在独立的 `CommentSheetViewModel`，避免污染首页 Feed 主 ViewModel。
4. **只做最小登录接入**：当前 `AuthSessionProvider.isLoggedIn()` 固定 `false` 时，写操作应稳定发出 `RequireLogin(CommentLoginContext)`；宿主先落 alert / placeholder login，再为后续真实登录恢复预留上下文。
5. **不新增依赖**：继续使用 Compose、StateFlow、Retrofit、kotlinx.serialization、MockK/Turbine。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/comments/ui/CommentBottomSheet.kt` | 新增 | 评论抽屉根 Composable |
| `android/app/src/main/java/com/djs66256/short_drama/feature/comments/ui/CommentComponents.kt` | 新增 | 评论行、输入区、状态视图、分页 footer 等组件 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentSheetViewModel.kt` | 新增 | 评论列表、排序、分页、发送、点赞、登录拦截状态机 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentUiState.kt` | 新增 | 评论 UI state 与 effect 定义 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/comments/model/CommentUiModel.kt` | 新增 | 评论 UI 模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/Comment.kt` | 新增 | 评论领域实体 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/CommentQuery.kt` | 新增 | 评论 query / sort / page 实体 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/ToggleCommentLikeResult.kt` | 新增 | 点赞切换结果实体 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/CommentRepository.kt` | 新增 | 评论仓库接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetDramaCommentsUseCase.kt` | 新增 | 获取评论列表用例 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/CreateCommentUseCase.kt` | 新增 | 发评论用例 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/ToggleCommentLikeUseCase.kt` | 新增 | 点赞切换用例 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/CommentDto.kt` | 新增 | 评论 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/CommentListResponseDto.kt` | 新增 | 评论列表响应 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/CreateCommentRequestDto.kt` | 新增 | 发评论请求 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/ToggleCommentLikeResponseDto.kt` | 新增 | 点赞返回 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/CommentRemoteDataSource.kt` | 新增 | 评论远端数据源 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/CommentRepositoryImpl.kt` | 新增 | 评论仓库实现 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增 comments endpoints |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 注入 `CommentRepository` |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 首页评论入口与 sheet 承载 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/components/PlayerComponents.kt` | 修改 | 评论按钮接 callback |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt` | 修改 | 新增评论 bottom sheet |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerUiState.kt` | 修改 | 增加评论相关状态 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` | 修改 | 接入评论入口和 require-login effect |
| `android/app/src/test/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentSheetViewModelTest.kt` | 新增 | 覆盖评论状态机 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/CommentRepositoryImplTest.kt` | 新增 | 覆盖 DTO 映射与错误透传 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModelTest.kt` | 修改 | 覆盖评论入口行为 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/home/ui/HomeScreenTest.kt` | 修改 | 验证首页评论按钮存在 |

---

## 3. UI 层设计

### 3.1 组件层级树

```text
CommentBottomSheet
├── CommentSheetHeader
│   ├── Title("评论")
│   ├── CountText
│   └── FilterChipRow(latest/hot)
├── CommentBody
│   ├── CommentLoadingState
│   ├── CommentErrorState
│   ├── CommentEmptyState
│   └── LazyColumn
│       └── CommentRow
│           ├── AvatarPlaceholder
│           ├── UserMeta
│           ├── ContentText
│           └── LikeAction
└── CommentComposer
    ├── OutlinedTextField
    ├── CharCount
    └── SendButton
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `CommentBottomSheet` | Composable | 评论抽屉根组件 | 否 |
| `CommentRow` | Composable | 单条评论展示与点赞按钮 | 否 |
| `CommentComposer` | Composable | 输入区与发送按钮 | 否 |
| `CommentStateView` | Composable | loading / empty / error 容器 | 否 |
| `HomeDramaCard` | Composable | action row 扩展评论按钮 | 修改复用 |
| `PlayerRightActionBar` | Composable | 评论芯片增加真实点击回调 | 修改复用 |

### 3.3 Composable 接口定义

```kotlin
@Composable
fun CommentBottomSheet(
    uiState: CommentUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onSelectSort: (CommentSort) -> Unit,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggleLike: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
)
```

```kotlin
@Composable
fun HomeDramaCard(
    drama: Drama,
    onPlay: () -> Unit,
    onDetail: () -> Unit,
    onComment: () -> Unit,
    modifier: Modifier = Modifier,
)
```

```kotlin
@Composable
fun PlayerRightActionBar(
    interactionState: PlayerInteractionState,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenComments: () -> Unit,
    modifier: Modifier = Modifier,
)
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| 宿主页 -> `CommentBottomSheet` | Composable 参数 | 当前 `dramaId`、评论状态、事件回调 |
| `CommentSheetViewModel` -> UI | `StateFlow<CommentUiState>` | 评论列表、输入、分页、错误、点赞状态 |
| UI -> ViewModel | Lambda callback | 排序切换、输入、发送、点赞、重试、触底加载 |
| `PlayerViewModel` / 宿主页 -> 应用壳 | `SharedFlow<Effect>` | 未登录写操作时触发 `RequireLogin(CommentLoginContext)` |
| 宿主页协调状态 | 页面级内存对象 | 保存 `CommentLoginContext`，在登录成功后恢复来源页与评论抽屉 |

### 3.5 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| Bottom sheet 高度 | `ModalBottomSheet` 默认自适应 | 与现有选集/倍速 sheet 风格一致 |
| 小屏手机 | `LazyColumn + sticky footer composer` 或底部固定输入区 | 保证输入区始终可见 |
| 横竖屏 | `rememberSaveable` + ViewModel state | 旋转后保留当前输入与评论状态 |
| 字体缩放 | Material3 typography + 文本换行 | 评论正文允许多行 |
| 深色模式 | 复用 `ShortDramaTheme` | 与宿主页视觉一致 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 UI | 职责 |
|-----------|---------|------|
| `CommentSheetViewModel` | `CommentBottomSheet` | 加载评论、分页、排序、发送、点赞、登录拦截事件 |
| `PlayerViewModel` | `PlayerScreen` | 打开/关闭评论 sheet、require-login effect |
| 首页页面级协调状态 | `HomeScreen` | 控制首页评论 sheet 展示与关闭 |

### 4.2 状态定义

```kotlin
data class CommentUiState(
    val dramaId: String = "",
    val source: CommentSource = CommentSource.PLAYER,
    val listState: CommentListState = CommentListState.Idle,
    val comments: List<CommentUiModel> = emptyList(),
    val selectedSort: CommentSort = CommentSort.LATEST,
    val inputText: String = "",
    val isSubmitting: Boolean = false,
    val likingCommentIds: Set<String> = emptySet(),
    val isAppending: Boolean = false,
    val appendErrorMessage: String? = null,
    val totalCount: Int = 0,
    val hasNextPage: Boolean = false,
)

data class CommentLoginContext(
    val source: CommentSource,
    val dramaId: String,
    val returnRoute: String,
    val action: PendingCommentAction,
)

data class PendingCommentAction(
    val type: CommentPendingActionType,
    val commentId: String? = null,
)

enum class CommentPendingActionType {
    OPEN_SHEET,
    CREATE_COMMENT,
    TOGGLE_LIKE,
}

sealed interface CommentEffect {
    data class RequireLogin(val context: CommentLoginContext) : CommentEffect
    data class ShowMessage(val message: String) : CommentEffect
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `dramaId` | `String` | `""` | 当前评论所属短剧 |
| `source` | `CommentSource` | `PLAYER` | 来源页：home / player |
| `listState` | `CommentListState` | `Idle` | loading/content/empty/error |
| `comments` | `List<CommentUiModel>` | `emptyList()` | 当前评论列表 |
| `selectedSort` | `CommentSort` | `LATEST` | 当前排序 |
| `inputText` | `String` | `""` | 输入框内容 |
| `isSubmitting` | `Boolean` | `false` | 发评论中 |
| `likingCommentIds` | `Set<String>` | `emptySet()` | 正在点赞中的评论 |
| `isAppending` | `Boolean` | `false` | 加载更多中 |
| `appendErrorMessage` | `String?` | `null` | 分页失败提示 |
| `totalCount` | `Int` | `0` | 来自分页 total |
| `hasNextPage` | `Boolean` | `false` | 是否还可翻页 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | UI 层表现 |
|---------|---------|----------|
| Loading | `listState is Loading && comments.isEmpty()` | 全区 loading |
| Success | `listState is Content` | 列表正常展示 |
| Empty | `listState is Empty` | 空态 |
| Error | `listState is Error && comments.isEmpty()` | 错误态 + 重试 |
| Append Loading | `isAppending` | footer loading |
| Append Error | `appendErrorMessage != null` | footer 错误 + 重试 |

### 4.5 登录恢复上下文

评论模块不再只使用单一 `returnRoute: String`。结合 spec 与 shared design，Android 侧单独保留结构化 `CommentLoginContext`，其中 `returnRoute` 只是恢复来源页的一个字段，不能代替 `source / action / commentId`。

```kotlin
fun buildCommentLoginContext(
    source: CommentSource,
    dramaId: String,
    action: PendingCommentAction,
): CommentLoginContext {
    val returnRoute = when (source) {
        CommentSource.HOME -> AppDestination.Route.HOME
        CommentSource.PLAYER -> AppDestination.play(dramaId)
    }

    return CommentLoginContext(
        source = source,
        dramaId = dramaId,
        returnRoute = returnRoute,
        action = action,
    )
}
```

说明：
- `AppDestination.kt` 当前不存在 `homeWithComment(dramaId)`；首页来源恢复只能先回到现有 `home` route，再由首页页面级协调状态重新打开对应 `dramaId` 的评论抽屉。
- `returnRoute` 继续复用当前 Android 登录承接基线，但仅承担“回到哪个页面”的语义。
- `PendingCommentAction` 负责承载 `create_comment` / `toggle_like` 与可选 `commentId`，满足 spec 对 pending action 的约束。
- 登录成功后只恢复来源页与评论抽屉打开状态，不自动重放发送/点赞写操作。

---

## 5. Navigation 路由设计

### 5.1 导航方案

- 首页评论：由首页页面内状态控制 `ModalBottomSheet`。
- 播放器评论：由 `PlayerScreen` 内状态控制 `ModalBottomSheet`。
- 登录拦截：评论模块发出 `RequireLogin(CommentLoginContext)`，宿主先消费为 alert / placeholder login；后续接入真实登录后，再结合 `returnRoute + source + action` 恢复来源页与评论抽屉。

### 5.2 路由清单

| 路由标识 | 目标 Composable/Activity | 参数 | 导航方式 | 说明 |
|---------|------------------------|------|---------|------|
| `play/{videoId}` | `PlayerScreen` | `videoId` | 现有 route | 评论不改变播放器主路由 |
| 评论抽屉 | 页面内 `ModalBottomSheet` | `dramaId` | 局部 UI 状态 | 不新增 Nav route |
| `RequireLogin(CommentLoginContext)` | 登录承接入口（现有/后续） | `source, dramaId, returnRoute, action, commentId?` | effect -> host state | 登录恢复上下文 |

### 5.3 导航图

- `NavGraph` 不新增 comments composable。
- 只需要把首页卡片与播放器 action bar 的评论点击回调接到各自页面内 sheet 状态上。

### 5.4 Deep Link 处理

- 本期不新增 comments deeplink。
- 评论恢复仍依赖来源页 route，而不是单独 deeplink。

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | Retrofit + OkHttp | 继续复用现有网络栈 |
| 数据模型 | kotlinx.serialization DTO | comments DTO 使用 `@SerialName` |
| 请求拦截器 | 现有 `AuthInterceptor` | 登录态成熟后自动透传 header |
| 错误处理 | `ApiResult<T>` | 与 `DramaRemoteDataSource` 保持一致 |

### 6.2 API 接口定义

```kotlin
interface ApiService {
    @GET("dramas/{id}/comments")
    suspend fun getDramaComments(
        @Path("id") id: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("sort") sort: String,
    ): CommentListResponseDto

    @POST("dramas/{id}/comments")
    suspend fun createDramaComment(
        @Path("id") id: String,
        @Body request: CreateCommentRequestDto,
    ): CommentDto

    @POST("dramas/{id}/comments/{commentId}/like")
    suspend fun toggleDramaCommentLike(
        @Path("id") id: String,
        @Path("commentId") commentId: String,
    ): ToggleCommentLikeResponseDto
}
```

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 网络超时 | 0 | — | 由用户手动重试 |
| 5xx 错误 | 0 | — | 评论交互避免隐式重放 |
| 401 未登录 | 0 | — | 直接触发 `RequireLogin(CommentLoginContext)` |

### 6.4 网络状态监听

- 本期不新增 Connectivity 专属逻辑。
- 仍通过错误态和重试按钮承接临时网络失败。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| 评论列表 | 不持久化 | ViewModel 内存状态 | 页面销毁释放 | 首版不落本地数据库 |
| 评论输入草稿 | 不持久化 | `CommentUiState.inputText` | 关闭抽屉清空 | 避免草稿跨 drama 泄漏 |
| 登录恢复上下文 | 页面级内存状态 | `CommentLoginContext` | 登录完成或取消后清空 | 不用 DataStore；首页恢复依赖现有 `home` route + 页面级 reopen |

### 7.2 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| 当前抽屉评论列表 | 内存缓存 | 当前页面生命周期 | 关闭抽屉或切换 drama 后清空 |
| 分页状态 | 内存缓存 | 同上 | 切换排序重置 |

### 7.3 数据库 Migration

- Android 端本期不引入 Room / DataStore schema 新增，因此无本地 migration。
- 也不引入 `EncryptedSharedPreferences`；遵守用户约束，无需额外审批。

---

## 8. 配置与环境

| 配置项 | 管理方式 | 开发环境值 | 生产环境值 | 说明 |
|--------|---------|----------|-----------|------|
| API Base URL | `AppConfig` | 现有配置 | 现有配置 | comments API 继续复用 |
| 登录态判断 | `AuthSessionProvider` | 当前默认 `false` | 后续真实实现接管 | 评论写操作不直接读取 BuildConfig |

> ⚠️ 禁止硬编码任何常量。继续通过 `AppConfig` / 依赖注入管理配置。

---

## 9. API 调用清单

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| `GET /api/dramas/{id}/comments` | 打开评论抽屉、切换排序、重试、触底分页 | `dramaId/page/pageSize/sort` | 更新评论列表和 `totalCount` | 局部错误态 / footer error |
| `POST /api/dramas/{id}/comments` | 点击发送 | `inputText.trim()` | 顶部插入新评论、输入清空、总数+1 | 本地校验 / `RequireLogin` / Snackbar |
| `POST /api/dramas/{id}/comments/{commentId}/like` | 点击评论点赞 | `dramaId/commentId` | 局部更新 liked 和 likeCount | `RequireLogin` / 单项错误提示 |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Android 端实现方式 |
|---------|---------------|-------------------|
| 评论承载方式 | 页面内半屏抽屉 | 首页与播放器均通过 `ModalBottomSheet` 打开 |
| 抽屉上下文唯一性 | 同一时刻只一个 `dramaId` | 宿主页持有单一 active comment context |
| 首屏请求参数 | `page=1&pageSize=20&sort=latest` | `CommentSheetViewModel` 默认 query |
| 热评参数兼容 | 支持 `hot` | `CommentSort.HOT` 完整透传 |
| 评论总数来源 | `pagination.total` | 存入 `totalCount` 并显示在 sheet header |
| 发评论成功处理 | 顶部插入、清空输入、总数+1 | 不整页重刷 |
| 点赞成功处理 | 局部更新目标评论 | 使用 `commentId` 精准替换 |
| 匿名可读、登录可写 | 未登录写操作需登录 | `AuthSessionProvider.isLoggedIn()` 为 false 时发 effect |
| 登录恢复策略 | 只恢复上下文，不自动重放 | effect 携带完整 `CommentLoginContext`；登录成功后按 `returnRoute` 回到来源页，并依据 `source + dramaId + action` 重新打开评论抽屉 |
| 错误隔离 | 评论错误不影响宿主页 | 所有评论错误只在 sheet 内展示 |

---

## 11. 边界与错误处理

### 11.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| 网络层 | `ApiResult.Success/Error/Exception` | 统一网络成功/失败分支 |
| ViewModel | `viewModelScope.launch + when(ApiResult)` | 区分错误态、分页错误、require-login |
| UI 层 | `Snackbar` / 内联错误 / footer 错误 | 不打断首页/播放器主内容 |

### 11.2 错误码映射表

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `VALIDATION_ERROR` | 评论内容不合法 | 输入框下方提示 / Snackbar |
| `UNAUTHORIZED` | 请先登录后再操作 | `RequireLogin(CommentLoginContext)` |
| `DRAMA_NOT_FOUND` | 当前短剧不存在 | 评论抽屉错误态 |
| `COMMENT_NOT_FOUND` | 评论不存在或已失效 | Snackbar |
| `INTERNAL_ERROR` | 加载失败，请稍后重试 | 错误态 + 重试 |
| `SERVICE_UNAVAILABLE` | 服务暂不可用，请稍后重试 | 错误态 / footer 重试 |
| `NETWORK_ERROR` | 网络异常，请检查后重试 | 错误态 / footer 重试 |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| 当前登录态固定为未登录 | `isLoggedIn() == false` | 所有写操作稳定触发 `RequireLogin` effect | 🔴 |
| 切换不同 drama 打开评论 | 在首页或播放器点开另一部剧 | 重置旧评论状态并重新加载 | 🔴 |
| 连续点击发送 | 快速点发送按钮 | `isSubmitting` 禁用发送按钮 | 🔴 |
| 连续点击点赞 | 多次点同一条评论 | `likingCommentIds` 单项加锁 | 🔴 |
| 关闭抽屉后重新打开 | 同一 drama 重入 | 可重载数据，首版不承诺跨次缓存 | 🟡 |
| 切换排序时已有分页数据 | `latest <-> hot` | 清空并回顶重载第一页 | 🟡 |
| 评论内容纯空白 | `trim().isBlank()` | 本地先拦截，不发请求 | 🔴 |
| `commentId` 失效 | 后端返回 404 | 不更新 liked，本地提示失败 | 🟡 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `CommentBottomSheet` | ✅ | ✅ | ✅ | ✅ | — |
| `CommentComposer` | — | ✅ | ✅ | ✅ | — |
| `CommentRow` | 单项点赞中 | ✅ | — | 单项失败轻提示 | — |

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | `CommentSheetViewModel` 状态机 | 核心状态全覆盖 | JUnit4 + MockK + Turbine |
| Repository 测试 | `CommentRepositoryImpl` DTO 映射与错误透传 | 关键映射覆盖 | JUnit4 + MockK |
| DTO 测试 | `CommentDto` / response DTO 到 domain 映射 | 全字段覆盖 | JUnit4 |
| UI 测试 | 首页/播放器评论入口存在与基础交互 | 关键入口覆盖 | Compose Testing |

### 12.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| A1 | 首次打开评论抽屉加载成功 | 正常网络 | `loadIfNeeded()` | 列表非空、总数正确 | ViewModel |
| A2 | 首次打开评论抽屉为空 | 接口返回空数组 | `loadIfNeeded()` | `listState == Empty` | ViewModel |
| A3 | 首次加载失败 | 接口异常 | `loadIfNeeded()` | `listState == Error` | ViewModel |
| A4 | 发评论成功 | 已登录 + 合法输入 | `submitComment()` | 顶部插入评论、输入清空、总数+1 | ViewModel |
| A5 | 发评论空白本地拦截 | 输入仅空白 | `submitComment()` | 不发请求，显示错误 | ViewModel |
| A6 | 未登录发评论 | `isLoggedIn=false` | `submitComment()` | 发出 `RequireLogin` | ViewModel |
| A7 | 点赞成功 | 已登录 | `toggleLike(commentId)` | 单项 liked 与计数更新 | ViewModel |
| A8 | 未登录点赞 | `isLoggedIn=false` | `toggleLike(commentId)` | 发出 `RequireLogin` | ViewModel |
| A9 | 切换排序重置分页 | 已有列表 | `selectSort(HOT)` | 清空并重新加载第一页 | ViewModel |
| A10 | 首页评论按钮存在 | 渲染首页卡片 | 查看 action row | 有“评论”按钮 | UI |
| A11 | 播放器评论芯片触发回调 | 渲染 `PlayerRightActionBar` | 点击“评论” | 回调被触发 | UI |
| A12 | DTO 映射 snake_case | 接口返回 snake_case 字段 | 解析 DTO | domain 字段正确 | DTO/Repository |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| `CommentRepository` | MockK mock | ViewModel 测试 |
| `AuthSessionProvider` | MockK mock | 已登录/未登录分支 |
| `ApiService` | MockK mock | Repository / data source 测试 |
| Compose UI | `createComposeRule()` | 入口与交互测试 |

---

## 13. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| — | — | 本期不新增开源依赖 | 继续使用现有 Compose / Retrofit / MockK / Turbine |

> ⚠️ 本期不引入 Jetpack Security / `EncryptedSharedPreferences`，因此不触发额外审批要求。

---

## 14. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 当前 `AuthSessionProvider.isLoggedIn()` 固定 false | 写操作始终走登录拦截，无法真发评论/点赞 | 🟡 | 高 | 设计中明确这是当前代码基线，UI 仍需完整承接 require-login | 后续接入 PRD-08 登录能力后直接打通 |
| 首页评论入口可能引入卡片布局拥挤 | 首页卡片视觉拥堵 | 🟡 | 中 | 保持 action row 三按钮同层级，必要时压缩按钮文案/样式 | 退回为 icon+text 轻量按钮 |
| 评论状态散落在首页/播放器中 | 维护成本上升 | 🟡 | 中 | 评论列表逻辑集中在 `CommentSheetViewModel` | 后续抽出 coordinator |
| `hot` 首版与 `latest` 实际一致 | 用户感知不足 | 🟢 | 高 | UI 完整支持排序 contract，后续仅补后端算法 | 临时隐藏 hot 不是首选，不建议 |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/features/video-player/index.md` | 播放器评论入口现状 | 评论芯片当前是占位 |
| `wiki/features/homepage-feed/index.md` | 首页卡片动作区 | 当前只有观看 / 详情 |
| `wiki/features/comments/index.md` | 评论能力现状 | 当前没有抽屉与 API 接入 |
| `wiki/features/auth/index.md` | 登录态现状 | 当前真实 auth module 仍未完整打通 |
| `wiki/features/ranking/index.md` | 登录拦截模式 | Android 已有 `RequireLogin(returnRoute)` 语义 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 当前没有 comments endpoints，需要新增 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt` | `ApiResult` 封装模式参考 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthSessionProvider.kt` | 当前只有 `isLoggedIn()` 能力 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt` | 默认 provider 目前固定返回未登录 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | `RequireLogin(returnRoute)` 设计参考 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 现有 route string 构造方式 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 首页卡片 action row 当前只有观看/详情 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/components/PlayerComponents.kt` | 评论按钮当前是 `AssistChip(onClick = {})` |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt` | 已有 `ModalBottomSheet` 模式可复用 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerUiState.kt` | 当前没有 comments state |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` | 当前只有播放/选集/倍速/点赞/收藏逻辑 |
