import Foundation

struct MessageRepository: MessageRepositoryProtocol, Sendable {
    private let dataSource: MessageRemoteDataSource

    init(dataSource: MessageRemoteDataSource = MessageRemoteDataSource()) {
        self.dataSource = dataSource
    }

    func fetchPreview() async throws -> MessagePreview? {
        try await dataSource.fetchPreview()?.toEntity()
    }

    func fetchSystemMessages(page: Int, pageSize: Int) async throws -> PagedResult<SystemMessage> {
        try await dataSource.fetchSystemMessages(page: page, pageSize: pageSize).toEntity()
    }

    func fetchInteractionMessages(page: Int, pageSize: Int, accessToken: String) async throws -> PagedResult<InteractionMessage> {
        try await dataSource.fetchInteractionMessages(page: page, pageSize: pageSize, accessToken: accessToken).toEntity()
    }
}
