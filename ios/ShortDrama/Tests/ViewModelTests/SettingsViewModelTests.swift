import Foundation
import Testing
@testable import ShortDrama

@MainActor
struct SettingsViewModelTests {
    @Test("settings view model presents confirmation before logout")
    func testRequestLogoutPresentsConfirmation() {
        let viewModel = SettingsViewModel(logoutAction: {})

        viewModel.requestLogout()

        #expect(viewModel.isLogoutConfirmPresented == true)
        #expect(viewModel.viewState == .idle)
    }

    @Test("settings view model resets to idle after successful logout")
    func testConfirmLogoutSuccess() async {
        var callCount = 0
        let viewModel = SettingsViewModel {
            callCount += 1
        }
        viewModel.requestLogout()

        let success = await viewModel.confirmLogout()

        #expect(success == true)
        #expect(callCount == 1)
        #expect(viewModel.isLogoutConfirmPresented == false)
        #expect(viewModel.viewState == .idle)
    }

    @Test("settings view model exposes error when logout fails")
    func testConfirmLogoutFailure() async {
        let viewModel = SettingsViewModel {
            throw APIError.server(code: 500, message: "退出登录失败")
        }
        viewModel.requestLogout()

        let success = await viewModel.confirmLogout()

        #expect(success == false)
        #expect(viewModel.viewState == .error("退出登录失败"))
    }
}
