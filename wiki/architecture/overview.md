# 系统总览 架构文档

> 最后更新：2026-08-03

## 概述

项目是一个多端短剧内容应用的 harness 仓库，覆盖 Web、Android、iOS 三端界面与 Backend 服务端骨架。当前主线真实能力已经包含：

- PRD-01：移动端 5 Tab 导航容器
- PRD-02：首页信息流与 `GET /api/dramas`
- PRD-05：搜索发现与排行浏览链路
- PRD-06：分类浏览链路
- PRD-07：首页汉堡菜单、最近在看与 `GET /api/player/recently-viewed`
- PRD-08：手机号验证码登录、自动注册、会话恢复 / refresh / logout，以及“我的”频道真实承载
- PRD-09：评论系统首版落地，首页与播放器都已接入 comments sheet / bottom sheet，Backend 已提供 comments API / service / migration
- PRD-10：首页签到浮层、7 日签到板、菜单消息预览、消息中心，以及 `check-ins` / `messages` API contract
- PRD-11：菜单“我的预约”真实资产页、`GET /api/users/me/bookings`、匿名登录承接与 booking route 回流
- PRD-12：剧场频道与 `GET /api/dramas/channel`
- PRD-13：商城频道 H5 容器接入、Web `/mall` / `/mall/product/[id]`、`GET /api/mall/products`、商城搜索 / 登录 bridge 与商城上下文恢复
- PRD-14：赚钱频道 H5 容器接入、`GET /api/earn/overview`、`POST /api/earn/complete-task`、earn 专属 bridge / host sync、Native 登录承接、播放器任务承接与回流结算顺序

当前移动端已经形成“首页发现 + 签到 + 菜单消息 + 预约资产 + 剧场入口 + 商城 H5 容器 + 赚钱 H5 容器 + 认证闭环 + 评论互动 + 预约鉴权”的主路径；Web 端继续保持路由骨架、H5 页面与管理后台，不在本期实现与移动端对等的用户端底部导航、菜单、剧场、评论或完整账号体验；商城（mall）与赚钱（earn）继续由 H5 承载，且 Android / iOS 已分别接入真实 Native 容器。

- **产品信息来源**：`PRODUCT.md`
- **仓库结构**：monorepo，按 `web/`、`android/`、`ios/`、`backend/` 分目录维护
- **技术标识**：Android / iOS 继续使用 `com.djs66256.short_drama`，移动端 deeplink scheme 为 `djsdrama://`
- **当前版本**：移动端首页已具备 Feed、签到浮层、菜单面板、消息系统、预约资产页、剧场频道、商城频道、赚钱频道、搜索发现、排行浏览、分类浏览、“我的”登录 / 设置承载与评论互动；Backend 已补齐 Auth API、check-ins API、messages API、个人资产 API、comments API、最近在看接口、剧场 / 发现 / 商城 / 赚钱接口；Web 仍以路由壳、商城 / 赚钱 H5 页面与管理后台为主。

## 架构设计

### 整体架构

