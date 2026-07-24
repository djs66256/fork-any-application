import Foundation

/// API response DTO for episode data.
struct EpisodeDTO: Codable, Equatable {
    let id: String
    let dramaId: String
    let title: String
    let episodeNumber: Int
    let videoUrl: String
    let duration: Int
    let thumbnailUrl: String
    let createdAt: String
    let updatedAt: String
}

extension EpisodeDTO {

    /// Converts this DTO to a domain entity.
    func toEntity() -> Episode {
        Episode(
            id: id,
            dramaId: dramaId,
            title: title,
            episodeNumber: episodeNumber,
            videoUrl: videoUrl,
            duration: duration,
            thumbnailUrl: thumbnailUrl,
            createdAt: createdAt,
            updatedAt: updatedAt
        )
    }
}
