import Foundation

/// ViewModel for the search discovery page.
@MainActor
final class SearchHomeViewModel: ObservableObject {

    enum HotSearchState: Equatable {
        case idle
        case loading
        case content([HotSearchItem])
        case error(String)
    }

    private enum Constants {
        static let maxQueryLength = 50
    }

    @Published var query = ""
    @Published private(set) var historyItems: [SearchHistoryItem] = []
    @Published private(set) var hotSearchState: HotSearchState = .idle
    @Published private(set) var quickEntries: [QuickEntry] = QuickEntry.defaults

    private let fetchHotSearchesUseCase: FetchHotSearchesUseCase
    private let loadSearchHistoryUseCase: LoadSearchHistoryUseCase
    private let clearSearchHistoryUseCase: ClearSearchHistoryUseCase

    private var hasLoaded = false
    private var hotSearchTask: Task<Void, Never>?

    init(
        fetchHotSearchesUseCase: FetchHotSearchesUseCase,
        loadSearchHistoryUseCase: LoadSearchHistoryUseCase,
        clearSearchHistoryUseCase: ClearSearchHistoryUseCase
    ) {
        self.fetchHotSearchesUseCase = fetchHotSearchesUseCase
        self.loadSearchHistoryUseCase = loadSearchHistoryUseCase
        self.clearSearchHistoryUseCase = clearSearchHistoryUseCase
    }

    var canSubmit: Bool {
        normalizedQuery(query) != nil
    }

    func loadIfNeeded() async {
        guard !hasLoaded else { return }
        hasLoaded = true
        reloadHistory()
        await loadHotSearches()
    }

    func retryHotSearch() async {
        await loadHotSearches()
    }

    func updateQuery(_ query: String) {
        self.query = query
    }

    func normalizedQuery(_ input: String) -> String? {
        let normalized = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty, normalized.count <= Constants.maxQueryLength else {
            return nil
        }
        return normalized
    }

    func reloadHistory() {
        historyItems = loadSearchHistoryUseCase.execute()
    }

    func clearHistory() {
        clearSearchHistoryUseCase.execute()
        reloadHistory()
    }

    func route(for entry: QuickEntry) -> AppRoute {
        switch entry.type {
        case .ranking:
            return .rankingHome
        case .newReleases:
            return .newReleases
        case .classification:
            return .classificationHome
        case .actorHub:
            return .actorHub
        }
    }

    private func loadHotSearches() async {
        hotSearchTask?.cancel()
        hotSearchState = .loading

        let task = Task {
            do {
                let items = try await fetchHotSearchesUseCase.execute()
                guard !Task.isCancelled else { return }
                await MainActor.run {
                    self.hotSearchState = .content(items)
                }
            } catch let error as APIError {
                guard !Task.isCancelled else { return }
                await MainActor.run {
                    self.hotSearchState = .error(error.errorDescription ?? "热搜加载失败")
                }
            } catch {
                guard !Task.isCancelled else { return }
                await MainActor.run {
                    self.hotSearchState = .error(error.localizedDescription)
                }
            }
        }

        hotSearchTask = task
        await task.value
    }
}
