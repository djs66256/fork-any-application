import Foundation
import Testing
@testable import ShortDrama

struct APIClientTests {

    // MARK: - Test Models

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

    // MARK: - Helper

    private func makeSession(
        handler: @escaping URLProtocolMock.RequestHandler
    ) -> URLSession {
        URLProtocolMock.handler = handler
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [URLProtocolMock.self]
        return URLSession(configuration: config)
    }

    /// Asserts that an async closure throws a specific APIError case.
    private func expectAPIError(
        _ operation: () async throws -> some Any,
        expectedCase: @escaping (APIError) -> Bool,
        sourceLocation: SourceLocation = #_sourceLocation
    ) async {
        do {
            _ = try await operation()
            Issue.record("Expected APIError but no error thrown", sourceLocation: sourceLocation)
        } catch let error as APIError {
            #expect(
                expectedCase(error),
                "Unexpected APIError case: \(error)",
                sourceLocation: sourceLocation
            )
        } catch {
            return
        }
    }

    // MARK: - T-11: GET returns 200 success

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

    // MARK: - T-12: 501 Not Implemented

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

    // MARK: - T-13: Network error

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

    // MARK: - Server error

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
