# 系统总览 架构文档

> 最后更新：2026-07-26

## 概述

项目是一个多端短剧内容应用的 harness 仓库，覆盖 Web、Android、iOS 三端界面与 Backend 服务端骨架。PRD-01 已完成移动端 5 Tab 导航容器；PRD-02 进一步把 Android / iOS 首页从应用信息占位页推进为 Native 首页信息流，并让 Backend 提供 canonical `GET /api/dramas` 列表接口作为首页首屏数据源。Web 端继续保持路由骨架与首页壳，不在本期实现 Feed；商城（mall）与赚钱（earn）继续由 H5 承载，不属于 Native 首页 Feed 范围。

- **产品信息来源**：`PRODUCT.md`
- **仓库结构**：monorepo，按 `web/`、`android/`、`ios/`、`backend/` 分目录维护
- **技术标识**：Android/iOS 继续使用 `com.djs66256.short_drama`，移动端 deeplink scheme 为 `djsdrama://`（`android/app/src/main/AndroidManifest.xml:18-27`，iOS scheme 来自 `project.yml`）
- **当前版本**：各端骨架版本仍为 `0.1.0`，但移动端首页已具备首屏内容承载能力

## 架构设计

### 整体架构

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│                              用户界面层                                       │
├──────────────┬─────────────────────────────┬─────────────────────────────────┤
│   Web 前端   │        Android App          │            iOS App              │
│ Next.js 16   │ Kotlin + Compose            │ SwiftUI                         │
│ App Router   │ Navigation Compose          │ TabView + NavigationStack       │
│ 首页仍为壳   │ 首页为 Native Feed           │ 首页为 Native Feed              │
└──────┬───────┴───────────────┬─────────────┴──────────────┬──────────────────┘
       │                       │                            │
       │   页面语义 / H5 边界   │  首页数据契约 / 路由语义     │  首页数据契约 / 路由语义
       ▼                       ▼                            ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                            Backend API 服务层                                 │
│  Next.js App Router Route Handlers                                           │
│  ├── /api/health                        已实现                                 │
│  ├── /api/dramas                        已实现：首页 Feed 列表接口               │
│  └── /api/player/start|stop             501 占位                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 当前首页承载结构

| 端 | 一级容器 | 首页承载 | 数据来源 / 入口 | 当前状态 |
|----|---------|---------|----------------|---------|
| Web | Next.js App Router 页面树 | 应用信息首页壳 | 本地静态 UI + 代表性链接 | 首页不实现 Feed |
| Android | `Scaffold` + `NavigationBar` + nested `NavHost` | `HomeScreen` Feed 状态机 | `GET /api/dramas` + `HomeViewModel` | 已实现 loading/content(empty/error)/retry |
| iOS | `TabView` + per-tab `NavigationStack` | `HomeView` Feed 状态机 | `GET /api/dramas` + `HomeViewModel` | 已实现 loading/content/empty/error + retry |
| Backend | Route Handlers | `GET /api/dramas` 首页列表接口 | `DramaService -> DramaMockRepository` | 已提供 12 条 mock 数据 |

### 核心流程调用栈

#### 流程：移动端冷启动进入首页并加载首页 Feed

```text
Android
1. MainActivity 启动 App Shell
2. NavGraph 渲染 home graph
3. HomeScreen LaunchedEffect(Unit) -> viewModel.loadIfNeeded()
4. HomeViewModel -> GetDramasUseCase(page=1,pageSize=10)
5. ApiService.getDramas(page,pageSize)
6. Backend GET /api/dramas -> DramaService -> DramaMockRepository
7. HomeScreen 渲染 loading / list / empty / error
8. 用户点击卡片 -> navigate(play/{id} | detail/{id})

iOS
1. ShortDramaApp 启动 App Shell
2. AppShellView / TabNavigationHostView 渲染 home Tab
3. HomeView.task -> await viewModel.loadIfNeeded()
4. HomeViewModel -> FetchDramasUseCase.execute(page:1,pageSize:10)
5. DramaRemoteDataSource.fetchDramas(page,pageSize)
6. Backend GET /api/dramas -> DramaService -> DramaMockRepository
7. HomeView 渲染 loading / content / empty / error
8. 用户点击卡片 -> navigate(.player(videoId:id) | .dramaDetail(dramaId:id))
```

| 调用层级 | 平台 | 文件 | 职责 |
|---------|------|------|------|
| 入口 | Android | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:117-163` | 注册 home graph、HomeScreen 与首页子路由 |
| 1 | Android | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:41-80` | 首次组合触发加载并按状态渲染页面 |
| 2 | Android | `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:35-95` | 维护首页状态机与重试逻辑 |
| 3 | Android | `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt:20-24` | 发起 `/api/dramas?page&pageSize` 请求 |
| 入口 | iOS | `ios/ShortDrama/Sources/App/TabNavigationHostView.swift:10-18` | 在 home Tab 注册 `HomeView` 与首页子路由 |
| 1 | iOS | `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:15-56` | 首次显示触发加载并按状态渲染页面 |
| 2 | iOS | `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:33-87` | 维护首页状态机与重试逻辑 |
| 3 | iOS | `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift:18-22,42-55` | 发起 `/api/dramas?page&pageSize` 请求并解码响应 |
| Backend | Backend | `backend/src/app/api/dramas/route.ts:8-24` | 校验 query 参数并返回首页 Feed 列表响应 |
| Repository | Backend | `backend/src/repositories/mock/drama.mock.repository.ts:4-180` | 提供 12 条 mock 数据与稳定分页切片 |

