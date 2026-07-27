package com.djs66256.short_drama.core.di

import com.djs66256.short_drama.core.storage.DataStorePlaybackSessionStore
import com.djs66256.short_drama.core.storage.PlaybackSessionStore
import com.djs66256.short_drama.data.datasource.DramaRemoteDataSource
import com.djs66256.short_drama.data.datasource.PlayerRemoteDataSource
import com.djs66256.short_drama.data.datasource.RankingRemoteDataSource
import com.djs66256.short_drama.data.datasource.SearchRemoteDataSource
import com.djs66256.short_drama.data.local.SearchHistoryLocalDataSource
import com.djs66256.short_drama.data.repository.DramaRepositoryImpl
import com.djs66256.short_drama.data.repository.PlayerRepositoryImpl
import com.djs66256.short_drama.data.repository.RankingRepositoryImpl
import com.djs66256.short_drama.data.repository.SearchRepositoryImpl
import com.djs66256.short_drama.domain.repository.DramaRepository
import com.djs66256.short_drama.domain.repository.PlayerRepository
import com.djs66256.short_drama.domain.repository.RankingRepository
import com.djs66256.short_drama.domain.repository.SearchRepository
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
        dataSource: DramaRemoteDataSource,
    ): DramaRepository = DramaRepositoryImpl(dataSource)

    @Provides
    @Singleton
    fun provideSearchRepository(
        remoteDataSource: SearchRemoteDataSource,
        localDataSource: SearchHistoryLocalDataSource,
    ): SearchRepository = SearchRepositoryImpl(remoteDataSource, localDataSource)

    @Provides
    @Singleton
    fun provideRankingRepository(
        remoteDataSource: RankingRemoteDataSource,
    ): RankingRepository = RankingRepositoryImpl(remoteDataSource)

    @Provides
    @Singleton
    fun providePlaybackSessionStore(
        implementation: DataStorePlaybackSessionStore,
    ): PlaybackSessionStore = implementation

    @Provides
    @Singleton
    fun providePlayerRepository(
        remoteDataSource: PlayerRemoteDataSource,
        playbackSessionStore: PlaybackSessionStore,
    ): PlayerRepository = PlayerRepositoryImpl(remoteDataSource, playbackSessionStore)
}
