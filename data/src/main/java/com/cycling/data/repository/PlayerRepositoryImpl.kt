package com.cycling.data.repository

import com.cycling.data.player.PlayerManager
import com.cycling.domain.model.PlayerState
import com.cycling.domain.model.RepeatMode
import com.cycling.domain.model.Song
import com.cycling.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepositoryImpl @Inject constructor(
    private val playerManager: PlayerManager
) : PlayerRepository {

    override val playerState: Flow<PlayerState> = playerManager.playerState

    override fun playSong(song: Song, queue: List<Song>) {
        Timber.d("PlayerRepository.playSong: ${song.title}, queueSize=${queue.size}")
        playerManager.playSong(song, queue)
    }

    override fun playPause() {
        Timber.d("PlayerRepository.playPause")
        playerManager.playPause()
    }

    override fun seekTo(position: Long) {
        Timber.d("PlayerRepository.seekTo: position=$position")
        playerManager.seekTo(position)
    }

    override fun skipToNext() {
        Timber.d("PlayerRepository.skipToNext")
        playerManager.skipToNext()
    }

    override fun skipToPrevious() {
        Timber.d("PlayerRepository.skipToPrevious")
        playerManager.skipToPrevious()
    }

    override fun setRepeatMode(repeatMode: RepeatMode) {
        Timber.d("PlayerRepository.setRepeatMode: $repeatMode")
        playerManager.setRepeatMode(repeatMode)
    }

    override fun setShuffleMode(shuffle: Boolean) {
        Timber.d("PlayerRepository.setShuffleMode: $shuffle")
        playerManager.setShuffleMode(shuffle)
    }

    override fun addToQueue(song: Song) {
        Timber.d("PlayerRepository.addToQueue: ${song.title}")
        playerManager.addToQueue(song)
    }

    override fun playNext(song: Song) {
        Timber.d("PlayerRepository.playNext: ${song.title}")
        playerManager.playNext(song)
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        Timber.d("PlayerRepository.moveQueueItem: from=$fromIndex to=$toIndex")
        playerManager.moveQueueItem(fromIndex, toIndex)
    }

    override fun removeFromQueue(index: Int) {
        Timber.d("PlayerRepository.removeFromQueue: index=$index")
        playerManager.removeFromQueue(index)
    }

    override fun clearQueue() {
        Timber.d("PlayerRepository.clearQueue")
        playerManager.clearQueue()
    }

    override fun getCurrentState(): PlayerState = playerManager.getCurrentState()
}
