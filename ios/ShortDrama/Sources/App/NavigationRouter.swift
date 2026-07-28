import SwiftUI

/// Manages the navigation state for the app.
@MainActor
final class NavigationRouter: ObservableObject {

    @Published var selectedTab: AppTab = .home
    @Published private(set) var pathsByTab: [AppTab: NavigationPath] = AppTab.allCases.reduce(into: [:]) {
        $0[$1] = NavigationPath()
    }
    @Published private(set) var pendingRoute: AppRoute?
    @Published private(set) var containerReady = false
    @Published private(set) var presentedLoginContext: LoginInterceptionContext?

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
             .settings:
            var path = pathsByTab[tab] ?? NavigationPath()
            path.append(route)
            pathsByTab[tab] = path
        }
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
             .dramaDetail:
            select(tab: returnRoute.owningTab)
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
}
