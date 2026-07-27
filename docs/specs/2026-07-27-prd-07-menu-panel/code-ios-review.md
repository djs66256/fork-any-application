# 代码 Review：iOS — 菜单面板

> Review 日期：2026-07-28

## 审查结果

| 维度 | 状态 | 说明 |
|------|------|------|
| 实现与 design 一致性 | ✅ | 已按壳层 overlay、关闭后导航、最近在看动态区与统一占位页方案落地 |
| 无硬编码常量 | ✅ | 未引入环境地址、token 或固定环境开关；接口仍通过既有配置与 session store 注入 |
| 代码风格符合平台规范 | ✅ | 变更文件已通过针对性 swiftlint 校验，0 violations |
| 错误处理完备 | ✅ | 最近在看区区分 empty / error / retry，session 初始化失败安全降级为局部错误态 |
| 性能无明显问题 | ✅ | 菜单数据加载具备 hasLoaded 与 inFlightTask 去重，关闭后导航只消费一次 |
| API 调用一致性 | ✅ | 新增 GET /api/player/recently-viewed，Header 为 X-Playback-Session-Id，符合共享 design |
| 所有测试通过 | ✅ | xcodebuild test 通过，179 tests in 20 suites passed |
| 内存与线程安全 | ✅ | ViewModel 使用 @MainActor，异步任务在必要处取消并避免重复请求 |
| View 层级与单元测试 | ✅ | Router / Data / ViewModel 已补充菜单面板相关单元测试覆盖 |

## 变更文件审查

| 文件 | 审查结果 | 问题数 |
|------|---------|--------|
| `ios/ShortDrama/Sources/App/AppRoute.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/App/AppShellView.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Domain/Entities/MenuPlaceholderKind.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Domain/Entities/RecentlyViewedItem.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/MenuPanelRepositoryProtocol.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchRecentlyViewedUseCase.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Data/DTOs/RecentlyViewedResponseDTO.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Data/Repositories/MenuPanelRepository.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelView.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPlaceholderView.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/Components/MenuLoginHeaderView.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/Components/MenuMessagePreviewView.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/Components/RecentlyViewedCardView.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/Components/MenuRecentlyViewedSection.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/Components/MenuGameCenterSection.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/MenuPanel/Views/Components/MenuCommonFunctionsSection.swift` | ✅ | 0 |
| `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift` | ✅ | 0 |
| `ios/ShortDrama/Tests/ViewModelTests/MenuPanelViewModelTests.swift` | ✅ | 0 |
| `ios/ShortDrama/Tests/DataTests/PlayerRemoteDataSourceTests.swift` | ✅ | 0 |
| `ios/ShortDrama/Tests/DataTests/MenuPanelRepositoryTests.swift` | ✅ | 0 |
| `ios/ShortDrama/Tests/Mocks/MockPlayerRepository.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | ✅ | 0 |

## 发现的问题

无新增阻塞或关注问题。

## 修复记录

| 轮次 | 修复项 |
|------|--------|
| 1 | 修复 `MenuPanelContainerView` 的 `.frame(width:maxHeight:)` 调用错误，拆分为两次 `.frame` 并抽出 `panelWidth` |
| 1 | 将 `PlayerRemoteDataSource` 的嵌套 endpoint 类型提取为文件级私有类型，消除变更文件中的 nesting warning |
| 1 | 修复 `PlayerRemoteDataSourceTests` 的 imports 与 force unwrap，确保变更测试文件 lint 通过 |
| 1 | 调整 `PlayerViewModel.handleBack()` 与 `handleDisappear()`，恢复既有播放器退出/消失语义，消除全量测试回归 |

## 上一轮问题修复验证

> 仅非首轮 review 时填写。验证上一轮 review 报告中标记为 `✅ 已修复` 的问题是否真正被修改到位。

本轮为首次 review，无上一轮问题。

## 遗留问题（需人工决策）

无。

## 结论

- [x] ✅ 所有问题已修复，代码质量合格
- [ ] ⚠️ 存在遗留问题，需人工确认
