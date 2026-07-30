package com.djs66256.short_drama.feature.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.auth.AuthStateHolder
import com.djs66256.short_drama.domain.model.AuthStatus
import com.djs66256.short_drama.domain.model.BookingAssetStatus
import com.djs66256.short_drama.domain.model.BookingAssetsPage
import com.djs66256.short_drama.domain.model.BookingAssetsQuery
import com.djs66256.short_drama.domain.usecase.GetBookingAssetsUseCase
import com.djs66256.short_drama.feature.booking.model.BookingAssetsEffect
import com.djs66256.short_drama.feature.booking.model.BookingAssetsUiState
import com.djs66256.short_drama.feature.booking.model.BookingAuthGate
import com.djs66256.short_drama.feature.booking.model.toUiModel
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BookingAssetsViewModel @Inject constructor(
    private val getBookingAssetsUseCase: GetBookingAssetsUseCase,
    private val authStateHolder: AuthStateHolder,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BookingAssetsUiState())
    val uiState: StateFlow<BookingAssetsUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<BookingAssetsEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: SharedFlow<BookingAssetsEffect> = _effects.asSharedFlow()

    private var latestQueryKey = BookingRequestKey(BookingAssetStatus.ONLINE)
    private var nextRequestToken = 0L
    private var activeRefreshToken: Long? = null
    private var activeAppendToken: Long? = null

    init {
        authStateHolder.authStatus
            .onEach(::handleAuthStatus)
            .launchIn(viewModelScope)
    }

    fun retry() {
        val state = _uiState.value
        if (state.isLoading || state.isRefreshing || state.isAppending || state.showLoginGate) {
            return
        }
        refresh(status = state.selectedStatus, isRetry = true)
    }

    fun onStatusSelected(status: BookingAssetStatus) {
        val state = _uiState.value
        if (state.selectedStatus == status || state.authGate == BookingAuthGate.Restoring) {
            return
        }
        _uiState.update { it.copy(selectedStatus = status, appendErrorMessage = null) }
        if (state.showLoginGate) {
            return
        }
        refresh(status = status, isRetry = false)
    }

    fun loadNextPageIfNeeded() {
        val state = _uiState.value
        if (
            state.showLoginGate ||
            state.isLoading ||
            state.isRefreshing ||
            state.isAppending ||
            !state.hasNextPage ||
            state.items.isEmpty()
        ) {
            return
        }

        val queryKey = BookingRequestKey(state.selectedStatus)
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
                val result = getBookingAssetsUseCase(
                    BookingAssetsQuery(
                        status = state.selectedStatus,
                        page = state.page + 1,
                        pageSize = PAGE_SIZE,
                    ),
                )
                if (!isLatestAppend(token, queryKey)) {
                    return@launch
                }
                when (result) {
                    is ApiResult.Success -> appendSuccess(result.data)
                    is ApiResult.Error -> {
                        if (handleUnauthorized(result.code)) {
                            return@launch
                        }
                        appendError(messageForErrorCode(result.code, result.message))
                    }
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

    fun onLoginClick() {
        viewModelScope.launch {
            _effects.emit(BookingAssetsEffect.RequireLogin(AppDestination.menuBooking()))
        }
    }

    private fun handleAuthStatus(status: AuthStatus) {
        when (status) {
            AuthStatus.Anonymous -> {
                activeRefreshToken = null
                activeAppendToken = null
                _uiState.value = BookingAssetsUiState(
                    selectedStatus = _uiState.value.selectedStatus,
                    authGate = BookingAuthGate.Anonymous,
                )
            }
            AuthStatus.Expired -> {
                activeRefreshToken = null
                activeAppendToken = null
                _uiState.value = BookingAssetsUiState(
                    selectedStatus = _uiState.value.selectedStatus,
                    authGate = BookingAuthGate.Expired,
                )
            }
            AuthStatus.Refreshing,
            AuthStatus.Restoring,
            -> {
                _uiState.update {
                    it.copy(
                        authGate = BookingAuthGate.Restoring,
                        isLoading = true,
                        isRefreshing = false,
                        isAppending = false,
                        appendErrorMessage = null,
                        errorMessage = null,
                    )
                }
            }
            is AuthStatus.Authenticated -> {
                val selectedStatus = _uiState.value.selectedStatus
                val shouldRefresh =
                    _uiState.value.authGate != BookingAuthGate.Authenticated ||
                        !_uiState.value.hasLoadedOnce
                _uiState.update {
                    it.copy(authGate = BookingAuthGate.Authenticated, selectedStatus = selectedStatus)
                }
                if (shouldRefresh) {
                    refresh(status = selectedStatus, isRetry = false)
                }
            }
        }
    }

    private fun refresh(status: BookingAssetStatus, isRetry: Boolean) {
        val queryKey = BookingRequestKey(status)
        latestQueryKey = queryKey
        activeAppendToken = null
        val token = nextRequestToken()
        activeRefreshToken = token
        val hadLoadedBefore = _uiState.value.hasLoadedOnce

        _uiState.update {
            it.copy(
                selectedStatus = status,
                authGate = BookingAuthGate.Authenticated,
                items = emptyList(),
                isLoading = !hadLoadedBefore || isRetry,
                isRefreshing = hadLoadedBefore && !isRetry,
                isAppending = false,
                appendErrorMessage = null,
                errorMessage = null,
                page = 1,
                hasNextPage = false,
                hasLoadedOnce = false,
            )
        }

        viewModelScope.launch {
            try {
                val result = getBookingAssetsUseCase(
                    BookingAssetsQuery(status = status, page = FIRST_PAGE, pageSize = PAGE_SIZE),
                )
                if (!isLatestRefresh(token, queryKey)) {
                    return@launch
                }
                when (result) {
                    is ApiResult.Success -> refreshSuccess(result.data)
                    is ApiResult.Error -> {
                        if (handleUnauthorized(result.code)) {
                            return@launch
                        }
                        refreshError(messageForErrorCode(result.code, result.message), status)
                    }
                    is ApiResult.Exception -> refreshError(DEFAULT_ERROR_MESSAGE, status)
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Throwable) {
                if (!isLatestRefresh(token, queryKey)) {
                    return@launch
                }
                refreshError(DEFAULT_ERROR_MESSAGE, status)
            } finally {
                if (activeRefreshToken == token) {
                    activeRefreshToken = null
                }
            }
        }
    }

    private fun refreshSuccess(page: BookingAssetsPage) {
        _uiState.update { state ->
            state.copy(
                items = page.items.map { it.toUiModel() },
                summary = page.summary,
                authGate = BookingAuthGate.Authenticated,
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

    private fun refreshError(message: String, status: BookingAssetStatus) {
        _uiState.value = BookingAssetsUiState(
            selectedStatus = status,
            authGate = BookingAuthGate.Authenticated,
            isLoading = false,
            isRefreshing = false,
            isAppending = false,
            appendErrorMessage = null,
            errorMessage = message,
            page = 1,
            hasNextPage = false,
            hasLoadedOnce = true,
        )
    }

    private fun appendSuccess(page: BookingAssetsPage) {
        _uiState.update { state ->
            state.copy(
                items = state.items + page.items.map { it.toUiModel() },
                summary = page.summary,
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
        _uiState.update {
            it.copy(
                isAppending = false,
                appendErrorMessage = message,
            )
        }
    }

    private fun handleUnauthorized(code: String): Boolean {
        if (code != UNAUTHORIZED_CODE && code != AUTH_UNAUTHORIZED_CODE) {
            return false
        }
        activeRefreshToken = null
        activeAppendToken = null
        _uiState.value = BookingAssetsUiState(
            selectedStatus = _uiState.value.selectedStatus,
            authGate = BookingAuthGate.Expired,
        )
        return true
    }

    private fun messageForErrorCode(code: String, fallback: String): String {
        return when (code) {
            TOO_MANY_REQUESTS_CODE,
            AUTH_RATE_LIMITED_CODE,
            -> TOO_MANY_REQUESTS_MESSAGE
            else -> fallback.ifBlank { DEFAULT_ERROR_MESSAGE }
        }
    }

    private fun nextRequestToken(): Long {
        nextRequestToken += 1
        return nextRequestToken
    }

    private fun isLatestRefresh(token: Long, queryKey: BookingRequestKey): Boolean {
        return activeRefreshToken == token && latestQueryKey == queryKey
    }

    private fun isLatestAppend(token: Long, queryKey: BookingRequestKey): Boolean {
        return activeAppendToken == token && latestQueryKey == queryKey
    }

    private data class BookingRequestKey(
        val status: BookingAssetStatus,
    )

    private companion object {
        const val FIRST_PAGE = 1
        const val PAGE_SIZE = 20
        const val DEFAULT_ERROR_MESSAGE = "加载失败，请重试"
        const val TOO_MANY_REQUESTS_MESSAGE = "操作过于频繁，请稍后再试"
        const val UNAUTHORIZED_CODE = "UNAUTHORIZED"
        const val AUTH_UNAUTHORIZED_CODE = "AUTH_UNAUTHORIZED"
        const val TOO_MANY_REQUESTS_CODE = "TOO_MANY_REQUESTS"
        const val AUTH_RATE_LIMITED_CODE = "AUTH_RATE_LIMITED"
    }
}
