# 实现计划：Android — PRD-04 搜索发现

> 创建日期：2026-07-26
> 对应技术方案：design-android.md
> 对应需求：spec.md

## 概述

Android 端将在现有单 Activity + Navigation Compose + Hilt + ViewModel 分层上，补齐“首页搜索入口 → 搜索发现页 → 搜索结果页 → 播放/详情”的 Native 搜索链路。实现以轻量 TDD 推进，优先补齐 route/deeplink、ViewModel 状态机、DataStore 搜索历史、本地错误解析与网络接线的 JVM 单测，再完成 Compose 页面与 Hilt 接线，避免在当前仅有首页/播放器/详情基础上一次性引入不可回归的改动。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> Android 端所有核心场景优先落在 `src/test/` 纯 JVM 单测中。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 搜索相关 route 与 deeplink 可生成并解析 | `search`、`search/result?query=逆袭`、`ranking`、`classification`、`new-releases`、`actors` | `AppDestination` 生成 canonical route；`DeeplinkRouteParser` 解析到正确 `PendingRoute`；非法 query 不触发导航 | 单元测试 | P0 |
| T-02 | 搜索发现页初始化成功与局部失败 | 本地历史为空/非空；热搜接口成功或失败 | `SearchHomeViewModel` 正确输出 `history`、`hotSearches`、`hotSearchErrorMessage` 与 4 个快捷入口；热搜失败不阻塞手动搜索 | 单元测试 | P0 |
| T-03 | 搜索结果成功后写入历史并进入内容态/空态 | 合法 query，`searchDramas()` 返回命中结果或空列表 | `SearchResultViewModel` 从 loading 切到 content/empty；成功返回后调用保存历史；顶部 query 与结果一致 | 单元测试 | P0 |
| T-04 | 搜索结果失败时走本地错误解析且不写历史 | 后端返回 `{ error: { code, message } }` 或网络异常 | `SearchRemoteDataSource` 输出 `ApiResult.Error/Exception`；`SearchResultViewModel` 进入 error 态且不调用保存历史 | 单元测试 | P0 |
| T-05 | 搜索历史 DataStore 规则正确 | 连续写入重复词、超过 10 条、清空、损坏数据 | 历史按 trim 后去重、最近优先、最多 10 条；可一键清空；损坏数据降级为空列表 | 单元测试 | P0 |
| T-06 | 搜索接口与热搜接口网络接线正确 | 调用 `ApiService.searchDramas()`、`getHotSearches()` | Retrofit 注解、query 名称、DTO 到 domain 映射与 Hilt repository 接线符合 design；默认 base url 前缀不再偏向旧 `/api/v1` | 单元测试 | P0 |
| T-07 | 首页搜索入口与结果卡片动作联通现有主链路 | 首页点击搜索入口；结果页点击“观看/详情”；快捷入口点击排行/分类/新剧/演员 | 导航进入搜索页/结果页/占位承接页；结果卡片继续复用现有 `play/{videoId}`、`detail/{dramaId}` 语义 | 单元测试 | P1 |

## 实现步骤

### Step 1：先锁定 route / deeplink / pending route 语义

- **关联测试**：T-01、T-07
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`
- **实现内容**：
  1. 先补 `RoutesTest`、`DeeplinkRouteParserTest`、`MainNavigationViewModelTest`，把 `search`、`search/result?query={query}`、`ranking`、`classification`、`new-releases`、`actors` 的 route 生成、URI 编解码、非法空 query 拦截行为固定下来。
  2. 在 `AppDestination.kt` 中扩展 `Route`、`Arg` 与 `PendingRoute`，保持现有 `play/player/detail` 不变，仅新增搜索相关语义。
  3. 在 `DeeplinkRouteParser.kt` 中补齐 `djsdrama://search`、`djsdrama://search/result/{query}`、`djsdrama://ranking`、`djsdrama://classification`、`djsdrama://new-releases`、`djsdrama://actors` 的解析，并统一对 query 做 decode + trim。
  4. 在 `NavGraph.kt` 中先注册搜索页、结果页和 4 个 Native 承接页的导航壳，继续复用 `MainNavigationViewModel` 的 pending route 消费模式，为后续 ViewModel/UI 落地腾出稳定路由骨架。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.navigation.RoutesTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.navigation.DeeplinkRouteParserTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.navigation.MainNavigationViewModelTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 扩展 search/result/quick entry route、arg 与 `PendingRoute` |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt` | 修改 | 新增搜索与快捷入口 deeplink 解析 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt` | 修改 | 支持搜索相关 pending route |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 注册搜索页、结果页和 4 个承接页 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 补 route 生成断言 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/DeeplinkRouteParserTest.kt` | 修改 | 补 deeplink 解析断言 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt` | 修改 | 补 pending route 发布/消费断言 |

