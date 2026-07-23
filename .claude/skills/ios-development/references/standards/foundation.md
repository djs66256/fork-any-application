# 基础库与基础能力 — iOS

> 本文档定义 iOS 端的基础库选型、集成方案与基础能力接入规范。

---

## 1. 网络层

### 1.1 Alamofire / URLSession

- 使用 **Alamofire** 作为 HTTP 客户端（见 open-source-libs.md）。
- 统一 Session 实例在 `Core/Network/APISession.swift` 中创建：

```swift
import Alamofire

final class APISession {
    let session: Session

    init() {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 15   // 请求超时 15 秒
        configuration.timeoutIntervalForResource = 30  // 资源超时 30 秒
        configuration.waitsForConnectivity = true       // 蜂窝网络等待连接

        let interceptor = APIRequestInterceptor()
        session = Session(configuration: configuration, interceptor: interceptor)
    }
}
```

- 使用 `APIRequestInterceptor` 统一注入 Authorization Header、Accept-Language 等公共请求头。
- API 端点通过 `URLRequestConvertible` 路由定义：

```swift
enum DramaAPI: URLRequestConvertible {
    case recommendations(page: Int, size: Int)
    case dramaDetail(id: String)
    case search(keyword: String, page: Int)

    var baseURL: URL { URL(string: ConfigurationManager.shared.apiBaseURL)! }

    func asURLRequest() throws -> URLRequest { ... }
}
```

- 禁止直接使用 `URLSession` 或 `AF.request` 内联发起网络请求。所有请求必须通过定义的 API Router。

### 1.2 序列化

- 默认使用 `JSONDecoder` 解码 API 响应，配置 `keyDecodingStrategy = .convertFromSnakeCase`。
- 日期格式统一为 ISO 8601（可同时配置多种格式的 `dateDecodingStrategy`）。
- 所有 DTO 必须遵循 `Decodable`，Domain Model 可选择性遵循 `Codable`。
- DTO 与 Domain Model 分离：
  ```swift
  // Data/DTO/DramaDTO.swift
  struct DramaDTO: Decodable {
      let id: String
      let title: String
      let coverUrl: String
      let totalEpisodes: Int
  }
  // Domain/Models/Drama.swift
  struct Drama: Identifiable, Sendable {
      let id: String
      let title: String
      let coverURL: URL
      let totalEpisodes: Int
  }
  ```
- DTO → Domain Model 的映射在 Repository 层完成，不在 ViewModel 或 View 中处理。

### 1.3 API 响应模型

- 后端统一响应格式：
  ```json
  { "code": 0, "message": "success", "data": { ... } }
  ```
- 定义通用响应包装：
  ```swift
  struct APIResponse<T: Decodable>: Decodable {
      let code: Int
      let message: String?
      let data: T
  }
  ```
- 错误码映射：非 0 code 通过 `APIErrorParser` 转换为 `AppError.server(code:message:)`。
- HTTP 状态码在 `APIRequestInterceptor` 的 `validate()` 中间层统一处理：
  - 401 → `AppError.authRequired`
  - 4xx → `AppError.server(code:httpCode, message:body.message)`
  - 5xx → `AppError.server(code:httpCode, message:"服务器繁忙，请稍后重试")`

---

## 2. 图片加载

### 2.1 Kingfisher 配置

- 使用 **Kingfisher** 加载与缓存图片。
- 全局配置在 App 启动时设置：

```swift
import Kingfisher

func configureKingfisher() {
    var config = KingfisherManager.shared.defaultOptions
    // 磁盘缓存最大 200MB
    ImageCache.default.diskStorage.config.sizeLimit = 200 * 1024 * 1024
    // 内存缓存最大 80MB
    ImageCache.default.memoryStorage.config.totalCostLimit = 80 * 1024 * 1024
    // 内存缓存过期 5 分钟
    ImageCache.default.memoryStorage.config.expiration = .seconds(300)
    // 磁盘缓存过期 7 天
    ImageCache.default.diskStorage.config.expiration = .days(7)
}
```

- 占位图统一通过 `KFImage.placeholder()` 设置，使用项目 `placeholder_drama` 图片资源。
- 图片加载失败时使用 `.onFailureImage()` 设置错误占位图。
- 磁盘缓存清理在 App 退到后台时触发：`ImageCache.default.cleanExpiredDiskCache()`。

### 2.2 SwiftUI 集成

- 统一使用 `KFImage` 加载图片：

```swift
import Kingfisher

KFImage(url)
    .placeholder { Image("placeholder_drama").resizable() }
    .onFailureImage(Image("placeholder_drama"))
    .resizable()
    .downsampling(size: CGSize(width: 200, height: 280))
    .cacheOriginalImage()
    .aspectRatio(contentMode: .fill)
    .frame(width: 100, height: 140)
    .clipShape(RoundedRectangle(cornerRadius: 8))
```

- 封装一个 `DramaCoverImage` 组件，对外只需传 `url: URL?` 和 `size: CGSize`，内部处理占位、缩放、圆角等细节。
- 通过 `KFImage.downsampling(size:)` 避免原始大图在列表 cell 中造成内存浪费。

