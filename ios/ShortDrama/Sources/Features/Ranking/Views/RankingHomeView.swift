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
            showsReferenceDecorations: false,
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
    let showsReferenceDecorations: Bool
    let onBack: () -> Void
    let onSelectContentType: (RankingContentType) -> Void
    let onSelectRankingType: (RankingType) -> Void
    @ViewBuilder let content: () -> Content

    var body: some View {
        ZStack(alignment: .top) {
            Color(red: 0.97, green: 0.97, blue: 0.97)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                heroBanner
                    .zIndex(1)

                tabSection
                    .padding(.top, -22)
                    .zIndex(2)

                content()
                    .padding(.top, 8)
            }
        }
        .toolbar(.hidden, for: .navigationBar)
    }

    private var heroBanner: some View {
        ZStack(alignment: .top) {
            headerBackground
                .frame(height: 236)
                .clipShape(Rectangle())

            VStack(spacing: 0) {
                HStack {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 22, weight: .semibold))
                            .foregroundStyle(Color.black)
                            .frame(width: 40, height: 40)
                    }
                    .buttonStyle(.plain)

                    Spacer(minLength: 0)

                    Button(action: {}) {
                        Image(systemName: "arrow.up.right.square")
                            .font(.system(size: 21, weight: .medium))
                            .foregroundStyle(Color.black)
                            .frame(width: 40, height: 40)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 18)
                .padding(.top, 6)

                Spacer()

                VStack(alignment: .leading, spacing: 12) {
                    Text(headerTitle)
                        .font(.system(size: 28, weight: .black))
                        .foregroundStyle(Color.black)

                    Text(headerSubtitle)
                        .font(.system(size: 13))
                        .foregroundStyle(Color.black.opacity(0.34))
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 30)
                .padding(.bottom, 54)
            }
        }
        .frame(height: 236)
        .clipShape(Rectangle())
        .ignoresSafeArea(edges: .top)
    }

    private var tabSection: some View {
        VStack(alignment: .leading, spacing: 20) {
            RankingPrimaryTabBar(
                selected: selectedContentType,
                showsReferenceDecorations: showsReferenceDecorations,
                onSelect: onSelectContentType
            )
            RankingSecondaryTabBar(
                selected: selectedRankingType,
                showsReferenceDecorations: showsReferenceDecorations,
                onSelect: onSelectRankingType
            )
        }
        .padding(.top, 24)
        .padding(.bottom, 18)
        .background {
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .fill(Color.white)
        }
        .padding(.horizontal, 18)
    }

    private var headerTitle: String {
        switch selectedRankingType {
        case .hot:
            return "◉红果热播榜◉"
        case .recommend:
            return "◉红果推荐榜◉"
        case .booking:
            return "◉红果预约榜◉"
        }
    }

    private var headerSubtitle: String {
        switch selectedRankingType {
        case .hot:
            return "7月24日已更新·基于红果内观看/互动等综合热度排序"
        case .recommend:
            return "7月24日已更新·基于红果观看/互动以及个人兴趣排序"
        case .booking:
            return "基于红果预约/播放等综合期待值排序"
        }
    }

    @ViewBuilder
    private var headerBackground: some View {
        switch selectedRankingType {
        case .hot:
            RankingHeaderHotBackground()
        case .recommend:
            RankingHeaderRecommendBackground()
        case .booking:
            RankingHeaderBookingBackground()
        }
    }
}

private struct RankingHeaderHotBackground: View {
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.99, green: 0.83, blue: 0.80),
                    Color(red: 0.99, green: 0.78, blue: 0.80),
                    Color(red: 0.95, green: 0.91, blue: 1.0)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )

            GeometryReader { proxy in
                Image("RankingBannerHotArt")
                    .resizable()
                    .scaledToFill()
                    .frame(width: proxy.size.width * 0.66, height: 206)
                    .clipped()
                    .offset(x: proxy.size.width * 0.38, y: 10)
                    .opacity(0.97)
            }

            Ellipse()
                .fill(Color.white.opacity(0.28))
                .frame(width: 230, height: 66)
                .rotationEffect(.degrees(-13))
                .offset(x: 122, y: -2)
                .blur(radius: 16)
        }
    }
}

