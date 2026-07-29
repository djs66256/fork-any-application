# 实现计划：iOS — PRD-09 评论系统

> 创建日期：2026-07-29
> 对应技术方案：design-ios.md
> 对应需求：spec.md

## 概述

本计划聚焦 iOS 端评论能力落地：先补齐 Comments 的 Data/Domain 链路与单元测试，再完成 `CommentSheetViewModel` 状态机、Home/Player 页面内 sheet 接线、登录拦截上下文恢复，以及最后的 XcodeGen / test / build / lint 回归验证。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 评论接口 DTO 与 Entity 映射正确 | `GET /api/dramas/{id}/comments` 的 snake_case 响应、分页信息、`sort=latest/hot` 查询参数 | `Comment`、`CommentQuery`、分页字段映射正确，query/body 构造符合契约 | 单元测试 | P0 |
| T-02 | 评论仓库透传创建评论与点赞切换 | 合法 `dramaId`、`content`、`commentId` | Repository 正确调用 RemoteDataSource，返回 Domain Entity / LikeResult | 单元测试 | P0 |
| T-03 | 评论抽屉首次加载成功与空态切换 | `loadIfNeeded()` 返回非空列表或空数组 | `listState` 分别进入 `content` / `empty`，`totalCount` 使用 `pagination.total` | 单元测试 | P0 |
| T-04 | 评论抽屉失败、重试、切换排序、分页状态正确 | 首屏 500、retry 成功、`selectSort(.hot)`、`loadMoreIfNeeded()` | `error -> loading -> content` 正确切换；排序重置第一页；分页只追加不覆盖旧数据 | 单元测试 | P0 |
| T-05 | 已登录发表评论成功 | 合法输入文本、已登录状态 | 新评论插入顶部、输入框清空、`totalCount + 1`、不整页重刷 | 单元测试 | P0 |
| T-06 | 未登录发表评论触发登录拦截上下文 | 合法输入文本、未登录状态、来源为 Home/Player | 不发起写请求，抛出 `requireLogin(CommentLoginContext)`，保留来源页与 `dramaId` | 单元测试 | P0 |
| T-07 | 未登录点赞与已登录点赞切换正确 | 点击同一评论点赞，分别覆盖已登录/未登录 | 已登录时仅局部更新 `liked/likeCount`；未登录时发出 `requireLogin` 且不改本地列表 | 单元测试 | P0 |
| T-08 | Home 与 Player 评论入口接线正确 | 点击首页卡片评论按钮、播放器评论按钮 | 打开对应 `.sheet`，创建正确 `CommentSheetViewModel`，关闭后不影响主页面状态 | 单元测试 | P1 |
| T-09 | 登录恢复只恢复评论上下文不重放写操作 | 已保存 `CommentLoginContext`，登录成功或取消登录 | 仅恢复来源页与评论抽屉打开状态，不自动提交评论或自动点赞 | 单元测试 | P1 |
| T-10 | iOS 工程回归可通过生成、测试、构建、lint | 新增 Comments 源文件与测试文件 | `xcodegen generate`、`xcodebuild test`、`xcodebuild build`、`swiftlint lint` 均可执行通过 | 单元测试 | P0 |

## 实现步骤

<!-- 每个步骤遵循：定义测试 → 写实现 → 验证 → 补充测试 → 记录变更 -->

### Step 1：补齐评论 Data / Domain 基础链路

