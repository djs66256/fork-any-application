# 2026-07-29 — PRD-09 评论系统 wiki 同步

> 触发来源：PRD-09 评论系统

## wiki/features/video-player/index.md
- **变更类型**：更新
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / 状态管理 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：将 Android / iOS 播放器“评论”从视觉占位修正为真实入口，补充播放器内 comments bottom sheet / sheet、登录恢复上下文，以及 Backend comments API 已落地的事实。
- **主要来源**：`android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/components/PlayerComponents.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/comments/`、`ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerRightActionBar.swift`、`ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift`、`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift`、`ios/ShortDrama/Sources/Features/Comments/`、`backend/src/app/api/dramas/[id]/comments/route.ts`、`backend/src/app/api/dramas/[id]/comments/[commentId]/like/route.ts`

## wiki/features/homepage-feed/index.md
- **变更类型**：更新
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 边界与异常处理 / 多端实现 / 状态管理 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：把 Android / iOS 首页 Feed 卡片“无评论入口”的旧结论修正为“观看 / 评论 / 详情”三按钮结构，并同步记录首页 comments sheet / bottom sheet 宿主状态与登录恢复语义。
- **主要来源**：`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/comments/`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift`、`ios/ShortDrama/Sources/Features/Home/Views/Components/HomeDramaCardView.swift`、`backend/src/app/api/dramas/route.ts`

## wiki/features/ranking/index.md
- **变更类型**：保持不变（复核）
- **变更章节**：—
- **变更摘要**：复核后确认排行预约仍是评论登录拦截模式的参考来源，且用户侧认证仍是 skeleton auth，该文档与当前代码一致，无需本轮重写。
- **主要来源**：`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt`、`ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift`、`backend/src/middleware/auth.ts`

## wiki/architecture/overview.md
- **变更类型**：更新
- **变更章节**：概述 / 架构设计 / 核心流程调用栈 / 设计决策 / 跨端涉及 / 技术栈总览 / 已知限制 / 修订历史
- **变更摘要**：把评论能力从“未实现”修正为“Backend + Android + iOS 首版已落地”，补充首页与播放器评论入口、comments API / service / migration、以及继续沿用 skeleton auth 与“只恢复上下文”语义的事实。
- **主要来源**：`backend/src/app/api/dramas/[id]/comments/route.ts`、`backend/src/services/comment/comment.service.ts`、`backend/supabase/migrations/20260729000100_add_comments_tables.sql`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift`、`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift`

## wiki/features/auth/index.md
- **变更类型**：保持不变（复核）
- **变更章节**：—
- **变更摘要**：复核后确认认证体系文档对 Admin 真实 JWT + role 校验与移动端用户侧 skeleton auth 的分层描述仍然准确，可直接作为评论能力的认证基线引用。
- **主要来源**：`backend/src/middleware/auth.ts`、`backend/src/app/api/admin/auth/login/route.ts`、`backend/src/app/api/dramas/[id]/comments/route.ts`

## wiki/features/comments/index.md
- **变更类型**：更新
- **变更章节**：功能概述 / 入口与路由 / 当前现状 / 核心逻辑 / 状态管理落点 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：将评论能力文档从“仅 spec 已定稿、代码未落地”整体改写为“Backend + Android + iOS 首版已落地”，补充首页/播放器入口、评论抽屉、登录恢复上下文、comments routes、repository registry 与 migration 现状。
- **主要来源**：`backend/src/app/api/dramas/[id]/comments/route.ts`、`backend/src/app/api/dramas/[id]/comments/[commentId]/like/route.ts`、`backend/src/services/comment/comment.service.ts`、`backend/src/repositories/repository-registry.ts`、`backend/supabase/migrations/20260729000100_add_comments_tables.sql`、`android/app/src/main/java/com/djs66256/short_drama/feature/comments/`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt`、`ios/ShortDrama/Sources/Features/Comments/`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift`、`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift`

## wiki/features/index.md
- **变更类型**：更新
- **变更章节**：功能域索引
- **变更摘要**：同步首页信息流、播放器、评论能力三个索引摘要，避免继续把评论描述成“未实现 / 占位”。

---
*本文档由 llm-wiki skill 自动维护。*