import SwiftUI

/// Quick entry grid shown on the search discovery page.
struct QuickEntryGrid: View {
    let entries: [QuickEntry]
    let onTap: (QuickEntry) -> Void

    private let columns = [
        GridItem(.flexible(), spacing: DesignTokens.Spacing.md),
        GridItem(.flexible(), spacing: DesignTokens.Spacing.md)
    ]

    var body: some View {
        LazyVGrid(columns: columns, spacing: DesignTokens.Spacing.md) {
            ForEach(entries) { entry in
                Button {
                    onTap(entry)
                } label: {
                    VStack(spacing: DesignTokens.Spacing.sm) {
                        Image(systemName: entry.systemImage)
                            .font(.system(size: DesignTokens.IconSize.md))
                        Text(entry.title)
                            .font(.subheadline)
                            .lineLimit(1)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, DesignTokens.Spacing.lg)
                    .background(Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))
                }
                .buttonStyle(.plain)
            }
        }
    }
}
