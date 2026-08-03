import SwiftUI

struct RankingDramaCardView: View {
    let drama: RankingDrama
    let rankingType: RankingType
    let rankIndex: Int
    let onTapCard: () -> Void
    let onTapBooking: () -> Void

    private var metaLine: String {
        let trimmed = drama.category.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? "短剧" : trimmed.replacingOccurrences(of: " · ", with: "·")
    }

    private var descriptionText: String {
        drama.description.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var bookingMetaText: String {
        let suffix = rankIndex <= 3 ? "预计11月上线" : (rankIndex <= 5 ? "预计12月上线" : "预计近期上线")
        return "预告 · \(formattedMetricCount(drama.bookingCount))人预约 · \(suffix)"
    }

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            rankingBadge
                .padding(.top, 2)

            RankingCoverView(drama: drama, rankingType: rankingType, rankIndex: rankIndex)
                .onTapGesture(perform: onTapCard)

            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top, spacing: 12) {
                    VStack(alignment: .leading, spacing: 7) {
                        Text(drama.title)
                            .font(.system(size: 17, weight: .bold))
                            .foregroundStyle(Color.black)
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)

                        Text(metaLine)
                            .font(.system(size: 13.5))
                            .foregroundStyle(Color.black.opacity(0.34))
                            .lineLimit(1)
                    }

                    Spacer(minLength: 0)

                    trailingMetric
                }

                if !descriptionText.isEmpty {
                    Text("“\(descriptionText)”")
                        .font(.system(size: 13))
                        .foregroundStyle(Color.black.opacity(0.36))
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                }

                footerContent
            }
            .contentShape(Rectangle())
            .onTapGesture(perform: onTapCard)
        }
        .padding(.vertical, 14)
    }

    @ViewBuilder
    private var trailingMetric: some View {
        HStack(spacing: 4) {
            Image(systemName: rankingType == .booking ? "checkmark.seal.fill" : "flame.fill")
                .font(.system(size: 12, weight: .bold))
            Text(trailingMetricText)
                .font(.system(size: 13, weight: .semibold))
        }
        .foregroundStyle(Color(red: 0.98, green: 0.45, blue: 0.16))
        .fixedSize()
        .padding(.top, 3)
    }

    private var trailingMetricText: String {
        switch rankingType {
        case .hot:
            return "\(formattedMetricCount(drama.playCount))热度"
        case .recommend:
            return "\(formattedMetricCount(max(drama.playCount / 6, Int(drama.recommendationScore * 10_000))))推荐"
        case .booking:
            return "\(formattedMetricCount(drama.bookingCount))期待"
        }
    }

    @ViewBuilder
    private var footerContent: some View {
        if rankingType == .booking {
            HStack(spacing: 8) {
                Text(bookingMetaText)
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(Color.black.opacity(0.38))
                    .lineLimit(1)

                Spacer(minLength: 0)

                RankingBookingButton(
                    booked: drama.isBooked,
                    isSubmitting: drama.isBookingSubmitting,
                    action: onTapBooking
                )
            }
            .padding(.horizontal, 12)
            .frame(height: 40)
            .background {
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(Color(red: 0.97, green: 0.97, blue: 0.97))
            }
        } else {
            HStack(spacing: 8) {
                ForEach(footerBadges, id: \.text) { badge in
                    footerBadge(text: badge.text, style: badge.style)
                }
            }
        }
    }

    private var footerBadges: [FooterBadge] {
        var badges: [FooterBadge] = []

        if rankIndex == 2 || rankIndex == 4 || rankIndex == 5 {
            badges.append(.init(text: "新剧", style: .mint))
        }

        if let rating = drama.rating {
            badges.append(.init(text: "评分\(String(format: "%.1f", rating))", style: .rating))
        }

        switch rankingType {
        case .hot:
            badges.append(.init(text: "\(formattedMetricCount(drama.bookingCount / 10))收藏", style: .plain))
            badges.append(.init(text: "\(formattedMetricCount(max(drama.playCount / 6, Int(drama.recommendationScore * 10_000))))次点赞", style: .plain))
        case .recommend:
            badges.append(.init(text: "\(formattedMetricCount(drama.playCount))热度", style: .plain))
            badges.append(.init(text: "\(formattedMetricCount(max(drama.playCount / 11, 1)))次点赞", style: .plain))
        case .booking:
            break
        }

        return Array(badges.prefix(3))
    }

    private func footerBadge(text: String, style: FooterBadge.Style) -> some View {
        HStack(spacing: style == .rating ? 4 : 0) {
            if style == .rating {
                Image(systemName: "seal.fill")
                    .font(.system(size: 10, weight: .bold))
            }

            Text(text)
                .font(.system(size: 11.5, weight: .medium))
                .lineLimit(1)
        }
        .foregroundStyle(style.foreground)
        .padding(.horizontal, 8)
        .frame(height: 28)
        .background {
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(style.background)
        }
    }

    private var rankingBadge: some View {
        ZStack(alignment: .topLeading) {
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(rankBadgeBackground)
                .frame(width: 34, height: 34)

            Text("\(rankIndex)")
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(Color.white)
                .frame(width: 34, height: 34)
        }
    }

    private var rankBadgeBackground: Color {
        switch rankIndex {
        case 1:
            return Color(red: 1.0, green: 0.69, blue: 0.24)
        case 2:
            return Color(red: 0.13, green: 0.84, blue: 0.70)
        case 3:
            return Color(red: 0.30, green: 0.58, blue: 1.0)
        default:
            return Color(red: 0.54, green: 0.54, blue: 0.58)
        }
    }

    private func formattedMetricCount(_ value: Int) -> String {
        guard value >= 10_000 else { return "\(value)" }

        let decimalValue = Double(value) / 10_000
        let formatted = String(format: "%.1f万", decimalValue)
        return formatted.replacingOccurrences(of: ".0万", with: "万")
    }
}

