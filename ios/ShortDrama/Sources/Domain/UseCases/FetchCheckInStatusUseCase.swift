import Foundation

struct FetchCheckInStatusUseCase: Sendable {
    private let repository: CheckInRepositoryProtocol

    init(repository: CheckInRepositoryProtocol) {
        self.repository = repository
    }

    func execute(installationId: String?, accessToken: String?) async throws -> SignInStatus {
        try await repository.fetchStatus(installationId: installationId, accessToken: accessToken)
    }
}