```text
┌────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                            用户界面层                                               │
├──────────────┬───────────────────────────────────────────────┬───────────────────────────────────────┤
│   Web 前端   │                 Android App                   │                 iOS App               │
│ Next.js 16   │ Kotlin + Compose                              │ SwiftUI                               │
│ App Router   │ Navigation Compose                            │ TabView + NavigationStack             │
│ 首页/用户端壳 │ 首页Feed + 签到 + 菜单 + 消息 + 剧场 + 商城H5 + 赚钱H5 + 搜索/排行/分类 + 评论 │ 首页Feed + 签到 + 菜单 + 消息 + 剧场 + 商城H5 + 赚钱H5 + 搜索/排行/分类 + 评论 │
│ 管理后台已落地 │ Login route + 设置页 + Deeplink + WebView 容器 │ Login fullScreenCover + 设置页 + Deeplink + WKWebView 容器 │
└──────┬───────┴─────────────────────────┬──────────────────────┴───────────────────────────┬────────┘
       │                                 │                                                  │
       │ 页面语义 / 管理后台                │ 内容发现 / 签到 / 消息 / 登录 / 评论 / 商城 / 赚钱 contract │ 内容发现 / 签到 / 消息 / 登录 / 评论 / 商城 / 赚钱 contract
       ▼                                 ▼                                                  ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                     Backend API 服务层                                             │
│                              Next.js App Router Route Handlers                                     │
│  ├── /api/health                                  已实现                                           │
│  ├── /api/dramas                                  已实现：首页 Feed                                │
│  ├── /api/check-ins/status|check-ins              已实现：签到状态 / 提交                          │
│  ├── /api/messages/preview                        已实现：菜单消息预览                            │
│  ├── /api/messages/system                         已实现：系统消息列表                            │
│  ├── /api/messages/interactions                   已实现：互动消息列表（登录）                    │
│  ├── /api/users/me/bookings                       已实现：预约资产列表 + summary（登录）          │
│  ├── /api/dramas/channel                          已实现：剧场 Feed                                │
│  ├── /api/dramas/search                           已实现：搜索结果                                 │
│  ├── /api/dramas/hot-search                       已实现：热搜                                     │
│  ├── /api/dramas/tags                             已实现：分类标签矩阵                             │
│  ├── /api/dramas/rankings                         已实现：可选 auth 排行列表                       │
│  ├── /api/dramas/:id/book                         已实现：强制 auth 预约                           │
│  ├── /api/dramas/:id/comments                     已实现：评论列表 / 发评论                        │
│  ├── /api/dramas/:id/comments/:commentId/like     已实现：评论点赞切换                             │
│  ├── /api/player/recently-viewed                  已实现：菜单最近在看                             │
│  ├── /api/auth/otp-requests                       已实现：发送验证码                               │
│  ├── /api/auth/sessions                           已实现：登录 / 自动注册                          │
│  ├── /api/auth/session-refreshes                  已实现：refresh                                  │
│  ├── /api/users/me                                已实现：当前用户                                 │
│  ├── /api/auth/session                            已实现：幂等 logout                              │
│  ├── /api/mall/products                           已实现：商城商品 Feed                            │
│  ├── /api/earn/overview                           已实现：赚钱首页聚合数据                         │
│  ├── /api/earn/complete-task                      已实现：代表性任务奖励结算                       │
│  ├── /api/admin/auth/login                        已实现：Admin 登录                               │
│  └── /api/player/start|stop                       已实现：原生播放器历史链路                       │
└────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 当前首页发现、签到、消息、预约资产、商城、赚钱与账号承载结构

| 端 | 一级容器 | 首页 / 发现 / 账号承载 | 数据来源 / 入口 | 当前状态 |
|----|---------|----------------------|----------------|---------|
| Web | Next.js App Router 页面树 | 应用信息首页壳 + `/search` / `/rankings` 占位页 + `/mall` 商城 H5 首页 + `/mall/product/[id]` 商品详情 + `/earn` H5 页面 + 管理后台登录 | 本地静态 UI + 代表性链接 + Mall API + Earn API + admin routes | 用户端首页、搜索、排行、分类、“我的预约”与“我的”都不实现真实数据，但 `/mall` 与 `/earn` 已提供真实 H5 页面 |
| Android | `Scaffold` + `NavigationBar` + nested `NavHost` + `MenuPanelDrawer` | `HomeScreen` Feed + `CheckInPopup` + `MessageCenterScreen` + `BookingAssetsScreen` + `TheaterScreen` + 搜索发现 + `RankingScreen` + `ClassificationScreen` + `MallScreen` + `EarnScreen` / `EarnLoginScreen` / earn task player + `ProfileScreen` / `SettingsScreen` / `LoginScreen` + Feed / Player comments | `GET /api/dramas` + `GET /api/check-ins/status` + `POST /api/check-ins` + `GET /api/messages/*` + `GET /api/users/me/bookings` + `GET /api/dramas/channel` + `GET /api/player/recently-viewed` + `GET /api/dramas/search` + `GET /api/dramas/hot-search` + `GET /api/dramas/tags` + `GET /api/dramas/rankings` + `GET /api/mall/products`（经 H5 间接消费）+ Auth API + comments APIs + Earn API | 已实现首页发现链路、签到浮层、消息系统、预约资产真实页面、剧场真实页面、商城 H5 容器、“我的”真实登录 / 设置承载、评论抽屉与赚金币 WebView 容器 / 登录承接 / 任务回流 |
| iOS | `TabView` + per-tab `NavigationStack` + `MenuPanelContainerView` + `fullScreenCover` | `HomeView` Feed + `CheckInPopupView` + `MessageCenterView` + `BookingAssetsView` + `TheaterView` + `SearchHomeView` + `SearchResultView` + `RankingHomeView` + `ClassificationHomeView` + `MallContainerView` + `EarnContainerView` / `.earnLogin` / `.earnPlayer` + `ProfileHomeView` / `SettingsView` / 登录弹层 + Feed / Player comments | `GET /api/dramas` + `GET /api/check-ins/status` + `POST /api/check-ins` + `GET /api/messages/*` + `GET /api/users/me/bookings` + `GET /api/dramas/channel` + `GET /api/player/recently-viewed` + `GET /api/dramas/search` + `GET /api/dramas/hot-search` + `GET /api/dramas/tags` + `GET /api/dramas/rankings` + `GET /api/mall/products`（经 H5 间接消费）+ Auth API + comments APIs + Earn API | 已实现首页发现链路、签到浮层、消息系统、预约资产真实页面、剧场真实页面、商城 H5 容器、“我的”真实登录 / 设置承载、comments sheet 与赚钱 WKWebView 容器 / 登录承接 / 任务回流 |
| Backend | Route Handlers | 首页 Feed + 签到状态 / 提交 + 菜单消息预览 + 消息中心 + 预约资产列表 + 剧场 Feed + 最近在看 + 搜索 + 热搜 + 分类 tags + 排行 + 预约 + auth + comments + mall + earn | `DramaService / CheckInService / MessageService / PlayerService / AuthService / CommentService / MallService / EarnService` + registry / Supabase / mock repositories | 已形成“发现接口 + 签到 + 消息 + 预约资产 + 最近在看 + 认证闭环 + 评论接口 + 商城接口 + 赚钱接口”的可运行结构，但不同子域仍有 mock / Supabase 混合数据源 |

### 当前认证、签到、消息、预约资产、评论、商城与赚钱能力分层现状

| 能力 | 当前实现层级 | 真实状态 | 主要证据 |
|------|-------------|---------|---------|
| 移动端用户认证 | Auth API + canonical auth middleware + 客户端 session store | 已实现 | `backend/src/middleware/auth.ts`、`backend/src/app/api/auth/*`、`backend/src/app/api/users/me/route.ts` |
| Admin 管理端认证 | Supabase JWT + role 校验 | 已实现 | `backend/src/middleware/auth.ts`、`backend/src/app/api/admin/auth/login/route.ts` |
| Android 用户登录态 | `AuthStateHolder` + `AuthBootstrapper` + 登录页 / 设置页 / 评论、排行、消息、mall、earn 拦截上下文 | 已实现闭环 | `android/app/src/main/java/com/djs66256/short_drama/core/auth/**`、`feature/auth/**` |
| iOS 用户登录态 | `AuthStore` + `AppShellView.fullScreenCover` + 登录页 / 设置页 / 评论、排行、消息、mall、earn 拦截上下文 | 已实现闭环 | `ios/ShortDrama/Sources/Features/Auth/**`、`ios/ShortDrama/Sources/App/AppShellView.swift` |
| 签到状态 / 提交 | Backend `check-ins` route + Android / iOS 首页浮层 | 已实现 | `backend/src/app/api/check-ins/*`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/**`、`ios/ShortDrama/Sources/Features/Home/**` |
| 菜单消息预览 / 消息中心 | Backend `messages` route + Android / iOS 菜单 / 消息页 | 已实现 | `backend/src/app/api/messages/*`、`android/.../feature/menu/**`、`android/.../feature/messages/**`、`ios/.../Features/MenuPanel/**`、`ios/.../Features/Messages/**` |
| 预约资产列表 | Backend `GET /api/users/me/bookings` + Android / iOS booking 页面 | 已实现 | `backend/src/app/api/users/me/bookings/route.ts`、`backend/src/repositories/supabase/drama.supabase.repository.ts`、`android/.../feature/booking/**`、`ios/.../Features/BookingAssets/**` |
| 评论列表 / 发评论 / 点赞评论 | Backend + Android + iOS 评论链路 | 已实现 | `backend/src/app/api/dramas/[id]/comments/*`、`android/.../feature/comments/`、`ios/.../Features/Comments/` |
| 商城 H5 容器 | Web H5 + Native host bridge + Mall API | 已实现首版容器承载 | `web/src/app/mall/**`、`android/.../feature/mall/**`、`ios/.../Features/Mall/**`、`backend/src/app/api/mall/**` |
| 赚钱 H5 容器 | Web H5 + Native host sync + Earn API | 已实现代表性任务闭环 | `web/src/features/earn/**`、`android/.../feature/earn/**`、`ios/.../Features/Earn/**`、`backend/src/app/api/earn/**` |
| 赚钱奖励结算 | Bearer-only complete-task + Native 播放承接 | 已实现首版 | `backend/src/app/api/earn/complete-task/route.ts`、`android/.../EarnViewModel.kt`、`ios/.../EarnContainerViewModel.swift` |

### 核心流程调用栈

#### 流程：移动端首页首屏后评估签到浮层

```text
Android
1. HomeScreen 首次组合触发 viewModel.loadIfNeeded()
2. HomeViewModel 先请求 GET /api/dramas?page=1&pageSize=10
3. 首屏内容成功后调用 loadCheckInStatusIfNeeded()
4. ApiService.getCheckInStatus() 透传 Authorization(可选) + X-Installation-Id
5. Backend GET /api/check-ins/status -> resolveOptionalAuthContext -> parseInstallationId -> CheckInService
6. CheckInService 计算 should_show_popup、today_signed、current_streak、days
7. HomeViewModel 再叠加本地 dismissed server_date 与 blocking modal 判断
8. HomeScreen 仅在无 comments / login blocking 时叠加 CheckInPopup
9. 点击提交 -> POST /api/check-ins -> 返回最新状态

iOS
1. HomeView.task 调用 await viewModel.loadIfNeeded()
2. HomeViewModel 先请求 GET /api/dramas?page=1&pageSize=10
3. 首屏内容成功后调用 evaluateCheckInPopupIfNeeded()
4. CheckInRemoteDataSource 透传 Authorization(可选) + X-Installation-Id
5. Backend GET /api/check-ins/status -> resolveOptionalAuthContext -> parseInstallationId -> CheckInService
6. CheckInService 返回 server_date / reward_copy / days / should_show_popup
7. HomeViewModel 再叠加 UserDefaults dismissed serverDate 与 activeCommentSheet / loginAlertContext 判断
8. HomeView 仅在无评论 sheet 与登录 alert 时渲染 CheckInPopupView
9. 点击提交 -> POST /api/check-ins -> 返回最新状态
```

#### 流程：移动端从首页菜单进入消息中心并在登录后留在消息页

```text
Android
1. HomeScreen 左上角汉堡按钮调用 onOpenMenu
2. NavGraph 在首页 graph 同层渲染 MenuPanelDrawer + MenuPanelRoute
3. MenuPanelViewModel.loadIfNeeded() 并发请求 recentlyViewed 与 messagePreview
4. 点击“我的消息” -> MainNavigationViewModel.closeMenuThenNavigate(PendingRoute.MenuMessages)
5. menu close 动画结束后再消费 pendingRoute 并打开 menu/messages
6. MessageCenterScreen 同时加载系统消息与互动消息；匿名态只展示系统消息 + 登录门槛
7. 点击登录 -> login?returnRoute=menu/messages&source=menu_messages
8. 登录成功后回到 menu/messages，并刷新互动消息分区

iOS
1. HomeView 左上角按钮调用 router.openMenuPanel()
2. AppShellView 在 ZStack 中叠加 MenuPanelContainerView
3. MenuPanelViewModel.loadIfNeeded() 并发加载 recently viewed 与 message preview
4. 点击消息入口 -> NavigationRouter.closeMenuPanelThenNavigate(to: .messages)
5. markMenuPanelDidClose() 后真正执行 .messages 导航
6. MessageCenterView 同时承载 systemMessages 与 interactionMessages 两个 section
7. 匿名态 interaction section 显示登录门槛
8. 点击登录 -> presentLogin(LoginInterceptionContext(source: .messagesEntry, returnRoute: .messages))
9. completeLogin() 后保持 messages 上下文，并基于 authStore 变化刷新互动消息区
```

#### 流程：移动端从菜单进入“我的预约”，登录后回到 booking route

```text
Android
1. 首页菜单静态项把“我的预约”映射为 PendingRoute.MenuBooking
2. MainNavigationViewModel.closeMenuThenNavigate() 先关闭菜单，再缓存 pendingMenuRoute
3. NavGraph 在菜单关闭后消费 pending route，并导航到 menu/booking
4. BookingAssetsScreen 首屏默认请求 GET /api/users/me/bookings?status=online&page=1&pageSize=20
5. 未登录或登录过期时，BookingAssetsViewModel 切到 Anonymous / Expired 登录门槛
6. 点击登录 -> login?returnRoute=menu/booking&source=menu_booking
7. 登录成功后 resolveSuccessRoute() 保留 menu/booking，返回 booking 页面并刷新资产数据
8. Backend 先聚合 summary，再按当前 status 返回分页 data + snake_case pagination

iOS
1. MenuPanelContainerView 点击“我的预约”后调用 closeMenuPanelThenNavigate(to: .bookingAssets)
2. NavigationRouter 在菜单关闭动画结束后真正导航到 .bookingAssets
3. BookingAssetsView 首屏默认请求 GET /api/users/me/bookings?status=online&page=1&pageSize=20
4. 匿名或过期状态下展示 BookingAssetsLoginGateView
5. 点击登录 -> BookingAssetsRouteBuilder.loginContext() 生成 source=.bookingAssets, returnRoute=.bookingAssets
6. AppShellView.fullScreenCover 承载统一登录页
7. completeLogin() 恢复 .bookingAssets；若当前已在 booking route，则不重复 push
8. Backend 返回 summary + 当前 Tab 分页数据；401 时 ViewModel resetForLoginGate() 清空旧资产并重新显示登录门槛
```

#### 流程：评论能力在当前运行架构中的真实落点

```text
1. 首页 Feed 卡片已增加评论入口
2. 播放器右侧“评论”已接成真实 action
3. Android / iOS 都以内嵌 bottom sheet / sheet 承载 comments UI
4. Comments ViewModel 负责列表、排序、分页、发送、点赞、登录恢复语义
5. Backend 已提供 /api/dramas/:id/comments 与 /api/dramas/:id/comments/:commentId/like
6. 评论列表走可选鉴权；写接口走强制鉴权；登录成功后只恢复评论容器上下文，不自动重放写操作
```

#### 流程：赚钱 H5 通过 Native 容器完成登录承接、播放器承接与奖励结算

```text
Web / Android / iOS / Backend
1. Web /earn 首屏调用 GET /api/earn/overview 获取匿名或已登录视角数据
2. Native 容器在页面加载成功后向 H5 注入 earn.syncAuthState
3. 未登录用户点击任务时，H5 发出 earn.requestLogin，请求 Native 拉起 earn 专属登录承接页
4. 登录完成后，Native 先回传 earn.syncAuthState，再回传 earn.restoreContext(reason=login-return)
5. 已登录用户点击代表性任务时，H5 发出 earn.openTaskPlayer，请求 Native 打开原生播放器承接页
6. 只有原生播放器自然播放结束时，Native 才回传 completed=true 的 earn.completeTask
7. H5 收到 completed=true 后，携带内存态 bearer token 调用 POST /api/earn/complete-task
8. Native 随后回传 earn.restoreContext(reason=task-return)，H5 恢复赚钱页上下文
```

### 设计决策

| 决策 | 原因 | 影响 |
|------|------|------|
| 移动端统一采用 5 个一级频道 | 为首页、剧场、商城、赚钱、我的提供稳定承载入口 | 后续功能默认挂载到既有频道容器，而不是新增顶级入口 |
| 菜单面板挂在首页根页，而不是新增一级 tab | 菜单属于个人工具抽屉，不改变既有底部导航 IA | Android / iOS 都由应用壳在 home tab 上统一承载 overlay 与关闭后导航 |
| 签到浮层挂在首页内容之上，而不是独立签到页 | 需求强调冷启动轻量激励，不应打断首页主内容消费 | Android / iOS 都在首页首屏后异步评估签到状态，并与评论 / 登录模态做互斥 |
| 消息系统入口继续放在首页菜单中 | 保持现有 IA，不新增一级 tab 或 profile 子频道 | 菜单预览、消息中心路由与登录回流都复用首页壳已有 close-menu-then-navigate 机制 |
| 搜索发现、排行与分类继续挂在首页频道内 | 都属于内容发现链路的延伸，而非新的一级频道 | `ranking`、`classification` 成为首页 tab 下的子路由 |
| 剧场作为独立一级频道，但详情 / 播放 / 发现拥有页面继续复用首页导航栈 | 保持底部 IA 稳定，同时避免重复实现搜索、排行、分类、详情和播放器 | theater tab 只承载频道 feed 与快捷入口 |
| PRD-08 认证统一走 Backend RESTful API | 收敛跨端 contract，统一 test OTP、本地 session、真实 bearer 校验与错误映射 | Android / iOS 都围绕 `POST /api/auth/*`、`GET /api/users/me`、`DELETE /api/auth/session` 实现登录闭环 |
| 评论首版按页面内抽屉而非独立 route 设计 | 保持用户停留在 Feed / Player 当前上下文 | 评论能力以内嵌 comments sheet / bottom sheet 落地 |
| 签到接口采用可选登录 + installationId 兜底 | 需要同时支持匿名用户和登录用户连续签到 | 服务端以账号优先，匿名态依赖 `X-Installation-Id` 作为安装级主体 |
| 消息系统拆成 preview / system / interactions 三个接口 | 菜单入口和消息中心需要不同粒度、不同鉴权口径的数据 | preview 匿名可读且空态返回 204；interactions 强制登录 |
| 预约资产列表独立为 `/api/users/me/bookings` 读取接口 | 预约写入已存在于排行接口链路中，但资产页需要稳定的分页、summary 与双 Tab contract | Backend 以独立受保护 route 提供 `{ data, pagination, summary }`，Android / iOS 只消费服务端聚合结果 |
| 商城（mall）与赚钱（earn）继续按 H5 承载 | 产品策略已明确这两类页面不按 Native 首批页面重写 | Native 重点负责容器、登录承接、搜索桥接与任务回流，而非重写完整业务页 |
| Native 到赚钱 H5 的唯一宿主同步通道是 host message | 统一跨端 bridge transport，避免 Android / iOS 分别定义不同注入协议 | Web 只需要消费一个 host message schema，Android / iOS 都按同一消息模型注入 |
| 赚钱奖励只在 Native 播放承接层自然播放结束后结算 | 避免 H5 自行伪造“完成任务” | 只有 `completed=true` 时才触发 `earn.completeTask`，然后再 `earn.restoreContext(reason=task-return)` |
| Backend 运行时采用混合仓储策略 | 在真实内容后台尚未完备前，先保证功能链路可验证 | 首页 / 搜索等仍多为 mock，comments、check-ins、system messages 等逐步接入更真实的 Supabase / mixed 路径 |

## 跨端涉及

| 端 | 相关模块 / 文件 | 说明 |
|----|-------------|------|
| Web | `web/src/app/layout.tsx`、`web/src/app/page.tsx`、`web/src/features/home/HomeScreen.tsx`、`web/src/app/search/page.tsx`、`web/src/app/rankings/page.tsx`、`web/src/app/mall/**`、`web/src/app/earn/page.tsx`、`web/src/features/earn/**`、`web/src/features/admin/**` | 用户端首页与发现页仍为壳；已落地重点在管理后台、商城 H5 页面与赚钱 H5 页面；PRD-11 预约资产页明确 skipped |
| Android | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/.../feature/home/**`、`feature/menu/**`、`feature/messages/**`、`feature/booking/**`、`feature/theater/**`、`feature/search/**`、`feature/ranking/**`、`feature/classification/**`、`feature/profile/**`、`feature/auth/**`、`feature/comments/**`、`feature/mall/**`、`feature/earn/**`、`core/auth/**` | 已同时接入首页发现、签到浮层、菜单 overlay、消息系统、预约资产页、剧场一级 tab、商城 H5 容器、账号承载、登录恢复、comments 容器与赚钱 WebView 容器 / 任务回流 |
| iOS | `ios/ShortDrama/Sources/App/AppShellView.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`、`ios/.../Features/Home/**`、`Features/MenuPanel/**`、`Features/Messages/**`、`Features/BookingAssets/**`、`Features/Theater/**`、`Features/Search/**`、`Features/Ranking/**`、`Features/Classification/**`、`Features/Profile/**`、`Features/Auth/**`、`Features/Comments/**`、`Features/Mall/**`、`Features/Earn/**`、`Features/Player/**` | 已同时接入首页发现、签到浮层、菜单 overlay、消息系统、预约资产页、剧场一级 tab、商城 H5 容器、账号承载、登录恢复、comments sheet 与赚钱 WKWebView 容器 / 任务回流 |
| Backend | `backend/src/app/api/dramas/**`、`backend/src/app/api/check-ins/**`、`backend/src/app/api/messages/**`、`backend/src/app/api/users/me/bookings/route.ts`、`backend/src/app/api/player/**`、`backend/src/app/api/auth/**`、`backend/src/app/api/users/me/route.ts`、`backend/src/app/api/admin/**`、`backend/src/app/api/mall/**`、`backend/src/app/api/earn/**`、`backend/src/services/**`、`backend/src/middleware/auth.ts`、`backend/src/repositories/**` | 提供首页发现、签到、消息、预约资产、最近在看、认证闭环、评论接口、商城接口、赚钱接口与混合 repository 运行结构 |

## 技术栈总览

| 层级 | Web | Backend | Android | iOS |
|------|-----|---------|---------|-----|
| 语言 | TypeScript | TypeScript | Kotlin 2.0.21 | Swift 6 |
| UI / 路由框架 | React 19 + Next.js 16 App Router | Next.js 16 Route Handlers | Jetpack Compose + Material3 + Navigation Compose + drawer overlay + check-in popup + comments bottom sheet + WebView 容器 | SwiftUI + TabView + NavigationStack + ZStack overlay + check-in popup + comments sheet + fullScreenCover + WKWebView 容器 |
| 状态管理 | 路由参数 + React 组件状态 / reducer | Route Handler 请求级状态 + middleware `request.auth` | `StateFlow` + `NavController` + `MainNavigationViewModel` + `AuthStateHolder` + 各页面 ViewModel | `ObservableObject` + `@Published` + `NavigationRouter` + `AuthStore` + 各页面 ViewModel |
| 构建工具 | next build | next build | AGP 8.7.0 + Gradle | XcodeGen + Xcode 27 |
| 测试 | Vitest + Testing Library | Vitest | JUnit4 + Turbine + Compose testing helpers | Swift Testing |
| 关键 contract | 用户端多为路由壳；商城 / 赚钱额外消费宿主 bridge，预约资产页本期 skipped | `/api/dramas*`、`/api/check-ins*`、`/api/messages*`、`/api/users/me/bookings`、`/api/auth*`、`/api/mall/*`、`/api/earn/*`、comments APIs、player APIs、admin APIs | 共享 Backend RESTful contract、`X-Playback-Session-Id`、`X-Installation-Id`、`AuthSession` payload、booking assets payload、mall / earn host bridge | 同 Android，共享 Backend RESTful contract 与宿主 bridge 语义 |

## 已知限制

- Web 端当前未实现与移动端对等的首页 Feed、签到浮层、消息系统、菜单面板、剧场频道、真实排行 / 分类页、“我的”登录页、预约资产页或评论能力；其主要已落地能力仍是管理后台与商城 / 赚钱 H5 页面。
- 菜单中的登录、消息与下载仍有部分 Native 占位承接页；其中“我的预约”已切换为真实资产页，“我的下载”仍保持占位；游戏中心仅提供本地“即将上线”反馈。
- 最近在看接口只从固定候选窗口中返回最多 3 条合法记录；过滤脏数据后允许不足 3 条，也不承诺继续向更老历史补足。
- 播放页与详情页跨端都还是以承载导航语义、播放状态、评论容器与 earn task 承接为主，不包含完整内容详情业务数据。
- Backend 当前首页 / 剧场 / 搜索 / 热搜 / 分类数据仍主要来自 `DramaMockRepository`；排行 / 预约与 auth 已接到更真实的 Supabase 路径；商城与赚钱仍采用 mock / seed 数据支撑首版 contract。
- Backend comments migration 的真实 `supabase db push` 仍受历史 migration 幂等性问题阻塞；当前 comments 代码、测试、构建与端侧接入不受影响。
- 赚钱奖励当前只允许代表性任务完成，且必须依赖 Native 播放承接层自然结束后才会结算；真实提现、账本与多任务并发尚未实现。
- 设备级黑盒验证在本轮 workflow 中按规范降级，当前跨端结论主要来自代码、自动化测试与 QA 文档。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-08-03 | 更新：合并 PRD-08、PRD-09、PRD-10、PRD-11、PRD-13、PRD-14 的系统总览，统一收录认证、评论、签到、消息、个人资产、商城 H5、赚钱 H5 与 mixed repository 运行结构 |
| 2026-07-28 | 更新：系统总览同步 PRD-12 剧场频道落地结果 |
| 2026-07-28 | 更新：系统总览同步 PRD-07 菜单面板落地结果 |
| 2026-07-27 | 更新：系统总览同步 PRD-06 分类浏览落地结果 |
| 2026-07-27 | 更新：系统总览同步 PRD-05 排行体系落地结果 |
| 2026-07-26 | 更新：系统总览同步 PRD-02 首页信息流落地结果 |
| 2026-07-25 | 更新：系统总览同步 PRD-01 导航骨架落地结果 |
| 2026-07-22 | 从各端代码提取信息，初始创建 |

---
*本文档由 llm-wiki skill 自动维护。*