private struct FooterBadge {
    enum Style {
        case plain
        case rating
        case mint

        var foreground: Color {
            switch self {
            case .plain, .rating:
                return Color(red: 0.84, green: 0.63, blue: 0.39)
            case .mint:
                return Color(red: 0.08, green: 0.75, blue: 0.63)
            }
        }

        var background: Color {
            switch self {
            case .plain, .rating:
                return Color(red: 0.99, green: 0.96, blue: 0.91)
            case .mint:
                return Color(red: 0.90, green: 0.99, blue: 0.96)
            }
        }
    }

    let text: String
    let style: Style
}

private struct RankingCoverView: View {
    let drama: RankingDrama
    let rankingType: RankingType
    let rankIndex: Int

    var body: some View {
        Group {
            if let assetName = referenceAssetName {
                Image(assetName)
                    .resizable()
                    .scaledToFill()
            } else if let url = URL(string: drama.coverUrl), !drama.coverUrl.isEmpty {
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
        .frame(width: 104, height: 145)
        .clipped()
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var referenceAssetName: String? {
        if let index = coverReferenceIndex(prefix: "asset://ranking-hot-") {
            return "RankingHotPoster\(index)"
        }

        if let index = coverReferenceIndex(prefix: "asset://ranking-booking-") {
            return "RankingBookingPoster\(index)"
        }

        return nil
    }

    private func coverReferenceIndex(prefix: String) -> Int? {
        guard drama.coverUrl.hasPrefix(prefix) else { return nil }
        return Int(drama.coverUrl.replacingOccurrences(of: prefix, with: ""))
    }

    private var placeholder: some View {
        ZStack(alignment: .bottomLeading) {
            LinearGradient(
                colors: placeholderGradient,
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )

            VStack(alignment: .leading, spacing: 4) {
                Text(categoryPrefix)
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(Color.white.opacity(0.82))
                    .lineLimit(1)

                Text(shortTitle)
                    .font(.system(size: 16, weight: .black))
                    .foregroundStyle(Color.white)
                    .lineLimit(3)
            }
            .padding(10)
            .shadow(color: Color.black.opacity(0.16), radius: 6, x: 0, y: 3)
        }
    }

    private var categoryPrefix: String {
        let normalized = drama.category
            .replacingOccurrences(of: " · ", with: "·")
            .split(separator: "·")
            .first
            .map(String.init) ?? "热榜"
        return normalized.isEmpty ? "热榜" : normalized
    }

    private var shortTitle: String {
        drama.title.replacingOccurrences(of: "，", with: "\n")
    }

    private var placeholderGradient: [Color] {
        switch rankIndex % 5 {
        case 1:
            return [Color(red: 0.16, green: 0.14, blue: 0.22), Color(red: 0.45, green: 0.20, blue: 0.30), Color(red: 0.88, green: 0.55, blue: 0.26)]
        case 2:
            return [Color(red: 0.05, green: 0.18, blue: 0.12), Color(red: 0.15, green: 0.36, blue: 0.18), Color(red: 0.88, green: 0.79, blue: 0.44)]
        case 3:
            return [Color(red: 0.18, green: 0.07, blue: 0.10), Color(red: 0.46, green: 0.13, blue: 0.12), Color(red: 0.96, green: 0.48, blue: 0.12)]
        case 4:
            return [Color(red: 0.14, green: 0.18, blue: 0.31), Color(red: 0.30, green: 0.46, blue: 0.73), Color(red: 0.80, green: 0.89, blue: 0.96)]
        default:
            return [Color(red: 0.30, green: 0.11, blue: 0.18), Color(red: 0.58, green: 0.18, blue: 0.24), Color(red: 0.97, green: 0.78, blue: 0.44)]
        }
    }
}
