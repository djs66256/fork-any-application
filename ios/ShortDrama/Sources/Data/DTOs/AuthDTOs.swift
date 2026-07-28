import Foundation

struct AuthEnvelopeDTO<T: Decodable>: Decodable, Equatable where T: Equatable {
    let code: Int
    let data: T
    let message: String
}

struct SendOtpRequestDTO: Encodable, Equatable {
    let countryCode: String
    let phone: String
    let scene: String
}

struct SendOtpResponseDTO: Decodable, Equatable {
    let requestId: String
    let cooldownSeconds: Int
    let expiresInSeconds: Int

    private enum CodingKeys: String, CodingKey {
        case requestId
        case cooldownSeconds
        case expiresInSeconds
    }

    init(requestId: String, cooldownSeconds: Int, expiresInSeconds: Int) {
        self.requestId = requestId
        self.cooldownSeconds = cooldownSeconds
        self.expiresInSeconds = expiresInSeconds
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        requestId = try container.decode(String.self, forKey: .requestId)
        cooldownSeconds = try container.decode(Int.self, forKey: .cooldownSeconds)
        expiresInSeconds = (try? container.decode(Int.self, forKey: .expiresInSeconds)) ?? 300
    }

    func toEntity() -> SendOtpResult {
        SendOtpResult(
            requestId: requestId,
            cooldownSeconds: cooldownSeconds,
            expiresInSeconds: expiresInSeconds
        )
    }
}

struct CreateAuthSessionRequestDTO: Encodable, Equatable {
    let countryCode: String
    let phone: String
    let code: String
}

struct RefreshAuthSessionRequestDTO: Encodable, Equatable {
    let refreshToken: String
}

struct AuthUserDTO: Codable, Equatable {
    let id: String
    let phone: String
    let displayName: String?
    let avatarURL: String?
    let role: String
    let isNewUser: Bool

    private enum CodingKeys: String, CodingKey {
        case id
        case phone
        case displayName
        case avatarURL = "avatarUrl"
        case role
        case isNewUser
    }

    func toEntity() -> AuthUser {
        AuthUser(
            id: id,
            phone: phone,
            displayName: displayName,
            avatarURL: avatarURL,
            role: role,
            isNewUser: isNewUser
        )
    }
}

struct AuthSessionDTO: Codable, Equatable {
    let accessToken: String
    let refreshToken: String
    let expiresAt: String
    let user: AuthUserDTO

    private enum CodingKeys: String, CodingKey {
        case accessToken
        case refreshToken
        case expiresAt
        case user
    }

    func toEntity() -> AuthSession {
        AuthSession(
            accessToken: accessToken,
            refreshToken: refreshToken,
            expiresAt: expiresAt,
            user: user.toEntity()
        )
    }
}

typealias SendOtpEnvelopeDTO = AuthEnvelopeDTO<SendOtpResponseDTO>
typealias AuthSessionEnvelopeDTO = AuthEnvelopeDTO<AuthSessionDTO>
typealias AuthUserEnvelopeDTO = AuthEnvelopeDTO<AuthUserDTO>
struct EmptySuccessDTO: Decodable, Equatable {}
