import Foundation

protocol CheckInRepositoryProtocol: Sendable {
    func fetchStatus(installationId: String?, accessToken: String?) async throws -> SignInStatus
    func submitCheckIn(installationId: String?, accessToken: String?) async throws -> SignInStatus
}
