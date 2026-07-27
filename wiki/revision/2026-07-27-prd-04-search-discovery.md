# 2026-07-27 — PRD-04 搜索发现 wiki 收录

> 触发来源：PRD-04 搜索发现

## wiki/features/search-discovery/index.md
- **变更类型**：新建
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：新增搜索发现功能文档，按代码事实收录首页搜索入口、搜索发现页、搜索结果页、热搜榜、本地搜索历史、快捷入口、结果页复用首页卡片与播放/详情主链路，以及 Backend 搜索/热搜接口、deeplink 扩展和 Web 不实现搜索的范围边界。
- **主要来源**：`backend/src/app/api/dramas/search/route.ts`、`backend/src/app/api/dramas/hot-search/route.ts`、`backend/src/lib/schemas.ts`、`backend/src/repositories/mock/drama.mock.repository.ts`、`android/app/src/main/java/com/djs66256/short_drama/feature/search/**`、`android/app/src/main/java/com/djs66256/short_drama/navigation/**`、`ios/ShortDrama/Sources/Features/Search/**`、`ios/ShortDrama/Sources/App/AppRoute.swift`、`ios/ShortDrama/Sources/App/DeeplinkHandler.swift`、`docs/specs/2026-07-26-prd-04-search-discovery/spec.md`、`docs/specs/2026-07-26-prd-04-search-discovery/qa-test.md`

## wiki/features/deeplink/index.md
- **变更类型**：更新
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：将 deeplink 文档从 `open` / `play` / `drama` 扩展到 `search` / `search/result` / `ranking` / `classification` / `new-releases` / `actors`，补充 Android canonical route 与 `player` 兼容别名、iOS 不兼容 `player` host 的差异，以及 Web 搜索页仍为占位的范围边界。
- **主要来源**：`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt`、`ios/ShortDrama/Sources/App/AppRoute.swift`、`ios/ShortDrama/Sources/App/DeeplinkHandler.swift`、`web/src/app/search/page.tsx`、`web/src/app/rankings/page.tsx`

## wiki/api/dramas.md
- **变更类型**：更新
- **变更章节**：GET /api/dramas / GET /api/dramas/search / GET /api/dramas/hot-search / 修订历史
- **变更摘要**：补充搜索与热搜两个新接口的 canonical contract，记录 `q.trim().min(1).max(50)`、title/category 不区分大小写 contains 匹配、超大页码 `200 + data=[]` 行为、热搜 Top 10 种子，以及 route 层仍接 `DramaMockRepository` 的现状。
- **主要来源**：`backend/src/app/api/dramas/search/route.ts`、`backend/src/app/api/dramas/hot-search/route.ts`、`backend/src/lib/schemas.ts`、`backend/src/repositories/mock/drama.mock.repository.ts`、`backend/src/app/api/__tests__/dramas-search.test.ts`、`backend/src/app/api/__tests__/dramas-hot-search.test.ts`、`backend/src/services/drama/drama.service.test.ts`

## wiki/api/index.md
- **变更类型**：更新
- **变更章节**：API 文档索引
- **变更摘要**：将 Dramas API 描述从首页 Feed 列表接口扩展为同时覆盖搜索与热搜能力的聚合入口。

## wiki/architecture/overview.md
- **变更类型**：更新
- **变更章节**：概述 / 架构设计 / 核心流程调用栈 / 设计决策 / 跨端涉及 / 技术栈总览 / 已知限制 / 修订历史
- **变更摘要**：将系统总览从 PRD-02 首页 Feed 扩展到 PRD-04 搜索发现，补充首页搜索入口、搜索发现页与结果页、本地历史、热搜接口、快捷入口承接页、deeplink 扩展，以及 Web 不实现搜索与 mall/earn 继续 H5 承载的范围边界。
- **主要来源**：`backend/src/app/api/dramas/search/route.ts`、`backend/src/app/api/dramas/hot-search/route.ts`、`backend/src/services/drama/drama.service.ts`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/local/SearchHistoryLocalDataSource.kt`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`、`ios/ShortDrama/Sources/Features/Search/ViewModels/SearchResultViewModel.swift`、`web/src/app/search/page.tsx`、`PRODUCT.md`

## wiki/features/index.md
- **变更类型**：更新
- **变更章节**：功能域索引
- **变更摘要**：新增“搜索发现 (Search Discovery)”功能入口，并同步 deeplink 描述扩展到搜索发现相关页面。