### 设计决策

| 决策 | 原因 | 影响 |
|------|------|------|
| 移动端统一采用 5 个一级频道 | 为首页、剧场、商城、赚钱、我的提供稳定承载入口 | 后续功能 PRD 默认挂载到既有频道容器，而不是新增顶级入口 |
| PRD-02 仅让 Android/iOS 首页接入 Native Feed | `mall` / `earn` 明确由 H5 承载，Web 首页本期不交付 Feed | 首页内容能力优先在移动端落地，Web 继续保持壳 |
| 首页接口统一为 `GET /api/dramas?page&pageSize` | 保持 RESTful 简洁契约，并统一 Android/iOS query 命名 | 响应分页字段仍保持 snake_case，客户端需自行映射 |
| 首页卡片复用既有 `play` / `detail` 路由语义 | 避免为首页新增一套播放器/详情命名 | `drama.id` 成为首页主路径的统一参数源 |
| Backend 采用 mock repository 提供稳定 12 条种子数据 | 在缺少真实内容后台前，先保障首页 Feed 首屏、分页与自动化测试可验证 | 当前所有首页数据均为预置 mock，不含真实推荐逻辑 |
| Web 仅补齐路由骨架，不做首页 Feed | 当前 Web 端目标仍是承载页面语义与 SSR 路由，而非复刻移动端首页体验 | 后续如需 Web Feed，可在现有壳之上增量演进 |

## 跨端涉及

| 端 | 相关模块/文件 | 说明 |
|----|-------------|------|
| Web | `web/src/app/layout.tsx`, `web/src/app/page.tsx`, `web/src/features/home/HomeScreen.tsx`, `web/src/app/play/[id]/page.tsx`, `web/src/app/detail/[id]/page.tsx` | 首页仍为应用壳与代表性路由入口，不消费首页 Feed |
| Android | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`, `feature/home/viewmodel/HomeViewModel.kt`, `feature/home/ui/HomeScreen.kt`, `core/network/ApiService.kt` | 首页 Tab 已接入 Feed 状态机、列表渲染与卡片跳转 |
| iOS | `ios/ShortDrama/Sources/App/TabNavigationHostView.swift`, `Features/Home/ViewModels/HomeViewModel.swift`, `Features/Home/Views/HomeView.swift`, `Data/DataSources/DramaRemoteDataSource.swift` | 首页 Tab 已接入 Feed 状态机、列表渲染与卡片跳转 |
| Backend | `backend/src/app/api/dramas/route.ts`, `backend/src/services/drama/drama.service.ts`, `backend/src/repositories/mock/drama.mock.repository.ts`, `backend/src/lib/schemas.ts` | 提供 canonical 首页列表接口、字段约束与 mock 数据 |

## 技术栈总览

| 层级 | Web | Backend | Android | iOS |
|------|-----|---------|---------|-----|
| 语言 | TypeScript | TypeScript | Kotlin 2.0.21 | Swift 6 |
| UI / 路由框架 | React 19 + Next.js 16 App Router | Next.js 16 Route Handlers | Jetpack Compose + Material3 + Navigation Compose | SwiftUI + TabView + NavigationStack |
| 状态管理 | 路由参数 + React 组件状态 | Route Handler 请求级状态 | `StateFlow` + `NavController` + 首页 `HomeUiState` | `ObservableObject` + `@Published` + 首页 `ViewState` |
| 构建工具 | next build | next build | AGP 8.7.0 + Gradle | XcodeGen + Xcode 27 |
| 测试 | Vitest + Testing Library | Vitest | JUnit4 + Turbine + Compose testing helpers | Swift Testing |
| 首页契约 | 首页壳 + 代表性链接 | `GET /api/dramas` | `page/pageSize` query，首页消费第 1 页 | `page/pageSize` query，首页消费第 1 页 |

## 已知限制

- Web 端当前未实现与移动端对等的首页 Feed 或底部导航 UI，只提供页面骨架和 canonical route。
- Android 与 iOS 的剧场、商城、赚钱、我的仍是占位页，真实业务会在后续 PRD 接入。
- 首页 Feed 当前只覆盖第一页；下拉刷新、加载更多、推荐排序与个性化能力均未实现。
- 播放页与详情页跨端都还是占位实现，仅展示路由参数，不包含真实业务数据。
- Backend 当前首页数据来自 mock repository，不是线上内容服务。
- 设备级黑盒验证未自动执行，当前跨端结论主要来自代码与自动化测试；移动端真实页面点击与恢复链路仍待补测（见 `docs/specs/2026-07-25-prd-02-homepage-feed/qa-test.md:22-25,84-160,297-314`）。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-26 | 更新：系统总览同步 PRD-02 首页信息流落地结果，补充 Backend `GET /api/dramas`、移动端首页状态机、首页卡片到播放/详情页主路径，以及 Web / H5 的范围边界 |
| 2026-07-25 | 更新：系统总览同步 PRD-01 导航骨架落地结果，修正移动端从单页骨架到 5 Tab 容器的架构描述，并补充 Web 路由骨架与 Backend 不变更说明 |
| 2026-07-22 | 从各端代码提取信息，初始创建 |

---
*本文档由 llm-wiki skill 自动维护。*