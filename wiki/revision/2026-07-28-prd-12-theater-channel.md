# 2026-07-28 PRD-12 theater-channel wiki 修订记录

## `wiki/features/index.md`
- 变更类型：增量更新
- 变更章节：功能域索引
- 变更摘要：新增“剧场频道”功能域入口，并把搜索发现索引说明扩展为同时覆盖首页与剧场的搜索入口复用。
- 主要来源：
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`
  - `ios/ShortDrama/Sources/App/AppTab.swift`
  - `backend/src/app/api/dramas/channel/route.ts`

## `wiki/features/theater/index.md`
- 变更类型：新增文档
- 变更章节：全文初始创建
- 变更摘要：新增剧场频道功能文档，收录独立一级 tab、8 个子频道、`GET /api/dramas/channel`、非 `all` 频道合法空态、剧场快捷入口到首页拥有页面的跨 tab 复用、预约榜上下文注入、扫码占位与 `play` 主路径复用。
- 主要来源：
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/TheaterShortcutRoute.kt`
  - `ios/ShortDrama/Sources/App/TabNavigationHostView.swift`
  - `ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift`
  - `ios/ShortDrama/Sources/App/NavigationRouter.swift`
  - `backend/src/app/api/dramas/channel/route.ts`
  - `backend/src/lib/schemas.ts`
  - `backend/src/repositories/mock/drama.mock.repository.ts`

## `wiki/features/app-shell/index.md`
- 变更类型：增量更新
- 变更章节：功能概述
- 变更摘要：将“剧场”从占位一级频道修正为真实 Native 内容入口，并补充剧场内搜索/分类/排行/新剧会切回首页所属导航栈复用既有页面的承载策略。
- 主要来源：
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`
  - `ios/ShortDrama/Sources/App/TabNavigationHostView.swift`
  - `ios/ShortDrama/Sources/App/NavigationRouter.swift`

## `wiki/features/search-discovery/index.md`
- 变更类型：增量更新
- 变更章节：功能概述
- 变更摘要：补充剧场顶部搜索框也会复用首页已有搜索发现页，而不是在剧场 tab 内重复实现搜索页面。
- 主要来源：
  - `android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`
  - `ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift`
  - `ios/ShortDrama/Sources/App/NavigationRouter.swift`

## `wiki/features/classification/index.md`
- 变更类型：增量更新
- 变更章节：功能概述
- 变更摘要：补充剧场“分类”快捷入口也会复用首页分类页，并明确该页面不会在剧场 tab 内重复实现。
- 主要来源：
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/TheaterShortcutRoute.kt`
  - `ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift`
  - `ios/ShortDrama/Sources/App/NavigationRouter.swift`

## `wiki/features/ranking/index.md`
- 变更类型：增量更新
- 变更章节：功能概述、入口与路由
- 变更摘要：补充剧场“排行 / 预约”快捷入口会复用排行页，其中预约快捷入口直达 `all + booking` 上下文；分别记录 Android query route 方案与 iOS `TheaterRankingEntryContext` 方案。
- 主要来源：
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/TheaterShortcutRoute.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`
  - `ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift`
  - `ios/ShortDrama/Sources/Domain/Entities/TheaterRankingEntryContext.swift`
  - `ios/ShortDrama/Sources/App/NavigationRouter.swift`
  - `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift`

## `wiki/features/video-player/index.md`
- 变更类型：增量更新
- 变更章节：功能概述
- 变更摘要：把剧场卡片点击补充为播放器新增入口之一，明确首页、剧场、排行和菜单最近在看四类入口都复用同一条 `play` 主路径。
- 主要来源：
  - `android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`
  - `ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift`
  - `ios/ShortDrama/Sources/App/AppRoute.swift`

## `wiki/api/index.md`
- 变更类型：增量更新
- 变更章节：API 文档索引
- 变更摘要：把 `GET /api/dramas/channel` 纳入 Dramas API 索引，并说明剧场 feed 已形成可消费契约。
- 主要来源：
  - `backend/src/app/api/dramas/channel/route.ts`
  - `backend/src/lib/schemas.ts`

## `wiki/api/dramas.md`
- 变更类型：增量更新
- 变更章节：文档头部、`GET /api/dramas/channel`、修订历史
- 变更摘要：新增剧场 feed 接口文档，补充 query 默认值、`heat` 字段、repository registry 注入、非 `all` 频道合法空态与 `INTERNAL_ERROR` 校验语义。
- 主要来源：
  - `backend/src/app/api/dramas/channel/route.ts`
  - `backend/src/repositories/interfaces/drama.repository.interface.ts`
  - `backend/src/repositories/repository-registry.ts`
  - `backend/src/repositories/mock/drama.mock.repository.ts`
  - `backend/src/lib/schemas.ts`
  - `backend/src/app/api/__tests__/dramas-channel.test.ts`
  - `backend/src/services/drama/drama.service.test.ts`

## `wiki/architecture/overview.md`
- 变更类型：增量更新
- 变更章节：概述、整体架构、当前首页与发现链路承载结构、跨端涉及、技术栈总览、已知限制、修订历史
- 变更摘要：将 PRD-12 纳入系统总览，补充剧场一级 tab、`GET /api/dramas/channel`、剧场 feed 承载、跨 tab 复用首页拥有页面、剧场合法空态与 Native / Web 范围边界。
- 主要来源：
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt`
  - `ios/ShortDrama/Sources/App/TabNavigationHostView.swift`
  - `ios/ShortDrama/Sources/App/NavigationRouter.swift`
  - `ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift`
  - `backend/src/app/api/dramas/channel/route.ts`
  - `backend/src/services/drama/drama.service.ts`
  - `backend/src/lib/schemas.ts`

---
*本文档由 llm-wiki skill 自动维护。*