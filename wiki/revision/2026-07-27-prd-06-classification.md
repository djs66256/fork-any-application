# 2026-07-27 — PRD-06 分类浏览 wiki 收录

> 触发来源：PRD-06 分类浏览

## wiki/features/classification/index.md
- **变更类型**：新建
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：新增分类浏览功能文档，收录搜索发现页“分类”入口、`全部 / 男频 / 女频` 顶部性别 Tab、固定三维度标签矩阵、标签点击复用搜索结果页、Backend `GET /api/dramas/tags` 与 tags 搜索扩展、Android / iOS 分类页状态机与双向滚动同步策略。
- **主要来源**：`backend/src/app/api/dramas/tags/route.ts`、`backend/src/lib/schemas.ts`、`backend/src/services/drama/drama.service.ts`、`backend/src/repositories/interfaces/drama.repository.interface.ts`、`backend/src/repositories/mock/drama.mock.repository.ts`、`backend/src/repositories/supabase/drama.supabase.repository.ts`、`android/app/src/main/java/com/djs66256/short_drama/feature/search/model/SearchQuickEntry.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/classification/ui/ClassificationScreen.kt`、`ios/ShortDrama/Sources/App/AppRoute.swift`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`、`ios/ShortDrama/Sources/Domain/Entities/QuickEntry.swift`、`ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift`、`ios/ShortDrama/Sources/Features/Classification/ViewModels/ClassificationViewModel.swift`、`ios/ShortDrama/Sources/Features/Classification/Views/ClassificationHomeView.swift`、`ios/ShortDrama/Sources/Data/DTOs/ClassificationTagsResponseDTO.swift`、`ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift`、`PRODUCT.md`

## wiki/api/dramas.md
- **变更类型**：更新
- **变更章节**：GET /api/dramas / GET /api/dramas/search / GET /api/dramas/hot-search / GET /api/dramas/tags / 修订历史
- **变更摘要**：补充首页/搜索共享 `tags` 字段事实；将搜索命中规则从 `title + category` 扩展为 `title + category + tags`；新增 `GET /api/dramas/hot-search` 与 `GET /api/dramas/tags` 文档，记录热搜静态列表、`gender` query、固定三维度、`all` 去重合并与当前 mock repository 运行事实。
- **主要来源**：`backend/src/lib/schemas.ts`、`backend/src/app/api/dramas/search/route.ts`、`backend/src/app/api/dramas/hot-search/route.ts`、`backend/src/app/api/dramas/tags/route.ts`、`backend/src/services/drama/drama.service.ts`、`backend/src/repositories/mock/drama.mock.repository.ts`、`backend/src/repositories/supabase/drama.supabase.repository.ts`、`backend/src/app/api/__tests__/dramas-search.test.ts`、`backend/src/app/api/__tests__/dramas-hot-search.test.ts`、`backend/src/lib/__tests__/schemas.test.ts`

## wiki/api/index.md
- **变更类型**：更新
- **变更章节**：API 文档索引
- **变更摘要**：将 Dramas API 描述扩展为首页 Feed + 搜索 + 热搜 + 排行 + 分类 tags + 预约接口均已落地。

## wiki/features/index.md
- **变更类型**：更新
- **变更章节**：功能域索引
- **变更摘要**：新增“分类浏览”功能入口，并同步更新搜索发现、应用壳、数据模型、深链摘要中的 classification 事实。

## wiki/features/app-shell/index.md
- **变更类型**：更新
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：同步首页频道从“承载 Feed + 排行”扩展为“承载 Feed + 搜索发现 + 排行 + 分类”，补充移动端 classification 子路由、Backend `GET /api/dramas/tags` 与 Web 仍无真实分类页的现状。
- **主要来源**：`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`、`ios/ShortDrama/Sources/App/AppRoute.swift`、`backend/src/app/api/dramas/tags/route.ts`、`web/src/app/search/page.tsx`、`PRODUCT.md`

## wiki/features/deeplink/index.md
- **变更类型**：更新
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 依赖关系 / 修订历史
- **变更摘要**：把 `classification` 从占位 deeplink 承接修正为真实分类页入口，并同步 search/ranking/classification 三条发现链路的当前代码事实。
- **主要来源**：`android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`ios/ShortDrama/Sources/App/DeeplinkHandler.swift`、`ios/ShortDrama/Sources/App/AppRoute.swift`

## wiki/features/data-models/index.md
- **变更类型**：更新
- **变更章节**：功能概述 / 核心逻辑 / 多端实现 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：新增 classification 相关数据模型约束，包括 `ClassificationGender`、固定维度 key、`ClassificationTagsResponse`，并记录 Android / iOS DTO / Entity 的对齐方式。
- **主要来源**：`backend/src/lib/schemas.ts`、`backend/src/repositories/interfaces/drama.repository.interface.ts`、`android/app/src/main/java/com/djs66256/short_drama/data/dto/ClassificationTagsResponseDto.kt`、`ios/ShortDrama/Sources/Data/DTOs/ClassificationTagsResponseDTO.swift`

## wiki/architecture/overview.md
- **变更类型**：更新
- **变更章节**：概述 / 架构设计 / 核心流程调用栈 / 设计决策 / 跨端涉及 / 技术栈总览 / 已知限制 / 修订历史
- **变更摘要**：将系统总览从“首页 Feed + 搜索发现 + 排行”扩展到“首页 Feed + 搜索发现 + 排行 + 分类浏览”，补充分类页经由搜索发现进入、调用 `GET /api/dramas/tags`、点击标签复用搜索结果页的主链路与 Native / Web 范围边界。
- **主要来源**：`backend/src/app/api/dramas/tags/route.ts`、`backend/src/repositories/mock/drama.mock.repository.ts`、`backend/src/repositories/supabase/drama.supabase.repository.ts`、`android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/classification/ui/ClassificationScreen.kt`、`ios/ShortDrama/Sources/Features/Classification/ViewModels/ClassificationViewModel.swift`、`ios/ShortDrama/Sources/Features/Classification/Views/ClassificationHomeView.swift`、`PRODUCT.md`
