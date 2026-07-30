import SwiftUI

struct BookingAssetsTabBar: View {
    let selectedStatus: BookingAssetAvailabilityStatus
    let summary: BookingAssetSummary
    let onSelect: (BookingAssetAvailabilityStatus) -> Void

    var body: some View {
        HStack(spacing: DesignTokens.Spacing.sm) {
            ForEach(BookingAssetAvailabilityStatus.allCases, id: \.self) { status in
                Button {
                    onSelect(status)
                } label: {
                    Text("\(status.title)(\(summary.count(for: status)))")
                }
                .buttonStyle(BookingAssetsTabButtonStyle(isSelected: selectedStatus == status))
            }
        }
    }
}

private struct BookingAssetsTabButtonStyle: ButtonStyle {
    let isSelected: Bool

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(isSelected ? Color.white : Color.primary)
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.vertical, DesignTokens.Spacing.sm)
            .frame(maxWidth: .infinity)
            .background(isSelected ? Color.accentColor : Color(.secondarySystemBackground))
            .clipShape(Capsule())
            .opacity(configuration.isPressed ? 0.85 : 1)
    }
}
