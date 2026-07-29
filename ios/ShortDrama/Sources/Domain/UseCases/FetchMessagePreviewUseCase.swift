import Foundation

struct FetchMessagePreviewUseCase: Sendable {
    private let repository: MessageRepositoryProtocol

    init(repository: MessageRepositoryProtocol) {
        self.repository = repository
    }

    func execute() async throws -> MessagePreview? {
        try await repository.fetchPreview()
    }
}
