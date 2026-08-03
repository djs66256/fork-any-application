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
            rootView
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

    @ViewBuilder
    private var rootView: some View {
        if isPreviewPlayerCommentsSheet {
            CommentPreviewEntryView()
        } else if let scenario = RankingScreenshotScenario.fromEnvironment() {
            RankingScreenshotHostView(scenario: scenario)
        } else if let scenario = ClassificationScreenshotScenario.fromEnvironment() {
            ClassificationScreenshotHostView(scenario: scenario)
        } else {
            AppShellView()
        }
    }
}
