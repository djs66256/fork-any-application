import Foundation

enum BookingAssetAvailabilityStatus: String, Codable, CaseIterable, Equatable, Sendable {
    case online
    case upcoming

    var title: String {
        switch self {
        case .online:
            return "已上线"
        case .upcoming:
            return "待上线"
        }
    }
}

struct BookingAsset: Identifiable, Equatable, Sendable {
    let dramaID: String
    let title: String
    let coverURL: String?
    let episodeCount: Int
    let bookedAt: String
    let availabilityStatus: BookingAssetAvailabilityStatus

    var id: String { dramaID }
}
