import Foundation
@testable import ShortDrama
import Testing

@MainActor
struct SearchHomeViewModelTests {

    private func makeHistoryRepository(testName: String = #function) -> UserDefaultsSearchHistoryRepository {
        let suiteName = "SearchHomeViewModelTests.\(testName)"
        let defaults = UserDefaults(suiteName: suiteName) ?? .standard
        defaults.removePersistentDomain(forName: suiteName)
        return UserDefaultsSearchHistoryRepository(
            userDefaults: defaults,
            storageKey: "search.history.items"
        )
    }

    @Test("search home loads history and hot searches successfully")
    func testLoadIfNeededSuccess() async {
        let repository = makeHistoryRepository()
        repository.save(keyword: "逆袭")
        repository.save(keyword: "豪门")

        let dramaRepository = MockDramaRepository()
        dramaRepository.hotSearchBehavior = .success([
            HotSearchItem(rank: 1, keyword: "逆袭", score: 9821),
            HotSearchItem(rank: 2, keyword: "豪门", score: 9540),
            HotSearchItem(rank: 3, keyword: "甜宠", score: 9000)
        ])

        let viewModel = SearchHomeViewModel(
            fetchHotSearchesUseCase: FetchHotSearchesUseCase(repository: dramaRepository),
            loadSearchHistoryUseCase: LoadSearchHistoryUseCase(repository: repository),
            clearSearchHistoryUseCase: ClearSearchHistoryUseCase(repository: repository)
        )

        await viewModel.loadIfNeeded()

        #expect(viewModel.historyItems.map(\.keyword) == ["豪门", "逆袭"])
        #expect(viewModel.quickEntries.map(\.title) == ["排行", "新剧", "分类", "演员"])
        #expect(viewModel.hotSearchState == .content([
            HotSearchItem(rank: 1, keyword: "逆袭", score: 9821),
            HotSearchItem(rank: 2, keyword: "豪门", score: 9540),
            HotSearchItem(rank: 3, keyword: "甜宠", score: 9000)
        ]))
        #expect(dramaRepository.fetchHotSearchesCallCount == 1)
    }

    @Test("search home keeps history when hot search fails")
    func testLoadIfNeededHotSearchFailure() async {
        let repository = makeHistoryRepository()
        repository.save(keyword: "逆袭")

        let dramaRepository = MockDramaRepository()
        dramaRepository.hotSearchBehavior = .failure(.server(code: 500, message: "热搜加载失败"))

        let viewModel = SearchHomeViewModel(
            fetchHotSearchesUseCase: FetchHotSearchesUseCase(repository: dramaRepository),
            loadSearchHistoryUseCase: LoadSearchHistoryUseCase(repository: repository),
            clearSearchHistoryUseCase: ClearSearchHistoryUseCase(repository: repository)
        )

        await viewModel.loadIfNeeded()

        #expect(viewModel.historyItems.map(\.keyword) == ["逆袭"])
        #expect(viewModel.hotSearchState == .error("热搜加载失败"))
        #expect(viewModel.quickEntries.count == 4)
    }

    @Test("search home only loads hot searches once")
    func testLoadIfNeededOnlyOnce() async {
        let repository = makeHistoryRepository()
        let dramaRepository = MockDramaRepository()
        dramaRepository.hotSearchBehavior = .success([])

        let viewModel = SearchHomeViewModel(
            fetchHotSearchesUseCase: FetchHotSearchesUseCase(repository: dramaRepository),
            loadSearchHistoryUseCase: LoadSearchHistoryUseCase(repository: repository),
            clearSearchHistoryUseCase: ClearSearchHistoryUseCase(repository: repository)
        )

        await viewModel.loadIfNeeded()
        await viewModel.loadIfNeeded()

        #expect(dramaRepository.fetchHotSearchesCallCount == 1)
    }

    @Test("search home normalizes queries and updates canSubmit")
    func testNormalizedQueryAndCanSubmit() {
        let repository = makeHistoryRepository()
        let viewModel = SearchHomeViewModel(
            fetchHotSearchesUseCase: FetchHotSearchesUseCase(repository: MockDramaRepository()),
            loadSearchHistoryUseCase: LoadSearchHistoryUseCase(repository: repository),
            clearSearchHistoryUseCase: ClearSearchHistoryUseCase(repository: repository)
        )

        #expect(viewModel.canSubmit == false)
        #expect(viewModel.normalizedQuery("   ") == nil)

        viewModel.updateQuery("  逆袭  ")

        #expect(viewModel.normalizedQuery(viewModel.query) == "逆袭")
        #expect(viewModel.canSubmit == true)
    }

    @Test("search home rejects overlong query")
    func testNormalizedQueryRejectsLongInput() {
        let repository = makeHistoryRepository()
        let viewModel = SearchHomeViewModel(
            fetchHotSearchesUseCase: FetchHotSearchesUseCase(repository: MockDramaRepository()),
            loadSearchHistoryUseCase: LoadSearchHistoryUseCase(repository: repository),
            clearSearchHistoryUseCase: ClearSearchHistoryUseCase(repository: repository)
        )

        let overlong = String(repeating: "长", count: 51)

        #expect(viewModel.normalizedQuery(overlong) == nil)
    }

    @Test("search home clears history and updates state")
    func testClearHistoryUpdatesState() {
        let repository = makeHistoryRepository()
        repository.save(keyword: "逆袭")
        repository.save(keyword: "豪门")

        let viewModel = SearchHomeViewModel(
            fetchHotSearchesUseCase: FetchHotSearchesUseCase(repository: MockDramaRepository()),
            loadSearchHistoryUseCase: LoadSearchHistoryUseCase(repository: repository),
            clearSearchHistoryUseCase: ClearSearchHistoryUseCase(repository: repository)
        )

        viewModel.reloadHistory()
        #expect(viewModel.historyItems.count == 2)

        viewModel.clearHistory()

        #expect(viewModel.historyItems.isEmpty)
        #expect(repository.load().isEmpty)
    }

    @Test("search home maps quick entries to routes")
    func testRouteForQuickEntry() {
        let repository = makeHistoryRepository()
        let viewModel = SearchHomeViewModel(
            fetchHotSearchesUseCase: FetchHotSearchesUseCase(repository: MockDramaRepository()),
            loadSearchHistoryUseCase: LoadSearchHistoryUseCase(repository: repository),
            clearSearchHistoryUseCase: ClearSearchHistoryUseCase(repository: repository)
        )

        let rankingEntry = QuickEntry(type: .ranking, title: "排行", systemImage: "chart.bar")
        let newReleasesEntry = QuickEntry(
            type: .newReleases,
            title: "新剧",
            systemImage: "sparkles.tv"
        )
        let classificationEntry = QuickEntry(
            type: .classification,
            title: "分类",
            systemImage: "square.grid.2x2"
        )
        let actorHubEntry = QuickEntry(type: .actorHub, title: "演员", systemImage: "person.2")

        #expect(viewModel.route(for: rankingEntry) == .rankingHome)
        #expect(viewModel.route(for: newReleasesEntry) == .newReleases)
        #expect(viewModel.route(for: classificationEntry) == .classificationHome)
        #expect(viewModel.route(for: actorHubEntry) == .actorHub)
    }

    @Test("search home can retry hot search after failure")
    func testRetryHotSearch() async {
        let repository = makeHistoryRepository()
        let dramaRepository = MockDramaRepository()
        dramaRepository.queuedHotSearchBehaviors = [
            .failure(.network(underlying: URLError(.notConnectedToInternet))),
            .success([HotSearchItem(rank: 1, keyword: "逆袭", score: 9821)])
        ]

        let viewModel = SearchHomeViewModel(
            fetchHotSearchesUseCase: FetchHotSearchesUseCase(repository: dramaRepository),
            loadSearchHistoryUseCase: LoadSearchHistoryUseCase(repository: repository),
            clearSearchHistoryUseCase: ClearSearchHistoryUseCase(repository: repository)
        )

        await viewModel.loadIfNeeded()
        let expectedMessage = "网络请求失败：The operation couldn’t be completed. "
            + "(NSURLErrorDomain error -1009.)"
        #expect(viewModel.hotSearchState == .error(expectedMessage))

        await viewModel.retryHotSearch()

        #expect(viewModel.hotSearchState == .content([
            HotSearchItem(rank: 1, keyword: "逆袭", score: 9821)
        ]))
        #expect(dramaRepository.fetchHotSearchesCallCount == 2)
    }
}
