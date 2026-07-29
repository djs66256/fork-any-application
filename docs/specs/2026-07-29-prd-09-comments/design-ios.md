# iOS 端技术方案：PRD-09 评论系统

> 创建日期：2026-07-29
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

iOS 端评论能力继续遵循 `ios/CLAUDE.md` 要求的 **MVVM + Clean Architecture（Core → Domain → Data → Presentation）**。评论不新增独立导航 route，而是作为首页卡片和播放器页面内的 `.sheet` 能力承载；评论 ViewModel 只管理页面内局部状态，不把评论状态塞进 `NavigationRouter`。

本期能力拆成五个子域：

1. 首页卡片评论入口扩展；
2. 播放器评论入口从静态占位改为真实点击；
3. 评论抽屉 `.sheet` UI 与状态机；
4. 评论 API Data/Repository/UseCase 接入；
5. 登录拦截上下文与评论抽屉恢复语义。

```text
HomeView / PlayerView
  -> tap comment entry
     -> present CommentSheet
        -> observe CommentSheetViewModel
           -> FetchDramaCommentsUseCase(query)
              -> CommentRepositoryProtocol.fetchComments(...)
                 -> CommentRemoteDataSource.fetchComments(...)
                    -> APIClient.request(CommentEndpoints.GetComments)
                       -> GET /api/dramas/{id}/comments?page=1&pageSize=20&sort=latest
        -> submit comment
           -> isUserLoggedIn ? CreateCommentUseCase : emit requireLogin(CommentLoginContext)
        -> toggle like
           -> isUserLoggedIn ? ToggleCommentLikeUseCase : emit requireLogin(CommentLoginContext)
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 不变 | 评论不新增独立 route，仍复用 `.player(videoId:)` / `.home` 等页面上下文 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 不变 | 评论抽屉为页面内 `.sheet`，不进入 router path |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 承接首页评论抽屉展示、关闭与登录恢复上下文 |
| `ios/ShortDrama/Sources/Features/Home/Views/Components/HomeDramaCardView.swift` | 修改 | 在现有 action row 中新增“评论”按钮 |
| `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` | 修改 | 新增评论 `.sheet`，承接播放器评论入口 |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerRightActionBar.swift` | 修改 | 将静态“评论”入口改为带 callback 的真实按钮 |
| `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | 修改 | 增加评论抽屉可见性、评论上下文与 require-login effect |
| `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift` | 参考复用 | 复用结构化 `LoginContext` 的设计思路 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 不变 / 复用 | 已支持 snake_case 解码与 nested error message 解析；当前只向上暴露 HTTP status + message，不暴露 nested `error.code` |
| `ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift` | 参考复用 | comments data source 设计风格参考 |
| `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` | 不变 / 轻量扩展 | 首页 Feed 主状态机保持不变，评论抽屉建议使用独立页面级协调状态 |
| `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | 修改 | 评论相关状态与页面内 effect 接入 |

### 1.2 设计原则

