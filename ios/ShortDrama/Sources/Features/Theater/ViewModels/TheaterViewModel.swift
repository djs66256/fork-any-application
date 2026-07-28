import Foundation

/// ViewModel for the theater channel page.
@MainActor
final class TheaterViewModel: ObservableObject {

    enum ViewState: Equatable {
        case loading
        case content([TheaterDrama])
        case empty
        case error(String)
    }

    enum RouteEffect: Equatable {
        case navigate(AppRoute)
        case openRanking(TheaterRankingEntryContext)
        case showScanPlaceholder(String)
    }

    private enum Constants {
        static let firstPage = 1
        static let pageSize = 20
    }

    @Published private(set) var selectedChannel: TheaterChannel = .all
    @Published private(set) var viewState: ViewState = .loading
    @Published private(set) var isAppending = false
    @Published private(set) var appendErrorMessage: String?
    @Published private(set) var routeEffect: RouteEffect?

    private let fetchTheaterFeedUseCase: FetchTheaterFeedUseCase

    private var hasLoaded = false
    private var isFirstPageLoading = false
    private var currentPage = 0
    private var totalPages = 1
    private var currentItems: [TheaterDrama] = []
    private var requestToken = UUID()

    init(fetchTheaterFeedUseCase: FetchTheaterFeedUseCase) {
        self.fetchTheaterFeedUseCase = fetchTheaterFeedUseCase
    }

    var channels: [TheaterChannel] {
        TheaterChannel.allCases
    }

    var shortcuts: [TheaterShortcut] {
        TheaterShortcut.allCases
    }

    func loadIfNeeded() async {
        guard !hasLoaded else { return }
        hasLoaded = true
        await reloadFirstPage(for: selectedChannel)
    }

    func selectChannel(_ channel: TheaterChannel) async {
        guard channel != selectedChannel else { return }
        selectedChannel = channel
        await reloadFirstPage(for: channel)
    }

    func retry() async {
        await reloadFirstPage(for: selectedChannel)
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

        let nextPage = currentPage + 1
        let token = requestToken

        do {
            let response = try await fetchTheaterFeedUseCase.execute(
                query: makeQuery(channel: selectedChannel, page: nextPage)
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

    func openSearch() {
        routeEffect = .navigate(.searchHome)
    }

    func openScanPlaceholder() {
        routeEffect = .showScanPlaceholder("识图功能开发中")
    }

    func openShortcut(_ shortcut: TheaterShortcut) {
        switch shortcut {
        case .classification:
            routeEffect = .navigate(.classificationHome)
        case .ranking:
            routeEffect = .openRanking(TheaterRankingEntryContext(rankingType: .hot))
        case .newReleases:
            routeEffect = .navigate(.newReleases)
        case .booking:
            routeEffect = .openRanking(TheaterRankingEntryContext(rankingType: .booking))
        }
    }

    func openDrama(_ drama: TheaterDrama) {
        guard !drama.id.isEmpty else { return }
        routeEffect = .navigate(.player(videoId: drama.id))
    }

    func clearRouteEffect() {
        routeEffect = nil
    }

    private func reloadFirstPage(for channel: TheaterChannel) async {
        requestToken = UUID()
        let token = requestToken
        isFirstPageLoading = true
        isAppending = false
        appendErrorMessage = nil
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
            let response = try await fetchTheaterFeedUseCase.execute(
                query: makeQuery(channel: channel, page: Constants.firstPage)
            )

            guard token == requestToken else { return }

            currentPage = response.page
            totalPages = max(response.totalPages, response.page)
            currentItems = response.items
            viewState = response.items.isEmpty ? .empty : .content(response.items)
        } catch let error as APIError {
            guard token == requestToken else { return }
            viewState = .error(error.errorDescription ?? "剧场加载失败，请重试")
        } catch {
            guard token == requestToken else { return }
            viewState = .error(error.localizedDescription)
        }
    }

    private func makeQuery(channel: TheaterChannel, page: Int) -> TheaterFeedQuery {
        TheaterFeedQuery(channel: channel, page: page, pageSize: Constants.pageSize)
    }
}
