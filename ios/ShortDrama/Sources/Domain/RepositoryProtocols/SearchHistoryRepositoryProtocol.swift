import Foundation

/// Protocol defining local search history persistence.
protocol SearchHistoryRepositoryProtocol: Sendable {
    func load() -> [SearchHistoryItem]
    func save(keyword: String)
    func clear()
}
