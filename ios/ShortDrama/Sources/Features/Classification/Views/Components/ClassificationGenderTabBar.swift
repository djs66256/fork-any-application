import SwiftUI

struct ClassificationGenderTabBar: View {
    let selectedGender: ClassificationGender
    let onSelect: (ClassificationGender) -> Void

    var body: some View {
        HStack(spacing: 30) {
            ForEach(ClassificationGender.allCases, id: \.self) { gender in
                Button {
                    onSelect(gender)
                } label: {
                    Text(displayTitle(for: gender))
                        .font(.system(size: selectedGender == gender ? 19 : 18, weight: selectedGender == gender ? .semibold : .medium))
                        .foregroundStyle(selectedGender == gender ? Color.black : Color(red: 0.58, green: 0.58, blue: 0.58))
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
