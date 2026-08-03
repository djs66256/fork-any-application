import SwiftUI

struct ClassificationHomeView: View {

    @EnvironmentObject private var router: NavigationRouter
    @StateObject private var viewModel: ClassificationViewModel
    @State private var scrollTarget: ClassificationDimensionKey?

    init() {
        let repository: DramaRepositoryProtocol = DramaRepository()
        _viewModel = StateObject(
            wrappedValue: ClassificationViewModel(
                fetchClassificationTagsUseCase: FetchClassificationTagsUseCase(repository: repository)
            )
        )
    }

    var body: some View {
        ClassificationPageScaffold(
            selectedGender: viewModel.selectedGender,
            dimensions: viewModel.dimensions,
            selectedDimension: viewModel.selectedDimension,
            viewState: viewModel.viewState,
            scrollTarget: scrollTarget,
            scrollResetSeed: viewModel.scrollResetSeed,
            showsThemeExpandIndicator: viewModel.selectedGender == .all,
            onBack: { router.dismiss() },
            onSelectGender: { gender in
                Task {
                    await viewModel.selectGender(gender)
                }
            },
            onSelectDimension: { dimension in
                viewModel.selectDimension(dimension)
                scrollTarget = dimension
            },
            onTapTag: handleTapTag(_:),
            onVisibleDimensionChange: viewModel.updateVisibleDimension(_:),
            onRetry: {
                await viewModel.retry()
            }
        )
        .task {
            await viewModel.loadIfNeeded()
        }
        .onChange(of: viewModel.scrollResetSeed) { _, _ in
            scrollTarget = viewModel.dimensions.first?.key
        }
    }

    private func handleTapTag(_ tag: String) {
        guard let query = viewModel.normalizedTagQuery(tag) else { return }
        router.navigate(to: .searchResult(query: query))
    }
}

private struct ClassificationPageScaffold: View {
    let selectedGender: ClassificationGender
    let dimensions: [ClassificationDimension]
    let selectedDimension: ClassificationDimensionKey
    let viewState: ClassificationViewModel.ViewState
    let scrollTarget: ClassificationDimensionKey?
    let scrollResetSeed: Int
    let showsThemeExpandIndicator: Bool
    let onBack: () -> Void
    let onSelectGender: (ClassificationGender) -> Void
    let onSelectDimension: (ClassificationDimensionKey) -> Void
    let onTapTag: (String) -> Void
    let onVisibleDimensionChange: (ClassificationDimensionKey) -> Void
    let onRetry: () async -> Void

    private let pageBackground = Color(red: 0.95, green: 0.95, blue: 0.95)

    var body: some View {
        VStack(spacing: 0) {
            header

            HStack(alignment: .top, spacing: 10) {
                ClassificationDimensionRail(
                    dimensions: dimensions,
                    selectedDimension: selectedDimension,
                    onSelect: onSelectDimension
                )
                .frame(width: 80)

                ClassificationStateView(
                    viewState: viewState,
                    onRetry: onRetry,
                    content: {
                        ClassificationTagSectionList(
                            dimensions: dimensions,
                            scrollTarget: scrollTarget,
                            scrollResetSeed: scrollResetSeed,
                            showsThemeExpandIndicator: showsThemeExpandIndicator,
                            onTapTag: onTapTag,
                            onVisibleDimensionChange: onVisibleDimensionChange
                        )
                    }
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            }
            .padding(.horizontal, 14)
            .padding(.top, 6)
            .padding(.bottom, 12)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        }
        .background(pageBackground.ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
    }

    private var header: some View {
        HStack(spacing: 14) {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 23, weight: .medium))
                    .foregroundStyle(Color.black)
                    .frame(width: 26, height: 26)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            ClassificationGenderTabBar(
                selectedGender: selectedGender,
                onSelect: onSelectGender
            )

            Spacer(minLength: 0)
        }
        .padding(.horizontal, 14)
        .padding(.top, 6)
        .padding(.bottom, 8)
        .background(pageBackground)
    }
}

struct ClassificationScreenshotHostView: View {
    let scenario: ClassificationScreenshotScenario

