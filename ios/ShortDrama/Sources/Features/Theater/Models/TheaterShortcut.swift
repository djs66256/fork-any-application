import Foundation
import SwiftUI

/// Supported quick shortcuts inside theater page.
enum TheaterShortcut: String, CaseIterable, Equatable, Sendable, Identifiable {
    case classification
    case ranking
    case newReleases
    case booking

    struct Style {
        let colors: [Color]
    }

    var id: String { rawValue }

    var title: String {
        switch self {
        case .classification:
            return "筛选"
        case .ranking:
            return "排行榜"
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
            return "flame.fill"
        case .newReleases:
            return "play.circle.fill"
        case .booking:
            return "circle.grid.2x2.fill"
        }
    }

    var style: Style {
        switch self {
        case .classification:
            return Style(colors: [Color(red: 0.76, green: 0.62, blue: 1.0), Color(red: 0.63, green: 0.55, blue: 0.95)])
        case .ranking:
            return Style(colors: [Color(red: 1.0, green: 0.66, blue: 0.24), Color(red: 0.98, green: 0.42, blue: 0.14)])
        case .newReleases:
            return Style(colors: [Color(red: 0.16, green: 0.84, blue: 0.83), Color(red: 0.16, green: 0.69, blue: 0.92)])
        case .booking:
            return Style(colors: [Color(red: 1.0, green: 0.79, blue: 0.38), Color(red: 0.95, green: 0.67, blue: 0.26)])
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