1. **评论作为页面内能力**：首页与播放器都通过 `.sheet` 打开评论，不新增 comments page route。
2. **登录恢复上下文结构化**：不照搬 Android `returnRoute` 字符串；iOS 使用结构化 `CommentLoginContext`。
3. **错误与空态局部化**：评论抽屉的 loading/error/empty 不影响首页 Feed 或播放器主内容。
4. **最小侵入式接入**：首页和播放器只扩展现有入口与局部状态，不重构原主页面状态机。
5. **沿用现有网络栈**：继续使用 `APIClient + APIEndpoint + URLSession + APIError`，不新增网络库。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `ios/ShortDrama/Sources/Features/Comments/Views/CommentSheetView.swift` | 新增 | 评论抽屉根视图 |
| `ios/ShortDrama/Sources/Features/Comments/Views/Components/CommentComposerView.swift` | 新增 | 评论输入区 |
| `ios/ShortDrama/Sources/Features/Comments/Views/Components/CommentListView.swift` | 新增 | 评论列表与分页承接 |
| `ios/ShortDrama/Sources/Features/Comments/Views/Components/CommentRowView.swift` | 新增 | 单条评论行 |
| `ios/ShortDrama/Sources/Features/Comments/Views/Components/CommentStateView.swift` | 新增 | loading / empty / error 统一容器 |
| `ios/ShortDrama/Sources/Features/Comments/ViewModels/CommentSheetViewModel.swift` | 新增 | 评论列表、发评论、点赞、分页、排序状态机 |
| `ios/ShortDrama/Sources/Features/Comments/CommentLoginContext.swift` | 新增 | 评论登录恢复上下文 |
| `ios/ShortDrama/Sources/Domain/Entities/Comment.swift` | 新增 | 评论实体 |
| `ios/ShortDrama/Sources/Domain/Entities/CommentQuery.swift` | 新增 | 评论列表 query 与排序实体 |
| `ios/ShortDrama/Sources/Domain/Entities/ToggleCommentLikeResult.swift` | 新增 | 点赞切换结果实体 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/CommentRepositoryProtocol.swift` | 新增 | 评论仓库协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchDramaCommentsUseCase.swift` | 新增 | 获取评论列表用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/CreateCommentUseCase.swift` | 新增 | 发表评论用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/ToggleCommentLikeUseCase.swift` | 新增 | 点赞切换用例 |
| `ios/ShortDrama/Sources/Data/DTOs/CommentDTO.swift` | 新增 | 评论 DTO 与映射 |
| `ios/ShortDrama/Sources/Data/DTOs/CommentListResponseDTO.swift` | 新增 | 列表响应 DTO |
| `ios/ShortDrama/Sources/Data/DTOs/CreateCommentRequestDTO.swift` | 新增 | 发评论请求 DTO |
| `ios/ShortDrama/Sources/Data/DTOs/ToggleCommentLikeResponseDTO.swift` | 新增 | 点赞返回 DTO |
| `ios/ShortDrama/Sources/Data/DataSources/CommentRemoteDataSource.swift` | 新增 | comments API 远端数据源 |
| `ios/ShortDrama/Sources/Data/Repositories/CommentRepository.swift` | 新增 | 评论仓库实现 |
| `ios/ShortDrama/Sources/Features/Home/Views/Components/HomeDramaCardView.swift` | 修改 | 新增首页评论入口 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 承接首页评论 sheet 与登录恢复 |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerRightActionBar.swift` | 修改 | 新增 `onComment` callback |
| `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` | 修改 | 承接播放器评论 sheet |
| `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | 修改 | 增加评论 UI 状态与 route effect |
| `ios/ShortDrama/Tests/DataTests/CommentRemoteDataSourceTests.swift` | 新增 | comments endpoint/query/body 测试 |
| `ios/ShortDrama/Tests/DataTests/CommentRepositoryTests.swift` | 新增 | DTO -> Entity 映射测试 |
| `ios/ShortDrama/Tests/ViewModelTests/CommentSheetViewModelTests.swift` | 新增 | 覆盖评论状态机 |
| `ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift` | 修改 | 覆盖播放器评论入口与 require-login effect |
| `ios/ShortDrama/Tests/ViewModelTests/HomeViewModelTests.swift` | 轻量修改/新增协调测试 | 验证首页评论入口与宿主协调逻辑 |

---

## 3. View 层设计

### 3.1 组件层级树

