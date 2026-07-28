# 实现计划：iOS — PRD-12 剧场频道

> 创建日期：2026-07-28
> 对应技术方案：design-ios.md
> 对应需求：spec.md

## 概述

本期 iOS 端将在现有 `SwiftUI + MVVM + Clean Architecture + NavigationRouter` 架构上，把 `theater` 一级 Tab 从 `PlaceholderTabView` 替换为真实剧场频道页，并接通顶部搜索、识图占位、8 个子频道、4 个快捷入口、双列 Feed、分页加载与播放跳转。计划采用轻量 TDD：先锁定数据契约、状态机和跨 Tab 路由测试，再分层补齐 Domain / Data / Presentation 实现，最后执行 `xcodegen generate`、test、build 与 lint 回归。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 剧场 Feed endpoint 与 DTO 映射正确 | `channel=all&page=1&pageSize=20` 的接口响应，包含 `heat` 与 `pagination` | `DramaRemoteDataSource / DramaRepository` 正确请求 `/api/dramas/channel` 并映射为 Domain 实体 | 单元测试 | P0 |
| T-02 | 首次进入默认加载找剧第一页 | `TheaterViewModel.loadIfNeeded()`，mock 返回 `all` 频道第一页数据 | `selectedChannel == .all`，请求参数为 `page=1&pageSize=20`，页面进入内容态 | 单元测试 | P0 |
| T-03 | 非找剧频道首版展示空态 | 切换到 `real` / `anime` 等频道，mock 返回空数组 | 页面进入空态而非错误态，旧列表被清空，分页回到第一页 | 单元测试 | P0 |
| T-04 | 快速切换频道时旧请求不会覆盖新状态 | `all` 请求延迟返回，随后切到 `anime` 并先返回结果 | 最终页面仅保留最后一次选中频道的数据或空态 | 单元测试 | P0 |
| T-05 | 找剧频道分页成功追加到尾部 | 第一页已成功加载，继续触发 `loadMoreIfNeeded()` | 第二页数据追加到现有列表尾部，页码递增，未到尾页前可继续加载 | 单元测试 | P0 |
| T-06 | 分页失败不清空已有内容 | 第一页成功、第二页失败 | `viewState` 仍保持内容态，仅暴露尾部错误信息，允许后续再次触发分页 | 单元测试 | P0 |
| T-07 | 搜索、快捷入口与识图入口产生正确交互 effect | 点击搜索、筛选、排行、预约、新剧、识图入口 | 分别产出 `.searchHome`、`.classificationHome`、排行默认榜/预约榜上下文、`.newReleases` 与本地占位提示 effect | 单元测试 | P0 |
| T-08 | 点击剧场卡片只在 `id` 有效时跳转播放 | 点击有效 `drama.id` 卡片，或点击空 `id` 卡片 | 有效时产出 `.player(videoId:)` effect；无效时阻止跳转 | 单元测试 | P0 |
| T-09 | 热度文案格式化符合剧场卡片展示约定 | `heat=23000`、`heat=980` 等输入 | 输出 `2.3万`、`980` 等稳定中文短数字文案 | 单元测试 | P1 |
| T-10 | 剧场预约入口能一步直达预约榜 | 从剧场点击“预约”快捷入口，router 注入 booking context | `RankingHomeView / RankingViewModel` 首屏进入 `all + booking`，且上下文只消费一次 | 单元测试 | P0 |

## 实现步骤

### Step 1：先锁定剧场 Feed 数据契约，再补齐 Domain / Data 层接线

