import Foundation
@testable import ShortDrama
import Testing

@MainActor
struct SearchResultViewModelTests {

    private func makeHistoryRepository(testName: String = #function) -> UserDefaultsSearchHistoryRepository {
        let suiteName = "SearchResultViewModelTests.\(testName)"
        let defaults = UserDefaults(suiteName: suiteName) ?? .standard
        defaults.removePersistentDomain(forName: suiteName)
        return UserDefaultsSearchHistoryRepository(
            userDefaults: defaults,
            storageKey: "search.history.items"
        )
    }

    private func makeDrama(id: String, title: String) -> Drama {
        Drama(
            id: id,
            title: title,
            description: "描述",
            coverUrl: "https://example.com/cover.jpg",
            category: "都市",
            episodeCount: 12,
            tags: ["逆袭"],
            rating: 8.6,
            createdAt: "2026-07-25T00:00:00Z",
            updatedAt: "2026-07-25T00:00:00Z"
        )
    }

    @Test("search result loads content and saves history on success")
    func testLoadInitialQuerySuccess() async {
        let repository = makeHistoryRepository()
        let dramaRepository = MockDramaRepository()
        let dramas = [makeDrama(id: "drama-001", title: "逆袭归来")]
        dramaRepository.searchBehavior = .success(dramas)

        let viewModel = SearchResultViewModel(
            initialQuery: "  逆袭  ",
            searchDramasUseCase: SearchDramasUseCase(repository: dramaRepository),
            saveSearchHistoryUseCase: SaveSearchHistoryUseCase(repository: repository)
        )

        await viewModel.loadIfNeeded()

        #expect(viewModel.draftQuery == "逆袭")
        #expect(viewModel.submittedQuery == "逆袭")
        #expect(viewModel.viewState == .content(dramas))
        #expect(repository.load().map(\.keyword) == ["逆袭"])
        #expect(dramaRepository.searchDramasCallCount == 1)
        #expect(dramaRepository.lastSearchQuery == "逆袭")
    }

    @Test("search result writes history for empty result")
    func testLoadInitialQueryEmptyStillSavesHistory() async {
        let repository = makeHistoryRepository()
        let dramaRepository = MockDramaRepository()
        dramaRepository.searchBehavior = .success([])

        let viewModel = SearchResultViewModel(
            initialQuery: "冷门词",
            searchDramasUseCase: SearchDramasUseCase(repository: dramaRepository),
            saveSearchHistoryUseCase: SaveSearchHistoryUseCase(repository: repository)
        )

        await viewModel.loadIfNeeded()

        #expect(viewModel.submittedQuery == "冷门词")
        #expect(viewModel.viewState == .empty)
        #expect(repository.load().map(\.keyword) == ["冷门词"])
    }

    @Test("search result failure does not save history")
    func testLoadInitialQueryFailureDoesNotSaveHistory() async {
        let repository = makeHistoryRepository()
        let dramaRepository = MockDramaRepository()
        dramaRepository.searchBehavior = .failure(.server(code: 500, message: "搜索失败，请重试"))

        let viewModel = SearchResultViewModel(
            initialQuery: "逆袭",
            searchDramasUseCase: SearchDramasUseCase(repository: dramaRepository),
            saveSearchHistoryUseCase: SaveSearchHistoryUseCase(repository: repository)
        )

        await viewModel.loadIfNeeded()

        #expect(viewModel.viewState == .error("搜索失败，请重试"))
        #expect(repository.load().isEmpty)
    }

    @Test("search result retry reuses submitted query")
    func testRetryUsesSubmittedQuery() async {
        let repository = makeHistoryRepository()
        let dramaRepository = MockDramaRepository()
        let dramas = [makeDrama(id: "drama-001", title: "逆袭归来")]
        dramaRepository.queuedSearchBehaviors = [
            .failure(.network(underlying: URLError(.timedOut))),
            .success(dramas)
        ]

        let viewModel = SearchResultViewModel(
            initialQuery: "逆袭",
            searchDramasUseCase: SearchDramasUseCase(repository: dramaRepository),
            saveSearchHistoryUseCase: SaveSearchHistoryUseCase(repository: repository)
        )

        await viewModel.loadIfNeeded()
        #expect(viewModel.viewState != .content(dramas))

        await viewModel.retry()

        #expect(viewModel.submittedQuery == "逆袭")
        #expect(viewModel.viewState == .content(dramas))
        #expect(repository.load().map(\.keyword) == ["逆袭"])
        #expect(dramaRepository.searchDramasCallCount == 2)
    }

