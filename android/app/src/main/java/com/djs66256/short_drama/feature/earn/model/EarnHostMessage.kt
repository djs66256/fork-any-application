package com.djs66256.short_drama.feature.earn.model

sealed interface EarnHostMessage {
    data class SyncAuthState(
        val payload: EarnHostAuthState,
    ) : EarnHostMessage

    data class RestoreContext(
        val payload: EarnRestoreContext,
    ) : EarnHostMessage

    data class CompleteTask(
        val payload: EarnTaskPlayerResult,
    ) : EarnHostMessage
}
