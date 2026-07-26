# Android 端技术方案：PRD-03 完整观看播放器

> 创建日期：2026-07-26
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

Android 端延续当前 Compose + ViewModel + Repository + DataSource + Retrofit 分层，在不破坏首页 Feed 已完成的 home graph、deeplink 兼容与 Hilt 注入模式前提下，将 `PlayerScreen` 从占位页演进为真实播放器页面，并补齐匿名续播、切集、倍速、生命周期和沉浸式导航行为。

```
┌───────────────────────────────────────────────────────────┐
│ UI Layer (Compose)                                        │
│  ├── PlayerScreen                                         │
│  ├── PlayerTopBar / PlayerRightActionBar                  │
│  ├── PlayerEpisodeBottomBar / EpisodePickerSheet          │
│  ├── SpeedPickerSheet                                     │
│  └── PlayerStatusContent                                  │
├───────────────────────────────────────────────────────────┤
│ ViewModel Layer                                            │
│  └── PlayerViewModel                                      │
│      ├── bootstrap(progress -> episodes -> start)         │
│      ├── switch episode / speed / lifecycle               │
│      └── StateFlow UI state                               │
├───────────────────────────────────────────────────────────┤
│ Data Layer                                                 │
│  ├── PlayerRepository / PlayerRemoteDataSource            │
│  ├── ApiService                                           │
│  └── PlaybackSessionStore                                 │
└───────────────────────────────────────────────────────────┘
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `android/.../feature/player/ui/PlayerScreen.kt` | 扩展 | 从展示 `Video ID` 的占位页改为真实播放器页面 |
| `android/.../feature/player/viewmodel/PlayerViewModel.kt` | 扩展 | 从只读 route 参数演进为完整状态机 |
| `android/.../navigation/NavGraph.kt` | 扩展 | 在 player route 命中时隐藏全局 `NavigationBar` |
| `android/.../navigation/AppDestination.kt` | 不变 | `play/{videoId}` 为 canonical，`player/{videoId}` 为 alias |
| `android/.../core/network/ApiService.kt` | 扩展 | 新增 progress/episodes/start/stop 端点的强类型定义 |
| `android/.../core/network/ApiResult.kt` | 轻扩展 | 延续现有 Success/Error/Exception 结果模型 |
| `android/.../core/network/AuthInterceptor.kt` | 不变 | 仍只负责未来 auth；本期不把 playback session header 放进 auth interceptor |
| `android/.../data/datasource/DramaRemoteDataSource.kt` | 不变 | 首页 Feed 继续使用 |
| Player 相关 repository / datasource / store / usecase | 新增 | 补齐播放器专属数据链路 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt` | 修改 | 渲染真实播放器页面、bottom sheet、错误态 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` | 修改 | 增补 bootstrap、切集、倍速、stop 上报、生命周期逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | player route 命中时隐藏底部 `NavigationBar` |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增强类型 DTO 与 `@Header("X-Playback-Session-Id")` 支持 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/NetworkModule.kt` | 可能修改 | 注入新 datasource/repository 所需依赖 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 绑定 `PlayerRepository` / `PlaybackSessionStore` |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/PlayerRemoteDataSource.kt` | 新增 | 封装 progress/episodes/start/stop 请求 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/PlayerRepositoryImpl.kt` | 新增 | 组合 remote datasource 与 session store |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/PlayerRepository.kt` | 新增 | 定义播放器 repository 接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/*.kt` | 新增 | progress / episodes / start / stop 四类 use case |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/PlaybackSessionStore.kt` | 新增 | 本地持久化匿名 session id |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/components/*.kt` | 新增 | 顶部栏、互动栏、选集栏、状态视图 |
| `android/app/src/test/java/.../feature/player/viewmodel/PlayerViewModelTest.kt` | 修改 | 从 route 参数测试扩展到完整状态机测试 |
| `android/app/src/test/java/.../feature/player/ui/PlayerScreenTest.kt` | 新增 | 覆盖 loading/error/no-resource/ready 渲染 |

---

## 3. UI 层设计

### 3.1 组件层级树

