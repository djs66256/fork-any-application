import Foundation
@testable import ShortDrama
import Testing

@MainActor
struct ClassificationViewModelTests {

    private func makePayload(
        gender: ClassificationGender = .all,
        eraTags: [String] = ["都市", "古装"],
        themeTags: [String] = ["逆袭", "甜宠"],
        characterTags: [String] = ["萌宝", "霸总"]
    ) -> ClassificationTagsPayload {
        ClassificationTagsPayload(
            gender: gender,
            dimensions: [
                ClassificationDimension(key: .eraBackground, name: "时代背景", tags: eraTags),
                ClassificationDimension(key: .themePlot, name: "主题情节", tags: themeTags),
                ClassificationDimension(key: .characterSetting, name: "角色设定", tags: characterTags)
            ]
        )
    }

    private func makeViewModel(repository: MockDramaRepository) -> ClassificationViewModel {
        ClassificationViewModel(
            fetchClassificationTagsUseCase: FetchClassificationTagsUseCase(repository: repository)
        )
    }

    @Test("classification loads default gender all with first dimension selected")
    func testLoadDefaultGenderAll() async {
        let repository = MockDramaRepository()
        repository.classificationBehavior = .success(makePayload())
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()

        #expect(viewModel.selectedGender == .all)
        #expect(viewModel.selectedDimension == .eraBackground)
        #expect(viewModel.viewState == .content(makePayload().dimensions))
        #expect(repository.fetchClassificationTagsCallCount == 1)
        #expect(repository.lastClassificationGender == .all)
    }

    @Test("classification keeps fixed three dimensions and preserves empty dimension")
    func testKeepsFixedDimensionsAndEmptySection() async {
        let repository = MockDramaRepository()
        let payload = makePayload(themeTags: [])
        repository.classificationBehavior = .success(payload)
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()

        guard case .content(let dimensions) = viewModel.viewState else {
            Issue.record("Expected content state")
            return
        }

        #expect(dimensions.count == 3)
        #expect(dimensions.map(\.key) == ClassificationDimensionKey.allCases)
        #expect(dimensions[1].key == .themePlot)
        #expect(dimensions[1].tags.isEmpty)
    }

    @Test("gender fast switch only consumes latest result and resets selected dimension")
    func testOnlyLatestGenderResultWinsAndResetsDimension() async {
        let repository = MockDramaRepository()
        let allPayload = makePayload(gender: .all)
        let malePayload = makePayload(gender: .male, eraTags: ["都市男频"], themeTags: ["战神"], characterTags: ["龙王"])
        let femalePayload = makePayload(gender: .female, eraTags: ["古言"], themeTags: ["闪婚"], characterTags: ["大女主"])
        repository.queuedClassificationBehaviors = [
            .success(allPayload),
            .delayed(malePayload, 0.2),
            .success(femalePayload)
        ]
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()
        viewModel.selectDimension(.characterSetting)

        await withTaskGroup(of: Void.self) { group in
            group.addTask {
                await viewModel.selectGender(.male)
            }
            group.addTask {
                try? await Task.sleep(for: .milliseconds(20))
                await viewModel.selectGender(.female)
            }
            await group.waitForAll()
        }

        #expect(viewModel.selectedGender == .female)
        #expect(viewModel.selectedDimension == .eraBackground)
        #expect(viewModel.viewState == .content(femalePayload.dimensions))
    }

    @Test("select dimension and visible dimension stay synchronized")
    func testDimensionSelectionAndVisibleUpdateSync() async {
        let repository = MockDramaRepository()
        repository.classificationBehavior = .success(makePayload())
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()
        viewModel.selectDimension(.themePlot)
        #expect(viewModel.selectedDimension == .themePlot)

        viewModel.updateVisibleDimension(.characterSetting)
        #expect(viewModel.selectedDimension == .characterSetting)
    }

    @Test("normalizedTagQuery trims valid input and blocks blank or overlong tag")
    func testNormalizedTagQuery() {
        let repository = MockDramaRepository()
        let viewModel = makeViewModel(repository: repository)
        let overlong = String(repeating: "长", count: 51)

        #expect(viewModel.normalizedTagQuery("  萌宝  ") == "萌宝")
        #expect(viewModel.normalizedTagQuery("   ") == nil)
        #expect(viewModel.normalizedTagQuery(overlong) == nil)
    }

    @Test("retry recovers from first load failure")
    func testRetryAfterFailure() async {
        let repository = MockDramaRepository()
        let success = makePayload(gender: .all, eraTags: ["民国"], themeTags: ["复仇"], characterTags: [])
        repository.queuedClassificationBehaviors = [
            .failure(.server(code: 500, message: "分类加载失败")),
            .success(success)
        ]
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()
        #expect(viewModel.viewState == .error("分类加载失败"))

        await viewModel.retry()

        #expect(viewModel.selectedDimension == .eraBackground)
        #expect(viewModel.viewState == .content(success.dimensions))
        #expect(repository.fetchClassificationTagsCallCount == 2)
    }
}