- **关联测试**：T-01、T-02
- **目标文件**：`ios/ShortDrama/Sources/Domain/Entities/Comment.swift`、`ios/ShortDrama/Sources/Domain/Entities/CommentQuery.swift`、`ios/ShortDrama/Sources/Domain/Entities/ToggleCommentLikeResult.swift`、`ios/ShortDrama/Sources/Domain/RepositoryProtocols/CommentRepositoryProtocol.swift`、`ios/ShortDrama/Sources/Domain/UseCases/FetchDramaCommentsUseCase.swift`、`ios/ShortDrama/Sources/Domain/UseCases/CreateCommentUseCase.swift`、`ios/ShortDrama/Sources/Domain/UseCases/ToggleCommentLikeUseCase.swift`、`ios/ShortDrama/Sources/Data/DTOs/CommentDTO.swift`、`ios/ShortDrama/Sources/Data/DTOs/CommentListResponseDTO.swift`、`ios/ShortDrama/Sources/Data/DTOs/CreateCommentRequestDTO.swift`、`ios/ShortDrama/Sources/Data/DTOs/ToggleCommentLikeResponseDTO.swift`、`ios/ShortDrama/Sources/Data/DataSources/CommentRemoteDataSource.swift`、`ios/ShortDrama/Sources/Data/Repositories/CommentRepository.swift`
- **实现内容**：
  1. 先为评论列表、发表评论、点赞切换定义 Domain Entity、Query、Repository Protocol 与 UseCase，保持 `Core → Domain → Data → Presentation` 依赖方向。
  2. 再补 DTO、RemoteDataSource、Repository，实现 `GET /api/dramas/{id}/comments`、`POST /api/dramas/{id}/comments`、`POST /api/dramas/{id}/comments/{commentId}/like` 三条链路。
  3. 统一复用现有 `APIClient` 与 snake_case 解码，不新增网络库，也不硬编码环境地址。
  4. 测试先覆盖 query/body 组装、DTO decode、Entity 映射、repository 调用透传，再进入上层状态机开发。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj \
  -scheme ShortDrama test \
  -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 Data / Domain 相关测试通过（如本机模拟器不同，按 `ios/CLAUDE.md` 相同格式替换可用目标）。✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Domain/Entities/Comment.swift` | 新增 | 定义评论实体与展示字段 |
| `ios/ShortDrama/Sources/Domain/Entities/CommentQuery.swift` | 新增 | 定义分页与排序查询参数 |
| `ios/ShortDrama/Sources/Domain/Entities/ToggleCommentLikeResult.swift` | 新增 | 定义点赞切换结果实体 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/CommentRepositoryProtocol.swift` | 新增 | 声明评论仓库协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchDramaCommentsUseCase.swift` | 新增 | 获取评论列表用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/CreateCommentUseCase.swift` | 新增 | 发表评论用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/ToggleCommentLikeUseCase.swift` | 新增 | 点赞切换用例 |
| `ios/ShortDrama/Sources/Data/DTOs/CommentDTO.swift` | 新增 | 评论 DTO 与映射 |
| `ios/ShortDrama/Sources/Data/DTOs/CommentListResponseDTO.swift` | 新增 | 评论列表响应 DTO |
| `ios/ShortDrama/Sources/Data/DTOs/CreateCommentRequestDTO.swift` | 新增 | 发评论请求 DTO |
| `ios/ShortDrama/Sources/Data/DTOs/ToggleCommentLikeResponseDTO.swift` | 新增 | 点赞切换响应 DTO |
| `ios/ShortDrama/Sources/Data/DataSources/CommentRemoteDataSource.swift` | 新增 | comments API 远端数据源 |
| `ios/ShortDrama/Sources/Data/Repositories/CommentRepository.swift` | 新增 | 评论仓库实现 |
| `ios/ShortDrama/Tests/DataTests/CommentRemoteDataSourceTests.swift` | 新增 | 覆盖 endpoint/query/body/响应解码 |
| `ios/ShortDrama/Tests/DataTests/CommentRepositoryTests.swift` | 新增 | 覆盖 Repository 到 Domain 的映射 |

### Step 2：实现 CommentSheetViewModel 的加载、排序、分页状态机

- **关联测试**：T-03、T-04
- **目标文件**：`ios/ShortDrama/Sources/Features/Comments/ViewModels/CommentSheetViewModel.swift`、`ios/ShortDrama/Sources/Features/Comments/Views/CommentSheetView.swift`、`ios/ShortDrama/Sources/Features/Comments/Views/Components/CommentStateView.swift`、`ios/ShortDrama/Sources/Features/Comments/Views/Components/CommentListView.swift`
- **实现内容**：
  1. 先为 `CommentSheetViewModel` 写首次加载成功、空态、失败、retry、`selectSort(.latest/.hot)`、`loadMoreIfNeeded()` 的状态机测试。
  2. 实现 `listState`、`appendState`、`comments`、`totalCount`、`selectedSort`、分页游标与 request token，保证切换 drama / 排序时能整页重置。
  3. 用 `CommentSheetView`、`CommentStateView`、`CommentListView` 承接 loading / content / empty / error / append error UI，保证评论错误隔离在 sheet 内。
  4. 约束评论总数统一来自 `pagination.total`，分页追加仅影响列表尾部，不覆盖已有评论。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj \
  -scheme ShortDrama test \
  -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 `CommentSheetViewModel` 的加载、排序、分页测试通过。✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/Comments/ViewModels/CommentSheetViewModel.swift` | 新增 | 管理评论抽屉主状态机 |
