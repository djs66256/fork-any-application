import Foundation

/// Unified error model for API and network errors.
enum APIError: LocalizedError, Equatable {

    case invalidURL
    case invalidResponse
    case decodingFailed(Error)
    case business(statusCode: Int, businessCode: String, message: String)
    case server(code: Int, message: String)
    case network(underlying: Error)
    case notImplemented(String)
    case cancelled

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "无效的请求地址"
        case .invalidResponse:
            return "服务器响应异常"
        case .decodingFailed(let error):
            return "数据解析失败：\(error.localizedDescription)"
        case .business(_, _, let message):
            return message
        case .server(_, let message):
            return message
        case .network(let error):
            return "网络请求失败：\(error.localizedDescription)"
        case .notImplemented(let message):
            return message
        case .cancelled:
            return "请求已取消"
        }
    }

    var statusCode: Int? {
        switch self {
        case .business(let statusCode, _, _):
            return statusCode
        case .server(let code, _):
            return code
        default:
            return nil
        }
    }

    var businessCode: String? {
        guard case .business(_, let businessCode, _) = self else {
            return nil
        }

        return businessCode
    }

    static func == (lhs: APIError, rhs: APIError) -> Bool {
        switch (lhs, rhs) {
        case (.invalidURL, .invalidURL),
             (.invalidResponse, .invalidResponse),
             (.cancelled, .cancelled):
            return true
        case (.decodingFailed, .decodingFailed):
            return true
        case (.network, .network):
            return true
        case (.business(let lStatus, let lCode, let lMsg), .business(let rStatus, let rCode, let rMsg)):
            return lStatus == rStatus && lCode == rCode && lMsg == rMsg
        case (.server(let lCode, let lMsg), .server(let rCode, let rMsg)):
            return lCode == rCode && lMsg == rMsg
        case (.notImplemented(let lMsg), .notImplemented(let rMsg)):
            return lMsg == rMsg
        default:
            return false
        }
    }
}
