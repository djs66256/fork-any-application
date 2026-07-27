# 实现计划：Android — PRD-05 排行体系

> 创建日期：2026-07-27
> 对应技术方案：design-android.md
> 对应需求：spec.md

## 概述

Android 端将在现有单 Activity + Navigation Compose + Hilt + ViewModel + Repository 架构上，把排行能力落到独立 `feature/ranking` 模块，并以轻量 TDD 方式分步推进：先锁定路由、DTO、Repository 与 ViewModel 状态机，再完成双层 Tab、分页、预约与登录拦截，最后接入 Compose 页面和导航回归。验证命令基于当前仓库实际 Android Gradle 工程，优先使用 JVM 单元测试、整包测试、编译与 detekt 校验。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> Android 端核心逻辑优先放在 `android/app/src/test/` 下的纯 JVM 单测中。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 排行页默认首次加载成功 | `SavedStateHandle` 无筛选参数，`GetDramaRankingsUseCase(all, hot, page=1, pageSize=10)` 返回 1 条以上数据 | `RankingUiState` 从 loading 进入 success，`selectedContentType=ALL`、`selectedRankingType=HOT`、`items` 非空 | 单元测试 | P0 |
| T-02 | 排行页默认首次加载为空 | 默认请求返回空列表 | 页面结束 loading，进入 empty，不显示全页错误态 | 单元测试 | P0 |
| T-03 | 排行页默认首次加载失败并支持重试 | 首次返回 `ApiResult.Error/Exception`，点击重试后返回成功 | 状态按 `loading -> error -> retry/loading -> success` 迁移，重试后列表恢复 | 单元测试 | P0 |
| T-04 | 一级 Tab 切换保留当前二级 Tab | 当前为 `all + booking + page>1`，切到 `live_action` | 保留 `booking`，重置到 `page=1`，清空旧列表并请求新榜单 | 单元测试 | P0 |
| T-05 | 二级 Tab 切换保留当前一级 Tab 且旧请求不会脏写 | 当前为 `ai + hot`，快速切到 `recommend`/`booking` | 保留 `ai`，只消费最后一次切换结果，旧请求返回不会覆盖当前状态 | 单元测试 | P0 |
| T-06 | 分页加载更多成功追加且不重复触发 | 第一页成功且 `hasNextPage=true`，连续触发触底 | 仅发起一次下一页请求，列表尾部追加新数据，页码递增 | 单元测试 | P0 |
| T-07 | 分页失败保留已加载列表并允许再次重试 | 已有第一页数据，第二页请求失败 | 已有列表保留，显示尾部错误态/重试入口，成功重试后可继续追加 | 单元测试 | P1 |
| T-08 | 已登录预约成功更新当前项状态 | 当前在预约榜，`AuthSessionProvider.isLoggedIn()=true`，`BookDramaUseCase` 成功 | 当前项 `isBooked=true`、`bookingCount` 更新、按钮进入已预约/禁用态 | 单元测试 | P0 |
| T-09 | 未登录点击预约触发统一拦截 | 当前在预约榜，`AuthSessionProvider.isLoggedIn()=false` | 不发起预约接口请求，发出 `RequireLogin(returnRoute)` 一次性事件 | 单元测试 | P0 |
| T-10 | 排行路由与播放跳转接线正确 | 调用 `AppDestination.ranking(contentType, type)`，点击排行项传入有效 `drama.id` | 生成 canonical `ranking?...` route，并继续复用 `play/{videoId}` 导航 | 单元测试 | P1 |

## 实现步骤

### Step 1：先锁定排行 route、查询模型与数据链路契约

