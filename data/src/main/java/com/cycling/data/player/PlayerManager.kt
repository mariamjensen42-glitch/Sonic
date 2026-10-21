package com.cycling.data.player

import android.os.SystemClock
import com.cycling.data.di.PlayerDispatcher
import com.cycling.data.player.engine.EngineState
import com.cycling.data.player.engine.PlaybackEngine
import com.cycling.data.player.engine.PlaybackEvent
import com.cycling.data.player.engine.QueueItem
import com.cycling.data.player.engine.TransitionReason
import com.cycling.data.store.PlaybackStateStore
import com.cycling.domain.model.PlayerEvent
import com.cycling.domain.model.PlayerState
import com.cycling.domain.model.RepeatMode
import com.cycling.domain.model.Song
import com.cycling.domain.online.model.MusicInfo
import com.cycling.domain.online.model.MusicQuality
import com.cycling.domain.online.model.OnlineSource
import com.cycling.domain.online.model.PlaySourceResolution
import com.cycling.domain.online.usecase.ResolveOnlinePlaySourceUseCase
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val playEventRecorder: PlayEventRecorder,
    private val resolveOnlinePlaySource: ResolveOnlinePlaySourceUseCase,
    @PlayerDispatcher private val dispatcher: CoroutineDispatcher
) : PlayerRepository {

    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _events = MutableSharedFlow<PlayerEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    override val events: SharedFlow<PlayerEvent> = _events.asSharedFlow()

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

    /**
     * 听歌统计：进度循环上一次计时的时刻（[SystemClock.elapsedRealtime]），0 表示这一轮还没开始。
     *
     * 用墙上时间而不是播放位置差：位置差在倍速播放时会翻倍，在缓冲时又会不动，
     * 而"听了多久"应该只跟真的在播的时间有关。
     */
    private var lastListenTickAtMs = 0L

    /** 进度循环最后看到的播放位置与总时长，收尾时用来判断是不是听到了结尾。 */
    private var recordedPositionMs = 0L
    private var recordedDurationMs = 0L

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

    /** 连续失败次数与上次失败时刻：坏歌成片时（整个目录被删）不能无限自动跳。 */
    private var consecutivePlaybackErrors = 0
    private var lastPlaybackErrorAtMs = 0L

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
            is PlaybackEvent.Error -> onPlaybackError(event)
            PlaybackEvent.Ended -> {
                Timber.d("onEngineEvent: playback ended")
                // 队列放到头了：这一首算"听完"。续播如果接上，下一首的 ItemChanged 会开新记录。
                playEventRecorder.finish(completed = true, songId = currentSongId)
                recordedPositionMs = 0L
                recordedDurationMs = 0L
                _playerState.update { it.copy(isPlaying = false) }
                scope.launch { continueQueueIfEnabled() }
            }
        }
    }

    /**
     * 一首歌放不出来时的处理：先上报，再决定要不要跳过。
     *
     * 跳过的边界：只在队列里还有下一首、且连续失败没超过 [MAX_AUTO_SKIPS] 时跳。
     * 一首坏歌不该让整个队列停住，但"整个目录都被删了"也绝不能变成一路狂跳。
     */
    private fun onPlaybackError(event: PlaybackEvent.Error) {
        Timber.e(
            "onEngineEvent: playback failed id=%s code=%s",
            event.itemId,
            event.errorCodeName
        )

        val now = SystemClock.elapsedRealtime()
        consecutivePlaybackErrors = if (now - lastPlaybackErrorAtMs > ERROR_STREAK_WINDOW_MS) {
            1
        } else {
            consecutivePlaybackErrors + 1
        }
        lastPlaybackErrorAtMs = now

        val engineState = engine.state.value
        val hasNext = engineState.currentIndex in 0 until engineState.queueIds.lastIndex
        val canSkip = hasNext && consecutivePlaybackErrors <= MAX_AUTO_SKIPS

        _events.tryEmit(PlayerEvent.PlaybackFailed(songId = event.itemId, skipped = canSkip))
        if (canSkip) {
            engine.skipToNext()
            engine.play()
        } else {
            // 停在出错的这一首上，界面据此把"正在播放"的错觉收掉。
            engine.pause()
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

        // 上一次听歌记录收尾：用进度循环最后看到的位置判断是否听到了结尾，再开新的记录。
        // 必须在 currentSongId 被改写之前发出，否则迟到的一次 Finish 会记到新歌头上。
        playEventRecorder.finish(
            completed = recordedDurationMs > 0L &&
                recordedPositionMs >= recordedDurationMs - END_OF_TRACK_TOLERANCE_MS,
            songId = currentSongId
        )
        recordedPositionMs = 0L
        recordedDurationMs = 0L

        hasCountedPlay = false
        lastPersistedPosition = -1L
        currentSongId = id
        continuationInFlight = false
        id?.let { songId ->
            songLookup[songId]?.let { song ->
                playEventRecorder.begin(song, PLAY_EVENT_SOURCE)
            }
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
        // 一轮进度循环就是一段连续的播放：第一拍只对表，从第二拍才开始累计时长。
        lastListenTickAtMs = 0L
        progressLoopJob = scope.launch {
            while (isActive) {
                val position = engine.currentPositionMs()
                val engineState = engine.state.value
                _playerState.update { it.copy(playbackPosition = position) }
                checkPlayProgress(position, engineState.durationMs)
                recordedPositionMs = position
                recordedDurationMs = engineState.durationMs
                val nowMs = SystemClock.elapsedRealtime()
                if (lastListenTickAtMs > 0L && engineState.isPlaying) {
                    // 单拍上限兜住"主线程被卡住一阵"的情况，否则一次卡顿会记进几十秒。
                    playEventRecorder.addListened(
                        (nowMs - lastListenTickAtMs).coerceAtMost(MAX_LISTEN_TICK_GAP_MS)
                    )
                }
                lastListenTickAtMs = nowMs
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

    /**
     * 在线曲目取链：拿指针换一次可播放地址。
     *
     * 指针缺失、平台未知、取链失败都返回 null —— 失败原因交给调用方按"放不出来"处理，
     * 这里不猜也不重试。
     */
    private suspend fun resolveOnlineUri(song: Song): String? {
        val source = OnlineSource.fromLxKey(song.onlineSourceKey ?: return null) ?: return null
        val trackId = song.onlineSongId ?: return null
        val track = MusicInfo(
            source = source,
            id = trackId,
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationSeconds = song.duration / 1000,
            coverUrl = song.albumArt
        )
        return when (val resolution = resolveOnlinePlaySource(track, MusicQuality.EXHIGH)) {
            is PlaySourceResolution.Success -> resolution.source.url
            is PlaySourceResolution.Failure -> {
                Timber.w("resolveOnlineUri: %s 取链失败 —— %s", song.title, resolution.message)
                null
            }
            PlaySourceResolution.NotConfigured -> null
        }
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
            // 在线曲目的 path 是空的：指针存在，地址每次播放时才取。
            // 取链要走网络（脚本启动 + 可播性探测，最长数秒），必须离开 Main，
            // 否则 @PlayerDispatcher 被占住，界面与引擎回调一起卡住。
            val playable = if (song.isOnline && song.path.isBlank()) {
                val url = withContext(Dispatchers.IO) { resolveOnlineUri(song) }
                if (url == null) {
                    _events.tryEmit(PlayerEvent.PlaybackFailed(songId = song.id, skipped = false))
                    return@launch
                }
                song.copy(path = url)
            } else {
                song
            }

            // 队列语义不变：空队列表示"播这一首，保留已有队列"。
            val queueSongs = if (queue.isNotEmpty()) {
                queue
            } else {
                val existing = engine.state.value.queueIds.mapNotNull(songLookup::get)
                if (existing.any { it.id == playable.id }) existing else existing + playable
            }

            // ponytail: 队列里**尚未解析**的在线曲目这次不参与播放。逐首预取要改引擎的
            // MediaSource 构造（占位项会先 prepare 失败），那是独立的一次改造；
            // 当前保证"点哪首播哪首"，宁可队列短也不产生坏项。
            val items = queueSongs
                .map { if (it.id == playable.id) playable else it }
                .filter { !it.isOnline || it.path.isNotBlank() }
                // 队列里同一首歌只能有一行：队列面板用 song.id 当列表 key，重复行会直接抛
                // "Key ... was already used" 闪退；随机/上一首的定位也按 id 找，重复只会踩错行。
                .distinctBy { it.id }
                .ifEmpty { listOf(playable) }

            registerSongs(items + playable)
            val startIndex = items.indexOfFirst { it.id == playable.id }.coerceAtLeast(0)

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
        // 已在队列里就什么都不做：重复行没有意义，还会让队列面板的 song.id key 撞车。
        if (engine.state.value.queueIds.contains(song.id)) return
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

        // 已经在队列里就把它**挪**到当前曲之后，而不是再插一行。
        // 目标下标取决于它在当前曲的前面还是后面：往前挪时当前曲会左移一格。
        val existing = engine.state.value.queueIds.indexOf(song.id)
        if (existing >= 0) {
            val target = if (existing > currentIndex) currentIndex + 1 else currentIndex
            if (existing != target) engine.move(existing, target)
            scope.launch { persistQueuePosition() }
            return
        }

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

        /** 一轮连续失败里最多自动跳过几首；超过就停下来，避免整库坏文件时一路狂跳。 */
        const val MAX_AUTO_SKIPS = 3

        /** 两次失败间隔超过它就算新的一轮（一首好歌播了 5 秒以上，之前的失败不该继续累计）。 */
        const val ERROR_STREAK_WINDOW_MS = 5_000L

        /** 播放位置离结尾这么近就算"听完了"，留出尾奏被跳过、误差几十毫秒的余量。 */
        const val END_OF_TRACK_TOLERANCE_MS = 1_500L

        /** 单拍最多累计多少听歌时长：主线程卡顿或设备休眠回来时不要一次记进一大段。 */
        const val MAX_LISTEN_TICK_GAP_MS = 3_000L

        /** 播放事件的来源标记，只用于排查，不参与统计。 */
        const val PLAY_EVENT_SOURCE = "queue"
    }
}
