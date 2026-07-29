import Foundation

struct SubmitCheckInUseCase: Sendable {
    private let repository: CheckInRepositoryProtocol

    init(repository: CheckInRepositoryProtocol) {
        self.repository = repository
    }

    func execute(installationId: String?, accessToken: String?) async throws -> SignInStatus {
        try await repository.submitCheckIn(installationId: installationId, accessToken: accessToken)
    }
}
