# 分类浏览 (Classification)

> 最后更新：2026-07-28
> 覆盖端：Android / iOS / Backend（Web 本期不实现）

## 功能概述

分类浏览能力在 PRD-04 搜索发现与 PRD-05 排行体系之后，继续补齐“按题材标签结构化找内容”的探索链路。当前 Android 与 iOS 已同时从搜索发现快捷入口和剧场频道快捷入口进入真实分类页；用户可在 `全部 / 男频 / 女频` 三个性别维度下浏览固定三组分类维度——`时代背景`、`主题情节`、`角色设定`——点击任一标签后不新建独立结果页，而是直接复用既有搜索结果页与 `GET /api/dramas/search` 主链路。Backend 同步提供 `GET /api/dramas/tags` 作为分类页唯一数据源，并把搜索命中规则从 `title + category` 扩展为 `title + category + tags`。剧场内点击“分类”时会直接切回首页所属导航栈承载该页面，不在剧场 tab 内重复实现分类页。Web 继续保持搜索相关占位页，不在本期实现真实分类浏览（`backend/src/app/api/dramas/tags/route.ts:7-18`、`backend/src/app/api/dramas/search/route.ts:7-19`、`android/app/src/main/java/com/djs66256/short_drama/feature/search/model/SearchQuickEntry.kt:18-39`、`android/app/src/main/java/com/djs66256/short_drama/navigation/TheaterShortcutRoute.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/classification/ui/ClassificationScreen.kt:58-127`、`ios/ShortDrama/Sources/Domain/Entities/QuickEntry.swift:20-27`、`ios/ShortDrama/Sources/Features/Theater/ViewModels/TheaterViewModel.swift`、`ios/ShortDrama/Sources/Features/Classification/Views/ClassificationHomeView.swift:18-69`、`web/src/app/search/page.tsx:1-10`）。

- **核心价值**：把搜索发现从“输入关键词找内容”扩展为“按分类标签结构化浏览并继续进入搜索结果”
- **覆盖范围**：Backend 分类标签接口与 tags 搜索扩展、Android 分类页、iOS 分类页、搜索发现快捷入口到搜索结果页的导航链路
- **当前状态**：Backend + Android + iOS 已实现；Web 继续保持占位页

## 入口与路由

| 端 | 入口 | 路由 / 触发方式 | 源文件 |
|----|------|----------------|--------|
| Android | 搜索发现快捷入口“分类” | `AppDestination.classification()`，路由为 `classification` | `android/app/src/main/java/com/djs66256/short_drama/feature/search/model/SearchQuickEntry.kt:18-39`、`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:102-115` |
| Android | Deeplink | `djsdrama://classification`，进入 `PendingRoute.Classification` 后导航至分类页 | `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:123-133`、`android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt:33-43` |
| iOS | 搜索发现快捷入口“分类” | `QuickEntryType.classification -> .classificationHome` | `ios/ShortDrama/Sources/Domain/Entities/QuickEntry.swift:20-27`、`ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift:76-87` |
| iOS | Deeplink | `djsdrama://classification`，解析为 `.classificationHome` | `ios/ShortDrama/Sources/App/AppRoute.swift:39-60`、`ios/ShortDrama/Sources/App/DeeplinkHandler.swift:26-45` |
| Android / iOS | 标签点击后的结果承接 | 点击标签后统一复用搜索结果页：Android `search/result?query={query}`，iOS `.searchResult(query:)` | `android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt:104-107`、`ios/ShortDrama/Sources/Features/Classification/Views/ClassificationHomeView.swift:65-68` |
| Backend | 分类标签接口 | `GET /api/dramas/tags?gender=all|male|female` | `backend/src/app/api/dramas/tags/route.ts:7-18` |
| Web | 占位页入口 | 仅保留 `/search` 页面语义，不提供真实分类页 | `web/src/app/search/page.tsx:1-10` |

### Deeplink / canonical naming

- Android canonical route 定义为 `classification`，并继续与 `search`、`search/result`、`ranking` 等搜索发现路由同处 HOME graph（`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt:48-53,102-115`）。
- iOS canonical `publicRouteName` 已把 `.classificationHome` 映射为 `classification`，归属于 `home` Tab（`ios/ShortDrama/Sources/App/AppRoute.swift:24-37,39-60`）。
- 两端 deeplink 均支持 `djsdrama://classification`，外部唤起后直接进入分类页，不再停留在占位承接（`android/app/src/main/java/com/djs66256/short_drama/navigation/DeeplinkRouteParser.kt:33-43`、`ios/ShortDrama/Sources/App/DeeplinkHandler.swift:26-45`）。

