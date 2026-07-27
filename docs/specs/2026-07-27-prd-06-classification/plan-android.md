# 实现计划：Android — PRD-06 分类浏览

> 创建日期：2026-07-27
> 对应技术方案：design-android.md
> 对应需求：spec.md

## 概述

Android 端将在现有单 Activity + Navigation Compose + Hilt + ViewModel + Repository 架构上，把搜索发现页已有的 `classification` 承接路由从 placeholder 切换为真实 `ClassificationScreen`，并以独立的 `ClassificationRepository / ClassificationRemoteDataSource / GetClassificationTagsUseCase / ClassificationViewModel` 落地分类页数据链路。整体实施顺序按“先收敛 ViewModel 契约与状态机，再补 Repository/DataSource 实现，再替换 Navigation 承接页，最后收口锚点同步与总回归测试”推进；每个步骤内部都遵循轻量 TDD：先写测试场景，再写实现。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> Android 端核心逻辑优先放在 `android/app/src/test/` 下的纯 JVM 单测中。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 分类页首次进入默认加载 `gender=all` | `SavedStateHandle` 无额外参数，`GetClassificationTagsUseCase(all)` 返回 3 个维度，其中 1 个 `tags=[]` | `ClassificationUiState` 默认选中 `ALL`，保留固定三维度顺序，空维度不丢失，`selectedDimension` 为首项 | 单元测试 | P0 |
| T-02 | 分类页首次加载失败并支持重试 | 首次返回 `ApiResult.Error/Exception`，点击重试后返回成功 | 状态按 `loading -> error -> retry/loading -> success` 迁移，重试后恢复三维度内容 | 单元测试 | P0 |
| T-03 | 切换 gender 成功后重置维度并滚动到首项 | 当前已在 `female` 且选中第 3 个维度，切换到 `male` | 重新请求 `male` 数据，`selectedDimension` 重置为第 1 个维度，并发出滚动到首项 section 的 effect | 单元测试 | P0 |
| T-04 | 快速切换 gender 时旧请求结果不会脏写 | 连续触发 `all -> male -> female`，旧请求晚于新请求返回 | 只有最后一次 `female` 请求结果写回 UI，旧结果被丢弃 | 单元测试 | P0 |
| T-05 | 左侧维度点击与右侧 section 滚动保持双向同步 | 点击左侧 `theme_plot`；或右侧滚动到 `character_setting` section | 左侧点击时发出滚动到对应锚点 effect；右侧滚动回调时左侧高亮同步到当前可见 section | 单元测试 | P0 |
| T-06 | 标签点击复用搜索结果页 route 构造 | 点击 `" 萌宝 "`、点击空白标签 | `buildSearchRoute()` 内部复用 `normalizeSearchQueryOrNull` 与 `AppDestination.searchResult(query)`；合法标签返回 `search/result?query=萌宝`，空白标签不导航 | 单元测试 | P0 |
| T-07 | 分类仓储独立映射三维度且透传 gender query | `ClassificationRemoteDataSource.getClassificationTags("female")` 返回固定三维度 DTO，其中 1 组 `tags=[]` | `ClassificationRepository` 不复用 `SearchRepository`，正确映射为 domain model，保留空维度并透传 `female` query | 单元测试 | P0 |
| T-08 | classification route 继续保持 canonical 语义并接到真实页面 | 搜索发现页点击分类快捷入口；调用 `AppDestination.classification()`；标签点击导航 | `classification` route 保持不变，`NavGraph` 改为承接真实 `ClassificationScreen`，标签跳转继续复用 `AppDestination.searchResult(query)`，不新增分类结果页 route | 单元测试 | P0 |
| T-09 | 分类接口 Retrofit 契约正确 | 调用 `ApiService.getDramaTags(gender="male")` | 路径为 `GET dramas/tags`，query 名为 `gender`，默认值为 `all` | 单元测试 | P1 |

## 实现步骤

### Step 1：先锁定 ClassificationViewModel 状态机与搜索跳转规则

