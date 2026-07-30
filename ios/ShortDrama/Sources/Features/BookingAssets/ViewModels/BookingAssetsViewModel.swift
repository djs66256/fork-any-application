import Foundation

@MainActor
final class BookingAssetsViewModel: ObservableObject {
    enum ViewState: Equatable {
        case idle
        case loading
        case content([BookingAsset])
        case empty
        case error(String)
    }

    private enum Constants {
        static let firstPage = BookingAssetQuery.defaultPage
        static let pageSize = BookingAssetQuery.defaultPageSize
    }

    @Published private(set) var selectedStatus: BookingAssetAvailabilityStatus = .online
    @Published private(set) var summary: BookingAssetSummary = .empty
    @Published private(set) var viewState: ViewState = .idle
    @Published private(set) var isAppending = false
    @Published private(set) var appendErrorMessage: String?
    @Published private(set) var requiresLogin = false

    private let fetchBookingAssetsUseCase: FetchBookingAssetsUseCase

    private var hasLoaded = false
    private var isFirstPageLoading = false
    private var currentPage = 0
    private var totalPages = 1
    private var currentItems: [BookingAsset] = []
    private var requestToken = UUID()

    init(fetchBookingAssetsUseCase: FetchBookingAssetsUseCase) {
        self.fetchBookingAssetsUseCase = fetchBookingAssetsUseCase
    }

    func loadIfNeeded(accessToken: String?) async {
        if accessToken == nil {
            resetForLoginGate()
            return
        }

        guard !hasLoaded else { return }
        hasLoaded = true
        await reloadFirstPage(accessToken: accessToken)
    }

    func retry(accessToken: String?) async {
        await reloadFirstPage(accessToken: accessToken)
    }

    func selectStatus(_ status: BookingAssetAvailabilityStatus, accessToken: String?) async {
        guard status != selectedStatus else { return }
        selectedStatus = status
        await reloadFirstPage(accessToken: accessToken)
    }

    func loadMoreIfNeeded(accessToken: String?) async {
        guard let accessToken,
              !requiresLogin,
              !isAppending,
              !isFirstPageLoading,
              !currentItems.isEmpty,
              currentPage < totalPages else {
            return
        }

        isAppending = true
        appendErrorMessage = nil

        let nextPage = currentPage + 1
        let token = requestToken

        do {
            let response = try await fetchBookingAssetsUseCase.execute(
                query: makeQuery(page: nextPage),
                accessToken: accessToken
            )

            guard token == requestToken else { return }

            summary = response.summary
            currentPage = response.page
            totalPages = max(response.totalPages, response.page)
            currentItems.append(contentsOf: response.items)
            viewState = .content(currentItems)
        } catch let error as APIError {
            guard token == requestToken else { return }
            if isUnauthorized(error) {
                resetForLoginGate()
            } else {
                appendErrorMessage = mapAppendError(error)
            }
        } catch {
            guard token == requestToken else { return }
            appendErrorMessage = error.localizedDescription
        }

        if token == requestToken {
            isAppending = false
        }
    }

    func handleAuthStatusChange(isAuthenticated: Bool) {
        if isAuthenticated {
            requiresLogin = false
            return
        }

        resetForLoginGate()
    }

    private func reloadFirstPage(accessToken: String?) async {
        guard let accessToken else {
            resetForLoginGate()
            return
        }

        requestToken = UUID()
        let token = requestToken
        isFirstPageLoading = true
        isAppending = false
        appendErrorMessage = nil
        requiresLogin = false
        currentPage = 0
        totalPages = 1
        currentItems = []
        viewState = .loading

        defer {
            if token == requestToken {
                isFirstPageLoading = false
            }
        }

        do {
            let response = try await fetchBookingAssetsUseCase.execute(
                query: makeQuery(page: Constants.firstPage),
                accessToken: accessToken
            )

            guard token == requestToken else { return }

            summary = response.summary
            currentPage = response.page
            totalPages = max(response.totalPages, response.page)
            currentItems = response.items
            viewState = response.items.isEmpty ? .empty : .content(response.items)
        } catch let error as APIError {
            guard token == requestToken else { return }
            if isUnauthorized(error) {
                resetForLoginGate()
            } else {
                viewState = .error(mapFirstPageError(error))
            }
        } catch {
            guard token == requestToken else { return }
            viewState = .error(error.localizedDescription)
        }
    }

    private func makeQuery(page: Int) -> BookingAssetQuery {
        BookingAssetQuery(
            status: selectedStatus,
            page: page,
            pageSize: Constants.pageSize
        )
    }

    private func resetForLoginGate() {
        hasLoaded = false
        isFirstPageLoading = false
        isAppending = false
        appendErrorMessage = nil
        requiresLogin = true
        currentPage = 0
        totalPages = 1
        currentItems = []
        summary = .empty
        viewState = .idle
        requestToken = UUID()
    }

    private func isUnauthorized(_ error: APIError) -> Bool {
        error.statusCode == 401
            || error.businessCode == "AUTH_UNAUTHORIZED"
            || error.businessCode == "UNAUTHORIZED"
    }

    private func mapFirstPageError(_ error: APIError) -> String {
        switch error.businessCode {
        case "VALIDATION_ERROR":
            return "加载失败，请重试"
        case "TOO_MANY_REQUESTS", "AUTH_RATE_LIMITED":
            return "操作过于频繁，请稍后再试"
        default:
            return error.errorDescription ?? "加载失败，请稍后重试"
        }
    }

    private func mapAppendError(_ error: APIError) -> String {
        switch error.businessCode {
        case "TOO_MANY_REQUESTS", "AUTH_RATE_LIMITED":
            return "操作过于频繁，请稍后再试"
        default:
            return error.errorDescription ?? "加载更多失败，请稍后重试"
        }
    }
}
