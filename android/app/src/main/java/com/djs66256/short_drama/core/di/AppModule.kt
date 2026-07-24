package com.djs66256.short_drama.core.di

import com.djs66256.short_drama.core.config.AppConfig
import com.djs66256.short_drama.core.config.BuildConfigAppConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
}
