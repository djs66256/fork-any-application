# 系统总览 架构文档

> 最后更新：2026-07-27

## 概述

项目是一个多端短剧内容应用的 harness 仓库，覆盖 Web、Android、iOS 三端界面与 Backend 服务端骨架。PRD-01 已完成移动端 5 Tab 导航容器；PRD-02 进一步把 Android / iOS 首页从应用信息占位页推进为 Native 首页信息流，并让 Backend 提供 canonical `GET /api/dramas` 列表接口作为首页首屏数据源；PRD-05 则在首页频道下落地搜索发现与排行浏览链路；PRD-06 继续补齐分类浏览链路：Android / iOS 现在都能从搜索发现“分类”入口进入真实 Native 分类页，并通过 Backend `GET /api/dramas/tags` 获取固定三维度标签矩阵，点击标签后再统一复用 `GET /api/dramas/search` 和既有搜索结果页完成内容承接。Web 端继续保持路由骨架与首页壳，不在本期实现真实分类页；商城（mall）与赚钱（earn）继续由 H5 承载，不属于 Native 分类范围（`PRODUCT.md:22-25`）。

- **产品信息来源**：`PRODUCT.md`
- **仓库结构**：monorepo，按 `web/`、`android/`、`ios/`、`backend/` 分目录维护
- **技术标识**：Android/iOS 继续使用 `com.djs66256.short_drama`，移动端 deeplink scheme 为 `djsdrama://`
- **当前版本**：各端骨架版本仍为 `0.1.0`，但移动端首页已具备 Feed、搜索发现、排行浏览与分类浏览承载能力

## 架构设计

### 整体架构

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│                              用户界面层                                       │
├──────────────┬─────────────────────────────┬─────────────────────────────────┤
│   Web 前端   │        Android App          │            iOS App              │
│ Next.js 16   │ Kotlin + Compose            │ SwiftUI                         │
│ App Router   │ Navigation Compose          │ TabView + NavigationStack       │
│ 首页/搜索为壳 │ 首页Feed + 搜索 + 排行 + 分类 │ 首页Feed + 搜索 + 排行 + 分类   │
└──────┬───────┴───────────────┬─────────────┴──────────────┬──────────────────┘
       │                       │                            │
       │ 页面语义 / H5 边界      │ 首页与分类数据契约 / 路由语义 │ 首页与分类数据契约 / 路由语义
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
│  └── /api/player/start|stop                501 占位                            │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 当前首页与发现链路承载结构

| 端 | 一级容器 | 首页 / 发现链路承载 | 数据来源 / 入口 | 当前状态 |
|----|---------|--------------------|----------------|---------|
| Web | Next.js App Router 页面树 | 应用信息首页壳 + `/search` / `/rankings` 占位页 | 本地静态 UI + 代表性链接 | 首页、搜索、榜单都不实现真实数据 |
| Android | `Scaffold` + `NavigationBar` + nested `NavHost` | `HomeScreen` Feed + 搜索发现 + `RankingScreen` + `ClassificationScreen` | `GET /api/dramas` + `GET /api/dramas/search` + `GET /api/dramas/hot-search` + `GET /api/dramas/tags` | 已实现首页状态机、分类/排行真实页面、标签到搜索结果页复用 |
| iOS | `TabView` + per-tab `NavigationStack` | `HomeView` Feed + 搜索发现 + `RankingHomeView` + `ClassificationHomeView` | `GET /api/dramas` + `GET /api/dramas/search` + `GET /api/dramas/hot-search` + `GET /api/dramas/tags` | 已实现首页状态机、分类/排行真实页面、标签到搜索结果页复用 |
| Backend | Route Handlers | 首页 Feed + 搜索 + 热搜 + 分类 tags + 排行 + 预约接口 | `DramaService -> DramaMockRepository` | 已提供固定分类 seed、搜索 tags 命中与排行/预约 mock 能力 |

### 核心流程调用栈

#### 流程：移动端从搜索发现进入分类页并复用搜索结果页

