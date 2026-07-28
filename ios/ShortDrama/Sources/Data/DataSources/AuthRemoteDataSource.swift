import Foundation

final class AuthRemoteDataSource: @unchecked Sendable {
    private let client: APIClient

    init(client: APIClient = .shared) {
        self.client = client
    }

    func sendOtp(phone: String, countryCode: String, scene: String) async throws -> SendOtpEnvelopeDTO {
        try await client.request(
            AuthEndpoints.SendOtp(
                request: SendOtpRequestDTO(
                    countryCode: countryCode,
                    phone: phone,
                    scene: scene
                )
            )
        )
    }

    func createSession(phone: String, countryCode: String, code: String) async throws -> AuthSessionEnvelopeDTO {
        try await client.request(
            AuthEndpoints.CreateSession(
                request: CreateAuthSessionRequestDTO(
                    countryCode: countryCode,
                    phone: phone,
                    code: code
                )
            )
        )
    }

    func refreshSession(refreshToken: String) async throws -> AuthSessionEnvelopeDTO {
        try await client.request(
            AuthEndpoints.RefreshSession(
                request: RefreshAuthSessionRequestDTO(refreshToken: refreshToken)
            )
        )
    }

    func getCurrentUser(accessToken: String) async throws -> AuthUserEnvelopeDTO {
        try await client.request(
            AuthEndpoints.GetCurrentUser(accessToken: accessToken)
        )
    }

    func logout(accessToken: String?) async throws {
        let _: EmptySuccessDTO = try await client.request(
            AuthEndpoints.Logout(accessToken: accessToken)
        )
    }
}

enum AuthEndpoints {
    private static var authBodyEncoder: JSONEncoder {
        let encoder = JSONEncoder()
        return encoder
    }

    struct SendOtp: APIEndpoint {
        typealias Response = SendOtpEnvelopeDTO

        let request: SendOtpRequestDTO

        var path: String { "/api/auth/otp-requests" }
        var method: HTTPMethod { .post }
        var body: Encodable? { request }
        var bodyEncoder: JSONEncoder { AuthEndpoints.authBodyEncoder }
    }

    struct CreateSession: APIEndpoint {
        typealias Response = AuthSessionEnvelopeDTO

        let request: CreateAuthSessionRequestDTO

        var path: String { "/api/auth/sessions" }
        var method: HTTPMethod { .post }
        var body: Encodable? { request }
        var bodyEncoder: JSONEncoder { AuthEndpoints.authBodyEncoder }
    }

    struct RefreshSession: APIEndpoint {
        typealias Response = AuthSessionEnvelopeDTO

        let request: RefreshAuthSessionRequestDTO

        var path: String { "/api/auth/session-refreshes" }
        var method: HTTPMethod { .post }
        var body: Encodable? { request }
        var bodyEncoder: JSONEncoder { AuthEndpoints.authBodyEncoder }
    }

    struct GetCurrentUser: APIEndpoint {
        typealias Response = AuthUserEnvelopeDTO

        let accessToken: String

        var path: String { "/api/users/me" }
        var method: HTTPMethod { .get }
        var headers: [String: String] {
            ["Authorization": "Bearer \(accessToken)"]
        }
    }

    struct Logout: APIEndpoint {
        typealias Response = EmptySuccessDTO

        let accessToken: String?

        var path: String { "/api/auth/session" }
        var method: HTTPMethod { .delete }
        var headers: [String: String] {
            guard let accessToken, !accessToken.isEmpty else {
                return [:]
            }

            return ["Authorization": "Bearer \(accessToken)"]
        }
    }
}
