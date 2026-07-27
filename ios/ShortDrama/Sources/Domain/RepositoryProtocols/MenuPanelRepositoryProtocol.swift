import Foundation

protocol MenuPanelRepositoryProtocol: Sendable {
    func fetchRecentlyViewed(playbackSessionId: String) async throws -> [RecentlyViewedItem]
}
