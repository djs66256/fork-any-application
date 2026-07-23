# 代码规范 — iOS

> 本文档定义 iOS 端 Swift + SwiftUI 的完整编码规范。

---

## 1. Swift 编码规范

### 1.1 命名约定

- **类型名**（class、struct、enum、protocol）：UpperCamelCase，如 `VideoPlayerViewModel`、`EpisodeRepository`。
- **函数与方法**：lowerCamelCase，动词开头，参数利用 Swift 参数标签表达语义，如 `func fetchRecommendations(for genre: Genre) async throws -> [Drama]`。
- **变量与常量**：lowerCamelCase，不缩写，如 `isLoading`、`dramaList`、`currentEpisodeIndex`。布尔值以 `is`/`has`/`should` 开头。
- **枚举 case**：lowerCamelCase，如 `.loading`、`.loaded`、`.error`。
- **全局常量**：lowerCamelCase，不鼓励使用全局变量；命名空间内静态常量优先。
- **协议**：描述能力用 `-able`/`-ible`（如 `Codable`、`Playable`），描述角色用名词（如 `DramaService`）。
- **不使用**：匈牙利前缀（`strName`）、下划线前缀（`_privateVar`）、单字母变量（循环变量除外：`i`、`j` 可接受）。

### 1.2 格式化

- 使用 **SwiftLint** 自动检查，配置文件为 `ios/.swiftlint.yml`。CI 中执行 `swiftlint lint --config ios/.swiftlint.yml --strict`，任何 warning 视为错误。
  ```yaml
  # .swiftlint.yml 核心规则
  line_length: 120
  type_body_length: 300
  file_length: 500
  function_body_length: 60
  cyclomatic_complexity: 12
  ```
- 缩进：4 空格，不使用 tab。
- 每行最多 120 字符（字符串字面量除外）。
- 文件末尾保留一个空行。
- `import` 按 Apple 框架 → 第三方 → 内部模块排序，组间空一行。
- 闭包参数尽量使用 trailing closure 语法，简化 `{ }` 中的参数名。

### 1.3 访问控制

- 默认使用 `private`：所有属性和方法除非明确需要暴露，否则标记为 `private`。
- `fileprivate` 仅在同一个文件内多个类型需要互相访问时才使用。
- `internal` 用于模块内共享（当前 App target 默认就是 internal，无需显式标注）。
- `public`/`open` 仅用于独立 Swift Package 对外暴露的 API。
- `private(set)` 用于外部可读但不可写的属性。

### 1.4 并发规范

- 使用 `async/await` 作为首选异步 API，避免回调闭包。
- 所有 UI 相关状态修改必须在 `@MainActor` 上下文中执行。ViewModel 标注 `@MainActor class`。
- 数据模型若跨线程传递，必须遵循 `Sendable`。编译器警告不得忽略。
- `Task` 中捕获的 `self` 不需要显式 `[weak self]`——结构化并发下 Task 不会造成引用循环。但仍需注意 cancellation：
  ```swift
  // 避免使用 Task { } 在 deinit 的对象中启动未绑定 cancellation 的任务
  func loadData() {
      self.task = Task { @MainActor in
          guard !Task.isCancelled else { return }
          let data = try await repository.fetch()
          guard !Task.isCancelled else { return }
          self.items = data
      }
  }
  ```
- 不推荐 `DispatchQueue` 与 `async/await` 混用。如需队列操作，使用 `Task.detached(priority:)` 或在 `Actor` 中完成。

---

## 2. SwiftUI 编码规范

### 2.1 View 编写

- 单个 `body` 计算属性不超过 30 行。超过则拆分为私有 `@ViewBuilder` 计算属性或独立子 View。
- 每个 View struct 只做一件事：展示该页面的结构，状态、逻辑交给 ViewModel。
- 务必提供至少一个 `#Preview`，覆盖 loading、empty、error、default 四种状态。
  ```swift
  #Preview("Default") { HomePage(viewModel: HomeViewModel.preview) }
  #Preview("Loading") { HomePage(viewModel: HomeViewModel.previewLoading) }
  ```
- 不直接在 View 中写网络请求、数据库操作。View 仅通过 ViewModel 暴露的 `@Published` 或 `@Observable` 属性驱动。
- 不要使用 `AnyView` 擦除类型——优先使用 `@ViewBuilder` 或 `some View` 返回类型。

### 2.2 状态管理

| 属性包装器 | 场景 |
|--------|------|
| `@State` | 视图私有、值类型的局部状态（如 TextField 的文本、弹窗 isPresented） |
| `@Binding` | 子视图需要读写父视图的 `@State` |
| `@StateObject` | 视图拥有 ViewModel 的生命周期（iOS 16 及以下） |
| `@ObservedObject` | 视图不拥有 ViewModel，由外部传入 |
| `@EnvironmentObject` | 全局共享状态（如当前用户、主题设置），通过 `.environmentObject()` 注入 |
| `@Environment` | 读取系统环境值（如 `\.dismiss`、`\.colorScheme`、`\.scenePhase`） |
| `@Observable` (iOS 17+) | 替代 ObservableObject，无需 `@Published`，自动追踪。项目中优先使用。 |

