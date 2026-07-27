# 系统总览 架构文档

> 最后更新：2026-07-28

## 概述

项目是一个多端短剧内容应用的 harness 仓库，覆盖 Web、Android、iOS 三端界面与 Backend 服务端骨架。PRD-01 已完成移动端 5 Tab 导航容器；PRD-02 进一步把 Android / iOS 首页从应用信息占位页推进为 Native 首页信息流，并让 Backend 提供 canonical `GET /api/dramas` 列表接口作为首页首屏数据源；PRD-05 则在首页频道下落地搜索发现与排行浏览链路；PRD-06 继续补齐分类浏览链路；PRD-07 再把首页左上角汉堡菜单扩展为由应用壳统一承载的左侧抽屉式菜单面板，并新增 `GET /api/player/recently-viewed` 作为 Android / iOS 菜单中“最近在看”的统一数据源。Web 端继续保持路由骨架与首页壳，不在本期实现真实菜单面板；商城（mall）与赚钱（earn）继续由 H5 承载，不属于本期菜单范围（`PRODUCT.md:22-25`）。

- **产品信息来源**：`PRODUCT.md`
- **仓库结构**：monorepo，按 `web/`、`android/`、`ios/`、`backend/` 分目录维护
- **技术标识**：Android/iOS 继续使用 `com.djs66256.short_drama`，移动端 deeplink scheme 为 `djsdrama://`
- **当前版本**：各端骨架版本仍为 `0.1.0`，但移动端首页已具备 Feed、菜单面板、搜索发现、排行浏览与分类浏览承载能力

## 架构设计

### 整体架构

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│                              用户界面层                                       │
├──────────────┬─────────────────────────────┬─────────────────────────────────┤
│   Web 前端   │        Android App          │            iOS App              │
│ Next.js 16   │ Kotlin + Compose            │ SwiftUI                         │
│ App Router   │ Navigation Compose          │ TabView + NavigationStack       │
│ 首页/搜索为壳 │ 首页Feed + 菜单 + 搜索/排行/分类 │ 首页Feed + 菜单 + 搜索/排行/分类 │
└──────┬───────┴───────────────┬─────────────┴──────────────┬──────────────────┘
       │                       │                            │
       │ 页面语义 / H5 边界      │ 首页/菜单数据契约 / 路由语义   │ 首页/菜单数据契约 / 路由语义
       ▼                       ▼                            ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                            Backend API 服务层                                 │
│  Next.js App Router Route Handlers                                           │
│  ├── /api/health                           已实现                              │
│  ├── /api/dramas                           已实现：首页 Feed 列表接口            │
│  ├── /api/dramas/search                    已实现：搜索结果接口                 │
│  ├── /api/dramas/hot-search                已实现：热搜接口                     │
│  ├── /api/dramas/tags                      已实现：分类标签接口                 │
│  ├── /api/dramas/rankings                  已实现：排行列表接口                 │
│  ├── /api/dramas/:id/book                  已实现：预约接口                     │
│  └── /api/player/progress|recently-viewed|start|stop 已实现：播放器历史与最近在看 │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 当前首页与发现链路承载结构

