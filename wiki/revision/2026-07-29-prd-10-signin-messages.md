# 2026-07-29 — PRD-10 签到与消息系统 wiki 收录

> 触发来源：PRD-10 签到与消息系统

## wiki/api/check-ins.md
- **变更类型**：新建
- **变更章节**：概述 / `GET /api/check-ins/status` / `POST /api/check-ins` / 数据结构补充 / 与客户端实现的关系 / 修订历史
- **变更摘要**：新增签到 API 文档，收录首页签到浮层的状态查询、当日签到提交、`X-Installation-Id` 匿名主体、`server_date` 业务日权威值、同日幂等与第 8 天重开规则。
- **主要来源**：`backend/src/app/api/check-ins/status/route.ts`、`backend/src/app/api/check-ins/route.ts`、`backend/src/app/api/check-ins/parse-installation-id.ts`、`backend/src/services/check-in/check-in.service.ts`、`backend/src/lib/schemas.ts`、`backend/supabase/migrations/20260729001000_create_signin_and_system_messages.sql`、`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt`、`ios/ShortDrama/Sources/Data/DataSources/CheckInRemoteDataSource.swift`

## wiki/api/messages.md
- **变更类型**：新建
- **变更章节**：概述 / `GET /api/messages/preview` / `GET /api/messages/system` / `GET /api/messages/interactions` / 数据结构补充 / 与客户端实现的关系 / 修订历史
- **变更摘要**：新增消息 API 文档，收录菜单消息预览、系统消息列表、互动消息列表的 contract，明确 preview 空态为 `204 No Content`、system 匿名可读、interactions 强制登录，以及双列表分页结构。
- **主要来源**：`backend/src/app/api/messages/preview/route.ts`、`backend/src/app/api/messages/system/route.ts`、`backend/src/app/api/messages/interactions/route.ts`、`backend/src/services/message/message.service.ts`、`backend/src/lib/schemas.ts`、`backend/src/repositories/repository-registry.ts`、`android/app/src/main/java/com/djs66256/short_drama/data/datasource/MessageRemoteDataSource.kt`、`ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift`、`ios/ShortDrama/Sources/Features/Messages/Views/MessageCenterView.swift`

## wiki/api/index.md
- **变更类型**：更新
- **变更章节**：API 文档索引
- **变更摘要**：新增 Check-Ins API 与 Messages API 入口，并补充各自已覆盖的 contract 范围。

## wiki/features/check-in/index.md
- **变更类型**：新建
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：新增签到能力功能文档，收录首页冷启动签到浮层、7 日签到板、账号优先 / installationId 兜底、服务端业务日、本地关闭态、评论 / 登录模态互斥与多端实现现状。
- **主要来源**：`android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/CheckInPopup.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/storage/CheckInLocalStore.kt`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`、`ios/ShortDrama/Sources/Core/Storage/InstallationIdStore.swift`、`backend/src/services/check-in/check-in.service.ts`

## wiki/features/messages/index.md
- **变更类型**：新建
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：新增消息系统功能文档，收录菜单消息预览、独立消息中心、系统消息与互动消息双分区、匿名 / 登录分流、消息页登录回流，以及 Android / iOS 的 close-menu-then-navigate 语义。
- **主要来源**：`android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/messages/viewmodel/MessageCenterViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift`、`ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift`、`ios/ShortDrama/Sources/Features/Messages/ViewModels/MessageCenterViewModel.swift`、`ios/ShortDrama/Sources/Features/Messages/Views/MessageCenterView.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`backend/src/services/message/message.service.ts`

## wiki/features/index.md
- **变更类型**：更新
- **变更章节**：功能域索引
- **变更摘要**：新增“签到能力 (Check-In)”与“消息系统 (Messages)”入口，并把应用壳 / 认证 / 首页信息流摘要同步到 PRD-10 语义。

## wiki/features/auth/index.md
- **变更类型**：更新
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：补充 PRD-10 的新认证边界：消息互动分区强制登录、消息页登录回流、签到接口可选登录 + installationId 兜底，以及对应 Android/iOS 拦截上下文与 Backend helper 复用。
- **主要来源**：`backend/src/app/api/messages/interactions/route.ts`、`backend/src/app/api/check-ins/status/route.ts`、`backend/src/app/api/check-ins/route.ts`、`backend/src/services/check-in/check-in.service.ts`、`android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`ios/ShortDrama/Sources/Features/Messages/ViewModels/MessageCenterViewModel.swift`、`ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift`、`ios/ShortDrama/Sources/Data/DataSources/CheckInRemoteDataSource.swift`

## wiki/features/app-shell/index.md
- **变更类型**：更新
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：把应用壳从“菜单 + 登录承载”继续扩展到“首页签到浮层 + 菜单消息预览 + 真实消息中心”，补充首页 overlay 宿主、`menu/messages` / `.messages` route、先关菜单再导航时序，以及消息页登录回流。
- **主要来源**：`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt`、`ios/ShortDrama/Sources/App/AppRoute.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`、`ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift`、`ios/ShortDrama/Sources/Features/Messages/Views/MessageCenterView.swift`

## wiki/features/homepage-feed/index.md
- **变更类型**：更新
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：补充首页首屏之后的签到评估流程，明确首页 Feed 与签到浮层的时序关系、服务端业务日、本地关闭态以及与评论 / 登录模态的互斥规则。
- **主要来源**：`android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`、`backend/src/app/api/check-ins/status/route.ts`、`backend/src/services/check-in/check-in.service.ts`

## wiki/architecture/overview.md
- **变更类型**：更新
- **变更章节**：概述 / 架构设计 / 核心流程调用栈 / 设计决策 / 跨端涉及 / 技术栈总览 / 已知限制 / 修订历史
- **变更摘要**：把系统总览从“发现 + 登录 + 评论”继续同步到 PRD-10“签到 + 消息”，补充 check-ins/messages API、首页签到浮层、菜单消息预览、消息中心、installationId 主体策略、preview 204 空态与 mixed repository 结构。
- **主要来源**：`backend/src/app/api/check-ins/**`、`backend/src/app/api/messages/**`、`backend/src/services/check-in/check-in.service.ts`、`backend/src/services/message/message.service.ts`、`backend/src/repositories/repository-registry.ts`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/**`、`android/app/src/main/java/com/djs66256/short_drama/feature/menu/**`、`android/app/src/main/java/com/djs66256/short_drama/feature/messages/**`、`ios/ShortDrama/Sources/Features/Home/**`、`ios/ShortDrama/Sources/Features/MenuPanel/**`、`ios/ShortDrama/Sources/Features/Messages/**`
