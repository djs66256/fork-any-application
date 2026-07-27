# 播放器

> 最后更新：2026-07-27
> 覆盖端：Web / Android / iOS / Backend

## 功能概述

播放器当前仍处于跨端路由承载阶段：Web、Android、iOS 都已能根据路由参数进入播放页占位页面并展示 `videoId`，但尚未接入真实视频播放、播放控制、进度上报或后端业务逻辑。PRD-02 之后移动端播放页的代表性入口已经从首页示例按钮切换为首页信息流卡片动作；PRD-05 则进一步把排行页卡片也接入同一条 `play/:id` 主路径，确保首页 Feed 与排行榜单都复用 canonical 播放路由语义。Backend 仍预留 `POST /api/player/start` 与 `POST /api/player/stop` 两个 RESTful 路由，但当前统一返回 501。

- 核心价值：先打通播放页路由、参数透传与首页 Feed / 排行卡片入口，为后续真实播放器能力提供稳定落点
- 覆盖范围：Web、Android、iOS、Backend
- 当前状态：页面路由与参数展示已实现，移动端入口已接入首页信息流和排行列表；播放能力与接口逻辑未实现

## 入口与路由

| 端 | 入口 | 路由 / deeplink | 源文件 |
|----|------|----------------|--------|
| Web | 首页代表性链接 | `/play/[id]` | `web/src/app/play/[id]/page.tsx:14-39`, `web/src/features/home/HomeScreen.tsx:36-38` |
| Android | 首页 Feed 卡片“播放”按钮、排行卡片点击、deeplink | `play/{videoId}`（兼容 `player/{videoId}`）、`djsdrama://play/{id}`、`djsdrama://player/{id}` | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:71-76`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:157-159,200-204`, `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:44-50,94-109`, `android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt:33-36` |
| iOS | 首页 Feed 卡片“观看”按钮、排行卡片点击、deeplink | `play` public route name、`djsdrama://play/{id}` | `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:46-56`, `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift:3-8`, `ios/ShortDrama/Sources/App/AppRoute.swift:39-60`, `ios/ShortDrama/Sources/App/DeeplinkHandler.swift:26-45` |
| Backend | N/A | `POST /api/player/start`, `POST /api/player/stop` | `backend/src/app/api/player/start/route.ts:1-6`, `backend/src/app/api/player/stop/route.ts:1-6` |

## 核心逻辑

### 流程：从首页信息流或排行列表进入播放页占位页

1. 用户从首页 Feed、排行列表或 deeplink 进入播放页。
   - Web：首页仍通过代表性链接进入 `/play/sample`（`web/src/features/home/HomeScreen.tsx:36-38`）。
   - Android：首页 `HomeScreen` 卡片点击时调用 `onOpenPlay(drama.id)`；排行页 `RankingScreen` 点击列表项时同样调用 `onOpenPlay(item.id)`（`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:209-218`, `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt:210-219`）。
   - iOS：首页 `HomeRouteBuilder.playerRoute(for:)` 与排行页 `RankingRouteBuilder.playRoute(for:)` 都把 `drama.id` 映射到 `.player(videoId:)`（`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:46-56,98-112`, `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift:3-8`）。
2. 路由层读取 `videoId` 并把它交给播放页占位 View / ViewModel。
   - Web：`PlayPage` 先 `trim()` + 判空，再渲染 `<PlayerScreen videoId={normalizedId} />`（`web/src/app/play/[id]/page.tsx:9-39`）。
   - Android：`PlayerViewModel` 从 `SavedStateHandle` 读取 `videoId`，必要时回退到通用 `id` key（`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:14-17`）。
   - iOS：`TabNavigationHostView` 在路由命中 `.player(let videoId)` 时构造 `PlayerViewModel(videoId: videoId)`（`ios/ShortDrama/Sources/App/TabNavigationHostView.swift:27-29`）。
3. 页面只展示占位标题和路由参数，不发起真实播放请求。
   - Web：`web/src/features/player/PlayerScreen.tsx:7-25`
   - Android：`android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt:14-34`
   - iOS：`ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift:4-18`
4. Backend 的 `/api/player/start`、`/api/player/stop` 仍为占位接口，调用后会抛出 `Errors.notImplemented(...)`，因此当前前端并未真正接入这些接口（`backend/src/app/api/player/start/route.ts:1-6`、`backend/src/app/api/player/stop/route.ts:1-6`）。

### 边界与异常处理

