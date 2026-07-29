package com.djs66256.short_drama.feature.comments.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.Comment
import com.djs66256.short_drama.domain.model.CommentPage
import com.djs66256.short_drama.domain.model.CommentQuery
import com.djs66256.short_drama.domain.model.CommentSort
import com.djs66256.short_drama.domain.model.ToggleCommentLikeResult
import com.djs66256.short_drama.domain.repository.AuthSessionProvider
import com.djs66256.short_drama.domain.usecase.CreateCommentUseCase
import com.djs66256.short_drama.domain.usecase.GetDramaCommentsUseCase
import com.djs66256.short_drama.domain.usecase.ToggleCommentLikeUseCase
import com.djs66256.short_drama.feature.comments.model.CommentPendingActionType
import com.djs66256.short_drama.feature.comments.model.CommentSource
import com.djs66256.short_drama.feature.comments.model.PendingCommentAction
import com.djs66256.short_drama.feature.comments.model.toUiModel
import com.djs66256.short_drama.feature.comments.model.buildCommentLoginContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CommentSheetViewModel @Inject constructor(
    private val getDramaCommentsUseCase: GetDramaCommentsUseCase,
    private val createCommentUseCase: CreateCommentUseCase,
    private val toggleCommentLikeUseCase: ToggleCommentLikeUseCase,
    private val authSessionProvider: AuthSessionProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommentUiState())
    val uiState: StateFlow<CommentUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<CommentEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: SharedFlow<CommentEffect> = _effects.asSharedFlow()

    fun open(
        dramaId: String,
        source: CommentSource,
    ) {
        val state = _uiState.value
        if (state.dramaId == dramaId && state.source == source && state.listState != CommentListState.Idle) {
            return
        }
        resetForContext(dramaId = dramaId, source = source, sort = CommentSort.LATEST)
        loadPage(page = FIRST_PAGE, append = false)
    }

    fun retry() {
        val state = _uiState.value
        if (state.dramaId.isBlank()) {
            return
        }
        loadPage(page = FIRST_PAGE, append = false)
    }

    fun selectSort(sort: CommentSort) {
        val state = _uiState.value
        if (state.dramaId.isBlank() || state.selectedSort == sort) {
            return
        }
        resetForContext(dramaId = state.dramaId, source = state.source, sort = sort)
        loadPage(page = FIRST_PAGE, append = false)
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (
            state.dramaId.isBlank() ||
            state.isAppending ||
            state.listState == CommentListState.Loading ||
            !state.hasNextPage
        ) {
            return
        }

        _uiState.update {
            it.copy(
                isAppending = true,
                appendErrorMessage = null,
                composerErrorMessage = null,
            )
        }
        loadPage(page = state.page + 1, append = true)
    }

    fun onInputChanged(value: String) {
        _uiState.update {
            it.copy(
                inputText = value,
                composerErrorMessage = null,
            )
        }
    }

    fun submitComment() {
        val state = _uiState.value
        val trimmed = state.inputText.trim()
        if (state.dramaId.isBlank() || state.isSubmitting) {
            return
        }
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(composerErrorMessage = EMPTY_COMMENT_ERROR_MESSAGE) }
            return
        }
        if (trimmed.length > MAX_COMMENT_LENGTH) {
            _uiState.update { it.copy(composerErrorMessage = COMMENT_TOO_LONG_ERROR_MESSAGE) }
            return
        }
        if (!authSessionProvider.isLoggedIn()) {
            requireLogin(
                action = PendingCommentAction(type = CommentPendingActionType.CREATE_COMMENT),
            )
            return
        }

        _uiState.update { it.copy(isSubmitting = true, composerErrorMessage = null) }
        viewModelScope.launch {
            try {
                when (val result = createCommentUseCase(state.dramaId, trimmed)) {
                    is ApiResult.Success -> onCreateCommentSuccess(result.data)
                    is ApiResult.Error -> onCreateCommentFailure(result.message.ifBlank { DEFAULT_MESSAGE })
                    is ApiResult.Exception -> onCreateCommentFailure(DEFAULT_MESSAGE)
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Throwable) {
                onCreateCommentFailure(DEFAULT_MESSAGE)
            }
        }
    }

    fun toggleLike(commentId: String) {
        val state = _uiState.value
        if (state.dramaId.isBlank() || commentId.isBlank() || commentId in state.likingCommentIds) {
            return
        }
        if (!authSessionProvider.isLoggedIn()) {
            requireLogin(
                action = PendingCommentAction(
                    type = CommentPendingActionType.TOGGLE_LIKE,
                    commentId = commentId,
                ),
            )
            return
        }

        _uiState.update {
            it.copy(
                likingCommentIds = it.likingCommentIds + commentId,
                composerErrorMessage = null,
            )
        }
        viewModelScope.launch {
            try {
                when (val result = toggleCommentLikeUseCase(state.dramaId, commentId)) {
                    is ApiResult.Success -> applyToggleLikeResult(result.data)
                    is ApiResult.Error -> emitMessage(result.message.ifBlank { ACTION_FAILED_MESSAGE })
                    is ApiResult.Exception -> emitMessage(ACTION_FAILED_MESSAGE)
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Throwable) {
                emitMessage(ACTION_FAILED_MESSAGE)
            } finally {
                _uiState.update { current ->
                    current.copy(likingCommentIds = current.likingCommentIds - commentId)
                }
            }
        }
    }

    private fun resetForContext(
        dramaId: String,
        source: CommentSource,
        sort: CommentSort,
    ) {
        _uiState.value = CommentUiState(
            dramaId = dramaId,
            source = source,
            listState = CommentListState.Loading,
            selectedSort = sort,
            page = FIRST_PAGE,
        )
    }

    private fun loadPage(page: Int, append: Boolean) {
        val state = _uiState.value
        val query = CommentQuery(
            dramaId = state.dramaId,
            page = page,
            pageSize = PAGE_SIZE,
            sort = state.selectedSort,
        )
        viewModelScope.launch {
            try {
                when (val result = getDramaCommentsUseCase(query)) {
                    is ApiResult.Success -> onLoadSuccess(result.data, append = append)
                    is ApiResult.Error -> onLoadFailure(result.message.ifBlank { DEFAULT_MESSAGE }, append = append)
                    is ApiResult.Exception -> onLoadFailure(DEFAULT_MESSAGE, append = append)
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Throwable) {
                onLoadFailure(DEFAULT_MESSAGE, append = append)
            }
        }
    }

    private fun onLoadSuccess(page: CommentPage, append: Boolean) {
        val mappedItems = page.items.map(Comment::toUiModel)
        val newItems = if (append) {
            _uiState.value.comments + mappedItems
        } else {
            mappedItems
        }
        val newListState = when {
            newItems.isEmpty() -> CommentListState.Empty
            else -> CommentListState.Content
        }
        _uiState.update {
            it.copy(
                listState = newListState,
                comments = newItems,
                totalCount = page.total,
                page = page.page,
                hasNextPage = page.hasNextPage,
                isAppending = false,
                appendErrorMessage = null,
                errorMessage = null,
            )
        }
    }

    private fun onLoadFailure(message: String, append: Boolean) {
        _uiState.update {
            if (append) {
                it.copy(
                    isAppending = false,
                    appendErrorMessage = message,
                )
            } else {
                it.copy(
                    listState = CommentListState.Error,
                    comments = emptyList(),
                    errorMessage = message,
                    totalCount = 0,
                    hasNextPage = false,
                    isAppending = false,
                    appendErrorMessage = null,
                )
            }
        }
    }

    private fun onCreateCommentSuccess(comment: Comment) {
        _uiState.update {
            val updatedComments = listOf(comment.toUiModel()) + it.comments
            it.copy(
                listState = CommentListState.Content,
                comments = updatedComments,
                inputText = "",
                totalCount = it.totalCount + 1,
                isSubmitting = false,
                composerErrorMessage = null,
                errorMessage = null,
            )
        }
    }

    private fun onCreateCommentFailure(message: String) {
        _uiState.update { it.copy(isSubmitting = false) }
        viewModelScope.launch {
            emitMessage(message)
        }
    }

    private fun applyToggleLikeResult(result: ToggleCommentLikeResult) {
        _uiState.update { state ->
            state.copy(
                comments = state.comments.map { item ->
                    if (item.id == result.commentId) {
                        item.copy(liked = result.liked, likeCount = result.likeCount)
                    } else {
                        item
                    }
                },
            )
        }
    }

    private fun requireLogin(action: PendingCommentAction) {
        val state = _uiState.value
        viewModelScope.launch {
            _effects.emit(
                CommentEffect.RequireLogin(
                    buildCommentLoginContext(
                        source = state.source,
                        dramaId = state.dramaId,
                        action = action,
                    ),
                ),
            )
        }
    }

    private suspend fun emitMessage(message: String) {
        _effects.emit(CommentEffect.ShowMessage(message))
    }

    private companion object {
        const val FIRST_PAGE = 1
        const val PAGE_SIZE = 20
        const val MAX_COMMENT_LENGTH = 500
        const val DEFAULT_MESSAGE = "加载失败，请重试"
        const val EMPTY_COMMENT_ERROR_MESSAGE = "评论内容不能为空"
        const val COMMENT_TOO_LONG_ERROR_MESSAGE = "评论内容不能超过 500 字"
        const val ACTION_FAILED_MESSAGE = "操作失败，请稍后重试"
    }
}
