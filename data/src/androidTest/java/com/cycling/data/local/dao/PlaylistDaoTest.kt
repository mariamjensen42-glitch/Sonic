package com.cycling.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cycling.data.local.database.AppDatabase
import com.cycling.data.local.entity.PlaylistEntity
import com.cycling.data.local.entity.PlaylistSongEntity
import com.cycling.data.local.entity.SongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 一首歌在一张歌单里只能有一行。
 *
 * 之前插入从不查重，"添加到歌单"点两次就能把同一首歌写进 `playlist_songs` 两遍；歌单详情页
 * 拿 `song.id` 当 LazyColumn 的 key，重复行会让 Compose 抛 "Key ... was already used"
 * —— 进歌单直接闪退。这些用例跑在真 Room 库上，锁住 DAO 的查重与唯一索引两条防线。
 */
@RunWith(AndroidJUnit4::class)
class PlaylistDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var playlistDao: PlaylistDao
    private var playlistId: Long = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        playlistDao = database.playlistDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addingTheSameSongTwiceKeepsOneRow() = runTest {
        seed()

        assertEquals("first add writes one row", 1, playlistDao.addSongToPlaylist(playlistId, 1L))
        assertEquals("second add must write nothing", 0, playlistDao.addSongToPlaylist(playlistId, 1L))

        assertEquals(1, songs().size)
        assertEquals(1L, songs().single().id)
    }

    @Test
    fun batchAddSkipsSongsAlreadyInThePlaylist() = runTest {
        seed()
        playlistDao.addSongToPlaylist(playlistId, 1L)

        // 只有 2 是新的，所以真实写入数是 1 —— 提示里报的就是这个值，不是 2。
        assertEquals(1, playlistDao.addSongsToPlaylist(playlistId, listOf(1L, 2L)))

        assertEquals("already-added song was inserted twice", listOf(1L, 2L), songs().map { it.id })
    }

    @Test
    fun batchAddSkipsDuplicatesWithinOneRequest() = runTest {
        seed()

        assertEquals(2, playlistDao.addSongsToPlaylist(playlistId, listOf(1L, 1L, 2L)))

        assertEquals(listOf(1L, 2L), songs().map { it.id })
    }

    /**
     * 重复添加不能改变已有位置：IGNORE + 队尾追加若写成 REPLACE，会把这首歌从原位删掉再
     * 插到最后，用户看到的是"加了一遍，顺序全乱"。
     */
    @Test
    fun addingAnExistingSongAgainDoesNotMoveIt() = runTest {
        seed()
        playlistDao.addSongsToPlaylist(playlistId, listOf(1L, 2L, 3L))

        assertEquals(0, playlistDao.addSongToPlaylist(playlistId, 1L))

        assertEquals(listOf(1L, 2L, 3L), songs().map { it.id })
    }

    /** 唯一索引是最后一道防线：绕过 DAO 的查重直接插重复行也必须被库挡住。 */
    @Test
    fun uniqueIndexRejectsARawDuplicateRow() = runTest {
        seed()
        playlistDao.addSongToPlaylist(playlistId, 1L)

        playlistDao.insertPlaylistSong(
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = 1L,
                position = 99
            )
        )

        assertEquals(1, songs().size)
    }

    private suspend fun seed() {
        database.songDao().insertSongs((1L..3L).map { song(it) })
        playlistId = playlistDao.insertPlaylist(
            PlaylistEntity(name = "My List", dateAdded = 1L, dateModified = 1L)
        )
    }

    private suspend fun songs() = playlistDao.getSongsInPlaylist(playlistId).first()

    private fun song(id: Long) = SongEntity(
        id = id,
        title = "Song $id",
        artist = "Artist",
        album = "Album",
        duration = 180_000L,
        path = "/storage/emulated/0/Music/$id.mp3",
        albumId = 1L,
        artistId = 1L,
        dateAdded = 0L,
        dateModified = 0L,
        size = 5_000_000L,
        mimeType = "audio/mpeg"
    )
}
