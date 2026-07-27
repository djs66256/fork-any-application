package com.djs66256.short_drama.feature.classification.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.ClassificationDimensionKey
import com.djs66256.short_drama.domain.model.ClassificationGender
import com.djs66256.short_drama.domain.model.normalizeSearchQueryOrNull
import com.djs66256.short_drama.domain.usecase.GetClassificationTagsUseCase
import com.djs66256.short_drama.feature.classification.model.ClassificationDimensionUiModel
import com.djs66256.short_drama.feature.classification.model.toUiModel
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

data class ClassificationUiState(
    val selectedGender: ClassificationGender = ClassificationGender.ALL,
    val selectedDimensionKey: ClassificationDimensionKey = DEFAULT_DIMENSION_KEY,
    val dimensions: List<ClassificationDimensionUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val hasLoadedOnce: Boolean = false,
)

sealed interface ClassificationEffect {
    data class ScrollToDimension(val key: ClassificationDimensionKey) : ClassificationEffect
}

private val DEFAULT_DIMENSION_KEY = ClassificationDimensionKey.ERA_BACKGROUND

@HiltViewModel
class ClassificationViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getClassificationTagsUseCase: GetClassificationTagsUseCase,
) : ViewModel() {
    private val initialGender = ClassificationGender.fromApiValue(
        savedStateHandle.get<String>(STATE_SELECTED_GENDER),
    )
    private val initialDimensionKey = ClassificationDimensionKey.fromApiValue(
        savedStateHandle.get<String>(STATE_SELECTED_DIMENSION),
    ) ?: DEFAULT_DIMENSION_KEY

    private val _uiState = MutableStateFlow(
        ClassificationUiState(
            selectedGender = initialGender,
            selectedDimensionKey = initialDimensionKey,
        ),
    )
    val uiState: StateFlow<ClassificationUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ClassificationEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: SharedFlow<ClassificationEffect> = _effects.asSharedFlow()

    private var latestGender = initialGender
    private var nextRequestToken = 0L
    private var activeRequestToken: Long? = null

    init {
        refresh(gender = initialGender, isRetry = false)
    }

    fun retry() {
        val state = _uiState.value
        if (state.isLoading || state.isRefreshing) {
            return
        }
        refresh(gender = state.selectedGender, isRetry = true)
    }

    fun onGenderSelected(gender: ClassificationGender) {
        val state = _uiState.value
        if (state.selectedGender == gender && (state.hasLoadedOnce || state.isLoading || state.isRefreshing)) {
            return
        }
        refresh(gender = gender, isRetry = false)
    }

    fun onDimensionSelected(key: ClassificationDimensionKey) {
        updateSelectedDimension(key)
        viewModelScope.launch {
            _effects.emit(ClassificationEffect.ScrollToDimension(key))
        }
    }

    fun onVisibleDimensionChanged(key: ClassificationDimensionKey) {
        updateSelectedDimension(key)
    }

    fun buildSearchRoute(rawTag: String): String? {
        val normalizedQuery = normalizeSearchQueryOrNull(rawTag) ?: return null
        return AppDestination.searchResult(normalizedQuery)
    }

    private fun refresh(
        gender: ClassificationGender,
        isRetry: Boolean,
    ) {
        val currentState = _uiState.value
        val keepContent = currentState.hasLoadedOnce && !isRetry
        val token = nextRequestToken()
        latestGender = gender
        activeRequestToken = token
        persistSelectedGender(gender)

        _uiState.update { state ->
            state.copy(
                selectedGender = gender,
                dimensions = if (keepContent) state.dimensions else emptyDimensions(),
                isLoading = !keepContent,
                isRefreshing = keepContent,
                errorMessage = null,
                hasLoadedOnce = keepContent,
            )
        }

        viewModelScope.launch {
            try {
                when (val result = getClassificationTagsUseCase(gender)) {
                    is ApiResult.Success -> handleRefreshSuccess(token, gender, result.data.toUiModel().dimensions)
                    is ApiResult.Error -> handleRefreshError(
                        token = token,
                        gender = gender,
                        message = result.message.ifBlank { DEFAULT_ERROR_MESSAGE },
                        keepContent = keepContent,
                    )
                    is ApiResult.Exception -> handleRefreshError(
                        token = token,
                        gender = gender,
                        message = DEFAULT_ERROR_MESSAGE,
                        keepContent = keepContent,
                    )
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Throwable) {
                handleRefreshError(
                    token = token,
                    gender = gender,
                    message = DEFAULT_ERROR_MESSAGE,
                    keepContent = keepContent,
                )
            } finally {
                if (activeRequestToken == token) {
                    activeRequestToken = null
                }
            }
        }
    }

    private suspend fun handleRefreshSuccess(
        token: Long,
        gender: ClassificationGender,
        dimensions: List<ClassificationDimensionUiModel>,
    ) {
        if (!isLatestRequest(token, gender)) {
            return
        }
        val selectedDimensionKey = dimensions.firstOrNull()?.key ?: DEFAULT_DIMENSION_KEY
        persistSelectedGender(gender)
        persistSelectedDimension(selectedDimensionKey)
        _uiState.value = ClassificationUiState(
            selectedGender = gender,
            selectedDimensionKey = selectedDimensionKey,
            dimensions = dimensions,
            isLoading = false,
            isRefreshing = false,
            errorMessage = null,
            hasLoadedOnce = true,
        )
        _effects.emit(ClassificationEffect.ScrollToDimension(selectedDimensionKey))
    }

    private fun handleRefreshError(
        token: Long,
        gender: ClassificationGender,
        message: String,
        keepContent: Boolean,
    ) {
        if (!isLatestRequest(token, gender)) {
            return
        }
        _uiState.update { state ->
            state.copy(
                selectedGender = gender,
                dimensions = if (keepContent) state.dimensions else emptyDimensions(),
                isLoading = false,
                isRefreshing = false,
                errorMessage = message,
                hasLoadedOnce = true,
            )
        }
    }

    private fun updateSelectedDimension(key: ClassificationDimensionKey) {
        _uiState.update { state ->
            if (state.selectedDimensionKey == key) {
                state
            } else {
                state.copy(selectedDimensionKey = key)
            }
        }
        persistSelectedDimension(key)
    }

    private fun persistSelectedGender(gender: ClassificationGender) {
        savedStateHandle[STATE_SELECTED_GENDER] = gender.apiValue
    }

    private fun persistSelectedDimension(key: ClassificationDimensionKey) {
        savedStateHandle[STATE_SELECTED_DIMENSION] = key.apiValue
    }

    private fun nextRequestToken(): Long {
        nextRequestToken += 1
        return nextRequestToken
    }

    private fun isLatestRequest(token: Long, gender: ClassificationGender): Boolean {
        return activeRequestToken == token && latestGender == gender
    }

    private fun emptyDimensions(): List<ClassificationDimensionUiModel> {
        return ClassificationDimensionKey.entries.map { key ->
            ClassificationDimensionUiModel(
                key = key,
                title = key.label,
                tags = emptyList(),
            )
        }
    }

    private companion object {
        const val STATE_SELECTED_GENDER = "classification.selected_gender"
        const val STATE_SELECTED_DIMENSION = "classification.selected_dimension"
        const val DEFAULT_ERROR_MESSAGE = "加载失败，请重试"
    }
}
