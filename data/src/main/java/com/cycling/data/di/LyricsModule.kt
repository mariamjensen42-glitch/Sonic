package com.cycling.data.di

import com.cycling.data.api.lyrics.LrclibLyricsProvider
import com.cycling.data.api.lyrics.NetEaseLyricsProvider
import com.cycling.domain.lyrics.provider.OnlineLyricsProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * The lyrics providers, bound as a set.
 *
 * Bound individually rather than as a map keyed by [com.cycling.domain.model.LyricsProvider]
 * because the *set* is what the repository needs: it iterates the user's configured order and
 * looks each entry up. Adding a provider is then a new `@Binds` here plus one enum constant.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LyricsModule {

    @Binds
    @IntoSet
    abstract fun bindLrclibProvider(impl: LrclibLyricsProvider): OnlineLyricsProvider

    @Binds
    @IntoSet
    abstract fun bindNetEaseProvider(impl: NetEaseLyricsProvider): OnlineLyricsProvider
}