- **关联测试**：T-10
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/RankingQuery.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/RankingDrama.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/dto/RankingDramaDto.kt`
- **实现内容**：
  1. 先补 `RoutesTest`，固定 `ranking?contentType={contentType}&type={type}` 的 route 生成规则，以及默认 `all + hot` 的 canonical 输出。
  2. 在 `AppDestination.kt` 中新增排行 route、query arg 常量与构造函数，保证登录回跳和默认进入都复用同一套路由表达。
  3. 在 `NavGraph.kt` 预留/注册真实 `RankingScreen` 落点，并保持点击排行项后仍走既有 `AppDestination.play(videoId)`，不新增播放器路由语义。
  4. 在 domain/data 层先补齐 `RankingContentType`、`RankingType`、`RankingQuery`、`RankingDrama`、`RankingPage`、`BookDramaResult` 与对应 DTO，锁定 API 字段和映射边界，避免后续 UI 实现时临时拼字段。
  5. 在 `ApiService.kt` 中新增 `getDramaRankings()`、`bookDrama()` 契约，但先以测试约束 query/path 命名，保证与 design 中的 `type/contentType/page/pageSize` 一致。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.navigation.RoutesTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.data.repository.RankingRepositoryImplTest"`（实现后）
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 新增排行 route/query args 与 route builder |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 注册排行页并复用 `play/{videoId}` 导航 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增排行查询与预约接口定义 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/RankingQuery.kt` | 新增 | 定义一级/二级 Tab 枚举与查询模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/RankingDrama.kt` | 新增 | 定义排行列表实体 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/RankingDramaDto.kt` | 新增 | 定义排行 DTO 与字段映射 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 补排行 route 与默认值断言 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/RankingRepositoryImplTest.kt` | 新增 | 固定 DTO 到 Domain 的映射契约 |

### Step 2：先写默认加载、空态、错误态与重试测试，再实现首屏状态机

- **关联测试**：T-01、T-02、T-03
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetDramaRankingsUseCase.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/repository/RankingRepository.kt`
- **实现内容**：
  1. 先编写 `RankingViewModelTest`，覆盖默认进入第一页成功、空列表进入空态、首次失败进入错误态、点击重试恢复成功。
  2. 在 `RankingViewModel` 中通过 `SavedStateHandle` 读取默认筛选，初始化时自动触发 `all + hot + page=1 + pageSize=10` 请求。
  3. 定义 `RankingUiState`，至少包含 `selectedContentType`、`selectedRankingType`、`items`、`isLoading`、`errorMessage`、`hasLoadedOnce`、`page`、`hasNextPage`。
  4. 把 `GetDramaRankingsUseCase` 与 `RankingRepository` 接到 ViewModel，统一处理 `ApiResult.Success/Error/Exception`，确保全页错误与空态边界稳定。
  5. 为重试提供单一入口 `retry()`，避免 UI 层重复拼接加载逻辑。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.ranking.viewmodel.RankingViewModelTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | 新增 | 实现默认加载、空态、错误态与重试状态机 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetDramaRankingsUseCase.kt` | 新增 | 排行查询用例 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/RankingRepository.kt` | 新增 | 排行查询仓储接口 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModelTest.kt` | 新增 | 覆盖首屏加载、空态、错误与重试 |

### Step 3：先锁定一级/二级 Tab 切换与乱序保护，再实现刷新逻辑

