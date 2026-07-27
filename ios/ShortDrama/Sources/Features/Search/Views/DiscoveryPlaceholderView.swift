import SwiftUI

/// Placeholder discovery page for ranking, classification, new releases, and actors.
struct DiscoveryPlaceholderView: View {

    enum Kind: String, Equatable {
        case ranking
        case classification
        case newReleases
        case actorHub

        var title: String {
            switch self {
            case .ranking:
                return "排行"
            case .classification:
                return "分类"
            case .newReleases:
                return "新剧"
            case .actorHub:
                return "演员"
            }
        }

        var description: String {
            switch self {
            case .ranking:
                return "排行承接页建设中，后续将与排行能力对齐。"
            case .classification:
                return "分类入口已升级为真实页面，此占位页不再用于线上承接。"
            case .newReleases:
                return "新剧承接页建设中，首版保持 Native 占位。"
            case .actorHub:
                return "演员承接页建设中，首版保持 Native 占位。"
            }
        }

        var systemImage: String {
            switch self {
            case .ranking:
                return "chart.bar.fill"
            case .classification:
                return "square.grid.2x2.fill"
            case .newReleases:
                return "sparkles.tv"
            case .actorHub:
                return "person.2.fill"
            }
        }
    }

    let kind: Kind

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.lg) {
            Image(systemName: kind.systemImage)
                .font(.system(size: 56))
                .foregroundStyle(.secondary)
            Text(kind.title)
                .font(.title2.bold())
            Text(kind.description)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(DesignTokens.Spacing.xl)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .navigationTitle(kind.title)
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview {
    DiscoveryPlaceholderView(kind: .ranking)
}
