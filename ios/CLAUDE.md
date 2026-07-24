## 端说明

iOS 端代码与说明统一维护在当前目录。
如无额外说明，当前目录仅承载 iOS 相关实现，不处理其他端逻辑。

## 技术约束

- 使用 Swift 6 与 SwiftUI 构建界面。
- 项目配置通过 XcodeGen（`project.yml`）管理，不直接修改 `.xcodeproj/project.pbxproj`。
- 优先采用声明式 UI 组织方式，避免将复杂业务逻辑直接堆叠在 View 中。
- 界面状态应保持清晰、可追踪、可推导，便于预览、测试与迭代。
- 网络层使用 URLSession 原生实现，不引入第三方网络库（如 Alamofire）。
- 路由通过 NavigationStack + djsdrama:// URL Scheme 实现 Deeplink。

## 架构约束

- 采用 MVVM 架构模式，配合 Clean Architecture 三层结构：Core → Domain → Data → Presentation。
- 分层职责如下：
  - **Core 层**：Foundation 级基础设施，如网络客户端（APIClient）、错误模型（APIError）、App 配置（AppConfig）、设计 tokens（DesignTokens）。不依赖任何其他层，不引用 SwiftUI/UIKit。
  - **Domain 层**：纯 Swift 业务实体（Entities）、仓库协议（RepositoryProtocols）、用例（UseCases）。零框架依赖，不引用 SwiftUI/UIKit。
  - **Data 层**：Domain 协议的具体实现，包含 DTO（Data Transfer Object）及其与 Entity 的映射、远程数据源（RemoteDataSource）、仓库实现（Repository）。依赖 Core 层的 Network 和 Domain 层的 Protocols。
  - **Presentation 层**：SwiftUI Views 和 ViewModels，按功能模块组织在 `Features/` 目录下。ViewModel 使用 @MainActor ObservableObject，不持有 View 引用。
- App 入口在 `Sources/App/ShortDramaApp.swift`，管理 NavigationStack 和路由注入。
- ViewModel 依赖 UseCase（或 RepositoryProtocol），不直接依赖 Data 层具体实现。
- 优先保证核心场景可通过自动化方式验证，而不是依赖人工点击验证。

## 测试要求

- 每个场景都需要有单元测试。
- 新增业务逻辑时，应同步补齐对应测试。
- 如某个场景暂时无法直接测试，需要先说明原因，再决定实现方式。
- 测试使用 Swift Testing 框架（`import Testing`，`@Test` 宏，`#expect` 断言）。
- 网络测试使用 URLProtocol 子类 mock，不发起真实网络请求。

## 命令约定

### 项目生成

```bash
cd ios && xcodegen generate
```

### 构建

```bash
cd ios && xcodebuild -project ShortDrama.xcodeproj \
  -scheme ShortDrama build \
  -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'
```

### 测试

```bash
cd ios && xcodebuild -project ShortDrama.xcodeproj \
  -scheme ShortDrama test \
  -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'
```

### Lint

```bash
cd ios && swiftlint lint
```

注意：SwiftLint 通过 `project.yml` 中的 preBuildScript 在每次构建时自动运行（仅当 swiftlint 已安装时）。

## 目录结构

```
ios/
├── CLAUDE.md                 # 本文档
├── .swiftlint.yml            # SwiftLint 规则配置
├── project.yml               # XcodeGen 项目定义
├── Configs/
│   ├── Debug.xcconfig        # Debug 构建配置（含 API_BASE_URL）
│   └── Release.xcconfig      # Release 构建配置（含 API_BASE_URL）
├── ShortDrama.xcodeproj/     # 由 xcodegen generate 生成，不手动修改
└── ShortDrama/
    ├── Resources/            # 资源文件（Info.plist, Assets.xcassets 等）
    ├── Sources/
    │   ├── App/              # 入口点 + 路由 + Deeplink
    │   ├── Core/             # 基础设施层
    │   │   ├── Config/       # AppConfig
    │   │   ├── DesignSystem/ # DesignTokens
    │   │   ├── Extensions/   # View+Extensions
    │   │   └── Network/      # APIClient, APIError, APIEndpoint
    │   ├── Domain/           # 业务领域层（纯 Swift，零框架依赖）
    │   │   ├── Entities/
    │   │   ├── RepositoryProtocols/
    │   │   └── UseCases/
    │   ├── Data/             # 数据层（DTO、远程数据源、仓库实现）
    │   │   ├── DTOs/
    │   │   ├── DataSources/
    │   │   └── Repositories/
    │   └── Features/         # 表现层（View + ViewModel）
    │       ├── Home/
    │       ├── Player/
    │       └── DramaDetail/
    └── Tests/                # 单元测试
        ├── Helpers/          # 测试辅助（URLProtocolMock 等）
        ├── Mocks/            # Mock 实现
        ├── DataTests/        # Data 层测试
        ├── DomainTests/      # Domain 层测试
        └── ViewModelTests/   # ViewModel 测试
```

## 开发约定

- 仅修改 `ios/` 目录下的文件。
- iOS 端如需依赖后端接口，对接方式需遵循仓库根目录中的 RESTful 约束。
- 禁止硬编码环境地址、token、开关或其他环境相关常量，应通过配置或统一常量管理。
- API base URL 从 Info.plist 的 `API_BASE_URL` 读取（由 xcconfig 注入），通过 `AppConfig.apiBaseURL` 访问。
- 新增源文件后需运行 `xcodegen generate` 重新生成项目（因为使用通配符 sources path `**/*.swift`，文件会自动被包含）。
- 代码中引用产品名时统一使用 `AppConfig.appName`，不硬编码 "ShortDrama"。
- SwiftLint 配置在 `.swiftlint.yml` 中，构建时自动运行。
