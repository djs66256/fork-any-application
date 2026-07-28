import Foundation

struct AuthUser: Codable, Equatable, Sendable {
    let id: String
    let phone: String
    let displayName: String?
    let avatarURL: String?
    let role: String
    let isNewUser: Bool
}
