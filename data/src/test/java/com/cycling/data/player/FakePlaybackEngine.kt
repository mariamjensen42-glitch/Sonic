package com.cycling.data.player

import com.cycling.data.player.engine.EngineState
import com.cycling.data.player.engine.PlaybackEngine
import com.cycling.data.player.engine.PlaybackEvent
import com.cycling.data.player.engine.PlaybackStatus
import com.cycling.data.player.engine.QueueItem
import com.cycling.data.player.engine.TransitionReason
import com.cycling.domain.model.RepeatMode
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Deterministic stand-in for ExoPlayer.
 *
 * Models the parts of the player contract that `PlayerManager` actually depends on: index
 * bookkeeping under insert/move/remove, repeat-mode-aware skipping, and the transition events
 * that drive play counting. Shuffle *ordering* is deliberately not modelled — the fake keeps
 * insertion order and only tracks the flag, because the ordering behaviour belongs to media3 and
 * has to be verified on a device.
 */
class FakePlaybackEngine : PlaybackEngine {

    private val _state = MutableStateFlow(EngineState())
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PlaybackEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    override val events: SharedFlow<PlaybackEvent> = _events.asSharedFlow()

    override val audioSessionId = 1

    /** Position reported by [currentPositionMs]; set directly by tests. */
    var positionMs: Long = 0L

    /** Duration reported for every item. */
    var durationMs: Long = 180_000L

    val eventsEmitted: List<PlaybackEvent> get() = emitted
    private val emitted = mutableListOf<PlaybackEvent>()

    override fun setQueue(items: List<QueueItem>, startIndex: Int, startPositionMs: Long) {
        if (items.isEmpty()) {
            _state.value = _state.value.copy(queueIds = emptyList(), currentIndex = -1, currentId = null)
            return
        }
        val index = startIndex.coerceIn(0, items.lastIndex)
        positionMs = startPositionMs
        _state.value = _state.value.copy(
            queueIds = items.map { it.id },
            currentIndex = index,
            currentId = items[index].id,
            durationMs = durationMs
        )
        emit(PlaybackEvent.ItemChanged(items[index].id, TransitionReason.SEEK))
    }

    override fun addToQueue(item: QueueItem) {
        val s = _state.value
        val wasEmpty = s.queueIds.isEmpty()
        _state.value = s.copy(
            queueIds = s.queueIds + item.id,
            currentIndex = if (wasEmpty) 0 else s.currentIndex,
            currentId = if (wasEmpty) item.id else s.currentId
        )
    }

    override fun insertNext(item: QueueItem, afterIndex: Int) {
        val s = _state.value
        val at = (afterIndex + 1).coerceIn(0, s.queueIds.size)
        val ids = s.queueIds.toMutableList().apply { add(at, item.id) }
        val index = when {
            s.queueIds.isEmpty() -> 0
            // Inserting at or before the current position shifts it along.
            at <= s.currentIndex -> s.currentIndex + 1
            else -> s.currentIndex
        }
        _state.value = s.copy(queueIds = ids, currentIndex = index, currentId = ids.getOrNull(index))
    }

    override fun move(fromIndex: Int, toIndex: Int) {
        val s = _state.value
        if (fromIndex !in s.queueIds.indices || toIndex !in s.queueIds.indices) return
        if (fromIndex == toIndex) return
        val ids = s.queueIds.toMutableList()
        val moved = ids.removeAt(fromIndex)
        ids.add(toIndex, moved)
        val index = when {
            s.currentIndex == fromIndex -> toIndex
            fromIndex < s.currentIndex && toIndex >= s.currentIndex -> s.currentIndex - 1
            fromIndex > s.currentIndex && toIndex <= s.currentIndex -> s.currentIndex + 1
            else -> s.currentIndex
        }
        _state.value = s.copy(queueIds = ids, currentIndex = index, currentId = ids.getOrNull(index))
    }

    override fun remove(index: Int) {
        val s = _state.value
        if (index !in s.queueIds.indices) return
        val ids = s.queueIds.toMutableList().apply { removeAt(index) }
        if (ids.isEmpty()) {
            _state.value = EngineState(
                repeatMode = s.repeatMode,
                shuffleEnabled = s.shuffleEnabled,
                playWhenReady = s.playWhenReady
            )
            return
        }
        val newIndex = when {
            index < s.currentIndex -> s.currentIndex - 1
            index == s.currentIndex -> s.currentIndex.coerceAtMost(ids.lastIndex)
            else -> s.currentIndex
        }
        if (index == s.currentIndex) positionMs = 0L
        _state.value = s.copy(queueIds = ids, currentIndex = newIndex, currentId = ids[newIndex])
    }

