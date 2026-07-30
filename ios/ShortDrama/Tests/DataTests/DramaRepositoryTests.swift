import Foundation
@testable import ShortDrama
import Testing

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

private func makeClassificationPayload() -> String {
    """
    {
      "data": {
        "gender": "female",
        "dimensions": [
          {
            "key": "era_background",
            "name": "时代背景",
            "tags": ["古言", "都市"]
          },
          {
            "key": "character_setting",
            "name": "角色设定",
            "tags": ["大女主"]
          }
        ]
      }
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

private func makeBookingAssetsPayload() -> String {
    """
    {
      "data": [
        {
          "drama_id": "drama-001",
          "title": "逆袭归来后我成了豪门团宠",
          "cover_url": "https://example.com/booking.jpg",
          "episode_count": 68,
          "booked_at": "2026-07-30T03:25:00.000Z",
          "availability_status": "online"
        }
      ],
      "pagination": {
        "page": 2,
        "page_size": 20,
        "total": 21,
        "total_pages": 2
      },
      "summary": {
        "online_count": 8,
        "upcoming_count": 3
      }
    }
    """
}

private func makeTheaterPayload() -> String {
    """
    {
      "data": [
        {
          "id": "theater-001",
          "title": "逆袭归来后我成了豪门团宠",
          "description": "剧场结果",
          "cover_url": "https://example.com/theater.jpg",
          "category": "都市",
          "episode_count": 68,
          "tags": ["逆袭", "豪门"],
          "rating": 8.9,
          "created_at": "2026-07-25T00:00:00Z",
          "updated_at": "2026-07-25T00:00:00Z",
          "heat": 98210
        }
      ],
      "pagination": {
        "page": 1,
        "page_size": 20,
        "total": 12,
        "total_pages": 3
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

struct DramaRepositoryTests {

    @Test("T-02: DramaRepository maps canonical response to entities")
    func testFetchDramasMapsCanonicalResponse() async throws {
        let url = URL(string: "https://api.example.com/api/dramas?page=1&pageSize=10")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/dramas")
            let response = try makeResponse(url: url, statusCode: 200)
            return (response, Data(makeDramaPayload().utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
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
            let response = try makeResponse(url: url, statusCode: 200)
            return (response, Data(makeSearchPayload().utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
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
            let response = try makeResponse(url: url, statusCode: 200)
            return (response, Data(makeHotSearchPayload().utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let dataSource = DramaRemoteDataSource(client: client)
        let repository = DramaRepository(dataSource: dataSource)

        let items = try await repository.fetchHotSearches()

        #expect(items.count == 2)
        #expect(items[0].rank == 1)
        #expect(items[0].keyword == "逆袭")
        #expect(items[0].score == 9821)
    }

    @Test("classification repository keeps fixed order and empty dimensions")
    func testFetchClassificationTagsMapsCanonicalResponse() async throws {
        let url = URL(string: "https://api.example.com/api/dramas/tags?gender=female")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/dramas/tags")
            #expect(request.url?.query?.contains("gender=female") == true)
            let response = try makeResponse(url: url, statusCode: 200)
            return (response, Data(makeClassificationPayload().utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let dataSource = DramaRemoteDataSource(client: client)
        let repository = DramaRepository(dataSource: dataSource)

        let payload = try await repository.fetchClassificationTags(gender: .female)

        #expect(payload.gender == .female)
        #expect(payload.dimensions.map(\.key) == ClassificationDimensionKey.allCases)
        #expect(payload.dimensions[0].tags == ["古言", "都市"])
        #expect(payload.dimensions[1].tags.isEmpty)
        #expect(payload.dimensions[1].name == "主题情节")
        #expect(payload.dimensions[2].tags == ["大女主"])
    }

    @Test("ranking repository maps ranking response to entities")
    func testFetchRankingsMapsCanonicalResponse() async throws {
        let url = URL(
            string: "https://api.example.com/api/dramas/rankings?type=hot&contentType=all&page=1&pageSize=10"
        )!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/dramas/rankings")
            #expect(request.url?.query?.contains("type=hot") == true)
            #expect(request.url?.query?.contains("contentType=all") == true)
            let response = try makeResponse(url: url, statusCode: 200)
            return (response, Data(makeRankingPayload().utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
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

    @Test("booking assets repository maps protected response to entity page")
    func testFetchBookingAssetsMapsCanonicalResponse() async throws {
        let url = URL(
            string: "https://api.example.com/api/users/me/bookings?status=upcoming&page=2&pageSize=20"
        )!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/users/me/bookings")
            #expect(request.url?.query?.contains("status=upcoming") == true)
            #expect(request.url?.query?.contains("page=2") == true)
            #expect(request.url?.query?.contains("pageSize=20") == true)
            #expect(request.value(forHTTPHeaderField: "Authorization") == "Bearer token-001")
            let response = try makeResponse(url: url, statusCode: 200)
            return (response, Data(makeBookingAssetsPayload().utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let dataSource = DramaRemoteDataSource(client: client)
        let repository = DramaRepository(dataSource: dataSource)

        let result = try await repository.fetchBookingAssets(
            query: BookingAssetQuery(status: .upcoming, page: 2, pageSize: 20),
            accessToken: "token-001"
        )

        #expect(result.items.count == 1)
        #expect(result.items[0].dramaID == "drama-001")
        #expect(result.items[0].availabilityStatus == .online)
        #expect(result.page == 2)
        #expect(result.pageSize == 20)
        #expect(result.total == 21)
        #expect(result.totalPages == 2)
        #expect(result.summary == BookingAssetSummary(onlineCount: 8, upcomingCount: 3))
    }

    @Test("T-01: theater repository maps channel response to entities")
    func testFetchTheaterFeedMapsCanonicalResponse() async throws {
        let url = URL(string: "https://api.example.com/api/dramas/channel?channel=all&page=1&pageSize=20")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/dramas/channel")
            #expect(request.url?.query?.contains("channel=all") == true)
            #expect(request.url?.query?.contains("page=1") == true)
            #expect(request.url?.query?.contains("pageSize=20") == true)
            let response = try makeResponse(url: url, statusCode: 200)
            return (response, Data(makeTheaterPayload().utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let dataSource = DramaRemoteDataSource(client: client)
        let repository = DramaRepository(dataSource: dataSource)

        let result = try await repository.fetchTheaterFeed(
            query: TheaterFeedQuery(channel: .all, page: 1, pageSize: 20)
        )

        #expect(result.channel == .all)
        #expect(result.items.count == 1)
        #expect(result.page == 1)
        #expect(result.totalPages == 3)
        #expect(result.items[0].id == "theater-001")
        #expect(result.items[0].heat == 98210)
        #expect(result.items[0].coverUrl == "https://example.com/theater.jpg")
    }

    @Test("booking repository maps booking response to entity")
    func testBookDramaMapsCanonicalResponse() async throws {
        let url = URL(string: "https://api.example.com/api/dramas/ranking-001/book")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/dramas/ranking-001/book")
            #expect(request.httpMethod == "POST")
            let response = try makeResponse(url: url, statusCode: 200)
            return (response, Data(makeBookingPayload().utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let dataSource = DramaRemoteDataSource(client: client)
        let repository = DramaRepository(dataSource: dataSource)

        let result = try await repository.bookDrama(id: "ranking-001")

        #expect(result.dramaID == "ranking-001")
        #expect(result.booked == true)
        #expect(result.bookingCount == 821)
    }
}
