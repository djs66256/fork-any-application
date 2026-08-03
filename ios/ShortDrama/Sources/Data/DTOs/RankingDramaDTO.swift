import Foundation

/// DTO for ranking list items.
struct RankingDramaDTO: Codable, Equatable {
    let id: String
    let title: String
    let description: String?
    let coverUrl: String?
    let category: String?
    let episodeCount: Int?
    let tags: [String]?
    let rating: Double?
    let createdAt: String?
    let updatedAt: String?
    let contentType: RankingContentTypeDTO?
    let playCount: Int?
    let bookingCount: Int?
    let recommendationScore: Double?
    let isBooked: Bool?
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

    static func fallback(from category: String) -> RankingContentTypeDTO {
        let normalized = category.lowercased()
        if normalized.contains("ai") || normalized.contains("动画") || normalized.contains("动漫") || normalized.contains("漫剧") {
            return .ai
        }
        return .liveAction
    }
}

extension RankingDramaDTO {
    func toEntity() -> RankingDrama {
        let normalizedCategory = category?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let resolvedContentType = contentType?.toEntity()
            ?? RankingContentTypeDTO.fallback(from: normalizedCategory).toEntity()

        return RankingDrama(
            id: id,
            title: title,
            description: description?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "",
            coverUrl: coverUrl ?? "",
            category: normalizedCategory,
            episodeCount: episodeCount ?? 0,
            tags: tags?.filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty },
            rating: rating,
            createdAt: createdAt ?? "",
            updatedAt: updatedAt ?? "",
            contentType: resolvedContentType,
            playCount: playCount ?? 0,
            bookingCount: bookingCount ?? 0,
            recommendationScore: recommendationScore ?? 0,
            isBooked: isBooked ?? false
        )
    }
}
