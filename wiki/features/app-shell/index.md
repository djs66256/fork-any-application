# 应用壳 (App Shell)

> 最后更新：2026-08-03

## 功能概述

应用壳负责承载各端应用的启动入口、路由容器与基础页面骨架。当前能力已经覆盖：

- PRD-01：移动端 5 个一级频道的底部导航容器
- PRD-02：首页 Native Feed 首屏
- PRD-05：搜索发现与排行浏览链路
- PRD-06：分类浏览链路
- PRD-07：首页汉堡菜单、左侧抽屉、最近在看与关闭后导航时序
- PRD-08：“我的”频道真实登录 / 设置 / 退出登录承载
- PRD-10：首页签到浮层、菜单消息预览与独立消息中心
- PRD-12：剧场频道真实 feed 承载
- PRD-13：商城频道 H5 容器接入、搜索 / 登录 bridge 与商城上下文恢复
- PRD-14：赚钱频道 H5 容器接入、earn 专属登录承接、任务播放器承接与 host message 回流

Web 端继续维持 SSR-first 的 Next.js App Router 结构，不提供与移动端对等的用户端应用壳；商城（mall）与赚钱（earn）遵循 H5 承载策略，Android / iOS 分别通过 WebView / WKWebView 提供宿主容器。

- **覆盖端**：Web、Android、iOS、Backend
- **核心价值**：为首页 Feed、签到浮层、菜单抽屉、消息入口、搜索发现、排行、分类、剧场频道、商城 / 赚钱 H5 容器，以及“我的”频道登录 / 设置链路提供统一承载容器
- **当前状态**：移动端 5 Tab 骨架已落地；首页频道已接入 Feed、签到、菜单、消息、搜索发现、排行与分类；剧场为真实频道；商城与赚钱为真实 H5 容器；“我的”频道为真实登录 / 设置承载

## 入口与路由

### Web
- 入口组件：`web/src/app/layout.tsx`、`web/src/app/page.tsx`
- 路由方案：Next.js App Router
- 当前用户端可访问路由：`/`、`/play/[id]`、`/detail/[id]`、`/search`、`/rankings`、`/mall`、`/mall/product/[id]`、`/earn`
- 当前现状：Web 首页仍是信息壳与代表性链接，不承载移动端同等的 Feed、签到、消息、菜单或“我的”频道

### Backend
- 入口组件：`backend/src/app/layout.tsx`、`backend/src/app/page.tsx`
- 提供 `/` 服务信息页与 `/api/health`
- 为应用壳提供的主数据与能力接口包括：
  - 首页 / 发现：`GET /api/dramas`、`GET /api/dramas/search`、`GET /api/dramas/hot-search`、`GET /api/dramas/tags`、`GET /api/dramas/rankings`、`POST /api/dramas/:id/book`
  - 剧场：`GET /api/dramas/channel`
  - 签到：`GET /api/check-ins/status`、`POST /api/check-ins`
  - 消息：`GET /api/messages/preview`、`GET /api/messages/system`、`GET /api/messages/interactions`
  - 菜单最近在看 / 播放历史：`GET /api/player/recently-viewed`、`POST /api/player/start`、`POST /api/player/stop`
  - 认证：`POST /api/auth/otp-requests`、`POST /api/auth/sessions`、`POST /api/auth/session-refreshes`、`GET /api/users/me`、`DELETE /api/auth/session`
  - 商城 / 赚钱：`GET /api/mall/products`、`GET /api/earn/overview`、`POST /api/earn/complete-task`

### Android
- 入口 Activity：`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt`
- 容器：`NavGraph` 统一承载 `NavigationBar`、home graph、剧场 graph、商城 / 赚钱容器、profile graph、菜单 overlay、签到浮层与 snackbar
- 首页子路由涵盖：搜索发现、排行、分类、菜单承接页、消息中心、播放页、详情页、登录页
- 商城与赚钱：分别由 mall / earn 容器承载 H5 页面，并通过 bridge / host sync 与 Native 登录态、任务播放承接配合

### iOS
- 入口 App：`ios/ShortDrama/Sources/App/ShortDramaApp.swift`
- 容器：`AppShellView` 使用 `TabView(selection: $router.selectedTab)` 渲染 5 个 Tab，并在最外层承载菜单 overlay 与登录 `fullScreenCover`
- 首页子路由涵盖：`searchHome`、`searchResult(query:)`、`rankingHome`、`classificationHome`、`messages`、`player(videoId:)`、`dramaDetail(dramaId:)`
- 商城与赚钱：`TabNavigationHostView` 已分别绑定 `MallContainerView()` 与 `EarnContainerView()`；赚钱场景额外支持 `.earnLogin` 与 `.earnPlayer` 回流

## 核心逻辑

### Web 端
1. 继续提供用户端路由骨架与管理后台入口，不承担移动端同构应用壳职责。
2. `/mall` 与 `/earn` 分别作为商城 / 赚钱 H5 页面入口，供 Native 容器加载。
3. `/play/[id]`、`/detail/[id]`、`/search`、`/rankings` 主要承担路由语义，不构成完整 Native 体验对等实现。

