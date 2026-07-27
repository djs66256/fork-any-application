import SwiftUI

struct MenuGameCenterSection: View {
    let onTapGame: () -> Void

    private let items: [(icon: String, title: String)] = [
        ("gamecontroller", "小游戏"),
        ("crown", "领奖励"),
        ("sparkles.tv", "新玩法"),
        ("gift", "福利")
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
            Text("游戏中心")
                .font(.title3)
                .fontWeight(.semibold)

            let columns = [GridItem(.flexible()), GridItem(.flexible())]
            LazyVGrid(columns: columns, spacing: DesignTokens.Spacing.md) {
                ForEach(items, id: \.title) { item in
                    Button(action: onTapGame) {
                        VStack(spacing: DesignTokens.Spacing.sm) {
                            Image(systemName: item.icon)
                                .font(.system(size: 24))
                                .foregroundStyle(Color.accentColor)
                            Text(item.title)
                                .font(.subheadline)
                                .foregroundStyle(.primary)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, DesignTokens.Spacing.lg)
                        .background(
                            Color(.secondarySystemBackground),
                            in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg)
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}
