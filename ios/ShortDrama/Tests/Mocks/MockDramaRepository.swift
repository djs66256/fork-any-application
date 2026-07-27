import Foundation
@testable import ShortDrama

/// Mock implementation of DramaRepositoryProtocol for testing.
final class MockDramaRepository: DramaRepositoryProtocol, @unchecked Sendable {

    enum MockBehavior {
        case success([Drama])
        case failure(APIError)
        case delayed([Drama], TimeInterval)
    }

    enum HotSearchBehavior {
        case success([HotSearchItem])
        case failure(APIError)
        case delayed([HotSearchItem], TimeInterval)
    }

    var behavior: MockBehavior = .success([])
    var queuedBehaviors: [MockBehavior] = []

    var searchBehavior: MockBehavior = .success([])
    var queuedSearchBehaviors: [MockBehavior] = []

    var hotSearchBehavior: HotSearchBehavior = .success([])
    var queuedHotSearchBehaviors: [HotSearchBehavior] = []

    private(set) var fetchDramasCallCount = 0
    private(set) var lastRequestedPage: Int?
    private(set) var lastRequestedPageSize: Int?

    private(set) var searchDramasCallCount = 0
    private(set) var lastSearchQuery: String?
    private(set) var lastSearchPage: Int?
    private(set) var lastSearchPageSize: Int?

    private(set) var fetchHotSearchesCallCount = 0

    func fetchDramas(page: Int, pageSize: Int) async throws -> [Drama] {
        fetchDramasCallCount += 1
        lastRequestedPage = page
        lastRequestedPageSize = pageSize

        let currentBehavior = queuedBehaviors.isEmpty ? behavior : queuedBehaviors.removeFirst()
        return try await resolveDramaBehavior(currentBehavior)
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

    func searchDramas(query: String, page: Int, pageSize: Int) async throws -> [Drama] {
        searchDramasCallCount += 1
        lastSearchQuery = query
        lastSearchPage = page
        lastSearchPageSize = pageSize

        let currentBehavior = queuedSearchBehaviors.isEmpty
            ? searchBehavior
            : queuedSearchBehaviors.removeFirst()
        return try await resolveDramaBehavior(currentBehavior)
    }

    func fetchHotSearches() async throws -> [HotSearchItem] {
        fetchHotSearchesCallCount += 1

        let currentBehavior = queuedHotSearchBehaviors.isEmpty
            ? hotSearchBehavior
            : queuedHotSearchBehaviors.removeFirst()

        switch currentBehavior {
        case .success(let items):
            return items
        case .failure(let error):
            throw error
        case .delayed(let items, let delay):
            try await Task.sleep(for: .seconds(delay))
            return items
        }
    }

    private func resolveDramaBehavior(_ behavior: MockBehavior) async throws -> [Drama] {
        switch behavior {
        case .success(let dramas):
            return dramas
        case .failure(let error):
            throw error
        case .delayed(let dramas, let delay):
            try await Task.sleep(for: .seconds(delay))
            return dramas
        }
    }
}
