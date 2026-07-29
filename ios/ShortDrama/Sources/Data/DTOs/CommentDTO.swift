import Foundation

struct CommentUserSummaryDTO: Codable, Equatable {
    let id: String
    let displayName: String
    let avatarUrl: String?

    func toEntity() -> CommentUserSummary {
        CommentUserSummary(id: id, displayName: displayName, avatarUrl: avatarUrl)
    }
}

struct CommentDTO: Codable, Equatable {
    let id: String
    let dramaId: String
    let content: String
    let likeCount: Int
    let liked: Bool
    let createdAt: String
    let updatedAt: String
    let user: CommentUserSummaryDTO

    func toEntity() -> Comment {
        Comment(
            id: id,
            dramaId: dramaId,
            content: content,
            likeCount: likeCount,
            liked: liked,
            createdAt: createdAt,
            updatedAt: updatedAt,
            user: user.toEntity()
        )
    }
}
