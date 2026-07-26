# 首页信息流 (Homepage Feed)

> 最后更新：2026-07-26
> 覆盖端：Android / iOS / Backend（Web 本期不实现）

## 功能概述

首页信息流在 PRD-01 已落地的多 Tab 应用壳之上，为移动端首页补齐首屏内容消费能力。当前 Android 与 iOS 冷启动进入首页后，会请求 Backend `GET /api/dramas` 的第一页数据，并按 loading / content / empty / error 四类状态渲染列表卡片；卡片主次动作继续复用既有 `play/:id` 与 `detail/:id` 路由语义。商城（mall）与赚钱（earn）仍按 H5 承载，不属于本期 Native 首页 Feed。

- **核心价值**：让用户冷启动后直接进入短剧浏览主链路，而不是停留在应用名/示例按钮占位页
- **覆盖范围**：Backend 首页 mock 列表接口、Android 首页 Feed、iOS 首页 Feed
- **当前状态**：移动端首页首屏 + Backend mock 数据已实现；Web 首页仍保持骨架

## 入口与路由

| 端 | 入口 | 路由 / 触发方式 | 源文件 |
|----|------|----------------|--------|
| Android | 默认首页 Tab | `home` graph 首屏自动触发 `loadIfNeeded()`；卡片动作跳转 `play/{videoId}` / `detail/{dramaId}` | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:117-163`, `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:41-80` |
| iOS | 默认首页 Tab | `HomeView.task` 自动触发 `loadIfNeeded()`；卡片动作跳转 `.player(videoId:)` / `.dramaDetail(dramaId:)` | `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:15-67`, `ios/ShortDrama/Sources/App/AppRoute.swift:4-29` |
| Backend | 首页列表接口 | `GET /api/dramas?page&pageSize` | `backend/src/app/api/dramas/route.ts:8-24` |
| Web | N/A | 首页仍为应用信息骨架，不消费 Feed | `web/src/features/home/HomeScreen.tsx:12-55` |

## 核心逻辑

### 流程：移动端首页首屏加载

1. 应用进入首页 Tab 后触发首屏加载。
   - Android 在 `HomeScreen` 首次组合时执行 `viewModel.loadIfNeeded()`（`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:47-52`）。
   - iOS 在 `HomeView.task` 中执行 `await viewModel.loadIfNeeded()`（`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:39-43`）。
2. ViewModel 固定请求第一页，每页 10 条。
   - Android `HomeViewModel` 使用 `FIRST_PAGE = 1`、`FEED_PAGE_SIZE = 10` 调用 `GetDramasUseCase`（`android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:50-66,102-106`）。
   - iOS `HomeViewModel` 使用 `Constants.firstPage = 1`、`pageSize = 10` 调用 `FetchDramasUseCase.execute(...)`（`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:14-17,72-81`）。
3. Data 层统一请求 canonical contract：`/api/dramas?page&pageSize`。
   - Android Retrofit 接口使用 `@Query("page")` 与 `@Query("pageSize")`（`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt:20-24`）。
   - iOS `DramaEndpoints.GetDramas` 使用 `/api/dramas` 与 `page/pageSize` query（`ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift:18-22,42-55`）。
4. Backend Route 校验分页参数后，通过 `DramaService -> DramaMockRepository` 返回 `{ data, pagination }`。
   - Route：`backend/src/app/api/dramas/route.ts:8-24`
   - Service：`backend/src/services/drama/drama.service.ts:5-10`
   - Mock 数据与分页切片：`backend/src/repositories/mock/drama.mock.repository.ts:4-180`
5. UI 按状态分支渲染首页。
   - Android：loading / error / empty / list 由 `HomeScreen` 的 `when` 分支驱动（`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:53-80`）。
   - iOS：loading / content / empty / error 由 `HomeView.viewState` 驱动（`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:15-44`）。
6. 卡片动作复用既有首页子路由。
   - Android 点击后导航到 `play/{id}` 与 `detail/{id}`（`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:71-76`, `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:70-76`）。
   - iOS 通过 `HomeRouteBuilder` 将 `drama.id` 映射到 `.player(videoId:)` 与 `.dramaDetail(dramaId:)`（`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:46-67`）。

### 边界与异常处理

| 场景 | 处理方式 | 源文件 |
|------|---------|--------|
| 首次加载成功但无数据 | Android / iOS 均进入空态，而不是回退到旧占位首页 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:66-67`, `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:78-81` |
| 首次加载失败 | 两端统一进入错误态，并暴露用户可触发的重试动作 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:71-95`, `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:83-87` |
| 重试期间重复点击 | Android 用 `requestInFlight` 拦截；iOS 用 `isRequestInFlight` 拦截 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:33-48`, `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:30-32,56-58` |
| 首页卡片 `id` 为空 | Android 仅允许非空 id 导航；iOS `HomeRouteBuilder` 返回 `nil` 阻止异常路由 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:158-170,270-287`, `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:57-67` |
| 分页参数非法 | Backend 返回 400，当前错误码实际为 `VALIDATION_ERROR` | `backend/src/app/api/__tests__/dramas.test.ts:76-92` |
| 大页码 | Backend 返回空数组，但保留正确分页元信息 | `backend/src/app/api/__tests__/dramas.test.ts:61-74`, `backend/src/repositories/mock/drama.mock.repository.ts:165-180` |

## 多端实现

### Android

- 首页状态源：`HomeUiState`（`isLoading` / `items` / `errorMessage` / `hasLoadedOnce` / `isRetrying`）定义在 `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:17-23`
- 首页视图：`HomeScreen` 用 `LazyColumn` 渲染卡片，错误态和空态内建在同文件中（`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:41-288`）
- 网络契约：`ApiService.getDramas(page, pageSize)` 已从 `page_size` 收口到 `pageSize`（`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt:20-24`）
- 自动化证据：`HomeViewModelTest`、`HomeScreenTest`、`RoutesTest` 覆盖状态机、卡片 meta 和路由映射（`android/app/src/test/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModelTest.kt:43-166`, `android/app/src/test/java/com/djs66256/short_drama/feature/home/ui/HomeScreenTest.kt:11-34`, `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt:14-34`）
- 实现特征：当前 Android 封面区仍使用 `DramaCoverPlaceholder` 展示占位视觉，不直接加载远程图片（`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:233-268`）

### iOS

- 首页状态源：`HomeViewModel.ViewState` 使用 `.loading / .content / .empty / .error` 明确建模（`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:7-12,21-23`）
- 首页视图：`HomeView` 根据 `viewState` 切换列表、空态、错误态与 loading（`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:15-44`）
- 网络契约：`DramaRemoteDataSource.fetchDramas` 已使用 `/api/dramas?page&pageSize` 并解码 `{ data, pagination }`（`ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift:18-22,42-55`）
- 自动化证据：`APIClientTests`、`DramaRepositoryTests`、`HomeViewModelTests`、`NavigationRouterTests` 覆盖 endpoint/query、canonical response 解码、状态机与路由映射（`ios/ShortDrama/Tests/DataTests/APIClientTests.swift:145-207`, `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift:41-67`, `ios/ShortDrama/Tests/ViewModelTests/HomeViewModelTests.swift:23-141`, `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift:35-55`）
- 实现特征：iOS 通过 `AsyncImage` 尝试展示封面，失败时回退到占位图（`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:159-193`）

### Backend

- Route：分页参数由 Zod 约束为 `page >= 1`、`1 <= pageSize <= 100`（`backend/src/app/api/dramas/route.ts:8-18`）
- Schema：`DramaSchema` 已统一到首页卡片字段集 `episode_count + tags`（`backend/src/lib/schemas.ts:15-25`）
- Repository：`HOMEPAGE_DRAMAS` 预置 12 条首页 mock 数据，并保留稳定分页顺序（`backend/src/repositories/mock/drama.mock.repository.ts:4-180`）
- 自动化证据：`dramas.test.ts`、`schemas.test.ts`、`drama.mock.repository.test.ts`、`drama.service.test.ts` 覆盖 contract、schema、分页与边界行为（`backend/src/app/api/__tests__/dramas.test.ts:6-106`, `backend/src/lib/__tests__/schemas.test.ts:44-167`, `backend/src/repositories/__tests__/drama.mock.repository.test.ts:17-113`, `backend/src/services/drama/drama.service.test.ts:18-69`）

### Web

- 首页仍展示应用信息与代表性链接，不消费首页 Feed（`web/src/features/home/HomeScreen.tsx:12-55`）
- 本期设计范围明确 Web 不实现首页信息流（`docs/specs/2026-07-25-prd-02-homepage-feed/spec.md:19-20,61-63`）

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/dramas` | [../../api/dramas.md](../../api/dramas.md) | 首页首屏 Feed 的唯一数据来源 |
| `POST /api/dramas` | [../../api/dramas.md](../../api/dramas.md) | 仍为 501 占位，不属于本期首页实现 |
| `GET /api/dramas/[id]` | [../../api/dramas.md](../../api/dramas.md) | 详情接口仍未实现，首页详情页目前只复用既有客户端占位路由 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Android `HomeUiState` | `MutableStateFlow` | 页面级 | 首页唯一状态源，聚合 loading / items / error / retry | `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:17-31` |
| Android `requestInFlight` | ViewModel 私有字段 | 页面级 | 阻止重复请求和重复重试 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:33-48` |
| iOS `viewState` | `@Published` | 页面级 | 首页列表、空态、错误态、loading 的统一来源 | `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:7-23` |
| iOS `hasLoaded` / `isRequestInFlight` | ViewModel 私有字段 | 页面级 | 避免重复自动请求和重试并发 | `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:30-32,41-58` |
| Backend `pagination` | JSON 响应 | 请求级 | 返回 `page / page_size / total / total_pages`，客户端本期只消费第一页 | `backend/src/repositories/interfaces/drama.repository.interface.ts:8-16`, `backend/src/repositories/mock/drama.mock.repository.ts:165-180` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 应用壳 | 默认承载容器 | 首页信息流依附于 PRD-01 的首页 Tab / home graph，而不是独立顶级入口 |
| 播放器 | 路由跳转 | 首页卡片主动作使用 `drama.id -> play/:id` 进入播放页占位能力 |
| 剧集详情 | 路由跳转 | 首页卡片次动作使用 `drama.id -> detail/:id` |
| 数据模型 | 共享字段约束 | 首页卡片字段与 `DramaSchema` / 客户端 Entity 保持对齐 |

### 外部依赖

| 服务 / 框架 | 用途 | 接入方式 |
|-------------|------|---------|
| Next.js Route Handlers | Backend 列表接口承载 | `backend/src/app/api/dramas/route.ts` |
| Retrofit | Android 首页拉取 Feed | `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` |
| URLSession + APIClient | iOS 首页拉取 Feed | `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| 本期只消费第一页 | 不支持下拉刷新、自动加载更多或第二页浏览 | 2026-07-26 | `HomeViewModel` / `GetDramasUseCase` 均固定使用 `page=1,pageSize=10` |
| Web 首页未实现 Feed | Web 仍只能查看应用骨架与示例链接 | 2026-07-26 | 属于明确范围外 |
| 商城 / 赚钱不属于 Native Feed | 首页 Feed 不覆盖 mall / earn 业务内容 | 2026-07-26 | `PRODUCT.md:22-25` 明确它们由 H5 承载 |
| Android 当前未加载真实封面图片 | Android 首页封面区仍是占位视觉，不是网络图片渲染 | 2026-07-26 | `DramaCoverPlaceholder` 仅根据 `coverUrl` 是否为空显示提示 |
| 设备级黑盒仍待补测 | 移动端真实点击、空态、错误态、重试恢复未在设备/模拟器上执行 | 2026-07-26 | `docs/specs/2026-07-25-prd-02-homepage-feed/qa-test.md:22-25,84-160,297-314` |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-26 | 初始创建：收录 PRD-02 首页信息流的移动端首屏状态机、Backend canonical contract 与卡片到播放/详情页的主路径实现 |

---
*本文档由 llm-wiki skill 自动维护。*