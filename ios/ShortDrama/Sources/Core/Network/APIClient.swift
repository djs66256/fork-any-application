import Foundation

/// Shared API client using URLSession for network requests.
final class APIClient: @unchecked Sendable {

    /// Shared singleton instance.
    static let shared = APIClient()

    private let session: URLSession
    private let baseURL: String

    private let decoder: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        return decoder
    }()

    private init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 15
        config.timeoutIntervalForResource = 30
        self.session = URLSession(configuration: config)
        self.baseURL = AppConfig.apiBaseURL()
    }

    /// Internal initializer for testing with custom URLSession.
    init(session: URLSession, baseURL: String = "https://api.example.com") {
        self.session = session
        self.baseURL = baseURL
    }

    /// Performs a type-safe API request.
    ///
    /// - Parameter endpoint: The API endpoint definition.
    /// - Returns: Decoded response of type T.
    func request<T: Decodable>(_ endpoint: some APIEndpoint) async throws -> T {
        guard var components = URLComponents(string: baseURL) else {
            throw APIError.invalidURL
        }
        components.path = endpoint.path
        components.queryItems = endpoint.queryItems

        guard let url = components.url else {
            throw APIError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = endpoint.method.rawValue

        if let body = endpoint.body {
            let encoder = JSONEncoder()
            encoder.keyEncodingStrategy = .convertToSnakeCase
            request.httpBody = try encoder.encode(body)
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }

        return try await perform(request: request)
    }

    // MARK: - Private

    private func perform<T: Decodable>(request: URLRequest) async throws -> T {
        let (data, response): (Data, URLResponse)
        do {
            (data, response) = try await session.data(for: request)
        } catch let error as URLError where error.code == .cancelled {
            throw APIError.cancelled
        } catch let error as URLError {
            throw APIError.network(underlying: error)
        } catch {
            throw APIError.network(underlying: error)
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }

        switch httpResponse.statusCode {
        case 200...299:
            do {
                return try decoder.decode(T.self, from: data)
            } catch {
                throw APIError.decodingFailed(error)
            }
        case 501:
            // Attempt to parse error response body
            if let errorResponse = try? decoder.decode(ErrorResponse.self, from: data) {
                throw APIError.notImplemented(errorResponse.message)
            }
            throw APIError.notImplemented("该功能暂未实现")
        default:
            if let errorResponse = try? decoder.decode(ErrorResponse.self, from: data) {
                throw APIError.server(code: httpResponse.statusCode, message: errorResponse.message)
            }
            throw APIError.server(
                code: httpResponse.statusCode,
                message: "请求失败（\(httpResponse.statusCode)）"
            )
        }
    }
}

/// Generic error response structure.
private struct ErrorResponse: Decodable {
    let message: String

    enum CodingKeys: String, CodingKey {
        case message
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        message = try container.decodeIfPresent(String.self, forKey: .message) ?? "未知错误"
    }
}
