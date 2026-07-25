# 应用壳 (App Shell)

> 最后更新：2026-07-25

## 功能概述

应用壳负责承载各端应用的启动入口、路由容器与基础页面骨架。当前移动端已从单页占位结构演进为 5 个一级频道的底部导航容器：首页、剧场、商城、赚钱、我的；Web 端维持 SSR-first 的 Next.js App Router 结构，并补齐与本期导航规划对齐的路由骨架；Backend 继续提供服务首页和健康检查入口。

- **覆盖端**：Web、Android、iOS、Backend
- **核心价值**：为后续首页 Feed、剧场、商城、赚钱中心、个人页、播放页和详情页提供统一承载容器
- **当前状态**：跨端导航骨架已落地，业务内容仍以占位实现为主

## 入口与路由

### Web
- 入口组件：`web/src/app/layout.tsx:15-33`（根布局与全局 metadata）+ `web/src/app/page.tsx`（首页）
- 路由方案：Next.js App Router，Page 层仅负责路由委托
- 当前可访问骨架路由：`/`、`/play/[id]`、`/detail/[id]`、`/search`、`/rankings`、`/mall`（见 `web/src/app/play/[id]/page.tsx:14-39`、`web/src/app/detail/[id]/page.tsx:14-39`、`web/src/app/search/page.tsx:1-10`、`web/src/app/rankings/page.tsx:1-10`、`web/src/app/mall/page.tsx:1-10`）

### Backend
- 入口组件：`backend/src/app/layout.tsx` + `backend/src/app/page.tsx`
- 提供 `/` 页面展示服务信息，并保留 `/api/health` 作为健康检查入口

### Android
- 入口 Activity：`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt:21-54`
- Manifest 声明：`android/app/src/main/AndroidManifest.xml:13-27`，同一 `MainActivity` 同时承担 LAUNCHER 与 deeplink 入口
- 当前容器：`NavGraph` 在 `Scaffold` 中挂载 `NavigationBar` + `NavHost`，提供 5 个一级频道 graph（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:77-214`）
- 首页子路由：`play/{videoId}`、`player/{videoId}`、`detail/{dramaId}`、`dramaDetail/{dramaId}`（`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:29-83`）

### iOS
- 入口 App：`ios/ShortDrama/Sources/App/ShortDramaApp.swift:5-23`
- 当前容器：`AppShellView` 使用 `TabView(selection: $router.selectedTab)` 渲染 5 个 Tab（`ios/ShortDrama/Sources/App/AppShellView.swift:3-20`）
- 一级频道：`AppTab` 定义 `home`、`theater`、`mall`、`earn`、`profile`，并绑定标题/图标（`ios/ShortDrama/Sources/App/AppTab.swift:3-41`）
- 首页子路由：`AppRoute.player(videoId:)` 与 `AppRoute.dramaDetail(dramaId:)`，对外公开名称分别为 `play`、`detail`（`ios/ShortDrama/Sources/App/AppRoute.swift:4-29`）

## 核心逻辑

### Web 端
1. `layout.tsx` 提供全局 metadata 和字体注入（`web/src/app/layout.tsx:15-33`）。
2. 首页 `HomeScreen` 展示应用元信息，并提供代表性导航链接到 `/play/sample`、`/detail/sample`、`/search`、`/rankings`、`/mall`（`web/src/features/home/HomeScreen.tsx:12-56`）。
3. 动态路由页在服务端先对 `id` 做 `trim()` + 非空校验，非法参数走 `notFound()`（`web/src/app/play/[id]/page.tsx:9-39`、`web/src/app/detail/[id]/page.tsx:9-39`）。
4. 搜索、榜单、商城页统一复用 `PlaceholderRouteScreen`，只承担路由骨架职责（`web/src/features/placeholder-route/PlaceholderRouteScreen.tsx:3-21`）。

### Backend 端
1. 服务首页继续作为运行信息与入口页，不参与本期导航契约。
2. `/api/health` 继续提供健康检查，不因本期改造发生变化。
3. `/api/player/start` 与 `/api/player/stop` 仍为 501 占位接口，本期未新增后端能力（`backend/src/app/api/player/start/route.ts:1-6`、`backend/src/app/api/player/stop/route.ts:1-6`）。

### Android 端
1. `MainActivity` 在冷启动与 `onNewIntent` 两条路径下统一接收 deeplink，并把解析结果写入 `MainNavigationViewModel` 的 `pendingRoute`（`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt:24-53`）。
2. `NavGraph` 通过 `Scaffold(bottomBar = { NavigationBar { ... }})` 渲染 5 个 Tab，点击时使用 `popUpTo(findStartDestination()) + saveState + restoreState` 保留多 back stack 状态（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:77-109`）。
3. 首页 graph 承载 `HomeScreen`、播放页和详情页；其余频道 graph 均渲染共享 `PlaceholderScreen`，避免同构占位实现重复（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:117-211`）。
4. `LaunchedEffect(uiState.pendingRoute)` 在导航容器 ready 后消费待执行路由，实现冷启动 deeplink 延迟跳转（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:45-75`）。

### iOS 端
1. `ShortDramaApp` 持有单例 `NavigationRouter` 并通过 `.environmentObject(router)` 注入全局导航状态（`ios/ShortDrama/Sources/App/ShortDramaApp.swift:7-23`）。
2. `AppShellView` 以 `TabView` 渲染 5 个一级频道，并在 `.task` 中调用 `router.markContainerReady()` 标记容器可导航（`ios/ShortDrama/Sources/App/AppShellView.swift:6-18`）。
3. `NavigationRouter` 为每个 Tab 维护独立 `NavigationPath`，切换频道时不清空其它频道栈；首页子页 push 到 `home` Tab 的独立路径中（`ios/ShortDrama/Sources/App/NavigationRouter.swift:7-64`）。
4. `TabNavigationHostView` 为每个 Tab 提供独立 `NavigationStack`，首页注册 `HomeView`、`PlayerView`、`DramaDetailView`，其余频道复用 `PlaceholderTabView`（`ios/ShortDrama/Sources/App/TabNavigationHostView.swift:8-32`）。

