import SwiftUI

/// The main home screen of the app.
struct HomeView: View {

    @EnvironmentObject private var router: NavigationRouter
    @EnvironmentObject private var authStore: AuthStore
    @StateObject private var viewModel: HomeViewModel
    @State private var loginAlertContext: CommentLoginContext?

    init(viewModel: HomeViewModel? = nil) {
        if let viewModel {
            _viewModel = StateObject(wrappedValue: viewModel)
        } else {
            let repository: DramaRepositoryProtocol = DramaRepository()
            let useCase = FetchDramasUseCase(repository: repository)
            _viewModel = StateObject(
                wrappedValue: HomeViewModel(fetchDramasUseCase: useCase)
            )
        }
    }

    var body: some View {
        ZStack {
            Group {
                switch viewModel.viewState {
                case .loading:
                    HomeFeedLoadingView()
                case .content(let dramas):
                    HomeFeedListView(
                        dramas: dramas,
                        onPlay: handlePlay(for:),
                        onComment: handleComment(for:),
                        onDetail: handleDetail(for:)
                    )
                case .empty:
                    HomeFeedEmptyView(
                        isRetrying: viewModel.isRetrying,
                        onRetry: { await viewModel.retry() }
                    )
                case .error(let message):
                    HomeFeedErrorView(
                        message: message,
                        isRetrying: viewModel.isRetrying,
                        onRetry: { await viewModel.retry() }
                    )
                }
            }

            if let popupState = viewModel.checkInPopupState,
               loginAlertContext == nil,
               viewModel.activeCommentSheet == nil {
                CheckInPopupView(
                    state: popupState,
                    onClose: viewModel.dismissCheckInPopup,
                    onSubmit: {
                        Task {
                            await viewModel.submitCheckIn()
                        }
                    }
                )
            }
        }
        .navigationTitle("首页")
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button {
                    router.openMenuPanel()
                } label: {
                    Image(systemName: "line.3.horizontal")
                }
                .accessibilityLabel("打开菜单")
            }

            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    router.navigate(to: .searchHome)
                } label: {
                    Image(systemName: "magnifyingglass")
                }
                .accessibilityLabel("搜索")
            }
        }
        .task {
            viewModel.updateAuthState(
                isUserLoggedIn: authStore.isAuthenticated,
                accessToken: authStore.status.currentSession?.accessToken
            )
            await viewModel.loadIfNeeded()
        }
        .onReceive(authStore.$status) { status in
            viewModel.updateAuthState(
                isUserLoggedIn: authStore.isAuthenticated,
                accessToken: status.currentSession?.accessToken
            )
        }
        .sheet(item: activeCommentSheetBinding) { context in
            CommentSheetView(
                viewModel: viewModel.makeCommentSheetViewModel(dramaId: context.id),
                onClose: viewModel.closeComments,
                onRequireLogin: viewModel.handleCommentLoginRequired(_:)
            )
        }
        .onReceive(viewModel.$pendingCommentLoginContext) { context in
            guard let context else { return }
            loginAlertContext = context
        }
        .alert("请先登录", isPresented: isShowingLoginAlert, presenting: loginAlertContext) { _ in
            Button("取消", role: .cancel) {
                loginAlertContext = nil
                viewModel.clearPendingCommentLoginContext()
            }
            Button("我知道了") {
                let context = loginAlertContext
                loginAlertContext = nil
                viewModel.clearPendingCommentLoginContext()
                if let context {
                    viewModel.restoreCommentContext(context)
                }
            }
        } message: { _ in
            Text("登录后即可发表评论或点赞评论。首版仅恢复评论抽屉上下文，不自动重放写操作。")
        }
    }

    private var activeCommentSheetBinding: Binding<HomeViewModel.CommentSheetContext?> {
        Binding(
            get: { viewModel.activeCommentSheet },
            set: { value in
                if value == nil {
                    viewModel.closeComments()
                }
            }
        )
    }

    private var isShowingLoginAlert: Binding<Bool> {
        Binding(
            get: { loginAlertContext != nil },
            set: { isPresented in
                if !isPresented {
                    loginAlertContext = nil
                    viewModel.clearPendingCommentLoginContext()
                }
            }
        )
    }

    private func handlePlay(for drama: Drama) {
        guard let route = HomeRouteBuilder.playerRoute(for: drama) else { return }
        router.navigate(to: route)
    }

    private func handleComment(for drama: Drama) {
        viewModel.openComments(for: drama)
    }

    private func handleDetail(for drama: Drama) {
        guard let route = HomeRouteBuilder.detailRoute(for: drama) else { return }
        router.navigate(to: route)
    }
}

enum HomeRouteBuilder {
    static func playerRoute(for drama: Drama) -> AppRoute? {
        guard !drama.id.isEmpty else { return nil }
        return .player(videoId: drama.id)
    }

    static func detailRoute(for drama: Drama) -> AppRoute? {
        guard !drama.id.isEmpty else { return nil }
        return .dramaDetail(dramaId: drama.id)
    }
}

private struct HomeFeedListView: View {
    let dramas: [Drama]
    let onPlay: (Drama) -> Void
    let onComment: (Drama) -> Void
    let onDetail: (Drama) -> Void

    var body: some View {
        ScrollView {
            LazyVStack(spacing: DesignTokens.Spacing.lg) {
                ForEach(dramas) { drama in
                    HomeDramaCardView(
                        drama: drama,
                        onPlay: { onPlay(drama) },
                        onDetail: { onDetail(drama) },
                        onComment: { onComment(drama) }
                    )
                }
            }
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.vertical, DesignTokens.Spacing.md)
        }
    }
}

private struct HomeFeedLoadingView: View {
    var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            ProgressView()
            Text("正在加载首页内容…")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(DesignTokens.Spacing.xl)
    }
}

private struct HomeFeedEmptyView: View {
    let isRetrying: Bool
    let onRetry: () async -> Void

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            Image(systemName: "tray")
                .font(.system(size: DesignTokens.IconSize.xl))
                .foregroundStyle(.secondary)
            Text("暂无内容")
                .font(.headline)
            Text("稍后再来看看新的短剧推荐")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Button(isRetrying ? "刷新中…" : "刷新首页") {
                Task {
                    await onRetry()
                }
            }
            .buttonStyle(.bordered)
            .disabled(isRetrying)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(DesignTokens.Spacing.xl)
    }
}

private struct HomeFeedErrorView: View {
    let message: String
    let isRetrying: Bool
    let onRetry: () async -> Void

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            Image(systemName: "wifi.exclamationmark")
                .font(.system(size: DesignTokens.IconSize.xl))
                .foregroundStyle(.secondary)
            Text("加载失败")
                .font(.headline)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button(isRetrying ? "重试中…" : "重试") {
                Task {
                    await onRetry()
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(isRetrying)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(DesignTokens.Spacing.xl)
    }
}

#Preview {
    HomeView()
        .environmentObject(NavigationRouter())
}
