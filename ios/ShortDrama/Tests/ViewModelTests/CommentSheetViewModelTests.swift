import Foundation
@testable import ShortDrama
import Testing

@MainActor
struct CommentSheetViewModelTests {
    private func makeComment(
        id: String,
        dramaId: String = "drama-001",
        content: String = "评论内容",
        likeCount: Int = 0,
        liked: Bool = false,
        createdAt: String = "2026-07-29T09:30:00.000Z"
    ) -> ShortDrama.Comment {
        ShortDrama.Comment(
            id: id,
            dramaId: dramaId,
            content: content,
            likeCount: likeCount,
            liked: liked,
            createdAt: createdAt,
            updatedAt: createdAt,
            user: CommentUserSummary(id: "user-\(id)", displayName: "用户\(id)", avatarUrl: nil)
        )
    }

    private func makePage(
        items: [ShortDrama.Comment],
        page: Int = 1,
        pageSize: Int = 20,
        total: Int? = nil,
        totalPages: Int = 1
    ) -> PagedResult<ShortDrama.Comment> {
        PagedResult(
            items: items,
            page: page,
            pageSize: pageSize,
            total: total ?? items.count,
            totalPages: totalPages
        )
    }

    private func makeViewModel(
        repository: MockCommentRepository,
        source: CommentLoginContext.Source = .home,
        dramaId: String = "drama-001",
        isUserLoggedIn: @escaping @Sendable () -> Bool = { true }
    ) -> CommentSheetViewModel {
        CommentSheetViewModel(
            dramaId: dramaId,
            source: source,
            fetchDramaCommentsUseCase: FetchDramaCommentsUseCase(repository: repository),
            createCommentUseCase: CreateCommentUseCase(repository: repository),
            toggleCommentLikeUseCase: ToggleCommentLikeUseCase(repository: repository),
            isUserLoggedIn: isUserLoggedIn
        )
    }

