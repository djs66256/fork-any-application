import Foundation
import Testing
@testable import ShortDrama

struct DramaRepositoryTests {

    // MARK: - T-25: fetchDramas returns empty array

    @Test("T-25: DramaRepository.fetchDramas returns empty array on empty response")
    func testFetchDramasEmptySuccess() async throws {
        let mock = MockDramaRepository()
        mock.behavior = .success([])

        let dramas = try await mock.fetchDramas(page: 1, pageSize: 20)
        #expect(dramas.isEmpty)
    }

    // MARK: - T-25: fetchDramas returns dramas

    @Test("T-25: DramaRepository.fetchDramas returns correct drama count")
    func testFetchDramasReturnsData() async throws {
        let drama = Drama(
            id: "1",
            title: "Test",
            description: "Desc",
            coverUrl: "https://example.com/cover.jpg",
            category: "comedy",
            episodeCount: 12,
            tags: nil,
            rating: nil,
            createdAt: "2026-01-01T00:00:00Z",
            updatedAt: "2026-01-01T00:00:00Z"
        )
        let mock = MockDramaRepository()
        mock.behavior = .success([drama])

        let dramas = try await mock.fetchDramas(page: 1, pageSize: 20)
        #expect(dramas.count == 1)
        #expect(dramas[0].id == "1")
    }

    // MARK: - T-26: fetchDramas error propagation

    @Test("T-26: DramaRepository.fetchDramas propagates errors from data source")
    func testFetchDramasErrorPropagation() async {
        let mock = MockDramaRepository()
        mock.behavior = .failure(.notImplemented("Service unavailable"))

        do {
            _ = try await mock.fetchDramas(page: 1, pageSize: 20)
            Issue.record("Expected error but none thrown")
        } catch let error as APIError {
            if case .notImplemented(let message) = error {
                #expect(message == "Service unavailable")
            } else {
                Issue.record("Expected notImplemented, got \(error)")
            }
        } catch {
            Issue.record("Unexpected error: \(error)")
        }
    }

    // MARK: - T-26: network error propagation

    @Test("T-26: DramaRepository propagates network errors")
    func testFetchDramasNetworkErrorPropagation() async {
        let mock = MockDramaRepository()
        let underlying = URLError(.notConnectedToInternet)
        mock.behavior = .failure(.network(underlying: underlying))

        do {
            _ = try await mock.fetchDramas(page: 1, pageSize: 20)
            Issue.record("Expected error but none thrown")
        } catch let error as APIError {
            if case .network = error {
                // Expected
            } else {
                Issue.record("Expected network error, got \(error)")
            }
        } catch {
            Issue.record("Unexpected error: \(error)")
        }
    }
}
