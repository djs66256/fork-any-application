import Foundation

final class PlayerRemoteDataSource: @unchecked Sendable {
    private let client: APIClient

    init(client: APIClient = .shared) {
        self.client = client
    }

    func fetchProgress(dramaId: String, playbackSessionId: String) async throws -> PlayerProgressResponseDTO {
        let endpoint = PlayerGetProgressEndpoint(
            dramaId: dramaId,
            playbackSessionId: playbackSessionId
        )
        return try await client.request(endpoint)
    }

    func fetchEpisodes(dramaId: String) async throws -> EpisodeListResponseDTO {
        let endpoint = PlayerGetDramaEpisodesEndpoint(dramaId: dramaId)
        return try await client.request(endpoint)
    }

    func fetchRecentlyViewed(playbackSessionId: String) async throws -> RecentlyViewedResponseDTO {
        let endpoint = PlayerGetRecentlyViewedEndpoint(playbackSessionId: playbackSessionId)
        return try await client.request(endpoint)
    }

    func startPlayback(
        request: StartPlaybackRequest,
        playbackSessionId: String
    ) async throws -> PlayerStartResponseDTO {
        let endpoint = PlayerStartPlaybackEndpoint(
            request: PlayerStartRequestDTO(
                dramaId: request.dramaId,
                episodeId: request.episodeId,
                progress: request.progress
            ),
            playbackSessionId: playbackSessionId
        )
        return try await client.request(endpoint)
    }

    func stopPlayback(
        request: StopPlaybackRequest,
        playbackSessionId: String
    ) async throws -> PlayerStopResponseDTO {
        let endpoint = PlayerStopPlaybackEndpoint(
            request: PlayerStopRequestDTO(
                dramaId: request.dramaId,
                episodeId: request.episodeId,
                progress: request.progress,
                duration: request.duration
            ),
            playbackSessionId: playbackSessionId
        )
        return try await client.request(endpoint)
    }
}

private struct PlayerGetProgressEndpoint: APIEndpoint {
    typealias Response = PlayerProgressResponseDTO

    let dramaId: String
    let playbackSessionId: String

    var path: String { "/api/player/progress" }
    var method: HTTPMethod { .get }
    var queryItems: [URLQueryItem]? {
        [URLQueryItem(name: "dramaId", value: dramaId)]
    }
    var headers: [String: String] {
        ["X-Playback-Session-Id": playbackSessionId]
    }
}

private struct PlayerGetDramaEpisodesEndpoint: APIEndpoint {
    typealias Response = EpisodeListResponseDTO

    let dramaId: String

    var path: String { "/api/dramas/\(dramaId)/episodes" }
    var method: HTTPMethod { .get }
}

private struct PlayerGetRecentlyViewedEndpoint: APIEndpoint {
    typealias Response = RecentlyViewedResponseDTO

    let playbackSessionId: String

    var path: String { "/api/player/recently-viewed" }
    var method: HTTPMethod { .get }
    var headers: [String: String] {
        ["X-Playback-Session-Id": playbackSessionId]
    }
}

private struct PlayerStartPlaybackEndpoint: APIEndpoint {
    typealias Response = PlayerStartResponseDTO

    let request: PlayerStartRequestDTO
    let playbackSessionId: String

    var path: String { "/api/player/start" }
    var method: HTTPMethod { .post }
    var headers: [String: String] {
        ["X-Playback-Session-Id": playbackSessionId]
    }
    var body: Encodable? { request }
}

private struct PlayerStopPlaybackEndpoint: APIEndpoint {
    typealias Response = PlayerStopResponseDTO

    let request: PlayerStopRequestDTO
    let playbackSessionId: String

    var path: String { "/api/player/stop" }
    var method: HTTPMethod { .post }
    var headers: [String: String] {
        ["X-Playback-Session-Id": playbackSessionId]
    }
    var body: Encodable? { request }
}
