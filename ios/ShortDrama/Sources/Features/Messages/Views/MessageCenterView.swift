import SwiftUI

struct MessageCenterView: View {
    @EnvironmentObject private var router: NavigationRouter
    @EnvironmentObject private var authStore: AuthStore
    @StateObject private var viewModel: MessageCenterViewModel

    init(viewModel: MessageCenterViewModel) {
        _viewModel = StateObject(wrappedValue: viewModel)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.xl) {
                section(title: "系统消息") {
                    systemSection
                }

                section(title: "互动消息") {
                    interactionSection
                }
            }
            .padding(DesignTokens.Spacing.lg)
        }
        .navigationTitle("我的消息")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.loadInitial()
        }
        .onReceive(authStore.$status) { _ in
            Task {
                await viewModel.handleLoginSuccess()
            }
        }
    }

    private func section<Content: View>(title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
            Text(title)
                .font(.headline)
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private var systemSection: some View {
        switch viewModel.systemMessages {
        case .idle, .loading:
            ProgressView()
                .frame(maxWidth: .infinity, alignment: .center)
        case .empty:
            emptyState(text: "暂无消息")
        case .error(let message):
            retryState(message: message) {
                await viewModel.retrySystemMessages()
            }
        case .content(let items, _):
            VStack(spacing: DesignTokens.Spacing.md) {
                ForEach(items) { item in
                    messageRow(title: item.title, summary: item.summary, time: item.sentAt)
                }
            }
        case .loginRequired:
            EmptyView()
        }
    }

    @ViewBuilder
    private var interactionSection: some View {
        switch viewModel.interactionMessages {
        case .idle, .loading:
            ProgressView()
                .frame(maxWidth: .infinity, alignment: .center)
        case .loginRequired:
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
                Text("登录后查看互动消息")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Button("立即登录") {
                    router.presentLogin(context: viewModel.loginContext)
                }
                .buttonStyle(.borderedProminent)
            }
        case .empty:
            emptyState(text: "暂无互动消息")
        case .error(let message):
            retryState(message: message) {
                await viewModel.retryInteractionMessages()
            }
        case .content(let items, _):
            VStack(spacing: DesignTokens.Spacing.md) {
                ForEach(items) { item in
                    messageRow(title: item.title, summary: item.summary, time: item.sentAt)
                }
            }
        }
    }

    private func messageRow(title: String, summary: String, time: String) -> some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
            HStack {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                Spacer()
                Text(time)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            Text(summary)
                .font(.footnote)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.leading)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(DesignTokens.Spacing.md)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))
    }

    private func emptyState(text: String) -> some View {
        Text(text)
            .font(.subheadline)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(DesignTokens.Spacing.md)
            .background(Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))
    }

    private func retryState(message: String, action: @escaping () async -> Void) -> some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            Text(message)
                .font(.footnote)
                .foregroundStyle(.secondary)
            Button("重试") {
                Task {
                    await action()
                }
            }
            .buttonStyle(.bordered)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(DesignTokens.Spacing.md)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))
    }
}
