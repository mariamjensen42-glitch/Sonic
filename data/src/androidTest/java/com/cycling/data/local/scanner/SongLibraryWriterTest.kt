package com.cycling.data.local.scanner

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cycling.data.local.dao.SongDao
import com.cycling.data.local.database.AppDatabase
import com.cycling.data.local.entity.PlaylistEntity
import com.cycling.data.local.entity.SongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Acceptance tests for the rescan data-loss bug.
 *
 * Before [SongLibraryWriter], a scan ran `deleteAllSongs()` and re-inserted. `PRAGMA
 * foreign_keys` is ON (Room sets it in `onOpen`) and `playlist_songs.songId` cascades, so every
 * scan silently emptied every playlist — and reset `isFavorite` / `playCount` / `lastPlayedAt`
 * on the way. These tests run against a real Room database and fail loudly if either returns.
 */
@RunWith(AndroidJUnit4::class)
class SongLibraryWriterTest {

    private lateinit var database: AppDatabase
    private lateinit var writer: SongLibraryWriter

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        writer = SongLibraryWriter(database, database.songDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun rescanRefreshesMetadataButKeepsUserData() = runTest {
        val songDao = database.songDao()
        seed(songDao, id = 1L, title = "Old Title")
        songDao.updateFavorite(1L, true)
        songDao.incrementPlayCount(1L, lastPlayedAt = 1_111L)
        songDao.updateRating(1L, 5)
        songDao.updateCustomAlbumArt(1L, "file:///covers/1.jpg")

        // The same song as MediaStore reports it on the next scan: same id, changed title.
        writer.write(listOf(song(id = 1L, title = "New Title")))

        val row = requireNotNull(songDao.getSongById(1L))
        assertEquals("New Title", row.title)
        assertTrue("favourite was wiped by the rescan", row.isFavorite)
        assertEquals("play count was wiped by the rescan", 1, row.playCount)
        assertEquals(1_111L, row.lastPlayedAt)
        assertEquals("rating was wiped by the rescan", 5, row.rating)
        assertEquals("custom cover was wiped by the rescan", "file:///covers/1.jpg", row.customAlbumArt)
    }

    /**
     * The headline regression: a rescan used to empty every playlist.
     */
    @Test
    fun rescanKeepsPlaylistEntriesIntact() = runTest {
        val songDao = database.songDao()
        val playlistDao = database.playlistDao()
        seed(songDao, id = 1L, title = "Song")

        val playlistId = playlistDao.insertPlaylist(
            PlaylistEntity(name = "My List", dateAdded = 1L, dateModified = 1L)
        )
        playlistDao.addSongToPlaylist(playlistId, 1L)

        writer.write(listOf(song(id = 1L, title = "Song")))

        assertEquals(
            "the rescan emptied the playlist",
            1,
            playlistDao.getSongsInPlaylist(playlistId).first().size
        )
    }

    @Test
    fun rescanPrunesSongsWhoseFileIsGone() = runTest {
        val songDao = database.songDao()
        seed(songDao, id = 1L, title = "Stays")
        seed(songDao, id = 2L, title = "Gone")

        writer.write(listOf(song(id = 1L, title = "Stays")))

        assertNotNull(songDao.getSongById(1L))
        assertNull("removed file was not pruned", songDao.getSongById(2L))
    }

    @Test
    fun rescanPreservesTheOriginalDateAdded() = runTest {
        val songDao = database.songDao()
        seed(songDao, id = 1L, title = "Song", dateAdded = 42L)

        // MediaStore can report a different DATE_ADDED; the first sighting is the meaningful one
        // for "recently added" ordering, so the column is deliberately not in the upsert.
        writer.write(listOf(song(id = 1L, title = "Song", dateAdded = 9_999L)))

        assertEquals(42L, requireNotNull(songDao.getSongById(1L)).dateAdded)
    }

    private suspend fun seed(songDao: SongDao, id: Long, title: String, dateAdded: Long = 0L) {
        songDao.insertSongs(listOf(song(id = id, title = title, dateAdded = dateAdded)))
    }

    private fun song(id: Long, title: String, dateAdded: Long = 0L) = SongEntity(
        id = id,
        title = title,
        artist = "Artist",
        album = "Album",
        duration = 180_000L,
        path = "/storage/emulated/0/Music/$id.mp3",
        albumId = 1L,
        artistId = 1L,
        dateAdded = dateAdded,
        dateModified = 0L,
        size = 5_000_000L,
        mimeType = "audio/mpeg"
    )
}
