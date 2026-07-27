import SwiftUI

struct MenuPanelContainerView: View {
    @ObservedObject var router: NavigationRouter
    @ObservedObject var viewModel: MenuPanelViewModel
    @State private var isGameHintVisible = false

    var body: some View {
        GeometryReader { proxy in
            let panelWidth = min(proxy.size.width * 0.78, 360)

            ZStack(alignment: .leading) {
                Color.black.opacity(0.35)
                    .ignoresSafeArea()
                    .contentShape(Rectangle())
                    .onTapGesture {
                        router.closeMenuPanel()
                    }

                MenuPanelView(
                    state: viewModel.viewState,
                    isRetrying: viewModel.isRetrying,
                    onTapLogin: {
                        router.closeMenuPanelThenNavigate(to: .menuPlaceholder(kind: .login))
                    },
                    onTapMessages: {
                        router.closeMenuPanelThenNavigate(to: .menuPlaceholder(kind: .messages))
                    },
                    onTapRecentlyViewed: { item in
                        guard let route = viewModel.route(for: item) else { return }
                        router.closeMenuPanelThenNavigate(to: route)
                    },
                    onTapBooking: {
                        router.closeMenuPanelThenNavigate(to: .menuPlaceholder(kind: .booking))
                    },
                    onTapDownloads: {
                        router.closeMenuPanelThenNavigate(to: .menuPlaceholder(kind: .downloads))
                    },
                    onTapGame: {
                        isGameHintVisible = true
                    },
                    onRetryRecentlyViewed: {
                        await viewModel.retry()
                    }
                )
                .frame(width: panelWidth)
                .frame(maxHeight: .infinity)
                .background(Color(.systemBackground))
                .offset(x: router.menuPanelState == .closing ? -panelWidth : 0)
                .animation(.easeInOut(duration: 0.25), value: router.menuPanelState)
                .task(id: router.menuPanelState) {
                    if router.menuPanelState == .open || router.menuPanelState == .opening {
                        await viewModel.loadIfNeeded()
                    }

                    if router.menuPanelState == .closing {
                        try? await Task.sleep(for: .milliseconds(250))
                        guard !Task.isCancelled else { return }
                        await MainActor.run {
                            router.markMenuPanelDidClose()
                        }
                    }
                }
            }
        }
        .alert("即将上线", isPresented: $isGameHintVisible) {
            Button("我知道了", role: .cancel) {}
        } message: {
            Text("游戏中心正在建设中，敬请期待。")
        }
    }
}
