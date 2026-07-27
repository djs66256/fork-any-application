import Foundation

/// DTO for a hot search item.
struct HotSearchItemDTO: Codable, Equatable {
    let rank: Int
    let keyword: String
    let score: Int
}

extension HotSearchItemDTO {
    func toEntity() -> HotSearchItem {
        HotSearchItem(rank: rank, keyword: keyword, score: score)
    }
}

/// API response for the hot search endpoint.
struct HotSearchListResponseDTO: Codable, Equatable {
    let data: [HotSearchItemDTO]
}
