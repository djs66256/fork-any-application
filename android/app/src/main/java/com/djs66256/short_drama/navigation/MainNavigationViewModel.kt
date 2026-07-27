package com.djs66256.short_drama.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class MenuPanelPresentationState {
    CLOSED,
    OPENING,
    OPEN,
    CLOSING,
}

@HiltViewModel
class MainNavigationViewModel @Inject constructor() : ViewModel() {

    data class UiState(
        val pendingRoute: PendingRoute? = null,
        val pendingMenuRoute: PendingRoute? = null,
        val menuPanelState: MenuPanelPresentationState = MenuPanelPresentationState.CLOSED,
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

    fun openMenu() {
        _uiState.update { state ->
            when (state.menuPanelState) {
                MenuPanelPresentationState.CLOSED -> state.copy(
                    menuPanelState = MenuPanelPresentationState.OPENING,
                    lastRejectedReason = null,
                )
                MenuPanelPresentationState.OPENING,
                MenuPanelPresentationState.OPEN,
                MenuPanelPresentationState.CLOSING,
                -> state
            }
        }
    }

    fun onMenuOpened() {
        _uiState.update { state ->
            when (state.menuPanelState) {
                MenuPanelPresentationState.OPENING -> state.copy(menuPanelState = MenuPanelPresentationState.OPEN)
                else -> state
            }
        }
    }

    fun closeMenu() {
        _uiState.update { state ->
            when (state.menuPanelState) {
                MenuPanelPresentationState.OPENING,
                MenuPanelPresentationState.OPEN,
                -> state.copy(menuPanelState = MenuPanelPresentationState.CLOSING)
                MenuPanelPresentationState.CLOSED,
                MenuPanelPresentationState.CLOSING,
                -> state
            }
        }
    }

    fun closeMenuThenNavigate(route: PendingRoute) {
        _uiState.update { state ->
            when (state.menuPanelState) {
                MenuPanelPresentationState.OPENING,
                MenuPanelPresentationState.OPEN,
                -> state.copy(
                    menuPanelState = MenuPanelPresentationState.CLOSING,
                    pendingMenuRoute = state.pendingMenuRoute ?: route,
                    lastRejectedReason = null,
                )
                MenuPanelPresentationState.CLOSING -> state
                MenuPanelPresentationState.CLOSED -> state.copy(
                    pendingRoute = route,
                    lastRejectedReason = null,
                )
            }
        }
    }

    fun onMenuClosedAnimationFinished() {
        _uiState.update { state ->
            if (state.menuPanelState != MenuPanelPresentationState.CLOSING) {
                return@update state
            }
            state.copy(
                pendingRoute = state.pendingMenuRoute ?: state.pendingRoute,
                pendingMenuRoute = null,
                menuPanelState = MenuPanelPresentationState.CLOSED,
            )
        }
    }
}
