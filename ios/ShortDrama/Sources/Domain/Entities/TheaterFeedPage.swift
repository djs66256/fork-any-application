import Foundation

/// Paged result for a theater channel feed.
struct TheaterFeedPage: Equatable, Sendable {
    let channel: TheaterChannel
    let items: [TheaterDrama]
    let page: Int
    let pageSize: Int
    let total: Int
    let totalPages: Int
}
