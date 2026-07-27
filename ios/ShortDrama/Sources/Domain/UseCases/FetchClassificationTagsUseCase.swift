import Foundation

/// Use case for fetching classification tags.
struct FetchClassificationTagsUseCase: Sendable {
    private let repository: DramaRepositoryProtocol

    init(repository: DramaRepositoryProtocol) {
        self.repository = repository
    }

    func execute(gender: ClassificationGender) async throws -> ClassificationTagsPayload {
        try await repository.fetchClassificationTags(gender: gender)
    }
}
