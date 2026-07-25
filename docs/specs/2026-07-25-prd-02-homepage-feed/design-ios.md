# iOS 端技术方案：PRD-02 首页信息流

> 创建日期：2026-07-25
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

iOS 端在 PRD-01 已完成的 `TabView + NavigationStack + NavigationRouter` 应用壳之上，将首页从占位信息页演进为**Native Feed 列表首页**。实现仍遵循现有 MVVM + Clean Architecture：View 负责渲染状态，ViewModel 负责首屏状态流转，Data 层负责从 canonical contract 拉取数据并映射到 Domain。

```text
HomeView
  -> observes HomeViewModel
     -> calls FetchDramasUseCase(page: 1, pageSize: 10)
        -> DramaRepository.fetchDramas
           -> DramaRemoteDataSource.fetchDramas
              -> APIClient.request(DramaEndpoints.GetDramas)
                 -> GET /api/dramas?page=1&pageSize=10
  -> renders Loading / Success / Empty / Error
  -> on tap -> NavigationRouter.navigate(to: .player / .dramaDetail)
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 从应用名 + 示例按钮演进为首页 Feed 列表、空态、错误态和重试入口 |
| `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` | 修改 | 从仅维护 `isLoading` / `errorMessage` 扩展为完整首页 Feed 状态模型 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 从 `/api/v1/dramas` + 包裹响应迁移到 `/api/dramas` + `{ data, pagination }` |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 轻微修改 | 继续承担 DTO -> Entity 映射，适配新的 DataSource 返回结构 |
| `ios/ShortDrama/Sources/Data/DTOs/DramaDTO.swift` | 轻微修改 | 依赖 `convertFromSnakeCase` 兼容 `cover_url`、`episode_count`、`created_at` 等字段 |
| `ios/ShortDrama/Sources/Data/DTOs/PaginationDTO.swift` | 不变 / 可轻微调整 | 继续依赖 `convertFromSnakeCase` 解码 pagination |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 不变 | 继续承担播放页 / 详情页跳转，不新增导航基础设施 |
| `ios/ShortDrama/Tests/ViewModelTests/HomeViewModelTests.swift` | 修改 | 从当前 loading/error 基础测试扩展到成功、有数据、空态、重试等首页状态测试 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 重构为 Feed 列表首页，补齐 loading / empty / error / retry / card actions |
| `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` | 修改 | 新增首页列表数据、空态判断、重试动作和统一 UI 状态输出 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 改为请求 `/api/dramas?page&pageSize`，解码 `{ data, pagination }` |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 修改 | 保持 UseCase 入参与 Domain 出参不变，承接新的远端响应结构 |
| `ios/ShortDrama/Sources/Data/DTOs/DramaDTO.swift` | 轻微修改 | 明确 snake_case 兼容依赖和字段默认值策略 |
| `ios/ShortDrama/Tests/ViewModelTests/HomeViewModelTests.swift` | 修改 | 增加 success with items、empty、retry、error 文案断言 |
| `ios/ShortDrama/Tests/DataTests/...` | 按需新增 | 如当前缺少 DataSource / Repository 解码测试，可补 DTO 与 endpoint 测试 |

---

## 3. View 层设计

### 3.1 组件层级树

```text
HomeView
├── HomeFeedContainer
│   ├── LoadingView
│   ├── ErrorStateView
│   │   └── RetryButton
│   ├── EmptyStateView
│   └── ScrollView
│       └── LazyVStack
│           └── HomeDramaCardView (ForEach)
│               ├── DramaCoverView
│               ├── DramaTextContent
│               │   ├── Title
│               │   ├── Description
│               │   └── MetaRow(category / tags / rating / episodeCount)
│               └── ActionRow
│                   ├── PlayButton
│                   └── DetailButton
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| `HomeView` | View | 首页根视图，根据 ViewModel 状态决定渲染 loading / list / empty / error | 否 |
| `HomeDramaCardView` | View | 渲染单条首页卡片，包含封面、标题、说明和动作区 | 是 |
| `HomeFeedEmptyView` | View | 渲染空态文案和轻量占位说明 | 是 |
| `HomeFeedErrorView` | View | 渲染错误文案和重试按钮 | 是 |
| `DramaCoverView` | View | 封面图或缺失封面占位块 | 是 |

