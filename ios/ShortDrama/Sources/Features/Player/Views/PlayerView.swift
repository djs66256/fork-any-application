import SwiftUI

struct PlayerView: View {
    @ObservedObject var viewModel: PlayerViewModel
    @Environment(\.scenePhase) private var scenePhase
    @State private var loginAlertContext: CommentLoginContext?

    var body: some View {
        Group {
            switch viewModel.uiState {
            case .idle, .bootstrapping:
                PlayerStatusView(
                    systemImage: "play.rectangle.on.rectangle",
                    title: "正在加载播放器",
                    message: "正在准备可播放内容，请稍候。",
                    primaryActionTitle: nil,
                    primaryAction: nil
                )
            case .error(let message):
                PlayerStatusView(
                    systemImage: "wifi.exclamationmark",
                    title: "加载失败",
                    message: message,
                    primaryActionTitle: "重试",
                    primaryAction: { Task { await viewModel.retryBootstrap() } }
                )
            case .noResource:
                PlayerStatusView(
                    systemImage: "tray",
                    title: "暂无可播放内容",
                    message: "当前短剧暂时没有可用资源，请稍后再试。",
                    primaryActionTitle: "返回",
                    primaryAction: viewModel.handleBack
                )
            case .ready, .playing, .paused, .switchingEpisode:
                playerContent
            }
        }
        .toolbar(.hidden, for: .tabBar)
        .toolbar(.hidden, for: .navigationBar)
        .navigationBarBackButtonHidden(true)
        .task {
            await viewModel.loadIfNeeded()
        }
        .onChange(of: scenePhase) { _, phase in
            Task {
                await viewModel.handleScenePhaseChange(phase)
            }
        }
        .onDisappear {
            viewModel.handleDisappear()
        }
        .onReceive(viewModel.$routeEffect) { effect in
            guard let effect else { return }
            switch effect {
            case .requireLogin(let context):
                loginAlertContext = context
            }
            viewModel.clearRouteEffect()
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

    private var playerContent: some View {
        ZStack(alignment: .top) {
            Color.black.ignoresSafeArea()

            NativeVideoPlayerView(
                url: viewModel.playbackURL,
                playbackRate: viewModel.playbackRate,
                onProgressChange: viewModel.updateCurrentProgress
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                PlayerTopBar(
                    title: viewModel.currentEpisode.map { "第 \($0.episodeNumber) 集" } ?? "播放器",
                    speedLabel: viewModel.currentSpeed.label,
                    onBack: viewModel.handleBack,
                    onSpeedTap: { viewModel.isSpeedDialogPresented = true },
                    onMoreTap: { viewModel.isMoreDialogPresented = true }
                )
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.top, DesignTokens.Spacing.xl)

                Spacer()

                HStack(alignment: .bottom, spacing: DesignTokens.Spacing.lg) {
                    PlayerBottomInfoView(
                        title: viewModel.currentEpisode?.title ?? "播放器",
                        metadata: viewModel.currentEpisode.map {
                            "\(viewModel.seriesStatus.displayText) · 第 \($0.episodeNumber) 集"
                        } ?? viewModel.seriesStatus.displayText,
                        description: viewModel.currentEpisode?.description ?? ""
                    )

                    PlayerRightActionBar(
                        liked: viewModel.liked,
                        favorited: viewModel.favorited,
                        onLike: viewModel.toggleLike,
                        onFavorite: viewModel.toggleFavorite,
                        onComment: viewModel.openComments
                    )
                }
                .padding(.horizontal, DesignTokens.Spacing.lg)

                PlayerEpisodeDock(
                    seriesStatus: viewModel.seriesStatus.displayText,
                    totalCount: viewModel.episodes.count,
                    currentEpisodeNumber: viewModel.currentEpisode?.episodeNumber,
                    onTap: { viewModel.isEpisodeSheetPresented = true }
                )
                .padding(DesignTokens.Spacing.lg)
            }

            if viewModel.uiState == .switchingEpisode {
                Color.black.opacity(0.35)
                    .ignoresSafeArea()
                ProgressView("正在切换剧集")
                    .tint(.white)
                    .foregroundStyle(.white)
            }
        }
        .sheet(isPresented: $viewModel.isEpisodeSheetPresented) {
            EpisodePickerSheet(
                episodes: viewModel.episodes,
                currentEpisodeId: viewModel.currentEpisode?.id,
                onSelect: { episode in
                    viewModel.isEpisodeSheetPresented = false
                    Task {
                        await viewModel.switchEpisode(to: episode)
                    }
                }
            )
            .presentationDetents([.medium, .large])
        }
        .sheet(isPresented: $viewModel.isCommentSheetPresented) {
            CommentSheetView(
                viewModel: viewModel.makeCommentSheetViewModel(),
                onClose: viewModel.closeComments,
                onRequireLogin: viewModel.handleCommentLoginRequired(_:)
            )
        }
        .confirmationDialog("播放速度", isPresented: $viewModel.isSpeedDialogPresented) {
            ForEach(PlayerViewModel.PlaybackSpeed.allCases, id: \.self) { speed in
                Button(speed.label) {
                    viewModel.selectSpeed(speed)
                }
            }
        }
        .confirmationDialog("更多", isPresented: $viewModel.isMoreDialogPresented) {
            Button("敬请期待") {}
        } message: {
            Text("更多能力首版仅提供占位入口，暂未开放具体功能。")
        }
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
}

#Preview {
    PlayerView(viewModel: PlayerViewModel(videoId: "preview-123"))
}
