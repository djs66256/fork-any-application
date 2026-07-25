import Foundation
import Testing
@testable import ShortDrama

@MainActor
struct HomeViewModelTests {

    private func makeDrama(id: String = "drama-001") -> Drama {
        Drama(
            id: id,
            title: "示例短剧",
            description: "首页卡片描述",
            coverUrl: "https://example.com/cover.jpg",
            category: "都市",
            episodeCount: 12,
            tags: ["逆袭", "甜宠"],
            rating: 8.6,
            createdAt: "2026-07-25T00:00:00Z",
            updatedAt: "2026-07-25T00:00:00Z"
        )
    }

    @Test("T-03: HomeViewModel first load enters content state with items")
    func testLoadIfNeededSuccessContent() async {
        let mock = MockDramaRepository()
        mock.behavior = .success([makeDrama()])
        let useCase = FetchDramasUseCase(repository: mock)
        let viewModel = HomeViewModel(fetchDramasUseCase: useCase)

        await viewModel.loadIfNeeded()

        #expect(viewModel.viewState == .content([makeDrama()]))
        #expect(viewModel.isRetrying == false)
        #expect(mock.fetchDramasCallCount == 1)
        #expect(mock.lastRequestedPage == 1)
        #expect(mock.lastRequestedPageSize == 10)
    }

    @Test("T-04: HomeViewModel first load enters empty state")
    func testLoadIfNeededEmpty() async {
        let mock = MockDramaRepository()
        mock.behavior = .success([])
        let useCase = FetchDramasUseCase(repository: mock)
        let viewModel = HomeViewModel(fetchDramasUseCase: useCase)

        await viewModel.loadIfNeeded()

        #expect(viewModel.viewState == .empty)
        #expect(mock.fetchDramasCallCount == 1)
    }

    @Test("T-04: HomeViewModel can retry from empty state to content")
    func testRetryFromEmptyRecoversToContent() async {
        let mock = MockDramaRepository()
        mock.queuedBehaviors = [
            .success([]),
            .success([makeDrama(id: "drama-003")])
        ]
        let useCase = FetchDramasUseCase(repository: mock)
        let viewModel = HomeViewModel(fetchDramasUseCase: useCase)

        await viewModel.loadIfNeeded()
        #expect(viewModel.viewState == .empty)

        await viewModel.retry()

        #expect(viewModel.viewState == .content([makeDrama(id: "drama-003")]))
        #expect(mock.fetchDramasCallCount == 2)
    }

    @Test("T-05: HomeViewModel first load enters error state on failure")
    func testLoadIfNeededError() async {
        let mock = MockDramaRepository()
        mock.behavior = .failure(.server(code: 500, message: "加载失败，请重试"))
        let useCase = FetchDramasUseCase(repository: mock)
        let viewModel = HomeViewModel(fetchDramasUseCase: useCase)

        await viewModel.loadIfNeeded()

        #expect(viewModel.viewState == .error("加载失败，请重试"))
        #expect(viewModel.isRetrying == false)
    }

    @Test("T-06: HomeViewModel retry recovers from error to content")
    func testRetryRecoversToContent() async {
        let mock = MockDramaRepository()
        mock.queuedBehaviors = [
            .failure(.network(underlying: URLError(.notConnectedToInternet))),
            .success([makeDrama(id: "drama-002")])
        ]
        let useCase = FetchDramasUseCase(repository: mock)
        let viewModel = HomeViewModel(fetchDramasUseCase: useCase)

        await viewModel.loadIfNeeded()
        if case .error(let message) = viewModel.viewState {
            #expect(!message.isEmpty)
        } else {
            Issue.record("Expected error state after initial failure")
        }

        await viewModel.retry()

        #expect(viewModel.viewState == .content([makeDrama(id: "drama-002")]))
        #expect(viewModel.isRetrying == false)
        #expect(mock.fetchDramasCallCount == 2)
    }

    @Test("T-06: HomeViewModel ignores duplicate retry while request in flight")
    func testRetryDeduplicatesConcurrentRequests() async {
        let mock = MockDramaRepository()
        mock.behavior = .delayed([makeDrama()], 0.2)
        let useCase = FetchDramasUseCase(repository: mock)
        let viewModel = HomeViewModel(fetchDramasUseCase: useCase)

        await withTaskGroup(of: Void.self) { group in
            group.addTask {
                await viewModel.retry()
            }
            group.addTask {
                await viewModel.retry()
            }
            await group.waitForAll()
        }

        #expect(mock.fetchDramasCallCount == 1)
        #expect(viewModel.viewState == .content([makeDrama()]))
        #expect(viewModel.isRetrying == false)
    }

    @Test("HomeViewModel.loadIfNeeded only loads once after success")
    func testLoadIfNeededOnlyOnce() async {
        let mock = MockDramaRepository()
        mock.behavior = .success([makeDrama()])
        let useCase = FetchDramasUseCase(repository: mock)
        let viewModel = HomeViewModel(fetchDramasUseCase: useCase)

        await viewModel.loadIfNeeded()
        await viewModel.loadIfNeeded()

        #expect(mock.fetchDramasCallCount == 1)
    }
}
