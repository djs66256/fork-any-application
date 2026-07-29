import Foundation
import Testing
@testable import ShortDrama

struct PlayerRepositoryTests {
    private func makeSession(handler: @escaping URLProtocolMock.RequestHandler) -> URLSession {
        URLProtocolMock.handler = handler
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [URLProtocolMock.self]
        return URLSession(configuration: config)
    }

    @Test("T-04: repository maps bootstrap chain models")
    func testRepositoryMapsProgressAndEpisodes() async throws {
        let progressBody = """
        {
          "code": 0,
          "data": {
            "drama_id": "drama-001",
            "has_history": true,
            "episode_id": "episode-002",
            "start_time": 45,
            "updated_at": "2026-07-26T00:00:00Z"
          },
          "message": "ok"
        }
        """
        let episodesBody = """
        {
          "code": 0,
          "data": {
            "drama_id": "drama-001",
            "series_status": "ongoing",
            "items": [
              {
                "id": "episode-001",
                "drama_id": "drama-001",
                "title": "第 1 集",
                "episode_number": 1,
                "duration": 180,
                "video_url": "https://example.com/1.mp4",
                "thumbnail_url": "https://example.com/1.jpg",
                "description": "第一集简介",
                "created_at": "2026-07-26T00:00:00Z",
                "updated_at": "2026-07-26T00:00:00Z"
              },
              {
                "id": "episode-002",
                "drama_id": "drama-001",
                "title": "第 2 集",
                "episode_number": 2,
                "duration": 180,
                "video_url": "",
                "thumbnail_url": "https://example.com/2.jpg",
                "description": "第二集简介",
                "created_at": "2026-07-26T00:00:00Z",
                "updated_at": "2026-07-26T00:00:00Z"
              }
            ]
          },
          "message": "ok"
        }
        """

        let session = makeSession { request in
            let body: String
            switch request.url?.path {
            case "/api/player/progress":
                #expect(request.value(forHTTPHeaderField: "X-Playback-Session-Id") == "session-001")
                body = progressBody
            case "/api/dramas/drama-001/episodes":
                #expect(request.value(forHTTPHeaderField: "X-Playback-Session-Id") == nil)
                body = episodesBody
            default:
                Issue.record("Unexpected path \(request.url?.path ?? "nil")")
                body = "{}"
            }
            let response = HTTPURLResponse(
                url: request.url!,
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            )!
            return (response, Data(body.utf8))
        }

        let repository = PlayerRepository(
            dataSource: PlayerRemoteDataSource(client: APIClient(session: session, baseURL: "https://api.example.com"))
        )

        let progress = try await repository.fetchProgress(dramaId: "drama-001", playbackSessionId: "session-001")
        let episodes = try await repository.fetchEpisodes(dramaId: "drama-001")

        #expect(progress.hasHistory == true)
        #expect(progress.episodeId == "episode-002")
        #expect(progress.startTime == 45)
        #expect(episodes.seriesStatus == .ongoing)
        #expect(episodes.items.count == 2)
        #expect(episodes.items[0].description == "第一集简介")
        #expect(episodes.items[0].isPlayable == true)
        #expect(episodes.items[1].isPlayable == false)
    }

    @Test("T-04: repository maps start and stop responses")
    func testRepositoryMapsStartAndStopResponses() async throws {
        let startBody = """
        {
          "code": 0,
          "data": {
            "drama_id": "drama-001",
            "episode_id": "episode-001",
            "accepted_progress": 90,
            "playback_session_id": "session-001",
            "started_at": "2026-07-26T00:00:00Z"
          },
          "message": "ok"
        }
        """
        let stopBody = """
        {
          "code": 0,
          "data": {
            "drama_id": "drama-001",
            "episode_id": "episode-001",
            "saved_progress": 120,
            "duration": 180,
            "updated_at": "2026-07-26T00:00:00Z"
          },
          "message": "ok"
        }
        """

        let session = makeSession { request in
            let body: String
            switch request.url?.path {
            case "/api/player/start":
                let requestBody = try #require(request.httpBody)
                let decoder = JSONDecoder()
                decoder.keyDecodingStrategy = .convertFromSnakeCase
                let decoded = try decoder.decode(PlayerStartRequestDTO.self, from: requestBody)
                #expect(decoded.progress == 90)
                body = startBody
            case "/api/player/stop":
                let requestBody = try #require(request.httpBody)
                let decoder = JSONDecoder()
                decoder.keyDecodingStrategy = .convertFromSnakeCase
                let decoded = try decoder.decode(PlayerStopRequestDTO.self, from: requestBody)
                #expect(decoded.progress == 120)
                #expect(decoded.duration == 180)
                body = stopBody
            default:
                Issue.record("Unexpected path \(request.url?.path ?? "nil")")
                body = "{}"
            }
            let response = HTTPURLResponse(
                url: request.url!,
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            )!
            return (response, Data(body.utf8))
        }

        let repository = PlayerRepository(
            dataSource: PlayerRemoteDataSource(client: APIClient(session: session, baseURL: "https://api.example.com"))
        )

        let startReceipt = try await repository.startPlayback(
            request: StartPlaybackRequest(dramaId: "drama-001", episodeId: "episode-001", progress: 90),
            playbackSessionId: "session-001"
        )
        let stopReceipt = try await repository.stopPlayback(
            request: StopPlaybackRequest(dramaId: "drama-001", episodeId: "episode-001", progress: 120, duration: 180),
            playbackSessionId: "session-001"
        )

        #expect(startReceipt.acceptedProgress == 90)
        #expect(startReceipt.playbackSessionId == "session-001")
        #expect(stopReceipt.savedProgress == 120)
        #expect(stopReceipt.duration == 180)
    }
}
