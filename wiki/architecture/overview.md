# 系统总览 架构文档

> 最后更新：2026-07-29

## 概述

项目是一个多端短剧内容应用的 harness 仓库，覆盖 Web、Android、iOS 三端界面与 Backend 服务端骨架。PRD-01 已完成移动端 5 Tab 导航容器；PRD-02 进一步把 Android / iOS 首页从应用信息占位页推进为 Native 首页信息流，并让 Backend 提供 canonical `GET /api/dramas` 列表接口作为首页首屏数据源；PRD-05 在首页频道下落地搜索发现与排行浏览链路；PRD-06 继续补齐分类浏览链路；PRD-08 再把“我的”频道从占位容器推进为真实账号承载入口，补齐手机号验证码登录、自动注册、会话恢复 / refresh / logout，以及排行预约登录拦截。当前移动端已经形成“首页发现 + 认证承载 + 预约鉴权”的完整闭环；Web 端继续保持路由骨架与管理后台，不在本期实现与移动端对等的用户端登录、排行或分类体验；商城（mall）与赚钱（earn）继续由 H5 承载，不属于本期 Native 能力范围（`PRODUCT.md:22-25`）。

- **产品信息来源**：`PRODUCT.md`
- **仓库结构**：monorepo，按 `web/`、`android/`、`ios/`、`backend/` 分目录维护
- **技术标识**：Android/iOS 继续使用 `com.djs66256.short_drama`，移动端 deeplink scheme 为 `djsdrama://`
- **当前版本**：移动端首页已具备 Feed、搜索发现、排行浏览、分类浏览与“我的”登录 / 设置承载能力；Backend 已补齐真实认证闭环与排行预约鉴权；Web 仍以路由壳与管理后台为主

## 架构设计

### 整体架构

```text
┌────────────────────────────────────────────────────────────────────────────────────┐
│                                  用户界面层                                        │
├──────────────┬─────────────────────────────────┬────────────────────────────────────┤
│   Web 前端   │          Android App            │              iOS App               │
│ Next.js 16   │ Kotlin + Compose                │ SwiftUI                            │
│ App Router   │ Navigation Compose              │ TabView + NavigationStack          │
│ 首页/用户端壳 │ 首页Feed + 搜索/排行/分类 + 我的 │ 首页Feed + 搜索/排行/分类 + 我的   │
│ 管理后台已落地 │ 登录路由 + 设置页 + Deeplink    │ Login fullScreenCover + 设置页     │
└──────┬───────┴────────────────┬────────────────┴────────────────┬──────────────────┘
       │                        │                                 │
       │ 页面语义 / 管理后台       │ 内容发现 / 登录 / 预约 contract      │ 内容发现 / 登录 / 预约 contract
       ▼                        ▼                                 ▼
┌────────────────────────────────────────────────────────────────────────────────────┐
│                               Backend API 服务层                                   │
│                     Next.js App Router Route Handlers                              │
│  ├── /api/health                                  已实现                           │
│  ├── /api/dramas                                  已实现：首页 Feed                │
│  ├── /api/dramas/search                           已实现：搜索结果                 │
│  ├── /api/dramas/hot-search                       已实现：热搜                     │
│  ├── /api/dramas/tags                             已实现：分类标签矩阵             │
│  ├── /api/dramas/rankings                         已实现：可选 auth 排行列表       │
│  ├── /api/dramas/:id/book                         已实现：真实登录态预约           │
│  ├── /api/auth/otp-requests                       已实现：发送验证码               │
│  ├── /api/auth/sessions                           已实现：登录 / 自动注册          │
│  ├── /api/auth/session-refreshes                  已实现：refresh                  │
│  ├── /api/users/me                                已实现：当前用户                 │
│  ├── /api/auth/session                            已实现：幂等 logout              │
│  └── /api/player/start|stop                       501 占位                         │
└────────────────────────────────────────────────────────────────────────────────────┘
```

### 当前首页发现与账号承载结构

