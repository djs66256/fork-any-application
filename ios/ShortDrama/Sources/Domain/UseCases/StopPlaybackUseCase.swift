import Foundation

struct StopPlaybackUseCase: Sendable {
    private let repository: PlayerRepositoryProtocol

    init(repository: PlayerRepositoryProtocol) {
        self.repository = repository
    }

    func execute(
        request: StopPlaybackRequest,
        playbackSessionId: String
    ) async throws -> PlaybackStopReceipt {
        try await repository.stopPlayback(
            request: request,
            playbackSessionId: playbackSessionId
        )
    }
}
