# 实现计划：iOS — PRD-06 分类浏览

> 创建日期：2026-07-27
> 对应技术方案：design-ios.md
> 对应需求：spec.md

## 概述

本期 iOS 端将在现有 `NavigationStack + NavigationRouter + MVVM + Clean Architecture` 基础上，把搜索发现页的 `classificationHome` 从 placeholder 承接替换为真实分类页，并接通 `GET /api/dramas/tags?gender=...`、左侧维度与右侧锚点滚动同步、以及标签点击后复用既有搜索结果页的完整链路。计划采用轻量 TDD：先锁定测试场景，再按 **ViewModel → Data → Router → 测试收口** 的顺序推进实现，过程中不新增第三方依赖，继续沿用现有 `URLSession`、`AppRoute`、`NavigationRouter` 与 Swift Testing 体系。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 首次进入分类页默认加载 `gender=all` | 首次进入 `classificationHome`，接口返回完整分类数据 | `ClassificationViewModel` 以 `gender=all` 发起请求，页面默认选中第一个维度并进入内容态 | 单元测试 | P0 |
| T-02 | 固定三维度与空维度保留 | 接口返回三个固定维度，其中某个维度 `tags=[]` | 左侧仍展示三个维度，右侧保留空维度分组与空态文案，不隐藏锚点 | 单元测试 | P0 |
| T-03 | 切换 gender 时只消费最后一次结果并重置选中维度 | `all -> male -> female` 快速切换，旧请求乱序返回 | UI 只展示最后一次 gender 的结果，`selectedDimension` 重置为首个维度 | 单元测试 | P0 |
| T-04 | 左侧维度点击与右侧滚动位置双向同步 | 点击左侧维度，或右侧滚动到其他分组 | 右侧滚动到对应锚点，且 `selectedDimension` 始终与当前可见分组同步 | 单元测试 | P0 |
| T-05 | 标签点击前执行 `normalizedTagQuery` 与 `guard let` | 点击标签文本为首尾空格、空串、超长文本或正常文本 | 仅正常文本会生成合法 query；空串或超长文本被拦截，不触发导航 | 单元测试 | P0 |
| T-06 | 标签点击复用既有搜索结果页路由 | 点击任意合法标签 | 导航到 `.searchResult(query:)`，不新增独立分类结果页 route | 单元测试 | P0 |
| T-07 | 搜索发现入口、Deeplink 与 public route name 保持不变 | 搜索发现页点击分类入口，或访问 `djsdrama://classification` | 仍进入 `.classificationHome`，`publicRouteName` 仍为 `classification` | 单元测试 | P0 |
| T-08 | 分类接口 endpoint 使用既有 REST 契约 | 请求 `female` 分类数据 | `GET /api/dramas/tags?gender=female` path / query 正确，响应可正常解码 | 单元测试 | P0 |
| T-09 | Repository 映射保留固定顺序与空维度 | DTO 返回顺序稳定但某维度为空 | Repository 输出固定三维度实体，空维度保留为空数组 | 单元测试 | P0 |
| T-10 | 首屏失败后可重试恢复 | 第一次加载失败，点击重试后第二次成功 | 页面先进入错误态，再恢复到内容态，且仍保持默认维度规则 | 单元测试 | P1 |

## 实现步骤

### Step 1：先锁定分类页状态机、默认维度与 query 规范化

