import Foundation
import SwiftUI

/// Design tokens for consistent spacing, icon sizes and corner radii.
enum DesignTokens {

    /// Spacing constants.
    enum Spacing {
        static let xs: CGFloat = 4
        static let sm: CGFloat = 8
        static let md: CGFloat = 12
        static let lg: CGFloat = 16
        static let xl: CGFloat = 24
        static let xxl: CGFloat = 32
    }

    /// Icon size constants.
    enum IconSize {
        static let sm: CGFloat = 16
        static let md: CGFloat = 24
        static let lg: CGFloat = 32
        static let xl: CGFloat = 48
        static let xxl: CGFloat = 64
    }

    /// Corner radius constants.
    enum CornerRadius {
        static let sm: CGFloat = 4
        static let md: CGFloat = 8
        static let lg: CGFloat = 12
        static let xl: CGFloat = 20
        static let xxl: CGFloat = 32
    }

    /// Home immersive chrome colors.
    enum HomeChrome {
        static let background = Color.black
        static let topBarBackground = Color.clear
        static let topBarBorder = Color.clear
        static let iconButtonBackground = Color.black.opacity(0.42)
        static let iconButtonBorder = Color.white.opacity(0.08)
        static let tabBarBackground = Color(red: 0.07, green: 0.07, blue: 0.08)
        static let tabBarSelected = Color(red: 0.98, green: 0.95, blue: 0.91)
        static let tabBarUnselected = Color.white.opacity(0.5)
        static let tabBarIndicator = Color.clear
        static let tabBarHairline = Color.white.opacity(0.06)
        static let frameCtaBackground = Color(red: 0.08, green: 0.08, blue: 0.09)
        static let frameCtaBorder = Color.white.opacity(0.1)
        static let accent = Color(red: 0.98, green: 0.41, blue: 0.14)
        static let accentSoft = Color(red: 1.0, green: 0.53, blue: 0.19)
        static let mutedText = Color.white.opacity(0.72)
    }
}
