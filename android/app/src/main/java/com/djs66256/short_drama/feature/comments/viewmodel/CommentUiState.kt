package com.djs66256.short_drama.feature.comments.viewmodel

import com.djs66256.short_drama.domain.model.CommentSort
import com.djs66256.short_drama.feature.comments.model.CommentLoginContext
import com.djs66256.short_drama.feature.comments.model.CommentSource
import com.djs66256.short_drama.feature.comments.model.CommentUiModel

sealed interface CommentListState {
    data object Idle : CommentListState
    data object Loading : CommentListState
    data object Content : CommentListState
    data object Empty : CommentListState
    data object Error : CommentListState
}

data class CommentUiState(
    val dramaId: String = "",
    val source: CommentSource = CommentSource.PLAYER,
    val listState: CommentListState = CommentListState.Idle,
    val comments: List<CommentUiModel> = emptyList(),
    val selectedSort: CommentSort = CommentSort.LATEST,
    val inputText: String = "",
    val isSubmitting: Boolean = false,
    val likingCommentIds: Set<String> = emptySet(),
    val isAppending: Boolean = false,
    val appendErrorMessage: String? = null,
    val errorMessage: String? = null,
    val composerErrorMessage: String? = null,
    val totalCount: Int = 0,
    val page: Int = 1,
    val hasNextPage: Boolean = false,
)

sealed interface CommentEffect {
    data class RequireLogin(val context: CommentLoginContext) : CommentEffect
    data class ShowMessage(val message: String) : CommentEffect
}
