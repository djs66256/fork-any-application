package com.djs66256.short_drama.feature.menu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.storage.PlaybackSessionStore
import com.djs66256.short_drama.domain.model.RecentlyViewed
import com.djs66256.short_drama.domain.usecase.GetRecentlyViewedUseCase
import com.djs66256.short_drama.feature.menu.model.MenuPanelStaticEntries
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

data class MenuPanelUiState(
    val items: List<RecentlyViewed> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasLoaded: Boolean = false,
    val isRetrying: Boolean = false,
) {
    val isEmpty: Boolean
        get() = hasLoaded && !isLoading && errorMessage == null && items.isEmpty()
}

sealed interface MenuPanelEvent {
    data class OpenPlayback(val dramaId: String) : MenuPanelEvent
}

@HiltViewModel
class MenuPanelViewModel @Inject constructor(
    private val playbackSessionStore: PlaybackSessionStore,
    private val getRecentlyViewedUseCase: GetRecentlyViewedUseCase,
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
        if (requestInFlight || _uiState.value.hasLoaded) {
            return
        }
        loadRecentlyViewed(isRetry = false)
    }

    fun retry() {
        val state = _uiState.value
        if (requestInFlight || state.errorMessage == null) {
            return
        }
        loadRecentlyViewed(isRetry = true)
    }

    fun onRecentlyViewedClick(dramaId: String) {
        if (dramaId.isBlank()) {
            return
        }
        viewModelScope.launch {
            _events.emit(MenuPanelEvent.OpenPlayback(dramaId))
        }
    }

    private fun loadRecentlyViewed(isRetry: Boolean) {
        requestInFlight = true
        _uiState.update { state ->
            state.copy(
                isLoading = true,
                errorMessage = null,
                isRetrying = isRetry,
            )
        }

        viewModelScope.launch {
            try {
                val sessionId = playbackSessionStore.getOrCreateSessionId()
                val result = getRecentlyViewedUseCase(sessionId)
                _uiState.value = when (result) {
                    is ApiResult.Success -> MenuPanelUiState(
                        items = result.data.take(MenuPanelStaticEntries.MAX_RECENTLY_VIEWED_COUNT),
                        isLoading = false,
                        errorMessage = null,
                        hasLoaded = true,
                        isRetrying = false,
                    )
                    is ApiResult.Error -> MenuPanelUiState(
                        items = emptyList(),
                        isLoading = false,
                        errorMessage = result.message.ifBlank { DEFAULT_ERROR_MESSAGE },
                        hasLoaded = true,
                        isRetrying = false,
                    )
                    is ApiResult.Exception -> MenuPanelUiState(
                        items = emptyList(),
                        isLoading = false,
                        errorMessage = DEFAULT_ERROR_MESSAGE,
                        hasLoaded = true,
                        isRetrying = false,
                    )
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Throwable) {
                _uiState.value = MenuPanelUiState(
                    items = emptyList(),
                    isLoading = false,
                    errorMessage = DEFAULT_ERROR_MESSAGE,
                    hasLoaded = true,
                    isRetrying = false,
                )
            } finally {
                requestInFlight = false
            }
        }
    }

    private companion object {
        const val DEFAULT_ERROR_MESSAGE = "最近在看加载失败，请重试"
    }
}