- 规则：一个 View 最多 3 个 `@State` 变量。过多状态 → 应抽离为 ViewModel。

### 2.3 数据流

- 严格单向数据流：
  ```
  User Action → View 调用 ViewModel 方法
  → ViewModel 更新状态 → View 响应式刷新
  ```
- 不通过 Delegate、NotificationCenter 回调通知 View。ViewModel 的状态变更驱动 View 刷新。
- 子 View 向父 View 通信：通过闭包回调（`onTap: () -> Void`）或 `@Binding`。
- 跨页面事件使用 `Combine` 的 `PassthroughSubject` 或自定义 `Publisher`，在 ViewModel 层处理，不在 View 层传递。

### 2.4 性能优化

- 使用 `Equatable` 协议配合 `.equatable()` 减少不必要的 body 重绘。
- 大列表使用 `LazyVStack`、`LazyVGrid`、`List` + `ForEach` 并确保 `ForEach` 中的元素遵循 `Identifiable`。
- 避免在 `body` 中执行复杂计算——计算结果应缓存为 ViewModel 的 `@Published` 属性。
- 图片列表中使用 `.resizable()` 配合显式 `frame(width:height:)`，避免图片解码时占用过大内存。
- 对 `@Observable` 中未被 View 读取的属性，使用 `@ObservationIgnored` 标记，避免不必要刷新。

---

## 3. 资源命名规范

### 3.1 Assets.xcassets

- 图片集命名：`snake_case`，按 `模块_功能_描述_状态` 格式。
  示例：`tab_home_icon_default`、`tab_home_icon_selected`、`player_pause_button_normal`。
- 颜色集命名：语义化命名，不限具体颜色值。
  示例：`text_primary`、`text_secondary`、`background_main`、`brand_primary`。
- 不使用 `1x` 图——仅保留 `2x` 和 `3x`，以 PDF 矢量或 SVG 优先。
- 命名空间中加前缀区分模块：`drama_`、`player_`、`user_`、`reward_`。

### 3.2 颜色

- 所有颜色统一在 `Assets.xcassets` 中作为 Color Set 定义，不得在代码中硬编码 `Color(hex: "#FF0000")`。
- 在 `Color+Theme.swift` 中通过扩展暴露语义化颜色：
  ```swift
  extension Color {
      static let textPrimary = Color("text_primary")
      static let brandPrimary = Color("brand_primary")
      static let bgMain = Color("background_main")
  }
  ```
- 支持 Dark Mode：每个 Color Set 必须提供 Light 和 Dark 两个变体。

### 3.3 字体

- 自定义字体文件放入 `ios/ShortDrama/Resources/Fonts/`，在 `Info.plist` 中通过 `UIAppFonts` 注册。
- 通过 `Font+Theme.swift` 统一字号和字重：
  ```swift
  extension Font {
      static let dramaTitle = Font.custom("PingFang SC", size: 18).weight(.semibold)
      static let dramaBody = Font.custom("PingFang SC", size: 14).weight(.regular)
      static let dramaCaption = Font.custom("PingFang SC", size: 12).weight(.light)
  }
  ```
- 不直接使用 `.system(size:)` 或 `.title`、`.body` 等语义字号——统一走项目字体扩展。

### 3.4 本地化字符串

- 使用 `Localizable.xcstrings`（Xcode 15+ 的 String Catalog 格式）替代旧的 `.strings` 文件。
- Key 使用英文原文，方便回退：
  ```swift
  Text("Start Watching", bundle: .main) // xcstrings 中 key = "Start Watching"
  ```
- 所有面向用户的字符串必须经过 `LocalizedStringKey`，禁止中文硬编码在 `Text("")` 中。
- 当前阶段支持 `zh-Hans`（简体中文）和 `en`（英文）。

---

## 4. 代码审查清单

每次 PR 的 iOS 端代码需逐项确认：

- [ ] 所有 `TODO` / `FIXME` 已移除或关联 JIRA 单号
- [ ] ViewModel 标注了 `@MainActor`（涉及 UI 状态时）
- [ ] 无 `print()`——使用 `Logger`（os.Logger）
- [ ] 无硬编码色值、字号、间距——走主题扩展或常量
- [ ] 无硬编码 URL、Token、环境地址——走统一配置管理
- [ ] `#Preview` 至少覆盖 loading/empty/error/default 中与当前改动相关的状态
- [ ] 新增业务逻辑有对应 XCTest 单元测试
- [ ] SwiftLint 零 warning（`swiftlint lint --strict` 通过）
- [ ] 无 `force unwrap`（`!`）——使用 `guard let` / `if let` 或提供明确的 crash 原因
- [ ] 数据模型标记 `Sendable`（跨并发域传递时）
- [ ] `body` 属性不超过 30 行（合理拆分）
- [ ] 未被 View 读取的 `@Observable` 属性已标记 `@ObservationIgnored`
