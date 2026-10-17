package com.cycling.data.mapper

import com.cycling.data.local.entity.PlaylistEntity
import com.cycling.data.local.entity.SongEntity
import com.cycling.domain.model.Artist
import com.cycling.domain.model.Playlist
import com.cycling.domain.model.Song

fun SongEntity.toDomain(): Song {
    return Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        path = path,
        albumId = albumId,
        artistId = artistId,
        dateAdded = dateAdded,
        dateModified = dateModified,
        size = size,
        mimeType = mimeType,
        // The user's own cover wins; MediaStore's is the fallback. Resolved here so every
        // existing `song.albumArt` call site keeps working untouched.
        albumArt = customAlbumArt ?: albumArt,
        isFavorite = isFavorite,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
        bitrate = bitrate,
        rating = rating,
        hasCustomAlbumArt = customAlbumArt != null
    )
}

/**
 * Converts a song **freshly read from MediaStore** into a row.
 *
 * [customAlbumArt] is intentionally left null. That is safe because the only writer of this
 * conversion path is [com.cycling.data.local.scanner.SongLibraryWriter], whose upsert omits
 * `customAlbumArt` from its `DO UPDATE SET` — so an existing row keeps whatever the user chose,
 * and a brand-new row genuinely has no custom cover yet.
 */
fun Song.toEntity(): SongEntity {
    return SongEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        path = path,
        albumId = albumId,
        artistId = artistId,
        dateAdded = dateAdded,
        dateModified = dateModified,
        size = size,
        mimeType = mimeType,
        albumArt = albumArt,
        isFavorite = isFavorite,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
        bitrate = bitrate,
        rating = rating
    )
}

fun PlaylistEntity.toDomain(): Playlist {
    return Playlist(
        id = id,
        name = name,
        dateAdded = dateAdded,
        dateModified = dateModified,
        numberOfSongs = 0,
        albumArt = null,
        description = description
    )
}
