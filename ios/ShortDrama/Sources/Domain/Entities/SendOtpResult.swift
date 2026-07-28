import Foundation

struct SendOtpResult: Codable, Equatable, Sendable {
    let requestId: String
    let cooldownSeconds: Int
    let expiresInSeconds: Int
}
