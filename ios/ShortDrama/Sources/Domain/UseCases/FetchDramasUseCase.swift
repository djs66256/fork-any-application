import Foundation

/// Use case for fetching a paginated list of dramas.
struct FetchDramasUseCase: Sendable {

    private let repository: DramaRepositoryProtocol

    /// Creates a new use case with the given repository.
    init(repository: DramaRepositoryProtocol) {
        self.repository = repository
    }

    /// Executes the fetch with pagination parameters.
    /// - Parameters:
    ///   - page: The page number (1-based).
    ///   - pageSize: Number of items per page.
    /// - Returns: Array of Drama entities.
    func execute(page: Int, pageSize: Int) async throws -> [Drama] {
        try await repository.fetchDramas(page: page, pageSize: pageSize)
    }
}
