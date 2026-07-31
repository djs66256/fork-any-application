import SwiftUI

struct RankingHomeView: View {

    @EnvironmentObject private var router: NavigationRouter
    @StateObject private var viewModel: RankingViewModel

    init(
        initialEntryContext: TheaterRankingEntryContext? = nil,
        isUserLoggedIn: @escaping @Sendable () -> Bool = { false }
    ) {
        let repository: DramaRepositoryProtocol = DramaRepository()
        _viewModel = StateObject(
            wrappedValue: RankingViewModel(
                fetchRankingsUseCase: FetchRankingsUseCase(repository: repository),
                bookDramaUseCase: BookDramaUseCase(repository: repository),
                initialEntryContext: initialEntryContext,
                isUserLoggedIn: isUserLoggedIn
            )
        )
    }

    var body: some View {
        RankingPageScaffold(
            selectedContentType: viewModel.selectedContentType,
            selectedRankingType: viewModel.selectedRankingType,
            onBack: { router.dismiss(in: .home) },
            onSelectContentType: { type in
                Task {
                    await viewModel.selectContentType(type)
                }
            },
            onSelectRankingType: { type in
                Task {
                    await viewModel.selectRankingType(type)
                }
            },
            content: {
                RankingStateView(
                    viewState: viewModel.viewState,
                    selectedRankingType: viewModel.selectedRankingType,
                    isAppending: viewModel.isAppending,
                    appendErrorMessage: viewModel.appendErrorMessage,
                    bookingErrorMessage: viewModel.bookingErrorMessage,
                    onTapDrama: handleTapDrama(_:),
                    onTapBooking: handleTapBooking(_:),
                    onRetry: { await viewModel.retry() },
                    onLoadMore: {
                        Task {
                            await viewModel.loadMoreIfNeeded()
                        }
                    }
                )
            }
        )
        .task {
            await viewModel.loadIfNeeded()
        }
        .onReceive(viewModel.$routeEffect) { effect in
            guard let effect else { return }
            handle(routeEffect: effect)
            viewModel.clearRouteEffect()
        }
    }

    private func handleTapDrama(_ drama: RankingDrama) {
        guard let route = RankingRouteBuilder.playRoute(for: drama) else { return }
        router.navigate(to: route)
    }

    private func handleTapBooking(_ drama: RankingDrama) {
        Task {
            await viewModel.book(drama: drama)
        }
    }

    private func handle(routeEffect: RankingViewModel.RouteEffect) {
        switch routeEffect {
        case .requireLogin(let context):
            router.presentLogin(context: RankingRouteBuilder.loginContext(for: context))
        }
    }
}

struct RankingPageScaffold<Content: View>: View {
    let selectedContentType: RankingContentType
    let selectedRankingType: RankingType
    let onBack: () -> Void
    let onSelectContentType: (RankingContentType) -> Void
    let onSelectRankingType: (RankingType) -> Void
    @ViewBuilder let content: () -> Content

