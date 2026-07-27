import SwiftUI

struct MenuMessagePreviewView: View {
    let onTapMessages: () -> Void

    var body: some View {
        Button(action: onTapMessages) {
            HStack(alignment: .top, spacing: DesignTokens.Spacing.md) {
                Circle()
                    .fill(Color.red)
                    .frame(width: 10, height: 10)
                    .padding(.top, 6)

                VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
                    Text("系统通知")
                        .font(.headline)
                        .foregroundStyle(.primary)
                    Text("精选续播与新内容提醒即将接入，后续会在这里展示你的最新消息。")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.leading)
                }

                Spacer(minLength: DesignTokens.Spacing.sm)

                Image(systemName: "chevron.right")
                    .foregroundStyle(.tertiary)
                    .padding(.top, 4)
            }
            .padding(DesignTokens.Spacing.lg)
            .background(
                Color(.secondarySystemBackground),
                in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg)
            )
        }
        .buttonStyle(.plain)
    }
}
