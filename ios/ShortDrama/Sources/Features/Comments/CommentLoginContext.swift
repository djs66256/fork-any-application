import Foundation

struct PendingCommentAction: Equatable, Sendable {
    enum Kind: String, Sendable {
        case openSheet
        case createComment
        case toggleLike
    }

    let kind: Kind
    let commentId: String?
}

struct CommentLoginContext: Equatable, Sendable {
    enum Source: String, Sendable {
        case home
        case player
    }

    let source: Source
    let dramaId: String
    let action: PendingCommentAction
}
