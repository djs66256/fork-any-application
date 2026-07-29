package com.djs66256.short_drama.feature.earn.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djs66256.short_drama.core.config.AppConfig
import com.djs66256.short_drama.domain.repository.AuthSessionProvider
import com.djs66256.short_drama.feature.earn.model.DEFAULT_EARN_ERROR_MESSAGE
import com.djs66256.short_drama.feature.earn.model.EarnBridgeMessage
import com.djs66256.short_drama.feature.earn.model.EarnHostAuthReason
import com.djs66256.short_drama.feature.earn.model.EarnHostAuthState
import com.djs66256.short_drama.feature.earn.model.EarnHostMessage
import com.djs66256.short_drama.feature.earn.model.EarnLoginContext
import com.djs66256.short_drama.feature.earn.model.EarnLoginResult
import com.djs66256.short_drama.feature.earn.model.EarnPageEvent
import com.djs66256.short_drama.feature.earn.model.EarnRestoreContext
import com.djs66256.short_drama.feature.earn.model.EarnRestoreReason
import com.djs66256.short_drama.feature.earn.model.EarnTaskContext
import com.djs66256.short_drama.feature.earn.model.EarnTaskPlayerResult
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

sealed interface EarnContainerState {
    data object Loading : EarnContainerState
    data object Success : EarnContainerState
    data class Error(
        val message: String,
    ) : EarnContainerState
}

data class EarnUiState(
    val state: EarnContainerState = EarnContainerState.Loading,
    val currentUrl: String,
    val pendingLoginContext: EarnLoginContext? = null,
    val pendingTaskContext: EarnTaskContext? = null,
    val lastLoadedHomeUrl: String? = null,
)

sealed interface EarnEffect {
    data class OpenEarnLogin(
        val context: EarnLoginContext,
    ) : EarnEffect

    data class OpenEarnTaskPlayer(
        val context: EarnTaskContext,
    ) : EarnEffect

    data class SendHostMessage(
        val message: EarnHostMessage,
    ) : EarnEffect
}

