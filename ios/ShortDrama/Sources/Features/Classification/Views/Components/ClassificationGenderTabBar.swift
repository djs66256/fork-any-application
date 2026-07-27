import SwiftUI

struct ClassificationGenderTabBar: View {
    let selectedGender: ClassificationGender
    let onSelect: (ClassificationGender) -> Void

    var body: some View {
        HStack(spacing: DesignTokens.Spacing.sm) {
            ForEach(ClassificationGender.allCases, id: \.self) { gender in
                Button {
                    onSelect(gender)
                } label: {
                    Text(gender.title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(selectedGender == gender ? Color.white : Color.primary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, DesignTokens.Spacing.sm)
                        .background(
                            RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg)
                                .fill(selectedGender == gender ? Color.accentColor : Color(.secondarySystemBackground))
                        )
                }
                .buttonStyle(.plain)
            }
        }
    }
}
