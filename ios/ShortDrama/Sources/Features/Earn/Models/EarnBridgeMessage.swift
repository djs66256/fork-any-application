import Foundation

enum EarnBridgeMessage: Equatable, Sendable {
    case requestLogin(EarnLoginContext)
    case openTaskPlayer(EarnTaskContext)

    init?(body: Any) {
        guard let dictionary = body as? [String: Any],
              let type = dictionary["type"] as? String,
              let payload = dictionary["payload"] as? [String: Any] else {
            return nil
        }

        switch type {
        case "earn.requestLogin":
            guard let source = payload["source"] as? String,
                  let returnTarget = payload["returnTarget"] as? String,
                  let context = EarnLoginContext(source: source, returnTarget: returnTarget) else {
                return nil
            }
            self = .requestLogin(context)
        case "earn.openTaskPlayer":
            guard let taskId = payload["taskId"] as? String,
                  let source = payload["source"] as? String,
                  let returnTarget = payload["returnTarget"] as? String,
                  let videoId = payload["videoId"] as? String,
                  let context = EarnTaskContext(
                    taskId: taskId,
                    source: source,
                    returnTarget: returnTarget,
                    videoId: videoId
                  ) else {
                return nil
            }
            self = .openTaskPlayer(context)
        default:
            return nil
        }
    }
}