## 核心逻辑

### 流程：从搜索发现进入分类页

1. 搜索发现页快捷入口触发分类路由，而不是新增一级 Tab。
   - Android `defaultSearchQuickEntries()` 固定暴露“分类”入口，并把 route 指向 `AppDestination.classification()`（`android/app/src/main/java/com/djs66256/short_drama/feature/search/model/SearchQuickEntry.kt:18-39`）。
   - iOS `QuickEntry.defaults` 固定包含 `.classification`，`SearchHomeViewModel.route(for:)` 会映射到 `.classificationHome`（`ios/ShortDrama/Sources/Domain/Entities/QuickEntry.swift:20-27`、`ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift:76-87`）。
2. 应用壳在首页频道内承载真实分类页。
   - Android `NavGraph` 已将 `classification` route 绑定到 `ClassificationScreen`，并通过 `onOpenSearchResult` 复用搜索结果页导航（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:211-218`）。
   - iOS `TabNavigationHostView` 已将 `.classificationHome` 绑定到 `ClassificationHomeView()`（`ios/ShortDrama/Sources/App/TabNavigationHostView.swift:19-23`）。
3. 分类页首屏请求 `GET /api/dramas/tags`，默认按 `gender=all` 拉取固定三维度数据。
   - Backend Route 用 `ClassificationTagsQuerySchema` 解析 query，并通过 `DramaService.listClassificationTags()` 返回响应（`backend/src/app/api/dramas/tags/route.ts:7-18`、`backend/src/services/drama/drama.service.ts:47-57`）。
   - Android `ClassificationRemoteDataSource.getClassificationTags()` 调用 `ApiService.getDramaTags(gender)`（`android/app/src/main/java/com/djs66256/short_drama/data/datasource/ClassificationRemoteDataSource.kt:13-21`）。
   - iOS `DramaRemoteDataSource.fetchClassificationTags(gender:)` 调用 `/api/dramas/tags`（`ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift:65-75,171-175`）。

### 流程：分类页渲染与维度切换

1. 分类页固定展示三个性别 Tab：`全部 / 男频 / 女频`。
   - Android `ClassificationGenderTabs` 直接遍历 `ClassificationGender.entries` 渲染顶部 TabRow（`android/app/src/main/java/com/djs66256/short_drama/feature/classification/ui/ClassificationScreen.kt:129-143`）。
   - iOS `ClassificationGenderTabBar` 由 `ClassificationHomeView` 承载，切换后异步调用 `viewModel.selectGender(_:)`（`ios/ShortDrama/Sources/Features/Classification/Views/ClassificationHomeView.swift:19-24`）。
2. 页面主体固定为左侧维度导航 + 右侧标签区，而不是列表页直接平铺标签。
   - Android `ClassificationBody` 固定渲染 `ClassificationDimensionRail + ClassificationSectionList` 两栏布局（`android/app/src/main/java/com/djs66256/short_drama/feature/classification/ui/ClassificationScreen.kt:187-209`）。
   - iOS `ClassificationHomeView` 在 `ClassificationStateView` content 中固定渲染 `ClassificationDimensionRail + ClassificationTagSectionList`（`ios/ShortDrama/Sources/Features/Classification/Views/ClassificationHomeView.swift:26-50`）。
3. 分类维度顺序是强约束，始终保持 `era_background -> theme_plot -> character_setting`，空维度也要保留。
   - Backend `CLASSIFICATION_DIMENSION_KEYS` 与 `ClassificationDimensionsSchema` 强制维度数量和顺序（`backend/src/lib/schemas.ts:99-150`）。
   - iOS `ClassificationTagsPayloadDTO.normalizedDimensions()` 会按 `ClassificationDimensionKey.allCases` 兜底补齐缺失维度为空标签（`ios/ShortDrama/Sources/Data/DTOs/ClassificationTagsResponseDTO.swift:25-41`）。
   - Android 初始空态也会用 `ClassificationDimensionKey.entries` 构造三组空维度，避免切换时丢失左侧结构（`android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt:237-245`）。
4. 切换 gender 时页面会重置到首个维度，并只消费最后一次请求结果，避免快速切换导致旧结果覆盖新状态。
   - Android 通过 `nextRequestToken / activeRequestToken / latestGender` 仅消费最新有效请求，并在成功后把选中维度重置为首项（`android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt:69-75,109-118,165-186,228-235`）。
   - iOS 用 `requestToken` 拦截过期响应，并在成功后把 `selectedDimension` 重置为首个维度；必要时触发 `scrollResetSeed` 强制右侧列表回滚（`ios/ShortDrama/Sources/Features/Classification/ViewModels/ClassificationViewModel.swift:24-31,78-104`）。
5. 左侧维度选择与右侧可见 section 保持双向同步。
   - Android 通过 `ClassificationEffect.ScrollToDimension` 驱动列表滚动，并在 `snapshotVisibleDimensionKey(...)` 中把当前可见维度回写到 ViewModel（`android/app/src/main/java/com/djs66256/short_drama/feature/classification/ui/ClassificationScreen.kt:70-91`）。
   - iOS `ClassificationTagSectionList` 通过 `onVisibleDimensionChange` 回写可见维度，同时点击左侧 rail 会更新 `scrollTarget`（`ios/ShortDrama/Sources/Features/Classification/Views/ClassificationHomeView.swift:32-48`）。

### 流程：点击标签后复用搜索结果页

1. 点击任一标签后，先对标签文案做与搜索一致的 trim / 长度校验。
   - Android `buildSearchRoute(rawTag)` 内部复用 `normalizeSearchQueryOrNull(rawTag)`，不合法时直接返回 `null`（`android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt:104-107`）。
   - iOS `normalizedTagQuery(_:)` 统一要求 trim 后非空且长度不超过 50（`ios/ShortDrama/Sources/Features/Classification/ViewModels/ClassificationViewModel.swift:70-76`）。
2. 通过合法标签构建搜索结果路由，而不是单独定义 `classification/result` 新页面。
   - Android 直接返回 `AppDestination.searchResult(normalizedQuery)`（`android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt:104-107`）。
   - iOS 在 `handleTapTag(_:)` 中直接 `router.navigate(to: .searchResult(query: query))`（`ios/ShortDrama/Sources/Features/Classification/Views/ClassificationHomeView.swift:65-68`）。
3. Backend 搜索命中规则已扩展为 `title + category + tags`，因此分类标签点击后能通过现有搜索接口命中对应内容。
   - Mock repository 搜索会同时检查标题、分类与 tags 列表的 contains 匹配（`backend/src/repositories/mock/drama.mock.repository.ts:426-440`）。
   - Supabase repository 搜索表达式也已扩展为 `title.ilike / category.ilike / tags.cs` 三路匹配（`backend/src/repositories/supabase/drama.supabase.repository.ts:259-304`）。

### 后端分类标签生成规则

1. `GET /api/dramas/tags` 只接受 `gender=all|male|female`，默认值为 `all`（`backend/src/lib/schemas.ts:101-111`）。
2. 维度名固定为：`时代背景`、`主题情节`、`角色设定`（`backend/src/repositories/mock/drama.mock.repository.ts:249-253`、`backend/src/repositories/supabase/drama.supabase.repository.ts:71-75`）。
3. `male` 与 `female` 各自维护独立种子标签集；`all` 会按 `male` 在前、`female` 在后的顺序去重合并（`backend/src/repositories/mock/drama.mock.repository.ts:255-289,371-410`、`backend/src/repositories/supabase/drama.supabase.repository.ts:77-160`）。
4. 即使某维度当前无标签，也不会省略该维度；例如 `female.character_setting` 当前返回空数组（`backend/src/repositories/mock/drama.mock.repository.ts:285-288`、`backend/src/repositories/supabase/drama.supabase.repository.ts:107-110`）。

### 边界与异常处理

| 场景 | 处理方式 | 源文件 |
|------|---------|--------|
| `gender` 非法 | Backend 直接返回 400 `VALIDATION_ERROR` | `backend/src/lib/schemas.ts:101-111` |
| 首屏加载失败 | Android / iOS 都进入错误态并提供重试入口 | `android/app/src/main/java/com/djs66256/short_drama/feature/classification/ui/ClassificationScreen.kt:159-165`、`android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt:188-207`、`ios/ShortDrama/Sources/Features/Classification/ViewModels/ClassificationViewModel.swift:95-103` |
| 快速切换 gender 导致旧请求晚返回 | Android 使用 request token + latestGender，iOS 使用 `requestToken`，只消费最后一次请求结果 | `android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt:109-118,165-186,233-235`、`ios/ShortDrama/Sources/Features/Classification/ViewModels/ClassificationViewModel.swift:24-31,78-104` |
| 返回缺少某个维度 | iOS DTO 层按固定维度顺序补空维度；Android ViewModel 初始态始终保留三组空维度 | `ios/ShortDrama/Sources/Data/DTOs/ClassificationTagsResponseDTO.swift:33-41`、`android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt:237-245` |
| 标签为空或超长 | 客户端直接拦截，不导航到搜索结果页 | `android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt:104-107`、`ios/ShortDrama/Sources/Features/Classification/ViewModels/ClassificationViewModel.swift:70-76` |
| Web 端访问搜索相关页面 | 仅能看到占位页，不提供分类浏览真实能力 | `web/src/app/search/page.tsx:1-10` |

## 多端实现

### Android

- 分类页与搜索结果页都挂在 HOME graph 内，不新增顶级 Tab（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:149-218`）。
- 分类远端能力由 `ClassificationRemoteDataSource -> ClassificationRepositoryImpl -> GetClassificationTagsUseCase` 提供（`android/app/src/main/java/com/djs66256/short_drama/data/datasource/ClassificationRemoteDataSource.kt:13-21`、`android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt:40-45`）。
- ViewModel 负责 gender 切换、维度选中、右侧滚动同步、搜索结果路由构建与并发保护（`android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt:27-252`）。
- `ClassificationScreen` 负责顶部 Tab、双栏布局、loading/error/refreshing 叠层与标签点击交互（`android/app/src/main/java/com/djs66256/short_drama/feature/classification/ui/ClassificationScreen.kt:58-260`）。

