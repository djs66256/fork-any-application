# 实现计划：Android — PRD-02 首页信息流

> 创建日期：2026-07-25
> 对应技术方案：design-android.md
> 对应需求：spec.md

## 概述

Android 端将在现有单 Activity + Navigation Compose 架构上，把首页从 appName/appVersion 占位页演进为 Native 首页信息流首屏。实现保持既有分层与依赖不变，围绕 `ApiService` query 收口、`HomeViewModel` 状态机、`HomeScreen` 列表/空态/错误态/重试，以及 `play/detail` 路由联通展开，并以轻量 TDD 先补齐单元测试与关键联通验证。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 首页首次加载成功进入列表态 | `GetDramasUseCase(page=1, pageSize=10)` 返回至少 1 条 `Drama` | `HomeUiState` 从 loading 切到 success，`items` 非空且 `errorMessage=null` | 单元测试 | P0 |
| T-02 | 首页首次加载为空进入空态 | `GetDramasUseCase` 返回空列表 | `HomeUiState` 结束 loading，`items` 为空且不显示错误 | 单元测试 | P0 |
| T-03 | 首页首次加载失败进入错误态 | `GetDramasUseCase` 返回 `ApiResult.Error` 或 `ApiResult.Exception` | `HomeUiState` 结束 loading，展示 `errorMessage`，保留重试入口 | 单元测试 | P0 |
| T-04 | 错误态重试恢复成功且避免重复请求 | 首次失败，重试后成功；或连续点击 retry | 状态按 `error -> retrying/loading -> success` 迁移，重复重试不会并发触发多次加载 | 单元测试 | P0 |
| T-05 | 列表卡片动作打通既有播放/详情路由 | 点击卡片播放/详情动作，传入有效 `drama.id` | `NavGraph` 继续导航到 `play/{videoId}` 与 `detail/{dramaId}`，空 id 不进入正常导航 | 单元测试 | P0 |
| T-06 | 列表接口 query 参数收口到 canonical contract | 调用 `ApiService.getDramas(page, pageSize)` | Retrofit 注解使用 `page` + `pageSize`，不再出现 `page_size` | 单元测试 | P0 |
| T-07 | DTO/Repository 映射继续支撑首页卡片字段 | `DramaListResponseDto` 含 `cover_url`、`episode_count`、`tags` 等字段 | `DramaRepositoryImpl` 输出可直接驱动首页卡片渲染的 `Drama` 列表 | 单元测试 | P1 |

## 实现步骤

<!-- 每个步骤遵循：定义测试 → 写实现 → 验证 → 补充测试 → 记录变更 -->

### Step 1：收口列表接口 query 契约并补齐数据链路测试

- **关联测试**：T-06、T-07
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt`
- **实现内容**：
  1. 先补一个针对 `ApiService.getDramas` 签名/注解的单元测试或反射断言，明确 query 必须是 `page` + `pageSize`。
  2. 将 `ApiService.kt` 中 `@Query("page_size")` 收口为 `@Query("pageSize")`，默认值与 design 保持到首屏第一页使用场景一致。
  3. 复核 `DramaRemoteDataSource` 与 `DramaRepositoryImpl` 的透传链路，确保只改 query 命名，不新增接口层分支或依赖。
  4. 如现有测试缺口明显，补充 repository/DTO 映射测试，固定首页卡片所需字段不会在后续编码中被误改。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.home.viewmodel.HomeViewModelTest"` 前，先补齐/运行对应网络与 repository 单测
  - 运行 `cd android && ./gradlew test` 确认 T-06、T-07 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 将 `getDramas` query 从 `page_size` 收口到 `pageSize` |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt` | 校验/轻微修改 | 保持数据源调用与 `ApiResult` 包装一致 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt` | 校验/轻微修改 | 保持 DTO -> Domain 映射与首页卡片字段一致 |
| `android/app/src/test/java/...` | 修改/按需新增 | 补 query 注解与 repository 映射测试 |

### Step 2：先写状态机测试，再把 HomeViewModel 切到首页 Feed 模型

- **关联测试**：T-01、T-02、T-03、T-04
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModelTest.kt`
- **实现内容**：
  1. 先重写 `HomeViewModelTest.kt`，用 MockK mock `GetDramasUseCase`，覆盖首次成功、空列表、失败、重试恢复、重复重试去重等状态流转。
  2. 将 `HomeViewModel` 依赖从 `AppConfig` 切换为 `GetDramasUseCase`，去掉首页占位页专属的 `appName/appVersion` 状态。
  3. 明确定义 `HomeUiState`：至少包含 `isLoading`、`items`、`errorMessage`、`hasLoadedOnce`、`isRetrying`（或等价字段），以匹配 design-android 的状态机。
  4. 在 ViewModel 中实现 `loadIfNeeded()` 与 `retry()`：首次进入默认请求 `page = 1, pageSize = 10`，错误态重试复用同一链路，并避免重复并发请求。
  5. 错误映射保持轻量：`ApiResult.Error` 优先展示服务端 message，`ApiResult.Exception` 退回通用失败文案，不额外引入新的错误抽象层。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.home.viewmodel.HomeViewModelTest"` 确认 T-01 ~ T-04 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/test/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModelTest.kt` | 修改 | 以首页 Feed 状态机替换现有 appName/appVersion 测试 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt` | 修改 | 接入 `GetDramasUseCase`，实现 loading/success/empty/error/retry 状态机 |

