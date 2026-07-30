import Foundation

struct BookingAssetListResponseDTO: Codable, Equatable {
    let data: [BookingAssetDTO]
    let pagination: PaginationDTO
    let summary: BookingAssetSummaryDTO
}

extension BookingAssetListResponseDTO {
    func toEntity() -> BookingAssetPage {
        BookingAssetPage(
            items: data.map { $0.toEntity() },
            page: pagination.page,
            pageSize: pagination.pageSize,
            total: pagination.total,
            totalPages: pagination.totalPages,
            summary: summary.toEntity()
        )
    }
}
