import Foundation
@testable import ShortDrama

/// Mock implementation of DramaRepositoryProtocol for testing.
final class MockDramaRepository: DramaRepositoryProtocol, @unchecked Sendable {

    enum MockBehavior {
        case success([Drama])
        case failure(APIError)
        case delayed([Drama], TimeInterval)
    }

    var behavior: MockBehavior = .success([])
    var queuedBehaviors: [MockBehavior] = []
    private(set) var fetchDramasCallCount = 0
    private(set) var lastRequestedPage: Int?
    private(set) var lastRequestedPageSize: Int?

    func fetchDramas(page: Int, pageSize: Int) async throws -> [Drama] {
        fetchDramasCallCount += 1
        lastRequestedPage = page
        lastRequestedPageSize = pageSize

        let currentBehavior = queuedBehaviors.isEmpty ? behavior : queuedBehaviors.removeFirst()

        switch currentBehavior {
        case .success(let dramas):
            return dramas
        case .failure(let error):
            throw error
        case .delayed(let dramas, let delay):
            try await Task.sleep(for: .seconds(delay))
            return dramas
        }
    }

    func fetchDramaDetail(id: String) async throws -> Drama {
        switch behavior {
        case .success(let dramas):
            if let drama = dramas.first(where: { $0.id == id }) {
                return drama
            }
            throw APIError.notImplemented("Drama not found")
        case .failure(let error):
            throw error
        case .delayed:
            throw APIError.notImplemented("Not supported in delayed mode")
        }
    }
}