private struct RankingHeaderRecommendBackground: View {
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.72, green: 0.96, blue: 0.89),
                    Color(red: 0.47, green: 0.89, blue: 0.81),
                    Color(red: 0.56, green: 0.78, blue: 0.67)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )

            Circle()
                .stroke(Color.white.opacity(0.34), lineWidth: 2)
                .frame(width: 156, height: 156)
                .offset(x: 74, y: 4)

            Circle()
                .stroke(Color.white.opacity(0.26), lineWidth: 1.6)
                .frame(width: 208, height: 208)
                .offset(x: 120, y: -24)

            Path { path in
                path.move(to: CGPoint(x: 180, y: 58))
                path.addQuadCurve(to: CGPoint(x: 298, y: 12), control: CGPoint(x: 248, y: 18))
                path.addQuadCurve(to: CGPoint(x: 346, y: 92), control: CGPoint(x: 336, y: 24))
            }
            .stroke(
                LinearGradient(
                    colors: [
                        Color.white.opacity(0.14),
                        Color(red: 1.0, green: 0.90, blue: 0.44).opacity(0.95),
                        Color(red: 1.0, green: 0.67, blue: 0.48).opacity(0.92)
                    ],
                    startPoint: .leading,
                    endPoint: .trailing
                ),
                style: StrokeStyle(lineWidth: 20, lineCap: .round, lineJoin: .round)
            )
            .blur(radius: 0.5)

            Path { path in
                path.move(to: CGPoint(x: 204, y: 72))
                path.addQuadCurve(to: CGPoint(x: 296, y: 36), control: CGPoint(x: 256, y: 40))
                path.addQuadCurve(to: CGPoint(x: 330, y: 98), control: CGPoint(x: 328, y: 48))
            }
            .stroke(Color.white.opacity(0.38), style: StrokeStyle(lineWidth: 6, lineCap: .round))
        }
    }
}

