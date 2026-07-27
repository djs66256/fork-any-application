package com.djs66256.short_drama.feature.ranking.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.RankingContentType
import com.djs66256.short_drama.domain.model.RankingDrama
import com.djs66256.short_drama.domain.model.RankingPage
import com.djs66256.short_drama.domain.model.RankingQuery
import com.djs66256.short_drama.domain.model.RankingType
import com.djs66256.short_drama.domain.repository.AuthSessionProvider
import com.djs66256.short_drama.domain.usecase.BookDramaUseCase
import com.djs66256.short_drama.domain.usecase.GetDramaRankingsUseCase
import com.djs66256.short_drama.feature.ranking.model.RankingDramaItemUiModel
import com.djs66256.short_drama.feature.ranking.model.toUiModel
import com.djs66256.short_drama.navigation.AppDestination
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

data class RankingUiState(
    val selectedContentType: RankingContentType = RankingContentType.ALL,
    val selectedRankingType: RankingType = RankingType.HOT,
    val items: List<RankingDramaItemUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isAppending: Boolean = false,
    val appendErrorMessage: String? = null,
    val errorMessage: String? = null,
    val page: Int = 1,
    val hasNextPage: Boolean = false,
    val hasLoadedOnce: Boolean = false,
    val bookingInFlightIds: Set<String> = emptySet(),
)

sealed interface RankingEffect {
    data class RequireLogin(val returnRoute: String) : RankingEffect
    data class ShowMessage(val message: String) : RankingEffect
}

