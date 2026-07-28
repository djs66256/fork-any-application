package com.djs66256.short_drama.core.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import com.djs66256.short_drama.core.di.AuthSessionPreferences
import com.djs66256.short_drama.domain.model.AuthSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class EncryptedPrefsAuthSessionStore @Inject constructor(
    @AuthSessionPreferences private val sharedPreferences: SharedPreferences,
    private val json: Json,
) : AuthSessionStore {

    override suspend fun read(): AuthSession? {
        val session = decodeAuthSession(sharedPreferences.getString(AUTH_SESSION_KEY, null), json)
        if (session == null && sharedPreferences.contains(AUTH_SESSION_KEY)) {
            clear()
        }
        return session
    }

    override suspend fun write(session: AuthSession) {
        sharedPreferences.edit {
            putString(AUTH_SESSION_KEY, encodeAuthSession(session, json))
        }
    }

    override suspend fun clear() {
        sharedPreferences.edit {
            remove(AUTH_SESSION_KEY)
        }
    }

    private companion object {
        const val AUTH_SESSION_KEY = "auth_session_payload"
    }
}
