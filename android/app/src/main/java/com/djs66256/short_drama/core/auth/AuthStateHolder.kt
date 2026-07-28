package com.djs66256.short_drama.core.auth

import com.djs66256.short_drama.core.storage.AuthSessionStore
import com.djs66256.short_drama.domain.model.AuthSession
import com.djs66256.short_drama.domain.model.AuthStatus
import com.djs66256.short_drama.domain.model.AuthUser
import com.djs66256.short_drama.domain.repository.AuthSessionProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class AuthStateHolder @Inject constructor(
    private val authSessionStore: AuthSessionStore,
) : AuthSessionProvider {
    private val _authStatus = MutableStateFlow<AuthStatus>(AuthStatus.Anonymous)
    private var currentSessionSnapshot: AuthSession? = null
    val authStatus: StateFlow<AuthStatus> = _authStatus.asStateFlow()

    override fun currentSession(): AuthSession? = currentSessionSnapshot

    override fun currentUser(): AuthUser? = currentSession()?.user

    suspend fun restoreIfNeeded() {
        if (_authStatus.value != AuthStatus.Anonymous) {
            return
        }

        _authStatus.value = AuthStatus.Restoring
        val session = authSessionStore.read()
        currentSessionSnapshot = session
        _authStatus.value = if (session == null) {
            AuthStatus.Anonymous
        } else {
            AuthStatus.Authenticated(session)
        }
    }

    suspend fun updateSession(session: AuthSession) {
        authSessionStore.write(session)
        currentSessionSnapshot = session
        _authStatus.value = AuthStatus.Authenticated(session)
    }

    suspend fun clearSession() {
        authSessionStore.clear()
        currentSessionSnapshot = null
        _authStatus.value = AuthStatus.Anonymous
    }

    fun markRefreshing() {
        _authStatus.value = AuthStatus.Refreshing
    }

    fun markExpired() {
        _authStatus.value = AuthStatus.Expired
    }
}
