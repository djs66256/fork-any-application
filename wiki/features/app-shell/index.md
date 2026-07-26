# 应用壳 (App Shell)

> 最后更新：2026-07-26

## 功能概述

应用壳负责承载各端应用的启动入口、路由容器与基础页面骨架。PRD-01 已将移动端应用从单页占位结构演进为 5 个一级频道的底部导航容器；PRD-02 则进一步让首页频道从“应用信息占位页”演进为可直接消费 Feed 的 Native 首页首屏。Web 端继续维持 SSR-first 的 Next.js App Router 结构，但首页仍是应用信息骨架，不实现 Feed；Backend 继续提供服务首页和健康检查入口，并新增（已实现）首页 Feed 列表接口供移动端首页消费。

- **覆盖端**：Web、Android、iOS、Backend
- **核心价值**：为首页 Feed、剧场、商城、赚钱中心、个人页、播放页和详情页提供统一承载容器
- **当前状态**：移动端导航骨架已落地，其中首页频道已接入真实列表数据；其余频道仍以占位实现为主

## 入口与路由

### Web
- 入口组件：`web/src/app/layout.tsx:15-33`（根布局与全局 metadata）+ `web/src/app/page.tsx`（首页）
- 路由方案：Next.js App Router，Page 层仅负责路由委托
- 当前可访问骨架路由：`/`、`/play/[id]`、`/detail/[id]`、`/search`、`/rankings`、`/mall`（见 `web/src/app/play/[id]/page.tsx:14-39`、`web/src/app/detail/[id]/page.tsx:14-39`、`web/src/app/search/page.tsx:1-10`、`web/src/app/rankings/page.tsx:1-10`、`web/src/app/mall/page.tsx:1-10`）
- 首页现状：`HomeScreen` 仍展示应用信息和代表性链接，不消费首页 Feed（`web/src/features/home/HomeScreen.tsx:12-55`）

### Backend
- 入口组件：`backend/src/app/layout.tsx` + `backend/src/app/page.tsx`
- 提供 `/` 页面展示服务信息，并保留 `/api/health` 作为健康检查入口
- 首页 Feed 数据接口：`GET /api/dramas` 已作为移动端首页频道的数据源落地（`backend/src/app/api/dramas/route.ts:8-24`）

### Android
- 入口 Activity：`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt:21-54`
- Manifest 声明：`android/app/src/main/AndroidManifest.xml:13-27`，同一 `MainActivity` 同时承担 LAUNCHER 与 deeplink 入口
- 当前容器：`NavGraph` 在 `Scaffold` 中挂载 `NavigationBar` + `NavHost`，提供 5 个一级频道 graph（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:77-214`）
- 首页子路由：`play/{videoId}`、`player/{videoId}`、`detail/{dramaId}`、`dramaDetail/{dramaId}`（`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:29-83`）
- 首页现状：home graph 默认展示 `HomeScreen` Feed 状态机，不再只是示例按钮占位页（`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:41-288`）

### iOS
- 入口 App：`ios/ShortDrama/Sources/App/ShortDramaApp.swift:5-23`
- 当前容器：`AppShellView` 使用 `TabView(selection: $router.selectedTab)` 渲染 5 个 Tab（`ios/ShortDrama/Sources/App/AppShellView.swift:3-20`）
- 一级频道：`AppTab` 定义 `home`、`theater`、`mall`、`earn`、`profile`，并绑定标题/图标（`ios/ShortDrama/Sources/App/AppTab.swift:3-41`）
- 首页子路由：`AppRoute.player(videoId:)` 与 `AppRoute.dramaDetail(dramaId:)`，对外公开名称分别为 `play`、`detail`（`ios/ShortDrama/Sources/App/AppRoute.swift:4-29`）
- 首页现状：home Tab 默认展示 `HomeView` Feed 状态机，不再只是演示型入口页（`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:15-224`）

## 核心逻辑

### Web 端
1. `layout.tsx` 提供全局 metadata 和字体注入（`web/src/app/layout.tsx:15-33`）。
2. 首页 `HomeScreen` 展示应用元信息，并提供代表性导航链接到 `/play/sample`、`/detail/sample`、`/search`、`/rankings`、`/mall`（`web/src/features/home/HomeScreen.tsx:12-55`）。
3. 动态路由页在服务端先对 `id` 做 `trim()` + 非空校验，非法参数走 `notFound()`（`web/src/app/play/[id]/page.tsx:9-39`、`web/src/app/detail/[id]/page.tsx:9-39`）。
4. 搜索、榜单、商城页统一复用 `PlaceholderRouteScreen`，只承担路由骨架职责（`web/src/features/placeholder-route/PlaceholderRouteScreen.tsx:3-21`）。

### Backend 端
1. 服务首页继续作为运行信息与入口页，不参与移动端首页容器渲染。
2. `/api/health` 继续提供健康检查。
3. `GET /api/dramas` 已成为移动端首页频道的数据源，请求参数为 `page` 与 `pageSize`，响应为 `{ data, pagination }`（`backend/src/app/api/dramas/route.ts:8-24`）。
4. `/api/player/start` 与 `/api/player/stop` 仍为 501 占位接口，本期未新增播放器真实能力（`backend/src/app/api/player/start/route.ts:1-6`、`backend/src/app/api/player/stop/route.ts:1-6`）。

### Android 端
1. `MainActivity` 在冷启动与 `onNewIntent` 两条路径下统一接收 deeplink，并把解析结果写入 `MainNavigationViewModel` 的 `pendingRoute`（`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt:24-53`）。
2. `NavGraph` 通过 `Scaffold(bottomBar = { NavigationBar { ... }})` 渲染 5 个 Tab，点击时使用 `popUpTo(findStartDestination()) + saveState + restoreState` 保留多 back stack 状态（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:77-109`）。
3. 首页 graph 当前承载真实 `HomeScreen` Feed，首次组合自动触发 `loadIfNeeded()` 拉取首页列表；卡片动作跳转播放页/详情页（`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:47-76`）。
4. 剧场、商城、赚钱、我的仍渲染共享 `PlaceholderScreen`，避免同构占位实现重复（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:164-211`）。
5. `LaunchedEffect(uiState.pendingRoute)` 在导航容器 ready 后消费待执行路由，实现冷启动 deeplink 延迟跳转（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:45-75`）。

