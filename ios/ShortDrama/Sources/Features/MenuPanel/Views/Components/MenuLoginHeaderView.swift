import SwiftUI

struct MenuLoginHeaderView: View {
    let onTapLogin: () -> Void

    var body: some View {
        HStack(alignment: .center, spacing: DesignTokens.Spacing.md) {
            ZStack {
                Circle()
                    .fill(Color.accentColor.opacity(0.12))
                    .frame(width: 56, height: 56)
                Image(systemName: "person.fill")
                    .font(.system(size: 24))
                    .foregroundStyle(Color.accentColor)
            }

            VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
                Text("登录后体验更多功能")
                    .font(.headline)
                Text("同步你的播放偏好、消息与个人资产")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            Spacer(minLength: DesignTokens.Spacing.sm)
        }
        .padding(DesignTokens.Spacing.lg)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))
        .overlay(alignment: .bottomTrailing) {
            Button("立即登录", action: onTapLogin)
                .buttonStyle(.borderedProminent)
                .padding(DesignTokens.Spacing.md)
        }
    }
}
