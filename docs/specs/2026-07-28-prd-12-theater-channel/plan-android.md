# 实现计划：Android — PRD-12 剧场频道

> 创建日期：2026-07-28
> 对应技术方案：design-android.md
> 对应需求：spec.md

## 概述

本计划聚焦 Android 端将 `theater_graph` 的占位页替换为真实剧场频道页，范围严格收敛到既有 spec/design 已定义的能力：默认 `all` 频道、8 个子频道 Tab、4 个快捷入口、双列 Feed、分页、错误/空态，以及复用现有 search / classification / ranking / new-releases / play 承接链路。

实现采用轻量 TDD：先定义单元测试场景，再按「数据契约 → 状态机 → UI → 导航接入 → 回归验证」推进；不新增开源依赖，验证命令遵循 `android/CLAUDE.md`。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> Android 每个场景都需要有单元测试；新增业务逻辑同步补齐测试。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 剧场 Feed DTO 与 Domain 映射正确 | `TheaterFeedResponseDto(data, pagination)` | 生成 `TheaterPage`，字段映射正确，`hasNextPage` 判断正确，`heat` 为非负整型展示源数据 | 单元测试 | P0 |
| T-02 | 首次进入默认加载 `all` 第一页 | ViewModel 初始化，repository 返回 `channel=all,page=1,pageSize=20` 的成功结果 | `selectedChannel=ALL`、`items` 渲染第一页、`isLoading=false`、`errorMessage=null` | 单元测试 | P0 |
| T-03 | 切换非 `all` 频道展示空态且不报错 | `onChannelSelected(REAL)`，repository 返回空页 | `items=[]`、`selectedChannel=REAL`、`errorMessage=null`、`hasLoadedOnce=true` | 单元测试 | P0 |
| T-04 | 快速切换频道时旧请求不得覆盖新状态 | 第一个频道请求慢、第二个频道请求快 | 最终状态只保留最后一次频道对应的数据和分页信息 | 单元测试 | P0 |
| T-05 | 加载更多成功与失败都符合约束 | 第一页成功；第二页分别返回成功/失败 | 成功时追加列表；失败时保留已有列表并写入 `appendErrorMessage`，不重复触发并发请求 | 单元测试 | P0 |
| T-06 | 快捷入口与播放路由构建正确 | 点击筛选/排行/预约/新剧/卡片播放 | 分别进入 `classification`、`ranking(all,hot)`、`ranking(all,booking)`、`new-releases`、`play(videoId)` | 单元测试 | P0 |
| T-07 | 热度格式化与卡片 UI model 正确 | `heat=23000`、卡片基础字段完整 | UI model 输出中文短数字文案，卡片所需标题/标签/封面占位字段可直接渲染 | 单元测试 | P1 |
| T-08 | 识图入口保持本地占位反馈 | 点击 scan 入口 | 仅发出本地 effect / 提示，不触发网络请求和权限链路 | 单元测试 | P1 |

## 实现步骤

### Step 1：补齐剧场数据契约与映射层

- **关联测试**：T-01
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/dto/TheaterFeedResponseDto.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/TheaterChannel.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/TheaterDrama.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/TheaterPage.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/TheaterQuery.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/repository/DramaRepository.kt`
- **实现内容**：
  1. 先编写 DTO → Domain 映射测试，锁定 `data + pagination`、默认分页参数、`hasNextPage` 和 `heat` 字段语义。
  2. 新增 Theater 相关 domain model 与 query model，保持 Domain 层纯 Kotlin、无 Android 依赖。
  3. 在 `ApiService` 增加 `GET dramas/channel`，在 DataSource / Repository 中沿用现有 `ApiResult` 风格封装。
  4. 保持 `channel` 使用固定枚举，不允许任意字符串穿透到请求层。
- **验证方式**：
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.data.dto.TheaterFeedResponseDtoTest"` 确认 T-01 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增剧场频道接口定义 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt` | 修改 | 新增剧场 Feed 请求封装 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/TheaterFeedResponseDto.kt` | 新增 | 剧场频道响应 DTO 与映射 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt` | 修改 | 实现剧场 Feed 拉取与映射 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/TheaterChannel.kt` | 新增 | 子频道枚举 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/TheaterDrama.kt` | 新增 | 剧场卡片实体 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/TheaterPage.kt` | 新增 | 分页实体 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/TheaterQuery.kt` | 新增 | 查询参数模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/DramaRepository.kt` | 修改 | 暴露 `getTheaterFeed(query)` 接口 |
| `android/app/src/test/java/com/djs66256/short_drama/data/dto/TheaterFeedResponseDtoTest.kt` | 新增 | DTO 映射测试 |