| 端 | 一级容器 | 首页 / 发现链路承载 | 数据来源 / 入口 | 当前状态 |
|----|---------|--------------------|----------------|---------|
| Web | Next.js App Router 页面树 | 应用信息首页壳 + `/search` / `/rankings` 占位页 | 本地静态 UI + 代表性链接 | 首页、搜索、榜单都不实现真实数据，也不涉及菜单面板 |
| Android | `Scaffold` + `NavigationBar` + nested `NavHost` + `MenuPanelDrawer` | `HomeScreen` Feed + 首页菜单抽屉 + 搜索发现 + `RankingScreen` + `ClassificationScreen` | `GET /api/dramas` + `GET /api/player/recently-viewed` + `GET /api/dramas/search` + `GET /api/dramas/hot-search` + `GET /api/dramas/tags` | 已实现首页状态机、菜单 overlay、分类/排行真实页面与最近在看到播放页复用 |
| iOS | `TabView` + per-tab `NavigationStack` + `MenuPanelContainerView` | `HomeView` Feed + 首页菜单抽屉 + 搜索发现 + `RankingHomeView` + `ClassificationHomeView` | `GET /api/dramas` + `GET /api/player/recently-viewed` + `GET /api/dramas/search` + `GET /api/dramas/hot-search` + `GET /api/dramas/tags` | 已实现首页状态机、菜单 overlay、分类/排行真实页面与最近在看到播放页复用 |
| Backend | Route Handlers | 首页 Feed + 最近在看 + 搜索 + 热搜 + 分类 tags + 排行 + 预约接口 | `DramaService / PlayerService -> mock repositories` | 已提供最近在看过滤逻辑、固定分类 seed、搜索 tags 命中与排行/预约 mock 能力 |

### 核心流程调用栈

#### 流程：移动端从首页菜单进入最近在看并跳转播放页

```text
Android
1. HomeScreen 左上角汉堡按钮调用 onOpenMenu
2. NavGraph 在首页 graph 同层渲染 MenuPanelDrawer + MenuPanelRoute
3. MenuPanelViewModel.loadIfNeeded() 通过 PlaybackSessionStore 取会话 ID
4. ApiService.getRecentlyViewed(X-Playback-Session-Id)
5. Backend GET /api/player/recently-viewed -> PlayerService -> PlaybackHistoryRepository
6. PlayerService 在固定候选窗口中过滤脏数据并最多返回 3 条
7. 点击最近在看卡片 -> emit OpenPlayback(dramaId)
8. MainNavigationViewModel.closeMenuThenNavigate(PendingRoute.Play)
9. Menu close 动画结束后再消费 pendingRoute 并打开 play/{videoId}

iOS
1. HomeView 左上角按钮调用 router.openMenuPanel()
2. AppShellView 在 ZStack 中叠加 MenuPanelContainerView
3. MenuPanelViewModel.loadIfNeeded() 通过 PlaybackSessionProviding 取会话 ID
4. PlayerRemoteDataSource 请求 GET /api/player/recently-viewed
5. Backend GET /api/player/recently-viewed -> PlayerService -> PlaybackHistoryRepository
6. PlayerService 在固定候选窗口中过滤脏数据并最多返回 3 条
7. 点击最近在看卡片 -> route(for:) 返回 .player(videoId:)
8. NavigationRouter.closeMenuPanelThenNavigate(to: .player(videoId:))
9. markMenuPanelDidClose() 后真正执行 player 导航
```

| 调用层级 | 平台 | 文件 | 职责 |
|---------|------|------|------|
| 入口 | Android | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:97-125` | 首页左上角汉堡按钮触发菜单打开 |
| 1 | Android | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:131-171,352-371` | 在首页 graph 同层承载 `MenuPanelDrawer`、菜单路由与关闭后导航消费 |
| 2 | Android | `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt:21-116` | 维护 `menuPanelState`、`pendingMenuRoute` 与 `pendingRoute` |
| 3 | Android | `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt:23-135` | 加载最近在看、发出菜单内播放/占位入口事件 |
| 4 | Android | `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt:84-87` | 发起带 `X-Playback-Session-Id` 的 `/api/player/recently-viewed` 请求 |
| 入口 | iOS | `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:41-59` | 首页 toolbar 汉堡按钮触发菜单打开 |
| 1 | iOS | `ios/ShortDrama/Sources/App/AppShellView.swift:10-31` | 在 `ZStack` 中叠加 `MenuPanelContainerView` 并禁用底层 Tab 交互 |
| 2 | iOS | `ios/ShortDrama/Sources/App/NavigationRouter.swift:15-145` | 维护 `menuPanelState`、`pendingMenuNavigation` 与关闭后导航 |
| 3 | iOS | `ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift:4-104` | 加载最近在看、映射 `.player(videoId:)` 与占位入口 |
| 4 | iOS | `ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift:23-26,85-95` | 发起带 `X-Playback-Session-Id` 的 `/api/player/recently-viewed` 请求 |
| Backend | Backend | `backend/src/app/api/player/recently-viewed/route.ts:1-21`, `backend/src/app/api/player/parse-playback-session-id.ts:1-17` | 校验 header 并暴露最近在看接口 |
| Service | Backend | `backend/src/services/player/player.service.ts:100-142`, `backend/src/lib/player.ts:1-2` | 在固定候选窗口中筛选合法历史并限制最多 3 条 |