- **关联测试**：T-01、T-02、T-03、T-04、T-05、T-10
- **目标文件**：`ios/ShortDrama/Sources/Domain/Entities/ClassificationGender.swift`、`ios/ShortDrama/Sources/Domain/Entities/ClassificationDimension.swift`、`ios/ShortDrama/Sources/Domain/Entities/ClassificationTagsPayload.swift`、`ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift`、`ios/ShortDrama/Sources/Domain/UseCases/FetchClassificationTagsUseCase.swift`、`ios/ShortDrama/Sources/Features/Classification/ViewModels/ClassificationViewModel.swift`、`ios/ShortDrama/Tests/ViewModelTests/ClassificationViewModelTests.swift`、`ios/ShortDrama/Tests/Mocks/MockDramaRepository.swift`
- **实现内容**：
  1. 先新增 `ClassificationViewModelTests`，用 `MockDramaRepository` 锁定首屏默认 `gender=all`、固定三维度、空维度保留、切换 gender 的并发保护、`selectedDimension` 重置到首项、失败重试恢复等核心状态流转。
  2. 在 Domain 层补齐 `ClassificationGender`、`ClassificationDimension`、`ClassificationTagsPayload` 与 `FetchClassificationTagsUseCase`，扩展 `DramaRepositoryProtocol.fetchClassificationTags(gender:)`，让 Presentation 层只依赖 UseCase 与协议。
  3. 在 `ClassificationViewModel` 中建立清晰状态机，至少覆盖 `loading / content / error`、`selectedGender`、`selectedDimension`、`hasLoaded` 与请求 token，默认值固定为 `.all` 与首个维度。
  4. 实现 `loadIfNeeded()`、`selectGender(_:)`、`selectDimension(_:)`、`updateVisibleDimension(_:)`、`retry()`，其中 gender 切换成功后必须把 `selectedDimension` 重置为第一个维度；旧请求晚回时不得覆盖新结果。
  5. 在 ViewModel 中新增 `normalizedTagQuery(_:)`，规则与现有搜索页保持一致：去首尾空格、清洗后不能为空、长度不超过 50；后续 View 层点击标签统一通过 `guard let query = viewModel.normalizedTagQuery(tag) else { return }` 再导航。
  6. 扩展 `MockDramaRepository`，支持分类标签成功、失败、延迟返回等场景，为并发保护和重试测试提供稳定假数据，不新增额外测试基础设施。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-01、T-02、T-03、T-04、T-05、T-10 对应状态机测试通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Domain/Entities/ClassificationGender.swift` | 新增 | 性别筛选领域实体 |
| `ios/ShortDrama/Sources/Domain/Entities/ClassificationDimension.swift` | 新增 | 固定三维度实体与稳定 key 定义 |
| `ios/ShortDrama/Sources/Domain/Entities/ClassificationTagsPayload.swift` | 新增 | 分类标签完整载荷实体 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 修改 | 扩展分类标签读取协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchClassificationTagsUseCase.swift` | 新增 | 分类标签读取用例 |
| `ios/ShortDrama/Sources/Features/Classification/ViewModels/ClassificationViewModel.swift` | 新增 | 默认加载、gender 切换、并发保护、query 规范化与维度同步状态机 |
| `ios/ShortDrama/Tests/ViewModelTests/ClassificationViewModelTests.swift` | 新增 | 覆盖默认值、空维度保留、并发保护、query 清洗、重试恢复 |
| `ios/ShortDrama/Tests/Mocks/MockDramaRepository.swift` | 修改 | 支持分类标签 mock 行为 |

### Step 2：先补数据契约测试，再接通分类接口与 DTO 映射

