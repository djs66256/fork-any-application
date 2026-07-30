# 应用壳 (App Shell)

> 最后更新：2026-08-03

## 功能概述

应用壳负责承载各端应用的启动入口、路由容器与基础页面骨架。当前能力已经覆盖：

- PRD-01：移动端 5 个一级频道的底部导航容器
- PRD-02：首页 Native Feed 首屏
- PRD-05 / PRD-06：搜索发现、排行与分类继续挂在首页频道所属导航栈中
- PRD-07：首页汉堡菜单、左侧抽屉、最近在看与关闭后导航时序
- PRD-08：“我的”频道真实登录 / 设置 / 退出登录承载
- PRD-10：首页签到浮层、菜单消息预览与独立消息中心
- PRD-11：菜单“我的预约”从占位承接切换为真实预约资产页，并保持“我的下载”为占位页
- PRD-12：剧场频道真实 feed 承载
- PRD-13：商城频道 H5 容器接入、搜索 / 登录 bridge 与商城上下文恢复
- PRD-14：赚钱频道 H5 容器接入、earn 专属登录承接、任务播放器承接与 host message 回流

Web 端继续维持 SSR-first 的 Next.js App Router 结构，不提供与移动端对等的用户端应用壳；商城（mall）与赚钱（earn）遵循 H5 承载策略，Android / iOS 分别通过 WebView / WKWebView 提供宿主容器。

- **覆盖端**：Web、Android、iOS、Backend
- **核心价值**：为首页 Feed、签到浮层、菜单抽屉、消息中心、预约资产页、剧场频道、商城 / 赚钱 H5 容器、搜索发现、排行、分类、播放页、详情页，以及“我的”频道登录 / 设置链路提供统一承载容器
- **当前状态**：移动端 5 Tab 骨架已落地；首页频道已接入 Feed、签到、菜单、消息、预约资产、搜索发现、排行与分类；剧场为真实频道；商城与赚钱为真实 H5 容器；“我的”频道为真实登录 / 设置承载。Web 端继续维持 App Router 路由骨架，并提供 `/mall`、`/mall/product/[id]`、`/earn` 与管理后台路由。

## 入口与路由

### Web
- 入口组件：`web/src/app/layout.tsx`、`web/src/app/page.tsx`
- 路由方案：Next.js App Router，Page 层负责路由委托
- 当前可访问骨架路由：`/`、`/play/[id]`、`/detail/[id]`、`/search`、`/rankings`、`/mall`、`/mall/product/[id]`、`/earn`
- 首页现状：`HomeScreen` 仍展示应用信息和代表性链接，不消费移动端同等的首页 Feed、签到、消息或账号壳逻辑；商城与赚钱由独立 H5 页面承载

### Backend
- 入口组件：`backend/src/app/layout.tsx`、`backend/src/app/page.tsx`
- 提供 `/` 服务信息页与 `/api/health`
- 为应用壳提供的主数据与能力接口包括：
  - 首页 / 发现：`GET /api/dramas`、`GET /api/dramas/search`、`GET /api/dramas/hot-search`、`GET /api/dramas/tags`、`GET /api/dramas/rankings`、`POST /api/dramas/:id/book`
  - 剧场：`GET /api/dramas/channel`
  - 签到：`GET /api/check-ins/status`、`POST /api/check-ins`
  - 消息：`GET /api/messages/preview`、`GET /api/messages/system`、`GET /api/messages/interactions`
  - 预约资产：`GET /api/users/me/bookings`
  - 菜单最近在看 / 播放历史：`GET /api/player/recently-viewed`、`POST /api/player/start`、`POST /api/player/stop`
  - 认证：`POST /api/auth/otp-requests`、`POST /api/auth/sessions`、`POST /api/auth/session-refreshes`、`GET /api/users/me`、`DELETE /api/auth/session`
  - 商城 / 赚钱：`GET /api/mall/products`、`GET /api/earn/overview`、`POST /api/earn/complete-task`

