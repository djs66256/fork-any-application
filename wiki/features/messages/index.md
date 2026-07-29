# 消息系统 (Messages)

> 最后更新：2026-07-29
> 覆盖端：Android / iOS / Backend（Web 本期不实现）

## 功能概述

PRD-10 把首页菜单中的“我的消息”从静态预览与占位承接，推进为“菜单消息预览 + 独立消息中心页”的真实消息系统。当前消息系统分为两层：

1. 菜单抽屉中的消息预览，只展示最新 1 条系统消息摘要，匿名也可访问；
2. 独立消息中心页，拆成“系统消息”和“互动消息”两个分区，其中系统消息匿名可看，互动消息要求登录（`backend/src/app/api/messages/preview/route.ts:9-20`、`backend/src/app/api/messages/system/route.ts:10-23`、`backend/src/app/api/messages/interactions/route.ts:11-30`）。

- 核心价值：把 PRD-07 菜单入口、PRD-08 登录承接与 PRD-10 站内消息消费串成真实链路
- 覆盖范围：菜单消息预览、消息中心路由、系统消息分页、互动消息登录门槛、登录后刷新互动消息
- 当前状态：Android / iOS / Backend 已落地；Web 用户端不实现消息中心（`docs/specs/2026-07-29-prd-10-signin-messages/spec.md:36-45,71-74`）

## 入口与路由

| 端 | 入口 | 路由 / 触发方式 | 源文件 |
|----|------|----------------|--------|
| Android | 首页汉堡菜单中的“我的消息”模块 | `PendingRoute.MenuMessages` → `menu/messages` | `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:61-65,152-158,176-179`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:135-146,286-298` |
| iOS | 首页菜单中的“我的消息”模块 | `router.closeMenuPanelThenNavigate(to: .messages)` | `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift:25-33`、`ios/ShortDrama/Sources/App/AppRoute.swift:23-31,53-82` |
| Android | 消息中心内互动分区登录按钮 | 跳转 `login?returnRoute=menu/messages&source=menu_messages` | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:286-297` |
| iOS | 消息中心内互动分区登录按钮 | `router.presentLogin(context: viewModel.loginContext)`，其中 `returnRoute = .messages` | `ios/ShortDrama/Sources/Features/Messages/Views/MessageCenterView.swift:75-84`、`ios/ShortDrama/Sources/Features/Messages/ViewModels/MessageCenterViewModel.swift:29-43` |
| Backend | 菜单消息预览接口 | `GET /api/messages/preview` | `backend/src/app/api/messages/preview/route.ts:9-20` |
| Backend | 系统消息列表接口 | `GET /api/messages/system?page&pageSize` | `backend/src/app/api/messages/system/route.ts:10-23` |
| Backend | 互动消息列表接口 | `GET /api/messages/interactions?page&pageSize` | `backend/src/app/api/messages/interactions/route.ts:11-30` |
| Web | N/A | 本期不实现消息中心 | `docs/specs/2026-07-29-prd-10-signin-messages/spec.md:36-45` |

## 核心逻辑

### 流程：菜单消息预览

1. 菜单面板打开后，消息预览和“最近在看”并发加载，而不是串行阻塞（`android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt:98-121`）。
2. Backend `GET /api/messages/preview` 从系统消息仓储取最新 1 条消息；若没有数据则返回 `204 No Content`，而不是 `200 + null`（`backend/src/app/api/messages/preview/route.ts:14-18`、`backend/src/services/message/message.service.ts:40-61`）。
3. `MessageService.getPreview()` 会把 `sent_at` 格式化为相对时间文案，如“2小时前”或“5分钟前”（`backend/src/services/message/message.service.ts:17-27,47-54`）。
4. Android 数据层把 `204` 视为成功空态，菜单状态转换为 `preview = null`（`android/app/src/main/java/com/djs66256/short_drama/data/datasource/MessageRemoteDataSource.kt:20-39`）。
5. iOS 也把 preview 结果建模为 `content / empty / error`，并在 503 或失败时降级为“暂无消息”或静态兜底文案（`ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift:97-154`）。

### 流程：进入消息中心并查看双分区

