import SwiftUI

struct PlayerStatusView: View {
    let systemImage: String
    let title: String
    let message: String
    let primaryActionTitle: String?
    let primaryAction: (() -> Void)?

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.lg) {
            Image(systemName: systemImage)
                .font(.system(size: 48))
                .foregroundStyle(.white.opacity(0.85))

            Text(title)
                .font(.title3.bold())
                .foregroundStyle(.white)

            Text(message)
                .font(.body)
                .foregroundStyle(.white.opacity(0.8))
                .multilineTextAlignment(.center)
                .padding(.horizontal, DesignTokens.Spacing.xl)

            if let primaryActionTitle, let primaryAction {
                Button(primaryActionTitle, action: primaryAction)
                    .buttonStyle(.borderedProminent)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(DesignTokens.Spacing.xl)
        .background(Color.black)
    }
}
