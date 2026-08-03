import Foundation

/// Response payload for booking a drama.
struct BookDramaResponseDTO: Codable, Equatable {
    let dramaID: String
    let booked: Bool
    let bookingCount: Int

    private enum CodingKeys: String, CodingKey {
        case dramaID
        case booked
        case bookingCount
    }
}

extension BookDramaResponseDTO {
    func toEntity() -> BookDramaResult {
        BookDramaResult(dramaID: dramaID, booked: booked, bookingCount: bookingCount)
    }
}
