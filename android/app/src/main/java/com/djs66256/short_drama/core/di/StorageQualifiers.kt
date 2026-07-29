package com.djs66256.short_drama.core.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppPreferencesDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlaybackSessionDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthCooldownDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthSessionPreferences

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CheckInDataStore
