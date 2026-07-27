# 系统总览 架构文档

> 最后更新：2026-07-27

## 概述

项目是一个多端短剧内容应用的 harness 仓库，覆盖 Web、Android、iOS 三端界面与 Backend 服务端骨架。PRD-01 已完成移动端 5 Tab 导航容器；PRD-02 进一步把 Android / iOS 首页从应用信息占位页推进为 Native 首页信息流，并让 Backend 提供 canonical `GET /api/dramas` 列表接口作为首页首屏数据源；PRD-05 则继续在首页频道下落地搜索发现与排行浏览链路：Android / iOS 现在都能从搜索发现“排行”入口进入真实 Native 排行页，并通过 Backend `GET /api/dramas/rankings` / `POST /api/dramas/:id/book` 完成榜单浏览与预约交互。Web 端继续保持路由骨架与首页壳，不在本期实现真实排行页；商城（mall）与赚钱（earn）继续由 H5 承载，不属于 Native 排行范围（`PRODUCT.md:22-25`）。

- **产品信息来源**：`PRODUCT.md`
- **仓库结构**：monorepo，按 `web/`、`android/`、`ios/`、`backend/` 分目录维护
- **技术标识**：Android/iOS 继续使用 `com.djs66256.short_drama`，移动端 deeplink scheme 为 `djsdrama://`（`android/app/src/main/AndroidManifest.xml:18-27`，iOS scheme 来自 `project.yml`）
- **当前版本**：各端骨架版本仍为 `0.1.0`，但移动端首页已具备 Feed 与排行浏览承载能力

## 架构设计

### 整体架构

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│                              用户界面层                                       │
├──────────────┬─────────────────────────────┬─────────────────────────────────┤
│   Web 前端   │        Android App          │            iOS App              │
│ Next.js 16   │ Kotlin + Compose            │ SwiftUI                         │
│ App Router   │ Navigation Compose          │ TabView + NavigationStack       │
│ 首页/榜单为壳 │ 首页Feed + 搜索 + 排行 Native │ 首页Feed + 搜索 + 排行 Native   │
└──────┬───────┴───────────────┬─────────────┴──────────────┬──────────────────┘
       │                       │                            │
       │ 页面语义 / H5 边界      │ 首页与排行数据契约 / 路由语义 │ 首页与排行数据契约 / 路由语义
       ▼                       ▼                            ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                            Backend API 服务层                                 │
│  Next.js App Router Route Handlers                                           │
│  ├── /api/health                           已实现                              │
│  ├── /api/dramas                           已实现：首页 Feed 列表接口            │
│  ├── /api/dramas/rankings                  已实现：排行列表接口                 │
│  ├── /api/dramas/:id/book                  已实现：预约接口                     │
│  └── /api/player/start|stop                501 占位                            │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 当前首页与排行承载结构

| 端 | 一级容器 | 首页 / 排行承载 | 数据来源 / 入口 | 当前状态 |
|----|---------|----------------|----------------|---------|
| Web | Next.js App Router 页面树 | 应用信息首页壳 + `/rankings` 占位页 | 本地静态 UI + 代表性链接 | 首页和榜单都不实现真实数据 |
| Android | `Scaffold` + `NavigationBar` + nested `NavHost` | `HomeScreen` Feed + 搜索发现 + `RankingScreen` | `GET /api/dramas` + `GET /api/dramas/rankings` + `RankingViewModel` | 已实现首页状态机、榜单双层 Tab、分页与预约拦截 |
| iOS | `TabView` + per-tab `NavigationStack` | `HomeView` Feed + 搜索发现 + `RankingHomeView` | `GET /api/dramas` + `GET /api/dramas/rankings` + `RankingViewModel` | 已实现首页状态机、榜单双层 Tab、分页与预约拦截 |
| Backend | Route Handlers | 首页 Feed + 排行 + 预约接口 | `DramaService -> DramaMockRepository` | 已提供 12 条 mock 榜单数据和幂等预约行为 |

### 核心流程调用栈

#### 流程：移动端从搜索发现进入排行页并浏览榜单

