import SwiftUI

/// The main entry point for ShortDrama.
@main
struct ShortDramaApp: App {

    @StateObject private var router = NavigationRouter()

    var body: some Scene {
        WindowGroup {
            AppShellView()
                .environmentObject(router)
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
