package com.djs66256.short_drama.feature.auth.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.storage.AuthCooldownStore
import com.djs66256.short_drama.feature.auth.model.LoginUiState
import com.djs66256.short_drama.feature.auth.model.isCodeValid
import com.djs66256.short_drama.feature.auth.model.isPhoneValid
import com.djs66256.short_drama.feature.auth.model.sanitizeOtpInput
import com.djs66256.short_drama.feature.auth.model.sanitizePhoneInput
import com.djs66256.short_drama.navigation.AppDestination
import com.djs66256.short_drama.domain.usecase.CreateSessionUseCase
import com.djs66256.short_drama.domain.usecase.SendOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LoginEvent {
    data class LoginSucceeded(val route: String) : LoginEvent
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sendOtpUseCase: SendOtpUseCase,
    private val createSessionUseCase: CreateSessionUseCase,
    private val authCooldownStore: AuthCooldownStore,
) : ViewModel() {
    private val returnRoute = savedStateHandle.get<String>(AppDestination.Arg.RETURN_ROUTE)
    private var countdownJob: Job? = null

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            syncCooldown(authCooldownStore.read())
        }
    }

    fun onPhoneChange(input: String) {
        val phone = sanitizePhoneInput(input)
        _uiState.value = _uiState.value.copy(
            phone = phone,
            phoneError = null,
            globalError = null,
        )
    }

    fun onCodeChange(input: String) {
        val code = sanitizeOtpInput(input)
        _uiState.value = _uiState.value.copy(
            code = code,
            codeError = null,
            globalError = null,
        )
    }

    fun onAgreementCheckedChange(checked: Boolean) {
        _uiState.value = _uiState.value.copy(
            hasAcceptedAgreement = checked,
            globalError = null,
        )
    }

    fun sendOtp() {
        val state = _uiState.value
        if (!state.hasAcceptedAgreement) {
            _uiState.value = state.copy(globalError = AGREEMENT_ERROR_MESSAGE)
            return
        }
        if (!isPhoneValid(state.phone)) {
            _uiState.value = state.copy(phoneError = PHONE_ERROR_MESSAGE, globalError = null)
            return
        }
        if (state.isSendingOtp || state.isSubmitting || state.cooldownRemainingSeconds > 0) {
            return
        }

        val phone = state.phone
        _uiState.value = state.copy(
            isSendingOtp = true,
            phoneError = null,
            globalError = null,
        )

        viewModelScope.launch {
            when (val result = sendOtpUseCase(
                countryCode = DEFAULT_COUNTRY_CODE,
                phone = phone,
                scene = OTP_SCENE_LOGIN,
            )) {
                is ApiResult.Success -> {
                    val deadlineEpochSeconds = currentEpochSeconds() + result.data.cooldownSeconds
                    authCooldownStore.write(deadlineEpochSeconds)
                    syncCooldown(deadlineEpochSeconds)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(globalError = mapSendOtpError(result.code, result.message))
                }
                is ApiResult.Exception -> {
                    _uiState.value = _uiState.value.copy(globalError = NETWORK_ERROR_MESSAGE)
                }
            }
            _uiState.value = _uiState.value.copy(isSendingOtp = false)
        }
    }

    fun submitLogin() {
        val state = _uiState.value
        var hasError = false
        var phoneError: String? = null
        var codeError: String? = null
        var globalError: String? = null

        if (!state.hasAcceptedAgreement) {
            hasError = true
            globalError = AGREEMENT_ERROR_MESSAGE
        }
        if (!isPhoneValid(state.phone)) {
            hasError = true
            phoneError = PHONE_ERROR_MESSAGE
        }
        if (!isCodeValid(state.code)) {
            hasError = true
            codeError = CODE_ERROR_MESSAGE
        }
        if (hasError) {
            _uiState.value = state.copy(
                phoneError = phoneError,
                codeError = codeError,
                globalError = globalError,
            )
            return
        }
        if (state.isSubmitting) {
            return
        }

        val phone = state.phone
        val code = state.code
        _uiState.value = state.copy(
            isSubmitting = true,
            phoneError = null,
            codeError = null,
            globalError = null,
        )

        viewModelScope.launch {
            when (val result = createSessionUseCase(
                countryCode = DEFAULT_COUNTRY_CODE,
                phone = phone,
                code = code,
            )) {
                is ApiResult.Success -> {
                    countdownJob?.cancel()
                    authCooldownStore.clear()
                    _uiState.value = _uiState.value.copy(cooldownRemainingSeconds = 0)
                    _events.emit(LoginEvent.LoginSucceeded(resolveSuccessRoute(returnRoute)))
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(globalError = mapSubmitError(result.code, result.message))
                }
                is ApiResult.Exception -> {
                    _uiState.value = _uiState.value.copy(globalError = NETWORK_ERROR_MESSAGE)
                }
            }
            _uiState.value = _uiState.value.copy(isSubmitting = false)
        }
    }

    private suspend fun syncCooldown(deadlineEpochSeconds: Long?) {
        countdownJob?.cancel()
        val deadline = deadlineEpochSeconds ?: return updateCooldown(0)
        val initialRemaining = (deadline - currentEpochSeconds()).coerceAtLeast(0L).toInt()
        updateCooldown(initialRemaining)
        if (initialRemaining <= 0) {
            authCooldownStore.clear()
            return
        }

        countdownJob = viewModelScope.launch {
            while (true) {
                val remaining = (deadline - currentEpochSeconds()).coerceAtLeast(0L).toInt()
                updateCooldown(remaining)
                if (remaining <= 0) {
                    authCooldownStore.clear()
                    break
                }
                delay(1_000)
            }
        }
    }

    private fun updateCooldown(remainingSeconds: Int) {
        _uiState.value = _uiState.value.copy(cooldownRemainingSeconds = remainingSeconds)
    }

    private fun currentEpochSeconds(): Long = System.currentTimeMillis() / 1_000

    private fun resolveSuccessRoute(rawReturnRoute: String?): String {
        val route = rawReturnRoute.orEmpty().trim()
        return if (route.isBlank() || route.startsWith("login") || route.startsWith("settings")) {
            AppDestination.Route.PROFILE
        } else {
            route
        }
    }

    private fun mapSendOtpError(code: String, message: String): String = when (code) {
        "AUTH_INVALID_PHONE" -> PHONE_ERROR_MESSAGE
        "AUTH_RATE_LIMITED" -> "请求过于频繁，请稍后再试"
        else -> message.ifBlank { NETWORK_ERROR_MESSAGE }
    }

    private fun mapSubmitError(code: String, message: String): String = when (code) {
        "AUTH_INVALID_PHONE" -> PHONE_ERROR_MESSAGE
        "AUTH_INVALID_CODE" -> "验证码错误，请重新输入"
        "AUTH_CODE_EXPIRED" -> "验证码已过期，请重新获取"
        else -> message.ifBlank { NETWORK_ERROR_MESSAGE }
    }

    private companion object {
        const val DEFAULT_COUNTRY_CODE = "+86"
        const val OTP_SCENE_LOGIN = "login"
        const val AGREEMENT_ERROR_MESSAGE = "请先同意用户协议与隐私政策"
        const val PHONE_ERROR_MESSAGE = "请输入正确的 11 位手机号"
        const val CODE_ERROR_MESSAGE = "请输入 6 位验证码"
        const val NETWORK_ERROR_MESSAGE = "暂时无法登录，请稍后重试"
    }
}
