package com.cycling.data.player

import com.cycling.data.player.engine.TransitionReason
import com.cycling.data.store.PlaybackStateStore
import com.cycling.data.util.TestDispatcherRule
import com.cycling.domain.model.RepeatMode
import com.cycling.domain.model.Song
import com.cycling.domain.repository.AudioMetadataRepository
import com.cycling.domain.repository.SettingsRepository
import com.cycling.domain.repository.SongRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Locks down the queue semantics that used to live in hand-rolled index arithmetic.
 *
 * Note these tests never call `advanceUntilIdle()`: once playback starts, `PlayerManager` runs a
 * `while (isActive) { … delay(1000) }` progress loop, so "advance until nothing is scheduled"
 * never terminates. Use [settle] to run pending work and `advanceTimeBy` to step the loop.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerManagerTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var engine: FakePlaybackEngine
    private lateinit var songRepository: SongRepository
    private lateinit var audioMetadataRepository: AudioMetadataRepository
    private lateinit var playbackStateStore: PlaybackStateStore
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var serviceController: PlaybackServiceController
    private lateinit var playerManager: PlayerManager

    @Before
    fun setUp() {
        engine = FakePlaybackEngine()
        songRepository = mockk(relaxed = true)
        audioMetadataRepository = mockk(relaxed = true)
        playbackStateStore = mockk(relaxed = true)
        settingsRepository = mockk()
        serviceController = mockk(relaxed = true)

        every { settingsRepository.playbackSpeed } returns flowOf(1f)
        every { settingsRepository.skipSilence } returns flowOf(false)
        // 淡入淡出默认关闭、音量均衡默认关闭：队列语义的测试不该被音量渐变的延迟搅进来。
        every { settingsRepository.replayGainEnabled } returns flowOf(false)
        every { settingsRepository.audioFadeDurationMs } returns flowOf(0L)
        every { settingsRepository.rememberPlaybackPosition } returns flowOf(true)
        every { settingsRepository.autoContinueEnabled } returns flowOf(false)
        coEvery { playbackStateStore.getSavedState() } returns null

        playerManager = PlayerManager(
            engine = engine,
            songRepository = songRepository,
            audioMetadataRepository = audioMetadataRepository,
            playbackStateStore = playbackStateStore,
            settingsRepository = settingsRepository,
            getContinuationSongs = mockk(relaxed = true),
            playbackServiceController = serviceController,
            dispatcher = dispatcherRule.testDispatcher
        )
    }

    @Test
    fun `playSong loads the whole queue and starts at the chosen song`() = runTest {
        val songs = listOf(song(1), song(2), song(3))

        playerManager.playSong(songs[1], songs)
        settle()

        val state = playerManager.playerState.value
        assertEquals(listOf(1L, 2L, 3L), state.playbackQueue.map { it.id })
        assertEquals(1, state.queueIndex)
        assertEquals(2L, state.currentSong?.id)
        assertTrue(state.isPlaying)
        verify { serviceController.start() }
    }

    @Test
    fun `playSong with an empty queue keeps whatever is already queued`() = runTest {
        playerManager.playSong(song(1), listOf(song(1), song(2)))
        settle()

        playerManager.playSong(song(2))
        settle()

        val state = playerManager.playerState.value
        assertEquals(listOf(1L, 2L), state.playbackQueue.map { it.id })
        assertEquals(1, state.queueIndex)
    }

    @Test
    fun `skipToNext stops at the end when repeat is off`() = runTest {
        playerManager.playSong(song(1), listOf(song(1), song(2)))
        settle()

        playerManager.skipToNext()
        settle()
        assertEquals(1, playerManager.playerState.value.queueIndex)

        playerManager.skipToNext()
        settle()
        assertEquals("should not wrap past the last item", 1, playerManager.playerState.value.queueIndex)
    }

    @Test
    fun `skipToNext wraps when repeat is all`() = runTest {
        playerManager.playSong(song(1), listOf(song(1), song(2)))
        settle()
        playerManager.setRepeatMode(RepeatMode.ALL)
        settle()

        playerManager.skipToNext()
        settle()
        playerManager.skipToNext()
        settle()

        assertEquals(0, playerManager.playerState.value.queueIndex)
    }

    @Test
    fun `skipToPrevious goes back one item, not to the start of the current one`() = runTest {
        playerManager.playSong(song(3), listOf(song(1), song(2), song(3)))
        settle()

        playerManager.skipToPrevious()
        settle()

        assertEquals(2L, playerManager.playerState.value.currentSong?.id)
    }

    @Test
    fun `playNext inserts directly after the current song`() = runTest {
        playerManager.playSong(song(1), listOf(song(1), song(2)))
        settle()

        playerManager.playNext(song(3))
        settle()

        assertEquals(listOf(1L, 3L, 2L), playerManager.playerState.value.playbackQueue.map { it.id })
    }

    @Test
    fun `removing a song before the current one keeps the current song playing`() = runTest {
        playerManager.playSong(song(2), listOf(song(1), song(2), song(3)))
        settle()

        playerManager.removeFromQueue(0)
        settle()

        val state = playerManager.playerState.value
        assertEquals(listOf(2L, 3L), state.playbackQueue.map { it.id })
        assertEquals(2L, state.currentSong?.id)
        assertEquals(0, state.queueIndex)
    }

    @Test
    fun `removing the last remaining song resets the player state`() = runTest {
        playerManager.playSong(song(1), listOf(song(1)))
        settle()

        playerManager.removeFromQueue(0)
        settle()

        val state = playerManager.playerState.value
        assertNull(state.currentSong)
        assertTrue(state.playbackQueue.isEmpty())
    }

    @Test
    fun `clearQueue resets the player state`() = runTest {
        playerManager.playSong(song(1), listOf(song(1), song(2)))
        settle()

        playerManager.clearQueue()
        settle()

        assertNull(playerManager.playerState.value.currentSong)
        assertTrue(playerManager.playerState.value.playbackQueue.isEmpty())
    }

    /**
     * Regression guard: the queue shown to the UI is rebuilt from the manager's own song lookup on
     * every engine change. If `setSongFavorite` only patched `PlayerState`, the next engine update
     * would revert the heart — the flicker that commit 0966b75 fixed upstream.
     */
    @Test
    fun `favourite state survives a later engine update`() = runTest {
        playerManager.playSong(song(1), listOf(song(1), song(2)))
        settle()

        playerManager.setSongFavorite(1L, true)
        settle()
        assertTrue(playerManager.playerState.value.currentSong?.isFavorite == true)

        // Anything that makes the engine re-publish its state must not lose the flag.
        engine.playPause()
        settle()

        assertTrue(
            "current song lost its favourite flag after an engine update",
            playerManager.playerState.value.currentSong?.isFavorite == true
        )
        assertTrue(
            "queued entry lost its favourite flag after an engine update",
            playerManager.playerState.value.playbackQueue.first { it.id == 1L }.isFavorite
        )
    }

    @Test
    fun `a play is counted once past the halfway point and only once`() = runTest {
        playerManager.playSong(song(1), listOf(song(1)))
        settle()

        engine.positionMs = 10_000L
        stepProgressLoop()
        coVerify(exactly = 0) { songRepository.incrementPlayCount(any()) }

        engine.positionMs = 100_000L
        stepProgressLoop()
        coVerify(exactly = 1) { songRepository.incrementPlayCount(1L) }

        engine.positionMs = 170_000L
        stepProgressLoop()
        coVerify(exactly = 1) { songRepository.incrementPlayCount(any()) }
    }

    /**
     * A playlist edit can fire a transition without the current song changing. Counting that as a
     * new play would inflate play counts and re-stamp `lastPlayedAt`.
     */
    @Test
    fun `a playlist edit that leaves the current item in place is not a new play`() = runTest {
        playerManager.playSong(song(1), listOf(song(1), song(2)))
        settle()
        coVerify(exactly = 1) { songRepository.updateLastPlayedAt(1L) }

        engine.emitTransition(1L, TransitionReason.PLAYLIST_CHANGED)
        settle()

        coVerify(exactly = 1) { songRepository.updateLastPlayedAt(1L) }
    }

    @Test
    fun `auto-advancing stamps the next song as played`() = runTest {
        playerManager.playSong(song(1), listOf(song(1), song(2)))
        settle()

        engine.simulateTrackFinished()
        settle()

        assertEquals(2L, playerManager.playerState.value.currentSong?.id)
        coVerify { songRepository.updateLastPlayedAt(2L) }
    }

    @Test
    fun `repeat one restarts the same song and counts as a fresh play`() = runTest {
        playerManager.playSong(song(1), listOf(song(1)))
        settle()
        playerManager.setRepeatMode(RepeatMode.ONE)
        settle()

        engine.positionMs = 100_000L
        stepProgressLoop()
        coVerify(exactly = 1) { songRepository.incrementPlayCount(1L) }

        engine.simulateTrackFinished()
        settle()
        engine.positionMs = 100_000L
        stepProgressLoop()

        coVerify(exactly = 2) { songRepository.incrementPlayCount(1L) }
    }

    @Test
    fun `reaching the end of the queue stops playback`() = runTest {
        playerManager.playSong(song(1), listOf(song(1)))
        settle()

        engine.simulateTrackFinished()
        settle()

        assertFalse(playerManager.playerState.value.isPlaying)
    }

    @Test
    fun `connect restores the saved queue without auto-playing`() = runTest {
        coEvery { playbackStateStore.getSavedState() } returns PlaybackStateStore.SavedState(
            queueSongIds = listOf(1L, 2L),
            currentIndex = 1,
            playbackPosition = 5_000L,
            repeatMode = RepeatMode.ALL,
            shuffleMode = true,
            wasPlaying = false
        )
        coEvery { songRepository.getSongById(1L) } returns song(1)
        coEvery { songRepository.getSongById(2L) } returns song(2)

        playerManager.connect()
        settle()

        val state = playerManager.playerState.value
        assertEquals(listOf(1L, 2L), state.playbackQueue.map { it.id })
        assertEquals(1, state.queueIndex)
        assertEquals(5_000L, state.playbackPosition)
        assertEquals(RepeatMode.ALL, state.repeatMode)
        assertTrue(state.shuffleMode)
        assertFalse("wasPlaying=false must not resume playback", state.isPlaying)
        verify(exactly = 0) { serviceController.start() }
    }

    @Test
    fun `connect starts the playback service when it resumes playback`() = runTest {
        coEvery { playbackStateStore.getSavedState() } returns PlaybackStateStore.SavedState(
            queueSongIds = listOf(1L),
            currentIndex = 0,
            playbackPosition = 0L,
            repeatMode = RepeatMode.OFF,
            shuffleMode = false,
            wasPlaying = true
        )
        coEvery { songRepository.getSongById(1L) } returns song(1)

        playerManager.connect()
        settle()

        assertTrue(playerManager.playerState.value.isPlaying)
        verify { serviceController.start() }
    }

    /**
     * Regression guard for the launch crash. `MainViewModel.init` used to call `connect()`, which
     * started the service on *every* cold start, including ones with nothing to play. media3 only
     * takes a `MediaSessionService` to the foreground while its player is playing, so when nothing
     * was restored `startForeground()` never happened and Android killed the process about five
     * seconds in with `ForegroundServiceDidNotStartInTimeException`.
     */
    @Test
    fun `connect with nothing to restore starts no service`() = runTest {
        playerManager.connect()
        settle()

        verify(exactly = 0) { serviceController.start() }
    }

    @Test
    fun `resuming after a paused restore starts the playback service`() = runTest {
        coEvery { playbackStateStore.getSavedState() } returns PlaybackStateStore.SavedState(
            queueSongIds = listOf(1L),
            currentIndex = 0,
            playbackPosition = 0L,
            repeatMode = RepeatMode.OFF,
            shuffleMode = false,
            wasPlaying = false
        )
        coEvery { songRepository.getSongById(1L) } returns song(1)
        playerManager.connect()
        settle()
        verify(exactly = 0) { serviceController.start() }

        playerManager.playPause()
        settle()

        assertTrue(playerManager.playerState.value.isPlaying)
        verify { serviceController.start() }
    }

    @Test
    fun `connect clears saved state when none of the songs exist any more`() = runTest {
        coEvery { playbackStateStore.getSavedState() } returns PlaybackStateStore.SavedState(
            queueSongIds = listOf(1L, 2L),
            currentIndex = 0,
            playbackPosition = 0L,
            repeatMode = RepeatMode.OFF,
            shuffleMode = false,
            wasPlaying = true
        )
        coEvery { songRepository.getSongById(any()) } returns null

        playerManager.connect()
        settle()

        assertNull(playerManager.playerState.value.currentSong)
        coVerify { playbackStateStore.clear() }
        verify(exactly = 0) { serviceController.start() }
    }

    @Test
    fun `playPause toggles the engine`() = runTest {
        playerManager.playSong(song(1), listOf(song(1)))
        settle()
        assertTrue(playerManager.playerState.value.isPlaying)

        playerManager.playPause()
        settle()
        assertFalse(playerManager.playerState.value.isPlaying)

        playerManager.playPause()
        settle()
        assertTrue(playerManager.playerState.value.isPlaying)
    }

    @Test
    fun `seekTo moves the reported position`() = runTest {
        playerManager.playSong(song(1), listOf(song(1)))
        settle()

        playerManager.seekTo(42_000L)
        settle()

        assertEquals(42_000L, playerManager.playerState.value.playbackPosition)
    }

    @Test
    fun `repeat and shuffle changes are persisted`() = runTest {
        playerManager.setRepeatMode(RepeatMode.ONE)
        playerManager.setShuffleMode(true)
        settle()

        coVerify { playbackStateStore.saveRepeatMode(RepeatMode.ONE) }
        coVerify { playbackStateStore.saveShuffleMode(true) }
    }

    /** Runs everything the manager has queued without advancing virtual time. */
    private fun kotlinx.coroutines.test.TestScope.settle() {
        repeat(3) { runCurrent() }
    }

    /** Advances virtual time past one progress-loop tick. */
    private fun kotlinx.coroutines.test.TestScope.stepProgressLoop() {
        advanceTimeBy(1_001L)
        settle()
    }

    private fun song(id: Long, title: String = "Song $id") = Song(
        id = id,
        title = title,
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
