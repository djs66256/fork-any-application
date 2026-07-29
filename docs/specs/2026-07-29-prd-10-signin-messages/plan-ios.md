# 实现计划：iOS — PRD-10 签到与消息系统

> 创建日期：2026-07-29
> 对应技术方案：design-ios.md
> 对应需求：spec.md

## 概述

本计划聚焦 iOS 端签到与消息系统落地，沿用当前 `SwiftUI + NavigationStack + MVVM + Clean Architecture` 架构，按轻量 TDD 先补 Data/Domain 契约测试，再接通首页签到浮层、菜单消息预览、消息中心页与登录承接，最后用仓库现有 `xcodegen / xcodebuild / swiftlint` 命令完成回归收口。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 签到与消息数据层 contract 正确 | `GET /api/check-ins/status`、`POST /api/check-ins`、`GET /api/messages/preview`、`GET /api/messages/system`、`GET /api/messages/interactions` 的响应体、query 与 header | DTO decode、Entity 映射、`X-Installation-Id` header、`page/pageSize` query、preview 204 空态处理都符合设计 | 单元测试 | P0 |
| T-02 | installationId 与签到弹层关闭态持久化正确 | Keychain 无 installationId / 已有 installationId、按 `server_date` 读写关闭态 | 首次生成并复用 installationId；同一 `server_date` 关闭态可正确命中 | 单元测试 | P0 |
| T-03 | 首页冷启动签到状态机正确 | 首次进入首页、`should_show_popup=true/false`、已关闭、已签到、签到成功/失败 | 只在符合条件时展示弹层；关闭后按 `server_date` 记忆；签到成功后 UI 立即进入已签到态，失败保留重试 | 单元测试 | P0 |
| T-04 | 菜单消息预览状态正确 | preview 成功、204 空态、503/network 失败、短时间重复打开菜单 | 展示真实摘要 / “暂无消息” / 降级文案；成功结果可复用，入口始终可点击 | 单元测试 | P0 |
| T-05 | 消息中心匿名与登录双分区状态正确 | 匿名进入、登录进入、系统消息空态、互动消息失败、登录成功回流 | 匿名仅看系统消息并展示登录门槛；登录后加载互动消息；互动区失败不阻塞系统区 | 单元测试 | P0 |
| T-06 | 菜单到消息中心导航与登录承接正确 | 点击菜单消息入口、点击消息页登录按钮、登录成功/取消 | 先关菜单再 push `messages`；登录成功留在消息页；取消登录保持当前消息页匿名态 | 单元测试 | P0 |
| T-07 | iOS 工程回归命令可通过 | 新增签到与消息源文件、测试文件 | `xcodegen generate`、`xcodebuild test`、`xcodebuild build`、`swiftlint lint` 按 iOS 约定可执行 | 单元测试 | P0 |

## 实现步骤

### Step 1：补齐签到与消息的 Domain/Data 基础链路

