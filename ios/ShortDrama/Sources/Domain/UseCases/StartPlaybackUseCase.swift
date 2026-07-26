import Foundation

struct StartPlaybackUseCase: Sendable {
    private let repository: PlayerRepositoryProtocol

    init(repository: PlayerRepositoryProtocol) {
        self.repository = repository
    }

    func execute(
        request: StartPlaybackRequest,
        playbackSessionId: String
    ) async throws -> PlaybackStartReceipt {
        try await repository.startPlayback(
            request: request,
            playbackSessionId: playbackSessionId
        )
    }
}
