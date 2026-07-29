import Foundation
@testable import ShortDrama
import Testing

@MainActor
struct PlayerViewModelTestSupport {
    func makeEpisode(
        id: String,
        number: Int,
        playable: Bool = true,
        duration: Int = 180
    ) -> Episode {
        Episode(
            id: id,
            dramaId: "drama-001",
            title: "第 \(number) 集",
            episodeNumber: number,
            videoUrl: playable ? "https://example.com/\(number).mp4" : "",
            duration: duration,
            thumbnailUrl: "https://example.com/\(number).jpg",
            description: "第 \(number) 集简介",
            createdAt: "2026-07-26T00:00:00Z",
            updatedAt: "2026-07-26T00:00:00Z"
        )
    }

    func makeSUT(
        repository: MockPlayerRepository,
        sessionStore: MockPlaybackSessionStore = MockPlaybackSessionStore(),
        router: NavigationRouter = NavigationRouter(),
        earnTaskContext: EarnTaskContext? = nil
    ) -> PlayerViewModel {
        PlayerViewModel(
            videoId: "drama-001",
            router: router,
            earnTaskContext: earnTaskContext,
            fetchPlayerProgressUseCase: FetchPlayerProgressUseCase(repository: repository),
            fetchDramaEpisodesUseCase: FetchDramaEpisodesUseCase(repository: repository),
            startPlaybackUseCase: StartPlaybackUseCase(repository: repository),
            stopPlaybackUseCase: StopPlaybackUseCase(repository: repository),
            playbackSessionStore: sessionStore
        )
    }
}

@MainActor
struct PlayerViewModelTests {}

@MainActor
extension PlayerViewModelTests {
    private var support: PlayerViewModelTestSupport { PlayerViewModelTestSupport() }

    @Test("T-06: loadIfNeeded bootstraps progress then episodes then start")
    func testLoadIfNeededBootstrapsInRequiredOrder() async {
        let repository = MockPlayerRepository()
        repository.progressResult = .success(makeProgress())
        repository.episodesResult = .success(
            makeEpisodeList(items: [
                support.makeEpisode(id: "episode-001", number: 1),
                support.makeEpisode(id: "episode-002", number: 2)
            ])
        )
        let viewModel = support.makeSUT(
            repository: repository,
            sessionStore: makeSessionStore()
        )

        #expect(viewModel.dramaId == "drama-001")
        await viewModel.loadIfNeeded()

        #expect(repository.calls == [
            .fetchProgress(dramaId: "drama-001", playbackSessionId: "session-001"),
            .fetchEpisodes(dramaId: "drama-001"),
            .start(
                request: StartPlaybackRequest(
                    dramaId: "drama-001",
                    episodeId: "episode-001",
                    progress: 0
                ),
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
            makeProgress(hasHistory: true, episodeId: "episode-002", startTime: 88)
        )
        repository.episodesResult = .success(
            makeEpisodeList(items: [
                support.makeEpisode(id: "episode-001", number: 1),
                support.makeEpisode(id: "episode-002", number: 2)
            ])
        )
        let viewModel = support.makeSUT(
            repository: repository,
            sessionStore: makeSessionStore()
        )

        await viewModel.loadIfNeeded()

        #expect(viewModel.currentEpisode?.id == "episode-002")
        #expect(repository.calls.last == .start(
            request: StartPlaybackRequest(
                dramaId: "drama-001",
                episodeId: "episode-002",
                progress: 88
            ),
            playbackSessionId: "session-001"
        ))
    }

    @Test("T-07: loadIfNeeded falls back when history episode is unavailable")
    func testLoadIfNeededFallsBackWhenHistoryEpisodeMissing() async {
        let repository = MockPlayerRepository()
        repository.progressResult = .success(
            makeProgress(hasHistory: true, episodeId: "episode-404", startTime: 88)
        )
        repository.episodesResult = .success(
            makeEpisodeList(
                seriesStatus: .ongoing,
                items: [
                    support.makeEpisode(id: "episode-001", number: 1),
                    support.makeEpisode(id: "episode-002", number: 2, playable: false)
                ]
            )
        )
        let viewModel = support.makeSUT(
            repository: repository,
            sessionStore: makeSessionStore()
        )

        await viewModel.loadIfNeeded()

        #expect(viewModel.currentEpisode?.id == "episode-001")
        #expect(repository.calls.last == .start(
            request: StartPlaybackRequest(
                dramaId: "drama-001",
                episodeId: "episode-001",
                progress: 0
            ),
            playbackSessionId: "session-001"
        ))
        #expect(viewModel.seriesStatus == .ongoing)
    }

    @Test("T-10: bootstrap enters no resource when all episodes are unplayable")
    func testLoadIfNeededNoResourceWhenNoPlayableEpisodes() async {
        let repository = MockPlayerRepository()
        repository.progressResult = .success(makeProgress())
        repository.episodesResult = .success(
            makeEpisodeList(
                items: [support.makeEpisode(id: "episode-001", number: 1, playable: false)]
            )
        )
        let viewModel = support.makeSUT(repository: repository)

        await viewModel.loadIfNeeded()

        #expect(viewModel.uiState == .noResource)
    }
}