### 设计决策

| 决策 | 原因 | 影响 |
|------|------|------|
| 移动端统一采用 5 个一级频道 | 为首页、剧场、商城、赚钱、我的提供稳定承载入口 | 后续功能 PRD 默认挂载到既有频道容器，而不是新增顶级入口 |
| 菜单面板挂在首页根页，而不是新增一级 tab | 菜单属于个人工具抽屉，不改变既有底部导航 IA | Android/iOS 都由应用壳在 home tab 上统一承载 overlay 与关闭后导航 |
| 菜单最近在看复用播放器主路径 | 避免为菜单入口再设计新的播放页语义或独立路由 | 菜单卡片点击后仍进入既有 `play/{id}` / `.player(videoId:)` 链路 |
| 最近在看接口统一为 `GET /api/player/recently-viewed` + `X-Playback-Session-Id` | 复用既有匿名播放会话与 RESTful player API 体系 | Android/iOS 菜单都走同一 header 和响应结构 |
| 最近在看只扫描固定候选窗口并过滤脏数据 | 先保证返回结果合法稳定，避免引入补偿式 offset 逻辑 | 返回最多 3 条，允许不足 3 条，不承诺向更老历史补足 |
| Web 本期不接入菜单面板 | 当前 Web 继续承担 canonical route 与首页壳职责，不复制移动端交互形态 | 菜单面板属于 Android/iOS 范围，Web 继续无菜单入口 |
| Backend 继续采用 mock repository / in-memory history 提供首页与播放数据 | 在缺少真实用户体系与内容后台前，先保障首页、播放器和菜单链路可验证 | 最近在看、首页 Feed、搜索/排行/分类当前都依赖本地 mock / history 数据 |

## 跨端涉及

| 端 | 相关模块/文件 | 说明 |
|----|-------------|------|
| Web | `web/src/app/layout.tsx`, `web/src/app/page.tsx`, `web/src/features/home/HomeScreen.tsx`, `web/src/app/search/page.tsx`, `web/src/app/rankings/page.tsx` | 首页与搜索都仍为壳，仅保留 canonical 页面语义，不承载菜单面板 |
| Android | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`, `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt`, `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt`, `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelScreen.kt`, `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 首页 Tab 已接入菜单抽屉 overlay、最近在看加载、先关菜单再导航与占位入口承接 |
| iOS | `ios/ShortDrama/Sources/App/AppShellView.swift`, `ios/ShortDrama/Sources/App/NavigationRouter.swift`, `ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift`, `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift`, `ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift` | 首页 Tab 已接入菜单 overlay、最近在看加载、先关菜单再导航与占位入口承接 |
| Backend | `backend/src/app/api/player/recently-viewed/route.ts`, `backend/src/app/api/player/parse-playback-session-id.ts`, `backend/src/services/player/player.service.ts`, `backend/src/lib/player.ts`, `backend/src/lib/schemas.ts` | 提供最近在看接口、header 校验、固定候选窗口过滤逻辑与响应约束 |

## 技术栈总览