### iOS 端
1. `ShortDramaApp` 持有单例 `NavigationRouter` 并通过 `.environmentObject(router)` 注入全局导航状态（`ios/ShortDrama/Sources/App/ShortDramaApp.swift:7-23`）。
2. `AppShellView` 以 `TabView` 渲染 5 个一级频道，并在 `.task` 中调用 `router.markContainerReady()` 标记容器可导航（`ios/ShortDrama/Sources/App/AppShellView.swift:6-18`）。
3. `NavigationRouter` 为每个 Tab 维护独立 `NavigationPath`，切换频道时不清空其它频道栈；首页子页 push 到 `home` Tab 的独立路径中（`ios/ShortDrama/Sources/App/NavigationRouter.swift:7-64`）。
4. `TabNavigationHostView` 为每个 Tab 提供独立 `NavigationStack`，首页注册 `HomeView`、`PlayerView`、`DramaDetailView`，其余频道复用 `PlaceholderTabView`（`ios/ShortDrama/Sources/App/TabNavigationHostView.swift:8-32`）。
5. `HomeView` 在首页 Tab 默认加载首页 Feed，请求成功后直接渲染内容列表，而不是旧的示例跳转页（`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:15-224`）。

## 多端实现

### Web
- 源文件：`web/src/app/layout.tsx:15-33`、`web/src/app/play/[id]/page.tsx:14-39`、`web/src/app/detail/[id]/page.tsx:14-39`
- 首页导航入口：`web/src/features/home/HomeScreen.tsx:27-51`
- 占位页复用：`web/src/features/placeholder-route/PlaceholderRouteScreen.tsx:3-21`
- 技术：Next.js 16、React 19、TypeScript、SSR-first App Router

