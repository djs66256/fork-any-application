import SwiftUI

struct ProfileHomeView: View {
    @EnvironmentObject private var router: NavigationRouter
    @EnvironmentObject private var authStore: AuthStore
    @StateObject private var viewModel = ProfileViewModel()

    var body: some View {
        Group {
            switch viewModel.viewState {
            case .anonymous:
                anonymousView
            case .restoring:
                loadingView
            case .authenticated(let user):
                authenticatedView(user: user)
            }
        }
        .navigationTitle("我的")
        .navigationBarTitleDisplayMode(.large)
        .task {
            viewModel.update(status: authStore.status)
        }
        .onReceive(authStore.$status) { status in
            viewModel.update(status: status)
        }
    }

    private var anonymousView: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.lg) {
            Text("登录后同步你的记录")
                .font(.title3)
                .fontWeight(.semibold)

            Text("登录后可查看你的账号信息、播放记录与预约状态。")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            Button("立即登录") {
                router.presentLogin(context: LoginInterceptionContext(source: .profileEntry))
            }
            .buttonStyle(.borderedProminent)

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .padding(DesignTokens.Spacing.lg)
    }

    private var loadingView: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            ProgressView()
            Text("正在恢复账号状态…")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(DesignTokens.Spacing.xl)
    }

    private func authenticatedView(user: AuthUser) -> some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.lg) {
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
                Text(user.displayName ?? maskedPhone(for: user.phone))
                    .font(.title3)
                    .fontWeight(.semibold)

                Text(maskedPhone(for: user.phone))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(DesignTokens.Spacing.lg)
            .background(Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))

            Button {
                router.navigate(to: .settings)
            } label: {
                HStack {
                    Text("设置")
                    Spacer()
                    Image(systemName: "chevron.right")
                        .foregroundStyle(.secondary)
                }
                .padding(DesignTokens.Spacing.lg)
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))
            }
            .buttonStyle(.plain)

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .padding(DesignTokens.Spacing.lg)
    }

    private func maskedPhone(for phone: String) -> String {
        guard phone.count == 11 else { return phone }

        let prefix = phone.prefix(3)
        let suffix = phone.suffix(4)
        return "\(prefix)****\(suffix)"
    }
}

#Preview {
    ProfileHomeView()
        .environmentObject(NavigationRouter())
        .environmentObject(AuthStore())
}
