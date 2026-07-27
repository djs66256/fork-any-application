# 实现计划：iOS — PRD-05 排行体系

> 创建日期：2026-07-27
> 对应技术方案：design-ios.md
> 对应需求：spec.md

## 概述

本期 iOS 端将在现有 `NavigationStack + NavigationRouter + MVVM + Clean Architecture` 基础上，把 `rankingHome` 从搜索发现页占位承接替换为真实排行页，并接通双层 Tab、列表分页、预约操作与播放页跳转。计划采用轻量 TDD：优先补齐 `ViewModel / Data / Router` 单元测试，再按测试驱动补齐 Domain、Data 与 Presentation 层实现，最后使用现有 iOS 工程命令完成工程生成、测试、构建与 lint 回归。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 默认进入排行页加载“全部 + 热榜”第一页 | 首次进入 `rankingHome`，接口返回第 1 页榜单数据 | `RankingViewModel` 以 `contentType=all&type=hot&page=1&pageSize=10` 发起请求，并进入内容态 | 单元测试 | P0 |
| T-02 | 一级 Tab 切换保留当前二级 Tab | 当前为 `all + recommend`，点击“AI” | 发起 `contentType=ai&type=recommend&page=1` 请求，旧列表清空并回到第一页 | 单元测试 | P0 |
| T-03 | 二级 Tab 切换保留当前一级 Tab | 当前为 `live_action + hot`，点击“预约榜” | 发起 `contentType=live_action&type=booking&page=1` 请求，旧列表清空并回到第一页 | 单元测试 | P0 |
| T-04 | 快速连续切换 Tab 仅消费最后一次结果 | 两次不同维度请求乱序返回 | 页面仅展示最后一次选择对应的数据，旧响应不得覆盖新状态 | 单元测试 | P0 |
| T-05 | 首屏失败后可通过重试恢复 | 第一次请求报错，第二次重试成功 | 页面先进入错误态，点击重试后恢复到内容态或空态 | 单元测试 | P0 |
| T-06 | 分页加载成功追加到当前列表尾部 | 第 1 页已加载，继续请求第 2 页成功 | 新数据追加到尾部，页码递增，维度选择保持不变 | 单元测试 | P0 |
| T-07 | 分页失败不清空已加载内容且可恢复 | 第 2 页请求失败，随后再次触底成功 | 已有列表保留，展示尾部错误，再次触发加载后恢复追加 | 单元测试 | P0 |
| T-08 | 已登录用户预约成功后局部更新 | 当前处于预约榜，点击未预约项，接口返回 `booked=true` 和新 `booking_count` | 当前项按钮更新为“已预约”，预约数同步更新，不重复刷新整页 | 单元测试 | P0 |
| T-09 | 未登录用户预约被拦截且不发起预约请求 | 当前处于预约榜，登录态为未登录，点击预约 | 不调用 `POST /api/dramas/:id/book`，而是抛出登录拦截 effect / 回调 | 单元测试 | P0 |
| T-10 | 点击排行项继续复用现有播放路由 | 列表项 `drama.id` 有效，点击卡片主区域 | 生成 `.player(videoId:)` 路由并进入现有 `play` 语义链路 | 单元测试 | P1 |
| T-11 | `djsdrama://ranking` 仍解析到现有排行承接路由 | Deep Link 为 `djsdrama://ranking` | 仍解析到 `.rankingHome`，不新增新的顶级排行路由 | 单元测试 | P1 |

## 实现步骤

### Step 1：先锁定排行承接页替换与导航回归

- **关联测试**：T-10、T-11
- **目标文件**：`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`、`ios/ShortDrama/Sources/Features/Search/Views/DiscoveryPlaceholderView.swift`、`ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift`、`ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/DeeplinkHandlerTests.swift`
- **实现内容**：
  1. 先在 `NavigationRouterTests`、`DeeplinkHandlerTests` 中补齐排行页点击播放与 `djsdrama://ranking` 承接的回归断言，先把现有路由语义锁死。
  2. 将 `TabNavigationHostView` 中 `.rankingHome` 的页面注册从 `DiscoveryPlaceholderView(kind: .ranking)` 切换为真实 `RankingHomeView`，但不改动 `.rankingHome` 的 route 名称和归属 tab。
  3. 在 `Features/Ranking` 下搭建最小页面骨架与 `RankingRouteBuilder`，保证后续列表点击统一收口到 `.player(videoId:)`，避免播放跳转逻辑散落在 View 中。
  4. 调整 `DiscoveryPlaceholderView`，让 `ranking` 不再承担真实页面职责，其余分类 / 新剧 / 演员入口继续保持现状。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-10、T-11 对应回归通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 将 `.rankingHome` 接到真实排行页 |