### Step 2：先补网络与本地错误解析测试，再接搜索 API

- **关联测试**：T-04、T-06
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiClient.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/datasource/SearchRemoteDataSource.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/dto/ErrorDto.kt`、`android/app/build.gradle.kts`
- **实现内容**：
  1. 先补 `ApiServiceTest` 与新增的 `SearchRemoteDataSourceTest`，锁定 `GET dramas/search`、`GET dramas/hot-search` 的 Retrofit 注解、`q/page/pageSize` query 命名，以及服务端 `{ error: { code, message } }` 包体需被解析为 `ApiResult.Error` 的行为。
  2. 在 `ApiService.kt` 中新增 `searchDramas()`、`getHotSearches()`，继续沿用当前 Retrofit + kotlinx.serialization 风格，不引入新的网络抽象。
  3. 新增 `SearchRemoteDataSource.kt`，本地复用已有 `ErrorDto` 做错误包体解析；明确区分服务端业务错误与异常网络错误，避免后续 ViewModel 只能拿到笼统异常。
  4. 校准 `ApiClient.kt`/`android/app/build.gradle.kts` 的 base URL 约束，使默认配置最终命中 canonical `/api/dramas/search` 与 `/api/dramas/hot-search`，而不是沿用旧的 `/api/v1` 假设。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.core.network.ApiServiceTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.data.datasource.SearchRemoteDataSourceTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增搜索与热搜接口定义 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiClient.kt` | 修改 | 校准搜索接口的 base URL 拼接约束 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/SearchRemoteDataSource.kt` | 新增 | 包装搜索/热搜请求并解析错误包体 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/ErrorDto.kt` | 复用/按需轻微修改 | 作为本地错误解析 DTO |
| `android/app/build.gradle.kts` | 修改 | 校准 `api.base.url` 默认前缀 |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/ApiServiceTest.kt` | 修改 | 补搜索接口注解与 query 断言 |
| `android/app/src/test/java/com/djs66256/short_drama/data/datasource/SearchRemoteDataSourceTest.kt` | 新增 | 覆盖成功、错误包体解析与异常兜底 |

### Step 3：先固化 DataStore 历史规则，再补 repository 与 DI 接线

- **关联测试**：T-05、T-06
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/data/local/SearchHistoryLocalDataSource.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/repository/SearchRepositoryImpl.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/repository/SearchRepository.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/usecase/ObserveSearchHistoryUseCase.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/usecase/SaveSearchHistoryUseCase.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/usecase/ClearSearchHistoryUseCase.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetHotSearchKeywordsUseCase.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/usecase/SearchDramasUseCase.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt`
- **实现内容**：
  1. 先写 `SearchHistoryLocalDataSourceTest`，固定 trim 去重、最近优先、最多 10 条、清空、损坏数据降级为空列表这些规则。
  2. 基于现有 DataStore 依赖新增 `SearchHistoryLocalDataSource`，使用 Preferences + JSON 字符串持久化历史列表，不引入 Room 或新依赖。
  3. 新增 `SearchRepository` 与 `SearchRepositoryImpl`，把远端搜索/热搜与本地历史收口到同一 repository，供两个搜索 ViewModel 共享。
  4. 在 `AppModule.kt`/`RepositoryModule.kt` 中补齐 DataStore、本地数据源、SearchRepository 与 use case 的 Hilt 绑定，保持当前 domain → data → core 依赖方向不变。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.data.local.SearchHistoryLocalDataSourceTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.data.repository.SearchRepositoryImplTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/data/local/SearchHistoryLocalDataSource.kt` | 新增 | 基于 DataStore Preferences 管理搜索历史 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/SearchRepositoryImpl.kt` | 新增 | 聚合搜索、热搜、本地历史 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/SearchRepository.kt` | 新增 | 定义搜索 domain 接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/SearchDramasUseCase.kt` | 新增 | 搜索结果查询 use case |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetHotSearchKeywordsUseCase.kt` | 新增 | 热搜查询 use case |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/ObserveSearchHistoryUseCase.kt` | 新增 | 历史订阅 use case |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/SaveSearchHistoryUseCase.kt` | 新增 | 历史保存 use case |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/ClearSearchHistoryUseCase.kt` | 新增 | 历史清空 use case |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt` | 修改 | 提供 DataStore / 本地数据源依赖 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 绑定 `SearchRepository` 实现 |
| `android/app/src/test/java/com/djs66256/short_drama/data/local/SearchHistoryLocalDataSourceTest.kt` | 新增 | 覆盖历史规则与损坏恢复 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/SearchRepositoryImplTest.kt` | 新增 | 覆盖 repository 映射与接线 |

