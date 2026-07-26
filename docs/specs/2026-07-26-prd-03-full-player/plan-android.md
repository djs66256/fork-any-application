# 实现计划：Android — PRD-03 完整观看播放器

> 创建日期：2026-07-26
> 对应技术方案：design-android.md
> 对应需求：spec.md

## 概述

Android 端将在现有 Compose + Hilt + Repository 分层上，把 `PlayerScreen` 从占位页演进为真实完整观看播放器，按 shared design 固定执行 `progress -> episodes -> start` bootstrap，并补齐匿名续播、切集、倍速、沉浸式导航与生命周期 stop 上报。由于当前项目约束要求新增开源依赖必须先获得用户明确批准，而 `androidx.media3` 尚未得到真实授权，因此本计划先按“不新增依赖已获批”前提收口：优先完成 network/data/domain/viewmodel/ui/navigation/tests 全链路改造与播放器适配抽象，真实播放内核接入作为 coding 阶段的待确认分支，仅在获得用户批准后再落地到 `android/gradle/libs.versions.toml` 与 `android/app/build.gradle.kts`；以下 Gradle 验证命令均按 `android/CLAUDE.md` 约定，在 `android/` 目录执行。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 播放器网络契约与 DTO 映射正确 | `GET /api/player/progress`、`GET /api/dramas/:id/episodes`、`POST /api/player/start`、`POST /api/player/stop` 的成功/空历史响应 | DataSource 与 DTO 能产出可被 Player 链路消费的强类型结果；`X-Playback-Session-Id` 仅用于 progress/start/stop | 单元测试 | P0 |
| T-02 | 匿名续播 session 首次生成并稳定复用 | 本地无 sessionId 或已有 sessionId | Repository 请求前可拿到稳定 UUID；重复进入同设备时复用同一值 | 单元测试 | P0 |
| T-03 | 无历史时默认播放第一条可播集 | `has_history=false`，episodes 含至少 1 条可播资源 | ViewModel 进入 ready/playing 前态，`currentEpisode` 为第一条可播集，`resumeProgress=0`，并触发 start | 单元测试 | P0 |
| T-04 | 有历史时优先恢复，失效时正确回退 | `has_history=true` 且返回 `episode_id + start_time`；或恢复集缺失/无资源 | 优先恢复目标集并 seek 到恢复点；恢复集失效时回退第一条可播集；全部无资源时进入 no-resource | 单元测试 | P0 |
| T-05 | 切集时先 stop 当前集再 start 新集，并沿用当前倍速 | 当前已有播放集，用户点击新剧集，当前倍速非 1.0x | 先 best-effort stop 当前集，再对新集 start(progress=0)；`currentSpeed` 在会话内保持不变 | 单元测试 | P0 |
| T-06 | 生命周期事件会触发暂停与进度上报 | 播放中收到后台/页面销毁事件 | ViewModel 将状态切到 paused 或 exiting，并对当前集执行 best-effort stop | 单元测试 | P1 |
| T-07 | UI 状态可覆盖 loading/error/no-resource/ready 与倍速/选集交互 | `PlayerUiState` 分别处于 bootstrapping、error、noResource、ready，且用户触发打开/关闭面板、点赞/收藏 | `PlayerScreen` 渲染正确区域，sheet 可开关，互动状态仅影响页面内 UI，不破坏播放主链路 | 单元测试 | P1 |
| T-08 | canonical/legacy 播放路由都进入同一页面并隐藏底部栏 | 当前 destination 为 `play/{videoId}` 或 `player/{videoId}` | 两条 route 都落到同一 `PlayerScreen`，全局 `NavigationBar` 隐藏，返回首页后恢复显示 | 单元测试 | P1 |
| T-09 | 未获批真实播放器依赖时仍能保持 UI 与状态机可验证 | 未引入 `androidx.media3` | 通过 `NativePlayerAdapter` fake/placeholder 完成状态联调，计划明确记录“真实播放待用户批准” | 单元测试 | P1 |

## 实现步骤

### Step 1：先收口播放器 API 契约与 DTO 映射

