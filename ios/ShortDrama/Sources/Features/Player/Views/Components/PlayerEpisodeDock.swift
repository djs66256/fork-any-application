import SwiftUI

struct PlayerEpisodeDock: View {
    let seriesStatus: String
    let totalCount: Int
    let currentEpisodeNumber: Int?
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack {
                VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
                    Text("选集")
                        .font(.headline)
                        .foregroundStyle(.white)
                    Text("\(seriesStatus) · 全 \(totalCount) 集")
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.75))
                }

                Spacer()

                if let currentEpisodeNumber {
                    Text("第 \(currentEpisodeNumber) 集")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.white)
                }

                Image(systemName: "chevron.up")
                    .foregroundStyle(.white)
            }
            .padding(DesignTokens.Spacing.lg)
            .background(Color.black.opacity(0.5))
            .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))
        }
        .buttonStyle(.plain)
    }
}
