import Foundation
@testable import ShortDrama
import Testing

@MainActor
struct TheaterViewModelTests {

    private func makeDrama(
        id: String,
        title: String = "剧场短剧",
        heat: Int = 98_210,
        tags: [String]? = ["逆袭", "豪门"]
    ) -> TheaterDrama {
        TheaterDrama(
            id: id,
            title: title,
            description: "剧场卡片描述",
            coverUrl: "https://example.com/\(id).jpg",
            category: "都市",
            episodeCount: 68,
            tags: tags,
            rating: 8.9,
            createdAt: "2026-07-25T00:00:00Z",
            updatedAt: "2026-07-25T00:00:00Z",
            heat: heat
        )
    }

    private func makePage(
        channel: TheaterChannel = .all,
        items: [TheaterDrama],
        page: Int = 1,
        pageSize: Int = 20,
        totalPages: Int = 1,
        total: Int? = nil
    ) -> TheaterFeedPage {
        TheaterFeedPage(
            channel: channel,
            items: items,
            page: page,
            pageSize: pageSize,
            total: total ?? items.count,
            totalPages: totalPages
        )
    }

    private func makeViewModel(repository: MockDramaRepository = MockDramaRepository()) -> TheaterViewModel {
        TheaterViewModel(
            fetchTheaterFeedUseCase: FetchTheaterFeedUseCase(repository: repository)
        )
    }

    @Test("T-02: theater loads default all first page")
    func testLoadDefaultFirstPage() async {
        let repository = MockDramaRepository()
        let firstPage = makePage(items: [makeDrama(id: "theater-001")], totalPages: 2, total: 2)
        repository.theaterBehavior = .success(firstPage)
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()

        #expect(viewModel.selectedChannel == .all)
        #expect(viewModel.viewState == .content(firstPage.items))
        #expect(repository.fetchTheaterFeedCallCount == 1)
        #expect(repository.lastTheaterQuery == TheaterFeedQuery(channel: .all, page: 1, pageSize: 20))
    }

    @Test("T-03: non-all channel empty response becomes empty state")
    func testSelectNonAllChannelShowsEmptyState() async {
        let repository = MockDramaRepository()
        repository.queuedTheaterBehaviors = [
            .success(makePage(items: [makeDrama(id: "all-001")], totalPages: 1)),
            .success(makePage(channel: .real, items: [], totalPages: 1, total: 0))
        ]
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()
        await viewModel.selectChannel(.real)

        #expect(viewModel.selectedChannel == .real)
        #expect(viewModel.viewState == .empty)
        #expect(repository.lastTheaterQuery == TheaterFeedQuery(channel: .real, page: 1, pageSize: 20))
    }

    @Test("T-04: out-of-order channel requests keep only latest result")
    func testOnlyLatestChannelRequestWins() async {
        let repository = MockDramaRepository()
        let delayed = makePage(items: [makeDrama(id: "old-all")])
        let latest = makePage(channel: .anime, items: [], totalPages: 1, total: 0)
        repository.queuedTheaterBehaviors = [
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
                await viewModel.selectChannel(.anime)
            }
            await group.waitForAll()
        }

        #expect(viewModel.selectedChannel == .anime)
        #expect(viewModel.viewState == .empty)
    }

    @Test("T-05: load more appends next page")
    func testLoadMoreAppends() async {
        let repository = MockDramaRepository()
        let firstPage = makePage(items: [makeDrama(id: "page-1")], page: 1, totalPages: 2, total: 2)
        let secondPage = makePage(items: [makeDrama(id: "page-2")], page: 2, totalPages: 2, total: 2)
        repository.queuedTheaterBehaviors = [
            .success(firstPage),
            .success(secondPage)
        ]
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()
        await viewModel.loadMoreIfNeeded()

        #expect(viewModel.viewState == .content(firstPage.items + secondPage.items))
        #expect(repository.lastTheaterQuery == TheaterFeedQuery(channel: .all, page: 2, pageSize: 20))
    }

    @Test("T-06: load more failure keeps content and append error")
    func testLoadMoreFailureKeepsContent() async {
        let repository = MockDramaRepository()
        let firstPage = makePage(items: [makeDrama(id: "page-1")], page: 1, totalPages: 2, total: 2)
        repository.queuedTheaterBehaviors = [
            .success(firstPage),
            .failure(.network(underlying: URLError(.timedOut)))
        ]
        let viewModel = makeViewModel(repository: repository)

        await viewModel.loadIfNeeded()
        await viewModel.loadMoreIfNeeded()

        #expect(viewModel.viewState == .content(firstPage.items))
        #expect(viewModel.appendErrorMessage != nil)
        #expect(viewModel.isAppending == false)
    }

    @Test("T-07: search and shortcut interactions emit correct route effects")
    func testInteractionsEmitRouteEffects() async {
        let viewModel = makeViewModel()

        viewModel.openSearch()
        #expect(viewModel.routeEffect == .navigate(.searchHome))
        viewModel.clearRouteEffect()

        viewModel.openShortcut(.classification)
        #expect(viewModel.routeEffect == .navigate(.classificationHome))
        viewModel.clearRouteEffect()

        viewModel.openShortcut(.ranking)
        #expect(
            viewModel.routeEffect == .openRanking(
                TheaterRankingEntryContext(contentType: .all, rankingType: .hot)
            )
        )
        viewModel.clearRouteEffect()

        viewModel.openShortcut(.booking)
        #expect(
            viewModel.routeEffect == .openRanking(
                TheaterRankingEntryContext(contentType: .all, rankingType: .booking)
            )
        )
        viewModel.clearRouteEffect()

        viewModel.openShortcut(.newReleases)
        #expect(viewModel.routeEffect == .navigate(.newReleases))
        viewModel.clearRouteEffect()

        viewModel.openScanPlaceholder()
        #expect(viewModel.routeEffect == .showScanPlaceholder("识图功能开发中"))
    }

    @Test("T-08: tapping drama navigates only with valid id")
    func testTapDramaRouteValidation() {
        let viewModel = makeViewModel()

        viewModel.openDrama(makeDrama(id: "play-001"))
        #expect(viewModel.routeEffect == .navigate(.player(videoId: "play-001")))

        viewModel.clearRouteEffect()
        viewModel.openDrama(makeDrama(id: ""))
        #expect(viewModel.routeEffect == nil)
    }
}
