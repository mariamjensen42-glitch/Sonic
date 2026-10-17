package com.cycling.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Single shared HTTP stack for the app.
 *
 * Everything that talks to the network (DeepSeek, online lyrics) goes through this client so
 * that the connection pool, the on-disk cache and the User-Agent are shared instead of each
 * service rolling its own [OkHttpClient].
 *
 * Consumers that need different timeouts derive a variant with `client.newBuilder()` — that
 * still shares the pool and cache, unlike building a second client from scratch.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val DISK_CACHE_SIZE_BYTES = 10L * 1024 * 1024

    /**
     * Sent on every request. LRCLIB asks clients to identify themselves, and a stable UA keeps
     * the unofficial NetEase endpoints from treating us as an unknown scraper.
     *
     * Deliberately carries no version number: `:data` does not generate a `BuildConfig`, and a
     * hardcoded version here would silently drift from the app's.
     */
    const val USER_AGENT = "Sonic (Android; +https://github.com/cycling02/Sonic)"

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val cacheDir = File(context.cacheDir, "http_cache")
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .cache(Cache(cacheDir, DISK_CACHE_SIZE_BYTES))
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
                chain.proceed(request)
            }
            .build()
            .also { Timber.d("NetworkModule: shared OkHttpClient created (cache=${cacheDir.path})") }
    }
}
