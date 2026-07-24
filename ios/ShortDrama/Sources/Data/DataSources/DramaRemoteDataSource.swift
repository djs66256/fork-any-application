import Foundation

/// API response wrapper for the drama list endpoint.
struct DramaListResponse: Decodable {
    let code: Int
    let data: DramaListData
}

struct DramaListData: Decodable {
    let items: [DramaDTO]
    let pagination: PaginationDTO
}

/// Remote data source for drama API calls.
final class DramaRemoteDataSource: @unchecked Sendable {

    private let client: APIClient

    init(client: APIClient = .shared) {
        self.client = client
    }

    /// Fetches a paginated list of dramas from the remote API.
    func fetchDramas(page: Int, pageSize: Int) async throws -> [DramaDTO] {
        let endpoint = DramaEndpoints.GetDramas(page: page, pageSize: pageSize)
        let response: DramaListResponse = try await client.request(endpoint)
        return response.data.items
    }

    /// Fetches a single drama by ID from the remote API.
    func fetchDrama(id: String) async throws -> DramaDTO {
        let endpoint = DramaEndpoints.GetDrama(id: id)
        let response: DramaDetailResponse = try await client.request(endpoint)
        return response.data
    }
}

/// API response wrapper for a single drama detail.
struct DramaDetailResponse: Decodable {
    let code: Int
    let data: DramaDTO
}

/// API endpoint definitions for drama resources.
enum DramaEndpoints {

    struct GetDramas: APIEndpoint {
        typealias Response = DramaListResponse

        let page: Int
        let pageSize: Int

        var path: String { "/api/v1/dramas" }
        var method: HTTPMethod { .get }
        var queryItems: [URLQueryItem]? {
            [
                URLQueryItem(name: "page", value: String(page)),
                URLQueryItem(name: "page_size", value: String(pageSize))
            ]
        }
    }

    struct GetDrama: APIEndpoint {
        typealias Response = DramaDetailResponse

        let id: String

        var path: String { "/api/v1/dramas/\(id)" }
        var method: HTTPMethod { .get }
    }
}
