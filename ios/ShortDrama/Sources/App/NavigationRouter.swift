import SwiftUI

/// Menu panel visibility state owned by the app shell.
enum MenuPanelPresentationState: Equatable {
    case closed
    case opening
    case open
    case closing
}

@MainActor
enum MallRestoreRequest: Equatable {
    case searchReturn
    case loginReturn(completed: Bool)
}

@MainActor
enum EarnRestoreRequest: Equatable {
    case loginReturn(completed: Bool)
    case taskReturn(EarnTaskPlayerResult)
}

/// Manages the navigation state for the app.
@MainActor
final class NavigationRouter: ObservableObject {

    @Published var selectedTab: AppTab = .home {
        didSet {
            guard oldValue != selectedTab else { return }
            handleSelectedTabChange()
        }
    }
    @Published private(set) var pathsByTab: [AppTab: NavigationPath] = AppTab.allCases.reduce(into: [:]) {
        $0[$1] = NavigationPath()
    }
    @Published private(set) var pendingRoute: AppRoute?
    @Published private(set) var containerReady = false
    @Published private(set) var menuPanelState: MenuPanelPresentationState = .closed
    @Published private(set) var pendingMenuNavigation: AppRoute?
    @Published private(set) var presentedLoginContext: LoginInterceptionContext?
    @Published private(set) var mallLoginContext: MallLoginContext?
    @Published private(set) var earnLoginContext: EarnLoginContext?
    @Published private(set) var activeSearchReturnTarget: String?
    @Published private(set) var activeSearchSourceTab: AppTab?
    @Published private(set) var pendingMallRestoreRequest: MallRestoreRequest?
    @Published private(set) var pendingEarnRestoreRequest: EarnRestoreRequest?
    @Published private(set) var pendingEarnTaskPlayerResult: EarnTaskPlayerResult?

    private var pendingTheaterRankingEntryContext: TheaterRankingEntryContext?

    var isMenuPanelVisible: Bool {
        switch menuPanelState {
        case .opening, .open, .closing:
            return true
        case .closed:
            return false
        }
    }

    var isPresentingSearchFromMall: Bool {
        activeSearchSourceTab == .mall && activeSearchReturnTarget == "/mall"
    }

    func pathBinding(for tab: AppTab) -> Binding<NavigationPath> {
        Binding(
            get: { self.pathsByTab[tab] ?? NavigationPath() },
            set: { self.pathsByTab[tab] = $0 }
        )
    }

    func select(tab: AppTab) {
        selectedTab = tab
    }

    func navigate(to route: AppRoute) {
        let tab = route.owningTab
        selectedTab = tab

        switch route {
        case .home:
            popToRoot(of: .home)
        case .searchHome,
             .searchResult,
             .rankingHome,
             .classificationHome,
             .newReleases,
             .actorHub,
             .player,
             .dramaDetail,
             .earnPlayer,
             .messages,
             .menuPlaceholder,
             .settings:
            var path = pathsByTab[tab] ?? NavigationPath()
            path.append(route)
            pathsByTab[tab] = path
        case .mallLogin(let context):
            mallLoginContext = context
        case .earnLogin(let context):
            earnLoginContext = context
        }
    }

    func openMenuPanel() {
        guard selectedTab == .home else { return }

        switch menuPanelState {
        case .closed:
            pendingMenuNavigation = nil
            menuPanelState = .opening
            menuPanelState = .open
        case .closing:
            pendingMenuNavigation = nil
            menuPanelState = .opening
            menuPanelState = .open
        case .opening, .open:
            break
        }
    }

    func closeMenuPanel() {
        pendingMenuNavigation = nil
        guard isMenuPanelVisible else {
            menuPanelState = .closed
            return
        }
        menuPanelState = .closing
    }

    func closeMenuPanelThenNavigate(to route: AppRoute) {
        guard pendingMenuNavigation == nil else { return }
        pendingMenuNavigation = route
        guard isMenuPanelVisible else {
            pendingMenuNavigation = nil
            navigate(to: route)
            return
        }
        menuPanelState = .closing
    }

    func markMenuPanelDidClose() {
        menuPanelState = .closed
        guard let route = pendingMenuNavigation else { return }
        pendingMenuNavigation = nil

        if case .menuPlaceholder(let kind) = route, kind == .login {
            let context = LoginInterceptionContext(
                source: .profileEntry,
                returnRoute: .profilePlaceholderReturnRoute
            )
            presentLogin(context: context)
            return
        }

        navigate(to: route)
    }

