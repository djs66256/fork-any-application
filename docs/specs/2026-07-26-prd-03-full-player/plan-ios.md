# 实现计划：iOS — PRD-03 完整观看播放器

> 创建日期：2026-07-26
> 对应技术方案：design-ios.md
> 对应需求：spec.md

## 概述

本期 iOS 端将在现有 `SwiftUI + MVVM + Clean Architecture` 基础上，把 `PlayerView` 从 `videoId` 占位页演进为真实完整观看播放器。实现范围覆盖播放器接口接入、匿名续播 session 持久化、剧集与倍速状态机、原生 `AVPlayer` 播放承载、沉浸式页面 UI 与生命周期进度上报；全程保持不新增第三方依赖，并以轻量 TDD 先锁定 network/core/data/domain/viewmodel/view 的主路径与边界行为。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 播放器请求正确注入 `X-Playback-Session-Id` header | progress/start/stop endpoint + 已存在 sessionId | `APIClient` 发出的 request 带正确 header，episodes 接口不注入该 header | 单元测试 | P0 |
| T-02 | 网络层正确解析 backend 错误结构 | `{ error: { code, message } }` 或 4xx/5xx 响应 | `APIClient` 映射为可展示的 `APIError.server` / 等价错误 | 单元测试 | P0 |
| T-03 | 播放器 DTO 与远程数据源正确解码四类接口 | progress、episodes、start、stop 的 snake_case payload | `PlayerRemoteDataSource` 返回正确 DTO / response model | 单元测试 | P0 |
| T-04 | 播放器 Repository / UseCase 串起 bootstrap 数据链路 | progress 有/无历史，episodes 含可播与不可播混合数据 | Domain 层拿到可消费实体，并保留默认集 / 恢复集判定所需字段 | 单元测试 | P0 |
| T-05 | `PlaybackSessionStore` 首次生成并稳定复用匿名 sessionId | Keychain 无值 / 有值两种情况 | 首次生成 UUID 并写入，后续读取返回同一值 | 单元测试 | P0 |
| T-06 | `PlayerViewModel` 首次进入时按 `progress -> episodes -> start` bootstrap | `has_history=false`，episodes 中存在可播集 | 选择第一条可播 Episode，`startPlayback(progress=0)` 被调用，UI 进入 ready/playing | 单元测试 | P0 |
| T-07 | `PlayerViewModel` 恢复历史或回退默认集 | `has_history=true` 且 episode 可播；或历史 episode 已失效 | 可播时恢复对应集与 `start_time`；失效时回退第一条可播 Episode | 单元测试 | P0 |
| T-08 | 切集时先 stop 再 start，并保留当前倍速 | 当前已有播放集、用户切换到新 episode | 先上报旧集进度，再以 `progress=0` 起播新集，并继续应用当前倍速 | 单元测试 | P0 |
| T-09 | 返回页面与切后台触发 best-effort stop，不阻塞导航 | 当前在播放中，触发 back 或 `scenePhase == .background` | stop 被调用，UI 进入 paused/退出流程，router dismiss 正常执行 | 单元测试 | P0 |
| T-10 | `PlayerView` 根据状态渲染 loading/error/no-resource/playable，并隐藏 Tab Bar | `PlayerViewModel.uiState` 分别为各状态 | 页面显示对应状态视图，播放器态隐藏 Tab Bar，选集 sheet / 倍速 dialog 可被触发 | 单元测试 | P1 |
| T-11 | 播放页导航继续复用 `.player(videoId:)` 路由语义 | 首页或 deeplink 导航到 `.player(videoId:)` | 仍进入 Player 页面，旧 `videoId` 命名兼容、业务语义按 `dramaId` 使用 | 单元测试 | P1 |

## 实现步骤

### Step 1：先锁定网络契约测试，再补齐 Core Network header 与错误解析

