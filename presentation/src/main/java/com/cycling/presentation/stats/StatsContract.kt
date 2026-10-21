package com.cycling.presentation.stats

import androidx.annotation.StringRes
import com.cycling.domain.model.ListeningStats
import com.cycling.domain.model.SongListeningStat
import java.time.LocalDate

/**
 * 听歌统计的界面状态。
 *
 * 时间脉络里"点开某一天"是一个内联的二级状态（不是一条导航路由）：[selectedDay] 非空时
 * 弹出当天详情，[daySongs] 是那一天的查询结果。
 */
data class StatsUiState(
    val stats: ListeningStats = ListeningStats(),
    val isLoading: Boolean = true,
    val selectedDay: LocalDate? = null,
    val daySongs: List<SongListeningStat> = emptyList(),
    val isLoadingDay: Boolean = false,
    val showClearConfirm: Boolean = false
)

sealed interface StatsIntent {
    /** 时间脉络里点了某一天。 */
    data class SelectDay(val date: LocalDate) : StatsIntent
    data object DismissDay : StatsIntent

    /** 点某首歌 → 播放（常听榜单、当天详情都走这里）。 */
    data class PlaySong(val songId: Long) : StatsIntent

    data object RequestClearStats : StatsIntent
    data object DismissClearStats : StatsIntent
    data object ConfirmClearStats : StatsIntent

    data object NavigateToLeaderboard : StatsIntent
    data object NavigateBack : StatsIntent
}

sealed interface StatsEffect {
    data class NavigateToPlayer(val songId: Long) : StatsEffect
    data object NavigateToLeaderboard : StatsEffect
    data object NavigateBack : StatsEffect
    data class ShowToast(@StringRes val message: Int) : StatsEffect
}
