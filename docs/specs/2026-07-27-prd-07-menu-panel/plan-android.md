# 实现计划：Android — PRD-07 菜单面板

> 创建日期：2026-07-28
> 对应技术方案：design-android.md
> 对应需求：spec.md

## 概述

Android 端将在现有单 Activity + Navigation Compose + Hilt + ViewModel + Repository 架构上，把菜单面板落到 `NavGraph` 外层 `Scaffold` 的壳层 Overlay 中，复用现有 `PlaybackSessionStore`、播放器路由和 `PlaceholderScreen`，补齐“首页汉堡入口 → 抽屉开合 → 最近在看 → 占位承接”的完整链路。整个计划遵循轻量 TDD：先用 JVM 单测锁定导航状态机、recently-viewed 数据契约和 ViewModel 状态流，再接 Compose 壳层与页面集成；不新增第三方依赖，只涉及 `android/` 目录相关实现。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> Android 端核心逻辑优先放在 `android/app/src/test/` 下的纯 JVM 单测中。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 菜单壳层状态机正确处理打开、关闭与 closing 防重入 | 依次调用 `openMenu()`、`closeMenuThenNavigate(PendingRoute.MenuLogin)`、closing 中再次触发菜单入口 | 状态按 `CLOSED -> OPENING -> OPEN -> CLOSING -> CLOSED` 迁移；closing 中只保留首个待导航目标 | 单元测试 | P0 |
| T-02 | 菜单相关 route helper 与首页菜单入口常量保持稳定 | 调用 `AppDestination.menuLogin()`、`menuMessages()`、`menuBooking()`、`menuDownloads()`；读取首页菜单按钮 content description | 输出 canonical route；首页菜单按钮文案稳定可回归 | 单元测试 | P1 |
| T-03 | recently-viewed API 契约正确透传播放会话 header | 调用 `ApiService.getRecentlyViewed(playbackSessionId)`，返回包含 `cover_url=null` 的响应 | 路径为 `GET player/recently-viewed`，header 名为 `X-Playback-Session-Id`，DTO 可正常解析空封面 | 单元测试 | P0 |
| T-04 | 菜单仓储复用独立数据链路并正确映射列表/空态/错误 | `MenuPanelRemoteDataSource` 返回 3 条、0 条、异常三类结果 | `MenuPanelRepository` 正确映射 `RecentlyViewed`；空数组进入空态；错误透传；不把只读聚合混入 `PlayerRepository` | 单元测试 | P0 |
| T-05 | 菜单 ViewModel 首次打开时先取 session 再加载最近在看 | 首次 `loadIfNeeded()`，`PlaybackSessionStore.getOrCreateSessionId()` 成功，use case 返回 2 条数据 | 状态按 `idle -> loading -> success` 迁移；仅首次打开发起请求；保留 `hasLoaded` 标记 | 单元测试 | P0 |
| T-06 | 菜单 ViewModel 正确处理 empty / error / retry / 非法点击 | use case 分别返回空数组、异常；点击重试；点击 `dramaId=""` 的最近在看卡片 | 最近在看区进入 empty 或 error；重试后恢复；非法 `dramaId` 不触发导航事件 | 单元测试 | P0 |
| T-07 | 菜单静态区块顺序与动作模型稳定 | 构造登录、消息、游戏中心、常用功能静态项 | 区块顺序固定；游戏入口为反馈型动作；登录/消息/预约/下载映射到对应 placeholder route | 单元测试 | P1 |
| T-08 | 壳层集成满足“先关抽屉再导航”和返回关闭语义 | 抽屉打开后点击登录入口、最近在看卡片、蒙层、系统返回 | 登录/消息/预约/下载与最近在看都在关闭动画完成后再导航；蒙层/返回只关闭抽屉不误跳页面 | 单元测试 | P0 |

## 实现步骤

### Step 1：先锁定菜单壳层状态机与路由契约