### 3.3 组件接口定义

```swift
struct HomeView: View {
    @EnvironmentObject var router: NavigationRouter
    @StateObject private var viewModel: HomeViewModel

    var body: some View {
        Group {
            switch viewModel.viewState {
            case .loading:
                ProgressView()
            case .empty:
                HomeFeedEmptyView()
            case .error(let message):
                HomeFeedErrorView(message: message) {
                    await viewModel.retry()
                }
            case .content(let items):
                ScrollView {
                    LazyVStack(spacing: DesignTokens.Spacing.md) {
                        ForEach(items) { item in
                            HomeDramaCardView(
                                drama: item,
                                onPlay: { router.navigate(to: .player(videoId: item.id)) },
                                onDetail: { router.navigate(to: .dramaDetail(dramaId: item.id)) }
                            )
                        }
                    }
                }
            }
        }
        .task {
            await viewModel.loadIfNeeded()
        }
    }
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| `HomeViewModel` → `HomeView` | `@Published` / `@StateObject` | 首页列表状态渲染 |
| `HomeView` → `HomeDramaCardView` | 构造参数 + closure | 单条卡片展示和动作回调 |
| `HomeView` → `NavigationRouter` | `@EnvironmentObject` | 打开 `play/:id` 与 `detail/:id` |
| Data 层 → Domain 层 | DTO `toEntity()` | 解耦 API 字段命名和 iOS 业务实体 |

### 3.5 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | 使用 `ScrollView + LazyVStack` 单列自适应布局 | 首版不做 iPad 双栏 |
| Dynamic Type | 复用系统字体语义和 `DesignTokens` 间距 | 标题、描述允许多行截断 |
| 深色模式 | 复用系统语义色与现有设计 token | 封面占位块在深浅模式都可读 |
| 安全区域 | 由 `NavigationStack` + `TabView` 默认处理 | 不额外侵入底部安全区 |
| 图片缺失 | 使用占位矩形 / icon | 避免空图导致布局塌陷 |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 View | 职责 |
|-----------|----------|------|
| `HomeViewModel` | `HomeView` | 管理首页首屏请求、状态切换、错误信息与重试 |

### 4.2 状态定义

```swift
@MainActor
final class HomeViewModel: ObservableObject {
    enum ViewState: Equatable {
        case loading
        case content([Drama])
        case empty
        case error(String)
    }

    @Published private(set) var viewState: ViewState = .loading
    @Published private(set) var isRetrying = false

    private let fetchDramasUseCase: FetchDramasUseCase
    private var hasLoaded = false

