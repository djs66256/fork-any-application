import Foundation

@MainActor
final class MenuPanelViewModel: ObservableObject {
    enum RecentlyViewedState: Equatable {
        case idle
        case loading
        case content([RecentlyViewedItem])
        case empty
        case error(String)
    }

    @Published private(set) var viewState: RecentlyViewedState = .idle
    @Published private(set) var isRetrying = false

    private let fetchRecentlyViewedUseCase: FetchRecentlyViewedUseCase
    private let playbackSessionStore: PlaybackSessionStore

    private var hasLoadedRecentlyViewed = false
    private var inFlightTask: Task<Void, Never>?

    init(
        fetchRecentlyViewedUseCase: FetchRecentlyViewedUseCase,
        playbackSessionStore: PlaybackSessionStore
    ) {
        self.fetchRecentlyViewedUseCase = fetchRecentlyViewedUseCase
        self.playbackSessionStore = playbackSessionStore
    }

    deinit {
        inFlightTask?.cancel()
    }

    func loadIfNeeded() async {
        guard !hasLoadedRecentlyViewed, inFlightTask == nil else { return }
        await load(isRetry: false)
    }

    func retry() async {
        await load(isRetry: true)
    }

    private func load(isRetry: Bool) async {
        inFlightTask?.cancel()

        let shouldLoadRecentlyViewed = isRetry || !hasLoadedRecentlyViewed

        if shouldLoadRecentlyViewed {
            isRetrying = isRetry
            viewState = .loading
        }

        let task = Task { [weak self] in
            guard let self else { return }

            if shouldLoadRecentlyViewed {
                await self.loadRecentlyViewed()
            }

            await MainActor.run {
                self.isRetrying = false
                self.inFlightTask = nil
            }
        }

        inFlightTask = task
        await task.value
    }

    private func loadRecentlyViewed() async {
        do {
            let sessionId = try playbackSessionStore.getOrCreateSessionId()
            let items = try await fetchRecentlyViewedUseCase.execute(playbackSessionId: sessionId)
            guard !Task.isCancelled else { return }
            hasLoadedRecentlyViewed = true
            viewState = items.isEmpty ? .empty : .content(items)
        } catch let error as APIError {
            guard !Task.isCancelled else { return }
            viewState = .error(Self.errorMessage(from: error))
        } catch {
            guard !Task.isCancelled else { return }
            viewState = .error("加载失败，请稍后重试")
        }
    }

    func route(for item: RecentlyViewedItem) -> AppRoute? {
        guard item.hasValidDramaId else { return nil }
        return .player(videoId: item.dramaId)
    }

    private static func errorMessage(from error: APIError) -> String {
        switch error {
        case .network:
            return "网络异常，请检查后重试"
        case .server(let code, _):
            if code == 503 {
                return "服务暂不可用，请稍后重试"
            }
            return "加载失败，请稍后重试"
        default:
            return "加载失败，请稍后重试"
        }
    }
}