```text
Android
1. SearchHomeScreen 入口触发 AppDestination.classification()
2. NavGraph 渲染 ClassificationScreen
3. ClassificationViewModel 首次请求 GetClassificationTagsUseCase(gender=all)
4. ApiService.getDramaTags(gender)
5. Backend GET /api/dramas/tags -> DramaService -> DramaMockRepository
6. ClassificationScreen 渲染顶部 gender tabs + 左 rail + 右侧 section list
7. 点击 tag -> buildSearchRoute(tag)
8. navigate(search/result?query={normalizedTag})
9. SearchResultScreen 继续通过 GET /api/dramas/search 展示结果

iOS
1. SearchHomeViewModel.route(for: .classification) -> .classificationHome
2. TabNavigationHostView 渲染 ClassificationHomeView
3. ClassificationViewModel loadIfNeeded() -> FetchClassificationTagsUseCase.execute(gender: .all)
4. DramaRemoteDataSource.fetchClassificationTags(gender)
5. Backend GET /api/dramas/tags -> DramaService -> DramaMockRepository
6. ClassificationHomeView 渲染顶部 gender tabs + 左 rail + 右侧 section list
7. 点击 tag -> router.navigate(.searchResult(query: normalizedTag))
8. SearchResultView 继续通过 GET /api/dramas/search 展示结果
```

| 调用层级 | 平台 | 文件 | 职责 |
|---------|------|------|------|
| 入口 | Android | `android/app/src/main/java/com/djs66256/short_drama/feature/search/model/SearchQuickEntry.kt:18-39` | 提供搜索发现“分类”快捷入口 |
| 1 | Android | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:211-218` | 注册分类页并把标签点击映射到搜索结果页 |
| 2 | Android | `android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt:27-252` | 维护分类页状态机、gender 切换、并发保护和搜索结果路由构建 |
| 3 | Android | `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 发起 `/api/dramas/tags` 与 `/api/dramas/search` 请求 |
| 入口 | iOS | `ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift:76-87` | 提供搜索发现“分类”快捷入口 |
| 1 | iOS | `ios/ShortDrama/Sources/App/TabNavigationHostView.swift:11-31` | 在 home Tab 注册 `ClassificationHomeView` |
| 2 | iOS | `ios/ShortDrama/Sources/Features/Classification/ViewModels/ClassificationViewModel.swift:5-109` | 维护分类页状态机、request token、防抖与 query 规范化 |
| 3 | iOS | `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift:65-75,171-175` | 发起 `/api/dramas/tags` 请求并解码响应 |
| Backend | Backend | `backend/src/app/api/dramas/tags/route.ts:7-18`, `backend/src/app/api/dramas/search/route.ts:7-19` | 校验 query 并返回分类标签矩阵、搜索结果 |
| Repository | Backend | `backend/src/repositories/mock/drama.mock.repository.ts:387-446` | 提供固定分类维度、`all` 去重合并与 tags 搜索匹配 |

### 设计决策

| 决策 | 原因 | 影响 |
|------|------|------|
| 移动端统一采用 5 个一级频道 | 为首页、剧场、商城、赚钱、我的提供稳定承载入口 | 后续功能 PRD 默认挂载到既有频道容器，而不是新增顶级入口 |
| 搜索发现、排行与分类继续挂在首页频道内 | 都属于内容发现链路的延伸，而非新的一级频道 | `ranking`、`classification` 成为首页 Tab 下的子路由，而不是新增 bottom tab |
| PRD-06 仅让 Android/iOS 接入真实分类页 | `mall` / `earn` 明确由 H5 承载，其他业务页当前按 Native 实现 | Web 继续保持 `/search` 占位页，不单独落地分类页 |
| 分类接口统一为 `GET /api/dramas/tags` | 保持 RESTful 简洁契约，并统一 Android/iOS 请求命名 | `gender` 成为跨端 canonical query |
| 分类页点击标签继续复用搜索结果页 | 避免为分类浏览再发明一套结果页与查询契约 | `GET /api/dramas/search` 命中规则必须覆盖 `title + category + tags` |
| 分类维度固定三组且空维度保留 | 保障 UI 结构稳定、左右锚点逻辑与跨端一致性 | `ClassificationDimensionsSchema` 必须始终返回 3 组 |
| Backend 继续采用 mock repository 提供分类 seed 数据 | 在缺少真实内容后台与标签运营系统前，先保障分类浏览链路可验证 | 当前分类标签矩阵与搜索命中都依赖本地 seed 与 mock 数据 |

## 跨端涉及

