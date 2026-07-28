package com.djs66256.short_drama.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.djs66256.short_drama.core.di.PlaybackSessionDataStore
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

interface PlaybackSessionStore {
    suspend fun getOrCreateSessionId(): String
}

@Singleton
class DataStorePlaybackSessionStore @Inject constructor(
    @PlaybackSessionDataStore private val dataStore: DataStore<Preferences>,
) : PlaybackSessionStore {

    override suspend fun getOrCreateSessionId(): String {
        val existing = dataStore.data.first()[PLAYBACK_SESSION_ID_KEY].orEmpty()
        if (existing.isNotBlank()) {
            return existing
        }

        val generated = UUID.randomUUID().toString()
        dataStore.edit { preferences ->
            val persisted = preferences[PLAYBACK_SESSION_ID_KEY].orEmpty()
            if (persisted.isBlank()) {
                preferences[PLAYBACK_SESSION_ID_KEY] = generated
            }
        }

        return dataStore.data.first()[PLAYBACK_SESSION_ID_KEY] ?: generated
    }

    companion object {
        const val PLAYBACK_SESSION_PREFERENCES = "playback_session.preferences_pb"
        val PLAYBACK_SESSION_ID_KEY = stringPreferencesKey("player_playback_session_id")
    }
}
