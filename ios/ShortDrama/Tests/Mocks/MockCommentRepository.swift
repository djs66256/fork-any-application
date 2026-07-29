import Foundation
@testable import ShortDrama

final class MockCommentRepository: CommentRepositoryProtocol, @unchecked Sendable {
    enum FetchBehavior {
        case success(PagedResult<Comment>)
        case failure(APIError)
        case delayed(PagedResult<Comment>, TimeInterval)
    }

    enum CreateBehavior {
        case success(Comment)
        case failure(APIError)
        case delayed(Comment, TimeInterval)
    }

    enum ToggleLikeBehavior {
        case success(ToggleCommentLikeResult)
        case failure(APIError)
        case delayed(ToggleCommentLikeResult, TimeInterval)
    }

    enum Call: Equatable {
        case fetch(CommentQuery)
        case create(dramaId: String, content: String)
        case toggleLike(dramaId: String, commentId: String)
    }

    var fetchBehavior: FetchBehavior = .success(
        PagedResult(items: [], page: 1, pageSize: 20, total: 0, totalPages: 1)
    )
    var queuedFetchBehaviors: [FetchBehavior] = []

    var createBehavior: CreateBehavior = .success(
        Comment(
            id: "comment-0",
            dramaId: "drama-0",
            content: "",
            likeCount: 0,
            liked: false,
            createdAt: "2026-07-29T00:00:00Z",
            updatedAt: "2026-07-29T00:00:00Z",
            user: CommentUserSummary(id: "user-0", displayName: "用户", avatarUrl: nil)
        )
    )
    var queuedCreateBehaviors: [CreateBehavior] = []

    var toggleLikeBehavior: ToggleLikeBehavior = .success(
        ToggleCommentLikeResult(commentId: "comment-0", liked: true, likeCount: 1)
    )
    var queuedToggleLikeBehaviors: [ToggleLikeBehavior] = []

    private(set) var calls: [Call] = []

    func fetchComments(query: CommentQuery) async throws -> PagedResult<Comment> {
        calls.append(.fetch(query))
        let behavior = queuedFetchBehaviors.isEmpty ? fetchBehavior : queuedFetchBehaviors.removeFirst()
        switch behavior {
        case .success(let page):
            return page
        case .failure(let error):
            throw error
        case .delayed(let page, let delay):
            try await Task.sleep(for: .seconds(delay))
            return page
        }
    }

    func createComment(dramaId: String, content: String) async throws -> Comment {
        calls.append(.create(dramaId: dramaId, content: content))
        let behavior = queuedCreateBehaviors.isEmpty ? createBehavior : queuedCreateBehaviors.removeFirst()
        switch behavior {
        case .success(let comment):
            return comment
        case .failure(let error):
            throw error
        case .delayed(let comment, let delay):
            try await Task.sleep(for: .seconds(delay))
            return comment
        }
    }

    func toggleCommentLike(dramaId: String, commentId: String) async throws -> ToggleCommentLikeResult {
        calls.append(.toggleLike(dramaId: dramaId, commentId: commentId))
        let behavior = queuedToggleLikeBehaviors.isEmpty ? toggleLikeBehavior : queuedToggleLikeBehaviors.removeFirst()
        switch behavior {
        case .success(let result):
            return result
        case .failure(let error):
            throw error
        case .delayed(let result, let delay):
            try await Task.sleep(for: .seconds(delay))
            return result
        }
    }
}