### iOS

- 分类页挂在 home tab 的 `NavigationStack` 上，由 `TabNavigationHostView` 注册（`ios/ShortDrama/Sources/App/TabNavigationHostView.swift:19-23`）。
- `ClassificationHomeView` 负责性别切换、左侧 rail、右侧 section list 与标签点击后跳转搜索结果（`ios/ShortDrama/Sources/Features/Classification/Views/ClassificationHomeView.swift:18-69`）。
- `ClassificationViewModel` 负责 request token 并发保护、选中维度同步、错误态与标签 query 规范化（`ios/ShortDrama/Sources/Features/Classification/ViewModels/ClassificationViewModel.swift:5-109`）。
- Data / Domain 侧已补齐 `ClassificationTagsResponseDTO`、`ClassificationTagsPayload`、`FetchClassificationTagsUseCase` 与仓库实现，保证分类接口契约完整落地（`ios/ShortDrama/Sources/Data/DTOs/ClassificationTagsResponseDTO.swift:4-48`、`ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift:65-75,171-175`）。

### Backend

- Route：新增 `GET /api/dramas/tags`，使用 `withErrorHandler` 统一错误格式（`backend/src/app/api/dramas/tags/route.ts:1-18`）。
- Service：`DramaService` 扩展 `listClassificationTags()`，并用 `ClassificationTagsResponseSchema` 再次校验 repository 输出（`backend/src/services/drama/drama.service.ts:47-57`）。
- Repository Contract：`DramaRepositoryInterface` 扩展 `ClassificationTagsQuery`、`ClassificationTagsResult`、`listClassificationTags()`（`backend/src/repositories/interfaces/drama.repository.interface.ts:26-33,56-67`）。
- Schema：`ClassificationGenderSchema`、`ClassificationDimensionSchema`、`ClassificationDimensionsSchema`、`ClassificationTagsResponseSchema` 定义了分类页接口的权威形态（`backend/src/lib/schemas.ts:99-152`）。
- 当前运行时 route 仍直接实例化 `DramaMockRepository()`；Supabase repository 已补齐同等 classification / tags 搜索能力，但还不是当前 route 的实际数据路径（`backend/src/app/api/dramas/tags/route.ts:13-15`、`backend/src/repositories/supabase/drama.supabase.repository.ts:53-160,292-328`）。

