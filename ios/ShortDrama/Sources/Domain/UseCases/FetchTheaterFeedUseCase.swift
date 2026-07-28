import Foundation

/// Use case for fetching theater feed pages.
struct FetchTheaterFeedUseCase: Sendable {
    private let repository: DramaRepositoryProtocol

    init(repository: DramaRepositoryProtocol) {
        self.repository = repository
    }

    func execute(query: TheaterFeedQuery) async throws -> TheaterFeedPage {
        try await repository.fetchTheaterFeed(query: query)
    }
}