    func openRanking(from context: TheaterRankingEntryContext) {
        pendingTheaterRankingEntryContext = context
        navigate(to: .rankingHome)
    }

    func consumeTheaterRankingEntryContext() -> TheaterRankingEntryContext? {
        defer { pendingTheaterRankingEntryContext = nil }
        return pendingTheaterRankingEntryContext
    }

    func enqueueDeepLink(_ route: AppRoute) {
        pendingRoute = route
    }

    func markContainerReady() {
        guard !containerReady else { return }
        containerReady = true

        if let pendingRoute {
            self.pendingRoute = nil
            navigate(to: pendingRoute)
        }
    }

    func presentLogin(context: LoginInterceptionContext?) {
        presentedLoginContext = context ?? LoginInterceptionContext(source: .unknown)
    }

    func cancelLogin() {
        presentedLoginContext = nil
    }

    func completeLogin() {
        let context = presentedLoginContext
        presentedLoginContext = nil

        guard let returnRoute = context?.returnRoute else {
            select(tab: .profile)
            return
        }

        switch returnRoute {
        case .home:
            popToRoot(of: .home)
        case .mallLogin:
            select(tab: .mall)
        case .earnLogin,
             .earnPlayer:
            select(tab: .earn)
        case .messages:
            select(tab: .home)
        case .settings:
            select(tab: .profile)
            navigate(to: .settings)
        case .searchHome,
             .searchResult,
             .rankingHome,
             .classificationHome,
             .newReleases,
             .actorHub,
             .player,
             .dramaDetail,
             .menuPlaceholder:
            navigate(to: returnRoute)
        }
    }

    func dismiss(in tab: AppTab? = nil) {
        let targetTab = tab ?? selectedTab
        guard var path = pathsByTab[targetTab], !path.isEmpty else { return }
        path.removeLast()
        pathsByTab[targetTab] = path
    }

    func popToRoot(of tab: AppTab) {
        pathsByTab[tab] = NavigationPath()
        selectedTab = tab
    }

    func openSearchFromMall() {
        activeSearchSourceTab = .mall
        activeSearchReturnTarget = "/mall"
        navigate(to: .searchHome)
    }

    func restoreMallContextAfterSearch() {
        guard isPresentingSearchFromMall else { return }
        popToRoot(of: .home)
        selectedTab = .mall
        pendingMallRestoreRequest = .searchReturn
        activeSearchSourceTab = nil
        activeSearchReturnTarget = nil
    }

    func presentMallLogin(_ context: MallLoginContext) {
        navigate(to: .mallLogin(context: context))
    }

    func dismissMallLogin(completed: Bool) {
        mallLoginContext = nil
        selectedTab = .mall
        pendingMallRestoreRequest = .loginReturn(completed: completed)
    }

    func consumeMallRestoreRequest() -> MallRestoreRequest? {
        defer { pendingMallRestoreRequest = nil }
        return pendingMallRestoreRequest
    }

    func presentEarnLogin(_ context: EarnLoginContext) {
        navigate(to: .earnLogin(context: context))
    }

    func dismissEarnLogin(completed: Bool) {
        earnLoginContext = nil
        selectedTab = .earn
        pendingEarnRestoreRequest = .loginReturn(completed: completed)
    }

    func openPlayerFromEarn(_ context: EarnTaskContext) {
        navigate(to: .earnPlayer(context: context))
    }

    func finishEarnTaskPlayer(result: EarnTaskPlayerResult) {
        pendingEarnTaskPlayerResult = result
        selectedTab = .earn
        dismiss(in: .earn)
        pendingEarnRestoreRequest = .taskReturn(result)
    }

    func consumeEarnRestoreRequest() -> EarnRestoreRequest? {
        defer { pendingEarnRestoreRequest = nil }
        return pendingEarnRestoreRequest
    }

    func consumeEarnTaskPlayerResult() -> EarnTaskPlayerResult? {
        defer { pendingEarnTaskPlayerResult = nil }
        return pendingEarnTaskPlayerResult
    }

    private func handleSelectedTabChange() {
        guard selectedTab != .home else { return }
        pendingMenuNavigation = nil
        menuPanelState = .closed
    }
}

private extension AppRoute {
    static let profilePlaceholderReturnRoute = AppRoute.settings
}
