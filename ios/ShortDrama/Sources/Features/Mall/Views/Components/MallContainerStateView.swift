import SwiftUI

struct MallContainerStateView: View {
    let state: MallContainerState
    let onRetry: () -> Void

    var body: some View {
        switch state {
        case .loading:
            VStack(spacing: DesignTokens.Spacing.md) {
                ProgressView()
                Text("正在打开商城")
                    .font(.headline)
                    .foregroundStyle(.primary)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding(DesignTokens.Spacing.xl)
        case .success:
            EmptyView()
        case .error(let message):
            VStack(spacing: DesignTokens.Spacing.lg) {
                Image(systemName: "wifi.exclamationmark")
                    .font(.system(size: 52))
                    .foregroundStyle(.secondary)
                Text("商城暂时不可用")
                    .font(.title3)
                    .fontWeight(.semibold)
                Text(message)
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                Button("重试", action: onRetry)
                    .buttonStyle(.borderedProminent)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding(DesignTokens.Spacing.xl)
        }
    }
}

#Preview {
    MallContainerStateView(state: .error(message: "商城加载失败，请稍后重试"), onRetry: {})
}