- **关联测试**：T-01、T-02、T-03、T-04、T-06
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/classification/model/ClassificationUiModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/ClassificationTagModels.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetClassificationTagsUseCase.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModelTest.kt`
- **实现内容**：
  1. 先补 `ClassificationViewModelTest`，固定默认 `gender=all`、固定三维度顺序、空维度保留、首次失败重试、gender 切换后 `selectedDimension` 重置、滚动首项 effect、旧请求乱序保护等核心状态机行为。
  2. 定义 `ClassificationGender`、`ClassificationDimensionKey`、`ClassificationDimension`、`ClassificationTagsPayload` 与 UI model，明确分类页只消费固定三维度，不在 UI 层临时拼接分组。
  3. 新增 `ClassificationViewModel`，以 `StateFlow` 暴露 `selectedGender`、`selectedDimensionKey`、`dimensions`、`isLoading`、`isRefreshing`、`errorMessage`、`hasLoadedOnce` 等状态，并参考 `RankingViewModel` 使用 `requestToken` / `latestGender` 做快切乱序保护。
  4. 在 ViewModel 中提供 `buildSearchRoute(rawTag: String): String?`，内部只调用 `normalizeSearchQueryOrNull` 和 `AppDestination.searchResult(query)`，不新增 `classification/result`、`search?q=` 等 route 语义。
  5. 先以 mock `GetClassificationTagsUseCase` 驱动 ViewModel 测试，把页面核心行为锁住，再进入真实数据链路实现。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.classification.viewmodel.ClassificationViewModelTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt` | 新增 | 实现默认加载、gender 切换、乱序保护、维度重置与标签 route 构造 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/classification/model/ClassificationUiModel.kt` | 新增 | 定义分类页 UI 状态与分组展示模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/ClassificationTagModels.kt` | 新增 | 定义 gender、dimension key、payload 等 domain 模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetClassificationTagsUseCase.kt` | 新增 | 分类标签查询 use case 抽象 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModelTest.kt` | 新增 | 覆盖默认加载、重试、快切保护、维度重置、route 构造 |

### Step 2：先补 Repository / RemoteDataSource / API 契约测试，再接真实分类数据链路

- **关联测试**：T-07、T-09
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/dto/ClassificationTagsResponseDto.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/datasource/ClassificationRemoteDataSource.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/repository/ClassificationRepositoryImpl.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/repository/ClassificationRepository.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt`、`android/app/src/test/java/com/djs66256/short_drama/data/repository/ClassificationRepositoryImplTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/data/datasource/ClassificationRemoteDataSourceTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/core/network/ApiServiceTest.kt`
- **实现内容**：
  1. 先补 `ApiServiceTest`、`ClassificationRemoteDataSourceTest`、`ClassificationRepositoryImplTest`，锁定 `GET dramas/tags?gender=...` 的路径/query 命名、错误解析语义，以及 DTO 到 domain 的三维度映射契约。
  2. 在 `ApiService.kt` 中新增 `getDramaTags(@Query("gender") gender: String = "all")`，保持与 shared design 一致，不额外引入新的成功包裹结构。
  3. 新增独立的 `ClassificationRemoteDataSource`，按 `SearchRemoteDataSource` 的风格解析 `{ error: { code, message } }` 错误体，避免把分类接口塞进现有搜索数据源。
  4. 新增 `ClassificationRepository` / `ClassificationRepositoryImpl`，把 DTO 映射为 domain 三维度结构，并确保某一维度 `tags=[]` 时仍保留该维度对象，不做过滤或折叠。
  5. 在 `RepositoryModule.kt` 绑定分类仓储，明确 Classification 与 Search/Ranking 平行存在，不复用 `SearchRepository` 承接分类数据。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.core.network.ApiServiceTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.data.datasource.ClassificationRemoteDataSourceTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.data.repository.ClassificationRepositoryImplTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增分类标签接口定义 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/ClassificationTagsResponseDto.kt` | 新增 | 对齐 `gender + dimensions[]` 的 DTO 结构 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/ClassificationRemoteDataSource.kt` | 新增 | 封装分类接口请求与错误解析 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/ClassificationRepository.kt` | 新增 | 定义分类仓储接口 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/ClassificationRepositoryImpl.kt` | 新增 | 实现三维度映射、空维度保留与错误透传 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 注入 `ClassificationRepository` |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/ApiServiceTest.kt` | 修改 | 补分类接口路径与 `gender` query 断言 |
