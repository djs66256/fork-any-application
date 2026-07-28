import Foundation

/// Initial ranking selection when entering from theater shortcuts.
struct TheaterRankingEntryContext: Equatable, Sendable {
    let contentType: RankingContentType
    let rankingType: RankingType

    init(contentType: RankingContentType = .all, rankingType: RankingType) {
        self.contentType = contentType
        self.rankingType = rankingType
    }
}
