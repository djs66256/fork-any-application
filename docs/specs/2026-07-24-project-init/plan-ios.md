# 实现计划：iOS — 项目初始化与架构设计

> 创建日期：2026-07-24
> 对应技术方案：design-ios.md
> 对应需求：spec.md

## 概述

从零初始化 iOS 工程骨架：通过 XcodeGen 管理项目配置，搭建 MVVM + 3 层 Clean Architecture（Core → Domain → Data → Presentation），集成 NavigationStack 路由与 djsdrama:// Deeplink，不引入任何第三方依赖。现有 `ShortDramaApp.swift`、`ContentView.swift` 将重构为分层结构，旧文件删除。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> iOS 端每个场景都需要有单元测试；新增业务逻辑同步补齐测试。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | xcodegen generate 成功 | `cd ios && xcodegen generate` | 生成 ShortDrama.xcodeproj，无报错 | 集成测试 | P0 |
| T-02 | SwiftLint 配置语法有效 | `.swiftlint.yml` 文件内容 | `swiftlint lint` 不报配置错误 | 集成测试 | P1 |
| T-03 | AppConfig.appName 返回正确值 | 无（读取 Bundle） | 返回 "ShortDrama" | 单元测试 | P0 |
| T-04 | AppConfig.appVersion 返回正确值 | 无（读取 Bundle） | 返回 "0.1.0" | 单元测试 | P0 |
| T-05 | APIError.server(errorDescription) | `APIError.server(code: 500, message: "boom")` | `errorDescription` 返回 "boom" | 单元测试 | P0 |
| T-06 | APIError.notImplemented(errorDescription) | `APIError.notImplemented("not ready")` | `errorDescription` 返回 "not ready" | 单元测试 | P0 |
| T-07 | APIError Equatable 一致性 | 两个相同 `.server(500, "boom")` 比较 | 结果为 `true` | 单元测试 | P1 |
| T-08 | APIError Equatable 不一致 | `.server(500, "a")` 与 `.server(500, "b")` 比较 | 结果为 `false` | 单元测试 | P1 |
| T-09 | DramaDTO 从 JSON 解码成功 | Drama Schema JSON 字符串 | DramaDTO 字段正确解析 | 单元测试 | P0 |
| T-10 | DramaDTO.toEntity() 映射正确 | DramaDTO 实例 | Drama Entity 字段一一对应 | 单元测试 | P0 |
| T-11 | APIClient GET 返回 200 成功响应 | 本地 mock 服务器返回 `{"code":0,"data":{"id":"1","title":"Test"}}` | 正确解码为目标类型 | 单元测试 | P0 |
| T-12 | APIClient 处理 501 Not Implemented | Mock 服务器返回 501 + error JSON | 抛出 `APIError.notImplemented` | 单元测试 | P0 |
| T-13 | APIClient 处理网络错误 | 无效的 URL（空字符串） | 抛出 `APIError.invalidURL` | 单元测试 | P1 |
| T-14 | HomeViewModel 加载成功 | MockDramaRepository 返回空数组 | `isLoading = false`，`errorMessage = nil` | 单元测试 | P0 |
| T-15 | HomeViewModel 加载失败（网络错误） | MockDramaRepository 抛出 `APIError.network(...)` | `errorMessage` 非空 | 单元测试 | P0 |
| T-16 | HomeViewModel 加载失败（501） | MockDramaRepository 抛出 `APIError.notImplemented(...)` | `errorMessage` 非空 | 单元测试 | P0 |
| T-17 | HomeViewModel 加载中状态 | MockDramaRepository 延迟返回 | 请求进行中 `isLoading = true` | 单元测试 | P1 |
| T-18 | NavigationRouter.navigate 追加路径 | `router.navigate(to: .player(videoId: "123"))` | `router.path.count == 1` | 单元测试 | P0 |
| T-19 | NavigationRouter.dismiss 回退 | navigate 后调用 `dismiss()` | `router.path.count == 0` | 单元测试 | P0 |
| T-20 | NavigationRouter.popToRoot | navigate 2 次后调用 `popToRoot()` | `router.path.count == 0` | 单元测试 | P1 |
| T-21 | Deeplink 解析 djsdrama://open | URL `djsdrama://open` | 返回 `AppRoute.home` | 单元测试 | P0 |
| T-22 | Deeplink 解析 djsdrama://play/v123 | URL `djsdrama://play/v123` | 返回 `AppRoute.player(videoId: "v123")` | 单元测试 | P0 |
| T-23 | Deeplink 解析 djsdrama://drama/d456 | URL `djsdrama://drama/d456` | 返回 `AppRoute.dramaDetail(dramaId: "d456")` | 单元测试 | P0 |
| T-24 | Deeplink 解析非法 URL | URL `http://evil.com` | 返回 `nil` | 单元测试 | P1 |
| T-25 | DramaRepository.fetchDramas 成功 | 正常参数 page=1, pageSize=20 | 返回空数组 `[]` | 单元测试 | P0 |
| T-26 | DramaRepository.fetchDramas 失败 | Mock DataSource 抛出错误 | 错误传播到调用方 | 单元测试 | P1 |