@MainActor
extension PlayerViewModelTests {

    @Test("T-08: switchEpisode stops current episode before starting target episode and keeps speed")
    func testSwitchEpisodeStopsBeforeStartingAndPreservesSpeed() async {
        let repository = MockPlayerRepository()
        let episode1 = support.makeEpisode(id: "episode-001", number: 1)
        let episode2 = support.makeEpisode(id: "episode-002", number: 2)
        repository.progressResult = .success(makeProgress())
        repository.episodesResult = .success(makeEpisodeList(items: [episode1, episode2]))
        let viewModel = support.makeSUT(
            repository: repository,
            sessionStore: makeSessionStore()
        )

        await viewModel.loadIfNeeded()
        viewModel.selectSpeed(.onePointFive)
        viewModel.updateCurrentProgress(42)
        await viewModel.switchEpisode(to: episode2)

        #expect(repository.calls == [
            .fetchProgress(dramaId: "drama-001", playbackSessionId: "session-001"),
            .fetchEpisodes(dramaId: "drama-001"),
            .start(
                request: StartPlaybackRequest(
                    dramaId: "drama-001",
                    episodeId: "episode-001",
                    progress: 0
                ),
                playbackSessionId: "session-001"
            ),
            .stop(
                request: StopPlaybackRequest(
                    dramaId: "drama-001",
                    episodeId: "episode-001",
                    progress: 42,
                    duration: 180
                ),
                playbackSessionId: "session-001"
            ),
            .start(
                request: StartPlaybackRequest(
                    dramaId: "drama-001",
                    episodeId: "episode-002",
                    progress: 0
                ),
                playbackSessionId: "session-001"
            )
        ])
        #expect(viewModel.currentEpisode?.id == "episode-002")
        #expect(viewModel.currentSpeed == .onePointFive)
        #expect(viewModel.playbackRate == 1.5)
    }

    @Test("T-09: handleBack triggers best effort stop and dismisses router")
    func testHandleBackStopsAndDismisses() async {
        let repository = makeRepositoryForSinglePlayableEpisode(support: support)
        let router = NavigationRouter()
        router.navigate(to: .player(videoId: "drama-001"))
        let viewModel = support.makeSUT(
            repository: repository,
            sessionStore: makeSessionStore(),
            router: router
        )

        await viewModel.loadIfNeeded()
        viewModel.updateCurrentProgress(75)
        viewModel.handleBack()
        try? await Task.sleep(for: .milliseconds(50))

        #expect(router.pathsByTab[.home]?.isEmpty == true)
        #expect(repository.calls.contains(makeStopCall(progress: 75)))
    }

    @Test("T-09: scene phase background pauses and reports stop")
    func testHandleScenePhaseBackground() async {
        let repository = makeRepositoryForSinglePlayableEpisode(support: support)
        let viewModel = support.makeSUT(
            repository: repository,
            sessionStore: makeSessionStore()
        )

        await viewModel.loadIfNeeded()
        viewModel.updateCurrentProgress(33)
        await viewModel.handleScenePhaseChange(.background)

        #expect(viewModel.uiState == .paused)
        #expect(repository.calls.contains(makeStopCall(progress: 33)))
    }

    @Test("T-09: disappear reports stop without dismissing router")
    func testHandleDisappearStopsPlaybackBestEffort() async {
        let repository = makeRepositoryForSinglePlayableEpisode(support: support)
        let router = NavigationRouter()
        router.navigate(to: .player(videoId: "drama-001"))
        let viewModel = support.makeSUT(
            repository: repository,
            sessionStore: makeSessionStore(),
            router: router
        )

        await viewModel.loadIfNeeded()
        viewModel.updateCurrentProgress(27)
        viewModel.handleDisappear()
        try? await Task.sleep(for: .milliseconds(50))

        #expect(router.pathsByTab[.home]?.count == 1)
        #expect(repository.calls.contains(makeStopCall(progress: 27)))
    }

    private func makeRepositoryForSinglePlayableEpisode(
        support: PlayerViewModelTestSupport
    ) -> MockPlayerRepository {
        let repository = MockPlayerRepository()
        repository.progressResult = .success(makeProgress())
        repository.episodesResult = .success(
            makeEpisodeList(items: [support.makeEpisode(id: "episode-001", number: 1)])
        )
        return repository
    }

    private func makeStopCall(progress: Double) -> MockPlayerRepository.Call {
        .stop(
            request: StopPlaybackRequest(
                dramaId: "drama-001",
                episodeId: "episode-001",
                progress: progress,
                duration: 180
            ),
            playbackSessionId: "session-001"
        )
    }
}

