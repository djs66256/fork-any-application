import Foundation

enum EarnHostAuthStateReason: String, Equatable, Sendable {
    case initialLoad = "initial-load"
    case loginSuccess = "login-success"
    case loginCancel = "login-cancel"
    case appResume = "app-resume"
}

enum EarnRestoreContextReason: String, Equatable, Sendable {
    case loginReturn = "login-return"
    case taskReturn = "task-return"
    case containerRecreated = "container-recreated"
}

struct EarnHostAuthState: Equatable, Sendable {
    let source: String
    let isLoggedIn: Bool
    let reason: EarnHostAuthStateReason
    let returnTarget: String
    let apiAccessToken: String?
    let expiresAt: String?
}

struct EarnRestoreContextPayload: Equatable, Sendable {
    let source: String
    let reason: EarnRestoreContextReason
    let returnTarget: String
    let preserveScroll: Bool
}

enum EarnHostMessage: Equatable, Sendable {
    case syncAuthState(EarnHostAuthState)
    case restoreContext(EarnRestoreContextPayload)
    case completeTask(EarnTaskPlayerResult)

    var script: String {
        let payload: [String: Any]
        switch self {
        case .syncAuthState(let authState):
            payload = [
                "type": "earn.syncAuthState",
                "payload": authState.jsonObject
            ]
        case .restoreContext(let context):
            payload = [
                "type": "earn.restoreContext",
                "payload": context.jsonObject
            ]
        case .completeTask(let result):
            payload = [
                "type": "earn.completeTask",
                "payload": result.jsonObject
            ]
        }

        return "window.dispatchEvent(new CustomEvent('earn.hostMessage', { detail: \(payload.jsonString) }));"
    }
}

private extension EarnHostAuthState {
    var jsonObject: [String: Any] {
        var payload: [String: Any] = [
            "source": source,
            "isLoggedIn": isLoggedIn,
            "reason": reason.rawValue,
            "returnTarget": returnTarget
        ]
        payload["apiAccessToken"] = apiAccessToken as Any
        payload["expiresAt"] = expiresAt as Any
        return payload
    }
}

private extension EarnRestoreContextPayload {
    var jsonObject: [String: Any] {
        [
            "source": source,
            "reason": reason.rawValue,
            "returnTarget": returnTarget,
            "preserveScroll": preserveScroll
        ]
    }
}

private extension EarnTaskPlayerResult {
    var jsonObject: [String: Any] {
        [
            "source": source,
            "taskId": taskId,
            "videoId": videoId,
            "completed": completed,
            "reason": reason.rawValue
        ]
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
