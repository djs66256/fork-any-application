import Foundation

/// HTTP methods supported by the API.
enum HTTPMethod: String {
    case get = "GET"
    case post = "POST"
    case delete = "DELETE"
}

/// Protocol for defining type-safe API endpoints.
protocol APIEndpoint {

    /// The expected response type that this endpoint returns.
    associatedtype Response: Decodable

    /// The path component (appended to base URL).
    var path: String { get }

    /// HTTP method for the request.
    var method: HTTPMethod { get }

    /// Optional query parameters.
    var queryItems: [URLQueryItem]? { get }

    /// Optional per-endpoint request headers.
    var headers: [String: String] { get }

    /// Optional request body.
    var body: Encodable? { get }

    /// Encoder used for the request body.
    var bodyEncoder: JSONEncoder { get }
}

// Default implementations for optional properties.
extension APIEndpoint {
    var queryItems: [URLQueryItem]? { nil }
    var headers: [String: String] { [:] }
    var body: Encodable? { nil }

    var bodyEncoder: JSONEncoder {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        return encoder
    }
}
