# 测试规范 — iOS

> 本文档定义 iOS 端的测试策略、框架选型与编写规范。

---

## 1. 测试金字塔

项目遵循经典测试金字塔，结合 iOS 端特性调整各层占比：

| 层级 | 框架 | 占比 | 目标 |
|------|------|:---:|------|
| Unit | XCTest | ~70% | 业务逻辑验证（UseCase、Repository、ViewModel） |
| Integration | XCTest | ~20% | 组件集成（Repository + DataSource 配合真实/模拟环境） |
| UI | XCUITest | ~10% | 关键路径 E2E（启动→首页→播放→返回） |

补充说明：
- 新增业务逻辑代码必须同步补齐单元测试（ios/CLAUDE.md 中已规定）。
- 快照测试作为 UI 测试的补充，不单独占一层。
- PR 中若 CI 测试覆盖不达标的代码，需在 PR 描述中说明原因。

---

## 2. 单元测试

### 2.1 XCTest

- 测试类命名：`{被测试类名}Tests`，如 `HomeViewModelTests`、`DramaRepositoryTests`。
- 测试方法命名：`test_{方法名}_{场景}_{预期结果}`，如 `test_fetchDramas_whenSuccess_updatesDramas`。
- 文件组织：`ios/ShortDrama/Tests/DomainTests/`、`ios/ShortDrama/Tests/DataTests/`、`ios/ShortDrama/Tests/ViewModelTests/`。
- 公共 setUp 逻辑：
  ```swift
  final class HomeViewModelTests: XCTestCase {
      var sut: HomeViewModel!
      var mockUseCase: MockFetchRecommendationsUseCase!

      override func setUp() {
          super.setUp()
          mockUseCase = MockFetchRecommendationsUseCase()
          sut = HomeViewModel(fetchUseCase: mockUseCase)
      }

      override func tearDown() {
          sut = nil
          mockUseCase = nil
          super.tearDown()
      }
  }
  ```
- **sut** (System Under Test) 为被测试对象的约定命名。
- 每个测试方法遵循 AAA 模式：**Arrange**（准备数据）→ **Act**（执行操作）→ **Assert**（验证结果）。

### 2.2 Swift Testing (Swift 6+)

- 若项目目标 Swift 6+，可使用 Swift Testing 替代 XCTest。
- 基本语法：
  ```swift
  import Testing

  @Suite struct HomeViewModelTests {
      let sut: HomeViewModel
      let mockUseCase = MockFetchRecommendationsUseCase()

      init() {
          sut = HomeViewModel(fetchUseCase: mockUseCase)
      }

      @Test("加载推荐列表成功时更新 dramas 属性")
      func fetchDramasSuccess() async {
          mockUseCase.stubbedResult = [.mock(id: "1"), .mock(id: "2")]
          await sut.loadContent()
          #expect(sut.dramas.count == 2)
          #expect(sut.isLoading == false)
          #expect(sut.errorMessage == nil)
      }

      @Test("网络错误时设置 errorMessage", arguments: [
          AppError.network(underlying: NSError(domain: "", code: -1009)),
          AppError.server(code: 500, message: "Server Error"),
      ])
      func fetchDramasError(error: AppError) async {
          mockUseCase.stubbedError = error
          await sut.loadContent()
          #expect(sut.dramas.isEmpty)
          #expect(sut.errorMessage != nil)
      }
  }
  ```
- Swift Testing 优势：`#expect` 替代 `XCTAssert*`、参数化测试（`@Test(arguments:)`）、结构化测试套件。

### 2.3 Mock 策略

- **Protocol-based Mocking**：手写 Mock 类，遵循被 mock 的协议。
  ```swift
  protocol DramaRepositoryProtocol {
      func fetchRecommendations(page: Int, size: Int) async throws -> [Drama]
  }

  final class MockDramaRepository: DramaRepositoryProtocol {
      var stubbedResult: [Drama] = []
      var stubbedError: AppError?
      var fetchCallCount = 0
      var lastPage: Int?

      func fetchRecommendations(page: Int, size: Int) async throws -> [Drama] {
          fetchCallCount += 1
          lastPage = page
          if let error = stubbedError { throw error }
          return stubbedResult
      }
  }
  ```
