import SwiftUI

struct PlayerEpisodeDock: View {
    let title: String
    let seriesStatus: String
    let totalCount: Int
    let onTap: () -> Void

    var body: some View {
        HStack(spacing: DesignTokens.Spacing.md) {
            Button(action: onTap) {
                HStack(spacing: 0) {
                    Text(title)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(.white)
                    Text(" · \(seriesStatus) · 全\(totalCount)集")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(.white)
                    Spacer(minLength: 12)
                    Image(systemName: "chevron.up")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(.white)
                }
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 18)
                .frame(height: 72)
                .background(Color.white.opacity(0.10))
                .clipShape(RoundedRectangle(cornerRadius: 18))
            }
            .buttonStyle(.plain)

            Image(systemName: "viewfinder")
                .font(.system(size: 22, weight: .regular))
                .foregroundStyle(.white)
                .frame(width: 44, height: 44)
        }
    }
}