    override fun clear() {
        _state.value = EngineState()
    }

    override fun play() {
        _state.value = _state.value.copy(isPlaying = true, playWhenReady = true)
    }

    override fun pause() {
        _state.value = _state.value.copy(isPlaying = false, playWhenReady = false)
    }

    override fun playPause() {
        val ready = !_state.value.playWhenReady
        _state.value = _state.value.copy(isPlaying = ready, playWhenReady = ready)
    }

    override fun stop() {
        _state.value = _state.value.copy(
            isPlaying = false,
            status = PlaybackStatus.IDLE,
            playWhenReady = false
        )
    }

    override fun seekTo(positionMs: Long) {
        this.positionMs = positionMs
    }

    override fun seekToIndex(index: Int, positionMs: Long) {
        val s = _state.value
        if (index !in s.queueIds.indices) return
        this.positionMs = positionMs
        _state.value = s.copy(currentIndex = index, currentId = s.queueIds[index])
        emit(PlaybackEvent.ItemChanged(s.queueIds[index], TransitionReason.SEEK))
    }

    override fun skipToNext() {
        val s = _state.value
        if (s.queueIds.isEmpty()) return
        val next = when {
            s.currentIndex < s.queueIds.lastIndex -> s.currentIndex + 1
            s.repeatMode == RepeatMode.ALL -> 0
            // REPEAT_MODE_OFF at the end of the queue: the real player does nothing.
            else -> return
        }
        moveToIndex(next, TransitionReason.AUTO)
    }

    override fun skipToPrevious() {
        val s = _state.value
        if (s.queueIds.isEmpty()) return
        val previous = when {
            s.currentIndex > 0 -> s.currentIndex - 1
            s.repeatMode == RepeatMode.ALL -> s.queueIds.lastIndex
            else -> return
        }
        moveToIndex(previous, TransitionReason.AUTO)
    }

    override fun setRepeatMode(mode: RepeatMode) {
        _state.value = _state.value.copy(repeatMode = mode)
    }

    override fun setShuffleEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(shuffleEnabled = enabled)
    }

    override fun setSpeed(speed: Float) {
        _state.value = _state.value.copy(speed = speed)
    }

    override fun setSkipSilence(enabled: Boolean) {
        skipSilenceEnabled = enabled
    }

    var skipSilenceEnabled: Boolean = false
        private set

    override fun setVolume(volume: Float) {
        volumeValue = volume
    }

    var volumeValue: Float = 1f
        private set

    override fun currentPositionMs(): Long = positionMs

    // region test hooks

    /**
     * Advances to the next item the way the player does when one finishes: repeat-one restarts the
     * same item, otherwise it skips, and off-the-end stops with [PlaybackEvent.Ended].
     */
    fun simulateTrackFinished() {
        val s = _state.value
        if (s.queueIds.isEmpty()) return
        when {
            s.repeatMode == RepeatMode.ONE -> {
                positionMs = 0L
                emit(PlaybackEvent.ItemChanged(s.currentId, TransitionReason.REPEAT))
            }
            s.currentIndex < s.queueIds.lastIndex -> moveToIndex(s.currentIndex + 1, TransitionReason.AUTO)
            s.repeatMode == RepeatMode.ALL -> moveToIndex(0, TransitionReason.AUTO)
            else -> {
                _state.value = s.copy(isPlaying = false, status = PlaybackStatus.ENDED)
                emit(PlaybackEvent.Ended)
            }
        }
    }

    /** Emits a transition without moving, to model a playlist edit that left the item in place. */
    fun emitTransition(id: Long?, reason: TransitionReason) {
        emit(PlaybackEvent.ItemChanged(id, reason))
    }

    // endregion

    private fun moveToIndex(index: Int, reason: TransitionReason) {
        val s = _state.value
        positionMs = 0L
        _state.value = s.copy(currentIndex = index, currentId = s.queueIds.getOrNull(index))
        emit(PlaybackEvent.ItemChanged(s.queueIds.getOrNull(index), reason))
    }

    private fun emit(event: PlaybackEvent) {
        emitted += event
        _events.tryEmit(event)
    }
}
