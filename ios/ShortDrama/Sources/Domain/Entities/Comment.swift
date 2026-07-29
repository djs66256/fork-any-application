import Foundation

struct CommentUserSummary: Equatable, Sendable {
    let id: String
    let displayName: String
    let avatarUrl: String?
}

struct Comment: Identifiable, Equatable, Sendable {
    let id: String
    let dramaId: String
    let content: String
    let likeCount: Int
    let liked: Bool
    let createdAt: String
    let updatedAt: String
    let user: CommentUserSummary

    func withLikeState(liked: Bool, likeCount: Int) -> Comment {
        Comment(
            id: id,
            dramaId: dramaId,
            content: content,
            likeCount: likeCount,
            liked: liked,
            createdAt: createdAt,
            updatedAt: updatedAt,
            user: user
        )
    }
}

enum CommentSort: String, CaseIterable, Equatable, Sendable {
    case latest
    case hot

    var title: String {
        switch self {
        case .latest:
            return "最新"
        case .hot:
            return "最热"
        }
    }
}
