import Foundation

@MainActor
final class SettingsViewModel: ObservableObject {
    enum ViewState: Equatable {
        case idle
        case loggingOut
        case error(String)
    }

    @Published private(set) var viewState: ViewState = .idle
    @Published var isLogoutConfirmPresented = false

    private let logoutAction: @MainActor @Sendable () async throws -> Void

    init(logoutAction: @escaping @MainActor @Sendable () async throws -> Void) {
        self.logoutAction = logoutAction
    }

    var isLoggingOut: Bool {
        viewState == .loggingOut
    }

    func requestLogout() {
        isLogoutConfirmPresented = true
    }

    func cancelLogout() {
        isLogoutConfirmPresented = false
    }

    func confirmLogout() async -> Bool {
        guard !isLoggingOut else { return false }

        isLogoutConfirmPresented = false
        viewState = .loggingOut

        do {
            try await logoutAction()
            viewState = .idle
            return true
        } catch let error as APIError {
            viewState = .error(error.errorDescription ?? "退出登录失败，请稍后重试")
            return false
        } catch {
            viewState = .error(error.localizedDescription)
            return false
        }
    }
}
