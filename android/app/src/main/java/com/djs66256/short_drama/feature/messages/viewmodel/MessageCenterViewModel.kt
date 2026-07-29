package com.djs66256.short_drama.feature.messages.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djs66256.short_drama.core.auth.AuthStateHolder
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.AuthStatus
import com.djs66256.short_drama.domain.model.InteractionMessage
import com.djs66256.short_drama.domain.model.SystemMessage
import com.djs66256.short_drama.domain.usecase.GetInteractionMessagesUseCase
import com.djs66256.short_drama.domain.usecase.GetSystemMessagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MessageCenterUiState(
    val systemMessages: List<SystemMessage> = emptyList(),
    val systemErrorMessage: String? = null,
    val isSystemLoading: Boolean = false,
    val interactionMessages: List<InteractionMessage> = emptyList(),
    val interactionErrorMessage: String? = null,
    val isInteractionLoading: Boolean = false,
    val showInteractionLoginGate: Boolean = true,
)

@HiltViewModel
class MessageCenterViewModel @Inject constructor(
    private val authStateHolder: AuthStateHolder,
    private val getSystemMessagesUseCase: GetSystemMessagesUseCase,
    private val getInteractionMessagesUseCase: GetInteractionMessagesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageCenterUiState())
    val uiState: StateFlow<MessageCenterUiState> = _uiState.asStateFlow()

    private var authCollectionJob: Job? = null

    init {
        loadSystemMessages()
        observeAuthState()
    }

    fun retrySystemMessages() {
        loadSystemMessages()
    }

    fun retryInteractionMessages() {
        val authStatus = authStateHolder.authStatus.value
        if (authStatus is AuthStatus.Authenticated) {
            loadInteractionMessages()
        }
    }

    private fun observeAuthState() {
        authCollectionJob?.cancel()
        authCollectionJob = viewModelScope.launch {
            authStateHolder.authStatus.collect { authStatus ->
                when (authStatus) {
                    is AuthStatus.Authenticated -> {
                        _uiState.update { state ->
                            state.copy(
                                showInteractionLoginGate = false,
                                interactionErrorMessage = null,
                            )
                        }
                        loadInteractionMessages()
                    }
                    AuthStatus.Anonymous,
                    AuthStatus.Expired,
                    AuthStatus.Refreshing,
                    AuthStatus.Restoring,
                    -> {
                        _uiState.update { state ->
                            state.copy(
                                interactionMessages = emptyList(),
                                interactionErrorMessage = null,
                                isInteractionLoading = false,
                                showInteractionLoginGate = true,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun loadSystemMessages() {
        _uiState.update { state ->
            state.copy(
                isSystemLoading = true,
                systemErrorMessage = null,
            )
        }

        viewModelScope.launch {
            when (val result = getSystemMessagesUseCase(page = DEFAULT_PAGE, pageSize = DEFAULT_PAGE_SIZE)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            systemMessages = result.data.items,
                            systemErrorMessage = null,
                            isSystemLoading = false,
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            systemMessages = emptyList(),
                            systemErrorMessage = result.message.ifBlank { DEFAULT_SYSTEM_ERROR_MESSAGE },
                            isSystemLoading = false,
                        )
                    }
                }
                is ApiResult.Exception -> {
                    _uiState.update { state ->
                        state.copy(
                            systemMessages = emptyList(),
                            systemErrorMessage = DEFAULT_SYSTEM_ERROR_MESSAGE,
                            isSystemLoading = false,
                        )
                    }
                }
            }
        }
    }

    private fun loadInteractionMessages() {
        _uiState.update { state ->
            state.copy(
                isInteractionLoading = true,
                interactionErrorMessage = null,
            )
        }

        viewModelScope.launch {
            when (val result = getInteractionMessagesUseCase(page = DEFAULT_PAGE, pageSize = DEFAULT_PAGE_SIZE)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            interactionMessages = result.data.items,
                            interactionErrorMessage = null,
                            isInteractionLoading = false,
                            showInteractionLoginGate = false,
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            interactionMessages = emptyList(),
                            interactionErrorMessage = result.message.ifBlank { DEFAULT_INTERACTION_ERROR_MESSAGE },
                            isInteractionLoading = false,
                            showInteractionLoginGate = false,
                        )
                    }
                }
                is ApiResult.Exception -> {
                    _uiState.update { state ->
                        state.copy(
                            interactionMessages = emptyList(),
                            interactionErrorMessage = DEFAULT_INTERACTION_ERROR_MESSAGE,
                            isInteractionLoading = false,
                            showInteractionLoginGate = false,
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_PAGE = 1
        const val DEFAULT_PAGE_SIZE = 20
        const val DEFAULT_SYSTEM_ERROR_MESSAGE = "系统消息加载失败，请稍后重试"
        const val DEFAULT_INTERACTION_ERROR_MESSAGE = "互动消息加载失败，稍后重试"
    }
}
