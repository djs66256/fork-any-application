import SwiftUI

struct PlayerView: View {
    @ObservedObject var viewModel: PlayerViewModel
    @Environment(\.scenePhase) private var scenePhase
    @State private var loginAlertContext: CommentLoginContext?

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
                                        colors: [Color(red: 0.99, green: 0.47, blue: 0.29), Color(red: 0.98, green: 0.31, blue: 0.23)],
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

            HStack(spacing: 0) {
                Spacer()
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color(red: 0.24, green: 0.21, blue: 0.17))
                    .frame(width: 110, height: 120)
                    .offset(x: 28, y: 14)
            }

            ZStack {
                Ellipse()
                    .fill(Color(red: 0.18, green: 0.18, blue: 0.19))
                    .frame(width: 180, height: 36)
                    .blur(radius: 14)
                    .offset(y: 88)

                VStack(spacing: -6) {
                    RoundedRectangle(cornerRadius: 18)
                        .fill(Color(red: 0.31, green: 0.31, blue: 0.34))
                        .frame(width: 116, height: 138)
                        .overlay(alignment: .top) {
                            RoundedRectangle(cornerRadius: 18)
                                .fill(Color(red: 0.22, green: 0.22, blue: 0.24))
                                .frame(width: 110, height: 34)
                                .offset(y: -8)
                        }
                        .overlay(alignment: .leading) {
                            stitchMarks
                                .offset(x: -12, y: -4)
                        }
                        .overlay(alignment: .trailing) {
                            stitchMarks
                                .rotationEffect(.degrees(180))
                                .offset(x: 12, y: 6)
                        }

                    RoundedRectangle(cornerRadius: 22)
                        .fill(Color(red: 0.29, green: 0.29, blue: 0.31))
                        .frame(width: 132, height: 80)
                }
                .offset(y: 64)

                VStack(spacing: -12) {
                    RoundedRectangle(cornerRadius: 36)
                        .fill(Color(red: 0.69, green: 0.53, blue: 0.44))
                        .frame(width: 122, height: 142)
                        .overlay {
                            faceDetails
                        }
                        .overlay(alignment: .top) {
                            RoundedRectangle(cornerRadius: 22)
                                .fill(Color(red: 0.34, green: 0.35, blue: 0.4))
                                .frame(width: 118, height: 36)
                                .offset(y: -8)
                        }

                    RoundedRectangle(cornerRadius: 32)
                        .fill(Color(red: 0.46, green: 0.46, blue: 0.5))
                        .frame(width: 154, height: 96)
                }
                .offset(y: 26)
            }
        }
    }

    private var faceDetails: some View {
        ZStack {
            eyebrowPath(offsetY: -28, left: true)
            eyebrowPath(offsetY: -28, left: false)
            eyeRow
            noseShape
            mouthShape
            wrinkleSet
        }
    }

    private func eyebrowPath(offsetY: CGFloat, left: Bool) -> some View {
        Path { path in
            if left {
                path.move(to: CGPoint(x: 28, y: 40 + offsetY))
                path.addQuadCurve(to: CGPoint(x: 53, y: 34 + offsetY), control: CGPoint(x: 40, y: 28 + offsetY))
            } else {
                path.move(to: CGPoint(x: 69, y: 34 + offsetY))
                path.addQuadCurve(to: CGPoint(x: 94, y: 40 + offsetY), control: CGPoint(x: 82, y: 28 + offsetY))
            }
        }
        .stroke(Color(red: 0.24, green: 0.15, blue: 0.11), style: StrokeStyle(lineWidth: 4, lineCap: .round))
        .frame(width: 122, height: 142)
    }

    private var eyeRow: some View {
        HStack(spacing: 18) {
            eyeShape
            eyeShape
        }
        .offset(y: -6)
    }

    private var eyeShape: some View {
        ZStack {
            Ellipse()
                .fill(Color.white.opacity(0.22))
                .frame(width: 24, height: 11)
            Circle()
                .fill(Color(red: 0.14, green: 0.08, blue: 0.07))
                .frame(width: 9, height: 9)
        }
    }

    private var noseShape: some View {
        Path { path in
            path.move(to: CGPoint(x: 61, y: 65))
            path.addLine(to: CGPoint(x: 56, y: 88))
            path.addQuadCurve(to: CGPoint(x: 68, y: 92), control: CGPoint(x: 58, y: 96))
        }
        .stroke(Color(red: 0.42, green: 0.28, blue: 0.22), style: StrokeStyle(lineWidth: 3, lineCap: .round, lineJoin: .round))
        .frame(width: 122, height: 142)
    }

    private var mouthShape: some View {
        Path { path in
            path.move(to: CGPoint(x: 43, y: 108))
            path.addQuadCurve(to: CGPoint(x: 79, y: 106), control: CGPoint(x: 61, y: 118))
        }
        .stroke(Color(red: 0.31, green: 0.14, blue: 0.1), style: StrokeStyle(lineWidth: 3, lineCap: .round))
        .frame(width: 122, height: 142)
    }

    private var wrinkleSet: some View {
        ZStack {
            Path { path in
                path.move(to: CGPoint(x: 26, y: 58))
                path.addQuadCurve(to: CGPoint(x: 18, y: 88), control: CGPoint(x: 12, y: 70))
            }
            .stroke(Color(red: 0.48, green: 0.34, blue: 0.28), style: StrokeStyle(lineWidth: 2, lineCap: .round))

            Path { path in
                path.move(to: CGPoint(x: 96, y: 57))
                path.addQuadCurve(to: CGPoint(x: 106, y: 87), control: CGPoint(x: 112, y: 70))
            }
            .stroke(Color(red: 0.48, green: 0.34, blue: 0.28), style: StrokeStyle(lineWidth: 2, lineCap: .round))

            Path { path in
                path.move(to: CGPoint(x: 44, y: 80))
                path.addQuadCurve(to: CGPoint(x: 30, y: 100), control: CGPoint(x: 32, y: 94))
            }
            .stroke(Color(red: 0.44, green: 0.3, blue: 0.24), style: StrokeStyle(lineWidth: 2, lineCap: .round))

            Path { path in
                path.move(to: CGPoint(x: 78, y: 78))
                path.addQuadCurve(to: CGPoint(x: 92, y: 100), control: CGPoint(x: 90, y: 92))
            }
            .stroke(Color(red: 0.44, green: 0.3, blue: 0.24), style: StrokeStyle(lineWidth: 2, lineCap: .round))
        }
        .frame(width: 122, height: 142)
    }

    private var stitchMarks: some View {
        VStack(spacing: 14) {
            ForEach(0..<4, id: \.self) { _ in
                HStack(spacing: 3) {
                    RoundedRectangle(cornerRadius: 1)
                        .fill(Color(red: 0.78, green: 0.75, blue: 0.72))
                        .frame(width: 9, height: 2)
                    RoundedRectangle(cornerRadius: 1)
                        .fill(Color(red: 0.78, green: 0.75, blue: 0.72))
                        .frame(width: 2, height: 9)
                }
            }
        }
    }

    private var progressScrubber: some View {
        VStack(spacing: 10) {
            ZStack(alignment: .leading) {
                Capsule()
                    .fill(Color.white.opacity(0.22))
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
