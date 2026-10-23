package com.cycling.presentation.player

import com.cycling.domain.lyrics.model.SyncedLyrics
import com.cycling.domain.model.HiResBadge
import com.cycling.domain.model.LyricsCandidate
import com.cycling.domain.model.LyricsSource
import com.cycling.domain.model.PlayerState
import com.cycling.domain.model.RepeatMode
import androidx.annotation.StringRes
import com.cycling.domain.model.Song
import com.cycling.domain.repository.SettingsRepository.Companion.DEFAULT_LYRICS_TEXT_SIZE_SP
import com.cycling.domain.repository.SettingsRepository.Companion.DEFAULT_PLAYBACK_SPEED
import com.cycling.domain.repository.SettingsRepository.Companion.DEFAULT_SLEEP_TIMER_PRESET_MINUTES

/** 播放页的三页：封面 / 歌词 / 队列。三者在同一屏内滑动交叉淡入（Apple Music 版式）。 */
enum class PlayerViewMode {
    COVER,
    LYRICS,
    QUEUE
}

data class PlayerUiState(
    val currentSong: Song? = null,
    val playbackPosition: Long = 0,
    val duration: Long = 0,
    val isPlaying: Boolean = false,
    val playbackQueue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleMode: Boolean = false,
    val isFavorite: Boolean = false,
    val viewMode: PlayerViewMode = PlayerViewMode.COVER,
    val lyrics: SyncedLyrics? = null,
    val isLoadingLyrics: Boolean = false,
    val hasLyrics: Boolean = false,
    /** 歌词来自哪里。本地 .lrc 或内嵌标签是用户自己的，「选择歌词」对它们没有意义。 */
    val lyricsSource: LyricsSource = LyricsSource.NONE,
    /** 可选的歌词版本；空表示这次还没查过，或确实没有别的版本。 */
    val lyricsCandidates: List<LyricsCandidate> = emptyList(),
    val showLyricsCandidates: Boolean = false,
    val isLoadingLyricsCandidates: Boolean = false,
    /** 歌词时间轴的手动校正，正值让歌词延后。每首歌各存一份，见 `SongRepository.getLyricsOffset`。 */
    val lyricsOffsetMs: Long = 0L,
    /** 「播放设置」面板是否展开（倍速与定时关闭）。 */
    val showPlaybackSettings: Boolean = false,
    val playbackSpeed: Float = DEFAULT_PLAYBACK_SPEED,
    /** 播放页底部频谱条；关闭后这一屏不再有自发重绘。 */
    val playerSpectrumEnabled: Boolean = true,
    /** 定时关闭剩余毫秒，没开就是 null。 */
    val sleepTimerRemainingMs: Long? = null,
    /** 上次选过的睡眠时长，用作面板里高亮的那一档。 */
    val sleepTimerMinutes: Int = DEFAULT_SLEEP_TIMER_PRESET_MINUTES,
    /** 封面左下角的小金标；纯装饰，不参与音频格式判断。 */
    val hiResBadge: HiResBadge = HiResBadge.OFF,
    /** 自定义小金标的图片 URI；[hiResBadge] 不是 CUSTOM 时忽略。 */
    val hiResBadgeCustomUri: String? = null,
    /** 歌词字号，sp。 */
    val lyricsTextSizeSp: Int = DEFAULT_LYRICS_TEXT_SIZE_SP,
    /** 歌词文本是否居中。 */
    val lyricsAlignCenter: Boolean = false,
    /** 歌词当前行是否带柔光。 */
    val lyricsGlowEnabled: Boolean = false,
    /** 歌词非当前行是否按距离虚化。 */
    val lyricsBlurEnabled: Boolean = true
)

/** 歌词偏移一次调整的步长，也是界面上两个按钮的量。 */
const val LYRICS_OFFSET_STEP_MS = 500L

/** 偏移的可调范围。再大就不是微调，而是这份歌词整体对错了歌。 */
const val MAX_LYRICS_OFFSET_MS = 5_000L

sealed interface PlayerIntent {
    data class PlaySong(val song: Song, val queue: List<Song> = emptyList()) : PlayerIntent
    data class PlayFromQueue(val index: Int) : PlayerIntent
    data object PlayPause : PlayerIntent
    data class SeekTo(val position: Long) : PlayerIntent
    data object SkipToNext : PlayerIntent
    data object SkipToPrevious : PlayerIntent
    data object ToggleRepeatMode : PlayerIntent
    data object ToggleShuffleMode : PlayerIntent
    data class AddToQueue(val song: Song) : PlayerIntent
    data class PlayNext(val song: Song) : PlayerIntent
    data class MoveQueueItem(val fromIndex: Int, val toIndex: Int) : PlayerIntent
    data class RemoveFromQueue(val index: Int) : PlayerIntent
    data object ClearQueue : PlayerIntent
    data object UpdateProgress : PlayerIntent
    data object ToggleFavorite : PlayerIntent
    data object ToggleViewMode : PlayerIntent

    /** 直接切到某一页（封面 / 歌词 / 队列）。 */
    data class SetViewMode(val mode: PlayerViewMode) : PlayerIntent

    /** Asks for the current song's lyrics again, ignoring whatever was downloaded for it before. */
    data object RefreshLyrics : PlayerIntent

    /** 打开「选择歌词」：向各在线来源要一份版本列表。每次打开都重新查，不做缓存。 */
    data object OpenLyricsCandidates : PlayerIntent

    data object DismissLyricsCandidates : PlayerIntent

    /** 换用 [candidateId] 那一份歌词，并记住它。 */
    data class SelectLyricsCandidate(val candidateId: String) : PlayerIntent

    /** Nudges the current song's lyric timing by [deltaMs]; positive shifts the lyrics later. */
    data class AdjustLyricsOffset(val deltaMs: Long) : PlayerIntent

    data object ResetLyricsOffset : PlayerIntent

    /** 展开 / 收起播放设置面板。 */
    data object TogglePlaybackSettings : PlayerIntent

    data class SetPlaybackSpeed(val speed: Float) : PlayerIntent

    /** 设定定时关闭；[minutes] 为 0 表示取消。 */
    data class SetSleepTimer(val minutes: Int) : PlayerIntent

    data object CancelSleepTimer : PlayerIntent

    /** 开关播放页底部的频谱条。 */
    data class SetPlayerSpectrumEnabled(val enabled: Boolean) : PlayerIntent

    // ── 歌词界面外观 ────────────────────────────────────────────────────
    // 这几项入口在歌词页自己的面板里：听歌时才想起来调，跳回设置页再回来非常打断。

    /** 歌词字号，sp。 */
    data class SetLyricsTextSizeSp(val sp: Int) : PlayerIntent

    data class SetLyricsAlignCenter(val enabled: Boolean) : PlayerIntent

    data class SetLyricsGlowEnabled(val enabled: Boolean) : PlayerIntent

    data class SetLyricsBlurEnabled(val enabled: Boolean) : PlayerIntent
}

sealed interface PlayerEffect {
    data class ShowToast(@StringRes val message: Int) : PlayerEffect
}

fun PlayerState.toUiState(): PlayerUiState {
    return PlayerUiState(
        currentSong = currentSong,
        playbackPosition = playbackPosition,
        duration = duration,
        isPlaying = isPlaying,
        playbackQueue = playbackQueue,
        queueIndex = queueIndex,
        repeatMode = repeatMode,
        shuffleMode = shuffleMode
    )
}
