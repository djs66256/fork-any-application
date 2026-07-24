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
}
