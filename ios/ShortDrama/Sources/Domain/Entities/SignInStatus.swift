import Foundation

struct SignInDay: Equatable, Sendable {
    enum Status: String, Codable, Equatable, Sendable {
        case signed
        case today
        case locked
    }

    let day: Int
    let title: String
    let rewardLabel: String
    let status: Status
}

struct SignInStatus: Equatable, Sendable {
    let serverDate: String
    let shouldShowPopup: Bool
    let todaySigned: Bool
    let currentStreak: Int
    let rewardCopy: String
    let days: [SignInDay]
}
