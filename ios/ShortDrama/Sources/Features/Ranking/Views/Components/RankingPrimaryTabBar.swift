import SwiftUI

struct RankingPrimaryTabBar: View {
    let selected: RankingContentType
    let onSelect: (RankingContentType) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: DesignTokens.Spacing.sm) {
                ForEach(RankingContentType.allCases, id: \.self) { type in
                    Button(type.title) {
                        onSelect(type)
                    }
                    .buttonStyle(RankingTabButtonStyle(isSelected: selected == type))
                }
            }
            .padding(.horizontal, DesignTokens.Spacing.lg)
        }
    }
}

private struct RankingTabButtonStyle: ButtonStyle {
    let isSelected: Bool

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(isSelected ? Color.white : Color.primary)
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.vertical, DesignTokens.Spacing.sm)
            .background(isSelected ? Color.accentColor : Color(.secondarySystemBackground))
            .clipShape(Capsule())
            .opacity(configuration.isPressed ? 0.8 : 1)
    }
}
