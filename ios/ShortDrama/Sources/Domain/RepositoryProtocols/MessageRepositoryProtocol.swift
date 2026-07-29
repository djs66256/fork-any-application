import Foundation

protocol MessageRepositoryProtocol: Sendable {
    func fetchPreview() async throws -> MessagePreview?
    func fetchSystemMessages(page: Int, pageSize: Int) async throws -> PagedResult<SystemMessage>
    func fetchInteractionMessages(page: Int, pageSize: Int, accessToken: String) async throws -> PagedResult<InteractionMessage>
}
