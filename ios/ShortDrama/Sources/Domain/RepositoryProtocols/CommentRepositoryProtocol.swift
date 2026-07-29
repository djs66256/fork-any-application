import Foundation

protocol CommentRepositoryProtocol: Sendable {
    func fetchComments(query: CommentQuery) async throws -> PagedResult<Comment>
    func createComment(dramaId: String, content: String) async throws -> Comment
    func toggleCommentLike(dramaId: String, commentId: String) async throws -> ToggleCommentLikeResult
}