### Step 4：先写搜索发现页 ViewModel 测试，再接首页入口与快捷入口 UI

- **关联测试**：T-02、T-07
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchHomeViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/search/ui/SearchHomeScreen.kt`
- **实现内容**：
  1. 先补 `SearchHomeViewModelTest`，覆盖初始化读取历史、加载热搜成功/失败、清空历史、点击历史词/热搜词/快捷入口后发出正确导航事件。
  2. 新增 `SearchHomeViewModel`，以 StateFlow 暴露 `draftQuery`、`history`、`hotSearches`、`hotSearchErrorMessage`、`quickEntries`，并保证热搜失败只影响区块状态。
  3. 修改 `HomeScreen.kt`，在现有首页 Feed 顶部增加搜索入口按钮，但不改动 `HomeDramaCard` 与首页列表主语义。
  4. 新增 `SearchHomeScreen.kt`，承载搜索框、快捷入口、历史区、热搜区；手动输入、历史词、热搜词三条触发链路统一导航到 `search/result?query=...`。
  5. 快捷入口页面优先复用现有 `feature/common/ui/PlaceholderScreen.kt` 的能力做 Native 承接，避免为占位页重复造通用组件。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.search.viewmodel.SearchHomeViewModelTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.navigation.RoutesTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 首页顶部新增搜索入口 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchHomeViewModel.kt` | 新增 | 管理历史、热搜、输入与快捷入口状态 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/ui/SearchHomeScreen.kt` | 新增 | 搜索发现页 Compose 容器 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/search/viewmodel/SearchHomeViewModelTest.kt` | 新增 | 覆盖发现页状态机与导航触发 |

### Step 5：先写搜索结果页 ViewModel 测试，再接结果页 UI 与播放/详情复用

- **关联测试**：T-03、T-04、T-07
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchResultViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/search/ui/SearchResultScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt`
- **实现内容**：
  1. 先补 `SearchResultViewModelTest`，覆盖首次加载、重搜、重复点击拦截、成功写历史、失败不写历史、empty/error/retry 状态切换。
  2. 新增 `SearchResultViewModel`，通过 `SavedStateHandle` 读取 route query，统一维护 `query`、`draftQuery`、`items`、`isLoading`、`errorMessage`、`hasLoadedOnce`、`isRetrying`。
  3. 新增 `SearchResultScreen.kt`，顶部保留可编辑搜索框，列表区复用现有 `HomeDramaCard`，继续走 `play/{videoId}` 与 `detail/{dramaId}`，不引入整卡点击新语义。
  4. 对 query 缺失、空白、超长、重复请求等异常做 ViewModel 级保护，确保不会发出空搜索，也不会让旧请求覆盖新关键词。
  5. 仅在需要与新 route arg 对齐时，轻微校准 `PlayerViewModel.kt` 或相关参数读取逻辑；若现有 `videoId/dramaId` 读取已满足要求，则保持不动。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.search.viewmodel.SearchResultViewModelTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.home.ui.HomeScreenTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchResultViewModel.kt` | 新增 | 管理结果页 query、搜索请求与历史写入时机 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/ui/SearchResultScreen.kt` | 新增 | 渲染 loading/content/empty/error 四态 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` | 校验/按需轻微修改 | 保持播放参数读取兼容 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/search/viewmodel/SearchResultViewModelTest.kt` | 新增 | 覆盖结果页状态机、重试与历史写入 |

### Step 6：做 Hilt/导航回归与统一验证收口

- **关联测试**：T-01 ～ T-07
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/di/*.kt`、`android/app/src/test/java/com/djs66256/short_drama/...`
- **实现内容**：
  1. 回查搜索页、结果页、快捷入口承接页在 `NavGraph.kt` 的注册与参数声明是否与 `AppDestination`、`SavedStateHandle`、`PendingRoute` 完全一致。
  2. 统一回归 Hilt DI，确保 SearchRepository、本地 DataStore、ViewModel 构造参数都可编译通过，不把 wiring 问题留到手工运行阶段。
  3. 执行 Android 端完整测试、构建与 detekt，记录已知限制：本期仅消费第一页、热搜失败为局部错误、`new-releases`/`actors` 仍为 Native 占位承接页。
