import Foundation

protocol AuthRepositoryProtocol: Sendable {
    func sendOtp(phone: String, countryCode: String, scene: String) async throws -> SendOtpResult
    func createSession(phone: String, countryCode: String, code: String) async throws -> AuthSession
    func refreshSession(refreshToken: String) async throws -> AuthSession
    func getCurrentUser(accessToken: String) async throws -> AuthUser
    func logout(accessToken: String?) async throws
}
