import Foundation

enum BookingAssetsRouteBuilder {
    static func loginContext() -> LoginInterceptionContext {
        LoginInterceptionContext(
            source: .bookingAssets,
            returnRoute: .bookingAssets
        )
    }
}