### Step 2：实现剧场首页状态机与首屏/切频道加载

- **关联测试**：T-02、T-03、T-04
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetTheaterFeedUseCase.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt`
- **实现内容**：
  1. 先编写 ViewModel 测试，覆盖默认进入 `all`、切换非 `all` 频道空态、快速切频道防乱序三类 P0 场景。
  2. 新增 `GetTheaterFeedUseCase`，保持 UseCase 只负责委托 Repository。
  3. 在 `TheaterViewModel` 中实现 `TheaterUiState`、初始化加载、频道切换重置、`hasLoadedOnce`、`errorMessage` 和 `latestRequestToken` 防旧请求覆盖。
  4. 保持 spec/design 约束：切频道时清空旧列表、回到第一页、保留当前选中频道；非 `all` 空结果进入 empty 而不是 error。
- **验证方式**：
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.feature.theater.viewmodel.TheaterViewModelTest"` 确认 T-02/T-03/T-04 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetTheaterFeedUseCase.kt` | 新增 | 剧场 Feed 用例 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt` | 新增 | 首页状态机、首屏加载、切频道、错误态、防乱序 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModelTest.kt` | 新增 | ViewModel 状态流转测试 |
| `android/app/src/test/java/com/djs66256/short_drama/domain/usecase/GetTheaterFeedUseCaseTest.kt` | 新增 | UseCase 委托行为测试 |

### Step 3：实现分页追加、热度格式化与列表 UI model

- **关联测试**：T-05、T-07
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/theater/model/TheaterDramaItemUiModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt`
- **实现内容**：
  1. 先补充分页与 formatter 测试，覆盖追加成功、追加失败不清空已有列表、`heat` 中文短数字格式化。
  2. 在 ViewModel 中实现 `loadNextPageIfNeeded()`，使用 `hasNextPage + isAppending` 双重 guard，确保无重复请求。
  3. 将 domain 实体映射为可直接渲染的 `TheaterDramaItemUiModel`，收敛卡片标题、标签、封面占位与热度文案。
  4. 分页失败仅更新 `appendErrorMessage`，不破坏已加载内容。
- **验证方式**：
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.feature.theater.viewmodel.TheaterViewModelTest"` 确认 T-05 通过
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.feature.theater.model.TheaterDramaItemUiModelTest"` 确认 T-07 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/theater/model/TheaterDramaItemUiModel.kt` | 新增 | 卡片 UI model 与热度 formatter |
| `android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt` | 修改 | 分页、追加错误、列表映射 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/theater/model/TheaterDramaItemUiModelTest.kt` | 新增 | formatter 与 UI model 测试 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModelTest.kt` | 修改 | 分页追加成功/失败测试 |

### Step 4：搭建剧场页面 Compose UI 与本地占位交互

