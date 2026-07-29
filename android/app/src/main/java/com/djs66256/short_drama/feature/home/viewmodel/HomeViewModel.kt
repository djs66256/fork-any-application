package com.djs66256.short_drama.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.CheckInDay
import com.djs66256.short_drama.domain.model.CheckInStatus
import com.djs66256.short_drama.domain.model.Drama
import com.djs66256.short_drama.domain.repository.CheckInRepository
import com.djs66256.short_drama.domain.usecase.GetCheckInStatusUseCase
import com.djs66256.short_drama.domain.usecase.GetDramasUseCase
import com.djs66256.short_drama.domain.usecase.SubmitCheckInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckInPopupUiState(
    val isVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val serverDate: String? = null,
    val todaySigned: Boolean = false,
    val currentStreak: Int = 0,
    val rewardCopy: String = "",
    val days: List<CheckInDay> = emptyList(),
    val submitErrorMessage: String? = null,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val items: List<Drama> = emptyList(),
    val errorMessage: String? = null,
    val hasLoadedOnce: Boolean = false,
    val isRetrying: Boolean = false,
    val checkInPopup: CheckInPopupUiState = CheckInPopupUiState(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDramasUseCase: GetDramasUseCase,
    private val getCheckInStatusUseCase: GetCheckInStatusUseCase,
    private val submitCheckInUseCase: SubmitCheckInUseCase,
    private val checkInRepository: CheckInRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var requestInFlight = false
    private var checkInPopupAbandoned = false

    fun loadIfNeeded() {
        if (requestInFlight || uiState.value.hasLoadedOnce) {
            return
        }
        loadDramas(isRetry = false)
    }

    fun retry() {
        val state = uiState.value
        if (requestInFlight || state.errorMessage == null) {
            return
        }
        loadDramas(isRetry = true)
    }

    fun submitCheckIn() {
        val popupState = uiState.value.checkInPopup
        if (!popupState.isVisible || popupState.isSubmitting || popupState.todaySigned) {
            return
        }

        _uiState.update { state ->
            state.copy(
                checkInPopup = state.checkInPopup.copy(
                    isSubmitting = true,
                    submitErrorMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            when (val result = submitCheckInUseCase()) {
                is ApiResult.Success -> {
                    checkInRepository.dismissForServerDate(result.data.serverDate)
                    applyCheckInStatus(result.data, forceVisible = true)
                }
                is ApiResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            checkInPopup = state.checkInPopup.copy(
                                isSubmitting = false,
                                submitErrorMessage = result.message.ifBlank { DEFAULT_CHECK_IN_SUBMIT_ERROR_MESSAGE },
                            ),
                        )
                    }
                }
                is ApiResult.Exception -> {
                    _uiState.update { state ->
                        state.copy(
                            checkInPopup = state.checkInPopup.copy(
                                isSubmitting = false,
                                submitErrorMessage = DEFAULT_CHECK_IN_SUBMIT_ERROR_MESSAGE,
                            ),
                        )
                    }
                }
            }
        }
    }

    fun dismissCheckInPopup() {
        val serverDate = uiState.value.checkInPopup.serverDate ?: return
        checkInPopupAbandoned = true
        _uiState.update { state ->
            state.copy(
                checkInPopup = state.checkInPopup.copy(isVisible = false),
            )
        }
        viewModelScope.launch {
            checkInRepository.dismissForServerDate(serverDate)
        }
    }

    fun abandonCheckInPopupForCurrentSession() {
        checkInPopupAbandoned = true
        _uiState.update { state ->
            state.copy(
                checkInPopup = state.checkInPopup.copy(isVisible = false),
            )
        }
    }

    private fun loadDramas(isRetry: Boolean) {
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
                val result = getDramasUseCase(page = FIRST_PAGE, pageSize = FEED_PAGE_SIZE)
                _uiState.value = when (result) {
                    is ApiResult.Success -> HomeUiState(
                        isLoading = false,
                        items = result.data,
                        errorMessage = null,
                        hasLoadedOnce = true,
                        isRetrying = false,
                        checkInPopup = _uiState.value.checkInPopup,
                    )
                    is ApiResult.Error -> HomeUiState(
                        isLoading = false,
                        items = emptyList(),
                        errorMessage = result.message.ifBlank { DEFAULT_ERROR_MESSAGE },
                        hasLoadedOnce = true,
                        isRetrying = false,
                        checkInPopup = _uiState.value.checkInPopup,
                    )
                    is ApiResult.Exception -> HomeUiState(
                        isLoading = false,
                        items = emptyList(),
                        errorMessage = DEFAULT_ERROR_MESSAGE,
                        hasLoadedOnce = true,
                        isRetrying = false,
                        checkInPopup = _uiState.value.checkInPopup,
                    )
                }
                loadCheckInStatusIfNeeded()
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Throwable) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    items = emptyList(),
                    errorMessage = DEFAULT_ERROR_MESSAGE,
                    hasLoadedOnce = true,
                    isRetrying = false,
                    checkInPopup = _uiState.value.checkInPopup,
                )
            } finally {
                requestInFlight = false
            }
        }
    }

    private suspend fun loadCheckInStatusIfNeeded() {
        if (checkInPopupAbandoned) {
            return
        }

        when (val result = getCheckInStatusUseCase()) {
            is ApiResult.Success -> applyCheckInStatus(result.data)
            is ApiResult.Error -> Unit
            is ApiResult.Exception -> Unit
        }
    }

    private suspend fun applyCheckInStatus(
        status: CheckInStatus,
        forceVisible: Boolean = false,
    ) {
        val dismissedServerDate = checkInRepository.getDismissedServerDate()
        val shouldShow = forceVisible || (
            !checkInPopupAbandoned &&
                status.shouldShowPopup &&
                dismissedServerDate != status.serverDate
        )

        _uiState.update { state ->
            state.copy(
                checkInPopup = state.checkInPopup.copy(
                    isVisible = shouldShow,
                    isSubmitting = false,
                    serverDate = status.serverDate,
                    todaySigned = status.todaySigned,
                    currentStreak = status.currentStreak,
                    rewardCopy = status.rewardCopy,
                    days = status.days,
                    submitErrorMessage = null,
                ),
            )
        }
    }

    private companion object {
        const val FIRST_PAGE = 1
        const val FEED_PAGE_SIZE = 10
        const val DEFAULT_ERROR_MESSAGE = "加载失败，请重试"
        const val DEFAULT_CHECK_IN_SUBMIT_ERROR_MESSAGE = "签到失败，请重试"
    }
}