- 在 Model 中提供 `static func mock(...)` 工厂方法方便测试数据构造：
  ```swift
  extension Drama {
      static func mock(id: String = "1", title: String = "测试短剧") -> Drama {
          Drama(id: id, title: title, coverURL: URL(string: "https://example.com/cover.jpg")!, totalEpisodes: 10)
      }
  }
  ```
- 不推荐引入 Sourcery 等代码生成工具生成 Mock 的过度工程——手写 Mock 更可控。

### 2.4 ViewModel 测试

- 测试 ViewModel 的核心要点：
  1. **状态转换**：loading → loaded、loading → error、loaded → refreshing。
  2. **方法调用转发**：用户操作是否正确调用了 UseCase。
  3. **属性变化**：`@Published` / `@Observable` 属性是否在预期时机更新。
- 对于 `@MainActor` ViewModel，测试方法也标记为 `@MainActor`，或使用 `await MainActor.run { }`。

```swift
@MainActor
final class HomeViewModelTests: XCTestCase {
    func test_loadContent_setsLoadingFlag() async {
        mockUseCase.stubbedResult = []
        let expectation = XCTestExpectation(description: "isLoading becomes true")
        let cancellable = sut.$isLoading.sink { isLoading in
            if isLoading { expectation.fulfill() }
        }
        Task { await sut.loadContent() }
        await fulfillment(of: [expectation], timeout: 1.0)
    }
}
```

---

## 3. UI 测试

### 3.1 XCUITest

- 仅覆盖关键用户路径（满足 ~10% 占比目标）：
  - 冷启动 → 首页加载成功 → 首屏内容可见
  - 点击短剧卡片 → 详情页打开 → 点击播放 → 播放器出现
  - 标签切换：首页 ↔ 发现 ↔ 福利 ↔ 我的
- 测试文件放在 `ios/ShortDrama/Tests/UITests/`。
- 示例：
  ```swift
  final class PlayerE2ETests: XCTestCase {
      let app = XCUIApplication()

      override func setUp() {
          continueAfterFailure = false
          app.launchArguments = ["-UITesting", "-MockNetwork"]
          app.launch()
      }

      func testLaunchAndPlayFirstDrama() {
          let firstCard = app.scrollViews.otherElements.buttons["drama_card_0"]
          XCTAssertTrue(firstCard.waitForExistence(timeout: 10))
          firstCard.tap()
          let playButton = app.buttons["play_button"]
          XCTAssertTrue(playButton.waitForExistence(timeout: 5))
          playButton.tap()
          let playerView = app.otherElements["player_view"]
          XCTAssertTrue(playerView.waitForExistence(timeout: 5))
      }
  }
  ```
- 使用 `launchArguments` 传入 `-UITesting` 标记，App 内部根据此标记使用 Mock 数据源，不依赖真实网络。

### 3.2 ViewInspector

- **ViewInspector** 用于在不启动 XCUITest 的情况下测试 SwiftUI View 的渲染和行为。
- 适用场景：
  - 验证 View 在特定状态下渲染的元素是否正确
  - 验证按钮点击是否触发 ViewModel 方法
  - 替代部分 UI 测试，降低 CI 耗时
- 示例：
  ```swift
  import ViewInspector

  final class DramaCardTests: XCTestCase {
      func test_dramaTitle_displayedCorrectly() throws {
          let drama = Drama.mock(title: "绝世神医")
          let view = DramaCard(drama: drama)

          let title = try view.inspect().find(text: "绝世神医")
          XCTAssertNotNil(title)
      }

      func test_tapCard_callsOnTap() throws {
          let drama = Drama.mock()
          var tapped = false
          let view = DramaCard(drama: drama, onTap: { tapped = true })

          try view.inspect().find(viewWithAccessibilityIdentifier: "drama_card_1").callOnTapGesture()
          XCTAssertTrue(tapped)
      }
  }
  ```
