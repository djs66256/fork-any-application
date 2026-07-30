# 实现计划：iOS — PRD-11 个人资产管理

> 创建日期：2026-07-30
> 对应技术方案：design-ios.md
> 对应需求：spec.md

## 概述

本计划聚焦 iOS 端“我的预约”从菜单占位升级为真实 Native 页面，打通独立 booking route、登录承接回流、受保护资产接口读取，以及列表分页与错误态管理；“我的下载”继续保留占位页，仅统一入口承接语义。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 所有场景都要求有单元测试，优先覆盖 Router、ViewModel 与 Data 层 contract。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 | 单元测试关注点 |
|------|---------|------|---------|------|--------|---------------|
| T-01 | booking 独立 route 注册成功 | 导航到 `.bookingAssets` | route 归属 `.home`，`publicRouteName = "menu/booking"`，`TabNavigationHostView` 能承接 booking 页面 | 单元测试 | P0 | `AppRoute` 元信息与 `NavigationRouterTests` 路由断言 |
| T-02 | 菜单点击“我的预约”走真实 booking route | 菜单打开时点击 booking 入口 | 先关闭菜单，再导航到 `.bookingAssets`；downloads 仍走 placeholder | 单元测试 | P0 | `NavigationRouterTests` 验证 pending navigation 与 close-then-navigate 语义 |
| T-03 | 匿名用户进入 booking 页看到登录承接 | `authStore.status = anonymous/expired` | 不发起受保护请求，展示 booking 登录承接文案并可触发登录 | 单元测试 | P0 | `BookingAssetsViewModelTests` 或 ViewModel 驱动测试验证匿名态不触发请求 |
| T-04 | 登录完成后 booking route 幂等回流 | 当前已在 `.bookingAssets` 顶部，执行 `completeLogin()` | 不重复 push booking route，仍停留在 booking 页面上下文 | 单元测试 | P0 | `NavigationRouterTests` 验证 stack 深度不增加 |
| T-05 | booking 首屏加载成功并消费服务端 summary | token 有效，接口返回 `{ data, pagination, summary }` | 默认 `online` 首屏进入 `content/empty`，`summary` 直接使用服务端值 | 单元测试 | P0 | `BookingAssetsViewModelTests` 验证首屏状态机、默认 query、summary 不本地重算 |
| T-06 | 快速切换 Tab 时旧请求不覆盖新状态 | 连续切换 `online -> upcoming -> online`，响应乱序返回 | 仅消费最后一次有效响应，不出现串页 | 单元测试 | P0 | `BookingAssetsViewModelTests` 验证 `requestToken`/乱序保护 |
| T-07 | 分页追加失败不清空现有列表 | 已有首屏内容，下一页请求失败 | 保留当前列表，只展示 append 局部错误并支持重试 | 单元测试 | P1 | `BookingAssetsViewModelTests` 验证 append error 与 content 分离 |
| T-08 | 401/未授权时回到登录承接态 | 首屏或分页返回 401 | 清空旧用户内容，回到 booking 登录承接态 | 单元测试 | P0 | `BookingAssetsViewModelTests` 验证 unauthorized 处理 |
| T-09 | 受保护 endpoint 正确携带 query 与 bearer token | `status=upcoming&page=2&pageSize=20` + token | 请求路径为 `/api/users/me/bookings`，query 正确，带 `Authorization` header | 单元测试 | P0 | `DramaRepositoryTests` / `APIClientTests` 使用 `URLProtocolMock` 校验 |
| T-10 | booking DTO 与 repository 正确解析 snake_case 响应 | backend 返回 booking assets 响应 JSON | DTO 解码成功，Entity 映射得到 `dramaID/title/bookedAt/availabilityStatus/summary/pagination` | 单元测试 | P0 | `DramaRepositoryTests` 验证 decode 与 DTO -> Entity 映射 |

## 实现步骤

### Step 1：补齐 booking 独立 route 与菜单入口导航骨架

