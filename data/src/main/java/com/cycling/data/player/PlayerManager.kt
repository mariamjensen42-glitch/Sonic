package com.cycling.data.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.cycling.domain.model.PlayerState
import com.cycling.domain.model.RepeatMode
import com.cycling.domain.model.Song
import com.cycling.domain.repository.SongRepository
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
@OptIn(UnstableApi::class)
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songRepository: SongRepository
) {
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(supervisorJob + Dispatchers.Main)

    private var playbackQueue: MutableList<Song> = mutableListOf()
    private var currentIndex: Int = -1
    private var hasCountedPlay: Boolean = false
    private var currentSongId: Long? = null

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Timber.d("Player state changed: isPlaying=$isPlaying")
            _playerState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            Timber.d("Playback state changed: $playbackState")
            if (playbackState == Player.STATE_ENDED) {
                onSongEnded()
            }
            updatePlaybackPosition()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            Timber.d("Media item transition: ${mediaItem?.mediaMetadata?.title}, reason=$reason")
            updatePlaybackPosition()
            resetPlayCountFlag()
        }
    }

    suspend fun connect(): Boolean {
        return try {
            if (mediaController != null) return true

            Timber.d("Connecting to PlayerService...")
            PlayerService.start(context)

            val sessionToken = SessionToken(context, android.content.ComponentName(context, PlayerService::class.java))
            controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            mediaController = controllerFuture?.await()
            mediaController?.addListener(playerListener)

            startProgressUpdateLoop()
            Timber.d("Connected to PlayerService successfully")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect to PlayerService")
            false
        }
    }

    private fun startProgressUpdateLoop() {
        scope.launch {
            while (true) {
                updatePlaybackPosition()
                checkPlayProgress()
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun resetPlayCountFlag() {
        hasCountedPlay = false
    }

    private fun checkPlayProgress() {
        val player = mediaController ?: return
        val duration = player.duration
        val position = player.currentPosition

        if (duration > 0 && position > 0 && !hasCountedPlay) {
            val progress = position.toFloat() / duration.toFloat()
            if (progress >= 0.5f) {
                currentSongId?.let { songId ->
                    scope.launch(Dispatchers.IO) {
                        songRepository.incrementPlayCount(songId)
                    }
                    hasCountedPlay = true
                }
            }
        }
    }

    private fun updatePlaybackPosition() {
        val player = mediaController ?: return
        _playerState.update {
            it.copy(
                playbackPosition = player.currentPosition,
                duration = player.duration.takeIf { d -> d > 0 } ?: 0
            )
        }
    }

    private fun onSongEnded() {
        when (_playerState.value.repeatMode) {
            RepeatMode.ONE -> {
                mediaController?.seekTo(0)
                mediaController?.play()
            }
            RepeatMode.ALL -> {
                skipToNextInternal(wrapAround = true)
            }
            RepeatMode.OFF -> {
                if (currentIndex < playbackQueue.size - 1) {
                    skipToNextInternal(wrapAround = false)
                }
            }
        }
    }

    private fun createMediaItem(song: Song): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)

        song.albumArt?.let { artworkUri ->
            try {
                val bitmap = loadAlbumArtBitmap(artworkUri, song.path)
                if (bitmap != null) {
                    val artworkData = bitmapToByteArray(bitmap)
                    metadataBuilder.setArtworkData(artworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    Timber.d("createMediaItem: embedded artwork data for ${song.title}, size=${artworkData.size}")
                } else {
                    Timber.d("createMediaItem: failed to load bitmap for ${song.title}")
                }
            } catch (e: Exception) {
                Timber.e(e, "createMediaItem: error loading artwork for ${song.title}")
            }
        }

        return MediaItem.Builder()
            .setUri(song.path)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun loadAlbumArtBitmap(uriString: String, songPath: String? = null): Bitmap? {
        return try {
            val uri = uriString.toUri()

            // Try to load thumbnail using modern API (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val thumbnail = context.contentResolver.loadThumbnail(
                        uri,
                        Size(512, 512),
                        null
                    )
                    if (thumbnail != null) {
                        return thumbnail
                    }
                } catch (e: Exception) {
                    Timber.d("loadAlbumArtBitmap: thumbnail loading failed, falling back to embedded artwork")
                }
            }

            // Fallback: try to open input stream (works on older Android versions)
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    return BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                Timber.d("loadAlbumArtBitmap: content resolver failed, trying embedded artwork")
            }

            // Final fallback: extract embedded artwork from audio file
            songPath?.let { path ->
                extractEmbeddedArtwork(path)
            }
        } catch (e: Exception) {
            Timber.e(e, "loadAlbumArtBitmap: failed to load from $uriString")
            null
        }
    }

    private fun extractEmbeddedArtwork(filePath: String): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val art = retriever.embeddedPicture
            retriever.release()
            art?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        } catch (e: Exception) {
            Timber.e(e, "extractEmbeddedArtwork: failed to extract artwork from $filePath")
            null
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val maxSize = 512
        val scaledBitmap = if (bitmap.width > maxSize || bitmap.height > maxSize) {
            val scale = maxSize.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }
        
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return outputStream.toByteArray()
    }

    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        val player = mediaController ?: return

        Timber.d("playSong: ${song.title} by ${song.artist}, queueSize=${queue.size}")

        if (queue.isNotEmpty()) {
            playbackQueue = queue.toMutableList()
            currentIndex = queue.indexOf(song)
        } else {
            if (!playbackQueue.contains(song)) {
                playbackQueue.add(song)
            }
            currentIndex = playbackQueue.indexOf(song)
        }

        val mediaItem = createMediaItem(song)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true

        currentSongId = song.id
        hasCountedPlay = false

        scope.launch(Dispatchers.IO) {
            songRepository.updateLastPlayedAt(song.id)
        }

        _playerState.update {
            it.copy(
                currentSong = song,
                playbackQueue = playbackQueue.toList(),
                queueIndex = currentIndex,
                isPlaying = true
            )
        }
    }

    fun playPause() {
        val player = mediaController ?: return
        if (player.isPlaying) {
            Timber.d("playPause: pausing playback")
            player.pause()
        } else {
            Timber.d("playPause: resuming playback")
            player.play()
        }
    }

    fun seekTo(position: Long) {
        val player = mediaController ?: return
        Timber.d("seekTo: position=$position ms")
        player.seekTo(position)
        _playerState.update { it.copy(playbackPosition = position) }
    }

    fun skipToNext() {
        Timber.d("skipToNext: currentIndex=$currentIndex, queueSize=${playbackQueue.size}")
        skipToNextInternal(wrapAround = _playerState.value.repeatMode == RepeatMode.ALL)
    }

    private fun skipToNextInternal(wrapAround: Boolean) {
        if (playbackQueue.isEmpty()) return

        val nextIndex = if (_playerState.value.shuffleMode) {
            playbackQueue.indices.random()
        } else {
            currentIndex + 1
        }

        if (nextIndex < playbackQueue.size) {
            playSongAtIndex(nextIndex)
        } else if (wrapAround && nextIndex >= playbackQueue.size) {
            playSongAtIndex(0)
        }
    }

    fun skipToPrevious() {
        val player = mediaController ?: return
        if (playbackQueue.isEmpty()) return

        Timber.d("skipToPrevious: currentIndex=$currentIndex, position=${player.currentPosition}")

        if (player.currentPosition > 3000) {
            Timber.d("skipToPrevious: seeking to beginning (position > 3000ms)")
            player.seekTo(0)
            return
        }

        val prevIndex = if (_playerState.value.shuffleMode) {
            playbackQueue.indices.random()
        } else {
            currentIndex - 1
        }

        if (prevIndex >= 0) {
            playSongAtIndex(prevIndex)
        } else if (_playerState.value.repeatMode == RepeatMode.ALL) {
            playSongAtIndex(playbackQueue.size - 1)
        }
    }

    private fun playSongAtIndex(index: Int) {
        val player = mediaController ?: return
        if (index !in playbackQueue.indices) return

        currentIndex = index
        val song = playbackQueue[index]

        val mediaItem = createMediaItem(song)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true

        currentSongId = song.id
        hasCountedPlay = false

        scope.launch(Dispatchers.IO) {
            songRepository.updateLastPlayedAt(song.id)
        }

        _playerState.update {
            it.copy(
                currentSong = song,
                queueIndex = index,
                isPlaying = true
            )
        }
    }

    fun setRepeatMode(repeatMode: RepeatMode) {
        Timber.d("setRepeatMode: $repeatMode")
        _playerState.update { it.copy(repeatMode = repeatMode) }
    }

    fun setShuffleMode(shuffle: Boolean) {
        Timber.d("setShuffleMode: $shuffle")
        _playerState.update { it.copy(shuffleMode = shuffle) }
    }

    fun addToQueue(song: Song) {
        Timber.d("addToQueue: ${song.title}")
        playbackQueue.add(song)
        _playerState.update { it.copy(playbackQueue = playbackQueue.toList()) }
    }

    fun playNext(song: Song) {
        Timber.d("playNext: ${song.title}, currentIndex=$currentIndex")
        
        if (playbackQueue.isEmpty() || currentIndex < 0) {
            playSong(song)
            return
        }
        
        val insertIndex = currentIndex + 1
        playbackQueue.add(insertIndex, song)
        
        _playerState.update { it.copy(playbackQueue = playbackQueue.toList()) }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in playbackQueue.indices || toIndex !in playbackQueue.indices) return
        if (fromIndex == toIndex) return
        
        Timber.d("moveQueueItem: from=$fromIndex to=$toIndex, currentIndex=$currentIndex")
        
        val song = playbackQueue.removeAt(fromIndex)
        playbackQueue.add(toIndex, song)
        
        when {
            currentIndex == fromIndex -> {
                currentIndex = toIndex
            }
            fromIndex < currentIndex && toIndex >= currentIndex -> {
                currentIndex--
            }
            fromIndex > currentIndex && toIndex <= currentIndex -> {
                currentIndex++
            }
        }
        
        _playerState.update { 
            it.copy(
                playbackQueue = playbackQueue.toList(),
                queueIndex = currentIndex
            )
        }
    }

    fun removeFromQueue(index: Int) {
        if (index !in playbackQueue.indices) return

        val player = mediaController ?: return
        Timber.d("removeFromQueue: index=$index, currentIndex=$currentIndex")
        playbackQueue.removeAt(index)
        if (currentIndex > index) {
            currentIndex--
        } else if (currentIndex == index) {
            if (playbackQueue.isNotEmpty()) {
                playSongAtIndex(minOf(currentIndex, playbackQueue.size - 1))
            } else {
                Timber.d("removeFromQueue: queue is now empty, stopping playback")
                player.stop()
                _playerState.update { PlayerState() }
            }
        }
        _playerState.update { it.copy(playbackQueue = playbackQueue.toList()) }
    }

    fun clearQueue() {
        val player = mediaController ?: return
        Timber.d("clearQueue: clearing queue with ${playbackQueue.size} items")
        playbackQueue.clear()
        currentIndex = -1
        player.stop()
        _playerState.update { PlayerState() }
    }

    fun getCurrentState(): PlayerState = _playerState.value

    fun release() {
        mediaController?.removeListener(playerListener)
        controllerFuture?.let {
            if (!it.isDone) {
                it.cancel(true)
            }
        }
        mediaController?.release()
        mediaController = null
        supervisorJob.cancel()
    }
}
