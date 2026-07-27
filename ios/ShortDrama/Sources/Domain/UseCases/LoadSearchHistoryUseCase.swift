import Foundation

/// Use case for loading local search history.
struct LoadSearchHistoryUseCase: Sendable {

    private let repository: SearchHistoryRepositoryProtocol

    init(repository: SearchHistoryRepositoryProtocol) {
        self.repository = repository
    }

    func execute() -> [SearchHistoryItem] {
        repository.load()
    }
}