| `android/app/src/test/java/com/djs66256/short_drama/data/datasource/ClassificationRemoteDataSourceTest.kt` | 新增 | 覆盖成功、错误包体与异常兜底 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/ClassificationRepositoryImplTest.kt` | 新增 | 覆盖三维度映射、空维度保留与 query 透传 |

### Step 3：把 classification route 从 placeholder 切到真实 ClassificationScreen

- **关联测试**：T-08
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/classification/ui/ClassificationScreen.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/search/viewmodel/SearchHomeViewModelTest.kt`
- **实现内容**：
  1. 先补 `RoutesTest` 与 `SearchHomeViewModelTest` 的回归断言，固定 `AppDestination.classification()` 仍输出 `classification`，搜索发现页的「分类」快捷入口 route 不变化。
  2. 在 `NavGraph.kt` 中把 `PlaceholderScreen(title = "分类")` 替换为真实 `ClassificationScreen`，但保持 route 名和所在 `home_graph` 不变，避免破坏既有 deeplink / quick entry 语义。
  3. `ClassificationScreen` 只接收 `onBack` 与 `onOpenSearchResult(route)` 回调，标签点击后的导航统一复用 ViewModel 产出的 `AppDestination.searchResult(query)`，不新增分类结果页 route。
  4. 如需回查路由构造，只允许复用 `AppDestination.classification()` 和 `AppDestination.searchResult(query)` 两套既有 canonical route，不扩散新的导航别名。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.navigation.RoutesTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.search.viewmodel.SearchHomeViewModelTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 用真实 `ClassificationScreen` 替换分类 placeholder |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 复用/按需微调 | 保持 `classification` 与 `searchResult(query)` canonical route 语义 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/classification/ui/ClassificationScreen.kt` | 新增 | 分类页导航壳，连接 ViewModel 与搜索结果页跳转 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 回归分类 route 与搜索结果 route 复用断言 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/search/viewmodel/SearchHomeViewModelTest.kt` | 修改 | 回归 classification quick entry 导航 |

### Step 4：补左侧维度与右侧 section 锚点同步，再完成真实双栏页面

- **关联测试**：T-03、T-05
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/classification/ui/ClassificationScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModelTest.kt`
- **实现内容**：
  1. 先在 `ClassificationViewModelTest` 中补“点击左侧维度发出滚动 effect”“右侧滚动切换 section 后左侧选中高亮同步”的双向同步用例，把锚点规则固定下来。
  2. 在 `ClassificationScreen.kt` 中实现顶部 gender Tab、左侧维度 rail、右侧 section 列表与标签矩阵，右侧以稳定的 `ClassificationDimensionKey` 作为 section key。
  3. 左侧点击维度时，调用 `viewModel.onDimensionSelected(key)` 并消费 `ScrollToDimension(key)` effect，把右侧滚动到对应 section。
  4. 右侧滚动时，通过 `LazyListState` + 可见 section 计算回调给 ViewModel，同步更新 `selectedDimensionKey`，保证左侧高亮随内容滚动变化。
  5. gender 切换成功后统一滚动到首项 section，并对 `tags=[]` 的维度渲染空态文案而不是隐藏整个 section，保证左右锚点稳定。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.classification.viewmodel.ClassificationViewModelTest"`
  - 运行 `cd android && ./gradlew assembleDebug`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/classification/ui/ClassificationScreen.kt` | 修改 | 实现双栏布局、section 锚点同步与空维度空态 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt` | 修改 | 补双向锚点同步事件与滚动首项 effect |
| `android/app/src/test/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModelTest.kt` | 修改 | 补锚点同步与滚动首项测试 |

### Step 5：做 Hilt 接线与 Android 端总回归测试收口

