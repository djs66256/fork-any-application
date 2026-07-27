import Foundation

enum MenuPlaceholderKind: String, CaseIterable, Hashable, Sendable {
    case login
    case messages
    case booking
    case downloads

    var title: String {
        switch self {
        case .login:
            return "立即登录"
        case .messages:
            return "消息中心"
        case .booking:
            return "我的预约"
        case .downloads:
            return "我的下载"
        }
    }

    var description: String {
        switch self {
        case .login:
            return "登录能力将在后续 PRD 中接入，当前先为你保留入口。"
        case .messages:
            return "消息中心仍在建设中，后续会接入真实通知与消息能力。"
        case .booking:
            return "预约资产列表将在后续 PRD 中上线，当前先提供统一承接页。"
        case .downloads:
            return "下载管理能力仍在建设中，后续会在这里接入真实内容。"
        }
    }

    var iconName: String {
        switch self {
        case .login:
            return "person.crop.circle.badge.plus"
        case .messages:
            return "bell.badge"
        case .booking:
            return "calendar.badge.clock"
        case .downloads:
            return "arrow.down.circle"
        }
    }
}
