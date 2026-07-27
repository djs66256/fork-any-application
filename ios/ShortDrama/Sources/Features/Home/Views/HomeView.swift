import SwiftUI

/// The main home screen of the app.
struct HomeView: View {

    @EnvironmentObject private var router: NavigationRouter
    @StateObject private var viewModel: HomeViewModel

    init() {
        let repository: DramaRepositoryProtocol = DramaRepository()
        let useCase = FetchDramasUseCase(repository: repository)
        _viewModel = StateObject(wrappedValue: HomeViewModel(fetchDramasUseCase: useCase))
    }

    var body: some View {
        Group {
            switch viewModel.viewState {
            case .loading:
                HomeFeedLoadingView()
            case .content(let dramas):
                HomeFeedListView(
                    dramas: dramas,
                    onPlay: handlePlay(for:),
                    onDetail: handleDetail(for:)
                )
            case .empty:
                HomeFeedEmptyView(
                    isRetrying: viewModel.isRetrying,
                    onRetry: { await viewModel.retry() }
                )
            case .error(let message):
                HomeFeedErrorView(
                    message: message,
                    isRetrying: viewModel.isRetrying,
                    onRetry: { await viewModel.retry() }
                )
            }
        }
        .navigationTitle("首页")
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button {
                    router.openMenuPanel()
                } label: {
                    Image(systemName: "line.3.horizontal")
                }
                .accessibilityLabel("打开菜单")
            }

            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    router.navigate(to: .searchHome)
                } label: {
                    Image(systemName: "magnifyingglass")
                }
                .accessibilityLabel("搜索")
            }
        }
        .task {
            await viewModel.loadIfNeeded()
        }
    }

    private func handlePlay(for drama: Drama) {
        guard let route = HomeRouteBuilder.playerRoute(for: drama) else { return }
        router.navigate(to: route)
    }

    private func handleDetail(for drama: Drama) {
        guard let route = HomeRouteBuilder.detailRoute(for: drama) else { return }
        router.navigate(to: route)
    }
}

enum HomeRouteBuilder {
    static func playerRoute(for drama: Drama) -> AppRoute? {
        guard !drama.id.isEmpty else { return nil }
        return .player(videoId: drama.id)
    }

    static func detailRoute(for drama: Drama) -> AppRoute? {
        guard !drama.id.isEmpty else { return nil }
        return .dramaDetail(dramaId: drama.id)
    }
}

private struct HomeFeedListView: View {
    let dramas: [Drama]
    let onPlay: (Drama) -> Void
    let onDetail: (Drama) -> Void

    var body: some View {
        ScrollView {
            LazyVStack(spacing: DesignTokens.Spacing.lg) {
                ForEach(dramas) { drama in
                    HomeDramaCardView(
                        drama: drama,
                        onPlay: { onPlay(drama) },
                        onDetail: { onDetail(drama) }
                    )
                }
            }
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.vertical, DesignTokens.Spacing.md)
        }
    }
}

private struct HomeFeedLoadingView: View {
    var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            ProgressView()
            Text("正在加载首页内容…")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(DesignTokens.Spacing.xl)
    }
}

private struct HomeFeedEmptyView: View {
    let isRetrying: Bool
    let onRetry: () async -> Void

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            Image(systemName: "tray")
                .font(.system(size: DesignTokens.IconSize.xl))
                .foregroundStyle(.secondary)
            Text("暂无内容")
                .font(.headline)
            Text("稍后再来看看新的短剧推荐")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Button(isRetrying ? "刷新中…" : "刷新首页") {
                Task {
                    await onRetry()
                }
            }
            .buttonStyle(.bordered)
            .disabled(isRetrying)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(DesignTokens.Spacing.xl)
    }
}

private struct HomeFeedErrorView: View {
    let message: String
    let isRetrying: Bool
    let onRetry: () async -> Void

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            Image(systemName: "wifi.exclamationmark")
                .font(.system(size: DesignTokens.IconSize.xl))
                .foregroundStyle(.secondary)
            Text("加载失败")
                .font(.headline)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button(isRetrying ? "重试中…" : "重试") {
                Task {
                    await onRetry()
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(isRetrying)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(DesignTokens.Spacing.xl)
    }
}

#Preview {
    HomeView()
        .environmentObject(NavigationRouter())
}