### Web

- Web 继续只保留 `/search` 占位页，没有单独 `/classification` 页面，也不消费 `/api/dramas/tags`（`web/src/app/search/page.tsx:1-10`）。
- 这与当前“除 mall / earn 外其余业务页优先按 Native 承接”的范围约束一致（`PRODUCT.md:22-25`）。

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/dramas/tags` | [../../api/dramas.md](../../api/dramas.md) | 分类页唯一数据源，返回固定三维度标签矩阵 |
| `GET /api/dramas/search` | [../../api/dramas.md](../../api/dramas.md) | 标签点击后的结果承接接口，现已支持 `title + category + tags` 匹配 |
| `GET /api/dramas` | [../../api/dramas.md](../../api/dramas.md) | 与分类结果页复用同一基础 `Drama` 字段契约 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Android `ClassificationUiState` | `MutableStateFlow` | 页面级 | 聚合 `selectedGender / selectedDimensionKey / dimensions / isLoading / isRefreshing / errorMessage` | `android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt:27-35,55-61` |
| Android `effects` | `MutableSharedFlow<ClassificationEffect>` | 页面级 | 驱动右侧列表滚动到左侧当前选中维度 | `android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt:63-67` |
| Android request token | ViewModel 私有字段 | 页面级 | 防止 gender 快切时旧请求覆盖新状态 | `android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt:69-72,228-235` |
| iOS `selectedGender / selectedDimension / viewState` | `@Published` | 页面级 | 分类页唯一状态源，承载当前 gender、维度与内容态 | `ios/ShortDrama/Sources/Features/Classification/ViewModels/ClassificationViewModel.swift:17-20` |
| iOS `scrollResetSeed` | `@Published` | 页面级 | 触发右侧 section list 在切换 gender / retry 后回滚到首个维度 | `ios/ShortDrama/Sources/Features/Classification/ViewModels/ClassificationViewModel.swift:17-20,92-94` |
| iOS request token | ViewModel 私有字段 | 页面级 | 避免过期请求覆盖最新分类数据 | `ios/ShortDrama/Sources/Features/Classification/ViewModels/ClassificationViewModel.swift:24-31,78-104` |
| Backend classification response | JSON 响应 | 请求级 | 返回 `gender + dimensions[]`，dimensions 始终固定 3 组 | `backend/src/lib/schemas.ts:141-152` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 搜索发现 | 入口来源 | 分类页从搜索发现快捷入口进入，不新增一级频道 |
| 应用壳 | Native 容器承载 | Android / iOS 分类页都挂在首页 Tab / home graph 下 |
| 搜索结果 | 路由复用 | 标签点击后统一复用 `search/result` 结果页，而不是新建分类结果页 |
| 深链 | 外部入口 | `djsdrama://classification` 可直接打开分类页 |
| 数据模型 | 共享字段约束 | 分类接口与搜索结果页都依赖统一的 `Drama` / classification schema |

