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

    enum MessagePreviewState: Equatable {
        case idle
        case loading
        case content(MessagePreview)
        case empty
        case error(String)
    }

    @Published private(set) var viewState: RecentlyViewedState = .idle
    @Published private(set) var messagePreviewState: MessagePreviewState = .idle
    @Published private(set) var isRetrying = false

    private let fetchRecentlyViewedUseCase: FetchRecentlyViewedUseCase
    private let fetchMessagePreviewUseCase: FetchMessagePreviewUseCase
    private let playbackSessionStore: PlaybackSessionStore

    private var hasLoadedRecentlyViewed = false
    private var hasLoadedMessagePreview = false
    private var inFlightTask: Task<Void, Never>?

    init(
        fetchRecentlyViewedUseCase: FetchRecentlyViewedUseCase,
        fetchMessagePreviewUseCase: FetchMessagePreviewUseCase,
        playbackSessionStore: PlaybackSessionStore
    ) {
        self.fetchRecentlyViewedUseCase = fetchRecentlyViewedUseCase
        self.fetchMessagePreviewUseCase = fetchMessagePreviewUseCase
        self.playbackSessionStore = playbackSessionStore
    }

    deinit {
        inFlightTask?.cancel()
    }

    func loadIfNeeded() async {
        guard (!hasLoadedRecentlyViewed || !hasLoadedMessagePreview), inFlightTask == nil else { return }
        await load(isRetry: false)
    }

    func retry() async {
        await load(isRetry: true)
    }

    func route(for item: RecentlyViewedItem) -> AppRoute? {
        guard item.hasValidDramaId else { return nil }
        return .player(videoId: item.dramaId)
    }

    private func load(isRetry: Bool) async {
        inFlightTask?.cancel()

        let shouldLoadRecentlyViewed = isRetry || !hasLoadedRecentlyViewed
        let shouldLoadPreview = isRetry || !hasLoadedMessagePreview

        if shouldLoadRecentlyViewed {
            isRetrying = isRetry
            viewState = .loading
        }

        if shouldLoadPreview {
            messagePreviewState = .loading
        }

        let task = Task { [weak self] in
            guard let self else { return }

            if shouldLoadPreview {
                await self.loadMessagePreview()
            }

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

    private func loadMessagePreview() async {
        do {
            let preview = try await fetchMessagePreviewUseCase.execute()
            guard !Task.isCancelled else { return }
            hasLoadedMessagePreview = true
            messagePreviewState = preview.map(MessagePreviewState.content) ?? .empty
        } catch let error as APIError {
            guard !Task.isCancelled else { return }
            messagePreviewState = .error(Self.messagePreviewErrorFallback(from: error))
        } catch {
            guard !Task.isCancelled else { return }
            messagePreviewState = .error("加载失败，请稍后重试")
        }
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
            viewState = .error(Self.recentlyViewedErrorMessage(from: error))
        } catch {
            guard !Task.isCancelled else { return }
            viewState = .error("加载失败，请稍后重试")
        }
    }

    private static func recentlyViewedErrorMessage(from error: APIError) -> String {
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

    private static func messagePreviewErrorFallback(from error: APIError) -> String {
        switch error {
        case .server(let code, _):
            if code == 503 {
                return "暂无消息"
            }
            return "精选续播与新内容提醒即将接入，后续会在这里展示你的最新消息。"
        case .network:
            return "精选续播与新内容提醒即将接入，后续会在这里展示你的最新消息。"
        default:
            return "精选续播与新内容提醒即将接入，后续会在这里展示你的最新消息。"
        }
    }
}