| 层级 | Web | Backend | Android | iOS |
|------|-----|---------|---------|-----|
| 语言 | TypeScript | TypeScript | Kotlin 2.0.21 | Swift 6 |
| UI / 路由框架 | React 19 + Next.js 16 App Router | Next.js 16 Route Handlers | Jetpack Compose + Material3 + Navigation Compose + drawer overlay | SwiftUI + TabView + NavigationStack + ZStack overlay |
| 状态管理 | 路由参数 + React 组件状态 | Route Handler 请求级状态 | `StateFlow` + `NavController` + 首页/菜单/排行/分类状态机 | `ObservableObject` + `@Published` + 首页/菜单/排行/分类状态机 |
| 构建工具 | next build | next build | AGP 8.7.0 + Gradle | XcodeGen + Xcode 27 |
| 测试 | Vitest + Testing Library | Vitest | JUnit4 + Turbine + Compose testing helpers | Swift Testing |
| 首页 / 菜单 / 发现契约 | 首页壳 + 搜索占位页 | `GET /api/dramas` + `GET /api/player/recently-viewed` + `GET /api/dramas/search` + `GET /api/dramas/hot-search` + `GET /api/dramas/tags` + `GET /api/dramas/rankings` + `POST /api/dramas/:id/book` + `POST /api/player/start|stop` | `page/pageSize`、`q/page/pageSize`、`gender`、`type/contentType/page/pageSize` query + `X-Playback-Session-Id` | `page/pageSize`、`q/page/pageSize`、`gender`、`type/contentType/page/pageSize` query + `X-Playback-Session-Id` |

## 已知限制

- Web 端当前未实现与移动端对等的首页 Feed、菜单面板、搜索发现或真实分类页，只提供页面骨架和 canonical route。
- Android 与 iOS 的剧场、商城、赚钱、我的仍是占位页，真实业务会在后续 PRD 接入。
- 商城（mall）与赚钱（earn）按产品策略应由 H5 承载，但当前移动端代码仍未接入真实 H5 容器。
- 菜单中的登录、消息、预约、下载仍是 Native 占位承接页；游戏中心仅提供本地“即将上线”反馈，不进入真实业务页面。
- 最近在看接口只从固定候选窗口中返回最多 3 条合法记录；过滤脏数据后允许不足 3 条，也不承诺继续向更老历史补足。
- 播放页与详情页跨端都还是占位实现，仅展示路由参数，不包含真实业务数据。
- Backend 当前首页 / 搜索 / 热搜 / 分类数据与预约状态都来自 `DramaMockRepository`，最近在看与播放历史也仍是 mock repository / in-memory history，不是线上内容服务或持久化存储。
- 设备级黑盒验证未自动执行，当前跨端结论主要来自代码、自动化测试与 QA 文档；移动端真实菜单开合、连点与最近在看点击表现仍待补测（见 `docs/specs/2026-07-27-prd-07-menu-panel/qa-test.md:14-32,246-275`）。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-28 | 更新：系统总览同步 PRD-07 菜单面板落地结果，补充首页汉堡菜单抽屉、关闭后导航时序、`GET /api/player/recently-viewed` 与 Web 不在本期范围的边界 |
| 2026-07-27 | 更新：系统总览同步 PRD-06 分类浏览落地结果，补充搜索发现到分类页的移动端链路、Backend 搜索/热搜/分类 tags 接口与 Native / Web / H5 的范围边界 |
| 2026-07-27 | 更新：系统总览同步 PRD-05 排行体系落地结果，补充搜索发现到排行页的移动端链路、Backend 排行/预约接口与 Native / Web / H5 的范围边界 |
| 2026-07-26 | 更新：系统总览同步 PRD-02 首页信息流落地结果，补充 Backend `GET /api/dramas`、移动端首页状态机、首页卡片到播放/详情页主路径，以及 Web / H5 的范围边界 |
| 2026-07-25 | 更新：系统总览同步 PRD-01 导航骨架落地结果，修正移动端从单页骨架到 5 Tab 容器的架构描述，并补充 Web 路由骨架与 Backend 不变更说明 |
| 2026-07-22 | 从各端代码提取信息，初始创建 |

---
*本文档由 llm-wiki skill 自动维护。*