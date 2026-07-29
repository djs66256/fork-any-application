import SwiftUI

struct EarnLoginPlaceholderView: View {
    let context: EarnLoginContext
    let onClose: () -> Void
    let onCompleteLogin: () -> Void

    var body: some View {
        NavigationStack {
            VStack(spacing: DesignTokens.Spacing.lg) {
                Image(systemName: "person.crop.circle.badge.plus")
                    .font(.system(size: 56))
                    .foregroundStyle(Color.accentColor)

                Text("登录后继续完成赚钱任务")
                    .font(.title3)
                    .fontWeight(.semibold)

                Text("赚钱中心登录承接页建设中。当前会保留赚钱上下文，并在返回后继续停留赚钱中心。")
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, DesignTokens.Spacing.xl)

                Text("返回目标：\(context.returnTarget)")
                    .font(.footnote)
                    .foregroundStyle(.secondary)

                HStack(spacing: DesignTokens.Spacing.md) {
                    Button("取消", role: .cancel, action: onClose)
                        .buttonStyle(.bordered)
                    Button("模拟登录成功", action: onCompleteLogin)
                        .buttonStyle(.borderedProminent)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding(DesignTokens.Spacing.xl)
            .navigationTitle("赚钱登录")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭", action: onClose)
                }
            }
        }
    }
}

private func makeEarnLoginPreviewContext() -> EarnLoginContext {
    guard let context = EarnLoginContext(source: "earn", returnTarget: "/earn") else {
        preconditionFailure("Expected valid earn login preview context")
    }
    return context
}

#Preview {
    EarnLoginPlaceholderView(
        context: makeEarnLoginPreviewContext(),
        onClose: {},
        onCompleteLogin: {}
    )
}