| 端 | 一级容器 | 首页 / 发现 / 账号承载 | 数据来源 / 入口 | 当前状态 |
|----|---------|----------------------|----------------|---------|
| Web | Next.js App Router 页面树 | 应用信息首页壳 + `/search` / `/rankings` 占位页 + 管理后台登录 | 本地静态 UI + 代表性链接 + admin routes | 用户端首页、搜索、排行、分类与“我的”都不实现真实数据 |
| Android | `Scaffold` + `NavigationBar` + nested `NavHost` | `HomeScreen` Feed + `SearchHomeScreen` + `SearchResultScreen` + `RankingScreen` + `ClassificationScreen` + `ProfileScreen` / `SettingsScreen` / `LoginScreen` | `GET /api/dramas` + `GET /api/dramas/search` + `GET /api/dramas/hot-search` + `GET /api/dramas/tags` + `GET /api/dramas/rankings` + Auth API | 已实现首页发现链路、“我的”真实登录 / 设置承载与排行预约登录拦截 |
| iOS | `TabView` + per-tab `NavigationStack` + `fullScreenCover` | `HomeView` Feed + `SearchHomeView` + `SearchResultView` + `RankingHomeView` + `ClassificationHomeView` + `ProfileHomeView` / `SettingsView` / 登录弹层 | `GET /api/dramas` + `GET /api/dramas/search` + `GET /api/dramas/hot-search` + `GET /api/dramas/tags` + `GET /api/dramas/rankings` + Auth API | 已实现首页发现链路、“我的”真实登录 / 设置承载与排行预约登录拦截 |
| Backend | Route Handlers | 首页 Feed + 搜索 + 热搜 + 分类 tags + 排行 + 预约 + auth | 首页 / 搜索 / 热搜 / 分类仍以 `DramaMockRepository` 为主；排行 / 预约走 `DramaSupabaseRepository`；认证走 `AuthService + verifyJwt()` | 已形成“发现接口 + 认证接口 + 预约鉴权”混合可运行结构 |

### 核心流程调用栈

#### 流程：移动端从“我的”或排行预约进入登录并完成回跳

```text
Android
1. ProfileScreen 的“立即登录”或 RankingViewModel.onBookClick() 触发登录入口
2. NavGraph 打开 login route，并把 returnRoute/source 透传给 LoginScreen
3. LoginViewModel 先发起 POST /api/auth/otp-requests，再发起 POST /api/auth/sessions
4. AuthStateHolder 保存 AuthSession，更新 authStatus
5. 登录成功后：profile 场景回到 profile；ranking 场景回到原 ranking?... route
6. 应用冷启动时 AuthBootstrapper.restoreIfNeeded() 先调用 GET /api/users/me，401 时再走 POST /api/auth/session-refreshes
7. 设置页退出登录调用 DELETE /api/auth/session，成功后回到 profile 根页

iOS
1. ProfileHomeView 或 RankingHomeView 通过 router.presentLogin(context) 设置 presentedLoginContext
2. AppShellView.fullScreenCover(item: presentedLoginContext) 承载 LoginView
3. LoginViewModel 先发起 POST /api/auth/otp-requests，再发起 POST /api/auth/sessions
4. AuthStore.handleLoginSuccess(session) 保存 session 并更新 status
5. router.completeLogin() 根据 context.returnRoute 回到 profile 或 rankingHome
6. 应用启动时 AuthStore.restoreIfNeeded() 先调用 GET /api/users/me，必要时再走 POST /api/auth/session-refreshes
7. SettingsView 调用 logout，后台执行 DELETE /api/auth/session，本地清理后回到 profile 根页
```

