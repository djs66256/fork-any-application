import Foundation
import Testing
@testable import ShortDrama

struct DramaDTOTests {

    // MARK: - T-09: DramaDTO JSON Decoding

    @Test("T-09: DramaDTO decodes from JSON correctly")
    func testDramaDTODecoding() throws {
        let json = """
        {
            "id": "drama-001",
            "title": "霸道总裁的独宠",
            "description": "一个精彩的故事",
            "cover_url": "https://img.example.com/cover001.jpg",
            "category": "romance",
            "episode_count": 24,
            "tags": ["霸总", "甜宠"],
            "rating": 4.8,
            "created_at": "2026-07-01T00:00:00Z",
            "updated_at": "2026-07-20T12:00:00Z"
        }
        """

        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        let dto = try decoder.decode(DramaDTO.self, from: Data(json.utf8))

        #expect(dto.id == "drama-001")
        #expect(dto.title == "霸道总裁的独宠")
        #expect(dto.description == "一个精彩的故事")
        #expect(dto.coverUrl == "https://img.example.com/cover001.jpg")
        #expect(dto.category == "romance")
        #expect(dto.episodeCount == 24)
        #expect(dto.tags == ["霸总", "甜宠"])
        #expect(dto.rating == 4.8)
        #expect(dto.createdAt == "2026-07-01T00:00:00Z")
        #expect(dto.updatedAt == "2026-07-20T12:00:00Z")
    }

    @Test("T-09: DramaDTO decodes with null optional fields")
    func testDramaDTODecodingNullOptionals() throws {
        let json = """
        {
            "id": "drama-002",
            "title": "Simple",
            "description": "A simple story",
            "cover_url": null,
            "category": "action",
            "episode_count": 12,
            "tags": null,
            "rating": null,
            "created_at": "2026-01-01T00:00:00Z",
            "updated_at": "2026-01-02T00:00:00Z"
        }
        """

        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        let dto = try decoder.decode(DramaDTO.self, from: Data(json.utf8))

        #expect(dto.id == "drama-002")
        #expect(dto.coverUrl == nil)
        #expect(dto.tags == nil)
        #expect(dto.rating == nil)
    }

    // MARK: - T-10: DTO to Entity Mapping

    @Test("T-10: DramaDTO.toEntity maps all fields correctly")
    func testDramaDTOToEntityMapping() {
        let dto = DramaDTO(
            id: "drama-001",
            title: "Test Drama",
            description: "Test description",
            coverUrl: "https://example.com/cover.jpg",
            category: "comedy",
            episodeCount: 36,
            tags: ["搞笑", "日常"],
            rating: 4.5,
            createdAt: "2026-07-01T00:00:00Z",
            updatedAt: "2026-07-20T12:00:00Z"
        )

        let entity = dto.toEntity()

        #expect(entity.id == dto.id)
        #expect(entity.title == dto.title)
        #expect(entity.description == dto.description)
        #expect(entity.coverUrl == dto.coverUrl)
        #expect(entity.category == dto.category)
        #expect(entity.episodeCount == dto.episodeCount)
        #expect(entity.tags == dto.tags)
        #expect(entity.rating == dto.rating)
        #expect(entity.createdAt == dto.createdAt)
        #expect(entity.updatedAt == dto.updatedAt)
    }

    @Test("T-10: DramaDTO.toEntity handles nil optionals")
    func testDramaDTOToEntityNilOptionals() {
        let dto = DramaDTO(
            id: "drama-002",
            title: "No Tags",
            description: "No tags story",
            coverUrl: nil,
            category: "horror",
            episodeCount: 8,
            tags: nil,
            rating: nil,
            createdAt: "2026-01-01T00:00:00Z",
            updatedAt: "2026-01-02T00:00:00Z"
        )

        let entity = dto.toEntity()

        #expect(entity.coverUrl.isEmpty)
        #expect(entity.tags == nil)
        #expect(entity.rating == nil)
    }

    // MARK: - EpisodeDTO

    @Test("EpisodeDTO decodes from JSON correctly")
    func testEpisodeDTODecoding() throws {
        let json = """
        {
            "id": "ep-001",
            "drama_id": "drama-001",
            "title": "第一集",
            "episode_number": 1,
            "video_url": "https://video.example.com/ep001.mp4",
            "duration": 180,
            "thumbnail_url": "https://img.example.com/ep001.jpg",
            "description": "第一集简介",
            "created_at": "2026-07-01T00:00:00Z",
            "updated_at": "2026-07-01T00:00:00Z"
        }
        """

        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        let dto = try decoder.decode(EpisodeDTO.self, from: Data(json.utf8))

        #expect(dto.id == "ep-001")
        #expect(dto.dramaId == "drama-001")
        #expect(dto.episodeNumber == 1)
        #expect(dto.duration == 180)
        #expect(dto.description == "第一集简介")
    }

    @Test("EpisodeDTO.toEntity maps all fields correctly")
    func testEpisodeDTOToEntityMapping() {
        let dto = EpisodeDTO(
            id: "ep-001",
            dramaId: "drama-001",
            title: "第一集",
            episodeNumber: 1,
            videoUrl: "https://video.example.com/ep001.mp4",
            duration: 180,
            thumbnailUrl: "https://img.example.com/ep001.jpg",
            description: "第一集简介",
            createdAt: "2026-07-01T00:00:00Z",
            updatedAt: "2026-07-01T00:00:00Z"
        )

        let entity = dto.toEntity()

        #expect(entity.id == dto.id)
        #expect(entity.dramaId == dto.dramaId)
        #expect(entity.title == dto.title)
        #expect(entity.episodeNumber == dto.episodeNumber)
        #expect(entity.videoUrl == dto.videoUrl)
        #expect(entity.duration == dto.duration)
        #expect(entity.thumbnailUrl == dto.thumbnailUrl)
        #expect(entity.description == dto.description)
    }
}
