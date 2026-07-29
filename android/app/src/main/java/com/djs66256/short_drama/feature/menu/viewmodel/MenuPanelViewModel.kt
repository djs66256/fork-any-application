package com.djs66256.short_drama.feature.menu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.storage.PlaybackSessionStore
import com.djs66256.short_drama.domain.model.MessagePreview
import com.djs66256.short_drama.domain.model.RecentlyViewed
import com.djs66256.short_drama.domain.usecase.GetMessagePreviewUseCase
import com.djs66256.short_drama.domain.usecase.GetRecentlyViewedUseCase
import com.djs66256.short_drama.feature.menu.model.MenuPanelStaticEntries
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecentlyViewedSectionUiState(
    val items: List<RecentlyViewed> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasLoaded: Boolean = false,
    val isRetrying: Boolean = false,
) {
    val isEmpty: Boolean
        get() = hasLoaded && !isLoading && errorMessage == null && items.isEmpty()
}

data class MessagePreviewUiState(
    val preview: MessagePreview? = null,
    val errorMessage: String? = null,
    val hasLoaded: Boolean = false,
) {
    val isEmpty: Boolean
        get() = hasLoaded && preview == null && errorMessage == null
}

data class MenuPanelUiState(
    val recentlyViewed: RecentlyViewedSectionUiState = RecentlyViewedSectionUiState(),
    val messagePreview: MessagePreviewUiState = MessagePreviewUiState(),
)

sealed interface MenuPanelEvent {
    data class OpenPlayback(val dramaId: String) : MenuPanelEvent
}

@HiltViewModel
class MenuPanelViewModel @Inject constructor(
    private val playbackSessionStore: PlaybackSessionStore,
    private val getRecentlyViewedUseCase: GetRecentlyViewedUseCase,
    private val getMessagePreviewUseCase: GetMessagePreviewUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuPanelUiState())
    val uiState: StateFlow<MenuPanelUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MenuPanelEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<MenuPanelEvent> = _events.asSharedFlow()

    private var requestInFlight = false

    fun loadIfNeeded() {
        if (requestInFlight || _uiState.value.recentlyViewed.hasLoaded) {
            return
        }
        loadSections(isRetry = false)
    }

    fun retry() {
        val state = _uiState.value.recentlyViewed
        if (requestInFlight || state.errorMessage == null) {
            return
        }
        loadSections(isRetry = true)
    }

    fun onRecentlyViewedClick(dramaId: String) {
        if (dramaId.isBlank()) {
            return
        }
        viewModelScope.launch {
            _events.emit(MenuPanelEvent.OpenPlayback(dramaId))
        }
    }

    private fun loadSections(isRetry: Boolean) {
        requestInFlight = true
        _uiState.update { state ->
            state.copy(
                recentlyViewed = state.recentlyViewed.copy(
                    isLoading = true,
                    errorMessage = null,
                    isRetrying = isRetry,
                ),
            )
        }

        viewModelScope.launch {
            try {
                val sessionId = playbackSessionStore.getOrCreateSessionId()
                val recentlyViewedDeferred = async { getRecentlyViewedUseCase(sessionId) }
                val messagePreviewDeferred = async { getMessagePreviewUseCase() }
                val results = awaitAll(recentlyViewedDeferred, messagePreviewDeferred)
                val recentlyViewedResult = results[0] as ApiResult<List<RecentlyViewed>>
                val messagePreviewResult = results[1] as ApiResult<MessagePreview?>
                _uiState.value = MenuPanelUiState(
                    recentlyViewed = recentlyViewedStateFrom(recentlyViewedResult),
                    messagePreview = messagePreviewStateFrom(messagePreviewResult),
                )
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Throwable) {
                _uiState.value = MenuPanelUiState(
                    recentlyViewed = RecentlyViewedSectionUiState(
                        items = emptyList(),
                        isLoading = false,
                        errorMessage = DEFAULT_RECENTLY_VIEWED_ERROR_MESSAGE,
                        hasLoaded = true,
                        isRetrying = false,
                    ),
                    messagePreview = MessagePreviewUiState(
                        preview = null,
                        errorMessage = DEFAULT_PREVIEW_ERROR_MESSAGE,
                        hasLoaded = true,
                    ),
                )
            } finally {
                requestInFlight = false
            }
        }
    }

    private fun recentlyViewedStateFrom(result: ApiResult<List<RecentlyViewed>>): RecentlyViewedSectionUiState {
        return when (result) {
            is ApiResult.Success -> RecentlyViewedSectionUiState(
                items = result.data.take(MenuPanelStaticEntries.MAX_RECENTLY_VIEWED_COUNT),
                isLoading = false,
                errorMessage = null,
                hasLoaded = true,
                isRetrying = false,
            )
            is ApiResult.Error -> RecentlyViewedSectionUiState(
                items = emptyList(),
                isLoading = false,
                errorMessage = result.message.ifBlank { DEFAULT_RECENTLY_VIEWED_ERROR_MESSAGE },
                hasLoaded = true,
                isRetrying = false,
            )
            is ApiResult.Exception -> RecentlyViewedSectionUiState(
                items = emptyList(),
                isLoading = false,
                errorMessage = DEFAULT_RECENTLY_VIEWED_ERROR_MESSAGE,
                hasLoaded = true,
                isRetrying = false,
            )
        }
    }

    private fun messagePreviewStateFrom(result: ApiResult<MessagePreview?>): MessagePreviewUiState {
        return when (result) {
            is ApiResult.Success -> MessagePreviewUiState(
                preview = result.data,
                errorMessage = null,
                hasLoaded = true,
            )
            is ApiResult.Error -> MessagePreviewUiState(
                preview = null,
                errorMessage = result.message.ifBlank { DEFAULT_PREVIEW_ERROR_MESSAGE },
                hasLoaded = true,
            )
            is ApiResult.Exception -> MessagePreviewUiState(
                preview = null,
                errorMessage = DEFAULT_PREVIEW_ERROR_MESSAGE,
                hasLoaded = true,
            )
        }
    }

    private companion object {
        const val DEFAULT_RECENTLY_VIEWED_ERROR_MESSAGE = "最近在看加载失败，请重试"
        const val DEFAULT_PREVIEW_ERROR_MESSAGE = "消息预览加载失败，稍后重试"
    }
}