@MainActor
extension PlayerViewModelTests {

    @Test("T-04: earn playback ended reports completed result once")
    func testEarnPlaybackEndedReportsCompletedResult() async {
        let repository = makeEarnRepository(duration: 120)
        let router = NavigationRouter()
        let earnTaskContext = makeEarnTaskContext()
        router.openPlayerFromEarn(earnTaskContext)
        let viewModel = support.makeSUT(
            repository: repository,
            sessionStore: makeSessionStore(),
            router: router,
            earnTaskContext: earnTaskContext
        )

        await viewModel.loadIfNeeded()
        viewModel.handlePlaybackEnded()
        viewModel.handlePlaybackEnded()
        await Task.yield()

        let result = router.consumeEarnTaskPlayerResult()
        #expect(result == makeEarnTaskPlayerResult(
            completed: true,
            reason: .playbackEnded
        ))
        #expect(router.consumeEarnTaskPlayerResult() == nil)
    }

    @Test("T-04: earn back action reports incomplete result with userExit")
    func testEarnBackReportsIncompleteResult() async {
        let repository = makeEarnRepository()
        let router = NavigationRouter()
        let earnTaskContext = makeEarnTaskContext()
        router.openPlayerFromEarn(earnTaskContext)
        let viewModel = support.makeSUT(
            repository: repository,
            sessionStore: makeSessionStore(),
            router: router,
            earnTaskContext: earnTaskContext
        )

        await viewModel.loadIfNeeded()
        viewModel.handleBack()
        await Task.yield()
        await Task.yield()

        #expect(router.consumeEarnTaskPlayerResult() == makeEarnTaskPlayerResult(
            completed: false,
            reason: .userExit
        ))
    }

    @Test("T-04: earn backgrounding reports incomplete result with backgrounded")
    func testEarnBackgroundingReportsIncompleteResult() async {
        let repository = makeEarnRepository()
        let router = NavigationRouter()
        let earnTaskContext = makeEarnTaskContext()
        router.openPlayerFromEarn(earnTaskContext)
        let viewModel = support.makeSUT(
            repository: repository,
            sessionStore: makeSessionStore(),
            router: router,
            earnTaskContext: earnTaskContext
        )

        await viewModel.loadIfNeeded()
        await viewModel.handleScenePhaseChange(.background)

        #expect(router.consumeEarnTaskPlayerResult() == makeEarnTaskPlayerResult(
            completed: false,
            reason: .backgrounded
        ))
    }

    private func makeEarnRepository(duration: Int = 180) -> MockPlayerRepository {
        let repository = MockPlayerRepository()
        repository.progressResult = .success(makeProgress())
        repository.episodesResult = .success(
            makeEpisodeList(
                items: [support.makeEpisode(id: "episode-001", number: 1, duration: duration)]
            )
        )
        return repository
    }
}
