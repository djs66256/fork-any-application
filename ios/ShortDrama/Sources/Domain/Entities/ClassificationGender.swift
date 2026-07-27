import Foundation

/// Supported gender filters for the classification page.
enum ClassificationGender: String, CaseIterable, Codable, Equatable, Sendable {
    case all
    case male
    case female

    var title: String {
        switch self {
        case .all:
            return "全部"
        case .male:
            return "男频"
        case .female:
            return "女频"
        }
    }
}