1. 用户从菜单点击“我的消息”后，两端都先关闭菜单，再进入真实消息中心页（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:135-146,484-498`、`ios/ShortDrama/Sources/App/NavigationRouter.swift:118-140`）。
2. 消息中心页面首屏同时承载两个分区：
   - 系统消息：匿名可访问
   - 互动消息：登录可访问，匿名展示门槛（`ios/ShortDrama/Sources/Features/Messages/Views/MessageCenterView.swift:13-35,47-98`、`android/app/src/main/java/com/djs66256/short_drama/feature/messages/viewmodel/MessageCenterViewModel.kt:22-47`）。
3. Backend `messages/system` 与 `messages/interactions` 共用 `MessageListQuerySchema`，分页参数统一为 `page/pageSize`，上限 20（`backend/src/app/api/messages/system/route.ts:10-23`、`backend/src/app/api/messages/interactions/route.ts:11-30`、`backend/src/lib/schemas.ts:149-159`）。
4. 互动消息 route 通过 `requireAuthContext()` 强制登录，并从 `getAuth(request)` 取 `userId`；匿名请求会拿到 `AUTH_UNAUTHORIZED`（`backend/src/app/api/messages/interactions/route.ts:11-29`、`backend/src/middleware/auth.ts:97-138`、`backend/src/services/message/message.service.ts:74-97`）。
5. 消息中心的两个分区相互隔离：互动消息失败不会影响系统消息区；系统消息失败也不阻塞互动区逻辑（`android/app/src/main/java/com/djs66256/short_drama/feature/messages/viewmodel/MessageCenterViewModel.kt:93-176`、`ios/ShortDrama/Sources/Features/Messages/ViewModels/MessageCenterViewModel.swift:62-111`、`docs/specs/2026-07-29-prd-10-signin-messages/qa-test.md:242-260`）。

### 流程：匿名用户登录后留在消息页

1. Android 消息页登录按钮固定携带 `returnRoute = menu/messages` 与 `source = menu_messages`（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:286-297`）。
2. iOS 使用 `LoginInterceptionContext(source: .messagesEntry, returnRoute: .messages)` 触发统一登录承接（`ios/ShortDrama/Sources/Features/Messages/ViewModels/MessageCenterViewModel.swift:29-43`、`ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift:3-29`）。
3. 登录成功后：
   - Android `LoginScreen.onLoginSuccess` 会重新导航回 `menu/messages`（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:471-479`）。
   - iOS `NavigationRouter.completeLogin()` 在 `.messages` returnRoute 下保持 home tab，并由消息页监听 `authStore.$status` 后触发互动分区刷新（`ios/ShortDrama/Sources/App/NavigationRouter.swift:174-204`、`ios/ShortDrama/Sources/Features/Messages/Views/MessageCenterView.swift:27-35`）。
4. QA 文档把“登录后停留消息页并加载互动消息”列为 P1 用例，当前自动化已覆盖，设备黑盒仍待补测（`docs/specs/2026-07-29-prd-10-signin-messages/qa-test.md:133-149,303-307`）。

### 数据源与运行时边界

- 系统消息支持 mock / supabase 双实现，preview 与 list 共用同一仓储（`backend/src/repositories/repository-registry.ts:59-65,128-134`）。
- 互动消息首版固定使用 mock repository，不存在 `interaction_messages` 表（`backend/src/repositories/repository-registry.ts:67-69,136-142`、`backend/supabase/migrations/20260729001000_create_signin_and_system_messages.sql:24-40`）。
- `system_messages` 表是当前真实持久化结构，按 `sent_at DESC` 排序支持 preview 与列表（`backend/supabase/migrations/20260729001000_create_signin_and_system_messages.sql:24-40`）。

### 边界与异常处理

| 场景 | 处理方式 | 源文件 |
|------|---------|--------|
| 菜单消息预览无数据 | Backend 返回 204，客户端渲染“暂无消息” | `backend/src/app/api/messages/preview/route.ts:16-18`、`android/app/src/main/java/com/djs66256/short_drama/data/datasource/MessageRemoteDataSource.kt:20-25` |
| 互动消息未登录 | Backend 返回 401 / `AUTH_UNAUTHORIZED`；客户端展示登录门槛 | `backend/src/services/message/message.service.ts:79-81`、`ios/ShortDrama/Sources/Features/Messages/ViewModels/MessageCenterViewModel.swift:79-85,102-107` |
| 互动消息失败 | 只影响互动分区，不影响系统消息 | `android/app/src/main/java/com/djs66256/short_drama/feature/messages/viewmodel/MessageCenterViewModel.kt:142-176`、`docs/specs/2026-07-29-prd-10-signin-messages/qa-test.md:242-260` |
| 登录成功回流 | 保持消息页上下文，不回首页占位页 | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:286-297,471-479`、`ios/ShortDrama/Sources/App/NavigationRouter.swift:183-203` |

## 多端实现

### Android