    func loadIfNeeded() async { ... }
    func loadDramas() async { ... }
    func retry() async { ... }
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| `viewState` | `ViewState` | `.loading` | 首页统一视图状态 |
| `isRetrying` | `Bool` | `false` | 控制错误态重试中的按钮与重复点击 |
| `hasLoaded` | `Bool` | `false` | 避免 View 反复出现时重复首屏请求 |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | View 层表现 |
|---------|---------|-----------|
| Loading | 首次进入 / 主动重试中 | `ProgressView` 或骨架占位 |
| Success (有数据) | 拉取成功且 `items.count > 0` | 列表卡片 |
| Empty | 拉取成功且 `items.isEmpty` | 空态文案 |
| Error | 抛出 `APIError` 或未知错误 | 错误文案 + 重试按钮 |

### 4.5 行为约束

- 首次进入首页自动请求 `page = 1, pageSize = 10`。
- 本期只拉取第一页，不实现滚动加载下一页。
- `retry()` 复用 `loadDramas()`，但需防止重复并发点击。
- 路由动作不由 ViewModel 管理，继续交给 `NavigationRouter` 处理。

---

## 5. Navigation 路由设计

### 5.1 导航方案

继续复用 PRD-01 的 `NavigationRouter + TabView + NavigationStack`。

### 5.2 路由清单

| 路由标识 | 目标页面 | 参数 | 导航方式 | 说明 |
|---------|---------|------|---------|------|
| `.player(videoId:)` | `PlayerView` | `videoId = drama.id` | Push | 首页主动作进入播放页 |
| `.dramaDetail(dramaId:)` | `DramaDetailView` | `dramaId = drama.id` | Push | 首页次动作进入详情页 |

### 5.3 路由管理

- 首页卡片点击只负责发出 closure，实际导航通过 `router.navigate(to:)` 完成。
- 保持首页所属 Tab 不变，返回时恢复首页列表状态。
- 本期不新增 deeplink 解析逻辑，仍沿用 PRD-01 已落地机制。

### 5.4 Deep Link 处理（如适用）

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `djsdrama://play/{id}` | `PlayerView` | `id -> videoId` |
| `djsdrama://drama/{id}` | `DramaDetailView` | `id -> dramaId` |

首页 Feed 本身不新增 deeplink 入口，仅保证点击卡片后的子路由仍与现有 scheme 契约一致。

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | `APIClient` + `URLSession` | 保持现有实现 |
| 请求构建 | `APIEndpoint` | 修改 drama endpoint 路径与 query |
| 响应解析 | `Codable` + `JSONDecoder.convertFromSnakeCase` | 兼容 snake_case 字段 |
| 错误处理 | `APIError` | 保持现有统一错误模型 |

### 6.2 API 端点定义

```swift
enum DramaEndpoints {
    struct GetDramas: APIEndpoint {
        typealias Response = DramaListResponse

        let page: Int
        let pageSize: Int

        var path: String { "/api/dramas" }
        var method: HTTPMethod { .get }
        var queryItems: [URLQueryItem]? {
            [
                URLQueryItem(name: "page", value: String(page)),
                URLQueryItem(name: "pageSize", value: String(pageSize))
            ]
        }
    }
}

struct DramaListResponse: Decodable {
    let data: [DramaDTO]
    let pagination: PaginationDTO
}
```

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 网络超时 | 0 | — | 本期由用户手动点击重试 |
| 5xx 服务端错误 | 0 | — | 本期由首页错误态提供重试按钮 |
| 401 Token 过期 | — | — | 首页当前无需登录，不涉及 |

### 6.4 网络状态监听

- 本期不引入 `NWPathMonitor`。
- 网络异常统一通过 `APIError.network` 映射到错误态。

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| 首页第一页数据 | 不持久化 | — | 会话内有效 | 首版不做缓存 |
| 首页 UI 状态 | SwiftUI 内存状态 | `@StateObject` | 页面生命周期 | 返回首页时沿用当前内存状态 |

### 7.2 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| 首页列表 | 不做磁盘缓存 | — | 页面销毁后释放 |
| 图片 | 交由系统 / 未来方案处理 | — | 本期不设计图片缓存模块 |

### 7.3 数据迁移策略

- 无本地数据库迁移。
- 主要迁移发生在远端协议层：从 `/api/v1/dramas` 包裹响应迁移到 `/api/dramas` 平铺响应。

---

## 8. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | iOS 实现方式 |
|---------|---------------|-------------|
| canonical contract | `/api/dramas?page&pageSize` + `{ data, pagination }` | 修改 `DramaRemoteDataSource` 与 `DramaEndpoints.GetDramas` |
| 首页状态机 | loading / content / empty / error | 在 `HomeViewModel.ViewState` 中显式建模 |
| 首屏第一页范围 | 只请求第一页 | `FetchDramasUseCase.execute(page: 1, pageSize: 10)` |
| 播放入口映射 | `drama.id -> videoId` | `router.navigate(to: .player(videoId: drama.id))` |
| 详情入口映射 | `drama.id -> dramaId` | `router.navigate(to: .dramaDetail(dramaId: drama.id))` |
| 缺封面 / 空标签容错 | 客户端容错渲染 | View 层提供占位封面和可选 meta 行 |

---

## 9. 边界与错误处理

### 9.1 全局错误处理架构

| 层级 | 机制 | 说明 |
|------|------|------|
| DataSource | `APIClient` 抛 `APIError` | 统一网络 / 服务端 / 解码错误 |
| Repository | DTO -> Entity | 不吞错误，直接上抛 |
| ViewModel | `do/catch` | 将错误映射为 `.error(message)` |
| View | 错误态 + 重试按钮 | 给用户可恢复入口 |

### 9.2 错误码 / 错误态映射

| 错误来源 | iOS 表现 | 说明 |
|---------|---------|------|
| `APIError.network` | 错误态 + “加载失败，请重试” | 弱网 / 断网 |
| `APIError.server` | 错误态 + 服务端 message | 保留后端提示 |
| `APIError.decodingFailed` | 错误态 | 防止脏数据进入列表 |
| 空数据 | 空态 | 非错误，单独建模 |

### 9.3 边界场景

| 场景 | 触发条件 | UI 行为 | 说明 |
|------|---------|---------|------|
| 空列表 | 返回 `[]` | 展示空态文案 | 不显示旧示例按钮 |
| 缺封面 | `coverUrl` 为空或无效 | 展示占位封面 | 不崩溃 |
| 长标题 / 长描述 | 文本超长 | 截断或限制行数 | 保持卡片稳定 |
| 重复重试 | 连续点击重试 | 按钮禁用或忽略重复调用 | 避免并发请求 |
| 返回首页 | 从播放 / 详情返回 | 保持首页现有状态 | 不重新回到旧占位页 |

---

## 10. 测试策略

### 10.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| ViewModel 测试 | loading / content / empty / error / retry | Swift Testing |
| Data 层测试 | endpoint 路径、query、响应解码 | Swift Testing + URLProtocol Mock |
| View 预览 / 轻量验证 | 列表、空态、错误态 | SwiftUI Preview（非自动化主验证） |

### 10.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 测试类型 |
|------|---------|------|---------|---------|
| I-01 | 首页加载成功且有数据 | repository 返回 1 条 drama | `viewState = .content([item])` | ViewModel |
| I-02 | 首页加载成功但空列表 | repository 返回 `[]` | `viewState = .empty` | ViewModel |
| I-03 | 首页加载失败 | repository 抛 `APIError.network` | `viewState = .error` | ViewModel |
| I-04 | 首页重试成功 | 首次失败，二次成功 | 状态从 `.error` 变 `.content` 或 `.empty` | ViewModel |
| I-05 | endpoint 收敛 | 调用 GetDramas | path 为 `/api/dramas`，query 为 `page/pageSize` | Data |
| I-06 | 响应解码 | `{ data, pagination }` snake_case | 正确解码为 `DramaDTO[]` | Data |

### 10.3 与现有测试的衔接

- 保留当前 `T-14 ~ T-17` 的测试意图，但需要升级断言对象，从 `isLoading` / `errorMessage` 扩展到统一 `viewState`。
- `MockDramaRepository` 可继续复用，但建议增加“首次失败、后续成功”的行为模式以支持 retry 测试。

---

## 11. 风险与取舍

| 风险 / 取舍 | 说明 | 对应策略 |
|------------|------|---------|
| 不做本地缓存 | 返回首页后若页面重建需重新请求 | 使用 `hasLoaded` 降低重复请求 |
| 不实现第二页 | 无法验证滚动翻页体验 | 明确这是 PRD-02 MVP 边界 |
| DTO 依赖 snake_case 自动映射 | 需确保字段命名与 decoder 策略匹配 | 增补 Data 层解码测试 |
| 首页 UI 从占位页升级为列表 | 现有测试和预览需要同步调整 | 以 ViewModel 状态为中心重构测试 |
