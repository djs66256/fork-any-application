import Foundation

struct EarnTaskPlayerResult: Equatable, Sendable {
    enum Reason: String, Equatable, Sendable {
        case playbackEnded = "playback-ended"
        case userExit = "user-exit"
        case backgrounded = "backgrounded"
        case error
        case containerRecreated = "container-recreated"
    }

    let taskId: String
    let videoId: String
    let completed: Bool
    let reason: Reason
    let source: String

    init?(
        taskId: String,
        videoId: String,
        completed: Bool,
        reason: Reason,
        source: String = "earn"
    ) {
        let normalizedTaskId = taskId.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedVideoId = videoId.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedSource = source.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !normalizedTaskId.isEmpty,
              !normalizedVideoId.isEmpty,
              normalizedSource == "earn" else {
            return nil
        }

        self.taskId = normalizedTaskId
        self.videoId = normalizedVideoId
        self.completed = completed
        self.reason = reason
        self.source = normalizedSource
    }
}
