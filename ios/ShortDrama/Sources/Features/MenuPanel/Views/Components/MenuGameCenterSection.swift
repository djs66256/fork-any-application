import SwiftUI

struct MenuGameCenterSection: View {
    let onTapGame: () -> Void

    private let items: [(title: String, background: Color, symbol: String)] = [
        ("赵云与阿...", Color(red: 0.96, green: 0.82, blue: 0.15), "scribble.variable"),
        ("抓大鹅", Color(red: 0.22, green: 0.62, blue: 0.96), "hand.draw"),
        ("梦幻消除...", Color(red: 0.86, green: 0.75, blue: 0.72), "sparkles"),
        ("羊了个羊...", Color(red: 0.73, green: 0.92, blue: 0.46), "tshirt")
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .firstTextBaseline) {
                Text("游戏中心")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(MenuPanelStyle.title)

                Spacer()

                HStack(spacing: 4) {
                    Text("更多")
                        .font(.system(size: 16))
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .semibold))
                }
                .foregroundStyle(MenuPanelStyle.secondaryText)
            }
            .padding(.horizontal, 4)

            HStack(alignment: .top, spacing: 18) {
                ForEach(items, id: \.title) { item in
                    Button(action: onTapGame) {
                        VStack(alignment: .leading, spacing: 10) {
                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .fill(item.background)
                                .frame(width: 62, height: 62)
                                .overlay {
                                    Image(systemName: item.symbol)
                                        .font(.system(size: 28, weight: .medium))
                                        .foregroundStyle(Color.black.opacity(0.76))
                                }

                            Text(item.title)
                                .font(.system(size: 14.5, weight: .medium))
                                .foregroundStyle(MenuPanelStyle.title)
                                .lineLimit(1)
                                .frame(width: 74, alignment: .leading)
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .padding(.bottom, 18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(sectionBackground)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
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
