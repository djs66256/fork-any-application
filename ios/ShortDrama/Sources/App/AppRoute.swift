import Foundation

/// Navigation destinations within the app.
enum AppRoute: Hashable {
    /// The main home screen.
    case home
    /// Player screen for a specific video.
    case player(videoId: String)
    /// Detail screen for a specific drama.
    case dramaDetail(dramaId: String)
}
