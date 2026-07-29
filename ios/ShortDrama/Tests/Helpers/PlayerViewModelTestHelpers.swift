import Foundation
@testable import ShortDrama

func makeProgress(
    dramaId: String = "drama-001",
    hasHistory: Bool = false,
    episodeId: String? = nil,
    startTime: Double = 0,
    updatedAt: String? = nil
) -> PlayerProgress {
    PlayerProgress(
        dramaId: dramaId,
        hasHistory: hasHistory,
        episodeId: episodeId,
        startTime: startTime,
        updatedAt: updatedAt
    )
}

func makeEpisodeList(
    dramaId: String = "drama-001",
    seriesStatus: DramaSeriesStatus = .completed,
    items: [Episode]
) -> EpisodeList {
    EpisodeList(
        dramaId: dramaId,
        seriesStatus: seriesStatus,
        items: items
    )
}

func makeSessionStore(sessionId: String = "session-001") -> MockPlaybackSessionStore {
    let sessionStore = MockPlaybackSessionStore()
    sessionStore.sessionId = sessionId
    return sessionStore
}