- **关联测试**：T-02、T-08、T-09
- **目标文件**：`ios/ShortDrama/Sources/Data/DTOs/ClassificationTagsResponseDTO.swift`、`ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift`、`ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift`、`ios/ShortDrama/Tests/DataTests/APIClientTests.swift`、`ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift`
- **实现内容**：
  1. 先在 `APIClientTests` 中锁定 `GET /api/dramas/tags?gender=all|male|female` 的 path、method 与 query 构造，确保 iOS 端沿用现有 RESTful / `APIEndpoint` 风格，不引入新的网络层模式。
  2. 新增 `ClassificationTagsResponseDTO`，在同一 DTO 文件中定义维度子 DTO，字段对齐 shared design：`gender`、`dimensions[].key`、`dimensions[].name`、`dimensions[].tags`。
  3. 扩展 `DramaRemoteDataSource` 与 `DramaEndpoints`，新增分类接口 endpoint 与 `fetchClassificationTags(gender:)`，继续复用现有 `APIClient + URLSession`。
  4. 在 `DramaRepository` 中补齐 DTO -> Entity 映射，显式保留固定三维度顺序，并允许某个维度 `tags=[]`；即便后端返回空维度，Repository 也不能在客户端侧把该分组过滤掉。
  5. 在 `DramaRepositoryTests` 中补齐分类 DTO 映射测试，重点锁定空维度保留、三维度顺序稳定、gender 值透传正确，避免后续 UI 层被迫做额外兜底分支。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-08、T-09 的数据契约测试通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Data/DTOs/ClassificationTagsResponseDTO.swift` | 新增 | 分类接口响应 DTO |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 新增 `/api/dramas/tags` endpoint 与请求方法 |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 修改 | 新增分类 DTO -> Entity 映射 |
| `ios/ShortDrama/Tests/DataTests/APIClientTests.swift` | 修改 | 覆盖分类 endpoint path / query / decode 契约 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 修改 | 覆盖分类数据映射、空维度保留与顺序稳定 |

### Step 3：替换 `classificationHome` 承接页，并锁定路由 / Deeplink 语义不变

- **关联测试**：T-06、T-07
- **目标文件**：`ios/ShortDrama/Sources/App/TabNavigationHostView.swift`、`ios/ShortDrama/Sources/Features/Search/Views/DiscoveryPlaceholderView.swift`、`ios/ShortDrama/Sources/Features/Classification/Views/ClassificationHomeView.swift`、`ios/ShortDrama/Tests/ViewModelTests/SearchHomeViewModelTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`、`ios/ShortDrama/Tests/ViewModelTests/DeeplinkHandlerTests.swift`
- **实现内容**：
  1. 先在 `SearchHomeViewModelTests`、`NavigationRouterTests`、`DeeplinkHandlerTests` 中回归锁定分类入口语义：搜索发现页快捷入口仍返回 `.classificationHome`，`publicRouteName` 仍为 `classification`，`djsdrama://classification` 仍解析到同一路由。
  2. 将 `TabNavigationHostView` 中 `.classificationHome` 的页面注册从 `DiscoveryPlaceholderView(kind: .classification)` 切换为真实 `ClassificationHomeView`，但不改动 `AppRoute.classificationHome` 的命名与归属 tab。
  3. 在 `ClassificationHomeView` 中只复用既有 `.searchResult(query:)` 作为标签点击后的目标路由，明确不新增分类结果页、`search?q=` 风格新语义或中间承接页。
  4. 调整 `DiscoveryPlaceholderView`，让 `classification` 不再承担真实页面职责，其余 `newReleases / actorHub` 占位页保持现状。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-06、T-07 对应的路由与 Deeplink 回归通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 将 `.classificationHome` 接到真实分类页 |
| `ios/ShortDrama/Sources/Features/Search/Views/DiscoveryPlaceholderView.swift` | 修改 | 移除 classification 的真实承接职责 |
| `ios/ShortDrama/Sources/Features/Classification/Views/ClassificationHomeView.swift` | 新增 | 分类页根视图与 Router 接线入口 |
| `ios/ShortDrama/Tests/ViewModelTests/SearchHomeViewModelTests.swift` | 修改 | 回归分类快捷入口仍映射到 `.classificationHome` |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 回归分类 route name / owning tab / 搜索结果复用语义 |
| `ios/ShortDrama/Tests/ViewModelTests/DeeplinkHandlerTests.swift` | 修改 | 回归 `djsdrama://classification` 承接语义 |

### Step 4：落地真实分类 UI，并完成左侧维度与右侧锚点滚动同步