```
PlayerScreen
├── PlayerScaffoldContent
│   ├── NativePlayerHost
│   ├── PlayerTopBar
│   ├── PlayerRightActionBar
│   ├── PlayerBottomInfo
│   └── PlayerEpisodeDock
├── PlayerLoadingContent
├── PlayerErrorContent
├── PlayerNoResourceContent
├── EpisodePickerSheet (ModalBottomSheet)
└── SpeedPickerSheet (ModalBottomSheet / DropdownMenu)
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `PlayerScreen` | Composable | 播放页根节点 + 状态分发 | 否 |
| `NativePlayerHost` | Composable | 包装原生播放器宿主，承载播放 / 暂停 / 进度显示能力 | 否 |
| `PlayerTopBar` | Composable | 返回、当前集、倍速、更多 | 否 |
| `PlayerRightActionBar` | Composable | 点赞 / 收藏 / 评论 / 分享入口 | 否 |
| `PlayerBottomInfo` | Composable | 标题、标签、简介 |
| `PlayerEpisodeDock` | Composable | 底部选集栏 |
| `EpisodePickerSheet` | Composable | 集数选择面板 |
| `SpeedPickerSheet` | Composable | 7 档倍速选择 |
| `PlayerStatusContent` | Composable | loading/error/no-resource 通用状态承载 | 可复用 |

### 3.3 Composable 接口定义

```kotlin
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }
    DisposableEffect(Unit) {
        onDispose { viewModel.onScreenDisposed() }
    }

    when (uiState.screenState) {
        PlayerScreenState.Bootstrapping -> PlayerLoadingContent()
        PlayerScreenState.Error -> PlayerErrorContent(...)
        PlayerScreenState.NoResource -> PlayerNoResourceContent(...)
        else -> PlayerScaffoldContent(...)
    }
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| `NavGraph -> PlayerScreen` | `hiltViewModel()` + nav args | 读取 `videoId` |
| `PlayerScreen -> 子 Composable` | 参数 + Lambda | 顶部栏、互动栏、底部选集栏 |
| 子 Composable -> ViewModel | Lambda callback | 切集、倍速、返回、点赞收藏 |
| 跨组件共享 | `StateFlow` + `collectAsStateWithLifecycle` | 页面全局状态 |

### 3.5 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | 竖屏优先，播放器区域占满可用空间 | 首版聚焦短剧竖屏体验 |
| 横竖屏 | 首版保持竖屏主布局，不额外做横屏控制条适配 | 避免范围膨胀 |
| 字体缩放 | 核心操作按钮固定最小点击区，文案允许 ellipsis | 避免控件超出视频区 |
| 深色模式 | Material3 + 半透明遮罩层 | 与首页主题保持一致 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 UI | 职责 |
|-----------|---------|------|
| `PlayerViewModel` | `PlayerScreen` | 播放 bootstrap、切集、倍速、生命周期与 stop 上报 |

### 4.2 状态定义

