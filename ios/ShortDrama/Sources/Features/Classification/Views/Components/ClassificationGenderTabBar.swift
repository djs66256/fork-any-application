import SwiftUI

struct ClassificationGenderTabBar: View {
    let selectedGender: ClassificationGender
    let onSelect: (ClassificationGender) -> Void

    var body: some View {
        HStack(spacing: 4) {
            ForEach(ClassificationGender.allCases, id: \.self) { gender in
                Button {
                    onSelect(gender)
                } label: {
                    Text(displayTitle(for: gender))
                        .font(.system(size: selectedGender == gender ? 22 : 19, weight: selectedGender == gender ? .semibold : .medium))
                        .foregroundStyle(selectedGender == gender ? Color.black : Color(red: 0.64, green: 0.64, blue: 0.64))
                        .frame(minWidth: 68, alignment: .leading)
                        .padding(.vertical, 2)
                }
                .buttonStyle(.plain)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func displayTitle(for gender: ClassificationGender) -> String {
        switch gender {
        case .all:
            return "全部"
        case .male:
            return "男生"
        case .female:
            return "女生"
        }
    }
}
