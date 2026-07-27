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

        return route(forHost: components.host, pathComponents: pathComponents)
    }

    private static func route(forHost host: String?, pathComponents: [String]) -> AppRoute? {
        switch host {
        case "open":
            return .home
        case "search":
            return handleSearchRoute(pathComponents: pathComponents)
        case "ranking":
            return .rankingHome
        case "classification":
            return .classificationHome
        case "new-releases":
            return .newReleases
        case "actors":
            return .actorHub
        case "play":
            return playerRoute(pathComponents: pathComponents)
        case "drama":
            return dramaDetailRoute(pathComponents: pathComponents)
        default:
            return nil
        }
    }

    private static func playerRoute(pathComponents: [String]) -> AppRoute? {
        guard let videoId = normalizedPathComponent(pathComponents.first) else {
            return nil
        }

        return .player(videoId: videoId)
    }

    private static func dramaDetailRoute(pathComponents: [String]) -> AppRoute? {
        guard let dramaId = normalizedPathComponent(pathComponents.first) else {
            return nil
        }

        return .dramaDetail(dramaId: dramaId)
    }

    private static func handleSearchRoute(pathComponents: [String]) -> AppRoute? {
        guard !pathComponents.isEmpty else {
            return .searchHome
        }

        guard pathComponents.first == "result",
              let rawQuery = pathComponents.dropFirst().first,
              let query = normalizedPathComponent(rawQuery) else {
            return nil
        }

        return .searchResult(query: query)
    }

    private static func normalizedPathComponent(_ component: String?) -> String? {
        guard let component else {
            return nil
        }

        let normalized = component
            .removingPercentEncoding?
            .trimmingCharacters(in: .whitespacesAndNewlines)

        guard let normalized, !normalized.isEmpty else {
            return nil
        }

        return normalized
    }
}
