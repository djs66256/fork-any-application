import SwiftUI

struct RankingMetricView: View {
    let rankingType: RankingType
    let drama: RankingDrama

    var body: some View {
        HStack(spacing: 5) {
            Image(systemName: rankingType.iconName)
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(rankingType.tint)

            Text(valueText)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(rankingType.tint)
                .lineLimit(1)
        }
    }

    private var valueText: String {
        switch rankingType {
        case .hot:
            return "\(formattedMetricCount(drama.playCount))热度"
        case .recommend:
            return "\(formattedMetricCount(recommendDisplayValue))推荐"
        case .booking:
            return "\(formattedMetricCount(drama.bookingCount))期待"
        }
    }

    private var recommendDisplayValue: Int {
        max(drama.playCount / 6, Int(drama.recommendationScore * 10_000))
    }

    private func formattedMetricCount(_ value: Int) -> String {
        guard value >= 10_000 else { return "\(value)" }

        let decimalValue = Double(value) / 10_000
        let formatted = String(format: decimalValue >= 100 ? "%.0f万" : "%.1f万", decimalValue)
        return formatted.replacingOccurrences(of: ".0万", with: "万")
    }
}

private extension RankingType {
    var iconName: String {
        switch self {
        case .hot:
            return "flame.fill"
        case .recommend:
            return "flame.fill"
        case .booking:
            return "checkmark.seal.fill"
        }
    }

    var tint: Color {
        Color(red: 0.98, green: 0.45, blue: 0.16)
    }
}
