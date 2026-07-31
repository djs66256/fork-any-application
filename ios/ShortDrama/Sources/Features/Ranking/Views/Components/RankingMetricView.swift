import SwiftUI

struct RankingMetricView: View {
    let rankingType: RankingType
    let drama: RankingDrama

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: rankingType.iconName)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(rankingType.tint)

            Text(valueText)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(rankingType.tint)
        }
    }

    private var valueText: String {
        switch rankingType {
        case .hot:
            return "\(formattedCount(drama.playCount))热度"
        case .recommend:
            return "推荐值 \(String(format: "%.1f", drama.recommendationScore))"
        case .booking:
            return "\(formattedCount(drama.bookingCount))期待"
        }
    }

    private func formattedCount(_ value: Int) -> String {
        if value >= 10_000 {
            return String(format: "%.0f万", Double(value) / 10_000)
        }
        return "\(value)"
    }
}

private extension RankingType {
    var iconName: String {
        switch self {
        case .hot:
            return "flame.fill"
        case .recommend:
            return "star.fill"
        case .booking:
            return "bell.fill"
        }
    }

    var tint: Color {
        switch self {
        case .hot:
            return Color(red: 1.0, green: 0.49, blue: 0.18)
        case .recommend:
            return Color(red: 0.98, green: 0.66, blue: 0.17)
        case .booking:
            return Color(red: 1.0, green: 0.49, blue: 0.18)
        }
    }
}
