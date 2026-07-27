import Foundation

/// ViewModel for the search result page.
@MainActor
final class SearchResultViewModel: ObservableObject {

    enum ViewState: Equatable {
        case loading
        case content([Drama])
        case empty
        case error(String)
    }

    private enum Constants {
        static let firstPage = 1
        static let pageSize = 10
        static let maxQueryLength = 50
    }

    @Published var draftQuery: String
    @Published private(set) var submittedQuery: String = ""
    @Published private(set) var viewState: ViewState = .loading

    private let searchDramasUseCase: SearchDramasUseCase
    private let saveSearchHistoryUseCase: SaveSearchHistoryUseCase

    private var hasLoaded = false
    private var currentRequestID = 0
    private var activeQuery: String?

    init(
        initialQuery: String,
        searchDramasUseCase: SearchDramasUseCase,
        saveSearchHistoryUseCase: SaveSearchHistoryUseCase
    ) {
        self.draftQuery = initialQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        self.searchDramasUseCase = searchDramasUseCase
        self.saveSearchHistoryUseCase = saveSearchHistoryUseCase
    }

    var canSubmit: Bool {
        normalizedQuery(draftQuery) != nil
    }

    func loadIfNeeded() async {
        guard !hasLoaded else { return }
        hasLoaded = true
        await submitSearch(query: draftQuery)
    }

    func retry() async {
        await submitSearch(query: submittedQuery)
    }

    func submitSearch() async {
        await submitSearch(query: draftQuery)
    }

    func updateDraftQuery(_ query: String) {
        draftQuery = query
    }

    private func submitSearch(query rawQuery: String) async {
        guard let normalized = normalizedQuery(rawQuery) else {
            return
        }

        if activeQuery == normalized {
            return
        }

        let requestID = currentRequestID + 1
        currentRequestID = requestID
        activeQuery = normalized
        draftQuery = normalized
        submittedQuery = normalized
        viewState = .loading

        do {
            let dramas = try await searchDramasUseCase.execute(
                query: normalized,
                page: Constants.firstPage,
                pageSize: Constants.pageSize
            )

            guard requestID == currentRequestID else { return }

            saveSearchHistoryUseCase.execute(keyword: normalized)
            activeQuery = nil
            viewState = dramas.isEmpty ? .empty : .content(dramas)
        } catch is CancellationError {
            guard requestID == currentRequestID else { return }
            activeQuery = nil
        } catch let error as APIError {
            guard requestID == currentRequestID else { return }
            activeQuery = nil
            viewState = .error(error.errorDescription ?? "搜索失败，请重试")
        } catch {
            guard requestID == currentRequestID else { return }
            activeQuery = nil
            viewState = .error(error.localizedDescription)
        }
    }

    private func normalizedQuery(_ input: String) -> String? {
        let normalized = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty, normalized.count <= Constants.maxQueryLength else {
            return nil
        }
        return normalized
    }
}
