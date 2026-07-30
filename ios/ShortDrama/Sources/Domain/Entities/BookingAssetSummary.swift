import Foundation

struct BookingAssetSummary: Equatable, Sendable {
    let onlineCount: Int
    let upcomingCount: Int

    static let empty = BookingAssetSummary(onlineCount: 0, upcomingCount: 0)

    func count(for status: BookingAssetAvailabilityStatus) -> Int {
        switch status {
        case .online:
            return onlineCount
        case .upcoming:
            return upcomingCount
        }
    }
}