| 场景 | 处理方式 | 源文件 |
|------|---------|--------|
| Web 路由参数为空或全空白 | `trim()` 后为空即 `notFound()`，不渲染有效播放页 | `web/src/app/play/[id]/page.tsx:9-39` |
| Android deeplink 使用历史 `player` host | 解析后统一映射到 `PendingRoute.Play(videoId)` | `android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt:33-36` |
| Android 页面参数 key 不一致 | `PlayerViewModel` 优先读 `videoId`，再回退通用 `id` | `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:14-17` |
| iOS 冷启动即收到播放 deeplink | 先写入 `pendingRoute`，待 `TabView` ready 后再导航 | `ios/ShortDrama/Sources/App/ShortDramaApp.swift:13-20`, `ios/ShortDrama/Sources/App/NavigationRouter.swift:39-50` |
| 首页 / 排行卡片 `id` 为空 | Android 不触发播放导航；iOS `HomeRouteBuilder` / `RankingRouteBuilder` 返回 `nil` | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:158-170,209-218`, `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt:214-217`, `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift:5-8` |
| Backend 播放接口被调用 | 统一返回 501 `NOT_IMPLEMENTED` 语义 | `backend/src/app/api/player/start/route.ts:4-5`, `backend/src/app/api/player/stop/route.ts:4-5` |

## 多端实现

### Web

- Page 层：`web/src/app/play/[id]/page.tsx:14-39`
- Feature 层：`web/src/features/player/PlayerScreen.tsx:7-25`
- 首页入口：`web/src/features/home/HomeScreen.tsx:36-38`
- 特点：Server Component 先做参数规范化，再委托占位 Feature 渲染

### Android

- 路由定义：`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:44-50,94-109`
- 导航注册：`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:187-243`
- 首页 Feed 入口：`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:177-232`
- 排行入口：`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt:193-243`
- 页面实现：`android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt:14-34`
- 参数读取：`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:14-17`
- 特点：同时兼容 canonical `play` 与 legacy `player` route / deeplink

### iOS

- 路由定义：`ios/ShortDrama/Sources/App/AppRoute.swift:39-60`
- 导航注册：`ios/ShortDrama/Sources/App/TabNavigationHostView.swift:11-31`
- 首页 Feed 入口：`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:73-224`
- 排行入口：`ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift:69-77`, `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift:3-8`
- 页面实现：`ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift:4-18`
- 参数承载：`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:4-11`
- 特点：播放页属于首页 Tab 的子路由，对外公开名为 `play`

### Backend

- 路由文件：`backend/src/app/api/player/start/route.ts:1-6`、`backend/src/app/api/player/stop/route.ts:1-6`
- 当前行为：两个接口都只抛出 `Errors.notImplemented(...)`
- 特点：尚未定义请求体解析、Service 层调用或进度持久化逻辑

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `POST /api/player/start` | [../../api/player.md](../../api/player.md) | 开始播放占位接口，当前返回 501 |
| `POST /api/player/stop` | [../../api/player.md](../../api/player.md) | 停止播放/上报占位接口，当前返回 501 |
| `GET /api/dramas` | [../../api/dramas.md](../../api/dramas.md) | 首页 Feed 提供进入播放页所需的 `drama.id` 与卡片数据 |
| `GET /api/dramas/rankings` | [../../api/dramas.md](../../api/dramas.md) | 排行页提供进入播放页所需的 `drama.id` 与榜单字段 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Web `videoId` | 路由 `params` | 页面级 | 页面渲染时由 App Router 提供，先做非空校验 | `web/src/app/play/[id]/page.tsx:14-39` |
| Android `videoId` | `SavedStateHandle` | 页面级 | 从导航参数恢复，兼容旧 key | `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:14-17` |
| Android `pendingRoute` | `StateFlow<UiState>` | 应用级 | deeplink 先缓存为播放目标，稍后再导航 | `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt:14-38` |
| iOS `videoId` | `PlayerViewModel` 初始化参数 | 页面级 | 由 `AppRoute.player(videoId:)` 直接传入 | `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:5-10` |
| iOS `pendingRoute` | `NavigationRouter.pendingRoute` | 应用级 | 冷启动 deeplink 场景的播放目标暂存 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:11,39-50` |
| 首页 Feed / 排行 `drama.id` | 列表项字段 | 页面级 | 作为移动端播放路由的统一参数来源 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:209-218`, `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt:214-217`, `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:98-112`, `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift:5-8` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 应用壳 | 路由承载 | 播放页依附于移动端应用壳与 Web App Router 的路由骨架 |
| 深链 | 外部入口 | Android/iOS 可通过 deeplink 直接落到播放页 |
| 首页信息流 | 导航入口 | 移动端真实入口之一来自首页 Feed 卡片 |
| 排行体系 | 导航入口 | 移动端另一条真实入口来自排行列表卡片 |

### 外部依赖

| 服务 | 用途 | 接入方式 |
|------|------|---------|
| Backend Dramas API | 提供首页卡片与排行项中的 `drama.id` | `GET /api/dramas`, `GET /api/dramas/rankings` |
| Backend Player API | 未来的播放启动与停止上报 | 目前尚未真正接入，接口仍为 501 占位 |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| 页面仅为占位实现 | 只能验证路由与参数透传，无法验证真实播放体验 | 2026-07-27 | 三端页面都只展示标题与 `videoId` |
| Backend 接口未实现 | 前端无法接入真实播放启动/停止流程 | 2026-07-27 | `POST /api/player/start`、`POST /api/player/stop` 都返回 501 |
| iOS 不兼容 `player` 历史 host | 旧 deeplink 兼容仅在 Android 保留 | 2026-07-27 | iOS 仅解析 `play` |
| Web 仅保留代表性播放入口 | 无法验证 Web 端从真实 Feed / 排行到播放页的业务链路 | 2026-07-27 | Web 本期不实现真实 Feed / 排行 |
| 设备级黑盒仍待补测 | 无法确认真实首页卡片、排行卡片点击后播放页跳转的设备表现 | 2026-07-27 | `docs/specs/2026-07-27-prd-05-ranking/qa-test.md:14-24,123-139` |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-27 | 更新：补充 PRD-05 后播放页真实入口新增排行列表卡片，记录 `drama.id -> play/:id` 在首页 Feed 与排行体系中的共用导航链路 |
| 2026-07-26 | 更新：补充 PRD-02 后播放页真实入口已切换为首页信息流卡片，记录 `drama.id -> play/:id` 的移动端导航链路，并保留 Backend 仍为 501 的事实 |
| 2026-07-25 | 更新：依据现有代码将播放器文档从“已设计但未初始化”修正为“跨端路由占位已落地、后端接口仍为 501”，并补充各端真实入口与参数透传链路 |
| 2026-07-22 | API 路径重命名：`/api/video/play` → `/api/player/start`，新增 `/api/player/stop`，API 定义移至 `wiki/api/player.md` |

---
*本文档由 llm-wiki skill 自动维护，从代码中提取。如有不一致，以代码为准。*