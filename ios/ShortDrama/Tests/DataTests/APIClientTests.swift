import Foundation
import Testing
@testable import ShortDrama

struct APIClientTests {

    private struct TestResponse: Decodable, Equatable {
        let code: Int
        let data: TestData
    }

    private struct TestData: Decodable, Equatable {
        let id: String
        let title: String
    }

    private struct TestGetEndpoint: APIEndpoint {
        typealias Response = TestResponse
        var path: String
        var method: HTTPMethod { .get }
    }

    private struct TestHeaderEndpoint: APIEndpoint {
        typealias Response = TestResponse

        let path: String
        let method: HTTPMethod
        let endpointHeaders: [String: String]

        var headers: [String: String] { endpointHeaders }
    }

    private struct TestMessageErrorEnvelope: Encodable {
        let message: String
    }

    private struct TestNestedErrorEnvelope: Encodable {
        let error: NestedError

        struct NestedError: Encodable {
            let code: String
            let message: String
        }
    }

    private func makeSession(
        handler: @escaping URLProtocolMock.RequestHandler
    ) -> URLSession {
        URLProtocolMock.handler = handler
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [URLProtocolMock.self]
        return URLSession(configuration: config)
    }

    @Test("T-11: APIClient GET returns 200 success and decodes response")
    func testGetSuccess() async throws {
        let json = """
        {"code":0,"data":{"id":"1","title":"Test"}}
        """
        let url = URL(string: "https://api.example.com/test")!
        let handler: URLProtocolMock.RequestHandler = { _ in
            let response = HTTPURLResponse(
                url: url,
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            )!
            return (response, Data(json.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session)
        let endpoint = TestGetEndpoint(path: "/test")
        let result: TestResponse = try await client.request(endpoint)

        #expect(result.code == 0)
        #expect(result.data.id == "1")
        #expect(result.data.title == "Test")
    }

    @Test("T-01: progress start and stop inject playback session header")
    func testPlayerEndpointsInjectPlaybackSessionHeader() async throws {
        let json = """
        {"code":0,"data":{"id":"1","title":"Test"}}
        """
        let expectedHeader = "session-123"
        let expectedPaths = ["/api/player/progress", "/api/player/start", "/api/player/stop"]
        var observedPaths: [String] = []

        let handler: URLProtocolMock.RequestHandler = { request in
            observedPaths.append(request.url?.path ?? "")
            #expect(request.value(forHTTPHeaderField: "X-Playback-Session-Id") == expectedHeader)
            let response = HTTPURLResponse(
                url: request.url!,
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            )!
            return (response, Data(json.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session)

        let endpoints = [
            TestHeaderEndpoint(
                path: "/api/player/progress",
                method: .get,
                endpointHeaders: ["X-Playback-Session-Id": expectedHeader]
            ),
            TestHeaderEndpoint(
                path: "/api/player/start",
                method: .post,
                endpointHeaders: ["X-Playback-Session-Id": expectedHeader]
            ),
            TestHeaderEndpoint(
                path: "/api/player/stop",
                method: .post,
                endpointHeaders: ["X-Playback-Session-Id": expectedHeader]
            )
        ]

        for endpoint in endpoints {
            let _: TestResponse = try await client.request(endpoint)
        }

        #expect(observedPaths == expectedPaths)
    }

    @Test("T-01: episodes endpoint does not inject playback session header")
    func testEpisodesEndpointDoesNotInjectPlaybackSessionHeader() async throws {
        let json = """
        {"code":0,"data":{"id":"1","title":"Test"}}
        """
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/dramas/drama-001/episodes")
            #expect(request.value(forHTTPHeaderField: "X-Playback-Session-Id") == nil)
            let response = HTTPURLResponse(
                url: request.url!,
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            )!
            return (response, Data(json.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session)
        let endpoint = TestHeaderEndpoint(
            path: "/api/dramas/drama-001/episodes",
            method: .get,
            endpointHeaders: [:]
        )

        let _: TestResponse = try await client.request(endpoint)
    }

    @Test("T-02: APIClient parses nested backend error envelope")
    func testNestedBackendErrorEnvelope() async throws {
        let payload = TestNestedErrorEnvelope(
            error: .init(code: "INVALID_PLAYBACK_SESSION", message: "播放身份异常，请重试")
        )
        let encoded = try JSONEncoder().encode(payload)
        let url = URL(string: "https://api.example.com/test")!
        let handler: URLProtocolMock.RequestHandler = { _ in
            let response = HTTPURLResponse(
                url: url,
                statusCode: 400,
                httpVersion: nil,
                headerFields: nil
            )!
            return (response, encoded)
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session)
        let endpoint = TestGetEndpoint(path: "/test")

        do {
            let _: TestResponse = try await client.request(endpoint)
            Issue.record("Expected server error but none thrown")
        } catch let error as APIError {
            #expect(error == .server(code: 400, message: "播放身份异常，请重试"))
        }
    }

    @Test("T-02: APIClient parses message-only backend error envelope")
    func testMessageOnlyBackendErrorEnvelope() async throws {
        let payload = TestMessageErrorEnvelope(message: "Bad request")
        let encoded = try JSONEncoder().encode(payload)
        let url = URL(string: "https://api.example.com/test")!
        let handler: URLProtocolMock.RequestHandler = { _ in
            let response = HTTPURLResponse(
                url: url,
                statusCode: 400,
                httpVersion: nil,
                headerFields: nil
            )!
            return (response, encoded)
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session)
        let endpoint = TestGetEndpoint(path: "/test")

        do {
            let _: TestResponse = try await client.request(endpoint)
            Issue.record("Expected server error but none thrown")
        } catch let error as APIError {
            #expect(error == .server(code: 400, message: "Bad request"))
        }
    }

    @Test("T-12: APIClient throws notImplemented on 501 response")
    func test501NotImplemented() async throws {
        let json = """
        {"message":"该功能暂未实现"}
        """
        let url = URL(string: "https://api.example.com/test")!
        let handler: URLProtocolMock.RequestHandler = { _ in
            let response = HTTPURLResponse(
                url: url,
                statusCode: 501,
                httpVersion: nil,
                headerFields: nil
            )!
            return (response, Data(json.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session)
        let endpoint = TestGetEndpoint(path: "/test")

        do {
            let _: TestResponse = try await client.request(endpoint)
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
        let client = APIClient(session: session)
        let endpoint = TestGetEndpoint(path: "/test")

        do {
            let _: TestResponse = try await client.request(endpoint)
            Issue.record("Expected error but none thrown")
        } catch let error as APIError {
            if case .network = error {
                // Expected
            } else {
                Issue.record("Expected network, got \(error)")
            }
        } catch {
            Issue.record("Unexpected error type: \(error)")
        }
    }

    @Test("T-01: Drama endpoint uses canonical path and query")
    func testDramaEndpointUsesCanonicalContract() {
        let endpoint = DramaEndpoints.GetDramas(page: 1, pageSize: 10)

        #expect(endpoint.path == "/api/dramas")
        #expect(endpoint.queryItems?.count == 2)
        #expect(endpoint.queryItems?.contains(URLQueryItem(name: "page", value: "1")) == true)
        #expect(endpoint.queryItems?.contains(URLQueryItem(name: "pageSize", value: "10")) == true)
        #expect(endpoint.queryItems?.contains(URLQueryItem(name: "page_size", value: "10")) == false)
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
            let response = HTTPURLResponse(
                url: url,
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            )!
            return (response, Data(json.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session)
        let endpoint = DramaEndpoints.GetDramas(page: 1, pageSize: 10)
        let response: DramaListResponse = try await client.request(endpoint)

        #expect(response.data.count == 1)
        #expect(response.data.first?.id == "drama-001")
        #expect(response.pagination.page == 1)
        #expect(response.pagination.pageSize == 10)
        #expect(response.pagination.total == 20)
        #expect(response.pagination.totalPages == 2)
    }

    @Test("APIClient throws server error on 400 response")
    func testServerError() async throws {
        let json = """
        {"message":"Bad request"}
        """
        let url = URL(string: "https://api.example.com/test")!
        let handler: URLProtocolMock.RequestHandler = { _ in
            let response = HTTPURLResponse(
                url: url,
                statusCode: 400,
                httpVersion: nil,
                headerFields: nil
            )!
            return (response, Data(json.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session)
        let endpoint = TestGetEndpoint(path: "/test")

        do {
            let _: TestResponse = try await client.request(endpoint)
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
}
