import Foundation
@testable import ShortDrama

final class MockPlayerRepository: PlayerRepositoryProtocol, MenuPanelRepositoryProtocol, @unchecked Sendable {
    enum Call: Equatable {
        case fetchProgress(dramaId: String, playbackSessionId: String)
        case fetchEpisodes(dramaId: String)
        case start(request: StartPlaybackRequest, playbackSessionId: String)
        case stop(request: StopPlaybackRequest, playbackSessionId: String)
        case fetchRecentlyViewed(playbackSessionId: String)
    }

    var progressResult: Result<PlayerProgress, Error> = .success(
        PlayerProgress(dramaId: "", hasHistory: false, episodeId: nil, startTime: 0, updatedAt: nil)
    )
    var episodesResult: Result<EpisodeList, Error> = .success(
        EpisodeList(dramaId: "", seriesStatus: .completed, items: [])
    )
    var startResult: Result<PlaybackStartReceipt, Error> = .success(
        PlaybackStartReceipt(
            dramaId: "",
            episodeId: "",
            acceptedProgress: 0,
            playbackSessionId: "session",
            startedAt: "2026-07-26T00:00:00Z"
        )
    )
    var stopResult: Result<PlaybackStopReceipt, Error> = .success(
        PlaybackStopReceipt(
            dramaId: "",
            episodeId: "",
            savedProgress: 0,
            duration: 1,
            updatedAt: "2026-07-26T00:00:00Z"
        )
    )
    var recentlyViewedResult: Result<[RecentlyViewedItem], Error> = .success([])

    private(set) var calls: [Call] = []

    func fetchProgress(dramaId: String, playbackSessionId: String) async throws -> PlayerProgress {
        calls.append(.fetchProgress(dramaId: dramaId, playbackSessionId: playbackSessionId))
        return try progressResult.get()
    }

    func fetchEpisodes(dramaId: String) async throws -> EpisodeList {
        calls.append(.fetchEpisodes(dramaId: dramaId))
        return try episodesResult.get()
    }

    func startPlayback(
        request: StartPlaybackRequest,
        playbackSessionId: String
    ) async throws -> PlaybackStartReceipt {
        calls.append(.start(request: request, playbackSessionId: playbackSessionId))
        return try startResult.get()
    }

    func stopPlayback(
        request: StopPlaybackRequest,
        playbackSessionId: String
    ) async throws -> PlaybackStopReceipt {
        calls.append(.stop(request: request, playbackSessionId: playbackSessionId))
        return try stopResult.get()
    }

    func fetchRecentlyViewed(playbackSessionId: String) async throws -> [RecentlyViewedItem] {
        calls.append(.fetchRecentlyViewed(playbackSessionId: playbackSessionId))
        return try recentlyViewedResult.get()
    }
}
