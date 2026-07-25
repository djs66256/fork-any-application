# 实现计划：Android — PRD-01 底部导航与应用路由

> 创建日期：2026-07-25
> 对应技术方案：design-android.md
> 对应需求：spec.md

## 概述

本期 Android 端将在现有单 Activity + 单 `NavGraph` 骨架上，扩展为 `NavigationBar + 顶层嵌套导航图 + 多 back stack` 的应用壳，并完成 canonical `play` 路由、`player` 兼容、`detail` / `dramaDetail` 映射、冷启动 deeplink 待执行与一级频道占位页复用。实现遵循纯 JVM 单元测试优先策略，核心导航契约通过 JUnit4 + MockK + Turbine 保障。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | canonical 播放路由生成正确 | `videoId = "abc123"` | `AppDestination.play("abc123") == "play/abc123"` | 单元测试 | P0 |
| T-02 | legacy `player` deeplink 可兼容归一 | `djsdrama://player/abc123` | 解析结果归一到 canonical `PendingRoute.Play("abc123")` | 单元测试 | P0 |
| T-03 | 非法 deeplink 可安全降级 | 未知 host、空参数、非 `djsdrama` scheme | 返回 `null` 或 home fallback，不产生崩溃导航 | 单元测试 | P0 |
| T-04 | 冷启动 pendingRoute 在根容器 ready 后被消费 | `pendingRoute = Play("123")` | 触发一次导航后清空 pendingRoute | 单元测试 | P0 |
| T-05 | `PlayerViewModel` / `DramaDetailViewModel` 参数兼容 | `SavedStateHandle(videoId/dramaId/id)` | 优先读取专用 key，兼容通用 `id` | 单元测试 | P0 |
| T-06 | 顶层 Tab 切换使用多 back stack 策略 | 首页进入子页面后切到商城再切回首页 | 首页栈仍保留原子页面上下文 | 手工回归 | P0 |
| T-07 | 4 个一级频道占位页复用单一实现 | theater / mall / earn / profile | 展示不同标题说明，但共用同一占位组件 | 单元测试 | P1 |

## 实现步骤

### Step 1：建立 Android 导航契约与 deeplink 解析层

- **关联测试**：T-01、T-02、T-03
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/DeeplinkRouteParserTest.kt`
- **实现内容**：
  1. 新增 `AppDestination`，集中维护顶层图路由、5 个 Tab、canonical `play/{videoId}`、alias `player/{videoId}`、canonical `detail/{dramaId}`、alias `dramaDetail/{dramaId}` 及参数 key。
  2. 新增 `DeeplinkRouteParser`，统一把 `djsdrama://open`、`play/{id}`、`player/{id}`、`drama/{id}` 解析为受控 `PendingRoute`。
  3. 收紧解析规则，拦截未知 host、空参数、非 `djsdrama` scheme。
  4. 先补 `RoutesTest` 与 `DeeplinkRouteParserTest`，验证 canonical/alias 构建与 deeplink 归一逻辑，再落地实现。
- **验证方式**：
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.navigation.RoutesTest"`
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.navigation.DeeplinkRouteParserTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 新增 | 集中定义 Tab、图、route pattern、参数 key |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt` | 新增 | 解析 deeplink 并归一到 canonical 目标 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 从旧 `player` 路由断言扩展到 canonical/alias 断言 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/DeeplinkRouteParserTest.kt` | 新增 | 覆盖合法 / 非法 deeplink 解析与降级 |

### Step 2：引入 pendingRoute 状态与根级导航容器

