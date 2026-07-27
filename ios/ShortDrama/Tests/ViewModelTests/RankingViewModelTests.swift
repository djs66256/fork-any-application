import Foundation
@testable import ShortDrama
import Testing

@MainActor
struct RankingViewModelTests {

    private func makeRankingDrama(
        id: String,
        title: String = "排行短剧",
        contentType: RankingContentType = .liveAction,
        playCount: Int = 100,
        bookingCount: Int = 20,
        recommendationScore: Double = 88.8,
        isBooked: Bool = false,
        isBookingSubmitting: Bool = false
    ) -> RankingDrama {
        RankingDrama(
            id: id,
            title: title,
            description: "排行榜描述",
            coverUrl: "https://example.com/\(id).jpg",
            category: "都市",
            episodeCount: 12,
            tags: ["逆袭"],
            rating: 8.6,
            createdAt: "2026-07-25T00:00:00Z",
            updatedAt: "2026-07-25T00:00:00Z",
            contentType: contentType,
            playCount: playCount,
            bookingCount: bookingCount,
            recommendationScore: recommendationScore,
            isBooked: isBooked,
            isBookingSubmitting: isBookingSubmitting
        )
    }

    private func makePage(
        items: [RankingDrama],
        page: Int = 1,
        totalPages: Int = 1,
        total: Int? = nil
    ) -> PagedResult<RankingDrama> {
        PagedResult(
            items: items,
            page: page,
            pageSize: 10,
            total: total ?? items.count,
            totalPages: totalPages
        )
    }

    private func makeViewModel(
        repository: MockDramaRepository,
        isUserLoggedIn: @escaping @Sendable () -> Bool = { false }
    ) -> RankingViewModel {
        RankingViewModel(
            fetchRankingsUseCase: FetchRankingsUseCase(repository: repository),
            bookDramaUseCase: BookDramaUseCase(repository: repository),
            isUserLoggedIn: isUserLoggedIn
        )
    }

