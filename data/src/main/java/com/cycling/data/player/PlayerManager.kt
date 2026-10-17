package com.cycling.data.player

import com.cycling.data.di.PlayerDispatcher
import com.cycling.data.player.engine.EngineState
import com.cycling.data.player.engine.PlaybackEngine
import com.cycling.data.player.engine.PlaybackEvent
import com.cycling.data.player.engine.QueueItem
import com.cycling.data.player.engine.TransitionReason
import com.cycling.data.store.PlaybackStateStore
import com.cycling.domain.model.PlayerState
import com.cycling.domain.model.RepeatMode
import com.cycling.domain.model.Song
import com.cycling.domain.repository.AudioMetadataRepository
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.SettingsRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.domain.usecase.GetContinuationSongsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the playback queue and the public [PlayerState].
 *
 * The queue itself lives inside ExoPlayer now, reached through [PlaybackEngine] — this class no
 * longer keeps a parallel `MutableList` with its own index bookkeeping, which is what used to
 * make shuffle repeat tracks, `skipToPrevious` fight the 3-second rule, and gapless playback
 * impossible.
 *
 * Everything here runs on [dispatcher] and holds no Android types, so the queue and state logic
 * is unit-testable against a fake engine.
 */
@Singleton
class PlayerManager @Inject constructor(
    private val engine: PlaybackEngine,
    private val songRepository: SongRepository,
    private val audioMetadataRepository: AudioMetadataRepository,
    private val playbackStateStore: PlaybackStateStore,
    private val settingsRepository: SettingsRepository,
    private val getContinuationSongs: GetContinuationSongsUseCase,
    private val playbackServiceController: PlaybackServiceController,
    @PlayerDispatcher private val dispatcher: CoroutineDispatcher
) : PlayerRepository {

    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(supervisorJob + dispatcher)

    private val _sleepTimerRemainingMs = MutableStateFlow<Long?>(null)
    override val sleepTimerRemainingMs: StateFlow<Long?> = _sleepTimerRemainingMs.asStateFlow()

    /**
     * Resolves queue ids back to full songs.
     *
     * `EngineState` carries ids only, because a `MediaItem` cannot hold a `Song` without dragging
     * app types into the timeline that media3 then marshals over Binder. This map is also what
     * `setSongFavorite` mutates, and it must stay in sync: the queue is rebuilt from it on every
     * engine change, so a stale entry silently reverts the heart icon.
     */
    private val songLookup = mutableMapOf<Long, Song>()

    private var progressLoopJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var fadeJob: Job? = null
    private var currentSongId: Long? = null
    private var hasCountedPlay = false
    private var lastPersistedPosition = -1L
    private var lastTransitionId: Long? = null
    private var lastIsPlaying: Boolean? = null
    private var hadNonEmptyQueue = false
    private var restored = false

    /** 队列播完后自动续播的开关。默认开，见 [SettingsRepository.autoContinueEnabled]。 */
    private var autoContinueEnabled = true

    /** 一次 Ended 只允许发起一次续播查询；换曲后复位，避免查询失败时 Ended 反复触发。 */
    private var continuationInFlight = false

    /** 音量均衡开关。关着的时候 [baseVolume] 恒为满音量。 */
    private var replayGainEnabled = false

    /** 淡入淡出时长；0 表示不做渐变，音量直接到位。 */
    private var fadeDurationMs = 0L

    /** 「记住播放位置」开关。关掉之后既不再存进度，恢复时也从 0 开始。 */
    private var rememberPosition = true

    /**
     * 当前曲目应有的音量，由 ReplayGain 决定。
     *
     * 与实际下发值分开：淡入淡出乘的是一个 0..1 的渐变系数，两者相乘才是 [engine] 收到的音量。
     * 合在一起的话，淡出结束时会把 ReplayGain 的结果一起抹掉。
     */
    private var baseVolume = FULL_VOLUME

    private var fadeFactor = FULL_VOLUME

    /** 每换一首歌自增，用来丢弃上一首还没读完的 ReplayGain 结果。 */
    private var volumeToken = 0

    init {
        scope.launch { engine.state.collect(::onEngineState) }
        scope.launch { engine.events.collect(::onEngineEvent) }
        // Playback preferences are pushed to the engine rather than read at build time, so a
        // change takes effect immediately and a stored value is applied on every cold start.
        scope.launch { settingsRepository.playbackSpeed.collect(engine::setSpeed) }
        scope.launch { settingsRepository.skipSilence.collect(engine::setSkipSilence) }
        scope.launch {
            settingsRepository.replayGainEnabled.collect { enabled ->
                replayGainEnabled = enabled
                applyTrackVolume(engine.state.value.currentId)
            }
        }
        scope.launch { settingsRepository.audioFadeDurationMs.collect { fadeDurationMs = it } }
        scope.launch {
            settingsRepository.rememberPlaybackPosition.collect { enabled -> rememberPosition = enabled }
        }
        scope.launch {
            settingsRepository.autoContinueEnabled.collect { enabled -> autoContinueEnabled = enabled }
        }
    }

    /**
     * Restores the queue and position saved by the last session.
     *
     * Does not start the playback service itself. Starting it is what happens on the way into
     * `play()`, and there are only three doors into playback — here, [playSong] and [playPause] —
     * so a launch with nothing to play leaves the service alone instead of arming a foreground
     * deadline it cannot meet. See [PlaybackServiceController.start].
     */
    suspend fun connect(): Boolean = try {
        // 读一次而不是等收集器：恢复要决定"从哪一秒开始"，而收集器的首次发射没有顺序保证，
        // 慢一拍就会先按默认值把进度恢复出来。
        rememberPosition = settingsRepository.rememberPlaybackPosition.first()
        restorePlaybackState()
        true
    } catch (e: Exception) {
        Timber.e(e, "connect: failed to restore playback state")
        false
    }

    // region engine -> state

    private fun onEngineState(engineState: EngineState) {
        if (engineState.queueIds.isEmpty()) {
            // Only treat an empty queue as "playback ended" if we had one — otherwise this fires
            // during startup, before restorePlaybackState has rebuilt anything, and would wipe
            // the very state it is about to restore.
            if (hadNonEmptyQueue) {
                hadNonEmptyQueue = false
                lastTransitionId = null
                currentSongId = null
                _playerState.value = PlayerState()
            }
            return
        }
        hadNonEmptyQueue = true

        if (engineState.isPlaying != lastIsPlaying) {
            lastIsPlaying = engineState.isPlaying
            if (engineState.isPlaying) startProgressLoop() else stopProgressLoop()
            scope.launch { playbackStateStore.saveWasPlaying(engineState.isPlaying) }
        }

        val position = engine.currentPositionMs()
        _playerState.update { previous ->
            previous.copy(
                currentSong = engineState.currentId?.let(songLookup::get),
                playbackQueue = engineState.queueIds.map { id ->
                    songLookup[id] ?: unknownSong(id)
                },
                queueIndex = engineState.currentIndex,
                isPlaying = engineState.isPlaying,
                playbackPosition = position,
                duration = engineState.durationMs,
                repeatMode = engineState.repeatMode,
                shuffleMode = engineState.shuffleEnabled
            )
        }
        currentSongId = _playerState.value.currentSong?.id
    }

    private fun onEngineEvent(event: PlaybackEvent) {
        when (event) {
            is PlaybackEvent.ItemChanged -> onItemChanged(event)
            PlaybackEvent.Ended -> {
                Timber.d("onEngineEvent: playback ended")
                _playerState.update { it.copy(isPlaying = false) }
                scope.launch { continueQueueIfEnabled() }
            }
        }
    }

    private fun onItemChanged(event: PlaybackEvent.ItemChanged) {
        val id = event.id
        val isDifferentItem = id != lastTransitionId
        lastTransitionId = id

        // A playlist edit can fire a transition without the current song changing — for example
        // when an item is inserted ahead of it. Counting that as a new play would inflate play
        // counts and re-stamp lastPlayedAt.
        val beginsNewPlay = when (event.reason) {
            TransitionReason.PLAYLIST_CHANGED, TransitionReason.UNKNOWN -> isDifferentItem
            else -> true
        }
        if (!beginsNewPlay) return

        hasCountedPlay = false
        lastPersistedPosition = -1L
        currentSongId = id
        continuationInFlight = false
        id?.let { songId ->
            scope.launch(Dispatchers.IO) { songRepository.updateLastPlayedAt(songId) }
        }
        if (isDifferentItem) {
            scope.launch { persistQueuePosition() }
        }
        applyTrackVolume(id)
        // 暂停状态下换曲（恢复会话、或队列被改写）不该有声音渐入，只在真的在播时做。
        if (engine.state.value.playWhenReady) fadeIn()
    }

    /**
     * 队列自然播完后的续播。
     *
     * 只在 repeat OFF 且确实停在最后一首时触发。续播不是重建播放队列，而是把新歌**追加**到原队列
     * 后面并从第一首新歌开始播 —— 用户仍能往回翻到刚才听过的部分。曲库被排除完时返回空列表，
     * 到此为止，不会自转。
     */
    private suspend fun continueQueueIfEnabled() {
        if (!autoContinueEnabled || continuationInFlight) return
        val engineState = engine.state.value
        if (engineState.repeatMode != RepeatMode.OFF) return
        if (engineState.queueIds.isEmpty()) return
        if (engineState.currentIndex != engineState.queueIds.lastIndex) return

        val seed = _playerState.value.currentSong ?: return
        continuationInFlight = true

        val continuation = try {
            getContinuationSongs(seed, engineState.queueIds.toSet(), CONTINUATION_SIZE)
        } catch (e: Exception) {
            Timber.w(e, "continueQueue: could not pick follow-up songs for ${seed.id}")
            emptyList()
        }
        if (continuation.isEmpty()) {
            Timber.d("continueQueue: library exhausted, stopping at ${seed.id}")
            return
        }

        registerSongs(continuation)
        val existing = engineState.queueIds.mapNotNull(songLookup::get)
        val newQueue = existing + continuation
        engine.setQueue(newQueue.map(::toQueueItem), existing.size)
        playbackServiceController.start()
        engine.play()
        fadeIn()
        Timber.d("continueQueue: appended ${continuation.size} songs after ${seed.id}")
    }

    private fun startProgressLoop() {
        if (progressLoopJob?.isActive == true) return
        progressLoopJob = scope.launch {
            while (isActive) {
                val position = engine.currentPositionMs()
                val engineState = engine.state.value
                _playerState.update { it.copy(playbackPosition = position) }
                checkPlayProgress(position, engineState.durationMs)
                if (rememberPosition &&
                    position > 0 &&
                    abs(position - lastPersistedPosition) >= POSITION_PERSIST_INTERVAL_MS
                ) {
                    lastPersistedPosition = position
                    launch { playbackStateStore.savePlaybackPosition(position) }
                }
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

    private fun stopProgressLoop() {
        progressLoopJob?.cancel()
        progressLoopJob = null
    }

    private fun checkPlayProgress(position: Long, duration: Long) {
        if (hasCountedPlay || duration <= 0 || position <= 0) return
        if (position.toFloat() / duration.toFloat() < PLAY_COUNT_THRESHOLD) return

        val songId = currentSongId ?: return
        hasCountedPlay = true
        scope.launch(Dispatchers.IO) { songRepository.incrementPlayCount(songId) }
    }

    private suspend fun persistQueuePosition() {
        val engineState = engine.state.value
        playbackStateStore.saveQueue(engineState.queueIds, engineState.currentIndex)
    }

    // endregion

    private suspend fun restorePlaybackState() {
        if (restored) return
        restored = true

        val saved = playbackStateStore.getSavedState() ?: return
        val songs = saved.queueSongIds.mapNotNull { songRepository.getSongById(it) }
        if (songs.isEmpty()) {
            Timber.d("restorePlaybackState: no songs found, clearing saved state")
            playbackStateStore.clear()
            return
        }

        val index = saved.currentIndex.coerceIn(0, songs.lastIndex)
        registerSongs(songs)

        // Shuffle has to be set before the timeline, because media3 regenerates the shuffle order
        // whenever the queue changes and the order itself is not persisted.
        engine.setRepeatMode(saved.repeatMode)
        engine.setShuffleEnabled(saved.shuffleMode)
        val startPosition = if (rememberPosition) saved.playbackPosition else 0L
        engine.setQueue(songs.map(::toQueueItem), index, startPosition)
        if (saved.wasPlaying) {
            playbackServiceController.start()
            engine.play()
            fadeIn()
        }
        Timber.d("restorePlaybackState: restored ${songs.size} songs at index=$index")
    }

    private fun registerSongs(songs: List<Song>) {
        songs.forEach { songLookup[it.id] = it }
    }

    private fun toQueueItem(song: Song) = QueueItem(
        id = song.id,
        uri = song.path,
        title = song.title,
        artist = song.artist,
        album = song.album,
        artworkUri = song.albumArt
    )

    private fun unknownSong(id: Long) = Song(
        id = id,
        title = "",
        artist = "",
        album = "",
        duration = 0L,
        path = "",
        albumId = -1L,
        artistId = -1L,
        dateAdded = 0L,
        dateModified = 0L,
        size = 0L,
        mimeType = ""
    )

    // region PlayerRepository

    override fun playSong(song: Song, queue: List<Song>) {
        scope.launch {
            val items = if (queue.isNotEmpty()) {
                queue
            } else {
                // An empty queue means "play this song, keep whatever is already queued".
                val existing = engine.state.value.queueIds.mapNotNull(songLookup::get)
                if (existing.any { it.id == song.id }) existing else existing + song
            }
            if (items.isEmpty()) return@launch

            registerSongs(items + song)
            val startIndex = items.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

            // Re-setting an identical queue would regenerate the shuffle order and lose the
            // user's place in it, so only rebuild the timeline when it actually differs.
            if (engine.state.value.queueIds == items.map { it.id }) {
                engine.seekToIndex(startIndex)
            } else {
                engine.setQueue(items.map(::toQueueItem), startIndex)
            }
            playbackServiceController.start()
            engine.play()
            fadeIn()
        }
    }

    override fun playPause() {
        // 用 playWhenReady 而不是 isPlaying 判断：缓冲期间 isPlaying 是 false，但它并不是"暂停"，
        // 那时按下的暂停键必须真的把播放意图关掉。
        if (engine.state.value.playWhenReady) {
            fadeOutThen { engine.pause() }
        } else {
            // Resuming is a fresh entry into playback: the queue may have been restored paused, in
            // which case nothing has brought the service up yet. Pausing cannot need this — the only
            // way to be playing at all is through a path below that already started the service.
            playbackServiceController.start()
            engine.play()
            fadeIn()
        }
    }

    override fun seekTo(position: Long) {
        engine.seekTo(position)
        _playerState.update { it.copy(playbackPosition = position) }
    }

    override fun skipToNext() {
        engine.skipToNext()
    }

    override fun skipToPrevious() {
        engine.skipToPrevious()
    }

    override fun setRepeatMode(repeatMode: RepeatMode) {
        engine.setRepeatMode(repeatMode)
        scope.launch { playbackStateStore.saveRepeatMode(repeatMode) }
    }

    override fun setShuffleMode(shuffle: Boolean) {
        engine.setShuffleEnabled(shuffle)
        scope.launch { playbackStateStore.saveShuffleMode(shuffle) }
    }

    override fun addToQueue(song: Song) {
        registerSongs(listOf(song))
        engine.addToQueue(toQueueItem(song))
        scope.launch { persistQueuePosition() }
    }

    override fun playNext(song: Song) {
        val currentIndex = engine.state.value.currentIndex
        if (currentIndex < 0) {
            playSong(song)
            return
        }
        registerSongs(listOf(song))
        engine.insertNext(toQueueItem(song), currentIndex)
        scope.launch { persistQueuePosition() }
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        engine.move(fromIndex, toIndex)
        scope.launch { persistQueuePosition() }
    }

    override fun removeFromQueue(index: Int) {
        engine.remove(index)
        scope.launch { persistQueuePosition() }
    }

    override fun clearQueue() {
        engine.clear()
        _playerState.value = PlayerState()
        hadNonEmptyQueue = false
        scope.launch { playbackStateStore.clear() }
    }

    override fun getCurrentState(): PlayerState = _playerState.value

    override fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemainingMs.value = null
            return
        }

        sleepTimerJob = scope.launch {
            var remaining = minutes * 60_000L
            _sleepTimerRemainingMs.value = remaining
            while (remaining > 0) {
                delay(SLEEP_TIMER_TICK_MS)
                remaining -= SLEEP_TIMER_TICK_MS
                _sleepTimerRemainingMs.value = remaining.coerceAtLeast(0L)
            }
            _sleepTimerRemainingMs.value = null
            // 到点暂停而不是停止：醒来接着播，队列和进度都还在原处。同样走淡出，
            // 睡着时被硬切一下比定时本身更容易把人吵醒。
            if (engine.state.value.playWhenReady) fadeOutThen { engine.pause() }
        }
    }

    override fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingMs.value = null
    }

    override fun setSongFavorite(songId: Long, isFavorite: Boolean) {
        // songLookup first: the queue shown to the UI is rebuilt from it on every engine change,
        // so updating only the current PlayerState would let the heart flip back a moment later.
        songLookup[songId]?.let { songLookup[songId] = it.copy(isFavorite = isFavorite) }
        _playerState.update { state ->
            state.copy(
                currentSong = state.currentSong?.withFavorite(songId, isFavorite),
                playbackQueue = state.playbackQueue.map { it.withFavorite(songId, isFavorite) }
            )
        }
    }

    private fun Song.withFavorite(songId: Long, isFavorite: Boolean): Song =
        if (id == songId) copy(isFavorite = isFavorite) else this

    // endregion

    // region 音量：ReplayGain 与淡入淡出

    /**
     * 决定当前曲目的基础音量。
     *
     * 标签读取要碰磁盘，所以先按"没有标签"把音量放到位，读完再修正 —— 起播不能等一次文件读。
     * [volumeToken] 保证上一首读得慢的结果不会落到这一首上。
     */
    private fun applyTrackVolume(songId: Long?) {
        val token = ++volumeToken

        if (!replayGainEnabled) {
            baseVolume = FULL_VOLUME
            applyVolume()
            return
        }

        val path = songId?.let { songLookup[it]?.path }
        if (path.isNullOrBlank()) {
            baseVolume = FULL_VOLUME
            applyVolume()
            return
        }

        baseVolume = FULL_VOLUME
        applyVolume()
        scope.launch(Dispatchers.IO) {
            val gainDb = runCatching { audioMetadataRepository.getReplayGainDb(path) }.getOrNull()
            if (token != volumeToken) return@launch
            baseVolume = gainDb?.let(::volumeForGainDb) ?: FULL_VOLUME
            applyVolume()
        }
    }

    /**
     * ReplayGain 的 dB 值换算成音量倍率：`10^(dB/20)`。
     *
     * 正增益会超过 1.0，而 `ExoPlayer.volume` 的上限就是 1.0，只能截断。所以这个开关对安静的歌
     * 是"不处理"，对响的歌才是"压下来" —— 这正是音量均衡要的效果，反过来的话就得改 DSP 了。
     */
    private fun volumeForGainDb(gainDb: Float): Float =
        10f.pow(gainDb / 20f).coerceIn(MIN_VOLUME, FULL_VOLUME)

    private fun applyVolume() {
        engine.setVolume(baseVolume * fadeFactor)
    }

    /** 从静音推上来。已经在淡入时重新开始，不会叠加。 */
    private fun fadeIn() {
        fadeJob?.cancel()
        if (fadeDurationMs <= 0L) {
            fadeFactor = FULL_VOLUME
            applyVolume()
            return
        }
        fadeJob = scope.launch {
            val stepMs = (fadeDurationMs / FADE_STEPS).coerceAtLeast(1L)
            for (step in 1..FADE_STEPS) {
                fadeFactor = step / FADE_STEPS.toFloat()
                applyVolume()
                delay(stepMs)
            }
        }
    }

    /**
     * 先压到静音，再执行 [action]。
     *
     * 渐变结束后把 [fadeFactor] 复位成 1 但不写回引擎：引擎停在 0 音量，下一次 [fadeIn] 才有
     * 起点。若这里写回 1，暂停期间引擎会一直是满音量，恢复播放时就没有淡入。
     */
    private fun fadeOutThen(action: () -> Unit) {
        fadeJob?.cancel()
        if (fadeDurationMs <= 0L) {
            fadeFactor = FULL_VOLUME
            applyVolume()
            action()
            return
        }
        fadeJob = scope.launch {
            val stepMs = (fadeDurationMs / FADE_STEPS).coerceAtLeast(1L)
            for (step in FADE_STEPS downTo 0) {
                fadeFactor = step / FADE_STEPS.toFloat()
                applyVolume()
                delay(stepMs)
            }
            action()
            fadeFactor = FULL_VOLUME
        }
    }

    // endregion

    private companion object {
        const val PROGRESS_INTERVAL_MS = 1_000L
        const val POSITION_PERSIST_INTERVAL_MS = 5_000L

        /** 睡眠定时器每秒减一次，界面上的倒计时才不会看起来卡住 */
        const val SLEEP_TIMER_TICK_MS = 1_000L

        /** Fraction of a track that must play before it counts as a play. */
        const val PLAY_COUNT_THRESHOLD = 0.5f

        /** How many songs a queue-end continuation appends at a time. */
        const val CONTINUATION_SIZE = 20

        /** 淡入淡出的步数。按 200ms 算每步 20ms，足够平滑又不至于每毫秒打一次播放器。 */
        const val FADE_STEPS = 10

        const val FULL_VOLUME = 1f

        /** 静音。ReplayGain 换算出来的倍率也用它做下界，避免 0 与"被压到听不见"混在一起。 */
        const val MIN_VOLUME = 0f
    }
}
