import SwiftUI

struct MenuPlaceholderView: View {
    let kind: MenuPlaceholderKind

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.lg) {
            Image(systemName: kind.iconName)
                .font(.system(size: 52))
                .foregroundStyle(Color.accentColor)

            Text(kind.title)
                .font(.title2)
                .fontWeight(.semibold)

            Text(kind.description)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, DesignTokens.Spacing.xl)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(DesignTokens.Spacing.xl)
        .navigationTitle(kind.title)
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview {
    NavigationStack {
        MenuPlaceholderView(kind: .login)
    }
}
