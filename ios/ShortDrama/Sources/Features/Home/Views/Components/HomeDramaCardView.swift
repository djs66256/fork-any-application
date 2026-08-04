import SwiftUI

/// Reusable drama card used by the home feed and search results.
struct HomeDramaCardView: View {
    let drama: Drama
    let onPlay: () -> Void
    let onDetail: () -> Void
    let onComment: () -> Void
    let layout: HomeDramaCardLayout
    let videoURL: URL?
    let videoPlaybackRate: Float
    let onVideoProgressChange: (Double) -> Void
    let onVideoPlaybackEnded: () -> Void
    let onVideoPlaybackFailed: (String) -> Void

    init(
        drama: Drama,
        onPlay: @escaping () -> Void,
        onDetail: @escaping () -> Void,
        onComment: @escaping () -> Void,
        layout: HomeDramaCardLayout = .card,
        videoURL: URL? = nil,
        videoPlaybackRate: Float = 1.0,
        onVideoProgressChange: @escaping (Double) -> Void = { _ in },
        onVideoPlaybackEnded: @escaping () -> Void = {},
        onVideoPlaybackFailed: @escaping (String) -> Void = { _ in }
    ) {
        self.drama = drama
        self.onPlay = onPlay
        self.onDetail = onDetail
        self.onComment = onComment
        self.layout = layout
        self.videoURL = videoURL
        self.videoPlaybackRate = videoPlaybackRate
        self.onVideoProgressChange = onVideoProgressChange
        self.onVideoPlaybackEnded = onVideoPlaybackEnded
        self.onVideoPlaybackFailed = onVideoPlaybackFailed
    }

    private var topBadges: [String] {
        var items: [String] = []

        if !drama.category.isEmpty {
            items.append(drama.category)
        }

        if drama.episodeCount > 0 {
            items.append("全\(drama.episodeCount)集")
        }

        return Array(items.prefix(2))
    }

    private var tagItems: [String] {
        let source = (drama.tags ?? []).filter { !$0.isEmpty }
        return Array(source.prefix(5))
    }

