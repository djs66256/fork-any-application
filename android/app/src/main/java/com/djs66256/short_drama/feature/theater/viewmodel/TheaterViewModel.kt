package com.djs66256.short_drama.feature.theater.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.TheaterChannel
import com.djs66256.short_drama.domain.model.TheaterDrama
import com.djs66256.short_drama.domain.model.TheaterPage
import com.djs66256.short_drama.domain.model.TheaterQuery
import com.djs66256.short_drama.domain.usecase.GetTheaterFeedUseCase
import com.djs66256.short_drama.feature.theater.model.TheaterDramaItemUiModel
import com.djs66256.short_drama.feature.theater.model.toUiModel
import com.djs66256.short_drama.navigation.TheaterShortcutRoute
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

data class TheaterUiState(
    val selectedChannel: TheaterChannel = TheaterChannel.ALL,
    val items: List<TheaterDramaItemUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isAppending: Boolean = false,
    val errorMessage: String? = null,
    val appendErrorMessage: String? = null,
    val page: Int = 1,
    val hasNextPage: Boolean = false,
    val hasLoadedOnce: Boolean = false,
)

sealed interface TheaterEffect {
    data class Navigate(val route: String) : TheaterEffect
    data class OpenPlay(val videoId: String) : TheaterEffect
    data class ShowMessage(val message: String) : TheaterEffect
}

