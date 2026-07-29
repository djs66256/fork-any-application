package com.djs66256.short_drama.feature.comments.ui

import com.djs66256.short_drama.feature.comments.model.CommentPendingActionType
import com.djs66256.short_drama.feature.comments.model.CommentSource
import com.djs66256.short_drama.feature.comments.model.PendingCommentAction
import com.djs66256.short_drama.feature.comments.model.buildCommentLoginContext
import org.junit.Assert.assertEquals
import org.junit.Test

class CommentLoginPlaceholderDialogTest {

    @Test
    fun `placeholder message keeps restore only semantics for home`() {
        val context = buildCommentLoginContext(
            source = CommentSource.HOME,
            dramaId = "drama-1",
            action = PendingCommentAction(CommentPendingActionType.CREATE_COMMENT),
        )

        assertEquals(
            "当前登录能力仍是占位流程。确认后会返回首页并重新打开评论抽屉，但不会自动重试刚才的操作。",
            commentLoginPlaceholderMessage(context),
        )
    }

    @Test
    fun `placeholder message keeps restore only semantics for player`() {
        val context = buildCommentLoginContext(
            source = CommentSource.PLAYER,
            dramaId = "drama-2",
            action = PendingCommentAction(
                CommentPendingActionType.TOGGLE_LIKE,
                commentId = "comment-1",
            ),
        )

        assertEquals(
            "当前登录能力仍是占位流程。确认后会返回播放器并重新打开评论抽屉，但不会自动重试刚才的操作。",
            commentLoginPlaceholderMessage(context),
        )
    }
}
