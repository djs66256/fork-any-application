# 系统总览 架构文档

> 最后更新：2026-07-27

## 概述

项目是一个多端短剧内容应用的 harness 仓库，覆盖 Web、Android、iOS 三端界面与 Backend 服务端骨架。PRD-01 完成移动端 5 Tab 导航容器；PRD-02 把 Android / iOS 首页从应用信息占位页推进为 Native 首页信息流；PRD-04 则继续把首页右上角搜索入口扩展为完整的“搜索发现”二级能力，包括搜索发现页、搜索结果页、热搜榜、本地搜索历史与排行/分类/新剧/演员承接页。Backend 同步提供 canonical `GET /api/dramas/search` 与 `GET /api/dramas/hot-search`；Web 端继续保持路由骨架和占位页，不在本期交付真实搜索发现体验；商城（mall）与赚钱（earn）继续由 H5 承载，不属于 Native 搜索发现范围（`backend/src/app/api/dramas/search/route.ts:7-19`、`backend/src/app/api/dramas/hot-search/route.ts:6-11`、`web/src/app/search/page.tsx:1-10`、`web/src/app/rankings/page.tsx:1-10`、`PRODUCT.md:22-25`）。

- **产品信息来源**：`PRODUCT.md`
- **仓库结构**：monorepo，按 `web/`、`android/`、`ios/`、`backend/` 分目录维护
- **技术标识**：Android/iOS 继续使用 `com.djs66256.short_drama`，移动端 deeplink scheme 为 `djsdrama://`（`android/app/src/main/AndroidManifest.xml:18-27`，iOS scheme 来自 `project.yml`）
- **当前版本**：各端骨架版本仍为 `0.1.0`，但移动端首页已扩展出 Native 搜索发现能力

## 架构设计

### 整体架构

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                                          用户界面层                                          │
├──────────────┬──────────────────────────────────┬───────────────────────────────────────────┤
│   Web 前端   │           Android App            │                  iOS App                  │
│ Next.js 16   │ Kotlin + Compose                 │ SwiftUI                                   │
│ App Router   │ Navigation Compose               │ TabView + NavigationStack                 │
│ 首页/搜索为壳 │ 首页 Feed + 搜索发现为 Native      │ 首页 Feed + 搜索发现为 Native               │
└──────┬───────┴────────────────┬─────────────────┴──────────────────┬────────────────────────┘
       │                        │                                    │
       │ 页面语义 / H5 边界       │ 首页 / 搜索数据契约 + 路由语义          │ 首页 / 搜索数据契约 + 路由语义
       ▼                        ▼                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                                      Backend API 服务层                                     │
│                                Next.js App Router Route Handlers                            │
│  ├── /api/health                              已实现                                         │
│  ├── /api/dramas                              已实现：首页 Feed 列表接口                        │
│  ├── /api/dramas/search                       已实现：搜索结果接口                              │
│  ├── /api/dramas/hot-search                   已实现：热搜榜接口                                │
│  └── /api/player/start|stop                   501 占位                                         │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 当前首页与搜索发现承载结构

| 端 | 一级容器 | 首页承载 | 搜索发现承载 | 数据来源 / 入口 | 当前状态 |
|----|---------|---------|-------------|----------------|---------|
| Web | Next.js App Router 页面树 | 应用信息首页壳 | `/search`、`/rankings` 占位页 | 本地静态 UI + 代表性链接 | 首页和搜索都不实现真实 Feed / 搜索 |
| Android | `Scaffold` + `NavigationBar` + nested `NavHost` | `HomeScreen` Feed 状态机 | `SearchHomeScreen`、`SearchResultScreen`、4 个 Native placeholder 页 | `GET /api/dramas`、`GET /api/dramas/search`、`GET /api/dramas/hot-search` | 已实现 loading/content(empty/error)/retry + 本地历史 |
| iOS | `TabView` + per-tab `NavigationStack` | `HomeView` Feed 状态机 | `SearchHomeView`、`SearchResultView`、`DiscoveryPlaceholderView` | `GET /api/dramas`、`GET /api/dramas/search`、`GET /api/dramas/hot-search` | 已实现 loading/content/empty/error + 本地历史 |
| Backend | Route Handlers | `GET /api/dramas` 首页列表接口 | `GET /api/dramas/search` + `GET /api/dramas/hot-search` | `DramaService -> DramaMockRepository` | 已提供 12 条短剧种子 + 10 条热搜种子 |

### 核心流程调用栈

#### 流程：移动端从首页进入搜索发现并打开搜索结果

