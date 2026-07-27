import SwiftUI

/// Shared search bar section for discovery and results pages.
struct SearchBarSection: View {
    let query: String
    let canSubmit: Bool
    let onQueryChange: (String) -> Void
    let onSubmit: () -> Void

    var body: some View {
        HStack(spacing: DesignTokens.Spacing.sm) {
            HStack(spacing: DesignTokens.Spacing.sm) {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(.secondary)

                TextField("搜索短剧", text: Binding(get: {
                    query
                }, set: { newValue in
                    onQueryChange(newValue)
                }))
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled(true)
                .submitLabel(.search)
                .onSubmit(onSubmit)
            }
            .padding(.horizontal, DesignTokens.Spacing.md)
            .padding(.vertical, DesignTokens.Spacing.sm)
            .background(Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))

            Button("搜索") {
                onSubmit()
            }
            .buttonStyle(.borderedProminent)
            .disabled(!canSubmit)
        }
    }
}
