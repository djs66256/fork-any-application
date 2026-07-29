package com.djs66256.short_drama.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.djs66256.short_drama.core.di.CheckInDataStore
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

interface CheckInLocalStore {
    suspend fun getOrCreateInstallationId(): String
    suspend fun getDismissedServerDate(): String?
    suspend fun setDismissedServerDate(serverDate: String)
}

@Singleton
class DataStoreCheckInLocalStore @Inject constructor(
    @CheckInDataStore private val dataStore: DataStore<Preferences>,
) : CheckInLocalStore {

    override suspend fun getOrCreateInstallationId(): String {
        val existing = dataStore.data.first()[INSTALLATION_ID_KEY].orEmpty()
        if (existing.isNotBlank()) {
            return existing
        }

        val generated = UUID.randomUUID().toString()
        dataStore.edit { preferences ->
            val persisted = preferences[INSTALLATION_ID_KEY].orEmpty()
            if (persisted.isBlank()) {
                preferences[INSTALLATION_ID_KEY] = generated
            }
        }
        return dataStore.data.first()[INSTALLATION_ID_KEY] ?: generated
    }

    override suspend fun getDismissedServerDate(): String? {
        return dataStore.data.first()[DISMISSED_SERVER_DATE_KEY]
    }

    override suspend fun setDismissedServerDate(serverDate: String) {
        dataStore.edit { preferences ->
            preferences[DISMISSED_SERVER_DATE_KEY] = serverDate
        }
    }

    companion object {
        const val CHECK_IN_PREFERENCES = "check_in.preferences_pb"
        val INSTALLATION_ID_KEY = stringPreferencesKey("check_in_installation_id")
        val DISMISSED_SERVER_DATE_KEY = stringPreferencesKey("check_in_dismissed_server_date")
    }
}
