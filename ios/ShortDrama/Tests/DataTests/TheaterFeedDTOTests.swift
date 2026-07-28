import Foundation
@testable import ShortDrama
import Testing

struct TheaterFeedDTOTests {

    private func makePayload() -> String {
        """
        {
          "data": [
            {
              "id": "theater-001",
              "title": "逆袭归来后我成了豪门团宠",
              "description": "剧场卡片描述",
              "cover_url": null,
              "category": "都市",
              "episode_count": 68,
              "tags": ["逆袭", "豪门"],
              "rating": 8.9,
              "created_at": "2026-07-25T00:00:00Z",
              "updated_at": "2026-07-25T00:00:00Z",
              "heat": 23000
            }
          ],
          "pagination": {
            "page": 1,
            "page_size": 20,
            "total": 12,
            "total_pages": 1
          }
        }
        """
    }

    @Test("T-01: theater endpoint builds canonical path and query items")
    func testTheaterEndpointBuildsCanonicalQuery() {
        let endpoint = DramaEndpoints.getTheaterFeed(
            query: TheaterFeedQuery(channel: .all, page: 1, pageSize: 20)
        )

        #expect(endpoint.path == "/api/dramas/channel")
        #expect(endpoint.method == .get)
        #expect(
            endpoint.queryItems == [
                URLQueryItem(name: "channel", value: "all"),
                URLQueryItem(name: "page", value: "1"),
                URLQueryItem(name: "pageSize", value: "20")
            ]
        )
    }

    @Test("T-01: theater DTO decodes heat and pagination into domain entity")
    func testTheaterDTOMapsToDomainEntity() throws {
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase

        let response = try decoder.decode(
            TheaterFeedResponseDTO.self,
            from: Data(makePayload().utf8)
        )
        let page = response.toEntity(channel: .all)

        #expect(page.channel == .all)
        #expect(page.items.count == 1)
        #expect(page.page == 1)
        #expect(page.pageSize == 20)
        #expect(page.total == 12)
        #expect(page.totalPages == 1)
        #expect(page.items[0].id == "theater-001")
        #expect(page.items[0].coverUrl == nil)
        #expect(page.items[0].heat == 23000)
        #expect(page.items[0].tags == ["逆袭", "豪门"])
    }

    @Test("T-09: theater heat formatter outputs stable Chinese short numbers")
    func testTheaterHeatFormatterOutputsChineseShortNumbers() {
        #expect(TheaterHeatFormatter.string(from: 980) == "980")
        #expect(TheaterHeatFormatter.string(from: 10_000) == "1万")
        #expect(TheaterHeatFormatter.string(from: 23_000) == "2.3万")
        #expect(TheaterHeatFormatter.string(from: 123_456_789) == "1.2亿")
    }
}
