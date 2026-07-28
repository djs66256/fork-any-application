import SwiftUI

struct TheaterShortcutGrid: View {
    let shortcuts: [TheaterShortcut]
    let onTap: (TheaterShortcut) -> Void

    private let columns = Array(repeating: GridItem(.flexible(), spacing: DesignTokens.Spacing.md), count: 4)

    var body: some View {
        LazyVGrid(columns: columns, spacing: DesignTokens.Spacing.md) {
            ForEach(shortcuts) { shortcut in
                Button {
                    onTap(shortcut)
                } label: {
                    VStack(spacing: DesignTokens.Spacing.sm) {
                        Image(systemName: shortcut.systemImage)
                            .font(.system(size: DesignTokens.IconSize.md))
                        Text(shortcut.title)
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
        .padding(.horizontal, DesignTokens.Spacing.lg)
    }
}

#Preview {
    TheaterShortcutGrid(shortcuts: TheaterShortcut.allCases, onTap: { _ in })
}
