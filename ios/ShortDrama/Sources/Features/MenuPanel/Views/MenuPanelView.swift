import SwiftUI

struct MenuPanelView: View {
    let state: MenuPanelViewModel.RecentlyViewedState
    let isRetrying: Bool
    let onTapLogin: () -> Void
    let onTapRecentlyViewed: (RecentlyViewedItem) -> Void
    let onTapBooking: () -> Void
    let onTapDownloads: () -> Void
    let onTapGame: () -> Void
    let onRetryRecentlyViewed: () async -> Void

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(alignment: .leading, spacing: 18) {
                MenuLoginHeaderView(onTapLogin: onTapLogin)
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
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 18)
            .padding(.top, 26)
            .padding(.bottom, 28)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(MenuPanelStyle.panelBackground)
    }
}