    @Test("search result submit with new query updates result and history")
    func testSubmitSearchWithNewQuery() async {
        let repository = makeHistoryRepository()
        repository.save(keyword: "旧词")

        let dramaRepository = MockDramaRepository()
        let dramas = [makeDrama(id: "drama-002", title: "豪门密令")]
        dramaRepository.searchBehavior = .success(dramas)

        let viewModel = SearchResultViewModel(
            initialQuery: "旧词",
            searchDramasUseCase: SearchDramasUseCase(repository: dramaRepository),
            saveSearchHistoryUseCase: SaveSearchHistoryUseCase(repository: repository)
        )

        viewModel.updateDraftQuery("  豪门  ")
        await viewModel.submitSearch()

        #expect(viewModel.submittedQuery == "豪门")
        #expect(viewModel.draftQuery == "豪门")
        #expect(viewModel.viewState == .content(dramas))
        #expect(repository.load().map(\.keyword) == ["豪门", "旧词"])
    }

    @Test("search result rejects blank query without request")
    func testSubmitSearchRejectsBlankQuery() async {
        let repository = makeHistoryRepository()
        let dramaRepository = MockDramaRepository()

        let viewModel = SearchResultViewModel(
            initialQuery: "逆袭",
            searchDramasUseCase: SearchDramasUseCase(repository: dramaRepository),
            saveSearchHistoryUseCase: SaveSearchHistoryUseCase(repository: repository)
        )

        viewModel.updateDraftQuery("   ")
        await viewModel.submitSearch()

        #expect(dramaRepository.searchDramasCallCount == 0)
        #expect(viewModel.canSubmit == false)
    }

    @Test("search result ignores duplicate in-flight query")
    func testSubmitSearchDeduplicatesSameQuery() async {
        let repository = makeHistoryRepository()
        let dramaRepository = MockDramaRepository()
        dramaRepository.searchBehavior = .delayed(
            [makeDrama(id: "drama-001", title: "逆袭归来")],
            0.2
        )

        let viewModel = SearchResultViewModel(
            initialQuery: "逆袭",
            searchDramasUseCase: SearchDramasUseCase(repository: dramaRepository),
            saveSearchHistoryUseCase: SaveSearchHistoryUseCase(repository: repository)
        )
        viewModel.updateDraftQuery("逆袭")

        await withTaskGroup(of: Void.self) { group in
            group.addTask { await viewModel.submitSearch() }
            group.addTask { await viewModel.submitSearch() }
            await group.waitForAll()
        }

        #expect(dramaRepository.searchDramasCallCount == 1)
    }

    @Test("search result keeps last result when switching query quickly")
    func testSubmitSearchKeepsLastQueryResult() async {
        let repository = makeHistoryRepository()
        let dramaRepository = MockDramaRepository()
        let firstResult = [makeDrama(id: "drama-001", title: "逆袭归来")]
        let secondResult = [makeDrama(id: "drama-002", title: "豪门密令")]
        dramaRepository.queuedSearchBehaviors = [
            .delayed(firstResult, 0.2),
            .success(secondResult)
        ]

        let viewModel = SearchResultViewModel(
            initialQuery: "逆袭",
            searchDramasUseCase: SearchDramasUseCase(repository: dramaRepository),
            saveSearchHistoryUseCase: SaveSearchHistoryUseCase(repository: repository)
        )

        await withTaskGroup(of: Void.self) { group in
            group.addTask {
                await MainActor.run {
                    viewModel.updateDraftQuery("逆袭")
                }
                await viewModel.submitSearch()
            }
            group.addTask {
                try? await Task.sleep(for: .milliseconds(20))
                await MainActor.run {
                    viewModel.updateDraftQuery("豪门")
                }
                await viewModel.submitSearch()
            }
            await group.waitForAll()
        }

        #expect(viewModel.submittedQuery == "豪门")
        #expect(viewModel.viewState == .content(secondResult))
        #expect(repository.load().map(\.keyword).first == "豪门")
        #expect(dramaRepository.searchDramasCallCount == 2)
    }
}
