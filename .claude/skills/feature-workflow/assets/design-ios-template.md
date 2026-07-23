# iOS 端技术方案：{{功能名称}}

> 创建日期：{{YYYY-MM-DD}}
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

<!-- 描述 iOS 端架构层级（View ↔ ViewModel ↔ Service/Repository） -->

```
┌─────────────────────────────────────────────────┐
│  View Layer (SwiftUI / UIKit)                   │
│  ├── Views / ViewControllers                    │
│  └── Reusable Components                        │
├─────────────────────────────────────────────────┤
│  ViewModel Layer (@Observable / @Published)     │
│  ├── State Management                           │
│  └── Business Logic                             │
├─────────────────────────────────────────────────┤
│  Service / Repository Layer                     │
│  ├── API Service (URLSession / Alamofire)       │
│  ├── Cache Service                              │
│  └── Persistence Service (CoreData / UserDefaults)│
└─────────────────────────────────────────────────┘
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| | 扩展 / 新增 / 不变 | |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `ios/.../xxx.swift` | 新增 | |
| `ios/.../xxx.swift` | 修改 | |
| `ios/.../xxx.swift` | 删除 | |

---

## 3. View 层设计

### 3.1 组件层级树

```
{{ScreenView}}
├── {{NavigationBar}}
├── {{ContentSection}}
│   ├── {{ListItemView}} (ForEach)
│   │   ├── {{ThumbnailView}}
│   │   └── {{InfoLabel}}
│   └── {{EmptyStateView}}
└── {{BottomBar}}
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| | View / ViewController | | 是 / 否 |

### 3.3 组件接口定义

```swift
// 以 SwiftUI 为例
struct {{ComponentName}}: View {
    // MARK: - Properties
    let param: Type           // 外部传入
    @State private var ...    // 内部状态
    @Environment(...) var ... // 环境值

    // MARK: - Body
    var body: some View { ... }
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| 父 → 子 | 构造函数参数 / `@Binding` | |
| 子 → 父 | Closure Callback / delegate | |
| 跨层级共享 | `@EnvironmentObject` / `@Environment` | |

### 3.5 屏幕适配

<!-- 不同屏幕尺寸、Dynamic Type、深色模式、横竖屏的处理策略 -->

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | 自适应布局 / Size Class | |
| Dynamic Type | `@ScaledMetric` / `UIFont.preferredFont` | |
| 深色模式 | Asset Catalog 颜色集 / `@Environment(\.colorScheme)` | |
| 安全区域 | `safeAreaInsets` / `edgesIgnoringSafeArea` | |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 View | 职责 |
|-----------|----------|------|
| | | |

### 4.2 状态定义

```swift
@Observable
final class {{ViewModelName}} {
    // MARK: - Published State
    var items: [Item] = []
    var isLoading: Bool = false
    var errorMessage: String?

    // MARK: - Dependencies
    private let apiService: APIService

