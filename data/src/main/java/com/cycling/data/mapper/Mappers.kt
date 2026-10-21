package com.cycling.data.mapper

import com.cycling.data.local.entity.PlaylistEntity
import com.cycling.data.local.entity.SongEntity
import com.cycling.data.local.entity.SongMetadataEntity
import com.cycling.domain.model.Artist
import com.cycling.domain.model.Playlist
import com.cycling.domain.model.Song
import com.cycling.domain.model.TagUpdate

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
        hasCustomAlbumArt = customAlbumArt != null,
        onlineSourceKey = onlineSourceKey,
        onlineSongId = onlineSongId
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
        rating = rating,
        onlineSourceKey = onlineSourceKey,
        onlineSongId = onlineSongId
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

/** The stored overrides, as a [TagUpdate] whose non-null fields are the ones the user edited. */
fun SongMetadataEntity.toTagUpdate(): TagUpdate {
    return TagUpdate(
        albumArtist = albumArtist,
        year = year,
        genre = genre,
        trackNumber = trackNumber,
        discNumber = discNumber,
        composer = composer,
        publisher = publisher,
        copyright = copyright,
        comment = comment
    )
}

/**
 * Lays [update] over the stored overrides.
 *
 * A `null` field in [update] means "the user did not touch this", so the already-stored override
 * (if any) is kept — otherwise editing one field would quietly drop the others. An empty string
 * means "clear the override", so the value read from the file shows through again.
 */
fun TagUpdate.mergeWith(update: TagUpdate): TagUpdate {
    return TagUpdate(
        albumArtist = mergeOverride(albumArtist, update.albumArtist),
        year = mergeOverride(year, update.year),
        genre = mergeOverride(genre, update.genre),
        trackNumber = mergeOverride(trackNumber, update.trackNumber),
        discNumber = mergeOverride(discNumber, update.discNumber),
        composer = mergeOverride(composer, update.composer),
        publisher = mergeOverride(publisher, update.publisher),
        copyright = mergeOverride(copyright, update.copyright),
        comment = mergeOverride(comment, update.comment)
    )
}

private fun mergeOverride(stored: String?, update: String?): String? = when {
    update == null -> stored
    update.isBlank() -> null
    else -> update
}

fun TagUpdate.toSongMetadataEntity(
    songId: Long,
    updatedAt: Long = System.currentTimeMillis()
): SongMetadataEntity {
    return SongMetadataEntity(
        songId = songId,
        albumArtist = albumArtist,
        year = year,
        genre = genre,
        trackNumber = trackNumber,
        discNumber = discNumber,
        composer = composer,
        publisher = publisher,
        copyright = copyright,
        comment = comment,
        updatedAt = updatedAt
    )
}