@HiltViewModel
class TheaterViewModel @Inject constructor(
    private val getTheaterFeedUseCase: GetTheaterFeedUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TheaterUiState())
    val uiState: StateFlow<TheaterUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<TheaterEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: SharedFlow<TheaterEffect> = _effects.asSharedFlow()

    private var rawItems: List<TheaterDrama> = emptyList()
    private var latestRequestKey = TheaterRequestKey(TheaterChannel.ALL)
    private var nextRequestToken = 0L
    private var activeRefreshToken: Long? = null
    private var activeAppendToken: Long? = null

    init {
        refresh(channel = TheaterChannel.ALL)
    }

    fun retry() {
        val state = _uiState.value
        if (state.isLoading || state.isAppending) {
            return
        }
        refresh(channel = state.selectedChannel)
    }

    fun onChannelSelected(channel: TheaterChannel) {
        if (_uiState.value.selectedChannel == channel) {
            return
        }
        refresh(channel = channel)
    }

    fun loadNextPageIfNeeded() {
        val state = _uiState.value
        if (state.isLoading || state.isAppending || !state.hasNextPage) {
            return
        }

        val requestKey = TheaterRequestKey(state.selectedChannel)
        val token = nextRequestToken()
        activeAppendToken = token
        _uiState.update {
            it.copy(
                isAppending = true,
                appendErrorMessage = null,
            )
        }

        viewModelScope.launch {
            try {
                val result = getTheaterFeedUseCase(
                    TheaterQuery(
                        channel = requestKey.channel,
                        page = state.page + 1,
                        pageSize = PAGE_SIZE,
                    ),
                )
                if (!isLatestAppend(token, requestKey)) {
                    return@launch
                }
                when (result) {
                    is ApiResult.Success -> appendSuccess(result.data)
                    is ApiResult.Error -> appendError(result.message.ifBlank { DEFAULT_ERROR_MESSAGE })
                    is ApiResult.Exception -> appendError(DEFAULT_ERROR_MESSAGE)
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Throwable) {
                if (!isLatestAppend(token, requestKey)) {
                    return@launch
                }
                appendError(DEFAULT_ERROR_MESSAGE)
            } finally {
                if (activeAppendToken == token) {
                    activeAppendToken = null
                }
            }
        }
    }

    fun onSearchClick() {
        emitEffect(TheaterEffect.Navigate(TheaterShortcutRoute.Search.route))
    }

    fun onScanClick() {
        emitEffect(TheaterEffect.ShowMessage(SCAN_PLACEHOLDER_MESSAGE))
    }

    fun onShortcutClick(shortcut: TheaterShortcutRoute) {
        emitEffect(TheaterEffect.Navigate(shortcut.route))
    }

    fun onDramaClick(dramaId: String) {
        if (dramaId.isBlank()) {
            return
        }
        emitEffect(TheaterEffect.OpenPlay(dramaId))
    }

    private fun refresh(channel: TheaterChannel) {
        latestRequestKey = TheaterRequestKey(channel)
        rawItems = emptyList()
        activeAppendToken = null
        val token = nextRequestToken()
        activeRefreshToken = token

        _uiState.value = TheaterUiState(
            selectedChannel = channel,
            items = emptyList(),
            isLoading = true,
            isAppending = false,
            errorMessage = null,
            appendErrorMessage = null,
            page = FIRST_PAGE,
            hasNextPage = false,
            hasLoadedOnce = false,
        )

        viewModelScope.launch {
            try {
                val result = getTheaterFeedUseCase(
                    TheaterQuery(
                        channel = channel,
                        page = FIRST_PAGE,
                        pageSize = PAGE_SIZE,
                    ),
                )
                if (!isLatestRefresh(token, TheaterRequestKey(channel))) {
                    return@launch
                }
                when (result) {
                    is ApiResult.Success -> refreshSuccess(result.data)
                    is ApiResult.Error -> refreshError(
                        message = result.message.ifBlank { DEFAULT_ERROR_MESSAGE },
                        channel = channel,
                    )
                    is ApiResult.Exception -> refreshError(DEFAULT_ERROR_MESSAGE, channel)
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Throwable) {
                if (!isLatestRefresh(token, TheaterRequestKey(channel))) {
                    return@launch
                }
                refreshError(DEFAULT_ERROR_MESSAGE, channel)
            } finally {
                if (activeRefreshToken == token) {
                    activeRefreshToken = null
                }
            }
        }
    }

    private fun refreshSuccess(page: TheaterPage) {
        rawItems = page.items
        _uiState.update { state ->
            state.copy(
                items = page.items.map(TheaterDrama::toUiModel),
                isLoading = false,
                isAppending = false,
                errorMessage = null,
                appendErrorMessage = null,
                page = page.page,
                hasNextPage = page.hasNextPage,
                hasLoadedOnce = true,
            )
        }
    }

    private fun refreshError(
        message: String,
        channel: TheaterChannel,
    ) {
        rawItems = emptyList()
        _uiState.value = TheaterUiState(
            selectedChannel = channel,
            items = emptyList(),
            isLoading = false,
            isAppending = false,
            errorMessage = message,
            appendErrorMessage = null,
            page = FIRST_PAGE,
            hasNextPage = false,
            hasLoadedOnce = true,
        )
    }

    private fun appendSuccess(page: TheaterPage) {
        rawItems = rawItems + page.items
        _uiState.update { state ->
            state.copy(
                items = rawItems.map(TheaterDrama::toUiModel),
                isAppending = false,
                appendErrorMessage = null,
                errorMessage = null,
                page = page.page,
                hasNextPage = page.hasNextPage,
                hasLoadedOnce = true,
            )
        }
    }

    private fun appendError(message: String) {
        _uiState.update { state ->
            state.copy(
                isAppending = false,
                appendErrorMessage = message,
            )
        }
    }

    private fun emitEffect(effect: TheaterEffect) {
        _effects.tryEmit(effect)
    }

    private fun nextRequestToken(): Long {
        nextRequestToken += 1
        return nextRequestToken
    }

    private fun isLatestRefresh(token: Long, requestKey: TheaterRequestKey): Boolean {
        return activeRefreshToken == token && latestRequestKey == requestKey
    }

    private fun isLatestAppend(token: Long, requestKey: TheaterRequestKey): Boolean {
        return activeAppendToken == token && latestRequestKey == requestKey
    }

    private data class TheaterRequestKey(
        val channel: TheaterChannel,
    )

    private companion object {
        const val FIRST_PAGE = 1
        const val PAGE_SIZE = 20
        const val DEFAULT_ERROR_MESSAGE = "加载失败，请重试"
        const val SCAN_PLACEHOLDER_MESSAGE = "识图功能开发中"
    }
}
