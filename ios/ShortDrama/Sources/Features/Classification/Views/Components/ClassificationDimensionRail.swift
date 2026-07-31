import SwiftUI

struct ClassificationDimensionRail: View {
    let dimensions: [ClassificationDimension]
    let selectedDimension: ClassificationDimensionKey
    let onSelect: (ClassificationDimensionKey) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                ForEach(dimensions) { dimension in
                    Button {
                        onSelect(dimension.key)
                    } label: {
                        HStack(spacing: 0) {
                            Rectangle()
                                .fill(selectedDimension == dimension.key ? railAccent : .clear)
                                .frame(width: 3, height: 22)
                                .padding(.trailing, 10)

                            Text(dimension.name)
                                .font(.system(size: 15, weight: selectedDimension == dimension.key ? .semibold : .medium))
                                .foregroundStyle(selectedDimension == dimension.key ? railAccent : Color.black)
                                .multilineTextAlignment(.leading)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .frame(maxWidth: .infinity, minHeight: 64, alignment: .leading)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.top, 8)
        }
        .scrollIndicators(.hidden)
    }

    private var railAccent: Color {
        Color(red: 0.96, green: 0.46, blue: 0.11)
    }
}