- ViewInspector 测试运行在 XCTest 中，速度远快于 XCUITest，适合放入 CI 每次运行。

---

## 4. 快照测试

### 4.1 SwiftSnapshotTesting

- 使用 **SwiftSnapshotTesting** 对关键页面组件做视觉回归测试。
- 快照策略：
  ```swift
  import SnapshotTesting

  final class HomePageSnapshotTests: XCTestCase {
      func test_homePage_default() {
          let viewModel = HomeViewModel.previewDefault
          let view = HomePage(viewModel: viewModel)
          assertSnapshot(of: view, as: .image(layoutGuide: .device(config: .iPhone15Pro)))
      }

      func test_homePage_loading() {
          let viewModel = HomeViewModel.previewLoading
          let view = HomePage(viewModel: viewModel)
          assertSnapshot(of: view, as: .image(layoutGuide: .device(config: .iPhone15Pro)))
      }
  }
  ```
- 首次运行生成参考快照（`-record` 模式），后续运行做比对。

### 4.2 快照存储

- 参考快照存放在 `ios/ShortDrama/Tests/Snapshots/` 下，按模块-测试类-方法名自动组织。
- 快照文件提交到 Git（通过 Git LFS 或直接提交，图片体积 < 100KB 可接受）。
- 设备 / OS 差异处理：
  - 快照绑定的设备配置（`config: .iPhone15Pro`）和 iOS 版本记录在测试注释中。
  - CI 固定模拟器为 `iPhone 15 Pro (iOS 17.4)`。
  - 若 iOS 小版本更新导致快照差异（通常是渲染微调），更新参考快照并附 PR 说明。
- 快照更新流程：
  1. 本地执行 `SIMULATOR_DEVICE_NAME=iPhone 15 Pro xcodebuild test -record`。
  2. 检查 diff 是否仅为预期内的渲染差异。
  3. 提交新的参考快照。

---

## 5. 测试覆盖率

### 5.1 工具

- 使用 Xcode 内置 Code Coverage：
  - 在 scheme 编辑器中勾选 "Test" → "Options" → "Code Coverage"。
  - 选择目标 "ShortDrama"。
- 生成覆盖率报告：
  ```bash
  xcodebuild test \
    -project ios/ShortDrama.xcodeproj \
    -scheme ShortDrama \
    -destination 'platform=iOS Simulator,name=iPhone 15 Pro,OS=17.4' \
    -enableCodeCoverage YES \
    -resultBundlePath .build/coverage.xcresult

  # 导出 JSON 覆盖率数据
  xcrun xccov view --report --json .build/coverage.xcresult > coverage.json
  ```
- 在 Fastlane `test` lane 中集成覆盖率上报：
  ```ruby
  lane :test do
    run_tests(
      scheme: "ShortDrama",
      devices: ["iPhone 15 Pro"],
      code_coverage: true
    )
  end
  ```

### 5.2 最低覆盖率

| 模块 | 行覆盖率目标 | 说明 |
|------|:---:|------|
| Domain (UseCase) | ≥ 90% | 纯业务逻辑，必须全面覆盖 |
| Domain (Model) | ≥ 80% | 模型映射、自定义编解码逻辑 |
| Data (Repository) | ≥ 75% | 含 MockDataSource 的集成测试 |
| Data (DataSource) | ≥ 60% | 网络层/DTO 解析，部分依赖真实响应 |
| UI (ViewModel) | ≥ 80% | 状态逻辑、事件处理 |
| UI (View) | 不要求硬性 | ViewInspector / 快照测试覆盖关键组件 |

- PR 合并门槛：整体行覆盖率 ≥ 70%，且 Domain+ViewModel 模块不得低于各自目标。
- 新增代码必须附带测试，不允许提交无测试覆盖的新业务逻辑。
- 若某些代码确实难以测试（如系统回调、ScenePhase），在代码中用 `// TESTABILITY:` 注释说明原因。
