import SwiftUI

/// Menu panel visibility state owned by the app shell.
enum MenuPanelPresentationState: Equatable {
    case closed
    case opening
    case open
    case closing
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

    var isMenuPanelVisible: Bool {
        switch menuPanelState {
        case .opening, .open, .closing:
            return true
        case .closed:
            return false
        }
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
             .menuPlaceholder:
            var path = pathsByTab[tab] ?? NavigationPath()
            path.append(route)
            pathsByTab[tab] = path
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
        navigate(to: route)
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

    private func handleSelectedTabChange() {
        guard selectedTab != .home else { return }
        pendingMenuNavigation = nil
        menuPanelState = .closed
    }
}