private struct RankingHeaderBookingBackground: View {
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.53, green: 0.93, blue: 1.0),
                    Color(red: 0.31, green: 0.80, blue: 1.0),
                    Color(red: 0.41, green: 0.70, blue: 0.99)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )

            GeometryReader { proxy in
                Image("RankingBannerBookingArt")
                    .resizable()
                    .scaledToFill()
                    .frame(width: proxy.size.width * 0.68, height: 214)
                    .clipped()
                    .offset(x: proxy.size.width * 0.34, y: 12)
                    .opacity(0.98)
            }

            Ellipse()
                .fill(Color.white.opacity(0.18))
                .frame(width: 180, height: 34)
                .offset(x: 96, y: -26)
                .blur(radius: 8)
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
                showsReferenceDecorations: true,
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

    static func fromEnvironment(
        _ environment: [String: String] = ProcessInfo.processInfo.environment,
        arguments: [String] = ProcessInfo.processInfo.arguments
    ) -> RankingScreenshotScenario? {
        if let value = environment["RANKING_SCREENSHOT_SCENARIO"],
           let scenario = RankingScreenshotScenario(rawValue: value) {
            return scenario
        }

        guard let argumentIndex = arguments.firstIndex(of: "--ranking-screenshot-scenario"),
              arguments.indices.contains(argumentIndex + 1) else {
            return nil
        }

        return RankingScreenshotScenario(rawValue: arguments[argumentIndex + 1])
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
            description: "第1季看了好几遍，终于等来了第2季，不容易啊😭😭……",
            coverUrl: "",
            category: "萌宝",
            episodeCount: 72,
            tags: ["爆款"],
            rating: nil,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .liveAction,
            playCount: 77_410_000,
            bookingCount: 3_878_000,
            recommendationScore: 1285.3,
            isBooked: false
        ),
        RankingDrama(
            id: "hot-2",
            title: "虎妈驾到，全家反骨仔乖乖立正",
            description: "杀猪匠林迎春因名下突然欠了50万网贷被迫回村，发现是…",
            coverUrl: "",
            category: "家庭 · 刘清心 · 冯青青",
            episodeCount: 68,
            tags: ["新剧"],
            rating: 9.3,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .liveAction,
            playCount: 76_760_000,
            bookingCount: 1_517_000,
            recommendationScore: 342.3,
            isBooked: false
        ),
        RankingDrama(
            id: "hot-3",
            title: "万妖图录传第九季",
            description: "以妖魔之血为墨，以百妖谱为卷，可辟画其形，夺其神…",
            coverUrl: "",
            category: "玄幻",
            episodeCount: 60,
            tags: nil,
            rating: nil,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .ai,
            playCount: 72_530_000,
            bookingCount: 1_566_000,
            recommendationScore: 347.3,
            isBooked: false
        ),
        RankingDrama(
            id: "hot-4",
            title: "浙染",
            description: "因前夫出轨果断离婚的财务总监程澄，在董事长谢岚止的帮助…",
            coverUrl: "",
            category: "爱情 · 郭宇欣 · 张翊",
            episodeCount: 64,
            tags: ["新剧"],
            rating: 9.5,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .liveAction,
            playCount: 69_880_000,
            bookingCount: 2_407_000,
            recommendationScore: 493.9,
            isBooked: false
        ),
        RankingDrama(
            id: "hot-5",
            title: "昼以继夜2",
            description: "窝趣！我居然穿成了一名观众，我要做首评！看这个…",
            coverUrl: "",
            category: "爱情 · 梁思伟 · 张晋宜",
            episodeCount: 66,
            tags: ["新剧"],
            rating: 9.5,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .liveAction,
            playCount: 65_120_000,
            bookingCount: 2_431_000,
            recommendationScore: 518.0,
            isBooked: false
        ),
        RankingDrama(
            id: "hot-6",
            title: "都重生了，谁还装富二代啊第三季",
            description: "剧情高能反转，重生后拆穿豪门骗局逆风翻盘。",
            coverUrl: "",
            category: "剧情",
            episodeCount: 58,
            tags: nil,
            rating: nil,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .liveAction,
            playCount: 64_050_000,
            bookingCount: 1_952_000,
            recommendationScore: 401.4,
            isBooked: false
        )
    ]

    private static let recommendItems: [RankingDrama] = [
        RankingDrama(
            id: "recommend-1",
            title: "全族托举农门状元郎",
            description: "好剧，全族都是好的没得勾心斗角，主角懂事努力无…",
            coverUrl: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=600&q=80",
            category: "剧情",
            episodeCount: 72,
            tags: nil,
            rating: nil,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .liveAction,
            playCount: 50_090_000,
            bookingCount: 1_850_000,
            recommendationScore: 996.0,
            isBooked: false
        ),
        RankingDrama(
            id: "recommend-2",
            title: "母亲是巨龙我咋是山海异兽呢？",
            description: "李烨意外穿越艾尔瑞亚大陆，重生成一只血脉稀薄，被…",
            coverUrl: "https://images.unsplash.com/photo-1546961329-78bef0414d7c?auto=format&fit=crop&w=600&q=80",
            category: "奇幻",
            episodeCount: 65,
            tags: nil,
            rating: nil,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .ai,
            playCount: 52_160_000,
            bookingCount: 1_920_000,
            recommendationScore: 989.0,
            isBooked: false
        ),
        RankingDrama(
            id: "recommend-3",
            title: "觉醒十殿阎罗吾即是红伞鬼仙",
            description: "异能者江阎觉醒双生神赐，手握SSS级十方鬼令统帅…",
            coverUrl: "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=600&q=80",
            category: "脑洞",
            episodeCount: 60,
            tags: nil,
            rating: nil,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .ai,
            playCount: 51_100_000,
            bookingCount: 1_430_000,
            recommendationScore: 975.0,
            isBooked: false
        ),
        RankingDrama(
            id: "recommend-4",
            title: "赠物得长生，老头修仙记！",
            description: "笑死了😂很好看的，剧情不错，推荐，我先收藏一波",
            coverUrl: "https://images.unsplash.com/photo-1519345182560-3f2917c472ef?auto=format&fit=crop&w=600&q=80",
            category: "玄幻",
            episodeCount: 58,
            tags: nil,
            rating: nil,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .ai,
            playCount: 47_410_000,
            bookingCount: 1_210_000,
            recommendationScore: 967.0,
            isBooked: false
        ),
        RankingDrama(
            id: "recommend-5",
            title: "香蜜沉沉烬如霜（上）",
            description: "穗禾，这次你放胆去做，因为这次你的身后人山人海…",
            coverUrl: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=600&q=80",
            category: "爱情 · 王云云 · 张庭睿",
            episodeCount: 55,
            tags: nil,
            rating: nil,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .liveAction,
            playCount: 37_340_000,
            bookingCount: 1_080_000,
            recommendationScore: 960.0,
            isBooked: false
        ),
        RankingDrama(
            id: "recommend-6",
            title: "老祖归来，全族成帝",
            description: "上古血脉觉醒，举族再踏长生之路。",
            coverUrl: "https://images.unsplash.com/photo-1521119989659-a83eee488004?auto=format&fit=crop&w=600&q=80",
            category: "玄幻",
            episodeCount: 48,
            tags: nil,
            rating: nil,
            createdAt: "2026-07-24T00:00:00Z",
            updatedAt: "2026-07-24T00:00:00Z",
            contentType: .ai,
            playCount: 35_210_000,
            bookingCount: 980_000,
            recommendationScore: 950.0,
            isBooked: false
        )
    ]

    private static let bookingItems: [RankingDrama] = [
        RankingDrama(
            id: "booking-1",
            title: "昼以继夜3",
            description: "婚后的日子，有浪漫的心跳，也有柴米油盐的温暖烟火…",
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
            description: "纪家迎来了最震惊的消息：纪家竟有子孙流落在外！为…",
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
            description: "东北虎妞小并嫁入周家，本以为从此只有豪门甜宠，一…",
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
            description: "上古相师龙问心，曾为救苍生将一身神力散作十二碎片…",
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
            description: "逃离都市喧嚣，沉浸式体验三对绝美CP的高糖治愈之旅…",
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
