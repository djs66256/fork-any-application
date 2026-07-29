import Foundation

struct ToggleCommentLikeUseCase: Sendable {
    private let repository: CommentRepositoryProtocol

    init(repository: CommentRepositoryProtocol) {
        self.repository = repository
    }

    func execute(dramaId: String, commentId: String) async throws -> ToggleCommentLikeResult {
        try await repository.toggleCommentLike(dramaId: dramaId, commentId: commentId)
    }
}
