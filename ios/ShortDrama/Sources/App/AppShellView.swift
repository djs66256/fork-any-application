import SwiftUI
import UIKit

struct AppShellView: View {

    init() {
        let appearance = UITabBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor(DesignTokens.HomeChrome.tabBarBackground)
        appearance.shadowColor = UIColor(DesignTokens.HomeChrome.tabBarHairline)
        appearance.stackedItemPositioning = .fill
        appearance.stackedItemWidth = 0
        appearance.stackedItemSpacing = 0

        let selectedColor = UIColor(DesignTokens.HomeChrome.tabBarSelected)
        let unselectedColor = UIColor(DesignTokens.HomeChrome.tabBarUnselected)
        let indicatorColor = UIColor(DesignTokens.HomeChrome.tabBarIndicator)

        [appearance.stackedLayoutAppearance, appearance.inlineLayoutAppearance, appearance.compactInlineLayoutAppearance]
            .forEach { itemAppearance in
                itemAppearance.selected.iconColor = selectedColor
                itemAppearance.selected.titleTextAttributes = [.foregroundColor: selectedColor]
                itemAppearance.normal.iconColor = unselectedColor
                itemAppearance.normal.titleTextAttributes = [.foregroundColor: unselectedColor]
                itemAppearance.selected.badgeBackgroundColor = indicatorColor
                itemAppearance.normal.badgeBackgroundColor = indicatorColor
                itemAppearance.normal.titlePositionAdjustment = UIOffset(horizontal: 0, vertical: 1)
                itemAppearance.selected.titlePositionAdjustment = UIOffset(horizontal: 0, vertical: 1)
            }

        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
    }
    @EnvironmentObject private var router: NavigationRouter
    @EnvironmentObject private var authStore: AuthStore
    @StateObject private var menuPanelViewModel = MenuPanelViewModel(
        fetchRecentlyViewedUseCase: FetchRecentlyViewedUseCase(repository: MenuPanelRepository()),
        fetchMessagePreviewUseCase: FetchMessagePreviewUseCase(repository: MessageRepository()),
        playbackSessionStore: KeychainPlaybackSessionStore()
    )

    var body: some View {
        ZStack(alignment: .leading) {
            TabView(selection: $router.selectedTab) {
                ForEach(AppTab.allCases) { tab in
                    TabNavigationHostView(tab: tab)
                        .tabItem {
                            Label(tab.title, systemImage: tab.systemImage)
                        }
                        .tag(tab)
                }
            }
            .disabled(router.isMenuPanelVisible)

            if router.selectedTab == .home && router.isMenuPanelVisible {
                MenuPanelContainerView(router: router, viewModel: menuPanelViewModel)
                    .transition(.opacity)
                    .zIndex(1)
            }
        }
        .fullScreenCover(item: presentedLoginContextBinding) { context in
            LoginView(
                context: context,
                onClose: {
                    router.cancelLogin()
                },
                onSuccess: {
                    router.completeLogin()
                },
                onLoginSuccess: { session in
                    try await authStore.handleLoginSuccess(session)
                }
            )
        }
        .task {
            await authStore.restoreIfNeeded()
            router.markContainerReady()
        }
    }

    private var presentedLoginContextBinding: Binding<LoginInterceptionContext?> {
        Binding(
            get: { router.presentedLoginContext },
            set: { context in
                if let context {
                    router.presentLogin(context: context)
                } else {
                    router.cancelLogin()
                }
            }
        )
    }
}

#Preview {
    AppShellView()
        .environmentObject(NavigationRouter())
        .environmentObject(AuthStore())
}