- **关联测试**：T-04
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt`
- **实现内容**：
  1. 新增 `MainNavigationViewModel`，用 `StateFlow` 保存 `pendingRoute` 与拒绝原因。
  2. 修改 `MainActivity`，在 `onCreate(intent)` / `onNewIntent(intent)` 中调用 parser，将 deeplink 先入队，而不是直接访问 `NavController`。
  3. 补充 ViewModel 测试，验证 `enqueuePendingRoute()`、`consumePendingRoute()`、`rejectPendingRoute()` 的状态流转。
  4. 为后续 `NavHost` ready 后消费 pendingRoute 留出单一入口。
- **验证方式**：
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.navigation.MainNavigationViewModelTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt` | 新增 | 管理 pending deeplink 与拒绝原因 |
| `android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt` | 修改 | 把 deeplink 解析接入 Activity 生命周期 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt` | 新增 | 覆盖 pendingRoute 状态流转 |

### Step 3：重构 NavGraph 为底部 5 Tab + 多 back stack

- **关联测试**：T-04、T-06
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt`
- **实现内容**：
  1. 将现有单一 `NavGraph` 扩展为 `Scaffold + NavigationBar + NavHost` 根壳层。
  2. 按 `home_graph / theater_graph / mall_graph / earn_graph / profile_graph` 拆分顶层嵌套图，并把 `play` / `player` / `detail` / `dramaDetail` 统一放在 `home_graph` 下。
  3. 底部 Tab 切换统一使用 `saveState + restoreState + popUpTo(findStartDestination())` 的官方多 back stack 策略。
  4. `LegacyRouteForwarder` 负责将 `player/{videoId}`、`dramaDetail/{dramaId}` 立即重定向到 canonical route，避免重复栈。
  5. 在 `LaunchedEffect(uiState.pendingRoute)` 中消费待执行 deeplink，成功后清空 pending，失败则回首页。
- **验证方式**：
  - 运行 `./gradlew assembleDebug`
  - 运行 `./gradlew test`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 接入根壳层、顶层嵌套图、多 back stack 与 pendingRoute 消费 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 补齐 top-level graph 与 alias route 辅助函数 |
| `android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt` | 修改 | 连接 `MainNavigationViewModel` 与 Compose 根容器 |

### Step 4：补齐首页入口、占位页复用与参数兼容回归

- **关联测试**：T-05、T-07
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/common/ui/PlaceholderScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/dramadetail/viewmodel/DramaDetailViewModel.kt`、对应测试文件
- **实现内容**：
  1. 新增 `PlaceholderScreen`，统一承载剧场 / 商城 / 赚钱 / 我的 4 个同构一级频道。
  2. 在 `HomeScreen` 中新增进入播放页 / 详情页的示例入口回调。
  3. 修改 `PlayerViewModel` 与 `DramaDetailViewModel`，兼容 `SavedStateHandle(videoId/dramaId/id)` 读取策略。
  4. 补齐 ViewModel 测试，确保 canonical route 与 alias route 都能把参数安全传递到页面层。
- **验证方式**：
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.feature.player.viewmodel.PlayerViewModelTest"`
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.feature.dramadetail.viewmodel.DramaDetailViewModelTest"`
  - 运行 `./gradlew test`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/common/ui/PlaceholderScreen.kt` | 新增 | 4 个一级频道的复用占位页面 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 增加播放页 / 详情页入口 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` | 修改 | 兼容 `videoId` 与 `id` |
| `android/app/src/main/java/com/djs66256/short_drama/feature/dramadetail/viewmodel/DramaDetailViewModel.kt` | 修改 | 兼容 `dramaId` 与 `id` |
| `android/app/src/test/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModelTest.kt` | 修改 | 增加参数兼容测试 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/dramadetail/viewmodel/DramaDetailViewModelTest.kt` | 新增 | 增加参数兼容测试 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4
```

## 验证总览

- [ ] 所有测试通过（`./gradlew test`）
- [ ] Build 成功（`./gradlew assembleDebug`）
- [ ] 无新增 lint 错误（`./gradlew detekt`）
- [ ] 手工验证首页进入子页面后切换 Tab 再返回仍能保持首页子栈

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 新增 | 集中维护导航契约、top-level tab 与 route builder |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt` | 新增 | deeplink 解析与 canonical 归一 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt` | 新增 | pending deeplink 状态管理 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/common/ui/PlaceholderScreen.kt` | 新增 | 一级频道占位页复用组件 |
| `android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt` | 修改 | 接入 deeplink 生命周期与根壳层 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 底部导航、多 back stack、alias 转 canonical |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 首页新增播放页 / 详情页入口 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` | 修改 | 参数兼容读取 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/dramadetail/viewmodel/DramaDetailViewModel.kt` | 修改 | 参数兼容读取 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | canonical / alias route 断言 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/DeeplinkRouteParserTest.kt` | 新增 | deeplink 解析与降级测试 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt` | 新增 | pendingRoute 状态流转测试 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModelTest.kt` | 修改 | 参数兼容测试 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/dramadetail/viewmodel/DramaDetailViewModelTest.kt` | 新增 | 参数兼容测试 |