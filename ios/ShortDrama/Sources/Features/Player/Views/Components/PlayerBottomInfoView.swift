import SwiftUI

struct PlayerBottomInfoView: View {
    let title: String
    let metadata: String
    let description: String

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            Text(title)
                .font(.headline)
                .foregroundStyle(.white)
                .lineLimit(2)

            if !metadata.isEmpty {
                Text(metadata)
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.8))
                    .lineLimit(1)
            }

            if !description.isEmpty {
                Text(description)
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.8))
                    .lineLimit(2)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