- **关联测试**：T-01、T-09
- **目标文件**：`ios/ShortDrama/Sources/Domain/Entities/TheaterChannel.swift`、`ios/ShortDrama/Sources/Domain/Entities/TheaterDrama.swift`、`ios/ShortDrama/Sources/Domain/Entities/TheaterFeedPage.swift`、`ios/ShortDrama/Sources/Domain/Entities/TheaterFeedQuery.swift`、`ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift`、`ios/ShortDrama/Sources/Domain/UseCases/FetchTheaterFeedUseCase.swift`、`ios/ShortDrama/Sources/Data/DTOs/TheaterFeedResponseDTO.swift`、`ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift`、`ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift`、`ios/ShortDrama/Sources/Features/Theater/Models/TheaterHeatFormatter.swift`、`ios/ShortDrama/Tests/DataTests/TheaterFeedDTOTests.swift`、`ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift`、`ios/ShortDrama/Tests/Mocks/MockDramaRepository.swift`
- **实现内容**：
  1. 先在数据层测试中锁定 `GET /api/dramas/channel` 的 path、queryItems、响应结构与 `heat` 原始数值映射规则，避免后续实现偏离 shared design。
  2. 在 Domain 层新增剧场频道、剧场卡片、分页结果与查询实体，并扩展 `DramaRepositoryProtocol` 与 `FetchTheaterFeedUseCase`，保持 `Presentation -> Domain -> Data` 依赖方向清晰。
  3. 在 Data 层新增 theater DTO、endpoint 与 repository 映射，继续复用现有 `APIClient + URLSession`，不引入新的网络抽象或第三方库。
  4. 为卡片热度展示新增轻量 formatter，并用单元测试锁定 `heat` 到中文短数字文案的纯逻辑转换。
  5. 同步扩展 `MockDramaRepository`，为后续 ViewModel 的首屏、空态、分页、乱序等场景准备可控 mock 行为。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-01、T-09 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Domain/Entities/TheaterChannel.swift` | 新增 | 剧场子频道枚举 |
| `ios/ShortDrama/Sources/Domain/Entities/TheaterDrama.swift` | 新增 | 剧场卡片实体 |
| `ios/ShortDrama/Sources/Domain/Entities/TheaterFeedPage.swift` | 新增 | 剧场分页实体 |
| `ios/ShortDrama/Sources/Domain/Entities/TheaterFeedQuery.swift` | 新增 | 剧场查询实体 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 修改 | 扩展剧场 Feed 获取协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchTheaterFeedUseCase.swift` | 新增 | 剧场 Feed 用例 |
| `ios/ShortDrama/Sources/Data/DTOs/TheaterFeedResponseDTO.swift` | 新增 | 剧场接口 DTO 与映射 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 新增 theater endpoint 与请求方法 |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 修改 | 实现剧场 Feed DTO -> Entity 映射 |
| `ios/ShortDrama/Sources/Features/Theater/Models/TheaterHeatFormatter.swift` | 新增 | 热度文案格式化 |
| `ios/ShortDrama/Tests/DataTests/TheaterFeedDTOTests.swift` | 新增 | 覆盖 endpoint 与 DTO 契约 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 修改 | 覆盖 repository 映射 |
| `ios/ShortDrama/Tests/Mocks/MockDramaRepository.swift` | 修改 | 支持剧场 Feed mock 行为 |

### Step 2：先写 TheaterViewModel 状态机测试，再实现首屏、切频道与分页逻辑

