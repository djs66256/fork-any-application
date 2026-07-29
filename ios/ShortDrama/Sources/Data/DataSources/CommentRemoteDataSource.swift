import Foundation

final class CommentRemoteDataSource: @unchecked Sendable {
    private let client: APIClient

    init(client: APIClient = .shared) {
        self.client = client
    }

    func fetchComments(query: CommentQuery) async throws -> CommentListResponseDTO {
        try await client.request(CommentGetListEndpoint(query: query))
    }

    func createComment(dramaId: String, content: String) async throws -> CommentDTO {
        try await client.request(
            CommentCreateEndpoint(
                dramaId: dramaId,
                request: CreateCommentRequestDTO(content: content)
            )
        )
    }

    func toggleLike(dramaId: String, commentId: String) async throws -> ToggleCommentLikeResponseDTO {
        try await client.request(CommentToggleLikeEndpoint(dramaId: dramaId, commentId: commentId))
    }
}

private struct CommentGetListEndpoint: APIEndpoint {
    typealias Response = CommentListResponseDTO

    let query: CommentQuery

    var path: String { "/api/dramas/\(query.dramaId)/comments" }
    var method: HTTPMethod { .get }
    var queryItems: [URLQueryItem]? {
        [
            URLQueryItem(name: "page", value: String(query.page)),
            URLQueryItem(name: "pageSize", value: String(query.pageSize)),
            URLQueryItem(name: "sort", value: query.sort.rawValue)
        ]
    }
}

private struct CommentCreateEndpoint: APIEndpoint {
    typealias Response = CommentDTO

    let dramaId: String
    let request: CreateCommentRequestDTO

    var path: String { "/api/dramas/\(dramaId)/comments" }
    var method: HTTPMethod { .post }
    var body: Encodable? { request }
}

private struct CommentToggleLikeEndpoint: APIEndpoint {
    typealias Response = ToggleCommentLikeResponseDTO

    let dramaId: String
    let commentId: String

    var path: String { "/api/dramas/\(dramaId)/comments/\(commentId)/like" }
    var method: HTTPMethod { .post }
}
