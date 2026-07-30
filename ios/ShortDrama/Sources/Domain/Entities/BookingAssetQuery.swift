import Foundation

struct BookingAssetQuery: Equatable, Sendable {
    static let defaultPage = 1
    static let defaultPageSize = 20

    let status: BookingAssetAvailabilityStatus
    let page: Int
    let pageSize: Int

    init(
        status: BookingAssetAvailabilityStatus = .online,
        page: Int = BookingAssetQuery.defaultPage,
        pageSize: Int = BookingAssetQuery.defaultPageSize
    ) {
        self.status = status
        self.page = page
        self.pageSize = pageSize
    }
}
