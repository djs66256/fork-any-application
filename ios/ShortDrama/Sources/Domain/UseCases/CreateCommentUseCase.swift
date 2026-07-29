import Foundation

struct CreateCommentUseCase: Sendable {
    private let repository: CommentRepositoryProtocol

    init(repository: CommentRepositoryProtocol) {
        self.repository = repository
    }

    func execute(dramaId: String, content: String) async throws -> Comment {
        try await repository.createComment(dramaId: dramaId, content: content)
    }
}
