# 实现计划：iOS — PRD-02 首页信息流

> 创建日期：2026-07-25
> 对应技术方案：design-ios.md
> 对应需求：spec.md

## 概述

本期 iOS 端将在现有 `NavigationRouter + MVVM + Clean Architecture` 基础上，把首页从应用信息占位页演进为 Native Feed 首屏。实现重点是把数据链路从 `/api/v1/dramas` 迁移到 `/api/dramas?page&pageSize` 与 `{ data, pagination }` canonical contract，同时将 `HomeViewModel` 扩展为明确的首页状态机，并让 `HomeView` 按列表、空态、错误态和重试语义渲染，再复用既有 `player/detail` 路由完成主链路联通；全程不新增依赖。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 首页列表接口迁移到 canonical contract | 调用 `DramaEndpoints.GetDramas(page: 1, pageSize: 10)` | path 为 `/api/dramas`，query 为 `page=1&pageSize=10`，不再出现 `/api/v1/dramas` 或 `page_size` | 单元测试 | P0 |
| T-02 | 列表响应按 `{ data, pagination }` 解码成功 | 返回包含 `data` 和 `pagination` 的 snake_case JSON | `DramaRemoteDataSource.fetchDramas` 返回 `DramaDTO[]`，Repository 成功映射为 `Drama[]` | 单元测试 | P0 |
| T-03 | 首页首次加载成功进入列表态 | repository 返回至少 1 条 `Drama` | `HomeViewModel` 从 loading 进入 content/list 状态，保留卡片数据且无错误文案 | 单元测试 | P0 |
| T-04 | 首页首次加载为空进入空态 | repository 返回空数组 | `HomeViewModel` 结束 loading，进入 empty 状态 | 单元测试 | P0 |
| T-05 | 首页首次加载失败进入错误态 | repository 抛出 `APIError.network` 或 `APIError.server` | `HomeViewModel` 结束 loading，进入 error 状态并暴露可展示文案 | 单元测试 | P0 |
| T-06 | 错误态重试可恢复且避免重复并发 | 首次失败，随后成功；或连续点击 retry | 状态按 `error -> retrying/loading -> content/empty` 迁移，重复重试不会并发触发多次请求 | 单元测试 | P0 |
| T-07 | 首页卡片动作复用既有播放/详情路由 | 列表项 `drama.id` 非空 | `HomeView` 通过 `NavigationRouter` 导航到 `.player(videoId:)` 与 `.dramaDetail(dramaId:)`；空 id 不进入正常导航 | 单元测试 | P0 |

## 实现步骤

### Step 1：先锁定数据契约测试，再迁移 `DramaRemoteDataSource`

- **关联测试**：T-01、T-02
- **目标文件**：`ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift`、`ios/ShortDrama/Tests/DataTests/APIClientTests.swift`、`ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift`、`ios/ShortDrama/Tests/DataTests/DramaDTOTests.swift`
- **实现内容**：
  1. 先在现有 DataTests 中补充对 drama 列表 endpoint 的断言，锁定 path 必须是 `/api/dramas`、query 必须是 `page/pageSize`，避免 coding 时继续沿用 `/api/v1/dramas` 与 `page_size`。
  2. 将 `DramaRemoteDataSource` 的列表响应从当前 `code + data.items` 包裹结构迁移为 `{ data, pagination }`，只保留首页首屏当前实际使用的 `data` 数组消费方式。
  3. 保持 `APIClient`、`DramaRepository`、`FetchDramasUseCase` 分层不变，让 Repository 继续承担 `DTO -> Entity` 映射，不新增新的网络层或 repository 抽象。
  4. 复用现有 `JSONDecoder.convertFromSnakeCase` 能力验证 `cover_url`、`episode_count`、`created_at`、`updated_at`、`page_size`、`total_pages` 等字段解码仍成立。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-01、T-02 对应数据层测试通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 列表请求迁移到 `/api/dramas?page&pageSize` 与 `{ data, pagination }` |
| `ios/ShortDrama/Tests/DataTests/APIClientTests.swift` | 修改 | 补列表 endpoint path/query 与响应结构回归测试 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 修改 | 补列表响应到 Repository 输出的回归断言 |
| `ios/ShortDrama/Tests/DataTests/DramaDTOTests.swift` | 复核/轻微修改 | 确认首页卡片字段的 snake_case 解码继续有效 |

### Step 2：先重写 `HomeViewModel` 测试，再收口首页状态机

