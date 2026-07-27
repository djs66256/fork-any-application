import Foundation
@testable import ShortDrama
import Testing

struct MenuPanelRepositoryTests {
    private func makeSession(handler: @escaping URLProtocolMock.RequestHandler) -> URLSession {
        URLProtocolMock.handler = handler
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [URLProtocolMock.self]
        return URLSession(configuration: config)
    }

    @Test("T-05: repository maps recently viewed payload including null cover")
    func testRepositoryMapsRecentlyViewedPayload() async throws {
        let responseBody = """
        {
          "code": 0,
          "data": {
            "items": [
              {
                "drama_id": "drama-001",
                "title": "逆袭归来后我成了豪门团宠",
                "cover_url": null,
                "episode_number": 12,
                "progress": 128.5,
                "updated_at": "2026-07-27T15:20:00.000Z"
              }
            ]
          },
          "message": "ok"
        }
        """

        let session = makeSession { request in
            #expect(request.httpMethod == "GET")
            #expect(request.url?.path == "/api/player/recently-viewed")
            #expect(request.value(forHTTPHeaderField: "X-Playback-Session-Id") == "session-001")
            let requestURL = try #require(request.url)
            let response = try #require(
                HTTPURLResponse(
                    url: requestURL,
                    statusCode: 200,
                    httpVersion: nil,
                    headerFields: nil
                )
            )
            return (response, Data(responseBody.utf8))
        }

        let repository = MenuPanelRepository(
            dataSource: PlayerRemoteDataSource(client: APIClient(session: session))
        )

        let items = try await repository.fetchRecentlyViewed(playbackSessionId: "session-001")

        #expect(items.count == 1)
        #expect(items[0].dramaId == "drama-001")
        #expect(items[0].coverURL == nil)
        #expect(items[0].episodeNumber == 12)
        #expect(items[0].progress == 128.5)
    }

    @Test("T-05: repository forwards API errors")
    func testRepositoryForwardsErrors() async {
        let session = makeSession { request in
            let requestURL = try #require(request.url)
            let response = try #require(
                HTTPURLResponse(
                    url: requestURL,
                    statusCode: 503,
                    httpVersion: nil,
                    headerFields: nil
                )
            )
            let body = "{\"error\": {\"code\": \"SERVICE_UNAVAILABLE\", \"message\": \"服务暂不可用，请稍后重试\"}}"
            return (response, Data(body.utf8))
        }

        let repository = MenuPanelRepository(
            dataSource: PlayerRemoteDataSource(client: APIClient(session: session))
        )

        await #expect(throws: APIError.self) {
            _ = try await repository.fetchRecentlyViewed(playbackSessionId: "session-001")
        }
    }
}