### Android
- 入口 Activity：`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt`
- 容器结构：`NavGraph` 在 `Scaffold` 中挂载 `NavigationBar` + `NavHost`，并额外在同层渲染 `MenuPanelDrawer`、签到浮层宿主与局部反馈
- 首页子路由：`search`、`search/result`、`ranking`、`classification`、`new-releases`、`actors`、`menu/messages`、`menu/login`、`menu/booking`、`menu/downloads`、`play/{videoId}`、`detail/{dramaId}`、`login?...`
- 菜单资产承载：`menu/booking` 为真实预约资产页，`menu/downloads` 仍为占位页
- 频道承载：
  - `home`：Feed、签到浮层、菜单抽屉、消息入口、预约资产、搜索发现、排行、分类
  - `theater`：真实剧场 feed
  - `mall`：真实 H5 容器
  - `earn`：真实 H5 容器 + earn 登录承接 + 原生播放器任务承接
  - `profile`：`ProfileScreen` / `SettingsScreen` / `LoginScreen`

### iOS
- 入口 App：`ios/ShortDrama/Sources/App/ShortDramaApp.swift`
- 容器结构：`AppShellView` 使用 `TabView(selection: $router.selectedTab)` 渲染 5 个 Tab，并在最外层承载菜单 overlay 与登录 `fullScreenCover`
- 一级频道：`home`、`theater`、`mall`、`earn`、`profile`
- 首页子路由：`searchHome`、`searchResult(query:)`、`rankingHome`、`classificationHome`、`messages`、`bookingAssets`、`player(videoId:)`、`dramaDetail(dramaId:)`
- 承载现状：
  - `home`：`HomeView` + `MenuPanelContainerView` + `CheckInPopupView` + `MessageCenterView` + `BookingAssetsView`
  - `theater`：`TheaterView`
  - `mall`：`MallContainerView`
  - `earn`：`EarnContainerView` + `.earnLogin` + `.earnPlayer`
  - `profile`：`ProfileHomeView` / `SettingsView` + 登录弹层

## 核心逻辑

### Web 端
1. 继续提供用户端路由骨架与管理后台入口，不承担移动端同构应用壳职责。
2. `/mall` 与 `/earn` 分别作为商城 / 赚钱 H5 页面入口，供 Native 容器加载。
3. `/play/[id]`、`/detail/[id]`、`/search`、`/rankings` 主要承担路由语义，不构成完整 Native 体验对等实现。

### Backend 端
1. 服务首页继续作为运行信息与入口页，不参与移动端壳层渲染。
2. `/api/health` 提供健康检查。
3. `/api/dramas*`、`/api/player/recently-viewed`、`/api/auth/*`、`/api/check-ins*`、`/api/messages*`、`/api/users/me/bookings`、`/api/mall/*`、`/api/earn/*` 共同承载移动端首页、剧场、菜单、登录、预约资产、商城与赚钱容器的主数据面。
4. 排行列表走可选鉴权，预约接口、预约资产接口、互动消息、赚钱代表性任务结算等场景继续要求真实登录态。

### Android 端
1. `MainActivity` 在冷启动与 `onNewIntent` 两条路径下统一接收 deeplink，并把解析结果写入导航状态。
2. `NavGraph` 通过 `Scaffold(bottomBar = { NavigationBar { ... }})` 渲染 5 个 Tab，切换时保留多 back stack 状态。
3. 首页 graph 当前承载真实 `HomeScreen` Feed、签到浮层、菜单抽屉、消息中心、预约资产页、搜索发现、排行与分类；菜单关闭后才执行待导航目标。
4. `menu/booking` 已接入 `BookingAssetsScreen`，匿名态通过统一登录页承接并保留 `returnRoute=menu/booking`；`menu/downloads` 继续为 Native 占位页。
5. `mall` tab 已接入商城 H5 容器；`earn` tab 已接入赚钱 H5 容器、专属登录承接与任务播放器回流；`profile` graph 已形成完整账号链路。