- **关联测试**：T-02、T-03、T-04、T-05、T-06
- **目标文件**：`ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift`、`ios/ShortDrama/Sources/Features/Theater/Models/TheaterShortcut.swift`、`ios/ShortDrama/Tests/ViewModelTests/TheaterViewModelTests.swift`、`ios/ShortDrama/Tests/Mocks/MockDramaRepository.swift`
- **实现内容**：
  1. 先在 `TheaterViewModelTests` 中覆盖默认加载 `all` 第一页、非 `all` 频道空态、快速切频道乱序保护、分页追加成功、分页失败保留旧列表等核心状态流转。
  2. 在 `TheaterViewModel` 中建立清晰状态机，至少区分 `loading / content / empty / error / appending`，并显式维护 `selectedChannel`、`currentPage`、`totalPages`、`appendErrorMessage` 与 `requestToken`。
  3. 实现 `loadIfNeeded()`、`selectChannel(_:)`、`loadMoreIfNeeded()`、`retry()`，确保切频道时先清空旧列表并回到第一页，且旧请求不会覆盖新状态。
  4. 将快捷入口定义收敛到 `TheaterShortcut` 本地模型，避免把业务映射散落在 View 组件中。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-02 ～ T-06 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift` | 新增 | 首屏加载、切频道、分页、乱序保护状态机 |
| `ios/ShortDrama/Sources/Features/Theater/Models/TheaterShortcut.swift` | 新增 | 快捷入口模型与静态配置 |
| `ios/ShortDrama/Tests/ViewModelTests/TheaterViewModelTests.swift` | 新增 | 覆盖默认加载、空态、乱序、分页、失败恢复 |
| `ios/ShortDrama/Tests/Mocks/MockDramaRepository.swift` | 修改 | 支持分页成功/失败与延迟响应 |

### Step 3：先补交互 effect 测试，再落地 Theater 页面与 theater Tab 替换

- **关联测试**：T-07、T-08
- **目标文件**：`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`、`ios/ShortDrama/Sources/Features/Theater/Views/TheaterView.swift`、`ios/ShortDrama/Sources/Features/Theater/Views/TheaterChannelTabBar.swift`、`ios/ShortDrama/Sources/Features/Theater/Views/TheaterShortcutGrid.swift`、`ios/ShortDrama/Sources/Features/Theater/Views/TheaterFeedGridView.swift`、`ios/ShortDrama/Sources/Features/Theater/Views/TheaterDramaCardView.swift`、`ios/ShortDrama/Tests/ViewModelTests/TheaterViewModelTests.swift`
- **实现内容**：
  1. 先在 `TheaterViewModelTests` 中补齐点击搜索、筛选、排行、预约、新剧、识图、卡片等交互对应的 route / placeholder effect 断言，确保业务逻辑不依赖手工点击回归。
  2. 将 `TabNavigationHostView` 中 `.theater` 从 `PlaceholderTabView` 替换为 `TheaterView()`，但不新增新的 theater 专属 `NavigationStack`，继续沿用现有壳层结构。
  3. 在 `Features/Theater` 下搭建页面组件：顶部搜索与识图入口、8 个子频道 Tab、4 个快捷入口、双列 Feed、空态/错误态/尾部 loading，与 ViewModel 单向绑定。
  4. 点击卡片统一走 ViewModel / route effect 收口，保证有效 `id` 复用 `.player(videoId:)`，无效 `id` 不产生脏导航。
  5. 识图入口只做本地占位反馈，不增加权限申请、上传逻辑或真实网络请求。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-07、T-08 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 将 theater root 从占位页替换为真实剧场页 |
| `ios/ShortDrama/Sources/Features/Theater/Views/TheaterView.swift` | 新增 | 剧场页根视图 |
| `ios/ShortDrama/Sources/Features/Theater/Views/TheaterChannelTabBar.swift` | 新增 | 8 个子频道横向 Tab |
| `ios/ShortDrama/Sources/Features/Theater/Views/TheaterShortcutGrid.swift` | 新增 | 筛选 / 排行 / 新剧 / 预约快捷入口 |
| `ios/ShortDrama/Sources/Features/Theater/Views/TheaterFeedGridView.swift` | 新增 | 双列 Feed、尾部 loading 与 append error |
| `ios/ShortDrama/Sources/Features/Theater/Views/TheaterDramaCardView.swift` | 新增 | 剧场卡片 UI 与点击承接 |
| `ios/ShortDrama/Tests/ViewModelTests/TheaterViewModelTests.swift` | 修改 | 补交互 effect 与卡片点击测试 |

### Step 4：先锁定跨 Tab 承接测试，再补齐预约榜上下文与现有路由复用

- **关联测试**：T-10
- **目标文件**：`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Sources/Domain/Entities/TheaterRankingEntryContext.swift`、`ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift`、`ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/RankingViewModelTests.swift`
- **实现内容**：
  1. 先补 `NavigationRouterTests` 与 `RankingViewModelTests`，锁定从剧场点击“排行”进入 `all + hot`、点击“预约”进入 `all + booking`，且 context 只消费一次的行为。
  2. 在 Domain 层新增 `TheaterRankingEntryContext`，把剧场进入排行页的初始化语义收敛成单一上下文对象，避免在多个 View 中硬编码榜单切换逻辑。
  3. 扩展 `NavigationRouter`，增加 theater ranking context 的暂存与消费方法，但保持 `.rankingHome`、`.searchHome`、`.classificationHome`、`.newReleases`、`.player(videoId:)` 继续归属 `.home`。
  4. 扩展 `RankingHomeView / RankingViewModel` 的初始化能力，使其能够消费剧场入口上下文，完成预约榜一步直达，而不是要求用户进入排行页后再二次点击。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-10 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 暂存并消费 theater ranking entry context |
| `ios/ShortDrama/Sources/Domain/Entities/TheaterRankingEntryContext.swift` | 新增 | 剧场进入排行页的初始化上下文 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift` | 修改 | 首屏消费剧场入口上下文 |
| `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` | 修改 | 支持 `all + hot` / `all + booking` 初始值注入 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 覆盖剧场快捷入口跨 Tab 导航语义 |
| `ios/ShortDrama/Tests/ViewModelTests/RankingViewModelTests.swift` | 修改 | 覆盖 booking context 初始化与一次性消费 |

### Step 5：执行工程生成与 iOS 全量回归，固化 coding 阶段验收基线

- **关联测试**：T-01 ～ T-10
- **目标文件**：`docs/specs/2026-07-28-prd-12-theater-channel/plan-ios.md`
- **实现内容**：
  1. coding 阶段严格按本计划顺序推进：先补测试，再补实现，所有新增业务逻辑都必须同步补齐 Swift Testing 用例。
  2. 由于会新增 `Features/Theater`、Domain、Data、Tests 多个 Swift 文件，先运行 `xcodegen generate` 让新文件被工程纳入，再执行 test / build / lint 回归。
  3. 回看自动化覆盖，确保剧场页首屏、空态、分页、乱序保护、快捷入口、播放跳转、预约榜上下文等关键行为已由单元测试锁定，而不是仅依赖手工点击验证。
  4. 将 coding 阶段收口标准固定为：theater tab 已替换为真实剧场页、剧场 Feed 契约接通、跨 Tab 承接语义稳定、无新增 lint 错误。
