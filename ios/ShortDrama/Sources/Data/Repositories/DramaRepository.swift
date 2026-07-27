import Foundation

/// Concrete implementation of DramaRepositoryProtocol
/// that fetches data from the remote API.
struct DramaRepository: DramaRepositoryProtocol, Sendable {

    private let dataSource: DramaRemoteDataSource

    init(dataSource: DramaRemoteDataSource = DramaRemoteDataSource()) {
        self.dataSource = dataSource
    }

    func fetchDramas(page: Int, pageSize: Int) async throws -> [Drama] {
        let dtos = try await dataSource.fetchDramas(page: page, pageSize: pageSize)
        return dtos.map { $0.toEntity() }
    }

    func fetchDramaDetail(id: String) async throws -> Drama {
        let dto = try await dataSource.fetchDrama(id: id)
        return dto.toEntity()
    }

    func searchDramas(query: String, page: Int, pageSize: Int) async throws -> [Drama] {
        let dtos = try await dataSource.searchDramas(query: query, page: page, pageSize: pageSize)
        return dtos.map { $0.toEntity() }
    }

    func fetchHotSearches() async throws -> [HotSearchItem] {
        let dtos = try await dataSource.fetchHotSearches()
        return dtos.map { $0.toEntity() }
    }

    func fetchRankings(query: RankingQuery) async throws -> PagedResult<RankingDrama> {
        let response = try await dataSource.fetchRankings(query: query)
        return response.toEntity()
    }

    func bookDrama(id: String) async throws -> BookDramaResult {
        let response = try await dataSource.bookDrama(id: id)
        return response.toEntity()
    }
}
