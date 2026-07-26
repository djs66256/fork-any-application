# iOS 端技术方案：PRD-03 完整观看播放器

> 创建日期：2026-07-26
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

iOS 端延续当前 SwiftUI + MVVM + Repository / UseCase 分层，在不破坏首页 Feed 已有导航与数据链路的前提下，把 `PlayerView` 从参数占位页演进为真实播放器页面，并补齐匿名续播、剧集切换、倍速、生命周期与沉浸式承载。

```
┌─────────────────────────────────────────────────────────────┐
│ View Layer (SwiftUI)                                        │
│  ├── PlayerView                                             │
│  ├── PlayerTopBar / PlayerRightActionBar                    │
│  ├── EpisodePickerSheet / SpeedPickerDialog                 │
│  └── PlayerStatusView (loading / error / no-resource)       │
├─────────────────────────────────────────────────────────────┤
│ ViewModel Layer                                             │
│  └── PlayerViewModel                                        │
│      ├── bootstrap(progress -> episodes -> start)           │
│      ├── episode switching / speed / lifecycle              │
│      └── UI state machine                                   │
├─────────────────────────────────────────────────────────────┤
│ Domain / Data                                               │
│  ├── FetchPlayerProgressUseCase                             │
│  ├── FetchDramaEpisodesUseCase                              │
│  ├── StartPlaybackUseCase / StopPlaybackUseCase             │
│  ├── PlayerRepository                                       │
│  └── PlaybackSessionStore (Keychain)                        │
└─────────────────────────────────────────────────────────────┘
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` | 扩展 | 从占位文本页演进为播放器页面 |
| `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | 扩展 | 从仅持有 `videoId` 扩展为完整状态机与业务编排 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 扩展 | 继续承载 `.player(videoId:)`，但播放器页面需隐藏 Tab Bar |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 不变 | 对外 public route name 继续为 `play` |
| `ios/ShortDrama/Sources/Core/Network/APIEndpoint.swift` | 扩展 | 增加 `headers` 支持，满足 `X-Playback-Session-Id` 透传 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 扩展 | 写入 endpoint headers、解析统一错误结构 |
| `DramaRepository` / `FetchDramasUseCase` | 不变 | 首页 Feed 现有链路不受影响 |
| Player 相关 Repository / UseCase / DTO / Store | 新增 | 补齐播放器数据链路与本地 session 存储 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` | 修改 | 渲染真实播放器页面、sheet、错误态与生命周期监听 |
| `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | 修改 | 增补 bootstrap、切集、倍速、续播、退出上报 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 播放页继续通过 `.player(videoId:)` 进入，但页面内部隐藏 Tab Bar |
| `ios/ShortDrama/Sources/Core/Network/APIEndpoint.swift` | 修改 | 新增 `headers` 属性默认实现 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 修改 | 支持 headers 注入与 `{ error: { code, message } }` 解析 |
| `ios/ShortDrama/Sources/Core/Storage/PlaybackSessionStore.swift` | 新增 | Keychain 持久化匿名 UUID |
| `ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift` | 新增 | 封装 progress / episodes / start / stop 四类请求 |
| `ios/ShortDrama/Sources/Data/Repositories/PlayerRepository.swift` | 新增 | 组织播放器相关 API 调用 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/PlayerRepositoryProtocol.swift` | 新增 | 定义播放器数据访问协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchPlayerProgressUseCase.swift` | 新增 | 查询续播进度 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchDramaEpisodesUseCase.swift` | 新增 | 查询剧集列表 |
| `ios/ShortDrama/Sources/Domain/UseCases/StartPlaybackUseCase.swift` | 新增 | 起播确认 |
| `ios/ShortDrama/Sources/Domain/UseCases/StopPlaybackUseCase.swift` | 新增 | 退出 / 切集上报 |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/*.swift` | 新增 | 顶部栏、互动栏、选集栏、选集面板、状态视图等组件 |
| `ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift` | 新增 | 覆盖 bootstrap、切集、倍速、退出上报 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 补充播放器导航 / 返回 / 隐藏 Tab Bar 相关预期 |

---

## 3. View 层设计

### 3.1 组件层级树

```
PlayerView
├── PlayerContainerView
│   ├── NativeVideoPlayerView
│   ├── PlayerTopBar
│   ├── PlayerRightActionBar
│   ├── PlayerBottomInfoView
│   └── PlayerEpisodeDock
├── PlayerLoadingView
├── PlayerErrorView
├── PlayerNoResourceView
├── SpeedPickerDialog (.confirmationDialog)
└── EpisodePickerSheet (.sheet)
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `PlayerView` | View | 播放页根视图与状态分发 | 否 |
| `NativeVideoPlayerView` | View | 包装系统 `VideoPlayer` / `AVPlayerLayer`，承载原生播放 / 暂停 / 进度显示能力 | 否 |
| `PlayerTopBar` | View | 返回、集数文案、倍速、更多按钮 | 否 |
| `PlayerRightActionBar` | View | 点赞 / 收藏 / 评论 / 分享入口 | 否 |
| `PlayerBottomInfoView` | View | 标题、标签、简介承载 | 否 |
| `PlayerEpisodeDock` | View | 底部固定选集栏 | 否 |
| `EpisodePickerSheet` | View | 剧集面板，当前集高亮、不可播放集置灰 | 否 |
| `SpeedPickerDialog` | View | 7 档倍速选择 | 否 |
| `PlayerStatusView` | View | loading / error / no-resource 通用承载 | 可复用 |

### 3.3 组件接口定义

```swift
struct PlayerView: View {
    @ObservedObject var viewModel: PlayerViewModel
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        ZStack {
            switch viewModel.uiState {
            case .bootstrapping:
                PlayerLoadingView()
            case .error(let message):
                PlayerErrorView(message: message, onRetry: { Task { await viewModel.retryBootstrap() } })
            case .noResource:
                PlayerNoResourceView(onBack: { viewModel.handleBack() })
            case .ready, .playing, .paused, .switchingEpisode:
                PlayerContainerView(viewModel: viewModel)
            }
        }
        .toolbar(.hidden, for: .tabBar)
        .task { await viewModel.loadIfNeeded() }
        .onChange(of: scenePhase) { _, phase in
            Task { await viewModel.handleScenePhaseChange(phase) }
        }
    }
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| `TabNavigationHostView -> PlayerView` | 构造函数注入 `PlayerViewModel(videoId:router:dependencies...)` | 入口参数与依赖注入 |
| `PlayerView -> 子组件` | 只读值 + closure | 顶部栏、互动栏、选集面板、倍速面板 |
| 子组件 -> ViewModel | Closure callback | 切集、倍速、点赞收藏、返回 |
| 跨层级共享 | `@EnvironmentObject NavigationRouter`（可选） | 返回 / 导航控制 |

### 3.5 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | 竖屏优先布局，横屏先不做沉浸切换 | PRD 以竖屏观看为主 |
| Dynamic Type | 核心文案可缩放，但播放器覆盖层限制最大行数 | 避免遮挡视频主体 |
| 深色模式 | 优先使用半透明深色遮罩 + DesignTokens | 保持播放器沉浸感 |
| 安全区域 | 视频区域 `ignoresSafeArea()`，覆盖层按 safe area 布局 | 顶部栏避开刘海和 Dynamic Island |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 View | 职责 |
|-----------|----------|------|
| `PlayerViewModel` | `PlayerView` | 完整播放器状态机、bootstrap、切集、倍速、退出上报、生命周期恢复 |

### 4.2 状态定义

```swift
@MainActor
final class PlayerViewModel: ObservableObject {
    enum UiState: Equatable {
        case idle
        case bootstrapping
        case ready
        case playing
        case paused
        case switchingEpisode
        case noResource
        case error(String)
    }

    @Published private(set) var uiState: UiState = .idle
    @Published private(set) var currentEpisode: Episode?
    @Published private(set) var episodes: [Episode] = []
    @Published private(set) var currentSpeed: PlaybackSpeed = .x1_0
    @Published var isEpisodeSheetPresented = false
    @Published var isSpeedDialogPresented = false
    @Published private(set) var liked = false
    @Published private(set) var favorited = false

    let videoId: String   // 兼容现有命名，业务语义为 dramaId
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `videoId` | `String` | 路由注入 | 对外沿用旧命名，内部按 `dramaId` 使用 |
| `uiState` | `UiState` | `.idle` | 页面整体状态机 |
| `episodes` | `[Episode]` | `[]` | 当前 drama 的剧集列表 |
| `currentEpisode` | `Episode?` | `nil` | 当前正在播放 / 准备播放的剧集 |
| `resumeProgress` | `Double` | `0` | 当前集起播位置或恢复位置 |
| `currentSpeed` | `PlaybackSpeed` | `.x1_0` | 页面会话级倍速 |
| `isEpisodeSheetPresented` | `Bool` | `false` | 选集面板状态 |
| `isSpeedDialogPresented` | `Bool` | `false` | 倍速面板状态 |
| `liked` / `favorited` | `Bool` | `false` | 首版本地反馈态 |
| `hasLoadedOnce` | `Bool` | `false` | 防重复 bootstrap |
| `requestTask` | `Task<Void, Never>?` | `nil` | 取消并发 bootstrap / 切集请求 |
| `hasPendingStopReport` | `Bool` | `false` | 避免重复 stop 上报 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | View 层表现 |
|---------|---------|-----------|
| `idle` | 尚未触发加载 | 空壳，不展示内容 |
| `bootstrapping` | 正在拉 progress / episodes | `PlayerLoadingView` |
| `ready` | 已确定 episode，等待播放器进入播放 | 页面骨架 + 播放器容器；复用系统原生播放控件 |
| `playing` | 播放器处于播放中 | 覆盖层正常显示，播放 / 暂停 / 进度显示由系统播放器控件承载 |
| `paused` | 用户暂停 / 进入后台 | 保留当前帧和控制层 |
| `switchingEpisode` | 切集中 | 保留当前页，显示轻量 loading |
| `noResource` | 所有 episode 都不可播放，或剧集列表为空 | 无资源态 + 返回 / 重试入口 |
| `error` | 接口失败 / 播放器初始化失败 | 错误态 + 重试按钮 |

---

## 5. Navigation 路由设计

### 5.1 导航方案

- 延续当前 `TabView + NavigationStack` 结构。
- 入口仍通过 `AppRoute.player(videoId:)` push 到 home Tab 的 path。
- 页面内部通过 `.toolbar(.hidden, for: .tabBar)` 隐藏底部 Tab Bar，返回时自动恢复。

### 5.2 路由清单

| 路由标识 | 目标页面 | 参数 | 导航方式 | 说明 |
|---------|---------|------|---------|------|
| `.player(videoId:)` | `PlayerView` | `videoId` | Push | 对外 public route name 继续为 `play` |
| `.dramaDetail(dramaId:)` | `DramaDetailView` | `dramaId` | Push | 不受本期影响 |
| `EpisodePickerSheet` | 剧集面板 | 当前 episodes | Sheet | 页面内局部弹层 |
| `SpeedPickerDialog` | 倍速面板 | 当前倍速 | ConfirmationDialog | 页面内局部弹层 |

### 5.3 路由管理

- `NavigationRouter` 不需要新增 route case；继续使用现有 `.player(videoId:)`。
- Player 页返回时调用 `router.dismiss()`。
- 对冷启动 deeplink 的消费逻辑保持不变：`pendingRoute` 被消费后进入 Player 页，再由 PlayerView 自行启动 bootstrap。

### 5.4 Deep Link 处理（如适用）

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `djsdrama://play/{id}` | `.player(videoId:)` | `id -> videoId` |

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | 现有 `APIClient` + `URLSession` | 继续复用 |
| 请求构建 | `APIEndpoint` | 新增 `headers` 支持 |
| 响应解析 | `Codable` + `JSONDecoder.convertFromSnakeCase` | 继续复用 |
| 错误处理 | `APIError` | 需兼容 backend `{ error: { code, message } }` 结构 |
| Session 注入 | `PlaybackSessionStore` + endpoint headers | 只对 progress/start/stop 注入 |

### 6.2 API 端点定义

```swift
protocol APIEndpoint {
    associatedtype Response: Decodable
    var path: String { get }
    var method: HTTPMethod { get }
    var queryItems: [URLQueryItem]? { get }
    var headers: [String: String] { get }
    var body: Encodable? { get }
}
```

播放器新增 endpoints：

- `PlayerEndpoints.GetProgress(dramaId:playbackSessionId:)`
- `PlayerEndpoints.GetDramaEpisodes(dramaId:)`
- `PlayerEndpoints.StartPlayback(playbackSessionId:request:)`
- `PlayerEndpoints.StopPlayback(playbackSessionId:request:)`

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 首次 bootstrap 网络超时 | 1 | 固定短延迟 | 避免进入播放器瞬时波动直接失败 |
| `stopPlayback` 失败 | 0 | 不重试 | 作为 best-effort 上报，不阻塞退出 |
| 用户点击“重试” | 用户触发 | 人工重试 | 由 ViewModel 重新走 bootstrap |

### 6.4 网络状态监听

- 本期不新增全局 `NWPathMonitor` 依赖。
- 使用 `scenePhase` + `APIError.network` 组合处理切后台 / 网络失败场景即可。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| 匿名 `playbackSessionId` | Keychain | `player.playback.session.id` | 不过期 | 满足“稳定且安全”的本地标识要求 |
| 页面会话倍速 | 内存 | `PlayerViewModel.currentSpeed` | 页面销毁即失效 | 首版不要求跨会话持久化 |
| 当前 episodes | 内存 | `PlayerViewModel.episodes` | 页面销毁即失效 | 避免反复拉取 |
| 点赞/收藏本地反馈态 | 内存 | `liked` / `favorited` | 页面销毁即失效 | 首版无服务端持久化 |

### 7.2 Keychain 设计

```swift
protocol PlaybackSessionStore {
    func getOrCreateSessionId() throws -> String
}

final class KeychainPlaybackSessionStore: PlaybackSessionStore {
    func getOrCreateSessionId() throws -> String { ... }
}
```

### 7.3 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| `Episode[]` | 页面级内存缓存 | 页面生命周期 | 页面释放时自动释放 |
| 当前播放进度 | 依赖播放器实例实时读取 | 页面生命周期 | stop 上报后不本地持久化 |

### 7.4 数据迁移策略

- 不涉及 CoreData migration。
- Keychain 若 key 不存在则生成 UUID；若读取失败则重新生成并覆盖写入。

---

## 8. 配置与环境

| 配置项 | 管理方式 | 开发环境值 | 生产环境值 | 说明 |
|--------|---------|----------|-----------|------|
| API Base URL | 现有 `AppConfig.apiBaseURL()` | 现有开发环境 | 环境注入 | 继续复用 |
| 播放页 feature flag | 暂不需要 | — | — | 本期直接替换占位实现 |
| 新三方播放器依赖 | 无 | — | — | iOS 采用系统 `AVPlayer` / `VideoPlayer`，不新增第三方依赖 |

---

## 9. API 调用清单

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| `GET /api/player/progress` | 页面 bootstrap 第一步 | `videoId(dramaId)` + `PlaybackSessionStore` | 保存恢复目标信息 | 进入 `error` 或回退默认集 |
| `GET /api/dramas/:id/episodes` | 页面 bootstrap 第二步 | `videoId(dramaId)` | 填充 `episodes`，选择目标集 | 全部无资源则 `noResource` |
| `POST /api/player/start` | 确定默认 / 恢复集后、切集后 | `currentEpisode` + `resumeProgress` + `sessionId` | 初始化 / 更新播放器实例 | 进入 `error` |
| `POST /api/player/stop` | 返回、切集前、进入后台 | 当前 episode + 当前进度 + duration + `sessionId` | best-effort 保存最近进度 | 记录日志，不阻塞退出 |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | iOS 端实现方式 |
|---------|---------------|---------------|
| 路由参数语义 | `videoId` 兼容命名，业务语义是 `dramaId` | `PlayerViewModel` 提供 `var dramaId: String { videoId }`，内部不再使用“video”语义 |
| Bootstrap 顺序 | `progress -> episodes -> start` | `loadIfNeeded()` 内按固定顺序串行执行 |
| 默认集选择 | 第一条可播放 Episode | ViewModel 从 `episodes.first(where: isPlayable)` 选择 |
| 恢复策略 | 先恢复 `episode_id + start_time` | 若恢复集仍可播放则用其初始化 `AVPlayer` 并 seek |
| 切集从 0 秒开始 | 切集不额外查询历史 | `switchEpisode()` 先上报当前集，再对新集 `progress=0` 调 `start` |
| 匿名 session 持久化 | 本地稳定 UUID | `KeychainPlaybackSessionStore` |
| 底部导航隐藏 | 进入播放页隐藏 Tab Bar | `PlayerView.toolbar(.hidden, for: .tabBar)` |
| 倍速会话级保留 | 切集沿用当前倍速 | `currentSpeed` 持有在 ViewModel 中，切换 episode 后重新应用到 player |
| stop 上报 best-effort | 失败不阻塞退出 | `defer`/独立 task 调用 stop，失败仅记录 |

---

## 11. 边界与错误处理

### 11.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| 网络层 | `APIClient` 抛 `APIError` | 解析服务器错误与网络错误 |
| ViewModel | `do-catch` | 转换为 `UiState.error` / `UiState.noResource` |
| View 层 | 内联状态页 + `confirmationDialog` / `sheet` | 不依赖全局 toast |
| 日志 | `print` / `os_log`（实现期选一） | 记录 stop 上报失败、恢复集失效 |

### 11.2 错误码映射表

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `INVALID_PARAMS` | 页面参数无效 | 返回上一页 |
| `INVALID_PLAYBACK_SESSION` | 播放身份异常，请重试 | 重新生成 session 后重试一次；失败则错误页 |
| `DRAMA_NOT_FOUND` | 内容不存在 | 错误页 + 返回按钮 |
| `EPISODE_NOT_FOUND` | 当前剧集不存在 | 回退默认集；若无默认集则无资源页 |
| `EPISODE_NOT_PLAYABLE` | 当前剧集暂无资源 | 轻提示 / 在面板中置灰；若是目标集则回退默认集 |
| `INTERNAL_ERROR` | 加载失败，请重试 | 错误页 + 重试按钮 |
| `NETWORK_ERROR` | 网络异常，请检查后重试 | 错误页 + 重试按钮 |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| App 进入后台 | `scenePhase == .background` | 若当前有有效 episode，则 best-effort `stopPlayback`，并将 UI 置为 `.paused` | 🔴 |
| App 回到前台 | `scenePhase == .active` | 不重新 bootstrap；若播放器实例仍有效则停留当前态 | 🟡 |
| 用户快速连点切集 | 连续点击多个 episode | 取消前一切集 task，只保留最后一次选择 | 🔴 |
| 页面关闭时请求未完成 | 返回首页 / dismiss | cancel 当前网络 task；stop 上报走单独 best-effort task | 🔴 |
| 恢复集失效 | progress 命中的 episode 不在列表里 | 回退第一条可播放 episode | 🔴 |
| 所有集无资源 | 列表存在但 `video_url` 全空 | `uiState = .noResource` | 🔴 |
| 倍速选择时播放器未就绪 | 尚未创建 `AVPlayer` | 禁止点击或忽略选择 | 🟡 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `PlayerView` | `PlayerLoadingView` | 播放器页面 | `PlayerNoResourceView` | `PlayerErrorView` | 内容不存在 + 返回 |
| `EpisodePickerSheet` | 骨架可省略 | 列表 + 当前集高亮 | 空列表文案 | 不单独展示 | 不单独展示 |
| `SpeedPickerDialog` | 不展示 | 正常展示 | 不适用 | 不适用 | 不适用 |

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | `PlayerViewModel` 状态机与业务逻辑 | 核心主路径全覆盖 | Swift Testing |
| 组件测试 | 关键 UI 状态渲染 | 关键态覆盖 | SwiftUI Preview + Swift Testing |
| 导航测试 | router / deeplink 到 player 入口语义 | 关键路径覆盖 | Swift Testing |

### 12.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| I-01 | 无历史默认进入第一可播集 | progress `has_history=false`，episodes 有可播集 | `loadIfNeeded()` | `currentEpisode == firstPlayable`，触发 `startPlayback(progress=0)` | 单元 |
| I-02 | 有历史恢复到指定集 | progress 返回 `episode_id + start_time` | `loadIfNeeded()` | 选择恢复集并记录 `resumeProgress` | 单元 |
| I-03 | 恢复集失效回退默认集 | progress 命中已删除 episode | `loadIfNeeded()` | 回退第一可播集 | 单元 |
| I-04 | 全部集无资源进入 noResource | episodes 全无 `video_url` | `loadIfNeeded()` | `uiState == .noResource` | 单元 |
| I-05 | 剧集列表为空进入 noResource | drama 存在但 `episodes=[]` | `loadIfNeeded()` | `uiState == .noResource` | 单元 |
| I-06 | 切集先 stop 再 start | 当前已有 episode | `switchEpisode()` | 先调用 stop，再调用新 episode 的 start(progress=0) | 单元 |
| I-07 | 退出页面 best-effort stop | 当前在播放中 | `handleBack()` | 触发 stop，但无论成功失败都 dismiss | 单元 |
| I-08 | 倍速切换沿用到切集后 | 当前倍速 1.5x | 切到下一集 | 新 player 仍应用 1.5x | 单元 |
| I-09 | 播放页隐藏 Tab Bar | router 导航到 `.player` | 渲染页面 | Tab Bar 隐藏 | 组件 / 导航 |
| I-10 | 原生播放控件可承载暂停与进度显示 | 播放器已 ready | 渲染 `NativeVideoPlayerView` | 页面不额外依赖自定义控制条即可满足基础播放 / 暂停 / 进度显示 | 组件 / 单元 |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| `PlayerRepositoryProtocol` | Spy / Fake | 精确断言 progress/start/stop 调用顺序 |
| `PlaybackSessionStore` | In-memory fake | 覆盖首次生成与已存在 sessionId |
| `NavigationRouter` | 测试实例 | 断言返回 / dismiss 行为 |
| Native player adapter | Stub | 不依赖真实视频播放内核做单元测试 |

---

## 13. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无新增第三方依赖 | — | 播放器能力使用系统 `AVPlayer` / `VideoPlayer` 与 Keychain | 满足首版真实播放与安全存储需求，且无需额外用户批准 |

---

## 14. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| `APIClient` 当前不支持 headers | progress/start/stop 无法透传 session | 🔴 | 高 | 扩展 `APIEndpoint.headers` 并统一在 `APIClient` 写入 | 若修改困难，可局部自建 request builder |
| `APIClient` 当前错误解析结构与 backend 不一致 | 服务端错误 message 丢失 | 🟡 | 高 | 支持解析 `{ error: { code, message } }` | 至少保底显示状态码消息 |
| 播放器覆盖层与安全区域冲突 | UI 遮挡 | 🟡 | 中 | 视频全屏，覆盖层按 safe area inset 布局 | 缩减顶部栏高度 |
| 后台切换导致 stop 重复上报 | 重复网络请求 | 🟡 | 中 | 使用 `hasPendingStopReport` 与生命周期去重 | 允许幂等，服务端覆盖写入 |
| 切集时旧任务未取消 | 状态错乱 | 🔴 | 中 | 保存 `requestTask`，切集时先 cancel | 回退到页面级 loading 重建 |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/architecture/overview.md` | iOS 架构 / 技术栈 | iOS 使用 SwiftUI + TabView + NavigationStack |
| `wiki/features/app-shell/index.md` | iOS 端 / 状态管理 | 播放页挂在 home Tab 的 `NavigationStack` 内 |
| `wiki/features/video-player/index.md` | iOS 端 / 状态管理 | 当前播放页只是 `videoId` 占位页 |
| `wiki/features/data-models/index.md` | Episode / Player 请求模型 | Episode 是播放器基础实体 |
| `PRODUCT.md` | 页面承载策略 | 除 mall/earn 外，业务页按 Native 实现 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `docs/specs/2026-07-26-prd-03-full-player/design.md` | shared 层收口了 bootstrap、header、状态机与沉浸式导航要求 |
| `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` | 当前仅展示 `Video ID` 文本 |
| `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | 当前仅持有 `videoId` 参数 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 当前 `.player(videoId:)` push 到 home Tab path |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 对外播放器 public route name 为 `play` |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 已有独立 Tab path 与 dismiss 能力 |
| `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` | 当前 MVVM + 状态机写法可复用 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 当前 DataSource + Endpoint 模式可复用 |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 当前 Repository 模式可复用 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 当前只支持 query/body，不支持 headers，且错误解析能力不足 |
| `ios/ShortDrama/Sources/Core/Network/APIEndpoint.swift` | 需扩展 headers 默认实现 |
| `ios/ShortDrama/Sources/Core/Network/APIError.swift` | 当前错误模型可继续扩展使用 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 已有导航测试结构可扩展 |