- **关联测试**：T-01、T-02
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/home/ui/HomeScreenTest.kt`
- **实现内容**：
  1. 先补 `MainNavigationViewModelTest`，把 `MenuPanelPresentationState`、`pendingMenuRoute`、`closeMenuThenNavigate()`、`onMenuOpened()`、`onMenuClosedAnimationFinished()` 的状态迁移和 closing 防重入规则固定下来。
  2. 在 `MainNavigationViewModel.kt` 中新增菜单开合状态、待消费菜单导航目标与关闭动画完成回调，避免沿用现有“立即导航”的 `pendingRoute` 逻辑直接跳转。
  3. 在 `AppDestination.kt` 中新增 `menu/login`、`menu/messages`、`menu/booking`、`menu/downloads` 的 canonical route helper，并为菜单承接目标补充 `PendingRoute` 分支。
  4. 在 `HomeScreen.kt` 中把顶部栏扩展为“菜单按钮 + 标题 + 搜索按钮”，新增 `onOpenMenu` 回调与可回归的菜单入口 content description 常量。
  5. 先把状态机和 route helper 用测试锁住，再进入数据链路和 Compose 壳层实现，避免后续 UI 集成时临时改导航语义。
- **验证方式**：
  - 运行候选命令 `cd android && ./gradlew test --tests "com.djs66256.short_drama.navigation.MainNavigationViewModelTest"`
  - 运行候选命令 `cd android && ./gradlew test --tests "com.djs66256.short_drama.navigation.RoutesTest"`
  - 运行候选命令 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.home.ui.HomeScreenTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt` | 修改 | 新增菜单开关状态、关闭后导航与防重入逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 新增菜单占位 route helper 与菜单相关 `PendingRoute` |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 首页顶部栏增加汉堡菜单入口与回调 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt` | 修改 | 覆盖菜单状态机与 closing 防重入 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 回归菜单 route 构造 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/home/ui/HomeScreenTest.kt` | 修改 | 回归菜单入口文案与顶部栏契约 |

### Step 2：先补 recently-viewed API 与仓储测试，再接 Android 数据链路

- **关联测试**：T-03、T-04
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/dto/RecentlyViewedResponseDto.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/datasource/MenuPanelRemoteDataSource.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/RecentlyViewed.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/repository/MenuPanelRepository.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/repository/MenuPanelRepositoryImpl.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt`、`android/app/src/test/java/com/djs66256/short_drama/core/network/ApiServiceTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/data/datasource/MenuPanelRemoteDataSourceTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/data/repository/MenuPanelRepositoryImplTest.kt`
- **实现内容**：
  1. 先补 `ApiServiceTest`，锁定 `GET player/recently-viewed` 与 `X-Playback-Session-Id` header 名称，确保契约与 shared design 一致。
  2. 新增 `RecentlyViewedResponseDto`、`RecentlyViewedItemDto` 和 `RecentlyViewed` domain model，覆盖 `drama_id`、`title`、`cover_url`、`episode_number`、`progress`、`updated_at` 的映射。
  3. 新增 `MenuPanelRemoteDataSource.kt`，按现有 `PlayerRemoteDataSource` 风格封装接口调用与 `ApiResult` 返回，保持异常处理方式一致。
  4. 新增独立 `MenuPanelRepository` / `MenuPanelRepositoryImpl`，把菜单的只读聚合链路与播放器进度读写职责分开，避免把 recently-viewed 混入 `PlayerRepositoryImpl`。
  5. 在 `RepositoryModule.kt` 中补齐菜单仓储注入，为后续 `GetRecentlyViewedUseCase` 和 `MenuPanelViewModel` 接线。
- **验证方式**：
  - 运行候选命令 `cd android && ./gradlew test --tests "com.djs66256.short_drama.core.network.ApiServiceTest"`
  - 运行候选命令 `cd android && ./gradlew test --tests "com.djs66256.short_drama.data.datasource.MenuPanelRemoteDataSourceTest"`
  - 运行候选命令 `cd android && ./gradlew test --tests "com.djs66256.short_drama.data.repository.MenuPanelRepositoryImplTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增 recently-viewed Retrofit 接口定义 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/RecentlyViewedResponseDto.kt` | 新增 | 对齐后端 recently-viewed 响应 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/MenuPanelRemoteDataSource.kt` | 新增 | 封装 recently-viewed 请求与异常处理 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/RecentlyViewed.kt` | 新增 | 菜单最近在看领域模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/MenuPanelRepository.kt` | 新增 | 菜单面板仓储接口 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/MenuPanelRepositoryImpl.kt` | 新增 | recently-viewed DTO 到 Domain 映射 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 注入 `MenuPanelRepository` |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/ApiServiceTest.kt` | 修改 | 补 recently-viewed header/path 断言 |
