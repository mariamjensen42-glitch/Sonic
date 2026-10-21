package com.cycling.data.player.engine

import com.cycling.domain.model.RepeatMode
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * One entry in the playback queue, expressed in this module's own types.
 *
 * Deliberately not a media3 `MediaItem`: keeping the abstraction free of media3 types is what
 * lets [FakePlaybackEngine] stand in for ExoPlayer in JVM unit tests, so queue behaviour can be
 * tested without a device, a service, or a Binder.
 */
data class QueueItem(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    /**
     * Artwork URI handed to media3 for the notification and lock screen.
     *
     * Never the artwork *bytes*: a queue is marshalled over Binder (controller → session, and
     * session → notification/Auto), and a few thousand items carrying downscaled JPEGs blows the
     * ~1 MB transaction limit with `TransactionTooLargeException`.
     */
    val artworkUri: String?
)

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    READY,
    ENDED
}

/** Why the current item changed; drives the play-count rule in `PlayerManager`. */
enum class TransitionReason {
    AUTO,
    SEEK,
    REPEAT,
    PLAYLIST_CHANGED,
    UNKNOWN
}

/**
 * A queue snapshot. Everything [com.cycling.data.player.PlayerManager] derives its public state
 * from lives here.
 */
data class EngineState(
    val queueIds: List<Long> = emptyList(),
    val currentIndex: Int = -1,
    val currentId: Long? = null,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val speed: Float = 1f,
    /**
     * The play/pause *intent*, as opposed to [isPlaying], which also goes false while buffering.
     *
     * Fade-out needs this: a pause pressed during buffering must still count as a pause, and
     * `isPlaying` cannot tell that apart from "not started yet".
     */
    val playWhenReady: Boolean = false
)

/**
 * Edge-triggered notifications.
 *
 * Separate from [PlaybackEngine.state] because these are the cases a state diff cannot express:
 * repeat-one restarts the *same* item, so an index comparison sees nothing change.
 */
sealed interface PlaybackEvent {
    data class ItemChanged(val id: Long?, val reason: TransitionReason) : PlaybackEvent
    data object Ended : PlaybackEvent

    /**
     * 一首歌放不出来：文件损坏、被删掉、或者解码失败。
     *
     * 引擎抛出这个之后会停在 IDLE，**不会自己跳过** —— "坏歌跳过继续放"是队列策略，
     * 属于 [com.cycling.data.player.PlayerManager]，引擎只负责把事实报上来。
     */
    data class Error(val itemId: Long?, val errorCodeName: String?) : PlaybackEvent
}

/**
 * The player, abstracted.
 *
 * Implemented by [ExoPlayerPlaybackEngine] in production. Note that the production engine drives
 * `ExoPlayer` directly rather than a `MediaController`: `setSkipSilenceEnabled` and
 * `setAudioSessionId` exist only on `ExoPlayer`, never on the `Player` interface the controller
 * implements, so skip-silence and every `android.media.audiofx` effect are unreachable through a
 * controller. The service runs in the same process, so a direct reference is available.
 */
interface PlaybackEngine {

    val state: StateFlow<EngineState>

    val events: SharedFlow<PlaybackEvent>

    /** Audio session the effects chain attaches to. */
    val audioSessionId: Int

    /** Replaces the queue and prepares [startIndex] at [startPositionMs]. Does not auto-play. */
    fun setQueue(items: List<QueueItem>, startIndex: Int = 0, startPositionMs: Long = 0L)

    fun addToQueue(item: QueueItem)

    /** Inserts [item] directly after [afterIndex]. */
    fun insertNext(item: QueueItem, afterIndex: Int)

    fun move(fromIndex: Int, toIndex: Int)

    fun remove(index: Int)

    fun clear()

    fun play()

    fun pause()

    fun playPause()

    fun stop()

    fun seekTo(positionMs: Long)

    /** Jumps to an item already in the queue. */
    fun seekToIndex(index: Int, positionMs: Long = 0L)

    fun skipToNext()

    fun skipToPrevious()

    fun setRepeatMode(mode: RepeatMode)

    fun setShuffleEnabled(enabled: Boolean)

    /** Playback rate; pitch is always left at 1.0 so speed does not change key. */
    fun setSpeed(speed: Float)

    fun setSkipSilence(enabled: Boolean)

    fun setVolume(volume: Float)

    /** Polled by the progress loop; position is not part of [state] because it changes every frame. */
    fun currentPositionMs(): Long
}
