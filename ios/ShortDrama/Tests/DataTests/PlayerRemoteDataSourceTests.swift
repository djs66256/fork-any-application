import Foundation
import Testing
@testable import ShortDrama

struct PlayerRemoteDataSourceTests {
    private func makeSession(handler: @escaping URLProtocolMock.RequestHandler) -> URLSession {
        URLProtocolMock.handler = handler
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [URLProtocolMock.self]
        return URLSession(configuration: config)
    }

    @Test("T-03: progress request decodes payload and injects playback session header")
    func testFetchProgress() async throws {
        let responseBody = """
        {
          "code": 0,
          "data": {
            "drama_id": "drama-001",
            "has_history": true,
            "episode_id": "episode-003",
            "start_time": 330,
            "updated_at": "2026-07-26T00:00:00Z"
          },
          "message": "ok"
        }
        """

        let session = makeSession { request in
            #expect(request.httpMethod == "GET")
            #expect(request.url?.path == "/api/player/progress")
            #expect(request.url?.query == "dramaId=drama-001")
            #expect(request.value(forHTTPHeaderField: "X-Playback-Session-Id") == "session-001")
            let response = HTTPURLResponse(
                url: request.url!,
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            )!
            return (response, Data(responseBody.utf8))
        }

        let dataSource = PlayerRemoteDataSource(client: APIClient(session: session))
        let response = try await dataSource.fetchProgress(dramaId: "drama-001", playbackSessionId: "session-001")

        #expect(response.data.dramaId == "drama-001")
        #expect(response.data.hasHistory == true)
        #expect(response.data.episodeId == "episode-003")
        #expect(response.data.startTime == 330)
    }

    @Test("T-03: episodes request decodes list without playback session header")
    func testFetchEpisodes() async throws {
        let responseBody = """
        {
          "code": 0,
          "data": {
            "drama_id": "drama-001",
            "series_status": "completed",
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
              }
            ]
          },
          "message": "ok"
        }
        """

        let session = makeSession { request in
            #expect(request.httpMethod == "GET")
            #expect(request.url?.path == "/api/dramas/drama-001/episodes")
            #expect(request.value(forHTTPHeaderField: "X-Playback-Session-Id") == nil)
            let response = HTTPURLResponse(
                url: request.url!,
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            )!
            return (response, Data(responseBody.utf8))
        }

        let dataSource = PlayerRemoteDataSource(client: APIClient(session: session))
        let response = try await dataSource.fetchEpisodes(dramaId: "drama-001")

        #expect(response.data.dramaId == "drama-001")
        #expect(response.data.seriesStatus == .completed)
        #expect(response.data.items.count == 1)
        #expect(response.data.items[0].description == "第一集简介")
    }

    @Test("T-03: start request sends body and header")
    func testStartPlayback() async throws {
        let responseBody = """
        {
          "code": 0,
          "data": {
            "drama_id": "drama-001",
            "episode_id": "episode-001",
            "accepted_progress": 120,
            "playback_session_id": "session-001",
            "started_at": "2026-07-26T00:00:00Z"
          },
          "message": "ok"
        }
        """

        let session = makeSession { request in
            #expect(request.httpMethod == "POST")
            #expect(request.url?.path == "/api/player/start")
            #expect(request.value(forHTTPHeaderField: "X-Playback-Session-Id") == "session-001")
            let body = try #require(request.httpBody)
            let decoder = JSONDecoder()
            decoder.keyDecodingStrategy = .convertFromSnakeCase
            let decoded = try decoder.decode(PlayerStartRequestDTO.self, from: body)
            #expect(decoded.dramaId == "drama-001")
            #expect(decoded.episodeId == "episode-001")
            #expect(decoded.progress == 120)
            let response = HTTPURLResponse(
                url: request.url!,
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            )!
            return (response, Data(responseBody.utf8))
        }

        let dataSource = PlayerRemoteDataSource(client: APIClient(session: session))
        let response = try await dataSource.startPlayback(
            request: StartPlaybackRequest(dramaId: "drama-001", episodeId: "episode-001", progress: 120),
            playbackSessionId: "session-001"
        )

        #expect(response.data.acceptedProgress == 120)
        #expect(response.data.playbackSessionId == "session-001")
    }

    @Test("T-03: stop request sends body and header")
    func testStopPlayback() async throws {
        let responseBody = """
        {
          "code": 0,
          "data": {
            "drama_id": "drama-001",
            "episode_id": "episode-001",
            "saved_progress": 150,
            "duration": 180,
            "updated_at": "2026-07-26T00:00:00Z"
          },
          "message": "ok"
        }
        """

        let session = makeSession { request in
            #expect(request.httpMethod == "POST")
            #expect(request.url?.path == "/api/player/stop")
            #expect(request.value(forHTTPHeaderField: "X-Playback-Session-Id") == "session-001")
            let body = try #require(request.httpBody)
            let decoder = JSONDecoder()
            decoder.keyDecodingStrategy = .convertFromSnakeCase
            let decoded = try decoder.decode(PlayerStopRequestDTO.self, from: body)
            #expect(decoded.dramaId == "drama-001")
            #expect(decoded.episodeId == "episode-001")
            #expect(decoded.progress == 150)
            #expect(decoded.duration == 180)
            let response = HTTPURLResponse(
                url: request.url!,
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            )!
            return (response, Data(responseBody.utf8))
        }

        let dataSource = PlayerRemoteDataSource(client: APIClient(session: session))
        let response = try await dataSource.stopPlayback(
            request: StopPlaybackRequest(dramaId: "drama-001", episodeId: "episode-001", progress: 150, duration: 180),
            playbackSessionId: "session-001"
        )

        #expect(response.data.savedProgress == 150)
        #expect(response.data.duration == 180)
    }
}
