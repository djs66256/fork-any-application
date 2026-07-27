import Foundation

/// Query used to fetch ranking content.
struct RankingQuery: Equatable, Sendable {
    let type: RankingType
    let contentType: RankingContentType
    let page: Int
    let pageSize: Int

    init(
        type: RankingType = .hot,
        contentType: RankingContentType = .all,
        page: Int = 1,
        pageSize: Int = 10
    ) {
        self.type = type
        self.contentType = contentType
        self.page = page
        self.pageSize = pageSize
    }
}
