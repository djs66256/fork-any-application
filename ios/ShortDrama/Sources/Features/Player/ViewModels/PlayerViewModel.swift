import Foundation
import SwiftUI

@MainActor
final class PlayerViewModel: ObservableObject {

    let videoId: String
    var dramaId: String { videoId }

    @Published private(set) var uiState: UiState = .idle
    @Published private(set) var episodes: [Episode] = []
    @Published private(set) var currentEpisode: Episode?
    @Published private(set) var currentProgress: Double = 0
    @Published private(set) var currentSpeed: PlaybackSpeed = .normal
    @Published private(set) var seriesStatus: DramaSeriesStatus = .completed
    @Published private(set) var playbackURL: URL?
    @Published private(set) var playbackRate: Float = 1.0
    @Published private(set) var liked = false
    @Published private(set) var favorited = false
    @Published var isEpisodeSheetPresented = false
    @Published var isSpeedDialogPresented = false
    @Published var isMoreDialogPresented = false

    private let router: NavigationRouter
    private let earnTaskContext: EarnTaskContext?
    private let fetchPlayerProgressUseCase: FetchPlayerProgressUseCase
    private let fetchDramaEpisodesUseCase: FetchDramaEpisodesUseCase
    private let startPlaybackUseCase: StartPlaybackUseCase
    private let stopPlaybackUseCase: StopPlaybackUseCase
    private let playbackSessionStore: PlaybackSessionStore

    private var hasLoadedOnce = false
    private var bootstrapTask: Task<Void, Never>?
    private var switchEpisodeTask: Task<Void, Never>?
    private var lastStopFingerprint: StopFingerprint?
    private var hasReportedEarnResult = false

    init(
        videoId: String,
        router: NavigationRouter = NavigationRouter(),
        earnTaskContext: EarnTaskContext? = nil,
        fetchPlayerProgressUseCase: FetchPlayerProgressUseCase = .init(
            repository: PlayerRepository()
        ),
        fetchDramaEpisodesUseCase: FetchDramaEpisodesUseCase = .init(
            repository: PlayerRepository()
        ),
        startPlaybackUseCase: StartPlaybackUseCase = .init(
            repository: PlayerRepository()
        ),
        stopPlaybackUseCase: StopPlaybackUseCase = .init(
            repository: PlayerRepository()
        ),
        playbackSessionStore: PlaybackSessionStore = KeychainPlaybackSessionStore()
    ) {
        self.videoId = videoId
        self.router = router
        self.earnTaskContext = earnTaskContext
        self.fetchPlayerProgressUseCase = fetchPlayerProgressUseCase
        self.fetchDramaEpisodesUseCase = fetchDramaEpisodesUseCase
        self.startPlaybackUseCase = startPlaybackUseCase
        self.stopPlaybackUseCase = stopPlaybackUseCase
        self.playbackSessionStore = playbackSessionStore
    }

    func loadIfNeeded() async {
        guard !hasLoadedOnce else { return }
        hasLoadedOnce = true
        await bootstrap()
    }

    func retryBootstrap() async {
        await bootstrap()
    }

    func switchEpisode(to episode: Episode) async {
        guard episode.id != currentEpisode?.id else { return }
        guard episode.isPlayable else { return }

        switchEpisodeTask?.cancel()
        switchEpisodeTask = Task { [weak self] in
            guard let self else { return }
            await self.performEpisodeSwitch(to: episode)
        }
        await switchEpisodeTask?.value
    }

    func handleBack() {
        finishEarnFlowIfNeeded(completed: false, reason: .userExit)
        if earnTaskContext == nil {
            router.dismiss()
        }
        Task(priority: .userInitiated) {
            await stopPlaybackIfNeeded(bestEffort: true)
        }
    }

    func handleDisappear() {
        lastStopFingerprint = nil
        finishEarnFlowIfNeeded(completed: false, reason: .containerRecreated)
        Task {
            await stopPlaybackIfNeeded(bestEffort: true)
        }
    }

    func handleScenePhaseChange(_ phase: ScenePhase) async {
        guard phase == .background else { return }
        finishEarnFlowIfNeeded(completed: false, reason: .backgrounded)
        await stopPlaybackIfNeeded(bestEffort: true)
        if currentEpisode != nil {
            uiState = .paused
        }
    }

    func handlePlaybackEnded() {
        if let episode = currentEpisode {
            currentProgress = Double(max(episode.duration, 1))
        }
        finishEarnFlowIfNeeded(completed: true, reason: .playbackEnded)
        if earnTaskContext == nil {
            uiState = .paused
        }
        Task {
            await stopPlaybackIfNeeded(bestEffort: true)
        }
    }

    func handlePlaybackFailure(message: String) {
        finishEarnFlowIfNeeded(completed: false, reason: .error)
        uiState = .error(message)
        Task {
            await stopPlaybackIfNeeded(bestEffort: true)
        }
    }

    func selectSpeed(_ speed: PlaybackSpeed) {
        currentSpeed = speed
        playbackRate = Float(speed.rawValue)
    }

