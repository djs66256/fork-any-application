# 实现计划：iOS — PRD-04 搜索发现

> 创建日期：2026-07-26
> 对应技术方案：design-ios.md
> 对应需求：spec.md

## 概述

本期 iOS 端将在现有 `NavigationStack + NavigationRouter + MVVM + Clean Architecture` 基础上，补齐“首页搜索入口 → 搜索发现页 → 搜索结果页 → 播放/详情页”的 Native 搜索主链路，并为排行、分类、新剧、演员快捷入口提供受控承接页。实现重点包括：扩展 `AppRoute` / deeplink 语义、为搜索页与结果页补充 ViewModel 状态机、把搜索/热搜请求接入现有 `APIClient + DramaRepository`、使用 `UserDefaults` 落地本地搜索历史，以及用 Swift Testing 为导航、ViewModel、网络接线和本地历史规则建立最小自动化验收面。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 搜索相关路由与 deeplink 解析正确 | `.searchHome`、`.searchResult(query: "逆袭")`、`djsdrama://search/result/%E9%80%86%E8%A2%AD` 等输入 | Route 归属 `home` tab，deeplink 分别解析到 `.searchHome` / `.searchResult(query:)` / `.rankingHome` / `.classificationHome` / `.newReleases` / `.actorHub` | 单元测试 | P0 |
| T-02 | 搜索发现页首次加载成功 | 本地历史返回 2 条，热搜接口返回 3 条 | `SearchHomeViewModel` 正确暴露历史、快捷入口和 `.content` 热搜状态 | 单元测试 | P0 |
| T-03 | 搜索发现页局部失败不阻塞使用 | 历史读取成功，但热搜请求失败 | 历史仍正常显示，`hotSearchState == .error`，手动搜索与快捷入口仍可用 | 单元测试 | P0 |
| T-04 | 本地搜索历史满足去重/裁剪/清空规则 | 连续保存重复词、超过 10 条，或执行清空 | 历史按 trim 后去重、最近优先、最多 10 条、清空后为空 | 单元测试 | P0 |
| T-05 | 搜索结果请求正确接到搜索与热搜 API | 构造 `DramaEndpoints.SearchDramas(query: "逆袭", page: 1, pageSize: 10)` 与 `GetHotSearches()` | path/query 与 shared design 一致，Repository/DataSource 可正确映射响应 | 单元测试 | P0 |
| T-06 | 搜索结果页成功后写历史 | 搜索接口成功返回非空结果或空结果 | `SearchResultViewModel` 分别进入 `content` / `empty`，并调用保存历史 | 单元测试 | P0 |
| T-07 | 搜索结果页失败不写历史且支持重试/重搜 | 搜索接口报错，随后点击重试或改词再次提交 | 先进入 `error`，失败不写历史；重试或新 query 可进入新的 loading 并更新最终状态 | 单元测试 | P0 |
| T-08 | 重复搜索请求被去抖或取消旧请求 | 同 query 连续点击，或短时间内提交不同 query | 同 query 不重复发起请求；不同 query 仅保留最后一次有效结果 | 单元测试 | P1 |

## 实现步骤

### Step 1：先补导航测试，再扩展搜索相关 route 与首页入口

- **关联测试**：T-01
- **目标文件**：`ios/ShortDrama/Sources/App/AppRoute.swift`、`ios/ShortDrama/Sources/App/NavigationRouter.swift`、`ios/ShortDrama/Sources/App/DeeplinkHandler.swift`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/DeeplinkHandlerTests.swift`
- **实现内容**：
  1. 先在现有 `NavigationRouterTests` 与 `DeeplinkHandlerTests` 中补齐搜索首页、搜索结果、排行、分类、新剧、演员六类 route/deeplink 场景，锁定 `owningTab == .home` 与 `djsdrama://search/result/{query}` 的解析规则。
  2. 在 `AppRoute`、`NavigationRouter`、`DeeplinkHandler` 中新增 `.searchHome`、`.searchResult(query:)`、`.rankingHome`、`.classificationHome`、`.newReleases`、`.actorHub`，保持搜索相关页面都由 Home Tab 承载。
  3. 在 `TabNavigationHostView` 注册搜索页、结果页与发现承接页的 `navigationDestination`，避免实现时出现“route 已定义但无法入栈”的断层。
  4. 在 `HomeView` 右上角增加搜索入口按钮，仅负责 `router.navigate(to: .searchHome)`，不把热搜、历史预取逻辑塞回首页。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-01 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 新增搜索与快捷入口 route 定义 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 支持搜索相关页面入栈，保持归属 Home Tab |
