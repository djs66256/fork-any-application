import Foundation

enum AuthStatus: Equatable, Sendable {
    case anonymous
    case restoring
    case authenticated(AuthSession)
    case refreshing(AuthSession)
    case expired

    var currentSession: AuthSession? {
        switch self {
        case .authenticated(let session), .refreshing(let session):
            return session
        case .anonymous, .restoring, .expired:
            return nil
        }
    }
}
