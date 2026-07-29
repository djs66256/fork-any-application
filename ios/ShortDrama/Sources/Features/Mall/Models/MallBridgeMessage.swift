import Foundation

enum MallRestoreContextReason: String, Equatable, Sendable {
    case searchReturn = "search-return"
    case loginReturn = "login-return"
    case containerRecreated = "container-recreated"
}

struct MallSearchContext: Equatable, Sendable {
    let source: String
    let returnTarget: String
}

struct MallHostAuthState: Equatable, Sendable {
    let source: String
    let isLoggedIn: Bool
    let reason: String
    let returnTarget: String
}

struct MallRestoreContextPayload: Equatable, Sendable {
    let source: String
    let reason: MallRestoreContextReason
    let returnTarget: String
    let preserveScroll: Bool
}

enum MallBridgeMessage: Equatable, Sendable {
    case openSearch(MallSearchContext)
    case requestLogin(MallLoginContext)

    init?(body: Any) {
        guard let dictionary = body as? [String: Any],
              let type = dictionary["type"] as? String,
              let payload = dictionary["payload"] as? [String: Any] else {
            return nil
        }

        switch type {
        case "mall.openSearch":
            guard let source = payload["source"] as? String,
                  source == "mall",
                  let returnTarget = payload["returnTarget"] as? String,
                  returnTarget == "/mall" else {
                return nil
            }
            self = .openSearch(MallSearchContext(source: source, returnTarget: returnTarget))
        case "mall.requestLogin":
            guard let source = payload["source"] as? String,
                  source == "mall",
                  let productID = payload["productId"] as? String,
                  !productID.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                  let returnTarget = payload["returnTarget"] as? String,
                  returnTarget == "/mall" else {
                return nil
            }
            self = .requestLogin(
                MallLoginContext(
                    source: source,
                    productID: productID,
                    returnTarget: returnTarget
                )
            )
        default:
            return nil
        }
    }
}

enum MallHostMessage: Equatable, Sendable {
    case syncAuthState(MallHostAuthState)
    case restoreContext(MallRestoreContextPayload)

    var script: String {
        switch self {
        case .syncAuthState(let payload):
            return "window.dispatchEvent(new CustomEvent('mall.syncAuthState', { detail: \(payload.jsonString) }));"
        case .restoreContext(let payload):
            return "window.dispatchEvent(new CustomEvent('mall.restoreContext', { detail: \(payload.jsonString) }));"
        }
    }
}

private extension MallHostAuthState {
    var jsonString: String {
        let payload: [String: Any] = [
            "source": source,
            "isLoggedIn": isLoggedIn,
            "reason": reason,
            "returnTarget": returnTarget
        ]
        return payload.jsonString
    }
}

private extension MallRestoreContextPayload {
    var jsonString: String {
        let payload: [String: Any] = [
            "source": source,
            "reason": reason.rawValue,
            "returnTarget": returnTarget,
            "preserveScroll": preserveScroll
        ]
        return payload.jsonString
    }
}

private extension Dictionary where Key == String, Value == Any {
    var jsonString: String {
        guard JSONSerialization.isValidJSONObject(self),
              let data = try? JSONSerialization.data(withJSONObject: self),
              let string = String(data: data, encoding: .utf8) else {
            return "{}"
        }
        return string
    }
}
