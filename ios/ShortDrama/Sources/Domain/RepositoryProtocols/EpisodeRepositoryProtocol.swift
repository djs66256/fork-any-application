import Foundation

/// Protocol defining episode data access operations.
protocol EpisodeRepositoryProtocol {
    /// Fetches a specific episode by ID.
    func fetchEpisode(id: String) async throws -> Episode
}
