# 2026-07-28 PRD-07 menu-panel wiki 修订记录

## `wiki/features/index.md`
- 变更类型：增量更新
- 变更章节：功能域索引
- 变更摘要：将“应用壳”索引说明补充为首页频道承载菜单抽屉；将“播放器”索引说明补充为菜单最近在看入口，明确 PRD-07 变更已分别归档到应用壳与播放器能力域。
- 主要来源：
  - `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelScreen.kt`
  - `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`
  - `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift`

## `wiki/features/app-shell/index.md`
- 变更类型：增量更新
- 变更章节：功能概述、入口与路由、核心逻辑、多端实现、API 引用、状态管理、依赖关系、已知限制、修订历史
- 变更摘要：补充首页左上角汉堡菜单触发的左侧抽屉式菜单面板不是新一级 tab，而是由 Android `NavGraph + MainNavigationViewModel` 与 iOS `AppShellView + NavigationRouter` 统一承载的首页 overlay；记录菜单关闭后再导航、菜单占位承接页、本地“即将上线”反馈，以及菜单最近在看复用播放器历史接口的事实。
- 主要来源：
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelDrawer.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelScreen.kt`
  - `ios/ShortDrama/Sources/App/AppShellView.swift`
  - `ios/ShortDrama/Sources/App/NavigationRouter.swift`
  - `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift`
  - `ios/ShortDrama/Sources/App/TabNavigationHostView.swift`

## `wiki/features/video-player/index.md`
- 变更类型：增量更新
- 变更章节：功能概述、入口与路由、核心逻辑、边界与异常处理、多端实现、API 引用、状态管理、依赖关系、已知限制、修订历史
- 变更摘要：补充菜单“最近在看”已成为 Android / iOS 播放器新增入口之一，记录菜单卡片关闭抽屉后跳转播放页、`GET /api/player/recently-viewed` 的统一复用方式、固定候选窗口过滤脏数据且最多返回 3 条的服务端语义，以及 Web 不涉及菜单入口的范围边界。
- 主要来源：
  - `backend/src/app/api/player/recently-viewed/route.ts`
  - `backend/src/services/player/player.service.ts`
  - `backend/src/lib/player.ts`
  - `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt`
  - `ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift`
  - `ios/ShortDrama/Sources/App/NavigationRouter.swift`
  - `ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift`

## `wiki/api/player.md`
- 变更类型：增量更新
- 变更章节：`GET /api/player/recently-viewed`、参数变更记录、修订历史
- 变更摘要：新增 `GET /api/player/recently-viewed` 文档，明确其统一复用 `X-Playback-Session-Id`，先取固定候选窗口再过滤脏数据，最终最多返回 3 条、允许不足 3 条且不承诺继续向更老历史补足，并补充当前成功响应与错误码语义。
- 主要来源：
  - `backend/src/app/api/player/recently-viewed/route.ts`
  - `backend/src/app/api/player/parse-playback-session-id.ts`
  - `backend/src/services/player/player.service.ts`
  - `backend/src/lib/player.ts`
  - `backend/src/lib/schemas.ts`
  - `backend/src/app/api/__tests__/player.recently-viewed.test.ts`
  - `backend/src/services/player/player.service.test.ts`

## `wiki/architecture/overview.md`
- 变更类型：增量更新
- 变更章节：概述、整体架构、当前首页与发现链路承载结构、核心流程调用栈、设计决策、跨端涉及、技术栈总览、已知限制、修订历史
- 变更摘要：将 PRD-07 纳入系统总览，补充首页菜单面板属于移动端应用壳 overlay、Backend 已实现 `GET /api/player/recently-viewed`、菜单最近在看到播放器的跨端调用链、Web 不在本期范围，以及菜单占位承接页和最近在看返回语义带来的系统级边界。
- 主要来源：
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt`
  - `ios/ShortDrama/Sources/App/AppShellView.swift`
  - `ios/ShortDrama/Sources/App/NavigationRouter.swift`
  - `ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift`
  - `backend/src/app/api/player/recently-viewed/route.ts`
  - `backend/src/services/player/player.service.ts`
  - `docs/specs/2026-07-27-prd-07-menu-panel/qa-test.md`

---
*本文档由 llm-wiki skill 自动维护。*
