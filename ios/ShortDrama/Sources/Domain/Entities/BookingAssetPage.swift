import Foundation

struct BookingAssetPage: Equatable, Sendable {
    let items: [BookingAsset]
    let page: Int
    let pageSize: Int
    let total: Int
    let totalPages: Int
    let summary: BookingAssetSummary
}