| `ios/ShortDrama/Sources/Features/Comments/Views/CommentSheetView.swift` | 新增 | 评论抽屉根视图与 `.task` 生命周期 |
| `ios/ShortDrama/Sources/Features/Comments/Views/Components/CommentStateView.swift` | 新增 | 承接加载/空态/错误态 |
| `ios/ShortDrama/Sources/Features/Comments/Views/Components/CommentListView.swift` | 新增 | 评论列表与分页触底逻辑 |
| `ios/ShortDrama/Tests/ViewModelTests/CommentSheetViewModelTests.swift` | 新增 | 覆盖加载、重试、排序、分页状态切换 |

### Step 3：实现评论发送、点赞与登录拦截上下文

- **关联测试**：T-05、T-06、T-07、T-09
- **目标文件**：`ios/ShortDrama/Sources/Features/Comments/ViewModels/CommentSheetViewModel.swift`、`ios/ShortDrama/Sources/Features/Comments/CommentLoginContext.swift`、`ios/ShortDrama/Sources/Features/Comments/Views/Components/CommentComposerView.swift`、`ios/ShortDrama/Sources/Features/Comments/Views/Components/CommentRowView.swift`
- **实现内容**：
  1. 先补发送评论与点赞切换测试，覆盖已登录成功、未登录拦截、输入为空白本地拦截、同一评论点赞中加锁、失败回滚。
  2. 定义 `CommentLoginContext` 与 `PendingCommentAction`，记录 `source`、`dramaId`、动作类型与可选 `commentId`。
  3. 在 `CommentSheetViewModel` 中实现 `submitComment()` 与 `toggleLike(commentID:)`：已登录走 UseCase，未登录只抛出 `requireLogin(CommentLoginContext)`，不自动重放写操作。
  4. 用 `CommentComposerView` 承接输入框、字数、发送按钮 disabled 逻辑；用 `CommentRowView` 承接单项点赞态与局部 loading。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj \
  -scheme ShortDrama test \
  -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认评论发送、点赞、登录拦截上下文测试通过。✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/Comments/ViewModels/CommentSheetViewModel.swift` | 修改 | 增加发送、点赞、登录拦截与回滚逻辑 |
| `ios/ShortDrama/Sources/Features/Comments/CommentLoginContext.swift` | 新增 | 结构化保存评论登录恢复上下文 |
| `ios/ShortDrama/Sources/Features/Comments/Views/Components/CommentComposerView.swift` | 新增 | 评论输入区与发送交互 |
| `ios/ShortDrama/Sources/Features/Comments/Views/Components/CommentRowView.swift` | 新增 | 评论项展示与点赞按钮 |
| `ios/ShortDrama/Tests/ViewModelTests/CommentSheetViewModelTests.swift` | 修改 | 覆盖发送、点赞、登录拦截与本地校验 |

### Step 4：接通 Home / Player 评论入口、sheet 宿主与上下文恢复

- **关联测试**：T-08、T-09
- **目标文件**：`ios/ShortDrama/Sources/Features/Home/Views/Components/HomeDramaCardView.swift`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`、`ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerRightActionBar.swift`、`ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift`、`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift`、`ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift`、`ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift`
- **实现内容**：
  1. 先写宿主协调测试：首页卡片点击评论能打开 sheet；播放器评论按钮从静态占位变为真实入口；关闭评论抽屉不影响首页 Feed / 播放器主内容。
  2. 为 `HomeDramaCardView` 新增 `onComment` 回调，在 `HomeView` 增加单一活动 `dramaId` 的评论 sheet 协调状态。
  3. 为 `PlayerRightActionBar` 增加 `onComment`，在 `PlayerView` 与 `PlayerViewModel` 中承接 `.sheet` 展示、评论 ViewModel 工厂与 require-login effect。
  4. 参考 `RankingLoginContext` / `RankingViewModel` 的结构化 effect 模式，把登录恢复约束落到 Home/Player 宿主层：登录成功只恢复评论上下文，不自动重放“发送/点赞”动作。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj \
  -scheme ShortDrama test \
  -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 Home / Player 评论入口、宿主协调、登录恢复测试通过。✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/Home/Views/Components/HomeDramaCardView.swift` | 修改 | 新增首页评论入口回调 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 承接首页评论 sheet、上下文恢复与关闭逻辑 |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerRightActionBar.swift` | 修改 | 评论按钮从静态视图改为可点击按钮 |
