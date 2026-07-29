package com.djs66256.short_drama.feature.mall.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djs66256.short_drama.core.config.AppConfig
import com.djs66256.short_drama.domain.repository.AuthSessionProvider
import com.djs66256.short_drama.feature.mall.model.DEFAULT_MALL_ERROR_MESSAGE
import com.djs66256.short_drama.feature.mall.model.MALL_RETURN_TARGET
import com.djs66256.short_drama.feature.mall.model.MallBridgeMessage
import com.djs66256.short_drama.feature.mall.model.MallHostAuthReason
import com.djs66256.short_drama.feature.mall.model.MallHostAuthState
import com.djs66256.short_drama.feature.mall.model.MallHostMessage
import com.djs66256.short_drama.feature.mall.model.MallLoginContext
import com.djs66256.short_drama.feature.mall.model.MallLoginResult
import com.djs66256.short_drama.feature.mall.model.MallPageEvent
import com.djs66256.short_drama.feature.mall.model.MallRestoreContext
import com.djs66256.short_drama.feature.mall.model.MallRestoreReason
import com.djs66256.short_drama.feature.mall.model.MallSearchContext
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

sealed interface MallContainerState {
    data object Loading : MallContainerState
    data object Success : MallContainerState
    data class Error(
        val message: String,
    ) : MallContainerState
}

data class MallUiState(
    val state: MallContainerState = MallContainerState.Loading,
    val currentUrl: String,
    val pendingLoginContext: MallLoginContext? = null,
    val lastLoadedHomeUrl: String? = null,
)

sealed interface MallEffect {
    data object OpenSearch : MallEffect

    data class OpenMallLogin(
        val context: MallLoginContext,
    ) : MallEffect

    data class SendHostMessage(
        val message: MallHostMessage,
    ) : MallEffect
}

@HiltViewModel
class MallViewModel @Inject constructor(
    private val appConfig: AppConfig,
    private val authSessionProvider: AuthSessionProvider,
) : ViewModel() {
    private val mallHomeUrl = normalizeMallHomeUrl(appConfig.mallBaseUrl)

    private val _uiState = MutableStateFlow(
        MallUiState(
            state = MallContainerState.Loading,
            currentUrl = mallHomeUrl,
        ),
    )
    val uiState: StateFlow<MallUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<MallEffect>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: SharedFlow<MallEffect> = _effects.asSharedFlow()

    fun retryLoadHome() {
        _uiState.update { state ->
            state.copy(
                state = MallContainerState.Loading,
                currentUrl = mallHomeUrl,
            )
        }
    }

    fun onPageEvent(event: MallPageEvent) {
        when (event) {
            is MallPageEvent.LoadStarted -> {
                _uiState.update { state ->
                    state.copy(
                        state = MallContainerState.Loading,
                        currentUrl = event.url?.takeIf { it.isNotBlank() } ?: state.currentUrl,
                    )
                }
            }

            is MallPageEvent.LoadSucceeded -> {
                _uiState.update { state ->
                    val resolvedUrl = event.url?.takeIf { it.isNotBlank() } ?: state.currentUrl
                    state.copy(
                        state = MallContainerState.Success,
                        currentUrl = resolvedUrl,
                        lastLoadedHomeUrl = mallHomeUrl,
                    )
                }
                emitHostAuthSync(MallHostAuthReason.INITIAL_LOAD)
            }

            is MallPageEvent.LoadFailed -> {
                _uiState.update { state ->
                    state.copy(
                        state = MallContainerState.Error(
                            event.message.ifBlank { DEFAULT_MALL_ERROR_MESSAGE },
                        ),
                        currentUrl = event.url?.takeIf { it.isNotBlank() } ?: state.currentUrl,
                    )
                }
            }
        }
    }

    fun onBridgeMessage(message: MallBridgeMessage) {
        when (message) {
            is MallBridgeMessage.OpenSearch -> {
                val context = MallSearchContext(
                    source = message.source,
                    returnTarget = message.returnTarget,
                )
                if (!context.isValid()) {
                    return
                }
                viewModelScope.launch {
                    _effects.emit(MallEffect.OpenSearch)
                }
            }

            is MallBridgeMessage.RequestLogin -> {
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
                    _effects.emit(MallEffect.OpenMallLogin(message.context))
                }
            }

            is MallBridgeMessage.Invalid -> Unit
        }
    }

    fun onSearchReturned() {
        emitHostAuthSync(MallHostAuthReason.INITIAL_LOAD)
        emitRestoreContext(MallRestoreReason.SEARCH_RETURN)
    }

    fun onMallLoginResult(result: MallLoginResult) {
        _uiState.update { state ->
            state.copy(pendingLoginContext = null)
        }
        val authReason = when (result) {
            MallLoginResult.SUCCESS -> MallHostAuthReason.LOGIN_SUCCESS
            MallLoginResult.CANCELLED,
            MallLoginResult.CLOSED,
            -> MallHostAuthReason.LOGIN_CANCEL
        }
        emitHostAuthSync(authReason)
        emitRestoreContext(MallRestoreReason.LOGIN_RETURN)
    }

    fun onAppResumed() {
        emitHostAuthSync(MallHostAuthReason.APP_RESUME)
    }

    fun onContainerRecreated() {
        _uiState.update { state ->
            state.copy(
                state = MallContainerState.Loading,
                currentUrl = mallHomeUrl,
            )
        }
        emitHostAuthSync(MallHostAuthReason.APP_RESUME)
        emitRestoreContext(MallRestoreReason.CONTAINER_RECREATED)
    }

    private fun emitHostAuthSync(reason: MallHostAuthReason) {
        viewModelScope.launch {
            _effects.emit(
                MallEffect.SendHostMessage(
                    MallHostMessage.SyncAuthState(
                        MallHostAuthState(
                            isLoggedIn = authSessionProvider.isLoggedIn(),
                            reason = reason,
                        ),
                    ),
                ),
            )
        }
    }

    private fun emitRestoreContext(reason: MallRestoreReason) {
        viewModelScope.launch {
            _effects.emit(
                MallEffect.SendHostMessage(
                    MallHostMessage.RestoreContext(
                        MallRestoreContext(
                            reason = reason,
                            returnTarget = MALL_RETURN_TARGET,
                            preserveScroll = false,
                        ),
                    ),
                ),
            )
        }
    }

    companion object {
        internal fun normalizeMallHomeUrl(rawBaseUrl: String): String {
            val trimmed = rawBaseUrl.trim().removeSuffix("/")
            return "$trimmed/mall"
        }
    }
}
