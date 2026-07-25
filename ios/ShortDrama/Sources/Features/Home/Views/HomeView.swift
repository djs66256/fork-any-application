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

private struct HomeDramaCardView: View {
    let drama: Drama
    let onPlay: () -> Void
    let onDetail: () -> Void

    private var metadataText: String {
        var items: [String] = []

        if !drama.category.isEmpty {
            items.append(drama.category)
        }

        if let firstTag = drama.tags?.first, !firstTag.isEmpty {
            items.append("#\(firstTag)")
        }

        items.append("\(drama.episodeCount) 集")

        if let rating = drama.rating {
            items.append(String(format: "%.1f 分", rating))
        }

        return items.joined(separator: " · ")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
            DramaCoverView(coverURL: drama.coverUrl)

            VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
                Text(drama.title)
                    .font(.headline)
                    .foregroundStyle(.primary)
                    .lineLimit(2)

                if !drama.description.isEmpty {
                    Text(drama.description)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(3)
                }

                if !metadataText.isEmpty {
                    Text(metadataText)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            HStack(spacing: DesignTokens.Spacing.md) {
                Button("观看") {
                    onPlay()
                }
                .buttonStyle(.borderedProminent)

                Button("详情") {
                    onDetail()
                }
                .buttonStyle(.bordered)
            }
        }
        .padding(DesignTokens.Spacing.lg)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))
    }
}

private struct DramaCoverView: View {
    let coverURL: String

    var body: some View {
        Group {
            if let url = URL(string: coverURL), !coverURL.isEmpty {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                    default:
                        placeholder
                    }
                }
            } else {
                placeholder
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 180)
        .clipped()
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.md))
    }

    private var placeholder: some View {
        ZStack {
            RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.md)
                .fill(Color(.tertiarySystemFill))
            Image(systemName: "photo")
                .font(.system(size: DesignTokens.IconSize.lg))
                .foregroundStyle(.secondary)
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
