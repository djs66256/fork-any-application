import SwiftUI

struct MenuCommonFunctionsSection: View {
    let onTapBooking: () -> Void
    let onTapDownloads: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("常用功能")
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(MenuPanelStyle.title)
                .padding(.horizontal, 4)

            VStack(spacing: 0) {
                actionRow(icon: "calendar.badge.clock", title: "我的预约", action: onTapBooking)
                Divider()
                    .padding(.leading, 54)
                actionRow(icon: "arrow.down.circle", title: "我的下载", action: onTapDownloads)
            }
            .background(sectionBackground)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func actionRow(icon: String, title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Image(systemName: icon)
                    .font(.system(size: 24, weight: .regular))
                    .foregroundStyle(MenuPanelStyle.title)
                    .frame(width: 28)

                Text(title)
                    .font(.system(size: 17))
                    .foregroundStyle(MenuPanelStyle.title)

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(MenuPanelStyle.secondaryText)
            }
            .padding(.horizontal, 16)
            .frame(height: 72)
        }
        .buttonStyle(.plain)
    }

    private var sectionBackground: some View {
        RoundedRectangle(cornerRadius: 18, style: .continuous)
            .fill(MenuPanelStyle.cardBackground)
            .overlay {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(MenuPanelStyle.cardBorder, lineWidth: 1)
            }
    }
}
