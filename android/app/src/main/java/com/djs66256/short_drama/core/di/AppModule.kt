package com.djs66256.short_drama.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.djs66256.short_drama.core.config.AppConfig
import com.djs66256.short_drama.core.config.BuildConfigAppConfig
import com.djs66256.short_drama.core.storage.DataStorePlaybackSessionStore
import com.djs66256.short_drama.domain.repository.AuthSessionProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
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
    @Named("searchHistory")
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("short_drama.preferences_pb") },
    )

    @Provides
    @Singleton
    fun provideAuthSessionProvider(): AuthSessionProvider = object : AuthSessionProvider {
        override fun isLoggedIn(): Boolean = false
    }

    @Provides
    @Singleton
    @Named("playbackSession")
    fun providePlaybackSessionPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = {
                context.preferencesDataStoreFile(DataStorePlaybackSessionStore.PLAYBACK_SESSION_PREFERENCES)
            },
        )
    }
}