```kotlin
data class PlayerUiState(
    val dramaId: String = "",
    val screenState: PlayerScreenState = PlayerScreenState.Idle,
    val episodes: List<Episode> = emptyList(),
    val currentEpisode: Episode? = null,
    val resumeProgress: Double = 0.0,
    val currentSpeed: PlaybackSpeed = PlaybackSpeed.X1_0,
    val isEpisodeSheetVisible: Boolean = false,
    val isSpeedSheetVisible: Boolean = false,
    val liked: Boolean = false,
    val favorited: Boolean = false,
    val errorMessage: String? = null,
)
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `dramaId` | `String` | 从 `SavedStateHandle` 读取 | 由旧 `videoId` 参数映射而来 |
| `screenState` | `PlayerScreenState` | `Idle` | 页面整体状态机 |
| `episodes` | `List<Episode>` | `emptyList()` | 当前 drama 的剧集列表 |
| `currentEpisode` | `Episode?` | `null` | 当前目标集 |
| `resumeProgress` | `Double` | `0.0` | 起播 / 恢复秒数 |
| `currentSpeed` | `PlaybackSpeed` | `X1_0` | 页面会话级倍速 |
| `liked` / `favorited` | `Boolean` | `false` | 首版本地反馈态 |
| `pendingStopReport` | 内部字段 | `false` | 避免重复 stop 上报 |
| `requestJob` | 内部字段 | `null` | 取消并发 bootstrap / 切集请求 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | UI 层表现 |
|---------|---------|----------|
| `Idle` | 尚未加载 | 空壳 |
| `Bootstrapping` | 正在拉 progress / episodes | 页面级 loading |
| `Ready` | 已解析目标 episode，播放器待开始/已开始 | 显示播放器与覆盖层；播放 / 暂停 / 进度显示优先由原生播放器承载 |
| `Playing` | 正在播放 | 正常播放器页 |
| `Paused` | 用户暂停 / 进入后台 | 保留当前页 |
| `SwitchingEpisode` | 切集中 | 保留页面并显示轻量 loading |
| `NoResource` | 无可播放集，或剧集列表为空 | 无资源态 |
| `Error` | 接口失败 / 播放失败 | 错误态 + 重试 |

---

## 5. Navigation 路由设计

### 5.1 导航方案

- 继续使用 Navigation Compose。
- `play/{videoId}` 与 `player/{videoId}` 两个 route 继续存在，但都进入同一个 `PlayerScreen`。
- 在 `NavGraph` 中根据当前 destination 是否为 Player route，动态隐藏 `NavigationBar`。

### 5.2 路由清单

| 路由标识 | 目标 Composable/Activity | 参数 | 导航方式 | 说明 |
|---------|------------------------|------|---------|------|
| `play/{videoId}` | `PlayerScreen` | `videoId` | `navController.navigate` | canonical route |
| `player/{videoId}` | `PlayerScreen` | `videoId` | `navController.navigate` / deeplink | legacy alias |
| `detail/{dramaId}` | `DramaDetailScreen` | `dramaId` | `navController.navigate` | 不受本期影响 |

### 5.3 导航图

- `NavGraph` 继续在 home graph 下注册两个 Player routes。
- 新增 `shouldShowBottomBar(destination)` 逻辑：当 destination 为 `PLAY` 或 `PLAYER_ALIAS` 时返回 `false`。
- `onBack` 统一调用 `navController.popBackStack()`。

### 5.4 Deep Link 处理（如适用）

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `djsdrama://play/{id}` | `PendingRoute.Play(videoId)` | `id` |
| `djsdrama://player/{id}` | `PendingRoute.Play(videoId)` | `id`，统一映射到同一播放流程 |

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | 现有 Retrofit + OkHttp | 继续复用 |
| 数据模型 | `@Serializable` DTO | 新增 player / episode DTO |
| 请求头注入 | `@Header("X-Playback-Session-Id")` 显式传参 | 仅对指定接口生效 |
| 错误处理 | `ApiResult` | DataSource 层把异常转换为 `ApiResult.Error/Exception` |
| 本地 session | `PlaybackSessionStore` | 请求前读取/生成 |

### 6.2 API 接口定义

```kotlin
interface ApiService {
    @GET("player/progress")
    suspend fun getPlaybackProgress(
        @Header("X-Playback-Session-Id") playbackSessionId: String,
        @Query("dramaId") dramaId: String,
    ): PlayerProgressResponseDto

    @GET("dramas/{id}/episodes")
    suspend fun getDramaEpisodes(
        @Path("id") dramaId: String,
    ): EpisodeListResponseDto

    @POST("player/start")
    suspend fun startPlayback(
        @Header("X-Playback-Session-Id") playbackSessionId: String,
        @Body body: PlayerStartRequestDto,
    ): PlayerStartResponseDto

    @POST("player/stop")
    suspend fun stopPlayback(
        @Header("X-Playback-Session-Id") playbackSessionId: String,
        @Body body: PlayerStopRequestDto,
    ): PlayerStopResponseDto
}
```

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| bootstrap 网络超时 | 1 | 固定短延迟 | 降低瞬时失败概率 |
| `stopPlayback` 失败 | 0 | 不重试 | 作为 best-effort 上报 |
| 用户点击“重试” | 用户触发 | 人工重试 | 重新走 bootstrap |

### 6.4 网络状态监听

- 本期不新增全局 ConnectivityManager 监听逻辑。
- 通过 `Lifecycle` + `ApiResult.Exception` 处理切后台与网络波动即可。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| 匿名 `playbackSessionId` | `DataStore<Preferences>` 封装 `PlaybackSessionStore` | `player_playback_session_id` | 不过期 | 首版先用 app-private 持久化；如后续用户批准安全依赖，可替换为加密实现 |
| 页面会话倍速 | 内存 | `PlayerUiState.currentSpeed` | 页面销毁即失效 | 不做跨会话持久化 |
| 当前 episodes | 内存 | `PlayerUiState.episodes` | 页面销毁即失效 | 避免重复请求 |
| 点赞/收藏反馈态 | 内存 | `liked` / `favorited` | 页面销毁即失效 | 首版无后端持久化 |

