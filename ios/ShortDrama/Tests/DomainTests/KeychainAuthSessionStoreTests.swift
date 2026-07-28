import Foundation
@testable import ShortDrama
import Testing

struct KeychainAuthSessionStoreTests {
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

    private func makeSession() -> AuthSession {
        AuthSession(
            accessToken: "access-token",
            refreshToken: "refresh-token",
            expiresAt: "2026-07-28T12:34:56Z",
            user: AuthUser(
                id: "550e8400-e29b-41d4-a716-446655440001",
                phone: "138****8000",
                displayName: "测试用户",
                avatarURL: "https://example.com/avatar.png",
                role: "viewer",
                isNewUser: false
            )
        )
    }

    @Test("auth session store saves and loads session payload")
    func testSaveAndLoadSession() throws {
        let keychain = InMemoryKeychainClient()
        let store = KeychainAuthSessionStore(keychainClient: keychain)
        let session = makeSession()

        try store.save(session)
        let loaded = try store.load()

        #expect(loaded == session)
        #expect(keychain.writeCallCount == 1)
        #expect(keychain.readCallCount == 1)
    }

    @Test("auth session store returns nil when payload missing")
    func testLoadReturnsNilWhenMissing() throws {
        let keychain = InMemoryKeychainClient()
        let store = KeychainAuthSessionStore(keychainClient: keychain)

        let loaded = try store.load()

        #expect(loaded == nil)
        #expect(keychain.deleteCallCount == 0)
    }

    @Test("auth session store clears corrupted payload and returns nil")
    func testLoadClearsCorruptedPayload() throws {
        let keychain = InMemoryKeychainClient()
        keychain.storedValue = "not-json"
        let store = KeychainAuthSessionStore(keychainClient: keychain)

        let loaded = try store.load()

        #expect(loaded == nil)
        #expect(keychain.deleteCallCount == 1)
        #expect(keychain.storedValue == nil)
    }

    @Test("auth session store clears payload explicitly")
    func testClearDeletesStoredPayload() throws {
        let keychain = InMemoryKeychainClient()
        let store = KeychainAuthSessionStore(keychainClient: keychain)

        try store.save(makeSession())
        try store.clear()

        #expect(keychain.deleteCallCount == 1)
        #expect(keychain.storedValue == nil)
    }
}
