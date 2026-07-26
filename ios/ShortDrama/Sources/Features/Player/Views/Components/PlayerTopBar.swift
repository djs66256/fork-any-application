import SwiftUI

struct PlayerTopBar: View {
    let title: String
    let speedLabel: String
    let onBack: () -> Void
    let onSpeedTap: () -> Void
    let onMoreTap: () -> Void

    var body: some View {
        HStack(spacing: DesignTokens.Spacing.md) {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.headline.weight(.semibold))
                    .foregroundStyle(.white)
                    .padding(10)
                    .background(Color.black.opacity(0.4))
                    .clipShape(Circle())
            }

            Text(title)
                .font(.headline)
                .foregroundStyle(.white)
                .lineLimit(1)

            Spacer()

            HStack(spacing: DesignTokens.Spacing.sm) {
                Button(speedLabel, action: onSpeedTap)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, DesignTokens.Spacing.md)
                    .padding(.vertical, DesignTokens.Spacing.sm)
                    .background(Color.black.opacity(0.4))
                    .clipShape(Capsule())

                Button(action: onMoreTap) {
                    Image(systemName: "ellipsis")
                        .font(.headline.weight(.semibold))
                        .foregroundStyle(.white)
                        .padding(10)
                        .background(Color.black.opacity(0.4))
                        .clipShape(Circle())
                }
            }
        }
    }
}
