# 实现计划：Android — PRD-09 评论系统

> 创建日期：2026-07-29
> 对应技术方案：design-android.md
> 对应需求：spec.md

## 概述

本计划聚焦 Android 评论抽屉的最小可交付链路：补齐 comments API/DTO/repository、`CommentSheetViewModel` 状态机、首页与播放器内 `ModalBottomSheet` 接线，以及评论登录恢复上下文。以下 Gradle 命令均在 `android/` 目录执行，并按轻量 TDD 顺序推进：先定义测试场景，再实现功能，最后执行 `test`、`assembleDebug`、`detekt` 回归。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | comments repository 正确透传查询并完成 DTO→Domain 映射 | `dramaId=drama-1`，`page=1`，`pageSize=20`，`sort=latest/hot`，后端返回 snake_case 评论列表 | `CommentRepository` 返回正确的评论项、分页信息与排序参数；错误分支维持 `ApiResult` 语义 | 单元测试 | P0 |
| T-02 | 评论写接口 DTO 与局部更新结果映射正确 | 创建评论响应、点赞 toggle 响应、服务端错误响应 | `createComment()` 返回完整评论实体，`toggleLike()` 返回 `commentId/liked/likeCount`，异常不被吞掉 | 单元测试 | P0 |
| T-03 | `CommentSheetViewModel` 首屏加载状态机正确切换 | 首次打开评论抽屉，分别返回内容、空列表、失败 | `Idle -> Loading -> Content/Empty/Error`，`totalCount`、`hasNextPage` 与分页状态同步 | 单元测试 | P0 |
| T-04 | `CommentSheetViewModel` 支持排序切换与分页追加 | 已有第一页评论，切换 `latest/hot` 或触底加载下一页 | 切换排序时重置列表并拉取第一页；追加成功只拼接下一页，追加失败只显示 footer 错误 | 单元测试 | P0 |
| T-05 | 已登录发送评论成功时更新列表顶部并清空输入框 | 已登录、输入 1~500 字合法评论、创建评论成功 | `isSubmitting` 正确开关，新评论插入顶部，`inputText` 清空，`totalCount + 1` | 单元测试 | P0 |
| T-06 | 未登录写操作触发 `CommentLoginContext`，且登录后只恢复上下文不自动重放 | 未登录发送评论或点赞评论，提供来源页 `home/player` 与可选 `commentId` | 发出 `RequireLogin(CommentLoginContext)` effect，携带 `source/dramaId/returnRoute/action/commentId?`；恢复后重新打开评论抽屉，但不自动再次发送/点赞 | 单元测试 | P0 |
| T-07 | 首页与播放器评论入口能接通宿主级评论抽屉 | 首页卡片点击评论、播放器点击评论芯片、关闭抽屉、重新打开 | Home/Player 都能维护单一活动 `dramaId` 评论上下文，`ModalBottomSheet` 打开/关闭与宿主状态一致 | 单元测试 | P1 |
| T-08 | 评论点赞单项锁与最终回归稳定 | 已登录连续点击同一条评论点赞；全部实现完成后执行全量回归 | 同一 `commentId` 只保留单项 in-flight 锁；`./gradlew test`、`./gradlew assembleDebug`、`./gradlew detekt` 均通过 | 单元测试 / 构建回归 | P0 |

## 实现步骤

<!-- 每个步骤遵循：定义测试 → 写实现 → 验证 → 补充测试 → 记录变更 -->

### Step 1：搭建 comments API、DTO 与 repository 基线

