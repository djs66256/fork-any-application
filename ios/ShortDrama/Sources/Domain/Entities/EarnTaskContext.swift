import Foundation

struct EarnTaskContext: Hashable, Identifiable, Sendable {
    let taskId: String
    let source: String
    let returnTarget: String
    let videoId: String

    var id: String {
        "\(taskId)-\(videoId)-\(returnTarget)"
    }

    init?(
        taskId: String,
        source: String,
        returnTarget: String,
        videoId: String
    ) {
        let normalizedTaskId = taskId.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedSource = source.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedReturnTarget = returnTarget.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedVideoId = videoId.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !normalizedTaskId.isEmpty,
              normalizedSource == "earn",
              normalizedReturnTarget == "/earn",
              !normalizedVideoId.isEmpty else {
            return nil
        }

        self.taskId = normalizedTaskId
        self.source = normalizedSource
        self.returnTarget = normalizedReturnTarget
        self.videoId = normalizedVideoId
    }
}
