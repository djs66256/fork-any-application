import SwiftUI

struct TheaterDramaCardView: View {
    let drama: TheaterDrama
    let index: Int
    let onTap: () -> Void

    private var displayStyle: TheaterCardDisplayStyle {
        TheaterCardDisplayStyle.make(for: drama, index: index)
    }

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 10) {
                coverView
                titleView
                tagsView
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 10)
            .padding(.top, 10)
            .padding(.bottom, 10)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(Color.white)
            )
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .buttonStyle(.plain)
    }

    private var coverView: some View {
        ZStack(alignment: .topLeading) {
            posterImage
                .frame(maxWidth: .infinity)
                .aspectRatio(0.735, contentMode: .fit)
                .background(Color(.tertiarySystemFill))
                .clipped()
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay(alignment: .bottomLeading) {
                    LinearGradient(
                        colors: [Color.clear, Color.black.opacity(0.16), Color.black.opacity(0.48)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .allowsHitTesting(false)
                }
                .overlay(alignment: .bottomLeading) {
                    HStack(spacing: 3) {
                        Image(systemName: "drop.fill")
                            .font(.system(size: 9, weight: .bold))
                        Text(displayStyle.heatText)
                            .font(.system(size: 10, weight: .semibold))
                    }
                    .foregroundStyle(.white)
                    .padding(.horizontal, 9)
                    .padding(.vertical, 4)
                    .background(Color.black.opacity(0.12))
                    .clipShape(Capsule())
                    .padding(.leading, 8)
                    .padding(.bottom, 9)
                }

            if let badgeText = displayStyle.badgeText {
                Text(badgeText)
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(badgeBackground(for: badgeText))
                    .clipShape(
                        UnevenRoundedRectangle(
                            topLeadingRadius: 16,
                            bottomLeadingRadius: 4,
                            bottomTrailingRadius: 16,
                            topTrailingRadius: 4,
                            style: .continuous
                        )
                    )
                    .padding(.top, 1)
                    .padding(.leading, 1)
            }

            if let coinOverlay = displayStyle.coinOverlay {
                coinOverlayView(text: coinOverlay.text)
                    .padding(.top, 126)
                    .padding(.leading, 108)
            }
        }
    }

    @ViewBuilder
    private var posterImage: some View {
        if let coverUrl = drama.coverUrl,
           let url = URL(string: coverUrl),
           !coverUrl.isEmpty,
           !isPlaceholderURL(coverUrl) {
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFill()
                default:
                    fallbackPoster
                }
            }
        } else {
            fallbackPoster
        }
    }

    private var fallbackPoster: some View {
        Group {
            if let image = TheaterPosterFallback.image(for: drama, index: index) {
                image
                    .resizable()
                    .scaledToFill()
            } else {
                placeholder
            }
        }
    }

    private var titleView: some View {
        Text(displayStyle.title)
            .font(.system(size: 17, weight: .semibold))
            .foregroundStyle(Color.primary)
            .lineSpacing(1.5)
            .lineLimit(2)
            .fixedSize(horizontal: false, vertical: true)
    }

    @ViewBuilder
    private var tagsView: some View {
        if !displayStyle.tagTexts.isEmpty {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(displayStyle.tagTexts, id: \.self) { tag in
                        HStack(spacing: 3) {
                            if shouldShowTagIcon(for: tag) {
                                Image(systemName: iconName(for: tag))
                                    .font(.system(size: 10, weight: .bold))
                            }

                            Text(tag)
                                .font(.system(size: 11, weight: .medium))
                                .lineLimit(1)

                            if tag.contains("No.") {
                                Image(systemName: "chevron.right")
                                    .font(.system(size: 8, weight: .bold))
                            }
                        }
                        .foregroundStyle(tagColor(for: tag))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(tagBackground(for: tag))
                        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    }
                }
            }
            .scrollDisabled(true)
        }
    }

    private var placeholder: some View {
        ZStack {
            LinearGradient(
                colors: [Color(.systemGray4), Color(.systemGray5)],
                startPoint: .top,
                endPoint: .bottom
            )
            Image(systemName: "photo")
                .font(.system(size: DesignTokens.IconSize.lg))
                .foregroundStyle(.white.opacity(0.8))
        }
    }

    private func coinOverlayView(text: String) -> some View {
        VStack(spacing: -8) {
            ZStack {
                Circle()
                    .fill(
                        RadialGradient(
                            colors: [Color(red: 1.0, green: 0.43, blue: 0.24), Color(red: 0.95, green: 0.16, blue: 0.08)],
                            center: .center,
                            startRadius: 8,
                            endRadius: 34
                        )
                    )
                    .frame(width: 62, height: 62)

                Image(systemName: "bitcoinsign.circle.fill")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(Color(red: 1.0, green: 0.84, blue: 0.36))

                RoundedRectangle(cornerRadius: 4, style: .continuous)
                    .fill(Color(red: 1.0, green: 0.48, blue: 0.29))
                    .frame(width: 44, height: 16)
                    .rotationEffect(.degrees(-18))
                    .offset(y: -17)
            }

            Text(text)
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 6)
                .background(Color(red: 1.0, green: 0.54, blue: 0.22))
                .clipShape(Capsule())
        }
        .shadow(color: Color.orange.opacity(0.22), radius: 10, x: 0, y: 5)
    }

    private func isPlaceholderURL(_ value: String) -> Bool {
        value.contains("example.com")
    }

    private func badgeBackground(for badge: String) -> LinearGradient {
        switch badge {
        case "新剧":
            return LinearGradient(
                colors: [Color(red: 0.12, green: 0.79, blue: 0.65), Color(red: 0.05, green: 0.67, blue: 0.57)],
                startPoint: .leading,
                endPoint: .trailing
            )
        case "红果首发":
            return LinearGradient(
                colors: [Color(red: 1.0, green: 0.74, blue: 0.12), Color(red: 1.0, green: 0.5, blue: 0.08)],
                startPoint: .leading,
                endPoint: .trailing
            )
        default:
            return LinearGradient(
                colors: [Color(red: 1.0, green: 0.48, blue: 0.45), Color(red: 1.0, green: 0.35, blue: 0.37)],
                startPoint: .leading,
                endPoint: .trailing
            )
        }
    }

    private func tagBackground(for tag: String) -> Color {
        if tag.contains("榜") || tag.contains("热度") {
            return Color(red: 1.0, green: 0.96, blue: 0.91)
        }

        return Color(red: 0.97, green: 0.97, blue: 0.97)
    }

    private func tagColor(for tag: String) -> Color {
        if tag.contains("榜") || tag.contains("热度") {
            return Color(red: 0.97, green: 0.52, blue: 0.08)
        }

        return Color.secondary
    }

    private func shouldShowTagIcon(for tag: String) -> Bool {
        tag.contains("热播榜")
    }

    private func iconName(for tag: String) -> String {
        if tag.contains("热播榜") {
            return "crown.fill"
        }

        return "tag.fill"
    }
}

#Preview {
    TheaterDramaCardView(
        drama: TheaterDrama(
            id: "preview",
            title: "逆袭归来后我成了豪门团宠",
            description: "描述",
            coverUrl: nil,
            category: "都市",
            episodeCount: 68,
            tags: ["爆剧", "逆袭"],
            rating: 8.9,
            createdAt: "2026-07-25T00:00:00Z",
            updatedAt: "2026-07-25T00:00:00Z",
            heat: 98_210_000
        ),
        index: 0,
        onTap: {}
    )
}
