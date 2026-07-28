package com.djs66256.short_drama.core.auth

import com.djs66256.short_drama.core.storage.AuthSessionStore
import com.djs66256.short_drama.domain.model.AuthRole
import com.djs66256.short_drama.domain.model.AuthSession
import com.djs66256.short_drama.domain.model.AuthStatus
import com.djs66256.short_drama.domain.model.AuthUser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthStateHolderTest {

    @Test
    fun `T-01 restoreIfNeeded loads persisted session into authenticated state`() = runTest {
        val session = sampleSession()
        val holder = AuthStateHolder(FakeAuthSessionStore(initialSession = session))

        holder.restoreIfNeeded()

        assertTrue(holder.authStatus.value is AuthStatus.Authenticated)
        assertEquals(session, (holder.authStatus.value as AuthStatus.Authenticated).session)
        assertEquals(session, holder.currentSession())
    }

    @Test
    fun `T-01 restoreIfNeeded falls back to anonymous when local session missing`() = runTest {
        val holder = AuthStateHolder(FakeAuthSessionStore(initialSession = null))

        holder.restoreIfNeeded()

        assertEquals(AuthStatus.Anonymous, holder.authStatus.value)
        assertEquals(null, holder.currentSession())
    }

    @Test
    fun `T-01 updateSession and clearSession keep provider view in sync`() = runTest {
        val store = FakeAuthSessionStore(initialSession = null)
        val holder = AuthStateHolder(store)
        val session = sampleSession()

        holder.updateSession(session)
        assertEquals(session, store.persistedSession)
        assertEquals(session, holder.currentSession())

        holder.clearSession()
        assertEquals(null, store.persistedSession)
        assertEquals(AuthStatus.Anonymous, holder.authStatus.value)
    }

    @Test
    fun `T-01 refreshing and expired states still retain current session until cleared`() = runTest {
        val session = sampleSession()
        val holder = AuthStateHolder(FakeAuthSessionStore(initialSession = null))

        holder.updateSession(session)
        holder.markRefreshing()
        assertEquals(AuthStatus.Refreshing, holder.authStatus.value)
        assertEquals(session, holder.currentSession())

        holder.markExpired()
        assertEquals(AuthStatus.Expired, holder.authStatus.value)
        assertEquals(session, holder.currentSession())

        holder.clearSession()
        assertEquals(null, holder.currentSession())
        assertEquals(AuthStatus.Anonymous, holder.authStatus.value)
    }

    private fun sampleSession(): AuthSession = AuthSession(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresAtIso = "2026-07-28T12:00:00Z",
        user = AuthUser(
            id = "user-1",
            phone = "138****8000",
            displayName = null,
            avatarUrl = null,
            role = AuthRole.VIEWER,
            isNewUser = false,
        ),
    )
}

private class FakeAuthSessionStore(
    initialSession: AuthSession?,
) : AuthSessionStore {
    var persistedSession: AuthSession? = initialSession

    override suspend fun read(): AuthSession? = persistedSession

    override suspend fun write(session: AuthSession) {
        persistedSession = session
    }

    override suspend fun clear() {
        persistedSession = null
    }
}