- **关联测试**：T-01、T-02
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/dto/CommentDto.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/datasource/CommentRemoteDataSource.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/repository/CommentRepositoryImpl.kt`
- **实现内容**：
  1. 新增评论相关 Domain Model、Repository Interface、UseCase 基线，明确 `Comment`、`CommentSort`、`CommentPage`、`ToggleCommentLikeResult` 等领域对象。
  2. 在 `ApiService` 中增加 `GET /dramas/{id}/comments`、`POST /dramas/{id}/comments`、`POST /dramas/{id}/comments/{commentId}/like` 三个 endpoint，并保持 query/body 设计与 design-android.md 一致。
  3. 新增评论 DTO、列表响应 DTO、创建评论请求 DTO、点赞响应 DTO，按 snake_case 契约完成到 Domain 的映射。
  4. 新增 `CommentRemoteDataSource` 与 `CommentRepositoryImpl`，沿用现有 `ApiResult` 封装模式，补充 `RepositoryModule` 注入。
  5. 先写 `CommentRepositoryImplTest`，覆盖 query 透传、字段映射、错误透传，再按测试实现数据层代码。
- **验证方式**：
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.data.repository.CommentRepositoryImplTest"` 确认 T-01、T-02 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增 comments 三条 Retrofit API |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/CommentDto.kt` | 新增 | 评论项 DTO 与用户摘要 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/CommentListResponseDto.kt` | 新增 | 评论列表与分页 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/CreateCommentRequestDto.kt` | 新增 | 发评论请求 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/ToggleCommentLikeResponseDto.kt` | 新增 | 点赞 toggle 响应 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/CommentRemoteDataSource.kt` | 新增 | 评论远端请求封装 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/CommentRepositoryImpl.kt` | 新增 | 评论仓储实现 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/Comment.kt` | 新增 | 评论领域实体 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/CommentQuery.kt` | 新增 | 排序、分页查询模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/ToggleCommentLikeResult.kt` | 新增 | 点赞局部更新模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/CommentRepository.kt` | 新增 | 评论仓储接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetDramaCommentsUseCase.kt` | 新增 | 评论列表用例 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/CreateCommentUseCase.kt` | 新增 | 发评论用例 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/ToggleCommentLikeUseCase.kt` | 新增 | 点赞 toggle 用例 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 注入 `CommentRepository` |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/CommentRepositoryImplTest.kt` | 新增 | 覆盖 DTO 映射与错误透传 |

### Step 2：实现 `CommentSheetViewModel` 的加载、排序与分页状态机

- **关联测试**：T-03、T-04
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentUiState.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentSheetViewModel.kt`
- **实现内容**：
  1. 先编写 `CommentSheetViewModelTest`，覆盖首次打开评论抽屉的 `Loading/Content/Empty/Error` 切换，以及 `latest/hot` 排序切换、追加分页成功/失败场景。
  2. 定义 `CommentUiState`、`CommentListState`、`CommentSource`、`CommentUiModel` 等 Presentation 状态，明确 `comments`、`selectedSort`、`totalCount`、`hasNextPage`、`isAppending`、`appendErrorMessage` 等字段。
  3. 在 `CommentSheetViewModel` 中实现 `open(dramaId, source)`、`retry()`、`selectSort(sort)`、`loadNextPage()` 等行为，保证切换 `dramaId` 或 `sort` 时重置列表和输入状态。
  4. 分页追加只修改 footer 状态，不把已有内容态回退为整页 loading；失败时保留旧数据并暴露重试入口。
  5. 保证所有状态转换都通过 StateFlow 收敛，便于 Home/Player 两个宿主复用同一套评论抽屉逻辑。
- **验证方式**：
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.feature.comments.viewmodel.CommentSheetViewModelTest"` 确认 T-03、T-04 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentUiState.kt` | 新增 | 评论抽屉 UI 状态、列表状态与 effect 定义 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentSheetViewModel.kt` | 新增 | 评论列表加载、排序切换、分页追加状态机 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/comments/model/CommentUiModel.kt` | 新增 | 评论列表 UI 模型与映射 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentSheetViewModelTest.kt` | 新增 | 覆盖加载、空态、错误态、排序与分页 |

### Step 3：补齐发送评论、点赞与 `CommentLoginContext` 恢复语义

