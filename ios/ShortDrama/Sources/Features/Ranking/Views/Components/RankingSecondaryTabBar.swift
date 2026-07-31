import SwiftUI

struct RankingSecondaryTabBar: View {
    let selected: RankingType
    let onSelect: (RankingType) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: DesignTokens.Spacing.sm) {
                ForEach(RankingType.allCases, id: \.self) { type in
                    Button {
                        onSelect(type)
                    } label: {
                        Text(type.title)
                            .font(.system(size: 17, weight: selected == type ? .semibold : .medium))
                            .foregroundStyle(selected == type ? type.tint : Color.secondary)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 10)
                            .background {
                                Capsule(style: .continuous)
                                    .fill(selected == type ? type.backgroundTint : Color(.secondarySystemBackground))
                            }
                    }
                    .buttonStyle(RankingSecondaryTabButtonStyle())
                }

                Button {
                } label: {
                    HStack(spacing: 4) {
                        Text("分类")
                            .font(.system(size: 17, weight: .semibold))
                        Image(systemName: "chevron.down")
                            .font(.system(size: 12, weight: .bold))
                    }
                    .foregroundStyle(Color.primary)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .background {
                        Capsule(style: .continuous)
                            .fill(Color(.secondarySystemBackground))
                    }
                }
                .buttonStyle(RankingSecondaryTabButtonStyle())
            }
            .padding(.horizontal, DesignTokens.Spacing.md)
            .padding(.bottom, DesignTokens.Spacing.sm)
        }
        .scrollIndicators(.hidden)
    }
}

private struct RankingSecondaryTabButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.82 : 1)
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

private extension RankingType {
    var tint: Color {
        switch self {
        case .hot:
            return Color(red: 1.0, green: 0.49, blue: 0.18)
        case .recommend:
            return Color(red: 0.98, green: 0.66, blue: 0.17)
        case .booking:
            return Color(red: 1.0, green: 0.49, blue: 0.18)
        }
    }

    var backgroundTint: Color {
        switch self {
        case .hot:
            return Color(red: 1.0, green: 0.95, blue: 0.89)
        case .recommend:
            return Color(red: 1.0, green: 0.96, blue: 0.89)
        case .booking:
            return Color(red: 1.0, green: 0.95, blue: 0.89)
        }
    }
}