- **关联测试**：T-08（以及对 T-02/T-03/T-05/T-07 的 UI 承接）
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/theater/ui/TheaterScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/theater/ui/TheaterComponents.kt`
- **实现内容**：
  1. 先补充 ViewModel / UI 交互测试，锁定 scan 入口只发本地 effect，不接权限与网络。
  2. 新增 `TheaterScreen` 与拆分组件，按 design-android.md 固定为顶部栏、频道 Tabs、快捷入口、双列 Feed、loading/empty/error/append footer。
  3. UI 仅消费 `StateFlow` 和回调，不把业务逻辑下沉到 Composable。
  4. 深色模式、排版、占位图和双列布局全部复用现有 Material3 与工程主题，不硬编码环境地址或新增依赖。
- **验证方式**：
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.feature.theater.viewmodel.TheaterViewModelTest"` 确认 scan 本地占位行为通过
  - 运行 `./gradlew assembleDebug` 确认 Compose UI 可编译
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/theater/ui/TheaterScreen.kt` | 新增 | 剧场页根容器与状态收集 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/theater/ui/TheaterComponents.kt` | 新增 | Tabs、快捷入口、卡片、空态、错误态、尾部状态 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModelTest.kt` | 修改 | scan 本地占位反馈测试 |

### Step 5：接入 theater 导航并完成路由回归验证

- **关联测试**：T-06
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`
- **实现内容**：
  1. 先编写导航/route builder 测试，锁定筛选、排行、预约、新剧、播放五个出口的目标路由。
  2. 将 `theater_graph` 的 root 从 `PlaceholderScreen` 替换为 `TheaterScreen`。
  3. 在剧场页回调中接入既有路由：必要时先切到 `home` top-level tab，再导航到 search / classification / ranking / new-releases / play。
  4. 预约入口必须一步进入 `ranking(contentType=all,type=booking)`；卡片点击继续复用 canonical `play`。
- **验证方式**：
  - 运行 `./gradlew test --tests "com.djs66256.short_drama.navigation.TheaterNavigationTest"` 确认 T-06 通过
  - 运行 `./gradlew assembleDebug` 确认导航图编译通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 将 theater graph root 替换为真实剧场页 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 复用/补充剧场相关 route builder |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/TheaterNavigationTest.kt` | 新增 | 快捷入口与播放路由测试 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5
```

## 验证总览

- [ ] 关键单测通过（`./gradlew test --tests "com.djs66256.short_drama.data.dto.TheaterFeedResponseDtoTest"`）
- [ ] 关键单测通过（`./gradlew test --tests "com.djs66256.short_drama.feature.theater.viewmodel.TheaterViewModelTest"`）
- [ ] 关键单测通过（`./gradlew test --tests "com.djs66256.short_drama.feature.theater.model.TheaterDramaItemUiModelTest"`）
- [ ] 关键单测通过（`./gradlew test --tests "com.djs66256.short_drama.navigation.TheaterNavigationTest"`）
- [ ] 全量单元测试通过（`./gradlew test`）
- [ ] Build 成功（`./gradlew assembleDebug`）
- [ ] 无新增 lint / 静态分析错误（`./gradlew detekt`）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增剧场频道接口 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt` | 修改 | 新增剧场 Feed 请求封装 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/TheaterFeedResponseDto.kt` | 新增 | DTO 与映射 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt` | 修改 | Repository 实现 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/TheaterChannel.kt` | 新增 | 子频道枚举 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/TheaterDrama.kt` | 新增 | 剧场卡片实体 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/TheaterPage.kt` | 新增 | 分页实体 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/TheaterQuery.kt` | 新增 | 查询模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/DramaRepository.kt` | 修改 | 暴露剧场仓储接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetTheaterFeedUseCase.kt` | 新增 | UseCase |
| `android/app/src/main/java/com/djs66256/short_drama/feature/theater/model/TheaterDramaItemUiModel.kt` | 新增 | UI model 与格式化 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModel.kt` | 新增 | 状态机与分页逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/theater/ui/TheaterScreen.kt` | 新增 | 剧场页根 UI |
| `android/app/src/main/java/com/djs66256/short_drama/feature/theater/ui/TheaterComponents.kt` | 新增 | 剧场组件 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 接入剧场页面 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 复用/补充路由 |
| `android/app/src/test/java/com/djs66256/short_drama/data/dto/TheaterFeedResponseDtoTest.kt` | 新增 | DTO 测试 |
| `android/app/src/test/java/com/djs66256/short_drama/domain/usecase/GetTheaterFeedUseCaseTest.kt` | 新增 | UseCase 测试 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/theater/viewmodel/TheaterViewModelTest.kt` | 新增/修改 | ViewModel 测试 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/theater/model/TheaterDramaItemUiModelTest.kt` | 新增 | Formatter 测试 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/TheaterNavigationTest.kt` | 新增 | 路由测试 |
