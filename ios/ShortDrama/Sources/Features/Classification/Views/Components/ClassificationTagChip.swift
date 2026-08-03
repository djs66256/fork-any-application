import SwiftUI

struct ClassificationTagChip: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 14, weight: .regular))
                .foregroundStyle(Color.black)
                .lineLimit(1)
                .minimumScaleFactor(0.76)
                .allowsTightening(true)
                .truncationMode(.tail)
                .frame(maxWidth: .infinity, minHeight: 54)
                .padding(.horizontal, 6)
                .background(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(Color(red: 0.96, green: 0.96, blue: 0.96))
                )
        }
        .buttonStyle(.plain)
    }
}
