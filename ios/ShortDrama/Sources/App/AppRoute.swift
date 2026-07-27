import Foundation

/// Navigation destinations within the app.
enum AppRoute: Hashable {
    /// The main home screen.
    case home
    /// Search discovery home.
    case searchHome
    /// Search result page for a query.
    case searchResult(query: String)
    /// Ranking placeholder home.
    case rankingHome
    /// Classification placeholder home.
    case classificationHome
    /// New releases placeholder home.
    case newReleases
    /// Actor hub placeholder home.
    case actorHub
    /// Player screen for a specific video.
    case player(videoId: String)
    /// Detail screen for a specific drama.
    case dramaDetail(dramaId: String)

    var owningTab: AppTab {
        switch self {
        case .home,
             .searchHome,
             .searchResult,
             .rankingHome,
             .classificationHome,
             .newReleases,
             .actorHub,
             .player,
             .dramaDetail:
            return .home
        }
    }

    var publicRouteName: String {
        switch self {
        case .home:
            return "home"
        case .searchHome:
            return "search"
        case .searchResult:
            return "search/result"
        case .rankingHome:
            return "ranking"
        case .classificationHome:
            return "classification"
        case .newReleases:
            return "new-releases"
        case .actorHub:
            return "actors"
        case .player:
            return "play"
        case .dramaDetail:
            return "detail"
        }
    }
}