    var body: some View {
        Group {
            switch layout {
            case .card:
                cardContent
                    .frame(maxWidth: .infinity)
                    .frame(minHeight: 400)
                    .background(Color.black)
                    .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.xl))
                    .overlay {
                        RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.xl)
                            .stroke(Color.white.opacity(0.05), lineWidth: 1)
                    }
            case .immersivePage:
                cardContent
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color.black)
                    .clipped()
            }
        }
    }

    private var cardContent: some View {
        ZStack(alignment: .bottomLeading) {
            DramaBackgroundView(
                coverURL: drama.coverUrl,
                videoURL: videoURL,
                playbackRate: videoPlaybackRate,
                onProgressChange: onVideoProgressChange,
                onPlaybackEnded: onVideoPlaybackEnded,
                onPlaybackFailed: onVideoPlaybackFailed
            )

            LinearGradient(
                colors: [
                    Color.black.opacity(0.05),
                    Color.black.opacity(0.18),
                    Color.black.opacity(0.55),
                    Color.black.opacity(0.94)
                ],
                startPoint: .top,
                endPoint: .bottom
            )

            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: DesignTokens.Spacing.sm) {
                    ForEach(topBadges, id: \.self) { badge in
                        Text(badge)
                            .font(.caption.weight(.medium))
                            .foregroundStyle(Color.white.opacity(0.92))
                            .padding(.horizontal, DesignTokens.Spacing.md)
                            .padding(.vertical, 6)
                            .background(Color.white.opacity(0.12))
                            .overlay {
                                Capsule()
                                    .stroke(Color.white.opacity(0.12), lineWidth: 1)
                            }
                            .clipShape(Capsule())
                    }

                    Spacer()
                }
                .padding(.horizontal, contentHorizontalPadding)
                .padding(.top, contentTopPadding)

                Spacer()

                HStack(alignment: .bottom, spacing: DesignTokens.Spacing.lg) {
                    contentSection
                    interactionRail
                }
                .padding(.horizontal, contentHorizontalPadding)
                .padding(.bottom, contentBottomPadding)
            }
        }
    }

    private var contentHorizontalPadding: CGFloat {
        switch layout {
        case .card:
            return DesignTokens.Spacing.lg
        case .immersivePage(let contentInsets):
            return max(contentInsets.leading, DesignTokens.Spacing.lg)
        }
    }

    private var contentTopPadding: CGFloat {
        switch layout {
        case .card:
            return DesignTokens.Spacing.xl
        case .immersivePage(let contentInsets):
            return max(contentInsets.top, DesignTokens.Spacing.xl)
        }
    }

    private var contentBottomPadding: CGFloat {
        switch layout {
        case .card:
            return DesignTokens.Spacing.xl
        case .immersivePage(let contentInsets):
            return max(contentInsets.bottom, DesignTokens.Spacing.xl)
        }
    }

    private var contentSection: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
            if !tagItems.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: DesignTokens.Spacing.sm) {
                        ForEach(tagItems, id: \.self) { tag in
                            Text(tag)
                                .font(.caption.weight(.medium))
                                .foregroundStyle(Color.white.opacity(0.9))
                                .padding(.horizontal, DesignTokens.Spacing.md)
                                .padding(.vertical, 6)
                                .background(Color.white.opacity(0.12))
                                .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.md))
                        }
                    }
                    .padding(.vertical, 2)
                }
                .scrollDisabled(true)
            }

            Text(drama.title)
                .font(.system(size: 32, weight: .bold))
                .foregroundStyle(.white)
                .lineLimit(2)
                .shadow(color: .black.opacity(0.25), radius: 10, y: 4)

            if !drama.description.isEmpty {
                Text(drama.description)
                    .font(.subheadline)
                    .foregroundStyle(Color.white.opacity(0.82))
                    .lineLimit(2)
            }

            Text("作者声明：内容由 AI 生成")
                .font(.footnote)
                .foregroundStyle(Color.white.opacity(0.48))

            HStack(spacing: DesignTokens.Spacing.md) {
                Text(drama.episodeCount > 0 ? "观看完整漫剧 · 全\(drama.episodeCount)集" : "立即观看完整漫剧")
                    .font(.headline.weight(.semibold))
                    .foregroundStyle(DesignTokens.HomeChrome.mutedText)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Button(action: onDetail) {
                    Text("详情 >")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(DesignTokens.HomeChrome.mutedText)
                }
                .buttonStyle(.plain)

                Button(action: onPlay) {
                    Text("去看")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, DesignTokens.Spacing.lg)
                        .padding(.vertical, DesignTokens.Spacing.sm)
                        .background(DesignTokens.HomeChrome.accent)
                        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))
                }
                .buttonStyle(.plain)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var interactionRail: some View {
        VStack(spacing: DesignTokens.Spacing.xl) {
            railButton(icon: "play.circle.fill", title: "观看", action: onPlay)
            railButton(icon: "text.bubble.fill", title: "评论", action: onComment)
            railButton(icon: "bookmark.fill", title: "详情", action: onDetail)
        }
        .padding(.bottom, DesignTokens.Spacing.sm)
    }

    private func railButton(icon: String, title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: DesignTokens.Spacing.sm) {
                ZStack {
                    Circle()
                        .fill(Color.white.opacity(0.18))
                        .frame(width: 54, height: 54)
                    Image(systemName: icon)
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundStyle(.white)
                }
                Text(title)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color.white.opacity(0.9))
            }
        }
        .buttonStyle(.plain)
    }
}

enum HomeDramaCardLayout {
    case card
    case immersivePage(contentInsets: EdgeInsets)
}

private struct DramaBackgroundView: View {
    let coverURL: String
    let videoURL: URL?
    let playbackRate: Float
    let onProgressChange: (Double) -> Void
    let onPlaybackEnded: () -> Void
    let onPlaybackFailed: (String) -> Void

    var body: some View {
        Group {
            if let videoURL {
                NativeVideoPlayerView(
                    url: videoURL,
                    playbackRate: playbackRate,
                    onProgressChange: onProgressChange,
                    onPlaybackEnded: onPlaybackEnded,
                    onPlaybackFailed: onPlaybackFailed
                )
            } else {
                DramaCoverView(coverURL: coverURL)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
        .allowsHitTesting(false)
    }
}

private struct DramaCoverView: View {
    let coverURL: String

    var body: some View {
        Group {
            if let url = URL(string: coverURL), !coverURL.isEmpty {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                    default:
                        placeholder
                    }
                }
            } else {
                placeholder
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
    }

    private var placeholder: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.17, green: 0.12, blue: 0.1),
                    Color(red: 0.11, green: 0.08, blue: 0.08),
                    Color.black
                ],
                startPoint: .top,
                endPoint: .bottom
            )

            Image(systemName: "play.rectangle.fill")
                .font(.system(size: DesignTokens.IconSize.xxl))
                .foregroundStyle(Color.white.opacity(0.2))
        }
    }
}
