import Foundation

struct MenuPanelRepository: MenuPanelRepositoryProtocol, Sendable {
    private let dataSource: PlayerRemoteDataSource

    init(dataSource: PlayerRemoteDataSource = PlayerRemoteDataSource()) {
        self.dataSource = dataSource
    }

    func fetchRecentlyViewed(playbackSessionId: String) async throws -> [RecentlyViewedItem] {
        let response = try await dataSource.fetchRecentlyViewed(playbackSessionId: playbackSessionId)
        return response.data.items.map { $0.toEntity() }
    }
}
