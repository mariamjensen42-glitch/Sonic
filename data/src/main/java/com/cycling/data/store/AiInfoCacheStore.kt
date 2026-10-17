package com.cycling.data.store

import com.cycling.data.local.dao.AiInfoCacheDao
import com.cycling.data.local.entity.AiInfoCacheEntity
import com.cycling.domain.model.AiInfo
import com.cycling.domain.model.AiInfoType
import com.cycling.domain.model.Song
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private const val FAVORITE_PLAYLIST_DESCRIPTION_CACHE_KEY = "FAVORITE_PLAYLIST_DESCRIPTION"

@Singleton
class AiInfoCacheStore @Inject constructor(
    private val aiInfoCacheDao: AiInfoCacheDao
) {
    fun buildSongCacheKey(songTitle: String, artist: String): String {
        return "SONG:${songTitle.lowercase()}:${artist.lowercase()}"
    }

    fun buildArtistCacheKey(artistName: String): String {
        return "ARTIST:${artistName.lowercase()}"
    }

    fun buildAlbumCacheKey(albumTitle: String, artist: String): String {
        return "ALBUM:${albumTitle.lowercase()}:${artist.lowercase()}"
    }

    suspend fun getSongInfo(songTitle: String, artist: String): AiInfo? {
        val cacheKey = buildSongCacheKey(songTitle, artist)
        return getByCacheKey(cacheKey)
    }

    suspend fun getArtistInfo(artistName: String): AiInfo? {
        val cacheKey = buildArtistCacheKey(artistName)
        return getByCacheKey(cacheKey)
    }

    suspend fun getAlbumInfo(albumTitle: String, artist: String): AiInfo? {
        val cacheKey = buildAlbumCacheKey(albumTitle, artist)
        return getByCacheKey(cacheKey)
    }

    suspend fun saveSongInfo(songTitle: String, artist: String, info: AiInfo) {
        val cacheKey = buildSongCacheKey(songTitle, artist)
        save(cacheKey, info)
    }

    suspend fun saveArtistInfo(artistName: String, info: AiInfo) {
        val cacheKey = buildArtistCacheKey(artistName)
        save(cacheKey, info)
    }

    suspend fun saveAlbumInfo(albumTitle: String, artist: String, info: AiInfo) {
        val cacheKey = buildAlbumCacheKey(albumTitle, artist)
        save(cacheKey, info)
    }

    suspend fun getFavoritePlaylistDescription(songs: List<Song>): String? {
        val entity = aiInfoCacheDao.getByCacheKey(FAVORITE_PLAYLIST_DESCRIPTION_CACHE_KEY) ?: return null
        val signature = buildFavoriteSongsSignature(songs)
        if (entity.type != AiInfoType.PLAYLIST_DESCRIPTION.name || entity.title != signature) return null
        return entity.content.takeIf { it.isNotBlank() }
    }

    suspend fun saveFavoritePlaylistDescription(songs: List<Song>, description: String) {
        val signature = buildFavoriteSongsSignature(songs)
        save(
            FAVORITE_PLAYLIST_DESCRIPTION_CACHE_KEY,
            AiInfo(
                type = AiInfoType.PLAYLIST_DESCRIPTION,
                title = signature,
                content = description
            )
        )
    }

    private fun buildFavoriteSongsSignature(songs: List<Song>): String {
        val input = songs.joinToString(separator = "\n") { "${it.id}\u0000${it.title}\u0000${it.artist}" }
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> byte.toUByte().toString(16).padStart(2, '0') }
    }

    private suspend fun getByCacheKey(cacheKey: String): AiInfo? {
        val entity = aiInfoCacheDao.getByCacheKey(cacheKey) ?: return null
        return AiInfo(
            type = AiInfoType.valueOf(entity.type),
            title = entity.title,
            content = entity.content
        )
    }

    private suspend fun save(cacheKey: String, info: AiInfo) {
        val entity = AiInfoCacheEntity(
            cacheKey = cacheKey,
            type = info.type.name,
            title = info.title,
            content = info.content
        )
        aiInfoCacheDao.insert(entity)
    }
}