    var body: some View {
        ZStack(alignment: .top) {
            Color(.systemGroupedBackground)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                heroBanner
                    .padding(.bottom, -28)
                    .zIndex(1)

                tabSection
                    .zIndex(1)

                content()
                    .padding(.top, DesignTokens.Spacing.md)
            }
        }
        .toolbar(.hidden, for: .navigationBar)
    }

    private var heroBanner: some View {
        ZStack(alignment: .top) {
            RoundedRectangle(cornerRadius: 0, style: .continuous)
                .fill(headerGradient)
                .frame(height: 208)
                .overlay(alignment: .topTrailing) {
                    ZStack {
                        Circle()
                            .fill(Color.white.opacity(0.14))
                            .frame(width: 146, height: 146)
                        Image(systemName: headerSymbol)
                            .font(.system(size: 56, weight: .regular))
                            .foregroundStyle(Color.white.opacity(0.26))
                    }
                    .offset(x: 28, y: 8)
                }
                .overlay(alignment: .bottomLeading) {
                    VStack(alignment: .leading, spacing: 10) {
                        Text(headerTitle)
                            .font(.system(size: 24, weight: .bold))
                            .foregroundStyle(Color.primary)
                        Text(headerSubtitle)
                            .font(.footnote)
                            .foregroundStyle(Color.primary.opacity(0.58))
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .padding(.horizontal, DesignTokens.Spacing.lg)
                    .padding(.bottom, 34)
                }

            HStack {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(.primary)
                        .frame(width: 40, height: 40)
                }
                .buttonStyle(.plain)

                Spacer(minLength: 0)

                Image(systemName: "square.and.arrow.up")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundStyle(.primary)
                    .frame(width: 40, height: 40)
                    .background(Color.white.opacity(0.14))
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.top, DesignTokens.Spacing.md)
        }
        .ignoresSafeArea(edges: .top)
    }

    private var tabSection: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
            RankingPrimaryTabBar(selected: selectedContentType, onSelect: onSelectContentType)
            RankingSecondaryTabBar(selected: selectedRankingType, onSelect: onSelectRankingType)
        }
        .padding(.top, 22)
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.bottom, DesignTokens.Spacing.lg)
        .background {
            RoundedRectangle(cornerRadius: 32, style: .continuous)
                .fill(Color.white)
        }
        .overlay {
            RoundedRectangle(cornerRadius: 32, style: .continuous)
                .stroke(Color.black.opacity(0.04), lineWidth: 1)
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .shadow(color: Color.black.opacity(0.05), radius: 18, x: 0, y: 10)
    }

    private var headerTitle: String {
        switch selectedRankingType {
        case .hot:
            return "红果热播榜"
        case .recommend:
            return "红果推荐榜"
        case .booking:
            return "红果预约榜"
        }
    }

    private var headerSubtitle: String {
        switch selectedRankingType {
        case .hot:
            return "7月24日已更新 · 基于站内观看与互动表现综合排序"
        case .recommend:
            return "7月24日已更新 · 基于推荐指数与内容表现综合排序"
        case .booking:
            return "基于预约/播放等综合期待值排序"
        }
    }

    private var headerSymbol: String {
        switch selectedRankingType {
        case .hot:
            return "flame.fill"
        case .recommend:
            return "sparkles"
        case .booking:
            return "bell.fill"
        }
    }

    private var headerGradient: LinearGradient {
        switch selectedRankingType {
        case .hot:
            return LinearGradient(
                colors: [
                    Color(red: 1.0, green: 0.87, blue: 0.84),
                    Color(red: 0.98, green: 0.73, blue: 0.71),
                    Color(red: 0.98, green: 0.85, blue: 0.92)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .recommend:
            return LinearGradient(
                colors: [
                    Color(red: 1.0, green: 0.89, blue: 0.79),
                    Color(red: 0.99, green: 0.76, blue: 0.58),
                    Color(red: 1.0, green: 0.88, blue: 0.84)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .booking:
            return LinearGradient(
                colors: [
                    Color(red: 0.79, green: 0.96, blue: 1.0),
                    Color(red: 0.43, green: 0.90, blue: 1.0),
                    Color(red: 0.83, green: 0.92, blue: 1.0)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        }
    }
}

struct RankingScreenshotHostView: View {
    @State private var selectedContentType: RankingContentType
    @State private var selectedRankingType: RankingType

    private let scenario: RankingScreenshotScenario

    init(scenario: RankingScreenshotScenario) {
        self.scenario = scenario
        _selectedContentType = State(initialValue: .all)
        _selectedRankingType = State(initialValue: scenario.defaultRankingType)
    }

    var body: some View {
        NavigationStack {
            RankingPageScaffold(
                selectedContentType: selectedContentType,
                selectedRankingType: selectedRankingType,
                onBack: {},
                onSelectContentType: { selectedContentType = $0 },
                onSelectRankingType: { selectedRankingType = $0 },
                content: {
                    RankingStateView(
                        viewState: .content(displayItems),
                        selectedRankingType: selectedRankingType,
                        isAppending: false,
                        appendErrorMessage: nil,
                        bookingErrorMessage: nil,
                        onTapDrama: { _ in },
                        onTapBooking: { _ in },
                        onRetry: {},
                        onLoadMore: {}
                    )
                }
            )
        }
    }

    private var displayItems: [RankingDrama] {
        scenario.items(for: selectedRankingType, contentType: selectedContentType)
    }
}

enum RankingScreenshotScenario: String {
    case hot
    case booking

    var defaultRankingType: RankingType {
        switch self {
        case .hot:
            return .hot
        case .booking:
            return .booking
        }
    }

    static func fromEnvironment(_ environment: [String: String] = ProcessInfo.processInfo.environment) -> RankingScreenshotScenario? {
        guard let value = environment["RANKING_SCREENSHOT_SCENARIO"] else { return nil }
        return RankingScreenshotScenario(rawValue: value)
    }

    func items(for rankingType: RankingType, contentType: RankingContentType) -> [RankingDrama] {
        let baseItems = switch rankingType {
        case .hot:
            Self.hotItems
        case .recommend:
            Self.recommendItems
        case .booking:
            Self.bookingItems
        }

        if contentType == .all {
            return baseItems
        }

        let filtered = baseItems.filter { $0.contentType == contentType }
        return filtered.isEmpty ? baseItems : filtered
    }

    private static let hotItems: [RankingDrama] = [
        RankingDrama(
            id: "hot-1",
            title: "咱家剑宗团宠小师妹第二季",
            description: "第1季看了好几遍，终于等来了第2季，不容易啊。",
            coverUrl: "",
            category: "萌宝",
            episodeCount: 72,
            tags: ["新剧"],
            rating: 9.1,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .liveAction,
            playCount: 77_410_000,
            bookingCount: 3_884_000,
            recommendationScore: 1289.0,
            isBooked: false
        ),
        RankingDrama(
            id: "hot-2",
            title: "虎妈驾到，全家反骨仔乖乖立正",
            description: "杀猪匠林迎春因名下突然欠了50万网贷被迫回村，发现全家都在等她收拾烂摊子。",
            coverUrl: "",
            category: "家庭 · 刘清心 · 冯青青",
            episodeCount: 68,
            tags: ["新剧"],
            rating: 9.3,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .liveAction,
            playCount: 76_760_000,
            bookingCount: 1_520_000,
            recommendationScore: 343.1,
            isBooked: false
        ),
        RankingDrama(
            id: "hot-3",
            title: "万妖图录传第九季",
            description: "以妖魔之血为墨，以百妖谱为卷，可辟画其形，夺其神。",
            coverUrl: "",
            category: "玄幻",
            episodeCount: 60,
            tags: nil,
            rating: nil,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .ai,
            playCount: 72_530_000,
            bookingCount: 1_567_000,
            recommendationScore: 347.5,
            isBooked: false
        ),
        RankingDrama(
            id: "hot-4",
            title: "浙染",
            description: "因前夫出轨果断离婚的财务总监程澄，在董事长谢岚止的帮助下重启人生。",
            coverUrl: "",
            category: "爱情 · 郭宇欣 · 张翊",
            episodeCount: 64,
            tags: ["新剧"],
            rating: 9.5,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .liveAction,
            playCount: 69_880_000,
            bookingCount: 2_413_000,
            recommendationScore: 495.0,
            isBooked: false
        ),
        RankingDrama(
            id: "hot-5",
            title: "昼以继夜2",
            description: "窝趣，我居然穿成了一名观众，我要做首评。",
            coverUrl: "",
            category: "爱情 · 梁晋宜 · 张晋宜",
            episodeCount: 66,
            tags: ["新剧"],
            rating: 9.5,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .liveAction,
            playCount: 65_120_000,
            bookingCount: 2_433_000,
            recommendationScore: 518.4,
            isBooked: false
        ),
        RankingDrama(
            id: "hot-6",
            title: "都重生了，谁还装富二代啊第三季",
            description: "重生后拆穿豪门骗局，逆风翻盘。",
            coverUrl: "",
            category: "剧情",
            episodeCount: 58,
            tags: nil,
            rating: nil,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .liveAction,
            playCount: 64_050_000,
            bookingCount: 1_980_000,
            recommendationScore: 401.0,
            isBooked: false
        )
    ]

    private static let recommendItems: [RankingDrama] = [
        RankingDrama(
            id: "recommend-1",
            title: "昼以继夜3",
            description: "婚后的日子，有浪漫的心跳，也有柴米油盐的温暖烟火。",
            coverUrl: "",
            category: "群像 · 张晋宜 · 梁思伟",
            episodeCount: 72,
            tags: ["爆款"],
            rating: 9.4,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .liveAction,
            playCount: 61_240_000,
            bookingCount: 2_980_000,
            recommendationScore: 98.8,
            isBooked: false
        ),
        RankingDrama(
            id: "recommend-2",
            title: "少夫人来自东北3",
            description: "东北虎妞小并嫁入周家，本以为从此只有豪门甜宠，结果连环危机接踵而至。",
            coverUrl: "",
            category: "都市爱情 · 梁雯晶 · 业文Kevin",
            episodeCount: 65,
            tags: nil,
            rating: 9.2,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .liveAction,
            playCount: 54_300_000,
            bookingCount: 1_870_000,
            recommendationScore: 95.4,
            isBooked: false
        ),
        RankingDrama(
            id: "recommend-3",
            title: "万妖图录传第九季",
            description: "以妖魔之血为墨，以百妖谱为卷，可辟画其形，夺其神。",
            coverUrl: "",
            category: "玄幻",
            episodeCount: 60,
            tags: nil,
            rating: 8.9,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .ai,
            playCount: 50_860_000,
            bookingCount: 1_430_000,
            recommendationScore: 94.8,
            isBooked: false
        )
    ]

    private static let bookingItems: [RankingDrama] = [
        RankingDrama(
            id: "booking-1",
            title: "昼以继夜3",
            description: "婚后的日子，有浪漫的心跳，也有柴米油盐的温暖烟火。",
            coverUrl: "",
            category: "群像 · 张晋宜 · 梁思伟",
            episodeCount: 72,
            tags: ["预告"],
            rating: nil,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .liveAction,
            playCount: 19_880_000,
            bookingCount: 33_640_000,
            recommendationScore: 11.0,
            isBooked: false
        ),
        RankingDrama(
            id: "booking-2",
            title: "十八岁太奶奶驾到，重整家族荣光",
            description: "纪家迎来了最震惊的消息：纪家竟有子孙流落在外，为了重回祖宅她决定放手一搏。",
            coverUrl: "",
            category: "穿越 · 李柯以 · 屈刚",
            episodeCount: 66,
            tags: ["预告"],
            rating: nil,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .liveAction,
            playCount: 13_694_000,
            bookingCount: 19_100_000,
            recommendationScore: 12.0,
            isBooked: false
        ),
        RankingDrama(
            id: "booking-3",
            title: "少夫人来自东北3",
            description: "东北虎妞小并嫁入周家，本以为从此只有豪门甜宠，一转身却卷入家族纷争。",
            coverUrl: "",
            category: "都市爱情 · 梁雯晶 · 业文Kevin",
            episodeCount: 65,
            tags: ["预告"],
            rating: nil,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .liveAction,
            playCount: 6_274_000,
            bookingCount: 14_240_000,
            recommendationScore: 12.0,
            isBooked: false
        ),
        RankingDrama(
            id: "booking-4",
            title: "女相师2",
            description: "上古相师龙问心，曾为救苍生将一身神力散作十二碎片，今朝重聚只为破局。",
            coverUrl: "",
            category: "东方玄幻 · 孟娜 · 时康",
            episodeCount: 60,
            tags: ["预告"],
            rating: nil,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .ai,
            playCount: 10_047_000,
            bookingCount: 11_320_000,
            recommendationScore: 7.0,
            isBooked: false
        ),
        RankingDrama(
            id: "booking-5",
            title: "昼以继夜2游玩篇",
            description: "逃离都市喧嚣，沉浸式体验三对绝美CP的高糖治愈之旅。",
            coverUrl: "",
            category: "职场婚恋 · 梁思伟 · 张晋宜",
            episodeCount: 58,
            tags: ["预告"],
            rating: nil,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .liveAction,
            playCount: 1_234_000,
            bookingCount: 10_430_000,
            recommendationScore: 7.0,
            isBooked: false
        ),
        RankingDrama(
            id: "booking-6",
            title: "一品布衣5:入蜀篇",
            description: "纵横乱世再起风云，入蜀之后步步为营。",
            coverUrl: "",
            category: "历史古代 · 胡家荣 · 潘子剑",
            episodeCount: 55,
            tags: ["预告"],
            rating: nil,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .liveAction,
            playCount: 9_670_000,
            bookingCount: 9_670_000,
            recommendationScore: 8.0,
            isBooked: false
        )
    ]
}

#Preview {
    NavigationStack {
        RankingHomeView()
            .environmentObject(NavigationRouter())
    }
}