```text
Android
1. Home/Search 入口触发 AppDestination.ranking(contentType, type)
2. NavGraph 渲染 RankingScreen
3. RankingViewModel 从 SavedStateHandle 读取 contentType/type
4. GetDramaRankingsUseCase(query: all + hot + page=1 + pageSize=10)
5. ApiService.getDramaRankings(type, contentType, page, pageSize)
6. Backend GET /api/dramas/rankings -> DramaService -> DramaMockRepository
7. RankingScreen 渲染双层 Tab、列表、空态/错误态/分页 footer
8. 点击排行项 -> navigate(play/{id})；点击预约 -> 预校验登录 -> POST /api/dramas/{id}/book

iOS
1. SearchHomeViewModel.route(for: .ranking) -> .rankingHome
2. TabNavigationHostView 渲染 RankingHomeView
3. RankingViewModel loadIfNeeded() -> FetchRankingsUseCase.execute(query: all + hot + page=1 + pageSize=10)
4. DramaRemoteDataSource.fetchRankings(query)
5. Backend GET /api/dramas/rankings -> DramaService -> DramaMockRepository
6. RankingHomeView 渲染双层 Tab、列表、空态/错误态/分页 footer
7. 点击排行项 -> navigate(.player(videoId:id))；点击预约 -> 登录拦截或 POST /api/dramas/{id}/book
```

| 调用层级 | 平台 | 文件 | 职责 |
|---------|------|------|------|
| 入口 | Android | `android/app/src/main/java/com/djs66256/short_drama/feature/search/model/SearchQuickEntry.kt:18-39` | 提供搜索发现“排行”快捷入口 |
| 1 | Android | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:187-209` | 注册排行页并把卡片点击映射到播放路由 |
| 2 | Android | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:51-421` | 维护榜单状态机、分页、旧请求防抖和预约拦截 |
| 3 | Android | `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt:39-48` | 发起 `/api/dramas/rankings` 与 `/api/dramas/{id}/book` 请求 |
| 入口 | iOS | `ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift:76-87` | 提供搜索发现“排行”快捷入口 |
| 1 | iOS | `ios/ShortDrama/Sources/App/TabNavigationHostView.swift:11-31` | 在 home Tab 注册 `RankingHomeView` |
| 2 | iOS | `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:5-224` | 维护榜单状态机、分页、预约与登录拦截 |
| 3 | iOS | `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift:65-89,155-165` | 发起 `/api/dramas/rankings` 与 `/api/dramas/{id}/book` 请求并解码响应 |
| Backend | Backend | `backend/src/app/api/dramas/rankings/route.ts:8-24`, `backend/src/app/api/dramas/[id]/book/route.ts:16-28` | 校验 query/path/header 并返回排行与预约响应 |
| Repository | Backend | `backend/src/repositories/mock/drama.mock.repository.ts:294-395` | 提供内容类型过滤、榜单排序、分页与幂等预约 |

### 设计决策

| 决策 | 原因 | 影响 |
|------|------|------|
| 移动端统一采用 5 个一级频道 | 为首页、剧场、商城、赚钱、我的提供稳定承载入口 | 后续功能 PRD 默认挂载到既有频道容器，而不是新增顶级入口 |
| 搜索发现与排行继续挂在首页频道内 | 排行是内容发现链路的延伸，而非新的一级频道 | `ranking` 成为首页 Tab 下的子路由，而不是新增 bottom tab |
| PRD-05 仅让 Android/iOS 接入真实排行页 | `mall` / `earn` 明确由 H5 承载，其他业务页当前按 Native 实现 | Web 继续保持 `/rankings` 占位页，移动端优先落地榜单体验 |
| 排行接口统一为 `GET /api/dramas/rankings` | 保持 RESTful 简洁契约，并统一 Android/iOS query 命名 | `type/contentType/page/pageSize` 成为跨端 canonical query |
| 排行项继续复用 `play` 路由语义 | 避免为排行榜单再发明一套播放器命名 | 首页 Feed 与排行页共享 `drama.id -> play/:id` 主路径 |
| 预约接口使用 `POST /api/dramas/:id/book` | 将预约建模为对 Dramas 资源的附属动作，避免另起资源树 | 当前只支持单向幂等预约成功语义，不支持取消 |
| Backend 继续采用 mock repository 提供稳定榜单数据 | 在缺少真实内容后台与登录体系前，先保障排行浏览、分页与预约交互可验证 | 当前所有排行数据与预约状态均为本地进程内 mock，不含真实推荐算法 |

## 跨端涉及

