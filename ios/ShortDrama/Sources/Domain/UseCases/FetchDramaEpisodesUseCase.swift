import Foundation

struct FetchDramaEpisodesUseCase: Sendable {
    private let repository: PlayerRepositoryProtocol

    init(repository: PlayerRepositoryProtocol) {
        self.repository = repository
    }

    func execute(dramaId: String) async throws -> EpisodeList {
        try await repository.fetchEpisodes(dramaId: dramaId)
    }
}
