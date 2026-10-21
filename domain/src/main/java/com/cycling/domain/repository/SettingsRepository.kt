package com.cycling.domain.repository

import com.cycling.domain.model.HiResBadge
import com.cycling.domain.model.LyricsProvider
import kotlinx.coroutines.flow.Flow

/**
 * User preferences, one [Flow] per setting.
 *
 * Deliberately not a single `Flow<AppSettings>` snapshot: changing the playback speed would then
 * re-emit the equalizer state and the lyrics provider order too, and every screen collecting any
 * part of it would recompose. Granular flows keep each change local to the screens that care.
 *
 * Implementations must be usable before anything has been written, so every getter has a
 * documented default.
 */
interface SettingsRepository {

    /** Playback rate, clamped to [MIN_PLAYBACK_SPEED]..[MAX_PLAYBACK_SPEED]. */
    val playbackSpeed: Flow<Float>
    suspend fun setPlaybackSpeed(speed: Float)

    val skipSilence: Flow<Boolean>
    suspend fun setSkipSilence(enabled: Boolean)

    /**
     * 播放页底部的频谱条。关掉之后这一屏不再有任何自发重绘 —— 对省电敏感的机器是唯一开关。
     */
    val playerSpectrumEnabled: Flow<Boolean>
    suspend fun setPlayerSpectrumEnabled(enabled: Boolean)

    val rememberPlaybackPosition: Flow<Boolean>
    suspend fun setRememberPlaybackPosition(enabled: Boolean)

    /** Fade in/out length at track boundaries; 0 disables fading entirely. */
    val audioFadeDurationMs: Flow<Long>
    suspend fun setAudioFadeDurationMs(durationMs: Long)

    val replayGainEnabled: Flow<Boolean>
    suspend fun setReplayGainEnabled(enabled: Boolean)

    /** Default duration offered by the sleep-timer sheet, in minutes. */
    val sleepTimerPresetMinutes: Flow<Int>
    suspend fun setSleepTimerPresetMinutes(minutes: Int)

    val onlineLyricsEnabled: Flow<Boolean>
    suspend fun setOnlineLyricsEnabled(enabled: Boolean)

    /** When false, starting playback from a list/song click does not open the full player page. */
    val openPlayerOnPlay: Flow<Boolean>
    suspend fun setOpenPlayerOnPlay(enabled: Boolean)

    /**
     * When the queue ends with repeat off, append songs similar to the last one and keep playing.
     * On by default: a queue that stops dead after "play this song" is the surprising outcome.
     */
    val autoContinueEnabled: Flow<Boolean>
    suspend fun setAutoContinueEnabled(enabled: Boolean)

    /** 磁盘缓存总上限，字节。超出后按占用从大到小整目录清空，直到回到上限内。 */
    val cacheLimitBytes: Flow<Long>
    suspend fun setCacheLimitBytes(bytes: Long)

    /** 运行期每小时检查一次缓存上限；启动时无条件补查一次。 */
    val autoCleanCacheEnabled: Flow<Boolean>
    suspend fun setAutoCleanCacheEnabled(enabled: Boolean)

    /** Providers to try, in order. Never empty. */
    val lyricsProviderOrder: Flow<List<LyricsProvider>>
    suspend fun setLyricsProviderOrder(providers: List<LyricsProvider>)

    /** 歌词字号，sp。取值范围 [MIN_LYRICS_TEXT_SIZE_SP]..[MAX_LYRICS_TEXT_SIZE_SP]。 */
    val lyricsTextSizeSp: Flow<Int>
    suspend fun setLyricsTextSizeSp(sp: Int)

    /** 歌词文本是否在可用宽度内居中；关掉就是当前的起始侧对齐。 */
    val lyricsAlignCenter: Flow<Boolean>
    suspend fun setLyricsAlignCenter(enabled: Boolean)

    /** 当前行文字是否带一层柔光。默认关：开着时低对比主题上更容易糊。 */
    val lyricsGlowEnabled: Flow<Boolean>
    suspend fun setLyricsGlowEnabled(enabled: Boolean)

    /** 非当前行是否按距离虚化。默认开，与改造前的表现一致。 */
    val lyricsBlurEnabled: Flow<Boolean>
    suspend fun setLyricsBlurEnabled(enabled: Boolean)

    /** 播放页封面左下角的小金标样式；[HiResBadge.OFF] 表示不显示。 */
    val hiResBadge: Flow<HiResBadge>
    suspend fun setHiResBadge(badge: HiResBadge)

    /**
     * 自定义小金标的图片位置，存 `file://` URI。
     *
     * 图片由设置页拷贝进应用私有目录后再存这里 —— 直接存 SAF 的 `content://` 只是临时授权，
     * 重启后有可能打不开。
     */
    val hiResBadgeCustomUri: Flow<String?>
    suspend fun setHiResBadgeCustomUri(uri: String?)

    companion object {
        const val MIN_PLAYBACK_SPEED = 0.5f
        const val MAX_PLAYBACK_SPEED = 2.0f
        const val DEFAULT_PLAYBACK_SPEED = 1.0f
        const val DEFAULT_AUDIO_FADE_DURATION_MS = 200L
        const val MAX_AUDIO_FADE_DURATION_MS = 2_000L
        const val DEFAULT_SLEEP_TIMER_PRESET_MINUTES = 30

        const val DEFAULT_CACHE_LIMIT_BYTES = 512L * 1024 * 1024
        const val MIN_CACHE_LIMIT_BYTES = 128L * 1024 * 1024
        const val MAX_CACHE_LIMIT_BYTES = 4L * 1024 * 1024 * 1024

        /**
         * 歌词字号范围，对齐 Salt Player 的 12–48。
         *
         * 默认值取主题里 `headlineMedium` 的 28sp —— 范围可以照搬，默认值不该照搬：
         * 换掉默认值等于把所有人现有的歌词版式改掉。
         */
        const val MIN_LYRICS_TEXT_SIZE_SP = 12
        const val MAX_LYRICS_TEXT_SIZE_SP = 48
        const val DEFAULT_LYRICS_TEXT_SIZE_SP = 28

        /**
         * AMLL first: when it has a document it is the best one in the app — word by word and
         * hand-aligned — and when it does not, the miss costs a single indexed request. NetEase
         * follows (word-by-word `yrc` with time translation), then the two line-level Chinese
         * catalogues, and LRCLIB — the documented one — last.
         */
        val DEFAULT_LYRICS_PROVIDER_ORDER = listOf(
            LyricsProvider.AMLL,
            LyricsProvider.NETEASE,
            LyricsProvider.QQ_MUSIC,
            LyricsProvider.KUGOU,
            LyricsProvider.LRCLIB
        )
    }
}
