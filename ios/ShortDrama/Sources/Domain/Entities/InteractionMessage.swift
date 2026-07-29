import Foundation

struct InteractionMessage: Equatable, Identifiable, Sendable {
    enum MessageType: String, Codable, Equatable, Sendable {
        case commentReply = "comment_reply"
        case commentLike = "comment_like"
        case systemHint = "system_hint"
    }

    let id: String
    let type: MessageType
    let title: String
    let summary: String
    let sentAt: String
}
