# iOS 初始化规范

## 技术栈

| 组件 | 选型 |
|------|------|
| 语言 | Swift 6 |
| UI 框架 | SwiftUI |
| 最低版本 | iOS 18.0 |
| 构建工具 | XcodeGen + Xcode 27 |
| 包管理 | SPM |
| 架构 | MVVM + Clean Architecture（Presentation → Domain → Data + Core） |
| 网络 | URLSession（原生，不引入第三方） |
| Lint | SwiftLint |
| 测试 | XCTest |

## 标准目录结构

```
ios/ShortDrama/Sources/
├── App/
│   ├── ShortDramaApp.swift        # @main App 入口
│   └── AppDelegate.swift          # Deeplink + ScenePhase 处理
├── Core/                          # Core 层
│   ├── Network/
│   │   └── APIClient.swift        # URLSession 封装
│   ├── Config/
│   │   └── AppConfig.swift        # 版本号、bundleId 等
│   ├── Extensions/
│   └── DesignSystem/
│       └── DesignTokens.swift     # 颜色、间距、字体
├── Domain/                        # Domain 层（纯 Swift，无 UIKit/SwiftUI 依赖）
│   ├── Entities/
│   │   ├── Drama.swift
│   │   └── Episode.swift
│   ├── UseCases/
│   │   └── FetchDramasUseCase.swift  # 骨架
│   └── RepositoryProtocols/
│       ├── DramaRepositoryProtocol.swift
│       └── EpisodeRepositoryProtocol.swift
├── Data/                          # Data 层
│   ├── Repositories/
│   │   └── DramaRepository.swift  # 实现 Protocol
│   ├── DataSources/
│   │   └── DramaRemoteDataSource.swift
│   └── DTOs/
│       └── DramaDTO.swift         # Codable API 模型 + Entity 转换
└── Features/                      # Presentation 层 — 按业务域独立目录
    ├── Home/
    │   ├── Views/
    │   │   └── HomeView.swift
    │   └── ViewModels/
    │       └── HomeViewModel.swift
    ├── Player/                    # 骨架
    │   ├── Views/
    │   │   └── PlayerView.swift
    │   └── ViewModels/
    │       └── PlayerViewModel.swift
    └── DramaDetail/               # 骨架
        ├── Views/
        │   └── DramaDetailView.swift
        └── ViewModels/
            └── DramaDetailViewModel.swift
```

## project.yml（XcodeGen）

- target：ShortDrama
- bundleId：引用 PRODUCT.md 中的 appId
- deploymentTarget：iOS 18.0
- Swift 6.0
- Debug 自动签名
- preBuildScripts：集成 SwiftLint
- Info.plist：`djsdrama://` URL Scheme（引用 PRODUCT.md 中的 schema）

## 关键约束

- Domain 层零框架依赖（不含 `import UIKit` / `import SwiftUI`）
- ViewModel 通过 `@Published` 暴露状态，不持有 View 引用
- Repository 协议在 Domain 层定义，Data 层实现
- Feature 间不直接引用，通过 Domain 层的 Entity 和 UseCase 通信
- 零第三方依赖（当前阶段全部使用 Apple 原生框架）
- SwiftLint 集成到 Xcode Build Phase
