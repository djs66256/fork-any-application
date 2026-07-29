import Foundation
import Testing
@testable import ShortDrama

struct AppConfigTests {

    /// Creates a mock bundle with given Info.plist dictionary for testing.
    private func makeBundle(info: [String: Any]) -> Bundle {
        let tempDir = NSTemporaryDirectory()
        let bundlePath = (tempDir as NSString).appendingPathComponent(UUID().uuidString)
        try? FileManager.default.createDirectory(
            atPath: bundlePath,
            withIntermediateDirectories: true
        )
        // Write a minimal Info.plist
        let plistPath = (bundlePath as NSString).appendingPathComponent("Info.plist")
        let data = try? PropertyListSerialization.data(
            fromPropertyList: info,
            format: .xml,
            options: 0
        )
        try? data?.write(to: URL(fileURLWithPath: plistPath))
        return Bundle(path: bundlePath) ?? .main
    }

    @Test("T-03: appName returns correct value from CFBundleDisplayName")
    func testAppNameFromDisplayName() {
        let bundle = makeBundle(info: [
            "CFBundleDisplayName": "ShortDrama",
            "CFBundleName": "ShortDramaInternal"
        ])
        #expect(AppConfig.appName(bundle: bundle) == "ShortDrama")
    }

    @Test("T-03: appName falls back to CFBundleName when display name missing")
    func testAppNameFallbackToBundleName() {
        let bundle = makeBundle(info: [
            "CFBundleName": "ShortDramaInternal"
        ])
        #expect(AppConfig.appName(bundle: bundle) == "ShortDramaInternal")
    }

    @Test("T-03: appName returns empty string when both names missing")
    func testAppNameEmptyWhenMissing() {
        let bundle = makeBundle(info: [:])
        #expect(AppConfig.appName(bundle: bundle) == "")
    }

    @Test("T-04: appVersion returns correct value")
    func testAppVersion() {
        let bundle = makeBundle(info: [
            "CFBundleShortVersionString": "0.1.0"
        ])
        #expect(AppConfig.appVersion(bundle: bundle) == "0.1.0")
    }

    @Test("T-04: appVersion returns empty string when missing")
    func testAppVersionMissing() {
        let bundle = makeBundle(info: [:])
        #expect(AppConfig.appVersion(bundle: bundle) == "")
    }

    @Test("buildNumber returns correct value")
    func testBuildNumber() {
        let bundle = makeBundle(info: [
            "CFBundleVersion": "1"
        ])
        #expect(AppConfig.buildNumber(bundle: bundle) == "1")
    }

    @Test("apiBaseURL returns correct value from Info.plist")
    func testAPIBaseURL() {
        let bundle = makeBundle(info: [
            "API_BASE_URL": "https://api.example.com"
        ])
        #expect(AppConfig.apiBaseURL(bundle: bundle) == "https://api.example.com")
    }

    @Test("apiBaseURL returns empty string when missing")
    func testAPIBaseURLFallback() {
        let bundle = makeBundle(info: [:])
        #expect(AppConfig.apiBaseURL(bundle: bundle) == "")
    }

    @Test("mallBaseURL returns correct value from Info.plist")
    func testMallBaseURL() {
        let bundle = makeBundle(info: [
            "MALL_BASE_URL": "https://app.example.com"
        ])
        #expect(AppConfig.mallBaseURL(bundle: bundle) == "https://app.example.com")
    }

    @Test("mallBaseURL returns empty string when missing")
    func testMallBaseURLFallback() {
        let bundle = makeBundle(info: [:])
        #expect(AppConfig.mallBaseURL(bundle: bundle) == "")
    }

    @Test("mallHomeURL appends canonical mall route")
    func testMallHomeURL() {
        let bundle = makeBundle(info: [
            "MALL_BASE_URL": "https://app.example.com"
        ])

        #expect(AppConfig.mallHomeURL(bundle: bundle)?.absoluteString == "https://app.example.com/mall")
    }
}
