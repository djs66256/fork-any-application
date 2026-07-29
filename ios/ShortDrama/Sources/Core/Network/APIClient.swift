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
    convenience init(session: URLSession) {
        self.init(session: session, baseURL: AppConfig.apiBaseURL())
    }

    /// Internal initializer for testing with custom URLSession.
    init(session: URLSession, baseURL: String) {
        self.session = session
        self.baseURL = baseURL
    }

    /// Performs a type-safe API request.
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

        for (headerField, value) in endpoint.headers {
            request.setValue(value, forHTTPHeaderField: headerField)
        }

        if let body = endpoint.body {
            request.httpBody = try endpoint.bodyEncoder.encode(body)
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }

        return try await perform(request: request)
    }

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
            if httpResponse.statusCode == 204 {
                if let emptyResponse = EmptySuccessDTO() as? T {
                    return emptyResponse
                }
                throw APIError.server(code: 204, message: "No Content")
            }

            do {
                return try decoder.decode(T.self, from: data)
            } catch {
                throw APIError.decodingFailed(error)
            }
        case 501:
            if let errorResponse = try? decoder.decode(ErrorResponse.self, from: data) {
                throw APIError.notImplemented(errorResponse.message)
            }
            throw APIError.notImplemented("该功能暂未实现")
        default:
            if let errorResponse = try? decoder.decode(ErrorResponse.self, from: data) {
                if let businessCode = errorResponse.code {
                    throw APIError.business(
                        statusCode: httpResponse.statusCode,
                        businessCode: businessCode,
                        message: errorResponse.message
                    )
                }

                throw APIError.server(code: httpResponse.statusCode, message: errorResponse.message)
            }
            throw APIError.server(
                code: httpResponse.statusCode,
                message: "请求失败（\(httpResponse.statusCode)）"
            )
        }
    }
}

private struct ErrorResponse: Decodable {
    private struct NestedError: Decodable {
        let code: String?
        let message: String?
    }

    private struct FlatError: Decodable {
        let code: String?
        let message: String?
    }

    let code: String?
    let message: String

    init(from decoder: Decoder) throws {
        if let container = try? decoder.container(keyedBy: CodingKeys.self),
           let nested = try? container.decode(NestedError.self, forKey: .error) {
            code = nested.code
            message = nested.message ?? "未知错误"
            return
        }

        if let flat = try? FlatError(from: decoder) {
            code = flat.code
            message = flat.message ?? "未知错误"
            return
        }

        code = nil
        message = "未知错误"
    }

    private enum CodingKeys: String, CodingKey {
        case error
    }
}
