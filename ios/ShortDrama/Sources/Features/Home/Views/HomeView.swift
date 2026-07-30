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
            DesignTokens.HomeChrome.background.ignoresSafeArea()

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
                .transition(.opacity)
                .zIndex(1)
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .safeAreaInset(edge: .top) {
            HomeTopOverlay(
                onOpenMenu: { router.openMenuPanel() },
                onOpenSearch: { router.navigate(to: .searchHome) }
            )
            .padding(.horizontal, DesignTokens.Spacing.md)
            .padding(.top, 2)
            .background(Color.clear)
        }
        .safeAreaInset(edge: .bottom) {
            if let drama = featuredDrama {
                HomeBottomChrome(
                    drama: drama,
                    onPlay: { handlePlay(for: drama) }
                )
                .padding(.horizontal, DesignTokens.Spacing.md)
                .padding(.top, 6)
                .padding(.bottom, 4)
                .background(Color.clear)
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

    private var featuredDrama: Drama? {
        if case .content(let dramas) = viewModel.viewState {
            return dramas.first
        }
        return nil
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
        GeometryReader { proxy in
            ScrollView(.vertical, showsIndicators: false) {
                LazyVStack(spacing: DesignTokens.Spacing.md) {
                    ForEach(dramas) { drama in
                        HomeDramaCardView(
                            drama: drama,
                            onPlay: { onPlay(drama) },
                            onDetail: { onDetail(drama) },
                            onComment: { onComment(drama) }
                        )
                        .frame(minHeight: max(proxy.size.height - 24, 620))
                    }
                }
                .padding(.horizontal, DesignTokens.Spacing.md)
                .padding(.top, DesignTokens.Spacing.sm)
                .padding(.bottom, 116)
            }
            .background(Color.black)
        }
    }
}

private struct HomeFeedLoadingView: View {
    var body: some View {
        VStack(spacing: DesignTokens.Spacing.lg) {
            ProgressView()
                .tint(.white)
            Text("正在加载首页内容…")
                .font(.subheadline)
                .foregroundStyle(Color.white.opacity(0.78))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.black)
        .padding(DesignTokens.Spacing.xl)
    }
}

private struct HomeFeedEmptyView: View {
    let isRetrying: Bool
    let onRetry: () async -> Void

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.lg) {
            Image(systemName: "play.slash.fill")
                .font(.system(size: DesignTokens.IconSize.xl))
                .foregroundStyle(Color.white.opacity(0.4))
            Text("暂无内容")
                .font(.headline)
                .foregroundStyle(.white)
            Text("稍后再来看看新的短剧推荐")
                .font(.subheadline)
                .foregroundStyle(Color.white.opacity(0.68))
            Button(isRetrying ? "刷新中…" : "刷新首页") {
                Task {
                    await onRetry()
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(Color.orange)
            .disabled(isRetrying)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.black)
        .padding(DesignTokens.Spacing.xl)
    }
}

private struct HomeFeedErrorView: View {
    let message: String
    let isRetrying: Bool
    let onRetry: () async -> Void

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.lg) {
            Image(systemName: "wifi.exclamationmark")
                .font(.system(size: DesignTokens.IconSize.xl))
                .foregroundStyle(Color.white.opacity(0.4))
            Text("加载失败")
                .font(.headline)
                .foregroundStyle(.white)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Color.white.opacity(0.68))
                .multilineTextAlignment(.center)
            Button(isRetrying ? "重试中…" : "重试") {
                Task {
                    await onRetry()
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(Color.orange)
            .disabled(isRetrying)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.black)
        .padding(DesignTokens.Spacing.xl)
    }
}

private struct HomeTopOverlay: View {
    let onOpenMenu: () -> Void
    let onOpenSearch: () -> Void

    var body: some View {
        HStack {
            topIconButton(systemName: "line.3.horizontal", action: onOpenMenu)
                .accessibilityLabel("打开菜单")

            Spacer()

            topIconButton(systemName: "magnifyingglass", action: onOpenSearch)
                .accessibilityLabel("搜索")
        }
        .padding(.horizontal, 2)
    }

    private func topIconButton(systemName: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 22, weight: .medium))
                .foregroundStyle(.white)
                .frame(width: 44, height: 44)
                .background(DesignTokens.HomeChrome.iconButtonBackground)
                .overlay {
                    Circle()
                        .stroke(DesignTokens.HomeChrome.iconButtonBorder, lineWidth: 1)
                }
                .clipShape(Circle())
        }
        .buttonStyle(.plain)
    }
}

private struct HomeBottomChrome: View {
    let drama: Drama
    let onPlay: () -> Void

    var body: some View {
        HStack(spacing: DesignTokens.Spacing.md) {
            RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.md)
                .fill(Color.white.opacity(0.12))
                .frame(width: 32, height: 32)
                .overlay {
                    Image(systemName: "play.fill")
                        .font(.headline)
                        .foregroundStyle(DesignTokens.HomeChrome.accentSoft)
                }

            VStack(alignment: .leading, spacing: 2) {
                Text(ctaTitle)
                    .font(.headline.weight(.semibold))
                    .foregroundStyle(.white)
                    .lineLimit(1)

                Text("首页主入口")
                    .font(.caption)
                    .foregroundStyle(Color.white.opacity(0.42))
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Button(action: onPlay) {
                Text("去看")
                    .font(.headline.weight(.semibold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 26)
                    .padding(.vertical, 14)
                    .background(DesignTokens.HomeChrome.accent)
                    .clipShape(RoundedRectangle(cornerRadius: 18))
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.vertical, 14)
        .background(DesignTokens.HomeChrome.frameCtaBackground)
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(DesignTokens.HomeChrome.frameCtaBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }

    private var ctaTitle: String {
        if drama.episodeCount > 0 {
            return "观看完整漫剧 · 全\(drama.episodeCount)集"
        }
        return "立即观看完整漫剧"
    }
}

#Preview {
    HomeView()
        .environmentObject(NavigationRouter())
        .environmentObject(AuthStore())
}
