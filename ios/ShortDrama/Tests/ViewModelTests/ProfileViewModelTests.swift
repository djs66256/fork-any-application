import Foundation
import Testing
@testable import ShortDrama

@MainActor
struct ProfileViewModelTests {
    private func makeSession() -> AuthSession {
        AuthSession(
            accessToken: "access-token",
            refreshToken: "refresh-token",
            expiresAt: "2026-07-28T12:34:56Z",
            user: AuthUser(
                id: "user-001",
                phone: "13800138000",
                displayName: "测试用户",
                avatarURL: nil,
                role: "viewer",
                isNewUser: false
            )
        )
    }

    @Test("profile view model maps anonymous status")
    func testAnonymousStatus() {
        let viewModel = ProfileViewModel()

        viewModel.update(status: .anonymous)

        #expect(viewModel.viewState == .anonymous)
    }

    @Test("profile view model maps restoring status")
    func testRestoringStatus() {
        let viewModel = ProfileViewModel()

        viewModel.update(status: .restoring)

        #expect(viewModel.viewState == .restoring)
    }

    @Test("profile view model maps authenticated status to user summary")
    func testAuthenticatedStatus() {
        let viewModel = ProfileViewModel()
        let session = makeSession()

        viewModel.update(status: .authenticated(session))

        #expect(viewModel.viewState == .authenticated(session.user))
    }
}
