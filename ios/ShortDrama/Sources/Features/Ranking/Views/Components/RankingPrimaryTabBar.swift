import SwiftUI

struct RankingPrimaryTabBar: View {
    let selected: RankingContentType
    let onSelect: (RankingContentType) -> Void

    var body: some View {
        HStack(spacing: 24) {
            ForEach(RankingContentType.allCases, id: \.self) { type in
                Button {
                    onSelect(type)
                } label: {
                    Text(type.title)
                        .font(.system(size: 18, weight: selected == type ? .bold : .medium))
                        .foregroundStyle(selected == type ? Color.primary : Color.secondary)
                }
                .buttonStyle(RankingPrimaryTabButtonStyle())
            }

            Spacer(minLength: 0)
        }
        .padding(.horizontal, DesignTokens.Spacing.md)
        .padding(.top, DesignTokens.Spacing.sm)
    }
}

private struct RankingPrimaryTabButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.72 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}
