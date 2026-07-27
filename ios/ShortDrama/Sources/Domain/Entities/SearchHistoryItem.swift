import Foundation

/// Domain entity representing a local search history item.
struct SearchHistoryItem: Codable, Equatable, Sendable, Identifiable {
    let keyword: String

    var id: String { keyword }
}
