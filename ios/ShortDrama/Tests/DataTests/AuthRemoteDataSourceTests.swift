import Foundation
@testable import ShortDrama
import Testing

struct AuthRemoteDataSourceTests {

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

    @Test("auth send-otp data source uses canonical contract")
    func testSendOtpUsesCanonicalContract() async throws {
        let url = URL(string: "https://api.example.com/api/auth/otp-requests")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/auth/otp-requests")
            #expect(request.httpMethod == "POST")
            #expect(request.value(forHTTPHeaderField: "Content-Type") == "application/json")
            let requestBodyData = try #require(request.httpBody)
            let requestBody = try #require(
                JSONSerialization.jsonObject(with: requestBodyData) as? [String: String]
            )
            #expect(requestBody == [
                "countryCode": "+86",
                "phone": "13800138000",
                "scene": "login"
            ])

            let response = try self.makeResponse(url: url, statusCode: 200)
            let body = """
            {
              "code": 0,
              "data": {
                "requestId": "otp_req_xxx",
                "cooldownSeconds": 60,
                "expiresInSeconds": 300
              },
              "message": "ok"
            }
            """
            return (response, Data(body.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let dataSource = AuthRemoteDataSource(client: client)

        let response = try await dataSource.sendOtp(phone: "13800138000", countryCode: "+86", scene: "login")

        #expect(response.code == 0)
        #expect(response.data.requestId == "otp_req_xxx")
        #expect(response.data.cooldownSeconds == 60)
        #expect(response.data.expiresInSeconds == 300)
    }

    @Test("auth create-session data source uses canonical contract")
    func testCreateSessionUsesCanonicalContract() async throws {
        let url = URL(string: "https://api.example.com/api/auth/sessions")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/auth/sessions")
            #expect(request.httpMethod == "POST")
            let requestBodyData = try #require(request.httpBody)
            let requestBody = try #require(
                JSONSerialization.jsonObject(with: requestBodyData) as? [String: String]
            )
            #expect(requestBody == [
                "countryCode": "+86",
                "phone": "13800138000",
                "code": "123456"
            ])

            let response = try self.makeResponse(url: url, statusCode: 200)
            let body = """
            {
              "code": 0,
              "data": {
                "accessToken": "access-token",
                "refreshToken": "refresh-token",
                "expiresAt": "2026-07-28T12:34:56Z",
                "user": {
                  "id": "550e8400-e29b-41d4-a716-446655440001",
                  "phone": "138****8000",
                  "displayName": null,
                  "avatarUrl": null,
                  "role": "viewer",
                  "isNewUser": true
                }
              },
              "message": "ok"
            }
            """
            return (response, Data(body.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let dataSource = AuthRemoteDataSource(client: client)

        let response = try await dataSource.createSession(
            phone: "13800138000",
            countryCode: "+86",
            code: "123456"
        )

        #expect(response.data.accessToken == "access-token")
        #expect(response.data.refreshToken == "refresh-token")
        #expect(response.data.user.id == "550e8400-e29b-41d4-a716-446655440001")
        #expect(response.data.user.isNewUser == true)
    }

    @Test("auth refresh-session data source uses canonical contract")
    func testRefreshSessionUsesCanonicalContract() async throws {
        let url = URL(string: "https://api.example.com/api/auth/session-refreshes")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/auth/session-refreshes")
            #expect(request.httpMethod == "POST")
            let requestBodyData = try #require(request.httpBody)
            let requestBody = try #require(
                JSONSerialization.jsonObject(with: requestBodyData) as? [String: String]
            )
            #expect(requestBody == [
                "refreshToken": "refresh-token"
            ])

            let response = try self.makeResponse(url: url, statusCode: 200)
            let body = """
            {
              "code": 0,
              "data": {
                "accessToken": "new-access-token",
                "refreshToken": "new-refresh-token",
                "expiresAt": "2026-07-28T13:34:56Z",
                "user": {
                  "id": "550e8400-e29b-41d4-a716-446655440001",
                  "phone": "138****8000",
                  "displayName": null,
                  "avatarUrl": null,
                  "role": "viewer",
                  "isNewUser": false
                }
              },
              "message": "ok"
            }
            """
            return (response, Data(body.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let dataSource = AuthRemoteDataSource(client: client)

        let response = try await dataSource.refreshSession(refreshToken: "refresh-token")

        #expect(response.data.accessToken == "new-access-token")
        #expect(response.data.refreshToken == "new-refresh-token")
        #expect(response.data.user.isNewUser == false)
    }

    @Test("auth current-user data source adds bearer authorization header")
    func testGetCurrentUserUsesBearerAuthorization() async throws {
        let url = URL(string: "https://api.example.com/api/users/me")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/users/me")
            #expect(request.httpMethod == "GET")
            #expect(request.value(forHTTPHeaderField: "Authorization") == "Bearer access-token")

            let response = try self.makeResponse(url: url, statusCode: 200)
            let body = """
            {
              "code": 0,
              "data": {
                "id": "550e8400-e29b-41d4-a716-446655440001",
                "phone": "138****8000",
                "displayName": null,
                "avatarUrl": null,
                "role": "viewer",
                "isNewUser": false
              },
              "message": "ok"
            }
            """
            return (response, Data(body.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let dataSource = AuthRemoteDataSource(client: client)

        let response = try await dataSource.getCurrentUser(accessToken: "access-token")

        #expect(response.data.id == "550e8400-e29b-41d4-a716-446655440001")
        #expect(response.data.role == "viewer")
    }

    @Test("auth logout data source sends delete with bearer authorization")
    func testLogoutUsesDeleteWithAuthorization() async throws {
        let url = URL(string: "https://api.example.com/api/auth/session")!
        let handler: URLProtocolMock.RequestHandler = { request in
            #expect(request.url?.path == "/api/auth/session")
            #expect(request.httpMethod == "DELETE")
            #expect(request.value(forHTTPHeaderField: "Authorization") == "Bearer access-token")

            let response = try self.makeResponse(url: url, statusCode: 200)
            let body = """
            {
              "code": 0,
              "data": {},
              "message": "ok"
            }
            """
            return (response, Data(body.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let dataSource = AuthRemoteDataSource(client: client)

        try await dataSource.logout(accessToken: "access-token")
    }

    @Test("auth data source preserves business errors from APIClient")
    func testCreateSessionPreservesBusinessError() async {
        let url = URL(string: "https://api.example.com/api/auth/sessions")!
        let handler: URLProtocolMock.RequestHandler = { _ in
            let response = try self.makeResponse(url: url, statusCode: 400)
            let body = """
            {"error":{"code":"AUTH_INVALID_CODE","message":"验证码错误，请重新输入"}}
            """
            return (response, Data(body.utf8))
        }

        let session = makeSession(handler: handler)
        let client = APIClient(session: session, baseURL: "https://api.example.com")
        let dataSource = AuthRemoteDataSource(client: client)

        do {
            _ = try await dataSource.createSession(
                phone: "13800138000",
                countryCode: "+86",
                code: "123456"
            )
            Issue.record("Expected business error but none thrown")
        } catch let error as APIError {
            if case .business(let statusCode, let businessCode, let message) = error {
                #expect(statusCode == 400)
                #expect(businessCode == "AUTH_INVALID_CODE")
                #expect(message == "验证码错误，请重新输入")
            } else {
                Issue.record("Expected business error, got \(error)")
            }
        } catch {
            Issue.record("Unexpected error type: \(error)")
        }
    }
}
