import SwiftUI

/// Shared search bar section for discovery and results pages.
struct SearchBarSection: View {
    let query: String
    let canSubmit: Bool
    let onQueryChange: (String) -> Void
    let onSubmit: () -> Void

    var body: some View {
        HStack(spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 17, weight: .regular))
                    .foregroundStyle(Color(uiColor: .systemGray3))

                TextField("观仙道途，我在修仙界无敌", text: Binding(get: {
                    query
                }, set: { newValue in
                    onQueryChange(newValue)
                }))
                .font(.system(size: 15))
                .foregroundStyle(.primary)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled(true)
                .submitLabel(.search)
                .onSubmit(onSubmit)
            }
            .padding(.horizontal, 14)
            .frame(height: 42)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(red: 0.95, green: 0.95, blue: 0.96))
            .clipShape(RoundedRectangle(cornerRadius: 11, style: .continuous))

            Button("搜索") {
                onSubmit()
            }
            .font(.system(size: 17, weight: .medium))
            .foregroundStyle(.primary)
            .buttonStyle(.plain)
            .disabled(!canSubmit)
            .opacity(canSubmit ? 1 : 0.38)
        }
    }
}