    @Test("ranking loads default all + hot first page")
    func testLoadDefaultFirstPage() async {
        let repository = MockDramaRepository()
        let firstPage = makePage(items: [makeRankingDrama(id: "ranking-001")], totalPages: 2)
        repository.rankingBehavior = .success(firstPage)
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()

        #expect(viewModel.selectedContentType == .all)
        #expect(viewModel.selectedRankingType == .hot)
        #expect(viewModel.viewState == .content(firstPage.items))
        #expect(repository.fetchRankingsCallCount == 1)
        #expect(
            repository.lastRankingQuery == RankingQuery(
                type: .hot,
                contentType: .all,
                page: 1,
                pageSize: 10
            )
        )
    }

    @Test("switching primary tab preserves secondary tab and reloads first page")
    func testSelectContentTypePreservesRankingType() async {
        let repository = MockDramaRepository()
        repository.queuedRankingBehaviors = [
            .success(makePage(items: [makeRankingDrama(id: "hot-1")], totalPages: 1)),
            .success(makePage(items: [makeRankingDrama(id: "recommend-ai-1", contentType: .ai)], totalPages: 1))
        ]
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()
        await viewModel.selectRankingType(.recommend)
        await viewModel.selectContentType(.ai)

        #expect(viewModel.selectedContentType == .ai)
        #expect(viewModel.selectedRankingType == .recommend)
        #expect(repository.lastRankingQuery == RankingQuery(type: .recommend, contentType: .ai, page: 1, pageSize: 10))
    }

    @Test("switching secondary tab preserves primary tab and reloads first page")
    func testSelectRankingTypePreservesContentType() async {
        let repository = MockDramaRepository()
        repository.queuedRankingBehaviors = [
            .success(makePage(items: [makeRankingDrama(id: "initial")], totalPages: 1)),
            .success(makePage(items: [makeRankingDrama(id: "ai-booking", contentType: .ai)], totalPages: 1)),
            .success(makePage(items: [makeRankingDrama(id: "ai-hot", contentType: .ai)], totalPages: 1))
        ]
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()
        await viewModel.selectContentType(.ai)
        await viewModel.selectRankingType(.booking)

        #expect(viewModel.selectedContentType == .ai)
        #expect(viewModel.selectedRankingType == .booking)
        #expect(repository.lastRankingQuery == RankingQuery(type: .booking, contentType: .ai, page: 1, pageSize: 10))
    }

    @Test("out-of-order tab requests keep only latest result")
    func testOnlyLatestRequestWins() async {
        let repository = MockDramaRepository()
        let delayed = makePage(items: [makeRankingDrama(id: "old-result")])
        let latest = makePage(items: [makeRankingDrama(id: "new-result", contentType: .ai)])
        repository.queuedRankingBehaviors = [
            .delayed(delayed, 0.2),
            .success(latest)
        ]
        let viewModel = makeViewModel(repository: repository)

        await withTaskGroup(of: Void.self) { group in
            group.addTask {
                await viewModel.loadIfNeeded()
            }
            group.addTask {
                try? await Task.sleep(for: .milliseconds(20))
                await viewModel.selectContentType(.ai)
            }
            await group.waitForAll()
        }

        #expect(viewModel.selectedContentType == .ai)
        #expect(viewModel.viewState == .content(latest.items))
    }

    @Test("retry recovers from first-page failure")
    func testRetryAfterFailure() async {
        let repository = MockDramaRepository()
        let success = makePage(items: [makeRankingDrama(id: "recovered")])
        repository.queuedRankingBehaviors = [
            .failure(.server(code: 500, message: "排行榜加载失败")),
            .success(success)
        ]
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()
        #expect(viewModel.viewState == .error("排行榜加载失败"))

        await viewModel.retry()

        #expect(viewModel.viewState == .content(success.items))
        #expect(repository.fetchRankingsCallCount == 2)
    }

    @Test("load more appends next page")
    func testLoadMoreAppends() async {
        let repository = MockDramaRepository()
        let first = makePage(items: [makeRankingDrama(id: "page-1")], page: 1, totalPages: 2, total: 2)
        let second = makePage(items: [makeRankingDrama(id: "page-2")], page: 2, totalPages: 2, total: 2)
        repository.queuedRankingBehaviors = [
            .success(first),
            .success(second)
        ]
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()
        await viewModel.loadMoreIfNeeded()

        #expect(viewModel.viewState == .content(first.items + second.items))
        #expect(repository.lastRankingQuery == RankingQuery(type: .hot, contentType: .all, page: 2, pageSize: 10))
    }

    @Test("load more failure keeps existing content and exposes append error")
    func testLoadMoreFailureKeepsContent() async {
        let repository = MockDramaRepository()
        let first = makePage(items: [makeRankingDrama(id: "page-1")], page: 1, totalPages: 2, total: 2)
        repository.queuedRankingBehaviors = [
            .success(first),
            .failure(.network(underlying: URLError(.timedOut)))
        ]
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()
        await viewModel.loadMoreIfNeeded()

        #expect(viewModel.viewState == .content(first.items))
        #expect(viewModel.appendErrorMessage != nil)
        #expect(viewModel.isAppending == false)
    }

    @Test("guest booking is intercepted without network request")
    func testGuestBookingRequiresLogin() async {
        let repository = MockDramaRepository()
        let page = makePage(items: [makeRankingDrama(id: "booking-1")])
        repository.queuedRankingBehaviors = [
            .success(makePage(items: [makeRankingDrama(id: "initial")])) ,
            .success(page)
        ]
        let viewModel = makeViewModel(repository: repository, isUserLoggedIn: { false })

        await viewModel.loadIfNeeded()
        await viewModel.selectRankingType(.booking)
        await viewModel.book(drama: page.items[0])

        #expect(repository.bookDramaCallCount == 0)
        #expect(viewModel.bookingErrorMessage == "请先登录后再预约")
        #expect(viewModel.routeEffect == .requireLogin(
            RankingLoginContext(source: "ranking", contentType: .all, rankingType: .booking, dramaID: "booking-1")
        ))
    }

    @Test("logged in booking updates current item locally")
    func testBookingSuccessUpdatesCurrentItem() async {
        let repository = MockDramaRepository()
        let drama = makeRankingDrama(id: "booking-1", bookingCount: 20)
        repository.queuedRankingBehaviors = [
            .success(makePage(items: [makeRankingDrama(id: "initial")])) ,
            .success(makePage(items: [drama]))
        ]
        repository.bookingBehavior = .success(
            BookDramaResult(dramaID: "booking-1", booked: true, bookingCount: 21)
        )
        let viewModel = makeViewModel(repository: repository, isUserLoggedIn: { true })

        await viewModel.loadIfNeeded()
        await viewModel.selectRankingType(.booking)
        await viewModel.book(drama: drama)

        guard case .content(let items) = viewModel.viewState else {
            Issue.record("Expected content state")
            return
        }
        #expect(items.first?.isBooked == true)
        #expect(items.first?.bookingCount == 21)
        #expect(items.first?.isBookingSubmitting == false)
        #expect(repository.bookDramaCallCount == 1)
    }

    @Test("401 booking failure becomes login interception")
    func testBookingUnauthorizedMapsToLogin() async {
        let repository = MockDramaRepository()
        let drama = makeRankingDrama(id: "booking-401")
        repository.queuedRankingBehaviors = [
            .success(makePage(items: [makeRankingDrama(id: "initial")])) ,
            .success(makePage(items: [drama]))
        ]
        repository.bookingBehavior = .failure(.server(code: 401, message: "请先登录后再预约"))
        let viewModel = makeViewModel(repository: repository, isUserLoggedIn: { true })

        await viewModel.loadIfNeeded()
        await viewModel.selectRankingType(.booking)
        await viewModel.book(drama: drama)

        #expect(viewModel.bookingErrorMessage == "请先登录后再预约")
        #expect(viewModel.routeEffect == .requireLogin(
            RankingLoginContext(source: "ranking", contentType: .all, rankingType: .booking, dramaID: "booking-401")
        ))
    }

    @Test("duplicate booking taps while submitting only send one request")
    func testDuplicateBookingTapDeduplicates() async {
        let repository = MockDramaRepository()
        let drama = makeRankingDrama(id: "booking-slow")
        repository.queuedRankingBehaviors = [
            .success(makePage(items: [makeRankingDrama(id: "initial")])) ,
            .success(makePage(items: [drama]))
        ]
        repository.bookingBehavior = .delayed(
            BookDramaResult(dramaID: "booking-slow", booked: true, bookingCount: 30),
            0.2
        )
        let viewModel = makeViewModel(repository: repository, isUserLoggedIn: { true })

        await viewModel.loadIfNeeded()
        await viewModel.selectRankingType(.booking)

        await withTaskGroup(of: Void.self) { group in
            group.addTask { await viewModel.book(drama: drama) }
            group.addTask { await viewModel.book(drama: drama) }
            await group.waitForAll()
        }

        #expect(repository.bookDramaCallCount == 1)
    }
}