| 调用层级 | 平台 | 文件 | 职责 |
|---------|------|------|------|
| 入口 | Android | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:205-219,320-371` | 管理排行预约登录拦截、profile 登录入口、login route 与设置页回跳 |
| 1 | Android | `android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/ProfileScreen.kt:28-146`, `android/app/src/main/java/com/djs66256/short_drama/feature/profile/ui/SettingsScreen.kt:36-117` | 承载“我的”匿名态 / 已登录态 / 登出确认 |
| 2 | Android | `android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt:29-248`, `android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthBootstrapper.kt:9-46` | 登录、回跳、会话恢复与 refresh |
| 入口 | iOS | `ios/ShortDrama/Sources/App/AppShellView.swift:7-49`, `ios/ShortDrama/Sources/App/NavigationRouter.swift:7-108` | 承载登录弹层、保存登录上下文并根据 returnRoute 回跳 |
| 1 | iOS | `ios/ShortDrama/Sources/Features/Profile/Views/ProfileHomeView.swift:8-105`, `ios/ShortDrama/Sources/Features/Profile/Views/SettingsView.swift:11-56` | 承载“我的”登录入口、设置页与登出确认 |
| 2 | iOS | `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift:19-79`, `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift:10-16`, `ios/ShortDrama/Sources/Features/Auth/AuthStore.swift:4-109` | 发起排行登录拦截、构造登录上下文、恢复与清理会话 |
| Backend Auth | Backend | `backend/src/app/api/auth/otp-requests/route.ts:7-14`, `backend/src/app/api/auth/sessions/route.ts:7-14`, `backend/src/app/api/auth/session-refreshes/route.ts:7-14`, `backend/src/app/api/users/me/route.ts:7-13`, `backend/src/app/api/auth/session/route.ts:6-13` | 提供 OTP、登录 / 自动注册、refresh、me、logout 接口 |
| Backend Middleware | Backend | `backend/src/middleware/auth.ts:27-138` | 统一 bearer token 解析、可选 / 强制鉴权与 request.auth 注入 |

#### 流程：移动端从搜索发现进入排行 / 分类并继续消费内容

```text
Android / iOS
1. 首页 Home Feed 进入搜索发现页
2. 搜索发现快捷入口可进入 ranking 或 classification
3. ranking 调用 GET /api/dramas/rankings；classification 调用 GET /api/dramas/tags
4. classification 点击标签后继续复用 GET /api/dramas/search 展示结果
5. ranking 点击条目复用 play 路由；booking 榜点击预约时未登录则回到上面的登录流程
```

| 调用层级 | 平台 | 文件 | 职责 |
|---------|------|------|------|
| 入口 | Android | `android/app/src/main/java/com/djs66256/short_drama/feature/search/model/SearchQuickEntry.kt:18-39`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:152-228` | 提供 search / ranking / classification 子路由并承载页面注册 |
| 入口 | iOS | `ios/ShortDrama/Sources/App/TabNavigationHostView.swift:9-65`, `ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift:76-87` | 在 home Tab 下注册搜索发现、排行与分类页面 |
| Backend | Backend | `backend/src/app/api/dramas/route.ts:8-24`, `backend/src/app/api/dramas/search/route.ts:7-19`, `backend/src/app/api/dramas/hot-search/route.ts:6-11`, `backend/src/app/api/dramas/tags/route.ts:7-18`, `backend/src/app/api/dramas/rankings/route.ts:8-24` | 提供首页 Feed、搜索、热搜、分类 tags 与排行数据 |
| Repository | Backend | `backend/src/repositories/mock/drama.mock.repository.ts:387-468`, `backend/src/repositories/supabase/drama.supabase.repository.ts:330-459` | 分类 / 搜索 / 热搜仍以 mock 数据为主；排行 / 预约走 Supabase 持久化路径 |

### 设计决策

| 决策 | 原因 | 影响 |
|------|------|------|
| 移动端统一采用 5 个一级频道 | 为首页、剧场、商城、赚钱、我的提供稳定承载入口 | 后续功能 PRD 默认挂载到既有频道容器，而不是新增顶级入口 |
| 搜索发现、排行与分类继续挂在首页频道内 | 都属于内容发现链路的延伸，而非新的一级频道 | `ranking`、`classification` 成为首页 Tab 下的子路由，而不是新增 bottom tab |
| PRD-08 的认证统一走 Backend RESTful API，不让移动端直连 Supabase Auth SDK | 收敛跨端 contract，统一 test OTP、本地 session、真实 bearer 校验与错误映射 | Android / iOS 都围绕 `POST /api/auth/*`、`GET /api/users/me`、`DELETE /api/auth/session` 实现登录闭环 |
| “我的”频道从占位页升级为真实账号承载入口 | 登录、恢复、登出、设置与排行预约拦截都需要统一宿主 | Android 引入 `ProfileScreen / SettingsScreen / LoginScreen`；iOS 引入 `ProfileHomeView / SettingsView / fullScreenCover` |
| 排行列表使用可选鉴权，预约使用强制鉴权 | 未登录也需要浏览榜单，但预约必须绑定真实用户 | `GET /api/dramas/rankings` 可补充 `is_booked`；`POST /api/dramas/:id/book` 必须要求 bearer access token |
| Backend 运行时采用“发现接口 mock、排行/预约 Supabase、认证 Supabase Auth”混合策略 | 在真实内容后台尚未完备前，优先保证 PRD-08 登录与预约闭环可验证 | 首页/搜索/热搜/分类仍受 seed 数据限制，但 auth 与 bookings 已具备真实持久化语义 |
| 商城（mall）与赚钱（earn）继续按 H5 承载 | 产品策略已明确这些页面不按 Native 首批实现 | 当前移动端仍以 placeholder tab 代替真实 WebView/WKWebView |

## 跨端涉及

