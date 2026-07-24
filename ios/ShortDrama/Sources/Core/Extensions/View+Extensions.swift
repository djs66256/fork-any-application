import SwiftUI

extension View {

    /// Applies a semi-transparent random background color for layout debugging.
    @ViewBuilder
    func debugBackground() -> some View {
        #if DEBUG
        self.background(Color(
            red: .random(in: 0.5...1),
            green: .random(in: 0.5...1),
            blue: .random(in: 0.5...1)
        ).opacity(0.3))
        #else
        self
        #endif
    }
}
