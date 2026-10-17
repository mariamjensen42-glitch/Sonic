package com.cycling.domain.repository

import com.cycling.domain.model.PlayerState
import com.cycling.domain.model.RepeatMode
import com.cycling.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    val playerState: Flow<PlayerState>
    
    fun playSong(song: Song, queue: List<Song> = emptyList())
    
    fun playPause()
    
    fun seekTo(position: Long)
    
    fun skipToNext()
    
    fun skipToPrevious()
    
    fun setRepeatMode(repeatMode: RepeatMode)
    
    fun setShuffleMode(shuffle: Boolean)
    
    fun addToQueue(song: Song)
    
    fun playNext(song: Song)
    
    fun moveQueueItem(fromIndex: Int, toIndex: Int)
    
    fun removeFromQueue(index: Int)
    
    fun clearQueue()

    fun setSongFavorite(songId: Long, isFavorite: Boolean)

    fun getCurrentState(): PlayerState

    /**
     * Pauses the player [minutes] from now. [minutes] of 0 or less means "no timer".
     *
     * Setting a timer replaces any timer already running rather than adding to it — the picker is a
     * choice of "how much longer", not an increment.
     */
    fun setSleepTimer(minutes: Int)

    fun cancelSleepTimer()

    /**
     * How long the sleep timer has left, or `null` when none is running.
     *
     * Only ever counts down while the process lives: a timer that survived a restart would fire on
     * a session the user has no memory of starting.
     */
    val sleepTimerRemainingMs: Flow<Long?>
}
