import SwiftUI

struct PlayerView: View {
    @ObservedObject var viewModel: PlayerViewModel
    @Environment(\.scenePhase) private var scenePhase
    @State private var loginAlertContext: CommentLoginContext?

    private let isPreviewPlayerCommentsSheet = ProcessInfo.processInfo.arguments.contains("--preview-player-comments-sheet")
    private let fallbackTitle = "全族托举农门状元郎"
    private let fallbackHotComment = "大伯母没错，要不是大伯母..."
    private let fallbackDisclaimer = "作者声明：内容由AI生成"
    private let fallbackFavoriteCount = "31.5万"
    private let fallbackCommentCount = "319"
    private let fallbackLikeCount = "1万"
    private let fallbackShareCount = "3649"

    var body: some View {
        playerContent
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

            backgroundLayer
                .ignoresSafeArea()
                .scaleEffect(isPreviewPlayerCommentsSheet ? 1.16 : 1.0)
                .offset(y: isPreviewPlayerCommentsSheet ? -24 : 0)
                .overlay(alignment: .top) {
                    LinearGradient(
                        colors: [Color.black.opacity(0.85), Color.black.opacity(0)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(height: 190)
                    .allowsHitTesting(false)
                }
                .overlay(alignment: .bottom) {
                    LinearGradient(
                        colors: [Color.black.opacity(0), Color.black.opacity(0.55), Color.black.opacity(0.92)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(height: 420)
                    .allowsHitTesting(false)
                }

            VStack(spacing: 0) {
                PlayerTopBar(
                    title: viewModel.currentEpisode.map { "第\($0.episodeNumber)集" } ?? "第3集",
                    speedLabel: viewModel.currentSpeed.label,
                    onBack: viewModel.handleBack,
                    onSpeedTap: { viewModel.isSpeedDialogPresented = true },
                    onMoreTap: { viewModel.isMoreDialogPresented = true }
                )
                .padding(.horizontal, 18)
                .padding(.top, 12)

                Spacer()

                HStack(alignment: .bottom, spacing: 16) {
                    PlayerBottomInfoView(
                        title: infoTitle,
                        hotComment: fallbackHotComment,
                        disclaimer: fallbackDisclaimer
                    )
                    .padding(.bottom, 54)

                    PlayerRightActionBar(
                        liked: viewModel.liked,
                        favorited: viewModel.favorited,
                        favoriteCountText: fallbackFavoriteCount,
                        commentCountText: fallbackCommentCount,
                        likeCountText: fallbackLikeCount,
                        shareCountText: fallbackShareCount,
                        onLike: viewModel.toggleLike,
                        onFavorite: viewModel.toggleFavorite,
                        onComment: viewModel.openComments
                    )
                }
                .padding(.horizontal, 24)

                progressScrubber
                    .padding(.horizontal, 28)
                    .padding(.top, 14)
                    .padding(.bottom, 16)

                PlayerEpisodeDock(
                    title: "选集",
                    seriesStatus: viewModel.seriesStatus.displayText,
                    totalCount: max(viewModel.episodes.count, 133),
                    onTap: { viewModel.isEpisodeSheetPresented = true }
                )
                .padding(.horizontal, 20)
                .padding(.bottom, 18)
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

    private var infoTitle: String {
        let title = viewModel.currentEpisode?.title.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !title.isEmpty else { return fallbackTitle }
        if title.hasPrefix("第") && title.contains("集") {
            return fallbackTitle
        }
        return title
    }

    @ViewBuilder
    private var backgroundLayer: some View {
        if let playbackURL = viewModel.playbackURL {
            NativeVideoPlayerView(
                url: playbackURL,
                playbackRate: viewModel.playbackRate,
                onProgressChange: viewModel.updateCurrentProgress,
                onPlaybackEnded: viewModel.handlePlaybackEnded,
                onPlaybackFailed: viewModel.handlePlaybackFailure(message:)
            )
        } else {
            fallbackPoster
        }
    }

    private var fallbackPoster: some View {
        GeometryReader { geometry in
            ZStack {
                LinearGradient(
                    colors: [Color.black, Color(red: 0.05, green: 0.08, blue: 0.1)],
                    startPoint: .top,
                    endPoint: .bottom
                )

                VStack(spacing: 0) {
                    Spacer()
                        .frame(height: geometry.size.height * 0.33)

                    ZStack(alignment: .trailing) {
                        posterIllustration
                            .frame(maxWidth: .infinity)
                            .frame(height: geometry.size.height * 0.27)
                            .clipped()

                        ZStack {
                            Circle()
                                .fill(Color(red: 0.97, green: 0.79, blue: 0.38))
                            Circle()
                                .stroke(Color(red: 0.99, green: 0.85, blue: 0.45), lineWidth: 5)
                            Circle()
                                .fill(
                                    LinearGradient(
                                        colors: [
                                            Color(red: 0.99, green: 0.47, blue: 0.29),
                                            Color(red: 0.98, green: 0.31, blue: 0.23)
                                        ],
                                        startPoint: .top,
                                        endPoint: .bottom
                                    )
                                )
                                .padding(9)
                            VStack(spacing: 0) {
                                Text("12879")
                                    .font(.system(size: 15, weight: .bold))
                                    .foregroundStyle(.white)
                            }
                        }
                        .frame(width: 74, height: 74)
                        .padding(.trailing, 32)
                    }

                    Spacer()
                }
            }
        }
    }

    private var posterIllustration: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.12, green: 0.2, blue: 0.18), Color(red: 0.07, green: 0.08, blue: 0.1)],
                startPoint: .leading,
                endPoint: .trailing
            )

            HStack(spacing: 0) {
                Rectangle()
                    .fill(Color(red: 0.2, green: 0.17, blue: 0.15))
                    .frame(width: 66)
                    .opacity(0.6)

                Spacer()
            }

            ZStack(alignment: .bottom) {
                RoundedRectangle(cornerRadius: 36, style: .continuous)
                    .fill(Color(red: 0.33, green: 0.23, blue: 0.19))
                    .frame(width: 154, height: 214)
                    .overlay(alignment: .top) {
                        RoundedRectangle(cornerRadius: 30, style: .continuous)
                            .fill(Color(red: 0.96, green: 0.79, blue: 0.67))
                            .frame(width: 104, height: 122)
                            .offset(y: 10)
                    }
                    .overlay(alignment: .center) {
                        VStack(spacing: 0) {
                            RoundedRectangle(cornerRadius: 24, style: .continuous)
                                .fill(Color(red: 0.22, green: 0.14, blue: 0.1))
                                .frame(width: 116, height: 140)
                            RoundedRectangle(cornerRadius: 18, style: .continuous)
                                .fill(Color(red: 0.08, green: 0.08, blue: 0.09))
                                .frame(width: 72, height: 72)
                                .offset(y: -18)
                        }
                    }

                VStack(spacing: 5) {
                    Text("全族托举")
                        .font(.system(size: 18, weight: .black))
                        .foregroundStyle(Color(red: 0.98, green: 0.88, blue: 0.48))
                    Text("农门状元郎")
                        .font(.system(size: 22, weight: .black))
                        .foregroundStyle(.white)
                }
                .padding(.bottom, 18)
                .shadow(color: .black.opacity(0.3), radius: 8, x: 0, y: 4)
            }
            .offset(x: -6, y: 6)
        }
    }

    private var progressScrubber: some View {
        VStack(spacing: 12) {
            HStack(spacing: 12) {
                Text("00:24")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(.white.opacity(0.88))

                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(Color.white.opacity(0.16))
                        .frame(height: 3)

                    Capsule()
                        .fill(Color.white)
                        .frame(width: 108, height: 3)

                    Circle()
                        .fill(Color.white)
                        .frame(width: 8, height: 8)
                        .offset(x: 104)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Text("01:40")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(.white.opacity(0.72))
            }

            Capsule()
                .fill(Color.white)
                .frame(width: 134, height: 5)
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
