import Foundation

/// Response payload for ranking list endpoints.
struct RankingListResponseDTO: Codable, Equatable {
    let data: [RankingDramaDTO]
    let pagination: PaginationDTO
}

extension RankingListResponseDTO {
    func toEntity() -> PagedResult<RankingDrama> {
        PagedResult(
            items: data.map { $0.toEntity() },
            page: pagination.page,
            pageSize: pagination.pageSize,
            total: pagination.total,
            totalPages: pagination.totalPages
        )
    }
}
