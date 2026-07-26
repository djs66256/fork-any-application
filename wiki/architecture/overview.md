# 系统总览 架构文档

> 最后更新：2026-07-26

## 概述

项目是一个多端短剧内容应用的 harness 仓库，覆盖 Web、Android、iOS 三端界面与 Backend 服务端骨架。PRD-01 已完成移动端 5 Tab 导航容器；PRD-02 把 Android / iOS 首页从应用信息占位页推进为 Native 首页信息流，并让 Backend 提供 canonical `GET /api/dramas` 列表接口；PRD-03 则进一步把 Backend / Android / iOS 的播放器主路径打通：Backend 已提供 `GET /api/player/progress`、`GET /api/dramas/:id/episodes`、`POST /api/player/start`、`POST /api/player/stop`，移动端播放页也从“只展示 `videoId` 的占位页”演进为具备 bootstrap、倍速、切集、续播与退出上报语义的真实播放器页面。Web 端播放页仍保持路由占位，商城（mall）与赚钱（earn）继续由 H5 承载，不属于本期 Native 播放器范围（`backend/src/app/api/player/progress/route.ts:1-45`, `backend/src/app/api/dramas/[id]/episodes/route.ts:1-20`, `backend/src/app/api/player/start/route.ts:1-47`, `backend/src/app/api/player/stop/route.ts:1-48`, `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:26-385`, `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:4-303`, `web/src/features/player/PlayerScreen.tsx:7-24`, `PRODUCT.md:22-25`).

- **产品信息来源**：`PRODUCT.md`
- **仓库结构**：monorepo，按 `web/`、`android/`、`ios/`、`backend/` 分目录维护
- **技术标识**：Android/iOS 继续使用 `com.djs66256.short_drama`，移动端 deeplink scheme 为 `djsdrama://`（`android/app/src/main/AndroidManifest.xml:18-27`，iOS scheme 来自 `project.yml`）
- **当前版本**：各端骨架版本仍为 `0.1.0`，但移动端首页与播放器主路径均已具备首版内容消费承载能力

## 架构设计

### 整体架构

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│                              用户界面层                                       │
├──────────────┬─────────────────────────────┬─────────────────────────────────┤
│   Web 前端   │        Android App          │            iOS App              │
│ Next.js 16   │ Kotlin + Compose            │ SwiftUI                         │
│ App Router   │ Navigation Compose          │ TabView + NavigationStack       │
│ 首页/播放仍壳 │ 首页 Native Feed + 播放器状态机 │ 首页 Native Feed + AVPlayer 播放器 │
└──────┬───────┴───────────────┬─────────────┴──────────────┬──────────────────┘
       │                       │                            │
       │   H5 范围 / Web 壳     │ 播放器 bootstrap / alias 兼容 │ 播放器 bootstrap / AVKit 承载
       ▼                       ▼                            ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                            Backend API 服务层                                 │
│  Next.js App Router Route Handlers                                           │
│  ├── /api/health                        已实现                                 │
│  ├── /api/dramas                        已实现：首页 Feed 列表接口               │
│  ├── /api/dramas/:id/episodes          已实现：播放器剧集列表子资源             │
│  └── /api/player/progress|start|stop   已实现：播放器续播 / 启播 / 停播          │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 当前首页与播放器承载结构

| 端 | 一级容器 | 首页承载 | 播放器承载 | 当前状态 |
|----|---------|---------|-----------|---------|
| Web | Next.js App Router 页面树 | 应用信息首页壳 | `/play/[id]` 占位页 | Web 不实现完整播放器 |
| Android | `Scaffold` + `NavigationBar` + nested `NavHost` | `HomeScreen` Feed 状态机 | `PlayerScreen` + `PlayerViewModel` + placeholder player host | 已实现 bootstrap / 选集 / 倍速 / stop 上报；未引入 `androidx.media3` |
| iOS | `TabView` + per-tab `NavigationStack` | `HomeView` Feed 状态机 | `PlayerView` + `PlayerViewModel` + `AVPlayer` | 已实现 bootstrap / 选集 / 倍速 / stop 上报 / 实际视频播放 |
| Backend | Route Handlers | `GET /api/dramas` | `GET /api/player/progress` + `GET /api/dramas/:id/episodes` + `POST /api/player/start|stop` | 已提供 mock 数据与首版播放记录语义 |

### 核心流程调用栈

#### 流程：移动端从首页进入播放器并完成 bootstrap

