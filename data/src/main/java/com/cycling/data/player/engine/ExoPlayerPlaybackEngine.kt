package com.cycling.data.player.engine

import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.cycling.domain.model.RepeatMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [PlaybackEngine] over a directly-held [ExoPlayer].
 *
 * Why not a `MediaController`: `setSkipSilenceEnabled` and `setAudioSessionId` exist only on
 * `ExoPlayer`, so a controller can never enable skip-silence or reach the `audiofx` effects, and
 * volume ramps would cost one Binder transaction per frame. `PlayerService` is declared without
 * `android:process`, so the service and the UI share this instance — which is what makes the
 * direct reference correct rather than a shortcut.
 *
 * Every `ExoPlayer` call must happen on the application looper, so each method funnels through
 * [runOnMain] instead of trusting callers. Operations are therefore queued in order but may not
 * have taken effect by the time a call returns; read [state] rather than the player.
 */
@Singleton
@OptIn(UnstableApi::class)
class ExoPlayerPlaybackEngine @Inject constructor(
    private val exoPlayer: ExoPlayer
) : PlaybackEngine {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(EngineState())
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    // Buffered so the listener never blocks the player's looper. The collector does no suspending
    // work before launching its coroutines, so this only fills if the main thread is wedged.
    private val _events = MutableSharedFlow<PlaybackEvent>(
        extraBufferCapacity = 32,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    override val events: SharedFlow<PlaybackEvent> = _events.asSharedFlow()

    override val audioSessionId: Int
        get() = exoPlayer.audioSessionId

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publishState(player)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _events.tryEmit(
                PlaybackEvent.ItemChanged(
                    id = mediaItem?.mediaId?.toLongOrNull(),
                    reason = reason.toTransitionReason()
                )
            )
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                _events.tryEmit(PlaybackEvent.Ended)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _events.tryEmit(
                PlaybackEvent.Error(
                    itemId = exoPlayer.currentMediaItem?.mediaId?.toLongOrNull(),
                    errorCodeName = error.errorCodeName
                )
            )
        }
    }

    init {
        exoPlayer.addListener(listener)
        publishState(exoPlayer)
    }

    override fun setQueue(items: List<QueueItem>, startIndex: Int, startPositionMs: Long) = runOnMain {
        if (items.isEmpty()) {
            exoPlayer.clearMediaItems()
            return@runOnMain
        }
        val index = startIndex.coerceIn(0, items.lastIndex)
        exoPlayer.setMediaItems(items.map(::toMediaItem), index, startPositionMs)
        exoPlayer.prepare()
    }

    override fun addToQueue(item: QueueItem) = runOnMain {
        exoPlayer.addMediaItem(toMediaItem(item))
    }

    override fun insertNext(item: QueueItem, afterIndex: Int) = runOnMain {
        val at = (afterIndex + 1).coerceIn(0, exoPlayer.mediaItemCount)
        exoPlayer.addMediaItem(at, toMediaItem(item))
    }

    override fun move(fromIndex: Int, toIndex: Int) = runOnMain {
        if (fromIndex !in 0 until exoPlayer.mediaItemCount) return@runOnMain
        if (toIndex !in 0 until exoPlayer.mediaItemCount) return@runOnMain
        if (fromIndex == toIndex) return@runOnMain
        exoPlayer.moveMediaItem(fromIndex, toIndex)
    }

    override fun remove(index: Int) = runOnMain {
        if (index !in 0 until exoPlayer.mediaItemCount) return@runOnMain
        // ExoPlayer advances on its own when the current item is removed.
        exoPlayer.removeMediaItem(index)
    }

    override fun clear() = runOnMain {
        exoPlayer.clearMediaItems()
        exoPlayer.stop()
    }

    override fun play() = runOnMain { exoPlayer.play() }

    override fun pause() = runOnMain { exoPlayer.pause() }

    override fun playPause() = runOnMain { exoPlayer.playWhenReady = !exoPlayer.playWhenReady }

    override fun stop() = runOnMain { exoPlayer.stop() }

    override fun seekTo(positionMs: Long) = runOnMain { exoPlayer.seekTo(positionMs) }

    override fun seekToIndex(index: Int, positionMs: Long) = runOnMain {
        if (index !in 0 until exoPlayer.mediaItemCount) return@runOnMain
        exoPlayer.seekTo(index, positionMs)
    }

    override fun skipToNext() = runOnMain { exoPlayer.seekToNextMediaItem() }

    override fun skipToPrevious() = runOnMain {
        // seekToPreviousMediaItem, not seekToPrevious: the latter restarts the current track when
        // more than a few seconds have elapsed, which this app deliberately removed.
        exoPlayer.seekToPreviousMediaItem()
    }

    override fun setRepeatMode(mode: RepeatMode) = runOnMain {
        exoPlayer.repeatMode = mode.toPlayerRepeatMode()
    }

    override fun setShuffleEnabled(enabled: Boolean) = runOnMain {
        exoPlayer.shuffleModeEnabled = enabled
    }

    override fun setSpeed(speed: Float) = runOnMain {
        exoPlayer.setPlaybackParameters(PlaybackParameters(speed))
    }

    override fun setSkipSilence(enabled: Boolean) = runOnMain {
        exoPlayer.setSkipSilenceEnabled(enabled)
    }

    override fun setVolume(volume: Float) = runOnMain {
        exoPlayer.volume = volume.coerceIn(0f, 1f)
    }

    override fun currentPositionMs(): Long = exoPlayer.currentPosition

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun publishState(player: Player) {
        val count = player.mediaItemCount
        val queueIds = if (count == 0) {
            // -1 marks an item this module did not create, e.g. one pushed in by another
            // controller over the MediaSession.
            emptyList()
        } else {
            (0 until count).map { player.getMediaItemAt(it).mediaId.toLongOrNull() ?: UNKNOWN_ITEM_ID }
        }

        _state.value = EngineState(
            queueIds = queueIds,
            currentIndex = if (count == 0) -1 else player.currentMediaItemIndex,
            currentId = player.currentMediaItem?.mediaId?.toLongOrNull(),
            durationMs = player.duration.takeIf { it > 0 } ?: 0L,
            isPlaying = player.isPlaying,
            status = player.playbackState.toPlaybackStatus(),
            repeatMode = player.repeatMode.toRepeatMode(),
            shuffleEnabled = player.shuffleModeEnabled,
            speed = player.playbackParameters.speed,
            playWhenReady = player.playWhenReady
        )
    }

    private fun toMediaItem(item: QueueItem): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(item.title)
            .setArtist(item.artist)
            .setAlbumTitle(item.album)
            .setArtworkUri(item.artworkUri?.toUri())
            .build()

        return MediaItem.Builder()
            .setMediaId(item.id.toString())
            .setUri(item.uri)
            .setMediaMetadata(metadata)
            .build()
    }

    companion object {
        /** `mediaId` for items not created here. */
        const val UNKNOWN_ITEM_ID = -1L

        private fun Int.toPlaybackStatus(): PlaybackStatus = when (this) {
            Player.STATE_IDLE -> PlaybackStatus.IDLE
            Player.STATE_BUFFERING -> PlaybackStatus.BUFFERING
            Player.STATE_READY -> PlaybackStatus.READY
            Player.STATE_ENDED -> PlaybackStatus.ENDED
            else -> PlaybackStatus.IDLE
        }

        private fun Int.toRepeatMode(): RepeatMode = when (this) {
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.OFF
        }

        private fun Int.toTransitionReason(): TransitionReason = when (this) {
            Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> TransitionReason.AUTO
            Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> TransitionReason.SEEK
            Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> TransitionReason.REPEAT
            Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> TransitionReason.PLAYLIST_CHANGED
            else -> TransitionReason.UNKNOWN
        }
    }
}

private fun RepeatMode.toPlayerRepeatMode(): Int = when (this) {
    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
}
