import SwiftUI

/// The main entry point for ShortDrama.
@main
struct ShortDramaApp: App {

    @StateObject private var router = NavigationRouter()

    var body: some Scene {
        WindowGroup {
            NavigationStack(path: $router.path) {
                HomeView()
                    .navigationDestination(for: AppRoute.self) { route in
                        switch route {
                        case .home:
                            HomeView()
                        case .player(let videoId):
                            PlayerView(viewModel: PlayerViewModel(videoId: videoId))
                        case .dramaDetail(let dramaId):
                            DramaDetailView(viewModel: DramaDetailViewModel(dramaId: dramaId))
                        }
                    }
            }
            .environmentObject(router)
            .onOpenURL { url in
                guard let route = DeeplinkHandler.handleDeepLink(url) else { return }
                router.navigate(to: route)
            }
        }
    }
}