- 菜单预览状态：`MessagePreviewUiState` 区分 preview、error 和 empty（`android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt:38-50,171-189`）
- 菜单加载时机：`recentlyViewed` 与 `messagePreview` 使用 `awaitAll` 并发请求（`android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt:110-121`）
- 消息中心路由：`menu/messages` 已从 placeholder 切到真实 `MessageCenterScreen`，且该页隐藏底部导航栏（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:286-298,507-512`）
- 鉴权注入：`AuthInterceptor` 只为 `messages/interactions` 自动补 bearer token，preview/system 不要求登录（`android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt:38-50`）
- 自动化证据：QA 文档确认 Android 的 ApiService/DTO/Repository/ViewModel/路由测试已通过（`docs/specs/2026-07-29-prd-10-signin-messages/qa-test.md:266-283`）

### iOS

- 菜单预览状态：`MenuPanelViewModel.MessagePreviewState` 区分 `idle/loading/content/empty/error`（`ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift:13-19`）
- 菜单入口：`MenuPanelContainerView` 直接把消息入口路由到 `.messages`（`ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift:25-33`）
- 真实消息 route：`AppRoute.messages` 已成为 home tab 拥有的页面，不再复用 placeholder route（`ios/ShortDrama/Sources/App/AppRoute.swift:23-31,32-51,53-82`）
- 登录承接：`LoginInterceptionContext.Source.messagesEntry` 专用于消息页互动分区登录门槛（`ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift:3-29`）
- 消息中心状态：`MessageCenterViewModel` 把系统消息和互动消息拆成两个 `SectionState`，并在 `authStore` 变化时刷新互动区（`ios/ShortDrama/Sources/Features/Messages/ViewModels/MessageCenterViewModel.swift:17-118`、`ios/ShortDrama/Sources/Features/Messages/Views/MessageCenterView.swift:27-35`）

### Backend

- Preview：从系统消息仓储取最新一条，空态返回 204（`backend/src/app/api/messages/preview/route.ts:9-20`）
- System list：匿名可访问，分页 query 由 `MessageListQuerySchema` 校验（`backend/src/app/api/messages/system/route.ts:10-23`）
- Interaction list：`requireAuthContext()` 强制登录，并通过 `getAuth(request)` 提取 `userId`（`backend/src/app/api/messages/interactions/route.ts:11-29`）
- Service：`MessageService` 负责 preview 相对时间格式化、匿名门槛与仓储错误映射（`backend/src/services/message/message.service.ts:17-97`）
- 持久化与 fixture：系统消息可落 Supabase，互动消息固定 mock（`backend/src/repositories/repository-registry.ts:59-69`）

### Web

- Web 用户端不实现菜单消息预览或消息中心页（`docs/specs/2026-07-29-prd-10-signin-messages/spec.md:36-45`）

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/messages/preview` | [../../api/messages.md](../../api/messages.md) | 菜单抽屉使用的最新系统消息摘要 |
| `GET /api/messages/system` | [../../api/messages.md](../../api/messages.md) | 消息中心系统消息分页列表 |
| `GET /api/messages/interactions` | [../../api/messages.md](../../api/messages.md) | 登录用户可见的互动消息分页列表 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Android `MenuPanelUiState.messagePreview` | `MutableStateFlow` | 页面级 | 菜单消息预览状态 | `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt:47-50,63-64` |
| Android `MessageCenterUiState` | `MutableStateFlow` | 页面级 | 系统消息、互动消息、登录门槛与局部错误 | `android/app/src/main/java/com/djs66256/short_drama/feature/messages/viewmodel/MessageCenterViewModel.kt:22-40` |
| Android `showInteractionLoginGate` | 派生自 `AuthStateHolder.authStatus` | 页面级 | 匿名 / restoring / expired 时展示登录门槛 | `android/app/src/main/java/com/djs66256/short_drama/feature/messages/viewmodel/MessageCenterViewModel.kt:60-90` |
| iOS `messagePreviewState` | `@Published` | 页面级 | 菜单预览内容态、空态、错误态 | `ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift:21-24` |
| iOS `systemMessages` / `interactionMessages` | `@Published` | 页面级 | 消息中心双分区状态 | `ios/ShortDrama/Sources/Features/Messages/ViewModels/MessageCenterViewModel.swift:26-27` |
| iOS `loginContext` | 常量 `LoginInterceptionContext` | 页面级 | 互动消息登录门槛唯一回流语义 | `ios/ShortDrama/Sources/Features/Messages/ViewModels/MessageCenterViewModel.swift:29-30` |
| Backend `request.auth` | 中间件注入 | 请求级 | 互动消息 route 的用户身份来源 | `backend/src/middleware/auth.ts:97-138` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 应用壳 | 菜单关闭后导航 | 消息中心必须复用现有 close-menu-then-navigate 机制 |
| 认证体系 | 互动消息登录门槛与回流 | 匿名只能看系统消息，登录后刷新互动消息 |
| 首页信息流 | 菜单入口挂载在首页壳上 | 消息系统的一级入口不在“我的”Tab，而在首页菜单抽屉 |

### 外部依赖

| 服务 / 框架 | 用途 | 接入方式 |
|-------------|------|---------|
| Backend Messages API | 预览、系统消息与互动消息数据源 | Android Retrofit / iOS URLSession 调用 `/api/messages/*` |
| Supabase `system_messages` | 系统消息真实持久化 | Backend system message repository |
| Mock interaction repository | 登录态互动消息 fixture | Backend interaction message repository |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| 互动消息仍是 mock / fixture 数据 | 还没有评论 / 点赞事件驱动的真实通知中台 | 2026-07-29 | PRD-10 首版明确允许 |
| Web 用户端不实现消息系统 | 消息体验仅存在于 Native 端 | 2026-07-29 | 范围外 |
| Preview 与列表没有详情页 | 只能查看摘要列表，不能进入消息详情 | 2026-07-29 | PRD-10 范围外 |
| 设备级黑盒验证仍待补测 | 当前主要基于代码、自动化测试与 QA 文档 | 2026-07-29 | QA 中 10 条设备黑盒用例跳过 |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 初始创建：收录 PRD-10 菜单消息预览、真实消息中心、系统消息匿名可见与互动消息登录门槛 |

---
*本文档由 llm-wiki skill 自动维护。*