- **关联测试**：T-04、T-05
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/model/RankingUiModel.kt`
- **实现内容**：
  1. 在 `RankingViewModelTest` 中补一级 Tab 切换、二级 Tab 切换、快速连续切换时旧请求结果不覆盖新状态的用例。
  2. 在 ViewModel 中把一级/二级 Tab 选择独立建模，确保切一级只改 `contentType`，切二级只改 `type`。
  3. 切换任一维度时统一走 `refresh()`：清空旧列表、重置 `page=1`、清理分页错误、保留当前另一维度选择。
  4. 增加请求 token 或 query key 校验，确保快切场景下只消费最后一次有效请求结果。
  5. 如需要，在 `RankingUiModel.kt` 中补齐榜单指标映射逻辑，让热榜/推荐榜/预约榜在 UI 层只消费统一模型。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.ranking.viewmodel.RankingViewModelTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | 修改 | 实现双层 Tab 切换、分页重置与乱序保护 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/model/RankingUiModel.kt` | 新增/修改 | 收口榜单指标与列表项 UI 映射 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModelTest.kt` | 修改 | 补一级/二级 Tab 与旧请求脏写保护测试 |

### Step 4：先写分页追加与分页失败测试，再实现加载更多链路

- **关联测试**：T-06、T-07
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/datasource/RankingRemoteDataSource.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/repository/RankingRepositoryImpl.kt`
- **实现内容**：
  1. 在 `RankingViewModelTest` 中先写分页成功追加、重复触底只发一次请求、分页失败保留旧列表、分页失败后可重试继续追加的用例。
  2. 在 `RankingViewModel` 中实现 `loadNextPageIfNeeded()`，用 `isAppending + hasNextPage` 防止重复请求。
  3. 在 `RankingRemoteDataSource` 中按现有 `SearchRemoteDataSource` 风格解析排行接口错误包体，保证分页失败能区分业务错误和异常错误。
  4. 在 `RankingRepositoryImpl` 中透传分页信息并合并到 domain `RankingPage`，避免分页控制散落在 UI 层。
  5. 明确“超大页码返回空数组”按成功处理，只停止继续加载，不进入全页错误态。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.ranking.viewmodel.RankingViewModelTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.data.datasource.RankingRemoteDataSourceTest"`（实现后）
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | 修改 | 实现加载更多、尾部错误与重试 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/RankingRemoteDataSource.kt` | 新增 | 统一封装排行接口与错误解析 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/RankingRepositoryImpl.kt` | 新增 | 透传分页信息并完成 DTO 映射 |
| `android/app/src/test/java/com/djs66256/short_drama/data/datasource/RankingRemoteDataSourceTest.kt` | 新增 | 覆盖分页成功、失败与错误包体解析 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModelTest.kt` | 修改 | 补分页追加、失败与去重测试 |

### Step 5：先写预约成功与未登录拦截测试，再实现 booking 状态更新

- **关联测试**：T-08、T-09
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/domain/usecase/BookDramaUseCase.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthSessionProvider.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt`
- **实现内容**：
  1. 在 `RankingViewModelTest` 中先写已登录预约成功、同一项请求中重复点击被忽略、未登录点击直接发 `RequireLogin(returnRoute)` 的测试。
  2. 新增 `BookDramaUseCase` 与 `AuthSessionProvider` 抽象，ViewModel 先查登录态，再决定走预约接口还是走统一拦截事件。
  3. 在 Hilt 模块中提供默认占位 `AuthSessionProvider`，使当前工程在未接入真实登录前也能完整编译和测试。
  4. 预约成功后只原位更新当前项 `isBooked` 和 `bookingCount`，不立即重排整个榜单，避免列表闪动。
  5. 未登录场景构造 `returnRoute = AppDestination.ranking(selectedContentType, selectedRankingType)`，为后续登录回跳保留上下文。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.ranking.viewmodel.RankingViewModelTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.data.repository.RankingRepositoryImplTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/BookDramaUseCase.kt` | 新增 | 预约提交用例 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthSessionProvider.kt` | 新增 | 登录态查询抽象 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt` | 修改 | 提供默认 `AuthSessionProvider` |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 注入排行仓储与相关依赖 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | 修改 | 实现预约成功、loading、防重复点击与登录拦截事件 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModelTest.kt` | 修改 | 补预约成功、未登录拦截与重复点击测试 |

### Step 6：接入 RankingScreen 与导航回归，完成端到端验证收口