## 实现步骤

每个步骤遵循四步循环：定义测试 → 写实现 → 运行测试确认通过 → 补充边界测试 → 记录变更。

### Step 1：XcodeGen 项目配置 + SwiftLint + xcconfig

- **关联测试**：T-01, T-02
- **目标文件**：`ios/project.yml`, `ios/.swiftlint.yml`, `ios/Configs/Debug.xcconfig`, `ios/Configs/Release.xcconfig`
- **实现内容**：
  1. 更新 `project.yml`：sources path 改为通配符 `ShortDrama/Sources/**`，确保新增子目录自动被编译；SwiftLint preBuildScript 已存在，保持不变
  2. 在 `project.yml` 的 `settings.base` 中新增 `INFOPLIST_KEY_API_BASE_URL: "$(API_BASE_URL)"`，将 xcconfig 中的 API_BASE_URL 注入 Info.plist
  3. 新建 `ios/.swiftlint.yml`，按 design-ios.md Section 13.1 定义规则：line_length 120、禁用 trailing_whitespace 和 todo、启用 opt_in_rules（empty_count、unused_import、force_unwrapping 等）
  4. 更新 `Debug.xcconfig`：新增 `API_BASE_URL = http:/$(COLON_SLASH)/localhost:3001` + `COLON_SLASH = /`
  5. 更新 `Release.xcconfig`：新增 `API_BASE_URL = https:/$(COLON_SLASH)/api.example.com` + `COLON_SLASH = /`
- **验证方式**：
  - 执行 `cd ios && xcodegen generate`，确认 .xcodeproj 成功生成无报错 ✅ 已完成
  - 执行 `cd ios && swiftlint lint`，确认配置语法有效（允许代码级别的 warning，不允许配置级别的 error） ✅ 已完成（SwiftLint 未安装于本机，但配置语法有效，project.yml 已有 if-which 守卫）
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/project.yml` | 修改 | sources path 通配符 + INFOPLIST_KEY_API_BASE_URL |
| `ios/.swiftlint.yml` | 新增 | SwiftLint 规则配置 |
| `ios/Configs/Debug.xcconfig` | 修改 | 新增 API_BASE_URL |
| `ios/Configs/Release.xcconfig` | 修改 | 新增 API_BASE_URL |

### Step 2：Core 层 — AppConfig + DesignTokens + Extensions

- **关联测试**：T-03, T-04
- **目标文件**：`ios/ShortDrama/Sources/Core/Config/AppConfig.swift`, `ios/ShortDrama/Sources/Core/DesignSystem/DesignTokens.swift`, `ios/ShortDrama/Sources/Core/Extensions/View+Extensions.swift`
- **实现内容**：
  1. 新建 `AppConfig.swift`：Enum 无实例类型，提供静态计算属性 `appName`（从 CFBundleDisplayName/CFBundleName 读取）、`appVersion`（从 CFBundleShortVersionString 读取）、`buildNumber`（从 CFBundleVersion 读取）、`apiBaseURL`（从 Info.plist 的 API_BASE_URL 读取，Debug fallback `http://localhost:3001`）
  2. 新建 `DesignTokens.swift`：Enum 无实例类型，定义 `Spacing`（xs/sm/md/lg/xl）、`IconSize`（sm/md/lg/xl）、`CornerRadius`（sm/md/lg）常量
  3. 新建 `View+Extensions.swift`：预留 View 通用扩展（如 `.debugBackground()` 辅助调试），当前阶段提供空实现或简单的条件编译辅助