- **关联测试**：T-03、T-04、T-05、T-06
- **目标文件**：`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift`、`ios/ShortDrama/Tests/ViewModelTests/HomeViewModelTests.swift`、`ios/ShortDrama/Tests/Mocks/MockDramaRepository.swift`
- **实现内容**：
  1. 先把现有 `HomeViewModelTests.swift` 从 `isLoading / errorMessage` 的简化断言升级为首页 Feed 状态机测试，覆盖 success with items、empty、error、retry recovery、重复重试去重。
  2. 在 `HomeViewModel` 中把当前 `isLoading`、`errorMessage` 为主的模型收口为显式 `ViewState`（至少覆盖 `loading / content / empty / error`），并保留 `isRetrying` 或等价字段以支撑重试按钮禁用与文案表现。
  3. 增加 `loadIfNeeded()`，确保首页首次进入自动请求 `page = 1, pageSize = 10`，同时避免用户返回首页或视图重复出现时无意义重复拉取。
  4. `retry()` 复用同一条数据加载链路，错误映射继续基于现有 `APIError`，不引入新的错误层；若 `MockDramaRepository` 现状不足以模拟“先失败后成功”，则在现有 mock 中补这一行为支持。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-03 ～ T-06 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` | 修改 | 引入首页 Feed 状态机、`loadIfNeeded()` 与 `retry()` |
| `ios/ShortDrama/Tests/ViewModelTests/HomeViewModelTests.swift` | 修改 | 用首页列表/空态/错误态/重试场景替换旧占位状态测试 |
| `ios/ShortDrama/Tests/Mocks/MockDramaRepository.swift` | 修改 | 支持重试恢复与重复请求控制所需的 mock 行为 |

### Step 3：按状态机重构 `HomeView`，落地列表/空态/错误态/重试与导航动作

- **关联测试**：T-03、T-04、T-05、T-06、T-07
- **目标文件**：`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`
- **实现内容**：
  1. 将 `HomeView` 从当前“应用名 + 版本号 + 示例按钮”占位结构改为基于 `HomeViewModel` 状态分支渲染：loading、列表态、空态、错误态。
  2. 列表态在现有 `HomeView.swift` 内先用 `ScrollView + LazyVStack` 搭建首页卡片，不额外臆造新的组件文件；卡片展示标题、描述及分类/标签/评分/集数中的稳定子集，并对缺封面、空描述、空标签做降级展示。
  3. 错误态提供 retry 按钮并接到 `viewModel.retry()`；重试中根据 `isRetrying` 或等价状态禁用按钮，避免重复并发请求。
  4. 卡片主次动作继续复用现有 `NavigationRouter`：`drama.id -> .player(videoId:)` 与 `drama.id -> .dramaDetail(dramaId:)`；若 id 为空则在 View 层拦截，不构造异常路由。
  5. 保持既有首页归属 home tab 的导航语义不变，不新增 deeplink 规则，不修改 player/detail 页的承载边界。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 回归首页状态机与路由测试
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认首页 UI 改造可编译
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 从占位页改为首页列表/空态/错误态/重试 UI |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 复核/轻微修改 | 复用既有 player/detail 导航语义，必要时补首页动作联通保护 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 复核/轻微修改 | 确认首页卡片动作不会破坏现有 home tab 路由归属 |

### Step 4：做 iOS 端回归收口，固化测试策略与验证基线

- **关联测试**：T-01 ～ T-07
- **目标文件**：`ios/ShortDrama/Tests/DataTests/APIClientTests.swift`、`ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/HomeViewModelTests.swift`、`docs/specs/2026-07-25-prd-02-homepage-feed/plan-ios.md`
- **实现内容**：
  1. 回看测试覆盖，确保首页主链路至少覆盖：endpoint 迁移、响应解码、列表成功、空态、错误态、重试恢复、导航联通。
  2. 执行全量 iOS 测试与 build，确认此次改造没有破坏 PRD-01 已完成的底部导航、deeplink 和 player/detail 路由基础能力。
  3. 若实现过程中发现某些 UI 细节难以在单元测试中直接断言，则以 ViewModel 状态测试和路由行为测试为主验证面，不额外引入 UI 测试框架或第三方库。
  4. 将最终 coding 约束固定为：只请求第一页、不新增依赖、首页状态由 `HomeViewModel` 单一来源驱动、`HomeView` 只负责状态渲染与动作转发。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - 运行 `cd ios && swiftlint lint`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Tests/DataTests/APIClientTests.swift` | 修改 | 收口 endpoint 与解码回归测试 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 修改 | 收口 Repository 列表映射回归测试 |
| `ios/ShortDrama/Tests/ViewModelTests/HomeViewModelTests.swift` | 修改 | 收口首页状态机与重试测试 |
| `docs/specs/2026-07-25-prd-02-homepage-feed/plan-ios.md` | 已完成 | 固化 iOS 端实现步骤与验证基线 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4
```

## 验证总览

- [ ] 所有测试通过（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [ ] Build 成功（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [ ] 无新增 lint 错误（`cd ios && swiftlint lint`）
- [ ] 列表接口已迁移到 `/api/dramas?page&pageSize` 且消费 `{ data, pagination }`
- [ ] 首页只请求第一页（`page = 1, pageSize = 10`）
- [ ] 首页状态机覆盖列表、空态、错误态、重试
- [ ] 首页卡片动作继续复用 `.player(videoId:)` 与 `.dramaDetail(dramaId:)`

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 首页列表接口迁移到 canonical contract |
| `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` | 修改 | 首页 Feed 状态机、首次加载与重试逻辑 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 首页列表/空态/错误态/重试与卡片动作渲染 |
| `ios/ShortDrama/Tests/DataTests/APIClientTests.swift` | 修改 | endpoint path/query 与响应结构回归测试 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 修改 | Repository 列表映射与数据返回回归测试 |
| `ios/ShortDrama/Tests/DataTests/DramaDTOTests.swift` | 复核/轻微修改 | 首页卡片字段 snake_case 解码回归 |
| `ios/ShortDrama/Tests/ViewModelTests/HomeViewModelTests.swift` | 修改 | 首页状态机与重试测试 |
| `ios/ShortDrama/Tests/Mocks/MockDramaRepository.swift` | 修改 | 支持首页状态机测试所需 mock 行为 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 复核/轻微修改 | 复用既有首页到播放/详情路由联通 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 复核/轻微修改 | 首页路由联通回归测试 |
