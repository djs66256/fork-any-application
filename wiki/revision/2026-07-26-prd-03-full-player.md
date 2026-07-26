# 2026-07-26 — PRD-03 完整观看播放器 wiki 收录

> 触发来源：PRD-03 完整观看播放器

## wiki/features/video-player/index.md
- **变更类型**：更新
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：按真实代码将播放器文档修正为“Backend 四个接口已实现并测试、iOS 已接入 AVPlayer 真播、Android 已接入真实页面/状态机/后端契约但播放宿主仍为 placeholder、Web 仍为占位页”，并补充 `play` canonical route、Android `player` alias 兼容、`X-Playback-Session-Id` 使用边界、bootstrap 顺序、best-effort stop 与 H5 策略边界。
- **主要来源**：`backend/src/app/api/player/progress/route.ts`、`backend/src/app/api/dramas/[id]/episodes/route.ts`、`backend/src/app/api/player/start/route.ts`、`backend/src/app/api/player/stop/route.ts`、`backend/src/services/player/player.service.ts`、`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/player/PlaceholderPlayerHost.kt`、`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift`、`ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift`、`ios/ShortDrama/Sources/Features/Player/Views/Components/NativeVideoPlayerView.swift`、`web/src/features/player/PlayerScreen.tsx`、`PRODUCT.md`

## wiki/api/player.md
- **变更类型**：更新
- **变更章节**：GET /api/player/progress / GET /api/dramas/:id/episodes / POST /api/player/start / POST /api/player/stop / 参数变更记录 / 修订历史
- **变更摘要**：将播放器 API 文档从“start/stop 占位接口”增量更新为 PRD-03 首版可用契约，补充 progress 与 episodes 两个新接口、`X-Playback-Session-Id` 仅用于 `progress/start/stop` 的边界，以及实际错误码、响应结构与服务端行为。
- **主要来源**：`backend/src/app/api/player/progress/route.ts`、`backend/src/app/api/dramas/[id]/episodes/route.ts`、`backend/src/app/api/player/start/route.ts`、`backend/src/app/api/player/stop/route.ts`、`backend/src/services/player/player.service.ts`、`backend/src/services/episode/episode.service.ts`、`backend/src/lib/schemas.ts`、`backend/src/app/api/__tests__/player.progress.test.ts`、`backend/src/app/api/__tests__/drama-episodes.test.ts`、`backend/src/app/api/__tests__/player.start.test.ts`、`backend/src/app/api/__tests__/player.stop.test.ts`

## wiki/architecture/overview.md
- **变更类型**：更新
- **变更章节**：概述 / 架构设计 / 核心流程调用栈 / 设计决策 / 跨端涉及 / 技术栈总览 / 已知限制 / 修订历史
- **变更摘要**：将系统总览从 PRD-02 首页 Feed 状态扩展到 PRD-03 完整观看播放器，补充 Backend `progress / episodes / start / stop`、移动端 bootstrap/续播/切集/stop 上报链路，并明确 Android 当前仍是 placeholder 播放宿主、Web 不在播放器范围、mall/earn 仍属产品层 H5 策略边界。
- **主要来源**：`backend/src/app/api/player/progress/route.ts`、`backend/src/app/api/dramas/[id]/episodes/route.ts`、`backend/src/app/api/player/start/route.ts`、`backend/src/app/api/player/stop/route.ts`、`backend/src/services/player/player.service.ts`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/player/PlaceholderPlayerHost.kt`、`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift`、`ios/ShortDrama/Sources/Features/Player/Views/Components/NativeVideoPlayerView.swift`、`web/src/features/player/PlayerScreen.tsx`、`PRODUCT.md`

## wiki/features/data-models/index.md
- **变更类型**：校验无需改动
- **变更章节**：N/A
- **变更摘要**：现有数据模型文档已覆盖 `Episode`、`PlayerStartRequest`、`PlayerStopRequest` 等与播放器相关的当前事实；本轮重点在功能链路与 API 契约，不额外扩写未被用户明确要求的数据模型章节。
- **主要来源**：`wiki/features/data-models/index.md`、`backend/src/lib/schemas.ts`

## docs/specs/2026-07-26-prd-03-full-player/wiki.md
- **变更类型**：新建
- **变更章节**：收录内容 / 修订记录 / 收录结论
- **变更摘要**：新增 PRD-03 wiki 收录报告，说明本轮实际更新了哪些 wiki、哪些候选 wiki 因现有内容已准确而保持不变，并强调收录结论以代码与自动化测试为准。
- **主要来源**：`wiki/features/video-player/index.md`、`wiki/api/player.md`、`wiki/architecture/overview.md`、`wiki/features/data-models/index.md`、`docs/specs/2026-07-26-prd-03-full-player/spec.md`、`docs/specs/2026-07-26-prd-03-full-player/qa-test.md`

---
*本文档由 llm-wiki skill 自动维护。*