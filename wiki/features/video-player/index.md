# 播放器

> 最后更新：2026-07-29
> 覆盖端：Web / Android / iOS / Backend

## 功能概述

播放器当前已具备完整的播放历史与最近在看 Backend 契约，以及 Android / iOS 统一复用的播放页主路径。首页 Feed、剧场频道、排行列表、菜单最近在看四类入口都会继续复用既有 `play/:id` 播放语义；PRD-14 又进一步把赚钱中心代表性任务接入同一原生播放器承接层。与 PRD-09 评论系统直接相关的最新现状是：Android 与 iOS 播放器都已经把“评论”从视觉位接成了真实入口，并在当前页面上下文内承载评论抽屉 / sheet；Backend 也已提供 comments API，因此播放器内已可完成评论浏览、发表评论、点赞切换以及未登录写操作拦截。赚钱任务不会在 H5 内直接播放，而是由 Native 打开同一播放器主路径并在自然播放结束时回流结果。

- **核心价值**：统一承载首页 Feed、剧场卡片、排行列表、菜单最近在看与赚钱任务五类入口，并在播放上下文内补齐评论互动能力。
- **覆盖范围**：Web、Android、iOS、Backend。
- **当前状态**：Android / iOS 已接入首页信息流、剧场频道、排行列表、菜单最近在看与赚钱任务五类播放器入口；Backend 已实现 `progress/start/stop/recently-viewed` 契约与 comments API；Web 仍保持占位播放器页，不承载评论能力，赚钱页只负责发起 Native 播放承接。

## 入口与路由

| 端 | 入口 | 路由 / deeplink | 源文件 |
|----|------|----------------|--------|
| Web | 首页代表性链接；赚钱页任务 CTA 发起 Native 承接 | `/play/[id]`；`earn.openTaskPlayer` bridge message | `web/src/app/play/[id]/page.tsx`、`web/src/features/home/HomeScreen.tsx`、`web/src/features/earn/hooks/useEarnPage.ts`、`web/src/features/earn/bridge/earn-bridge.ts` |
| Android | 首页 Feed 卡片“播放”按钮、排行卡片点击、菜单最近在看卡片、赚钱任务、deeplink | `play/{videoId}`（兼容 `player/{videoId}`）、`earn/play?taskId=...&source=earn&returnTarget=/earn&videoId=...`、`djsdrama://play/{id}`、`djsdrama://player/{id}` | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt` |
| iOS | 首页 Feed 卡片“观看”按钮、排行卡片点击、菜单最近在看卡片、赚钱任务、deeplink | `play` public route name、`.earnPlayer(context:)`、`djsdrama://play/{id}` | `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`、`ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift`、`ios/ShortDrama/Sources/App/AppRoute.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Sources/App/DeeplinkHandler.swift` |
| Backend | 菜单最近在看接口 + 播放历史接口 | `GET /api/player/progress`, `GET /api/player/recently-viewed`, `POST /api/player/start`, `POST /api/player/stop` | `backend/src/app/api/player/progress/route.ts`、`backend/src/app/api/player/recently-viewed/route.ts`、`backend/src/app/api/player/start/route.ts`、`backend/src/app/api/player/stop/route.ts` |
| Android | 播放器内评论入口 | `AssistChip(onClick = onOpenComments, ...)`，当前页内打开评论抽屉 | `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/components/PlayerComponents.kt` |
| iOS | 播放器内评论入口 | `actionButton(systemName: "message", title: "评论", action: onComment)`，当前页内打开 comments sheet | `ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerRightActionBar.swift` |
| Backend | 播放器评论接口 | `GET /api/dramas/:id/comments`、`POST /api/dramas/:id/comments`、`POST /api/dramas/:id/comments/:commentId/like` | `backend/src/app/api/dramas/[id]/comments/route.ts`、`backend/src/app/api/dramas/[id]/comments/[commentId]/like/route.ts` |

## 核心逻辑

### 流程：从首页信息流、排行列表、菜单最近在看或赚钱任务进入播放页

1. 用户从首页 Feed、排行列表、菜单最近在看、赚钱任务或 deeplink 进入播放页。
   - Web：首页仍只有代表性链接 `/play/sample`；赚钱页已登录任务点击时不会在 H5 内直接播放，而是发出 `earn.openTaskPlayer` 请求 Native 宿主承接。
   - Android：首页、排行、菜单最近在看都复用 `play/{videoId}`；赚钱页通过 `AppDestination.earnPlay(...)` 把 `taskId/source/returnTarget/videoId` 一并透传给原生承接路由。
   - iOS：首页、排行、菜单最近在看都复用 `.player(videoId:)`；赚钱页则通过 `router.openPlayerFromEarn(_:)` 导航到 `.earnPlayer(context:)`。
2. 菜单入口会先关闭抽屉，再由壳层执行真正的播放导航；赚钱任务入口则直接从 earn tab 内打开原生播放器承接页，不经过首页菜单状态机。
3. 最近在看列表由 Backend 统一返回，移动端只消费当前匿名播放会话的数据。
4. 路由层读取 `videoId` 并交给播放页 View / ViewModel；赚钱任务入口只是在同一播放器主路径上额外挂载 earn 上下文，不创建 H5 内播放器实现。
5. 播放历史仍由 `progress/start/stop` 与播放页本身维护；菜单最近在看与赚钱任务都只是额外入口，不引入新的播放器页面实现或新的 player API。

### 流程：播放器内评论入口已接通真实 comments 容器

1. Android 播放器右侧操作区已把“评论”芯片接成真实回调，不再是 `onClick = {}` 占位。
2. Android `PlayerViewModel.openComments()` 会在 `commentSheetState` 中写入 `isVisible = true` 与当前 `dramaId`；`PlayerScreen` 根据该状态渲染评论 bottom sheet。
3. iOS 播放器右侧操作区同样已改为真实 action button，点击后调用 `PlayerViewModel.openComments()` 并通过 `.sheet` 打开 `CommentSheetView`。
4. 播放器内评论列表、排序、分页、发表评论与点赞都由独立 comments ViewModel 承载，而不是塞入播放器已有播放状态机。
5. Backend 当前已提供播放器评论所需的三条接口：
   - `GET /api/dramas/:id/comments`
   - `POST /api/dramas/:id/comments`
   - `POST /api/dramas/:id/comments/:commentId/like`
6. 因为评论容器仍然是页面内增强而不是独立 route，所以播放页不会离开当前上下文。

### 流程：播放器评论写操作的登录拦截与恢复

1. 评论写操作前，客户端会先检查是否已登录。
2. 未登录时：
   - Android：`PlayerViewModel.onCommentLoginRequired(context)` 保存 `pendingCommentLoginContext` 并发出 `PlayerEffect.RequireLogin(context)`。
   - iOS：`PlayerViewModel.handleCommentLoginRequired(_:)` 保存 `pendingCommentLoginContext`，同时设置 `routeEffect = .requireLogin(context)`。
3. 当前登录承接仍为占位：
   - Android：placeholder dialog / Toast
   - iOS：alert
4. 登录恢复后只重新打开评论抽屉 / sheet：
   - Android：`restoreCommentSheetAfterLogin()`
   - iOS：`restoreCommentContext(_:)`
5. 首版明确**不自动重放**原发送评论或点赞动作。

### 流程：赚钱任务复用原生播放器承接并回流结果

1. H5 `/earn` 点击代表性任务后，通过 `earn.openTaskPlayer` 把 `taskId/source/returnTarget/videoId` 发给 Native 宿主。
2. Android earn route 与 iOS `.earnPlayer(context:)` 都复用现有播放器，而不是新建 earn 专属播放器实现。
3. Native 播放器关闭、后台、容器销毁或出错时，都会回流 `completed=false` 的任务结果；只有自然播放结束时才会产出 `completed=true`。
4. 回流顺序固定为：先 `earn.completeTask`（仅 `completed=true`），再 `earn.restoreContext(reason=task-return)`。
5. 这样既复用了统一 player API / UI 状态机，又把奖励结算语义收敛在 Native 承接层。

### 边界与异常处理

| 场景 | 处理方式 | 源文件 |
|------|---------|--------|
| Web 路由参数为空或全空白 | `trim()` 后为空即 `notFound()`，不渲染有效播放页 | `web/src/app/play/[id]/page.tsx` |
| Android deeplink 使用历史 `player` host | 解析后统一映射到 `PendingRoute.Play(videoId)` | `android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt` |
| Android 页面参数 key 不一致 | `PlayerViewModel` 优先读 `videoId`，再回退通用 `id` | `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` |
| Android 赚钱任务上下文非法 | earn 承接路由直接产出 `completed=false` 的错误结果并返回赚钱页，不进入有效播放完成链路 | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` |
| iOS 冷启动即收到播放 deeplink | 先写入 `pendingRoute`，待 `TabView` ready 后再导航 | `ios/ShortDrama/Sources/App/ShortDramaApp.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift` |
| iOS earn 播放被返回 / 消失 / 后台 / 错误打断 | `PlayerViewModel.finishEarnFlowIfNeeded()` 只上报 `completed=false`，不会伪造已完成结果 | `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` |
| 最近在看 header 缺失或非法 | Backend 直接返回 `INVALID_PLAYBACK_SESSION` 400 | `backend/src/app/api/player/parse-playback-session-id.ts` |
| 最近在看候选记录含脏数据 | 服务端过滤无效 drama / episode 或不匹配关系，允许返回不足 3 条 | `backend/src/services/player/player.service.ts` |
| 点击播放器评论后未登录写操作 | 不自动提交写请求，只缓存评论上下文并恢复评论容器 | `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt`、`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` |

## 多端实现

### Web

- Page 层：`web/src/app/play/[id]/page.tsx`
- Feature 层：`web/src/features/player/PlayerScreen.tsx`
- 首页入口：`web/src/features/home/HomeScreen.tsx`
- 赚钱任务入口：`web/src/features/earn/hooks/useEarnPage.ts`、`web/src/features/earn/bridge/earn-bridge.ts`
- 特点：Server Component 先做参数规范化，再委托占位 Feature 渲染；赚钱页任务只发起 Native 播放承接，不在 H5 内嵌播放器

### Android

- 路由定义：`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`
- 导航注册：`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`
- 首页 Feed 入口：`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`
- 排行入口：`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt`
- 赚钱任务入口：`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`
- 页面实现：`android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt`
- 参数读取：`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt`
- 评论现状：评论入口、评论抽屉、登录恢复上下文都已接通；`CommentBottomSheet` 作为独立模块嵌入播放器页
- 特点：同时兼容 canonical `play` 与 legacy `player` route / deeplink，评论能力以页面内增强方式落地，赚钱任务继续复用同一原生播放器，只是在路由侧额外挂载 earn 上下文与完成回传

### iOS

- 路由定义：`ios/ShortDrama/Sources/App/AppRoute.swift`
- 导航注册：`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`
- 首页 Feed 入口：`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`
- 排行入口：`ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift`、`ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift`
- 赚钱任务入口：`ios/ShortDrama/Sources/App/NavigationRouter.swift`
- 页面实现：`ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift`
- 参数承载：`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift`
- 评论现状：评论入口、comments sheet、登录恢复上下文都已接通；评论能力不离开播放器上下文
- 特点：普通播放页属于首页 Tab 的子路由，对外公开名为 `play`；赚钱任务则通过 `.earnPlayer(context:)` 复用同一 `PlayerViewModel`，并由 ViewModel 统一发出 completed / non-completed 的 earn 结果

### Backend

- 路由文件：`backend/src/app/api/player/progress/route.ts`、`backend/src/app/api/player/recently-viewed/route.ts`、`backend/src/app/api/player/start/route.ts`、`backend/src/app/api/player/stop/route.ts`
- Service 与 header 解析：`backend/src/services/player/player.service.ts`、`backend/src/app/api/player/parse-playback-session-id.ts`
- 当前行为：`progress/start/stop` 维护匿名播放历史，`recently-viewed` 在固定候选窗口内过滤脏数据后最多返回 3 条
- 评论现状：已新增 comments routes、service、repository 与 migration，可直接承载播放器评论列表 / 发评论 / 点赞
- 特点：播放器历史链路与评论链路并存，评论作为 drama 子资源挂到 `/api/dramas/:id/comments*`

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/player/progress` | [../../api/player.md](../../api/player.md) | 播放页 bootstrap 的续播查询接口 |
| `GET /api/player/recently-viewed` | [../../api/player.md](../../api/player.md) | 菜单最近在看数据源，Android / iOS 都复用该接口 |
| `POST /api/player/start` | [../../api/player.md](../../api/player.md) | 开始播放接口，菜单最近在看点击后仍进入同一播放器起播链路 |
| `POST /api/player/stop` | [../../api/player.md](../../api/player.md) | 停止播放 / 保存历史接口 |
| `GET /api/dramas/:id/comments` | [../../api/dramas.md](../../api/dramas.md) | 播放器评论列表接口 |
| `POST /api/dramas/:id/comments` | [../../api/dramas.md](../../api/dramas.md) | 播放器发表评论接口 |
| `POST /api/dramas/:id/comments/:commentId/like` | [../../api/dramas.md](../../api/dramas.md) | 播放器点赞 / 取消点赞接口 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Web `videoId` | 路由 `params` | 页面级 | 页面渲染时由 App Router 提供，先做非空校验 | `web/src/app/play/[id]/page.tsx` |
| Web earn task context | bridge payload | 页面级 | `/earn` 只把 `taskId/source/returnTarget/videoId` 发给宿主，不在 H5 内保留播放器实例 | `web/src/features/earn/hooks/useEarnPage.ts`、`web/src/lib/schemas.ts` |
| Android `videoId` | `SavedStateHandle` | 页面级 | 从导航参数恢复，兼容旧 key | `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` |
| Android `pendingRoute` | `StateFlow<UiState>` | 应用级 | deeplink 与菜单关闭后的播放目标都先缓存，稍后再导航 | `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt` |
| Android 播放器页面状态 | `MutableStateFlow<PlayerUiState>` | 页面级 | 承载播放 bootstrap、选集、倍速、点赞、收藏，以及评论抽屉可见性和登录恢复上下文 | `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerUiState.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` |
| Android 评论状态 | `MutableStateFlow<CommentUiState>` | 组件级 | 承载评论列表、分页、排序、输入、发送中、点赞中 | `android/app/src/main/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentSheetViewModel.kt` |
| Android earn task result | `latestEarnTaskPlayerResult` + `earnTaskPlayerResultSignal` | graph 级 | 赚钱任务播放结束后，把完成/退出结果回传给 earn 容器，由其决定是否触发奖励结算 | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` |
| iOS `videoId` | `PlayerViewModel` 初始化参数 | 页面级 | 由 `AppRoute.player(videoId:)` 直接传入 | `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` |
| iOS `pendingRoute` | `NavigationRouter.pendingRoute` | 应用级 | 冷启动 deeplink 场景的播放目标暂存 | `ios/ShortDrama/Sources/App/NavigationRouter.swift` |
| iOS 播放器页面状态 | `@Published` + ViewModel 私有字段 | 页面级 | 维护播放、选集、倍速、点赞、收藏，以及 comments sheet 可见性、登录恢复上下文与 earn task context | `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` |
| iOS 评论状态 | `@Published` | 组件级 | 承载 `listState`、`appendState`、`selectedSort`、`comments`、`inputText` 等 | `ios/ShortDrama/Sources/Features/Comments/ViewModels/CommentSheetViewModel.swift` |
| iOS earn task result | `NavigationRouter.pendingEarnTaskPlayerResult` | 应用级 | earn 播放器关闭后暂存回传结果，再由 earn 容器转换为 host message | `ios/ShortDrama/Sources/App/NavigationRouter.swift` |
| 当前播放会话 ID | `PlaybackSessionStore` / `KeychainPlaybackSessionStore` | 应用级 | 最近在看、progress、start、stop 统一复用同一个 `X-Playback-Session-Id` | `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt`、`ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift`、`backend/src/app/api/player/parse-playback-session-id.ts` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 应用壳 | 路由承载 | 播放页依附于移动端应用壳与 Web App Router 的路由骨架，菜单入口也由壳层统一关闭后导航 |
| 深链 | 外部入口 | Android / iOS 可通过 deeplink 直接落到播放页 |
| 首页信息流 | 导航入口 | 移动端真实入口之一来自首页 Feed 卡片 |
| 排行体系 | 导航入口 | 移动端另一条真实入口来自排行列表卡片 |
| 菜单面板 | 导航入口 | Android / iOS 菜单中的最近在看卡片会复用播放器主路径 |
| 评论能力 | 页面内增强 | PRD-09 已把评论抽屉 / sheet 接在播放器页内，不新增独立 comments route |
| 赚钱中心 | 导航入口 + 完成回流 | earn H5 不内嵌播放器，而是通过 Native 承接页复用同一播放器，再把 completed / non-completed 结果回流给赚钱容器 |

### 外部依赖

| 服务 | 用途 | 接入方式 |
|------|------|---------|
| Backend Dramas API | 提供首页卡片与排行项中的 `drama.id` | `GET /api/dramas`, `GET /api/dramas/rankings` |
| Backend Player API | 提供续播、最近在看、起播与停止上报 | `GET /api/player/progress`, `GET /api/player/recently-viewed`, `POST /api/player/start`, `POST /api/player/stop` |
| Backend Comments API | 提供播放器评论列表、发评论、点赞 | `GET /api/dramas/:id/comments`, `POST /api/dramas/:id/comments`, `POST /api/dramas/:id/comments/:commentId/like` |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| Web 无菜单最近在看入口 | 无法验证 Web 端与移动端一致的菜单到播放页链路 | 2026-07-28 | Web 本期不涉及菜单面板，首页仍只有代表性播放链接 |
| Web 赚钱页不在 H5 内直接播放任务 | 浏览器环境无法独立验证原生任务播放完成链路 | 2026-07-29 | 赚钱任务必须依赖 Native 宿主打开原生播放器 |
| 菜单最近在看最多只返回 3 条且允许不足 3 条 | 过滤脏数据后可能看到 0-2 条，不承诺继续向后补足 | 2026-07-28 | `RECENTLY_VIEWED_FETCH_LIMIT=10` 只定义候选窗口，不是 offset 补足承诺 |
| 播放器评论登录承接仍是占位方案 | 能验证“拦截 + 恢复评论容器”语义，但不能验证真实登录回流 | 2026-07-29 | Android placeholder dialog；iOS alert |
| Backend comments migration 的本地推送验证仍受历史 migration 阻塞 | 无法在本轮完成 comments migration 的真实 `supabase db push` 验证 | 2026-07-29 | 属于既有环境遗留，不影响当前播放器评论代码链路 |
| 登录恢复不自动重放写操作 | 登录后用户需要自行再次发送评论或再次点赞 | 2026-07-29 | 这是首版设计语义，不是 bug |
| 赚钱任务只有自然播放结束才会产出 `completed=true` | 返回、后台、容器销毁或错误退出都不会结算奖励 | 2026-07-29 | 这是 PRD-14 当前实现的显式业务边界 |
| 设备级黑盒仍待补测 | 无法确认真实菜单开合、连点、最近在看点击、播放器评论抽屉与赚钱任务回流的设备表现 | 2026-07-29 | 当前以代码、自动化测试和 QA 文档为主 |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 更新：同步 PRD-09 评论系统与 PRD-14 赚钱中心落地结果，将 Android / iOS 播放器“评论”从视觉占位修正为真实入口，补充播放器内 comments sheet / bottom sheet、登录恢复上下文、赚钱任务复用原生播放器承接以及完成结果回流给赚钱容器的语义 |
| 2026-07-28 | 更新：同步 PRD-12 剧场频道落地结果，补充剧场卡片点击也复用 canonical `play` 主路径，并明确首页、剧场、排行与菜单最近在看四类入口共用播放器导航语义 |
| 2026-07-28 | 更新：同步 PRD-07 菜单面板落地结果，补充最近在看接口、菜单卡片到 `play` 路由的复用链路、关闭后导航时序与 Web 范围边界 |
| 2026-07-27 | 更新：补充 PRD-05 后播放页真实入口新增排行列表卡片，记录 `drama.id` 映射到 `play/:id` 在首页 Feed 与排行体系中的共用导航链路 |
| 2026-07-26 | 更新：补充 PRD-02 后播放页真实入口已切换为首页信息流卡片，记录 `drama.id` 映射到 `play/:id` 的移动端导航链路，并保留 Backend 仍为 501 的事实 |
| 2026-07-25 | 更新：依据现有代码将播放器文档从“已设计但未初始化”修正为“跨端路由占位已落地、后端接口仍为 501”，并补充各端真实入口与参数透传链路 |
| 2026-07-22 | API 路径重命名：`/api/video/play` → `/api/player/start`，新增 `/api/player/stop`，API 定义移至 `wiki/api/player.md` |

---
*本文档由 llm-wiki skill 自动维护，从代码中提取。如有不一致，以代码为准。*
