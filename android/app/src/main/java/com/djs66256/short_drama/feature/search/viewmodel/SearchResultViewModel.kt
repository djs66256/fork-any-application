package com.djs66256.short_drama.feature.search.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.Drama
import com.djs66256.short_drama.domain.model.limitSearchQueryDraft
import com.djs66256.short_drama.domain.model.normalizeSearchQueryOrNull
import com.djs66256.short_drama.domain.usecase.SaveSearchHistoryUseCase
import com.djs66256.short_drama.domain.usecase.SearchDramasUseCase
import com.djs66256.short_drama.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchResultUiState(
    val query: String = "",
    val draftQuery: String = "",
    val items: List<Drama> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasLoadedOnce: Boolean = false,
    val isRetrying: Boolean = false,
)

@HiltViewModel
class SearchResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val searchDramasUseCase: SearchDramasUseCase,
    private val saveSearchHistoryUseCase: SaveSearchHistoryUseCase,
) : ViewModel() {
    private val initialQuery = normalizeSearchQueryOrNull(
        savedStateHandle.get<String>(AppDestination.Arg.QUERY).orEmpty(),
    )

    private val _uiState = MutableStateFlow(
        SearchResultUiState(
            query = initialQuery.orEmpty(),
            draftQuery = initialQuery.orEmpty(),
            errorMessage = initialQuery?.let { null } ?: INVALID_QUERY_MESSAGE,
            hasLoadedOnce = false,
        ),
    )
    val uiState: StateFlow<SearchResultUiState> = _uiState.asStateFlow()

    private var activeQuery: String? = null
    private var requestInFlight = false

    init {
        if (initialQuery != null) {
            search(query = initialQuery, isRetry = false)
        }
    }

    fun onDraftQueryChange(input: String) {
        _uiState.update { state ->
            state.copy(draftQuery = limitSearchQueryDraft(input))
        }
    }

    fun submitDraftQuery() {
        val normalizedQuery = normalizeSearchQueryOrNull(_uiState.value.draftQuery) ?: return
        if (requestInFlight || normalizedQuery == activeQuery) {
            return
        }
        search(query = normalizedQuery, isRetry = false)
    }

    fun retry() {
        val retryQuery = normalizeSearchQueryOrNull(_uiState.value.query) ?: return
        if (requestInFlight) {
            return
        }
        search(query = retryQuery, isRetry = true)
    }

    private fun search(query: String, isRetry: Boolean) {
        if (requestInFlight) {
            return
        }

        requestInFlight = true
        activeQuery = query
        _uiState.update { state ->
            state.copy(
                query = query,
                draftQuery = query,
                items = emptyList(),
                isLoading = true,
                errorMessage = null,
                isRetrying = isRetry,
            )
        }

        viewModelScope.launch {
            try {
                when (val result = searchDramasUseCase(query = query, page = FIRST_PAGE, pageSize = PAGE_SIZE)) {
                    is ApiResult.Success -> {
                        saveSearchHistoryUseCase(query)
                        _uiState.value = SearchResultUiState(
                            query = query,
                            draftQuery = query,
                            items = result.data,
                            isLoading = false,
                            errorMessage = null,
                            hasLoadedOnce = true,
                            isRetrying = false,
                        )
                    }
                    is ApiResult.Error -> {
                        _uiState.value = SearchResultUiState(
                            query = query,
                            draftQuery = query,
                            items = emptyList(),
                            isLoading = false,
                            errorMessage = result.message.ifBlank { DEFAULT_ERROR_MESSAGE },
                            hasLoadedOnce = true,
                            isRetrying = false,
                        )
                    }
                    is ApiResult.Exception -> {
                        _uiState.value = SearchResultUiState(
                            query = query,
                            draftQuery = query,
                            items = emptyList(),
                            isLoading = false,
                            errorMessage = DEFAULT_ERROR_MESSAGE,
                            hasLoadedOnce = true,
                            isRetrying = false,
                        )
                    }
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Throwable) {
                _uiState.value = SearchResultUiState(
                    query = query,
                    draftQuery = query,
                    items = emptyList(),
                    isLoading = false,
                    errorMessage = DEFAULT_ERROR_MESSAGE,
                    hasLoadedOnce = true,
                    isRetrying = false,
                )
            } finally {
                requestInFlight = false
            }
        }
    }

    companion object {
        const val INVALID_QUERY_MESSAGE = "搜索词无效，请返回重新输入"
        private const val DEFAULT_ERROR_MESSAGE = "搜索失败，请重试"
        private const val FIRST_PAGE = 1
        private const val PAGE_SIZE = 10
    }
}
