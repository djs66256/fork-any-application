import Foundation
@testable import ShortDrama
import Testing

struct MessageRemoteDataSourceTests {
    private func makeSession(handler: @escaping URLProtocolMock.RequestHandler) -> URLSession {
        URLProtocolMock.handler = handler
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [URLProtocolMock.self]
        return URLSession(configuration: config)
    }

    @Test("T-01: preview request decodes latest message summary")
    func testFetchPreview() async throws {
        let body = """
        {
          "title": "系统通知",
          "summary": "你关注的剧集已更新第 12 集。",
          "relative_time": "2小时前"
        }
        """

        let session = makeSession { request in
            #expect(request.httpMethod == "GET")
            #expect(request.url?.path == "/api/messages/preview")
            let requestURL = try #require(request.url)
            let response = try #require(HTTPURLResponse(url: requestURL, statusCode: 200, httpVersion: nil, headerFields: nil))
            return (response, Data(body.utf8))
        }

        let dataSource = MessageRemoteDataSource(client: APIClient(session: session, baseURL: "https://api.example.com"))
        let preview = try await dataSource.fetchPreview()

        #expect(preview?.title == "系统通知")
        #expect(preview?.relativeTime == "2小时前")
    }

    @Test("T-01: preview 204 is handled as empty state")
    func testFetchPreview204ReturnsNil() async throws {
        let session = makeSession { request in
            let requestURL = try #require(request.url)
            let response = try #require(HTTPURLResponse(url: requestURL, statusCode: 204, httpVersion: nil, headerFields: nil))
            return (response, Data())
        }

        let dataSource = MessageRemoteDataSource(client: APIClient(session: session, baseURL: "https://api.example.com"))
        let preview = try await dataSource.fetchPreview()

        #expect(preview == nil)
    }

    @Test("T-01: system messages request sends page query")
    func testFetchSystemMessages() async throws {
        let body = """
        {
          "data": [
            {
              "id": "550e8400-e29b-41d4-a716-446655440001",
              "title": "系统通知",
              "summary": "你关注的剧集已更新第 12 集。",
              "sent_at": "2026-07-29T08:00:00.000Z"
            }
          ],
          "pagination": {
            "page": 1,
            "page_size": 20,
            "total": 1,
            "total_pages": 1
          }
        }
        """

        let session = makeSession { request in
            #expect(request.url?.path == "/api/messages/system")
            #expect(request.url?.query == "page=1&pageSize=20")
            let requestURL = try #require(request.url)
            let response = try #require(HTTPURLResponse(url: requestURL, statusCode: 200, httpVersion: nil, headerFields: nil))
            return (response, Data(body.utf8))
        }

        let dataSource = MessageRemoteDataSource(client: APIClient(session: session, baseURL: "https://api.example.com"))
        let response = try await dataSource.fetchSystemMessages(page: 1, pageSize: 20)

        #expect(response.data.count == 1)
        #expect(response.pagination.pageSize == 20)
    }

    @Test("T-01: interaction messages request sends authorization header")
    func testFetchInteractionMessages() async throws {
        let body = """
        {
          "data": [
            {
              "id": "660e8400-e29b-41d4-a716-446655440010",
              "type": "comment_reply",
              "title": "有人回复了你的评论",
              "summary": "“这集反转真不错” 收到一条新回复。",
              "sent_at": "2026-07-29T09:00:00.000Z"
            }
          ],
          "pagination": {
            "page": 1,
            "page_size": 20,
            "total": 1,
            "total_pages": 1
          }
        }
        """

        let session = makeSession { request in
            #expect(request.url?.path == "/api/messages/interactions")
            #expect(request.url?.query == "page=1&pageSize=20")
            #expect(request.value(forHTTPHeaderField: "Authorization") == "Bearer access-token")
            let requestURL = try #require(request.url)
            let response = try #require(HTTPURLResponse(url: requestURL, statusCode: 200, httpVersion: nil, headerFields: nil))
            return (response, Data(body.utf8))
        }

        let dataSource = MessageRemoteDataSource(client: APIClient(session: session, baseURL: "https://api.example.com"))
        let response = try await dataSource.fetchInteractionMessages(page: 1, pageSize: 20, accessToken: "access-token")

        #expect(response.data.first?.type == .commentReply)
        #expect(response.pagination.totalPages == 1)
    }
}