@HiltViewModel
class EarnViewModel @Inject constructor(
    private val appConfig: AppConfig,
    private val authSessionProvider: AuthSessionProvider,
) : ViewModel() {
    private val earnHomeUrl = normalizeEarnHomeUrl(appConfig.earnBaseUrl)

    private val _uiState = MutableStateFlow(
        EarnUiState(
            state = EarnContainerState.Loading,
            currentUrl = earnHomeUrl,
        ),
    )
    val uiState: StateFlow<EarnUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<EarnEffect>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: SharedFlow<EarnEffect> = _effects.asSharedFlow()

    fun retryLoadHome() {
        _uiState.update { state ->
            state.copy(
                state = EarnContainerState.Loading,
                currentUrl = earnHomeUrl,
            )
        }
    }

    fun onPageEvent(event: EarnPageEvent) {
        when (event) {
            is EarnPageEvent.LoadStarted -> {
                _uiState.update { state ->
                    state.copy(
                        state = EarnContainerState.Loading,
                        currentUrl = event.url?.takeIf { it.isNotBlank() } ?: state.currentUrl,
                    )
                }
            }

            is EarnPageEvent.LoadSucceeded -> {
                _uiState.update { state ->
                    val resolvedUrl = event.url?.takeIf { it.isNotBlank() } ?: state.currentUrl
                    state.copy(
                        state = EarnContainerState.Success,
                        currentUrl = resolvedUrl,
                        lastLoadedHomeUrl = earnHomeUrl,
                    )
                }
                emitHostAuthSync(EarnHostAuthReason.INITIAL_LOAD)
            }

            is EarnPageEvent.LoadFailed -> {
                _uiState.update { state ->
                    state.copy(
                        state = EarnContainerState.Error(
                            event.message.ifBlank { DEFAULT_EARN_ERROR_MESSAGE },
                        ),
                        currentUrl = event.url?.takeIf { it.isNotBlank() } ?: state.currentUrl,
                    )
                }
            }
        }
    }

    fun onBridgeMessage(message: EarnBridgeMessage) {
        when (message) {
            is EarnBridgeMessage.RequestLogin -> {
                if (!message.context.isValid()) {
                    return
                }
                if (_uiState.value.pendingLoginContext != null) {
                    return
                }
                _uiState.update { state ->
                    state.copy(pendingLoginContext = message.context)
                }
                viewModelScope.launch {
                    _effects.emit(EarnEffect.OpenEarnLogin(message.context))
                }
            }

            is EarnBridgeMessage.OpenTaskPlayer -> {
                if (!message.context.isValid()) {
                    return
                }
                if (_uiState.value.pendingTaskContext != null) {
                    return
                }
                _uiState.update { state ->
                    state.copy(pendingTaskContext = message.context)
                }
                viewModelScope.launch {
                    _effects.emit(EarnEffect.OpenEarnTaskPlayer(message.context))
                }
            }

            is EarnBridgeMessage.Invalid -> Unit
        }
    }

    fun onEarnLoginResult(result: EarnLoginResult) {
        _uiState.update { state ->
            state.copy(pendingLoginContext = null)
        }
        val authReason = when (result) {
            EarnLoginResult.SUCCESS -> EarnHostAuthReason.LOGIN_SUCCESS
            EarnLoginResult.CANCELLED,
            EarnLoginResult.CLOSED,
            -> EarnHostAuthReason.LOGIN_CANCEL
        }
        emitHostAuthSync(authReason)
        emitRestoreContext(EarnRestoreReason.LOGIN_RETURN)
    }

    fun onEarnTaskPlayerResult(result: EarnTaskPlayerResult) {
        val pendingTaskContext = _uiState.value.pendingTaskContext ?: return
        if (!result.isValid()) {
            return
        }
        if (pendingTaskContext.taskId != result.taskId || pendingTaskContext.videoId != result.videoId) {
            return
        }
        _uiState.update { state ->
            state.copy(pendingTaskContext = null)
        }
        if (result.completed) {
            emitTaskCompletion(result)
        }
        emitRestoreContext(EarnRestoreReason.TASK_RETURN)
    }

    fun onAppResumed() {
        emitHostAuthSync(EarnHostAuthReason.APP_RESUME)
    }

    fun onContainerRecreated() {
        _uiState.update { state ->
            state.copy(
                state = EarnContainerState.Loading,
                currentUrl = earnHomeUrl,
            )
        }
        emitHostAuthSync(EarnHostAuthReason.APP_RESUME)
        emitRestoreContext(EarnRestoreReason.CONTAINER_RECREATED)
    }

    private fun emitHostAuthSync(reason: EarnHostAuthReason) {
        val session = authSessionProvider.currentSession()
        viewModelScope.launch {
            _effects.emit(
                EarnEffect.SendHostMessage(
                    EarnHostMessage.SyncAuthState(
                        EarnHostAuthState(
                            isLoggedIn = authSessionProvider.isLoggedIn(),
                            reason = reason,
                            apiAccessToken = session?.accessToken,
                            expiresAt = session?.expiresAtIso,
                        ),
                    ),
                ),
            )
        }
    }

    private fun emitRestoreContext(reason: EarnRestoreReason) {
        viewModelScope.launch {
            _effects.emit(
                EarnEffect.SendHostMessage(
                    EarnHostMessage.RestoreContext(
                        EarnRestoreContext(
                            reason = reason,
                            preserveScroll = false,
                        ),
                    ),
                ),
            )
        }
    }

    private fun emitTaskCompletion(result: EarnTaskPlayerResult) {
        viewModelScope.launch {
            _effects.emit(
                EarnEffect.SendHostMessage(
                    EarnHostMessage.CompleteTask(result),
                ),
            )
        }
    }

    companion object {
        internal fun normalizeEarnHomeUrl(rawBaseUrl: String): String {
            val trimmed = rawBaseUrl.trim().removeSuffix("/")
            return "$trimmed/earn"
        }
    }
}
