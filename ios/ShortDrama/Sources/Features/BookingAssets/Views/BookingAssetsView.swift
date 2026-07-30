import SwiftUI

struct BookingAssetsView: View {
    @EnvironmentObject private var router: NavigationRouter
    @EnvironmentObject private var authStore: AuthStore
    @StateObject private var viewModel: BookingAssetsViewModel

    init() {
        let repository: DramaRepositoryProtocol = DramaRepository()
        _viewModel = StateObject(
            wrappedValue: BookingAssetsViewModel(
                fetchBookingAssetsUseCase: FetchBookingAssetsUseCase(repository: repository)
            )
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
            switch authStore.status {
            case .restoring:
                loadingView(message: "正在恢复登录状态…")
            case .anonymous, .expired:
                BookingAssetsLoginGateView(onTapLogin: handleTapLogin)
            case .authenticated, .refreshing:
                authenticatedContent
            }
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.top, DesignTokens.Spacing.md)
        .navigationTitle("我的预约")
        .navigationBarTitleDisplayMode(.inline)
        .task(id: authStore.accessToken ?? "guest") {
            await viewModel.loadIfNeeded(accessToken: authStore.accessToken)
        }
        .onChange(of: authStore.isAuthenticated) { _, isAuthenticated in
            viewModel.handleAuthStatusChange(isAuthenticated: isAuthenticated)
        }
    }

    @ViewBuilder
    private var authenticatedContent: some View {
        BookingAssetsTabBar(
            selectedStatus: viewModel.selectedStatus,
            summary: viewModel.summary,
            onSelect: handleSelectStatus(_:)
        )

        switch viewModel.viewState {
        case .idle, .loading:
            loadingView(message: "正在加载预约内容…")
        case .content(let assets):
            contentList(assets: assets)
        case .empty:
            BookingAssetsEmptyView(status: viewModel.selectedStatus)
        case .error(let message):
            BookingAssetsErrorView(message: message, onRetry: handleRetry)
        }
    }

    private func contentList(assets: [BookingAsset]) -> some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: DesignTokens.Spacing.md) {
                    Color.clear
                        .frame(height: 1)
                        .id("booking-assets-top")

                    ForEach(Array(assets.enumerated()), id: \.element.id) { index, asset in
                        BookingAssetCardView(asset: asset)
                            .onAppear {
                                if index == assets.count - 1 {
                                    Task {
                                        await viewModel.loadMoreIfNeeded(accessToken: authStore.accessToken)
                                    }
                                }
                            }
                    }

                    if viewModel.isAppending {
                        ProgressView("正在加载更多…")
                            .padding(.vertical, DesignTokens.Spacing.md)
                    }

                    if let appendErrorMessage = viewModel.appendErrorMessage {
                        Text(appendErrorMessage)
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.vertical, DesignTokens.Spacing.md)
                    }
                }
                .padding(.bottom, DesignTokens.Spacing.lg)
            }
            .onChange(of: assets.first?.id) { _, _ in
                withAnimation(.easeInOut(duration: 0.2)) {
                    proxy.scrollTo("booking-assets-top", anchor: .top)
                }
            }
        }
    }

    private func loadingView(message: String) -> some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            ProgressView()
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(DesignTokens.Spacing.xl)
    }

    private func handleTapLogin() {
        router.presentLogin(context: BookingAssetsRouteBuilder.loginContext())
    }

    private func handleSelectStatus(_ status: BookingAssetAvailabilityStatus) {
        Task {
            await viewModel.selectStatus(status, accessToken: authStore.accessToken)
        }
    }

    private func handleRetry() {
        Task {
            await viewModel.retry(accessToken: authStore.accessToken)
        }
    }
}

#Preview {
    NavigationStack {
        BookingAssetsView()
            .environmentObject(NavigationRouter())
            .environmentObject(AuthStore())
    }
}
