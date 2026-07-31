import SwiftUI

struct ClassificationTagSectionList: View {
    let dimensions: [ClassificationDimension]
    let scrollTarget: ClassificationDimensionKey?
    let scrollResetSeed: Int
    let onTapTag: (String) -> Void
    let onVisibleDimensionChange: (ClassificationDimensionKey) -> Void

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 12), count: 3)

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 26) {
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
                .padding(.horizontal, 16)
                .padding(.vertical, 18)
            }
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(Color.white)
            )
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
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
            HStack(alignment: .firstTextBaseline) {
                Text(dimension.name)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(Color(red: 0.66, green: 0.66, blue: 0.66))

                Spacer(minLength: 0)

                if dimension.key == .themePlot, dimension.tags.count > 12 {
                    Label("展开", systemImage: "chevron.down")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(Color(red: 0.66, green: 0.66, blue: 0.66))
                        .labelStyle(.titleAndIcon)
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
                LazyVGrid(columns: columns, alignment: .leading, spacing: 12) {
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