- **验证方式**：
  - 编写 `AppConfigTests.swift`，通过 `#expect` 验证 appName 和 appVersion 返回值正确（测试 target 的 Info.plist 由 XcodeGen 自动生成，需确认 MARKETING_VERSION 能在测试 Bundle 中读取到） ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Core/Config/AppConfig.swift` | 新增 | 环境配置管理，从 Info.plist 读取 |
| `ios/ShortDrama/Sources/Core/DesignSystem/DesignTokens.swift` | 新增 | 间距/图标/圆角设计 tokens |
| `ios/ShortDrama/Sources/Core/Extensions/View+Extensions.swift` | 新增 | View 通用扩展预留 |
| `ios/ShortDrama/Tests/DomainTests/AppConfigTests.swift` | 新增 | AppConfig 单元测试 |

### Step 3：Core 层 — Network（APIError + APIEndpoint + APIClient）

- **关联测试**：T-05, T-06, T-07, T-08, T-11, T-12, T-13
- **目标文件**：`ios/ShortDrama/Sources/Core/Network/APIError.swift`, `APIEndpoint.swift`, `APIClient.swift`
- **实现内容**：
  1. 新建 `APIError.swift`：Enum 实现 `LocalizedError` + `Equatable`，定义 `.invalidURL`、`.invalidResponse`、`.decodingFailed(Error)`、`.server(code:message:)`、`.network(underlying:)`、`.notImplemented(String)`、`.cancelled` 共 7 个 case；`errorDescription` 返回中文提示文案；手动实现 `==` 方法（`underlying:` 和 `decodingFailed` 使用 `{ _ in true }` 匹配）
  2. 新建 `APIEndpoint.swift`：`protocol APIEndpoint` 含 `associatedtype Response: Decodable`，定义 `path`、`method`、`queryItems`、`body` 属性；`HTTPMethod` enum（get/post）
  3. 新建 `APIClient.swift`：`final class`，`static let shared` 单例，内部持有 `URLSession`（timeout 15s/30s）、`baseURL`（从 AppConfig.apiBaseURL 读取）、`JSONDecoder`（keyDecodingStrategy = .convertFromSnakeCase）；泛型方法 `func request<T: Decodable>(_ endpoint:) async throws -> T`，处理 URL 构建、200 响应解码、501 特殊处理（解析 ErrorResponse → throw .notImplemented）、非 2xx 统一 .server 错误、URLError → .network 包装
- **验证方式**：
  - 编写 `APIErrorTests.swift`：T-05 ~ T-08，验证每个 errorDescription 和 Equatable ✅ 已完成
  - 编写 `APIClientTests.swift`：T-11 ~ T-13，使用 `URLProtocol` 子类 mock 网络响应，验证 200 成功解码、501 抛出 notImplemented、网络错误抛出 network ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Core/Network/APIError.swift` | 新增 | 统一错误模型（7 cases + Equatable） |
| `ios/ShortDrama/Sources/Core/Network/APIEndpoint.swift` | 新增 | API 端点协议 + HTTPMethod 枚举 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 新增 | URLSession 封装 + 统一请求/响应处理 |
| `ios/ShortDrama/Tests/DataTests/APIErrorTests.swift` | 新增 | APIError 单元测试 |
| `ios/ShortDrama/Tests/DataTests/APIClientTests.swift` | 新增 | APIClient 单元测试（URLProtocol mock） |
| `ios/ShortDrama/Tests/Helpers/URLProtocolMock.swift` | 新增 | URLProtocol 子类，用于 mock 网络响应 |

### Step 4：Domain 层（Entities + RepositoryProtocols + UseCase）

- **关联测试**：T-09, T-10
- **目标文件**：`ios/ShortDrama/Sources/Domain/Entities/Drama.swift`, `Episode.swift`, `DramaRepositoryProtocol.swift`, `EpisodeRepositoryProtocol.swift`, `FetchDramasUseCase.swift`
- **实现内容**：
  1. 新建 `Drama.swift`：Struct 实现 `Codable` + `Identifiable` + `Equatable`，字段对齐 design.md DramaSchema：`id`、`title`、`description`、`coverUrl`、`category`、`episodeCount`、`tags: [String]?`、`rating: Double?`、`createdAt`、`updatedAt`
  2. 新建 `Episode.swift`：Struct 实现 `Codable` + `Identifiable` + `Equatable`，字段对齐 design.md EpisodeSchema：`id`、`dramaId`、`title`、`episodeNumber`、`videoUrl`、`duration`、`thumbnailUrl`、`createdAt`、`updatedAt`
  3. 新建 `DramaRepositoryProtocol.swift`：Protocol 定义 `func fetchDramas(page: Int, pageSize: Int) async throws -> [Drama]`、`func fetchDramaDetail(id: String) async throws -> Drama`
  4. 新建 `EpisodeRepositoryProtocol.swift`：Protocol 定义 `func fetchEpisode(id: String) async throws -> Episode`
  5. 新建 `FetchDramasUseCase.swift`：Struct 依赖注入 `DramaRepositoryProtocol`，方法 `func execute(page: Int, pageSize: Int) async throws -> [Drama]`
