import Foundation

/// Domain entity representing an episode within a drama series.
struct Episode: Codable, Identifiable, Equatable, Sendable {
    let id: String
    let dramaId: String
    let title: String
    let episodeNumber: Int
    let videoUrl: String
    let duration: Int
    let thumbnailUrl: String
    let description: String?
    let createdAt: String
    let updatedAt: String

    var isPlayable: Bool {
        !videoUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}
