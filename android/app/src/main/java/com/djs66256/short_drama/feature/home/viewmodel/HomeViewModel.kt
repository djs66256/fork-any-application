package com.djs66256.short_drama.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.Drama
import com.djs66256.short_drama.domain.usecase.GetDramasUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val items: List<Drama> = emptyList(),
    val errorMessage: String? = null,
    val hasLoadedOnce: Boolean = false,
    val isRetrying: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDramasUseCase: GetDramasUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var requestInFlight = false

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
                    )
                    is ApiResult.Error -> HomeUiState(
                        isLoading = false,
                        items = emptyList(),
                        errorMessage = result.message.ifBlank { DEFAULT_ERROR_MESSAGE },
                        hasLoadedOnce = true,
                        isRetrying = false,
                    )
                    is ApiResult.Exception -> HomeUiState(
                        isLoading = false,
                        items = emptyList(),
                        errorMessage = DEFAULT_ERROR_MESSAGE,
                        hasLoadedOnce = true,
                        isRetrying = false,
                    )
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Throwable) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    items = emptyList(),
                    errorMessage = DEFAULT_ERROR_MESSAGE,
                    hasLoadedOnce = true,
                    isRetrying = false,
                )
            } finally {
                requestInFlight = false
            }
        }
    }

    private companion object {
        const val FIRST_PAGE = 1
        const val FEED_PAGE_SIZE = 10
        const val DEFAULT_ERROR_MESSAGE = "加载失败，请重试"
    }
}