- **关联测试**：T-01
- **目标文件**：`ios/ShortDrama/Sources/Domain/Entities/SignInStatus.swift`、`ios/ShortDrama/Sources/Domain/Entities/MessagePreview.swift`、`ios/ShortDrama/Sources/Domain/Entities/SystemMessage.swift`、`ios/ShortDrama/Sources/Domain/Entities/InteractionMessage.swift`、`ios/ShortDrama/Sources/Domain/RepositoryProtocols/CheckInRepositoryProtocol.swift`、`ios/ShortDrama/Sources/Domain/RepositoryProtocols/MessageRepositoryProtocol.swift`、`ios/ShortDrama/Sources/Domain/UseCases/FetchCheckInStatusUseCase.swift`、`ios/ShortDrama/Sources/Domain/UseCases/SubmitCheckInUseCase.swift`、`ios/ShortDrama/Sources/Domain/UseCases/FetchMessagePreviewUseCase.swift`、`ios/ShortDrama/Sources/Domain/UseCases/FetchSystemMessagesUseCase.swift`、`ios/ShortDrama/Sources/Domain/UseCases/FetchInteractionMessagesUseCase.swift`、`ios/ShortDrama/Sources/Data/DTOs/CheckInDTOs.swift`、`ios/ShortDrama/Sources/Data/DTOs/MessageDTOs.swift`、`ios/ShortDrama/Sources/Data/DataSources/CheckInRemoteDataSource.swift`、`ios/ShortDrama/Sources/Data/DataSources/MessageRemoteDataSource.swift`、`ios/ShortDrama/Sources/Data/Repositories/CheckInRepository.swift`、`ios/ShortDrama/Sources/Data/Repositories/MessageRepository.swift`、`ios/ShortDrama/Tests/DataTests/CheckInRemoteDataSourceTests.swift`、`ios/ShortDrama/Tests/DataTests/CheckInRepositoryTests.swift`、`ios/ShortDrama/Tests/DataTests/MessageRemoteDataSourceTests.swift`、`ios/ShortDrama/Tests/DataTests/MessageRepositoryTests.swift`
- **实现内容**：
  1. 先为签到状态、签到日卡片、消息预览、系统消息、互动消息及分页列表定义 Domain Entity、Repository Protocol 与 UseCase，保证 ViewModel 只依赖 UseCase/Protocol。
  2. 再补 DTO、RemoteDataSource、Repository，接入五条 PRD-10 接口，复用当前 `APIClient`、`APIEndpoint`、`PaginationDTO` 与 snake_case 解码方式，不引入新依赖。
  3. 在 endpoint 测试中锁定 `X-Installation-Id` 注入、`page/pageSize` query、preview 的 `204 No Content` 空态，以及互动消息受保护接口的 Authorization 透传前提，避免后续 UI 层重复解析协议。
  4. repository 测试覆盖 DTO 到 Domain 的映射、preview 空态转本地 empty 语义、错误透传与分页实体转换，为首页、菜单、消息页状态机提供稳定输入。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`，确认签到与消息 Data/Domain 契约测试通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Domain/Entities/SignInStatus.swift` | 新增 | 定义签到状态、签到日卡片与展示字段 |
| `ios/ShortDrama/Sources/Domain/Entities/MessagePreview.swift` | 新增 | 定义菜单消息摘要实体 |
| `ios/ShortDrama/Sources/Domain/Entities/SystemMessage.swift` | 新增 | 定义系统消息实体 |
| `ios/ShortDrama/Sources/Domain/Entities/InteractionMessage.swift` | 新增 | 定义互动消息实体 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/CheckInRepositoryProtocol.swift` | 新增 | 声明签到仓储协议 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/MessageRepositoryProtocol.swift` | 新增 | 声明消息仓储协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchCheckInStatusUseCase.swift` | 新增 | 查询签到状态用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/SubmitCheckInUseCase.swift` | 新增 | 提交签到用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchMessagePreviewUseCase.swift` | 新增 | 获取菜单消息预览用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchSystemMessagesUseCase.swift` | 新增 | 获取系统消息列表用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchInteractionMessagesUseCase.swift` | 新增 | 获取互动消息列表用例 |
| `ios/ShortDrama/Sources/Data/DTOs/CheckInDTOs.swift` | 新增 | 签到 DTO 与映射 |
| `ios/ShortDrama/Sources/Data/DTOs/MessageDTOs.swift` | 新增 | 消息 DTO、分页响应与映射 |
| `ios/ShortDrama/Sources/Data/DataSources/CheckInRemoteDataSource.swift` | 新增 | 封装签到状态查询与提交接口 |
| `ios/ShortDrama/Sources/Data/DataSources/MessageRemoteDataSource.swift` | 新增 | 封装 preview/system/interactions 接口 |
| `ios/ShortDrama/Sources/Data/Repositories/CheckInRepository.swift` | 新增 | 签到仓储实现 |
| `ios/ShortDrama/Sources/Data/Repositories/MessageRepository.swift` | 新增 | 消息仓储实现 |
| `ios/ShortDrama/Tests/DataTests/CheckInRemoteDataSourceTests.swift` | 新增 | 覆盖签到 endpoint header/body/response 契约 |
| `ios/ShortDrama/Tests/DataTests/CheckInRepositoryTests.swift` | 新增 | 覆盖签到 DTO → Entity 映射 |
| `ios/ShortDrama/Tests/DataTests/MessageRemoteDataSourceTests.swift` | 新增 | 覆盖消息 endpoint query/空态/响应解码 |
| `ios/ShortDrama/Tests/DataTests/MessageRepositoryTests.swift` | 新增 | 覆盖消息仓储映射与错误透传 |

### Step 2：实现 installationId 存储、首页签到浮层状态机与宿主接线

- **关联测试**：T-02、T-03
- **目标文件**：`ios/ShortDrama/Sources/Core/Storage/InstallationIdStore.swift`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`、`ios/ShortDrama/Sources/Features/Home/Views/CheckInPopupView.swift`、`ios/ShortDrama/Tests/DomainTests/InstallationIdStoreTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/HomeViewModelTests.swift`
- **实现内容**：
  1. 先参考现有 `PlaybackSessionStore` 测试模式，为 `InstallationIdStore` 写 Keychain 读写测试，锁定“首次生成、后续复用、异常透传”行为，并补一个轻量本地关闭态存储方案，按 `checkin.popup.dismissed.<serverDate>` 记录服务端业务日。
  2. 扩展 `HomeViewModel`，在保留现有首页 feed/comment sheet 逻辑的前提下，新增冷启动签到检查入口、弹层可见状态、提交签到 loading/error/success 状态，以及与 comment sheet 冲突时的放弃展示策略。
  3. 修改 `HomeView`，把 `CheckInPopupView` 作为首页宿主层的 overlay 接入，保持首页主内容、评论 sheet 与签到弹层相互隔离；关闭与签到成功都只更新浮层状态，不重新触发 feed 加载。
  4. 测试覆盖 `should_show_popup=false`、同一 `server_date` 已关闭、已签到不弹、签到成功立即变更为今日已签到、失败保持可重试等场景，确保首页不会因签到失败白屏。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`，确认 installationId 与 `HomeViewModel` 相关测试通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Core/Storage/InstallationIdStore.swift` | 新增 | 复用 Keychain 模式保存安装级 UUID |
