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
            return "预约中…"
        }
        return "预约"
    }

    var body: some View {
        Button(title) {
            action()
        }
        .buttonStyle(.borderedProminent)
        .tint(booked ? .green : .accentColor)
        .disabled(booked || isSubmitting)
    }
}
