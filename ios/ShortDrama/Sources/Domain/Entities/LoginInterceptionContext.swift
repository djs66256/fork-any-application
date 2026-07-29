import Foundation

struct LoginInterceptionContext: Equatable, Identifiable, Sendable {
    enum Source: String, Codable, Equatable, Sendable {
        case profileEntry
        case rankingBooking
        case messagesEntry
        case unknown
    }

    let source: Source
    let returnRoute: AppRoute?
    let rankingContext: RankingLoginContext?

    var id: String {
        let routeName = returnRoute?.publicRouteName ?? "none"
        let rankingID = rankingContext?.dramaID ?? "none"
        return "\(source.rawValue)-\(routeName)-\(rankingID)"
    }

    init(
        source: Source,
        returnRoute: AppRoute? = nil,
        rankingContext: RankingLoginContext? = nil
    ) {
        self.source = source
        self.returnRoute = returnRoute
        self.rankingContext = rankingContext
    }
}