### Step 3：按状态机重构 HomeScreen，打通列表/空态/错误态/重试与路由动作

- **关联测试**：T-01、T-02、T-03、T-04、T-05
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`
- **实现内容**：
  1. 基于 Step 2 已通过的 ViewModel 测试，重构 `HomeScreen.kt`，不再展示 app 图标 + 示例按钮，而是按 `HomeUiState` 渲染 loading、列表、空态、错误态。
  2. 在 `HomeScreen` 进入组合时触发 `viewModel.loadIfNeeded()`，并在错误态提供 retry 按钮调用 `viewModel.retry()`；列表态使用现有 Compose/Material3 组件搭建单列卡片，不新增 UI 依赖。
  3. 卡片至少展示标题、描述、分类/标签/评分/集数字段中的稳定子集，并对缺封面、空描述、空标签做降级展示，避免把 design 中的理想字段误写成新文件或新组件体系。
  4. 列表卡片主次动作分别调用 `onOpenPlay(drama.id)` 与 `onOpenDetail(drama.id)`，保持 `NavGraph.kt` 现有 `AppDestination.play()` / `AppDestination.detail()` 联通方式不变。
  5. 若发现首页直接传空 id 的风险，在 UI 层提前禁用点击或在 ViewModel/UI 层过滤，确保不构造异常路由；不臆造新的导航中间层。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.home.viewmodel.HomeViewModelTest"` 回归状态机
  - 运行 `cd android && ./gradlew test` 确认路由与数据层测试未被破坏
  - 运行 `cd android && ./gradlew assembleDebug` 确认首页 Compose 改造可编译
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 从占位页改为 Feed 列表、空态、错误态、重试 UI |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 校验/轻微修改 | 复用既有 `play/detail` 导航，确保首页列表动作接线正确 |

### Step 4：补齐首页主链路验收与回归验证

- **关联测试**：T-01 ～ T-07
- **目标文件**：`android/app/src/test/java/...`、必要时 `android/app/src/main/java/.../navigation/*.kt`
- **实现内容**：
  1. 回看已有测试覆盖，确保首页状态机、query 收口、DTO/Repository 映射、播放/详情路由联通均有对应断言，不把“手工点一下能用”当成唯一验收。
  2. 如当前仓库已有可复用的导航测试基础，则补首页卡片动作到 `play/detail` 的轻量联通测试；若无现成基础，则至少在计划实现阶段明确用 `NavGraph.kt` 现有路由常量做单元级断言，不新增测试框架。
  3. 执行 Android 端核心验证命令，记录失败项与已知限制（例如本期不做下拉刷新、加载更多、本地缓存）。
  4. 将最终实现约束回写到 coding 阶段说明：只消费第一页、无新增依赖、首页状态由 ViewModel 单一来源驱动。
- **验证方式**：
  - 运行 `cd android && ./gradlew test`
  - 运行 `cd android && ./gradlew assembleDebug`
  - 如时间允许，运行 `cd android && ./gradlew detekt`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/test/java/...` | 修改/按需新增 | 补首页主链路与回归测试 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/*.kt` | 校验/轻微修改 | 仅在联通性验证需要时调整现有导航接线 |

## 依赖关系

```
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4
```

## 验证总览

- [ ] 所有测试通过（`cd android && ./gradlew test`）
- [ ] Build 成功（`cd android && ./gradlew assembleDebug`）
- [ ] 无新增 lint 错误（`cd android && ./gradlew detekt`）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 首页列表接口 query 从 `page_size` 收口到 `pageSize` |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt` | 校验/轻微修改 | 维持列表请求与 `ApiResult` 包装一致 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt` | 校验/轻微修改 | 维持 DTO -> Domain 映射满足首页卡片字段 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt` | 修改 | 首页 ViewModel 切换到 Feed 状态机与重试逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 首页改为列表/空态/错误态/重试 UI |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 校验/轻微修改 | 复用 `play/detail` 路由联通首页卡片动作 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModelTest.kt` | 修改 | 重建首页状态机测试 |
| `android/app/src/test/java/...` | 按需新增 | 补 query 契约、映射与路由联通测试 |
