import Foundation
@testable import ShortDrama
import Testing

struct CommentRemoteDataSourceTests {
    private func makeSession(handler: @escaping URLProtocolMock.RequestHandler) -> URLSession {
        URLProtocolMock.handler = handler
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [URLProtocolMock.self]
        return URLSession(configuration: config)
    }

    @Test("T-01: comments list request sends contract query and decodes pagination")
    func testFetchComments() async throws {
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
            "page": 2,
            "page_size": 20,
            "total": 36,
            "total_pages": 2
          }
        }
        """

        let session = makeSession { request in
            #expect(request.httpMethod == "GET")
            #expect(request.url?.path == "/api/dramas/drama-001/comments")
            #expect(request.url?.query == "page=2&pageSize=20&sort=hot")
            let requestURL = try #require(request.url)
            let response = try #require(
                HTTPURLResponse(url: requestURL, statusCode: 200, httpVersion: nil, headerFields: nil)
            )
            return (response, Data(responseBody.utf8))
        }

        let dataSource = CommentRemoteDataSource(client: APIClient(session: session))
        let response = try await dataSource.fetchComments(
            query: CommentQuery(dramaId: "drama-001", page: 2, pageSize: 20, sort: .hot)
        )

        #expect(response.data.count == 1)
        #expect(response.data[0].id == "comment-001")
        #expect(response.data[0].user.displayName == "Alice")
        #expect(response.pagination.page == 2)
        #expect(response.pagination.pageSize == 20)
        #expect(response.pagination.total == 36)
        #expect(response.pagination.totalPages == 2)
    }

    @Test("T-01: create comment request sends snake_case body")
    func testCreateComment() async throws {
        let responseBody = """
        {
          "id": "comment-002",
          "drama_id": "drama-001",
          "content": "新评论",
          "like_count": 0,
          "liked": false,
          "created_at": "2026-07-29T09:31:00.000Z",
          "updated_at": "2026-07-29T09:31:00.000Z",
          "user": {
            "id": "user-002",
            "display_name": "Bob",
            "avatar_url": "https://example.com/avatar.jpg"
          }
        }
        """

        let session = makeSession { request in
            #expect(request.httpMethod == "POST")
            #expect(request.url?.path == "/api/dramas/drama-001/comments")
            let body = try #require(request.httpBody)
            let decoder = JSONDecoder()
            decoder.keyDecodingStrategy = .convertFromSnakeCase
            let decoded = try decoder.decode(CreateCommentRequestDTO.self, from: body)
            #expect(decoded.content == "新评论")
            let requestURL = try #require(request.url)
            let response = try #require(
                HTTPURLResponse(url: requestURL, statusCode: 200, httpVersion: nil, headerFields: nil)
            )
            return (response, Data(responseBody.utf8))
        }

        let dataSource = CommentRemoteDataSource(client: APIClient(session: session))
        let response = try await dataSource.createComment(dramaId: "drama-001", content: "新评论")

        #expect(response.id == "comment-002")
        #expect(response.content == "新评论")
        #expect(response.user.avatarUrl == "https://example.com/avatar.jpg")
    }

    @Test("T-01: toggle like request targets nested like endpoint")
    func testToggleLike() async throws {
        let responseBody = """
        {
          "comment_id": "comment-001",
          "liked": false,
          "like_count": 11
        }
        """

        let session = makeSession { request in
            #expect(request.httpMethod == "POST")
            #expect(request.url?.path == "/api/dramas/drama-001/comments/comment-001/like")
            #expect(request.httpBody == nil)
            let requestURL = try #require(request.url)
            let response = try #require(
                HTTPURLResponse(url: requestURL, statusCode: 200, httpVersion: nil, headerFields: nil)
            )
            return (response, Data(responseBody.utf8))
        }

        let dataSource = CommentRemoteDataSource(client: APIClient(session: session))
        let response = try await dataSource.toggleLike(dramaId: "drama-001", commentId: "comment-001")

        #expect(response.commentId == "comment-001")
        #expect(response.liked == false)
        #expect(response.likeCount == 11)
    }
}
