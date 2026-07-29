import Foundation
@testable import ShortDrama

final class MockMessageRepository: MessageRepositoryProtocol, @unchecked Sendable {
    enum Call: Equatable {
        case fetchPreview
        case fetchSystemMessages(page: Int, pageSize: Int)
        case fetchInteractionMessages(page: Int, pageSize: Int, accessToken: String)
    }

    var previewResult: Result<MessagePreview?, Error> = .success(.fixture())
    var systemMessagesResult: Result<PagedResult<SystemMessage>, Error> = .success(.systemFixture())
    var interactionMessagesResult: Result<PagedResult<InteractionMessage>, Error> = .success(.interactionFixture())

    private(set) var calls: [Call] = []

    func fetchPreview() async throws -> MessagePreview? {
        calls.append(.fetchPreview)
        return try previewResult.get()
    }

    func fetchSystemMessages(page: Int, pageSize: Int) async throws -> PagedResult<SystemMessage> {
        calls.append(.fetchSystemMessages(page: page, pageSize: pageSize))
        return try systemMessagesResult.get()
    }

    func fetchInteractionMessages(page: Int, pageSize: Int, accessToken: String) async throws -> PagedResult<InteractionMessage> {
        calls.append(.fetchInteractionMessages(page: page, pageSize: pageSize, accessToken: accessToken))
        return try interactionMessagesResult.get()
    }
}
