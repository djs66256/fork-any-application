import Foundation

/// API response for theater feed endpoint.
struct TheaterFeedResponseDTO: Codable, Equatable {
    let data: [TheaterDramaDTO]
    let pagination: PaginationDTO
}

struct TheaterDramaDTO: Codable, Equatable {
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

extension TheaterFeedResponseDTO {
    func toEntity(channel: TheaterChannel) -> TheaterFeedPage {
        TheaterFeedPage(
            channel: channel,
            items: data.map { $0.toEntity() },
            page: pagination.page,
            pageSize: pagination.pageSize,
            total: pagination.total,
            totalPages: pagination.totalPages
        )
    }
}

private extension TheaterDramaDTO {
    func toEntity() -> TheaterDrama {
        TheaterDrama(
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
            heat: heat
        )
    }
}
