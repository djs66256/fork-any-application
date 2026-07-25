package com.djs66256.short_drama.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class MainNavigationViewModel @Inject constructor() : ViewModel() {

    data class UiState(
        val pendingRoute: PendingRoute? = null,
        val lastRejectedReason: NavigationErrorCode? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun enqueuePendingRoute(route: PendingRoute) {
        _uiState.update {
            it.copy(pendingRoute = route, lastRejectedReason = null)
        }
    }

    fun rejectPendingRoute(reason: NavigationErrorCode) {
        _uiState.update {
            it.copy(pendingRoute = null, lastRejectedReason = reason)
        }
    }

    fun consumePendingRoute() {
        _uiState.update {
            it.copy(pendingRoute = null)
        }
    }
}
