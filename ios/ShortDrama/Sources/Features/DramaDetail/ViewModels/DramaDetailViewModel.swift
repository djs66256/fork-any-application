import Foundation

/// ViewModel for the drama detail screen.
@MainActor
final class DramaDetailViewModel: ObservableObject {

    let dramaId: String

    init(dramaId: String) {
        self.dramaId = dramaId
    }
}
