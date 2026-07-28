import SwiftUI

struct AppShellView: View {
    @EnvironmentObject private var router: NavigationRouter
    @EnvironmentObject private var authStore: AuthStore

    var body: some View {
        TabView(selection: $router.selectedTab) {
            ForEach(AppTab.allCases) { tab in
                TabNavigationHostView(tab: tab)
                    .tabItem {
                        Label(tab.title, systemImage: tab.systemImage)
                    }
                    .tag(tab)
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
