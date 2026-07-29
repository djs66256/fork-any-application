import Foundation
@testable import ShortDrama
import Testing

private struct APIClientTestResponse: Decodable, Equatable {
    let code: Int
    let data: APIClientTestData
}

private struct APIClientTestData: Decodable, Equatable {
    let id: String
    let title: String
}

private struct APIClientTestGetEndpoint: APIEndpoint {
    typealias Response = APIClientTestResponse

    var path: String
    var method: HTTPMethod { .get }
}

struct APIClientTests {

    private func makeSession(
        handler: @escaping URLProtocolMock.RequestHandler
    ) -> URLSession {
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

    @Test("T-11: APIClient GET returns 200 success and decodes response")
    func testGetSuccess() async throws {
        let json = """
        {"code":0,"data":{"id":"1","title":"Test"}}
        """
        let url = URL(string: "https://api.example.com/test")!
        let handler: URLProtocolMock.RequestHandler = { _ in
            let response = try self.makeResponse(url: url, statusCode: 200)
            return (response, Data(json.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let endpoint = APIClientTestGetEndpoint(path: "/test")
        let result: APIClientTestResponse = try await client.request(endpoint)

        #expect(result.code == 0)
        #expect(result.data.id == "1")
        #expect(result.data.title == "Test")
    }

    @Test("T-12: APIClient throws notImplemented on 501 response")
    func test501NotImplemented() async throws {
        let json = """
        {"message":"该功能暂未实现"}
        """
        let url = URL(string: "https://api.example.com/test")!
        let handler: URLProtocolMock.RequestHandler = { _ in
            let response = try self.makeResponse(url: url, statusCode: 501)
            return (response, Data(json.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let endpoint = APIClientTestGetEndpoint(path: "/test")

        do {
            let _: APIClientTestResponse = try await client.request(endpoint)
            Issue.record("Expected notImplemented error")
        } catch let error as APIError {
            if case .notImplemented(let message) = error {
                #expect(message == "该功能暂未实现")
            } else {
                Issue.record("Expected notImplemented, got \(error)")
            }
        } catch {
            Issue.record("Unexpected error type: \(error)")
        }
    }

    @Test("T-13: APIClient wraps network errors as APIError.network")
    func testNetworkErrorWrapping() async throws {
        let handler: URLProtocolMock.RequestHandler = { _ in
            throw URLError(.notConnectedToInternet)
        }
        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let endpoint = APIClientTestGetEndpoint(path: "/test")

        do {
            let _: APIClientTestResponse = try await client.request(endpoint)
            Issue.record("Expected error but none thrown")
        } catch let error as APIError {
            if case .network = error {
            } else {
                Issue.record("Expected network, got \(error)")
            }
        } catch {
            Issue.record("Unexpected error type: \(error)")
        }
    }

    @Test("T-01: Drama endpoint uses canonical path and query")
    func testDramaEndpointUsesCanonicalContract() {
        let endpoint = DramaEndpoints.getDramas(page: 1, pageSize: 10)

        #expect(endpoint.path == "/api/dramas")
        #expect(endpoint.queryItems?.count == 2)
        #expect(endpoint.queryItems?.contains(URLQueryItem(name: "page", value: "1")) == true)
        #expect(endpoint.queryItems?.contains(URLQueryItem(name: "pageSize", value: "10")) == true)
        #expect(endpoint.queryItems?.contains(URLQueryItem(name: "page_size", value: "10")) == false)
    }

    @Test("search endpoint uses canonical path and query")
    func testSearchEndpointUsesCanonicalContract() {
        let endpoint = DramaEndpoints.searchDramas(query: "逆袭", page: 1, pageSize: 10)

        #expect(endpoint.path == "/api/dramas/search")
        #expect(endpoint.queryItems?.count == 3)
        #expect(endpoint.queryItems?.contains(URLQueryItem(name: "q", value: "逆袭")) == true)
        #expect(endpoint.queryItems?.contains(URLQueryItem(name: "page", value: "1")) == true)
        #expect(endpoint.queryItems?.contains(URLQueryItem(name: "pageSize", value: "10")) == true)
    }

    @Test("hot search endpoint uses canonical path")
    func testHotSearchEndpointUsesCanonicalContract() {
        let endpoint = DramaEndpoints.getHotSearches()

        #expect(endpoint.path == "/api/dramas/hot-search")
        #expect(endpoint.queryItems == nil)
    }

    @Test("classification endpoint uses canonical path and gender query")
    func testClassificationEndpointUsesCanonicalContract() {
        let endpoint = DramaEndpoints.getClassificationTags(gender: .female)

        #expect(endpoint.path == "/api/dramas/tags")
        #expect(endpoint.method == .get)
        #expect(endpoint.queryItems?.contains(URLQueryItem(name: "gender", value: "female")) == true)
    }

    @Test("ranking endpoint uses canonical path and query")
    func testRankingEndpointUsesCanonicalContract() {
        let endpoint = DramaEndpoints.getRankings(
            query: RankingQuery(type: .hot, contentType: .all, page: 1, pageSize: 10)
        )

        #expect(endpoint.path == "/api/dramas/rankings")
        #expect(endpoint.queryItems?.contains(URLQueryItem(name: "type", value: "hot")) == true)
        #expect(endpoint.queryItems?.contains(URLQueryItem(name: "contentType", value: "all")) == true)
        #expect(endpoint.queryItems?.contains(URLQueryItem(name: "page", value: "1")) == true)
        #expect(endpoint.queryItems?.contains(URLQueryItem(name: "pageSize", value: "10")) == true)
    }

    @Test("booking endpoint uses canonical path and post method")
    func testBookingEndpointUsesCanonicalContract() {
        let endpoint = DramaEndpoints.bookDrama(id: "drama-001")

        #expect(endpoint.path == "/api/dramas/drama-001/book")
        #expect(endpoint.method == .post)
        #expect(endpoint.queryItems == nil)
    }

    @Test("T-02: Drama list response decodes canonical payload")
    func testDramaListResponseDecoding() async throws {
        let json = """
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
        let url = URL(string: "https://api.example.com/api/dramas?page=1&pageSize=10")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/dramas")
            #expect(request.url?.query?.contains("page=1") == true)
            #expect(request.url?.query?.contains("pageSize=10") == true)
            let response = try self.makeResponse(url: url, statusCode: 200)
            return (response, Data(json.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let endpoint = DramaEndpoints.getDramas(page: 1, pageSize: 10)
        let response: DramaListResponse = try await client.request(endpoint)

        #expect(response.data.count == 1)
        #expect(response.data.first?.id == "drama-001")
        #expect(response.pagination.page == 1)
        #expect(response.pagination.pageSize == 10)
        #expect(response.pagination.total == 20)
        #expect(response.pagination.totalPages == 2)
    }

    @Test("search response decodes canonical payload")
    func testSearchResponseDecoding() async throws {
        let json = """
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
        let url = URL(string: "https://api.example.com/api/dramas/search?q=%E9%80%86%E8%A2%AD&page=1&pageSize=10")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/dramas/search")
            #expect(request.url?.query?.contains("q=%E9%80%86%E8%A2%AD") == true)
            #expect(request.url?.query?.contains("page=1") == true)
            #expect(request.url?.query?.contains("pageSize=10") == true)
            let response = try self.makeResponse(url: url, statusCode: 200)
            return (response, Data(json.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let endpoint = DramaEndpoints.searchDramas(query: "逆袭", page: 1, pageSize: 10)
        let response: DramaListResponse = try await client.request(endpoint)

        #expect(response.data.count == 1)
        #expect(response.data.first?.id == "search-001")
        #expect(response.data.first?.title == "逆袭归来后我成了豪门团宠")
    }

    @Test("hot search response decodes canonical payload")
    func testHotSearchResponseDecoding() async throws {
        let json = """
        {
          "data": [
            { "rank": 1, "keyword": "逆袭", "score": 9821 },
            { "rank": 2, "keyword": "豪门", "score": 9540 }
          ]
        }
        """
        let url = URL(string: "https://api.example.com/api/dramas/hot-search")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/dramas/hot-search")
            let response = try self.makeResponse(url: url, statusCode: 200)
            return (response, Data(json.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let endpoint = DramaEndpoints.getHotSearches()
        let response: HotSearchListResponseDTO = try await client.request(endpoint)

        #expect(response.data.count == 2)
        #expect(response.data.first?.rank == 1)
        #expect(response.data.first?.keyword == "逆袭")
        #expect(response.data.first?.score == 9821)
    }

    @Test("classification response decodes canonical payload")
    func testClassificationResponseDecoding() async throws {
        let json = """
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
                "key": "theme_plot",
                "name": "主题情节",
                "tags": []
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
        let url = URL(string: "https://api.example.com/api/dramas/tags?gender=female")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/dramas/tags")
            #expect(request.url?.query?.contains("gender=female") == true)
            let response = try self.makeResponse(url: url, statusCode: 200)
            return (response, Data(json.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let endpoint = DramaEndpoints.getClassificationTags(gender: .female)
        let response: ClassificationTagsResponseDTO = try await client.request(endpoint)

        #expect(response.data.gender == .female)
        #expect(response.data.dimensions.count == 3)
        #expect(response.data.dimensions[1].key == .themePlot)
        #expect(response.data.dimensions[1].tags.isEmpty)
    }

    @Test("ranking response decodes canonical payload")
    func testRankingResponseDecoding() async throws {
        let json = """
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
        let url = URL(string: "https://api.example.com/api/dramas/rankings?type=hot&contentType=all&page=1&pageSize=10")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/dramas/rankings")
            #expect(request.url?.query?.contains("type=hot") == true)
            #expect(request.url?.query?.contains("contentType=all") == true)
            let response = try self.makeResponse(url: url, statusCode: 200)
            return (response, Data(json.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let endpoint = DramaEndpoints.getRankings(
            query: RankingQuery(type: .hot, contentType: .all, page: 1, pageSize: 10)
        )
        let response: RankingListResponseDTO = try await client.request(endpoint)

        #expect(response.data.count == 1)
        #expect(response.data.first?.id == "ranking-001")
        #expect(response.data.first?.contentType == .liveAction)
        #expect(response.pagination.totalPages == 2)
    }

    @Test("booking response decodes canonical payload")
    func testBookingResponseDecoding() async throws {
        let json = """
        {
          "drama_id": "ranking-001",
          "booked": true,
          "booking_count": 821
        }
        """
        let url = URL(string: "https://api.example.com/api/dramas/ranking-001/book")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/dramas/ranking-001/book")
            #expect(request.httpMethod == "POST")
            let response = try self.makeResponse(url: url, statusCode: 200)
            return (response, Data(json.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let endpoint = DramaEndpoints.bookDrama(id: "ranking-001")
        let response: BookDramaResponseDTO = try await client.request(endpoint)

        #expect(response.dramaID == "ranking-001")
        #expect(response.booked == true)
        #expect(response.bookingCount == 821)
    }

    @Test("APIClient throws server error on 400 response")
    func testServerError() async throws {
        let json = """
        {"message":"Bad request"}
        """
        let url = URL(string: "https://api.example.com/test")!
        let handler: URLProtocolMock.RequestHandler = { _ in
            let response = try self.makeResponse(url: url, statusCode: 400)
            return (response, Data(json.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let endpoint = APIClientTestGetEndpoint(path: "/test")

        do {
            let _: APIClientTestResponse = try await client.request(endpoint)
            Issue.record("Expected error but none thrown")
        } catch let error as APIError {
            if case .server(let code, let message) = error {
                #expect(code == 400)
                #expect(message == "Bad request")
            } else {
                Issue.record("Expected server error, got \(error)")
            }
        } catch {
            Issue.record("Unexpected error type: \(error)")
        }
    }

    @Test("APIClient decodes nested error payload")
    func testNestedErrorPayload() async throws {
        let json = """
        {"error":{"code":"VALIDATION_ERROR","message":"输入内容无效，请检查后重试"}}
        """
        let url = URL(string: "https://api.example.com/api/dramas/search?q=%20")!
        let handler: URLProtocolMock.RequestHandler = { _ in
            let response = try self.makeResponse(url: url, statusCode: 400)
            return (response, Data(json.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let endpoint = DramaEndpoints.searchDramas(query: " ", page: 1, pageSize: 10)

        do {
            let _: DramaListResponse = try await client.request(endpoint)
            Issue.record("Expected server error but none thrown")
        } catch let error as APIError {
            if case .server(let code, let message) = error {
                #expect(code == 400)
                #expect(message == "输入内容无效，请检查后重试")
            } else {
                Issue.record("Expected server error, got \(error)")
            }
        } catch {
            Issue.record("Unexpected error type: \(error)")
        }
    }
}
