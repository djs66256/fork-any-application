import SwiftUI

struct BookingAssetsLoginGateView: View {
    let onTapLogin: () -> Void

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.lg) {
            Image(systemName: "person.crop.circle.badge.exclamationmark")
                .font(.system(size: 56))
                .foregroundStyle(Color.accentColor)

            Text("登录后查看我的预约")
                .font(.title3)
                .fontWeight(.semibold)

            Text("登录后可查看已上线与待上线的预约内容，并在当前页面继续浏览。")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            Button("立即登录") {
                onTapLogin()
            }
            .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(DesignTokens.Spacing.xl)
    }
}