```text
CommentSheetView
├── CommentSheetHeader
│   ├── Title("评论")
│   ├── CountLabel
│   └── SortSegmentedControl(latest/hot)
├── CommentStateView
│   ├── CommentLoadingView
│   ├── CommentErrorView
│   ├── CommentEmptyView
│   └── CommentListView
│       └── CommentRowView (ForEach)
│           ├── AvatarView
│           ├── UserMeta
│           ├── ContentText
│           └── LikeButton
└── CommentComposerView
    ├── TextEditor / placeholder
    ├── CharCountLabel
    └── SubmitButton
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `CommentSheetView` | View | 评论抽屉根视图 | 否 |
| `CommentStateView` | View | 切换 loading / content / empty / error | 否 |
| `CommentListView` | View | 列表渲染、分页触底、append error 承接 | 否 |
| `CommentRowView` | View | 单条评论展示与点赞交互 | 否 |
| `CommentComposerView` | View | 文本输入、字数提示、发送按钮 | 否 |
| `HomeDramaCardView` | View | 扩展 action row 评论入口 | 修改复用 |
| `PlayerRightActionBar` | View | 评论入口从静态位改为真实按钮 | 修改复用 |

### 3.3 组件接口定义

```swift
struct CommentSheetView: View {
    @StateObject var viewModel: CommentSheetViewModel
    let onClose: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            CommentSheetHeader(
                count: viewModel.totalCount,
                selectedSort: viewModel.selectedSort,
                onSelectSort: { sort in
                    Task { await viewModel.selectSort(sort) }
                },
                onClose: onClose
            )

            CommentStateView(
                state: viewModel.listState,
                comments: viewModel.comments,
                appendState: viewModel.appendState,
                onRetry: { Task { await viewModel.retry() } },
                onLoadMore: { Task { await viewModel.loadMoreIfNeeded() } },
                onToggleLike: { comment in
                    Task { await viewModel.toggleLike(commentID: comment.id) }
                }
            )

            CommentComposerView(
                text: $viewModel.inputText,
                isSubmitting: viewModel.isSubmitting,
                onSubmit: { Task { await viewModel.submitComment() } }
            )
        }
        .presentationDetents([.medium, .large])
        .task { await viewModel.loadIfNeeded() }
    }
}
```

```swift
struct HomeDramaCardView: View {
    let drama: Drama
    let onPlay: () -> Void
    let onDetail: () -> Void
    let onComment: () -> Void
}
```

```swift
struct PlayerRightActionBar: View {
    let liked: Bool
    let favorited: Bool
    let onLike: () -> Void
    let onFavorite: () -> Void
    let onComment: () -> Void
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| 宿主页 -> `CommentSheetView` | 构造参数 | 传入 `dramaId`、来源页 `source`、关闭回调 |
| `CommentSheetViewModel` -> View | `@Published` / `@StateObject` | 评论列表、输入框、分页、错误、排序状态 |
| View -> ViewModel | closure / async action | 切换排序、发送评论、点赞、重试、加载更多 |
| 宿主页 -> App 层 | 页面级 `routeEffect` / alert 状态 | 未登录时提示并保留 `CommentLoginContext` |
| 子组件 -> 宿主页 | `onComment` callback | 首页卡片和播放器入口统一打开 sheet |

### 3.5 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| `.sheet` 高度 | `.presentationDetents([.medium, .large])` | 与现有播放器选集 sheet 风格保持一致 |
| Dynamic Type | 使用语义字体 + 多行文本 | 评论正文允许换行，标题与按钮不硬截断 |
| 深色模式 | 复用 `DesignTokens` / 系统语义色 | 保证抽屉与宿主页风格一致 |
| 安全区域 | 输入框贴底时保留 safe area inset | 避免遮挡 home indicator |
| 长列表 | `ScrollView` / `List` + 触底回调 | 切换排序后滚动回顶 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 View | 职责 |
|-----------|----------|------|
| `CommentSheetViewModel` | `CommentSheetView` | 评论列表加载、分页、排序、发评论、点赞、登录拦截、输入校验 |
| `PlayerViewModel` | `PlayerView` | 播放器评论抽屉展示状态、require-login effect |
| `HomeView` 页面级协调状态 | `HomeView` | 首页评论抽屉展示与关闭协调 |

### 4.2 状态定义

```swift
@MainActor
final class CommentSheetViewModel: ObservableObject {
    enum ListState: Equatable {
        case idle
        case loading
        case content
        case empty
        case error(String)
    }

    enum AppendState: Equatable {
        case idle
        case loading
        case error(String)
        case noMore
    }

    @Published private(set) var listState: ListState = .idle
    @Published private(set) var appendState: AppendState = .idle
    @Published private(set) var comments: [Comment] = []
    @Published private(set) var totalCount: Int = 0
    @Published private(set) var selectedSort: CommentSort = .latest
    @Published var inputText: String = ""
    @Published private(set) var isSubmitting = false
    @Published private(set) var likingCommentIDs: Set<String> = []
    @Published private(set) var routeEffect: CommentRouteEffect?
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `listState` | `ListState` | `.idle` | 首屏列表状态 |
| `appendState` | `AppendState` | `.idle` | 分页尾部状态 |
| `comments` | `[Comment]` | `[]` | 当前评论列表 |
| `totalCount` | `Int` | `0` | 来自 `pagination.total` |
| `selectedSort` | `CommentSort` | `.latest` | 当前排序 |
| `inputText` | `String` | `""` | 输入框内容 |
| `isSubmitting` | `Bool` | `false` | 发评论中 |
| `likingCommentIDs` | `Set<String>` | `[]` | 点赞中评论 ID 集合 |
| `routeEffect` | `CommentRouteEffect?` | `nil` | require-login 等一次性事件 |
| `page` / `hasNextPage` | 私有字段 | `1 / false` | 分页控制 |
| `hasLoaded` / `isRequestInFlight` | 私有字段 | `false` | 防重复请求 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | View 层表现 |
|---------|---------|-----------|
| Loading | `listState == .loading && comments.isEmpty` | 全区 `ProgressView` |
| Success | `listState == .content` | 评论列表 |
| Empty | `listState == .empty` | 空态插图/文案 |
| Error | `listState == .error` 且 `comments.isEmpty` | 错误态 + 重试 |
| Append Loading | `appendState == .loading` | 列表尾部 loading |
| Append Error | `appendState == .error` | 列表尾部重试提示 |

### 4.5 登录恢复上下文

```swift
struct CommentLoginContext: Equatable, Sendable {
    enum Source: String, Sendable {
        case home
        case player
    }

