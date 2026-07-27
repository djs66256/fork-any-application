import Foundation

/// Stable keys for the fixed classification dimensions.
enum ClassificationDimensionKey: String, CaseIterable, Codable, Equatable, Sendable, Identifiable {
    case eraBackground = "era_background"
    case themePlot = "theme_plot"
    case characterSetting = "character_setting"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .eraBackground:
            return "时代背景"
        case .themePlot:
            return "主题情节"
        case .characterSetting:
            return "角色设定"
        }
    }
}

/// Single classification dimension section.
struct ClassificationDimension: Equatable, Sendable, Identifiable {
    let key: ClassificationDimensionKey
    let name: String
    let tags: [String]

    var id: ClassificationDimensionKey { key }
}
