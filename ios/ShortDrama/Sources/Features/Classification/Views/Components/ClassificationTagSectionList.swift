import SwiftUI

struct ClassificationTagSectionList: View {
    let dimensions: [ClassificationDimension]
    let scrollTarget: ClassificationDimensionKey?
    let scrollResetSeed: Int
    let showsThemeExpandIndicator: Bool
    let onTapTag: (String) -> Void
    let onVisibleDimensionChange: (ClassificationDimensionKey) -> Void

    private let columns = Array(repeating: GridItem(.flexible(minimum: 0, maximum: .infinity), spacing: 10), count: 3)

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 34) {
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
                .padding(.horizontal, 14)
                .padding(.top, 16)
                .padding(.bottom, 24)
            }
            .background(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .fill(Color(red: 0.968, green: 0.968, blue: 0.968))
            )
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
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
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(dimension.name)
                    .font(.system(size: 17, weight: .medium))
                    .foregroundStyle(Color(red: 0.72, green: 0.72, blue: 0.72))

                Spacer(minLength: 0)

                if dimension.key == .themePlot, showsThemeExpandIndicator {
                    HStack(spacing: 1) {
                        Text("展开")
                        Image(systemName: "chevron.down")
                            .font(.system(size: 9, weight: .medium))
                    }
                    .font(.system(size: 13, weight: .regular))
                    .foregroundStyle(Color(red: 0.70, green: 0.70, blue: 0.70))
                }
            }

            if dimension.tags.isEmpty {
                Text("当前维度暂无标签")
                    .font(.system(size: 14))
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 16)
                    .background(
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .fill(Color(red: 0.96, green: 0.96, blue: 0.96))
                    )
            } else {
                LazyVGrid(columns: columns, alignment: .leading, spacing: 10) {
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
