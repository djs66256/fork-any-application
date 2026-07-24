import SwiftUI

/// Placeholder player screen.
struct PlayerView: View {

    @ObservedObject var viewModel: PlayerViewModel

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            Text("Player")
                .font(.title)
            Text("Video ID: \(viewModel.videoId)")
                .font(.body)
                .foregroundColor(.secondary)
        }
        .padding()
    }
}

#Preview {
    PlayerView(viewModel: PlayerViewModel(videoId: "preview-123"))
}