    var body: some View {
        NavigationStack {
            ClassificationPageScaffold(
                selectedGender: scenario.gender,
                dimensions: scenario.dimensions,
                selectedDimension: .eraBackground,
                viewState: .content(scenario.dimensions),
                scrollTarget: nil,
                scrollResetSeed: 0,
                showsThemeExpandIndicator: scenario.showsThemeExpandIndicator,
                onBack: {},
                onSelectGender: { _ in },
                onSelectDimension: { _ in },
                onTapTag: { _ in },
                onVisibleDimensionChange: { _ in },
                onRetry: {}
            )
        }
    }
}

enum ClassificationScreenshotScenario: String {
    case all
    case male
    case female

    static func fromEnvironment(
        _ environment: [String: String] = ProcessInfo.processInfo.environment
    ) -> ClassificationScreenshotScenario? {
        guard let value = environment["CLASSIFICATION_SCREENSHOT_SCENARIO"] else { return nil }
        return ClassificationScreenshotScenario(rawValue: value)
    }

    var gender: ClassificationGender {
        switch self {
        case .all:
            return .all
        case .male:
            return .male
        case .female:
            return .female
        }
    }

    var showsThemeExpandIndicator: Bool {
        self == .all
    }

    var dimensions: [ClassificationDimension] {
        [
            ClassificationDimension(
                key: .eraBackground,
                name: ClassificationDimensionKey.eraBackground.title,
                tags: eraTags
            ),
            ClassificationDimension(
                key: .themePlot,
                name: ClassificationDimensionKey.themePlot.title,
                tags: themeTags
            ),
            ClassificationDimension(
                key: .characterSetting,
                name: ClassificationDimensionKey.characterSetting.title,
                tags: characterTags
            )
        ]
    }

    private var eraTags: [String] {
        switch self {
        case .all, .male:
            ["乡村", "职场", "民国", "校园", "历史古代", "古装"]
        case .female:
            ["职场", "民国", "校园", "古装"]
        }
    }

    private var themeTags: [String] {
        switch self {
        case .all:
            [
                "打脸虐渣", "逆袭", "马甲",
                "女性成长", "都市日常", "重生",
                "穿越", "系统", "亲情",
                "家庭伦理", "奇幻脑洞", "奇幻爱情",
                "闪婚", "暗恋成真", "古风言情",
                "穿书", "破镜重圆", "战神归来",
                "追妻", "现代言情", "豪门恩怨",
                "异能", "虐恋", "传承觉醒",
                "玄幻仙侠", "古风权谋", "年代爱情",
                "赘婿逆袭", "娱乐圈", "剧情"
            ]
        case .male:
            [
                "逆袭", "马甲", "都市日常",
                "重生", "穿越", "系统",
                "亲情", "奇幻脑洞", "穿书",
                "战神归来", "异能", "传承觉醒",
                "玄幻仙侠", "赘婿逆袭", "娱乐圈",
                "剧情", "无敌神医", "悬疑推理",
                "喜剧"
            ]
        case .female:
            [
                "打脸虐渣", "逆袭", "马甲",
                "女性成长", "重生", "穿越",
                "系统", "亲情", "家庭伦理",
                "奇幻爱情", "闪婚", "暗恋成真",
                "古风言情", "穿书", "破镜重圆",
                "追妻", "现代言情", "豪门恩怨",
                "虐恋", "古风权谋", "年代爱情",
                "娱乐圈", "剧情", "悬疑推理",
                "喜剧", "现言甜宠"
            ]
        }
    }

    private var characterTags: [String] {
        switch self {
        case .all:
            [
                "大女主", "萌宝", "小人物",
                "神豪", "强者回归", "真假千金",
                "欢喜冤家", "强强联合", "天下无敌",
                "青梅竹马", "王妃", "女帝",
                "龙王", "皇后", "替身"
            ]
        case .male:
            [
                "小人物", "神豪", "强者回归",
                "天下无敌", "女帝", "龙王"
            ]
        case .female:
            [
                "大女主", "萌宝", "小人物",
                "真假千金", "欢喜冤家", "强强联合",
                "青梅竹马", "王妃", "女帝",
                "皇后", "替身", "大叔",
                "团宠"
            ]
        }
    }
}

#Preview {
    NavigationStack {
        ClassificationHomeView()
            .environmentObject(NavigationRouter())
    }
}

#Preview("Classification Screenshot - All") {
    ClassificationScreenshotHostView(scenario: .all)
}
