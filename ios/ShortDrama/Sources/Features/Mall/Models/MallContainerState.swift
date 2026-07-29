import Foundation

enum MallContainerState: Equatable {
    case loading
    case success
    case error(message: String)

    var isRetryable: Bool {
        if case .error = self {
            return true
        }
        return false
    }
}
