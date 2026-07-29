import Foundation

struct FetchInteractionMessagesUseCase: Sendable {
    private let repository: MessageRepositoryProtocol

    init(repository: MessageRepositoryProtocol) {
        self.repository = repository
    }

    func execute(page: Int, pageSize: Int, accessToken: String) async throws -> PagedResult<InteractionMessage> {
        try await repository.fetchInteractionMessages(page: page, pageSize: pageSize, accessToken: accessToken)
    }
}
