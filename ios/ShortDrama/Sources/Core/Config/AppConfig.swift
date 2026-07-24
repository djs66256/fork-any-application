import Foundation

/// Application configuration, reading values from Info.plist.
enum AppConfig {

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
    /// Falls back to localhost debug endpoint when not set.
    static func apiBaseURL(bundle: Bundle = .main) -> String {
        bundle.infoDictionary?["API_BASE_URL"] as? String ?? "http://localhost:3001"
    }
}
