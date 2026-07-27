package com.djs66256.short_drama.domain.repository

interface AuthSessionProvider {
    fun isLoggedIn(): Boolean
}
