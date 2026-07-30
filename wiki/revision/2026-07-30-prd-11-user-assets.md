# 2026-07-30 — PRD-11 个人资产管理 wiki 收录

> 触发来源：PRD-11 个人资产管理

## wiki/features/user-assets/index.md
- **变更类型**：新建
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：新增个人资产管理主功能文档，收录菜单“我的预约”真实页、`GET /api/users/me/bookings`、匿名登录承接与 booking route 回流、双 Tab `online/upcoming` + summary，以及“我的下载”继续占位与 Web skipped 边界。
- **主要来源**：`backend/src/app/api/users/me/bookings/route.ts`、`backend/src/lib/schemas.ts`、`backend/src/services/drama/drama.service.ts`、`backend/src/repositories/supabase/drama.supabase.repository.ts`、`backend/src/app/api/__tests__/users-me-bookings.test.ts`、`android/app/src/main/java/com/djs66256/short_drama/feature/booking/**`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntries.kt`、`ios/ShortDrama/Sources/Features/BookingAssets/**`、`ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`

## wiki/api/user-assets.md
- **变更类型**：新建
- **变更章节**：概述 / `GET /api/users/me/bookings` / 与其它接口的关系 / 修订历史
- **变更摘要**：新增用户资产 API 文档，收录当前用户预约资产读取接口的鉴权方式、query 默认值、`{ data, pagination, summary }` contract、状态映射、脏数据过滤、超大页码空列表与错误码语义。
- **主要来源**：`backend/src/app/api/users/me/bookings/route.ts`、`backend/src/lib/schemas.ts`、`backend/src/services/drama/drama.service.ts`、`backend/src/repositories/supabase/drama.supabase.repository.ts`、`backend/src/app/api/__tests__/users-me-bookings.test.ts`

## wiki/features/index.md
- **变更类型**：更新
- **变更章节**：功能域索引
- **变更摘要**：新增“个人资产管理 (User Assets)”入口，并把 Auth 摘要同步到预约资产登录承接语义。

## wiki/api/index.md
- **变更类型**：更新
- **变更章节**：API 文档索引
- **变更摘要**：新增“个人资产 (User Assets)” API 入口，标记已覆盖 `GET /api/users/me/bookings` contract。

## wiki/features/auth/index.md
- **变更类型**：更新
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 修订历史
- **变更摘要**：补充预约资产页登录承接、Android `returnRoute=menu/booking`、iOS `.bookingAssets` 登录上下文与 `completeLogin()` 回流逻辑，并把 booking assets 接口纳入统一鉴权基线。
- **主要来源**：`backend/src/app/api/users/me/bookings/route.ts`、`android/app/src/main/java/com/djs66256/short_drama/feature/booking/viewmodel/BookingAssetsViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt`、`ios/ShortDrama/Sources/Features/BookingAssets/Views/BookingAssetsView.swift`、`ios/ShortDrama/Sources/Features/BookingAssets/BookingAssetsRouteBuilder.swift`、`ios/ShortDrama/Sources/App/AppShellView.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`

## wiki/features/app-shell/index.md
- **变更类型**：更新
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：把应用壳口径同步到 PRD-11：菜单“我的预约”已切换为真实 booking 页面，`menu/booking` / `.bookingAssets` 已成为真实 route，“我的下载”继续保持占位，应用壳负责 close-menu-then-navigate 与登录回流承载。
- **主要来源**：`android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntries.kt`、`ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift`、`ios/ShortDrama/Sources/App/AppRoute.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`

## wiki/architecture/overview.md
- **变更类型**：更新
- **变更章节**：概述 / 架构设计 / 当前首页发现、签到、消息、预约资产、商城、赚钱与账号承载结构 / 当前认证、签到、消息、预约资产、评论、商城与赚钱能力分层现状 / 核心流程调用栈 / 设计决策 / 跨端涉及 / 技术栈总览 / 已知限制 / 修订历史
- **变更摘要**：将系统总览同步到 PRD-11 口径，补充 `GET /api/users/me/bookings`、移动端 booking 页面承载、匿名登录承接回 booking route、服务端 summary 聚合，以及 Web skipped / 下载占位的系统级边界。
- **主要来源**：`backend/src/app/api/users/me/bookings/route.ts`、`backend/src/repositories/supabase/drama.supabase.repository.ts`、`android/app/src/main/java/com/djs66256/short_drama/feature/booking/**`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`ios/ShortDrama/Sources/Features/BookingAssets/**`、`ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`docs/specs/2026-07-30-prd-11-user-assets/design-web.md`