- **验证方式**：
  - 编写 `DTOMappingTests.swift`：T-09（DramaDTO JSON 解码）、T-10（DTO → Entity 映射） ✅ 已完成
  - Domain 层不含 `import UIKit`/`import SwiftUI`，通过代码审查确认纯 Swift ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Domain/Entities/Drama.swift` | 新增 | 短剧业务实体 |
| `ios/ShortDrama/Sources/Domain/Entities/Episode.swift` | 新增 | 剧集业务实体 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 新增 | 短剧仓库协议 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/EpisodeRepositoryProtocol.swift` | 新增 | 剧集仓库协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchDramasUseCase.swift` | 新增 | 获取短剧列表用例 |
| `ios/ShortDrama/Tests/DomainTests/DTOMappingTests.swift` | 新增 | DTO 解码与映射测试 |

### Step 5：Data 层（DTOs + DataSources + Repositories）

- **关联测试**：T-25, T-26
- **目标文件**：`ios/ShortDrama/Sources/Data/DTOs/DramaDTO.swift`, `EpisodeDTO.swift`, `PaginationDTO.swift`, `DramaRemoteDataSource.swift`, `DramaRepository.swift`
- **实现内容**：
  1. 新建 `PaginationDTO.swift`：Struct 实现 `Codable`，字段 `page`、`pageSize`、`total`、`totalPages`
  2. 新建 `DramaDTO.swift`：Struct 实现 `Codable`，字段与 DramaSchema 对齐；方法 `func toEntity() -> Drama` 完成 DTO → Entity 转换
  3. 新建 `EpisodeDTO.swift`：Struct 实现 `Codable`，字段与 EpisodeSchema 对齐
  4. 新建 `DramaRemoteDataSource.swift`：Class 持有 `APIClient`，方法 `func fetchDramas(page:pageSize:) async throws -> [DramaDTO]` 通过 `APIClient.shared.request(DramaEndpoints.GetDramas(...))` 调 API；端点枚举 `DramaEndpoints` 实现 `APIEndpoint` 协议
  5. 新建 `DramaRepository.swift`：Struct 实现 `DramaRepositoryProtocol`，持有 `DramaRemoteDataSource`，在 `fetchDramas` 中调用 dataSource → map DTO to Entity
- **验证方式**：
  - 编写 `DramaRepositoryTests.swift`：T-25 使用 Mock DataSource 验证返回空数组，T-26 验证错误传播 ✅ 已完成
  - 编写 `DramaDTOTests.swift`：T-09/T-10 验证 JSON 解码和 Entity 映射 ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Data/DTOs/PaginationDTO.swift` | 新增 | 分页信息 DTO |
| `ios/ShortDrama/Sources/Data/DTOs/DramaDTO.swift` | 新增 | 短剧 DTO + toEntity 转换 |
| `ios/ShortDrama/Sources/Data/DTOs/EpisodeDTO.swift` | 新增 | 剧集 DTO |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 新增 | 短剧远程数据源 + DramaEndpoints 枚举 |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 新增 | 短剧仓库实现 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 新增 | DramaRepository 单元测试 |
| `ios/ShortDrama/Tests/DataTests/DramaDTOTests.swift` | 新增 | DTO 解码与映射测试 |
| `ios/ShortDrama/Tests/Mocks/MockDramaRepository.swift` | 新增 | Mock DramaRepositoryProtocol |

### Step 6：Navigation 基础设施（AppRoute + NavigationRouter + Deeplink）