### iOS 端
1. `ShortDramaApp` 持有单例 `NavigationRouter` 并通过 `.environmentObject(router)` 注入全局导航状态。
2. `AppShellView` 以 `TabView` 渲染 5 个一级频道，并在最外层 `ZStack` 中叠加 `MenuPanelContainerView`；同时通过 `.fullScreenCover(item: presentedLoginContext)` 承载登录页。
3. `NavigationRouter` 为每个 Tab 维护独立 `NavigationPath`，并统一维护 `menuPanelState`、`pendingMenuNavigation`、登录上下文、商城登录上下文与赚钱登录上下文。
4. `MenuPanelContainerView` 已把“我的预约”绑定到 `.bookingAssets`，登录成功后由 `completeLogin()` 恢复 booking route；“我的下载”仍进入 `.menuPlaceholder(kind: .downloads)`。
5. `TabNavigationHostView` 已把 `.messages` 绑定到 `MessageCenterView`、把 `.bookingAssets` 绑定到 `BookingAssetsView`、把 `.mall` 绑定到 `MallContainerView`、把 `.earn` 绑定到 `EarnContainerView`，并支持 `.earnPlayer` 的原生播放器承接。

## 多端实现

### Web
- 源文件：`web/src/app/layout.tsx`、`web/src/app/page.tsx`、`web/src/app/search/page.tsx`、`web/src/app/rankings/page.tsx`、`web/src/app/mall/**`、`web/src/app/earn/page.tsx`
- 技术：Next.js 16、React 19、TypeScript、SSR-first App Router

### Android
- 入口与容器：`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`
- 首页 / 签到 / 菜单 / 消息 / 预约资产 / 搜索 / 排行 / 分类承载：`android/app/src/main/java/com/djs66256/short_drama/feature/home/**`、`feature/menu/**`、`feature/messages/**`、`feature/booking/**`、`feature/search/**`、`feature/ranking/**`、`feature/classification/**`
- 剧场 / 商城 / 赚钱 / 我的：`feature/theater/**`、`feature/mall/**`、`feature/earn/**`、`feature/profile/**`、`feature/auth/**`
- 技术：Kotlin 2.0.21、Jetpack Compose、Material3、Navigation Compose、WebView、Hilt

### iOS
- 入口与容器：`ios/ShortDrama/Sources/App/ShortDramaApp.swift`、`ios/ShortDrama/Sources/App/AppShellView.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`
- 首页 / 签到 / 菜单 / 消息 / 预约资产 / 搜索 / 排行 / 分类承载：`ios/ShortDrama/Sources/Features/Home/**`、`Features/MenuPanel/**`、`Features/Messages/**`、`Features/BookingAssets/**`、`Features/Search/**`、`Features/Ranking/**`、`Features/Classification/**`
- 剧场 / 商城 / 赚钱 / 我的：`Features/Theater/**`、`Features/Mall/**`、`Features/Earn/**`、`Features/Profile/**`、`Features/Auth/**`
- 技术：Swift 6、SwiftUI、TabView、NavigationStack、WKWebView、Swift Testing

