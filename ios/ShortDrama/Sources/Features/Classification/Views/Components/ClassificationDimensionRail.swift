import SwiftUI

struct ClassificationDimensionRail: View {
    let dimensions: [ClassificationDimension]
    let selectedDimension: ClassificationDimensionKey
    let onSelect: (ClassificationDimensionKey) -> Void

    private let accentColor = Color(red: 0.96, green: 0.52, blue: 0.17)
    private let baseTextColor = Color.black.opacity(0.96)

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                ForEach(dimensions) { dimension in
                    let isSelected = selectedDimension == dimension.key
                    Button {
                        onSelect(dimension.key)
                    } label: {
                        HStack(alignment: .center, spacing: 0) {
                            Rectangle()
                                .fill(isSelected ? accentColor : .clear)
                                .frame(width: 3, height: 20)
                                .padding(.trailing, 10)

                            Text(dimension.name)
                                .font(.system(size: 15, weight: isSelected ? .medium : .regular))
                                .foregroundStyle(isSelected ? accentColor : baseTextColor)
                                .multilineTextAlignment(.leading)
                                .lineSpacing(0)
                                .fixedSize(horizontal: false, vertical: true)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .frame(maxWidth: .infinity, minHeight: 78, alignment: .leading)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.top, 6)
        }
        .scrollIndicators(.hidden)
    }
}
