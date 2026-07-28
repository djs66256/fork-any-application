import Foundation

/// Formats theater heat values into short Chinese strings.
enum TheaterHeatFormatter {
    static func string(from value: Int) -> String {
        guard value >= 10_000 else {
            return String(value)
        }

        if value >= 100_000_000 {
            return shortString(value: Double(value), unit: 100_000_000, suffix: "亿")
        }

        return shortString(value: Double(value), unit: 10_000, suffix: "万")
    }

    private static func shortString(value: Double, unit: Double, suffix: String) -> String {
        let scaled = value / unit
        let rounded = (scaled * 10).rounded() / 10
        let text: String

        if rounded.truncatingRemainder(dividingBy: 1) == 0 {
            text = String(Int(rounded))
        } else {
            text = String(format: "%.1f", rounded)
        }

        return text + suffix
    }
}