### 外部依赖

| 服务 / 框架 | 用途 | 接入方式 |
|-------------|------|---------|
| Next.js Route Handlers | Backend 分类标签与搜索接口承载 | `backend/src/app/api/dramas/tags/route.ts`、`backend/src/app/api/dramas/search/route.ts` |
| Retrofit | Android 拉取分类标签 | `android/app/src/main/java/com/djs66256/short_drama/data/datasource/ClassificationRemoteDataSource.kt` |
| URLSession + APIClient | iOS 拉取分类标签 | `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` |
| Zod | Backend 分类 query / response 校验 | `backend/src/lib/schemas.ts` |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| Web 不实现真实分类页 | Web 只能看到搜索占位页，无法按分类浏览内容 | 2026-07-27 | 属于明确范围外，符合 Native 页面优先策略 |
| Backend 运行时仍使用 mock repository | 分类标签与 tags 搜索当前来自内置种子，不具备真实内容后台和动态运营能力 | 2026-07-27 | Supabase repository 已补齐能力，但 route 仍未切换 |
| 分类标签集合仍是固定种子 | 不能自动从真实内容 tags 聚合出分类矩阵 | 2026-07-27 | male/female/all 都由代码内 seed 生成 |
| 标签点击后仍走普通搜索结果页 | 暂无独立的分类结果页排序、筛选或统计能力 | 2026-07-27 | 这是当前明确设计决策 |
| 设备级黑盒测试未执行 | 当前对真实点击、滚动联动、gender 快切的结论仍以代码与自动化检查为主 | 2026-07-27 | `docs/specs/2026-07-27-prd-06-classification/qa-test.md` |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-28 | 更新：补充 PRD-12 剧场“分类”快捷入口会切回首页所属导航栈复用既有分类页，而不是在剧场 tab 内重复实现分类页面 |
| 2026-07-27 | 初始创建：收录 PRD-06 分类浏览的搜索发现入口、固定三维度标签矩阵、`GET /api/dramas/tags`、tags 搜索扩展、两端分类页状态机与搜索结果页复用策略 |

---
*本文档由 llm-wiki skill 自动维护。*