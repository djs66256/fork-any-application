import Foundation

/// Supported quick entry kinds on the search discovery page.
enum QuickEntryType: String, Codable, Equatable, Sendable {
    case ranking
    case newReleases
    case classification
    case actorHub
}

/// UI model for search discovery quick entries.
struct QuickEntry: Equatable, Sendable, Identifiable {
    let type: QuickEntryType
    let title: String
    let systemImage: String

    var id: QuickEntryType { type }
}

extension QuickEntry {
    static let defaults: [QuickEntry] = [
        QuickEntry(type: .ranking, title: "排行", systemImage: "chart.bar.fill"),
        QuickEntry(type: .newReleases, title: "新剧", systemImage: "sparkles.tv"),
        QuickEntry(type: .classification, title: "分类", systemImage: "square.grid.2x2.fill"),
        QuickEntry(type: .actorHub, title: "演员", systemImage: "person.2.fill")
    ]
}
