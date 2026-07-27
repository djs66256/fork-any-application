import Foundation

/// Use case for saving a normalized search keyword.
struct SaveSearchHistoryUseCase: Sendable {

    private let repository: SearchHistoryRepositoryProtocol

    init(repository: SearchHistoryRepositoryProtocol) {
        self.repository = repository
    }

    func execute(keyword: String) {
        repository.save(keyword: keyword)
    }
}
