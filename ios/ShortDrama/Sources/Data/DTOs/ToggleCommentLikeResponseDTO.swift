import Foundation

struct ToggleCommentLikeResponseDTO: Codable, Equatable {
    let commentId: String
    let liked: Bool
    let likeCount: Int

    func toEntity() -> ToggleCommentLikeResult {
        ToggleCommentLikeResult(commentId: commentId, liked: liked, likeCount: likeCount)
    }
}
