package com.cycling.presentation.leaderboard

import androidx.annotation.StringRes
import com.cycling.domain.model.LeaderboardMetric
import com.cycling.domain.model.LeaderboardRange
import com.cycling.domain.model.ListeningLeaderboard

/**
 * 听歌排行榜的界面状态。
 *
 * [range] 与 [metric] 是用户的选择（也就是查询参数），[board] 是查回来的结果 —— 两者分开存
 * 是为了让 tab 在结果还没回来时也立刻切到新选中态，而不是等一次查询才响应。
 */
data class LeaderboardUiState(
    val range: LeaderboardRange = LeaderboardRange.WEEK,
    val metric: LeaderboardMetric = LeaderboardMetric.DURATION,
    val board: ListeningLeaderboard = ListeningLeaderboard(),
    val isLoading: Boolean = true
)

sealed interface LeaderboardIntent {
    data class SelectRange(val range: LeaderboardRange) : LeaderboardIntent
    data class SelectMetric(val metric: LeaderboardMetric) : LeaderboardIntent

    /** 点榜上一行 → 播放这首歌。 */
    data class PlaySong(val songId: Long) : LeaderboardIntent

    data object NavigateToStats : LeaderboardIntent
    data object NavigateBack : LeaderboardIntent
}

sealed interface LeaderboardEffect {
    /** 起播成功，请求外壳打开播放页。 */
    data class NavigateToPlayer(val songId: Long) : LeaderboardEffect
    data object NavigateToStats : LeaderboardEffect
    data object NavigateBack : LeaderboardEffect
    data class ShowToast(@StringRes val message: Int) : LeaderboardEffect
}