- **关联测试**：T-01 ～ T-09
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/test/java/com/djs66256/short_drama/...`
- **实现内容**：
  1. 回查 Hilt 依赖接线，确保 `ClassificationViewModel -> GetClassificationTagsUseCase -> ClassificationRepository -> ClassificationRemoteDataSource -> ApiService` 全链路可编译、可注入。
  2. 回归分类入口、真实页面、标签跳搜索结果页三条主链路，确认没有把分类数据混入现有 SearchRepository，也没有引入新的分类结果 route。
  3. 执行 Android 端全量单测、构建与 detekt，作为本 PRD 的交付收口；若有新增已知限制，应回写到实现备注中，而不是留到代码阶段临时判断。
- **验证方式**：
  - 运行 `cd android && ./gradlew test`
  - 运行 `cd android && ./gradlew assembleDebug`
  - 运行 `cd android && ./gradlew detekt`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 收口分类仓储与 use case 依赖注入 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 校验/收口 | 确认分类页接线与搜索结果页跳转无回归 |
| `android/app/src/test/java/com/djs66256/short_drama/...` | 修改/补齐 | 收口分类模块遗漏测试与回归 case |

## 依赖关系

```text
Step 1（ViewModel 契约与测试）
  └──▶ Step 2（Repository / RemoteDataSource / API）
        └──▶ Step 3（Navigation 替换 placeholder）
              └──▶ Step 4（ClassificationScreen 双栏锚点同步）
                    └──▶ Step 5（DI + 总回归测试）
```

## 验证总览

- [ ] 分类 ViewModel 测试通过（`cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.classification.viewmodel.ClassificationViewModelTest"`）
- [ ] 分类仓储测试通过（`cd android && ./gradlew test --tests "com.djs66256.short_drama.data.repository.ClassificationRepositoryImplTest"`）
- [ ] 分类数据源测试通过（`cd android && ./gradlew test --tests "com.djs66256.short_drama.data.datasource.ClassificationRemoteDataSourceTest"`）
- [ ] API 契约测试通过（`cd android && ./gradlew test --tests "com.djs66256.short_drama.core.network.ApiServiceTest"`）
- [ ] 分类与搜索导航回归通过（`cd android && ./gradlew test --tests "com.djs66256.short_drama.navigation.RoutesTest"`）
- [ ] 搜索发现页分类快捷入口回归通过（`cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.search.viewmodel.SearchHomeViewModelTest"`）
- [ ] 所有测试通过（`cd android && ./gradlew test`）
- [ ] Build 成功（`cd android && ./gradlew assembleDebug`）
- [ ] 无新增 lint 错误（`cd android && ./gradlew detekt`）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/classification/ui/ClassificationScreen.kt` | 新增 | 分类页真实 Compose 页面，承接 gender Tab、左侧维度与右侧标签 section |
| `android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt` | 新增 | 管理默认加载、维度同步、乱序保护与标签 route 构造 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/classification/model/ClassificationUiModel.kt` | 新增 | 分类页 UI 模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/ClassificationTagModels.kt` | 新增 | gender、dimension、payload domain 模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/ClassificationRepository.kt` | 新增 | 分类仓储接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetClassificationTagsUseCase.kt` | 新增 | 分类标签查询 use case |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/ClassificationTagsResponseDto.kt` | 新增 | 分类标签接口 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/ClassificationRemoteDataSource.kt` | 新增 | 分类标签远端数据源 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/ClassificationRepositoryImpl.kt` | 新增 | 分类仓储实现与三维度映射 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增 `GET dramas/tags` 接口 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 绑定分类仓储依赖 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 将 `classification` 从 placeholder 切到真实 `ClassificationScreen` |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 复用/按需微调 | 继续复用 `classification` 与 `searchResult(query)` route |
| `android/app/src/test/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModelTest.kt` | 新增 | 分类页状态机与锚点同步测试 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/ClassificationRepositoryImplTest.kt` | 新增 | 三维度映射与空维度保留测试 |
| `android/app/src/test/java/com/djs66256/short_drama/data/datasource/ClassificationRemoteDataSourceTest.kt` | 新增 | 错误解析与接口调用测试 |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/ApiServiceTest.kt` | 修改 | 补分类接口注解与 query 测试 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 回归 classification 与 search result route 语义 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/search/viewmodel/SearchHomeViewModelTest.kt` | 修改 | 回归分类快捷入口导航 |