```text
Android
1. HomeScreen 顶部搜索按钮 -> navController.navigate(search)
2. SearchHomeScreen 首屏加载本地历史 + GET /api/dramas/hot-search
3. 用户提交 query / 点击热搜 / 点击历史
4. NavGraph 跳转 search/result?query={query}
5. SearchResultViewModel -> SearchDramasUseCase(query, page=1, pageSize=10)
6. ApiService.searchDramas -> Backend GET /api/dramas/search
7. Backend search route -> DramaService -> DramaMockRepository.search(title/category contains)
8. 成功后写入 DataStore 历史；空结果也写，失败不写
9. SearchResultScreen 复用 HomeDramaCard，继续跳转 play/{id} / detail/{id}

iOS
1. HomeView toolbar 搜索按钮 -> router.navigate(.searchHome)
2. SearchHomeView 首屏加载本地历史 + GET /api/dramas/hot-search
3. 用户提交 query / 点击热搜 / 点击历史
4. NavigationStack 跳转 .searchResult(query:)
5. SearchResultViewModel -> SearchDramasUseCase.execute(query: page:1, pageSize:10)
6. DramaRemoteDataSource.searchDramas -> Backend GET /api/dramas/search
7. Backend search route -> DramaService -> DramaMockRepository.search(title/category contains)
8. 成功后写入 UserDefaults 历史；空结果也写，失败不写
9. SearchResultView 复用 HomeDramaCardView，继续跳转 .player(videoId:) / .dramaDetail(dramaId:)
```

| 调用层级 | 平台 | 文件 | 职责 |
|---------|------|------|------|
| 首页入口 | Android | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:92-111` | 首页 Feed 顶部搜索按钮触发搜索发现入口 |
| 搜索路由注册 | Android | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:147-241` | 在 HOME graph 下注册搜索首页、结果页与 4 个快捷入口承接页 |
| 搜索首页状态 | Android | `android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchHomeViewModel.kt:29-157` | 管理热搜加载、历史监听、快捷入口与搜索提交事件 |
| 搜索结果状态 | Android | `android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchResultViewModel.kt:22-163` | 管理搜索 loading/content/error/empty 与成功后写历史 |
| 搜索历史存储 | Android | `android/app/src/main/java/com/djs66256/short_drama/data/local/SearchHistoryLocalDataSource.kt:20-123` | 使用 DataStore 保存最多 10 条本地历史 |
| 首页入口 | iOS | `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:41-49` | 首页 toolbar 搜索按钮触发搜索发现入口 |
| 搜索路由注册 | iOS | `ios/ShortDrama/Sources/App/TabNavigationHostView.swift:9-31` | 在 home `NavigationStack` 下注册搜索首页、结果页与 4 个快捷入口承接页 |
| 搜索首页状态 | iOS | `ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift:18-115` | 管理热搜加载、历史刷新、快捷入口与搜索提交 |
| 搜索结果状态 | iOS | `ios/ShortDrama/Sources/Features/Search/ViewModels/SearchResultViewModel.swift:24-112` | 管理搜索 loading/content/empty/error 与成功后写历史 |
| 搜索历史存储 | iOS | `ios/ShortDrama/Sources/Data/Repositories/UserDefaultsSearchHistoryRepository.swift:10-60` | 使用 UserDefaults 保存最多 10 条本地历史 |
| 搜索接口 | Backend | `backend/src/app/api/dramas/search/route.ts:7-19` | 校验 query 并返回搜索结果列表 |
| 热搜接口 | Backend | `backend/src/app/api/dramas/hot-search/route.ts:6-11` | 返回热搜榜列表 |
| 搜索 service | Backend | `backend/src/services/drama/drama.service.ts:12-30` | 统一校验搜索与热搜响应 contract |
| 搜索 repository | Backend | `backend/src/repositories/mock/drama.mock.repository.ts:151-223` | 提供热搜种子和 title/category contains 搜索匹配 |

### 设计决策

| 决策 | 原因 | 影响 |
|------|------|------|
| 搜索发现挂在首页导航栈内，而不是新增一级 Tab | 搜索属于首页内容发现能力的扩展，不改变 5 Tab 主结构 | Android / iOS 都在 HOME graph / home stack 内注册搜索路由 |
| 搜索结果复用首页 `Drama` 列表契约 | 避免新增一套搜索专用卡片字段与 DTO | 首页 Feed 与搜索结果共用卡片组件、播放/详情入口和分页结构 |
| 本地搜索历史只在搜索成功后写入 | 避免失败请求污染用户历史；空结果仍代表真实探索意图 | Android / iOS 都实现“成功写、空结果也写、失败不写” |
| 搜索发现快捷入口首版按 Native 承接 | 与“除 mall / earn 外其他业务页当前按 Native 承接”的产品策略一致 | 排行 / 分类 / 新剧 / 演员在移动端落为占位承接页，不回退 Web |
| Backend 搜索继续使用 mock repository | 在没有真实搜索索引与内容后台前，先稳定交付接口 contract 与测试 | 搜索命中规则、热搜榜和分页行为当前都基于 mock 数据 |
| Deeplink 同步扩展到搜索发现相关 host | 保持外部入口与端内 canonical route 一致 | Android / iOS 都支持 search/ranking/classification/new-releases/actors，Android 继续兼容 `player` 历史 alias |

