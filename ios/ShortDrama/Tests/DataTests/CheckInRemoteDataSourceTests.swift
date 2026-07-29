import Foundation
@testable import ShortDrama
import Testing

struct CheckInRemoteDataSourceTests {
    private func makeSession(handler: @escaping URLProtocolMock.RequestHandler) -> URLSession {
        URLProtocolMock.handler = handler
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [URLProtocolMock.self]
        return URLSession(configuration: config)
    }

    @Test("T-01: check-in status request injects installation and authorization headers")
    func testFetchStatusHeaders() async throws {
        let body = """
        {
          "server_date": "2026-07-29",
          "should_show_popup": true,
          "today_signed": false,
          "current_streak": 3,
          "reward_copy": "今日签到可领取第 4 天奖励",
          "days": [
            {"day": 1, "title": "第 1 天", "reward_label": "金币 x10", "status": "signed"},
            {"day": 2, "title": "第 2 天", "reward_label": "金币 x20", "status": "signed"},
            {"day": 3, "title": "第 3 天", "reward_label": "金币 x30", "status": "signed"},
            {"day": 4, "title": "第 4 天", "reward_label": "金币 x40", "status": "today"},
            {"day": 5, "title": "第 5 天", "reward_label": "金币 x50", "status": "locked"},
            {"day": 6, "title": "第 6 天", "reward_label": "金币 x60", "status": "locked"},
            {"day": 7, "title": "第 7 天", "reward_label": "金币 x70", "status": "locked"}
          ]
        }
        """

        let session = makeSession { request in
            #expect(request.httpMethod == "GET")
            #expect(request.url?.path == "/api/check-ins/status")
            #expect(request.value(forHTTPHeaderField: "X-Installation-Id") == "installation-001")
            #expect(request.value(forHTTPHeaderField: "Authorization") == "Bearer access-token")
            let requestURL = try #require(request.url)
            let response = try #require(HTTPURLResponse(url: requestURL, statusCode: 200, httpVersion: nil, headerFields: nil))
            return (response, Data(body.utf8))
        }

        let dataSource = CheckInRemoteDataSource(client: APIClient(session: session, baseURL: "https://api.example.com"))
        let response = try await dataSource.fetchStatus(installationId: "installation-001", accessToken: "access-token")

        #expect(response.serverDate == "2026-07-29")
        #expect(response.todaySigned == false)
        #expect(response.days.count == 7)
    }

    @Test("T-01: submit check-in uses post and returns latest status")
    func testSubmitCheckIn() async throws {
        let body = """
        {
          "server_date": "2026-07-29",
          "should_show_popup": false,
          "today_signed": true,
          "current_streak": 4,
          "reward_copy": "今日签到已完成",
          "days": [
            {"day": 1, "title": "第 1 天", "reward_label": "金币 x10", "status": "signed"},
            {"day": 2, "title": "第 2 天", "reward_label": "金币 x20", "status": "signed"},
            {"day": 3, "title": "第 3 天", "reward_label": "金币 x30", "status": "signed"},
            {"day": 4, "title": "第 4 天", "reward_label": "金币 x40", "status": "signed"},
            {"day": 5, "title": "第 5 天", "reward_label": "金币 x50", "status": "locked"},
            {"day": 6, "title": "第 6 天", "reward_label": "金币 x60", "status": "locked"},
            {"day": 7, "title": "第 7 天", "reward_label": "金币 x70", "status": "locked"}
          ]
        }
        """

        let session = makeSession { request in
            #expect(request.httpMethod == "POST")
            #expect(request.url?.path == "/api/check-ins")
            #expect(request.value(forHTTPHeaderField: "X-Installation-Id") == "installation-001")
            let requestURL = try #require(request.url)
            let response = try #require(HTTPURLResponse(url: requestURL, statusCode: 200, httpVersion: nil, headerFields: nil))
            return (response, Data(body.utf8))
        }

        let dataSource = CheckInRemoteDataSource(client: APIClient(session: session, baseURL: "https://api.example.com"))
        let response = try await dataSource.submitCheckIn(installationId: "installation-001", accessToken: nil)

        #expect(response.todaySigned == true)
        #expect(response.shouldShowPopup == false)
        #expect(response.currentStreak == 4)
    }
}
