import Foundation

/// Supported theater channels.
enum TheaterChannel: String, CaseIterable, Codable, Equatable, Sendable, Identifiable {
    case all
    case real
    case anime
    case movie
    case audio
    case novel
    case comic
    case bigscreen

    var id: String { rawValue }

    var title: String {
        switch self {
        case .all:
            return "找剧"
        case .real:
            return "真人"
        case .anime:
            return "动漫"
        case .movie:
            return "电影"
        case .audio:
            return "有声书"
        case .novel:
            return "小说"
        case .comic:
            return "漫画"
        case .bigscreen:
            return "大屏"
        }
    }
}