    // MARK: - Actions
    func loadData() async { ... }
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| | | | |

### 4.4 UI 状态建模

<!-- 定义每个 UI 状态对应的数据和 View 层表现 -->

| UI 状态 | 判别条件 | View 层表现 |
|---------|---------|-----------|
| Loading | `isLoading == true` | 骨架屏 / ProgressView |
| Success (有数据) | `items.count > 0 && errorMessage == nil` | 正常列表/内容 |
| Empty (空数据) | `items.isEmpty && !isLoading` | 空态插图 + 引导文案 |
| Error (可重试) | `errorMessage != nil` | 错误提示 + 重试按钮 |
| Error (不可重试) | `errorMessage != nil && !isRetryable` | 错误提示 + 返回引导 |

---

## 5. Navigation 路由设计

### 5.1 导航方案

<!-- NavigationStack（iOS 16+）/ UINavigationController -->

### 5.2 路由清单

| 路由标识 | 目标页面 | 参数 | 导航方式 | 说明 |
|---------|---------|------|---------|------|
| | | | Push / Sheet / FullScreenCover / Alert | |

### 5.3 路由管理

```swift
// NavigationStack + NavigationPath
@Observable
final class NavigationRouter {
    var path = NavigationPath()

    func navigate(to destination: Destination) { ... }
    func dismiss() { ... }
}

enum Destination: Hashable {
    case detail(id: String)
    case settings
}
```

### 5.4 Deep Link 处理（如适用）

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `app://xxx/{id}` | {{DetailView}} | id |

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | URLSession / Alamofire | |
| 请求构建 | `APIEndpoint` protocol / Router pattern | |
| 请求拦截器 | Token 注入、设备信息、日志 | |
| 响应解析 | `Codable` + `JSONDecoder` | |
| 错误处理 | 统一 `APIError` 模型 | |

### 6.2 API 端点定义

```swift
protocol APIEndpoint {
    associatedtype Response: Decodable
    var path: String { get }
    var method: HTTPMethod { get }
    var headers: [String: String] { get }
    var body: Data? { get }
}
```

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 网络超时 | 2 | 指数退避 | |
| 5xx 服务端错误 | 3 | 指数退避 | |
| 401 Token 过期 | — | — | 刷新 token 后重放原请求 |

### 6.4 网络状态监听

<!-- NWPathMonitor / Reachability，处理 Wi-Fi ↔ 蜂窝切换 -->

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| | CoreData | | | |
| | UserDefaults | | | |
| | Keychain | | | |
| | FileManager | | | |

### 7.2 CoreData 模型设计（如适用）

```
Entity: {{EntityName}}
├── id: UUID
├── title: String
├── createdAt: Date
└── relationship: {{RelatedEntity}} (to-many / to-one)
```

### 7.3 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| | 内存缓存（NSCache） | | |
| | 磁盘缓存（FileManager） | | |

### 7.4 数据迁移策略

<!-- CoreData migration（轻量级 / 重量级）、UserDefaults key 兼容处理 -->

---

## 8. 配置与环境

| 配置项 | 管理方式 | 开发环境值 | 生产环境值 | 说明 |
|--------|---------|----------|-----------|------|
| API Base URL | xcconfig | | | |
| Feature Flag | Info.plist / Remote Config | | | |
| API Key | Keychain / xcconfig | | | |

> ⚠️ 禁止硬编码任何常量。使用 xcconfig + Info.plist 管理配置，敏感信息存入 Keychain。

---

## 9. API 调用清单

<!-- 列出本端需要调用的所有 API，与 design.md 保持严格一致 -->

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| `METHOD /api/path` | 页面加载 / 用户操作 | ViewModel 状态 | 更新 UI / 导航 | Toast / 内联 / Alert |

---

## 10. 跨端共享逻辑落地

<!-- 对应 design.md 中「跨端共享逻辑」章节 -->

| 共享逻辑 | design.md 定义 | iOS 端实现方式 |
|---------|---------------|---------------|
| | | |

---

## 11. 边界与错误处理

### 11.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| 网络层 | 响应拦截 → `APIError` 模型 | |
| ViewModel | `do-catch` / `Result` 类型 | |
| View 层 | `.alert()` / `.toast()` / 内联错误视图 | |
| 日志 | Crashlytics / os_log | |

### 11.2 错误码映射表

<!-- 与 design.md 中错误码对应，补充 iOS 端交互方式 -->

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `INVALID_PARAMS` | | 内联校验提示 |
| `UNAUTHORIZED` | | Alert + 跳转登录 |
| `FORBIDDEN` | | Toast / 返回上一页 |
| `NOT_FOUND` | | Toast / 空态页 |
| `CONFLICT` | | Toast + 重试引导 |
| `RATE_LIMITED` | | Toast（含倒计时） |
| `INTERNAL_ERROR` | | Toast + 重试按钮 |
| `NETWORK_ERROR` | | Toast + 重试按钮 |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| 网络切换（Wi-Fi ↔ 蜂窝） | NWPathMonitor 回调 | 中断的请求自动重试 / 暂停大文件下载 | 🟡 |
| App 进入后台 | `scenePhase` → `.background` | 暂停播放/任务、保存状态 | 🟡 |
| App 返回前台 | `scenePhase` → `.active` | 恢复任务、刷新过期数据 | 🟡 |
| 页面销毁时未完成请求 | View `onDisappear` / VC `deinit` | `task.cancel()` 取消请求 | 🔴 |
| 本地缓存过期/损坏 | 读取时 Decode 失败 | 清除损坏缓存，降级到网络请求 | 🟡 |
| 用户快速连续操作 | 快速点击 / 重复提交 | 防抖（300ms）/ 按钮 disabled | 🔴 |
| 内存警告 | `didReceiveMemoryWarning` | 释放 NSCache / 图片缓存 | 🟢 |
| 首次启动 | UserDefaults 无缓存标记 | 展示骨架屏 / Onboarding | 🟡 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| | | | | | |

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | ViewModel 逻辑 | | XCTest |
| 快照测试 | 关键 UI 状态 | | SnapshotTesting |
| UI 测试 | 关键用户流程 | | XCUITest |

### 12.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| | ViewModel 加载成功 | 正常网络 | 调用 loadData() | items 非空，isLoading = false | 单元 |
| | ViewModel 加载失败 | 网络异常 | 调用 loadData() | errorMessage 非空 | 单元 |
| | 空列表展示 | 返回空数据 | 页面加载完成 | 展示 empty 视图 | 单元 |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| API 请求 | Protocol Mock / URLProtocol Stub | |
| CoreData | NSInMemoryStoreType | |
| Keychain | Protocol Wrapper Mock | |

---

## 13. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| | | | |

> ⚠️ 新增开源依赖前必须征得用户同意（遵守根目录 CLAUDE.md 开发约束）。

---

## 14. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| | | 🔴/🟡/🟢 | 高/中/低 | | |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| | | |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| | |
