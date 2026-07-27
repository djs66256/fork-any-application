import Foundation

/// Use case for searching dramas by keyword.
struct SearchDramasUseCase: Sendable {

    private let repository: DramaRepositoryProtocol

    init(repository: DramaRepositoryProtocol) {
        self.repository = repository
    }

    func execute(query: String, page: Int, pageSize: Int) async throws -> [Drama] {
        try await repository.searchDramas(query: query, page: page, pageSize: pageSize)
    }
}
