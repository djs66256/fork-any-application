import Foundation

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
}
