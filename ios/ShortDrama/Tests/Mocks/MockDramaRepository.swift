import Foundation
@testable import ShortDrama

/// Mock implementation of DramaRepositoryProtocol for testing.
final class MockDramaRepository: DramaRepositoryProtocol, @unchecked Sendable {

    enum DramaBehavior {
        case success([Drama])
        case failure(APIError)
        case delayed([Drama], TimeInterval)
    }

    enum HotSearchBehavior {
        case success([HotSearchItem])
        case failure(APIError)
        case delayed([HotSearchItem], TimeInterval)
    }

    enum ClassificationBehavior {
        case success(ClassificationTagsPayload)
        case failure(APIError)
        case delayed(ClassificationTagsPayload, TimeInterval)
    }

    enum RankingBehavior {
        case success(PagedResult<RankingDrama>)
        case failure(APIError)
        case delayed(PagedResult<RankingDrama>, TimeInterval)
    }

    enum TheaterBehavior {
        case success(TheaterFeedPage)
        case failure(APIError)
        case delayed(TheaterFeedPage, TimeInterval)
    }

    enum BookingBehavior {
        case success(BookDramaResult)
        case failure(APIError)
        case delayed(BookDramaResult, TimeInterval)
    }

    var behavior: DramaBehavior = .success([])
    var queuedBehaviors: [DramaBehavior] = []

    var searchBehavior: DramaBehavior = .success([])
    var queuedSearchBehaviors: [DramaBehavior] = []

    var hotSearchBehavior: HotSearchBehavior = .success([])
    var queuedHotSearchBehaviors: [HotSearchBehavior] = []

    var classificationBehavior: ClassificationBehavior = .success(
        ClassificationTagsPayload(
            gender: .all,
            dimensions: ClassificationDimensionKey.allCases.map {
                ClassificationDimension(key: $0, name: $0.title, tags: [])
            }
        )
    )
    var queuedClassificationBehaviors: [ClassificationBehavior] = []

    var rankingBehavior: RankingBehavior = .success(
        PagedResult(items: [], page: 1, pageSize: 10, total: 0, totalPages: 1)
    )
    var queuedRankingBehaviors: [RankingBehavior] = []

    var theaterBehavior: TheaterBehavior = .success(
        TheaterFeedPage(channel: .all, items: [], page: 1, pageSize: 20, total: 0, totalPages: 1)
    )
    var queuedTheaterBehaviors: [TheaterBehavior] = []

    var bookingBehavior: BookingBehavior = .success(
        BookDramaResult(dramaID: "", booked: true, bookingCount: 0)
    )
    var queuedBookingBehaviors: [BookingBehavior] = []

    private(set) var fetchDramasCallCount = 0
    private(set) var lastRequestedPage: Int?
    private(set) var lastRequestedPageSize: Int?

    private(set) var searchDramasCallCount = 0
    private(set) var lastSearchQuery: String?
    private(set) var lastSearchPage: Int?
    private(set) var lastSearchPageSize: Int?

    private(set) var fetchHotSearchesCallCount = 0

    private(set) var fetchClassificationTagsCallCount = 0
    private(set) var lastClassificationGender: ClassificationGender?

    private(set) var fetchRankingsCallCount = 0
    private(set) var lastRankingQuery: RankingQuery?

    private(set) var fetchTheaterFeedCallCount = 0
    private(set) var lastTheaterQuery: TheaterFeedQuery?

    private(set) var bookDramaCallCount = 0
    private(set) var lastBookedDramaID: String?

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

    func fetchClassificationTags(gender: ClassificationGender) async throws -> ClassificationTagsPayload {
        fetchClassificationTagsCallCount += 1
        lastClassificationGender = gender

        let currentBehavior = queuedClassificationBehaviors.isEmpty
            ? classificationBehavior
            : queuedClassificationBehaviors.removeFirst()

        switch currentBehavior {
        case .success(let payload):
            return payload
        case .failure(let error):
            throw error
        case .delayed(let payload, let delay):
            try await Task.sleep(for: .seconds(delay))
            return payload
        }
    }

    func fetchRankings(query: RankingQuery) async throws -> PagedResult<RankingDrama> {
        fetchRankingsCallCount += 1
        lastRankingQuery = query

        let currentBehavior = queuedRankingBehaviors.isEmpty
            ? rankingBehavior
            : queuedRankingBehaviors.removeFirst()

        switch currentBehavior {
        case .success(let result):
            return result
        case .failure(let error):
            throw error
        case .delayed(let result, let delay):
            try await Task.sleep(for: .seconds(delay))
            return result
        }
    }

    func fetchTheaterFeed(query: TheaterFeedQuery) async throws -> TheaterFeedPage {
        fetchTheaterFeedCallCount += 1
        lastTheaterQuery = query

        let currentBehavior = queuedTheaterBehaviors.isEmpty
            ? theaterBehavior
            : queuedTheaterBehaviors.removeFirst()

        switch currentBehavior {
        case .success(let result):
            return result
        case .failure(let error):
            throw error
        case .delayed(let result, let delay):
            try await Task.sleep(for: .seconds(delay))
            return result
        }
    }

    func bookDrama(id: String) async throws -> BookDramaResult {
        bookDramaCallCount += 1
        lastBookedDramaID = id

        let currentBehavior = queuedBookingBehaviors.isEmpty
            ? bookingBehavior
            : queuedBookingBehaviors.removeFirst()

        switch currentBehavior {
        case .success(let result):
            return result
        case .failure(let error):
            throw error
        case .delayed(let result, let delay):
            try await Task.sleep(for: .seconds(delay))
            return result
        }
    }

    private func resolveDramaBehavior(_ behavior: DramaBehavior) async throws -> [Drama] {
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
