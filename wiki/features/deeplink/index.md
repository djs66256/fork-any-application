# 深链 (Deeplink)

> 最后更新：2026-07-25

## 功能概述

通过自定义 URL Scheme `djsdrama://` 实现外部唤起应用并落到应用壳内部路由的能力。当前 iOS 与 Android 都已接入 deeplink 解析；其中 iOS 直接支持 `open` / `play` / `drama` 三类 host，Android 在此基础上额外兼容历史 `player` host，并统一映射到 canonical `play` 语义。Web 不参与 `djsdrama://` 协议处理。

- **覆盖端**：iOS、Android
- **核心价值**：让外部入口可在多 Tab 应用壳下仍稳定打开首页、播放页与详情页
- **当前状态**：多端解析与导航承载已接入，设备级外部唤起黑盒验证仍待补测

## 入口与路由

### iOS
- URL Scheme 声明：`ios/project.yml` → Info.plist（`djsdrama`）
- Deeplink 入口：`ios/ShortDrama/Sources/App/ShortDramaApp.swift:11-20`（`.onOpenURL`）
- 解析器：`ios/ShortDrama/Sources/App/DeeplinkHandler.swift:13-41`
- 路由目标：`AppRoute.home`、`AppRoute.player(videoId:)`、`AppRoute.dramaDetail(dramaId:)`（`ios/ShortDrama/Sources/App/AppRoute.swift:4-29`）
- 导航承载：`NavigationRouter` + `TabView` / `NavigationStack`（`ios/ShortDrama/Sources/App/NavigationRouter.swift:5-64`、`ios/ShortDrama/Sources/App/AppShellView.swift:6-18`）

### Android
- Deep Links 声明：`android/app/src/main/AndroidManifest.xml:18-27`
- 入口 Activity：`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt:24-53`
- 解析器：`android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt:6-47`
- 中间态：`PendingRoute.Home` / `PendingRoute.Play` / `PendingRoute.Detail`（`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:79-83`）
- 导航承载：`NavGraph` 在 `LaunchedEffect(uiState.pendingRoute)` 中消费待执行路由（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:45-75`）

### Web
- 不处理 `djsdrama://` Scheme
- Web 对应的公开页面语义由 `/play/[id]` 与 `/detail/[id]` 文件系统路由承载（`web/src/app/play/[id]/page.tsx:14-39`、`web/src/app/detail/[id]/page.tsx:14-39`）

## 核心逻辑

### Deeplink 格式

| URL 格式 | 含义 | iOS | Android |
|---------|------|-----|---------|
| `djsdrama://open` | 打开首页 | ✅ | ✅ |
| `djsdrama://play/{videoId}` | 打开播放页 | ✅ | ✅ |
| `djsdrama://player/{videoId}` | 历史播放页别名 | ❌ 不支持 | ✅ 兼容并映射为 `play` |
| `djsdrama://drama/{dramaId}` | 打开详情页 | ✅ | ✅ |

### iOS Deeplink 流程

1. 外部入口触发 `ShortDramaApp.onOpenURL`（`ios/ShortDrama/Sources/App/ShortDramaApp.swift:13-20`）。
2. `DeeplinkHandler.handleDeepLink(_:)` 校验 scheme 必须为 `djsdrama`，并按 host 解析到 `AppRoute`（`ios/ShortDrama/Sources/App/DeeplinkHandler.swift:13-41`）。
3. 如果导航容器已 ready，则直接 `router.navigate(to: route)`；否则先 `router.enqueueDeepLink(route)` 缓存（`ios/ShortDrama/Sources/App/ShortDramaApp.swift:13-20`）。
4. `AppShellView.task` 调用 `router.markContainerReady()` 后，会自动消费 `pendingRoute` 并落到对应 Tab / 子页面（`ios/ShortDrama/Sources/App/AppShellView.swift:15-18`、`ios/ShortDrama/Sources/App/NavigationRouter.swift:39-50`）。

### Android Deeplink 流程

