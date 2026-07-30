import SwiftUI

struct PlayerTopBar: View {
    let title: String
    let speedLabel: String
    let onBack: () -> Void
    let onSpeedTap: () -> Void
    let onMoreTap: () -> Void

    private var displaySpeedLabel: String {
        speedLabel == PlayerViewModel.PlaybackSpeed.normal.label ? "倍速" : speedLabel
    }

    var body: some View {
        HStack(spacing: DesignTokens.Spacing.lg) {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 28, height: 28)
            }
            .buttonStyle(.plain)

            Text(title)
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(.white)
                .lineLimit(1)

            Spacer()

            Button(action: onSpeedTap) {
                HStack(spacing: 6) {
                    Image(systemName: "speedometer")
                        .font(.system(size: 18, weight: .medium))
                    Text(displaySpeedLabel)
                        .font(.system(size: 18, weight: .semibold))
                }
                .foregroundStyle(.white)
            }
            .buttonStyle(.plain)

            Button(action: onMoreTap) {
                Image(systemName: "ellipsis.vertical")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 28, height: 28)
            }
            .buttonStyle(.plain)
        }
    }
}
