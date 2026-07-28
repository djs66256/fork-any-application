import Foundation
@testable import ShortDrama
import Testing

struct MockDramaRepositoryTests {

    @Test("mock dramas repository returns configured items")
    func testFetchDramasReturnsConfiguredItems() async throws {
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

    @Test("mock dramas repository returns empty array on empty response")
    func testFetchDramasReturnsEmptyArray() async throws {
        let mock = MockDramaRepository()
        mock.behavior = .success([])

        let dramas = try await mock.fetchDramas(page: 1, pageSize: 20)

        #expect(dramas.isEmpty)
    }

    @Test("mock search repository tracks canonical query arguments")
    func testSearchTracksArguments() async throws {
        let mock = MockDramaRepository()
        let drama = Drama(
            id: "search-1",
            title: "逆袭",
            description: "",
            coverUrl: "https://example.com/cover.jpg",
            category: "都市",
            episodeCount: 8,
            tags: nil,
            rating: nil,
            createdAt: "2026-01-01T00:00:00Z",
            updatedAt: "2026-01-01T00:00:00Z"
        )
        mock.searchBehavior = .success([drama])

        let dramas = try await mock.searchDramas(query: "逆袭", page: 1, pageSize: 10)

        #expect(dramas.count == 1)
        #expect(mock.searchDramasCallCount == 1)
        #expect(mock.lastSearchQuery == "逆袭")
        #expect(mock.lastSearchPage == 1)
        #expect(mock.lastSearchPageSize == 10)
    }

    @Test("mock hot search repository returns configured items")
    func testFetchHotSearchesReturnsConfiguredItems() async throws {
        let mock = MockDramaRepository()
        mock.hotSearchBehavior = .success([
            HotSearchItem(rank: 1, keyword: "逆袭", score: 9821)
        ])

        let items = try await mock.fetchHotSearches()

        #expect(items.count == 1)
        #expect(items[0].keyword == "逆袭")
        #expect(mock.fetchHotSearchesCallCount == 1)
    }

    @Test("mock ranking repository tracks query arguments")
    func testRankingTracksArguments() async throws {
        let mock = MockDramaRepository()
        let result = PagedResult(
            items: [
                RankingDrama(
                    id: "ranking-1",
                    title: "排行短剧",
                    description: "描述",
                    coverUrl: "https://example.com/cover.jpg",
                    category: "都市",
                    episodeCount: 8,
                    tags: nil,
                    rating: nil,
                    createdAt: "2026-01-01T00:00:00Z",
                    updatedAt: "2026-01-01T00:00:00Z",
                    contentType: .all,
                    playCount: 1,
                    bookingCount: 2,
                    recommendationScore: 3,
                    isBooked: false
                )
            ],
            page: 1,
            pageSize: 10,
            total: 1,
            totalPages: 1
        )
        mock.rankingBehavior = .success(result)

        let query = RankingQuery(type: .recommend, contentType: .ai, page: 2, pageSize: 10)
        let fetched = try await mock.fetchRankings(query: query)

        #expect(fetched == result)
        #expect(mock.fetchRankingsCallCount == 1)
        #expect(mock.lastRankingQuery == query)
    }

    @Test("mock theater repository tracks query arguments")
    func testTheaterFeedTracksArguments() async throws {
        let mock = MockDramaRepository()
        let result = TheaterFeedPage(
            channel: .all,
            items: [
                TheaterDrama(
                    id: "theater-1",
                    title: "剧场短剧",
                    description: "描述",
                    coverUrl: "https://example.com/theater.jpg",
                    category: "都市",
                    episodeCount: 8,
                    tags: ["逆袭"],
                    rating: 8.6,
                    createdAt: "2026-01-01T00:00:00Z",
                    updatedAt: "2026-01-01T00:00:00Z",
                    heat: 12345
                )
            ],
            page: 2,
            pageSize: 20,
            total: 21,
            totalPages: 2
        )
        mock.theaterBehavior = .success(result)

        let query = TheaterFeedQuery(channel: .anime, page: 2, pageSize: 20)
        let fetched = try await mock.fetchTheaterFeed(query: query)

        #expect(fetched == result)
        #expect(mock.fetchTheaterFeedCallCount == 1)
        #expect(mock.lastTheaterQuery == query)
    }

    @Test("mock booking repository tracks drama id")
    func testBookingTracksArguments() async throws {
        let mock = MockDramaRepository()
        mock.bookingBehavior = .success(
            BookDramaResult(dramaID: "ranking-1", booked: true, bookingCount: 9)
        )

        let result = try await mock.bookDrama(id: "ranking-1")

        #expect(result.dramaID == "ranking-1")
        #expect(result.bookingCount == 9)
        #expect(mock.bookDramaCallCount == 1)
        #expect(mock.lastBookedDramaID == "ranking-1")
    }

    @Test("mock dramas repository propagates notImplemented errors")
    func testFetchDramasPropagatesNotImplementedError() async {
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

    @Test("mock dramas repository propagates network errors")
    func testFetchDramasPropagatesNetworkError() async {
        let mock = MockDramaRepository()
        let underlying = URLError(.notConnectedToInternet)
        mock.behavior = .failure(.network(underlying: underlying))

        do {
            _ = try await mock.fetchDramas(page: 1, pageSize: 20)
            Issue.record("Expected error but none thrown")
        } catch let error as APIError {
            if case .network = error {
            } else {
                Issue.record("Expected network error, got \(error)")
            }
        } catch {
            Issue.record("Unexpected error: \(error)")
        }
    }
}
