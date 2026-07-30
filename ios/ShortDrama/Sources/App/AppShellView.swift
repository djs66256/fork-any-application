import SwiftUI

struct AppShellView: View {
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
            .tint(.black)
            .onAppear(perform: configureTabBarAppearance)
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

    private func configureTabBarAppearance() {
        let appearance = UITabBarAppearance()
        appearance.configureWithDefaultBackground()
        appearance.backgroundColor = UIColor(white: 1.0, alpha: 0.96)
        appearance.backgroundEffect = UIBlurEffect(style: .systemUltraThinMaterialLight)
        appearance.shadowColor = UIColor.black.withAlphaComponent(0.04)
        appearance.stackedLayoutAppearance.selected.iconColor = .black
        appearance.stackedLayoutAppearance.selected.titleTextAttributes = [
            .foregroundColor: UIColor.black,
            .font: UIFont.systemFont(ofSize: 10.5, weight: .semibold)
        ]
        appearance.stackedLayoutAppearance.normal.iconColor = UIColor.black.withAlphaComponent(0.5)
        appearance.stackedLayoutAppearance.normal.titleTextAttributes = [
            .foregroundColor: UIColor.black.withAlphaComponent(0.5),
            .font: UIFont.systemFont(ofSize: 10.5, weight: .regular)
        ]
        appearance.stackedItemSpacing = 1

        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
    }
}

#Preview {
    AppShellView()
        .environmentObject(NavigationRouter())
        .environmentObject(AuthStore())
}
