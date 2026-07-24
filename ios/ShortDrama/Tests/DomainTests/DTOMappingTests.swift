import Foundation
import Testing
@testable import ShortDrama

/// Domain layer entity tests — verifies Entity structs are pure Swift
/// with no framework dependencies beyond Foundation.
struct DTOMappingTests {

    @Test("Drama entity conforms to Codable")
    func testDramaIsCodable() {
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
        // Verify it can be encoded/decoded
        let encoder = JSONEncoder()
        let decoder = JSONDecoder()
        guard let data = try? encoder.encode(drama),
              let decoded = try? decoder.decode(Drama.self, from: data) else {
            Issue.record("Drama should be encodable and decodable")
            return
        }
        #expect(decoded.id == drama.id)
    }

    @Test("Drama entity conforms to Identifiable")
    func testDramaIsIdentifiable() {
        let drama = Drama(
            id: "unique-id",
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
        #expect(drama.id == "unique-id")
    }

    @Test("Drama entity conforms to Equatable")
    func testDramaIsEquatable() {
        let a = Drama(
            id: "1", title: "A", description: "D", coverUrl: "u",
            category: "c", episodeCount: 1, tags: nil, rating: nil,
            createdAt: "t1", updatedAt: "t2"
        )
        let b = Drama(
            id: "1", title: "A", description: "D", coverUrl: "u",
            category: "c", episodeCount: 1, tags: nil, rating: nil,
            createdAt: "t1", updatedAt: "t2"
        )
        #expect(a == b)
    }

    @Test("Drama entities with different IDs are not equal")
    func testDramaDifferentIDsNotEqual() {
        let a = Drama(
            id: "1", title: "A", description: "D", coverUrl: "u",
            category: "c", episodeCount: 1, tags: nil, rating: nil,
            createdAt: "t1", updatedAt: "t2"
        )
        let b = Drama(
            id: "2", title: "A", description: "D", coverUrl: "u",
            category: "c", episodeCount: 1, tags: nil, rating: nil,
            createdAt: "t1", updatedAt: "t2"
        )
        #expect(a != b)
    }

    @Test("Episode entity conforms to Codable and Identifiable")
    func testEpisodeCodableIdentifiable() {
        let episode = Episode(
            id: "ep-1", dramaId: "d-1", title: "E1",
            episodeNumber: 1, videoUrl: "https://v.example.com/1.mp4",
            duration: 180, thumbnailUrl: "https://img.example.com/1.jpg",
            createdAt: "2026-01-01T00:00:00Z",
            updatedAt: "2026-01-01T00:00:00Z"
        )
        let encoder = JSONEncoder()
        let decoder = JSONDecoder()
        guard let data = try? encoder.encode(episode),
              let decoded = try? decoder.decode(Episode.self, from: data) else {
            Issue.record("Episode should be encodable and decodable")
            return
        }
        #expect(decoded.id == episode.id)
        #expect(decoded.episodeNumber == 1)
    }

    @Test("Episode entities with same fields are equal")
    func testEpisodeEquatable() {
        let a = Episode(
            id: "ep-1", dramaId: "d-1", title: "E1",
            episodeNumber: 1, videoUrl: "v1", duration: 180,
            thumbnailUrl: "t1", createdAt: "c1", updatedAt: "u1"
        )
        let b = Episode(
            id: "ep-1", dramaId: "d-1", title: "E1",
            episodeNumber: 1, videoUrl: "v1", duration: 180,
            thumbnailUrl: "t1", createdAt: "c1", updatedAt: "u1"
        )
        #expect(a == b)
    }
}
