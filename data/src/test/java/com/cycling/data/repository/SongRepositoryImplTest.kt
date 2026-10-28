package com.cycling.data.repository

import com.cycling.data.local.dao.LyricsIndexDao
import com.cycling.data.local.dao.SongDao
import com.cycling.data.local.dao.SongStateDao
import com.cycling.data.local.entity.SongEntity
import com.cycling.data.local.entity.SongStateEntity
import com.cycling.data.local.mediastore.MediaStoreHelper
import com.cycling.data.local.saf.SafFolderBrowser
import com.cycling.data.local.scanner.SongLibraryWriter
import com.cycling.domain.repository.ExcludedFolderRepository
import com.cycling.domain.repository.RecordingFilterRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SongRepositoryImplTest {

    private lateinit var songDao: SongDao
    private lateinit var songStateDao: SongStateDao
    private lateinit var lyricsIndexDao: LyricsIndexDao
    private lateinit var mediaStoreHelper: MediaStoreHelper
    private lateinit var songLibraryWriter: SongLibraryWriter
    private lateinit var excludedFolderRepository: ExcludedFolderRepository
    private lateinit var recordingFilterRepository: RecordingFilterRepository
    private lateinit var safFolderBrowser: SafFolderBrowser
    private lateinit var songRepository: SongRepositoryImpl

    @Before
    fun setup() {
        songDao = mockk()
        songStateDao = mockk()
        lyricsIndexDao = mockk()
        mediaStoreHelper = mockk()
        songLibraryWriter = mockk()
        excludedFolderRepository = mockk()
        recordingFilterRepository = mockk()
        coEvery { recordingFilterRepository.isExcludeRecordingsEnabled() } returns false
        safFolderBrowser = mockk()
        songRepository = SongRepositoryImpl(
            songDao,
            songStateDao,
            lyricsIndexDao,
            mediaStoreHelper,
            songLibraryWriter,
            excludedFolderRepository,
            recordingFilterRepository,
            safFolderBrowser
        )
    }

    @Test
    fun `a song that was never nudged has no lyrics offset`() = runTest {
        coEvery { songStateDao.get(1L) } returns null

        assertEquals(0L, songRepository.getLyricsOffset(1L))
    }

    @Test
    fun `a stored lyrics offset is read back`() = runTest {
        coEvery { songStateDao.get(1L) } returns SongStateEntity(songId = 1L, lyricsOffsetMs = -250L)

        assertEquals(-250L, songRepository.getLyricsOffset(1L))
    }

    @Test
    fun `a new lyrics offset is written to the song state row`() = runTest {
        coEvery { songStateDao.upsertLyricsOffset(any(), any(), any()) } returns Unit

        songRepository.setLyricsOffset(1L, -250L)

        coVerify(exactly = 1) { songStateDao.upsertLyricsOffset(1L, -250L, any()) }
    }

    private fun createSongEntity(
        id: Long = 1L,
        title: String = "Test Song",
        artist: String = "Test Artist",
        album: String = "Test Album",
        duration: Long = 180000L,
        path: String = "/storage/emulated/0/Music/test.mp3",
        albumId: Long = 1L,
        artistId: Long = 1L,
        dateAdded: Long = System.currentTimeMillis(),
        dateModified: Long = System.currentTimeMillis(),
        size: Long = 5000000L,
        mimeType: String = "audio/mpeg",
        albumArt: String? = null,
        isFavorite: Boolean = false,
        playCount: Int = 0,
        lastPlayedAt: Long? = null,
        bitrate: Int = 320
    ): SongEntity = SongEntity(
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
        bitrate = bitrate
    )

    @Test
    fun `getAllSongs returns mapped songs from dao`() = runTest {
        val entities = listOf(
            createSongEntity(id = 1, title = "Song 1"),
            createSongEntity(id = 2, title = "Song 2")
        )
        every { songDao.getAllSongs() } returns flowOf(entities)

        val result = songRepository.getAllSongs().first()

        assertEquals(2, result.size)
        assertEquals("Song 1", result[0].title)
        assertEquals("Song 2", result[1].title)
    }

    @Test
    fun `getAllSongs returns empty list when no songs`() = runTest {
        every { songDao.getAllSongs() } returns flowOf(emptyList())

        val result = songRepository.getAllSongs().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getSongById returns mapped song when found`() = runTest {
        val entity = createSongEntity(id = 1, title = "Found Song")
        coEvery { songDao.getSongById(1) } returns entity

        val result = songRepository.getSongById(1)

        assertEquals("Found Song", result?.title)
    }

    @Test
    fun `getSongById returns null when not found`() = runTest {
        coEvery { songDao.getSongById(999) } returns null

        val result = songRepository.getSongById(999)

        assertNull(result)
    }

    @Test
    fun `getSongsByAlbum returns mapped songs`() = runTest {
        val albumId = 1L
        val entities = listOf(
            createSongEntity(id = 1, albumId = albumId, title = "Album Song 1"),
            createSongEntity(id = 2, albumId = albumId, title = "Album Song 2")
        )
        every { songDao.getSongsByAlbum(albumId) } returns flowOf(entities)

        val result = songRepository.getSongsByAlbum(albumId).first()

        assertEquals(2, result.size)
        assertEquals(albumId, result[0].albumId)
    }

    @Test
    fun `getSongsByArtist returns mapped songs`() = runTest {
        val artistId = 1L
        val entities = listOf(
            createSongEntity(id = 1, artistId = artistId, title = "Artist Song 1")
        )
        every { songDao.getSongsByArtist(artistId) } returns flowOf(entities)

        val result = songRepository.getSongsByArtist(artistId).first()

        assertEquals(1, result.size)
        assertEquals(artistId, result[0].artistId)
    }

    @Test
    fun `searchSongs returns matching songs`() = runTest {
        val query = "test"
        val entities = listOf(
            createSongEntity(id = 1, title = "Test Song")
        )
        every { songDao.searchSongs(query) } returns flowOf(entities)

        val result = songRepository.searchSongs(query).first()

        assertEquals(1, result.size)
        assertEquals("Test Song", result[0].title)
    }

    @Test
    fun `getFavoriteSongs returns only favorites`() = runTest {
        val entities = listOf(
            createSongEntity(id = 1, title = "Favorite Song", isFavorite = true)
        )
        every { songDao.getFavoriteSongs() } returns flowOf(entities)

        val result = songRepository.getFavoriteSongs().first()

        assertEquals(1, result.size)
        assertTrue(result[0].isFavorite)
    }

    @Test
    fun `getMostPlayedSongs returns songs ordered by play count`() = runTest {
        val entities = listOf(
            createSongEntity(id = 1, title = "Popular Song", playCount = 100)
        )
        every { songDao.getMostPlayedSongs() } returns flowOf(entities)

        val result = songRepository.getMostPlayedSongs().first()

        assertEquals(1, result.size)
        assertEquals(100, result[0].playCount)
    }

    @Test
    fun `getRecentlyPlayedSongs returns songs with lastPlayedAt`() = runTest {
        val entities = listOf(
            createSongEntity(id = 1, title = "Recent Song", lastPlayedAt = System.currentTimeMillis())
        )
        every { songDao.getRecentlyPlayedSongs() } returns flowOf(entities)

        val result = songRepository.getRecentlyPlayedSongs().first()

        assertEquals(1, result.size)
        assertTrue(result[0].lastPlayedAt != null)
    }

    @Test
    fun `toggleFavorite returns new favorite status when song exists`() = runTest {
        val songId = 1L
        val entity = createSongEntity(id = songId, isFavorite = false)
        coEvery { songDao.getSongById(songId) } returns entity
        coEvery { songDao.updateFavorite(songId, true) } returns Unit

        val result = songRepository.toggleFavorite(songId)

        assertTrue(result)
        coVerify { songDao.updateFavorite(songId, true) }
    }

    @Test
    fun `toggleFavorite returns false when song not found`() = runTest {
        val songId = 999L
        coEvery { songDao.getSongById(songId) } returns null

        val result = songRepository.toggleFavorite(songId)

        assertFalse(result)
    }

    @Test
    fun `toggleFavorite toggles from true to false`() = runTest {
        val songId = 1L
        val entity = createSongEntity(id = songId, isFavorite = true)
        coEvery { songDao.getSongById(songId) } returns entity
        coEvery { songDao.updateFavorite(songId, false) } returns Unit

        val result = songRepository.toggleFavorite(songId)

        assertFalse(result)
        coVerify { songDao.updateFavorite(songId, false) }
    }

    @Test
    fun `incrementPlayCount calls dao with correct parameters`() = runTest {
        val songId = 1L
        coEvery { songDao.incrementPlayCount(eq(songId), any()) } returns Unit

        songRepository.incrementPlayCount(songId)

        coVerify { songDao.incrementPlayCount(eq(songId), any()) }
    }

    @Test
    fun `getSongCount returns count from dao`() = runTest {
        coEvery { songDao.getSongCount() } returns 42

        val result = songRepository.getSongCount()

        assertEquals(42, result)
    }

    /**
     * Regression guard for the second destructive write path.
     *
     * `refreshSongs` used to do `deleteAllSongs()` + a bulk re-insert, which wiped favourites,
     * play counts, and — through the `playlist_songs` cascade — every playlist. It must now go
     * through [SongLibraryWriter] and must never delete the table wholesale.
     */
    @Test
    fun `refreshSongs delegates to the writer and never wipes the table`() = runTest {
        val excluded = listOf("/storage/emulated/0/Podcasts")
        coEvery { excludedFolderRepository.getExcludedPaths() } returns excluded
        every { mediaStoreHelper.queryAllSongs(excluded, false) } returns listOf(createSong())
        coEvery { songLibraryWriter.write(any(), any()) } returns 1

        songRepository.refreshSongs()

        coVerify(exactly = 1) { songLibraryWriter.write(any(), any()) }
        coVerify(exactly = 0) { songDao.deleteAllSongs() }
        coVerify(exactly = 0) { songDao.insertSongs(any()) }
    }

    @Test
    fun `refreshSongs respects excluded folders`() = runTest {
        val excluded = listOf("/storage/emulated/0/Podcasts")
        coEvery { excludedFolderRepository.getExcludedPaths() } returns excluded
        every { mediaStoreHelper.queryAllSongs(excluded, false) } returns emptyList()
        coEvery { songLibraryWriter.write(any(), any()) } returns 0

        songRepository.refreshSongs()

        coVerify(exactly = 1) { mediaStoreHelper.queryAllSongs(excluded, false) }
    }

    /** 「排除录音」开关必须一路传到 MediaStore 查询里，否则开关只是存了个偏好。 */
    @Test
    fun `refreshSongs forwards the exclude recordings flag`() = runTest {
        val excluded = listOf("/storage/emulated/0/Podcasts")
        coEvery { excludedFolderRepository.getExcludedPaths() } returns excluded
        coEvery { recordingFilterRepository.isExcludeRecordingsEnabled() } returns true
        every { mediaStoreHelper.queryAllSongs(excluded, true) } returns emptyList()
        coEvery { songLibraryWriter.write(any(), any()) } returns 0

        songRepository.refreshSongs()

        coVerify(exactly = 1) { mediaStoreHelper.queryAllSongs(excluded, true) }
    }

    /**
     * 媒体库目录树：旧实现以空串为根，绝对路径 split("/") 的首元素是空串，
     * 根层会塌成一个无名目录；现在树从 "/" 开始。这两条用例把两个行为都钉住。
     */
    @Test
    fun `library folder root lists the storage root, not a nameless folder`() = runTest {
        coEvery { songDao.getAllSongPaths() } returns listOf(
            "/storage/emulated/0/Music/Rock/a.mp3",
            "/storage/emulated/0/Music/Jazz/b.mp3",
            "/storage/emulated/0/Podcasts/p.mp3"
        )

        val content = songRepository.getLibraryFolderContent("")

        assertEquals(1, content.folders.size)
        assertEquals("storage", content.folders[0].name)
        assertEquals("/storage", content.folders[0].path)
        assertEquals(3, content.folders[0].songCount)
        assertNull(content.parentPath)
    }

    @Test
    fun `a sibling folder sharing a name prefix is not leaked into its neighbor`() = runTest {
        coEvery { songDao.getAllSongPaths() } returns listOf(
            "/storage/emulated/0/Music/rock/a.mp3",
            "/storage/emulated/0/Music2/live/b.mp3"
        )
        coEvery { songDao.getSongsByPathPrefix("/storage/emulated/0/") } returns emptyList()

        val content = songRepository.getLibraryFolderContent("/storage/emulated/0")

        assertEquals(
            listOf("/storage/emulated/0/Music", "/storage/emulated/0/Music2"),
            content.folders.map { it.path }
        )
        assertEquals("/storage/emulated", content.parentPath)
    }

    @Test
    fun `a library folder lists direct subfolders and direct songs only`() = runTest {
        coEvery { songDao.getAllSongPaths() } returns listOf(
            "/storage/emulated/0/Music/direct.mp3",
            "/storage/emulated/0/Music/nested/deep.mp3"
        )
        coEvery {
            songDao.getSongsByPathPrefix("/storage/emulated/0/Music/")
        } returns listOf(
            createSongEntity(path = "/storage/emulated/0/Music/direct.mp3"),
            createSongEntity(path = "/storage/emulated/0/Music/nested/deep.mp3")
        )

        val content = songRepository.getLibraryFolderContent("/storage/emulated/0/Music")

        assertEquals(listOf("/storage/emulated/0/Music/nested"), content.folders.map { it.path })
        assertEquals(listOf("direct.mp3"), content.songs.map { it.path.substringAfterLast('/') })
        assertEquals(listOf("/storage/emulated/0/Music/direct.mp3"), content.songs.map { it.path })
    }

    private fun createSong(
        id: Long = 1L,
        path: String = "/storage/emulated/0/Music/test.mp3"
    ) = com.cycling.domain.model.Song(
        id = id,
        title = "Test Song",
        artist = "Test Artist",
        album = "Test Album",
        duration = 180000L,
        path = path,
        albumId = 1L,
        artistId = 1L,
        dateAdded = 1L,
        dateModified = 1L,
        size = 5000000L,
        mimeType = "audio/mpeg"
    )
}
