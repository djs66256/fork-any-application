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

    func fetchDramas(page: Int, pageSize: Int) async throws -> [Drama] {
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
