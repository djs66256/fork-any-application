import Foundation

struct FetchPlayerProgressUseCase: Sendable {
    private let repository: PlayerRepositoryProtocol

    init(repository: PlayerRepositoryProtocol) {
        self.repository = repository
    }

    func execute(dramaId: String, playbackSessionId: String) async throws -> PlayerProgress {
        try await repository.fetchProgress(
            dramaId: dramaId,
            playbackSessionId: playbackSessionId
        )
    }
}
