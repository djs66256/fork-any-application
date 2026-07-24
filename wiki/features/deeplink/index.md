# 深链 (Deeplink)

> 最后更新：2026-07-24

## 功能概述

通过自定义 URL Scheme 实现外部唤起应用的能力。iOS 端已声明 URL Scheme 并实现 Deeplink 路由解析和分发逻辑；Android 端已声明 Deep Links intent-filter，路由解析为骨架。统一使用 `djsdrama://` 协议。

- **覆盖端**：iOS（完整实现）、Android（声明 + 骨架路由）、Web（不涉及）
- **核心价值**：支持外部链接唤起应用，为分享、推送通知落地页等功能奠定基础

## 入口与路由

### iOS
- URL Scheme声明：`ios/project.yml` → Info.plist（CFBundleURLSchemes: `["djsdrama"]`)
- Deeplink 入口：`ios/ShortDrama/Sources/App/ShortDramaApp.swift:25-28`（`.onOpenURL` 修饰符）
- 解析器：`ios/ShortDrama/Sources/App/DeeplinkHandler.swift`（`DeeplinkHandler.handleDeepLink(_:)`）
- 路由枚举：`AppRoute`（.home / .player(videoId:) / .dramaDetail(dramaId:)）
- 路由处理：`NavigationRouter`（`ios/ShortDrama/Sources/App/NavigationRouter.swift:1`）管理 NavigationPath

### Android
- Deep Links 声明：`android/app/src/main/AndroidManifest.xml:22-27`
- Scheme：`djsdrama://`
- 路由处理：骨架阶段，在 `MainActivity` 中解析 `intent.data`
- Application：`android/app/src/main/java/com/djs66256/short_drama/ShortDramaApplication.kt:1`

## 核心逻辑

### Deeplink 格式

| URL 格式 | 含义 | 状态 |
|---------|------|------|
| `djsdrama://open` | 通用唤起（打开首页） | ✅ 已实现 |
| `djsdrama://play/{videoId}` | 打开指定视频播放页 | ✅ iOS 已实现 / Android 骨架 |
| `djsdrama://drama/{dramaId}` | 打开指定剧集详情页 | ✅ iOS 已实现 / Android 骨架 |

### iOS Deeplink 流程

1. 外部 App 或浏览器打开 `djsdrama://` 链接
2. iOS 系统调用 `ShortDramaApp.onOpenURL`
3. `DeeplinkHandler.handleDeepLink(url)` 解析 URL → 返回 `AppRoute?`
4. `router.navigate(to: route)` 推入 NavigationStack
5. `NavigationStack.navigationDestination` 根据 `AppRoute` 渲染对应 View

## 多端实现

### iOS
- 配置：`ios/project.yml` → CFBundleURLSchemes
- 源文件：
  - `ios/ShortDrama/Sources/App/ShortDramaApp.swift:25-28` — onOpenURL 入口
  - `ios/ShortDrama/Sources/App/DeeplinkHandler.swift` — URL 解析
  - `ios/ShortDrama/Sources/App/AppRoute.swift` — 路由枚举
  - `ios/ShortDrama/Sources/App/NavigationRouter.swift:1-25` — 导航状态管理

### Android
- 配置：`android/app/src/main/AndroidManifest.xml:22-27` — intent-filter
- scheme: `djsdrama`
- launchMode: `singleTask`（避免重复创建 Activity）
- 源文件：`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt` — URL 解析（骨架）
- 后续 PRD 补齐 Compose Navigation 路由分发

### Web
- 不涉及（Web 端使用标准 HTTPS URL 路由）

## 配置参考

| 端 | Scheme | 配置位置 | 状态 |
|----|--------|---------|------|
| iOS | `djsdrama://` | project.yml → Info.plist | ✅ Scheme 已声明 + 路由解析已实现 |
| Android | `djsdrama://` | AndroidManifest.xml intent-filter | ✅ Scheme 已声明 + 路由解析骨架 |
| Web | N/A | 使用 HTTPS 路由 | — |

## 依赖关系

- iOS：依赖 `NavigationRouter`（导航管理）、`DeeplinkHandler`（URL 解析）、`AppRoute`（路由枚举）
- Android：依赖 `MainActivity` + Compose Navigation

## 已知限制

- Android 端路径解析逻辑尚未实现（骨架阶段）
- 未配置 Universal Links（iOS）/ App Links（Android）
- 无 deeplink 端到端测试覆盖

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-24 | 扩展：iOS 端实现完整 Deeplink 路由解析和分发（DeeplinkHandler + NavigationRouter + AppRoute）；Android 端声明 Deep Links intent-filter；统一 djsdrama:// 协议格式 |
| 2026-07-22 | 初始创建：仅 iOS 声明 URL Scheme，无路由处理逻辑 |

---

*本文档由 llm-wiki skill 自动维护。*
