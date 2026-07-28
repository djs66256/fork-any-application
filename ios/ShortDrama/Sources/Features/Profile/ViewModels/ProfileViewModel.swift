import Foundation

@MainActor
final class ProfileViewModel: ObservableObject {
    enum ViewState: Equatable {
        case anonymous
        case restoring
        case authenticated(AuthUser)
    }

    @Published private(set) var viewState: ViewState = .anonymous

    func update(status: AuthStatus) {
        switch status {
        case .anonymous, .expired:
            viewState = .anonymous
        case .restoring:
            viewState = .restoring
        case .authenticated(let session), .refreshing(let session):
            viewState = .authenticated(session.user)
        }
    }
}
