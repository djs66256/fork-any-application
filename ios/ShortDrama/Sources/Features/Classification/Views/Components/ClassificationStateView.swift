import SwiftUI

struct ClassificationStateView<Content: View>: View {
    let viewState: ClassificationViewModel.ViewState
    let onRetry: () async -> Void
    @ViewBuilder let content: () -> Content

    var body: some View {
        switch viewState {
        case .loading:
            loadingView
        case .content:
            content()
        case .error(let message):
            errorView(message: message)
        }
    }

    private var loadingView: some View {
        VStack(spacing: 14) {
            ProgressView()
                .tint(Color.black)
            Text("正在加载分类…")
                .font(.system(size: 14))
                .foregroundStyle(Color(red: 0.55, green: 0.55, blue: 0.55))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(Color(red: 0.97, green: 0.97, blue: 0.97))
        )
    }

    private func errorView(message: String) -> some View {
        VStack(spacing: 14) {
            Image(systemName: "wifi.exclamationmark")
                .font(.system(size: 34))
                .foregroundStyle(Color(red: 0.65, green: 0.65, blue: 0.65))
            Text("加载失败")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(Color.black)
            Text(message)
                .font(.system(size: 13))
                .foregroundStyle(Color(red: 0.55, green: 0.55, blue: 0.55))
                .multilineTextAlignment(.center)
            Button("重试") {
                Task {
                    await onRetry()
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(Color.black)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.horizontal, 24)
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(Color(red: 0.97, green: 0.97, blue: 0.97))
        )
    }
}