---

## 3. 数据持久化

### 3.1 Core Data / SwiftData

- 当前阶段使用 **Core Data**（Apple 原生，兼容性好；SwiftData 为 iOS 17+，待覆盖率提升后迁移）。
- Model 文件 `ShortDrama.xcdatamodeld` 放在 `ios/ShortDrama/Resources/`。
- `NSPersistentContainer` 封装在 `CoreDataStack` 中，单例持有：

```swift
final class CoreDataStack: @unchecked Sendable {
    static let shared = CoreDataStack()

    let persistentContainer: NSPersistentContainer

    private init() {
        persistentContainer = NSPersistentContainer(name: "ShortDrama")
        persistentContainer.loadPersistentStores { _, error in
            if let error { fatalError("Core Data 加载失败: \(error)") }
        }
        persistentContainer.viewContext.automaticallyMergesChangesFromParent = true
    }

    func saveIfNeeded() { ... }
}
```

- 数据迁移：使用 `NSMappingModel` 或轻量迁移（设置 `NSInferMappingModelAutomaticallyOption = true`）。
- 所有 Core Data 操作在 `private context` 或 `perform(_:)` 中执行，不在主线程直接操作 `viewContext`。

### 3.2 Keychain

- 使用 **KeychainAccess** 封装 Keychain 读写：

```swift
import KeychainAccess

final class KeychainManager {
    static let shared = KeychainManager()
    private let keychain = Keychain(service: "com.djs66256.short_drama")

    var authToken: String? {
        get { try? keychain.get("auth_token") }
        set { try? keychain.set(newValue ?? "", key: "auth_token") }
    }

    var userId: String? {
        get { try? keychain.get("user_id") }
        set { try? keychain.set(newValue ?? "", key: "user_id") }
    }

    func clearAll() { try? keychain.removeAll() }
}
```

- Keychain 中存储的内容：auth token、user id、refresh token。不得存储图片、JSON 等大数据。
- 用户退出登录时调用 `clearAll()` 清空 Keychain。

### 3.3 UserDefaults

- 使用 `@AppStorage` 读取简单偏好（主题、语言、首次启动标记等），但只在 View 层使用。
- 非 View 层（ViewModel、UseCase）通过 `UserDefaults.standard` 或封装的 `PreferencesManager` 读写。
- 不将敏感数据（Token、密码）存入 UserDefaults——UserDefaults 不加密。
- 推荐的 Key 命名：使用 `ud_` 前缀 + 功能描述，如 `ud_isFirstLaunch`、`ud_selectedLanguage`。
- 所有 UserDefaults key 统一在 `PreferencesKeys.swift` 枚举中定义，禁止散布字符串。

---

## 4. 日志系统

### 4.1 日志配置

- 使用 Apple **swift-log** (`Logging`)，在 App 启动时配置 `LoggingSystem`：

```swift
import Logging

func configureLogging() {
    LoggingSystem.bootstrap { label in
        var handler = StreamLogHandler.standardOutput(label: label)
        #if DEBUG
        handler.logLevel = .trace
        #else
        handler.logLevel = .warning
        #endif
        return handler
    }
}
```

- 各模块通过 `Logger(label:)` 创建日志实例：

```swift
extension Logger {
    static let network = Logger(label: "com.shortdrama.network")
    static let player = Logger(label: "com.shortdrama.player")
    static let auth = Logger(label: "com.shortdrama.auth")
}
```

### 4.2 日志级别

| 级别 | 场景 | Release 是否输出 |
|------|------|:---:|
| `trace` | 函数进入/退出、变量值 | 否 |
| `debug` | 调试信息、中间状态 | 否 |
| `info` | 关键路径流程（页面进入、视频开始播放） | 否 |
| `notice` | 需要注意但非错误（Token 即将过期） | 否 |
| `warning` | 可恢复的异常（请求重试、缓存未命中） | 是 |
| `error` | 请求失败、解码失败、数据库写入失败 | 是 |
| `critical` | 崩溃级别、不可恢复错误 | 是 |

### 4.3 Release 日志

- Release 构建 `logLevel` 设为 `.warning`，减少性能开销。
- Warning 及以上级别日志可配置上报至崩溃收集平台（如 Firebase Crashlytics 自定义日志）。
- 不记录用户隐私信息到日志（Token、手机号、姓名），在日志输出前脱敏。

---

## 5. 性能监控

### 5.1 启动耗时

- 使用 **MetricKit** 监控启动耗时。
- 启动阶段定义：
  - **Pre-main**：dylib 加载、rebase/bind、ObjC 初始化——只能通过 Xcode Organizer 或 MetricKit `MXAppLaunchMetric` 获取。
  - **Post-main**：`application(_:didFinishLaunchingWithOptions:)` 到第一个 `CA::Transaction::commit()`。
- 在 App 启动完成后上报启动耗时：`MXMetricManager.shared.add(self)`，实现 `MXMetricManagerSubscriber` 协议。
- 优化目标：冷启动 < 2 秒（包含网络首屏数据加载前骨架屏展示）。

### 5.2 卡顿监控

