# 2026-07-26 — PRD-02 首页信息流 wiki 收录

> 触发来源：PRD-02 首页信息流

## wiki/features/homepage-feed/index.md
- **变更类型**：新建
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：新增首页信息流功能文档，按代码事实收录 Android / iOS 首页 Feed 状态机、Backend `GET /api/dramas` canonical contract、首页卡片到 `play` / `detail` 的主路径，以及 Web 不实现 Feed、mall / earn 为 H5 承载的范围边界。
- **主要来源**：`backend/src/app/api/dramas/route.ts`、`backend/src/repositories/mock/drama.mock.repository.ts`、`backend/src/lib/schemas.ts`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`、`docs/specs/2026-07-25-prd-02-homepage-feed/spec.md`、`docs/specs/2026-07-25-prd-02-homepage-feed/qa-test.md`

## wiki/api/dramas.md
- **变更类型**：更新
- **变更章节**：GET /api/dramas / POST /api/dramas / GET /api/dramas/[id] / 修订历史
- **变更摘要**：将 `GET /api/dramas` 从空数组骨架修正为首页 Feed 列表接口，补充 `page/pageSize` query、首页卡片字段、12 条 mock 数据分页行为，以及非法参数当前返回 `VALIDATION_ERROR` 的实际结果；并保留 POST / detail 接口仍为 501 占位的现状。
- **主要来源**：`backend/src/app/api/dramas/route.ts`、`backend/src/app/api/__tests__/dramas.test.ts`、`backend/src/repositories/mock/drama.mock.repository.ts`、`backend/src/lib/schemas.ts`

## wiki/features/data-models/index.md
- **变更类型**：更新
- **变更章节**：功能概述 / 核心逻辑 / 多端实现 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：将 `Drama` 数据模型从旧的详情型字段集修正为首页卡片字段集，明确 `episode_count`、`tags`、`rating` 等当前事实来源，并补充 Android / iOS DTO 与 Entity 的实际映射方式。
- **主要来源**：`backend/src/lib/schemas.ts`、`backend/src/lib/__tests__/schemas.test.ts`、`android/app/src/main/java/com/djs66256/short_drama/data/dto/DramaDto.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/Drama.kt`、`ios/ShortDrama/Sources/Data/DTOs/DramaDTO.swift`、`ios/ShortDrama/Sources/Domain/Entities/Drama.swift`

## wiki/features/app-shell/index.md
- **变更类型**：更新
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：同步首页频道从 PRD-01 的占位页演进为 PRD-02 的 Native Feed 首屏，补充 Backend `GET /api/dramas` 作为首页容器依赖的数据源，并保留 Web 首页壳、H5 mall/earn 边界和其他频道仍为占位的现状。
- **主要来源**：`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`、`backend/src/app/api/dramas/route.ts`、`web/src/features/home/HomeScreen.tsx`、`PRODUCT.md`

## wiki/features/video-player/index.md
- **变更类型**：更新
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：将播放器入口从“首页示例按钮”修正为“移动端首页 Feed 卡片动作”，记录 `drama.id -> play/:id` 的真实导航链路，并补充设备级黑盒仍待补测的限制。
- **主要来源**：`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`、`ios/ShortDrama/Sources/App/AppRoute.swift`、`docs/specs/2026-07-25-prd-02-homepage-feed/qa-test.md`

## wiki/features/index.md
- **变更类型**：更新
- **变更章节**：功能域索引
- **变更摘要**：新增首页信息流功能入口，并同步应用壳、数据模型、播放器的最新描述。

## wiki/api/index.md
- **变更类型**：更新
- **变更章节**：API 文档索引
- **变更摘要**：将 Dramas API 从“骨架”修正为“`GET /api/dramas` 已作为移动端首页 Feed 列表接口落地”。

## wiki/architecture/overview.md
- **变更类型**：更新
- **变更章节**：概述 / 架构设计 / 核心流程调用栈 / 设计决策 / 跨端涉及 / 技术栈总览 / 已知限制 / 修订历史
- **变更摘要**：将系统总览从 PRD-01 的导航骨架状态扩展到 PRD-02 的首页 Feed 架构，补充移动端首页状态机、Backend 首页列表接口、Web 不实现 Feed 与 H5 承载边界。
- **主要来源**：`backend/src/app/api/dramas/route.ts`、`backend/src/repositories/mock/drama.mock.repository.ts`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift`、`web/src/features/home/HomeScreen.tsx`、`PRODUCT.md`
