import Foundation

/// Application configuration, reading values from Info.plist.
enum AppConfig {
    private static let mallHomePath = "/mall"
    private static let earnHomePath = "/earn"

    /// Display name from CFBundleDisplayName or CFBundleName.
    static func appName(bundle: Bundle = .main) -> String {
        let info = bundle.infoDictionary
        return (info?["CFBundleDisplayName"] as? String)
            ?? (info?["CFBundleName"] as? String)
            ?? ""
    }

    /// Marketing version from CFBundleShortVersionString.
    static func appVersion(bundle: Bundle = .main) -> String {
        bundle.infoDictionary?["CFBundleShortVersionString"] as? String ?? ""
    }

    /// Build number from CFBundleVersion.
    static func buildNumber(bundle: Bundle = .main) -> String {
        bundle.infoDictionary?["CFBundleVersion"] as? String ?? ""
    }

    /// API base URL injected via Info.plist key API_BASE_URL.
    static func apiBaseURL(bundle: Bundle = .main) -> String {
        bundle.infoDictionary?["API_BASE_URL"] as? String ?? ""
    }

    /// Mall H5 base URL injected via Info.plist key MALL_BASE_URL.
    static func mallBaseURL(bundle: Bundle = .main) -> String {
        bundle.infoDictionary?["MALL_BASE_URL"] as? String ?? ""
    }

    /// Earn H5 base URL injected via Info.plist key EARN_BASE_URL.
    static func earnBaseURL(bundle: Bundle = .main) -> String {
        bundle.infoDictionary?["EARN_BASE_URL"] as? String ?? ""
    }

    /// Canonical mall home URL used by the native mall container.
    static func mallHomeURL(bundle: Bundle = .main) -> URL? {
        makeURL(baseURL: mallBaseURL(bundle: bundle), path: mallHomePath)
    }

    /// Canonical earn home URL used by the native earn container.
    static func earnHomeURL(bundle: Bundle = .main) -> URL? {
        makeURL(baseURL: earnBaseURL(bundle: bundle), path: earnHomePath)
    }

    static func makeURL(baseURL: String, path: String) -> URL? {
        guard var components = URLComponents(string: baseURL) else {
            return nil
        }

        let trimmedPath = path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        if trimmedPath.isEmpty {
            components.path = "/"
        } else {
            components.path = "/\(trimmedPath)"
        }

        return components.url
    }
}
