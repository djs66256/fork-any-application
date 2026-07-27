import Foundation

/// Supported ranking dimensions.
enum RankingType: String, CaseIterable, Codable, Equatable, Sendable {
    case hot
    case recommend
    case booking

    var title: String {
        switch self {
        case .hot:
            return "热榜"
        case .recommend:
            return "推荐榜"
        case .booking:
            return "预约榜"
        }
    }
}
