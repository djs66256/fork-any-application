package com.djs66256.short_drama.feature.earn.model

sealed interface EarnBridgeMessage {
    data class RequestLogin(
        val context: EarnLoginContext,
    ) : EarnBridgeMessage

    data class OpenTaskPlayer(
        val context: EarnTaskContext,
    ) : EarnBridgeMessage

    data class Invalid(
        val type: String?,
        val reason: String,
    ) : EarnBridgeMessage
}
