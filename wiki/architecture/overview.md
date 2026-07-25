# 系统总览 架构文档

> 最后更新：2026-07-25

## 概述

项目是一个多端短剧内容应用的 harness 仓库，覆盖 Web、Android、iOS 三端界面与 Backend 服务端骨架。本期 `PRD-01 底部导航与应用路由` 之后，系统的关键变化是：移动端应用壳已从单页占位结构演进为 5 个一级频道的导航容器，Web 端补齐与频道规划一致的路由骨架，Backend 继续保持不新增接口的稳定状态。

- **产品信息来源**：`PRODUCT.md`
- **仓库结构**：monorepo，按 `web/`、`android/`、`ios/`、`backend/` 分目录维护
- **技术标识**：Android/iOS 继续使用 `com.djs66256.short_drama`，移动端 deeplink scheme 为 `djsdrama://`（`android/app/src/main/AndroidManifest.xml:18-27`，iOS scheme 来自 `project.yml`）
- **当前版本**：各端骨架版本仍为 `0.1.0`，但导航承载能力已明显前移

## 架构设计

### 整体架构

```text
┌──────────────────────────────────────────────────────────────┐
│                        用户界面层                              │
├──────────────┬────────────────────────┬───────────────────────┤
│   Web 前端   │      Android App       │       iOS App         │
│ Next.js 16   │ Kotlin + Compose       │ SwiftUI               │
│ App Router   │ Navigation Compose     │ TabView + NavStack    │
└──────┬───────┴────────────┬───────────┴────────────┬──────────┘
       │                    │                        │
       │   页面语义对齐      │  Navigation Contract   │
       │  `/play` `/detail` │  5 Tab + deeplink      │
       ▼                    ▼                        ▼
┌──────────────────────────────────────────────────────────────┐
│                    Backend API 服务层                          │
│  Next.js App Router Route Handlers                           │
│  ├── /api/health                 已实现                       │
│  └── /api/player/start|stop      501 占位                     │
└──────────────────────────────────────────────────────────────┘
```

### 当前导航承载结构

| 端 | 一级容器 | 二级路由承载 | 当前状态 |
|----|---------|-------------|---------|
| Web | Next.js App Router 页面树 | `/play/[id]`、`/detail/[id]`、`/search`、`/rankings`、`/mall` | 已补齐路由骨架，无底部 Tab UI |
| Android | `Scaffold` + `NavigationBar` + nested `NavHost` | 首页 graph 内承载播放页/详情页；其余频道为占位 graph | 已实现 5 Tab + 多 back stack |
| iOS | `TabView` + per-tab `NavigationStack` | `home` Tab 内承载播放页/详情页；其余频道为占位页 | 已实现 5 Tab + 每 Tab 独立 `NavigationPath` |
| Backend | Route Handlers | 不参与导航承载 | 本期无变更 |

### 核心流程调用栈

#### 流程：移动端冷启动后消费 deeplink

```text
Android
1. MainActivity.handleDeepLink(intent)
2. DeeplinkRouteParser.parse(...)
3. MainNavigationViewModel.enqueuePendingRoute(...)
4. NavGraph LaunchedEffect(uiState.pendingRoute)
5. navController.navigate(...)

iOS
1. ShortDramaApp.onOpenURL
2. DeeplinkHandler.handleDeepLink(url)
3. NavigationRouter.enqueueDeepLink(...) / navigate(...)
4. AppShellView.task -> markContainerReady()
5. TabNavigationHostView NavigationStack 呈现目标页
```