- **关联测试**：T-02、T-04、T-05
- **目标文件**：`ios/ShortDrama/Sources/Features/Classification/Views/ClassificationHomeView.swift`、`ios/ShortDrama/Sources/Features/Classification/Views/Components/ClassificationGenderTabBar.swift`、`ios/ShortDrama/Sources/Features/Classification/Views/Components/ClassificationDimensionRail.swift`、`ios/ShortDrama/Sources/Features/Classification/Views/Components/ClassificationTagSectionList.swift`、`ios/ShortDrama/Sources/Features/Classification/Views/Components/ClassificationTagChip.swift`、`ios/ShortDrama/Sources/Features/Classification/Views/Components/ClassificationStateView.swift`、`ios/ShortDrama/Tests/ViewModelTests/ClassificationViewModelTests.swift`
- **实现内容**：
  1. 按 design-ios 的三层结构落地 `ClassificationGenderTabBar + ClassificationDimensionRail + ClassificationTagSectionList`，顶部性别 Tab 固定为“全部 / 男频 / 女频”，左侧与右侧都使用同一组三维度 key。
  2. 在 `ClassificationHomeView` 中使用 `ScrollViewReader` + section anchor 接线：点击左侧维度时滚动到右侧对应分组；右侧滚动导致首个可见分组变化时，回写 `viewModel.updateVisibleDimension(_:)`，保证双向同步而不是单向跳转。
  3. 右侧标签区采用固定三列胶囊布局；若某个维度为空，仍渲染分组标题与空态文案，保留 anchor，不通过条件分支隐藏整个 section。
  4. gender 切换成功或 retry 恢复后，页面滚动位置重置到首个维度，确保 `selectedDimension`、右侧可见分组和顶部内容一致。
  5. 标签点击统一通过 `guard let query = viewModel.normalizedTagQuery(tag) else { return }` 处理，再执行 `router.navigate(to: .searchResult(query: query))`，严格复用现有搜索结果页。
- **验证方式**：
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'` 确认 T-02、T-04、T-05 相关状态同步与 query 规范化测试通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/Features/Classification/Views/ClassificationHomeView.swift` | 修改 | 接入分类页容器、滚动同步与标签导航 |
| `ios/ShortDrama/Sources/Features/Classification/Views/Components/ClassificationGenderTabBar.swift` | 新增 | 顶部 gender 切换组件 |
| `ios/ShortDrama/Sources/Features/Classification/Views/Components/ClassificationDimensionRail.swift` | 新增 | 左侧维度导航 |
| `ios/ShortDrama/Sources/Features/Classification/Views/Components/ClassificationTagSectionList.swift` | 新增 | 右侧分组列表、锚点与标签矩阵 |
| `ios/ShortDrama/Sources/Features/Classification/Views/Components/ClassificationTagChip.swift` | 新增 | 标签胶囊组件 |
| `ios/ShortDrama/Sources/Features/Classification/Views/Components/ClassificationStateView.swift` | 新增 | loading / error / content / 空维度状态容器 |
| `ios/ShortDrama/Tests/ViewModelTests/ClassificationViewModelTests.swift` | 修改 | 补充维度同步、空维度展示与 query 拦截测试 |

### Step 5：执行工程生成与 iOS 全量回归，固化分类页交付基线

- **关联测试**：T-01 ～ T-10
- **目标文件**：`docs/specs/2026-07-27-prd-06-classification/plan-ios.md`
- **实现内容**：
  1. coding 阶段严格按本计划顺序推进：先以测试清单锁定行为，再依次完成 ViewModel、Data、Router / UI 接线，最后做全量测试回归。
  2. 由于会新增 `Features/Classification`、Domain、Data、Tests 多个 Swift 文件，先运行 `xcodegen generate`，再执行 test / build / lint 回归，保持与当前仓库 iOS 工程命令风格一致。
  3. 回看自动化覆盖面，确保默认 `gender=all`、固定三维度、空维度保留、gender 切换并发保护、`selectedDimension` 重置、左侧与右侧同步、标签 query 规范化、搜索结果页复用、Deeplink 语义不变等关键逻辑都已被测试锁定。
  4. 将 coding 阶段收口标准固定为：`classificationHome` 已替换真实页面；不新增分类结果页；`.searchResult(query:)` 与 `djsdrama://classification` 语义保持稳定；网络仍基于 `APIClient + URLSession`；无新增第三方依赖与 lint 错误。
