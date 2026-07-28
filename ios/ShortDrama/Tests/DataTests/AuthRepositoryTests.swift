import Foundation
@testable import ShortDrama
import Testing

struct AuthRepositoryTests {

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

    @Test("auth repository maps send-otp response to entity")
    func testSendOtpMapsToEntity() async throws {
        let url = URL(string: "https://api.example.com/api/auth/otp-requests")!
        let handler: URLProtocolMock.RequestHandler = { _ in
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
        let repository = AuthRepository(
            dataSource: AuthRemoteDataSource(client: APIClient(session: session))
        )

        let result = try await repository.sendOtp(phone: "13800138000", countryCode: "+86", scene: "login")

        #expect(result == SendOtpResult(requestId: "otp_req_xxx", cooldownSeconds: 60, expiresInSeconds: 300))
    }

    @Test("auth repository maps create-session response to entity")
    func testCreateSessionMapsToEntity() async throws {
        let url = URL(string: "https://api.example.com/api/auth/sessions")!
        let handler: URLProtocolMock.RequestHandler = { _ in
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
        let repository = AuthRepository(
            dataSource: AuthRemoteDataSource(client: APIClient(session: session))
        )

        let sessionEntity = try await repository.createSession(
            phone: "13800138000",
            countryCode: "+86",
            code: "123456"
        )

        #expect(sessionEntity.accessToken == "access-token")
        #expect(sessionEntity.refreshToken == "refresh-token")
        #expect(sessionEntity.expiresAt == "2026-07-28T12:34:56Z")
        #expect(sessionEntity.user == AuthUser(
            id: "550e8400-e29b-41d4-a716-446655440001",
            phone: "138****8000",
            displayName: nil,
            avatarURL: nil,
            role: "viewer",
            isNewUser: true
        ))
    }

    @Test("auth repository maps refresh-session response to entity")
    func testRefreshSessionMapsToEntity() async throws {
        let url = URL(string: "https://api.example.com/api/auth/session-refreshes")!
        let handler: URLProtocolMock.RequestHandler = { _ in
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
                  "displayName": "已登录用户",
                  "avatarUrl": "https://example.com/avatar.png",
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
        let repository = AuthRepository(
            dataSource: AuthRemoteDataSource(client: APIClient(session: session))
        )

        let sessionEntity = try await repository.refreshSession(refreshToken: "refresh-token")

        #expect(sessionEntity.accessToken == "new-access-token")
        #expect(sessionEntity.user.displayName == "已登录用户")
        #expect(sessionEntity.user.avatarURL == "https://example.com/avatar.png")
        #expect(sessionEntity.user.isNewUser == false)
    }

    @Test("auth repository maps current-user response to entity")
    func testGetCurrentUserMapsToEntity() async throws {
        let url = URL(string: "https://api.example.com/api/users/me")!
        let handler: URLProtocolMock.RequestHandler = { _ in
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
        let repository = AuthRepository(
            dataSource: AuthRemoteDataSource(client: APIClient(session: session))
        )

        let user = try await repository.getCurrentUser(accessToken: "access-token")

        #expect(user == AuthUser(
            id: "550e8400-e29b-41d4-a716-446655440001",
            phone: "138****8000",
            displayName: nil,
            avatarURL: nil,
            role: "viewer",
            isNewUser: false
        ))
    }

    @Test("auth repository logout passes through success")
    func testLogoutPassesThroughSuccess() async throws {
        let url = URL(string: "https://api.example.com/api/auth/session")!
        let handler: URLProtocolMock.RequestHandler = { request in
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
        let repository = AuthRepository(
            dataSource: AuthRemoteDataSource(client: APIClient(session: session))
        )

        try await repository.logout(accessToken: "access-token")
    }

    @Test("auth repository preserves business errors")
    func testRepositoryPreservesBusinessErrors() async {
        let url = URL(string: "https://api.example.com/api/auth/session-refreshes")!
        let handler: URLProtocolMock.RequestHandler = { _ in
            let response = try self.makeResponse(url: url, statusCode: 401)
            let body = """
            {"error":{"code":"AUTH_REFRESH_EXPIRED","message":"登录态已失效，请重新登录"}}
            """
            return (response, Data(body.utf8))
        }

        let session = makeSession(handler: handler)
        let repository = AuthRepository(
            dataSource: AuthRemoteDataSource(client: APIClient(session: session))
        )

        do {
            _ = try await repository.refreshSession(refreshToken: "refresh-token")
            Issue.record("Expected business error but none thrown")
        } catch let error as APIError {
            if case .business(let statusCode, let businessCode, let message) = error {
                #expect(statusCode == 401)
                #expect(businessCode == "AUTH_REFRESH_EXPIRED")
                #expect(message == "登录态已失效，请重新登录")
            } else {
                Issue.record("Expected business error, got \(error)")
            }
        } catch {
            Issue.record("Unexpected error type: \(error)")
        }
    }
}
