import Foundation

/// Use case for clearing local search history.
struct ClearSearchHistoryUseCase: Sendable {

    private let repository: SearchHistoryRepositoryProtocol

    init(repository: SearchHistoryRepositoryProtocol) {
        self.repository = repository
    }

    func execute() {
        repository.clear()
    }
}
