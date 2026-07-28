package com.djs66256.short_drama.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.djs66256.short_drama.core.di.AuthCooldownDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class AuthCooldownStore @Inject constructor(
    @AuthCooldownDataStore private val dataStore: DataStore<Preferences>,
) {
    val cooldownDeadlineEpochSeconds: Flow<Long?> = dataStore.data
        .catch {
            emit(emptyPreferences())
        }
        .map { preferences ->
            preferences[COOLDOWN_DEADLINE_KEY]
        }

    suspend fun read(): Long? {
        return cooldownDeadlineEpochSeconds.first()
    }

    suspend fun write(deadlineEpochSeconds: Long) {
        dataStore.edit { preferences ->
            preferences[COOLDOWN_DEADLINE_KEY] = deadlineEpochSeconds
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(COOLDOWN_DEADLINE_KEY)
        }
    }

    private companion object {
        val COOLDOWN_DEADLINE_KEY = longPreferencesKey("auth_otp_cooldown_deadline")
    }
}
