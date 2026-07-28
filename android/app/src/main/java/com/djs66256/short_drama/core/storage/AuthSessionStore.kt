package com.djs66256.short_drama.core.storage

import com.djs66256.short_drama.domain.model.AuthSession

interface AuthSessionStore {
    suspend fun read(): AuthSession?

    suspend fun write(session: AuthSession)

    suspend fun clear()
}
