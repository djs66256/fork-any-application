import SwiftUI

struct TheaterView: View {

    @EnvironmentObject private var router: NavigationRouter
    @StateObject private var viewModel: TheaterViewModel
    @State private var scanPlaceholderMessage: String?

    init() {
        let repository: DramaRepositoryProtocol = DramaRepository()
        _viewModel = StateObject(
            wrappedValue: TheaterViewModel(
                fetchTheaterFeedUseCase: FetchTheaterFeedUseCase(repository: repository)
            )
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
            topBar
            TheaterChannelTabBar(
                channels: viewModel.channels,
                selectedChannel: viewModel.selectedChannel
            ) { channel in
                Task {
                    await viewModel.selectChannel(channel)
                }
            }
            TheaterShortcutGrid(shortcuts: viewModel.shortcuts, onTap: viewModel.openShortcut(_:))
            contentView
        }
        .padding(.top, DesignTokens.Spacing.md)
        .navigationTitle("剧场")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.loadIfNeeded()
        }
        .onReceive(viewModel.$routeEffect) { effect in
            guard let effect else { return }
            handle(routeEffect: effect)
            viewModel.clearRouteEffect()
        }
        .alert("提示", isPresented: isShowingScanAlert, actions: {
            Button("我知道了", role: .cancel) {
                scanPlaceholderMessage = nil
            }
        }, message: {
            Text(scanPlaceholderMessage ?? "")
        })
    }

    private var topBar: some View {
        HStack(spacing: DesignTokens.Spacing.md) {
            Button {
                viewModel.openSearch()
            } label: {
                HStack(spacing: DesignTokens.Spacing.sm) {
                    Image(systemName: "magnifyingglass")
                    Text("搜索短剧")
                        .foregroundStyle(.secondary)
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, DesignTokens.Spacing.md)
                .padding(.vertical, DesignTokens.Spacing.md)
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))
            }
            .buttonStyle(.plain)

            Button {
                viewModel.openScanPlaceholder()
            } label: {
                Image(systemName: "camera.viewfinder")
                    .font(.system(size: DesignTokens.IconSize.md))
                    .frame(width: 44, height: 44)
                    .background(Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
    }

    @ViewBuilder
    private var contentView: some View {
        switch viewModel.viewState {
        case .loading:
            stateContainer(systemImage: "progress.indicator", title: "正在加载剧场内容…", message: nil) {
                ProgressView()
            }
        case .content(let dramas):
            TheaterFeedGridView(
                dramas: dramas,
                isAppending: viewModel.isAppending,
                appendErrorMessage: viewModel.appendErrorMessage,
                onTapDrama: viewModel.openDrama(_:),
                onLoadMore: {
                    Task {
                        await viewModel.loadMoreIfNeeded()
                    }
                }
            )
        case .empty:
            stateContainer(systemImage: "tray", title: "当前频道内容筹备中", message: "可以切换其他频道继续找剧。") {
                EmptyView()
            }
        case .error(let message):
            stateContainer(systemImage: "wifi.exclamationmark", title: "加载失败", message: message) {
                Button("重试") {
                    Task {
                        await viewModel.retry()
                    }
                }
                .buttonStyle(.borderedProminent)
            }
        }
    }

    @ViewBuilder
    private func stateContainer<Action: View>(
        systemImage: String,
        title: String,
        message: String?,
        @ViewBuilder action: () -> Action
    ) -> some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            if systemImage == "progress.indicator" {
                EmptyView()
            } else {
                Image(systemName: systemImage)
                    .font(.system(size: DesignTokens.IconSize.xl))
                    .foregroundStyle(.secondary)
            }

            Text(title)
                .font(.headline)

            if let message {
                Text(message)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }

            action()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(DesignTokens.Spacing.xl)
    }

    private func handle(routeEffect: TheaterViewModel.RouteEffect) {
        switch routeEffect {
        case .navigate(let route):
            router.navigate(to: route)
        case .openRanking(let context):
            router.openRanking(from: context)
        case .showScanPlaceholder(let message):
            scanPlaceholderMessage = message
        }
    }

    private var isShowingScanAlert: Binding<Bool> {
        Binding(
            get: { scanPlaceholderMessage != nil },
            set: { isPresented in
                if !isPresented {
                    scanPlaceholderMessage = nil
                }
            }
        )
    }
}

#Preview {
    NavigationStack {
        TheaterView()
            .environmentObject(NavigationRouter())
    }
}