    @Test("T-03: loadIfNeeded enters content and uses pagination total")
    func testLoadIfNeededContent() async {
        let repository = MockCommentRepository()
        let comments = [makeComment(id: "c1"), makeComment(id: "c2")]
        repository.fetchBehavior = .success(makePage(items: comments, total: 36, totalPages: 2))
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()

        #expect(viewModel.listState == .content)
        #expect(viewModel.comments == comments)
        #expect(viewModel.totalCount == 36)
        #expect(viewModel.selectedSort == .latest)
        #expect(repository.calls == [
            .fetch(CommentQuery(dramaId: "drama-001", page: 1, pageSize: 20, sort: .latest))
        ])
    }

    @Test("T-03: loadIfNeeded enters empty when API returns no comments")
    func testLoadIfNeededEmpty() async {
        let repository = MockCommentRepository()
        repository.fetchBehavior = .success(makePage(items: [], total: 0, totalPages: 0))
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()

        #expect(viewModel.listState == .empty)
        #expect(viewModel.comments.isEmpty)
        #expect(viewModel.totalCount == 0)
    }

    @Test("T-04: retry recovers from first page failure")
    func testRetryRecoversFromError() async {
        let repository = MockCommentRepository()
        let recovered = [makeComment(id: "recovered")]
        repository.queuedFetchBehaviors = [
            .failure(.server(code: 500, message: "加载失败，请稍后重试")),
            .success(makePage(items: recovered, total: 1, totalPages: 1))
        ]
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()
        #expect(viewModel.listState == .error("加载失败，请稍后重试"))

        await viewModel.retry()

        #expect(viewModel.listState == .content)
        #expect(viewModel.comments == recovered)
    }

    @Test("T-04: selectSort resets list and requests first page with new sort")
    func testSelectSortReloadsFirstPage() async {
        let repository = MockCommentRepository()
        repository.queuedFetchBehaviors = [
            .success(makePage(items: [makeComment(id: "latest-1")], total: 2, totalPages: 1)),
            .success(makePage(items: [makeComment(id: "hot-1", likeCount: 50)], total: 1, totalPages: 1))
        ]
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()
        await viewModel.selectSort(.hot)

        #expect(viewModel.selectedSort == .hot)
        #expect(viewModel.comments == [makeComment(id: "hot-1", likeCount: 50)])
        #expect(repository.calls == [
            .fetch(CommentQuery(dramaId: "drama-001", page: 1, pageSize: 20, sort: .latest)),
            .fetch(CommentQuery(dramaId: "drama-001", page: 1, pageSize: 20, sort: .hot))
        ])
    }

    @Test("T-04: loadMoreIfNeeded appends next page without overwriting current list")
    func testLoadMoreAppends() async {
        let repository = MockCommentRepository()
        let firstPage = [makeComment(id: "c1"), makeComment(id: "c2")]
        let secondPage = [makeComment(id: "c3")]
        repository.queuedFetchBehaviors = [
            .success(makePage(items: firstPage, page: 1, total: 3, totalPages: 2)),
            .success(makePage(items: secondPage, page: 2, total: 3, totalPages: 2))
        ]
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()
        await viewModel.loadMoreIfNeeded()

        #expect(viewModel.comments == firstPage + secondPage)
        #expect(viewModel.appendState == .noMore)
    }

    @Test("T-04: append failure keeps current list and exposes footer error")
    func testLoadMoreFailureKeepsCurrentList() async {
        let repository = MockCommentRepository()
        let firstPage = [makeComment(id: "c1")]
        repository.queuedFetchBehaviors = [
            .success(makePage(items: firstPage, page: 1, total: 2, totalPages: 2)),
            .failure(.network(underlying: URLError(.timedOut)))
        ]
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()
        await viewModel.loadMoreIfNeeded()

        #expect(viewModel.comments == firstPage)
        if case .error(let message) = viewModel.appendState {
            #expect(!message.isEmpty)
        } else {
            Issue.record("Expected appendState.error after pagination failure")
        }
    }

    @Test("T-05: logged in submit inserts new comment at top and clears input")
    func testSubmitCommentSuccess() async {
        let repository = MockCommentRepository()
        let existing = makeComment(id: "existing", createdAt: "2026-07-29T09:00:00.000Z")
        let created = makeComment(id: "new", content: "新评论", createdAt: "2026-07-29T10:00:00.000Z")
        repository.fetchBehavior = .success(makePage(items: [existing], total: 1, totalPages: 1))
        repository.createBehavior = .success(created)
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()
        viewModel.inputText = "  新评论  "
        await viewModel.submitComment()

        #expect(viewModel.comments == [created, existing])
        #expect(viewModel.totalCount == 2)
        #expect(viewModel.inputText.isEmpty)
        #expect(repository.calls.suffix(1).first == .create(dramaId: "drama-001", content: "新评论"))
    }

    @Test("T-06: guest submit emits require-login context and does not call create")
    func testSubmitCommentRequiresLogin() async {
        let repository = MockCommentRepository()
        let viewModel = makeViewModel(repository: repository, source: .player, isUserLoggedIn: { false })

        viewModel.inputText = "未登录评论"
        await viewModel.submitComment()

        #expect(repository.calls.isEmpty)
        #expect(viewModel.routeEffect == .requireLogin(
            CommentLoginContext(
                source: .player,
                dramaId: "drama-001",
                action: PendingCommentAction(kind: .createComment, commentId: nil)
            )
        ))
    }

    @Test("T-07: logged in toggleLike updates only target comment")
    func testToggleLikeSuccess() async {
        let repository = MockCommentRepository()
        let target = makeComment(id: "target", likeCount: 1, liked: false)
        let other = makeComment(id: "other", likeCount: 5, liked: true)
        repository.fetchBehavior = .success(makePage(items: [target, other], total: 2, totalPages: 1))
        repository.toggleLikeBehavior = .success(
            ToggleCommentLikeResult(commentId: "target", liked: true, likeCount: 2)
        )
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()
        await viewModel.toggleLike(commentID: "target")

        #expect(viewModel.comments[0] == makeComment(id: "target", likeCount: 2, liked: true))
        #expect(viewModel.comments[1] == other)
    }

    @Test("T-07: guest toggleLike emits require-login and leaves list unchanged")
    func testToggleLikeRequiresLogin() async {
        let repository = MockCommentRepository()
        let target = makeComment(id: "target", likeCount: 1, liked: false)
        repository.fetchBehavior = .success(makePage(items: [target], total: 1, totalPages: 1))
        let viewModel = makeViewModel(repository: repository, isUserLoggedIn: { false })

        await viewModel.loadIfNeeded()
        await viewModel.toggleLike(commentID: "target")

        #expect(viewModel.comments == [target])
        #expect(repository.calls == [.fetch(CommentQuery(dramaId: "drama-001", page: 1, pageSize: 20, sort: .latest))])
        #expect(viewModel.routeEffect == .requireLogin(
            CommentLoginContext(
                source: .home,
                dramaId: "drama-001",
                action: PendingCommentAction(kind: .toggleLike, commentId: "target")
            )
        ))
    }

    @Test("T-09: restoreLoginContext only reopens matching sheet without replaying action")
    func testRestoreLoginContextOnlyRestoresOpenState() async {
        let repository = MockCommentRepository()
        let viewModel = makeViewModel(repository: repository, source: .home)
        let createContext = CommentLoginContext(
            source: .home,
            dramaId: "drama-001",
            action: PendingCommentAction(kind: .createComment, commentId: nil)
        )

        viewModel.restoreLoginContext(createContext)

        #expect(viewModel.shouldRestoreOpenSheet == true)
        #expect(repository.calls.isEmpty)
        #expect(viewModel.routeEffect == nil)
    }

    @Test("empty input is blocked locally without network request")
    func testSubmitCommentRejectsBlankInput() async {
        let repository = MockCommentRepository()
        let viewModel = makeViewModel(repository: repository)

        viewModel.inputText = "   \n   "
        await viewModel.submitComment()

        #expect(repository.calls.isEmpty)
        #expect(viewModel.composerErrorMessage == "请输入 1~500 字评论")
    }

    @Test("duplicate like taps while one request in flight are deduplicated")
    func testToggleLikeDeduplicatesWhileSubmitting() async {
        let repository = MockCommentRepository()
        let target = makeComment(id: "target")
        repository.fetchBehavior = .success(makePage(items: [target], total: 1, totalPages: 1))
        repository.toggleLikeBehavior = .delayed(
            ToggleCommentLikeResult(commentId: "target", liked: true, likeCount: 1),
            0.2
        )
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()

        await withTaskGroup(of: Void.self) { group in
            group.addTask { await viewModel.toggleLike(commentID: "target") }
            group.addTask { await viewModel.toggleLike(commentID: "target") }
            await group.waitForAll()
        }

        #expect(repository.calls.filter {
            if case .toggleLike(dramaId: "drama-001", commentId: "target") = $0 {
                return true
            }
            return false
        }.count == 1)
    }
}