| 端 | 相关模块/文件 | 说明 |
|----|-------------|------|
| Web | `web/src/app/layout.tsx`, `web/src/app/page.tsx`, `web/src/features/home/HomeScreen.tsx`, `web/src/app/search/page.tsx`, `web/src/app/rankings/page.tsx`, `web/src/features/admin/**` | 用户端首页与发现页仍为壳；已落地重点在管理后台而非 PRD-08 移动端登录闭环 |
| Android | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`, `feature/home/**`, `feature/search/**`, `feature/ranking/**`, `feature/classification/**`, `feature/profile/**`, `feature/auth/**`, `core/auth/**` | 首页 Tab 已接入发现链路；“我的”频道已接入登录、设置、恢复与退出登录 |
| iOS | `ios/ShortDrama/Sources/App/AppShellView.swift`, `App/NavigationRouter.swift`, `Features/Home/**`, `Features/Search/**`, `Features/Ranking/**`, `Features/Classification/**`, `Features/Profile/**`, `Features/Auth/**` | 首页发现链路与“我的”频道账号承载都已接通 |
| Backend | `backend/src/app/api/dramas/**`, `backend/src/app/api/auth/**`, `backend/src/app/api/users/me/route.ts`, `backend/src/services/auth/**`, `backend/src/services/drama/**`, `backend/src/middleware/auth.ts`, `backend/src/repositories/mock/**`, `backend/src/repositories/supabase/**` | 提供首页发现、认证闭环、排行预约鉴权与混合 repository 运行结构 |

## 技术栈总览

| 层级 | Web | Backend | Android | iOS |
|------|-----|---------|---------|-----|
| 语言 | TypeScript | TypeScript | Kotlin 2.0.21 | Swift 6 |
| UI / 路由框架 | React 19 + Next.js 16 App Router | Next.js 16 Route Handlers | Jetpack Compose + Material3 + Navigation Compose | SwiftUI + TabView + NavigationStack |
| 状态管理 | 路由参数 + React 组件状态 | Route Handler 请求级状态 + middleware `request.auth` | `StateFlow` + `NavController` + AuthStateHolder + 各页面 ViewModel | `ObservableObject` + `@Published` + NavigationRouter + AuthStore |
| 构建工具 | next build | next build | AGP 8.7.0 + Gradle | XcodeGen + Xcode 27 |
| 测试 | Vitest + Testing Library | Vitest | JUnit4 + Turbine + Compose testing helpers | Swift Testing |
| 关键 contract | 用户端多为路由壳，管理后台接口另行维护 | `GET /api/dramas` / `search` / `hot-search` / `tags` / `rankings` / `POST /api/dramas/:id/book` / Auth API | `page/pageSize`、`q/page/pageSize`、`gender`、`type/contentType/page/pageSize`、AuthSession payload | 同 Android，共享 Backend RESTful contract |

## 已知限制

- Web 端当前未实现与移动端对等的首页 Feed、搜索发现、真实排行 / 分类页或“我的”登录页；其主要已落地能力仍是管理后台。
- Android 与 iOS 的“我的”频道已接入真实登录 / 设置承载，但剧场、商城、赚钱仍是占位页，真实业务内容尚未接入。
- 商城（mall）与赚钱（earn）按产品策略应由 H5 承载，但当前移动端代码仍未接入真实 H5 容器。
- 播放页与详情页跨端都还是占位实现，仅展示路由参数，不包含真实业务数据。
- Backend 当前首页 / 搜索 / 热搜 / 分类数据仍主要来自 `DramaMockRepository`；只有排行 / 预约与 auth 已切到真实 Supabase 路径。
- iOS 排行登录成功后只回到 `.rankingHome`，不显式恢复更细粒度的 `contentType/rankingType` 语义；Android 会保留完整 `ranking?...` returnRoute。
- 设备级黑盒验证在本轮 workflow 中按规范降级，当前跨端结论主要来自代码、自动化测试与 QA 文档（`docs/specs/2026-07-28-prd-08-login/qa-test.md:1-40`）。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 更新：系统总览同步 PRD-08 登录闭环落地结果，补充“我的”频道真实登录 / 设置承载、Auth API、会话恢复 / refresh / logout、排行预约登录拦截，以及 Backend 发现接口与 auth / bookings 的混合运行结构 |
| 2026-07-27 | 更新：系统总览同步 PRD-06 分类浏览落地结果，补充搜索发现到分类页的移动端链路、Backend 搜索/热搜/分类 tags 接口与 Native / Web / H5 的范围边界 |
| 2026-07-27 | 更新：系统总览同步 PRD-05 排行体系落地结果，补充搜索发现到排行页的移动端链路、Backend 排行/预约接口与 Native / Web / H5 的范围边界 |
| 2026-07-26 | 更新：系统总览同步 PRD-02 首页信息流落地结果，补充 Backend `GET /api/dramas`、移动端首页状态机、首页卡片到播放/详情页主路径，以及 Web / H5 的范围边界 |
| 2026-07-25 | 更新：系统总览同步 PRD-01 导航骨架落地结果，修正移动端从单页骨架到 5 Tab 容器的架构描述，并补充 Web 路由骨架与 Backend 不变更说明 |
| 2026-07-22 | 从各端代码提取信息，初始创建 |

---
*本文档由 llm-wiki skill 自动维护。*