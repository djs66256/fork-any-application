import Foundation

/// Domain entity representing a theater feed card.
struct TheaterDrama: Identifiable, Equatable, Sendable {
    let id: String
    let title: String
    let description: String
    let coverUrl: String?
    let category: String
    let episodeCount: Int
    let tags: [String]?
    let rating: Double?
    let createdAt: String
    let updatedAt: String
    let heat: Int
}