- **验证方式**：
  - 运行 `cd android && ./gradlew test`
  - 运行 `cd android && ./gradlew assembleDebug`
  - 运行 `cd android && ./gradlew detekt`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 校验/收口 | 回归搜索链路、快捷入口承接页与主链路接线 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/*.kt` | 校验/收口 | 确认 Hilt 绑定与编译可用 |
| `android/app/src/test/java/com/djs66256/short_drama/...` | 修改/补齐 | 收口回归测试与遗漏 case |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5 ──▶ Step 6
```

## 验证总览

- [ ] 所有测试通过（`cd android && ./gradlew test`）
- [ ] 搜索相关核心单测通过（`RoutesTest`、`DeeplinkRouteParserTest`、`ApiServiceTest`、`SearchRemoteDataSourceTest`、`SearchHistoryLocalDataSourceTest`、`SearchHomeViewModelTest`、`SearchResultViewModelTest`）
- [ ] Build 成功（`cd android && ./gradlew assembleDebug`）
- [ ] 无新增 lint 错误（`cd android && ./gradlew detekt`）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 扩展 search/result/quick entry route 与 `PendingRoute` |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt` | 修改 | 扩展搜索相关 deeplink 解析 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt` | 修改 | 扩展 pending route 消费能力 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 注册搜索页、结果页与快捷入口承接页 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增搜索与热搜接口 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiClient.kt` | 修改 | 校准搜索接口 base URL 约束 |
| `android/app/build.gradle.kts` | 修改 | 校准 `api.base.url` 默认前缀 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/SearchRemoteDataSource.kt` | 新增 | 搜索/热搜远端数据源与错误解析 |
| `android/app/src/main/java/com/djs66256/short_drama/data/local/SearchHistoryLocalDataSource.kt` | 新增 | DataStore 搜索历史本地存储 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/SearchRepositoryImpl.kt` | 新增 | 搜索 repository 实现 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/SearchRepository.kt` | 新增 | 搜索 repository 接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/SearchDramasUseCase.kt` | 新增 | 搜索请求 use case |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetHotSearchKeywordsUseCase.kt` | 新增 | 热搜查询 use case |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/ObserveSearchHistoryUseCase.kt` | 新增 | 历史订阅 use case |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/SaveSearchHistoryUseCase.kt` | 新增 | 历史保存 use case |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/ClearSearchHistoryUseCase.kt` | 新增 | 历史清空 use case |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt` | 修改 | 提供 DataStore / 本地数据源 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 绑定 `SearchRepository` |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 首页顶部新增搜索入口 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchHomeViewModel.kt` | 新增 | 搜索发现页状态机 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/ui/SearchHomeScreen.kt` | 新增 | 搜索发现页 UI |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/viewmodel/SearchResultViewModel.kt` | 新增 | 搜索结果页状态机 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/search/ui/SearchResultScreen.kt` | 新增 | 搜索结果页 UI |
| `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` | 校验/按需轻微修改 | 保持播放参数读取兼容 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 路由生成测试 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/DeeplinkRouteParserTest.kt` | 修改 | deeplink 解析测试 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt` | 修改 | pending route 测试 |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/ApiServiceTest.kt` | 修改 | 搜索接口注解测试 |
| `android/app/src/test/java/com/djs66256/short_drama/data/datasource/SearchRemoteDataSourceTest.kt` | 新增 | 本地错误解析测试 |
| `android/app/src/test/java/com/djs66256/short_drama/data/local/SearchHistoryLocalDataSourceTest.kt` | 新增 | DataStore 历史规则测试 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/SearchRepositoryImplTest.kt` | 新增 | repository 接线测试 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/search/viewmodel/SearchHomeViewModelTest.kt` | 新增 | 搜索发现页 ViewModel 测试 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/search/viewmodel/SearchResultViewModelTest.kt` | 新增 | 搜索结果页 ViewModel 测试 |