@HiltViewModel
class RankingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDramaRankingsUseCase: GetDramaRankingsUseCase,
    private val bookDramaUseCase: BookDramaUseCase,
    private val authSessionProvider: AuthSessionProvider,
) : ViewModel() {
    private val initialContentType = RankingContentType.fromApiValue(
        savedStateHandle.get<String>(AppDestination.Arg.CONTENT_TYPE),
    )
    private val initialRankingType = RankingType.fromApiValue(
        savedStateHandle.get<String>(AppDestination.Arg.TYPE),
    )

    private val _uiState = MutableStateFlow(
        RankingUiState(
            selectedContentType = initialContentType,
            selectedRankingType = initialRankingType,
        ),
    )
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<RankingEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: SharedFlow<RankingEffect> = _effects.asSharedFlow()

    private var latestQueryKey = RankingRequestKey(initialContentType, initialRankingType)
    private var nextRequestToken = 0L
    private var activeRefreshToken: Long? = null
    private var activeAppendToken: Long? = null
    private var rawItems: List<RankingDrama> = emptyList()

    init {
        refresh(
            contentType = initialContentType,
            rankingType = initialRankingType,
            isRetry = false,
        )
    }

    fun retry() {
        val state = _uiState.value
        if (state.isLoading || state.isRefreshing || state.isAppending) {
            return
        }
        refresh(
            contentType = state.selectedContentType,
            rankingType = state.selectedRankingType,
            isRetry = true,
        )
    }

    fun onContentTypeSelected(contentType: RankingContentType) {
        val state = _uiState.value
        if (state.selectedContentType == contentType) {
            return
        }
        refresh(
            contentType = contentType,
            rankingType = state.selectedRankingType,
            isRetry = false,
        )
    }

    fun onRankingTypeSelected(rankingType: RankingType) {
        val state = _uiState.value
        if (state.selectedRankingType == rankingType) {
            return
        }
        refresh(
            contentType = state.selectedContentType,
            rankingType = rankingType,
            isRetry = false,
        )
    }

    fun loadNextPageIfNeeded() {
        val state = _uiState.value
        if (state.isLoading || state.isRefreshing || state.isAppending || !state.hasNextPage) {
            return
        }

        val queryKey = RankingRequestKey(state.selectedContentType, state.selectedRankingType)
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
                val result = getDramaRankingsUseCase(
                    RankingQuery(
                        contentType = queryKey.contentType,
                        type = queryKey.rankingType,
                        page = state.page + 1,
                        pageSize = PAGE_SIZE,
                    ),
                )
                if (!isLatestAppend(token, queryKey)) {
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
                if (!isLatestAppend(token, queryKey)) {
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

    fun retryAppend() {
        if (_uiState.value.appendErrorMessage == null) {
            return
        }
        loadNextPageIfNeeded()
    }

    fun onBookClick(dramaId: String) {
        val state = _uiState.value
        if (state.selectedRankingType != RankingType.BOOKING || dramaId.isBlank()) {
            return
        }
        if (dramaId in state.bookingInFlightIds) {
            return
        }
        if (!authSessionProvider.isLoggedIn()) {
            viewModelScope.launch {
                _effects.emit(
                    RankingEffect.RequireLogin(
                        AppDestination.ranking(
                            contentType = state.selectedContentType,
                            type = state.selectedRankingType,
                        ),
                    ),
                )
            }
            return
        }

        _uiState.update { currentState ->
            currentState.copy(bookingInFlightIds = currentState.bookingInFlightIds + dramaId)
        }

        viewModelScope.launch {
            try {
                when (val result = bookDramaUseCase(dramaId)) {
                    is ApiResult.Success -> {
                        rawItems = rawItems.map { drama ->
                            if (drama.id == dramaId) {
                                drama.copy(
                                    isBooked = result.data.booked,
                                    bookingCount = result.data.bookingCount,
                                )
                            } else {
                                drama
                            }
                        }
                        publishItems()
                    }
                    is ApiResult.Error -> emitMessage(result.message.ifBlank { BOOKING_ERROR_MESSAGE })
                    is ApiResult.Exception -> emitMessage(BOOKING_ERROR_MESSAGE)
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Throwable) {
                emitMessage(BOOKING_ERROR_MESSAGE)
            } finally {
                _uiState.update { currentState ->
                    currentState.copy(bookingInFlightIds = currentState.bookingInFlightIds - dramaId)
                }
            }
        }
    }

    private fun refresh(
        contentType: RankingContentType,
        rankingType: RankingType,
        isRetry: Boolean,
    ) {
        val queryKey = RankingRequestKey(contentType, rankingType)
        latestQueryKey = queryKey
        rawItems = emptyList()
        activeAppendToken = null
        val token = nextRequestToken()
        activeRefreshToken = token
        val hasLoadedBefore = _uiState.value.hasLoadedOnce

        _uiState.value = RankingUiState(
            selectedContentType = contentType,
            selectedRankingType = rankingType,
            items = emptyList(),
            isLoading = !hasLoadedBefore || isRetry,
            isRefreshing = hasLoadedBefore && !isRetry,
            isAppending = false,
            appendErrorMessage = null,
            errorMessage = null,
            page = 1,
            hasNextPage = false,
            hasLoadedOnce = false,
            bookingInFlightIds = emptySet(),
        )

        viewModelScope.launch {
            try {
                val result = getDramaRankingsUseCase(
                    RankingQuery(
                        contentType = contentType,
                        type = rankingType,
                        page = FIRST_PAGE,
                        pageSize = PAGE_SIZE,
                    ),
                )
                if (!isLatestRefresh(token, queryKey)) {
                    return@launch
                }
                when (result) {
                    is ApiResult.Success -> refreshSuccess(result.data)
                    is ApiResult.Error -> refreshError(
                        result.message.ifBlank { DEFAULT_ERROR_MESSAGE },
                        contentType,
                        rankingType,
                    )
                    is ApiResult.Exception -> refreshError(DEFAULT_ERROR_MESSAGE, contentType, rankingType)
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Throwable) {
                if (!isLatestRefresh(token, queryKey)) {
                    return@launch
                }
                refreshError(DEFAULT_ERROR_MESSAGE, contentType, rankingType)
            } finally {
                if (activeRefreshToken == token) {
                    activeRefreshToken = null
                }
            }
        }
    }

    private fun refreshSuccess(page: RankingPage) {
        rawItems = page.items
        _uiState.update { state ->
            state.copy(
                items = page.items.mapIndexed { index, drama ->
                    drama.toUiModel(
                        rank = index + 1,
                        rankingType = state.selectedRankingType,
                    )
                },
                isLoading = false,
                isRefreshing = false,
                isAppending = false,
                appendErrorMessage = null,
                errorMessage = null,
                page = page.page,
                hasNextPage = page.hasNextPage,
                hasLoadedOnce = true,
            )
        }
    }

    private fun refreshError(
        message: String,
        contentType: RankingContentType,
        rankingType: RankingType,
    ) {
        rawItems = emptyList()
        _uiState.value = RankingUiState(
            selectedContentType = contentType,
            selectedRankingType = rankingType,
            items = emptyList(),
            isLoading = false,
            isRefreshing = false,
            isAppending = false,
            appendErrorMessage = null,
            errorMessage = message,
            page = 1,
            hasNextPage = false,
            hasLoadedOnce = true,
            bookingInFlightIds = emptySet(),
        )
    }

    private fun appendSuccess(page: RankingPage) {
        rawItems = rawItems + page.items
        _uiState.update { state ->
            state.copy(
                items = rawItems.mapIndexed { index, drama ->
                    drama.toUiModel(
                        rank = index + 1,
                        rankingType = state.selectedRankingType,
                    )
                },
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

    private fun publishItems() {
        _uiState.update { state ->
            state.copy(
                items = rawItems.mapIndexed { index, drama ->
                    drama.toUiModel(
                        rank = index + 1,
                        rankingType = state.selectedRankingType,
                    )
                },
            )
        }
    }

    private suspend fun emitMessage(message: String) {
        _effects.emit(RankingEffect.ShowMessage(message))
    }

    private fun nextRequestToken(): Long {
        nextRequestToken += 1
        return nextRequestToken
    }

    private fun isLatestRefresh(token: Long, queryKey: RankingRequestKey): Boolean {
        return activeRefreshToken == token && latestQueryKey == queryKey
    }

    private fun isLatestAppend(token: Long, queryKey: RankingRequestKey): Boolean {
        return activeAppendToken == token && latestQueryKey == queryKey
    }

    private data class RankingRequestKey(
        val contentType: RankingContentType,
        val rankingType: RankingType,
    )

    private companion object {
        const val FIRST_PAGE = 1
        const val PAGE_SIZE = 10
        const val DEFAULT_ERROR_MESSAGE = "加载失败，请重试"
        const val BOOKING_ERROR_MESSAGE = "预约失败，请稍后重试"
    }
}