| `ios/ShortDrama/Sources/App/DeeplinkHandler.swift` | 修改 | 扩展 `search/result/ranking/classification/new-releases/actors` deeplink 解析 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 注册搜索页、结果页与承接页导航目标 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 首页右上角新增搜索入口 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 补搜索相关 route 行为测试 |
| `ios/ShortDrama/Tests/ViewModelTests/DeeplinkHandlerTests.swift` | 修改 | 补搜索相关 deeplink 测试 |

### Step 2：先写发现页状态与历史规则测试，再落地 SearchHomeViewModel 和本地持久化

- **关联测试**：T-02、T-03、T-04
- **目标文件**：`ios/ShortDrama/Sources/Domain/Entities/HotSearchItem.swift`、`ios/ShortDrama/Sources/Domain/Entities/SearchHistoryItem.swift`、`ios/ShortDrama/Sources/Domain/RepositoryProtocols/SearchHistoryRepositoryProtocol.swift`、`ios/ShortDrama/Sources/Domain/UseCases/FetchHotSearchesUseCase.swift`、`ios/ShortDrama/Sources/Domain/UseCases/LoadSearchHistoryUseCase.swift`、`ios/ShortDrama/Sources/Domain/UseCases/SaveSearchHistoryUseCase.swift`、`ios/ShortDrama/Sources/Domain/UseCases/ClearSearchHistoryUseCase.swift`、`ios/ShortDrama/Sources/Data/Repositories/UserDefaultsSearchHistoryRepository.swift`、`ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift`、`ios/ShortDrama/Sources/Features/Search/Views/SearchHomeView.swift`、`ios/ShortDrama/Tests/DataTests/UserDefaultsSearchHistoryRepositoryTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/SearchHomeViewModelTests.swift`
- **实现内容**：
  1. 先补 `UserDefaults` 历史仓库测试与 `SearchHomeViewModel` 测试，覆盖读取历史、热搜成功、热搜失败局部降级、重复词去重、最多 10 条、清空历史等规则。
  2. 在 Domain/Data 层补 `HotSearchItem`、`SearchHistoryItem`、历史仓库协议与相关 use case，保证 ViewModel 依赖 use case 或 protocol，而不直接操作 `UserDefaults`。
  3. 用 `UserDefaultsSearchHistoryRepository` 实现 trim、去重、倒序、最多 10 条、解码失败降级为空并清理坏数据的逻辑，满足本地历史要求。
  4. 新增 `SearchHomeViewModel` 与 `SearchHomeView`，承载 query、快捷入口、历史、热搜和局部错误重试；发现页只负责准备搜索与导航，不直接执行搜索结果请求。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-02、T-03、T-04 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Domain/Entities/HotSearchItem.swift` | 新增 | 热搜实体 |
| `ios/ShortDrama/Sources/Domain/Entities/SearchHistoryItem.swift` | 新增 | 本地历史实体 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/SearchHistoryRepositoryProtocol.swift` | 新增 | 搜索历史读写协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchHotSearchesUseCase.swift` | 新增 | 获取热搜用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/LoadSearchHistoryUseCase.swift` | 新增 | 读取历史用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/SaveSearchHistoryUseCase.swift` | 新增 | 保存历史用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/ClearSearchHistoryUseCase.swift` | 新增 | 清空历史用例 |
| `ios/ShortDrama/Sources/Data/Repositories/UserDefaultsSearchHistoryRepository.swift` | 新增 | 基于 `UserDefaults` 的历史持久化实现 |
| `ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift` | 新增 | 搜索发现页状态机 |
| `ios/ShortDrama/Sources/Features/Search/Views/SearchHomeView.swift` | 新增 | 搜索发现页 UI |
| `ios/ShortDrama/Tests/DataTests/UserDefaultsSearchHistoryRepositoryTests.swift` | 新增 | 历史去重、裁剪、清空测试 |
| `ios/ShortDrama/Tests/ViewModelTests/SearchHomeViewModelTests.swift` | 新增 | 搜索发现页加载、局部错误、快捷入口测试 |

### Step 3：先补网络接线与结果页状态机测试，再实现 SearchResultViewModel 与搜索 API 接线

