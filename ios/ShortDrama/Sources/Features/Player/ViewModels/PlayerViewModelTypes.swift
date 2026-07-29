import Foundation

extension PlayerViewModel {
    enum UiState: Equatable {
        case idle
        case bootstrapping
        case ready
        case playing
        case paused
        case switchingEpisode
        case noResource
        case error(String)
    }

    enum PlaybackSpeed: Double, CaseIterable, Equatable, Sendable {
        case half = 0.5
        case threeQuarter = 0.75
        case normal = 1.0
        case onePointTwentyFive = 1.25
        case onePointFive = 1.5
        case onePointSeventyFive = 1.75
        case double = 2.0

        var label: String {
            switch self {
            case .half:
                return "0.5x"
            case .threeQuarter:
                return "0.75x"
            case .normal:
                return "1.0x"
            case .onePointTwentyFive:
                return "1.25x"
            case .onePointFive:
                return "1.5x"
            case .onePointSeventyFive:
                return "1.75x"
            case .double:
                return "2.0x"
            }
        }
    }

    struct StopFingerprint: Equatable {
        let episodeId: String
        let progress: Double
        let duration: Double
    }
}
