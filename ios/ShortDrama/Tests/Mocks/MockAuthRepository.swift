import Foundation
@testable import ShortDrama

final class MockAuthRepository: AuthRepositoryProtocol, @unchecked Sendable {
    var sendOtpResult: Result<SendOtpResult, Error> = .success(
        SendOtpResult(requestId: "otp-request", cooldownSeconds: 60, expiresInSeconds: 300)
    )
    var createSessionResult: Result<AuthSession, Error> = .success(
        AuthSession(
            accessToken: "access-token",
            refreshToken: "refresh-token",
            expiresAt: "2026-07-28T12:34:56Z",
            user: AuthUser(
                id: "user-001",
                phone: "13800138000",
                displayName: "测试用户",
                avatarURL: nil,
                role: "viewer",
                isNewUser: false
            )
        )
    )
    var refreshSessionResult: Result<AuthSession, Error> = .success(
        AuthSession(
            accessToken: "refreshed-access-token",
            refreshToken: "refreshed-refresh-token",
            expiresAt: "2026-07-28T13:34:56Z",
            user: AuthUser(
                id: "user-001",
                phone: "13800138000",
                displayName: "测试用户",
                avatarURL: nil,
                role: "viewer",
                isNewUser: false
            )
        )
    )
    var getCurrentUserResult: Result<AuthUser, Error> = .success(
        AuthUser(
            id: "user-001",
            phone: "13800138000",
            displayName: "测试用户",
            avatarURL: nil,
            role: "viewer",
            isNewUser: false
        )
    )
    var logoutError: Error?

    private(set) var sendOtpCallCount = 0
    private(set) var createSessionCallCount = 0
    private(set) var refreshSessionCallCount = 0
    private(set) var getCurrentUserCallCount = 0
    private(set) var logoutCallCount = 0

    private(set) var lastSendOtpPhone: String?
    private(set) var lastSendOtpCountryCode: String?
    private(set) var lastSendOtpScene: String?
    private(set) var lastCreateSessionPhone: String?
    private(set) var lastCreateSessionCountryCode: String?
    private(set) var lastCreateSessionCode: String?
    private(set) var lastRefreshToken: String?
    private(set) var lastGetCurrentUserAccessToken: String?
    private(set) var lastLogoutAccessToken: String?

    func sendOtp(phone: String, countryCode: String, scene: String) async throws -> SendOtpResult {
        sendOtpCallCount += 1
        lastSendOtpPhone = phone
        lastSendOtpCountryCode = countryCode
        lastSendOtpScene = scene
        return try sendOtpResult.get()
    }

    func createSession(phone: String, countryCode: String, code: String) async throws -> AuthSession {
        createSessionCallCount += 1
        lastCreateSessionPhone = phone
        lastCreateSessionCountryCode = countryCode
        lastCreateSessionCode = code
        return try createSessionResult.get()
    }

    func refreshSession(refreshToken: String) async throws -> AuthSession {
        refreshSessionCallCount += 1
        lastRefreshToken = refreshToken
        return try refreshSessionResult.get()
    }

    func getCurrentUser(accessToken: String) async throws -> AuthUser {
        getCurrentUserCallCount += 1
        lastGetCurrentUserAccessToken = accessToken
        return try getCurrentUserResult.get()
    }

    func logout(accessToken: String?) async throws {
        logoutCallCount += 1
        lastLogoutAccessToken = accessToken
        if let logoutError {
            throw logoutError
        }
    }
}
