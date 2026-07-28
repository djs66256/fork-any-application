import Foundation

/// Supported quick shortcuts inside theater page.
enum TheaterShortcut: String, CaseIterable, Equatable, Sendable, Identifiable {
    case classification
    case ranking
    case newReleases
    case booking

    var id: String { rawValue }

    var title: String {
        switch self {
        case .classification:
            return "筛选"
        case .ranking:
            return "排行"
        case .newReleases:
            return "新剧"
        case .booking:
            return "预约"
        }
    }

    var systemImage: String {
        switch self {
        case .classification:
            return "line.3.horizontal.decrease.circle.fill"
        case .ranking:
            return "chart.bar.fill"
        case .newReleases:
            return "sparkles.tv"
        case .booking:
            return "calendar.badge.plus"
        }
    }

    var rankingContext: TheaterRankingEntryContext? {
        switch self {
        case .classification, .newReleases:
            return nil
        case .ranking:
            return TheaterRankingEntryContext(rankingType: .hot)
        case .booking:
            return TheaterRankingEntryContext(rankingType: .booking)
        }
    }
}
