import SwiftUI

/// The main entry point for ShortDrama.
@main
struct ShortDramaApp: App {

    @StateObject private var router = NavigationRouter()
    @StateObject private var authStore = AuthStore()

    var body: some Scene {
        WindowGroup {
            AppShellView()
                .environmentObject(router)
                .environmentObject(authStore)
                .onOpenURL { url in
                    guard let route = DeeplinkHandler.handleDeepLink(url) else { return }
                    if router.containerReady {
                        router.navigate(to: route)
                    } else {
                        router.enqueueDeepLink(route)
                    }
                }
        }
    }
}
