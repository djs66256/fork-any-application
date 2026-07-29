# PRD-14 赚钱中心 iOS 代码评审

## 实现范围

本次实现严格限定在 `ios/` 目录内，并额外补充本评审文档，完成了以下内容：

1. 赚钱中心配置接入
   - 通过 `ios/project.yml`、`ios/Configs/Debug.xcconfig`、`ios/Configs/Release.xcconfig`、`ios/ShortDrama/Sources/Core/Config/AppConfig.swift` 注入并读取 `EARN_BASE_URL`
   - 提供 `earnBaseURL` / `earnHomeURL`，未直接手改 `.xcodeproj/project.pbxproj`

2. Earn 专属路由与 Router 承接
   - 在 `ios/ShortDrama/Sources/App/AppRoute.swift` 增加 `earnLogin` / `earnPlayer`
   - 在 `ios/ShortDrama/Sources/App/NavigationRouter.swift` 增加 earn 专属登录承接、播放器承接、restore request、结果消费逻辑
   - 在 `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` 将 earn tab 切换为真实容器，并将 earn player 路由接入原生播放器

3. Earn 容器、Bridge 与 Host Message 注入
   - 新增 `Features/Earn` 目录下容器视图、状态视图、WebView、ViewModel、bridge / host message 模型
   - H5 -> Native 仅解析 `earn.requestLogin` 与 `earn.openTaskPlayer`
   - Native -> H5 仅通过 `CustomEvent('earn.hostMessage', { detail })` 注入
   - 支持登录返回、任务返回、容器重建、前后台切换后的 auth/restore 同步

4. Earn 专属登录与播放返回语义
   - 新增 `EarnLoginContext`、`EarnTaskContext`、`EarnTaskPlayerResult`
   - 登录返回与播放返回均保持 earn 独立语义，没有复用 mall 状态
   - 仅代表性任务视频自然播放结束时返回 `completed = true`
   - back / background / error / container recreation 均返回 `completed = false`

5. Player handoff 与结果收口
   - 在 `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` 增加 earn 上下文输入与结果回传
   - 在 `ios/ShortDrama/Sources/Features/Player/Views/Components/NativeVideoPlayerView.swift` 增加自然播放结束与播放失败回调
   - 在 `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` 连接新的播放器回调

6. 单元测试补齐
   - 扩展/新增以下测试：
     - `ios/ShortDrama/Tests/DomainTests/AppConfigTests.swift`
     - `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift`
     - `ios/ShortDrama/Tests/ViewModelTests/EarnContainerViewModelTests.swift`
     - `ios/ShortDrama/Tests/ViewModelTests/PlayerViewModelTests.swift`
   - 补充测试辅助文件以消除新增改动中的 lint 问题：
     - `ios/ShortDrama/Tests/Helpers/EarnTestHelpers.swift`
     - `ios/ShortDrama/Tests/Helpers/NavigationRouterTestHelpers.swift`
     - `ios/ShortDrama/Tests/Helpers/PlayerViewModelTestHelpers.swift`

## 验证结果

### 1. 项目生成

命令：

```bash
cd ios && xcodegen generate
```

结果：通过。

### 2. 定向测试

命令：

```bash
cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0' -only-testing:ShortDramaTests/AppConfigTests -only-testing:ShortDramaTests/NavigationRouterTests -only-testing:ShortDramaTests/EarnContainerViewModelTests -only-testing:ShortDramaTests/PlayerViewModelTests
```

结果：通过。

说明：最终定向测试结果为 62 tests in 3 suites passed。

### 3. 全量测试

命令：

```bash
cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'
```

结果：通过。

说明：全量测试结果为 267 tests in 34 suites passed。

### 4. 构建验证

命令：

```bash
cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'
```

结果：通过。

### 5. SwiftLint

命令：

```bash
cd ios && swiftlint lint
```

结果：命令执行成功，退出码为 0。

补充说明：
- 本次改动涉及的新增/修改文件已额外做过定向 lint 清理，结果为 0 violation。
- 全仓 `swiftlint lint` 仍输出 44 条 warning，但均来自仓库既有基线文件或非本次改动文件，不是本次 earn iOS 实现新增问题。

## 发现并修复的问题

1. 新增 earn 测试与实现初版存在 SwiftLint 警告
   - 包括 sorted imports、force unwrap、file/type body length、部分长行问题
   - 已通过测试 helper 抽取、测试重构、`PlayerViewModelTypes.swift` 类型拆分等方式清理本次改动相关警告

2. `PlayerViewModel` 类型体积超限
   - 初版将 `UiState`、`PlaybackSpeed`、`StopFingerprint` 都放在同一个文件中
   - 已抽出到 `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModelTypes.swift`

3. `NavigationRouterTests.swift` / `PlayerViewModelTests.swift` 体积偏大
   - 已对测试按职责重组，并抽取 helper，保证本次修改文件通过定向 lint

4. 预览/测试中的强制解包问题
   - 已改为 helper 构造，移除新增代码中的 force unwrap

## 遗留风险

1. 仓库存在既有 SwiftLint warning 基线
   - 不影响本次 earn iOS 交付的构建、测试和命令退出状态
   - 但若后续团队希望将 lint 作为“零 warning”门禁，需要另行做全仓清理

2. `ios/ShortDrama.xcodeproj/project.pbxproj` 已随 `xcodegen generate` 重新生成
   - 该变更为生成产物，不是人工直接编辑
   - 提交时应与 `project.yml` / 新增源文件一起保留，否则工程定义与生成结果可能不一致

## 结论

本次 PRD-14 赚钱中心 iOS 编码实现已完成既定范围：

- earn tab 容器已落地
- 配置、路由、router 登录承接、player handoff、`EarnTaskPlayerResult` 收口、host message 注入与测试均已完成
- 关键约束均已满足：
  - 配置经由 `project.yml` / xcconfig / `AppConfig`
  - earn host sync 只走 `CustomEvent('earn.hostMessage')`
  - 登录/播放返回保持 earn 专属语义
  - 仅自然播放结束返回 `completed = true`
  - 未新增依赖，未扩需求

评审结论：可以通过。
