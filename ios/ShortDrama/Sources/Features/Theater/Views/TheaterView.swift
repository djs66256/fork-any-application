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
        ZStack(alignment: .top) {
            Color(red: 0.97, green: 0.97, blue: 0.97)
                .ignoresSafeArea()

            Rectangle()
                .fill(Color(red: 0.92, green: 0.96, blue: 0.97))
                .frame(height: 100)
                .ignoresSafeArea(edges: .top)

            VStack(alignment: .leading, spacing: 8) {
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
            .padding(.top, 4)
        }
        .toolbar(.hidden, for: .navigationBar)
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
        HStack(spacing: 10) {
            Button {
                viewModel.openSearch()
            } label: {
                HStack(spacing: 9) {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 18, weight: .regular))
                        .foregroundStyle(Color.black.opacity(0.38))

                    Text("一剑镇狱第二季")
                        .font(.system(size: 16, weight: .regular))
                        .foregroundStyle(Color.black.opacity(0.36))
                        .lineLimit(1)

                    Spacer(minLength: 0)
                }
                .padding(.horizontal, 16)
                .frame(height: 46)
                .background(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(Color.white.opacity(0.82))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Color.white.opacity(0.5), lineWidth: 0.5)
                )
            }
            .buttonStyle(.plain)

            Button {
                viewModel.openScanPlaceholder()
            } label: {
                HStack(spacing: 5) {
                    Image(systemName: "camera")
                        .font(.system(size: 18, weight: .regular))
                    Text("识剧")
                        .font(.system(size: 16, weight: .regular))
                }
                .foregroundStyle(Color.black.opacity(0.38))
                .frame(width: 92, height: 46)
                .background(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(Color.white.opacity(0.82))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Color.white.opacity(0.5), lineWidth: 0.5)
                )
                .overlay(alignment: .leading) {
                    Rectangle()
                        .fill(Color.black.opacity(0.04))
                        .frame(width: 1, height: 20)
                        .offset(x: -9)
                }
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.bottom, 2)
    }

    @ViewBuilder
    private var contentView: some View {
        switch viewModel.viewState {
        case .loading:
            stateContainer(systemImage: nil, title: "正在加载剧场内容…", message: nil) {
                ProgressView()
                    .tint(.orange)
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
            stateContainer(systemImage: "square.grid.2x2", title: "当前频道内容筹备中", message: "可以切换其他频道继续找剧。") {
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
        systemImage: String?,
        title: String,
        message: String?,
        @ViewBuilder action: () -> Action
    ) -> some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            if let systemImage {
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
        .background(Color(red: 0.97, green: 0.97, blue: 0.97))
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
