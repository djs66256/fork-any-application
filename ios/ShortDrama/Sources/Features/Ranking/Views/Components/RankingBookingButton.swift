import SwiftUI

struct RankingBookingButton: View {
    let booked: Bool
    let isSubmitting: Bool
    let action: () -> Void

    private var title: String {
        if booked {
            return "已预约"
        }
        if isSubmitting {
            return "预约中"
        }
        return "预约"
    }

    var body: some View {
        Button(title) {
            action()
        }
        .font(.system(size: 16, weight: .semibold))
        .foregroundStyle(booked ? bookedTextColor : bookingTint)
        .frame(minWidth: 56)
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background {
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(buttonBackground)
        }
        .opacity(isSubmitting ? 0.75 : 1)
        .disabled(booked || isSubmitting)
    }

    private var bookingTint: Color {
        Color(red: 0.98, green: 0.45, blue: 0.16)
    }

    private var bookedTextColor: Color {
        Color(red: 0.63, green: 0.63, blue: 0.66)
    }

    private var buttonBackground: Color {
        Color(red: 0.97, green: 0.97, blue: 0.97)
    }
}
