import Foundation
@testable import ShortDrama

func makeTestDrama(id: String = "drama-001") -> Drama {
    Drama(
        id: id,
        title: "示例短剧",
        description: "首页卡片描述",
        coverUrl: "https://example.com/cover.jpg",
        category: "都市",
        episodeCount: 12,
        tags: ["逆袭"],
        rating: 8.6,
        createdAt: "2026-07-25T00:00:00Z",
        updatedAt: "2026-07-25T00:00:00Z"
    )
}

func makeTestRankingDrama(id: String = "ranking-001") -> RankingDrama {
    RankingDrama(
        id: id,
        title: "排行短剧",
        description: "排行榜短剧描述",
        coverUrl: "https://example.com/ranking.jpg",
        category: "都市",
        episodeCount: 68,
        tags: ["逆袭"],
        rating: 8.9,
        createdAt: "2026-07-25T00:00:00Z",
        updatedAt: "2026-07-25T00:00:00Z",
        contentType: .liveAction,
        playCount: 98210,
        bookingCount: 820,
        recommendationScore: 58930.6,
        isBooked: false
    )
}
