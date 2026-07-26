import Foundation
@testable import ShortDrama

final class MockPlaybackSessionStore: PlaybackSessionStore, @unchecked Sendable {
    var sessionId = "playback-session-001"
    var stubbedError: Error?
    private(set) var getOrCreateSessionIdCallCount = 0

    func getOrCreateSessionId() throws -> String {
        getOrCreateSessionIdCallCount += 1
        if let stubbedError {
            throw stubbedError
        }
        return sessionId
    }
}
