import Foundation

struct BookingAssetDTO: Codable, Equatable {
    let dramaID: String
    let title: String
    let coverURL: String?
    let episodeCount: Int
    let bookedAt: String
    let availabilityStatus: BookingAssetAvailabilityStatus

    private enum CodingKeys: String, CodingKey {
        case dramaID = "dramaId"
        case title
        case coverURL = "coverUrl"
        case episodeCount
        case bookedAt
        case availabilityStatus
    }
}

extension BookingAssetDTO {
    func toEntity() -> BookingAsset {
        BookingAsset(
            dramaID: dramaID,
            title: title,
            coverURL: coverURL,
            episodeCount: episodeCount,
            bookedAt: bookedAt,
            availabilityStatus: availabilityStatus
        )
    }
}
