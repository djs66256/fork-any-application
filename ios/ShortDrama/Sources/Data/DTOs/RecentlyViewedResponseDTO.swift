import Foundation

struct RecentlyViewedResponseDTO: Decodable, Equatable {
    let code: Int
    let data: RecentlyViewedDataDTO
    let message: String
}

struct RecentlyViewedDataDTO: Decodable, Equatable {
    let items: [RecentlyViewedItemDTO]
}

struct RecentlyViewedItemDTO: Decodable, Equatable {
    let dramaId: String
    let title: String
    let coverUrl: String?
    let episodeNumber: Int
    let progress: Double
    let updatedAt: String

    func toEntity() -> RecentlyViewedItem {
        RecentlyViewedItem(
            dramaId: dramaId,
            title: title,
            coverURL: coverUrl,
            episodeNumber: episodeNumber,
            progress: progress,
            updatedAt: updatedAt
        )
    }
}
