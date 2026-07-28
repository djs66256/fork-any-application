package com.djs66256.short_drama.domain.repository

import com.djs66256.short_drama.domain.model.AuthSession
import com.djs66256.short_drama.domain.model.AuthUser

interface AuthSessionProvider {
    fun isLoggedIn(): Boolean = currentSession() != null

    fun currentSession(): AuthSession?

    fun accessToken(): String? = currentSession()?.accessToken

    fun refreshToken(): String? = currentSession()?.refreshToken

    fun currentUser(): AuthUser? = currentSession()?.user
}
