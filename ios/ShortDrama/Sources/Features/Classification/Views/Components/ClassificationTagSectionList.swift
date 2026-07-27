import SwiftUI

struct ClassificationTagSectionList: View {
    let dimensions: [ClassificationDimension]
    let scrollTarget: ClassificationDimensionKey?
    let scrollResetSeed: Int
    let onTapTag: (String) -> Void
    let onVisibleDimensionChange: (ClassificationDimensionKey) -> Void

    private let columns = Array(repeating: GridItem(.flexible(), spacing: DesignTokens.Spacing.sm), count: 3)

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(alignment: .leading, spacing: DesignTokens.Spacing.xl) {
                    ForEach(dimensions) { dimension in
                        sectionView(for: dimension)
                            .id(dimension.key)
                            .background(
                                GeometryReader { geometry in
                                    Color.clear
                                        .preference(
                                            key: ClassificationSectionOffsetPreferenceKey.self,
                                            value: [
                                                ClassificationSectionOffset(
                                                    key: dimension.key,
                                                    minY: geometry.frame(in: .named("classificationTagScroll")).minY
                                                )
                                            ]
                                        )
                                }
                            )
                    }
                }
                .padding(.bottom, DesignTokens.Spacing.xl)
            }
            .coordinateSpace(name: "classificationTagScroll")
            .scrollIndicators(.hidden)
            .onChange(of: scrollTarget) { _, newValue in
                guard let newValue else { return }
                withAnimation(.easeInOut(duration: 0.2)) {
                    proxy.scrollTo(newValue, anchor: .top)
                }
            }
            .onChange(of: scrollResetSeed) { _, _ in
                guard let firstKey = dimensions.first?.key else { return }
                withAnimation(.easeInOut(duration: 0.2)) {
                    proxy.scrollTo(firstKey, anchor: .top)
                }
            }
            .onPreferenceChange(ClassificationSectionOffsetPreferenceKey.self) { offsets in
                guard let current = offsets
                    .sorted(by: { lhs, rhs in
                        abs(lhs.minY) < abs(rhs.minY)
                    })
                    .first?
                    .key else {
                    return
                }

                onVisibleDimensionChange(current)
            }
        }
    }

    @ViewBuilder
    private func sectionView(for dimension: ClassificationDimension) -> some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
            Text(dimension.name)
                .font(.headline)

            if dimension.tags.isEmpty {
                Text("当前维度暂无标签")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(DesignTokens.Spacing.md)
                    .background(
                        RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg)
                            .fill(Color(.secondarySystemBackground))
                    )
            } else {
                LazyVGrid(columns: columns, alignment: .leading, spacing: DesignTokens.Spacing.sm) {
                    ForEach(dimension.tags, id: \.self) { tag in
                        ClassificationTagChip(title: tag) {
                            onTapTag(tag)
                        }
                    }
                }
            }
        }
    }
}

private struct ClassificationSectionOffset: Equatable {
    let key: ClassificationDimensionKey
    let minY: CGFloat
}

private struct ClassificationSectionOffsetPreferenceKey: PreferenceKey {
    static let defaultValue: [ClassificationSectionOffset] = []

    static func reduce(value: inout [ClassificationSectionOffset], nextValue: () -> [ClassificationSectionOffset]) {
        value.append(contentsOf: nextValue())
    }
}
