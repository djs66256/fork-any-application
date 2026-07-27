import Foundation

/// Use case for fetching ranking lists.
struct FetchRankingsUseCase: Sendable {
    private let repository: DramaRepositoryProtocol

    init(repository: DramaRepositoryProtocol) {
        self.repository = repository
    }

    func execute(query: RankingQuery) async throws -> PagedResult<RankingDrama> {
        try await repository.fetchRankings(query: query)
    }
}
