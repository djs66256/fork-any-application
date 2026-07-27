import SwiftUI

struct AppShellView: View {
    @EnvironmentObject private var router: NavigationRouter
    @StateObject private var menuPanelViewModel = MenuPanelViewModel(
        fetchRecentlyViewedUseCase: FetchRecentlyViewedUseCase(repository: MenuPanelRepository()),
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
        .task {
            router.markContainerReady()
        }
    }
}

#Preview {
    AppShellView()
        .environmentObject(NavigationRouter())
}
