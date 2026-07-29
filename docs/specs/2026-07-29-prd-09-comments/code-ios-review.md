# 代码 Review：iOS — PRD-09 评论系统

> Review 日期：2026-07-29

## 审查结果

| 维度 | 状态 | 说明 |
|------|------|------|
| 实现与 design 一致性 | ✅ | 已覆盖 design-ios.md 中要求的 Domain/Data 链路、`CommentSheetViewModel`、`CommentLoginContext`、Home/Player 页面内 `.sheet` 宿主接线与登录恢复约束。 |
| 无硬编码常量 | ✅ | 评论接口路径使用现有 `APIClient` + `APIEndpoint`，未引入固定环境地址、token 或临时常量。 |
| 代码风格符合平台规范 | ⚠️ | 本次新增文件已做基础整理，但仓库当前仍存在多处既有 SwiftLint warning，且 `PlayerViewModel` 仍有 `type_body_length` warning。 |
| 错误处理完备 | ✅ | 首屏失败、分页失败、401 登录拦截、本地空输入校验、重复点赞防抖均有显式处理。 |
| 性能无明显问题 | ✅ | 评论分页仅追加尾部数据，点赞仅局部更新目标评论，未引入全量重刷或重复请求。 |
| API 调用一致性 | ✅ | REST 路径、query/body、snake_case 编解码与 `pagination.total` 使用均与 spec/design 保持一致。 |
| 所有测试通过 | ✅ | `xcodebuild test` 已通过，测试日志显示 215 tests passed。 |
| SwiftUI 状态归属清晰 | ✅ | 评论抽屉保持页面内局部状态，Home/Player 仅持有宿主级 sheet/context 协调状态。 |
| 登录恢复不重放写操作 | ✅ | 登录成功后仅恢复评论抽屉上下文，不自动提交评论或自动点赞。 |

## 变更文件审查

| 文件 | 审查结果 | 问题数 |
|------|---------|--------|
| `ios/ShortDrama/Sources/Domain/Entities/Comment.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Domain/Entities/CommentQuery.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Domain/Entities/ToggleCommentLikeResult.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Domain/RepositoryProtocols/CommentRepositoryProtocol.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Domain/UseCases/FetchDramaCommentsUseCase.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Domain/UseCases/CreateCommentUseCase.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Domain/UseCases/ToggleCommentLikeUseCase.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Data/DTOs/CommentDTO.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Data/DTOs/CommentListResponseDTO.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Data/DTOs/CreateCommentRequestDTO.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Data/DTOs/ToggleCommentLikeResponseDTO.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Data/DataSources/CommentRemoteDataSource.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Data/Repositories/CommentRepository.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/Comments/CommentLoginContext.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/Comments/ViewModels/CommentSheetViewModel.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/Comments/Views/CommentSheetView.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/Comments/Views/Components/CommentComposerView.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/Comments/Views/Components/CommentListView.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/Comments/Views/Components/CommentRowView.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/Comments/Views/Components/CommentStateView.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/Home/Views/Components/HomeDramaCardView.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | ⚠️ | 1 |
| `ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerRightActionBar.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` | ✅ | 0 |
| `ios/ShortDrama/Sources/Features/Search/Views/Components/SearchResultStateView.swift` | ✅ | 0 |
| `ios/ShortDrama/Tests/Mocks/MockCommentRepository.swift` | ✅ | 0 |
| `ios/ShortDrama/Tests/DataTests/CommentRemoteDataSourceTests.swift` | ✅ | 0 |
| `ios/ShortDrama/Tests/DataTests/CommentRepositoryTests.swift` | ✅ | 0 |
| `ios/ShortDrama/Tests/ViewModelTests/CommentSheetViewModelTests.swift` | ✅ | 0 |
| `ios/ShortDrama/Tests/ViewModelTests/HomeViewModelTests.swift` | ✅ | 0 |
| `ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift` | ✅ | 0 |

## 发现的问题

### 问题 1：`PlayerViewModel` 体量已超过当前 SwiftLint 阈值

- **严重程度**：🟢 低
- **文件**：`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:5`
- **类型**：可维护性
- **描述**：评论入口与宿主协调逻辑加入后，`PlayerViewModel` 触发了现有 `type_body_length` warning（316 行）。这不会阻塞构建或测试，但后续若继续增加播放器能力，维护成本会继续上升。
- **建议修复**：后续可将评论宿主协调或播放停止/切集细节拆分为 extension / helper，降低主 ViewModel 体量。
- **修复状态**：❌ 未修复
- **修复方案**：本次以功能交付为主，先保留现结构，避免在评论需求内做额外重构扩大变更面。

## 修复记录

| 轮次 | 修复项 |
|------|--------|
| 1 | 修复 `CommentSheetViewModelTests` 中 `Comment` 类型歧义，改为显式引用 `ShortDrama.Comment`。 |
| 2 | 为 Home / Player 评论宿主补齐测试，修复 `PlayerViewModelTests` 中 `handleBack` / `handleDisappear` 的异步等待不稳定问题。 |
| 3 | 调整部分测试 import 顺序，并去除新引入的测试文件中的 force unwrap。 |

## 遗留问题（需人工决策）

| 编号 | 问题 | 文件 | 建议 | 状态 |
|------|------|------|------|------|
| H-01 | SwiftLint 仍有多处 warning，其中多数为仓库既有测试文件风格问题，本次未统一清理 | `ios/ShortDrama/Tests/...`、`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` | 若希望 `swiftlint lint` 达到完全无 warning，需要单独发起一次 lint 治理任务，避免把功能 PR 扩大成样式清理 PR | 待确认 |

## 结论

- [ ] ✅ 所有问题已修复，代码质量合格
- [x] ⚠️ 存在遗留问题，需人工确认

结论说明：功能实现、自动化测试与构建均已通过，评论系统 iOS 代码可以进入人工 review；但由于仓库当前仍存在既有 SwiftLint warning，若验收标准要求 `swiftlint lint` 完全零 warning，则还不能视为完全收口。