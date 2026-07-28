import SwiftUI

struct AppShellView: View {
    @EnvironmentObject private var router: NavigationRouter
    @EnvironmentObject private var authStore: AuthStore
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
