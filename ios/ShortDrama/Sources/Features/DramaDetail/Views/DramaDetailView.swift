import SwiftUI

/// Placeholder drama detail screen.
struct DramaDetailView: View {

    @ObservedObject var viewModel: DramaDetailViewModel

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            Text("Drama Detail")
                .font(.title)
            Text("Drama ID: \(viewModel.dramaId)")
                .font(.body)
                .foregroundColor(.secondary)
        }
        .padding()
    }
}

#Preview {
    DramaDetailView(viewModel: DramaDetailViewModel(dramaId: "preview-456"))
}
