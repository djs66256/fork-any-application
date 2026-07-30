import SwiftUI

struct MenuLoginHeaderView: View {
    let onTapLogin: () -> Void

    var body: some View {
        HStack(alignment: .center, spacing: 14) {
            ZStack {
                Circle()
                    .fill(Color.black.opacity(0.04))
                    .overlay {
                        Circle()
                            .stroke(MenuPanelStyle.iconPlaceholderStroke, lineWidth: 1)
                    }

                Image(systemName: "play.circle")
                    .font(.system(size: 21, weight: .regular))
                    .foregroundStyle(MenuPanelStyle.tertiaryText)
            }
            .frame(width: 58, height: 58)

            Text("登录看完整信息")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(MenuPanelStyle.title)
                .lineLimit(1)

            Spacer(minLength: 12)

            Button(action: onTapLogin) {
                Text("立即登录")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 20)
                    .frame(height: 44)
                    .background(MenuPanelStyle.accent)
                    .clipShape(RoundedRectangle(cornerRadius: 15, style: .continuous))
            }
            .buttonStyle(.plain)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
