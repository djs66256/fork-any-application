import Foundation

struct BookingAssetSummaryDTO: Codable, Equatable {
    let onlineCount: Int
    let upcomingCount: Int
}

extension BookingAssetSummaryDTO {
    func toEntity() -> BookingAssetSummary {
        BookingAssetSummary(
            onlineCount: onlineCount,
            upcomingCount: upcomingCount
        )
    }
}
