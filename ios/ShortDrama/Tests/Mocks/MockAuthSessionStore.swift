import Foundation
@testable import ShortDrama

final class MockAuthSessionStore: AuthSessionStore, @unchecked Sendable {
    var loadedSession: AuthSession?
    var loadError: Error?
    var saveError: Error?
    var clearError: Error?

    private(set) var loadCallCount = 0
    private(set) var saveCallCount = 0
    private(set) var clearCallCount = 0
    private(set) var lastSavedSession: AuthSession?

    func load() throws -> AuthSession? {
        loadCallCount += 1
        if let loadError {
            throw loadError
        }
        return loadedSession
    }

    func save(_ session: AuthSession) throws {
        saveCallCount += 1
        if let saveError {
            throw saveError
        }
        lastSavedSession = session
        loadedSession = session
    }

    func clear() throws {
        clearCallCount += 1
        if let clearError {
            throw clearError
        }
        loadedSession = nil
    }
}
