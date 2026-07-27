import Foundation

/// Protocol defining drama data access operations.
protocol DramaRepositoryProtocol: Sendable {
    /// Fetches a paginated list of dramas.
    func fetchDramas(page: Int, pageSize: Int) async throws -> [Drama]

    /// Fetches detailed information for a specific drama.
    func fetchDramaDetail(id: String) async throws -> Drama

    /// Searches dramas by keyword.
    func searchDramas(query: String, page: Int, pageSize: Int) async throws -> [Drama]

    /// Fetches hot search items.
    func fetchHotSearches() async throws -> [HotSearchItem]
}
