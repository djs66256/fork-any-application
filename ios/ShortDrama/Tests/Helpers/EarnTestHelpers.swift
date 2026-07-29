import Foundation
@testable import ShortDrama

func makeEarnLoginContext(
    source: String = "earn",
    returnTarget: String = "/earn"
) -> EarnLoginContext {
    guard let context = EarnLoginContext(source: source, returnTarget: returnTarget) else {
        preconditionFailure("Expected valid earn login context")
    }
    return context
}

func makeEarnTaskContext(
    taskId: String = "task-001",
    source: String = "earn",
    returnTarget: String = "/earn",
    videoId: String = "drama-001"
) -> EarnTaskContext {
    guard let context = EarnTaskContext(
        taskId: taskId,
        source: source,
        returnTarget: returnTarget,
        videoId: videoId
    ) else {
        preconditionFailure("Expected valid earn task context")
    }
    return context
}

func makeEarnTaskPlayerResult(
    taskId: String = "task-001",
    videoId: String = "drama-001",
    completed: Bool,
    reason: EarnTaskPlayerResult.Reason,
    source: String = "earn"
) -> EarnTaskPlayerResult {
    guard let result = EarnTaskPlayerResult(
        taskId: taskId,
        videoId: videoId,
        completed: completed,
        reason: reason,
        source: source
    ) else {
        preconditionFailure("Expected valid earn task player result")
    }
    return result
}
