import Foundation
@testable import ShortDrama

extension SignInStatus {
    static func fixture(
        serverDate: String = "2026-07-29",
        shouldShowPopup: Bool = true,
        todaySigned: Bool = false,
        currentStreak: Int = 3,
        rewardCopy: String = "今日签到可领取第 4 天奖励"
    ) -> SignInStatus {
        SignInStatus(
            serverDate: serverDate,
            shouldShowPopup: shouldShowPopup,
            todaySigned: todaySigned,
            currentStreak: currentStreak,
            rewardCopy: rewardCopy,
            days: [
                SignInDay(day: 1, title: "第 1 天", rewardLabel: "金币 x10", status: .signed),
                SignInDay(day: 2, title: "第 2 天", rewardLabel: "金币 x20", status: .signed),
                SignInDay(day: 3, title: "第 3 天", rewardLabel: "金币 x30", status: .signed),
                SignInDay(day: 4, title: "第 4 天", rewardLabel: "金币 x40", status: todaySigned ? .signed : .today),
                SignInDay(day: 5, title: "第 5 天", rewardLabel: "金币 x50", status: .locked),
                SignInDay(day: 6, title: "第 6 天", rewardLabel: "金币 x60", status: .locked),
                SignInDay(day: 7, title: "第 7 天", rewardLabel: "金币 x70", status: .locked)
            ]
        )
    }

    static func signedFixture(serverDate: String = "2026-07-29") -> SignInStatus {
        SignInStatus.fixture(
            serverDate: serverDate,
            shouldShowPopup: false,
            todaySigned: true,
            currentStreak: 4,
            rewardCopy: "今日签到已完成"
        )
    }
}

extension MessagePreview {
    static func fixture(
        title: String = "系统通知",
        summary: String = "你关注的剧集已更新第 12 集。",
        relativeTime: String = "2小时前"
    ) -> MessagePreview {
        MessagePreview(title: title, summary: summary, relativeTime: relativeTime)
    }
}

extension SystemMessage {
    static func fixture(
        id: String = "550e8400-e29b-41d4-a716-446655440001",
        title: String = "系统通知",
        summary: String = "你关注的剧集已更新第 12 集。",
        sentAt: String = "2026-07-29T08:00:00.000Z"
    ) -> SystemMessage {
        SystemMessage(id: id, title: title, summary: summary, sentAt: sentAt)
    }
}

extension InteractionMessage {
    static func fixture(
        id: String = "660e8400-e29b-41d4-a716-446655440010",
        type: InteractionMessage.MessageType = .commentReply,
        title: String = "有人回复了你的评论",
        summary: String = "“这集反转真不错” 收到一条新回复。",
        sentAt: String = "2026-07-29T09:00:00.000Z"
    ) -> InteractionMessage {
        InteractionMessage(id: id, type: type, title: title, summary: summary, sentAt: sentAt)
    }
}

extension PagedResult where Item == SystemMessage {
    static func systemFixture(items: [SystemMessage] = [.fixture()]) -> PagedResult<SystemMessage> {
        PagedResult(items: items, page: 1, pageSize: 20, total: items.count, totalPages: 1)
    }
}

extension PagedResult where Item == InteractionMessage {
    static func interactionFixture(items: [InteractionMessage] = [.fixture()]) -> PagedResult<InteractionMessage> {
        PagedResult(items: items, page: 1, pageSize: 20, total: items.count, totalPages: 1)
    }
}
