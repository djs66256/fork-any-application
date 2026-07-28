package com.djs66256.short_drama.feature.auth.model

data class LoginUiState(
    val phone: String = "",
    val code: String = "",
    val hasAcceptedAgreement: Boolean = false,
    val phoneError: String? = null,
    val codeError: String? = null,
    val globalError: String? = null,
    val cooldownRemainingSeconds: Int = 0,
    val isSendingOtp: Boolean = false,
    val isSubmitting: Boolean = false,
) {
    val canSendOtp: Boolean
        get() = hasAcceptedAgreement &&
            !isSendingOtp &&
            !isSubmitting &&
            cooldownRemainingSeconds <= 0 &&
            phoneError == null &&
            isPhoneValid(phone)

    val canSubmit: Boolean
        get() = hasAcceptedAgreement &&
            !isSubmitting &&
            phoneError == null &&
            codeError == null &&
            isPhoneValid(phone) &&
            isCodeValid(code)
}

internal fun sanitizePhoneInput(input: String): String = input.filter(Char::isDigit).take(11)

internal fun sanitizeOtpInput(input: String): String = input.filter(Char::isDigit).take(6)

internal fun isPhoneValid(phone: String): Boolean = phone.length == 11 && phone.firstOrNull() == '1'

internal fun isCodeValid(code: String): Boolean = code.length == 6