| `android/app/src/test/java/com/djs66256/short_drama/data/datasource/MenuPanelRemoteDataSourceTest.kt` | 新增 | 覆盖成功、异常与 header 透传 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/MenuPanelRepositoryImplTest.kt` | 新增 | 覆盖映射、空列表与错误透传 |

### Step 3：先写菜单 ViewModel 状态流测试，再实现 session 驱动的最近在看状态机

- **关联测试**：T-05、T-06
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetRecentlyViewedUseCase.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModelTest.kt`
- **实现内容**：
  1. 先补 `MenuPanelViewModelTest`，覆盖首次打开加载成功、空态、错误态、重试恢复、重复 `loadIfNeeded()` 不重复发请求、非法 `dramaId` 不触发导航等行为。
  2. 新增 `GetRecentlyViewedUseCase.kt`，把菜单读取动作收口到 Domain 层，保持与现有 use case 分层一致。
  3. 在 `MenuPanelViewModel.kt` 中注入 `PlaybackSessionStore` 与 `GetRecentlyViewedUseCase`，严格按“先 `getOrCreateSessionId()`，再请求 recently-viewed”执行。
  4. 使用 `StateFlow` 建模 `isLoading`、`items`、`errorMessage`、`hasLoaded`、`isRetrying` 等字段，并用单一 in-flight 控制避免重复重试。
  5. 为点击最近在看卡片提供纯状态/事件出口，只在 `dramaId` 非空时向壳层抛出播放目标，真正导航放到壳层集成步骤处理。
- **验证方式**：
  - 运行候选命令 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.menu.viewmodel.MenuPanelViewModelTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetRecentlyViewedUseCase.kt` | 新增 | 菜单最近在看查询 use case |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt` | 新增 | 管理 loading/empty/error/retry 状态机 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModelTest.kt` | 新增 | 覆盖 session 初始化、成功、空态、错误与重试 |

### Step 4：先固定静态区块动作模型，再实现菜单面板 Compose 页面（已完成）

- **关联测试**：T-07
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntries.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/components/MenuPanelComponents.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntriesTest.kt`
- **实现内容**：
  1. 先补 `MenuPanelStaticEntriesTest`，把登录引导、消息预览、最近在看、游戏中心、常用功能的展示顺序和动作类型固定下来。
  2. 新增 `MenuPanelStaticEntries.kt` 或等价纯 Kotlin 模型文件，收敛静态入口定义，确保游戏入口只产生“即将上线”反馈动作，登录/消息/预约/下载映射为明确的 placeholder 导航动作。
  3. 实现 `MenuPanelScreen.kt` 与菜单组件文件，拆分登录头部、消息区、最近在看区、游戏区、常用功能区，使静态区块与动态 recently-viewed 区块解耦。
  4. 页面层只消费 ViewModel 状态和静态动作模型，不在 Composable 内直接创建路由字符串，避免 UI 层散落导航语义。
  5. 复用已有 `PlaceholderScreen` 作为后续承接页容器，游戏中心继续停留在当前页面并通过壳层反馈“即将上线”。
- **验证方式**：
  - 运行候选命令 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.menu.model.MenuPanelStaticEntriesTest"`
  - 运行候选命令 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.menu.viewmodel.MenuPanelViewModelTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntries.kt` | 新增 | 收敛静态菜单区块、顺序与动作模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelScreen.kt` | 新增 | 菜单面板内容根页面 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/components/MenuPanelComponents.kt` | 新增 | 登录、消息、最近在看、游戏、常用功能组件 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntriesTest.kt` | 新增 | 回归区块顺序与动作类型 |

### Step 5：把菜单接入 NavGraph 壳层并做 Android 端总回归（代码已完成，验证受环境阻断）

- **关联测试**：T-01、T-02、T-05、T-06、T-08
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelDrawer.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt`
- **实现内容**：
  1. 先补 `NavGraphTest` 与 `MainNavigationViewModelTest` 的回归 case，锁定“抽屉打开时返回优先关闭菜单”“点击菜单入口先关闭抽屉再导航”“蒙层点击只关闭不跳转”的壳层语义。
  2. 新增 `MenuPanelDrawer.kt`，在 `NavGraph.kt` 的外层 `Scaffold` 内容区叠加抽屉 Surface 和蒙层，确保覆盖首页内容区与底部 Tab 的交互层。
  3. 在 `NavGraph.kt` 中注册菜单占位 routes，复用 `PlaceholderScreen(title, description)` 承接登录、消息、我的预约、我的下载，不修改其它频道路由语义。
  4. 把 `HomeScreen(onOpenMenu = ...)`、`MenuPanelViewModel`、`MainNavigationViewModel` 串起来，落实 `BackHandler`、`closeMenuThenNavigate()`、`onMenuClosedAnimationFinished()` 和点击最近在看后复用 `AppDestination.play(dramaId)` 的导航时序。
  5. 收口 Android 端验证，优先使用仓库已存在的真实 Gradle 命令；若本地 SDK/环境差异导致单项测试命令需微调，以 `android/gradlew` 实际脚本行为为准。
- **验证方式**：
  - 运行候选命令 `cd android && ./gradlew test --tests "com.djs66256.short_drama.navigation.NavGraphTest"`
  - 运行 `cd android && ./gradlew test`
  - 运行 `cd android && ./gradlew assembleDebug`
  - 运行 `cd android && ./gradlew detekt`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 在壳层 `Scaffold` 叠加菜单抽屉并注册 placeholder routes |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelDrawer.kt` | 新增 | 实现抽屉容器、蒙层、动画完成回调 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 接入菜单打开回调 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt` | 修改 | 回归抽屉关闭、菜单路由与底部栏遮罩语义 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt` | 修改 | 收口关闭后导航与防重入回归测试 |

