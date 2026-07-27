# PRD-07 menu-panel wiki 收录说明

## 收录内容

本次最小必要 wiki 收录覆盖以下文件：

- `wiki/features/index.md`
- `wiki/features/app-shell/index.md`
- `wiki/features/video-player/index.md`
- `wiki/api/player.md`
- `wiki/architecture/overview.md`
- `wiki/revision/2026-07-28-prd-07-menu-panel.md`

## 变更点

- `wiki/features/index.md`
  - 将菜单面板归入“应用壳”能力域。
  - 将菜单“最近在看”归入“播放器”入口说明。
- `wiki/features/app-shell/index.md`
  - 明确菜单面板不是新一级 tab，而是首页左上角汉堡菜单触发的左侧抽屉式 overlay。
  - 记录 Android 由 `NavGraph + MainNavigationViewModel`、iOS 由 `AppShellView + NavigationRouter` 承载菜单状态与关闭后导航。
  - 补充登录/消息/预约/下载为先关菜单再导航，游戏中心仅本地“即将上线”反馈。
- `wiki/features/video-player/index.md`
  - 补充菜单“最近在看”是播放器新增入口之一。
  - 记录 Android / iOS 都复用 `GET /api/player/recently-viewed`，点击卡片后进入既有播放页路由。
  - 明确最近在看最多返回 3 条、允许不足 3 条、不承诺 offset 补足。
- `wiki/api/player.md`
  - 新增 `GET /api/player/recently-viewed` 接口文档。
  - 明确其统一复用 `X-Playback-Session-Id`，并记录固定候选窗口、过滤脏数据、最多 3 条的当前行为。
- `wiki/architecture/overview.md`
  - 将 PRD-07 菜单面板纳入系统总览、跨端承载结构、核心流程与限制说明。
  - 明确 Web 不涉及本期菜单面板，Backend 已新增 `GET /api/player/recently-viewed`。
- `wiki/revision/2026-07-28-prd-07-menu-panel.md`
  - 按文件记录本次 wiki 增量更新的章节、摘要与主要代码来源。

## 验证方式

本次收录以代码为准，主要依据以下实现与验证材料：

- Android
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt`
  - `android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt`
- iOS
  - `ios/ShortDrama/Sources/App/AppShellView.swift`
  - `ios/ShortDrama/Sources/App/NavigationRouter.swift`
  - `ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift`
  - `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`
- Backend
  - `backend/src/app/api/player/recently-viewed/route.ts`
  - `backend/src/app/api/player/parse-playback-session-id.ts`
  - `backend/src/services/player/player.service.ts`
  - `backend/src/app/api/__tests__/player.recently-viewed.test.ts`
  - `backend/src/services/player/player.service.test.ts`
- QA 辅助材料
  - `docs/specs/2026-07-27-prd-07-menu-panel/qa-test.md`

## 限制

- 本次仅增量更新用户允许范围内的 wiki / docs 文件，未扩展到其他 feature 文档。
- 收录内容只记录代码中已落地事实，不包含设计稿或未来规划。
- Web 端菜单面板不在本次收录范围内，因为当前代码未落地对应实现。
- QA 文档显示本轮仍未完成真实设备或模拟器黑盒点击验证，当前结论主要来自代码与自动化测试。
