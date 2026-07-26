import Foundation
import Testing
@testable import ShortDrama

@MainActor
struct PlayerViewModelTests {
    private func makeEpisode(
        id: String,
        number: Int,
        playable: Bool = true
    ) -> Episode {
        Episode(
            id: id,
            dramaId: "drama-001",
            title: "第 \(number) 集",
            episodeNumber: number,
            videoUrl: playable ? "https://example.com/\(number).mp4" : "",
            duration: 180,
            thumbnailUrl: "https://example.com/\(number).jpg",
            description: "第 \(number) 集简介",
            createdAt: "2026-07-26T00:00:00Z",
            updatedAt: "2026-07-26T00:00:00Z"
        )
    }

    private func makeSUT(
        repository: MockPlayerRepository,
        sessionStore: MockPlaybackSessionStore = MockPlaybackSessionStore(),
        router: NavigationRouter = NavigationRouter()
    ) -> PlayerViewModel {
        PlayerViewModel(
            videoId: "drama-001",
            router: router,
            fetchPlayerProgressUseCase: FetchPlayerProgressUseCase(repository: repository),
            fetchDramaEpisodesUseCase: FetchDramaEpisodesUseCase(repository: repository),
            startPlaybackUseCase: StartPlaybackUseCase(repository: repository),
            stopPlaybackUseCase: StopPlaybackUseCase(repository: repository),
            playbackSessionStore: sessionStore
        )
    }

