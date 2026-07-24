package com.djs66256.short_drama.core.di

import com.djs66256.short_drama.data.datasource.DramaRemoteDataSource
import com.djs66256.short_drama.data.repository.DramaRepositoryImpl
import com.djs66256.short_drama.domain.repository.DramaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing repository bindings (interface → implementation).
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideDramaRepository(
        dataSource: DramaRemoteDataSource
    ): DramaRepository = DramaRepositoryImpl(dataSource)
}
