import Foundation

struct FetchRecentlyViewedUseCase: Sendable {
    private let repository: MenuPanelRepositoryProtocol

    init(repository: MenuPanelRepositoryProtocol) {
        self.repository = repository
    }

    func execute(playbackSessionId: String) async throws -> [RecentlyViewedItem] {
        try await repository.fetchRecentlyViewed(playbackSessionId: playbackSessionId)
    }
}
