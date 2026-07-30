import SwiftUI

struct TheaterShortcutGrid: View {
    let shortcuts: [TheaterShortcut]
    let onTap: (TheaterShortcut) -> Void

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 12), count: 4)

    var body: some View {
        LazyVGrid(columns: columns, spacing: 12) {
            ForEach(shortcuts) { shortcut in
                Button {
                    onTap(shortcut)
                } label: {
                    HStack(spacing: 4) {
                        iconView(for: shortcut)
                        Text(shortcut.title)
                            .font(.system(size: shortcut == .ranking ? 11 : 12, weight: .semibold))
                            .foregroundStyle(Color.primary)
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                        Spacer(minLength: 0)
                    }
                    .frame(maxWidth: .infinity, minHeight: 60, alignment: .leading)
                    .padding(.horizontal, 7)
                    .background(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(Color.white.opacity(0.95))
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(Color.black.opacity(0.03), lineWidth: 1)
                    )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
    }

    @ViewBuilder
    private func iconView(for shortcut: TheaterShortcut) -> some View {
        let style = shortcut.style

        ZStack {
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: style.colors,
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: 31, height: 31)

            Image(systemName: shortcut.systemImage)
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(.white)
        }
    }

}

#Preview {
    TheaterShortcutGrid(shortcuts: TheaterShortcut.allCases, onTap: { _ in })
}
