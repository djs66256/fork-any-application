import Foundation
import SwiftUI

/// Supported quick entry kinds on the search discovery page.
enum QuickEntryType: String, Codable, Equatable, Sendable {
    case imageSearch
    case ranking
    case newReleases
    case actorHub
    case classification
}

/// UI model for search discovery quick entries.
struct QuickEntry: Equatable, Sendable, Identifiable {
    let type: QuickEntryType
    let title: String
    let systemImage: String
    let accentColor: Color
    let symbolBackgroundColor: Color

    var id: QuickEntryType { type }
}

extension QuickEntry {
    static let defaults: [QuickEntry] = [
        QuickEntry(
            type: .imageSearch,
            title: "识剧",
            systemImage: "camera.fill",
            accentColor: Color(red: 0.03, green: 0.72, blue: 0.77),
            symbolBackgroundColor: Color(red: 0.86, green: 0.97, blue: 0.96)
        ),
        QuickEntry(
            type: .ranking,
            title: "排行",
            systemImage: "flame.fill",
            accentColor: Color(red: 1.0, green: 0.48, blue: 0.14),
            symbolBackgroundColor: Color(red: 1.0, green: 0.94, blue: 0.86)
        ),
        QuickEntry(
            type: .newReleases,
            title: "上新",
            systemImage: "play.fill",
            accentColor: Color(red: 0.12, green: 0.78, blue: 0.83),
            symbolBackgroundColor: Color(red: 0.87, green: 0.98, blue: 0.99)
        ),
        QuickEntry(
            type: .actorHub,
            title: "演员",
            systemImage: "person.fill",
            accentColor: Color(red: 0.95, green: 0.70, blue: 0.29),
            symbolBackgroundColor: Color(red: 1.0, green: 0.96, blue: 0.86)
        ),
        QuickEntry(
            type: .classification,
            title: "分类",
            systemImage: "square.grid.2x2.fill",
            accentColor: Color(red: 0.61, green: 0.51, blue: 0.96),
            symbolBackgroundColor: Color(red: 0.93, green: 0.89, blue: 0.99)
        )
    ]
}
