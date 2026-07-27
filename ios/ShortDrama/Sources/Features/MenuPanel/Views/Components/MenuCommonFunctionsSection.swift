import SwiftUI

struct MenuCommonFunctionsSection: View {
    let onTapBooking: () -> Void
    let onTapDownloads: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
            Text("常用功能")
                .font(.title3)
                .fontWeight(.semibold)

            VStack(spacing: DesignTokens.Spacing.sm) {
                actionRow(icon: "calendar", title: "我的预约", action: onTapBooking)
                actionRow(icon: "arrow.down.circle", title: "我的下载", action: onTapDownloads)
            }
        }
    }

    private func actionRow(icon: String, title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: DesignTokens.Spacing.md) {
                Image(systemName: icon)
                    .foregroundStyle(Color.accentColor)
                Text(title)
                    .foregroundStyle(.primary)
                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundStyle(.tertiary)
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
