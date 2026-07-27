import Foundation

struct RecentlyViewedItem: Equatable, Identifiable, Sendable {
    let dramaId: String
    let title: String
    let coverURL: String?
    let episodeNumber: Int
    let progress: Double
    let updatedAt: String

    var id: String { dramaId }

    var episodeProgressText: String {
        "已看到第 \(episodeNumber) 集"
    }

    var hasValidDramaId: Bool {
        !dramaId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}