| `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` | 修改 | 新增签到弹层状态、关闭态与签到提交逻辑 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 在首页接入签到浮层宿主与事件回调 |
| `ios/ShortDrama/Sources/Features/Home/Views/CheckInPopupView.swift` | 新增 | 渲染签到浮层、7 日签到板与主操作按钮 |
| `ios/ShortDrama/Tests/DomainTests/InstallationIdStoreTests.swift` | 新增 | 覆盖 installationId 生成与复用 |
| `ios/ShortDrama/Tests/ViewModelTests/HomeViewModelTests.swift` | 修改 | 覆盖首页签到展示、关闭、提交与冲突降级状态机 |

### Step 3：接通菜单消息预览与消息中心页面状态机

- **关联测试**：T-04、T-05
- **目标文件**：`ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift`、`ios/ShortDrama/Sources/Features/MenuPanel/Views/Components/MenuMessagePreviewView.swift`、`ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelView.swift`、`ios/ShortDrama/Sources/Features/Messages/ViewModels/MessageCenterViewModel.swift`、`ios/ShortDrama/Sources/Features/Messages/Views/MessageCenterView.swift`、`ios/ShortDrama/Tests/ViewModelTests/MenuPanelViewModelTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/MessageCenterViewModelTests.swift`
- **实现内容**：
  1. 先补 `MenuPanelViewModelTests`，把当前仅覆盖 recently viewed 的状态机扩展为“最近在看 + 消息预览”双状态，锁定 preview 成功、204 空态、失败降级与短时间重复打开菜单的缓存复用行为。
  2. 修改 `MenuPanelViewModel`、`MenuMessagePreviewView` 与 `MenuPanelView`，把“我的消息”模块从静态文案替换为远程 preview state，同时保证请求失败时入口仍可点击，且不影响 recently viewed 区块。
  3. 新增 `MessageCenterViewModel` 与 `MessageCenterView`，按当前 `AuthStore` 真实状态建模系统消息区与互动消息区：匿名时不请求互动接口，直接展示登录门槛；登录时加载互动列表，并将错误限制在互动区内。
  4. 测试覆盖系统消息空态、互动消息空态、互动消息失败局部重试、登录成功回流后重新加载互动消息，确保消息页符合 design-ios 中的双分区和错误隔离约束。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`，确认菜单 preview 与消息中心状态机测试通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift` | 修改 | 新增消息预览状态、缓存与加载逻辑 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/Components/MenuMessagePreviewView.swift` | 修改 | 绑定 preview 成功/空态/降级文案 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelView.swift` | 修改 | 承接菜单消息预览展示 |
| `ios/ShortDrama/Sources/Features/Messages/ViewModels/MessageCenterViewModel.swift` | 新增 | 管理系统消息、互动消息与登录门槛状态 |
| `ios/ShortDrama/Sources/Features/Messages/Views/MessageCenterView.swift` | 新增 | 承接消息中心页布局与重试交互 |
| `ios/ShortDrama/Tests/ViewModelTests/MenuPanelViewModelTests.swift` | 修改 | 覆盖 preview 状态、降级与缓存复用 |
| `ios/ShortDrama/Tests/ViewModelTests/MessageCenterViewModelTests.swift` | 新增 | 覆盖匿名/登录双分区与局部错误状态 |