- **关联测试**：T-01
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/dto/*.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/datasource/PlayerRemoteDataSource.kt`
- **实现内容**：
  1. 先补播放器接口契约与 DTO 映射测试，固定 progress/episodes/start/stop 的字段语义，以及 `X-Playback-Session-Id` 只出现在 progress/start/stop 三个接口。
  2. 将 `ApiService.kt` 中播放器相关 `Map<String, String>/Unit` 占位定义改为强类型请求/响应；补齐 `getPlaybackProgress`、`getDramaEpisodes`、`startPlayback`、`stopPlayback` 的 Retrofit 签名。
  3. 新增 `PlayerRemoteDataSource`，沿用现有 `ApiResult` 模式包装播放器请求，为后续 repository/usecase 提供稳定入口。
  4. 此步骤不改动 Gradle 依赖，只先锁定网络 contract 与可测试的数据边界。
- **验证方式**：
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.data.datasource.PlayerRemoteDataSourceTest"` 确认 T-01 通过
  - 运行 `./gradlew assembleDebug` 确认网络层签名可编译
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 将播放器接口升级为强类型 Retrofit 定义 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/*.kt` | 新增 | 新增 player progress/start/stop/episode list DTO 与映射 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/PlayerRemoteDataSource.kt` | 新增 | 封装播放器网络请求与 `ApiResult` 转换 |
| `android/app/src/test/java/com/djs66256/short_drama/data/datasource/PlayerRemoteDataSourceTest.kt` | 新增 | 固定播放器网络契约与 header 透传规则 |

### Step 2：补齐 data/domain 播放器数据链路与匿名 session

- **关联测试**：T-02
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/di/NetworkModule.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/storage/PlaybackSessionStore.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/repository/PlayerRepositoryImpl.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/repository/PlayerRepository.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/*.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/usecase/*.kt`
- **实现内容**：
  1. 先写 repository/session 相关单测，固定“首次生成 UUID、后续稳定复用、请求前按接口规则附带 header”的行为。
  2. 新增 `PlaybackSessionStore`（基于 DataStore 封装）与 Player 专属 domain model，抽象 progress、bootstrap 结果、start/stop 请求所需领域对象。
  3. 新增 `PlayerRepository` / `PlayerRepositoryImpl`，组合 `PlayerRemoteDataSource` 与 `PlaybackSessionStore`，统一处理 sessionId 获取、DTO -> Domain 映射与播放器请求编排。
  4. 新增 `GetPlaybackProgressUseCase`、`GetDramaEpisodesUseCase`、`StartPlaybackUseCase`、`StopPlaybackUseCase` 等 use case，并在 Hilt module 中完成依赖绑定。
- **验证方式**：
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.data.repository.PlayerRepositoryImplTest"` 确认 T-02 通过
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.domain.usecase.StartPlaybackUseCaseTest"` 回归 domain/data 链路
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/PlaybackSessionStore.kt` | 新增 | 封装匿名 playback session 的读取/生成逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/PlayerRepositoryImpl.kt` | 新增 | 实现播放器 repository 与 session/header 编排 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/PlayerRepository.kt` | 新增 | 定义播放器领域接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/*.kt` | 新增 | 新增播放器链路所需领域模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/*.kt` | 新增 | 提供 progress/episodes/start/stop 用例 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 绑定 `PlayerRepository` 与 `PlaybackSessionStore` |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/NetworkModule.kt` | 校验/轻微修改 | 保持 PlayerRemoteDataSource 可被 Hilt 注入 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/PlayerRepositoryImplTest.kt` | 新增 | 覆盖 session 复用与 repository 编排 |
| `android/app/src/test/java/com/djs66256/short_drama/domain/usecase/*.kt` | 新增 | 覆盖播放器 use case 委托行为 |

### Step 3：先写状态机测试，再重构 PlayerViewModel bootstrap 主链路

- **关联测试**：T-03、T-04、T-05
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerUiState.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModelTest.kt`
- **实现内容**：
  1. 先重写 `PlayerViewModelTest.kt`，用 MockK + Turbine 固定无历史默认集、历史恢复、恢复集失效回退、全部无资源、切集 stop->start、倍速会话保持等关键状态流转。
  2. 将当前只读取 `videoId` 的 `PlayerViewModel` 升级为完整状态机：读取 `SavedStateHandle` 后把 route 参数统一解释为 `dramaId`，执行 `progress -> episodes -> start` bootstrap。
  3. 定义 `PlayerUiState` / `PlayerScreenState` / `PlaybackSpeed` 等状态模型，显式建模 `idle`、`bootstrapping`、`ready`、`playing`、`paused`、`switchingEpisode`、`noResource`、`error`。
  4. 实现 `loadIfNeeded()`、`retry()`、`switchEpisode()`、`selectSpeed()`、`toggleLike()`、`toggleFavorite()` 等动作，保证切集时新集固定 `progress=0`，且当前倍速在本次会话内沿用。
- **验证方式**：
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.feature.player.viewmodel.PlayerViewModelTest"` 确认 T-03、T-04、T-05 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` | 修改 | 从占位 ViewModel 升级为完整播放器状态机 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerUiState.kt` | 新增 | 拆分播放器 UI 状态与动作建模 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModelTest.kt` | 修改 | 补齐 bootstrap、回退、切集、倍速核心单测 |

### Step 4：落地播放器适配抽象与播放器 UI 结构

- **关联测试**：T-05、T-07、T-09
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/components/*.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/player/NativePlayerAdapter.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/player/PlaceholderPlayerHost.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/player/PlayerEventAdapter.kt`
- **实现内容**：
  1. 先补 UI 状态相关单测，固定 loading/error/no-resource/ready 四类页面态与倍速、选集、点赞/收藏交互的状态开关，不把 UI 正确性完全交给手工点击验证。
  2. 新增 `NativePlayerAdapter` 或等价抽象，承接真实视频加载、播放/暂停、seek、倍速应用与释放逻辑；在未获批 `androidx.media3` 前，先提供 placeholder/fake host 以打通状态与 UI 结构。
  3. 将 `PlayerScreen.kt` 从“Video ID 占位页”改为真实播放器页面，拆出顶部栏、右侧互动栏、底部信息区、选集 dock、倍速面板、选集面板与错误/无资源态组件。
  4. UI 层只消费 `PlayerUiState` 与 ViewModel action，不在 Composable 内重复实现业务判断；播放器事件通过适配层或 callback 回传给 ViewModel，同步当前进度与播放状态。
  5. 若后续获得用户批准，再在此步骤下追加 Media3 实现文件与 Gradle 依赖，不改变上层 ViewModel/UI contract。
- **验证方式**：
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.feature.player.ui.PlayerScreenStateTest"` 确认 T-07、T-09 通过
  - 运行 `./gradlew assembleDebug` 确认适配层与 Compose UI 可编译
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt` | 修改 | 渲染真实播放器页面与状态内容 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/components/*.kt` | 新增 | 拆分顶部栏、互动栏、选集栏、speed/episode sheet 等组件 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/player/NativePlayerAdapter.kt` | 新增 | 封装播放器引擎抽象接口 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/player/PlaceholderPlayerHost.kt` | 新增 | 未获批真实依赖前的占位宿主 / fake 实现 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/player/PlayerEventAdapter.kt` | 新增 | 统一播放器事件到 ViewModel 的回调边界 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/player/ui/PlayerScreenStateTest.kt` | 新增 | 覆盖播放器主要 UI 状态与交互开关 |

### Step 5：补齐沉浸式导航与生命周期 stop 上报

- **关联测试**：T-06、T-08
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt`
- **实现内容**：
  1. 先补 route/bottom bar 策略测试，固定 `play/{videoId}` 与 `player/{videoId}` 都进入同一 `PlayerScreen`，且命中播放器路由时隐藏全局 `NavigationBar`。
  2. 在 `NavGraph.kt` 中新增或抽出 `shouldShowBottomBar(destination)` 判定，确保进入播放器后沉浸式隐藏底部栏，返回 home graph 后恢复。
  3. 在 `PlayerScreen` 与 `PlayerViewModel` 间接入生命周期事件，处理进入后台、页面销毁、返回上一页时的 `stopPlayback` best-effort 上报，不阻塞导航退出。
  4. 保持 `play/{videoId}` canonical 与 `player/{videoId}` legacy alias 的兼容，不改动对外 deeplink 语义，只在内部统一解释为同一 `dramaId` 播放流程。
- **验证方式**：
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.navigation.NavGraphTest"` 确认 T-08 通过
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.feature.player.viewmodel.PlayerViewModelTest"` 回归 T-06
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 命中播放器 route 时隐藏底部导航并保持 alias 兼容 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 校验/轻微修改 | 保持 canonical/legacy route 常量与参数语义一致 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt` | 修改 | 接入生命周期与返回事件 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt` | 新增 | 覆盖 route 映射与 bottom bar 显隐策略 |

### Step 6：回归测试、构建验证与待批准依赖分支收口

- **关联测试**：T-01 ～ T-09
- **目标文件**：`android/app/src/test/java/...`、`android/app/src/main/java/.../feature/player/...`、`android/app/src/main/java/.../navigation/...`、`android/gradle/libs.versions.toml`、`android/app/build.gradle.kts`
- **实现内容**：
  1. 回看所有新增测试是否覆盖 network/data/domain/viewmodel/ui/navigation 六个面向，确保播放器首版主链路与关键回退路径都有自动化断言。
  2. 统一清理命名与文件归档，避免把 `videoId`、`dramaId`、`episodeId` 语义混写在不同层级中；对未纳入本期的投屏/字幕/下载/持久化互动能力保持明确边界。
  3. 执行完整测试、构建与 detekt 回归，记录 coding 阶段需要继续关注的已知限制，例如 UI 测试以 JVM 单测 + 编译回归为主、stop 上报仍为 best-effort、不做离线补偿队列。
  4. 若此时已获得用户对 `androidx.media3` 的明确批准，则在本步骤追加：更新 `android/gradle/libs.versions.toml`、`android/app/build.gradle.kts`，并以 `Media3PlayerHost` 实现替换 placeholder adapter；若仍未获批，则保持占位实现并在交付说明中明确 Android 真播能力尚未闭环。
- **验证方式**：
  - 运行 `./gradlew test`
  - 运行 `./gradlew assembleDebug`
  - 运行 `./gradlew detekt`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/test/java/...` | 修改/按需新增 | 补齐播放器全链路回归测试 |
| `android/app/src/main/java/.../feature/player/...` | 校验/轻微修改 | 收口命名、状态与边界处理 |
| `android/app/src/main/java/.../navigation/...` | 校验/轻微修改 | 确认沉浸式导航回归稳定 |
| `android/gradle/libs.versions.toml` | 条件修改 | 仅在获批 `androidx.media3` 后新增版本与库坐标 |
| `android/app/build.gradle.kts` | 条件修改 | 仅在获批 `androidx.media3` 后引入播放器依赖 |

## 依赖关系

```
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5 ──▶ Step 6
```

## 验证总览

- [ ] 所有测试通过（`./gradlew test`）
- [ ] Build 成功（`./gradlew assembleDebug`）
- [ ] 无新增 lint 错误（`./gradlew detekt`）
- [ ] 播放器 bootstrap 固定为 `progress -> episodes -> start`
- [ ] `X-Playback-Session-Id` 仅用于 `progress/start/stop` 三类接口
- [ ] 播放页进入后隐藏底部导航，返回时恢复
- [ ] 切集遵循“先 stop 当前集，再 start 新集，且新集从 0 秒开始”
- [ ] 倍速为页面会话级状态，切集后默认沿用
- [ ] 若未获批 `androidx.media3`，计划与实现明确保留真实播放待确认边界；若已获批，则完成真实播放器内核接入

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 收口播放器 Retrofit 契约 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/*.kt` | 新增 | 新增播放器 DTO 与映射 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/PlayerRemoteDataSource.kt` | 新增 | 封装播放器远端请求 |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/PlaybackSessionStore.kt` | 新增 | 匿名 playback session 持久化 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/PlayerRepositoryImpl.kt` | 新增 | 实现播放器 repository |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/PlayerRepository.kt` | 新增 | 定义播放器领域接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/*.kt` | 新增 | 播放器领域模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/*.kt` | 新增 | 播放器 use case |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 绑定播放器 repository/store |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/NetworkModule.kt` | 校验/轻微修改 | 保持播放器网络依赖注入稳定 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` | 修改 | 实现播放器状态机与主链路编排 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerUiState.kt` | 新增 | 播放器 UI 状态建模 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt` | 修改 | 落地真实播放器页面 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/components/*.kt` | 新增 | 顶部栏、互动栏、选集栏、sheet 组件 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/player/NativePlayerAdapter.kt` | 新增 | 播放器引擎抽象层 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/player/PlaceholderPlayerHost.kt` | 新增 | 未获批真实依赖前的占位播放器宿主 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/player/PlayerEventAdapter.kt` | 新增 | 播放器事件适配层 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 隐藏底部栏并统一播放器 route 行为 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 校验/轻微修改 | 保持播放器 route 常量与语义一致 |
| `android/app/src/test/java/com/djs66256/short_drama/data/datasource/PlayerRemoteDataSourceTest.kt` | 新增 | 覆盖网络契约与 header 规则 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/PlayerRepositoryImplTest.kt` | 新增 | 覆盖 repository/session 编排 |
| `android/app/src/test/java/com/djs66256/short_drama/domain/usecase/*.kt` | 新增 | 覆盖播放器 use case |
| `android/app/src/test/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModelTest.kt` | 修改 | 覆盖 bootstrap/回退/切集/生命周期 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/player/ui/PlayerScreenStateTest.kt` | 新增 | 覆盖主要 UI 状态 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt` | 新增 | 覆盖路由与底部栏显隐 |
| `android/gradle/libs.versions.toml` | 条件修改 | 仅在获批 `androidx.media3` 后新增版本与库坐标 |
| `android/app/build.gradle.kts` | 条件修改 | 仅在获批 `androidx.media3` 后引入播放器相关依赖 |