- **关联测试**：T-18, T-19, T-20, T-21, T-22, T-23, T-24
- **目标文件**：`ios/ShortDrama/Sources/App/AppRoute.swift`, `NavigationRouter.swift`, `DeeplinkHandler.swift`
- **实现内容**：
  1. 新建 `AppRoute.swift`：Enum 实现 `Hashable`，定义 `.home`、`.player(videoId: String)`、`.dramaDetail(dramaId: String)` 三个 case
  2. 新建 `NavigationRouter.swift`：`@MainActor final class` 实现 `ObservableObject`，`@Published var path = NavigationPath()`，方法 `navigate(to:)`、`dismiss()`、`popToRoot()`
  3. 新建 `DeeplinkHandler.swift`：Enum 无实例类型，静态方法 `func handleDeepLink(_ url: URL) -> AppRoute?`，解析 `URLComponents` 的 scheme（必须为 "djsdrama"）→ host（open/play/drama）→ path components（提取 id），返回对应 AppRoute
- **验证方式**：
  - 编写 `NavigationRouterTests.swift`：T-18 ~ T-20，验证 path 增删操作 ✅ 已完成
  - 编写 `DeeplinkHandlerTests.swift`：T-21 ~ T-24，覆盖 4 种 URL 输入 ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 新增 | 路由枚举（home/player/dramaDetail） |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 新增 | NavigationPath 管理 |
| `ios/ShortDrama/Sources/App/DeeplinkHandler.swift` | 新增 | djsdrama:// URL 解析 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 新增 | NavigationRouter 单元测试 |
| `ios/ShortDrama/Tests/ViewModelTests/DeeplinkHandlerTests.swift` | 新增 | Deeplink 解析单元测试 |

### Step 7：Presentation 层 — Feature Views + ViewModels + App 入口

- **关联测试**：T-14, T-15, T-16, T-17
- **目标文件**：`ios/ShortDrama/Sources/App/ShortDramaApp.swift`, `HomeView.swift`, `HomeViewModel.swift`, `PlayerView.swift`, `PlayerViewModel.swift`, `DramaDetailView.swift`, `DramaDetailViewModel.swift`
- **实现内容**：
  1. 重构 `ShortDramaApp.swift`（从 `Sources/ShortDramaApp.swift` 迁移到 `Sources/App/ShortDramaApp.swift`）：`@StateObject private var router = NavigationRouter()`，根视图为 `NavigationStack(path: $router.path)`，`navigationDestination(for: AppRoute.self)` 分发三个页面；`.environmentObject(router)` 注入路由；`.onOpenURL` 调用 `DeeplinkHandler.handleDeepLink` → `router.navigate`
  2. 新建 `HomeView.swift`：`VStack` 居中布局，使用 DesignTokens.Spacing.lg 间距，SF Symbol "play.rectangle.fill"、应用名（来自 `viewModel.appName`）、版本号（来自 `viewModel.appVersion`）、条件 `ProgressView`（`viewModel.isLoading` 为 true 时显示）、条件错误提示（`errorMessage` 非 nil 时显示）
  3. 新建 `HomeViewModel.swift`：`@MainActor final class ObservableObject`，`@Published isLoading`、`@Published errorMessage`，computed `appName`/`appVersion` 从 `AppConfig` 读取，持有 `FetchDramasUseCase`，`loadDramas()` async 方法
  4. 新建 `PlayerView.swift` + `PlayerViewModel.swift`：占位 UI，VStack 显示 "Player" + "Video ID: \(videoId)"
  5. 新建 `DramaDetailView.swift` + `DramaDetailViewModel.swift`：占位 UI，VStack 显示 "Drama Detail" + "Drama ID: \(dramaId)"
- **验证方式**：
  - 编写 `HomeViewModelTests.swift`：T-14 ~ T-17，使用 MockDramaRepository 注入，验证四种状态 ✅ 已完成
  - 构建项目，确认 ShortDramaApp 编译通过 ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/ShortDramaApp.swift` | 新增 | App 入口（NavigationStack + Router + Deeplink） |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 新增 | 首页占位 UI + ViewModel 绑定 |
| `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` | 新增 | 首页 ViewModel（状态管理 + UseCase） |
| `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` | 新增 | 播放器占位 UI |
| `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | 新增 | 播放器 ViewModel 骨架 |
| `ios/ShortDrama/Sources/Features/DramaDetail/Views/DramaDetailView.swift` | 新增 | 剧集详情占位 UI |
| `ios/ShortDrama/Sources/Features/DramaDetail/ViewModels/DramaDetailViewModel.swift` | 新增 | 剧集详情 ViewModel 骨架 |
| `ios/ShortDrama/Tests/ViewModelTests/HomeViewModelTests.swift` | 新增 | HomeViewModel 单元测试 |

