import Foundation

struct ToggleCommentLikeResult: Equatable, Sendable {
    let commentId: String
    let liked: Bool
    let likeCount: Int
}