### Backend 端
1. 首页 / 剧场 / 搜索 / 排行 / 分类接口共同构成应用壳下的内容发现数据源。
2. `check-ins` 与 `messages` 接口分别支撑首页签到浮层、菜单消息预览与消息中心。
3. Auth API 负责移动端登录闭环；mall / earn API 分别支撑 H5 容器首屏与任务结算。
4. 最近在看统一收敛到 `GET /api/player/recently-viewed`，由菜单面板消费。

### Android 端
1. `MainActivity` 在冷启动与 `onNewIntent` 路径下统一处理 deeplink 与 debug verification 入口。
2. `NavGraph` 负责 5 Tab 导航、多 back stack、首页菜单 overlay 与受保护路由承接。
3. 首页 `HomeScreen` 同时是 Feed、签到浮层与评论 bottom sheet 的宿主。
4. 菜单关闭后再执行导航，避免 closing 阶段重复点击导致多次跳转。
5. mall / earn H5 容器通过 bridge 复用 Native 搜索、登录、任务播放与上下文恢复能力。

### iOS 端
1. `NavigationRouter` 为每个 Tab 维护独立 `NavigationPath`，并统一维护菜单状态、登录上下文与 H5 回流状态。
2. `AppShellView` 通过 `TabView + ZStack + fullScreenCover` 承载页面栈、菜单 overlay 与登录弹层。
3. `HomeView` 是首页 Feed、签到浮层与评论 sheet 的宿主；消息中心继续挂在 home tab 导航栈内。
4. `TabNavigationHostView` 已注册 `MessageCenterView`、`MallContainerView`、`EarnContainerView`、`SettingsView` 等真实页面。
5. 赚钱任务通过 `.earnPlayer` 打开原生播放器承接，完成后再把结果回传 H5。

## 多端实现

### Web
- 入口与路由：`web/src/app/layout.tsx`、`web/src/app/page.tsx`
- 用户端壳：`web/src/features/home/HomeScreen.tsx`
- 商城 / 赚钱 H5：`web/src/features/mall/**`、`web/src/features/earn/**`
- 技术：Next.js 16、React 19、TypeScript

### Android
- 入口与容器：`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`
- 首页 / 签到 / 菜单：`android/app/src/main/java/com/djs66256/short_drama/feature/home/**`、`feature/menu/**`
- 消息：`android/app/src/main/java/com/djs66256/short_drama/feature/messages/**`
- 剧场 / 排行 / 分类 / 搜索：`feature/theater/**`、`feature/ranking/**`、`feature/classification/**`、`feature/search/**`
- 商城 / 赚钱：`feature/mall/**`、`feature/earn/**`
- 技术：Kotlin 2.0.21、Jetpack Compose、Material3、Navigation Compose、Hilt

### iOS
- 入口与容器：`ios/ShortDrama/Sources/App/ShortDramaApp.swift`、`ios/ShortDrama/Sources/App/AppShellView.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`
- 首页 / 签到 / 菜单：`ios/ShortDrama/Sources/Features/Home/**`、`Features/MenuPanel/**`
- 消息：`ios/ShortDrama/Sources/Features/Messages/**`
- 剧场 / 排行 / 分类 / 搜索：`Features/Theater/**`、`Features/Ranking/**`、`Features/Classification/**`、`Features/Search/**`
- 商城 / 赚钱：`ios/ShortDrama/Sources/Features/Mall/**`、`Features/Earn/**`
- 技术：Swift 6、SwiftUI、TabView、NavigationStack、Swift Testing

