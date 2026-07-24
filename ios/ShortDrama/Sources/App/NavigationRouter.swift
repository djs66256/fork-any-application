import SwiftUI

/// Manages the navigation stack for the app.
@MainActor
final class NavigationRouter: ObservableObject {

    /// The shared navigation path used by NavigationStack.
    @Published var path = NavigationPath()

    /// Navigates to the specified route by pushing it onto the stack.
    func navigate(to route: AppRoute) {
        path.append(route)
    }

    /// Dismisses the topmost view in the navigation stack.
    func dismiss() {
        guard !path.isEmpty else { return }
        path.removeLast()
    }

    /// Pops back to the root of the navigation stack.
    func popToRoot() {
        path.removeLast(path.count)
    }
}
