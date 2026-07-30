import SwiftUI

struct MenuPanelContainerView: View {
    @ObservedObject var router: NavigationRouter
    @ObservedObject var viewModel: MenuPanelViewModel
    @State private var isGameHintVisible = false

    var body: some View {
        GeometryReader { proxy in
            let panelWidth = min(proxy.size.width * 0.82, 360)
            let closedOffset = -(panelWidth + 24)

            ZStack(alignment: .leading) {
                Color.black.opacity(0.18)
                    .ignoresSafeArea()
                    .contentShape(Rectangle())
                    .onTapGesture {
                        router.closeMenuPanel()
                    }

                HStack(spacing: 0) {
                    MenuPanelView(
                        state: viewModel.viewState,
                        isRetrying: viewModel.isRetrying,
                        onTapLogin: {
                            router.closeMenuPanelThenNavigate(to: .menuPlaceholder(kind: .login))
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
                    .frame(width: panelWidth, alignment: .leading)
                    .frame(maxHeight: .infinity, alignment: .topLeading)
                    .background(MenuPanelStyle.panelBackground)
                    .ignoresSafeArea(edges: .vertical)

                    Spacer(minLength: 0)
                }
                .offset(x: router.menuPanelState == .closing ? closedOffset : 0)
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
