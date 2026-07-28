package com.djs66256.short_drama.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djs66256.short_drama.core.auth.AuthStateHolder
import com.djs66256.short_drama.domain.model.AuthStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

sealed interface ProfileUiState {
    data object Anonymous : ProfileUiState
    data object Restoring : ProfileUiState
    data class Authenticated(
        val maskedPhone: String,
        val displayName: String?,
        val isNewUser: Boolean,
    ) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authStateHolder: AuthStateHolder,
) : ViewModel() {
    private val _uiState = MutableStateFlow(authStateHolder.authStatus.value.toProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        authStateHolder.authStatus
            .onEach { status ->
                _uiState.update { status.toProfileUiState() }
            }
            .launchIn(viewModelScope)
    }

    private fun AuthStatus.toProfileUiState(): ProfileUiState = when (this) {
        AuthStatus.Anonymous,
        AuthStatus.Expired,
        -> ProfileUiState.Anonymous

        AuthStatus.Refreshing,
        AuthStatus.Restoring,
        -> ProfileUiState.Restoring

        is AuthStatus.Authenticated -> ProfileUiState.Authenticated(
            maskedPhone = session.user.phone,
            displayName = session.user.displayName,
            isNewUser = session.user.isNewUser,
        )
    }
}
