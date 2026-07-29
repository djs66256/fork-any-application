# 代码 Review：iOS — PRD-13 商城

> Review 日期：2026-07-29

## 审查结果

| 维度 | 状态 | 说明 |
|------|------|------|
| 实现与 design 一致性 | ✅ | mall 一级 tab 已切换为 `MallContainerView`，覆盖商城容器、搜索桥接、登录承接、登录态同步与上下文恢复闭环。 |
| 无硬编码常量 | ✅ | 商城 H5 基址通过 `project.yml` + `xcconfig` 注入到 `Info.plist`，Swift 代码统一通过 `AppConfig.mallBaseURL()` / `AppConfig.mallHomeURL()` 读取。 |
| 代码风格符合平台规范 | ✅ | `xcodegen generate`、`xcodebuild build`、`xcodebuild test` 均通过；`swiftlint lint` 仅报告仓库既有 warning，0 serious。 |
| 错误处理完备 | ✅ | 容器首屏加载失败可重试；非法 bridge payload、重复登录请求、容器重建、登录/搜索返回均有受控分支。 |
| 路由与宿主通信闭环 | ✅ | `NavigationRouter` 已接入 mall 搜索/登录返回恢复，`MallContainerViewModel` 已处理 `mall.openSearch` / `mall.requestLogin` 与宿主消息回送。 |
| 配置管理符合端约束 | ✅ | `MALL_BASE_URL` 仅通过 XcodeGen 管理，`ShortDrama.xcodeproj/project.pbxproj` 的相关改动已由 `xcodegen generate` 生成确认，并非手工源头。 |
| 所有测试通过 | ✅ | `xcodebuild test` 通过，`TEST SUCCEEDED`。 |
| Build 成功 | ✅ | `xcodebuild build` 通过，`BUILD SUCCEEDED`。 |

## 变更文件审查

| 文件 | 审查结果 | 问题数 |
|------|---------|--------|
| `ios/project.yml` | ✅ | 0 |
| `ios/Configs/Debug.xcconfig` | ✅ | 0 |
| `ios/Configs/Release.xcconfig` | ✅ | 0 |
| `ios/ShortDrama.xcodeproj/project.pbxproj` | ✅ | 0 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Core/Config/AppConfig.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Domain/Entities/MallLoginContext.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/Mall/**/*.swift` | ✅ | 0 |
| `ios/ShortDrama/Tests/ViewModelTests/MallContainerViewModelTests.swift` | ✅ | 0 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | ✅ | 0 |
| `ios/ShortDrama/Tests/DomainTests/AppConfigTests.swift` | ✅ | 0 |
| `ios/ShortDrama/Tests/DataTests/APIClientTests.swift` | ✅ | 0 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | ✅ | 0 |
| `ios/ShortDrama/Tests/DataTests/MenuPanelRepositoryTests.swift` | ✅ | 0 |
| `ios/ShortDrama/Tests/DataTests/PlayerRemoteDataSourceTests.swift` | ✅ | 0 |
| `ios/ShortDrama/Tests/DataTests/PlayerRepositoryTests.swift` | ✅ | 0 |

## 发现的问题

本轮收口过程中发现 1 个回归问题，已修复：

| 问题编号 | 摘要 | 状态 | 说明 |
|---------|------|------|------|
| #1 | `APIClient(session:)` 测试初始化调用与新增 `baseURL` 参数签名不一致，导致多组 DataTests 编译失败 | ✅ 已修复 | 已将相关测试改为显式传入 `baseURL: "https://api.example.com"`，并重新验证 test/build 均通过。 |

## 修复记录

| 轮次 | 修复项 |
|------|--------|
| 1 | 按 `project.yml` 注入 `MALL_BASE_URL`，并通过 `AppConfig.mallBaseURL()` / `mallHomeURL()` 暴露配置读取能力。 |
| 1 | 将 mall tab 根视图从占位页替换为 `MallContainerView()`，接入商城 H5 容器。 |
| 1 | 实现 `MallContainerViewModel`、`MallContainerState`、`MallWebView` 与 `MallContainerStateView`，补齐容器加载/失败/重试状态机。 |
| 1 | 实现 `MallBridgeMessage` 与 `MallLoginContext`，接入 `mall.openSearch`、`mall.requestLogin`、`mall.syncAuthState`、`mall.restoreContext`。 |
| 1 | 扩展 `NavigationRouter`，完成搜索返回、登录返回与容器重建时的商城上下文恢复。 |
| 1 | 修复因 `APIClient` 测试构造器签名变更导致的 `DramaRepositoryTests`、`MenuPanelRepositoryTests`、`PlayerRemoteDataSourceTests`、`PlayerRepositoryTests` 编译失败。 |
| 1 | 执行 `xcodegen generate` 重新生成工程，确认 `project.pbxproj` 中 `INFOPLIST_KEY_MALL_BASE_URL` 与 `MallLoginContext.swift` 等条目均为生成产物。 |

## 验证记录

| 命令 | 结果 | 说明 |
|------|------|------|
| `cd ios && xcodegen generate` | ✅ 通过 | 工程成功重生成。 |
| `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` | ✅ 通过 | `TEST SUCCEEDED`。 |
| `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` | ✅ 通过 | `BUILD SUCCEEDED`。 |
| `cd ios && swiftlint lint` | ✅ 通过 | 仅有仓库既有 warning，0 serious。 |

## 遗留问题（需人工决策）

无。

## 结论

- [x] ✅ 所有问题已修复，代码质量合格
- [ ] ⚠️ 存在遗留问题，需人工确认
