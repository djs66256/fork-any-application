import SwiftUI

struct RankingMetricView: View {
    let rankingType: RankingType
    let drama: RankingDrama

    private var label: String {
        switch rankingType {
        case .hot:
            return "热度"
        case .recommend:
            return "推荐值"
        case .booking:
            return "预约数"
        }
    }

    private var valueText: String {
        switch rankingType {
        case .hot:
            return "\(drama.playCount)"
        case .recommend:
            return String(format: "%.1f", drama.recommendationScore)
        case .booking:
            return "\(drama.bookingCount)"
        }
    }

    var body: some View {
        HStack(spacing: DesignTokens.Spacing.xs) {
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(valueText)
                .font(.caption.weight(.semibold))
                .foregroundStyle(.orange)
        }
    }
}
