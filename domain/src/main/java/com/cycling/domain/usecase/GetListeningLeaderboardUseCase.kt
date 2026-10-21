package com.cycling.domain.usecase

import com.cycling.domain.model.LeaderboardMetric
import com.cycling.domain.model.LeaderboardRange
import com.cycling.domain.model.ListeningLeaderboard
import com.cycling.domain.repository.ListeningStatsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * 听歌排行榜。
 *
 * 窗口与口径都是界面上可切换的参数，所以这里只做一层转交：真正的排序、占比、
 * 乐迷档案都在仓库里一次算完，切换 tab 不需要重新收集列表。
 */
class GetListeningLeaderboardUseCase @Inject constructor(
    private val repository: ListeningStatsRepository
) {
    operator fun invoke(
        range: LeaderboardRange,
        metric: LeaderboardMetric
    ): Flow<ListeningLeaderboard> = repository.observeLeaderboard(range, metric)
}
