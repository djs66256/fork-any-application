import SwiftUI

struct AppShellView: View {
    @EnvironmentObject private var router: NavigationRouter

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
        .task {
            router.markContainerReady()
        }
    }
}

#Preview {
    AppShellView()
        .environmentObject(NavigationRouter())
}
