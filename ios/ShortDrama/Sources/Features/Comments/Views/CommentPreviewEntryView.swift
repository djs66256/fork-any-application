import SwiftUI

struct CommentPreviewEntryView: View {
    var body: some View {
        PlayerView(viewModel: makePreviewPlayerViewModel())
    }

    private func makePreviewPlayerViewModel() -> PlayerViewModel {
        let commentRepository = CommentPreviewRepository()
        let playerRepository = PlayerPreviewRepository()

        let commentViewModel = CommentSheetViewModel(
            dramaId: "preview-123",
            source: .player,
            fetchDramaCommentsUseCase: FetchDramaCommentsUseCase(repository: commentRepository),
            createCommentUseCase: CreateCommentUseCase(repository: commentRepository),
            toggleCommentLikeUseCase: ToggleCommentLikeUseCase(repository: commentRepository),
            isUserLoggedIn: { true }
        )

        let playerViewModel = PlayerViewModel(
            videoId: "preview-123",
            router: NavigationRouter(),
            fetchPlayerProgressUseCase: FetchPlayerProgressUseCase(repository: playerRepository),
            fetchDramaEpisodesUseCase: FetchDramaEpisodesUseCase(repository: playerRepository),
            startPlaybackUseCase: StartPlaybackUseCase(repository: playerRepository),
            stopPlaybackUseCase: StopPlaybackUseCase(repository: playerRepository),
            playbackSessionStore: PreviewPlaybackSessionStore(),
            commentRepository: commentRepository,
            isUserLoggedIn: { true },
            commentPreviewViewModel: commentViewModel
        )
        playerViewModel.openComments()
        return playerViewModel
    }
}

private struct CommentPreviewRepository: CommentRepositoryProtocol {
    func fetchComments(query: CommentQuery) async throws -> PagedResult<Comment> {
        PagedResult(
            items: previewComments,
            page: 1,
            pageSize: query.pageSize,
            total: 470,
            totalPages: 24
        )
    }

    func createComment(dramaId: String, content: String) async throws -> Comment {
        previewComments[0]
    }

    func toggleCommentLike(dramaId: String, commentId: String) async throws -> ToggleCommentLikeResult {
        let target = previewComments.first(where: { $0.id == commentId })
        return ToggleCommentLikeResult(
            commentId: commentId,
            liked: !(target?.liked ?? false),
            likeCount: max((target?.likeCount ?? 0) + 1, 1)
        )
    }

    private var previewComments: [Comment] {
        [
            Comment(
                id: "preview-comment-001",
                dramaId: "preview-123",
                content: "敢情这些蛋挞么都是抢来的，不是亲生的😂",
                likeCount: 2839,
                liked: false,
                createdAt: "07-20",
                updatedAt: "07-20",
                user: CommentUserSummary(id: "user-001", displayName: "用户名31750495", avatarUrl: nil)
            ),
            Comment(
                id: "preview-comment-002",
                dramaId: "preview-123",
                content: "大荒龙君",
                likeCount: 2,
                liked: false,
                createdAt: "2天前",
                updatedAt: "2天前",
                user: CommentUserSummary(id: "user-002", displayName: "用户名50499622", avatarUrl: nil)
            ),
            Comment(
                id: "preview-comment-003",
                dramaId: "preview-123",
                content: "红果，你咋知道我抖音刚刷到要来找这个???",
                likeCount: 4,
                liked: false,
                createdAt: "2天前",
                updatedAt: "2天前",
                user: CommentUserSummary(id: "user-003", displayName: "用户名69744097", avatarUrl: nil)
            ),
            Comment(
                id: "preview-comment-004",
                dramaId: "preview-123",
                content: "兄弟你好香",
                likeCount: 1,
                liked: false,
                createdAt: "3天前",
                updatedAt: "3天前",
                user: CommentUserSummary(id: "user-004", displayName: "小铮哥", avatarUrl: nil)
            ),
            Comment(
                id: "preview-comment-005",
                dramaId: "preview-123",
                content: "",
                likeCount: 0,
                liked: false,
                createdAt: "",
                updatedAt: "",
                user: CommentUserSummary(id: "user-005", displayName: "女儿阁的流架", avatarUrl: nil)
            )
        ]
    }
}

private struct PlayerPreviewRepository: PlayerRepositoryProtocol {
    func fetchProgress(dramaId: String, playbackSessionId: String) async throws -> PlayerProgress {
        PlayerProgress(
            dramaId: dramaId,
            hasHistory: true,
            episodeId: "preview-ep-003",
            startTime: 24,
            updatedAt: "2026-07-31T00:00:00Z"
        )
    }

    func fetchEpisodes(dramaId: String) async throws -> EpisodeList {
        EpisodeList(
            dramaId: dramaId,
            seriesStatus: .completed,
            items: [
                Episode(
                    id: "preview-ep-003",
                    dramaId: dramaId,
                    title: "全族托举农门状元郎",
                    episodeNumber: 3,
                    videoUrl: "",
                    duration: 100,
                    thumbnailUrl: "",
                    description: nil,
                    createdAt: "2026-07-31T00:00:00Z",
                    updatedAt: "2026-07-31T00:00:00Z"
                )
            ]
        )
    }

    func startPlayback(
        request: StartPlaybackRequest,
        playbackSessionId: String
    ) async throws -> PlaybackStartReceipt {
        PlaybackStartReceipt(
            dramaId: request.dramaId,
            episodeId: request.episodeId,
            acceptedProgress: request.progress,
            playbackSessionId: playbackSessionId,
            startedAt: "2026-07-31T00:00:00Z"
        )
    }

    func stopPlayback(
        request: StopPlaybackRequest,
        playbackSessionId: String
    ) async throws -> PlaybackStopReceipt {
        PlaybackStopReceipt(
            dramaId: request.dramaId,
            episodeId: request.episodeId,
            savedProgress: request.progress,
            duration: request.duration,
            updatedAt: "2026-07-31T00:00:00Z"
        )
    }
}

private struct PreviewPlaybackSessionStore: PlaybackSessionStore {
    func getOrCreateSessionId() throws -> String {
        "preview-session"
    }
}
