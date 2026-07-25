import Foundation

/// ViewModel for the home screen feed.
@MainActor
final class HomeViewModel: ObservableObject {

    enum ViewState: Equatable {
        case loading
        case content([Drama])
        case empty
        case error(String)
    }

    private enum Constants {
        static let firstPage = 1
        static let pageSize = 10
    }

    // MARK: - Published State

    @Published private(set) var viewState: ViewState = .loading
    @Published private(set) var isRetrying = false

    // MARK: - Dependencies

    private let fetchDramasUseCase: FetchDramasUseCase

    // MARK: - State

    private var hasLoaded = false
    private var isRequestInFlight = false

    // MARK: - Init

    init(fetchDramasUseCase: FetchDramasUseCase) {
        self.fetchDramasUseCase = fetchDramasUseCase
    }

    // MARK: - Actions

    func loadIfNeeded() async {
        guard !hasLoaded else { return }
        await loadDramas()
    }

    func loadDramas() async {
        await performLoad(isRetry: false)
    }

    func retry() async {
        await performLoad(isRetry: true)
    }

    // MARK: - Private

    private func performLoad(isRetry: Bool) async {
        guard !isRequestInFlight else { return }

        isRequestInFlight = true
        if isRetry {
            isRetrying = true
        } else {
            viewState = .loading
        }

        defer {
            isRequestInFlight = false
            isRetrying = false
            hasLoaded = true
        }

        do {
            let dramas = try await fetchDramasUseCase.execute(
                page: Constants.firstPage,
                pageSize: Constants.pageSize
            )

            if dramas.isEmpty {
                viewState = .empty
            } else {
                viewState = .content(dramas)
            }
        } catch let error as APIError {
            viewState = .error(error.errorDescription ?? "加载失败，请重试")
        } catch {
            viewState = .error(error.localizedDescription)
        }
    }
}