```text
Android
1. 首页卡片 navigate(play/{dramaId}) 或 deeplink 命中 play/player
2. PlayerViewModel 读取 route 参数并取得 playbackSessionId
3. GET /api/player/progress?dramaId=...   (带 X-Playback-Session-Id)
4. GET /api/dramas/:id/episodes           (不带 X-Playback-Session-Id)
5. 本地解析目标 episode（续播 or 第一条可播集）
6. POST /api/player/start                 (带 X-Playback-Session-Id)
7. PlayerScreen 渲染顶部栏 / 互动栏 / 选集栏 / 倍速面板
8. 切后台 / 离开页面 / 切集 -> best-effort POST /api/player/stop

iOS
1. 首页卡片或 deeplink 命中 AppRoute.player(videoId:)
2. PlayerViewModel 读取 videoId 并映射为 dramaId，取得 Keychain playbackSessionId
3. GET /api/player/progress?dramaId=...   (带 X-Playback-Session-Id)
4. GET /api/dramas/:id/episodes           (不带 X-Playback-Session-Id)
5. 本地解析目标 episode（续播 or 第一条可播集）
6. POST /api/player/start                 (带 X-Playback-Session-Id)
7. NativeVideoPlayerView(AVPlayer) 播放目标资源
8. 返回 / disappear / background -> best-effort POST /api/player/stop
```

