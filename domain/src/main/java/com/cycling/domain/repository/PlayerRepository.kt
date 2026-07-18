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
    
    fun getCurrentState(): PlayerState
}