- **验证方式**：
  - 运行 `cd ios && xcodegen generate`
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - 运行 `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - 运行 `cd ios && swiftlint lint`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `docs/specs/2026-07-27-prd-06-classification/plan-ios.md` | 新增 | 固化 iOS 分类页实现步骤、测试场景与验收基线 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5
```

## 验证总览

- [ ] 所有测试通过（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [ ] Build 成功（`cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`）
- [ ] 无新增 lint 错误（`cd ios && swiftlint lint`）
- [ ] 新增源码后已执行 `cd ios && xcodegen generate`
- [ ] 默认进入分类页时固定请求 `gender=all`，且有单元测试覆盖
- [ ] 固定三维度与空维度保留行为有单元测试覆盖
- [ ] gender 切换并发保护与 `selectedDimension` 重置有单元测试覆盖
- [ ] 左侧维度与右侧锚点滚动同步有单元测试与集成接线说明
- [ ] 标签点击前 `normalizedTagQuery` 与 `guard let` 拦截规则有单元测试覆盖
- [ ] 标签点击继续复用 `.searchResult(query:)` 与现有搜索页，不新增分类结果页
- [ ] `classificationHome`、`publicRouteName == "classification"`、`djsdrama://classification` 语义保持一致
- [ ] 分类接口对接继续基于现有 `APIClient + URLSession`，无新增第三方依赖

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | 修改 | 将 `.classificationHome` 接到真实分类页 |
| `ios/ShortDrama/Sources/Features/Search/Views/DiscoveryPlaceholderView.swift` | 修改 | 移除 classification 的占位承接职责 |
| `ios/ShortDrama/Sources/Features/Classification/ViewModels/ClassificationViewModel.swift` | 新增 | 分类页状态机、并发保护、维度同步与 query 规范化 |
| `ios/ShortDrama/Sources/Features/Classification/Views/ClassificationHomeView.swift` | 新增 | 分类页根视图与 Router 接线 |
| `ios/ShortDrama/Sources/Features/Classification/Views/Components/*.swift` | 新增 | gender Tab、维度导航、标签列表、标签胶囊、状态容器 |
| `ios/ShortDrama/Sources/Domain/Entities/ClassificationGender.swift` | 新增 | 性别枚举 |
| `ios/ShortDrama/Sources/Domain/Entities/ClassificationDimension.swift` | 新增 | 固定三维度实体 |
| `ios/ShortDrama/Sources/Domain/Entities/ClassificationTagsPayload.swift` | 新增 | 分类数据载荷实体 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/DramaRepositoryProtocol.swift` | 修改 | 扩展分类标签查询协议 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchClassificationTagsUseCase.swift` | 新增 | 分类标签读取用例 |
| `ios/ShortDrama/Sources/Data/DTOs/ClassificationTagsResponseDTO.swift` | 新增 | 分类接口 DTO |
| `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift` | 修改 | 新增 `/api/dramas/tags` API 接线 |
| `ios/ShortDrama/Sources/Data/Repositories/DramaRepository.swift` | 修改 | 新增分类数据映射 |
| `ios/ShortDrama/Tests/Mocks/MockDramaRepository.swift` | 修改 | 支持分类 mock 数据与延迟场景 |
| `ios/ShortDrama/Tests/ViewModelTests/ClassificationViewModelTests.swift` | 新增 | 覆盖默认加载、并发保护、维度同步、query 规范化与重试 |
| `ios/ShortDrama/Tests/DataTests/APIClientTests.swift` | 修改 | 覆盖分类 endpoint 契约 |
| `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift` | 修改 | 覆盖分类 DTO -> Entity 映射 |
| `ios/ShortDrama/Tests/ViewModelTests/SearchHomeViewModelTests.swift` | 修改 | 回归分类快捷入口 route |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | 修改 | 回归 route public name 与搜索结果页复用语义 |
| `ios/ShortDrama/Tests/ViewModelTests/DeeplinkHandlerTests.swift` | 修改 | 回归分类 Deeplink 语义 |