- **关联测试**：T-01、T-02
- **目标文件**：`ios/ShortDrama/Sources/App/AppRoute.swift`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`、`ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift`
- **实现内容**：
  1. 在 `AppRoute` 中新增 `case bookingAssets`，并将其 `owningTab` 固定到 `.home`。
  2. 为 booking route 配置 canonical `publicRouteName = "menu/booking"`，与共享设计保持一致。
  3. 在 `TabNavigationHostView` 中注册 `BookingAssetsView` destination，确保 home tab 的 `NavigationStack` 能承接真实 booking 页面。
  4. 将 `MenuPanelContainerView` 的 booking 点击逻辑从 `.menuPlaceholder(kind: .booking)` 改为 `.bookingAssets`。
  5. 保持 downloads 入口继续走 `.menuPlaceholder(kind: .downloads)`，避免把下载占位误纳入本期真实资产页链路。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`，确认 T-01、T-02 对应单测通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增 booking route 元信息 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 注册 booking 页面 destination |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift` | 修改 | booking 入口改走真实 route |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 增加 booking route / 菜单入口路由断言 |

### Step 2：补齐登录承接上下文与 `completeLogin()` 幂等回流

- **关联测试**：T-03、T-04
- **目标文件**：`ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift`、`ios/ShortDrama/Sources/Features/Auth/Views/LoginView.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Sources/Features/BookingAssets/BookingAssetsRouteBuilder.swift`
- **实现内容**：
  1. 为 `LoginInterceptionContext.Source` 增加 `.bookingAssets`，用于区分预约页登录承接来源。
  2. 新增 `BookingAssetsRouteBuilder`，统一产出 `source = .bookingAssets`、`returnRoute = .bookingAssets` 的登录上下文。
  3. 在 `LoginView` 中补齐 booking 场景 copy，例如“登录后查看我的预约”，让文案与 profile/ranking 分离。
  4. 扩展 `NavigationRouter.completeLogin()`：当当前 tab 已是 `.home` 且栈顶已经为 `.bookingAssets` 时，仅完成登录 dismiss，不重复 append booking route。
  5. 继续兼容现有 ranking/profile 回流，不回归已有登录流程。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`，确认 T-03、T-04 对应单测通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift` | 修改 | 新增 booking 登录来源 |
| `ios/ShortDrama/Sources/Features/Auth/Views/LoginView.swift` | 修改 | 新增 booking 登录承接文案 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | `completeLogin()` 增加 booking 幂等回流 |
| `ios/ShortDrama/Sources/Features/BookingAssets/BookingAssetsRouteBuilder.swift` | 新增 | 统一构建 booking 登录上下文 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 增加 booking 登录回流与不重复 push 测试 |

### Step 3：实现 BookingAssets 领域模型、受保护 endpoint、DTO 与 repository

- **关联测试**：T-09、T-10
- **目标文件**：`ios/ShortDrama/Sources/Domain/Entities/BookingAsset*.swift`、`ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift`、`ios/ShortDrama/Sources/Domain/UseCases/FetchBookingAssetsUseCase.swift`、`ios/ShortDrama/Sources/Data/DTOs/BookingAsset*.swift`、`ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift`、`ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift`
- **实现内容**：
  1. 在 Domain 层新增 `BookingAsset`、`BookingAssetQuery`、`BookingAssetSummary`、`BookingAssetPage` 与状态枚举，明确默认查询参数与分页语义。
  2. 扩展 `DramaRepositoryProtocol` 与 `FetchBookingAssetsUseCase`，为 booking assets 提供独立读取 contract。
  3. 在 Data 层新增 `BookingAssetDTO`、`BookingAssetSummaryDTO`、`BookingAssetListResponseDTO`，对齐 `{ data, pagination, summary }` 响应结构。
  4. 在 `DramaRemoteDataSource` 中新增 `GET /api/users/me/bookings` endpoint，按 query 透传 `status/page/pageSize`，并显式带上 `Authorization: Bearer <token>`。
  5. 在 `DramaRepository` 中完成 DTO -> Entity 映射，确保 `summary` 与分页字段原样进入领域模型。
  6. 如需提及 `BookDramaResponseDTO` 与 backend snake_case 的历史漂移，只作为背景记录，不纳入本期主实现步骤。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`，确认 T-09、T-10 对应单测通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Domain/Entities/BookingAsset.swift` | 新增 | 预约资产实体 |
| `ios/ShortDrama/Sources/Domain/Entities/BookingAssetQuery.swift` | 新增 | booking 查询参数 |
| `ios/ShortDrama/Sources/Domain/Entities/BookingAssetSummary.swift` | 新增 | 双 Tab 计数摘要 |
| `ios/ShortDrama/Sources/Domain/Entities/BookingAssetPage.swift` | 新增 | booking 分页结果 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 修改 | 新增 booking assets contract |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchBookingAssetsUseCase.swift` | 新增 | booking assets 用例 |
| `ios/ShortDrama/Sources/Data/DTOs/BookingAssetDTO.swift` | 新增 | booking item DTO |
| `ios/ShortDrama/Sources/Data/DTOs/BookingAssetSummaryDTO.swift` | 新增 | booking summary DTO |
| `ios/ShortDrama/Sources/Data/DTOs/BookingAssetListResponseDTO.swift` | 新增 | booking 列表响应 DTO |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 新增受保护 booking endpoint |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 修改 | 实现 booking DTO 到 Entity 映射 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 修改 | 增加 booking assets 请求/映射测试 |
| `ios/ShortDrama/Tests/DataTests/APIClientTests.swift` | 修改 | 增加 query/header contract 测试 |

