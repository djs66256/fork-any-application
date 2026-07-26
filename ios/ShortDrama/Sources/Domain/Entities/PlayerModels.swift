import Foundation

/// Series status metadata used by the player page.
enum DramaSeriesStatus: String, Codable, Equatable, Sendable {
    case completed
    case ongoing

    var displayText: String {
        switch self {
        case .completed:
            return "已完结"
        case .ongoing:
            return "更新中"
        }
    }
}

/// Episode list payload used by the player bootstrap flow.
struct EpisodeList: Equatable, Sendable {
    let dramaId: String
    let seriesStatus: DramaSeriesStatus
    let items: [Episode]
}

/// Resume progress metadata returned by the player progress API.
struct PlayerProgress: Equatable, Sendable {
    let dramaId: String
    let hasHistory: Bool
    let episodeId: String?
    let startTime: Double
    let updatedAt: String?
}

/// Request model for start playback.
struct StartPlaybackRequest: Equatable, Sendable {
    let dramaId: String
    let episodeId: String
    let progress: Double
}

/// Request model for stop playback.
struct StopPlaybackRequest: Equatable, Sendable {
    let dramaId: String
    let episodeId: String
    let progress: Double
    let duration: Double
}

/// Response model for a successful start playback call.
struct PlaybackStartReceipt: Equatable, Sendable {
    let dramaId: String
    let episodeId: String
    let acceptedProgress: Double
    let playbackSessionId: String
    let startedAt: String
}

/// Response model for a successful stop playback call.
struct PlaybackStopReceipt: Equatable, Sendable {
    let dramaId: String
    let episodeId: String
    let savedProgress: Double
    let duration: Double
    let updatedAt: String
}
