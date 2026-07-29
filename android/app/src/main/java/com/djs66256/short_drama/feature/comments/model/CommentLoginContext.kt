package com.djs66256.short_drama.feature.comments.model

import com.djs66256.short_drama.navigation.AppDestination

enum class CommentSource {
    HOME,
    PLAYER,
}

enum class CommentPendingActionType {
    OPEN_SHEET,
    CREATE_COMMENT,
    TOGGLE_LIKE,
}

data class PendingCommentAction(
    val type: CommentPendingActionType,
    val commentId: String? = null,
)

data class CommentLoginContext(
    val source: CommentSource,
    val dramaId: String,
    val returnRoute: String,
    val action: PendingCommentAction,
)

fun buildCommentLoginContext(
    source: CommentSource,
    dramaId: String,
    action: PendingCommentAction,
): CommentLoginContext {
    val returnRoute = when (source) {
        CommentSource.HOME -> AppDestination.Route.HOME
        CommentSource.PLAYER -> AppDestination.play(dramaId)
    }
    return CommentLoginContext(
        source = source,
        dramaId = dramaId,
        returnRoute = returnRoute,
        action = action,
    )
}