### Step 4：实现 BookingAssetsViewModel 首屏、切 Tab、分页与未授权处理

- **关联测试**：T-05、T-06、T-07、T-08
- **目标文件**：`ios/ShortDrama/Sources/Features/BookingAssets/ViewModels/BookingAssetsViewModel.swift`、`ios/ShortDrama/Tests/Mocks/MockDramaRepository.swift`、`ios/ShortDrama/Tests/ViewModelTests/BookingAssetsViewModelTests.swift`
- **实现内容**：
  1. 新增 `BookingAssetsViewModel`，定义 `idle/loading/content/empty/error` 主状态机，以及 `isAppending/appendErrorMessage` 追加状态。
  2. 默认 `selectedStatus = .online`，首次进入按 `page=1&pageSize=20` 请求受保护接口。
  3. 复用 ranking 的 `requestToken` 思路，保证切换 Tab 或 retry 时旧响应不会覆盖最新状态。
  4. 分离首屏失败与追加失败：首屏失败走整页错误态，追加失败只影响 footer，不清空已有列表。
  5. 对 401 做统一恢复：清空当前快照并回到登录承接态，不展示旧用户资产。
  6. 仅消费服务端返回的 `summary`，不在 ViewModel 本地重算 `online/upcoming` 计数。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`，确认 T-05、T-06、T-07、T-08 对应单测通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/BookingAssets/ViewModels/BookingAssetsViewModel.swift` | 新增 | booking 页状态机与交互逻辑 |
| `ios/ShortDrama/Tests/Mocks/MockDramaRepository.swift` | 修改 | 支持 booking assets mock 场景 |
| `ios/ShortDrama/Tests/ViewModelTests/BookingAssetsViewModelTests.swift` | 新增 | 首屏、切 Tab、防乱序、分页、401 测试 |

### Step 5：实现 BookingAssetsView 与登录承接/列表/空态/错误态界面拼装

- **关联测试**：T-03、T-05、T-07、T-08
- **目标文件**：`ios/ShortDrama/Sources/Features/BookingAssets/Views/BookingAssetsView.swift`、`ios/ShortDrama/Sources/Features/BookingAssets/Views/Components/*.swift`
- **实现内容**：
  1. 新增 `BookingAssetsView`，通过 `@EnvironmentObject` 接入 `NavigationRouter` 与 `AuthStore`。
  2. 按认证状态分层渲染：`restoring` 显示 loading；`anonymous/expired` 显示登录承接；`authenticated/refreshing` 进入 ViewModel 加载。
  3. 在页面中组合顶部导航栏、Tab 条、列表卡片、空态、错误态、append footer 与登录承接视图。
  4. 登录按钮通过 `BookingAssetsRouteBuilder.loginContext()` 调用统一登录流程；登录成功后依赖 Step 2 的幂等回流语义继续停留在 booking route。
  5. 空态文案按当前 Tab 区分“暂无已上线预约 / 暂无待上线预约”，downloads 不在此页面承接。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`，确认与 BookingAssetsViewModel 相关单测保持通过
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`，确认新页面与新组件可编译
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/BookingAssets/Views/BookingAssetsView.swift` | 新增 | booking 页根视图 |
| `ios/ShortDrama/Sources/Features/BookingAssets/Views/Components/BookingAssetsTabBar.swift` | 新增 | 状态 Tab 组件 |
| `ios/ShortDrama/Sources/Features/BookingAssets/Views/Components/BookingAssetCardView.swift` | 新增 | 资产卡片组件 |
| `ios/ShortDrama/Sources/Features/BookingAssets/Views/Components/BookingAssetsEmptyView.swift` | 新增 | 状态化空态组件 |
| `ios/ShortDrama/Sources/Features/BookingAssets/Views/Components/BookingAssetsErrorView.swift` | 新增 | 首屏错误态组件 |
| `ios/ShortDrama/Sources/Features/BookingAssets/Views/Components/BookingAssetsLoginGateView.swift` | 新增 | 登录承接组件 |

### Step 6：补齐回归测试与端侧验证收口

- **关联测试**：T-01 ～ T-10
- **目标文件**：`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/BookingAssetsViewModelTests.swift`、`ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift`、`ios/ShortDrama/Tests/DataTests/APIClientTests.swift`
- **实现内容**：
  1. 将 booking 相关新增测试整理到 Router、ViewModel、Data 三层，形成 route -> auth handoff -> endpoint contract -> state machine 的闭环。
  2. 明确每个需求场景至少对应一个单测用例，避免只测 happy path。
  3. 回归校验 ranking/profile/下载占位路径不被 booking 改造破坏。
  4. 在编码完成后统一执行生成、测试与构建命令，确保新增文件被 XcodeGen 自动纳入工程并成功编译。
- **验证方式**：
  - 运行 `cd ios && xcodegen generate`
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | booking route、菜单导航、登录回流测试 |
| `ios/ShortDrama/Tests/ViewModelTests/BookingAssetsViewModelTests.swift` | 新增 | booking 状态机测试 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 修改 | booking DTO/repository 测试 |
| `ios/ShortDrama/Tests/DataTests/APIClientTests.swift` | 修改 | booking endpoint contract 测试 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 5
   │          │
   │          └──────▶ Step 4
   └──────▶ Step 3 ──▶ Step 4 ──▶ Step 5 ──▶ Step 6
```