- **关联测试**：T-05、T-06、T-07、T-08
- **目标文件**：`ios/ShortDrama/Sources/Core/Network/APIClient.swift`、`ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift`、`ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift`、`ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift`、`ios/ShortDrama/Sources/Domain/UseCases/SearchDramasUseCase.swift`、`ios/ShortDrama/Sources/Features/Search/ViewModels/SearchResultViewModel.swift`、`ios/ShortDrama/Sources/Features/Search/Views/SearchResultView.swift`、`ios/ShortDrama/Tests/DataTests/APIClientTests.swift`、`ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/SearchResultViewModelTests.swift`
- **实现内容**：
  1. 先在数据层测试中锁定 `GET /api/dramas/search?q&page&pageSize` 与 `GET /api/dramas/hot-search` 的 endpoint 结构、DTO 解码与错误包体兼容，确保 coding 阶段不会偏离 shared design。
  2. 扩展 `DramaRepositoryProtocol`、`DramaRemoteDataSource`、`DramaRepository` 与 `SearchDramasUseCase`，把搜索和热搜能力接到现有 `APIClient + URLSession`，不引入新的网络基础设施。
  3. 在 `APIClient` 兼容 `{ error: { code, message } }` 错误包体，同时保持现有顶层 `message` 结构可继续工作，避免搜索新接口与现有接口冲突。
  4. 新增 `SearchResultViewModel` 与 `SearchResultView`，统一处理 loading / content / empty / error 四态、失败不写历史、成功后写历史、同 query 去抖、不同 query 取消旧请求并保留最后一次结果。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-05 ～ T-08 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 修改 | 兼容 nested error envelope 与既有错误结构 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 新增 search 与 hot-search 请求 |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 修改 | 实现搜索与热搜映射 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 修改 | 扩展搜索与热搜协议能力 |
| `ios/ShortDrama/Sources/Domain/UseCases/SearchDramasUseCase.swift` | 新增 | 搜索短剧用例 |
| `ios/ShortDrama/Sources/Features/Search/ViewModels/SearchResultViewModel.swift` | 新增 | 搜索结果页状态机与写历史策略 |
| `ios/ShortDrama/Sources/Features/Search/Views/SearchResultView.swift` | 新增 | 搜索结果页 UI |
| `ios/ShortDrama/Tests/DataTests/APIClientTests.swift` | 修改 | 搜索/热搜 endpoint 与错误包体测试 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 修改 | 搜索与热搜映射测试 |
| `ios/ShortDrama/Tests/ViewModelTests/SearchResultViewModelTests.swift` | 新增 | 成功/空态/失败/重试/去抖测试 |

### Step 4：补齐发现承接页与结果列表复用，完成端到端页面拼装回归

