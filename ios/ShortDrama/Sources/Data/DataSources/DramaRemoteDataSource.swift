import Foundation

/// API response for the drama list endpoint.
struct DramaListResponse: Decodable {
    let data: [DramaDTO]
    let pagination: PaginationDTO
}

/// API response wrapper for a single drama detail.
struct DramaDetailResponse: Decodable {
    let code: Int
    let data: DramaDTO
}

struct GetDramasEndpoint: APIEndpoint {
    typealias Response = DramaListResponse

    let page: Int
    let pageSize: Int

    var path: String { "/api/dramas" }
    var method: HTTPMethod { .get }
    var queryItems: [URLQueryItem]? {
        [
            URLQueryItem(name: "page", value: String(page)),
            URLQueryItem(name: "pageSize", value: String(pageSize))
        ]
    }
}

struct GetDramaEndpoint: APIEndpoint {
    typealias Response = DramaDetailResponse

    let id: String

    var path: String { "/api/dramas/\(id)" }
    var method: HTTPMethod { .get }
}

struct SearchDramasEndpoint: APIEndpoint {
    typealias Response = DramaListResponse

    let query: String
    let page: Int
    let pageSize: Int

    var path: String { "/api/dramas/search" }
    var method: HTTPMethod { .get }
    var queryItems: [URLQueryItem]? {
        [
            URLQueryItem(name: "q", value: query),
            URLQueryItem(name: "page", value: String(page)),
            URLQueryItem(name: "pageSize", value: String(pageSize))
        ]
    }
}

struct GetHotSearchesEndpoint: APIEndpoint {
    typealias Response = HotSearchListResponseDTO

    var path: String { "/api/dramas/hot-search" }
    var method: HTTPMethod { .get }
}

struct GetRankingsEndpoint: APIEndpoint {
    typealias Response = RankingListResponseDTO

    let query: RankingQuery

    var path: String { "/api/dramas/rankings" }
    var method: HTTPMethod { .get }
    var queryItems: [URLQueryItem]? {
        [
            URLQueryItem(name: "type", value: query.type.rawValue),
            URLQueryItem(name: "contentType", value: query.contentType.requestValue),
            URLQueryItem(name: "page", value: String(query.page)),
            URLQueryItem(name: "pageSize", value: String(query.pageSize))
        ]
    }
}

struct BookDramaEndpoint: APIEndpoint {
    typealias Response = BookDramaResponseDTO

    let dramaID: String

    var path: String { "/api/dramas/\(dramaID)/book" }
    var method: HTTPMethod { .post }
}

/// API endpoint definitions for drama resources.
enum DramaEndpoints {
    static func getDramas(page: Int, pageSize: Int) -> GetDramasEndpoint {
        GetDramasEndpoint(page: page, pageSize: pageSize)
    }

    static func getDrama(id: String) -> GetDramaEndpoint {
        GetDramaEndpoint(id: id)
    }

    static func searchDramas(query: String, page: Int, pageSize: Int) -> SearchDramasEndpoint {
        SearchDramasEndpoint(query: query, page: page, pageSize: pageSize)
    }

    static func getHotSearches() -> GetHotSearchesEndpoint {
        GetHotSearchesEndpoint()
    }

    static func getRankings(query: RankingQuery) -> GetRankingsEndpoint {
        GetRankingsEndpoint(query: query)
    }

    static func bookDrama(id: String) -> BookDramaEndpoint {
        BookDramaEndpoint(dramaID: id)
    }
}

/// Remote data source for drama API calls.
final class DramaRemoteDataSource: @unchecked Sendable {

    private let client: APIClient

    init(client: APIClient = .shared) {
        self.client = client
    }

    /// Fetches a paginated list of dramas from the remote API.
    func fetchDramas(page: Int, pageSize: Int) async throws -> [DramaDTO] {
        let endpoint = DramaEndpoints.getDramas(page: page, pageSize: pageSize)
        let response: DramaListResponse = try await client.request(endpoint)
        return response.data
    }

    /// Fetches a single drama by ID from the remote API.
    func fetchDrama(id: String) async throws -> DramaDTO {
        let endpoint = DramaEndpoints.getDrama(id: id)
        let response: DramaDetailResponse = try await client.request(endpoint)
        return response.data
    }

    /// Searches dramas by keyword.
    func searchDramas(query: String, page: Int, pageSize: Int) async throws -> [DramaDTO] {
        let endpoint = DramaEndpoints.searchDramas(query: query, page: page, pageSize: pageSize)
        let response: DramaListResponse = try await client.request(endpoint)
        return response.data
    }

    /// Fetches hot searches.
    func fetchHotSearches() async throws -> [HotSearchItemDTO] {
        let endpoint = DramaEndpoints.getHotSearches()
        let response: HotSearchListResponseDTO = try await client.request(endpoint)
        return response.data
    }

    /// Fetches rankings for the given query.
    func fetchRankings(query: RankingQuery) async throws -> RankingListResponseDTO {
        let endpoint = DramaEndpoints.getRankings(query: query)
        return try await client.request(endpoint)
    }

    /// Books a drama.
    func bookDrama(id: String) async throws -> BookDramaResponseDTO {
        let endpoint = DramaEndpoints.bookDrama(id: id)
        return try await client.request(endpoint)
    }
}
