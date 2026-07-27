import Foundation
@testable import ShortDrama
import Testing

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

    private func makeSearchPayload() -> String {
        """
        {
          "data": [
            {
              "id": "search-001",
              "title": "逆袭归来后我成了豪门团宠",
              "description": "搜索命中结果",
              "cover_url": "https://example.com/search.jpg",
              "category": "都市",
              "episode_count": 68,
              "tags": ["逆袭", "豪门"],
              "rating": 8.9,
              "created_at": "2026-07-25T00:00:00Z",
              "updated_at": "2026-07-25T00:00:00Z"
            }
          ],
          "pagination": {
            "page": 1,
            "page_size": 10,
            "total": 1,
            "total_pages": 1
          }
        }
        """
    }

    private func makeHotSearchPayload() -> String {
        """
        {
          "data": [
            { "rank": 1, "keyword": "逆袭", "score": 9821 },
            { "rank": 2, "keyword": "豪门", "score": 9540 }
          ]
        }
        """
    }

    private func makeRankingPayload() -> String {
        """
        {
          "data": [
            {
              "id": "ranking-001",
              "title": "逆袭归来后我成了豪门团宠",
              "description": "排行榜结果",
              "cover_url": "https://example.com/ranking.jpg",
              "category": "都市",
              "episode_count": 68,
              "tags": ["逆袭", "豪门"],
              "rating": 8.9,
              "created_at": "2026-07-25T00:00:00Z",
              "updated_at": "2026-07-25T00:00:00Z",
              "content_type": "live_action",
              "play_count": 98210,
              "booking_count": 820,
              "recommendation_score": 58930.6,
              "is_booked": false
            }
          ],
          "pagination": {
            "page": 1,
            "page_size": 10,
            "total": 12,
            "total_pages": 2
          }
        }
        """
    }

    private func makeBookingPayload() -> String {
        """
        {
          "drama_id": "ranking-001",
          "booked": true,
          "booking_count": 821
        }
        """
    }

    private func makeSession(handler: @escaping URLProtocolMock.RequestHandler) -> URLSession {
        URLProtocolMock.handler = handler
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [URLProtocolMock.self]
        return URLSession(configuration: config)
    }

    private func makeResponse(url: URL, statusCode: Int) throws -> HTTPURLResponse {
        guard let response = HTTPURLResponse(
            url: url,
            statusCode: statusCode,
            httpVersion: nil,
            headerFields: nil
        ) else {
            throw APIError.invalidResponse
        }

        return response
    }

    @Test("T-02: DramaRepository maps canonical response to entities")
    func testFetchDramasMapsCanonicalResponse() async throws {
        let url = URL(string: "https://api.example.com/api/dramas?page=1&pageSize=10")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/dramas")
            let response = try self.makeResponse(url: url, statusCode: 200)
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

    @Test("search repository maps search response to entities")
    func testSearchDramasMapsCanonicalResponse() async throws {
        let url = URL(string: "https://api.example.com/api/dramas/search?q=%E9%80%86%E8%A2%AD&page=1&pageSize=10")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/dramas/search")
            #expect(request.url?.query?.contains("q=%E9%80%86%E8%A2%AD") == true)
            let response = try self.makeResponse(url: url, statusCode: 200)
            return (response, Data(self.makeSearchPayload().utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session)
        let dataSource = DramaRemoteDataSource(client: client)
        let repository = DramaRepository(dataSource: dataSource)

        let dramas = try await repository.searchDramas(query: "逆袭", page: 1, pageSize: 10)

        #expect(dramas.count == 1)
        #expect(dramas[0].id == "search-001")
        #expect(dramas[0].title == "逆袭归来后我成了豪门团宠")
        #expect(dramas[0].category == "都市")
    }

    @Test("hot search repository maps response to entities")
    func testFetchHotSearchesMapsCanonicalResponse() async throws {
        let url = URL(string: "https://api.example.com/api/dramas/hot-search")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/dramas/hot-search")
            let response = try self.makeResponse(url: url, statusCode: 200)
            return (response, Data(self.makeHotSearchPayload().utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session)
        let dataSource = DramaRemoteDataSource(client: client)
        let repository = DramaRepository(dataSource: dataSource)

        let items = try await repository.fetchHotSearches()

        #expect(items.count == 2)
        #expect(items[0].rank == 1)
        #expect(items[0].keyword == "逆袭")
        #expect(items[0].score == 9821)
    }

    @Test("ranking repository maps ranking response to entities")
    func testFetchRankingsMapsCanonicalResponse() async throws {
        let url = URL(string: "https://api.example.com/api/dramas/rankings?type=hot&contentType=all&page=1&pageSize=10")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/dramas/rankings")
            #expect(request.url?.query?.contains("type=hot") == true)
            #expect(request.url?.query?.contains("contentType=all") == true)
            let response = try self.makeResponse(url: url, statusCode: 200)
            return (response, Data(self.makeRankingPayload().utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session)
        let dataSource = DramaRemoteDataSource(client: client)
        let repository = DramaRepository(dataSource: dataSource)

        let result = try await repository.fetchRankings(
            query: RankingQuery(type: .hot, contentType: .all, page: 1, pageSize: 10)
        )

        #expect(result.items.count == 1)
        #expect(result.page == 1)
        #expect(result.totalPages == 2)
        #expect(result.items[0].id == "ranking-001")
        #expect(result.items[0].contentType == .liveAction)
        #expect(result.items[0].playCount == 98210)
        #expect(result.items[0].recommendationScore == 58930.6)
    }

    @Test("booking repository maps booking response to entity")
    func testBookDramaMapsCanonicalResponse() async throws {
        let url = URL(string: "https://api.example.com/api/dramas/ranking-001/book")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/dramas/ranking-001/book")
            #expect(request.httpMethod == "POST")
            let response = try self.makeResponse(url: url, statusCode: 200)
            return (response, Data(self.makeBookingPayload().utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session)
        let dataSource = DramaRemoteDataSource(client: client)
        let repository = DramaRepository(dataSource: dataSource)

        let result = try await repository.bookDrama(id: "ranking-001")

        #expect(result.dramaID == "ranking-001")
        #expect(result.booked == true)
        #expect(result.bookingCount == 821)
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

    @Test("mock search repository tracks canonical query arguments")
    func testMockSearchTracksArguments() async throws {
        let mock = MockDramaRepository()
        let drama = Drama(
            id: "search-1",
            title: "逆袭",
            description: "",
            coverUrl: "https://example.com/cover.jpg",
            category: "都市",
            episodeCount: 8,
            tags: nil,
            rating: nil,
            createdAt: "2026-01-01T00:00:00Z",
            updatedAt: "2026-01-01T00:00:00Z"
        )
        mock.searchBehavior = .success([drama])

        let dramas = try await mock.searchDramas(query: "逆袭", page: 1, pageSize: 10)

        #expect(dramas.count == 1)
        #expect(mock.searchDramasCallCount == 1)
        #expect(mock.lastSearchQuery == "逆袭")
        #expect(mock.lastSearchPage == 1)
        #expect(mock.lastSearchPageSize == 10)
    }

    @Test("mock hot search repository returns configured items")
    func testMockFetchHotSearchesReturnsItems() async throws {
        let mock = MockDramaRepository()
        mock.hotSearchBehavior = .success([
            HotSearchItem(rank: 1, keyword: "逆袭", score: 9821)
        ])

        let items = try await mock.fetchHotSearches()

        #expect(items.count == 1)
        #expect(items[0].keyword == "逆袭")
        #expect(mock.fetchHotSearchesCallCount == 1)
    }

    @Test("mock ranking repository tracks query arguments")
    func testMockRankingTracksArguments() async throws {
        let mock = MockDramaRepository()
        let result = PagedResult(
            items: [
                RankingDrama(
                    id: "ranking-1",
                    title: "排行短剧",
                    description: "描述",
                    coverUrl: "https://example.com/cover.jpg",
                    category: "都市",
                    episodeCount: 8,
                    tags: nil,
                    rating: nil,
                    createdAt: "2026-01-01T00:00:00Z",
                    updatedAt: "2026-01-01T00:00:00Z",
                    contentType: .all,
                    playCount: 1,
                    bookingCount: 2,
                    recommendationScore: 3,
                    isBooked: false
                )
            ],
            page: 1,
            pageSize: 10,
            total: 1,
            totalPages: 1
        )
        mock.rankingBehavior = .success(result)

        let query = RankingQuery(type: .recommend, contentType: .ai, page: 2, pageSize: 10)
        let fetched = try await mock.fetchRankings(query: query)

        #expect(fetched == result)
        #expect(mock.fetchRankingsCallCount == 1)
        #expect(mock.lastRankingQuery == query)
    }

    @Test("mock booking repository tracks drama id")
    func testMockBookingTracksArguments() async throws {
        let mock = MockDramaRepository()
        mock.bookingBehavior = .success(
            BookDramaResult(dramaID: "ranking-1", booked: true, bookingCount: 9)
        )

        let result = try await mock.bookDrama(id: "ranking-1")

        #expect(result.dramaID == "ranking-1")
        #expect(result.bookingCount == 9)
        #expect(mock.bookDramaCallCount == 1)
        #expect(mock.lastBookedDramaID == "ranking-1")
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
            } else {
                Issue.record("Expected network error, got \(error)")
            }
        } catch {
            Issue.record("Unexpected error: \(error)")
        }
    }
}
