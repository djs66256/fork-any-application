import Foundation

/// Supported ranking content types.
enum RankingContentType: String, CaseIterable, Codable, Equatable, Sendable {
    case all
    case liveAction = "live_action"
    case ai

    var title: String {
        switch self {
        case .all:
            return "全部"
        case .liveAction:
            return "真人"
        case .ai:
            return "AI"
        }
    }

    var requestValue: String { rawValue }
}
