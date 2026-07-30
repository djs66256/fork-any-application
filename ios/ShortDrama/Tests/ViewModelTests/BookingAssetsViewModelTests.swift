import Foundation
@testable import ShortDrama
import Testing

@MainActor
struct BookingAssetsViewModelTests {
    private func makeAsset(
        id: String,
        status: BookingAssetAvailabilityStatus = .online,
        bookedAt: String = "2026-07-30T03:25:00.000Z"
    ) -> BookingAsset {
        BookingAsset(
            dramaID: id,
            title: "短剧\(id)",
            coverURL: "https://example.com/\(id).jpg",
            episodeCount: 12,
            bookedAt: bookedAt,
            availabilityStatus: status
        )
    }

    private func makePage(
        items: [BookingAsset],
        page: Int = 1,
        totalPages: Int = 1,
        total: Int? = nil,
        summary: BookingAssetSummary = BookingAssetSummary(onlineCount: 1, upcomingCount: 1)
    ) -> BookingAssetPage {
        BookingAssetPage(
            items: items,
            page: page,
            pageSize: BookingAssetQuery.defaultPageSize,
            total: total ?? items.count,
            totalPages: totalPages,
            summary: summary
        )
    }

    private func makeViewModel(repository: MockDramaRepository) -> BookingAssetsViewModel {
        BookingAssetsViewModel(
            fetchBookingAssetsUseCase: FetchBookingAssetsUseCase(repository: repository)
        )
    }

    @Test("booking route metadata uses home tab and menu path")
    func testBookingRouteMetadata() {
        #expect(AppRoute.bookingAssets.owningTab == .home)
        #expect(AppRoute.bookingAssets.publicRouteName == "menu/booking")
    }

    @Test("anonymous state does not request protected endpoint")
    func testAnonymousDoesNotRequestProtectedEndpoint() async {
        let repository = MockDramaRepository()
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded(accessToken: nil)

        #expect(repository.fetchBookingAssetsCallCount == 0)
        #expect(viewModel.requiresLogin == true)
        #expect(viewModel.viewState == .idle)
        #expect(viewModel.summary == .empty)
    }

    @Test("booking first page loads with default online query and summary")
    func testFirstPageLoadsDefaultQueryAndSummary() async {
        let repository = MockDramaRepository()
        let summary = BookingAssetSummary(onlineCount: 8, upcomingCount: 3)
        let page = makePage(items: [makeAsset(id: "asset-001")], totalPages: 2, total: 8, summary: summary)
        repository.bookingAssetsBehavior = .success(page)
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded(accessToken: "token-001")

        #expect(repository.fetchBookingAssetsCallCount == 1)
        #expect(repository.lastBookingAssetsAccessToken == "token-001")
        #expect(repository.lastBookingAssetsQuery == BookingAssetQuery(status: .online, page: 1, pageSize: 20))
        #expect(viewModel.selectedStatus == .online)
        #expect(viewModel.summary == summary)
        #expect(viewModel.viewState == .content(page.items))
        #expect(viewModel.requiresLogin == false)
    }

    @Test("booking tab switch ignores stale response and keeps latest result")
    func testOnlyLatestBookingTabRequestWins() async {
        let repository = MockDramaRepository()
        let delayedOnline = makePage(
            items: [makeAsset(id: "online-old")],
            summary: BookingAssetSummary(onlineCount: 1, upcomingCount: 2)
        )
        let latestUpcoming = makePage(
            items: [makeAsset(id: "upcoming-new", status: .upcoming)],
            summary: BookingAssetSummary(onlineCount: 3, upcomingCount: 4)
        )
        repository.queuedBookingAssetsBehaviors = [
            .delayed(delayedOnline, 0.2),
            .success(latestUpcoming)
        ]
        let viewModel = makeViewModel(repository: repository)

        await withTaskGroup(of: Void.self) { group in
            group.addTask {
                await viewModel.loadIfNeeded(accessToken: "token-001")
            }
            group.addTask {
                try? await Task.sleep(for: .milliseconds(20))
                await viewModel.selectStatus(.upcoming, accessToken: "token-001")
            }
            await group.waitForAll()
        }

        #expect(viewModel.selectedStatus == .upcoming)
        #expect(viewModel.summary == latestUpcoming.summary)
        #expect(viewModel.viewState == .content(latestUpcoming.items))
        #expect(repository.lastBookingAssetsQuery == BookingAssetQuery(status: .upcoming, page: 1, pageSize: 20))
    }

    @Test("append failure keeps existing content and exposes local error")
    func testAppendFailureKeepsExistingContent() async {
        let repository = MockDramaRepository()
        let firstPage = makePage(
            items: [makeAsset(id: "page-1")],
            page: 1,
            totalPages: 2,
            total: 2,
            summary: BookingAssetSummary(onlineCount: 2, upcomingCount: 0)
        )
        repository.queuedBookingAssetsBehaviors = [
            .success(firstPage),
            .failure(.network(underlying: URLError(.timedOut)))
        ]
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded(accessToken: "token-001")
        await viewModel.loadMoreIfNeeded(accessToken: "token-001")

        #expect(viewModel.viewState == .content(firstPage.items))
        #expect(viewModel.isAppending == false)
        #expect(viewModel.appendErrorMessage != nil)
        #expect(repository.lastBookingAssetsQuery == BookingAssetQuery(status: .online, page: 2, pageSize: 20))
    }

    @Test("401 response clears content and returns to login gate")
    func testUnauthorizedResetsToLoginGate() async {
        let repository = MockDramaRepository()
        repository.bookingAssetsBehavior = .failure(.server(code: 401, message: "请先登录后查看预约"))
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded(accessToken: "token-401")

        #expect(viewModel.requiresLogin == true)
        #expect(viewModel.viewState == .idle)
        #expect(viewModel.summary == .empty)
    }
}
