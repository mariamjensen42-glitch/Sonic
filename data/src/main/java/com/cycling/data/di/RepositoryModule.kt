package com.cycling.data.di

import com.cycling.data.repository.AiRepositoryImpl
import com.cycling.data.repository.AlbumRepositoryImpl
import com.cycling.data.repository.ArtistRepositoryImpl
import com.cycling.data.repository.AudioMetadataRepositoryImpl
import com.cycling.data.repository.ExcludedFolderRepositoryImpl
import com.cycling.data.repository.LyricsRepositoryImpl
import com.cycling.data.repository.MusicScannerRepositoryImpl
import com.cycling.data.repository.PlaylistRepositoryImpl
import com.cycling.data.repository.ScanProgressRepositoryImpl
import com.cycling.data.repository.SearchHistoryRepositoryImpl
import com.cycling.data.repository.SongRepositoryImpl
import com.cycling.data.repository.SongsPreferencesRepositoryImpl
import com.cycling.data.repository.ThemeRepositoryImpl
import com.cycling.domain.repository.AiRepository
import com.cycling.domain.repository.AlbumRepository
import com.cycling.domain.repository.ArtistRepository
import com.cycling.domain.repository.AudioMetadataRepository
import com.cycling.domain.repository.ExcludedFolderRepository
import com.cycling.domain.repository.LyricsRepository
import com.cycling.domain.repository.MusicScannerRepository
import com.cycling.domain.repository.PlaylistRepository
import com.cycling.domain.repository.ScanProgressRepository
import com.cycling.domain.repository.SearchHistoryRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.domain.repository.SongsPreferencesRepository
import com.cycling.domain.repository.ThemeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSongRepository(
        songRepositoryImpl: SongRepositoryImpl
    ): SongRepository

    @Binds
    @Singleton
    abstract fun bindAlbumRepository(
        albumRepositoryImpl: AlbumRepositoryImpl
    ): AlbumRepository

    @Binds
    @Singleton
    abstract fun bindArtistRepository(
        artistRepositoryImpl: ArtistRepositoryImpl
    ): ArtistRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        playlistRepositoryImpl: PlaylistRepositoryImpl
    ): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindMusicScannerRepository(
        musicScannerRepositoryImpl: MusicScannerRepositoryImpl
    ): MusicScannerRepository

    @Binds
    @Singleton
    abstract fun bindScanProgressRepository(
        scanProgressRepositoryImpl: ScanProgressRepositoryImpl
    ): ScanProgressRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(
        themeRepositoryImpl: ThemeRepositoryImpl
    ): ThemeRepository

    @Binds
    @Singleton
    abstract fun bindExcludedFolderRepository(
        excludedFolderRepositoryImpl: ExcludedFolderRepositoryImpl
    ): ExcludedFolderRepository

    @Binds
    @Singleton
    abstract fun bindSongsPreferencesRepository(
        songsPreferencesRepositoryImpl: SongsPreferencesRepositoryImpl
    ): SongsPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindAiRepository(
        aiRepositoryImpl: AiRepositoryImpl
    ): AiRepository

    @Binds
    @Singleton
    abstract fun bindSearchHistoryRepository(
        searchHistoryRepositoryImpl: SearchHistoryRepositoryImpl
    ): SearchHistoryRepository

    @Binds
    @Singleton
    abstract fun bindLyricsRepository(
        lyricsRepositoryImpl: LyricsRepositoryImpl
    ): LyricsRepository

    @Binds
    @Singleton
    abstract fun bindAudioMetadataRepository(
        audioMetadataRepositoryImpl: AudioMetadataRepositoryImpl
    ): AudioMetadataRepository
}
