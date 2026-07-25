import Foundation

/// Handles parsing and routing of deeplink URLs with the djsdrama:// scheme.
enum DeeplinkHandler {

    /// The expected URL scheme.
    private static let expectedScheme = "djsdrama"

    /// Parses a deeplink URL and returns the corresponding AppRoute.
    ///
    /// - Parameter url: The URL to parse.
    /// - Returns: The matched AppRoute, or nil if the URL is invalid or unsupported.
    static func handleDeepLink(_ url: URL) -> AppRoute? {
        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              components.scheme == expectedScheme else {
            return nil
        }

        let pathComponents = components.path
            .split(separator: "/")
            .map(String.init)

        switch components.host {
        case "open":
            return .home
        case "play":
            guard let videoId = pathComponents.first?.trimmingCharacters(in: .whitespacesAndNewlines),
                  !videoId.isEmpty else {
                return nil
            }
            return .player(videoId: videoId)
        case "drama":
            guard let dramaId = pathComponents.first?.trimmingCharacters(in: .whitespacesAndNewlines),
                  !dramaId.isEmpty else {
                return nil
            }
            return .dramaDetail(dramaId: dramaId)
        default:
            return nil
        }
    }
}
