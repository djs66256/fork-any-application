import Foundation

final class MessageRemoteDataSource: @unchecked Sendable {
    private let client: APIClient

    init(client: APIClient = .shared) {
        self.client = client
    }

    func fetchPreview() async throws -> MessagePreviewDTO? {
        do {
            return try await client.request(MessageEndpoints.Preview())
        } catch let error as APIError where error.statusCode == 204 {
            return nil
        }
    }

    func fetchSystemMessages(page: Int, pageSize: Int) async throws -> SystemMessageListResponseDTO {
        try await client.request(MessageEndpoints.SystemMessages(page: page, pageSize: pageSize))
    }

    func fetchInteractionMessages(page: Int, pageSize: Int, accessToken: String) async throws -> InteractionMessageListResponseDTO {
        try await client.request(
            MessageEndpoints.InteractionMessages(page: page, pageSize: pageSize, accessToken: accessToken)
        )
    }
}

enum MessageEndpoints {
    struct Preview: APIEndpoint {
        typealias Response = MessagePreviewDTO

        var path: String { "/api/messages/preview" }
        var method: HTTPMethod { .get }
    }

    struct SystemMessages: APIEndpoint {
        typealias Response = SystemMessageListResponseDTO

        let page: Int
        let pageSize: Int

        var path: String { "/api/messages/system" }
        var method: HTTPMethod { .get }
        var queryItems: [URLQueryItem]? {
            [
                URLQueryItem(name: "page", value: String(page)),
                URLQueryItem(name: "pageSize", value: String(pageSize))
            ]
        }
    }

    struct InteractionMessages: APIEndpoint {
        typealias Response = InteractionMessageListResponseDTO

        let page: Int
        let pageSize: Int
        let accessToken: String

        var path: String { "/api/messages/interactions" }
        var method: HTTPMethod { .get }
        var queryItems: [URLQueryItem]? {
            [
                URLQueryItem(name: "page", value: String(page)),
                URLQueryItem(name: "pageSize", value: String(pageSize))
            ]
        }
        var headers: [String: String] {
            ["Authorization": "Bearer \(accessToken)"]
        }
    }
}
