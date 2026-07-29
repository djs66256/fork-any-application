import Foundation

struct FetchSystemMessagesUseCase: Sendable {
    private let repository: MessageRepositoryProtocol

    init(repository: MessageRepositoryProtocol) {
        self.repository = repository
    }

    func execute(page: Int, pageSize: Int) async throws -> PagedResult<SystemMessage> {
        try await repository.fetchSystemMessages(page: page, pageSize: pageSize)
    }
}