    @Test("T-06: loadIfNeeded bootstraps progress then episodes then start")
    func testLoadIfNeededBootstrapsInRequiredOrder() async {
        let repository = MockPlayerRepository()
        repository.progressResult = .success(
            PlayerProgress(dramaId: "drama-001", hasHistory: false, episodeId: nil, startTime: 0, updatedAt: nil)
        )
        repository.episodesResult = .success(
            EpisodeList(
                dramaId: "drama-001",
                seriesStatus: .completed,
                items: [makeEpisode(id: "episode-001", number: 1), makeEpisode(id: "episode-002", number: 2)]
            )
        )
        let sessionStore = MockPlaybackSessionStore()
        sessionStore.sessionId = "session-001"
        let viewModel = makeSUT(repository: repository, sessionStore: sessionStore)

        #expect(viewModel.dramaId == "drama-001")
        await viewModel.loadIfNeeded()

        #expect(repository.calls == [
            .fetchProgress(dramaId: "drama-001", playbackSessionId: "session-001"),
            .fetchEpisodes(dramaId: "drama-001"),
            .start(
                request: StartPlaybackRequest(dramaId: "drama-001", episodeId: "episode-001", progress: 0),
                playbackSessionId: "session-001"
            )
        ])
        #expect(viewModel.currentEpisode?.id == "episode-001")
        #expect(viewModel.currentProgress == 0)
        #expect(viewModel.uiState == .playing)
    }

    @Test("T-07: loadIfNeeded restores playable history episode")
    func testLoadIfNeededRestoresHistoryEpisode() async {
        let repository = MockPlayerRepository()
        repository.progressResult = .success(
            PlayerProgress(dramaId: "drama-001", hasHistory: true, episodeId: "episode-002", startTime: 88, updatedAt: nil)
        )
        repository.episodesResult = .success(
            EpisodeList(
                dramaId: "drama-001",
                seriesStatus: .completed,
                items: [makeEpisode(id: "episode-001", number: 1), makeEpisode(id: "episode-002", number: 2)]
            )
        )
        let sessionStore = MockPlaybackSessionStore()
        sessionStore.sessionId = "session-001"
        let viewModel = makeSUT(repository: repository, sessionStore: sessionStore)

        await viewModel.loadIfNeeded()

        #expect(viewModel.currentEpisode?.id == "episode-002")
        #expect(repository.calls.last == .start(
            request: StartPlaybackRequest(dramaId: "drama-001", episodeId: "episode-002", progress: 88),
            playbackSessionId: "session-001"
        ))
    }

    @Test("T-07: loadIfNeeded falls back when history episode is unavailable")
    func testLoadIfNeededFallsBackWhenHistoryEpisodeMissing() async {
        let repository = MockPlayerRepository()
        repository.progressResult = .success(
            PlayerProgress(dramaId: "drama-001", hasHistory: true, episodeId: "episode-404", startTime: 88, updatedAt: nil)
        )
        repository.episodesResult = .success(
            EpisodeList(
                dramaId: "drama-001",
                seriesStatus: .ongoing,
                items: [makeEpisode(id: "episode-001", number: 1), makeEpisode(id: "episode-002", number: 2, playable: false)]
            )
        )
        let sessionStore = MockPlaybackSessionStore()
        sessionStore.sessionId = "session-001"
        let viewModel = makeSUT(repository: repository, sessionStore: sessionStore)

        await viewModel.loadIfNeeded()

        #expect(viewModel.currentEpisode?.id == "episode-001")
        #expect(repository.calls.last == .start(
            request: StartPlaybackRequest(dramaId: "drama-001", episodeId: "episode-001", progress: 0),
            playbackSessionId: "session-001"
        ))
        #expect(viewModel.seriesStatus == .ongoing)
    }

    @Test("T-08: switchEpisode stops current episode before starting target episode and keeps speed")
    func testSwitchEpisodeStopsBeforeStartingAndPreservesSpeed() async {
        let repository = MockPlayerRepository()
        repository.progressResult = .success(
            PlayerProgress(dramaId: "drama-001", hasHistory: false, episodeId: nil, startTime: 0, updatedAt: nil)
        )
        let episode1 = makeEpisode(id: "episode-001", number: 1)
        let episode2 = makeEpisode(id: "episode-002", number: 2)
        repository.episodesResult = .success(
            EpisodeList(dramaId: "drama-001", seriesStatus: .completed, items: [episode1, episode2])
        )
        let sessionStore = MockPlaybackSessionStore()
        sessionStore.sessionId = "session-001"
        let viewModel = makeSUT(repository: repository, sessionStore: sessionStore)

        await viewModel.loadIfNeeded()
        viewModel.selectSpeed(.onePointFive)
        viewModel.updateCurrentProgress(42)
        await viewModel.switchEpisode(to: episode2)

        #expect(repository.calls == [
            .fetchProgress(dramaId: "drama-001", playbackSessionId: "session-001"),
            .fetchEpisodes(dramaId: "drama-001"),
            .start(request: StartPlaybackRequest(dramaId: "drama-001", episodeId: "episode-001", progress: 0), playbackSessionId: "session-001"),
            .stop(request: StopPlaybackRequest(dramaId: "drama-001", episodeId: "episode-001", progress: 42, duration: 180), playbackSessionId: "session-001"),
            .start(request: StartPlaybackRequest(dramaId: "drama-001", episodeId: "episode-002", progress: 0), playbackSessionId: "session-001")
        ])
        #expect(viewModel.currentEpisode?.id == "episode-002")
        #expect(viewModel.currentSpeed == .onePointFive)
        #expect(viewModel.playbackRate == 1.5)
    }

    @Test("T-09: handleBack triggers best effort stop and dismisses router")
    func testHandleBackStopsAndDismisses() async {
        let repository = MockPlayerRepository()
        repository.progressResult = .success(
            PlayerProgress(dramaId: "drama-001", hasHistory: false, episodeId: nil, startTime: 0, updatedAt: nil)
        )
        repository.episodesResult = .success(
            EpisodeList(dramaId: "drama-001", seriesStatus: .completed, items: [makeEpisode(id: "episode-001", number: 1)])
        )
        let sessionStore = MockPlaybackSessionStore()
        sessionStore.sessionId = "session-001"
        let router = NavigationRouter()
        router.navigate(to: .player(videoId: "drama-001"))
        let viewModel = makeSUT(repository: repository, sessionStore: sessionStore, router: router)

        await viewModel.loadIfNeeded()
        viewModel.updateCurrentProgress(75)
        viewModel.handleBack()
        await Task.yield()

        #expect(router.pathsByTab[.home]?.isEmpty == true)
        #expect(repository.calls.contains(.stop(
            request: StopPlaybackRequest(dramaId: "drama-001", episodeId: "episode-001", progress: 75, duration: 180),
            playbackSessionId: "session-001"
        )))
    }

    @Test("T-09: scene phase background pauses and reports stop")
    func testHandleScenePhaseBackground() async {
        let repository = MockPlayerRepository()
        repository.progressResult = .success(
            PlayerProgress(dramaId: "drama-001", hasHistory: false, episodeId: nil, startTime: 0, updatedAt: nil)
        )
        repository.episodesResult = .success(
            EpisodeList(dramaId: "drama-001", seriesStatus: .completed, items: [makeEpisode(id: "episode-001", number: 1)])
        )
        let sessionStore = MockPlaybackSessionStore()
        sessionStore.sessionId = "session-001"
        let viewModel = makeSUT(repository: repository, sessionStore: sessionStore)

        await viewModel.loadIfNeeded()
        viewModel.updateCurrentProgress(33)
        await viewModel.handleScenePhaseChange(.background)

        #expect(viewModel.uiState == .paused)
        #expect(repository.calls.contains(.stop(
            request: StopPlaybackRequest(dramaId: "drama-001", episodeId: "episode-001", progress: 33, duration: 180),
            playbackSessionId: "session-001"
        )))
    }

    @Test("T-09: disappear reports stop without dismissing router")
    func testHandleDisappearStopsPlaybackBestEffort() async {
        let repository = MockPlayerRepository()
        repository.progressResult = .success(
            PlayerProgress(dramaId: "drama-001", hasHistory: false, episodeId: nil, startTime: 0, updatedAt: nil)
        )
        repository.episodesResult = .success(
            EpisodeList(dramaId: "drama-001", seriesStatus: .completed, items: [makeEpisode(id: "episode-001", number: 1)])
        )
        let sessionStore = MockPlaybackSessionStore()
        sessionStore.sessionId = "session-001"
        let router = NavigationRouter()
        router.navigate(to: .player(videoId: "drama-001"))
        let viewModel = makeSUT(repository: repository, sessionStore: sessionStore, router: router)

        await viewModel.loadIfNeeded()
        viewModel.updateCurrentProgress(27)
        viewModel.handleDisappear()
        await Task.yield()

        #expect(router.pathsByTab[.home]?.count == 1)
        #expect(repository.calls.contains(.stop(
            request: StopPlaybackRequest(dramaId: "drama-001", episodeId: "episode-001", progress: 27, duration: 180),
            playbackSessionId: "session-001"
        )))
    }

    @Test("T-10: bootstrap enters no resource when all episodes are unplayable")
    func testLoadIfNeededNoResourceWhenNoPlayableEpisodes() async {
        let repository = MockPlayerRepository()
        repository.progressResult = .success(
            PlayerProgress(dramaId: "drama-001", hasHistory: false, episodeId: nil, startTime: 0, updatedAt: nil)
        )
        repository.episodesResult = .success(
            EpisodeList(
                dramaId: "drama-001",
                seriesStatus: .completed,
                items: [makeEpisode(id: "episode-001", number: 1, playable: false)]
            )
        )
        let viewModel = makeSUT(repository: repository)

        await viewModel.loadIfNeeded()

        #expect(viewModel.uiState == .noResource)
    }
}