| `ios/ShortDrama/Sources/Features/Search/Views/DiscoveryPlaceholderView.swift` | 修改 | 移除 `ranking` 的真实承接职责 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift` | 新增 | 排行页根视图骨架 |
| `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift` | 新增 | 排行项到 `.player(videoId:)` 的统一路由构建 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 补排行点击播放路由回归测试 |
| `ios/ShortDrama/Tests/ViewModelTests/DeeplinkHandlerTests.swift` | 修改 | 补排行 Deeplink 回归测试 |

### Step 2：先补数据契约测试，再接通排行与预约接口

- **关联测试**：T-01、T-08、T-09
- **目标文件**：`ios/ShortDrama/Sources/Domain/Entities/RankingType.swift`、`ios/ShortDrama/Sources/Domain/Entities/RankingContentType.swift`、`ios/ShortDrama/Sources/Domain/Entities/RankingQuery.swift`、`ios/ShortDrama/Sources/Domain/Entities/RankingDrama.swift`、`ios/ShortDrama/Sources/Domain/Entities/BookDramaResult.swift`、`ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift`、`ios/ShortDrama/Sources/Domain/UseCases/FetchRankingsUseCase.swift`、`ios/ShortDrama/Sources/Domain/UseCases/BookDramaUseCase.swift`、`ios/ShortDrama/Sources/Data/DTOs/RankingDramaDTO.swift`、`ios/ShortDrama/Sources/Data/DTOs/RankingListResponseDTO.swift`、`ios/ShortDrama/Sources/Data/DTOs/BookDramaResponseDTO.swift`、`ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift`、`ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift`、`ios/ShortDrama/Tests/DataTests/APIClientTests.swift`、`ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift`、`ios/ShortDrama/Tests/Mocks/MockDramaRepository.swift`
- **实现内容**：
  1. 先在 `APIClientTests`、`DramaRepositoryTests` 中锁定 `GET /api/dramas/rankings` 的 query 参数、`POST /api/dramas/{id}/book` 的 path/方法，以及 DTO 到 Entity 的映射规则。
  2. 在 Domain 层新增排行枚举、查询实体、列表项实体和预约结果实体，并扩展 `DramaRepositoryProtocol`、`FetchRankingsUseCase`、`BookDramaUseCase`，让 Presentation 只依赖 UseCase/Protocol。
  3. 在 Data 层补齐 rankings / booking 的 DTO、endpoint、RemoteDataSource 和 Repository 映射，保持网络实现继续基于现有 `APIClient + URLSession`。
  4. 同步扩展 `MockDramaRepository`，支持默认加载、分页、预约成功、预约失败和未登录拦截等后续 ViewModel 场景，不为测试临时引入新的 mock 基础设施。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认数据层契约测试通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Domain/Entities/RankingType.swift` | 新增 | 榜单类型枚举 |
| `ios/ShortDrama/Sources/Domain/Entities/RankingContentType.swift` | 新增 | 内容类型枚举 |
| `ios/ShortDrama/Sources/Domain/Entities/RankingQuery.swift` | 新增 | 排行查询实体 |
| `ios/ShortDrama/Sources/Domain/Entities/RankingDrama.swift` | 新增 | 排行列表项实体 |
| `ios/ShortDrama/Sources/Domain/Entities/BookDramaResult.swift` | 新增 | 预约结果实体 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 修改 | 扩展排行读取与预约协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchRankingsUseCase.swift` | 新增 | 排行读取用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/BookDramaUseCase.swift` | 新增 | 预约提交用例 |
| `ios/ShortDrama/Sources/Data/DTOs/RankingDramaDTO.swift` | 新增 | 排行数据 DTO |
| `ios/ShortDrama/Sources/Data/DTOs/RankingListResponseDTO.swift` | 新增 | 排行列表响应 DTO |
| `ios/ShortDrama/Sources/Data/DTOs/BookDramaResponseDTO.swift` | 新增 | 预约响应 DTO |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 新增 rankings / book 请求 |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 修改 | 新增排行与预约映射 |
| `ios/ShortDrama/Tests/DataTests/APIClientTests.swift` | 修改 | 覆盖 rankings/booking endpoint 契约 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 修改 | 覆盖 DTO -> Entity 映射 |
| `ios/ShortDrama/Tests/Mocks/MockDramaRepository.swift` | 修改 | 支持排行分页与预约 mock 行为 |

### Step 3：先写 ViewModel 测试，再落地默认加载、双层 Tab 与首屏恢复状态机