    let source: Source
    let dramaID: String
    let action: PendingCommentAction
}

struct PendingCommentAction: Equatable, Sendable {
    enum Kind: String, Sendable {
        case openSheet
        case createComment
        case toggleLike
    }

    let kind: Kind
    let commentID: String?
}
```

说明：
- `openSheet` 用于仅恢复评论抽屉。
- `createComment` / `toggleLike` 只用于恢复上下文，不自动重放写操作。
- 宿主页在未来登录成功后只需根据 context 重新打开对应 `dramaID` 的评论抽屉。

---

## 5. Navigation 路由设计

### 5.1 导航方案

- 首页评论：仍停留在 `HomeView`，通过 `@State` 驱动 `.sheet`。
- 播放器评论：仍停留在 `PlayerView`，通过 `@Published` / `@State` 驱动 `.sheet`。
- 登录拦截：首版继续沿用现有 alert / routeEffect 语义，不在本期实现真实登录页 push。

### 5.2 路由清单

| 路由标识 | 目标页面 | 参数 | 导航方式 | 说明 |
|---------|---------|------|---------|------|
| `home comments sheet` | `CommentSheetView` | `dramaId` | `.sheet` | 首页页面内评论抽屉 |
| `player comments sheet` | `CommentSheetView` | `dramaId` | `.sheet` | 播放器页面内评论抽屉 |
| `requireLogin(CommentLoginContext)` | 当前宿主页 alert / future login flow | `source, dramaID, action` | `routeEffect` / `.alert` | 登录恢复上下文 |

### 5.3 路由管理

```swift
struct PlayerView: View {
    @StateObject private var viewModel: PlayerViewModel

