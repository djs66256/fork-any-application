# Wiki 收录报告：PRD-03 完整观看播放器

> 收录日期：2026-07-26
> 对应需求：spec.md

## 收录内容

| wiki 文档 | 操作 | 变更章节 | 说明 |
|-----------|------|---------|------|
| `wiki/features/video-player/index.md` | 更新 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史 | 按 Backend / Android / iOS / Web 实际代码，将播放器修正为“Backend 四个接口已实现并测试、iOS 已接入 AVPlayer 真播、Android 已接入真实页面/状态机/后端契约但播放宿主仍为 placeholder、Web 仍为占位页”，并补充 `play` canonical route、Android `player` alias 兼容、bootstrap 顺序、best-effort stop 与 H5 边界 |
| `wiki/api/player.md` | 更新 | GET /api/player/progress / GET /api/dramas/:id/episodes / POST /api/player/start / POST /api/player/stop / 参数变更记录 / 修订历史 | 将播放器 API 文档从“start/stop 占位接口”增量更新为 PRD-03 首版可用契约，补充 progress 与 episodes 两个接口、header 透传边界、实际错误码与响应结构 |
| `wiki/architecture/overview.md` | 更新 | 概述 / 架构设计 / 核心流程调用栈 / 设计决策 / 跨端涉及 / 技术栈总览 / 已知限制 / 修订历史 | 将系统总览扩展到 PRD-03 完整观看播放器，补充 Backend `progress / episodes / start / stop`、移动端 bootstrap/续播/切集/stop 上报链路，以及 Android placeholder 宿主与 Web / H5 边界 |

## 未修改但已检查的候选文档

| 文档 | 是否修改 | 原因 |
|------|---------|------|
| `wiki/features/data-models/index.md` | 否 | 本轮代码没有新增或修改核心 Schema/Entity 结构，只是消费既有 `Episode`、`PlayerStartRequest`、`PlayerStopRequest` 等模型；现有数据模型文档已覆盖这些事实（`backend/src/lib/schemas.ts:41-52,68-81`） |

## 修订记录

- `wiki/revision/2026-07-26-prd-03-full-player.md` 已创建/更新

## 收录结论

本轮收录坚持“代码为准、spec 为辅”，结论来源如下：

### 直接来自代码的结论

- Backend 已实现并测试 `GET /api/player/progress`、`GET /api/dramas/:id/episodes`、`POST /api/player/start`、`POST /api/player/stop`（`backend/src/app/api/player/progress/route.ts:1-45`、`backend/src/app/api/dramas/[id]/episodes/route.ts:1-20`、`backend/src/app/api/player/start/route.ts:1-47`、`backend/src/app/api/player/stop/route.ts:1-48`、`backend/src/app/api/__tests__/player.progress.test.ts:16-115`、`backend/src/app/api/__tests__/drama-episodes.test.ts:14-103`、`backend/src/app/api/__tests__/player.start.test.ts:11-120`、`backend/src/app/api/__tests__/player.stop.test.ts:13-143`）
- `X-Playback-Session-Id` 仅用于 `progress/start/stop`，不用于 `episodes`（`backend/src/app/api/player/progress/route.ts:12-24`、`backend/src/app/api/player/start/route.ts:12-24`、`backend/src/app/api/player/stop/route.ts:12-24`、`backend/src/app/api/dramas/[id]/episodes/route.ts:13-20`、`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt:39-63`、`ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift:10-107`）
- iOS 已接入真实 `AVPlayer`/`VideoPlayer` 播放、bootstrap、切集、倍速、进度更新、隐藏 tab/navigation bar 与 best-effort stop（`ios/ShortDrama/Sources/Features/Player/Views/Components/NativeVideoPlayerView.swift:4-55`、`ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift:38-52`、`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:115-296`、`ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift:42-257`）
- Android 已接入真实播放器页面、状态机、后端接线、`play` canonical route 与 `player` alias 兼容、bootstrap/切集/倍速 UI/续播/best-effort stop，以及播放器路由下隐藏 app bottom bar；但实际视频宿主仍是 placeholder，未引入 `androidx.media3`，也未看到系统级沉浸式 bars hiding 代码（`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:29-80`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:77-118,123-157,227-235`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt:46-196`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:26-385`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/player/PlaceholderPlayerHost.kt:19-75`）
- Web 播放器仍是占位页，不在 PRD-03 真播范围内（`web/src/app/play/[id]/page.tsx:30-39`、`web/src/features/player/PlayerScreen.tsx:7-20`）
- `PRODUCT.md` 中 mall / earn 是 H5 策略，但当前 Android / iOS 代码仍是占位频道页，不能写成已落地 H5 容器（`PRODUCT.md:22-25`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:188-208`、`ios/ShortDrama/Sources/Features/Shell/Views/PlaceholderTabView.swift:6-22`）

### 由于代码未涉及而未修改的部分

- 数据模型 wiki 未改，因为核心 schema / entity 没有新增字段或新模型，只是现有 Episode / Player 请求模型被播放器主路径继续消费。
- API 文档与系统总览未改，因为现有内容已经与当前代码事实一致。

### spec / 预期 与 实际代码差异

- spec/预期容易把 Android 与 iOS 一并表述为“完整真播”；实际代码里 iOS 已用 `AVPlayer` 真正播放视频，但 Android 当前仍是 `PlaceholderPlayerHost`，不能写成已完成 Media3 原生播放（`ios/ShortDrama/Sources/Features/Player/Views/Components/NativeVideoPlayerView.swift:4-55`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/player/PlaceholderPlayerHost.kt:39-75`）。
- “沉浸式隐藏”在 iOS 可确认的是隐藏 `tabBar` 与 `navigationBar`；Android 可确认的是隐藏应用内 bottom bar，但未看到系统级状态栏/导航栏显式隐藏逻辑（`ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift:38-40`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:77-118,227-235`）。
- public route 语义统一为 `play`；Android 继续保留 `player` alias 兼容，iOS 不保留 `player` host（`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:29-80`、`android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt:28-45`、`ios/ShortDrama/Sources/App/AppRoute.swift:19-28`、`ios/ShortDrama/Sources/App/DeeplinkHandler.swift:23-40`）。

- [x] 已完成本轮 wiki 收录
- [x] 结论均已用代码路径支撑
- [x] 未修改候选项已逐项说明原因