- **关联测试**：T-01、T-02
- **目标文件**：`ios/ShortDrama/Sources/Core/Network/APIEndpoint.swift`、`ios/ShortDrama/Sources/Core/Network/APIClient.swift`、`ios/ShortDrama/Sources/Core/Network/APIError.swift`、`ios/ShortDrama/Tests/DataTests/APIClientTests.swift`
- **实现内容**：
  1. 先在 `APIClientTests.swift` 中增加播放器接口 request 构建测试，锁定 `progress/start/stop` 必须透传 `X-Playback-Session-Id`，`GET /api/dramas/:id/episodes` 不透传该 header。
  2. 扩展 `APIEndpoint`，新增 `headers` 默认实现，并在 `APIClient` 统一写入 header，保持现有 drama/home 请求兼容不变。
  3. 扩展 `APIClient` 的错误响应解析，兼容 backend `{ error: { code, message } }` 与现有 `{ message }` 结构，避免播放器错误态只剩通用状态码。
  4. 如现有 `APIError` 枚举不足以表达播放器错误映射，则做最小补充，但不引入新的网络库或全局错误层。
- **验证方式**：
  - 先运行项目生成命令，确保新增文件会被 Xcode 工程纳入：
    ```bash
    cd ios && xcodegen generate
    ```
  - 运行测试命令确认 T-01、T-02 通过：
    ```bash
    cd ios && xcodebuild -project ShortDrama.xcodeproj \
      -scheme ShortDrama test \
      -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'
    ```
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Core/Network/APIEndpoint.swift` | 修改 | 新增 `headers` 默认属性 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 修改 | 统一注入 endpoint headers，并增强错误响应解析 |
| `ios/ShortDrama/Sources/Core/Network/APIError.swift` | 复核/轻微修改 | 补足播放器错误映射所需能力 |
| `ios/ShortDrama/Tests/DataTests/APIClientTests.swift` | 修改 | 增加 header 注入与错误结构解析回归测试 |

### Step 2：补齐播放器 data/domain 主链路与匿名 session 存储

- **关联测试**：T-03、T-04、T-05
- **目标文件**：`ios/ShortDrama/Sources/Core/Storage/PlaybackSessionStore.swift`、`ios/ShortDrama/Sources/Data/DTOs/PlayerDTOs.swift`、`ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift`、`ios/ShortDrama/Sources/Data/Repositories/PlayerRepository.swift`、`ios/ShortDrama/Sources/Domain/RepositoryProtocols/PlayerRepositoryProtocol.swift`、`ios/ShortDrama/Sources/Domain/UseCases/FetchPlayerProgressUseCase.swift`、`ios/ShortDrama/Sources/Domain/UseCases/FetchDramaEpisodesUseCase.swift`、`ios/ShortDrama/Sources/Domain/UseCases/StartPlaybackUseCase.swift`、`ios/ShortDrama/Sources/Domain/UseCases/StopPlaybackUseCase.swift`、`ios/ShortDrama/Tests/DataTests/PlayerRemoteDataSourceTests.swift`、`ios/ShortDrama/Tests/DataTests/PlayerRepositoryTests.swift`、`ios/ShortDrama/Tests/DomainTests/PlaybackSessionStoreTests.swift`
- **实现内容**：
  1. 先定义 progress/episodes/start/stop 四类 response/request 的 DTO 与 data source 测试，锁定 snake_case 解码和 endpoint path/query/body 契约。
  2. 新增 `PlaybackSessionStore`，用 Keychain 维护匿名 UUID，并先用单元测试锁定“首次生成、重复读取稳定复用”的行为。
  3. 新增 `PlayerRepositoryProtocol` 与具体 `PlayerRepository`，对外提供 progress、episodes、start、stop 四类能力，避免 ViewModel 直接依赖 data source。
  4. 为 bootstrap 主链路补齐 UseCase，保持 `Presentation -> Domain -> Data` 依赖方向清晰；默认集 / 恢复集的最终判定仍放在 ViewModel，但 Domain 需要把必要原始信息完整暴露出来。
  5. 新增必要的测试 mock / spy，确保后续 ViewModel 测试可精确断言调用顺序与参数。
- **验证方式**：
  - 运行项目生成命令纳入新增源文件：
    ```bash
    cd ios && xcodegen generate
    ```
  - 运行测试命令确认 T-03、T-04、T-05 通过：
    ```bash
    cd ios && xcodebuild -project ShortDrama.xcodeproj \
      -scheme ShortDrama test \
      -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'
    ```
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Core/Storage/PlaybackSessionStore.swift` | 新增 | Keychain 持久化匿名播放 sessionId |
| `ios/ShortDrama/Sources/Data/DTOs/PlayerDTOs.swift` | 新增 | 播放器接口 DTO 与响应模型 |
| `ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift` | 新增 | 封装 progress/episodes/start/stop 远程请求 |
| `ios/ShortDrama/Sources/Data/Repositories/PlayerRepository.swift` | 新增 | PlayerRepositoryProtocol 的 data 层实现 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/PlayerRepositoryProtocol.swift` | 新增 | 播放器领域仓库协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchPlayerProgressUseCase.swift` | 新增 | 查询续播记录 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchDramaEpisodesUseCase.swift` | 新增 | 查询剧集列表 |
| `ios/ShortDrama/Sources/Domain/UseCases/StartPlaybackUseCase.swift` | 新增 | 确认起播请求 |
| `ios/ShortDrama/Sources/Domain/UseCases/StopPlaybackUseCase.swift` | 新增 | 保存退出/切集进度 |
| `ios/ShortDrama/Tests/DataTests/PlayerRemoteDataSourceTests.swift` | 新增 | DTO 解码与 endpoint 契约测试 |
| `ios/ShortDrama/Tests/DataTests/PlayerRepositoryTests.swift` | 新增 | Repository 映射与请求串联测试 |
| `ios/ShortDrama/Tests/DomainTests/PlaybackSessionStoreTests.swift` | 新增 | sessionId 生成与复用测试 |

### Step 3：先写状态机测试，再实现 `PlayerViewModel` 的 bootstrap 与恢复逻辑

- **关联测试**：T-06、T-07
- **目标文件**：`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift`、`ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift`、`ios/ShortDrama/Tests/Mocks/MockPlayerRepository.swift`、`ios/ShortDrama/Tests/Mocks/MockPlaybackSessionStore.swift`
- **实现内容**：
  1. 先在 `PlayerViewModelTests.swift` 中锁定 `loadIfNeeded()` 的主路径：必须按 `progress -> episodes -> start` 顺序执行，且首次无历史时选择第一条可播 Episode。
  2. 增加“恢复历史 episode”与“恢复 episode 失效回退默认集”的测试，确保 ViewModel 而非 View 层负责 bootstrap 决策。
  3. 将当前仅持有 `videoId` 的 `PlayerViewModel` 扩展为明确的播放器状态机，至少覆盖 `idle / bootstrapping / ready / playing / paused / switchingEpisode / noResource / error`。
  4. 继续兼容现有 `videoId` 命名，但在 ViewModel 内统一按 `dramaId` 语义消费，避免下层继续传播错误命名语义。
  5. 为 ViewModel 注入 UseCase / RepositoryProtocol / SessionStore / Router 等依赖，保持可测试性，不把网络或 Keychain 逻辑写入 View 层。
- **验证方式**：
  - 运行测试命令确认 T-06、T-07 通过：
    ```bash
    cd ios && xcodebuild -project ShortDrama.xcodeproj \
      -scheme ShortDrama test \
      -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'
    ```
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | 修改 | 从占位参数对象升级为完整播放器状态机与 bootstrap 编排 |
| `ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift` | 新增 | 主路径、恢复历史、回退默认集等状态机测试 |
| `ios/ShortDrama/Tests/Mocks/MockPlayerRepository.swift` | 新增 | 精确断言 progress/episodes/start/stop 调用顺序与参数 |
| `ios/ShortDrama/Tests/Mocks/MockPlaybackSessionStore.swift` | 新增 | 注入可控 sessionId 的测试替身 |

### Step 4：实现切集、倍速、生命周期与原生播放器适配，并继续用测试锁定副作用顺序

- **关联测试**：T-08、T-09
- **目标文件**：`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift`、`ios/ShortDrama/Sources/Features/Player/Views/Components/NativeVideoPlayerView.swift`、`ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerTopBar.swift`、`ios/ShortDrama/Sources/Features/Player/Views/Components/EpisodePickerSheet.swift`、`ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift`
- **实现内容**：
  1. 先补“切集先 stop 再 start，并保留当前倍速”的测试，再实现 `switchEpisode()`、`selectSpeed()`、`handleBack()`、`handleScenePhaseChange(_:)` 等动作。
  2. 用系统 `AVPlayer` / `VideoPlayer` 增加最小播放器适配层，复用系统原生播放/暂停/进度显示能力，不扩展自定义复杂手势。
  3. 让 ViewModel 在切集后重新应用当前倍速，在进入后台时 best-effort 调用 `stopPlayback` 并把 UI 收敛到 `.paused` 或等价可理解状态。
  4. 对连续切集、重复返回、后台重复 stop 上报等场景做 task cancel / 幂等保护，避免状态机错乱。
  5. 继续保持“stop 失败不阻塞退出”的策略，测试只断言副作用被触发与 dismiss 正常执行，不把退出成功绑定在网络结果上。
- **验证方式**：
  - 运行测试命令确认 T-08、T-09 通过：
    ```bash
    cd ios && xcodebuild -project ShortDrama.xcodeproj \
      -scheme ShortDrama test \
      -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'
    ```
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | 修改 | 增加切集、倍速、返回、前后台生命周期处理 |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/NativeVideoPlayerView.swift` | 新增 | 封装原生 `AVPlayer` / `VideoPlayer` 视图承载 |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerTopBar.swift` | 新增 | 返回、集数文案、倍速、更多入口 |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/EpisodePickerSheet.swift` | 新增 | 剧集面板与当前集高亮、不可播集置灰 |
| `ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift` | 修改 | 新增切集、倍速、后台与返回场景测试 |

