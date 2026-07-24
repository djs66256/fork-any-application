import Foundation

/// API response DTO for drama data.
struct DramaDTO: Codable, Equatable {
    let id: String
    let title: String
    let description: String
    let coverUrl: String
    let category: String
    let episodeCount: Int
    let tags: [String]?
    let rating: Double?
    let createdAt: String
    let updatedAt: String
}

extension DramaDTO {

    /// Converts this DTO to a domain entity.
    func toEntity() -> Drama {
        Drama(
            id: id,
            title: title,
            description: description,
            coverUrl: coverUrl,
            category: category,
            episodeCount: episodeCount,
            tags: tags,
            rating: rating,
            createdAt: createdAt,
            updatedAt: updatedAt
        )
    }
}
