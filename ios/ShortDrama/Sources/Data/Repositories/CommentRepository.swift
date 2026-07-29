import Foundation

struct CommentRepository: CommentRepositoryProtocol, Sendable {
    private let dataSource: CommentRemoteDataSource

    init(dataSource: CommentRemoteDataSource = CommentRemoteDataSource()) {
        self.dataSource = dataSource
    }

    func fetchComments(query: CommentQuery) async throws -> PagedResult<Comment> {
        try await dataSource.fetchComments(query: query).toEntity()
    }

    func createComment(dramaId: String, content: String) async throws -> Comment {
        try await dataSource.createComment(dramaId: dramaId, content: content).toEntity()
    }

    func toggleCommentLike(dramaId: String, commentId: String) async throws -> ToggleCommentLikeResult {
        try await dataSource.toggleLike(dramaId: dramaId, commentId: commentId).toEntity()
    }
}
