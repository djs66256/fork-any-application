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
        checkInRepository: MockCheckInRepository = MockCheckInRepository(),
        installationIdStore: MockInstallationIdStore = MockInstallationIdStore(),
        dismissStore: MockCheckInPopupDismissStore = MockCheckInPopupDismissStore(),
        isUserLoggedIn: @escaping @Sendable () -> Bool = { false },
        accessTokenProvider: @escaping @Sendable () -> String? = { nil },
        featuredDramaPopupAutoHideDuration: Duration = .seconds(3),
        featuredDramaPopupSleep: @escaping @Sendable (Duration) async throws -> Void = { duration in
            try await Task.sleep(for: duration)
        }
    ) -> HomeViewModel {
        HomeViewModel(
            fetchDramasUseCase: FetchDramasUseCase(repository: repository),
            commentRepository: commentRepository,
            fetchCheckInStatusUseCase: FetchCheckInStatusUseCase(repository: checkInRepository),
            submitCheckInUseCase: SubmitCheckInUseCase(repository: checkInRepository),
            installationIdStore: installationIdStore,
            dismissStore: dismissStore,
            isUserLoggedIn: isUserLoggedIn,
            accessTokenProvider: accessTokenProvider,
            featuredDramaPopupAutoHideDuration: featuredDramaPopupAutoHideDuration,
            featuredDramaPopupSleep: featuredDramaPopupSleep
        )
    }

    @Test("T-03: HomeViewModel first load enters content state with items")
    func testLoadIfNeededSuccessContent() async {
        let mock = MockDramaRepository()
        mock.behavior = .success([makeDrama()])
        let checkInRepository = MockCheckInRepository()
        checkInRepository.fetchStatusResult = .success(.fixture())
        let viewModel = makeViewModel(repository: mock, checkInRepository: checkInRepository)

        await viewModel.loadIfNeeded()

        #expect(viewModel.viewState == .content([makeDrama()]))
        #expect(viewModel.isRetrying == false)
        #expect(viewModel.isFeaturedDramaPopupVisible == true)
        #expect(mock.fetchDramasCallCount == 1)
        #expect(mock.lastRequestedPage == 1)
        #expect(mock.lastRequestedPageSize == 10)
        #expect(viewModel.checkInPopupState?.serverDate == "2026-07-29")
    }

    @Test("T-04: HomeViewModel first load enters empty state")
    func testLoadIfNeededEmpty() async {
        let mock = MockDramaRepository()
        mock.behavior = .success([])
        let checkInRepository = MockCheckInRepository()
        checkInRepository.fetchStatusResult = .success(.fixture(shouldShowPopup: false))
        let viewModel = makeViewModel(repository: mock, checkInRepository: checkInRepository)

        await viewModel.loadIfNeeded()

        #expect(viewModel.viewState == .empty)
        #expect(mock.fetchDramasCallCount == 1)
        #expect(viewModel.checkInPopupState == nil)
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

    @Test("T-03: dismissed server date suppresses popup")
    func testDismissedServerDateSuppressesPopup() async {
        let mock = MockDramaRepository()
        mock.behavior = .success([makeDrama()])
        let checkInRepository = MockCheckInRepository()
        checkInRepository.fetchStatusResult = .success(.fixture(serverDate: "2026-07-29"))
        let dismissStore = MockCheckInPopupDismissStore()
        dismissStore.dismissedDates.insert("2026-07-29")
        let viewModel = makeViewModel(
            repository: mock,
            checkInRepository: checkInRepository,
            dismissStore: dismissStore
        )

        await viewModel.loadIfNeeded()

        #expect(viewModel.checkInPopupState == nil)
    }

    @Test("T-03: logged-in state sends access token and keeps installation id for precedence")
    func testLoggedInCheckInUsesAccessTokenAndInstallationId() async {
        let mock = MockDramaRepository()
        mock.behavior = .success([makeDrama()])
        let checkInRepository = MockCheckInRepository()
        checkInRepository.fetchStatusResult = .success(.fixture())
        checkInRepository.submitCheckInResult = .success(.signedFixture())
        let installationIdStore = MockInstallationIdStore()
        installationIdStore.installationId = "installation-auth-001"
        let viewModel = makeViewModel(
            repository: mock,
            checkInRepository: checkInRepository,
            installationIdStore: installationIdStore,
            isUserLoggedIn: { true },
            accessTokenProvider: { "access-token-001" }
        )

        await viewModel.loadIfNeeded()
        await viewModel.submitCheckIn()

        #expect(checkInRepository.calls == [
            .fetchStatus(installationId: "installation-auth-001", accessToken: "access-token-001"),
            .submitCheckIn(installationId: "installation-auth-001", accessToken: "access-token-001")
        ])
    }

    @Test("T-03: blocking login overlay skips popup for this cold start")
    func testBlockingLoginOverlaySkipsPopup() async {
        let mock = MockDramaRepository()
        mock.behavior = .success([makeDrama()])
        let checkInRepository = MockCheckInRepository()
        checkInRepository.fetchStatusResult = .success(.fixture())
        let viewModel = makeViewModel(repository: mock, checkInRepository: checkInRepository, isUserLoggedIn: { true })
        viewModel.updateAuthState(isUserLoggedIn: true, accessToken: nil)

        await viewModel.loadIfNeeded()

        #expect(viewModel.checkInPopupState == nil)
        #expect(checkInRepository.calls.isEmpty)
    }

    @Test("T-03: active comment sheet causes popup to be skipped for this cold start")
    func testExistingCommentSheetSkipsPopup() async {
        let mock = MockDramaRepository()
        mock.behavior = .success([makeDrama(id: "drama-001")])
        let checkInRepository = MockCheckInRepository()
        checkInRepository.fetchStatusResult = .success(.fixture())
        let viewModel = makeViewModel(repository: mock, checkInRepository: checkInRepository)
        viewModel.openComments(dramaId: "drama-001")

        await viewModel.loadIfNeeded()

        #expect(viewModel.checkInPopupState == nil)
    }

    @Test("T-03: dismissing popup records server date")
    func testDismissCheckInPopupMarksDismissedDate() async {
        let mock = MockDramaRepository()
        mock.behavior = .success([makeDrama()])
        let checkInRepository = MockCheckInRepository()
        checkInRepository.fetchStatusResult = .success(.fixture())
        let dismissStore = MockCheckInPopupDismissStore()
        let viewModel = makeViewModel(
            repository: mock,
            checkInRepository: checkInRepository,
            dismissStore: dismissStore
        )

        await viewModel.loadIfNeeded()
        viewModel.dismissCheckInPopup()

        #expect(dismissStore.markedDates == ["2026-07-29"])
        #expect(viewModel.checkInPopupState == nil)
    }

    @Test("T-03: successful check-in updates popup to signed state")
    func testSubmitCheckInUpdatesPopupState() async {
        let mock = MockDramaRepository()
        mock.behavior = .success([makeDrama()])
        let checkInRepository = MockCheckInRepository()
        checkInRepository.fetchStatusResult = .success(.fixture())
        checkInRepository.submitCheckInResult = .success(.signedFixture())
        let viewModel = makeViewModel(repository: mock, checkInRepository: checkInRepository)

        await viewModel.loadIfNeeded()
        await viewModel.submitCheckIn()

        #expect(viewModel.checkInPopupState?.todaySigned == true)
        #expect(viewModel.checkInPopupState?.feedbackMessage == "签到成功")
    }

    @Test("T-03: failed check-in keeps popup open and retryable")
    func testSubmitCheckInFailureKeepsPopupOpen() async {
        let mock = MockDramaRepository()
        mock.behavior = .success([makeDrama()])
        let checkInRepository = MockCheckInRepository()
        checkInRepository.fetchStatusResult = .success(.fixture())
        checkInRepository.submitCheckInResult = .failure(APIError.server(code: 503, message: "服务暂不可用，请稍后重试"))
        let viewModel = makeViewModel(repository: mock, checkInRepository: checkInRepository)

        await viewModel.loadIfNeeded()
        await viewModel.submitCheckIn()

        #expect(viewModel.checkInPopupState?.todaySigned == false)
        #expect(viewModel.checkInPopupState?.isError == true)
        #expect(viewModel.checkInPopupState?.feedbackMessage == "服务暂不可用，请稍后重试")
    }
}
