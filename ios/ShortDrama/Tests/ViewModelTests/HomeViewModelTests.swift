import Foundation
@testable import ShortDrama
import Testing

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

    private func makeViewModel(
        repository: MockDramaRepository,
        commentRepository: MockCommentRepository = MockCommentRepository(),
        isUserLoggedIn: @escaping @Sendable () -> Bool = { false }
    ) -> HomeViewModel {
        HomeViewModel(
            fetchDramasUseCase: FetchDramasUseCase(repository: repository),
            commentRepository: commentRepository,
            isUserLoggedIn: isUserLoggedIn
        )
    }

    @Test("T-03: HomeViewModel first load enters content state with items")
    func testLoadIfNeededSuccessContent() async {
        let mock = MockDramaRepository()
        mock.behavior = .success([makeDrama()])
        let viewModel = makeViewModel(repository: mock)

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
        let viewModel = makeViewModel(repository: mock)

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
        let viewModel = makeViewModel(repository: mock)

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
        let viewModel = makeViewModel(repository: mock)

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
        let viewModel = makeViewModel(repository: mock)

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
        let viewModel = makeViewModel(repository: mock)

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
        let viewModel = makeViewModel(repository: mock)

        await viewModel.loadIfNeeded()
        await viewModel.loadIfNeeded()

        #expect(mock.fetchDramasCallCount == 1)
    }

    @Test("T-08: Home comments entry opens a single active sheet context")
    func testOpenCommentsCreatesSheetContext() {
        let viewModel = makeViewModel(repository: MockDramaRepository())

        viewModel.openComments(for: makeDrama(id: "drama-comment"))

        #expect(viewModel.activeCommentSheet == .init(id: "drama-comment"))
    }

    @Test("T-08: closing comment sheet does not affect feed state")
    func testCloseCommentsKeepsMainFeedState() async {
        let mock = MockDramaRepository()
        mock.behavior = .success([makeDrama(id: "drama-001")])
        let viewModel = makeViewModel(repository: mock)

        await viewModel.loadIfNeeded()
        viewModel.openComments(dramaId: "drama-001")
        viewModel.closeComments()

        #expect(viewModel.activeCommentSheet == nil)
        #expect(viewModel.viewState == .content([makeDrama(id: "drama-001")]))
    }

    @Test("T-09: Home login restore only reopens comment context")
    func testRestoreCommentContextOnlyRestoresSheet() {
        let viewModel = makeViewModel(repository: MockDramaRepository())
        let context = CommentLoginContext(
            source: .home,
            dramaId: "drama-restore",
            action: PendingCommentAction(kind: .toggleLike, commentId: "comment-1")
        )

        viewModel.restoreCommentContext(context)

        #expect(viewModel.activeCommentSheet == .init(id: "drama-restore"))
        #expect(viewModel.pendingCommentLoginContext == nil)
    }

    @Test("T-09: Home login required stores context without replaying action")
    func testHandleCommentLoginRequiredStoresContext() {
        let viewModel = makeViewModel(repository: MockDramaRepository())
        let context = CommentLoginContext(
            source: .home,
            dramaId: "drama-login",
            action: PendingCommentAction(kind: .createComment, commentId: nil)
        )

        viewModel.handleCommentLoginRequired(context)

        #expect(viewModel.pendingCommentLoginContext == context)
        #expect(viewModel.activeCommentSheet == .init(id: "drama-login"))
    }
}