- **验证方式**：
  - 运行 `cd ios && xcodegen generate`
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - 运行 `cd ios && swiftlint lint`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `docs/specs/2026-07-28-prd-12-theater-channel/plan-ios.md` | 新增 | 固化 iOS 剧场频道实现步骤、测试场景与验收基线 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5
```

## 验证总览

- [ ] 新增源码后已执行 `cd ios && xcodegen generate`
- [ ] 所有测试通过（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [ ] Build 成功（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [ ] 无新增 lint 错误（`cd ios && swiftlint lint`）
- [ ] 剧场页默认加载 `channel=all&page=1&pageSize=20` 且空态、错误态、分页态均有单元测试覆盖
- [ ] 搜索 / 筛选 / 排行 / 预约 / 新剧继续复用现有 home-owned route，不新增 theater 内副本页面
- [ ] 预约快捷入口可一步直达 `all + booking`，无需用户二次切换榜单
- [ ] 卡片点击继续复用 `.player(videoId:)`，不引入新的播放路由语义
- [ ] 识图入口仍为本地占位反馈，不引入权限请求或真实识图网络流程

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | theater root 接到真实剧场页 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 增加 theater ranking 上下文暂存与消费 |
| `ios/ShortDrama/Sources/Domain/Entities/TheaterChannel.swift` | 新增 | 剧场频道枚举 |
| `ios/ShortDrama/Sources/Domain/Entities/TheaterDrama.swift` | 新增 | 剧场卡片实体 |
| `ios/ShortDrama/Sources/Domain/Entities/TheaterFeedPage.swift` | 新增 | 剧场分页实体 |
| `ios/ShortDrama/Sources/Domain/Entities/TheaterFeedQuery.swift` | 新增 | 剧场查询实体 |
| `ios/ShortDrama/Sources/Domain/Entities/TheaterRankingEntryContext.swift` | 新增 | 剧场进入排行的初始化上下文 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 修改 | 扩展剧场 Feed 协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchTheaterFeedUseCase.swift` | 新增 | 剧场 Feed 用例 |
| `ios/ShortDrama/Sources/Data/DTOs/TheaterFeedResponseDTO.swift` | 新增 | 剧场接口 DTO |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 新增 `/api/dramas/channel` 接线 |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 修改 | 剧场 Feed repository 映射 |
| `ios/ShortDrama/Sources/Features/Theater/Models/TheaterShortcut.swift` | 新增 | 快捷入口模型 |
| `ios/ShortDrama/Sources/Features/Theater/Models/TheaterHeatFormatter.swift` | 新增 | 热度展示格式化 |
| `ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift` | 新增 | 剧场状态机、分页与交互 effect |
| `ios/ShortDrama/Sources/Features/Theater/Views/TheaterView.swift` | 新增 | 剧场页根视图 |
| `ios/ShortDrama/Sources/Features/Theater/Views/TheaterChannelTabBar.swift` | 新增 | 子频道横向 Tab |
| `ios/ShortDrama/Sources/Features/Theater/Views/TheaterShortcutGrid.swift` | 新增 | 快捷入口区 |
| `ios/ShortDrama/Sources/Features/Theater/Views/TheaterFeedGridView.swift` | 新增 | 双列 Feed 与尾部状态 |
| `ios/ShortDrama/Sources/Features/Theater/Views/TheaterDramaCardView.swift` | 新增 | 剧场卡片视图 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift` | 修改 | 消费剧场入口的排行初始化上下文 |
| `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` | 修改 | 支持预约榜直达初始化 |
| `ios/ShortDrama/Tests/Mocks/MockDramaRepository.swift` | 修改 | 支持剧场 Feed mock 场景 |
| `ios/ShortDrama/Tests/DataTests/TheaterFeedDTOTests.swift` | 新增 | 剧场 endpoint / DTO 契约测试 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 修改 | 覆盖剧场 repository 映射 |
| `ios/ShortDrama/Tests/ViewModelTests/TheaterViewModelTests.swift` | 新增 | 覆盖首屏、空态、分页、effect、播放跳转 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 覆盖剧场快捷入口跨 Tab 承接 |
| `ios/ShortDrama/Tests/ViewModelTests/RankingViewModelTests.swift` | 修改 | 覆盖 booking context 初始化 |
