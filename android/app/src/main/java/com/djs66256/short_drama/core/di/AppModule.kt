package com.djs66256.short_drama.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.djs66256.short_drama.core.config.AppConfig
import com.djs66256.short_drama.core.config.BuildConfigAppConfig
import com.djs66256.short_drama.core.storage.DataStorePlaybackSessionStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
