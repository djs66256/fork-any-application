import Foundation
@testable import ShortDrama
import Testing

@MainActor
struct HomeFeedPlaybackCoordinatorTests {
    @Test("Home feed coordinator only boots the active drama and only exposes its playback URL")
    func testOnlyActiveDramaExposesPlayback() async {
        let firstRepository = makeRepository(id: "drama-001")
        let secondRepository = makeRepository(id: "drama-002")
        let coordinator = HomeFeedPlaybackCoordinator { drama in
            switch drama.id {
            case "drama-001":
                self.makePlayerViewModel(id: drama.id, repository: firstRepository)
            case "drama-002":
                self.makePlayerViewModel(id: drama.id, repository: secondRepository)
            default:
                Issue.record("Unexpected drama id: \(drama.id)")
                return self.makePlayerViewModel(id: drama.id, repository: MockPlayerRepository())
            }
        }

        await coordinator.configure(with: [makeDrama(id: "drama-001"), makeDrama(id: "drama-002")])
        await coordinator.setActiveDrama(id: "drama-001")

        let firstFetchProgress = MockPlayerRepository.Call.fetchProgress(
            dramaId: "drama-001",
            playbackSessionId: "session-drama-001"
        )
        #expect(firstRepository.calls.contains(firstFetchProgress))
        #expect(firstRepository.calls.contains(.fetchEpisodes(dramaId: "drama-001")))
        #expect(firstRepository.calls.contains(.start(
            request: StartPlaybackRequest(dramaId: "drama-001", episodeId: "episode-drama-001", progress: 0),
            playbackSessionId: "session-drama-001"
        )))
        #expect(secondRepository.calls.isEmpty)
        #expect(coordinator.playbackURL(for: "drama-001") == URL(string: "https://example.com/drama-001.mp4"))
        #expect(coordinator.playbackRate(for: "drama-001") == 1.0)
        #expect(coordinator.playbackURL(for: "drama-002") == nil)
        #expect(coordinator.playbackRate(for: "drama-002") == 1.0)

        await coordinator.setActiveDrama(id: "drama-002")

        let firstStop = MockPlayerRepository.Call.stop(
            request: StopPlaybackRequest(
                dramaId: "drama-001",
                episodeId: "episode-drama-001",
                progress: 0,
                duration: 180
            ),
            playbackSessionId: "session-drama-001"
        )
        let secondFetchProgress = MockPlayerRepository.Call.fetchProgress(
            dramaId: "drama-002",
            playbackSessionId: "session-drama-002"
        )
        #expect(firstRepository.calls.contains(firstStop))
        #expect(secondRepository.calls.contains(secondFetchProgress))
        #expect(coordinator.playbackURL(for: "drama-001") == nil)
        #expect(coordinator.playbackURL(for: "drama-002") == URL(string: "https://example.com/drama-002.mp4"))
    }

    @Test("Home feed coordinator forwards progress and stop lifecycle to active player only")
    func testCoordinatorForwardsProgressOnlyToActiveDrama() async {
        let firstRepository = makeRepository(id: "drama-001")
        let secondRepository = makeRepository(id: "drama-002")
        let coordinator = HomeFeedPlaybackCoordinator { drama in
            switch drama.id {
            case "drama-001":
                self.makePlayerViewModel(id: drama.id, repository: firstRepository)
            case "drama-002":
                self.makePlayerViewModel(id: drama.id, repository: secondRepository)
            default:
                Issue.record("Unexpected drama id: \(drama.id)")
                return self.makePlayerViewModel(id: drama.id, repository: MockPlayerRepository())
            }
        }

        await coordinator.configure(with: [makeDrama(id: "drama-001"), makeDrama(id: "drama-002")])
        await coordinator.setActiveDrama(id: "drama-001")

        coordinator.updateProgress(66, for: "drama-002")
        coordinator.handlePlaybackEnded(for: "drama-002")
        coordinator.handlePlaybackFailure("ignored", for: "drama-002")
        #expect(secondRepository.calls.isEmpty)

        coordinator.updateProgress(42, for: "drama-001")
        coordinator.handleContainerDisappear()
        try? await Task.sleep(for: .milliseconds(50))

        let progressedStop = MockPlayerRepository.Call.stop(
            request: StopPlaybackRequest(
                dramaId: "drama-001",
                episodeId: "episode-drama-001",
                progress: 42,
                duration: 180
            ),
            playbackSessionId: "session-drama-001"
        )
        #expect(firstRepository.calls.contains(progressedStop))
        #expect(secondRepository.calls.isEmpty)
    }

    private func makeDrama(id: String) -> Drama {
        Drama(
            id: id,
            title: "示例短剧 \(id)",
            description: "首页视频流测试数据",
            coverUrl: "https://example.com/\(id).jpg",
            category: "都市",
            episodeCount: 10,
            tags: ["逆袭"],
            rating: 9.0,
            createdAt: "2026-08-03T00:00:00Z",
            updatedAt: "2026-08-03T00:00:00Z"
        )
    }

    private func makeRepository(id: String) -> MockPlayerRepository {
        let repository = MockPlayerRepository()
        repository.progressResult = .success(makeProgress(dramaId: id))
        repository.episodesResult = .success(
            makeEpisodeList(
                dramaId: id,
                items: [
                    Episode(
                        id: "episode-\(id)",
                        dramaId: id,
                        title: "第 1 集",
                        episodeNumber: 1,
                        videoUrl: "https://example.com/\(id).mp4",
                        duration: 180,
                        thumbnailUrl: "https://example.com/\(id).jpg",
                        description: "示例描述",
                        createdAt: "2026-08-03T00:00:00Z",
                        updatedAt: "2026-08-03T00:00:00Z"
                    )
                ]
            )
        )
        return repository
    }

    private func makePlayerViewModel(id: String, repository: MockPlayerRepository) -> PlayerViewModel {
        let sessionStore = MockPlaybackSessionStore()
        sessionStore.sessionId = "session-\(id)"
        return PlayerViewModel(
            videoId: id,
            fetchPlayerProgressUseCase: FetchPlayerProgressUseCase(repository: repository),
            fetchDramaEpisodesUseCase: FetchDramaEpisodesUseCase(repository: repository),
            startPlaybackUseCase: StartPlaybackUseCase(repository: repository),
            stopPlaybackUseCase: StopPlaybackUseCase(repository: repository),
            playbackSessionStore: sessionStore
        )
    }
}