    var body: some View {
        content
            .sheet(isPresented: $viewModel.isCommentSheetPresented) {
                CommentSheetView(
                    viewModel: viewModel.makeCommentSheetViewModel()
                )
            }
    }
}
```

### 5.4 Deep Link 处理

- 本期不新增 comments deeplink。
- 评论仅作为页面内局部能力恢复，不通过 URL Scheme 直接打开。

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | `APIClient` | 继续复用统一 URLSession 客户端 |
| 请求构建 | `CommentEndpoints` | 为 comments API 新增 endpoint 枚举/类型 |
| 响应解析 | `Codable + JSONDecoder(.convertFromSnakeCase)` | 兼容 backend snake_case 字段 |
| 错误处理 | `APIError` | 当前可稳定拿到 HTTP status 与 message；如需消费 nested `error.code`，需后续扩展网络核心层 |

### 6.2 API 端点定义

```swift
enum CommentEndpoints {
    struct GetComments: APIEndpoint {
        typealias Response = CommentListResponseDTO
        let dramaID: String
        let page: Int
        let pageSize: Int
        let sort: String
    }

    struct CreateComment: APIEndpoint {
        typealias Response = CommentDTO
        let dramaID: String
        let content: String
    }

    struct ToggleLike: APIEndpoint {
        typealias Response = ToggleCommentLikeResponseDTO
        let dramaID: String
        let commentID: String
    }
}
```

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 网络超时 | 0 | — | 评论交互优先保持明确错误反馈，不做隐式自动重试 |
| 5xx 服务端错误 | 0 | — | 由用户手动点击重试 |
| 401 未登录 | 0 | — | 转成 require-login effect |

### 6.4 网络状态监听

- 本期不新增专属网络监听。
- 继续由页面级重试按钮承接临时失败恢复。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| 评论列表 | 不持久化 | 内存状态 | 关闭页面释放 | 首版不做本地缓存 |
| 评论输入框草稿 | 不持久化 | 内存状态 | 关闭抽屉释放 | 避免跨页面残留 |
| 评论登录恢复上下文 | 页面级内存状态 | 宿主页 `@State` / ViewModel | 登录完成或取消后清空 | 不落 UserDefaults / Keychain |

### 7.2 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| 当前抽屉评论列表 | 内存缓存 | 仅页面存活期间 | 关闭抽屉或切换 drama 后清空 |
| 分页状态 | 内存缓存 | 同上 | 切换排序后重置 |

### 7.3 数据迁移策略

- 本期不新增 CoreData / UserDefaults schema，因此无迁移需求。

---

## 8. 配置与环境

| 配置项 | 管理方式 | 开发环境值 | 生产环境值 | 说明 |
|--------|---------|----------|-----------|------|
| API Base URL | `xcconfig -> Info.plist -> AppConfig.apiBaseURL` | 现有配置 | 现有配置 | comments API 继续复用同一 base URL |
| 登录态判断 | 现有页面级逻辑 / future auth module | 当前为占位/最小语义 | 后续接真实 auth | 评论设计不新增硬编码 token |

> ⚠️ 禁止硬编码任何常量。继续通过 `AppConfig` 获取环境配置。

---

## 9. API 调用清单

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| `GET /api/dramas/{id}/comments` | 打开评论抽屉、切换排序、重试、加载更多 | `dramaID + page + pageSize + sort` | 更新评论列表与 `totalCount` | 局部 error / append error |
| `POST /api/dramas/{id}/comments` | 点击发送评论 | `inputText.trim()` | 顶部插入新评论、清空输入框、`totalCount + 1` | 内联错误 / require-login |
| `POST /api/dramas/{id}/comments/{commentId}/like` | 点击评论点赞 | `dramaID + commentID` | 局部更新 `liked/likeCount` | require-login / 局部错误提示 |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | iOS 端实现方式 |
|---------|---------------|---------------|
| 评论承载方式 | 页面内 sheet | 首页与播放器均通过 `.sheet` 打开 `CommentSheetView` |
| 抽屉上下文唯一性 | 同时只有一个活动 `dramaId` | 宿主页持有单一 `activeCommentDramaID` |
| 首屏请求参数 | `page=1&pageSize=20&sort=latest` | `CommentSheetViewModel` 默认值 |
| 热评参数兼容 | 支持 `hot` | `CommentSort` 枚举支持 `.hot`，接口完整透传 |
| 评论总数来源 | `pagination.total` | `totalCount` 由 ViewModel 直接使用 |
| 发评论成功处理 | 顶部插入、清空输入、总数+1 | 不整页重刷 |
| 点赞成功处理 | 局部更新目标项 | 只更新单条评论 |
| 匿名可读、登录可写 | 未登录写操作触发 require-login | `CommentRouteEffect.requireLogin(CommentLoginContext)` |
| 登录恢复策略 | 只恢复上下文，不自动重放 | 宿主页保留 context，未来登录成功后只 reopen sheet |
| 错误隔离 | 评论错误不影响宿主页 | error/empty/loading 都限定在 sheet 内 |

---

## 11. 边界与错误处理

### 11.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| 网络层 | `APIClient` -> `APIError` | 当前统一暴露 HTTP status 与 message，不向上暴露 nested `error.code` |
| ViewModel | `do-catch` | 按 HTTP status（401 / 404 / 5xx）与 message 做错误分流 |
| View 层 | `.alert` / 内联错误 / footer 错误 | 轻量反馈，不中断宿主页 |

### 11.2 HTTP 状态与错误类型映射表

| 端侧可见信号 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| 本地输入校验失败 | 评论内容不合法 | 输入框下方内联提示 |
| `APIError.server(code: 401, ...)` | 请先登录后再操作 | alert / routeEffect |
| `APIError.server(code: 404, ...)` | 当前内容不存在或已失效 | 评论抽屉错误态或轻提示，按当前交互上下文选择 |
| `APIError.server(code: 500..., ...)` | 加载失败，请稍后重试 | 错误态 + 重试 |
| `APIError.network(...)` | 网络异常，请检查后重试 | 错误态 / footer 重试 |
| `APIError.decodingFailed` / `invalidResponse` | 数据异常，请稍后重试 | 错误态 + 重试 |
| 其它未知错误 | 操作失败，请稍后重试 | Toast / inline / 错误态 |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| 首页/播放器切换 drama | 用户在不同 drama 间打开评论 | 重置列表、输入和分页状态 | 🔴 |
| 连续快速点发送 | 连点提交按钮 | `isSubmitting` 禁用按钮 | 🔴 |
| 连续快速点点赞 | 多次点击同一评论 | `likingCommentIDs` 单项加锁 | 🔴 |
| 切换排序时已有列表 | `latest <-> hot` | 回顶并整页重载第一页 | 🟡 |
| 抽屉关闭后重新打开 | 同一 `dramaID` 再次进入 | 可重新请求，首版不保证跨次缓存 | 🟡 |
| 登录拦截后用户取消 | 仅关闭 alert | 保留或清空 context 由宿主页决定，默认清空写动作上下文 | 🟡 |
| 评论内容全空白 | `trim().isEmpty` | 本地先拦截，不发请求 | 🔴 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `CommentSheetView` | ✅ | ✅ | ✅ | ✅ | — |
| `CommentComposerView` | — | ✅ | ✅ | ✅ | — |
| `CommentRowView` | 单项点赞中 | ✅ | — | 单项失败轻提示 | — |

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | `CommentSheetViewModel`、`PlayerViewModel` 评论相关逻辑 | 核心状态机全覆盖 | Swift Testing |
| Data 测试 | `CommentRemoteDataSource` / `CommentRepository` | endpoint/query/body/DTO 映射 | Swift Testing |
| ViewModel 协调测试 | 首页/播放器评论入口与 require-login effect | 关键路径覆盖 | Swift Testing |

### 12.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| I1 | 首次打开评论抽屉加载成功 | 正常网络 | `loadIfNeeded()` | `comments` 非空，`totalCount` 正确 | ViewModel |
| I2 | 首次打开评论抽屉无评论 | 接口返回空数组 | `loadIfNeeded()` | `listState == .empty` | ViewModel |
| I3 | 首次打开评论抽屉失败 | 接口 500 | `loadIfNeeded()` | `listState == .error` | ViewModel |
| I4 | 发评论成功 | 已登录 + 合法输入 | `submitComment()` | 顶部插入新评论、输入清空、总数+1 | ViewModel |
| I5 | 发评论空白被本地拦截 | 输入全空白 | `submitComment()` | 不发请求，展示输入错误 | ViewModel |
| I6 | 未登录发评论 | 未登录 | `submitComment()` | 发出 `requireLogin(CommentLoginContext)` | ViewModel |
| I7 | 点赞成功 | 已登录 | `toggleLike(commentID)` | 目标评论 liked 切换、计数更新 | ViewModel |
| I8 | 未登录点赞 | 未登录 | `toggleLike(commentID)` | 发出 `requireLogin`，不改本地 liked | ViewModel |
| I9 | 切换排序 | 已有数据 | `selectSort(.hot)` | 重置列表并重新请求 | ViewModel |
| I10 | 播放器评论按钮可点击 | `PlayerViewModel` 已初始化 | 打开评论 | `isCommentSheetPresented == true` | ViewModel |
| I11 | DTO 映射 | 后端 snake_case 响应 | decode + map | Entity 字段正确 | Data |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| `CommentRepositoryProtocol` | Mock repository | ViewModel 测试 |
| 网络请求 | `URLProtocol` stub | Data 层测试，不发真实请求 |
| 登录态 | 协议注入 / 闭包注入 | 区分已登录/未登录路径 |

---

## 13. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| — | — | 本期不新增开源依赖 | 继续使用 SwiftUI / URLSession / Swift Testing 现有栈 |

---

## 14. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 当前 iOS 没有真实登录页 | require-login 只能停留在 alert / context | 🟡 | 高 | 明确本期只定义恢复上下文语义，不承诺真实登录跳转闭环 | 后续复用 PRD-08 登录能力接入 |
| 评论抽屉状态塞进宿主页导致状态污染 | 首页/播放器主内容复杂度上升 | 🟡 | 中 | 评论列表状态收敛在独立 `CommentSheetViewModel` | 必要时抽出页面级 coordinator |
| 排序/分页并发导致 UI 闪烁 | 评论体验不稳定 | 🟡 | 中 | 使用 `isRequestInFlight` 与 appendState 区分主加载/分页 | 最差回退为仅支持第一页 |
| 后端 `hot` 与 `latest` 首版结果相同 | 用户感知弱 | 🟢 | 高 | UI 仍完整支持排序选项，文档明确首版范围 | 后续仅补后端算法 |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/features/video-player/index.md` | 评论入口现状 | 播放器评论入口是静态占位 |
| `wiki/features/homepage-feed/index.md` | 首页卡片动作区 | 当前只有观看 / 详情 |
| `wiki/features/comments/index.md` | 功能现状 | 当前没有评论抽屉与 API 对接 |
| `wiki/features/auth/index.md` | 登录态现状 | 移动端真实 auth/session 模块仍未完整落地 |
| `wiki/features/ranking/index.md` | 登录恢复上下文 | iOS 已有结构化 `RankingLoginContext` 参考 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 已支持 snake_case 解码，并能从 nested error envelope 中提取 message；当前不会把 `error.code` 暴露给上层 |
| `ios/ShortDrama/Sources/Core/Network/APIError.swift` | 可直接承接 401/404/500 等 server error |
| `ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift` | 远端数据源写法参考 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 评论不应新增 route |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | router 更适合页面级导航，不适合 comments sheet |
| `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` | 页面状态机建模参考 |
| `ios/ShortDrama/Sources/Features/Home/Views/Components/HomeDramaCardView.swift` | 首页卡片 action row 当前只有观看/详情 |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerRightActionBar.swift` | 评论入口当前为 `staticButton` |
| `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` | 已有 `.sheet` / `.confirmationDialog` 承载模式 |
| `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | 当前无评论状态，需增量扩展 |
| `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift` | `RankingLoginContext` 说明 iOS 更适合结构化上下文 |
| `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` | 现有 require-login effect 模式参考 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift` | 当前登录拦截只到 alert 层 |