1. `MainActivity` 在 `onCreate()` 和 `onNewIntent()` 都调用 `handleDeepLink(intent)`，保证冷启动和单任务复用都能接收 deeplink（`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt:24-53`）。
2. `DeeplinkRouteParser.parse(...)` 先校验 scheme，再读取 host/path segment，并把 `play` 与历史 `player` 都映射为 `PendingRoute.Play`（`android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt:11-45`）。
3. 解析结果先写入 `MainNavigationViewModel.pendingRoute`（`android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt:14-38`）。
4. `NavGraph` 监听 `pendingRoute`，在容器已可导航时执行 `navController.navigate(...)` 并消费队列；非法空参数会写入 `NavigationErrorCode.INVALID_ROUTE_PARAMS`（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:45-75`）。

### 边界与异常处理

| 场景 | 处理方式 | 代码依据 |
|------|---------|---------|
| 非 `djsdrama` scheme | 直接返回 `nil` / `null`，不进入导航 | `ios/ShortDrama/Sources/App/DeeplinkHandler.swift:14-16`；`android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt:17-20` |
| 未知 host | 返回 `nil` / `null`，安全降级为不处理 | `ios/ShortDrama/Sources/App/DeeplinkHandler.swift:23-39`；`android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt:28-45` |
| 空 `play` / `drama` 参数 | 返回 `nil` / `null`，不进入有效子页 | `ios/ShortDrama/Sources/App/DeeplinkHandler.swift:26-37`；`android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt:30-42` |
| 冷启动容器未 ready | 先缓存 pending route，容器 ready 后再消费 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:39-50`；`android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt:22-38` + `NavGraph.kt:45-75` |

## 多端实现

### iOS
- Scheme 声明：`ios/project.yml`
- 入口：`ios/ShortDrama/Sources/App/ShortDramaApp.swift:11-20`
- 解析器：`ios/ShortDrama/Sources/App/DeeplinkHandler.swift:13-41`
- 路由与状态：`ios/ShortDrama/Sources/App/AppRoute.swift:4-29`、`ios/ShortDrama/Sources/App/NavigationRouter.swift:5-64`
- 自动化证据：`ios/ShortDrama/Tests/ViewModelTests/DeeplinkHandlerTests.swift:7-55`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift:63-74`

### Android
- Scheme 与入口：`android/app/src/main/AndroidManifest.xml:18-27`、`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt:24-53`
- 解析器：`android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt:6-47`
- 待执行路由状态：`android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt:14-38`
- 导航消费：`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:45-75`
- 自动化证据：`android/app/src/test/java/com/djs66256/short_drama/navigation/DeeplinkRouteParserTest.kt:9-49`

### Web
- 不实现自定义 Scheme 解析。
- 公开页面语义与移动端 canonical naming 对齐：`play` / `detail`（`web/src/app/play/[id]/page.tsx:14-39`、`web/src/app/detail/[id]/page.tsx:14-39`）。

## API 引用

本功能不新增 RESTful API。deeplink 解析和路由分发均在端内完成。

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| iOS `pendingRoute` | `@Published private(set) var pendingRoute` | 应用级 | 冷启动时先缓存 deeplink，再等容器 ready 消费 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:11,39-50` |
| iOS `containerReady` | `@Published private(set) var containerReady` | 应用级 | 标识 `TabView` / `NavigationStack` 是否已完成承载准备 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:12,43-50` |
| Android `pendingRoute` | `MutableStateFlow<UiState>` | 应用级 | 用于跨 `MainActivity` 入口与 `NavGraph` 消费链路传递 deeplink 目标 | `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt:14-38` |
| Android `lastRejectedReason` | `MutableStateFlow<UiState>` | 应用级 | 记录非法参数等被拒绝的原因码 | `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt:14-38` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 应用壳 | 导航承载 | deeplink 目标最终依赖 App Shell 的 Tab 容器与子路由注册 |
| 播放器 | 路由目标 | `play` / `player` deeplink 最终映射到播放页占位路由 |
| 剧集详情 | 路由目标 | `drama` deeplink 最终映射到详情页占位路由 |

### 外部依赖

| 服务 / 框架 | 用途 | 接入方式 |
|-------------|------|---------|
| iOS URL Scheme | 系统级唤起 | Info.plist / `onOpenURL` |
| Android intent-filter | 系统级唤起 | `AndroidManifest.xml` + `launchMode="singleTask"` |
| Navigation Compose / SwiftUI Navigation | 子页面落地 | 端内导航容器消费解析结果 |

## 已知限制

- iOS 当前不兼容 `djsdrama://player/{id}` 历史 host；仅 Android 做了 legacy alias 兼容。
- Web 不处理 `djsdrama://` Scheme，只负责 canonical 页面语义。
- 当前自动化验证覆盖了解析与状态流转，但尚未完成真实设备/模拟器上的外部 App 唤起黑盒测试（见 `docs/specs/2026-07-25-prd-01-bottom-nav/qa-test.md:22-25,163-179,229-247`）。
- 尚未接入 Universal Links（iOS）或 App Links（Android）。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-25 | 更新：Android deeplink 从骨架状态修正为已接入解析+待执行路由消费，补充 `player` 别名兼容、容器未就绪排队与跨端 canonical naming 说明 |
| 2026-07-24 | 扩展：iOS 端实现 Deeplink 路由解析和分发，Android 端声明 Deep Links intent-filter |
| 2026-07-22 | 初始创建：仅 iOS 声明 URL Scheme，无路由处理逻辑 |

---
*本文档由 llm-wiki skill 自动维护。*
