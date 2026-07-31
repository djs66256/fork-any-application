import SwiftUI

/// The main entry point for ShortDrama.
@main
struct ShortDramaApp: App {

    @StateObject private var router = NavigationRouter()
    @StateObject private var authStore = AuthStore()

    private let isPreviewPlayerCommentsSheet =
        ProcessInfo.processInfo.arguments.contains("--preview-player-comments-sheet")

    var body: some Scene {
        WindowGroup {
            Group {
                if isPreviewPlayerCommentsSheet {
                    CommentPreviewEntryView()
                } else {
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
    }
}