- **关联测试**：T-05、T-06、T-08
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentSheetViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/comments/model/CommentLoginContext.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthSessionProvider.kt`
- **实现内容**：
  1. 在 `CommentSheetViewModelTest` 中先补充发送成功、空白输入拦截、未登录发送、已登录点赞、未登录点赞、重复点赞单项锁等测试。
  2. 为评论模块定义结构化 `CommentLoginContext` / `PendingCommentAction`，字段至少包含 `source`、`dramaId`、`returnRoute`、`action`、`commentId?`，并提供从 `home` / `player` 生成 `returnRoute` 的 helper。
  3. 实现 `submitComment()`：本地先做 `trim()` 与长度校验，再根据登录态决定走 `CreateCommentUseCase` 或发出 `RequireLogin(CommentLoginContext)`；成功时插入顶部、清空输入、递增总数，失败时保留输入。
  4. 实现 `toggleLike(commentId)`：已登录时调用 `ToggleCommentLikeUseCase` 并仅更新目标评论项；未登录时发出 `RequireLogin(CommentLoginContext)`；对单条评论使用 `likingCommentIds` 加锁，避免乱序覆盖。
  5. 严格遵守 spec 的恢复语义：登录成功后只恢复来源页和评论抽屉打开状态，不自动重放原发送/点赞动作。
- **验证方式**：
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.feature.comments.viewmodel.CommentSheetViewModelTest"` 确认 T-05、T-06、T-08 的状态机场景通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentSheetViewModel.kt` | 修改 | 增加发送、点赞、未登录拦截与单项锁 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentUiState.kt` | 修改 | 增加 `CommentEffect`、`likingCommentIds`、提交态字段 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/comments/model/CommentLoginContext.kt` | 新增 | 评论登录恢复上下文与 pending action 模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/CreateCommentUseCase.kt` | 修改 | 串起创建评论逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/ToggleCommentLikeUseCase.kt` | 修改 | 串起点赞 toggle 逻辑 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentSheetViewModelTest.kt` | 修改 | 补齐提交、点赞、登录恢复语义测试 |

### Step 4：接通 Home/Player 宿主与 `ModalBottomSheet` 评论抽屉

- **关联测试**：T-07
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/components/PlayerComponents.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt`
- **实现内容**：
  1. 新增 `CommentBottomSheet` 与评论子组件，把 `CommentSheetViewModel` 暴露的状态与事件完整渲染为列表、空态、错误态、输入区和排序入口。
  2. 在首页卡片 action row 增加评论入口，并由 Home 宿主维护当前活动 `dramaId` 的评论抽屉状态；同一时刻只允许一个评论上下文。
  3. 在播放器中把 `AssistChip(onClick = {})` 替换为真实 `onOpenComments` 回调，在 `PlayerUiState` / `PlayerViewModel` 中增加评论抽屉显隐与当前评论上下文。
  4. 在 `PlayerScreen` 内新增第三个 `ModalBottomSheet`，与现有选集、倍速 sheet 并列但互不干扰；关闭评论抽屉时不影响播放主状态。
  5. 宿主消费 `RequireLogin(CommentLoginContext)` effect：先按当前登录承接能力保存恢复上下文，登录完成后回到来源页并重新打开评论抽屉，不自动重放原动作。
- **验证方式**：
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.feature.player.viewmodel.PlayerViewModelTest"` 确认播放器评论入口与宿主状态切换通过
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.feature.home.ui.HomeScreenTest"` 确认首页评论入口与宿主辅助逻辑通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/comments/ui/CommentBottomSheet.kt` | 新增 | 评论抽屉根组件 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/comments/ui/CommentComponents.kt` | 新增 | 评论列表项、输入区、状态视图等子组件 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 首页卡片评论入口与评论抽屉宿主状态 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/components/PlayerComponents.kt` | 修改 | 评论按钮改为真实回调 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt` | 修改 | 新增评论 `ModalBottomSheet` 与 effect 消费 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerUiState.kt` | 修改 | 增加评论抽屉显示态和上下文字段 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` | 修改 | 打开/关闭评论抽屉与恢复语义协调 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModelTest.kt` | 修改 | 增加评论入口与宿主协调测试 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/home/ui/HomeScreenTest.kt` | 修改 | 增加首页评论入口相关测试 |

### Step 5：补齐回归测试并执行 Gradle 全量验证

