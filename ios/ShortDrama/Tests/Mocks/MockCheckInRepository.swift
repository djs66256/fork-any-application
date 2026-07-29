import Foundation
@testable import ShortDrama

final class MockCheckInRepository: CheckInRepositoryProtocol, @unchecked Sendable {
    enum Call: Equatable {
        case fetchStatus(installationId: String?, accessToken: String?)
        case submitCheckIn(installationId: String?, accessToken: String?)
    }

    var fetchStatusResult: Result<SignInStatus, Error> = .success(.fixture())
    var submitCheckInResult: Result<SignInStatus, Error> = .success(.signedFixture())

    private(set) var calls: [Call] = []

    func fetchStatus(installationId: String?, accessToken: String?) async throws -> SignInStatus {
        calls.append(.fetchStatus(installationId: installationId, accessToken: accessToken))
        return try fetchStatusResult.get()
    }

    func submitCheckIn(installationId: String?, accessToken: String?) async throws -> SignInStatus {
        calls.append(.submitCheckIn(installationId: installationId, accessToken: accessToken))
        return try submitCheckInResult.get()
    }
}