- **关联测试**：T-01 ～ T-10
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt`
- **实现内容**：
  1. 新增 `RankingScreen`，按 `RankingUiState` 渲染顶部栏、一级/二级 Tab、列表、空态、错误态、分页尾部和预约按钮。
  2. 在 Compose 层只做状态消费与事件转发，不把分页、预约或登录判断逻辑下沉到 UI。
  3. 在 `NavGraph.kt` 中把真实 `RankingScreen` 与 `AppDestination.ranking()` 接起来，并把点击排行项、点击预约、返回操作全部导回既有导航壳。
  4. 回归 `RoutesTest`、`NavGraphTest` 与新增的 `RankingViewModelTest` / `RankingRepositoryImplTest` / `RankingRemoteDataSourceTest`，确保路由、状态机、网络映射和主流程一致。
  5. 执行 Android 端总体验证，记录已知限制：当前不引入 Paging/图片加载库，排行榜浏览链路先依赖 JVM 单测 + 编译验证收口。
- **验证方式**：
  - 运行 `cd android && ./gradlew test`
  - 运行 `cd android && ./gradlew assembleDebug`
  - 运行 `cd android && ./gradlew detekt`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt` | 新增 | 渲染双层 Tab、列表、空态、错误态、分页与预约按钮 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 将排行页接入主导航并复用播放跳转 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt` | 修改 | 补排行页路由注册与主链路导航断言 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 回归排行 route 生成 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModelTest.kt` | 修改 | 回归关键状态机 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5 ──▶ Step 6
```

## 验证总览

- [ ] 关键状态机测试通过（`cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.ranking.viewmodel.RankingViewModelTest"`）
- [ ] 路由与导航测试通过（`cd android && ./gradlew test --tests "com.djs66256.short_drama.navigation.RoutesTest"`）
- [ ] 数据层测试通过（`cd android && ./gradlew test --tests "com.djs66256.short_drama.data.repository.RankingRepositoryImplTest"`）
- [ ] 远端数据源测试通过（`cd android && ./gradlew test --tests "com.djs66256.short_drama.data.datasource.RankingRemoteDataSourceTest"`）
- [ ] 所有测试通过（`cd android && ./gradlew test`）
- [ ] Build 成功（`cd android && ./gradlew assembleDebug`）
- [ ] 无新增 lint 错误（`cd android && ./gradlew detekt`）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 新增排行 route/query args 与默认回跳表达 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 注册排行页并复用播放跳转 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增排行查询与预约 API |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/RankingQuery.kt` | 新增 | 一级/二级 Tab 与查询模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/RankingDrama.kt` | 新增 | 排行列表实体 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/BookDramaResult.kt` | 新增 | 预约结果实体 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/RankingRepository.kt` | 新增 | 排行仓储接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthSessionProvider.kt` | 新增 | 登录态查询抽象 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetDramaRankingsUseCase.kt` | 新增 | 排行查询用例 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/BookDramaUseCase.kt` | 新增 | 预约提交用例 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/RankingDramaDto.kt` | 新增 | 排行 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/RankingListResponseDto.kt` | 新增 | 排行分页响应 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/BookDramaResponseDto.kt` | 新增 | 预约响应 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/RankingRemoteDataSource.kt` | 新增 | 排行/预约远端数据源 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/RankingRepositoryImpl.kt` | 新增 | 排行仓储实现与映射 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/model/RankingUiModel.kt` | 新增 | 排行列表 UI 模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | 新增 | 默认加载、Tab 切换、分页、预约与登录拦截状态机 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/ui/RankingScreen.kt` | 新增 | 排行页 Compose UI |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt` | 修改 | 提供默认登录态依赖 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 绑定排行仓储 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 排行 route 断言 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt` | 修改 | 排行导航接线断言 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/RankingRepositoryImplTest.kt` | 新增 | DTO 映射与分页透传测试 |
| `android/app/src/test/java/com/djs66256/short_drama/data/datasource/RankingRemoteDataSourceTest.kt` | 新增 | 错误解析与请求链路测试 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModelTest.kt` | 新增 | 覆盖默认加载、Tab、分页、预约、未登录拦截、错误重试 |
