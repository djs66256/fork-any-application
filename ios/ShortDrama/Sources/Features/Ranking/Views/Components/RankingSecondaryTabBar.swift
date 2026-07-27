import SwiftUI

struct RankingSecondaryTabBar: View {
    let selected: RankingType
    let onSelect: (RankingType) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: DesignTokens.Spacing.sm) {
                ForEach(RankingType.allCases, id: \.self) { type in
                    Button(type.title) {
                        onSelect(type)
                    }
                    .buttonStyle(RankingSecondaryTabButtonStyle(isSelected: selected == type))
                }
            }
            .padding(.horizontal, DesignTokens.Spacing.lg)
        }
    }
}

private struct RankingSecondaryTabButtonStyle: ButtonStyle {
    let isSelected: Bool

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.footnote.weight(.medium))
            .foregroundStyle(isSelected ? Color.accentColor : Color.secondary)
            .padding(.horizontal, DesignTokens.Spacing.md)
            .padding(.vertical, DesignTokens.Spacing.sm)
            .background(isSelected ? Color.accentColor.opacity(0.12) : Color.clear)
            .clipShape(Capsule())
            .overlay {
                Capsule()
                    .stroke(isSelected ? Color.accentColor.opacity(0.2) : Color(.separator), lineWidth: 1)
            }
            .opacity(configuration.isPressed ? 0.8 : 1)
    }
}