- **关联测试**：T-01、T-02、T-03、T-04、T-05
- **目标文件**：`ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift`、`ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift`、`ios/ShortDrama/Sources/Features/Ranking/Views/Components/RankingPrimaryTabBar.swift`、`ios/ShortDrama/Sources/Features/Ranking/Views/Components/RankingSecondaryTabBar.swift`、`ios/ShortDrama/Sources/Features/Ranking/Views/Components/RankingStateView.swift`、`ios/ShortDrama/Tests/ViewModelTests/RankingViewModelTests.swift`
- **实现内容**：
  1. 先新增 `RankingViewModelTests`，覆盖默认加载 `all + hot`、一级 / 二级 Tab 切换保留另一维、乱序响应只消费最后一次结果、首屏失败后 `retry()` 恢复等关键状态流转。
  2. 在 `RankingViewModel` 中建立显式状态机，至少包含 `loading / content / empty / error` 与分页、请求 token、当前页码、总页数等状态，保证状态清晰可追踪。
  3. 实现 `loadIfNeeded()`、`selectContentType(_:)`、`selectRankingType(_:)`、`retry()`，所有 Tab 切换都必须先重置列表和页码，再发起新请求。
  4. 在 View 层接好 `RankingHomeView`、一级 Tab、二级 Tab 和主状态容器，让页面结构先能稳定承接状态机，再进入列表和预约细节实现。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-01 ～ T-05 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` | 新增 | 默认加载、Tab 切换、失败恢复状态机 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift` | 修改 | 接入排行页根视图与状态驱动渲染 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/Components/RankingPrimaryTabBar.swift` | 新增 | 一级 Tab 组件 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/Components/RankingSecondaryTabBar.swift` | 新增 | 二级 Tab 组件 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/Components/RankingStateView.swift` | 新增 | loading / empty / error / content 状态容器 |
| `ios/ShortDrama/Tests/ViewModelTests/RankingViewModelTests.swift` | 新增 | 覆盖默认加载、双层 Tab、乱序响应、重试恢复 |

### Step 4：先补列表与预约交互测试，再实现分页追加、预约更新与登录拦截

- **关联测试**：T-06、T-07、T-08、T-09、T-10
- **目标文件**：`ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift`、`ios/ShortDrama/Sources/Features/Ranking/Views/Components/RankingListView.swift`、`ios/ShortDrama/Sources/Features/Ranking/Views/Components/RankingDramaCardView.swift`、`ios/ShortDrama/Sources/Features/Ranking/Views/Components/RankingMetricView.swift`、`ios/ShortDrama/Sources/Features/Ranking/Views/Components/RankingBookingButton.swift`、`ios/ShortDrama/Tests/ViewModelTests/RankingViewModelTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`
- **实现内容**：
  1. 继续在 `RankingViewModelTests` 中补齐分页成功追加、分页失败保留旧列表、已登录预约成功局部更新、未登录预约拦截、重复点击去重等场景，先定义期望状态再补实现。
  2. 在 `RankingViewModel` 中增加 `loadMoreIfNeeded()`、预约提交与局部 patch 逻辑，区分首屏错误与分页尾部错误，保证失败恢复不把已加载内容清空。
  3. 用 `RankingListView`、`RankingDramaCardView`、`RankingMetricView`、`RankingBookingButton` 落地列表项 UI：热榜展示热度值，推荐榜展示推荐值，预约榜展示预约数与按钮。
  4. 对未登录预约，优先通过 `routeEffect` 或等价回调把登录拦截交给 App 层统一消费，不在排行模块内部新建登录页面；对已登录预约成功，仅更新当前项 `isBooked` 和 `bookingCount`。
  5. 将卡片点击统一接到 `RankingRouteBuilder.playRoute(for:)`，并在 `NavigationRouterTests` 中回归验证排行项依旧进入 `.player(videoId:)`。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-06 ～ T-10 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` | 修改 | 增加分页、预约、登录拦截与局部更新逻辑 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/Components/RankingListView.swift` | 新增 | 列表渲染与触底分页承接 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/Components/RankingDramaCardView.swift` | 新增 | 排行卡片、整卡点击与预约入口 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/Components/RankingMetricView.swift` | 新增 | 按榜单类型渲染指标值 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/Components/RankingBookingButton.swift` | 新增 | 未预约 / 提交中 / 已预约按钮状态 |
| `ios/ShortDrama/Tests/ViewModelTests/RankingViewModelTests.swift` | 修改 | 覆盖分页、预约成功、未登录拦截、失败恢复 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 回归排行项点击播放路由 |

