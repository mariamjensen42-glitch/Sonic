package com.cycling.data.di

import com.cycling.data.api.cover.ITunesCoverProvider
import com.cycling.data.api.cover.KugouCoverProvider
import com.cycling.data.api.cover.NetEaseCoverProvider
import com.cycling.data.api.cover.QQMusicCoverProvider
import com.cycling.domain.repository.CoverArtProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * The online cover sources, bound as a set.
 *
 * Bound as a set for the same reason as the lyrics providers: the repository owns the precedence
 * order and asks sources one at a time. Adding a source is a new `@Binds` here plus a
 * [com.cycling.domain.model.CoverSource] constant.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CoverArtModule {

    @Binds
    @IntoSet
    abstract fun bindNetEaseCoverProvider(impl: NetEaseCoverProvider): CoverArtProvider

    @Binds
    @IntoSet
    abstract fun bindQqMusicCoverProvider(impl: QQMusicCoverProvider): CoverArtProvider

    @Binds
    @IntoSet
    abstract fun bindKugouCoverProvider(impl: KugouCoverProvider): CoverArtProvider

    @Binds
    @IntoSet
    abstract fun bindITunesCoverProvider(impl: ITunesCoverProvider): CoverArtProvider
}