### Step 4：完成消息页路由、菜单导航与登录承接回流

- **关联测试**：T-05、T-06
- **目标文件**：`ios/ShortDrama/Sources/App/AppRoute.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`、`ios/ShortDrama/Sources/App/AppShellView.swift`、`ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift`、`ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift`、`ios/ShortDrama/Sources/Features/Auth/Views/LoginView.swift`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`
- **实现内容**：
  1. 先在 `NavigationRouterTests` 中锁定新路由语义：`AppRoute.messages` 归属 home tab，菜单点击消息入口时保持“先关菜单再导航”，不再继续使用 `.menuPlaceholder(kind: .messages)`。
  2. 修改 `AppRoute`、`NavigationRouter`、`TabNavigationHostView` 与 `MenuPanelContainerView`，注册真实 `MessageCenterView` 目标页，并保证从消息页返回后仍回到首页根上下文且菜单保持关闭。
  3. 扩展 `LoginInterceptionContext.Source` 增加 `.messagesEntry`，在消息页登录按钮与 `LoginView` 提示文案中复用现有登录承接链路，固定 `returnRoute: .messages`，登录成功后停留消息页并刷新互动分区。
  4. 在 `AppShellView` 与消息页之间保持现有 fullScreenCover 登录流，不新增第二套弹层机制；测试覆盖成功回流、取消登录、匿名继续停留系统消息页的行为。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`，确认路由与登录回流测试通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增 `messages` 路由并替换菜单消息占位语义 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 管理菜单关闭后消息导航与登录回流 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 注册 `MessageCenterView` 导航目标 |
