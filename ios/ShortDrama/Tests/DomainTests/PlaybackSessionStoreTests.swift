import Foundation
import Testing
@testable import ShortDrama

struct PlaybackSessionStoreTests {
    private final class InMemoryKeychainClient: KeychainClient, @unchecked Sendable {
        var storedValue: String?
        private(set) var readCallCount = 0
        private(set) var writeCallCount = 0
        private(set) var deleteCallCount = 0

        func read(service: String, account: String, accessGroup: String?) throws -> String? {
            readCallCount += 1
            return storedValue
        }

        func write(value: String, service: String, account: String, accessGroup: String?) throws {
            writeCallCount += 1
            storedValue = value
        }

        func delete(service: String, account: String, accessGroup: String?) throws {
            deleteCallCount += 1
            storedValue = nil
        }
    }

    @Test("T-05: playback session store creates and reuses session id")
    func testCreateAndReuseSessionId() throws {
        let keychain = InMemoryKeychainClient()
        let store = KeychainPlaybackSessionStore(keychainClient: keychain)

        let first = try store.getOrCreateSessionId()
        let second = try store.getOrCreateSessionId()

        #expect(!first.isEmpty)
        #expect(first == second)
        #expect(keychain.writeCallCount == 1)
        #expect(keychain.readCallCount == 2)
    }

    @Test("T-05: playback session store returns existing session id without rewriting")
    func testReturnsExistingSessionId() throws {
        let keychain = InMemoryKeychainClient()
        keychain.storedValue = "existing-session-id"
        let store = KeychainPlaybackSessionStore(keychainClient: keychain)

        let sessionId = try store.getOrCreateSessionId()

        #expect(sessionId == "existing-session-id")
        #expect(keychain.writeCallCount == 0)
    }
}
