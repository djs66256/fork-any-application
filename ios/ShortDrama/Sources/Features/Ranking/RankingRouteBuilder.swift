import Foundation

/// Builds routes owned by the ranking feature.
enum RankingRouteBuilder {
    static func playRoute(for drama: RankingDrama) -> AppRoute? {
        guard !drama.id.isEmpty else { return nil }
        return .player(videoId: drama.id)
    }

    static func loginContext(for context: RankingLoginContext) -> LoginInterceptionContext {
        LoginInterceptionContext(
            source: .rankingBooking,
            returnRoute: .rankingHome,
            rankingContext: context
        )
    }
}

struct RankingLoginContext: Equatable, Sendable {
    let source: String
    let contentType: RankingContentType
    let rankingType: RankingType
    let dramaID: String
}
