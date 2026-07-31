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
        .font(.system(size: 17, weight: .medium))
        .foregroundStyle(booked ? bookedTextColor : bookingTint)
        .frame(minWidth: 74)
        .padding(.horizontal, 14)
        .padding(.vertical, 6)
        .background {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Color(red: 0.98, green: 0.96, blue: 0.94))
        }
        .opacity(isSubmitting ? 0.75 : 1)
        .disabled(booked || isSubmitting)
    }

    private var bookingTint: Color {
        Color(red: 1.0, green: 0.49, blue: 0.18)
    }

    private var bookedTextColor: Color {
        Color(red: 0.54, green: 0.54, blue: 0.58)
    }
}
