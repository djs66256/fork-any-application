import SwiftUI

enum AppTab: String, CaseIterable, Hashable, Identifiable, Sendable {
    case home
    case theater
    case mall
    case earn
    case profile

    var id: String { rawValue }

    var title: String {
        switch self {
        case .home:
            return "首页"
        case .theater:
            return "剧场"
        case .mall:
            return "商城"
        case .earn:
            return "赚钱"
        case .profile:
            return "我的"
        }
    }

    var systemImage: String {
        switch self {
        case .home:
            return "house"
        case .theater:
            return "tv"
        case .mall:
            return "bag"
        case .earn:
            return "creditcard"
        case .profile:
            return "person"
        }
    }
}