### Step 5：执行工程生成与 iOS 全量回归，固化 coding 验收基线

- **关联测试**：T-01 ～ T-11
- **目标文件**：`docs/specs/2026-07-27-prd-05-ranking/plan-ios.md`
- **实现内容**：
  1. 在 coding 阶段严格按本计划顺序推进：先补测试，再补实现，新增业务逻辑必须同步补齐 Swift Testing 用例。
  2. 由于会新增 `Features/Ranking`、Domain、Data、Tests 多个 Swift 文件，先运行 `xcodegen generate` 更新工程，再执行 test / build / lint 回归。
  3. 回看自动化覆盖面，确保默认加载、双层 Tab 切换、分页追加、预约成功、未登录预约拦截、失败态恢复这些关键 iOS 逻辑都已被单元测试锁定，而不是只靠手工点击验证。
  4. 将 coding 阶段的收口标准固定为：真实排行页可承接 `rankingHome`，状态机可回归，接口契约正确，播放跳转不破坏现有 `play` 语义，且无新增 lint 错误。
- **验证方式**：
  - 运行 `cd ios && xcodegen generate`
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - 运行 `cd ios && swiftlint lint`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `docs/specs/2026-07-27-prd-05-ranking/plan-ios.md` | 新增 | 固化 iOS 排行实现步骤、测试场景与验收基线 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5
```

## 验证总览

- [ ] 所有测试通过（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [ ] Build 成功（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [ ] 无新增 lint 错误（`cd ios && swiftlint lint`）
- [ ] 新增源码后已执行 `cd ios && xcodegen generate`
- [ ] 默认加载、双层 Tab 切换、分页追加、预约成功、未登录预约拦截、失败态恢复均有单元测试覆盖
- [ ] `djsdrama://ranking` 仍承接到 `.rankingHome`
- [ ] 排行项点击仍复用 `.player(videoId:)`，不引入新的播放路由语义
- [ ] 排行数据与预约请求均通过现有 `APIClient + URLSession` 接线，无新增第三方依赖

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 将 `.rankingHome` 接到真实排行页 |
| `ios/ShortDrama/Sources/Features/Search/Views/DiscoveryPlaceholderView.swift` | 修改 | 移除排行占位承接职责 |
| `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift` | 新增 | 统一排行到播放页路由构建 |
| `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift` | 新增 | 排行状态机、分页、预约与登录拦截 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift` | 新增 | 排行页根视图 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/Components/*.swift` | 新增 | 一级 Tab、二级 Tab、状态容器、列表、卡片、指标、预约按钮 |
| `ios/ShortDrama/Sources/Domain/Entities/RankingType.swift` | 新增 | 榜单类型枚举 |
| `ios/ShortDrama/Sources/Domain/Entities/RankingContentType.swift` | 新增 | 内容类型枚举 |
| `ios/ShortDrama/Sources/Domain/Entities/RankingQuery.swift` | 新增 | 排行查询实体 |
| `ios/ShortDrama/Sources/Domain/Entities/RankingDrama.swift` | 新增 | 排行列表项实体 |
| `ios/ShortDrama/Sources/Domain/Entities/BookDramaResult.swift` | 新增 | 预约结果实体 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 修改 | 扩展排行与预约协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchRankingsUseCase.swift` | 新增 | 排行读取用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/BookDramaUseCase.swift` | 新增 | 预约提交用例 |
| `ios/ShortDrama/Sources/Data/DTOs/RankingDramaDTO.swift` | 新增 | 排行 DTO |
| `ios/ShortDrama/Sources/Data/DTOs/RankingListResponseDTO.swift` | 新增 | 排行分页响应 DTO |
| `ios/ShortDrama/Sources/Data/DTOs/BookDramaResponseDTO.swift` | 新增 | 预约响应 DTO |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 新增 rankings / book API 接线 |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 修改 | 新增排行与预约映射 |
| `ios/ShortDrama/Tests/Mocks/MockDramaRepository.swift` | 修改 | 支持排行分页与预约 mock |
| `ios/ShortDrama/Tests/DataTests/APIClientTests.swift` | 修改 | 覆盖接口契约与 query/path |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 修改 | 覆盖 DTO -> Entity 映射 |
| `ios/ShortDrama/Tests/ViewModelTests/RankingViewModelTests.swift` | 新增 | 覆盖默认加载、切换、分页、预约、恢复 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 覆盖排行点击播放路由 |
| `ios/ShortDrama/Tests/ViewModelTests/DeeplinkHandlerTests.swift` | 修改 | 覆盖排行 Deeplink 回归 |
