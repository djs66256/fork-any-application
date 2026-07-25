import SwiftUI

struct PlaceholderTabView: View {
    let tab: AppTab

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            Image(systemName: tab.systemImage)
                .font(.system(size: 48))
                .foregroundColor(.accentColor)

            Text(tab.title)
                .font(.title2)
                .fontWeight(.semibold)

            Text("\(tab.title)频道占位页，后续 PRD 会在这里接入真实内容。")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding()
    }
}

#Preview {
    PlaceholderTabView(tab: .theater)
}
