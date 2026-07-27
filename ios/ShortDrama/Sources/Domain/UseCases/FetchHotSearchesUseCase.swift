import Foundation

/// Use case for fetching hot searches.
struct FetchHotSearchesUseCase: Sendable {

    private let repository: DramaRepositoryProtocol

    init(repository: DramaRepositoryProtocol) {
        self.repository = repository
    }

    func execute() async throws -> [HotSearchItem] {
        try await repository.fetchHotSearches()
    }
}
