import SwiftUI

struct RankingStateView: View {
    let viewState: RankingViewModel.ViewState
    let selectedRankingType: RankingType
    let isAppending: Bool
    let appendErrorMessage: String?
    let bookingErrorMessage: String?
    let onTapDrama: (RankingDrama) -> Void
    let onTapBooking: (RankingDrama) -> Void
    let onRetry: () async -> Void
    let onLoadMore: () -> Void

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            if let bookingErrorMessage {
                Text(bookingErrorMessage)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, DesignTokens.Spacing.lg)
            }

            switch viewState {
            case .loading:
                rankingLoadingView
            case .content(let dramas):
                RankingListView(
                    dramas: dramas,
                    rankingType: selectedRankingType,
                    isAppending: isAppending,
                    appendErrorMessage: appendErrorMessage,
                    onTapDrama: onTapDrama,
                    onTapBooking: onTapBooking,
                    onLoadMore: onLoadMore
                )
            case .empty:
                rankingEmptyView
            case .error(let message):
                RankingErrorView(message: message, onRetry: onRetry)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }

    private var rankingLoadingView: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            ProgressView()
            Text("正在加载排行榜…")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(DesignTokens.Spacing.xl)
    }

    private var rankingEmptyView: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            Image(systemName: "tray")
                .font(.system(size: DesignTokens.IconSize.xl))
                .foregroundStyle(.secondary)
            Text("当前榜单暂无内容")
                .font(.headline)
            Text("可以切换榜单类型或稍后再试")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(DesignTokens.Spacing.xl)
    }
}

private struct RankingErrorView: View {
    let message: String
    let onRetry: () async -> Void

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            Image(systemName: "wifi.exclamationmark")
                .font(.system(size: DesignTokens.IconSize.xl))
                .foregroundStyle(.secondary)
            Text("加载失败")
                .font(.headline)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button("重试") {
                Task {
                    await onRetry()
                }
            }
            .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(DesignTokens.Spacing.xl)
    }
}
