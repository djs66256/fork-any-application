package com.djs66256.short_drama.feature.search.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.HotSearchItem
import com.djs66256.short_drama.domain.model.SearchHistoryItem
import com.djs66256.short_drama.domain.model.limitSearchQueryDraft
import com.djs66256.short_drama.domain.model.normalizeSearchQueryOrNull
import com.djs66256.short_drama.domain.usecase.ClearSearchHistoryUseCase
import com.djs66256.short_drama.domain.usecase.GetHotSearchKeywordsUseCase
import com.djs66256.short_drama.domain.usecase.ObserveSearchHistoryUseCase
import com.djs66256.short_drama.feature.search.model.SearchQuickEntry
import com.djs66256.short_drama.feature.search.model.SearchQuickEntryType
import com.djs66256.short_drama.feature.search.model.defaultSearchQuickEntries
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchHomeUiState(
    val draftQuery: String = "",
    val history: List<SearchHistoryItem> = emptyList(),
    val hotSearches: List<HotSearchItem> = emptyList(),
    val isHotSearchLoading: Boolean = true,
    val hotSearchErrorMessage: String? = null,
    val quickEntries: List<SearchQuickEntry> = defaultSearchQuickEntries(),
) {
    val normalizedQuery: String? = normalizeSearchQueryOrNull(draftQuery)
}

sealed interface SearchHomeEvent {
    data class OpenSearchResult(val route: String) : SearchHomeEvent
    data class OpenQuickEntry(val route: String) : SearchHomeEvent
}

@HiltViewModel
class SearchHomeViewModel @Inject constructor(
    observeSearchHistoryUseCase: ObserveSearchHistoryUseCase,
    private val getHotSearchKeywordsUseCase: GetHotSearchKeywordsUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchHomeUiState())
    val uiState: StateFlow<SearchHomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SearchHomeEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<SearchHomeEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            observeSearchHistoryUseCase().collect { history ->
                _uiState.update { state -> state.copy(history = history) }
            }
        }
        loadHotSearches()
    }

    fun onQueryChange(input: String) {
        _uiState.update { state ->
            state.copy(draftQuery = limitSearchQueryDraft(input))
        }
    }

    fun submitDraftQuery() {
        submitQuery(_uiState.value.draftQuery)
    }

    fun submitHistory(keyword: String) {
        submitQuery(keyword)
    }

    fun submitHotSearch(keyword: String) {
        submitQuery(keyword)
    }

    fun openQuickEntry(type: SearchQuickEntryType) {
        val route = _uiState.value.quickEntries.firstOrNull { it.type == type }?.route ?: return
        viewModelScope.launch {
            _events.emit(SearchHomeEvent.OpenQuickEntry(route))
        }
    }

    fun retryHotSearch() {
        loadHotSearches()
    }

    fun clearHistory() {
        viewModelScope.launch {
            clearSearchHistoryUseCase()
        }
    }

    private fun submitQuery(rawQuery: String) {
        val normalizedQuery = normalizeSearchQueryOrNull(rawQuery) ?: return
        viewModelScope.launch {
            _events.emit(SearchHomeEvent.OpenSearchResult(route = searchResultRoute(normalizedQuery)))
        }
    }

    private fun loadHotSearches() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isHotSearchLoading = true,
                    hotSearchErrorMessage = null,
                )
            }

            try {
                when (val result = getHotSearchKeywordsUseCase()) {
                    is ApiResult.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                hotSearches = result.data,
                                isHotSearchLoading = false,
                                hotSearchErrorMessage = null,
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        showHotSearchError(result.message.ifBlank { HOT_SEARCH_ERROR_MESSAGE })
                    }
                    is ApiResult.Exception -> {
                        showHotSearchError(HOT_SEARCH_ERROR_MESSAGE)
                    }
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Throwable) {
                showHotSearchError(HOT_SEARCH_ERROR_MESSAGE)
            }
        }
    }

    private fun showHotSearchError(message: String) {
        _uiState.update { state ->
            state.copy(
                hotSearches = emptyList(),
                isHotSearchLoading = false,
                hotSearchErrorMessage = message,
            )
        }
    }

    private companion object {
        const val HOT_SEARCH_ERROR_MESSAGE = "热搜加载失败，请重试"

        fun searchResultRoute(query: String): String {
            return com.djs66256.short_drama.navigation.AppDestination.searchResult(query)
        }
    }
}
