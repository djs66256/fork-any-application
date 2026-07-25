import Foundation
import Testing
@testable import ShortDrama

struct DramaRepositoryTests {

    private func makeDramaPayload() -> String {
        """
        {
          "data": [
            {
              "id": "drama-001",
              "title": "示例短剧",
              "description": "首页卡片描述",
              "cover_url": "https://example.com/cover.jpg",
              "category": "都市",
              "episode_count": 12,
              "tags": ["逆袭", "甜宠"],
              "rating": 8.6,
              "created_at": "2026-07-25T00:00:00Z",
              "updated_at": "2026-07-25T00:00:00Z"
            }
          ],
          "pagination": {
            "page": 1,
            "page_size": 10,
            "total": 20,
            "total_pages": 2
          }
        }
        """
    }

    private func makeSession(handler: @escaping URLProtocolMock.RequestHandler) -> URLSession {
        URLProtocolMock.handler = handler
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [URLProtocolMock.self]
        return URLSession(configuration: config)
    }

    @Test("T-02: DramaRepository maps canonical response to entities")
    func testFetchDramasMapsCanonicalResponse() async throws {
        let url = URL(string: "https://api.example.com/api/dramas?page=1&pageSize=10")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/dramas")
            let response = HTTPURLResponse(
                url: url,
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            )!
            return (response, Data(self.makeDramaPayload().utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session)
        let dataSource = DramaRemoteDataSource(client: client)
        let repository = DramaRepository(dataSource: dataSource)

        let dramas = try await repository.fetchDramas(page: 1, pageSize: 10)

        #expect(dramas.count == 1)
        #expect(dramas[0].id == "drama-001")
        #expect(dramas[0].title == "示例短剧")
        #expect(dramas[0].episodeCount == 12)
        #expect(dramas[0].tags == ["逆袭", "甜宠"])
    }

    @Test("T-25: MockDramaRepository returns empty array on empty response")
    func testMockFetchDramasEmptySuccess() async throws {
        let mock = MockDramaRepository()
        mock.behavior = .success([])

        let dramas = try await mock.fetchDramas(page: 1, pageSize: 20)
        #expect(dramas.isEmpty)
    }

    @Test("T-25: MockDramaRepository returns correct drama count")
    func testMockFetchDramasReturnsData() async throws {
        let drama = Drama(
            id: "1",
            title: "Test",
            description: "Desc",
            coverUrl: "https://example.com/cover.jpg",
            category: "comedy",
            episodeCount: 12,
            tags: nil,
            rating: nil,
            createdAt: "2026-01-01T00:00:00Z",
            updatedAt: "2026-01-01T00:00:00Z"
        )
        let mock = MockDramaRepository()
        mock.behavior = .success([drama])

        let dramas = try await mock.fetchDramas(page: 1, pageSize: 20)
        #expect(dramas.count == 1)
        #expect(dramas[0].id == "1")
    }

    @Test("T-26: MockDramaRepository propagates notImplemented errors")
    func testMockFetchDramasErrorPropagation() async {
        let mock = MockDramaRepository()
        mock.behavior = .failure(.notImplemented("Service unavailable"))

        do {
            _ = try await mock.fetchDramas(page: 1, pageSize: 20)
            Issue.record("Expected error but none thrown")
        } catch let error as APIError {
            if case .notImplemented(let message) = error {
                #expect(message == "Service unavailable")
            } else {
                Issue.record("Expected notImplemented, got \(error)")
            }
        } catch {
            Issue.record("Unexpected error: \(error)")
        }
    }

    @Test("T-26: MockDramaRepository propagates network errors")
    func testMockFetchDramasNetworkErrorPropagation() async {
        let mock = MockDramaRepository()
        let underlying = URLError(.notConnectedToInternet)
        mock.behavior = .failure(.network(underlying: underlying))

        do {
            _ = try await mock.fetchDramas(page: 1, pageSize: 20)
            Issue.record("Expected error but none thrown")
        } catch let error as APIError {
            if case .network = error {
                // Expected
            } else {
                Issue.record("Expected network error, got \(error)")
            }
        } catch {
            Issue.record("Unexpected error: \(error)")
        }
    }
}
