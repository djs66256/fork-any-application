import Foundation

/// Builds routes owned by the ranking feature.
enum RankingRouteBuilder {
    static func playRoute(for drama: RankingDrama) -> AppRoute? {
        guard !drama.id.isEmpty else { return nil }
        return .player(videoId: drama.id)
    }
}

struct RankingLoginContext: Equatable, Sendable {
    let source: String
    let contentType: RankingContentType
    let rankingType: RankingType
    let dramaID: String
}