### Backend
- 内容发现：`backend/src/app/api/dramas/**`
- 签到：`backend/src/app/api/check-ins/**`
- 消息：`backend/src/app/api/messages/**`
- 认证：`backend/src/app/api/auth/**`、`backend/src/app/api/users/me/route.ts`
- 商城 / 赚钱：`backend/src/app/api/mall/**`、`backend/src/app/api/earn/**`
- 技术：Next.js 16、TypeScript、App Router Route Handlers

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/dramas` | [../../api/dramas.md](../../api/dramas.md) | 首页 Feed 数据源 |
| `GET /api/check-ins/status` | [../../api/check-ins.md](../../api/check-ins.md) | 首页签到状态查询 |
| `POST /api/check-ins` | [../../api/check-ins.md](../../api/check-ins.md) | 首页签到提交 |
| `GET /api/messages/preview` | [../../api/messages.md](../../api/messages.md) | 首页菜单消息预览 |
| `GET /api/messages/system` | [../../api/messages.md](../../api/messages.md) | 消息中心系统消息分区 |
| `GET /api/messages/interactions` | [../../api/messages.md](../../api/messages.md) | 消息中心互动消息分区 |
| `GET /api/dramas/channel` | [../../api/dramas.md](../../api/dramas.md) | 剧场频道 Feed |
| `GET /api/dramas/search` | [../../api/dramas.md](../../api/dramas.md) | 搜索结果与分类结果承接 |
| `GET /api/dramas/hot-search` | [../../api/dramas.md](../../api/dramas.md) | 搜索发现热搜 |
| `GET /api/dramas/tags` | [../../api/dramas.md](../../api/dramas.md) | 分类页标签矩阵 |
| `GET /api/dramas/rankings` | [../../api/dramas.md](../../api/dramas.md) | 排行页数据源 |
| `POST /api/dramas/:id/book` | [../../api/dramas.md](../../api/dramas.md) | 预约榜提交 |
| `GET /api/player/recently-viewed` | [../../api/player.md](../../api/player.md) | 菜单最近在看数据源 |
| `GET /api/mall/products` | [../../api/mall.md](../../api/mall.md) | 商城 H5 首页数据源 |
| `GET /api/earn/overview` | [../../api/earn.md](../../api/earn.md) | 赚钱 H5 首页聚合数据 |
| `POST /api/earn/complete-task` | [../../api/earn.md](../../api/earn.md) | 赚钱代表性任务奖励结算 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 |
|------|---------|--------|------|
| Android `pendingRoute` / `pendingMenuRoute` / `menuPanelState` | `MutableStateFlow` | 应用级 | 统一承载 deeplink、菜单关闭后导航和 overlay 状态 |
| Android 多 Tab 栈 | `NavController` + `saveState/restoreState` | Tab 级 | 切换频道时保留导航返回栈 |
| Android 首页 Feed / 签到 / 消息 / 评论状态 | 各页面 ViewModel `StateFlow` | 页面级 | 首页作为多能力宿主，分别维护子域状态 |
| iOS `selectedTab` / `pathsByTab` | `@Published` | 应用级 / Tab 级 | 每个 Tab 维护独立 `NavigationPath` |
| iOS `menuPanelState` / `pendingMenuNavigation` | `@Published` | 应用级 | 控制首页菜单 overlay 和延迟导航 |
| iOS 登录 / mall / earn 上下文 | `presentedLoginContext`、`mallLoginContext`、`earnLoginContext` | 应用级 | 承接 profile、消息、商城、赚钱登录与任务回流 |
| Backend route state | `request.auth` + route params / query | 请求级 | 路由层负责请求级主体解析与 contract 输出 |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 深链 | 共享导航容器 | deeplink 解析后的目标由应用壳负责承载和跳转 |
| 搜索发现 / 排行 / 分类 | 首页子路由 | 属于首页发现链路延伸，不新增一级频道 |
| 签到能力 | 首页 overlay | 冷启动后挂在首页内容上方，与评论 / 登录模态互斥 |
| 消息系统 | 菜单入口 + 首页导航栈 | 菜单预览、消息页与登录回流依赖 close-menu-then-navigate 机制 |
| 认证体系 | 登录页承载与回跳 | “我的”频道、排行预约、消息互动、商城 / 赚钱登录都依赖应用壳统一承接 |
| 商城 / 赚钱 | H5 容器宿主 | Native 提供 bridge、auth sync、任务承接与上下文恢复 |
| 评论能力 | 页面内容器 | 首页 / 播放器内嵌评论 sheet，不新增 comments 顶级路由 |

### 外部依赖

| 服务 / 框架 | 用途 | 接入方式 |
|-------------|------|---------|
| Next.js App Router | Web / Backend 路由承载 | 文件系统路由 + Route Handlers |
| Navigation Compose | Android 多级导航与状态恢复 | `NavHost` + nested graph |
| SwiftUI `TabView` / `NavigationStack` | iOS 一级频道与子路由承载 | 声明式导航容器 |
| Backend RESTful API | 移动端内容、认证、签到、消息与 H5 容器能力 | Android / iOS / Web 统一调用 |

## 已知限制

- Web 端当前只提供用户端路由壳与 H5 页面，不实现移动端对等的底部导航、菜单、签到、消息中心、剧场或“我的”频道。
- 播放页与详情页跨端仍以承载导航语义、评论容器与任务回流为主，不是完整线上业务页面。
- 菜单中的预约、下载与游戏中心仍有占位承接；游戏中心只提供本地“即将上线”反馈。
- 商城与赚钱虽然都已接入真实容器，但 H5 内容仅覆盖当前 PRD 范围，不代表完整线上业务。
- Backend 当前仍采用 mixed repository 运行结构，首页 / 剧场 / 搜索等子域仍受 seed / mock 数据边界影响。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-08-03 | 更新：合并 PRD-10、PRD-13、PRD-14 的应用壳现状，统一补充签到、消息、商城 H5、赚钱 H5 与登录 / 任务回流承载说明 |
| 2026-07-29 | 更新：同步 PRD-08 登录闭环后“我的”频道真实登录 / 设置 / 退出登录承载 |
| 2026-07-28 | 更新：同步 PRD-12 剧场频道落地结果 |
| 2026-07-28 | 更新：同步 PRD-07 菜单面板落地结果 |
| 2026-07-27 | 更新：同步 PRD-06 分类浏览落地结果 |
| 2026-07-27 | 更新：同步 PRD-05 排行体系落地结果 |
| 2026-07-26 | 更新：同步 PRD-02 首页信息流落地结果 |
| 2026-07-25 | 更新：移动端应用壳从单页骨架演进为 5 Tab 导航容器 |

---
*本文档由 llm-wiki skill 自动维护。*
