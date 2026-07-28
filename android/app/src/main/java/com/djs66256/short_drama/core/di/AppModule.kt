package com.djs66256.short_drama.core.di

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.djs66256.short_drama.core.auth.AuthStateHolder
import com.djs66256.short_drama.core.config.AppConfig
import com.djs66256.short_drama.core.config.BuildConfigAppConfig
import com.djs66256.short_drama.core.storage.AuthSessionStore
import com.djs66256.short_drama.core.storage.DataStorePlaybackSessionStore
import com.djs66256.short_drama.core.storage.EncryptedPrefsAuthSessionStore
import com.djs66256.short_drama.domain.repository.AuthSessionProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json

/**
 * Hilt module for application-level bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppConfig(): AppConfig = BuildConfigAppConfig()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    @AuthIoDispatcher
    fun provideAuthIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @AppPreferencesDataStore
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("short_drama.preferences_pb") },
    )

    @Provides
    @Singleton
    @AuthCooldownDataStore
    fun provideAuthCooldownPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("auth_cooldown.preferences_pb") },
    )

    @Provides
    @Singleton
    @PlaybackSessionDataStore
    fun providePlaybackSessionPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = {
                context.preferencesDataStoreFile(DataStorePlaybackSessionStore.PLAYBACK_SESSION_PREFERENCES)
            },
        )
    }

    @Provides
    @Singleton
    @AuthSessionPreferences
    fun provideEncryptedAuthSessionSharedPreferences(
        @ApplicationContext context: Context,
    ): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "auth_session.preferences",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    @Provides
    @Singleton
    fun provideAuthSessionStore(
        implementation: EncryptedPrefsAuthSessionStore,
    ): AuthSessionStore = implementation

    @Provides
    @Singleton
    fun provideAuthSessionProvider(
        authStateHolder: AuthStateHolder,
    ): AuthSessionProvider = authStateHolder
}
