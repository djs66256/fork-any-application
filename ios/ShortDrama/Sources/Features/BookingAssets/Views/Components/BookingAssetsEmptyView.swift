import SwiftUI

struct BookingAssetsEmptyView: View {
    let status: BookingAssetAvailabilityStatus

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            Image(systemName: status == .online ? "play.rectangle" : "calendar.badge.clock")
                .font(.system(size: DesignTokens.IconSize.xl))
                .foregroundStyle(.secondary)

            Text(status == .online ? "暂无已上线预约" : "暂无待上线预约")
                .font(.headline)

            Text(status == .online ? "你预约的内容上线后会显示在这里。" : "后续有新预约后，可在这里查看待上线内容。")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(DesignTokens.Spacing.xl)
    }
}