    func updateCurrentProgress(_ progress: Double) {
        currentProgress = max(0, progress)
        lastStopFingerprint = nil
    }

    func toggleLike() {
        liked.toggle()
    }

    func toggleFavorite() {
        favorited.toggle()
    }

    func playableEpisodes() -> [Episode] {
        episodes.filter(\.isPlayable)
    }

    private func bootstrap() async {
        bootstrapTask?.cancel()
        bootstrapTask = Task { [weak self] in
            guard let self else { return }
            await self.performBootstrap()
        }
        await bootstrapTask?.value
    }

    private func performBootstrap() async {
        uiState = .bootstrapping
        playbackURL = nil
        currentEpisode = nil
        currentProgress = 0
        lastStopFingerprint = nil

        do {
            let sessionId = try playbackSessionStore.getOrCreateSessionId()
            let progress = try await fetchPlayerProgressUseCase.execute(
                dramaId: dramaId,
                playbackSessionId: sessionId
            )
            let episodeList = try await fetchDramaEpisodesUseCase.execute(dramaId: dramaId)
            episodes = episodeList.items.sorted(by: { $0.episodeNumber < $1.episodeNumber })
            seriesStatus = episodeList.seriesStatus

            guard let targetEpisode = resolveBootstrapEpisode(progress: progress, episodes: episodes) else {
                uiState = .noResource
                return
            }

            let startProgress = progress.hasHistory && progress.episodeId == targetEpisode.id ? progress.startTime : 0
            try await startPlayback(
                episode: targetEpisode,
                progress: startProgress,
                playbackSessionId: sessionId
            )
        } catch is CancellationError {
            return
        } catch let error as APIError {
            uiState = .error(error.errorDescription ?? "加载失败，请重试")
        } catch {
            uiState = .error(error.localizedDescription)
        }
    }

    private func performEpisodeSwitch(to episode: Episode) async {
        uiState = .switchingEpisode

        do {
            let sessionId = try playbackSessionStore.getOrCreateSessionId()
            await stopPlaybackIfNeeded(bestEffort: true, playbackSessionId: sessionId)
            try await startPlayback(episode: episode, progress: 0, playbackSessionId: sessionId)
        } catch is CancellationError {
            uiState = .paused
        } catch let error as APIError {
            uiState = .error(error.errorDescription ?? "切换失败，请重试")
        } catch {
            uiState = .error(error.localizedDescription)
        }
    }

    private func resolveBootstrapEpisode(progress: PlayerProgress, episodes: [Episode]) -> Episode? {
        let playable = episodes.filter(\.isPlayable)
        guard !playable.isEmpty else { return nil }

        if progress.hasHistory,
           let episodeId = progress.episodeId,
           let resumed = playable.first(where: { $0.id == episodeId }) {
            return resumed
        }

        return playable.first
    }

    private func startPlayback(
        episode: Episode,
        progress: Double,
        playbackSessionId: String
    ) async throws {
        let receipt = try await startPlaybackUseCase.execute(
            request: StartPlaybackRequest(
                dramaId: dramaId,
                episodeId: episode.id,
                progress: progress
            ),
            playbackSessionId: playbackSessionId
        )

        currentEpisode = episode
        currentProgress = receipt.acceptedProgress
        playbackURL = URL(string: episode.videoUrl)
        playbackRate = Float(currentSpeed.rawValue)
        uiState = .playing
        lastStopFingerprint = nil
    }

    private func stopPlaybackIfNeeded(
        bestEffort: Bool,
        playbackSessionId: String? = nil
    ) async {
        guard let episode = currentEpisode else { return }

        let duration = Double(max(episode.duration, 1))
        let normalizedProgress = min(max(currentProgress, 0), duration)
        let fingerprint = StopFingerprint(
            episodeId: episode.id,
            progress: normalizedProgress,
            duration: duration
        )
        if lastStopFingerprint == fingerprint {
            return
        }

        do {
            let resolvedSessionId: String
            if let playbackSessionId {
                resolvedSessionId = playbackSessionId
            } else {
                resolvedSessionId = try self.playbackSessionStore.getOrCreateSessionId()
            }
            _ = try await stopPlaybackUseCase.execute(
                request: StopPlaybackRequest(
                    dramaId: dramaId,
                    episodeId: episode.id,
                    progress: normalizedProgress,
                    duration: duration
                ),
                playbackSessionId: resolvedSessionId
            )
            lastStopFingerprint = fingerprint
        } catch {
            guard !bestEffort else { return }
        }
    }

    private func finishEarnFlowIfNeeded(completed: Bool, reason: EarnTaskPlayerResult.Reason) {
        guard let earnTaskContext,
              !hasReportedEarnResult,
              let result = EarnTaskPlayerResult(
                taskId: earnTaskContext.taskId,
                videoId: earnTaskContext.videoId,
                completed: completed,
                reason: reason,
                source: earnTaskContext.source
              ) else {
            return
        }

        hasReportedEarnResult = true
        router.finishEarnTaskPlayer(result: result)
    }
}
