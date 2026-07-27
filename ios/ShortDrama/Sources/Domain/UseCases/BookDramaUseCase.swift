import Foundation

/// Use case for booking a drama.
struct BookDramaUseCase: Sendable {
    private let repository: DramaRepositoryProtocol

    init(repository: DramaRepositoryProtocol) {
        self.repository = repository
    }

    func execute(id: String) async throws -> BookDramaResult {
        try await repository.bookDrama(id: id)
    }
}
