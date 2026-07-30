import SwiftUI

struct TabNavigationHostView: View {
    let tab: AppTab

    @EnvironmentObject private var router: NavigationRouter
    @EnvironmentObject private var authStore: AuthStore

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
                        RankingHomeView(
                            initialEntryContext: router.consumeTheaterRankingEntryContext(),
                            isUserLoggedIn: { authStore.isAuthenticated }
                        )
                    case .bookingAssets:
                        BookingAssetsView()
                    case .classificationHome:
                        ClassificationHomeView()
                    case .newReleases:
                        DiscoveryPlaceholderView(kind: .newReleases)
                    case .actorHub:
                        DiscoveryPlaceholderView(kind: .actorHub)
                    case .player(let videoId):
                        PlayerView(viewModel: makePlayerViewModel(videoId: videoId))
                    case .earnPlayer(let context):
                        PlayerView(viewModel: makePlayerViewModel(videoId: context.videoId, earnTaskContext: context))
                    case .dramaDetail(let dramaId):
                        DramaDetailView(viewModel: DramaDetailViewModel(dramaId: dramaId))
                    case .mallLogin,
                         .earnLogin:
                        EmptyView()
                    case .messages:
                        MessageCenterView(viewModel: makeMessageCenterViewModel())
                    case .menuPlaceholder(let kind):
                        MenuPlaceholderView(kind: kind)
                    case .settings:
                        SettingsView(logoutAction: {
                            try await authStore.logout()
                        })
                    }
                }
        }
    }

    @ViewBuilder
    private var rootView: some View {
        switch tab {
        case .home:
            HomeView(viewModel: makeHomeViewModel())
        case .theater:
            TheaterView()
        case .mall:
            MallContainerView()
        case .earn:
            EarnContainerView()
        case .profile:
            ProfileHomeView()
        }
    }

    private func makeHomeViewModel() -> HomeViewModel {
        let repository: DramaRepositoryProtocol = DramaRepository()
        return HomeViewModel(
            fetchDramasUseCase: FetchDramasUseCase(repository: repository),
            isUserLoggedIn: { authStore.isAuthenticated },
            accessTokenProvider: { authStore.status.currentSession?.accessToken }
        )
    }

    private func makePlayerViewModel(
        videoId: String,
        earnTaskContext: EarnTaskContext? = nil
    ) -> PlayerViewModel {
        let repository = PlayerRepository()
        return PlayerViewModel(
            videoId: videoId,
            router: router,
            earnTaskContext: earnTaskContext,
            fetchPlayerProgressUseCase: FetchPlayerProgressUseCase(repository: repository),
            fetchDramaEpisodesUseCase: FetchDramaEpisodesUseCase(repository: repository),
            startPlaybackUseCase: StartPlaybackUseCase(repository: repository),
            stopPlaybackUseCase: StopPlaybackUseCase(repository: repository),
            playbackSessionStore: KeychainPlaybackSessionStore(),
            isUserLoggedIn: { authStore.isAuthenticated }
        )
    }

    private func makeMessageCenterViewModel() -> MessageCenterViewModel {
        let repository = MessageRepository()
        return MessageCenterViewModel(
            fetchSystemMessagesUseCase: FetchSystemMessagesUseCase(repository: repository),
            fetchInteractionMessagesUseCase: FetchInteractionMessagesUseCase(repository: repository),
            authTokenProvider: {
                authStore.status.currentSession?.accessToken
            }
        )
    }
}