| 端 | 相关模块/文件 | 说明 |
|----|-------------|------|
| Web | `web/src/app/layout.tsx`, `web/src/app/page.tsx`, `web/src/features/home/HomeScreen.tsx`, `web/src/app/rankings/page.tsx`, `web/src/app/play/[id]/page.tsx` | 首页与榜单都仍为壳，仅保留 canonical 页面语义 |
| Android | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`, `feature/search/model/SearchQuickEntry.kt`, `feature/ranking/viewmodel/RankingViewModel.kt`, `feature/ranking/ui/RankingScreen.kt`, `core/network/ApiService.kt` | 首页 Tab 已接入搜索发现、排行页状态机、分页与预约交互 |
| iOS | `ios/ShortDrama/Sources/App/TabNavigationHostView.swift`, `Features/Search/ViewModels/SearchHomeViewModel.swift`, `Features/Ranking/ViewModels/RankingViewModel.swift`, `Features/Ranking/Views/RankingHomeView.swift`, `Data/DataSources/DramaRemoteDataSource.swift` | 首页 Tab 已接入搜索发现、排行页状态机、分页与预约交互 |
| Backend | `backend/src/app/api/dramas/rankings/route.ts`, `backend/src/app/api/dramas/[id]/book/route.ts`, `backend/src/services/drama/drama.service.ts`, `backend/src/repositories/mock/drama.mock.repository.ts`, `backend/src/lib/schemas.ts` | 提供 canonical 排行 / 预约接口、字段约束与 mock 数据 |

## 技术栈总览

| 层级 | Web | Backend | Android | iOS |
|------|-----|---------|---------|-----|
| 语言 | TypeScript | TypeScript | Kotlin 2.0.21 | Swift 6 |
| UI / 路由框架 | React 19 + Next.js 16 App Router | Next.js 16 Route Handlers | Jetpack Compose + Material3 + Navigation Compose | SwiftUI + TabView + NavigationStack |
| 状态管理 | 路由参数 + React 组件状态 | Route Handler 请求级状态 | `StateFlow` + `NavController` + 首页/排行双状态机 | `ObservableObject` + `@Published` + 首页/排行双状态机 |
| 构建工具 | next build | next build | AGP 8.7.0 + Gradle | XcodeGen + Xcode 27 |
| 测试 | Vitest + Testing Library | Vitest | JUnit4 + Turbine + Compose testing helpers | Swift Testing |
| 首页 / 排行契约 | 首页壳 + 榜单占位页 | `GET /api/dramas` + `GET /api/dramas/rankings` + `POST /api/dramas/:id/book` | `page/pageSize` 与 `type/contentType/page/pageSize` query | `page/pageSize` 与 `type/contentType/page/pageSize` query |

## 已知限制

- Web 端当前未实现与移动端对等的首页 Feed、搜索发现或真实排行页，只提供页面骨架和 canonical route。
- Android 与 iOS 的剧场、商城、赚钱、我的仍是占位页，真实业务会在后续 PRD 接入。
- 商城（mall）与赚钱（earn）按产品策略应由 H5 承载，但当前移动端代码仍未接入真实 H5 容器。
- 播放页与详情页跨端都还是占位实现，仅展示路由参数，不包含真实业务数据。
- Backend 当前首页 / 排行数据与预约状态都来自 `DramaMockRepository`，不是线上内容服务或持久化存储。
- 预约认证仍是 skeleton auth：`x-user-id` 或 `Bearer <user-id>` 被当作 userId 使用，尚未接入真实 JWT / Supabase 校验（`backend/src/middleware/auth.ts:5-32`）。
- 设备级黑盒验证未自动执行，当前跨端结论主要来自代码、自动化测试与 QA 文档；移动端真实点击、翻页与预约拦截表现仍待补测（见 `docs/specs/2026-07-27-prd-05-ranking/qa-test.md:14-24,59-79`）。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-27 | 更新：系统总览同步 PRD-05 排行体系落地结果，补充搜索发现到排行页的移动端链路、Backend 排行/预约接口与 Native / Web / H5 的范围边界 |
| 2026-07-26 | 更新：系统总览同步 PRD-02 首页信息流落地结果，补充 Backend `GET /api/dramas`、移动端首页状态机、首页卡片到播放/详情页主路径，以及 Web / H5 的范围边界 |
| 2026-07-25 | 更新：系统总览同步 PRD-01 导航骨架落地结果，修正移动端从单页骨架到 5 Tab 容器的架构描述，并补充 Web 路由骨架与 Backend 不变更说明 |
| 2026-07-22 | 从各端代码提取信息，初始创建 |

---
*本文档由 llm-wiki skill 自动维护。*