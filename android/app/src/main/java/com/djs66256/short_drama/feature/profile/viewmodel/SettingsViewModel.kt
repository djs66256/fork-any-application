package com.djs66256.short_drama.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLogoutSubmitting: Boolean = false,
)

sealed interface SettingsEvent {
    data object LogoutCompleted : SettingsEvent
    data class ShowMessage(val message: String) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    fun logout() {
        if (_uiState.value.isLogoutSubmitting) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLogoutSubmitting = true) }
            when (val result = logoutUseCase()) {
                is ApiResult.Success -> _events.emit(SettingsEvent.LogoutCompleted)
                is ApiResult.Error -> {
                    _events.emit(SettingsEvent.ShowMessage(result.message.ifBlank { LOGOUT_FALLBACK_MESSAGE }))
                    _events.emit(SettingsEvent.LogoutCompleted)
                }
                is ApiResult.Exception -> {
                    _events.emit(SettingsEvent.ShowMessage(LOGOUT_FALLBACK_MESSAGE))
                    _events.emit(SettingsEvent.LogoutCompleted)
                }
            }
            _uiState.update { it.copy(isLogoutSubmitting = false) }
        }
    }

    private companion object {
        const val LOGOUT_FALLBACK_MESSAGE = "退出时网络异常，已为你清理本地登录态"
    }
}