| 调用层级 | 平台 | 文件 | 职责 |
|---------|------|------|------|
| 入口 | Android | `android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt:24-53` | 接收冷启动/复用 Activity 的 deeplink |
| 1 | Android | `android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt:11-45` | 解析 scheme/host/path，并兼容 `player` -> `play` |
| 2 | Android | `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt:22-38` | 维护待执行路由状态 |
| 3 | Android | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:45-75` | 在容器 ready 后执行导航 |
| 入口 | iOS | `ios/ShortDrama/Sources/App/ShortDramaApp.swift:11-20` | 接收 URL Scheme 唤起 |
| 1 | iOS | `ios/ShortDrama/Sources/App/DeeplinkHandler.swift:13-41` | 解析 `open` / `play` / `drama` |
| 2 | iOS | `ios/ShortDrama/Sources/App/NavigationRouter.swift:39-50` | 在容器未 ready 时排队 deeplink |
| 3 | iOS | `ios/ShortDrama/Sources/App/AppShellView.swift:15-18` | 标记容器 ready 并触发待执行导航 |

### 设计决策

| 决策 | 原因 | 影响 |
|------|------|------|
| 移动端统一采用 5 个一级频道 | 为首页、剧场、商城、赚钱、我的提供稳定承载入口 | 后续功能 PRD 默认挂载到既有频道容器，而不是新增顶级入口 |
| 公开路由统一使用 `play` / `detail` | 避免多端命名分叉，便于文档、测试与 deeplink 对齐 | Android 需额外兼容历史 `player` 别名 |
| Android 使用 nested navigation + `saveState/restoreState` | 在单 Activity 架构下保留多 Tab 返回栈 | 切换 Tab 时不必重建全部页面上下文 |
| iOS 使用 `TabView` + per-tab `NavigationStack` | 让每个 Tab 拥有独立导航路径，与 Android 多 back stack 语义对齐 | `NavigationRouter` 成为跨 Tab 状态的唯一来源 |
| Web 仅补齐路由骨架，不做底部 Tab 视觉实现 | 当前 Web 端目标是承载页面语义与 SSR 路由，而非复刻移动端 UI | 后续如需 Web 导航 UI，可在既有路由骨架上增量演进 |
| 本期不新增 Backend API | 需求只涉及客户端导航承载 | 播放器相关接口文档必须明确标注为 501 占位，不得误写成已实现 |

## 跨端涉及

| 端 | 相关模块/文件 | 说明 |
|----|-------------|------|
| Web | `web/src/app/layout.tsx`, `web/src/app/play/[id]/page.tsx`, `web/src/app/detail/[id]/page.tsx`, `web/src/app/search/page.tsx`, `web/src/app/rankings/page.tsx`, `web/src/app/mall/page.tsx`, `web/src/features/home/HomeScreen.tsx` | SSR-first 页面树，补齐导航骨架所需路由 |
| Android | `android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt`, `navigation/AppDestination.kt`, `navigation/DeeplinkRouteParser.kt`, `navigation/MainNavigationViewModel.kt`, `navigation/NavGraph.kt` | 单 Activity + Compose Navigation，已具备 5 Tab、多 back stack、deeplink 排队消费 |
| iOS | `ios/ShortDrama/Sources/App/ShortDramaApp.swift`, `AppShellView.swift`, `AppTab.swift`, `AppRoute.swift`, `NavigationRouter.swift`, `TabNavigationHostView.swift` | SwiftUI 应用壳已切换到 `TabView` + 每 Tab 独立 `NavigationStack` |
| Backend | `backend/src/app/api/health/route.ts`, `backend/src/app/api/player/start/route.ts`, `backend/src/app/api/player/stop/route.ts` | 健康检查保留，播放器接口仍为占位实现 |

## 技术栈总览

| 层级 | Web | Backend | Android | iOS |
|------|-----|---------|---------|-----|
| 语言 | TypeScript | TypeScript | Kotlin 2.0.21 | Swift 6 |
| UI / 路由框架 | React 19 + Next.js 16 App Router | Next.js 16 Route Handlers | Jetpack Compose + Material3 + Navigation Compose | SwiftUI + TabView + NavigationStack |
| 状态管理 | 路由参数 + React 组件状态 | Route Handler 层无长期导航状态 | `StateFlow` + `NavController` | `ObservableObject` + `@Published` |
| 构建工具 | next build | next build | AGP 8.7.0 + Gradle | XcodeGen + Xcode 27 |
| 测试 | Vitest + Testing Library | Vitest [待确认] | JUnit4 + Turbine | Swift Testing |
| 导航契约 | `/play/[id]`、`/detail/[id]` 等页面语义 | 不直接参与 | `play` canonical + `player` 兼容 | `play` canonical |

## 已知限制

- Web 端当前未实现与移动端对等的底部导航 UI，只提供页面骨架和 canonical route。
- Android 与 iOS 的剧场、商城、赚钱、我的仍是占位页，真实业务会在后续 PRD 接入。
- 播放页与详情页跨端都还是占位实现，仅展示路由参数，不包含真实业务数据。
- Backend 尚未提供播放器真实能力，`POST /api/player/start` 与 `POST /api/player/stop` 仍返回 501。
- 设备级黑盒验证未自动执行，当前跨端结论主要来自代码、构建与自动化测试（见 `docs/specs/2026-07-25-prd-01-bottom-nav/qa-test.md:22-25,73-79,173-201,241-269`）。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-25 | 更新：系统总览同步 PRD-01 导航骨架落地结果，修正移动端从单页骨架到 5 Tab 容器的架构描述，并补充 Web 路由骨架与 Backend 不变更说明 |
| 2026-07-22 | 从各端代码提取信息，初始创建 |

---
*本文档由 llm-wiki skill 自动维护。*