### Android
- 入口与容器：`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt:21-54`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:36-214`
- 路由常量：`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:3-83`
- 首页承载：`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:41-288`
- 占位频道：`android/app/src/main/java/com/djs66256/short_drama/feature/common/ui/PlaceholderScreen.kt:14-38`
- 技术：Kotlin 2.0.21、Jetpack Compose、Material3、Navigation Compose、Hilt

### iOS
- 入口与容器：`ios/ShortDrama/Sources/App/ShortDramaApp.swift:5-23`、`ios/ShortDrama/Sources/App/AppShellView.swift:3-20`
- Tab 定义：`ios/ShortDrama/Sources/App/AppTab.swift:3-41`
- 路由与状态：`ios/ShortDrama/Sources/App/AppRoute.swift:4-29`、`ios/ShortDrama/Sources/App/NavigationRouter.swift:5-64`
- 首页承载：`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:15-224`
- 占位频道：`ios/ShortDrama/Sources/Features/Shell/Views/PlaceholderTabView.swift:3-27`
- 技术：Swift 6、SwiftUI、TabView、NavigationStack、Swift Testing

### Backend
- 服务首页：`backend/src/app/page.tsx`
- 健康检查：`backend/src/app/api/health/route.ts`
- 首页 Feed 接口：`backend/src/app/api/dramas/route.ts:8-24`
- 与导航骨架相关的播放器接口现状：`backend/src/app/api/player/start/route.ts:1-6`、`backend/src/app/api/player/stop/route.ts:1-6`
- 技术：Next.js 16、TypeScript、App Router Route Handlers

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/health` | [../../api/health.md](../../api/health.md) | 服务健康检查，不受首页 Feed 改造影响 |
| `GET /api/dramas` | [../../api/dramas.md](../../api/dramas.md) | 移动端首页频道的 Feed 数据源 |
| `POST /api/player/start` | [../../api/player.md](../../api/player.md) | 播放页骨架相关占位接口，当前仍返回 501 |
| `POST /api/player/stop` | [../../api/player.md](../../api/player.md) | 播放结束上报占位接口，当前仍返回 501 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Web 路由参数 | Next.js `params` | 页面级 | `play` / `detail` 页面按需读取 `id` 并在服务端先校验 | `web/src/app/play/[id]/page.tsx:14-39`, `web/src/app/detail/[id]/page.tsx:14-39` |
| Android `pendingRoute` | `MutableStateFlow<UiState>` | 应用级 | deeplink 先入队，待 `NavHost` 可消费后执行 | `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt:14-38` |
| Android 多 Tab 栈 | `NavController` + `saveState/restoreState` | Tab 级 | 切换频道时保留已访问 graph 的返回栈 | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:87-96` |
| Android 首页 Feed 状态 | `MutableStateFlow<HomeUiState>` | 页面级 | 首页默认页面状态，承载 loading / list / empty / error / retrying | `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:17-31` |
| iOS `selectedTab` | `@Published var selectedTab` | 应用级 | 控制当前激活的一级频道 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:7` |
| iOS `pathsByTab` | `[AppTab: NavigationPath]` | Tab 级 | 每个 Tab 独立维护自己的导航路径 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:8-18` |
| iOS `containerReady` | `@Published private(set) var containerReady` | 应用级 | 冷启动时用于判定是否可立即执行 deeplink 跳转 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:11-18,43-50` |
| iOS 首页 Feed 状态 | `@Published private(set) var viewState` | 页面级 | 首页默认页面状态，承载 loading / content / empty / error | `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:7-23` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 深链 | 共享导航容器 | deeplink 解析后的目标需要由 App Shell 负责承载和跳转 |
| 播放器 | 首页子路由 | `play/:id` 当前由应用壳负责注册与参数透传，真实播放能力后续补齐 |
| 剧集详情 | 首页子路由 | `detail/:id` 当前由应用壳负责注册与参数透传 |
| 首页信息流 | 一级频道默认内容 | 首页是默认激活 Tab，并已从占位页演进为 Feed 首屏 |

### 外部依赖

| 服务 / 框架 | 用途 | 接入方式 |
|-------------|------|---------|
| Next.js App Router | Web/Backend 路由承载 | 文件系统路由 + Route Handlers |
| Navigation Compose | Android 多级导航与状态恢复 | `NavHost` + nested navigation graph |
| SwiftUI `TabView` / `NavigationStack` | iOS 一级频道与子路由承载 | 声明式导航容器 |
| `GET /api/dramas` | 移动端首页数据加载 | Backend Route Handler + 各端网络层调用 |

## 已知限制

- 除首页外，剧场、商城、赚钱、我的在 iOS/Android 仍为占位页，真实业务内容尚未接入。
- Web 端当前只补齐路由骨架，没有实现移动端同等的底部导航 UI，也没有实现首页 Feed。
- 商城（mall）与赚钱（earn）保持 H5 承载，不属于本期 Native 首页 Feed（`PRODUCT.md:22-25`）。
- 播放页与详情页仍是占位实现，仅展示路由参数，不含真实业务数据或播放能力（见 `web/src/features/player/PlayerScreen.tsx:7-25`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt:14-34`、`ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift:4-18`）。
- Backend 未新增播放器真实能力，`/api/player/start` 与 `/api/player/stop` 仍返回 501。
- 移动端首页设备级黑盒验证未在本轮自动执行，当前证据主要来自自动化测试、QA 文档与代码审查（`docs/specs/2026-07-25-prd-02-homepage-feed/qa-test.md:22-25,84-160,297-314`）。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-26 | 更新：同步 PRD-02 后首页频道从占位页演进为 Native Feed 首屏，补充 Backend `GET /api/dramas` 作为首页容器依赖的数据源，并修正文档中的首页承载现状 |
| 2026-07-25 | 更新：移动端应用壳从单页骨架演进为 5 Tab 导航容器，Web 补齐路由骨架，并同步修正文档中的入口、路由、状态管理与限制说明 |

---
*本文档由 llm-wiki skill 自动维护。*