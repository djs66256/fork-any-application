import Foundation

/// ViewModel for the ranking page.
@MainActor
final class RankingViewModel: ObservableObject {

    enum ViewState: Equatable {
        case loading
        case content([RankingDrama])
        case empty
        case error(String)
    }

    enum RouteEffect: Equatable {
        case requireLogin(RankingLoginContext)
    }

    private enum Constants {
        static let firstPage = 1
        static let pageSize = 10
    }

    @Published private(set) var selectedContentType: RankingContentType
    @Published private(set) var selectedRankingType: RankingType
    @Published private(set) var viewState: ViewState = .loading
    @Published private(set) var isAppending = false
    @Published private(set) var appendErrorMessage: String?
    @Published private(set) var bookingErrorMessage: String?
    @Published private(set) var routeEffect: RouteEffect?

    private let fetchRankingsUseCase: FetchRankingsUseCase
    private let bookDramaUseCase: BookDramaUseCase
    private let isUserLoggedIn: @Sendable () -> Bool

    private var hasLoaded = false
    private var isFirstPageLoading = false
    private var currentPage = 0
    private var totalPages = 1
    private var currentItems: [RankingDrama] = []
    private var requestToken = UUID()
    private var bookingInFlightIDs: Set<String> = []

    init(
        fetchRankingsUseCase: FetchRankingsUseCase,
        bookDramaUseCase: BookDramaUseCase,
        initialEntryContext: TheaterRankingEntryContext? = nil,
        isUserLoggedIn: @escaping @Sendable () -> Bool = { false }
    ) {
        self.fetchRankingsUseCase = fetchRankingsUseCase
        self.bookDramaUseCase = bookDramaUseCase
        self.isUserLoggedIn = isUserLoggedIn
        self.selectedContentType = initialEntryContext?.contentType ?? .all
        self.selectedRankingType = initialEntryContext?.rankingType ?? .hot
    }

    func loadIfNeeded() async {
        guard !hasLoaded else { return }
        hasLoaded = true
        await reloadFirstPage()
    }

    func selectContentType(_ type: RankingContentType) async {
        guard type != selectedContentType else { return }
        selectedContentType = type
        await reloadFirstPage()
    }

    func selectRankingType(_ type: RankingType) async {
        guard type != selectedRankingType else { return }
        selectedRankingType = type
        await reloadFirstPage()
    }

    func retry() async {
        await reloadFirstPage()
    }

    func loadMoreIfNeeded() async {
        guard !isAppending,
              !isFirstPageLoading,
              !currentItems.isEmpty,
              currentPage < totalPages else {
            return
        }

        isAppending = true
        appendErrorMessage = nil
        bookingErrorMessage = nil

        let nextPage = currentPage + 1
        let token = requestToken

        do {
            let response = try await fetchRankingsUseCase.execute(
                query: makeQuery(page: nextPage)
            )

            guard token == requestToken else { return }

            currentPage = response.page
            totalPages = max(response.totalPages, response.page)
            currentItems.append(contentsOf: response.items)
            viewState = .content(currentItems)
        } catch let error as APIError {
            guard token == requestToken else { return }
            appendErrorMessage = error.errorDescription ?? "加载更多失败，请稍后重试"
        } catch {
            guard token == requestToken else { return }
            appendErrorMessage = error.localizedDescription
        }

        if token == requestToken {
            isAppending = false
        }
    }

    func book(drama: RankingDrama) async {
        guard selectedRankingType == .booking,
              !drama.id.isEmpty,
              !drama.isBooked,
              !bookingInFlightIDs.contains(drama.id) else {
            return
        }

        guard isUserLoggedIn() else {
            bookingErrorMessage = "请先登录后再预约"
            routeEffect = .requireLogin(loginContext(for: drama.id))
            return
        }

        bookingInFlightIDs.insert(drama.id)
        bookingErrorMessage = nil
        updateDrama(id: drama.id) { $0.withSubmitting(true) }

        do {
            let result = try await bookDramaUseCase.execute(id: drama.id)
            bookingInFlightIDs.remove(drama.id)
            updateDrama(id: drama.id) {
                $0.withBookingState(
                    isBooked: result.booked,
                    bookingCount: result.bookingCount,
                    isSubmitting: false
                )
            }
        } catch let error as APIError {
            bookingInFlightIDs.remove(drama.id)
            updateDrama(id: drama.id) { $0.withSubmitting(false) }

            if case .server(let code, _) = error, code == 401 {
                bookingErrorMessage = "请先登录后再预约"
                routeEffect = .requireLogin(loginContext(for: drama.id))
            } else {
                bookingErrorMessage = error.errorDescription ?? "预约失败，请稍后重试"
            }
        } catch {
            bookingInFlightIDs.remove(drama.id)
            updateDrama(id: drama.id) { $0.withSubmitting(false) }
            bookingErrorMessage = error.localizedDescription
        }
    }

    func clearRouteEffect() {
        routeEffect = nil
    }

    private func reloadFirstPage() async {
        requestToken = UUID()
        let token = requestToken
        isFirstPageLoading = true
        isAppending = false
        appendErrorMessage = nil
        bookingErrorMessage = nil
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
            let response = try await fetchRankingsUseCase.execute(
                query: makeQuery(page: Constants.firstPage)
            )

            guard token == requestToken else { return }

            currentPage = response.page
            totalPages = max(response.totalPages, response.page)
            currentItems = response.items
            viewState = response.items.isEmpty ? .empty : .content(response.items)
        } catch let error as APIError {
            guard token == requestToken else { return }
            viewState = .error(error.errorDescription ?? "排行加载失败，请重试")
        } catch {
            guard token == requestToken else { return }
            viewState = .error(error.localizedDescription)
        }
    }

    private func makeQuery(page: Int) -> RankingQuery {
        RankingQuery(
            type: selectedRankingType,
            contentType: selectedContentType,
            page: page,
            pageSize: Constants.pageSize
        )
    }

    private func loginContext(for dramaID: String) -> RankingLoginContext {
        RankingLoginContext(
            source: "ranking",
            contentType: selectedContentType,
            rankingType: selectedRankingType,
            dramaID: dramaID
        )
    }

    private func updateDrama(id: String, transform: (RankingDrama) -> RankingDrama) {
        guard let index = currentItems.firstIndex(where: { $0.id == id }) else { return }
        currentItems[index] = transform(currentItems[index])
        viewState = .content(currentItems)
    }
}
