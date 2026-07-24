import Foundation

/// ViewModel for the player screen.
@MainActor
final class PlayerViewModel: ObservableObject {

    let videoId: String

    init(videoId: String) {
        self.videoId = videoId
    }
}