## 跨端涉及

| 端 | 相关模块/文件 | 说明 |
|----|-------------|------|
| Web | `web/src/app/search/page.tsx`, `web/src/app/rankings/page.tsx`, `web/src/app/play/[id]/page.tsx`, `web/src/app/detail/[id]/page.tsx` | 搜索和榜单仍为占位页，仅承担页面语义，不实现真实搜索发现体验 |
| Android | `navigation/NavGraph.kt`, `navigation/AppDestination.kt`, `navigation/DeeplinkRouteParser.kt`, `feature/search/**`, `data/local/SearchHistoryLocalDataSource.kt` | 首页 Tab 已扩展出搜索发现、搜索结果、快捷入口承接页、本地历史与 deeplink 路由 |
| iOS | `Sources/App/AppRoute.swift`, `Sources/App/TabNavigationHostView.swift`, `Sources/App/DeeplinkHandler.swift`, `Sources/Features/Search/**`, `Sources/Data/Repositories/UserDefaultsSearchHistoryRepository.swift` | 首页 tab 已扩展出搜索发现、搜索结果、快捷入口承接页、本地历史与 deeplink 路由 |
| Backend | `backend/src/app/api/dramas/search/route.ts`, `backend/src/app/api/dramas/hot-search/route.ts`, `backend/src/services/drama/drama.service.ts`, `backend/src/repositories/mock/drama.mock.repository.ts`, `backend/src/lib/schemas.ts` | 提供搜索/热搜接口、字段约束、mock 匹配规则与种子数据 |

## 技术栈总览

| 层级 | Web | Backend | Android | iOS |
|------|-----|---------|---------|-----|
| 语言 | TypeScript | TypeScript | Kotlin 2.0.21 | Swift 6 |
| UI / 路由框架 | React 19 + Next.js 16 App Router | Next.js 16 Route Handlers | Jetpack Compose + Material3 + Navigation Compose | SwiftUI + TabView + NavigationStack |
| 状态管理 | 路由参数 + React 组件状态 | Route Handler 请求级状态 | `StateFlow` + `NavController` + 搜索 `SearchHomeUiState` / `SearchResultUiState` | `ObservableObject` + `@Published` + 搜索 `SearchHomeViewModel` / `SearchResultViewModel` |
| 构建工具 | next build | next build | AGP 8.7.0 + Gradle | XcodeGen + Xcode 27 |
| 测试 | Vitest + Testing Library | Vitest | JUnit4 + Turbine + Compose testing helpers | Swift Testing |
| 首页 / 搜索契约 | 占位页 | `GET /api/dramas` + `GET /api/dramas/search` + `GET /api/dramas/hot-search` | 第 1 页搜索结果 + DataStore 历史 | 第 1 页搜索结果 + UserDefaults 历史 |

## 已知限制

- Web 端当前未实现与移动端对等的首页 Feed、搜索发现页或热搜榜，只提供页面骨架和 canonical route。
- Android 与 iOS 的排行、分类、新剧、演员当前仍是 Native 占位承接页，真实业务能力将在后续 PRD 接入。
- 搜索结果当前只覆盖第一页；搜索联想、翻页、聚合排序与个性化推荐能力均未实现。
- Backend 当前搜索与热搜数据来自 `DramaMockRepository`，不是线上真实内容服务或日志聚合结果。
- iOS 不兼容 `djsdrama://player/{id}` 历史 host；仅 Android 做了 legacy alias 兼容。
- 设备级黑盒验证未自动执行，当前跨端结论主要来自代码与自动化测试；移动端真实页面点击、外部 deeplink 唤起与恢复链路仍待补测（见 `docs/specs/2026-07-26-prd-04-search-discovery/qa-test.md:23-24,59-60`）。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-27 | 更新：系统总览从 PRD-02 首页 Feed 扩展到 PRD-04 搜索发现，补充首页搜索入口、搜索发现页/结果页、本地历史、热搜接口、deeplink 扩展与 Web / H5 范围边界 |
| 2026-07-26 | 更新：系统总览同步 PRD-02 首页信息流落地结果，补充 Backend `GET /api/dramas`、移动端首页状态机、首页卡片到播放/详情页主路径，以及 Web / H5 的范围边界 |
| 2026-07-25 | 更新：系统总览同步 PRD-01 导航骨架落地结果，修正移动端从单页骨架到 5 Tab 容器的架构描述，并补充 Web 路由骨架与 Backend 不变更说明 |
| 2026-07-22 | 从各端代码提取信息，初始创建 |

---
*本文档由 llm-wiki skill 自动维护。*
