import Foundation
@testable import ShortDrama
import Testing

struct MessageRepositoryTests {
    private func makeSession(handler: @escaping URLProtocolMock.RequestHandler) -> URLSession {
        URLProtocolMock.handler = handler
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [URLProtocolMock.self]
        return URLSession(configuration: config)
    }

    @Test("T-01: message repository maps preview and list payloads")
    func testRepositoryMapsPreviewAndLists() async throws {
        let previewBody = """
        {
          "title": "系统通知",
          "summary": "你关注的剧集已更新第 12 集。",
          "relative_time": "2小时前"
        }
        """

        var requestCount = 0
        let session = makeSession { request in
            requestCount += 1
            let requestURL = try #require(request.url)
            switch requestCount {
            case 1:
                let response = try #require(HTTPURLResponse(url: requestURL, statusCode: 200, httpVersion: nil, headerFields: nil))
                return (response, Data(previewBody.utf8))
            default:
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
                let response = try #require(HTTPURLResponse(url: requestURL, statusCode: 200, httpVersion: nil, headerFields: nil))
                return (response, Data(body.utf8))
            }
        }

        let repository = MessageRepository(
            dataSource: MessageRemoteDataSource(client: APIClient(session: session, baseURL: "https://api.example.com"))
        )

        let preview = try await repository.fetchPreview()
        let systemMessages = try await repository.fetchSystemMessages(page: 1, pageSize: 20)

        #expect(preview?.summary == "你关注的剧集已更新第 12 集。")
        #expect(systemMessages.items.count == 1)
        #expect(systemMessages.items[0].id == "550e8400-e29b-41d4-a716-446655440001")
    }

    @Test("T-01: message repository maps preview 204 to nil")
    func testRepositoryMapsPreview204ToNil() async throws {
        let session = makeSession { request in
            let requestURL = try #require(request.url)
            let response = try #require(HTTPURLResponse(url: requestURL, statusCode: 204, httpVersion: nil, headerFields: nil))
            return (response, Data())
        }

        let repository = MessageRepository(
            dataSource: MessageRemoteDataSource(client: APIClient(session: session, baseURL: "https://api.example.com"))
        )

        let preview = try await repository.fetchPreview()

        #expect(preview == nil)
    }
}
