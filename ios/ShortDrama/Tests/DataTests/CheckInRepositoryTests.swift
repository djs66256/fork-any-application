import Foundation
@testable import ShortDrama
import Testing

struct CheckInRepositoryTests {
    private func makeSession(handler: @escaping URLProtocolMock.RequestHandler) -> URLSession {
        URLProtocolMock.handler = handler
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [URLProtocolMock.self]
        return URLSession(configuration: config)
    }

    @Test("T-01: check-in repository maps DTO to entity")
    func testRepositoryMapsStatusEntity() async throws {
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
            let requestURL = try #require(request.url)
            let response = try #require(HTTPURLResponse(url: requestURL, statusCode: 200, httpVersion: nil, headerFields: nil))
            return (response, Data(body.utf8))
        }

        let repository = CheckInRepository(
            dataSource: CheckInRemoteDataSource(client: APIClient(session: session, baseURL: "https://api.example.com"))
        )

        let status = try await repository.fetchStatus(installationId: "installation-001", accessToken: nil)

        #expect(status.serverDate == "2026-07-29")
        #expect(status.days[3].status == .today)
        #expect(status.rewardCopy == "今日签到可领取第 4 天奖励")
    }

    @Test("T-01: check-in repository forwards API errors")
    func testRepositoryForwardsErrors() async {
        let session = makeSession { request in
            let requestURL = try #require(request.url)
            let response = try #require(HTTPURLResponse(url: requestURL, statusCode: 503, httpVersion: nil, headerFields: nil))
            let body = "{\"error\": {\"code\": \"SERVICE_UNAVAILABLE\", \"message\": \"服务暂不可用，请稍后重试\"}}"
            return (response, Data(body.utf8))
        }

        let repository = CheckInRepository(
            dataSource: CheckInRemoteDataSource(client: APIClient(session: session, baseURL: "https://api.example.com"))
        )

        await #expect(throws: APIError.self) {
            _ = try await repository.fetchStatus(installationId: "installation-001", accessToken: nil)
        }
    }
}
