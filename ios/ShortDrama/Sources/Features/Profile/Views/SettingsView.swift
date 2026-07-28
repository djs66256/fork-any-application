import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var router: NavigationRouter
    @StateObject private var viewModel: SettingsViewModel

    init(logoutAction: @escaping @MainActor @Sendable () async throws -> Void) {
        _viewModel = StateObject(wrappedValue: SettingsViewModel(logoutAction: logoutAction))
    }

    var body: some View {
        List {
            Section {
                Button(role: .destructive) {
                    viewModel.requestLogout()
                } label: {
                    Text("退出登录")
                }
                .disabled(viewModel.isLoggingOut)
            } footer: {
                footerText
            }
        }
        .navigationTitle("设置")
        .navigationBarTitleDisplayMode(.inline)
        .confirmationDialog(
            "确认退出当前账号？",
            isPresented: $viewModel.isLogoutConfirmPresented,
            titleVisibility: .visible
        ) {
            Button("退出登录", role: .destructive) {
                Task {
                    if await viewModel.confirmLogout() {
                        router.popToRoot(of: .profile)
                    }
                }
            }
            Button("取消", role: .cancel) {
                viewModel.cancelLogout()
            }
        }
    }

    @ViewBuilder
    private var footerText: some View {
        switch viewModel.viewState {
        case .idle:
            Text("退出后将清除本机保存的登录状态。")
        case .loggingOut:
            Text("正在退出登录…")
        case .error(let message):
            Text(message)
                .foregroundStyle(.red)
        }
    }
}

#Preview {
    NavigationStack {
        SettingsView(logoutAction: {})
            .environmentObject(NavigationRouter())
    }
}
