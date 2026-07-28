import Foundation

struct AuthRepository: AuthRepositoryProtocol, Sendable {
    private let dataSource: AuthRemoteDataSource

    init(dataSource: AuthRemoteDataSource = AuthRemoteDataSource()) {
        self.dataSource = dataSource
    }

    func sendOtp(phone: String, countryCode: String, scene: String) async throws -> SendOtpResult {
        let response = try await dataSource.sendOtp(
            phone: phone,
            countryCode: countryCode,
            scene: scene
        )
        return response.data.toEntity()
    }

    func createSession(phone: String, countryCode: String, code: String) async throws -> AuthSession {
        let response = try await dataSource.createSession(
            phone: phone,
            countryCode: countryCode,
            code: code
        )
        return response.data.toEntity()
    }

    func refreshSession(refreshToken: String) async throws -> AuthSession {
        let response = try await dataSource.refreshSession(refreshToken: refreshToken)
        return response.data.toEntity()
    }

    func getCurrentUser(accessToken: String) async throws -> AuthUser {
        let response = try await dataSource.getCurrentUser(accessToken: accessToken)
        return response.data.toEntity()
    }

    func logout(accessToken: String?) async throws {
        try await dataSource.logout(accessToken: accessToken)
    }
}
