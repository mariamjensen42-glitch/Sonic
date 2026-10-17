package com.cycling.domain.repository

import com.cycling.domain.model.AiInfo
import com.cycling.domain.model.AiInfoType
import com.cycling.domain.model.Song

interface AiRepository {
    suspend fun getApiKey(): String?
    
    suspend fun setApiKey(apiKey: String)
    
    suspend fun hasApiKey(): Boolean
    
    suspend fun getSongInfo(songTitle: String, artist: String): Result<AiInfo>
    
    suspend fun getArtistInfo(artistName: String): Result<AiInfo>
    
    suspend fun getAlbumInfo(albumTitle: String, artist: String): Result<AiInfo>
    
    suspend fun getCachedSongInfo(songTitle: String, artist: String): AiInfo?
    
    suspend fun getCachedArtistInfo(artistName: String): AiInfo?
    
    suspend fun getCachedAlbumInfo(albumTitle: String, artist: String): AiInfo?

    suspend fun getCachedFavoritePlaylistDescription(songs: List<Song>): String?

    suspend fun cacheFavoritePlaylistDescription(songs: List<Song>, description: String)
    
    suspend fun generatePlaylistByTheme(
        theme: String,
        songs: List<Song>,
        songCount: Int
    ): Result<List<Long>>

    /**
     * 为歌单生成一段介绍文案。
     *
     * 与 [generatePlaylistByTheme] 配套：先按主题挑出曲目，再据此写介绍 ——
     * 介绍要基于真实选曲凭空编造没有意义。
     */
    suspend fun generatePlaylistDescription(
        playlistName: String,
        theme: String,
        songs: List<Song>
    ): Result<String>
}
