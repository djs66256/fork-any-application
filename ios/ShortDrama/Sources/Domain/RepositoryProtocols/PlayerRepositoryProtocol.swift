import Foundation

protocol PlayerRepositoryProtocol: Sendable {
    func fetchProgress(dramaId: String, playbackSessionId: String) async throws -> PlayerProgress
    func fetchEpisodes(dramaId: String) async throws -> EpisodeList
    func startPlayback(
        request: StartPlaybackRequest,
        playbackSessionId: String
    ) async throws -> PlaybackStartReceipt
    func stopPlayback(
        request: StopPlaybackRequest,
        playbackSessionId: String
    ) async throws -> PlaybackStopReceipt
}
