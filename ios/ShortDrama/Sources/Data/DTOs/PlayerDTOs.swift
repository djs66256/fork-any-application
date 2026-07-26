import Foundation

struct EpisodeListResponseDTO: Decodable, Equatable {
    let code: Int
    let data: EpisodeListPayloadDTO
    let message: String
}

struct EpisodeListPayloadDTO: Decodable, Equatable {
    let dramaId: String
    let seriesStatus: DramaSeriesStatus
    let items: [PlayerEpisodeDTO]
}

struct PlayerEpisodeDTO: Codable, Equatable {
    let id: String
    let dramaId: String
    let title: String
    let episodeNumber: Int
    let duration: Int
    let videoUrl: String
    let thumbnailUrl: String
    let description: String?
    let createdAt: String
    let updatedAt: String

    func toEntity() -> Episode {
        Episode(
            id: id,
            dramaId: dramaId,
            title: title,
            episodeNumber: episodeNumber,
            videoUrl: videoUrl,
            duration: duration,
            thumbnailUrl: thumbnailUrl,
            description: description,
            createdAt: createdAt,
            updatedAt: updatedAt
        )
    }
}

struct PlayerProgressResponseDTO: Decodable, Equatable {
    let code: Int
    let data: PlayerProgressPayloadDTO
    let message: String
}

struct PlayerProgressPayloadDTO: Decodable, Equatable {
    let dramaId: String
    let hasHistory: Bool
    let episodeId: String?
    let startTime: Double
    let updatedAt: String?

    func toEntity() -> PlayerProgress {
        PlayerProgress(
            dramaId: dramaId,
            hasHistory: hasHistory,
            episodeId: episodeId,
            startTime: startTime,
            updatedAt: updatedAt
        )
    }
}

struct PlayerStartRequestDTO: Codable, Equatable {
    let dramaId: String
    let episodeId: String
    let progress: Double
}

struct PlayerStartResponseDTO: Decodable, Equatable {
    let code: Int
    let data: PlayerStartPayloadDTO
    let message: String
}

struct PlayerStartPayloadDTO: Decodable, Equatable {
    let dramaId: String
    let episodeId: String
    let acceptedProgress: Double
    let playbackSessionId: String
    let startedAt: String

    func toEntity() -> PlaybackStartReceipt {
        PlaybackStartReceipt(
            dramaId: dramaId,
            episodeId: episodeId,
            acceptedProgress: acceptedProgress,
            playbackSessionId: playbackSessionId,
            startedAt: startedAt
        )
    }
}

struct PlayerStopRequestDTO: Codable, Equatable {
    let dramaId: String
    let episodeId: String
    let progress: Double
    let duration: Double
}

struct PlayerStopResponseDTO: Decodable, Equatable {
    let code: Int
    let data: PlayerStopPayloadDTO
    let message: String
}

struct PlayerStopPayloadDTO: Decodable, Equatable {
    let dramaId: String
    let episodeId: String
    let savedProgress: Double
    let duration: Double
    let updatedAt: String

    func toEntity() -> PlaybackStopReceipt {
        PlaybackStopReceipt(
            dramaId: dramaId,
            episodeId: episodeId,
            savedProgress: savedProgress,
            duration: duration,
            updatedAt: updatedAt
        )
    }
}

extension EpisodeListPayloadDTO {
    func toEntity() -> EpisodeList {
        EpisodeList(
            dramaId: dramaId,
            seriesStatus: seriesStatus,
            items: items.map { $0.toEntity() }
        )
    }
}
