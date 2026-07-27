import SwiftUI

struct MenuPanelView: View {
    let state: MenuPanelViewModel.RecentlyViewedState
    let isRetrying: Bool
    let onTapLogin: () -> Void
    let onTapMessages: () -> Void
    let onTapRecentlyViewed: (RecentlyViewedItem) -> Void
    let onTapBooking: () -> Void
    let onTapDownloads: () -> Void
    let onTapGame: () -> Void
    let onRetryRecentlyViewed: () async -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.lg) {
                MenuLoginHeaderView(onTapLogin: onTapLogin)
                MenuMessagePreviewView(onTapMessages: onTapMessages)
                MenuRecentlyViewedSection(
                    state: state,
                    isRetrying: isRetrying,
                    onRetry: onRetryRecentlyViewed,
                    onTapItem: onTapRecentlyViewed
                )
                MenuGameCenterSection(onTapGame: onTapGame)
                MenuCommonFunctionsSection(
                    onTapBooking: onTapBooking,
                    onTapDownloads: onTapDownloads
                )
            }
            .padding(DesignTokens.Spacing.lg)
        }
        .scrollIndicators(.hidden)
        .background(Color(.systemBackground))
    }
}