| 调用层级 | 平台 | 文件 | 职责 |
|---------|------|------|------|
| 入口 | Android | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:123-157` | 注册 canonical `play` 路由与 `player` alias 转发 |
| 1 | Android | `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:188-297` | 执行 `progress -> episodes -> start` bootstrap，解析续播目标 |
| 2 | Android | `android/app/src/main/java/com/djs66256/short_drama/data/repository/PlayerRepositoryImpl.kt:24-85` | 统一注入 playbackSessionId，并转发到 RemoteDataSource |
| 3 | Android | `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt:39-63` | 定义 progress / episodes / start / stop 接口与 header 范围 |
| UI | Android | `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt:46-196` | 渲染 loading / error / no-resource / content 与底部 sheet |
| 入口 | iOS | `ios/ShortDrama/Sources/App/TabNavigationHostView.swift:9-45` | 在 home tab 注册播放器子路由并注入 PlayerRepository |
| 1 | iOS | `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:157-296` | 执行 bootstrap、续播、切集、best-effort stop |
| 2 | iOS | `ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift:10-108` | 定义 progress / episodes / start / stop endpoint，控制 header 透传范围 |
| 3 | iOS | `ios/ShortDrama/Sources/Features/Player/Views/Components/NativeVideoPlayerView.swift:4-55` | 用 `AVPlayer` 承载实际播放、进度观察与倍速变更 |
| Backend | Backend | `backend/src/app/api/player/progress/route.ts:1-45` | 校验 query + playback header 并返回续播信息 |
| Backend | Backend | `backend/src/app/api/dramas/[id]/episodes/route.ts:1-20` | 返回当前 drama 的剧集列表与 `series_status` |
| Backend | Backend | `backend/src/app/api/player/start/route.ts:1-47`, `backend/src/app/api/player/stop/route.ts:1-48` | 校验 body + header 并处理启播 / 停播 |
| Service | Backend | `backend/src/services/player/player.service.ts:25-127`, `backend/src/services/episode/episode.service.ts:10-35` | 实现续播恢复、播放校验、stop clamp 与剧集排序 |

### 设计决策

| 决策 | 原因 | 影响 |
|------|------|------|
| 路由语义统一使用 `play` | 沿用 PRD-01 / PRD-02 已稳定的播放入口语义，避免播放器阶段再引入新命名 | iOS / Web 只保留 `play`；Android 保留 `player` alias 做兼容 |
| `X-Playback-Session-Id` 仅用于 `progress/start/stop` | 续播身份只影响历史查询与进度写入，不影响通用剧集列表 | `GET /api/dramas/:id/episodes` 保持通用子资源语义，不携带该 header |
| bootstrap 职责在客户端完成 | `progress` 只负责返回历史，`episodes` 只负责提供列表，`start` 只负责在目标集已知后启动播放 | 目标集选择逻辑统一在 Android / iOS ViewModel 实现，便于端侧控制 fallback |
| Android 不引入 `androidx.media3` | 当前仓库未获新增依赖授权，需尊重现有实现事实 | Android 文档只记录当前已落地的页面壳、状态机与 placeholder host，不假设 media3 |
| iOS 直接采用 AVKit | SwiftUI/AVPlayer 已可满足首版真实播放、倍速和进度观察需求 | iOS 首版可以在不新增第三方依赖的前提下落地真实视频播放 |
| Web 仍保持播放器占位 | 本期目标是 Native 完整观看主路径，不扩展到 Web 播放体验 | wiki 需明确 Web player 不在 PRD-03 范围 |
| `mall` / `earn` 继续由 H5 承载 | 这是产品层既有页面承载策略，和播放器主链路无直接关系 | 架构边界继续保持 Native 主业务 + H5 mall/earn 的分工 |

## 跨端涉及

| 端 | 相关模块/文件 | 说明 |
|----|-------------|------|
| Web | `web/src/app/play/[id]/page.tsx`, `web/src/features/player/PlayerScreen.tsx` | 播放页仍为占位路由，不消费播放器 API |
| Android | `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`, `navigation/NavGraph.kt`, `feature/player/viewmodel/PlayerViewModel.kt`, `feature/player/ui/PlayerScreen.kt`, `data/repository/PlayerRepositoryImpl.kt`, `core/storage/PlaybackSessionStore.kt` | 已接入播放器状态机、会话持久化、选集 / 倍速 / stop 上报与 `play/player` 路由兼容 |
| iOS | `ios/ShortDrama/Sources/App/TabNavigationHostView.swift`, `Features/Player/ViewModels/PlayerViewModel.swift`, `Features/Player/Views/PlayerView.swift`, `Features/Player/Views/Components/NativeVideoPlayerView.swift`, `Core/Storage/PlaybackSessionStore.swift` | 已接入播放器状态机、Keychain 会话持久化与 AVPlayer 播放 |
| Backend | `backend/src/app/api/player/progress/route.ts`, `app/api/dramas/[id]/episodes/route.ts`, `app/api/player/start/route.ts`, `app/api/player/stop/route.ts`, `services/player/player.service.ts`, `services/episode/episode.service.ts`, `repositories/mock/episode.mock.repository.ts` | 提供首版播放器接口、剧集列表、续播记录语义与 mock 资源 |

## 技术栈总览

| 层级 | Web | Backend | Android | iOS |
|------|-----|---------|---------|-----|
| 语言 | TypeScript | TypeScript | Kotlin 2.0.21 | Swift 6 |
| UI / 路由框架 | React 19 + Next.js 16 App Router | Next.js 16 Route Handlers | Jetpack Compose + Material3 + Navigation Compose | SwiftUI + TabView + NavigationStack + AVKit |
| 状态管理 | 路由参数 + React 组件状态 | Route Handler 请求级状态 | `StateFlow<PlayerUiState>` + ViewModel | `ObservableObject` + `@Published` |
| 构建工具 | next build | next build | AGP 8.7.0 + Gradle | XcodeGen + Xcode 27 |
| 测试 | Vitest + Testing Library | Vitest | JUnit4 + Turbine + Compose testing helpers | Swift Testing |
| 播放器契约 | 占位路由 | `progress / episodes / start / stop` | Retrofit + DataStore playback session | URLSession + Keychain playback session + `AVPlayer` |

## 已知限制

- Web 端当前未实现完整观看播放器，只提供 `/play/[id]` 占位路由。
- Android 已落地播放器页面壳、状态机和进度链路，但视频宿主仍是 placeholder；当前没有 `androidx.media3` 依赖，也未看到系统级状态栏 / 导航栏显式隐藏逻辑。
- Android 与 iOS 的剧场、商城、赚钱、我的仍是占位频道；其中 mall / earn 的页面承载策略继续是 H5，不在本次播放器 PRD 范围内。
- Backend 当前剧集与播放历史数据仍来自 mock repository，不代表真实内容与真实账户体系。
- 评论 / 分享 / 收藏只实现了页面承载或本地反馈，未接入后端持久化。
- 设备级黑盒验证未自动执行，当前跨端结论仍以代码和自动化测试为主（见 `docs/specs/2026-07-26-prd-03-full-player/qa-test.md:14-39,56-249`）。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-26 | 更新：系统总览同步 PRD-03 完整观看播放器落地结果，补充 Backend `progress / episodes / start / stop`、移动端播放器 bootstrap / 续播 / 倍速 / stop 上报链路，并明确 Web 与 H5 边界、Android 当前仍未引入 `androidx.media3` |
| 2026-07-26 | 更新：系统总览同步 PRD-02 首页信息流落地结果，补充 Backend `GET /api/dramas`、移动端首页状态机、首页卡片到播放/详情页主路径，以及 Web / H5 的范围边界 |
| 2026-07-25 | 更新：系统总览同步 PRD-01 导航骨架落地结果，修正移动端从单页骨架到 5 Tab 容器的架构描述，并补充 Web 路由骨架与 Backend 不变更说明 |
| 2026-07-22 | 从各端代码提取信息，初始创建 |

---
*本文档由 llm-wiki skill 自动维护。*