| `ios/ShortDrama/Sources/App/AppShellView.swift` | 修改 | 保持消息页登录承接与全局注入协同 |
| `ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift` | 修改 | 新增 `.messagesEntry` 来源与 returnRoute 语义 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift` | 修改 | 消息入口改为导航真实消息页 |
| `ios/ShortDrama/Sources/Features/Auth/Views/LoginView.swift` | 修改 | 增加消息页登录文案分支 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 覆盖消息页导航、返回与登录回流路径 |

### Step 5：执行 XcodeGen、测试、构建与 Lint 回归收口

- **关联测试**：T-07
- **目标文件**：`ios/project.yml`、`docs/specs/2026-07-29-prd-10-signin-messages/plan-ios.md`
- **实现内容**：
  1. 检查新增 `Features/Messages`、签到相关 Domain/Data/Core/Tests 文件是否能被当前 XcodeGen 通配路径自动纳入；如需补充工程配置，仅修改 `ios/project.yml`，不直接改 `.xcodeproj/project.pbxproj`。
  2. 按 `ios/CLAUDE.md` 的真实命令执行收口：先 `xcodegen generate`，再完整 `xcodebuild test`、`xcodebuild build`、`swiftlint lint`，确保 PRD-10 变更与现有首页、菜单、登录、评论路径可共存。
  3. 若回归暴露 Sendable、主线程、导航或命名问题，在同一轮内回补对应测试与实现，保持计划中的轻量 TDD 顺序不被跳过。
  4. 将本计划作为 coding 阶段唯一 iOS 落地基线，避免开发过程中再把消息入口做回 placeholder 或绕过现有 auth/router 架构。
- **验证方式**：
  - 运行 `cd ios && xcodegen generate`
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - 运行 `cd ios && swiftlint lint`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/project.yml` | 视情况修改 | 确保新增签到与消息源码、测试文件纳入工程 |
| `docs/specs/2026-07-29-prd-10-signin-messages/plan-ios.md` | 新增 | 固化 PRD-10 iOS 实现步骤、测试矩阵与验收基线 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5
```

## 验证总览

- [ ] 所有测试通过（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [ ] Build 成功（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [ ] 无新增 lint 错误（`cd ios && swiftlint lint`）
- [ ] 新增源码后已执行 `cd ios && xcodegen generate`
- [ ] 首页签到浮层、菜单消息预览、消息中心双分区与登录回流均有对应单元测试覆盖
- [ ] `X-Installation-Id`、`page/pageSize`、preview 204 空态等 API contract 已通过数据层测试锁定

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Core/Storage/InstallationIdStore.swift` | 新增 | 安装级 UUID 存储 |
| `ios/ShortDrama/Sources/Domain/Entities/SignInStatus.swift` | 新增 | 签到状态实体 |
| `ios/ShortDrama/Sources/Domain/Entities/MessagePreview.swift` | 新增 | 菜单消息摘要实体 |
| `ios/ShortDrama/Sources/Domain/Entities/SystemMessage.swift` | 新增 | 系统消息实体 |
| `ios/ShortDrama/Sources/Domain/Entities/InteractionMessage.swift` | 新增 | 互动消息实体 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/CheckInRepositoryProtocol.swift` | 新增 | 签到仓储协议 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/MessageRepositoryProtocol.swift` | 新增 | 消息仓储协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchCheckInStatusUseCase.swift` | 新增 | 查询签到状态用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/SubmitCheckInUseCase.swift` | 新增 | 提交签到用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchMessagePreviewUseCase.swift` | 新增 | 查询菜单消息预览用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchSystemMessagesUseCase.swift` | 新增 | 查询系统消息列表用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchInteractionMessagesUseCase.swift` | 新增 | 查询互动消息列表用例 |
| `ios/ShortDrama/Sources/Data/DTOs/CheckInDTOs.swift` | 新增 | 签到 DTO |
| `ios/ShortDrama/Sources/Data/DTOs/MessageDTOs.swift` | 新增 | 消息 DTO 与分页响应 |
| `ios/ShortDrama/Sources/Data/DataSources/CheckInRemoteDataSource.swift` | 新增 | 签到远端数据源 |
| `ios/ShortDrama/Sources/Data/DataSources/MessageRemoteDataSource.swift` | 新增 | 消息远端数据源 |
| `ios/ShortDrama/Sources/Data/Repositories/CheckInRepository.swift` | 新增 | 签到仓储实现 |
| `ios/ShortDrama/Sources/Data/Repositories/MessageRepository.swift` | 新增 | 消息仓储实现 |
| `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` | 修改 | 接入签到浮层状态机 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 首页签到宿主接线 |
| `ios/ShortDrama/Sources/Features/Home/Views/CheckInPopupView.swift` | 新增 | 签到浮层 UI |
| `ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift` | 修改 | 接入消息预览状态 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelView.swift` | 修改 | 菜单承接消息预览 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/Components/MenuMessagePreviewView.swift` | 修改 | 展示真实消息摘要/空态/降级文案 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift` | 修改 | 消息入口导航到真实消息页 |
| `ios/ShortDrama/Sources/Features/Messages/ViewModels/MessageCenterViewModel.swift` | 新增 | 消息中心状态机 |
| `ios/ShortDrama/Sources/Features/Messages/Views/MessageCenterView.swift` | 新增 | 消息中心页面 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增消息页路由 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 先关菜单再导航消息页 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 注册消息页导航目标 |
| `ios/ShortDrama/Sources/App/AppShellView.swift` | 修改 | 协调消息页登录承接 |
| `ios/ShortDrama/Sources/Domain/Entities/LoginInterceptionContext.swift` | 修改 | 增加消息页登录来源 |
| `ios/ShortDrama/Sources/Features/Auth/Views/LoginView.swift` | 修改 | 增加消息页登录文案 |
| `ios/project.yml` | 视情况修改 | 确保新增文件纳入工程 |
| `ios/ShortDrama/Tests/DataTests/CheckInRemoteDataSourceTests.swift` | 新增 | 签到远端数据源测试 |
| `ios/ShortDrama/Tests/DataTests/CheckInRepositoryTests.swift` | 新增 | 签到仓储测试 |
| `ios/ShortDrama/Tests/DataTests/MessageRemoteDataSourceTests.swift` | 新增 | 消息远端数据源测试 |
| `ios/ShortDrama/Tests/DataTests/MessageRepositoryTests.swift` | 新增 | 消息仓储测试 |
| `ios/ShortDrama/Tests/DomainTests/InstallationIdStoreTests.swift` | 新增 | installationId 存储测试 |
| `ios/ShortDrama/Tests/ViewModelTests/HomeViewModelTests.swift` | 修改 | 首页签到状态机测试 |
| `ios/ShortDrama/Tests/ViewModelTests/MenuPanelViewModelTests.swift` | 修改 | 菜单消息预览状态测试 |
| `ios/ShortDrama/Tests/ViewModelTests/MessageCenterViewModelTests.swift` | 新增 | 消息中心双分区测试 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 消息页导航与登录回流测试 |