## 验证总览

- [ ] 已生成工程（`cd ios && xcodegen generate`）
- [ ] 所有单元测试通过（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [ ] Build 成功（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [ ] 无新增 lint 错误（构建阶段 SwiftLint preBuildScript 无新增失败）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增 booking route 元信息 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | booking 登录幂等回流与菜单导航收口 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 注册 booking 页面 destination |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift` | 修改 | booking 入口改造 |
| `ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift` | 修改 | 新增 booking 登录来源 |
| `ios/ShortDrama/Sources/Features/Auth/Views/LoginView.swift` | 修改 | booking 登录承接 copy |
| `ios/ShortDrama/Sources/Features/BookingAssets/BookingAssetsRouteBuilder.swift` | 新增 | booking 登录上下文 builder |
| `ios/ShortDrama/Sources/Domain/Entities/BookingAsset.swift` | 新增 | 预约资产实体 |
| `ios/ShortDrama/Sources/Domain/Entities/BookingAssetQuery.swift` | 新增 | 预约资产查询模型 |
| `ios/ShortDrama/Sources/Domain/Entities/BookingAssetSummary.swift` | 新增 | summary 实体 |
| `ios/ShortDrama/Sources/Domain/Entities/BookingAssetPage.swift` | 新增 | booking 分页聚合结果 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 修改 | 新增 booking assets contract |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchBookingAssetsUseCase.swift` | 新增 | booking assets 用例 |
| `ios/ShortDrama/Sources/Data/DTOs/BookingAssetDTO.swift` | 新增 | booking item DTO |
| `ios/ShortDrama/Sources/Data/DTOs/BookingAssetSummaryDTO.swift` | 新增 | summary DTO |
| `ios/ShortDrama/Sources/Data/DTOs/BookingAssetListResponseDTO.swift` | 新增 | 列表响应 DTO |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 受保护 booking endpoint |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 修改 | booking DTO -> Entity 映射 |
| `ios/ShortDrama/Sources/Features/BookingAssets/ViewModels/BookingAssetsViewModel.swift` | 新增 | booking 状态机 |
| `ios/ShortDrama/Sources/Features/BookingAssets/Views/BookingAssetsView.swift` | 新增 | booking 页面根视图 |
| `ios/ShortDrama/Sources/Features/BookingAssets/Views/Components/*.swift` | 新增 | booking 子组件 |
| `ios/ShortDrama/Tests/Mocks/MockDramaRepository.swift` | 修改 | booking mock 支持 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | booking router 测试 |
| `ios/ShortDrama/Tests/ViewModelTests/BookingAssetsViewModelTests.swift` | 新增 | booking ViewModel 测试 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 修改 | booking repository 测试 |
| `ios/ShortDrama/Tests/DataTests/APIClientTests.swift` | 修改 | booking endpoint contract 测试 |

## 备注

- `BookDramaResponseDTO` 与 backend contract 的历史漂移仅作为非阻塞背景说明，不作为本期 iOS 主计划步骤；若后续顺手修复，应单独评估回归影响。