- **关联测试**：T-08
- **目标文件**：`android/app/src/test/java/com/djs66256/short_drama/data/repository/CommentRepositoryImplTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentSheetViewModelTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModelTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/home/ui/HomeScreenTest.kt`
- **实现内容**：
  1. 复盘前四步的测试缺口，补齐代表性边界场景：不同 `dramaId` 切换重置、`hot` 排序透传、匿名点赞不落本地 optimistic 更新、评论发送失败保留输入、播放器评论抽屉关闭后重开。
  2. 执行评论相关定向测试，确保 repository、ViewModel、Home/Player 宿主行为全部收敛。
  3. 执行 Android 端全量单元测试、Debug 构建和 detekt，作为 PRD-09 Android 评论实现的最终回归门槛。
  4. 若全量回归暴露新的命名、可见性或 detekt 问题，回到对应步骤最小化修正，再重复本步命令直至稳定。
- **验证方式**：
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.data.repository.CommentRepositoryImplTest"`
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.feature.comments.viewmodel.CommentSheetViewModelTest"`
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.feature.player.viewmodel.PlayerViewModelTest"`
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.feature.home.ui.HomeScreenTest"`
  - 运行 `./gradlew test`
  - 运行 `./gradlew assembleDebug`
  - 运行 `./gradlew detekt`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/CommentRepositoryImplTest.kt` | 修改 | 补齐数据层边界场景 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentSheetViewModelTest.kt` | 修改 | 补齐状态机边界与恢复语义 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModelTest.kt` | 修改 | 补齐播放器评论抽屉回归 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/home/ui/HomeScreenTest.kt` | 修改 | 补齐首页评论入口回归 |

## 依赖关系

```
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5
```

## 验证总览

- [ ] 所有测试通过（`./gradlew test`）
- [ ] Build 成功（`./gradlew assembleDebug`）
- [ ] 无新增 lint 错误（`./gradlew detekt`）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增评论 API 契约 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 注入评论仓储 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/CommentDto.kt` | 新增 | 评论 DTO 与用户摘要 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/CommentListResponseDto.kt` | 新增 | 评论列表响应 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/CreateCommentRequestDto.kt` | 新增 | 发评论请求 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/ToggleCommentLikeResponseDto.kt` | 新增 | 点赞响应 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/CommentRemoteDataSource.kt` | 新增 | 评论远端数据源 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/CommentRepositoryImpl.kt` | 新增 | 评论仓储实现 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/Comment.kt` | 新增 | 评论领域实体 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/CommentQuery.kt` | 新增 | 评论查询与排序模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/ToggleCommentLikeResult.kt` | 新增 | 点赞结果模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/CommentRepository.kt` | 新增 | 评论仓储接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetDramaCommentsUseCase.kt` | 新增 | 获取评论列表用例 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/CreateCommentUseCase.kt` | 新增/修改 | 创建评论用例 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/ToggleCommentLikeUseCase.kt` | 新增/修改 | 点赞 toggle 用例 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/comments/model/CommentUiModel.kt` | 新增 | 评论 UI 模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/comments/model/CommentLoginContext.kt` | 新增 | 评论登录恢复上下文 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentUiState.kt` | 新增/修改 | 评论状态与 effect |
| `android/app/src/main/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentSheetViewModel.kt` | 新增 | 评论抽屉状态机 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/comments/ui/CommentBottomSheet.kt` | 新增 | 评论抽屉 UI 根组件 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/comments/ui/CommentComponents.kt` | 新增 | 评论子组件 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 首页评论入口与宿主逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/components/PlayerComponents.kt` | 修改 | 评论按钮接线 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt` | 修改 | 评论 `ModalBottomSheet` 接线 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerUiState.kt` | 修改 | 评论宿主状态 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` | 修改 | 评论入口与恢复协调 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/CommentRepositoryImplTest.kt` | 新增/修改 | 数据层测试 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentSheetViewModelTest.kt` | 新增/修改 | 评论状态机测试 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModelTest.kt` | 修改 | 播放器评论入口测试 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/home/ui/HomeScreenTest.kt` | 修改 | 首页评论入口测试 |