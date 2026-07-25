import Foundation

/// Navigation destinations within the app.
enum AppRoute: Hashable {
    /// The main home screen.
    case home
    /// Player screen for a specific video.
    case player(videoId: String)
    /// Detail screen for a specific drama.
    case dramaDetail(dramaId: String)

    var owningTab: AppTab {
        switch self {
        case .home, .player, .dramaDetail:
            return .home
        }
    }

    var publicRouteName: String {
        switch self {
        case .home:
            return "home"
        case .player:
            return "play"
        case .dramaDetail:
            return "detail"
        }
    }
}
