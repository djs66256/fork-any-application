import Foundation

/// Generic paged result wrapper used by list endpoints.
struct PagedResult<Item: Equatable & Sendable>: Equatable, Sendable {
    let items: [Item]
    let page: Int
    let pageSize: Int
    let total: Int
    let totalPages: Int
}
