import Foundation

/// Domain entity representing a hot search item.
struct HotSearchItem: Codable, Equatable, Sendable, Identifiable {
    let rank: Int
    let keyword: String
    let score: Int

    var id: String { "\(rank)-\(keyword)" }
}