> 说明：Android 端当前仓库尚未引入可满足本期真播、倍速与生命周期要求的系统级 Compose 播放封装；若不新增依赖，很难在 coding 阶段完整交付真实视频播放。因此“是否批准引入 `androidx.media3`”是 design-human-review 前必须显式确认的前置决策，而不是 coding 阶段再讨论的可选优化项。

### 7.2 Store 设计

```kotlin
interface PlaybackSessionStore {
    suspend fun getOrCreateSessionId(): String
}

class DataStorePlaybackSessionStore(...) : PlaybackSessionStore {
    override suspend fun getOrCreateSessionId(): String { ... }
}
```

### 7.3 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| `Episode[]` | ViewModel 内存缓存 | 页面生命周期 | ViewModel 清理时释放 |
| 当前进度 | 由播放器实例实时提供 | 页面生命周期 | stop 上报后不另存 |

### 7.4 数据库 Migration

- 本期不引入 Room。
- 仅新增 DataStore key，不涉及 schema migration。

---

## 8. 配置与环境

| 配置项 | 管理方式 | 开发环境值 | 生产环境值 | 说明 |
|--------|---------|----------|-----------|------|
| API Base URL | `AppConfig` / `BuildConfig` | 现有开发环境 | 环境注入 | 继续复用 |
| 播放器实现 | 抽象 `NativePlayerAdapter` | 待编码阶段落地 | 环境无差异 | 保持 UI 与播放器引擎解耦 |
| 三方播放器依赖 | 待批准 | — | — | 若采用 Media3 需用户批准 |

> 说明：Android 真正落地“可播放视频”通常推荐 Media3/ExoPlayer；因项目约束要求新增三方依赖需先获用户同意，本方案在 design 阶段先定义 `NativePlayerAdapter` 抽象，不在此阶段强绑定具体依赖。若用户批准，coding 阶段优先落地 Media3；若未批准，则只能保留架构与接口接线，无法完整交付真实视频播放。

---

## 9. API 调用清单

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| `GET /api/player/progress` | 页面 bootstrap 第一步 | route `videoId -> dramaId` + `PlaybackSessionStore` | 保存恢复信息 | 错误时进入 error/回退 |
| `GET /api/dramas/:id/episodes` | 页面 bootstrap 第二步 | `dramaId` | 填充 `episodes` 并选择目标集 | 全无资源则 no-resource |
| `POST /api/player/start` | 确定默认 / 恢复集、切集后 | `currentEpisode` + `resumeProgress` + sessionId | 创建/更新播放器上下文 | 进入 error |
| `POST /api/player/stop` | 返回、切集前、进入后台 | 当前 episode + progress + duration + sessionId | best-effort 保存最近进度 | 记录日志，不阻塞流程 |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Android 端实现方式 |
|---------|---------------|-------------------|
| 路由参数语义 | `videoId` 实际表示 `dramaId` | `PlayerViewModel` 读取 `SavedStateHandle` 后映射为 `dramaId` 属性 |
| legacy route 兼容 | `player/{id}` 仍兼容 | `NavGraph` 两条 route 均进入同一 `PlayerScreen` |
| Bootstrap 顺序 | `progress -> episodes -> start` | `loadIfNeeded()` 内串行执行四步流程 |
| 默认集选择 | 第一条可播放集 | `episodes.firstOrNull { it.videoUrl.isNotBlank() }` |
| 恢复策略 | 优先恢复集，失效则回退默认集 | ViewModel 在拿到 progress + episodes 后合并决策 |
| 切集从 0 秒开始 | 不额外查历史 | `switchEpisode()` 对新 episode 固定 `progress=0` |
| 匿名续播 session | 本地 UUID 持久化 | `PlaybackSessionStore` 负责 `getOrCreateSessionId()` |
| 底部导航隐藏 | Player route 不显示 NavigationBar | `NavGraph` 基于 destination 控制 bottomBar 可见性 |
| stop 上报 best-effort | 失败不阻塞退出 | `viewModelScope.launch` 异步上报，失败仅记录 |

---

## 11. 边界与错误处理