- 采用 MetricKit `MXHangDiagnostic` 自动收集主线程卡顿（iOS 14+）。
- 自定义卡顿阈值：主线程阻塞 > 250ms 记录 warning 日志，> 1s 记录 error 日志。
- 使用 `Instruments → Time Profiler` 分析卡顿时的方法调用栈。
- 代码层面：避免在主线程执行 Core Data fetch、图片解码、JSON 解析——均已通过 async/await 或 Background Context 处理。

### 5.3 内存

- 使用 Xcode **Memory Graph Debugger** 定期检测循环引用（Debug Navigator → Memory → 右上角相机图标）。
- 避免 retain cycle：
  - 闭包捕获 `self` 时，非 async 闭包使用 `[weak self]`。
  - delegate 使用 `weak var`。
- 大对象（图片缓存、视频缓冲区）及时释放：`ImageCache.default.clearMemoryCache()` 在内存警告时调用。
- 监听 `UIApplication.didReceiveMemoryWarningNotification`，触发 ViewModel 中的缓存清理。

---

## 6. 崩溃收集

### 6.1 Firebase Crashlytics 集成

- 使用 **Firebase Crashlytics** 收集崩溃（见 open-source-libs.md）。
- 在 `AppDelegate` / `App` 入口初始化：

```swift
import Firebase

func application(_ application: UIApplication,
                 didFinishLaunchingWithOptions launchOptions: ...) -> Bool {
    FirebaseApp.configure()
    return true
}
```

### 6.2 dSYM 上传

- dSYM 通过 Fastlane 自动化上传：
  ```ruby
  # fastlane/Fastfile
  lane :upload_dsyms do
    upload_symbols_to_crashlytics(
      dsym_path: "./ShortDrama.app.dSYM.zip",
      gsp_path: "./GoogleService-Info.plist"
    )
  end
  ```
- CI 构建配置 `DEBUG_INFORMATION_FORMAT = dwarf-with-dsym`，确保每次 archive 都生成 dSYM。

### 6.3 自定义日志 (Breadcrumbs)

- 在关键路径记录 breadcrumbs，帮助定位崩溃前的用户操作路径：
  ```swift
  // 页面进入
  Crashlytics.crashlytics().log("Home page appeared")
  // 视频开始播放
  Crashlytics.crashlytics().log("Playing drama_id=\(id) episode=\(ep)")
  // 网络请求发起
  Crashlytics.crashlytics().log("Request: GET /api/drama/recommendations")
  ```
- 用户 ID 绑定：`Crashlytics.crashlytics().setUserID(userId)`，登录成功后立即调用。
- 自定义 key：`Crashlytics.crashlytics().setCustomValue(version, forKey: "app_version")`。

---

## 7. 国际化 (i18n)

### 7.1 资源组织

- 使用 **String Catalog**（`Localizable.xcstrings`，Xcode 15+）管理多语言字符串。
- 文件位于 `ios/ShortDrama/Resources/Localizable.xcstrings`。
- 支持的语种：`zh-Hans`（简体中文）为默认，`en`（英文）为第二语种。
- 新增字符串时直接在 xcstrings 中添加对应翻译，不创建单独的 `.strings` 文件。

### 7.2 运行时语言切换

- 通过 `PreferencesManager` 存储当前语言选择（Key：`ud_appLanguage`）。
- 切换语言时，修改 `Bundle` 的加载语言，可不重启 App 实现切换（依赖具体方案复杂度决定是否需要重启）。
- 若选择不重启方案，需在 `UserDefaults` 中设置 `AppleLanguages` 并刷新根视图。
- 数字、日期格式化使用 `NumberFormatter` / `DateFormatter` 并设置 `locale`，不硬编码格式。

---

## 8. 无障碍 (A11y)

### 8.1 accessibilityLabel

- 所有可交互元素必须有 `accessibilityLabel`：
  ```swift
  Button(action: { viewModel.play() }) {
      Image(systemName: "play.fill")
  }
  .accessibilityLabel("播放")
  ```
- 仅展示性元素（纯装饰图片、分割线）标记 `accessibilityHidden(true)`。
- Label 应简洁描述元素内容，不包含类型（"播放"而非"播放按钮"——VoiceOver 会读出类型）。

### 8.2 accessibilityHint

- 仅当操作结果不明显时才添加 `accessibilityHint`：
  ```swift
  .accessibilityHint("双击开始播放当前剧集")
  ```
- Hint 描述的是操作的结果，而非操作方式（不说"双击以..."）。

### 8.3 Dynamic Type

- 所有文字使用项目自定义 `Font` 扩展（`Font.dramaTitle` / `Font.dramaBody` / `Font.dramaCaption`），底层使用 Dynamic Type 兼容的字体度量。
- 使用 `@ScaledMetric` 动态缩放固定尺寸的 UI 元素（图标、间距）：
  ```swift
  @ScaledMetric(relativeTo: .body) private var iconSize = 24.0
  ```
- 列表行高不得硬编码固定值——根据内容自适应。
- 使用 `.lineLimit(nil)` 或合理范围以避免文字截断。
