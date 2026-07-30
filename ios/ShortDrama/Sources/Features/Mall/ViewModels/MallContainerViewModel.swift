import Foundation

@MainActor
final class MallContainerViewModel: ObservableObject {
    enum RouteEffect: Equatable {
        case openSearch(MallSearchContext)
        case requestLogin(MallLoginContext)
    }

    private enum Constants {
        static let initialLoadErrorMessage = "商城加载失败，请稍后重试"
    }

    @Published private(set) var state: MallContainerState = .loading
    @Published private(set) var currentRequest: URLRequest?
    @Published private(set) var currentURL: URL?
    @Published private(set) var lastLoadedHomeURL: URL?
    @Published private(set) var loadRevision = 0
    @Published private(set) var pendingLoginContext: MallLoginContext?
    @Published private(set) var routeEffect: RouteEffect?
    @Published private(set) var hostMessage: MallHostMessage?

    private let homeURLProvider: () -> URL?
    private var isUserLoggedIn: Bool
    private var hasLoaded = false

    init(
        homeURLProvider: @escaping () -> URL? = { AppConfig.mallHomeURL() },
        isUserLoggedIn: Bool = false
    ) {
        self.homeURLProvider = homeURLProvider
        self.isUserLoggedIn = isUserLoggedIn
    }

    func updateAuthSnapshot(isLoggedIn: Bool) {
        isUserLoggedIn = isLoggedIn
    }

    func loadInitialPage() {
        guard !hasLoaded else { return }
        hasLoaded = true
        reloadHome(resetState: true)
    }

    func reload() {
        if let lastLoadedHomeURL {
            load(url: lastLoadedHomeURL, resetState: true)
            return
        }
        reloadHome(resetState: true)
    }

    func handlePageLoaded(url: URL?) {
        state = .success
        if let url {
            currentURL = url
            if isMallHomeURL(url) {
                lastLoadedHomeURL = url
            }
        }
        syncAuthState(reason: "initial-load")
    }

    func handlePageLoadFailed(url: URL? = nil, message: String = Constants.initialLoadErrorMessage) {
        if let url {
            currentURL = url
        }
        state = .error(message: message)
    }

    func handleBridgeMessage(_ message: MallBridgeMessage) {
        switch message {
        case .openSearch(let context):
            routeEffect = .openSearch(context)
        case .requestLogin(let context):
            guard pendingLoginContext == nil else { return }
            pendingLoginContext = context
            routeEffect = .requestLogin(context)
        }
    }

    func clearRouteEffect() {
        routeEffect = nil
    }

    func clearHostMessage() {
        hostMessage = nil
    }

    func handleSearchReturn() {
        restoreContext(reason: .searchReturn, preserveScroll: true)
    }

    func handleLoginCompletion() {
        pendingLoginContext = nil
        syncAuthState(reason: "login-cancel")
        restoreContext(reason: .loginReturn, preserveScroll: true)
    }

    func handleLoginSuccess() {
        pendingLoginContext = nil
        syncAuthState(reason: "login-success")
        restoreContext(reason: .loginReturn, preserveScroll: true)
    }

    func handleAppResumed() {
        syncAuthState(reason: "app-resume")
    }

    func handleContainerRecreated() {
        reloadHome(resetState: true)
        pendingLoginContext = nil
        restoreContext(reason: .containerRecreated, preserveScroll: false)
    }

    private func syncAuthState(reason: String) {
        hostMessage = .syncAuthState(
            MallHostAuthState(
                source: "mall",
                isLoggedIn: isUserLoggedIn,
                reason: reason,
                returnTarget: "/mall"
            )
        )
    }

    private func restoreContext(reason: MallRestoreContextReason, preserveScroll: Bool) {
        hostMessage = .restoreContext(
            MallRestoreContextPayload(
                source: "mall",
                reason: reason,
                returnTarget: "/mall",
                preserveScroll: preserveScroll
            )
        )
    }

    private func reloadHome(resetState: Bool) {
        guard let homeURL = homeURLProvider() else {
            state = .error(message: Constants.initialLoadErrorMessage)
            currentRequest = nil
            currentURL = nil
            return
        }

        lastLoadedHomeURL = homeURL
        load(url: homeURL, resetState: resetState)
    }

    private func load(url: URL, resetState: Bool) {
        currentURL = url
        currentRequest = URLRequest(url: url)
        loadRevision += 1
        if resetState {
            state = .loading
        }
    }

    private func isMallHomeURL(_ url: URL) -> Bool {
        url.path == "/mall"
    }
}