### 11.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| 网络层 | Retrofit 异常 -> `ApiResult.Exception` | 统一捕获网络与序列化异常 |
| DataSource | 解析错误体 -> `ApiResult.Error` | 后续补齐后端错误码映射 |
| ViewModel | `when(ApiResult)` | 转为 `PlayerUiState` |
| UI 层 | 内联错误页 / Snackbar / BottomSheet 禁用态 | 不依赖全局页面跳转 |

### 11.2 错误码映射表

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `INVALID_PARAMS` | 页面参数无效 | 返回上一页 |
| `INVALID_PLAYBACK_SESSION` | 播放身份异常，请重试 | 重新生成 session 后重试一次；失败则错误页 |
| `DRAMA_NOT_FOUND` | 内容不存在 | 错误页 + 返回 |
| `EPISODE_NOT_FOUND` | 当前剧集不存在 | 回退默认集；失败则错误页 |
| `EPISODE_NOT_PLAYABLE` | 当前剧集暂无资源 | 在选集面板置灰，必要时回退默认集 |
| `INTERNAL_ERROR` | 加载失败，请重试 | 错误页 + 重试按钮 |
| `NETWORK_ERROR` | 网络异常，请检查后重试 | 错误页 + 重试按钮 |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| Activity/Composable 销毁 | 返回上一页 / 进程回收 | 取消进行中的 bootstrap/cutover job；stop 上报单独 best-effort | 🔴 |
| App 进入后台 | 生命周期 `ON_STOP` | 若当前有可上报 episode，则调用 stop，UI 置为 Paused | 🔴 |
| App 返回前台 | 生命周期 `ON_START` | 不重新 bootstrap；保留当前集与当前倍速 | 🟡 |
| 用户快速连点切集 | 多次点击 episode | cancel 旧 job，仅保留最后一次切集 | 🔴 |
| 恢复集失效 | progress 中 episode 不在 episodes 列表 | 回退第一条可播集 | 🔴 |
| 全部集无资源 | `videoUrl` 全为空 | `screenState = NoResource` | 🔴 |
| 倍速切换时播放器未就绪 | 无 native player 实例 | 忽略操作或禁用按钮 | 🟡 |
| 旧 deeplink 命中 alias | `djsdrama://player/{id}` | 正常映射到相同播放器流程 | 🟢 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `PlayerScreen` | `PlayerLoadingContent` | 播放器页 | `PlayerNoResourceContent` | `PlayerErrorContent` | 参数无效/内容不存在 -> 返回 |
| `EpisodePickerSheet` | 可省略 | 集数列表 + 高亮 + 置灰 | 空列表文案 | 不单独展示 | 不单独展示 |
| `SpeedPickerSheet` | 不展示 | 正常展示 | 不适用 | 不适用 | 不适用 |

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | `PlayerViewModel` 业务逻辑 | 核心路径全覆盖 | JUnit4 + MockK |
| UI 测试 | `PlayerScreen` 状态渲染与主要交互 | 关键态覆盖 | Compose Testing |
| 导航测试 | Player route / alias / bottomBar 隐藏 | 关键路径覆盖 | JUnit / Navigation 测试 |

### 12.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| A-01 | route 参数正确映射为 dramaId | `SavedStateHandle(videoId=001)` | 创建 ViewModel | `dramaId == 001` | 单元 |
| A-02 | 无历史默认进入第一可播集 | progress 无历史，episodes 有可播集 | `loadIfNeeded()` | `currentEpisode == firstPlayable` | 单元 |
| A-03 | 有历史恢复到指定集 | progress 命中某 episode | `loadIfNeeded()` | `resumeProgress` 正确 | 单元 |
| A-04 | 恢复集失效回退默认集 | progress 中 episode 不在列表 | `loadIfNeeded()` | 选择第一可播集 | 单元 |
| A-05 | 全部集无资源 | episodes 全无 `videoUrl` | `loadIfNeeded()` | `screenState == NoResource` | 单元 |
| A-06 | 剧集列表为空进入 noResource | drama 存在但 `episodes=[]` | `loadIfNeeded()` | `screenState == NoResource` | 单元 |
| A-07 | 切集先 stop 后 start | 当前已有 episode | `switchEpisode()` | 先 stop 再 start(progress=0) | 单元 |
| A-08 | 进入后台触发 stop | 页面在播放中 | 生命周期切到后台 | 发起 stop 上报 | 单元 |
| A-09 | Player route 隐藏 bottom bar | destination 为 `play/{videoId}` | 渲染 NavGraph | bottom bar 不显示 | UI / 导航 |
| A-10 | alias route 与 canonical 行为一致 | destination 为 `player/{videoId}` | 渲染 PlayerScreen | 行为与 canonical 相同 | UI / 导航 |
| A-11 | 原生播放器宿主可承载基础控制 | 播放器已 ready | 渲染 `NativePlayerHost` | 页面不额外依赖自定义控制条即可满足播放 / 暂停 / 进度显示 | UI / 单元 |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| `PlayerRepository` | MockK | 验证 progress / episodes / start / stop 调用顺序 |
| `PlaybackSessionStore` | Fake | 覆盖首次生成 / 已存在 session |
| native player adapter | Fake | 不依赖真实播放器内核做单元测试 |
| `SavedStateHandle` | 原生测试对象 | 验证 route 参数兼容逻辑 |

