import Foundation

@MainActor
final class EarnContainerViewModel: ObservableObject {
    enum RouteEffect: Equatable {
        case requestLogin(EarnLoginContext)
        case openTaskPlayer(EarnTaskContext)
    }

    private enum Constants {
        static let initialLoadErrorMessage = "赚钱页加载失败，请稍后重试"
    }

    @Published private(set) var state: EarnContainerState = .loading
    @Published private(set) var currentRequest: URLRequest?
    @Published private(set) var currentURL: URL?
    @Published private(set) var lastLoadedHomeURL: URL?
    @Published private(set) var loadRevision = 0
    @Published private(set) var pendingLoginContext: EarnLoginContext?
    @Published private(set) var pendingTaskContext: EarnTaskContext?
    @Published private(set) var routeEffect: RouteEffect?
    @Published private(set) var hostMessage: EarnHostMessage?

    private let homeURLProvider: () -> URL?
    private var isUserLoggedIn: Bool
    private var authToken: String?
    private var authExpiry: String?
    private var hasLoaded = false

    init(
        homeURLProvider: @escaping () -> URL? = { AppConfig.earnHomeURL() },
        isUserLoggedIn: Bool = false,
        authToken: String? = nil,
        authExpiry: String? = nil
    ) {
        self.homeURLProvider = homeURLProvider
        self.isUserLoggedIn = isUserLoggedIn
        self.authToken = authToken
        self.authExpiry = authExpiry
    }

    func updateAuthSnapshot(isLoggedIn: Bool, authToken: String?, expiresAt: String?) {
        self.isUserLoggedIn = isLoggedIn
        self.authToken = authToken
        self.authExpiry = expiresAt
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
            if isEarnHomeURL(url) {
                lastLoadedHomeURL = url
            }
        }
        syncAuthState(reason: .initialLoad)
    }

    func handlePageLoadFailed(url: URL? = nil, message: String = Constants.initialLoadErrorMessage) {
        if let url {
            currentURL = url
        }
        state = .error(message: message)
    }

    func handleBridgeMessage(_ message: EarnBridgeMessage) {
        switch message {
        case .requestLogin(let context):
            guard pendingLoginContext == nil else { return }
            pendingLoginContext = context
            routeEffect = .requestLogin(context)
        case .openTaskPlayer(let context):
            guard pendingTaskContext == nil else { return }
            pendingTaskContext = context
            routeEffect = .openTaskPlayer(context)
        }
    }

    func clearRouteEffect() {
        routeEffect = nil
    }

    func clearHostMessage() {
        hostMessage = nil
    }

    func handleLoginCompletion() {
        pendingLoginContext = nil
        syncAuthState(reason: .loginCancel)
        restoreContext(reason: .loginReturn, preserveScroll: true)
    }

    func handleLoginSuccess() {
        pendingLoginContext = nil
        syncAuthState(reason: .loginSuccess)
        restoreContext(reason: .loginReturn, preserveScroll: true)
    }

    func handleTaskPlayerResult(_ result: EarnTaskPlayerResult) {
        pendingTaskContext = nil
        if result.completed {
            hostMessage = .completeTask(result)
        } else {
            restoreContext(reason: .taskReturn, preserveScroll: true)
        }
    }

    func handleTaskCompletionDispatchFinished() {
        restoreContext(reason: .taskReturn, preserveScroll: true)
    }

    func handleAppResumed() {
        syncAuthState(reason: .appResume)
    }

    func handleContainerRecreated() {
        reloadHome(resetState: true)
        pendingLoginContext = nil
        pendingTaskContext = nil
        restoreContext(reason: .containerRecreated, preserveScroll: false)
    }

    private func syncAuthState(reason: EarnHostAuthStateReason) {
        hostMessage = .syncAuthState(
            EarnHostAuthState(
                source: "earn",
                isLoggedIn: isUserLoggedIn,
                reason: reason,
                returnTarget: "/earn",
                apiAccessToken: authToken,
                expiresAt: authExpiry
            )
        )
    }

    private func restoreContext(reason: EarnRestoreContextReason, preserveScroll: Bool) {
        hostMessage = .restoreContext(
            EarnRestoreContextPayload(
                source: "earn",
                reason: reason,
                returnTarget: "/earn",
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

    private func isEarnHomeURL(_ url: URL) -> Bool {
        url.path == "/earn"
    }
}
