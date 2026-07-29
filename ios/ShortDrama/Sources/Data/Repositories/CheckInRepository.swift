import Foundation

struct CheckInRepository: CheckInRepositoryProtocol, Sendable {
    private let dataSource: CheckInRemoteDataSource

    init(dataSource: CheckInRemoteDataSource = CheckInRemoteDataSource()) {
        self.dataSource = dataSource
    }

    func fetchStatus(installationId: String?, accessToken: String?) async throws -> SignInStatus {
        try await dataSource.fetchStatus(installationId: installationId, accessToken: accessToken).toEntity()
    }

    func submitCheckIn(installationId: String?, accessToken: String?) async throws -> SignInStatus {
        try await dataSource.submitCheckIn(installationId: installationId, accessToken: accessToken).toEntity()
    }
}
