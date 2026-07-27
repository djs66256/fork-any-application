import Foundation
@testable import ShortDrama
import Testing

@MainActor
struct MenuPanelViewModelTests {
    private func makeItem(dramaId: String = "drama-001") -> RecentlyViewedItem {
        RecentlyViewedItem(
            dramaId: dramaId,
            title: "逆袭归来后我成了豪门团宠",
            coverURL: "https://example.com/cover.jpg",
            episodeNumber: 12,
            progress: 128.5,
            updatedAt: "2026-07-27T15:20:00.000Z"
        )
    }

    @Test("T-06: first load enters content state with recently viewed items")
    func testLoadIfNeededSuccessContent() async {
        let repository = MockPlayerRepository()
        repository.recentlyViewedResult = .success([makeItem(), makeItem(dramaId: "drama-002")])
        let sessionStore = MockPlaybackSessionStore()
        let viewModel = MenuPanelViewModel(
            fetchRecentlyViewedUseCase: FetchRecentlyViewedUseCase(repository: repository),
            playbackSessionStore: sessionStore
        )

        await viewModel.loadIfNeeded()

        #expect(viewModel.viewState == .content([makeItem(), makeItem(dramaId: "drama-002")]))
        #expect(sessionStore.getOrCreateSessionIdCallCount == 1)
        #expect(repository.calls == [.fetchRecentlyViewed(playbackSessionId: "playback-session-001")])
    }

    @Test("T-07: empty response enters empty state")
    func testLoadIfNeededEmpty() async {
        let repository = MockPlayerRepository()
        repository.recentlyViewedResult = .success([])
        let sessionStore = MockPlaybackSessionStore()
        let viewModel = MenuPanelViewModel(
            fetchRecentlyViewedUseCase: FetchRecentlyViewedUseCase(repository: repository),
            playbackSessionStore: sessionStore
        )

        await viewModel.loadIfNeeded()

        #expect(viewModel.viewState == .empty)
        #expect(sessionStore.getOrCreateSessionIdCallCount == 1)
    }

    @Test("T-07: session store failure enters local error state")
    func testLoadIfNeededSessionFailure() async {
        struct SessionError: Error {}

        let repository = MockPlayerRepository()
        let sessionStore = MockPlaybackSessionStore()
        sessionStore.stubbedError = SessionError()
        let viewModel = MenuPanelViewModel(
            fetchRecentlyViewedUseCase: FetchRecentlyViewedUseCase(repository: repository),
            playbackSessionStore: sessionStore
        )

        await viewModel.loadIfNeeded()

        #expect(viewModel.viewState == .error("加载失败，请稍后重试"))
        #expect(repository.calls.isEmpty)
    }

    @Test("T-07: network failure enters error state and retry can recover")
    func testRetryRecoversFromNetworkFailure() async {
        let repository = MockPlayerRepository()
        repository.recentlyViewedResult = .failure(APIError.network(underlying: URLError(.notConnectedToInternet)))
        let sessionStore = MockPlaybackSessionStore()
        let viewModel = MenuPanelViewModel(
            fetchRecentlyViewedUseCase: FetchRecentlyViewedUseCase(repository: repository),
            playbackSessionStore: sessionStore
        )

        await viewModel.loadIfNeeded()
        #expect(viewModel.viewState == .error("网络异常，请检查后重试"))

        repository.recentlyViewedResult = .success([makeItem(dramaId: "drama-009")])
        await viewModel.retry()

        #expect(viewModel.viewState == .content([makeItem(dramaId: "drama-009")]))
        #expect(sessionStore.getOrCreateSessionIdCallCount == 2)
    }

    @Test("T-08: loadIfNeeded does not repeat request after success")
    func testLoadIfNeededDeduplicatesAfterSuccess() async {
        let repository = MockPlayerRepository()
        repository.recentlyViewedResult = .success([makeItem()])
        let sessionStore = MockPlaybackSessionStore()
        let viewModel = MenuPanelViewModel(
            fetchRecentlyViewedUseCase: FetchRecentlyViewedUseCase(repository: repository),
            playbackSessionStore: sessionStore
        )

        await viewModel.loadIfNeeded()
        await viewModel.loadIfNeeded()

        #expect(repository.calls.count == 1)
        #expect(sessionStore.getOrCreateSessionIdCallCount == 1)
    }

    @Test("T-08: route helper rejects empty drama id")
    func testRouteForRecentlyViewedItemRequiresDramaId() {
        let repository = MockPlayerRepository()
        let sessionStore = MockPlaybackSessionStore()
        let viewModel = MenuPanelViewModel(
            fetchRecentlyViewedUseCase: FetchRecentlyViewedUseCase(repository: repository),
            playbackSessionStore: sessionStore
        )

        let emptyItem = RecentlyViewedItem(
            dramaId: "   ",
            title: "无效数据",
            coverURL: nil,
            episodeNumber: 1,
            progress: 0,
            updatedAt: "2026-07-27T15:20:00.000Z"
        )

        #expect(viewModel.route(for: emptyItem) == nil)
        #expect(viewModel.route(for: makeItem()) == .player(videoId: "drama-001"))
    }
}
