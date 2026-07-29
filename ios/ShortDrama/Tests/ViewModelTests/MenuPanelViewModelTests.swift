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

    private func makeViewModel(
        playerRepository: MockPlayerRepository = MockPlayerRepository(),
        messageRepository: MockMessageRepository = MockMessageRepository(),
        sessionStore: MockPlaybackSessionStore = MockPlaybackSessionStore()
    ) -> MenuPanelViewModel {
        MenuPanelViewModel(
            fetchRecentlyViewedUseCase: FetchRecentlyViewedUseCase(repository: playerRepository),
            fetchMessagePreviewUseCase: FetchMessagePreviewUseCase(repository: messageRepository),
            playbackSessionStore: sessionStore
        )
    }

    @Test("T-04: first load enters content state with recently viewed items and preview")
    func testLoadIfNeededSuccessContent() async {
        let repository = MockPlayerRepository()
        repository.recentlyViewedResult = .success([makeItem(), makeItem(dramaId: "drama-002")])
        let messageRepository = MockMessageRepository()
        messageRepository.previewResult = .success(.fixture())
        let sessionStore = MockPlaybackSessionStore()
        let viewModel = makeViewModel(playerRepository: repository, messageRepository: messageRepository, sessionStore: sessionStore)

        await viewModel.loadIfNeeded()

        #expect(viewModel.viewState == .content([makeItem(), makeItem(dramaId: "drama-002")]))
        #expect(viewModel.messagePreviewState == .content(.fixture()))
        #expect(sessionStore.getOrCreateSessionIdCallCount == 1)
        #expect(repository.calls == [.fetchRecentlyViewed(playbackSessionId: "playback-session-001")])
        #expect(messageRepository.calls == [.fetchPreview])
    }

    @Test("T-04: preview 204 empty state is rendered as empty")
    func testLoadIfNeededPreviewEmpty() async {
        let repository = MockPlayerRepository()
        repository.recentlyViewedResult = .success([])
        let messageRepository = MockMessageRepository()
        messageRepository.previewResult = .success(nil)
        let viewModel = makeViewModel(playerRepository: repository, messageRepository: messageRepository)

        await viewModel.loadIfNeeded()

        #expect(viewModel.viewState == .empty)
        #expect(viewModel.messagePreviewState == .empty)
    }

    @Test("T-04: preview failure falls back while recently viewed still loads")
    func testPreviewFailureFallsBack() async {
        let repository = MockPlayerRepository()
        repository.recentlyViewedResult = .success([makeItem()])
        let messageRepository = MockMessageRepository()
        messageRepository.previewResult = .failure(APIError.server(code: 503, message: "服务暂不可用，请稍后重试"))
        let viewModel = makeViewModel(playerRepository: repository, messageRepository: messageRepository)

        await viewModel.loadIfNeeded()

        #expect(viewModel.viewState == .content([makeItem()]))
        #expect(viewModel.messagePreviewState == .error("暂无消息"))
    }

    @Test("T-04: network failure enters error state and retry can recover")
    func testRetryRecoversFromNetworkFailure() async {
        let repository = MockPlayerRepository()
        repository.recentlyViewedResult = .failure(APIError.network(underlying: URLError(.notConnectedToInternet)))
        let messageRepository = MockMessageRepository()
        messageRepository.previewResult = .success(.fixture())
        let sessionStore = MockPlaybackSessionStore()
        let viewModel = makeViewModel(playerRepository: repository, messageRepository: messageRepository, sessionStore: sessionStore)

        await viewModel.loadIfNeeded()
        #expect(viewModel.viewState == .error("网络异常，请检查后重试"))
        #expect(viewModel.messagePreviewState == .content(.fixture()))

        repository.recentlyViewedResult = .success([makeItem(dramaId: "drama-009")])
        await viewModel.retry()

        #expect(viewModel.viewState == .content([makeItem(dramaId: "drama-009")]))
        #expect(sessionStore.getOrCreateSessionIdCallCount == 2)
    }

    @Test("T-04: loadIfNeeded does not repeat request after success")
    func testLoadIfNeededDeduplicatesAfterSuccess() async {
        let repository = MockPlayerRepository()
        repository.recentlyViewedResult = .success([makeItem()])
        let messageRepository = MockMessageRepository()
        let sessionStore = MockPlaybackSessionStore()
        let viewModel = makeViewModel(playerRepository: repository, messageRepository: messageRepository, sessionStore: sessionStore)

        await viewModel.loadIfNeeded()
        await viewModel.loadIfNeeded()

        #expect(repository.calls.count == 1)
        #expect(sessionStore.getOrCreateSessionIdCallCount == 1)
        #expect(messageRepository.calls.count == 1)
    }

    @Test("T-04: route helper rejects empty drama id")
    func testRouteForRecentlyViewedItemRequiresDramaId() {
        let viewModel = makeViewModel()

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