---

## 13. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| `androidx.media3`（推荐，待用户批准） | 待定 | 真实视频播放内核 | Android 首版真实播放器的主流方案，支持倍速与生命周期控制 |
| 无依赖变更（当前 design 阶段） | — | 仅完成架构与接口设计 | 遵守“新增依赖需用户同意”的约束 |

> 说明：Android 若要真正交付“可播放视频”，推荐在 coding 阶段引入 Media3；该动作需要用户批准。若未批准，则 Android 只能完成 ViewModel、页面结构和 API 链路，无法完整交付真实视频播放。

---

## 14. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 未获准新增播放器依赖 | Android 真播能力 | 🔴 | 高 | design 阶段先抽象 `NativePlayerAdapter`；获准后用 Media3 实现 | 未获批准时只能交付非真实播放的结构层实现 |
| 当前 `ApiService` 对播放器接口全是 `Map<String, String>/Unit` 占位 | 类型安全与错误处理 | 🟡 | 高 | 改为强类型 DTO + `@Header` 传参 | 先局部定义 PlayerApiService |
| `NavGraph` 全局 Scaffold 默认显示底部栏 | 沉浸式体验被破坏 | 🔴 | 高 | 基于 destination 动态隐藏 bottom bar | 临时在 PlayerScreen 顶部再做遮挡，但不推荐 |
| DataSource 当前不解析服务端错误码 | 错误体验不稳定 | 🟡 | 中 | 增补 HTTP 错误体解析 -> `ApiResult.Error` | 至少兜底为通用错误页 |
| 切集并发导致状态错乱 | 播放稳定性 | 🔴 | 中 | 保存 job，切集前 cancel 旧任务 | 回退到页面级 loading 重建 |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/architecture/overview.md` | Android 架构 / 技术栈 | Android 使用 Compose + Navigation Compose + Hilt |
| `wiki/features/app-shell/index.md` | Android 端 / 状态管理 | `NavGraph` 当前包在全局 `Scaffold(bottomBar)` 中 |
| `wiki/features/video-player/index.md` | Android 端 / 状态管理 | 当前 PlayerScreen 仅展示 `Video ID` |
| `wiki/features/data-models/index.md` | Episode / Player 请求模型 | 后端已有 Episode 与 start/stop 请求模型 |
| `PRODUCT.md` | 页面承载策略 | 除 mall/earn 外按 Native 实现 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `docs/specs/2026-07-26-prd-03-full-player/design.md` | shared 层定义了 bootstrap、匿名续播、沉浸式导航与错误语义 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt` | 当前仍是占位页 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` | 当前只读取 `videoId/id` |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | Player route 当前位于带 bottom bar 的全局 Scaffold 中 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | `play/{videoId}` canonical，`player/{videoId}` alias |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt` | 现有 DataSource + ApiResult 模式可复用 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt` | 现有 Repository 模式可复用 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/DramaRepository.kt` | 现有 domain repository 结构可复用 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetDramasUseCase.kt` | 现有 use case 结构可复用 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 当前播放器接口仍是 `Unit/Map<String, String>` 占位定义 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiClient.kt` | 现有 Retrofit/OkHttp 配置可复用 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt` | 仅保留未来 auth；不适合作为 playback session header 注入器 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/NetworkModule.kt` | 现有网络依赖注入入口 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 当前只绑定 DramaRepository，需扩展 PlayerRepository |
| `android/app/src/test/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModelTest.kt` | 当前仅验证 route 参数，适合作为扩展入口 |
