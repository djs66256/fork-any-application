import Foundation

/// DTO for ranking list items.
struct RankingDramaDTO: Codable, Equatable {
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
    let contentType: RankingContentTypeDTO
    let playCount: Int
    let bookingCount: Int
    let recommendationScore: Double
    let isBooked: Bool
}

enum RankingContentTypeDTO: String, Codable, Equatable {
    case liveAction = "live_action"
    case ai

    func toEntity() -> RankingContentType {
        switch self {
        case .liveAction:
            return .liveAction
        case .ai:
            return .ai
        }
    }
}

extension RankingDramaDTO {
    func toEntity() -> RankingDrama {
        RankingDrama(
            id: id,
            title: title,
            description: description,
            coverUrl: coverUrl,
            category: category,
            episodeCount: episodeCount,
            tags: tags,
            rating: rating,
            createdAt: createdAt,
            updatedAt: updatedAt,
            contentType: contentType.toEntity(),
            playCount: playCount,
            bookingCount: bookingCount,
            recommendationScore: recommendationScore,
            isBooked: isBooked
        )
    }
}
