package com.djs66256.short_drama.core.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshApiService

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthIoDispatcher
