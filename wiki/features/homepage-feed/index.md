# 首页信息流 (Homepage Feed)

> 最后更新：2026-07-29
> 覆盖端：Android / iOS / Backend（Web 本期不实现）

## 功能概述

首页信息流在 PRD-01 已落地的多 Tab 应用壳之上，为移动端首页补齐首屏内容消费能力。当前 Android 与 iOS 冷启动进入首页后，会请求 Backend `GET /api/dramas` 的第一页数据，并按 loading / content / empty / error 四类状态渲染列表卡片；卡片主次动作继续复用既有 `play/:id` 与 `detail/:id` 路由语义。与 PRD-09 评论系统直接相关的最新现状是：首页 Feed 卡片在 Android 与 iOS 两端都已接入评论入口，评论以内嵌 bottom sheet / sheet 的方式在首页上下文中打开，不新增独立 comments route；未登录写操作会被拦截并只恢复评论抽屉上下文，不自动重放原写操作。PRD-10 则继续把签到浮层挂在首页信息流之上：首页首屏加载成功后，客户端才会查询签到状态，并在没有本地关闭态、评论 sheet / 登录模态冲突时叠加 7 日签到浮层。商城（mall）与赚钱（earn）仍按 H5 承载，不属于本期 Native 首页 Feed。

- **核心价值**：让用户冷启动后直接进入短剧浏览主链路，并可在首页上下文内直接浏览评论、触发签到和发起互动
- **覆盖范围**：Backend 首页 mock 列表接口、Android 首页 Feed、iOS 首页 Feed、首页签到浮层、首页评论入口挂载
- **当前状态**：移动端首页首屏 + Backend mock 数据已实现；卡片现已提供观看 / 评论 / 详情三类动作；首页签到浮层与 7 日签到板也已接入；Web 首页仍保持骨架

## 入口与路由

| 端 | 入口 | 路由 / 触发方式 | 源文件 |
|----|------|----------------|--------|
| Android | 默认首页 Tab | `home` graph 首屏自动触发 `loadIfNeeded()`；卡片动作跳转 `play/{videoId}` / `detail/{dramaId}` | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`, `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` |
| iOS | 默认首页 Tab | `HomeView.task` 自动触发 `loadIfNeeded()`；卡片动作跳转 `.player(videoId:)` / `.dramaDetail(dramaId:)` | `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`, `ios/ShortDrama/Sources/App/AppRoute.swift` |
| Android | 首页卡片 action row「评论」按钮 | 无独立 route；点击后设置 `activeCommentDramaId` 并打开 `CommentBottomSheet` | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` |
| iOS | 首页卡片 action row `Button("评论")` | 无独立 route；点击后设置 `activeCommentSheet` 并打开 `CommentSheetView` | `ios/ShortDrama/Sources/Features/Home/Views/Components/HomeDramaCardView.swift`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` |
| Android | 首页签到浮层 | 无独立 route；`HomeScreen` 在根容器上方叠加 `CheckInPopup` | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:110-123`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/CheckInPopup.kt:37-149` |
| iOS | 首页签到浮层 | 无独立 route；`HomeView` 在根容器上方叠加 `CheckInPopupView` | `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:50-63` |
| Backend | 首页列表接口 | `GET /api/dramas?page&pageSize` | `backend/src/app/api/dramas/route.ts` |
| Backend | 首页签到状态接口 | `GET /api/check-ins/status` | `backend/src/app/api/check-ins/status/route.ts` |
| Backend | 首页签到提交接口 | `POST /api/check-ins` | `backend/src/app/api/check-ins/route.ts` |
| Web | N/A | 首页仍为应用信息骨架，不消费 Feed | `web/src/features/home/HomeScreen.tsx` |

## 核心逻辑

### 流程：移动端首页首屏加载

1. 应用进入首页 Tab 后触发首屏加载。
   - Android 在 `HomeScreen` 首次组合时执行 `viewModel.loadIfNeeded()`。
   - iOS 在 `HomeView.task` 中执行 `await viewModel.loadIfNeeded()`。
2. ViewModel 固定请求第一页，每页 10 条。
   - Android `HomeViewModel` 使用 `FIRST_PAGE = 1`、`FEED_PAGE_SIZE = 10` 调用 `GetDramasUseCase`。
   - iOS `HomeViewModel` 使用 `Constants.firstPage = 1`、`pageSize = 10` 调用 `FetchDramasUseCase.execute(...)`。
3. Data 层统一请求 canonical contract：`/api/dramas?page&pageSize`。
   - Android Retrofit 接口使用 `@Query("page")` 与 `@Query("pageSize")`。
   - iOS `DramaEndpoints.GetDramas` 使用 `/api/dramas` 与 `page/pageSize` query。
4. Backend Route 校验分页参数后，通过 `DramaService -> DramaMockRepository` 返回 `{ data, pagination }`。
5. UI 按状态分支渲染首页。
   - Android：loading / error / empty / list 由 `HomeScreen` 的 `when` 分支驱动。
   - iOS：loading / content / empty / error 由 `HomeView.viewState` 驱动。
6. 卡片动作复用既有首页子路由。
   - Android 点击后导航到 `play/{id}` 与 `detail/{id}`。
   - iOS 通过 `HomeRouteBuilder` 将 `drama.id` 映射到 `.player(videoId:)` 与 `.dramaDetail(dramaId:)`。
7. 与 PRD-09 评论能力相关的当前事实是：首页卡片 action row 已扩展评论入口。
   - Android `HomeDramaCard` 现已接入 `onComment = { activeCommentDramaId = drama.id }`，并在当前页面内渲染 `CommentBottomSheet`。
   - iOS `HomeDramaCardView` 现已增加 `Button("评论")`，并由 `HomeView` 通过 `.sheet(item:)` 承载 `CommentSheetView`。
8. 未登录写评论或点赞时，首页不会直接重放写操作；登录恢复后只重新打开评论抽屉 / sheet。
   - Android：首页缓存 `pendingCommentLoginContext`。
   - iOS：首页使用 `pendingCommentLoginContext` 与 `restoreCommentContext(_:)`。

### 流程：首页首屏之后评估签到浮层

1. 首页内容请求不会被签到状态查询阻塞，客户端只有在首屏 Feed 加载成功后才继续评估签到弹层（`android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:177-205`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:197-249`）。
2. Backend `GET /api/check-ins/status` 使用可选登录 + `X-Installation-Id` 兜底，返回 `server_date`、`today_signed`、`current_streak`、`reward_copy`、`days` 与 `should_show_popup`（`backend/src/app/api/check-ins/status/route.ts:8-18`、`backend/src/services/check-in/check-in.service.ts:111-149`）。
3. 客户端会在服务端资格之上再叠加本地判定：
   - 同一 `server_date` 已关闭则不再展示
   - 评论容器或登录模态存在时不展示
   - 当前会话若已放弃展示，则本次首页停留期间不再补弹（`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:110-123`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:195-232`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:50-63`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:230-247`）。
4. 用户点击“立即签到”后，首页仍停留在当前页面上下文，只在弹层内部切换 loading / signed 状态，并把当天 `server_date` 标记为已处理（`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/CheckInPopup.kt:113-149`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:144-183`）。
5. 提交失败不会中断首页主列表；首页内容继续可浏览，失败反馈只停留在签到浮层局部（`android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:92-111`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:163-182`）。

### 边界与异常处理

| 场景 | 处理方式 | 源文件 |
|------|---------|--------|
| 首次加载成功但无数据 | Android / iOS 均进入空态，而不是回退到旧占位首页 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` |
| 首次加载失败 | 两端统一进入错误态，并暴露用户可触发的重试动作 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` |
| 重试期间重复点击 | Android 用 `requestInFlight` 拦截；iOS 用 `isRequestInFlight` 拦截 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` |
| 首页卡片 `id` 为空 | Android 仅允许非空 id 导航；iOS `HomeRouteBuilder` 返回 `nil` 阻止异常路由 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` |
| 分页参数非法 | Backend 返回 400，当前错误码实际为 `VALIDATION_ERROR` | `backend/src/app/api/__tests__/dramas.test.ts` |
| 大页码 | Backend 返回空数组，但保留正确分页元信息 | `backend/src/app/api/__tests__/dramas.test.ts`、`backend/src/repositories/mock/drama.mock.repository.ts` |
| 首页评论命中未登录写操作 | 不直接发评论 / 点赞请求，只缓存评论登录上下文并恢复评论容器 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` |
| 首页签到状态查询失败 | 首页主内容继续可用，本次不弹签到浮层 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:200-204`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:246-247` |
| 评论 / 登录模态阻塞签到展示 | Android 放弃本次会话签到浮层；iOS 在模态存在时不渲染签到弹层 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:110-123`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:50-63` |

## 多端实现

### Android

- 首页状态源：`HomeUiState`（`isLoading` / `items` / `errorMessage` / `hasLoadedOnce` / `isRetrying`）定义在 `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt`
- 首页视图：`HomeScreen` 用 `LazyColumn` 渲染卡片，错误态和空态内建在同文件中
- 网络契约：`ApiService.getDramas(page, pageSize)` 已从 `page_size` 收口到 `pageSize`
- 评论相关现状：首页已新增 `activeCommentDramaId` 与 `pendingCommentLoginContext`，并以 `CommentBottomSheet` 承载评论能力
- 签到相关现状：首页已新增 `checkInPopup` 状态、`CheckInPopup` 组件与本地 dismissed `server_date`，并在 `HomeScreen` 上与评论 / 登录模态互斥
- 自动化证据：`HomeViewModelTest`、`HomeScreenTest`、`RoutesTest` 覆盖状态机、卡片 meta 和路由映射；评论接线与共享卡片签名兼容性也已回归；QA 文档补充签到状态机已由自动化验证（`docs/specs/2026-07-29-prd-10-signin-messages/qa-test.md:281-283`）
- 实现特征：当前 Android 封面区仍使用 `DramaCoverPlaceholder` 展示占位视觉，不直接加载远程图片

### iOS

- 首页状态源：`HomeViewModel.ViewState` 使用 `.loading / .content / .empty / .error` 明确建模
- 首页视图：`HomeView` 根据 `viewState` 切换列表、空态、错误态与 loading
- 网络契约：`DramaRemoteDataSource.fetchDramas` 已使用 `/api/dramas?page&pageSize` 并解码 `{ data, pagination }`
- 评论相关现状：首页卡片组件 `HomeDramaCardView` 现已提供评论按钮，`HomeViewModel` / `HomeView` 已承载 comments sheet 与登录恢复上下文
- 签到相关现状：首页已增加 `checkInPopupState`、`evaluateCheckInPopupIfNeeded()`、`CheckInPopupView` 与按 `serverDate` 的本地关闭态
- 自动化证据：`APIClientTests`、`DramaRepositoryTests`、`HomeViewModelTests`、`NavigationRouterTests` 覆盖 endpoint/query、canonical response 解码、状态机与路由映射；评论宿主状态与签到状态机也已在首页 ViewModel 测试中补充
- 实现特征：iOS 通过 `AsyncImage` 尝试展示封面，失败时回退到占位图

### Backend

- Route：分页参数由 Zod 约束为 `page >= 1`、`1 <= pageSize <= 100`
- Schema：`DramaSchema` 已统一到首页卡片字段集 `episode_count + tags`
- Repository：`HOMEPAGE_DRAMAS` 预置 12 条首页 mock 数据，并保留稳定分页顺序
- 评论相关现状：首页接口本身仍只负责 Feed 列表，不直接返回评论子资源；评论入口所需的真实数据由独立 comments API 提供
- 签到相关现状：首页通过独立 `check-ins` route 组合 installationId、服务端业务日与 7 日签到板，不污染 `GET /api/dramas` contract
- 自动化证据：`dramas.test.ts`、`schemas.test.ts`、`drama.mock.repository.test.ts`、`drama.service.test.ts` 覆盖 contract、schema、分页与边界行为；PRD-10 QA 文档补充了 check-ins contract 与跨端一致性测试（`docs/specs/2026-07-29-prd-10-signin-messages/qa-test.md:266-283`）

### Web

- 首页仍展示应用信息与代表性链接，不消费首页 Feed
- 本期设计范围明确 Web 不实现首页信息流、签到浮层与 comments UI

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/dramas` | [../../api/dramas.md](../../api/dramas.md) | 首页首屏 Feed 的唯一数据来源 |
| `GET /api/check-ins/status` | [../../api/check-ins.md](../../api/check-ins.md) | 首页首屏后的签到状态查询 |
| `POST /api/check-ins` | [../../api/check-ins.md](../../api/check-ins.md) | 首页签到浮层提交 |
| `GET /api/dramas/:id/comments` | [../../api/dramas.md](../../api/dramas.md) | 首页评论抽屉读取评论列表 |
| `POST /api/dramas/:id/comments` | [../../api/dramas.md](../../api/dramas.md) | 首页评论抽屉发表评论 |
| `POST /api/dramas/:id/comments/:commentId/like` | [../../api/dramas.md](../../api/dramas.md) | 首页评论抽屉点赞 / 取消点赞 |
| `GET /api/dramas/[id]` | [../../api/dramas.md](../../api/dramas.md) | 详情接口仍未实现，首页详情页目前只复用既有客户端占位路由 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Android `HomeUiState` | `MutableStateFlow` | 页面级 | 首页唯一状态源，聚合 loading / items / error / retry | `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt` |
| Android `requestInFlight` | ViewModel 私有字段 | 页面级 | 阻止重复请求和重复重试 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt` |
| Android `activeCommentDramaId` / `pendingCommentLoginContext` | Compose state | 页面级 | 首页评论容器可见性与登录恢复上下文 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` |
| Android `checkInPopup` / `checkInPopupAbandoned` | ViewModel `StateFlow` + 私有字段 | 页面级 | 首页签到弹层状态与本会话是否放弃再弹 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:33-40,53-55,129-136,195-232` |
| Android dismissed `server_date` | DataStore Preferences | 设备级 | 按服务端业务日记录签到浮层关闭 / 已处理状态 | `android/app/src/main/java/com/djs66256/short_drama/core/storage/CheckInLocalStore.kt:40-54` |
| iOS `viewState` | `@Published` | 页面级 | 首页列表、空态、错误态、loading 的统一来源 | `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` |
| iOS `hasLoaded` / `isRequestInFlight` | ViewModel 私有字段 | 页面级 | 避免重复自动请求和重试并发 | `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` |
| iOS `activeCommentSheet` / `pendingCommentLoginContext` | `@Published` | 页面级 | 首页评论 sheet 可见性与登录恢复上下文 | `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` |
| iOS `checkInPopupState` / `hasEvaluatedCheckInPopup` | `@Published` + ViewModel 私有字段 | 页面级 | 首页签到浮层状态与首屏后只评估一次的约束 | `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:42-47,60-63,230-268` |
| iOS dismissed `serverDate` | `UserDefaults` | 设备级 | 按 `serverDate` 记录首页签到浮层关闭态 | `ios/ShortDrama/Sources/Core/Storage/InstallationIdStore.swift:44-69` |
| Backend `pagination` | JSON 响应 | 请求级 | 返回 `page / page_size / total / total_pages`，客户端本期只消费第一页 | `backend/src/repositories/interfaces/drama.repository.interface.ts`、`backend/src/repositories/mock/drama.mock.repository.ts` |
| Backend `server_date` | JSON 响应字段 | 请求级 | 首页签到本地关闭态的唯一服务端权威日期 | `backend/src/services/check-in/check-in.service.ts:88-90,140-149` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 应用壳 | 默认承载容器 | 首页信息流依附于 PRD-01 的首页 Tab / home graph，而不是独立顶级入口 |
| 签到能力 | 首页 overlay 承载 | PRD-10 签到浮层必须挂在首页首屏内容之上，并与评论 / 登录容器互斥 |
| 播放器 | 路由跳转 | 首页卡片主动作使用 `drama.id -> play/:id` 进入播放页 |
| 剧集详情 | 路由跳转 | 首页卡片次动作使用 `drama.id -> detail/:id` |
| 评论能力 | 入口挂载 + 页面内容器 | PRD-09 已在首页卡片现有 action row 中扩展评论入口，并以内嵌 comments sheet 承载互动 |
| 数据模型 | 共享字段约束 | 首页卡片字段与 `DramaSchema` / 客户端 Entity 保持对齐 |

### 外部依赖

| 服务 / 框架 | 用途 | 接入方式 |
|-------------|------|---------|
| Next.js Route Handlers | Backend 列表接口、签到接口与评论接口承载 | `backend/src/app/api/dramas/route.ts`、`backend/src/app/api/check-ins/*`、`backend/src/app/api/dramas/[id]/comments/*` |
| Retrofit | Android 首页拉取 Feed、签到状态与首页评论请求 | `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` |
| URLSession + APIClient | iOS 首页拉取 Feed、签到状态与首页评论请求 | `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift`、`ios/ShortDrama/Sources/Data/DataSources/CheckInRemoteDataSource.swift`、`ios/ShortDrama/Sources/Data/DataSources/CommentRemoteDataSource.swift` |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| 本期只消费第一页 | 不支持首页 Feed 下拉刷新、自动加载更多或第二页浏览 | 2026-07-26 | `HomeViewModel` / `GetDramasUseCase` 均固定使用 `page=1,pageSize=10` |
| 首页评论登录承接仍是占位方案 | 能验证“拦截 + 恢复评论容器”语义，但不能验证真实登录回流 | 2026-07-29 | Android placeholder dialog；iOS alert |
| 首页签到最终展示仍依赖客户端上下文 | Backend 只能返回资格状态，无法知道评论 / 登录模态冲突 | 2026-07-29 | 有意保持前后端分层 |
| Web 首页未实现 Feed | Web 仍只能查看应用骨架与示例链接 | 2026-07-26 | 属于明确范围外 |
| 商城 / 赚钱不属于 Native Feed | 首页 Feed 不覆盖 mall / earn 业务内容 | 2026-07-26 | `PRODUCT.md:22-25` 明确它们由 H5 承载 |
| Android 当前未加载真实封面图片 | Android 首页封面区仍是占位视觉，不是网络图片渲染 | 2026-07-26 | `DramaCoverPlaceholder` 仅根据 `coverUrl` 是否为空显示提示 |
| 设备级黑盒仍待补测 | 移动端真实点击、首页签到浮层与首页评论抽屉交互、错误态和恢复时序未在设备/模拟器上执行 | 2026-07-29 | 本轮 QA 以自动化测试、构建和代码检查为主 |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 更新：同步 PRD-10 首页签到浮层落地结果，补充首屏后评估签到状态、服务端业务日、本地关闭态与评论 / 登录模态互斥 |
| 2026-07-29 | 更新：同步 PRD-09 评论系统落地结果，补充 Android / iOS 首页卡片已新增评论入口、首页内 comments sheet / bottom sheet 宿主状态，以及登录恢复仅恢复评论上下文的语义 |
| 2026-07-26 | 初始创建：收录 PRD-02 首页信息流的移动端首屏状态机、Backend canonical contract 与卡片到播放/详情页的主路径实现 |

---
*本文档由 llm-wiki skill 自动维护。*