### Step 5：组装 Player 页面 UI、导航承载与最终回归验证

- **关联测试**：T-10、T-11
- **目标文件**：`ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift`、`ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerRightActionBar.swift`、`ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerBottomInfoView.swift`、`ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerEpisodeDock.swift`、`ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerStatusView.swift`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`
- **实现内容**：
  1. 将 `PlayerView` 从纯文本占位页改造成基于 `uiState` 分发的页面根视图，覆盖 loading、error、no-resource、playable 四类主状态。
  2. 补齐顶部栏、右侧互动栏、底部信息区、底部选集栏与倍速 dialog / 选集 sheet 的基础 UI 组件，交互事件统一回传 ViewModel。
  3. 在 `PlayerView` 上落地 `.toolbar(.hidden, for: .tabBar)`，确保从 `TabNavigationHostView` push 进入后隐藏 Tab Bar，返回首页时自动恢复。
  4. 复核 `.player(videoId:)` 的现有导航语义，不新增 route case，只在测试中锁定“旧命名兼容、内部按 dramaId 语义消费”。
  5. 执行一次 iOS 全量测试、build 与 lint 回归，确认播放器接入没有破坏已有首页/详情/路由基础能力。
- **验证方式**：
  - 运行项目生成命令：
    ```bash
    cd ios && xcodegen generate
    ```
  - 运行测试命令确认 T-10、T-11 及全量回归通过：
    ```bash
    cd ios && xcodebuild -project ShortDrama.xcodeproj \
      -scheme ShortDrama test \
      -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'
    ```
  - 运行构建命令确认可编译：
    ```bash
    cd ios && xcodebuild -project ShortDrama.xcodeproj \
      -scheme ShortDrama build \
      -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'
    ```
  - 运行 lint 命令确认无新增 lint 错误：
    ```bash
    cd ios && swiftlint lint
    ```
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` | 修改 | 从占位页改为播放器主页面与状态分发容器 |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerRightActionBar.swift` | 新增 | 点赞/收藏/评论/分享入口 UI |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerBottomInfoView.swift` | 新增 | 标题、标签、简介承载 |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerEpisodeDock.swift` | 新增 | 底部固定选集栏 |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerStatusView.swift` | 新增 | loading/error/no-resource 通用状态视图 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 复核/轻微修改 | 保持 `.player(videoId:)` 入口并为 Player 注入完整依赖 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 播放页导航承载与 Tab Bar 隐藏语义回归测试 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5
```

## 验证总览

- [ ] 所有测试通过
  ```bash
  cd ios && xcodebuild -project ShortDrama.xcodeproj \
    -scheme ShortDrama test \
    -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'
  ```
- [ ] Build 成功
  ```bash
  cd ios && xcodebuild -project ShortDrama.xcodeproj \
    -scheme ShortDrama build \
    -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'
  ```
- [ ] 无新增 lint 错误
  ```bash
  cd ios && swiftlint lint
  ```
- [ ] 新增源文件后已执行项目生成
  ```bash
  cd ios && xcodegen generate
  ```
- [ ] 播放器 bootstrap 固定为 `progress -> episodes -> start`
- [ ] `X-Playback-Session-Id` 仅注入 `progress/start/stop` 三类接口
- [ ] 播放页进入后隐藏 Tab Bar，返回时恢复
- [ ] 切集遵循“先 stop 当前集，再 start 新集，且新集从 0 秒开始”
- [ ] 倍速为页面会话级状态，切集后默认沿用

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Core/Network/APIEndpoint.swift` | 修改 | 增加 endpoint headers 能力 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 修改 | request header 注入与播放器错误解析 |
| `ios/ShortDrama/Sources/Core/Network/APIError.swift` | 复核/轻微修改 | 播放器接口错误映射 |
| `ios/ShortDrama/Sources/Core/Storage/PlaybackSessionStore.swift` | 新增 | Keychain 匿名 sessionId 存储 |
| `ios/ShortDrama/Sources/Data/DTOs/PlayerDTOs.swift` | 新增 | progress/episodes/start/stop DTO |
| `ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift` | 新增 | 播放器远程数据源 |
| `ios/ShortDrama/Sources/Data/Repositories/PlayerRepository.swift` | 新增 | PlayerRepositoryProtocol 实现 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/PlayerRepositoryProtocol.swift` | 新增 | 播放器仓库协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchPlayerProgressUseCase.swift` | 新增 | 查询续播记录 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchDramaEpisodesUseCase.swift` | 新增 | 查询剧集列表 |
| `ios/ShortDrama/Sources/Domain/UseCases/StartPlaybackUseCase.swift` | 新增 | 起播确认 |
| `ios/ShortDrama/Sources/Domain/UseCases/StopPlaybackUseCase.swift` | 新增 | 退出/切集进度上报 |
| `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | 修改 | 播放器状态机、bootstrap、切集、倍速、生命周期 |
| `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` | 修改 | 播放器主页面与状态分发 |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/NativeVideoPlayerView.swift` | 新增 | 原生视频播放容器 |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerTopBar.swift` | 新增 | 顶部操作栏 |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerRightActionBar.swift` | 新增 | 右侧互动栏 |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerBottomInfoView.swift` | 新增 | 底部信息区 |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerEpisodeDock.swift` | 新增 | 底部选集栏 |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/EpisodePickerSheet.swift` | 新增 | 选集面板 |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerStatusView.swift` | 新增 | loading/error/no-resource 状态视图 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 复核/轻微修改 | 播放页依赖注入与导航承载 |
| `ios/ShortDrama/Tests/DataTests/APIClientTests.swift` | 修改 | headers 与错误结构测试 |
| `ios/ShortDrama/Tests/DataTests/PlayerRemoteDataSourceTests.swift` | 新增 | data source 契约测试 |
| `ios/ShortDrama/Tests/DataTests/PlayerRepositoryTests.swift` | 新增 | repository 映射与调用测试 |
| `ios/ShortDrama/Tests/DomainTests/PlaybackSessionStoreTests.swift` | 新增 | 匿名 session 存储测试 |
| `ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift` | 新增 | bootstrap、恢复、切集、倍速、生命周期测试 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 播放页路由兼容与导航回归测试 |
| `ios/ShortDrama/Tests/Mocks/MockPlayerRepository.swift` | 新增 | 播放器 ViewModel 测试替身 |
| `ios/ShortDrama/Tests/Mocks/MockPlaybackSessionStore.swift` | 新增 | session store 测试替身 |
