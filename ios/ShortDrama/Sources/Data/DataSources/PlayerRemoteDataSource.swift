import Foundation

final class PlayerRemoteDataSource: @unchecked Sendable {
    private let client: APIClient

    init(client: APIClient = .shared) {
        self.client = client
    }

    func fetchProgress(dramaId: String, playbackSessionId: String) async throws -> PlayerProgressResponseDTO {
        let endpoint = PlayerEndpoints.GetProgress(
            dramaId: dramaId,
            playbackSessionId: playbackSessionId
        )
        return try await client.request(endpoint)
    }

    func fetchEpisodes(dramaId: String) async throws -> EpisodeListResponseDTO {
        let endpoint = PlayerEndpoints.GetDramaEpisodes(dramaId: dramaId)
        return try await client.request(endpoint)
    }

    func startPlayback(
        request: StartPlaybackRequest,
        playbackSessionId: String
    ) async throws -> PlayerStartResponseDTO {
        let endpoint = PlayerEndpoints.StartPlayback(
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
        let endpoint = PlayerEndpoints.StopPlayback(
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

enum PlayerEndpoints {
    struct GetProgress: APIEndpoint {
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

    struct GetDramaEpisodes: APIEndpoint {
        typealias Response = EpisodeListResponseDTO

        let dramaId: String

        var path: String { "/api/dramas/\(dramaId)/episodes" }
        var method: HTTPMethod { .get }
    }

    struct StartPlayback: APIEndpoint {
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

    struct StopPlayback: APIEndpoint {
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
}
