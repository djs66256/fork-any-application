import Foundation
@testable import ShortDrama
import Testing

struct CommentRepositoryTests {
    private func makeSession(handler: @escaping URLProtocolMock.RequestHandler) -> URLSession {
        URLProtocolMock.handler = handler
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [URLProtocolMock.self]
        return URLSession(configuration: config)
    }

    @Test("T-02: repository maps fetch page into comment entities")
    func testRepositoryMapsFetchComments() async throws {
        let responseBody = """
        {
          "data": [
            {
              "id": "comment-001",
              "drama_id": "drama-001",
              "content": "第一条评论",
              "like_count": 12,
              "liked": true,
              "created_at": "2026-07-29T09:30:00.000Z",
              "updated_at": "2026-07-29T09:30:00.000Z",
              "user": {
                "id": "user-001",
                "display_name": "Alice",
                "avatar_url": null
              }
            }
          ],
          "pagination": {
            "page": 1,
            "page_size": 20,
            "total": 36,
            "total_pages": 2
          }
        }
        """

        let session = makeSession { request in
            #expect(request.url?.path == "/api/dramas/drama-001/comments")
            let requestURL = try #require(request.url)
            let response = try #require(
                HTTPURLResponse(url: requestURL, statusCode: 200, httpVersion: nil, headerFields: nil)
            )
            return (response, Data(responseBody.utf8))
        }

        let repository = CommentRepository(
            dataSource: CommentRemoteDataSource(client: APIClient(session: session))
        )

        let page = try await repository.fetchComments(
            query: CommentQuery(dramaId: "drama-001", page: 1, pageSize: 20, sort: .latest)
        )

        #expect(page.page == 1)
        #expect(page.pageSize == 20)
        #expect(page.total == 36)
        #expect(page.totalPages == 2)
        #expect(page.items.count == 1)
        #expect(page.items[0] == Comment(
            id: "comment-001",
            dramaId: "drama-001",
            content: "第一条评论",
            likeCount: 12,
            liked: true,
            createdAt: "2026-07-29T09:30:00.000Z",
            updatedAt: "2026-07-29T09:30:00.000Z",
            user: CommentUserSummary(id: "user-001", displayName: "Alice", avatarUrl: nil)
        ))
    }

    @Test("T-02: repository maps create and toggle like responses")
    func testRepositoryMapsCreateAndToggleLike() async throws {
        let createBody = """
        {
          "id": "comment-010",
          "drama_id": "drama-001",
          "content": "创建成功",
          "like_count": 0,
          "liked": false,
          "created_at": "2026-07-29T09:40:00.000Z",
          "updated_at": "2026-07-29T09:40:00.000Z",
          "user": {
            "id": "user-010",
            "display_name": "Created User",
            "avatar_url": null
          }
        }
        """
        let toggleBody = """
        {
          "comment_id": "comment-010",
          "liked": true,
          "like_count": 1
        }
        """

        let session = makeSession { request in
            let body: String
            switch request.url?.path {
            case "/api/dramas/drama-001/comments":
                let requestBody = try #require(request.httpBody)
                let decoder = JSONDecoder()
                decoder.keyDecodingStrategy = .convertFromSnakeCase
                let decoded = try decoder.decode(CreateCommentRequestDTO.self, from: requestBody)
                #expect(decoded.content == "创建成功")
                body = createBody
            case "/api/dramas/drama-001/comments/comment-010/like":
                body = toggleBody
            default:
                Issue.record("Unexpected path \(request.url?.path ?? "nil")")
                body = "{}"
            }
            let requestURL = try #require(request.url)
            let response = try #require(
                HTTPURLResponse(url: requestURL, statusCode: 200, httpVersion: nil, headerFields: nil)
            )
            return (response, Data(body.utf8))
        }

        let repository = CommentRepository(
            dataSource: CommentRemoteDataSource(client: APIClient(session: session))
        )

        let created = try await repository.createComment(dramaId: "drama-001", content: "创建成功")
        let toggled = try await repository.toggleCommentLike(dramaId: "drama-001", commentId: "comment-010")

        #expect(created.id == "comment-010")
        #expect(created.user.displayName == "Created User")
        #expect(toggled == ToggleCommentLikeResult(commentId: "comment-010", liked: true, likeCount: 1))
    }
}
