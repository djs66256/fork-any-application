import Foundation

struct SignInStatusDTO: Decodable, Equatable {
    let serverDate: String
    let shouldShowPopup: Bool
    let todaySigned: Bool
    let currentStreak: Int
    let rewardCopy: String
    let days: [SignInDayDTO]

    func toEntity() -> SignInStatus {
        SignInStatus(
            serverDate: serverDate,
            shouldShowPopup: shouldShowPopup,
            todaySigned: todaySigned,
            currentStreak: currentStreak,
            rewardCopy: rewardCopy,
            days: days.map { $0.toEntity() }
        )
    }
}

struct SignInDayDTO: Decodable, Equatable {
    let day: Int
    let title: String
    let rewardLabel: String
    let status: SignInDay.Status

    func toEntity() -> SignInDay {
        SignInDay(day: day, title: title, rewardLabel: rewardLabel, status: status)
    }
}