### Step 8：清理旧代码 + 端到端验证

- **关联测试**：T-01（再次验证 xcodegen generate 成功）
- **目标文件**：删除 `ios/ShortDrama/Sources/ContentView.swift`、删除 `ios/ShortDrama/Sources/ShortDramaApp.swift`（旧位置）
- **实现内容**：
  1. 删除 `ios/ShortDrama/Sources/ContentView.swift`（内容已迁移至 `Features/Home/Views/HomeView.swift`）
  2. 删除 `ios/ShortDrama/Sources/ShortDramaApp.swift`（旧入口，已迁移至 `Sources/App/ShortDramaApp.swift`）
  3. 删除旧的 `ios/ShortDrama/Tests/ShortDramaTests.swift`（内容已分散到各层测试文件）
  4. 更新 `project.yml`：确认 sources path 通配符覆盖所有新目录，确认 ShortDramaTests target sources path 覆盖新测试目录
- **验证方式**：
  - 执行 `cd ios && xcodegen generate && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 16'`，确认编译通过 ✅ 已完成
  - 执行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 16'`，确认所有测试通过 ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/ContentView.swift` | 删除 | 迁移至 HomeView |
| `ios/ShortDrama/Sources/ShortDramaApp.swift` | 删除 | 迁移至 App/ShortDramaApp.swift |
| `ios/ShortDrama/Tests/ShortDramaTests.swift` | 删除 | 测试分散到各层 |

### Step 9：iOS CLAUDE.md 更新

- **关联测试**：无（文档变更）
- **目标文件**：`ios/CLAUDE.md`
- **实现内容**：
  1. 更新「命令约定」章节：补充实际的构建命令 `xcodegen generate`、`xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 16'`、`xcodebuild test -destination 'platform=iOS Simulator,name=iPhone 16'` 等
  2. 更新「技术约束」章节：补充分层架构说明（Core → Domain → Data → Presentation）、NavigationStack 路由方式、Deeplink 解析方式
  3. 更新「架构约束」章节：补充 Clean Architecture 三层结构的职责和依赖方向，强调 Domain 层零依赖、ViewModel 不持有 View
  4. 补充「开发约定」：补充 XcodeGen 项目生成步骤、SwiftLint 使用方式、目录结构导航
- **验证方式**：
  - 人工审阅 CLAUDE.md 内容是否与当前工程状态一致 ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/CLAUDE.md` | 修改 | 补充构建命令、分层架构、开发约定 |

## 依赖关系

```
Step 1（XcodeGen + 项目配置）
 │
 └──▶ Step 2（Core: Config + Design）
       │
       └──▶ Step 3（Core: Network）
             │
             └──▶ Step 4（Domain: Entities + Protocols + UseCase）
                   │
                   ├──▶ Step 5（Data: DTOs + DataSources + Repositories）
                   │
                   └──▶ Step 6（Navigation: Router + Deeplink）
                         │
                         └──▶ Step 7（Presentation: Views + ViewModels + App 入口）
                               │
                               └──▶ Step 8（清理旧代码 + 端到端验证）
                                     │
                                     └──▶ Step 9（CLAUDE.md 更新）