### Backend
- 服务首页：`backend/src/app/page.tsx`
- 首页 Feed / 剧场 / 搜索 / 热搜 / 分类 / 排行接口：`backend/src/app/api/dramas/**`
- 登录、签到、消息、预约资产、商城、赚钱与最近在看接口：`backend/src/app/api/auth/**`、`backend/src/app/api/check-ins/**`、`backend/src/app/api/messages/**`、`backend/src/app/api/users/me/bookings/route.ts`、`backend/src/app/api/mall/**`、`backend/src/app/api/earn/**`、`backend/src/app/api/player/**`
- 技术：Next.js 16、TypeScript、App Router Route Handlers

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/health` | [../../api/health.md](../../api/health.md) | 服务健康检查 |
| `GET /api/dramas` | [../../api/dramas.md](../../api/dramas.md) | 首页频道 Feed 数据源 |
| `GET /api/check-ins/status` | [../../api/check-ins.md](../../api/check-ins.md) | 首页签到状态查询 |
| `POST /api/check-ins` | [../../api/check-ins.md](../../api/check-ins.md) | 首页签到提交 |
| `GET /api/messages/preview` | [../../api/messages.md](../../api/messages.md) | 菜单消息预览 |
| `GET /api/messages/system` | [../../api/messages.md](../../api/messages.md) | 消息中心系统消息 |
| `GET /api/messages/interactions` | [../../api/messages.md](../../api/messages.md) | 消息中心互动消息 |
| `GET /api/users/me/bookings` | [../../api/user-assets.md](../../api/user-assets.md) | 菜单“我的预约”真实资产页数据源 |
| `GET /api/dramas/channel` | [../../api/dramas.md](../../api/dramas.md) | 剧场频道 Feed 数据源 |
| `GET /api/dramas/search` | [../../api/dramas.md](../../api/dramas.md) | 搜索结果与分类承接 |
| `GET /api/dramas/hot-search` | [../../api/dramas.md](../../api/dramas.md) | 搜索发现热搜数据源 |
| `GET /api/dramas/tags` | [../../api/dramas.md](../../api/dramas.md) | 分类标签矩阵 |
| `GET /api/dramas/rankings` | [../../api/dramas.md](../../api/dramas.md) | 排行页数据源 |
| `POST /api/dramas/:id/book` | [../../api/dramas.md](../../api/dramas.md) | 排行预约接口 |
| `GET /api/player/recently-viewed` | [../../api/player.md](../../api/player.md) | 菜单最近在看 |
| `POST /api/auth/otp-requests` | [../../api/auth.md](../../api/auth.md) | 登录页发送验证码 |
| `POST /api/auth/sessions` | [../../api/auth.md](../../api/auth.md) | 验证码登录 / 自动注册 |
| `POST /api/auth/session-refreshes` | [../../api/auth.md](../../api/auth.md) | 会话刷新 |
| `GET /api/users/me` | [../../api/auth.md](../../api/auth.md) | 当前用户校验 |
| `DELETE /api/auth/session` | [../../api/auth.md](../../api/auth.md) | 设置页退出登录 |
| `GET /api/mall/products` | [../../api/mall.md](../../api/mall.md) | 商城商品 Feed |
| `GET /api/earn/overview` | [../../api/earn.md](../../api/earn.md) | 赚钱首页聚合数据 |
| `POST /api/earn/complete-task` | [../../api/earn.md](../../api/earn.md) | 赚钱代表性任务结算 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 |
|------|---------|--------|------|
| Android `pendingRoute` | `MutableStateFlow` | 应用级 | deeplink 与菜单关闭后的最终导航目标先入队，待容器 ready 后消费 |
| Android `menuPanelState` / `pendingMenuRoute` | `MutableStateFlow` | 应用级 | 菜单 opening / open / closing 状态与“先关菜单再导航”目标 |
| Android booking 页面状态 | `BookingAssetsUiState` + route `returnRoute=menu/booking` | 页面级 | 持有双 Tab、summary、分页、登录门槛与登录回流目标 |
| Android 多 Tab 栈 | `NavController` + `saveState/restoreState` | Tab 级 | 切换频道时保留已访问 graph 的返回栈 |
| iOS `selectedTab` | `@Published` | 应用级 | 控制当前激活的一级频道 |
| iOS `pathsByTab` | `[AppTab: NavigationPath]` | Tab 级 | 每个 Tab 独立维护导航路径 |
| iOS `menuPanelState` / `pendingMenuNavigation` | `@Published private(set)` | 应用级 | 控制菜单 overlay 显隐与关闭后导航 |
| iOS booking 页面状态 | `BookingAssetsViewModel` + `presentedLoginContext.returnRoute = .bookingAssets` | 页面级 / 应用级 | 持有双 Tab、summary、分页、登录门槛与登录回流目标 |
| iOS 登录 / 商城登录 / 赚钱登录上下文 | `presentedLoginContext` / `mallLoginContext` / `earnLoginContext` | 应用级 | 区分不同来源和回流目标 |
| Backend route state | `request.auth` + route params / query | 请求级 | 路由层负责请求级主体解析与 contract 输出 |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 深链 | 共享导航容器 | deeplink 解析后的目标由应用壳负责承载和跳转 |
| 搜索发现 | 首页子路由 | 排行和分类继续挂在首页频道内 |
| 菜单面板 | 首页 overlay | 抽屉、最近在看、消息预览与菜单导航都由应用壳统一承载 |
| 消息系统 | 菜单入口 + 首页拥有消息页 | 菜单消息入口、预览状态与登录回流依赖“先关菜单再导航” |
| 个人资产管理 | 菜单入口 + 登录回流 | “我的预约”依赖应用壳承载真实 booking route、菜单关闭后导航与登录后回 booking route |
| 认证体系 | 登录页承载与登录后回跳 | “我的”、排行预约、消息互动、预约资产、商城登录与赚钱登录都依赖应用壳承载 |
| 签到能力 | 首页容器 overlay | 签到浮层挂在首页首屏内容之上，并与评论 / 登录模态互斥 |
| 剧场频道 | 独立一级 Tab | 剧场 feed 留在 theater tab，发现链路继续复用首页导航栈 |
| 商城频道 | H5 容器承载 | 匿名商品点击可回到统一 Native 登录承接 |
| 赚钱中心 | H5 容器承载 + Native 回流 | 赚钱依赖宿主登录同步、任务播放器承接与奖励结算回流 |
| 评论能力 | 页面内容器 | 首页 / 播放器内嵌评论 sheet，不新增 comments 顶级路由 |

### 外部依赖

| 服务 / 框架 | 用途 | 接入方式 |
|-------------|------|---------|
| Next.js App Router | Web / Backend 路由承载 | 文件系统路由 + Route Handlers |
| Navigation Compose | Android 多级导航与状态恢复 | `NavHost` + nested navigation graph |
| SwiftUI `TabView` / `NavigationStack` | iOS 一级频道与子路由承载 | 声明式导航容器 |
| RESTful APIs | 移动端首页、签到、消息、登录、预约资产、商城、赚钱数据加载 | 各端统一消费 Backend Route Handlers |

## 已知限制

- Web 端当前未实现与移动端对等的首页 Feed、菜单面板、剧场频道、签到浮层、消息中心、预约资产页或用户端登录页；其主要已落地能力仍是路由壳、商城 / 赚钱 H5 页面与管理后台。
- 菜单中的登录与下载仍有部分 Native 占位承接页；其中“我的预约”已切换为真实 booking 页面，“我的下载”仍保持占位；游戏中心仅提供“即将上线”本地反馈，不导航到真实业务页面。
- 播放页与详情页仍以承载导航语义、评论容器和赚钱任务承接为主，不包含完整内容详情业务数据。
- Backend 最近在看、播放进度与部分内容数据当前仍主要来自 mock / mixed repository，不是完整线上内容服务。
- iOS 排行登录成功后只回到 `.rankingHome`，不显式恢复更细粒度 query；Android 会保留完整 `ranking?...` returnRoute。
- 设备级黑盒验证在本轮 workflow 中按规范降级，当前证据主要来自自动化测试、QA 文档与代码审查。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-08-03 | 更新：合并 PRD-10、PRD-11、PRD-13、PRD-14 的应用壳现状，统一补充签到、消息、预约资产、商城 H5、赚钱 H5 与登录 / 任务回流承载说明 |
| 2026-07-29 | 更新：同步 PRD-08 登录闭环后“我的”频道真实登录 / 设置 / 退出登录承载 |
| 2026-07-28 | 更新：同步 PRD-12 剧场频道落地结果 |
| 2026-07-28 | 更新：同步 PRD-07 菜单面板落地结果 |
| 2026-07-27 | 更新：同步 PRD-06 分类浏览落地结果 |
| 2026-07-27 | 更新：同步 PRD-05 排行体系落地结果 |
| 2026-07-26 | 更新：同步 PRD-02 首页信息流落地结果 |
| 2026-07-25 | 更新：移动端应用壳从单页骨架演进为 5 Tab 导航容器 |

---
*本文档由 llm-wiki skill 自动维护。*
