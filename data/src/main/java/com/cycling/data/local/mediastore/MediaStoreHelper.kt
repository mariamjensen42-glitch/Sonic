package com.cycling.data.local.mediastore

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import com.cycling.domain.model.Album
import com.cycling.domain.model.Artist
import com.cycling.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun queryAllSongs(excludedPaths: List<String> = emptyList()): List<Song> {
        Timber.d("queryAllSongs: starting query, excludedPaths = $excludedPaths")
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = ? AND ${MediaStore.Audio.Media.DURATION} > ?"
        val selectionArgs = arrayOf("1", MIN_DURATION.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val song = cursor.toSong()
                val isExcluded = excludedPaths.any { excludedPath ->
                    song.path.startsWith(excludedPath)
                }
                if (!isExcluded) {
                    songs.add(song)
                }
            }
        }
        Timber.d("queryAllSongs: found ${songs.size} songs")

        return songs
    }

    fun queryAllAlbums(): List<Album> {
        Timber.d("queryAllAlbums: starting query")
        val albums = mutableListOf<Album>()
        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
            MediaStore.Audio.Albums.FIRST_YEAR,
            MediaStore.Audio.Albums.LAST_YEAR
        )

        val sortOrder = "${MediaStore.Audio.Albums.ALBUM} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                albums.add(cursor.toAlbum())
            }
        }
        Timber.d("queryAllAlbums: found ${albums.size} albums")

        return albums
    }

    fun queryAllArtists(): List<Artist> {
        Timber.d("queryAllArtists: starting query")
        val artists = mutableListOf<Artist>()
        val projection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS
        )

        val sortOrder = "${MediaStore.Audio.Artists.ARTIST} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val artist = cursor.toArtist()
                val artistArt = queryArtistArt(artist.id)
                artists.add(artist.copy(artistArt = artistArt))
            }
        }
        Timber.d("queryAllArtists: found ${artists.size} artists")

        return artists
    }

    private fun queryArtistArt(artistId: Long): String? {
        val projection = arrayOf(
            MediaStore.Audio.Albums._ID
        )

        val selection = "${MediaStore.Audio.Albums.ARTIST_ID} = ?"
        val selectionArgs = arrayOf(artistId.toString())
        val sortOrder = "${MediaStore.Audio.Albums.ALBUM} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID))
                return ContentUris.withAppendedId(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    albumId
                ).toString()
            }
        }

        return null
    }

    fun querySongsByAlbum(albumId: Long): List<Song> {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = ? AND ${MediaStore.Audio.Media.DURATION} > ? AND ${MediaStore.Audio.Media.ALBUM_ID} = ?"
        val selectionArgs = arrayOf("1", MIN_DURATION.toString(), albumId.toString())
        val sortOrder = "${MediaStore.Audio.Media.TRACK} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                songs.add(cursor.toSong())
            }
        }

        return songs
    }

    fun querySongsByArtist(artistId: Long): List<Song> {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = ? AND ${MediaStore.Audio.Media.DURATION} > ? AND ${MediaStore.Audio.Media.ARTIST_ID} = ?"
        val selectionArgs = arrayOf("1", MIN_DURATION.toString(), artistId.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                songs.add(cursor.toSong())
            }
        }

        return songs
    }

    fun searchSongs(query: String): List<Song> {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = ? AND ${MediaStore.Audio.Media.DURATION} > ? AND (${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ? OR ${MediaStore.Audio.Media.ALBUM} LIKE ?)"
        val searchQuery = "%$query%"
        val selectionArgs = arrayOf("1", MIN_DURATION.toString(), searchQuery, searchQuery, searchQuery)
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                songs.add(cursor.toSong())
            }
        }

        return songs
    }

    private fun Cursor.toSong(): Song {
        val albumId = getLong(getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
        val albumArtUri = ContentUris.withAppendedId(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            albumId
        ).toString()
        val path = getString(getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)) ?: ""
        
        return Song(
            id = getLong(getColumnIndexOrThrow(MediaStore.Audio.Media._ID)),
            title = getString(getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)) ?: UNKNOWN,
            artist = getString(getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)) ?: UNKNOWN,
            album = getString(getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)) ?: UNKNOWN,
            duration = getLong(getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)),
            path = path,
            albumId = albumId,
            artistId = getLong(getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)),
            dateAdded = getLong(getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)),
            dateModified = getLong(getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)),
            size = getLong(getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)),
            mimeType = getString(getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)) ?: "",
            albumArt = albumArtUri,
            bitrate = 0
        )
    }

    fun extractBitrate(path: String): Int {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            retriever.release()
            bitrate?.toIntOrNull()?.div(1000) ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun Cursor.toAlbum(): Album {
        val albumId = getLong(getColumnIndexOrThrow(MediaStore.Audio.Albums._ID))
        val albumArtUri = ContentUris.withAppendedId(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            albumId
        ).toString()
        
        return Album(
            id = albumId,
            name = getString(getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)) ?: UNKNOWN,
            artist = getString(getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)) ?: UNKNOWN,
            albumArt = albumArtUri,
            numberOfSongs = getInt(getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)),
            firstYear = getInt(getColumnIndexOrThrow(MediaStore.Audio.Albums.FIRST_YEAR)),
            lastYear = getInt(getColumnIndexOrThrow(MediaStore.Audio.Albums.LAST_YEAR))
        )
    }

    private fun Cursor.toArtist(): Artist {
        return Artist(
            id = getLong(getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)),
            name = getString(getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)) ?: UNKNOWN,
            numberOfAlbums = getInt(getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)),
            numberOfTracks = getInt(getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS))
        )
    }

    companion object {
        private const val MIN_DURATION = 30000L
        private const val UNKNOWN = "<unknown>"
    }
}
