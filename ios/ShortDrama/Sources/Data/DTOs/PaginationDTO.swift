import Foundation

/// DTO for pagination metadata in API responses.
struct PaginationDTO: Codable, Equatable {
    let page: Int
    let pageSize: Int
    let total: Int
    let totalPages: Int
}