| `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` | 修改 | 承接播放器评论 `.sheet` |
| `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | 修改 | 增加评论入口状态与登录拦截 effect |
| `ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift` | 修改 | 覆盖播放器评论入口与上下文恢复 |
| `ios/ShortDrama/Tests/ViewModelTests/HomeViewModelTests.swift` | 修改/新增 | 覆盖首页评论入口与宿主协调 |

### Step 5：完成 XcodeGen、测试、构建、Lint 回归收口

- **关联测试**：T-10
- **目标文件**：`ios/project.yml`、`ios/ShortDrama/Tests/DataTests/CommentRemoteDataSourceTests.swift`、`ios/ShortDrama/Tests/DataTests/CommentRepositoryTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/CommentSheetViewModelTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/HomeViewModelTests.swift`
- **实现内容**：
  1. 检查新增 Comments 源文件与测试文件是否被 XcodeGen 通配路径正常纳入；如需显式配置，仅修改 `project.yml`，不直接改 `.xcodeproj/project.pbxproj`。
  2. 按 `ios/CLAUDE.md` 命令顺序执行收口计划：先 `xcodegen generate`，再完整 `xcodebuild test`，然后 `xcodebuild build`，最后 `swiftlint lint`。
  3. 若回归中暴露命名、并发、Sendable、SwiftLint 或测试隔离问题，在同一轮内回补对应测试与实现，保持 Comments 变更可持续回归。
- **验证方式**：
  - 运行 `cd ios && xcodegen generate` ✅ 已完成
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj \
  -scheme ShortDrama test \
  -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` ✅ 已完成
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj \
  -scheme ShortDrama build \
  -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` ✅ 已完成
  - 运行 `cd ios && swiftlint lint` ✅ 已完成（命令执行成功，但存在仓库既有 warning，详见 `code-ios-review.md`）
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/project.yml` | 视情况修改 | 确保新增源文件/测试文件纳入工程 |
| `ios/ShortDrama/Tests/DataTests/CommentRemoteDataSourceTests.swift` | 修改 | 根据回归补齐契约边界测试 |
| `ios/ShortDrama/Tests/DataTests/CommentRepositoryTests.swift` | 修改 | 根据回归补齐映射边界测试 |
| `ios/ShortDrama/Tests/ViewModelTests/CommentSheetViewModelTests.swift` | 修改 | 根据回归补齐状态机边界测试 |
| `ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift` | 修改 | 根据回归补齐播放器接线测试 |
| `ios/ShortDrama/Tests/ViewModelTests/HomeViewModelTests.swift` | 修改 | 根据回归补齐首页接线测试 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5
```

## 验证总览

- [x] 所有测试通过（`cd ios && xcodebuild -project ShortDrama.xcodeproj \
  -scheme ShortDrama test \
  -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [x] Build 成功（`cd ios && xcodebuild -project ShortDrama.xcodeproj \
  -scheme ShortDrama build \
  -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [x] 无新增 lint 错误（`cd ios && swiftlint lint`，命令执行成功；仓库仍有既有 warning，未额外引入 comments 相关 lint error）
- [x] 新增文件已纳入工程（`cd ios && xcodegen generate`）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Domain/Entities/Comment.swift` | 新增 | 评论实体 |
| `ios/ShortDrama/Sources/Domain/Entities/CommentQuery.swift` | 新增 | 评论分页与排序实体 |
| `ios/ShortDrama/Sources/Domain/Entities/ToggleCommentLikeResult.swift` | 新增 | 点赞切换结果实体 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/CommentRepositoryProtocol.swift` | 新增 | 评论仓库协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchDramaCommentsUseCase.swift` | 新增 | 获取评论列表用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/CreateCommentUseCase.swift` | 新增 | 发表评论用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/ToggleCommentLikeUseCase.swift` | 新增 | 点赞切换用例 |
| `ios/ShortDrama/Sources/Data/DTOs/CommentDTO.swift` | 新增 | 评论 DTO |
| `ios/ShortDrama/Sources/Data/DTOs/CommentListResponseDTO.swift` | 新增 | 列表响应 DTO |
| `ios/ShortDrama/Sources/Data/DTOs/CreateCommentRequestDTO.swift` | 新增 | 发评论请求 DTO |
| `ios/ShortDrama/Sources/Data/DTOs/ToggleCommentLikeResponseDTO.swift` | 新增 | 点赞切换响应 DTO |
| `ios/ShortDrama/Sources/Data/DataSources/CommentRemoteDataSource.swift` | 新增 | 评论远端数据源 |
| `ios/ShortDrama/Sources/Data/Repositories/CommentRepository.swift` | 新增 | 评论仓库实现 |
| `ios/ShortDrama/Sources/Features/Comments/CommentLoginContext.swift` | 新增 | 评论登录恢复上下文 |
| `ios/ShortDrama/Sources/Features/Comments/ViewModels/CommentSheetViewModel.swift` | 新增 | 评论状态机 |
| `ios/ShortDrama/Sources/Features/Comments/Views/CommentSheetView.swift` | 新增 | 评论抽屉根视图 |
| `ios/ShortDrama/Sources/Features/Comments/Views/Components/CommentComposerView.swift` | 新增 | 评论输入区 |
| `ios/ShortDrama/Sources/Features/Comments/Views/Components/CommentListView.swift` | 新增 | 评论列表组件 |
| `ios/ShortDrama/Sources/Features/Comments/Views/Components/CommentRowView.swift` | 新增 | 评论行组件 |
| `ios/ShortDrama/Sources/Features/Comments/Views/Components/CommentStateView.swift` | 新增 | 评论抽屉状态容器 |
| `ios/ShortDrama/Sources/Features/Home/Views/Components/HomeDramaCardView.swift` | 修改 | 首页评论入口 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 首页评论 sheet 宿主接线 |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerRightActionBar.swift` | 修改 | 播放器评论按钮接线 |
| `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` | 修改 | 播放器评论 sheet 宿主接线 |
| `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | 修改 | 播放器评论状态与 effect |
| `ios/project.yml` | 视情况修改 | XcodeGen 工程配置收口 |
| `ios/ShortDrama/Tests/DataTests/CommentRemoteDataSourceTests.swift` | 新增 | 评论远端数据源测试 |
| `ios/ShortDrama/Tests/DataTests/CommentRepositoryTests.swift` | 新增 | 评论仓库测试 |
| `ios/ShortDrama/Tests/ViewModelTests/CommentSheetViewModelTests.swift` | 新增 | 评论状态机测试 |
| `ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift` | 修改 | 播放器评论入口测试 |
| `ios/ShortDrama/Tests/ViewModelTests/HomeViewModelTests.swift` | 修改/新增 | 首页评论入口与协调测试 |
