import Foundation

/// Navigation destinations within the app.
enum AppRoute: Hashable, Sendable {
    /// The main home screen.
    case home
    /// Search discovery home.
    case searchHome
    /// Search result page for a query.
    case searchResult(query: String)
    /// Ranking home.
    case rankingHome
    /// Classification home.
    case classificationHome
    /// New releases placeholder home.
    case newReleases
    /// Actor hub placeholder home.
    case actorHub
    /// Player screen for a specific video.
    case player(videoId: String)
    /// Detail screen for a specific drama.
    case dramaDetail(dramaId: String)
    /// Mall login handoff placeholder.
    case mallLogin(context: MallLoginContext)
    /// Earn login handoff placeholder.
    case earnLogin(context: EarnLoginContext)
    /// Earn player handoff route.
    case earnPlayer(context: EarnTaskContext)
    /// Message center page pushed from the menu panel.
    case messages
    /// Placeholder page pushed from the menu panel.
    case menuPlaceholder(kind: MenuPlaceholderKind)
    /// Settings screen under profile tab.
    case settings

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
             .dramaDetail,
             .messages,
             .menuPlaceholder:
            return .home
        case .mallLogin:
            return .mall
        case .earnLogin,
             .earnPlayer:
            return .earn
        case .settings:
            return .profile
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
        case .mallLogin:
            return "mall/login"
        case .earnLogin:
            return "earn/login"
        case .earnPlayer:
            return "earn/player"
        case .messages:
            return "messages"
        case .menuPlaceholder:
            return "menu/placeholder"
        case .settings:
            return "profile/settings"
        }
    }
}