| 端 | 相关模块/文件 | 说明 |
|----|-------------|------|
| Web | `web/src/app/layout.tsx`, `web/src/app/page.tsx`, `web/src/features/home/HomeScreen.tsx`, `web/src/app/search/page.tsx`, `web/src/app/rankings/page.tsx` | 首页与搜索都仍为壳，仅保留 canonical 页面语义 |
| Android | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`, `feature/search/model/SearchQuickEntry.kt`, `feature/classification/viewmodel/ClassificationViewModel.kt`, `feature/classification/ui/ClassificationScreen.kt`, `core/network/ApiService.kt` | 首页 Tab 已接入搜索发现、分类页状态机、左/右同步与标签到搜索结果页的导航 |
| iOS | `ios/ShortDrama/Sources/App/TabNavigationHostView.swift`, `Features/Search/ViewModels/SearchHomeViewModel.swift`, `Features/Classification/ViewModels/ClassificationViewModel.swift`, `Features/Classification/Views/ClassificationHomeView.swift`, `Data/DataSources/DramaRemoteDataSource.swift` | 首页 Tab 已接入搜索发现、分类页状态机、左/右同步与标签到搜索结果页的导航 |
| Backend | `backend/src/app/api/dramas/search/route.ts`, `backend/src/app/api/dramas/hot-search/route.ts`, `backend/src/app/api/dramas/tags/route.ts`, `backend/src/services/drama/drama.service.ts`, `backend/src/repositories/mock/drama.mock.repository.ts`, `backend/src/lib/schemas.ts` | 提供 canonical 搜索 / 热搜 / 分类 tags 接口、字段约束与 mock 数据 |

## 技术栈总览

| 层级 | Web | Backend | Android | iOS |
|------|-----|---------|---------|-----|
| 语言 | TypeScript | TypeScript | Kotlin 2.0.21 | Swift 6 |
| UI / 路由框架 | React 19 + Next.js 16 App Router | Next.js 16 Route Handlers | Jetpack Compose + Material3 + Navigation Compose | SwiftUI + TabView + NavigationStack |
| 状态管理 | 路由参数 + React 组件状态 | Route Handler 请求级状态 | `StateFlow` + `NavController` + 首页/排行/分类状态机 | `ObservableObject` + `@Published` + 首页/排行/分类状态机 |
| 构建工具 | next build | next build | AGP 8.7.0 + Gradle | XcodeGen + Xcode 27 |
| 测试 | Vitest + Testing Library | Vitest | JUnit4 + Turbine + Compose testing helpers | Swift Testing |
| 首页 / 发现契约 | 首页壳 + 搜索占位页 | `GET /api/dramas` + `GET /api/dramas/search` + `GET /api/dramas/hot-search` + `GET /api/dramas/tags` + `GET /api/dramas/rankings` + `POST /api/dramas/:id/book` | `page/pageSize`、`q/page/pageSize`、`gender`、`type/contentType/page/pageSize` query | `page/pageSize`、`q/page/pageSize`、`gender`、`type/contentType/page/pageSize` query |

## 已知限制

- Web 端当前未实现与移动端对等的首页 Feed、搜索发现或真实分类页，只提供页面骨架和 canonical route。
- Android 与 iOS 的剧场、商城、赚钱、我的仍是占位页，真实业务会在后续 PRD 接入。
- 商城（mall）与赚钱（earn）按产品策略应由 H5 承载，但当前移动端代码仍未接入真实 H5 容器。
- 播放页与详情页跨端都还是占位实现，仅展示路由参数，不包含真实业务数据。
- Backend 当前首页 / 搜索 / 热搜 / 分类数据与预约状态都来自 `DramaMockRepository`，不是线上内容服务或持久化存储。
- 分类标签集合当前是代码内固定 seed，不具备真实运营后台或按剧库自动聚合的能力。
- 设备级黑盒验证未自动执行，当前跨端结论主要来自代码、自动化测试与 QA 文档；移动端真实点击、滚动与 gender 快切表现仍待补测（见 `docs/specs/2026-07-27-prd-06-classification/qa-test.md:14-24,278-313`）。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-27 | 更新：系统总览同步 PRD-06 分类浏览落地结果，补充搜索发现到分类页的移动端链路、Backend 搜索/热搜/分类 tags 接口与 Native / Web / H5 的范围边界 |
| 2026-07-27 | 更新：系统总览同步 PRD-05 排行体系落地结果，补充搜索发现到排行页的移动端链路、Backend 排行/预约接口与 Native / Web / H5 的范围边界 |
| 2026-07-26 | 更新：系统总览同步 PRD-02 首页信息流落地结果，补充 Backend `GET /api/dramas`、移动端首页状态机、首页卡片到播放/详情页主路径，以及 Web / H5 的范围边界 |
| 2026-07-25 | 更新：系统总览同步 PRD-01 导航骨架落地结果，修正移动端从单页骨架到 5 Tab 容器的架构描述，并补充 Web 路由骨架与 Backend 不变更说明 |
| 2026-07-22 | 从各端代码提取信息，初始创建 |

---
*本文档由 llm-wiki skill 自动维护。*