## 多端实现

### Web
- 源文件：`web/src/app/layout.tsx:15-33`、`web/src/app/play/[id]/page.tsx:14-39`、`web/src/app/detail/[id]/page.tsx:14-39`
- 首页导航入口：`web/src/features/home/HomeScreen.tsx:27-51`
- 占位页复用：`web/src/features/placeholder-route/PlaceholderRouteScreen.tsx:3-21`
- 技术：Next.js 16、React 19、TypeScript、SSR-first App Router

### Android
- 入口与容器：`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt:21-54`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:36-214`
- 路由常量：`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:3-83`
- 首页代表性入口：`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:27-79`
- 占位频道：`android/app/src/main/java/com/djs66256/short_drama/feature/common/ui/PlaceholderScreen.kt:14-38`
- 技术：Kotlin 2.0.21、Jetpack Compose、Material3、Navigation Compose、Hilt

### iOS
- 入口与容器：`ios/ShortDrama/Sources/App/ShortDramaApp.swift:5-23`、`ios/ShortDrama/Sources/App/AppShellView.swift:3-20`
- Tab 定义：`ios/ShortDrama/Sources/App/AppTab.swift:3-41`
- 路由与状态：`ios/ShortDrama/Sources/App/AppRoute.swift:4-29`、`ios/ShortDrama/Sources/App/NavigationRouter.swift:5-64`
- 首页代表性入口：`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:15-61`
- 占位频道：`ios/ShortDrama/Sources/Features/Shell/Views/PlaceholderTabView.swift:3-27`
- 技术：Swift 6、SwiftUI、TabView、NavigationStack、Swift Testing

### Backend
- 服务首页：`backend/src/app/page.tsx`
- 健康检查：`backend/src/app/api/health/route.ts`
- 与导航骨架相关的播放器接口现状：`backend/src/app/api/player/start/route.ts:1-6`、`backend/src/app/api/player/stop/route.ts:1-6`
- 技术：Next.js 16、TypeScript、App Router Route Handlers

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/health` | [api/health.md](../../api/health.md) | 服务健康检查，不受本期导航改造影响 |
| `POST /api/player/start` | [api/player.md](../../api/player.md) | 播放页骨架相关占位接口，当前仍返回 501 |
| `POST /api/player/stop` | [api/player.md](../../api/player.md) | 播放结束上报占位接口，当前仍返回 501 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Web 路由参数 | Next.js `params` | 页面级 | `play` / `detail` 页面按需读取 `id` 并在服务端先校验 | `web/src/app/play/[id]/page.tsx:14-39`, `web/src/app/detail/[id]/page.tsx:14-39` |
| Android `pendingRoute` | `MutableStateFlow<UiState>` | 应用级 | deeplink 先入队，待 `NavHost` 可消费后执行 | `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt:14-38` |
| Android 多 Tab 栈 | `NavController` + `saveState/restoreState` | Tab 级 | 切换频道时保留已访问 graph 的返回栈 | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:87-96` |
| iOS `selectedTab` | `@Published var selectedTab` | 应用级 | 控制当前激活的一级频道 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:7` |
| iOS `pathsByTab` | `[AppTab: NavigationPath]` | Tab 级 | 每个 Tab 独立维护自己的导航路径 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:8-18` |
| iOS `containerReady` | `@Published private(set) var containerReady` | 应用级 | 冷启动时用于判定是否可立即执行 deeplink 跳转 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:11-18,43-50` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 深链 | 共享导航容器 | deeplink 解析后的目标需要由 App Shell 负责承载和跳转 |
| 播放器 | 首页子路由 | `play/:id` 当前由应用壳负责注册与参数透传，真实播放能力后续补齐 |
| 剧集详情 | 首页子路由 | `detail/:id` 当前由应用壳负责注册与参数透传 |
| 首页 | 一级频道入口 | 首页是默认激活 Tab，同时承担进入播放页与详情页的代表性入口 |

### 外部依赖

| 服务 / 框架 | 用途 | 接入方式 |
|-------------|------|---------|
| Next.js App Router | Web/Backend 路由承载 | 文件系统路由 + Route Handlers |
| Navigation Compose | Android 多级导航与状态恢复 | `NavHost` + nested navigation graph |
| SwiftUI `TabView` / `NavigationStack` | iOS 一级频道与子路由承载 | 声明式导航容器 |

## 已知限制

- 除首页外，剧场、商城、赚钱、我的在 iOS/Android 仍为占位页，真实业务内容尚未接入。
- Web 端当前只补齐路由骨架，没有实现移动端同等的底部导航 UI。
- 播放页与详情页仍是占位实现，仅展示路由参数，不含真实业务数据或播放能力（见 `web/src/features/player/PlayerScreen.tsx:7-25`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt:14-34`、`ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift:4-18`）。
- Backend 未新增导航相关接口，`/api/player/start` 与 `/api/player/stop` 仍返回 501。
- 设备级黑盒验证未在本轮自动执行，当前证据主要来自构建、测试与代码审查（见 `docs/specs/2026-07-25-prd-01-bottom-nav/qa-test.md:22-25,93-119,217-223`）。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-25 | 更新：移动端应用壳从单页骨架演进为 5 Tab 导航容器，Web 补齐路由骨架，并同步修正文档中的入口、路由、状态管理与限制说明 |

---
*本文档由 llm-wiki skill 自动维护。*