## 依赖关系

```text
Step 1（菜单状态机与 route）
  └──▶ Step 2（recently-viewed 数据链路）
        └──▶ Step 3（MenuPanelViewModel 状态机）
              └──▶ Step 4（菜单静态区块与 Compose 页面）
                    └──▶ Step 5（NavGraph 壳层集成与总回归）
```

## 验证总览

- [x] 菜单状态机、静态动作模型、壳层 helper 与 recently-viewed 收口逻辑已补齐对应源码与 JVM 回归用例（涉及 `MainNavigationViewModelTest`、`MenuPanelStaticEntriesTest`、`NavGraphTest`、`MenuPanelViewModelTest`）
- [x] `cd android && ./gradlew app:testDebugUnitTest --tests "com.djs66256.short_drama.navigation.MainNavigationViewModelTest" --tests "com.djs66256.short_drama.navigation.RoutesTest" --tests "com.djs66256.short_drama.feature.home.ui.HomeScreenTest" --tests "com.djs66256.short_drama.core.network.ApiServiceTest" --tests "com.djs66256.short_drama.data.datasource.MenuPanelRemoteDataSourceTest" --tests "com.djs66256.short_drama.data.repository.MenuPanelRepositoryImplTest" --tests "com.djs66256.short_drama.feature.menu.viewmodel.MenuPanelViewModelTest" --tests "com.djs66256.short_drama.feature.menu.model.MenuPanelStaticEntriesTest" --tests "com.djs66256.short_drama.navigation.NavGraphTest"`
  - 已使用 Android Studio 自带 JBR 补跑通过。
- [ ] `cd android && ./gradlew test`
  - 已实际执行；失败原因为仓库内既有的无关用例失败：`ClassificationViewModelTest > T-03 switching gender resets dimension and emits scroll to first dimension`、`SearchHomeViewModelTest > T-02 submit history and quick entry emit navigation events`。本期菜单面板相关定向测试已通过。
- [x] `cd android && ./gradlew assembleDebug`
  - 已使用 Android Studio 自带 JBR 执行通过。
- [x] `cd android && ./gradlew detekt`
  - 已使用 Android Studio 自带 JBR 执行通过，`0 code smells`。

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt` | 修改 | 菜单壳层状态机、关闭后导航与防重入 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 菜单 placeholder route helper 与导航目标 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 抽屉 Overlay、BackHandler、菜单 routes 注册 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 首页汉堡菜单入口与事件回调 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelDrawer.kt` | 新增 | 菜单抽屉容器与动画回调 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelScreen.kt` | 新增 | 菜单内容页面 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/components/MenuPanelComponents.kt` | 新增 | 登录、消息、最近在看、游戏、常用功能区块 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntries.kt` | 新增 | 菜单静态区块与动作模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt` | 新增 | 菜单最近在看状态机 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/RecentlyViewed.kt` | 新增 | 最近在看领域模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/MenuPanelRepository.kt` | 新增 | 菜单面板仓储接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetRecentlyViewedUseCase.kt` | 新增 | 最近在看查询 use case |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/RecentlyViewedResponseDto.kt` | 新增 | recently-viewed DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/MenuPanelRemoteDataSource.kt` | 新增 | recently-viewed 远端数据源 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/MenuPanelRepositoryImpl.kt` | 新增 | 菜单仓储实现 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增 `GET player/recently-viewed` 接口 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 注入菜单仓储 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt` | 修改 | 回归菜单状态机 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 回归菜单 route helper |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt` | 修改 | 回归壳层关闭与导航时序 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/home/ui/HomeScreenTest.kt` | 修改 | 回归首页菜单入口 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModelTest.kt` | 新增 | 覆盖最近在看状态机 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntriesTest.kt` | 新增 | 覆盖静态区块顺序与动作 |
| `android/app/src/test/java/com/djs66256/short_drama/data/datasource/MenuPanelRemoteDataSourceTest.kt` | 新增 | 覆盖 recently-viewed 请求链路 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/MenuPanelRepositoryImplTest.kt` | 新增 | 覆盖 DTO 映射与错误透传 |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/ApiServiceTest.kt` | 修改 | 回归 recently-viewed path/header 注解 |
