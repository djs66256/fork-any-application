import SwiftUI

struct TabNavigationHostView: View {
    let tab: AppTab

    @EnvironmentObject private var router: NavigationRouter

    var body: some View {
        NavigationStack(path: router.pathBinding(for: tab)) {
            rootView
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
    }

    @ViewBuilder
    private var rootView: some View {
        switch tab {
        case .home:
            HomeView()
        case .theater, .mall, .earn, .profile:
            PlaceholderTabView(tab: tab)
        }
    }
}
