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
                    case .searchHome:
                        SearchHomeView()
                    case .searchResult(let query):
                        SearchResultView(query: query)
                    case .rankingHome:
                        RankingHomeView(initialEntryContext: router.consumeTheaterRankingEntryContext())
                    case .classificationHome:
                        ClassificationHomeView()
                    case .newReleases:
                        DiscoveryPlaceholderView(kind: .newReleases)
                    case .actorHub:
                        DiscoveryPlaceholderView(kind: .actorHub)
                    case .player(let videoId):
                        PlayerView(viewModel: makePlayerViewModel(videoId: videoId))
                    case .dramaDetail(let dramaId):
                        DramaDetailView(viewModel: DramaDetailViewModel(dramaId: dramaId))
                    case .menuPlaceholder(let kind):
                        MenuPlaceholderView(kind: kind)
                    }
                }
        }
    }

    @ViewBuilder
    private var rootView: some View {
        switch tab {
        case .home:
            HomeView()
        case .theater:
            TheaterView()
        case .mall, .earn, .profile:
            PlaceholderTabView(tab: tab)
        }
    }

    private func makePlayerViewModel(videoId: String) -> PlayerViewModel {
        let repository = PlayerRepository()
        return PlayerViewModel(
            videoId: videoId,
            router: router,
            fetchPlayerProgressUseCase: FetchPlayerProgressUseCase(repository: repository),
            fetchDramaEpisodesUseCase: FetchDramaEpisodesUseCase(repository: repository),
            startPlaybackUseCase: StartPlaybackUseCase(repository: repository),
            stopPlaybackUseCase: StopPlaybackUseCase(repository: repository),
            playbackSessionStore: KeychainPlaybackSessionStore()
        )
    }
}
