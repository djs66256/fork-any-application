import Foundation

struct FetchDramaCommentsUseCase: Sendable {
    private let repository: CommentRepositoryProtocol

    init(repository: CommentRepositoryProtocol) {
        self.repository = repository
    }

    func execute(query: CommentQuery) async throws -> PagedResult<Comment> {
        try await repository.fetchComments(query: query)
    }
}
