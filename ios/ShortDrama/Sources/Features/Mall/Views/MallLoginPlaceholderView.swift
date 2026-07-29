import SwiftUI

struct MallLoginPlaceholderView: View {
    let context: MallLoginContext
    let onClose: () -> Void
    let onCompleteLogin: () -> Void

    var body: some View {
        NavigationStack {
            VStack(spacing: DesignTokens.Spacing.lg) {
                Image(systemName: "person.crop.circle.badge.plus")
                    .font(.system(size: 56))
                    .foregroundStyle(Color.accentColor)

                Text("登录后继续查看商品")
                    .font(.title3)
                    .fontWeight(.semibold)

                Text("商城登录承接页建设中。当前会保留商品上下文，并在返回后继续停留商城。")
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, DesignTokens.Spacing.xl)

                Text("商品 ID：\(context.productID)")
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
            .navigationTitle("商城登录")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭", action: onClose)
                }
            }
        }
    }
}

#Preview {
    MallLoginPlaceholderView(
        context: MallLoginContext(source: "mall", productID: "product-001", returnTarget: "/mall"),
        onClose: {},
        onCompleteLogin: {}
    )
}
