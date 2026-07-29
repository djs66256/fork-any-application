import Foundation
import Testing
@testable import ShortDrama

struct InstallationIdStoreTests {
    private final class InMemoryKeychainClient: KeychainClient, @unchecked Sendable {
        var storedValue: String?
        private(set) var readCallCount = 0
        private(set) var writeCallCount = 0

        func read(service: String, account: String, accessGroup: String?) throws -> String? {
            readCallCount += 1
            return storedValue
        }

        func write(value: String, service: String, account: String, accessGroup: String?) throws {
            writeCallCount += 1
            storedValue = value
        }

        func delete(service: String, account: String, accessGroup: String?) throws {}
    }

    @Test("T-02: installation id store creates and reuses UUID")
    func testCreateAndReuseInstallationId() throws {
        let keychain = InMemoryKeychainClient()
        let store = KeychainInstallationIdStore(keychainClient: keychain)

        let first = try store.getOrCreateInstallationId()
        let second = try store.getOrCreateInstallationId()

        #expect(!first.isEmpty)
        #expect(first == second)
        #expect(keychain.writeCallCount == 1)
        #expect(keychain.readCallCount == 2)
    }

    @Test("T-02: popup dismiss store persists by server date")
    func testDismissStorePersistsByServerDate() {
        let defaults = UserDefaults(suiteName: "InstallationIdStoreTests")!
        defaults.removePersistentDomain(forName: "InstallationIdStoreTests")
        let store = UserDefaultsCheckInPopupDismissStore(defaults: defaults, keyPrefix: "checkin.popup.dismissed.test")

        #expect(store.isDismissed(serverDate: "2026-07-29") == false)
        store.markDismissed(serverDate: "2026-07-29")
        #expect(store.isDismissed(serverDate: "2026-07-29") == true)
        #expect(store.isDismissed(serverDate: "2026-07-30") == false)
    }
}
