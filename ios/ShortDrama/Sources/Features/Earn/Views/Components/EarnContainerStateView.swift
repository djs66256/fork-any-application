import SwiftUI

struct EarnContainerStateView: View {
    let state: EarnContainerState
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.lg) {
            switch state {
            case .loading:
                ProgressView("正在加载赚钱中心")
            case .success:
                EmptyView()
            case .error(let message):
                Image(systemName: "wifi.exclamationmark")
                    .font(.system(size: 48))
                    .foregroundStyle(Color.accentColor)

                Text("赚钱页暂时不可用")
                    .font(.title3)
                    .fontWeight(.semibold)

                Text(message)
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, DesignTokens.Spacing.xl)

                Button("重试", action: onRetry)
                    .buttonStyle(.borderedProminent)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(DesignTokens.Spacing.xl)
    }
}

#Preview {
    EarnContainerStateView(state: .error(message: "网络异常"), onRetry: {})
}
