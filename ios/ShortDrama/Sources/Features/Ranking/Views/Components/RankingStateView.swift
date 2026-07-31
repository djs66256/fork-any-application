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
                HStack(spacing: DesignTokens.Spacing.sm) {
                    Image(systemName: "exclamationmark.circle.fill")
                        .foregroundStyle(Color(red: 1.0, green: 0.49, blue: 0.18))
                    Text(bookingErrorMessage)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, DesignTokens.Spacing.md)
                .padding(.vertical, DesignTokens.Spacing.sm)
                .background {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(Color.white)
                }
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
        RankingPlaceholderPanel(
            systemImage: "chart.bar.xaxis",
            title: "正在刷新排行榜",
            message: "马上为你同步最新的榜单内容。"
        ) {
            ProgressView()
                .tint(Color(red: 1.0, green: 0.49, blue: 0.18))
        }
    }

    private var rankingEmptyView: some View {
        RankingPlaceholderPanel(
            systemImage: "tray",
            title: "当前榜单暂无内容",
            message: "可以切换榜单类型或内容分区后再看看。"
        ) {
            EmptyView()
        }
    }
}

private struct RankingErrorView: View {
    let message: String
    let onRetry: () async -> Void

    var body: some View {
        RankingPlaceholderPanel(
            systemImage: "wifi.exclamationmark",
            title: "加载失败",
            message: message
        ) {
            Button("重新加载") {
                Task {
                    await onRetry()
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(Color(red: 1.0, green: 0.49, blue: 0.18))
        }
    }
}

private struct RankingPlaceholderPanel<Action: View>: View {
    let systemImage: String
    let title: String
    let message: String
    @ViewBuilder let action: () -> Action

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            Image(systemName: systemImage)
                .font(.system(size: 30, weight: .semibold))
                .foregroundStyle(Color(red: 1.0, green: 0.49, blue: 0.18))

            Text(title)
                .font(.headline)
                .foregroundStyle(.primary)

            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            action()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(DesignTokens.Spacing.xl)
        .background {
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(Color.white)
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.top, DesignTokens.Spacing.sm)
    }
}
