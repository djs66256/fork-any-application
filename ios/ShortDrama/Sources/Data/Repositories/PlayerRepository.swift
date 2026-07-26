import Foundation

struct PlayerRepository: PlayerRepositoryProtocol, Sendable {
    private let dataSource: PlayerRemoteDataSource

    init(dataSource: PlayerRemoteDataSource = PlayerRemoteDataSource()) {
        self.dataSource = dataSource
    }

    func fetchProgress(dramaId: String, playbackSessionId: String) async throws -> PlayerProgress {
        let response = try await dataSource.fetchProgress(
            dramaId: dramaId,
            playbackSessionId: playbackSessionId
        )
        return response.data.toEntity()
    }

    func fetchEpisodes(dramaId: String) async throws -> EpisodeList {
        let response = try await dataSource.fetchEpisodes(dramaId: dramaId)
        return response.data.toEntity()
    }

    func startPlayback(
        request: StartPlaybackRequest,
        playbackSessionId: String
    ) async throws -> PlaybackStartReceipt {
        let response = try await dataSource.startPlayback(
            request: request,
            playbackSessionId: playbackSessionId
        )
        return response.data.toEntity()
    }

    func stopPlayback(
        request: StopPlaybackRequest,
        playbackSessionId: String
    ) async throws -> PlaybackStopReceipt {
        let response = try await dataSource.stopPlayback(
            request: request,
            playbackSessionId: playbackSessionId
        )
        return response.data.toEntity()
    }
}
