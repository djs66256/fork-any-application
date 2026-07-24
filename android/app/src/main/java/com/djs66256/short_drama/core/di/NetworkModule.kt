package com.djs66256.short_drama.core.di

import com.djs66256.short_drama.core.config.AppConfig
import com.djs66256.short_drama.core.network.ApiClient
import com.djs66256.short_drama.core.network.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing network-related singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideApiService(appConfig: AppConfig): ApiService {
        ApiClient.initialize(appConfig)
        return ApiClient.apiService
    }
}
