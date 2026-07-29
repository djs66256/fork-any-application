import Foundation

final class CheckInRemoteDataSource: @unchecked Sendable {
    private let client: APIClient

    init(client: APIClient = .shared) {
        self.client = client
    }

    func fetchStatus(installationId: String?, accessToken: String?) async throws -> SignInStatusDTO {
        try await client.request(CheckInEndpoints.GetStatus(installationId: installationId, accessToken: accessToken))
    }

    func submitCheckIn(installationId: String?, accessToken: String?) async throws -> SignInStatusDTO {
        try await client.request(CheckInEndpoints.Submit(installationId: installationId, accessToken: accessToken))
    }
}

enum CheckInEndpoints {
    struct GetStatus: APIEndpoint {
        typealias Response = SignInStatusDTO

        let installationId: String?
        let accessToken: String?

        var path: String { "/api/check-ins/status" }
        var method: HTTPMethod { .get }
        var headers: [String: String] { CheckInEndpoints.makeHeaders(installationId: installationId, accessToken: accessToken) }
    }

    struct Submit: APIEndpoint {
        typealias Response = SignInStatusDTO

        let installationId: String?
        let accessToken: String?

        var path: String { "/api/check-ins" }
        var method: HTTPMethod { .post }
        var headers: [String: String] { CheckInEndpoints.makeHeaders(installationId: installationId, accessToken: accessToken) }
    }

    private static func makeHeaders(installationId: String?, accessToken: String?) -> [String: String] {
        var headers: [String: String] = [:]
        if let installationId, !installationId.isEmpty {
            headers["X-Installation-Id"] = installationId
        }
        if let accessToken, !accessToken.isEmpty {
            headers["Authorization"] = "Bearer \(accessToken)"
        }
        return headers
    }
}