- **关联测试**：T-01、T-02、T-06、T-07
- **目标文件**：`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`、`ios/ShortDrama/Sources/Features/Search/Views/Components/*.swift`、`ios/ShortDrama/Sources/Features/Search/Views/DiscoveryPlaceholderView.swift`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`
- **实现内容**：
  1. 从现有首页视图中抽取或复用 `HomeDramaCardView`，让搜索结果页只复用“观看 / 详情”动作语义，不额外定义新的整卡点击能力。
  2. 新增 `DiscoveryPlaceholderView`，把排行、分类、新剧、演员入口统一接到受控 Native 承接页，其中 `new-releases` 与 `actors` 明确保持首版占位，不擅自扩到真实业务逻辑。
  3. 完成搜索栏、历史区、热搜区、快捷入口、结果列表等页面组件拼装，确保首页入口、发现页、结果页、播放/详情页在导航上可连续衔接。
  4. 回看测试覆盖，确认导航、ViewModel、网络接线、本地历史四个面都已被自动化覆盖，再进入 coding 阶段。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改/抽取复用 | 复用首页 Drama 卡片给搜索结果页 |
| `ios/ShortDrama/Sources/Features/Search/Views/Components/*.swift` | 新增 | 搜索栏、历史、热搜、快捷入口、结果列表组件 |
| `ios/ShortDrama/Sources/Features/Search/Views/DiscoveryPlaceholderView.swift` | 新增 | 排行/分类/新剧/演员承接页 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 串起搜索页、结果页与承接页渲染 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 复核/补充 | 回归首页入口到搜索主链路的导航语义 |

### Step 5：执行 iOS 回归验证，固化 coding 阶段验收基线

- **关联测试**：T-01 ～ T-08
- **目标文件**：`docs/specs/2026-07-26-prd-04-search-discovery/plan-ios.md`
- **实现内容**：
  1. 在 coding 阶段按本计划顺序推进：先测试、后实现，所有新增业务逻辑必须同步补测试，避免搜索页、结果页和历史逻辑仅靠手点验证。
  2. 执行全量 iOS test/build，并在新增源码后运行 `xcodegen generate`，保证 Search 模块文件被工程自动纳入。
  3. 若实现中出现某些 UI 细节难以直接单测，优先保证 ViewModel、Repository、Router、Deeplink 的自动化覆盖完整，不新增第三方测试框架。
  4. 将 coding 阶段的收口标准固定为：搜索入口可达、导航语义稳定、ViewModel 状态完整、网络接线正确、本地历史规则可回归。
- **验证方式**：
  - 运行 `cd ios && xcodegen generate`
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - 运行 `cd ios && swiftlint lint`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `docs/specs/2026-07-26-prd-04-search-discovery/plan-ios.md` | 新增 | 固化 iOS 搜索发现实现步骤、测试场景与验收基线 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5
```

## 验证总览

- [ ] 所有测试通过（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [ ] Build 成功（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [ ] 无新增 lint 错误（`cd ios && swiftlint lint`）
- [ ] 新增源码后已执行 `cd ios && xcodegen generate`
- [ ] 导航覆盖首页搜索入口、搜索发现页、搜索结果页、排行/分类/新剧/演员承接页
- [ ] ViewModel 覆盖搜索发现页与结果页的 loading / content / empty / error 关键状态
- [ ] 网络层已接入 `/api/dramas/search` 与 `/api/dramas/hot-search`
- [ ] 本地历史满足 trim、去重、倒序、最多 10 条、清空规则

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 修改 | 扩展搜索与快捷入口 route |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 修改 | 支持搜索相关页面导航 |
| `ios/ShortDrama/Sources/App/DeeplinkHandler.swift` | 修改 | 扩展搜索与承接页 deeplink |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 注册搜索页、结果页和承接页 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | 修改 | 首页搜索入口与结果卡片复用 |
| `ios/ShortDrama/Sources/Features/Search/ViewModels/SearchHomeViewModel.swift` | 新增 | 搜索发现页状态机 |
| `ios/ShortDrama/Sources/Features/Search/ViewModels/SearchResultViewModel.swift` | 新增 | 搜索结果页状态机 |
| `ios/ShortDrama/Sources/Features/Search/Views/SearchHomeView.swift` | 新增 | 搜索发现页 |
| `ios/ShortDrama/Sources/Features/Search/Views/SearchResultView.swift` | 新增 | 搜索结果页 |
| `ios/ShortDrama/Sources/Features/Search/Views/DiscoveryPlaceholderView.swift` | 新增 | 排行/分类/新剧/演员承接页 |
| `ios/ShortDrama/Sources/Features/Search/Views/Components/*.swift` | 新增 | 搜索模块组件 |
| `ios/ShortDrama/Sources/Domain/Entities/HotSearchItem.swift` | 新增 | 热搜实体 |
| `ios/ShortDrama/Sources/Domain/Entities/SearchHistoryItem.swift` | 新增 | 搜索历史实体 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/SearchHistoryRepositoryProtocol.swift` | 新增 | 本地历史协议 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 修改 | 扩展搜索/热搜仓库协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchHotSearchesUseCase.swift` | 新增 | 热搜用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/LoadSearchHistoryUseCase.swift` | 新增 | 历史读取用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/SaveSearchHistoryUseCase.swift` | 新增 | 历史保存用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/ClearSearchHistoryUseCase.swift` | 新增 | 历史清空用例 |
| `ios/ShortDrama/Sources/Domain/UseCases/SearchDramasUseCase.swift` | 新增 | 搜索用例 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 修改 | 兼容搜索接口错误包体 |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 搜索与热搜请求接线 |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 修改 | 搜索与热搜映射 |
| `ios/ShortDrama/Sources/Data/Repositories/UserDefaultsSearchHistoryRepository.swift` | 新增 | 本地历史持久化 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 搜索 route 回归测试 |
| `ios/ShortDrama/Tests/ViewModelTests/DeeplinkHandlerTests.swift` | 修改 | deeplink 回归测试 |
| `ios/ShortDrama/Tests/ViewModelTests/SearchHomeViewModelTests.swift` | 新增 | 搜索发现页 ViewModel 测试 |
| `ios/ShortDrama/Tests/ViewModelTests/SearchResultViewModelTests.swift` | 新增 | 搜索结果页 ViewModel 测试 |
| `ios/ShortDrama/Tests/DataTests/APIClientTests.swift` | 修改 | 搜索与热搜 endpoint / 错误包体测试 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 修改 | 搜索与热搜映射测试 |
| `ios/ShortDrama/Tests/DataTests/UserDefaultsSearchHistoryRepositoryTests.swift` | 新增 | 本地历史规则测试 |