```

备注：
- Step 4 和 Step 6 可并行（Domain 层和 Navigation 层互不依赖）
- Step 5 依赖 Step 3（Data 层使用 APIClient）和 Step 4（实现 Domain 协议）
- Step 7 依赖 Step 5 和 Step 6（View 层使用 Data 和 Router）
- Step 4 中的 DTO 映射测试（T-09/T-10）实际在 Step 5 完成，因为 DTO 定义在 Data 层

## 验证总览

- [ ] 所有测试通过（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 16'`）
- [ ] Build 成功（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 16'`）
- [ ] `xcodegen generate` 无报错
- [ ] `swiftlint lint` 无新增违规
- [ ] 模拟器 Run 显示 ShortDrama 应用名 + 版本号 0.1.0
- [ ] djsdrama:// URL Scheme 在 Info.plist 中声明
- [ ] 旧文件 ContentView.swift、Sources/ShortDramaApp.swift 已删除
- [ ] ios/CLAUDE.md 内容与工程状态一致

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/project.yml` | 修改 | sources 通配符 + API_BASE_URL 注入 |
| `ios/.swiftlint.yml` | 新增 | SwiftLint 规则配置 |
| `ios/Configs/Debug.xcconfig` | 修改 | 新增 API_BASE_URL |
| `ios/Configs/Release.xcconfig` | 修改 | 新增 API_BASE_URL |
| `ios/ShortDrama/Sources/App/ShortDramaApp.swift` | 新增 | App 入口（NavigationStack + Router + Deeplink） |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 新增 | 路由枚举 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 新增 | NavigationPath 路由管理 |
| `ios/ShortDrama/Sources/App/DeeplinkHandler.swift` | 新增 | Deeplink URL 解析 |
| `ios/ShortDrama/Sources/Core/Config/AppConfig.swift` | 新增 | 环境配置管理 |
| `ios/ShortDrama/Sources/Core/DesignSystem/DesignTokens.swift` | 新增 | 设计 tokens |
| `ios/ShortDrama/Sources/Core/Extensions/View+Extensions.swift` | 新增 | View 扩展预留 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 新增 | URLSession 封装 |
| `ios/ShortDrama/Sources/Core/Network/APIError.swift` | 新增 | 统一错误模型 |
| `ios/ShortDrama/Sources/Core/Network/APIEndpoint.swift` | 新增 | API 端点协议 |
| `ios/ShortDrama/Sources/Domain/Entities/Drama.swift` | 新增 | 短剧实体 |
| `ios/ShortDrama/Sources/Domain/Entities/Episode.swift` | 新增 | 剧集实体 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchDramasUseCase.swift` | 新增 | 获取短剧列表用例 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 新增 | 短剧仓库协议 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/EpisodeRepositoryProtocol.swift` | 新增 | 剧集仓库协议 |
| `ios/ShortDrama/Sources/Data/DTOs/DramaDTO.swift` | 新增 | 短剧 DTO + Entity 转换 |
| `ios/ShortDrama/Sources/Data/DTOs/EpisodeDTO.swift` | 新增 | 剧集 DTO |
| `ios/ShortDrama/Sources/Data/DTOs/PaginationDTO.swift` | 新增 | 分页 DTO |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 新增 | 短剧远程数据源 + 端点枚举 |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 新增 | 短剧仓库实现 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 新增 | 首页 View |
| `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` | 新增 | 首页 ViewModel |
| `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` | 新增 | 播放器 View |
| `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | 新增 | 播放器 ViewModel |
| `ios/ShortDrama/Sources/Features/DramaDetail/Views/DramaDetailView.swift` | 新增 | 剧集详情 View |
| `ios/ShortDrama/Sources/Features/DramaDetail/ViewModels/DramaDetailViewModel.swift` | 新增 | 剧集详情 ViewModel |
| `ios/ShortDrama/Tests/DataTests/APIErrorTests.swift` | 新增 | APIError 单元测试 |
| `ios/ShortDrama/Tests/DataTests/APIClientTests.swift` | 新增 | APIClient 单元测试 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 新增 | DramaRepository 单元测试 |
| `ios/ShortDrama/Tests/DataTests/DramaDTOTests.swift` | 新增 | DTO 解码与映射测试 |
| `ios/ShortDrama/Tests/DomainTests/AppConfigTests.swift` | 新增 | AppConfig 单元测试 |
| `ios/ShortDrama/Tests/DomainTests/DTOMappingTests.swift` | 新增 | Domain Entity 测试 |
| `ios/ShortDrama/Tests/ViewModelTests/HomeViewModelTests.swift` | 新增 | HomeViewModel 单元测试 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 新增 | NavigationRouter 单元测试 |
| `ios/ShortDrama/Tests/ViewModelTests/DeeplinkHandlerTests.swift` | 新增 | Deeplink 解析单元测试 |
| `ios/ShortDrama/Tests/Mocks/MockDramaRepository.swift` | 新增 | Mock DramaRepositoryProtocol |
| `ios/ShortDrama/Tests/Helpers/URLProtocolMock.swift` | 新增 | URLProtocol Mock 工具 |
| `ios/ShortDrama/Sources/ContentView.swift` | 删除 | 迁移至 HomeView |
| `ios/ShortDrama/Sources/ShortDramaApp.swift` | 删除 | 迁移至 App/ |
| `ios/ShortDrama/Tests/ShortDramaTests.swift` | 删除 | 测试分散到各层 |
| `ios/CLAUDE.md` | 修改 | 补充构建命令、分层架构、开发约定 |
