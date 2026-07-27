import SwiftUI

struct ClassificationDimensionRail: View {
    let dimensions: [ClassificationDimension]
    let selectedDimension: ClassificationDimensionKey
    let onSelect: (ClassificationDimensionKey) -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: DesignTokens.Spacing.sm) {
                ForEach(dimensions) { dimension in
                    Button {
                        onSelect(dimension.key)
                    } label: {
                        Text(dimension.name)
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(selectedDimension == dimension.key ? Color.accentColor : Color.primary)
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, DesignTokens.Spacing.md)
                            .padding(.horizontal, DesignTokens.Spacing.sm)
                            .background(
                                RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg)
                                    .fill(
                                        selectedDimension == dimension.key
                                            ? Color.accentColor.opacity(0.12)
                                            : Color(.secondarySystemBackground)
                                    )
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.vertical, DesignTokens.Spacing.xs)
        }
        .scrollIndicators(.hidden)
    }
}
