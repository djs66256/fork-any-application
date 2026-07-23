---
name: ios-development
description: >
  iOS 端技术方案设计与编码规范。
  凡涉及 iOS 端的技术方案设计、功能开发、代码修改、架构决策、测试编写、
  构建配置等，都必须在第一时间加载本 skill。
  触发场景：用户提到"iOS"、"苹果"、"Swift"、"SwiftUI"、"ios/"；
  需求涉及移动端 iOS 实现；iOS 端技术方案评审；iOS 端 bug 修复或功能迭代。
  优先级最高——任何 iOS 相关任务都不允许跳过本 skill。
---

# iOS Development

> ⚠️ **强制加载**: 任何涉及 iOS 端的任务（方案设计、编码、review、测试）都必须首先加载本 skill。

## 定位

iOS 端是 [PRODUCT.md](../../../PRODUCT.md) 的移动端实现之一，负责 Swift + SwiftUI 工程的全生命周期开发。

## 规范索引

各规范已拆分到独立文件以便按需加载，详见下表：

| 规范 | 文件 | 说明 |
|------|------|------|
| **代码规范** | [standards/coding-standards.md](standards/coding-standards.md) | Swift 风格、SwiftUI 规范、资源命名、CR 清单 |
| **架构设计** | [standards/architecture.md](standards/architecture.md) | 分层架构、导航、状态管理、DI、模块化、错误处理 |
| **基础库与基础能力** | [standards/foundation.md](standards/foundation.md) | 网络、图片、持久化、日志、监控、崩溃、i18n、A11y |
| **编译、运行与调试** | [standards/build-and-debug.md](standards/build-and-debug.md) | Xcode、xcodebuild、LLDB、Instruments |
| **AI 操作与自动化** | [standards/ai-operations.md](standards/ai-operations.md) | simctl、XCUITest、截图、日志采集、真机调试 |
| **测试规范** | [standards/testing.md](standards/testing.md) | 单元/UI/快照测试、覆盖率 |
| **安全** | [standards/security.md](standards/security.md) | Keychain、SSL Pinning、代码安全 |
| **CI/CD** | [standards/cicd.md](standards/cicd.md) | 流水线、自动打包、TestFlight |
| **开源库选型** | [open-source-libs.md](open-source-libs.md) | 各领域库清单与选型状态 |
| **常见问题** | [faq.md](faq.md) | 常见开发问题与解决方案 |

## 文件结构

```
ios-development/
├── SKILL.md
├── references/
│   ├── open-source-libs.md
│   ├── faq.md
│   └── standards/
│       ├── coding-standards.md
│       ├── architecture.md
│       ├── foundation.md
│       ├── build-and-debug.md
│       ├── ai-operations.md
│       ├── testing.md
│       ├── security.md
│       └── cicd.md
├── assets/
└── scripts/
```

## 参考

- [iOS CLAUDE.md](../../../ios/CLAUDE.md) — iOS 端工程级约束
- [PRODUCT.md](../../../PRODUCT.md) — 产品信息
