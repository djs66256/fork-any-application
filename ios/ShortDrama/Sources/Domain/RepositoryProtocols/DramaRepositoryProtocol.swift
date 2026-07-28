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

    /// Fetches classification tags for the selected gender.
    func fetchClassificationTags(gender: ClassificationGender) async throws -> ClassificationTagsPayload

    /// Fetches ranking data for the given query.
    func fetchRankings(query: RankingQuery) async throws -> PagedResult<RankingDrama>

    /// Fetches theater feed data for the given query.
    func fetchTheaterFeed(query: TheaterFeedQuery) async throws -> TheaterFeedPage

    /// Books a drama for the current user.
    func bookDrama(id: String) async throws -> BookDramaResult
}
