# iOS 端技术方案：PRD-10 签到与消息系统

> 创建日期：2026-07-29
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

```text
SwiftUI View
  → ViewModel (@MainActor ObservableObject)
  → UseCase / RepositoryProtocol
  → Repository
  → RemoteDataSource + APIEndpoint
  → APIClient (URLSession)
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `Features/Home` | 扩展 | 首页新增签到浮层宿主与冷启动资格检查 |
| `Features/MenuPanel` | 扩展 | 菜单消息预览从静态文案改为真实远程数据 |
| `App/NavigationRouter.swift` | 扩展 | 新增真实消息中心 route，继续沿用 close-menu-then-navigate |
| `App/AppRoute.swift` | 扩展 | 用新消息页 route 替换 `.menuPlaceholder(kind: .messages)` |
| `Core/Storage/PlaybackSessionStore.swift` | 参考 | 复用 Keychain 生成安装级 UUID 的模式，实现 installationId store |
| `Data/DataSources/PlayerRemoteDataSource.swift` | 参考 | 复用带自定义 header 的 `APIEndpoint` 声明方式 |
| `Data/Repositories/MenuPanelRepository.swift` | 扩展 | 菜单预览状态改为调用统一 `MessageRepository` 的 preview 能力，不再继续复用 recently viewed 文案 |
| `Features/Auth` / `AuthStore` | 复用 | 互动消息登录门槛与登录后回流复用既有登录闭环 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增 `messages` route，替换原 `.menuPlaceholder(kind: .messages)` |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 支持消息中心页导航与登录后留在消息页 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 首页增加签到浮层宿主 |
| `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` | 修改 | 注入签到状态查询与签到提交逻辑，管理冷启动触发与浮层状态 |
| `ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift` | 修改 | 增加消息预览状态、错误降级与复用缓存 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/Components/MenuMessagePreviewView.swift` | 修改 | 由静态文案改为绑定 preview state |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift` | 修改 | 消息入口跳转到真实消息中心页 |
| `ios/ShortDrama/Sources/Features/Messages/Views/MessageCenterView.swift` | 新增 | 消息中心页 |
| `ios/ShortDrama/Sources/Features/Messages/ViewModels/MessageCenterViewModel.swift` | 新增 | 管理系统消息、互动消息、登录门槛与分页状态 |
| `ios/ShortDrama/Sources/Features/Home/Views/CheckInPopupView.swift` | 新增 | 签到浮层 UI |
| `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` | 修改 | 统一承载签到浮层状态与提交逻辑，不新增独立 `CheckInPopupViewModel` |
| `ios/ShortDrama/Sources/Core/Storage/InstallationIdStore.swift` | 新增 | Keychain 安装级 UUID 存储 |
| `ios/ShortDrama/Sources/Data/DataSources/CheckInRemoteDataSource.swift` | 新增 | 封装签到接口 |
| `ios/ShortDrama/Sources/Data/DataSources/MessageRemoteDataSource.swift` | 新增 | 封装 preview/system/interactions 接口 |
| `ios/ShortDrama/Sources/Data/Repositories/CheckInRepository.swift` | 新增 | 实现签到仓储 |
| `ios/ShortDrama/Sources/Data/Repositories/MessageRepository.swift` | 新增 | 实现消息仓储 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/CheckInRepositoryProtocol.swift` | 新增 | 定义签到协议 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/MessageRepositoryProtocol.swift` | 新增 | 定义消息协议 |
| `ios/ShortDrama/Sources/Domain/Entities/SignInStatus.swift` | 新增 | 签到领域实体 |
| `ios/ShortDrama/Sources/Domain/Entities/MessagePreview.swift` | 新增 | 菜单预览领域实体 |
| `ios/ShortDrama/Sources/Domain/Entities/SystemMessage.swift` | 新增 | 系统消息领域实体 |
| `ios/ShortDrama/Sources/Domain/Entities/InteractionMessage.swift` | 新增 | 互动消息领域实体 |
| `ios/ShortDrama/Sources/Domain/UseCases/*CheckIn*.swift` | 新增 | 查询状态与提交签到 use case |
| `ios/ShortDrama/Sources/Domain/UseCases/*Message*.swift` | 新增 | 消息预览与列表 use case |
| `ios/ShortDrama/Tests/DataTests/*` | 新增 | DTO / endpoint / repository 测试 |
| `ios/ShortDrama/Tests/ViewModelTests/*` | 新增 | Home / MenuPanel / MessageCenter ViewModel 测试 |

---

## 3. View 层设计

### 3.1 组件层级树

```text
HomeView
├── HomeFeedListView
├── CheckInPopupPresenter
│   └── CheckInPopupView
│       ├── PopupHeader
│       ├── CheckInDayGrid
│       ├── RewardCopy
│       └── PrimaryActionButton
└── CommentSheetView (existing)

MessageCenterView
├── MessageCenterHeader
├── SystemMessageSection
│   ├── SystemMessageList
│   ├── EmptyStateView
│   └── RetryView
└── InteractionMessageSection
    ├── LoginGateView (anonymous)
    ├── InteractionMessageList (authenticated)
    ├── EmptyStateView
    └── RetryView

MenuPanelView
└── MenuMessagePreviewView
    ├── Title
    ├── Summary
    ├── RelativeTime
    └── Chevron
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `CheckInPopupView` | View | 渲染签到浮层、7 日签到板、关闭与签到按钮 | 否 |
| `CheckInDayCellView` | View | 单个签到日卡片 | 否 |
| `MessageCenterView` | View | 承载系统消息区和互动消息区 | 否 |
| `SystemMessageRowView` | View | 系统消息项 | 否 |
| `InteractionMessageRowView` | View | 互动消息项 | 否 |
| `InteractionLoginGateView` | View | 匿名用户登录门槛 | 否 |
| `MenuMessagePreviewView` | View | 菜单消息预览 | 是（保留原组件名，替换实现） |

### 3.3 组件接口定义

```swift
struct CheckInPopupView: View {
    let state: CheckInPopupState
    let onClose: () -> Void
    let onSubmit: () -> Void

    var body: some View { ... }
}

struct MessageCenterView: View {
    @StateObject private var viewModel: MessageCenterViewModel
    let onBack: () -> Void

    var body: some View { ... }
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| 父 → 子 | 构造函数参数 | `HomeView` 把签到弹层状态传给 `CheckInPopupView` |
| 子 → 父 | Closure Callback | 浮层关闭、提交签到、列表重试 |
| 跨层级共享 | `@EnvironmentObject` | `NavigationRouter`、`AuthStore` |
| View ↔ ViewModel | `@StateObject` / `@ObservedObject` | `MessageCenterView`、`MenuPanelContainerView` |

### 3.5 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | `GeometryReader` + 自适应宽度 | 签到浮层最大宽度受限，iPad 与大屏保持居中卡片 |
| Dynamic Type | `@ScaledMetric` + 文本允许换行 | 签到板标题与消息摘要避免截断关键信息 |
| 深色模式 | 复用 `DesignTokens` / semantic colors | 避免硬编码颜色 |
| 安全区域 | 浮层使用遮罩 + 居中卡片，不侵入导航栏 | 首页与消息页保持既有 safe area 行为 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 View | 职责 |
|-----------|----------|------|
| `HomeViewModel` | `HomeView` | 新增冷启动签到资格检查、浮层展示与关闭态同步 |
| `MenuPanelViewModel` | `MenuPanelContainerView` | 新增消息预览状态管理 |
| `MessageCenterViewModel` | `MessageCenterView` | 管理系统消息分页、互动消息列表、登录门槛与重试 |

### 4.2 状态定义

```swift
@MainActor
final class MessageCenterViewModel: ObservableObject {
    enum SectionState<T: Equatable>: Equatable {
        case idle
        case loading
        case content([T], pagination: Pagination?)
        case empty
        case error(String)
    }

    @Published private(set) var systemMessages: SectionState<SystemMessage> = .idle
    @Published private(set) var interactionMessages: SectionState<InteractionMessage> = .idle
    @Published private(set) var isAuthenticated = false
    @Published private(set) var isPresentingLoginGate = false

    func loadInitial() async { ... }
    func retrySystemMessages() async { ... }
    func retryInteractionMessages() async { ... }
    func handleLoginSuccess() async { ... }
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `HomeViewModel.checkInPopupState` | `CheckInPopupState?` | `nil` | 当前是否展示签到浮层 |
| `HomeViewModel.lastCheckInServerDate` | `String?` | `nil` | 用于按服务端业务日记录关闭态和避免重复查询 |
| `MenuPanelViewModel.messagePreviewState` | `MessagePreviewState` | `.idle` | 菜单消息预览 |
| `MessageCenterViewModel.systemMessages` | `SectionState<SystemMessage>` | `.idle` | 系统消息列表状态 |
| `MessageCenterViewModel.interactionMessages` | `SectionState<InteractionMessage>` | `.idle` | 互动消息列表状态 |
| `MessageCenterViewModel.isAuthenticated` | `Bool` | 从 `AuthStore` 派生 | 决定是否显示登录门槛 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | View 层表现 |
|---------|---------|-----------|
| 签到弹层不展示 | `checkInPopupState == nil` | 首页保持现状 |
| 签到弹层展示中 | `checkInPopupState != nil` | 首页上方叠加遮罩和卡片 |
| 消息 preview loading | `messagePreviewState == .loading` | 菜单展示骨架或占位文案 |
| 消息 preview success | `.content` | 展示标题、摘要、时间 |
| 消息 preview empty | `.empty` | 展示“暂无消息” |
| 消息 preview error | `.error` | 展示静态降级文案，但入口可点 |
| 消息页系统消息 loading | `systemMessages == .loading` | 首屏 skeleton / ProgressView |
| 消息页互动门槛 | `!isAuthenticated` | 展示登录按钮，不请求互动列表 |
| 消息页互动列表 error | `interactionMessages == .error` | 局部错误态，不影响系统消息区 |

---

## 5. Navigation 路由设计

### 5.1 导航方案

- 继续使用 `NavigationStack + NavigationRouter`。
- 消息中心页作为 `AppRoute.messages` 新 route 推入 home tab 导航栈。
- 从菜单点击进入时继续使用 `router.closeMenuPanelThenNavigate(to:)`。
- 登录按钮复用现有 `router.presentLogin(...)` 统一登录承接，并固定使用 `LoginInterceptionContext(source: .messagesEntry, returnRoute: .messages)`；登录成功后保持在当前消息中心页，不回首页，也不恢复菜单面板。

### 5.2 路由清单

| 路由标识 | 目标页面 | 参数 | 导航方式 | 说明 |
|---------|---------|------|---------|------|
| `.messages` | `MessageCenterView` | 无 | Push | 替代 `.menuPlaceholder(kind: .messages)`，作为唯一真实消息页 route |
| `.home` | `HomeView` | 无 | Root | 消息页返回目标 |
| `LoginInterceptionContext(source: .messagesEntry)` | 登录承接 | returnRoute = `.messages` | FullScreenCover | 登录成功后回到当前消息页 |

### 5.3 路由管理

```swift
enum AppRoute: Hashable, Sendable {
    case home
    case messages
    // ... existing routes
}
```

- `owningTab` 仍归属 `.home`；
- `publicRouteName` 明确新增为 `"messages"`，避免继续复用 `menu/placeholder`；
- `NavigationRouter.closeMenuPanelThenNavigate(to: .messages)` 保持“先关菜单再导航”；
- `LoginInterceptionContext.Source` 新增 `.messagesEntry`，只用于消息页互动分区登录承接，不影响现有 `profileEntry` / `rankingBooking` 语义；消息页登录承接唯一使用 `LoginInterceptionContext(source: .messagesEntry, returnRoute: .messages)`。

### 5.4 Deep Link 处理

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `djsdrama://messages` | `MessageCenterView` | 无 |

本期不实现对外 deeplink，也不注册 `djsdrama://messages` 入口；消息中心仅通过应用内菜单导航进入。

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | `APIClient` | 复用既有 URLSession client |
| 请求构建 | `APIEndpoint` protocol | 新增签到 / 消息 endpoint |
| 请求头接线 | `Authorization` 继续复用现有 auth/network 接线方式；`X-Installation-Id` 由 endpoint 定向注入 | 与当前 `APIClient` 真实职责保持一致 |
| 响应解析 | `Codable` + `JSONDecoder` | 签到状态单对象，消息列表 `{ data, pagination }` |
| 错误处理 | `APIError` | 复用既有 network/business/server 错误分类 |

### 6.2 API 端点定义

```swift
private struct GetCheckInStatusEndpoint: APIEndpoint {
    typealias Response = SignInStatusDTO

    let installationId: String?

    var path: String { "/api/check-ins/status" }
    var method: HTTPMethod { .get }
    var headers: [String: String] {
        guard let installationId else { return [:] }
        return ["X-Installation-Id": installationId]
    }
}
```

需要新增的 endpoint：
- `GetCheckInStatusEndpoint`
- `SubmitCheckInEndpoint`
- `GetMessagePreviewEndpoint`
- `GetSystemMessagesEndpoint`
- `GetInteractionMessagesEndpoint`

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 菜单 preview 网络超时 | 1~2 | 短指数退避 | 不阻塞菜单主体验 |
| 首页签到状态失败 | 0 | 不自动重试 | 当前次冷启动可直接降级不弹层 |
| 签到提交失败 | 0 | 手动重试 | 由用户再次点击触发 |
| 互动消息 401 | — | — | 依赖现有 auth 刷新 / 失效处理 |

### 6.4 网络状态监听

- 不新增专门监听器；
- 继续使用现有 `APIClient` 错误分类，由 ViewModel 决定展示文案与重试按钮。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| installationId | Keychain | `checkin.installation.id` | 安装期长期有效 | 复用 `PlaybackSessionStore` 模式 |
| 签到关闭态 | UserDefaults | `checkin.popup.dismissed.<serverDate>` | 随业务日轮换 | 仅本地 UI 态 |
| 消息 preview 最近成功结果 | 内存 | ViewModel 私有缓存 | 当前 App 生命周期 | 用于短时间重复开关菜单 |

### 7.2 CoreData 模型设计

本需求首版不引入 CoreData。

### 7.3 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| 菜单消息 preview | 内存缓存 | 当前会话 | 菜单 ViewModel 重建时失效 |
| 签到关闭态 | UserDefaults 以 `server_date` 为 key | 1 个业务日 | 次日自然失效 |

### 7.4 数据迁移策略

- 新增 Keychain / UserDefaults key，不覆盖既有 auth / player key；
- 安装 ID 与 playback session ID 分离存储，避免语义混淆。

---

## 8. 交互与状态机细化

### 8.1 首页签到浮层触发状态机

```text
App 冷启动进入 Home
  → 请求签到状态
  → should_show_popup = false → 不展示
  → should_show_popup = true
      → 检查本地 dismissed.<server_date>
      → 已关闭 → 不展示
      → 未关闭 → 展示浮层
          → 用户关闭 → 记录 dismissed.<server_date>
          → 用户签到成功 → 更新浮层为今日已签到 + 关闭展示资格
```

### 8.2 消息页双分区状态机

```text
进入 MessageCenter
  → 并行/顺序加载 systemMessages
  → 读取 AuthStore
      → anonymous → 展示 interaction login gate
      → authenticated → 加载 interactionMessages
```

---

## 9. 测试策略

### 9.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| Data 层测试 | DTO 映射、endpoint headers/query、repository contract | Swift Testing + URLProtocolMock |
| ViewModel 测试 | Home / MenuPanel / MessageCenter 状态流转 | Swift Testing |
| Router 测试 | 消息页导航与登录回跳 | Swift Testing |
| 存储测试 | installationId / dismissed server_date 持久化 | Swift Testing |

### 9.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 测试类型 |
|------|---------|------|---------|---------|
| I-01 | 冷启动且 should_show_popup=true | 合法签到状态 + 未关闭 | 首页展示浮层 | ViewModel |
| I-02 | 同一 `server_date` 已关闭 | 本地存在 dismissed key | 不展示浮层 | ViewModel |
| I-03 | 签到成功 | `POST /api/check-ins` 成功 | 浮层更新为今日已签到 | ViewModel |
| I-04 | 菜单 preview 成功 | preview 返回消息摘要 | 菜单显示真实摘要 | ViewModel |
| I-05 | 菜单 preview 失败 | 503 / network | 菜单显示降级文案，入口仍可点 | ViewModel |
| I-06 | 匿名进入消息页 | 无登录态 | 系统消息显示，互动区为登录门槛 | ViewModel |
| I-07 | 登录用户进入消息页 | 有登录态 | 系统消息与互动消息都加载 | ViewModel |
| I-08 | 互动消息失败 | 503 | 系统消息区正常，互动区局部错误 | ViewModel |
| I-09 | 从菜单进入消息页 | 点击 menu messages | 先关闭菜单，再 push 消息页 | Router |
| I-10 | 登录后回到消息页 | 消息页点登录并成功 | 停留消息页，不丢失导航上下文 | Router |

---

## 10. 风险与实现约束

- 不修改 `.xcodeproj/project.pbxproj`，新增文件后通过 `xcodegen generate` 纳入工程；
- 不引入第三方网络库；
- 不为 installationId 新增额外安全存储依赖，直接复用系统 Keychain；
- 消息中心首版不做已读态、未读红点、详情页与下钻路由；
- 若冷启动命中签到资格但首页已有 `CommentSheetView` 或其他登录承接在展示，本次冷启动直接放弃签到浮层，不等待评论关闭后补弹；下一次真正冷启动再重新评估。
